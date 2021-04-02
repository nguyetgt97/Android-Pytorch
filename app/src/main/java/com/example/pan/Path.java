package com.example.pan;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.davemorrissey.labs.subscaleview.ImageSource;
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;

import org.pytorch.IValue;
import org.pytorch.Module;
import org.pytorch.Tensor;
import org.pytorch.torchvision.TensorImageUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

public class Path {

    private static final String TAG = "Path";

    public static final String EXT_RAW = ".dng";
    public static final String EXT_JPG = ".jpg";

    public static final String MIME_RAW = "image/x-adobe-dng";
    public static final String MIME_JPG = "image/jpeg";

    public static final String ROOT = Environment.getExternalStorageDirectory().toString();
    public static final String HDRP = "HDRP";
    private static String id;


    public static String generateTimestamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd HHmmss", Locale.US);
        return sdf.format(new Date());
    }

    public static String getPathFromUri(Context context, Uri uri) {
        String filePath = getColomn(context, uri, MediaStore.Images.Media.DATA);

        if (filePath == null) {
            filePath = uri.getPath();
            if (filePath.contains(":")) {
                String[] split = filePath.split(":");
                filePath = split[split.length - 1];
            }
        }
        Log.d(TAG, "Resolved" + uri.toString() + "to Path" + filePath);
        return filePath;
    }

    private static String getColomn(Context context, Uri uri, String column) {
        ContentResolver cr = context.getContentResolver();
        String result = null;
        if (DocumentsContract.isDocumentUri(context, uri)) {
            String id = DocumentsContract.getDocumentId(uri);
            if (id.contains((":"))) {
                id = id.split(":")[1];
            }
            result = query(cr, MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    column,
                    MediaStore.Images.Media._ID + "=?",
                    new String[]{id});

            if (result == null) {
                try {
                    long l = Long.valueOf(id);
                    result = query(cr, ContentUris.withAppendedId(
                            Uri.parse("content://downloads/public_downloads"), l),
                            column);
                } catch (Exception ignored) {
                }
            }
        }
        if (result == null) {
            result = query(cr, uri, column);
        }
        return result;

    }

    private static String query(ContentResolver cr, Uri uri, String column) {
        return query(cr, uri, column, null, null);
    }


    private static String query(ContentResolver cr, Uri uri, String column, String s, String[] strings) {
        try (Cursor cursor = cr.query(
                uri, new String[]{column}, s, strings, null)){
            if (cursor != null){
                int columnIndex = cursor.getColumnIndex(column);
                if (cursor.moveToFirst()){
                    return cursor.getString(columnIndex);
                }
            }
        }
            return null;
    }
}

//
//package com.example.pan;
//
//        import androidx.annotation.NonNull;
//        import androidx.annotation.Nullable;
//        import androidx.appcompat.app.AppCompatActivity;
//        import androidx.core.app.ActivityCompat;
//        import androidx.core.content.ContextCompat;
//
//        import android.Manifest;
//        import android.annotation.SuppressLint;
//        import android.content.Context;
//        import android.content.Intent;
//        import android.content.pm.PackageManager;
//        import android.database.Cursor;
//        import android.graphics.Bitmap;
//        import android.graphics.BitmapFactory;
//        import android.graphics.Matrix;
//        import android.media.ExifInterface;
//        import android.net.Uri;
//        import android.os.Build;
//        import android.os.Bundle;
//        import android.provider.MediaStore;
//        import android.util.Log;
//        import android.view.View;
//        import android.view.View.OnClickListener;
//        import android.widget.Button;
//        import android.widget.ImageView;
//        import android.widget.ProgressBar;
//        import android.widget.Toast;
//
//        import com.davemorrissey.labs.subscaleview.ImageSource;
//        import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;
//
//        import org.pytorch.IValue;
//        import org.pytorch.Module;
//        import org.pytorch.PyTorchAndroid;
//        import org.pytorch.Tensor;
//        import org.pytorch.torchvision.TensorImageUtils;
//
//        import java.io.File;
//        import java.io.FileNotFoundException;
//        import java.io.FileOutputStream;
//        import java.io.IOException;
//        import java.io.InputStream;
//        import java.io.OutputStream;
//        import java.nio.ByteBuffer;
//        import java.util.Arrays;
//        import java.util.Map;
//
//        import static com.example.pan.Preprocess.opencvResize;
//
//
//public class MainActivity extends AppCompatActivity implements Runnable {
//    private static final String TAG = "MainActivity";
//
////    int cameraRequestCode = 001;
//
//    //    Preprocess preprocess;
//    private Bitmap mbitmap;
//    private SubsamplingScaleImageView msubsamplingimageview;
//    private SubsamplingScaleImageView getMsubsamplingimageview_result;
//    private Button mButtonStart;
//    private ProgressBar mPregressBar;
//    private Module mModule;
//    private Button mSelectButton;
//    private static final int INPUT_SZIE = 200;
//    private int originalOrientation;
//
//    private static final int REQUEST_CORE_STORAGE_PERMISSION = 1;
//    private static final int REQUEST_CODE_SELECT_IMAGE = 2;
//
//
//    public static String assetFilepath(Context context, String assetName) throws IOException {
//        File file = new File(context.getFilesDir(), assetName);
//        if (file.exists() && file.length() > 0) {
//            return file.getAbsolutePath();
//        }
//        try (InputStream is = context.getAssets().open(assetName)) {
//            try (OutputStream os = new FileOutputStream(file)) {
//                byte[] buffer = new byte[4 * 1024];
//                int read;
//                while ((read = is.read(buffer)) != -1) {
//                    os.write(buffer, 0, read);
//                }
//                os.flush();
//            }
//            return file.getAbsolutePath();
//        }
//    }
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_main);
//        mbitmap = BitmapFactory.decodeResource(getResources(), R.drawable.headx2);
//
//        msubsamplingimageview = findViewById(R.id.imageview);
//        getMsubsamplingimageview_result = findViewById(R.id.imageview_result);
//        msubsamplingimageview.setImage(ImageSource.bitmap(mbitmap));
//        mSelectButton = findViewById(R.id.collectImage);
//        mButtonStart = findViewById(R.id.buttonStart);
//        mPregressBar = findViewById(R.id.progressBar);
//
//        mButtonStart.setOnClickListener(v -> {
//            Log.d(TAG, "onCreate: mButtonStart click");
//            mButtonStart.setEnabled(false);
//            mPregressBar.setVisibility(ProgressBar.VISIBLE);
//            mButtonStart.setText("Running model");
//            Thread thread = new Thread(MainActivity.this);
//            thread.start();
//
//        });
//        try {
//            mModule = Module.load(MainActivity.assetFilepath(getApplicationContext(), "model_4x_mobile.pt"));
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//
//        mSelectButton.setOnClickListener(new View.OnClickListener() {
//
//            @Override
//            public void onClick(View v) {
//                if (ContextCompat.checkSelfPermission(
//                        getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE
//                ) != PackageManager.PERMISSION_GRANTED) {
//                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
//                            REQUEST_CORE_STORAGE_PERMISSION);
//                } else {
//                    pickImageFromGallery();
//                }
//            }
//        });
//    }
//
////    private static final int REQUEST_IMAGE = 2;
////
////    public static int exifToDegrees(int exifOrientation) {
////        if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_90) {
////            return 90;
////        } else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_180) {
////            return 180;
////        } else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_270) {
////            return 270;
////        }
////        return 0;
////    }
////
////    int rotationInDegree;
////    int rotation;
////
////    public boolean selectImage() {
////        Intent chooseFile;
////        Intent intent;
////        chooseFile = new Intent(Intent.ACTION_GET_CONTENT);
////        chooseFile.addCategory(Intent.CATEGORY_OPENABLE);
////        chooseFile.setType("image/*");
////        chooseFile.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
////        intent = Intent.createChooser(chooseFile, "choose a file");
////        startActivityForResult(intent, REQUEST_IMAGE);
////
////        return false;
////    }
////
////    private void storeImage(Bitmap image) {
////        File pictureFile = new File("/sdcard/bokeh.png");
////        if (pictureFile == null) {
////            Log.d(TAG,
////                    "Error creating media file, check storage permissions: ");// e.getMessage());
////            return;
////        }
////        try {
////            FileOutputStream fos = new FileOutputStream(pictureFile);
////            image.compress(Bitmap.CompressFormat.PNG, 90, fos);
////            fos.close();
////        } catch (FileNotFoundException e) {
////            Log.d(TAG, "File not found: " + e.getMessage());
////        } catch (IOException e) {
////            Log.d(TAG, "Error accessing file: " + e.getMessage());
////        }
////    }
//
////    Bitmap orginalBitmap;
//
//    private void pickImageFromGallery() {
////        intent to pick image
//        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
//        if (intent.resolveActivity(getPackageManager()) != null) {
//            startActivityForResult(intent, REQUEST_CODE_SELECT_IMAGE);
//        }
//    }
//
//    //handle result of runtime permission
//    @Override
//    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//        if (requestCode == REQUEST_CORE_STORAGE_PERMISSION && grantResults.length > 0) {
//            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                pickImageFromGallery();
//            } else {
////                    Permission Denied
//                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
//            }
//        }
//    }
//
//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//        if (requestCode == REQUEST_CODE_SELECT_IMAGE && resultCode == RESULT_OK) {
////            String filePath = Path.getPathFromUri(this, data.getData());
////            String originalPath = filePath;
////            File testFile = new File(originalPath);
////            Log.d("TEST", "onActivityResult: testFile" + testFile.exists());
////            try {
////                ExifInterface exif = new ExifInterface(filePath);
////                rotation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
////                rotationInDegree = exifToDegrees(rotation);
////                Log.d("TEST", "onActivityResult: rotationInDegrees" + rotationInDegree);
////            } catch (IOException e) {
////                e.printStackTrace();
////            }
////
////            Matrix matrix = new Matrix();
////            if (rotation != 0) {
////                matrix.preRotate(rotationInDegree);
////            }
////            orginalBitmap = BitmapFactory.decodeFile(filePath);
//////            orginalBitmap = Bitmap.createBitmap(orginalBitmap, 0,0, orginalBitmap.getWidth(),orginalBitmap.getHeight(), matrix, true);
////            if (orginalBitmap.getWidth() > 4200 || orginalBitmap.getHeight() > 4200) {
////                orginalBitmap = opencvResize(orginalBitmap, orginalBitmap.getWidth() / 3, orginalBitmap.getHeight() / 3);
////            }
////            final Bitmap pred = preprocess.generate(orginalBitmap);
////            runOnUiThread(() -> {
////                        msubsamplingimageview.setImage(ImageSource.bitmap(pred));
////                        getMsubsamplingimageview_result.setImage(ImageSource.bitmap(orginalBitmap));
////
////                    }
////            );
//            if (data != null) {
//
//                Uri selectedImageUri = data.getData();
//                if (selectedImageUri != null) {
//                    String realPath = getPathFromUri(selectedImageUri);
//                    Log.d(TAG, "onActivityResult: realPath = "+realPath);
//                    if(realPath!=null) {
//                        try {
//                            InputStream inputStream = getContentResolver().openInputStream(selectedImageUri);
//                            mbitmap = BitmapFactory.decodeStream(inputStream);
//
////                            mbitmap = getResizedBitmap(mbitmap,mbitmap.getWidth()/21,mbitmap.getHeight()/21);
//                            originalOrientation = getOrientation(realPath);
//                            msubsamplingimageview.setOrientation(originalOrientation);
//                            msubsamplingimageview.setImage(ImageSource.bitmap(mbitmap));
//
//                        } catch (Exception exception) {
//                            Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
//                        }
//                    }
//                }
//            }
//        }
//
//    }
//
////
////    final Button buttonRestart = findViewById(R.id.restartButton);
////        buttonRestart.setOnClickListener((v) -> {
////            if (mTestImage == "test2.png")
////                mTestImage = "test3.png";
////            else
////                mTestImage = "test3.png";
////
////            try {
////                mbitmap = BitmapFactory.decodeStream(getAssets().open(mTestImage));
////            } catch (IOException e) {
////                Log.e("ImageSegmentation", "Error reading assets", e);
////                finish();
////            }
////        });
////
//
//
////    @Override
////    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
////        super.onActivityResult(requestCode, resultCode, data);
////        if (resultCode == RESULT_OK && requestCode ==IMAGE_PICK_CODE){
//////            set image to image view
////            msubsamplingimageview.setImage(ImageSource.uri(data.getData()));
////
////
////        }
////    }
//
//    float[] mean = {0.f, 0.f, 0.f};
//    float[] std = {1.0f, 1.f, 1.f};
//
//    @Override
//    public void run() {
//
//        int width = Math.min(mbitmap.getWidth(), mbitmap.getHeight());
//
//        Log.d(TAG, "run: width = "+width);
//
//        Bitmap inputBitmap = Bitmap.createBitmap(mbitmap,0,0,width, width);
//        /*Bitmap inputBitmap = Bitmap.createScaledBitmap(mbitmap, INPUT_SZIE, INPUT_SZIE, false);*/
//        final Tensor inputTensor = TensorImageUtils.bitmapToFloat32Tensor(inputBitmap,
//                mean, std);
//
////     forward input tensor
//        IValue inputs = IValue.from(inputTensor);
//        Tensor outputs = mModule.forward(inputs).toTensor();
//        float[] outputValues = outputs.getDataAsFloatArray();
//        Log.d(TAG, "run: outputValues " + Arrays.toString(outputs.getDataAsFloatArray()));
//        Bitmap outputBitmap = Bitmap.createBitmap(224 * 4, 224 * 4, mbitmap.getConfig());
//        outputBitmap = arrayFloatToBitmap(width * 4, width * 4, outputValues);
//        Bitmap finalOutputBitmap = outputBitmap;
//        runOnUiThread(() -> {
//            msubsamplingimageview.setImage(ImageSource.bitmap(inputBitmap));
//            getMsubsamplingimageview_result.setOrientation(originalOrientation);
//            getMsubsamplingimageview_result.setImage(ImageSource.bitmap(finalOutputBitmap));
//            mButtonStart.setEnabled(true);
//            mButtonStart.setText("Ran model");
//            mPregressBar.setVisibility(ProgressBar.INVISIBLE);
//        });
//
//    }
//
//    private Bitmap arrayFloatToBitmap(int width, int height, float[] pixels) {
//
//        byte alpha = (byte) 255;
//        Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
//
//        ByteBuffer byteBuffer = ByteBuffer.allocate(width * height * 4 * 3);
//
//        float maxValue = pixels[0];
//        float minValue = pixels[0];
//        for (int i = 1; i < pixels.length; i++) {
//            if (pixels[i] > maxValue) {
//                maxValue = pixels[i];
//            }
//            if (pixels[i] < minValue) {
//                minValue = pixels[i];
//            }
//        }
//        float delta = maxValue - minValue;
//
//        for (int i = 0; i < pixels.length; i++) {
//            int c = i / (height * width);
//            int buffer_idx = i % (height * width);
//            byte temValue = (byte) ((byte) ((((pixels[i] - minValue) / delta) * 255)));
//            byteBuffer.put(buffer_idx * 4 + c, temValue);
//        }
//        for (int i = 0; i < height * width; i++) {
//            byteBuffer.put(i * 4 + 3, alpha);
//        }
//        bmp.copyPixelsFromBuffer(byteBuffer);
//        return bmp;
//    }
//
//    private int getOrientation(String filePath){
//        try {
//            ExifInterface exif = new ExifInterface(filePath);
//            int rotation = exif.getAttributeInt(ExifInterface. TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
//            return exifToDegrees(rotation);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        return 0;
//    }
//
//    public static int exifToDegrees(int exifOrientation) {
//        if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_90) { return 90; }
//        else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_180) {  return 180; }
//        else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_270) {  return 270; }
//        return 0;
//    }
//
//    private String getPathFromUri(Uri uri){
//        String[] filePathColumn = {MediaStore.Images.Media.DATA};
//        Cursor cursor = getContentResolver().query(uri, filePathColumn, null, null, null);
//        if(cursor.moveToFirst()){
//            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
//            return cursor.getString(columnIndex);
//        } else {
//            //boooo, cursor doesn't have rows ...
//        }
//        cursor.close();
//        return null;
//    }
//
//    public Bitmap getResizedBitmap(Bitmap bm, int newWidth, int newHeight) {
//        int width = bm.getWidth();
//        int height = bm.getHeight();
//        float scaleWidth = ((float) newWidth) / width;
//        float scaleHeight = ((float) newHeight) / height;
//        // CREATE A MATRIX FOR THE MANIPULATION
//        Matrix matrix = new Matrix();
//        // RESIZE THE BIT MAP
//        matrix.postScale(scaleWidth, scaleHeight);
//
//        // "RECREATE" THE NEW BITMAP
//        Bitmap resizedBitmap = Bitmap.createBitmap(
//                bm, 0, 0, width, height, matrix, false);
//        bm.recycle();
//        return resizedBitmap;
//    }
//
//}
//






