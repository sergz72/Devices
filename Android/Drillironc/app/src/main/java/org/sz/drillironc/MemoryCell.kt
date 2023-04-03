package org.sz.drillironc

import android.content.SharedPreferences
import android.view.View
import android.widget.Button

class MemoryCell(val id: String, private val setButton: Button, private val getButton: Button,
                 private val temperatureMinControl: ParameterControl,
                 private val temperatureMaxControl: ParameterControl) : View.OnClickListener {
    private var mTemperatureMin: Int = 0
    private var mTemperatureMax: Int = 0

    init {
        setButton.setOnClickListener(this)
        getButton.setOnClickListener(this)
    }

    override fun onClick(view: View?) {
        if (view == setButton) {
            mTemperatureMin = temperatureMinControl.getValue()
            mTemperatureMax = temperatureMaxControl.getValue()
            updateGetButton()
        } else {
            if (mTemperatureMin != 0)
            {
                temperatureMinControl.setValue(mTemperatureMin)
                temperatureMaxControl.setValue(mTemperatureMax)
            }
        }
    }

    private fun buildKey(prefix: String, parameterName: String): String {
        return "$prefix.$id$parameterName"
    }

    fun readSettings(settings: SharedPreferences, prefix: String) {
        mTemperatureMin = settings.getInt(buildKey(prefix, HeaterControl.mTemperatureMinKey), 0)
        mTemperatureMax = settings.getInt(buildKey(prefix, HeaterControl.mTemperatureMaxKey), 0)
        updateGetButton()
    }

    fun saveSettings(editor: SharedPreferences.Editor, prefix: String) {
        if (mTemperatureMin > 0) {
            editor.putInt(buildKey(prefix, HeaterControl.mTemperatureMinKey), mTemperatureMin)
            editor.putInt(buildKey(prefix, HeaterControl.mTemperatureMaxKey), mTemperatureMax)
        }
    }

    private fun updateGetButton() {
        getButton.text = if (mTemperatureMin > 0) {
            "$id $mTemperatureMax/$mTemperatureMin"
        } else {
            id
        }
    }
}