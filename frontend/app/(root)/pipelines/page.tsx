"use client"

import HeaderBox from '@/components/HeaderBox'
import PipelineTable from '@/components/pipelines/PipelineTable'
import { Input } from '@/components/ui/input'
import React, { useState } from 'react'

const TransactionHistory = ({ searchParams: { id, page } }: SearchParamProps) => {
  const [searchTerm, setSearchTerm] = useState('')

  return (
      <div className='transactions'>
        <div className='transactions-header'>
          <HeaderBox 
            title="Pipeline browser"
            subtext="Search your pipeline"
          />
        </div>

        <div className='w-full max-w-sm mb-6'>
            <Input 
                placeholder="Search pipeline..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
            />
        </div>
        
        <div className='space-y-6'>

          <section className='flex flex-col w-full gap-6'>
            <PipelineTable filter={searchTerm} />
          </section>

        </div>
      </div>
  )
}

export default TransactionHistory
