/*******************************************************************************
 * Copyright (C) 2014  Stefan Schroeder
 * <p/>
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or (at your option) any later version.
 * <p/>
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.graphhopper.jsprit.examples;

import com.graphhopper.jsprit.analysis.toolbox.GraphStreamViewer;
import com.graphhopper.jsprit.analysis.toolbox.Plotter;
import com.graphhopper.jsprit.core.algorithm.VehicleRoutingAlgorithm;
import com.graphhopper.jsprit.core.algorithm.box.SchrimpfFactory;
import com.graphhopper.jsprit.core.problem.Location;
import com.graphhopper.jsprit.core.problem.VehicleRoutingProblem;
import com.graphhopper.jsprit.core.problem.io.VrpXMLReader;
import com.graphhopper.jsprit.core.problem.io.VrpXMLWriter;
import com.graphhopper.jsprit.core.problem.job.Shipment;
import com.graphhopper.jsprit.core.problem.solution.VehicleRoutingProblemSolution;
import com.graphhopper.jsprit.core.problem.vehicle.Vehicle;
import com.graphhopper.jsprit.core.problem.vehicle.VehicleImpl;
import com.graphhopper.jsprit.core.problem.vehicle.VehicleImpl.Builder;
import com.graphhopper.jsprit.core.problem.vehicle.VehicleType;
import com.graphhopper.jsprit.core.problem.vehicle.VehicleTypeImpl;
import com.graphhopper.jsprit.core.reporting.SolutionPrinter;
import com.graphhopper.jsprit.core.util.Coordinate;
import com.graphhopper.jsprit.core.util.Solutions;
import com.graphhopper.jsprit.util.Examples;

import java.util.Collection;


public class SimpleEnRoutePickupAndDeliveryOpenRoutesExample {
    public VehicleRoutingProblem xml2Problem(String xmlPath) {
        VehicleRoutingProblem.Builder vrpBuilder = VehicleRoutingProblem.Builder.newInstance();

        new VrpXMLReader(vrpBuilder).read(xmlPath);

        return vrpBuilder.build();
    }

    public VehicleRoutingProblem manualProblem() {
        /*
         * get a vehicle type-builder and build a type with the typeId "vehicleType" and a capacity of 2
		 */
        VehicleTypeImpl.Builder vehicleTypeBuilderRegular = VehicleTypeImpl.Builder.newInstance("vehicleRegular").addCapacityDimension(0, 2).addCapacityDimension(1, 1);
        VehicleType vehicleTypeRegular = vehicleTypeBuilderRegular.build();

        VehicleTypeImpl.Builder vehicleTypeBuilderLarge = VehicleTypeImpl.Builder.newInstance("vehicleLarge").addCapacityDimension(0, 2).addCapacityDimension(1, 10);
        VehicleType vehicleTypeLarge = vehicleTypeBuilderLarge.build();

		/*
         * get a vehicle-builder and build a vehicle located at (10,10) with type "vehicleType"
		 */
        Builder vehicleBuilder = VehicleImpl.Builder.newInstance("vehicle1");
        vehicleBuilder.setStartLocation(loc(Coordinate.newInstance(5, 20)));
        vehicleBuilder.setType(vehicleTypeLarge);
        vehicleBuilder.setReturnToDepot(false);

        VehicleImpl vehicle1 = vehicleBuilder.build();

        vehicleBuilder = VehicleImpl.Builder.newInstance("vehicle2");
        vehicleBuilder.setStartLocation(loc(Coordinate.newInstance(10, 10)));
        vehicleBuilder.setType(vehicleTypeRegular);
        vehicleBuilder.setReturnToDepot(false);
        VehicleImpl vehicle2 = vehicleBuilder.build();


		/*
         * build shipments at the required locations, each with a capacity-demand of 1.
		 * 4 shipments
		 * 1: (5,7)->(6,9)
		 * 2: (5,13)->(6,11)
		 * 3: (15,7)->(14,9)
		 * 4: (15,13)->(14,11)
		 */

        Shipment shipment1 = Shipment.Builder.newInstance("1").addSizeDimension(0, 1).addSizeDimension(1, 1).setPickupLocation(loc(Coordinate.newInstance(5, 7))).setDeliveryLocation(loc(Coordinate.newInstance(6, 9))).build();
        Shipment shipment2 = Shipment.Builder.newInstance("2").addSizeDimension(0, 1).addSizeDimension(1, 10).setPickupLocation(loc(Coordinate.newInstance(5, 13))).setDeliveryLocation(loc(Coordinate.newInstance(6, 11))).build();

        Shipment shipment3 = Shipment.Builder.newInstance("3").addSizeDimension(0, 1).addSizeDimension(1, 1).setPickupLocation(loc(Coordinate.newInstance(11, 12))).setDeliveryLocation(loc(Coordinate.newInstance(12, 12))).build();
//        Shipment shipment4 = Shipment.Builder.newInstance("4").addSizeDimension(0, 1).setPickupLocation(loc(Coordinate.newInstance(15, 13))).setDeliveryLocation(loc(Coordinate.newInstance(14, 11))).build();


        VehicleRoutingProblem.Builder vrpBuilder = VehicleRoutingProblem.Builder.newInstance();
        vrpBuilder.addVehicle(vehicle1);
        vrpBuilder.addVehicle(vehicle2);
        vrpBuilder.setFleetSize(VehicleRoutingProblem.FleetSize.FINITE);
//        vrpBuilder.addJob(shipment1).addJob(shipment2).addJob(shipment3).addJob(shipment4);
        vrpBuilder.addJob(shipment1).addJob(shipment2).addJob(shipment3);

        return vrpBuilder.build();
    }

    public static void main(String[] args) {

        SimpleEnRoutePickupAndDeliveryOpenRoutesExample eigen = new SimpleEnRoutePickupAndDeliveryOpenRoutesExample();

        /*
         * some preparation - create output folder
		 */
        Examples.createOutputFolder();


        // get a vehicle problem by building it from xml or manually
        VehicleRoutingProblem problem = eigen.xml2Problem("jsprit-examples/input/enroute_pickup_delivery.xml");

		/*
         * get the algorithm out-of-the-box.
		 */
        VehicleRoutingAlgorithm algorithm = new SchrimpfFactory().createAlgorithm(problem);

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
        new VrpXMLWriter(problem, solutions).write("output/shipment-problem-with-solution.xml");

		/*
		 * print nRoutes and totalCosts of bestSolution
		 */
        SolutionPrinter.print(problem, bestSolution, SolutionPrinter.Print.VERBOSE);

		/*
		 * plot problem without solution
		 */
        Plotter problemPlotter = new Plotter(problem);
        problemPlotter.plotShipments(true);
        problemPlotter.plot("output/simpleEnRoutePickupAndDeliveryExample_problem.png", "en-route pickup and delivery");

		/*
		 * plot problem with solution
		 */
        Plotter solutionPlotter = new Plotter(problem, Solutions.bestOf(solutions).getRoutes());
        solutionPlotter.plotShipments(true);
        solutionPlotter.plot("output/simpleEnRoutePickupAndDeliveryExample_solution.png", "en-route pickup and delivery");

        new GraphStreamViewer(problem, bestSolution).setRenderShipments(true).setRenderDelay(100).display();
    }

    private static Location loc(Coordinate coordinate) {
        return Location.Builder.newInstance().setCoordinate(coordinate).build();
    }

}
