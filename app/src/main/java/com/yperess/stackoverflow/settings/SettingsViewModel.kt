// Copyright (c) 2018 Magicleap. All right reserved.

package com.yperess.stackoverflow.settings

import android.app.Application
import android.preference.PreferenceManager
import android.view.View
import android.widget.AdapterView
import androidx.databinding.ObservableField
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import com.yperess.stackoverflow.MovieLiveWallpaperService

/**
 *
 */
class SettingsViewModel(
    application: Application
) : AndroidViewModel(application), AdapterView.OnItemSelectedListener {

    val scaleTypes = ObservableField<List<String>>()

    val scaleType = ObservableField<String>()

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // AdapterView.OnItemSelectedListener

    override fun onNothingSelected(parent: AdapterView<*>) {}

    override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
        PreferenceManager.getDefaultSharedPreferences(view.context)
                .edit()
                .putString("scale_type", parent.adapter.getItem(position).toString())
                .apply()
    }

    init {
        scaleTypes.set(MovieLiveWallpaperService.Mode.values().map { it.name })
        scaleType.set(PreferenceManager.getDefaultSharedPreferences(application)
                .getString("scale_type", MovieLiveWallpaperService.Mode.CENTER_CROP.name))
    }
}