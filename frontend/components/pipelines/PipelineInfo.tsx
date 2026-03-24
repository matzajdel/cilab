import { Button } from "@/components/ui/button";
import { Play } from "lucide-react";

const PipelineInfo = ({ pipelineInfo, handleRunPipeline }: PipelineInfoProps) => {
    const hasPipelineInfo = Boolean(pipelineInfo);
    
    return (
        <div className='transactions-account'>
            <div className='flex flex-col gap-2'>
            <h2 className='text-18 text-white font-bold'>
                {pipelineInfo?.name ?? "Pipeline details unavailable"}
            </h2>

            <p className='text-14 text-blue-25'>
                {pipelineInfo?.id ?? "No pipeline metadata returned"}
            </p>

            <p className='text-14 font-semibold tracking-[1.1px] text-white'>
                Author: {pipelineInfo?.authorEmail ?? "Unknown"}
            </p>
            </div>


            <Button 
                onClick={handleRunPipeline}
                disabled={!hasPipelineInfo}
                className='transactions-account-balance flex flex-col items-center justify-center gap-2 h-auto py-4 bg-blue-600 hover:bg-blue-700 text-white'
            >
                <p className='text-14 font-medium'>
                    Run pipeline
                </p>
                <Play className="size-6 fill-white" />
            </Button>
        </div>
    );
}

export default PipelineInfo;