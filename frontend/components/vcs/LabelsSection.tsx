import { useEffect, useMemo, useState } from 'react';
import { CheckCircle, XCircle, MinusCircle } from "lucide-react";
import {
    Dialog,
    DialogContent,
    DialogDescription,
    DialogHeader,
    DialogTitle,
    DialogTrigger,
} from "@/components/ui/dialog"
import { Button } from "@/components/ui/button";
import api from '@/lib/api';

const LabelsSection = ({ labels }: LabelProps) => {
    const [codeReview, setCodeReview] = useState<number>(0);
    const [isVerified, setIsVerified] = useState(false);
    const [isDialogOpen, setIsDialogOpen] = useState(false);
    const [isUpdating, setIsUpdating] = useState(false);

    const normalizedLabels = useMemo(() => {
        if (!labels) {
            return [] as Label[];
        }

        return Array.isArray(labels) ? labels : [labels];
    }, [labels]);

    const initialVerified = useMemo(() => {
        const verifiedLabel = normalizedLabels.find((label) =>
            label.name.toLowerCase() === 'verified'
        );

        return !!verifiedLabel && verifiedLabel.value > 0;
    }, [normalizedLabels]);

    const initialCodeReview = useMemo(() => {
        const codeReviewLabel = normalizedLabels.find((label) => {
            const normalizedName = label.name.toLowerCase();
            return normalizedName === 'code-review' || normalizedName === 'code_review' || normalizedName === 'codereview';
        });

        if (!codeReviewLabel) {
            return 0;
        }

        if (codeReviewLabel.value > 0) {
            return 1;
        }

        if (codeReviewLabel.value < 0) {
            return -1;
        }

        return 0;
    }, [normalizedLabels]);

    const labelMetadata = useMemo(() => {
        const metadataSource = normalizedLabels.find((label) => label.commitId);

        return {
            commitId: metadataSource?.commitId || '',
        };
    }, [normalizedLabels]);

    useEffect(() => {
        setCodeReview(initialCodeReview);
    }, [initialCodeReview]);

    useEffect(() => {
        setIsVerified(initialVerified);
    }, [initialVerified]);

    const sendLabelUpdate = async (name: string, value: number) => {
        if (!labelMetadata.commitId) {
            console.warn('Cannot update label: missing commitId in labels payload');
            return;
        }

        const payload: LabelDTO = {
            name,
            value,
            commitId: labelMetadata.commitId,
        };

        await api.post('/api/v1/vcs/labels', payload);
    };

    const handleCodeReviewChange = async (newCodeReview: number) => {
        setCodeReview(newCodeReview);
        setIsDialogOpen(false);

        try {
            setIsUpdating(true);
            await sendLabelUpdate('Code-Review', newCodeReview * 2);
        } catch (error) {
            console.error('Failed to update code review label:', error);
            setCodeReview(initialCodeReview);
        } finally {
            setIsUpdating(false);
        }
    };

    const handleVerifiedToggle = async () => {
        const nextVerified = !isVerified;
        setIsVerified(nextVerified);

        try {
            setIsUpdating(true);
            await sendLabelUpdate('Verified', nextVerified ? 1 : 0);
        } catch (error) {
            console.error('Failed to update verified label:', error);
            setIsVerified(initialVerified);
        } finally {
            setIsUpdating(false);
        }
    };

    return (
        <div className="flex items-center gap-6 p-4 bg-white rounded-lg border border-gray-200 shadow-sm mt-4">
            <div className="flex items-center gap-2">
                <span className="text-sm font-medium text-gray-500">Verified:</span>
                <button
                    type="button"
                    onClick={handleVerifiedToggle}
                    disabled={isUpdating}
                    className="focus:outline-none"
                >
                    {isVerified ? (
                        <span className="inline-flex items-center gap-1 rounded-full bg-green-100 px-2.5 py-0.5 text-xs font-medium text-green-800 hover:bg-green-200 cursor-pointer transition-colors">
                            <CheckCircle className="h-3 w-3" />
                            Verified
                        </span>
                    ) : (
                        <span className="inline-flex items-center gap-1 rounded-full bg-gray-100 px-2.5 py-0.5 text-xs font-medium text-gray-800 hover:bg-gray-200 cursor-pointer transition-colors">
                            Not Verified
                        </span>
                    )}
                </button>
            </div>

            <div className="h-4 w-px bg-gray-300"></div>

            <div className="flex items-center gap-2">
                <span className="text-sm font-medium text-gray-500">Code Review:</span>
                <Dialog open={isDialogOpen} onOpenChange={setIsDialogOpen}>
                    <DialogTrigger asChild>
                        <button className="focus:outline-none" disabled={isUpdating}>
                            {codeReview === 1 ? (
                                <span className="inline-flex items-center gap-1 rounded-full bg-green-100 px-2.5 py-0.5 text-xs font-medium text-green-800 hover:bg-green-200 cursor-pointer transition-colors">
                                    <CheckCircle className="h-3 w-3" />
                                    +2
                                </span>
                            ) : codeReview === -1 ? (
                                <span className="inline-flex items-center gap-1 rounded-full bg-red-100 px-2.5 py-0.5 text-xs font-medium text-red-800 hover:bg-red-200 cursor-pointer transition-colors">
                                    <XCircle className="h-3 w-3" />
                                    -2
                                </span>
                            ) : (
                                <span className="inline-flex items-center gap-1 rounded-full bg-gray-100 px-2.5 py-0.5 text-xs font-medium text-gray-600 hover:bg-gray-200 cursor-pointer transition-colors">
                                    <MinusCircle className="h-3 w-3" />
                                    0
                                </span>
                            )}
                        </button>
                    </DialogTrigger>
                    <DialogContent className="sm:max-w-md bg-white">
                        <DialogHeader>
                            <DialogTitle>Set Code Review Score</DialogTitle>
                            <DialogDescription>
                                Select a score for the code review. This will update the status of the commit.
                            </DialogDescription>
                        </DialogHeader>
                        <div className="flex items-center justify-center space-x-4 py-4">
                            <Button
                                variant="outline"
                                className={`flex flex-col items-center gap-2 h-24 w-24 border-2 ${codeReview === -1 ? 'border-red-500 bg-red-50' : 'border-gray-200 hover:bg-gray-50 hover:border-red-200'}`}
                                onClick={() => handleCodeReviewChange(-1)}
                                disabled={isUpdating}
                            >
                                <XCircle className={`h-8 w-8 ${codeReview === -1 ? 'text-red-500' : 'text-gray-400'}`} />
                                <span className="font-semibold text-lg">-2</span>
                            </Button>
                            <Button
                                variant="outline"
                                className={`flex flex-col items-center gap-2 h-24 w-24 border-2 ${codeReview === 0 ? 'border-gray-500 bg-gray-50' : 'border-gray-200 hover:bg-gray-50 hover:border-gray-300'}`}
                                onClick={() => handleCodeReviewChange(0)}
                                disabled={isUpdating}
                            >
                                <MinusCircle className={`h-8 w-8 ${codeReview === 0 ? 'text-gray-600' : 'text-gray-400'}`} />
                                <span className="font-semibold text-lg">0</span>
                            </Button>
                            <Button
                                variant="outline"
                                className={`flex flex-col items-center gap-2 h-24 w-24 border-2 ${codeReview === 1 ? 'border-green-500 bg-green-50' : 'border-gray-200 hover:bg-gray-50 hover:border-green-200'}`}
                                onClick={() => handleCodeReviewChange(1)}
                                disabled={isUpdating}
                            >
                                <CheckCircle className={`h-8 w-8 ${codeReview === 1 ? 'text-green-500' : 'text-gray-400'}`} />
                                <span className="font-semibold text-lg">+2</span>
                            </Button>
                        </div>
                    </DialogContent>
                </Dialog>
            </div>
        </div>
    )
}

export default LabelsSection;