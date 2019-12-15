package co.touchlab.stately.collections

import co.touchlab.stately.isFrozen
import co.touchlab.stately.isNative

internal fun <T> T.isNativeFrozen(): Boolean = !isNative || isFrozen