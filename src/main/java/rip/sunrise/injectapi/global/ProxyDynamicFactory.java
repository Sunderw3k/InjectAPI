package rip.sunrise.injectapi.global;

import java.lang.invoke.*;

public class ProxyDynamicFactory {
    /**
     * The InjectAPI ClassLoader.
     * Instantly set after class definition.
     */
    public static ClassLoader classLoader;

    /**
     * Used from within the internal hook code to make sure we are not going into recursion.
     * Instantly set after class definition, reset on transform.
     */
    @SuppressWarnings("unused")
    public static ThreadLocal<boolean[]> runningHooks = ThreadLocal.withInitial(() -> new boolean[0]);

    /**
     * Called from hooks. This is the bridge between any ClassLoader and the InjectAPI ClassLoader.
     * Because this is only called once. It's okay to use reflection here.
     */
    public static CallSite bootstrap(MethodHandles.Lookup caller, String name, MethodType type, int hookId) {
        return new ConstantCallSite(getHandle(hookId).asType(type));
    }

    @SuppressWarnings("ReplaceOnLiteralHasNoEffect")
    public static MethodHandle getHandle(int hookId) {
        try {
            Class<?> managerClass = classLoader.loadClass("@HOOK_MANAGER@".replace("/", "."));
            Class<?> hookClass = classLoader.loadClass("@HOOK@".replace("/", "."));

            Object instance = managerClass.getDeclaredField("INSTANCE").get(null);

            Object hook = managerClass.getDeclaredMethod("getCachedHook", int.class).invoke(instance, hookId);

            return (MethodHandle) hookClass.getDeclaredMethod("getHandle").invoke(hook);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
