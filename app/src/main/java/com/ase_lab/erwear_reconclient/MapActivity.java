package com.ase_lab.erwear_reconclient;

/**
 * Created by Yuxibro on 16-06-06.
 */
import android.media.AudioManager;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

import com.google.android.gms.common.api.GoogleApiActivity;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.provider.Settings;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.view.animation.AnimationUtils;
import android.view.animation.Animation.AnimationListener;

import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.util.Log;
import io.socket.*;
import android.net.Uri;
import org.json.*;
import java.net.MalformedURLException;
import java.net.URI;

import com.reconinstruments.os.HUDOS;
import com.reconinstruments.os.metrics.HUDMetricsID;
import com.reconinstruments.os.metrics.HUDMetricsManager;
import com.reconinstruments.os.metrics.MetricsValueChangedListener;
import com.reconinstruments.ui.list.SimpleListActivity;


public class MapActivity extends FragmentActivity implements OnMapReadyCallback,MetricsValueChangedListener {

    private GoogleMap mMap;
    private GoogleApiClient mGoogleApiClient;
    AudioManager audioManager = null;
    String TAG = "ERWear_MapView";
    //Load animation
    Animation slide_down = null;
    Animation slide_up = null;
    Animation fade_in = null;
    Animation fade_out = null;
    LinearLayout ERWearConsoleLinearLayout;
    SocketIO socket;


    HUDMetricsManager mHUDMetricsManager;
    NotificationManager mNotificationManager;

    TextView altitudeGMapTextView;
    TextView speedGMapVrtTextView;
    TextView nameTextView;
    float altitudePressure = 0;
    float speedVertical    = 0;
    float startPressure = Float.MIN_VALUE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.googlemap_layout);
        setContentView(R.layout.googlemap_layout);
        /*SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);*/
        audioManager = (AudioManager) getSystemService(getApplicationContext().AUDIO_SERVICE);



        // UI Animation
        slide_down = AnimationUtils.loadAnimation(getApplicationContext(),
                R.anim.slide_down);
        slide_up = AnimationUtils.loadAnimation(getApplicationContext(),
                R.anim.slide_up);
        fade_in = AnimationUtils.loadAnimation(getApplicationContext(),
                R.anim.fade_in);
        fade_out = AnimationUtils.loadAnimation(getApplicationContext(),
                R.anim.fade_out);

        fade_in.setAnimationListener(new AnimationListener() {
            @Override
            public void onAnimationStart(Animation arg0) {
                //Functionality here
                ERWearConsoleLinearLayout.setVisibility(View.VISIBLE);
            }
            @Override
            public void onAnimationEnd(Animation arg0) {
                //Functionality here
                ERWearConsoleLinearLayout.setAlpha(1.0f);

            }
            @Override
            public void onAnimationRepeat(Animation arg0) {
                //Functionality here
            }
        });

        fade_out.setAnimationListener(new AnimationListener() {
            @Override
            public void onAnimationStart(Animation arg0) {
                //Functionality here
            }
            @Override
            public void onAnimationEnd(Animation arg0) {
                //Functionality here
                ERWearConsoleLinearLayout.setAlpha(0.1f);
                ERWearConsoleLinearLayout.setVisibility(View.INVISIBLE);
            }
            @Override
            public void onAnimationRepeat(Animation arg0) {
                //Functionality here
            }
        });

        mHUDMetricsManager   = (HUDMetricsManager) HUDOS.getHUDService(HUDOS.HUD_METRICS_SERVICE);
        mNotificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);

        // UI
        altitudeGMapTextView= (TextView) findViewById(R.id.altitudeTextViewGMap);
        speedGMapVrtTextView = (TextView) findViewById(R.id.speedTextViewGMap);
        nameTextView = (TextView) findViewById(R.id.nameGMapTextView);
        nameTextView.setText("John-117");
        //nameTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
    }


    @Override
    protected void onResume() {
        super.onResume();


        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        ERWearConsoleLinearLayout = (LinearLayout) findViewById(R.id.ERWearConsole);
        mHUDMetricsManager.registerMetricsListener(this, HUDMetricsID.ALTITUDE_PRESSURE);
        mHUDMetricsManager.registerMetricsListener(this, HUDMetricsID.SPEED_VERTICAL);

        try{
            socket = new SocketIO("http://192.168.1.73:3000/");
        }catch(MalformedURLException e){
            System.out.println(e.toString());
            Log.e(TAG+" Exception","Malformed URL Error");
        }
        // Socket IO Setup
        socket.connect(new IOCallback() {
            @Override
            public void onMessage(JSONObject json, IOAcknowledge ack) {
                try {
                    System.out.println("Server said:" + json.toString(2));
                    //TODO: Add handler this.socket.Emit("updatePOIRequest", new { id = id, description = description, location = new { lat = location.Item1, lng = location.Item2 },eventType = eventType }, null, null);


                } catch (JSONException e) {
                    e.printStackTrace();
                    System.out.println("OnMessage Json Exception: " + e.toString());
                }
            }

            @Override
            public void onMessage(String data, IOAcknowledge ack) {
                System.out.println("Server said: " + data);
            }

            @Override
            public void onError(SocketIOException socketIOException) {
                System.out.println("an Error occured");
                socketIOException.printStackTrace();
            }

            @Override
            public void onDisconnect() {
                System.out.println("Connection terminated.");
            }

            @Override
            public void onConnect() {
                System.out.println("Connection established");
            }

            @Override
            public void on(String event, IOAcknowledge ack, Object... args) {
                System.out.println("Server triggered event '" + event + "'");
                try {
                    switch (event) {
                        case "POIUpdate":
                            System.out.println("POI Update YO! '" + event + "'");
                            JSONObject data = (JSONObject) args[0];
                            System.out.println(data.getString("description"));
                            Notification notification = new Notification.Builder(getApplicationContext())
                                    .setContentTitle("POI Update")
                                    .setSmallIcon(R.drawable.icon_checkmark)
                                    .setContentText(data.getString("description"))
                                    .build();
                            mNotificationManager.notify(0, notification);
                            break;
                        case "remoteNavigation":
                            System.out.println(event+" ");
                            JSONObject navData = (JSONObject) args[0];
                            //startActivity(intent);
                            String lat = navData.getJSONObject("payload").getString("lat");
                            String lng = navData.getJSONObject("payload").getString("lng");
                            Intent NavIntent = new Intent(android.content.Intent.ACTION_VIEW,
                                    Uri.parse("google.navigation:q="+lat+","+lng+""));
                            PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, NavIntent , 0);
                            Notification remoteNavNotification = new Notification.Builder(getApplicationContext())
                                    .setContentTitle("Remote Navigation Request")
                                    .setSmallIcon(R.drawable.ic_launcher_share)
                                    .setContentText("Destination: "+lat+","+lng)
                                    .setContentIntent(pendingIntent)
                                    .build();
                            mNotificationManager.notify(0, remoteNavNotification);
                            break;
                    }
                } catch (JSONException e) {
                    Log.e(TAG, e.toString());
                }

            }
        });

        // This line is cached until the connection is establisched.
        socket.send("Hello Server!");
    }

    //MarkerOptions userMarker = new MarkerOptions().position(new LatLng(51.048093, -114.071201)).title("User");
    Marker userMarker = null;
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        userMarker = mMap.addMarker(new MarkerOptions().position(new LatLng(51.048093, -114.071201))
                .title(""));
        Log.d(TAG, "MapReady");
        // Acquire a reference to the system Location Manager
        LocationManager locationManager = (LocationManager) this.getSystemService(getApplicationContext().LOCATION_SERVICE);

        // Define a listener that responds to location updates
        final SocketIO SoDSocket = this.socket;
        LocationListener locationListener = new LocationListener() {
            public void onLocationChanged(Location location) {
                LatLng current_location = new LatLng(location.getLatitude(), location.getLongitude());
                Log.d("ERWear", "!!" + current_location.toString());
                userMarker.setPosition(current_location);
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(current_location, 15));
                //
                socket.emit("updateERLocation", current_location.latitude + "," + current_location.longitude);
            }

            public void onStatusChanged(String provider, int status, Bundle extras) {}

            public void onProviderEnabled(String provider) {}

            public void onProviderDisabled(String provider) {}
        };

// Register the listener with the Location Manager to receive location updates
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,3000, 0, locationListener);
    }
    public void onPause() {
        super.onPause();  // Always call the superclass method first

        // Release the Camera because we don't need it when paused
        // and other activities might need to use it.
        if (this.socket != null) {
            if (this.socket.isConnected()) {
                this.socket.disconnect();
            }
        }
        mHUDMetricsManager.unregisterMetricsListener(this, HUDMetricsID.ALTITUDE_PRESSURE);
        mHUDMetricsManager.unregisterMetricsListener(this, HUDMetricsID.SPEED_VERTICAL);
    }
    @Override
    public void onMetricsValueChanged(int metricID, float value, long changeTime, boolean isValid)
    {
        switch(metricID)
        {
            case HUDMetricsID.ALTITUDE_PRESSURE :
                altitudePressure = value;
                altitudeGMapTextView.setText(String.format("%.1f", value) + " m");
                break;
            case HUDMetricsID.SPEED_VERTICAL :
                speedVertical = value;
                speedGMapVrtTextView.setText(String.format("%.1f", value) + " km/h");
                break;
        }

        if(Math.abs(startPressure - altitudePressure) > 2)
        {
            //if(startPressure != Float.MIN_VALUE){ CreateNotification(); }
            //startPressure = altitudePressure;
        }
    }


    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        Log.v(TAG, "onKeyDown keyCode: " + keyCode);
        switch(keyCode)
        {
            case KeyEvent.KEYCODE_DPAD_LEFT:
                Log.d(TAG, "Left");
                Intent CameraIntent = new Intent(getApplicationContext(), CameraActivity.class);
                startActivity(CameraIntent);
                return true;
            case KeyEvent.KEYCODE_DPAD_CENTER:
                // Somehow disabled in Google Map

                return true;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                hideERWearDashBoard();
                return true;
            case KeyEvent.KEYCODE_DPAD_UP:
                showERWearDashBoard();
                return true;
            case KeyEvent.KEYCODE_DPAD_RIGHT:

                return true;
        }
        return super.onKeyDown(keyCode, event);
    }
    public void hideERWearDashBoard(){
        Log.d(TAG, "DPAD DOWN" + (ERWearConsoleLinearLayout.getAlpha()));
        ERWearConsoleLinearLayout.setVisibility(View.VISIBLE);
        if(ERWearConsoleLinearLayout.getAlpha()==0.1f){
            ERWearConsoleLinearLayout.startAnimation(fade_in);
            fade_in.setFillAfter(true);
            ERWearConsoleLinearLayout.setAlpha(1.0f);
        }
    }
    public void showERWearDashBoard(){
        Log.d(TAG, "DPAD UP" + ERWearConsoleLinearLayout.getAlpha());
        ERWearConsoleLinearLayout.setVisibility(View.VISIBLE);
        if(ERWearConsoleLinearLayout.getAlpha()==1.0f){
            ERWearConsoleLinearLayout.startAnimation(fade_out);
            fade_out.setFillAfter(true);
        }
    }
}