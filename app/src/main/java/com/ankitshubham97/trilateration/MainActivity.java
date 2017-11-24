package com.ankitshubham97.trilateration;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {
    Button getInfoButton,captureRssiButton,dispalyRssiButton,calculatePositionButton,calculateCustomPositionButton;
    Button captureLocationButton;
    Button displayLocationButton;
    WifiManager wifiManager;
    WifiInfo wifiInfo;
    TextView infoText;
    EditText dEditText,iEditText,jEditText,countEditText;
    protected LocationManager locationManager;
    AlertDialog.Builder alertDialog;



    HashMap<String, Double> rssiMap;
    HashMap<String, Tuple<Double, Double> > locationMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getInfoButton=(Button)findViewById(R.id.getInfoButton);
        captureLocationButton=(Button)findViewById(R.id.captureLocationButton);
        captureRssiButton=(Button)findViewById(R.id.captureRssiButton);
        displayLocationButton=(Button)findViewById(R.id.displayLocationButton);
        dispalyRssiButton=(Button)findViewById(R.id.dispalyRssiButton);
        calculatePositionButton=(Button)findViewById(R.id.calculatePositionButton);
        calculateCustomPositionButton=(Button)findViewById(R.id.calculateCustomPositionButton);
        infoText = (TextView)findViewById(R.id.infoText);
        dEditText=(EditText)findViewById(R.id.dEditText);
        iEditText=(EditText)findViewById(R.id.iEditText);
        jEditText=(EditText)findViewById(R.id.jEditText);
        countEditText=(EditText)findViewById(R.id.countEditText);
        rssiMap = new HashMap<>();
        locationMap = new HashMap<>();
        rssiMap.put("dummy",-49.0);
        //locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        //locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        alertDialog  = new AlertDialog.Builder(this);


        getInfoButton.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                getInfo();
            }
        });
        captureLocationButton.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                captureLocation();
            }
        });
        displayLocationButton.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                displayLocation();
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
                displayRssi();
            }
        });
        calculatePositionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(locationMap.size()<3) {
                    showDialog("Error", "Number of AP locations are less than 3. Taking manual input...");
                    if(dEditText.getText().toString().split(",").length==3 && iEditText.getText().toString().split(",").length==3 && jEditText.getText().toString().split(",").length==3) {
                        FourValues<Double, Double, Double, Double> data= getTransformedCoords(true);
                        Tuple<Double, Double> myLoc = calculatePosition(data.x,data.y,data.a,
                                Double.parseDouble(dEditText.getText().toString().split(",")[2]),
                                Double.parseDouble(iEditText.getText().toString().split(",")[2]),
                                Double.parseDouble(jEditText.getText().toString().split(",")[2]) );
                        System.out.println("My coordinates: "+myLoc.x+", "+myLoc.y);
                        myLoc = inverseTransformedCoords(myLoc, data.b, data.origin);
                        infoText.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                        infoText.setText("Coordinates:("+myLoc.x+","+myLoc.y+")");
                        System.out.println("My lat, long: "+myLoc.x+", "+myLoc.y);
                    }
                    else {
                        showDialog("Error","Invalid manual input");
                    }
                }
                else {
                    System.out.println("Automated Calculation");
                    FourValues<Double, Double, Double, Double> data= getTransformedCoords(false);
                    Tuple<Double, Double> myLoc = calculatePosition(data.x,data.y,data.a);  //d,i,j
                    System.out.println("My coordinates: "+myLoc.x+", "+myLoc.y);
                    myLoc = inverseTransformedCoords(myLoc, data.b, data.origin);
                    infoText.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                    infoText.setText("Coordinates:("+myLoc.x+","+myLoc.y+")");
                    System.out.println("My lat, long: "+myLoc.x+", "+myLoc.y);
                }
            }
        });
        calculateCustomPositionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Tuple<Double, Double> myLoc = calculatePosition(Double.parseDouble(dEditText.getText().toString().split(",")[0]),
                        Double.parseDouble(iEditText.getText().toString().split(",")[0]),
                        Double.parseDouble(jEditText.getText().toString().split(",")[0]),

                        Double.parseDouble(dEditText.getText().toString().split(",")[1]),
                        Double.parseDouble(iEditText.getText().toString().split(",")[1]),
                        Double.parseDouble(jEditText.getText().toString().split(",")[1]));
                infoText.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                infoText.setText("Coordinates:("+myLoc.x+","+myLoc.y+")");
            }
        });

    }

    private final static int DELAY = 3000;
    private final Handler handler = new Handler();
    private  Timer timer;
    private  TimerTask task ;


    protected Tuple<Double,Double> calculatePosition(double d, double i, double j){
        Iterator it = rssiMap.entrySet().iterator();
        double[] radii = new double[3];
        int c=0;
        while (c++<3) {
            HashMap.Entry pair = (HashMap.Entry)it.next();
            radii[c-1]=getDistance((double)pair.getValue());
        }
        double res[] = new double[2];
//        res[0]=(Math.pow(radii[0],2)-Math.pow(radii[1],2)+Math.pow(d,2))/(2*d);
//        res[1]=(Math.pow(radii[0],2)-Math.pow(radii[2],2)+Math.pow(i,2)+Math.pow(j,2)-(2*i*res[0]))/(2*j);
        res[0]=(radii[0]*radii[0]-(radii[1]*radii[1])+(d*d))/(2*d);
        res[1]=((radii[0]*radii[0])-(radii[2]*radii[2])+(i*i)+(j*j)-(2*i*res[0]))/(2*j);
//        infoText.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
//        infoText.setText("Coordinates:("+res[0]+","+res[1]+")");
        return new Tuple<>(res[0],res[1]);
    }

    protected void captureLocation() {
        if (locationManager != null) {
            try {
                List<String> providers = locationManager.getProviders(true);
                Location lastKnownLocation = null;
                for (String provider : providers) {
                    Location l = locationManager.getLastKnownLocation(provider);
                    if (l == null) {
                        continue;
                    }
                    if (lastKnownLocation == null || l.getAccuracy() < lastKnownLocation.getAccuracy()) {
                        lastKnownLocation = l;
                    }
                }
                if (lastKnownLocation != null) {
                    wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                    if(wifiManager!=null) {
                        wifiInfo = wifiManager.getConnectionInfo();
                    }
                    String infogetIpAddress = getIpAsString(wifiInfo.getIpAddress());
                    showDialog("Found Location",infogetIpAddress+"\n"+lastKnownLocation.getLatitude()+"\n"+lastKnownLocation.getLongitude());
                    locationMap.put(infogetIpAddress, new Tuple<>(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude()));
                } else {
                    showDialog("Error","Cannot capture location. Make sure GPS is on.");
                }
            }
            catch (SecurityException e) {
                showDialog("Error","Permission Denied");
            }
        } else {
            showDialog("Error","Cannot capture location. Make sure GPS is on.");
        }
    }

    protected Tuple<Double,Double> calculatePosition(double d, double i, double j, double r1, double r2, double r3){
        double[] radii = new double[3];
        radii[0]=r1;
        radii[1]=r2;
        radii[2]=r3;
        double res[] = new double[2];
        res[0]=(Math.pow(radii[0],2)-Math.pow(radii[1],2)+Math.pow(d,2))/(2*d);
        res[1]=(Math.pow(radii[0],2)-Math.pow(radii[2],2)+Math.pow(i,2)+Math.pow(j,2)-(2*i*res[0]))/(2*j);
//        infoText.setText("Coordinates of my location: ("+res[0]+","+res[1]+")");
        return new Tuple<>(res[0],res[1]);
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
    protected void displayRssi(){
        Iterator it = rssiMap.entrySet().iterator();
        String print = "";
        while (it.hasNext()) {
            HashMap.Entry pair = (HashMap.Entry)it.next();
            print = print+pair.toString()+"\n";
        }
        infoText.setText(print);
    }
    protected void displayLocation(){
        Iterator it = locationMap.entrySet().iterator();
        String print = "";
        while (it.hasNext()) {
            HashMap.Entry pair = (HashMap.Entry)it.next();
            print = print+pair.toString()+"\n";
        }
        showDialog("AP Locations", print);
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
        return Math.pow(10,-(rssi+2.425)/23.28)/100000/100;
    }

    protected FourValues<Double, Double, Double, Double> getTransformedCoords(boolean custom) {

        Tuple<Double, Double> x_axis, origin, i_j;

        if(!custom) {
            Iterator it = locationMap.entrySet().iterator();

            HashMap.Entry pair = (HashMap.Entry) it.next();
            origin = (Tuple<Double, Double>) pair.getValue();
            pair = (HashMap.Entry) it.next();
            x_axis = (Tuple<Double, Double>) pair.getValue();
            pair = (HashMap.Entry) it.next();
            i_j = (Tuple<Double, Double>) pair.getValue();
        }
        else {
            origin = new Tuple<>(Double.parseDouble(dEditText.getText().toString().split(",")[0]),
                    Double.parseDouble(dEditText.getText().toString().split(",")[1]));
            x_axis = new Tuple<>(Double.parseDouble(iEditText.getText().toString().split(",")[0]),
                    Double.parseDouble(iEditText.getText().toString().split(",")[1]));
            i_j = new Tuple<>(Double.parseDouble(jEditText.getText().toString().split(",")[0]),
                    Double.parseDouble(jEditText.getText().toString().split(",")[1]));
        }

        System.out.println("0,0 = "+origin.x+", "+origin.y);
        System.out.println("d,0 = "+x_axis.x+", "+x_axis.y);
        System.out.println("i,j = "+i_j.x+", "+i_j.y);
        x_axis.x =  x_axis.x - origin.x;
        x_axis.y =  x_axis.y - origin.y;
        i_j.x =  i_j.x - origin.x;
        i_j.y =  i_j.y - origin.y;
        System.out.println("0,0 = "+origin.x+", "+origin.y);
        System.out.println("d,0 = "+x_axis.x+", "+x_axis.y);
        System.out.println("i,j = "+i_j.x+", "+i_j.y);
        Double theta = Math.atan(x_axis.y/x_axis.x);
        if(x_axis.x<0 && x_axis.y>0)
            theta = Math.PI - theta;
        if(x_axis.x<0 && x_axis.y<0)
            theta = Math.PI + theta;

        System.out.println("theta = "+theta);
        Double tempx, tempy;
        tempx = x_axis.x * Math.cos(theta) + x_axis.y * Math.sin(theta);
        tempy = x_axis.y * Math.cos(theta) - x_axis.x * Math.sin(theta);
        x_axis.x = tempx;
        x_axis.y = tempy;
        tempx = i_j.x * Math.cos(theta) + i_j.y * Math.sin(theta);
        tempy = i_j.y * Math.cos(theta) - i_j.x * Math.sin(theta);
        i_j.x = tempx;
        i_j.y = tempy;

        System.out.println("d,0 = "+x_axis.x+", "+x_axis.y);
        System.out.println("i,j = "+i_j.x+", "+i_j.y);

        return new FourValues<>(x_axis.x, i_j.x, i_j.y, theta, origin);

    }

    protected Tuple<Double, Double> inverseTransformedCoords(Tuple<Double, Double> prev, Double theta, Tuple<Double, Double> origin) {
        Double tempx, tempy;

        tempx = prev.x * Math.cos(theta) - prev.y * Math.sin(theta) + origin.x;
        tempy = prev.y * Math.cos(theta) + prev.x * Math.sin(theta) + origin.y;

        return new Tuple<>(tempx, tempy);
    }

    public void showDialog(String title, String message) {
        alertDialog.setTitle(title);
        alertDialog.setMessage(message);
        alertDialog.setPositiveButton("Ok",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        //dismiss the dialog
                    }
                });
//        alertDialog.setCancelable(true);
        alertDialog.create().show();
    }
}

class Tuple<X, Y> {
    public X x;
    public Y y;
    Tuple(X x, Y y) {
        this.x = x;
        this.y = y;
    }
    @Override
    public String toString() {
        return "("+x.toString()+","+y.toString()+")";
    }
}

class FourValues<X, Y, A, B> {
    public X x;
    public Y y;
    public A a;
    public B b;
    public Tuple<X,Y> origin;
    FourValues(X x, Y y, A a, B b, Tuple<X,Y> o) {
        this.x = x;
        this.y = y;
        this.a = a;
        this.b = b;
        origin = o;
    }
}


