import {
  Table,
  TableBody,
  TableCaption,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table"
import CategoryBadge from "../ui/CategoryBadge";
import { useRouter } from "next/navigation";

const PipelineRunsTable = ({ pipelineRuns }: PipelineRunsTableProps) => {
    const router = useRouter();
    
    return (
        <Table>
            <TableHeader className='bg-[#f9fafb]'>
                <TableRow>
                    <TableHead className='px-2'>Run number</TableHead>
                    <TableHead className='px-2'>Status</TableHead>
                    <TableHead className='px-2'>Start Time</TableHead>
                    <TableHead className='px-2'>End Time</TableHead>
                    <TableHead className='px-2 max-md:hidden'>Runned By</TableHead>
                    {/* <TableHead className='px-2 max-md:hidden'>Category</TableHead> */}
                </TableRow>
            </TableHeader>
            <TableBody>
                {pipelineRuns.map((t) => (
                    <TableRow
                        key={t.runId}
                        onClick={() => router.push(`/pipelines/runs/${t.runId}`)}
                        className={`
                            cursor-pointer hover:bg-blue-100
                            ${
                            t.status === 'SUCCESSFUL'
                                ? 'bg-green-50'
                                : t.status === 'IN_PROGRESS'
                                ? 'bg-yellow-50'
                                : t.status === 'FAILED'
                                ? 'bg-red-50'
                                : 'bg-blue-50'
                            }
                        `}
                    >
                        <TableCell className='max-w-[250px] pl-2 pr-10'>
                            <div className='flex items-center gap-3'>
                                <h1 className="text-14 truncate font-semibold text-[#344054]">
                                    {t.runId}
                                </h1>
                            </div>
                        </TableCell>
                
                        <TableCell className='pl-2 pr-10'>
                            <CategoryBadge category={t.status} />
                        </TableCell>
    
                        <TableCell className='pl-2 pr-10'>
                            {(t.startTime != null) ? new Date(t.startTime).toLocaleString() : "-"}
                        </TableCell>

                        <TableCell className='pl-2 pr-10'>
                            {(t.endTime != null) ? new Date(t.endTime).toLocaleString() : "-"}
                        </TableCell>
    
                        <TableCell className="pl-2 pr-10 max-md:hidden capitalize min-w-24">
                            {t.runBy}
                        </TableCell>
    
                    </TableRow>
                ))}
            </TableBody>
        </Table>
    )
}

export default PipelineRunsTable;