package com.tapshop.ui.nav

import androidx.compose.runtime.mutableStateListOf

/** Minimal back-stack navigator; deliberately dependency-free so it behaves identically on web and desktop. */
class Navigator<S : Any>(start: S) {
    val stack = mutableStateListOf(start)

    val current: S get() = stack.last()
    val canGoBack: Boolean get() = stack.size > 1

    fun push(screen: S) {
        if (stack.last() != screen) stack.add(screen)
    }

    fun replace(screen: S) {
        stack[stack.lastIndex] = screen
    }

    fun pop(): Boolean {
        if (stack.size <= 1) return false
        stack.removeAt(stack.lastIndex)
        return true
    }

    fun popTo(predicate: (S) -> Boolean) {
        while (stack.size > 1 && !predicate(stack.last())) stack.removeAt(stack.lastIndex)
    }

    fun reset(screen: S) {
        stack.clear()
        stack.add(screen)
    }
}
