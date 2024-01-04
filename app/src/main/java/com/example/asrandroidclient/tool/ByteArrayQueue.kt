package com.example.asrandroidclient.tool

/**
 * Create by MJ on 2023/12/8.
 * Describe : byte数组 队列
 */

class ByteArrayQueue {
    private var elements = byteArrayOf()
    val size get() = elements.size

    fun append(items: ByteArray) {
        elements += items
    }

    fun pop(num: Int = 1): ByteArray? {
        return if (num < elements.size) {
            // 返回一个新的字节数组 范围 [0，num)
            val re = elements.copyOfRange(0, num)
            elements = elements.copyOfRange(num, elements.size)
            re
        } else null
    }

    fun clear() {
        elements = byteArrayOf()
    }

    fun popAll(): ByteArray {
        // 数组复制
        val re = elements.copyOf()
        // clear()
        return re
    }

    operator fun plusAssign(items: ByteArray) = append(items)
}