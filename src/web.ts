import { WebPlugin } from '@capacitor/core';
import { HeartratePlugin } from './definitions';

export class HeartrateWeb extends WebPlugin implements HeartratePlugin {
  constructor() {
    super({
      name: 'Heartrate',
      platforms: ['web'],
    });
  }

  async getLuminosityFeed(): Promise<{ callStatus: string }> {
    return { callStatus: 'success' };
  }

  async stopAnalysis(): Promise<{ success: boolean }> {
    return { success: true };
  }
}

const Heartrate = new HeartrateWeb();

export { Heartrate };

import { registerWebPlugin } from '@capacitor/core';
registerWebPlugin(Heartrate);
