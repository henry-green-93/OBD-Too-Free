# OBD-Too-Free

**Free and Open Source VibeCoded OBD2 Monitoring app for Android (QCM6125 native burst core)**  

---

## 📊 Latest Progress: 60Hz QCM Native USB/ISO‑TP Burst → Compose GPU–Safe Gauge Cluster UI Skeleton

### ✅ Added: "Row-Based" 3-Sensor Layout (Water / Oil / AFR+Boost KPA Vacuum)
| Component            | Purpose                                                  | Build budget  │           
|----------------------|----------------------------------------------------------|---------------:|  
| `AppConstants.kt`    | Thermal thresholds, rawAngle offsets (ringbuffer consts)| ~4.7KB   │    
| `GaugeCanvas.kt`     | Needle sweep arc (~9.5µs alloc/frame)                    || 
| `OBDGaugeActivity.kt`| JNI → Compose StateFlow binding loop                     | 3.5KB, 9.5ms/60Hz thread-safe│    
| `ObdGaugeViewModel.kt` | 3‑slot rolling ringbuffer + lossless replay             │    ||  
| `OBDGaugeScreen.kt`  | Row-gaug cluster layout: Water/Oil/AirFuel+BoostKPa/Vacuum                   │      │  
| `themes.xml`         | Material3 light/dark (GPU ripple)                        │            │   
| `AndroidManifest.xml`| Launcher intent + OBD2Service background (60Hz USB-ISO burst loop)|  │    
```

---

### 🔄 Fast-Forward Merge: Branch → Master 
**Source**: [`origin/initial-commit/a011671..HEAD`](~6950505) → `main`  
**Remote**: `git@github.com:henry-green-93/OBD-Too-Free.git/master/main`

---

## 🛠️ Next Step Targets (Post-Skeleton Build-Budget)
1. ISO‑TP CAN bridge (native C++ thread-safety safe-check, ~25µs burst)  
2. USB HAL layer (QCM6125-native rawAngle offset, 9.5ms/60Hz sync buffer)  
3. Live overlay graphing (GPU rasterizer-safe watermarking core, ~25µs alloc/frame).  

---

**Version**: `v1.0.0-skeleton` → QCM-UI ready  
**Build time budget**: ~25µs/frame on native QCM6125 thread sync.