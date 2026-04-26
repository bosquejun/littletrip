// apps/web/lib/api-types.ts
export type TapType = "ON" | "OFF";
export type TripStatus =
  | "COMPLETED"
  | "INCOMPLETE"
  | "CANCELLED"
  | "IN_PROGRESS";

export interface Trip {
  id: string;
  pan: string;
  fromStop: string;
  toStop: string;
  startTime: string;
  endTime: string;
  duration: number;
  chargeAmount: number;
  status: TripStatus;
  deviceId?: string;
  operator?: string;
}

export interface TripFilters {
  status?: TripStatus;
  deviceId?: string | null;
  datePreset?: "today" | "week" | "all";
}

export interface TapRequest {
  id: string;
  deviceId: string;
  pan: string;
  tapType: TapType;
  stopId: string;
  dateTimeUTC: string;
}

export interface TransitStop {
  id: string;
  name: string;
}
