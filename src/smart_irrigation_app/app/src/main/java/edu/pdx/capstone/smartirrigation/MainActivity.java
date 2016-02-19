package edu.pdx.capstone.smartirrigation;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity
{
    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
    public static String device_name;
    public static String device_address;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Intent intent = getIntent();
        if (intent.hasExtra(EXTRAS_DEVICE_NAME))
        {
            device_name = intent.getStringExtra(EXTRAS_DEVICE_NAME);
            TextView info_device_name = (TextView) findViewById(R.id.info_device_name);
            info_device_name.setText(device_name);
        }

        if (intent.hasExtra(EXTRAS_DEVICE_ADDRESS))
        {
            device_address = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);
            TextView info_device_name = (TextView) findViewById(R.id.info_device_address);
            info_device_name.setText(device_address);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int id = item.getItemId();

        if (id == R.id.device_scan)
        {
            Intent intent_device_scan = new Intent(this, DeviceScanActivity.class);
            startActivity(intent_device_scan);
            return true;
        }

        return super.onOptionsItemSelected(item);
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
