package rip.sunrise.injectapi

import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import rip.sunrise.injectapi.hooks.CapturedArgument
import rip.sunrise.injectapi.hooks.TargetField
import rip.sunrise.injectapi.hooks.TargetMethod
import rip.sunrise.injectapi.hooks.redirect.field.FieldRedirectHook
import rip.sunrise.injectapi.managers.HookManager
import rip.sunrise.injectapi.transformers.GlobalTransformer
import kotlin.test.assertEquals

private const val CLASS_NAME = "RedirectFieldTest"

class RedirectFieldTest {
    private val clazz = classLoader.loadClass(CLASS_NAME)
    private val thinStaticField = clazz.getDeclaredField("thinStaticValue")
    private val wideStaticField = clazz.getDeclaredField("wideStaticValue")
    private val thinVirtualField = clazz.getDeclaredField("thinVirtualValue")
    private val wideVirtualField = clazz.getDeclaredField("wideVirtualValue")

    @Test
    fun testGetVirtualThin() {
        testHookedMethodInvocation(CLASS_NAME, "testGetVirtualThin") { clazz, method ->
            arrayOf(
                FieldRedirectHook(
                    FieldRedirectHook.Type.GET,
                    clazz,
                    TargetMethod(method.name, Type.getMethodDescriptor(method)),
                    TargetField("thinVirtualValue", CLASS_NAME, "I"),
                ) { original: Int ->
                    assertEquals(1, original)
                    return@FieldRedirectHook 0
                }
            )
        }
    }

    @Test
    fun testGetVirtualWide() {
        testHookedMethodInvocation(CLASS_NAME, "testGetVirtualWide") { clazz, method ->
            arrayOf(
                FieldRedirectHook(
                    FieldRedirectHook.Type.GET,
                    clazz,
                    TargetMethod(method.name, Type.getMethodDescriptor(method)),
                    TargetField("wideVirtualValue", CLASS_NAME, "J"),
                ) { original: Long ->
                    assertEquals(1L, original)
                    return@FieldRedirectHook 0L
                }
            )
        }
    }

    @Test
    fun testGetStaticThin() {
        testHookedMethodInvocation(CLASS_NAME, "testGetStaticThin") { clazz, method ->
            arrayOf(
                FieldRedirectHook(
                    FieldRedirectHook.Type.GET,
                    clazz,
                    TargetMethod(method.name, Type.getMethodDescriptor(method)),
                    TargetField("thinStaticValue", CLASS_NAME, "I"),
                ) { original: Int ->
                    assertEquals(1, original)
                    return@FieldRedirectHook 0
                }
            )
        }
    }

    @Test
    fun testGetStaticWide() {
        testHookedMethodInvocation(CLASS_NAME, "testGetStaticWide") { clazz, method ->
            arrayOf(
                FieldRedirectHook(
                    FieldRedirectHook.Type.GET,
                    clazz,
                    TargetMethod(method.name, Type.getMethodDescriptor(method)),
                    TargetField("wideStaticValue", CLASS_NAME, "J"),
                ) { original: Long ->
                    assertEquals(1L, original)
                    return@FieldRedirectHook 0L
                }
            )
        }
    }

    @Test
    fun testSetVirtualThin() {
        var i: Any? = null
        testHookedMethodInvocation(CLASS_NAME, "testSetVirtualThin") { clazz, method ->
            arrayOf(
                FieldRedirectHook(
                    FieldRedirectHook.Type.SET,
                    clazz,
                    TargetMethod(method.name, Type.getMethodDescriptor(method)),
                    TargetField("thinVirtualValue", CLASS_NAME, "I"),
                    listOf(CapturedArgument(Opcodes.ALOAD, 0))
                ) { original: Int, instance: Any ->
                    i = instance
                    assertEquals(1, original)
                    return@FieldRedirectHook 0
                }
            )
        }

        assertEquals(0, thinVirtualField.get(i))
    }

    @Test
    fun testSetVirtualWide() {
        var i: Any? = null
        testHookedMethodInvocation(CLASS_NAME, "testSetVirtualWide") { clazz, method ->
            arrayOf(
                FieldRedirectHook(
                    FieldRedirectHook.Type.SET,
                    clazz,
                    TargetMethod(method.name, Type.getMethodDescriptor(method)),
                    TargetField("wideVirtualValue", CLASS_NAME, "J"),
                    listOf(CapturedArgument(Opcodes.ALOAD, 0))
                ) { original: Long, instance: Any ->
                    i = instance
                    assertEquals(1L, original)
                    return@FieldRedirectHook 0L
                }
            )
        }

        assertEquals(0L, wideVirtualField.get(i))
    }

    @Test
    fun testSetStaticThin() {
        testHookedMethodInvocation(CLASS_NAME, "testSetStaticThin") { clazz, method ->
            arrayOf(
                FieldRedirectHook(
                    FieldRedirectHook.Type.SET,
                    clazz,
                    TargetMethod(method.name, Type.getMethodDescriptor(method)),
                    TargetField("thinStaticValue", CLASS_NAME, "I"),
                ) { original: Int ->
                    assertEquals(1, original)
                    return@FieldRedirectHook 0
                }
            )
        }

        assertEquals(0, thinStaticField.get(null))
    }

    @Test
    fun testSetStaticWide() {
        testHookedMethodInvocation(CLASS_NAME, "testSetStaticWide") { clazz, method ->
            arrayOf(
                FieldRedirectHook(
                    FieldRedirectHook.Type.SET,
                    clazz,
                    TargetMethod(method.name, Type.getMethodDescriptor(method)),
                    TargetField("wideStaticValue", CLASS_NAME, "J"),
                ) { original: Long ->
                    assertEquals(1L, original)
                    return@FieldRedirectHook 0L
                }
            )
        }

        assertEquals(0L, wideStaticField.get(null))
    }

    @Test
    fun testCaptureThinArgumentStatic() {
        testHookedMethodInvocation(CLASS_NAME, "testCaptureThinArgumentStatic", 42) { clazz, method ->
            arrayOf(
                FieldRedirectHook(
                    FieldRedirectHook.Type.SET,
                    clazz,
                    TargetMethod(method.name, Type.getMethodDescriptor(method)),
                    TargetField("thinStaticValue", CLASS_NAME, "I"),
                    listOf(CapturedArgument(Opcodes.ILOAD, 0))
                ) { original: Int, a: Int ->
                    assertEquals(1, original)
                    assertEquals(42, a)
                    return@FieldRedirectHook a
                }
            )
        }

        assertEquals(42, thinStaticField.get(null))
    }

    @Test
    fun testCaptureWideArgumentStatic() {
        testHookedMethodInvocation(CLASS_NAME, "testCaptureWideArgumentStatic", 42L) { clazz, method ->
            arrayOf(
                FieldRedirectHook(
                    FieldRedirectHook.Type.SET,
                    clazz,
                    TargetMethod(method.name, Type.getMethodDescriptor(method)),
                    TargetField("wideStaticValue", CLASS_NAME, "J"),
                    listOf(CapturedArgument(Opcodes.LLOAD, 0))
                ) { original: Long, a: Long ->
                    assertEquals(1L, original)
                    assertEquals(42L, a)
                    return@FieldRedirectHook a
                }
            )
        }

        assertEquals(42L, wideStaticField.get(null))
    }


    @Test
    fun testCaptureThinArgumentVirtual() {
        testHookedMethodInvocation(CLASS_NAME, "testCaptureThinArgumentVirtual", 42) { clazz, method ->
            arrayOf(
                FieldRedirectHook(
                    FieldRedirectHook.Type.SET,
                    clazz,
                    TargetMethod(method.name, Type.getMethodDescriptor(method)),
                    TargetField("thinStaticValue", CLASS_NAME, "I"),
                    listOf(CapturedArgument(Opcodes.ILOAD, 1))
                ) { original: Int, a: Int ->
                    assertEquals(1, original)
                    assertEquals(42, a)
                    return@FieldRedirectHook a
                }
            )
        }

        assertEquals(42, thinStaticField.get(null))
    }

    @Test
    fun testCaptureWideArgumentVirtual() {
        testHookedMethodInvocation(CLASS_NAME, "testCaptureWideArgumentVirtual", 42L) { clazz, method ->
            arrayOf(
                FieldRedirectHook(
                    FieldRedirectHook.Type.SET,
                    clazz,
                    TargetMethod(method.name, Type.getMethodDescriptor(method)),
                    TargetField("wideStaticValue", CLASS_NAME, "J"),
                    listOf(CapturedArgument(Opcodes.LLOAD, 1))
                ) { original: Long, a: Long ->
                    assertEquals(1L, original)
                    assertEquals(42L, a)
                    return@FieldRedirectHook a
                }
            )
        }

        assertEquals(42L, wideStaticField.get(null))
    }

    @AfterEach
    fun clearHooks() {
        thinStaticField.set(null, 1)
        assertEquals(1, thinStaticField.get(null))

        wideStaticField.set(null, 1L)
        assertEquals(1L, wideStaticField.get(null))

        HookManager.getHooks().forEach {
            @OptIn(InjectApi.Internal::class)
            HookManager.removeHook(it)
        }
        assert(HookManager.getHooks().isEmpty())
    }

    companion object {
        @BeforeAll
        @JvmStatic
        fun registerTransformers() {
            instrumentation.addTransformer(GlobalTransformer, true)
        }

        @AfterAll
        @JvmStatic
        fun unregisterTransformers() {
            instrumentation.removeTransformer(GlobalTransformer)
        }
    }
}