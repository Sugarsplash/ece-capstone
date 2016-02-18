package edu.pdx.capstone.smartirrigation;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    }

    public void onClickConfig(View view)
    {
        Intent intent_config = new Intent(this, ConfigActivity.class);
        startActivity(intent_config);
    }

    public void onClickReceive(View view)
    {
        Intent intent_receive = new Intent(this, ReceiveActivity.class);
        startActivity(intent_receive);
    }
}
