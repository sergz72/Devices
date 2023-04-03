package org.sz.drillironc

import android.content.Context
import android.content.SharedPreferences
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.GridLayout
import kotlinx.android.synthetic.main.heater_layout.view.*
import org.sz.drillironc.entities.HeaterProfile
import android.widget.ArrayAdapter
import org.sz.drillironc.entities.DrillIronCommand

class HeaterControl(ctx: Context, attrs: AttributeSet?) : GridLayout(ctx, attrs), ParameterControl.ParameterChangeListener,
    AdapterView.OnItemSelectedListener, View.OnClickListener {
    companion object {
        const val mTemperatureMaxKey = ".temperature_max"
        const val mTemperatureMinKey = ".temperature_min"
    }
    private val mProfiles = HashMap<HeaterProfile, Int>()
    private val mProfilesArray = ArrayList<HeaterProfile>()
    private var mSelectedProfile: HeaterProfile? = null
    private var mSettings: SharedPreferences? = null
    private val mMemoryCells: Array<MemoryCell?> = Array(3) { null }
    private var mHeaterOn = true
    private var mTemperatureMin = 0
    private var mTemperatureMax = 0

    init {
        val inflater = LayoutInflater.from(ctx)
        inflater.inflate(R.layout.heater_layout, this)
        temperatureMaxControl.setParameterChangeListener(this)
        temperatureMinControl.setParameterChangeListener(this)
        profileName.onItemSelectedListener = this
        mMemoryCells[0] = MemoryCell("M1", bnM1Set, bnM1, temperatureMinControl, temperatureMaxControl)
        mMemoryCells[1] = MemoryCell("M2", bnM2Set, bnM2, temperatureMinControl, temperatureMaxControl)
        mMemoryCells[2] = MemoryCell("M3", bnM3Set, bnM3, temperatureMinControl, temperatureMaxControl)
        bnSet.setOnClickListener(this)
        bnOff.setOnClickListener(this)
    }

    fun addProfile(profileId: Int, profile: HeaterProfile) {
        mProfiles[profile] = profileId
        mProfilesArray.add(profile)
    }

    fun readSettings(settings: SharedPreferences) {
        val adapter = ArrayAdapter(context, R.layout.spinner_item, mProfilesArray)
        profileName.adapter = adapter
        mSettings = settings
        heaterOff()
        readSettings()
    }

    private fun buildPrefix(): String {
        return mSelectedProfile?.Title.hashCode().toString()
    }

    private fun readSettings() {
        val prefix = buildPrefix()
        temperatureMaxControl.readSettings(mSettings!!, prefix + mTemperatureMaxKey)
        temperatureMinControl.readSettings(mSettings!!, prefix + mTemperatureMinKey)
        mMemoryCells.forEach { cell -> cell!!.readSettings(mSettings!!, prefix) }
    }

    fun saveSettings(editor: SharedPreferences.Editor) {
        val prefix = buildPrefix()
        temperatureMaxControl.saveSettings(editor, prefix + mTemperatureMaxKey)
        temperatureMinControl.saveSettings(editor, prefix + mTemperatureMinKey)
        mMemoryCells.forEach { cell -> cell!!.saveSettings(editor, prefix) }
    }

    override fun onParameterChanged(c: ParameterControl, value: Int) {
        if (mSelectedProfile?.DualTemperatureEnable == false) {
            return
        }
        if (c == temperatureMaxControl) {
            if (temperatureMaxControl.getValue() < temperatureMinControl.getValue()) {
                temperatureMinControl.setValue(temperatureMaxControl.getValue())
            }
        } else {
            if (temperatureMinControl.getValue() > temperatureMaxControl.getValue()) {
                temperatureMaxControl.setValue(temperatureMinControl.getValue())
            }
        }
    }

    override fun onNothingSelected(parentView: AdapterView<*>?) {
    }

    override fun onItemSelected(parentView: AdapterView<*>?, selectedItemView: View?, position: Int, id: Long) {
        val editor = mSettings!!.edit()
        saveSettings(editor)
        editor.apply()
        mSelectedProfile = mProfilesArray[position]
        readSettings()
        temperatureMaxControl.isEnabled = mSelectedProfile!!.DualTemperatureEnable
        heaterOff()
    }

    override fun onClick(view: View?) {
        if (view == bnSet) {
            setHeaterTemperature()
        } else { //bnOff
            heaterOff()
        }
    }

    fun heaterOff() {
        DeviceTask.drillIronCommand(DrillIronCommand("heaterOff", mProfilesArray[0].HeaterID, null, null))
        mHeaterOn = false
    }

    private fun getMaxTemperature(): Int {
        return if (mSelectedProfile!!.DualTemperatureEnable) temperatureMaxControl.getValue() else temperatureMinControl.getValue()
    }

    private fun setHeaterTemperature() {
        val profileId = mProfiles[mSelectedProfile]
        mTemperatureMax = getMaxTemperature()
        mTemperatureMin = temperatureMinControl.getValue()
        DeviceTask.drillIronCommand(DrillIronCommand("profileOn", profileId,
                                      mTemperatureMin, mTemperatureMax))
        mHeaterOn = true
    }

    fun showStatus(statusValue: Int, buttonPressed: Boolean) {
        status.text = statusValue.toString()
        if (mHeaterOn) {
            temperatureValue.text = if (buttonPressed) mTemperatureMax.toString()
                                    else mTemperatureMin.toString()
        } else {
            temperatureValue.text = "OFF"
        }
    }
}