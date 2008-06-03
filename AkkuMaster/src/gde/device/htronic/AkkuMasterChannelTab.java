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
package osde.device.htronic;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;

import osde.data.Channel;
import osde.data.Channels;
import osde.data.RecordSet;
import osde.exception.DataInconsitsentException;
import osde.ui.OpenSerialDataExplorer;
import osde.ui.SWTResourceManager;

/**
  * Implementation for one channel tab, this will be initialized according number of available channels
  * @author Winfried Brügmann
 */
public class AkkuMasterChannelTab {
	final static Logger						log												= Logger.getLogger(AkkuMasterChannelTab.class.getName());

	AkkuMasterC4Dialog						parent;
	String												name;
	byte[]												channelSig;
	String[]											aCapacity;
	String[]											aCellCount;
	String[]											aAkkuTyp;
	String[]											aProgramm;
	String[]											aChargeCurrent_mA;
	String[]											aDischargeCurrent_mA;
	AkkuMasterC4SerialPort				serialPort;
	Channel												channel;
	Timer													timer;
	TimerTask											timerTask;

	CTabItem											channelTab;
	Button												captureOnlyButton;
	Group													programGroup;
	Group													captureOnlyGroup;
	CCombo												memoryNumberCombo;
	CCombo												capacityMilliAh;
	Group													akkuGroup;
	Text													chargeCurrentText;
	Button												stopDataGatheringButton;
	Button												startDataGatheringButton;
	Text													memoryNumberText;
	CCombo												dischargeCurrent;
	Text													dischargeCurrentText;
	CCombo												chargeCurrent;
	CCombo												program;
	Text													programText;
	Group													programTypeGroup;
	Text													akkuTypeText;
	CCombo												akkuType;
	CCombo												countCells;
	Text													countCellsText;
	Text													capacityText;
	Text													captureOnlyText;
	Button												programmButton;
	Composite											channelComposite;
	boolean												isCaptureOnly							= false;
	boolean												isDefinedProgram					= false;
	boolean												isDataGatheringEnabled		= false;
	boolean												isStopButtonEnabled				= false;
	String												capacityMilliAhValue			= "0";
	int														countCellsValue						= 0;
	int														akkuTypeValue							= 0;
	int														programValue							= 0;
	String												chargeCurrentValue				= "0";
	String												dischargeCurrentValue			= "0";
	int														memoryNumberValue					= 1;

	boolean												isCollectData							= false;
	RecordSet											recordSet;
	int														retryCounter							= 3;
	long													timeStamp;
	boolean												isChargeCurrentAdded			= false;
	boolean												isDischargeCurrentAdded		= false;
	boolean												isCollectDataStopped			= false;
	boolean												isMemorySelectionChanged	= false;
	String												recordSetKey							= ") nicht definiert";

	final Channels								channels;
	final OpenSerialDataExplorer	application;

	/**
	 * constructor initialization of one channel tab
	 * @param newName
	 * @param useChannel byte signature
	 * @param useSerialPort
	 * @param arrayCapacity
	 * @param arrayCellCount
	 * @param arrayAkkuTyp
	 * @param arrayProgramm
	 * @param arrayChargeCurrent_mA
	 * @param arrayDischargeCurrent_mA
	 */
	public AkkuMasterChannelTab(AkkuMasterC4Dialog useParent, String newName, byte[] useChannelSig, AkkuMasterC4SerialPort useSerialPort, Channel useChannel, String[] arrayCapacity,
			String[] arrayCellCount, String[] arrayAkkuTyp, String[] arrayProgramm, String[] arrayChargeCurrent_mA, String[] arrayDischargeCurrent_mA) {
		this.parent = useParent;
		this.name = newName;
		this.channelSig = useChannelSig;
		this.serialPort = useSerialPort;
		this.channel = useChannel;
		this.aCapacity = arrayCapacity;
		this.aCellCount = arrayCellCount;
		this.aAkkuTyp = arrayAkkuTyp;
		this.aProgramm = arrayProgramm;
		this.aChargeCurrent_mA = arrayChargeCurrent_mA;
		this.aDischargeCurrent_mA = arrayDischargeCurrent_mA;
		this.channels = Channels.getInstance();
		this.application = OpenSerialDataExplorer.getInstance();
	}

	/**
	 * add the tab to the dialog
	 */
	public void addChannelTab(CTabFolder tabFolder) {
		{
			this.channelTab = new CTabItem(tabFolder, SWT.NONE);
			this.channelTab.setText(this.name);
			{ // begin channel composite
				this.channelComposite = new Composite(tabFolder, SWT.NONE);
				this.channelTab.setControl(this.channelComposite);
				this.channelComposite.setLayout(null);
				this.channelComposite.addPaintListener(new PaintListener() {
					public void paintControl(PaintEvent evt) {
						AkkuMasterChannelTab.log.finest("channelComposite.widgetSelected, event=" + evt);
						updateStartDataGatheringButton();
						updateStopDataGatheringButton();
					}
				});

				{ // begin capture only group
					this.captureOnlyGroup = new Group(this.channelComposite, SWT.NONE);
					this.captureOnlyGroup.setLayout(null);
					this.captureOnlyGroup.setBounds(12, 8, 400, 80);
					this.captureOnlyGroup.addPaintListener(new PaintListener() {
						public void paintControl(PaintEvent evt) {
							AkkuMasterChannelTab.log.finest("captureOnlyGroup.widgetSelected, event=" + evt);
							updateCaptureOnlyButton();
						}
					});
					{
						this.captureOnlyText = new Text(this.captureOnlyGroup, SWT.MULTI | SWT.WRAP);
						this.captureOnlyText.setText("Mit dieser Funktion kann ein am Ladegerät gestarteter Vorgang aufgenommen werden");
						this.captureOnlyText.setBackground(OpenSerialDataExplorer.COLOR_LIGHT_GREY);
						this.captureOnlyText.setBounds(51, 40, 315, 37);
					}
					{
						this.captureOnlyButton = new Button(this.captureOnlyGroup, SWT.RADIO | SWT.LEFT);
						this.captureOnlyButton.setText("  Nur Datenaufnahme");
						this.captureOnlyButton.setFont(SWTResourceManager.getFont("Microsoft Sans Serif", 10, 1, false, false));
						this.captureOnlyButton.setBounds(12, 15, 310, 22);
						this.captureOnlyButton.addSelectionListener(new SelectionAdapter() {
							@Override
							public void widgetSelected(SelectionEvent evt) {
								AkkuMasterChannelTab.log.finest("captureOnlyButton.widgetSelected, event=" + evt);
								if (getCaptureOnlyButtonSelection()) {
									try {
										setCaptureOnly(true);
										setDefinedProgram(false);
										setDataGatheringEnabled(true);
										updateAdjustedValues();
									}
									catch (Exception e) {
										OpenSerialDataExplorer.getInstance().openMessageDialog(
												"Bei der seriellen Kommunikation ist ein Fehler aufgetreten, bitte die Porteinstellung überprüfen. " + e.getClass().getSimpleName() + " - " + e.getMessage());
									}
									updateCaptureOnlyButton();
									updateProgramButton();
									updateStartDataGatheringButton();
								}
							}
						});
					}
				} // end capture only group

				{ // begin program group
					this.programGroup = new Group(this.channelComposite, SWT.NONE);
					this.programGroup.setLayout(null);
					this.programGroup.setBounds(12, 95, 400, 250);
					this.programGroup.addPaintListener(new PaintListener() {
						public void paintControl(PaintEvent evt) {
							AkkuMasterChannelTab.log.finest("programGroup.widgetSelected, event=" + evt);
							updateProgramButton();
						}
					});
					{
						this.programmButton = new Button(this.programGroup, SWT.RADIO | SWT.LEFT);
						this.programmButton.setText("  Selbst konfiguriertes Programm");
						this.programmButton.setBounds(12, 15, 295, 21);
						this.programmButton.setFont(SWTResourceManager.getFont("Microsoft Sans Serif", 10, 1, false, false));
						this.programmButton.addSelectionListener(new SelectionAdapter() {
							@Override
							public void widgetSelected(SelectionEvent evt) {
								AkkuMasterChannelTab.log.finest("programmButton.widgetSelected, event=" + evt);
								if (getProgramButtonSelection()) {
									try {
										setCaptureOnly(false);
										setDefinedProgram(true);
										setDataGatheringEnabled(true);
										updateAdjustedValues();
									}
									catch (Exception e) {
										OpenSerialDataExplorer.getInstance().openMessageDialog("Das angeschlossene Gerät antwortet nicht auf dem seriellen Port!");
									}
									updateCaptureOnlyButton();
									updateProgramButton();
									updateStartDataGatheringButton();
								}
							}
						});
					}
					{
						this.akkuGroup = new Group(this.programGroup, SWT.NONE);
						this.akkuGroup.setLayout(null);
						this.akkuGroup.setText("Akku");
						this.akkuGroup.setBounds(15, 40, 369, 67);
						this.akkuGroup.addPaintListener(new PaintListener() {
							public void paintControl(PaintEvent evt) {
								AkkuMasterChannelTab.log.finest("akkuGroup.widgetSelected, event=" + evt);
								updateCapacityMilliAhText();
								updateCountCellSelection();
								updateAkkuType();
							}
						});
						{
							this.capacityText = new Text(this.akkuGroup, SWT.NONE);
							this.capacityText.setBounds(12, 20, 105, 18);
							this.capacityText.setText(" Kapazität [mAh]");
							this.capacityText.setBackground(OpenSerialDataExplorer.COLOR_LIGHT_GREY);
							this.capacityText.setEditable(false);
						}
						{
							this.capacityMilliAh = new CCombo(this.akkuGroup, SWT.NONE);
							this.capacityMilliAh.setItems(this.aCapacity);
							this.capacityMilliAh.setText(this.aCapacity[5]);
							this.capacityMilliAh.setBounds(12, 40, 105, 18);
							this.capacityMilliAh.addSelectionListener(new SelectionAdapter() {
								@Override
								public void widgetSelected(SelectionEvent evt) {
									AkkuMasterChannelTab.log.finest("capacityMilliAh.widgetSelected, event=" + evt);
									updateCapacityMilliAhValue();
								}
							});
						}
						{
							this.countCellsText = new Text(this.akkuGroup, SWT.NONE);
							this.countCellsText.setBackground(OpenSerialDataExplorer.COLOR_LIGHT_GREY);
							this.countCellsText.setBounds(130, 20, 105, 18);
							this.countCellsText.setText("  Zellenzahl");
							this.countCellsText.setEditable(false);
						}
						{
							this.countCells = new CCombo(this.akkuGroup, SWT.NONE);
							this.countCells.setBounds(130, 40, 105, 18);
							this.countCells.setItems(this.aCellCount);
							this.countCells.setText(this.aCellCount[3]);
							this.countCells.setEditable(false);
							this.countCells.setBackground(OpenSerialDataExplorer.COLOR_WHITE);
							this.countCells.addSelectionListener(new SelectionAdapter() {
								@Override
								public void widgetSelected(SelectionEvent evt) {
									AkkuMasterChannelTab.log.finest("countCells.widgetSelected, event=" + evt);
									updateCellCountValue();
								}
							});
						}
						{
							this.akkuTypeText = new Text(this.akkuGroup, SWT.NONE);
							this.akkuTypeText.setBackground(OpenSerialDataExplorer.COLOR_LIGHT_GREY);
							this.akkuTypeText.setBounds(255, 20, 105, 18);
							this.akkuTypeText.setText("   Akkutyp");
							this.akkuTypeText.setDoubleClickEnabled(false);
							this.akkuTypeText.setDragDetect(false);
							this.akkuTypeText.setEditable(false);
						}
						{
							this.akkuType = new CCombo(this.akkuGroup, SWT.NONE);
							this.akkuType.setBounds(255, 40, 105, 18);
							this.akkuType.setItems(this.aAkkuTyp);
							this.akkuType.setText(this.aAkkuTyp[0]);
							this.akkuType.setEditable(false);
							this.akkuType.setBackground(OpenSerialDataExplorer.COLOR_WHITE);
							this.akkuType.addSelectionListener(new SelectionAdapter() {
								@Override
								public void widgetSelected(SelectionEvent evt) {
									AkkuMasterChannelTab.log.finest("akkuType.widgetSelected, event=" + evt);
									updateAkkuTypeValue();
								}
							});
						}
					}
					{
						this.programTypeGroup = new Group(this.programGroup, SWT.NONE);
						this.programTypeGroup.setBounds(15, 110, 369, 123);
						this.programTypeGroup.setText("Programmtyp");
						this.programTypeGroup.setLayout(null);
						this.programTypeGroup.addPaintListener(new PaintListener() {
							public void paintControl(PaintEvent evt) {
								AkkuMasterChannelTab.log.finest("programTypeGroup.widgetSelected, event=" + evt);
								updateProgramText();
								updateChargeCurrentText();
								updateDichargeCurrentText();
								updateMemoryNumberSelection();
							}
						});
						{
							this.programText = new Text(this.programTypeGroup, SWT.NONE);
							this.programText.setBackground(OpenSerialDataExplorer.COLOR_LIGHT_GREY);
							this.programText.setBounds(130, 20, 105, 18);
							this.programText.setText("Programmname");
						}
						{
							this.program = new CCombo(this.programTypeGroup, SWT.NONE);
							this.program.setBounds(12, 40, 347, 18);
							this.program.setItems(this.aProgramm);
							this.program.select(2);
							this.program.setEditable(false);
							this.program.setBackground(OpenSerialDataExplorer.COLOR_WHITE);
							this.program.addSelectionListener(new SelectionAdapter() {
								public void widgetSelected(SelectionEvent evt) {
									AkkuMasterChannelTab.log.finest("program.widgetSelected, event=" + evt);
									updateProgramSelectionValue();
								}
							});
						}
						{
							this.chargeCurrentText = new Text(this.programTypeGroup, SWT.NONE);
							this.chargeCurrentText.setBackground(OpenSerialDataExplorer.COLOR_LIGHT_GREY);
							this.chargeCurrentText.setBounds(12, 70, 105, 18);
							this.chargeCurrentText.setText("  Ladestrom [mA]");
							this.chargeCurrentText.setEditable(false);
						}
						{
							this.chargeCurrent = new CCombo(this.programTypeGroup, SWT.NONE);
							this.chargeCurrent.setBounds(12, 93, 105, 18);
							this.chargeCurrent.setItems(this.aChargeCurrent_mA);
							this.chargeCurrent.setText(this.aChargeCurrent_mA[5]);
							this.chargeCurrent.addSelectionListener(new SelectionAdapter() {
								public void widgetSelected(SelectionEvent evt) {
									AkkuMasterChannelTab.log.finest("chargeCurrent.widgetSelected, event=" + evt);
									updateChargeCurrentValue();
								}
							});
						}
						{
							this.dischargeCurrentText = new Text(this.programTypeGroup, SWT.NONE);
							this.dischargeCurrentText.setBackground(OpenSerialDataExplorer.COLOR_LIGHT_GREY);
							this.dischargeCurrentText.setBounds(130, 70, 105, 18);
							this.dischargeCurrentText.setDragDetect(false);
							this.dischargeCurrentText.setDoubleClickEnabled(false);
							this.dischargeCurrentText.setText("Entladestrom [mA]");
							this.dischargeCurrentText.setEditable(false);
						}
						{
							this.dischargeCurrent = new CCombo(this.programTypeGroup, SWT.NONE);
							this.dischargeCurrent.setBounds(130, 93, 105, 18);
							this.dischargeCurrent.setItems(this.aDischargeCurrent_mA);
							this.dischargeCurrent.setText(this.aDischargeCurrent_mA[5]);
							this.dischargeCurrent.addSelectionListener(new SelectionAdapter() {
								public void widgetSelected(SelectionEvent evt) {
									AkkuMasterChannelTab.log.finest("dischargeCurrent.widgetSelected, event=" + evt);
									updateDischargeCurrentValue();
								}
							});
						}
						{
							this.memoryNumberText = new Text(this.programTypeGroup, SWT.NONE);
							this.memoryNumberText.setBackground(OpenSerialDataExplorer.COLOR_LIGHT_GREY);
							this.memoryNumberText.setBounds(255, 70, 105, 18);
							this.memoryNumberText.setText("Speicher No");
							this.memoryNumberText.setEditable(false);
						}
						{
							this.memoryNumberCombo = new CCombo(this.programTypeGroup, SWT.NONE);
							this.memoryNumberCombo.setBounds(255, 93, 105, 18);
							this.memoryNumberCombo.setItems(new String[] { "0", "1", "2", "3", "4", "5", "6", "7" });
							this.memoryNumberCombo.select(1);
							this.memoryNumberCombo.setEditable(false);
							this.memoryNumberCombo.setBackground(OpenSerialDataExplorer.COLOR_WHITE);
							this.memoryNumberCombo.addSelectionListener(new SelectionAdapter() {
								public void widgetSelected(SelectionEvent evt) {
									AkkuMasterChannelTab.log.finest("memoryNumberCombo.widgetSelected, event=" + evt);
									updateMemoryNumberValue();
									setMemorySelectionChanged(true);
								}
							});
						}
					}
				} // end program group

				{
					this.startDataGatheringButton = new Button(this.channelComposite, SWT.PUSH | SWT.CENTER);
					this.startDataGatheringButton.setBounds(12, 360, 190, 28);
					this.startDataGatheringButton.setText("S t a r t");
					this.startDataGatheringButton.setFont(SWTResourceManager.getFont("Microsoft Sans Serif", 10, 1, false, false));
					this.startDataGatheringButton.setSelection(this.isCollectData);
					this.startDataGatheringButton.setEnabled(false);
					this.startDataGatheringButton.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							AkkuMasterChannelTab.log.finest("startAufzeichnungButton.widgetSelected, event=" + evt);
							setDataGatheringEnabled(false);
							setStopButtonEnabled(true);
							updateStartDataGatheringButton();
							updateStopDataGatheringButton();
							if (!isCollectData()) {
								setCollectData(true);

								try {
									if (isCaptureOnly()) {
										updateAdjustedValues();
									}
									else {
										int programNumber = getProgramNumber();
										int waitTime_days = 1; // new Integer(warteZeitTage.getText()).intValue();
										int accuTyp = getAkkuType();
										int cellCount = getCellCount();
										int akkuCapacity = getAkkuCapacity();
										int dischargeCurrent_mA = getDischargeCurrent();
										int chargeCurrent_mA = getChargeCurrent();
										AkkuMasterChannelTab.log.fine(" programNumber = " + programNumber + " waitTime_days = " + waitTime_days + " accuTyp = " + accuTyp + " cellCount = " + cellCount
												+ " akkuCapacity = " + akkuCapacity + " dischargeCurrent_mA = " + dischargeCurrent_mA + " chargeCurrent_mA = " + chargeCurrent_mA);
										AkkuMasterChannelTab.this.serialPort.writeNewProgram(AkkuMasterChannelTab.this.channelSig, programNumber, waitTime_days, accuTyp, cellCount, akkuCapacity, dischargeCurrent_mA, chargeCurrent_mA);

										if (isMemorySelectionChanged()) {
											int memoryNumber = getMemoryNumberSelectionIndex();
											AkkuMasterChannelTab.log.fine("memoryNumber =" + memoryNumber);
											AkkuMasterChannelTab.this.serialPort.setMemoryNumberCycleCoundSleepTime(AkkuMasterChannelTab.this.channelSig, memoryNumber, 2, 2000);
										}

										if (AkkuMasterChannelTab.this.parent.getMaxCurrent() < AkkuMasterChannelTab.this.parent.getActiveCurrent() + dischargeCurrent_mA || AkkuMasterChannelTab.this.parent.getMaxCurrent() < AkkuMasterChannelTab.this.parent.getActiveCurrent() + chargeCurrent_mA) {
											AkkuMasterChannelTab.this.application.openMessageDialog(
													"Der für das Gerät erlaubte Gesammtstrom würde mit den angegebenen Werten für Entladestrom = " + dischargeCurrent_mA + " mA oder für den Ladestrom = " + chargeCurrent_mA
															+ " mA überschritten, bitte korrigieren.");
											setCollectData(false);
											setStartDataGatheringSelection(true);
											return;
										}

										AkkuMasterChannelTab.this.serialPort.start(AkkuMasterChannelTab.this.channelSig);
										AkkuMasterChannelTab.this.serialPort.ok(AkkuMasterChannelTab.this.channelSig);
									}
									getChannels().switchChannel(getChannel().getName());
									// prepare timed data gatherer thread
									int delay = 0;
									int period = AkkuMasterChannelTab.this.application.getActiveDevice().getTimeStep_ms().intValue(); // repeat every 10 sec.
									setTimer(new Timer());
									setTimerTask(new TimerTask() {
										HashMap<String, Object>	data; // [8]

										public void run() {
											/*
											 * [0] String Aktueller Prozessname 			"4 ) Laden" = AkkuMaster aktiv Laden
											 * [1] int 		Aktuelle Fehlernummer				"0" = kein Fehler
											 * [2] int		Aktuelle Akkuspannung 			[mV]
											 * [3] int 		Aktueller Prozesssstrom 		[mA] 	(laden/entladen)
											 * [4] int 		Aktuelle Prozesskapazität		[mAh] (laden/entladen)
											 * [5] int 		Errechnete Leistung					[mW]			
											 * [6] int		Errechnete Energie					[mWh]			
											 * [7] int		Prozesszeit									[msec]			
											 */
											try {
												this.data = AkkuMasterChannelTab.this.serialPort.getData(AkkuMasterChannelTab.this.channelSig);
												// check for no error state
												AkkuMasterChannelTab.log.fine("error state = " + this.data.get(AkkuMasterC4SerialPort.PROCESS_ERROR_NO));
												if (0 == (Integer) this.data.get(AkkuMasterC4SerialPort.PROCESS_ERROR_NO)) {
													String processName = ((String) this.data.get(AkkuMasterC4SerialPort.PROCESS_NAME)).split(" ")[1];
													AkkuMasterChannelTab.log.fine("processName = " + processName);

													// check if device is ready for data capturing
													int processNumber = new Integer(((String) this.data.get(AkkuMasterC4SerialPort.PROCESS_NAME)).split(" ")[0]).intValue();
													if (processNumber == 1 || processNumber == 2) { // 1=Laden; 2=Entladen - AkkuMaster activ
														// check state change waiting to discharge to charge
														// check if a record set matching for re-use is available and prepare a new if required
														AkkuMasterChannelTab.log.fine(getChannel().getName() + "=" + getChannel().size());
														if (getChannel().size() == 0 || !getChannel().getRecordSetNames()[getChannel().getRecordSetNames().length - 1].endsWith(processName)
																|| (new Date().getTime() - getTimeStamp()) > 30000 || isCollectDataStopped()) {
															setCollectDataStopped(false);
															// record set does not exist or is outdated, build a new name and create
															setRecordSetKey((getChannel().size() + 1) + ") " + processName);
															getChannel().put(getRecordSetKey(), RecordSet.createRecordSet(getName().trim(), getRecordSetKey(), AkkuMasterChannelTab.this.application.getActiveDevice(), true, false));
															getChannel().applyTemplateBasics(getRecordSetKey());
															AkkuMasterChannelTab.log.fine(getRecordSetKey() + " created for channel " + getChannel().getName());
															if (getChannel().getActiveRecordSet() == null) getChannel().setActiveRecordSet(getRecordSetKey());
															setRecordSet(getChannel().get(getRecordSetKey()));
															getRecordSet().setTableDisplayable(false); // suppress table calc + display 
															getRecordSet().setAllDisplayable();
															getChannel().applyTemplate(getRecordSetKey());
															// switch the active record set if the current record set is child of active channel
															if (getChannel().getName().equals(getChannels().getActiveChannel().getName())) {
																AkkuMasterChannelTab.this.application.getMenuToolBar().addRecordSetName(getRecordSetKey());
																getChannels().getActiveChannel().switchRecordSet(getRecordSetKey());
															}
															// update discharge / charge current display
															int actualCurrent = ((Integer) this.data.get(AkkuMasterC4SerialPort.PROCESS_CURRENT)).intValue();
															if (processName.equals("Laden")) {
																AkkuMasterChannelTab.this.parent.addTotalChargeCurrent(actualCurrent);
																setChargeCurrentAdded(true);
															}
															else if (processName.equals("Entladen")) {
																AkkuMasterChannelTab.this.parent.addTotalDischargeCurrent(actualCurrent);
																setDischargeCurrentAdded(true);
															}
															if (processName.equals("Laden") && isDischargeCurrentAdded()) {
																AkkuMasterChannelTab.this.parent.subtractTotalChargeCurrent(actualCurrent);
															}
															else if (processName.equals("Entladen") && isChargeCurrentAdded()) {
																AkkuMasterChannelTab.this.parent.subtractTotalDischargeCurrent(actualCurrent);
															}
														}
														else {
															setRecordSetKey(getChannel().size() + ") " + processName);
															AkkuMasterChannelTab.log.fine("re-using " + getRecordSetKey());
														}
														setTimeStamp();

														// prepare the data for adding to record set
														setRecordSet(getChannel().get(getRecordSetKey()));
														// build the point array according curves from record set
														int[] points = new int[getRecordSet().size()];

														points[0] = new Integer((Integer) this.data.get(AkkuMasterC4SerialPort.PROCESS_VOLTAGE)).intValue(); //Spannung 	[mV]
														points[1] = new Integer((Integer) this.data.get(AkkuMasterC4SerialPort.PROCESS_CURRENT)).intValue(); //Strom 			[mA]
														// display adaption * 1000  -  / 1000
														points[2] = new Integer((Integer) this.data.get(AkkuMasterC4SerialPort.PROCESS_CAPACITY)).intValue() * 1000; //Kapazität	[mAh] 
														points[3] = new Integer((Integer) this.data.get(AkkuMasterC4SerialPort.PROCESS_POWER)).intValue() / 1000; //Leistung		[mW]
														points[4] = new Integer((Integer) this.data.get(AkkuMasterC4SerialPort.PROCESS_ENERGIE)).intValue() / 1000; //Energie		[mWh]
														AkkuMasterChannelTab.log.fine(points[0] + " mV; " + points[1] + " mA; " + points[2] + " mAh; " + points[3] + " mW; " + points[4] + " mWh");

														getRecordSet().addPoints(points, false); // updates data table and digital windows
														AkkuMasterChannelTab.this.application.updateGraphicsWindow();
														AkkuMasterChannelTab.this.application.updateDigitalWindowChilds();
														AkkuMasterChannelTab.this.application.updateAnalogWindowChilds();
													}
													else {
														// only the voltage can be displayed and updated
														//String voltage = new Double(new Integer(measuredData[2]) / 1000.0).toString(); // V
														//String current = new Double(new Integer(measuredData[3])).toString(); // mA
														//application.updateDigitalLabelText(new String[] { voltage, current });

														// enable switching records sets
														if (0 == (setRetryCounter(getRetryCounter() - 1))) {
															stopTimer();
															AkkuMasterChannelTab.log.fine("Timer stopped AkkuMaster inactiv");
															setRetryCounter(3);
														}
													}
												}
												else { // some error state
													AkkuMasterChannelTab.log.fine("canceling timer due to error");
													if (!isCaptureOnly()) try {
														AkkuMasterChannelTab.this.serialPort.stop(AkkuMasterChannelTab.this.channelSig);
													}
													catch (IOException e) {
														AkkuMasterChannelTab.log.log(Level.SEVERE, e.getMessage(), e);
													}
													setCollectData(false);
													stopTimer();
													AkkuMasterChannelTab.this.application.openMessageDialog("Das angeschlossenen Gerät meldet einen Fehlerstatus, bitte überprüfen.");
												}
											}
											catch (DataInconsitsentException e) {
												// exception is logged where it is thrown first log.log(Level.SEVERE, e.getMessage(), e);
												setCollectData(false);
												stopTimer();
												if (!AkkuMasterChannelTab.this.parent.isDisposed()) AkkuMasterChannelTab.this.application.openMessageDialog("Das Datenmodell der Anwendung wird fehlerhaft bedient.\n" + e.getClass().getSimpleName() + " - " + e.getMessage());
											}
											catch (Exception e) {
												// exception is logged where it is thrown first log.log(Level.SEVERE, e.getMessage(), e);
												setCollectData(false);
												stopTimer();
												if (!AkkuMasterChannelTab.this.parent.isDisposed()) AkkuMasterChannelTab.this.application.openMessageDialog("Das angeschlossenen Gerät meldet einen Fehlerstatus, bitte überprüfen.\n" + e.getClass().getSimpleName() + " - " + e.getMessage());
											}
										}
									});
									getTimer().scheduleAtFixedRate(getTimerTask(), delay, period);

								}
								catch (Exception e1) {
									AkkuMasterChannelTab.this.application.openMessageDialog("Das angeschlossene Gerät antwortet nicht auf dem seriellen Port!");
								}
							}
						}

					});
				}
				{
					this.stopDataGatheringButton = new Button(this.channelComposite, SWT.PUSH | SWT.CENTER);
					this.stopDataGatheringButton.setBounds(225, 360, 190, 28);
					this.stopDataGatheringButton.setText("S t o p");
					this.stopDataGatheringButton.setEnabled(false);
					this.stopDataGatheringButton.setFont(SWTResourceManager.getFont("Microsoft Sans Serif", 10, 1, false, false));
					this.stopDataGatheringButton.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							AkkuMasterChannelTab.log.finest("stopAuzeichnungButton.widgetSelected, event=" + evt);
							updateDialogAfterStop();
							if (!isCaptureOnly()) try {
								AkkuMasterChannelTab.this.serialPort.stop(AkkuMasterChannelTab.this.channelSig);
							}
							catch (IOException e) {
								e.printStackTrace();
							}
							stopTimer();
							// hope this is the right record set
							setRecordSet(getChannels().getActiveChannel().get(getRecordSetKey()));
							if (getRecordSet() != null) getRecordSet().setTableDisplayable(true); // enable table display after calculation
							AkkuMasterChannelTab.this.application.updateDataTable();
						}
					});
				}
			} // end channel composite
		}
	}

	/**
	 * @throws IOException
	 */
	void updateAdjustedValues() throws Exception {
		// update channel tab with values red from device
		if (!this.serialPort.isConnected()) this.serialPort.open();
		String[] configuration = this.serialPort.getConfiguration(this.channelSig);
		if (AkkuMasterChannelTab.log.isLoggable(Level.FINER)) this.serialPort.print(configuration);
		if (!configuration[0].equals("0")) { // AkkuMaster somehow active			
			this.programValue = new Integer(configuration[2].split(" ")[0]).intValue() - 1;
			this.program.setText(this.aProgramm[this.programValue]);

			this.akkuTypeValue = new Integer(configuration[3].split(" ")[0]).intValue();
			this.akkuType.setText(this.aAkkuTyp[this.akkuTypeValue]);

			this.countCellsValue = new Integer(configuration[4].split(" ")[0]).intValue() - 1;
			this.countCells.select(this.countCellsValue);

			this.capacityMilliAhValue = configuration[5].split(" ")[0];
			this.capacityMilliAh.setText(this.capacityMilliAhValue);

			this.chargeCurrentValue = configuration[7].split(" ")[0];
			this.chargeCurrent.setText(this.chargeCurrentValue);

			this.dischargeCurrentValue = configuration[6].split(" ")[0];
			this.dischargeCurrent.setText(this.dischargeCurrentValue);

			String[] adjustments = this.serialPort.getAdjustedValues(this.channelSig);
			this.memoryNumberValue = new Integer(adjustments[0].split(" ")[0]).intValue();
			this.memoryNumberCombo.select(this.memoryNumberValue);
			if (AkkuMasterChannelTab.log.isLoggable(Level.FINER)) this.serialPort.print(adjustments);
		}
	}

	/**
	 * stop the timer task thread, this tops data capturing
	 */
	public void stopTimer() {
		if (this.timerTask != null) this.timerTask.cancel();
		if (this.timer != null) {
			this.timer.cancel();
			this.timer.purge();
		}

		this.isCollectData = false;
		this.isCollectDataStopped = true;

		if (Thread.currentThread().getId() == AkkuMasterChannelTab.this.application.getThreadId()) {
			updateDialogAfterStop();
		}
		else {
			OpenSerialDataExplorer.display.asyncExec(new Runnable() {
				public void run() {
					updateDialogAfterStop();
				}
			});
		}
	}

	/**
	 * updates dialog UI after stop timer operation
	 */
	void updateDialogAfterStop() {
		this.isDataGatheringEnabled = false;
		this.isStopButtonEnabled = false;
		this.isCaptureOnly = false;
		this.isDefinedProgram = false;
		this.isMemorySelectionChanged = false;
		this.startDataGatheringButton.setEnabled(this.isDataGatheringEnabled);
		this.stopDataGatheringButton.setEnabled(this.isStopButtonEnabled);

		this.captureOnlyButton.setSelection(this.isCaptureOnly);
		this.programmButton.setSelection(this.isDefinedProgram);
	}

	public boolean isDataColletionActive() {
		return isCollectData() && isCollectDataStopped();
	}

	/**
	 * @return the isCollectData
	 */
	public boolean isCollectData() {
		return this.isCollectData;
	}

	/**
	 * @return the isCollectDataStopped
	 */
	public boolean isCollectDataStopped() {
		return this.isCollectDataStopped;
	}

	/**
	 * update the StartDataGatheringButton with the actual value of isDataGatheringEnabled
	 */
	void updateStartDataGatheringButton() {
		this.startDataGatheringButton.setEnabled(this.isDataGatheringEnabled);
	}

	/**
	 * update the StopDataGatheringButton with the actual value of isDataGatheringEnabled
	 */
	void updateStopDataGatheringButton() {
		this.stopDataGatheringButton.setEnabled(this.isStopButtonEnabled);
	}

	/**
	 * update the CaptureOnlyButton with the actual value of isCaptureOnly
	 */
	void updateCaptureOnlyButton() {
		this.captureOnlyButton.setSelection(this.isCaptureOnly);
	}

	/**
	 * 
	 */
	void updateProgramButton() {
		this.programmButton.setSelection(this.isDefinedProgram);
	}

	/**
	 * @param enabled the isCaptureOnly to set
	 */
	public void setCaptureOnly(boolean enabled) {
		this.isCaptureOnly = enabled;
	}

	/**
	 * @param enabled the isDefinedProgram to set
	 */
	public void setDefinedProgram(boolean enabled) {
		this.isDefinedProgram = enabled;
	}

	/**
	 * @param enabled the isDataGatheringEnabled to set
	 */
	public void setDataGatheringEnabled(boolean enabled) {
		this.isDataGatheringEnabled = enabled;
	}

	/**
	 * @param enabled the isStopButtonEnabled to set
	 */
	public void setStopButtonEnabled(boolean enabled) {
		this.isStopButtonEnabled = enabled;
	}

	/**
	 * @return
	 */
	boolean getCaptureOnlyButtonSelection() {
		return this.captureOnlyButton.getSelection();
	}

	/**
	 * @return
	 */
	boolean getProgramButtonSelection() {
		return this.programmButton.getSelection();
	}

	/**
	 * 
	 */
	void updateCapacityMilliAhText() {
		this.capacityMilliAh.setText(this.capacityMilliAhValue);
	}

	/**
	 * 
	 */
	void updateCountCellSelection() {
		this.countCells.select(this.countCellsValue);
	}

	/**
	 * 
	 */
	void updateAkkuType() {
		this.akkuType.setText(this.aAkkuTyp[this.akkuTypeValue]);
	}

	/**
	 * 
	 */
	void updateCapacityMilliAhValue() {
		this.capacityMilliAhValue = this.capacityMilliAh.getText();
	}

	/**
	 * 
	 */
	void updateCellCountValue() {
		this.countCellsValue = this.countCells.getSelectionIndex();
	}

	/**
	 * 
	 */
	void updateAkkuTypeValue() {
		this.akkuTypeValue = this.akkuType.getSelectionIndex();
	}

	/**
	 * 
	 */
	void updateProgramText() {
		this.program.setText(this.aProgramm[this.programValue]);
	}

	/**
	 * 
	 */
	void updateChargeCurrentText() {
		this.chargeCurrent.setText(this.chargeCurrentValue);
	}

	/**
	 * 
	 */
	void updateDichargeCurrentText() {
		this.dischargeCurrent.setText(this.dischargeCurrentValue);
	}

	/**
	 * 
	 */
	void updateMemoryNumberSelection() {
		this.memoryNumberCombo.select(this.memoryNumberValue);
	}

	/**
	 * 
	 */
	void updateProgramSelectionValue() {
		this.programValue = this.program.getSelectionIndex() + 1;
	}

	/**
	 * 
	 */
	void updateChargeCurrentValue() {
		this.chargeCurrentValue = this.chargeCurrent.getText();
	}

	/**
	 * 
	 */
	void updateDischargeCurrentValue() {
		this.dischargeCurrentValue = this.dischargeCurrent.getText();
	}

	/**
	 * 
	 */
	void updateMemoryNumberValue() {
		this.memoryNumberValue = this.memoryNumberCombo.getSelectionIndex();
		this.memoryNumberCombo.setBackground(SWTResourceManager.getColor(SWT.COLOR_CYAN));
	}

	/**
	 * @param enabled the isMemorySelectionChanged to set
	 */
	void setMemorySelectionChanged(boolean enabled) {
		this.isMemorySelectionChanged = enabled;
	}

	/**
	 * @param enabled the isCollectData to set
	 */
	void setCollectData(boolean enabled) {
		this.isCollectData = enabled;
	}

	/**
	 * @return the isCaptureOnly
	 */
	boolean isCaptureOnly() {
		return this.isCaptureOnly;
	}

	/**
	 * @return
	 */
	int getProgramNumber() {
		return new Integer(this.program.getText().split(" ")[0]).intValue();
	}

	/**
	 * @return
	 */
	int getAkkuType() {
		return new Integer(this.akkuType.getText().split(" ")[0]).intValue();
	}

	/**
	 * @return
	 */
	int getCellCount() {
		return new Integer(this.countCells.getText().split(" ")[0]).intValue();
	}

	/**
	 * @return
	 */
	int getAkkuCapacity() {
		return new Integer(this.capacityMilliAh.getText()).intValue();
	}

	/**
	 * @return
	 */
	int getDischargeCurrent() {
		return new Integer(this.dischargeCurrent.getText()).intValue();
	}

	/**
	 * @return
	 */
	int getChargeCurrent() {
		return new Integer(this.chargeCurrent.getText()).intValue();
	}

	/**
	 * @return the isMemorySelectionChanged
	 */
	boolean isMemorySelectionChanged() {
		return this.isMemorySelectionChanged;
	}

	/**
	 * @return
	 */
	int getMemoryNumberSelectionIndex() {
		return this.memoryNumberCombo.getSelectionIndex();
	}

	/**
	 * @param enabled
	 */
	void setStartDataGatheringSelection(boolean enabled) {
		this.startDataGatheringButton.setSelection(enabled);
	}

	/**
	 * @param newTimer the timer to set
	 */
	public void setTimer(Timer newTimer) {
		this.timer = newTimer;
	}

	/**
	 * @return the timer
	 */
	public Timer getTimer() {
		return this.timer;
	}

	/**
	 * @param newTimerTask the timerTask to set
	 */
	void setTimerTask(TimerTask newTimerTask) {
		this.timerTask = newTimerTask;
	}

	/**
	 * @return the channel
	 */
	Channel getChannel() {
		return this.channel;
	}

	/**
	 * set timeStamp using Date().getTime()
	 */
	public void setTimeStamp() {
		this.timeStamp = new Date().getTime();
	}

	/**
	 * @return the timeStamp
	 */
	public long getTimeStamp() {
		return this.timeStamp;
	}

	/**
	 * @param enabled the isCollectDataStopped to set
	 */
	void setCollectDataStopped(boolean enabled) {
		this.isCollectDataStopped = enabled;
	}

	/**
	 * @param newRecordSetKey the recordSetKey to set
	 */
	public void setRecordSetKey(String newRecordSetKey) {
		this.recordSetKey = newRecordSetKey;
	}

	/**
	 * @return the recordSetKey
	 */
	public String getRecordSetKey() {
		return this.recordSetKey;
	}

	/**
	 * @return the name
	 */
	String getName() {
		return this.name;
	}

	/**
	 * @param newRecordSet the recordSet to set
	 */
	public void setRecordSet(RecordSet newRecordSet) {
		this.recordSet = newRecordSet;
	}

	/**
	 * @return the recordSet
	 */
	public RecordSet getRecordSet() {
		return this.recordSet;
	}

	/**
	 * @return the channels
	 */
	public Channels getChannels() {
		return this.channels;
	}

	/**
	 * @return the timerTask
	 */
	public TimerTask getTimerTask() {
		return this.timerTask;
	}

	/**
	 * @param isAdded the isChargeCurrentAdded to set
	 */
	public void setChargeCurrentAdded(boolean isAdded) {
		this.isChargeCurrentAdded = isAdded;
	}

	/**
	 * @return the isChargeCurrentAdded
	 */
	public boolean isChargeCurrentAdded() {
		return this.isChargeCurrentAdded;
	}

	/**
	 * @param isAdded the isDischargeCurrentAdded to set
	 */
	public void setDischargeCurrentAdded(boolean isAdded) {
		this.isDischargeCurrentAdded = isAdded;
	}

	/**
	 * @return the isDischargeCurrentAdded
	 */
	public boolean isDischargeCurrentAdded() {
		return this.isDischargeCurrentAdded;
	}

	/**
	 * @param newRetryCounter the retryCounter to set
	 */
	public int setRetryCounter(int newRetryCounter) {
		return this.retryCounter = newRetryCounter;
	}

	/**
	 * @return the retryCounter
	 */
	public int getRetryCounter() {
		return this.retryCounter;
	}
}
