"use client"

import HeaderBox from '@/components/HeaderBox';
import React, { useEffect, useState } from 'react'
import { useRouter } from 'next/navigation'
import { API_BASE_URL } from '@/lib/config';

import PipelineParametersForm from '@/components/pipelines/PipelineParametersForm';
import PipelineInfo from '@/components/pipelines/PipelineInfo';
import PipelineRunsTable from '@/components/pipelines/PipelineRunsTable';
import api from '@/lib/api';

const PipelineRunsPanel = ({ params }: { params: { id: string } }) => {
    const { id } = params;

    const router = useRouter();
    const [pipelineRuns, setPipelineRuns] = useState<PipelineRun[]>([]);
    const [loading, setLoading] = useState(true);
    const [pipelineInfo, setPipelineInfo] = useState<PipelineDefinition>();
    const [showParams, setShowParams] = useState(false);
    const [refreshTrigger, setRefreshTrigger] = useState(0);

    const handleRunPipeline = async () => {
    if (pipelineInfo?.parameters && pipelineInfo.parameters.length > 0) {
        setShowParams(true);
        return;
    }

    try {
        const response = await api.post(`/api/v1/pipelines/${id}/run`, {
            parameters: {}
        });

        setRefreshTrigger(prev => prev + 1);
        
    } catch (e) {
        console.error("Błąd podczas uruchamiania pipeline'u:", e);
    }
};

    useEffect(() => {
        const fetchPipelineInfo = async () => {
             try {
                const response = await api.get(`/api/v1/pipelines/${id}`)
                setPipelineInfo(response.data);
            } catch (error) {
                console.error('Error fetching pipeline info:', error);
            } finally {
                setLoading(false);
            }
        }
        fetchPipelineInfo();
    }, []); 

    useEffect(() => {
        const fetchPipelineRuns = async () => {
            try {
                const response = await api.get(`/api/v1/runs/pipelines/${id}`);
                setPipelineRuns(response.data);
            } catch (error) {
                console.error('Error fetching pipelines:', error);
            }
        };
        
        fetchPipelineRuns();
    }, [refreshTrigger]);


    if (loading) {
        return <div>Loading...</div>;
    }

    if (showParams && pipelineInfo) {
        return (
            <div className="transactions">
                <div className="transactions-header">
                    <HeaderBox 
                        title="Run Pipeline"
                        subtext="Please provide the required parameters"
                    />
                </div>

                <section className='size-full pt-5'>
                    <PipelineParametersForm 
                        pipelineId={id} 
                        parameters={pipelineInfo.parameters} 
                        onCancel={() => {
                            setShowParams(false);
                            setRefreshTrigger(prev => prev + 1);
                        }}
                    />
                </section>
            </div>
        );
    }
    
    return (
        <div className="transactions">
            <div className="transactions-header">
                <HeaderBox title={`Pipeline Details`} subtext={`Viewing details for pipeline ${id}`} />
            </div>
            
            <PipelineInfo pipelineInfo={pipelineInfo} handleRunPipeline={handleRunPipeline}/>

            <PipelineRunsTable pipelineRuns={pipelineRuns} />
        </div>
    );
};

export default PipelineRunsPanel;