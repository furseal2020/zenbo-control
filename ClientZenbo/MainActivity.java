package com.example.clientzenbo;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    public static Handler mHandler = new Handler();
    TextView response;
    EditText IP, Port;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        response = (TextView) findViewById(R.id.response);
        IP = (EditText) findViewById(R.id.ip);
        Port = (EditText) findViewById(R.id.port);

        Button connect = (Button) findViewById(R.id.connect);

        connect.setOnClickListener(new Button.OnClickListener(){
            public void onClick(View v){
                Bundle bundle = new Bundle();
                if(TextUtils.isEmpty(IP.getText().toString())| TextUtils.isEmpty(Port.getText().toString())){
                    response.setText("IP or Port is empty\n");
                }
                else {
                    response.setText("");
                    bundle.putString("IP", IP.getText().toString());
                    bundle.putInt("Port", Integer.parseInt(Port.getText().toString()));

                    Intent intent = new Intent( MainActivity.this, Client.class);
                    intent.putExtras(bundle);
                    startActivity(intent);
                }
            }
        });
    }
}
