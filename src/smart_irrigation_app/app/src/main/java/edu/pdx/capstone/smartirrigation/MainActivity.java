package edu.pdx.capstone.smartirrigation;

import android.Manifest;
import android.bluetooth.BluetoothGattDescriptor;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import java.util.UUID;

public class MainActivity extends AppCompatActivity implements ConnectionCallbacks, OnConnectionFailedListener
{
    // Extras used to pass device information from the scanning activity back
    public static final String EXTRAS_DEVICE_NAME       = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS    = "DEVICE_ADDRESS";
    public static final String EXTRAS_DEVICE            = "DEVICE";

    // Strings to hold the device name and address retrieved from the extras
    public static String mDeviceName;
    public static String mDeviceAddr;

    private static final double SQ_METER_TO_SQ_FT = 10.7639;
    private static final double SQ_FT_TO_SQ_METER = 1/SQ_METER_TO_SQ_FT;

    public static final String BLE_UUID_SERVICE             = "30d1beef-a6ff-4f2f-8a2f-a267a2dbe320";
    public static final String BLE_UUID_CHAR_DATE           = "30d1000a-a6ff-4f2f-8a2f-a267a2dbe320";
    public static final String BLE_UUID_CHAR_TIME           = "30d1000b-a6ff-4f2f-8a2f-a267a2dbe320";
    public static final String BLE_UUID_CHAR_AREA           = "30d1000c-a6ff-4f2f-8a2f-a267a2dbe320";
    public static final String BLE_UUID_CHAR_FLOOD          = "30d1000d-a6ff-4f2f-8a2f-a267a2dbe320";
    public static final String BLE_UUID_CHAR_LATITUDE       = "30d1000e-a6ff-4f2f-8a2f-a267a2dbe320";
    public static final String BLE_UUID_CHAR_TEMP           = "30d1001a-a6ff-4f2f-8a2f-a267a2dbe320";
    public static final String BLE_UUID_CHAR_SOIL           = "30d1001b-a6ff-4f2f-8a2f-a267a2dbe320";
    public static final String BLE_UUID_CHAR_RAIN           = "30d1001c-a6ff-4f2f-8a2f-a267a2dbe320";
    public static final String BLE_UUID_CHAR_FLOW           = "30d1001d-a6ff-4f2f-8a2f-a267a2dbe320";
    public static final String BLE_UUID_CHAR_SENSOR_SIGNAL  = "30d1001e-a6ff-4f2f-8a2f-a267a2dbe320";
    public static final String BLE_UUID_CHAR_HISTORY_SIGNAL = "30d10020-a6ff-4f2f-8a2f-a267a2dbe320";
    public static final String BLE_UUID_CHAR_HISTORY        = "30d10021-a6ff-4f2f-8a2f-a267a2dbe320";
    public static final String BLE_UUID_CHAR_HISTORY_SIZE   = "30d10022-a6ff-4f2f-8a2f-a267a2dbe320";
    public static final String BLE_UUID_CHAR_NOTIFY_DESC    = "00002902-0000-1000-8000-00805f9b34fb";

    private BluetoothDevice mBTDevice;
    private BluetoothGatt mBTGatt;
    private BluetoothGattDescriptor mHistoryDescriptor;
    private BluetoothGattDescriptor mHistorySizeDescriptor;
    private BluetoothGattCharacteristic mBTCharDate;
    private BluetoothGattCharacteristic mBTCharTime;
    private BluetoothGattCharacteristic mBTCharArea;
    private BluetoothGattCharacteristic mBTCharFlood;
    private BluetoothGattCharacteristic mBTCharSensorSignal;
    private BluetoothGattCharacteristic mBTCharLatitude;
    private BluetoothGattCharacteristic mBTCharTemp;
    private BluetoothGattCharacteristic mBTCharSoil;
    private BluetoothGattCharacteristic mBTCharRain;
    private BluetoothGattCharacteristic mBTCharFlow;
    private BluetoothGattCharacteristic mBTCharHistorySignal;
    private BluetoothGattCharacteristic mBTCharHistorySize;

    private int mBTStatus;
    private boolean mBTReady;

    private TextView mDataTemp;
    private TextView mDataSoil;
    private TextView mDataRain;
    private TextView mDataFlow;
    private TextView mHistorySize;
    private TextView mConfigDate;
    private TextView mConfigLatitude;
    private TextView mConfigTime;
    private TextView mConfigArea;
    private Spinner mConfigAreaUnits;

    GoogleApiClient mGoogleApiClient = null;
    Location mLastLocation;
    final int REQUEST_CODE_LOCATION_PERMISSION = 1;

    private File mHistoryFile;
    private int mHistoryCount = 0;

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

        // Text views for displaying sensor values
        mDataTemp = (TextView) findViewById(R.id.data_sensor_temp);
        mDataSoil = (TextView) findViewById(R.id.data_sensor_soil);
        mDataRain = (TextView) findViewById(R.id.data_sensor_rain);
        mDataFlow = (TextView) findViewById(R.id.data_flow_signal);

        // Text views for setting and displaying configuration values
        mConfigDate = (TextView) findViewById(R.id.input_date);
        mConfigLatitude = (TextView) findViewById(R.id.input_latitude);
        mConfigTime = (TextView) findViewById(R.id.input_time);
        mConfigArea = (TextView) findViewById(R.id.input_area);
        mConfigAreaUnits = (Spinner) findViewById(R.id.spinner_area);

        // "Download History" button contains number of days field and must be editable
        mHistorySize = (Button) findViewById(R.id.button_download);

        // Number of days of history is initially unknown and is displayed as "?"
        Resources res = getResources();
        String history_text = String.format(res.getString(R.string.string_download), "?");
        mHistorySize.setText(history_text);

        Intent intent = getIntent();
        if (intent.hasExtra(EXTRAS_DEVICE_NAME))
        {
            // Retrieve connected device's name from scanning activity
            mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
            TextView info_device_name = (TextView) findViewById(R.id.info_device_name);
            info_device_name.setText(mDeviceName);
        }

        if (intent.hasExtra(EXTRAS_DEVICE_ADDRESS))
        {
            // Retrieve connected device's address from scanning activity
            mDeviceAddr = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);
            TextView info_device_name = (TextView) findViewById(R.id.info_device_address);
            info_device_name.setText(mDeviceAddr);
        }

        if (intent.hasExtra(EXTRAS_DEVICE))
        {
            // Retrieve device structure itself from scanning activity
            mBTDevice = intent.getParcelableExtra(EXTRAS_DEVICE);
        }

        // Initially disconnected
        mBTStatus = BluetoothProfile.STATE_DISCONNECTED;
    }

    @Override
    public void onConnected(Bundle connectionHint)
    {
        /* New permissions model for API 23 and beyond only */
        /* Need location permissions for getting latitude */
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
    protected void onResume()
    {
        super.onResume();

        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);

        BluetoothAdapter mBTAdapter = bluetoothManager.getAdapter();

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
        super.onStart();
        mGoogleApiClient.connect();     // For retrieving latitude
    }

    protected void onStop()
    {
        super.onStop();
        mGoogleApiClient.disconnect();
    }

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback()
    {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState)
        {
            mBTStatus = newState;
            if (mBTStatus == BluetoothProfile.STATE_CONNECTED)
            {
                mBTGatt.discoverServices();
            }
            else if (mBTStatus == BluetoothProfile.STATE_DISCONNECTED)
            {
                mBTReady = false;
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
                    mBTCharDate = service.getCharacteristic(UUID.fromString(BLE_UUID_CHAR_DATE));
                    mBTCharTime = service.getCharacteristic(UUID.fromString(BLE_UUID_CHAR_TIME));
                    mBTCharArea = service.getCharacteristic(UUID.fromString(BLE_UUID_CHAR_AREA));
                    mBTCharFlood = service.getCharacteristic(UUID.fromString(BLE_UUID_CHAR_FLOOD));
                    mBTCharSensorSignal = service.getCharacteristic(UUID.fromString(BLE_UUID_CHAR_SENSOR_SIGNAL));
                    mBTCharLatitude = service.getCharacteristic(UUID.fromString(BLE_UUID_CHAR_LATITUDE));
                    mBTCharTemp = service.getCharacteristic(UUID.fromString(BLE_UUID_CHAR_TEMP));
                    mBTCharSoil = service.getCharacteristic(UUID.fromString(BLE_UUID_CHAR_SOIL));
                    mBTCharRain = service.getCharacteristic(UUID.fromString(BLE_UUID_CHAR_RAIN));
                    mBTCharFlow = service.getCharacteristic(UUID.fromString(BLE_UUID_CHAR_FLOW));
                    mBTCharHistorySignal = service.getCharacteristic(UUID.fromString(BLE_UUID_CHAR_HISTORY_SIGNAL));
                    mBTCharHistorySize = service.getCharacteristic(UUID.fromString(BLE_UUID_CHAR_HISTORY_SIZE));

                    // History characteristic needs the indication property so that it knows when
                    // a new history packet is available for downloading
                    BluetoothGattCharacteristic mBTCharHistory = service.getCharacteristic(UUID.fromString(BLE_UUID_CHAR_HISTORY));
                    mBTGatt.setCharacteristicNotification(mBTCharHistory, true);
                    mHistoryDescriptor = mBTCharHistory.getDescriptor(UUID.fromString(BLE_UUID_CHAR_NOTIFY_DESC));
                    mHistoryDescriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);

                    // Only one descriptor may be written to at a time; any others written will be
                    // lost, so History's descriptor is written first
                    mBTGatt.writeDescriptor(mHistoryDescriptor);
                }
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status)
        {
            if (descriptor == mHistoryDescriptor)
            {
                // History's descriptor has been written and now HistorySize's descriptor can be
                mBTGatt.setCharacteristicNotification(mBTCharHistorySize, true);
                mHistorySizeDescriptor = mBTCharHistorySize.getDescriptor(UUID.fromString(BLE_UUID_CHAR_NOTIFY_DESC));
                mHistorySizeDescriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                mBTGatt.writeDescriptor(mHistorySizeDescriptor);
            }
            else if (descriptor == mHistorySizeDescriptor)
            {
                // Bluetooth fully set up, read the last sampled sensor values immediately
                mBTGatt.readCharacteristic(mBTCharTemp);

                mBTReady = true;
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

                        // Once read, each characteristic calls a read of the next characteristic in
                        // its sequence and the return of that read calls the next and so on, a
                        // result of how Android handles reading and writing of characteristics
                        // Sensors: Temp -> Soil -> Rain -> Flow
                        // Config: Latitude -> Date -> Time -> Area
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

                            mBTGatt.readCharacteristic(mBTCharFlow);
                        }
                        else if ( BLE_UUID_CHAR_FLOW.equalsIgnoreCase(characteristic.getUuid().toString()) )
                        {
                            String text;

                            if (char_string.equals("1"))
                            {
                                text = "On";
                                mDataFlow.setText(text);
                            }
                            else if (char_string.equals("0"))
                            {
                                text = "Off";
                                mDataFlow.setText(text);
                            }
                            else
                            {
                                text = "Error";
                                mDataFlow.setText(text);
                            }
                        }
                        else if ( BLE_UUID_CHAR_LATITUDE.equalsIgnoreCase(characteristic.getUuid().toString()) )
                        {
                            // Latitude value in characteristic is multiplied by 100 due to the
                            // BL600's lack of float support, so it needs to be converted back
                            // to a float for viewing
                            float latitude = Float.valueOf(char_string);
                            latitude /= 100;

                            String latitude_text = String.valueOf(latitude);
                            mConfigLatitude.setText(latitude_text);

                            mBTGatt.readCharacteristic(mBTCharDate);
                        }
                        else if ( BLE_UUID_CHAR_DATE.equalsIgnoreCase(characteristic.getUuid().toString()) )
                        {
                            mConfigDate.setText(char_string);

                            mBTGatt.readCharacteristic(mBTCharTime);
                        }
                        else if ( BLE_UUID_CHAR_TIME.equalsIgnoreCase(characteristic.getUuid().toString()) )
                        {
                            mConfigTime.setText(char_string);

                            mBTGatt.readCharacteristic(mBTCharArea);
                        }
                        else if ( BLE_UUID_CHAR_AREA.equalsIgnoreCase(characteristic.getUuid().toString()) )
                        {
                            // Area value in characteristic is multiplied by 100 due to the
                            // BL600's lack of float support, so it needs to be converted back
                            // to a float for viewing
                            float area = Float.valueOf(char_string);
                            area /= 100;

                            // Convert to feet if necessary
                            if (mConfigAreaUnits.getSelectedItem().toString().equals("sq. ft"))
                            {
                                area *= SQ_METER_TO_SQ_FT;
                            }

                            String area_text = String.valueOf(area);
                            mConfigArea.setText(area_text);
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
                // Once written, each characteristic calls a write of the next characteristic in
                // its sequence and the return of that write calls the next and so on, a
                // result of how Android handles reading and writing of characteristics
                // Latitude -> Date -> Time -> Area
                if ( BLE_UUID_CHAR_LATITUDE.equalsIgnoreCase(characteristic.getUuid().toString()))
                {
                    mBTCharDate.setValue(mConfigDate.getText().toString());
                    mBTGatt.writeCharacteristic(mBTCharDate);
                }
                else if ( BLE_UUID_CHAR_DATE.equalsIgnoreCase(characteristic.getUuid().toString()) )
                {
                    mBTCharTime.setValue(mConfigTime.getText().toString());
                    mBTGatt.writeCharacteristic(mBTCharTime);
                }
                else if ( BLE_UUID_CHAR_TIME.equalsIgnoreCase(characteristic.getUuid().toString()) )
                {
                    float area = Float.valueOf(mConfigArea.getText().toString());

                    // BL600 expects meters
                    if (mConfigAreaUnits.getSelectedItem().toString().equals("sq. ft"))
                    {
                        area *= SQ_FT_TO_SQ_METER;
                    }

                    // Minimum supported area is 1 square meter
                    if (area < 1)
                    {
                        area = 1;
                    }

                    // Area needs to be multiplied by 100 because BL600 cannot deal in floats
                    area *= 100;

                    int area_in_meters = Math.round(area); // Get rid of trailing decimal point
                    mBTCharArea.setValue(String.valueOf(area_in_meters));

                    mBTGatt.writeCharacteristic(mBTCharArea);
                }

                // Device has been told to update all of its sensor values, now read them by
                // initiating first in chain of sensor reads
                else if (BLE_UUID_CHAR_SENSOR_SIGNAL.equalsIgnoreCase(characteristic.getUuid().toString()))
                {
                    mBTGatt.readCharacteristic(mBTCharTemp);
                }
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic)
        {
            final String char_string = characteristic.getStringValue(0);

            // A value has been written to the History characteristic and must be downloaded
            if ( BLE_UUID_CHAR_HISTORY.equalsIgnoreCase(characteristic.getUuid().toString()))
            {
                try {
                    FileOutputStream stream = new FileOutputStream(mHistoryFile, true);
                    stream.write(char_string.getBytes());
                    ++mHistoryCount;

                    // After every 8 items (one day of data), write a newline, otherwise write a comma
                    if (mHistoryCount == 8)
                    {
                        stream.write("\n".getBytes());
                        mHistoryCount = 0;
                    }
                    else
                    {
                        stream.write(",".getBytes());
                    }

                    stream.close();
                } catch (IOException e) {
                    Log.e("Smart", "File not found");
                }
            }

            // A new day of history is available for download so the button must be updated
            else if ( BLE_UUID_CHAR_HISTORY_SIZE.equalsIgnoreCase(characteristic.getUuid().toString()))
            {
                Resources res = getResources();
                String history_text = String.format(res.getString(R.string.string_download), char_string);
                mHistorySize.setText(history_text);
            }
        }
    };

    public void onClickConnect(View view)
    {
        Intent intent_device_scan = new Intent(this, DeviceScanActivity.class);
        startActivity(intent_device_scan);
    }

    public void onClickConfigWrite(View view)
    {
        if (!mBTReady)
        {
            Toast.makeText(this, "Not connected to a device", Toast.LENGTH_SHORT).show();
        }
        else
        {
            // Latitude needs to be multiplied by 100 because BL600 cannot deal in floats
            float latitude = Float.parseFloat(mConfigLatitude.getText().toString());
            latitude *= 100;

            int latitude_rounded = Math.round(latitude); // Get rid of trailing decimal point
            mBTCharLatitude.setValue(String.valueOf(latitude_rounded));

            // Start chain of config characteristic writes
            mBTGatt.writeCharacteristic(mBTCharLatitude);
        }
    }

    public void onClickConfigRead(View view)
    {
        if (!mBTReady)
        {
            Toast.makeText(this, "Not connected to a device", Toast.LENGTH_SHORT).show();
        }
        else
        {
            // Start chain of config characteristic reads
            mBTGatt.readCharacteristic(mBTCharLatitude);
        }
    }

    public void onClickReadSensors(View view)
    {
        if (!mBTReady)
        {
            Toast.makeText(this, "Not connected to a device", Toast.LENGTH_SHORT).show();
        }
        else
        {
            // Tell device we want immediate sensor values
            mBTCharSensorSignal.setValue("1");
            mBTGatt.writeCharacteristic(mBTCharSensorSignal);
        }
    }

    public void onClickFlood(View view)
    {
        if (!mBTReady)
        {
            Toast.makeText(this, "Not connected to a device", Toast.LENGTH_SHORT).show();
        }
        else
        {
            mBTCharFlood.setValue("1");
            mBTGatt.writeCharacteristic(mBTCharFlood);
        }
    }

    public void onClickGetLatitude(View view)
    {
        EditText input_latitude = (EditText) findViewById(R.id.input_latitude);

        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (mLastLocation == null)
        {
            Toast.makeText(this, "Unable to retrieve location", Toast.LENGTH_SHORT).show();
        }
        else
        {
            double latitude = mLastLocation.getLatitude();
            DecimalFormat decimal_format = new DecimalFormat("#.##");
            latitude = Double.valueOf(decimal_format.format(latitude));

            input_latitude.setText(String.valueOf(latitude));
        }
    }

    public void onClickDownloadHistory(View view)
    {
        if (!mBTReady)
        {
            Toast.makeText(this, "Not connected to a device", Toast.LENGTH_SHORT).show();
        }
        else
        {
            // Create file for storing values with timestamped name
            Calendar calendar = Calendar.getInstance();
            SimpleDateFormat date_format = new SimpleDateFormat("MMddyykkmss", Locale.US);
            String timestamp = date_format.format(calendar.getTime());

            String filename = timestamp + ".csv";

            File mHistoryFolder = this.getExternalFilesDir(null);
            mHistoryFile = new File(mHistoryFolder, filename);

            mBTCharHistorySignal.setValue("1");
            mBTGatt.writeCharacteristic(mBTCharHistorySignal);
        }
    }

    public void onClickGetDate(View view)
    {
        EditText input_date = (EditText) findViewById(R.id.input_date);

        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat date_format = new SimpleDateFormat("MMddyy", Locale.US);
        String date = date_format.format(calendar.getTime());

        input_date.setText(date);
    }

    public void onClickGetTime(View view)
    {
        EditText input_time = (EditText) findViewById(R.id.input_time);

        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat time_format = new SimpleDateFormat("kkmmss", Locale.US);
        String time = time_format.format(calendar.getTime());

        input_time.setText(time);
    }

    @Override
    public void onConnectionSuspended(int param)
    {
        // Stub, required but unused
    }

    @Override
    public void onConnectionFailed(ConnectionResult result)
    {
        // Stub, required but unused
    }
}
