package com.zegois.demo;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.nio.ByteBuffer;
import java.util.Collections;

public class NativeCameraActivity extends Activity {
    private static final int CAMERA_PERMISSION = 200;
    private static final int FRAME_WIDTH = 320;
    private static final int FRAME_HEIGHT = 240;

    static {
        System.loadLibrary("zegois_android");
    }

    public native int processFrame(int[] pixels, int width, int height);

    private CameraDevice camera;
    private CameraCaptureSession session;
    private ImageReader reader;
    private HandlerThread cameraThread;
    private Handler cameraHandler;
    private ImageView output;
    private TextView status;
    private boolean running;
    private long frameNumber;

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void add(LinearLayout parent, View child) {
        parent.addView(child, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setTitle("Câmera + Rust em tempo real");

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(dp(12), dp(12), dp(12), dp(12));
        root.setKeepScreenOn(true);

        TextView title = new TextView(this);
        title.setText("Filtro de imagem em Rust");
        title.setTextSize(24);
        title.setPadding(dp(8), dp(8), dp(8), dp(8));
        add(root, title);

        TextView explanation = new TextView(this);
        explanation.setText("Camera2 captura YUV, Java monta pixels ARGB, Rust aplica "
                + "um filtro de cor no mesmo buffer e o resultado volta para esta tela.");
        explanation.setTextSize(14);
        explanation.setPadding(dp(8), dp(8), dp(8), dp(8));
        add(root, explanation);

        output = new ImageView(this);
        output.setBackgroundColor(Color.BLACK);
        output.setScaleType(ImageView.ScaleType.FIT_CENTER);
        output.setContentDescription("Frame processado pelo Rust");
        root.addView(output, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(300)));

        status = new TextView(this);
        status.setText("Câmera parada.");
        status.setTextSize(14);
        status.setPadding(dp(8), dp(8), dp(8), dp(8));
        add(root, status);

        Button start = new Button(this);
        start.setText("Iniciar câmera + filtro Rust");
        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                requestCameraAndStart();
            }
        });
        add(root, start);

        Button stop = new Button(this);
        stop.setText("Parar câmera");
        stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopCamera();
            }
        });
        add(root, stop);

        setContentView(root);
        requestCameraAndStart();
    }

    private void requestCameraAndStart() {
        if (Build.VERSION.SDK_INT >= 23
                && checkSelfPermission(Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[] {Manifest.permission.CAMERA}, CAMERA_PERMISSION);
            status.setText("Permissão de câmera solicitada.");
            return;
        }
        startCamera();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode == CAMERA_PERMISSION
                && results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else if (requestCode == CAMERA_PERMISSION) {
            status.setText("Permissão de câmera negada.");
        }
    }

    private void startCamera() {
        if (running) {
            return;
        }
        try {
            cameraThread = new HandlerThread("rust-camera");
            cameraThread.start();
            cameraHandler = new Handler(cameraThread.getLooper());

            reader = ImageReader.newInstance(FRAME_WIDTH, FRAME_HEIGHT,
                    android.graphics.ImageFormat.YUV_420_888, 2);
            reader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader source) {
                    processLatestFrame(source);
                }
            }, cameraHandler);

            CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
            String cameraId = chooseBackCamera(manager);
            if (cameraId == null) {
                status.setText("Nenhuma câmera encontrada.");
                stopCamera();
                return;
            }
            manager.openCamera(cameraId, cameraState, cameraHandler);
            running = true;
            status.setText("Abrindo câmera...");
        } catch (Exception error) {
            status.setText("Erro ao iniciar câmera: " + error);
            stopCamera();
        }
    }

    private String chooseBackCamera(CameraManager manager) throws Exception {
        String fallback = null;
        for (String id : manager.getCameraIdList()) {
            if (fallback == null) {
                fallback = id;
            }
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(id);
            Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
            if (facing != null && facing == CameraCharacteristics.LENS_FACING_BACK) {
                return id;
            }
        }
        return fallback;
    }

    private final CameraDevice.StateCallback cameraState = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice opened) {
            camera = opened;
            try {
                CaptureRequest.Builder request = camera.createCaptureRequest(
                        CameraDevice.TEMPLATE_PREVIEW);
                request.addTarget(reader.getSurface());
                request.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO);
                camera.createCaptureSession(Collections.singletonList(reader.getSurface()),
                        new CameraCaptureSession.StateCallback() {
                            @Override
                            public void onConfigured(CameraCaptureSession configured) {
                                session = configured;
                                try {
                                    session.setRepeatingRequest(request.build(), null, cameraHandler);
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            status.setText("Capturando e processando em Rust...");
                                        }
                                    });
                                } catch (Exception error) {
                                    status.setText("Erro no stream: " + error);
                                }
                            }

                            @Override
                            public void onConfigureFailed(CameraCaptureSession failed) {
                                status.setText("Falha ao configurar sessão da câmera.");
                            }
                        }, cameraHandler);
            } catch (Exception error) {
                status.setText("Erro ao criar captura: " + error);
            }
        }

        @Override
        public void onDisconnected(CameraDevice disconnected) {
            disconnected.close();
            camera = null;
        }

        @Override
        public void onError(CameraDevice failed, int error) {
            failed.close();
            camera = null;
            status.setText("Erro da câmera: " + error);
        }
    };

    private void processLatestFrame(ImageReader source) {
        Image image = source.acquireLatestImage();
        if (image == null) {
            return;
        }
        try {
            int width = image.getWidth();
            int height = image.getHeight();
            int[] pixels = yuvToArgb(image);
            int processed = processFrame(pixels, width, height);
            final Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
            final long frame = ++frameNumber;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    output.setImageBitmap(bitmap);
                    status.setText("Frame " + frame + " processado em Rust: " + processed + " pixels");
                }
            });
        } finally {
            image.close();
        }
    }

    private int[] yuvToArgb(Image image) {
        int width = image.getWidth();
        int height = image.getHeight();
        Image.Plane[] planes = image.getPlanes();
        byte[][] data = new byte[3][];
        for (int index = 0; index < 3; index++) {
            ByteBuffer buffer = planes[index].getBuffer();
            data[index] = new byte[buffer.remaining()];
            buffer.get(data[index]);
        }

        int[] pixels = new int[width * height];
        int[] rowStride = new int[] {
                planes[0].getRowStride(), planes[1].getRowStride(), planes[2].getRowStride()
        };
        int[] pixelStride = new int[] {
                planes[0].getPixelStride(), planes[1].getPixelStride(), planes[2].getPixelStride()
        };

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int yIndex = y * rowStride[0] + x * pixelStride[0];
                int uvX = x / 2;
                int uvY = y / 2;
                int uIndex = uvY * rowStride[1] + uvX * pixelStride[1];
                int vIndex = uvY * rowStride[2] + uvX * pixelStride[2];
                int yValue = data[0][yIndex] & 0xff;
                int uValue = (data[1][uIndex] & 0xff) - 128;
                int vValue = (data[2][vIndex] & 0xff) - 128;
                int red = clamp(yValue + (int) (1.402f * vValue));
                int green = clamp(yValue - (int) (0.344f * uValue + 0.714f * vValue));
                int blue = clamp(yValue + (int) (1.772f * uValue));
                pixels[y * width + x] = Color.rgb(red, green, blue);
            }
        }
        return pixels;
    }

    private int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }

    private void stopCamera() {
        running = false;
        if (session != null) {
            session.close();
            session = null;
        }
        if (camera != null) {
            camera.close();
            camera = null;
        }
        if (reader != null) {
            reader.close();
            reader = null;
        }
        if (cameraThread != null) {
            cameraThread.quitSafely();
            cameraThread = null;
            cameraHandler = null;
        }
        if (status != null) {
            status.setText("Câmera parada.");
        }
    }

    @Override
    protected void onPause() {
        stopCamera();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        stopCamera();
        super.onDestroy();
    }
}
