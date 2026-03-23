import { NativeModules, NativeEventEmitter, Platform } from 'react-native';
import type { NfcTagEvent, NfcEvent } from './types';

declare const global: { __turboModuleProxy?: unknown };

const ANDROID_ONLY_ERROR =
  '@spo/react-native-nfc-scanner is Android only. ' +
  'Wrap calls in Platform.OS === "android" checks.';

const LINKING_ERROR =
  `The package '@spo/react-native-nfc-scanner' doesn't seem to be linked.\n\n` +
  `Make sure to:\n` +
  `1. Add NfcManagerPackage() to your MainApplication\n` +
  `2. Rebuild the Android app (npx react-native run-android)\n`;

function getNativeModule() {
  if (Platform.OS !== 'android') return null;

  // New architecture: TurboModules via JSI
  if (global.__turboModuleProxy != null) {
    // eslint-disable-next-line @typescript-eslint/no-var-requires
    const mod = require('./specs/NativeNfcManager').default;
    if (mod == null) throw new Error(LINKING_ERROR);
    return mod;
  }

  // Old architecture: legacy bridge
  if (NativeModules.NfcManager == null) throw new Error(LINKING_ERROR);
  return NativeModules.NfcManager;
}

const NfcNative = getNativeModule();
const eventEmitter = NfcNative ? new NativeEventEmitter(NfcNative) : null;

/** Start scanning for NFC tags. Tags are emitted via the 'NfcTagScanned' event. */
export function startScanning(): Promise<void> {
  if (!NfcNative) throw new Error(ANDROID_ONLY_ERROR);
  return NfcNative.startScanning();
}

/** Stop scanning for NFC tags. */
export function stopScanning(): Promise<void> {
  if (!NfcNative) throw new Error(ANDROID_ONLY_ERROR);
  return NfcNative.stopScanning();
}

/** Returns true if the device hardware supports NFC. */
export function isSupported(): Promise<boolean> {
  if (!NfcNative) throw new Error(ANDROID_ONLY_ERROR);
  return NfcNative.isSupported();
}

/** Returns true if NFC is enabled in device settings. */
export function isEnabled(): Promise<boolean> {
  if (!NfcNative) throw new Error(ANDROID_ONLY_ERROR);
  return NfcNative.isEnabled();
}

/** Opens the Android NFC settings screen. */
export function goToNfcSetting(): Promise<boolean> {
  if (!NfcNative) throw new Error(ANDROID_ONLY_ERROR);
  return NfcNative.goToNfcSetting();
}

/**
 * Subscribe to NFC events from the native module.
 * Returns an EmitterSubscription — call .remove() to clean up.
 *
 * @example
 * const sub = addNfcListener('NfcTagScanned', (e) => console.log(e.payloads));
 * return () => sub.remove();
 */
export function addNfcListener(
  event: NfcEvent,
  callback: (data: NfcTagEvent) => void
) {
  if (!eventEmitter) throw new Error(ANDROID_ONLY_ERROR);
  return eventEmitter.addListener(event, callback);
}
