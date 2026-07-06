import { NativeModules, NativeEventEmitter, Platform } from 'react-native';
import type { NfcTagEvent, NfcEvent } from './types';

declare const global: { __turboModuleProxy?: unknown };

const ANDROID_ONLY_ERROR =
  '@santoshpk/react-native-nfc-scanner is Android only. ' +
  'Wrap calls in Platform.OS === "android" checks.';

const LINKING_ERROR =
  `The package '@santoshpk/react-native-nfc-scanner' doesn't seem to be linked.\n\n` +
  `Make sure to rebuild the Android app: npx react-native run-android\n`;

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

let _nfcNative: ReturnType<typeof getNativeModule> | undefined;
let _eventEmitter: NativeEventEmitter | null | undefined;

function getNfcNative() {
  if (_nfcNative === undefined) {
    _nfcNative = getNativeModule();
    _eventEmitter = _nfcNative ? new NativeEventEmitter(_nfcNative) : null;
  }
  return _nfcNative;
}

function getEventEmitter() {
  if (_eventEmitter === undefined) getNfcNative();
  return _eventEmitter;
}

/** Start scanning for NFC tags. Tags are emitted via the 'NfcTagScanned' event. */
export function startScanning(): Promise<void> {
  if (!getNfcNative()) throw new Error(ANDROID_ONLY_ERROR);
  return getNfcNative()!.startScanning();
}

/** Stop scanning for NFC tags. */
export function stopScanning(): Promise<void> {
  if (!getNfcNative()) throw new Error(ANDROID_ONLY_ERROR);
  return getNfcNative()!.stopScanning();
}

/** Returns true if the device hardware supports NFC. */
export function isSupported(): Promise<boolean> {
  if (!getNfcNative()) throw new Error(ANDROID_ONLY_ERROR);
  return getNfcNative()!.isSupported();
}

/** Returns true if NFC is enabled in device settings. */
export function isEnabled(): Promise<boolean> {
  if (!getNfcNative()) throw new Error(ANDROID_ONLY_ERROR);
  return getNfcNative()!.isEnabled();
}

/** Opens the Android NFC settings screen. */
export function goToNfcSetting(): Promise<boolean> {
  if (!getNfcNative()) throw new Error(ANDROID_ONLY_ERROR);
  return getNfcNative()!.goToNfcSetting();
}

/**
 * When enabled, NFC tags are silently consumed even when scanning is stopped,
 * preventing the default Android NFC dialog from appearing.
 */
export function preventDefaultNfcScreen(enabled: boolean): Promise<void> {
  if (!getNfcNative()) throw new Error(ANDROID_ONLY_ERROR);
  return getNfcNative()!.preventDefaultNfcScreen(enabled);
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
  if (!getEventEmitter()) throw new Error(ANDROID_ONLY_ERROR);
  return getEventEmitter()!.addListener(event, callback);
}
