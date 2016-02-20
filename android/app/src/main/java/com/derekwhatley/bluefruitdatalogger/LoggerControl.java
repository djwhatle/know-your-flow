package com.derekwhatley.bluefruitdatalogger;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.derekwhatley.bluefruitcontroller.R;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;


public class LoggerControl extends Activity {
    //controls
    private TextView mTxtMAC;
    private ToggleButton mBluetoothToggle;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothSocket mBluetoothSocket;
    private ToggleButton mBtnContinuousCapture;
    private Button mBtnSingleCapture;
    private Button mBtnClearLogging;
    private ListView mListViewResponses;
    ArrayAdapter<String> dataItemsAdapter;
    ArrayList<SensorData> arrayOfSensorData;
    SensorDataAdapter adapter;

    //vars
    private InputStream bluetoothInputStream;


    //vars from read code
    private boolean stopWorker;
    InputStream mmInputStream;
    Thread workerThread;
    byte[] readBuffer;
    int readBufferPosition;

    //callbacks
    final int REQUEST_ENABLE_BT = 1;

    //constants
    final String DEVICE_IDENTIFIER = "022f";


    final String SPP_UUID = "00001101-0000-1000-8000-00805F9B34FB";

    private class StartSerialOverBT extends AsyncTask<BluetoothAdapter, Void, Boolean> {
        protected void onPreExecute() {
            mBluetoothToggle.setEnabled(false); //disable the toggle while task running
        }

        protected Boolean doInBackground(BluetoothAdapter... params) {
            Set<BluetoothDevice> pairedDevices = params[0].getBondedDevices();
            return getSerialSocket(pairedDevices);
        }

        protected void onPostExecute(Boolean result) {
            mBluetoothToggle.setEnabled(true); //re-enable the toggle when finished
            if(result == true) {
                mTxtMAC.setText(getMACAddress());
                mBluetoothToggle.setChecked(true);
                mBtnContinuousCapture.setEnabled(true);
                mBtnSingleCapture.setEnabled(true);
                Toast.makeText(getBaseContext(), "Connected", Toast.LENGTH_SHORT).show();
                beginListenForData();
            }
            else {
                Toast.makeText(getBaseContext(), "Connection failed", Toast.LENGTH_SHORT).show();
                mBluetoothToggle.setChecked(false);
                mBtnContinuousCapture.setEnabled(false);
                mBtnSingleCapture.setEnabled(false);
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.datalog_control);

        mTxtMAC = (TextView) findViewById(R.id.txtMAC);
        mBluetoothToggle = (ToggleButton) findViewById(R.id.btnBluetoothToggle);
        mBtnContinuousCapture = (ToggleButton) findViewById(R.id.btnContinuousCapture);
        mBtnSingleCapture = (Button) findViewById(R.id.btnSingleCapture);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mListViewResponses = (ListView) findViewById(R.id.lstViewResponses);
        mBtnClearLogging = (Button) findViewById(R.id.btnClearLogging);

        //attach view adapter
        // Construct the data source
        arrayOfSensorData = new ArrayList<SensorData>();
        // Create the adapter to convert the array to views
        adapter = new SensorDataAdapter(this, arrayOfSensorData);
        // Attach the adapter to a ListView
        mListViewResponses.setAdapter(adapter);

        if (mBluetoothSocket != null) {
            if (mBluetoothAdapter.isEnabled()) {
                StartSerialOverBT SerialStarter = new StartSerialOverBT();
                SerialStarter.execute(mBluetoothAdapter);
            }
        }

        mBtnClearLogging.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
               adapter.clear();
            }
        });

        mBtnContinuousCapture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mBtnContinuousCapture.isChecked()) {
                    mBtnSingleCapture.setEnabled(false);
                    sendBTData("start\n".getBytes());
                }
                else {
                    mBtnSingleCapture.setEnabled(true);
                    sendBTData("stop\n".getBytes());
                }

            }
        });

        mBtnSingleCapture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendBTData("single\n".getBytes());
            }
        });

        mBluetoothToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

                if (mBluetoothToggle.isChecked()) {
                    if (!mBluetoothAdapter.isEnabled()) {
                        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                    } else {
                        //adapter is enabled, Serial Connection is not.
                        Toast.makeText(getBaseContext(), "Connecting", Toast.LENGTH_SHORT).show();
                        StartSerialOverBT SerialStarter = new StartSerialOverBT();
                        SerialStarter.execute(mBluetoothAdapter);
                    }
                } else {
                    //user wants to turn off connection
                    try {
                        sendBTData("stop\n".getBytes());
                        stopWorker = true;
                        mmInputStream.close();
                        mBluetoothSocket.close();
                        mTxtMAC.setText("n/a");
                        mBtnContinuousCapture.setEnabled(false);
                        mBtnContinuousCapture.setChecked(false);
                        mBtnSingleCapture.setEnabled(false);
                    } catch (IOException e) {
                        //not much to be done
                        Toast.makeText(getBaseContext(), "Failed to close connection.", Toast.LENGTH_SHORT).show();
                        //don't change the button state if we failed to connect
                        mBluetoothToggle.setChecked(true);
                        mBtnContinuousCapture.setEnabled(true);
                        mBtnSingleCapture.setEnabled(true);
                    }
                    mBluetoothToggle.setEnabled(true);
                }
            }
        });
    }

    private void sendBTData(byte[] data) {

        if(mBluetoothSocket != null) {
            if(mBluetoothSocket.isConnected()) {
                try {
                    OutputStream btOut = mBluetoothSocket.getOutputStream();
                    btOut.write(data);
                    //btOut.close();
                }
                catch (IOException e) {
                    //not much to do here
                    System.out.println("failed to send data :: " + e.getMessage());
                }
            }
        }
    }

    void beginListenForData()
    {
        final Handler handler = new Handler();
        final byte delimiter = 10; //This is the ASCII code for a newline character

        stopWorker = false;
        readBufferPosition = 0;
        readBuffer = new byte[1024];
        workerThread = new Thread(new Runnable()
        {
            public void run()
            {
                while(!Thread.currentThread().isInterrupted() && !stopWorker)
                {
                    try
                    {
                        int bytesAvailable = mmInputStream.available();
                        if(bytesAvailable > 0)
                        {
                            byte[] packetBytes = new byte[bytesAvailable];
                            mmInputStream.read(packetBytes);
                            for(int i=0;i<bytesAvailable;i++)
                            {
                                byte b = packetBytes[i];
                                if(b == delimiter)
                                {
                                    byte[] encodedBytes = new byte[readBufferPosition];
                                    System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                    final String data = new String(encodedBytes, "US-ASCII");
                                    readBufferPosition = 0;

                                    handler.post(new Runnable()
                                    {
                                        public void run()
                                        {
                                            //mTxtMAC.setText(data);
                                            SensorData sensorData = new SensorData(data, "");
                                            adapter.add(sensorData);
                                            mListViewResponses.smoothScrollToPosition(adapter.getCount());
                                        }
                                    });
                                }
                                else
                                {
                                    readBuffer[readBufferPosition++] = b;
                                }
                            }
                        }
                    }
                    catch (IOException ex)
                    {
                        stopWorker = true;
                    }
                }
            }
        });

        workerThread.start();
    }


    private String getMACAddress() {
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();

        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                if(device.getName().contains(DEVICE_IDENTIFIER)) {
                    return device.getAddress();
                }
            }
        }
        return "";
    }

    private boolean getSerialSocket(Set<BluetoothDevice> pairedDevices) {
        // If there are paired devices
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                String name = device.getName();
                if(device.getName().contains(DEVICE_IDENTIFIER)) {
                    try {
                        mBluetoothSocket = device.createRfcommSocketToServiceRecord(UUID.fromString(SPP_UUID));
                        mmInputStream = mBluetoothSocket.getInputStream();
                    }
                    catch (IOException e) {
                        //no handling for now.
                        return false;
                    }
                    try {
                        mBluetoothSocket.connect();
                        return true;
                    }
                    catch (IOException e) {
                        //no handling for now
                        return false;
                    }
                }
            }
        }
        return false;
    }


    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == REQUEST_ENABLE_BT) {
            if(resultCode == RESULT_OK){
                Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
                StartSerialOverBT SerialStarter = new StartSerialOverBT();
                SerialStarter.execute(mBluetoothAdapter);
                //Toast.makeText(getBaseContext(), "Bluetooth enabled", Toast.LENGTH_SHORT).show();
                //mBluetoothToggle.setEnabled(true);
            }
            if (resultCode == RESULT_CANCELED) {
                Toast.makeText(getBaseContext(), "Bluetooth enable failed", Toast.LENGTH_SHORT).show();
                mBluetoothToggle.setEnabled(true);
            }
        }
    }

    public class SensorDataAdapter extends ArrayAdapter<SensorData> {
        public SensorDataAdapter(Context context, ArrayList<SensorData> SensorDataPoints) {
            super(context, 0, SensorDataPoints);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // Get the data item for this position
            SensorData sensorData = getItem(position);
            // Check if an existing view is being reused, otherwise inflate the view
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_sensor_data, parent, false);
            }
            // Lookup view for data population
            TextView tvValue1 = (TextView) convertView.findViewById(R.id.tvValue1);
            TextView tvValue2 = (TextView) convertView.findViewById(R.id.tvValue2);
            // Populate the data into the template view using the data object
            tvValue1.setText(sensorData.value1);
            tvValue2.setText(sensorData.value2);
            // Return the completed view to render on screen
            return convertView;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.datalog_control, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
