import { getTrips, getDevices, Devices } from "@/lib/api";
import { Trip, TripFilters, TripStatus } from "@/lib/api-types";
import { MerchantPage } from "@/features/merchant/MerchantPage";
import { Suspense } from "react";

interface PageProps {
  searchParams: Promise<{
    status?: string;
    deviceId?: string;
    datePreset?: string;
  }>;
}

export default async function Route({ searchParams }: PageProps) {
  const params = await searchParams;
  const filters: TripFilters = {
    status: params.status as TripStatus | undefined,
    deviceId: params.deviceId,
    datePreset: params.datePreset as TripFilters["datePreset"],
  };

  let trips: Trip[] = [];
  let devices: Devices[] = [];
  let error: string | undefined;

  try {
    trips = await getTrips(filters);
  } catch (err) {
    error = err instanceof Error ? err.message : "Failed to load trips";
  }

  try {
    devices = await getDevices();
  } catch {
    // Device fetch failure degrades gracefully — dropdown shows empty
  }

  return (
    <Suspense>
      <MerchantPage
        trips={trips}
        filters={filters}
        devices={devices}
        error={error}
      />
    </Suspense>
  );
}
