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

### Quick start

```tsx
import React, { useEffect, useState } from 'react';
import { View, Text, Platform } from 'react-native';
import {
  startScanning,
  stopScanning,
  isSupported,
  isEnabled,
  addNfcListener,
} from '@spo/react-native-nfc-scanner';
import type { NfcTagEvent } from '@spo/react-native-nfc-scanner';

export default function ScanScreen() {
  const [tag, setTag] = useState<NfcTagEvent | null>(null);

  useEffect(() => {
    if (Platform.OS !== 'android') return;

    const sub = addNfcListener('NfcTagScanned', setTag);

    (async () => {
      if (await isSupported() && await isEnabled()) {
        await startScanning();
      }
    })();

    return () => { stopScanning(); sub.remove(); };
  }, []);

  return (
    <View>
      {tag
        ? <Text>Scanned: {tag.payloads.join(', ')}</Text>
        : <Text>Hold an NFC tag near the device</Text>
      }
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

## Examples

### Prompt user to enable NFC

```tsx
import React, { useEffect, useState } from 'react';
import { View, Text, Button, Platform } from 'react-native';
import { isSupported, isEnabled, goToNfcSetting } from '@spo/react-native-nfc-scanner';

export default function NfcGate({ children }: { children: React.ReactNode }) {
  const [status, setStatus] = useState<'loading' | 'ready' | 'unsupported' | 'disabled'>('loading');

  useEffect(() => {
    if (Platform.OS !== 'android') { setStatus('ready'); return; }

    (async () => {
      if (!(await isSupported())) { setStatus('unsupported'); return; }
      if (!(await isEnabled()))   { setStatus('disabled');    return; }
      setStatus('ready');
    })();
  }, []);

  if (status === 'loading')     return null;
  if (status === 'unsupported') return <Text>NFC is not available on this device.</Text>;
  if (status === 'disabled')    return (
    <View>
      <Text>NFC is turned off.</Text>
      <Button title="Enable NFC" onPress={() => goToNfcSetting()} />
    </View>
  );

  return <>{children}</>;
}
```

---

### Reusable hook

```tsx
// hooks/useNfc.ts
import { useEffect, useRef, useState } from 'react';
import { Platform } from 'react-native';
import {
  startScanning, stopScanning,
  isSupported, isEnabled, goToNfcSetting,
  addNfcListener,
} from '@spo/react-native-nfc-scanner';
import type { NfcTagEvent } from '@spo/react-native-nfc-scanner';

type Status = 'idle' | 'scanning' | 'unsupported' | 'disabled';

export function useNfc(onTag: (tag: NfcTagEvent) => void) {
  const [status, setStatus] = useState<Status>('idle');
  const cb = useRef(onTag);
  cb.current = onTag;

  useEffect(() => {
    if (Platform.OS !== 'android') return;

    const sub = addNfcListener('NfcTagScanned', (t) => cb.current(t));

    (async () => {
      if (!(await isSupported())) { setStatus('unsupported'); return; }
      if (!(await isEnabled()))   { setStatus('disabled');    return; }
      await startScanning();
      setStatus('scanning');
    })();

    return () => { stopScanning(); sub.remove(); };
  }, []);

  return { status, goToNfcSetting };
}
```

```tsx
// Using the hook
import { useNfc } from './hooks/useNfc';

function ScanScreen() {
  const { status, goToNfcSetting } = useNfc((tag) => {
    console.log('Tag data:', tag.payloads);
  });

  if (status === 'unsupported') return <Text>NFC not available on this device</Text>;
  if (status === 'disabled')    return <Button title="Enable NFC" onPress={goToNfcSetting} />;
  if (status === 'scanning')    return <Text>Ready — hold a tag near the device</Text>;
  return null;
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
