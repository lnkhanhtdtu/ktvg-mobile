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

    //region Khai báo biến
    private static final int REQUEST_CODE_PICK_FILE = 1; // Mã yêu cầu chọn file
    private static final int REQUEST_CODE_PERMISSION_SEND_SMS = 2; // Mã yêu cầu quyền gửi SMS
    private static final int REQUEST_CODE_WRITE_EXTERNAL_STORAGE = 3; // Mã yêu cầu quyền ghi vào bộ nhớ

    private static final String SENT = "SMS_SENT"; // Action cho Intent khi tin nhắn được gửi
    private static final String DELIVERED = "SMS_DELIVERED"; // Action cho Intent khi tin nhắn được chuyển giao (không dùng)

    private Button btnChooseFile; // Nút chọn file
    private TextView tvFilePath; // TextView hiển thị đường dẫn file
    private EditText edtDelay;  // EditText nhập thời gian delay giữa các tin nhắn (giây)
    private EditText edtMsg;    // EditText nhập nội dung tin nhắn
    private Button btnSend;      // Nút gửi tin nhắn
    private TextView tvProcessedCount; // TextView hiển thị số tin nhắn đã xử lý/tổng số tin
    private TextView tvLog;      // TextView hiển thị log
    private Button btnSaveLog;    // Nút lưu log

    private List<String> phoneNumbers; // Danh sách số điện thoại đọc từ file
    private int processedCount = 0;     // Số tin nhắn đã được xử lý (đã gửi hoặc thử gửi)
    private StringBuilder logBuilder;  // StringBuilder để xây dựng chuỗi log
    private Handler handler;          // Handler để xử lý việc gửi tin nhắn với độ trễ
    private int currentPhoneNumberIndex = 0; // Chỉ số của số điện thoại hiện tại trong danh sách
    private long delayMillis = 2000;    // Thời gian delay mặc định giữa các tin nhắn (2 giây)

    // BroadcastReceiver để nhận thông báo khi tin nhắn được gửi thành công hoặc thất bại
    private BroadcastReceiver sentReceiver;
    //private BroadcastReceiver deliveredReceiver; // Không dùng deliveredReceiver

    // Định dạng thời gian để ghi log
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
    String currentTime; // Biến lưu thời gian hiện tại (String, đã format)

    int totalLines = 0; // Biến đếm tổng số dòng trong file
    //endregion

    //region Vòng đời Activity
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Ánh xạ các thành phần UI từ file XML
        btnChooseFile = findViewById(R.id.btn_choose_file);
        tvFilePath = findViewById(R.id.tv_file_path);
        edtDelay = findViewById(R.id.edt_delay);
        edtMsg = findViewById(R.id.edt_msg);
        btnSend = findViewById(R.id.btn_send);
        tvProcessedCount = findViewById(R.id.tv_processed_count);
        tvLog = findViewById(R.id.tv_log);
        btnSaveLog = findViewById(R.id.btn_save_log);

        // Khởi tạo các đối tượng
        logBuilder = new StringBuilder(); // Khởi tạo StringBuilder để lưu log
        handler = new Handler();            // Khởi tạo Handler để xử lý delay
        phoneNumbers = new ArrayList<>();    // Khởi tạo danh sách số điện thoại

        // Gán sự kiện click cho các nút
        btnChooseFile.setOnClickListener(v -> openFilePicker());  // Mở trình chọn file khi nhấn nút "Chọn File"
        btnSend.setOnClickListener(v -> startSendingMessages()); // Bắt đầu gửi tin nhắn khi nhấn nút "Gửi"
        btnSaveLog.setOnClickListener(v -> saveLogToFile());   // Lưu log ra file khi nhấn nút "Lưu Log"

        // Kiểm tra và yêu cầu các quyền cần thiết (SEND_SMS, WRITE_EXTERNAL_STORAGE)
        checkAndRequestPermissions();
    }

    // Các phương thức khác giữ nguyên (checkAndRequestPermissions, onRequestPermissionsResult, openFilePicker, onActivityResult, readPhoneNumbersFromFile)

    private void checkAndRequestPermissions() {
        // Kiểm tra xem ứng dụng đã có quyền SEND_SMS chưa
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            // Nếu chưa có quyền, yêu cầu quyền từ người dùng
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, REQUEST_CODE_PERMISSION_SEND_SMS);
        }
        // Kiểm tra xem ứng dụng đã có quyền WRITE_EXTERNAL_STORAGE chưa
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
        {
            // Nếu chưa có quyền, yêu cầu quyền từ người dùng
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE_WRITE_EXTERNAL_STORAGE);
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // Xử lý kết quả yêu cầu cấp quyền SEND_SMS
        if (requestCode == REQUEST_CODE_PERMISSION_SEND_SMS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Quyền SEND_SMS đã được cấp
            } else {
                // Quyền SEND_SMS bị từ chối
                Toast.makeText(this, "SMS permission denied!", Toast.LENGTH_SHORT).show();
                // Xử lý trường hợp không có quyền (ví dụ: vô hiệu hóa nút gửi tin nhắn)
            }
        }

        // Xử lý kết quả yêu cầu cấp quyền WRITE_EXTERNAL_STORAGE
        if (requestCode == REQUEST_CODE_WRITE_EXTERNAL_STORAGE)
        {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
            {
                // Quyền WRITE_EXTERNAL_STORAGE đã được cấp
            } else {
                // Quyền WRITE_EXTERNAL_STORAGE bị từ chối
                Toast.makeText(this,"Storage permission is required to save logs.", Toast.LENGTH_LONG).show();
                btnSaveLog.setEnabled(false); // Vô hiệu hóa nút lưu log
            }
        }
    }

    private void openFilePicker() {
        // Tạo Intent để mở trình chọn file
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/plain"); // Or use "*/*" for all file types
        //For API Level > = 19
        startActivityForResult(intent, REQUEST_CODE_PICK_FILE);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Xử lý kết quả trả về từ trình chọn file
        if (requestCode == REQUEST_CODE_PICK_FILE && resultCode == RESULT_OK) {
            if (data != null) {
                Uri uri = data.getData(); // Lấy URI của file được chọn
                tvFilePath.setText(uri.getPath()); // Hiển thị đường dẫn file lên TextView
                readPhoneNumbersFromFile(uri); // Đọc nội dung file
            }
        }
    }
    private void readPhoneNumbersFromFile(Uri uri) {

        currentTime = sdf.format(new Date()); // Cập nhật thời gian hiện tại

        phoneNumbers.clear(); // Xóa danh sách số điện thoại cũ (nếu có)
        processedCount = 0; // Đặt lại số tin nhắn đã xử lý về 0
        currentPhoneNumberIndex = 0; // Đặt lại chỉ số số điện thoại hiện tại về 0
        tvProcessedCount.setText("[" + currentTime + "] Số tin đã xử lý: 0/" + totalLines); // Hiển thị số tin đã xử lý/tổng số tin
        logBuilder = new StringBuilder(); // Tạo mới StringBuilder để lưu log
        tvLog.setText(""); // Xóa nội dung log cũ (nếu có)

        try {
            InputStream inputStream = getContentResolver().openInputStream(uri); // Mở luồng đọc từ URI của file
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream)); // Tạo BufferedReader để đọc file
            String line;
            // Đọc từng dòng của file
            while ((line = reader.readLine()) != null) {
                totalLines++; // Tăng số dòng

                // Loại bỏ khoảng trắng và các ký tự không phải số khỏi dòng
                String cleanedNumber = line.trim().replaceAll("[^0-9]", "");
                // Nếu chuỗi sau khi làm sạch không rỗng, thêm nó vào danh sách số điện thoại
                if (!cleanedNumber.isEmpty()) {
                    phoneNumbers.add(cleanedNumber);
                }
            }
            reader.close(); // Đóng BufferedReader
            inputStream.close(); // Đóng luồng đọc file
        } catch (IOException e) { // Xử lý ngoại lệ nếu có lỗi xảy ra khi đọc file
            e.printStackTrace();
            Toast.makeText(this, "Lỗi đọc file!", Toast.LENGTH_SHORT).show();

            currentTime = sdf.format(new Date()); // Cập nhật thời gian hiện tại
            logBuilder.append("[" + currentTime + "] Lỗi đọc file: " + e.getMessage() + "\n"); // Thêm thông tin lỗi vào log

            tvLog.setText(logBuilder.toString()); // Hiển thị log lên TextView

        }
    }


    private void startSendingMessages() {
        // Kiểm tra xem danh sách số điện thoại có rỗng không
        if (phoneNumbers.isEmpty()) {
            Toast.makeText(this, "Không có số điện thoại!", Toast.LENGTH_SHORT).show();
            return; // Thoát khỏi hàm nếu không có số điện thoại
        }

        // Lấy thời gian delay từ EditText (nếu có)
        String delayStr = edtDelay.getText().toString();
        if (!delayStr.isEmpty()) {
            try {
                delayMillis = Long.parseLong(delayStr) * 1000; // Chuyển đổi thời gian delay từ giây sang mili giây
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Delay không hợp lệ!", Toast.LENGTH_SHORT).show();
                return; // Thoát khỏi hàm nếu thời gian delay không hợp lệ
            }
        }

        // Vô hiệu hóa các nút và EditText trong khi gửi tin nhắn
        btnSend.setEnabled(false);
        btnChooseFile.setEnabled(false);
        edtDelay.setEnabled(false);
        edtMsg.setEnabled(false);
        btnSaveLog.setEnabled(false);

        currentPhoneNumberIndex = 0; // Đặt lại chỉ số số điện thoại hiện tại về 0
        sendMessageWithDelay(); // Bắt đầu gửi tin nhắn
    }

    private void sendMessageWithDelay() {
        // Nếu vẫn còn số điện thoại để gửi
        if (currentPhoneNumberIndex < phoneNumbers.size()) {
            String phoneNumber = phoneNumbers.get(currentPhoneNumberIndex); // Lấy số điện thoại hiện tại

            currentTime = sdf.format(new Date()); // Cập nhật thời gian hiện tại

            if (isValidPhoneNumber(phoneNumber)) { // Kiểm tra tính hợp lệ của số điện thoại
                sendSMS(phoneNumber); // Gửi tin nhắn
                logBuilder.append("[" + currentTime + "] Gửi thành công: " + phoneNumber + "\n"); //Thêm log gửi thành công
                tvLog.setText(logBuilder.toString()); // Cập nhật log trên UI
            } else { //Nếu số điện thoại không hợp lệ
                logBuilder.append("[" + currentTime + "] Lỗi: Số điện thoại không hợp lệ: " + phoneNumber + "\n"); //Thêm log số điện thoại không hợp lệ
                tvLog.setText(logBuilder.toString()); // Hiển thị lỗi trên UI
            }

            processedCount++; // Tăng số lượng tin nhắn đã xử lý
            tvProcessedCount.setText("Số tin đã xử lý: " + processedCount + "/" + totalLines); // Cập nhật số lượng tin nhắn đã xử lý trên UI

            currentPhoneNumberIndex++; // Chuyển sang số điện thoại tiếp theo
            handler.postDelayed(this::sendMessageWithDelay, delayMillis); // Lặp lại hàm này sau một khoảng thời gian delay
        } else {
            // Đã gửi hết tin nhắn

            // Kích hoạt lại các nút và EditText
            btnSend.setEnabled(true);
            btnChooseFile.setEnabled(true);
            edtDelay.setEnabled(true);
            edtMsg.setEnabled(true);
            btnSaveLog.setEnabled(true);

            Toast.makeText(this, "Đã gửi xong!", Toast.LENGTH_SHORT).show(); // Thông báo cho người dùng

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

                    tvLog.setText(logBuilder.toString()); // Cập nhật UI
                }
            };
            registerReceiver(sentReceiver, new IntentFilter(SENT)); // Đăng kí
        }
        // PendingIntent deliveredPI = PendingIntent.getBroadcast(this, 0, new Intent(DELIVERED), PendingIntent.FLAG_IMMUTABLE); // Không cần thiết.

        // 3. Gửi tin nhắn, truyền PendingIntent
        try {
            String msgStr = edtMsg.getText().toString();       // Lấy nội dung tin nhắn
            SmsManager smsManager = SmsManager.getDefault();  // Lấy đối tượng SmsManager

            // Kiểm tra tin nhắn có vượt quá giới hạn ký tự không
            ArrayList<String> parts = smsManager.divideMessage(msgStr);
            if (parts.size() > 1) {
                // Tin nhắn dài, gửi nhiều phần
                ArrayList<PendingIntent> sentIntents = new  ArrayList<PendingIntent>();
                for(int i = 0; i< parts.size(); i++)
                {
                    sentIntents.add(sentPI); // Thêm pending intent cho mỗi phần
                }
                smsManager.sendMultipartTextMessage(phoneNumber, null, parts, sentIntents, null); // Gửi tin nhắn
            } else {
                // Tin nhắn ngắn, gửi bình thường
                smsManager.sendTextMessage(phoneNumber, null, msgStr, sentPI, null); // Gửi tin nhắn
            }

        } catch (Exception e) { // Bắt Exception
            currentTime = sdf.format(new Date()); // Cập nhật thời gian

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
            // Tạo file trong thư mục Downloads
            File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File logFile = new File(downloadsDir, "sms_log.txt");


            FileOutputStream fileOutputStream = new FileOutputStream(logFile); // Luồng ghi file
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream); // Writer để ghi vào luồng
            outputStreamWriter.write(logBuilder.toString()); // Ghi nội dung log vào file
            outputStreamWriter.close(); // Đóng writer
            fileOutputStream.close(); // Đóng luồng

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