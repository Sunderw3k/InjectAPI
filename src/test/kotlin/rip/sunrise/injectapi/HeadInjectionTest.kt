package rip.sunrise.injectapi

import net.bytebuddy.agent.ByteBuddyAgent
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import rip.sunrise.injectapi.global.Context
import rip.sunrise.injectapi.hooks.CapturedArgument
import rip.sunrise.injectapi.hooks.Hook
import rip.sunrise.injectapi.hooks.TargetMethod
import rip.sunrise.injectapi.hooks.inject.InjectHook
import rip.sunrise.injectapi.hooks.inject.modes.HeadInjection
import rip.sunrise.injectapi.managers.HookManager
import rip.sunrise.injectapi.transformers.GlobalTransformer
import rip.sunrise.injectapi.utils.SanityTransformer
import java.io.File
import java.lang.reflect.Method
import java.net.URLClassLoader
import kotlin.test.assertEquals

class HeadInjectionTest {
    @Test
    fun testEarlyReturnVoid() {
        testHookedMethodInvocation("InjectTest", "testEarlyReturnVoid") { clazz, method ->
            arrayOf(
                InjectHook(
                    HeadInjection(),
                    clazz,
                    TargetMethod(method.name, Type.getMethodDescriptor(method)),
                    emptyList(),
                ) { ctx: Context ->
                    ctx.setReturnValue(0)
                }
            )
        }
    }

    @Test
    fun testEarlyReturnNonVoid() {
        testHookedMethodInvocation("InjectTest", "testEarlyReturnNonVoid") { clazz, method ->
            arrayOf(
                InjectHook(
                    HeadInjection(),
                    clazz,
                    TargetMethod(method.name, Type.getMethodDescriptor(method)),
                    emptyList(),
                ) { ctx: Context ->
                    ctx.setReturnValue(0)
                }
            )
        }
    }

    @Test
    fun testCaptureThinArgument() {
        testHookedMethodInvocation("InjectTest", "testCaptureThinArgument", 42) { clazz, method ->
            arrayOf(
                InjectHook(
                    HeadInjection(),
                    clazz,
                    TargetMethod(method.name, Type.getMethodDescriptor(method)),
                    listOf(CapturedArgument(Opcodes.ILOAD, 0)),
                ) { ctx: Context, a: Int ->
                    assertEquals(42, a)
                }
            )
        }
    }

    @Test
    fun testCaptureWideArgument() {
        testHookedMethodInvocation("InjectTest", "testCaptureWideArgument", 42L) { clazz, method ->
            arrayOf(
                InjectHook(
                    HeadInjection(),
                    clazz,
                    TargetMethod(method.name, Type.getMethodDescriptor(method)),
                    listOf(CapturedArgument(Opcodes.LLOAD, 0)),
                ) { ctx: Context, a: Long ->
                    assertEquals(42L, a)
                }
            )
        }
    }

    @Test
    fun testCaptureObjectArgument() {
        testHookedMethodInvocation("InjectTest", "testCaptureObjectArgument", System.out) { clazz, method ->
            arrayOf(
                InjectHook(
                    HeadInjection(),
                    clazz,
                    TargetMethod(method.name, Type.getMethodDescriptor(method)),
                    listOf(CapturedArgument(Opcodes.ALOAD, 0)),
                ) { ctx: Context, a: Any ->
                    assertEquals(System.out, a)
                }
            )
        }
    }

    @Test
    fun testCaptureThinArgumentAfterThin() {
        testHookedMethodInvocation("InjectTest", "testCaptureThinArgumentAfterThin", 42, 69) { clazz, method ->
            arrayOf(
                InjectHook(
                    HeadInjection(),
                    clazz,
                    TargetMethod(method.name, Type.getMethodDescriptor(method)),
                    listOf(CapturedArgument(Opcodes.ILOAD, 1)),
                ) { ctx: Context, a: Int ->
                    assertEquals(69, a)
                }
            )
        }
    }

    fun testHookedMethodInvocation(
        className: String,
        methodName: String,
        vararg args: Any,
        getHooks: (Class<*>, Method) -> Array<Hook>
    ) {
        val clazz = classLoader.loadClass(className)
        val method = clazz.declaredMethods.first { it.name == methodName }

        val hooks = getHooks(clazz, method)
        hooks.forEach {
            HookManager.addHook(it)
        }
        InjectApi.transform(instrumentation)

        // TODO: assertDoesNotThrow is cleaner, but this unwraps the message
        val result = runCatching {
            method.invoke(null, *args)
        }

        assert(result.isSuccess) {
            val cause = result.exceptionOrNull()!!.cause!!
            cause.message!!
        }
    }

    @AfterEach
    fun clearHooks() {
        HookManager.getHookMap().forEach { (_, hook) ->
            @OptIn(InjectApi.Internal::class)
            HookManager.removeHook(hook)
        }
    }

    companion object {
        private val instrumentation = ByteBuddyAgent.install()
        private val classLoader = URLClassLoader.newInstance(
            arrayOf(File("build/libs/test-targets.jar").toURI().toURL())
        )

        @BeforeAll
        @JvmStatic
        fun registerTransformer() {
            instrumentation.addTransformer(SanityTransformer, true)
            GlobalTransformer().register(instrumentation)
        }
    }
}