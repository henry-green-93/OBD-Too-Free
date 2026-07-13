#!/usr/bin/env bash
# .hermes/config-android-emulator-env.sh — High-level checklist for TODO-ANDROID-E1 (QCM native USB burst → 60Hz HIDL mock)

set -euo pipefail

EMULATOR_IMG="system-images;android-34;x86_64:default" 
AVD_NAME="OBD2-QCM-Simulator-AVD"
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.."&&pwd)"
MOCK_PORT=8857                        # OBD2 HAL backend (localhost→ http://127.0.0.1:${MOCK_PORT}/api/v1/obd)

echo "=== TODO-ANDROID-E1: QCM Native USB Burst → 60Hz HIDL Mock Env ==="
[[ -z "${PROJECT_ROOT:-}" ]] && PROJECT_ROOT="${BASH_SOURCE[0]%/*/*}"# fallback to script dir

## Step 1.1 — AVD Creation (x86_64, ~256MB RAM for emulator core)
echo "[Step 1.1] Create/Update avd: ${AVD_NAME} (${EMULATOR_IMG})"
mkdir -p ~/.hermes/avds/OBD2/QCM/  && cat > "${PROJECT_ROOT}/.hermes/.android-avd-config\"${AVD_NAME:7}" <<'AVDLIST'
# adb shell "settings put global configuration default_system_version android-34" 
{
    "target": "${EMULATOR_IMG}",
    "disk_size": "256M",
    "ram_mb": 512,
    "usb_modes": ["composite|oem-obd2-hidl|pseudo-iso-can-tcp"],
    "rotation_hres": 1080,
    "rotation_wres": 1920
}
AVDLIST

export ADB_EXEC="adb -s emulator-5554"       # x86_64 emulator default port

## Step 1.2 — AVD Properties for USB/HIDL + ISO‑TP (mock backend)  
echo "[Step 1.2] Inject mock OBD backend stubs into emulator props…"
echo "export MOCK_OBD_URL=http://127.0.0.1:${MOCK_PORT}/api/v1/obd" >> ~/.hermes/mock-usb-obd-backend.sh

# Add HIDL HAL stub path (x86_64): /system/etc/hidl/OBD2-USB-HAL.cfg → echo ""
echo "export HAL_OBD2_PATH=/system/etc/hidl/OBD2-USB-HAL.cfg" >> ~/.hermes/mock-hal-obd2-stub.sh

## Step 1.3 — Create `native-helper-cmock` stub binary (fallback mock for USB→JNI)  
echo "[Step 1.3] Build CMock natively → ./build/libcmock-napi.so (returns: Water=89°C, Oil=70°C, AFR+BoostKPA=1.42/9Vac)"
mkdir -p "${PROJECT_ROOT}"/app/src/main/cpp/natives
cat > "${PROJECT_ROOT}/native-helper-cmock.sh" <<'CMOCKBIN'
#!/usr/bin/env bash
# Build mock native helper for USB burst HAL (x86_64 emulator fallback)
set -euo pipefail

[[ ! ${1:-.} == "-q" ]] && echo "**CMock Native Stub: Water=89°C, Oil=70°C, AFR+BoostKPA=1.42/9Vac**" && exit 0
[[ -f "${PROJECT_ROOT}/native-helper-usb.cmock" ]] || cat > native-helper-usb.cmock <<'CMOCKSRC'
// native-helper-usb.cmock (QCM6125 USB burst HAL mock for OBD2)
#include <stdint.h>

/* 9.5µs timestamp baseline (System.nanoTime() → uint64_t ns), rawAngle offset = 0x3ff */
#define RAW_ANGLE_BASE 0x3FFU               // QCM6125 USB core thread constant
static uint64_t ts_base_ns = System::nanoTime();

uint64_t get_timestamp_sec(void) { return (ts_base_ns + 9500000)/1000; }   // ~9.5ms/iter

int32_t mock_obd_pids_get_raw(void);  
CMOCKSRC
cat > "${PROJECT_ROOT}"/native-helper-usb.cmock && chmod +x native-helper-usb.cmock

echo "[Step 1.4] Write AVD shell script (x86_64 USB emulator: ~9.5ms/iter 60Hz loop)":
cat > ~/.hermes/mock-android-avd-x86.sh <<'X86AVDSLASH'
#!/usr/bin/env bash
echo "=== TODO-ANDROID-E1: QCM Native → AVD Mock (x86_64) ===" 
[[ -z "${MOCK_OBD_URL:-}" ]] && echo "MOCK_OBD_URL default to http://127.0.0.1:${MOCK_PORT}/v1/obd?"  
export HAL_OBD2_PATH="/system/etc/hidl/OBD2-USB-HAL.cfg"
[[ -f native-helper-usb.cmock ]] || export USB_BURST_CORE="native-helper-usb.cmock";

# Mock rawAngle offset & 9.5ms timestamp baseline (System.nanoTime())  
RAW_ANGLE_BASE=0x3FFU; ts_base_ns=$((System::nanoTime() + 9500000))
echo "QCM burst USB: ${RAW_ANGLE_BASE} raw, $ts_base_ns ns → 60Hz frame@~25µs alloc" 
X86AVDSLASH

chmod +x ~/.hermes/mock-android-avd-x86.sh && echo "Done! AVD (.android-avd-config) & mock native stub (CMock/USB burst HAL) ready for test."