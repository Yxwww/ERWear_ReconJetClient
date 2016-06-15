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
import com.google.android.gms.maps.model.MarkerOptions;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.view.KeyEvent;
import android.provider.Settings;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.view.animation.AnimationUtils;

import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.util.Log;
import io.socket.*;
import android.net.Uri;
import org.json.*;
import java.net.MalformedURLException;
import java.net.URI;


public class MapActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private GoogleApiClient mGoogleApiClient;
    NotificationManager mNotificationManager;
    AudioManager audioManager = null;
    String TAG = "ERWear";
    //Load animation
    Animation slide_down = null;
    Animation slide_up = null;
    Animation fade_in = null;
    Animation fade_out = null;
    LinearLayout ERWearConsoleLinearLayout;
    SocketIO socket;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.googlemap_layout);
        /*SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);*/
        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        audioManager = (AudioManager) getSystemService(getApplicationContext().AUDIO_SERVICE);

        slide_down = AnimationUtils.loadAnimation(getApplicationContext(),
                R.anim.slide_down);
        slide_up = AnimationUtils.loadAnimation(getApplicationContext(),
                R.anim.slide_up);
        fade_in = AnimationUtils.loadAnimation(getApplicationContext(),
                R.anim.fade_in);
        fade_out = AnimationUtils.loadAnimation(getApplicationContext(),
                R.anim.fade_out);

    }

    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        Log.v(TAG, "onKeyDown keyCode: " + keyCode);
        switch(keyCode)
        {
            case KeyEvent.KEYCODE_DPAD_LEFT:
                Log.d(TAG, "interactive(centre)");
                PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, new Intent(Settings.ACTION_SETTINGS), 0);
                Notification notification = new Notification.Builder(getApplicationContext())
                        .setContentTitle("interactive title " + "centre")
                        .setSmallIcon(R.drawable.ic_launcher_share)
                        .setContentText("interactive text " + "centre")
                        .setContentIntent(pendingIntent)
                        .build();
                mNotificationManager.notify(0, notification);
                return true;
            case KeyEvent.KEYCODE_DPAD_CENTER:
                // Somehow disabled in Google Map

                return true;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                ERWearConsoleLinearLayout.startAnimation(fade_in);
                return true;
            case KeyEvent.KEYCODE_DPAD_UP:
                ERWearConsoleLinearLayout.startAnimation(fade_out);
                return true;
        }
        return super.onKeyDown(keyCode, event);
    }
    @Override
    protected void onResume() {
        super.onResume();
        setContentView(R.layout.googlemap_layout);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        ERWearConsoleLinearLayout = (LinearLayout) findViewById(R.id.ERWearConsole);

        try{
            socket = new SocketIO("http://192.168.1.73:3000/");
        }catch(MalformedURLException e){
            System.out.println(e.toString());
            Log.e("ERWear Exception","Malformed URL Error");
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
            }
        });

        // This line is cached until the connection is establisched.
        socket.send("Hello Server!");
    }



    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;


        LatLng current_location = new LatLng(51.071099, -114.128332);
        Log.d("ERWear","!!"+current_location.toString());
        mMap.addMarker(new MarkerOptions().position(current_location).title("You"));
        //mMap.moveCamera(CameraUpdateFactory.newLatLng(current_location));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(current_location, 15));


        // Add a marker in Sydney, Australia, and move the camera.
        /*Log.d("ERWear","MapReady");
        LatLng sydney = new LatLng(-34, 151);
        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(sydney, 15));*/
        Log.d("ERWear","MapReady");
        // Acquire a reference to the system Location Manager
        /*LocationManager locationManager = (LocationManager) this.getSystemService(getApplicationContext().LOCATION_SERVICE);

// Define a listener that responds to location updates
        LocationListener locationListener = new LocationListener() {
            public void onLocationChanged(Location location) {
                // Called when a new location is found by the gps location provider.
                //makeUseOfNewLocation(location);
                //System.out.println("!!" + location.toString());
                LatLng current_location = new LatLng(location.getLatitude(), location.getLongitude());
                Log.d("ERWear","!!"+current_location.toString());
                //mMap.addMarker(new MarkerOptions().position(current_location).title("You"));
                //mMap.moveCamera(CameraUpdateFactory.newLatLng(current_location));
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(current_location, 20));
            }

            public void onStatusChanged(String provider, int status, Bundle extras) {}

            public void onProviderEnabled(String provider) {}

            public void onProviderDisabled(String provider) {}
        };

// Register the listener with the Location Manager to receive location updates
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);*/
    }
}