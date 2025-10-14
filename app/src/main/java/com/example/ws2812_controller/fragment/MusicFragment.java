// app/src/main/java/com/example/ws2812_controller/fragment/MusicFragment.java
package com.example.ws2812_controller.fragment;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.media.audiofx.Visualizer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.ws2812_controller.R;
import com.example.ws2812_controller.adapter.MusicAdapter;
import com.example.ws2812_controller.model.Music;
import com.example.ws2812_controller.ui.VisualizerBarsView;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Locale;

public class MusicFragment extends Fragment {

    // UI
    private Button btnAdd, btnPlay, btnPause, btnStop;
    private TextView tvNowPlaying, tvSensitivity;
    private SeekBar seekSensitivity;
    private Switch switchReactive;
    private Spinner spnMode;
    private ListView listView;
    private VisualizerBarsView visualizerBars; // custom view bars

    // Audio
    private MediaPlayer mediaPlayer;
    private Visualizer visualizer;

    // Data
    private final ArrayList<Music> musics = new ArrayList<>();
    private MusicAdapter musicAdapter;
    private int currentIndex = -1;

    // Config
    private int sensitivity = 60;               // 0..100
    private String reactiveMode = "SPECTRUM";   // SPECTRUM | BEAT | ENERGY
    private static final int REQ_PICK_AUDIO = 1001;
    private static final int REQ_RECORD_AUDIO = 2001;

    // Điền IP/host ESP32 nếu muốn gửi dữ liệu (ví dụ: "http://192.168.1.50")
    private String esp32BaseUrl = ""; // để trống thì không gửi

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_music, container, false);

        // Find views
        btnAdd = v.findViewById(R.id.btnAddMusic);
        btnPlay = v.findViewById(R.id.btnPlay);
        btnPause = v.findViewById(R.id.btnPause);
        btnStop = v.findViewById(R.id.btnStop);
        tvNowPlaying = v.findViewById(R.id.tvNowPlaying);
        tvSensitivity = v.findViewById(R.id.tvSensitivity);
        seekSensitivity = v.findViewById(R.id.seekSensitivity);
        switchReactive = v.findViewById(R.id.switchReactive);
        spnMode = v.findViewById(R.id.spnMode);
        listView = v.findViewById(R.id.lvPlaylist);
        visualizerBars = v.findViewById(R.id.visualizerBars);

        // List adapter
        musicAdapter = new MusicAdapter(requireContext(), musics);
        listView.setAdapter(musicAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                playIndex(position);
                musicAdapter.setSelectedIndex(position);
            }
        });

        // Mode spinner (ẩn trong layout mới, nhưng giữ để tương thích)
        ArrayAdapter<String> modeAdapter = new ArrayAdapter<String>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                new String[]{"SPECTRUM", "BEAT", "ENERGY"});
        modeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnMode.setAdapter(modeAdapter);
        spnMode.setSelection(0);
        spnMode.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                reactiveMode = (String) parent.getItemAtPosition(position);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        });

        // Sensitivity
        tvSensitivity.setText("Sensitivity: " + sensitivity + "%");
        seekSensitivity.setProgress(sensitivity);
        seekSensitivity.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                sensitivity = progress;
                tvSensitivity.setText("Sensitivity: " + sensitivity + "%");
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) { }
            @Override public void onStopTrackingTouch(SeekBar seekBar) { }
        });

        // Buttons
        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) { openAudioPicker(); }
        });
        btnPlay.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) { resumeOrPlayFirst(); }
        });
        btnPause.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) { pausePlayback(); }
        });
        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) { stopPlayback(); }
        });

        ensureRecordAudioPermission();
        return v;
    }

    // === File picker ===
    private void openAudioPicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("audio/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, REQ_PICK_AUDIO);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent dataIntent) {
        super.onActivityResult(requestCode, resultCode, dataIntent);
        if (requestCode == REQ_PICK_AUDIO && dataIntent != null && dataIntent.getData() != null) {
            Uri uri = dataIntent.getData();

            // giữ quyền đọc lâu dài
            final int takeFlags = dataIntent.getFlags()
                    & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            try { requireContext().getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION); }
            catch (Exception ignore) { }

            String title = getDisplayName(uri);
            if (TextUtils.isEmpty(title)) title = uri.toString();

            musics.add(new Music(uri, title));
            musicAdapter.notifyDataSetChanged();

            if (currentIndex == -1) {
                playIndex(0);
                musicAdapter.setSelectedIndex(0);
            } else {
                Toast.makeText(getContext(), "Đã thêm: " + title, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private String getDisplayName(Uri uri) {
        Cursor c = null;
        try {
            c = requireContext().getContentResolver()
                    .query(uri, null, null, null, null);
            if (c != null && c.moveToFirst()) {
                int idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (idx >= 0) return c.getString(idx);
            }
        } catch (Exception ignore) {
        } finally {
            if (c != null) c.close();
        }
        return null;
    }

    // === Playback ===
    private void playIndex(int index) {
        if (index < 0 || index >= musics.size()) return;
        currentIndex = index;

        stopPlayback(); // release cũ
        mediaPlayer = new MediaPlayer();

        try {
            mediaPlayer.setDataSource(requireContext(), musics.get(index).getUri());
            mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override public void onPrepared(MediaPlayer mp) {
                    String title = (currentIndex >= 0 && currentIndex < musics.size())
                            ? musics.get(currentIndex).getTitle() : "(unknown)";
                    tvNowPlaying.setText("Now Playing: " + title);
                    mp.start();
                    setupVisualizer();
                    musicAdapter.setSelectedIndex(currentIndex);
                }
            });
            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override public void onCompletion(MediaPlayer mp) {
                    int next = currentIndex + 1;
                    if (next < musics.size()) {
                        playIndex(next);
                    } else {
                        stopPlayback();
                    }
                }
            });
            mediaPlayer.prepareAsync();
        } catch (IOException e) {
            Toast.makeText(getContext(), "Không phát được: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void resumeOrPlayFirst() {
        if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
            mediaPlayer.start();
            enableVisualizer(true);
        } else if (mediaPlayer == null && !musics.isEmpty()) {
            playIndex(0);
        } else if (musics.isEmpty()) {
            Toast.makeText(getContext(), "Hãy thêm bài hát trước", Toast.LENGTH_SHORT).show();
        }
    }

    private void pausePlayback() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            enableVisualizer(false);
        }
    }

    private void stopPlayback() {
        enableVisualizer(false);
        releaseVisualizer();

        if (mediaPlayer != null) {
            try { mediaPlayer.stop(); } catch (Exception ignore) { }
            try { mediaPlayer.release(); } catch (Exception ignore) { }
            mediaPlayer = null;
        }
        tvNowPlaying.setText("Now Playing: (none)");
        if (visualizerBars != null) visualizerBars.setLevels(0f, 0f, 0f);
    }

    // === Visualizer (FFT) ===
    private void setupVisualizer() {
        releaseVisualizer();
        if (mediaPlayer == null) return;

        try {
            int session = mediaPlayer.getAudioSessionId();
            if (session <= 0) return;

            visualizer = new Visualizer(session);
            int[] range = Visualizer.getCaptureSizeRange();
            int capture = (range != null && range.length >= 2) ? range[1] : 256;
            visualizer.setCaptureSize(capture);
            int rate = Visualizer.getMaxCaptureRate();

            visualizer.setDataCaptureListener(new Visualizer.OnDataCaptureListener() {
                @Override public void onWaveFormDataCapture(Visualizer visualizer, byte[] waveform, int samplingRate) {
                    // không dùng waveform
                }

                @Override public void onFftDataCapture(Visualizer vis, byte[] fft, int samplingRate) {
                    if (!switchReactive.isChecked()) {
                        if (visualizerBars != null) visualizerBars.setLevels(0f, 0f, 0f);
                        return;
                    }

                    float bass = 0f, mid = 0f, treble = 0f;
                    int n = fft.length / 2;
                    for (int i = 2; i < fft.length; i += 2) {
                        float re = fft[i];
                        float im = fft[i + 1];
                        float mag = (float) Math.hypot(re, im);
                        int bin = (i / 2);

                        if (bin < n * 0.10f) {
                            bass += mag;
                        } else if (bin < n * 0.45f) {
                            mid += mag;
                        } else {
                            treble += mag;
                        }
                    }

                    // scale độ nhạy
                    float sens = Math.max(10, sensitivity) / 100f;
                    bass *= sens; mid *= sens; treble *= sens;

                    // Map về 0..255
                    int b = clamp255((int) (bass / 10f));
                    int m = clamp255((int) (mid / 10f));
                    int t = clamp255((int) (treble / 10f));

                    // Cập nhật UI bars
                    if (visualizerBars != null) {
                        visualizerBars.setLevels255(b, m, t);
                    }

                    // Gửi ESP32 theo chế độ
                    if ("BEAT".equals(reactiveMode)) {
                        boolean beat = bass > (mid + treble) * 0.6f;
                        sendBeatToEsp32(beat ? 1 : 0);
                    } else if ("ENERGY".equals(reactiveMode)) {
                        int energy = clamp255((int) ((bass + mid + treble) / 20f));
                        sendEnergyToEsp32(energy);
                    } else {
                        sendSpectrumToEsp32(b, m, t);
                    }
                }
            }, rate / 2, false, true);

            visualizer.setEnabled(true);
        } catch (Throwable t) {
            Toast.makeText(getContext(), "Visualizer lỗi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            releaseVisualizer();
        }
    }

    private void enableVisualizer(boolean enable) {
        if (visualizer != null) {
            try { visualizer.setEnabled(enable); } catch (Exception ignore) { }
        }
    }

    private void releaseVisualizer() {
        if (visualizer != null) {
            try { visualizer.release(); } catch (Exception ignore) { }
            visualizer = null;
        }
    }

    // === Permission ===
    private void ensureRecordAudioPermission() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{ Manifest.permission.RECORD_AUDIO }, REQ_RECORD_AUDIO);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_RECORD_AUDIO) {
            if (grantResults.length == 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(getContext(), "Không có quyền RECORD_AUDIO, không thể phân tích FFT.", Toast.LENGTH_LONG).show();
            }
        }
    }

    // === Networking tới ESP32 (GET đơn giản) ===
    private void sendSpectrumToEsp32(int bass, int mid, int treble) {
        if (TextUtils.isEmpty(esp32BaseUrl)) return;
        final String url = String.format(Locale.US,
                "%s/music/spectrum?b=%d&m=%d&t=%d&sens=%d&mode=%s",
                esp32BaseUrl, clamp255(bass), clamp255(mid), clamp255(treble), sensitivity, reactiveMode);
        fireAndForget(url);
    }

    private void sendBeatToEsp32(int beat) {
        if (TextUtils.isEmpty(esp32BaseUrl)) return;
        final String url = String.format(Locale.US,
                "%s/music/beat?hit=%d&sens=%d", esp32BaseUrl, beat, sensitivity);
        fireAndForget(url);
    }

    private void sendEnergyToEsp32(int energy) {
        if (TextUtils.isEmpty(esp32BaseUrl)) return;
        final String url = String.format(Locale.US,
                "%s/music/energy?e=%d&sens=%d", esp32BaseUrl, clamp255(energy), sensitivity);
        fireAndForget(url);
    }

    private int clamp255(int v) {
        if (v < 0) return 0;
        if (v > 255) return 255;
        return v;
    }

    private void fireAndForget(final String urlStr) {
        new Thread(new Runnable() {
            @Override public void run() {
                HttpURLConnection conn = null;
                try {
                    URL u = new URL(urlStr);
                    conn = (HttpURLConnection) u.openConnection();
                    conn.setConnectTimeout(1000);
                    conn.setReadTimeout(1000);
                    conn.setRequestMethod("GET");
                    conn.getResponseCode(); // bỏ kết quả
                } catch (Exception ignore) {
                } finally {
                    if (conn != null) conn.disconnect();
                }
            }
        }).start();
    }

    // === Lifecycle ===
    @Override
    public void onPause() {
        super.onPause();
        enableVisualizer(false);
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopPlayback();
    }
}
