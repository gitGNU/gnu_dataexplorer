/**************************************************************************************
  	This file is part of OpenSerialdataExplorer.

    OpenSerialdataExplorer is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    OpenSerialdataExplorer is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with OpenSerialdataExplorer.  If not, see <http://www.gnu.org/licenses/>.
****************************************************************************************/
package osde.device.wb;

import gnu.io.NoSuchPortException;

import java.io.FileNotFoundException;
import java.util.logging.Logger;

import javax.xml.bind.JAXBException;

import osde.data.RecordSet;
import osde.device.DeviceConfiguration;
import osde.device.IDevice;
import osde.ui.OpenSerialDataExplorer;

/**
 * Sample and test device implementation, scale adjustment, ...
 * @author Winfried Brügmann
 */
public class Simulator extends DeviceConfiguration implements IDevice {
	private Logger												log	= Logger.getLogger(this.getClass().getName());

	private final OpenSerialDataExplorer application;
	private final SimulatorSerialPort serialPort;
	private final SimulatorDialog	dialog;
	
	/**
	 * constructor using properties file
	 * @param deviceProperties
	 * @throws JAXBException 
	 * @throws FileNotFoundException 
	 * @throws NoSuchPortException 
	 */
	public Simulator(String deviceProperties) throws FileNotFoundException, JAXBException, NoSuchPortException {
		super(deviceProperties);
		this.application = OpenSerialDataExplorer.getInstance();
		this.serialPort = new SimulatorSerialPort(this, application);
		this.dialog = new SimulatorDialog(this.application.getShell(), this);
	}

	/**
	 * constructor using existing device configuration
	 * @param deviceConfig device configuration
	 * @throws NoSuchPortException 
	 */
	public Simulator(DeviceConfiguration deviceConfig) throws NoSuchPortException {
		super(deviceConfig);
		this.application = OpenSerialDataExplorer.getInstance();
		this.serialPort = new SimulatorSerialPort(this, application);
		this.dialog = new SimulatorDialog(this.application.getShell(), this);
	}

	/**
	 * function to translate measured values from a device to values represented
	 * this function should be over written by device and measurement specific algorithm
	 * @return double of device dependent value
	 */
	public double translateValue(String channelConfigKey, String recordKey, double value) {
		double newValues = this.getMeasurementOffset(channelConfigKey, recordKey) + this.getMeasurementFactor(channelConfigKey, recordKey) * value;
		log.fine("newValue = " + newValues);
		// do some calculation
		return newValues;
	}

	/**
	 * function to reverse translate measured values from a device to values represented
	 * this function should be over written by device and measurement specific algorithm
	 * @return double of device dependent value
	 */
	public double reverseTranslateValue(String channelConfigKey, String recordKey, double value) {
		double newValues = value / this.getMeasurementFactor(channelConfigKey, recordKey) - this.getMeasurementOffset(channelConfigKey, recordKey);
		// do some calculation
		return newValues;
	}
		
	/**
	 * function to calculate values for inactive records, data not readable from device
	 * if calculation is done during data gathering this can be a loop switching all records to displayable
	 * for calculation which requires more effort or is time consuming it can call a background thread, 
	 * target is to make sure all data point not coming from device directly are available and can be displayed 
	 */
	public void makeInActiveDisplayable(RecordSet recordSet) {
		//add implementation where data point are calculated
		//do not forget to make record displayable -> record.setDisplayable(true);
	}

	/**
	 * @return the dialog
	 */
	public SimulatorDialog getDialog() {
		return dialog;
	}

	/**
	 * @return the serialPort
	 */
	public SimulatorSerialPort getSerialPort() {
		return serialPort;
	}

}
