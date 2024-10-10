package rip.sunrise.injectapi.utils

import sun.misc.Unsafe
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.security.ProtectionDomain

private fun getUnsafeField(): Field {
    return Unsafe::class.java.getDeclaredField("theUnsafe")
}

fun getUnsafe(): Unsafe {
    return getUnsafeField().also { it.isAccessible = true }.get(null) as Unsafe
}

private fun getAccessibleFlagOffset(): Long {
    val accessible = getUnsafeField().also { it.isAccessible = true }
    val inaccessible = getUnsafeField().also { it.isAccessible = false }

    var offset = 0L

    val unsafe = getUnsafe()
    while (unsafe.getByte(accessible, offset) == unsafe.getByte(inaccessible, offset)) {
        offset++
    }

    return offset
}

fun Field.setAccessibleUnsafe(state: Boolean) {
    getUnsafe().putBoolean(this, getAccessibleFlagOffset(), state)
}

fun Method.setAccessibleUnsafe(state: Boolean) {
    getUnsafe().putBoolean(this, getAccessibleFlagOffset(), state)
}

fun defineClass(bytes: ByteArray, classLoader: ClassLoader?, protectionDomain: ProtectionDomain?) {
    val internalUnsafe =
        Unsafe::class.java.getDeclaredField("theInternalUnsafe").also { it.setAccessibleUnsafe(true) }.get(null) as Any

    Class.forName("jdk.internal.misc.Unsafe").getDeclaredMethod(
        "defineClass",
        String::class.java,
        ByteArray::class.java,
        Int::class.java,
        Int::class.java,
        ClassLoader::class.java,
        ProtectionDomain::class.java
    ).also { it.setAccessibleUnsafe(true) }.invoke(internalUnsafe, null, bytes, 0, bytes.size, classLoader, protectionDomain)
}