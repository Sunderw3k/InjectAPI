@SuppressWarnings("unused")
public class RedirectFieldTest {
    public static int thinStaticValue = 1;
    public int thinVirtualValue = 1;

    public static long wideStaticValue = 1L;
    public long wideVirtualValue = 1L;

    public void testGetVirtualThin() {
        if (thinVirtualValue != 0) {
            throw new AssertionError("Expected thin virtual value to be 0");
        }
    }
    public void testGetVirtualWide() {
        if (wideVirtualValue != 0L) {
            throw new AssertionError("Expected wide virtual value to be 0");
        }
    }

    public static void testGetStaticThin() {
        if (thinStaticValue != 0) {
            throw new AssertionError("Expected thin static value to be 0");
        }
    }
    public static void testGetStaticWide() {
        if (wideStaticValue != 0L) {
            throw new AssertionError("Expected wide static value to be 0");
        }
    }

    public void testSetVirtualThin() {
        thinVirtualValue = 1;

        if (thinVirtualValue != 0) {
            throw new AssertionError("Expected thin virtual value to be 0");
        }
    }
    public void testSetVirtualWide() {
        wideVirtualValue = 1L;

        if (wideVirtualValue != 0L) {
            throw new AssertionError("Expected wide virtual value to be 0");
        }
    }

    public static void testSetStaticThin() {
        thinStaticValue = 1;

        if (thinStaticValue != 0) {
            throw new AssertionError("Expected thin static value to be 0");
        }
    }
    public static void testSetStaticWide() {
        wideStaticValue = 1L;

        if (wideStaticValue != 0L) {
            throw new AssertionError("Expected wide static value to be 0");
        }
    }

    public static void testCaptureThinArgumentStatic(int a) {
        thinStaticValue = 1;
    }
    public static void testCaptureWideArgumentStatic(long a) {
        wideStaticValue = 1L;
    }

    public void testCaptureThinArgumentVirtual(int a) {
        thinStaticValue = 1;
    }
    public void testCaptureWideArgumentVirtual(long a) {
        wideStaticValue = 1L;
    }
}
