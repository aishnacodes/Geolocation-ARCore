package com.example.aishnaagrawal.ardemo.activity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.example.aishnaagrawal.ardemo.R;
import com.example.aishnaagrawal.ardemo.api.MarkerApi;
import com.example.aishnaagrawal.ardemo.helper.CameraPermissionHelper;
import com.example.aishnaagrawal.ardemo.model.MarkerInfo;
import com.example.aishnaagrawal.ardemo.renderer.BackgroundRenderer;
import com.example.aishnaagrawal.ardemo.renderer.ObjectRenderer;
import com.example.aishnaagrawal.ardemo.response.MarkerResponse;
import com.google.ar.core.Config;
import com.google.ar.core.Frame;
import com.google.ar.core.Frame.TrackingState;
import com.google.ar.core.Pose;
import com.google.ar.core.Session;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ARActivity extends AppCompatActivity implements GLSurfaceView.Renderer, SensorEventListener, LocationListener {

    private static final String TAG = ARActivity.class.getSimpleName();

    private TextView mTag;
    private TextView mDesc;

    private GLSurfaceView mSurfaceView;
    private Config mDefaultConfig;
    private Session mSession;
    private BackgroundRenderer mBackgroundRenderer = new BackgroundRenderer();
    private GestureDetector mGestureDetector;

    private ObjectRenderer mVirtualObject = new ObjectRenderer();
    private final float[] mAnchorMatrix = new float[16];

    // Tap handling and UI.
    private ArrayBlockingQueue<MotionEvent> mQueuedSingleTaps = new ArrayBlockingQueue<>(16);

    //Location-based stuff
    private SensorManager sensorManager;
    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 0; // 10 meters
    private static final long MIN_TIME_BW_UPDATES = 0;//1000 * 60 * 1; // 1 minute
    public static final int REQUEST_LOCATION_PERMISSIONS_CODE = 0;

    private LocationManager locationManager;
    public Location mLocation;
    private Location destination;
    boolean isGPSEnabled;
    boolean isNetworkEnabled;
    boolean locationServiceAvailable;

    private boolean isCorrectLocation;
    private boolean isTapped = false;

    private static MarkerApi mMarkerApi;
    private String mBaseUrl = "http://139.59.30.117:3000/listings/";
    private Retrofit mRetrofit;
    private List<MarkerInfo> mMarkerList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ar);

        mSurfaceView = (GLSurfaceView) findViewById(R.id.surfaceview);

        mTag = (TextView) findViewById(R.id.textview);
        mDesc = (TextView) findViewById(R.id.textview2);

        mSession = new Session(/*context=*/this);

        sensorManager = (SensorManager) this.getSystemService(SENSOR_SERVICE);

        // Create default config, check is supported, create session from that config.
        mDefaultConfig = Config.createDefaultConfig();
        if (!mSession.isSupported(mDefaultConfig)) {
            Toast.makeText(this, "This device does not support AR", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Set up tap listener.
        mGestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                onSingleTap(e);
                return true;
            }

            @Override
            public boolean onDown(MotionEvent e) {
                return true;
            }
        });

        mSurfaceView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return mGestureDetector.onTouchEvent(event);
            }
        });

        // Set up renderer.
        mSurfaceView.setPreserveEGLContextOnPause(true);
        mSurfaceView.setEGLContextClientVersion(2);
        mSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0); // Alpha used for plane blending.
        mSurfaceView.setRenderer(this);
        mSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

        destination = new Location("Engineering");
        destination.setLatitude(36.969527);
        destination.setLongitude(-122.026749);
        destination.setAltitude(-25.0);

        mRetrofit = new Retrofit.Builder()
                .baseUrl(mBaseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        mMarkerApi = mRetrofit.create(MarkerApi.class);

        new LoadMarkerTask().execute();


    }

    public void setMarkerList(List<MarkerInfo> markerList) {
        this.mMarkerList = markerList;
    }


    private class LoadMarkerTask extends AsyncTask<Void, Integer, List<MarkerInfo>> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected List<MarkerInfo> doInBackground(Void... params) {

            Log.d("background", "here");

            MarkerResponse response;

            try {
                response = runMarker();
                return response.markerList;
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }

        }

        @Override
        protected void onPostExecute(List<MarkerInfo> markerList) {
            super.onPostExecute(markerList);
            setMarkerList(markerList);

        }
    }

    public static MarkerResponse runMarker() throws IOException {

        Call<MarkerResponse> call = mMarkerApi.getMarker();
        MarkerResponse response = call.execute().body();

        return response;
    }

    @Override
    protected void onResume() {
        super.onResume();

        Log.d("resume", "here");
        requestLocationPermission();
        registerSensors();
        requestCameraPermission();
    }


    public void requestLocationPermission() {
        if (this.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            this.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSIONS_CODE);
        } else {
            Log.d("location", "here");
            initLocationService();
        }
    }

    public void requestCameraPermission() {
        if (CameraPermissionHelper.hasCameraPermission(this)) {
            Log.d("camera", "here");
            mSession.resume(mDefaultConfig);
            mSurfaceView.onResume();
        } else {
            CameraPermissionHelper.requestCameraPermission(this);
        }
    }

    private void registerSensors() {
        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR),
                SensorManager.SENSOR_DELAY_FASTEST);
    }

    @Override
    public void onPause() {
        super.onPause();

        mSurfaceView.onPause();
        mSession.pause();
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {

        if (sensorEvent.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {

            float azimuth, pitch, bearing;

            float[] rotationMatrixFromVector = new float[16];
            float[] updatedRotationMatrix = new float[16];
            float[] orientationValues = new float[3];

            boolean isAzimuthInRange = false;
            boolean isPitchInRange = false;

            SensorManager.getRotationMatrixFromVector(rotationMatrixFromVector, sensorEvent.values);

            SensorManager
                    .remapCoordinateSystem(rotationMatrixFromVector,
                            SensorManager.AXIS_X, SensorManager.AXIS_Y,
                            updatedRotationMatrix);

            SensorManager.getOrientation(updatedRotationMatrix, orientationValues);

            bearing = mLocation.bearingTo(destination);

            azimuth = (float) Math.toDegrees(orientationValues[0]);
            pitch = (float) Math.toDegrees(orientationValues[1]);


            if (azimuth > (bearing - 50) && azimuth < (bearing + 50)) {
                isAzimuthInRange = true;
            }

            if (pitch < -45 && pitch > -90) {
                isPitchInRange = true;
            }


            //Log.d("location", mLocation.getAltitude() + "");

            //  Log.d("azimuth", azimuth + " bearing: " + bearing + " ");

            if (isAzimuthInRange && isPitchInRange) {
                //  Log.d("In range", "yes");
                mTag.setText(getString(R.string.location));
                isCorrectLocation = true;

            } else {
                mTag.setText("");
                isCorrectLocation = false;

            }

        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
        //do nothing
    }

    private void initLocationService() {

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        try {
            this.locationManager = (LocationManager) this.getSystemService(this.LOCATION_SERVICE);

            // Get GPS and network status
            this.isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            this.isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

            if (!isNetworkEnabled && !isGPSEnabled) {
                // cannot get location
                this.locationServiceAvailable = false;
            }

            this.locationServiceAvailable = true;

            if (isNetworkEnabled) {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
                        MIN_TIME_BW_UPDATES,
                        MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
                if (locationManager != null) {
                    mLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                }
            }

            if (isGPSEnabled) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                        MIN_TIME_BW_UPDATES,
                        MIN_DISTANCE_CHANGE_FOR_UPDATES, this);

                if (locationManager != null) {
                    mLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                }
            }
        } catch (Exception ex) {
            Log.e(TAG, ex.getMessage());

        }
    }

    @Override
    public void onLocationChanged(Location location) {
        mLocation = location;
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        if (!CameraPermissionHelper.hasCameraPermission(this)) {
            Toast.makeText(this,
                    "Camera permission is needed to run this application", Toast.LENGTH_LONG).show();
            Log.d("permission", "here");
//            finish();
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            // Standard Android full-screen functionality.
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    private void onSingleTap(MotionEvent e) {
        // Queue tap if there is space. Tap is lost if queue is full.
        mQueuedSingleTaps.offer(e);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f);

        // Create the texture and pass it to ARCore session to be filled during update().
        mBackgroundRenderer.createOnGlThread(/*context=*/this);
        mSession.setCameraTextureName(mBackgroundRenderer.getTextureId());

        // Prepare the other rendering objects.
        try {
            mVirtualObject.createOnGlThread(/*context=*/this, "andy.obj", "andy.png");
            mVirtualObject.setMaterialProperties(0.0f, 3.5f, 1.0f, 6.0f);

        } catch (IOException e) {
            Log.e(TAG, "Failed to read obj file");
        }

    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        // Notify ARCore session that the view size changed so that the perspective matrix and
        // the video background can be properly adjusted.
        mSession.setDisplayGeometry(width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        // Clear screen to notify driver it should not load any pixels from previous frame.
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);


        try {
            // Obtain the current frame from ARSession. When the configuration is set to
            // UpdateMode.BLOCKING (it is by default), this will throttle the rendering to the
            // camera framerate.
            Frame frame = mSession.update();

            MotionEvent tap = mQueuedSingleTaps.poll();

            // Draw background.
            mBackgroundRenderer.draw(frame);

            // If not tracking, don't draw 3d objects.
            if (frame.getTrackingState() == TrackingState.NOT_TRACKING) {
                return;
            }

            // Get projection matrix.
            float[] projmtx = new float[16];
            mSession.getProjectionMatrix(projmtx, 0, 0.1f, 100.0f);

            // Get camera matrix and draw.
            float[] viewmtx = new float[16];
            frame.getViewMatrix(viewmtx, 0);

            // Compute lighting from average intensity of the image.
            final float lightIntensity = frame.getLightEstimate().getPixelIntensity();

            float scaleFactor = 1.0f;

            if (!isCorrectLocation) {
                scaleFactor = 0.0f;
            }

//            if (!isAnchorAdded && isCorrectLocation) {
//
//                float[] translation = new float[3];
//                float[] rotation = new float[]{0.00f, 0.00f, 0.00f, 0.99f};
//
//                frame.getPose().getTranslation(translation, 0);
//
//                translation[1]
//                translation[2] += -0.8f;
//
//                Pose pose = new Pose(translation, rotation);
//                pose.toMatrix(mAnchorMatrix, 0);
//
//                mSession.addAnchor(pose);
//
//                Log.d("Anchor added", pose.toString());
//
//                isAnchorAdded = true;
//            }
//
//            if (isAnchorAdded) {
//
//                anchor = mSession.getAllAnchors().iterator().next();
//                Log.d("Anchor", anchor.getPose().toString());
//
//                anchor.getPose().toMatrix(mAnchorMatrix, 0);
//
//                mVirtualObject.updateModelMatrix(mAnchorMatrix, scaleFactor);
//                mVirtualObject.draw(viewmtx, projmtx, lightIntensity);
//
//            }

            if (isCorrectLocation) {

                float[] translation, rotation;
                Pose pose;

                translation = new float[]{-0.1f, 0.05f, -0.8f};
                rotation = new float[]{0.00f, 0.00f, 0.00f, 0.99f};

                frame.getPose().getTranslation(translation, 0);

                //Log.d("frame pose", translation[0] + " " + translation[1] + " " + translation[2] + "");
                //  frame.getPose().getRotationQuaternion(rotation, 0);

                translation[2] = -0.8f;

                pose = new Pose(translation, rotation);
                pose.toMatrix(mAnchorMatrix, 0);

                mVirtualObject.updateModelMatrix(mAnchorMatrix, scaleFactor);
                mVirtualObject.draw(viewmtx, projmtx, lightIntensity);

                if (tap != null) {
                    if (!isTapped) {
                        mDesc.setText("Welcome to Jack Baskin School of Engineering");
                        isTapped = true;
                    } else {
                        mDesc.setText("");
                        isTapped = false;
                    }
                }

            }


        } catch (Throwable t) {
            // Avoid crashing the application due to unhandled exceptions.
            Log.e(TAG, "Exception on the OpenGL thread", t);
        }
    }

}