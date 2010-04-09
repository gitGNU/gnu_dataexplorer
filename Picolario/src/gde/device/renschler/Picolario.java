/**************************************************************************************
  	This file is part of GNU DataExplorer.

    GNU DataExplorer is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    GNU DataExplorer is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with GNU DataExplorer.  If not, see <http://www.gnu.org/licenses/>.
****************************************************************************************/
package gde.device.renschler;

import java.io.FileNotFoundException;
import java.util.HashMap;
import gde.log.Level;
import java.util.logging.Logger;

import javax.xml.bind.JAXBException;

import gde.DE;
import gde.config.Settings;
import gde.data.Channels;
import gde.data.Record;
import gde.data.RecordSet;
import gde.device.DeviceConfiguration;
import gde.device.IDevice;
import gde.device.PropertyType;
import gde.exception.DataInconsitsentException;
import gde.messages.Messages;
import gde.serial.DeviceSerialPort;
import gde.ui.DataExplorer;
import gde.utils.CalculationThread;
import gde.utils.LinearRegression;
import gde.utils.QuasiLinearRegression;

/**
 * Picolariolog device main implementaion class
 * @author Winfried Brügmann
 */
public class Picolario extends DeviceConfiguration implements IDevice {
	final static String						$CLASS_NAME				=	Picolario.class.getName();
	final static Logger						log								= Logger.getLogger($CLASS_NAME);

	public final static String		DO_NO_ADAPTION		= "do_no_adaption"; //$NON-NLS-1$
	public final static String		DO_OFFSET_HEIGHT	= "do_offset_height"; //$NON-NLS-1$
	public final static String		DO_SUBTRACT_FIRST	= "do_subtract_first"; //$NON-NLS-1$
	public final static String		DO_SUBTRACT_LAST	= "subtract_last"; //$NON-NLS-1$

	final DataExplorer	application;
	final PicolarioDialog					dialog;
	final PicolarioSerialPort			serialPort;
	final Channels								channels;

	/**
	 * @param iniFile
	 * @throws JAXBException 
	 * @throws FileNotFoundException 
	 */
	public Picolario(String iniFile) throws FileNotFoundException, JAXBException {
		super(iniFile);
		// initializing the resource bundle for this device
		Messages.setDeviceResourceBundle("de.device.renschler.messages", Settings.getInstance().getLocale(), this.getClass().getClassLoader()); //$NON-NLS-1$

		this.application = DataExplorer.getInstance();
		this.serialPort = new PicolarioSerialPort(this, this.application);
		this.dialog = new PicolarioDialog(this.application.getShell(), this);
		this.channels = Channels.getInstance();
		if (this.application.getMenuToolBar() != null) this.configureSerialPortMenu(DeviceSerialPort.ICON_SET_OPEN_CLOSE);
	}

	/**
	 * constructor using existing device configuration
	 * @param deviceConfig device configuration
	 */
	public Picolario(DeviceConfiguration deviceConfig) {
		super(deviceConfig);
		// initializing the resource bundle for this device
		Messages.setDeviceResourceBundle("de.device.renschler.messages", Settings.getInstance().getLocale(), this.getClass().getClassLoader()); //$NON-NLS-1$

		this.application = DataExplorer.getInstance();
		this.serialPort = new PicolarioSerialPort(this, this.application);
		this.dialog = new PicolarioDialog(this.application.getShell(), this);
		this.channels = Channels.getInstance();
		this.configureSerialPortMenu(DeviceSerialPort.ICON_SET_OPEN_CLOSE);
	}
	
	/**
	 * query the default stem used as record set name
	 * @return recordSetStemName
	 */
	public String getRecordSetStemName() {
		return Messages.getString(MessageIds.GDE_MSGT1220);
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
	 * convert record LogView config data to OSDE config keys into records section
	 * @param header reference to header data, contain all key value pairs
	 * @param lov2osdMap reference to the map where the key mapping
	 * @param channelNumber 
	 * @return converted configuration data
	 */
	public String getConvertedRecordConfigurations(HashMap<String, String> header, HashMap<String, String> lov2osdMap, int channelNumber) {
		return ""; //$NON-NLS-1$
	}

	/**
	 * get LogView data bytes size, as far as known modulo 16 and depends on the bytes received from device 
	 */
	public int getLovDataByteSize() {
		return 16;  // 0x0C = 12 + 4 (counter)
	}

	/**
	 * add record data size points from LogView data stream to each measurement, if measurement is calculation 0 will be added
	 * adaption from LogView stream data format into the device data buffer format is required
	 * do not forget to call makeInActiveDisplayable afterwords to calualte th emissing data
	 * since this is a long term operation the progress bar should be updated to signal busyness to user 
	 * @param recordSet
	 * @param dataBuffer
	 * @param recordDataSize
	 * @param doUpdateProgressBar
	 * @throws DataInconsitsentException 
	 */
	public void addConvertedLovDataBufferAsRawDataPoints(RecordSet recordSet, byte[] dataBuffer, int recordDataSize, boolean doUpdateProgressBar) throws DataInconsitsentException {
		int offset = 4;
		int size = this.getLovDataByteSize();
		int deviceDataBufferSize = 3; // meight not equal as this.getNumberOfMeasurements(recordSet.getChannelConfigName());
		byte[] convertBuffer = new byte[deviceDataBufferSize];
		int[] points = new int[deviceDataBufferSize];
		String sThreadId = String.format("%06d", Thread.currentThread().getId());
		int progressCycle = 0;
		if (doUpdateProgressBar) this.application.setProgress(progressCycle, sThreadId);
	
		for (int i = 0; i < recordDataSize; i++) { 
			System.arraycopy(dataBuffer, offset + i*size, convertBuffer, 0, deviceDataBufferSize);
			recordSet.addPoints(convertDataBytes(points, convertBuffer));
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
		// add voltage U = 2.5 + (byte3 - 45) * 0.0532 - no calculation take place here - refer to translateValue/reverseTranslateValue
		points[0] = Integer.valueOf(dataBuffer[2]) * 1000;

		// calculate height values and add
		if (((dataBuffer[1] & 0x80) >> 7) == 0) // we have signed [feet]
			points[1] = ((dataBuffer[0] & 0xFF) + ((dataBuffer[1] & 0x7F) << 8)) * 1000; // only positive part of height data
		else
			points[1] = (((dataBuffer[0] & 0xFF) + ((dataBuffer[1] & 0x7F) << 8)) * -1) * 1000; // height is negative

		return points;
	}
	
	/**
	 * add record data size points from file stream to each measurement
	 * it is possible to add only none calculation records if makeInActiveDisplayable calculates the rest
	 * do not forget to call makeInActiveDisplayable afterwords to calualte the emissing data
	 * since this is a long term operation the progress bar should be updated to signal busyness to user 
	 * @param recordSet
	 * @param dataBuffer
	 * @param recordDataSize
	 * @param doUpdateProgressBar
	 */
	public void addDataBufferAsRawDataPoints(RecordSet recordSet, byte[] dataBuffer, int recordDataSize, boolean doUpdateProgressBar) throws DataInconsitsentException {
		int dataBufferSize = GDE.SIZE_BYTES_INTEGER * recordSet.getNoneCalculationRecordNames().length;
		byte[] convertBuffer = new byte[dataBufferSize];
		int[] points = new int[recordSet.getRecordNames().length];
		String sThreadId = String.format("%06d", Thread.currentThread().getId());
		int progressCycle = 0;
		if (doUpdateProgressBar) this.application.setProgress(progressCycle, sThreadId);
		
		for (int i = 0; i < recordDataSize; i++) {
			System.arraycopy(dataBuffer, i*dataBufferSize, convertBuffer, 0, dataBufferSize);
			
			points[0] = (((convertBuffer[0]&0xff) << 24) + ((convertBuffer[1]&0xff) << 16) + ((convertBuffer[2]&0xff) << 8) + ((convertBuffer[3]&0xff) << 0));
			points[1] = (((convertBuffer[4]&0xff) << 24) + ((convertBuffer[5]&0xff) << 16) + ((convertBuffer[6]&0xff) << 8) + ((convertBuffer[7]&0xff) << 0));
			
			recordSet.addPoints(points);
			
			if (doUpdateProgressBar && i % 50 == 0) this.application.setProgress(((++progressCycle*5000)/recordDataSize), sThreadId);
		}
		if (doUpdateProgressBar) this.application.setProgress(100, sThreadId);
	}

	/**
	 * function to prepare a data table row of record set while translating available measurement values
	 * @return pointer to filled data table row with formated values
	 */
	public int[] prepareDataTableRow(RecordSet recordSet, int rowIndex) {
		int[] dataTableRow = new int[recordSet.size()+1]; // this.device.getMeasurementNames(this.channelNumber).length
		try {
			String[] recordNames = recordSet.getRecordNames();  // 0=Spannung, 1=Höhe, 2=Steigung
			int numberRecords = recordNames.length;			

			dataTableRow[0] = (int)recordSet.getTime_ms(rowIndex);
			for (int j = 0; j < numberRecords; j++) {
				Record record = recordSet.get(recordNames[j]);
				double offset = record.getOffset(); // != 0 if curve has an defined offset
				double reduction = record.getReduction();
				double factor = record.getFactor(); // != 1 if a unit translation is required
				
				switch (j) { // 0=Spannung, 1=Höhe, 2=Steigung
				case 0: //Spannung/Voltage
					break;
				case 1: //Höhe/Height
					PropertyType property = record.getProperty(Picolario.DO_SUBTRACT_FIRST);
					boolean subtractFirst = property != null ? Boolean.valueOf(property.getValue()).booleanValue() : false;
					property = record.getProperty(Picolario.DO_SUBTRACT_LAST);
					boolean subtractLast = property != null ? Boolean.valueOf(property.getValue()).booleanValue() : false;
					
					if (subtractFirst) {
						reduction = record.getFirst()/1000.0;
					}
					else if (subtractLast) {
						reduction = record.getLast()/1000.0;
					}
					else {
						reduction = 0;
					}
					break;
				case 2: //Steigung/Slope
					factor = recordSet.get(recordNames[1]).getFactor(); // 1=height
					break;
				default:
					log.log(Level.WARNING, "exceed known record names"); //$NON-NLS-1$
					break;
				}
				
				dataTableRow[j+1] = Double.valueOf((offset + ((record.get(rowIndex)/1000.0) - reduction) * factor) * 1000.0).intValue();				
			}
		}
		catch (RuntimeException e) {
			log.log(Level.SEVERE, e.getMessage(), e);
		}
		return dataTableRow;		
	}

	/**
	 * function to translate measured value from a device to values represented (((value - reduction) * factor) + offset - firstLastAdaption)
	 * @return double with the adapted value
	 */
	public double translateValue(Record record, double value) {
		final String $METHOD_NAME = "translateValue()";
		log.log(Level.FINEST, String.format("input value for %s - %f", record.getName(), value)); //$NON-NLS-1$

		String recordKey = "?"; //$NON-NLS-1$
		double newValue = 0.0;
		try {
			// 0=Spannung, 1=Höhe, 2=Steigung
			recordKey = record.getName();
			double offset = record.getOffset(); // != 0 if curve has an defined offset
			double reduction = record.getReduction();
			double factor = record.getFactor(); // != 1 if a unit translation is required

			// height calculation need special procedure
			if (record.getOrdinal() == 1) { // 1=Höhe
				PropertyType property = record.getProperty(Picolario.DO_SUBTRACT_FIRST);
				boolean subtractFirst = property != null ? Boolean.valueOf(property.getValue()).booleanValue() : false;
				property = record.getProperty(Picolario.DO_SUBTRACT_LAST);
				boolean subtractLast = property != null ? Boolean.valueOf(property.getValue()).booleanValue() : false;

				try {
					if (subtractFirst) {
						reduction = record.getFirst().intValue() / 1000.0;
					}
					else if (subtractLast) {
						reduction = record.getLast().intValue() / 1000.0;
					}
				}
				catch (Throwable e) {
					log.log(Level.SEVERE, record.getParent().getName() + " " + record.getName() + " " + e.getMessage() + " " + $CLASS_NAME + "." + $METHOD_NAME);
				}
			}

			// slope calculation needs height factor for calculation
			else if (record.getOrdinal() == 2) { // 2=slope
				factor = this.getMeasurementFactor(record.getParent().getChannelConfigNumber(), 1); // 1=height
			}

			newValue = offset + (value - reduction) * factor;
		}
		catch (RuntimeException e) {
			log.log(Level.SEVERE, e.getMessage(), e);
		}

		log.log(Level.FINER, String.format("value calculated for %s - inValue %f - outValue %f", recordKey, value, newValue)); //$NON-NLS-1$
		return newValue;
	}

	/**
	 * function to translate measured value from a device to values represented (((value - offset + firstLastAdaption)/factor) + reduction)
	 * @return double with the adapted value
	 */
	public double reverseTranslateValue(Record record, double value) {
		log.log(Level.FINEST, String.format("input value for %s - %f", record.getName(), value)); //$NON-NLS-1$
		final String $METHOD_NAME = "reverseTranslateValue()";

		// 0=Spannung, 1=Höhe, 2=Steigung
		String recordKey = record.getName();
		double offset = record.getOffset(); // != 0 if curve has an defined offset
		double reduction = record.getReduction();
		double factor = record.getFactor(); // != 1 if a unit translation is required

		// height calculation need special procedure
		if (record.getOrdinal() == 1) { // 1=Höhe
			PropertyType property = record.getProperty(Picolario.DO_SUBTRACT_FIRST);
			boolean subtractFirst = property != null ? Boolean.valueOf(property.getValue()).booleanValue() : false;
			property = record.getProperty(Picolario.DO_SUBTRACT_LAST);
			boolean subtractLast = property != null ? Boolean.valueOf(property.getValue()).booleanValue() : false;

			try {
				if (subtractFirst) {
					reduction = record.getFirst().intValue() / 1000.0;
				}
				else if (subtractLast) {
					reduction = record.getLast().intValue() / (double)1000;
				}
			}
			catch (Throwable e) {
				log.log(Level.SEVERE, record.getParent().getName() + " " + record.getName() + " " + e.getMessage() + " " + $CLASS_NAME + "." + $METHOD_NAME);
			}
		}

		// slope calculation needs height factor for calculation
		else if (record.getOrdinal() == 2) { // 2=slope
			factor = this.getMeasurementFactor(record.getParent().getChannelConfigNumber(), 1); // 1=height
		}

		double newValue = (value - offset) / factor + reduction;

		log.log(Level.FINER, String.format("new value calculated for %s - inValue %f - outValue %f", recordKey, value, newValue)); //$NON-NLS-1$
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
		recordSet.setAllVisibleAndDisplayable();
	}

	/**
	 * function to calculate values for inactive and to be calculated records
	 */
	public void makeInActiveDisplayable(RecordSet recordSet) {
		// since there are measurement point every 10 seconds during capturing only and the calculation will take place directly switch all to displayable
		if (recordSet.isRaw() && recordSet.isRecalculation()) {
			String[] measurements = recordSet.getRecordNames(); // 0=Spannung, 1=Höhe, 2=Steigrate
			// calculate the values required		
			Record slopeRecord = recordSet.get(measurements[2]);
			slopeRecord.setDisplayable(false);
			PropertyType property = slopeRecord.getProperty(CalculationThread.REGRESSION_INTERVAL_SEC);
			int regressionInterval = property != null ? new Integer(property.getValue()) : 10;
			property = slopeRecord.getProperty(CalculationThread.REGRESSION_TYPE);
			if (property == null || property.getValue().equals(CalculationThread.REGRESSION_TYPE_CURVE))
				this.calculationThread = new QuasiLinearRegression(recordSet, measurements[1], measurements[2], regressionInterval);
			else
				this.calculationThread = new LinearRegression(recordSet, measurements[1], measurements[2], regressionInterval);

			try {
				this.calculationThread.start();
			}
			catch (RuntimeException e) {
				log.log(Level.WARNING, e.getMessage(), e);
			}
		}
	}

	/**
	 * query for all the property keys this device has in use
	 * - the property keys are used to filter serialized properties form OSD data file
	 * @return [offset, factor, reduction, number_cells, prop_n100W, ...]
	 */
	public String[] getUsedPropertyKeys() {
		return new String[] {	IDevice.OFFSET, IDevice.FACTOR, IDevice.REDUCTION, 
				DO_NO_ADAPTION, DO_OFFSET_HEIGHT, DO_SUBTRACT_FIRST, DO_SUBTRACT_LAST,
				CalculationThread.REGRESSION_INTERVAL_SEC, CalculationThread.REGRESSION_TYPE};
	}

	/**
	 * @return the dialog
	 */
	public PicolarioDialog getDialog() {
		return this.dialog;
	}

	/**
	 * @return the serialPort
	 */
	public PicolarioSerialPort getSerialPort() {
		return this.serialPort;
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
					this.application.openMessageDialog(this.dialog.getDialogShell(), Messages.getString(de.messages.MessageIds.GDE_MSGE0025, new Object[] {e.getClass().getSimpleName(), e.getMessage() } ));
				}
			}
			else {
				this.serialPort.close();
			}
		}
	}
	
	/**
	 * set the measurement ordinal of the values displayed in cell voltage window underneath the cell voltage bars
	 * set value of -1 to suppress this measurement
	 */
	public int[] getCellVoltageOrdinals() {
		return new int[] {-1, -1};
	}
}
