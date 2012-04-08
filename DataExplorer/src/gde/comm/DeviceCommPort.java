/**************************************************************************************
  	This file is part of GNU DataExplorer.

    GNU DataExplorer is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    DataExplorer is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with GNU DataExplorer.  If not, see <http://www.gnu.org/licenses/>.
    
    Copyright (c) 2008,2009,2010,2011,2012 Winfried Bruegmann
****************************************************************************************/
package gde.comm;

import gde.config.Settings;
import gde.device.DeviceConfiguration;
import gde.device.IDevice;
import gde.exception.ApplicationConfigurationException;
import gde.exception.FailedQueryException;
import gde.exception.SerialPortException;
import gde.exception.TimeOutException;
import gde.ui.DataExplorer;
import gnu.io.SerialPort;

import java.io.IOException;
import java.util.Vector;

/**
 * @author brueg
 *
 */
public class DeviceCommPort implements IDeviceCommPort {
	final static String 				$CLASS_NAME 			= DeviceCommPort.class.getName();

	final protected Settings							settings;
	final protected IDevice								device;
	final protected DeviceConfiguration		deviceConfig;
	final protected DataExplorer					application;
	final protected IDeviceCommPort				port;

	public static final int ICON_SET_OPEN_CLOSE = 0;
	public static final int ICON_SET_START_STOP = 1;
	public static final int ICON_SET_IMPORT_CLOSE = 2;
	
	public boolean				isInterruptedByUser	= false;	

	/**
	 * constructor of default implementation
	 * @param currentDeviceConfig - required by super class to initialize the serial communication port
	 * @param currentApplication - may be used to reflect serial receive,transmit on/off status or overall status by progress bar 
	 */
	public DeviceCommPort(IDevice currentDevice, DataExplorer currentApplication) {
		this.device = currentDevice;
		this.deviceConfig = currentDevice.getDeviceConfiguration();
		this.application = currentApplication;
		this.settings = Settings.getInstance();
		if (Boolean.parseBoolean(System.getProperty("GDE_IS_SIMULATION"))) {
			this.port = new DeviceSerialPortSimulatorImpl(this.device, this.application, this.device.getTimeStep_ms() > 0, (this.device.getTimeStep_ms() < 0 
						? (System.getProperty("GDE_SIMULATION_TIME_STEP_MSEC") != null ? Integer.parseInt(System.getProperty("GDE_SIMULATION_TIME_STEP_MSEC")) : 100) 
						: (System.getProperty("GDE_SIMULATION_TIME_STEP_MSEC") != null ? Integer.parseInt(System.getProperty("GDE_SIMULATION_TIME_STEP_MSEC")) : (int)this.device.getTimeStep_ms())));
		}
		else {
			this.port = new DeviceSerialPortImpl(this.deviceConfig, this.application);
		}
	}
	
	/**
	 * constructor to enable serial port tests without complete device construction
	 * @param currentDeviceConfig - required by super class to initialize the serial communication port
	 * @param currentApplication - may be used to reflect serial receive,transmit on/off status or overall status by progress bar 
	 */
	public DeviceCommPort(DeviceConfiguration deviceConfiguration) {
		this.device = null;
		this.deviceConfig = deviceConfiguration;
		this.application = null;
		this.settings = Settings.getInstance();
		if (Boolean.parseBoolean(System.getProperty("GDE_IS_SIMULATION"))) {
			this.port = new DeviceSerialPortSimulatorImpl(null, this.application, this.deviceConfig.getTimeStep_ms() > 0, (this.deviceConfig.getTimeStep_ms() < 0 
						? (System.getProperty("GDE_SIMULATION_TIME_STEP_MSEC") != null ? Integer.parseInt(System.getProperty("GDE_SIMULATION_TIME_STEP_MSEC")) : 100) 
						: (System.getProperty("GDE_SIMULATION_TIME_STEP_MSEC") != null ? Integer.parseInt(System.getProperty("GDE_SIMULATION_TIME_STEP_MSEC")) : (int)this.deviceConfig.getTimeStep_ms())));
		}
		else {
			this.port = new DeviceSerialPortImpl(this.deviceConfig, this.application);
		}
	}

	/* (non-Javadoc)
	 * @see gde.serial.IDeviceCommPort#open()
	 */
	public SerialPort open() throws ApplicationConfigurationException, SerialPortException {
		return this.port.open();
	}

	/* (non-Javadoc)
	 * @see gde.serial.IDeviceCommPort#close()
	 */
	public void close() {
		this.port.close();	
	}

	/* (non-Javadoc)
	 * @see gde.serial.IDeviceCommPort#read(byte[], int)
	 */
	public byte[] read(byte[] readBuffer, int timeout_msec) throws IOException, TimeOutException {
		return this.port.read(readBuffer, timeout_msec);
	}

	/**
	 * read number of given bytes by the length of the referenced read buffer in a given time frame defined by time out value
	 * @param readBuffer
	 * @param timeout_msec
	 * @param checkFailedQuery
	 * @return the red byte array
	 * @throws IOException
	 * @throws TimeOutException
	 */
	public synchronized byte[] read(byte[] readBuffer, int timeout_msec, boolean checkFailedQuery) throws IOException, FailedQueryException, TimeOutException {
		return this.port.read(readBuffer, timeout_msec, checkFailedQuery);
	}

	/* (non-Javadoc)
	 * @see gde.serial.IDeviceCommPort#read(byte[], int, int)
	 */
	public byte[] read(byte[] readBuffer, int timeout_msec, int stableIndex) throws IOException, TimeOutException {
		return this.port.read(readBuffer, timeout_msec, stableIndex);
	}

	/* (non-Javadoc)
	 * @see gde.serial.IDeviceCommPort#read(byte[], int, java.util.Vector)
	 */
	public byte[] read(byte[] readBuffer, int timeout_msec, Vector<Long> waitTimes) throws IOException, TimeOutException {
		return this.port.read(readBuffer, timeout_msec, waitTimes);
	}

	/* (non-Javadoc)
	 * @see gde.serial.IDeviceCommPort#write(byte[])
	 */
	public void write(byte[] writeBuffer) throws IOException {
		this.port.write(writeBuffer);
	}

	/* (non-Javadoc)
	 * @see gde.serial.IDeviceCommPort#write(byte[])
	 */
	public void write(byte[] writeBuffer, long gap_ms) throws IOException {
		this.port.write(writeBuffer, gap_ms);
	}

	/* (non-Javadoc)
	 * @see gde.serial.IDeviceCommPort#write(byte[])
	 */
	public int cleanInputStream() throws IOException {
		return this.port.cleanInputStream();
	}

	/* (non-Javadoc)
	 * @see gde.serial.IDeviceCommPort#wait4Bytes(int)
	 */
	public long wait4Bytes(int timeout_msec) throws InterruptedException, TimeOutException, IOException {
		return this.port.wait4Bytes(timeout_msec);
	}

	/* (non-Javadoc)
	 * @see gde.serial.IDeviceCommPort#wait4Bytes(int, int)
	 */
	public int wait4Bytes(int numBytes, int timeout_msec) throws IOException {
		return this.port.wait4Bytes(numBytes, timeout_msec);
	}

	/* (non-Javadoc)
	 * @see gde.serial.IDeviceCommPort#waitForStableReceiveBuffer(int, int, int)
	 */
	public int waitForStableReceiveBuffer(int expectedBytes, int timeout_msec, int stableIndex) throws InterruptedException, TimeOutException, IOException {
		return this.port.waitForStableReceiveBuffer(expectedBytes, timeout_msec, stableIndex);
	}

	/* (non-Javadoc)
	 * @see gde.serial.IDeviceCommPort#isConnected()
	 */
	public boolean isConnected() {
		return this.port.isConnected();
	}

	/* (non-Javadoc)
	 * @see gde.serial.IDeviceCommPort#getXferErrors()
	 */
	public int getXferErrors() {
		return this.port.getXferErrors();
	}

	/* (non-Javadoc)
	 * @see gde.serial.IDeviceCommPort#addXferError()
	 */
	public void addXferError() {
		this.port.addXferError();
	}

	/* (non-Javadoc)
	 * @see gde.serial.IDeviceCommPort#getXferErrors()
	 */
	public int getTimeoutErrors() {
		return this.port.getTimeoutErrors();
	}

	/* (non-Javadoc)
	 * @see gde.serial.IDeviceCommPort#addXferError()
	 */
	public void addTimeoutError() {
		this.port.addTimeoutError();
	}

	/* (non-Javadoc)
	 * @see gde.serial.IDeviceCommPort#isMatchAvailablePorts(java.lang.String)
	 */
	public boolean isMatchAvailablePorts(String newSerialPortStr) {
		return this.port.isMatchAvailablePorts(newSerialPortStr);
	}

	/* (non-Javadoc)
	 * @see gde.serial.IDeviceSerialPort#waitForStableReceiveBuffer(int, int, int)
	 */
	public int getAvailableBytes() throws IOException {
		return this.port.getAvailableBytes();
	}

	/**
	 * @param setInterruptedByUser the isInterruptedByUser to set
	 */
	public synchronized void setInterruptedByUser(boolean setInterruptedByUser) {
		this.isInterruptedByUser = setInterruptedByUser;
	}
}
