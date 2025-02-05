package rip.sunrise.injectapi.global;

import org.jetbrains.annotations.NotNull;

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
    public Object returnValue = null;

    /**
     * Used in hooks to check if there is a return value.
     */
    private boolean hasReturnValue = false;

    public void setReturnValue(Object value) {
        this.returnValue = value;
        this.hasReturnValue = true;
    }

    public boolean hasReturnValue() {
        return hasReturnValue;
    }

    /**
     * Because this class is used as a bridge between hooks and hooked code, it is necessary to serialize the object into something both classloaders can understand.
     * To not lose ALL type information, a Map isn't the worst idea.
     */
    @SuppressWarnings("unused")
    public @NotNull Map<String, Object> serialize() {
        HashMap<String, Object> map = new HashMap<>();

        map.put("returnValue", returnValue);
        map.put("hasReturnValue", hasReturnValue);

        return map;
    }

    /**
     * Only static to make it easier to invoke from MethodHandles and bytecode.
     */
    @SuppressWarnings({"unused"})
    public static @NotNull Context deserialize(@NotNull Map<String, Object> data) {
        Context ctx = new Context();

        ctx.returnValue = data.get("returnValue");
        ctx.hasReturnValue = (boolean) data.get("hasReturnValue");

        return ctx;
    }
}