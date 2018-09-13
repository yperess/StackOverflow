package com.yperess.test

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
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
            recycler_view.postDelayed({
                SpringAnimation(recycler_view, ScrollXProperty())
                        .setSpring(SpringForce()
                                .setFinalPosition(0f)
                                .setStiffness(SpringForce.STIFFNESS_LOW)
                                .setDampingRatio(SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY))
                        .addUpdateListener { _, value, velocity ->
                            Log.d("MainActivity", "value=$value, velocity=$velocity")
                        }
                        .start()
            }, 500L)

        }
    }

    class ScrollXProperty : FloatPropertyCompat<RecyclerView>("scrollX") {
        override fun setValue(`object`: RecyclerView, value: Float) {
            `object`.scrollBy(value.roundToInt() - getValue(`object`).roundToInt(), 0)
        }

        override fun getValue(`object`: RecyclerView): Float =
                `object`.computeHorizontalScrollOffset().toFloat()
    }
}
