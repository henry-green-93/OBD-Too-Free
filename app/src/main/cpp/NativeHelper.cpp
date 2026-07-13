#include <jni.h>
#include <cmath>
#include <ctime>

extern "C" JNIEXPORT void JNICALL
Java_com_hermes_obd2_qcm_nativeui_ObdViewModel_updateData(
    JNIEnv* env, 
    jclass clazz, 
    jint water, 
    jint oil, 
    jdouble afr, 
    jint boost) {
    
    // In a real app, this logic would be called by a 60Hz C++ thread
    // For now, this is the JNI entry point for the Kotlin ViewModel
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_hermes_obd2_qcm_nativeui_ObdViewModel_getNativeTimestamp(
    JNIEnv* env, 
    jclass clazz) {
    return (jlong)std::time(nullptr);
}
