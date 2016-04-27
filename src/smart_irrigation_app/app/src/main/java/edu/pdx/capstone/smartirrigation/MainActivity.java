package edu.pdx.capstone.smartirrigation;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.widget.Toast;

import java.util.UUID;

public class MainActivity extends AppCompatActivity
{
    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
    public static final String EXTRAS_DEVICE = "DEVICE";

    public static String device_name;
    public static String device_address;

    public static final String BLE_SERVICE = "30d1beef-a6ff-4f2f-8a2f-a267a2dbe320";
    public static final String BLE_CHAR_TEMP = "30d1001b-a6ff-4f2f-8a2f-a267a2dbe320";

    private BluetoothAdapter mBTAdapter;
    private BluetoothDevice mDevice;
    private BluetoothGatt mConnGatt;
    private BluetoothGattCharacteristic mTempChar;
    private int mStatus;

    private TextView data_temp;


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        data_temp = (TextView) findViewById(R.id.data_sensor_temp);

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

        if (intent.hasExtra(EXTRAS_DEVICE))
        {
            mDevice = intent.getParcelableExtra(EXTRAS_DEVICE);
        }

        mStatus = BluetoothProfile.STATE_DISCONNECTED;
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);

        mBTAdapter = bluetoothManager.getAdapter();

        if (mBTAdapter == null)
        {
            Toast.makeText(this, "BT unavailable", Toast.LENGTH_SHORT).show();
            finish();
        }

        if (mDevice != null)
        {
            if ( (mConnGatt == null) && (mStatus == BluetoothProfile.STATE_DISCONNECTED) )
            {
                mConnGatt = mDevice.connectGatt(this, false, mGattCallback);
                mStatus = BluetoothProfile.STATE_CONNECTING;
            }
            else
            {
                if (mConnGatt != null)
                {
                    mConnGatt.connect();
                    mConnGatt.discoverServices();
                }
                else
                {
                    Log.e("Smart_Irrigation",  "State error");
                    finish();
                }
            }
        }
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        if (mConnGatt != null)
        {
            if ( (mStatus != BluetoothProfile.STATE_DISCONNECTING) && (mStatus != BluetoothProfile.STATE_DISCONNECTED) )
            {
                mConnGatt.disconnect();
            }

            mConnGatt.close();
            mConnGatt = null;
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

    public void onClickReadSensors(View view)
    {
        mConnGatt.readCharacteristic(mTempChar);
    }

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback()
    {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState)
        {
            if (newState == BluetoothProfile.STATE_CONNECTED)
            {
                mStatus = newState;
                mConnGatt.discoverServices();
            }
            else if (newState == BluetoothProfile.STATE_DISCONNECTED)
            {
                mStatus = newState;

            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status)
        {
            for (BluetoothGattService service : gatt.getServices())
            {
                if ( (service == null) || (service.getUuid() == null) )
                {
                    continue;
                }
                else if ( (BLE_SERVICE.equalsIgnoreCase(service.getUuid().toString())))
                {
                    mTempChar = service.getCharacteristic(UUID.fromString(BLE_CHAR_TEMP));
                }
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status)
        {
            if (status == BluetoothGatt.GATT_SUCCESS)
            {
                final String temperature = characteristic.getStringValue(0);

                runOnUiThread(new Runnable()
                {
                    public void run()
                    {
                        data_temp.setText(temperature);
                    };
                });
            }
        }
    };


}
