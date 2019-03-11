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

    /**
     * Runs the [consumer] with the current state
     * This might be asynchronous
     */
    fun withState(consumer: Consumer<T>)

    /**
     * Sets the the new with the [reducer]
     * This might be asynchronous
     */
    fun setState(reducer: Reducer<T>)

    /**
     * Returns the current state
     */
    fun peekState(): T

    /**
     * Notifies the [listener] on state changes
     */
    fun addStateListener(listener: StateListener<T>)

    /**
     * Removes the previously added [listener]
     */
    fun removeStateListener(listener: StateListener<T>)

    /**
     * Notifies the [listener] when this store gets closed
     */
    fun addCloseListener(listener: CloseListener)

    /**
     * Removes the previously added [listener]
     */
    fun removeCloseListener(listener: CloseListener)

    /**
     * Closes this store
     */
    fun close()

}

/**
 * Returns a new [StateStore]
 */
fun <T> StateStore(
    initialState: T,
    executor: Executor = StateStorePlugins.defaultExecutor ?: defaultExecutor,
    callbackExecutor: Executor = StateStorePlugins.defaultCallbackExecutor ?: executor
): StateStore<T> = RealStateStore(initialState, executor, callbackExecutor)