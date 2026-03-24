"use client"

import React, { useEffect, useState } from 'react'
import { Loader2, CheckCircle2, XCircle, Clock, User, Box, Terminal, Layers, ArrowRight } from "lucide-react";
import { useRouter } from 'next/navigation'
import { API_BASE_URL } from '@/lib/config';
import StatusBadge from '@/components/pipelines/StatusBadge';
import Card from '@/components/pipelines/Card';
import KeyValueList from '@/components/pipelines/KeyValueList';
import StageItem from '@/components/pipelines/StageItem';
import api from '@/lib/api';


const RunDetailsPage = ({ params }: { params: { id: string } }) => {
    const { id } = params;
    const router = useRouter();

    const [runData, setRunData] = useState<PipelineRunDetails | null>(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        const fetchRunDetails = async () => {
            try {
                // Fetch data from API
                const response = await api.get(`/api/v1/runs/${id}`);
                
                const data = response.data;
                setRunData(data);
            } catch (error) {
                console.error("Error fetching run details:", error);

            } finally {
                setLoading(false);
            }
        };

        if (id) {
            fetchRunDetails();
        }
    }, [id]);

    if (loading) {
        return (
            <div className="w-full h-96 flex flex-col items-center justify-center gap-2">
                <Loader2 className="w-8 h-8 animate-spin text-blue-600"/>
                <p className="text-sm text-gray-500">Loading run details...</p>
            </div>
        );
    }

    if (!runData) {
        return (
            <div className="p-8 text-center">
                <div className="inline-flex items-center justify-center w-12 h-12 rounded-full bg-red-100 mb-4">
                    <XCircle className="w-6 h-6 text-red-600" />
                </div>
                <h3 className="text-lg font-medium text-gray-900">Run not found</h3>
                <p className="mt-1 text-gray-500">Could not retrieve details for run ID: {id}</p>
            </div>
        );
    }

    return (
        <section className="flex flex-col gap-6 p-1 md:p-6 bg-gray-50/30 overflow-y-auto h-screen">
            {/* Header */}
            <Card className="p-6 border-l-4 border-l-blue-500">
                <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4">
                    <div>
                        <div className="flex items-center gap-2 mb-1">
                             <Layers className="w-5 h-5 text-blue-500"/>
                             <h1 className="text-xl font-bold text-gray-900">Pipeline Run Details</h1>
                        </div>
                        <p className="text-sm text-gray-500 ml-7">
                            Run ID: <span className="font-mono text-gray-700 bg-gray-100 px-1 rounded">{runData.runId}</span>
                        </p>
                        
                        <div className="flex flex-wrap gap-4 mt-3 ml-7 text-xs text-gray-500">
                            <span className="flex items-center gap-1.5 px-2 py-1 bg-gray-100 rounded-md">
                                <User className="w-3.5 h-3.5 text-gray-400"/> {runData.runBy}
                            </span>
                            <span className="flex items-center gap-1.5 px-2 py-1 bg-gray-100 rounded-md">
                                <Clock className="w-3.5 h-3.5 text-gray-400"/> 
                                {new Date(runData.startTime).toLocaleString()}
                            </span>
                             <span className="flex items-center gap-1.5 px-2 py-1 bg-gray-100 rounded-md">
                                <ArrowRight className="w-3.5 h-3.5 text-gray-400"/> 
                                {runData.endTime != null ? new Date(runData.endTime).toLocaleString() : "-"}
                            </span>
                        </div>
                    </div>
                    
                    <div className="flex flex-col items-end gap-2">
                        <StatusBadge status={runData.status} />
                        <span className="text-xs text-gray-400 font-mono bg-white border px-2 py-1 rounded">
                            {runData.pipelineId}
                        </span>
                    </div>
                </div>
            </Card>

            <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
                
                {/* Left Column: Workflow Stages */}
                <div className="lg:col-span-2">
                    <Card className="h-full border-t-4 border-t-indigo-500">
                        <div className="px-6 py-4 border-b border-gray-100 bg-white">
                            <h2 className="text-base font-bold text-gray-800 flex items-center gap-2">
                                <Terminal className="w-4 h-4 text-indigo-500"/>
                                Execution Workflow
                            </h2>
                        </div>
                        
                        <div className="p-6 bg-white min-h-[300px]">
                            {runData.stagesInfo.length === 0 ? (
                                <div className="text-center py-10 text-gray-400 text-sm">
                                    No stages recorded for this run.
                                </div>
                            ) : (
                                <div className="mt-2">
                                    {runData.stagesInfo.map((stage, idx) => (
                                        <StageItem 
                                            key={stage.stageId || idx} 
                                            stage={stage} 
                                            index={idx}
                                            total={runData.stagesInfo.length}
                                        />
                                    ))}
                                </div>
                            )}
                        </div>
                    </Card>
                </div>

                {/* Right Column: Metadata */}
                <div className="space-y-6">
                    <KeyValueList 
                        title="Run Parameters" 
                        data={runData.parameters} 
                        icon={Box}
                    />
                    <KeyValueList 
                        title="Environment Variables" 
                        data={runData.envVariables} 
                        icon={Terminal}
                    />
                    <KeyValueList 
                        title="Labels" 
                        data={runData.labels} 
                        icon={CheckCircle2} 
                    />
                </div>
            </div>
        </section>
    );
};

export default RunDetailsPage;
