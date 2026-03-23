# @spo/react-native-nfc-scanner

> Android NFC scanner for React Native with support for **foreground** and **background** scanning.
> Works on both **old architecture** (legacy bridge) and **new architecture** (TurboModules).

[![npm version](https://img.shields.io/npm/v/@spo/react-native-nfc-scanner)](https://www.npmjs.com/package/@spo/react-native-nfc-scanner)
[![Platform](https://img.shields.io/badge/platform-Android-green)](https://developer.android.com/guide/topics/connectivity/nfc)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

---

## Features

- **Foreground scanning** — uses Android Reader Mode while the app is active
- **Background scanning** — registers an intent filter via `NfcDispatchActivity`, works when the app is backgrounded
- Automatic vibration feedback on tag detection
- Full **TypeScript** support
- Supports **React Native 0.70+** (old arch) and **0.74+** (new arch / TurboModules)
- Supports tag types: IsoDep, NfcA, NfcB, Ndef, NdefFormatable, MifareClassic, MifareUltralight, NfcV, NfcF

---

## Installation

```sh
npm install @spo/react-native-nfc-scanner
```

---

## Android Setup

### 1. Register the package — `MainApplication`

**Kotlin (`MainApplication.kt`):**

```kotlin
import com.spo.nfcscanner.nfc.NfcManagerPackage

override fun getPackages(): List<ReactPackage> =
  PackageList(this).packages.apply {
    add(NfcManagerPackage())
  }
```

**Java (`MainApplication.java`):**

```java
import com.spo.nfcscanner.nfc.NfcManagerPackage;

@Override
protected List<ReactPackage> getPackages() {
  List<ReactPackage> packages = new PackageList(this).getPackages();
  packages.add(new NfcManagerPackage());
  return packages;
}
```

---

### 2. Add NFC lifecycle hooks — `MainActivity`

These hooks enable foreground Reader Mode and route NFC intents to the library.

**Kotlin (`MainActivity.kt`):**

```kotlin
import android.content.Intent
import android.os.Bundle
import com.spo.nfcscanner.nfc.NfcController

class MainActivity : ReactActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(null)
    NfcController.init(this)
  }

  override fun onResume() {
    super.onResume()
    NfcController.enableReaderMode(this)
  }

  override fun onPause() {
    super.onPause()
    NfcController.disableReaderMode(this)
  }

  override fun onStop() {
    super.onStop()
    if (isFinishing || isChangingConfigurations) {
      NfcController.disableDispatchHandling()
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    NfcController.handleIntent(intent)
  }
}
```

**Java (`MainActivity.java`):**

```java
import android.content.Intent;
import android.os.Bundle;
import com.spo.nfcscanner.nfc.NfcController;

public class MainActivity extends ReactActivity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(null);
    NfcController.getInstance().init(this);
  }

  @Override
  protected void onResume() {
    super.onResume();
    NfcController.getInstance().enableReaderMode(this);
  }

  @Override
  protected void onPause() {
    super.onPause();
    NfcController.getInstance().disableReaderMode(this);
  }

  @Override
  protected void onStop() {
    super.onStop();
    if (isFinishing() || isChangingConfigurations()) {
      NfcController.getInstance().disableDispatchHandling();
    }
  }

  @Override
  public void onNewIntent(Intent intent) {
    super.onNewIntent(intent);
    if (intent != null) {
      NfcController.getInstance().handleIntent(intent);
    }
  }
}
```

> **Note:** The library automatically merges NFC permissions (`android.permission.NFC`, `android.permission.VIBRATE`) and the `NfcDispatchActivity` + `NfcLifecycleGuardService` entries into your app manifest via Android manifest merging. No manual manifest changes needed.

---

## Usage

### Quick start

```tsx
import React, { useEffect } from 'react';
import { Platform, Text, View } from 'react-native';
import {
  startScanning,
  stopScanning,
  addNfcListener,
  isSupported,
  isEnabled,
} from '@spo/react-native-nfc-scanner';

export default function App() {
  useEffect(() => {
    if (Platform.OS !== 'android') return;

    let subscription: ReturnType<typeof addNfcListener>;

    async function setup() {
      const supported = await isSupported();
      if (!supported) {
        console.warn('NFC not supported on this device');
        return;
      }

      const enabled = await isEnabled();
      if (!enabled) {
        console.warn('NFC is disabled — ask user to enable it');
        return;
      }

      subscription = addNfcListener('NfcTagScanned', (event) => {
        console.log('Tag scanned!');
        console.log('Raw tag:', event.tag);
        console.log('NDEF data:', event.ndef);
        console.log('Text payloads:', event.payloads);
      });

      await startScanning();
      console.log('NFC scanning started');
    }

    setup();

    return () => {
      stopScanning();
      subscription?.remove();
    };
  }, []);

  return (
    <View>
      <Text>Hold an NFC tag near the device</Text>
    </View>
  );
}
```

---

### Custom hook (recommended pattern)

```tsx
// hooks/useNfcScanner.ts
import { useEffect, useRef, useState } from 'react';
import { Platform } from 'react-native';
import {
  startScanning,
  stopScanning,
  isSupported,
  isEnabled,
  goToNfcSetting,
  addNfcListener,
} from '@spo/react-native-nfc-scanner';
import type { NfcTagEvent } from '@spo/react-native-nfc-scanner';

type Status = 'idle' | 'scanning' | 'unsupported' | 'disabled';

export function useNfcScanner(onTagScanned: (tag: NfcTagEvent) => void) {
  const [status, setStatus] = useState<Status>('idle');
  const callbackRef = useRef(onTagScanned);
  callbackRef.current = onTagScanned;

  useEffect(() => {
    if (Platform.OS !== 'android') return;

    let active = true;

    async function start() {
      const supported = await isSupported();
      if (!active) return;
      if (!supported) { setStatus('unsupported'); return; }

      const enabled = await isEnabled();
      if (!active) return;
      if (!enabled) { setStatus('disabled'); return; }

      const sub = addNfcListener('NfcTagScanned', (event) => {
        callbackRef.current(event);
      });

      await startScanning();
      if (!active) { stopScanning(); sub.remove(); return; }

      setStatus('scanning');

      return () => { stopScanning(); sub.remove(); };
    }

    const cleanup = start();

    return () => {
      active = false;
      cleanup.then((fn) => fn?.());
    };
  }, []);

  return { status, goToNfcSetting };
}
```

```tsx
// Using the hook in a screen
import { useNfcScanner } from './hooks/useNfcScanner';

export function ScanScreen() {
  const { status, goToNfcSetting } = useNfcScanner((tag) => {
    console.log('Scanned:', tag.payloads);
  });

  if (status === 'unsupported') return <Text>NFC not available on this device</Text>;
  if (status === 'disabled')    return <Button title="Enable NFC" onPress={goToNfcSetting} />;
  if (status === 'scanning')    return <Text>Ready — hold a tag near the device</Text>;
  return <Text>Starting...</Text>;
}
```

---

## API Reference

### `startScanning(): Promise<void>`

Enables NFC dispatch and foreground reader mode. Tags scanned while active will fire the `NfcTagScanned` event.

### `stopScanning(): Promise<void>`

Disables NFC dispatch and stops listening. Always call this when the screen unmounts.

### `isSupported(): Promise<boolean>`

Returns `true` if the device has NFC hardware. Returns `false` if NFC chip is absent.

### `isEnabled(): Promise<boolean>`

Returns `true` if NFC is currently turned on in device settings.

### `goToNfcSetting(): Promise<boolean>`

Opens the Android NFC settings screen so the user can enable NFC.

### `addNfcListener(event, callback): EmitterSubscription`

Subscribes to NFC events. Returns a subscription — **always call `.remove()`** when done.

| Event | Callback payload | Description |
|---|---|---|
| `NfcTagScanned` | `NfcTagEvent` | Fired each time a tag is successfully scanned |

---

## Types

```typescript
interface NfcTagEvent {
  /** Raw Tag.toString() from Android — contains tag ID and tech types */
  tag: string;

  /** Raw Ndef.toString() — empty string if the tag has no NDEF data */
  ndef: string;

  /** Decoded UTF-8 text payloads extracted from NDEF records */
  payloads: string[];
}

type NfcEvent = 'NfcTagScanned';
```

---

## Architecture support

| React Native version | Architecture | Works? |
|---|---|---|
| 0.70 – 0.73 | Old arch (legacy bridge) | ✅ |
| 0.74+ | New arch (TurboModules) | ✅ |
| 0.74+ | Old arch (bridge mode) | ✅ |

The library automatically uses the correct native module based on the architecture enabled in the consuming app's `gradle.properties` (`newArchEnabled=true/false`).

---

## How foreground vs background scanning works

| Mode | How it works | When active |
|---|---|---|
| **Foreground (Reader Mode)** | `NfcAdapter.enableReaderMode()` in `onResume` | App is in foreground |
| **Background (Dispatch)** | `NfcDispatchActivity` receives NFC intents | App is backgrounded, `startScanning()` was called |

Background scanning is **opt-in** — the `NfcDispatchActivity` component starts disabled and is only enabled when `startScanning()` is called from JavaScript. It is automatically disabled when `stopScanning()` is called or when the app is removed from recents (via `NfcLifecycleGuardService`).

---

## Troubleshooting

**"NfcManager module not found"**
- Did you add `NfcManagerPackage()` to `MainApplication`?
- Did you run `npx react-native run-android` after installing?

**Tags not detected in foreground**
- Did you add the `onResume`/`onPause` hooks to `MainActivity`?
- Is NFC enabled on the device? Call `isEnabled()` to check.

**Tags not detected in background**
- Did you call `startScanning()` before backgrounding the app?
- Check that `NfcDispatchActivity` is in your merged manifest (run `./gradlew mergeDebugManifests` and inspect the output).

**App crashes on iOS**
- Wrap all calls with `if (Platform.OS === 'android')` — this library is Android only.

---

## License

MIT © Santosh Kumar
