import Link from 'next/link'
import React from 'react'
import Image from 'next/image'

const RightSidebar = ({ user, transactions, banks }: RightSidebarProps) => {
  return (
    <aside className='right-sidebar'>
      <section className='flex flex-col pb-8'>
        <div className='profile-banner' />

        <div className='profile'>
          <div className='profile-img'> 
            <span className='text-5xl font-bold text-blue-500'>
              {user?.name[0]}
            </span>
          </div>

          <div className='profile-details'>
            <h1 className='profile-name'>
              {user?.name}
            </h1>

            <p className='profile-email'>
              {user?.email}
            </p>
          </div>
        </div>
      </section>

      <section className='banks'>
        <div className='flex w-full justify-between'>
          <h2 className='header-2'>
            My groups
          </h2>

          <Link href='/' className='flex gap-2'>
            <Image 
              src='/icons/plus.svg'
              width={20}
              height={20}
              alt='plus'
            />

            <h2 className='text-14 font-semibold text-gray-500'>
              Add group
            </h2>
          </Link>
        </div>

        {user?.accessGroups && user.accessGroups.length > 0 ? (
          <div className='flex flex-col gap-2 mt-5'>
            {user.accessGroups.map((group, index) => (
              <div 
                key={index}
                className='flex items-center gap-3 px-4 py-3 rounded-lg bg-gradient-to-r from-blue-50 to-indigo-50 border border-blue-100 hover:border-blue-300 transition-all cursor-pointer'
              >
                <div className='flex items-center justify-center w-10 h-10 rounded-full bg-blue-500 text-white font-semibold'>
                  {group.charAt(0).toUpperCase()}
                </div>
                <div className='flex-1'>
                  <p className='text-14 font-semibold text-gray-800'>
                    {group}
                  </p>
                </div>
              </div>
            ))}
          </div>
        ) : (
          <p className='text-14 text-gray-500 mt-5'>
            No groups assigned yet
          </p>
        )}
      </section>
    </aside>
  )
}

export default RightSidebar
