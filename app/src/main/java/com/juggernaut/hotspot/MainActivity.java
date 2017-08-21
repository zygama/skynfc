package com.juggernaut.hotspot;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.design.widget.TextInputLayout;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;


public class MainActivity extends Activity implements View.OnClickListener
{

    public Button enableHotspot;
    public TextView status;
    public Boolean aBoolean;
    public TextInputLayout textInputLayout;
    public List<String> IP_Client = new ArrayList<>();

    private Handler myHandler = new Handler();
    private int delay = 1000;

    //    private Runnable myRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        enableHotspot = (Button) findViewById(R.id.enableHotspot);
        enableHotspot.setOnClickListener(this);

        // status = (TextView) findViewById(R.id.status);

        aBoolean = OnOfHotspot.isApOn(this);

        if (aBoolean) {
            enableHotspot.setText("DISABLE");
            //status.setText("WiFi Hotspot is ON!");
        } else {
            enableHotspot.setText("ENABLE");
            //status.setText("WiFi Hotspot is OFF!");
        }

        //textInputLayout = (TextInputLayout) findViewById(R.id.jugger);

        writePermission();

    }

    public void writePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.System.canWrite(getApplicationContext())) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, 200);
            }
        }
    }

    @Override
    protected void onStart() {
        // Will check if there is a client every seconds

        myHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Button buttonHubSN = (Button) findViewById(R.id.buttonHubSerialNumber);
                Button buttonAssociateCtsHub = (Button) findViewById(R.id.buttonHubCtsConfig);

                EditText CTS_Number_Text = (EditText) findViewById(R.id.CTS_Number);
                String CTS_Number = CTS_Number_Text.getText().toString();

                EditText SN_Hub_Text = (EditText) findViewById(R.id.SN_Hub);
                String SN_Hub = SN_Hub_Text.getText().toString();

                TextView numberOfDevicesConnected = (TextView) findViewById(R.id.devicesConnected);

                if (OnOfHotspot.isApOn(getApplicationContext())) {
                    // check if Hotspot is enabled before checking for IP clients
                    getClientList();
                    if (IP_Client.size() == 1) {
                        numberOfDevicesConnected.setText("Number of devices connected : 1");
                        
                        if (SN_Hub.length() >= 1){
                            buttonHubSN.setEnabled(true);
                        }
                        else{
                            buttonHubSN.setEnabled(false);
                        }

                        if (CTS_Number.length() >= 1){
                            buttonAssociateCtsHub.setEnabled(true);
                        }
                        else{
                            buttonAssociateCtsHub.setEnabled(false);
                        }
                    } else if (IP_Client.size() >= 2){
                        numberOfDevicesConnected.setText("TOO MANY DEVICES CONNECTED");
                        numberOfDevicesConnected.setTextColor(Color.RED);
                    }
                } else {
                    buttonHubSN.setEnabled(false);
                    buttonAssociateCtsHub.setEnabled(false);
                    numberOfDevicesConnected.setText("Number of devices connected : 0");
                }
                myHandler.postDelayed(this, delay);
            }
        }, delay);

        super.onStart();
    }

    @Override
    public void onClick(View v) {
        Button toggle = (Button) v;
        String buttonText = toggle.getText().toString();
        if (buttonText.equals("DISABLE")) {

            try {
                OnOfHotspot.getApConfiguration(this);
                OnOfHotspot.configApState(this, false);
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            toggle.setText("ENABLE");

        } else if (buttonText.equals("ENABLE")) {
            try {
                OnOfHotspot.hotspotName = "skylightshub";  // name of the default hotspot
                OnOfHotspot.password = "skylights12";  //password of the default hotspot
                OnOfHotspot.getApConfiguration(this);
                OnOfHotspot.configApState(this, true);

                toggle.setText("DISABLE");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void setHubSerialNumber(View v)
    {
        String SN_Value;

        TextView SN_Hub_Text = (TextView) findViewById(R.id.SN_Hub);
        SN_Value = SN_Hub_Text.getText().toString();

        WebView myWebview = (WebView) findViewById(R.id.myWebview);
        //myWebview.setWebViewClient(new WebViewClient());
        if (SN_Value.length() == 1) {
            myWebview.loadUrl("http://" + IP_Client.get(0) + "/SETSN?sn=SKY-HUB-TC-000" + SN_Value);
        } else if (SN_Value.length() == 2) {
            myWebview.loadUrl("http://" + IP_Client.get(0) + "/SETSN?sn=SKY-HUB-TC-00" + SN_Value);
        }
    }

    public void setAssociationHubAndCts(View v)
    {
        String CTS_Number;

        TextView CTS_Number_Text = (TextView) findViewById(R.id.CTS_Number);
        CTS_Number = CTS_Number_Text.getText().toString();

        WebView myWebview = (WebView) findViewById(R.id.myWebview);
        // myWebview.setWebViewClient(new WebViewClient());
        myWebview.loadUrl("http://" + IP_Client.get(0) + "/CONFIG?ssid=skylightscts" + CTS_Number + "&pwd=" +
                "XFncc7wvyXzuME7x2QbeMCbyA4rXt7R7ySmzQDdNy3cswqD7y3h2kPMXp8YN4vz");
    }


    public void getClientList()
    {
    //  int ipCount = 0;
        BufferedReader br;
        try {
            br = new BufferedReader(new FileReader("/proc/net/arp"));
            String line;
            while ((line = br.readLine()) != null) {
                String[] splitted = line.split(" +");
                if (splitted != null) {
                    // Basic sanity check
                    String ip = splitted[0];

                    // check if ip contains numbers (yes, this char sequence is ununderstable...)
                    if (ip.matches(".*\\d.*") && !IP_Client.contains(ip)) {
                        IP_Client.add(ip);
                    }
                }
            }
        } catch (Exception e) {
            Log.e("Error read file", " Error reading /proc/net/arp");
        }
    }

    public void test(View v)
    {
        Log.i("TTTEST", "ONCLICKEEEUH");
    }

//================================================================================================//
}