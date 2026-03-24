import { useState } from "react";
import { Button } from "@/components/ui/button";
import api from "@/lib/api";
import { GitMerge, Send } from "lucide-react";
import axios from "axios";
import {
    Dialog,
    DialogContent,
    DialogDescription,
    DialogHeader,
    DialogTitle,
} from "@/components/ui/dialog";

const CommitInfo = ({ commitInfo }: CommitInfoProps) => {
    const [submitError, setSubmitError] = useState<string | null>(null);
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [submitRejectedMessage, setSubmitRejectedMessage] = useState<string>("");
    const [isSubmitRejectedOpen, setIsSubmitRejectedOpen] = useState(false);

    const handleRebase = async () => {
        try {
            await api.post(`/api/v1/vcs/rebase/${commitInfo?.id}`);
        } catch (error) {
            console.error('Rebase failed:', error);
        }
    };

    const handleSubmit = async () => {
        if (!commitInfo?.id) {
            return;
        }

        try {
            setSubmitError(null);
            setIsSubmitting(true);
            await api.post(`/api/v1/vcs/submit/${commitInfo.id}`);
        } catch (error) {
            if (axios.isAxiosError(error) && error.response?.status === 422) {
                const backendMessage = typeof error.response.data === 'string'
                    ? error.response.data
                    : 'Submit rejected by validation rules.';

                setSubmitRejectedMessage(backendMessage);
                setIsSubmitRejectedOpen(true);
                return;
            }

            setSubmitError('Submit failed. Please try again.');
            console.error('Submit failed:', error);
        } finally {
            setIsSubmitting(false);
        }
    };

    return (
        <div className='transactions-account'>
            <div className='flex flex-col gap-2'>
                <h2 className='text-18 text-white font-bold  max-w-xl break-words'>
                    {commitInfo?.message || "Loading..."}
                </h2>

                <p className='text-14 text-blue-25 font-mono'>
                    {commitInfo?.id}
                </p>

                <p className='text-14 font-semibold tracking-[1.1px] text-white'>
                    Author: {commitInfo?.authorEmail}
                </p>

                {submitError && (
                    <p className='text-13 font-medium text-red-200'>
                        {submitError}
                    </p>
                )}
            </div>


            <div className="flex gap-4">
                <Button 
                    className='flex items-center gap-2 bg-purple-600 hover:bg-purple-700 text-white px-6 py-2 rounded-lg'
                    onClick={handleRebase}
                    disabled={!commitInfo?.id}
                >
                    <GitMerge className="size-5" />
                    <span className='text-14 font-medium'>Rebase</span>
                </Button>

                <Button 
                    className='flex items-center gap-2 bg-blue-600 hover:bg-blue-700 text-white px-6 py-2 rounded-lg'
                    onClick={handleSubmit}
                    disabled={!commitInfo?.id || isSubmitting}
                >
                    <Send className="size-5" />
                        <span className='text-14 font-medium'>{isSubmitting ? 'Submitting...' : 'Submit'}</span>
                </Button>
            </div>

            <Dialog open={isSubmitRejectedOpen} onOpenChange={setIsSubmitRejectedOpen}>
                <DialogContent className="sm:max-w-md bg-white">
                    <DialogHeader>
                        <DialogTitle>Submit Rejected</DialogTitle>
                        <DialogDescription>
                            This commit cannot be submitted right now.
                        </DialogDescription>
                    </DialogHeader>

                    <div className="mt-2 rounded-md border border-red-200 bg-red-50 p-3 text-sm text-red-700">
                        Submit rejected by validation rules.
                    </div>

                    <div className="mt-4 flex justify-end">
                        <Button type="button" onClick={() => setIsSubmitRejectedOpen(false)}>
                            OK
                        </Button>
                    </div>
                </DialogContent>
            </Dialog>
        </div>
    )
}

export default CommitInfo;