package com.zegois.demo;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;

public class StorageActivity extends Activity {
    private final ArrayList<String> names = new ArrayList<String>();
    private ArrayAdapter<String> adapter;
    private TextView status;
    private TextView contents;

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private TextView text(String value, float size) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setPadding(dp(8), dp(8), dp(8), dp(8));
        return view;
    }

    private void add(LinearLayout parent, View child) {
        parent.addView(child, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setTitle("Storage privado");

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(12), dp(12), dp(12), dp(12));

        add(root, text("Arquivos privados do aplicativo", 24));
        add(root, text("filesDir: " + getFilesDir().getAbsolutePath(), 13));
        add(root, text("cacheDir: " + getCacheDir().getAbsolutePath(), 13));

        final EditText name = new EditText(this);
        name.setHint("Nome: exemplo.txt");
        name.setText("demo.txt");
        add(root, name);

        final EditText value = new EditText(this);
        value.setHint("Conteúdo do arquivo");
        value.setMinLines(3);
        value.setText("Arquivo criado pela demo Android + Rust.");
        add(root, value);

        Button save = new Button(this);
        save.setText("Salvar em filesDir");
        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveFile(name.getText().toString(), value.getText().toString());
            }
        });
        add(root, save);

        Button refresh = new Button(this);
        refresh.setText("Atualizar lista");
        refresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                refreshFiles();
            }
        });
        add(root, refresh);

        status = text("Toque em um arquivo para lê-lo.", 14);
        add(root, status);

        ListView list = new ListView(this);
        adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, names);
        list.setAdapter(adapter);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                readFile(names.get(position));
            }
        });
        root.addView(list, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(220)));

        contents = text("Conteúdo lido:", 15);
        contents.setBackgroundColor(0xFFEFF5EF);
        add(root, contents);

        Button clear = new Button(this);
        clear.setText("Apagar arquivos da demo");
        clear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                for (String fileName : new ArrayList<String>(names)) {
                    deleteFile(fileName);
                }
                refreshFiles();
                contents.setText("Conteúdo lido:");
            }
        });
        add(root, clear);

        setContentView(root);
        refreshFiles();
    }

    private String safeName(String value) {
        String result = value.replaceAll("[^a-zA-Z0-9._-]", "_");
        return result.length() == 0 ? "demo.txt" : result;
    }

    private void saveFile(String requestedName, String value) {
        String fileName = safeName(requestedName);
        try {
            OutputStreamWriter writer = new OutputStreamWriter(
                    openFileOutput(fileName, MODE_PRIVATE), "UTF-8");
            writer.write(value);
            writer.close();
            status.setText("Salvo: " + fileName);
            refreshFiles();
        } catch (Exception error) {
            status.setText("Erro ao salvar: " + error.getMessage());
        }
    }

    private void readFile(String fileName) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    new FileInputStream(new File(getFilesDir(), fileName)), "UTF-8"));
            StringBuilder result = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line).append('\n');
            }
            reader.close();
            contents.setText("Conteúdo de " + fileName + ":\n" + result.toString());
        } catch (Exception error) {
            contents.setText("Não foi possível ler " + fileName + ":\n" + error);
        }
    }

    private void refreshFiles() {
        names.clear();
        File[] files = getFilesDir().listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    names.add(file.getName());
                }
            }
        }
        adapter.notifyDataSetChanged();
        status.setText(names.size() + " arquivo(s) em filesDir");
    }
}
