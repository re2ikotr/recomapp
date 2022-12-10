package com.java.recomapp.decisiontree;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

enum Algorithm{
    ID3, C4_5, CART
}

class Features{
    public static final int TIME=0;
    public static final int APP=1;
    public static final int DEVICE=2;
    public static final int NOISE=3;
    public static final int POSITION=4;
    public static final int INVALID=-1;
}

class TreeNode{
    public Map<Integer, TreeNode> branches;
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
        appNameList.add(appName);
        if(appNameList.size() > maxReturnNumber){
            appNameList.remove(0);
        }
    }
}

class Dataset{
    public List<int[]> xData;
    public List<String> yData;
    public int maxDatasetSize;
    public Map<String, Integer> appCount;
    public Dataset(int datasetSize){
        xData = new ArrayList<>();
        yData = new ArrayList<>();
        appCount = new HashMap<>();
        maxDatasetSize = datasetSize;
    }

    public void update(int[] x, String y){
        xData.add(x);
        yData.add(y);
        appCount.put(y, appCount.getOrDefault(y, 0)+1);
        if(yData.size() > maxDatasetSize){
            String dropAppName = yData.get(0);
            int result = appCount.getOrDefault(dropAppName, 0);
            if(result <= 1){
                appCount.remove(y);
            }else{
                appCount.put(dropAppName, result-1);
            }
            xData.remove(0);
            yData.remove(0);
        }
    }

}
public class Decision {
    TreeNode root;
    Algorithm algorithm;
    // 训练数据中可用的feature
    boolean[] validList;
    boolean trained;
    List<String> appNameList;
    int returnAppCount;
    int trainStep;
    Dataset data;
    public Decision(int DatasetSize, int trainStep, int returnAppCount, List<String> appNameList){
        this.root = new TreeNode();
        this.algorithm = Algorithm.CART;
        this.trained = false;
        this.validList = new boolean[]{true, true, true, true, true};
        this.data = new Dataset(DatasetSize);
        this.trainStep = trainStep;
        this.returnAppCount = returnAppCount;
        this.appNameList = appNameList;
    }

    public void genTree(TreeNode rootNode, Dataset data, boolean[] validList){
        boolean isSameAppName = this.checkSameAppName(data.appCount);
        if(isSameAppName){
            rootNode.appName = data.yData.get(0);
            return ;
        }
        rootNode.appName = this.getMajorityAppName(data.appCount);
        int bestFeature = this.decideBestFeature(validList);
        if(bestFeature != -1){
            Set<Integer> values = this.getValuesOfFeature(bestFeature);
            boolean[] newValidList = new boolean[5];
            System.arraycopy(validList, 0, newValidList, 0, 5);
            newValidList[bestFeature] = false;
            rootNode.feature = bestFeature;
            for (int value:values){
                Dataset newData = this.getFeatureValueDataset(bestFeature, value);
                TreeNode subNode = new TreeNode();
                rootNode.branches.put(value, subNode);
                this.genTree(subNode, newData, newValidList);
            }
        }
    }

    public void setValidList(boolean[] valid){
        this.validList = valid;
    }

    public void setTrained(boolean train){
        this.trained = train;
    }

    public void setAppNameList(List<String> appName){
        this.appNameList = appName;
    }

    public boolean checkSameAppName(Map<String, Integer> appCount){
        return appCount.size() == 1;
    }

    public String getMajorityAppName(Map<String, Integer> appCount){
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

    public int decideBestFeature(boolean[] nodeValidList){
        double maxGain = -1.1;
        int bestFeature = -1;
        for(int i=0; i<5; i++){
            boolean valid = nodeValidList[i];
            if(!valid){
                continue;
            }
            double currentGain;
            if (this.algorithm == Algorithm.ID3){
                currentGain = gainID3(i);
            } else if(this.algorithm == Algorithm.C4_5){
                currentGain = this.gainC4_5(i);
            } else {
                currentGain = this.gainCART(i);
            }
            if(currentGain > maxGain){
                maxGain = currentGain;
                bestFeature = i;
            }
        }
        return bestFeature;
    }

    public Set<Integer> getValuesOfFeature(int feature){
        // 需要确保feature在0~4区间内
        Set<Integer> values = new HashSet<>();
        for(int[] x: data.xData){
            values.add(x[feature]);
        }
        return values;
    }

    public Dataset getFeatureValueDataset(int feature, int value){
        Dataset newData = new Dataset(this.data.maxDatasetSize);
        Iterator<int[]> xIterator = data.xData.iterator();
        Iterator<String> yIterator = data.yData.iterator();
        while(xIterator.hasNext() && yIterator.hasNext()){
            int[] x = xIterator.next();
            String y = yIterator.next();
            if(x[feature] == value){
                newData.update(x, y);
            }
        }
        return newData;
    }

    public double infoD(Dataset data){
        double sum = 0;
        for(Map.Entry<String, Integer> entry:data.appCount.entrySet()){
            double prob = (double)(entry.getValue()) / (double)(data.yData.size());
            sum -= prob *  Math.log(prob) / Math.log(2);
        }
        return sum;
    }

    public double infoA(int feature){
        double sum = 0;
        Set<Integer> values = this.getValuesOfFeature(feature);
        for(int value: values){
            Dataset newData = this.getFeatureValueDataset(feature, value);
            double prob = (double)(newData.yData.size()) / (double)(this.data.yData.size());
            sum += prob * this.infoD(newData);
        }
        return sum;
    }

    public double gainID3(int feature){
        return this.infoD(this.data) - this.infoA(feature);
    }

    public double splitInfo(int feature){
        double sum = 0;
        Set<Integer> values = this.getValuesOfFeature(feature);
        for(int value:values){
            Dataset newData = this.getFeatureValueDataset(feature, value);
            double prob = (double)(newData.yData.size()) / (double)(this.data.yData.size());
            // 确认这里的prob不可能为0，因为如果feature对应的value不存在则value也不会在集合values中
            sum -= prob * Math.log(prob) / Math.log(2);
        }
        return sum;
    }

    public double gainC4_5(int feature){
        // TODO make sure splitInfo will not return 0
        return this.gainID3(feature) / this.splitInfo(feature);
    }

    public double gini(Dataset data){
        double sum = 1.0;
        for(Map.Entry<String, Integer> entry:data.appCount.entrySet()){
            double prob = (double)(entry.getValue()) / (double)(data.yData.size());
            sum -= prob *  prob;
        }
        return sum;
    }

    public double gainCART(int feature){
        double sum = 0;
        Set<Integer> values = this.getValuesOfFeature(feature);
        for(int value:values){
            Dataset newData = this.getFeatureValueDataset(feature, value);
            sum += (double)(this.gini(newData) * newData.yData.size()) / (double)(data.yData.size());
        }
        return sum;
    }

    public List<List<String>> predict(List<int[]> xPredict){
        // TODO make sure that there is a decisionTree
        List<List<String>> predictArray = new ArrayList<>();
        for (int[] xData: xPredict){
            TreeNode currentNode = this.root;
            AppNameReturn result = new AppNameReturn(this.returnAppCount);
            result.add(this.root.appName);
            while (true){
                TreeNode subNode = currentNode.branches.get(xData[currentNode.feature]);
                if (subNode == null){
                    break;
                }else{
                    currentNode = subNode;
                    result.add(currentNode.appName);
                }
            }
            predictArray.add(result.appNameList);
        }
        return predictArray;
    }

    public double score(Dataset data){
        int count = 0;
        List<List<String>> predictResult = this.predict(data.xData);
        Iterator<String> realIterator = data.yData.iterator();
        Iterator<List<String>> predictIterator = predictResult.iterator();
        while(realIterator.hasNext() && predictIterator.hasNext()){
            String realAppName = realIterator.next();
            List<String> predictAppNameList = predictIterator.next();
            if(this.isProper(predictAppNameList, realAppName)){
                count += 1;
            }
        }
        return (double)(count) / (double)(data.yData.size());
    }

    public boolean isProper(List<String> predict, String real){
        for(String predictAppName:predict){
            if(real.equals(real)){
                return true;
            }
        }
        return false;
    }
}
