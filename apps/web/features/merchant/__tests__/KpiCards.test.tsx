import { render, screen } from "@testing-library/react";
import { describe, it, expect } from "vitest";
import { KpiCards } from "../KpiCards";

describe("KpiCards", () => {
  const defaultStats = {
    totalRevenue: 0,
    activeSessions: 0,
    settledJourneys: 0,
  };

  it("renders all three KPI card labels", () => {
    render(<KpiCards stats={defaultStats} />);
    const labels = screen.getAllByText(/total revenue|active sessions|settled journeys/i);
    expect(labels).toHaveLength(3);
  });

  it('renders "Total Revenue" card with label', () => {
    render(<KpiCards stats={defaultStats} />);
    expect(screen.getByText("Total Revenue")).toBeInTheDocument();
  });

  it('renders "Active Sessions" card with label', () => {
    render(<KpiCards stats={defaultStats} />);
    expect(screen.getByText("Active Sessions")).toBeInTheDocument();
  });

  it('renders "Settled Journeys" card with label', () => {
    render(<KpiCards stats={defaultStats} />);
    expect(screen.getByText("Settled Journeys")).toBeInTheDocument();
  });

  it("displays totalRevenue formatted as currency with $ sign", () => {
    const stats = { ...defaultStats, totalRevenue: 1234.5 };
    render(<KpiCards stats={stats} />);
    expect(screen.getByText("$1234.50")).toBeInTheDocument();
  });

  it("displays zero revenue with 0.00 format", () => {
    render(<KpiCards stats={defaultStats} />);
    expect(screen.getByText("$0.00")).toBeInTheDocument();
  });

  it("displays activeSessions as plain number", () => {
    const stats = { ...defaultStats, activeSessions: 42 };
    render(<KpiCards stats={stats} />);
    expect(screen.getByText("42")).toBeInTheDocument();
  });

  it("displays settledJourneys as plain number", () => {
    const stats = { ...defaultStats, settledJourneys: 99 };
    render(<KpiCards stats={stats} />);
    expect(screen.getByText("99")).toBeInTheDocument();
  });

  it("displays trend values in each card", () => {
    render(<KpiCards stats={defaultStats} />);
    expect(screen.getByText("+12.5%")).toBeInTheDocument();
    expect(screen.getByText("+3")).toBeInTheDocument();
    expect(screen.getByText("98.2%")).toBeInTheDocument();
  });

  it('displays "vs last month" label in each card', () => {
    render(<KpiCards stats={defaultStats} />);
    const labels = screen.getAllByText("vs last month");
    expect(labels).toHaveLength(3);
  });
});