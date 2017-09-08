package io.github.zeleven.recorder;

import android.content.Intent;
import android.graphics.Color;
import android.os.Environment;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.view.menu.MenuBuilder;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Main activity
 */
public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private Chronometer chronometer;
    private Button cancelButton;
    private Button recordButton;
    private Button stopButton;
    private TextView recordStatusText;

    private boolean isStart = false;
    private boolean isRecording = false;
    private int pointCount;
    private long interruptTime;
    private String fileName;
    private String filePath;
    private long recordStartTime;
    private File file;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setPopupTheme(R.style.Theme_AppCompat_Light);
        if(toolbar != null) {
            setSupportActionBar(toolbar);
        }

        // find the element in layout
        chronometer = (Chronometer) findViewById(R.id.chronometer);
        cancelButton = (Button) findViewById(R.id.btn_cancel);
        recordButton = (Button) findViewById(R.id.btn_record);
        stopButton = (Button) findViewById(R.id.btn_stop);
        recordStatusText = (TextView) findViewById(R.id.record_status_text);

        // Setting click listener for cancel button and disable it before record
        cancelButton.setOnClickListener(this);
        cancelButton.setEnabled(false);

        recordButton.setOnClickListener(this);

        // Setting click listener for stop button and disable it before record
        stopButton.setOnClickListener(this);
        stopButton.setEnabled(false);

        // Creating folder to save the recording files
        File folder = new File(Environment.getExternalStorageDirectory() + "/Recorder");
        if(!folder.exists()) {
            folder.mkdir();
        }
    }

    /**
     * Creating menu on toolbar
     * @param menu
     * @return
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (menu instanceof MenuBuilder) {
            ((MenuBuilder) menu).setOptionalIconsVisible(true);
        }
        getMenuInflater().inflate(R.menu.toolbar_menu, menu);
        return true;
    }

    /**
     * The menu item click listener
     * @param item item of menu
     * @return
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        switch (item.getItemId()) {
            // start settings activity
            case R.id.action_setting:
                intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                return true;
            // start file list activity
            case R.id.action_file_view:
                intent = new Intent(this, FilesActivity.class);
                startActivity(intent);
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * The buttons click listeners
     * @param view
     */
    @Override
    public void onClick(View view) {
        Intent intent = new Intent(this, RecordingService.class);
        switch (view.getId()) {
            case R.id.btn_record:
                changeButtonStyle(true, R.drawable.round_button_small, "#000000");
                onRecord(intent);
                break;
            case R.id.btn_cancel:
                changeButtonStyle(false, R.drawable.button_disable, "#dddddd");
                cancelRecord(intent);
                break;
            case R.id.btn_stop:
                changeButtonStyle(false, R.drawable.button_disable, "#dddddd");
                stopRecord(intent);
                break;
            default:
                break;
        }
    }

    /**
     * The record process
     * @param intent
     */
    private void onRecord(Intent intent) {
        // If the record process is not start, start it.
        // If the record process has start, and if the user click the record button,
        // pause the record process, otherwise resume it.
        if(!isStart) {
            recordButton.setBackgroundResource(R.drawable.round_button_pause);

            // The toast message to hint user record process has start
            Toast.makeText(this, getString(R.string.toast_started_record),
                    Toast.LENGTH_SHORT).show();

            isStart = true;
            isRecording = true;

            // Setting chronometer
            chronometer.setBase(SystemClock.elapsedRealtime());
            chronometer.start();
            chronometer.setOnChronometerTickListener(new Chronometer.OnChronometerTickListener() {
                @Override
                public void onChronometerTick(Chronometer chronometer) {
                    if(pointCount == 0) {
                        recordStatusText.setText(getString(R.string.record_status_text_recording)
                                + ".");
                    } else if(pointCount == 1) {
                        recordStatusText.setText(getString(R.string.record_status_text_recording)
                                + "..");
                    } else {
                        recordStatusText.setText(getString(R.string.record_status_text_recording)
                                + "...");
                        pointCount = -1;
                    }
                    pointCount++;
                }
            });

            // Creating file to save the record content
            setFileNameAndPath();

            intent.putExtra("filePath", filePath);
            recordStartTime = System.currentTimeMillis();
            startService(intent);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

            recordStatusText.setText(getString(R.string.record_status_text_recording) + ".");
            pointCount++;
        } else {
            if(isRecording) {
                Toast.makeText(this, getString(R.string.toast_paused_record),
                        Toast.LENGTH_SHORT).show();
                recordButton.setBackgroundResource(R.drawable.round_button);
                recordStatusText.setText(getString(R.string.record_status_text_paused));
                interruptTime = chronometer.getBase() - SystemClock.elapsedRealtime();
                chronometer.stop();

                isRecording = false;
            } else {
                Toast.makeText(this, getString(R.string.toast_resume_record),
                        Toast.LENGTH_SHORT).show();
                recordButton.setBackgroundResource(R.drawable.round_button_pause);
                recordStatusText.setText(getString(R.string.record_status_text_recording) + ".");
                chronometer.setBase(SystemClock.elapsedRealtime() + interruptTime);
                chronometer.start();

                isRecording = true;
            }
        }
    }

    /**
     * To stop the record process and save file
     * @param intent used to stop record service
     */
    public void stopRecord(Intent intent) {
        interruptRecord(intent);
    }

    /**
     * To stop the record process and remove the file
     * @param intent used to stop record service
     */
    public void cancelRecord(Intent intent) {
        interruptRecord(intent);
        file.delete();
    }

    /**
     * To interrupt the record process, if the user click stop or cancel button.
     * @param intent used to stop record service
     */
    public void interruptRecord(Intent intent) {
        recordButton.setBackgroundResource(R.drawable.round_button);
        chronometer.stop();
        chronometer.setBase(SystemClock.elapsedRealtime());
        interruptTime = 0;

        recordStatusText.setText(getString(R.string.reocrd_status_text_click_to_record));
        isStart = false;
        stopService(intent);
    }

    /**
     * To change the button style, if the user click stop or cancel button
     * @param isEnabled whether enable the button, if it's true, enable it, otherwise disable
     * @param resid The resource's id used to change the button background
     * @param colorValue color value used to change button text color
     */
    public void changeButtonStyle(boolean isEnabled, int resid, String colorValue) {
        cancelButton.setEnabled(isEnabled);
        cancelButton.setBackgroundResource(resid);
        cancelButton.setTextColor(Color.parseColor(colorValue));
        stopButton.setEnabled(isEnabled);
        stopButton.setBackgroundResource(resid);
        stopButton.setTextColor(Color.parseColor(colorValue));
    }

    /**
     * Creating file before record process
     */
    private void setFileNameAndPath() {
        Calendar c = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
        dateFormat.format(c.getTime());
        fileName = dateFormat.format(c.getTime()) + ".mp3";
        filePath = Environment.getExternalStorageDirectory().getAbsolutePath();
        filePath += "/Recorder/" + fileName;
        file = new File(filePath);
    }
}
