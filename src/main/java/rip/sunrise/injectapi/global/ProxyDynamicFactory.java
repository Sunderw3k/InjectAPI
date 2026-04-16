package rip.sunrise.injectapi.global;

import rip.sunrise.injectapi.annotations.ReferencedByName;

import java.lang.invoke.*;

public class ProxyDynamicFactory {
    /**
     * The InjectAPI ClassLoader.
     * Instantly set after class definition.
     */
    @ReferencedByName(reason = "Set by reflection during transformation")
    public static ClassLoader classLoader;

    /**
     * Used from within the internal hook code to make sure we are not going into recursion.
     * Instantly set after class definition, reset on transform.
     */
    @ReferencedByName(reason = "Set by reflection during transformation")
    public static ThreadLocal<boolean[]> runningHooks = ThreadLocal.withInitial(() -> new boolean[0]);

    /**
     * Called from hooks. This is the bridge between any ClassLoader and the InjectAPI ClassLoader.
     * Because this is only called once. It's okay to use reflection here.
     */
    @ReferencedByName(reason = "Called from hooks")
    public static CallSite bootstrap(MethodHandles.Lookup caller, String name, MethodType type, int hookId) {
        return new ConstantCallSite(getHandle(hookId).asType(type));
    }

    @ReferencedByName(reason = "Called from hooks when invokedynamic isn't supported")
    @SuppressWarnings("ReplaceOnLiteralHasNoEffect")
    public static MethodHandle getHandle(int hookId) {
        try {
            Class<?> managerClass = classLoader.loadClass("@HOOK_MANAGER@".replace("/", "."));
            Class<?> hookClass = classLoader.loadClass("@HOOK@".replace("/", "."));

            Object hook = managerClass.getDeclaredMethod("getCachedHook", int.class).invoke(null, hookId);

            return (MethodHandle) hookClass.getDeclaredMethod("getHandle").invoke(hook);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
