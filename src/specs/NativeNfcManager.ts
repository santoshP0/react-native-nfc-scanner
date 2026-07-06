import { Platform, TurboModuleRegistry } from 'react-native';
import type { TurboModule } from 'react-native';

export interface Spec extends TurboModule {
  startScanning(): Promise<void>;
  stopScanning(): Promise<void>;
  isSupported(): Promise<boolean>;
  isEnabled(): Promise<boolean>;
  goToNfcSetting(): Promise<boolean>;
  preventDefaultNfcScreen(enabled: boolean): Promise<void>;

  // Required by React Native for TurboModules that emit events
  addListener(eventName: string): void;
  removeListeners(count: number): void;
}

export default Platform.OS === 'android'
  ? TurboModuleRegistry.getEnforcing<Spec>('NfcManager')
  : null as any as Spec;
