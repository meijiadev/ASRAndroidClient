package com.example.asrandroidclient.tool

/**
 * Create by MJ on 2024/1/8.
 * Describe :
 */

class IntArrayQueue {
    private var elements = intArrayOf()
    val size get() = elements.size
    fun getAll(): IntArray {
        return elements
    }
    fun append(items: Int) {
        elements += items
    }

    fun pop(num: Int = 1) {
        if (num < elements.size) {
            // 返回一个新的字节数组 范围 [0，num)
            elements = elements.copyOfRange(num, elements.size)

        }
    }

    fun clear() {
        elements = intArrayOf()
    }


    operator fun plusAssign(items: Int) = append(items)
}