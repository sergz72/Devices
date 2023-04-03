package org.sz.drillironc

import android.os.AsyncTask
import com.github.kittinunf.fuel.httpPost
import com.github.kittinunf.result.Result
import com.google.gson.Gson
import org.sz.drillironc.entities.DrillIronCommand
import java.util.*

object DeviceTask: AsyncTask<MainActivity, Void, Unit>() {
    val mStatusCommand = DrillIronCommand("status", null, null, null)

    private val mCommands = LinkedList<DrillIronCommand>()
    private var mPause = true
    private var mStop = false
    private var mNetworkEnabled = true
    private var mUrl: String? = null
    private var mActivity: MainActivity? = null

    fun getUrl(): String? {
        return mUrl
    }

    fun setUrl(url: String): Boolean {
        mUrl = url
        mNetworkEnabled = !mUrl!!.contains(" ")
        return mNetworkEnabled
    }

    fun pause() {
        mPause = true
    }

    fun resume() {
        mPause = false
    }

    fun stop() {
        mStop = true
    }

    fun drillIronCommand(command: DrillIronCommand) {
        mCommands.add(command)
    }

    fun setActivity(activity: MainActivity) {
        mActivity = activity
    }

    fun enableNetwork() {
        mNetworkEnabled = true
    }

    fun disableNetwork() {
        mNetworkEnabled = false
    }

    override fun doInBackground(vararg activity: MainActivity) {
        mActivity = activity[0]
        var busy = false
        while (true) {
            Thread.sleep(1000)
            if (mStop) {
                if (!mNetworkEnabled) {
                    return
                }
                if (busy) {
                    continue
                }
                if (mCommands.isEmpty()) {
                    return
                }
            } else {
                if (mPause || busy || !mNetworkEnabled) {
                    continue
                }
            }
            if (mCommands.isEmpty()) {
                drillIronCommand(mStatusCommand)
            }
            val body = Gson().toJson(mCommands.peek())
            busy = true
            mUrl!!.httpPost().body(body).timeout(1000).responseString { _, _, result ->
                when (result) {
                    is Result.Failure -> {
                        mCommands.poll()
                        mNetworkEnabled = false
                        busy = false
                        val ex = result.getException()
                        mActivity?.handleDeviceTaskException(ex)
                    }
                    is Result.Success -> {
                        val command = mCommands.poll()
                        busy = false
                        val data = result.get()
                        mActivity?.handleDeviceTaskResult(command, data)
                    }
                }
            }
        }
    }
}