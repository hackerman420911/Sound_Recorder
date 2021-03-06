package com.efeproductions.soundrecorder;
        import android.app.AlertDialog;
        import android.content.Context;
        import android.content.DialogInterface;
        import android.os.Environment;
        import android.os.SystemClock;
        import android.support.v7.app.AppCompatActivity;
        import android.os.Bundle;
        import android.Manifest;
        import android.content.Intent;
        import android.content.pm.PackageManager;
        import android.graphics.Color;
        import android.graphics.drawable.ColorDrawable;
        import android.media.MediaPlayer;
        import android.media.MediaRecorder;
        import android.support.annotation.NonNull;
        import android.support.v4.app.ActivityCompat;
        import android.support.v4.content.ContextCompat;
        import android.support.v7.app.ActionBar;
        import android.support.v7.app.AppCompatActivity;
        import android.os.Bundle;
        import android.util.Log;
        import android.view.Gravity;
        import android.view.LayoutInflater;
        import android.view.MotionEvent;
        import android.view.View;
        import android.view.inputmethod.InputMethodManager;
        import android.widget.Button;
        import android.widget.Chronometer;
        import android.widget.EditText;
        import android.widget.LinearLayout;
        import android.widget.PopupWindow;
        import android.widget.RelativeLayout;
        import android.widget.TextView;
        import android.widget.Toast;

        import com.shitij.goyal.slidebutton.SwipeButton;

        import java.io.BufferedWriter;
        import java.io.File;
        import java.io.FileOutputStream;
        import java.io.IOException;
        import java.io.OutputStreamWriter;
        import java.util.UUID;


public class MainActivity extends AppCompatActivity {

    //declare variables
    Button btnRecord, btnStopRecord, btnRecordScreen, btnPlaybackScreen;
    String pathSave = "";
    MediaRecorder mediaRecorder;
    Chronometer timer;
    String Stevilo;
    final int REQUEST_PERMISSION_CODE = 1000;
    boolean continueRecording = true;
    boolean stopClicked = false;
    boolean showPopup = false;


    //ZA SWIPE
    float x1, x2, y1, y2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        //Request Runtime permissions
        if(!checkPermissionFromDevice())
            requestPermissions();

        //init view
        timer = findViewById(R.id.timer);
        btnRecord = findViewById(R.id.btnStartRecord);
        btnStopRecord = findViewById(R.id.btnStopRecord);
        btnStopRecord.setEnabled(false);


        btnRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(continueRecording &&  mediaRecorder == null && btnRecord.isEnabled() && !btnStopRecord.isEnabled()){

                    continueRecording = false;
                    btnRecord.setEnabled(false);
                    btnRecord.setVisibility(View.INVISIBLE);
                    btnStopRecord.setVisibility(View.VISIBLE);
                    timer.setVisibility(View.VISIBLE);

                    btnStopRecord.setEnabled(true);
                    timer.setBase(SystemClock.elapsedRealtime());
                    timer.start();
                    //from Android M , you need request Run time permission
                    if (checkPermissionFromDevice()) {
                        // direktorij, ime datoteke
                        //pathSave = Environment.getExternalStorageDirectory().getAbsolutePath() + "/MyRecordings/" + "New Recording " + Stevilo + ".3gp";

                        String path = Environment.getExternalStorageDirectory().toString();
                        File appDirectory = new File(path + "/" + "MyRecordings/");
                        if(!appDirectory.isDirectory()) {
                            boolean wasSuccessful = appDirectory.mkdirs();
                            if (!wasSuccessful) {
                                Log.d("Was not successfull", "sarzaewrz");
                            }
                        }
                        //stevilo recordingov
                        int stRec = countRecordings();
                        //stRec++;
                        Stevilo = Integer.toString(stRec);
                        pathSave = path + "/" + "MyRecordings/" + "New Recording " + Stevilo + ".3gp";
                        setupMediaRecorder();
                        try{
                            mediaRecorder.prepare();
                            mediaRecorder.start();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        Toast.makeText(getApplicationContext(), "Recording...", Toast.LENGTH_SHORT).show();

                    }
                    else {
                        requestPermissions();
                    }
                }
            }
        });

        btnStopRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!stopClicked && mediaRecorder != null && !btnRecord.isEnabled() && btnStopRecord.isEnabled()) {
                    btnStopRecord.setEnabled(false);
                    stopClicked = true;
                    timer.stop();

                    try{
                        mediaRecorder.stop();
                        mediaRecorder.reset();
                        mediaRecorder.release();
                        mediaRecorder = null;
                        showPopup = true;
                    }catch(RuntimeException stopException){ //ce prehitro kliknes start in stop v datoteki ni skor nc in ti da zanalasc ta error
                        mediaRecorder.reset();
                        mediaRecorder.release();
                        mediaRecorder = null;
                        delete(pathSave);
                        continueRecording = true;
                        stopClicked = false;
                        btnRecord.setVisibility(View.VISIBLE);
                        btnStopRecord.setVisibility(View.INVISIBLE);
                        timer.setVisibility(View.INVISIBLE);
                        btnRecord.setEnabled(true);
                    }
                    if(showPopup){
                        showPopup = false;
                        showPopupWindow(v);
                    }
                }
            }
        });
    }


    //RECORDING

    private void requestPermissions() {
        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.RECORD_AUDIO
        }, REQUEST_PERMISSION_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch(requestCode){
            case REQUEST_PERMISSION_CODE:
            {
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show();
                else
                    Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
            }
            break;
        }
    }


    private boolean checkPermissionFromDevice() {
        int write_external_storage_result = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int record_audio_result = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO);
        return write_external_storage_result == PackageManager.PERMISSION_GRANTED &&
                record_audio_result == PackageManager.PERMISSION_GRANTED;
    }


    private void setupMediaRecorder() {
        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mediaRecorder.setAudioEncoder(MediaRecorder.OutputFormat.AMR_NB);
        mediaRecorder.setOutputFile(pathSave);
    }

    // POPUP DIALOG

    private static int countRecordings() {
        String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + "MyRecordings/";
        File dir = new File(path);
        File[] files = dir.listFiles();

        if(files == null){
            return 1;
        }

        int stevilo = 1;
        for(int i = 0; i <= files.length; i++){
            path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + "MyRecordings/" + "New Recording " + stevilo + ".3gp";
            File file = new File(path);
            if(file.exists()){
                stevilo++;
            }
            else{
                return stevilo;
            }
        }
        return 1;
    }

    public void callPlayback(View v) {
        Intent playback = new Intent(this, PlaybackActivity.class);
        startActivity(playback);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }


    public void rename(String pathToItemRename, String name) {

        String pathDir = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + "MyRecordings/";
        /* File (or directory) with old name */
        File file = new File(pathToItemRename);

        /* File (or directory) with new name */
        File file2 = new File(pathDir + name);

        if (file2.exists()) {
            try {
                throw new java.io.IOException("File already exists!");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        /* Rename file */
        boolean success = file.renameTo(file2);
        if (!success) {
            Log.e("Rename File","Couldn't rename file!");
        } else {
            Log.i("Rename File","File renamed successfully!");
        }
    }


    public void showPopupWindow(View v){
        AlertDialog.Builder nameFileBuilder = new AlertDialog.Builder(this);

        LayoutInflater inflater = LayoutInflater.from(this);
        View view = inflater.inflate(R.layout.dialog_name_file, null);

        final EditText input = (EditText) view.findViewById(R.id.new_name);
        input.setText("New Recording " + Stevilo, TextView.BufferType.EDITABLE);

        nameFileBuilder.setTitle(this.getString(R.string.dialog_title_name));
        nameFileBuilder.setCancelable(false);
        nameFileBuilder.setPositiveButton(this.getString(R.string.dialog_action_ok),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        try {
                            String value = input.getText().toString().trim() + ".3gp";
                            if(!value.equals(".3gp")){
                                rename(pathSave, value);
                            }
                            Log.d("pritisnil si ok", "jajaja");

                        } catch (Exception e) {
                            Log.d("Error pri temule", e.toString());
                        }

                        dialog.cancel();
                        continueRecording = true;
                        stopClicked = false;
                        btnRecord.setVisibility(View.VISIBLE);
                        btnStopRecord.setVisibility(View.INVISIBLE);
                        timer.setVisibility(View.INVISIBLE);
                        btnRecord.setEnabled(true);
                    }
                });
        nameFileBuilder.setNegativeButton(this.getString(R.string.dialog_action_delete),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                        delete(pathSave);
                        dialog.cancel();
                        continueRecording = true;
                        stopClicked = false;
                        btnRecord.setVisibility(View.VISIBLE);
                        btnStopRecord.setVisibility(View.INVISIBLE);
                        timer.setVisibility(View.INVISIBLE);
                        btnRecord.setEnabled(true);
                    }
                });

        nameFileBuilder.setView(view);
        AlertDialog alert = nameFileBuilder.create();
        alert.setCanceledOnTouchOutside(false);
        alert.show();

    }

    public void delete(String path){
        File file = new File(path);
        boolean deleted;

        try {
            if (file.exists()) {
                deleted = file.delete();
                Log.d("MainActivity", "Deletion Succesfull");
            }
        } catch (Exception e){
            Log.d("Error pri brisanju", e.toString());
        }

    }

    public boolean onTouchEvent(MotionEvent touchEvent){
        switch(touchEvent.getAction()){
            case MotionEvent.ACTION_DOWN:
                x1 = touchEvent.getX();
                y1 = touchEvent.getY();
                Log.d("MainActivity", "x1=" + x1);
                break;

            case MotionEvent.ACTION_UP:
                x2 = touchEvent.getX();
                y2 = touchEvent.getY();
                Log.d("MainActivity", "x2=" + x2);
                if(x1 > x2){
                    Intent i = new Intent(MainActivity.this, PlaybackActivity.class);
                    startActivity(i);
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                }
                break;
        }
        return false;
    }

    @Override
    public void onPause() {
        super.onPause();
        if(mediaRecorder != null){
            mediaRecorder.stop();
            mediaRecorder.reset();
            mediaRecorder.release();
            mediaRecorder = null;
        }
        if(!btnRecord.isEnabled()){
            btnRecord.setEnabled(true);
            btnRecord.setVisibility(View.VISIBLE);
            btnStopRecord.setVisibility(View.INVISIBLE);
            btnStopRecord.setEnabled(false);
            timer.setVisibility(View.INVISIBLE);
            timer.stop();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(mediaRecorder != null){
            mediaRecorder.stop();
            mediaRecorder.reset();
            mediaRecorder.release();
            mediaRecorder = null;
        }
        if(!btnRecord.isEnabled()){
            btnRecord.setEnabled(true);
            btnRecord.setVisibility(View.VISIBLE);
            btnStopRecord.setVisibility(View.INVISIBLE);
            btnStopRecord.setEnabled(false);
            timer.setVisibility(View.INVISIBLE);
            timer.stop();
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        if(!btnRecord.isEnabled()){
            btnRecord.setEnabled(true);
            btnRecord.setVisibility(View.VISIBLE);
            btnStopRecord.setVisibility(View.INVISIBLE);
            btnStopRecord.setEnabled(false);
            timer.setVisibility(View.INVISIBLE);
            timer.stop();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(!btnRecord.isEnabled()){
            btnRecord.setEnabled(true);
            btnRecord.setVisibility(View.VISIBLE);
            btnStopRecord.setVisibility(View.INVISIBLE);
            btnStopRecord.setEnabled(false);
            timer.setVisibility(View.INVISIBLE);
            timer.stop();
        }
    }
}