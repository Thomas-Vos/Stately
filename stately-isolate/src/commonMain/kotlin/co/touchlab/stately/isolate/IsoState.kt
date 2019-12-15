package co.touchlab.stately.isolate

import co.touchlab.stately.ensureNeverFrozen
import co.touchlab.stately.isFrozen
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.native.concurrent.SharedImmutable
import kotlin.native.concurrent.ThreadLocal

@ThreadLocal
internal val stateMap = mutableMapOf<Int, Any>()

@ThreadLocal
internal var stateId = 0

@SharedImmutable
expect val stateDispatcher:CoroutineDispatcher

expect fun <T> runBlocking(context: CoroutineContext = EmptyCoroutineContext, block: suspend CoroutineScope.() -> T): T

open class IsolateState<T : Any>(producer: () -> T) {
    private val stateId: Int = createStateBlocking(producer)

    internal suspend fun <R> access(block: (T) -> R): R = withContext(stateDispatcher) {
        block(stateMap.get(stateId) as T)
    }
}

fun <T : Any> createStateBlocking(producer: () -> T): Int = runBlocking { createState(producer) }

suspend fun <T : Any> createState(producer: () -> T): Int = withContext(stateDispatcher) {
    val t = producer()
    if (t.isFrozen)
        throw IllegalStateException("Mutable state shouldn't be frozen")
    t.ensureNeverFrozen()
    val newId = stateId++
    stateMap.put(newId, t)
    newId
}

suspend fun removeState(id: Int) = withContext(stateDispatcher) {
    stateMap.remove(id)
}