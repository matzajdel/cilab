import {
    Dialog,
    DialogContent,
    DialogDescription,
    DialogHeader,
    DialogTitle,
} from "@/components/ui/dialog"
import { GitMerge, Send, CheckCircle, XCircle, MinusCircle, FileCode, MessageSquare, User, Loader2 } from "lucide-react";

const FileContent = ({ selectedFileName, selectedFileContent, isFileLoading, isFileViewerOpen, setIsFileViewerOpen  }) => {
    return (
        <Dialog open={isFileViewerOpen} onOpenChange={setIsFileViewerOpen}>
            <DialogContent className="max-w-4xl h-[80vh] flex flex-col bg-white">
                <DialogHeader>
                    <DialogTitle className="flex items-center gap-2 font-mono text-sm">
                        <FileCode className="h-5 w-5 text-blue-600" />
                        {selectedFileName}
                    </DialogTitle>
                    <DialogDescription>
                        Viewing file content at this commit.
                    </DialogDescription>
                </DialogHeader>
                <div className="flex-1 overflow-hidden rounded-md border border-gray-200 bg-gray-50">
                    {isFileLoading ? (
                        <div className="flex h-full items-center justify-center">
                            <Loader2 className="h-8 w-8 animate-spin text-blue-600" />
                        </div>
                    ) : (
                        <pre className="h-full w-full overflow-auto p-4 text-sm font-mono text-gray-800">
                            {selectedFileContent || "No content available."}
                        </pre>
                    )}
                </div>
            </DialogContent>
        </Dialog>
    )
}

export default FileContent;