import { categoryStyles } from '@/constants';
import { cn } from '@/lib/utils';


const CategoryBadge = ({ category }: CategoryBadgeProps) => {
    const {
        borderColor,
        backgroundColor,
        textColor,
        chipBackgroundColor
    } = categoryStyles[category as keyof typeof categoryStyles] || categoryStyles.default;
    
    return (
        <div className={cn('category-badge', borderColor, chipBackgroundColor)}>
            <div className={cn('size-2 rounded-full', backgroundColor)}/>
            <p className={cn('text-[12px] font-medium', textColor)}>{category}</p>
        </div>
    )
}

export default CategoryBadge;