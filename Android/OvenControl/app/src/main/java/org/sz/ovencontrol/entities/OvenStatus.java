package org.sz.ovencontrol.entities;

import com.google.gson.annotations.SerializedName;

public class OvenStatus {
    @SerializedName("Temperature")
    private int temperature;

    @SerializedName("ProgramStep")
    private int programStep;

    @SerializedName("TimeLeft")
    private int timeLeft;

    @SerializedName("HeaterOn")
    private boolean heaterOn;

    @SerializedName("StabMode")
    private boolean stabMode;

    @SerializedName("ErrorMessage")
    private String errorMessage;

    public int getTemperature() {
        return temperature;
    }

    public int getProgramStep() {
        return programStep;
    }

    public int getTimeLeft() {
        return timeLeft;
    }

    public boolean isHeaterOn() {
        return heaterOn;
    }

    public boolean isStabMode() {
        return stabMode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
