package co.touchlab.stately.collections

import co.touchlab.stately.freeze
import co.touchlab.stately.isFrozen
import co.touchlab.stately.isNative
import kotlinx.atomicfu.AtomicRef
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.locks.reentrantLock
import kotlinx.atomicfu.locks.withLock

class ObjectPool<T>(
  private val maxSize: Int,
  private val createBlock:()->T,
  private val cleanupBlock:((t:T)->Unit)? = null) {
  init {
    if(maxSize < 0)
      throw IllegalArgumentException("maxSize cannot be negative")
  }

  internal val pool = Array<AtomicRef<T?>>(maxSize) {
    atomic(null)
  }

  private val poolIndex = atomic(0)
  private val lock = reentrantLock()

  fun push(t: T): Boolean = lock.withLock {
    if (isNative && !t.isFrozen)
      throw IllegalStateException("Object pool entries must be frozen")

    if(maxSize == 0){
      cleanupBlock?.invoke(t)
      false
    } else {
      val index = poolIndex.value

      return if (index >= maxSize) {
        cleanupBlock?.invoke(t)
        false
      } else {
        pool[index].value = t
        poolIndex.incrementAndGet()
        true
      }
    }
  }

  fun pop(): T = lock.withLock {
    if(maxSize == 0){
      createBlock().freeze()
    } else {
      val index = poolIndex.value

      val fromPool = if (index <= 0)
        null
      else {
        val ref = pool[poolIndex.decrementAndGet()]
        val t = ref.value
        ref.value = null
        t
      }

      fromPool ?: createBlock().freeze()
    }
  }

  fun clear() = lock.withLock {
    pool.forEach {
      val t = it.value
      if (t != null)
      {
        cleanupBlock?.invoke(t)
        it.value = null
      }
    }

    poolIndex.value = 0
  }
}