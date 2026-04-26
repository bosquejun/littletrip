"use client";

import { cn } from "@/lib/utils";
import { Briefcase, Bus } from "lucide-react";
import Link from "next/link";
import { usePathname } from "next/navigation";

const ROLES = [
  {
    id: "/merchant",
    label: "Merchant",
    icon: Briefcase,
    color: "text-emerald-400",
  },
];

export default function SidebarMenu() {
  const pathname = usePathname();

  return (
    <>
      <Link
        href="/merchant"
        className="w-10 h-10 rounded-xl bg-gradient-to-br from-teal-500 to-emerald-600 flex items-center justify-center text-white shadow-lg shadow-teal-500/20 mb-8 shrink-0"
      >
        <Bus className="w-5 h-5" />
      </Link>

      <nav className="flex-1 flex flex-col items-center gap-4 w-full px-2">
        {ROLES.map((r) => {
          const isActive =
            pathname === r.id || (r.id !== "/" && pathname.startsWith(r.id));
          return (
            <Link
              key={r.id}
              href={r.id}
              title={r.label}
              className={cn(
                "relative group w-12 h-12 flex items-center justify-center rounded-xl transition-all duration-300",
                isActive
                  ? "bg-slate-800 shadow-inner"
                  : "hover:bg-slate-800/50",
              )}
            >
              {isActive && (
                <div className="absolute left-0 top-1/2 -translate-y-1/2 w-1 h-6 bg-teal-500 rounded-r-full" />
              )}
              <r.icon
                className={cn(
                  "w-5 h-5 transition-colors",
                  isActive
                    ? r.color
                    : "text-slate-500 group-hover:text-slate-300",
                )}
              />
            </Link>
          );
        })}
      </nav>
    </>
  );
}
