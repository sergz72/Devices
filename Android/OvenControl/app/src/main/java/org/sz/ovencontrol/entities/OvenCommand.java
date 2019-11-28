package org.sz.ovencontrol.entities;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class OvenCommand {
    public static class OvenProgramItem {
        @SerializedName("temperature")
        private int temperature;

        @SerializedName("seconds")
        private int seconds;

        public OvenProgramItem() {

        }

        public OvenProgramItem(int t, int s) {
            temperature = t;
            seconds = s;
        }

        public int getTemperature() {
            return temperature;
        }

        public void setTemperature(int temperature) {
            this.temperature = temperature;
        }

        public int getSeconds() {
            return seconds;
        }

        public void setSeconds(int seconds) {
            this.seconds = seconds;
        }

        @Override
        public String toString() {
            return temperature + ":" + seconds;
        }
    }

    @SerializedName("command")
    private String command;

    @SerializedName("program")
    private List<OvenProgramItem> program;

    public OvenCommand() {
    }

    public OvenCommand(String command) {
        this.command = command;
        this.program = null;
    }

    public OvenCommand(String command, List<OvenProgramItem> program) {
        this.command = command;
        this.program = program;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public List<OvenProgramItem> getProgram() {
        return program;
    }

    public void setProgram(List<OvenProgramItem> program) {
        this.program = program;
    }

    @Override
    public String toString() {
        return command + (program == null ? "" : program.toString());
    }
}
