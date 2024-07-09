package com.example.tmp1;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.content.Context;
import android.telephony.SmsManager;
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
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    ListView myListView;
    Button getResult;
    ArrayList<Customer> customerList = new ArrayList<>();
    MyArrayAdapter myArrayAdapter;

    public class Customer {
        private int id;
        private String customerName;
        private String phoneNumber;
        private String productName;
        private String vehicleNumber;
        private String registrationDate;
        private String latestRenewalDate;
        private String expirationDate;
        private String lastTime;
        private int expirationDays;

        public Customer(int id, String customerName, String phoneNumber, String productName, String vehicleNumber, String registrationDate, String latestRenewalDate, String expirationDate, String lastTime, int expirationDays) {
            this.id = id;
            this.customerName = customerName;
            this.phoneNumber = phoneNumber;
            this.productName = productName;
            this.vehicleNumber = vehicleNumber;
            this.registrationDate = registrationDate;
            this.latestRenewalDate = latestRenewalDate;
            this.expirationDate = expirationDate;
            this.lastTime = lastTime;
            this.expirationDays = expirationDays;
        }

        public String getCustomerName() {
            return customerName;
        }

        public String getPhoneNumber() {
            return "SĐT: " + phoneNumber;
        }

        public String getRegistrationDate() {
            return registrationDate;
        }

        public String getExpirationDate() {
            return expirationDate;
        }

        public String getLastTime() {
            return lastTime;
        }
    }

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

        getResult = findViewById(R.id.getresult);
        getResult.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                List<Customer> resultList = myArrayAdapter.getCheckedItems();
                for (Customer customer : resultList) {
                    String output = "Khách hàng: " + customer.getCustomerName()
                            + " SDT: " + customer.getPhoneNumber()
                            + " NGÀY ĐK: " + customer.getRegistrationDate()
                            + " NGÀY HĐ: " + customer.getExpirationDate()
                            + " GỬI GẦN NHẤT: " + customer.getLastTime();
                    System.out.println(output);
                    Toast.makeText(getApplicationContext(), output, Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    AdapterView.OnItemClickListener myOnItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            myArrayAdapter.toggleChecked(position);
        }
    };

    private class MyArrayAdapter extends ArrayAdapter<Customer> {

        private HashMap<Integer, Boolean> myChecked = new HashMap<>();

        public MyArrayAdapter(Context context, int resource, List<Customer> objects) {
            super(context, resource, objects);
            for (int i = 0; i < objects.size(); i++) {
                myChecked.put(i, false);
            }
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
                viewHolder.registrationDate = convertView.findViewById(R.id.registrationDate);
                viewHolder.expirationDate = convertView.findViewById(R.id.expirationDate);
                viewHolder.lastTime = convertView.findViewById(R.id.lastTime);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }

            Customer customer = getItem(position);
            if (customer != null) {
                viewHolder.customerName.setText(customer.getCustomerName());
                viewHolder.phoneNumber.setText(customer.getPhoneNumber());
                viewHolder.registrationDate.setText(customer.getRegistrationDate());
                viewHolder.expirationDate.setText(customer.getExpirationDate());
                viewHolder.lastTime.setText(customer.getLastTime());

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
            TextView lastTime;
        }
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

    private List<Customer> parseCustomers(JSONArray response) throws JSONException {
        List<Customer> customers = new ArrayList<>();
        for (int i = 0; i < response.length(); i++) {
            JSONObject jsonObject = response.getJSONObject(i);
            int id = jsonObject.getInt("id");
            String customerName = jsonObject.getString("customerName");
            String phoneNumber = jsonObject.getString("phoneNumber");
            String productName = jsonObject.getString("productName");
            String vehicleNumber = jsonObject.getString("vehicleNumber");
            String registrationDate = jsonObject.getString("registrationDate");
            String latestRenewalDate = jsonObject.optString("latestRenewalDate", null);
            String expirationDate = jsonObject.optString("expirationDate", null);
            String lastTime = jsonObject.getString("lastTime");
            int expirationDays = jsonObject.getInt("expirationDays");

            Customer customer = new Customer(id, customerName, phoneNumber, productName, vehicleNumber, convertDateFormat(registrationDate), convertDateFormat(latestRenewalDate), convertDateFormat(expirationDate), convertDateFormat(lastTime), expirationDays);
            customers.add(customer);
        }
        return customers;
    }

    public static String convertDateFormat(String dateString) {
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



//package com.example.tmp1;
//
//import androidx.appcompat.app.AppCompatActivity;
//
//import java.text.SimpleDateFormat;
//import java.util.ArrayList;
//import java.util.Calendar;
//import java.util.Date;
//import java.util.HashMap;
//import java.util.List;
//
//import android.os.Bundle;
//import android.content.Context;
//import android.telephony.SmsManager;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.View.OnClickListener;
//import android.view.ViewGroup;
//import android.widget.AdapterView;
//import android.widget.AdapterView.OnItemClickListener;
//import android.widget.ArrayAdapter;
//import android.widget.Button;
//import android.widget.CheckedTextView;
//import android.widget.ListView;
//import android.widget.TextView;
//import android.widget.Toast;
//
//import com.android.volley.Request;
//import com.android.volley.Response;
//import com.android.volley.VolleyError;
//import com.android.volley.toolbox.JsonArrayRequest;
//import com.android.volley.toolbox.JsonObjectRequest;
//import com.android.volley.toolbox.Volley;
//
//import org.json.JSONArray;
//import org.json.JSONException;
//import org.json.JSONObject;
//
//public class MainActivity extends AppCompatActivity {
//
//    ListView myListView;
//    Button getResult;
//
//    public class Customer {
//        private int id;
//        private String customerName;
//        private String phoneNumber;
//        private String productName;
//        private String vehicleNumber;
//        private String registrationDate;
//        private String latestRenewalDate;
//        private String expirationDate;
//        private String lastTime;
//        private int expirationDays;
//
//        public Customer(int id, String customerName, String phoneNumber, String productName, String vehicleNumber, String registrationDate, String latestRenewalDate, String expirationDate, String lastTime, int expirationDays) {
//            this.id = id;
//            this.customerName = customerName;
//            this.phoneNumber = phoneNumber;
//            this.productName = productName;
//            this.vehicleNumber = vehicleNumber;
//            this.registrationDate = registrationDate;
//            this.latestRenewalDate = latestRenewalDate;
//            this.expirationDate = expirationDate;
//            this.lastTime = lastTime;
//            this.expirationDays = expirationDays;
//        }
//
//        public int getId() {
//            return id;
//        }
//
//        public String getCustomerName() {
//            return customerName;
//        }
//
//        public String getPhoneNumber() {
//            return "SĐT: " + phoneNumber;
//        }
//
//        public String getProductName() {
//            return productName;
//        }
//
//        public String getVehicleNumber() {
//            return vehicleNumber;
//        }
//
//        public String getRegistrationDate() {
//            return registrationDate;
//        }
//
//        public String getLatestRenewalDate() {
//            return latestRenewalDate;
//        }
//
//        public String getExpirationDate() {
//            return expirationDate;
//        }
//
//        public String getLastTime() {
//            return lastTime;
//        }
//
//        public int getExpirationDays() {
//            return expirationDays;
//        }
//    }
//
//    private ArrayList<Customer> customerList = new ArrayList<>();
//
//    private static Date createDate(int year, int month, int day) {
//        Calendar calendar = Calendar.getInstance();
//        calendar.set(year, month - 1, day);
//        return calendar.getTime();
//    }
//
//    MyArrayAdapter myArrayAdapter;
//
//    @Override
//    public void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//
//        loadLocates();
//
//        setContentView(R.layout.activity_main);
//
//        myListView = (ListView)findViewById(R.id.list);
//
//        myArrayAdapter = new MyArrayAdapter(
//                this,
//                R.layout.row,
//                android.R.id.text1,
//                customerList
//        );
//
//        myListView.setAdapter(myArrayAdapter);
//        myListView.setOnItemClickListener(myOnItemClickListener);
//
//        getResult = (Button)findViewById(R.id.getresult);
//        getResult.setOnClickListener(new OnClickListener(){
//
//            @Override
//            public void onClick(View v) {
//                String result = "";
//
//                //getCheckedItems
//                List<Customer> resultList = myArrayAdapter.getCheckedItems();
//                for(int i = 0; i < resultList.size(); i++){
////                    result += String.valueOf(resultList.get(i)) + "\n";
//
////                        System.out.println("Gửi tin nhắn lần thứ " + i);
//
//                    String output = "Khách hàng: " + resultList.get(i).getCustomerName()
//                            + " SDT: " + resultList.get(i).getPhoneNumber()
//                            + " NGÀY ĐK: " + resultList.get(i).getRegistrationDate()
//                            + " NGÀY HĐ: " + resultList.get(i).getExpirationDate()
//                            + " GỬI GẦN NHẤT: " + resultList.get(i).getLastTime();
//                    System.out.println(output);
//
//                    try {
//                        Thread.sleep(500);
//                    } catch (InterruptedException e) {
//                        throw new RuntimeException(e);
//                    }
//
//                    Toast.makeText(getApplicationContext(), output, Toast.LENGTH_LONG).show();
//
////                    SendSmsContent("0818597397", "Test gui sms " + i);
//                }
//
//                myArrayAdapter.getCheckedItemPositions().toString();
//
//                System.out.println("===> Result: " + result);
//
//
//
////                Toast.makeText(MainActivity.this, result, Toast.LENGTH_LONG).show();
//            }});
//
//    }
//
//    OnItemClickListener myOnItemClickListener = new OnItemClickListener(){
//
//        @Override
//        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//            myArrayAdapter.toggleChecked(position);
//        }};
//
//    private void SendSmsContent(String number, String msg) {
//        try {
//            SmsManager smsManager=SmsManager.getDefault();
//            smsManager.sendTextMessage(number,null,msg,null,null);
//            Toast.makeText(getApplicationContext(),"Gửi tin nhă thành công",Toast.LENGTH_LONG).show();
//
////            Toast toast=Toast.makeText(getApplicationContext(),"Gửi SMS thanh cong",Toast.LENGTH_SHORT);
////            toast.setMargin(50,50);
////            toast.show();
//
//            System.out.println("Gui thanh cong");
//        }catch (Exception e)
//        {
//            System.out.println("Gui that bai");
//            Toast.makeText(getApplicationContext(),"Some fields is Empty",Toast.LENGTH_LONG).show();
//        }
//    };
//
//    private class MyArrayAdapter extends ArrayAdapter<Customer> {
//
//        private HashMap<Integer, Boolean> myChecked = new HashMap<>();
//
//        public MyArrayAdapter(Context context, int resource, int textViewResourceId, List<Customer> objects) {
//            super(context, resource, textViewResourceId, objects);
//            for (int i = 0; i < objects.size(); i++) {
//                myChecked.put(i, false);
//            }
//        }
//
//        public void toggleChecked(int position) {
//            myChecked.put(position, !myChecked.get(position));
//            notifyDataSetChanged();
//        }
//
//        public List<Integer> getCheckedItemPositions() {
//            List<Integer> checkedItemPositions = new ArrayList<>();
//            for (int i = 0; i < myChecked.size(); i++) {
//                if (myChecked.get(i)) {
//                    checkedItemPositions.add(i);
//                }
//            }
//            return checkedItemPositions;
//        }
//
//        public List<Customer> getCheckedItems() {
//            List<Customer> checkedItems = new ArrayList<>();
//            for (int i = 0; i < myChecked.size(); i++) {
//                if (myChecked.get(i)) {
//                    checkedItems.add(customerList.get(i));
//                }
//            }
//            return checkedItems;
//        }
//
//        @Override
//        public View getView(int position, View convertView, ViewGroup parent) {
//            View row = convertView;
//            ViewHolder viewHolder;
//
//            if (row == null) {
//                LayoutInflater inflater = getLayoutInflater();
//                row = inflater.inflate(R.layout.row, parent, false);
//                viewHolder = new ViewHolder();
//                viewHolder.checkedTextView = row.findViewById(R.id.text1);
//                viewHolder.customerName = row.findViewById(R.id.customerName);
//                viewHolder.phoneNumber = row.findViewById(R.id.phoneNumber);
//                viewHolder.registrationDate = row.findViewById(R.id.registrationDate);
//                viewHolder.expirationDate = row.findViewById(R.id.expirationDate);
//                viewHolder.lastTime = row.findViewById(R.id.lastTime);
//                row.setTag(viewHolder);
//            } else {
//                viewHolder = (ViewHolder) row.getTag();
//            }
//
//            Customer customer = getItem(position);
//            if (customer != null) {
//                viewHolder.customerName.setText(customer.getCustomerName());
//                viewHolder.phoneNumber.setText(customer.getPhoneNumber());
//                viewHolder.registrationDate.setText(customer.getRegistrationDate());
//                viewHolder.expirationDate.setText(customer.getExpirationDate());
//                viewHolder.lastTime.setText(customer.getLastTime());
//
//                Boolean checked = myChecked.get(position);
//                if (checked != null) {
//                    viewHolder.checkedTextView.setChecked(checked);
//                }
//            }
//
//            return row;
//        }
//
//        private class ViewHolder {
//            CheckedTextView checkedTextView;
//            TextView customerName;
//            TextView phoneNumber;
//            TextView registrationDate;
//            TextView expirationDate;
//            TextView lastTime;
//        }
//    }
//
//
//
//    private void loadLocates() {
//        String url = "https://ktvinagroup.com/api/LocateClients";
//
//        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null,
//                new Response.Listener<JSONArray>() {
//                    @Override
//                    public void onResponse(JSONArray response) {
//                        // Debug để kiểm tra response từ API
//                        System.out.println("Response from API: " + response.toString());
//
//                        try {
//                            // Parse dữ liệu JSON thành danh sách khách hàng
//                            List<Customer> customers = parseCustomers(response);
//
//                            // Cập nhật danh sách khách hàng và thông báo cho ListView biết dữ liệu đã thay đổi
//                            customerList.clear(); // Xóa dữ liệu cũ
//                            customerList.addAll(customers); // Thêm dữ liệu mới từ API
//                            // Nếu bạn có adapter cho ListView, hãy gọi notifyDataSetChanged() tại đây
//                            // customerListViewAdapter.notifyDataSetChanged();
//                        } catch (JSONException e) {
//                            e.printStackTrace();
//                            Toast.makeText(MainActivity.this, "Error parsing JSON", Toast.LENGTH_SHORT).show();
//                        }
//                    }
//                },
//                new Response.ErrorListener() {
//                    @Override
//                    public void onErrorResponse(VolleyError error) {
//                        // Xử lý lỗi khi không thể kết nối hoặc nhận dữ liệu từ API
//                        Toast.makeText(MainActivity.this, "Error loading customers", Toast.LENGTH_SHORT).show();
//                    }
//                });
//
//        // Thêm request vào hàng đợi của Volley
//        Volley.newRequestQueue(this).add(request);
//    }
//
//    private List<Customer> parseCustomers(JSONArray response) throws JSONException {
//        List<Customer> customers = new ArrayList<>();
//
//        for (int i = 0; i < response.length(); i++) {
//            JSONObject jsonObject = response.getJSONObject(i);
//
//            int id = jsonObject.getInt("id");
//            String customerName = jsonObject.getString("customerName");
//            String phoneNumber = jsonObject.getString("phoneNumber");
//            String productName = jsonObject.getString("productName");
//            String vehicleNumber = jsonObject.getString("vehicleNumber");
//            String registrationDate = jsonObject.getString("registrationDate");
//            String latestRenewalDate = jsonObject.optString("latestRenewalDate", null);
//            String expirationDate = jsonObject.optString("expirationDate", null);
//            String lastTime = jsonObject.getString("lastTime");
//            int expirationDays = jsonObject.getInt("expirationDays");
//
//            Customer customer = new Customer(id, customerName, phoneNumber, productName, vehicleNumber, registrationDate, latestRenewalDate, expirationDate, lastTime, expirationDays);
//
//            customers.add(customer);
//        }
//
//        return customers;
//    }
//}