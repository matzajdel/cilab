import { Loader2, CheckCircle2, XCircle, Clock, PlayCircle, User, Box, Terminal, Layers, ArrowRight, ChevronDown, ChevronUp } from "lucide-react";
import React, { useEffect, useState } from 'react';
import { API_BASE_URL } from '@/lib/config';
import { fetchEventSource } from '@microsoft/fetch-event-source';

const StageLogs = ({ stageId }: { stageId: string }) => {
    const [logs, setLogs] = useState<Log[]>([]);
    const eventSourceRef = React.useRef<EventSource | null>(null);

    useEffect(() => {
        const ctrl = new AbortController();

        const token = typeof window !== 'undefined' ? localStorage.getItem('access_token') : null;

        const connectToLogs = async () => {
            try {
                await fetchEventSource(`${API_BASE_URL}/api/v1/pipelines/logs/${stageId}`, {
                    method: 'GET',
                    headers: {
                        'Authorization': `Bearer ${token}`,
                        'Accept': 'text/event-stream',
                    },
                    signal: ctrl.signal,

                    async onopen(response) {
                        if (response.ok) {
                            console.log("Connection to logs opened");
                        } else if (response.status >= 400 && response.status < 500 && response.status !== 429) {
                            throw new Error(`Server responded with ${response.status}`);
                        }
                    },

                    onmessage(event) {
                        console.log("Raw event.data:", event.data);
                        
                        try {
                            const logData = JSON.parse(event.data);
                            console.log("Parsed logData:", logData);
                            
                            if (Array.isArray(logData)) {
                                setLogs(logData);
                            } else {
                                setLogs((prev) => [...prev, logData]);
                            }
                        } catch (e) {
                            console.error("Failed to parse log data:", e);
                            setLogs((prev) => [...prev, {
                                timestampNs: String(Date.now() * 1000000),
                                message: event.data
                            }]);
                        }
                    },

                    onclose() {
                        console.log("Connection closed by server");
                    },

                    onerror(err) {
                        console.error("EventSource failed:", err);
                        ctrl.abort(); 
                    }
                });
            } catch (error) {
                console.error("Failed to connect to SSE:", error);
            }
        };

        connectToLogs();

        return () => {
            ctrl.abort();
        };
    }, [stageId]);

    // useEffect(() => {
    //     // Close existing connection if any
    //     if (eventSourceRef.current) {
    //         eventSourceRef.current.close();
    //     }

    //     const eventSource = new EventSource(`${API_BASE_URL}/api/v1/pipelines/logs/${stageId}`);
    //     eventSourceRef.current = eventSource;
        
    //     eventSource.onopen = () => {
    //          console.log("Connection to logs opened");
    //     };

    //     eventSource.onmessage = (event) => {
    //         console.log("Raw event.data:", event.data);
    //         // Parse JSON log data
    //         try {
    //             const logData = JSON.parse(event.data);
    //             console.log("Parsed logData:", logData);
                
    //             // Check if it's an array of logs or a single log
    //             if (Array.isArray(logData)) {
    //                 // Server sends the entire array, replace the state
    //                 setLogs(logData);
    //             } else {
    //                 // Single log object, append it
    //                 setLogs((prev) => [...prev, logData]);
    //             }
    //         } catch (e) {
    //             // If not JSON, treat as plain text with current timestamp
    //             console.error("Failed to parse log data:", e);
    //             setLogs((prev) => [...prev, {
    //                 timestampNs: String(Date.now() * 1000000),
    //                 message: event.data
    //             }]);
    //         }
    //     };

    //     eventSource.onerror = (err) => {
    //         console.error("EventSource failed:", err);
    //         eventSource.close();
    //     };

    //     return () => {
    //          eventSource.close();
    //     };
    // }, [stageId]);

    return (
        <div className="mt-2">
            <h5 className="text-[10px] font-bold text-gray-400 uppercase mb-1.5 tracking-wider flex items-center gap-2">
                <Terminal className="w-3 h-3"/> Live Logs
            </h5>
            <div className="bg-gray-950 text-gray-300 font-mono text-xs p-3 rounded-md max-h-[700px] overflow-y-auto shadow-inner border border-gray-800">
                {logs.length === 0 ? (
                    <span className="text-gray-500 italic animate-pulse">Waiting for logs...</span>
                ) : (
                    logs.map((log, i) => (
                        <div key={i} className="whitespace-pre-wrap border-b border-gray-800/50 last:border-0 py-0.5">
                            <span className="text-gray-500 mr-2">
                                {log.timestampNs 
                                    ? new Date(Number(BigInt(log.timestampNs) / BigInt(1000000))).toLocaleString()
                                    : new Date().toLocaleString()
                                }
                            </span>
                            <span className="text-gray-300">
                                {log.message || JSON.stringify(log)}
                            </span>
                        </div>
                    ))
                )}
            </div>
        </div>
    );
};

export default StageLogs;