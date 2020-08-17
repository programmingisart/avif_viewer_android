package com.jackco.avifviewer;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import android.app.Activity;
import android.app.Dialog;
import android.app.UiModeManager;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.ortiz.touchview.TouchImageView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {
    TextView text2 = null;
    TextView text3 = null;
    ProgressBar progress1;
    ImageView image1;

    Bitmap mainImage;

    String decodedIn = null;
    String resultS = null;

    String decodeResult = null;


    Button button2;


    private class AsyncOpenAVIF extends AsyncTask<String, String, Bitmap> {


        @Override
        protected Bitmap doInBackground(String... filePath) {
            return openAVIF(filePath[0]);
        }

        @Override
        protected void onPreExecute() {
            button2.setVisibility(View.GONE);
            image1.setVisibility(View.GONE);
            text2.setVisibility(View.GONE);
            text3.setVisibility(View.GONE);

            progress1.setVisibility(View.VISIBLE);
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {

            updateImage();
        }
    }


    private void updateImage(){
        progress1.setVisibility(View.GONE);
        image1.setVisibility(View.VISIBLE);
        image1.setImageBitmap(mainImage);
        text2.setText(decodedIn);
        text3.setText(resultS);
        text2.setVisibility(View.VISIBLE);
        text3.setVisibility(View.VISIBLE);
        button2.setVisibility(View.VISIBLE);

    }

    private String execCmd(String cmd, String[] envp) throws java.io.IOException {

        Process proc = null;
        proc = Runtime.getRuntime().exec(cmd, envp);
        java.io.InputStream is = proc.getInputStream();
        java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
        String val = "";
        if (s.hasNext()) {
            val = s.next();
        }
        else {
            val = "";
        }
        return val;
    }


    public static void copy(InputStream in, File dst) throws IOException {
        try (OutputStream out = new FileOutputStream(dst)) {
            // Transfer bytes from in to out
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
        }
    }


    Bitmap openAVIF(String filePath){

        String ev = getApplicationInfo().nativeLibraryDir;
        String[] envp = {"LD_LIBRARY_PATH=" + ev};

        String rawFileP = getDataDir() + "/decoded.raw";
        File file = new File(rawFileP);

        if(file.exists()) {
            file.delete();
        }

        String name = getApplicationInfo().nativeLibraryDir + "/libavif_example1.so " + filePath + " " + rawFileP;

        long time = System.currentTimeMillis();


        String res = null;
        try {
            res = execCmd(name, envp);
        } catch (IOException e) {
            e.printStackTrace();
        }
        decodedIn = "Decoded in " + String.valueOf(System.currentTimeMillis() - time) + " milliseconds";


        File todeleteF = new File(filePath);
        if(todeleteF.exists())todeleteF.delete();



        Log.e("RES", res);
        String resS[]= res.split("\n");
        String sizS[] = resS[1].split(":");
        String widheiS[] = sizS[1].split("x");

        decodeResult = resS[0];


        if(!decodeResult.equals("decodeResult:OK")){
            resultS = "Decode Failed";
            mainImage = null;
            if(file.exists()) file.delete();
            return null;
        }
        resultS = res;

        int width = Integer.parseInt(widheiS[0]);
        int height = Integer.parseInt(widheiS[1]);

        Log.e("SIZE", String.valueOf(file.length()));


        int x = 0;
        int y = 0;

        Bitmap p = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        FileInputStream fi = null;
        try {
           fi = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        int READONCE = 64;

        if(READONCE > height) READONCE = height;

        int lines = READONCE;
        int read = width*4*lines;
        byte[] fileContent = new byte[read];

        int[] colors = new int[read];

        while(y < height) {
            try {
                fi.read(fileContent);
            } catch (IOException e) {
                e.printStackTrace();
            }
            x=0;
            for (int i = 0; i < read; ) {
                int color = (fileContent[i++] & 0xff) << 24 | (fileContent[i++] & 0xff) << 16 | (fileContent[i++] & 0xff) << 8 | (fileContent[i++] & 0xff);
                colors[x] = color;
                x++;
            }
            p.setPixels(colors,0,width,0, y,width, lines);
            y += READONCE;
            if(height-y < READONCE) lines = height - y;
        }


        file.delete();



        mainImage = p;

        return p;

    }


    final int fileRequestCode = 21;

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == fileRequestCode && resultCode == Activity.RESULT_OK && data != null) {
            Uri uri = null;
            uri = data.getData();
            File destF = new File(getDataDir() + "/image1.avif");
            if (destF.exists()) {
                destF.delete();
            }
            try {
                copy(getContentResolver().openInputStream(uri), destF);
            } catch (IOException e) {
            }

            AsyncOpenAVIF asyncTask=new AsyncOpenAVIF();
            asyncTask.execute(getDataDir().getAbsolutePath() + "/image1.avif");


        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        progress1 = findViewById(R.id.progress1);
        progress1.setVisibility(View.GONE);

        text2 = findViewById(R.id.text2);
        text3 = findViewById(R.id.text3);
        button2 = findViewById(R.id.button2);

        image1 = findViewById(R.id.image1);



        try {
            OutputStream myOutput = new FileOutputStream(getDataDir() + "/test1.avif");
            byte[] buffer = new byte[1024];
            int length;
            InputStream myInput = getAssets().open("test1.avif");
            while ((length = myInput.read(buffer)) > 0) {
                myOutput.write(buffer, 0, length);
            }
            myInput.close();
            myOutput.flush();
            myOutput.close();
        }
        catch (Exception e) {
        }

        AsyncOpenAVIF asyncTask=new AsyncOpenAVIF();
        asyncTask.execute(getDataDir() + "/test1.avif");

        final Button button1 = findViewById(R.id.button1);

        button1.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_DEFAULT);
                intent.setType("*/*");
                startActivityForResult(intent,fileRequestCode);
            }
        });





        button2.setVisibility(View.GONE);
        button2.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                MediaStore.Images.Media.insertImage(getContentResolver(), mainImage, "image" , "saved by Avif Viewer");
                Toast.makeText(getApplicationContext(), "Image saved to Gallery (/sdcard/Pictures folder)", Toast.LENGTH_LONG).show();
            }
        });

        ImageButton buttonA = findViewById(R.id.buttona);

        buttonA.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Dialog licenseDialog = new Dialog(MainActivity.this);
                licenseDialog.setContentView(R.layout.about_layout);

                Window window = licenseDialog.getWindow();
                window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
                window.setGravity(Gravity.CENTER);

                licenseDialog.show();
            }
        });

        image1.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if(mainImage == null) return;
                Dialog imageDialog = new Dialog(MainActivity.this, R.style.Theme_AppCompat_NoActionBar);

                imageDialog.setContentView(R.layout.fullscreen_image_layout);
                Window window = imageDialog.getWindow();
                window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
                window.setGravity(Gravity.CENTER);
                window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                        WindowManager.LayoutParams.FLAG_FULLSCREEN);
                TouchImageView imagez = imageDialog.findViewById(R.id.imagef1);

                imagez.setImageBitmap(mainImage);
                imageDialog.show();

            }
        });

    }


}