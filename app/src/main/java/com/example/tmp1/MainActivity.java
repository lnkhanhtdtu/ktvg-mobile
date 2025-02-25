package com.example.tmp1;

import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.telephony.SmsManager;
import android.text.TextUtils;
import android.util.Log;
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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    //region Khai báo biến
    private static final int REQUEST_CODE_PICK_FILE = 1;
    private static final int REQUEST_CODE_PERMISSION_SEND_SMS = 2;
    private static final int REQUEST_CODE_WRITE_EXTERNAL_STORAGE = 3;

    private static final String SENT = "SMS_SENT";
    // private static final String DELIVERED = "SMS_DELIVERED"; // Không dùng

    private Button btnChooseFile;
    private TextView tvFilePath;
    private EditText edtDelay;
    private EditText edtMsg;
    private Button btnSend;
    private TextView tvProcessedCount;
    private TextView tvLog;
    private Button btnSaveLog;

    private List<String> phoneNumbers;
    // Xóa phoneNumbersKhongHopLe
    // private List<String> phoneNumbersKhongHopLe;

    private int processedCount = 0;
    private int totalLines = 0; // Loại bỏ totalLines
    private StringBuilder logBuilder;
    private Handler handler;
    private int currentPhoneNumberIndex = 0;
    private long delayMillis = 2000;

    private BroadcastReceiver sentReceiver;

    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

    private int sentSuccessfullyCount = 0;
    private int totalValidNumbers = 0;
    private int invalidNumberCount = 0; // Số lượng số không hợp lệ

    //region Vòng đời Activity
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnChooseFile = findViewById(R.id.btn_choose_file);
        tvFilePath = findViewById(R.id.tv_file_path);
        edtDelay = findViewById(R.id.edt_delay);
        edtMsg = findViewById(R.id.edt_msg);
        btnSend = findViewById(R.id.btn_send);
        tvProcessedCount = findViewById(R.id.tv_processed_count);
        tvLog = findViewById(R.id.tv_log);
        btnSaveLog = findViewById(R.id.btn_save_log);

        logBuilder = new StringBuilder();
        handler = new Handler(Looper.getMainLooper()); // Sử dụng Main Looper
        phoneNumbers = new ArrayList<>();
//        phoneNumbersKhongHopLe = new ArrayList<>(); // Xóa

        btnChooseFile.setOnClickListener(v -> openFilePicker());
        btnSend.setOnClickListener(v -> startSendingMessages());
        btnSaveLog.setOnClickListener(v -> saveLogToFile());

        checkAndRequestPermissions();
        updateProcessedCountDisplay(); // Cập nhật hiển thị ban đầu
    }

    // Các phương thức khác giữ nguyên

    private void checkAndRequestPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, REQUEST_CODE_PERMISSION_SEND_SMS);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE_WRITE_EXTERNAL_STORAGE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSION_SEND_SMS) {
            if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "SMS permission denied!", Toast.LENGTH_SHORT).show();
                btnSend.setEnabled(false); // Vô hiệu hóa nút gửi nếu không có quyền
            }
        }
        if (requestCode == REQUEST_CODE_WRITE_EXTERNAL_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Storage permission is required to save logs.", Toast.LENGTH_LONG).show();
                btnSaveLog.setEnabled(false);
            }
        }
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/plain");
        startActivityForResult(intent, REQUEST_CODE_PICK_FILE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_PICK_FILE && resultCode == RESULT_OK) {
            if (data != null) {
                Uri uri = data.getData();
                tvFilePath.setText(uri.getPath());
                readPhoneNumbersFromFile(uri);
            }
        }
    }

    private void readPhoneNumbersFromFile(Uri uri) {
        phoneNumbers.clear();
        // phoneNumbersKhongHopLe.clear(); // Xóa

        processedCount = 0;
        sentSuccessfullyCount = 0;
        currentPhoneNumberIndex = 0;
        totalValidNumbers = 0;
        invalidNumberCount = 0; // Reset
        logBuilder = new StringBuilder();
        tvLog.setText("");

        try (InputStream inputStream = getContentResolver().openInputStream(uri);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {

            String line;
            while ((line = reader.readLine()) != null) {
                // totalLines++; // Không dùng totalLines nữa
                String cleanedNumber = line.trim().replaceAll("[^0-9]", "");
                if (!cleanedNumber.isEmpty()) { // Chỉ xử lý dòng không rỗng
                    if (isValidPhoneNumber(cleanedNumber)) {
                        phoneNumbers.add(cleanedNumber);
                        totalValidNumbers++;
                    } else {
                        // Xử lý số KHÔNG hợp lệ NGAY TẠI ĐÂY
                        logError("Số điện thoại không hợp lệ", cleanedNumber);
                        processedCount++; // Tăng số đã "xử lý" (bỏ qua)
                        invalidNumberCount++;
                    }
                } else {
                    processedCount++;//dòng rỗng
                }
            }
            updateProcessedCountDisplay(); // Cập nhật UI

        } catch (IOException e) {
            e.printStackTrace();
            logError("Lỗi đọc file: " + e.getMessage(), null);
        }
    }

    private void startSendingMessages() {
        if (phoneNumbers.isEmpty() && invalidNumberCount == 0) { // Kiểm tra cả số hợp lệ và không hợp lệ
            Toast.makeText(this, "Không có số điện thoại!", Toast.LENGTH_SHORT).show();
            return;
        }

        String delayStr = edtDelay.getText().toString();
        if (!delayStr.isEmpty()) {
            try {
                delayMillis = Long.parseLong(delayStr) * 1000;
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Delay không hợp lệ!", Toast.LENGTH_SHORT).show();
                return;
            }
        }

// Reset lại trạng thái
        processedCount = invalidNumberCount; // Số đã xử lý ban đầu là số không hợp lệ đã bỏ qua
        sentSuccessfullyCount = 0;
        currentPhoneNumberIndex = 0;
        updateProcessedCountDisplay();

        btnSend.setEnabled(false);
        btnChooseFile.setEnabled(false);
        btnSaveLog.setEnabled(false);
        edtDelay.setEnabled(false);
        edtMsg.setEnabled(false);

        handler.post(this::sendMessageWithDelay);
    }


    private void sendMessageWithDelay() {
        if (currentPhoneNumberIndex < phoneNumbers.size()) {
            String phoneNumber = phoneNumbers.get(currentPhoneNumberIndex);

            if (isValidPhoneNumber(phoneNumber)) {
                sendSMS(phoneNumber); // Gửi nếu hợp lệ
                // KHÔNG tăng processedCount ở đây, tăng trong onReceive
            } else {
                // Xử lý số điện thoại KHÔNG hợp lệ
                logError("Số điện thoại không hợp lệ", phoneNumber);
                processedCount++;       // Tăng số đã xử lý (nhưng không gửi)
                updateProcessedCountDisplay(); // Cập nhật UI
            }

            currentPhoneNumberIndex++; // Luôn tăng index để chuyển số tiếp theo
            handler.postDelayed(this::sendMessageWithDelay, delayMillis); // Lặp lại

        } else {
            // ... (phần code khi gửi xong) ...
            btnSend.setEnabled(true);
            btnChooseFile.setEnabled(true);
            btnSaveLog.setEnabled(true);
            edtDelay.setEnabled(true);
            edtMsg.setEnabled(true);
            btnSaveLog.setEnabled(true);
            Toast.makeText(this, "Đã gửi xong!", Toast.LENGTH_SHORT).show();
        }
    }

    private void sendSMS(String phoneNumber) {
        PendingIntent sentPI = PendingIntent.getBroadcast(this, 0,
                new Intent(SENT).putExtra("phoneNumber", phoneNumber),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT); // Thêm FLAG_UPDATE_CURRENT

        // Tạo BroadcastReceiver *bên ngoài* sendSMS, chỉ tạo một lần
        if (sentReceiver == null) {
            createSentReceiver(); // Gọi phương thức tạo receiver
        }
        try {
            SmsManager smsManager = SmsManager.getDefault();
            String msgStr = edtMsg.getText().toString();
            ArrayList<String> parts = smsManager.divideMessage(msgStr);

            if (parts.size() > 1) {
                ArrayList<PendingIntent> sentIntents = new ArrayList<>();
                for (int i = 0; i < parts.size(); i++) {
                    sentIntents.add(sentPI);
                }
                smsManager.sendMultipartTextMessage(phoneNumber, null, parts, sentIntents, null);
            } else {
                smsManager.sendTextMessage(phoneNumber, null, msgStr, sentPI, null);
            }
            // Không log "gửi thành công" ở đây.  Log trong onReceive.

        } catch (IllegalArgumentException e) {
            logError("Lỗi đối số: " + e.getMessage(), phoneNumber); // Log lỗi
        } catch (Exception e) {
            logError("Lỗi không xác định: " + e.getMessage(), phoneNumber);
        }
    }

    private void createSentReceiver() {
        sentReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String receivedPhoneNumber = intent.getStringExtra("phoneNumber");
                if (receivedPhoneNumber == null) {
                    Log.e("MainActivity", "Phone number is missing in the intent!");
                    return;
                }

                String currentTime = sdf.format(new Date());
                int resultCode = getResultCode();

                switch (resultCode) {
                    case Activity.RESULT_OK:
                        logMessage("Gửi thành công: " + receivedPhoneNumber, currentTime);
                        sentSuccessfullyCount++; // Tăng số tin nhắn gửi thành công
                        break;
                    case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                        logError("Lỗi chung", receivedPhoneNumber, currentTime);
                        break;
                    case SmsManager.RESULT_ERROR_NO_SERVICE:
                        logError("Không có dịch vụ", receivedPhoneNumber, currentTime);
                        break;
                    case SmsManager.RESULT_ERROR_NULL_PDU:
                        logError("Lỗi PDU", receivedPhoneNumber, currentTime);
                        break;
                    case SmsManager.RESULT_ERROR_RADIO_OFF:
                        logError("Chế độ máy bay/radio tắt", receivedPhoneNumber, currentTime);
                        break;
                    default:
                        logError("Lỗi không xác định (" + resultCode + ")", receivedPhoneNumber, currentTime);
                        break;
                }
                processedCount++; // Luôn tăng số đã xử lý (thành công hoặc thất bại)
                updateProcessedCountDisplay();
            }
        };
        registerReceiver(sentReceiver, new IntentFilter(SENT));
    }

    private void saveLogToFile() {
        if (logBuilder.length() == 0) {
            Toast.makeText(this, "Không có nội dung để lưu!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE_WRITE_EXTERNAL_STORAGE);
            return;
        }


        try {
            File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File logFile = new File(downloadsDir, "sms_log.txt");

            try (FileOutputStream fileOutputStream = new FileOutputStream(logFile);
                 OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream)) {
                outputStreamWriter.write(logBuilder.toString());
            }

            Toast.makeText(this, "Đã lưu log tại: " + logFile.getAbsolutePath(), Toast.LENGTH_LONG).show();

        } catch (IOException e) {
            logError("Lỗi lưu file: " + e.getMessage(), null); // Log lỗi
            Log.e("SMS_APP", "Error saving log file: " + e.getMessage());
        }
    }

    private boolean isValidPhoneNumber(String phoneNumber) {
        String regex = "^[+]?[0-9]{10,13}$";
        return phoneNumber.matches(regex);
    }

    private void logMessage(String message, String time) {
        logBuilder.append("[").append(time).append("] ").append(message).append("\n");
        updateLogTextView();
    }

    private void logError(String errorMessage, String phoneNumber) {
        logError(errorMessage, phoneNumber, sdf.format(new Date()));
    }

    private void logError(String errorMessage, String phoneNumber, String time) {
        logBuilder.append("[").append(time).append("] ");
        if (phoneNumber != null) {
            logBuilder.append("Lỗi gửi tới ").append(phoneNumber).append(": ");
        }
        logBuilder.append(errorMessage).append("\n");
        updateLogTextView();
    }

    private void updateLogTextView() {
        runOnUiThread(() -> tvLog.setText(logBuilder.toString()));
    }

    private void updateProcessedCountDisplay() {
        runOnUiThread(() -> {
            // Hiển thị: Đã xử lý / (Tổng hợp lệ + Đã bỏ qua) (Thành công)
            String displayText = String.format(Locale.getDefault(), "Đã xử lý: %d/%d (Thành công: %d)", processedCount, totalValidNumbers + invalidNumberCount, sentSuccessfullyCount);
            tvProcessedCount.setText(displayText);
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (sentReceiver != null) {
            try {
                unregisterReceiver(sentReceiver);
            } catch (IllegalArgumentException e) {
                // Receiver might have already been unregistered.  Ignore.
            }
            sentReceiver = null;
        }
    }
}