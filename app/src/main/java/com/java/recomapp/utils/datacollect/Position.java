package com.java.recomapp.utils.datacollect;

import java.util.List;

public class Position {
    private String id;
    private double latitude;
    private double longitude;
    private List<String> wifiIds;

    public Position(String id, double latitude, double longitude, List<String> wifiIds) {
        this.id = id;
        this.latitude = latitude;
        this.longitude = longitude;
        this.wifiIds = wifiIds;
    }

    public Position(String id, double latitude, double longitude) {
        this.id = id;
        this.latitude = latitude;
        this.longitude = longitude;
        this.wifiIds = null;
    }

    public Position(String id, List<String> wifiIds) {
        this.id = id;
        this.latitude = -200;
        this.longitude = -200;
        this.wifiIds = wifiIds;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public List<String> getWifiIds() {
        return wifiIds;
    }

    public String getId() { return id; }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public void setWifiIds(List<String> wifiIds) {
        this.wifiIds = wifiIds;
    }

    public double getGpsScore(Position position) {
        double score = 0;
        double distance = getDistance(position);
        if (distance > 200)
            return -1;
        else
            score += 50 * (200 - distance) / 200;
        return score;
    }

    public double getWifiScore(Position position) {
        double score = 0;
        List<String> wifiList = position.getWifiIds();
        double count = 0;
        for (String wifiId: wifiIds) {
            if (wifiList.contains(wifiId)) {
                count += 1;
            }
        }
        score += 50 * (count / wifiIds.size());
        return score;
    }

    public double getScore(Position position) {
        double wifi_score = 0;
        double gps_score = 0;
        boolean wifi_valid = false;
        boolean gps_valid = false;
        if (!(position.getLatitude() < -90 && position.getLongitude() < -180) && !(latitude < -90 && longitude < -180) && !(position.getLatitude() == 0 && position.getLongitude() == 0) && !(latitude == 0 && longitude == 0)) {
            gps_valid = true;
            double distance = getDistance(position);
            if (distance > 200)
                return -1;
            else
                gps_score = 50 * (200 - distance) / 200;
        }
        List<String> wifiList = position.getWifiIds();
        if (wifiList != null && wifiList.size() > 0 && wifiIds != null && wifiIds.size() > 0) {
            wifi_valid = true;
            double count = 0;
            for (String wifiId : wifiIds) {
                if (wifiList.contains(wifiId)) {
                    count += 1;
                }
            }
            wifi_score = 50 * Math.sqrt(count / wifiIds.size());
        }
        double score = 0;
        if(!wifi_valid && !gps_valid) score = -1;
        if(wifi_valid && !gps_valid) score = 2 * wifi_score;
        if(!wifi_valid && gps_valid) score = 2 * gps_score;
        if(wifi_valid && gps_valid) score = wifi_score + gps_score;
        return score;
    }

    public boolean sameAs(Position position) {
        return getScore(position) > 40;
    }

    private static double rad(double d) {
        return d * Math.PI / 180.0;
    }

    public double getDistance(Position position) {
        double lat1 = rad(position.getLatitude());
        double lat2 = rad(latitude);
        double lon1 = rad(position.getLongitude());
        double lon2 = rad(longitude);

        lat1 = Math.PI / 2 - lat1;
        lat2 = Math.PI / 2 - lat2;
        if (lon1 < 0)
            lon1 = Math.PI * 2 + lon1;
        if (lon2 < 0)
            lon2 = Math.PI * 2 + lon2;

        double EARTH_RADIUS = 6378137;
        double x1 = EARTH_RADIUS * Math.cos(lon1) * Math.sin(lat1);
        double y1 = EARTH_RADIUS * Math.sin(lon1) * Math.sin(lat1);
        double z1 = EARTH_RADIUS * Math.cos(lat1);
        double x2 = EARTH_RADIUS * Math.cos(lon2) * Math.sin(lat2);
        double y2 = EARTH_RADIUS * Math.sin(lon2) * Math.sin(lat2);
        double z2 = EARTH_RADIUS * Math.cos(lat2);

        double d = Math.sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2) + (z1 - z2) * (z1 - z2));
        double theta = Math.acos((2 * EARTH_RADIUS * EARTH_RADIUS - d * d) / (2 * EARTH_RADIUS * EARTH_RADIUS));
        return theta * EARTH_RADIUS;
    }
}
