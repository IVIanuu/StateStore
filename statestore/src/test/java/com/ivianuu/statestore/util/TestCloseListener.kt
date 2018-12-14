package com.ivianuu.statestore.util

class TestCloseListener : () -> Unit {
    var closeCalls = 0
    override fun invoke() {
        closeCalls++
    }
}