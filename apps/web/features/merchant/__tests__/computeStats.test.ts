import { describe, it, expect } from "vitest";
import { computeStats } from "../MerchantPage";
import type { Trip } from "@/lib/api-types";

function makeTrip(overrides: Partial<Trip>): Trip {
  return {
    id: "id",
    pan: "pan",
    fromStop: "from",
    toStop: "to",
    startTime: "t",
    endTime: "t",
    duration: 0,
    chargeAmount: 0,
    status: "COMPLETED",
    ...overrides,
  };
}

describe("computeStats", () => {
  it("returns zeros for empty trip array", () => {
    const result = computeStats([]);
    expect(result.totalRevenue).toBe(0);
    expect(result.activeSessions).toBe(0);
    expect(result.settledJourneys).toBe(0);
  });

  it("sums all chargeAmount values as totalRevenue", () => {
    const trips = [
      makeTrip({ chargeAmount: 100 }),
      makeTrip({ chargeAmount: 250 }),
      makeTrip({ chargeAmount: 150 }),
    ];
    const result = computeStats(trips);
    expect(result.totalRevenue).toBe(500);
  });

  it("counts trips with INCOMPLETE status as activeSessions", () => {
    const trips = [
      makeTrip({ status: "INCOMPLETE" }),
      makeTrip({ status: "INCOMPLETE" }),
      makeTrip({ status: "COMPLETED" }),
      makeTrip({ status: "IN_PROGRESS" }),
    ];
    const result = computeStats(trips);
    expect(result.activeSessions).toBe(2);
  });

  it("counts trips with COMPLETED status as settledJourneys", () => {
    const trips = [
      makeTrip({ status: "COMPLETED" }),
      makeTrip({ status: "COMPLETED" }),
      makeTrip({ status: "COMPLETED" }),
      makeTrip({ status: "INCOMPLETE" }),
    ];
    const result = computeStats(trips);
    expect(result.settledJourneys).toBe(3);
  });

  it("handles mixed trip statuses correctly", () => {
    const trips = [
      makeTrip({ status: "COMPLETED", chargeAmount: 300 }),
      makeTrip({ status: "INCOMPLETE", chargeAmount: 0 }),
      makeTrip({ status: "CANCELLED", chargeAmount: 0 }),
      makeTrip({ status: "IN_PROGRESS", chargeAmount: 150 }),
    ];
    const result = computeStats(trips);
    expect(result.totalRevenue).toBe(450);
    expect(result.activeSessions).toBe(1);
    expect(result.settledJourneys).toBe(1);
  });

  it("handles large revenue values", () => {
    const trips = Array.from({ length: 1000 }, (_, i) =>
      makeTrip({ chargeAmount: 1000 }),
    );
    const result = computeStats(trips);
    expect(result.totalRevenue).toBe(1_000_000);
  });

  it("does not count CANCELLED or IN_PROGRESS in activeSessions or settledJourneys", () => {
    const trips = [
      makeTrip({ status: "CANCELLED" }),
      makeTrip({ status: "IN_PROGRESS" }),
    ];
    const result = computeStats(trips);
    expect(result.activeSessions).toBe(0);
    expect(result.settledJourneys).toBe(0);
    expect(result.totalRevenue).toBe(0);
  });
});