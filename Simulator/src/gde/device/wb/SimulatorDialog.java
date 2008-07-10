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
package osde.device.wb;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.HelpEvent;
import org.eclipse.swt.events.HelpListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import osde.config.Settings;
import osde.data.Channel;
import osde.data.Channels;
import osde.data.RecordSet;
import osde.device.DeviceDialog;
import osde.exception.DataInconsitsentException;
import osde.messages.Messages;
import osde.ui.OpenSerialDataExplorer;
import osde.ui.SWTResourceManager;

/**
 * Dialog sample for the simulator device 
 * @author Winfried Brügmann
 */
public class SimulatorDialog extends DeviceDialog {
	final static Logger						log		= Logger.getLogger(SimulatorDialog.class.getName());
	private static final String		DEVICE_NAME	= "Simulator";

	CLabel												currentLabel;
	CCombo												currentCombo;
	CLabel												voltageLabel;
	CCombo												voltageCombo;
	CLabel												timeLabel;
	CCombo												timeCombo;

	Button												stopButton;

	final Simulator								device;																																		// get device specific things, get serial port, ...
	Text													description;
	CLabel												descriptionLabel;
	Button												startButton;
	final SimulatorSerialPort			serialPort;																																// open/close port execute getData()....
	final OpenSerialDataExplorer	application;																																// interaction with application instance
	final Channels								channels;																																	// interaction with channels, source of all records
	final Settings								settings;

	Channel												channel;
	Timer													timer;
	CLabel												timeResultLabel;
	CLabel												timesLabel;
	CCombo												clusterCombo;
	CLabel												clusterLabel;
	TimerTask											timerTask;
	boolean												isCollectDataStopped	= false;
	int														recordNumber					= 0;

	/**
	* main method to test this dialog inside a shell 
	*/
	public static void main(String[] args) {
		try {
			Display display = Display.getDefault();
			Shell shell = new Shell(display);
			Simulator device = new Simulator("c:\\Documents and Settings\\user\\Application Data\\OpenSerialDataExplorer\\Devices\\Htronic Akkumaster C4.ini"); //$NON-NLS-1$
			SimulatorDialog inst = new SimulatorDialog(shell, device);
			inst.open();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * default constructor initialize all variables required
	 * @param parent Shell
	 * @param useDevice device specific class implementation
	 */
	public SimulatorDialog(Shell parent, Simulator useDevice) {
		super(parent);
		this.serialPort = useDevice.getSerialPort();
		this.device = useDevice;
		this.application = OpenSerialDataExplorer.getInstance();
		this.channels = Channels.getInstance();
		this.settings = Settings.getInstance();
	}

	/**
	 * default method where the default controls are defined, this needs to be overwritten by specific device dialog
	 */
	@Override
	public void open() {
		try {
			log.fine("dialogShell.isDisposed() " + ((this.dialogShell == null) ? "null" : this.dialogShell.isDisposed())); //$NON-NLS-1$ //$NON-NLS-2$
			if (this.dialogShell == null || this.dialogShell.isDisposed()) {
				if (this.settings.isDeviceDialogsModal())
					this.dialogShell = new Shell(this.application.getShell(), SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
				else
					this.dialogShell = new Shell(this.application.getDisplay(), SWT.DIALOG_TRIM);

				SWTResourceManager.registerResourceUser(this.dialogShell);
				if(this.isAlphaEnabled) {
					this.dialogShell.setAlpha(this.shellAlpha); // TODO settings
					this.isFadeOut = false;
				}
				this.dialogShell.setLayout(new FormLayout());
				this.dialogShell.layout();
				this.dialogShell.pack();
				this.dialogShell.setSize(336, 393);
				this.dialogShell.setText(DEVICE_NAME + Messages.getString(osde.messages.MessageIds.OSDE_MSGT0273));
				this.dialogShell.setImage(SWTResourceManager.getImage("osde/resource/ToolBoxHot.gif")); //$NON-NLS-1$
				{
					FormData timeResultLabelLData = new FormData();
					timeResultLabelLData.width = 94;
					timeResultLabelLData.height = 22;
					timeResultLabelLData.left = new FormAttachment(0, 1000, 213);
					timeResultLabelLData.top = new FormAttachment(0, 1000, 111);
					this.timeResultLabel = new CLabel(this.dialogShell, SWT.NONE);
					this.timeResultLabel.setLayoutData(timeResultLabelLData);
					this.timeResultLabel.setText(Messages.getString(MessageIds.OSDE_MSGT1050));
				}
				{
					FormData timesLabelLData = new FormData();
					timesLabelLData.width = 13;
					timesLabelLData.height = 22;
					timesLabelLData.left = new FormAttachment(0, 1000, 106);
					timesLabelLData.top = new FormAttachment(0, 1000, 111);
					this.timesLabel = new CLabel(this.dialogShell, SWT.NONE);
					this.timesLabel.setLayoutData(timesLabelLData);
					this.timesLabel.setText(Messages.getString(MessageIds.OSDE_MSGT1051));
				}
				{
					FormData clusterComboLData = new FormData();
					clusterComboLData.width = 84;
					clusterComboLData.height = 17;
					clusterComboLData.left = new FormAttachment(0, 1000, 125);
					clusterComboLData.top = new FormAttachment(0, 1000, 138);
					this.clusterCombo = new CCombo(this.dialogShell, SWT.NONE);
					this.clusterCombo.setLayoutData(clusterComboLData);
					this.clusterCombo.setItems(new String[] { "10", "20", "50", "100" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
					this.clusterCombo.setText(new Integer(this.device.getDataBlockSize()).toString());
					this.clusterCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.fine("clusterCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							SimulatorDialog.this.device.setDataBlockSize(new Integer(SimulatorDialog.this.clusterCombo.getText()).intValue());
							log.fine(" new clusterSize = " + SimulatorDialog.this.device.getDataBlockSize()); //$NON-NLS-1$
						}
					});
				}
				{
					FormData clusterLabelLData = new FormData();
					clusterLabelLData.width = 82;
					clusterLabelLData.height = 22;
					clusterLabelLData.left = new FormAttachment(0, 1000, 125);
					clusterLabelLData.top = new FormAttachment(0, 1000, 111);
					this.clusterLabel = new CLabel(this.dialogShell, SWT.NONE);
					this.clusterLabel.setLayoutData(clusterLabelLData);
					this.clusterLabel.setText(Messages.getString(MessageIds.OSDE_MSGT1052));
				}
				{
					FormData currentLabelLData = new FormData();
					currentLabelLData.width = 70;
					currentLabelLData.height = 22;
					currentLabelLData.left = new FormAttachment(0, 1000, 29);
					currentLabelLData.top = new FormAttachment(0, 1000, 221);
					this.currentLabel = new CLabel(this.dialogShell, SWT.CENTER | SWT.EMBEDDED);
					this.currentLabel.setLayoutData(currentLabelLData);
					this.currentLabel.setText(Messages.getString(MessageIds.OSDE_MSGT1053));
				}
				{
					FormData currentComboLData = new FormData();
					currentComboLData.width = 84;
					currentComboLData.height = 17;
					currentComboLData.left = new FormAttachment(0, 1000, 29);
					currentComboLData.top = new FormAttachment(0, 1000, 244);
					this.currentCombo = new CCombo(this.dialogShell, SWT.NONE);
					this.currentCombo.setLayoutData(currentComboLData);
					this.currentCombo.setText("cCombo1"); //$NON-NLS-1$
					this.currentCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.fine("currentCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							//TODO add your code for currentCombo.widgetSelected
						}
					});
				}
				{
					FormData voltageLabelLData = new FormData();
					voltageLabelLData.width = 84;
					voltageLabelLData.height = 22;
					voltageLabelLData.left = new FormAttachment(0, 1000, 29);
					voltageLabelLData.top = new FormAttachment(0, 1000, 167);
					this.voltageLabel = new CLabel(this.dialogShell, SWT.NONE);
					this.voltageLabel.setLayoutData(voltageLabelLData);
					this.voltageLabel.setText(Messages.getString(MessageIds.OSDE_MSGT1054));
				}
				{
					FormData voltageComboLData = new FormData();
					voltageComboLData.width = 84;
					voltageComboLData.height = 17;
					voltageComboLData.left = new FormAttachment(0, 1000, 29);
					voltageComboLData.top = new FormAttachment(0, 1000, 198);
					this.voltageCombo = new CCombo(this.dialogShell, SWT.NONE);
					this.voltageCombo.setLayoutData(voltageComboLData);
					this.voltageCombo.setText("cCombo1"); //$NON-NLS-1$
					this.voltageCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.fine("voltageCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							//TODO add your code for voltageCombo.widgetSelected
						}
					});
				}
				{
					FormData timeLabelLData = new FormData();
					timeLabelLData.width = 74;
					timeLabelLData.height = 22;
					timeLabelLData.left = new FormAttachment(0, 1000, 29);
					timeLabelLData.top = new FormAttachment(0, 1000, 111);
					this.timeLabel = new CLabel(this.dialogShell, SWT.NONE);
					this.timeLabel.setLayoutData(timeLabelLData);
					this.timeLabel.setText(Messages.getString(MessageIds.OSDE_MSGT1055));
				}
				{
					FormData timeComboLData = new FormData();
					timeComboLData.width = 84;
					timeComboLData.height = 17;
					timeComboLData.left = new FormAttachment(0, 1000, 29);
					timeComboLData.top = new FormAttachment(0, 1000, 138);
					this.timeCombo = new CCombo(this.dialogShell, SWT.NONE);
					this.timeCombo.setLayoutData(timeComboLData);
					this.timeCombo.setItems(new String[] { "1", "2", "3", "4", "5", "10", "20", "50", "100", "1000", "10000" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$ //$NON-NLS-9$ //$NON-NLS-10$ //$NON-NLS-11$
					this.timeCombo.setText(String.format("%.0f", this.device.getTimeStep_ms())); //$NON-NLS-1$
					this.timeCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.fine("timeCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							SimulatorDialog.this.device.setTimeStep_ms(new Integer(SimulatorDialog.this.timeCombo.getText()).intValue());
							log.fine(" new timeStep_ms = " + SimulatorDialog.this.device.getTimeStep_ms()); //$NON-NLS-1$
						}
					});
				}
				this.dialogShell.addDisposeListener(new DisposeListener() {
					@Override
					public void widgetDisposed(DisposeEvent evt) {
						log.fine("dialogShell.widgetDisposed, event=" + evt); //$NON-NLS-1$
						//TODO check if some thing to do before exiting
					}
				});
				this.dialogShell.addKeyListener(new KeyAdapter() {
					@Override
					public void keyReleased(KeyEvent evt) {
						log.fine("dialogShell.keyReleased, event=" + evt); //$NON-NLS-1$
						//TODO add your code for dialogShell.keyReleased
					}
				});
				this.dialogShell.addHelpListener(new HelpListener() {
					public void helpRequested(HelpEvent evt) {
						log.fine("dialogShell.helpRequested, event=" + evt); //$NON-NLS-1$
						SimulatorDialog.this.application.openHelpDialog(DEVICE_NAME, "HelpInfo.html"); //$NON-NLS-2$
					}
				});
				this.dialogShell.addMouseTrackListener(new MouseTrackAdapter() {
					public void mouseEnter(MouseEvent evt) {
						log.finer("dialogShell.mouseEnter, event=" + evt); //$NON-NLS-1$
						fadeOutAplhaBlending(evt, SimulatorDialog.this.getDialogShell().getClientArea(), 10, 10, 0, 10);
					}
					public void mouseHover(MouseEvent evt) {
						log.finest("dialogShell.mouseHover, event=" + evt); //$NON-NLS-1$
					}
					public void mouseExit(MouseEvent evt) {
						log.finer("dialogShell.mouseExit, event=" + evt); //$NON-NLS-1$
						fadeInAlpaBlending(evt, SimulatorDialog.this.getDialogShell().getClientArea(), 10, 10, 0, 10);
					}
				});
				{
					FormData descriptionLData = new FormData();
					descriptionLData.width = 277;
					descriptionLData.height = 60;
					descriptionLData.left = new FormAttachment(0, 1000, 26);
					descriptionLData.top = new FormAttachment(0, 1000, 49);
					this.description = new Text(this.dialogShell, SWT.CENTER | SWT.WRAP);
					this.description.setLayoutData(descriptionLData);
					this.description.setText(Messages.getString(MessageIds.OSDE_MSGT1056));
					this.description.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
					// enable fade in for big areas inside the dialog while fast mouse move
					this.description.addMouseTrackListener(SimulatorDialog.this.mouseTrackerEnterFadeOut);
				}
				{
					FormData descriptionLabelLData = new FormData();
					descriptionLabelLData.width = 304;
					descriptionLabelLData.height = 37;
					descriptionLabelLData.left = new FormAttachment(0, 1000, 12);
					descriptionLabelLData.top = new FormAttachment(0, 1000, 12);
					descriptionLabelLData.right = new FormAttachment(1000, 1000, -12);
					this.descriptionLabel = new CLabel(this.dialogShell, SWT.CENTER | SWT.WRAP | SWT.EMBEDDED);
					this.descriptionLabel.setLayoutData(descriptionLabelLData);
					this.descriptionLabel.setText(Messages.getString(MessageIds.OSDE_MSGT1057));
					this.descriptionLabel.setFont(SWTResourceManager.getFont("Microsoft Sans Serif", 14, 0, false, false)); //$NON-NLS-1$
					// enable fade in for big areas inside the dialog while fast mouse move
					this.descriptionLabel.addMouseTrackListener(SimulatorDialog.this.mouseTrackerEnterFadeOut);
				}
				{
					FormData startButtonLData = new FormData();
					startButtonLData.width = 121;
					startButtonLData.height = 31;
					startButtonLData.left = new FormAttachment(0, 1000, 29);
					startButtonLData.top = new FormAttachment(0, 1000, 296);
					this.startButton = new Button(this.dialogShell, SWT.PUSH | SWT.CENTER);
					this.startButton.setLayoutData(startButtonLData);
					this.startButton.setText(Messages.getString(osde.messages.MessageIds.OSDE_MSGT0274));
					this.startButton.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.fine("startButton.widgetSelected, event=" + evt); //$NON-NLS-1$
							SimulatorDialog.this.startButton.setEnabled(false);
							SimulatorDialog.this.stopButton.setEnabled(true);
							SimulatorDialog.this.channel = SimulatorDialog.this.channels.getActiveChannel();
							SimulatorDialog.this.channels.switchChannel(SimulatorDialog.this.channel.getName());

							// prepare timed data gatherer thread
							int delay = 0;
							int period = new Double(SimulatorDialog.this.device.getTimeStep_ms() * SimulatorDialog.this.device.getDataBlockSize()).intValue();
							log.fine("timer period = " + period + " ms"); //$NON-NLS-1$ //$NON-NLS-2$
							SimulatorDialog.this.timer = new Timer();
							SimulatorDialog.this.timerTask = new TimerTask() {
								String									recordSetKeyStem	= Messages.getString(MessageIds.OSDE_MSGT1058); 
								HashMap<String, Object>	data;															// Spannung, Strom
								String									recordSetKey;

								@Override
								@SuppressWarnings("unchecked") //$NON-NLS-1$
								//(Vector<Integer>)
								public void run() {
									RecordSet recordSet;
									try {

										if (SimulatorDialog.this.channel.size() == 0 || SimulatorDialog.this.isCollectDataStopped) {
											SimulatorDialog.this.isCollectDataStopped = false;
											SimulatorDialog.this.recordNumber++;

											this.recordSetKey = (SimulatorDialog.this.channel.size() + 1) + this.recordSetKeyStem;
											SimulatorDialog.this.channel.put(this.recordSetKey, RecordSet.createRecordSet(SimulatorDialog.this.device.getChannelName(1), this.recordSetKey, SimulatorDialog.this.application
													.getActiveDevice(), true, false));
											log.fine(this.recordSetKey + " created for channel " + SimulatorDialog.this.channel.getName()); //$NON-NLS-1$
											if (SimulatorDialog.this.channel.getActiveRecordSet() == null) Channels.getInstance().getActiveChannel().setActiveRecordSet(this.recordSetKey);
											recordSet = SimulatorDialog.this.channel.get(this.recordSetKey);
											recordSet.setTableDisplayable(false); // suppress table display during live data gathering
											recordSet.setTimeStep_ms(new Double(SimulatorDialog.this.device.getTimeStep_ms()));
											recordSet.setAllDisplayable();
											SimulatorDialog.this.channel.applyTemplate(this.recordSetKey);

											// switch the active record set if the current record set is child of active channel
											if (SimulatorDialog.this.channel.getName().equals(SimulatorDialog.this.channels.getActiveChannel().getName())) {
												SimulatorDialog.this.application.getMenuToolBar().addRecordSetName(this.recordSetKey);
												SimulatorDialog.this.channels.getActiveChannel().switchRecordSet(this.recordSetKey);
											}
											log.fine("recordSetKey = " + this.recordSetKey + " channelKonfigKey = " + recordSet.getChannelConfigName()); //$NON-NLS-1$ //$NON-NLS-2$
										}
										else {
											recordSet = SimulatorDialog.this.channel.get(this.recordSetKey);
											log.fine("re-using " + this.recordSetKey); //$NON-NLS-1$
										}
										// prepare the data for adding to record set
										
										this.data = SimulatorDialog.this.serialPort.getData(SimulatorDialog.this.recordNumber, recordSet.getChannelConfigName());

										// build the point array according curves from record set
										int[] points = new int[recordSet.size()];

										String[] measurements = SimulatorDialog.this.device.getMeasurementNames(recordSet.getChannelConfigName()); // 0=Spannung, 1=Strom
										Vector<Integer> voltage = (Vector<Integer>) this.data.get(measurements[0]);
										Vector<Integer> current = (Vector<Integer>) this.data.get(measurements[1]);
										Iterator<Integer> iterV = voltage.iterator();
										Iterator<Integer> iterA = current.iterator();
										while (iterV.hasNext()) {
											points[0] = iterV.next().intValue();//Spannung 
											points[1] = iterA.next().intValue();//Strom 
											log.fine(String.format("Spannung = %d mV, Strom = %d mA", points[0], points[1])); //$NON-NLS-1$
											recordSet.addPoints(points, false);
										}

										SimulatorDialog.this.application.updateGraphicsWindow();
										SimulatorDialog.this.application.updateDigitalWindowChilds();
										SimulatorDialog.this.application.updateAnalogWindowChilds();
									}
									catch (DataInconsitsentException e) {
										if (SimulatorDialog.this.timerTask != null) SimulatorDialog.this.timerTask.cancel();
										if (SimulatorDialog.this.timer != null) SimulatorDialog.this.timer.cancel();
										SimulatorDialog.log.log(Level.SEVERE, e.getMessage(), e);
										SimulatorDialog.this.application.openMessageDialog(Messages.getString(osde.messages.MessageIds.OSDE_MSGE0028, new Object[] { e.getClass().getSimpleName(), e.getMessage() }));
									}
									catch (Exception e) {
										log.log(Level.SEVERE, e.getMessage(), e);
									}
								}
							};
							SimulatorDialog.this.timer.scheduleAtFixedRate(SimulatorDialog.this.timerTask, delay, period);
						}
					});
				}
				{
					FormData okButtonLData = new FormData();
					okButtonLData.width = 121;
					okButtonLData.height = 31;
					okButtonLData.left = new FormAttachment(0, 1000, 180);
					okButtonLData.top = new FormAttachment(0, 1000, 296);
					this.stopButton = new Button(this.dialogShell, SWT.PUSH | SWT.CENTER);
					this.stopButton.setLayoutData(okButtonLData);
					this.stopButton.setText(Messages.getString(osde.messages.MessageIds.OSDE_MSGT0275));
					this.stopButton.setEnabled(false);
					this.stopButton.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.fine("stopButton.widgetSelected, event=" + evt); //$NON-NLS-1$
							stopTimer();
							SimulatorDialog.this.isCollectDataStopped = true;
							SimulatorDialog.this.startButton.setEnabled(true);
							SimulatorDialog.this.stopButton.setEnabled(false);
							SimulatorDialog.this.channels.getActiveChannel().getActiveRecordSet().setTableDisplayable(true); // enable table display after calculation
							SimulatorDialog.this.application.updateDataTable();
						}
					});
				}
				this.dialogShell.setLocation(getParent().toDisplay(100, 100));
				this.dialogShell.open();
			}
			else {
				this.dialogShell.setVisible(true);
				this.dialogShell.setActive();
			}
			Display display = this.dialogShell.getDisplay();
			while (!this.dialogShell.isDisposed()) {
				if (!display.readAndDispatch()) display.sleep();
			}
		}
		catch (Exception e) {
			log.log(Level.SEVERE, e.getMessage(), e);
		}
	}

	/**
	 * stop the timer task thread, this tops data capturing
	 */
	public void stopTimer() {
		if (this.timerTask != null) this.timerTask.cancel();
		if (this.timer != null) this.timer.cancel();
	}

}
