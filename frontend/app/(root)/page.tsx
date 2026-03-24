import HeaderBox from '@/components/HeaderBox'
import RecentActivity from '@/components/RecentActivity'
import RightSidebar from '@/components/RightSidebar'
import React from 'react'

const Home = async ({ searchParams: { id, page } }: SearchParamProps) => {
  // const loggedIn = await getLoggedInUser();
  const loggedIn = { 
    $id: '1',
    email: 'mateusz.zajdel@cilab.com',
    firstName: 'Mateusz',
    lastName: 'Zajdel',
    name: 'Mateusz Zajdel',
    accessGroups: ['Developers', 'Admin', 'CI/CD Team']
  }
  const currentPage = Number(page) || 1

  return (
    <div>
      <section className='home'>
        <div className='home-content'>
          <header className='home-header'>
            <HeaderBox 
              type="greeting"
              title="Welcome"
              user={ loggedIn?.name || 'Guest' }
              subtext='Access and manage your projects easily.'
            />

          </header>

          <RecentActivity />
        </div>

        <RightSidebar 
          user={ loggedIn }
          transactions={ [] }
          banks={ [] }
        />
      </section>
    </div>
  )
}

export default Home;
