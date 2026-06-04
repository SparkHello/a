package com.spark.apibalance;

import android.app.Activity;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;

public class MainActivity extends Activity {
    private LocalStore store;
    private EditText deepSeekKey;
    private EditText siliconKey;
    private TextView result;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        store = new LocalStore(this);
        setContentView(makeUi());
        deepSeekKey.setText(store.get("deepseek"));
        siliconKey.setText(store.get("silicon"));
    }

    private View makeUi() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(36, 36, 36, 36);
        scroll.addView(root);

        TextView title = new TextView(this);
        title.setText("API Balance");
        title.setTextSize(28);
        title.setGravity(Gravity.CENTER_HORIZONTAL);
        root.addView(title);

        TextView hint = new TextView(this);
        hint.setText("本地输入 API Key，点击刷新查询余额。请不要把真实 Key 提交到仓库。");
        hint.setTextSize(14);
        root.addView(hint);

        deepSeekKey = input("DeepSeek API Key");
        siliconKey = input("SiliconFlow API Key");
        root.addView(deepSeekKey);
        root.addView(siliconKey);

        Button save = button("保存 Key 到本机");
        save.setOnClickListener(v -> {
            store.put("deepseek", deepSeekKey.getText().toString().trim());
            store.put("silicon", siliconKey.getText().toString().trim());
            result.setText("已保存到本机 SharedPreferences。第一版先保证能打包和查询，后续再换 Keystore 加密版。");
        });
        root.addView(save);

        Button refreshAll = button("刷新全部余额");
        refreshAll.setOnClickListener(v -> refreshAll());
        root.addView(refreshAll);

        Button clear = button("清除本机 Key");
        clear.setOnClickListener(v -> {
            store.clear("deepseek");
            store.clear("silicon");
            deepSeekKey.setText("");
            siliconKey.setText("");
            result.setText("已清除。");
        });
        root.addView(clear);

        result = new TextView(this);
        result.setTextSize(16);
        result.setPadding(0, 24, 0, 0);
        result.setText("等待刷新。");
        root.addView(result);
        return scroll;
    }

    private EditText input(String hint) {
        EditText edit = new EditText(this);
        edit.setHint(hint);
        edit.setSingleLine(true);
        edit.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        edit.setPadding(0, 18, 0, 18);
        return edit;
    }

    private Button button(String text) {
        Button b = new Button(this);
        b.setText(text);
        return b;
    }

    private void refreshAll() {
        store.put("deepseek", deepSeekKey.getText().toString().trim());
        store.put("silicon", siliconKey.getText().toString().trim());
        result.setText("查询中...");
        new Thread(() -> {
            StringBuilder out = new StringBuilder();
            out.append(queryDeepSeek(deepSeekKey.getText().toString().trim())).append("\n\n");
            out.append(querySilicon(siliconKey.getText().toString().trim()));
            runOnUiThread(() -> result.setText(out.toString()));
        }).start();
    }

    private String queryDeepSeek(String key) {
        if (key.isEmpty()) return "DeepSeek：未填写 Key";
        try {
            String body = get("https://api.deepseek.com/user/balance", key);
            JSONObject json = new JSONObject(body);
            boolean ok = json.optBoolean("is_available", false);
            JSONArray infos = json.optJSONArray("balance_infos");
            StringBuilder sb = new StringBuilder("DeepSeek：").append(ok ? "可用" : "不可用");
            if (infos != null) {
                for (int i = 0; i < infos.length(); i++) {
                    JSONObject item = infos.getJSONObject(i);
                    sb.append("\n").append(item.optString("currency", "CNY"))
                            .append(" total=").append(item.optString("total_balance", "?"))
                            .append(" granted=").append(item.optString("granted_balance", "?"))
                            .append(" topped_up=").append(item.optString("topped_up_balance", "?"));
                }
            }
            return sb.toString();
        } catch (Exception e) {
            return "DeepSeek 查询失败：" + e.getMessage();
        }
    }

    private String querySilicon(String key) {
        if (key.isEmpty()) return "硅基流动：未填写 Key";
        try {
            String body = get("https://api.siliconflow.cn/v1/user/info", key);
            JSONObject json = new JSONObject(body);
            JSONObject data = json.optJSONObject("data");
            if (data == null) data = json;
            return String.format(Locale.US,
                    "硅基流动：%s\nbalance=%s\nchargeBalance=%s\ntotalBalance=%s",
                    data.optString("status", "unknown"),
                    data.optString("balance", "?"),
                    data.optString("chargeBalance", "?"),
                    data.optString("totalBalance", "?"));
        } catch (Exception e) {
            return "硅基流动查询失败：" + e.getMessage();
        }
    }

    private String get(String urlText, String key) throws Exception {
        URL url = new URL(urlText);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(15000);
        conn.setRequestProperty("Authorization", "Bearer " + key);
        conn.setRequestProperty("Accept", "application/json");
        int code = conn.getResponseCode();
        InputStream stream = code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) sb.append(line);
        if (code < 200 || code >= 300) throw new RuntimeException("HTTP " + code + " " + sb);
        return sb.toString();
    }
}
