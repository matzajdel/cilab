package org.example.versioncontrolserver.services;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.example.versioncontrolserver.dto.*;
import org.example.versioncontrolserver.entities.Commit;
import org.example.versioncontrolserver.dto.LabelDTO;
import org.example.versioncontrolserver.dto.MessageDTO;
import org.example.versioncontrolserver.mapper.CommitFileMapper;
import org.example.versioncontrolserver.mapper.CommitMapper;
import org.example.versioncontrolserver.mapper.LabelMapper;
import org.example.versioncontrolserver.mapper.MessageMapper;
import org.example.versioncontrolserver.repositories.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class RepositoryQueryService {

    private final CommitFileMapper commitFileMapper;
    private final LabelMapper labelMapper;
    private final MessageMapper messageMapper;

    private final RepoRepository repoRepository;
    private final CommitRepository commitRepository;
    private final BranchRepository branchRepository;

    private final MessageRepository messageRepository;
    private final LabelRepository labelRepository;
    private final CommitMapper commitMapper;

    public List<RepoDTO> getAllRepositories() {
        return repoRepository.findAllBy();
    }

    public CommitDetailsDTO getCommitById(String commitId) {
        Commit commit = commitRepository.findCommitDetailsById(commitId)
                .orElseThrow(() -> new EntityNotFoundException("Not found commit with id: " + commitId));

        return commitMapper.toCommitDetailsDTO(commit);
    }

    public List<CommitSummaryDTO> getCommitsByRepository(String repoId) {
        return commitRepository.findAllByRepo_IdOrderByTimestampDesc(Long.parseLong(repoId));
    }

    public List<CommitSummaryDTO> getLastCommitsByUser(String authorEmail) {
        return commitRepository.findFirst6AllByAuthorEmailOrderByTimestampDesc(authorEmail);
    }

    public List<BranchDTO> getBranchesByRepository(String repoId) {
        return branchRepository.findAllByRepo_Id(Long.parseLong(repoId));
    }

    @Transactional(readOnly = true)
    public List<CommitFileDTO> getDiffFilesByCommit(String commitId) {
        Commit currentCommit = commitRepository.findById(commitId)
                .orElseThrow(() -> new EntityNotFoundException("Commit with id: " + commitId + " not found"));

        String parentCommitId = currentCommit.getParentId();
        if (parentCommitId == null) {
            return currentCommit.getFiles().stream()
                    .map(commitFileMapper::toDTO)
                    .toList();
        }

        Commit parentCommit = commitRepository.findById(parentCommitId)
                .orElseThrow(() -> new EntityNotFoundException("Unable to find parent commit"));

        Map<String, String> parentCommitFiles = parentCommit.getFileMap();

        return currentCommit.getFiles().stream()
                .filter(ccf -> {
                    String parentHash = parentCommitFiles.get(ccf.getPath());

                    return !Objects.equals(ccf.getBlobHash(), parentHash);
                })
                .map(commitFileMapper::toDTO)
                .toList();
    }

    public void saveLabel(LabelDTO label, String authorEmail) {
        Commit commitProxy = commitRepository.getReferenceById(label.getCommitId());

        labelRepository.findByCommitIdAndName(label.getCommitId(), label.getName())
                .ifPresentOrElse(
                        existingLabel -> {
                            existingLabel.setValue(label.getValue());
                            labelRepository.save(existingLabel);
                        },
                        () -> labelRepository.save(labelMapper.toEntity(label, commitProxy, authorEmail))
                );

        saveMessage(MessageDTO.builder()
                .text("User " + authorEmail + " marked label " + label.getName() + " as: " + label.getValue())
                .authorEmail(authorEmail)
                .commitId(label.getCommitId())
                .build());
    }

    public void saveMessage(MessageDTO message) {
        Commit commitProxy = commitRepository.getReferenceById(message.getCommitId());
        messageRepository.save(messageMapper.toEntity(message, commitProxy));
    }
}
