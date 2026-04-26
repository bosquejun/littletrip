"use client";

import { Wifi, Zap, Bitcoin, Smartphone } from "lucide-react";
import { cn } from "@/lib/utils";

interface DigitalCardProps {
  pan: string;
  label?: string;
  type?: string;
  color?: string;
  isActive?: boolean;
  isEditable?: boolean;
  cardHolder?: string;
  onPanChange?: (pan: string) => void;
  onEditingChange?: (isEditing: boolean) => void;
  className?: string;
  onClick?: () => void;
}

export function DigitalCard({
  pan,
  label = "Personal Card",
  type = "VISA",
  color = "bg-gradient-to-br from-blue-600 to-indigo-900",
  isActive = false,
  isEditable = false,
  cardHolder = "JUAN DELA CRUZ",
  onPanChange,
  onEditingChange,
  className,
  onClick,
}: DigitalCardProps) {
  const isCrypto = pan.startsWith("0x");

  return (
    <button
      type="button"
      onClick={onClick}
      disabled={!onClick && !isEditable}
      className={cn(
        "snap-center w-full h-48 rounded-3xl p-7 flex flex-col justify-between relative overflow-hidden transition-all duration-500 text-left cursor-default",
        color,
        isActive
          ? "ring-4 ring-teal-500/30 scale-[1.02] shadow-2xl"
          : "opacity-40 grayscale-[0.5] scale-95",
        onClick && "cursor-pointer active:scale-[0.98]",
        className,
      )}
    >
      <div className="flex justify-between items-start relative z-10 w-full">
        <div className="flex flex-col items-start gap-1">
          {isCrypto ? (
            <div className="w-9 h-9 rounded-full bg-white/10 flex items-center justify-center border border-white/20">
              <Bitcoin className="w-5 h-5 text-white" />
            </div>
          ) : (
            <div className="w-12 h-8 bg-yellow-400/80 rounded shadow-inner" />
          )}
          <span className="text-[10px] font-black italic text-white/50 uppercase tracking-widest mt-1">
            {label}
          </span>
        </div>
        <div className="flex flex-col items-end gap-2">
          <Wifi className="w-5 h-5 text-white/50 rotate-90" />
          {type?.toUpperCase() === "MASTERCARD" ? (
            <div className="flex -space-x-2.5 opacity-90 pr-1">
              <div className="w-6 h-6 rounded-full bg-[#eb001b]" />
              <div className="w-6 h-6 rounded-full bg-[#f79e1b] opacity-80" />
            </div>
          ) : (
            <span className="text-[11px] font-black text-white uppercase tracking-widest">
              {type}
            </span>
          )}
        </div>
      </div>

      <div className="space-y-4 relative z-10 w-full">
        {isEditable ? (
          <div className="relative flex items-end border-b border-white/20 focus-within:border-white/50 transition-colors pb-1">
            <input
              maxLength={19}
              type="text"
              value={pan}
              onChange={(e) => onPanChange?.(e.target.value)}
              onFocus={() => onEditingChange?.(true)}
              onBlur={() => onEditingChange?.(false)}
              className={cn(
                "w-full bg-transparent text-white font-mono focus:outline-none placeholder:text-white/30 text-lg tracking-wider",
              )}
              placeholder="Card Number"
              spellCheck={false}
            />
          </div>
        ) : (
          <div
            className={cn(
              "text-white font-mono truncate",
              isCrypto
                ? "text-[13px] tracking-normal"
                : "text-lg tracking-wider",
            )}
          >
            {pan}
          </div>
        )}

        <div className="flex justify-between items-end">
          <div className="text-left">
            <div className="text-[8px] text-white/40 uppercase font-black tracking-widest leading-none">
              Holder
            </div>
            <div className="text-xs text-white uppercase font-bold mt-1 tracking-wider">
              {cardHolder}
            </div>
          </div>
          {isCrypto ? (
            <Bitcoin className={cn("w-5 h-5 text-white/50")} />
          ) : (
            <Zap
              className={cn(
                "w-5 h-5 text-[#10b981]",
                isActive ? "animate-pulse" : "opacity-20",
              )}
            />
          )}
        </div>
      </div>

      {/* Decorative Elements */}
      <div className="absolute top-[-20%] right-[-10%] w-64 h-64 rounded-full bg-white/5 blur-3xl pointer-events-none" />
      <div className="absolute bottom-[-20%] left-[-10%] w-48 h-48 rounded-full bg-black/10 blur-3xl pointer-events-none" />
    </button>
  );
}
