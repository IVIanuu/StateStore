package com.ivianuu.statestore.util

import com.ivianuu.statestore.CloseListener

class TestCloseListener : CloseListener {
    var closeCalls = 0
    override fun invoke() {
        closeCalls++
    }
}