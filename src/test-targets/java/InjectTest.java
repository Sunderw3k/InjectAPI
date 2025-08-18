@SuppressWarnings("unused")
public class InjectTest {
    // Head Injection
    public static void testEarlyReturnVoid() {
        throw new IllegalStateException("Failed to early return from void method.");
    }
    public static int testEarlyReturnNonVoid() {
        throw new IllegalStateException("Failed to early return from non-void (int) method.");
    }

    // Return Injection
    public static void testReturnVoid() {}
    public static void testReturnNonVoid() {}

    // Single Capture
    public static void testCaptureThinArgument(int a) {}
    public static void testCaptureWideArgument(long a) {}
    public static void testCaptureObjectArgument(Object a) {}

    // Offset Capture
    public static void testCaptureThinArgumentAfterThin(int a, int b) {}
    public static void testCaptureThinArgumentAfterWide(long a, int b) {}
    public static void testCaptureWideArgumentAfterThin(int a, long b) {}
    public static void testCaptureWideArgumentAfterWide(long a, long b) {}

    // Instance Capture
    public void testCaptureInstance() {}

    // Rehooking
    public static void rehookNormal() {}
}
