package com.java.recomapp.decisiontree;
import com.java.recomapp.MainActivity;
import com.java.recomapp.SideBarService;
import com.java.recomapp.utils.FileUtils;
import com.java.recomapp.utils.datacollect.DeviceManager;
import com.java.recomapp.utils.datacollect.MotionManager;
import com.java.recomapp.utils.datacollect.NoiseManager;
import com.java.recomapp.utils.datacollect.PositionManager;
import android.content.Context;
import android.provider.ContactsContract;
import android.util.Log;

import androidx.annotation.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Stack;
import java.util.Vector;
import com.java.recomapp.decisiontree.Features;
import java.util.concurrent.ScheduledExecutorService;

class TreeNode{
    public Map<Object, TreeNode> branches;
    public int feature;
    public String appName;
    public TreeNode(){
        feature = Features.INVALID;
        branches = new HashMap<>();
    }
}

class UserPreference{
    Map<String, int[]> featureCount;
    public UserPreference(){
        featureCount = new HashMap<>();
    }

    public void update(Object[] x, int[] validFeatureList){
        String featureContent = makeObjectArrayToString(x);
        if(featureCount.containsKey(featureContent)){
            Objects.requireNonNull(featureCount.get(featureContent))[makeBitMapToInt(validFeatureList)] += 1;
        }else{
            int[] featureBitMap = new int[1<<6];
            featureBitMap[makeBitMapToInt(validFeatureList)] = 1;
            featureCount.put(featureContent, featureBitMap);
        }
    }

    public boolean[] getValidFeatureList(Object[] x, boolean[] validFeatureList){
        String featureContent = makeObjectArrayToString(x);
        boolean[] returnValidFeatureList = validFeatureList.clone();
        if(featureCount.containsKey(featureContent)){
            int[] featureBitMap = Objects.requireNonNull(featureCount.get(featureContent));
            int maxNumber = -1;
            int maxIndex=0;
            for(int i=0; i<featureBitMap.length; i++){
                if(featureBitMap[i]>maxNumber){
                    maxNumber = featureBitMap[i];
                    maxIndex = i;
                }
            }
            boolean[] bitmap = makeIntToBitMap(maxIndex);
            for(int i=0; i<bitmap.length; i++){
                returnValidFeatureList[i] = returnValidFeatureList[i] && bitmap[i];
            }
        }
        return returnValidFeatureList;
    }

    int makeBitMapToInt(int[] validFeatureList){
        int result = 0;
        for(int featureIndex:validFeatureList){
            result += (1<<featureIndex);
        }
        return result;
    }

    boolean[] makeIntToBitMap(int bit){
        boolean[] bitmap = new boolean[6];
        for(int i=0; i<bitmap.length; i++){
            bitmap[i] = ((bit & (1 << i)) != 0);
        }
        return bitmap;
    }

    String makeObjectArrayToString(Object[] x){
        return Arrays.toString(x);
    }
}

class AppNameReturn{
    public List<String> appNameList;
    public int maxReturnNumber;
    public AppNameReturn(int size){
        appNameList = new ArrayList<>();
        maxReturnNumber = size;
    }
    public void add(String appName){
        int index = appNameList.indexOf(appName);
        if(index == -1){
            appNameList.add(appName);
        } else {
            return;
        }
        if(appNameList.size() > maxReturnNumber){
            appNameList.remove(maxReturnNumber);
        }
    }
}

class Dataset{
    public List<Object[]> xData;
    public List<String> yData;
    public int maxDatasetSize;
    public Map<String, Integer> appCount;
    public Map<String, Integer> positionCount;
    public Dataset(int datasetSize){
        xData = new ArrayList<>();
        yData = new ArrayList<>();
        appCount = new HashMap<>();
        maxDatasetSize = datasetSize;
        positionCount = new HashMap<>();
    }

    public void update(Object[] x, String y){
        String newPosition = (String) x[Features.POSITION];
        xData.add(x);
        yData.add(y);
        appCount.put(y, appCount.getOrDefault(y, 0)+1);
        positionCount.put(newPosition, positionCount.getOrDefault(newPosition, 0) + 1);
        if(yData.size() > maxDatasetSize){
            this.removeByIndex(0);
        }
    }

    public void removeByIndex(int index){
        if(index >= xData.size()){
            return ;
        }
        String dropAppName = yData.get(index);
        int dropAppCount = appCount.getOrDefault(dropAppName, 0);
        String dropPosition = (String) xData.get(index)[Features.POSITION];
        int dropPositionCount = positionCount.getOrDefault(dropPosition, 0);
        if(dropAppCount <= 1){
            appCount.remove(dropAppName);
        }else{
            appCount.put(dropAppName, dropAppCount-1);
        }
        if(dropPositionCount <= 1){
            positionCount.remove(dropPosition);
        }else{
            positionCount.put(dropPosition, dropPositionCount - 1);
        }
        xData.remove(index);
        yData.remove(index);
    }

    public static Dataset loadDataset(String fileName){
        File datasetFile = new File(fileName);
        if(!datasetFile.canRead())
            return null;
        String datasetContent = FileUtils.getFileContent(fileName);

        return loadContent(datasetContent);
    }

    public static Dataset loadContent(String datasetContent){
        String[] lines = datasetContent.split(";");
        int datasetSize = Integer.parseInt(lines[0]);
        Dataset d = new Dataset(datasetSize);
        for(int i=1; i<lines.length; i++){
            String line = lines[i];
            String[] entryList = line.split(",");
            Object[] x = new Object[6];
            String y;
            x[Features.TIME] = Integer.parseInt(entryList[Features.TIME]);
            x[Features.APP] = entryList[Features.APP];
            x[Features.DEVICE] = Integer.parseInt(entryList[Features.DEVICE]);
            x[Features.NOISE] = Integer.parseInt(entryList[Features.NOISE]);
            x[Features.POSITION] = entryList[Features.POSITION];
            x[Features.STEP] = Integer.parseInt(entryList[Features.STEP]);
            y = entryList[6];
            d.update(x, y);
        }
        return d;
    }

    public static Dataset getDefaultDataset(){
        String content = "10000;5,,1024,0,0_0_0@0,0,com.ss.android.tuchong;5,com.ss.android.tuchong,1024,0,0_0_0@0,0,com.dongqiudi.news;5,com.dongqiudi.news,1024,0,0_0_0@0,0,com.sankuai.meituan;5,com.sankuai.meituan,1024,0,0_0_0@0,0,com.jingdong.app.mall;5,com.jingdong.app.mall,1024,0,0_0_0@0,0,com.sankuai.meituan;5,com.sankuai.meituan,1024,0,0_0_0@0,0,com.lbe.security.miui;5,com.lbe.security.miui,1024,0,0_0_0@0,0,com.miui.securitycenter;5,com.miui.securitycenter,1024,0,0_0_0@0,0,com.github.kr328.clash.foss;5,com.github.kr328.clash.foss,1024,1,0_0_0@0,0,com.tencent.mm;5,com.tencent.mm,1024,0,0_0_0@0,0,com.miui.securitycenter;5,com.miui.securitycenter,1024,0,0_0_0@0,0,com.tencent.mm;5,com.tencent.mm,1024,0,0_0_0@0,0,com.zhihu.android;0,com.zhihu.android,1024,0,0_0_0@0,0,com.tencent.mm;0,com.tencent.mm,1024,0,0_0_0@0,0,com.xingin.xhs;0,com.xingin.xhs,1024,0,0_0_0@0,0,com.tencent.mm;0,com.tencent.mm,1024,0,0_0_0@0,0,com.jingdong.app.mall;0,com.jingdong.app.mall,1024,0,0_0_0@0,0,com.tencent.mm;0,com.tencent.mm,1024,0,0_0_0@0,0,com.jingdong.app.mall;0,com.jingdong.app.mall,1024,0,0_0_0@0,0,com.tencent.mm;0,com.tencent.mm,1024,0,0_0_0@0,0,com.baidu.tieba;0,com.baidu.tieba,1024,0,0_0_0@0,0,com.tencent.mm;0,com.tencent.mm,1024,0,0_0_0@0,0,com.miui.securitycenter;0,com.miui.securitycenter,1024,0,0_0_0@0,0,com.tencent.mm;0,com.tencent.mm,1024,0,0_0_0@0,0,com.dongqiudi.news;0,com.dongqiudi.news,1024,0,0_0_0@0,0,com.tencent.mm;0,com.tencent.mm,1024,0,0_0_0@0,0,com.dongqiudi.news;0,com.dongqiudi.news,1024,0,0_0_0@0,0,com.tencent.mm;0,com.tencent.mm,1024,0,0_0_0@0,0,com.dongqiudi.news;0,com.dongqiudi.news,1024,0,0_0_0@0,0,com.tencent.mm;0,com.tencent.mm,1024,0,0_0_0@0,0,com.miui.screenshot;0,com.miui.screenshot,1024,0,0_0_0@0,0,com.tencent.mm;0,com.tencent.mm,1024,0,0_0_0@0,0,chuxin.shimo.shimowendang;0,chuxin.shimo.shimowendang,1024,0,0_0_0@0,0,com.miui.screenshot;0,com.miui.screenshot,1024,0,0_0_0@0,0,com.tencent.mm;0,com.tencent.mm,1024,0,0_0_0@0,0,com.quark.browser;0,com.quark.browser,1024,0,0_0_0@0,0,com.tencent.mm;0,com.tencent.mm,1024,0,0_0_0@0,0,com.quark.browser;0,com.quark.browser,1024,0,0_0_0@0,0,com.tencent.mm;0,com.tencent.mm,1024,0,0_0_0@0,0,com.quark.browser;0,com.quark.browser,1024,0,0_0_0@0,0,com.tencent.mm;0,com.tencent.mm,1024,0,0_0_0@0,0,com.quark.browser;0,com.quark.browser,1024,0,0_0_0@0,0,com.tencent.mm;0,com.tencent.mm,1024,0,0_0_0@0,0,com.quark.browser;0,com.quark.browser,1024,0,0_0_0@0,0,com.github.kr328.clash.foss;0,com.github.kr328.clash.foss,1024,0,0_0_0@0,0,com.tencent.mm;0,com.tencent.mm,1024,0,0_0_0@0,0,com.quark.browser;0,com.quark.browser,1024,0,0_0_0@0,0,com.tencent.mm;0,com.tencent.mm,1024,0,0_0_0@0,0,com.quark.browser;0,com.quark.browser,1024,0,0_0_0@0,0,com.tencent.mm;0,com.tencent.mm,1024,0,0_0_0@0,0,com.quark.browser;0,com.quark.browser,1024,0,0_0_0@0,0,com.tencent.mm;0,com.tencent.mm,1024,0,0_0_0@0,0,com.quark.browser;0,com.quark.browser,1024,0,0_0_0@0,0,com.github.kr328.clash.foss;0,com.github.kr328.clash.foss,1024,0,0_0_0@0,0,com.tencent.mm;0,com.tencent.mm,1024,0,0_0_0@0,0,com.dongqiudi.news;0,com.dongqiudi.news,1024,0,0_0_0@0,0,com.tencent.mm;0,com.tencent.mm,1024,0,0_0_0@0,0,com.dongqiudi.news;0,com.dongqiudi.news,1024,0,0_0_0@0,0,com.tencent.mm;0,com.tencent.mm,1024,0,0_0_0@0,0,com.dongqiudi.news;0,com.dongqiudi.news,1024,0,0_0_0@0,0,com.baidu.tieba;0,com.baidu.tieba,1024,0,0_0_0@0,0,com.tencent.mm;0,com.tencent.mm,1024,0,0_0_0@0,0,com.dongqiudi.news;0,com.dongqiudi.news,1024,0,0_0_0@0,0,com.tencent.mm;0,com.tencent.mm,1024,0,0_0_0@0,0,com.dongqiudi.news;0,com.dongqiudi.news,1024,0,0_0_0@0,0,com.tencent.mm;0,com.tencent.mm,1024,0,0_0_0@0,0,com.dongqiudi.news;0,com.dongqiudi.news,1024,0,0_0_0@0,0,com.futbin;0,com.futbin,1024,0,0_0_0@0,0,com.tencent.mm;2,com.tencent.mm,1024,0,0_0_0@0,0,com.baidu.tieba;3,com.baidu.tieba,1024,0,0_0_0@0,0,com.jingdong.app.mall;3,com.jingdong.app.mall,1024,0,0_0_0@0,0,com.tencent.mm;3,com.tencent.mm,1024,0,0_0_0@0,0,com.quark.browser;3,com.quark.browser,1024,0,0_0_0@0,0,com.tencent.mm;3,com.tencent.mm,1024,0,0_0_0@0,0,com.baidu.tieba;3,com.baidu.tieba,1024,0,0_0_0@0,0,com.jingdong.app.mall;3,com.jingdong.app.mall,1024,0,0_0_0@0,0,com.tencent.mm;3,com.tencent.mm,1024,0,0_0_0@0,0,com.baidu.tieba;3,com.baidu.tieba,1024,0,0_0_0@0,0,com.tencent.mm;3,com.tencent.mm,1024,0,0_0_0@0,0,com.baidu.tieba;3,com.baidu.tieba,1024,0,0_0_0@0,0,com.tencent.mm;3,com.tencent.mm,1024,0,0_0_0@0,0,com.baidu.tieba;3,com.baidu.tieba,1024,0,0_0_0@0,0,com.tencent.mm;3,com.tencent.mm,1024,0,0_0_0@0,0,com.jingdong.app.mall;3,com.jingdong.app.mall,1024,0,0_0_0@0,0,com.tencent.mm;3,com.tencent.mm,1024,0,0_0_0@0,0,com.android.contacts;3,com.android.contacts,1024,0,0_0_0@0,0,com.jingdong.app.mall;3,com.jingdong.app.mall,1024,0,0_0_0@0,0,com.miui.gallery;3,com.miui.gallery,1024,1,0_0_0@0,0,com.quark.browser;3,com.quark.browser,1024,0,0_0_0@0,0,com.tencent.mm;3,com.tencent.mm,1024,0,0_0_0@0,0,com.sina.weibo;3,com.sina.weibo,1024,0,0_0_0@0,0,com.baidu.tieba;3,com.baidu.tieba,1024,0,0_0_0@0,0,com.tencent.mm;3,com.tencent.mm,1024,0,0_0_0@0,0,com.quark.browser;3,com.quark.browser,1024,0,0_0_0@0,0,com.github.kr328.clash.foss;3,com.github.kr328.clash.foss,1024,0,0_0_0@0,0,com.quark.browser;3,com.quark.browser,1024,0,0_0_0@0,0,com.microsoft.emmx;3,com.microsoft.emmx,1024,0,0_0_0@0,0,com.quark.browser;3,com.quark.browser,1024,0,0_0_0@0,0,com.tencent.mm;3,com.tencent.mm,1024,0,0_0_0@0,0,com.jingdong.app.mall;4,com.jingdong.app.mall,1024,0,0_0_0@0,0,com.tencent.mm;4,com.tencent.mm,1024,0,0_0_0@0,0,com.jingdong.app.mall;4,com.jingdong.app.mall,1024,0,0_0_0@0,0,com.tencent.mm;4,com.tencent.mm,1024,0,0_0_0@0,0,com.jingdong.app.mall;4,com.jingdong.app.mall,1024,0,0_0_0@0,0,com.tencent.mm;4,com.tencent.mm,1024,0,0_0_0@0,0,com.baidu.tieba;4,com.baidu.tieba,1024,0,0_0_0@0,0,com.tencent.mm;4,com.tencent.mm,1024,0,0_0_0@0,0,com.baidu.tieba;4,com.baidu.tieba,1024,0,0_0_0@0,0,com.tencent.mm;4,com.tencent.mm,1024,1,0_0_0@0,0,com.baidu.tieba;4,com.baidu.tieba,1024,1,0_0_0@0,0,com.tencent.mm;4,com.tencent.mm,1024,0,0_0_0@0,0,com.github.kr328.clash.foss;4,com.github.kr328.clash.foss,1024,0,0_0_0@0,0,com.tencent.mm;4,com.tencent.mm,1024,1,0_0_0@0,0,com.jingdong.app.mall;4,com.jingdong.app.mall,1024,1,0_0_0@0,0,com.douban.frodo;4,com.douban.frodo,1024,0,0_0_0@0,0,com.tencent.mm;4,com.tencent.mm,1024,0,0_0_0@0,0,com.miui.screenshot;4,com.miui.screenshot,1024,0,0_0_0@0,0,com.tencent.mm;4,com.tencent.mm,1024,0,0_0_0@0,0,com.baidu.tieba;4,com.baidu.tieba,1024,0,0_0_0@0,0,com.tencent.mm;4,com.tencent.mm,1024,0,0_0_0@0,0,com.quark.browser;4,com.quark.browser,1024,0,0_0_0@0,0,com.taobao.taobao;4,com.taobao.taobao,1024,0,0_0_0@0,0,com.jingdong.app.mall;4,com.jingdong.app.mall,1024,0,0_0_0@0,0,com.taobao.idlefish;4,com.taobao.idlefish,1024,0,0_0_0@0,0,com.tencent.mm;4,com.tencent.mm,1024,0,0_0_0@0,0,com.jingdong.app.mall;4,com.jingdong.app.mall,1024,0,0_0_0@0,0,com.tencent.mm;4,com.tencent.mm,1024,0,0_0_0@0,0,com.dongqiudi.news;4,com.dongqiudi.news,1024,0,0_0_0@0,0,com.tencent.mm;5,com.tencent.mm,1024,0,0_0_0@0,0,com.android.fileexplorer;5,com.android.fileexplorer,1024,0,0_0_0@0,0,cn.wps.moffice_eng;5,cn.wps.moffice_eng,1024,0,0_0_0@0,0,com.android.fileexplorer;5,com.android.fileexplorer,1024,0,0_0_0@0,0,cn.wps.moffice_eng;5,cn.wps.moffice_eng,1024,0,0_0_0@0,0,com.android.fileexplorer;5,com.android.fileexplorer,1024,1,0_0_0@0,0,com.tencent.mm;5,com.tencent.mm,1024,0,0_0_0@0,0,com.jingdong.app.mall;5,com.jingdong.app.mall,1024,0,0_0_0@0,0,com.tencent.mm;5,com.tencent.mm,1024,0,0_0_0@0,0,com.android.fileexplorer;5,com.android.fileexplorer,1024,0,0_0_0@0,0,com.tencent.mm;0,com.tencent.mm,1024,0,0_0_0@0,0,com.jingdong.app.mall;0,com.jingdong.app.mall,1024,0,0_0_0@0,0,com.android.deskclock;1,com.android.deskclock,1024,2,0_0_0@0,0,com.tencent.mm;1,com.tencent.mm,1024,0,0_0_0@0,0,com.jingdong.app.mall;2,com.jingdong.app.mall,1024,0,0_0_0@0,0,com.android.camera;2,com.android.camera,1024,0,0_0_0@0,0,com.tencent.mm;2,com.tencent.mm,1024,0,0_0_0@0,0,com.tencent.wemeet.app;2,com.tencent.wemeet.app,1024,0,0_0_0@0,0,com.miui.securityinputmethod;2,com.miui.securityinputmethod,1024,0,0_0_0@0,0,com.tencent.wemeet.app;2,com.tencent.wemeet.app,1024,0,0_0_0@0,0,com.android.camera;2,com.android.camera,1024,0,0_0_0@0,0,com.tencent.wemeet.app;2,com.tencent.wemeet.app,1024,0,0_0_0@0,0,com.xiaomi.scanner;2,com.xiaomi.scanner,1024,0,0_0_0@0,0,com.android.camera;2,com.android.camera,1024,0,0_0_0@0,0,com.tencent.mm;2,com.tencent.mm,1024,0,0_0_0@0,0,com.douban.frodo;2,com.douban.frodo,1024,0,0_0_0@0,0,com.zhihu.android;2,com.zhihu.android,1024,0,0_0_0@0,0,com.douban.frodo;2,com.douban.frodo,1024,0,0_0_0@0,0,com.tencent.mm;2,com.tencent.mm,1024,0,0_0_0@0,0,com.jingdong.app.mall;2,com.jingdong.app.mall,1024,0,0_0_0@0,0,com.miui.screenshot;2,com.miui.screenshot,1024,0,0_0_0@0,0,com.jingdong.app.mall;2,com.jingdong.app.mall,1024,0,0_0_0@0,0,com.miui.screenshot;2,com.miui.screenshot,1024,0,0_0_0@0,0,com.jingdong.app.mall;2,com.jingdong.app.mall,1024,0,0_0_0@0,0,com.xiaomi.scanner;2,com.xiaomi.scanner,1024,0,0_0_0@0,0,com.tencent.mm;3,com.tencent.mm,1024,0,0_0_0@0,0,com.jingdong.app.mall;3,com.jingdong.app.mall,1024,1,0_0_0@0,0,com.baidu.tieba;3,com.baidu.tieba,1024,0,0_0_0@0,0,com.jingdong.app.mall;3,com.jingdong.app.mall,1024,0,0_0_0@0,0,com.baidu.tieba;3,com.baidu.tieba,1024,0,0_0_0@0,0,com.jingdong.app.mall;3,com.jingdong.app.mall,1024,0,0_0_0@0,0,com.baidu.tieba;3,com.baidu.tieba,1024,0,0_0_0@0,0,com.jingdong.app.mall;3,com.jingdong.app.mall,1024,0,0_0_0@0,0,com.baidu.tieba;3,com.baidu.tieba,1024,0,0_0_0@0,0,com.dongqiudi.news;3,com.dongqiudi.news,1024,0,0_0_0@0,0,com.baidu.tieba;3,com.baidu.tieba,1024,0,0_0_0@0,0,com.dongqiudi.news;3,com.dongqiudi.news,1024,0,0_0_0@0,0,com.tencent.mm;3,com.tencent.mm,1024,0,0_0_0@0,0,com.jingdong.app.mall;3,com.jingdong.app.mall,1024,0,0_0_0@0,0,com.android.deskclock;3,com.android.deskclock,1024,0,0_0_0@0,0,com.jingdong.app.mall;3,com.jingdong.app.mall,1024,0,0_0_0@0,0,com.tencent.mm;3,com.tencent.mm,1024,0,0_0_0@0,0,com.baidu.tieba;3,com.baidu.tieba,1024,0,0_0_0@0,0,com.jingdong.app.mall;4,com.jingdong.app.mall,1024,0,0_0_0@0,0,com.tencent.mm;4,com.tencent.mm,1024,0,0_0_0@0,0,com.baidu.tieba;4,com.baidu.tieba,1024,0,0_0_0@0,0,com.jingdong.app.mall;4,com.jingdong.app.mall,1024,0,0_0_0@0,0,com.baidu.tieba;4,com.baidu.tieba,1024,0,0_0_0@0,0,com.tencent.mm;4,com.tencent.mm,1024,0,0_0_0@0,0,com.baidu.tieba;4,com.baidu.tieba,1024,0,0_0_0@0,0,com.tencent.mm;4,com.tencent.mm,1024,0,0_0_0@0,0,com.baidu.tieba;4,com.baidu.tieba,1024,0,0_0_0@0,0,com.tencent.mm;4,com.tencent.mm,1024,0,0_0_0@0,0,com.jingdong.app.mall;4,com.jingdong.app.mall,1024,0,0_0_0@0,0,tv.danmaku.bili;4,tv.danmaku.bili,1024,0,0_0_0@0,0,com.miui.securitycenter;4,com.miui.securitycenter,1024,0,0_0_0@0,0,tv.danmaku.bili;4,tv.danmaku.bili,1024,0,0_0_0@0,0,com.miui.securitycenter;4,com.miui.securitycenter,1024,0,0_0_0@0,0,tv.danmaku.bili;4,tv.danmaku.bili,1024,0,0_0_0@0,0,com.miui.securitycenter;4,com.miui.securitycenter,1024,0,0_0_0@0,0,tv.danmaku.bili;4,tv.danmaku.bili,1024,0,0_0_0@0,0,com.miui.securitycenter;4,com.miui.securitycenter,1024,0,0_0_0@0,0,tv.danmaku.bili;4,tv.danmaku.bili,1024,0,0_0_0@0,0,com.tencent.mm;4,com.tencent.mm,1024,0,0_0_0@0,0,com.jingdong.app.mall;4,com.jingdong.app.mall,1024,0,0_0_0@0,0,com.tencent.mm;4,com.tencent.mm,1024,0,0_0_0@0,0,com.baidu.tieba;4,com.baidu.tieba,1024,0,0_0_0@0,0,com.tencent.mm;4,com.tencent.mm,1024,0,0_0_0@0,0,com.android.fileexplorer;4,com.android.fileexplorer,1024,0,0_0_0@0,0,com.tencent.mm;4,com.tencent.mm,1024,0,0_0_0@0,0,com.miui.securitycenter;4,com.miui.securitycenter,1024,0,0_0_0@0,0,com.taobao.taobao;4,com.taobao.taobao,1024,0,0_0_0@0,0,com.android.fileexplorer;4,com.android.fileexplorer,1024,0,0_0_0@0,0,com.taobao.taobao;4,com.taobao.taobao,1024,0,0_0_0@0,0,com.tencent.mm;4,com.tencent.mm,1024,0,0_0_0@0,0,com.jingdong.app.mall;4,com.jingdong.app.mall,1024,0,0_0_0@0,0,com.android.deskclock;4,com.android.deskclock,1024,0,0_0_0@0,0,com.tencent.mm;4,com.tencent.mm,1024,0,0_0_0@0,0,com.android.deskclock;4,com.android.deskclock,1024,0,0_0_0@0,0,com.tencent.mm;4,com.tencent.mm,1024,0,0_0_0@0,0,com.android.fileexplorer;4,com.android.fileexplorer,1024,0,0_0_0@0,0,com.tencent.mm;4,com.tencent.mm,1024,0,0_0_0@0,0,com.android.fileexplorer;4,com.android.fileexplorer,1024,0,0_0_0@0,0,com.tencent.mm;";
        return loadContent(content);
    }

    public void saveDataset(String fileName){
        StringBuilder saveContent = new StringBuilder();
        saveContent.append(this.maxDatasetSize);
        saveContent.append(";");
        Iterator<Object[]> xIterator = xData.iterator();
        Iterator<String> yIterator = yData.iterator();
        while(xIterator.hasNext() && yIterator.hasNext()){
            Object[] x = xIterator.next();
            String y = yIterator.next();
            for(int i=0; i<6; i++) {
                saveContent.append(x[i].toString());
                saveContent.append(",");
            }
            saveContent.append(y);
            saveContent.append(";");
        }
        Log.i("savaData", "saveDataset: 1");
        FileUtils.writeStringToFile(saveContent.toString(), new File(fileName));
    }

    public void setMaxDatasetSize(int datasetSize){
        int removeNumber = this.xData.size() - datasetSize;
        for(int i=datasetSize; i<removeNumber; i++){
            this.removeByIndex(0);
        }
        this.maxDatasetSize = datasetSize;
    }

    public Dataset getSubDataset(List<String> accessAppNameList){
        Dataset d = new Dataset(this.maxDatasetSize);
        Iterator<Object[]> xIterator = xData.iterator();
        Iterator<String> yIterator = yData.iterator();
        while (xIterator.hasNext() && yIterator.hasNext()){
            Object[] x = xIterator.next();
            String y = yIterator.next();
            if(accessAppNameList.contains(y)){
                d.update(x, y);
            }
        }
        return d;
    }
}

public class Decision {
    TreeNode root;
    int algorithm = Algorithm.CART;
    // ????????????????????????feature
    List<String> accessAppNameList;
    Object[] last_state;
    Dataset dataset;
    UserPreference userPreference;
    boolean[] validFeatureList = {true, true, true, true, true, true};
    int returnAppCount = 5;
    int trainStep = 1;
    int preferenceWeight = 10;
    int datasetSize = 10000;
    int updateCount = 0;
    DeviceManager deviceManager;
    MotionManager motionManager;
    NoiseManager noiseManager;
    PositionManager positionManager;
    static final String datasetFilePath = MainActivity.FILE_FOLDER + "dataset.txt";
    static final String modelConfigFilePath = MainActivity.FILE_FOLDER + "modelconfig.txt";
    /*
        ??????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
        wzgg???????????????????????????????????????????????????
     */
    public Decision(Context context, ScheduledExecutorService executorService){
        this.deviceManager = new DeviceManager(context, executorService);
        this.motionManager = new MotionManager(context, executorService);
        this.noiseManager = new NoiseManager(context, executorService);
        this.positionManager = new PositionManager(context, executorService);
        this.dataset = Dataset.loadDataset(datasetFilePath);
        this.userPreference = new UserPreference();
        this.root = new TreeNode();
        if(this.dataset == null){
            this.dataset = Dataset.getDefaultDataset();
        }else{
            this.loadModel();
        }
    }

    /*
        ???????????????????????????????????????????????????datasetSize???app???????????????
     */
    public void setDatasetSize(int datasetSize){
        this.datasetSize = datasetSize;
        this.dataset.setMaxDatasetSize(datasetSize);
        this.genTree(this.root, this.dataset.getSubDataset(this.accessAppNameList), this.validFeatureList);
    }

    /*
        ????????????????????????????????????????????????????????????????????????update????????????????????????????????????
     */
    public void setTrainStep(int trainStep){
        this.trainStep = trainStep;
    }

    /*
        ????????????????????????????????????app?????????
     */
    public void setReturnAppCount(int returnAppCount) {
        this.returnAppCount = returnAppCount;
    }

    /*
        ??????????????????????????????????????????????????????Algorithm??????
     */
    public void setAlgorithm(int algorithm){
        this.algorithm = algorithm;
        this.genTree(this.root, this.dataset.getSubDataset(this.accessAppNameList), this.validFeatureList);
    }

    /*
    ???????????????????????????????????????????????????
    ????????????bool??????????????????????????????????????? Features ???
 */
    public void setValidFeatureList(boolean[] valid){
        this.validFeatureList = valid;
        this.genTree(this.root, this.dataset.getSubDataset(this.accessAppNameList), this.validFeatureList);
        Log.d("tag","setValidFeatureList");
    }

    /*
            ????????????????????????????????????????????????????????????app?????????
            ???????????????List<String>??????????????????????????????????????????returnAppCount
            ??????????????????????????????????????????????????????????????????????????????app??????
         */
    public List<String> predict() {
        last_state = getX();
        return predict(last_state);
    }

    /*
        ????????????????????????????????????????????????
        topIndex: ???????????????app?????????????????????topIndex????????????????????????(???0????????????)
     */
    public double score(int topIndex){
        int count = 0;
        Iterator<Object[]> xDataIterator = this.dataset.xData.iterator();
        Iterator<String> yDataIterator = this.dataset.yData.iterator();
        while(xDataIterator.hasNext() && yDataIterator.hasNext()){
            String realAppName = yDataIterator.next();
            Object[] xData = xDataIterator.next();
            List<String> predictAppNameList = predict(xData);
            if(this.isProper(predictAppNameList, realAppName, topIndex)){
                count += 1;
            }
        }
        return (double)(count) / (double)(this.dataset.yData.size());
    }

    /*
        ????????????app?????????????????????????????????????????????????????????app
        ?????????????????????????????????appNameList?????????????????????????????????????????????
     */
    public void setAccessAppNameList(List<String> validAppNameList){
        this.accessAppNameList = validAppNameList;
        this.genTree(this.root, this.dataset.getSubDataset(this.accessAppNameList), this.validFeatureList);
    }

    /*
        ?????????????????????????????????????????????app?????????
        ?????????????????????????????????????????????
     */
    public void update(String appName){
        if(!this.accessAppNameList.contains(appName)){
            return ;
        }
        this.updateCount %= this.trainStep;
        this.updateCount += 1;
        this.dataset.update(getX(), appName);
        if(this.updateCount == this.trainStep){
            this.dataset.saveDataset(Decision.datasetFilePath);
            this.saveModel();
            this.genTree(this.root, this.dataset.getSubDataset(this.accessAppNameList), this.validFeatureList);
        }
    }
    /*
        ?????????????????????????????????????????????app?????????
        ???????????????????????????????????????????????????
     */
    public void update(String appName, int[] userPreferenceFeatureList){
        if(last_state == null){
            last_state = getX();
        }
        if(!this.accessAppNameList.contains(appName)){
            return ;
        }
        this.userPreference.update(last_state, userPreferenceFeatureList);
        for(int i=0; i<this.preferenceWeight; i++){
            this.dataset.update(last_state, appName);
        }
        this.updateCount %= this.trainStep;
        this.updateCount += 1;
        if(this.updateCount == this.trainStep){
            this.dataset.saveDataset(Decision.datasetFilePath);
            this.saveModel();
            this.genTree(this.root, this.dataset.getSubDataset(this.accessAppNameList), this.validFeatureList);
        }
    }

    /*
        ???????????????????????????????????????????????????????????????????????????????????????????????????save
     */
    public void saveModel(){
        StringBuilder saveContent = new StringBuilder();
        saveContent.append(this.datasetSize);
        saveContent.append(";");
        saveContent.append(this.returnAppCount);
        saveContent.append(";");
        saveContent.append(this.trainStep);
        saveContent.append(";");
        saveContent.append(this.algorithm);
        saveContent.append(";");
        saveContent.append(String.join(",", this.accessAppNameList));
        saveContent.append(";");
        saveContent.append(this.validFeatureList[0]);
        for(int i=1; i<6; i++){
            saveContent.append(",");
            saveContent.append(this.validFeatureList[i]);
        }
    }

    protected void loadModel(){
        File inStream = new File(modelConfigFilePath);
        if(!inStream.canRead()){
            return;
        }
        String[] content = FileUtils.getFileContent(modelConfigFilePath).split(";");
        this.datasetSize = Integer.parseInt(content[0]);
        this.returnAppCount = Integer.parseInt(content[1]);
        this.trainStep = Integer.parseInt(content[2]);
        this.algorithm = Integer.parseInt(content[3]);
        String[] appNameArray = content[4].split(",");
        this.accessAppNameList = new ArrayList<>(Arrays.asList(appNameArray));
        this.validFeatureList = new boolean[6];
        String[] validFeatureArray = content[5].split(",");
        for(int i=0; i<6; i++){
            this.validFeatureList[i] = validFeatureArray[i].equals("true");
        }
        this.genTree(this.root, this.dataset.getSubDataset(this.accessAppNameList), this.validFeatureList);
    }

    protected Object[] getX(){
        Object[] x = new Object[6];
        x[Features.TIME] = this.getTime();
        x[Features.APP] = this.getLastApp();
        x[Features.DEVICE] = this.getDevice();
        x[Features.NOISE] = this.getNoise();
        x[Features.POSITION] = this.getPosition();
        x[Features.STEP] = this.getStep();
        Log.e("get x: ", Arrays.toString(x));
        return x;
    }

    protected void genTree(TreeNode rootNode, Dataset data, boolean[] validList){
        boolean isSameAppName = this.checkSameAppName(data.appCount);
        if(isSameAppName){
            rootNode.appName = data.yData.get(0);
            return ;
        }
        rootNode.appName = this.getMajorityAppName(data.appCount);
        int bestFeature = this.decideBestFeature(data, validList);
        if(bestFeature != -1){
            Set<Object> values = this.getValuesOfFeature(data, bestFeature);
            boolean[] newValidList = new boolean[6];
            System.arraycopy(validList, 0, newValidList, 0, 6);
            newValidList[bestFeature] = false;
            rootNode.feature = bestFeature;
            for (Object value:values){
                Dataset newData = this.getFeatureValueDataset(data, bestFeature, value);
                TreeNode subNode = new TreeNode();
                rootNode.branches.put(value, subNode);
                this.genTree(subNode, newData, newValidList);
            }
        }
    }

    protected boolean checkSameAppName(Map<String, Integer> appCount){
        return appCount.size() == 1;
    }

    protected String getMajorityAppName(Map<String, Integer> appCount){
        int maxCount = 0;
        String maxAppName = "";
        for (Map.Entry<String, Integer> entry : appCount.entrySet()) {
            Integer value = entry.getValue();
            if(value > maxCount){
                maxAppName = entry.getKey();
                maxCount = value;
            }
        }
        return maxAppName;
    }

    protected int decideBestFeature(Dataset data, boolean[] nodeValidList){
        double maxGain = -1.1;
        int bestFeature = -1;
        for(int i=0; i<6; i++){
            boolean valid = nodeValidList[i];
            if(!valid){
                continue;
            }
            double currentGain;
            if (this.algorithm == Algorithm.ID3){
                currentGain = gainID3(data, i);
            } else if(this.algorithm == Algorithm.C4_5){
                currentGain = this.gainC4_5(data, i);
            } else {
                currentGain = this.gainCART(data, i);
            }
            if(currentGain > maxGain){
                maxGain = currentGain;
                bestFeature = i;
            }
        }
        return bestFeature;
    }

    protected Set<Object> getValuesOfFeature(Dataset data, int feature){
        // ????????????feature???0~5?????????
        Set<Object> values = new HashSet<>();
        for(Object[] x: data.xData){
            values.add(x[feature]);
        }
        return values;
    }

    protected Dataset getFeatureValueDataset(Dataset data, int feature, Object value){
        Dataset newData = new Dataset(data.maxDatasetSize);
        Iterator<Object[]> xIterator = data.xData.iterator();
        Iterator<String> yIterator = data.yData.iterator();
        while(xIterator.hasNext() && yIterator.hasNext()){
            Object[] x = xIterator.next();
            String y = yIterator.next();
            if(value.equals(x[feature])){
                newData.update(x, y);
            }
        }
        return newData;
    }

    protected double infoD(Dataset data){
        double sum = 0;
        for(Map.Entry<String, Integer> entry:data.appCount.entrySet()){
            double prob = (double)(entry.getValue()) / (double)(data.yData.size());
            sum -= prob *  Math.log(prob) / Math.log(2);
        }
        return sum;
    }

    protected double infoA(Dataset data, int feature){
        double sum = 0;
        Set<Object> values = this.getValuesOfFeature(data, feature);
        for(Object value: values){
            Dataset newData = this.getFeatureValueDataset(data, feature, value);
            double prob = (double)(newData.yData.size()) / (double)(data.yData.size());
            sum += prob * this.infoD(newData);
        }
        return sum;
    }

    protected double gainID3(Dataset data, int feature){
        return this.infoD(data) - this.infoA(data, feature);
    }

    protected double splitInfo(Dataset data, int feature){
        double sum = 0;
        Set<Object> values = this.getValuesOfFeature(data, feature);
        for(Object value:values){
            Dataset newData = this.getFeatureValueDataset(data, feature, value);
            double prob = (double)(newData.yData.size()) / (double)(data.yData.size());
            // ???????????????prob????????????0???????????????feature?????????value????????????value??????????????????values???
            sum -= prob * Math.log(prob) / Math.log(2);
        }
        return sum;
    }

    protected double gainC4_5(Dataset data, int feature){
        // TODO make sure splitInfo will not return 0
        return this.gainID3(data, feature) / this.splitInfo(data, feature);
    }

    protected double gini(Dataset data){
        double sum = 1.0;
        for(Map.Entry<String, Integer> entry:data.appCount.entrySet()){
            double prob = (double)(entry.getValue()) / (double)(data.yData.size());
            sum -= prob *  prob;
        }
        return sum;
    }

    protected double gainCART(Dataset data, int feature){
        double sum = 0;
        Set<Object> values = this.getValuesOfFeature(data, feature);
        for(Object value:values){
            Dataset newData = this.getFeatureValueDataset(data, feature, value);
            sum += (this.gini(newData) * newData.yData.size()) / (double)(data.yData.size());
        }
        return sum;
    }

    protected Integer getTime(){
        Calendar c = Calendar.getInstance();
        int hour = c.get(Calendar.HOUR_OF_DAY);
        return hour / 4;
    }

    protected String getLastApp(){
        if(!this.validFeatureList[Features.APP])
            return "";
        return SideBarService.last_last_packageName;
    }

    protected Integer getDevice(){
        if(!this.validFeatureList[Features.DEVICE])
            return -1;
        List<Integer> deviceList = deviceManager.getDevices();
        int result = 0;
        boolean[] deviceExist = new boolean[11];
        Vector<Integer> deviceNumberList = new Vector<>(Arrays.asList(1024, 256, 2304, 1536, 0, 768, 1280, 512, 2048, 7936, 1792));

        for(int i=0; i<11; i++){
            deviceExist[i] = false;
        }
        for(int device: deviceList){
            int index = deviceNumberList.indexOf(device);
            if(index != -1){
                deviceExist[index] = true;
            }
        }
        for(int i=0; i<11; i++){
            if(deviceExist[i]){
                result += (1<<i);
            }
        }
        return result;
    }

    protected String getPosition(){
        // ??????????????????map????????????????????????????????????
        if(!this.validFeatureList[Features.POSITION])
            return "";
        return positionManager.getPosition();
    }

    protected Integer getNoise(){
        if(!this.validFeatureList[Features.NOISE])
            return -1;
        // ?????????
        double noise = noiseManager.getNoise();
        if (noise < 40){
            return 0;
        } else if (noise < 60) {
            return 1;
        } else if (noise < 100){
            return 2;
        } else {
            return 3;
        }
    }

    protected Integer getStep(){
        if (!this.validFeatureList[Features.STEP]) {
            return -1;
        }
        return motionManager.getStepCount();
    }

    protected List<String> predict(Object[] x) {
        boolean[] genTreeFeatureList = userPreference.getValidFeatureList(x, this.validFeatureList);
        if(!Arrays.equals(genTreeFeatureList, this.validFeatureList)){
            this.genTree(this.root, this.dataset.getSubDataset(this.accessAppNameList), validFeatureList);
        }
        TreeNode currentNode = this.root;
        AppNameReturn result = new AppNameReturn(this.returnAppCount);
        Stack<TreeNode> stack = new Stack<>();
        stack.add(this.root);
        while (true){
            if (currentNode.branches.size() == 0) {
                break;
            }
            TreeNode subNode = currentNode.branches.get(x[currentNode.feature]);
            if (subNode == null){
                break;
            }else{
                stack.add(subNode);
                currentNode = subNode;
            }
        }
        while(!stack.empty() && result.appNameList.size()<this.returnAppCount){
            currentNode = stack.pop();
            if(currentNode.branches.size()!=0){
                for(Map.Entry<Object, TreeNode> entry: currentNode.branches.entrySet()){
                    stack.add(entry.getValue());
                }
            }else{
                result.add(currentNode.appName);
            }
        }
        return result.appNameList;
    }

    protected boolean isProper(List<String> predict, String real, int topIndex){
        int index = predict.indexOf(real);
        return (index != -1) && (index <= topIndex);
    }
}
