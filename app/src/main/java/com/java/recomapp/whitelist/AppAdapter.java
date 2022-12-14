package com.java.recomapp.whitelist;

import static java.security.AccessController.getContext;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.java.recomapp.MainActivity;
import com.java.recomapp.R;
import com.java.recomapp.SideBarService;
import com.java.recomapp.utils.FileUtils;

import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AppAdapter extends ArrayAdapter<WhiteListEntry>{
//    private int resourceId;

    private Context mContext;
    private List<WhiteListEntry> whiteList;

    private PackageManager packageManager;
    private List<PackageInfo> packageInfoList;

    public AppAdapter(Context context, List<WhiteListEntry> objects){
        super(context, 0, objects);
//        resourceId = textViewResourceId;

        mContext = context;
        whiteList = objects;

        packageManager = mContext.getPackageManager();
        packageInfoList = packageManager.getInstalledPackages(0);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent){
        WhiteListEntry app_entry = whiteList.get(position); // 获取当前项的实例

        convertView = LayoutInflater.from(getContext()).inflate(R.layout.layout_whitelist_entry, null);
        ImageView app_icon = (ImageView)convertView.findViewById(R.id.app_icon);
        TextView app_name = (TextView)convertView.findViewById(R.id.app_name);
        CheckBox app_check = (CheckBox)convertView.findViewById(R.id.app_check);

        for (PackageInfo packageInfo: packageInfoList) {
            if (packageInfo.applicationInfo.packageName.equals(app_entry.getPackage_name())) {
                app_icon.setImageDrawable(packageInfo.applicationInfo.loadIcon(packageManager));
                app_name.setText(packageInfo.applicationInfo.loadLabel(packageManager).toString());

                app_check.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        Map<String, Boolean> whiteList = WhiteListActivity.getWhiteList();
                        if(whiteList.containsKey(app_entry.getPackage_name())) {
                            whiteList.put(app_entry.getPackage_name(), isChecked);
                        } else {
                            Log.i("whitelist_check", "package doesn't exist");
                        }
                        app_entry.is_in_whitelist = isChecked;
                        app_check.setChecked(isChecked);
                        String FILENAME = "whitelist.txt";
                        File file = new File(MainActivity.FILE_FOLDER + FILENAME);
                        String result = new JSONObject(whiteList).toString();
                        FileUtils.writeStringToFile(result, file, false);


                        List<String> accessibleApps = new ArrayList<>();
                        whiteList.forEach((name, is_checked) -> {
                            WhiteListEntry entry = new WhiteListEntry(name, is_checked);
                            if(entry.is_in_whitelist) {
                                accessibleApps.add(entry.getPackage_name());
                            }
                        });
                        SideBarService.decisionTree.setAccessAppNameList(accessibleApps);
                    }
                });

                app_check.setChecked(app_entry.is_in_whitelist);
            }
        }

        return convertView;
    }
}


