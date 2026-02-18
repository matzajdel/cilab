package org.example.pipelineservice.service;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.common.errors.ResourceNotFoundException;
import org.example.pipelineservice.dto.PipelineRunSummaryDTO;
import org.example.pipelineservice.exception.EntityNotFoundException;
import org.example.pipelineservice.exception.PipelineNotFoundException;
import org.example.pipelineservice.exception.PipelineRunException;
import org.example.pipelineservice.kafka.PipelineAssignedProducer;
import org.example.pipelineservice.kafka.events.PipelineAssignedEvent;
import org.example.pipelineservice.kafka.events.PipelineResultEvent;
import org.example.pipelineservice.kafka.events.StageAssignedEvent;
import org.example.pipelineservice.kafka.events.StageResultEvent;
import org.example.pipelineservice.mapper.PipelineRunMapper;
import org.example.pipelineservice.model.pipeline.Pipeline;
import org.example.pipelineservice.model.pipeline.PipelineParameter;
import org.example.pipelineservice.model.pipeline.Stage;
import org.example.pipelineservice.model.pipelineRun.PipelineRun;
import org.example.pipelineservice.model.pipelineRun.PipelineStatus;
import org.example.pipelineservice.model.pipelineRun.StageRunInfo;
import org.example.pipelineservice.model.pipelineRun.StageStatus;
import org.example.pipelineservice.repository.PipelineRepository;
import org.example.pipelineservice.repository.PipelineRunRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PipelineRunService {
    private final PipelineRepository pipelineRepository;
    private final PipelineRunRepository pipelineRunRepository;
    private final PipelineAssignedProducer pipelineAssignedProducer;
    private final PipelineService pipelineService;
    private final PipelineRunMapper pipelineRunMapper;

    public List<PipelineRunSummaryDTO> getRunsByPipelineId(String pipelineId) {
        List<PipelineRun> runs = pipelineRunRepository.findByPipelineId(pipelineId)
                .orElseThrow(() -> new PipelineRunException("No runs found for pipelineId: " + pipelineId));

        return runs.stream()
                .map(pipelineRunMapper::toSummaryDTO)
                .toList();
    }

    public PipelineRun getPipelineRunById(String runId) {
        return pipelineRunRepository.findByRunId(runId)
                .orElseThrow(() -> new PipelineRunException("PipelineRun not found with runId: " + runId));
    }

    public StageRunInfo getStageRunInfo(String stageId) {
        PipelineRun pipelineRun = pipelineRunRepository.findByStagesInfoStageId(stageId);

        if (pipelineRun == null) throw new EntityNotFoundException("PipelineRun not found for stageId: " + stageId);

        return pipelineRun.getStagesInfo().stream()
                .filter(sri -> sri.getStageId().equals(stageId))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("StageRunInfo not found for stageId: " + stageId));
    }

    @Transactional
    public void updatePipelineRunInfo(PipelineResultEvent event) {
        PipelineRun run = pipelineRunRepository.findById(event.getPipelineRunId())
                .orElseThrow(() -> new PipelineRunException("PipelineRun document was not created"));//TODO

        run.setStatus(event.getStatus());
        if (event.getStatus() == PipelineStatus.SUCCESSFUL || event.getStatus() == PipelineStatus.FAILED) {
            run.setEndTime(new Date().toInstant());
        }

        pipelineRunRepository.save(run);
    }

    @Transactional
    public void updateStageRunInfo(StageResultEvent event) {
        PipelineRun run = pipelineRunRepository.findByStagesInfoStageId(event.getStageId());

        StageRunInfo stageInfo = run.getStagesInfo().stream()
                .filter(sri -> sri.getStageId().equals(event.getStageId()))
                .findFirst()
                .orElseThrow(() -> new PipelineRunException("StageRunInfo not found for stageId: " + event.getStageId()));

        stageInfo.setStatus(event.getStatus());
        stageInfo.setResultEnvs(event.getResultEnvs());
        stageInfo.setMessage(event.getMessage());

        if (event.getStartTime() != null) {
            stageInfo.setStartTime(event.getStartTime());
        }
        if (event.getEndTime() != null) {
            stageInfo.setEndTime(event.getEndTime());
        }

        pipelineRunRepository.save(run);
    }

    @Transactional
    public void runPipeline(String pipelineId, String runByEmail, Map<String, String> userParameters) {
        Pipeline pipeline = pipelineService.getPipelineById(pipelineId);

        // 1) Create pipelineRun document in MongoDB
        Map<String, String> parameters = mergeParameters(pipeline, userParameters);
        Map<Stage, String> stageIds = generateStageIds(pipeline);
        List<List<StageAssignedEvent>> stagesAssigned = prepareStagesAssignedEvents(pipeline, stageIds);
        List<StageRunInfo> stagesInfo = prepareStagesInfo(pipeline, stageIds);
        String pipelineRunId = savePipelineRun(pipeline, parameters, stagesInfo, runByEmail);

        // 2) Create PipelineAssignedEvent
        PipelineAssignedEvent event = createPipelineAssignedEvent(pipeline, pipelineRunId, stagesAssigned, parameters); //TODO results needs to be added to script

        // 3) Triggering execution in worker-service (sending event to kafka broker)
        pipelineAssignedProducer.publish(event);
    }

    private Map<Stage, String> generateStageIds(Pipeline pipeline) {
        Map<Stage, String> stageIds = new HashMap<>();
        pipeline.getStages().stream()
                .flatMap(List::stream)
                .forEach(stage -> {
                    String id = UUID.randomUUID().toString();
                    stageIds.put(stage, id);
                });

        return stageIds;
    }

    private Map<String, String> mergeParameters(Pipeline pipeline, Map<String, String> userParameters) {
        Map<String, String> parameters = new HashMap<>(pipeline.getParameters().stream()
                .collect(Collectors.toMap(
                        PipelineParameter::getName,
                        PipelineParameter::getDefaultValue
                )));
        parameters.putAll(userParameters);

        return parameters;
    }

    private List<List<StageAssignedEvent>> prepareStagesAssignedEvents(Pipeline pipeline, Map<Stage, String> stageIds) {
        List<List<StageAssignedEvent>> stagesAssignedEvents = new ArrayList<>();

        pipeline.getStages().forEach(stagesGroup -> {
            List<StageAssignedEvent> parallelStages = new ArrayList<>();

            stagesGroup.forEach(stage -> {
                String id = stageIds.get(stage);

                StageAssignedEvent stageAssigned = new StageAssignedEvent(
                        id,
                        stage.getImage(),
                        addStageEnvironmentToScript(stage)   //script needs parsing
                );
                parallelStages.add(stageAssigned);
            });
            stagesAssignedEvents.add(parallelStages);
        });

        return stagesAssignedEvents;
    }

    private List<StageRunInfo> prepareStagesInfo(Pipeline pipeline, Map<Stage, String> stageIds) {
        List<StageRunInfo> infos = new ArrayList<>();

        pipeline.getStages().stream()
                .flatMap(List::stream)
                .forEach(stage -> {
                    String id = stageIds.get(stage);

                    StageRunInfo stageRunInfo = StageRunInfo.builder()
                            .stageId(id)
                            .name(stage.getName())
                            .image(stage.getImage())
                            .stageEnvVariables(stage.getStageEnvironment().getStageEnvVariables())
                            .status(StageStatus.WAITING)
                            .build();

                    infos.add(stageRunInfo);
                });

        return infos;
    }

    private String savePipelineRun(Pipeline pipeline, Map<String, String> parameters, List<StageRunInfo> stagesInfo, String runByEmail) {
        String pipelineRunId = UUID.randomUUID().toString();
        Map<String, Integer> labels = pipeline.getLabels().stream()
                    .collect(Collectors.toMap(
                            Function.identity(),
                            _ -> 0
                    ));

        pipelineRunRepository.save(
                PipelineRun.builder()
                        .runId(pipelineRunId)
                        .pipelineId(pipeline.getId())
                        .parameters(parameters)
                        .envVariables(pipeline.getEnvVariables())
                        .stagesInfo(stagesInfo)
                        .labels(labels)
                        .status(PipelineStatus.IN_PROGRESS)
                        .runBy(runByEmail)
                        .startTime(new Date().toInstant())
                        .build()
        );

        return pipelineRunId;
    }

    private PipelineAssignedEvent createPipelineAssignedEvent(Pipeline pipeline, String pipelineRunId, List<List<StageAssignedEvent>> stagesAssigned,Map<String, String> parameters) {
        Map<String, String> envToSet = new HashMap<>(parameters);
        envToSet.putAll(pipeline.getEnvVariables());

        return PipelineAssignedEvent.builder()
                .pipelineId(pipeline.getId())
                .pipelineRunId(pipelineRunId)
                .stages(stagesAssigned)
                .envToSet(envToSet)
                .build();
    }

    private String addStageEnvironmentToScript(Stage stage) {
        StringBuilder script = new StringBuilder();

        Map<String, String> stageEnvVariables = stage.getStageEnvironment().getStageEnvVariables() != null
                ? stage.getStageEnvironment().getStageEnvVariables()
                : Map.of();
        stageEnvVariables.forEach((key, val) ->
            script.append("export ").append(key).append("=").append(val).append(" && ")
        );

        return script.append(stage.getScript()).toString();
    }
}
