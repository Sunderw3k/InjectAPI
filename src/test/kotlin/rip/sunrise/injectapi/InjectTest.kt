package rip.sunrise.injectapi

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import rip.sunrise.injectapi.global.Context
import rip.sunrise.injectapi.hooks.CapturedArgument
import rip.sunrise.injectapi.hooks.TargetMethod
import rip.sunrise.injectapi.hooks.inject.InjectHook
import rip.sunrise.injectapi.hooks.inject.modes.HeadInjection
import rip.sunrise.injectapi.managers.HookManager
import rip.sunrise.injectapi.transformers.GlobalTransformer
import rip.sunrise.injectapi.utils.SanityTransformer
import kotlin.test.assertEquals

private const val CLASS_NAME = "InjectTest"

class InjectTest {
    @Test
    fun testEarlyReturnVoid() {
        testHookedMethodInvocation(CLASS_NAME, "testEarlyReturnVoid") { clazz, method ->
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
        testHookedMethodInvocation(CLASS_NAME, "testEarlyReturnNonVoid") { clazz, method ->
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
    fun testReturnVoid() {
        var ran = false
        testHookedMethodInvocation(CLASS_NAME, "testReturnVoid") { clazz, method ->
            arrayOf(
                InjectHook(
                    HeadInjection(),
                    clazz,
                    TargetMethod(method.name, Type.getMethodDescriptor(method)),
                    emptyList(),
                ) { _: Context ->
                    ran = true
                }
            )
        }

        assert(ran)
    }

    @Test
    fun testReturnNonVoid() {
        var ran = false
        testHookedMethodInvocation(CLASS_NAME, "testReturnNonVoid") { clazz, method ->
            arrayOf(
                InjectHook(
                    HeadInjection(),
                    clazz,
                    TargetMethod(method.name, Type.getMethodDescriptor(method)),
                    emptyList(),
                ) { _: Context ->
                    ran = true
                }
            )
        }

        assert(ran)
    }

    @Test
    fun testCaptureThinArgument() {
        testHookedMethodInvocation(CLASS_NAME, "testCaptureThinArgument", 42) { clazz, method ->
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
        testHookedMethodInvocation(CLASS_NAME, "testCaptureWideArgument", 42L) { clazz, method ->
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
        testHookedMethodInvocation(CLASS_NAME, "testCaptureObjectArgument", System.out) { clazz, method ->
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
        testHookedMethodInvocation(CLASS_NAME, "testCaptureThinArgumentAfterThin", 42, 69) { clazz, method ->
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

    @Test
    fun testCaptureThinArgumentAfterWide() {
        testHookedMethodInvocation(CLASS_NAME, "testCaptureThinArgumentAfterWide", 42L, 69) { clazz, method ->
            arrayOf(
                InjectHook(
                    HeadInjection(),
                    clazz,
                    TargetMethod(method.name, Type.getMethodDescriptor(method)),
                    listOf(CapturedArgument(Opcodes.ILOAD, 2)),
                ) { ctx: Context, a: Int ->
                    assertEquals(69, a)
                }
            )
        }
    }

    @Test
    fun testCaptureWideArgumentAfterThin() {
        testHookedMethodInvocation(CLASS_NAME, "testCaptureWideArgumentAfterThin", 42, 69L) { clazz, method ->
            arrayOf(
                InjectHook(
                    HeadInjection(),
                    clazz,
                    TargetMethod(method.name, Type.getMethodDescriptor(method)),
                    listOf(CapturedArgument(Opcodes.LLOAD, 1)),
                ) { ctx: Context, a: Long ->
                    assertEquals(69L, a)
                }
            )
        }
    }

    @Test
    fun testCaptureWideArgumentAfterWide() {
        testHookedMethodInvocation(CLASS_NAME, "testCaptureWideArgumentAfterWide", 42L, 69L) { clazz, method ->
            arrayOf(
                InjectHook(
                    HeadInjection(),
                    clazz,
                    TargetMethod(method.name, Type.getMethodDescriptor(method)),
                    listOf(CapturedArgument(Opcodes.LLOAD, 2)),
                ) { ctx: Context, a: Long ->
                    assertEquals(69L, a)
                }
            )
        }
    }

    @Test
    fun testCaptureInstance() {
        testHookedMethodInvocation(CLASS_NAME, "testCaptureInstance") { clazz, method ->
            arrayOf(
                InjectHook(
                    HeadInjection(),
                    clazz,
                    TargetMethod(method.name, Type.getMethodDescriptor(method)),
                    listOf(CapturedArgument(Opcodes.ALOAD, 0)),
                ) { ctx: Context, instance: Any ->
                    assertInstanceOf(clazz, instance)
                }
            )
        }
    }

    @AfterEach
    fun clearHooks() {
        HookManager.getHookMap().forEach { (_, hook) ->
            @OptIn(InjectApi.Internal::class)
            HookManager.removeHook(hook)
        }
        assert(HookManager.getHookMap().isEmpty())
    }

    companion object {
        @BeforeAll
        @JvmStatic
        fun registerTransformer() {
            instrumentation.addTransformer(SanityTransformer, true)
            GlobalTransformer().register(instrumentation)
        }
    }
}