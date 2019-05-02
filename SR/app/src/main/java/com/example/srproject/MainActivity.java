package com.example.srproject;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
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
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

import static java.lang.Thread.currentThread;
import static java.lang.Thread.sleep;

public class MainActivity extends AppCompatActivity {

    int SpeeckRecognitionCode=1234;
    Button VoiceRecognition;
    TextView DisplayText;
    ArrayList<String> result;
    HashMap<String,Integer> WordsDatabase;
    int TextPleasureScore;

    private MediaRecorder mRecorder = null;
    Button start,sample,stop;
    Thread VolumeThread;
    double VolumeLevel;
    TextView VolumeText,statusText;

    int Pleasure=-1;
    int Arousal=-1;
    ImageView EmotionDisplayImage;

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
        VoiceRecognition=findViewById(R.id.button1);
        start=findViewById(R.id.button3);
        sample=findViewById(R.id.button2);
        stop=findViewById(R.id.button4);
        statusText=findViewById(R.id.textView3);
        DisplayText=findViewById(R.id.textView2);
        VolumeText=findViewById(R.id.textView4);
        DisplayText.setMovementMethod(new ScrollingMovementMethod());
        EmotionDisplayImage=findViewById(R.id.imageView);
        WordsDatabase=new HashMap<String,Integer>();
        try {
            createDataBaseOfPleasure();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
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
                    //VolumeText.setText(VolumeText.getText()+" "+currentThread().getName());

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            String temp="";
                            if(VolumeLevel>30000){
                                Arousal=1;
                                temp="High";
                            }
                            else if(VolumeLevel>10000){
                                Arousal=0;
                                temp="Low";
                            }
                            if(VolumeLevel>10000){
                                VolumeText.setText("Arousal = "+temp+"\n-----------\nResults : Volume Detection = "+VolumeLevel);
                                changeEmotionStatus();
                            }
                        }
                    });
                    //mainThread.run();
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

                analyseText(result.get(0));
                String temp;
                if(TextPleasureScore>=0){
                    Pleasure=1;
                    temp="High";
                }
                else{
                    Pleasure=0;
                    temp="Low";
                }
                DisplayText.setText("Score = "+TextPleasureScore+"\nPleasure = "+temp);
                DisplayText.setText(DisplayText.getText()+"\n-----------\n"+"Results : Voice Recognition = "+result.get(0));
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        changeEmotionStatus();
                    }
                });
            }
        }
        //
        //S1.stop();

    }

    //https://stackoverflow.com/questions/14181449/android-detect-sound-level
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

    public void changeEmotionStatus(){

        if(Pleasure==-1 || Arousal==-1){
            return;
        }
        else if(Pleasure==1 && Arousal==1){
            EmotionDisplayImage.setImageResource(R.drawable.happy);
        }
        else if(Pleasure==1 && Arousal==0){
            EmotionDisplayImage.setImageResource(R.drawable.calm);
        }
        else if(Pleasure==0 && Arousal==0){
            EmotionDisplayImage.setImageResource(R.drawable.sad);
        }
        else{
            EmotionDisplayImage.setImageResource(R.drawable.angry);
        }
    }

    //Create Database
    void createDataBaseOfPleasure() throws URISyntaxException, IOException {
        insertValuesIntoDataset("BadWordsDataset.txt",-1);
        insertValuesIntoDataset("GoodWordsDataset.txt",1);
    }

    void insertValuesIntoDataset(String datasetName, int val)throws IOException{
        InputStream myInputStream = getAssets().open(datasetName);
        BufferedReader reader1 = new BufferedReader(new InputStreamReader(myInputStream));
        String temp;
        while((temp = reader1.readLine())!= null) {
            String[] words = temp.split(",");
            WordsDatabase.put(words[0],val);
        }
    }

    void analyseText(String text){
        String[] words=text.split(" ");
        TextPleasureScore=0;
        for(String temp : words){
            if(WordsDatabase.containsKey(temp.toLowerCase())){
                TextPleasureScore+=WordsDatabase.get(temp.toLowerCase());
            }
        }
    }

}


