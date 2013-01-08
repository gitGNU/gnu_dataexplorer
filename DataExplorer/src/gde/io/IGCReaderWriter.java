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
    
    Copyright (c) 2012 Winfried Bruegmann
****************************************************************************************/
package gde.io;

import gde.GDE;
import gde.data.Channel;
import gde.data.Channels;
import gde.data.Record;
import gde.data.RecordSet;
import gde.device.IDevice;
import gde.device.InputTypes;
import gde.exception.DataInconsitsentException;
import gde.exception.DataTypeException;
import gde.exception.DevicePropertiesInconsistenceException;
import gde.exception.MissMatchDeviceException;
import gde.exception.NotSupportedFileFormatException;
import gde.log.Level;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.utils.GPSHelper;
import gde.utils.StringHelper;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.logging.Logger;

/**
 * Class to write IGC conform files
 * @author Winfried Brügmann
 */
public class IGCReaderWriter {
	static Logger							log					= Logger.getLogger(IGCReaderWriter.class.getName());

	final static DataExplorer	application	= DataExplorer.getInstance();
	final static Channels			channels		= Channels.getInstance();

	/**
	 * read the selected IGC file and read/parse
	 * @param filePath
	 * @param device
	 * @param recordNameExtend
	 * @param channelConfigNumber
	 * @return record set created
	 * @throws NotSupportedFileFormatException 
	 * @throws MissMatchDeviceException 
	 * @throws IOException 
	 * @throws DataInconsitsentException 
	 * @throws DataTypeException 
	 */
	public static RecordSet read(String filePath, IDevice device, String recordNameExtend, Integer channelConfigNumber) throws NotSupportedFileFormatException, IOException, DataInconsitsentException,
			DataTypeException {
		String sThreadId = String.format("%06d", Thread.currentThread().getId()); //$NON-NLS-1$
		String line = GDE.STRING_STAR, lastLine;
		RecordSet recordSet = null;
		BufferedReader reader; // to read the data
		Channel activeChannel = null;
		int lineNumber = 1;
		int activeChannelConfigNumber = 1; // at least each device needs to have one channelConfig to place record sets
		String recordSetNameExtend = device.getRecordSetStemName();
		long timeStamp = -1, actualTimeStamp = -1, startTimeStamp = -1;
		StringBuilder header = new StringBuilder();
		String date = "000000", time; //16 02 40
		int hour, minute, second;
		int latitude, longitude, altitude, height;
		int values[] = new int[] {0,0,0,0};
		File inputFile = new File(filePath);
		boolean isGsentence = false;
		String dllID = "XXX";
		if (IGCReaderWriter.application.getStatusBar() != null) IGCReaderWriter.application.setProgress(0, sThreadId);

		try {			
			if (channelConfigNumber == null)
				activeChannel = IGCReaderWriter.channels.getActiveChannel();
			else
				activeChannel = IGCReaderWriter.channels.get(channelConfigNumber);

			if (activeChannel != null) {
				if (IGCReaderWriter.application.getStatusBar() != null) IGCReaderWriter.application.setStatusMessage(Messages.getString(MessageIds.GDE_MSGT0594) + filePath);
				activeChannelConfigNumber = activeChannel.getNumber();

				if (IGCReaderWriter.application.getStatusBar() != null) {
					IGCReaderWriter.channels.switchChannel(activeChannelConfigNumber, GDE.STRING_EMPTY);
					IGCReaderWriter.application.getMenuToolBar().updateChannelSelector();
					activeChannel = IGCReaderWriter.channels.getActiveChannel();
				}
				String recordSetName = (activeChannel.size() + 1) + ") " + recordSetNameExtend; //$NON-NLS-1$
				int measurementSize = device.getNumberOfMeasurements(activeChannelConfigNumber);
				int dataBlockSize = Math.abs(device.getDataBlockSize(InputTypes.FILE_IO)); // measurements size must not match data block size, there are some measurements which are result of calculation			
				log.log(java.util.logging.Level.FINE, "measurementSize = " + measurementSize + "; dataBlockSize = " + dataBlockSize); //$NON-NLS-1$ //$NON-NLS-2$
				if (measurementSize < dataBlockSize) {
					dataBlockSize = measurementSize;
				}

				long approximateLines = inputFile.length()/35; //B sentence is the most used one and has 35 bytes
				reader = new BufferedReader(new InputStreamReader(new FileInputStream(filePath), "ISO-8859-1")); //$NON-NLS-1$						

				//skip all lines which describe the hardware, pilot and plane, save as header
				while ((line = reader.readLine()) == null || !line.startsWith(device.getDataBlockLeader())) {
					if (line != null) {
						if (line.startsWith("HFDTE")) { //160701	UTC date of flight, here 16th July 2001
							date = line.substring(5).trim();
						}
						//do not care about UTC time offset, the OLE/IGC server will do instead
						//else if (line.startsWith("HFTZNTIMEZONE")) {
						//	timeOffsetUTC = (int) Double.parseDouble(line.substring(14).trim());
						//}
						else if (line.startsWith("H")) {
							header.append(line).append(GDE.LINE_SEPARATOR);
						}
						else if (line.startsWith("A")) { // first line contains manufacturer identifier
							dllID = line.substring(1, 4);
							log.log(Level.FINE, "IGCDLL iddentifier = " + dllID);
						}
					}
					++lineNumber;
				}
				//calculate the start time stamp using the first B record
				int year = Integer.parseInt(date.substring(4)) + 2000;
				int month = Integer.parseInt(date.substring(2, 4));
				int day = Integer.parseInt(date.substring(0, 2));
				time = line.substring(1, 7); //16 02 40
				hour = Integer.parseInt(time.substring(0, 2));
				minute = Integer.parseInt(time.substring(2, 4));
				second = Integer.parseInt(time.substring(4, 6));
				startTimeStamp = new GregorianCalendar(year, month - 1, day, hour, minute, second).getTimeInMillis();
								
				//parse B records B160240 5407121N 00249342W A 00280 00421
				do {
					lastLine = line = line.trim();
					++lineNumber;
					if (line.length() >= 35 && line.startsWith(device.getDataBlockLeader())) {
						time = line.substring(1, 7); //16 02 40
						 if (hour > Integer.parseInt(time.substring(0, 2))) 
							 ++day; // switch to next day if 12 -> 0 0r 23 -> 0
						hour = Integer.parseInt(time.substring(0, 2));
						minute = Integer.parseInt(time.substring(2, 4));
						second = Integer.parseInt(time.substring(4, 6));
						actualTimeStamp = new GregorianCalendar(year, month - 1, day, hour, minute, second).getTimeInMillis();

						int progress = (int) (lineNumber*100/approximateLines);
						if (IGCReaderWriter.application.getStatusBar() != null && progress % 5 == 0) 	IGCReaderWriter.application.setProgress(progress, sThreadId);
						
						if (device.getStateType() == null) 
							throw new DevicePropertiesInconsistenceException(Messages.getString(MessageIds.GDE_MSGE0043, new Object[] { device.getPropertiesFileName() }));

						try {
							recordSetNameExtend = device.getStateType().getProperty().get(0).getName(); // state name
							if (recordNameExtend.length() > 0) {
								recordSetNameExtend = recordSetNameExtend + GDE.STRING_BLANK + GDE.STRING_LEFT_BRACKET + recordNameExtend + GDE.STRING_RIGHT_BRACKET;
							}
						}
						catch (Exception e) {
							throw new DevicePropertiesInconsistenceException(Messages.getString(MessageIds.GDE_MSGE0044, new Object[] { 0, filePath, device.getPropertiesFileName() }));
						}

						//detect states where a new record set has to be created
						if (recordSet == null || !recordSet.getName().contains(recordSetNameExtend)) {
							//prepare new record set now
							recordSetName = (activeChannel.size() + 1) + ") " + recordSetNameExtend; //$NON-NLS-1$

							recordSet = RecordSet.createRecordSet(recordSetName, device, activeChannel.getNumber(), true, true);
							recordSetName = recordSet.getName(); // cut/correct length
							String dateTime = new SimpleDateFormat("yyyy-MM-dd, HH:mm:ss").format(actualTimeStamp); //$NON-NLS-1$
							String description = device.getName() + GDE.STRING_MESSAGE_CONCAT + Messages.getString(MessageIds.GDE_MSGT0129) + dateTime 
									+ GDE.LINE_SEPARATOR + header.toString();
							recordSet.setRecordSetDescription(description);
							activeChannel.setFileDescription(dateTime.substring(0, 10) + (activeChannel.getFileDescription().length() < 11 ? "" : activeChannel.getFileDescription().substring(10)));

							activeChannel.put(recordSetName, recordSet);
							
							activeChannel.get(recordSetName).setStartTimeStamp(actualTimeStamp);
							activeChannel.setActiveRecordSet(recordSetName);
							activeChannel.applyTemplate(recordSetName, true);
						}

						if (timeStamp < actualTimeStamp) {
							//B160240 5407121N 00249342W A 00280 00421
							//0123456 78901234 567890123 4 56789 01234
							try {
								latitude = Integer.valueOf(line.substring(7, 14))*10;
								latitude = line.substring(14, 15).equalsIgnoreCase("N") ? latitude : -1 * latitude;  //$NON-NLS-1$
							}
							catch (Exception e) {
								latitude = values[0];
							}
							try {
								longitude = Integer.valueOf(line.substring(15, 23))*10;
								longitude = line.substring(23, 24).equalsIgnoreCase("E") ? longitude : -1 * longitude;  //$NON-NLS-1$
							}
							catch (Exception e) {
								longitude = values[1];
							}
							try {
								altitude = Integer.valueOf(line.substring(25, 30))*1000;
							}
							catch (Exception e) {
								altitude = values[2];
							}
							try {
								height = Integer.valueOf(line.substring(31, 35))*1000;
							}
							catch (Exception e) {
								height = values[3];
							}
							values[0] = latitude;
							values[1] = longitude;
							values[2] = altitude;
							values[3] = height;
							
							recordSet.addNoneCalculationRecordsPoints(values, actualTimeStamp-startTimeStamp);
						}
						timeStamp = actualTimeStamp;
					}
					else if (line.startsWith("G")) {
						isGsentence = true;
						log.log(Level.FINE, "line number " + lineNumber + " contains security code and is voted as last line! " + lastLine); //$NON-NLS-1$ //$NON-NLS-2$
						break;
					}
					else {
						log.log(Level.WARNING, "line number " + lineNumber + " line length to short or missing " + device.getDataBlockLeader() + " !"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						continue;
					}
				}
				while ((line = reader.readLine()) != null);

				device.updateVisibilityStatus(activeChannel.get(recordSetName), true);
				activeChannel.get(recordSetName).checkAllDisplayable(); // raw import needs calculation of passive records
				if (IGCReaderWriter.application.getStatusBar() != null) activeChannel.switchRecordSet(recordSetName);

				reader.close();
				reader = null;
				

				if (GDE.IS_WINDOWS && isGsentence && GDE.BIT_MODE.equals("32")) {
					if (IGCDLL.loadIgcDll(dllID)) {
						System.out.println("verification = " + IGCDLL.validateLog(filePath));
					}
				}
			}
		}
		catch (FileNotFoundException e) {
			log.log(java.util.logging.Level.WARNING, e.getMessage(), e);
			IGCReaderWriter.application.openMessageDialog(e.getMessage());
		}
		catch (IOException e) {
			log.log(java.util.logging.Level.WARNING, e.getMessage(), e);
			IGCReaderWriter.application.openMessageDialog(e.getMessage());
		}
		catch (Exception e) {
			// check if previous records are available and needs to be displayed
			if (activeChannel != null && activeChannel.size() > 0) {
				String recordSetName = activeChannel.getFirstRecordSetName();
				activeChannel.setActiveRecordSet(recordSetName);
				device.updateVisibilityStatus(activeChannel.get(recordSetName), true);
				activeChannel.get(recordSetName).checkAllDisplayable(); // raw import needs calculation of passive records
				if (IGCReaderWriter.application.getStatusBar() != null) activeChannel.switchRecordSet(recordSetName);
			}
			// now display the error message
			String msg = filePath + GDE.STRING_MESSAGE_CONCAT + Messages.getString(MessageIds.GDE_MSGE0045, new Object[] { e.getMessage(), lineNumber });
			log.log(java.util.logging.Level.WARNING, msg, e);
			IGCReaderWriter.application.openMessageDialog(msg);
		}
		finally {
			if (IGCReaderWriter.application.getStatusBar() != null) {
				IGCReaderWriter.application.setProgress(100, sThreadId);
				IGCReaderWriter.application.setStatusMessage(GDE.STRING_EMPTY);
			}
		}

		return recordSet;
	}

	/**
	 * write the IGC header and way points
	 * Short file name: 36HXABC2.IGC
	 * Long file name: 2003-06-17-XXX-ABC-02.IGC
	 * where XXX is the manufacturer's three-letter IGC identifier ABC is the IGC serial number/letters of the individual recorder.
	 * The following records are mandatory for an IGC file from an IGC-approved FR:
	 * A - Manufacturer and unique ID for FR
	 * H - Header record
	 * I - FXA addition to B-record, ENL for motor gliders
	 * B - Fix record (lat/long/alt etc.)
	 * F - Satellites used in B record fixes
	 * G - Security record
	 * Use: + for N Lat or E Long     - for S Lat or W Long.
	 * @param device
	 * @param igcFilePath
	 * @param header
	 * @param recordSet
	 * @param ordinalLongitude
	 * @param ordinalLatitude
	 * @param ordinalAltitude
	 * @param startAltitude
	 * @param offsetUTC
	 * @throws Exception
	 */
	public static void write(IDevice device, String igcFilePath, StringBuilder header, RecordSet recordSet, final int ordinalLongitude, final int ordinalLatitude, final int ordinalAltitude,
			final int startAltitude) throws Exception {
		BufferedWriter writer;
		StringBuilder content = new StringBuilder().append(header);
		String sThreadId = String.format("%06d", Thread.currentThread().getId()); //$NON-NLS-1$
		long startTime = new Date().getTime();

		try {
			if (IGCReaderWriter.application.getStatusBar() != null) IGCReaderWriter.application.setStatusMessage(Messages.getString(MessageIds.GDE_MSGT0138, new String[] { GDE.FILE_ENDING_IGC, igcFilePath }));

			if (recordSet != null) {
				int startIndex = GPSHelper.getStartIndexGPS(recordSet, ordinalLatitude, ordinalLongitude);
				Record recordAlitude = recordSet.get(ordinalAltitude);
				SimpleDateFormat sdf = new SimpleDateFormat("HHmmss"); //$NON-NLS-1$
				int offsetHeight = (int) (startAltitude - device.translateValue(recordAlitude, recordAlitude.get(startIndex) / 1000.0));
				char fixValidity = offsetHeight == 0 ? 'A' : 'V'; //$NON-NLS-1$ //$NON-NLS-2$
				long lastTimeStamp = -1, timeStamp;
				long recordSetStartTimeStamp = recordSet.getStartTimeStamp();
				log.log(Level.TIME, "start time stamp = " + StringHelper.getFormatedTime("yyyy-MM-dd HH:mm:ss", recordSetStartTimeStamp));
				
				for (int i = startIndex; startIndex >= 0 && i < recordSet.get(ordinalLongitude).realSize(); i++) {
					// absolute time as recorded, needs to be converted into UTC
					timeStamp = recordSet.getTime(i) / 10 + recordSetStartTimeStamp;
					if ((timeStamp - lastTimeStamp) >= 950 || lastTimeStamp == -1) {
						content.append(String.format("B%s%s\r\n", sdf.format(timeStamp), device.translateGPS2IGC(recordSet, i, fixValidity, startAltitude, offsetHeight))); //$NON-NLS-1$

						lastTimeStamp = timeStamp;
					}
				}
			}

			writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(igcFilePath), "ISO-8859-1")); //$NON-NLS-1$
			writer.append(content.toString());
			writer.flush();
			writer.close();
			writer = null;
			//recordSet.setSaved(true);
			if (IGCReaderWriter.application.getStatusBar() != null) IGCReaderWriter.application.setProgress(100, sThreadId);
		}
		catch (RuntimeException e) {
			IGCReaderWriter.log.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
			throw new Exception(Messages.getString(MessageIds.GDE_MSGE0007) + e.getClass().getSimpleName() + GDE.STRING_MESSAGE_CONCAT + e.getMessage());
		}
		finally {
			if (IGCReaderWriter.application.getStatusBar() != null) IGCReaderWriter.application.setStatusMessage(GDE.STRING_EMPTY);
		}
		if (log.isLoggable(Level.TIME)) log.log(Level.TIME, "IGC file = " + igcFilePath + " written successfuly" //$NON-NLS-1$ //$NON-NLS-2$
				+ "write time = " + StringHelper.getFormatedTime("ss:SSS", (new Date().getTime() - startTime)));//$NON-NLS-1$ //$NON-NLS-2$
	}
}