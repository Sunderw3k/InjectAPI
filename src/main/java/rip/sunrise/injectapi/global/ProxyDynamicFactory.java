package rip.sunrise.injectapi.global;

import java.lang.invoke.*;

public class ProxyDynamicFactory {
    public static CallSite bootstrap(MethodHandles.Lookup caller, String name, MethodType type, int hookId) {
        System.out.println("Got invokedynamic call from" + caller + " for hookId " + hookId);

        try {
            ClassLoader cl = ((ClassLoader) ClassLoader.getSystemClassLoader()
                    .loadClass("rip.sunrise.injectapi.global.DataTransport")
                    .getDeclaredField("classLoader")
                    .get(null));

            Class<?> managerClass = cl.loadClass("rip.sunrise.injectapi.managers.HookManager");
            Class<?> hookClass = cl.loadClass("rip.sunrise.injectapi.hooks.Hook");

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
