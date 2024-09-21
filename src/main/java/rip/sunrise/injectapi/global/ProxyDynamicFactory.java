package rip.sunrise.injectapi.global;

import java.lang.invoke.*;

public class ProxyDynamicFactory {
    /**
     * The InjectAPI ClassLoader.
     * Instantly set after class definition.
     */
    public static ClassLoader classLoader;

    /**
     * Called from hooks. This is the bridge between any ClassLoader and the InjectAPI ClassLoader.
     * Because this is only called once. It's okay to use reflection here.
     */
    @SuppressWarnings("ReplaceOnLiteralHasNoEffect")
    public static CallSite bootstrap(MethodHandles.Lookup caller, String name, MethodType type, int hookId) {
        System.out.println("Got invokedynamic call from" + caller + " for hookId " + hookId);

        try {
            Class<?> managerClass = classLoader.loadClass("@HOOK_MANAGER@".replace("/", "."));
            Class<?> hookClass = classLoader.loadClass("@HOOK@".replace("/", "."));

            Object instance = managerClass.getDeclaredField("INSTANCE").get(null);

            Object hook = managerClass.getDeclaredMethod("getHook", int.class).invoke(instance, hookId);

            MethodHandle handle = (MethodHandle) hookClass.getDeclaredMethod("getHandle").invoke(hook);

            // TODO: Kinda forced, it won't fail. Only happens because types are lost when calling unreflect.
            return new ConstantCallSite(handle.asType(type));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
