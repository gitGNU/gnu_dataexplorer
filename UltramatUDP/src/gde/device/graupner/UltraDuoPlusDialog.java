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
    
    Copyright (c) 2008,2009,2010,2011,2012,2013 Winfried Bruegmann
****************************************************************************************/
package gde.device.graupner;

import gde.GDE;
import gde.comm.DeviceSerialPortImpl;
import gde.config.Settings;
import gde.data.Channel;
import gde.data.Channels;
import gde.data.Record;
import gde.data.RecordSet;
import gde.device.DataTypes;
import gde.device.DeviceConfiguration;
import gde.device.DeviceDialog;
import gde.device.MeasurementPropertyTypes;
import gde.device.PropertyType;
import gde.device.graupner.UltraDuoPlusSychronizer.SYNC_TYPE;
import gde.device.graupner.UltraDuoPlusType.ChannelData1;
import gde.device.graupner.UltraDuoPlusType.ChannelData2;
import gde.device.graupner.UltraDuoPlusType.MotorRunData;
import gde.device.graupner.UltraDuoPlusType.TireHeaterData;
import gde.device.graupner.Ultramat.GraupnerDeviceType;
import gde.exception.DataInconsitsentException;
import gde.exception.SerialPortException;
import gde.exception.TimeOutException;
import gde.log.Level;
import gde.log.LogFormatter;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.ui.ParameterConfigControl;
import gde.ui.SWTResourceManager;
import gde.ui.tab.GraphicsWindow;
import gde.utils.FileUtils;
import gde.utils.StringHelper;
import gde.utils.WaitTimer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.Vector;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Logger;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.UnmarshalException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.HelpEvent;
import org.eclipse.swt.events.HelpListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

/**
 * Graupner Ultra Duo Plus setup dialog
 * @author Winfried Brügmann
 */
public class UltraDuoPlusDialog extends DeviceDialog {
	final static Logger				log												= Logger.getLogger(UltraDuoPlusDialog.class.getName());
	static final String				DEVICE_JAR_NAME						= "UltramatUDP";																																																											//$NON-NLS-1$
	static final String				STRING_FORMAT_02D					= "%02d";																																																														//$NON-NLS-1$
	static final String				STRING_FORMAT_02d_s				= "%02d - %s";																																																												//$NON-NLS-1$
	static final String				STRING_16_BLANK						= "                ";																																																								//$NON-NLS-1$
	static final String				ULTRA_DUO_PLUS_XSD				= "UltraDuoPlus_V02.xsd";																																																						//$NON-NLS-1$
	static final String				UDP_CONFIGURATION_SUFFIX	= "/UltraDuoPlus_";																																																									//$NON-NLS-1$

	Text											userNameText;
	Group											baseDeviceSetupGroup, baseDeviceSetupGroup1, baseDeviceSetupGroup2;
	CLabel										userLabel;
	Button										restoreButton;
	Button										backupButton;
	Button										closeButton;
	Button										helpButton;
	Button										copyButton;
	CTabFolder								mainTabFolder, chargeTypeTabFolder;
	CTabItem									setupTabItem, memorySetupTabItem, chargeTabItem, dischargeTabItem, memoryCycleDataTabItem;
	ScrolledComposite					scrolledchargeComposite;
	Composite									boundsComposite, deviceComposite, dischargeCycleComposite;
	Group											chargeGroup, dischargeGroup, cycleGroup;

	Composite									memoryBoundsComposite, memorySelectComposite;
	Composite									memoryDataComposite, memoryDataSelectComposite, channelSelectComposite;
	CLabel										memorySelectLabel, memoryDataSelectLabel, channelSelectLabel;
	CCombo										memoryCombo, memoryDataCombo, channelCombo;
	ProgressBar								cycleDataProgressBar, graphicsDataProgressBar;
	Table											dataTable;

	final Ultramat						device;																																																																												// get device specific things, get serial port, ...
	final UltramatSerialPort	serialPort;																																																																										// open/close port execute getData()....
	final Channels						channels;																																																																											// interaction with channels, source of all records
	final Settings						settings;																																																																											// application configuration settings
	final Listener						memoryParameterChangeListener;

	final static String[]			cellTypeNames							= Messages.getString(MessageIds.GDE_MSGT2246).split(GDE.STRING_COMMA);
	final static String[]			soundTime									= new String[] { Messages.getString(MessageIds.GDE_MSGT2241), "5sec", "15sec", "1min", Messages.getString(MessageIds.GDE_MSGT2240) }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	final static String[]			powerOnDisplayType				= new String[] { Messages.getString(MessageIds.GDE_MSGT2244), Messages.getString(MessageIds.GDE_MSGT2245) };
	final static String[]			cycleDirectionTypes				= Messages.getString(MessageIds.GDE_MSGT2292).split(GDE.STRING_COMMA);
	final static String[]			offOnType									= new String[] { Messages.getString(MessageIds.GDE_MSGT2241), Messages.getString(MessageIds.GDE_MSGT2240) };
	final static String[]			onOffType									= new String[] { Messages.getString(MessageIds.GDE_MSGT2240), Messages.getString(MessageIds.GDE_MSGT2241) };
	final static String[]			temperatureDegreeType			= new String[] { DeviceConfiguration.UNIT_DEGREE_CELSIUS, DeviceConfiguration.UNIT_DEGREE_FAHRENHEIT };
	final static String[]			languageTypes							= new String[] { "En", "De", "Fr", "It" };																																														//$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
	final static String[]			diableEnableType					= new String[] { Messages.getString(MessageIds.GDE_MSGT2243), Messages.getString(MessageIds.GDE_MSGT2242) };
	final static String[]			hourFormatType						= new String[] { "12H", "24H" };																																																			//$NON-NLS-1$ //$NON-NLS-2$

	Schema										schema;
	JAXBContext								jc;
	UltraDuoPlusType					ultraDuoPlusSetup;
	UltraDuoPlusSychronizer		synchronizerRead, synchronizerWrite;

	String										deviceIdentifierName			= "NEW USER NAME";																																																										//$NON-NLS-1$

	int[]											channelValues1						= new int[UltramatSerialPort.SIZE_CHANNEL_1_SETUP];
	int[]											channelValues2						= new int[UltramatSerialPort.SIZE_CHANNEL_2_SETUP];
	ParameterConfigControl[]	channelParameters					= new ParameterConfigControl[UltramatSerialPort.SIZE_CHANNEL_1_SETUP + UltramatSerialPort.SIZE_CHANNEL_2_SETUP];

	static int								numberMemories						= 60;
	String[]									memoryNames								= new String[UltraDuoPlusDialog.numberMemories];
	int[]											memoryValues							= new int[UltramatSerialPort.SIZE_MEMORY_SETUP];
	int[]											memoryValuesNiCd					= new int[UltramatSerialPort.SIZE_MEMORY_SETUP];
	int[]											memoryValuesNiMh					= new int[UltramatSerialPort.SIZE_MEMORY_SETUP];
	int[]											memoryValuesLiIo					= new int[UltramatSerialPort.SIZE_MEMORY_SETUP];
	int[]											memoryValuesLiPo					= new int[UltramatSerialPort.SIZE_MEMORY_SETUP];
	int[]											memoryValuesLiFe					= new int[UltramatSerialPort.SIZE_MEMORY_SETUP];
	int[]											memoryValuesPb						= new int[UltramatSerialPort.SIZE_MEMORY_SETUP];
	ParameterConfigControl[]	memoryParameters					= new ParameterConfigControl[UltramatSerialPort.SIZE_MEMORY_SETUP];
	int												lastMemorySelectionIndex	= -1;
	int												lastCellSelectionIndex		= -1;
	int												memorySelectionIndexData	= 1;
	int												channelSelectionIndex			= 1;
	String[]									channelNumbers						= { "1", "2" };																																																											//$NON-NLS-1$ //$NON-NLS-2$
	int												parameterSelectHeight			= 30;
	int												chargeSelectHeight				= 11 * this.parameterSelectHeight;
	int												dischargeSelectHeight			= 5 * this.parameterSelectHeight;
	int												cycleSelectHeight					= 4 * this.parameterSelectHeight;
	byte[]										initialAnswerData					= null;

	/**
	 * method to test this class
	 * @param args
	 */
	public static void main(String[] args) {
		Handler ch = new ConsoleHandler();
		LogFormatter lf = new LogFormatter();
		ch.setFormatter(lf);
		ch.setLevel(java.util.logging.Level.ALL);
		Logger.getLogger(GDE.STRING_EMPTY).addHandler(ch);
		Logger.getLogger(GDE.STRING_EMPTY).setLevel(Level.TIME);

		String basePath = Settings.getInstance().getApplHomePath();
		UltramatSerialPort serialPort = null;

		try {
			Schema schema = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema(
					new StreamSource(UltraDuoPlusDialog.class.getClassLoader().getResourceAsStream("resource/" + UltraDuoPlusDialog.ULTRA_DUO_PLUS_XSD))); //$NON-NLS-1$
			JAXBContext jc = JAXBContext.newInstance("gde.device.graupner"); //$NON-NLS-1$

			UltraDuoPlusType ultraDuoPlusSetup = new ObjectFactory().createUltraDuoPlusType();
			String deviceIdentifierName = GDE.STRING_EMPTY;
			log.log(Level.TIME, "XSD init time = " + StringHelper.getFormatedTime("ss:SSS", (new Date().getTime() - GDE.StartTime))); //$NON-NLS-1$ //$NON-NLS-2$

			serialPort = new UltramatSerialPort(new DeviceConfiguration(basePath + "/Devices/UltraDuoPlus60.xml")); //$NON-NLS-1$
			if (!serialPort.isConnected()) {
				try {
					long time = new Date().getTime();
					serialPort.open();
					serialPort.write(UltramatSerialPort.RESET_CONFIG);

					deviceIdentifierName = serialPort.readDeviceUserName(); //read the device identifier name to read available cache file

					try {
						Unmarshaller unmarshaller = jc.createUnmarshaller();
						unmarshaller.setSchema(schema);
						ultraDuoPlusSetup = (UltraDuoPlusType) unmarshaller.unmarshal(new File(basePath + UltraDuoPlusDialog.UDP_CONFIGURATION_SUFFIX
								+ deviceIdentifierName.replace(GDE.STRING_BLANK, GDE.STRING_UNDER_BAR) + GDE.FILE_ENDING_DOT_XML));
						log.log(Level.TIME, "read memory setup XML time = " + StringHelper.getFormatedTime("ss:SSS", (new Date().getTime() - time))); //$NON-NLS-1$ //$NON-NLS-2$
					}
					catch (Exception e) {
						log.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
						if (e.getCause() instanceof FileNotFoundException) {
							ultraDuoPlusSetup = new ObjectFactory().createUltraDuoPlusType();
							List<MemoryType> cellMemories = ultraDuoPlusSetup.getMemory();
							if (cellMemories.size() < UltraDuoPlusDialog.numberMemories) { // initially create only base setup data
								for (int i = 0; i < UltraDuoPlusDialog.numberMemories; i++) {
									MemoryType cellMemory = new ObjectFactory().createMemoryType();
									cellMemory.setSetupData(new ObjectFactory().createMemoryTypeSetupData());
									cellMemories.add(cellMemory);
								}
							}
						}
						else
							throw e;
					}

					ultraDuoPlusSetup.setIdentifierName(deviceIdentifierName);

					Thread readSynchronizer = new UltraDuoPlusSychronizer(null, serialPort, ultraDuoPlusSetup, UltraDuoPlusSychronizer.SYNC_TYPE.READ);
					readSynchronizer.start();
					readSynchronizer.join();
				}
				catch (Exception e) {
					log.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
					return;
				}
				finally {
					if (serialPort.isConnected()) {
						serialPort.write(UltramatSerialPort.RESET);
						serialPort.close();
					}

				}
			}

			if (ultraDuoPlusSetup != null) {
				//remove synchronized flag
				ultraDuoPlusSetup.getChannelData1().synced = null;
				ultraDuoPlusSetup.getChannelData2().synced = null;
				Iterator<MemoryType> iterator = ultraDuoPlusSetup.getMemory().iterator();
				while (iterator.hasNext()) {
					MemoryType cellMemory = iterator.next();
					cellMemory.synced = null;
					cellMemory.getSetupData().synced = null;
					if (cellMemory.getStepChargeData() != null) cellMemory.getStepChargeData().synced = null;
					if (cellMemory.getTraceData() != null) cellMemory.getTraceData().synced = null;
					if (cellMemory.getCycleData() != null) cellMemory.getCycleData().synced = null;
				}
				Iterator<TireHeaterData> tireIterator = ultraDuoPlusSetup.getTireHeaterData().iterator();
				while (tireIterator.hasNext()) {
					tireIterator.next().synced = null;
				}
				Iterator<MotorRunData> motorIterator = ultraDuoPlusSetup.getMotorRunData().iterator();
				while (motorIterator.hasNext()) {
					motorIterator.next().synced = null;
				}

				// store back manipulated XML
				Marshaller marshaller = jc.createMarshaller();
				marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.valueOf(true));
				marshaller.setProperty(Marshaller.JAXB_NO_NAMESPACE_SCHEMA_LOCATION, UltraDuoPlusDialog.ULTRA_DUO_PLUS_XSD);
				marshaller.marshal(ultraDuoPlusSetup, new FileOutputStream(basePath + UltraDuoPlusDialog.UDP_CONFIGURATION_SUFFIX + deviceIdentifierName.replace(GDE.STRING_BLANK, GDE.STRING_UNDER_BAR)
						+ GDE.FILE_ENDING_DOT_XML));
				log.log(Level.TIME, "write memory setup XML time = " + StringHelper.getFormatedTime("ss:SSS", (new Date().getTime() - GDE.StartTime))); //$NON-NLS-1$ //$NON-NLS-2$
			}

		}
		catch (Exception e) {
			log.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
		}
		catch (Throwable t) {
			log.log(java.util.logging.Level.SEVERE, t.getMessage(), t);
		}
	}

	/**
	 * default constructor initialize all variables required
	 * @param parent Shell
	 * @param useDevice device specific class implementation
	 */
	public UltraDuoPlusDialog(Shell parent, Ultramat useDevice) {
		super(parent);
		this.serialPort = useDevice.getCommunicationPort();
		this.device = useDevice;
		this.channels = Channels.getInstance();
		this.settings = Settings.getInstance();
		switch (this.device.getDeviceTypeIdentifier()) {
		case UltraDuoPlus45:
			UltraDuoPlusDialog.numberMemories = 45;
			this.channelNumbers = new String[] { "1" }; //$NON-NLS-1$
			break;
		case UltraDuoPlus50:
			UltraDuoPlusDialog.numberMemories = 50;
			this.channelNumbers = new String[] { "1", "2" }; //$NON-NLS-1$ //$NON-NLS-2$
			break;
		case UltraDuoPlus60:
			UltraDuoPlusDialog.numberMemories = 60;
			this.channelNumbers = new String[] { "1", "2" }; //$NON-NLS-1$ //$NON-NLS-2$
			break;
		default:
			break;
		}
		this.memoryNames = new String[UltraDuoPlusDialog.numberMemories];
		this.memoryNames[1] = " initial "; //$NON-NLS-1$

		this.memoryParameterChangeListener = new Listener() {
			public void handleEvent(Event evt) {
				if (UltraDuoPlusDialog.this.lastMemorySelectionIndex >= 0 && UltraDuoPlusDialog.this.lastMemorySelectionIndex < UltraDuoPlusDialog.numberMemories) {
					if (UltraDuoPlusDialog.this.ultraDuoPlusSetup != null) {
						log
								.log(
										java.util.logging.Level.FINE,
										"memoryComposite.handleEvent, (" + UltraDuoPlusDialog.this.lastMemorySelectionIndex + GDE.STRING_RIGHT_PARENTHESIS + UltraDuoPlusDialog.this.ultraDuoPlusSetup.getMemory().get(UltraDuoPlusDialog.this.lastMemorySelectionIndex).getName() + " memoryValues[" + evt.index + "] changed"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ 
						UltraDuoPlusDialog.this.ultraDuoPlusSetup.getMemory().get(UltraDuoPlusDialog.this.lastMemorySelectionIndex).getSetupData().setChanged(true);
						UltraDuoPlusDialog.this.ultraDuoPlusSetup.getMemory().get(UltraDuoPlusDialog.this.lastMemorySelectionIndex).getSetupData()
								.setValue(StringHelper.integer2Hex4ByteString(UltraDuoPlusDialog.this.memoryValues));
					}

					if (evt.index == 0) {
						updateBatteryMemoryParameter(UltraDuoPlusDialog.this.memoryValues[0]);
						updateBatteryParameterValues();
					}
					else if (evt.index == 2) { // capacity change
						updateBatteryParameterValues();
					}
					if (UltraDuoPlusDialog.this.ultraDuoPlusSetup != null && log.isLoggable(java.util.logging.Level.FINE)) {
						StringBuffer sb = new StringBuffer();
						for (int i = 0; i < UltramatSerialPort.SIZE_MEMORY_SETUP; i++) {
							sb.append(UltraDuoPlusDialog.this.memoryValues[i]).append(GDE.STRING_LEFT_BRACKET).append(i).append(GDE.STRING_RIGHT_BRACKET_COMMA);
						}
						log.log(java.util.logging.Level.FINE, sb.toString());
					}
				}
			}
		};
	}

	/**
	 * default method where the default controls are defined, this needs to be overwritten by specific device dialog
	 */
	@Override
	public void open() {
		long openStartTime = new Date().getTime();
		try {
			this.application.setCursor(SWTResourceManager.getCursor(SWT.CURSOR_WAIT));

			if (this.serialPort != null && !this.serialPort.isConnected()) {
				try {
					this.serialPort.open();
					this.serialPort.write(UltramatSerialPort.RESET);
					this.initialAnswerData = this.serialPort.getData(true);
					if (this.device.isProcessing(1, this.initialAnswerData) || this.device.isProcessing(2, this.initialAnswerData)) {
						this.application.openMessageDialogAsync(null, Messages.getString(MessageIds.GDE_MSGW2201));
						return;
					}
					this.serialPort.write(UltramatSerialPort.RESET_CONFIG);
					this.deviceIdentifierName = this.serialPort.readDeviceUserName();

					this.jc = JAXBContext.newInstance("gde.device.graupner"); //$NON-NLS-1$
					this.schema = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema(
							new StreamSource(UltraDuoPlusDialog.class.getClassLoader().getResourceAsStream("resource/" + UltraDuoPlusDialog.ULTRA_DUO_PLUS_XSD))); //$NON-NLS-1$

					try {
						Unmarshaller unmarshaller = this.jc.createUnmarshaller();
						unmarshaller.setSchema(this.schema);
						this.ultraDuoPlusSetup = (UltraDuoPlusType) unmarshaller.unmarshal(new File(this.settings.getApplHomePath() + UltraDuoPlusDialog.UDP_CONFIGURATION_SUFFIX
								+ this.deviceIdentifierName.replace(GDE.STRING_BLANK, GDE.STRING_UNDER_BAR) + GDE.FILE_ENDING_DOT_XML));
					}
					catch (UnmarshalException e) {
						log.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
						createUltraDuoPlusSetup(this.deviceIdentifierName);
					}
					catch (Exception e) {
						if (e.getCause() instanceof FileNotFoundException) {
							createUltraDuoPlusSetup(this.deviceIdentifierName);
						}
						else
							throw e;
					}

					this.synchronizerRead = new UltraDuoPlusSychronizer(this, this.serialPort, this.ultraDuoPlusSetup, UltraDuoPlusSychronizer.SYNC_TYPE.READ);
					this.synchronizerRead.start();
				}
				catch (Exception e) {
					log.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
					this.serialPort.close();
					this.application.openMessageDialog(null,
							Messages.getString(gde.messages.MessageIds.GDE_MSGE0015, new Object[] { e.getClass().getSimpleName() + GDE.STRING_BLANK_COLON_BLANK + e.getMessage() }));
					this.application.getDeviceSelectionDialog().open();
					if (!this.application.getActiveDevice().equals(this.device)) //check if device was changed
						return;
				}
			}
			else {
				log.log(java.util.logging.Level.SEVERE, "serial port == null"); //$NON-NLS-1$
				this.application.openMessageDialogAsync(null, Messages.getString(gde.messages.MessageIds.GDE_MSGE0010));
				this.application.getDeviceSelectionDialog().open();
				return;
			}

			log.log(java.util.logging.Level.FINE, "dialogShell.isDisposed() " + ((this.dialogShell == null) ? "null" : this.dialogShell.isDisposed())); //$NON-NLS-1$ //$NON-NLS-2$
			if (this.dialogShell == null || this.dialogShell.isDisposed()) {
				if (this.settings.isDeviceDialogsModal())
					this.dialogShell = new Shell(this.application.getShell(), SWT.DIALOG_TRIM | SWT.PRIMARY_MODAL);
				else if (this.settings.isDeviceDialogsOnTop())
					this.dialogShell = new Shell(this.application.getDisplay(), SWT.DIALOG_TRIM | SWT.ON_TOP);
				else
					this.dialogShell = new Shell(this.application.getDisplay(), SWT.DIALOG_TRIM);

				SWTResourceManager.registerResourceUser(this.dialogShell);
				this.dialogShell.setLayout(new FormLayout());
				this.dialogShell.setText(this.device.getName() + Messages.getString(gde.messages.MessageIds.GDE_MSGT0273));
				this.dialogShell.setImage(SWTResourceManager.getImage("gde/resource/ToolBoxHot.gif")); //$NON-NLS-1$
				this.dialogShell.layout();
				this.dialogShell.pack();
				this.dialogShell.setSize(655, GDE.IS_MAC ? 675 : 655);
				this.dialogShell.addListener(SWT.Traverse, new Listener() {
		      public void handleEvent(Event event) {
		        switch (event.detail) {
		        case SWT.TRAVERSE_ESCAPE:
		        	UltraDuoPlusDialog.this.dialogShell.close();
		          event.detail = SWT.TRAVERSE_NONE;
		          event.doit = false;
		          break;
		        }
		      }
		    });
				this.dialogShell.addHelpListener(new HelpListener() {
					public void helpRequested(HelpEvent evt) {
						log.log(java.util.logging.Level.FINER, "dialogShell.helpRequested, event=" + evt); //$NON-NLS-1$
						UltraDuoPlusDialog.this.application.openHelpDialog(UltraDuoPlusDialog.DEVICE_JAR_NAME, "HelpInfo.html"); //$NON-NLS-1$ 
					}
				});
				this.dialogShell.addDisposeListener(new DisposeListener() {
					public void widgetDisposed(DisposeEvent evt) {
						log.log(java.util.logging.Level.FINEST, "dialogShell.widgetDisposed, event=" + evt); //$NON-NLS-1$
						if (UltraDuoPlusDialog.this.serialPort != null && UltraDuoPlusDialog.this.serialPort.isConnected()) {
							try {
								UltraDuoPlusDialog.this.synchronizerRead.join();
								try {
									//set the date to sync with PC time
									String[] date = StringHelper.getDateAndTime("yy:MM:dd:hh:mm").split(GDE.STRING_COLON); //$NON-NLS-1$
									switch (UltraDuoPlusDialog.this.device.getDeviceTypeIdentifier()) {
									case UltraDuoPlus45:
										UltraDuoPlusDialog.this.channelValues1[13] = Integer.parseInt(date[2]);
										UltraDuoPlusDialog.this.channelValues1[14] = Integer.parseInt(date[1]);
										UltraDuoPlusDialog.this.channelValues1[15] = Integer.parseInt(date[0]);
										UltraDuoPlusDialog.this.channelValues1[16] = Integer.parseInt(date[3]);
										UltraDuoPlusDialog.this.channelValues1[17] = Integer.parseInt(date[4]);
										break;
									case UltraDuoPlus50:
										UltraDuoPlusDialog.this.channelValues1[10] = Integer.parseInt(date[2]);
										UltraDuoPlusDialog.this.channelValues1[11] = Integer.parseInt(date[1]);
										UltraDuoPlusDialog.this.channelValues1[12] = Integer.parseInt(date[0]);
										UltraDuoPlusDialog.this.channelValues1[13] = Integer.parseInt(date[3]);
										UltraDuoPlusDialog.this.channelValues1[14] = Integer.parseInt(date[4]);
										break;
									case UltraDuoPlus60:
										UltraDuoPlusDialog.this.channelValues1[10] = Integer.parseInt(date[2]);
										UltraDuoPlusDialog.this.channelValues1[11] = Integer.parseInt(date[1]);
										UltraDuoPlusDialog.this.channelValues1[12] = Integer.parseInt(date[0]);
										UltraDuoPlusDialog.this.channelValues1[13] = Integer.parseInt(date[3]);
										UltraDuoPlusDialog.this.channelValues1[14] = Integer.parseInt(date[4]);
										break;
									default:
										break;
									}
									ChannelData1 value = new ChannelData1();
									value.setValue(StringHelper.integer2Hex4ByteString(UltraDuoPlusDialog.this.channelValues1));
									UltraDuoPlusDialog.this.ultraDuoPlusSetup.setChannelData1(value);
									UltraDuoPlusDialog.this.synchronizerWrite = new UltraDuoPlusSychronizer(UltraDuoPlusDialog.this, UltraDuoPlusDialog.this.serialPort, UltraDuoPlusDialog.this.ultraDuoPlusSetup,
											UltraDuoPlusSychronizer.SYNC_TYPE.WRITE);
									UltraDuoPlusDialog.this.synchronizerWrite.start();
									UltraDuoPlusDialog.this.synchronizerWrite.join();
								}
								catch (Exception e) {
									e.printStackTrace();
								}
								saveConfigUDP(UltraDuoPlusDialog.this.settings.getApplHomePath() + UltraDuoPlusDialog.UDP_CONFIGURATION_SUFFIX
										+ UltraDuoPlusDialog.this.deviceIdentifierName.replace(GDE.STRING_BLANK, GDE.STRING_UNDER_BAR) + GDE.FILE_ENDING_DOT_XML);

								UltraDuoPlusDialog.this.serialPort.write(UltramatSerialPort.RESET);
							}
							catch (Throwable e) {
								e.printStackTrace();
							}
							finally {
								UltraDuoPlusDialog.this.serialPort.close();
								UltraDuoPlusDialog.this.application.resetShellIcon();
							}
						}
					}
				});
				{
					this.boundsComposite = new Composite(this.dialogShell, SWT.NONE);
					FormData boundsCompositeLData = new FormData();
					boundsCompositeLData.left = new FormAttachment(0, 1000, 0);
					boundsCompositeLData.right = new FormAttachment(1000, 1000, 0);
					boundsCompositeLData.top = new FormAttachment(0, 1000, 0);
					boundsCompositeLData.bottom = new FormAttachment(1000, 1000, 0);
					this.boundsComposite.setLayoutData(boundsCompositeLData);
					this.boundsComposite.setLayout(new FormLayout());
					{
						this.userLabel = new CLabel(this.boundsComposite, SWT.RIGHT);
						this.userLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this.userLabel.setText(Messages.getString(MessageIds.GDE_MSGT2289));
						FormData userLabelLData = new FormData();
						userLabelLData.left = new FormAttachment(0, 1000, 12);
						userLabelLData.top = new FormAttachment(0, 1000, 7);
						userLabelLData.width = 280;
						userLabelLData.height = 20;
						this.userLabel.setLayoutData(userLabelLData);
					}
					{
						this.userNameText = new Text(this.boundsComposite, SWT.SINGLE | SWT.BORDER);
						this.userNameText.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this.userNameText.setText(this.deviceIdentifierName);
						this.userNameText.setTextLimit(16);
						FormData userNameTextLData = new FormData();
						userNameTextLData.width = 120;
						userNameTextLData.height = 16;
						userNameTextLData.left = new FormAttachment(0, 1000, 305);
						userNameTextLData.top = new FormAttachment(0, 1000, 7);
						this.userNameText.setLayoutData(userNameTextLData);
						this.userNameText.addVerifyListener(new VerifyListener() {
							public void verifyText(VerifyEvent evt) {
								log.log(java.util.logging.Level.FINEST, "evt.doit = " + (evt.text.length() <= 16)); //$NON-NLS-1$
								evt.doit = evt.text.length() <= 16;
							}
						});
						this.userNameText.addKeyListener(new KeyAdapter() {
							@Override
							public void keyReleased(KeyEvent evt) {
								log.log(java.util.logging.Level.FINEST, "text.keyReleased, event=" + evt); //$NON-NLS-1$
								File oldConfigDataFile = new File(UltraDuoPlusDialog.this.settings.getApplHomePath() + UltraDuoPlusDialog.UDP_CONFIGURATION_SUFFIX
										+ UltraDuoPlusDialog.this.deviceIdentifierName.replace(GDE.STRING_BLANK, GDE.STRING_UNDER_BAR) + GDE.FILE_ENDING_DOT_XML);
								if (oldConfigDataFile.exists()) if (!oldConfigDataFile.delete()) log.log(java.util.logging.Level.WARNING, "could not delete " + oldConfigDataFile.getName()); //$NON-NLS-1$
								UltraDuoPlusDialog.this.deviceIdentifierName = (UltraDuoPlusDialog.this.userNameText.getText().trim() + UltraDuoPlusDialog.STRING_16_BLANK).substring(0, 16);
								UltraDuoPlusDialog.this.ultraDuoPlusSetup.setIdentifierName(UltraDuoPlusDialog.this.deviceIdentifierName);
								UltraDuoPlusDialog.this.ultraDuoPlusSetup.setChanged(true);
								int position = UltraDuoPlusDialog.this.userNameText.getCaretPosition();
								UltraDuoPlusDialog.this.userNameText.setText(UltraDuoPlusDialog.this.deviceIdentifierName.trim());
								UltraDuoPlusDialog.this.userNameText.setSelection(position);
							}

							@Override
							public void keyPressed(KeyEvent evt) {
								log.log(java.util.logging.Level.FINEST, "text.keyPressed, event=" + evt); //$NON-NLS-1$
							}
						});
					}
					{
						this.mainTabFolder = new CTabFolder(this.boundsComposite, SWT.BORDER);
						this.mainTabFolder.setSimple(false);
						FormData TabFolderLData = new FormData();
						TabFolderLData.left = new FormAttachment(0, 1000, 0);
						TabFolderLData.right = new FormAttachment(1000, 1000, 0);
						TabFolderLData.top = new FormAttachment(0, 1000, 35);
						TabFolderLData.bottom = new FormAttachment(1000, 1000, -45);
						this.mainTabFolder.setLayoutData(TabFolderLData);
						{
							this.setupTabItem = new CTabItem(this.mainTabFolder, SWT.BORDER);
							this.setupTabItem.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
							this.setupTabItem.setText(Messages.getString(MessageIds.GDE_MSGT2290));
							{
								this.deviceComposite = new Composite(this.mainTabFolder, SWT.BORDER);
								GridLayout deviceCompositeLayout = new GridLayout();
								deviceCompositeLayout.makeColumnsEqualWidth = true;
								this.deviceComposite.setLayout(deviceCompositeLayout);
								this.setupTabItem.setControl(this.deviceComposite);
								{
									this.baseDeviceSetupGroup = new Group(this.deviceComposite, SWT.NONE);
									GridData group1LData = new GridData();
									group1LData.horizontalAlignment = GridData.FILL;
									group1LData.verticalAlignment = GridData.BEGINNING;
									group1LData.widthHint = 580;
									group1LData.heightHint = this.device.getDeviceTypeIdentifier() != GraupnerDeviceType.UltraDuoPlus45 ? 182 : 392;
									this.baseDeviceSetupGroup.setLayoutData(group1LData);
									FillLayout baseDeviceSetupCompositeLayout = new FillLayout(SWT.VERTICAL);
									this.baseDeviceSetupGroup.setLayout(baseDeviceSetupCompositeLayout);
									this.baseDeviceSetupGroup.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
									this.baseDeviceSetupGroup.setText(Messages.getString(MessageIds.GDE_MSGT2291));
									this.baseDeviceSetupGroup.setForeground(SWTResourceManager.getColor(SWT.COLOR_BLUE));

									//new ParameterHeaderControl(this.baseDeviceSetupComposite, Messages.getString(MessageIds.GDE_MSGT2247), 175, Messages.getString(MessageIds.GDE_MSGT2248), 50, Messages.getString(MessageIds.GDE_MSGT2249), 175, 20);
									if (this.device.getDeviceTypeIdentifier() != GraupnerDeviceType.UltraDuoPlus45) {
										this.channelParameters[4] = new ParameterConfigControl(this.baseDeviceSetupGroup, this.channelValues1, 4, Messages.getString(MessageIds.GDE_MSGT2293), 175,
												"°C - °F", 175, UltraDuoPlusDialog.temperatureDegreeType, 50, 150); //$NON-NLS-1$ 
										this.channelParameters[5] = new ParameterConfigControl(this.baseDeviceSetupGroup, this.channelValues1, 5, Messages.getString(MessageIds.GDE_MSGT2294), 175,
												Messages.getString(MessageIds.GDE_MSGT2240) + GDE.STRING_MESSAGE_CONCAT + Messages.getString(MessageIds.GDE_MSGT2241), 175, UltraDuoPlusDialog.offOnType, 50, 150);
										this.channelParameters[6] = new ParameterConfigControl(this.baseDeviceSetupGroup, this.channelValues1, 6, Messages.getString(MessageIds.GDE_MSGT2295), 175,
												"En - De - Fr - It", 175, UltraDuoPlusDialog.languageTypes, 50, 150); //$NON-NLS-1$ 
										//channelParameters[7] = new ParameterConfigControl(baseDeviceSetupComposite, channelValues1, 7, "PC setup", 175, "DISABLE | ENABLE", 175, diableEnableType, 50, 150);
										this.channelParameters[8] = new ParameterConfigControl(this.baseDeviceSetupGroup, this.channelValues1, 8, GDE.STRING_EMPTY, Messages.getString(MessageIds.GDE_MSGT2296), 175,
												"120 ~ 150 (12.0 ~ 15.0V)", 175, true, 50, 150, 120, 150, -100); //$NON-NLS-1$ 
										this.channelParameters[9] = new ParameterConfigControl(this.baseDeviceSetupGroup, this.channelValues1, 9, GDE.STRING_EMPTY, Messages.getString(MessageIds.GDE_MSGT2297), 175,
												"50 ~ 400 (5 ~ 40A)", 175, true, 50, 150, 50, 400, -50); //$NON-NLS-1$ 
										this.channelParameters[15] = new ParameterConfigControl(this.baseDeviceSetupGroup, this.channelValues1, 15, Messages.getString(MessageIds.GDE_MSGT2303), 175,
												"12H - 24H", 175, UltraDuoPlusDialog.hourFormatType, 50, 150); //$NON-NLS-1$ 
									}
									else { //UltraDuoPlus45
										this.channelParameters[0] = new ParameterConfigControl(this.baseDeviceSetupGroup, this.channelValues1, 0, Messages.getString(MessageIds.GDE_MSGT2293), 175,
												"°C - °F", 175, UltraDuoPlusDialog.temperatureDegreeType, 50, 150); //$NON-NLS-1$ 
										this.channelParameters[1] = new ParameterConfigControl(this.baseDeviceSetupGroup, this.channelValues1, 1, Messages.getString(MessageIds.GDE_MSGT2294), 175,
												Messages.getString(MessageIds.GDE_MSGT2240) + GDE.STRING_MESSAGE_CONCAT + Messages.getString(MessageIds.GDE_MSGT2241), 175, UltraDuoPlusDialog.offOnType, 50, 150);
										this.channelParameters[2] = new ParameterConfigControl(this.baseDeviceSetupGroup, this.channelValues1, 2, Messages.getString(MessageIds.GDE_MSGT2306), 175,
												Messages.getString(MessageIds.GDE_MSGT2313), 175, UltraDuoPlusDialog.soundTime, 50, 150);
										this.channelParameters[3] = new ParameterConfigControl(this.baseDeviceSetupGroup, this.channelValues1, 3, GDE.STRING_EMPTY, Messages.getString(MessageIds.GDE_MSGT2307), 175,
												"1 ~ 10", 175, false, 50, 150, 1, 10); //$NON-NLS-1$ 
										this.channelParameters[4] = new ParameterConfigControl(this.baseDeviceSetupGroup, this.channelValues1, 4, GDE.STRING_EMPTY, Messages.getString(MessageIds.GDE_MSGT2305), 175,
												"1 ~ 15", 175, false, 50, 150, 1, 15); //$NON-NLS-1$ 
										this.channelParameters[5] = new ParameterConfigControl(this.baseDeviceSetupGroup, this.channelValues1, 5, Messages.getString(MessageIds.GDE_MSGT2295), 175,
												"En - De - Fr - It", 175, UltraDuoPlusDialog.languageTypes, 50, 150); //$NON-NLS-1$ 
										this.channelParameters[6] = new ParameterConfigControl(this.baseDeviceSetupGroup, this.channelValues1, 6, Messages.getString(MessageIds.GDE_MSGT2308), 175,
												Messages.getString(MessageIds.GDE_MSGT2244) + GDE.STRING_MESSAGE_CONCAT + Messages.getString(MessageIds.GDE_MSGT2245), 175, UltraDuoPlusDialog.powerOnDisplayType, 50, 150);
										//channelParameters[7] = new ParameterConfigControl(baseDeviceSetupComposite, channelValues1, 7, "PC setup", 175, "DISABLE | ENABLE", 175, diableEnableType, 50, 150);
										this.channelParameters[8] = new ParameterConfigControl(this.baseDeviceSetupGroup, this.channelValues1, 8, GDE.STRING_EMPTY,
												Messages.getString(MessageIds.GDE_MSGT2296) + "(1)", 175, "120 ~ 150 (12.0 ~ 15.0V)", 175, true, 50, 150, 120, 150, -100); //$NON-NLS-1$ //$NON-NLS-2$ 
										this.channelParameters[9] = new ParameterConfigControl(this.baseDeviceSetupGroup, this.channelValues1, 9, GDE.STRING_EMPTY,
												Messages.getString(MessageIds.GDE_MSGT2297) + "(1)", 175, "50 ~ 400 (5 ~ 40A)", 175, true, 50, 150, 50, 400, -50); //$NON-NLS-1$ //$NON-NLS-2$ 
										this.channelParameters[10] = new ParameterConfigControl(this.baseDeviceSetupGroup, this.channelValues1, 10, GDE.STRING_EMPTY,
												Messages.getString(MessageIds.GDE_MSGT2296) + "(2)", 175, "120 ~ 150 (12.0 ~ 15.0V)", 175, true, 50, 150, 120, 150, -100); //$NON-NLS-1$ //$NON-NLS-2$ 
										this.channelParameters[11] = new ParameterConfigControl(this.baseDeviceSetupGroup, this.channelValues1, 11, GDE.STRING_EMPTY,
												Messages.getString(MessageIds.GDE_MSGT2297) + "(2)", 175, "50 ~ 400 (5 ~ 40A)", 175, true, 50, 150, 50, 400, -50); //$NON-NLS-1$ //$NON-NLS-2$ 
										this.channelParameters[12] = new ParameterConfigControl(this.baseDeviceSetupGroup, this.channelValues1, 12, GDE.STRING_EMPTY, Messages.getString(MessageIds.GDE_MSGT2315), 175,
												"1 - 99%", 175, false, 50, 150, 1, 99, -1); //$NON-NLS-1$ 
										this.channelParameters[18] = new ParameterConfigControl(this.baseDeviceSetupGroup, this.channelValues1, 18, Messages.getString(MessageIds.GDE_MSGT2303), 175,
												"12H - 24H", 175, UltraDuoPlusDialog.hourFormatType, 50, 150); //$NON-NLS-1$ 
									}
									//time setup will synced with PC
									//this.channelParameters[10] = new ParameterConfigControl(this.baseDeviceSetupComposite, this.channelValues1, 10, Messages.getString(MessageIds.GDE_MSGT2298), 175,	"1 ~ 31", 175, false, 50, 150, 1, 31); //$NON-NLS-1$ 
									//this.channelParameters[11] = new ParameterConfigControl(this.baseDeviceSetupComposite, this.channelValues1, 11, Messages.getString(MessageIds.GDE_MSGT2299), 175,	"1 ~ 12", 175, false, 50, 150, 1, 12); //$NON-NLS-1$ 
									//this.channelParameters[12] = new ParameterConfigControl(this.baseDeviceSetupComposite, this.channelValues1, 12, "%02d", Messages.getString(MessageIds.GDE_MSGT2300), 175,	"0 ~ 99", 175, false, 50, 150, 0, 99); //$NON-NLS-1$ 
									//this.channelParameters[13] = new ParameterConfigControl(this.baseDeviceSetupComposite, this.channelValues1, 13, "%02d", Messages.getString(MessageIds.GDE_MSGT2302), 175,	"0 ~ 12", 175, false, 50, 150, 0, 12); //$NON-NLS-1$ 
									//this.channelParameters[14] = new ParameterConfigControl(this.baseDeviceSetupComposite, this.channelValues1, 14, "%02d", Messages.getString(MessageIds.GDE_MSGT2301), 175,	"0 ~ 59", 175, false, 50, 150, 0, 59); //$NON-NLS-1$ 
								}
								if (this.device.getDeviceTypeIdentifier() != GraupnerDeviceType.UltraDuoPlus45) { //no configurable outlet channel 2
									{
										this.baseDeviceSetupGroup1 = new Group(this.deviceComposite, SWT.NONE);
										GridData group2LData = new GridData();
										group2LData.verticalAlignment = GridData.BEGINNING;
										group2LData.horizontalAlignment = GridData.CENTER;
										group2LData.widthHint = 600;
										group2LData.heightHint = 130;
										this.baseDeviceSetupGroup1.setLayoutData(group2LData);
										FillLayout composite1Layout = new FillLayout(SWT.VERTICAL);
										this.baseDeviceSetupGroup1.setLayout(composite1Layout);
										this.baseDeviceSetupGroup1.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
										this.baseDeviceSetupGroup1.setText(Messages.getString(MessageIds.GDE_MSGT2304));
										this.baseDeviceSetupGroup1.setForeground(SWTResourceManager.getColor(SWT.COLOR_BLUE));

										//new ParameterHeaderControl(this.baseDeviceSetupComposite1, Messages.getString(MessageIds.GDE_MSGT2247), 175, Messages.getString(MessageIds.GDE_MSGT2248), 50,	Messages.getString(MessageIds.GDE_MSGT2249), 175, 20);
										this.channelParameters[0] = new ParameterConfigControl(this.baseDeviceSetupGroup1, this.channelValues1, 0, Messages.getString(MessageIds.GDE_MSGT2306), 175,
												Messages.getString(MessageIds.GDE_MSGT2313), 175, UltraDuoPlusDialog.soundTime, 50, 150);
										this.channelParameters[1] = new ParameterConfigControl(this.baseDeviceSetupGroup1, this.channelValues1, 1, GDE.STRING_EMPTY, Messages.getString(MessageIds.GDE_MSGT2307), 175,
												"1 ~ 10", 175, false, 50, 150, 1, 10); //$NON-NLS-1$ 
										this.channelParameters[2] = new ParameterConfigControl(this.baseDeviceSetupGroup1, this.channelValues1, 2, GDE.STRING_EMPTY, Messages.getString(MessageIds.GDE_MSGT2305), 175,
												"1 ~ 15", 175, false, 50, 150, 1, 15); //$NON-NLS-1$ 
										this.channelParameters[3] = new ParameterConfigControl(this.baseDeviceSetupGroup1, this.channelValues1, 3, Messages.getString(MessageIds.GDE_MSGT2308), 175,
												Messages.getString(MessageIds.GDE_MSGT2244) + GDE.STRING_MESSAGE_CONCAT + Messages.getString(MessageIds.GDE_MSGT2245), 175, UltraDuoPlusDialog.powerOnDisplayType, 50, 150);
									}
									{
										this.baseDeviceSetupGroup2 = new Group(this.deviceComposite, SWT.NONE);
										GridData group3LData = new GridData();
										group3LData.verticalAlignment = GridData.BEGINNING;
										group3LData.horizontalAlignment = GridData.CENTER;
										group3LData.widthHint = 600;
										group3LData.heightHint = 130;
										this.baseDeviceSetupGroup2.setLayoutData(group3LData);
										FillLayout composite1Layout = new FillLayout(SWT.VERTICAL);
										this.baseDeviceSetupGroup2.setLayout(composite1Layout);
										this.baseDeviceSetupGroup2.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
										this.baseDeviceSetupGroup2.setText(Messages.getString(MessageIds.GDE_MSGT2309));
										this.baseDeviceSetupGroup2.setForeground(SWTResourceManager.getColor(SWT.COLOR_BLUE));

										//new ParameterHeaderControl(this.baseDeviceSetupComposite2, Messages.getString(MessageIds.GDE_MSGT2247), 175, Messages.getString(MessageIds.GDE_MSGT2248), 50,	Messages.getString(MessageIds.GDE_MSGT2249), 175, 20);
										this.channelParameters[UltramatSerialPort.SIZE_CHANNEL_1_SETUP + 0] = new ParameterConfigControl(this.baseDeviceSetupGroup2, this.channelValues2, 0,
												Messages.getString(MessageIds.GDE_MSGT2255), 175, Messages.getString(MessageIds.GDE_MSGT2313), 175, UltraDuoPlusDialog.soundTime, 50, 150);
										this.channelParameters[UltramatSerialPort.SIZE_CHANNEL_1_SETUP + 1] = new ParameterConfigControl(this.baseDeviceSetupGroup2, this.channelValues2, 1, GDE.STRING_EMPTY,
												Messages.getString(MessageIds.GDE_MSGT2254), 175, "1 ~ 10", 175, false, 50, 150, 1, 10); //$NON-NLS-1$ 
										this.channelParameters[UltramatSerialPort.SIZE_CHANNEL_1_SETUP + 2] = new ParameterConfigControl(this.baseDeviceSetupGroup2, this.channelValues2, 2, GDE.STRING_EMPTY,
												Messages.getString(MessageIds.GDE_MSGT2305), 175, "1 ~ 15", 175, false, 50, 150, 1, 15); //$NON-NLS-1$ 
										this.channelParameters[UltramatSerialPort.SIZE_CHANNEL_1_SETUP + 3] = new ParameterConfigControl(this.baseDeviceSetupGroup2, this.channelValues2, 3,
												Messages.getString(MessageIds.GDE_MSGT2308), 175, Messages.getString(MessageIds.GDE_MSGT2244) + GDE.STRING_MESSAGE_CONCAT + Messages.getString(MessageIds.GDE_MSGT2245), 175,
												UltraDuoPlusDialog.powerOnDisplayType, 50, 150);
									}
								}
							}
						}
						{
							this.memorySetupTabItem = new CTabItem(this.mainTabFolder, SWT.NONE);
							this.memorySetupTabItem.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
							this.memorySetupTabItem.setText(Messages.getString(MessageIds.GDE_MSGT2250));
							{
								this.memoryBoundsComposite = new Composite(this.mainTabFolder, SWT.NONE);
								this.memorySetupTabItem.setControl(this.memoryBoundsComposite);
								this.memoryBoundsComposite.setLayout(new FormLayout());
								{
									this.memorySelectComposite = new Composite(this.memoryBoundsComposite, SWT.NONE);
									FormData memorySelectLData = new FormData();
									memorySelectLData.height = 150;
									memorySelectLData.left = new FormAttachment(0, 1000, 0);
									memorySelectLData.right = new FormAttachment(1000, 1000, 0);
									memorySelectLData.top = new FormAttachment(0, 1000, 0);
									this.memorySelectComposite.setLayoutData(memorySelectLData);
									RowLayout composite2Layout = new RowLayout(SWT.HORIZONTAL);
									this.memorySelectComposite.setLayout(composite2Layout);
									this.memorySelectComposite.setBackground(DataExplorer.COLOR_CANVAS_YELLOW);
									{
										this.memorySelectLabel = new CLabel(this.memorySelectComposite, SWT.RIGHT);
										RowData memorySelectLabelLData = new RowData();
										memorySelectLabelLData.width = 120;
										memorySelectLabelLData.height = 20;
										this.memorySelectLabel.setLayoutData(memorySelectLabelLData);
										this.memorySelectLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
										this.memorySelectLabel.setText(Messages.getString(MessageIds.GDE_MSGT2251));
										this.memorySelectLabel.setBackground(DataExplorer.COLOR_CANVAS_YELLOW);
									}
									{
										updateBatterySetup(1);
										this.memoryCombo = new CCombo(this.memorySelectComposite, SWT.BORDER);
										this.memoryCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
										this.memoryCombo.setItems(this.memoryNames);
										this.memoryCombo.setVisibleItemCount(20);
										this.memoryCombo.setTextLimit(5 + 16);
										RowData memoryComboLData = new RowData();
										memoryComboLData.width = 165;
										memoryComboLData.height = GDE.IS_WINDOWS ? 16 : 18;
										this.memoryCombo.setLayoutData(memoryComboLData);
										this.memoryCombo.setToolTipText(Messages.getString(MessageIds.GDE_MSGT2252));
										this.memoryCombo.select(0);
										this.memoryCombo.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
										this.memoryCombo.setEditable(true);
										this.memoryCombo.addSelectionListener(new SelectionAdapter() {
											@Override
											public void widgetSelected(SelectionEvent evt) {
												log.log(java.util.logging.Level.FINEST, "memoryCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
												try {
													int actualSelectionIndex = UltraDuoPlusDialog.this.memoryCombo.getSelectionIndex();
													if (UltraDuoPlusDialog.this.lastMemorySelectionIndex != actualSelectionIndex) {
														if (UltraDuoPlusDialog.this.ultraDuoPlusSetup != null && UltraDuoPlusDialog.this.lastMemorySelectionIndex >= 0
																&& UltraDuoPlusDialog.this.lastMemorySelectionIndex < UltraDuoPlusDialog.numberMemories) {
															//write memory if setup data has been changed changed (update memory name executed while keyListener)
															if (UltraDuoPlusDialog.this.ultraDuoPlusSetup.getMemory().get(UltraDuoPlusDialog.this.lastMemorySelectionIndex).getSetupData().isChanged()) {
																UltraDuoPlusDialog.this.ultraDuoPlusSetup.getMemory().get(UltraDuoPlusDialog.this.lastMemorySelectionIndex).getSetupData()
																		.setValue(StringHelper.integer2Hex4ByteString(UltraDuoPlusDialog.this.memoryValues));
																UltraDuoPlusDialog.this.serialPort.writeConfigData(UltramatSerialPort.WRITE_MEMORY_SETUP,
																		UltraDuoPlusDialog.this.ultraDuoPlusSetup.getMemory().get(UltraDuoPlusDialog.this.lastMemorySelectionIndex).getSetupData().getValue().getBytes(),
																		UltraDuoPlusDialog.this.lastMemorySelectionIndex + 1);
																UltraDuoPlusDialog.this.ultraDuoPlusSetup.getMemory().get(UltraDuoPlusDialog.this.lastMemorySelectionIndex).getSetupData().changed = null;
															}
															//check for copy selected
															if (UltraDuoPlusDialog.this.copyButton.getSelection()) {
																UltraDuoPlusDialog.this.copyButton.setSelection(false);

																if (SWT.YES == UltraDuoPlusDialog.this.application.openYesNoMessageDialog(
																		UltraDuoPlusDialog.this.dialogShell,
																		Messages.getString(MessageIds.GDE_MSGI2205, new Object[] { UltraDuoPlusDialog.this.lastMemorySelectionIndex,
																				UltraDuoPlusDialog.this.ultraDuoPlusSetup.getMemory().get(UltraDuoPlusDialog.this.lastMemorySelectionIndex).getName(), (actualSelectionIndex + 1),
																				UltraDuoPlusDialog.this.ultraDuoPlusSetup.getMemory().get(actualSelectionIndex).getName() }))) {
																	//copy memory name and setup data of lastMemorySelectionIndex to memoryCombo.getSelectionIndex()																
																	log
																			.log(
																					java.util.logging.Level.FINE,
																					"copy memory: (" + UltraDuoPlusDialog.this.lastMemorySelectionIndex + GDE.STRING_RIGHT_PARENTHESIS + UltraDuoPlusDialog.this.ultraDuoPlusSetup.getMemory().get(UltraDuoPlusDialog.this.lastMemorySelectionIndex).getName()//$NON-NLS-1$
																							+ " to (" + (actualSelectionIndex + 1) + GDE.STRING_RIGHT_PARENTHESIS + UltraDuoPlusDialog.this.ultraDuoPlusSetup.getMemory().get(actualSelectionIndex).getName()); //$NON-NLS-1$
																	if (log.isLoggable(java.util.logging.Level.FINE)) {
																		StringBuffer sb = new StringBuffer();
																		for (int i = 0; i < UltramatSerialPort.SIZE_MEMORY_SETUP; i++) {
																			sb.append(UltraDuoPlusDialog.this.memoryValues[i]).append(GDE.STRING_LEFT_BRACKET).append(i).append(GDE.STRING_RIGHT_BRACKET_COMMA);
																		}
																		log.log(java.util.logging.Level.FINE, sb.toString());
																	}
																	UltraDuoPlusDialog.this.serialPort.writeConfigData(UltramatSerialPort.WRITE_MEMORY_NAME,
																			UltraDuoPlusDialog.this.ultraDuoPlusSetup.getMemory().get(UltraDuoPlusDialog.this.lastMemorySelectionIndex).getName().getBytes(), actualSelectionIndex + 1);
																	UltraDuoPlusDialog.this.ultraDuoPlusSetup.getMemory().get(UltraDuoPlusDialog.this.lastMemorySelectionIndex).changed = null;
																	UltraDuoPlusDialog.this.ultraDuoPlusSetup.getMemory().get(actualSelectionIndex)
																			.setName(UltraDuoPlusDialog.this.ultraDuoPlusSetup.getMemory().get(UltraDuoPlusDialog.this.lastMemorySelectionIndex).getName());

																	UltraDuoPlusDialog.this.serialPort.writeConfigData(UltramatSerialPort.WRITE_MEMORY_SETUP,
																			UltraDuoPlusDialog.this.ultraDuoPlusSetup.getMemory().get(UltraDuoPlusDialog.this.lastMemorySelectionIndex).getSetupData().getValue().getBytes(),
																			actualSelectionIndex + 1);
																	UltraDuoPlusDialog.this.ultraDuoPlusSetup.getMemory().get(UltraDuoPlusDialog.this.lastMemorySelectionIndex).getSetupData().changed = null;
																	UltraDuoPlusDialog.this.ultraDuoPlusSetup.getMemory().get(actualSelectionIndex).getSetupData()
																			.setValue(UltraDuoPlusDialog.this.ultraDuoPlusSetup.getMemory().get(UltraDuoPlusDialog.this.lastMemorySelectionIndex).getSetupData().getValue());

																	String newMemoryName = String.format(UltraDuoPlusDialog.STRING_FORMAT_02d_s, actualSelectionIndex + 1,
																			UltraDuoPlusDialog.this.ultraDuoPlusSetup.getMemory().get(actualSelectionIndex).getName());
																	UltraDuoPlusDialog.this.memoryCombo.setText(newMemoryName);
																	UltraDuoPlusDialog.this.memoryNames[actualSelectionIndex] = (newMemoryName + UltraDuoPlusDialog.STRING_16_BLANK).substring(5, 16 + 5);
																	UltraDuoPlusDialog.this.memoryCombo.setItem(actualSelectionIndex, newMemoryName);
																}
															}
														}
														updateBatterySetup(actualSelectionIndex);
														UltraDuoPlusDialog.this.lastMemorySelectionIndex = actualSelectionIndex;
													}
												}
												catch (Throwable e) {
													log.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
												}
											}
										});
										this.memoryCombo.addKeyListener(new KeyAdapter() {
											@Override
											public void keyReleased(KeyEvent evt) {
												log.log(java.util.logging.Level.FINEST, "memoryCombo.keyReleased, event=" + evt); //$NON-NLS-1$
											}

											@Override
											public void keyPressed(KeyEvent evt) {
												log.log(java.util.logging.Level.FINEST, "memoryCombo.keyPressed, event=" + evt); //$NON-NLS-1$
												if (evt.character == SWT.CR) {
													try {
														String newMemoryName = String.format(UltraDuoPlusDialog.STRING_FORMAT_02d_s, UltraDuoPlusDialog.this.lastMemorySelectionIndex + 1,
																(UltraDuoPlusDialog.this.memoryCombo.getText() + UltraDuoPlusDialog.STRING_16_BLANK).substring(5, 16 + 5));
														UltraDuoPlusDialog.this.memoryNames[UltraDuoPlusDialog.this.lastMemorySelectionIndex] = newMemoryName;
														UltraDuoPlusDialog.this.memoryCombo.setText(newMemoryName);
														UltraDuoPlusDialog.this.memoryCombo.setItem(UltraDuoPlusDialog.this.lastMemorySelectionIndex, newMemoryName);

														UltraDuoPlusDialog.this.ultraDuoPlusSetup.getMemory().get(UltraDuoPlusDialog.this.lastMemorySelectionIndex)
																.setName(UltraDuoPlusDialog.this.memoryNames[UltraDuoPlusDialog.this.lastMemorySelectionIndex].substring(5));
														UltraDuoPlusDialog.this.serialPort.writeConfigData(UltramatSerialPort.WRITE_MEMORY_NAME,
																UltraDuoPlusDialog.this.memoryNames[UltraDuoPlusDialog.this.lastMemorySelectionIndex].substring(5).getBytes(), UltraDuoPlusDialog.this.lastMemorySelectionIndex + 1);
													}
													catch (Exception e) {
														UltraDuoPlusDialog.this.application.openMessageDialog(UltraDuoPlusDialog.this.dialogShell,
																Messages.getString(gde.messages.MessageIds.GDE_MSGE0007, new String[] { e.getMessage() }));
													}
												}
											}
										});
									}
									{
										CLabel filler = new CLabel(this.memorySelectComposite, SWT.RIGHT);
										filler.setLayoutData(new RowData(140, 20));
										filler.setText("<---------   "); //$NON-NLS-1$
										filler.setBackground(DataExplorer.COLOR_CANVAS_YELLOW);
									}
									{
										this.copyButton = new Button(this.memorySelectComposite, SWT.CHECK | SWT.LEFT);
										RowData cellTypeSelectLabelLData = new RowData();
										cellTypeSelectLabelLData.width = 200;
										cellTypeSelectLabelLData.height = 20;
										this.copyButton.setLayoutData(cellTypeSelectLabelLData);
										this.copyButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
										this.copyButton.setText(Messages.getString(MessageIds.GDE_MSGT2288));
										this.copyButton.setToolTipText(Messages.getString(MessageIds.GDE_MSGT2256));
										this.copyButton.setBackground(DataExplorer.COLOR_CANVAS_YELLOW);
									}
									//cell type
									this.memoryParameters[0] = new ParameterConfigControl(this.memorySelectComposite, this.memoryValues, 0, Messages.getString(MessageIds.GDE_MSGT2257), 175,
											Messages.getString(MessageIds.GDE_MSGT2246), 220, UltraDuoPlusDialog.cellTypeNames, 50, 150);
									//number cells
									this.memoryParameters[1] = new ParameterConfigControl(this.memorySelectComposite, this.memoryValues, 1, GDE.STRING_EMPTY, Messages.getString(MessageIds.GDE_MSGT2258), 175,
											"1 ~ 18", 220, false, 50, 150, 1, 18); //$NON-NLS-1$ 
									//battery capacity
									this.memoryParameters[2] = new ParameterConfigControl(this.memorySelectComposite, this.memoryValues, 2, GDE.STRING_EMPTY, Messages.getString(MessageIds.GDE_MSGT2259), 175,
											"100 ~ 50000 mAh", 220, true, 50, 150, 100, 50000, -100); //$NON-NLS-1$ 
									//year, month, day
									this.memoryParameters[3] = new ParameterConfigControl(this.memorySelectComposite, this.memoryValues, 3, UltraDuoPlusDialog.STRING_FORMAT_02D,
											Messages.getString(MessageIds.GDE_MSGT2260), 100, GDE.STRING_EMPTY, 5, false, 30, 70, 0, 99);
									this.memoryParameters[4] = new ParameterConfigControl(this.memorySelectComposite, this.memoryValues, 4, UltraDuoPlusDialog.STRING_FORMAT_02D,
											Messages.getString(MessageIds.GDE_MSGT2261), 60, GDE.STRING_EMPTY, 5, false, 20, 80, 1, 12);
									this.memoryParameters[5] = new ParameterConfigControl(this.memorySelectComposite, this.memoryValues, 5, UltraDuoPlusDialog.STRING_FORMAT_02D,
											Messages.getString(MessageIds.GDE_MSGT2262), 55, GDE.STRING_EMPTY, 5, false, 20, 80, 1, 31);
									//new ParameterHeaderControl(this.memorySelectComposite, Messages.getString(MessageIds.GDE_MSGT2247), 175, Messages.getString(MessageIds.GDE_MSGT2248), 50,	Messages.getString(MessageIds.GDE_MSGT2249), 180, 20);
								}
								{
									this.chargeTypeTabFolder = new CTabFolder(this.memoryBoundsComposite, SWT.BORDER);
									this.chargeTypeTabFolder.setSimple(false);
									FormData chargeTypeTabFolderLData = new FormData();
									chargeTypeTabFolderLData.left = new FormAttachment(0, 1000, 0);
									chargeTypeTabFolderLData.right = new FormAttachment(1000, 1000, 0);
									chargeTypeTabFolderLData.bottom = new FormAttachment(1000, 1000, 0);
									chargeTypeTabFolderLData.top = new FormAttachment(0, 1000, 150);
									this.chargeTypeTabFolder.setLayoutData(chargeTypeTabFolderLData);
									this.chargeTypeTabFolder.addSelectionListener(new SelectionAdapter() {
										@Override
										public void widgetSelected(SelectionEvent evt) {
											log.log(java.util.logging.Level.FINEST, "restoreButton.widgetSelected, event=" + evt); //$NON-NLS-1$
											UltraDuoPlusDialog.this.lastCellSelectionIndex = -1;
											updateBatteryMemoryParameter(UltraDuoPlusDialog.this.memoryValues[0]);
										}
									});
									{
										this.chargeTabItem = new CTabItem(this.chargeTypeTabFolder, SWT.NONE);
										this.chargeTabItem.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
										this.chargeTabItem.setText(Messages.getString(MessageIds.GDE_MSGT2298));
										{
											this.scrolledchargeComposite = new ScrolledComposite(this.chargeTypeTabFolder, SWT.BORDER | SWT.V_SCROLL);
											FillLayout scrolledMemoryCompositeLayout = new FillLayout();
											this.scrolledchargeComposite.setLayout(scrolledMemoryCompositeLayout);
											this.chargeTabItem.setControl(this.scrolledchargeComposite);
											{
												this.chargeGroup = new Group(this.scrolledchargeComposite, SWT.NONE);
												FillLayout memoryCompositeLayout = new FillLayout(SWT.VERTICAL);
												this.chargeGroup.setLayout(memoryCompositeLayout);
												this.chargeGroup.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
												this.chargeGroup.setText(Messages.getString(MessageIds.GDE_MSGT2299));
												this.chargeGroup.setForeground(SWTResourceManager.getColor(SWT.COLOR_BLUE));
												//charge parameter
												this.memoryParameters[6] = new ParameterConfigControl(this.chargeGroup, this.memoryValues, 6, GDE.STRING_EMPTY, Messages.getString(MessageIds.GDE_MSGT2263), 175,
														"100 ~ 20000 mA", 220, true, 50, 150, 100, 20000, -100); //$NON-NLS-1$ 
												this.memoryParameters[11] = new ParameterConfigControl(this.chargeGroup, this.memoryValues, 11, GDE.STRING_EMPTY, Messages.getString(MessageIds.GDE_MSGT2265), 175,
														Messages.getString(MessageIds.GDE_MSGT2310), 220, true, 50, 150, 10, 155);
												this.memoryParameters[10] = (this.device.getDeviceTypeIdentifier() != GraupnerDeviceType.UltraDuoPlus45 ||this.device.getDeviceTypeIdentifier() != GraupnerDeviceType.UltraDuoPlus40 ? this.channelValues1[4] == 0 : this.channelValues1[0] == 0)//°C
												? new ParameterConfigControl(this.chargeGroup, this.memoryValues, 10, GDE.STRING_EMPTY, Messages.getString(MessageIds.GDE_MSGT2264), 175,
														"10 ~ 80°C", 220, false, 50, 150, 10, 80) //$NON-NLS-1$ 
														: new ParameterConfigControl(this.chargeGroup, this.memoryValues, 10, GDE.STRING_EMPTY, Messages.getString(MessageIds.GDE_MSGT2264), 175,
																"50 ~ 176°F", 220, false, 50, 150, 50, 176); //$NON-NLS-1$ 
												this.memoryParameters[12] = new ParameterConfigControl(this.chargeGroup, this.memoryValues, 12, GDE.STRING_EMPTY, Messages.getString(MessageIds.GDE_MSGT2266), 175,
														"10 ~ 905min (905=off)", 220, false, 50, 150, 10, 905); //$NON-NLS-1$ 
												this.memoryParameters[14] = new ParameterConfigControl(this.chargeGroup, this.memoryValues, 14, GDE.STRING_EMPTY, Messages.getString(MessageIds.GDE_MSGT2267), 175,
														"1000 ~ 4300 mV", 220, true, 50, 150, 1000, 4300, -1000); //$NON-NLS-1$ 
												this.memoryParameters[9] = new ParameterConfigControl(this.chargeGroup, this.memoryValues, 9, GDE.STRING_EMPTY, Messages.getString(MessageIds.GDE_MSGT2279), 175,
														Messages.getString(MessageIds.GDE_MSGT2312), 220, false, 50, 150, 0, 500);
												this.memoryParameters[26] = new ParameterConfigControl(this.chargeGroup, this.memoryValues, 26, GDE.STRING_EMPTY, Messages.getString(MessageIds.GDE_MSGT2276), 175,
														"1000 ~ 4000 mV", 220, true, 50, 150, 1000, 4000, -1000); //$NON-NLS-1$ 
												this.memoryParameters[7] = new ParameterConfigControl(this.chargeGroup, this.memoryValues, 7, GDE.STRING_EMPTY, Messages.getString(MessageIds.GDE_MSGT2277), 175,
														"0 ~ 25mV", 220, false, 50, 150, 0, 25); //$NON-NLS-1$ 
												this.memoryParameters[8] = new ParameterConfigControl(this.chargeGroup, this.memoryValues, 8, GDE.STRING_EMPTY, Messages.getString(MessageIds.GDE_MSGT2278), 175,
														"1 ~ 20min", 220, false, 50, 150, 1, 20); //$NON-NLS-1$ 
												this.memoryParameters[15] = new ParameterConfigControl(this.chargeGroup, this.memoryValues, 15, GDE.STRING_EMPTY, Messages.getString(MessageIds.GDE_MSGT2281), 175,
														"1 ~ 30min", 220, false, 50, 150, 1, 30); //$NON-NLS-1$ 
												this.memoryParameters[13] = new ParameterConfigControl(this.chargeGroup, this.memoryValues, 13, GDE.STRING_EMPTY, Messages.getString(MessageIds.GDE_MSGT2280), 175,
														"1 ~ 5", 220, false, 50, 150, 1, 5); //$NON-NLS-1$ 
												this.memoryParameters[16] = new ParameterConfigControl(this.chargeGroup, this.memoryValues, 16, Messages.getString(MessageIds.GDE_MSGT2282), 175,
														Messages.getString(MessageIds.GDE_MSGT2241) + GDE.STRING_MESSAGE_CONCAT + Messages.getString(MessageIds.GDE_MSGT2240), 220, UltraDuoPlusDialog.offOnType, 50, 150);
											}
											this.scrolledchargeComposite.setContent(this.chargeGroup);
											this.chargeGroup.setSize(620, this.chargeSelectHeight);
											this.scrolledchargeComposite.addControlListener(new ControlListener() {
												public void controlResized(ControlEvent evt) {
													log.log(java.util.logging.Level.FINEST, "scrolledMemoryComposite.controlResized, event=" + evt); //$NON-NLS-1$
													UltraDuoPlusDialog.this.chargeGroup.setSize(UltraDuoPlusDialog.this.scrolledchargeComposite.getClientArea().width, UltraDuoPlusDialog.this.chargeSelectHeight);
												}
												public void controlMoved(ControlEvent evt) {
													log.log(java.util.logging.Level.FINEST, "scrolledMemoryComposite.controlMoved, event=" + evt); //$NON-NLS-1$
													UltraDuoPlusDialog.this.chargeGroup.setSize(UltraDuoPlusDialog.this.scrolledchargeComposite.getClientArea().width, UltraDuoPlusDialog.this.chargeSelectHeight);
												}
											});
										}
									}
									{
										this.dischargeTabItem = new CTabItem(this.chargeTypeTabFolder, SWT.NONE);
										this.dischargeTabItem.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
										this.dischargeTabItem.setText(Messages.getString(MessageIds.GDE_MSGT2300));
										{
											this.dischargeCycleComposite = new Composite(this.chargeTypeTabFolder, SWT.NONE);
											RowLayout scrolledComposite1Layout = new RowLayout(org.eclipse.swt.SWT.VERTICAL);
											this.dischargeCycleComposite.setLayout(scrolledComposite1Layout);
											this.dischargeTabItem.setControl(this.dischargeCycleComposite);
											{
												this.dischargeGroup = new Group(this.dischargeCycleComposite, SWT.NONE);
												RowData dischargeGroupLData = new RowData(620, UltraDuoPlusDialog.this.dischargeSelectHeight);
												this.dischargeGroup.setLayoutData(dischargeGroupLData);
												FillLayout memoryCompositeLayout = new FillLayout(SWT.VERTICAL);
												this.dischargeGroup.setLayout(memoryCompositeLayout);
												this.dischargeGroup.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
												this.dischargeGroup.setText(Messages.getString(MessageIds.GDE_MSGT2301));
												this.dischargeGroup.setForeground(SWTResourceManager.getColor(SWT.COLOR_BLUE));
												//discharge parameter
												this.memoryParameters[17] = new ParameterConfigControl(this.dischargeGroup, this.memoryValues, 17, GDE.STRING_EMPTY, Messages.getString(MessageIds.GDE_MSGT2268), 175,
														"100 ~ 10000 mA", 220, true, 50, 150, 100, 10000, -100); //$NON-NLS-1$ 
												this.memoryParameters[18] = new ParameterConfigControl(this.dischargeGroup, this.memoryValues, 18, GDE.STRING_EMPTY, Messages.getString(MessageIds.GDE_MSGT2269), 175,
														"100 ~ 4200 mV", 220, true, 50, 150, 100, 4200, -100); //$NON-NLS-1$ 
												this.memoryParameters[20] = new ParameterConfigControl(this.dischargeGroup, this.memoryValues, 20, GDE.STRING_EMPTY, Messages.getString(MessageIds.GDE_MSGT2271), 175,
														Messages.getString(MessageIds.GDE_MSGT2311), 220, false, 50, 150, 10, 105, -10);
												this.memoryParameters[19] = (this.device.getDeviceTypeIdentifier() != GraupnerDeviceType.UltraDuoPlus45 ? this.channelValues1[4] == 0 : this.channelValues1[0] == 0)//°C
												? new ParameterConfigControl(this.dischargeGroup, this.memoryValues, 19, GDE.STRING_EMPTY, Messages.getString(MessageIds.GDE_MSGT2270), 175,
														"10 ~ 80°C", 220, false, 50, 150, 10, 80) //$NON-NLS-1$ 
														: new ParameterConfigControl(this.dischargeGroup, this.memoryValues, 19, GDE.STRING_EMPTY, Messages.getString(MessageIds.GDE_MSGT2270), 175,
																"50 ~ 176°F", 220, false, 50, 150, 50, 176); //$NON-NLS-1$ 
												this.memoryParameters[21] = new ParameterConfigControl(this.dischargeGroup, this.memoryValues, 21, GDE.STRING_EMPTY, Messages.getString(MessageIds.GDE_MSGT2283), 175,
														"1100 ~ 1300 mV", 220, true, 50, 150, 1100, 1300, -1100); //$NON-NLS-1$
											}
											{
												this.cycleGroup = new Group(this.dischargeCycleComposite, SWT.NONE);
												RowData cycleGroupLData = new RowData(625, UltraDuoPlusDialog.this.cycleSelectHeight);
												this.cycleGroup.setLayoutData(cycleGroupLData);
												FillLayout memoryCompositeLayout = new FillLayout(SWT.VERTICAL);
												this.cycleGroup.setLayout(memoryCompositeLayout);
												this.cycleGroup.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
												this.cycleGroup.setText(Messages.getString(MessageIds.GDE_MSGT2302));
												this.cycleGroup.setForeground(SWTResourceManager.getColor(SWT.COLOR_BLUE));
												//cycle parameter
												this.memoryParameters[22] = new ParameterConfigControl(this.cycleGroup, this.memoryValues, 22, Messages.getString(MessageIds.GDE_MSGT2272), 175,
														Messages.getString(MessageIds.GDE_MSGT2292), 220, UltraDuoPlusDialog.cycleDirectionTypes, 50, 150);
												this.memoryParameters[23] = new ParameterConfigControl(this.cycleGroup, this.memoryValues, 23, GDE.STRING_EMPTY, Messages.getString(MessageIds.GDE_MSGT2273), 175,
														"1 ~ 10", 220, false, 50, 150, 1, 10); //$NON-NLS-1$ 
												this.memoryParameters[24] = new ParameterConfigControl(this.cycleGroup, this.memoryValues, 24, GDE.STRING_EMPTY, Messages.getString(MessageIds.GDE_MSGT2274), 175,
														"1 ~ 30min", 220, false, 50, 150, 1, 30); //$NON-NLS-1$ 
												this.memoryParameters[25] = new ParameterConfigControl(this.cycleGroup, this.memoryValues, 25, GDE.STRING_EMPTY, Messages.getString(MessageIds.GDE_MSGT2275), 175,
														"1 ~ 30min", 220, false, 50, 150, 1, 30); //$NON-NLS-1$ 
											}
										}
									}
									this.chargeTypeTabFolder.setSelection(0);
								}
							}
						}
						{
							this.memoryCycleDataTabItem = new CTabItem(this.mainTabFolder, SWT.NONE);
							this.memoryCycleDataTabItem.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
							this.memoryCycleDataTabItem.setText(Messages.getString(MessageIds.GDE_MSGT2320));
							{
								this.memoryDataComposite = new Composite(this.mainTabFolder, SWT.NONE);
								this.memoryCycleDataTabItem.setControl(this.memoryDataComposite);
								this.memoryDataComposite.setLayout(new FormLayout());
								{
									this.memoryDataSelectComposite = new Composite(this.memoryDataComposite, SWT.BORDER);
									FormData memorySelectLData = new FormData();
									memorySelectLData.height = 50;
									memorySelectLData.left = new FormAttachment(0, 1000, 0);
									memorySelectLData.right = new FormAttachment(1000, 1000, 0);
									memorySelectLData.top = new FormAttachment(0, 1000, 0);
									this.memoryDataSelectComposite.setLayoutData(memorySelectLData);
									RowLayout composite2Layout = new RowLayout(SWT.HORIZONTAL);
									this.memoryDataSelectComposite.setLayout(composite2Layout);
									this.memoryDataSelectComposite.setBackground(DataExplorer.COLOR_CANVAS_YELLOW);
									{
										Composite filler = new Composite(this.memoryDataSelectComposite, SWT.NONE);
										filler.setLayoutData(new RowData(500, 10));
										filler.setBackground(DataExplorer.COLOR_CANVAS_YELLOW);
									}
									{
										this.memoryDataSelectLabel = new CLabel(this.memoryDataSelectComposite, SWT.RIGHT);
										RowData memoryCycleDataSelectLabelLData = new RowData();
										memoryCycleDataSelectLabelLData.width = 315;
										memoryCycleDataSelectLabelLData.height = 20;
										this.memoryDataSelectLabel.setLayoutData(memoryCycleDataSelectLabelLData);
										this.memoryDataSelectLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
										this.memoryDataSelectLabel.setText(Messages.getString(MessageIds.GDE_MSGT2321) + Messages.getString(MessageIds.GDE_MSGT2251));
										this.memoryDataSelectLabel.setBackground(DataExplorer.COLOR_CANVAS_YELLOW);
									}
									{
										//this.memoryNames will be updated by memoryCombo selection handler
										this.memoryDataCombo = new CCombo(this.memoryDataSelectComposite, SWT.BORDER);
										this.memoryDataCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
										this.memoryDataCombo.setItems(this.memoryNames);
										this.memoryDataCombo.setVisibleItemCount(20);
										this.memoryDataCombo.setTextLimit(5 + 16);
										RowData memoryComboCycleDataLData = new RowData();
										memoryComboCycleDataLData.width = 165;
										memoryComboCycleDataLData.height = GDE.IS_WINDOWS ? 16 : 18;
										this.memoryDataCombo.setLayoutData(memoryComboCycleDataLData);
										this.memoryDataCombo.setToolTipText(Messages.getString(MessageIds.GDE_MSGT2339));
										this.memoryDataCombo.select(0);
										this.memoryDataCombo.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
										this.memoryDataCombo.setEditable(true);
										this.memoryDataCombo.addSelectionListener(new SelectionAdapter() {
											@Override
											public void widgetSelected(SelectionEvent evt) {
												log.log(java.util.logging.Level.FINEST, "memoryComboData.widgetSelected, event=" + evt); //$NON-NLS-1$
												UltraDuoPlusDialog.this.memorySelectionIndexData = UltraDuoPlusDialog.this.memoryDataCombo.getSelectionIndex() + 1;
											}
										});
									}
								}
								{
									Button cycleDataButton = new Button(this.memoryDataComposite, SWT.Selection);
									FormData cycleDataButtonLData = new FormData();
									cycleDataButtonLData.height = 30;
									cycleDataButtonLData.left = new FormAttachment(0, 1000, 150);
									cycleDataButtonLData.right = new FormAttachment(1000, 1000, -150);
									cycleDataButtonLData.top = new FormAttachment(0, 1000, 70);
									cycleDataButton.setLayoutData(cycleDataButtonLData);
									cycleDataButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
									cycleDataButton.setText(Messages.getString(MessageIds.GDE_MSGT2322));
									cycleDataButton.setToolTipText(Messages.getString(MessageIds.GDE_MSGT2316));
									cycleDataButton.addSelectionListener(new SelectionAdapter() {
										@Override
										public void widgetSelected(SelectionEvent evt) {
											log.log(java.util.logging.Level.FINEST, "cycleDataButton.widgetSelected, event=" + evt); //$NON-NLS-1$
											try {
												UltraDuoPlusDialog.this.cycleDataProgressBar.setSelection(0);
												GraphicsWindow cycleGraph = (GraphicsWindow) UltraDuoPlusDialog.this.device.getUtilityDeviceTabItem();
												RecordSet utilitySet = UltraDuoPlusDialog.this.application.getUtilitySet();
												utilitySet.clear();
												for (int i = 0; i < Ultramat.cycleDataRecordNames.length; ++i) {
													Record tmpRecord = new Record(UltraDuoPlusDialog.this.device, i, Ultramat.cycleDataRecordNames[i], GDE.STRING_EMPTY, Ultramat.cycleDataUnitNames[i], true, null,
															new ArrayList<PropertyType>(), 11);
													tmpRecord.setFactor(Ultramat.cycleDataFactors[i]);
													if (Ultramat.cycleDataSyncRefOrdinal[i] >= 0) {
														tmpRecord.createProperty(MeasurementPropertyTypes.SCALE_SYNC_REF_ORDINAL.value(), DataTypes.INTEGER, Ultramat.cycleDataSyncRefOrdinal[i]);
													}
													tmpRecord.setColorDefaultsAndPosition(i);
													utilitySet.put(Ultramat.cycleDataRecordNames[i], tmpRecord);
													tmpRecord.setColor(SWTResourceManager.getColor(Ultramat.cycleDataColors[i][0], Ultramat.cycleDataColors[i][1], Ultramat.cycleDataColors[i][2]));
													if (i >= 4) tmpRecord.setPositionLeft(false);
													if ((i + 1) % 2 == 0) tmpRecord.setVisible(false);
												}
												utilitySet.setHorizontalGridType(RecordSet.HORIZONTAL_GRID_EVERY);
												utilitySet.setHorizontalGridRecordOrdinal(4);
												utilitySet.setTimeGridType(RecordSet.TIME_GRID_MAIN);
												utilitySet.setTimeStep_ms(-1.0); //different time steps
												utilitySet.syncScaleOfSyncableRecords();

												UltraDuoPlusDialog.this.application.getTabFolder().setSelection(cycleGraph);
												UltraDuoPlusDialog.this.cycleDataProgressBar.setSelection(50);

												long justNowPlus2Hours = new Date().getTime() + 7200000L;
												long justNowMinus2Year = new Date().getTime() - 63072000000L;
												boolean isDateChanged = false;
												Vector<byte[]> cyclesData = new Vector<byte[]>();
												TreeMap<Long, int[]> sortCyclesData = new TreeMap<Long, int[]>();
												if (Boolean.parseBoolean(System.getProperty("GDE_IS_SIMULATION"))) { //$NON-NLS-1$
													//test data - change dates in timeSteps block below to vary
													//original data
													String memoryCycleData = "00030026000B00040011050141C2040C00213F880000000B0004000B00040018017341D905910000000000000009002D000B0005000709BF417E03B50000000000000008002B000C00050009084341DC04B4000B0000000000000032000F00050012001A41F0000000000000000000090015000C0005001D0A2E4194044E00000000000000100004000B000600040ADF41960566000F0022001D00120034000B0006000C07D0418004740000000000000003000E000E0006000D094241E206A30000000000000000000000000000000000000000000000000000000000210088000000000000000000000000000000000000";
													//updated data
													//String memoryCycleData = "00030026000B00040011050141C2040C00213F880000000B0004000B00040018017341D905910000000000000009002D000B0005000709BF417E03B500000000000000130013000B0005000A084341DC04B4000B0000000000000032000000050012001A41F0000000000000000000130028000B0005001E0A2E4194044E00000000000000100004000B000600040ADF41960566000F0022001D00120034000B0006000C07D041800474000000000000000D0022000B00060010094241E206A30000000000000000000000000000000000000000000000000000000000210088000000000000000000000000000000000000";
													for (int i = 0; i < 11; i++) {
														int startIndex = i * (11 * 4);
														int endIndex = (i + 1) * (11 * 4);
														cyclesData.add(memoryCycleData.substring(startIndex, endIndex).getBytes());
													}

													//													long[] timeSteps = {
													//															new GregorianCalendar(2011, 06, 01, 03, 38, 0).getTimeInMillis(), 
													//															new GregorianCalendar(2011, 06, 02, 03, 38, 0).getTimeInMillis(), 
													//															new GregorianCalendar(2011, 06, 03, 11, 04, 0).getTimeInMillis(),
													//															new GregorianCalendar(2011, 06, 04,  9, 45, 0).getTimeInMillis(),
													//															new GregorianCalendar(2011, 06, 04, 16, 04, 0).getTimeInMillis(),
													//															new GregorianCalendar(2011, 06, 12, 18, 52, 0).getTimeInMillis(),
													//													};
													//													int[][] pointss = { 
													//															{16834,     0, 1000,  0, 1300,  0},
													//															{16834, 16264, 1281, 33, 1036,  0},
													//															{16857,     0,  371,  0, 1425,  0},
													//															{16766,     0, 2783,  0,  949,  0},
													//															{16790,    34, 2783, 15, 1382, 29},
													//															{16768, 		0, 2000,  0, 1140,  0},
													//													};
													//													//                             2;        0;     	 4;        3;        1;        5
													//													//2011-04-17, 03:38:00;     1281;    16834;     1036;       33;    16264;        0
													//													//2011-04-24, 11:04:00;      371;    16857;     1425;        0;        0;        0
													//													//2011-05-07, 09:45:00;     2783;    16766;      949;        0;        0;        0
													//													//2011-06-04, 16:04:00;     2783;    16790;     1382;       15;       34;       29
													//													//2011-06-12, 18:52:00;     2000;    16768;     1140;        0;        0;        0
													//													for (int i = 0; i < timeSteps.length; i++) {
													//														sortCyclesData.put(timeSteps[i], pointss[i].clone());
													//													}
												}
												else {
													cyclesData = UltraDuoPlusDialog.this.serialPort.readMemoryCycleData(UltraDuoPlusDialog.this.memorySelectionIndexData);
												}

												if (log.isLoggable(java.util.logging.Level.FINE)) {
													StringBuilder sb = new StringBuilder();
													for (byte[] cycleData : cyclesData) {
														sb.append(new String(cycleData));
													}
													log.log(java.util.logging.Level.FINE, sb.toString());
												}
												UltraDuoPlusDialog.this.dataTable.removeAll();

												for (byte[] cycleData : cyclesData) {
													long timeStamp = 0;
													int[] points = new int[6];
													int hour = 0;
													int minute = 0;
													int year = 0;
													int month = 0;
													int day = 0;
													try {
														hour = Integer.parseInt(String.format(DeviceSerialPortImpl.FORMAT_4_CHAR, cycleData[0], cycleData[1], cycleData[2], cycleData[3]), 16);
														minute = Integer.parseInt(String.format(DeviceSerialPortImpl.FORMAT_4_CHAR, cycleData[4], cycleData[5], cycleData[6], cycleData[7]), 16);
														day = Integer.parseInt(String.format(DeviceSerialPortImpl.FORMAT_4_CHAR, cycleData[8], cycleData[9], cycleData[10], cycleData[11]), 16);
														month = Integer.parseInt(String.format(DeviceSerialPortImpl.FORMAT_4_CHAR, cycleData[12], cycleData[13], cycleData[14], cycleData[15]), 16);
														year = Integer.parseInt(String.format(DeviceSerialPortImpl.FORMAT_4_CHAR, cycleData[16], cycleData[17], cycleData[18], cycleData[19]), 16);
														timeStamp = new GregorianCalendar(2000 + year, month - 1, day, hour, minute, 0).getTimeInMillis();
													}
													catch (NumberFormatException e) {
														log.log(java.util.logging.Level.WARNING, e.getMessage(), e);
													}

													try {
														points[2] = Integer.parseInt(String.format(DeviceSerialPortImpl.FORMAT_4_CHAR, cycleData[20], (char) cycleData[21], (char) cycleData[22], (char) cycleData[23]), 16);
														points[0] = Integer.parseInt(String.format(DeviceSerialPortImpl.FORMAT_4_CHAR, cycleData[24], (char) cycleData[25], (char) cycleData[26], (char) cycleData[27]), 16);
														points[4] = Integer.parseInt(String.format(DeviceSerialPortImpl.FORMAT_4_CHAR, cycleData[28], (char) cycleData[29], (char) cycleData[30], (char) cycleData[31]), 16);
														points[3] = Integer.parseInt(String.format(DeviceSerialPortImpl.FORMAT_4_CHAR, cycleData[32], (char) cycleData[33], (char) cycleData[34], (char) cycleData[35]), 16);
														points[1] = Integer.parseInt(String.format(DeviceSerialPortImpl.FORMAT_4_CHAR, cycleData[36], (char) cycleData[37], (char) cycleData[38], (char) cycleData[39]), 16);
														points[5] = Integer.parseInt(String.format(DeviceSerialPortImpl.FORMAT_4_CHAR, cycleData[40], (char) cycleData[41], (char) cycleData[42], (char) cycleData[43]), 16);
													}
													catch (Exception e) {
														log.log(java.util.logging.Level.WARNING, e.getMessage(), e);
													}

													if (log.isLoggable(java.util.logging.Level.FINE)) {
														StringBuilder sb = new StringBuilder();
														for (int point : points) {
															sb.append("; ").append(String.format("%8d", point)); //$NON-NLS-1$ //$NON-NLS-2$
														}
														log.log(java.util.logging.Level.FINE, StringHelper.getFormatedTime("yyyy-MM-dd, HH:mm:ss", timeStamp) + sb.toString()); //$NON-NLS-1$
													}

													//if time stamp is not between just now - 1 year and just now + 2 hrs  and contains data ask if the date should be corrected
													long dataSum = 0;
													for (int point : points) {
														dataSum += point;
													}
													if (dataSum > 0 && (timeStamp < justNowMinus2Year || timeStamp > justNowPlus2Hours)) {
														log.log(java.util.logging.Level.FINER, "time stamp out of range ! " + StringHelper.getFormatedTime("yyyy-MM-dd, HH:mm:ss", timeStamp)); //$NON-NLS-1$ //$NON-NLS-2$
														int[] newTimeStamp = new ChangeDateDialog(UltraDuoPlusDialog.this.dialogShell, SWT.NONE, new int[] { hour, minute, 2000 + year, month, day, points[2], points[3] }).open();
														if (newTimeStamp.length > 0) { //change requested
															log.log(Level.FINE, "date change requested !"); //$NON-NLS-1$
															isDateChanged = true;
															newTimeStamp[0] = newTimeStamp[0] < 0 ? 0 : newTimeStamp[0] > 24 ? 24 : newTimeStamp[0]; //hour
															newTimeStamp[1] = newTimeStamp[1] < 0 ? 0 : newTimeStamp[1] > 60 ? 60 : newTimeStamp[1]; //minute
															newTimeStamp[2] = newTimeStamp[2] <= 2000 ? 0 : newTimeStamp[2] - 2000; //year
															newTimeStamp[3] = newTimeStamp[3] < 1 ? 1 : newTimeStamp[3] > 12 ? 12 : newTimeStamp[3]; //month
															newTimeStamp[4] = newTimeStamp[4] < 1 ? 1 : newTimeStamp[4] > 30 ? 30 : newTimeStamp[4]; //day
															for (int i = 0, k = 0; i < newTimeStamp.length; i++, k += 4) {
																byte[] bytes = String.format("%04X", newTimeStamp[i]).getBytes(); //$NON-NLS-1$
																for (int j = 0; j < bytes.length; j++) {
																	cycleData[j + k] = bytes[j];
																}
															}
															hour = Integer.parseInt(String.format(DeviceSerialPortImpl.FORMAT_4_CHAR, cycleData[0], cycleData[1], cycleData[2], cycleData[3]), 16);
															minute = Integer.parseInt(String.format(DeviceSerialPortImpl.FORMAT_4_CHAR, cycleData[4], cycleData[5], cycleData[6], cycleData[7]), 16);
															year = Integer.parseInt(String.format(DeviceSerialPortImpl.FORMAT_4_CHAR, cycleData[8], cycleData[9], cycleData[10], cycleData[11]), 16);
															month = Integer.parseInt(String.format(DeviceSerialPortImpl.FORMAT_4_CHAR, cycleData[12], cycleData[13], cycleData[14], cycleData[15]), 16);
															day = Integer.parseInt(String.format(DeviceSerialPortImpl.FORMAT_4_CHAR, cycleData[16], cycleData[17], cycleData[18], cycleData[19]), 16);
															timeStamp = new GregorianCalendar(2000 + year, month - 1, day, hour, minute, 0).getTimeInMillis();
														}
													}
													//add selected entries to the sorted map, this is what gets displayed in the utility record
													if (dataSum > 0 && timeStamp > justNowMinus2Year && timeStamp < justNowPlus2Hours) {
														sortCyclesData.put(timeStamp, points.clone());
													}
												}
												for (Entry<Long, int[]> entry : sortCyclesData.entrySet()) {
													//display values
													TableItem item = new TableItem(UltraDuoPlusDialog.this.dataTable, SWT.CENTER);
													int[] points = entry.getValue();
													item.setText(new String[] { StringHelper.getFormatedTime("yyyy-MM-dd, HH:mm", entry.getKey()), //$NON-NLS-1$
															String.format("%.2f", points[0] / 1000.0), //$NON-NLS-1$
															String.format("%.2f", points[1] / 1000.0), //$NON-NLS-1$
															String.format("%.2f", points[2] / 1.0), //$NON-NLS-1$
															String.format("%.2f", points[3] / 1.0), //$NON-NLS-1$
															String.format("%.2f", points[4] / 10.0), //$NON-NLS-1$
															String.format("%.2f", points[5] / 10.0), //$NON-NLS-1$
													});

												}

												//check if time stamp was changed, if yes write back changed data to device
												if (isDateChanged) {
													UltraDuoPlusDialog.this.application.openMessageDialogAsync(UltraDuoPlusDialog.this.dialogShell, Messages.getString(MessageIds.GDE_MSGT2333));
													//not implemented device firmware: serialPort.writeMemoryCycleData(UltraDuoPlusDialog.this.memorySelectionIndexData, cyclesData);
													isDateChanged = false;
												}

												log.log(Level.FINE, "used entries between " + StringHelper.getFormatedTime("yyyy-MM-dd, HH:mm:ss", justNowMinus2Year) + GDE.STRING_MESSAGE_CONCAT + StringHelper.getFormatedTime("yyyy-MM-dd, HH:mm:ss", justNowPlus2Hours) + GDE.LINE_SEPARATOR); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
												long lastTimeStamp = 0;
												for (Entry<Long, int[]> entry : sortCyclesData.entrySet()) {
													utilitySet.addPoints(entry.getValue(), (lastTimeStamp == 0 ? 0 : entry.getKey() - lastTimeStamp));

													if (log.isLoggable(java.util.logging.Level.FINE)) {
														StringBuilder sb = new StringBuilder();
														for (int i = 0; i < entry.getValue().length; i++) {
															sb.append("; ").append(String.format("%8d", entry.getValue()[i])); //$NON-NLS-1$ //$NON-NLS-2$
														}
														log.log(Level.FINE, StringHelper.getFormatedTime("yyyy-MM-dd, HH:mm:ss", entry.getKey()) + String.format("; %12d%s", (lastTimeStamp == 0 ? 0 : entry.getKey() - lastTimeStamp), sb.toString())); //$NON-NLS-1$ //$NON-NLS-2$
													}
													lastTimeStamp = lastTimeStamp == 0 ? entry.getKey() : lastTimeStamp;
												}
												Long[] dates = sortCyclesData.keySet().toArray(new Long[0]);
												Arrays.sort(dates);
												if (dates != null && dates.length > 1) {
													utilitySet.setRecordSetDescription(UltraDuoPlusDialog.this.memoryDataCombo.getText()
															+ GDE.STRING_BLANK_COLON_BLANK
															+ StringHelper.getFormatedTime("yyyy-MM-dd, HH:mm:ss", dates[0]) + GDE.STRING_MESSAGE_CONCAT + StringHelper.getFormatedTime("yyyy-MM-dd, HH:mm:ss", dates[dates.length - 1])); //$NON-NLS-1$ //$NON-NLS-2$
												}
												else {
													utilitySet.setRecordSetDescription(UltraDuoPlusDialog.this.memoryDataCombo.getText());
												}
												cycleGraph.enableGraphicsHeader(true);
												UltraDuoPlusDialog.this.application.getTabFolder().notifyListeners(SWT.Selection, new Event());
												UltraDuoPlusDialog.this.cycleDataProgressBar.setSelection(100);
											}
											catch (Exception e) {
												log.log(java.util.logging.Level.WARNING, e.getMessage(), e);
											}
										}
									});
								}
								{
									this.cycleDataProgressBar = new ProgressBar(this.memoryDataComposite, SWT.NONE);
									FormData cycleDataProgressBarLData = new FormData();
									cycleDataProgressBarLData.height = 15;
									cycleDataProgressBarLData.left = new FormAttachment(0, 1000, 150);
									cycleDataProgressBarLData.right = new FormAttachment(1000, 1000, -150);
									cycleDataProgressBarLData.top = new FormAttachment(0, 1000, 110);
									this.cycleDataProgressBar.setLayoutData(cycleDataProgressBarLData);
									this.cycleDataProgressBar.setMinimum(0);
									this.cycleDataProgressBar.setMinimum(100);
								}
								{
									this.dataTable = new Table(this.memoryDataComposite, SWT.BORDER);
									FormData dataTableLData = new FormData();
									dataTableLData.height = 200;
									dataTableLData.left = new FormAttachment(0, 1000, 5);
									dataTableLData.right = new FormAttachment(1000, 1000, -5);
									dataTableLData.top = new FormAttachment(0, 1000, 140);
									this.dataTable.setLayoutData(dataTableLData);
									this.dataTable.setLinesVisible(true);
									this.dataTable.setHeaderVisible(true);
									this.dataTable.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
									TableColumn timeColumn = new TableColumn(this.dataTable, SWT.CENTER);
									timeColumn.setText("yyyy-MM-dd, HH:mm"); //$NON-NLS-1$
									timeColumn.setWidth(timeColumn.getText().length() * 9);
									for (int i = 0; i < 6; i++) {
										StringBuilder sb = new StringBuilder();
										sb.append(Ultramat.cycleDataTableNames[i]).append(GDE.STRING_BLANK_LEFT_BRACKET).append(Ultramat.cycleDataUnitNames[i]).append(GDE.STRING_RIGHT_BRACKET);
										TableColumn column = new TableColumn(this.dataTable, SWT.CENTER);
										column.setWidth(77);
										column.setText(sb.toString());
									}
								}
								{
									this.channelSelectComposite = new Composite(this.memoryDataComposite, SWT.BORDER);
									FormData channelSelectLData = new FormData();
									channelSelectLData.height = 50;
									channelSelectLData.left = new FormAttachment(0, 1000, 0);
									channelSelectLData.right = new FormAttachment(1000, 1000, 0);
									channelSelectLData.bottom = new FormAttachment(1000, 1000, -90);
									this.channelSelectComposite.setLayoutData(channelSelectLData);
									RowLayout composite2Layout = new RowLayout(SWT.HORIZONTAL);
									this.channelSelectComposite.setLayout(composite2Layout);
									this.channelSelectComposite.setBackground(DataExplorer.COLOR_CANVAS_YELLOW);
									{
										Composite filler = new Composite(this.channelSelectComposite, SWT.NONE);
										filler.setLayoutData(new RowData(500, 10));
										filler.setBackground(DataExplorer.COLOR_CANVAS_YELLOW);
									}
									{
										this.channelSelectLabel = new CLabel(this.channelSelectComposite, SWT.RIGHT);
										RowData memoryCycleDataSelectLabelLData = new RowData();
										memoryCycleDataSelectLabelLData.width = 370;
										memoryCycleDataSelectLabelLData.height = 20;
										this.channelSelectLabel.setLayoutData(memoryCycleDataSelectLabelLData);
										this.channelSelectLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
										this.channelSelectLabel.setText(Messages.getString(MessageIds.GDE_MSGT2334));
										this.channelSelectLabel.setBackground(DataExplorer.COLOR_CANVAS_YELLOW);
									}
									{
										//this.memoryNames will be updated by memoryCombo selection handler
										this.channelCombo = new CCombo(this.channelSelectComposite, SWT.BORDER | SWT.CENTER);
										this.channelCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
										this.channelCombo.setItems(this.channelNumbers);
										this.channelCombo.setVisibleItemCount(2);
										RowData memoryComboCycleDataLData = new RowData();
										memoryComboCycleDataLData.width = 35;
										memoryComboCycleDataLData.height = GDE.IS_WINDOWS ? 16 : 18;
										this.channelCombo.setLayoutData(memoryComboCycleDataLData);
										this.channelCombo.setToolTipText(Messages.getString(MessageIds.GDE_MSGT2335));
										this.channelCombo.select(0);
										this.channelCombo.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
										this.channelCombo.setEditable(false);
										this.channelCombo.addSelectionListener(new SelectionAdapter() {
											@Override
											public void widgetSelected(SelectionEvent evt) {
												log.log(java.util.logging.Level.FINEST, "memoryComboData.widgetSelected, event=" + evt); //$NON-NLS-1$
												UltraDuoPlusDialog.this.channelSelectionIndex = UltraDuoPlusDialog.this.channelCombo.getSelectionIndex() + 1;
											}
										});
									}
								}
								{
									Button graphicsDataButton = new Button(this.memoryDataComposite, SWT.Selection);
									FormData graphicsDataButtonLData = new FormData();
									graphicsDataButtonLData.height = 30;
									graphicsDataButtonLData.left = new FormAttachment(0, 1000, 150);
									graphicsDataButtonLData.right = new FormAttachment(1000, 1000, -150);
									graphicsDataButtonLData.bottom = new FormAttachment(1000, 1000, -40);
									graphicsDataButton.setLayoutData(graphicsDataButtonLData);
									graphicsDataButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
									graphicsDataButton.setText(Messages.getString(MessageIds.GDE_MSGT2318));
									graphicsDataButton.setToolTipText(Messages.getString(MessageIds.GDE_MSGT2319));
									graphicsDataButton.addSelectionListener(new SelectionAdapter() {
										@Override
										public void widgetSelected(SelectionEvent evt) {
											log.log(java.util.logging.Level.FINEST, "graphicsDataButton.widgetSelected, event=" + evt); //$NON-NLS-1$
											try {
												UltraDuoPlusDialog.this.dialogShell.setCursor(SWTResourceManager.getCursor(SWT.CURSOR_WAIT));
												int[] points = new int[UltraDuoPlusDialog.this.device.getMeasurementNames(UltraDuoPlusDialog.this.channelSelectionIndex).length];
												final byte[][] graphicsData = new byte[3][];
												UltraDuoPlusDialog.this.serialPort.readGraphicsData(graphicsData, UltraDuoPlusDialog.this.channelSelectionIndex, UltraDuoPlusDialog.this);

												//create a new record set at the selected output channel
												String processName = Messages.getString(MessageIds.GDE_MSGT2337);
												RecordSet recordSet = null;
												Channel channel = Channels.getInstance().get(UltraDuoPlusDialog.this.channelSelectionIndex);
												if (channel != null) {
													String recordSetKey = channel.getNextRecordSetNumber() + GDE.STRING_RIGHT_PARENTHESIS_BLANK + processName;
													recordSetKey = recordSetKey.length() <= RecordSet.MAX_NAME_LENGTH ? recordSetKey : recordSetKey.substring(0, RecordSet.MAX_NAME_LENGTH);

													channel.put(recordSetKey, RecordSet.createRecordSet(recordSetKey, UltraDuoPlusDialog.this.device, UltraDuoPlusDialog.this.channelSelectionIndex, true, false));
													channel.applyTemplateBasics(recordSetKey);
													log.log(java.util.logging.Level.FINE, recordSetKey + " created for channel " + channel.getName()); //$NON-NLS-1$
													recordSet = channel.get(recordSetKey);
													UltraDuoPlusDialog.this.device.setTemperatureUnit(UltraDuoPlusDialog.this.channelSelectionIndex, recordSet, UltraDuoPlusDialog.this.initialAnswerData); //°C or °F
													recordSet.setAllDisplayable();
													// switch the active record set if the current record set is child of active channel
													UltraDuoPlusDialog.this.channels.switchChannel(UltraDuoPlusDialog.this.channelSelectionIndex, recordSetKey);
													channel.switchRecordSet(recordSetKey);
													String description = recordSet.getRecordSetDescription() + GDE.LINE_SEPARATOR
															+ "Firmware  : " + UltraDuoPlusDialog.this.device.getFirmwareVersion(UltraDuoPlusDialog.this.initialAnswerData) //$NON-NLS-1$
															+ (UltraDuoPlusDialog.this.device.getBatteryMemoryNumber(UltraDuoPlusDialog.this.channelSelectionIndex, UltraDuoPlusDialog.this.initialAnswerData) >= 1 ? "; Memory #" + UltraDuoPlusDialog.this.device.getBatteryMemoryNumber(UltraDuoPlusDialog.this.channelSelectionIndex, UltraDuoPlusDialog.this.initialAnswerData) : GDE.STRING_EMPTY); //$NON-NLS-1$
													try {
														int batteryMemoryNumber = UltraDuoPlusDialog.this.device.getBatteryMemoryNumber(UltraDuoPlusDialog.this.channelSelectionIndex, UltraDuoPlusDialog.this.initialAnswerData);
														if (batteryMemoryNumber > 0 && UltraDuoPlusDialog.this.ultraDuoPlusSetup != null && UltraDuoPlusDialog.this.ultraDuoPlusSetup.getMemory().get(batteryMemoryNumber) != null) {
															String batteryMemoryName = UltraDuoPlusDialog.this.ultraDuoPlusSetup.getMemory()
																	.get(UltraDuoPlusDialog.this.device.getBatteryMemoryNumber(UltraDuoPlusDialog.this.channelSelectionIndex, UltraDuoPlusDialog.this.initialAnswerData) - 1).getName();
															description = description + GDE.STRING_MESSAGE_CONCAT + batteryMemoryName;
															if (recordSetKey.startsWith("1)")) UltraDuoPlusDialog.this.device.matchBatteryMemory2ObjectKey(batteryMemoryName); //$NON-NLS-1$
														}
													}
													catch (Exception e) {
														e.printStackTrace();
														// ignore and do not append memory name
													}
													recordSet.setRecordSetDescription(description);

													int numOfPoints = Integer.parseInt(String.format(DeviceSerialPortImpl.FORMAT_4_CHAR, graphicsData[0][1], (char) graphicsData[0][2], (char) graphicsData[0][3], (char) graphicsData[0][4]), 16) - 10;
													int timeStep_sec = Integer.parseInt(String.format(DeviceSerialPortImpl.FORMAT_4_CHAR, graphicsData[0][5], (char) graphicsData[0][6], (char) graphicsData[0][7], (char) graphicsData[0][8]), 16);
													recordSet.setNewTimeStep_ms(timeStep_sec * 1000.0);
													for (int i = 0, j = 9; i < numOfPoints; i++, j += 4) {
														// 0=Spannung 1=Strom 5=BatteryTemperature
														points[0] = Integer.parseInt(String.format(DeviceSerialPortImpl.FORMAT_4_CHAR, (char) graphicsData[0][j], (char) graphicsData[0][j + 1], (char) graphicsData[0][j + 2],
																(char) graphicsData[0][j + 3]), 16);
														points[1] = Integer.parseInt(String.format(DeviceSerialPortImpl.FORMAT_4_CHAR, (char) graphicsData[1][j], (char) graphicsData[1][j + 1], (char) graphicsData[1][j + 2],
																(char) graphicsData[1][j + 3]), 16);
														points[5] = Integer.parseInt(String.format(DeviceSerialPortImpl.FORMAT_4_CHAR, (char) graphicsData[2][j], (char) graphicsData[2][j + 1], (char) graphicsData[2][j + 2],
																(char) graphicsData[2][j + 3]), 16);
														recordSet.addPoints(points);
													}
													UltraDuoPlusDialog.this.device.updateVisibilityStatus(recordSet, true);
													UltraDuoPlusDialog.this.application.updateAllTabs(true);
												}

											}
											catch (DataInconsitsentException e) {
												log.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
												UltraDuoPlusDialog.this.application.openMessageDialogAsync(UltraDuoPlusDialog.this.dialogShell, Messages.getString(MessageIds.GDE_MSGT2338));
											}
											catch (Exception e) {
												log.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
												UltraDuoPlusDialog.this.application.openMessageDialogAsync(UltraDuoPlusDialog.this.dialogShell, Messages.getString(MessageIds.GDE_MSGT2336));												
											}
											finally {
												UltraDuoPlusDialog.this.dialogShell.setCursor(SWTResourceManager.getCursor(SWT.CURSOR_ARROW));
											}
										}
									});
								}
								{
									this.graphicsDataProgressBar = new ProgressBar(this.memoryDataComposite, SWT.NONE);
									FormData graphicsDataProgressBarLData = new FormData();
									graphicsDataProgressBarLData.height = 15;
									graphicsDataProgressBarLData.left = new FormAttachment(0, 1000, 150);
									graphicsDataProgressBarLData.right = new FormAttachment(1000, 1000, -150);
									graphicsDataProgressBarLData.bottom = new FormAttachment(1000, 1000, -15);
									this.graphicsDataProgressBar.setLayoutData(graphicsDataProgressBarLData);
									this.graphicsDataProgressBar.setMinimum(0);
									this.graphicsDataProgressBar.setMinimum(100);
								}
							}
						}
						this.mainTabFolder.setSelection(1);
					}
					{
						this.restoreButton = new Button(this.boundsComposite, SWT.PUSH | SWT.CENTER);
						FormData restoreButtonLData = new FormData();
						restoreButtonLData.width = 118;
						restoreButtonLData.height = GDE.IS_MAC ? 33 : 30;
						restoreButtonLData.left = new FormAttachment(0, 1000, 165);
						restoreButtonLData.bottom = new FormAttachment(1000, 1000, -8);
						this.restoreButton.setLayoutData(restoreButtonLData);
						this.restoreButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this.restoreButton.setText(Messages.getString(MessageIds.GDE_MSGT2284));
						this.restoreButton.setEnabled(false);
						this.restoreButton.addSelectionListener(new SelectionAdapter() {
							@Override
							public void widgetSelected(SelectionEvent evt) {
								log.log(java.util.logging.Level.FINEST, "restoreButton.widgetSelected, event=" + evt); //$NON-NLS-1$
								FileDialog fileDialog = UltraDuoPlusDialog.this.application.openFileOpenDialog(UltraDuoPlusDialog.this.dialogShell, Messages.getString(MessageIds.GDE_MSGT2284), new String[] {
										GDE.FILE_ENDING_STAR_XML, GDE.FILE_ENDING_STAR }, UltraDuoPlusDialog.this.settings.getDataFilePath(), GDE.STRING_EMPTY, SWT.SINGLE);
								if (fileDialog.getFileName().length() > 4) {
									try {
										UltraDuoPlusDialog.this.setBackupRetoreButtons(false);
										UltraDuoPlusDialog.this.dialogShell.setCursor(SWTResourceManager.getCursor(SWT.CURSOR_WAIT));

										Unmarshaller unmarshaller = UltraDuoPlusDialog.this.jc.createUnmarshaller();
										unmarshaller.setSchema(UltraDuoPlusDialog.this.schema);
										//merge loaded configuration into active
										mergeUltraDuoPlusSetup((UltraDuoPlusType) unmarshaller.unmarshal(new File(fileDialog.getFilterPath() + GDE.FILE_SEPARATOR + fileDialog.getFileName())));
										//write updated entries while merging
										UltraDuoPlusDialog.this.synchronizerWrite = new UltraDuoPlusSychronizer(UltraDuoPlusDialog.this, UltraDuoPlusDialog.this.serialPort, UltraDuoPlusDialog.this.ultraDuoPlusSetup,
												UltraDuoPlusSychronizer.SYNC_TYPE.WRITE);
										UltraDuoPlusDialog.this.synchronizerWrite.start();
										UltraDuoPlusDialog.this.synchronizerWrite.join();
										//check and sync active configuration with device content
										updateBatterySetup(0);
										if (UltraDuoPlusDialog.this.memoryCombo != null && !UltraDuoPlusDialog.this.memoryCombo.isDisposed()) {
											UltraDuoPlusDialog.this.memoryCombo.setItems(UltraDuoPlusDialog.this.memoryNames);
											UltraDuoPlusDialog.this.memoryCombo.select(0);
										}
									}
									catch (Exception e) {
										log.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
										if (e.getCause() instanceof FileNotFoundException) {
											UltraDuoPlusDialog.this.ultraDuoPlusSetup = new ObjectFactory().createUltraDuoPlusType();
											List<MemoryType> cellMemories = UltraDuoPlusDialog.this.ultraDuoPlusSetup.getMemory();
											if (cellMemories.size() < UltraDuoPlusDialog.numberMemories) { // initially create only base setup data
												for (int i = 0; i < UltraDuoPlusDialog.numberMemories; i++) {
													MemoryType cellMemory = new ObjectFactory().createMemoryType();
													cellMemory.setSetupData(new ObjectFactory().createMemoryTypeSetupData());
													cellMemories.add(cellMemory);
												}
											}
										}
										else
											UltraDuoPlusDialog.this.application.openMessageDialog(UltraDuoPlusDialog.this.dialogShell,
													Messages.getString(gde.messages.MessageIds.GDE_MSGE0007, new String[] { e.getMessage() }));
									}
									finally {
										UltraDuoPlusDialog.this.dialogShell.setCursor(SWTResourceManager.getCursor(SWT.CURSOR_ARROW));
										UltraDuoPlusDialog.this.setBackupRetoreButtons(true);
									}
								}
							}
						});
					}
					{
						this.backupButton = new Button(this.boundsComposite, SWT.PUSH | SWT.CENTER);
						FormData backupButtonLData = new FormData();
						backupButtonLData.width = 118;
						backupButtonLData.height = GDE.IS_MAC ? 33 : 30;
						backupButtonLData.left = new FormAttachment(0, 1000, 29);
						backupButtonLData.bottom = new FormAttachment(1000, 1000, -8);
						this.backupButton.setLayoutData(backupButtonLData);
						this.backupButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this.backupButton.setText(Messages.getString(MessageIds.GDE_MSGT2286));
						this.backupButton.setEnabled(false);
						this.backupButton.addSelectionListener(new SelectionAdapter() {
							@Override
							public void widgetSelected(SelectionEvent evt) {
								log.log(java.util.logging.Level.FINEST, "backupButton.widgetSelected, event=" + evt); //$NON-NLS-1$
								FileDialog fileDialog = UltraDuoPlusDialog.this.application.prepareFileSaveDialog(UltraDuoPlusDialog.this.dialogShell, Messages.getString(MessageIds.GDE_MSGT2285), new String[] {
										GDE.FILE_ENDING_STAR_XML, GDE.FILE_ENDING_STAR }, UltraDuoPlusDialog.this.settings.getDataFilePath(), StringHelper.getDateAndTime("yyyy-MM-dd-HH-mm-ss") + GDE.STRING_UNDER_BAR //$NON-NLS-1$
										+ UltraDuoPlusDialog.this.device.getName() + UltraDuoPlusDialog.this.ultraDuoPlusSetup.getIdentifierName());
								String configFilePath = fileDialog.open();
								if (configFilePath != null && fileDialog.getFileName().length() > 4) {
									if (FileUtils.checkFileExist(configFilePath)) {
										if (SWT.YES != UltraDuoPlusDialog.this.application.openYesNoMessageDialog(UltraDuoPlusDialog.this.dialogShell,
												Messages.getString(gde.messages.MessageIds.GDE_MSGI0007, new Object[] { configFilePath }))) {
											return;
										}
									}
									saveConfigUDP(configFilePath);
								}
							}
						});
					}
					{
						this.closeButton = new Button(this.boundsComposite, SWT.PUSH | SWT.CENTER);
						FormData writeButtonLData = new FormData();
						writeButtonLData.width = 118;
						writeButtonLData.height = GDE.IS_MAC ? 33 : 30;
						writeButtonLData.bottom = new FormAttachment(1000, 1000, -8);
						writeButtonLData.right = new FormAttachment(1000, 1000, -21);
						this.closeButton.setLayoutData(writeButtonLData);
						this.closeButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this.closeButton.setText(Messages.getString(MessageIds.GDE_MSGT2287));
						this.closeButton.addSelectionListener(new SelectionAdapter() {
							@Override
							public void widgetSelected(SelectionEvent evt) {
								log.log(java.util.logging.Level.FINEST, "closeButton.widgetSelected, event=" + evt); //$NON-NLS-1$
								UltraDuoPlusDialog.this.dialogShell.dispose();
							}
						});
					}
					{
						this.helpButton = new Button(this.boundsComposite, SWT.PUSH | SWT.CENTER);
						this.helpButton.setImage(SWTResourceManager.getImage("gde/resource/QuestionHot.gif")); //$NON-NLS-1$
						FormData LoadButtonLData = new FormData();
						LoadButtonLData.width = 118;
						LoadButtonLData.height = GDE.IS_MAC ? 33 : 30;
						LoadButtonLData.bottom = new FormAttachment(1000, 1000, -8);
						LoadButtonLData.right = new FormAttachment(1000, 1000, -158);
						this.helpButton.setLayoutData(LoadButtonLData);
						this.helpButton.addSelectionListener(new SelectionAdapter() {
							@Override
							public void widgetSelected(SelectionEvent evt) {
								log.log(java.util.logging.Level.FINEST, "helpButton.widgetSelected, event=" + evt); //$NON-NLS-1$
								UltraDuoPlusDialog.this.application.openHelpDialog(UltraDuoPlusDialog.DEVICE_JAR_NAME, "HelpInfo.html"); //$NON-NLS-1$ 
							}
						});
					}
					this.boundsComposite.addPaintListener(new PaintListener() {
						public void paintControl(PaintEvent evt) {
							log.log(java.util.logging.Level.FINER, "boundsComposite.paintControl() " + evt); //$NON-NLS-1$
						}
					});
				} // end boundsComposite
				this.application.setCursor(SWTResourceManager.getCursor(SWT.CURSOR_ARROW));
				this.dialogShell.setLocation(getParent().toDisplay(getParent().getSize().x / 2 - 300, 100));
				this.dialogShell.open();
				this.lastMemorySelectionIndex = -1;
				this.lastCellSelectionIndex = -1;
				updateBaseSetup();
				this.memoryCombo.notifyListeners(SWT.Selection, new Event());
			}
			else {
				this.application.setCursor(SWTResourceManager.getCursor(SWT.CURSOR_ARROW));
				this.dialogShell.setVisible(true);
				this.dialogShell.setActive();
				this.lastMemorySelectionIndex = -1;
				this.lastCellSelectionIndex = -1;
				updateBaseSetup();
				this.memoryCombo.notifyListeners(SWT.Selection, new Event());
			}
			Display display = this.dialogShell.getDisplay();
			log.log(Level.TIME, "open dialog time = " + StringHelper.getFormatedTime("ss:SSS", (new Date().getTime() - openStartTime))); //$NON-NLS-1$ //$NON-NLS-2$
			while (!this.dialogShell.isDisposed()) {
				if (!display.readAndDispatch()) display.sleep();
			}
		}
		catch (Throwable e) {
			log.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
		}
		finally {
			if (!GDE.shell.isDisposed()) {
				this.application.setCursor(SWTResourceManager.getCursor(SWT.CURSOR_ARROW));
				this.application.resetShellIcon();
				if (this.serialPort != null && this.serialPort.isConnected()) {
					try {
						this.serialPort.write(UltramatSerialPort.RESET);
					}
					catch (IOException e) {
						// ignore
					}
					finally {
						this.serialPort.close();
					}
				}
			}
		}
	}

	/**
	 * create minimal ultra duo plus XML data 
	 * @param useDeviceIdentifierName
	 */
	private void createUltraDuoPlusSetup(String useDeviceIdentifierName) {
		this.ultraDuoPlusSetup = new ObjectFactory().createUltraDuoPlusType();
		List<MemoryType> cellMemories = this.ultraDuoPlusSetup.getMemory();
		if (cellMemories.size() < UltraDuoPlusDialog.numberMemories) { // initially create only base setup data
			for (int i = 0; i < UltraDuoPlusDialog.numberMemories; i++) {
				MemoryType cellMemory = new ObjectFactory().createMemoryType();
				cellMemory.setSetupData(new ObjectFactory().createMemoryTypeSetupData());
				cellMemories.add(cellMemory);
			}
		}
		this.ultraDuoPlusSetup.setIdentifierName(useDeviceIdentifierName);
	}

	/**
	 * update basic setup data from cache or actual red
	 * @throws TimeOutException 
	 * @throws IOException 
	 * @throws SerialPortException 
	 */
	private void updateBaseSetup() throws IOException, TimeOutException, SerialPortException {
		log.log(java.util.logging.Level.FINEST, GDE.STRING_ENTRY);
		if (this.ultraDuoPlusSetup != null) {
			if (this.ultraDuoPlusSetup.getChannelData1() == null || !this.ultraDuoPlusSetup.getChannelData1().isSynced()) {
				ChannelData1 channelData1 = new ObjectFactory().createUltraDuoPlusTypeChannelData1();
				channelData1.setValue(this.serialPort.readChannelData(1));
				this.ultraDuoPlusSetup.setChannelData1(channelData1);
			}
			if (this.device.getDeviceTypeIdentifier() != GraupnerDeviceType.UltraDuoPlus45 && (this.ultraDuoPlusSetup.getChannelData2() == null || !this.ultraDuoPlusSetup.getChannelData2().isSynced())) {
				ChannelData2 channelData2 = new ObjectFactory().createUltraDuoPlusTypeChannelData2();
				channelData2.setValue(this.serialPort.readChannelData(2));
				this.ultraDuoPlusSetup.setChannelData2(channelData2);
			}
			this.device.convert2IntArray(this.channelValues1, this.ultraDuoPlusSetup.channelData1.getValue());
			if (this.device.getDeviceTypeIdentifier() != GraupnerDeviceType.UltraDuoPlus45) { //no configurable outlet channel 2
				this.device.convert2IntArray(this.channelValues2, this.ultraDuoPlusSetup.channelData2.getValue());
			}
			if (log.isLoggable(java.util.logging.Level.FINEST)) {
				StringBuffer sb = new StringBuffer();
				for (int i = 0; i < UltramatSerialPort.SIZE_CHANNEL_1_SETUP; i++) {
					sb.append(this.channelValues1[i]).append(GDE.STRING_LEFT_BRACKET).append(i).append(GDE.STRING_RIGHT_BRACKET_COMMA);
				}
				sb.append(" : ");//$NON-NLS-1$
				for (int i = 0; this.device.getDeviceTypeIdentifier() != GraupnerDeviceType.UltraDuoPlus45 && i < UltramatSerialPort.SIZE_CHANNEL_2_SETUP; i++) {
					sb.append(this.channelValues2[i]).append(GDE.STRING_LEFT_BRACKET).append(i).append(GDE.STRING_RIGHT_BRACKET_COMMA);
				}
				log.log(java.util.logging.Level.FINEST, sb.toString());
			}

			if (this.device.getDeviceTypeIdentifier() != GraupnerDeviceType.UltraDuoPlus45) {
				log.log(java.util.logging.Level.FINEST, "add handler"); //$NON-NLS-1$
				//don't need a change listener handler for baseDeviceSetupGroup and baseDeviceSetupGroup1, it will always written to sync date and time
				this.baseDeviceSetupGroup2.addListener(SWT.Selection, new Listener() {
					public void handleEvent(Event evt) {
						log.log(java.util.logging.Level.FINEST, "baseDeviceSetupComposite2.handleEvent, channelValues2[" + evt.index + "] changed"); //$NON-NLS-1$ //$NON-NLS-2$
						ChannelData2 value = new ChannelData2();
						value.setValue(StringHelper.integer2Hex4ByteString(UltraDuoPlusDialog.this.channelValues2));
						UltraDuoPlusDialog.this.ultraDuoPlusSetup.setChannelData2(value);
						UltraDuoPlusDialog.this.ultraDuoPlusSetup.getChannelData2().setChanged(true);
					}
				});
			}
		}

		for (int i = 0; i < UltramatSerialPort.SIZE_CHANNEL_1_SETUP; i++) {
			if (this.channelParameters[i] != null) {
				this.channelParameters[i].setSliderSelection(this.channelValues1[i]);
			}
		}
		for (int i = 0; this.device.getDeviceTypeIdentifier() != GraupnerDeviceType.UltraDuoPlus45 && i < UltramatSerialPort.SIZE_CHANNEL_2_SETUP; i++) {
			if (this.channelParameters[UltramatSerialPort.SIZE_CHANNEL_1_SETUP + i] != null) {
				this.channelParameters[UltramatSerialPort.SIZE_CHANNEL_1_SETUP + i].setSliderSelection(this.channelValues2[i]);
			}
		}
		log.log(java.util.logging.Level.FINEST, GDE.STRING_EXIT);
	}

	/**
	 * update values by given memory number
	 * @param memoryNumber
	 * @throws IOException
	 * @throws TimeOutException
	 */
	private void updateBatterySetup(int memoryNumber) {
		log.log(java.util.logging.Level.FINER, GDE.STRING_ENTRY + memoryNumber);
		try {
			if (this.chargeGroup != null && !this.chargeGroup.isDisposed()) {
				log.log(java.util.logging.Level.FINEST, "remove event handler"); //$NON-NLS-1$
				this.memorySelectComposite.removeListener(SWT.Selection, this.memoryParameterChangeListener);
				this.chargeGroup.removeListener(SWT.Selection, this.memoryParameterChangeListener);
				this.dischargeGroup.removeListener(SWT.Selection, this.memoryParameterChangeListener);
				this.cycleGroup.removeListener(SWT.Selection, this.memoryParameterChangeListener);
			}

			if (this.ultraDuoPlusSetup != null && this.ultraDuoPlusSetup.getMemory() != null) {
				int i = 0;
				while (i < UltraDuoPlusDialog.numberMemories) {
					if (this.ultraDuoPlusSetup.getMemory().get(i).isSynced())
						this.memoryNames[i] = String.format(UltraDuoPlusDialog.STRING_FORMAT_02d_s, i + 1, this.ultraDuoPlusSetup.getMemory().get(i).getName());
					else if (this.ultraDuoPlusSetup.getMemory().get(i) != null && this.ultraDuoPlusSetup.getMemory().get(i).getName() != null) {
						this.memoryNames[i] = String.format(UltraDuoPlusDialog.STRING_FORMAT_02d_s, i + 1, this.ultraDuoPlusSetup.getMemory().get(i).getName());
					}
					else {
						WaitTimer.delay(100);
						continue;
					}
					if (log.isLoggable(java.util.logging.Level.FINE)) log.log(java.util.logging.Level.FINE, "-------> [" + i + "] " + this.memoryNames[i]); //$NON-NLS-1$ //$NON-NLS-2$
					++i;
				}

				if (this.ultraDuoPlusSetup.getMemory().get(memoryNumber).getSetupData() != null && !this.ultraDuoPlusSetup.getMemory().get(memoryNumber + 1).getSetupData().isSynced()) {
					this.ultraDuoPlusSetup.getMemory().get(memoryNumber).getSetupData().setValue(this.serialPort.readMemorySetup(memoryNumber + 1));
				}
				this.memoryValues = this.device.convert2IntArray(this.memoryValues, this.ultraDuoPlusSetup.getMemory().get(memoryNumber).getSetupData().getValue());
			}
			else {
				for (int i = 0; i < UltraDuoPlusDialog.numberMemories; i++) {
					this.memoryNames[i] = String.format("%02d - NEW-BATT-NAME", i + 1); //$NON-NLS-1$
				}
				//some default values
				this.memoryValues[0] = 0;
				this.memoryValues[1] = 4;
				this.memoryValues[2] = 2000;
				this.memoryValues[3] = 0;
				this.memoryValues[4] = 1;
				this.memoryValues[5] = 1;
			}

			if (log.isLoggable(java.util.logging.Level.FINE)) {
				StringBuffer sb = new StringBuffer();
				for (int i = 0; i < UltramatSerialPort.SIZE_MEMORY_SETUP; i++) {
					sb.append(String.format("%04d", this.memoryValues[i])).append(GDE.STRING_LEFT_BRACKET).append(i).append(GDE.STRING_RIGHT_BRACKET_COMMA); //$NON-NLS-1$
				}
				log.log(java.util.logging.Level.FINE, sb.toString());
			}
			if (this.memoryParameters[0] != null && !this.memoryParameters[0].getSlider().isDisposed()) updateBatteryMemoryParameter(this.memoryValues[0]);

			for (int i = 0; i < UltramatSerialPort.SIZE_MEMORY_SETUP; i++) {
				if (this.memoryParameters[i] != null) {
					this.memoryParameters[i].setSliderSelection(this.memoryValues[i]);
				}
			}

			if (this.chargeGroup != null && !this.chargeGroup.isDisposed()) {
				log.log(java.util.logging.Level.FINEST, "add event handler"); //$NON-NLS-1$
				this.memorySelectComposite.addListener(SWT.Selection, this.memoryParameterChangeListener);
				this.chargeGroup.addListener(SWT.Selection, this.memoryParameterChangeListener);
				this.dischargeGroup.addListener(SWT.Selection, this.memoryParameterChangeListener);
				this.cycleGroup.addListener(SWT.Selection, this.memoryParameterChangeListener);
			}
		}
		catch (Throwable e) {
			log.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
		}
		log.log(java.util.logging.Level.FINER, GDE.STRING_EXIT);
	}

	/**
	 * update the memory setup parameter list to cell type dependent
	 */
	private void updateBatteryMemoryParameter(int selectionIndex) {
		log.log(java.util.logging.Level.FINEST, GDE.STRING_ENTRY);
		if (this.lastCellSelectionIndex != selectionIndex) {
			log.log(java.util.logging.Level.FINE, "cell type changed to : " + UltraDuoPlusDialog.cellTypeNames[this.memoryValues[0]]); //$NON-NLS-1$
			this.memoryValues[0] = selectionIndex;
			//update memory parameter table to reflect not edit able parameters for selected cell type
			switch (this.memoryValues[0]) {
			case 0: //NiCd
			case 1: //NiMh
				this.memoryParameters[9] = this.memoryParameters[9] == null ? new ParameterConfigControl(this.chargeGroup, this.memoryValues, 9, GDE.STRING_EMPTY, Messages.getString(MessageIds.GDE_MSGT2279),
						175, Messages.getString(MessageIds.GDE_MSGT2312), 220, false, 50, 150, 0, 550) : this.memoryParameters[9];
				this.memoryParameters[7] = this.memoryParameters[7] == null ? new ParameterConfigControl(this.chargeGroup, this.memoryValues, 7, GDE.STRING_EMPTY, Messages.getString(MessageIds.GDE_MSGT2277),
						175, "0 ~ 25mV", 220, false, 50, 150, 0, 25) : this.memoryParameters[7]; //$NON-NLS-1$ 
				this.memoryParameters[8] = this.memoryParameters[8] == null ? new ParameterConfigControl(this.chargeGroup, this.memoryValues, 8, GDE.STRING_EMPTY, Messages.getString(MessageIds.GDE_MSGT2278),
						175, "1 ~ 20min", 220, false, 50, 150, 1, 20) : this.memoryParameters[8]; //$NON-NLS-1$ 
				this.memoryParameters[13] = this.memoryParameters[13] == null ? new ParameterConfigControl(this.chargeGroup, this.memoryValues, 13, GDE.STRING_EMPTY,
						Messages.getString(MessageIds.GDE_MSGT2280), 175, "1 ~ 5", 220, false, 50, 150, 1, 5) : this.memoryParameters[13]; //$NON-NLS-1$ 
				this.memoryParameters[14] = this.memoryParameters[14] != null ? this.memoryParameters[14].dispose() : null;
				this.memoryParameters[15] = this.memoryParameters[15] == null ? new ParameterConfigControl(this.chargeGroup, this.memoryValues, 15, GDE.STRING_EMPTY,
						Messages.getString(MessageIds.GDE_MSGT2281), 175, "1 ~ 30min", 220, false, 50, 150, 1, 30) : this.memoryParameters[15]; //$NON-NLS-1$ 
				this.memoryParameters[16] = this.memoryParameters[16] == null ? new ParameterConfigControl(this.chargeGroup, this.memoryValues, 16, Messages.getString(MessageIds.GDE_MSGT2282), 175,
						Messages.getString(MessageIds.GDE_MSGT2241) + GDE.STRING_MESSAGE_CONCAT + Messages.getString(MessageIds.GDE_MSGT2240), 220, UltraDuoPlusDialog.offOnType, 50, 150)
						: this.memoryParameters[16];
				this.memoryParameters[21] = this.memoryParameters[21] == null ? new ParameterConfigControl(this.dischargeGroup, this.memoryValues, 21, GDE.STRING_EMPTY,
						Messages.getString(MessageIds.GDE_MSGT2283), 175, "1100 ~ 1300 mV", 220, true, 50, 150, 1100, 1300, -1100) : this.memoryParameters[21]; //$NON-NLS-1$ 
				this.memoryParameters[26] = this.memoryParameters[26] != null ? this.memoryParameters[26].dispose() : null;
				this.chargeSelectHeight = 10 * this.parameterSelectHeight;
				this.dischargeSelectHeight = 5 * this.parameterSelectHeight;
				this.memoryParameters[11].updateValueRange(Messages.getString(MessageIds.GDE_MSGT2310), 10, 155);
				break;
			case 2: //LiIo
			case 3: //LiPo
			case 4: //LiFe
				this.memoryParameters[7] = this.memoryParameters[7] != null ? this.memoryParameters[7].dispose() : null;
				this.memoryParameters[8] = this.memoryParameters[8] != null ? this.memoryParameters[8].dispose() : null;
				this.memoryParameters[9] = this.memoryParameters[9] != null ? this.memoryParameters[9].dispose() : null;
				this.memoryParameters[13] = this.memoryParameters[13] != null ? this.memoryParameters[13].dispose() : null;
				this.memoryParameters[14] = this.memoryParameters[14] == null ? new ParameterConfigControl(this.chargeGroup, this.memoryValues, 14, GDE.STRING_EMPTY,
						Messages.getString(MessageIds.GDE_MSGT2267), 175, "1000 ~ 4300 mV", 220, true, 50, 150, 1000, 4300, -1000) : this.memoryParameters[14]; //$NON-NLS-1$ 
				this.memoryParameters[15] = this.memoryParameters[15] != null ? this.memoryParameters[15].dispose() : null;
				this.memoryParameters[16] = this.memoryParameters[16] != null ? this.memoryParameters[16].dispose() : null;
				this.memoryParameters[21] = this.memoryParameters[21] != null ? this.memoryParameters[21].dispose() : null;
				this.memoryParameters[26] = this.memoryParameters[26] == null ? new ParameterConfigControl(this.chargeGroup, this.memoryValues, 26, GDE.STRING_EMPTY,
						Messages.getString(MessageIds.GDE_MSGT2276), 175, "1000 ~ 4000", 220, true, 50, 150, 1000, 4000, -1000) : this.memoryParameters[26]; //$NON-NLS-1$ 
				this.chargeSelectHeight = 6 * this.parameterSelectHeight + 10;
				this.dischargeSelectHeight = 4 * this.parameterSelectHeight;
				this.memoryParameters[11].updateValueRange(Messages.getString(MessageIds.GDE_MSGT2314), 10, 125);
				break;
			case 5: //Pb
				this.memoryParameters[7] = this.memoryParameters[7] != null ? this.memoryParameters[7].dispose() : null;
				this.memoryParameters[8] = this.memoryParameters[8] != null ? this.memoryParameters[8].dispose() : null;
				this.memoryParameters[9] = this.memoryParameters[9] != null ? this.memoryParameters[9].dispose() : null;
				this.memoryParameters[13] = this.memoryParameters[13] != null ? this.memoryParameters[13].dispose() : null;
				this.memoryParameters[14] = this.memoryParameters[14] == null ? new ParameterConfigControl(this.chargeGroup, this.memoryValues, 14, GDE.STRING_EMPTY,
						Messages.getString(MessageIds.GDE_MSGT2267), 175, "1000 ~ 4300 mV", 220, true, 50, 150, 1000, 4300, -1000) : this.memoryParameters[14]; //$NON-NLS-1$ 
				this.memoryParameters[15] = this.memoryParameters[15] == null ? new ParameterConfigControl(this.chargeGroup, this.memoryValues, 15, GDE.STRING_EMPTY,
						Messages.getString(MessageIds.GDE_MSGT2281), 175, "1 ~ 30min", 220, false, 50, 150, 1, 30) : this.memoryParameters[15]; //$NON-NLS-1$ 
				this.memoryParameters[16] = this.memoryParameters[16] != null ? this.memoryParameters[16].dispose() : null;
				this.memoryParameters[21] = this.memoryParameters[21] != null ? this.memoryParameters[21].dispose() : null;
				this.memoryParameters[26] = this.memoryParameters[26] != null ? this.memoryParameters[26].dispose() : null;
				this.chargeSelectHeight = 6 * this.parameterSelectHeight + 10;
				this.dischargeSelectHeight = 4 * this.parameterSelectHeight;
				this.memoryParameters[11].updateValueRange(Messages.getString(MessageIds.GDE_MSGT2310), 10, 155);
				break;
			}
			this.chargeGroup.setSize(this.scrolledchargeComposite.getClientArea().width, this.chargeSelectHeight);
			this.chargeGroup.layout(true);
			this.scrolledchargeComposite.layout(true);
			this.dischargeGroup.setLayoutData(new RowData(this.dischargeCycleComposite.getClientArea().width-18, this.dischargeSelectHeight));
			this.dischargeGroup.layout(true);
			this.dischargeCycleComposite.layout(true);

			//updateBatteryParameterValues();
			this.lastCellSelectionIndex = this.memoryValues[0];
		}
		log.log(java.util.logging.Level.FINEST, GDE.STRING_EXIT);
	}

	/**
	 * update the memory setup parameter values with dependency to cell type, capacity, charge current
	 * set meaningful initial values
	 */
	private void updateBatteryParameterValues() {
		log.log(java.util.logging.Level.FINEST, GDE.STRING_ENTRY);
		//0=cellType,1=numCells,2=capacity,3=year,4=month,5=day,
		//6=chargeCurrent,7=deltaPeak,8=preDeltaPeakDelay,9=trickleCurrent,10=chargeOffTemperature,11=chargeMaxCapacity,12=chargeSafetyTimer,13=rePeakCycle,14=chargeVoltage,15=repaekDelay,16=flatLimitCheck,26=storeVoltage
		//17=dischargeCurrent,18=dischargOffVolage,19=dischargeOffTemp,20=dischargemaxCapacity,21=NiMhMatchVoltage
		//22=cycleDirection,23=cycleCount,24=chargeEndDelay,25=dischargeEndDelay
		this.memoryValues[6] = this.memoryValues[2] / 2; //charge current 0.5 C
		this.memoryValues[17] = this.memoryValues[2]; //discharge current 1.0 C
		this.memoryValues[12] = this.memoryValues[2] / 30; //chargeSafetyTimer
		this.memoryValues[12] = this.memoryValues[12] - (this.memoryValues[12] % 10); //chargeSafetyTimer
		this.memoryValues[20] = 100; //dischargemaxCapacity
		this.memoryValues[22] = 1; //cycleDirection D->C
		this.memoryValues[23] = 1; //cycleCount
		this.memoryValues[24] = this.memoryValues[25] = (int) ((double) this.memoryValues[2] / 440 + 0.5); //chargeEndDelay, dischargeEndDelay
		switch (this.memoryValues[0]) {
		case 0: //NiCd
			this.memoryValues[11] = 120; //chargeMaxCapacity
			this.memoryValues[10] = (this.device.getDeviceTypeIdentifier() != GraupnerDeviceType.UltraDuoPlus45 ? this.channelValues1[4] == 0 : this.channelValues1[0] == 0) ? 50 : (9 / 5) * 50 + 32; //chargeOffTemperature
			this.memoryValues[12] = this.memoryValues[12] + 30; //chargeSafetyTimer
			this.memoryValues[9] = this.memoryValues[2] / 10; //trickleCurrent
			this.memoryValues[7] = 7; //deltaPeak mV
			this.memoryValues[8] = 3; //preDeltaPeakDelay min
			this.memoryValues[13] = 1; //rePeakCycle
			this.memoryValues[15] = 3; //rePaekDelay min
			this.memoryValues[7] = 1; //flatLimitCheck
			this.memoryValues[18] = 1000; //dischargeVoltage/cell
			this.memoryValues[19] = (this.device.getDeviceTypeIdentifier() != GraupnerDeviceType.UltraDuoPlus45 ? this.channelValues1[4] == 0 : this.channelValues1[0] == 0) ? 45 : (9 / 5) * 45 + 32; //dischargeOffTemperature
			break;
		case 1: //NiMh
			this.memoryValues[11] = 120; //chargeMaxCapacity
			this.memoryValues[10] = (this.device.getDeviceTypeIdentifier() != GraupnerDeviceType.UltraDuoPlus45 ? this.channelValues1[4] == 0 : this.channelValues1[0] == 0) ? 50 : (9 / 5) * 50 + 32; //chargeOffTemperature
			this.memoryValues[12] = this.memoryValues[12] + 30; //chargeSafetyTimer
			this.memoryValues[9] = this.memoryValues[2] / 10; //trickleCurrent
			this.memoryValues[7] = 5; //deltaPeak mV
			this.memoryValues[8] = 3; //preDeltaPeakDelay min
			this.memoryValues[13] = 1; //rePeakCycle
			this.memoryValues[15] = 3; //rePaekDelay min
			this.memoryValues[7] = 1; //flatLimitCheck
			this.memoryValues[18] = 1000; //dischargeVoltage/cell
			this.memoryValues[19] = (this.device.getDeviceTypeIdentifier() != GraupnerDeviceType.UltraDuoPlus45 ? this.channelValues1[4] == 0 : this.channelValues1[0] == 0) ? 45 : (9 / 5) * 45 + 32; //dischargeOffTemperature
			this.memoryValues[21] = 1250; //NiMhMatchVoltage
			break;
		case 2: //LiIo
			this.memoryValues[11] = 105; //chargeMaxCapacity
			this.memoryValues[10] = (this.device.getDeviceTypeIdentifier() != GraupnerDeviceType.UltraDuoPlus45 ? this.channelValues1[4] == 0 : this.channelValues1[0] == 0) ? 45 : (9 / 5) * 45 + 32; //chargeOffTemperature
			this.memoryValues[12] = this.memoryValues[12] + 60; //chargeSafetyTimer
			this.memoryValues[9] = 0; //trickleCurrent
			this.memoryValues[14] = 4100; //chargeVoltage/cell
			this.memoryValues[26] = 3900; //storeVoltage/cell
			this.memoryValues[18] = 3000; //dischargeVoltage/cell
			this.memoryValues[19] = (this.device.getDeviceTypeIdentifier() != GraupnerDeviceType.UltraDuoPlus45 ? this.channelValues1[4] == 0 : this.channelValues1[0] == 0) ? 40 : (9 / 5) * 40 + 32; //dischargeOffTemperature
			break;
		case 3: //LiPo
			this.memoryValues[11] = 105; //chargeMaxCapacity
			this.memoryValues[10] = (this.device.getDeviceTypeIdentifier() != GraupnerDeviceType.UltraDuoPlus45 ? this.channelValues1[4] == 0 : this.channelValues1[0] == 0) ? 45 : (9 / 5) * 45 + 32; //chargeOffTemperature
			this.memoryValues[12] = this.memoryValues[12] + 60; //chargeSafetyTimer
			this.memoryValues[9] = 0; //trickleCurrent
			this.memoryValues[14] = 4200; //chargeVoltage/cell
			this.memoryValues[26] = 4000; //storeVoltage/cell
			this.memoryValues[18] = 3000; //dischargeVoltage/cell
			this.memoryValues[19] = (this.device.getDeviceTypeIdentifier() != GraupnerDeviceType.UltraDuoPlus45 ? this.channelValues1[4] == 0 : this.channelValues1[0] == 0) ? 40 : (9 / 5) * 40 + 32; //dischargeOffTemperature
			break;
		case 4: //LiFe
			this.memoryValues[11] = 125; //chargeMaxCapacity
			this.memoryValues[10] = (this.device.getDeviceTypeIdentifier() != GraupnerDeviceType.UltraDuoPlus45 ? this.channelValues1[4] == 0 : this.channelValues1[0] == 0) ? 45 : (9 / 5) * 45 + 32; //chargeOffTemperature
			this.memoryValues[12] = this.memoryValues[12] + 60; //chargeSafetyTimer
			this.memoryValues[9] = 0; //trickleCurrent
			this.memoryValues[14] = 3600; //chargeVoltage/cell
			this.memoryValues[26] = 3500; //storeVoltage/cell
			this.memoryValues[18] = 2500; //dischargeVoltage/cell
			this.memoryValues[19] = (this.device.getDeviceTypeIdentifier() != GraupnerDeviceType.UltraDuoPlus45 ? this.channelValues1[4] == 0 : this.channelValues1[0] == 0) ? 40 : (9 / 5) * 40 + 32; //dischargeOffTemperature
			break;
		case 5: //PB
			this.memoryValues[6] = this.memoryValues[2] / 10; //charge current 0.1 C
			this.memoryValues[11] = 155; //chargeMaxCapacity
			this.memoryValues[10] = (this.device.getDeviceTypeIdentifier() != GraupnerDeviceType.UltraDuoPlus45 ? this.channelValues1[4] == 0 : this.channelValues1[0] == 0) ? 45 : (9 / 5) * 45 + 32; //chargeOffTemperature
			this.memoryValues[12] = 905; //chargeSafetyTimer
			this.memoryValues[9] = this.memoryValues[2] / 10; //trickleCurrent
			this.memoryValues[14] = 2300; //chargeVoltage/cell
			this.memoryValues[18] = 1800; //dischargeVoltage/cell
			this.memoryValues[19] = (this.device.getDeviceTypeIdentifier() != GraupnerDeviceType.UltraDuoPlus45 ? this.channelValues1[4] == 0 : this.channelValues1[0] == 0) ? 50 : (9 / 5) * 50 + 32; //dischargeOffTemperature
			break;
		}
		this.memoryValues[12] = this.memoryValues[12] > 30 ? 30 : this.memoryValues[12];
		//update parameter controls
		for (int i = 0; i < UltramatSerialPort.SIZE_MEMORY_SETUP; i++) {
			if (this.memoryParameters[i] != null) {
				this.memoryParameters[i].setSliderSelection(this.memoryValues[i]);
			}
		}
		if (log.isLoggable(java.util.logging.Level.FINER)) {
			StringBuffer sb = new StringBuffer();
			for (int i = 0; i < UltramatSerialPort.SIZE_MEMORY_SETUP; i++) {
				sb.append(String.format("%04d", this.memoryValues[i])).append(GDE.STRING_LEFT_BRACKET).append(i).append(GDE.STRING_RIGHT_BRACKET_COMMA); //$NON-NLS-1$
			}
			log.log(java.util.logging.Level.FINER, sb.toString());
		}
	}

	/**
	 * save ultra duo plus configuration data at given full qualified file path
	 * @param fullQualifiedFilePath
	 */
	private void saveConfigUDP(String fullQualifiedFilePath) {
		try {
			long startTime = new Date().getTime();
			if (this.ultraDuoPlusSetup != null) {
				new UltraDuoPlusSychronizer(this, this.serialPort, this.ultraDuoPlusSetup, SYNC_TYPE.WRITE).run(); //call run() instead of start()and join()

				this.ultraDuoPlusSetup.changed = null;
				//remove synchronized flag
				this.ultraDuoPlusSetup.getChannelData1().synced = null;
				if (this.device.getDeviceTypeIdentifier() != GraupnerDeviceType.UltraDuoPlus45) {
					this.ultraDuoPlusSetup.getChannelData2().synced = null;
				}
				Iterator<MemoryType> iterator = this.ultraDuoPlusSetup.getMemory().iterator();
				while (iterator.hasNext()) {
					MemoryType cellMemory = iterator.next();
					cellMemory.synced = null;
					cellMemory.changed = null;
					cellMemory.getSetupData().synced = null;
					cellMemory.getSetupData().changed = null;
					if (cellMemory.getStepChargeData() != null) {
						cellMemory.getStepChargeData().synced = null;
						cellMemory.getStepChargeData().changed = null;
					}
					if (cellMemory.getTraceData() != null) {
						cellMemory.getTraceData().synced = null;
						cellMemory.getTraceData().changed = null;
					}
					if (cellMemory.getCycleData() != null) {
						cellMemory.getCycleData().synced = null;
						cellMemory.getCycleData().changed = null;
					}
				}
				Iterator<TireHeaterData> tireIterator = this.ultraDuoPlusSetup.getTireHeaterData().iterator();
				while (tireIterator.hasNext()) {
					TireHeaterData tireHeater = tireIterator.next();
					tireHeater.synced = null;
					tireHeater.changed = null;
				}
				Iterator<MotorRunData> motorIterator = this.ultraDuoPlusSetup.getMotorRunData().iterator();
				while (motorIterator.hasNext()) {
					MotorRunData motorRunData = motorIterator.next();
					motorRunData.synced = null;
					motorRunData.changed = null;
				}

				// store back manipulated XML
				Marshaller marshaller = this.jc.createMarshaller();
				marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.valueOf(true));
				marshaller.setProperty(Marshaller.JAXB_NO_NAMESPACE_SCHEMA_LOCATION, UltraDuoPlusDialog.ULTRA_DUO_PLUS_XSD);
				marshaller.marshal(this.ultraDuoPlusSetup, new FileOutputStream(fullQualifiedFilePath));
				log.log(Level.TIME, "write memory setup XML time = " + StringHelper.getFormatedTime("ss:SSS", (new Date().getTime() - startTime))); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		catch (Throwable e) {
			log.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
			this.application.openMessageDialogAsync(this.dialogShell != null && !this.dialogShell.isDisposed() ? this.dialogShell : null,
					Messages.getString(gde.messages.MessageIds.GDE_MSGE0007, new String[] { e.getMessage() }));
		}
	}

	/**
	 * enable or disable backup and restore buttons, disable while synchronizing setup configuration with device
	 * @param enable
	 */
	public void setBackupRetoreButtons(final boolean enable) {
		if (Thread.currentThread().getId() == this.application.getThreadId()) {
			if (!UltraDuoPlusDialog.this.dialogShell.isDisposed() && UltraDuoPlusDialog.this.restoreButton != null && UltraDuoPlusDialog.this.backupButton != null
					&& !UltraDuoPlusDialog.this.restoreButton.isDisposed() && !UltraDuoPlusDialog.this.backupButton.isDisposed()) {
				this.restoreButton.setEnabled(enable);
				this.backupButton.setEnabled(enable);
				this.closeButton.setEnabled(enable);
			}
		}
		else {
			GDE.display.asyncExec(new Runnable() {
				public void run() {
					if (!UltraDuoPlusDialog.this.dialogShell.isDisposed() && UltraDuoPlusDialog.this.restoreButton != null && UltraDuoPlusDialog.this.backupButton != null
							&& !UltraDuoPlusDialog.this.restoreButton.isDisposed() && !UltraDuoPlusDialog.this.backupButton.isDisposed()) {
						UltraDuoPlusDialog.this.restoreButton.setEnabled(enable);
						UltraDuoPlusDialog.this.backupButton.setEnabled(enable);
						UltraDuoPlusDialog.this.closeButton.setEnabled(enable);
					}
				}
			});
		}
	}

	/**
	 * deep copy of complete ultraDuoPlusSetup data
	 * @param tmpUltraDuoPlusSetup
	 */
	private void mergeUltraDuoPlusSetup(UltraDuoPlusType tmpUltraDuoPlusSetup) {
		if (!this.ultraDuoPlusSetup.getIdentifierName().equals(tmpUltraDuoPlusSetup.getIdentifierName())) {
			this.deviceIdentifierName = (tmpUltraDuoPlusSetup.getIdentifierName() + UltraDuoPlusDialog.STRING_16_BLANK).substring(0, 16);
			this.ultraDuoPlusSetup.setIdentifierName(this.deviceIdentifierName);
			this.ultraDuoPlusSetup.setChanged(true);
			this.userNameText.setText(this.deviceIdentifierName);
		}

		//channel base setup
		if (!this.ultraDuoPlusSetup.getChannelData1().getValue().equals(tmpUltraDuoPlusSetup.getChannelData1().getValue())) {
			this.ultraDuoPlusSetup.getChannelData1().setValue(tmpUltraDuoPlusSetup.getChannelData1().getValue());
			this.ultraDuoPlusSetup.getChannelData1().setChanged(true);
			this.device.convert2IntArray(this.channelValues1, this.ultraDuoPlusSetup.channelData1.getValue());
			for (int i = 0; i < UltramatSerialPort.SIZE_CHANNEL_1_SETUP; i++) {
				if (this.channelParameters[i] != null) {
					this.channelParameters[i].setSliderSelection(this.channelValues1[i]);
				}
			}
		}
		if (this.device.getDeviceTypeIdentifier() != GraupnerDeviceType.UltraDuoPlus45 && !this.ultraDuoPlusSetup.getChannelData2().getValue().equals(tmpUltraDuoPlusSetup.getChannelData2().getValue())) {
			this.ultraDuoPlusSetup.getChannelData2().setValue(tmpUltraDuoPlusSetup.getChannelData2().getValue());
			this.ultraDuoPlusSetup.getChannelData2().setChanged(true);
			this.device.convert2IntArray(this.channelValues2, this.ultraDuoPlusSetup.channelData2.getValue());
			for (int i = 0; i < UltramatSerialPort.SIZE_CHANNEL_2_SETUP; i++) {
				if (this.channelParameters[UltramatSerialPort.SIZE_CHANNEL_2_SETUP + i] != null) {
					this.channelParameters[UltramatSerialPort.SIZE_CHANNEL_2_SETUP + i].setSliderSelection(this.channelValues2[i]);
				}
			}
		}

		//battery memories
		List<MemoryType> cellMemories = this.ultraDuoPlusSetup.getMemory();
		List<MemoryType> tmpCellMemories = tmpUltraDuoPlusSetup.getMemory();
		Iterator<MemoryType> iterator = cellMemories.iterator();
		Iterator<MemoryType> tmpIterator = tmpCellMemories.iterator();
		while (iterator.hasNext()) {
			MemoryType cellMemory = iterator.next();
			MemoryType tmpCellMemory = tmpIterator.next();
			//memory name
			if (!cellMemory.getName().equals(tmpCellMemory.getName())) {
				cellMemory.setName((tmpCellMemory.getName() + UltraDuoPlusDialog.STRING_16_BLANK).substring(0, 16));
				cellMemory.setChanged(true);
			}

			//memory setup data
			if (tmpCellMemory.getSetupData() != null && cellMemory.getSetupData() != null && !cellMemory.getSetupData().getValue().equals(tmpCellMemory.getSetupData().getValue())) {
				cellMemory.getSetupData().setValue(tmpCellMemory.getSetupData().getValue());
				cellMemory.getSetupData().setChanged(true);
			}
			//memory step charge data
			if (tmpCellMemory.getStepChargeData() == null && cellMemory.getStepChargeData() != null) {
				cellMemory.setTraceData(null);
			}
			if (tmpCellMemory.getStepChargeData() != null && cellMemory.getStepChargeData() == null) {
				cellMemory.setStepChargeData(new ObjectFactory().createMemoryTypeStepChargeData());
				cellMemory.getStepChargeData().setValue(tmpCellMemory.getStepChargeData().getValue());
				cellMemory.getStepChargeData().setChanged(true);
			}
			if (tmpCellMemory.getStepChargeData() != null && cellMemory.getStepChargeData() != null && !cellMemory.getStepChargeData().getValue().equals(tmpCellMemory.getStepChargeData().getValue())) {
				cellMemory.getStepChargeData().setValue(tmpCellMemory.getStepChargeData().getValue());
				cellMemory.getStepChargeData().setChanged(true);
			}

			//memory trace data
			if (tmpCellMemory.getTraceData() == null && cellMemory.getTraceData() != null) {
				cellMemory.setTraceData(null);
			}
			if (tmpCellMemory.getTraceData() != null && cellMemory.getTraceData() == null) {
				cellMemory.setTraceData(new ObjectFactory().createMemoryTypeTraceData());
				cellMemory.getTraceData().setValue(tmpCellMemory.getTraceData().getValue());
				cellMemory.getTraceData().setChanged(true);
			}
			if (tmpCellMemory.getTraceData() != null && cellMemory.getTraceData() != null && !cellMemory.getTraceData().getValue().equals(tmpCellMemory.getTraceData().getValue())) {
				cellMemory.getTraceData().setValue(tmpCellMemory.getTraceData().getValue());
				cellMemory.getTraceData().setChanged(true);
			}

			//memory cycle data
			if (tmpCellMemory.getCycleData() == null && cellMemory.getCycleData() != null) {
				cellMemory.setCycleData(null);
			}
			if (tmpCellMemory.getCycleData() != null && cellMemory.getCycleData() == null) {
				cellMemory.setCycleData(new ObjectFactory().createMemoryTypeCycleData());
				cellMemory.getCycleData().setValue(tmpCellMemory.getCycleData().getValue());
				cellMemory.getCycleData().setChanged(true);
			}
			if (tmpCellMemory.getCycleData() != null && cellMemory.getCycleData() != null && !cellMemory.getCycleData().getValue().equals(tmpCellMemory.getCycleData().getValue())) {
				cellMemory.getCycleData().setValue(tmpCellMemory.getCycleData().getValue());
				cellMemory.getCycleData().setChanged(true);
			}
		}

		//tire heater data
		List<TireHeaterData> tireHeaters = this.ultraDuoPlusSetup.getTireHeaterData();
		List<TireHeaterData> tmpTireHeaters = tmpUltraDuoPlusSetup.getTireHeaterData();
		Iterator<TireHeaterData> tireIterator = tireHeaters.iterator();
		Iterator<TireHeaterData> tmpTireIterator = tmpTireHeaters.iterator();
		while (iterator.hasNext()) {
			TireHeaterData tireHeaterData = tireIterator.next();
			TireHeaterData tmpTireHeaterData = tmpTireIterator.next();

			if (tmpTireHeaterData == null && tireHeaterData != null) {
				tireHeaterData = null;
			}
			if (tmpTireHeaterData != null && tireHeaterData == null) {
				tireHeaterData = new ObjectFactory().createUltraDuoPlusTypeTireHeaterData();
				tireHeaterData.setValue(tmpTireHeaterData.getValue());
				tireHeaterData.setChanged(true);
			}
			if (tmpTireHeaterData != null && tireHeaterData != null && !tireHeaterData.getValue().equals(tmpTireHeaterData.getValue())) {
				tireHeaterData.setValue(tmpTireHeaterData.getValue());
				tireHeaterData.setChanged(true);
			}
		}

		//motor run data
		List<MotorRunData> motorRunDatas = this.ultraDuoPlusSetup.getMotorRunData();
		List<MotorRunData> tmpMotorRunDatas = tmpUltraDuoPlusSetup.getMotorRunData();
		Iterator<MotorRunData> motorRunIterator = motorRunDatas.iterator();
		Iterator<MotorRunData> tmpMotorRunIterator = tmpMotorRunDatas.iterator();
		while (iterator.hasNext()) {
			MotorRunData motorRunData = motorRunIterator.next();
			MotorRunData tmpMotorRunData = tmpMotorRunIterator.next();

			if (tmpMotorRunData == null && motorRunData != null) {
				motorRunData = null;
			}
			if (tmpMotorRunData != null && motorRunData == null) {
				motorRunData = new ObjectFactory().createUltraDuoPlusTypeMotorRunData();
				motorRunData.setValue(tmpMotorRunData.getValue());
				motorRunData.setChanged(true);
			}
			if (tmpMotorRunData != null && motorRunData != null && !motorRunData.getValue().equals(tmpMotorRunData.getValue())) {
				motorRunData.setValue(tmpMotorRunData.getValue());
				motorRunData.setChanged(true);
			}
		}
	}

	/**
	 * set the percentage value of the progress reading last available graphics data from device
	 * @param percentage
	 */
	public void setGraphicsDataReadProgress(final int percentage) {
		this.graphicsDataProgressBar.setSelection(percentage);
	}
}
