package com.gmail.taikingyo.modelviewer;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.MenuItem;
import android.support.v4.app.NavUtils;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;

import com.gmail.taikingyo.modelviewer.mqo.MQODoc;
import com.gmail.taikingyo.modelviewer.mqo.MQOParser;
import com.gmail.taikingyo.modelviewer.pmx.PMXDoc;
import com.gmail.taikingyo.modelviewer.pmx.PMXParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.LinkedList;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class MainActivity extends AppCompatActivity implements SensorEventListener {
    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static final boolean AUTO_HIDE = true;

    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

    /**
     * Some older devices needs a small delay between UI widget updates
     * and a change of the status and navigation bar.
     */
    private static final int UI_ANIMATION_DELAY = 300;
    private final Handler mHideHandler = new Handler();
    private FrameLayout mContentView;
    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            // Delayed removal of status and navigation bar

            // Note that some of these constants are new as of API 16 (Jelly Bean)
            // and API 19 (KitKat). It is safe to use them, as they are inlined
            // at compile-time and do nothing on earlier devices.
            mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    };
    private View mControlsView;
    private final Runnable mShowPart2Runnable = new Runnable() {
        @Override
        public void run() {
            // Delayed display of UI elements
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.show();
            }
            mControlsView.setVisibility(View.VISIBLE);
        }
    };
    private boolean mVisible;
    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };
    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    private final View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (AUTO_HIDE) {
                delayedHide(AUTO_HIDE_DELAY_MILLIS);
            }
            return false;
        }
    };

    private static final String SOLID_VERTEX_SHADER = "solidVertexShader.txt";
    private static final String SOLID_FRAGMENT_SHADER = "solidFragmentShader.txt";
    private static final String TEXTURE_VERTEX_SHADER = "texVertexShader.txt";
    private static final String TEXTURE_FRAGMENT_SHADER = "texFragmentShader.txt";

    GLRenderer renderer;
    GLSurfaceView glView;

    private ArrayList<SimpleSegment> visibles = new ArrayList<SimpleSegment>();
    private SensorManager sensorManager;
    private Sensor rotationVectorSensor;
    private float[] prevValues5 = {0.0f, 0.0f, 0.0f, 0.0f, 0.0f};
    private float[] prevValues4 = {0.0f, 0.0f, 0.0f, 0.0f};

    @RequiresApi(api = Build.VERSION_CODES.CUPCAKE)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        mVisible = true;
        mControlsView = findViewById(R.id.fullscreen_content_controls);
        mContentView = (FrameLayout) findViewById(R.id.fullscreen_content);


        // Set up the user interaction to manually show or hide the system UI.
        mContentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggle();
            }
        });

        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.
        findViewById(R.id.controll_button).setOnTouchListener(mDelayHideTouchListener);

        Intent intent = getIntent();
        String fileName = intent.getStringExtra(getString(R.string.iData_name));

        try {
            String[] shaders = new String[4];
            shaders[0] = Utils.loadFromAsset(this, SOLID_VERTEX_SHADER);
            shaders[1] = Utils.loadFromAsset(this, SOLID_FRAGMENT_SHADER);
            shaders[2] = Utils.loadFromAsset(this, TEXTURE_VERTEX_SHADER);
            shaders[3] = Utils.loadFromAsset(this, TEXTURE_FRAGMENT_SHADER);

            LinkedList<Node> model = new LinkedList<Node>();
            Bitmap[] maps;

            if(fileName.endsWith(".mqo")) {
                MQOParser parser = new MQOParser(this, fileName);
                MQODoc doc = parser.doc;
                model.add(doc.buildNode());
                maps = parser.getTextures();
            }else if(fileName.endsWith(".pmx")) {
                PMXParser parser = new PMXParser(this, fileName);
                PMXDoc doc = parser.doc;
                model.add(doc.buildNode());
                maps = doc.textures;
            }else {
                maps = new Bitmap[0];
            }

            final AlertDialog optionDialog = initOptionDialog(model);

            renderer = new GLRenderer(shaders, model, maps);
            glView = Utils.initView(this, renderer);
            mContentView.addView(glView);

            sensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
            rotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
            findViewById(R.id.controll_button).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    optionDialog.show();
                }
            });
        }catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            // This ID represents the Home or Up button.
            NavUtils.navigateUpFromSameTask(this);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void toggle() {
        if (mVisible) {
            hide();
        } else {
            show();
        }
    }

    private void hide() {
        // Hide UI first
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        mControlsView.setVisibility(View.GONE);
        mVisible = false;

        // Schedule a runnable to remove the status and navigation bar after a delay
        mHideHandler.removeCallbacks(mShowPart2Runnable);
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
    }

    @SuppressLint("InlinedApi")
    private void show() {
        // Show the system bar
        mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        mVisible = true;

        // Schedule a runnable to display UI elements after a delay
        mHideHandler.removeCallbacks(mHidePart2Runnable);
        mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY);
    }

    /**
     * Schedules a call to hide() in [delay] milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }

    @Override
    protected void onResume() {
        super.onResume();
        glView.onResume();
        sensorManager.registerListener(this, rotationVectorSensor, SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    protected void onPause() {
        super.onPause();
        glView.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        for(int i = 0; i < sensorEvent.values.length; i++) {
            prevValues5[i] = (prevValues5[i] + sensorEvent.values[i]) / 2.0f;
        }

        float[] rotationMatrix = new float[16];
        if(sensorEvent.values.length == 4) {
            System.arraycopy(prevValues5, 0, prevValues4, 0, 4);
            SensorManager.getRotationMatrixFromVector(rotationMatrix, prevValues4);
        }else {
            SensorManager.getRotationMatrixFromVector(rotationMatrix, prevValues5);
        }

        renderer.rotateView(rotationMatrix);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }


    private AlertDialog initOptionDialog(LinkedList<Node> model) {
        addVisible(model);
        final CheckBox[] checks = new CheckBox[visibles.size()];

        ScrollView scroll = new ScrollView(this);
        LinearLayout optionLayout = new LinearLayout(this);
        optionLayout.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(optionLayout);

        TextView text = new TextView(this);
        text.setText("rotation method");
        final RadioGroup rotationGroup = new RadioGroup(this);
        RadioButton sensor = new RadioButton(this);
        sensor.setText("sensor");
        RadioButton touch = new RadioButton(this);
        touch.setText("touch");
        RadioButton stop = new RadioButton(this);
        stop.setText("stop");
        rotationGroup.addView(sensor);
        rotationGroup.addView(touch);
        rotationGroup.addView(stop);
        sensor.setChecked(true);
        optionLayout.addView(text);
        optionLayout.addView(rotationGroup);

        TextView visible = new TextView(this);
        visible.setText("visible segment");
        optionLayout.addView(visible);

        for(int i = 0; i < visibles.size(); i++) {
            checks[i] = new CheckBox(this);
            checks[i].setText(visibles.get(i).getName());
            checks[i].setChecked(visibles.get(i).isVisible());
            optionLayout.addView(checks[i]);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Option");
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

                for(int j = 0; j < visibles.size(); j++) {
                    visibles.get(j).setVisible(checks[j].isChecked());
                }

                int rot = rotationGroup.getCheckedRadioButtonId();
                if(rot == 1) {
                    Log.i("MainActivity", "sensor");
                    //renderer.lock(false);
                    sensorManager.registerListener(MainActivity.this, rotationVectorSensor, SensorManager.SENSOR_DELAY_UI);
                }else if(rot == 2) {
                    Log.i("MainActivity", "touch(unimplemented)");
                    //renderer.lock(true);
                    sensorManager.unregisterListener(MainActivity.this);
                }else {
                    Log.i("MainActivity", "stop");
                    //renderer.lock(true);
                    sensorManager.unregisterListener(MainActivity.this);
                }
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

            }
        });

        builder.setView(scroll);
        AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.argb(128, 192, 192, 192)));
        return dialog;
    }

    private void addVisible(LinkedList<Node> model) {
        for(Node node : model) {
            if(node instanceof Group) {
                if(node instanceof SimpleSegment) visibles.add((SimpleSegment) node);
                addVisible(((Group) node).getChildren());
            }
        }
    }

    private LinkedList<Node> buildTestSegment() {
        FloatBuffer positions = Utils.buildFloatBuffer(new float[] {
                1.0f,  5.0f,  1.0f,    -1.0f,  5.0f,  1.0f,    -1.0f,  0.0f,  1.0f,     1.0f,  0.0f,  1.0f, //前(z = 1.0)
                1.0f,  5.0f,  1.0f,     1.0f,  0.0f,  1.0f,     1.0f,  0.0f, -1.0f,     1.0f,  5.0f, -1.0f, //右(x = 1.0)
                1.0f,  5.0f,  1.0f,     1.0f,  5.0f, -1.0f,    -1.0f,  5.0f, -1.0f,    -1.0f,  5.0f,  1.0f, //上(y = 5.0)
                -1.0f,  5.0f, -1.0f,    -1.0f,  0.0f, -1.0f,    -1.0f,  0.0f,  1.0f,    -1.0f,  5.0f,  1.0f, //左(x = -1.0)
                -1.0f,  0.0f,  1.0f,    -1.0f,  0.0f, -1.0f,     1.0f,  0.0f, -1.0f,     1.0f,  0.0f,  1.0f, //下(y =  0.0)
                -1.0f,  0.0f, -1.0f,     1.0f,  0.0f, -1.0f,     1.0f,  5.0f, -1.0f,    -1.0f,  5.0f, -1.0f, //奥(z = -1.0)
        });

        FloatBuffer colors = Utils.buildFloatBuffer(new float[]{
                1.0f, 0.4f, 0.6f,  1.0f, 0.4f, 0.6f,  1.0f, 0.4f, 0.6f,  1.0f, 0.4f, 0.6f,
                1.0f, 0.4f, 0.6f,  1.0f, 0.4f, 0.6f,  1.0f, 0.4f, 0.6f,  1.0f, 0.4f, 0.6f,
                1.0f, 0.4f, 0.6f,  1.0f, 0.4f, 0.6f,  1.0f, 0.4f, 0.6f,  1.0f, 0.4f, 0.6f,
                1.0f, 0.4f, 0.6f,  1.0f, 0.4f, 0.6f,  1.0f, 0.4f, 0.6f,  1.0f, 0.4f, 0.6f,
                1.0f, 0.4f, 0.6f,  1.0f, 0.4f, 0.6f,  1.0f, 0.4f, 0.6f,  1.0f, 0.4f, 0.6f,
                1.0f, 0.4f, 0.6f,  1.0f, 0.4f, 0.6f,  1.0f, 0.4f, 0.6f,  1.0f, 0.4f, 0.6f,
        });

        FloatBuffer normals = Utils.buildFloatBuffer(new float[]{
                0.0f,  0.0f,  1.0f,     0.0f,  0.0f,  1.0f,     0.0f,  0.0f,  1.0f,     0.0f,  0.0f,  1.0f, //前
                1.0f,  0.0f,  0.0f,     1.0f,  0.0f,  0.0f,     1.0f,  0.0f,  0.0f,     1.0f,  0.0f,  0.0f, //右
                0.0f,  1.0f,  0.0f,     0.0f,  1.0f,  0.0f,     0.0f,  1.0f,  0.0f,     0.0f,  1.0f,  0.0f, //上
                -1.0f,  0.0f,  0.0f,    -1.0f,  0.0f,  0.0f,    -1.0f,  0.0f,  0.0f,    -1.0f,  0.0f,  0.0f, //左
                0.0f, -1.0f,  0.0f,     0.0f, -1.0f,  0.0f,     0.0f, -1.0f,  0.0f,     0.0f, -1.0f,  0.0f, //下
                0.0f,  0.0f, -1.0f,     0.0f,  0.0f, -1.0f,     0.0f,  0.0f, -1.0f,     0.0f,  0.0f, -1.0f, //奥
        });

        ShortBuffer indices = Utils.buildShortBuffer(new short[] {
                0,  1,  2,    0,  2,  3, // 前
                4,  5,  6,    4,  6,  7, // 右
                8,  9, 10,    8, 10, 11, // 上
                12, 13, 14,   12, 14, 15, // 左
                16, 17, 18,   16, 18, 19, // 下
                20, 21, 22,   20, 22, 23, // 奥
        });

        SolidSegment box1 = new SolidSegment("box1", positions, colors, normals, indices, 3, 3);

        float armLength = 5.0f;
        float angle1 = 0.0f;
        float angle2 = 30.0f;

        box1.translate(0.0f, 0.0f, 0.0f);
        box1.rotate(angle1, 0.0f, 1.0f, 0.0f);

        LinkedList<Node> model = new LinkedList<Node>();
        model.add(box1);
        return model;
    }
}
