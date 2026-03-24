"use client";

import { useRouter } from 'next/navigation'
import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs";
import CommitsTab from "@/components/vcs/CommitsTab";
import BranchesTab from "@/components/vcs/BranchesTab";

interface VcsTabsProps {
  commits: Commit[];
  branches: Branch[];
}

const VcsTabs = ({ commits, branches }: VcsTabsProps) => {
    const router = useRouter();
    return (
        <Tabs defaultValue="commits" className="w-full">
        
            <div className="flex items-center justify-between mb-4">
                <h2 className="header-2">Repository Details</h2>
                <TabsList>
                <TabsTrigger value="commits">Commits</TabsTrigger>
                <TabsTrigger value="branches">Branches</TabsTrigger>
                </TabsList>
            </div>

            <CommitsTab commits={commits} />  

            <BranchesTab branches={branches }/>
        
        </Tabs>
    );
}

export default VcsTabs;