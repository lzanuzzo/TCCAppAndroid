package com.example.lzanuzzo.tccapp;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.UUID;

import static android.bluetooth.BluetoothAdapter.ACTION_REQUEST_ENABLE;

public class Historic extends AppCompatActivity
implements ChartFragment.OnFragmentInteractionListener
{

    @Override
    public void onFragmentInteraction(Uri uri) {

    }

    private String TAG = Historic.class.getSimpleName();

    Integer REQ_BT_ENABLE=1;
    Integer BT_AC_FLAG = 1;

    ListAdapter adapterHistoric;

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

        readList = new ArrayList<>();
        listViewHistoric = (ListView) findViewById(R.id.ListViewHistoric);
        listViewHistoric.setLongClickable(true);
        listViewHistoric.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1,int position, long arg3)
            {
                String positionContent = listViewHistoric.getItemAtPosition(position).toString();
                Log.d(TAG,"Chart Fragment at position :"+positionContent);
                positionContent = positionContent.replaceAll("\\{id=", "");
                String[] content = positionContent.split(",");
                final String stringId = content[0];
                getChartData(stringId);
            }
        });
        listViewHistoric.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int position, long id) {
                String positionContent = listViewHistoric.getItemAtPosition(position).toString();
                positionContent = positionContent.replaceAll("\\{id=", "");
                String[] content = positionContent.split(",");
                final String stringId = content[0];

                new AlertDialog.Builder(Historic.this)
                        .setTitle("Delete")
                        .setMessage("Do you really want to delete this read?")
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                deleteRead(stringId);
                            }})
                        .setNegativeButton(android.R.string.no, null).show();
                return true;
            }
        });

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

            Boolean HistoricGet = true;
            Boolean BeginRead = false;
            final byte delimiter = 10; //This is the ASCII code for a newline character
            byte[] readBuffer = new byte[1024];
            int readBufferPosition = 0;
            List<String> historicalStrings = new ArrayList<>();
            try {
                if (mmSocket.isConnected()) {
                    mmInputStream = mmSocket.getInputStream();
                    while (HistoricGet) {
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

                                        if (data.equals("initialhistoric")) {
                                            Log.d(TAG, "Find initial string for historic");
                                            if (BeginRead == true) {
                                                HistoricGet = false;
                                            } else {
                                                BeginRead = true;
                                            }
                                        } else if (data.equals("finalhistoric")) {
                                            Log.d(TAG, "Find final string for historic");
                                            if (BeginRead == true) {
                                                HistoricGet = false;
                                            }
                                            BeginRead = false;
                                        }

                                        if (BeginRead == true && !data.equals("initialhistoric")) {
                                            historicalStrings.add(data);
                                        }

                                    } else {
                                        readBuffer[readBufferPosition++] = b;
                                    }
                                }

                            }
                        } catch (IOException e) {
                            Log.e(TAG, "Error reading data, work done");
                            Log.e(TAG, e.toString());
                            HistoricGet = false;
                        }
                        if (!HistoricGet) {
                            break;
                        }

                    }
                }
                else{
                    Log.d(TAG,"Socket is not connected for read historical data..");
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

                    String litersStr = String.format("%.2f ml",Float.parseFloat(dataStringParts[3]));
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
            adapterHistoric = new SimpleAdapter(
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

            listViewHistoric.setAdapter(adapterHistoric);
        }

    }

    private class getReadChart extends AsyncTask<String, Void, Boolean>{
        private ProgressDialog dialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            this.dialog = new ProgressDialog(Historic.this);
            this.dialog.setMessage("Attempting to pair with the device...");
            this.dialog.setCancelable(true);
            this.dialog.setOnCancelListener(new DialogInterface.OnCancelListener()
            {
                @Override
                public void onCancel(DialogInterface dialog)
                {
                    // cancel AsyncTask
                    cancel(false);
                }
            });
            this.dialog.show();
        }

        @Override
        protected Boolean doInBackground(String... params) {
            String id = params[0];
            Boolean ChartGet = true;
            Boolean BeginRead = false;
            final byte delimiter = 10; //This is the ASCII code for a newline character
            byte[] readBuffer = new byte[1024];
            int readBufferPosition = 0;
            List<String> chartStrings = new ArrayList<>();
            if (mmSocket.isConnected()) {
                while (ChartGet) {
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
                                    Log.d(TAG,data);

                                    if (data.equals("ini,"+id)) {
                                        Log.d(TAG, "Find ini string for chart");
                                        if (BeginRead == true) {
                                            ChartGet = false;
                                        } else {
                                            BeginRead = true;
                                        }
                                    } else if (data.equals("fin,"+id)) {
                                        Log.d(TAG, "Find fin string for chart");
                                        if (BeginRead == true) {
                                            ChartGet = false;
                                        }
                                        BeginRead = false;
                                    }

                                    if (BeginRead == true && !data.equals("ini,"+id)) {
                                        chartStrings.add(data);
                                    }

                                } else {
                                    readBuffer[readBufferPosition++] = b;
                                }
                            }

                        }

                    } catch (IOException e) {
                        Log.e(TAG, "Error reading data, work done");
                        Log.e(TAG, e.toString());
                        ChartGet = false;
                    }
                    if (!ChartGet) {
                        break;
                    }
                }
            }
            else
            {
                Log.d(TAG,"Socket is not connected to read chart data");
                return false;
            }

            if(!chartStrings.isEmpty()){
                Log.d(TAG,"ChartData");
                String xvalues = "";
                String yvalues = "";
                String[] splitedValues;
                for (int i=0;i < chartStrings.size();i++)
                {
                    Log.e(TAG,chartStrings.get(i));
                    splitedValues = chartStrings.get(i).split(",");
                    xvalues = xvalues+","+splitedValues[0];
                    yvalues = yvalues+","+splitedValues[2];
                }

                Fragment chart = ChartFragment.newInstance(xvalues,yvalues);
                FragmentManager fragmentManager = getSupportFragmentManager();
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                Log.d(TAG,"Before Commit");
                fragmentTransaction.replace(R.id.chart, chart).commit();
            }

            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);
            if (this.dialog != null) {
                this.dialog.dismiss();
            }
        }
    }

    public void getChartData(final String id){
        if (mmSocket.isConnected()) {

            final AsyncTask<String, Void, Boolean> beginChartLoop = new AsyncTask<String, Void, Boolean>() {

                private ProgressDialog dialog;

                @Override
                protected void onPreExecute()
                {
                    this.dialog = new ProgressDialog(Historic.this);
                    this.dialog.setMessage("Attempting to pair with the device...");
                    this.dialog.setCancelable(true);
                    this.dialog.setOnCancelListener(new DialogInterface.OnCancelListener()
                    {
                        @Override
                        public void onCancel(DialogInterface dialog)
                        {
                            // cancel AsyncTask
                            cancel(false);
                        }
                    });
                    this.dialog.show();
                }

                @Override
                protected Boolean doInBackground(String... params) {
                    String id = params[0];
                    try {
                        String msg = "c:"+id;
                        OutputStream mmOutputStream = mmSocket.getOutputStream();
                        Log.d(TAG,"mmOutputStream created");
                        mmOutputStream.write(msg.getBytes());
                        Log.d(TAG,"Msg Send 'c:"+id+"' (Historic)");
                        return true;
                    } catch (IOException e) {
                        Log.e(TAG,"Error trying to create mmSocket");
                        Log.e(TAG,e.toString());
                        e.printStackTrace();
                        return false;
                    }
                }

                @Override
                protected void onPostExecute(Boolean result)
                {
                    if (this.dialog != null) {
                        this.dialog.dismiss();
                    }
                    if(result){
                        new getReadChart().execute(id);
                    }
                    else{
                        finish();
                    }
                }

            };
            beginChartLoop.execute(id);
        }
        else {
            errorAlertDialog("Socket not connected! Try again!");
            Intent enableBluetooth = new Intent(ACTION_REQUEST_ENABLE);
            onActivityResult(1,-1,enableBluetooth);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQ_BT_ENABLE) {
            if (resultCode == RESULT_OK) {
                Log.d(TAG, "BlueTooth is now Enabled");

                final AsyncTask<BluetoothSocket, Void, Boolean> beginHistoricLoop = new AsyncTask<BluetoothSocket, Void, Boolean>() {
                    private ProgressDialog dialog;

                    @Override
                    protected void onPreExecute()
                    {
                        this.dialog = new ProgressDialog(Historic.this);
                        this.dialog.setMessage("Attempting to pair with the device...");
                        this.dialog.setCancelable(true);
                        this.dialog.setOnCancelListener(new DialogInterface.OnCancelListener()
                        {
                            @Override
                            public void onCancel(DialogInterface dialog)
                            {
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
                                        return true;
                                    } catch (IOException e) {
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
                        if (this.dialog != null) {
                            this.dialog.dismiss();
                        }
                        if(result){
                            new getReadList().execute();
                        }
                        else{
                            finish();
                        }
                    }
                };
                beginHistoricLoop.execute();
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
        listViewHistoric.setAdapter(null);
    }

    public void deleteRead(final String readId){
        final AsyncTask<BluetoothSocket, Void, Boolean> deleteReadAsyncTask = new AsyncTask<BluetoothSocket, Void, Boolean>() {
            private ProgressDialog dialog;

            @Override
            protected void onPreExecute()
            {
                this.dialog = new ProgressDialog(Historic.this);
                this.dialog.setMessage("Trying to delete a read...");
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
                Log.d(TAG,"Delete read raspberry pressed");
                if(mmSocket!=null){
                    Log.d(TAG,"mmSocket available");
                    try {
                        if (!mmSocket.isConnected() ){
                            Log.d(TAG,"mmSocket not connected, connecting...");
                            mmSocket.connect();
                        }
                        String msg = "d:"+readId;

                        mmOutputStream = mmSocket.getOutputStream();
                        Log.d(TAG,"mmOutputStream created");
                        mmOutputStream.write(msg.getBytes());
                        Log.d(TAG,"Msg Send 'd:"+readId+"' (delete)");
                    } catch (IOException e) {
                        Log.e(TAG,"Exception trying to delete the read raspberry");
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
                    errorAlertDialog("Error trying to delete a read!");
                }
                else{
                    listViewHistoric.setAdapter(null);
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
                    recreate();
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
        deleteReadAsyncTask.execute();
    }

    public void errorAlertDialog(String errorMsg){
        new AlertDialog.Builder(Historic.this)
                .setTitle("Error")
                .setMessage(errorMsg)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

}
