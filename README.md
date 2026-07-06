# @santoshpk/react-native-nfc-scanner

Android NFC scanning for React Native. Supports foreground and background scanning out of the box.

> **Android only.** Works with both New Architecture (TurboModules) and Old Architecture (Bridge).

[![npm version](https://img.shields.io/npm/v/@santoshpk/react-native-nfc-scanner)](https://www.npmjs.com/package/@santoshpk/react-native-nfc-scanner)
[![Platform](https://img.shields.io/badge/platform-Android-green)](https://developer.android.com/guide/topics/connectivity/nfc)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

---

## Installation

```sh
npm install @santoshpk/react-native-nfc-scanner
```

Auto-links on install. No extra setup needed.

The library automatically detects your architecture:
- **New Architecture** (`newArchEnabled=true`) — uses TurboModules via JSI
- **Old Architecture** — uses the standard React Native Bridge

---

## Usage

```tsx
import { useEffect } from 'react';
import { Button, Platform, View } from 'react-native';
import {
  startScanning,
  stopScanning,
  isSupported,
  isEnabled,
  goToNfcSetting,
  addNfcListener,
} from '@santoshpk/react-native-nfc-scanner';

function ScanScreen() {
  async function scan() {
    if (Platform.OS !== 'android') return;

    const supported = await isSupported();
    if (!supported) return console.log('NFC not supported');

    const enabled = await isEnabled();
    if (!enabled) return goToNfcSetting();

    const sub = addNfcListener('NfcTagScanned', (tag) => {
      console.log('Tag ID:', tag.tag);
      console.log('Payloads:', tag.payloads);
      stopScanning();
      sub.remove();
    });

    await startScanning();
  }

  return (
    <View>
      <Button title="Scan NFC Tag" onPress={scan} />
    </View>
  );
}
```

---

## API

| Method | Returns | Description |
|---|---|---|
| `startScanning()` | `Promise<void>` | Start listening for NFC tags |
| `stopScanning()` | `Promise<void>` | Stop listening |
| `isSupported()` | `Promise<boolean>` | Check if device has NFC hardware |
| `isEnabled()` | `Promise<boolean>` | Check if NFC is turned on |
| `goToNfcSetting()` | `Promise<boolean>` | Open Android NFC settings |
| `preventDefaultNfcScreen(enabled)` | `Promise<void>` | When `true`, silently consume NFC tags while not scanning (prevents default Android NFC dialog) |
| `addNfcListener(event, cb)` | `EmitterSubscription` | Subscribe to scan events. Call `.remove()` to unsubscribe |

All methods throw if called on iOS. Guard with `Platform.OS === 'android'`.

### Events

| Event | Payload |
|---|---|
| `NfcTagScanned` | `NfcTagEvent` |

### Types

```typescript
interface NfcTagEvent {
  tag: string;         // tag identifier
  ndef: string;        // NDEF data (empty string if none)
  payloads: string[];  // decoded text from each NDEF record
}
```

---

## Permissions

The library automatically merges the following into your app's manifest:

- `android.permission.NFC`
- `android.permission.VIBRATE` (haptic feedback on scan)
- `android.hardware.nfc` feature (declared as optional)

No manual manifest edits required.

---

## Requirements

- React Native **0.71+**
- Android **API 21+**
- Physical device with NFC

---

## License

MIT
