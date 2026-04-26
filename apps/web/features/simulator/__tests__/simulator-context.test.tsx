import { render, screen } from "@testing-library/react";
import { describe, it, expect } from "vitest";
import { TapFormContext } from "../TapFormContext";
import { useTapForm } from "../TapFormContext";
import { DeviceSelectClient } from "../DeviceSelectClient";
import { StopsClient } from "../StopsClient";
import type { TransitStop } from "@/lib/api-types";

function TestComponent() {
  const ctx = useTapForm();
  return (
    <div>
      <span data-testid="pan">{ctx.pan}</span>
      <span data-testid="deviceId">{ctx.deviceId}</span>
      <span data-testid="stopId">{ctx.stopId}</span>
      <span data-testid="stops-count">{ctx.stops.length}</span>
    </div>
  );
}

function makeStops(): TransitStop[] {
  return [
    { id: "stop-1", name: "Airport Hub" },
    { id: "stop-2", name: "North Station" },
  ];
}

function makeDevices() {
  return {
    id: "dev-1",
    operator: { id: "op-1", name: "Westside" },
    name: "Device Alpha",
    status: "active" as const,
  };
}

describe("TapFormContext", () => {
  describe("useTapForm", () => {
    const defaultContext = {
      pan: "4111111111111111",
      deviceId: "dev-1",
      stopId: "stop-1",
      stops: makeStops(),
      setPan: () => {},
      setDeviceId: () => {},
      setStopId: () => {},
      setStops: () => {},
    };

    it("provides context values to child components", () => {
      render(
        <TapFormContext.Provider value={defaultContext}>
          <TestComponent />
        </TapFormContext.Provider>,
      );
      expect(screen.getByTestId("pan")).toHaveTextContent(
        "4111111111111111",
      );
      expect(screen.getByTestId("deviceId")).toHaveTextContent("dev-1");
      expect(screen.getByTestId("stopId")).toHaveTextContent("stop-1");
      expect(screen.getByTestId("stops-count")).toHaveTextContent("2");
    });

    it("throws error when used outside provider", () => {
      expect(() => render(<TestComponent />)).toThrow(
        "useTapForm must be used within TapSimulatorInteractive",
      );
    });
  });
});

describe("DeviceSelectClient", () => {
  it("renders a select dropdown with device options", () => {
    render(
      <TapFormContext.Provider
        value={{
          pan: "",
          deviceId: "",
          stopId: "",
          stops: [],
          setPan: () => {},
          setDeviceId: () => {},
          setStopId: () => {},
          setStops: () => {},
        }}
      >
        <DeviceSelectClient devices={[makeDevices()]} />
      </TapFormContext.Provider>,
    );
    expect(screen.getByRole("combobox")).toBeInTheDocument();
  });
});

describe("StopsClient", () => {
  it("renders stop list with names", () => {
    render(
      <TapFormContext.Provider
        value={{
          pan: "",
          deviceId: "",
          stopId: "",
          stops: [],
          setPan: () => {},
          setDeviceId: () => {},
          setStopId: () => {},
          setStops: () => {},
        }}
      >
        <StopsClient
          stops={[
            { id: "stop-1", name: "Airport Hub" },
            { id: "stop-2", name: "North Station" },
          ]}
        />
      </TapFormContext.Provider>,
    );
    expect(screen.getByText("Airport Hub")).toBeInTheDocument();
    expect(screen.getByText("North Station")).toBeInTheDocument();
  });
});