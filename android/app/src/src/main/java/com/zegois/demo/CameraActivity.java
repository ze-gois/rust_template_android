package com.zegois.demo;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.io.FileOutputStream;

public class CameraActivity extends Activity {
    private static final int CAMERA_REQUEST = 100;
    private ImageView preview;
    private TextView status;

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
        setTitle("Câmera");

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(dp(12), dp(12), dp(12), dp(12));

        TextView title = new TextView(this);
        title.setText("Câmera via Intent");
        title.setTextSize(24);
        title.setPadding(dp(8), dp(8), dp(8), dp(8));
        add(root, title);

        TextView explanation = new TextView(this);
        explanation.setText("A aplicação pede uma imagem à câmera externa. "
                + "O resultado desta demonstração é a miniatura retornada pelo Intent.");
        explanation.setTextSize(15);
        explanation.setPadding(dp(8), dp(8), dp(8), dp(8));
        add(root, explanation);

        Button capture = new Button(this);
        capture.setText("Abrir câmera");
        capture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openCamera();
            }
        });
        add(root, capture);

        preview = new ImageView(this);
        preview.setBackgroundColor(0xFFE5EAF0);
        preview.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        preview.setContentDescription("Imagem capturada pela câmera");
        root.addView(preview, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(280)));

        status = new TextView(this);
        status.setText("Nenhuma imagem capturada.");
        status.setTextSize(14);
        status.setPadding(dp(8), dp(8), dp(8), dp(8));
        add(root, status);

        setContentView(root);
    }

    private void openCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) == null) {
            status.setText("Nenhuma aplicação de câmera disponível.");
            return;
        }
        startActivityForResult(intent, CAMERA_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != CAMERA_REQUEST) {
            return;
        }
        if (resultCode != RESULT_OK || data == null || data.getExtras() == null) {
            status.setText("Captura cancelada ou sem resultado.");
            return;
        }

        Bitmap image = (Bitmap) data.getExtras().get("data");
        if (image == null) {
            status.setText("A câmera não retornou uma miniatura.");
            return;
        }

        preview.setImageBitmap(image);
        String fileName = "camera_" + System.currentTimeMillis() + ".png";
        try {
            FileOutputStream output = openFileOutput(fileName, MODE_PRIVATE);
            image.compress(Bitmap.CompressFormat.PNG, 100, output);
            output.close();
            status.setText("Imagem exibida e salva em filesDir como:\n" + fileName
                    + "\nTamanho: " + image.getWidth() + " x " + image.getHeight());
        } catch (Exception error) {
            status.setText("Imagem exibida, mas não foi possível salvar:\n" + error);
        }
    }
}
