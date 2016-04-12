package com.graphhopper.jsprit.core.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.graphhopper.jsprit.core.problem.Location;
import com.graphhopper.jsprit.core.problem.cost.VehicleRoutingActivityCosts;
import com.graphhopper.jsprit.core.problem.cost.VehicleRoutingTransportCosts;
import com.graphhopper.jsprit.core.problem.driver.Driver;
import com.graphhopper.jsprit.core.problem.vehicle.Vehicle;
import com.graphhopper.jsprit.core.util.Coordinate;
import com.graphhopper.jsprit.core.util.EuclideanCosts;
import com.graphhopper.jsprit.core.util.ManhattanCosts;
import com.sun.tools.javac.util.Pair;
import com.sun.tools.javah.Util;
import com.sun.tools.jdi.DoubleTypeImpl;


import java.io.*;
import java.net.*;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


public class BaiduDistance implements VehicleRoutingTransportCosts {

    public BaiduDistance() {
        // init singleton if necessary
        if (distanceCache == null) {
            distanceCache = new HashMap<Pair<String, String>, Double>();
            System.out.println("BaiduDistance.BaiduDistance init distance cache");
        }

        if (durationCache == null) {
            durationCache = new HashMap<Pair<String, String>, Double>();
            System.out.println("BaiduDistance.BaiduDistance init duration cache");
        }
    }

    public Map<Pair<String, String>, Double> distanceCache;
    public Map<Pair<String, String>, Double> durationCache;

    public String getCoorString(Location loc) {

        double lng = loc.getCoordinate().getX();
        double lat = loc.getCoordinate().getY();


        String strLng = new DecimalFormat("###.###").format(lng);
        String strLat = new DecimalFormat("###.###").format(lat);

        return strLng + ',' + strLat;
    }

    public void putDistanceCache(Location from, Location to, Double distance) {
        String strFrom = getCoorString(from);
        String strTo = getCoorString(to);

        Pair<String, String> p1 = new Pair<String, String>(strFrom, strTo);
        Pair<String, String> p2 = new Pair<String, String>(strTo, strFrom);

        distanceCache.put(p1, distance);
        distanceCache.put(p2, distance);
    }

    public void putDurationCache(Location from, Location to, Double duration) {
        String strFrom = getCoorString(from);
        String strTo = getCoorString(to);

        Pair<String, String> p1 = new Pair<String, String>(strFrom, strTo);
        Pair<String, String> p2 = new Pair<String, String>(strTo, strFrom);

        durationCache.put(p1, duration);
        durationCache.put(p2, duration);
    }


    public Double getDistanceCache(Location from, Location to) {
        String strFrom = getCoorString(from);
        String strTo = getCoorString(to);

        Pair<String, String> p1 = new Pair<String, String>(strFrom, strTo);
        Pair<String, String> p2 = new Pair<String, String>(strTo, strFrom);

        Double d1 = distanceCache.get(p1);
        Double d2 = distanceCache.get(p2);
        if (d1 != null) {
            return d1;
        } else if (d2 != null) {
            return d2;
        }

        return null;
    }

    public Double getDurationCache(Location from, Location to) {
        String strFrom = getCoorString(from);
        String strTo = getCoorString(to);

        Pair<String, String> p1 = new Pair<String, String>(strFrom, strTo);
        Pair<String, String> p2 = new Pair<String, String>(strTo, strFrom);

        Double d1 = durationCache.get(p1);
        Double d2 = durationCache.get(p2);
        if (d1 != null) {
            return d1;
        } else if (d2 != null) {
            return d2;
        }

        return null;
    }

    public double getTransportCost(Location from, Location to,
                                   double departureTime, Driver driver, Vehicle vehicle) {

        String apiUrl = getApiUrl(from, to);

        Double cachedD = getDistanceCache(from, to);
        if (cachedD != null) {
            return cachedD;
        }

        try {
//            JsonObject j = getHTML(apiUrl);
            JsonObject j  = postLeanCloud(from, to);

            double distance = j.getAsJsonObject("result")
                .getAsJsonObject("result")
                .getAsJsonArray("routes")
                .get(0).getAsJsonObject()
                .get("distance").getAsDouble();

            // save to cache

            putDistanceCache(from, to, distance);

            return distance;

        } catch (Exception e) {
            ManhattanCosts m = new ManhattanCosts();
            return m.getTransportCost(from, to, departureTime, driver, vehicle);
        }

    }

    public double getTransportTime(Location from, Location to,
                                   double departureTime, Driver driver, Vehicle vehicle) {
        String apiUrl = getApiUrl(from, to);

        Double cachedD = getDurationCache(from, to);
        if (cachedD != null) {
            return cachedD;
        }

        try {
//            JsonObject j = getHTML(apiUrl);
//            double duration = j.getAsJsonObject("result")
//                .getAsJsonArray("routes")
//                .get(0).getAsJsonObject()
//                .get("duration").getAsDouble();
//
            JsonObject j = postLeanCloud(from, to);
            double duration = j.getAsJsonObject("result")
                .getAsJsonObject("result")
                .getAsJsonArray("routes")
                .get(0).getAsJsonObject()
                .get("duration").getAsDouble();
            // save to cache

            putDurationCache(from, to, duration);

            return duration;

        } catch (Exception e) {
            ManhattanCosts m = new ManhattanCosts();
            return m.getTransportTime(from, to, departureTime, driver, vehicle);
        }

    }

    public double getBackwardTransportTime(Location from, Location to,
                                           double arrivalTime, Driver driver, Vehicle vehicle) {
        return getTransportTime(from, to, arrivalTime, driver, vehicle);
    }

    public double getBackwardTransportCost(Location from, Location to,
                                           double arrivalTime, Driver driver, Vehicle vehicle) {
        return getTransportCost(from, to, arrivalTime, driver, vehicle);
    }

    public String getApiUrl(Location from, Location to) {
        double fromLng = from.getCoordinate().getX();
        double fromLat = from.getCoordinate().getY();

        double toLng = to.getCoordinate().getX();
        double toLat = to.getCoordinate().getY();

        String urlTemplate = "http://api.map.baidu.com/direction/v1?mode=riding" +
            "&origin=$origin$" +
            "&destination=$destination$&" +
            "origin_region=%E5%8C%97%E4%BA%AC" +
            "&destination_region=%E5%8C%97%E4%BA%AC" +
            "&output=json" +
            "&ak=o0RuzQRVNo1YuIorz50uWVLs6DXVSo7X";

        String strOrigin = String.valueOf(fromLat) + ',' + String.valueOf(fromLng);
        String strDestination = String.valueOf(toLat) + ',' + String.valueOf(toLng);
        urlTemplate = urlTemplate.replace("$origin$", strOrigin);
        urlTemplate = urlTemplate.replace("$destination$", strDestination);

        return urlTemplate;
    }

    public static JsonObject getHTML(String urlToRead) throws Exception {
        StringBuilder result = new StringBuilder();
        URL url = new URL(urlToRead);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String line;
        while ((line = rd.readLine()) != null) {
            result.append(line);
        }
        rd.close();

        return new JsonParser().parse(result.toString()).getAsJsonObject();
    }

    public static JsonObject postLeanCloud(Location from, Location to) throws Exception {

        double fromLng = from.getCoordinate().getX();
        double fromLat = from.getCoordinate().getY();
        JsonArray fromj = new JsonArray();
        fromj.add(fromLat);
        fromj.add(fromLng);

        double toLng = to.getCoordinate().getX();
        double toLat = to.getCoordinate().getY();
        JsonArray toj = new JsonArray();
        toj.add(toLat);
        toj.add(toLng);

        JsonObject params = new JsonObject();
        params.add("from", fromj);
        params.add("to", toj);
        params.addProperty("from_region", "北京");
        params.addProperty("to_region", "北京");
        params.addProperty("transport_mode", "riding");

        String url = "https://leancloud.cn/1.1/functions/baiduDistanceCoors";
        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();

        //add reuqest header
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/json");
        con.setRequestProperty("charset", "utf-8");
        con.setRequestProperty("X-LC-Id", "lsreYNDOs3JODLsFbIRKGdDb-gzGzoHsz");
        con.setRequestProperty("X-LC-Key", "9MNq2j3O2SCTkNbaERmRAWVx");
        con.setRequestProperty("X-LC-Prod", "0");

        // Send post request
        con.setDoOutput(true);
        OutputStreamWriter wr = new OutputStreamWriter(con.getOutputStream());
        wr.write(params.toString());
        wr.flush();
        wr.close();

        BufferedReader in = new BufferedReader(
            new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuffer response = new StringBuffer();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        return new JsonParser().parse(response.toString()).getAsJsonObject();
    }

    private static Location loc(Coordinate coordinate) {
        return Location.Builder.newInstance().setCoordinate(coordinate).build();
    }


    public static void main(String[] args) throws Exception {
        String url = "http://api.map.baidu.com/direction/v1?mode=riding&origin=40.056878,116.30815&destination=39.915285,116.403857&origin_region=%E5%8C%97%E4%BA%AC&destination_region=%E5%8C%97%E4%BA%AC&output=json&ak=o0RuzQRVNo1YuIorz50uWVLs6DXVSo7X";
//      // get distance j.get("result").get("routes").get(0).get("distance").getAsDouble()
        // get duration j.get("result").get("routes").get(0).get("duration").getAsDouble()
//        JsonObject j = getHTML(url);
//        System.out.println(j.toString());
//
//        Location from = new Location()

//      在这里顺序是 (x, y) x是longitude, y是latitude, 百度请求的顺序是[latitude, longitude]
        Location from = loc(Coordinate.newInstance(116.403857, 39.915285));
        Location to = loc(Coordinate.newInstance(116.30815, 40.056878));

        JsonObject res = postLeanCloud(from, to);

        System.out.println(res.toString());
    }


}
