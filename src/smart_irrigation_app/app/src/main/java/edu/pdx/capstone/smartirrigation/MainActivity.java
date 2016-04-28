package edu.pdx.capstone.smartirrigation;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
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

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationServices;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import java.util.UUID;

public class MainActivity extends AppCompatActivity implements ConnectionCallbacks, OnConnectionFailedListener
{
    public static final String EXTRAS_DEVICE_NAME       = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS    = "DEVICE_ADDRESS";
    public static final String EXTRAS_DEVICE            = "DEVICE";

    public static String mDeviceName;
    public static String mDeviceAddr;

    public static final String BLE_UUID_SERVICE     = "30d1beef-a6ff-4f2f-8a2f-a267a2dbe320";
    public static final String BLE_UUID_CHAR_TEMP   = "30d1001a-a6ff-4f2f-8a2f-a267a2dbe320";
    public static final String BLE_UUID_CHAR_SOIL   = "30d1001b-a6ff-4f2f-8a2f-a267a2dbe320";
    public static final String BLE_UUID_CHAR_RAIN   = "30d1001c-a6ff-4f2f-8a2f-a267a2dbe320";
    public static final String BLE_UUID_CHAR_DATE   = "30d1000a-a6ff-4f2f-8a2f-a267a2dbe320";
    public static final String BLE_UUID_CHAR_TIME   = "30d1000b-a6ff-4f2f-8a2f-a267a2dbe320";
    public static final String BLE_UUID_CHAR_AREA   = "30d1000c-a6ff-4f2f-8a2f-a267a2dbe320";
    public static final String BLE_UUID_CHAR_FLOOD  = "30d1000d-a6ff-4f2f-8a2f-a267a2dbe320";
    public static final String BLE_UUID_CHAR_LATITUDE = "30d1000e-a6ff-4f2f-8a2f-a267a2dbe320";

    private BluetoothAdapter mBTAdapter;
    private BluetoothDevice mBTDevice;
    private BluetoothGatt mBTGatt;
    private BluetoothGattCharacteristic mBTCharTemp;
    private BluetoothGattCharacteristic mBTCharSoil;
    private BluetoothGattCharacteristic mBTCharRain;
    private BluetoothGattCharacteristic mBTCharDate;
    private BluetoothGattCharacteristic mBTCharTime;
    private BluetoothGattCharacteristic mBTCharArea;
    private BluetoothGattCharacteristic mBTCharFlood;
    private BluetoothGattCharacteristic mBTCharLatitude;

    private int mBTStatus;

    private TextView mDataTemp;
    private TextView mDataSoil;
    private TextView mDataRain;
    private TextView mConfigDate;
    private TextView mConfigLatitude;
    private TextView mConfigTime;
    private TextView mConfigArea;

    GoogleApiClient mGoogleApiClient = null;
    Location mLastLocation;
    final int REQUEST_CODE_LOCATION_PERMISSION = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Create an instance of GoogleAPIClient.
        if (mGoogleApiClient == null)
        {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }

        mDataTemp = (TextView) findViewById(R.id.data_sensor_temp);
        mDataSoil = (TextView) findViewById(R.id.data_sensor_soil);
        mDataRain = (TextView) findViewById(R.id.data_sensor_rain);

        mConfigDate = (TextView) findViewById(R.id.input_date);
        mConfigLatitude = (TextView) findViewById(R.id.input_latitude);
        mConfigTime = (TextView) findViewById(R.id.input_time);
        mConfigArea = (TextView) findViewById(R.id.input_area);

        Intent intent = getIntent();
        if (intent.hasExtra(EXTRAS_DEVICE_NAME))
        {
            mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
            TextView info_device_name = (TextView) findViewById(R.id.info_device_name);
            info_device_name.setText(mDeviceName);
        }

        if (intent.hasExtra(EXTRAS_DEVICE_ADDRESS))
        {
            mDeviceAddr = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);
            TextView info_device_name = (TextView) findViewById(R.id.info_device_address);
            info_device_name.setText(mDeviceAddr);
        }

        if (intent.hasExtra(EXTRAS_DEVICE))
        {
            mBTDevice = intent.getParcelableExtra(EXTRAS_DEVICE);
        }

        mBTStatus = BluetoothProfile.STATE_DISCONNECTED;
    }

    @Override
    public void onConnected(Bundle connectionHint)
    {
        /* New permissions model for API 23 and beyond only */
        int api_version = android.os.Build.VERSION.SDK_INT;
        if (api_version >= Build.VERSION_CODES.M)
        {
            int hasLocationPermission = checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION);
            if (hasLocationPermission != PackageManager.PERMISSION_GRANTED)
            {
                requestPermissions(new String[] {Manifest.permission.ACCESS_COARSE_LOCATION},
                        REQUEST_CODE_LOCATION_PERMISSION);
                return;
            }
        }

    }

    @Override
    public void onConnectionSuspended(int param)
    {
        // Stub
    }

    @Override
    public void onConnectionFailed(ConnectionResult result)
    {
        // Stub
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

        if (mBTDevice != null)
        {
            if ( (mBTGatt == null) && (mBTStatus == BluetoothProfile.STATE_DISCONNECTED) )
            {
                mBTGatt = mBTDevice.connectGatt(this, false, mGattCallback);
                mBTStatus = BluetoothProfile.STATE_CONNECTING;
            }
            else
            {
                if (mBTGatt != null)
                {
                    mBTGatt.connect();
                    mBTGatt.discoverServices();
                }
            }
        }
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        if (mBTGatt != null)
        {
            if ( (mBTStatus != BluetoothProfile.STATE_DISCONNECTING) && (mBTStatus != BluetoothProfile.STATE_DISCONNECTED) )
            {
                mBTGatt.disconnect();
            }

            mBTGatt.close();
            mBTGatt = null;
        }
    }

    protected void onStart()
    {
        mGoogleApiClient.connect();
        super.onStart();
    }

    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    public void onClickConfig(View view)
    {
        if (mBTGatt == null)
        {
            Toast.makeText(this, "Not connected to a device", Toast.LENGTH_LONG).show();
        }
        else
        {
            mBTCharDate.setValue(mConfigDate.getText().toString());
            mBTGatt.writeCharacteristic(mBTCharDate);
        }
    }

    public void onClickConnect(View view)
    {
        Intent intent_device_scan = new Intent(this, DeviceScanActivity.class);
        startActivity(intent_device_scan);
    }

    public void onClickReadSensors(View view)
    {
        if (mBTGatt == null)
        {
            Toast.makeText(this, "Not connected to a device", Toast.LENGTH_LONG).show();
        }
        else
        {
            mBTGatt.readCharacteristic(mBTCharTemp);
        }
    }

    public void onClickFlood(View view)
    {
        if (mBTGatt == null)
        {
            Toast.makeText(this, "Not connected to a device", Toast.LENGTH_LONG).show();
        }
        else
        {
            mBTCharFlood.setValue("1");
            mBTGatt.writeCharacteristic(mBTCharFlood);
        }
    }

    public void onClickButtonLatitudeGet(View view)
    {
        EditText input_latitude = (EditText) findViewById(R.id.input_latitude);

        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (mLastLocation != null)
        {
            double latitude = mLastLocation.getLatitude();
            DecimalFormat decimal_format = new DecimalFormat("#.##");
            latitude = Double.valueOf(decimal_format.format(latitude));

            input_latitude.setText(String.valueOf(latitude));
        }
        else
        {
            input_latitude.setText(0);
        }
    }

    public void onClickButtonDateGet(View view)
    {
        EditText input_date = (EditText) findViewById(R.id.input_date);

        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat date_format = new SimpleDateFormat("MMddyy", Locale.US);
        String date = date_format.format(calendar.getTime());

        input_date.setText(date);
    }

    public void onClickButtonTimeGet(View view)
    {
        EditText input_time = (EditText) findViewById(R.id.input_time);

        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat time_format = new SimpleDateFormat("kkmmss", Locale.US);
        String time = time_format.format(calendar.getTime());

        input_time.setText(time);
    }

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback()
    {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState)
        {
            if (newState == BluetoothProfile.STATE_CONNECTED)
            {
                mBTStatus = newState;
                mBTGatt.discoverServices();
            }
            else if (newState == BluetoothProfile.STATE_DISCONNECTED)
            {
                mBTStatus = newState;

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
                else if ( (BLE_UUID_SERVICE.equalsIgnoreCase(service.getUuid().toString())))
                {
                    mBTCharTemp = service.getCharacteristic(UUID.fromString(BLE_UUID_CHAR_TEMP));
                    mBTCharSoil = service.getCharacteristic(UUID.fromString(BLE_UUID_CHAR_SOIL));
                    mBTCharRain = service.getCharacteristic(UUID.fromString(BLE_UUID_CHAR_RAIN));
                    mBTCharDate = service.getCharacteristic(UUID.fromString(BLE_UUID_CHAR_DATE));
                    mBTCharTime = service.getCharacteristic(UUID.fromString(BLE_UUID_CHAR_TIME));
                    mBTCharArea = service.getCharacteristic(UUID.fromString(BLE_UUID_CHAR_AREA));
                    mBTCharLatitude = service.getCharacteristic(UUID.fromString(BLE_UUID_CHAR_LATITUDE));
                    mBTCharFlood = service.getCharacteristic(UUID.fromString(BLE_UUID_CHAR_FLOOD));
                }
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic, int status)
        {
            if (status == BluetoothGatt.GATT_SUCCESS)
            {
                final String char_string = characteristic.getStringValue(0);

                runOnUiThread(new Runnable()
                {
                    public void run()
                    {
                        if ( BLE_UUID_CHAR_TEMP.equalsIgnoreCase(characteristic.getUuid().toString()) )
                        {
                            mDataTemp.setText(char_string);

                            mBTGatt.readCharacteristic(mBTCharSoil);
                        }
                        else if ( BLE_UUID_CHAR_SOIL.equalsIgnoreCase(characteristic.getUuid().toString()) )
                        {
                            mDataSoil.setText(char_string);

                            mBTGatt.readCharacteristic(mBTCharRain);
                        }
                        else if ( BLE_UUID_CHAR_RAIN.equalsIgnoreCase(characteristic.getUuid().toString()) )
                        {
                            mDataRain.setText(char_string);
                        }

                    }
                });
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status)
        {
            if (status == BluetoothGatt.GATT_SUCCESS)
            {
                if ( BLE_UUID_CHAR_DATE.equalsIgnoreCase(characteristic.getUuid().toString()) )
                {
                    mBTCharTime.setValue(mConfigTime.getText().toString());
                    mBTGatt.writeCharacteristic(mBTCharTime);
                }
                else if ( BLE_UUID_CHAR_TIME.equalsIgnoreCase(characteristic.getUuid().toString()) )
                {

                    mBTCharArea.setValue(mConfigArea.getText().toString());
                    mBTGatt.writeCharacteristic(mBTCharArea);
                }
                else if ( BLE_UUID_CHAR_AREA.equalsIgnoreCase(characteristic.getUuid().toString()))
                {
                    // Latitude needs to be multiplied by 100 because BL600 cannot deal in floats
                    float latitude = Float.parseFloat(mConfigLatitude.getText().toString());
                    latitude *= 100;
                    int latitude_rounded = Math.round(latitude); // Get rid of trailing decimal point

                    mBTCharLatitude.setValue(String.valueOf(latitude_rounded));
                    mBTGatt.writeCharacteristic(mBTCharLatitude);
                }
            }
        }
    };


}
