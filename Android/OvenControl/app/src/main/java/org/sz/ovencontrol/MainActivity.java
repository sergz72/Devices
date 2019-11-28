package org.sz.ovencontrol;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.OrderedXYSeries;
import com.androidplot.xy.StepMode;
import com.androidplot.xy.XYGraphWidget;
import com.androidplot.xy.XYPlot;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.sz.ovencontrol.entities.OvenCommand;
import org.sz.ovencontrol.entities.OvenStatus;

import java.text.FieldPosition;
import java.text.Format;
import java.text.ParseException;
import java.text.ParsePosition;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Collectors;

import static java.time.temporal.ChronoUnit.SECONDS;

public class MainActivity extends AppCompatActivity implements RestTask.Callback, View.OnClickListener,
        SharedPreferences.OnSharedPreferenceChangeListener, AdapterView.OnItemSelectedListener {
    private static class GraphDataSeries implements OrderedXYSeries {
        private final List<Integer> mData;
        private final String mDataName;

        GraphDataSeries(String dataName) {
            mData = new ArrayList<>();
            mDataName = dataName;
        }

        public void clear() {
            mData.clear();
        }

        public void addDataItem(int dataItem) {
            mData.add(dataItem);
        }

        @Override
        public int size() {
            return mData.size();
        }

        @Override
        public Number getX(int index) {
            return index;
        }

        @Override
        public Number getY(int index) {
            return mData.get(index);
        }

        @Override
        public String getTitle() {
            return mDataName;
        }

        @Override
        public XOrder getXOrder() {
            return XOrder.ASCENDING;
        }
    }

    private static final Format XLABEL_FORMATTER = new Format() {
        @Override
        public StringBuffer format(Object obj, @NonNull StringBuffer toAppendTo, @NonNull FieldPosition pos) {
            long time = ((Number)obj).longValue();
            return toAppendTo.append(String.format("%d", time / 60));
        }
        @Override
        public Object parseObject(String source, @NonNull ParsePosition pos) {
            return null;
        }
    };

    private static final Format YLABEL_FORMATTER = new Format() {
        @Override
        public StringBuffer format(Object obj, @NonNull StringBuffer toAppendTo, @NonNull FieldPosition pos) {
            long temperature = ((Number)obj).longValue();
            if (temperature == 0) {
                return toAppendTo.append("");
            }
            return toAppendTo.append(String.format("%d", temperature));
        }
        @Override
        public Object parseObject(String source, @NonNull ParsePosition pos) {
            return null;
        }
    };

    public static final String PREFS_NAME = "OvenControl";
    private static final String HOST_KEY = "serverUrl";
    private static final String PROGRAMS_KEY = "programs";
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("mm:ss");

    private static final OvenCommand STATUS_COMMAND = new OvenCommand("status");
    private static final OvenCommand STOP_COMMAND = new OvenCommand("stop");

    private RestTask mRestTask;
    private final Gson mGson;
    private String mUrl;
    private Timer mTimer;
    private final Handler mHandler;

    private TextView mTemperatureView;
    private TextView mTimeLeftView;
    private TextView mTimeView;
    private TextView mProgramStepView;
    private TextView mHeaterStatusView;
    private TextView mStabModeStatusView;
    private Button mStartButton;
    private Button mEndButton;
    private Spinner mProgramSelector;
    private XYPlot mPlot;

    private Queue<OvenCommand> mCommands;
    private Map<String, List<OvenCommand.OvenProgramItem>> mPrograms;
    private String mSelectedProgram;
    private LocalTime mStartTime;
    private boolean mActiveAlert;
    private int mProgramStep;
    private GraphDataSeries mDataSeries;
    private LineAndPointFormatter mLineFormatter;
    private boolean mNetworkEnabled;

    public MainActivity() {
        GsonBuilder gsonBuilder = new GsonBuilder();
        mGson = gsonBuilder.create();
        mHandler =  new Handler();
        //set a new Timer
        mTimer = null;
        mCommands = new LinkedList<>();
        mStartTime = null;
        mActiveAlert = false;
        mProgramStep = -1;
        mNetworkEnabled = true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mTemperatureView = findViewById(R.id.temperature_value);
        mTimeLeftView = findViewById(R.id.timeleft_value);
        mProgramStepView = findViewById(R.id.programStep_value);
        mHeaterStatusView = findViewById(R.id.heater_status);
        mStabModeStatusView = findViewById(R.id.stabMode_status);
        mTimeView = findViewById(R.id.time_value);
        mStartButton = (Button)findViewById(R.id.buttonStart);
        mStartButton.setOnClickListener(this);
        mEndButton = (Button)findViewById(R.id.buttonStop);
        mEndButton.setOnClickListener(this);
        mProgramSelector = (Spinner)findViewById(R.id.programSelector);
        mProgramSelector.setOnItemSelectedListener(this);
        mPlot = (XYPlot) findViewById(R.id.plot);
        mPlot.setRangeBoundaries(0, 250, BoundaryMode.FIXED); //temperature
        mPlot.setRangeStepMode(StepMode.INCREMENT_BY_VAL);
        mPlot.setRangeStepValue(50);
        mPlot.setDomainBoundaries(0, 300, BoundaryMode.FIXED); // time
        mPlot.setDomainStepMode(StepMode.INCREMENT_BY_VAL);
        mPlot.setDomainStepValue(60); // 1 minute
        mPlot.getGraph().getLineLabelStyle(XYGraphWidget.Edge.BOTTOM).setFormat(XLABEL_FORMATTER);
        mPlot.getGraph().getLineLabelStyle(XYGraphWidget.Edge.LEFT).setFormat(YLABEL_FORMATTER);
        mDataSeries = new GraphDataSeries("test");
        mLineFormatter = new LineAndPointFormatter(Color.RED, Color.GREEN, Color.TRANSPARENT, null);

        _readSettings();
    }

    private void _updateUrl(SharedPreferences settings) {
        mUrl = settings.getString(HOST_KEY, "http://192.168.56.101:59998/command/Oven");
        if (mUrl.contains(" ")) {
            _alert("Incorrect URL");
            mNetworkEnabled = false;
        } else {
            mNetworkEnabled = true;
        }
    }

    private void _readSettings() {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        boolean urlExists = settings.contains(HOST_KEY);
        _updateUrl(settings);
        if (!urlExists) {
            SharedPreferences.Editor editor = settings.edit();
            editor.putString(HOST_KEY, mUrl);
            editor.commit();
        }
        boolean programsExists = settings.contains(PROGRAMS_KEY);
        try {
            String programCodes = _parsePrograms(settings);
            if (!urlExists) {
                SharedPreferences.Editor editor = settings.edit();
                editor.putString(PROGRAMS_KEY, programCodes);
                editor.commit();
            }
        } catch (ParseException|NumberFormatException e) {
            _alert(e.getMessage());
        }
    }

    private void _addProgram(Map<String, List<OvenCommand.OvenProgramItem>> programMap, String programName, List<OvenCommand.OvenProgramItem> programItems,
                             int lineNo) throws ParseException {
        if (programName == null) {
            return;
        }
        if (programName.length() == 0) {
            throw new ParseException("Empty program name", lineNo);
        }
        if (programItems.isEmpty()) {
            throw new ParseException("Empty program", lineNo);
        }
        if (programMap.containsKey(programName)) {
            throw new ParseException("Duplicate program name", lineNo);
        }
        programMap.put(programName, new ArrayList<>(programItems));
        programItems.clear();
    }

    private String _parsePrograms(SharedPreferences settings) throws ParseException {
        String programCodes = settings.getString(PROGRAMS_KEY, getResources().getString(R.string.default_programs));
        String[] lines = programCodes.split("\n");
        Map<String, List<OvenCommand.OvenProgramItem>> programMap = new HashMap<>();
        List<OvenCommand.OvenProgramItem> programItems = new ArrayList<>();
        String programName = null;
        int lineNo = 1;
        for (String line : lines) {
            String[] parts = line.split(":");
            if (parts.length == 0) {
                continue;
            }
            if (parts.length == 1) {
                if (parts[0].charAt(0) <= ' ') {
                    continue;
                }
                _addProgram(programMap, programName, programItems, lineNo);
                programName = parts[0];
                continue;
            }
            int temperature = Integer.parseInt(parts[0]);
            int time = Integer.parseInt(parts[1]);
            if (time <= 0) {
                throw new ParseException("Invalid line: " + line, lineNo);
            }
            programItems.add(new OvenCommand.OvenProgramItem(temperature, time));
            lineNo++;
        }
        _addProgram(programMap, programName, programItems, lineNo);
        mPrograms = programMap;
        List<OvenCommand> commands = mPrograms.entrySet().stream()
                .map(entry -> new OvenCommand(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
        ArrayAdapter<OvenCommand> adapter =
                new ArrayAdapter<>(this, R.layout.support_simple_spinner_dropdown_item, commands);
        mProgramSelector.setAdapter(adapter);
        return programCodes;
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        mSelectedProgram = ((OvenCommand)parent.getSelectedItem()).getCommand();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        _alert("onNothingSelected");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void _alert(String message) {
        if (mActiveAlert) {
            return;
        }
        mActiveAlert = true;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(message)
                .setTitle("Error")
                .setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialogInterface) {
                        mActiveAlert = false;
                    }
                });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mRestTask != null) {
            mRestTask.cancel(true);
            mRestTask = null;
        }
    }

    @Override
    public void finishDownloading(RestTask.Result result) {
        mRestTask = null;
        if (result.mException != null) {
            _alert(result.mException.getMessage());
        } else {
            OvenCommand command = mCommands.poll();
            if (command == STATUS_COMMAND) {
                if (result.mResultValue.startsWith("{")) {
                    _showResults(result.mResultValue);
                } else {
                    _alert(result.mResultValue);
                }
            } else if (command == STOP_COMMAND) {
                _stop(result.mResultValue);
            } else {
                _start(result.mResultValue);
            }
        }
    }

    private void _stop(String results) {
        if (!results.equals("Ok")) {
            _alert(results);
            return;
        }
        mStartButton.setEnabled(true);
        mStartTime = null;
        mTimeView.setText("00:00");
    }

    private void _start(String results) {
        if (!results.equals("Ok")) {
            _alert(results);
            return;
        }
        mStartButton.setEnabled(false);
        mStartTime = LocalTime.now();
        mDataSeries.clear();
        mPlot.clear();
        mPlot.removeSeries(mDataSeries);
        mPlot.redraw();
    }

    private void _showResults(String results) {
        OvenStatus ovenStatus = mGson.fromJson(results, OvenStatus.class);
        mTemperatureView.setText(Integer.toString(ovenStatus.getTemperature()));
        mTimeLeftView.setText(Integer.toString(ovenStatus.getTimeLeft()));
        if (mStartTime != null) {
            long seconds = mStartTime.until(LocalTime.now(), SECONDS);
            mTimeView.setText(String.format("%02d:%02d", seconds / 60, seconds % 60));
        }
        mHeaterStatusView.setText(ovenStatus.isHeaterOn() ? "On" : "Off");
        mStabModeStatusView.setText(ovenStatus.isStabMode() ? "On" : "Off");
        mProgramStepView.setText(Integer.toString(ovenStatus.getProgramStep()));
        if (mProgramStep >= 0 && ovenStatus.getProgramStep() == -1) {
            mStartButton.setEnabled(true);
        }
        mProgramStep = ovenStatus.getProgramStep();
        if (mProgramStep >= 0) {
            mPlot.clear();
            mPlot.removeSeries(mDataSeries);
            mDataSeries.addDataItem(ovenStatus.getTemperature());
            mPlot.addSeries(mDataSeries, mLineFormatter);
            mPlot.redraw();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        mTimer = new Timer();
        //initialize the TimerTask's job
        TimerTask timerTask = _initializeTimerTask();
        mTimer.schedule(timerTask, 0, 1000);
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;
        }
    }

    private TimerTask _initializeTimerTask() {
        return new TimerTask() {
            public void run() {
                mHandler.post(new Runnable() {
                    public void run() {
                        _timerTask();
                    }
                });
            }
        };
    }

    private void _timerTask() {
        if (!mNetworkEnabled) {
            return;
        }
        if (mRestTask == null) {
            mRestTask = new RestTask(this);
            if (mCommands.isEmpty()) {
                mCommands.add(STATUS_COMMAND);
            }
            String body = mGson.toJson(mCommands.peek());
            mRestTask.execute(new RestTask.Parameters(mUrl, "POST", body));
        }
    }

    @Override
    public void onClick(View v) {
        if (v == mStartButton) {
            if (mSelectedProgram == null) {
                _alert("No selected program");
                return;
            }
            mCommands.add(new OvenCommand("start", mPrograms.get(mSelectedProgram)));
        } else {
            mCommands.add(STOP_COMMAND);
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key == HOST_KEY) {
            _updateUrl(sharedPreferences);
            return;
        }
        try {
            _parsePrograms(sharedPreferences);
        }
        catch (ParseException| NumberFormatException e) {
            _alert(e.getMessage());
        }
    }
}

