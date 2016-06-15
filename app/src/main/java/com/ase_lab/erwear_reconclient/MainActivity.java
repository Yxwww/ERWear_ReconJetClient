package com.ase_lab.erwear_reconclient;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.widget.TextView;
import android.content.Intent;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;

import com.reconinstruments.os.HUDOS;
import com.reconinstruments.os.metrics.HUDMetricsID;
import com.reconinstruments.os.metrics.HUDMetricsManager;
import com.reconinstruments.os.metrics.MetricsValueChangedListener;
import com.reconinstruments.ui.list.SimpleListActivity;
import android.view.KeyEvent;
import android.widget.TextView;
import android.media.AudioManager;

import android.app.Notification;
import android.app.NotificationManager;


import io.socket.*;
import org.json.*;

import java.net.MalformedURLException;
import java.net.URI;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
//import com.google.android.gms.common.api.GoogleApiClient.;

public class MainActivity extends SimpleListActivity implements MetricsValueChangedListener
{
    HUDMetricsManager mHUDMetricsManager;
    NotificationManager mNotificationManager;

    TextView altitudeTextView;
    TextView speedVrtTextView;
    TextView nameTextView;

    float altitudePressure = 0;
    float speedVertical    = 0;

    float startPressure = Float.MIN_VALUE;

    private GoogleApiClient mGoogleApiClient;

    SocketIO socket = null;
    AudioManager audioManager = null;
    public String TAG = "ERWear";
    public String ResponderName = "John-117";
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        Log.v(TAG, "onKeyDown keyCode: " + keyCode);
        switch(keyCode)
        {
            case KeyEvent.KEYCODE_DPAD_LEFT:

                return true;
            case KeyEvent.KEYCODE_DPAD_CENTER:
                //doStuff("SELECT", AudioManager.FX_KEYPRESS_RETURN);
                audioManager.playSoundEffect(AudioManager.FX_KEYPRESS_RETURN, 1);
                Intent mapIntent = new Intent(getApplicationContext(), MapActivity.class);
                startActivity(mapIntent);

                return true;
        }
        return super.onKeyDown(keyCode, event);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        altitudeTextView = (TextView) findViewById(R.id.altitudeTextView);

        speedVrtTextView = (TextView) findViewById(R.id.speedTextView);
        nameTextView = (TextView) findViewById(R.id.nameTextView);
        nameTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
        //altitudeTextView.getLayout.setMargins(50, 0, 0, 0);
        //speedVrtTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 52);

        mHUDMetricsManager   = (HUDMetricsManager) HUDOS.getHUDService(HUDOS.HUD_METRICS_SERVICE);
        mNotificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);

        audioManager = (AudioManager) getSystemService(getApplicationContext().AUDIO_SERVICE);
    }

    @Override
    public void onResume()
    {
        super.onResume();
        mHUDMetricsManager.registerMetricsListener(this, HUDMetricsID.ALTITUDE_PRESSURE);
        mHUDMetricsManager.registerMetricsListener(this, HUDMetricsID.SPEED_VERTICAL);


        // UI Init
        nameTextView.setText(String.format("John-117"));

        /*Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("google.navigation:q=51.080018013294136,-114.12513971328735"));
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);*/

        //TODO: Correctly follow the google map API signin footsetup https://developers.google.com/places/android-api/signup#release-cert https://developer.android.com/studio/publish/app-signing.html




        try{
            socket = new SocketIO("http://192.168.1.73:3000/");
        }catch(MalformedURLException e){
            System.out.println(e.toString());
            Log.e("ERWear Exception","Malformed URL Error");
        }


        // SOD Events
        Log.d("ERWear","!!"+socket.toString());
        socket.connect(new IOCallback() {
            @Override
            public void onMessage(JSONObject json, IOAcknowledge ack) {
                try {
                    System.out.println("Server said:" + json.toString(2));
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
        //socket.send("Hello Server!");
    }

    @Override
    public void onPause()
    {
        super.onPause();
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
                altitudeTextView.setText(String.format("%.1f", value) + " m");
                break;
            case HUDMetricsID.SPEED_VERTICAL :
                speedVertical = value;
                speedVrtTextView.setText(String.format("%.1f", value) + " km/h");
                break;
        }

        if(Math.abs(startPressure - altitudePressure) > 2)
        {
            //if(startPressure != Float.MIN_VALUE){ CreateNotification(); }
            //startPressure = altitudePressure;
        }
    }

    private void CreateNotification()
    {
        Notification notification = new Notification.Builder(getApplicationContext())
                .setContentTitle("Notification")
                .setSmallIcon(R.drawable.icon_checkmark)
                .setContentText("Altitude Changed (+/- 2 Meters)!")
                .build();
        mNotificationManager.notify(0, notification);
    }
}



/*import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

public class MainActivity extends AppCompatActivity {

    /@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
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
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
*/