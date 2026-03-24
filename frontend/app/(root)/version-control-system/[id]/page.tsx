"use client"; // <-- To przenosi ten komponent do przeglądarki

import { useEffect, useState } from "react";
import { getCommits, getBranches } from "@/lib/actions/vcs.actions";
import VcsTabs from "@/components/vcs/VcsTabs";
import HeaderBox from "@/components/HeaderBox";

const VcsDetails = ({ params: { id } }: { params: { id: string } }) => {
    const [commits, setCommits] = useState([]);
    const [branches, setBranches] = useState([]);
    const [isLoading, setIsLoading] = useState(true);

    useEffect(() => {
        const fetchData = async () => {
            setIsLoading(true);
            try {
                const [fetchedCommits, fetchedBranches] = await Promise.all([
                    getCommits(id),
                    getBranches(id)
                ]);
                
                setCommits(fetchedCommits);
                setBranches(fetchedBranches);
            } catch (error) {
                console.error("Błąd ładowania danych VCS", error);
            } finally {
                setIsLoading(false);
            }
        };

        fetchData();
    }, [id]);

    return (
        <section className="no-scrollbar flex w-full flex-col gap-8 bg-gray-25 p-8 md:max-h-screen overflow-y-scroll">
            <div className="flex w-full flex-col gap-8">
                <HeaderBox
                    title="VCS Repository"
                    subtext="View and manage your repository commits and branches."
                />
                
                {/* Prosty stan ładowania */}
                {isLoading ? (
                    <div className="animate-pulse">Ładowanie danych...</div>
                ) : (
                    <VcsTabs commits={commits} branches={branches} />
                )}
            </div>
        </section>
    );
};

export default VcsDetails;