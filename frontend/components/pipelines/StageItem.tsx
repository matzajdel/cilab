import { cn } from '@/lib/utils';
import { useState } from 'react';
import { Loader2, CheckCircle2, XCircle, Clock, PlayCircle, User, Box, Terminal, Layers, ArrowRight, ChevronDown, ChevronUp } from "lucide-react";
import StatusBadge from './StatusBadge';
import Card from './Card';
import StageLogs from './StageLogs';


const StageItem = ({ stage, index, total }: { stage: StageInfo, index: number, total: number }) => {
    const [logsOpen, setLogsOpen] = useState(false);
    
    // Calculate duration
    const duration = stage.startTime && stage.endTime
        ? ((new Date(stage.endTime).getTime() - new Date(stage.startTime).getTime()) / 1000).toFixed(2) + 's'
        : '-';

    return (
        <div className="relative pl-8 pb-8 last:pb-0">
            {/* Timeline connector and dot */}
            <div className={cn(
                "absolute left-0 top-0 bottom-0 w-px bg-gray-200 ml-[11px]",
                index === total - 1 ? "h-6" : ""
            )} />
            
            <div className={cn(
                "absolute left-0 top-1 w-6 h-6 rounded-full flex items-center justify-center border-2 z-10 bg-white shadow-sm",
                stage.status === 'SUCCESSFUL' ? 'border-green-500 text-green-600' :
                stage.status === 'FAILED' ? 'border-red-500 text-red-600' : 
                stage.status === 'IN_PROGRESS' ? 'border-blue-500 text-blue-600' : 'border-gray-300 text-gray-400'
            )}>
                {stage.status === 'SUCCESSFUL' && <CheckCircle2 className="w-3 h-3" />}
                {stage.status === 'FAILED' && <XCircle className="w-3 h-3" />}
                {stage.status === 'IN_PROGRESS' && <Loader2 className="w-3 h-3 animate-spin" />}
                {(stage.status !== 'SUCCESSFUL' && stage.status !== 'FAILED' && stage.status !== 'IN_PROGRESS') && <span className="text-[10px] font-bold">{index + 1}</span>}
            </div>

            {/* Stage Card */}
            <Card className="flex-1 transition-all hover:shadow-md border-gray-200">
                <div 
                    className="px-4 py-3 bg-gray-50/30 border-b border-gray-100 flex flex-wrap items-center justify-between gap-2 cursor-pointer hover:bg-gray-100/50"
                    onClick={() => setLogsOpen(!logsOpen)}
                >
                    <div>
                        <h4 className="text-sm font-bold text-gray-800 flex items-center gap-2">
                             {stage.name}
                        </h4>
                        <div className="flex gap-3 mt-1 text-xs text-gray-500">
                             <span className="flex items-center gap-1 bg-gray-100 px-1.5 py-0.5 rounded text-[10px]">
                                <Box className="w-3 h-3"/> {stage.image}
                            </span>
                            <span className="flex items-center gap-1 bg-gray-100 px-1.5 py-0.5 rounded text-[10px]">
                                <Clock className="w-3 h-3"/> {duration}
                            </span>
                        </div>
                    </div>
                    <div className="flex items-center gap-3">
                         <StatusBadge status={stage.status} />
                         <div className="text-gray-400 hover:text-gray-600 p-1">
                            {logsOpen ? <ChevronUp className="w-4 h-4" /> : <ChevronDown className="w-4 h-4" />}
                         </div>
                    </div>
                </div>
                
                {/* Expansible Content */}
                <div className={cn("p-4 transition-all duration-300", logsOpen ? "block" : "hidden")}>
                    {stage.message && (
                        <div className="mb-3 bg-gray-900 text-gray-200 p-2.5 rounded text-xs font-mono overflow-x-auto">
                            <span className="text-green-400 mr-2">$</span>
                            {stage.message}
                        </div>
                    )}

                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mb-4">
                        {stage.stageEnvVariables && Object.keys(stage.stageEnvVariables).length > 0 && (
                            <div>
                                <h5 className="text-[10px] font-bold text-gray-400 uppercase mb-1.5 tracking-wider">Stage environment</h5>
                                <div className="bg-blue-50/50 border border-blue-100 p-2 rounded text-xs space-y-1">
                                    {Object.entries(stage.stageEnvVariables).map(([k, v]) => (
                                        <div key={k} className="flex gap-2 overflow-hidden"><span className="text-blue-600 font-medium shrink-0">{k}=</span><span className="truncate text-gray-600" title={String(v)}>{v}</span></div>
                                    ))}
                                </div>
                            </div>
                        )}
                         {stage.resultEnvs && Object.keys(stage.resultEnvs).length > 0 && (
                            <div>
                                <h5 className="text-[10px] font-bold text-gray-400 uppercase mb-1.5 tracking-wider">Output Variables</h5>
                                <div className="bg-green-50/50 border border-green-100 p-2 rounded text-xs space-y-1">
                                    {Object.entries(stage.resultEnvs).map(([k, v]) => (
                                        <div key={k} className="flex gap-2 overflow-hidden"><span className="text-green-600 font-medium shrink-0">{k}=</span><span className="truncate text-gray-600" title={String(v)}>{v}</span></div>
                                    ))}
                                </div>
                            </div>
                        )}
                    </div>

                    {/* Live Logs Section */}
                    {logsOpen && (
                        <div className="border-t border-gray-100 pt-2">
                            <StageLogs stageId={stage.stageId} />
                        </div>
                    )}
                </div>
            </Card>
        </div>
    )
}

export default StageItem;