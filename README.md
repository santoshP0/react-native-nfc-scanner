# @spo/react-native-nfc-scanner

Android NFC scanning for React Native. Supports foreground and background scanning out of the box.

> **Android only.** Requires React Native 0.71+.

[![npm version](https://img.shields.io/npm/v/@spo/react-native-nfc-scanner)](https://www.npmjs.com/package/@spo/react-native-nfc-scanner)
[![Platform](https://img.shields.io/badge/platform-Android-green)](https://developer.android.com/guide/topics/connectivity/nfc)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

---

## Installation

```sh
npm install @spo/react-native-nfc-scanner
```

That's it. The package auto-links and wires itself into the Android lifecycle automatically. No `MainApplication` or `MainActivity` changes needed.

---

## Usage

```tsx
import { Button, View } from 'react-native';
import {
  startScanning,
  stopScanning,
  isSupported,
  isEnabled,
  goToNfcSetting,
  addNfcListener,
} from '@spo/react-native-nfc-scanner';

function ScanScreen() {
  async function scan() {
    // Check if the device has NFC hardware
    const supported = await isSupported();
    if (!supported) {
      console.log('NFC is not supported on this device');
      return;
    }

    // Check if NFC is turned on in settings
    const enabled = await isEnabled();
    if (!enabled) {
      // Opens Android NFC settings so the user can turn it on
      await goToNfcSetting();
      return;
    }

    // Listen for a tag — fires when a tag is scanned
    const sub = addNfcListener('NfcTagScanned', (tag) => {
      console.log('Tag ID:', tag.tag);
      console.log('NDEF payload:', tag.payloads);

      // Stop scanning and clean up once we have the result
      stopScanning();
      sub.remove();
    });

    // Start scanning
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

### Methods

| Method | Returns | When to use | Why |
|---|---|---|---|
| `startScanning()` | `Promise<void>` | When the user is ready to scan | Activates NFC dispatch — tags are detected in foreground and background |
| `stopScanning()` | `Promise<void>` | When leaving the screen or on logout | Stops dispatch to save battery and prevent unwanted reads |
| `isSupported()` | `Promise<boolean>` | Before showing any NFC UI | Some devices have no NFC chip — check this first |
| `isEnabled()` | `Promise<boolean>` | Before calling `startScanning()` | NFC may be supported but turned off — use this to decide whether to prompt the user |
| `goToNfcSetting()` | `Promise<boolean>` | When `isEnabled()` returns `false` | Opens Android NFC settings so the user can turn it on without leaving the app |
| `addNfcListener(event, cb)` | `EmitterSubscription` | Before calling `startScanning()` | Registers the callback that fires on every scan — always call `.remove()` on cleanup |

### Events

| Event | Payload | Description |
|---|---|---|
| `NfcTagScanned` | `NfcTagEvent` | Fired each time an NFC tag is successfully read |

### Types

```typescript
interface NfcTagEvent {
  tag: string;         // Android tag identifier string
  ndef: string;        // NDEF data — empty string if the tag has no NDEF content
  payloads: string[];  // decoded UTF-8 text from each NDEF record
}
```


---

## Requirements

- React Native **0.71+**
- Android **API 21+**
- Physical Android device with NFC (does not work on emulators)

---

## License

MIT
