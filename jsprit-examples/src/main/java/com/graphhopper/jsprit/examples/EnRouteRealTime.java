package com.graphhopper.jsprit.examples;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.lang.reflect.Array;
import java.util.*;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.graphhopper.jsprit.analysis.toolbox.GraphStreamViewer;
import com.graphhopper.jsprit.analysis.toolbox.Plotter;
import com.graphhopper.jsprit.core.algorithm.VehicleRoutingAlgorithm;
import com.graphhopper.jsprit.core.algorithm.io.VehicleRoutingAlgorithms;
import com.graphhopper.jsprit.core.problem.Location;
import com.graphhopper.jsprit.core.problem.VehicleRoutingProblem;
import com.graphhopper.jsprit.core.problem.io.VrpXMLWriter;
import com.graphhopper.jsprit.core.problem.job.Delivery;
import com.graphhopper.jsprit.core.problem.job.Pickup;
import com.graphhopper.jsprit.core.problem.VehicleRoutingProblem.Builder;

import com.graphhopper.jsprit.core.problem.job.Shipment;
import com.graphhopper.jsprit.core.problem.solution.VehicleRoutingProblemSolution;
import com.graphhopper.jsprit.core.problem.solution.route.VehicleRoute;
import com.graphhopper.jsprit.core.problem.solution.route.activity.TimeWindow;
import com.graphhopper.jsprit.core.problem.vehicle.Vehicle;
import com.graphhopper.jsprit.core.problem.vehicle.VehicleImpl;
import com.graphhopper.jsprit.core.problem.vehicle.VehicleType;
import com.graphhopper.jsprit.core.problem.vehicle.VehicleTypeImpl;
import com.graphhopper.jsprit.core.reporting.SolutionPrinter;
import com.graphhopper.jsprit.core.util.Coordinate;
import com.graphhopper.jsprit.core.util.Solutions;

import com.graphhopper.jsprit.core.util.ManhattanCosts;
import sun.jvm.hotspot.code.ConstantOopReadValue;

/**
 * Created by jiusi on 16/4/1.
 */


public class EnRouteRealTime {
    public static String exampleBase = "/Users/jiusi/IdeaProjects/jsprit/jsprit-examples/";

    public ArrayList<ArrayList<JsonObject>> readFile(String path) {

        ArrayList<JsonObject> destinations = new ArrayList<JsonObject>();
        ArrayList<ArrayList<JsonObject>> lists = new ArrayList<ArrayList<JsonObject>>();
        // get that fucking file
        // read it into list
        File file = new File(path);
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));

            try {
                String line;

                while ((line = br.readLine()) != null) {

                    JsonParser parser = new JsonParser();
                    JsonObject o = parser.parse(line).getAsJsonObject();

                    destinations.add(o);
                }
            } catch (java.io.IOException fuck) {
                System.out.println("line fucked");
            }

            // make lists according to date


            ArrayList<JsonObject> list = new ArrayList<JsonObject>();
            JsonObject preEle = destinations.get(0);
            for (int i = 1; i < destinations.size(); i++) {
                JsonObject ele = destinations.get(i);

                String date = ele.get("date").getAsString();

                if (date.equals(preEle.get("date").getAsString())) {
                    list.add(ele);
                } else {
                    lists.add(list);
                    list = new ArrayList<JsonObject>();
                    list.add(ele);
                }

                preEle = ele;
            }
            if (list.size() != 0) {
                lists.add(list);
            }

        } catch (java.io.IOException fuck) {
            System.out.println("wtf:" + fuck.toString());
        }
        return lists;
    }

    private static Location loc(Coordinate coordinate) {
        return Location.Builder.newInstance().setCoordinate(coordinate).build();
    }

    public Map<String, double[]> bizNameCoorMap = new HashMap<String, double[]>();

    public void enroute(ArrayList<JsonObject> list, int staffSize, double[] staffInitCoor) {

        // 0. build vehicles (staff with bike)
        ArrayList<VehicleImpl> vehicles = new ArrayList<VehicleImpl>();
        VehicleTypeImpl.Builder vehicleTypeBuilder = VehicleTypeImpl.Builder.newInstance("vehicleType")
            .addCapacityDimension(0, 1);
        vehicleTypeBuilder.setCostPerDistance(10.0);
        VehicleType vehicleType = vehicleTypeBuilder.build();

        for (int i = 0; i < staffSize; i++) {
            String staffInitCoorStr = "vehicles" + i + "@[" + staffInitCoor[0] + "," + staffInitCoor[1] + "]";
            VehicleImpl.Builder vehicleBuilder = VehicleImpl.Builder.newInstance(staffInitCoorStr);
            vehicleBuilder.setStartLocation(loc(Coordinate.newInstance(staffInitCoor[0], staffInitCoor[1]))).setReturnToDepot(false);
            vehicleBuilder.setType(vehicleType);
            VehicleImpl vehicle = vehicleBuilder.build();

            vehicles.add(vehicle);
        }


        // 1. build shipments
        ArrayList<Shipment> shipments = new ArrayList<Shipment>();
        for (int i = 0; i < list.size(); i++) {
            JsonObject ele = list.get(i);

            float[] deliCoor = {ele.getAsJsonArray("coor").get(0).getAsFloat(),
                ele.getAsJsonArray("coor").get(1).getAsFloat()};

            String date = ele.get("date").getAsString();
            String bizName = ele.get("businessName").getAsString();
            double[] bizCoor = bizNameCoorMap.get(bizName);

            String customerName = this.safeGetAsString(ele, "customerName");
            String customerPhone = this.safeGetAsString(ele, "customerPhone");
            String deliverAddr = this.safeGetAsString(ele, "deliverAddress");

            if (bizCoor != null) {

                System.out.println("Shipment start:" + Arrays.toString(bizCoor) + " stop:" + Arrays.toString(deliCoor));

                Shipment shipment = Shipment.Builder.newInstance(customerName + ',' + customerPhone + ',' + deliverAddr + " i:" + i)
                    .addSizeDimension(0, 1)
                    .setPickupLocation(loc(Coordinate.newInstance(bizCoor[0], bizCoor[1])))
                    .setDeliveryLocation(loc(Coordinate.newInstance(deliCoor[0], deliCoor[1])))
                    .build();
                shipments.add(shipment);
            } else {
                System.out.println("warn: biz no coor:" + bizName);
            }
        }

        // 2. build a vrp, add vehicles and shipments to it
        VehicleRoutingProblem.Builder vrpBuilder = VehicleRoutingProblem.Builder.newInstance();
        for (VehicleImpl v : vehicles) {
            vrpBuilder.addVehicle(v);
        }

        for (Shipment s : shipments) {
            vrpBuilder.addJob(s);
        }

        vrpBuilder.setFleetSize(VehicleRoutingProblem.FleetSize.FINITE);
        VehicleRoutingProblem problem = vrpBuilder.build();


        // 3. search solutions and plot the fucking result
        /*
         * get the algorithm out-of-the-box.
		 */
        VehicleRoutingAlgorithm algorithm = VehicleRoutingAlgorithms.readAndCreateAlgorithm(problem, exampleBase + "input/algorithmConfig.xml");
//		algorithm.setMaxIterations(30000);
        /*
         * and search a solution
		 */
        Collection<VehicleRoutingProblemSolution> solutions = algorithm.searchSolutions();

		/*
         * get the best
		 */
        VehicleRoutingProblemSolution bestSolution = Solutions.bestOf(solutions);

		/*
         * write out problem and solution to xml-file
		 */
        new VrpXMLWriter(problem, solutions).write(exampleBase + "output/shipment-problem-with-solution.xml");

		/*
         * print nRoutes and totalCosts of bestSolution
		 */
        SolutionPrinter.print(bestSolution);

		/*
         * plot problem without solution
		 */
        Plotter problemPlotter = new Plotter(problem);
        problemPlotter.plotShipments(true);
        problemPlotter.plot(exampleBase + "output/enRoutePickupAndDeliveryWithMultipleLocationsExample_problem.png", "en-route pickup and delivery");

		/*
         * plot problem with solution
		 */
        Plotter solutionPlotter = new Plotter(problem, Arrays.asList(Solutions.bestOf(solutions).getRoutes().iterator().next()));
        solutionPlotter.plotShipments(true);
        solutionPlotter.plot(exampleBase + "output/enRoutePickupAndDeliveryWithMultipleLocationsExample_solution.png", "en-route pickup and delivery");

        new GraphStreamViewer(problem, Solutions.bestOf(solutions)).labelWith(GraphStreamViewer.Label.ACTIVITY).setRenderDelay(100).setRenderShipments(true).display();
    }

    public String safeGetAsString(JsonObject j, String stringFieldName) {
        if (j.get(stringFieldName) != null) {
            return j.get(stringFieldName).getAsString();
        } else {
            return "";
        }
    }

    public void batchProblemBuilder(String inputPath) {
        // generate shipments
        EnRouteRealTime r = new EnRouteRealTime();

        r.bizNameCoorMap.put("格林沙拉（自销）", new double[]{39.936935, 116.460901});
        r.bizNameCoorMap.put("恩之方", new double[]{39.940916, 116.451592});
        r.bizNameCoorMap.put("念客", new double[]{39.923294, 116.466292});
        r.bizNameCoorMap.put("拌物沙拉", new double[]{39.916824, 116.462717});
        r.bizNameCoorMap.put("宇甜品", new double[]{39.936895, 116.460746});
        r.bizNameCoorMap.put("拌物", new double[]{39.917, 116.463035});
        r.bizNameCoorMap.put("格林沙拉", new double[]{39.936935, 116.460901});
        r.bizNameCoorMap.put("匹考克", new double[]{39.924461, 116.45983});
        r.bizNameCoorMap.put("臻享甜品", new double[]{39.929068, 116.484437});
        r.bizNameCoorMap.put("爱贝里", new double[]{39.942715, 116.461778});
        r.bizNameCoorMap.put("仙juice", new double[]{39.92435, 116.463573});

        // make that vrp problem and plot it

        // modify plot function as let it show receiver info
        ArrayList<ArrayList<JsonObject>> lists = r.readFile(inputPath);

        double[] dormitary = {39.914, 116.502};
        r.enroute(lists.get(0), 20, dormitary);
    }


    public void realTimeProblemBuilder(ArrayList<EnRouteVehicleContext> vehicleContexts, ArrayList<Shipment> newShipments) {
        // 0. get all vehicle's status from API and make EnRouteContext
        // status: picked up, location, all pending plans

        // 1.1 build vehicles



        VehicleRoutingProblem.Builder vrpBuilder = VehicleRoutingProblem.Builder.newInstance();

        for(EnRouteVehicleContext ctx : vehicleContexts) {
            vrpBuilder.addVehicle(ctx.vehicle);
        }

        vrpBuilder.addAllJobs(newShipments);

        //  1.2 setup shipments

        // 1.2.1 setup on going pick up

        for (int i = 0; i < vehicleContexts.size(); i++) {
            EnRouteVehicleContext vehicleContext = vehicleContexts.get(i);
            String vehicleId = vehicleContext.vehicleId;
            EnRouteVehicleContext.ShipmentInfo onGoingPickUp = vehicleContext.onGoingPickUp;

            if (onGoingPickUp != null) {
                double[] pickLoc = onGoingPickUp.pickupLoc;
                double[] pickTW = onGoingPickUp.pickupTimeWindowAlgo;
                double[] deliLoc = onGoingPickUp.deliverLoc;

                double[] deliTW = onGoingPickUp.deliverTimeWindowAlgo;

                Shipment shipment = Shipment.Builder.newInstance(
                        onGoingPickUp.customerName + ',' + onGoingPickUp.customerPhone + ',' + onGoingPickUp.customerAddress
                    )
                    .addSizeDimension(0, 1)
                    .setPickupLocation(loc(Coordinate.newInstance(pickLoc[0], pickLoc[1])))
                    .setPickupTimeWindow(new TimeWindow(pickTW[0], pickTW[1]))
                    .setDeliveryLocation(loc(Coordinate.newInstance(deliLoc[0], deliLoc[1])))
                    .setDeliveryTimeWindow(new TimeWindow(deliTW[0], deliTW[1])) // set that fucking window
                    .addRequiredSkill(vehicleId) // make sure this on going deli will only be taken by that vehicle
                    .build();

//                VehicleRoute initialRoute = VehicleRoute.Builder.newInstance(
//                    vehicleContext.vehicle
//                ).addPickup(shipment).addDelivery(shipment).build();
//
//                vrpBuilder.addInitialVehicleRoute(initialRoute);
                vrpBuilder.addJob(shipment);
            }

        }

        //  1.2.2 setup picked-ups
        //  use required skill to make sure on going delivery won't be allocated to other vehicles
        for (int i = 0; i < vehicleContexts.size(); i++) {
            EnRouteVehicleContext vehicleContext = vehicleContexts.get(i);
            String vehicleId = vehicleContext.vehicleId;
            ArrayList<EnRouteVehicleContext.ShipmentInfo> pickedUps = vehicleContext.pickedups;

            for (int j = 0; j < pickedUps.size(); j++) {
                EnRouteVehicleContext.ShipmentInfo pickedUp = pickedUps.get(j);

                double[] pickUpLoc = pickedUp.pickupLoc;
                double[] pickUpTW = pickedUp.pickupTimeWindowAlgo;
                double[] deliLoc = pickedUp.deliverLoc;
                double[] deliTW = pickedUp.deliverTimeWindowAlgo;

                Shipment shipment = Shipment.Builder.newInstance(
                    pickedUp.customerName + ',' + pickedUp.customerPhone + ',' + pickedUp.customerAddress
                )
                    .addSizeDimension(0, 1)
                    .setPickupLocation(loc(Coordinate.newInstance(pickUpLoc[0], pickUpLoc[1])))
                    .setPickupTimeWindow(new TimeWindow(pickUpTW[0], pickUpTW[1]))
                    .setDeliveryLocation(loc(Coordinate.newInstance(deliLoc[0], deliLoc[1])))
                    .setDeliveryTimeWindow(new TimeWindow(deliTW[0], deliTW[1]))
                    .addRequiredSkill(vehicleId)
                    .build();

                VehicleRoute initialRoute = VehicleRoute.Builder.newInstance(
                    vehicleContext.vehicle
                ).addPickup(shipment).addDelivery(shipment).build();

                vrpBuilder.addInitialVehicleRoute(initialRoute);
            }
        }

        // 2 add shipment and deliveries to vrpBuilder
        vrpBuilder.setRoutingCost(new BaiduDistance());

        vrpBuilder.setFleetSize(VehicleRoutingProblem.FleetSize.FINITE);
        VehicleRoutingProblem problem = vrpBuilder.build();

        /*
         * get the algorithm out-of-the-box.
		 */
        VehicleRoutingAlgorithm algorithm = VehicleRoutingAlgorithms.readAndCreateAlgorithm(problem, exampleBase + "input/algorithmConfig.xml");
//		algorithm.setMaxIterations(30000);
        /*
         * and search a solution
		 */
        Collection<VehicleRoutingProblemSolution> solutions = algorithm.searchSolutions();

		/*
         * get the best
		 */
        VehicleRoutingProblemSolution bestSolution = Solutions.bestOf(solutions);

        this.solutionWriter(problem, solutions, bestSolution);
    }


    public void solutionWriter(VehicleRoutingProblem problem,
                               Collection<VehicleRoutingProblemSolution> solutions,
                               VehicleRoutingProblemSolution bestSolution) {
        new VrpXMLWriter(problem, solutions).write("output/shipment-problem-with-solution.xml");

		/*
         * print nRoutes and totalCosts of bestSolution
		 */
        SolutionPrinter.print(bestSolution);

		/*
		 * plot problem without solution
		 */
        Plotter problemPlotter = new Plotter(problem);
        problemPlotter.plotShipments(true);
        problemPlotter.plot("output/enRoutePickupAndDeliveryWithMultipleLocationsExample_problem.png", "en-route pickup and delivery");

		/*
		 * plot problem with solution
		 */
        Plotter solutionPlotter = new Plotter(problem, Arrays.asList(Solutions.bestOf(solutions).getRoutes().iterator().next()));
        solutionPlotter.plotShipments(true);
        solutionPlotter.plot("output/enRoutePickupAndDeliveryWithMultipleLocationsExample_solution.png", "en-route pickup and delivery");

        new GraphStreamViewer(problem, Solutions.bestOf(solutions)).labelWith(GraphStreamViewer.Label.ACTIVITY).setRenderDelay(100).setRenderShipments(true).display();


    }

    public void showAllBizNames(String inputPath) {

        EnRouteRealTime r = new EnRouteRealTime();

        HashSet<String> bizNames = new HashSet<String>();

        ArrayList<ArrayList<JsonObject>> lists = r.readFile(inputPath);

        for (int i = 0; i < lists.size(); i++) {
            ArrayList<JsonObject> list = lists.get(i);

            for (int j = 0; j < list.size(); j++) {
                JsonObject ele = list.get(j);
                try {
                    String bizName = ele.get("businessName").getAsString();

                    bizNames.add(bizName);
                } catch (Exception e) {
                    System.out.println("ij:" + i + j);
                }
            }
        }

        System.out.println(bizNames.toString());
    }


    private static Vehicle getVehicle(String vehicleId, Builder vrpBuilder) {
        for (Vehicle v : vrpBuilder.getAddedVehicles()) {
            if (v.getId().equals(vehicleId)) return v;
        }
        return null;
    }


    public static void main(String[] args) {
        String inputPath = "/Users/jiusi/WebstormProjects/xls2json/output_coor.txt";

        EnRouteRealTime r = new EnRouteRealTime();

//        r.showAllBizNames(inputPath);
//        r.problemBuilder(inputPath);
    }


}
