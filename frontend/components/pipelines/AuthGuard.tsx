"use client"

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { Loader2 } from 'lucide-react';
import { jwtDecode } from 'jwt-decode';

interface DecodedToken {
    exp?: number;
}

export default function AuthGuard({ children }: { children: React.ReactNode }) {
    const router = useRouter();
    const [isChecking, setIsChecking] = useState(true);
    const [isAuthorized, setIsAuthorized] = useState(false);

    useEffect(() => {
        const token = localStorage.getItem('access_token');

        if (!token) {
            setIsAuthorized(false);
            setIsChecking(false);
            router.replace('/sign-in');
            return;
        }

        try {
            const decoded = jwtDecode<DecodedToken>(token);
            const isExpired = !!decoded.exp && decoded.exp * 1000 <= Date.now();

            if (isExpired) {
                localStorage.removeItem('access_token');
                localStorage.removeItem('refresh_token');
                setIsAuthorized(false);
                router.replace('/sign-in');
            } else {
                setIsAuthorized(true);
            }
        } catch (error) {
            console.error('Invalid access token:', error);
            localStorage.removeItem('access_token');
            localStorage.removeItem('refresh_token');
            setIsAuthorized(false);
            router.replace('/sign-in');
        } finally {
            setIsChecking(false);
        }
    }, [router]);

    if (isChecking) {
        return (
            <div className="flex h-screen w-full items-center justify-center">
                <Loader2 className="h-8 w-8 animate-spin text-gray-500" />
            </div>
        );
    }

    if (!isAuthorized) {
        return null;
    }

    return <>{children}</>;
}