package com.example.photoupload;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

public class StartScreen extends AppCompatActivity {

    TextView St;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_screen);
        St = (TextView)findViewById(R.id.Start);
        St.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent in=new Intent(StartScreen.this,MainActivity.class);
                startActivity(in);
            }
        });

    }
}