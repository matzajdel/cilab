import { cn } from '@/lib/utils';
import { Loader2, CheckCircle2, XCircle, Clock } from "lucide-react";

const StatusBadge = ({ status }: { status: string }) => {
    const styles: Record<string, string> = {
        SUCCESSFUL: "bg-green-100 text-green-700 border-green-200",
        FAILED: "bg-red-100 text-red-700 border-red-200",
        IN_PROGRESS: "bg-blue-100 text-blue-700 border-blue-200",
        PENDING: "bg-yellow-100 text-yellow-700 border-yellow-200",
    };

    const icon: Record<string, React.ReactNode> = {
        SUCCESSFUL: <CheckCircle2 className="w-4 h-4 mr-1" />,
        FAILED: <XCircle className="w-4 h-4 mr-1" />,
        IN_PROGRESS: <Loader2 className="w-4 h-4 mr-1 animate-spin" />,
        PENDING: <Clock className="w-4 h-4 mr-1" />,
    };

    const key = status as string;

    return (
        <span className={cn("inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium border", styles[key] || "bg-gray-100 text-gray-700 border-gray-200")}>
            {icon[key]}
            {status}
        </span>
    );
};

export default StatusBadge;