package com.java.recomapp;

import android.accessibilityservice.AccessibilityService;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.LinearLayout;

import com.java.recomapp.decisiontree.Decision;
import com.java.recomapp.utils.FileUtils;
import com.java.recomapp.views.FloatButton;
import com.java.recomapp.whitelist.WhiteListActivity;
import com.java.recomapp.whitelist.WhiteListEntry;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * a service to help user simulate click event
 *
 * @author majh
 */
public class SideBarService extends AccessibilityService {

    private SideBarHideReceiver mReceiver;
    private FloatButton mFloatButton;

    private static final String ACTION_HIDE = "com.xunfeivr.maxsidebar.ACTION_HIDE";
    private static final String TAG = "side_bar_service";

    // from wsy's MyAccessibilityService
    public static Map<String, Integer> app_count;
    public static String last_packageName = "";
    public static String last_last_packageName = "";
    public static Decision decisionTree;
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
            "com.android.permissioncontroller",
            "com.java.recomapp"
    ));

    @Override
    public void onCreate() {
        super.onCreate();
        createToucher();

        ScheduledExecutorService executorService = Executors.newScheduledThreadPool(6);
        Log.i(TAG, "onCreate: before decision");
        decisionTree = new Decision(this, executorService);
        Log.i(TAG, "onCreate: after decision");
        Map<String, Boolean> whitelist_map = WhiteListActivity.getWhiteList();
        List<String> accessibleApps = new ArrayList<>();
        whitelist_map.forEach((name, is_checked) -> {
            WhiteListEntry entry = new WhiteListEntry(name, is_checked);
            if(entry.is_in_whitelist) {
                accessibleApps.add(entry.getPackage_name());
            }
        });
        decisionTree.setAccessAppNameList(accessibleApps);
    }

    @SuppressLint({"RtlHardcoded", "InflateParams"})
    private void createToucher() {
        // get window manager
        WindowManager windowManager = (WindowManager) getApplication().getSystemService(Context.WINDOW_SERVICE);
        // right arrow
        mFloatButton = new FloatButton();
        LinearLayout mFloatButtonView = mFloatButton.getView(this, false, windowManager, this);
//        // left arrow
//        mLeftArrowBar = new FloatButton();
//        LinearLayout mArrowLeft = mLeftArrowBar.getView(this, true, windowManager, this);
        // handler another bar
        mFloatButton.setAnotherArrowBar(mFloatButtonView);
//        mLeftArrowBar.setAnotherArrowBar(mArrowRight);
        // register
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_HIDE);
        mReceiver = new SideBarHideReceiver();
        mReceiver.setSideBar(mFloatButton);
        registerReceiver(mReceiver, filter);
    }

    @Override
    public void onDestroy() {
        mFloatButton.clearAll();
        unregisterReceiver(mReceiver);
        super.onDestroy();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getPackageName() != null && event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            String packageName = event.getPackageName().toString();
//            Log.d("tap_event","package name: " + packageName);
//            Log.d("tap_event","config: " + Arrays.toString(MainActivity.validFeatureList));

            if (!systemPackages.contains(packageName) && !last_packageName.equals(packageName)) {
                Integer count = app_count.get(packageName);
                if (count == null)
                    app_count.put(packageName, 1);
                else
                    app_count.put(packageName, count + 1);
                last_last_packageName = last_packageName;
                last_packageName = packageName;
                saveMap(app_count);

                decisionTree.update(packageName);
            }
        }
    }

    @Override
    public void onInterrupt() {

    }

    // from wsy's MyAccessibilityService
    @Override
    public void onServiceConnected() {
        Log.i(TAG, "onServiceConnected: ");
        app_count = getMap();

        Log.e(TAG, new JSONObject(app_count).toString());
    };

    @Override
    public boolean onUnbind(Intent intent) {
        saveMap(app_count);
        return true;
    }

    private void saveMap(Map<String, Integer> counts) {
        String result = new JSONObject(app_count).toString();
        String FILENAME = "app_times_count.txt";

        Log.d(TAG, "saving map" + result);

        FileUtils.writeStringToFile(result, new File(MainActivity.FILE_FOLDER + FILENAME));
    }

    public Map<String, Integer> getMap() {
        String FILENAME = "app_times_count.txt";
        String text = FileUtils.getFileContent(MainActivity.FILE_FOLDER + FILENAME);
        Map<String, Integer> result = new HashMap<>();
        try {
            JSONObject jsonObject = new JSONObject(text);
            //????????????
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
