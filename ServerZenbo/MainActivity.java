package com.example.serverzenbo;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.graphics.Color;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.asus.robotframework.API.MotionControl;
import com.asus.robotframework.API.RobotAPI;
import com.asus.robotframework.API.WheelLights;

import static com.asus.robotframework.API.WheelLights.Lights.SYNC_BOTH;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    private RobotAPI m_robot;
    private ServerSocket serverSocket;
    private Socket tempClientSocket;
    Thread serverThread = null;
    public static final int SERVER_PORT = 1803;
    private LinearLayout msgList;
    private Handler handler;
    private int greenColor;
    String serverIPAddr;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setTitle("Server Zenbo");
        m_robot = new RobotAPI(getApplicationContext(), null);
        greenColor = ContextCompat.getColor(this, R.color.green);
        handler = new Handler();
        msgList = findViewById(R.id.msgList);

    }

    public TextView textView(String message, int color) {
        TextView tv = new TextView(this);
        tv.setTextColor(color);
        tv.setText(message + " [" + getTime() +"]");
        tv.setTextSize(20);
        tv.setPadding(0, 5, 0, 0);
        return tv;
    }

    public void showMessage(final String message, final int color) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                msgList.addView(textView(message, color));
            }
        });
    }


    public void onClick(View view) {
        if (view.getId() == R.id.start_server) {
            new Thread(new GetWifiIP()).start();
            while(serverIPAddr==null)
            {
                //stuck here.
            }
            showMessage(serverIPAddr+ ":" + SERVER_PORT, Color.BLACK);

            msgList.removeAllViews();
            showMessage("Server Started.", Color.BLACK);
            this.serverThread = new Thread(new ServerThread());
            this.serverThread.start();
            return;
        }
    }

    class GetWifiIP implements Runnable {

        @Override
        public void run()
        {
            WifiManager wifi_service = (WifiManager)getApplicationContext().getSystemService(WIFI_SERVICE);
            WifiInfo wifiInfo = wifi_service.getConnectionInfo();
            int ipAddress = wifiInfo.getIpAddress();
            serverIPAddr = String.format("%d.%d.%d.%d",(ipAddress & 0xff),(ipAddress >> 8 & 0xff),(ipAddress >> 16 & 0xff),(ipAddress >> 24 & 0xff));
        }
    }


    class ServerThread implements Runnable {

        public void run() {
            Socket socket;
            try {
                serverSocket = new ServerSocket(SERVER_PORT);
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        findViewById(R.id.start_server).setVisibility(View.GONE);
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
                showMessage("Error Starting Server : " + e.getMessage(), Color.RED);
            }
            if (null != serverSocket) {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        socket = serverSocket.accept();
                        CommunicationThread commThread = new CommunicationThread(socket);
                        new Thread(commThread).start();
                    } catch (IOException e) {
                        e.printStackTrace();
                        showMessage("Error Communicating to Client :" + e.getMessage(), Color.RED);
                    }
                }
            }
        }
    }

    class CommunicationThread implements Runnable {

        private Socket clientSocket;
        private boolean flag=FALSE; //for LIGHT
        private BufferedReader input;

        public CommunicationThread(Socket clientSocket) {
            this.clientSocket = clientSocket;
            tempClientSocket = clientSocket;
            try {
                this.input = new BufferedReader(new InputStreamReader(this.clientSocket.getInputStream()));
            } catch (IOException e) {
                e.printStackTrace();
                showMessage("Error Connecting to Client!!", Color.RED);
            }
            showMessage("Connected to Client!!", greenColor);
        }

        public void run() {

            while (!Thread.currentThread().isInterrupted()) {
                try {
                    String read = input.readLine();
                    if (null == read || "Disconnect".contentEquals(read)) {
                        Thread.interrupted();
                        read = "Client Disconnected!!";

                        showMessage(read, greenColor);
                        break;
                    }
                    showMessage("Client : " + read, greenColor);
                    //for body
                    if(read.contains("FORWARD")) {
                        m_robot.motion.remoteControlBody(MotionControl.Direction.Body.FORWARD);
                    }
                    else if(read.contains("TURN_RIGHT")) {
                        m_robot.motion.remoteControlBody(MotionControl.Direction.Body.TURN_RIGHT);
                    }
                    else if(read.contains("TURN_LEFT")) {
                        m_robot.motion.remoteControlBody(MotionControl.Direction.Body.TURN_LEFT);
                    }
                    else if(read.contains("BACKWARD")) {
                        m_robot.motion.remoteControlBody(MotionControl.Direction.Body.BACKWARD);
                    }
                    else if(read.contains("STOP")) {
                        m_robot.motion.remoteControlBody(MotionControl.Direction.Body.STOP);
                    }
                    //for head
                    else if (read.contains("HEAD UP"))
                    {
                        m_robot.motion.remoteControlHead(MotionControl.Direction.Head.UP);
                    }
                    else if (read.contains("HEAD DOWN"))
                    {
                        m_robot.motion.remoteControlHead(MotionControl.Direction.Head.DOWN);
                    }
                    else if (read.contains("RIGHT"))
                    {
                        m_robot.motion.remoteControlHead(MotionControl.Direction.Head.RIGHT);
                    }
                    else if (read.contains("LEFT"))
                    {
                        m_robot.motion.remoteControlHead(MotionControl.Direction.Head.LEFT);
                    }
                    else if (read.contains("NEUTRAL"))
                    {
                        m_robot.motion.remoteControlHead(MotionControl.Direction.Head.STOP);
                    }
                    else if (read.contains("LIGHT")) {
                        if(flag==FALSE) {
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    flag=TRUE;
                                    /*
                                    m_robot.wheelLights.startBlinking(WheelLights.Lights.SYNC_BOTH, 255, 100, 100, 5); //red color light
                                    m_robot.wheelLights.turnOff(WheelLights.Lights.SYNC_BOTH, 255);

                                     */
                                    m_robot.wheelLights.turnOff(WheelLights.Lights.SYNC_BOTH, 0xff);
                                    m_robot.wheelLights.setColor(WheelLights.Lights.SYNC_BOTH, 0xff, 0x007F7F);
                                    m_robot.wheelLights.setBrightness(WheelLights.Lights.SYNC_BOTH, 0xff, 10);
                                    m_robot.wheelLights.startBlinking(WheelLights.Lights.SYNC_BOTH, 0xff, 30, 10, 5);
                                    flag=FALSE;
                                }
                            }).start();
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    String getTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        return sdf.format(new Date());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (null != serverThread) {
            serverThread.interrupt();
            serverThread = null;
        }
    }
}
