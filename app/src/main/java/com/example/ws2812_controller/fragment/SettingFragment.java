package com.example.ws2812_controller.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ws2812_controller.R;
import com.example.ws2812_controller.adapter.NetworkAdapter;
import com.example.ws2812_controller.model.WifiNetwork;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.*;

public class SettingFragment extends Fragment {

    private static final String TAG = "SettingFragment";
    private static final String ESP32_AP_IP = "192.168.71.1";
    private static final int POLL_INTERVAL = 2000; // 2 giây
    private static final int MAX_POLL_COUNT = 25; // Tối đa 50 giây

    // SharedPreferences keys
    private static final String PREFS_NAME = "ESP32_Prefs";
    private static final String KEY_CURRENT_IP = "current_esp_ip";
    private static final String KEY_LAST_STA_IP = "last_sta_ip";
    private static final String KEY_IS_CONNECTED = "is_connected";
    private static final String KEY_NETWORK_IP_MAP = "network_ip_map";

    private RecyclerView rvNetworks;
    private Button btnScan, btnClearCredentials, btnFindEsp;
    private TextView tvNetworks, tvConnectionStatus;
    private View layoutProgress;

    private NetworkAdapter adapter;
    private OkHttpClient client;
    private Handler handler = new Handler(Looper.getMainLooper());
    private ExecutorService executorService = Executors.newFixedThreadPool(10);
    private List<WifiNetwork> networkList = new ArrayList<>();

    private SharedPreferences sharedPreferences;

    private String currentEspIp = "";
    private boolean isScanning = false;
    private boolean isConnecting = false;
    private boolean isFindingEsp = false;
    private int pollCount = 0;
    private String targetSsid = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_setting, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvNetworks = view.findViewById(R.id.rvNetworks);
        btnScan = view.findViewById(R.id.btnScan);
        btnClearCredentials = view.findViewById(R.id.btnClearCredentials);
        btnFindEsp = view.findViewById(R.id.btnFindEsp);
        tvNetworks = view.findViewById(R.id.tvNetworks);
        tvConnectionStatus = view.findViewById(R.id.tvConnectionStatus);
        layoutProgress = view.findViewById(R.id.layoutProgress);

        // Khởi tạo SharedPreferences
        sharedPreferences = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        // Load saved IP từ SharedPreferences
        currentEspIp = sharedPreferences.getString(KEY_CURRENT_IP, "");
        String lastStaIp = sharedPreferences.getString(KEY_LAST_STA_IP, "");
        boolean wasConnected = sharedPreferences.getBoolean(KEY_IS_CONNECTED, false);

        if (currentEspIp.isEmpty()) {
            // Chưa có IP nào được lưu
            currentEspIp = ESP32_AP_IP;
        }

        Log.d(TAG, "Loaded saved IP: " + currentEspIp);
        Log.d(TAG, "Last STA IP: " + lastStaIp);
        Log.d(TAG, "Was connected: " + wasConnected);

        client = new OkHttpClient();

        setupRecyclerView();
        setupButtons();

        // Cập nhật UI ban đầu dựa trên trạng thái đã lưu
        updateUIForConnectionState(!currentEspIp.equals(ESP32_AP_IP) && !currentEspIp.isEmpty());

        // Kiểm tra trạng thái ban đầu
        checkConnectionStatus();
    }

    // ====================== SHARED PREFERENCES ======================
    private void saveCurrentIp(String ip) {
        currentEspIp = ip;
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_CURRENT_IP, ip);
        editor.putBoolean(KEY_IS_CONNECTED, !ip.equals(ESP32_AP_IP) && !ip.isEmpty());
        editor.apply();
        Log.d(TAG, "Saved current IP: " + ip + ", isConnected: " + !ip.equals(ESP32_AP_IP));
    }

    private void saveLastStaIp(String ip) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_LAST_STA_IP, ip);
        editor.apply();
        Log.d(TAG, "Saved last STA IP: " + ip);
    }

    private String getLastStaIp() {
        return sharedPreferences.getString(KEY_LAST_STA_IP, "");
    }

    private void clearSavedState() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(KEY_CURRENT_IP);
        editor.remove(KEY_LAST_STA_IP);
        editor.remove(KEY_IS_CONNECTED);
        editor.apply();
        Log.d(TAG, "Cleared saved state");
    }

    // ====================== UI UPDATE METHODS ======================
    private void updateUIForConnectionState(boolean isConnectedToWifi) {
        runOnUiThread(() -> {
            if (isConnectedToWifi) {
                // Đã kết nối WiFi - Ẩn nút quét và danh sách mạng
                btnScan.setVisibility(View.GONE);
                tvNetworks.setVisibility(View.GONE);
                rvNetworks.setVisibility(View.GONE);

                // Clear danh sách network
                networkList.clear();
                adapter.notifyDataSetChanged();

            } else {
                // Chế độ AP - Hiện nút quét và danh sách
                btnScan.setVisibility(View.VISIBLE);
                tvNetworks.setVisibility(View.VISIBLE);
                rvNetworks.setVisibility(View.VISIBLE);
                btnClearCredentials.setVisibility(View.GONE);

                // Reset text nếu cần
                if (networkList.isEmpty()) {
                    tvNetworks.setText("Nhấn 'Quét tìm Wifi' để bắt đầu");
                }
            }
        });
    }

    private void setupRecyclerView() {
        adapter = new NetworkAdapter(networkList, this::showPasswordDialog);
        rvNetworks.setLayoutManager(new LinearLayoutManager(getContext()));
        rvNetworks.setAdapter(adapter);
    }

    private void setupButtons() {
        btnScan.setOnClickListener(v -> scanNetworks());
        btnClearCredentials.setOnClickListener(v -> clearCredentials());
        btnFindEsp.setOnClickListener(v -> findEspOnNetwork());
    }

    private void checkConnectionStatus() {
        showProgress(true);
        updateStatus("Đang kiểm tra kết nối...", false);

        // Nếu chưa có IP, thử AP IP trước
        if (currentEspIp.isEmpty()) {
            checkApMode();
        } else {
            // Thử IP hiện tại trước
            checkSpecificIp(currentEspIp);
        }
    }

    private void checkSpecificIp(String ip) {
        Request request = new Request.Builder()
            .url("http://" + ip + "/wifi/status")
            .get()
            .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                // IP hiện tại không hoạt động
                runOnUiThread(() -> {
                    showProgress(false);
                    if (ip.equals(ESP32_AP_IP)) {
                        // AP IP cũng không hoạt động
                        updateStatus("Không tìm thấy ESP32", false);
                        updateUIForConnectionState(false);
                        btnFindEsp.setVisibility(View.VISIBLE);
                    } else {
                        // STA IP không hoạt động, thử AP IP
                        checkApMode();
                    }
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful()) {
                    runOnUiThread(() -> {
                        showProgress(false);
                        checkApMode();
                    });
                    return;
                }

                try {
                    String body = response.body().string();
                    JSONObject json = new JSONObject(body);
                    boolean connected = json.optBoolean("connected", false);
                    String staIp = json.optString("ip", "").trim();

                    runOnUiThread(() -> {
                        showProgress(false);
                        if (connected && !staIp.isEmpty() && !staIp.equals("0.0.0.0")) {
                            // Đã kết nối WiFi, có IP mới
                            saveCurrentIp(staIp);
                            saveLastStaIp(staIp);
                            updateStatus("Đã kết nối WiFi - IP: " + staIp, true);
                            updateUIForConnectionState(true);
                            btnFindEsp.setVisibility(View.GONE);
                        } else if (ip.equals(ESP32_AP_IP)) {
                            // Ở chế độ AP
                            saveCurrentIp(ESP32_AP_IP);
                            updateStatus("Chế độ AP - Sẵn sàng cấu hình", false);
                            updateUIForConnectionState(false);
                            btnFindEsp.setVisibility(View.GONE);
                        } else {
                            // IP cũ không còn hợp lệ
                            currentEspIp = "";
                            checkApMode();
                        }
                    });
                } catch (Exception e) {
                    Log.e(TAG, "Parse error", e);
                    runOnUiThread(() -> {
                        showProgress(false);
                        checkApMode();
                    });
                }
            }
        });
    }

    private void checkApMode() {
        Request request = new Request.Builder()
            .url("http://" + ESP32_AP_IP + "/wifi/status")
            .get()
            .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    showProgress(false);
                    // Không tìm thấy AP IP
                    updateStatus("Không tìm thấy ESP32", false);
                    updateUIForConnectionState(false);
                    btnFindEsp.setVisibility(View.VISIBLE);
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                runOnUiThread(() -> {
                    showProgress(false);
                    saveCurrentIp(ESP32_AP_IP);
                    updateStatus("Chế độ AP - Sẵn sàng cấu hình", false);
                    updateUIForConnectionState(false);
                    btnFindEsp.setVisibility(View.GONE);
                });
            }
        });
    }

    // ====================== TÌM ESP TRÊN MẠNG HIỆN TẠI ======================
    private void findEspOnNetwork() {
        if (isFindingEsp) return;

        isFindingEsp = true;
        btnFindEsp.setEnabled(false);
        btnFindEsp.setText("Đang tìm...");
        showProgress(true);
        updateStatus("Đang tìm thiết bị trên mạng hiện tại...", false);

        executorService.execute(() -> {
            String foundIp = discoverEspOnNetwork();

            runOnUiThread(() -> {
                isFindingEsp = false;
                btnFindEsp.setEnabled(true);
                btnFindEsp.setText("Tìm thiết bị");
                showProgress(false);

                if (foundIp != null && !foundIp.isEmpty()) {
                    saveCurrentIp(foundIp);
                    saveLastStaIp(foundIp);
                    updateStatus("Đã tìm thấy thiết bị - IP: " + foundIp, true);
                    updateUIForConnectionState(true);
                    showToast("Đã tìm thấy thiết bị tại " + foundIp);
                    btnFindEsp.setVisibility(View.GONE);
                } else {
                    updateStatus("Không tìm thấy thiết bị", false);
                    showToast("Không tìm thấy thiết bị trên mạng này. Kiểm tra kết nối ESP32-LED-XXXX");
                }
            });
        });
    }

    private String discoverEspOnNetwork() {
        try {
            WifiManager wifiManager = (WifiManager) getContext().getApplicationContext()
                .getSystemService(Context.WIFI_SERVICE);
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            int ipAddress = wifiInfo.getIpAddress();

            if (ipAddress == 0) {
                return null;
            }

            String ipString = String.format("%d.%d.%d",
                (ipAddress & 0xff),
                (ipAddress >> 8 & 0xff),
                (ipAddress >> 16 & 0xff));

            List<String> ipList = new ArrayList<>();
            for (int i = 1; i < 255; i++) {
                ipList.add(ipString + "." + i);
            }

            final String[] foundIp = {null};
            final CountDownLatch latch = new CountDownLatch(ipList.size());

            for (String ip : ipList) {
                executorService.execute(() -> {
                    if (foundIp[0] == null && pingEsp(ip)) {
                        foundIp[0] = ip;
                    }
                    latch.countDown();
                });
            }

            try {
                latch.await(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            return foundIp[0];

        } catch (Exception e) {
            Log.e(TAG, "Error discovering ESP", e);
            return null;
        }
    }

    private boolean pingEsp(String ip) {
        OkHttpClient pingClient = new OkHttpClient.Builder()
            .connectTimeout(1, TimeUnit.SECONDS)
            .readTimeout(1, TimeUnit.SECONDS)
            .build();

        Request request = new Request.Builder()
            .url("http://" + ip + "/wifi/status")
            .get()
            .build();

        try {
            Response response = pingClient.newCall(request).execute();
            if (response.isSuccessful()) {
                String body = response.body().string();
                JSONObject json = new JSONObject(body);
                return json.has("connected");
            }
        } catch (Exception e) {
            // Không phải ESP32
        }
        return false;
    }

    private void saveIpForNetwork(String ssid, String ip) {
        try {
            String mapJson = sharedPreferences.getString(KEY_NETWORK_IP_MAP, "{}");
            JSONObject map = new JSONObject(mapJson);
            map.put(ssid, ip);

            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(KEY_NETWORK_IP_MAP, map.toString());
            editor.apply();
            Log.d(TAG, "Saved IP " + ip + " for network " + ssid);
        } catch (Exception e) {
            Log.e(TAG, "Error saving IP for network", e);
        }
    }

    // ====================== SCAN NETWORKS ======================
    private void scanNetworks() {
        if (isScanning) return;

        // Chỉ cho phép quét khi ở chế độ AP
        if (!currentEspIp.equals(ESP32_AP_IP)) {
            showToast("Đang kết nối WiFi, không thể quét");
            return;
        }

        isScanning = true;
        btnScan.setEnabled(false);
        btnScan.setText("Đang quét...");
        showProgress(true);
        tvNetworks.setText("Đang quét mạng WiFi...");
        networkList.clear();
        adapter.notifyDataSetChanged();

        Request request = new Request.Builder()
            .url("http://" + ESP32_AP_IP + "/wifi/scan")
            .get()
            .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    showToast("Kiểm tra kết nối ESP32-LED-XXXX");
                    resetScanUI();
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful()) {
                    runOnUiThread(() -> {
                        showToast("Lỗi từ ESP32: HTTP " + response.code());
                        resetScanUI();
                    });
                    return;
                }

                try {
                    String body = response.body().string();
                    JSONObject json = new JSONObject(body);
                    JSONArray networks = json.optJSONArray("networks");

                    List<WifiNetwork> newList = new ArrayList<>();
                    if (networks != null) {
                        for (int i = 0; i < networks.length(); i++) {
                            JSONObject net = networks.getJSONObject(i);
                            String ssid = net.optString("ssid", "").trim();
                            if (!ssid.isEmpty()) {
                                newList.add(new WifiNetwork(
                                    ssid,
                                    net.optInt("rssi", -100),
                                    net.optString("auth", "UNKNOWN")
                                ));
                            }
                        }
                    }

                    runOnUiThread(() -> {
                        networkList.clear();
                        networkList.addAll(newList);
                        adapter.notifyDataSetChanged();

                        if (newList.isEmpty()) {
                            tvNetworks.setText("Không tìm thấy mạng WiFi nào");
                        } else {
                            tvNetworks.setText("Tìm thấy " + newList.size() + " mạng");
                        }
                        resetScanUI();
                    });

                } catch (Exception e) {
                    Log.e(TAG, "Parse error", e);
                    runOnUiThread(() -> {
                        showToast("Dữ liệu không hợp lệ");
                        resetScanUI();
                    });
                }
            }
        });
    }

    private void resetScanUI() {
        isScanning = false;
        tvNetworks.setText("Nhấn'Quét tìm WiFi' để bắt đầu");
        btnScan.setEnabled(true);
        btnScan.setText("Quét tìm Wifi");
        showProgress(false);
    }

    // ====================== CONNECT TO WIFI ======================
    private void connectToWifi(String ssid, String password) {
        if (isConnecting) return;

        targetSsid = ssid;
        isConnecting = true;
        showProgress(true);
        updateStatus("Đang gửi thông tin WiFi...", false);

        String form = "ssid=" + urlEncode(ssid) + "&password=" + urlEncode(password);
        RequestBody body = RequestBody.create(form, MediaType.parse("application/x-www-form-urlencoded"));

        Request request = new Request.Builder()
            .url("http://" + ESP32_AP_IP + "/wifi/connect")
            .post(body)
            .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    isConnecting = false;
                    showProgress(false);
                    showToast("Không thể gửi yêu cầu");
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String respBody = response.body().string();

                try {
                    JSONObject json = new JSONObject(respBody);
                    String status = json.optString("status", "");

                    runOnUiThread(() -> {
                        isConnecting = false;
                        showProgress(false);

                        if ("ok".equals(status)) {
                            // Mật khẩu đúng, yêu cầu người dùng chuyển mạng
                            showSwitchNetworkDialog(ssid);
                        } else {
                            showToast(json.optString("message", "Kết nối thất bại"));
                        }
                    });
                } catch (Exception e) {
                    Log.e(TAG, "Parse response error", e);
                    runOnUiThread(() -> {
                        isConnecting = false;
                        showProgress(false);
                        showToast("Phản hồi không hợp lệ");
                    });
                }
            }
        });
    }

    private void showSwitchNetworkDialog(String ssid) {
        new AlertDialog.Builder(requireContext())
            .setTitle("Kết nối thành công!")
            .setMessage("ESP32 đã nhận thông tin WiFi.\n\n" +
                "Vui lòng:\n" +
                "1. Vào Cài đặt WiFi trên điện thoại\n" +
                "2. Chuyển sang mạng: " + ssid + "\n" +
                "3. Quay lại app để kiểm tra kết nối\n\n" +
                "Sau khi chuyển mạng, nhấn OK để tự động tìm ESP32.")
            .setPositiveButton("OK", (d, w) -> {
                updateStatus("Đang tìm ESP32 trên mạng mới...", false);
                startAutoDiscovery();
            })
            .setCancelable(false)
            .show();
    }

    private void autoDiscoverEsp() {
        showProgress(true);
        updateStatus("Đang tự động tìm ESP32...", false);

        // Ẩn nút quét WiFi khi đang tìm ESP32
        btnScan.setEnabled(false);

        executorService.execute(() -> {
            String foundIp = discoverEspOnNetwork();

            runOnUiThread(() -> {
                showProgress(false);
                btnScan.setEnabled(true);

                if (foundIp != null && !foundIp.isEmpty()) {
                    saveCurrentIp(foundIp);
                    saveLastStaIp(foundIp);
                    updateStatus("Đã tìm thấy ESP32 - IP: " + foundIp, true);
                    updateUIForConnectionState(true);
                    showToast("Tự động tìm thấy ESP32 tại " + foundIp);
                    btnFindEsp.setVisibility(View.GONE);
                } else {
                    updateStatus("Không tìm thấy ESP32", false);
                    updateUIForConnectionState(false);
                    showToast("Không tìm thấy ESP32, vui lòng thử tìm thủ công");
                    btnFindEsp.setVisibility(View.VISIBLE);
                }
            });
        });
    }

    private void startAutoDiscovery() {
        pollCount = 0;
        handler.postDelayed(this::autoDiscoverEsp, 1000);
    }

    private void stopPolling() {
        handler.removeCallbacks(this::autoDiscoverEsp);
    }

    private final Runnable autoDiscoverEspRunnable = new Runnable() {
        @Override
        public void run() {
            pollCount++;

            if (pollCount > MAX_POLL_COUNT) {
                runOnUiThread(() -> {
                    updateStatus("Không tìm thấy ESP32 trên mạng mới", false);
                    updateUIForConnectionState(false);
                    showToast("Vui lòng thủ công tìm ESP32");
                    btnFindEsp.setVisibility(View.VISIBLE);
                });
                return;
            }

            runOnUiThread(() -> {
                updateStatus("Đang tìm ESP32... (" + pollCount + "/" + MAX_POLL_COUNT + ")", false);
            });

            // Tìm ESP32 trên mạng hiện tại
            executorService.execute(() -> {
                String foundIp = discoverEspOnNetwork();

                runOnUiThread(() -> {
                    if (foundIp != null && !foundIp.isEmpty()) {
                        stopPolling();
                        saveCurrentIp(foundIp);
                        saveLastStaIp(foundIp);
                        updateStatus("✓ Đã kết nối WiFi - IP: " + foundIp, true);
                        updateUIForConnectionState(true);

                        new AlertDialog.Builder(requireContext())
                            .setTitle("Kết nối hoàn tất!")
                            .setMessage("Đã tự động tìm thấy ESP32.\n" +
                                "IP mới: " + foundIp + "\n\n" +
                                "App sẽ sử dụng IP này để điều khiển LED.")
                            .setPositiveButton("OK", null)
                            .show();
                    } else {
                        // Chưa tìm thấy, tiếp tục
                        handler.postDelayed(autoDiscoverEspRunnable, POLL_INTERVAL);
                    }
                });
            });
        }
    };

    private void clearCredentials() {
        // Chỉ cho phép xóa khi đang ở IP STA mới
        if (currentEspIp.equals(ESP32_AP_IP) || currentEspIp.isEmpty()) {
            showToast("Chỉ xóa được khi ESP32 đã kết nối WiFi");
            return;
        }

        new AlertDialog.Builder(requireContext())
            .setTitle("Xác nhận xóa")
            .setMessage("Xóa thông tin WiFi đã lưu trên ESP32?\n\n" +
                "ESP32 sẽ khởi động lại về chế độ AP.")
            .setPositiveButton("Xóa", (d, w) -> performClear())
            .setNegativeButton("Hủy", null)
            .show();
    }

    private void performClear() {
        showProgress(true);
        updateStatus("Đang xóa thông tin WiFi...", false);

        Request request = new Request.Builder()
            .url("http://" + currentEspIp + "/wifi/credentials")
            .delete()
            .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    showProgress(false);
                    showToast("Không thể kết nối đến ESP32");
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                runOnUiThread(() -> {
                    showProgress(false);

                    if (response.isSuccessful()) {
                        // Reset về AP mode
                        saveCurrentIp(ESP32_AP_IP);
                        saveLastStaIp("");
                        updateUIForConnectionState(false);

                        new AlertDialog.Builder(requireContext())
                            .setTitle("Đã xóa")
                            .setMessage("ESP32 đang khởi động lại về chế độ AP.\n\n" +
                                "Vui lòng:\n" +
                                "1. Chuyển về mạng ESP32\n" +
                                "2. Đợi 10 giây\n" +
                                "3. Quay lại app để cấu hình mới")
                            .setPositiveButton("OK", (d, w) -> {
                                // Đợi một chút rồi check lại status
                                handler.postDelayed(() -> checkConnectionStatus(), 10000);
                            })
                            .show();
                    } else {
                        showToast("Lỗi: " + response.code());
                    }
                });
            }
        });
    }

    private void showPasswordDialog(WifiNetwork network) {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_wifi_password, null);
        EditText etPassword = view.findViewById(R.id.etPassword);
        TextView tvSsid = view.findViewById(R.id.tvSsid);
        tvSsid.setText("Kết nối: " + network.ssid);

        new AlertDialog.Builder(requireContext())
            .setTitle("Nhập mật khẩu WiFi")
            .setView(view)
            .setPositiveButton("Kết nối", (d, w) -> {
                String pass = etPassword.getText().toString().trim();
                if (pass.isEmpty() && !network.auth.equals("OPEN")) {
                    showToast("Vui lòng nhập mật khẩu");
                    return;
                }
                connectToWifi(network.ssid, pass);
            })
            .setNegativeButton("Hủy", null)
            .show();
    }

    private void updateStatus(String message, boolean connected) {
        tvConnectionStatus.setText(message);
        tvConnectionStatus.setTextColor(getResources().getColor(
            connected ? android.R.color.holo_green_dark : android.R.color.holo_orange_dark));
    }

    private void showProgress(boolean show) {
        layoutProgress.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void showToast(String msg) {
        Toast.makeText(getContext(), msg, Toast.LENGTH_LONG).show();
    }

    private void runOnUiThread(Runnable r) {
        if (getActivity() != null) getActivity().runOnUiThread(r);
    }

    private String urlEncode(String s) {
        try {
            return java.net.URLEncoder.encode(s, "UTF-8");
        } catch (Exception e) {
            return s;
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        stopPolling();

        // Lưu trạng thái hiện tại
        if (!currentEspIp.isEmpty()) {
            saveCurrentIp(currentEspIp);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume - Fragment được hiển thị lại");

        // Cập nhật UI dựa trên currentEspIp
        updateUIForConnectionState(!currentEspIp.equals(ESP32_AP_IP) && !currentEspIp.isEmpty());

        // Kiểm tra lại trạng thái khi quay lại app
        handler.postDelayed(() -> {
            if (getActivity() != null && isAdded()) {
                checkConnectionStatus();
            }
        }, 1000);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TAG, "onDestroyView");

        // Lưu trạng thái cuối cùng
        if (!currentEspIp.isEmpty()) {
            saveCurrentIp(currentEspIp);
        }

        stopPolling();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}