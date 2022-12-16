package com.java.recomapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

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
    private static final int FLAT_REQUEST_CODE = 213;
    private static final int ACCESSIBILITY_REQUEST_CODE = 438;
    private static final int WHITELIST_REQUEST_CODE = 403;
    public static final Gson gson = new GsonBuilder().disableHtmlEscaping().create();

    private AppCompatButton mFlatWindowButton;
    private AppCompatButton mAccessibilityButton;
    private LinearLayout other_ui_container;
    private LinearLayout switch_container;
    private TextView activate_service_hint;
    private Button btn_grant_permission;
    private SwitchCompat enable_decision_tree;
    private SwitchCompat enable_time;
    private SwitchCompat enable_app;
    private SwitchCompat enable_device;
    private SwitchCompat enable_noise;
    private SwitchCompat enable_position;
    private SwitchCompat enable_steps;

    public static boolean[] validFeatureList; // 决策树考虑的因素
    public static boolean useDecisionTree;    // 是否用决策树算法
    // 要申请的权限
    private String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.RECORD_AUDIO};

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

        validFeatureList = new boolean[]{true, true, true, true, true, true};
        switch_container = findViewById(R.id.switch_container);
        enable_time = findViewById(R.id.enable_time);
        enable_time.setChecked(true);
        enable_time.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked){
                    validFeatureList[0] = true;
                }else {
                    validFeatureList[0] = false;
                }
                SideBarService.decisionTree.setValidFeatureList(validFeatureList);
            }
        });
        enable_app = findViewById(R.id.enable_app);
        enable_app.setChecked(true);
        enable_app.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked){
                    validFeatureList[1] = true;
                }else {
                    validFeatureList[1] = false;
                }
                SideBarService.decisionTree.setValidFeatureList(validFeatureList);
            }
        });
        enable_device = findViewById(R.id.enable_device);
        enable_device.setChecked(true);
        enable_device.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked){
                    validFeatureList[2] = true;
                }else {
                    validFeatureList[2] = false;
                }
                SideBarService.decisionTree.setValidFeatureList(validFeatureList);
            }
        });
        enable_noise = findViewById(R.id.enable_noise);
        enable_noise.setChecked(true);
        enable_noise.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked){
                    validFeatureList[3] = true;
                }else {
                    validFeatureList[3] = false;
                }
                SideBarService.decisionTree.setValidFeatureList(validFeatureList);
            }
        });
        enable_position = findViewById(R.id.enable_position);
        enable_position.setChecked(true);
        enable_position.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked){
                    validFeatureList[4] = true;
                }else {
                    validFeatureList[4] = false;
                }
                SideBarService.decisionTree.setValidFeatureList(validFeatureList);
            }
        });
        enable_steps = findViewById(R.id.enable_steps);
        enable_steps.setChecked(true);
        enable_steps.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked){
                    validFeatureList[5] = true;
                }else {
                    validFeatureList[5] = false;
                }
                SideBarService.decisionTree.setValidFeatureList(validFeatureList);
            }
        });

        enable_decision_tree = findViewById(R.id.enable_decision_tree);
        enable_decision_tree.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                setPermissionBtn();
                if (enable_decision_tree.isChecked()) {
                    useDecisionTree = true;
                }
                else {
                    useDecisionTree = false;
                }
            }
        });
        btn_grant_permission = findViewById(R.id.btn_grant_permission);
        setPermissionBtn();
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
     * 检查当前权限获取情况，设置按钮的 Visible 情况
     */
    private void setPermissionBtn() {
        Log.d("test","is_checked: " + enable_decision_tree.isChecked());
        if (!enable_decision_tree.isChecked()) {
            btn_grant_permission.setVisibility(View.GONE);
            switch_container.setVisibility(View.GONE);
            return;
        }
        int i = ContextCompat.checkSelfPermission(getApplicationContext(), permissions[0]);
        int l = ContextCompat.checkSelfPermission(getApplicationContext(), permissions[1]);
        // 权限是否已经 授权 GRANTED---授权  DINIED---拒绝
        if (i != PackageManager.PERMISSION_GRANTED || l != PackageManager.PERMISSION_GRANTED) {
            btn_grant_permission.setVisibility(View.VISIBLE);
            switch_container.setVisibility(View.GONE);
        }
        else {
            btn_grant_permission.setVisibility(View.GONE);
            switch_container.setVisibility(View.VISIBLE);
        }
    }

    /**
     * 点击 btn_grant_permission 触发获取权限
     */
    public void grantPermission(View view) {
        int i = ContextCompat.checkSelfPermission(getApplicationContext(), permissions[0]);
        int l = ContextCompat.checkSelfPermission(getApplicationContext(), permissions[1]);
        // 权限是否已经 授权 GRANTED---授权  DINIED---拒绝
        if (i != PackageManager.PERMISSION_GRANTED || l != PackageManager.PERMISSION_GRANTED) {
            // 如果没有授予该权限，就去提示用户请求
            startRequestPermission();
        }
    }

    /**
     * 按打开次数的简单推荐算法，次数统计信息存在本地文件。
     * 此函数用来将其删除。
     */
    public void clearLocalCache(View view) {

    }

    /**
     * 开始提交请求权限
     */
    private void startRequestPermission() {
        ActivityCompat.requestPermissions(this, permissions, 321);
    }

    /**
     * 用户权限申请的回调方法
     * @param requestCode: int
     * @param permissions: string[]
     * @param grantResults: int[]
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 321) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                //如果没有获取权限，那么可以提示用户去设置界面--->应用权限开启权限
            } else {
                //获取权限成功提示，可以不要
                Toast toast = Toast.makeText(this, "获取GPS, 麦克风权限成功", Toast.LENGTH_LONG);
                toast.show();
            }
            setPermissionBtn();
        }
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
