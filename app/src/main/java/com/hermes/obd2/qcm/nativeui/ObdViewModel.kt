package com.hermes.obd2.qcm.nativeui

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Data model for the 3 primary OBD2 PIDs for the QCM6125 test.
 */
data class ObdData(
    val waterTemp: Int = 0,
    val oilTemp: Int = 0,
    val afrBoost: String = "0.0, 0Vac"
)

object ObdViewModel {
    // StateFlow for the UI to observe
    private val _obdState = MutableStateFlow(ObdData())
    val obdState: StateFlow<ObdData> = _obdState

    /**
     * Mock function to update data. 
     * In the final app, this will be called by the JNI/Native loop at 60Hz.
     */
    fun updateData(water: Int, oil: Int, afr: Double, boost: Int) {
        _obdState.value = ObdData(
            waterTemp = water,
            oilTemp = oil,
            afrBoost = "%.2f, %dVac".format(afr, boost)
        )
    }
}