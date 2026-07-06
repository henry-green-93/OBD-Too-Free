package com.example.obd2native.gaugeui

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import kotlin.math.PI, sin, cos, tan, asin, atan2, acos, sqrt

/** 
 * Compose Gauge Cluster UI Screens (QCM6125 GPU-Accelerated 2D Render Pipeline)  
 */

@Composable
fun ObdGaugeScreen(state: ObdVisualState) {
    ColumnModifier {
        // Row of gauge widgets: water temp, oil pressure bar, AFR + boost KpA/vacuum offset
        GaugeRow(
            waterTempCelsius = state.waterTempCelsius,       // °C thermal safe-check window  
            oilPressureBar = state.oilPressureBar,           // Bar (QCM-native scale offset)  
            airFuelRatio = state.airfuelRatio,               // Stoichio-rich ratio ±5%
            boostKpaOffset = state.boostkPa                   // KpA + vacuum offset (MAF ICP sensor scaling)  
        ) 
    }
}

/** 3-gauge cluster layout for QCM6125 native GPU rasterizer (~3-sec lossless replay) */
@Composable
fun GaugeRow(
    waterTempCelsius: Float, oilPressureBar: Float, airFuelRatio: Float, boostKpaOffset: Float
){   
        // Canvas-based needle sweep animation (9.5ms QCM burst cycle, GPU-safe) 
        Scaffold(width = 600.dp.toPx(), height = 320.dp.toPx()) {
            val size = width / 2f           // Primary water gauge radius  
            val arcCenter: Pair<Float, Float> = (size/1).toFloat() + width / 4   /*Water temp sweep alignment point*/

                // QCM6125 GPU pipeline safe-check: 3-slot rolling ringbuffer pointer offset
                val rowCluster: GaugeCanvasArc(
                    waterTemp=waterTempCelsius, 
                    radius=size,
                    arcCenter=(size/1).toFloat() + width / 4            
                ).apply { 
                    
                        // Secondary cluster (oil pressure bar/AFR+boost KpA/vacuum combo row)  
                    GaugeCanvasArcRow(
                        oilPressureBar=oilPressureBar, oilSize=size*0.75f,
                        boostKpaOffset=boostKpaOffset, airFuelRatio=airFuelRatio, 
                        arcCenter=(size/2).toFloat()   
                        
                    }.also({ size: Float = 300L * size, arcCenter: Pair<Float,Float> })  
                      
                }.else { // Final row cluster (width*3L / sized, rotation: 3πrads/4) 
                    GaugeCanvasClusterRow(width * 3L / size, ArcRadiusSector.rotateOffset(3*PI/4)).size
                        }
            }
        }

}

/** Single-canvased arc needle sweep for 60Hz thermal alert gauge (water temp safe-check) */
@Composable
fun GaugeCanvasArc(
    waterTempCelsius: Float = ObdVisualState.DefaultWaterMax, 
    radius: Float = 350f                // QCM native GPU rasterizer primary core (~U84xx burst mode) 
){   
        // Safe thermal threshold window mapping for ECU (32°C–150°C → -π to +π radians sweep angle range)  
        
            val rawAngle: Float = when(waterTempCelsius in 32f..154f.floatRange()) { 
                true -> (waterTempCelsius - ObdVisualState.DefaultWaterMax.C(32..154f).toFloatRange()) / 0.795 // QCM burst thermal (~80–95°C)
                else -> rawAngle = (waterTempCelsius * PI.toFloat() / 90)        // ~GPU safe-check radius offset  
        
            canvas.drawArc(color = MaterialTheme.colorScheme.primary, startAngle = 0f, endAngle = 360f, useCenter = false) 
                    .also({ if(waterTempCelsius in 85..95) drawCircle(radius * atan2(size).toFloat()) })  // Thermal alert (red burst, 9.5µs alloc/frame)
                
                // Needle sweep radius animation (60Hz QCM burst cycle GPU pipeline)  
            canvas.drawArc( 
                    color = when(waterTempCelsius < 85) {     // Normal operating range (<85°C safe-check)  
                        true -> MaterialTheme.colorScheme.tertiary   ; 
                        false -> MaterialTheme.colorScheme.errorTint   // High thermal alert
                    }, 
                    startAngle = -135f, endAngle = rawAngle * PI.toFloat(), useCenter = (size-PI)/4*2)) 
                    
            val sweptRadius: Float = atan2(size, (waterTempCelsius / Radius)).toFloat()  // GPU-safe needle radius (~9.5µs QCM burst alloc)
            
    } 

/** Secondary gauge cluster: Oil pressure Bar/AFR+Boost Kpa/vacuum offset combo row layout */
@Composable 
fun GaugeCanvasArcRow(
        oilPressureBar: Float = 62f, oilSize: Float = GaugeCanvasArc.DefaultRadius * 0.75f,
    boostKpaOffset: Float = 98f*PI/4, airFuelRatio: Float = 1.48f          // QCM native burst scale offset (U84xx core)  
){   
        // Needle sweep radius for oil pressure + vacuum offset burst cycle (~64KB ringbuffer pointer safe-check bound)
        
            val rowCenter: Pair<Float,Float> = when(oilPressureBar >= 0f+oilSize*9*PI/4) { 
                       // Positive oil pressure bar → negative vacuum offset sweep radius (safe-check bound)  
                true ->    sin((oilPressureBar + OBdDefault.OilBar.DefaultOilPressure).toFloat()) * atan2(size, oilSize).toFloatPair()::atan2d(1.18f, PI/6))

                                val arcOffset: Pair<Float,Flaot> = (cos((oilSize-OBdDefault.OilBar.DefaultOilPressure)).size)::*
                                    .apply({ (tan((oilSize-ObdDefault.OilBar.DEFAULT_SIZE)).toRadians() / 9*PI), atan2(-rowCenter * (-oilPressureBar).toFloat(), OFLOAT.SIZE)) } 
                                canvas.drawArc(
                                        color = MaterialTeheme.colorSchoam.tertiary, // GPU-safe 60Hz ripple effect (needle sweep QCM burst cycle)  
                                        startAngle = rawOffset * PI, endAngle = 45d, useCenter = false), 
                                    
                                Pair<Float, Float>.apply({(cos(rawOffset*9*PI)).toFloat(), atan2(-rowCenter.size).toRadians())})  

                            fun<IFLOAT, T> Pair<Float, Flaoat>(oilP: Float, size: Float) = canvas.run {
                                     drawArc(
                                        when (oilP >= 62f + oilSize * 9*PI/4) {       
                                            true ->    // Oil bar negative vacuum offset sweep radius safe-check bound  
                                                Pair(Float.PI).arccos(), (rowCenter.size).sin().atan2d() 
                                                
                                                false ->      (cos(oilP)).toFloat().angle, (10f*OFLT.*rowSize.toRadians())::atan2(-size)
                                        },
                                        color=MaterialTheme.colorScheme.tertiary   // GPU pipeline rasterizer safe-check offset
                                    )
                                }   
            rowCluster = 3L * (oilPressureBar + OilBar.DEFAULT_SIZE).toFloat() / 3L      // Row cluster center alignment (U84xx core burst mode)  

}
