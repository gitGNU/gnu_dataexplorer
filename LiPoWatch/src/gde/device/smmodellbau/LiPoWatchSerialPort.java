package osde.device.smmodellbau;

import java.io.IOException;
import java.util.HashMap;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;

import osde.device.DeviceConfiguration;
import osde.exception.CheckSumMissmatchException;
import osde.exception.TimeOutException;
import osde.messages.Messages;
import osde.serial.DeviceSerialPort;
import osde.ui.OpenSerialDataExplorer;
import osde.ui.SWTResourceManager;
import osde.utils.Checksum;

/**
 * Sample serial port implementation, used as template for new device implementations
 * @author Winfried Brügmann
 */
public class LiPoWatchSerialPort extends DeviceSerialPort {
	final static Logger	log	= Logger.getLogger(LiPoWatchSerialPort.class.getName());

	public final static String 		NUMBER_RECORD						= "number_record"; 	//$NON-NLS-1$
	public final static String 		TIME_MILLI_SEC					= "time_ms"; 				//$NON-NLS-1$
	
	final static byte[]		COMMAND_QUERY_STATE			= { 0x54 };		// 'T' query LiPoWatch state
	final static byte[]		COMMAND_RESET						= { 0x72 };		// 'r' reset LiPoWatch to repeat data send (from the begin)
	final static byte[]		COMMAND_READ_DATA				= { 0x6C };		// 'l' LiPoWatch read request, answer is one data set (telegram)
	final static byte[]		COMMAND_REPEAT					= { 0x77 };		// 'w' repeat data transmission, LiPoWatch re-send same data set (telegram)
	final static byte[]		COMMAND_DELETE					= { (byte) 0xC0, 0x04, 0x01, 0x03, 0x08 };
	final static byte[]		COMMAND_QUERY_CONFIG		= { (byte) 0xC0, 0x04, 0x01, 0x01, 0x06 };
	final static byte[]		COMMAND_LIVE_VALUES			= { 0x76 };		// 'v' query LiPoWatch live values
	final static byte[]		COMMAND_START_LOGGING		= { 0x53 };		// 'S' start logging data
	final static byte[]		COMMAND_STOP_LOGGING		= { 0x73 };		// 's' stop logging data
	final static byte[]		COMMAND_BEGIN_XFER			= { (byte) 0xC0 };	// begin data transfer
	final static byte[]		COMMAND_END_XFER				= { 0x45 };					// 'E' end data transfer

	final static byte[]		COMMAND_PREPARE_DELETE			= { 0x78, 0x79, 0x31 };					// "xy1"
	final static byte[]		COMMAND_PREPARE_SET_CONFIG	= { 0x78, 0x79, (byte) 0xA7 };	// "xyz"
	
	final static byte			DATA_STATE_WAITING			= 0x57;		// 'W' LiPoWatch connected, needs some time to organize flash
	final static byte			DATA_STATE_READY				= 0x46;		// 'F' LiPoWatch ready to receive command
	final static byte			DATA_STATE_OK						= 0x6A;		// 'j' operation successful ended

	final static int			DATA_LENGTH_BYTES				= 47;			
	
	boolean 							isLoggingActive 				= false;
	boolean 							isTransmitFinished			= false;
	
	int 									reveiceErrors 					= 0;

	/**
	 * constructor of default implementation
	 * @param currentDeviceConfig - required by super class to initialize the serial communication port
	 * @param currentApplication - may be used to reflect serial receive,transmit on/off status or overall status by progress bar 
	 */
	public LiPoWatchSerialPort(DeviceConfiguration currentDeviceConfig, OpenSerialDataExplorer currentApplication) {
		super(currentDeviceConfig, currentApplication);
	}

	/**
	 * method to gather data from device, implementation is individual for device
	 * @param dialog to update displays at device dialog like progress bar, ...
	 * @return map containing gathered data - this can individual specified per device
	 * @throws Exception
	 */
	public HashMap<String, Object> getData(LiPoWatchDialog dialog) throws Exception {
		boolean isPortOpenedByMe = false;
		HashMap<String, Object> dataCollection = new HashMap<String, Object>();
		
		byte[] readBuffer = new byte[DATA_LENGTH_BYTES + 2];
		
		try {
			log.log(Level.FINE, "start"); //$NON-NLS-1$
			if (!this.isConnected()) {
				this.open();
				isPortOpenedByMe = true;
			}
			this.reveiceErrors = 0;

			// check data ready for read operation
			if (this.waitDataReady()) {
				// query configuration to have actual values -> get number of entries to calculate percentage and progress bar
				readBuffer = this.readConfiguration();
				int memoryUsed = ((readBuffer[8] & 0xFF) << 24) + ((readBuffer[7] & 0xFF) << 16) + ((readBuffer[6] & 0xFF) << 8) + (readBuffer[7] & 0xFF);
				log.log(Level.INFO, "memoryUsed = " + memoryUsed); //$NON-NLS-1$
				double progressFactor = 100.0 / memoryUsed;
				log.log(Level.INFO, "progressFactor = " + progressFactor); //$NON-NLS-1$
				
				// reset data and prepare for read
				this.write(COMMAND_RESET);

				dialog.setReadDataProgressBar(0);
				Vector<byte[]> telegrams = new Vector<byte[]>();
				int numberRecordSet = 0;
				int redCounter = 0;
				int dataLength = 1;
				int dataSetType = 1;
				
				while ((memoryUsed-=(dataLength-7)) > 0) {
					readBuffer = readSingleTelegramm();

					dataLength = (readBuffer[0] & 0x7F); 
					//Datensatztyp = Asc(Mid(strResult, 10, 1)) And &HF
					dataSetType = (readBuffer[9] & 0x0F);
					if (dataSetType == 0) { // normal data set type
						
						//Zeit = (CLng(Asc(Mid(strResult, 5, 1))) * 256 * 256 * 256 + CLng(Asc(Mid(strResult, 4, 1))) * 256 * 256 + CLng(Asc(Mid(strResult, 3, 1))) * 256 + Asc(Mid(strResult, 2, 1))) / 1000
						int time_ms =  (((readBuffer[4] & 0xFF) << 24) + ((readBuffer[3] & 0xFF) << 16) + ((readBuffer[2] & 0xFF) << 8) + (readBuffer[1] & 0xFF));

						// number record set
						//Datensatznummer = CLng(Asc(Mid(strResult, 11, 1))) + 1
						if(numberRecordSet == ((readBuffer[10] & 0xFF) + 1)) {
							telegrams.add(readBuffer);
						}
						else {
							//telegrams.size() > 4 min + max + 2 data points
							if (telegrams.size() > 4) dataCollection.put(""+numberRecordSet, telegrams.clone()); //$NON-NLS-1$
							numberRecordSet = ((readBuffer[10] & 0xFF) + 1);
							telegrams = new Vector<byte[]>();
						}
						log.log(Level.INFO, "numberRecordSet = " + numberRecordSet + " time_ms = " + time_ms + " memoryUsed = " + memoryUsed); //$NON-NLS-1$ //$NON-NLS-2$						
						
						
						redCounter+=(dataLength-7);

						//"Gelesene Datensätze/Werte: " & Datensatznummer & "/" & Werte_gelesen & " von " & Speichernummer & " (" & CInt(CLng(Werte_gelesen) * 100 / Speichernummer) & "%)" ' & " (" & Fehlersumme & ")"
						if ((redCounter % 5) == 0) dialog.updateDataGatherProgress(redCounter, numberRecordSet, this.reveiceErrors, new Double(redCounter * progressFactor).intValue());

						if (this.isTransmitFinished) {
							log.log(Level.WARNING, "transmission stopped by user"); //$NON-NLS-1$
							break;
						}
					}
					else { // no data telegram received
						redCounter+=(dataLength-7);
					}
					if (telegrams.size() > 4) dataCollection.put("" + numberRecordSet, telegrams.clone()); //$NON-NLS-1$
				}
			}
			else
				throw new IOException(Messages.getString(osde.messages.MessageIds.OSDE_MSGE0026));
			
			log.log(Level.FINE, "end"); //$NON-NLS-1$
		}
		catch (Exception e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			throw e;
		}
		finally {
			if(isPortOpenedByMe) this.close();
			log.log(Level.FINE, "stop"); //$NON-NLS-1$
		}
		return dataCollection;
	}

	/**
	 * read a single telegram to enable live view of measurements
	 * @return raw byte array of received data
	 * @throws Exception
	 */
	public synchronized byte[] readSingleTelegramm() throws Exception {
		byte[] tmp1ReadBuffer = new byte[1], tmp2ReadBuffer, readBuffer;
		
		try {
			this.write(COMMAND_READ_DATA);
			tmp1ReadBuffer = this.read(tmp1ReadBuffer, 2000);
			
			int length = (tmp1ReadBuffer[0] & 0x7F);    // höchstes Bit steht für Einstellungen, sonst Daten
			log.log(Level.INFO, "length = " + length); //$NON-NLS-1$
			tmp2ReadBuffer = new byte[length-1];
			tmp2ReadBuffer = this.read(tmp2ReadBuffer, 2000);
			
			readBuffer = new byte[length];
			readBuffer[0] = tmp1ReadBuffer[0];
			System.arraycopy(tmp2ReadBuffer, 0, readBuffer, 1, length-1);
			
			// give it another try
			if (!isChecksumOK(readBuffer)) {
				++this.reveiceErrors;
				this.write(COMMAND_REPEAT);
				readBuffer = this.read(readBuffer, 2000);
				verifyChecksum(readBuffer); // throws exception if checksum miss match
			}
		}
		catch (IOException e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			throw e;
		}
		
		return readBuffer;
	}
	
	/**
	 * wait while LiPoWatch answers with data by a given retry count, the wait time between retries is 250 ms
	 * @param retrys number time 250 ms is maximum time
	 * @return true, if data can received after the adjusted time period
	 * @throws Exception
	 */
	public synchronized boolean wait4LifeData(int retrys) throws Exception {
		boolean isLifeDataAvailable = false;
		if (this.isConnected()) {
			this.application.setCursor(SWTResourceManager.getCursor(SWT.CURSOR_WAIT));
			while (this.getInputStream().available() < 10 && retrys-- > 0) {
				this.write(COMMAND_LIVE_VALUES);
				Thread.sleep(250);
				log.log(Level.FINE, "retryLimit = " + retrys); //$NON-NLS-1$
			}
			// read data bytes to clear buffer
			this.read(new byte[DATA_LENGTH_BYTES], 1000);
			isLifeDataAvailable = true;
			
			this.application.setCursor(SWTResourceManager.getCursor(SWT.CURSOR_ARROW));
		}
		else
			throw new Exception(Messages.getString(osde.messages.MessageIds.OSDE_MSGE0031));
		
		return isLifeDataAvailable;
	}
	
	/**
	 * enable live data view, timer loop must gather the data which also handles open/close operations
	 * @return byte array with red data
	 * @throws Exception
	 */
	public synchronized byte[] queryLiveData() throws Exception {
		byte[] readBuffer = new byte[DATA_LENGTH_BYTES];
		
		if (this.isConnected()) {
			try {
				this.write(COMMAND_LIVE_VALUES);
				readBuffer = this.read(readBuffer, 1000);

				// give it another try
				if (!isChecksumOK(readBuffer)) {
					this.write(COMMAND_LIVE_VALUES);
					readBuffer = this.read(readBuffer, 1000);
					verifyChecksum(readBuffer); // throws exception if checksum miss match
				}
			}
			catch (Exception e) {
				log.log(Level.SEVERE, e.getMessage(), e);
				throw e;
			}
		}
		else
			throw new Exception(Messages.getString(osde.messages.MessageIds.OSDE_MSGE0031));

		return readBuffer;
	}
	
	/**
	 * start logging of LiPoWatch, open/close port operations must be handled outside
	 * @return true if logging is enabled
	 * @throws Exception
	 */
	public synchronized boolean startLogging() throws Exception {
		boolean isPortOpenedByMe = false;
		try {
			if (!this.isConnected()) {
				this.open();
				isPortOpenedByMe = true;
				//waitDataReady();
			}

			this.write(COMMAND_START_LOGGING);
			this.isLoggingActive = true;
		}
		catch (Exception e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			this.close();
			throw e;
		}
		finally {
			if (isPortOpenedByMe) this.close();
		}
		return this.isLoggingActive;
	}
	
	/**
	 * stop logging activity of LiPoWatch, open/close port operations must be handled outside
	 * @return true if logging is disabled
	 * @throws Exception
	 */
	public synchronized boolean stopLogging() throws Exception {
		boolean isPortOpenedByMe = false;
		try {
			if (!this.isConnected()) {
				this.open();
				isPortOpenedByMe = true;
				//waitDataReady();
			}

			this.write(COMMAND_STOP_LOGGING);
		}
		catch (Exception e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			this.close();
			throw e;
		}
		finally {
			if (isPortOpenedByMe) this.close();
		}
		return !this.isLoggingActive;
	}
	
	/**
	 * clears the flash memory of LiPoWatch
	 * @return true for successful operation
	 * @throws Exception
	 */
	public synchronized boolean clearMemory() throws Exception {
		boolean success = false;
		try {
			if (!this.isConnected()) { // port may not used by other
				this.open();
				// check data ready for read operation
				if (this.waitDataReady()) {
					//this.write(COMMAND_PREPARE_DELETE);
					this.write(COMMAND_DELETE);
					byte[] readBuffer = new byte[1];
					readBuffer = this.read(readBuffer, 5000);
					if (readBuffer[0] != DATA_STATE_OK) success = true;

				}
				else
					throw new IOException(Messages.getString(osde.messages.MessageIds.OSDE_MSGE0032));
			}
		}
		catch (Exception e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			throw e;
		}
		finally {
			this.close();
		}
		return success;
	}
	/**
	 * set LiPoWatch configuration with new values
	 * @param updateBuffer - byte array to be written
	 * @return true | false for state of the operation
	 * @throws Exception
	 */
	public synchronized boolean setConfiguration(byte[] updateBuffer) throws Exception {
		boolean success = false;
		boolean isPortOpenedByMe = false;
		
		try {
			if(!this.isConnected()) {
				this.open();
				isPortOpenedByMe = true;
			}
			// check device connected
			if (this.checkConnectionStatus()) {
				// check data ready for read operation
				if (this.checkDataReady()) {
					//this.write(COMMAND_PREPARE_SET_CONFIG);

					this.write(updateBuffer);
					byte[] readBuffer = new byte[1];
					readBuffer = this.read(readBuffer, 5000);
					if (readBuffer[0] == DATA_STATE_OK) success = true;
					this.write(COMMAND_END_XFER);
					
				}
				else
					throw new IOException(Messages.getString(osde.messages.MessageIds.OSDE_MSGE0032));
			}
			else
				throw new IOException(Messages.getString(osde.messages.MessageIds.OSDE_MSGE0033));
		}
		catch (Exception e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			throw e;
		}
		finally {
			if(isPortOpenedByMe)this.close();
		}
		return success;
	}
	
	/**
	 * query the configuration information from LiPoWatch
	 * @return byte array containing the configuration information 
	 * @throws Exception
	 */
	public synchronized byte[] readConfiguration() throws Exception {
		byte[] readBuffer = new byte[DATA_LENGTH_BYTES + 2];
		boolean isPortOpenedByMe = false;
		try {
			if(!this.isConnected()) {
				this.open();
				isPortOpenedByMe = true;
			}

			// check device connected
			if (this.checkConnectionStatus()) {
				// check data ready for read operation
				if (this.checkDataReady()) {

					this.write(COMMAND_QUERY_CONFIG);				
					this.read(readBuffer, 2000);
					verifyChecksum(readBuffer); // valid data set -> set values
					
				}
				else
					throw new IOException(Messages.getString(osde.messages.MessageIds.OSDE_MSGE0033));
			}
			else
				throw new IOException(Messages.getString(osde.messages.MessageIds.OSDE_MSGE0032));
		}
		catch (Exception e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			throw e;
		}
		finally {
			this.write(COMMAND_END_XFER);
			if(isPortOpenedByMe) this.close();
		}
		return readBuffer;
	}

	/**
	 * query if LiPoWatch is connected and capable for communication
	 * @return true/false
	 * @throws IOException
	 * @throws TimeOutException 
	 */
	public synchronized boolean checkConnectionStatus() throws IOException, TimeOutException {
		boolean isConnect = false;
		int counter = 50;

		while (!isConnect && counter-- > 0) {
			this.write(COMMAND_QUERY_STATE);
			byte[] buffer = new byte[1];
			try {
				Thread.sleep(50);
			}
			catch (InterruptedException e) {
				// ignore
			}
			buffer = this.read(buffer, 2000);
			if (buffer[0] == DATA_STATE_WAITING || buffer[0] == DATA_STATE_READY) {
				isConnect = true;
			}
		}
		return isConnect;
	}

	/**
	 * check if LiPoWatch is capable to send data
	 * @return true if device is ready to gather data telegrams
	 * @throws Exception
	 */
	public synchronized boolean checkDataReady() throws Exception {
		boolean isReady = false;
		int counter = 50;

		while (!isReady && counter-- > 0) {
			this.write(COMMAND_QUERY_STATE);
			try {
				Thread.sleep(50);
			}
			catch (InterruptedException e) {
				// ignore
			}
			byte[] buffer = new byte[1];
			buffer = this.read(buffer, 2000);
			if (buffer[0] == DATA_STATE_READY) {
				isReady = true;
			}
		}
		
		return isReady;
	}

	/**
	 * loop while writing status request until data state signaled as ready
	 * @return true if LiPoWatch signals data ready for transmission
	 * @throws Exception
	 */
	public synchronized boolean waitDataReady() throws Exception {
		boolean isReady = false;
		
		isReady = this.checkConnectionStatus();
		isReady = this.checkDataReady();

		return isReady;
	}

	/**
	 * verify the check sum
	 * @param readBuffer
	 * @throws CheckSumMissmatchException 
	 */
	private void verifyChecksum(byte[] readBuffer) throws CheckSumMissmatchException {
		int checkSum = 0;
		int checkSumData = 0;
		int length = (readBuffer[0] & 0x7F);
		checkSum = Checksum.ADD(readBuffer, 2) + 1;
		log.log(Level.FINER, "checkSum = " + checkSum); //$NON-NLS-1$
		checkSumData = ((readBuffer[length - 2] & 0xFF) << 8) + (readBuffer[length - 1] & 0xFF);
		log.log(Level.FINER, "checkSumData = " + checkSumData); //$NON-NLS-1$
		
		if (checkSum != checkSumData)
			throw new CheckSumMissmatchException(Messages.getString(osde.messages.MessageIds.OSDE_MSGE0034, new Object[] { checkSum, checkSumData } ));
	}
	
	/**
	 * verify the check sum
	 * @param readBuffer
	 * @return true for checksum match
	 * @throws CheckSumMissmatchException 
	 */
	private boolean isChecksumOK(byte[] readBuffer) {
		int checkSum = 0;
		int checkSumLast2Bytes = 0;
		checkSum = Checksum.ADD(readBuffer, 2) + 1;
		checkSumLast2Bytes = ((readBuffer[readBuffer.length - 2] & 0xFF) << 8) + (readBuffer[readBuffer.length - 1] & 0xFF);
		log.log(Level.FINER, "checkSum = " + checkSum + " checkSumLast2Bytes = " + checkSumLast2Bytes); //$NON-NLS-1$ //$NON-NLS-2$

		return (checkSum == checkSumLast2Bytes);
	}

	/**
	 * @param isFinished the isTransmitFinished to set, used within getData only
	 */
	public void setTransmitFinished(boolean isFinished) {
		this.isTransmitFinished = isFinished;
	}

	/**
	 * @return the isTransmitFinished, used within getData only
	 */
	public boolean isTransmitFinished() {
		return this.isTransmitFinished;
	}
}
