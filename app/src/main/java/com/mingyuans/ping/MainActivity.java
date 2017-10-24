package com.mingyuans.ping;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final Button clickButton = (Button) findViewById(R.id.tv_main_click);
        clickButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                while (true) {
                    tryPing("www.taobao.com");
                }
            }
        });
    }


    private void tryPing(String hostname) {
        String pingCommand = AndroidPing.createSimplePingCommand(hostname,1,1);
        Log.i("AndroidPing","Ping command: " + pingCommand);
        String pingAnswerString = AndroidPing.ping(pingCommand);
        Log.i("AndroidPing","Ping answer string: " + pingAnswerString);
        AndroidPing.PingAnswer pingAnswer = AndroidPing.parsePingAnswerString(pingAnswerString);
        if (pingAnswer != null) {
            Log.i("AndroidPing",String.valueOf(pingAnswer));
        } else {
            Log.i("AndroidPing","ping answer is null!!!");
        }
    }

}
