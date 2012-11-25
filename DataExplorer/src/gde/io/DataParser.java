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
package gde.io;

import gde.GDE;
import gde.device.CheckSumTypes;
import gde.device.FormatTypes;
import gde.exception.DevicePropertiesInconsistenceException;
import gde.log.Level;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.utils.Checksum;

import java.util.logging.Logger;

/**
 * Class to parse comma separated input line from a comma separated textual line which simulates serial data 
 * one data line consist of $1;1;0; 14780;  598;  1000;  8838;.....;0002;
 * where $recordSetNumber; stateNumber; timeStepSeconds; firstIntValue; secondIntValue; .....;checkSumIntValue;
 * All properties around the textual data in this line has to be specified in DataBlockType (type=TEXT, size number of values, separator=;, ...), refer to DeviceProperties_XY.XSD
 * @author Winfried Brügmann
 */
public class DataParser extends NMEAParser {
	static Logger					log			= Logger.getLogger(DataParser.class.getName());

	int									start_time_ms = Integer.MIN_VALUE;
	int									valueSize;
	
	int									recordSetNumberOffset = 0;
	int									timeResetCounter	= 0;
	boolean							isTimeResetEnabled				= false;
	
	final int						timeFactor;
	final FormatTypes		checkSumFormatType;
	final FormatTypes		dataFormatType;
	final boolean				isMultiply1000;

	/**
	 * constructor to initialize required configuration parameter
	 * assuming checkSumFormatType == FormatTypes.TEXT to checksum is build using contained values
	 * dataFormatType == FormatTypes.TEXT where dataBlockSize specifies the number of contained values (file input)
	 * @param useTimeFactor
	 * @param useLeaderChar
	 * @param useSeparator
	 * @param useCheckSumType if null, no checksum calculation will occur
	 * @param useDataSize
	 */
	public DataParser(int useTimeFactor, String useLeaderChar, String useSeparator, CheckSumTypes useCheckSumType, int useDataSize) {
		super(useLeaderChar, useSeparator, useCheckSumType, useDataSize, DataExplorer.getInstance().getActiveDevice(), DataExplorer.getInstance().getActiveChannelNumber(), (short) 0);
		this.timeFactor = useTimeFactor;
		this.checkSumFormatType = FormatTypes.VALUE; //checksum is build using contained values
		this.dataFormatType = FormatTypes.VALUE; //dataBlockSize specifies the number of contained values
		this.isMultiply1000 = true;
	}
	
	/**
	 * constructor to initialize required configuration parameter
	 * a 
	 * @param useTimeFactor
	 * @param useLeaderChar
	 * @param useSeparator
	 * @param useCheckSumType 
	 * @param useCheckSumFormatType if null, no checksum calculation will occur
	 * @param useDataSize
	 * @param useDataFormatType FormatTypes.TEXT where dataBlockSize specifies the number of contained values, FormatTypes.BINARY where dataBlockSize specifies the number of bytes received via seral connection
	 * @param doMultiply1000 some transferred values are already in a format required to enable 3 digits 
	 */
	public DataParser(int useTimeFactor, String useLeaderChar, String useSeparator, CheckSumTypes useCheckSumType, FormatTypes useCheckSumFormatType, int useDataSize, FormatTypes useDataFormatType, boolean doMultiply1000) {
		super(useLeaderChar, useSeparator, useCheckSumType, useDataSize, DataExplorer.getInstance().getActiveDevice(), DataExplorer.getInstance().getActiveChannelNumber(), (short) 0);
		this.timeFactor = useTimeFactor;
		this.checkSumFormatType = useCheckSumFormatType;
		this.dataFormatType = useDataFormatType; //dataBlockSize specifies the number of contained values if FormatTypes.TEXT else FormatTypes.BINARY the contained byte size
		this.isMultiply1000 = doMultiply1000;
	}

	@Override
	public void parse(String inputLine, int lineNum) throws DevicePropertiesInconsistenceException, Exception {
		try {			
			String[] strValues = inputLine.split(this.separator); // {$1, 1, 0, 14780, 0,598, 1,000, 8,838, 22}
			try {
				Integer.parseInt(strValues[0].substring(1).trim());
				this.valueSize = this.dataFormatType != null && this.dataFormatType == FormatTypes.BINARY ? strValues.length-4 
						: this.dataFormatType != null && this.dataFormatType == FormatTypes.VALUE &&  this.dataBlockSize > 0 ? Math.abs(this.dataBlockSize) : strValues.length-4;
				this.values = new int[this.valueSize];
				log.log(Level.FINER, "parser inputLine = " + inputLine); //$NON-NLS-1$
				if (strValues.length-4 != this.valueSize)  throw new DevicePropertiesInconsistenceException(Messages.getString(MessageIds.GDE_MSGE0048, new String[] {inputLine}));
				
				parse(inputLine, strValues);
			}
			catch (RuntimeException e) {
				//Multiplex FlightRecorder
				super.parse(inputLine, lineNum);
			}
		}
		catch (NumberFormatException e) {
			log.log(Level.WARNING, e.getMessage(), e);
			throw e;
		}
	}

	/**
	 * default parse method for $1, 1, 0, 14780, 0,598, 1,000, 8,838, 22 like lines
	 * @param inputLine
	 * @param strValues
	 * @throws DevicePropertiesInconsistenceException
	 */
	@Override
	public void parse(String inputLine, String[] strValues) throws DevicePropertiesInconsistenceException {
		String strValue = strValues[0].trim().substring(1);
		this.channelConfigNumber = Integer.parseInt(strValue);
		
		strValue = strValues[1].trim();
		this.state = Integer.parseInt(strValue);

		strValue = strValues[2].trim().replace(GDE.STRING_COMMA, GDE.STRING_DOT);
		strValue = strValue.length() > 0 ? strValue : "0";
		if (start_time_ms == Integer.MIN_VALUE)	{
			start_time_ms = (int) (Double.parseDouble(strValue) * this.timeFactor); // Seconds * 1000 = msec
		}
		else {
			if (deviceName.startsWith("JLog") && time_ms >= 3276700) {
				//JLog2 workaround maximum logging time of 54 minutes
				time_ms = time_ms + 100;
			}
			else 
				time_ms = (int) (Double.parseDouble(strValue) * this.timeFactor) - start_time_ms; // Seconds * 1000 = msec			
		}
		
		for (int i = 0; i < this.valueSize; i++) { 
			strValue = strValues[i+3].trim();
			try {
				long tmpValue = strValue.length() > 0 ? Long.parseLong(strValue) : 0;
				if (isMultiply1000 && tmpValue < Integer.MAX_VALUE/1000 && tmpValue > Integer.MIN_VALUE/1000)
					this.values[i] = (int) (tmpValue*1000); // enable 3 positions after decimal place
				else // needs special processing within IDevice.translateValue(), IDevice.reverseTranslateValue()
					if (tmpValue < Integer.MAX_VALUE || tmpValue > Integer.MIN_VALUE) {
						this.values[i] = (int) tmpValue;
					}
					else {
						this.values[i] = (int) (tmpValue/1000);
					}
			}
			catch (NumberFormatException e) {
				this.values[i] = 0;
			}
		}
		
		//check time reset to force a new data set creation
		if (this.device.getTimeStep_ms() < 0 && time_ms <= 0 && this.isTimeResetEnabled) {
				this.recordSetNumberOffset += ++this.timeResetCounter;
				this.isTimeResetEnabled = false;
		}

		if (checkSumType != null) {
			boolean isValid = isChecksumOK(inputLine, Integer.parseInt(strValues[strValues.length-1].trim()));
			if (!isValid) {
				DevicePropertiesInconsistenceException e = new DevicePropertiesInconsistenceException(Messages.getString(MessageIds.GDE_MSGE0049, new String[] {strValue})); 
				log.log(Level.WARNING, e.getMessage(), e);
				throw e;
			}
		}
	}

	/**
	 * check the checksum with configured checksum algorithm
	 * @param inputLine
	 * @param tmpCheckSum
	 * @return
	 */
	public boolean isChecksumOK(String inputLine, int tmpCheckSum) {
		boolean isValid = true;
			switch (checkSumType) {
			case ADD:
				switch (checkSumFormatType) {
				case VALUE:
					isValid = tmpCheckSum == Checksum.ADD(this.values, 0, this.valueSize);
					break;
				case BINARY:
				default:
					isValid = tmpCheckSum == Checksum.ADD(inputLine.substring(0, inputLine.lastIndexOf(this.separator) + 1).getBytes());
					break;
				}
				break;
			case XOR:
				switch (checkSumFormatType) {
				case VALUE:
					isValid = tmpCheckSum == Checksum.XOR(this.values, 0, this.valueSize);
					break;
				case BINARY:
				default:
					isValid = tmpCheckSum == Checksum.XOR(inputLine.substring(0, inputLine.lastIndexOf(this.separator) + 1).getBytes());
					break;
				}
				break;
			case OR:
				switch (checkSumFormatType) {
				case VALUE:
					isValid = tmpCheckSum == Checksum.OR(this.values, 0, this.valueSize);
					break;
				case BINARY:
				default:
					isValid = tmpCheckSum == Checksum.OR(inputLine.substring(0, inputLine.lastIndexOf(this.separator) + 1).getBytes());
					break;
				}
				break;
			case AND:
				switch (checkSumFormatType) {
				case VALUE:
					isValid = tmpCheckSum == Checksum.AND(this.values, 0, this.valueSize);
					break;
				case BINARY:
				default:
					isValid = tmpCheckSum == Checksum.AND(inputLine.substring(0, inputLine.lastIndexOf(this.separator) + 1).getBytes());
					break;
				}
				break;
			}
		return isValid;
	}

	/**
	 * @return the recordSetNumberOffset
	 */
	public int getRecordSetNumberOffset() {
		return recordSetNumberOffset;
	}

	/**
	 * @return the state
	 */
	public int getState() {
		return state;
	}

	/**
	 * parse 4 byte of a data buffer to integer value
	 * @param buffer
	 * @param startIndex index of low byte
	 */
	public static int parse2Int(byte[] buffer, int startIndex) {
		return (((buffer[startIndex+3] & 0xff) << 24) | ((buffer[startIndex+2] & 0xff) << 16) | ((buffer[startIndex+1] & 0xff) << 8) | (buffer[startIndex] & 0xff));
	}
	
	/**
	 * parse 2 byte of a data buffer to short integer value, buffer byte sequence low byte high byte
	 * @param buffer
	 * @param startIndex index of low byte 
	 */
	public static short parse2Short(byte[] buffer, int startIndex) {
		return (short) (((buffer[startIndex+1] & 0xff) << 8) | (buffer[startIndex] & 0xff));
	}
	
	/**
	 * parse 2 byte of a data buffer to integer value, buffer byte sequence low byte high byte
	 * @param buffer
	 * @param startIndex index of low byte 
	 */
	public static int parse2UnsignedShort(byte[] buffer, int startIndex) {
		return ((buffer[startIndex+1] & 0xff) << 8) | (buffer[startIndex] & 0xff);
	}
	
	/**
	 * parse high and low byte to short integer value
	 * @param low byte
	 * @param high byte
	 */
	public static short parse2Short(byte low, byte high) {
		return (short) (((high & 0xFF) << 8) | (low & 0xFF));
	}
	
	/**
	 * parse high and low byte to short integer value
	 * @param low byte
	 * @param high byte
	 */
	public static int parse2UnsignedShort(byte low, byte high) {
		return ((high & 0xFF) << 8) | (low & 0xFF);
	}

	/**
	 * @param isTimeResetPrepared the isTimeResetPrepared to set
	 */
	public synchronized void setTimeResetEnabled(boolean isTimeResetPrepared) {
		this.isTimeResetEnabled = isTimeResetPrepared;
	}
}
