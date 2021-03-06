package com.graphhopper.jsprit.examples;


import com.graphhopper.jsprit.core.problem.Location;
import com.graphhopper.jsprit.core.problem.job.Shipment;
import com.graphhopper.jsprit.core.problem.solution.route.activity.TimeWindow;
import com.graphhopper.jsprit.core.util.Coordinate;
import com.graphhopper.jsprit.examples.EnRouteRealTime;

import java.lang.reflect.Array;
import java.util.ArrayList;

/**
 * Created by jiusi on 16/4/7.
 */
public class EnRouteRealTimeTakeTest {


    public EnRouteVehicleContext bike1(double currentTimestamp) {
        // 这个人从建外soho出发送摩玛大厦,中途取易思凯的件送甜水园北里
        // 现在位置是光华路东侧路口,和三环交界,他的位置上有个新快递,但是方向是往西的


        // 1. pickedUps
        ArrayList<double[]> pickupLocs = new ArrayList<double[]>();
        pickupLocs.add(new double[]{116.466414, 39.912085}); // 建外soho
        ArrayList<double[]> pickupTimeWindowTSs = new ArrayList<double[]>();
        pickupTimeWindowTSs.add(new double[]{1459998630.0, 1460000400}); // 11:10, 11:40

        ArrayList<double[]> deliverLocs = new ArrayList<double[]>();
        deliverLocs.add(new double[]{116.479349, 39.929336}); // 摩码大厦
        ArrayList<double[]> deliverTimeWindowTSs = new ArrayList<double[]>();
        deliverTimeWindowTSs.add(new double[]{1459998630.0, 1460002224.0}); // 11:10, 12:10

        ArrayList<String> pickupAddresses = new ArrayList<String>();
        pickupAddresses.add("建外soho B100");

        ArrayList<String> customerNames = new ArrayList<String>();
        customerNames.add("摩码大厦小姐");
        ArrayList<String> deliverAddresses = new ArrayList<String>();
        deliverAddresses.add("摩码大厦301");
        ArrayList<String> customerPhones = new ArrayList<String>();
        customerPhones.add("123");

        // 2. on going pickup
        double[] pickupLoc = {116.469601, 39.925889}; // 易思凯斯咖啡
        double[] pickupTimeWindowTS = {1459999824.0, 1460001624.0}; // 11:30, 12:00
        double[] deliverLoc = {116.488349, 39.934078}; // 甜水园北里
        double[] deliverTimeWindwoTS = {1459999824.0, 1460003424.0}; // 11:30, 12:30

        String customerName = "甜水园北里屌丝";
        String pickupAddress = "易思凯斯咖啡";
        String deliverAddress = "甜水园北里4栋2单元202";
        String customerPhone = "444";


        return new EnRouteVehicleContext(
            "bike1", currentTimestamp, //11:40
            new double[]{116.468354, 39.919238}, // 光华路东侧路口

            pickupLocs, deliverLocs,
            pickupTimeWindowTSs, deliverTimeWindowTSs,
            customerNames, customerPhones, pickupAddresses, deliverAddresses,

            pickupLoc, deliverLoc,
            pickupTimeWindowTS, deliverTimeWindwoTS,
            customerName, customerPhone, pickupAddress, deliverAddress
        );

    }

    public EnRouteVehicleContext bike2(double currentTimestamp) {
        // 这个人从现代城鲜花店到朝阳区人民政府,送花,应该接上途中那个往西的快递
        // 现在位置是光华路东侧路口,和三环交界,他的位置上有个新快递,但是方向是往西的

        // picked-ups
        ArrayList<double[]> pickupLocs = new ArrayList<double[]>();
        pickupLocs.add(new double[]{116.482862, 39.912448}); // 现代城鲜花店
        ArrayList<double[]> pickupTimeWindowTSs = new ArrayList<double[]>();
        pickupTimeWindowTSs.add(new double[]{1459998630.0, 1460000400}); // 11:10, 11:40

        ArrayList<double[]> deliverLocs = new ArrayList<double[]>();
        deliverLocs.add(new double[]{116.45001, 39.927189}); // 朝阳区人民政府
        ArrayList<double[]> deliverTimeWindowTSs = new ArrayList<double[]>();
        deliverTimeWindowTSs.add(new double[]{1459998630.0, 1460002224.0}); // 11:10, 12:10


        ArrayList<String> pickupAddresses = new ArrayList<String>();
        pickupAddresses.add("现代城鲜花店");

        ArrayList<String> customerNames = new ArrayList<String>();
        customerNames.add("张菊长");
        ArrayList<String> deliverAddresses = new ArrayList<String>();
        deliverAddresses.add("朝阳区人民政府菊长办公室");
        ArrayList<String> customerPhones = new ArrayList<String>();
        customerPhones.add("110");

        // 2. No on going pickup

        return new EnRouteVehicleContext(
            "bike2", currentTimestamp, //11:40
            new double[]{116.468354, 39.919238}, // 光华路东侧路口

            pickupLocs, deliverLocs,
            pickupTimeWindowTSs, deliverTimeWindowTSs,
            customerNames, customerPhones, pickupAddresses, deliverAddresses,

            null, null,
            null, null,
            null, null, null, null
        );

    }

    public Shipment newShipmentWithTW(double[] pickupLoc, double[] deliverLoc, double[] pickupTW, double[] deliverTW,
                                      String customerName, String customerPhone, String pickupAddress, String deliverAddress, double currentTimestamp) {

        double pstart = pickupTW[0] - currentTimestamp;
        if (pstart < 0) {
            pstart = 0;
        }

        double dstart = deliverTW[0] - currentTimestamp;
        if(dstart < 0) {
            dstart = 0;
        }

        double[] pickupTimeWindowAlgo = new double[]{
            pstart,
            pickupTW[1] - currentTimestamp
        };

        double[] deliverTimeWindowAlgo = new double[]{
            dstart,
            deliverTW[1] - currentTimestamp
        };

        return Shipment.Builder.newInstance("pickup:" + pickupAddress + ", deliver:" + deliverAddress)
            .addSizeDimension(0, 1)
            .setPickupLocation(loc(Coordinate.newInstance(pickupLoc[0], pickupLoc[1])))
            .setPickupTimeWindow(new TimeWindow(pickupTimeWindowAlgo[0], pickupTimeWindowAlgo[1]))
            .setDeliveryLocation(loc(Coordinate.newInstance(deliverLoc[0], deliverLoc[1])))
            .setDeliveryTimeWindow(new TimeWindow(deliverTimeWindowAlgo[0], deliverTimeWindowAlgo[1]))
            .build();
    }


    public Shipment newShipment(double currentTimestamp) {
        // 新出现的货是从东到西的
        // 这里需要考虑的是TW的修改,知道现在的时间得到TW而不是timestamp
        // 从嘉里中心北楼 116.467295,39.919685 接单送往日坛商务楼 116.452471,39.919293

        double[] pickupLoc = {116.467295, 39.919685}; // 嘉里中心
        double[] pickupTW = {1459998024.0, 1459999800}; // 11:00, 11:30
        double[] deliverLoc = {116.452471, 39.919293}; // 日坛商务楼
        double[] deliverTW = {1459998024.0, 1460005200}; // 11:00, 13:00

        String customerName =  "嘉里中心 Ovelia de la Wang";
        String customerPhone = "456";
        String pickupAddress = "嘉里中心1107前台";
        String deliverAddress = "日坛商务楼";

        return newShipmentWithTW(pickupLoc, deliverLoc,
            pickupTW, deliverTW,
            customerName, customerPhone, pickupAddress, deliverAddress, currentTimestamp);
    }


    private static Location loc(Coordinate coordinate) {
        return Location.Builder.newInstance().setCoordinate(coordinate).build();
    }


    public static void main(String[] args) {
        /*
        这个测试是针对近距离但是方向不对的快递员接货的问题
        货物会分配给离得不太远但是方向对的快递员,而不会分配给最近的那个快递员,已达到cost最小的结果
         */

        double currentTimestamp = 1459999200; // 4-7 11:20

        EnRouteRealTimeTakeTest t = new EnRouteRealTimeTakeTest();
        EnRouteVehicleContext bike1 = t.bike1(currentTimestamp);
        EnRouteVehicleContext bike2 = t.bike2(currentTimestamp);

        Shipment newShipment = t.newShipment(currentTimestamp);

        EnRouteRealTime problemBuilder = new EnRouteRealTime();

        ArrayList<EnRouteVehicleContext> contexts = new ArrayList<EnRouteVehicleContext>();
        contexts.add(bike1);
        contexts.add(bike2);

        ArrayList<Shipment> shipments = new ArrayList<Shipment>();
        shipments.add(newShipment);

        problemBuilder.realTimeProblemBuilder(contexts, shipments);


    }


}
