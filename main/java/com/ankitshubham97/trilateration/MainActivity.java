package com.ankitshubham97.trilateration;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {
    Button getInfoButton,captureRssiButton,dispalyRssiButton,calculatePositionButton,calculateCustomPositionButton;
    WifiManager wifiManager;
    WifiInfo wifiInfo;
    TextView infoText;
    EditText dEditText,iEditText,jEditText,countEditText;

    HashMap<String, Double> rssiMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getInfoButton=(Button)findViewById(R.id.getInfoButton);
        captureRssiButton=(Button)findViewById(R.id.captureRssiButton);
        dispalyRssiButton=(Button)findViewById(R.id.dispalyRssiButton);
        calculatePositionButton=(Button)findViewById(R.id.calculatePositionButton);
        calculateCustomPositionButton=(Button)findViewById(R.id.calculateCustomPositionButton);
        infoText = (TextView)findViewById(R.id.infoText);
        dEditText=(EditText)findViewById(R.id.dEditText);
        iEditText=(EditText)findViewById(R.id.iEditText);
        jEditText=(EditText)findViewById(R.id.jEditText);
        countEditText=(EditText)findViewById(R.id.countEditText);
        rssiMap = new HashMap();
        rssiMap.put("dummy",-49.0);


        getInfoButton.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                getInfo();
            }
        });
        captureRssiButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                captureRssi();
            }
        });
        dispalyRssiButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dispalyRssi();
            }
        });
        calculatePositionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                calculatePosition(Double.parseDouble(dEditText.getText().toString()),Double.parseDouble(iEditText.getText().toString()),Double.parseDouble(jEditText.getText().toString()));
            }
        });
        calculateCustomPositionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                calculatePosition(Double.parseDouble(dEditText.getText().toString().split(",")[0]),
                        Double.parseDouble(iEditText.getText().toString().split(",")[0]),
                        Double.parseDouble(jEditText.getText().toString().split(",")[0]),

                        Double.parseDouble(dEditText.getText().toString().split(",")[1]),
                        Double.parseDouble(iEditText.getText().toString().split(",")[1]),
                        Double.parseDouble(jEditText.getText().toString().split(",")[1]));
            }
        });

    }

    private final static int DELAY = 3000;
    private final Handler handler = new Handler();
    private  Timer timer;
    private  TimerTask task ;


    protected void calculatePosition(double d, double i, double j){
        Iterator it = rssiMap.entrySet().iterator();
        double[] radii = new double[3];
        int c=0;
        while (c++<3) {
            HashMap.Entry pair = (HashMap.Entry)it.next();
            radii[c-1]=getDistance((double)pair.getValue());
        }
        double res[] = new double[2];
        res[0]=(Math.pow(radii[0],2)-Math.pow(radii[1],2)+Math.pow(d,2))/(2*d);
        res[1]=(Math.pow(radii[0],2)-Math.pow(radii[2],2)+Math.pow(i,2)+Math.pow(j,2)-(2*i*res[0]))/(2*j);
        res[0]=(radii[0]*radii[0]-(radii[1]*radii[1])+(d*d))/(2*d);
        res[1]=((radii[0]*radii[0])-(radii[2]*radii[2])+(i*i)+(j*j)-(2*i*res[0]))/(2*j);
        infoText.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        infoText.setText("Coordinates:("+res[0]+","+res[1]+")");
    }


    protected void calculatePosition(double d, double i, double j, double r1, double r2, double r3){
        double[] radii = new double[3];
        radii[0]=r1;
        radii[1]=r2;
        radii[2]=r3;
        double res[] = new double[2];
        res[0]=(Math.pow(radii[0],2)-Math.pow(radii[1],2)+Math.pow(d,2))/(2*d);
        res[1]=(Math.pow(radii[0],2)-Math.pow(radii[2],2)+Math.pow(i,2)+Math.pow(j,2)-(2*i*res[0]))/(2*j);
        infoText.setText("Coordinates of my location: ("+res[0]+","+res[1]+")");
    }

    protected void viewAllDistance(){
        Iterator it = rssiMap.entrySet().iterator();
        String print = "";
        while (it.hasNext()) {
            HashMap.Entry pair = (HashMap.Entry)it.next();
            System.out.println(pair.getKey() + " = " + pair.getValue());
            print = print+pair.getKey()+":"+getDistance((double)pair.getValue())+"\n";
        }
        infoText.setText(print);


    }

    protected void captureRssi(){

        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        wifiInfo= wifiManager.getConnectionInfo();
        String infogetIpAddress =  getIpAsString(wifiInfo.getIpAddress());
        final int limit = Integer.parseInt(countEditText.getText().toString());


        timer = new Timer();
        task = new TimerTask() {
            private int counter = 0;
            public void run() {
                handler.post(new Runnable() {
                    public void run() {
                        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                        wifiInfo = wifiManager.getConnectionInfo();
                        String infogetIpAddress = getIpAsString(wifiInfo.getIpAddress());
                        int infogetRssi = wifiInfo.getRssi();
                        if (rssiMap.containsKey( infogetIpAddress)) {
                            double rssisum = rssiMap.get( infogetIpAddress);
                            rssisum += infogetRssi;
                            rssiMap.put(infogetIpAddress, rssisum);
                        } else {
                            rssiMap.put( infogetIpAddress, (double) infogetRssi);
                        }
                        if(counter==limit){
                            double rssisum =  rssiMap.get( infogetIpAddress);
                            rssisum /= counter;
                            rssiMap.put( infogetIpAddress, rssisum);
                            Toast.makeText(MainActivity.this, "test ended;"+"averaged rssi:"+rssiMap.get(infogetIpAddress), Toast.LENGTH_SHORT).show();
                        }
                        else{
                            Toast.makeText(MainActivity.this, "test#" + counter + "rssi:" + infogetRssi+(double) rssiMap.get((String) infogetIpAddress), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
                if (++counter == limit) {
                    timer.cancel();
                    timer.purge();
                }
            }
        };
        timer.schedule(task, DELAY, DELAY);
    }
    protected void getInfo(){

        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        wifiInfo= wifiManager.getConnectionInfo();
        String infogetSSID = wifiInfo.getSSID();
        String infogetFrequency = wifiInfo.getFrequency()+"";

        String infogetIpAddress =  getIpAsString(wifiInfo.getIpAddress());
        String infogetLinkSpeed = wifiInfo.getLinkSpeed()+"";
        String infogetNetworkId = wifiInfo.getNetworkId()+"";
        String infogetRssi = wifiInfo.getRssi()+"";
        infoText.setText(
                "infogetBSSID:"+infogetNetworkId+
                        "\ninfogetSSID:"+infogetSSID+
                        "\ninfogetFrequency:"+infogetFrequency+
                        "\ninfogetIpAddress:"+infogetIpAddress+
                        "\ninfogetLinkSpeed:"+infogetLinkSpeed+
                        "\ninfogetNetworkId:"+infogetNetworkId+
                        "\ninfogetRssi:"+infogetRssi);

    }
    protected void dispalyRssi(){
        Iterator it = rssiMap.entrySet().iterator();
        String print = "";
        while (it.hasNext()) {
            HashMap.Entry pair = (HashMap.Entry)it.next();
            print = print+pair.toString()+"\n";
        }
        infoText.setText(print);
    }

    protected String getIpAsString(int ipAddress){
        //source: https://stackoverflow.com/questions/16730711/get-my-wifi-ip-address-android
        if (ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN)) {
            ipAddress = Integer.reverseBytes(ipAddress);
        }

        byte[] ipByteArray = BigInteger.valueOf(ipAddress).toByteArray();

        String ipAddressString;
        try {
            ipAddressString = InetAddress.getByAddress(ipByteArray).getHostAddress();
        } catch (UnknownHostException ex) {
            ipAddressString = null;
        }
        return ipAddressString;
    }
    protected double getDistance(double rssi){
        return Math.pow(10,-(rssi+2.425)/23.28);
    }
}
