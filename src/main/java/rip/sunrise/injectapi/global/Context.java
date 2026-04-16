package rip.sunrise.injectapi.global;

import org.jetbrains.annotations.NotNull;
import rip.sunrise.injectapi.annotations.ReferencedByName;

import java.util.HashMap;
import java.util.Map;

/**
 * Passed into hooks, then used for the return value.
 * <p>
 * Note: Written in java because it is loaded in ALL hooked ClassLoaders.
 */
public class Context {
    /**
     * Specifies the early return value returned right after the hook callback.
     */
    @ReferencedByName(reason = "Fetched from hooks")
    public Object returnValue = null;

    /**
     * Used in hooks.
     * @see Context#hasReturnValue()
     */
    private boolean hasReturnValue = false;

    /**
     * User-facing method to force an early return with the specified value.
     */
    public void setReturnValue(Object value) {
        this.returnValue = value;
        this.hasReturnValue = true;
    }

    /**
     * Called from hooks to check whether there is a return value set.
     */
    @ReferencedByName(reason = "Called from hooks")
    public boolean hasReturnValue() {
        return hasReturnValue;
    }

    /**
     * Because this class is used as a bridge between hooks and hooked code, it is necessary to serialize the object into something both classloaders can understand.
     * To not lose ALL type information, a Map isn't the worst idea.
     */
    @ReferencedByName(reason = "Called from hooks")
    public @NotNull Map<String, Object> serialize() {
        HashMap<String, Object> map = new HashMap<>();

        map.put("returnValue", returnValue);
        map.put("hasReturnValue", hasReturnValue);

        return map;
    }

    /**
     * Only static to make it easier to invoke from MethodHandles and bytecode.
     */
    @ReferencedByName(reason = "Called from hooks and when transforming an InjectHook MethodHandle")
    public static @NotNull Context deserialize(@NotNull Map<String, Object> data) {
        Context ctx = new Context();

        ctx.returnValue = data.get("returnValue");
        ctx.hasReturnValue = (boolean) data.get("hasReturnValue");

        return ctx;
    }
}