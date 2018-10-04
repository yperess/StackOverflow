package com.yperess.stackoverflow.settings

import android.os.Bundle
import android.preference.PreferenceManager
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import com.yperess.stackoverflow.MovieLiveWallpaperService
import com.yperess.stackoverflow.R
import kotlinx.android.synthetic.main.activity_main.*

/**
 *
 */
class SettingsActivity : AppCompatActivity(), AdapterView.OnItemSelectedListener {

    private val scaleTypes = MovieLiveWallpaperService.Mode.values().map { it.name }
    private val prefs by lazy { PreferenceManager.getDefaultSharedPreferences(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, scaleTypes).let {
            it.setDropDownViewResource(android.R.layout.simple_spinner_item)
            scale_type.adapter = it
        }
        scale_type.onItemSelectedListener = this
        scaleTypes.indexOf(scaleType)
                .takeIf { it >= 0 }
                ?.let { selectedIndex ->
                    scale_type.setSelection(selectedIndex)
                }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // AdapterView.OnItemSelectedListener

    override fun onNothingSelected(parent: AdapterView<*>) {
        scaleType = MovieLiveWallpaperService.Mode.CENTER_CROP.name
    }

    override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
        scaleType = scaleTypes[position]
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Private helper methods

    var scaleType: String
        get() = prefs.getString("scale_type", MovieLiveWallpaperService.Mode.CENTER_CROP.name)!!
        set(value) {
            prefs.edit().putString("scale_type", value).apply()
        }
}