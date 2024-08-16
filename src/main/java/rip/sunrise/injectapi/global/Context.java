package rip.sunrise.injectapi.global;

import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Optional;

/**
 * Passed into hooks, then used for the return value.
 * <p>
 * Note: Written in java because it is loaded in ALL hooked ClassLoaders.
 */
public class Context {
    /**
     * Specifies the early return value returned right after the hook callback.
     */
    // TODO: Change to null because its java? A null jump should be cheaper than this
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public Optional<Object> returnValue = Optional.empty();

    /**
     * Because this class is used as a bridge between hooks and hooked code, it is necessary to serialize the object into something both classloaders can understand.
     * To not lose ALL type information, a Map isn't the worst idea.
     */
    @SuppressWarnings("unused")
    public @NotNull Map<String, Object> serialize() {
        return Map.of("returnValue", returnValue);
    }

    /**
     * Only static to make it easier to invoke from MethodHandles and bytecode.
     */
    @SuppressWarnings("unused")
    public static @NotNull Context deserialize(@NotNull Map<String, Object> data) {
        Context ctx = new Context();
        ctx.returnValue = Optional.of(data.get("returnValue"));

        return ctx;
    }
}
