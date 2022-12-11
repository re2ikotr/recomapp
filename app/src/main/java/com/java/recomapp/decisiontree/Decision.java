package com.java.recomapp.decisiontree;
import com.java.recomapp.SideBarService;
import com.java.recomapp.utils.datacollect.DeviceManager;
import com.java.recomapp.utils.datacollect.MotionManager;
import com.java.recomapp.utils.datacollect.NoiseManager;
import com.java.recomapp.utils.datacollect.PositionManager;

import android.content.Context;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ScheduledExecutorService;

enum Algorithm{
    ID3, C4_5, CART
}

class Features{
    public static final int TIME=0;
    public static final int APP=1;
    public static final int DEVICE=2;
    public static final int NOISE=3;
    public static final int POSITION=4;
    public static final int STEP=5;
    public static final int INVALID=-1;
}

class TreeNode{
    public Map<Object, TreeNode> branches;
    public int feature;
    public String appName;
    public TreeNode(){
        feature = Features.INVALID;
        branches = new HashMap<>();
    }

    public void saveModel(String savePath){
    }

    public void loadModel(String loadPath){

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
            appNameList.remove(index);
            appNameList.add(appName);
        }
        if(appNameList.size() > maxReturnNumber){
            appNameList.remove(0);
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
            String dropAppName = yData.get(0);
            String dropPosition = (String) xData.get(0)[Features.POSITION];
            int dropPositionCount = positionCount.getOrDefault(dropPosition, 0);
            int dropAppCount = appCount.getOrDefault(dropAppName, 0);
            if(dropAppCount <= 1){
                appCount.remove(y);
            }else{
                appCount.put(dropAppName, dropAppCount-1);
            }
            if(dropPositionCount <= 1){
                positionCount.remove(dropPosition);
            }else{
                positionCount.put(dropPosition, dropPositionCount - 1);
            }

            xData.remove(0);
            yData.remove(0);
        }
    }

    public static Dataset loadDataset(String fileName){
        return new Dataset(0);
    }

    public void saveDataset(String fileName){

    }

}

public class Decision {
    TreeNode root;
    Algorithm algorithm;
    // 训练数据中可用的feature
    List<String> accessAppNameList;
    Dataset dataset;
    boolean[] validFeatureList = {true, true, true, true, true, true};
    int returnAppCount = 5;
    int trainStep = 10;
    int datasetSize = 10000;
    DeviceManager deviceManager;
    MotionManager motionManager;
    NoiseManager noiseManager;
    PositionManager positionManager;

    /*
        初始化函数，需要提供一些参数来初始化我们的服务以读取手机中的一些数据（如位置，运动数据等等）
        wzgg写的我也不太知道这两个参数什么意思，前端有问题去问wzgg
     */
    public Decision(Context context, ScheduledExecutorService executorService){
        this.deviceManager = new DeviceManager(context, executorService);
        this.motionManager = new MotionManager(context, executorService);
        this.noiseManager = new NoiseManager(context, executorService);
        this.positionManager = new PositionManager(context, executorService);
    }

    /*
        设置决策树存储的数据集的大小（存储datasetSize条app打开记录）
     */
    public void setDatasetSize(int datasetSize){
        this.datasetSize = datasetSize;
    }

    /*
        设置训练的次数间隔，即更新多少次数据（调用多少次update函数）会重新训练一次模型
     */
    public void setTrainStep(int trainStep){
        this.trainStep = trainStep;
    }

    /*
        设置预测过程中需要返回的app的数目
     */
    public void setReturnAppCount(int returnAppCount) {
        this.returnAppCount = returnAppCount;
    }

    /*
        设置使用的决策树算法（共有三种，详见Algorithm类）
     */
    public void setAlgorithm(Algorithm algorithm){
        // TODO 重新训练决策树
        this.algorithm = algorithm;
    }

    /*
    通过该函数来设置智能推荐使用的权限
    传入一个bool数组，每一项对应的权限参照 Features 类
 */
    public void setValidFeatureList(boolean[] valid){
        this.validFeatureList = valid;
    }

    /*
            使用这个接口来获取决策树对当前可能使用的app的预测
            注意返回的List<String>理论上只保证数目不超过设置的returnAppCount
            但是有可能达不到，如果达不到前端需要用频率排名靠前的app填充
         */
    public List<String> predict() {
        Object[] x = new Object[6];
        x[Features.TIME] = this.getTime();
        x[Features.APP] = this.getLastApp();
        x[Features.DEVICE] = this.getDevice();
        x[Features.NOISE] = this.getNoise();
        x[Features.POSITION] = this.getPosition();
        x[Features.STEP] = this.getStep();
        return predict(x);
    }

    /*
        获取在当前的数据集中预测的准确率
        topIndex: 实际打开的app在预测列表的前topIndex位就认为预测正确(从0开始计数)
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
        传入一个app名称列表，里面存储的是决策树可以获取的app
        应用白名单就是重新设置appNameList，将加入白名单的应用从其中剔除
     */
    public void setAccessAppNameList(List<String> validAppNameList){
        this.accessAppNameList = validAppNameList;
        /*
            1.把我的数据集更新一遍，把不再appNameList中的数据全部剔除
            2.我不会更新我的数据集，在每条数据送到训练的地方的时候去判断在不在appNameList中
         */
        saveModel();
    }

    /*
        传递给后端在validAppNameList中的当前用户打开的app的名称
        后端会调用一些接口读取当前有权限获取的数据，
     */
    public void update(String appName){

    }

    /*
        调用该接口会存储当前模型的本身的所有数据，可以过一定的时间就做一次save
     */
    public void saveModel(){

    }

    protected void loadModel(){}

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
        // 需要确保feature在0~5区间内
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
            // 确认这里的prob不可能为0，因为如果feature对应的value不存在则value也不会在集合values中
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
            sum += (double)(this.gini(newData) * newData.yData.size()) / (double)(data.yData.size());
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
        Vector<Integer> deviceNumberList = new Vector<Integer>(11);
        int[] deviceNumberArray = new int[]{1024, 256, 2304, 1536, 0, 768, 1280, 512, 2048, 7936, 1792};
        for(int i=0; i<11; i++){
            deviceNumberList.add(deviceNumberArray[i]);
        }

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
        // 存一个地点的map，对应的如果找不到就更新
        if(!this.validFeatureList[Features.POSITION])
            return "";
        return positionManager.getPosition();
    }

    protected Integer getNoise(){
        if(!this.validFeatureList[Features.NOISE])
            return -1;
        // 分档次
        double noise = noiseManager.getNoise();
        if(noise < 30){
            return 0;
        } else if (noise < 60) {
            return 1;
        } else if (noise <100){
            return 2;
        } else{
            return 3;
        }
    }

    protected Integer getStep(){
        if(!this.validFeatureList[Features.STEP]){
            return -1;
        }
        return motionManager.getStepCount();
    }

    protected List<String> predict(Object[] x){
        TreeNode currentNode = this.root;
        AppNameReturn result = new AppNameReturn(this.returnAppCount);
        result.add(this.root.appName);
        while (true){
            TreeNode subNode = currentNode.branches.get(x[currentNode.feature]);
            if (subNode == null){
                break;
            }else{
                currentNode = subNode;
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
