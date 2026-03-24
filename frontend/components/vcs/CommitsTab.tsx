import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { useRouter } from 'next/navigation'
import CategoryBadge from "@/components/ui/CategoryBadge";

interface CommitsTabProps {
  commits: Commit[];
}

const CommitsTab = ({ commits }: CommitsTabProps) => {
    const router = useRouter();

    return (
        <Table>
                <TableHeader className='bg-[#f9fafb]'>
                    <TableRow>
                        <TableHead className='px-2'>Commit message</TableHead>
                        <TableHead className='px-2'>Author</TableHead>
                        <TableHead className='px-2'>Branch</TableHead>
                        <TableHead className='px-2 max-md:hidden'>Commit date</TableHead>
                        <TableHead className='px-2'>Status</TableHead>
                    </TableRow>
                </TableHeader>
                <TableBody>
                    {commits.map((t) => (
                        <TableRow
                            key={t.id}
                            onClick={() => router.push(`/version-control-system/commits/${t.id}`)}
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
                                        {t.message}
                                    </h1>
                                </div>
                            </TableCell>
                            
                            <TableCell className='pl-2 pr-10'>
                                {t.authorEmail}
                            </TableCell>
                            
                            <TableCell className='pl-2 pr-10'>
                                {t.branchName}
                            </TableCell>
        
                            <TableCell className="pl-2 pr-10 max-md:hidden capitalize min-w-24">
                                {new Date(t.timestamp).toLocaleString("pl-PL")}
                            </TableCell>

                            <TableCell className='pl-2 pr-10'>
                                <CategoryBadge category={t.status} />
                            </TableCell>
        
                        </TableRow>
                    ))}
                </TableBody>
            </Table>
    )
}

export default CommitsTab;