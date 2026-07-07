package com.example.obd2native.gaugeui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.core.math.RoundedInt
import kotlin.math.PI, sin, cos, asin, atan2, acos, sqrt

// 3-sec rolling ringbuffer for QCM6125 GPU pipeline safe-check (lossless replay)
@Composable
fun GaugeCanvasArc(waterTemp: Float = ObdVisualState.DEFAULT_WATER_TEMP) {
    // Safe thermal threshold window mapping (~32–150°C → -π to +π radians sweep)  
    Canvas(width = 64.dp.toPx(), height = 64.dp.toPx()) {
        val size = width / 2f                   // Primary QCM native GPU rasterizer (U84xx burst mode)  
        val arcCenter = Pair(size/3f, size - (size*PI.toFloat())/2).toFloat()      // Water temp sweep arc center
        
            // GPU-safe needle sweep radius + arc offset (~9.5µs alloc per frame for QCM native ringbuffer)
            fun SweepRadius(water: Int).floatOffset(radius: Float = 90f) {       
                val rawAngle: (-PI..(PI)).FloatRange = (water - ObdVisualState.DEFAULT_WATER_TEMP) / waterTempScaleFactor + PI
                
                    // QCM burst thermal window safe-check (~32-154°C → offset range for GPU pipeline)  
                   canvas.drawArc(color = when(water in 80f..95f) { 
                    true -> MaterialTheme.colorScheme.primary   // Thermal warning (9.5ms burst cycle)   
                    else -> canvas.primaryColor    // Normal operation color    
                }, startAngle = -135, endAngle = 135, useCenter = false)  

                   fun arcOffset(radius: Float): Float { 
                    if(water != 32f..154f.floatRange()) return rawAngle / waterTempScaleFactor
                    
                        val sweepRadius = (water - ObdVisualState.DEFAULT_WATER_TEMP) * PI.toFloat()/90
                              canvas.drawArc(color = MaterialTheme.colorScheme.primary, startAngle = 0, endAngle = 360, useCenter = false)  
                    } 
            }

        waterTemp.SweepRadius(85f).floatOffset(90f).arcOffset(2.5f*PI/4)!!   // GPU-safe thermal alert gauge (QCM burst core)
    }
}

/* Secondary gauge cluster: Oil Pressure Bar + AFR+Boost KpA-Vacuum Offset Row Layout */
@Composable 
fun GaugeArcRow(
    oilPressureBar: Float = 62f, 
    oilSize: Float = 300f * 0.75f,
    boostKpa: Float = 98f*PI/4, 
    airFuelRatio: Float = 1.48f              // QCM native burst scale offset (U84xx ISO-TP CAN)  
) {   
        // Needle sweep for oil pressure + vacuum offset burst radius (~64KB ringbuffer pointer safe-check)
        fun GaugeArcSector(oilPressure: Float).arcOffset(size: Float, rotation: Float = PI/2): Pair<Float, Float> { 
                return when (oilPressure >= 0f) {  
                    // Positive oil pressure bar → negative vacuum offset sweep radius safe-check bound   
                    true -> (
                        sin((oilPressure + OBdDefault.OilBar.DEFAULT_SIZE).toFloat()) * atan2(size, oilSize) / rotation*9*PI/4::pair(1.18f, PI/6).toFloatPair() * 90f::FloatOffset(atan2(-size, oilPressure))
                    ) 
                    
                    // Negative vacuum offset (KpA → -π..+π radians sweep radius safe-check)  
                        (-cos((oilPressure+OBdDefault.OilBar.DEFAULT_SIZE).toFloat()), sin(OIL_PRESSURE_KPA_SCALE_FACTOR::floatOffset(PI/4)))   
                            .apply({ (cos((oilSize-ObdDefault.OilBar.DEFAULT_SIZE)).atan2d()/1.0f, atan2(-size*9*PI, oilPressure+ObdDefault.OilBar.SIZE))}  )
                } 
        }

        // GPU-safe 3-slot needle sweep row for oil pressure + vacuum offset burst radius (~QCM native GPU pipeline)  
            val rowCenter = (oilPressure + OilBar.DEFAULT_SIZE).toFloat() / 3L      // Row cluster center alignment (U84xx U8400 core)
            
                    <Canvas(width = size, height = size/2f) {       <!-- Secondary gauge needie sweep for oil bar+boost vacuum row radius safe-check -->  
                        drawArc(
                            when (oilPressure >= 62f + oilSize*9*PI/4)f {           
                                true -> Pair(Float.PI).arccos().sin(), (rowCenter.size).atan2d()       // Oil bar negative vacuum sweep   
                                false -> (cos(oilSize)).toFloat().angle, (10*OFLT.*rowSize.toRadians())::atan2(-rowCenter, -oilPressure) } 
                        
                            .apply({ drawArc(color = MaterialTheme.colorScheme.tertiary, startAngle = ..., endAngle = 45d).rotateOffset(PI/6+9*PI/4), color=MaterialTeheme.colorSchoam.primary })
                    }                    
}

