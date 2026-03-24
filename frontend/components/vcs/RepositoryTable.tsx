"use client"

import React, { useEffect, useState } from 'react'
import { useRouter } from 'next/navigation'
import { API_BASE_URL } from '@/lib/config';
import {
  Table,
  TableBody,
  TableCaption,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table"
import { cn } from '@/lib/utils';
import { categoryStyles } from '@/constants';
import api from '@/lib/api';

interface Repository {
  id: string;
  name: string;
}

const CategoryBadge = ({ category }: { category: string }) => {
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

interface RepositoryTableProps {
  filter?: string;
}

const RepositoryTable: React.FC<RepositoryTableProps> = ({ filter = "" }) => {
  const router = useRouter();
  const [repos, setRepos] = useState<Repository[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchPipelines = async () => {
      try {
        const response = await api.get(`/api/v1/vcs/repos`);
        console.log(response);
        const data = response.data;
        setRepos(data);
      } catch (error) {
        console.error('Error fetching repositories:', error);
      } finally {
        setLoading(false);
      }
    };

    fetchPipelines();
  }, []);

  const filteredPipelines = repos.filter(t => 
    t.name.toLowerCase().includes(filter.toLowerCase())
  );

  if (loading) {
    return <div>Loading...</div>;
  }

  return (
    <Table>
        <TableHeader className='bg-[#f9fafb]'>
            <TableRow>
                <TableHead className='px-2'>Repository Name</TableHead>
            </TableRow>
        </TableHeader>
        <TableBody>
            {filteredPipelines.map((t) => (
                <TableRow 
                    key={t.id}
                    onClick={() => router.push(`/version-control-system/${t.id}`)}
                    className="cursor-pointer hover:bg-blue-100 bg-blue-50"
                >
                    <TableCell className='max-w-[250px] pl-2 pr-10'>
                        <div className='flex items-center gap-3'>
                            <h1 className="text-14 truncate font-semibold text-[#344054]">
                                {t.name}
                            </h1>
                        </div>
                    </TableCell>

                </TableRow>
            ))}
        </TableBody>
    </Table>
  )
}

export default RepositoryTable
