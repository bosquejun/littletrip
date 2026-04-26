import { getStops } from "@/lib/api";
import { StopsClient } from "./StopsClient";

export async function StopsServer() {
  const stops = await getStops();
  return <StopsClient stops={stops} />;
}
