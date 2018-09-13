package com.yperess.test

import android.animation.ObjectAnimator
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.util.Property
import android.view.ViewGroup
import android.view.animation.BounceInterpolator
import android.widget.EdgeEffect
import android.widget.ImageView
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.FloatPropertyCompat
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.activity_main.*
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val itemSize = resources.getDimensionPixelSize(R.dimen.list_item_size)
        val itemsCount = 6
        recycler_view.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup,
                    viewType: Int): RecyclerView.ViewHolder {
                val imageView = ImageView(this@MainActivity)
                imageView.setImageResource(android.R.drawable.sym_def_app_icon)
                imageView.layoutParams = ViewGroup.LayoutParams(itemSize, itemSize)
                return object : RecyclerView.ViewHolder(imageView) {}
            }

            override fun getItemCount(): Int = itemsCount

            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {}
        }
        bounce.setOnClickListener {
            ObjectAnimator.ofInt(recycler_view, ScrollXProperty(), 0).apply {
                interpolator = BounceInterpolator()
                duration = 500L
            }.start()
        }
        spring.setOnClickListener {
            SpringAnimation(recycler_view, ScrollXFloatPropertyCompat())
                    .setSpring(SpringForce()
                            .setFinalPosition(0f)
                            .setStiffness(SpringForce.STIFFNESS_LOW)
                            .setDampingRatio(SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY))
                    .start()
        }
    }

    class ScrollXFloatPropertyCompat : FloatPropertyCompat<RecyclerView>("scrollX") {
        override fun setValue(`object`: RecyclerView, value: Float) {
            `object`.scrollBy(value.roundToInt() - getValue(`object`).roundToInt(), 0)
        }

        override fun getValue(`object`: RecyclerView): Float =
                `object`.computeHorizontalScrollOffset().toFloat()
    }

    class ScrollXProperty : Property<RecyclerView, Int>(Int::class.java, "horozontalOffset") {
        override fun get(`object`: RecyclerView): Int =
                `object`.computeHorizontalScrollOffset()

        override fun set(`object`: RecyclerView, value: Int) {
            `object`.scrollBy(value - get(`object`), 0)
        }
    }
}
