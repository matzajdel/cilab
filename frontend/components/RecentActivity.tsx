'use client'

import React, { useEffect, useState } from 'react'
import Link from 'next/link'
import { Tabs, TabsContent, TabsList } from "@/components/ui/tabs"
import PipelineTable from './pipelines/PipelineTable'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import CategoryBadge from "@/components/ui/CategoryBadge"
import { useRouter } from 'next/navigation'
import { Loader2 } from 'lucide-react'
import { API_BASE_URL } from '@/lib/config';
import { cn } from '@/lib/utils'
import CommitsTab from './vcs/CommitsTab'
import PipelineRunsTable from './pipelines/PipelineRunsTable'
import api from '@/lib/api'

const RecentActivity = () => {
  const router = useRouter();
  const [commits, setCommits] = useState<any[]>([]);
  const [runs, setRuns] = useState<any[]>([]);

  const [loading, setLoading] = useState(true);
  const [activeTab, setActiveTab] = useState('pipelines');

  useEffect(() => {
    const fetchLastCommits = async () => {
      try {
        const response = await api.get(`/api/v1/vcs/commits?authorEmail=sid_cilab%40cilab.com`);
        const data = response.data;
        console.log(data)
        setCommits(data.slice(0, 6)); // Show only last 5 commits
      } catch (error) {
        console.error('Error fetching commits:', error);
      } finally {
        setLoading(false);
      }
    };

    const fetchLastRuns = async () => {
      try {
        const response = await api.get(`/api/v1/runs?authorEmail=mateusz.zajdel%40cilab.com`);
        const data = response.data;
        setRuns(data.slice(0, 6)); // Show only last 5 commits
      } catch (error) {
        console.error('Error fetching commits:', error);
      } finally {
        setLoading(false);
      }
    };

    fetchLastCommits();
    fetchLastRuns();

  }, []);

  return (
    <section className='recent-transactions'>
        <header className='flex items-center justify-between'>
            <h2 className='recent-transactions-label'>
                Recent Activity
            </h2>
        </header>

        <Tabs value={activeTab} onValueChange={setActiveTab} className="w-full">
            <TabsList className='recent-transactions-tablist'>
                <div
                  onClick={() => setActiveTab('pipelines')}
                  className={cn(`banktab-item cursor-pointer`, {
                    " border-blue-600": activeTab === 'pipelines',
                  })}
                >
                  <p
                    className={cn(`text-16 line-clamp-1 flex-1 font-medium text-gray-500`, {
                      " text-blue-600": activeTab === 'pipelines',
                    })}
                  >
                    Pipelines
                  </p>
                </div>
                
                <div
                  onClick={() => setActiveTab('changes')}
                  className={cn(`banktab-item cursor-pointer`, {
                    " border-blue-600": activeTab === 'changes',
                  })}
                >
                  <p
                    className={cn(`text-16 line-clamp-1 flex-1 font-medium text-gray-500`, {
                      " text-blue-600": activeTab === 'changes',
                    })}
                  >
                    Changes
                  </p>
                </div>
            </TabsList>

            <TabsContent value="pipelines" className="space-y-4">
                <PipelineRunsTable pipelineRuns={runs} />
            </TabsContent>

            <TabsContent value="changes" className="space-y-4">
                <CommitsTab commits={commits} />
            </TabsContent>
        </Tabs>
    </section>
  )
}

export default RecentActivity
