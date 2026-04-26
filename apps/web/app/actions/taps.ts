"use server";

import { revalidateTag } from "next/cache";
import { buildTapPayload } from "@/lib/build-tap-payload";

export interface TapBody {
  pan: string;
  stopId: string;
  deviceId: string;
  tapType: "ON" | "OFF";
}

export async function createTap(body: TapBody) {
  const apiKey = process.env.API_KEY;
  const apiUrl = process.env.API_URL?.replace(/\/$/, "");
  if (!apiKey || !apiUrl) {
    throw new Error("Server misconfiguration");
  }
  const payload = buildTapPayload(body);
  const res = await fetch(`${apiUrl}/taps`, {
    method: "POST",
    headers: { "Content-Type": "application/json", "X-API-Key": apiKey },
    body: JSON.stringify(payload),
  });
  if (!res.ok) {
    const text = await res.text();
    throw new Error(text);
  }
  await revalidateTag("trips", "max");
  return res.json();
}
