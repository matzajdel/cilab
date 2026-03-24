"use client"

import Sidebar from "@/components/Sidebar";
import Image from "next/image";
import MobileNav from "@/components/MobileNav";
import AuthGuard from "@/components/pipelines/AuthGuard";
import { useState, useEffect } from "react";
import { jwtDecode } from 'jwt-decode'

interface KeycloakToken {
  sub: string;
  given_name?: string;
  family_name?: string;
  name?: string;
  email?: string;
  preferred_username: string;
}

interface LoggedUser {
  $id: string;
  firstName: string;
  lastName: string;
  name: string;
  email: string;
}

export default function RootLayout({ children }: { children: React.ReactNode }) {
  const [loggedIn, setLoggedIn] = useState<LoggedUser | null>(null);
  const [isProfileLoading, setIsProfileLoading] = useState(true);

  useEffect(() => {
    const token = localStorage.getItem('access_token');

    if (!token) {
      setIsProfileLoading(false);
      return;
    }

    try {
      const decoded = jwtDecode<KeycloakToken>(token);

      setLoggedIn({
        $id: decoded.sub,
        firstName: decoded.given_name || decoded.preferred_username,
        lastName: decoded.family_name || '',
        name: decoded.name || decoded.preferred_username,
        email: decoded.email || '',
      });
    } catch (error) {
      console.error("Error loading token: ", error);
      localStorage.removeItem('access_token');
      localStorage.removeItem('refresh_token');
    } finally {
      setIsProfileLoading(false);
    }
  }, []);

  if (isProfileLoading) {
    return <div className="flex h-screen items-center justify-center">Ładowanie profilu...</div>;
  }

  if (!loggedIn) {
    return <AuthGuard>{children}</AuthGuard>;
  }

  return (
    <AuthGuard>
      <main className="flex h-screen w-full font-inter">
        <Sidebar user={loggedIn} />

        <div className="flex size-full flex-col">
          <div className="root-layout">
            <Image src="/icons/logo.svg" width={30} height={30} alt="logo" />
            <div>
              <MobileNav user={loggedIn} />
            </div>
          </div>
          {children}
        </div>
      </main>
    </AuthGuard>
  );
}