package com.example.tmp1;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.DocumentsContract;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_PICK_FILE = 1;
    private static final int REQUEST_CODE_PERMISSION_SEND_SMS = 2;
    private static final int REQUEST_CODE_WRITE_EXTERNAL_STORAGE = 3;


    private Button btnChooseFile;
    private TextView tvFilePath;
    private EditText edtDelay;
    private Button btnSend;
    private TextView tvProcessedCount;
    private TextView tvLog;
    private Button btnSaveLog;

    private List<String> phoneNumbers;
    private int processedCount = 0;
    private StringBuilder logBuilder;
    private Handler handler;
    private int currentPhoneNumberIndex = 0;
    private long delayMillis = 2000; // Default delay: 2 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnChooseFile = findViewById(R.id.btn_choose_file);
        tvFilePath = findViewById(R.id.tv_file_path);
        edtDelay = findViewById(R.id.edt_delay);
        btnSend = findViewById(R.id.btn_send);
        tvProcessedCount = findViewById(R.id.tv_processed_count);
        tvLog = findViewById(R.id.tv_log);
        btnSaveLog = findViewById(R.id.btn_save_log);
        logBuilder = new StringBuilder();
        handler = new Handler();

        phoneNumbers = new ArrayList<>(); // Initialize the list

        btnChooseFile.setOnClickListener(v -> openFilePicker());
        btnSend.setOnClickListener(v -> startSendingMessages());
        btnSaveLog.setOnClickListener(v -> saveLogToFile());

        checkAndRequestPermissions();
    }
    private void checkAndRequestPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, REQUEST_CODE_PERMISSION_SEND_SMS);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE_WRITE_EXTERNAL_STORAGE);
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSION_SEND_SMS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // SMS permission granted
            } else {
                Toast.makeText(this, "SMS permission denied!", Toast.LENGTH_SHORT).show();
                // Handle permission denial (e.g., disable send button)
            }
        }

        if (requestCode == REQUEST_CODE_WRITE_EXTERNAL_STORAGE)
        {
            if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED)
            {
                Toast.makeText(this,"Storage permission is required to save logs.", Toast.LENGTH_LONG).show();
                btnSaveLog.setEnabled(false);
            }
        }
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/plain"); // Or use "*/*" for all file types
        //For API Level > = 19
        startActivityForResult(intent, REQUEST_CODE_PICK_FILE);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_PICK_FILE && resultCode == RESULT_OK) {
            if (data != null) {
                Uri uri = data.getData();
                tvFilePath.setText(uri.getPath()); // Display the path (for user info)
                readPhoneNumbersFromFile(uri);
            }
        }
    }
    private void readPhoneNumbersFromFile(Uri uri) {
        phoneNumbers.clear(); // Clear previous numbers
        processedCount = 0; //Reset
        currentPhoneNumberIndex = 0;
        tvProcessedCount.setText("Số tin đã xử lý: 0");
        logBuilder = new StringBuilder();
        tvLog.setText("");

        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = reader.readLine()) != null) {
                // Simple validation and cleaning
                String cleanedNumber = line.trim().replaceAll("[^0-9]", "");
                if (!cleanedNumber.isEmpty()) {
                    phoneNumbers.add(cleanedNumber);
                }
            }
            reader.close();
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Lỗi đọc file!", Toast.LENGTH_SHORT).show();
            logBuilder.append("Lỗi đọc file: " + e.getMessage() + "\n");
            tvLog.setText(logBuilder.toString());

        }
    }



    private void startSendingMessages() {
        if (phoneNumbers.isEmpty()) {
            Toast.makeText(this, "Không có số điện thoại nào!", Toast.LENGTH_SHORT).show();
            return;
        }

        String delayStr = edtDelay.getText().toString();
        if (!delayStr.isEmpty()) {
            try {
                delayMillis = Long.parseLong(delayStr) * 1000; // Convert seconds to milliseconds
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Delay không hợp lệ!", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        btnSend.setEnabled(false); // Prevent multiple clicks
        btnChooseFile.setEnabled(false); // Disable while sending
        edtDelay.setEnabled(false);

        currentPhoneNumberIndex = 0; //Start sending
        sendMessageWithDelay();

    }

    private void sendMessageWithDelay() {
        if (currentPhoneNumberIndex < phoneNumbers.size()) {
            String phoneNumber = phoneNumbers.get(currentPhoneNumberIndex);
            sendSMS(phoneNumber);
            processedCount++;
            tvProcessedCount.setText("Số tin đã xử lý: " + processedCount);

            currentPhoneNumberIndex++;
            handler.postDelayed(this::sendMessageWithDelay, delayMillis); // Recursive call with delay
        } else {
            // All messages sent (or attempted)
            btnSend.setEnabled(true);
            btnChooseFile.setEnabled(true);
            edtDelay.setEnabled(true);
            Toast.makeText(this, "Đã gửi xong!", Toast.LENGTH_SHORT).show();

        }
    }



    private void sendSMS(String phoneNumber) {
        try {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(phoneNumber, null, "Noi dung tin nhan test", null, null);  // Replace "Test SMS"
            logBuilder.append("Gửi thành công: " + phoneNumber + "\n");

        } catch (Exception e) {
            logBuilder.append("Lỗi gửi tới " + phoneNumber + ": " + e.getMessage() + "\n");
            e.printStackTrace(); // Log the full stack trace for debugging

        }
        tvLog.setText(logBuilder.toString()); // Update the UI with current log
    }



    private void saveLogToFile() {

        if (logBuilder.length() == 0) {
            Toast.makeText(this, "Không có nội dung để lưu!", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // Create a file in the Downloads directory
            File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File logFile = new File(downloadsDir, "sms_log.txt");


            FileOutputStream fileOutputStream = new FileOutputStream(logFile);
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream);
            outputStreamWriter.write(logBuilder.toString());
            outputStreamWriter.close();
            fileOutputStream.close();

            Toast.makeText(this, "Đã lưu log tại: " + logFile.getAbsolutePath(), Toast.LENGTH_LONG).show();

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Lỗi lưu file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e("SMS_APP", "Error saving log file: " + e.getMessage());

        }
    }

}