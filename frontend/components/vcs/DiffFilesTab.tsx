import {
  Table,
  TableBody,
  TableCaption,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table"
import { GitMerge, Send, CheckCircle, XCircle, MinusCircle, FileCode, MessageSquare, User, Loader2 } from "lucide-react";
import { useState } from 'react';

import FileContent from "./FileContent";
import api from "@/lib/api";

const DiffFilesTab = ({ commitDiffFiles }: { commitDiffFiles: CommitFile[] }) => {
    const [selectedFileContent, setSelectedFileContent] = useState<string>("");
    const [selectedFileName, setSelectedFileName] = useState<string>("");
    const [isFileViewerOpen, setIsFileViewerOpen] = useState(false);
    const [isFileLoading, setIsFileLoading] = useState(false);

    const handleFileClick = async (file: CommitFile) => {
        setSelectedFileName(file.path);
        setIsFileViewerOpen(true);
        setIsFileLoading(true);
        setSelectedFileContent("");

        try {
            const response = await api.get(`/api/v1/vcs/files/${file.blobHash}`);
            setSelectedFileContent(typeof response.data === 'string' ? response.data : JSON.stringify(response.data, null, 2));
        } catch (error) {
            console.error("Error fetching file content:", error);
            setSelectedFileContent("Error loading file content. The API endpoint might not be available.");
        } finally {
            setIsFileLoading(false);
        }
    };

    return (
        <>
            <div className="rounded-lg border border-gray-200 bg-white shadow-sm overflow-hidden">
                <div className="px-6 py-4 border-b border-gray-200 bg-gray-50">
                    <h3 className="text-lg font-medium text-gray-900">Changed Files ({commitDiffFiles.length})</h3>
                </div>
                <Table>
                    <TableHeader className='bg-gray-50'>
                        <TableRow>
                            <TableHead className='w-[50px] pl-4'></TableHead>
                            <TableHead className='px-2'>Filename</TableHead>
                            <TableHead className='px-2 text-right pr-6'>Hash</TableHead>
                        </TableRow>
                    </TableHeader>
                    <TableBody>
                        {commitDiffFiles.length > 0 ? (
                            commitDiffFiles.map((t: CommitFile) => (
                                <TableRow
                                    key={t.id}
                                    onClick={() => handleFileClick(t)}
                                    className={`
                                        cursor-pointer hover:bg-blue-50/50 transition-colors
                                    `}
                                >
                                    <TableCell className="pl-4">
                                        <FileCode className="h-5 w-5 text-gray-400" />
                                    </TableCell>
                                    <TableCell className='pl-2'>
                                        <div className='flex items-center gap-3'>
                                            <h1 className="text-14 font-medium text-gray-700 break-all">
                                                {t.path}
                                            </h1>
                                        </div>
                                    </TableCell>
                                    <TableCell className='text-right pr-6 font-mono text-xs text-gray-500'>
                                        {t.blobHash ? t.blobHash.substring(0, 7) : '-'}
                                    </TableCell>
                
                                </TableRow>
                            ))
                        ) : (
                            <TableRow>
                                <TableCell colSpan={3} className="h-24 text-center text-gray-500">
                                    No changes detected.
                                </TableCell>
                            </TableRow>
                        )}
                    </TableBody>
                </Table>
            </div>

            <FileContent
                selectedFileName={selectedFileName}
                selectedFileContent={selectedFileContent}
                isFileLoading={isFileLoading}
                isFileViewerOpen={isFileViewerOpen}
                setIsFileViewerOpen={setIsFileViewerOpen}
            />
        </>
    )
}

export default DiffFilesTab;