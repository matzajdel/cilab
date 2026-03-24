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
import api from '@/lib/api';


const PipelineTable: React.FC<PipelineTableProps> = ({ filter = "" }) => {
  const router = useRouter();
  const [pipelines, setPipelines] = useState<Pipeline[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchPipelines = async () => {
      try {
        const response = await api.get(`/api/v1/pipelines`);
        console.log(response)
        setPipelines(response.data);
      } catch (error) {
        console.error('Error fetching pipelines:', error);
      } finally {
        setLoading(false);
      }
    };

    fetchPipelines();
  }, []);

  const filteredPipelines = pipelines.filter(t => 
    t.name.toLowerCase().includes(filter.toLowerCase())
  );

  if (loading) {
    return <div>Loading...</div>;
  }

  return (
    <Table>
        <TableHeader className='bg-[#f9fafb]'>
            <TableRow>
                <TableHead className='px-2'>Pipeline Name</TableHead>
                <TableHead className='px-2'>Last Updated</TableHead>
                <TableHead className='px-2 max-md:hidden'>Created By</TableHead>
            </TableRow>
        </TableHeader>
        <TableBody>
            {filteredPipelines.map((t) => (
                <TableRow 
                    key={t.id}
                    onClick={() => router.push(`/pipelines/${t.id}`)}
                    className="cursor-pointer hover:bg-blue-100 bg-blue-50"
                >
                    <TableCell className='max-w-[250px] pl-2 pr-10'>
                        <div className='flex items-center gap-3'>
                            <h1 className="text-14 truncate font-semibold text-[#344054]">
                                {t.name}
                            </h1>
                        </div>
                    </TableCell>

                    <TableCell className='pl-2 pr-10'>
                        {new Date(t.lastUpdated).toLocaleDateString()}
                    </TableCell>

                    <TableCell className="pl-2 pr-10 max-md:hidden min-w-24">
                        {t.authorEmail}
                    </TableCell>

                </TableRow>
            ))}
        </TableBody>
    </Table>
  )
}

export default PipelineTable
