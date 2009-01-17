/**************************************************************************************
  	This file is part of OpenSerialDataExplorer.

    OpenSerialDataExplorer is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    OpenSerialDataExplorer is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with OpenSerialDataExplorer.  If not, see <http://www.gnu.org/licenses/>.
****************************************************************************************/
package osde.device.htronic;

import gnu.io.NoSuchPortException;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.bind.JAXBException;

import osde.OSDE;
import osde.config.Settings;
import osde.data.Channels;
import osde.data.Record;
import osde.data.RecordSet;
import osde.device.DeviceConfiguration;
import osde.device.IDevice;
import osde.exception.DataInconsitsentException;
import osde.messages.Messages;
import osde.serial.DeviceSerialPort;
import osde.ui.OpenSerialDataExplorer;

/**
 * AkkuMaster C4 device class implementation
 * @author Winfried Brügmann
 */
public class AkkuMasterC4 extends DeviceConfiguration implements IDevice {
	final static Logger														log									= Logger.getLogger(AkkuMasterC4.class.getName());

	final OpenSerialDataExplorer									application;
	final AkkuMasterC4Dialog											dialog;
	final AkkuMasterC4SerialPort									serialPort;
	final Channels																channels;
	HashMap<String, AkkuMasterCalculationThread>	calculationThreads	= new HashMap<String, AkkuMasterCalculationThread>();

	/**
	 * constructor using the device properties file for initialization
	 * @param deviceProperties file
	 * @throws JAXBException 
	 * @throws FileNotFoundException 
	 * @throws NoSuchPortException 
	 */
	public AkkuMasterC4(String deviceProperties) throws FileNotFoundException, JAXBException {
		super(deviceProperties);
		// initializing the resource bundle for this device
		Messages.setDeviceResourceBundle("osde.device.htronic.messages", Settings.getInstance().getLocale(), this.getClass().getClassLoader()); //$NON-NLS-1$

		this.application = OpenSerialDataExplorer.getInstance();
		this.serialPort = new AkkuMasterC4SerialPort(this, this.application);
		this.dialog = new AkkuMasterC4Dialog(this.application.getShell(), this);
		this.channels = Channels.getInstance();
		this.configureSerialPortMenu(DeviceSerialPort.ICON_SET_OPEN_CLOSE);
	}

	/**
	 * constructor using existing device configuration
	 * @param deviceConfig device configuration
	 * @throws NoSuchPortException 
	 */
	public AkkuMasterC4(DeviceConfiguration deviceConfig) {
		super(deviceConfig);
		// initializing the resource bundle for this device
		Messages.setDeviceResourceBundle("osde.device.htronic.messages", Settings.getInstance().getLocale(), this.getClass().getClassLoader()); //$NON-NLS-1$

		this.application = OpenSerialDataExplorer.getInstance();
		this.serialPort = new AkkuMasterC4SerialPort(this, this.application);
		this.dialog = new AkkuMasterC4Dialog(this.application.getShell(), this);
		this.channels = Channels.getInstance();
		this.configureSerialPortMenu(DeviceSerialPort.ICON_SET_OPEN_CLOSE);
	}

	/**
	 * load the mapping exist between lov file configuration keys and OSDE keys
	 * @param lov2osdMap reference to the map where the key mapping has to be put
	 * @return lov2osdMap same reference as input parameter
	 */
	public HashMap<String, String> getLovKeyMappings(HashMap<String, String> lov2osdMap) {
		//nothing to do here
		return lov2osdMap;
	}

	/**
	 * convert record logview config data to OSDE config keys into records section
	 * @param header reference to header data, contain all key value pairs
	 * @param lov2osdMap reference to the map where the key mapping
	 * @param channelNumber
	 * @return
	 */
	@SuppressWarnings("unused") //$NON-NLS-1$
	public String getConvertedRecordConfigurations(HashMap<String, String> header, HashMap<String, String> lov2osdMap, int channelNumber) {
		return ""; //$NON-NLS-1$
	}

	/**
	 * get LogView data bytes size, as far as known modulo 16 and depends on the bytes received from device 
	 */
	public int getLovDataByteSize() {
		return 55; // 0x33 = 51 + 4 (counter)
	}
	
	/**
	 * add record data size points from LogView data stream to each measurement, if measurement is calculation 0 will be added
	 * adaption from LogView stream data format into the device data buffer format is required
	 * do not forget to call makeInActiveDisplayable afterwords to calualte th emissing data
	 * @param recordSet
	 * @param dataBuffer
	 * @param recordDataSize
	 * @param doUpdateProgressBar
	 * @throws DataInconsitsentException 
	 */
	public synchronized void addConvertedLovDataBufferAsRawDataPoints(RecordSet recordSet, byte[] dataBuffer, int recordDataSize, boolean doUpdateProgressBar) throws DataInconsitsentException {
		int lovDataSize = this.getLovDataByteSize();
		
		//byte[] configurationBuffer	= new byte[14];
		//byte[] measurementsBuffer		= new byte[16];
		byte[] convertDataBuffer = new byte[14 + 16];
		int[] points = new int[this.getNumberOfMeasurements(recordSet.getChannelConfigName())];
		String sThreadId = String.format("%06d", Thread.currentThread().getId());
		int progressCycle = 0;
		if (doUpdateProgressBar) this.application.setProgress(progressCycle, sThreadId);
		
		for (int i = 0; i < recordDataSize; i++) { 
			
//		StringBuilder sb = new StringBuilder();
//		for (byte b : dataBuffer) {
//			sb.append(String.format("%02x", b)).append(" ");
//		}
//		log.log(Level.INFO, sb.toString());
//discharge   			      Ni 12 capa  dis   charge                        disc  charge      hh:mm:ss hh:mm:ss  #                                           line
//	                      Nc    city  charge                              capac capac volt  charge   discharge                                                 counter
//33 00 00 00 51 82 00 03 01 0c 08 98 02 56 02 56 00 3c 61 05 09 02 56 52 03 ca 00 00 07 a3 01 25 19 00 00 00 01 00 3c 0d 0a 20 20 35 38 33 30 00 00 00 00 48 02 00 00             
//offset      51 82 00 03 00 0C 08 98 02 58 02 58                      52 00 02 00 00 0B C7 00 00 0D 00 00 00 01
//charge  				        Ni 12 capa  dis   charge                        disc  charge      hh:mm:ss hh:mm:ss  #                                           line
//		                    Nc    city  charge                              capac capac volt  charge   discharge                                                 counter
//33 00 00 00 51 81 00 03 01 0c 08 98 02 56 02 56 00 3c 61 05 09 02 56 52 03 cb 07 f0 0d 02 01 25 1f 03 17 38 01 00 3c 0d 0a 20 31 38 31 30 30 00 00 00 00 13 07 00 00 
//offset      51 81 00 03 00 0C 08 98 02 58 02 58                      52 02 85 0A 4A 0C DE 01 04 1F 04 17 1B 01

			System.arraycopy(dataBuffer,  4 + i*lovDataSize, convertDataBuffer,  0, 14); //configurationBuffer.length
			System.arraycopy(dataBuffer, 23 + i*lovDataSize, convertDataBuffer, 14, 16); //measurementsBuffer.length
			recordSet.addPoints(convertDataBytes(points, convertDataBuffer), false);
			
			if (doUpdateProgressBar && i % 50 == 0) this.application.setProgress(((++progressCycle*5000)/recordDataSize), sThreadId);
		}
		if (doUpdateProgressBar) this.application.setProgress(100, sThreadId);
	}

	/**
	 * convert the device bytes into raw values, no calculation will take place here, see translateValue reverseTranslateValue
	 * inactive or to be calculated data point are filled with 0 and needs to be handles after words
	 * @param points pointer to integer array to be filled with converted data
	 * @param dataBuffer byte arrax with the data to be converted
	 */
	public int[] convertDataBytes(int[] points, byte[] dataBuffer) {	

		// build the point array according curves from record set
		//int[] points = new int[getRecordSet().size()];
		
		HashMap<String, Object> values = new HashMap<String, Object>(7);
		byte[] configurationBuffer	= new byte[14];
		System.arraycopy(dataBuffer, 0, configurationBuffer, 0, configurationBuffer.length);
		byte[] measurementsBuffer		= new byte[16];
		System.arraycopy(dataBuffer, 14, measurementsBuffer, 0, measurementsBuffer.length);
		values = AkkuMasterC4SerialPort.getConvertedValues(values, AkkuMasterC4SerialPort.convertConfigurationAnswer(configurationBuffer), AkkuMasterC4SerialPort.convertMeasurementValues(measurementsBuffer));


		points[0] = new Integer((Integer) values.get(AkkuMasterC4SerialPort.PROCESS_VOLTAGE)).intValue(); //Spannung 	[mV]
		points[1] = new Integer((Integer) values.get(AkkuMasterC4SerialPort.PROCESS_CURRENT)).intValue(); //Strom 			[mA]
		// display adaption * 1000  -  / 1000
		points[2] = new Integer((Integer) values.get(AkkuMasterC4SerialPort.PROCESS_CAPACITY)).intValue() * 1000; //Kapazität	[mAh] 
		points[3] = new Double((points[0] / 1000.0) * (points[1] / 1000.0) * 1000).intValue(); 							// power U*I [W]
		points[4] = new Double((points[0] / 1000.0) * (points[2] / 1000.0)).intValue();											// energy U*C [mWh]
		log.log(Level.FINE, points[0] + " mV; " + points[1] + " mA; " + points[2] + " mAh; " + points[3] + " mW; " + points[4] + " mWh"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$

		return points;
	}
	
	/**
	 * add record data size points from file stream to each measurement
	 * it is possible to add only none calculation records if makeInActiveDisplayable calculates the rest
	 * do not forget to call makeInActiveDisplayable afterwords to calualte th emissing data
	 * since this is a long term operation the progress bar should be updated to signal busyness to user 
	 * @param recordSet
	 * @param dataBuffer
	 * @param recordDataSize
	 * @param doUpdateProgressBar
	 * @throws DataInconsitsentException 
	 */
	public void addDataBufferAsRawDataPoints(RecordSet recordSet, byte[] dataBuffer, int recordDataSize, boolean doUpdateProgressBar) throws DataInconsitsentException {
		int dataBufferSize = OSDE.SIZE_BYTES_INTEGER * recordSet.getNoneCalculationRecordNames().length;
		byte[] convertBuffer = new byte[dataBufferSize];
		int[] points = new int[recordSet.getRecordNames().length];
		String sThreadId = String.format("%06d", Thread.currentThread().getId());
		int progressCycle = 0;
		if (doUpdateProgressBar) this.application.setProgress(progressCycle, sThreadId);
		
		for (int i = 0; i < recordDataSize; i++) {
			System.arraycopy(dataBuffer, i*dataBufferSize, convertBuffer, 0, dataBufferSize);
			
			points[0] = (((convertBuffer[0]&0xff) << 24) + ((convertBuffer[1]&0xff) << 16) + ((convertBuffer[2]&0xff) << 8) + ((convertBuffer[3]&0xff) << 0));
			points[1] = (((convertBuffer[4]&0xff) << 24) + ((convertBuffer[5]&0xff) << 16) + ((convertBuffer[6]&0xff) << 8) + ((convertBuffer[7]&0xff) << 0));
			points[2] = (((convertBuffer[8]&0xff) << 24) + ((convertBuffer[9]&0xff) << 16) + ((convertBuffer[10]&0xff) << 8) + ((convertBuffer[11]&0xff) << 0));
			points[3] = new Double((points[0] / 1000.0) * (points[1] / 1000.0) * 1000).intValue(); 							// power U*I [W]
			points[4] = new Double((points[0] / 1000.0) * (points[2] / 1000.0)).intValue();											// energy U*C [mWh]
			log.log(Level.FINE, points[0] + " mV; " + points[1] + " mA; " + points[2] + " mAh; " + points[3] + " mW; " + points[4] + " mWh"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
			
			recordSet.addPoints(points, false);
			
			if (doUpdateProgressBar && i % 50 == 0) this.application.setProgress(((++progressCycle*5000)/recordDataSize), sThreadId);
		}
		if (doUpdateProgressBar) this.application.setProgress(100, sThreadId);
	}

	/**
	 * function to translate measured value from a device to values represented
	 * @return double with the adapted value
	 */
	public double translateValue(Record record, double value) { //$NON-NLS-1$
		double newValue = value;
		log.log(Level.FINEST, String.format("input value for %s - %f", record.getName(), value)); //$NON-NLS-1$
		log.log(Level.FINEST, String.format("value calculated for %s - %f", record.getName(), newValue)); //$NON-NLS-1$
		return newValue;
	}

	/**
	 * function to translate measured value from a device to values represented
	 * @return double with the adapted value
	 */
	public double reverseTranslateValue(Record record, double value) { //$NON-NLS-1$
		double newValue = value;
		log.log(Level.FINEST, String.format("input value for %s - %f", record.getName(), value)); //$NON-NLS-1$
		log.log(Level.FINEST, String.format("value calculated for %s - %f", record.getName(), newValue)); //$NON-NLS-1$
		return newValue;
	}

	/**
	 * check and update visibility status of all records according the available device configuration
	 * this function must have only implementation code if the device implementation supports different configurations
	 * where some curves are hided for better overview 
	 * example: if device supports voltage, current and height and no sensors are connected to voltage and current
	 * it makes less sense to display voltage and current curves, if only height has measurement data
	 * at least an update of the graphics window should be included at the end of this method
	 */
	public void updateVisibilityStatus(RecordSet recordSet) {
		log.log(Level.FINE, "no update required for " + recordSet.getName()); //$NON-NLS-1$
	}
	
	/**
	 * function to calculate values for inactive or to be calculated records
	 * @param recordSet
	 */
	public void makeInActiveDisplayable(RecordSet recordSet) {
		// since there are measurement point every 10 seconds during capturing only and the calculation will take place directly switch all to displayable
		if (recordSet.isRaw()) {
			// calculate the values required
			try {
				// 0=Spannung, 1=Strom, 2=Ladung, 3=Leistung, 4=Energie
				String[] recordNames = recordSet.getRecordNames();
				
				String recordKey = recordNames[3]; //3=Leistung/Power
				Record record = recordSet.get(recordKey);
				if (record != null && (record.size() == 0 || (record.getRealMinValue() == 0 && record.getRealMaxValue() == 0))) {
					this.calculationThreads.put(recordKey, new AkkuMasterCalculationThread(recordKey, this.channels.getActiveChannel().getActiveRecordSet()));
					this.calculationThreads.get(recordKey).start();
				}
				recordKey = recordNames[4]; //4=Energie/Energy
				record = recordSet.get(recordKey);
				if (record != null && (record.size() == 0 || (record.getRealMinValue() == 0 && record.getRealMaxValue() == 0))) {
					this.calculationThreads.put(recordKey, new AkkuMasterCalculationThread(recordKey, this.channels.getActiveChannel().getActiveRecordSet()));
					this.calculationThreads.get(recordKey).start();
				}
			}
			catch (RuntimeException e) {
				log.log(Level.SEVERE, e.getMessage(), e);
			}
		}
	}

	/**
	 * @return the device dialog
	 */
	public AkkuMasterC4Dialog getDialog() {
		return this.dialog;
	}

	/**
	 * @return the device serialPort
	 */
	public AkkuMasterC4SerialPort getSerialPort() {
		return this.serialPort;
	}

	/**
	 * query for all the property keys this device has in use
	 * - the property keys are used to filter serialized properties form OSD data file
	 * @return [offset, factor, reduction, number_cells, prop_n100W, ...]
	 */
	public String[] getUsedPropertyKeys() {
		return new String[0];
	}
	
	/**
	 * method toggle open close serial port or start/stop gathering data from device
	 */
	public void openCloseSerialPort() {
		if (this.serialPort != null) {
			if (!this.serialPort.isConnected()) {
				try {
					this.serialPort.open();
				}
				catch (Exception e) {
					log.log(Level.SEVERE, e.getMessage(), e);
					this.application.openMessageDialog(Messages.getString(osde.messages.MessageIds.OSDE_MSGE0015, new Object[] {e.getClass().getSimpleName() + OSDE.STRING_MESSAGE_CONCAT + e.getMessage() } ));
				}
			}
			else {
				this.serialPort.close();
			}
		}
	}	
}
