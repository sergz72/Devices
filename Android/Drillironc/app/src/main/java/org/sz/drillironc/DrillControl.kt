package org.sz.drillironc

import android.content.Context
import android.content.SharedPreferences
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.GridLayout
import kotlinx.android.synthetic.main.drill_layout.view.*
import org.sz.drillironc.entities.DrillIronCommand

class DrillControl(ctx: Context, attrs: AttributeSet) : GridLayout(ctx, attrs), View.OnClickListener {
    companion object {
        const val POWER_KEY = "drill.power"
        const val RISE_TIME_KEY = "drill.rise_time"
    }
    init {
        val inflater = LayoutInflater.from(ctx)
        inflater.inflate(R.layout.drill_layout, this)
        bnSet.setOnClickListener(this)
    }

    fun readSettings(settings: SharedPreferences) {
        powerControl.readSettings(settings, POWER_KEY)
        rtimeControl.readSettings(settings, RISE_TIME_KEY)
        onClick(null)
    }

    fun saveSettings(editor: SharedPreferences.Editor) {
        powerControl.saveSettings(editor, POWER_KEY)
        rtimeControl.saveSettings(editor, RISE_TIME_KEY)
    }

    override fun onClick(view: View?) {
        DeviceTask.drillIronCommand(DrillIronCommand("drillPower", adjust(powerControl.getValue()), null, null))
        DeviceTask.drillIronCommand(DrillIronCommand("drillRiseTime", rtimeControl.getValue(), null, null))
    }

    private fun adjust(value: Int) = if (value > 99) 99 else value
}