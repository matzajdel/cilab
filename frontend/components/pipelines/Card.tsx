import { cn } from '@/lib/utils';

const Card = ({ children, className }: { children: React.ReactNode, className?: string }) => (
    <div className={cn("bg-white rounded-lg shadow-sm border border-gray-200/60 overflow-hidden", className)}>
        {children}
    </div>
);

export default Card;