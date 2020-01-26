package co.touchlab.stately.isolate

import java.util.concurrent.Callable
import java.util.concurrent.Executors

actual class StateHolder<out T : Any> actual constructor(t: T) {
    actual val myState: T = t

    actual fun remove() {
    }
}

internal val stateExecutor = Executors.newSingleThreadExecutor()

internal actual fun <R> stateRun(block: () -> R): R {
    val result = stateExecutor.submit(Callable<RunResult> {
        try {
            Ok(block())
        } catch (e: Throwable) {
            Thrown(e)
        }
    }).get()

    return when(result){
        is Ok<*> -> result.result as R
        is Thrown -> throw result.throwable
    }
}