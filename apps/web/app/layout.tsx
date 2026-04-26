import type { Metadata } from "next";
import { Inter } from "next/font/google";
import "./globals.css";
import { cn } from "@/lib/utils";
import { Toaster } from "@/components/ui/sonner";

const inter = Inter({ subsets: ["latin"], variable: "--font-sans" });

export const metadata: Metadata = {
  title: "LittleTrips | Transit Dashboard",
  description: "Transit dashboard and tap simulator.",
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="en" className={cn("font-sans antialiased", inter.variable)}>
      <body suppressHydrationWarning>
        <div className="flex h-screen overflow-hidden font-sans text-[#0f172a]">
          {children}
          <Toaster position="bottom-right" closeButton theme="light" />
        </div>
      </body>
    </html>
  );
}
