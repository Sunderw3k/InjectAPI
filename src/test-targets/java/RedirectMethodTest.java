@SuppressWarnings("unused")
public class RedirectMethodTest {
    public static int thinStaticMethod() { return 1; }
    public int thinVirtualMethod() { return 1; }

    public static long wideStaticMethod() { return 1L; }
    public long wideVirtualMethod() { return 1L; }

    public static void testRedirectStaticThin() {
        if (thinStaticMethod() != 0) {
            throw new AssertionError("Expected thin static method to return 0");
        }
    }

    public void testRedirectVirtualThin() {
        if (thinVirtualMethod() != 0) {
            throw new AssertionError("Expected thin virtual method to return 0");
        }
    }

    public static void testRedirectStaticWide() {
        if (wideStaticMethod() != 0L) {
            throw new AssertionError("Expected wide static method to return 0");
        }
    }

    public void testRedirectVirtualWide() {
        if (wideVirtualMethod() != 0L) {
            throw new AssertionError("Expected wide virtual method to return 0");
        }
    }
}
