package com.example.lzanuzzo.tccapp;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

public class GoalAndTariff extends AppCompatActivity {

    String goalValue;
    String cubicMeters;
    String volumeValue;
    File file;
    String filename = "waterfyAppVariables";
    FileOutputStream outputStream;
    FileInputStream inputStream;
    static final int READ_BLOCK_SIZE = 100;
    private String TAG = GoalAndTariff.class.getSimpleName();
    EditText goalEditText;
    EditText cubicMetersEditText;
    EditText volumeValueEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_goal_and_tariff);

        goalEditText        = (EditText) findViewById(R.id.goal_value);
        cubicMetersEditText = (EditText) findViewById(R.id.tariff_volume);
        volumeValueEditText = (EditText) findViewById(R.id.tariff_value);

        try {
            file = new File(GoalAndTariff.this.getFilesDir(), filename);
            if(file.exists())
            {
                Log.d(TAG,"The file exists!");
                try {
                    inputStream = openFileInput(filename);
                    InputStreamReader InputRead = new InputStreamReader(inputStream);

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
                        goalValue = valuesArray[0];
                        cubicMeters = valuesArray[1];
                        volumeValue = valuesArray[2];

                        goalEditText.setText(goalValue);
                        cubicMetersEditText.setText(cubicMeters);
                        volumeValueEditText.setText(volumeValue);
                        Log.d(TAG,"Values set at EditText's");
                    }

                } catch (Exception e) {
                    Log.e(TAG,e.toString());
                    e.printStackTrace();
                }

            }else {
                Log.d(TAG,"Creating a new file...");
                Log.d(TAG,GoalAndTariff.this.getFilesDir().toString());
                outputStream = new FileOutputStream(GoalAndTariff.this.getFilesDir()+"/"+filename);
                //outputStream = openFileOutput(filename, Context.MODE_PRIVATE);
                outputStream.write("10;10;26.20".getBytes());
                outputStream.close();
                Log.d(TAG,"Default values loaded successfully");

                goalEditText.setText("110");
                cubicMetersEditText.setText("10");
                volumeValueEditText.setText("26.20");

                Toast.makeText(getBaseContext(), "Default values are set!",
                        Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            Log.e(TAG,e.toString());
            e.printStackTrace();
        }


        goalEditText.addTextChangedListener(new TextWatcher() {
            String newString;

            public void afterTextChanged(Editable s) {
                try {
                    outputStream = new FileOutputStream(GoalAndTariff.this.getFilesDir()+"/"+filename);
                    newString = s.toString()+";"+cubicMetersEditText.getText()+";"+volumeValueEditText.getText();
                    outputStream.write(newString.getBytes());
                    outputStream.close();
                    Log.d(TAG,"New values loaded successfully at afterTextChanged goal value");
                }catch (Exception e) {
                    Log.e(TAG,e.toString());
                    e.printStackTrace();
                }
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            public void onTextChanged(CharSequence s, int start, int before, int count) {}
        });

        cubicMetersEditText.addTextChangedListener(new TextWatcher() {
            String newString;

            public void afterTextChanged(Editable s) {
                try {
                    outputStream = new FileOutputStream(GoalAndTariff.this.getFilesDir()+"/"+filename);
                    newString = goalEditText.getText()+";"+s.toString()+";"+volumeValueEditText.getText();
                    outputStream.write(newString.getBytes());
                    outputStream.close();
                    Log.d(TAG,"New values loaded successfully at cubic meter value");
                }catch (Exception e) {
                    Log.e(TAG,e.toString());
                    e.printStackTrace();
                }
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            public void onTextChanged(CharSequence s, int start, int before, int count) {}
        });

        volumeValueEditText.addTextChangedListener(new TextWatcher() {
            String newString;

            public void afterTextChanged(Editable s) {
                try {
                    outputStream = new FileOutputStream(GoalAndTariff.this.getFilesDir()+"/"+filename);
                    newString = goalEditText.getText()+";"+cubicMetersEditText.getText()+";"+s.toString();
                    outputStream.write(newString.getBytes());
                    outputStream.close();
                    Log.d(TAG,"New values loaded successfully at afterTextChanged volume Value");
                }catch (Exception e) {
                    Log.e(TAG,e.toString());
                    e.printStackTrace();
                }
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            public void onTextChanged(CharSequence s, int start, int before, int count) {}
        });

    }




}
