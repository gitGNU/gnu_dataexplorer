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
package osde.io;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import osde.log.Level;
import java.util.logging.Logger;

import osde.OSDE;
import osde.data.Channel;
import osde.data.Channels;
import osde.data.RecordSet;
import osde.device.IDevice;
import osde.exception.DataInconsitsentException;
import osde.exception.DataTypeException;
import osde.exception.DevicePropertiesInconsistenceException;
import osde.exception.MissMatchDeviceException;
import osde.exception.NotSupportedFileFormatException;
import osde.ui.OpenSerialDataExplorer;

/**
 * Class to read and write comma separated value files which simulates serial data 
 * one data line consist of $1;1;0; 14780;  598;  1000;  8838;.....;0002;
 * where $recordSetNumber; stateNumber; timeStepSeconds; firstIntValue; secondIntValue; .....;checkSumIntValue;
 * All properties around the textual data in this line has to be specified in DataBlockType (type=TEXT, size number of values, separator=;, ...), refer to DeviceProperties_XY.XSD
 * @author Winfried Brügmann
 */
public class CSVSerialDataReaderWriter {
	static Logger					log			= Logger.getLogger(CSVSerialDataReaderWriter.class.getName());

	static String					lineSep	= OSDE.LINE_SEPARATOR;
	static DecimalFormat	df3			= new DecimalFormat("0.000"); //$NON-NLS-1$
	static StringBuffer		sb;
	
	final static OpenSerialDataExplorer	application	= OpenSerialDataExplorer.getInstance();
	final static Channels								channels		= Channels.getInstance();
	

	/**
	 * read the selected CSV file
	 * @return record set created
	 * @throws NotSupportedFileFormatException 
	 * @throws MissMatchDeviceException 
	 * @throws IOException 
	 * @throws DataInconsitsentException 
	 * @throws DataTypeException 
	 */
	public static RecordSet read(String filePath, IDevice device, String recordSetNameExtend, Integer channelConfigNumber, boolean isRaw) throws NotSupportedFileFormatException, IOException, DataInconsitsentException, DataTypeException {
		String sThreadId = String.format("%06d", Thread.currentThread().getId());
		String line = OSDE.STRING_STAR;
		RecordSet recordSet = null;
		BufferedReader reader; // to read the data
		Channel activeChannel = null;
		int lineNumber = 0;
		int activeChannelConfigNumber = 1; // at least each device needs to have one channelConfig to place record sets

		try {
			if (channelConfigNumber == null)
				activeChannel = channels.getActiveChannel();
			else
				activeChannel = channels.get(channelConfigNumber);

			if (activeChannel != null) {
				if (application.getStatusBar() != null) application.setStatusMessage("Reading serial text data input from: " + filePath);
				activeChannelConfigNumber = activeChannel.getNumber();
				
				

				if (application.getStatusBar() != null) {
					channels.switchChannel(activeChannel.getNumber(), OSDE.STRING_EMPTY);
					application.getMenuToolBar().updateChannelSelector();
					activeChannel = channels.getActiveChannel();
				}
				String recordSetName = (activeChannel.size() + 1) + ") " + recordSetNameExtend; //$NON-NLS-1$
				int lastRecordNumber = -1;

				//now get all data   $1;1;0; 14780;  598;  1000;  8838;  0002
				//$recordSetNumber;stateNumber;timeStepSeconds;firstIntValue;secondIntValue;.....;checkSumIntValue;
				int measurementSize = device.getNumberOfMeasurements(activeChannelConfigNumber);
				int dataBlockSize = device.getDataBlockSize(); // measurements size must not match data block size, there are some measurements which are result of calculation			
				log.log(Level.FINE, "measurementSize = " + measurementSize + "; dataBlockSize = " + dataBlockSize); 
				if (measurementSize != dataBlockSize)  throw new DevicePropertiesInconsistenceException("Konfigurationsfehler in " + filePath + "\nAnzahl der definierten Messwerte passt nicht zur Datenblockgröße!\nDas verwendete Separatorzeichen könnte auch an einigen Stellen falsch sein!");
				DataParser data = new DataParser(device.getDataBlockTimeUnitFactor(), device.getDataBlockSeparator().value(), device.getDataBlockCheckSumType(), dataBlockSize); //$NON-NLS-1$  //$NON-NLS-2$

				DataInputStream binReader    = new DataInputStream(new FileInputStream(new File(filePath)));
				byte[] buffer = new byte[1024];
				byte[] lineEnding = device.getDataBlockEnding();
				boolean lineEndingOcurred = false;
				int chars = binReader.read(buffer);
				for (int i = 0; i < chars; i++) {
					if (buffer[i] == lineEnding[0] && i < chars-lineEnding.length-1 && buffer[i+lineEnding.length+1] != '$'
						&& (lineEnding.length > 1 ? buffer[i+1] == lineEnding[1] : true)) {
						lineEndingOcurred = true;
					}
				}
				binReader.close();
				if (!lineEndingOcurred) throw new DevicePropertiesInconsistenceException("Defined line ending does not occur in first " + chars + " characters of " + filePath);

				reader = new BufferedReader(new InputStreamReader(new FileInputStream(filePath), "ISO-8859-1")); //$NON-NLS-1$			
				while ((line = reader.readLine()) != null) {
					++lineNumber;
					data.parse(line);

					if (device.getStateType() == null) 
						throw new DataInconsitsentException("no state defined");
					try {
						recordSetNameExtend = device.getStateType().getProperty().get(data.state - 1).getName(); // state name
					}
					catch (Exception e) {
						throw new DevicePropertiesInconsistenceException("Device state " + data.state + " in " + filePath + " is not defined in " + device.getPropertiesFileName());
					}

					//detect states where a new record set has to be created
					if (recordSet == null || !recordSet.getName().endsWith(recordSetNameExtend) || lastRecordNumber != data.recordNumber) {
						
						if (recordSet != null) { // apply something to previous record set
							//check reasonable size of data points
							if (recordSet.get(0).realSize() < 3) {
								activeChannel.remove(recordSetName);
								log.log(Level.WARNING, "remove record set with < 3 data points");
								application.openMessageDialog("remove record set with < 3 data points");

							}
							else
								recordSet.checkAllDisplayable(); // raw import needs calculation of passive records
						}
						//prepare new record set now
						lastRecordNumber = data.recordNumber;
						recordSetName = (activeChannel.size() + 1) + ") " + recordSetNameExtend; //$NON-NLS-1$

						recordSet = RecordSet.createRecordSet(recordSetName, device, activeChannel.getNumber(), isRaw, true);
						recordSetName = recordSet.getName(); // cut/correct length

						// make all records displayable while absolute data
						String[] recordNames = device.getMeasurementNames(activeChannel.getNumber());
						if (!isRaw) { // absolute
							for (String recordKey : recordNames) {
								recordSet.get(recordKey).setDisplayable(true); // all data available 
							}
						}
						//recordSet.setTimeStep_ms(device.getTimeStep_ms()); // set -1 for none constant time step between measurement points
						activeChannel.put(recordSetName, recordSet);
						activeChannel.applyTemplate(recordSetName, true);
					}

					if (isRaw)
						recordSet.addNoneCalculationRecordsPoints(data.values, data.time_ms);
					else
						recordSet.addPoints(data.values, data.time_ms);
				}

				activeChannel.setActiveRecordSet(recordSetName);
				device.updateVisibilityStatus(activeChannel.get(recordSetName));
				activeChannel.get(recordSetName).checkAllDisplayable(); // raw import needs calculation of passive records
				if (application.getStatusBar() != null) activeChannel.switchRecordSet(recordSetName);

				reader.close();
				reader = null;
			}
		}
		catch (FileNotFoundException e) {
			log.log(Level.WARNING, e.getMessage(), e);
			application.openMessageDialog(e.getMessage());
		}
		catch (IOException e) {
			log.log(Level.WARNING, e.getMessage(), e);
			application.openMessageDialog(e.getMessage());
		}
		catch (Exception e) {
			String msg = e.getMessage() + " line number " + lineNumber + ", please correct!";
			log.log(Level.WARNING, msg, e);
			application.openMessageDialog(msg);
		}
		finally {
			if (application.getStatusBar() != null) {
				if (device.isTableTabRequested())
					application.setProgress(10, sThreadId);
				else
					application.setProgress(100, sThreadId);
				
				application.setStatusMessage(OSDE.STRING_EMPTY);
			}
		}
		
		return recordSet;
	}

//	/**
//	 * write data CVS file
//	 * @throws Exception 
//	 */
//	public static void write(char separator, String recordSetKey, String filePath, boolean isRaw) throws Exception {
//		BufferedWriter writer;
//		String sThreadId = String.format("%06d", Thread.currentThread().getId());
//
//		try {
//			if (application.getStatusBar() != null) application.setStatusMessage(Messages.getString(MessageIds.OSDE_MSGT0138) + filePath);
//			writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filePath), "ISO-8859-1")); //$NON-NLS-1$
//			char decimalSeparator = Settings.getInstance().getDecimalSeparator();
//
//			df3.setGroupingUsed(false);
//			sb = new StringBuffer();
//			RecordSet recordSet = Channels.getInstance().getActiveChannel().get(recordSetKey);
//			IDevice device = OpenSerialDataExplorer.getInstance().getActiveDevice();
//			// write device name , manufacturer, and serial port string
//			sb.append(device.getName()).append(separator).append(recordSet.getChannelConfigName()).append(lineSep);
//			writer.write(sb.toString());
//			log.log(Level.FINE, "written header line = " + sb.toString());  //$NON-NLS-1$
//			
//			sb = new StringBuffer();
//			sb.append(Messages.getString(MessageIds.OSDE_MSGT0137)).append(separator); // Spannung [V];Strom [A];Ladung [Ah];Leistung [W];Energie [Wh]"; //$NON-NLS-1$
//			// write the measurements signature
//			String[] recordNames = recordSet.getRecordNames();
//			for (int i = 0; i < recordNames.length; i++) {
//				MeasurementType  measurement = device.getMeasurement(recordSet.getChannelConfigNumber(), i);
//				Record record = recordSet.get(recordNames[i]);
//				log.log(Level.FINEST, "append " + recordNames[i]); //$NON-NLS-1$
//				if (isRaw) {
//					if (!measurement.isCalculation()) {	// only use active records for writing raw data 
//						sb.append(recordNames[i]).append(" [---]").append(separator);	 //$NON-NLS-1$
//						log.log(Level.FINEST, "append " + recordNames[i]); //$NON-NLS-1$
//					}
//				}
//				else {
//					sb.append(recordNames[i]).append(" [").append(record.getUnit()).append(']').append(separator);	 //$NON-NLS-1$
//					log.log(Level.FINEST, "append " + recordNames[i]); //$NON-NLS-1$
//				}
//			}
//			sb.deleteCharAt(sb.length() - 1).append(lineSep);
//			log.log(Level.FINER, "header line = " + sb.toString()); //$NON-NLS-1$
//			writer.write(sb.toString());
//
//			// write data
//			long startTime = new Date().getTime();
//			int recordEntries = recordSet.getRecordDataSize(true);
//			int progressCycle = 0;
//			if (application.getStatusBar() != null) application.setProgress(progressCycle, sThreadId);
//			for (int i = 0; i < recordEntries; i++) {
//				sb = new StringBuffer();
//				// add time entry
//				sb.append((df3.format(new Double(i * recordSet.getTimeStep_ms() / 1000.0))).replace('.', decimalSeparator)).append(separator).append(' ');
//				// add data entries
//				for (int j = 0; j < recordNames.length; j++) {
//					Record record = recordSet.getRecord(recordNames[j]);
//					if (record == null)
//						throw new Exception(Messages.getString(MessageIds.OSDE_MSGE0005, new Object[]{recordNames[j], recordSet.getChannelConfigName()}));
//
//					MeasurementType measurement = device.getMeasurement(recordSet.getChannelConfigNumber(), j);
//					if (isRaw) { // do not change any values
//						if (!measurement.isCalculation())
//							if (record.getParent().isRaw())
//								sb.append(df3.format((record.get(i)/1000.0)).replace('.', decimalSeparator)).append(separator);
//							else
//								sb.append(df3.format(device.reverseTranslateValue(record, record.get(i)/1000.0)).replace('.', decimalSeparator)).append(separator);
//					}
//					else
//						// translate according device and measurement unit
//						sb.append(df3.format(device.translateValue(record, record.get(i)/1000.0)).replace('.', decimalSeparator)).append(separator);
//				}
//				sb.deleteCharAt(sb.length() - 1).append(lineSep);
//				writer.write(sb.toString());
//				if (application.getStatusBar() != null && i % 50 == 0) application.setProgress(((++progressCycle*5000)/recordEntries), sThreadId);
//				log.log(Level.FINE, "data line = " + sb.toString()); //$NON-NLS-1$
//			}
//			sb = null;
//			log.log(Level.FINE, "CSV file = " + filePath + " erfolgreich geschieben"  //$NON-NLS-1$ //$NON-NLS-2$
//					+ "write time = " + StringHelper.getFormatedTime("ss:SSS", (new Date().getTime() - startTime)));//$NON-NLS-1$ //$NON-NLS-2$
//
//			writer.flush();
//			writer.close();
//			writer = null;
//			//recordSet.setSaved(true);
//			if (application.getStatusBar() != null) application.setProgress(100, sThreadId);
//		}
//		catch (IOException e) {
//			log.log(Level.SEVERE, e.getMessage(), e);
//			throw new Exception(Messages.getString(MessageIds.OSDE_MSGE0006, new Object[]{ filePath }));
//		}
//		catch (Exception e) {
//			log.log(Level.SEVERE, e.getMessage(), e);
//			throw new Exception(Messages.getString(MessageIds.OSDE_MSGE0007) + e.getClass().getSimpleName() + OSDE.STRING_MESSAGE_CONCAT + e.getMessage()); 
//		}
//		finally {
//			if (application.getStatusBar() != null) application.setStatusMessage(OSDE.STRING_EMPTY);
//		}
//
//	}

}
