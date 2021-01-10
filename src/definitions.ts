declare module '@capacitor/core' {
  interface PluginRegistry {
    Heartrate: HeartratePlugin;
  }
}

export interface HeartratePlugin {
  getLuminosityFeed(): Promise<{ callStatus: string }>;
  stopAnalysis(): Promise<{ success: boolean }>;
}
