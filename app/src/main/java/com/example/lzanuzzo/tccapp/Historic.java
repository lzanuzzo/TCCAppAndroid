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
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import static android.bluetooth.BluetoothAdapter.ACTION_REQUEST_ENABLE;

public class Historic extends AppCompatActivity {

    private String TAG = Historic.class.getSimpleName();

    Integer REQ_BT_ENABLE=1;
    Integer BT_AC_FLAG = 1;

    private ProgressDialog pDialogHistoric;
    private ListView listViewHistoric;

    BluetoothSocket mmSocket = null;
    BluetoothDevice mmDevice = null;
    BluetoothAdapter mBluetoothAdapter = null;
    OutputStream mmOutputStream;
    InputStream mmInputStream;

    ArrayList<HashMap<String, String>> readList;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_historic);


        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(mBluetoothAdapter == null)
        {
            Log.d(TAG,"Adapter null");
            errorAlertDialog("No bluetooth adapter available ...");
        }
        else {
            BT_AC_FLAG = 1;
            Log.d(TAG,"Adapter available");
            if (!mBluetoothAdapter.isEnabled()) {
                Log.d(TAG,"Enable adapter");
                Intent enableBluetooth = new Intent(ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBluetooth, 0);
            }else
            {
                Log.d(TAG,"Adapter Already Enabled");
                Intent enableBluetooth = new Intent(ACTION_REQUEST_ENABLE);
                onActivityResult(1,-1,enableBluetooth);
            }
        }
    }

    private class getReadList extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // Showing progress dialog
            pDialogHistoric = new ProgressDialog(Historic.this);
            pDialogHistoric.setMessage("Please wait...");
            pDialogHistoric.setCancelable(true);
            pDialogHistoric.show();
        }

        @Override
        protected Void doInBackground(Void... arg0) {
            HttpHandler serviceHandler = new HttpHandler();

            String jsonStr = "";
            Boolean HistoricGet = true;
            Boolean BeginRead = false;
            final byte delimiter = 10; //This is the ASCII code for a newline character
            byte[] readBuffer = new byte[1024];
            int readBufferPosition = 0;
            List<String> historicalStrings = new ArrayList<>();
            try {
                if (!mmSocket.isConnected()) {

                    Log.d(TAG,"mmSocket not connected, connecting...");
                    mmSocket.connect();

                }
                //mmOutputStream = mmSocket.getOutputStream();
                mmInputStream = mmSocket.getInputStream();
                while(HistoricGet) {
                    try {
                        int bytesAvailable = mmInputStream.available();
                        readBufferPosition = 0;
                        if (bytesAvailable > 0) {
                            byte[] packetBytes = new byte[bytesAvailable];
                            mmInputStream.read(packetBytes);
                            for (int i = 0; i < bytesAvailable; i++) {
                                byte b = packetBytes[i];
                                if (b == delimiter) {
                                    byte[] encodedBytes = new byte[readBufferPosition];
                                    System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                    final String data = new String(encodedBytes, "US-ASCII");
                                    readBufferPosition = 0;
                                    //Log.d(TAG,data);
                                    if(Objects.equals(data ,getResources().getString(R.string.begin_historic_data))){
                                        Log.d(TAG,"Find initial string for historic");
                                        BeginRead = true;
                                    }
                                    else if(Objects.equals(data,getResources().getString(R.string.end_historic_data))){
                                        Log.d(TAG,"Find final string for historic");
                                        if(BeginRead == true){
                                            HistoricGet = false;
                                        }
                                        BeginRead = false;
                                    }
                                    if(BeginRead == true && !Objects.equals(data,getResources().getString(R.string.begin_historic_data))){
                                        historicalStrings.add(data);
                                    }

                                }else {
                                    readBuffer[readBufferPosition++] = b;
                                }
                            }

                        }
                    }
                    catch (IOException e) {
                        Log.e(TAG,"Error reading data, work done");
                        Log.e(TAG,e.toString());
                        HistoricGet = false;
                    }
                    if (!HistoricGet){
                        try {
                            Log.d(TAG,"Work is done, closing the socket");
                            //String msg = "h";
                            //mmOutputStream.write(msg.getBytes());
                            mmSocket.close();

                        } catch (IOException e) {
                            Log.e(TAG,"mmSocket not connected, connecting...");
                            Log.e(TAG,e.toString());
                            e.printStackTrace();
                        }
                        break;
                    }

                }
            } catch (IOException e) {
                Log.e(TAG,"Error reading data, work done");
                Log.e(TAG,e.toString());

            }

            String dataString;
            if(!historicalStrings.isEmpty()){


                for (int i=0;i < historicalStrings.size();i++)
                {
                    dataString = historicalStrings.get(i);
                    String[] dataStringParts = dataString.split(",");
                    String litersStr = dataStringParts[3];
                    String start_dateStr = dataStringParts[4];
                    String end_dateStr = dataStringParts[5];
                    String id = dataStringParts[0];

                    HashMap<String, String> historicListItem = new HashMap<>();
                    historicListItem.put("liters",litersStr);
                    historicListItem.put("start_date",start_dateStr);
                    historicListItem.put("end_date",end_dateStr);
                    historicListItem.put("id",id);

                    readList.add(historicListItem);
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            // Dismiss the progress dialog
            if (pDialogHistoric.isShowing())
                pDialogHistoric.dismiss();
            /**
             * Updating parsed JSON data into ListView
             * */
            ListAdapter adapter = new SimpleAdapter(
                    Historic.this, readList,
                    R.layout.list_item, new String[]
                    {   "liters",
                        "start_date",
                        "end_date",
                        "id"
                    }, new int[]
                    {
                        R.id.textViewListLiters,
                        R.id.textViewListStartDate,
                        R.id.textViewListEndDate,
                        R.id.textViewListId
                    });

            listViewHistoric.setAdapter(adapter);
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQ_BT_ENABLE) {
            if (resultCode == RESULT_OK) {
                Log.d(TAG, "BlueTooth is now Enabled");
                Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
                if (pairedDevices.size() > 0) {
                    for (BluetoothDevice device : pairedDevices) {
                        if (device.getName().equals("Rasp Wiki"))
                        {
                            Log.d(TAG, "Device paired: "+device.getName());
                            mmDevice = device;
                            Log.d(TAG, "Device assigned ");
                            UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); //Standard SerialPortService ID
                            Log.d(TAG, "UUID assigned ");

                            try {
                                String msg = "h";

                                mmSocket = mmDevice.createRfcommSocketToServiceRecord(uuid);
                                Log.d(TAG,"mmSocket Created");
                                mmSocket.connect();
                                Log.d(TAG,"mmSocket Connected");
                                OutputStream mmOutputStream = mmSocket.getOutputStream();
                                Log.d(TAG,"mmOutputStream created");
                                mmOutputStream.write(msg.getBytes());
                                Log.d(TAG,"Msg Send 'h' (Historic)");

                                readList = new ArrayList<>();
                                listViewHistoric = (ListView) findViewById(R.id.ListViewHistoric);
                                listViewHistoric.setLongClickable(true);
                                listViewHistoric.setOnItemClickListener(new AdapterView.OnItemClickListener()
                                {
                                    @Override
                                    public void onItemClick(AdapterView<?> arg0, View arg1,int position, long arg3)
                                    {
                                        String positionContent = listViewHistoric.getItemAtPosition(position).toString();
                                        Toast.makeText(Historic.this, "Position: " + positionContent, Toast.LENGTH_SHORT).show();
                                    }
                                });
                                listViewHistoric.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                                    @Override
                                    public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int position, long id) {

                                        String positionContent = listViewHistoric.getItemAtPosition(position).toString();
                                        Toast.makeText(Historic.this, "Position: " + positionContent, Toast.LENGTH_SHORT).show();

                                        return true;
                                    }
                                });
                                new getReadList().execute();

                            } catch (IOException e) {
                                Log.e(TAG,"Error trying to create mmSocket");
                                Log.e(TAG,e.toString());
                                new AlertDialog.Builder(this)
                                        .setTitle("Error")
                                        .setMessage("Error trying to connect with the raspberry!")
                                        .setIcon(android.R.drawable.ic_dialog_alert)
                                        .setOnCancelListener(new DialogInterface.OnCancelListener() {
                                            @Override
                                            public void onCancel(DialogInterface dialog) {
                                                finish();
                                            }
                                        })
                                        .show();

                                e.printStackTrace();
                            }

                        }
                    }

                }
            }
        }
        if(resultCode == RESULT_CANCELED){
            Log.d(TAG, "Cant enable bluetooth!! Finishing the Historical activity");
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
    protected void onResume() {
        super.onResume();
        Log.d(TAG,"OnResume");
        if(BT_AC_FLAG!=1){
            Intent enableBluetooth = new Intent(ACTION_REQUEST_ENABLE);
            BT_AC_FLAG = 1;
            onActivityResult(1,-1,enableBluetooth);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        BT_AC_FLAG = 0;
    }

    public void errorAlertDialog(String errorMsg){
        new AlertDialog.Builder(Historic.this)
                .setTitle("Error")
                .setMessage(errorMsg)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

}
