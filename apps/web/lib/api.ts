// apps/web/lib/api.ts
import { cacheTag, cacheLife } from "next/cache";
import { Trip, TripFilters, TransitStop } from "./api-types";

export interface Devices {
  id: string;
  operator: {
    id: string;
    name: string;
  };
  name: string;
  status: "active";
}

function getApiKey(): string {
  const key = process.env.API_KEY;
  if (!key) throw new Error("API_KEY environment variable is not set");
  return key;
}

function getApiUrl(): string {
  const url = process.env.API_URL;
  if (!url) throw new Error("API_URL environment variable is not set");
  return url.replace(/\/$/, "");
}

export interface TripDto {
  id: string;
  cardToken: string;
  fromStopId: string | null;
  toStopId: string | null;
  started: string | null;
  finished: string | null;
  durationSecs: number;
  chargeAmount: number;
  operatorId: string | null;
  status: string;
  deviceId?: string;
}

export function mapTripDto(dto: TripDto): Trip {
  return {
    id: String(dto.id),
    pan: dto.cardToken ?? "",
    fromStop: dto.fromStopId ?? "",
    toStop: dto.toStopId ?? "",
    startTime: dto.started ?? "",
    endTime: dto.finished ?? "",
    duration: dto.durationSecs,
    deviceId: dto.deviceId ?? "",
    chargeAmount: dto.chargeAmount / 100,
    operator: dto.operatorId ?? "",
    status: dto.status as Trip["status"],
  };
}

export function getTripsUrl(baseUrl: string, _filters: TripFilters): string {
  const url = new URL(`${baseUrl}/trips`);
  url.searchParams.set("page", "1");
  url.searchParams.set("size", "100");
  return url.toString();
}

export async function getTrips(filters: TripFilters = {}): Promise<Trip[]> {
  "use cache";
  cacheLife("minutes");
  cacheTag(
    `trips`,
    ...Object.entries(filters).map(([key, val]) => `${key}:${val}`),
  );

  const url = getTripsUrl(getApiUrl(), filters);
  const res = await fetch(url, {
    cache: "no-store",
    headers: { "X-API-Key": getApiKey() },
  });
  if (!res.ok) throw new Error(`Failed to fetch trips: ${res.status}`);
  const data = (await res.json()) as { content: TripDto[] };
  let trips = data.content.map(mapTripDto);

  if (filters.status) trips = trips.filter((t) => t.status === filters.status);
  if (filters.deviceId)
    trips = trips.filter((t) => t.deviceId === filters.deviceId);
  if (filters.datePreset && filters.datePreset !== "all") {
    const now = new Date();
    const today = new Date(
      Date.UTC(now.getUTCFullYear(), now.getUTCMonth(), now.getUTCDate()),
    );
    const weekAgo = new Date(today);
    weekAgo.setUTCDate(weekAgo.getUTCDate() - 6);
    trips = trips.filter((t) => {
      if (!t.startTime) return false;
      const start = new Date(t.startTime);
      return filters.datePreset === "today" ? start >= today : start >= weekAgo;
    });
  }

  return trips;
}

export async function getStops(): Promise<TransitStop[]> {
  "use cache";
  cacheTag("stops");
  const baseUrl = getApiUrl();
  const adminUser = process.env.ADMIN_USERNAME;
  const adminPass = process.env.ADMIN_PASSWORD;
  if (!adminUser || !adminPass)
    throw new Error("ADMIN_USERNAME / ADMIN_PASSWORD not set");
  const credentials = Buffer.from(`${adminUser}:${adminPass}`).toString(
    "base64",
  );
  const res = await fetch(`${baseUrl}/admin/fares?size=100`, {
    cache: "no-store",
    headers: { Authorization: `Basic ${credentials}` },
  });
  if (!res.ok) throw new Error(`Failed to fetch fares: ${res.status}`);
  const data = (await res.json()) as {
    content: {
      stopAId: string;
      stopAName: string;
      stopBId: string;
      stopBName: string;
    }[];
  };
  const map = new Map<string, string>();
  for (const fare of data.content) {
    map.set(fare.stopAId, fare.stopAName);
    map.set(fare.stopBId, fare.stopBName);
  }
  return Array.from(map.entries()).map(([id, name]) => ({ id, name }));
}

export async function getDevices(): Promise<Devices[]> {
  "use cache";
  cacheLife("hours");
  cacheTag("devices");

  const baseUrl = getApiUrl();
  const apiKey = getApiKey();
  const res = await fetch(`${baseUrl}/operator/devices`, {
    cache: "no-store",
    headers: { "X-API-Key": apiKey },
  });
  if (!res.ok) throw new Error(`Failed to fetch devices: ${res.status}`);
  return (await res.json()).content as Devices[];
}
