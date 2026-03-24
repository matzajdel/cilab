import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { TabsContent } from "@/components/ui/tabs";
import { useRouter } from 'next/navigation'

interface BranchesTabProps {
  branches: Branch[];
}

const BranchesTab = ({ branches }: BranchesTabProps) => {
    const router = useRouter();

    return (
        <TabsContent value="branches">
        <div className="rounded-md border">
          <Table>
                <TableHeader className='bg-[#f9fafb]'>
                    <TableRow>
                        <TableHead className='px-2'>Branch name</TableHead>
                        <TableHead className='px-2'>Head commit Id</TableHead>
                    </TableRow>
                </TableHeader>
                <TableBody>
                    {branches.map((t) => (
                        <TableRow
                            key={t.id}
                            onClick={() => router.push(`branches/${t.id}`)}
                            className="cursor-pointer hover:bg-blue-100"
                        >
                            <TableCell className='max-w-[250px] pl-2 pr-10'>
                                <div className='flex items-center gap-3'>
                                    <h1 className="text-14 truncate font-semibold text-[#344054]">
                                        {t.name}
                                    </h1>
                                </div>
                            </TableCell>
                            
                            <TableCell className='pl-2 pr-10'>
                                {t.headCommitId}
                            </TableCell>
                        </TableRow>
                    ))}
                </TableBody>
            </Table>
        </div>
      </TabsContent>
    );
}

export default BranchesTab;