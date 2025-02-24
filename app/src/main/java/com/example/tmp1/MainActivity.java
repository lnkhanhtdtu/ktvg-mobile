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

    private static final int REQUEST_CODE_PICK_FILE = 1;
    private static final int REQUEST_CODE_PERMISSION_SEND_SMS = 2;
    private static final int REQUEST_CODE_WRITE_EXTERNAL_STORAGE = 3;

    private static final String SENT = "SMS_SENT";
    private static final String DELIVERED = "SMS_DELIVERED"; // Không dùng nhưng giữ lại

    private Button btnChooseFile;
    private TextView tvFilePath;
    private EditText edtDelay;
    private EditText edtMsg;
    private Button btnSend;
    private TextView tvProcessedCount;
    private TextView tvLog;
    private Button btnSaveLog;

    private List<String> phoneNumbers;
    private int processedCount = 0;
    private StringBuilder logBuilder;
    private Handler handler;
    private int currentPhoneNumberIndex = 0;
    private long delayMillis = 2000;

    // BroadcastReceivers
    private BroadcastReceiver sentReceiver;
    //private BroadcastReceiver deliveredReceiver; // Không dùng deliveredReceiver

    // Lấy thời gian hiện tại
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
    String currentTime;

    int totalLines = 0; // Biến đếm số dòng

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
        handler = new Handler();
        phoneNumbers = new ArrayList<>();

        btnChooseFile.setOnClickListener(v -> openFilePicker());
        btnSend.setOnClickListener(v -> startSendingMessages());
        btnSaveLog.setOnClickListener(v -> saveLogToFile());

        checkAndRequestPermissions();
    }

    // Các phương thức khác giữ nguyên (checkAndRequestPermissions, onRequestPermissionsResult, openFilePicker, onActivityResult, readPhoneNumbersFromFile)

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

        currentTime = sdf.format(new Date());

        phoneNumbers.clear(); // Clear previous numbers
        processedCount = 0; //Reset
        currentPhoneNumberIndex = 0;
        tvProcessedCount.setText("[" + currentTime + "] Số tin đã xử lý: 0/" + totalLines);
        logBuilder = new StringBuilder();
        tvLog.setText("");

        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = reader.readLine()) != null) {
                totalLines++; // Tăng số dòng

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

            currentTime = sdf.format(new Date());
            logBuilder.append("[" + currentTime + "] Lỗi đọc file: " + e.getMessage() + "\n");

            tvLog.setText(logBuilder.toString());

        }
    }


    private void startSendingMessages() {
        if (phoneNumbers.isEmpty()) {
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

        btnSend.setEnabled(false);
        btnChooseFile.setEnabled(false);
        edtDelay.setEnabled(false);
        edtMsg.setEnabled(false);

        currentPhoneNumberIndex = 0;
        sendMessageWithDelay();
    }

    private void sendMessageWithDelay() {
        if (currentPhoneNumberIndex < phoneNumbers.size()) {
            String phoneNumber = phoneNumbers.get(currentPhoneNumberIndex);

            currentTime = sdf.format(new Date());

            if (isValidPhoneNumber(phoneNumber)) { // Kiểm tra số điện thoại
                sendSMS(phoneNumber);
                logBuilder.append("[" + currentTime + "] Gửi thành công: " + phoneNumber + "\n");
                tvLog.setText(logBuilder.toString());
            } else {
                logBuilder.append("[" + currentTime + "] Lỗi: Số điện thoại không hợp lệ: " + phoneNumber + "\n");
                tvLog.setText(logBuilder.toString()); // Hiển thị lỗi
            }

            processedCount++;
            tvProcessedCount.setText("Số tin đã xử lý: " + processedCount + "/" + totalLines);

            currentPhoneNumberIndex++;
            handler.postDelayed(this::sendMessageWithDelay, delayMillis); // Lặp lại sau delay
        } else {
            // Đã gửi hết tin nhắn
            btnSend.setEnabled(true);
            btnChooseFile.setEnabled(true);
            edtDelay.setEnabled(true);
            edtMsg.setEnabled(true);
            Toast.makeText(this, "Đã gửi xong!", Toast.LENGTH_SHORT).show();

            // Hủy đăng ký receiver KHI ĐÃ XỬ LÝ XONG
            if (sentReceiver != null) {
                unregisterReceiver(sentReceiver);
                sentReceiver = null; // Đặt lại thành null
            }
        }
    }
    private void sendSMS(String phoneNumber) {
        // 1. Tạo PendingIntent cho việc GỬI
        PendingIntent sentPI = PendingIntent.getBroadcast(this, 0, new Intent(SENT), PendingIntent.FLAG_IMMUTABLE);

        // 2. Tạo BroadcastReceiver (và đăng ký) *NGAY TRƯỚC KHI* gọi sendTextMessage.
        if (sentReceiver == null) { // Chỉ tạo và đăng ký MỘT LẦN
            sentReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {

                    String phoneNumber = intent.getStringExtra("phoneNumber");
                    if (phoneNumber == null) {
                        Log.e("MainActivity", "Phone number is missing in the intent!");
                        return;
                    }

                    currentTime = sdf.format(new Date());

                    switch (getResultCode()) {
                        case Activity.RESULT_OK:
                            logBuilder.append("[" + currentTime + "] Gửi thành công: " + phoneNumber + "\n");
                            break;
                        case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                            logBuilder.append("[" + currentTime + "] Lỗi gửi tới " + phoneNumber + ": Lỗi chung\n");
                            break;
                        case SmsManager.RESULT_ERROR_NO_SERVICE:
                            logBuilder.append("[" + currentTime + "] Lỗi gửi tới " + phoneNumber + ": Không có dịch vụ (không SIM/sóng)\n");
                            break;
                        case SmsManager.RESULT_ERROR_NULL_PDU:
                            logBuilder.append("[" + currentTime + "] Lỗi gửi tới " + phoneNumber + ": Lỗi PDU\n");
                            break;
                        case SmsManager.RESULT_ERROR_RADIO_OFF:
                            logBuilder.append("[" + currentTime + "] Lỗi gửi tới " + phoneNumber + ": Chế độ máy bay/radio tắt\n");
                            break;
                        default: // Thêm trường hợp default
                            logBuilder.append("[" + currentTime + "] Lỗi gửi tới " + phoneNumber + ": Lỗi không xác định (" + getResultCode() + ")\n");
                            break;
                    }

                    tvLog.setText(logBuilder.toString());
                }
            };
            registerReceiver(sentReceiver, new IntentFilter(SENT));
        }
        // PendingIntent deliveredPI = PendingIntent.getBroadcast(this, 0, new Intent(DELIVERED), PendingIntent.FLAG_IMMUTABLE); // Không cần thiết.

        // 3. Gửi tin nhắn, truyền PendingIntent
        try {
            String msgStr = edtMsg.getText().toString();
            SmsManager smsManager = SmsManager.getDefault();

            // Kiểm tra tin nhắn có vượt quá giới hạn ký tự không
            ArrayList<String> parts = smsManager.divideMessage(msgStr);
            if (parts.size() > 1) {
                // Tin nhắn dài, gửi nhiều phần
                ArrayList<PendingIntent> sentIntents = new  ArrayList<PendingIntent>();
                for(int i = 0; i< parts.size(); i++)
                {
                    sentIntents.add(sentPI);
                }
                smsManager.sendMultipartTextMessage(phoneNumber, null, parts, sentIntents, null);
            } else {
                // Tin nhắn ngắn, gửi bình thường
                smsManager.sendTextMessage(phoneNumber, null, msgStr, sentPI, null);
            }

        } catch (Exception e) { // Bắt Exception
            currentTime = sdf.format(new Date());

            logBuilder.append("[" + currentTime + "] Lỗi gửi tới " + phoneNumber + ": " + e.getMessage() + "\n");
            e.printStackTrace();
            tvLog.setText(logBuilder.toString()); // Cập nhật log
        }
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

    private boolean isValidPhoneNumber(String phoneNumber) {
        // PhoneNumberUtils.isGlobalPhoneNumber() kiểm tra khá rộng
        // Bạn có thể tùy chỉnh thêm, ví dụ: kiểm tra độ dài, mã quốc gia, ...
        if (TextUtils.isEmpty(phoneNumber)) {
            return false;
        }
//        return PhoneNumberUtils.isGlobalPhoneNumber(phoneNumber);


        // Hoặc, sử dụng regex (tùy chỉnh chặt chẽ hơn):
         String regex = "^[+]?[0-9]{10,13}$"; // Ví dụ: số điện thoại từ 10-13 chữ số, có thể có dấu +
         return phoneNumber.matches(regex);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Hủy đăng ký receiver trong onDestroy ĐỂ TRÁNH RÒ RỈ BỘ NHỚ
        if (sentReceiver != null) {
            try{
                unregisterReceiver(sentReceiver);
            } catch(IllegalArgumentException e) {
                // Receiver might have already been unregistered.  Ignore.
            }
            sentReceiver = null;
        }
    }
}