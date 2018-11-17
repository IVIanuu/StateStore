/*
 * Copyright 2018 Manuel Wrage
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ivianuu.statestore

import java.util.concurrent.Executor

/**
 * A store for state of [T]'s
 */
interface StateStore<T> {

    fun withState(consumer: (T) -> Unit)

    fun setState(reducer: T.() -> T)

    fun peekState(): T

    fun addListener(listener: (T) -> Unit)

    fun removeListener(listener: (T) -> Unit)

    fun close()

}

/**
 * Returns a new [StateStore]
 */
fun <T> StateStore(
    initialState: T,
    executor: Executor = StateStorePlugins.defaultExecutor ?: SingleThreadExecutor(),
    callbackExecutor: Executor = StateStorePlugins.defaultCallbackExecutor ?: executor
): StateStore<T> = RealStateStore(initialState, executor, callbackExecutor)