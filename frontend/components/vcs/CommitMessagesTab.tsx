import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table"
import { MessageSquare, User } from "lucide-react";

const CommitMessagesTab = ({ messages }: CommitMessagesTabProps) => {
    const normalizedMessages = Array.isArray(messages)
        ? messages
        : messages
            ? [messages]
            : [];
    const hasMessages = normalizedMessages.length > 0;

    return (
        <div className="rounded-lg border border-gray-200 bg-white shadow-sm overflow-hidden">
            <div className="px-6 py-4 border-b border-gray-200 bg-gray-50">
                <h3 className="text-lg font-medium text-gray-900">Commit messages</h3>
            </div>
            <Table>
                <TableHeader className='bg-gray-50'>
                    <TableRow>
                        <TableHead className='w-[50px] pl-4'></TableHead>
                        <TableHead className='w-[300px] px-2'>Author</TableHead>
                        <TableHead className='px-2'>Content</TableHead>
                        {/* <TableHead className='px-2 text-right pr-6'>Date</TableHead> */}
                    </TableRow>
                </TableHeader>
                <TableBody>
                    {hasMessages ? (
                        normalizedMessages.map((message, index) => (
                            <TableRow key={`${message.commitId || 'message'}-${index}`} className='cursor-pointer hover:bg-blue-50/50 transition-colors'>
                                <TableCell className="pl-4">
                                    <MessageSquare className="h-5 w-5 text-blue-500" />
                                </TableCell>
                                <TableCell className='w-[300px] pl-2 align-top'>
                                    <div className='flex items-center gap-2'>
                                        <User className="h-4 w-4 text-gray-400" />
                                        <span
                                            className="text-14 font-medium text-gray-700 truncate block max-w-[250px]"
                                            title={message.authorEmail || 'Unknown'}
                                        >
                                            {message.authorEmail || 'Unknown'}
                                        </span>
                                    </div>
                                </TableCell>
                                <TableCell className='pl-2 font-medium text-sm text-gray-900 whitespace-pre-wrap'>
                                    {message.text}
                                </TableCell>
                            </TableRow>
                        ))
                    ) : (
                        <TableRow>
                            <TableCell colSpan={3} className="h-24 text-center text-gray-500">
                                No messages.
                            </TableCell>
                        </TableRow>
                    )}
                </TableBody>
            </Table>
        </div>
    )
}

export default CommitMessagesTab;