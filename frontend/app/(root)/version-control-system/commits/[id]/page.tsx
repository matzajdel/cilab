"use client"

import HeaderBox from '@/components/HeaderBox';
import React, { useEffect, useState } from 'react'

import CommitInfo from "@/components/vcs/CommitInfo";
import LabelsSection from "@/components/vcs/LabelsSection";
import CommitMessagesTab from "@/components/vcs/CommitMessagesTab"
import DiffFilesTab from '@/components/vcs/DiffFilesTab';
import api from '@/lib/api';

const CommitPanel = ({ params: { id } }: SearchParamProps) => {
    
    const [commitInfo, setCommitInfo] = useState<Commit | null>(null);
    const [commitDiffFiles, setCommitDiffFiles] = useState<CommitFile[]>([]);
    
    useEffect(() => {
        const fetchCommitInfo = async () => {
            try {
                const response = await api.get(`/api/v1/vcs/commits/${id}`);
                console.log(response.data)
                setCommitInfo(response.data);
            } catch (error) {
                console.error("Error fetching commit info:", error);
            }
        }

        const fetchCommitDiffFiles = async () => {
            try {
                const response = await api.get(`/api/v1/vcs/commits/${id}/diff`)
                setCommitDiffFiles(response.data);
            } catch (error) {
                 console.error("Error fetching diff files:", error);
            }
        }

        fetchCommitInfo();
        fetchCommitDiffFiles();
    }, [id])


    return (
        <div className="transactions no-scrollbar max-h-screen overflow-y-scroll pb-20">
            <div className="transactions-header">
                <HeaderBox title={`Commit Details`} subtext={`Viewing details for commit ${id}`} />
            </div>
            
            <CommitInfo commitInfo={commitInfo} />

            <LabelsSection labels={commitInfo?.labels} />

            <div className="flex w-full flex-col gap-6 mt-6 pb-8">
                <DiffFilesTab commitDiffFiles={commitDiffFiles} />

                <CommitMessagesTab messages={commitInfo?.messages} />
            </div>
            
        </div>
        
    )
}

export default CommitPanel