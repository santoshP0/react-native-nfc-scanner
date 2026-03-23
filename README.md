# @spo/react-native-nfc-scanner

Android NFC scanning for React Native. Supports foreground and background scanning, works with both old and new React Native architecture.

> **Android only.**

[![npm version](https://img.shields.io/npm/v/@spo/react-native-nfc-scanner)](https://www.npmjs.com/package/@spo/react-native-nfc-scanner)
[![Platform](https://img.shields.io/badge/platform-Android-green)](https://developer.android.com/guide/topics/connectivity/nfc)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

---

## Installation

```sh
npm install @spo/react-native-nfc-scanner
```

---

## Setup

### 1. Register the package

**Kotlin** (`MainApplication.kt`):
```kotlin
import com.spo.nfcscanner.nfc.NfcManagerPackage

add(NfcManagerPackage())
```

**Java** (`MainApplication.java`):
```java
import com.spo.nfcscanner.nfc.NfcManagerPackage;

packages.add(new NfcManagerPackage());
```

---

### 2. Add lifecycle hooks to `MainActivity`

**Kotlin** (`MainActivity.kt`):
```kotlin
import android.content.Intent
import android.os.Bundle
import com.spo.nfcscanner.NfcActivityHelper

override fun onCreate(savedInstanceState: Bundle?) {
  super.onCreate(null)
  NfcActivityHelper.onCreate(this)
}
override fun onResume() {
  super.onResume()
  NfcActivityHelper.onResume(this)
}
override fun onPause() {
  super.onPause()
  NfcActivityHelper.onPause(this)
}
override fun onStop() {
  super.onStop()
  NfcActivityHelper.onStop(this)
}
override fun onNewIntent(intent: Intent) {
  super.onNewIntent(intent)
  setIntent(intent)
  NfcActivityHelper.onNewIntent(intent)
}
```

**Java** (`MainActivity.java`):
```java
import android.content.Intent;
import android.os.Bundle;
import com.spo.nfcscanner.NfcActivityHelper;

@Override
protected void onCreate(Bundle savedInstanceState) {
  super.onCreate(null);
  NfcActivityHelper.onCreate(this);
}
@Override
protected void onResume() {
  super.onResume();
  NfcActivityHelper.onResume(this);
}
@Override
protected void onPause() {
  super.onPause();
  NfcActivityHelper.onPause(this);
}
@Override
protected void onStop() {
  super.onStop();
  NfcActivityHelper.onStop(this);
}
@Override
public void onNewIntent(Intent intent) {
  super.onNewIntent(intent);
  NfcActivityHelper.onNewIntent(intent);
}
```

> NFC permissions and background components are merged into your app automatically. No `AndroidManifest.xml` changes needed.

---

## API

### Methods

| Method | Returns | When to use | Why |
|---|---|---|---|
| `startScanning()` | `Promise<void>` | When user enters a screen that needs NFC | Activates background dispatch so tags are detected even when the app is backgrounded |
| `stopScanning()` | `Promise<void>` | When user leaves the screen or logs out | Stops background dispatch to save battery and avoid unwanted reads |
| `isSupported()` | `Promise<boolean>` | On app launch or before showing NFC UI | Some devices have no NFC chip — check before doing anything NFC-related |
| `isEnabled()` | `Promise<boolean>` | Before calling `startScanning()` | NFC may be supported but turned off in settings — use this to decide whether to prompt the user |
| `goToNfcSetting()` | `Promise<boolean>` | When `isEnabled()` returns `false` | Opens the Android NFC settings screen so the user can turn it on without leaving the app manually |
| `addNfcListener(event, cb)` | `EmitterSubscription` | Before calling `startScanning()` | Registers a callback that fires every time a tag is scanned — always call `.remove()` on cleanup |

### Events

| Event | Payload | Description |
|---|---|---|
| `NfcTagScanned` | `NfcTagEvent` | Fired each time an NFC tag is successfully read |

### Types

```typescript
interface NfcTagEvent {
  tag: string;         // Android tag identifier string
  ndef: string;        // NDEF data string — empty string if the tag has no NDEF data
  payloads: string[];  // decoded UTF-8 text from each NDEF record
}
```

---

## Examples

### Basic scan on a screen

The most common use case — start scanning when the screen mounts, stop when it unmounts.

```tsx
import React, { useEffect, useState } from 'react';
import { View, Text, StyleSheet, Platform } from 'react-native';
import {
  startScanning,
  stopScanning,
  isSupported,
  isEnabled,
  addNfcListener,
} from '@spo/react-native-nfc-scanner';
import type { NfcTagEvent } from '@spo/react-native-nfc-scanner';

export default function ScanScreen() {
  const [lastTag, setLastTag] = useState<NfcTagEvent | null>(null);
  const [status, setStatus] = useState('Initialising...');

  useEffect(() => {
    if (Platform.OS !== 'android') return;

    const subscription = addNfcListener('NfcTagScanned', (tag) => {
      setLastTag(tag);
    });

    (async () => {
      const supported = await isSupported();
      if (!supported) { setStatus('NFC not available on this device'); return; }

      const enabled = await isEnabled();
      if (!enabled) { setStatus('NFC is turned off'); return; }

      await startScanning();
      setStatus('Ready — hold a tag near the device');
    })();

    return () => {
      stopScanning();
      subscription.remove();
    };
  }, []);

  return (
    <View style={styles.container}>
      <Text style={styles.status}>{status}</Text>

      {lastTag && (
        <View style={styles.result}>
          <Text style={styles.label}>Tag ID</Text>
          <Text>{lastTag.tag}</Text>

          <Text style={styles.label}>NDEF Payloads</Text>
          {lastTag.payloads.length > 0
            ? lastTag.payloads.map((p, i) => <Text key={i}>{p}</Text>)
            : <Text>No text payload</Text>
          }
        </View>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, padding: 24 },
  status:    { fontSize: 16, marginBottom: 24, color: '#555' },
  result:    { padding: 16, backgroundColor: '#f5f5f5', borderRadius: 8 },
  label:     { fontWeight: 'bold', marginTop: 12 },
});
```

---

### Prompt user to enable NFC

Check NFC state on mount and guide the user if it is off.

```tsx
import React, { useEffect, useState } from 'react';
import { View, Text, Button, Platform } from 'react-native';
import { isSupported, isEnabled, goToNfcSetting } from '@spo/react-native-nfc-scanner';

export default function NfcGate({ children }: { children: React.ReactNode }) {
  const [ready, setReady] = useState(false);
  const [message, setMessage] = useState('');

  useEffect(() => {
    if (Platform.OS !== 'android') { setReady(true); return; }

    (async () => {
      if (!(await isSupported())) {
        setMessage('This device does not support NFC.');
        return;
      }
      if (!(await isEnabled())) {
        setMessage('NFC is disabled. Please enable it to continue.');
        return;
      }
      setReady(true);
    })();
  }, []);

  if (ready) return <>{children}</>;

  return (
    <View style={{ flex: 1, justifyContent: 'center', alignItems: 'center', padding: 24 }}>
      <Text style={{ textAlign: 'center', marginBottom: 16 }}>{message}</Text>
      {message.includes('disabled') && (
        <Button title="Open NFC Settings" onPress={() => goToNfcSetting()} />
      )}
    </View>
  );
}
```

---

### Reusable hook

For apps that use NFC on multiple screens.

```tsx
// hooks/useNfc.ts
import { useEffect, useRef, useState } from 'react';
import { Platform } from 'react-native';
import {
  startScanning,
  stopScanning,
  isSupported,
  isEnabled,
  addNfcListener,
} from '@spo/react-native-nfc-scanner';
import type { NfcTagEvent } from '@spo/react-native-nfc-scanner';

type NfcStatus = 'checking' | 'ready' | 'unsupported' | 'disabled' | 'error';

export function useNfc(onTag: (tag: NfcTagEvent) => void) {
  const [status, setStatus] = useState<NfcStatus>('checking');
  const onTagRef = useRef(onTag);
  onTagRef.current = onTag;

  useEffect(() => {
    if (Platform.OS !== 'android') return;

    const sub = addNfcListener('NfcTagScanned', (tag) => onTagRef.current(tag));

    (async () => {
      try {
        if (!(await isSupported())) { setStatus('unsupported'); return; }
        if (!(await isEnabled()))   { setStatus('disabled'); return; }
        await startScanning();
        setStatus('ready');
      } catch {
        setStatus('error');
      }
    })();

    return () => { stopScanning(); sub.remove(); };
  }, []);

  return status;
}
```

```tsx
// Using the hook
import { useNfc } from './hooks/useNfc';

function MyScreen() {
  const status = useNfc((tag) => {
    console.log('Scanned:', tag.payloads);
  });

  if (status === 'unsupported') return <Text>NFC not available on this device</Text>;
  if (status === 'disabled')    return <Text>Please enable NFC in settings</Text>;
  if (status === 'ready')       return <Text>Hold a tag near the device</Text>;
  return null;
}
```

---

## Requirements

- React Native **0.70+**
- Android **API 21+**
- Physical Android device with NFC (does not work on emulators)

---

## License

MIT
