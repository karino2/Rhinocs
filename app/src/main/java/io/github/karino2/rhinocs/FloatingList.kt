package io.github.karino2.rhinocs

class FloatingList {
    var items: List<String> = emptyList()
        set(value) {
            field = value
            selectedIndex = if (value.isEmpty()) -1 else value.size - 1 // 一番下を選択
        }

    val numItems: Int
        get() = items.size
    var selectedIndex: Int = -1

    fun isEmpty() = items.isEmpty()

    fun clear() {
        items = emptyList()
    }

    fun moveUp() {
        if (isEmpty()) return
        selectedIndex = (selectedIndex - 1).coerceAtLeast(0)
    }

    fun moveDown() {
        if (isEmpty()) return
        selectedIndex = (selectedIndex + 1).coerceAtMost(items.size - 1)
    }
}
