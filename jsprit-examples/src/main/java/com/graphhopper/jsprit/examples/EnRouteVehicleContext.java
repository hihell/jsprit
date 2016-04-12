package com.graphhopper.jsprit.examples;

import com.graphhopper.jsprit.core.problem.Location;
import com.graphhopper.jsprit.core.problem.job.Shipment;
import com.graphhopper.jsprit.core.problem.vehicle.Vehicle;
import com.graphhopper.jsprit.core.problem.vehicle.VehicleImpl;
import com.graphhopper.jsprit.core.problem.vehicle.VehicleType;
import com.graphhopper.jsprit.core.problem.vehicle.VehicleTypeImpl;
import com.graphhopper.jsprit.core.util.Coordinate;
import scala.util.parsing.combinator.testing.Str;

import java.util.ArrayList;

/**
 * Created by jiusi on 16/4/6.
 */
public class EnRouteVehicleContext {
    public class ShipmentInfo {
        public double[] pickupLoc;
        public double[] deliverLoc;

        public double[] pickupTimeWindowTS;
        public double[] pickupTimeWindowAlgo;

        public double[] deliverTimeWindowTS;
        public double[] deliverTimeWindowAlgo;

        // customer here is the receiver
        public String customerName;
        public String customerPhone;

        public String pickupAddress;
        public String deliverAddress;


        public ShipmentInfo(double[] pickupLoc, double[] deliverLoc,
                            double[] pickupTimeWindowTS, double[] deliverTimeWindowTS,
                            String customerName, String customerPhone, String pickupAddress, String deliverAddress) {
            this.pickupLoc = pickupLoc;
            this.pickupTimeWindowTS = pickupTimeWindowTS;

            double pstart = pickupTimeWindowTS[0] - currentTimestamp;
            if (pstart < 0) {
                pstart = 0;
            }

            double dstart = deliverTimeWindowTS[0] - currentTimestamp;
            if(dstart < 0) {
                dstart = 0;
            }


            this.pickupTimeWindowAlgo = new double[]{
                pstart,
                pickupTimeWindowTS[1] - currentTimestamp
            };

            this.deliverLoc = deliverLoc;
            this.deliverTimeWindowTS = deliverTimeWindowTS;
            this.deliverTimeWindowAlgo = new double[]{
                dstart,
                deliverTimeWindowTS[1] - currentTimestamp
            };

            this.customerName = customerName;
            this.customerPhone = customerPhone;
            this.pickupAddress = pickupAddress;
            this.deliverAddress = deliverAddress;
        }

    }

    public ArrayList<ShipmentInfo> pickedups;
    public ShipmentInfo onGoingPickUp;
    public ArrayList<ShipmentInfo> plannedShipments;

    public double[] currentLoc;
    public double currentTimestamp;

    public String vehicleId;
    public Vehicle vehicle;

    private static Location loc(Coordinate coordinate) {
        return Location.Builder.newInstance().setCoordinate(coordinate).build();
    }

    public EnRouteVehicleContext(String vehicleId, double currentTimestamp, double[] currentLoc, // some info

                                 // for picked-ups
                                 ArrayList<double[]> pickupLocs, ArrayList<double[]> deliverLocs,
                                 ArrayList<double[]> pickupTimeWindowTSs, ArrayList<double[]> deliverTimeWindowTSs,
                                 ArrayList<String> customerNames, ArrayList<String> customerPhones,
                                 ArrayList<String> pickupAddresses, ArrayList<String> deliverAddresses,

                                 // for on going pickup
                                 double[] pickupLoc, double[] deliverLoc,
                                 double[] pickupTimeWindowTS, double[] deliverTimeWindowTS,
                                 String customerName, String customerPhone,
                                 String pickupAddress, String deliverAddress

    ) {
        // create a vehicle implementation


        VehicleTypeImpl.Builder vehicleTypeBuilder = VehicleTypeImpl.Builder.newInstance("bike")
            .addCapacityDimension(0, 3);
        vehicleTypeBuilder.setCostPerDistance(10.0);
        VehicleType vehicleType = vehicleTypeBuilder.build();

        String staffCurrentCoorStr =  vehicleId + "@[" + currentLoc[0] + "," + currentLoc[1] + "]";
        VehicleImpl.Builder vehicleBuilder = VehicleImpl.Builder.newInstance(staffCurrentCoorStr);
        vehicleBuilder.setStartLocation(loc(Coordinate.newInstance(currentLoc[0], currentLoc[1]))).setReturnToDepot(false);
        vehicleBuilder.setType(vehicleType);
        vehicleBuilder.addSkill(vehicleId); // make sure the going deli will only be taken by the vehicle

        this.vehicle = vehicleBuilder.build();

        // 应该是笛卡尔积但是我他妈懒得写
        assert (pickupLocs.size() == deliverLocs.size() &&
            pickupTimeWindowTSs.size() == deliverTimeWindowTSs.size() &&
            customerNames.size() == customerPhones.size() &&
            pickupLocs.size() == pickupAddresses.size()
        );

        this.vehicleId = vehicleId;
        this.currentTimestamp = currentTimestamp;
        this.currentLoc = currentLoc;

        if(pickupLocs != null) {
            // make picked-up shipments info
            this.pickedups = new ArrayList<ShipmentInfo>();
            for (int i = 0; i < pickupLocs.size(); i++) {

                ShipmentInfo si = new ShipmentInfo(pickupLocs.get(i), deliverLocs.get(i),
                    pickupTimeWindowTSs.get(i), deliverTimeWindowTSs.get(i),
                    customerNames.get(i), customerPhones.get(i), pickupAddresses.get(i), deliverAddresses.get(i)
                );

                this.pickedups.add(si);
            }
        }

        if(pickupLoc != null) {
            // make on going pickup shipment info
            this.onGoingPickUp = new ShipmentInfo(pickupLoc, deliverLoc,
                pickupTimeWindowTS, deliverTimeWindowTS,
                customerName, customerPhone, pickupAddress, deliverAddress
            );

        }


    }

    /*
    the state machine
    1. multiple pick ups
    2. single on going pick up
    3. multiple on going deliver (planned)


    pickedUps contains info of multiple pick ups and on going delivers
    on going pickup is not included in picked ups
     */
}
