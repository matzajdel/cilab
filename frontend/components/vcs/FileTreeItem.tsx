import { useState } from 'react'
import { cn } from '@/lib/utils';
import { API_BASE_URL } from '@/lib/config';
import { Loader2, Folder, FolderOpen, File, ChevronRight, ChevronDown, GitBranch, Hash, FileCode } from "lucide-react";
import {
    Dialog,
    DialogContent,
    DialogDescription,
    DialogHeader,
    DialogTitle,
} from "@/components/ui/dialog"
import axios from 'axios';
import FileContent from './FileContent';

interface FileTreeNode {
    blobHash: string | null;
    children: FileTreeNode[] | null;
    name: string;
    path: string;
    type: 'FILE' | 'DIRECTORY';
}

const FileTreeItem = ({ node, level = 0 }: { node: FileTreeNode, level?: number }) => {
    const [isOpen, setIsOpen] = useState(level === 0); // Root always open
    const [isFileViewerOpen, setIsFileViewerOpen] = useState(false);
    const [isFileLoading, setIsFileLoading] = useState(false);
    const [selectedFileContent, setSelectedFileContent] = useState<string>("");
    
    const isDirectory = node.type === 'DIRECTORY';
    const hasChildren = node.children && node.children.length > 0;

    const handleClick = async () => {
        if (isDirectory) {
            setIsOpen(!isOpen);
        } else {
            if (!node.blobHash) {
                console.log('No blob hash available for file:', node.path);
                return;
            }
            
            setIsFileViewerOpen(true);
            setIsFileLoading(true);
            setSelectedFileContent("");

            try {
                const response = await api.get(`/api/v1/vcs/files/${node.blobHash}`);
                setSelectedFileContent(typeof response.data === 'string' ? response.data : JSON.stringify(response.data, null, 2));
            } catch (error) {
                console.error("Error fetching file content:", error);
                setSelectedFileContent("Error loading file content. The API endpoint might not be available.");
            } finally {
                setIsFileLoading(false);
            }
        }
    };

    return (
        <div className="select-none">
            <div 
                className={cn(
                    "flex items-center gap-2 py-1.5 px-2 rounded cursor-pointer transition-colors",
                    "hover:bg-gray-100",
                    level === 0 ? "" : "ml-4"
                )}
                onClick={handleClick}
                style={{ paddingLeft: `${level * 16 + 8}px` }}
            >
                {/* Chevron for directories */}
                {isDirectory && hasChildren && (
                    <span className="flex-shrink-0">
                        {isOpen ? (
                            <ChevronDown className="w-4 h-4 text-gray-500" />
                        ) : (
                            <ChevronRight className="w-4 h-4 text-gray-500" />
                        )}
                    </span>
                )}
                {isDirectory && !hasChildren && (
                    <span className="flex-shrink-0 w-4" />
                )}

                {/* Icon */}
                {isDirectory ? (
                    isOpen ? (
                        <FolderOpen className="w-4 h-4 text-blue-500" />
                    ) : (
                        <Folder className="w-4 h-4 text-blue-500" />
                    )
                ) : (
                    <File className="w-4 h-4 text-gray-400" />
                )}

                {/* Name */}
                <span className={cn(
                    "text-sm flex-1",
                    isDirectory ? "font-medium text-gray-800" : "text-gray-600"
                )}>
                    {node.name}
                </span>

                {/* Blob hash for files */}
                {!isDirectory && node.blobHash && (
                    <span className="text-xs text-gray-400 font-mono flex items-center gap-1">
                        <Hash className="w-3 h-3" />
                        {node.blobHash.substring(0, 7)}
                    </span>
                )}
            </div>

            {/* Children */}
            {isDirectory && isOpen && hasChildren && (
                <div className="ml-0">
                    {node.children!.map((child, idx) => (
                        <FileTreeItem 
                            key={child.path || idx} 
                            node={child} 
                            level={level + 1}
                        />
                    ))}
                </div>
            )}

            {/* File Viewer Dialog */}
            {!isDirectory && (
                <FileContent
                    selectedFileName={node.path}
                    selectedFileContent={selectedFileContent}
                    isFileLoading={isFileLoading}
                    isFileViewerOpen={isFileViewerOpen}
                    setIsFileViewerOpen={setIsFileViewerOpen}
                />
            )}
        </div>
    );
};

export default FileTreeItem;