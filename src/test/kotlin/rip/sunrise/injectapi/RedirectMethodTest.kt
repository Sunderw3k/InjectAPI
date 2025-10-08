package rip.sunrise.injectapi

import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import rip.sunrise.injectapi.hooks.CapturedArgument
import rip.sunrise.injectapi.hooks.TargetMethod
import rip.sunrise.injectapi.hooks.redirect.method.MethodRedirectHook
import rip.sunrise.injectapi.managers.HookManager
import rip.sunrise.injectapi.transformers.GlobalTransformer
import kotlin.test.assertEquals

private const val CLASS_NAME = "RedirectMethodTest"

class RedirectMethodTest {
    @Test
    fun testRedirectStaticThin() {
        testHookedMethodInvocation(CLASS_NAME, "testRedirectStaticThin") { clazz, method ->
            arrayOf(
                MethodRedirectHook(
                    clazz,
                    TargetMethod(method.name, Type.getMethodDescriptor(method)),
                    TargetMethod("thinStaticMethod", "()I"),
                ) {
                    val original = classLoader.loadClass(CLASS_NAME).getDeclaredMethod("thinStaticMethod").invoke(null) as Int
                    assertEquals(1, original)

                    return@MethodRedirectHook 0
                }
            )
        }
    }

    @Test
    fun testRedirectStaticWide() {
        testHookedMethodInvocation(CLASS_NAME, "testRedirectStaticWide") { clazz, method ->
            arrayOf(
                MethodRedirectHook(
                    clazz,
                    TargetMethod(method.name, Type.getMethodDescriptor(method)),
                    TargetMethod("wideStaticMethod", "()J"),
                ) {
                    val original = classLoader.loadClass(CLASS_NAME).getDeclaredMethod("wideStaticMethod").invoke(null) as Long
                    assertEquals(1L, original)

                    return@MethodRedirectHook 0L
                }
            )
        }
    }

    @Test
    fun testRedirectVirtualThin() {
        testHookedMethodInvocation(CLASS_NAME, "testRedirectVirtualThin") { clazz, method ->
            arrayOf(
                MethodRedirectHook(
                    clazz,
                    TargetMethod(method.name, Type.getMethodDescriptor(method)),
                    TargetMethod("thinVirtualMethod", "()I"),
                    listOf(CapturedArgument(Opcodes.ALOAD, 0))
                ) { instance: Any ->
                    val original = instance::class.java.getDeclaredMethod("thinVirtualMethod").invoke(instance) as Int
                    assertEquals(1, original)

                    return@MethodRedirectHook 0
                }
            )
        }
    }

    @Test
    fun testRedirectVirtualWide() {
        testHookedMethodInvocation(CLASS_NAME, "testRedirectVirtualWide") { clazz, method ->
            arrayOf(
                MethodRedirectHook(
                    clazz,
                    TargetMethod(method.name, Type.getMethodDescriptor(method)),
                    TargetMethod("wideVirtualMethod", "()J"),
                    listOf(CapturedArgument(Opcodes.ALOAD, 0))
                ) { instance: Any ->
                    val original = instance::class.java.getDeclaredMethod("wideVirtualMethod").invoke(instance) as Long
                    assertEquals(1L, original)

                    return@MethodRedirectHook 0L
                }
            )
        }
    }

    @AfterEach
    fun clearHooks() {
        HookManager.getHooks().toList().forEach {
            @OptIn(InjectApi.Internal::class)
            HookManager.removeHook(it)
        }
        assert(HookManager.getHooks().isEmpty())
    }

    companion object {
        @BeforeAll
        @JvmStatic
        fun registerTransformers() {
            backend.addTransformer(GlobalTransformer)
        }

        @AfterAll
        @JvmStatic
        fun unregisterTransformers() {
            backend.removeTransformer(GlobalTransformer)
        }
    }
}