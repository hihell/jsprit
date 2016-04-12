package com.graphhopper.jsprit.examples;

import com.graphhopper.jsprit.core.algorithm.VehicleRoutingAlgorithm;
import com.graphhopper.jsprit.core.algorithm.io.VehicleRoutingAlgorithms;
import com.graphhopper.jsprit.core.problem.Location;
import com.graphhopper.jsprit.core.problem.VehicleRoutingProblem;
import com.graphhopper.jsprit.core.problem.job.Shipment;
import com.graphhopper.jsprit.core.problem.solution.VehicleRoutingProblemSolution;
import com.graphhopper.jsprit.core.problem.solution.route.VehicleRoute;
import com.graphhopper.jsprit.core.problem.solution.route.activity.TimeWindow;
import com.graphhopper.jsprit.core.problem.vehicle.Vehicle;
import com.graphhopper.jsprit.core.problem.vehicle.VehicleImpl;
import com.graphhopper.jsprit.core.problem.vehicle.VehicleType;
import com.graphhopper.jsprit.core.problem.vehicle.VehicleTypeImpl;
import com.graphhopper.jsprit.core.util.Coordinate;
import com.graphhopper.jsprit.core.util.ManhattanCosts;
import com.graphhopper.jsprit.core.util.Solutions;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by jiusi on 16/4/12.
 */
public class EnRouteTestInitRoute {
    public static String exampleBase = "/Users/jiusi/IdeaProjects/jsprit/jsprit-examples/";

    public Vehicle buildVehicle(String vehicleId, double[] currentLoc) {
        VehicleTypeImpl.Builder vehicleTypeBuilder = VehicleTypeImpl.Builder.newInstance("bike")
            .addCapacityDimension(0, 3);
        vehicleTypeBuilder.setCostPerDistance(10.0);
        VehicleType vehicleType = vehicleTypeBuilder.build();

        String staffCurrentCoorStr =  vehicleId + "@[" + currentLoc[0] + "," + currentLoc[1] + "]";
        VehicleImpl.Builder vehicleBuilder = VehicleImpl.Builder.newInstance(staffCurrentCoorStr);
        vehicleBuilder.setStartLocation(loc(Coordinate.newInstance(currentLoc[0], currentLoc[1]))).setReturnToDepot(false);
        vehicleBuilder.setType(vehicleType);
        vehicleBuilder.addSkill(vehicleId); // make sure the going deli will only be taken by the vehicle

        return vehicleBuilder.build();
    }


    private static Location loc(Coordinate coordinate) {
        return Location.Builder.newInstance().setCoordinate(coordinate).build();
    }

    public void test1() {
        Vehicle v1 = buildVehicle("v1", new double[]{1,5});

        Shipment pickedUpV1 = Shipment.Builder.newInstance("pickedUpV1")
            .addSizeDimension(0, 1)
            .setPickupLocation(loc(Coordinate.newInstance(0, 0)))
            .setDeliveryLocation(loc(Coordinate.newInstance(1, 10)))
            .setPickupTimeWindow(new TimeWindow(0, 10))
            .setDeliveryTimeWindow(new TimeWindow(0, 10))
            .build();


        VehicleRoute vehicleRoute = VehicleRoute.Builder.newInstance(v1)
            .addPickup(pickedUpV1).addDelivery(pickedUpV1).build();

        VehicleRoutingProblem.Builder vrpBuilder = VehicleRoutingProblem.Builder.newInstance();
        vrpBuilder.addVehicle(v1);
        vrpBuilder.addInitialVehicleRoute(vehicleRoute);

        vrpBuilder.setRoutingCost(new ManhattanCosts());
        vrpBuilder.setFleetSize(VehicleRoutingProblem.FleetSize.FINITE);

        VehicleRoutingProblem problem = vrpBuilder.build();

        VehicleRoutingAlgorithm algorithm = VehicleRoutingAlgorithms.readAndCreateAlgorithm(problem, exampleBase + "input/algorithmConfig.xml");

        Collection<VehicleRoutingProblemSolution> solutions = algorithm.searchSolutions();

        VehicleRoutingProblemSolution bestSolution = Solutions.bestOf(solutions);

        EnRouteRealTime.solutionWriter(problem, solutions, bestSolution);

    }


    public static void main(String[] args) {
        EnRouteTestInitRoute t = new EnRouteTestInitRoute();
        t.test1();
    }
}
