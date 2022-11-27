package com.java.recomapp;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class MyAccessibilityService extends AccessibilityService {
    public String TAG = "MyAccessibilityService";
    public static Map<String, Integer> app_count;
    private String last_packageName = "";
    public static final List<String> systemPackages = new ArrayList<>(Arrays.asList(
            "com.android.systemui",
            "com.huawei.android.launcher",
            "com.bbk.launcher2",
            "com.miui.home",
            "com.android.launcher",
            "com.hihonor.android.launcher",
            "com.google.android.inputmethod.latin",
            "com.vivo.hiboard",
            "com.oppo.launcher",
            "com.huawei.ohos.inputmethod",
            "com.huawei.aod",
            "net.oneplus.launcher",
            "com.hihonor.aod",
            "com.android.incallui",
            "com.baidu.input_huawei",
            "com.baidu.input_oppo",
            "com.android.settings",
            "android",
            "com.android.mms",
            "com.miui.personalassistant",
            "com.huawei.android.totemweather",
            "com.huawei.magazine",
            "com.java.myfirsttest",
            "com.baidu.input_mi",
            "miui.systemui.plugin",
            "com.android.permissioncontroller"
    ));

    @Override
    public void onServiceConnected() {
        Log.i(TAG, "onServiceConnected: ");
        app_count = getMap();
        Log.e(TAG, new JSONObject(app_count).toString());
    };

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        Log.i(TAG, event.toString());
        if (event.getPackageName() != null) {
            String packageName = event.getPackageName().toString();
            if (!systemPackages.contains(packageName) && !last_packageName.equals(packageName)) {
                Integer count = app_count.get(packageName);
                if (count == null)
                    app_count.put(packageName, 1);
                else
                    app_count.put(packageName, count + 1);
                last_packageName = packageName;
                saveMap(app_count);
            }
        }
    }

    @Override
    public void onInterrupt() {
        saveMap(app_count);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        saveMap(app_count);
        return true;
    }

    private void saveMap(Map<String, Integer> counts) {
        String result = new JSONObject(app_count).toString();
        String FILENAME = "app_times_count.txt";

        FileOutputStream fos = null;
        try {
            fos = openFileOutput(FILENAME, Context.MODE_PRIVATE);
            fos.write(result.getBytes());
            fos.flush();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Map<String, Integer> getMap() {
        FileInputStream fis = null;
        String text="";
        try{
            fis = openFileInput("app_times_count.txt");		//获取FILE_NAME文件内容
            if(fis.available() == 0){			//如果什么都没获取到就返回空字符串
                return new HashMap<>();
            }
            byte[] readBytes = new byte[fis.available()];	//将获取数据以二进制方式保存到数组中
            fis.read(readBytes);	//这句不加就不能正常读取，我认为这句是将转为二进制的信息解码
            text = new String(readBytes, StandardCharsets.UTF_8);	//转化为utf8类型字符串

        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return new HashMap<>();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Map<String, Integer> result = new HashMap<>();
        try {
            JSONObject jsonObject = new JSONObject(text);
            //循环转换
            for (Iterator<String> it = jsonObject.keys(); it.hasNext(); ) {
                String key = it.next();
                result.put(key, (Integer)jsonObject.getInt(key));
            }
        } catch (JSONException e) {
            Log.e(TAG, "failed to create JSONObject");
        }
        return result;
    }
}