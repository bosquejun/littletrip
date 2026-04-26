import { Sidebar } from "@/components/layout/Sidebar";
import { TapSimulator } from "@/features/simulator/TapSimulator";
import { Suspense } from "react";

export default function DashboardLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <Suspense>
      <Sidebar simulator={<TapSimulator />} />
      <main className="flex-1 p-8 overflow-y-auto flex flex-col bg-[#f8fafc]">
        <div className="max-w-7xl mx-auto w-full space-y-8 animate-in fade-in slide-in-from-bottom-4 duration-500 fill-mode-both">
          {children}
        </div>
      </main>
      <div className="flex flex-col max-w-[380px]">
        <TapSimulator />
      </div>
    </Suspense>
  );
}
