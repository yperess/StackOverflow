package com.yperess.test

import android.animation.ObjectAnimator
import android.animation.TimeInterpolator
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AccelerateInterpolator
import android.view.animation.AnticipateInterpolator
import android.view.animation.AnticipateOvershootInterpolator
import android.view.animation.BaseInterpolator
import android.view.animation.BounceInterpolator
import android.view.animation.CycleInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), AdapterView.OnItemSelectedListener {

    private lateinit var interpolator: BaseInterpolator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val itemSize = resources.getDimensionPixelSize(R.dimen.list_item_size)
        val itemsCount = 100
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
        interpolation_selection.apply {
            adapter = ArrayAdapter.createFromResource(this@MainActivity,
                    R.array.interpolators, android.R.layout.simple_spinner_item).also { adapter ->
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_item)
            }
            onItemSelectedListener = this@MainActivity
            setSelection(0)
        }
        bounce.setOnClickListener { view ->
            val interpolatorWrapper = TimeInterpolator { interpolator.getInterpolation(it) }
            ObjectAnimator.ofInt(recycler_view, ScrollXProperty(enable_optimizations.isChecked), 0).apply {
                interpolator = interpolatorWrapper
                duration = 500L
            }.start()
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // AdapterView.OnItemSelectedListener

    override fun onNothingSelected(parent: AdapterView<*>) {
        interpolation_selection.setSelection(0)
    }

    override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
        interpolator = when (position) {
            0 -> AccelerateDecelerateInterpolator()
            1 -> AccelerateInterpolator()
            2 -> AnticipateInterpolator()
            3 -> AnticipateOvershootInterpolator()
            4 -> BounceInterpolator()
            5 -> CycleInterpolator(3f)
            6 -> DecelerateInterpolator()
            7 -> LinearInterpolator()
            8 -> OvershootInterpolator()
            else -> {
                onNothingSelected(parent)
                return
            }
        }
    }

}
