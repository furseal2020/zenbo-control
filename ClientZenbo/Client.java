package com.example.clientzenbo;

import androidx.appcompat.app.AppCompatActivity;
import io.github.controlwear.virtual.joystick.android.JoystickView;

import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.HashMap;

public class Client extends AppCompatActivity {

    public TextView serverState;
    public String IP;
    public int Port;
    public String tmp;
    public Socket socket_client;
    private Thread thread;
    private BufferedReader br;
    private BufferedWriter bw;
    public Handler handler = new Handler();
    public Button disconnect;
    public TextView responseMsg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client);

        Bundle bundle = this.getIntent().getExtras();
        IP = bundle.getString("IP");
        Port = bundle.getInt("Port");
        disconnect = findViewById(R.id.disconnect);
        serverState = findViewById(R.id.serverState);
        responseMsg = findViewById(R.id.responseMsg);
        socket_client = null;
        StrictMode.ThreadPolicy tp = StrictMode.ThreadPolicy.LAX;  //to escape the NetworkOnMainThreadException
        StrictMode.setThreadPolicy(tp);
        thread = new Thread(readData);
        thread.start();


        final JoystickView joystickLeft = (JoystickView) findViewById(R.id.joystickView_left); //for body
        joystickLeft.setOnMoveListener(new JoystickView.OnMoveListener() {
            @Override
            public void onMove(int angle, int strength) {
                if(angle >= 45 && angle < 135)
                {
                    sendMessage(setGsonMsg("action", "FORWARD"));
                }
                else if(angle >= 135 && angle < 225) {
                    sendMessage(setGsonMsg("action", "TURN_LEFT"));
                }
                else if(angle >= 225 && angle < 315) {
                    sendMessage(setGsonMsg("action", "BACKWARD"));
                }
                else if(angle >= 315 || angle < 45) {
                    sendMessage(setGsonMsg("action", "TURN_RIGHT"));
                }
                if (strength < 10){
                    sendMessage(setGsonMsg("action", "STOP"));
                }
            }
        }, 250);
        final JoystickView joystickRight = (JoystickView) findViewById(R.id.joystickView_right); //for head
        joystickRight.setOnMoveListener(new JoystickView.OnMoveListener() {
            @Override
            public void onMove(int angle, int strength) {
                if(angle >= 45 && angle < 135) {
                    sendMessage(setGsonMsg("action", "HEAD UP"));
                }
                else if(angle >= 135 && angle < 225) {
                    sendMessage(setGsonMsg("action", "LEFT"));
                }
                else if(angle >= 225 && angle < 315) {
                    sendMessage(setGsonMsg("action", "HEAD DOWN"));
                }
                else if(angle >= 315 || angle < 45) {
                    sendMessage(setGsonMsg("action", "RIGHT"));
                }
                if (strength < 10){
                    sendMessage(setGsonMsg("action", "NEUTRAL"));
                }

            }
        }, 250);
    }

    public void btnLight_onClick(View view)
    {
        sendMessage(setGsonMsg("action", "LIGHT"));
    }

    private String setGsonMsg(String type, String content){
        HashMap<String, Object> map = new HashMap<String, Object>();
        map.put("Type", type);
        map.put("Content", content);

        return new GsonBuilder().create().toJson(map);
    }

    private Runnable readData = new Runnable() {
        public void run() {
            try {
                socket_client = new Socket();
                SocketAddress Address = new InetSocketAddress(IP, Port);
                socket_client.connect(Address, 1500);
                Client.this.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        serverState.setText("Server Active");
                    }
                });


                br = new BufferedReader(new InputStreamReader(
                        socket_client.getInputStream()));

                while (socket_client.isConnected()) {

                    String receiveJsonMsg =  br.readLine();
                    Gson gson=  new GsonBuilder().create();
                    JsonObject jsonObject = gson.fromJson(receiveJsonMsg, JsonObject.class);
                    String msgType = jsonObject.get("Type").getAsString();
                    tmp = jsonObject.get("Content").getAsString();


                    if(msgType.equals("text"))
                    {
                        handler.post(updateText);
                    }

                }

            } catch (Exception e) {
                Log.e("text", e.toString());
                Client.this.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        serverState.setText("Server Not Active");
                        responseMsg.append("Server Disconnect\n");
                    }
                });
            }finally {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e2) {
                    e2.printStackTrace();
                }
                if(socket_client != null)
                    try{
                        socket_client.close();
                    }catch (Exception e2){
                        Log.e("text", e2.toString());
                    }
                finish();
            }
        }
    };

    private void sendMessage(String command){
        if(socket_client.isConnected()){
            try {
                bw = new BufferedWriter( new OutputStreamWriter(socket_client.getOutputStream()));
                bw.write(command+"\n");
                bw.flush();
            } catch (IOException e) {
                Log.e("text", e.toString());
                responseMsg.append("Send Fail\n");
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e2) {
                    e2.printStackTrace();
                }
                if(socket_client != null)
                    try{
                        socket_client.close();
                    }catch (Exception e2){
                        Log.e("text", e2.toString());
                    }
                finish();
            }
        }
    }

    private Runnable updateText = new Runnable() {
        public void run() {

            responseMsg.append(tmp + "\n");
        }
    };

    public void Disconnect(View v) throws IOException {
        //stopCheck();
        sendMessage(setGsonMsg("disconnect", "DISCONNECT"));

        if(socket_client != null) {
            socket_client.close();
            socket_client = null;

        }

        finish();
        //Call this when your activity is done and should be closed.
        //當呼叫此方法時，系統只是將最上面的 Activity 移出 stack，並沒有及時的調用 onDestory() 方法，所以下面的 Code 還是會繼續執行喔！，其佔用的資源也沒有被及時釋放。 因為移出了stack，所以當按下手機的 back 鍵的時候，也找不到此 Activity。
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        try{
            sendMessage(setGsonMsg("disconnect", "DISCONNECT"));
            bw.close();
            br.close();
            socket_client.close();
        }
        catch (Exception e){}

    }
}
