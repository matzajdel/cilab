"use client"

import HeaderBox from '@/components/HeaderBox'
import RepositoryTable from '@/components/vcs/RepositoryTable'
import { Input } from '@/components/ui/input'
import React, { useState } from 'react'

const RepositoryMainPanel = ({ searchParams: { id, page } }: SearchParamProps) => {
  const [searchTerm, setSearchTerm] = useState('')

  return (
      <div className='transactions'>

        <div className='transactions-header'>
          <HeaderBox 
            title="Repository browser"
            subtext="Search your repository"
          />
        </div>

        <div className='w-full max-w-sm mb-6'>
            <Input 
                placeholder="Search repository..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
            />
        </div>
        
        <div className='space-y-6'>
          <section className='flex flex-col w-full gap-6'>
            <RepositoryTable filter={searchTerm} />
          </section>
        </div>
      </div>
  )
}

export default RepositoryMainPanel
