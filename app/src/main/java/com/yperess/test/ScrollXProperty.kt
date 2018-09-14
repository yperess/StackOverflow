package com.yperess.test

import android.util.Property
import androidx.recyclerview.widget.RecyclerView

/**
 *
 */
class ScrollXProperty(
        private val enableOptimizations: Boolean
) : Property<RecyclerView, Int>(Int::class.java, "horizontalOffset") {

    private var lastKnownValue: Int? = null

    override fun get(`object`: RecyclerView): Int =
            `object`.computeHorizontalScrollOffset().also {
                if (enableOptimizations) {
                    lastKnownValue = it
                }
            }

    override fun set(`object`: RecyclerView, value: Int) {
        val currentValue = lastKnownValue?.takeIf { enableOptimizations } ?: get(`object`)
        if (enableOptimizations) {
            lastKnownValue = value
        }
        `object`.scrollBy(value - currentValue, 0)
    }
}