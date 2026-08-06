package com.zegois.demo;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.inputmethod.EditorInfo;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.text.InputType;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.HorizontalScrollView;

import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.NumberPicker;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RatingBar;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.ToggleButton;
import android.widget.ScrollView;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class MainActivity extends Activity {
    private final ArrayList<String> spreadsheetItems = new ArrayList<String>();
    private ArrayAdapter<String> spreadsheetItemAdapter;

    static {
        System.loadLibrary("zegois_android");
    }

    public native int answer();

    public native long fibonacci(int n);

    public native int checksum(int value);

    public native int transform(int value);

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private TextView text(String value, float size) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(Color.rgb(30, 30, 30));
        view.setPadding(dp(4), dp(4), dp(4), dp(4));
        return view;
    }

    private void add(LinearLayout parent, View child) {
        parent.addView(child, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    private void addSpace(LinearLayout parent, int heightDp) {
        View space = new View(this);
        parent.addView(space, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(heightDp)));
    }

    private LinearLayout section(LinearLayout parent, String title) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(12), dp(8), dp(12), dp(12));
        box.setBackgroundColor(Color.rgb(245, 247, 250));

        TextView heading = text(title, 20);
        heading.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        heading.setTextColor(Color.rgb(20, 70, 120));
        add(box, heading);
        add(parent, box);
        addSpace(parent, 12);
        return box;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final int rustAnswer = answer();
        android.util.Log.i("ZEGOIS", "Rust retornou: " + rustAnswer);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(12), dp(12), dp(12), dp(24));
        page.setBackgroundColor(Color.WHITE);
        scroll.addView(page, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView title = text("Android Native Reference Demo", 28);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setTextColor(Color.rgb(15, 60, 100));
        add(page, title);

        TextView subtitle = text(
                "Views nativas, eventos, desenho e JNI sem AndroidX", 16);
        subtitle.setTextColor(Color.DKGRAY);
        add(page, subtitle);
        addSpace(page, 8);

        LinearLayout textSection = section(page, "1. Texto e entrada");
        add(textSection, text(
                "TextView: texto simples, multilinha e redimensionável.\n"
                        + "Cada seção desta tela é construída diretamente em Java.", 16));

        final EditText input = new EditText(this);
        input.setHint("EditText: escreva alguma coisa");
        input.setSingleLine(false);
        input.setMinLines(2);
        add(textSection, input);

        final TextView echo = text("O texto digitado aparecerá aqui.", 16);
        add(textSection, echo);

        Button echoButton = new Button(this);
        echoButton.setText("Copiar texto para TextView");
        echoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                echo.setText("Texto recebido: " + input.getText().toString());
            }
        });
        add(textSection, echoButton);

        LinearLayout actionSection = section(page, "2. Ações e seleção");
        Button button = new Button(this);
        button.setText("Button: ação simples");
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                android.widget.Toast.makeText(MainActivity.this,
                        "Button clicado", android.widget.Toast.LENGTH_SHORT).show();
            }
        });
        add(actionSection, button);

        ImageButton imageButton = new ImageButton(this);
        imageButton.setImageResource(android.R.drawable.ic_menu_info_details);
        imageButton.setContentDescription("ImageButton de informação");
        imageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showInfoDialog();
            }
        });
        add(actionSection, imageButton);

        CheckBox checkBox = new CheckBox(this);
        checkBox.setText("CheckBox: opção independente");
        add(actionSection, checkBox);

        RadioGroup radioGroup = new RadioGroup(this);
        RadioButton firstRadio = new RadioButton(this);
        firstRadio.setId(View.generateViewId());
        firstRadio.setText("RadioButton: primeira opção");
        RadioButton secondRadio = new RadioButton(this);
        secondRadio.setId(View.generateViewId());
        secondRadio.setText("RadioButton: segunda opção");
        radioGroup.addView(firstRadio);
        radioGroup.addView(secondRadio);
        radioGroup.check(firstRadio.getId());
        add(actionSection, radioGroup);

        Switch switchView = new Switch(this);
        switchView.setText("Switch: estado ligado/desligado");
        add(actionSection, switchView);

        ToggleButton toggle = new ToggleButton(this);
        toggle.setTextOn("ToggleButton ligado");
        toggle.setTextOff("ToggleButton desligado");
        add(actionSection, toggle);

        LinearLayout valueSection = section(page, "3. Valores e seleção");
        Spinner spinner = new Spinner(this);
        String[] colors = {"Spinner: vermelho", "Spinner: verde", "Spinner: azul"};
        spinner.setAdapter(new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, colors));
        add(valueSection, spinner);

        final TextView seekValue = text("SeekBar: 50", 16);
        add(valueSection, seekValue);
        SeekBar seekBar = new SeekBar(this);
        seekBar.setMax(100);
        seekBar.setProgress(50);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                seekValue.setText("SeekBar: " + progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar bar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar bar) {
            }
        });
        add(valueSection, seekBar);

        ProgressBar progress = new ProgressBar(this, null,
                android.R.attr.progressBarStyleHorizontal);
        progress.setMax(100);
        progress.setProgress(65);
        add(valueSection, progress);

        RatingBar rating = new RatingBar(this, null, android.R.attr.ratingBarStyle);
        rating.setNumStars(5);
        rating.setRating(3.5f);
        add(valueSection, rating);

        NumberPicker number = new NumberPicker(this);
        number.setMinValue(0);
        number.setMaxValue(10);
        number.setValue(3);
        add(valueSection, number);

        Button dateButton = new Button(this);
        dateButton.setText("DatePickerDialog");
        dateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Calendar now = Calendar.getInstance();
                new DatePickerDialog(MainActivity.this, null,
                        now.get(Calendar.YEAR), now.get(Calendar.MONTH),
                        now.get(Calendar.DAY_OF_MONTH)).show();
            }
        });
        add(valueSection, dateButton);

        Button timeButton = new Button(this);
        timeButton.setText("TimePickerDialog");
        timeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Calendar now = Calendar.getInstance();
                new TimePickerDialog(MainActivity.this, null,
                        now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE),
                        true).show();
            }
        });
        add(valueSection, timeButton);

        LinearLayout imageSection = section(page, "4. Imagem, Drawable e Canvas");
        ImageView image = new ImageView(this);
        image.setImageResource(android.R.drawable.ic_dialog_info);
        image.setContentDescription("Imagem drawable do sistema");
        image.setAdjustViewBounds(true);
        image.setBackgroundColor(Color.rgb(225, 235, 245));
        image.setPadding(dp(30), dp(20), dp(30), dp(20));
        add(imageSection, image);

        add(imageSection, text("Imagem criada a partir de um drawable do framework.", 14));
        add(imageSection, new PaintedView());

        LinearLayout listSection = section(page, "5. ListView e Adapter");
        add(listSection, text(
                "ListView recebe dados através de um Adapter. A altura fixa mantém a lista dentro da demonstração rolável.", 14));
        ListView list = new ListView(this);
        String[] entries = {
                "Item 0: Android View", "Item 1: Java", "Item 2: JNI",
                "Item 3: Rust", "Item 4: ELF", "Item 5: DEX"
        };
        list.setAdapter(new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, entries));
        listSection.addView(list, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(240)));

        LinearLayout dialogSection = section(page, "6. Dialog, Toast e menu");
        Button dialogButton = new Button(this);
        dialogButton.setText("AlertDialog");
        dialogButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showInfoDialog();
            }
        });
        add(dialogSection, dialogButton);

        Button popupButton = new Button(this);
        popupButton.setText("PopupWindow");
        popupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showPopup(view);
            }
        });
        add(dialogSection, popupButton);

        LinearLayout rustSection = section(page, "7. JNI e Rust");
        TextView rustText = text("Rust respondeu através de JNI: " + rustAnswer, 20);
        rustText.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        rustText.setTextColor(Color.rgb(150, 70, 20));
        add(rustSection, rustText);

        final TextView rustDetails = text(
                "Fibonacci(20) = " + fibonacci(20)
                        + "\nChecksum(42) = 0x" + Integer.toHexString(checksum(42))
                        + "\nTransform(42) = " + transform(42), 16);
        add(rustSection, rustDetails);

        Button rustButton = new Button(this);
        rustButton.setText("Chamar Rust novamente");
        rustButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                rustText.setText("Rust respondeu novamente: " + answer());
            }
        });
        add(rustSection, rustButton);

        Button rustComputeButton = new Button(this);
        rustComputeButton.setText("Executar operações Rust novamente");
        rustComputeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                rustDetails.setText(
                        "Fibonacci(20) = " + fibonacci(20)
                                + "\nChecksum(42) = 0x" + Integer.toHexString(checksum(42))
                                + "\nTransform(42) = " + transform(42));
            }
        });
        add(rustSection, rustComputeButton);

        LinearLayout spreadsheetSection = section(page, "8. Spreadsheet nativa");
        add(spreadsheetSection, text(
                "Grade editável feita com TableLayout e EditText. Digite números e recalcule a linha Σ.", 15));
        addSpreadsheet(spreadsheetSection);

        LinearLayout deviceSection = section(page, "9. Telas de integração com o dispositivo");
        add(deviceSection, text(
                "Estas telas usam APIs Android concretas: câmera via Intent, filesDir privado e MediaPlayer.", 15));
        add(deviceSection, screenButton("Abrir demo da câmera", CameraActivity.class));
        add(deviceSection, screenButton("Abrir demo de arquivos", StorageActivity.class));
        add(deviceSection, screenButton("Abrir demo de áudio", MediaActivity.class));

        setContentView(scroll);
    }

    private void addSpreadsheet(final LinearLayout parent) {
        final int rowCount = 5;
        final int columnCount = 5;
        final EditText[][] cells = new EditText[rowCount][columnCount];
        final TextView[] totals = new TextView[columnCount];

        loadSpreadsheetItems();
        spreadsheetItemAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_dropdown_item_1line, spreadsheetItems);

        HorizontalScrollView horizontal = new HorizontalScrollView(this);
        TableLayout table = new TableLayout(this);
        table.setPadding(dp(2), dp(8), dp(2), dp(8));

        TableRow header = new TableRow(this);
        addSpreadsheetLabel(header, "", false);
        for (int column = 0; column < columnCount; column++) {
            String label = column == 0 ? "Item" : String.valueOf((char) ('A' + column));
            addSpreadsheetLabel(header, label, true);
        }
        table.addView(header);

        for (int row = 0; row < rowCount; row++) {
            TableRow tableRow = new TableRow(this);
            addSpreadsheetLabel(tableRow, String.valueOf(row + 1), true);
            for (int column = 0; column < columnCount; column++) {
                EditText cell;
                int width = dp(86);
                if (column == 0) {
                    final AutoCompleteTextView item = new AutoCompleteTextView(this);
                    item.setTextSize(16);
                    item.setSingleLine(true);
                    item.setHint("item");
                    item.setThreshold(1);
                    item.setAdapter(spreadsheetItemAdapter);
                    item.setInputType(InputType.TYPE_CLASS_TEXT
                            | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
                    item.setImeOptions(EditorInfo.IME_ACTION_DONE);
                    item.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                        @Override
                        public boolean onEditorAction(TextView view, int actionId,
                                android.view.KeyEvent event) {
                            if (actionId == EditorInfo.IME_ACTION_DONE) {
                                rememberSpreadsheetItem(item.getText().toString());
                                item.dismissDropDown();
                            }
                            return false;
                        }
                    });
                    item.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                        @Override
                        public void onFocusChange(View view, boolean hasFocus) {
                            if (!hasFocus) {
                                rememberSpreadsheetItem(item.getText().toString());
                            }
                        }
                    });
                    cell = item;
                    width = dp(150);
                } else {
                    EditText number = new EditText(this);
                    number.setText(String.valueOf((row + 1) * (column + 1)));
                    number.setTextSize(16);
                    number.setGravity(Gravity.CENTER);
                    number.setSingleLine(true);
                    number.setInputType(InputType.TYPE_CLASS_NUMBER
                            | InputType.TYPE_NUMBER_FLAG_DECIMAL
                            | InputType.TYPE_NUMBER_FLAG_SIGNED);
                    cell = number;
                }
                cells[row][column] = cell;
                tableRow.addView(cell, new TableRow.LayoutParams(width, dp(52)));
            }
            table.addView(tableRow);
        }

        TableRow totalRow = new TableRow(this);
        addSpreadsheetLabel(totalRow, "Σ", true);
        for (int column = 0; column < columnCount; column++) {
            TextView total = new TextView(this);
            total.setText(column == 0 ? "0 itens" : "0.00");
            total.setTextSize(16);
            total.setGravity(Gravity.CENTER);
            total.setTextColor(Color.rgb(20, 90, 50));
            total.setBackgroundColor(Color.rgb(220, 240, 225));
            totals[column] = total;
            int width = column == 0 ? dp(150) : dp(86);
            totalRow.addView(total, new TableRow.LayoutParams(width, dp(52)));
        }
        table.addView(totalRow);
        horizontal.addView(table, new HorizontalScrollView.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        add(parent, horizontal);

        Button calculate = new Button(this);
        calculate.setText("Calcular totais");
        calculate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int itemCount = 0;
                for (int row = 0; row < rowCount; row++) {
                    if (cells[row][0].getText().toString().trim().length() > 0) {
                        itemCount++;
                    }
                }
                totals[0].setText(itemCount + " itens");

                for (int column = 1; column < columnCount; column++) {
                    double sum = 0.0;
                    for (int row = 0; row < rowCount; row++) {
                        try {
                            String value = cells[row][column].getText().toString().trim();
                            if (value.length() > 0) {
                                sum += Double.parseDouble(value);
                            }
                        } catch (NumberFormatException ignored) {
                            cells[0][column].setError("Número inválido");
                        }
                    }
                    totals[column].setText(String.format(Locale.US, "%.2f", sum));
                }
            }
        });
        add(parent, calculate);

        TextView persistence = text(
                "Itens persistidos em: " + spreadsheetItemsFile().getAbsolutePath(), 12);
        persistence.setTextColor(Color.DKGRAY);
        add(parent, persistence);
    }

    private File spreadsheetItemsFile() {
        return new File(getFilesDir(), "spreadsheet-items.txt");
    }

    private void loadSpreadsheetItems() {
        spreadsheetItems.clear();
        File file = spreadsheetItemsFile();
        if (!file.exists()) {
            return;
        }
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    new FileInputStream(file), "UTF-8"));
            String line;
            while ((line = reader.readLine()) != null) {
                String item = line.trim();
                if (item.length() > 0 && !spreadsheetItems.contains(item)) {
                    spreadsheetItems.add(item);
                }
            }
            reader.close();
        } catch (Exception ignored) {
            // A planilha continua utilizável mesmo se o arquivo não puder ser lido.
        }
    }

    private void rememberSpreadsheetItem(String value) {
        String item = value.trim();
        if (item.length() == 0 || spreadsheetItems.contains(item)) {
            return;
        }
        try {
            OutputStreamWriter writer = new OutputStreamWriter(
                    new FileOutputStream(spreadsheetItemsFile(), true), "UTF-8");
            writer.write(item);
            writer.write("\n");
            writer.close();
            spreadsheetItems.add(item);
            if (spreadsheetItemAdapter != null) {
                spreadsheetItemAdapter.notifyDataSetChanged();
            }
        } catch (Exception ignored) {
            // A célula permanece editável mesmo se a persistência falhar.
        }
    }

    private void addSpreadsheetLabel(TableRow row, String value, boolean highlighted) {
        TextView label = text(value, 16);
        label.setGravity(Gravity.CENTER);
        label.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        if (highlighted) {
            label.setTextColor(Color.rgb(20, 70, 120));
            label.setBackgroundColor(Color.rgb(225, 235, 245));
        }
        row.addView(label, new TableRow.LayoutParams(dp(52), dp(52)));
    }

    private Button screenButton(String label, final Class<? extends Activity> screen) {
        Button button = new Button(this);
        button.setText(label);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, screen));
            }
        });
        return button;
    }

    private void showInfoDialog() {
        new AlertDialog.Builder(this)
                .setTitle("AlertDialog nativo")
                .setMessage("Este diálogo pertence ao framework Android e não usa AndroidX.")
                .setPositiveButton("OK", null)
                .show();
    }

    private void showPopup(View anchor) {
        TextView content = text("PopupWindow\nView independente flutuante", 16);
        content.setTextColor(Color.WHITE);
        content.setBackgroundColor(Color.rgb(40, 40, 40));
        content.setPadding(dp(16), dp(12), dp(16), dp(12));

        final android.widget.PopupWindow popup = new android.widget.PopupWindow(
                content, dp(240), dp(90), true);
        popup.setBackgroundDrawable(new ColorDrawable(Color.DKGRAY));
        popup.setOutsideTouchable(true);
        popup.showAsDropDown(anchor);
    }

    private class PaintedView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        PaintedView() {
            super(MainActivity.this);
            setContentDescription("Canvas com formas desenhadas pelo aplicativo");
            setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(170)));
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            canvas.drawColor(Color.rgb(250, 250, 250));

            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.rgb(220, 70, 70));
            canvas.drawRect(dp(20), dp(20), dp(115), dp(105), paint);

            paint.setColor(Color.rgb(60, 140, 220));
            canvas.drawCircle(dp(175), dp(62), dp(42), paint);

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(5));
            paint.setColor(Color.rgb(40, 150, 90));
            canvas.drawLine(dp(245), dp(105), dp(340), dp(20), paint);

            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.DKGRAY);
            paint.setTextSize(dp(16));
            canvas.drawText("Canvas + Paint + formas", dp(20), dp(145), paint);
        }
    }
}
