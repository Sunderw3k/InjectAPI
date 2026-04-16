package rip.sunrise.injectapi.annotations

/**
 * Marks a member whose original JVM name must be preserved by the obfuscator
 * because it's accessed by name from ASM-emitted bytecode or reflection.
 *
 * Consumed at build time by `GenerateRefByNameRulesTask` in `buildSrc`,
 * which scans compiled classfiles for this annotation and emits ProGuard rules.
 *
 * Retention is [AnnotationRetention.BINARY] so that the annotation type is never loaded at runtime.
 */
@Retention(AnnotationRetention.BINARY)
@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.CONSTRUCTOR,
    AnnotationTarget.FIELD,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
)
annotation class ReferencedByName(val reason: String)
