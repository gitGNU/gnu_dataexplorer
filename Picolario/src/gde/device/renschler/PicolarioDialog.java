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
package osde.device.renschler;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.HelpEvent;
import org.eclipse.swt.events.HelpListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Shell;

import osde.config.Settings;
import osde.data.Channel;
import osde.data.Channels;
import osde.data.RecordSet;
import osde.device.DeviceDialog;
import osde.messages.Messages;
import osde.ui.OpenSerialDataExplorer;
import osde.ui.SWTResourceManager;

/**
 * Dialog class for the Picolariolog device of Uwe Renschler
 * @author Winfried Brügmann
 */
public class PicolarioDialog extends DeviceDialog {
	final static Logger				log								= Logger.getLogger(PicolarioDialog.class.getName());
	private static final String	DEVICE_NAME	= "Picolario";

	Group											numberAvailableRecorsSetsGroup1;
	Button										queryAvailableRecordSetButton;
	CLabel										numberAvailableRecordSetsLabel;
	String										numberAvailable		= "0"; //$NON-NLS-1$

	CTabFolder								configTabFolder;
	CTabItem									configTabItem1, configTabItem2;
	PicolarioConfigTab				configTab1, configTab2;

	Group											readDataGroup3;
	Button										readSingle;
	Button										closeButton;
	Button										stopButton;
	CLabel										alreadyRedLabel;
	CLabel										alreadyRedDataSetsLabel;
	CLabel										redDataSets;
	Button										switchRecordSetButton;
	Button										readAllRecords;
	CLabel										numberRedTelegramLabel;
	CCombo										recordSetSelectCombo;
	String										redDatagrams			= "0"; //$NON-NLS-1$
	String										redDataSetsText		= "0"; //$NON-NLS-1$
	boolean										doSwtichRecordSet	= false;

	final Settings						settings;
	final Picolario						device;
	final PicolarioSerialPort	serialPort;
	OpenSerialDataExplorer		application;
	DataGathererThread				gatherThread;

	/**
	 * constructor initialize all variables required
	 * @param parent Shell
	 * @param useDevice Picolario class implementation == IDevice
	 */
	public PicolarioDialog(Shell parent, Picolario useDevice) {
		super(parent);
		this.device = useDevice;
		this.serialPort = useDevice.getSerialPort();
		this.application = OpenSerialDataExplorer.getInstance();
		this.settings = Settings.getInstance();
	}

	@Override
	public void open() {
		this.shellAlpha = Settings.getInstance().getDialogAlphaValue(); 
		this.isAlphaEnabled = Settings.getInstance().isDeviceDialogAlphaEnabled();

		log.log(Level.FINE, "dialogShell.isDisposed() " + ((this.dialogShell == null) ? "null" : this.dialogShell.isDisposed())); //$NON-NLS-1$ //$NON-NLS-2$
		if (this.dialogShell == null || this.dialogShell.isDisposed()) {
			if (this.settings.isDeviceDialogsModal())
				this.dialogShell = new Shell(this.application.getShell(), SWT.DIALOG_TRIM | SWT.PRIMARY_MODAL);
			else if (this.settings.isDeviceDialogsOnTop())
				this.dialogShell = new Shell(this.application.getDisplay(), SWT.DIALOG_TRIM | SWT.ON_TOP);
			else
				this.dialogShell = new Shell(this.application.getDisplay(), SWT.DIALOG_TRIM);

			SWTResourceManager.registerResourceUser(this.dialogShell);
			if (this.isAlphaEnabled) this.dialogShell.setAlpha(254);
			this.dialogShell.setLayout(null);
			this.dialogShell.layout();
			this.dialogShell.pack();
			this.dialogShell.setSize(345, 590);
			this.dialogShell.setText(DEVICE_NAME + Messages.getString(osde.messages.MessageIds.OSDE_MSGT0273));
			this.dialogShell.setImage(SWTResourceManager.getImage("osde/resource/ToolBoxHot.gif")); //$NON-NLS-1$
			this.dialogShell.addDisposeListener(new DisposeListener() {
				public void widgetDisposed(DisposeEvent evt) {
					log.log(Level.FINE, "dialogShell.widgetDisposed, event=" + evt); //$NON-NLS-1$
					if (PicolarioDialog.this.gatherThread != null && PicolarioDialog.this.gatherThread.isAlive()) PicolarioDialog.this.gatherThread.setThreadStop(true);
				}
			});
			this.dialogShell.addPaintListener(new PaintListener() {
				public void paintControl(PaintEvent evt) {
					log.log(Level.FINEST, "dialogShell.paintControl, event=" + evt); //$NON-NLS-1$
					PicolarioDialog.this.configTabFolder.setSelection(Channels.getInstance().getActiveChannelNumber() - 1);
				}
			});
			this.dialogShell.addHelpListener(new HelpListener() {
				public void helpRequested(HelpEvent evt) {
					log.log(Level.FINEST, "dialogShell.helpRequested, event=" + evt); //$NON-NLS-1$
					PicolarioDialog.this.application.openHelpDialog(DEVICE_NAME, "HelpInfo.html"); //$NON-NLS-2$
				}
			});
			this.dialogShell.addMouseTrackListener(new MouseTrackAdapter() {
				public void mouseEnter(MouseEvent evt) {
					log.log(Level.FINER, "dialogShell.mouseEnter, event=" + evt); //$NON-NLS-1$
					fadeOutAplhaBlending(evt, PicolarioDialog.this.getDialogShell().getClientArea(), 10, 10, 10, 15);
				}
				public void mouseHover(MouseEvent evt) {
					log.log(Level.FINEST, "dialogShell.mouseHover, event=" + evt); //$NON-NLS-1$
				}
				public void mouseExit(MouseEvent evt) {
					log.log(Level.FINER, "dialogShell.mouseExit, event=" + evt); //$NON-NLS-1$
					fadeInAlpaBlending(evt, PicolarioDialog.this.getDialogShell().getClientArea(), 10, 10, -10, 15);
				}
			});

			{ // group 1
				this.numberAvailableRecorsSetsGroup1 = new Group(this.dialogShell, SWT.NONE);
				this.numberAvailableRecorsSetsGroup1.setLayout(null);
				this.numberAvailableRecorsSetsGroup1.setText(Messages.getString(MessageIds.OSDE_MSGT1200));
				this.numberAvailableRecorsSetsGroup1.setBounds(10, 5, 320, 60);
				this.numberAvailableRecorsSetsGroup1.addMouseTrackListener(PicolarioDialog.this.mouseTrackerEnterFadeOut);
				{
					this.queryAvailableRecordSetButton = new Button(this.numberAvailableRecorsSetsGroup1, SWT.PUSH | SWT.CENTER);
					this.queryAvailableRecordSetButton.setText(Messages.getString(MessageIds.OSDE_MSGT1201)); 
					this.queryAvailableRecordSetButton.setBounds(10, 25, 250, 25);
					this.queryAvailableRecordSetButton.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "anzahlAufzeichnungenButton.widgetSelected, event=" + evt); //$NON-NLS-1$
							try {
								if (PicolarioDialog.this.serialPort != null) {
									PicolarioDialog.this.setClosePossible(false);
									int availableRecords = PicolarioDialog.this.serialPort.readNumberAvailableRecordSets();
									PicolarioDialog.this.numberAvailable = new Integer(availableRecords).toString();
									PicolarioDialog.this.numberAvailableRecordSetsLabel.setText(PicolarioDialog.this.numberAvailable);
									setRecordSetSelection(availableRecords, 0);
									PicolarioDialog.this.readSingle.setEnabled(true);
									PicolarioDialog.this.readAllRecords.setEnabled(true);
									resetTelegramLabel();
									resetDataSetsLabel();
									PicolarioDialog.this.setClosePossible(true);
								}
							}
							catch (Exception e) {
								PicolarioDialog.this.setClosePossible(true);
								PicolarioDialog.this.serialPort.close();
								PicolarioDialog.this.application.openMessageDialog(Messages.getString(osde.messages.MessageIds.OSDE_MSGE0024, new Object[] { e.getClass().getSimpleName(), e.getMessage() } ));
								PicolarioDialog.this.application.getDeviceSelectionDialog().open();
							}
						}
					});
				}
				{
					this.numberAvailableRecordSetsLabel = new CLabel(this.numberAvailableRecorsSetsGroup1, SWT.RIGHT | SWT.BORDER);
					this.numberAvailableRecordSetsLabel.setBackground(OpenSerialDataExplorer.COLOR_WHITE);
					this.numberAvailableRecordSetsLabel.setBounds(270, 25, 30, 24);
				}
			} // end group1

			{ // config tab 2
				this.configTabFolder = new CTabFolder(this.dialogShell, SWT.BORDER);

				if (this.device.getChannelCount() > 0) {
					this.configTabItem1 = new CTabItem(this.configTabFolder, SWT.NONE);
					this.configTabItem1.setText(this.device.getChannelName(1));
					this.configTab1 = new PicolarioConfigTab(this.configTabFolder, this.device, this.device.getChannelName(1));
					this.configTabItem1.setControl(this.configTab1);
				}
				if (this.device.getChannelCount() > 1) {
					this.configTabItem2 = new CTabItem(this.configTabFolder, SWT.NONE);
					this.configTabItem2.setText(this.device.getChannelName(2));
					this.configTab2 = new PicolarioConfigTab(this.configTabFolder, this.device, this.device.getChannelName(2));
					this.configTabItem2.setControl(this.configTab2);
				}

				this.configTabFolder.setSelection(0);
				this.configTabFolder.setBounds(10, 70, 320, 235);
				this.configTabFolder.addMouseTrackListener(PicolarioDialog.this.mouseTrackerEnterFadeOut);
				this.configTabFolder.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent evt) {
						log.log(Level.FINEST, "configTabFolder.widgetSelected, event=" + evt); //$NON-NLS-1$
						int channelNumber = PicolarioDialog.this.configTabFolder.getSelectionIndex() + 1;
						String configKey = channelNumber + " : " + ((CTabItem) evt.item).getText();
						Channels channels = Channels.getInstance();
						Channel activeChannel = channels.getActiveChannel();
						if (activeChannel != null) {
							log.log(Level.FINE, "activeChannel = " + activeChannel.getName() + " configKey = " + configKey); //$NON-NLS-1$ //$NON-NLS-2$
							RecordSet activeRecordSet = activeChannel.getActiveRecordSet();
							if (activeRecordSet != null && !activeChannel.getName().trim().equals(configKey)) {
								int answer = PicolarioDialog.this.application.openYesNoMessageDialog(Messages.getString(MessageIds.OSDE_MSGT1202));
								if (answer == SWT.YES) {
									String recordSetKey = activeRecordSet.getName();
									log.log(Level.FINE, "move record set " + recordSetKey + " to configuration " + configKey); //$NON-NLS-1$ //$NON-NLS-2$
									channels.get(channelNumber).put(recordSetKey, activeRecordSet.clone(configKey.split(":")[1].trim())); //$NON-NLS-1$
									activeChannel.remove(recordSetKey);
									channels.switchChannel(channelNumber, recordSetKey);
									PicolarioDialog.this.getDialogShell().redraw();
								}
							}
						}
					}
				});
			} // config tab 2

			{ // group 3
				this.readDataGroup3 = new Group(this.dialogShell, SWT.NONE);
				this.readDataGroup3.setLayout(null);
				this.readDataGroup3.setText(Messages.getString(MessageIds.OSDE_MSGT1203));
				this.readDataGroup3.setBounds(10, 310, 320, 195);
				this.readDataGroup3.addMouseTrackListener(PicolarioDialog.this.mouseTrackerEnterFadeOut);
				{
					this.switchRecordSetButton = new Button(this.readDataGroup3, SWT.CHECK | SWT.CENTER);
					this.switchRecordSetButton.setBounds(15, 20, 290, 17);
					this.switchRecordSetButton.setText(Messages.getString(MessageIds.OSDE_MSGT1204));
					this.switchRecordSetButton.setSelection(this.doSwtichRecordSet);
					this.switchRecordSetButton.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "switchRecordSetButton.widgetSelected, event=" + evt); //$NON-NLS-1$
							PicolarioDialog.this.doSwtichRecordSet = PicolarioDialog.this.switchRecordSetButton.getSelection();
						}
					});
				}
				{
					this.readSingle = new Button(this.readDataGroup3, SWT.PUSH | SWT.CENTER);
					this.readSingle.setText(Messages.getString(MessageIds.OSDE_MSGT1205));
					this.readSingle.setBounds(10, 45, 240, 25);
					this.readSingle.setEnabled(false);
					this.readSingle.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "ausleseButton.widgetSelected, event=" + evt); //$NON-NLS-1$
							PicolarioDialog.this.setClosePossible(false);
							PicolarioDialog.this.queryAvailableRecordSetButton.setEnabled(false);
							PicolarioDialog.this.readSingle.setEnabled(false);
							PicolarioDialog.this.readAllRecords.setEnabled(false);
							PicolarioDialog.this.stopButton.setEnabled(true);
							PicolarioDialog.this.gatherThread = new DataGathererThread(PicolarioDialog.this.application, PicolarioDialog.this.device, PicolarioDialog.this.serialPort,
									new String[] { PicolarioDialog.this.recordSetSelectCombo.getText() });
							PicolarioDialog.this.gatherThread.start();
							log.log(Level.FINE, "gatherThread.run() - executing"); //$NON-NLS-1$
						} // end widget selected
					}); // end selection adapter
				}
				{
					this.recordSetSelectCombo = new CCombo(this.readDataGroup3, SWT.BORDER | SWT.RIGHT);
					this.recordSetSelectCombo.setText("0"); //$NON-NLS-1$
					this.recordSetSelectCombo.setBounds(260, 47, 45, 21);
				}
				{
					this.numberRedTelegramLabel = new CLabel(this.readDataGroup3, SWT.RIGHT);
					this.numberRedTelegramLabel.setBounds(10, 75, 234, 24);
					this.numberRedTelegramLabel.setText(Messages.getString(MessageIds.OSDE_MSGT1206));
					this.numberRedTelegramLabel.setForeground(SWTResourceManager.getColor(64, 128, 128));
				}
				{
					this.alreadyRedLabel = new CLabel(this.readDataGroup3, SWT.RIGHT);
					this.alreadyRedLabel.setBounds(244, 75, 56, 24);
					this.alreadyRedLabel.setText(this.redDatagrams);
				}
				{
					this.readAllRecords = new Button(this.readDataGroup3, SWT.PUSH | SWT.CENTER);
					this.readAllRecords.setBounds(8, 100, 300, 25);
					this.readAllRecords.setText(Messages.getString(MessageIds.OSDE_MSGT1207));
					this.readAllRecords.setEnabled(false);
					this.readAllRecords.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "readAllRecords.widgetSelected, event=" + evt); //$NON-NLS-1$
							PicolarioDialog.this.setClosePossible(false);
							PicolarioDialog.this.queryAvailableRecordSetButton.setEnabled(false);
							PicolarioDialog.this.readAllRecords.setEnabled(false);
							PicolarioDialog.this.readSingle.setEnabled(false);
							PicolarioDialog.this.stopButton.setEnabled(true);
							String[] itemNames = PicolarioDialog.this.recordSetSelectCombo.getItems();
							PicolarioDialog.this.gatherThread = new DataGathererThread(PicolarioDialog.this.application, PicolarioDialog.this.device, PicolarioDialog.this.serialPort, itemNames);
							PicolarioDialog.this.gatherThread.start();
							log.log(Level.FINE, "gatherThread.run() - executing"); //$NON-NLS-1$
						}
					});
				}
				{
					this.alreadyRedDataSetsLabel = new CLabel(this.readDataGroup3, SWT.RIGHT);
					this.alreadyRedDataSetsLabel.setBounds(10, 125, 234, 24);
					this.alreadyRedDataSetsLabel.setForeground(SWTResourceManager.getColor(64, 128, 128));
					this.alreadyRedDataSetsLabel.setText(Messages.getString(MessageIds.OSDE_MSGT1208));
				}
				{
					this.redDataSets = new CLabel(this.readDataGroup3, SWT.RIGHT);
					this.redDataSets.setBounds(244, 125, 56, 24);
					this.redDataSets.setText(this.redDataSetsText);
				}
				{
					this.stopButton = new Button(this.readDataGroup3, SWT.PUSH | SWT.CENTER);
					this.stopButton.setText(Messages.getString(osde.messages.MessageIds.OSDE_MSGT0278));
					this.stopButton.setEnabled(false);
					this.stopButton.setBounds(80, 155, 150, 25);
					this.stopButton.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "stopButton.widgetSelected, event=" + evt); //$NON-NLS-1$
							PicolarioDialog.this.gatherThread.setThreadStop(true);
							PicolarioDialog.this.setClosePossible(true);
						}
					});
				}
			} // end group 3

			{
				this.closeButton = new Button(this.dialogShell, SWT.PUSH | SWT.CENTER);
				this.closeButton.setText(Messages.getString(osde.messages.MessageIds.OSDE_MSGT0188));
				this.closeButton.setBounds(70, 520, 200, 25);
				this.closeButton.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent evt) {
						log.log(Level.FINE, "closeButton.widgetSelected, event=" + evt); //$NON-NLS-1$
						dispose();
					}
				});
			}

			this.dialogShell.setLocation(getParent().toDisplay(getParent().getSize().x/2-175, 100));
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

	public void setAvailableRecordSets(int number) {
		this.numberAvailableRecordSetsLabel.setText(new Integer(number).toString());
	}

	/**
	 * method to enable setting of selectable values for record set selection
	 * @param items
	 * @param index
	 */
	public void setRecordSetSelection(int items, int index) {
		String[] itemNames = new String[items];
		for (int i = 0; i < items; i++) {
			itemNames[i] = new Integer(i + 1).toString();
		}
		this.recordSetSelectCombo.setItems(itemNames);
		this.recordSetSelectCombo.select(index);
		this.recordSetSelectCombo.setVisibleItemCount(items);
	}

	/**
	 * function to reset counter labels fro red and calculated
	 */
	public void resetTelegramLabel() {
		OpenSerialDataExplorer.display.asyncExec(new Runnable() {
			public void run() {
				if (!PicolarioDialog.this.application.getDeviceDialog().isDisposed()) {
					if (!PicolarioDialog.this.application.getDeviceDialog().isDisposed()) {
						PicolarioDialog.this.redDatagrams = "0"; //$NON-NLS-1$
						PicolarioDialog.this.alreadyRedLabel.setText(PicolarioDialog.this.redDatagrams);
					}
				}
			}
		});
	}

	/**
	 * use this method to set displayed text during data gathering
	 * @param newValue
	 */
	public void setAlreadyRedText(final int newValue) {
		this.redDatagrams = new Integer(newValue).toString();
		OpenSerialDataExplorer.display.asyncExec(new Runnable() {
			public void run() {
				if (!PicolarioDialog.this.application.getDeviceDialog().isDisposed()) PicolarioDialog.this.alreadyRedLabel.setText(PicolarioDialog.this.redDatagrams);
			}
		});
	}

	/**
	 * function to reset counter labels fro red and calculated
	 */
	public void resetDataSetsLabel() {
		OpenSerialDataExplorer.display.asyncExec(new Runnable() {
			public void run() {
				if (!PicolarioDialog.this.application.getDeviceDialog().isDisposed()) {
					PicolarioDialog.this.redDataSetsText = "0"; //$NON-NLS-1$
					PicolarioDialog.this.redDataSets.setText(PicolarioDialog.this.redDataSetsText);
				}
			}
		});
	}

	/**
	 * use this method to set displayed text during data gathering
	 * @param newValue
	 */
	public void setAlreadyRedDataSets(final String newValue) {
		this.redDataSetsText = newValue;
		OpenSerialDataExplorer.display.asyncExec(new Runnable() {
			public void run() {
				if (!PicolarioDialog.this.application.getDeviceDialog().isDisposed()) {
					PicolarioDialog.this.redDataSets.setText(newValue);
				}
			}
		});
	}

	/**
	 * function to enable all the read data read buttons, normally called after data gathering finished
	 */
	public void enableReadButtons() {
		if (Thread.currentThread().getId() == this.application.getThreadId()) {
			this.queryAvailableRecordSetButton.setEnabled(true);
			this.readSingle.setEnabled(true);
			this.readAllRecords.setEnabled(true);
			this.stopButton.setEnabled(false);
			this.isClosePossible = true;
		}
		else {
			OpenSerialDataExplorer.display.asyncExec(new Runnable() {
				public void run() {
					if (!PicolarioDialog.this.application.getDeviceDialog().isDisposed()) {
						PicolarioDialog.this.queryAvailableRecordSetButton.setEnabled(true);
						PicolarioDialog.this.readSingle.setEnabled(true);
						PicolarioDialog.this.readAllRecords.setEnabled(true);
						PicolarioDialog.this.stopButton.setEnabled(false);
						PicolarioDialog.this.setClosePossible(true);
					}
				}
			});
		}
	}

	/**
	 * @return the doSwtichRecordSet
	 */
	public boolean isDoSwtichRecordSet() {
		return this.doSwtichRecordSet;
	}

	/**
	 * reset the button states to default
	 */
	public void resetButtons() {
		if (Thread.currentThread().getId() == this.application.getThreadId()) {
			PicolarioDialog.this.setClosePossible(true);
			PicolarioDialog.this.queryAvailableRecordSetButton.setEnabled(true);
			PicolarioDialog.this.readSingle.setEnabled(true);
			PicolarioDialog.this.readAllRecords.setEnabled(true);
			PicolarioDialog.this.stopButton.setEnabled(false);
		}
		else {
			OpenSerialDataExplorer.display.asyncExec(new Runnable() {
				public void run() {
					PicolarioDialog.this.setClosePossible(true);
					PicolarioDialog.this.queryAvailableRecordSetButton.setEnabled(true);
					PicolarioDialog.this.readSingle.setEnabled(true);
					PicolarioDialog.this.readAllRecords.setEnabled(true);
					PicolarioDialog.this.stopButton.setEnabled(false);
				}
			});
		}
	}
}
