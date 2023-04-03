package org.sz.drillironc

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity

import kotlinx.android.synthetic.main.activity_main.toolbar
import kotlinx.android.synthetic.main.content_main.*
import androidx.appcompat.app.AlertDialog
import org.sz.drillironc.entities.DrillIronCommand
import android.os.Handler
import android.view.View
import com.google.gson.Gson
import org.sz.drillironc.entities.DrillIronStatus
import org.sz.drillironc.entities.HeaterProfile
import android.view.ViewGroup
import java.lang.Exception

class MainActivity : AppCompatActivity(), SharedPreferences.OnSharedPreferenceChangeListener, View.OnClickListener {
    companion object {
        const val PREFS_NAME = "Drillironc"
        private const val HOST_KEY = "serverUrl"
    }
    private val mProfilesCommand = DrillIronCommand("profiles", null, null, null)

    @Volatile private var mActiveAlert = false
    private val mHandler = Handler()
    private val mHeaterControls = mutableMapOf<Int, HeaterControl>()
    private var mButtonPressed: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        bnEnableNetwork.setOnClickListener(this)
        val initialized = DeviceTask.getUrl() != null
        readUrl()
        DeviceTask.drillIronCommand(mProfilesCommand)
        updatebuttonStatus()
        if (!initialized) {
            DeviceTask.execute(this)
        } else {
            DeviceTask.setActivity(this)
        }
   }

    private fun readUrl() {
        val settings = getSharedPreferences(PREFS_NAME, 0)
        val urlExists = settings.contains(HOST_KEY)
        updateUrl(settings)
        if (!urlExists) {
            val editor = settings.edit()
            editor.putString(HOST_KEY, DeviceTask.getUrl())
            editor.apply()
        }
    }

    private fun readSettings() {
        val settings = getSharedPreferences(PREFS_NAME, 0)
        drillControl.readSettings(settings)
        mHeaterControls.values.forEach { it.readSettings(settings)}
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    private fun startSettingsActivity(): Boolean {
        val intent = Intent(this, SettingsActivity::class.java)
        startActivity(intent)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> startSettingsActivity()
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onResume() {
        super.onResume()
        DeviceTask.resume()
    }

    override fun onPause() {
        super.onPause()
        DeviceTask.pause()
    }

    fun handleDeviceTaskException(ex: Exception) {
        mHandler.post { alert(ex.toString()) }
    }

    fun handleDeviceTaskResult(command: DrillIronCommand, data: String) {
        mHandler.post { handleResult(command, data) }
    }

    private fun handleResult(command: DrillIronCommand, data: String) {
        if (command == DeviceTask.mStatusCommand) {
            if (data.startsWith("{")) {
                showStatus(data)
            } else {
                DeviceTask.disableNetwork()
                alert(data)
            }
        } else if (command == mProfilesCommand) {
            if (data.startsWith("[")) {
                addProfiles(data)
            } else {
                DeviceTask.disableNetwork()
                alert(data)
            }
        } else {
            if (data != "Ok") {
                alert(data)
            }
        }
    }

    private fun addProfiles(data: String) {
        val profiles = Gson().fromJson(data, Array<HeaterProfile>::class.java)
        for (i in 0 until profiles.size) {
            val profile = profiles[i]
            val control = mHeaterControls.getOrPut(profile.HeaterID, { HeaterControl(this, null) })
            control.addProfile(i, profile)
            if (control.parent == null) {
                control.layoutParams =
                    ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                control.visibility = View.VISIBLE
                mainLayout.addView(control)
            }
        }
        readSettings()
        mainLayout.invalidate()
    }

    private fun showStatus(data: String) {
        val status = Gson().fromJson(data, DrillIronStatus::class.java)
        mHeaterControls.entries.forEach { (key, value) ->
            value.showStatus(status.HeaterTemperature[key - 1], status.ButtonPressed)
        }
        if (mButtonPressed != status.ButtonPressed) {
            mButtonPressed = status.ButtonPressed
            updatebuttonStatus()
        }
    }

    private fun updatebuttonStatus() {
        buttonStatus.text = if (mButtonPressed) "(button pressed)" else ""
    }

    override fun onStop() {
        super.onStop()
        val settings = getSharedPreferences(PREFS_NAME, 0)
        val editor = settings.edit()
        drillControl.saveSettings(editor)
        mHeaterControls.values.forEach { it.saveSettings(editor)}
        editor.apply()
        mHeaterControls.values.forEach { it.heaterOff()}
    }

    override fun onDestroy() {
        super.onDestroy()
        DeviceTask.stop()
    }

    private fun updateUrl(settings: SharedPreferences) {
        if (!DeviceTask.setUrl(settings.getString(HOST_KEY, "http://192.168.56.101:59998/command/DrillIronC")!!)) {
            alert("Incorrect URL")
        }
    }

    private fun alert(message: String) {
        if (mActiveAlert) return
        mActiveAlert = true
        val builder = AlertDialog.Builder(this)
        builder.setMessage(message)
            .setTitle("Error")
            .setOnDismissListener { mActiveAlert = false }
        val dialog = builder.create()
        dialog.show()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key == HOST_KEY) {
            updateUrl(sharedPreferences!!)
        }
    }

    override fun onClick(view: View?) {
        DeviceTask.enableNetwork()
    }
}
