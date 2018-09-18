package com.yperess.stackoverflow

import android.os.Bundle
import android.view.animation.BaseInterpolator
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var interpolator: BaseInterpolator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

}
