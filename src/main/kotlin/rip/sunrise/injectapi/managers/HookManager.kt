package rip.sunrise.injectapi.managers

import com.google.common.collect.HashBiMap
import com.google.common.collect.ImmutableBiMap
import rip.sunrise.injectapi.InjectApi
import rip.sunrise.injectapi.hooks.Hook
import rip.sunrise.injectapi.managers.HookManager.addHook
import rip.sunrise.injectapi.managers.HookManager.hookMap

object HookManager {
    /**
     * Starts at 0 and is incremented by every call to [addHook].
     */
    private var hookId = 0
    private val hookMap = HashBiMap.create<Int, Hook>()

    /**
     * Returns an immutable [hookMap].
     */
    fun getHookMap(): ImmutableBiMap<Int, Hook> {
        return ImmutableBiMap.copyOf(hookMap)
    }

    /**
     * Adds a [hook] into [hookMap].
     */
    fun addHook(hook: Hook) {
        hookMap[hookId++] = hook
    }

    /**
     * For testing, so that multiple calls to [InjectApi.transform] don't reintroduce broken code.
     */
    @InjectApi.Internal
    fun removeHook(hook: Hook) {
        val hookId = getHookId(hook)
        hookMap.remove(hookId)
    }

    /**
     * Used in generated bytecode. Gets a hook from the [id].
     */
    @Suppress("unused")
    fun getHook(id: Int): Hook {
        return hookMap[id] ?: throw NoSuchElementException("Hook with id '$id' couldn't be found")
    }

    /**
     * Returns a hook id from a [hook]
     *
     * @throws NoSuchElementException when the hook is not added to the [hookMap].
     */
    fun getHookId(hook: Hook): Int {
        return hookMap.inverse()[hook] ?: throw NoSuchElementException("Hook '$hook' has no ID assigned")
    }

    /**
     * Returns a set containing all classes targeted by currently added hooks.
     */
    fun getTargetClasses(): Set<Class<*>> {
        return hookMap.values.map { it.clazz }.toSet()
    }
}