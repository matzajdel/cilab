"use client";

import { Loader2 } from "lucide-react";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { useForm } from "react-hook-form";
import * as z from "zod";
import { API_BASE_URL } from '@/lib/config';
import api from "@/lib/api";
import { Button } from "../ui/button";
import {
  Form,
  FormControl,
  FormDescription,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from "../ui/form";
import { Input } from "../ui/input";


const PipelineParametersForm = ({ pipelineId, parameters, onCancel }: PipelineParametersFormProps) => {
  const router = useRouter();
  const [isLoading, setIsLoading] = useState(false);

  const form = useForm({
    defaultValues: parameters.reduce((acc, param) => {
      acc[param.name] = param.defaultValue || "";
      return acc;
    }, {} as Record<string, string>),
  });

  const onSubmit = async (data: Record<string, string>) => {
    setIsLoading(true);

    try {
      console.log("Submitting run with params:", data);
      const response = await api.post(`/api/v1/runs/pipelines/${pipelineId}`, data);

      console.log("Run result:", response.data);
      router.refresh();
      onCancel(); // Go back to list
      
    } catch (error) {
      console.error("Error submitting form:", error);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <Form {...form}>
      <form onSubmit={form.handleSubmit(onSubmit)} className="flex flex-col gap-4">
        {parameters.map((param) => (
          <FormField
            key={param.name}
            control={form.control}
            name={param.name}
            render={({ field }) => (
              <FormItem className="border-t border-gray-200">
                <div className="payment-transfer_form-item pb-6 pt-5">
                    <div className="payment-transfer_form-content">
                        <FormLabel className="text-14 font-medium text-gray-700">
                        {param.name}
                        </FormLabel>
                        <FormDescription className="text-12 font-normal text-gray-600">
                        {param.description}
                        </FormDescription>
                    </div>
                    <div className="flex w-full flex-col">
                        <FormControl>
                        <Input
                            placeholder={`Enter ${param.name}`}
                            className="input-class"
                            {...field}
                        />
                        </FormControl>
                        <FormMessage className="text-12 text-red-500" />
                    </div>
                </div>
              </FormItem>
            )}
          />
        ))}

        <div className="payment-transfer_btn-box">
          <Button type="submit" className="payment-transfer_btn" disabled={isLoading}>
            {isLoading ? (
              <>
                <Loader2 size={20} className="animate-spin" /> &nbsp; Running...
              </>
            ) : (
              "Run Pipeline"
            )}
          </Button>

            <Button type="button" variant="outline" className="ml-2" onClick={onCancel}>
                Cancel
            </Button>
        </div>
      </form>
    </Form>
  );
};

export default PipelineParametersForm;
