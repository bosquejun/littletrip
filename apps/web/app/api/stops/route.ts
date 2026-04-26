import { NextResponse } from "next/server";
import { getStops } from "@/lib/api";

export async function GET() {
  try {
    const stops = await getStops();
    return NextResponse.json(stops);
  } catch (err) {
    const message = err instanceof Error ? err.message : "Failed to fetch stops";
    return NextResponse.json({ error: message }, { status: 500 });
  }
}
