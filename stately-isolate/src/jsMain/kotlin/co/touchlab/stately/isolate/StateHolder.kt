package co.touchlab.stately.isolate

actual class StateHolder<out T : Any> actual constructor(t: T) {
    actual val myState: T = t

    actual fun remove() {
    }
}

internal actual fun <R> stateRun(block: () -> R): R = block()