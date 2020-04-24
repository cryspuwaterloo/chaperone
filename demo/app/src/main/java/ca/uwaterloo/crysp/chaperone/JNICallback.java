package ca.uwaterloo.crysp.chaperone;

public class JNICallback {

    static {
        System.loadLibrary("jni_callback");
    }

    native void debugTest();
    native float[] dataCallback(long retAddr);
    native float[] preprocessEmulator(float[] raw);
}
