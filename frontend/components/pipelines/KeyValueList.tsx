import Card from "./Card";

const KeyValueList = ({ data, title, icon: Icon }: { data: KeyValueMap, title: string, icon?: any }) => {
    if (!data || Object.keys(data).length === 0) return null;

    return (
        <Card className="mb-4">
            <div className="bg-gray-50/50 px-4 py-3 border-b border-gray-100 flex items-center gap-2">
                {Icon && <Icon className="w-4 h-4 text-gray-500" />}
                <h3 className="text-sm font-semibold text-gray-700">{title}</h3>
            </div>
            <div className="p-4">
                <div className="grid grid-cols-1 gap-2">
                    {Object.entries(data).map(([key, value]) => (
                        <div key={key} className="flex justify-between text-sm py-1 border-b border-dashed border-gray-100 last:border-0 last:pb-0">
                            <span className="font-mono text-xs text-gray-500">{key}</span>
                            <span className="font-mono text-xs font-medium text-gray-800 truncate pl-4 max-w-[200px]" title={String(value)}>{value}</span>
                        </div>
                    ))}
                </div>
            </div>
        </Card>
    );
};

export default KeyValueList;