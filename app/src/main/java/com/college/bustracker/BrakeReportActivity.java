package com.college.bustracker;

import android.Manifest;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class BrakeReportActivity extends AppCompatActivity {

    private ListView listBrakeLogs;
    private Button btnGeneratePdf;

    private final ArrayList<String> displayList = new ArrayList<>();
    private final ArrayList<BrakeLog> brakeLogs = new ArrayList<>();
    private ArrayAdapter<String> adapter;

    private static final int STORAGE_PERMISSION_CODE = 5001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_brake_report);

        listBrakeLogs = findViewById(R.id.listBrakeLogs);
        btnGeneratePdf = findViewById(R.id.btnGeneratePdf);

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, displayList);
        listBrakeLogs.setAdapter(adapter);

        loadBrakeLogs();

        btnGeneratePdf.setOnClickListener(v -> {
            if (brakeLogs.isEmpty()) {
                Toast.makeText(this, "No brake logs found", Toast.LENGTH_SHORT).show();
                return;
            }

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                            != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        STORAGE_PERMISSION_CODE
                );
            } else {
                generatePdfReport();
            }
        });
    }

    private void loadBrakeLogs() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("suddenBrakeLogs");

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                brakeLogs.clear();
                displayList.clear();

                for (DataSnapshot logSnap : snapshot.getChildren()) {
                    BrakeLog log = logSnap.getValue(BrakeLog.class);

                    if (log != null) {
                        brakeLogs.add(log);

                        String time = new SimpleDateFormat("dd-MM-yyyy hh:mm a", Locale.getDefault())
                                .format(new Date(log.timestamp));

                        displayList.add(
                                "Bus: " + log.busId +
                                        "\nDriver: " + log.driverName +
                                        "\nOld Speed: " + Math.round(log.oldSpeed) + " km/h" +
                                        "\nNew Speed: " + Math.round(log.newSpeed) + " km/h" +
                                        "\nTime: " + time
                        );
                    }
                }

                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(BrakeReportActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void generatePdfReport() {
        PdfDocument pdfDocument = new PdfDocument();
        Paint titlePaint = new Paint();
        Paint textPaint = new Paint();

        titlePaint.setTextSize(18f);
        titlePaint.setFakeBoldText(true);

        textPaint.setTextSize(11f);

        int pageWidth = 1200;
        int pageHeight = 1800;
        int y = 60;
        int pageNumber = 1;

        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create();
        PdfDocument.Page page = pdfDocument.startPage(pageInfo);

        for (int i = 0; i < brakeLogs.size(); i++) {
            BrakeLog log = brakeLogs.get(i);

            if (y > 1650) {
                pdfDocument.finishPage(page);
                pageNumber++;
                pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create();
                page = pdfDocument.startPage(pageInfo);
                y = 60;
            }

            if (i == 0 || y == 60) {
                page.getCanvas().drawText("Bus Sudden Brake Report", 40, y, titlePaint);
                y += 40;
            }

            String time = new SimpleDateFormat("dd-MM-yyyy hh:mm a", Locale.getDefault())
                    .format(new Date(log.timestamp));

            page.getCanvas().drawText("Bus ID: " + safe(log.busId), 40, y, textPaint); y += 25;
            page.getCanvas().drawText("Driver Name: " + safe(log.driverName), 40, y, textPaint); y += 25;
            page.getCanvas().drawText("Old Speed: " + Math.round(log.oldSpeed) + " km/h", 40, y, textPaint); y += 25;
            page.getCanvas().drawText("New Speed: " + Math.round(log.newSpeed) + " km/h", 40, y, textPaint); y += 25;
            page.getCanvas().drawText("Latitude: " + log.latitude, 40, y, textPaint); y += 25;
            page.getCanvas().drawText("Longitude: " + log.longitude, 40, y, textPaint); y += 25;
            page.getCanvas().drawText("Time: " + time, 40, y, textPaint); y += 35;
            page.getCanvas().drawLine(40, y, 1100, y, textPaint); y += 25;
        }

        pdfDocument.finishPage(page);

        String fileName = "Brake_Report_" +
                new SimpleDateFormat("ddMMyyyy_HHmmss", Locale.getDefault()).format(new Date()) +
                ".pdf";

        try {
            OutputStream outputStream;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
                values.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
                values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

                Uri uri = getContentResolver().insert(MediaStore.Files.getContentUri("external"), values);
                if (uri == null) {
                    Toast.makeText(this, "Failed to create file", Toast.LENGTH_SHORT).show();
                    pdfDocument.close();
                    return;
                }

                outputStream = getContentResolver().openOutputStream(uri);
            } else {
                File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                File file = new File(downloadsDir, fileName);
                outputStream = new FileOutputStream(file);
            }

            if (outputStream != null) {
                pdfDocument.writeTo(outputStream);
                outputStream.flush();
                outputStream.close();
            }

            pdfDocument.close();
            Toast.makeText(this, "PDF saved in Downloads", Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            pdfDocument.close();
            Toast.makeText(this, "PDF failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                generatePdfReport();
            } else {
                Toast.makeText(this, "Storage permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}