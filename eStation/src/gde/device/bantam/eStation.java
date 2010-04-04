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
package osde.device.bantam;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Vector;

import osde.log.Level;
import java.util.logging.Logger;

import javax.xml.bind.JAXBException;

import osde.DE;
import osde.config.Settings;
import osde.data.Channel;
import osde.data.Channels;
import osde.data.Record;
import osde.data.RecordSet;
import osde.device.DeviceConfiguration;
import osde.device.IDevice;
import osde.exception.ApplicationConfigurationException;
import osde.exception.DataInconsitsentException;
import osde.exception.SerialPortException;
import osde.io.LogViewReader;
import osde.messages.Messages;
import osde.serial.DeviceSerialPort;
import osde.ui.DataExplorer;

/**
 * eStation base device class
 * @author Winfried Brügmann
 */
public class eStation extends DeviceConfiguration implements IDevice {
	final static Logger						log	= Logger.getLogger(eStation.class.getName());
	
	public final	String[]	USAGE_MODE;
	public final	String[]	ACCU_TYPES;

	public final static String		CONFIG_EXT_TEMP_CUT_OFF			= "ext_temp_cut_off"; //$NON-NLS-1$
	public final static String		CONFIG_WAIT_TIME						= "wait_time"; //$NON-NLS-1$
	public final static String		CONFIG_IN_VOLTAGE_CUT_OFF		= "in_voltage_cut_off"; //$NON-NLS-1$
	public final static String		CONFIG_SAFETY_TIME					= "safety_time"; //$NON-NLS-1$
	public final static String		CONFIG_SET_CAPASITY					= "capacity_cut_off"; //$NON-NLS-1$
	public final static String		CONFIG_PROCESSING						= "processing"; //$NON-NLS-1$
	public final static String		CONFIG_BATTERY_TYPE					= "battery_type"; //$NON-NLS-1$
	public final static String		CONFIG_PROCESSING_TIME			= "processing_time"; //$NON-NLS-1$

	protected final DataExplorer				application;
	protected final EStationSerialPort						serialPort;
	protected final Channels											channels;
	protected       EStationDialog								dialog;

	protected HashMap<String, CalculationThread>	calculationThreads	= new HashMap<String, CalculationThread>();

	/**
	 * constructor using properties file
	 * @throws JAXBException 
	 * @throws FileNotFoundException 
	 */
	public eStation(String deviceProperties) throws FileNotFoundException, JAXBException {
		super(deviceProperties);		Messages.setDeviceResourceBundle("osde.device.htronic.messages", Settings.getInstance().getLocale(), this.getClass().getClassLoader()); //$NON-NLS-1$
		// initializing the resource bundle for this device
		Messages.setDeviceResourceBundle("osde.device.bantam.messages", Settings.getInstance().getLocale(), this.getClass().getClassLoader()); //$NON-NLS-1$
		this.USAGE_MODE = new String[] { Messages.getString(MessageIds.DE_MSGT1400), Messages.getString(MessageIds.DE_MSGT1401), Messages.getString(MessageIds.DE_MSGT1402)};
		this.ACCU_TYPES = new String[] { Messages.getString(MessageIds.DE_MSGT1403), Messages.getString(MessageIds.DE_MSGT1404), Messages.getString(MessageIds.DE_MSGT1405), Messages.getString(MessageIds.DE_MSGT1406)};

		this.application = DataExplorer.getInstance();
		this.serialPort = new EStationSerialPort(this, this.application);
		this.channels = Channels.getInstance();
		if (this.application.getMenuToolBar() != null) this.configureSerialPortMenu(DeviceSerialPort.ICON_SET_START_STOP);
	}

	/**
	 * constructor using existing device configuration
	 * @param deviceConfig device configuration
	 */
	public eStation(DeviceConfiguration deviceConfig) {
		super(deviceConfig);
		// initializing the resource bundle for this device
		Messages.setDeviceResourceBundle("osde.device.bantam.messages", Settings.getInstance().getLocale(), this.getClass().getClassLoader()); //$NON-NLS-1$
		this.USAGE_MODE = new String[] { Messages.getString(MessageIds.DE_MSGT1400), Messages.getString(MessageIds.DE_MSGT1401), Messages.getString(MessageIds.DE_MSGT1402)};
		this.ACCU_TYPES = new String[] { Messages.getString(MessageIds.DE_MSGT1403), Messages.getString(MessageIds.DE_MSGT1404), Messages.getString(MessageIds.DE_MSGT1405), Messages.getString(MessageIds.DE_MSGT1406)};

		this.application = DataExplorer.getInstance();
		this.serialPort = new EStationSerialPort(this, this.application);
		this.channels = Channels.getInstance();
		this.configureSerialPortMenu(DeviceSerialPort.ICON_SET_START_STOP);
	}
	
	/**
	 * query the default stem used as record set name
	 * @return recordSetStemName
	 */
	public String getRecordSetStemName() {
		return Messages.getString(MessageIds.DE_MSGT1411);
	}

	/**
	 * load the mapping exist between lov file configuration keys and OSDE keys
	 * @param lov2osdMap reference to the map where the key mapping has to be put
	 * @return lov2osdMap same reference as input parameter
	 */
	public HashMap<String, String> getLovKeyMappings(HashMap<String, String> lov2osdMap) {
		// no device specific mapping required
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
		// ...
		return ""; //$NON-NLS-1$
	}

	/**
	 * get LogView data bytes size, as far as known modulo 16 and depends on the bytes received from device 
	 */
	public int getLovDataByteSize() {
		return 84;  
	}
	/**
	 * add record data size points from LogView data stream to each measurement, if measurement is calculation 0 will be added
	 * adaption from LogView stream data format into the device data buffer format is required
	 * do not forget to call makeInActiveDisplayable afterwords to calualte th emissing data
	 * this method is more usable for real logger, where data can be stored and converted in one block
	 * @param recordSet
	 * @param dataBuffer
	 * @param recordDataSize
	 * @param doUpdateProgressBar
	 * @throws DataInconsitsentException 
	 */
	public synchronized void addConvertedLovDataBufferAsRawDataPoints(RecordSet recordSet, byte[] dataBuffer, int recordDataSize, boolean doUpdateProgressBar) throws DataInconsitsentException {
		String sThreadId = String.format("%06d", Thread.currentThread().getId()); //$NON-NLS-1$
		int deviceDataBufferSize = 76; // const.
		int[] points = new int[this.getNumberOfMeasurements(1)];
		int offset = 0;
		int progressCycle = 0;
		int lovDataSize = this.getLovDataByteSize();
		long lastDateTime = 0, sumTimeDelta = 0, deltaTime = 0; 
		
		if (dataBuffer[0] == 0x7B) {
			byte[] convertBuffer = new byte[deviceDataBufferSize];
			if (doUpdateProgressBar) this.application.setProgress(progressCycle, sThreadId);

			for (int i = 0; i < recordDataSize; i++) {
				System.arraycopy(dataBuffer, offset + i * lovDataSize, convertBuffer, 0, deviceDataBufferSize);
				recordSet.addPoints(convertDataBytes(points, convertBuffer));

				if (doUpdateProgressBar && i % 50 == 0) this.application.setProgress(((++progressCycle * 5000) / recordDataSize), sThreadId);
			}
			
			recordSet.setTimeStep_ms(this.getAverageTimeStep_ms() != null ? this.getAverageTimeStep_ms() : 1478); // no average time available, use a hard coded one
		}
		else { // none constant time steps
			byte[] sizeBuffer = new byte[4];
			byte[] convertBuffer = new byte[deviceDataBufferSize];
			
			if (doUpdateProgressBar) this.application.setProgress(progressCycle, sThreadId);
			for (int i = 0; i < recordDataSize; i++) {
				System.arraycopy(dataBuffer, offset, sizeBuffer, 0, 4);
				lovDataSize = 4 + LogViewReader.parse2Int(sizeBuffer);
				System.arraycopy(dataBuffer, offset + 4, convertBuffer, 0, deviceDataBufferSize);
				recordSet.addPoints(convertDataBytes(points, convertBuffer));
				offset += lovDataSize;
				
				StringBuilder sb = new StringBuilder();
				byte[] timeBuffer = new byte[lovDataSize - deviceDataBufferSize - 4];
				//sb.append(timeBuffer.length).append(" - ");
				System.arraycopy(dataBuffer, offset - timeBuffer.length, timeBuffer, 0, timeBuffer.length);
				String timeStamp = new String(timeBuffer).substring(0, timeBuffer.length-8)+"0000000000";
				long dateTime = new Long(timeStamp.substring(6,17));
				log.log(Level.FINEST, timeStamp + " " + timeStamp.substring(6,17) + " " + dateTime);
				sb.append(dateTime);
				//System.arraycopy(dataBuffer, offset - 4, sizeBuffer, 0, 4);
				//sb.append(" ? ").append(LogViewReader.parse2Int(sizeBuffer));
				deltaTime = lastDateTime == 0 ? 0 : (dateTime - lastDateTime)/1000 - 217; // value 217 is a compromis manual selected
				sb.append(" - ").append(deltaTime);
				sb.append(" - ").append(sumTimeDelta += deltaTime);
				log.log(Level.FINER, sb.toString());
				lastDateTime = dateTime;
				
				recordSet.addTimeStep_ms(sumTimeDelta);

				if (doUpdateProgressBar && i % 50 == 0) this.application.setProgress(((++progressCycle * 5000) / recordDataSize), sThreadId);
			}
//			recordSet.setTimeStep_ms((double)sumTimeDelta/recordDataSize);
//			log.log(Level.FINE, sumTimeDelta/recordDataSize + " " + sumTimeDelta);
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
		
		//StringBuilder sb = new StringBuilder();
		//for (byte b : dataBuffer) {
		//	sb.append(String.format("%02x", b)).append(" ");
		//}
		//log.log(Level.FINE, sb.toString());
		
		//int modeIndex = getProcessingMode(dataBuffer);
		//String mode = USAGE_MODE[modeIndex];
		//int accuIndex = getAccuCellType(dataBuffer);
		//String accuType = ACCU_TYPES[accuIndex - 1]; 
		//getNumberOfLithiumXCells(dataBuffer);
		
		// 0=Spannung 1=Strom 2=Ladung 3=Leistung 4=Energie 5=Temp.extern 6=Temp.intern 7=VersorgungsSpg. 
		points[0] = Integer.valueOf((((dataBuffer[35] & 0xFF)-0x80)*100 + ((dataBuffer[36] & 0xFF)-0x80))*10);  //35,36   feed-back voltage
		points[1] = Integer.valueOf((((dataBuffer[33] & 0xFF)-0x80)*100 + ((dataBuffer[34] & 0xFF)-0x80))*10);  //33,34   feed-back current : 0=0.0A,900=9.00A
		points[2] = Integer.valueOf((((dataBuffer[43] & 0xFF)-0x80)*100 + ((dataBuffer[44] & 0xFF)-0x80))*1000);//43,44  cq_capa_dis;  : charged capacity
		points[3] = Double.valueOf((points[0] / 1000.0) * (points[1] / 1000.0) * 1000).intValue(); 							// power U*I [W]
		points[4] = Double.valueOf((points[0] / 1000.0) * (points[2] / 1000.0)).intValue();											// energy U*C [mWh]
		points[5] = Integer.valueOf((((dataBuffer[37] & 0xFF)-0x80)*100 + ((dataBuffer[38] & 0xFF)-0x80))*10);  //37,38  fd_ex_th;     : external temperature
		points[6] = Integer.valueOf((((dataBuffer[39] & 0xFF)-0x80)*100 + ((dataBuffer[40] & 0xFF)-0x80))*10);  //39,40  fd_in_th      : internal temperature
		points[7] = Integer.valueOf((((dataBuffer[41] & 0xFF)-0x80)*100 + ((dataBuffer[42] & 0xFF)-0x80))*10);  //41,42  fd_in_12v;    : input voltage(00.00V 30.00V)
		// 8=SpannungZelle1 9=SpannungZelle2 10=SpannungZelle3 11=SpannungZelle4 12=SpannungZelle5 13=SpannungZelle6
		for (int i=0, j=0; i<points.length - 8; ++i, j+=2) {
			//log_base.info("cell " + (i+1) + " points[" + (i+8) + "]  = new Integer((((dataBuffer[" + (j+45) + "] & 0xFF)-0x80)*100 + ((dataBuffer[" + (j+46)+ "] & 0xFF)-0x80))*10);");  //45,46 CELL_420v[1];
			points[i+8]  = Integer.valueOf((((dataBuffer[j+45] & 0xFF)-0x80)*100 + ((dataBuffer[j+46] & 0xFF)-0x80))*10);  //45,46 CELL_420v[1];
		}

		return points;
	}
	
	/**
	 * query if the eStation executes discharge > charge > discharge cycles
	 */
	boolean isCycleMode(byte[] dataBuffer) {
		return (((dataBuffer[8] & 0xFF)-0x80) & 0x10) > 0;
	}
	
	/**
	 * getNumberOfCycle for NiCd and NiMh, for LiXx it  will return 0
	 * accuCellType -> Lithium=1, NiMH=2, NiCd=3, Pb=4
	 * @param dataBuffer
	 * @return cycle count
	 */
	public int getNumberOfCycle(byte[] dataBuffer) {
		int cycleCount = 0;
		int accuCellType = getAccuCellType(dataBuffer);
		
		if 			(accuCellType == 2) {
			cycleCount = (dataBuffer[16] & 0xFF)- 0x80;
			//log.info("NiMh D<C " + ((dataBuffer[15] & 0xFF)- 0x80));
		}
		else if (accuCellType == 3) {
			cycleCount = (dataBuffer[12] & 0xFF)- 0x80;
			//log.info("NiCd D<C " + ((dataBuffer[11] & 0xFF)- 0x80));
		}
		
		return cycleCount;
	}

	/**
	 * @param dataBuffer
	 * @return cell count (0=auto, 1=1cell, 12=12cells)
	 */
	public int getNumberOfLithiumXCells(byte[] dataBuffer) {
		return (dataBuffer[18] & 0xFF)- 0x80;// cell count (0=auto, 1=1cell, 12=12cells)
	}

	/**
	 * @param dataBuffer
	 * @return for Lithium=1, NiMH=2, NiCd=3, Pb=4
	 */
	public int getAccuCellType(byte[] dataBuffer) {
		return (dataBuffer[23] & 0xFF)- 0x80; //Lithium=1, NiMH=2, NiCd=3, Pb=4
	}

	/**
	 * @param dataBuffer
	 * @return for Lithium=1, NiMH=2, NiCd=3, Pb=4
	 */
	public boolean isProcessing(byte[] dataBuffer) {
		return ((dataBuffer[24] & 0xFF)- 0x80) == 1; //processing = 1; stop = 0
	}

	/**
	 * @param dataBuffer [lenght 76 bytes]
	 * @return 0 = no processing, 1 = discharge, 2 = charge
	 */
	public int getProcessingMode(byte[] dataBuffer) {
		int modeIndex = (dataBuffer[24] & 0xFF) - 0x80; // processing=1, stop=0 
		if(modeIndex != 0) {
			modeIndex = (dataBuffer[8] & 0x0F) == 0x01 ? 2 : 1;
		}
		return modeIndex;
	}

	/**
	 * @param dataBuffer [lenght 76 bytes]
	 * @return processing time in seconds
	 */
	public int getProcessingTime(byte[] dataBuffer) {
		return  ((dataBuffer[69] & 0xFF - 0x80)*100 + (dataBuffer[70] & 0xFF - 0x80));
	}

	/**
	 * @param dataBuffer [lenght 76 bytes]
	 * @return processing current
	 */
	public int getFeedBackCurrent(byte[] dataBuffer) {
		return  (((dataBuffer[33] & 0xFF)-0x80)*100 + ((dataBuffer[34] & 0xFF)-0x80))*10;
	}

	/**
	 * get global device configuration values
	 * @param configData
	 * @param dataBuffer
	 */
	public HashMap<String, String> getConfigurationValues(HashMap<String, String> configData, byte[] dataBuffer) {
		configData.put(eStation.CONFIG_EXT_TEMP_CUT_OFF,   ""+(dataBuffer[ 4] & 0xFF - 0x80)); //$NON-NLS-1$
		configData.put(eStation.CONFIG_WAIT_TIME,      ""+(dataBuffer[ 5] & 0xFF - 0x80)); //$NON-NLS-1$
		configData.put(eStation.CONFIG_IN_VOLTAGE_CUT_OFF, ""+(dataBuffer[ 7] & 0xFF - 0x80)/10); //$NON-NLS-1$
		configData.put(eStation.CONFIG_SAFETY_TIME,  ""+((dataBuffer[29] & 0xFF - 0x80)*100 + (dataBuffer[30] & 0xFF - 0x80) * 10)); //$NON-NLS-1$
		configData.put(eStation.CONFIG_SET_CAPASITY, ""+(((dataBuffer[31] & 0xFF - 0x80)*100 + (dataBuffer[32] & 0xFF - 0x80)))); //$NON-NLS-1$
		if(getProcessingMode(dataBuffer) != 0) {
			configData.put(eStation.CONFIG_BATTERY_TYPE, this.ACCU_TYPES[(dataBuffer[23] & 0xFF - 0x80) - 1]);
			configData.put(eStation.CONFIG_PROCESSING_TIME, ""+((dataBuffer[69] & 0xFF - 0x80)*100 + (dataBuffer[70] & 0xFF - 0x80))); //$NON-NLS-1$
		}
		for (String key : configData.keySet()) {
			log.log(Level.FINE, key + " = " + configData.get(key)); //$NON-NLS-1$
		}
		return configData;
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
		int dataBufferSize = DE.SIZE_BYTES_INTEGER * recordSet.getNoneCalculationRecordNames().length;
		byte[] convertBuffer = new byte[dataBufferSize];
		int[] points = new int[recordSet.getRecordNames().length];
		String sThreadId = String.format("%06d", Thread.currentThread().getId()); //$NON-NLS-1$
		int progressCycle = 0;
		Vector<Integer> timeStamps = new Vector<Integer>(1,1);
		if (doUpdateProgressBar) this.application.setProgress(progressCycle, sThreadId);
		
		int timeStampBufferSize = 0;
		if(!recordSet.isTimeStepConstant()) {
			timeStampBufferSize = DE.SIZE_BYTES_INTEGER * recordDataSize;
			byte[] timeStampBuffer = new byte[timeStampBufferSize];
			System.arraycopy(dataBuffer, 0, timeStampBuffer, 0, timeStampBufferSize);

			for (int i = 0; i < recordDataSize; i++) {
				timeStamps.add(((timeStampBuffer[0 + (i * 4)] & 0xff) << 24) + ((timeStampBuffer[1 + (i * 4)] & 0xff) << 16) + ((timeStampBuffer[2 + (i * 4)] & 0xff) << 8) + ((timeStampBuffer[3 + (i * 4)] & 0xff) << 0));
			}
		}
		log.log(Level.FINE, timeStamps.size() + " timeStamps = " + timeStamps.toString());

		for (int i = 0; i < recordDataSize; i++) {
			log.log(Level.FINER, i + " i*dataBufferSize+timeStampBufferSize = " + i*dataBufferSize+timeStampBufferSize);
			System.arraycopy(dataBuffer, i*dataBufferSize+timeStampBufferSize, convertBuffer, 0, dataBufferSize);
			
			// 0=Spannung 1=Strom 2=Ladung 3=Leistung 4=Energie 5=Temp.extern 6=Temp.intern 7=VersorgungsSpg. 
			points[0] = (((convertBuffer[0]&0xff) << 24) + ((convertBuffer[1]&0xff) << 16) + ((convertBuffer[2]&0xff) << 8) + ((convertBuffer[3]&0xff) << 0));
			points[1] = (((convertBuffer[4]&0xff) << 24) + ((convertBuffer[5]&0xff) << 16) + ((convertBuffer[6]&0xff) << 8) + ((convertBuffer[7]&0xff) << 0));
			points[2] = (((convertBuffer[8]&0xff) << 24) + ((convertBuffer[9]&0xff) << 16) + ((convertBuffer[10]&0xff) << 8) + ((convertBuffer[11]&0xff) << 0));
			points[3] = Double.valueOf((points[0] / 1000.0) * (points[1] / 1000.0) * 1000).intValue(); 							// power U*I [W]
			points[4] = Double.valueOf((points[0] / 1000.0) * (points[2] / 1000.0)).intValue();											// energy U*C [mWh]
			points[5] = (((convertBuffer[12]&0xff) << 24) + ((convertBuffer[13]&0xff) << 16) + ((convertBuffer[14]&0xff) << 8) + ((convertBuffer[15]&0xff) << 0));
			points[6] = (((convertBuffer[16]&0xff) << 24) + ((convertBuffer[17]&0xff) << 16) + ((convertBuffer[18]&0xff) << 8) + ((convertBuffer[19]&0xff) << 0));
			points[7] = (((convertBuffer[20]&0xff) << 24) + ((convertBuffer[21]&0xff) << 16) + ((convertBuffer[22]&0xff) << 8) + ((convertBuffer[23]&0xff) << 0));
			// 8=SpannungZelle1 9=SpannungZelle2 10=SpannungZelle3 11=SpannungZelle4 12=SpannungZelle5 13=SpannungZelle6
			for (int j=0, k=0; j<points.length - 8; ++j, k+=DE.SIZE_BYTES_INTEGER) {
				//log_base.info("cell " + (i+1) + " points[" + (i+8) + "]  = new Integer((((dataBuffer[" + (j+45) + "] & 0xFF)-0x80)*100 + ((dataBuffer[" + (j+46)+ "] & 0xFF)-0x80))*10);");  //45,46 CELL_420v[1];
				points[j+8] = (((convertBuffer[k+24]&0xff) << 24) + ((convertBuffer[k+25]&0xff) << 16) + ((convertBuffer[k+26]&0xff) << 8) + ((convertBuffer[k+27]&0xff) << 0));
			}
			
			if(recordSet.isTimeStepConstant()) 
				recordSet.addPoints(points);
			else
				recordSet.addPoints(points, timeStamps.get(i)/10.0);
			
			if (doUpdateProgressBar && i % 50 == 0) this.application.setProgress(((++progressCycle*5000)/recordDataSize), sThreadId);
		}
		if (doUpdateProgressBar) this.application.setProgress(100, sThreadId);
	}

	/**
	 * function to prepare a data table row of record set while translating available measurement values
	 * @return pointer to filled data table row with formated values
	 */
	public int[] prepareDataTableRow(RecordSet recordSet, int[][] dataTable, int rowIndex) {
		try {
			String[] recordNames = recordSet.getRecordNames();				// 0=Spannung 1=Strom 2=Ladung 3=Leistung 4=Energie 5=Temp.extern 6=Temp.intern 7=VersorgungsSpg. 
			// 8=SpannungZelle1 9=SpannungZelle2 10=SpannungZelle3 11=SpannungZelle4 12=SpannungZelle5 13=SpannungZelle6
			int numberRecords = recordNames.length;			

			for (int j = 0; j < numberRecords; j++) {
				Record record = recordSet.get(recordNames[j]);
				double reduction = record.getReduction();
				double factor = record.getFactor(); // != 1 if a unit translation is required
				dataTable[rowIndex][j+1] = Double.valueOf(((record.get(rowIndex)/1000.0) - reduction) * factor * 1000.0).intValue();				
			}
			dataTable[rowIndex][0] = (int)recordSet.getTime_ms(rowIndex);
		}
		catch (RuntimeException e) {
			log.log(Level.SEVERE, e.getMessage(), e);
		}
		return dataTable[rowIndex];		
	}

	/**
	 * function to prepare a data table row of record set while translating available measurement values
	 * @return pointer to filled data table row with formated values
	 */
	public int[] prepareDataTableRow(RecordSet recordSet, int rowIndex) {
		int[] dataTableRow = new int[recordSet.size()+1]; // this.device.getMeasurementNames(this.channelNumber).length
		try {
			String[] recordNames = recordSet.getRecordNames();				
			// 0=Spannung 1=Strom 2=Ladung 3=Leistung 4=Energie 5=Temp.extern 6=Temp.intern 7=VersorgungsSpg. 
			// 8=SpannungZelle1 9=SpannungZelle2 10=SpannungZelle3 11=SpannungZelle4 12=SpannungZelle5 13=SpannungZelle6
			int numberRecords = recordNames.length;			
			dataTableRow[0] = (int)recordSet.getTime_ms(rowIndex); // time msec

			for (int j = 0; j < numberRecords; j++) {
				Record record = recordSet.get(recordNames[j]);
				double reduction = record.getReduction();
				double factor = record.getFactor(); // != 1 if a unit translation is required
				dataTableRow[j+1] = Double.valueOf(((record.get(rowIndex)/1000.0) - reduction) * factor * 1000.0).intValue();				
			}
		}
		catch (RuntimeException e) {
			log.log(Level.SEVERE, e.getMessage(), e);
		}
		return dataTableRow;		
	}

	/**
	 * function to translate measured values from a device to values represented
	 * this function should be over written by device and measurement specific algorithm
	 * @return double of device dependent value
	 */
	public double translateValue(Record record, double value) {
		// 0=Spannung 1=Strom 2=Ladung 3=Leistung 4=Energie 5=Temp.extern 6=Temp.intern 7=VersorgungsSpg. 
		// 8=SpannungZelle1 9=SpannungZelle2 10=SpannungZelle3 11=SpannungZelle4 12=SpannungZelle5 13=SpannungZelle6
		double offset = record.getOffset(); // != 0 if curve has an defined offset
		double factor = record.getFactor(); // != 1 if a unit translation is required
		
		double newValue = value * factor + offset;
		log.log(Level.FINE, "for " + record.getName() + " in value = " + value + " out value = " + newValue); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		return newValue;
	}

	/**
	 * function to reverse translate measured values from a device to values represented
	 * this function should be over written by device and measurement specific algorithm
	 * @return double of device dependent value
	 */
	public double reverseTranslateValue(Record record, double value) {
		// 0=Spannung 1=Strom 2=Ladung 3=Leistung 4=Energie 5=Temp.extern 6=Temp.intern 7=VersorgungsSpg. 
		// 8=SpannungZelle1 9=SpannungZelle2 10=SpannungZelle3 11=SpannungZelle4 12=SpannungZelle5 13=SpannungZelle6
		double offset = record.getOffset(); // != 0 if curve has an defined offset
		double factor = record.getFactor(); // != 1 if a unit translation is required

		double newValue = value / factor - offset;
		log.log(Level.FINE, "for " + record.getName() + " in value = " + value + " out value = " + newValue); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
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
		String[] recordKeys = recordSet.getRecordNames();

		//recordSet.setAllVisibleAndDisplayable();
		for (String recordKey : recordSet.getNoneCalculationRecordNames()) {
			recordSet.get(recordKey).setActive(true);
		}
		for (int i=6; i<recordKeys.length; ++i) {
				Record record = recordSet.get(recordKeys[i]);
				boolean hasReasonableData = record.getRealMaxValue() != 0 || record.getRealMinValue() != record.getRealMaxValue();
				//record.setVisible(record.isActive() && hasReasonableData);
				//log.log(Level.FINER, record.getName() + ".setVisible = " + hasReasonableData);
				record.setDisplayable(record.getOrdinal() <= 5 || hasReasonableData);
				log.log(Level.FINER, recordKeys[i] + " setDisplayable=" + (record.getOrdinal() <= 5 || hasReasonableData));
		}
		recordSet.isSyncableDisplayableRecords(true);
		
		if (log.isLoggable(Level.FINE)) {
			for (String recordKey : recordKeys) {
				Record record = recordSet.get(recordKey);
				log.log(Level.FINE, recordKey + " isActive=" + record.isActive() + " isVisible=" + record.isVisible() + " isDisplayable=" + record.isDisplayable());
			}
		}
	}

	/**
	 * function to calculate values for inactive records, data not readable from device
	 * if calculation is done during data gathering this can be a loop switching all records to displayable
	 * for calculation which requires more effort or is time consuming it can call a background thread, 
	 * target is to make sure all data point not coming from device directly are available and can be displayed 
	 */
	public void makeInActiveDisplayable(RecordSet recordSet) {
		// since there are live measurement points only the calculation will take place directly after switch all to displayable
		if (recordSet.isRaw()) {
			// calculate the values required
			try {
				// 0=Spannung 1=Strom 2=Ladung 3=Leistung 4=Energie 5=Temp.extern 6=Temp.intern 7=VersorgungsSpg. 
				String[] recordNames = recordSet.getRecordNames();
				int displayableCounter = 0;

				
				// check if measurements isActive == false and set to isDisplayable == false
				for (String measurementKey : recordNames) {
					Record record = recordSet.get(measurementKey);
					
					if (record.isActive() && (record.getOrdinal() <= 5 || record.getRealMaxValue() != 0 || record.getRealMinValue() != record.getRealMaxValue())) {
						++displayableCounter;
					}
				}
				
				String recordKey = recordNames[3]; //3=Leistung
				Record record = recordSet.get(recordKey);
				if (record != null && (record.size() == 0 || (record.getRealMinValue() == 0 && record.getRealMaxValue() == 0))) {
					this.calculationThreads.put(recordKey, new CalculationThread(recordKey, this.channels.getActiveChannel().getActiveRecordSet()));
					try {
						this.calculationThreads.get(recordKey).start();
					}
					catch (RuntimeException e) {
						log.log(Level.WARNING, e.getMessage(), e);
					}
				}
				++displayableCounter;
				
				recordKey = recordNames[4]; //4=Energie
				record = recordSet.get(recordKey);
				if (record != null && (record.size() == 0 || (record.getRealMinValue() == 0 && record.getRealMaxValue() == 0))) {
					this.calculationThreads.put(recordKey, new CalculationThread(recordKey, this.channels.getActiveChannel().getActiveRecordSet()));
					try {
						this.calculationThreads.get(recordKey).start();
					}
					catch (RuntimeException e) {
						log.log(Level.WARNING, e.getMessage(), e);
					}
				}		
				++displayableCounter;
				
				log.log(Level.FINE, "displayableCounter = " + displayableCounter); //$NON-NLS-1$
				recordSet.setConfiguredDisplayable(displayableCounter);		

				if (recordSet.getName().equals(this.channels.getActiveChannel().getActiveRecordSet().getName())) {
					this.application.updateGraphicsWindow();
				}
			}
			catch (RuntimeException e) {
				log.log(Level.SEVERE, e.getMessage(), e);
			}
		}
	}

	/**
	 * @return the serialPort
	 */
	public EStationSerialPort getSerialPort() {
		return this.serialPort;
	}

	/**
	 * query for all the property keys this device has in use
	 * - the property keys are used to filter serialized properties form OSD data file
	 * @return [offset, factor, reduction, number_cells, prop_n100W, ...]
	 */
	public String[] getUsedPropertyKeys() {
		return new String[] {IDevice.OFFSET, IDevice.FACTOR};
	}
	
	/**
	 * @return the dialog
	 */
	public EStationDialog getDialog() {
		return this.dialog;
	}
	
	/**
	 * method toggle open close serial port or start/stop gathering data from device
	 */
	public void openCloseSerialPort() {
		if (this.serialPort != null) {
			if (!this.serialPort.isConnected()) {
				try {
					Channel activChannel = Channels.getInstance().getActiveChannel();
					if (activChannel != null) {
						this.getDialog().dataGatherThread = new GathererThread(this.application, this, this.serialPort, activChannel.getNumber(), this.getDialog());
						try {
							this.getDialog().dataGatherThread.start();
						}
						catch (RuntimeException e) {
							log.log(Level.SEVERE, e.getMessage(), e);
						}
						catch (Throwable e) {
							log.log(Level.SEVERE, e.getMessage(), e);
						}
						if (this.getDialog().boundsComposite != null && !this.getDialog().isDisposed()) this.getDialog().boundsComposite.redraw();
					}
				}
				catch (SerialPortException e) {
					log.log(Level.SEVERE, e.getMessage(), e);
					this.application.openMessageDialog(this.dialog.getDialogShell(), Messages.getString(osde.messages.MessageIds.DE_MSGE0015, new Object[] { e.getClass().getSimpleName() + DE.STRING_BLANK_COLON_BLANK + e.getMessage()}));
				}
				catch (ApplicationConfigurationException e) {
					log.log(Level.SEVERE, e.getMessage(), e);
					this.application.openMessageDialog(this.dialog.getDialogShell(), Messages.getString(osde.messages.MessageIds.DE_MSGE0010));
					this.application.getDeviceSelectionDialog().open();
				}
				catch (Throwable e) {
					log.log(Level.SEVERE, e.getMessage(), e);
				}
			}
			else {
				if (this.getDialog().dataGatherThread != null) {
					this.getDialog().dataGatherThread.stopDataGatheringThread(false, null);
				}
				if (this.getDialog().boundsComposite != null && !this.getDialog().isDisposed()) this.getDialog().boundsComposite.redraw();
				this.serialPort.close();
			}
		}
	}
	
	/**
	 * set the measurement ordinal of the values displayed in cell voltage window underneath the cell voltage bars
	 * set value of -1 to suppress this measurement
	 */
	public int[] getCellVoltageOrdinals() {
		// 0=Spannung 1=Strom 2=Ladung 3=Leistung 4=Energie 5=Temp.extern 6=Temp.intern 7=VersorgungsSpg. 
		return new int[] {0, 2};
	}
}
