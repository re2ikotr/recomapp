package com.java.recomapp.whitelist;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import com.java.recomapp.MainActivity;
import com.java.recomapp.R;
import com.java.recomapp.utils.FileUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


/**
 * home page
 *
 * @author majh
 */
public class WhiteListActivity extends AppCompatActivity {

    private ListView whiteList;
    private List<WhiteListEntry> whiteListApps;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_white_list);
        whiteList = (ListView)findViewById(R.id.white_list);

        whiteListApps = new ArrayList<>();
        Map<String, Boolean> whitelist_map = getWhiteList();
        whitelist_map.forEach((name, is_checked) -> {
            WhiteListEntry entry = new WhiteListEntry(name, is_checked);
            whiteListApps.add(entry);
        });

        whiteList.setAdapter(new AppAdapter(this, whiteListApps));
    }

    /**
     * go to app white list settings
     */
    public void returnMain(View view) {
        finish();
    }

    public static Map<String, Boolean> getWhiteList() {
        String FILENAME = "whitelist.txt";
        String text = FileUtils.getFileContent(MainActivity.FILE_FOLDER + FILENAME);
        Map<String, Boolean> result = new HashMap<>();
        try {
            JSONObject jsonObject = new JSONObject(text);
            //循环转换
            for (Iterator<String> it = jsonObject.keys(); it.hasNext(); ) {
                String key = it.next();
                result.put(key, (Boolean)jsonObject.getBoolean(key));
            }
        } catch (JSONException e) {
            Log.e("getWhiteList()", "failed to create JSONObject");
        }
        return result;
    }
}
