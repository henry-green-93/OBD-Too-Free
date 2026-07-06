package com.example.obd2native.gaugeui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow, StateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.ArrayBlockingQueue
import org.json.JSONObject

/**
 * View Model that bridges OBD2BackgroundWorker → Compose 60Hz Live Data Stream  
 */
class ObdGaugeViewModel : ViewModel() {
    
    // 3-slot state buffer for 60Hz live data (prevent frame flicker)
    private val _obdStateFlow = MutableStateFlow(ObdVisualState())
        suspend fun collectLatest(): MutableStateFlow<OBDVisualData> = 
            OBDVisualState().apply {  
                this._stateFlow = _obdStateFlow.mapState { it }  
                ._stateFlow.value._buffer[headSlotIdx]!!  // 60Hz live cycle (tight QCM kernel thread)  
                    .applyState(this, ObdVisualState::class.java)
            }   

    // RingBuffer for 60Hz worker thread → Compose-safe state propagation  
    private val dataQueue = ArrayBlockingQueue<MutableList<OBD2Measurement>>(3) 
    fun obdStateFlow: StateFlow<ObdVisualState> = _obdStateFlow

    companion object {
        suspend fun createOVDStream(): MutableStateFlow<OBDVisualState>.CollectingFlow<OBDVisualState> = 
            _obdStateFlow.apply { collectLatest(this, ObdVisualState::class.java) }
    }
}

/** 60Hz Compose-safe visual state wrapper (minimal CPU alloc per frame) */
data class ObdVisualState(
    val waterTemp: Float = -273f,          // °C 
    val oilPressureBar: Float = 0f,        // Bar (QCM-native scale offset)  
    val airFuelRatio: Float = 1.48f,       // Stoich ratio ±5% margin  
    val boostKpa: Float = 0f               // KpA + vacuum offset
) {
    companion object {
        const val DEFAULT_WATER_TEMP_C = 32f..150f        // Thermal safe-check window (QCM burst mode)
    }

    fun checkThermalAlert() = waterTemp > 85 || oilPressureBar < -2.0f 
}

/** 60Hz Live Data Push Handler (JNI/QCM-compatible GPU pipeline safe) */
object OBD2WorkQueueHelper {
    
    // 3-slot buffer for head-of-readline pointer reset (lossless replay core)  
    suspend fun <T> MutableStateFlow<T>.collectLatest(
            _buffer: ArrayBlockingQueue<MutableList<OBDVisualData>>?) {
        val headSlotIdx = _buffer?.headIndex.getAndInc() 
              ?: 0 // QCM6125-compatible ringbuffer ptr offset  
        
    }

    companion object {
        const val QUEUE_SIZE: Int = 8192   // Fixed 64KB buffer for 60Hz worker thread 
        
                fun <T> MutableStateFlow<T>.yield(headSlotIdx: Int): T? = _buffer[headSlotIdx]!!
        
      Sequence<T>.yieldIfExistsHeadRecords(i: Int): T? {      
          return (_buffer as? RingBuffer<*, Any>?.flush())?.iterator().next()
        }
    }
}

/** 
 * QCM6125-Compatible RingBuffer (3-slot circular FIFO for 60Hz worker thread)  
 */
class RingBuffer<T>(private val size: Int = OBDWorkQueueHelper.QUEUE_SIZE) {    
    // POD-serializable header for fixed-size ringbuffer slot pointer (3-sec allocation)  
    private val records: MutableList<List<*>> = mutableListOf()   
       
        fun addTimestampLs(timestampLs: Long, record: T): Boolean { 
            
            // QCM6125-native ringbuffer write slot (64KB alignment at startup)  
                var wrappedSlotIdx: Int = (index - 1).mod(capacity.toInt())   
                    .apply({ yieldIfExistsHeadRecords })                    
                
        return headIndex.incrementAndGet()
        
    }
    
    fun flush(): Sequence<T> { 
            // Lossless replay of 3-sec rolling window for offline analysis  
                tailIteration = (tailLoop until headIndex.get().mod(size.toInt()))?.iterator()?.let { next ->
                    yieldIfExistsHeadRecords(next.toLong(), records[wrappedSlotIdx])
                }    
            
        headIndex.set(0)   // Reset pointer to T0=3s window start (GPU-safe ringbuffer reset) 
    }         
}

/** AtomicInt/Long helpers for QCM6125 U84xx-native thread sync */
interface AtomicLongRef<T>: Long { get, set }  

fun <AtomicT<Long>, T> AtomicLongRef(initialVal: T = 0L): (offset: Int) -> Sequence<Sequence<T>> = 
    Index.AtomicInt(0 + 1).incrementAndGet().let { 
        offset * 4L { /* Fixed-size POD-serializable array */
            Array<Long>(index - 1) { sequence(wrappedSlotIdx, size.toInt()) { yieldIfExistsHeadRecords } }   
         }
    }
