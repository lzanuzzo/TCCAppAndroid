package com.example.lzanuzzo.tccapp;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

import static android.bluetooth.BluetoothAdapter.ACTION_REQUEST_ENABLE;

public class Config extends AppCompatActivity {

    private String TAG = Config.class.getSimpleName();
    Integer REQ_BT_ENABLE=1;

    BluetoothSocket mmSocket = null;
    BluetoothDevice mmDevice = null;
    BluetoothAdapter mBluetoothAdapter = null;
    OutputStream mmOutputStream;
    InputStream mmInputStream;

    Integer BT_AC_FLAG = 1;

    Button shutdownButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config);

        Log.d(TAG,"On create Called");

        shutdownButton = (Button) findViewById(R.id.buttonShutdown);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(mBluetoothAdapter == null)
        {
            Log.d(TAG,"Adapter null");
            errorAlertDialog("No bluetooth adapter available ...");
        }
        else {
            BT_AC_FLAG = 1;
            Log.d(TAG, "Adapter available");
            if (!mBluetoothAdapter.isEnabled()) {
                Log.d(TAG, "Enable adapter");
                Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBluetooth, 0);
            }else
            {
                Log.d(TAG,"Adapter Already Enabled");
                Intent enableBluetooth = new Intent(ACTION_REQUEST_ENABLE);
                onActivityResult(1,-1,enableBluetooth);
            }
        }

        shutdownButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                shutdownRasp();
            }
        });
    }

    void shutdownRasp(){
        final AsyncTask<BluetoothSocket, Void, Boolean> resetAsyncTask = new AsyncTask<BluetoothSocket, Void, Boolean>() {
            private ProgressDialog dialog;

            @Override
            protected void onPreExecute()
            {
                this.dialog = new ProgressDialog(Config.this);
                this.dialog.setMessage("Trying to send shutdown command...");
                this.dialog.setCancelable(true);
                this.dialog.setOnCancelListener(new DialogInterface.OnCancelListener()
                {
                    @Override
                    public void onCancel(DialogInterface dialog)
                    {
                        cancel(false);
                    }
                });

                this.dialog.show();

            }

            @Override
            protected Boolean doInBackground(BluetoothSocket... params)
            {
                Log.d(TAG,"Shutdown raspberry pressed");
                if(mmSocket!=null){
                    Log.d(TAG,"mmSocket available");
                    try {
                        if (!mmSocket.isConnected() ){
                            Log.d(TAG,"mmSocket not connected, connecting...");
                            mmSocket.connect();
                        }
                        String msg = "s";

                        mmOutputStream = mmSocket.getOutputStream();
                        Log.d(TAG,"mmOutputStream created");
                        mmOutputStream.write(msg.getBytes());
                        Log.d(TAG,"Msg Send 's' (shutdown)");
                    } catch (IOException e) {
                        Log.e(TAG,"Exception trying to shutdown raspberry");
                        Log.e(TAG,e.toString());
                        return false;
                    }
                }
                return true;
            }

            @Override
            protected void onPostExecute(Boolean result)
            {
                if(result == false){
                    errorAlertDialog("Error trying to shutdown raspberry!");
                }
                //called on ui thread
                if (this.dialog != null) {
                    this.dialog.dismiss();
                }
            }

            @Override
            protected void onCancelled()
            {
                //called on ui thread
                if (this.dialog != null) {
                    this.dialog.dismiss();
                }
            }
        };
        resetAsyncTask.execute();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQ_BT_ENABLE) {
            if (resultCode == RESULT_OK) {
                Log.d(TAG, "BlueTooth is now Enabled");


                final AsyncTask<BluetoothSocket, Void, Boolean> beginBluetoothConnection = new AsyncTask<BluetoothSocket, Void, Boolean>() {
                    private ProgressDialog dialog;

                    @Override
                    protected void onPreExecute() {
                        this.dialog = new ProgressDialog(Config.this);
                        this.dialog.setMessage("Attempting to pair with the device...");
                        this.dialog.setCancelable(true);
                        this.dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                            @Override
                            public void onCancel(DialogInterface dialog) {
                                // cancel AsyncTask
                                cancel(false);
                            }
                        });
                        this.dialog.show();
                    }

                    @Override
                    protected Boolean doInBackground(BluetoothSocket... params)
                    {
                        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
                        if (pairedDevices.size() > 0) {
                            for (BluetoothDevice device : pairedDevices) {
                                if (device.getName().equals("Rasp Wiki")) {
                                    Log.d(TAG, "Device paired: " + device.getName());
                                    mmDevice = device;
                                    Log.d(TAG, "Device assigned ");
                                    UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); //Standard SerialPortService ID
                                    Log.d(TAG, "UUID assigned ");

                                    try {
                                        mmSocket = mmDevice.createRfcommSocketToServiceRecord(uuid);
                                        Log.d(TAG,"mmSocket Created");
                                        mmSocket.connect();
                                        Log.d(TAG,"mmSocket Connected");
                                        return true;

                                    }catch (IOException e) {
                                        Log.e(TAG,"Error trying to create mmSocket");
                                        Log.e(TAG,e.toString());
                                        e.printStackTrace();
                                        return false;
                                    }
                                }
                            }
                            return false;
                        }
                        return false;
                    }

                    @Override
                    protected void onPostExecute(Boolean result)
                    {
                        if(!result){
                            finish();
                        }
                        if (this.dialog != null) {
                            this.dialog.dismiss();
                        }
                    }

                };
                beginBluetoothConnection.execute();
            }
        }
        if(resultCode == RESULT_CANCELED){
            Log.d(TAG, "Cant enable bluetooth!! Finishing the activity");
            finish();
        }
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG,"OnPause");
        if(BT_AC_FLAG!=1){
            try {
                if(mmSocket!=null) {
                    mmSocket.close();
                    Log.d(TAG,"Socket Closed");
                }
            }
            catch (IOException e) {
                Log.e(TAG,"Error closing mmSocket...");
                Log.e(TAG,e.toString());
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        BT_AC_FLAG = 0;
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG,"OnResume");
        if(BT_AC_FLAG!=1){
            Intent enableBluetooth = new Intent(ACTION_REQUEST_ENABLE);
            BT_AC_FLAG = 1;
            onActivityResult(1,-1,enableBluetooth);
        }
    }

    public void errorAlertDialog(String errorMsg){
        new AlertDialog.Builder(Config.this)
                .setTitle("Error")
                .setMessage(errorMsg)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }
}
