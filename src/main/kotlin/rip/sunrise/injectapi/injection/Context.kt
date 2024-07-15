package rip.sunrise.injectapi.injection

import java.util.*

/**
 * Passed into hooks, then used for the return value.
 *
 * NOTE: Loaded on ALL hooked ClassLoaders.
 */
class Context {
    /**
     * Specifies the early return value returned right after the hook callback.
     */
    var returnValue = Optional.empty<Any>()

    /**
     * Because this class is used as a bridge between hooks and hooked code, it is necessary to serialize the object into something both classloaders can understand.
     * To not lose ALL type information, a Map<String, Any> isn't the worst idea.
     */
    @Suppress("unused")
    fun serialize(): Map<String, Any> {
        return mapOf("returnValue" to returnValue)
    }

    companion object {
        /**
         * Only static to make it easier to invoke from MethodHandles and bytecode.
         *
         * Note: The Companion class has to be loaded in all classloaders too, because the static version in Context invokes this virtual one.
         */
        @JvmStatic
        @Suppress("unused")
        fun deserialize(data: Map<String, Any>): Context {
            return Context().also {
                it.returnValue = data["returnValue"] as Optional<Any>
            }
        }
    }
}