import { TapBody } from "@/app/actions/taps";
import crypto from "node:crypto";

export function buildTapPayload(body: TapBody) {
  const hashed = crypto.createHash("sha256").update(body.pan).digest("hex");
  return {
    id: crypto.randomUUID(),
    cardToken: body.pan.slice(0, 2) + hashed.slice(2),
    stopId: body.stopId,
    deviceId: body.deviceId,
    tapType: body.tapType,
    dateTimeUtc: new Date().toISOString(),
  };
}
