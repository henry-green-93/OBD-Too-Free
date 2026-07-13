package com.hermes.obd2.qcm.nativeui

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ObdData(
    val waterTemp: Int = 0,
    val oilTemp: Int = 0,
    val afrBoost: String = "0.0, 0Vac"
)

class ObdNumericViewModel : ViewModel() {
    private val _obdState = MutableStateFlow(ObdData())
    val obdState: StateFlow<ObdData> = _obdState

    init {
        // Start the 60Hz loop (approx 16.6ms per frame, but we're aiming for ~9.5ms)
        // We'll use 16ms for a smooth 60Hz on the emulator.
        viewModelScope.launch {
            while (true) {
                // Call the native C++ mock data fetcher
                fetchNextMockData()
                
                // Update the state flow
                _obdState.value = ObdData(
                    waterTemp = waterTemp,
                    oilTemp = oilTemp,
                    afrBoost = afrBoost
                )
                
                delay(16) // 60 FPS
            }
        }
    }

    // JNI Declarations
    external fun updateData(water: Int, oil: Int, afr: Double, boost: Int)
    external fun getNativeTimestamp(): Long
    external fun fetchNextMockData()

    // Local variables to store values from NativeHelper
    private var waterTemp = 0
    private var oilTemp = 0
    private var afrBoost = "0.0, 0Vac"

    // This is a bridge function to handle the JNI callback
    // Note: In a standard JNI setup, we'd use a callback or a listener, 
    // but here we'll simplify by updating local variables via the native call.
    // Since the native call 'fetchNextMockData' calls 'updateData' internally,
    // we need a way to bring those values back to Kotlin.
}
