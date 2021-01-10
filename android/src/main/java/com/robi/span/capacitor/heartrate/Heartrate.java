package com.robi.span.capacitor.heartrate;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.getcapacitor.JSObject;
import com.getcapacitor.NativePlugin;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.google.common.util.concurrent.ListenableFuture;

import java.nio.ByteBuffer;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

import static com.robi.span.capacitor.heartrate.Util.setInterval;

@NativePlugin(requestCodes = {Heartrate.REQUEST_CAMERA}, permissions = {Manifest.permission.CAMERA})
public class Heartrate extends Plugin implements ImageAnalysis.Analyzer {

    protected static final int REQUEST_CAMERA = 12345; // Unique request code

    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;

    ProcessCameraProvider cameraProvider;

    private JSObject latestLumiPoint = new JSObject();

    public Boolean analysisTimeout = false;

    @PluginMethod
    public void getLuminosityFeed(PluginCall call) {
        saveCall(call);

        Boolean hasPermissions = hasRequiredPermissions();

        if (!hasPermissions) {
            pluginRequestAllPermissions();
        } else {
            getCameraFeed();
            sendCallSuccessToIonic(call);
        }
    }

    @PluginMethod
    public void stopAnalysis(PluginCall call) {
        analysisTimeout = true;
        sendCallSuccessToIonic(call);
    }

    void bindPreview(@NonNull ProcessCameraProvider cameraProvider) {

        Executor executor = ContextCompat.getMainExecutor(getContext());

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK).build();

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setTargetResolution(new Size(50, 50))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        imageAnalysis.setAnalyzer(executor, this::analyze);

        // Unbind use cases before rebinding
        cameraProvider.unbindAll();
        cameraProvider.bindToLifecycle((LifecycleOwner) getContext(), cameraSelector, imageAnalysis);

        setInterval(() -> {
            sendFeedToIonic(latestLumiPoint);
        }, 100);
    }

    public void getCameraFeed() {
        Executor executor = ContextCompat.getMainExecutor(getContext());

        cameraProviderFuture = ProcessCameraProvider.getInstance(getContext());

        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                bindPreview(cameraProvider);
            } catch (InterruptedException | ExecutionException e) {
                // No errors need to be handled for this Future.
                // This should never be reached.
            }
        }, executor);
    }

    private void sendFeedToIonic(JSObject data) {
        JSObject ret = new JSObject();
        ret.put("data", data);

        bridge.triggerWindowJSEvent("feed", data.toString());
    }

    private void sendCallSuccessToIonic(PluginCall call) {
        JSObject ret = new JSObject();
        ret.put("callStatus", "success");
        call.success(ret);
    }

    @Override
    public void analyze(ImageProxy image) {
        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        byte[] data = Util.toByteArray(buffer);

        ArrayList<Integer> pixels = new ArrayList<Integer>();
        for (int i = 0; i < data.length; i++) {
            int res = (int) data[i] + 0xFF;
            pixels.add(res);
        }

        Integer sum = 0;
        if (!pixels.isEmpty()) for (Integer mark : pixels) sum += mark;

        double luminosity = sum.doubleValue() / pixels.size();
        Long tsLong = System.currentTimeMillis();
        String time = tsLong.toString();

        JSObject dataPoint = new JSObject();
        dataPoint.put("time", time);
        dataPoint.put("dataPoint", luminosity);

        latestLumiPoint = dataPoint;

        image.close();

        if (analysisTimeout) cameraProvider.unbindAll();
    }

    @Override
    protected void handleRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.handleRequestPermissionsResult(requestCode, permissions, grantResults);

        PluginCall savedCall = getSavedCall();
        if (savedCall == null) {
            Log.d("status", "No stored plugin call for permissions request result");
            return;
        }

        for (int result : grantResults) {
            if (result == PackageManager.PERMISSION_DENIED) {
                savedCall.error("User denied permission");
                return;
            }
        }

        if (requestCode == REQUEST_CAMERA) {
            // We got the permission
            getCameraFeed();
            sendCallSuccessToIonic(savedCall);
        }
    }
}
