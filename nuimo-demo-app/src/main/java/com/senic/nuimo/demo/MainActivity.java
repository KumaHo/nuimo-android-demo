package com.senic.nuimo.demo;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.ContentObservable;
import android.media.AudioManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ListView;

import com.senic.nuimo.NuimoController;
import com.senic.nuimo.NuimoControllerListener;
import com.senic.nuimo.NuimoDiscoveryListener;
import com.senic.nuimo.NuimoDiscoveryManager;
import com.senic.nuimo.NuimoGesture;
import com.senic.nuimo.NuimoGestureEvent;
import com.senic.nuimo.NuimoLedMatrix;

import org.jetbrains.annotations.NotNull;

import java.text.SimpleDateFormat;
import java.util.Date;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Revoking/granting permissions:
 *
 * ~/Library/Android/sdk/platform-tools/adb shell pm grant com.senic.nuimo.demo android.permission.ACCESS_COARSE_LOCATION
 * ~/Library/Android/sdk/platform-tools/adb shell pm revoke com.senic.nuimo.demo android.permission.ACCESS_COARSE_LOCATION
 *
 */
public class MainActivity extends AppCompatActivity implements NuimoDiscoveryListener, NuimoControllerListener {

    NuimoDiscoveryManager discovery;
    NuimoController controller;
    int ControlIndex = 0;

    @Bind(R.id.log)
    ListView logListView;
    LogArrayAdapter logAdapter;

    @Bind(R.id.led_animation)
    FloatingActionButton toggleAnimationButton;
    boolean animatingLed = false;
    int animationIndex;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        discovery = NuimoDiscoveryManager.init(getApplicationContext());
        ButterKnife.bind(this);

        logAdapter = new LogArrayAdapter(this);
        logListView.setAdapter(logAdapter);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        findViewById(R.id.discover).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                discoverAndConnect();
            }
        });
        findViewById(R.id.led_all_on).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                displayAllLedsOn();
            }
        });
        toggleAnimationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                animatingLed = !animatingLed;
                toggleAnimationButton.setImageResource(animatingLed ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play);
                if (animatingLed) {
                    animationIndex = 0;
                    displayMatrix(LED_ANIMATION_FRAMES[0]);
                }
            }
        });

        discovery.addDiscoveryListener(this);
    }

    @Override
    protected void onDestroy() {
        discovery.stopDiscovery();
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        discovery.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @SuppressLint("SimpleDateFormat")
    static SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS");

    public void log(final String text) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                logAdapter.add(new String[]{sdf.format(new Date()), text});
            }
        });
    }

    void discoverAndConnect() {
        if (controller != null) {
            controller.disconnect();
        }
        log("Start discovery");
        discovery.startDiscovery();
    }

    void displayAllLedsOn() {
        displayMatrix(
                "*********" +
                        "*********" +
                        "*********" +
                        "*********" +
                        "*********" +
                        "*********" +
                        "*********" +
                        "*********" +
                        "*********");
    }

    void displayMatrix(String string) {
        if (controller == null) return;
        controller.displayLedMatrix(new NuimoLedMatrix(string), 2.0);
        log("Send matrix");
    }

    /**
     * NuimoDiscoveryListener implementation
     */

    @Override
    public void onDiscoverNuimoController(@NotNull NuimoController nuimoController) {
        //if (!nuimoController.getAddress().equals(""))
        log("Discovered " + nuimoController.getAddress() + ". Trying to connect...");
        log("Stop discovery");
        discovery.stopDiscovery();
        controller = nuimoController;
        controller.addControllerListener(this);
        controller.connect();
    }

    @Override
    public void onLoseNuimoController(@NotNull NuimoController nuimoController) {
        log("Lose Nuimo Controller");
    }

    /**
     * NuimoControllerListener implementation
     */

    @Override
    public void onConnect() {
        log("Connected to " + (controller != null ? controller.getAddress() : "null"));
        displayAllLedsOn();
    }

    @Override
    public void onFailToConnect() {
        log("Failed to connect to " + (controller != null ? controller.getAddress() : "null"));
    }

    @Override
    public void onDisconnect() {
        log("Disconnected");
    }

    @Override
    public void onLedMatrixWrite() {
        log("Matrix written");
        if (animatingLed) {
            displayMatrix(LED_ANIMATION_FRAMES[(++animationIndex) % LED_ANIMATION_FRAMES.length]);
        }
    }

    @Override
    public void onGestureEvent(@NotNull NuimoGestureEvent event) {
        System.out.println(event.getGesture().toString() + ": " + event.getValue());
        String logText;
        switch (event.getGesture()) {
            case BUTTON_PRESS:
                logText = "Button pressed";
                break;
            case BUTTON_RELEASE:
                logText = "Button released";
                break;
            case SWIPE_LEFT:
                logText = "Swiped left";
                displayMatrix(VOLUME_BRIGHTNESS_LED[(--ControlIndex) % VOLUME_BRIGHTNESS_LED.length]);
                break;
            case SWIPE_RIGHT:
                logText = "Swiped right";
                displayMatrix(VOLUME_BRIGHTNESS_LED[(++ControlIndex) % VOLUME_BRIGHTNESS_LED.length]);
                break;
            case SWIPE_UP:
                logText = "Swiped up";
                break;
            case SWIPE_DOWN:
                logText = "Swiped down";
                break;
            case ROTATE:
                logText = "";// getLogTextForRotationEvent(event);
                if (ControlIndex % VOLUME_BRIGHTNESS_LED.length == 0) setVolume(event);
                else if (ControlIndex % VOLUME_BRIGHTNESS_LED.length == 1) setBrightness(event);
                break;
            case FLY_LEFT:
                logText = "Fly left, speed = " + event.getValue();
                break;
            case FLY_RIGHT:
                logText = "Fly right, speed = " + event.getValue();
                break;
            case FLY_BACKWARDS:
                logText = "Fly backwards, speed = " + event.getValue();
                break;
            case FLY_TOWARDS:
                logText = "Fly towards, speed = " + event.getValue();
                break;
            case FLY_UP_DOWN:
                logText = "Fly, distance = " + event.getValue();
                break;
            default:
                logText = event.getGesture().name();
        }
        log(logText);
    }

    @Override
    public void onBatteryPercentageChange(int i) {
        log("Battery percentage updated: " + i + "%");
    }

    Date lastRotationDate;
    int lastRotationDirection = 0;
    int accumulatedRotationValue = 0;

    private String getLogTextForRotationEvent(NuimoGestureEvent event) {
        Date now = new Date();
        double speed = 0;
        int rotationValue = (event.getValue() == null ? 0 : event.getValue());
        int rotationDirection = rotationValue > 0 ? 1 : -1;
        if (rotationDirection == lastRotationDirection && (now.getTime() - lastRotationDate.getTime() < 2000)) {
            speed = rotationValue / (double) (now.getTime() - lastRotationDate.getTime());
            accumulatedRotationValue += rotationValue;
        } else {
            accumulatedRotationValue = rotationValue;
        }
        lastRotationDirection = rotationDirection;
        lastRotationDate = now;
        return String.format("Rotated %d\n  Speed: %.3f, Accumulated: %d", rotationValue, speed, accumulatedRotationValue);
    }

    private void setVolume(NuimoGestureEvent event) {
        int rotationValue = (event.getValue() == null ? 0 : event.getValue());
        float setValue = 0;
        AudioManager audio = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);

        if (rotationValue > 0) setValue = (float) (rotationValue / 360);

        int currRing = audio.getStreamVolume(AudioManager.STREAM_RING);
        int currSystem = audio.getStreamVolume(AudioManager.STREAM_SYSTEM);
        int currMusic = audio.getStreamVolume(AudioManager.STREAM_MUSIC);


        int maxRingVolume = audio.getStreamMaxVolume(AudioManager.STREAM_RING);
        //int setRingVolume = (int) (maxRingVolume * (float)volumes / 100);
        int maxSystemVolume = audio.getStreamMaxVolume(AudioManager.STREAM_SYSTEM);
        //int setSystemVolume = (int) (maxSystemVolume * (float)volumes / 100);
        int maxMusicVolume = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        //int setMusicVolume = (int) (maxMusicVolume * (float)volumes / 100);

        currRing = currRing + (int) (maxRingVolume * ((float) rotationValue / 360));
        currSystem = currSystem + (int) (maxSystemVolume * ((float) rotationValue / 360));
        currMusic = currMusic + (int) (maxMusicVolume * ((float) rotationValue / 360));

        if (currRing > maxRingVolume) currRing = maxRingVolume;
        if (currSystem > maxSystemVolume) currSystem = maxSystemVolume;
        if (currMusic > maxMusicVolume) currMusic = maxMusicVolume;

        if (currRing < 0) currRing = 0;
        if (currSystem < 0) currSystem = 0;
        if (currMusic < 0) currMusic = 0;

        audio.setRingerMode(AudioManager.RINGER_MODE_NORMAL);


        audio.setStreamVolume(AudioManager.STREAM_RING, currRing, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
        audio.setStreamVolume(AudioManager.STREAM_SYSTEM, currSystem, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
        audio.setStreamVolume(AudioManager.STREAM_MUSIC, currMusic, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);

        displayMatrix(VOLUME_SCALE[(int) (10 * (float) currRing / maxRingVolume)]);

        log("curr: " + currRing + ", rotation: " + (float) rotationValue / 360 + ", max: " + maxRingVolume +
                "increase: " + (int) (maxRingVolume * (float) rotationValue / 360) + ", display index: " + (int) (10 * (float) currRing / maxRingVolume));
    }

    private void setBrightness(NuimoGestureEvent event) {
        int rotationValue = (event.getValue() == null ? 0 : event.getValue());
        double ratio = 0;
        float curBrightnessValue = 0;
        float maxBright = 255;
        float minBright = 0;

        if (rotationValue > 0) ratio = ((double) (rotationValue / 360)) * 255.00;
        //displayMatrix(VOLUME_BRIGHTNESS_LED[1]);

        try {
            curBrightnessValue = android.provider.Settings.System.getInt(
                    getContentResolver(), android.provider.Settings.System.SCREEN_BRIGHTNESS);
        } catch (Settings.SettingNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        displayMatrix(VOLUME_SCALE[(int) (9 * curBrightnessValue / maxBright)]);

        //log("Set brightness:"+setValue+", curr: "+curBrightnessValue);
        log("curr: " + curBrightnessValue + ", rotation: " + (float) rotationValue / 360 + ", max: " + maxBright +
                "increase: " + (int) (maxBright * (float) rotationValue / 360) + ", display index: " + (int) (9 * curBrightnessValue / maxBright));

        curBrightnessValue = curBrightnessValue + (int) (maxBright * ((float) rotationValue / 360));
        if (curBrightnessValue > maxBright) curBrightnessValue = maxBright;
        if (curBrightnessValue < 0) curBrightnessValue = minBright;

        android.provider.Settings.System.putInt(this.getContentResolver(),
                android.provider.Settings.System.SCREEN_BRIGHTNESS,
                (int) curBrightnessValue);


    }

    private static String[] LED_ANIMATION_FRAMES = new String[]{
            "    **   " +
                    "   ***   " +
                    "  ****   " +
                    "    **   " +
                    "    **   " +
                    "    **   " +
                    "    **   " +
                    "    **   " +
                    "   ****  ",

            "   ***   " +
                    "  *****  " +
                    " **   ** " +
                    "      ** " +
                    "     **  " +
                    "    **   " +
                    "   **    " +
                    "  ****** " +
                    " ******* ",

            " ******* " +
                    " ******* " +
                    "     **  " +
                    "    **   " +
                    "     **  " +
                    "      ** " +
                    " **   ** " +
                    " ******* " +
                    "  *****  ",
    };

    private static String[] VOLUME_BRIGHTNESS_LED = new String[]{
            "         " +
                    "     *   " +
                    "   *  *  " +
                    " ** *  * " +
                    " ** *  * " +
                    " ** *  * " +
                    "   *  *  " +
                    "     *   " +
                    "         ",
            "   ***   " +
                    "  *   *  " +
                    " *     * " +
                    " *     * " +
                    " *  *  * " +
                    " *  *  * " +
                    "  * * *  " +
                    "   ***   " +
                    "   ***   "
    };
    private static String[] BRIGHTNESS_SCALE = new String[]{

            "*        " +
                    "*        " +
                    "*        " +
                    "*        " +
                    "*        " +
                    "*        " +
                    "*        " +
                    "*        " +
                    "*        ",

            "**       " +
                    "**       " +
                    "**       " +
                    "**       " +
                    "**       " +
                    "**       " +
                    "**       " +
                    "**       " +
                    "**       ",

            "***      " +
                    "***      " +
                    "***      " +
                    "***      " +
                    "***      " +
                    "***      " +
                    "***      " +
                    "***      " +
                    "***      ",

            "****     " +
                    "****     " +
                    "****     " +
                    "****     " +
                    "****     " +
                    "****     " +
                    "****     " +
                    "****     " +
                    "****     ",

            "*****    " +
                    "*****    " +
                    "*****    " +
                    "*****    " +
                    "*****    " +
                    "*****    " +
                    "*****    " +
                    "*****    " +
                    "*****    ",

            "******   " +
                    "******   " +
                    "******   " +
                    "******   " +
                    "******   " +
                    "******   " +
                    "******   " +
                    "******   " +
                    "******   ",

            "*******  " +
                    "*******  " +
                    "*******  " +
                    "*******  " +
                    "*******  " +
                    "*******  " +
                    "*******  " +
                    "*******  " +
                    "*******  ",

            "******** " +
                    "******** " +
                    "******** " +
                    "******** " +
                    "******** " +
                    "******** " +
                    "******** " +
                    "******** " +
                    "******** ",

            "*********" +
                    "*********" +
                    "*********" +
                    "*********" +
                    "*********" +
                    "*********" +
                    "*********" +
                    "*********" +
                    "*********",
    };
    private static String[] VOLUME_SCALE = new String[]{
            "         " +
                    " *     * " +
                    "  *   *  " +
                    "   * *   " +
                    "    *    " +
                    "   * *   " +
                    "  *   *  " +
                    " *     * " +
                    "         ",
            "*        " +
                    "*        " +
                    "*        " +
                    "*        " +
                    "*        " +
                    "*        " +
                    "*        " +
                    "*        " +
                    "*        ",

            "**       " +
                    "**       " +
                    "**       " +
                    "**       " +
                    "**       " +
                    "**       " +
                    "**       " +
                    "**       " +
                    "**       ",

            "***      " +
                    "***      " +
                    "***      " +
                    "***      " +
                    "***      " +
                    "***      " +
                    "***      " +
                    "***      " +
                    "***      ",

            "****     " +
                    "****     " +
                    "****     " +
                    "****     " +
                    "****     " +
                    "****     " +
                    "****     " +
                    "****     " +
                    "****     ",

            "*****    " +
                    "*****    " +
                    "*****    " +
                    "*****    " +
                    "*****    " +
                    "*****    " +
                    "*****    " +
                    "*****    " +
                    "*****    ",

            "******   " +
                    "******   " +
                    "******   " +
                    "******   " +
                    "******   " +
                    "******   " +
                    "******   " +
                    "******   " +
                    "******   ",

            "*******  " +
                    "*******  " +
                    "*******  " +
                    "*******  " +
                    "*******  " +
                    "*******  " +
                    "*******  " +
                    "*******  " +
                    "*******  ",

            "******** " +
                    "******** " +
                    "******** " +
                    "******** " +
                    "******** " +
                    "******** " +
                    "******** " +
                    "******** " +
                    "******** ",

            "*********" +
                    "*********" +
                    "*********" +
                    "*********" +
                    "*********" +
                    "*********" +
                    "*********" +
                    "*********" +
                    "*********",
    };
}
