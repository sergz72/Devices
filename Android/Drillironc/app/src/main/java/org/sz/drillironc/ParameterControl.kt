package org.sz.drillironc

import android.content.Context
import android.content.SharedPreferences
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.SeekBar
import kotlinx.android.synthetic.main.parameter_layout.view.*

private const val VALUE_KEY = ".value"

class ParameterControl(ctx: Context, attrs: AttributeSet) : LinearLayout(ctx, attrs), SeekBar.OnSeekBarChangeListener,
    View.OnClickListener {

    interface ParameterChangeListener {
        fun onParameterChanged(c: ParameterControl, value: Int)
    }

    private val mDivider: Int
    private val mMinValue: Int
    private val mStep: Int
    private var mParameterChangeListener: ParameterChangeListener? = null

    init {
        val inflater = LayoutInflater.from(ctx)
        inflater.inflate(R.layout.parameter_layout, this)

        val typedArray = context.theme.obtainStyledAttributes(attrs, R.styleable.ParameterControl,
                                                              0, 0)
        mDivider = typedArray.getInt(R.styleable.ParameterControl_dividerValue, 1)
        mMinValue = typedArray.getInt(R.styleable.ParameterControl_minValue, 0)
        val maxValue = typedArray.getInt(R.styleable.ParameterControl_maxValue, 10)
        mStep = typedArray.getInt(R.styleable.ParameterControl_step, 0)
        typedArray.recycle()

        val minValueText = mMinValue.formatValue()
        parameterValue.text = minValueText
        parameterBar.max = maxValue - mMinValue
        parameterBar.setOnSeekBarChangeListener(this)
        parameterMinLabel.text = minValueText
        parameterMaxLabel.text = maxValue.formatValue()
        parameterDecrease.setOnClickListener(this)
        parameterIncrease.setOnClickListener(this)
    }

    fun setParameterChangeListener(l: ParameterChangeListener) {
        mParameterChangeListener = l
    }

    private fun Int.formatValue(): String {
        return if (mDivider == 1) {
            toString()
        } else {
            "%d.%d".format(this / 10, this % 10)
        }
    }

    private fun updateParameterValue(v: Int) {
        val vReal = v + mMinValue
        parameterValue.text = vReal.formatValue()
        mParameterChangeListener?.onParameterChanged(this, vReal)
    }

    override fun onProgressChanged(sb: SeekBar?, value: Int, fromUser: Boolean) {
        if (fromUser) {
            val v = (value / mStep) * mStep
            sb?.progress = v
            updateParameterValue(v)
        }
    }

    override fun onStartTrackingTouch(sb: SeekBar?) {
    }

    override fun onStopTrackingTouch(sb: SeekBar?) {
    }

    fun readSettings(settings: SharedPreferences, prefix: String) {
        val v = settings.getInt(prefix + VALUE_KEY, 0)
        parameterBar.progress = v
        updateParameterValue(v)
    }

    fun saveSettings(editor: SharedPreferences.Editor, prefix: String) {
        editor.putInt(prefix + VALUE_KEY, parameterBar.progress)
    }

    override fun onClick(view: View?) {
        var v = parameterBar.progress
        if (view == parameterDecrease) {
            if (v > 0) {
                v = if (v > mStep) v - mStep else 0
                parameterBar.progress = v
                updateParameterValue(v)
            }
        } else {
            if (v < parameterBar.max) {
                v += mStep
                if (v > parameterBar.max) v = parameterBar.max
                parameterBar.progress = v
                updateParameterValue(v)
            }
        }
    }

    fun getValue(): Int {
        return parameterBar.progress + mMinValue
    }

    fun setValue(value: Int) {
        val progressValue = value - mMinValue
        if (progressValue >= 0 && progressValue <= parameterBar.max && parameterBar.progress != progressValue) {
            parameterValue.text = value.formatValue()
            parameterBar.progress = progressValue
        }
    }
}