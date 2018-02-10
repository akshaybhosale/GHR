package com.example.admin.ghr;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;

public class MainActivity extends AppCompatActivity {
    private Button mAmbulance,mVictim;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAmbulance=(Button) findViewById(R.id.ambulance_button);
        mVictim=(Button) findViewById(R.id.victim_button);

        mAmbulance.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent=new Intent(MainActivity.this,AmbulanceLoginActivity.class);
                startActivity(intent);
                finish();
                return;
            }
        });

        mVictim.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent=new Intent(MainActivity.this,VictimLoginActivity.class);
                startActivity(intent);
                finish();
                return;
            }
        });

    }
}
