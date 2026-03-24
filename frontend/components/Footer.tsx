import Image from 'next/image'
import { useRouter } from 'next/navigation'
import React, { use } from 'react'

const Footer = ({ user, type="desktop" }: FooterProps) => {
  const router = useRouter()
  
  const handleLogout = async () => {
      try {
          const refreshToken = localStorage.getItem('refresh_token');

          if (refreshToken) {
              const params = new URLSearchParams();
              params.append('client_id', 'cilab-client');
              params.append('refresh_token', refreshToken);

              await fetch('http://localhost:8085/realms/cilab-realm/protocol/openid-connect/logout', {
                  method: 'POST',
                  headers: {
                      'Content-Type': 'application/x-www-form-urlencoded',
                  },
                  body: params,
              });
          }
      } catch (error) {
          console.error("Error while logging out:", error);
      } finally {
          localStorage.removeItem('access_token');
          localStorage.removeItem('refresh_token');

          window.location.href = '/sign-in';
      }
  }

  return (
    <footer className='footer'>
      <div className={type === "mobile" ? "footer-name-mobile" : "footer_name"}>
        <p className='text-xl font-bold text-gray-700'>
          {user?.name[0]}
        </p>
      </div>

      <div className={type === 'mobile' ? 'footer_email-mobile' : 'footer_email'}>
        <h1 className='text-14 truncate font-normal text-gray-600'>
          {user?.name}
        </h1>
        <p className='text-14 truncate font-normal text-gray-700'>
          {user?.email}
        </p>
      </div>

      <div className='footer_image' onClick={ handleLogout }>
        <Image src='icons/logout.svg' fill alt='jsm'/>
      </div>
    </footer>
  )
}

export default Footer
