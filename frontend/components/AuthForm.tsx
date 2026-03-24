"use client"

import React, { useState } from 'react'
import Image from 'next/image'
import { useRouter } from 'next/navigation'

import { zodResolver } from "@hookform/resolvers/zod"
import { useForm } from "react-hook-form"
import { z } from "zod"

import { Button } from "@/components/ui/button"
import { Form } from "@/components/ui/form"
import CustomInput from './CustomInput'
import { Loader2 } from 'lucide-react'


const formSchema = z.object({
  email: z.string().min(1, { message: "Username/Email is required." }), 
  password: z.string().min(1, { message: "Password is required." })
})

const AuthForm = () => {
    const router = useRouter();
    const [isLoading, setIsLoading] = useState(false);
    const [error, setError] = useState('');

    const form = useForm<z.infer<typeof formSchema>>({
        resolver: zodResolver(formSchema),
        defaultValues: {
            email: "",
            password: ""
        },
    })
    
    const onSubmit = async (data: z.infer<typeof formSchema>) => {
        setIsLoading(true);
        setError('');

        try {
            const params = new URLSearchParams();
            params.append('client_id', 'cilab-client');
            params.append('grant_type', 'password');
            params.append('username', data.email); 
            params.append('password', data.password);

            const response = await fetch('http://localhost:8085/realms/cilab-realm/protocol/openid-connect/token', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded',
                },
                body: params,
            });

            if (!response.ok) {
                throw new Error('Invalid email or password');
            }

            const tokenData = await response.json();
            
            localStorage.setItem('access_token', tokenData.access_token);
            localStorage.setItem('refresh_token', tokenData.refresh_token);

            router.push('/');
            
        } catch (err: any) {
            console.error("Login failed:", err);
            setError(err.message || "An error occurred during sign in.");
        } finally {
            setIsLoading(false);
        }
    }

    return (
        <section className='auth-form'>
            <header className='flex flex-col gap-5 md:gap-6'>
                <div className='flex items-center gap-1'>
                    <Image
                        src='/icons/logo.svg'
                        width={34}
                        height={34}
                        alt='CILab logo'
                    />
                    <h1 className='text-26 font-ibm-plex-serif font-bold text-black-1'>
                        CILab
                    </h1>
                </div>

                <div className='flex flex-col gap-1 md:gap-3'>
                    <h1 className='text-24 lg:text-36 font-semibold text-gray-900'>
                        Sign In
                    </h1>
                    <p className='text-16 font-normal text-gray-600'>
                        Enter your details to access the system
                    </p>
                </div>
            </header>

            <Form {...form}>
                <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-8">
                    
                    <CustomInput control={form.control} name="email" label="Email / Username" placeholder="Enter your email" />
                    <CustomInput control={form.control} name="password" label="Password" placeholder="Enter your password" />

                    {/* Wrong password message */}
                    {error && <p className="text-red-500 text-sm font-medium">{error}</p>}

                    <div className='flex flex-col gap-4'>
                        <Button type="submit" className='form-btn' disabled={isLoading}>
                            {isLoading ? (
                                <>
                                    <Loader2 size={20} className='animate-spin' /> &nbsp;
                                    Loading...
                                </>
                            ) : (
                                'Sign In'
                            )}
                        </Button>
                    </div>
                </form>
            </Form>
        </section>
    )
}

export default AuthForm


// const AuthForm = ({ type }: { type: string }) => {
//     const router = useRouter();
//     const [user, setUser] = useState(null);
//     const [isLoading, setIsLoading] = useState(false);

//     const formSchema = authFormSchema(type);

//     // 1. Define your form.
//     const form = useForm<z.infer<typeof formSchema>>({
//         resolver: zodResolver(formSchema),
//         defaultValues: {
//             email: "",
//             password: ""
//         },
//     })
    
//     // 2. Define a submit handler.
//     const onSubmit =  async (data: z.infer<typeof formSchema>) => {
//         setIsLoading(true)

//         try {
//             if (type === 'sign-up') {
//                 const userData = {
//                     firstName: data.firstName!,
//                     lastName: data.lastName!,
//                     address1: data.address1!,
//                     city: data.city!,
//                     state: data.state!,
//                     postalCode: data.postalCode!,
//                     dateOfBirth: data.dateOfBirth!,
//                     ssn: data.ssn!,
//                     email: data.email,
//                     password: data.password
//                 }

//                 const newUser = await signUp(userData)

//                 setUser(newUser);
//             }

//             if (type === 'sign-in') {
//                 const response = await signIn({
//                     email: data.email,
//                     password: data.password
//                 })

//                 if (response) {
//                     router.push('/')
//                     setUser(response)
//                 }
//             }
//         } catch (err) {
//             console.log(err)
//         } finally {
//             setIsLoading(false)
//         }
//     }

//     return (
//     <section className='auth-form'>
//         <header className='flex flex-col gap-5 md:gap-6'>
//             <Link href='/' className='cursor-pointer flex items-center gap-1'>
//                 <Image
//                     src='/icons/logo.svg'
//                     width={34}
//                     height={34}
//                     alt='Horizon logo'
//                 />

//                 <h1 className='text-26 font-ibm-plex-serif font-bold text-black-1'>
//                     Horizon
//                 </h1>
//             </Link>

//             <div className='flex flex-col gap-1 md:gap-3'>
//                 <h1 className='text-24 lg:text-36 font-semibold text-gray-900'>
//                     {
//                         user
//                             ? 'Link account'
//                             : type === 'sign-in'
//                                 ? 'Sign In'
//                                 : 'Sign Up'
//                     }

//                     <p className='text-16 font-normal text-gray-600'>
//                         {
//                             user
//                                 ? 'Link your account to start'
//                                 : 'Enter your details'
//                         }
//                     </p>
//                 </h1>
//             </div>
//         </header>

//         {
//             user
//                 ? (
//                     <div className='flex flex-col gap-4'>
//                         <PlaidLink user={user} variant='primary'/>
//                     </div>
//                 )
//                 : (
//                     <>
//                         <Form {...form}>
//                         <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-8">
//                             {
//                                 type === 'sign-up' && (
//                                     <>
//                                         <div className='flex gap-4'>
//                                             <CustomInput control={form.control} name="firstName" label="First name" placeholder="Enter your first name" />
//                                             <CustomInput control={form.control} name="lastName" label="Last name" placeholder="Enter your last name" />
//                                         </div>
//                                         <CustomInput control={form.control} name="address1" label="Address" placeholder="Enter your address" />
//                                         <CustomInput control={form.control} name="city" label="City" placeholder="Enter your city" />
//                                         <div className='flex gap-4'>
//                                             <CustomInput control={form.control} name="state" label="State" placeholder="Enter your state" />
//                                             <CustomInput control={form.control} name="postalCode" label="Postal code" placeholder="XX-XXX" />
//                                         </div>
//                                         <div className='flex gap-4'>
//                                             <CustomInput control={form.control} name="dateOfBirth" label="Date of Birth" placeholder="YYYY-MM-DD" />
//                                             <CustomInput control={form.control} name="ssn" label="SSN" placeholder="Enter SSN" />
//                                         </div>
//                                     </>
//                                 )
//                             }

//                             <CustomInput control={form.control} name="email" label="Email" placeholder="Enter your email" />
//                             <CustomInput control={form.control} name="password" label="Password" placeholder="Enter your password" />

//                             <div className = 'flex flex-col gap-4'>
//                                 <Button type="submit" className='form-btn'>
//                                     {
//                                         isLoading
//                                             ? (
//                                                 <>
//                                                     <Loader2 size={20} className='animate-spin' /> &nbsp;
//                                                     Loading...
//                                                 </>
//                                             )
//                                             : (
//                                                 type === 'sign-in'
//                                                     ? 'Sign in'
//                                                     : 'Sign up'
//                                             )
//                                     }
//                                 </Button>
//                             </div>
    
//                         </form>
//                         </Form>

//                         <footer className='flex justify-center gap-1'>
//                             <p className='text-14 font-normal text-gray-600'>
//                                 {
//                                     type === 'sign-in'
//                                         ? 'Don’t have an account?'
//                                         : 'Already have an account?'
//                                 }
//                             </p>

//                             <Link href={type === 'sign-in' ? '/sign-up' : '/sign-in'} className='form-link'>
//                                 {
//                                     type === 'sign-in'
//                                         ? 'Sign up'
//                                         : 'Sign in'
//                                 }
//                             </Link>
//                         </footer>
//                     </>
//                 )
//         }
//     </section>
//   )
// }

// export default AuthForm