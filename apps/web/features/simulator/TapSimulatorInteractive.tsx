/** biome-ignore-all lint/a11y/noLabelWithoutControl: <explanation> */
/** biome-ignore-all lint/correctness/useExhaustiveDependencies: <explanation> */
/** biome-ignore-all lint/a11y/noLabelWithoutControl: simulator */
"use client";

import React, { useRef, useState, useEffect, type ReactNode } from "react";
import { toast } from "sonner";
import {
  CreditCard,
  MapPin,
  CheckCircle2,
  ChevronLeft,
  ChevronRight,
} from "lucide-react";
import { cn } from "@/lib/utils";
import { DigitalCard } from "@/components/shared/DigitalCard";
import { TapFormContext } from "./TapFormContext";
import type { TransitStop } from "@/lib/api-types";
import { useRouter } from "next/navigation";
import { createTap } from "@/app/actions/taps";

const DEFAULT_PRESET_CARDS = [
  { name: "Visa", pan: "4532 1188 9922 4455" },
  { name: "Mastercard", pan: "5500 0055 5555 5559" },
  { name: "AMEX", pan: "3782 822463 10005" },
  { name: "Crypto", pan: "0x89 205A 3B18 CA44" },
];

export function getCardBrandInfo(pan: string) {
  const p = pan.replace(/\s/g, "");
  if (p.startsWith("4"))
    return { name: "VISA", color: "from-blue-600 to-blue-800" };
  if (p.startsWith("5"))
    return { name: "Mastercard", color: "from-slate-800 to-black" };
  if (p.startsWith("3"))
    return { name: "AMEX", color: "from-emerald-600 to-teal-900" };
  if (p.startsWith("0x"))
    return { name: "CRYPTO", color: "from-purple-600 to-violet-900" };
  return { name: "Card", color: "from-slate-500 to-slate-700" };
}

interface Props {
  deviceSelect: ReactNode;
  stops: ReactNode;
}

export function TapSimulatorInteractive({ deviceSelect, stops }: Props) {
  const router = useRouter();
  const [deviceId, setDeviceId] = useState("");
  const [stopId, setStopId] = useState("");
  const [stopsData, setStopsData] = useState<TransitStop[]>([]);
  const [lastTap, setLastTap] = useState<{
    type: string;
    time: string;
  } | null>(null);
  const [activePan, setActivePan] = useState<string>(
    DEFAULT_PRESET_CARDS[0]?.pan as string,
  );
  const [presetCards, setPresetCards] = useState(DEFAULT_PRESET_CARDS);
  const [isEditing, setIsEditing] = useState(false);
  const [cooldown, setCooldown] = useState(false);
  const lastTapRef = useRef<number>(0);
  const scrollRef = useRef<HTMLDivElement>(null);
  const [currentIndex, setCurrentIndex] = useState(0);

  const scrollToIndex = (index: number) => {
    if (scrollRef.current) {
      const container = scrollRef.current;
      const children = Array.from(container.children) as HTMLElement[];
      const target = children[index];
      if (target) {
        const targetScrollLeft =
          target.offsetLeft - (container.clientWidth - target.offsetWidth) / 2;
        container.scrollTo({ left: targetScrollLeft, behavior: "smooth" });
      }
    }
  };

  useEffect(() => {
    const idx = presetCards.findIndex((c) => c.pan === activePan);
    if (idx !== -1 && idx !== currentIndex) {
      setCurrentIndex(idx);
      scrollToIndex(idx);
    }
  }, [activePan]);

  const scroll = (direction: "left" | "right") => {
    const nextIndex =
      direction === "left" ? currentIndex - 1 : currentIndex + 1;
    if (nextIndex >= 0 && nextIndex < presetCards.length) {
      scrollToIndex(nextIndex);
    }
  };

  const handleScroll = () => {
    if (scrollRef.current) {
      const container = scrollRef.current;
      const center = container.scrollLeft + container.clientWidth / 2;
      const children = Array.from(container.children) as HTMLElement[];

      let closestIndex = currentIndex;
      let minDistance = Infinity;

      children.forEach((child, idx) => {
        const childCenter = child.offsetLeft + child.offsetWidth / 2;
        const distance = Math.abs(center - childCenter);
        if (distance < minDistance) {
          minDistance = distance;
          closestIndex = idx;
        }
      });

      if (
        closestIndex !== currentIndex &&
        closestIndex >= 0 &&
        closestIndex < presetCards.length &&
        !isEditing
      ) {
        setCurrentIndex(closestIndex);
        setActivePan(presetCards[closestIndex]?.pan as string);
      }
    }
  };

  const handleSubmit = async (tapType: "ON" | "OFF") => {
    const debounceMs = Number(process.env.NEXT_PUBLIC_TAP_DEBOUNCE_MS) || 1000;
    if (cooldown || activePan.replace(/\s/g, "").length < 8) {
      return;
    }
    if (!stopId) {
      toast.error("No stop selected");
      return;
    }
    setCooldown(true);
    setTimeout(() => setCooldown(false), debounceMs);
    try {
      await createTap({
        pan: activePan?.replace(/\s/g, ""),
        stopId,
        deviceId,
        tapType,
      });
      lastTapRef.current = Date.now();
      const stopName = stopsData.find((s) => s.id === stopId)?.name ?? stopId;
      setLastTap({ type: tapType, time: new Date().toLocaleTimeString() });
      router.refresh();
      toast.success(`Success: TAP ${tapType} at ${stopName}`, {
        icon: tapType === "ON" ? "🟢" : "🔴",
      });
    } catch (err) {
      toast.error("Process Failed", {
        description: err instanceof Error ? err.message : "Error",
      });
    }
  };

  return (
    <TapFormContext.Provider
      value={{
        pan: activePan!,
        deviceId,
        stopId,
        stops: stopsData,
        setPan: setActivePan!,
        setDeviceId,
        setStopId,
        setStops: setStopsData,
      }}
    >
      <div className="flex flex-col shrink-0 bg-white text-slate-900 border border-slate-200/60 rounded-xl overflow-hidden shadow-sm">
        <div className="flex items-center justify-between px-4 py-3 bg-slate-50 border-b border-slate-200/60">
          <div className="flex items-center gap-2">
            <div className="w-2 h-2 rounded-full bg-emerald-500 shadow-[0_0_8px_rgba(16,185,129,0.5)] animate-pulse" />
            <span className="font-semibold text-[11px] tracking-widest text-slate-600 uppercase">
              Simulator
            </span>
          </div>
          {deviceSelect}
        </div>

        <div className="p-4 flex flex-col gap-4">
          <div>
            <label className="text-[11px] font-bold text-slate-400 uppercase tracking-wider flex items-center gap-1.5">
              <CreditCard className="w-3.5 h-3.5" />
              Digital Wallets
            </label>
            <div className="relative group/wallet pb-4 -mx-4">
              {/* Edge Gradients for Seamless Scrolling */}
              <div className="absolute left-0 top-0 bottom-0 w-5 bg-gradient-to-r from-slate-50 via-slate-50/80 to-transparent z-10 pointer-events-none" />
              <div className="absolute right-0 top-0 bottom-0 w-5 bg-gradient-to-l from-slate-50 via-slate-50/80 to-transparent z-10 pointer-events-none" />

              {/* Navigation Arrows */}
              <button
                type="button"
                onClick={() => scroll("left")}
                className={cn(
                  "absolute left-2 top-1/2 -translate-y-1/2 z-20 w-10 h-10 bg-white/80 backdrop-blur-md rounded-full shadow-lg flex items-center justify-center transition-all opacity-0 group-hover/wallet:opacity-100 hover:bg-white active:scale-95 disabled:hidden",
                  currentIndex === 0 && "hidden",
                )}
                disabled={currentIndex === 0}
              >
                <ChevronLeft className="w-6 h-6 text-slate-600" />
              </button>

              <button
                type="button"
                onClick={() => scroll("right")}
                className={cn(
                  "absolute right-2 top-1/2 -translate-y-1/2 z-20 w-10 h-10 bg-white/80 backdrop-blur-md rounded-full shadow-lg flex items-center justify-center transition-all opacity-0 group-hover/wallet:opacity-100 hover:bg-white active:scale-95 disabled:hidden",
                  currentIndex === presetCards.length - 1 && "hidden",
                )}
                disabled={currentIndex === presetCards.length - 1}
              >
                <ChevronRight className="w-6 h-6 text-slate-600" />
              </button>

              <div
                ref={scrollRef}
                onScroll={handleScroll}
                className="flex gap-4 overflow-x-auto py-8 snap-x snap-mandatory px-20 scrollbar-hide scroll-smooth"
              >
                {presetCards.map((card, idx) => {
                  const brandCard = getCardBrandInfo(card.pan);

                  return (
                    <DigitalCard
                      key={card.pan}
                      pan={activePan}
                      isEditable
                      label={
                        brandCard.name === "Card"
                          ? "Custom Input"
                          : brandCard.name
                      }
                      isActive={activePan.slice(0, 2) === card.pan.slice(0, 2)}
                      type={brandCard.name}
                      color={`bg-gradient-to-br ${brandCard.color}`}
                      className="shrink-0 sm:min-w-[280px] max-w-[calc(100vw-48px)] h-44 gap-2 p-4"
                      onPanChange={(newPan) => {
                        setActivePan(newPan);
                        setPresetCards((prev) =>
                          prev.map((c, i) =>
                            i === idx ? { ...c, pan: newPan } : c,
                          ),
                        );
                      }}
                      onEditingChange={setIsEditing}
                      onClick={() => {
                        setActivePan(card.pan);
                        setCurrentIndex(idx);
                        scrollToIndex(idx);
                      }}
                    />
                  );
                })}
              </div>
            </div>

            {/* Carousel Indicators */}
            <div className="flex justify-center gap-2 mt-2">
              {presetCards.map((card, idx) => (
                <div
                  key={card.pan}
                  className={cn(
                    "h-1.5 rounded-full transition-all duration-300",
                    currentIndex === idx
                      ? "w-8 bg-teal-500"
                      : "w-1.5 bg-slate-200",
                  )}
                />
              ))}
            </div>
          </div>

          <div className="space-y-2">
            <label className="text-[11px] font-bold text-slate-400 uppercase tracking-wider flex items-center gap-1.5">
              <MapPin className="w-3.5 h-3.5" />
              Route Stops
            </label>
            {stops}
          </div>
        </div>

        <div className="mt-auto p-4 bg-slate-50 border-t border-slate-200/60 space-y-3">
          {lastTap && (
            <div className="flex items-center justify-between px-3 py-2 bg-white border border-slate-200 rounded-lg shadow-sm">
              <div className="flex items-center gap-2">
                <CheckCircle2 className="w-4 h-4 text-emerald-500" />
                <span className="text-xs font-semibold text-slate-700">
                  {lastTap.type === "ON"
                    ? "Tap On Recorded"
                    : "Tap Off Recorded"}
                </span>
              </div>
              <span className="text-[10px] font-mono text-slate-400">
                {lastTap.time}
              </span>
            </div>
          )}
          <div className="flex gap-3">
            <button
              type="button"
              disabled={cooldown}
              onClick={() => handleSubmit("ON")}
              className={cn(
                "flex-1 h-11 bg-white border border-slate-300 border-b-4 text-emerald-600 font-black uppercase tracking-wider rounded-xl transition-all hover:bg-emerald-50 hover:border-emerald-200 hover:border-b-emerald-600 active:border-b-0 active:translate-y-[4px] disabled:opacity-50 shadow-sm",
                {
                  "pointer-events-none": cooldown,
                },
              )}
            >
              TAP ON
            </button>
            <button
              type="button"
              disabled={cooldown}
              onClick={() => handleSubmit("OFF")}
              className={cn(
                "flex-1 h-11 bg-white border border-slate-300 border-b-4 text-rose-600 font-black uppercase tracking-wider rounded-xl transition-all hover:bg-rose-50 hover:border-rose-200 hover:border-b-rose-600 active:border-b-0 active:translate-y-[4px] disabled:opacity-50 shadow-sm",
                {
                  "pointer-events-none": cooldown,
                },
              )}
            >
              TAP OFF
            </button>
          </div>
        </div>
      </div>
    </TapFormContext.Provider>
  );
}
