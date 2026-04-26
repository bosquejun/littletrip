"use client";

import { type ReactNode } from "react";
import { Terminal } from "lucide-react";
import {
  Sheet,
  SheetContent,
  SheetHeader,
  SheetTitle,
  SheetTrigger,
} from "@/components/ui/sheet";
import SidebarMenu from "./SidebarMenu";

export function Sidebar({ simulator }: { simulator?: ReactNode }) {
  return (
    <aside className="w-[80px] bg-[#0f172a] border-r border-slate-800 flex flex-col items-center py-6 overflow-y-auto shrink-0 z-20">
      <SidebarMenu />

      <div className="md:hidden mt-auto flex flex-col items-center gap-4 pt-6 border-t border-slate-800 w-full px-2">
        <Sheet>
          <SheetTrigger
            title="Tap Simulator"
            className="w-12 h-12 flex items-center justify-center rounded-xl transition-all hover:bg-slate-800/50 text-white hover:text-teal-400 bg-transparent border border-teal-400/30 cursor-pointer"
          >
            <Terminal className="w-5 h-5" />
          </SheetTrigger>
          <SheetContent
            side="right"
            className="w-[360px] p-0 bg-white overflow-y-auto"
          >
            <SheetHeader className="px-4 pt-4 pb-2 border-b border-slate-100">
              <SheetTitle className="text-sm font-black uppercase tracking-widest text-slate-700">
                Tap Simulator
              </SheetTitle>
            </SheetHeader>
            <div className="p-4">{simulator}</div>
          </SheetContent>
        </Sheet>
      </div>
    </aside>
  );
}
