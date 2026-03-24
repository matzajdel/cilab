"use client"

import React, { useEffect, useState } from 'react'
import { cn } from '@/lib/utils';
import { API_BASE_URL } from '@/lib/config';
import { Loader2, Folder, FolderOpen, File, ChevronRight, ChevronDown, GitBranch, Hash } from "lucide-react";
import FileTreeItem from '@/components/vcs/FileTreeItem';
import api from '@/lib/api';

const Card = ({ children, className }: { children: React.ReactNode, className?: string }) => (
    <div className={cn("bg-white rounded-lg shadow-sm border border-gray-200/60 overflow-hidden", className)}>
        {children}
    </div>
);

const BranchFilesPage = ({ params }: { params: { id: string } }) => {
    const { id } = params;
    const [fileTree, setFileTree] = useState<FileTreeNode[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        const fetchFileTree = async () => {
            try {
                const response = await api.get(`/api/v1/vcs/branches/${id}/file-tree`);
                
                const data = response.data;
                setFileTree(data);
            } catch (error) {
                console.error("Error fetching file tree:", error);
                setError(error instanceof Error ? error.message : 'Unknown error');
            } finally {
                setLoading(false);
            }
        };

        if (id) {
            fetchFileTree();
        }
    }, [id]);

    if (loading) {
        return (
            <div className="w-full h-96 flex flex-col items-center justify-center gap-2">
                <Loader2 className="w-8 h-8 animate-spin text-blue-600"/>
                <p className="text-sm text-gray-500">Loading file tree...</p>
            </div>
        );
    }

    if (error) {
        return (
            <div className="p-8 text-center">
                <div className="inline-flex items-center justify-center w-12 h-12 rounded-full bg-red-100 mb-4">
                    <Folder className="w-6 h-6 text-red-600" />
                </div>
                <h3 className="text-lg font-medium text-gray-900">Failed to load file tree</h3>
                <p className="mt-1 text-gray-500">{error}</p>
            </div>
        );
    }

    return (
        <section className="flex flex-col gap-6 p-1 md:p-6 bg-gray-50/30 overflow-y-auto h-screen">
            {/* Header */}
            <Card className="p-6 border-l-4 border-l-blue-500">
                <div className="flex items-center gap-3">
                    <GitBranch className="w-5 h-5 text-blue-500"/>
                    <div>
                        <h1 className="text-xl font-bold text-gray-900">Branch Files</h1>
                        <p className="text-sm text-gray-500 mt-1">
                            Branch: <span className="font-mono text-gray-700 bg-gray-100 px-2 py-0.5 rounded">{id}</span>
                        </p>
                    </div>
                </div>
            </Card>

            {/* File Tree */}
            <Card className="border-t-4 border-t-blue-500">
                <div className="px-6 py-4 border-b border-gray-100 bg-white">
                    <h2 className="text-base font-bold text-gray-800 flex items-center gap-2">
                        <Folder className="w-4 h-4 text-green-500"/>
                        File Explorer
                    </h2>
                </div>
                
                <div className="p-4 bg-white min-h-[400px]">
                    {fileTree.length === 0 ? (
                        <div className="text-center py-10 text-gray-400 text-sm">
                            No files found in this branch.
                        </div>
                    ) : (
                        <div className="space-y-0.5">
                            {fileTree.map((node, idx) => (
                                <FileTreeItem 
                                    key={node.path || idx} 
                                    node={node}
                                    level={0}
                                />
                            ))}
                        </div>
                    )}
                </div>
            </Card>
        </section>
    );
};

export default BranchFilesPage;
