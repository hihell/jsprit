package com.graphhopper.jsprit.examples;


import java.util.*;

import com.google.gson.JsonObject;
import com.graphhopper.jsprit.analysis.toolbox.GraphStreamViewer;
import com.graphhopper.jsprit.analysis.toolbox.Plotter;
import com.graphhopper.jsprit.core.algorithm.VehicleRoutingAlgorithm;
import com.graphhopper.jsprit.core.algorithm.io.VehicleRoutingAlgorithms;
import com.graphhopper.jsprit.core.problem.Location;
import com.graphhopper.jsprit.core.problem.VehicleRoutingProblem;
import com.graphhopper.jsprit.core.problem.io.VrpXMLWriter;
import com.graphhopper.jsprit.core.problem.job.Shipment;
import com.graphhopper.jsprit.core.problem.solution.VehicleRoutingProblemSolution;
import com.graphhopper.jsprit.core.problem.vehicle.VehicleImpl;
import com.graphhopper.jsprit.core.problem.vehicle.VehicleType;
import com.graphhopper.jsprit.core.problem.vehicle.VehicleTypeImpl;
import com.graphhopper.jsprit.core.reporting.SolutionPrinter;
import com.graphhopper.jsprit.core.util.Coordinate;
import com.graphhopper.jsprit.core.util.Solutions;


public class EnRouteTimeCostTest {
    public static String exampleBase = "/Users/jiusi/IdeaProjects/jsprit/jsprit-examples/";

    private static Location loc(Coordinate coordinate) {
        return Location.Builder.newInstance().setCoordinate(coordinate).build();
    }

    public Map<String, double[]> bizNameCoorMap = new HashMap<String, double[]>();

    public void enroute(ArrayList<JsonObject> list, int staffSize, double[] staffInitCoor) {

        // 0. build vehicles (staff with bike)
        ArrayList<VehicleImpl> vehicles = new ArrayList<VehicleImpl>();
        VehicleTypeImpl.Builder vehicleTypeBuilder = VehicleTypeImpl.Builder.newInstance("vehicleType")
            .addCapacityDimension(0, 15);
        vehicleTypeBuilder.setCostPerDistance(1.0);
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

            if(bizCoor != null) {



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
        if (j.get(stringFieldName) != null ) {
            return j.get(stringFieldName).getAsString();
        } else {
            return "";
        }
    }

    public void problemBuilder(String inputPath) {
        // generate shipments
        EnRouteRealTime r = new EnRouteRealTime();

        r.bizNameCoorMap.put("格林沙拉（自销）", new double[] {39.936935, 116.460901});
        r.bizNameCoorMap.put("恩之方", new double[] {39.940916, 116.451592});
        r.bizNameCoorMap.put("念客",  new double[]{39.923294, 116.466292});
        r.bizNameCoorMap.put("拌物沙拉", new double[]{39.916824, 116.462717});
        r.bizNameCoorMap.put("宇甜品", new double[]{39.936895, 116.460746});
        r.bizNameCoorMap.put("拌物", new double[]{39.917, 116.463035});
        r.bizNameCoorMap.put("格林沙拉" , new double[] {39.936935, 116.460901});
        r.bizNameCoorMap.put("匹考克", new double[] {39.924461, 116.45983});
        r.bizNameCoorMap.put("臻享甜品", new double[] {39.929068, 116.484437});
        r.bizNameCoorMap.put("爱贝里", new double[] {39.942715, 116.461778});
        r.bizNameCoorMap.put("仙juice", new double[] {39.92435, 116.463573});

        // make that vrp problem and plot it

        // modify plot function as let it show receiver info
        ArrayList<ArrayList<JsonObject>> lists = r.readFile(inputPath);

        double []dormitary = {39.914, 116.502};
        r.enroute(lists.get(0), 20, dormitary);
    }

    public void showAllBizNames(String inputPath) {

        EnRouteRealTime r = new EnRouteRealTime();

        HashSet<String> bizNames = new HashSet<String>();

        ArrayList<ArrayList<JsonObject>> lists = r.readFile(inputPath);

        for (int i=0; i<lists.size(); i++) {
            ArrayList<JsonObject> list= lists.get(i);

            for(int j=0; j<list.size(); j++) {
                JsonObject ele = list.get(j);
                try {
                    String bizName = ele.get("businessName").getAsString();

                    bizNames.add(bizName);
                }catch (Exception e) {
                    System.out.println("ij:" + i + j);
                }
            }
        }

        System.out.println(bizNames.toString());
    }


    public static void main(String[] argss) {
        String inputPath = "/Users/jiusi/WebstormProjects/xls2json/output_coor.txt";

        EnRouteTimeCostTest r = new EnRouteTimeCostTest();

//        r.showAllBizNames(inputPath);
        r.problemBuilder(inputPath);
    }


}
