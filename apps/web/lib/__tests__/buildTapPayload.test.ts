import { describe, it, expect } from "vitest";
import { buildTapPayload } from "@/lib/build-tap-payload";
import type { TapBody } from "@/app/actions/taps";

describe("buildTapPayload", () => {
  it("returns an object with id, cardToken, stopId, deviceId, tapType, dateTimeUtc", () => {
    const body: TapBody = {
      pan: "4111111111111111",
      stopId: "stop-1",
      deviceId: "dev-1",
      tapType: "ON",
    };
    const result = buildTapPayload(body);
    expect(result.id).toBeDefined();
    expect(result.cardToken).toBeDefined();
    expect(result.stopId).toBe("stop-1");
    expect(result.deviceId).toBe("dev-1");
    expect(result.tapType).toBe("ON");
    expect(result.dateTimeUtc).toBeDefined();
  });

  it("cardToken starts with first 2 chars of PAN", () => {
    const body: TapBody = {
      pan: "4111111111111111",
      stopId: "stop-1",
      deviceId: "dev-1",
      tapType: "OFF",
    };
    const result = buildTapPayload(body);
    expect(result.cardToken.startsWith("41")).toBe(true);
  });

  it("cardToken is different from the original PAN", () => {
    const body: TapBody = {
      pan: "4111111111111111",
      stopId: "stop-1",
      deviceId: "dev-1",
      tapType: "ON",
    };
    const result = buildTapPayload(body);
    expect(result.cardToken).not.toBe("4111111111111111");
  });

  it("cardToken contains hashed portion from SHA256 of pan", () => {
    const body: TapBody = {
      pan: "4111111111111111",
      stopId: "stop-1",
      deviceId: "dev-1",
      tapType: "ON",
    };
    const result = buildTapPayload(body);
    expect(result.cardToken.length).toBeGreaterThan(2);
    expect(result.cardToken.slice(2)).toHaveLength(62);
  });

  it("generates valid UUIDs for id", () => {
    const body: TapBody = {
      pan: "4111111111111111",
      stopId: "stop-1",
      deviceId: "dev-1",
      tapType: "ON",
    };
    const result = buildTapPayload(body);
    const uuidRegex =
      /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
    expect(result.id).toMatch(uuidRegex);
  });

  it("dateTimeUtc is a valid ISO timestamp", () => {
    const body: TapBody = {
      pan: "4111111111111111",
      stopId: "stop-1",
      deviceId: "dev-1",
      tapType: "ON",
    };
    const result = buildTapPayload(body);
    expect(() => new Date(result.dateTimeUtc)).not.toThrow();
  });

  it("produces same output for same input (deterministic hashing)", () => {
    const body: TapBody = {
      pan: "4111111111111111",
      stopId: "stop-1",
      deviceId: "dev-1",
      tapType: "ON",
    };
    const result1 = buildTapPayload(body);
    const result2 = buildTapPayload({ ...body });
    expect(result1.cardToken).toBe(result2.cardToken);
  });

  it("different PANs produce different cardTokens", () => {
    const body1: TapBody = {
      pan: "4111111111111111",
      stopId: "stop-1",
      deviceId: "dev-1",
      tapType: "ON",
    };
    const body2: TapBody = {
      pan: "5500000000000004",
      stopId: "stop-1",
      deviceId: "dev-1",
      tapType: "ON",
    };
    const result1 = buildTapPayload(body1);
    const result2 = buildTapPayload(body2);
    expect(result1.cardToken).not.toBe(result2.cardToken);
  });

  it("handles TAP OFF tapType", () => {
    const body: TapBody = {
      pan: "4111111111111111",
      stopId: "stop-1",
      deviceId: "dev-1",
      tapType: "OFF",
    };
    const result = buildTapPayload(body);
    expect(result.tapType).toBe("OFF");
  });

  it("handles PAN with spaces", () => {
    const body: TapBody = {
      pan: "4111 1111 1111 1111",
      stopId: "stop-1",
      deviceId: "dev-1",
      tapType: "ON",
    };
    const result = buildTapPayload(body);
    expect(result.id).toBeDefined();
    expect(result.cardToken).toBeDefined();
  });
});
