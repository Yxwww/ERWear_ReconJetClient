package com.ase_lab.erwear_reconclient;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.widget.TextView;

import com.reconinstruments.os.HUDOS;
import com.reconinstruments.os.metrics.HUDMetricsID;
import com.reconinstruments.os.metrics.HUDMetricsManager;
import com.reconinstruments.os.metrics.MetricsValueChangedListener;
import android.app.Notification;
import android.app.NotificationManager;

import io.socket.*;
import org.json.*;

import java.net.MalformedURLException;

public class MainActivity extends Activity implements MetricsValueChangedListener
{
    HUDMetricsManager mHUDMetricsManager;
    NotificationManager mNotificationManager;

    TextView altitudeTextView;
    TextView speedVrtTextView;

    float altitudePressure = 0;
    float speedVertical    = 0;

    float startPressure = Float.MIN_VALUE;


    SocketIO socket;
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        altitudeTextView = (TextView) findViewById(R.id.altitudeTextView);

        speedVrtTextView = (TextView) findViewById(R.id.speedTextView);
        speedVrtTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 52);

        mHUDMetricsManager   = (HUDMetricsManager) HUDOS.getHUDService(HUDOS.HUD_METRICS_SERVICE);
        mNotificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
    }

    @Override
    public void onResume()
    {
        super.onResume();
        mHUDMetricsManager.registerMetricsListener(this, HUDMetricsID.ALTITUDE_PRESSURE);
        mHUDMetricsManager.registerMetricsListener(this, HUDMetricsID.SPEED_VERTICAL);

        try{
            socket = new SocketIO("http://127.0.0.1:3000/");
        }catch(MalformedURLException e){
            System.out.println(e.toString());
            Log.e("ERWear Exception","Malformed URL Error");
        }

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
        socket.send("Hello Server!");
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
            if(startPressure != Float.MIN_VALUE){ CreateNotification(); }
            startPressure = altitudePressure;
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