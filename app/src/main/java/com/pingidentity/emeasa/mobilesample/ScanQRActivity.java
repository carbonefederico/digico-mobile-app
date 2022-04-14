package com.pingidentity.emeasa.mobilesample;

import static java.lang.Math.abs;
import static java.lang.Math.min;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.Barcode;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.common.InputImage;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ScanQRActivity extends AppCompatActivity {

    private static final String TAG = ScanQRActivity.class.getSimpleName();
    private final double  RATIO_4_3_VALUE = 4.0 / 3.0;
    private final double RATIO_16_9_VALUE = 16.0 / 9.0;
    /** Blocking camera operations are performed using this executor */
    private ExecutorService cameraExecutor;
    private PreviewView previewView;

    /*
     * permission request codes (need to be < 256)
     */
    private static final int RC_HANDLE_CAMERA_PERM = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_qr);

        // Initialize background executor each time the view is recreated
        cameraExecutor = Executors.newSingleThreadExecutor();
        previewView = findViewById(R.id.camera_preview);

        /*
         * Check for the camera permission before accessing the camera.  If the
         * permission is not granted yet, request permission.
         */
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG,"Permissions granted, showing camera preview");
            startCameraPreview();
        } else {
            Log.d(TAG,"Permissions not granted, asking for permissions");
            requestCameraPermission();
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        // If request is cancelled, the result arrays are empty.
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == RC_HANDLE_CAMERA_PERM) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // permission was granted
                startCameraPreview();
            } else {
                Toast.makeText(this, "Camera Disabled", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private int aspectRatio(int width, int height) {
        double previewRatio = (double) Math.max(width, height) /min(width, height);
        if (abs(previewRatio - RATIO_4_3_VALUE) <= abs(previewRatio - RATIO_16_9_VALUE)) {
            return AspectRatio.RATIO_4_3;
        }
        return AspectRatio.RATIO_16_9;
    }

    /**
     * Declare and bind preview and analysis use cases
     */
    private void bindCameraUseCases(){

        DisplayMetrics metrics = new DisplayMetrics();
        previewView.getDisplay().getRealMetrics(metrics);
        final int rotation = previewView.getDisplay().getRotation();
        final int screenAspectRatio = aspectRatio(metrics.widthPixels, metrics.heightPixels);
        //camera selector
        final CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        final ListenableFuture futureCameraProvider = ProcessCameraProvider.getInstance(this);
        futureCameraProvider.addListener(() -> {
            try{
                Log.d(TAG,"Camera provider listener start");
                ProcessCameraProvider processCameraProvider = (ProcessCameraProvider) futureCameraProvider.get();


                // Preview
                Preview preview = new Preview.Builder()
                        .setTargetAspectRatio(screenAspectRatio)
                        // Set initial target rotation
                        .setTargetRotation(rotation)
                        .build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                //QrCode analyzer
                ImageAnalysis analysis = new ImageAnalysis.Builder()
                        // Set initial target rotation, we will have to call this again if rotation changes
                        // during the lifecycle of this use case
                        .setTargetRotation(rotation)
                        .build();
                analysis.setAnalyzer(cameraExecutor, new QrCodeAnalyzer());

                processCameraProvider.unbindAll();

                processCameraProvider.bindToLifecycle(this, cameraSelector, preview, analysis);
            }catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private class QrCodeAnalyzer implements ImageAnalysis.Analyzer{

        @SuppressLint("UnsafeExperimentalUsageError")
        @Override
        public void analyze(@NonNull ImageProxy imageProxy) {
            Log.d(TAG,"Starting analyze");
            Image mediaImage = imageProxy.getImage();
            if (mediaImage == null) {
                Log.d(TAG,"Image null, returning");
                return;
            }

            InputImage image =
                    InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());

            BarcodeScanner scanner = BarcodeScanning.getClient();
            Task<List<Barcode>> result = scanner.process(image)
                    .addOnSuccessListener(new OnSuccessListener<List<Barcode>>() {
                        @Override
                        public void onSuccess(List<Barcode> barcodes) {
                            Log.d(TAG,"Successful scan " + barcodes.toString());
                            for (Barcode barcode: barcodes) {
                                String rawValue = barcode.getRawValue();
                                if (!TextUtils.isEmpty(rawValue))   {
                                    Log.d (TAG,"Decoded value " + rawValue);
                                    cameraExecutor.shutdown();
                                    Intent i = new Intent(ScanQRActivity.this,MainActivity.class);
                                    i.setData(Uri.parse(rawValue));
                                    startActivity(i);
                                }
                                imageProxy.close();
                            }
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.d(TAG,"Error decoding barcode");
                        }
                    })
                    .addOnCompleteListener(new OnCompleteListener<List<Barcode>>() {
                        @Override
                        public void onComplete(@NonNull Task<List<Barcode>> task) {
                            imageProxy.close();
                        }
                    })
                    ;
        }
    }

    private void startCameraPreview(){
        previewView.post(this::bindCameraUseCases);
    }

    /*
     * Handles the requesting of the camera permission.
     */
    private void requestCameraPermission() {

        final String[] permissions = new String[]{Manifest.permission.CAMERA};
        /*
         * returns true if the user has previously denied the request, and returns false if a user
         * has denied a permission and selected the Don't ask again option in the permission request
         * dialog
         */
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.CAMERA)) {
            /*
             * Show an explanation to the user *asynchronously* -- don't block
             * this thread waiting for the user's response! After the user
             * sees the explanation, try again to request the permission.
             */
            Toast.makeText(this, "Camera Disabled", Toast.LENGTH_SHORT).show();
        } else {
            requestPermissions(permissions, RC_HANDLE_CAMERA_PERM);
        }

    }


}