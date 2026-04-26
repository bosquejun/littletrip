import { getDevices } from "@/lib/api";
import { DeviceSelectClient } from "./DeviceSelectClient";

export async function DeviceSelectServer() {
  const devices = await getDevices();

  return <DeviceSelectClient devices={devices} />;
}
