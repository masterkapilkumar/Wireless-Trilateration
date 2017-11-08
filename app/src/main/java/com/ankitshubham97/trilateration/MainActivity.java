package com.ankitshubham97.trilateration;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.format.Formatter;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteOrder;

public class MainActivity extends AppCompatActivity {
    Button getInfoButton;
    WifiManager wifiManager;
    WifiInfo wifiInfo;
    TextView infoText;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getInfoButton=(Button)findViewById(R.id.button3);
        infoText = (TextView)findViewById(R.id.infoText);


        getInfoButton.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                wifiInfo= wifiManager.getConnectionInfo();
                String infogetBSSID = wifiInfo.getBSSID();
//                String infogetBSSID = wifiInfo.getMacAddress();
                String infogetSSID = wifiInfo.getSSID();
                String infogetFrequency = wifiInfo.getFrequency()+"";
                int ipAddress = wifiInfo.getIpAddress();

                // Convert little-endian to big-endianif needed
                if (ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN)) {
                    ipAddress = Integer.reverseBytes(ipAddress);
                }

                byte[] ipByteArray = BigInteger.valueOf(ipAddress).toByteArray();

                String ipAddressString;
                try {
                    ipAddressString = InetAddress.getByAddress(ipByteArray).getHostAddress();
                } catch (UnknownHostException ex) {
                    Log.e("WIFIIP", "Unable to get host address.");
                    ipAddressString = null;
                }

                String infogetIpAddress =  ipAddressString;
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
        });

    }
}
