package com.ivianuu.statestore

import java.util.concurrent.Executor
import java.util.concurrent.Executors

internal val defaultExecutor: Executor by lazy { Executors.newSingleThreadExecutor() }