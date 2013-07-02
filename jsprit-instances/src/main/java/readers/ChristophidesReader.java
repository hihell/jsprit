/*******************************************************************************
 * Copyright (C) 2013  Stefan Schroeder
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 * 
 * Contributors:
 *     Stefan Schroeder - initial API and implementation
 ******************************************************************************/
package readers;


import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import org.apache.log4j.Logger;

import readers.ChristophidesReader;
import util.Coordinate;

import basics.route.Vehicle;
import basics.route.VehicleImpl;
import basics.route.VehicleType;
import basics.route.VehicleTypeImpl;
import basics.Service;
import basics.VehicleRoutingProblem;
import basics.VehicleRoutingProblem.FleetSize;

/**
 * Reader that reads Christophides, Mingozzi and Toth instances.
 * 
 * <p>Files and file-description can be found <a href="http://neo.lcc.uma.es/vrp/vrp-instances/capacitated-vrp-instances/">here</a>.
 * 
 * @author stefan schroeder
 *
 */
public class ChristophidesReader {

	private static Logger logger = Logger.getLogger(ChristophidesReader.class);
	
	private final VehicleRoutingProblem.Builder vrpBuilder;

	private double coordProjectionFactor = 1;

	/**
	 * Constructs the reader.
	 * 
	 * @param vrpBuilder
	 */
	public ChristophidesReader(VehicleRoutingProblem.Builder vrpBuilder) {
		super();
		this.vrpBuilder = vrpBuilder;
	}
	
	/**
	 * Reads instance-file and memorizes vehicles, customers and so forth in
	 * {@link VehicleRoutingProblem.Builder}.
	 * 
	 * @param fileName
	 */
	public void read(String fileName){
		vrpBuilder.setFleetSize(FleetSize.INFINITE);
		BufferedReader reader = getReader(fileName);
		int vehicleCapacity = 0;
		double serviceTime = 0.0;
		double endTime = Double.MAX_VALUE;
		int counter = 0;
		String line = null;
		while((line = readLine(reader)) != null){
			line = line.replace("\r", "");
			line = line.trim();
			String[] tokens = line.split(" ");
			if(counter == 0){
				vehicleCapacity = Integer.parseInt(tokens[1].trim());
				endTime = Double.parseDouble(tokens[2].trim());
				serviceTime = Double.parseDouble(tokens[3].trim());
			}
			else if(counter == 1){
				Coordinate depotCoord = makeCoord(tokens[0].trim(),tokens[1].trim());
				VehicleTypeImpl vehicleType = VehicleTypeImpl.Builder.newInstance("christophidesType", vehicleCapacity).
						setCostPerDistance(1.0).build();
				Vehicle vehicle = VehicleImpl.Builder.newInstance("christophidesVehicle").setLatestArrival(endTime).setLocationCoord(depotCoord).
						setType(vehicleType).build();
				vrpBuilder.addVehicle(vehicle);
			}
			else{
				Coordinate customerCoord = makeCoord(tokens[0].trim(),tokens[1].trim());
				int demand = Integer.parseInt(tokens[2].trim());
				String customer = Integer.valueOf(counter-1).toString();
				Service service = Service.Builder.newInstance(customer, demand).setServiceTime(serviceTime).setCoord(customerCoord).build();
				vrpBuilder.addService(service);
			}
			counter++;
		}
		close(reader);
	}

	public void setCoordProjectionFactor(double coordProjectionFactor) {
		this.coordProjectionFactor = coordProjectionFactor;
	}

	private void close(BufferedReader reader)  {
		try {
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
			logger.error(e);
			System.exit(1);
		}
	}

	private String readLine(BufferedReader reader) {
		try {
			return reader.readLine();
		} catch (IOException e) {
			e.printStackTrace();
			logger.error(e);
			System.exit(1);
			return null;
		}
	}
	
	private Coordinate makeCoord(String xString, String yString) {
		double x = Double.parseDouble(xString);
		double y = Double.parseDouble(yString);
		return new Coordinate(x*coordProjectionFactor,y*coordProjectionFactor);
	}

	private BufferedReader getReader(String solomonFile) {
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(solomonFile));
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
			logger.error(e1);
			System.exit(1);
		}
		return reader;
	}
}
