package com.example.lzanuzzo.tccapp;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

import static android.bluetooth.BluetoothAdapter.ACTION_REQUEST_ENABLE;

public class MainActivity extends AppCompatActivity
    implements NavigationView.OnNavigationItemSelectedListener
    {

    private String TAG = MainActivity.class.getSimpleName();
    // MainActivity

    Integer REQ_BT_ENABLE = 1;
    Integer BT_AC_FLAG = 1;
    BluetoothSocket mmSocket;
    BluetoothDevice mmDevice = null;
    BluetoothAdapter mBluetoothAdapter;

    OutputStream mmOutputStream;
    InputStream mmInputStream;
    Thread workerThread;
    boolean workDone = true;
    byte[] readBuffer;
    int readBufferPosition;

    TextView textViewFlowValue;
    TextView textViewLitersValue;
    TextView textViewReadDate;
    TextView textViewUnixValue;
    TextView textViewGoal;
    TextView textViewSpend;
    TextView textViewTariff;
    TextView debugLabel;
    ImageView syncImage;
    LinearLayout centerShape;

    File file;
    String filename = "waterfyAppVariables";
    static final int READ_BLOCK_SIZE = 100;

    Button buttonBeginRead;
    Button buttonEndRead;

    int goalValue;
    Float cubicMeters;
    Float volumeValue;
    Float spendNow;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG,"On Create");
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        textViewFlowValue = (TextView) findViewById(R.id.textViewFlowValue);
        textViewLitersValue = (TextView) findViewById(R.id.textViewLitersValue);
        textViewUnixValue = (TextView) findViewById(R.id.textViewUnixValue);
        textViewReadDate = (TextView) findViewById(R.id.TextViewReadDate);

        textViewGoal = (TextView) findViewById(R.id.textViewValueGoal);
        textViewSpend = (TextView) findViewById(R.id.textViewValueSpend);
        textViewTariff = (TextView) findViewById(R.id.textViewValueTariff);

        debugLabel = (TextView) findViewById(R.id.debugLabel);
        syncImage = (ImageView) findViewById(R.id.syncImage);
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        syncImage.setImageResource(R.drawable.sync_off);
        centerShape = (LinearLayout) findViewById(R.id.LinearLayoutCenterShape);
        buttonBeginRead = (Button) findViewById(R.id.buttonBeginRead);
        buttonEndRead = (Button) findViewById(R.id.buttonEndRead);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        centerShape.setVisibility(View.GONE);
        buttonBeginRead.setVisibility(View.GONE);
        buttonEndRead.setVisibility(View.GONE);

        debugLabel.setText("No device...");

        spendNow = 0.0f;

        if(mBluetoothAdapter == null)
        {
            Log.d(TAG,"Adapter null");
            debugLabel.setText("No bluetooth adapter available");
            syncImage.setImageResource(R.drawable.sync_off);
            centerShape.setVisibility(View.GONE);
        }
        else
        {
            BT_AC_FLAG = 1;
            Log.d(TAG,"Adapter avaible");
            if (!mBluetoothAdapter.isEnabled()) {
                Log.d(TAG,"Enable adapter");
                Intent enableBluetooth = new Intent(ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBluetooth, REQ_BT_ENABLE);

            }else
            {
                Log.d(TAG,"Adapter Already Enabled");
                Intent enableBluetooth = new Intent(ACTION_REQUEST_ENABLE);
                onActivityResult(1,-1,enableBluetooth);
            }


            buttonBeginRead.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    beginRead();
                }
            });

            buttonEndRead.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    endRead();
                }
            });

            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Snackbar.make(view, "Getting data from sensor..", Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                    beginListenForData();
                }
            });
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == REQ_BT_ENABLE){
            if (resultCode == RESULT_OK){
                Log.d(TAG, "BlueTooth is now Enabled");
                Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
                if (pairedDevices.size() > 0) {
                    Log.d(TAG,"Paired Devices > 0");
                    for (BluetoothDevice device : pairedDevices) {
                        if (device.getName().equals("Rasp Wiki"))
                        {
                            Log.d(TAG, "Device paired: "+device.getName());
                            debugLabel.setText(device.getName());

                            mmDevice = device;
                            Log.d(TAG, "Device assigned ");
                            UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); //Standard SerialPortService ID
                            Log.d(TAG, "UUID assigned ");
                            try {
                                mmSocket = mmDevice.createRfcommSocketToServiceRecord(uuid);
                                Log.d(TAG,"mmSocket Created");
                                textViewReadDate.setText("Use the sync button!");
                                mmSocket.connect();
                                Log.d(TAG,"mmSocket Connected");
                                String msg = "r";
                                OutputStream mmOutputStream = mmSocket.getOutputStream();
                                Log.d(TAG,"mmOutputStream created");
                                mmOutputStream.write(msg.getBytes());
                                Log.d(TAG,"Msg Send 'r' (read)");


                            } catch (IOException e) {
                                Log.e(TAG,"Error trying to create mmSocket");
                                Log.e(TAG,e.toString());
                                new AlertDialog.Builder(this)
                                        .setTitle("Error")
                                        .setMessage("Error trying to connect with the raspberry!")
                                        .setIcon(android.R.drawable.ic_dialog_alert)
                                        .show();
                                e.printStackTrace();
                            }
                            syncImage.setImageResource(R.drawable.sync_off);
                            centerShape.setVisibility(View.VISIBLE);
                            buttonBeginRead.setVisibility(View.VISIBLE);
                            buttonEndRead.setVisibility(View.VISIBLE);
                            break;
                        }
                        else{
                            Log.d(TAG,"No device found...");
                            debugLabel.setText("No device...");
                            syncImage.setImageResource(R.drawable.sync_off);
                            centerShape.setVisibility(View.GONE);
                        }
                    }
                }
                else
                {
                    Log.d(TAG,"No paired devices...");
                }
            }
            if(resultCode == RESULT_CANCELED){
                Log.d(TAG, "Cant enable bluetooth!! Finishing the app");
                finish();
            }
        }
    }

    void beginListenForData() {

        textViewReadDate.setVisibility(View.INVISIBLE);
        Log.d(TAG,"Begin listening for data");
        if(mmDevice != null && mmSocket != null) {
            // Open bluetooth connection
            Log.d(TAG,"mmDevice and mmSocket not null");
            try {
                if (!mmSocket.isConnected()) {

                    Log.d(TAG,"mmSocket not connected, connecting...");
                    mmSocket.connect();

                }
                //mmOutputStream = mmSocket.getOutputStream();
                mmInputStream = mmSocket.getInputStream();
                Log.d(TAG,"mmInputStream created");

            } catch (IOException e) {
                Log.d(TAG,"Error trying to create mmInputStream");
                Log.d(TAG,e.toString());
                e.printStackTrace();
            }

            final Handler handler = new Handler();
            final byte delimiter = 10; //This is the ASCII code for a newline character
            final byte delimiterValue = 59; //ASCII ;
            workDone = false;
            readBuffer = new byte[1024];
            workerThread = new Thread(new Runnable() {
                public void run() {
                    Log.d(TAG,"Begin worker thread, reading data...");
                    while (!Thread.currentThread().isInterrupted() && !workDone) {
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

                                        handler.post(new Runnable() {
                                            public void run() {
                                                String[] dataArray = data.split(";");
                                                Log.e(TAG,data.toString());
                                                if(dataArray.length == 3){
                                                    textViewFlowValue.setText(dataArray[1] + " l/min");
                                                    textViewLitersValue.setText(dataArray[2] + " ml");
                                                    textViewUnixValue.setText(dataArray[0] + " s");

                                                    Float litersNow = Float.parseFloat(dataArray[2]);
                                                    if(cubicMeters != 0.0f){
                                                        spendNow = ((litersNow/1000.0f)*volumeValue)/cubicMeters;
                                                    }
                                                    else spendNow = 0.0f;
                                                    textViewSpend.setText(" R$ "+spendNow.toString());
                                                }
                                                else {
                                                    workDone = true;
                                                    new AlertDialog.Builder(MainActivity.this)
                                                            .setTitle("Error")
                                                            .setMessage("Error in received data")
                                                            .setIcon(android.R.drawable.ic_dialog_alert)
                                                            .show();
                                                }
                                            }
                                        });
                                    } else {
                                        readBuffer[readBufferPosition++] = b;
                                    }
                                }
                            }

                        } catch (IOException e) {
                            Log.e(TAG,"Error reading data, work done");
                            Log.e(TAG,e.toString());
                            workDone = true;
                        }
                        if (workDone == true){
                            try {
                                Log.d(TAG,"Work is done, closing the socket");
                                mmSocket.close();
                            } catch (IOException e) {
                                Log.e(TAG,"mmSocket not connected! Unable to close");
                                Log.e(TAG,e.toString());
                                e.printStackTrace();
                            }
                            break;
                        }
                    }
                }
            });

            if(mmInputStream != null && !workDone)
            {
                Log.d(TAG,"mmInputStream created and !workdone, starting worker thread");
                syncImage.setImageResource(R.drawable.sync_on);
                workerThread.start();
            }
            else{
                Log.e(TAG,"Error trying to start the worker thread... mmInputStream == null");
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Error")
                        .setMessage("Raspberry isn't sending receiving connections... ")
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show();
            }

        }
        else {
            Log.d(TAG,"mmDevice or mmSocket are null");
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle("Error")
                    .setMessage("No synchronized device!")
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();

        }


    }

    void beginRead(){

        final AsyncTask<BluetoothSocket, Void, Boolean> beginReadAsyncTask = new AsyncTask<BluetoothSocket, Void, Boolean>() {
            private ProgressDialog dialog;

            @Override
            protected void onPreExecute()
            {
                this.dialog = new ProgressDialog(MainActivity.this);
                this.dialog.setMessage("Trying to begin a read...");
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
                Log.d(TAG,"End read pressed");
                if(mmSocket!=null){
                    Log.d(TAG,"mmSocket avaible");
                    try {
                        if (!mmSocket.isConnected() ){
                            Log.d(TAG,"mmSocket not connected, connecting...");
                            mmSocket.connect();
                        }
                        String msg = "e";

                        mmOutputStream = mmSocket.getOutputStream();
                        Log.d(TAG,"mmOutputStream created");
                        mmOutputStream.write(msg.getBytes());
                        Log.d(TAG,"Msg Send 'e' (execute)");
                        //textViewReadDate.setVisibility(View.INVISIBLE);
                    } catch (IOException e) {
                        Log.e(TAG,"Exception trying to end a read");
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
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle("Error")
                            .setMessage("Error trying to begin a read!")
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .show();
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
        beginReadAsyncTask.execute();
    }

    void endRead(){
        final AsyncTask<BluetoothSocket, Void, Boolean> endReadAsyncTask = new AsyncTask<BluetoothSocket, Void, Boolean>() {
            private ProgressDialog dialog;

            @Override
            protected void onPreExecute()
            {
                this.dialog = new ProgressDialog(MainActivity.this);
                this.dialog.setMessage("Trying to end a read...");
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
                Log.d(TAG,"Begin read pressed");
                if(mmSocket!=null){
                    Log.d(TAG,"mmSocket avaible");
                    try {
                        if (!mmSocket.isConnected() ){
                            Log.d(TAG,"mmSocket not connected, connecting...");
                            mmSocket.connect();
                        }
                        String msg = "k";
                        //msg += "\n";

                        mmOutputStream = mmSocket.getOutputStream();
                        Log.d(TAG,"mmOutputStream created");
                        mmOutputStream.write(msg.getBytes());
                        Log.d(TAG,"Msg Send 'k' (kill)");
                        //textViewReadDate.setVisibility(View.INVISIBLE);
                    } catch (IOException e) {
                        Log.e(TAG,"Exception trying to end a read");
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
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle("Error")
                            .setMessage("Error trying to end a read!")
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .show();
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
        endReadAsyncTask.execute();
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
                    syncImage.setImageResource(R.drawable.sync_off);
                }
            }
            catch (IOException e) {
                Log.e(TAG,"Error closing mmSocket...");
                syncImage.setImageResource(R.drawable.sync_off);
                Log.e(TAG,e.toString());
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG,"OnResume");

        try{
            file = new File(MainActivity.this.getFilesDir(), filename);
            if(file.exists())
            {
                FileInputStream fileInputStream = openFileInput(filename);
                InputStreamReader InputRead = new InputStreamReader(fileInputStream);

                char[] inputBuffer= new char[READ_BLOCK_SIZE];
                String valuesString="";
                int charRead;

                while ((charRead = InputRead.read(inputBuffer))>0) {
                    // char to string conversion
                    String readString = String.copyValueOf(inputBuffer,0,charRead);
                    valuesString +=readString;
                }
                InputRead.close();
                Log.d(TAG,"Read file content!");

                String[] valuesArray = valuesString.split(";");
                if(valuesArray.length == 3){

                    goalValue = Integer.parseInt(valuesArray[0]);
                    cubicMeters = Float.parseFloat(valuesArray[1]);
                    volumeValue = Float.parseFloat(valuesArray[2]);

                    textViewGoal.setText(" "+valuesArray[0]+" L");
                    textViewSpend.setText(" R$ "+spendNow.toString());
                    textViewTariff.setText(" R$"+valuesArray[2]+" - "+valuesArray[1]+"m³");

                    Log.d(TAG,"Values set at EditText's");
                }

            }else {
                Log.d(TAG,"Creating a new file...");
                Log.d(TAG,MainActivity.this.getFilesDir().toString());
                FileOutputStream fileOutputStream = new FileOutputStream(MainActivity.this.getFilesDir()+"/"+filename);
                //outputStream = openFileOutput(filename, Context.MODE_PRIVATE);
                fileOutputStream.write("10;10;26.20".getBytes());
                fileOutputStream.close();
                Log.d(TAG,"Default values loaded successfully");
            }
        } catch (IOException e) {
            Log.e(TAG,e.toString());
            e.printStackTrace();
        }

        if(BT_AC_FLAG!=1){
            Intent enableBluetooth = new Intent(ACTION_REQUEST_ENABLE);
            BT_AC_FLAG = 1;
            onActivityResult(1,-1,enableBluetooth);
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        Log.e(TAG,String.valueOf(id));

        BT_AC_FLAG = 0;

        if (id == R.id.nav_historic) {
            Log.d(TAG,"Historic selected at drawer");
            Intent historic = new Intent(MainActivity.this, Historic.class);
            MainActivity.this.startActivity(historic);
        } else if (id == R.id.nav_tariff) {
            Log.d(TAG,"Tariff and Goal selected at drawer");
            Intent goalAndTariff = new Intent(MainActivity.this, GoalAndTariff.class);
            MainActivity.this.startActivity(goalAndTariff);

        }  else if (id == R.id.nav_rasp) {
            Log.d(TAG,"Rasp selected at drawer");
            Intent configActivity = new Intent(MainActivity.this, Config.class);
            MainActivity.this.startActivity(configActivity);

        }


        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
}
