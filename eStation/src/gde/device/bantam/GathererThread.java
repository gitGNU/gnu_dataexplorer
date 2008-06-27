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
package osde.device.bantam;

import java.util.Date;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import osde.data.Channel;
import osde.data.Channels;
import osde.data.Record;
import osde.data.RecordSet;
import osde.exception.DataInconsitsentException;
import osde.ui.OpenSerialDataExplorer;

/**
 * Thread implementation to gather data from UniLog device
 * @author Winfied Brügmann
 */
public class GathererThread extends Thread {
	final static Logger						log													= Logger.getLogger(GathererThread.class.getName());

	OpenSerialDataExplorer				application;
	final EStationSerialPort			serialPort;
	final eStation								device;
	final Channels								channels;
	final Channel									channel;
	final Integer									channelNumber;
	final String[]								measurements;
	final String									configKey;
	final int											timeStep_ms;
	Timer													timer;
	TimerTask											timerTask;
	boolean												isTimerRunning							= false;
	boolean												isPortOpenedByLiveGatherer	= false;
	boolean												isSwitchedRecordSet					= false;

	RecordSet											recordSet;
	int														retryCounter								= 180;																							// 3*60 sec, 1 sec for each retry
	long													timeStamp;
	boolean												isCollectDataStopped				= false;
	String												recordSetKey								= ") nicht definiert";
	int														numberBatteryCells					= 0;																								// only if battery type is Lithium* single cell voltages will be available
	boolean												isUpdatedRecordSetCellState	= false;
	int														waitTime_ms									= 0;
	boolean												isWaitTimeChargeDischarge 	= false;
	boolean												isConfigUpdated							= false;
	HashMap<String, String>				configData									= new HashMap<String, String>();

	// offsets and factors are constant over thread live time
	final HashMap<String, Double>	calcValues									= new HashMap<String, Double>();

	/**
	 * @throws Exception 
	 * 
	 */
	public GathererThread(OpenSerialDataExplorer currentApplication, eStation useDevice, EStationSerialPort useSerialPort, String channelName) throws Exception {
		this.application = currentApplication;
		this.device = useDevice;
		this.serialPort = useSerialPort;
		this.channels = Channels.getInstance();
		this.channelNumber = new Integer(channelName.trim().split(":")[0].trim());
		this.channel = this.channels.get(this.channelNumber);
		this.configKey = channelName.trim().split(":")[1].trim();

		this.measurements = useDevice.getMeasurementNames(this.configKey); // 0=Spannung, 1=Höhe, 2=Steigrate, ....

		if (!this.serialPort.isConnected()) {
			this.serialPort.open();
			this.isPortOpenedByLiveGatherer = true;
		}
		this.timeStep_ms = new Double(this.device.getTimeStep_ms()).intValue();
	}

	@Override
	public void run() {
		this.isTimerRunning = true;

		// prepare timed data gatherer thread
		int delay = 0;
		int period = this.timeStep_ms;
		GathererThread.log.info("timer period = " + period + " ms");
		this.isCollectDataStopped = true;
		this.isUpdatedRecordSetCellState = false;

		final int[] points = new int[this.device.getMeasurementNames(this.configKey).length];
		final eStation usedDevice = this.device;

		this.timer = new Timer();
		this.timerTask = new TimerTask() {

			@Override
			public void run() {
				if (GathererThread.log.isLoggable(Level.FINE)) GathererThread.log.fine("====> entry");
				try {
					if (GathererThread.this.isTimerRunning) {
						// prepare the data for adding to record set
						//if (GathererThread.log.isLoggable(Level.FINE)) GathererThread.log.fine("recordSetKey = " + recordSetKey + " channelKonfigKey = " + recordSet.getChannelConfigName());

						// build the point array according curves from record set
						byte[] dataBuffer = GathererThread.this.serialPort.getData();

						// check if device is ready for data capturing, discharge or charge allowed only
						// else wait for 180 seconds max. for actions
						String processName = eStation.USAGE_MODE[usedDevice.getProcessingMode(dataBuffer)];
						GathererThread.log.info("usage mode = " + processName);
						if (processName.equals(eStation.USAGE_MODE[1]) || processName.equals(eStation.USAGE_MODE[2])) { // 1=discharge; 2=charge -> eStation active
							// check state change waiting to discharge to charge
							// check if a record set matching for re-use is available and prepare a new if required
							GathererThread.log.info("GathererThread.this.channel.size() == 0 = " + (GathererThread.this.channel.size() == 0));
							GathererThread.log.info("GathererThread.this.isCollectDataStopped = " + GathererThread.this.isCollectDataStopped);
							if (GathererThread.this.channel.size() != 0) {
								GathererThread.log.info("!GathererThread.this.channel.getRecordSetNames()[GathererThread.this.channel.getRecordSetNames().length - 1].endsWith(processName) = "
										+ (!GathererThread.this.channel.getRecordSetNames()[GathererThread.this.channel.getRecordSetNames().length - 1].endsWith(processName)));
							}
							GathererThread.log.info("GathererThread.this.isCollectDataStopped" + (GathererThread.this.isCollectDataStopped));
							GathererThread.log.info("(new Date().getTime() - getTimeStamp()) > 30000 = " + ((new Date().getTime() - getTimeStamp()) > 30000));
							if (GathererThread.this.channel.size() == 0 
									|| !GathererThread.this.isWaitTimeChargeDischarge
									|| !GathererThread.this.channel.getRecordSetNames()[GathererThread.this.channel.getRecordSetNames().length - 1].endsWith(processName)
									|| (new Date().getTime() - getTimeStamp()) > 30000 || GathererThread.this.isCollectDataStopped) {
								GathererThread.this.isCollectDataStopped = false;
								GathererThread.this.isWaitTimeChargeDischarge = true;
								GathererThread.this.isConfigUpdated	= false;
								// record set does not exist or is outdated, build a new name and create
								GathererThread.this.waitTime_ms = new Integer(usedDevice.getConfigurationValues(GathererThread.this.configData, dataBuffer).get(eStation.CONFIG_WAIT_TIME)).intValue() * 60000;
								log.info("waitTime_ms = " + GathererThread.this.waitTime_ms);
								GathererThread.this.recordSetKey = GathererThread.this.channel.size() + 1 + ") [" + GathererThread.this.configData.get(eStation.CONFIG_BATTERY_TYPE) + "] " + processName;
								GathererThread.this.channel.put(GathererThread.this.recordSetKey, RecordSet.createRecordSet(getName().trim(), GathererThread.this.recordSetKey, GathererThread.this.application
										.getActiveDevice(), true, false));
								GathererThread.this.channel.applyTemplateBasics(GathererThread.this.recordSetKey);
								GathererThread.log.info(GathererThread.this.recordSetKey + " created for channel " + GathererThread.this.channel.getName());
								if (GathererThread.this.channel.getActiveRecordSet() == null) GathererThread.this.channel.setActiveRecordSet(GathererThread.this.recordSetKey);
								GathererThread.this.recordSet = GathererThread.this.channel.get(GathererThread.this.recordSetKey);
								GathererThread.this.recordSet.setTableDisplayable(false); // suppress table calc + display 
								GathererThread.this.recordSet.setAllDisplayable();
								GathererThread.this.channel.applyTemplate(GathererThread.this.recordSetKey);
								// switch the active record set if the current record set is child of active channel
								// for eStation its always the case since we have only one channel
								if (GathererThread.this.channel.getName().equals(GathererThread.this.channels.getActiveChannel().getName())) {
									GathererThread.this.channels.getActiveChannel().switchRecordSet(GathererThread.this.recordSetKey);
								}
							}
							else {
								GathererThread.this.recordSetKey = GathererThread.this.channel.size() + ") [" + GathererThread.this.configData.get(eStation.CONFIG_BATTERY_TYPE) + "] " + processName;
								GathererThread.log.info("re-using " + GathererThread.this.recordSetKey);
							}
							setTimeStamp();

							//recordSet = GathererThread.this.channel.get(GathererThread.this.recordSetKey);
							//TODO check this.device.updateInitialRecordSetComment(recordSet);

							// prepare the data for adding to record set
							GathererThread.this.recordSet.addPoints(usedDevice.converDataBytes(points, dataBuffer), false);

							GathererThread.this.application.updateGraphicsWindow();
							GathererThread.this.application.updateDigitalWindowChilds();
							GathererThread.this.application.updateAnalogWindowChilds();

							GathererThread.this.numberBatteryCells = 0;
							String[] recordKeys = GathererThread.this.recordSet.getRecordNames();
							for (int i = 8; i < GathererThread.this.recordSet.size(); i++) {
								Record record = GathererThread.this.recordSet.get(recordKeys[i]);
								if ((record.getMinValue() + record.getMinValue()) == 0) GathererThread.this.numberBatteryCells++;
							}
							GathererThread.log.info("numberBatteryCells = " + GathererThread.this.numberBatteryCells);

							if (GathererThread.this.numberBatteryCells > 0) {
								int[] voltages = new int[GathererThread.this.numberBatteryCells];
								for (int i = 0; i < GathererThread.this.numberBatteryCells; i++) {
									voltages[i] = points[i + 8];
									GathererThread.log.info("points[" + i + "+ 8] = " + points[i + 8]);
								}
								GathererThread.this.application.updateCellVoltageChilds(voltages);
							}

							//switch off single cell voltage lines if not battery type of lithium where cell voltages are available
							for (int i = 8+GathererThread.this.numberBatteryCells; !GathererThread.this.isConfigUpdated && i < points.length-GathererThread.this.numberBatteryCells; i++) {
								GathererThread.this.recordSet.get(recordKeys[i]).setDisplayable(false);
								GathererThread.this.recordSet.get(recordKeys[i]).setVisible(false);
							}
							GathererThread.this.isConfigUpdated = true;
						}
						else { // else wait for 180 seconds max. for actions
							if (0 == (setRetryCounter(getRetryCounter() - 1))) {
								stopTimerThread();
								GathererThread.log.info("Timer stopped eStation inactiv");
								setRetryCounter(180);
							}
						}
					}
				}
				catch (DataInconsitsentException e) {
					String message = "Das Datenmodell der Anwendung wird fehlerhaft bedient.\n" + e.getClass().getSimpleName() + " - " + e.getMessage();
					cleanup(GathererThread.this.recordSetKey, message, e);
				}
				catch (Throwable e) {
					finalizeRecordSet(GathererThread.this.recordSetKey, false);
					if (e instanceof java.io.IOException && GathererThread.this.isWaitTimeChargeDischarge) {
						try {
							log.info("waiting...");
							Thread.sleep(GathererThread.this.waitTime_ms+1500);
							GathererThread.this.isWaitTimeChargeDischarge = false;
							return;
						}
						catch (InterruptedException e1) {
							// ignore
						}
					}
					String message = "Die serielle Kommunikation zu Gerät zeigt den Fehler : " + e.getClass().getSimpleName() + " - " + e.getMessage();
					cleanup(GathererThread.this.recordSetKey, message, e);
				}
				if (GathererThread.log.isLoggable(Level.FINE)) GathererThread.log.fine("======> exit");
			}
		};

		// start the prepared timer thread within the life data gatherer thread
		this.timer.scheduleAtFixedRate(this.timerTask, delay, period);
		this.isTimerRunning = true;
		GathererThread.log.info("exit");
	}

	/**
	 * @param useRecordSet
	 * @param displayableCounter
	 * @return
	 */
	void updateActiveState(RecordSet useRecordSet) {
		// check if measurements isActive == false and set to isDisplayable == false
		for (String element : this.measurements) {
			Record record = useRecordSet.get(element);
			if (!record.isActive()) {
				record.setDisplayable(false);
				record.setVisible(false);
			}
		}
	}

	/**
	 * close port, set isDisplayable according channel configuration and calculate slope
	 * @param newRecordSetKey
	 */
	public void finalizeRecordSet(String newRecordSetKey, boolean doClosePort) {
		if (doClosePort && this.isPortOpenedByLiveGatherer) 
			this.serialPort.close();

		RecordSet tmpRecordSet = this.channel.get(newRecordSetKey);
		tmpRecordSet.setTableDisplayable(true); // enable table display after calculation
		this.device.updateVisibilityStatus(tmpRecordSet);
		this.device.makeInActiveDisplayable(tmpRecordSet);
		this.channel.applyTemplate(newRecordSetKey);
		this.application.updateDataTable();
	}

	/**
	 * stop the timer task thread, this tops data capturing
	 * waits for all running timers tasks are ended before return 
	 */
	public synchronized void stopTimerThread() {
		if (this.timerTask != null) this.timerTask.cancel();
		if (this.timer != null) this.timer.cancel();
		this.isTimerRunning = false;
		this.isCollectDataStopped = true;
	}

	/**
	 * cleanup all allocated resources and display the message
	 * @param useRecordSetKey
	 * @param message
	 * @param e
	 */
	void cleanup(final String useRecordSetKey, final String message, Throwable e) {
		this.stopTimerThread();
		if (this.isPortOpenedByLiveGatherer) this.serialPort.close();
		if (this.channel.get(useRecordSetKey) != null) {
			this.channel.get(useRecordSetKey).clear();
			this.channel.remove(useRecordSetKey);
			if (Thread.currentThread().getId() == this.application.getThreadId()) {
				this.application.getMenuToolBar().updateRecordSetSelectCombo();
				this.application.updateDataTable();
				this.application.openMessageDialog(message);
				this.device.getDialog().resetButtons();
			}
			else {
				OpenSerialDataExplorer.display.asyncExec(new Runnable() {
					public void run() {
						GathererThread.this.application.getMenuToolBar().updateRecordSetSelectCombo();
						GathererThread.this.application.updateDataTable();
						GathererThread.this.application.openMessageDialog(message);
						GathererThread.this.device.getDialog().resetButtons();
					}
				});
			}
		}
		GathererThread.log.log(Level.SEVERE, e.getMessage(), e);
	}

	/**
	 * set timeStamp using Date().getTime()
	 */
	void setTimeStamp() {
		this.timeStamp = new Date().getTime();
	}

	/**
	 * @return the timeStamp
	 */
	long getTimeStamp() {
		return this.timeStamp;
	}

	/**
	 * @param enabled the isCollectDataStopped to set
	 */
	public void setCollectDataStopped(boolean enabled) {
		this.isCollectDataStopped = enabled;
	}

	/**
	 * @return the isCollectDataStopped
	 */
	public boolean isCollectDataStopped() {
		return this.isCollectDataStopped;
	}

	/**
	 * @return the retryCounter
	 */
	int getRetryCounter() {
		return this.retryCounter;
	}

	/**
	 * @param newRetryCounter the retryCounter to set
	 */
	int setRetryCounter(int newRetryCounter) {
		return this.retryCounter = newRetryCounter;
	}
}
