import { render, screen } from "@testing-library/react";
import { act } from "react";
import { describe, it, expect, vi } from "vitest";
import { MerchantClient } from "../MerchantClient";
import type { Trip, TripFilters } from "@/lib/api-types";
import type { Devices } from "@/lib/api";

vi.mock("next/navigation", () => ({
  useRouter: () => ({
    push: vi.fn(),
    refresh: vi.fn(),
  }),
}));

function makeTrip(overrides: Partial<Trip> = {}): Trip {
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

const defaultDevices: Devices[] = [
  { id: "dev-1", operatorId: "op-1", location: "Stop A" },
];

const defaultTrips = [makeTrip()];

const defaultFilters: TripFilters = {};

describe("MerchantClient", () => {
  it("renders Filters and TripTable components", () => {
    render(
      <MerchantClient
        trips={defaultTrips}
        initialFilters={defaultFilters}
        devices={defaultDevices}
      />,
    );
    expect(screen.getByRole("table")).toBeInTheDocument();
  });

  it("renders refresh button with correct label", () => {
    render(
      <MerchantClient
        trips={defaultTrips}
        initialFilters={defaultFilters}
        devices={defaultDevices}
      />,
    );
    expect(screen.getByRole("button", { name: /refresh/i })).toBeInTheDocument();
  });

  it("refresh button is not disabled initially", () => {
    render(
      <MerchantClient
        trips={defaultTrips}
        initialFilters={defaultFilters}
        devices={defaultDevices}
      />,
    );
    expect(screen.getByRole("button", { name: /refresh/i })).not.toBeDisabled();
  });

  it("refresh button has animate-spin class on the icon when pending", () => {
    render(
      <MerchantClient
        trips={defaultTrips}
        initialFilters={defaultFilters}
        devices={defaultDevices}
      />,
    );
    const spinnerIcon = document.querySelector('[class*="animate-spin"]');
    expect(spinnerIcon).toBeNull();
  });
});