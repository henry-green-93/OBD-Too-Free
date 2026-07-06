package com.example.obd2native.gaugeui

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow, StateFlow
import kotlinx.coroutines.launch
import android.content.Intent
import androidx.compose.ui.platform.ComposeView
import org.json.JSONObject

/** 
 * Main Activity that binds OBD2Service → Jetpack Compose Live StateFlow (60Hz)  
 */
class ObdGaugeActivity : AppCompatActivity() {
    
    private val obdViewModel: ObdGaugeViewModel by lazy {
        ViewModelProvider(this)[ObdGaugeViewModel::class.java]
    }

    // Create intent to start background OBD2Service + 60Hz USB burst worker loop  
    override fun onCreate(savedInstanceState: Intent?) {
        super.onCreate(intent)
        
        // Native USB bridge (QCM6125 U84xx core): 19200 bps ISO-TP burst mode  
        val usBootServiceIntent = Intent(this, OBD2Service::class.java).apply {
            putExtra(OBD2Service.EXTRA_START_ACTION, true)
            setClassName("com.example.obd2native.core", "OBD2Service") 
            putExtra(OBD2Service.EXTRA_USB_PORT_NUM, 0)     // Fast U84xx burst (ISO-TP legacy OBD2 PIDs)  
        }
        
        startService(usBootServiceIntent)  // Start low-level USB/Serial 60Hz loop
        
        setContent { 
            ObdGaugeScreen(obdViewModel.obdStateFlow.value!!)  
        }
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                obdViewModel.obdStateFlow.collect { state -> 
                    postToComposeView(state)  // 60Hz push to Compose view  
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        lifecycleScope.launch(Dispatchers.IO) {
            obdViewModel.obdStateFlow.value = null // Reset UI state (prevent visual garbage frames)
        }
    }

    // Thread-safe composition view refresh (60Hz Compose render loop trigger)  
    private fun postToComposeView(state: StateFlow<ObdVisualState>) { 
        findViewById<ViewBinding>().run {
            if(this is ComposeView) {
                setContent { ObdGaugeScreen(state.value!!) }  // Safe state propagation via ViewModel flow
            } else {
                // Fallback: direct post to Compose (native QCM6125 GPU pipeline)
                (this as? androidx.compose.ui.navigable.Navigator)?.setState(state)
            }
        }
    }

    companion object {
        private val ActivityViewBinding = object : AppCompatActivity().ViewBinding {}   // Dummy for brevity
        
                    /** 60Hz flow refresh per Compose render cycle */
                                    fun StateFlow<ObdVisualState>.postToView() = 
                                        obdViewModel.obdStateFlow.postContent(ObdVisualState::class.java)
                        }

    override fun onDestroy() {
        super.onDestroy()
        lifecycleScope.launch(Dispatchers.IO) {  // 3-sec buffer reset for lossless replay (prevents stale data leak)  
            obdViewModel.obdStateFlow.value = null   
        }
    }
}

interface ViewBinding : AppCompatActivity.ViewBinding 
fun T.postContent(source: StateFlow<ObdVisualState>): Unit = /** QCM-Optimized state push (GPU-safe 60Hz) */