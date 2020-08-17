package com.jackco.avifviewer;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;

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

        long time = System.currentTimeMillis();
        Process proc = null;
        proc = Runtime.getRuntime().exec(cmd, envp);
        decodedIn = "Decoded in " + String.valueOf(System.currentTimeMillis() - time) + " milliseconds";
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
        File executable = new File(name);
        Process process = null;
        String res = null;
        try {
            res = execCmd(name, envp);
        } catch (IOException e) {
            e.printStackTrace();
        }

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
            return null;
        }
        resultS = res;

        int width = Integer.parseInt(widheiS[0]);
        int height = Integer.parseInt(widheiS[1]);

        byte[] fileContent = null;
        try {
            fileContent = Files.readAllBytes(file.toPath());
        } catch (IOException e) {
            e.printStackTrace();
        }


        Bitmap p = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        int x = 0;
        int y = 0;

        for(int i = 0; i < width*height*4; ){
            if(x == width) {
                y++;
                x = 0;
            }
            int color = (fileContent[i++] & 0xff) << 24 | (fileContent[i++] & 0xff) << 16 | (fileContent[i++] & 0xff) << 8 | (fileContent[i++] & 0xff);
            p.setPixel(x,y,color);
            x++;
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
            File destF = new File(getDataDir() + "/about_layout.avif");
            if (destF.exists()) {
                destF.delete();
            }
            try {
                copy(getContentResolver().openInputStream(uri), destF);
            } catch (IOException e) {
            }

            AsyncOpenAVIF asyncTask=new AsyncOpenAVIF();
            asyncTask.execute(getDataDir().getAbsolutePath() + "/about_layout.avif");


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
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
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
                Dialog custoDialog = new Dialog(MainActivity.this);
                custoDialog.setContentView(R.layout.about_layout);

                Window window = custoDialog.getWindow();
                window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
                window.setGravity(Gravity.CENTER);



                custoDialog.show();
            }
        });


    }


}