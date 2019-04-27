package com.example.srproject;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.speech.RecognizerIntent;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;

import static java.lang.Thread.sleep;

public class MainActivity extends AppCompatActivity {

    int SpeeckRecognitionCode=1234;
    MediaRecorder myAudioRecorder;
    Button VoiceRecognition;
    Button start,sample,stop;
    TextView DisplayText,statusText,VolumeText;
    String AudioSavePathInDevice="";
    Thread VolumeThread;
    double VolumeLevel;
    ArrayList<String> result;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (ContextCompat.checkSelfPermission(
                getApplicationContext(), Manifest.permission.RECORD_AUDIO)!= PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                        getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(
                    this,new String[] {Manifest.permission.RECORD_AUDIO,Manifest.permission.WRITE_EXTERNAL_STORAGE},1);
        }
        myAudioRecorder=new MediaRecorder();
        VoiceRecognition=findViewById(R.id.button1);
        start=findViewById(R.id.button3);
        sample=findViewById(R.id.button2);
        stop=findViewById(R.id.button4);
        statusText=findViewById(R.id.textView3);
        DisplayText=findViewById(R.id.textView2);
        VolumeText=findViewById(R.id.textView4);
        DisplayText.setMovementMethod(new ScrollingMovementMethod());
    }

    protected void onResume(){
        super.onResume();



        VoiceRecognition.setOnClickListener(new  View.OnClickListener() {
            @Override
            public void onClick(View view) {

                voiceRecognition();
            }
        });
        start.setOnClickListener(new  View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.e("My Activity","Started Recording for Volume");
                start();
                ThreadCreatorFunction();
                VolumeThread.start();
                statusText.setText("Status : On");
            }
        });
        sample.setOnClickListener(new  View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.e("My Activity","Sample Recording for Volume");
                VolumeLevel=getAmplitude();
                VolumeText.setText(""+VolumeLevel);

            }
        });
        stop.setOnClickListener(new  View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.e("My Activity","Stopped Recording for Volume");
                stop();
                statusText.setText("Status : Off");
            }
        });
    }

    void ThreadCreatorFunction(){
        VolumeThread=new Thread(new Runnable() {
            public void run(){
                while(mRecorder != null){
                    VolumeLevel=getAmplitude();
                    VolumeText.setText(""+VolumeLevel);
                    try {
                        sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    void voiceRecognition(){
        Intent speechToText=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        speechToText.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speechToText.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        speechToText.putExtra(RecognizerIntent.EXTRA_PROMPT,"Speak Now");

        if(speechToText.resolveActivity(getPackageManager())==null){
            Toast.makeText(this,"Feature not available",Toast.LENGTH_LONG).show();
        }

        try{
            startActivityForResult(speechToText,SpeeckRecognitionCode);
        }catch(Exception e){
            Toast.makeText(this,""+e.getMessage(),Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode,resultCode, data);
        // Check which request we're responding to
        if (requestCode == SpeeckRecognitionCode) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                result=data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                DisplayText.setText(result.get(0));
            }
        }
        //
        //S1.stop();

    }


    //https://stackoverflow.com/questions/14181449/android-detect-sound-level

    // you have to tap each time you want the loudness
    //
    private MediaRecorder mRecorder = null;

    public void start() {
        if (mRecorder == null) {
            mRecorder = new MediaRecorder();
            mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mRecorder.setOutputFile("/dev/null");
            try {
                mRecorder.prepare();
            } catch (IOException e) {
                e.printStackTrace();
            }
            mRecorder.start();
        }
    }



    public void stop() {
        if (mRecorder != null) {
            mRecorder.stop();
            mRecorder.release();
            mRecorder = null;
        }
    }

    public double getAmplitude() {
        if (mRecorder != null)
            return  mRecorder.getMaxAmplitude();
        else
            return 1;

    }
}


