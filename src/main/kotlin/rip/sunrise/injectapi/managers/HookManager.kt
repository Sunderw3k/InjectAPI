package rip.sunrise.injectapi.managers

import com.google.common.collect.ImmutableList
import rip.sunrise.injectapi.InjectApi
import rip.sunrise.injectapi.hooks.Hook
import rip.sunrise.injectapi.managers.HookManager.cachedHooks
import rip.sunrise.injectapi.managers.HookManager.hooks

object HookManager {
    /**
     * Stores all hooks, copied into [cachedHooks] during [InjectApi.transform].
     */
    private val hooks = mutableListOf<Hook>()

    /**
     * Stores all currently loaded hooks.
     */
    private val cachedHooks = mutableListOf<Hook>()

    /**
     * Saves hooks used in currently loaded bytecode into [cachedHooks].
     *
     * @return The number of hooks in the current transformation
     */
    @InjectApi.Internal
    fun onTransform(): Int {
        cachedHooks.clear()
        cachedHooks.addAll(hooks)

        return cachedHooks.size
    }

    /**
     * Returns an immutable list of [hooks].
     */
    fun getHooks(): ImmutableList<Hook> {
        return ImmutableList.copyOf(hooks)
    }

    /**
     * Adds a [hook] into [hooks].
     */
    fun addHook(hook: Hook) {
        hooks.add(hook)
    }

    /**
     * For testing, so that multiple calls to [InjectApi.transform] don't reintroduce broken code.
     */
    @InjectApi.Internal
    fun removeHook(hook: Hook) {
        val hookId = getCachedHookId(hook)
        hooks.removeAt(hookId)
    }

    /**
     * Used in generated bytecode. Gets a cached hook from the [id].
     */
    @InjectApi.Internal
    @Suppress("unused")
    fun getCachedHook(id: Int): Hook {
        return cachedHooks[id]
    }

    /**
     * Returns a hook id from a cached [hook]
     *
     * @throws NoSuchElementException when the hook is not added to [cachedHooks].
     */
    @InjectApi.Internal
    fun getCachedHookId(hook: Hook): Int {
        val index = cachedHooks.indexOf(hook)
        if (index == -1) {
            throw NoSuchElementException("Hook '$hook' has no ID assigned!")
        }

        return index
    }

    /**
     * Returns a set containing all classes targeted by currently added hooks.
     */
    fun getTargetClasses(): Set<Class<*>> {
        return hooks.map { it.clazz }.toSet()
    }
}