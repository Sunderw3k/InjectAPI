package rip.sunrise.injectapi.utils.extensions

fun Module.addOpensAll(other: Module) {
    packages.forEach {
        addOpens(it, other)
    }
}