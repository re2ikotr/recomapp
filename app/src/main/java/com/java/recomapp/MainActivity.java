package com.java.recomapp;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.java.recomapp.utils.CheckAppType;
import com.java.recomapp.utils.FileUtils;
import com.java.recomapp.utils.PermissionUtil;
import com.java.recomapp.whitelist.WhiteListActivity;

import org.json.JSONObject;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * home page
 *
 * @author majh
 */
public class MainActivity extends AppCompatActivity {
    public static String FILE_FOLDER;
    public static final Gson gson = new GsonBuilder().disableHtmlEscaping().create();

    private AppCompatButton mFlatWindowButton;
    private AppCompatButton mAccessibilityButton;
    private LinearLayout other_ui_container;
    private TextView activate_service_hint;
    private static final int FLAT_REQUEST_CODE = 213;
    private static final int ACCESSIBILITY_REQUEST_CODE = 438;
    private static final int WHITELIST_REQUEST_CODE = 403;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FILE_FOLDER = getExternalMediaDirs()[0].getAbsolutePath() + "/";
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);
        mFlatWindowButton = findViewById(R.id.btn_flatwindow);
        mAccessibilityButton = findViewById(R.id.btn_accessibility);
        other_ui_container = findViewById(R.id.other_ui_container);
        other_ui_container.setVisibility(View.GONE);
        activate_service_hint = findViewById(R.id.activate_service_hint);
        flatWindowVisible();
        initWhiteList();
    }

    /**
     * float button visible
     */
    private void flatWindowVisible() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // > M,grant permission
            if (PermissionUtil.isCanDrawOverlays(this)) {
                // permission authorized,service go,button gone
                mFlatWindowButton.setVisibility(View.GONE);
                accessibilityVisible();
            } else {
                // permission unauthorized,button visible
                mFlatWindowButton.setVisibility(View.VISIBLE);
                activate_service_hint.setVisibility(View.VISIBLE);
                Toast.makeText(this,getString(R.string.permission_flatwindow_),Toast.LENGTH_SHORT).show();
            }
        } else {
            // < M,service go,gone
            mFlatWindowButton.setVisibility(View.GONE);
            accessibilityVisible();
        }
    }

    /**
     * Accessibility button visible
     */
    private void accessibilityVisible() {
        if(PermissionUtil.isAccessibilityServiceEnable(this)) {
            Toast.makeText(this,getString(R.string.permission_notice),Toast.LENGTH_SHORT).show();
            mAccessibilityButton.setVisibility(View.GONE);
            other_ui_container.setVisibility(View.VISIBLE);
            activate_service_hint.setVisibility(View.GONE);
        }else {
            mAccessibilityButton.setVisibility(View.VISIBLE);
            activate_service_hint.setVisibility(View.VISIBLE);
            Toast.makeText(this,getString(R.string.permission_accessibility_),Toast.LENGTH_SHORT).show();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public void goGetFlatWindow(View view) {
        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
        startActivityForResult(intent,FLAT_REQUEST_CODE);
    }

    public void goGetAccessibility(View view) {
        Intent accessibleIntent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        startActivityForResult(accessibleIntent,ACCESSIBILITY_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == FLAT_REQUEST_CODE) {
            flatWindowVisible();
        }else if(requestCode == ACCESSIBILITY_REQUEST_CODE){
            accessibilityVisible();
        }
    }

    /**
     * 按打开次数的简单推荐算法，次数统计信息存在本地文件。
     * 此函数用来将其删除。
     */
    public void clearLocalCache() {
    }

    /**
     * go to app white list settings
     */
    public void gotoWhiteList(View view) {
        Intent intent = new Intent(MainActivity.this, WhiteListActivity.class);
        startActivityForResult(intent, WHITELIST_REQUEST_CODE);

    }

    /**
     * init app white list file
     */
    public void initWhiteList() {
        String FILENAME = "whitelist.txt";
        File file = new File(FILE_FOLDER + FILENAME);
        if(!file.exists()) {
            Map<String, Boolean> whiteList = new HashMap<>();
            PackageManager packageManager = this.getPackageManager();
            List<PackageInfo> packageInfoList = packageManager.getInstalledPackages(0);
            for (PackageInfo packageInfo: packageInfoList) {
                if(CheckAppType.checkAppType(packageInfo.applicationInfo.packageName, packageManager) != CheckAppType.SYSTEM_APP) {
                    whiteList.put(packageInfo.applicationInfo.packageName, true);
                }
            }
            String result = new JSONObject(whiteList).toString();
            FileUtils.writeStringToFile(result, file, false);
        } else {
            Map<String, Boolean> whiteList = WhiteListActivity.getWhiteList();
            PackageManager packageManager = this.getPackageManager();
            List<PackageInfo> packageInfoList = packageManager.getInstalledPackages(0);
            Map<String, Boolean> newList = new HashMap<>();
            for (PackageInfo packageInfo: packageInfoList) {
                if(CheckAppType.checkAppType(packageInfo.applicationInfo.packageName, packageManager) != CheckAppType.SYSTEM_APP) {
                    if(whiteList.containsKey(packageInfo.applicationInfo.packageName)) {
                        newList.put(packageInfo.applicationInfo.packageName, whiteList.get(packageInfo.applicationInfo.packageName));
                    } else {
                        newList.put(packageInfo.applicationInfo.packageName, true);
                    }
                }
            }
            String result = new JSONObject(newList).toString();
            FileUtils.writeStringToFile(result, file, false);
        }
    }
}
