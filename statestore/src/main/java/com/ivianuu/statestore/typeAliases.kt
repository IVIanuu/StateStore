package com.ivianuu.statestore

typealias Consumer<T> = (T) -> Unit

typealias Reducer<T> = T.() -> T

typealias StateListener<T> = (T) -> Unit