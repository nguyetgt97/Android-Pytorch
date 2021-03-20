package com.example.pan;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;

import org.pytorch.IValue;
import org.pytorch.Module;
import org.pytorch.PyTorchAndroid;
import org.pytorch.Tensor;
import org.pytorch.torchvision.TensorImageUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Map;


public class MainActivity extends AppCompatActivity  implements Runnable {
    private static final String TAG = "MainActivity";
    private String mTestImage = "test1.png";
    private Bitmap mbitmap;
    private ImageView mImageview;
    private ImageView mImageviewResult;
    private Button mButtonStart;
    private ProgressBar mPregressBar;
    private Module mModule;
    private int mImageindex = 0;

    private static final int INPUT_SZIE = 140;
    public static String assetFilepath(Context context, String assetName) throws IOException {
        File file = new File(context.getFilesDir(), assetName);
        if (file.exists() && file.length() > 0) {
            return file.getAbsolutePath();
        }
        try (InputStream is = context.getAssets().open(assetName)) {
            try (OutputStream os = new FileOutputStream(file)) {
                byte[] buffer = new byte[4 * 1024];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    os.write(buffer, 0, read);
                }
                os.flush();
            }
            return file.getAbsolutePath();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
            mbitmap = BitmapFactory.decodeResource(getResources(), R.drawable.headx2);


        mImageview = findViewById(R.id.imageview);
        mImageviewResult = findViewById(R.id.imageview_result);
        mImageview.setImageBitmap(mbitmap);

        final Button buttonRestart = findViewById(R.id.restartButton);
        buttonRestart.setOnClickListener((v) -> {
            if (mTestImage == "test1.png")
                mTestImage = "test2.png";
            else
                mTestImage = "test1.png";

            try {
                mbitmap = BitmapFactory.decodeStream(getAssets().open(mTestImage));
            } catch (IOException e) {
                Log.e("ImageSegmentation", "Error reading assets", e);
                finish();
            }
        });


        mButtonStart = findViewById(R.id.buttonStart);
        mPregressBar = findViewById(R.id.progressBar);
        mButtonStart.setOnClickListener(v -> {
            Log.d(TAG, "onCreate: mButtonStart click");
            mButtonStart.setEnabled(false);
            mPregressBar.setVisibility(ProgressBar.VISIBLE);
            mButtonStart.setText("Running model");

            Thread thread = new Thread(MainActivity.this);
            thread.start();

        });

        try {
            mModule = Module.load(MainActivity.assetFilepath(getApplicationContext(), "model_4x_mobile.pt"));
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    float[] mean = {0.f, 0.f, 0.f};
    float[] std = {1.0f, 1.f, 1.f};
    @Override
    public void run() {
        Bitmap inputBitmap  = Bitmap.createScaledBitmap(mbitmap, INPUT_SZIE, INPUT_SZIE, false);
        final Tensor inputTensor = TensorImageUtils.bitmapToFloat32Tensor(inputBitmap,
                mean,std);

        //HiepTHb: forward input tensor
        IValue inputs = IValue.from(inputTensor);
        Tensor outputs = mModule.forward(inputs).toTensor();
        float[] outputValues = outputs.getDataAsFloatArray();
        Log.d(TAG, "run: outputValues "+ Arrays.toString(outputs.getDataAsFloatArray()));
        Bitmap outputBitmap =Bitmap.createBitmap(224*4, 224*4, mbitmap.getConfig());
        outputBitmap = arrayFloatToBitmap(INPUT_SZIE*4, INPUT_SZIE*4, outputValues);
        Bitmap finalOutputBitmap = outputBitmap;
        runOnUiThread(() -> {
            mImageviewResult.setImageBitmap(finalOutputBitmap);
            mImageview.setImageBitmap(Bitmap.createScaledBitmap(mbitmap, INPUT_SZIE*4, INPUT_SZIE*4, false));
            mButtonStart.setEnabled(true);
            mButtonStart.setText("Resolution");
            mPregressBar.setVisibility(ProgressBar.INVISIBLE);
        });
        /*final float[] inputs = inputTensor.getDataAsFloatArray();
        Map<String, IValue> outTensors = mModule.forward(IValue.from(inputTensor)).toDictStringKey();
        final Tensor outputTensor = outTensors.get("out").toTensor();
        final float[] scores = outputTensor.getDataAsFloatArray();
        int width = mbitmap.getWidth();
        int height = mbitmap.getHeight();
        int[] intValues = new int[width * height];

//        for (int j = 0; j< width; j++){
//            for (int k = 0; k <height; k++){
//                int maxi = 0
//    }

        Bitmap pan = Bitmap.createScaledBitmap(mbitmap, width, height, true);
        Bitmap outputBitmap = pan.copy(pan.getConfig(), true);
        outputBitmap.setPixels(intValues, 0, outputBitmap.getWidth(), 0, 0, outputBitmap.getWidth(), outputBitmap.getHeight());
        final Bitmap tranferredBitmap = Bitmap.createScaledBitmap(outputBitmap, mbitmap.getWidth(),mbitmap.getHeight(), true);
        runOnUiThread(() -> {
            mImageview.setImageBitmap(tranferredBitmap);
            mButtonStart.setEnabled(true);
            mButtonStart.setText("Resolution");
            mPregressBar.setVisibility(ProgressBar.INVISIBLE);
        });
*/
    }

    private Bitmap arrayFloatToBitmap(int width, int height, float[] pixels){

        byte alpha = (byte) 255 ;
        Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888) ;

        ByteBuffer byteBuffer = ByteBuffer.allocate(width*height*4*3) ;

        float maxValue = pixels[0];
        float minValue = pixels[0];
        for(int i=1;i < pixels.length;i++){
            if(pixels[i] > maxValue){
                maxValue = pixels[i];
            }
            if(pixels[i] < minValue){
                minValue = pixels[i];
            }
        }
        float delta = maxValue - minValue ;

        for(int i=0; i < pixels.length; i++){
            int c = i / (height * width);
            int buffer_idx = i % (height * width);
            byte temValue = (byte) ((byte) ((((pixels[i]-minValue)/delta)*255)));
            byteBuffer.put(buffer_idx*4+c, temValue) ;
        }
        for(int i=0; i < height*width; i++){
            byteBuffer.put(i*4+3, alpha) ;
        }
        bmp.copyPixelsFromBuffer(byteBuffer) ;
        return bmp ;
    }
    }



