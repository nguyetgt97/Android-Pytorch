package com.example.pan;

import android.graphics.Bitmap;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;
import org.pytorch.IValue;
import org.pytorch.Module;
import org.opencv.android.OpenCVLoader;
import org.pytorch.Tensor;
import org.pytorch.torchvision.TensorImageUtils;

import android.util.Log;

import java.nio.ByteBuffer;

import javax.crypto.spec.IvParameterSpec;

public class Preprocess {
    private static final String TAG = "Preprocess";

    Module model;
    float[] mean = {0.5f, 0.5f, 0.5f};
    float[] std = {0.5f, 0.5f, 0.5f};

    public Preprocess (String modelPath){
        model = Module.load(modelPath);
        if (!OpenCVLoader.initDebug()) {
            Log.e("Opencv", "unable to load OpenCV") ;
        }
        else
            Log.d("Opencv","OpenCV Loaded");
    }


    public void setMeanAndStd(float[] mean, float[] std){
        this.mean = mean;
        this.std = std;
    }

    public Tensor prepocess(Bitmap bitmap, int w, int h){
        Bitmap bitmap1 = opencvResize(bitmap, w, h);
        return TensorImageUtils.bitmapToFloat32Tensor(bitmap1, this.mean, this.std);
    }

    public static Bitmap opencvResize(Bitmap bitmap, int w, int h) {
        if (bitmap == null){
            return null;
        }
        Bitmap resized = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Mat input = new Mat();
        Mat output = new Mat();
        Utils.bitmapToMat(bitmap, input);
        Imgproc.resize(input, output, new org.opencv.core.Size(w,h), Imgproc.INTER_LANCZOS4);
        Utils.matToBitmap(output, resized);
        return resized;
    }
    public int argMax(float[] inputs){
        int maxIndex = -1;
        float maxvalue = 0.0f;

        for ( int i =0; i < inputs.length; i++){
            if(inputs[i]>maxvalue){
                maxIndex = i;
                maxvalue = inputs[i];
            }
        }
        return maxIndex;
    }

    private Bitmap arrayFloatToBitMap(float[] pixels){
        byte alpha = (byte) 255;
        int width = 768;
        int height = 512;
        Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        ByteBuffer byteBuffer = ByteBuffer.allocate(width*height*4*3);

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

     public Bitmap generate(Bitmap bitmap){
        Log.d(TAG, "running");
        Tensor tensor = prepocess(bitmap, 768, 512);

         IValue inputs = IValue.from(tensor);
         Tensor outputs = model.forward(inputs).toTensor();
         float[] scores = outputs.getDataAsFloatArray();
         Log.d(TAG, "End");
         Bitmap output = Bitmap.createBitmap(768,512, bitmap.getConfig());
         output = arrayFloatToBitMap(scores);
         output = opencvResize(output, bitmap.getWidth(), bitmap.getHeight());
         return output;
     }
    }


