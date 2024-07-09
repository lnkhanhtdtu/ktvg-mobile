package com.example.tmp1;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.content.Context;
import android.telephony.SmsManager;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class MainActivity extends AppCompatActivity {

    ListView myListView;
    Button sendSMS;
    Button selectAll;
    ArrayList<Customer> customerList = new ArrayList<>();
    MyArrayAdapter myArrayAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try {
            loadLocates();
        } catch (Exception e){
            System.out.println("Lỗi: " + e.getMessage());
        }

        myListView = findViewById(R.id.list);
        myArrayAdapter = new MyArrayAdapter(this, R.layout.row, customerList);
        myListView.setAdapter(myArrayAdapter);
        myListView.setOnItemClickListener(myOnItemClickListener);

        sendSMS = findViewById(R.id.getresult);
        sendSMS.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                List<Customer> resultList = myArrayAdapter.getCheckedItems();
                for (Customer customer : resultList) {
                    System.out.println(customer.toString());

                    SendSmsContent(customer);
                }
            }
        });

//        selectAll = findViewById(R.id.select_all);
//        selectAll.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                if (selectAll.getText().toString().contains("Chọn tất cả")) {
//                    myArrayAdapter.selectAll();
//                    selectAll.setText("Bỏ chọn tất cả");
//                } else {
//                    myArrayAdapter.deselectAll();
//                    selectAll.setText("Chọn tất cả");
//                }
//                updateSelectedItemCount();
//            }
//        });
    }

    private void updateSelectedItemCount() {
        int count = myArrayAdapter.getCheckedItemCount();
        selectAll.setText("Chọn tất cả (" + count + ")");
    }

    private void updateSendSMSItemCount() {
        List<Customer> resultList = myArrayAdapter.getCheckedItems();
        int count = resultList.size();
        sendSMS.setText("Gửi SMS (" + count + ")");
    }

    private void SendSmsContent(Customer customer) {
        try
        {
            SmsManager smsManager=SmsManager.getDefault();
            smsManager.sendTextMessage(customer.getPhoneNumber(),null, customer.getMessageContent(),null,null);
            Toast.makeText(getApplicationContext(),"Gửi SMS tới [" + customer.getCustomerName() + "] thành công",Toast.LENGTH_LONG).show();
            addLog(customer, true, "");
            System.out.println("Gui thanh cong");
        }
        catch (Exception e)
        {
            addLog(customer, true, e.getMessage());
            System.out.println("Gui that bai");
            Toast.makeText(getApplicationContext(),"Gửi SMS tới [" + customer.getCustomerName() + "] thất bại",Toast.LENGTH_LONG).show();
        }
    };

    AdapterView.OnItemClickListener myOnItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            myArrayAdapter.toggleChecked(position);
            updateSendSMSItemCount();
        }
    };

    private class MyArrayAdapter extends ArrayAdapter<Customer> {

        private HashMap<Integer, Boolean> myChecked = new HashMap<>();


        private List<Customer> customerList;
        private SparseBooleanArray checkedItems;


        public MyArrayAdapter(Context context, int resource, List<Customer> objects) {
            super(context, resource, objects);
            for (int i = 0; i < objects.size(); i++) {
                myChecked.put(i, false);
            }

            this.customerList = objects;
            this.checkedItems = new SparseBooleanArray();
        }

        public void toggleChecked(int position) {
            if (myChecked.containsKey(position)) {
                myChecked.put(position, !myChecked.get(position));
            } else {
                myChecked.put(position, true);
            }
            notifyDataSetChanged();
        }

        public List<Customer> getCheckedItems() {
            List<Customer> checkedItems = new ArrayList<>();
            for (int i = 0; i < getCount(); i++) {
                if (myChecked.get(i) != null && myChecked.get(i)) {
                    checkedItems.add(getItem(i));
                }
            }
            return checkedItems;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder viewHolder;
            if (convertView == null) {
                LayoutInflater inflater = getLayoutInflater();
                convertView = inflater.inflate(R.layout.row, parent, false);
                viewHolder = new ViewHolder();
                viewHolder.checkedTextView = convertView.findViewById(R.id.text1);
                viewHolder.customerName = convertView.findViewById(R.id.customerName);
                viewHolder.phoneNumber = convertView.findViewById(R.id.phoneNumber);
//                viewHolder.registrationDate = convertView.findViewById(R.id.registrationDate);
//                viewHolder.expirationDate = convertView.findViewById(R.id.expirationDate);
                viewHolder.useDate = convertView.findViewById(R.id.useDate);
                viewHolder.lastTime = convertView.findViewById(R.id.lastTime);

                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }

            Customer customer = getItem(position);
            if (customer != null) {
                // Ngày hiện tại
                long currentTimeMillis = System.currentTimeMillis();

                // Ngày "20/01/2024"
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
                Date date2024 = null;
                try {
                    date2024 = sdf.parse(customer.getLastTime());
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                long date2024Millis = date2024 != null ? date2024.getTime() : 0;

                // Tính số ngày
                long diffInMillis = currentTimeMillis - date2024Millis;
                long daysBetween = diffInMillis / (1000 * 60 * 60 * 24);
                String lastTime = customer.getLastTime().equals("01/01/0001") ? " - " : customer.getLastTime() + " (Đã gửi ngày "+daysBetween+" trước)";

                viewHolder.customerName.setText(customer.getCustomerName());
                viewHolder.phoneNumber.setText(customer.getPhoneNumber());
//                viewHolder.registrationDate.setText(customer.getRegistrationDate());
//                viewHolder.expirationDate.setText(customer.getExpirationDate());
                viewHolder.useDate.setText(customer.getRegistrationDate() + " ~ " + customer.getExpirationDate() + "\n(Còn "+customer.getExpirationDays()+" ngày)");
                viewHolder.lastTime.setText(lastTime);

                Boolean checked = myChecked.get(position);
                viewHolder.checkedTextView.setChecked(checked != null && checked);
            }

            return convertView;
        }

        private class ViewHolder {
            CheckedTextView checkedTextView;
            TextView customerName;
            TextView phoneNumber;
            TextView registrationDate;
            TextView expirationDate;
            TextView useDate;
            TextView lastTime;
        }

        public void selectAll() {
            for (int i = 0; i < customerList.size(); i++) {
                checkedItems.put(i, true);
            }
            notifyDataSetChanged();
        }

        public void deselectAll() {
            checkedItems.clear();
            notifyDataSetChanged();
        }

        public int getCheckedItemCount() {
            int count = 0;
            for (int i = 0; i < checkedItems.size(); i++) {
                if (checkedItems.valueAt(i)) {
                    count++;
                }
            }
            return count;
        }

//        public List<Customer> getCheckedItems() {
//            List<Customer> checkedCustomers = new ArrayList<>();
//            for (int i = 0; i < customerList.size(); i++) {
//                if (checkedItems.get(i)) {
//                    checkedCustomers.add(customerList.get(i));
//                }
//            }
//            return checkedCustomers;
//        }
    }

    private void loadLocates() {
        String url = "https://ktvinagroup.com/api/LocateClients";

        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        System.out.println("Response from API: " + response.toString());
                        try {
                            List<Customer> customers = parseCustomers(response);
                            customerList.clear();
                            customerList.addAll(customers);
                            myArrayAdapter.notifyDataSetChanged();
                        } catch (JSONException e) {
                            e.printStackTrace();
                            Toast.makeText(MainActivity.this, "Error parsing JSON: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(MainActivity.this, "Error loading customers: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });

        Volley.newRequestQueue(this).add(request);
    }

    private void addLog(Customer customer, Boolean isSuccess, String responseMessage) {
        String url = "https://ktvinagroup.com/api/LogSmsClients";

        JSONObject logJson = new JSONObject();

        try {
            // Lấy thời gian hiện tại và định dạng theo chuẩn ISO 8601
            Date now = new Date();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
//            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            String formattedDate = sdf.format(now);

            logJson.put("phoneNumber", customer.getPhoneNumber());
            logJson.put("messageContent", customer.getMessageContent());
            logJson.put("sentTime", formattedDate);
            logJson.put("isSuccess", isSuccess);
            logJson.put("responseMessage", responseMessage);
            logJson.put("locateId", customer.getId());

        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(MainActivity.this, "Error creating JSON: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            return;
        }

        // Tạo yêu cầu POST
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, logJson,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        // Xử lý phản hồi từ API
                        System.out.println("Response from API (thành công): " + response.toString());
//                        Toast.makeText(MainActivity.this, "Customer added successfully!", Toast.LENGTH_SHORT).show();
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // Xử lý lỗi từ API
                        error.printStackTrace();
                        System.out.println("Response from API (thất bại): " + error.getMessage());
                        System.out.println("Response from API (thất bại) với json: " + logJson);
////                        Toast.makeText(MainActivity.this, "Error adding customer: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });

        // Thêm yêu cầu vào hàng đợi
        Volley.newRequestQueue(this).add(request);
    }

    private List<Customer> parseCustomers(JSONArray response) throws JSONException {
        List<Customer> customers = new ArrayList<>();
        for (int i = 0; i < response.length(); i++) {
            JSONObject jsonObject = response.getJSONObject(i);
            int id = jsonObject.getInt("id");
            String customerName = jsonObject.getString("customerName");
            String phoneNumber = jsonObject.getString("phoneNumber");
            String messageContent = jsonObject.getString("messageContent");
            String productName = jsonObject.getString("productName");
            String vehicleNumber = jsonObject.getString("vehicleNumber");
            String registrationDate = jsonObject.getString("registrationDate");
            String latestRenewalDate = jsonObject.optString("latestRenewalDate", null);
            String expirationDate = jsonObject.optString("expirationDate", null);
            String lastTime = jsonObject.getString("lastTime");
            int expirationDays = jsonObject.getInt("expirationDays");

            Customer customer = new Customer(id, customerName, phoneNumber, messageContent, productName, vehicleNumber, convertDateFormat(registrationDate), convertDateFormat(latestRenewalDate), convertDateFormat(expirationDate), convertDateFormat(lastTime), expirationDays);
            customers.add(customer);
        }
        return customers;
    }

    private String convertDateFormat(String dateString) {
        if (dateString == null || dateString.isEmpty()) {
            return "";
        }

        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        SimpleDateFormat outputFormat = new SimpleDateFormat("dd/MM/yyyy");

        try {
            Date date = inputFormat.parse(dateString); // Chuyển chuỗi thành đối tượng Date
            return outputFormat.format(date); // Chuyển đối tượng Date thành chuỗi với định dạng mong muốn
        } catch (ParseException e) {
            e.printStackTrace();
            return dateString; // Trả về chuỗi gốc nếu có lỗi
        }
    }

}