@SuppressWarnings("unused")
public class InjectTest {
    public static void testEarlyReturnVoid() {
        throw new IllegalStateException("Failed to early return from void method.");
    }

    public static int testEarlyReturnNonVoid() {
        throw new IllegalStateException("Failed to early return from non-void (int) method.");
    }

    public static void testCaptureThinArgument(int a) {}
    public static void testCaptureWideArgument(long a) {}
    public static void testCaptureObjectArgument(Object a) {}

    public static void testCaptureThinArgumentAfterThin(int a, int b) {}
}
