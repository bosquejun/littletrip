import { describe, it, expect } from "vitest";
import { getCardBrandInfo } from "../TapSimulatorInteractive";

describe("getCardBrandInfo", () => {
  it('returns VISA for PANs starting with 4', () => {
    const result = getCardBrandInfo("4532118899224455");
    expect(result.name).toBe("VISA");
    expect(result.color).toBe("from-blue-600 to-blue-800");
  });

  it("strips spaces before parsing", () => {
    const result = getCardBrandInfo("4532 1188 9922 4455");
    expect(result.name).toBe("VISA");
  });

  it('returns Mastercard for PANs starting with 5', () => {
    const result = getCardBrandInfo("5500005555555559");
    expect(result.name).toBe("Mastercard");
    expect(result.color).toBe("from-slate-800 to-black");
  });

  it('returns AMEX for PANs starting with 3', () => {
    const result = getCardBrandInfo("378282246310005");
    expect(result.name).toBe("AMEX");
    expect(result.color).toBe("from-emerald-600 to-teal-900");
  });

  it('returns CRYPTO for PANs starting with 0x', () => {
    const result = getCardBrandInfo("0x89205A3B18CA44");
    expect(result.name).toBe("CRYPTO");
    expect(result.color).toBe("from-purple-600 to-violet-900");
  });

  it("returns default Card for unrecognized PANs", () => {
    const result = getCardBrandInfo("9999999999999999");
    expect(result.name).toBe("Card");
    expect(result.color).toBe("from-slate-500 to-slate-700");
  });

  it("returns default Card for empty string", () => {
    const result = getCardBrandInfo("");
    expect(result.name).toBe("Card");
  });

  it("returns VISA for partial VISA-like PAN", () => {
    const result = getCardBrandInfo("4111");
    expect(result.name).toBe("VISA");
  });

  it("returns default Card for numeric string starting with 9", () => {
    const result = getCardBrandInfo("9111111111111111");
    expect(result.name).toBe("Card");
  });

  it("returns VISA for long VISA PAN", () => {
    const result = getCardBrandInfo("41111111111111111111");
    expect(result.name).toBe("VISA");
  });
});