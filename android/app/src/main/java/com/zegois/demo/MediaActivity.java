package com.zegois.demo;

import android.app.Activity;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.io.File;
import java.io.FileOutputStream;

public class MediaActivity extends Activity {
    private MediaPlayer player;
    private File wavFile;
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
        setTitle("Áudio");

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(12), dp(12), dp(12), dp(12));

        TextView title = new TextView(this);
        title.setText("Arquivo e reprodução de áudio");
        title.setTextSize(24);
        title.setPadding(dp(8), dp(8), dp(8), dp(8));
        add(root, title);

        TextView explanation = new TextView(this);
        explanation.setText("O app gera um WAV PCM diretamente em filesDir e o reproduz com MediaPlayer.\n"
                + "Não há arquivo de áudio empacotado nem biblioteca externa.");
        explanation.setTextSize(15);
        explanation.setPadding(dp(8), dp(8), dp(8), dp(8));
        add(root, explanation);

        Button generate = new Button(this);
        generate.setText("Gerar WAV em filesDir");
        generate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                generateWav();
            }
        });
        add(root, generate);

        Button play = new Button(this);
        play.setText("Reproduzir arquivo");
        play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                playWav();
            }
        });
        add(root, play);

        Button stop = new Button(this);
        stop.setText("Parar reprodução");
        stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopWav();
            }
        });
        add(root, stop);

        status = new TextView(this);
        status.setText("Nenhum arquivo gerado.");
        status.setTextSize(15);
        status.setPadding(dp(8), dp(16), dp(8), dp(8));
        add(root, status);

        setContentView(root);
    }

    private void generateWav() {
        final int sampleRate = 22050;
        final int seconds = 2;
        final int samples = sampleRate * seconds;
        final int dataSize = samples * 2;
        wavFile = new File(getFilesDir(), "demo-tone.wav");

        try {
            FileOutputStream output = new FileOutputStream(wavFile);
            output.write(new byte[] {'R', 'I', 'F', 'F'});
            writeIntLE(output, 36 + dataSize);
            output.write(new byte[] {'W', 'A', 'V', 'E'});
            output.write(new byte[] {'f', 'm', 't', ' '});
            writeIntLE(output, 16);
            writeShortLE(output, 1);
            writeShortLE(output, 1);
            writeIntLE(output, sampleRate);
            writeIntLE(output, sampleRate * 2);
            writeShortLE(output, 2);
            writeShortLE(output, 16);
            output.write(new byte[] {'d', 'a', 't', 'a'});
            writeIntLE(output, dataSize);

            for (int index = 0; index < samples; index++) {
                double time = (double) index / sampleRate;
                double envelope = Math.min(1.0, Math.min(time * 20.0,
                        (seconds - time) * 20.0));
                short sample = (short) (Math.sin(time * Math.PI * 2.0 * 440.0)
                        * 12000.0 * envelope);
                writeShortLE(output, sample);
            }
            output.close();
            status.setText("WAV criado:\n" + wavFile.getAbsolutePath()
                    + "\n" + wavFile.length() + " bytes");
        } catch (Exception error) {
            status.setText("Erro ao gerar WAV: " + error);
        }
    }

    private void playWav() {
        if (wavFile == null || !wavFile.exists()) {
            generateWav();
        }
        stopWav();
        try {
            player = new MediaPlayer();
            player.setDataSource(wavFile.getAbsolutePath());
            player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer completed) {
                    status.setText("Reprodução concluída: " + wavFile.getName());
                    completed.release();
                    player = null;
                }
            });
            player.prepare();
            player.start();
            status.setText("Reproduzindo: " + wavFile.getName());
        } catch (Exception error) {
            status.setText("Erro ao reproduzir WAV: " + error);
            stopWav();
        }
    }

    private void stopWav() {
        if (player != null) {
            if (player.isPlaying()) {
                player.stop();
            }
            player.release();
            player = null;
            status.setText("Reprodução parada.");
        }
    }

    private void writeIntLE(FileOutputStream output, int value) throws Exception {
        output.write(value & 0xff);
        output.write((value >> 8) & 0xff);
        output.write((value >> 16) & 0xff);
        output.write((value >> 24) & 0xff);
    }

    private void writeShortLE(FileOutputStream output, int value) throws Exception {
        output.write(value & 0xff);
        output.write((value >> 8) & 0xff);
    }

    @Override
    protected void onDestroy() {
        stopWav();
        super.onDestroy();
    }
}
