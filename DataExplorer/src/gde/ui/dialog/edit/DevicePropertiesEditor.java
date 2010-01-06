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
package osde.ui.dialog.edit;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.bind.JAXBException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabFolder2Adapter;
import org.eclipse.swt.custom.CTabFolderEvent;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.xml.sax.SAXParseException;

import osde.OSDE;
import osde.config.Settings;
import osde.device.CheckSumTypes;
import osde.device.CommaSeparatorTypes;
import osde.device.DataTypes;
import osde.device.DesktopPropertyTypes;
import osde.device.DeviceConfiguration;
import osde.device.DeviceTypes;
import osde.device.FormatTypes;
import osde.device.IDevice;
import osde.device.LineEndingTypes;
import osde.device.MeasurementPropertyTypes;
import osde.device.ObjectFactory;
import osde.device.PropertyType;
import osde.log.LogFormatter;
import osde.messages.MessageIds;
import osde.messages.Messages;
import osde.ui.OpenSerialDataExplorer;
import osde.ui.SWTResourceManager;
import osde.utils.FileUtils;
import osde.utils.StringHelper;

/**
 * Dialog class enable to edit existing and create new device property files 
 * @author Winfried Brügmann
 */
public class DevicePropertiesEditor extends Composite {
	final static Logger					log												= Logger.getLogger(DevicePropertiesEditor.class.getName());
	public static Shell 				dialogShell;										

	public final static List<String>			onExitRenameJar						= new ArrayList<String>();

	private static DevicePropertiesEditor devicePropsEditor = null;

	CTabFolder									tabFolder;
	Label												devicePropFileNamelabel;
	Text												deviceFileNameText;
	Button											deviceFileNameSelectionButton, saveButton, closeButton;

	CTabItem										deviceTabItem;
	Menu												popupMenu;
	ContextMenu									contextMenu;
	String 											lastTabItemName; // to dispose menu widget
	Composite										deviceComposite;
	Label												deviceDescriptionlabel;
	Label												deviceNameLabel, manufacturerLabel, manufURLabel, imageFileNameLabel, usageLabel, groupLabel;
	Text												nameText, manufacturerText, manufURLText, imageFileNameText;
	Button											usageButton, fileSelectButton;
	CCombo											groupSelectionCombo;
	Composite										deviceLabelComposite, devicePropsComposite;

	String											devicePropertiesFileName	= OSDE.STRING_EMPTY;
	String											deviceName								= OSDE.STRING_EMPTY;
	String											manufacuturerURL					= OSDE.STRING_EMPTY;
	String											imageFileName							= OSDE.STRING_EMPTY;
	String											manufacturer							= OSDE.STRING_EMPTY;
	boolean											isDeviceUsed							= false;
	DeviceTypes									deviceGroup								= DeviceTypes.LOGGER;

	SeriaPortTypeTabItem				serialPortTabItem;

	CTabItem										timeBaseTabItem;
	Composite										timeBaseComposite;
	Label												timeBaseDescriptionLabel;
	Label												timeBaseNameLabel, timeBaseSymbolLabel, timeBaseUnitLabel, timeBaseTimeStepLabel;
	Text												timeBaseNameText, timeBaseSymbolText, timeBaseUnitText, timeBaseTimeStepText;

	double											timeStep_ms								= 0.0;

	CTabItem										dataBlockTabItem;
	Composite										dataBlockComposite;
	Label												dataBlockDescriptionLabel;
	Label												dataBlockFormatLabel, dataBlockSizeLabel, dataBlockSeparatorLabel, dataBlockCheckSumTypeLabel;
	Button 											dataBlockEndingButton, dataBlockCheckSumFormatButton, preferredDataLocationButton, preferredFileExtensionButton;
	CCombo											dataBlockEndingCombo, dataBlockFormatCombo, dataBlockcheckSumFormatCombo, dataBlockCheckSumTypeCombo, dataBlockSeparatorCombo;
	Text												dataBlockSizeText, preferredDataLocationText, preferredFileExtensionText;
	Group												dataBlockRequiredGroup, dataBlockOptionalGroup;

	FormatTypes									dataBlockFormat						= FormatTypes.BINARY, dataBlockcheckSumFormat = FormatTypes.BINARY;
	int													dataBlockSize							= 30;
	CommaSeparatorTypes					dataBlockSeparator 	= CommaSeparatorTypes.SEMICOLON;
	CheckSumTypes								dataBlockCheckSumType			= CheckSumTypes.ADD;
	String											dataBlockEnding						= "0a0d"; //$NON-NLS-1$
	boolean											isDataBlockOptionalChecksumEnabled = false;
	boolean											isDataBlockOptionalEndingEnabled = false;
	boolean											isDataBlockOptionalDataLocationEnabled = false;
	String											dataBlockOptionalDataLocation = OSDE.STRING_EMPTY;
	boolean											isDataBlockOptionalFileExtentionEnabled = false;
	String											dataBlockOptionalFileExtention = OSDE.FILE_ENDING_STAR_CSV;

	CTabItem										stateTabItem;
	Composite										stateComposite;
	Label												stateDescriptionLabel;
	Button											addButton;
	CTabFolder									stateTabFolder;
	PropertyTypeTabItem					modeStateItemComposite;

	CTabItem										channelConfigurationTabItem;
	Composite										channelConfigComposite;
	Label												channelConfigDescriptionLabel;
	CTabFolder									channelConfigInnerTabFolder;

	CTabItem										destopTabItem;
	PropertyTypeTabItem					desktopInnerTabItem1, desktopInnerTabItem2, desktopInnerTabItem3, desktopInnerTabItem4;
	Composite										desktopComposite;
	Label												desktopDescriptionLabel;
	CTabFolder									desktopTabFolder;

	//cross over fields
	DeviceConfiguration					deviceConfig;
	final Settings							settings;
	
	public static DevicePropertiesEditor getInstance() {
		if (devicePropsEditor == null) {
			dialogShell = new Shell(Display.getDefault(), SWT.DIALOG_TRIM | SWT.MIN | SWT.APPLICATION_MODAL);
			devicePropsEditor = new DevicePropertiesEditor(dialogShell, SWT.NONE);
		}
		else if (devicePropsEditor.isDisposed()) {
			dialogShell = new Shell(Display.getDefault(), SWT.DIALOG_TRIM | SWT.MIN | SWT.APPLICATION_MODAL);
			devicePropsEditor = new DevicePropertiesEditor(dialogShell, SWT.NONE);
		}
			
		return devicePropsEditor;
	}

	private DevicePropertiesEditor(Shell parent, int style) {
		super(parent, style);
		this.settings = Settings.getInstance();
	}

	/**
	 * main method to display this dialog inside a shell
	 */
	public static void main(String[] args) {
		try {
			Display display = Display.getDefault();
			Rectangle displayBounds = display.getBounds();
			DevicePropertiesEditor devicePropsEditor = DevicePropertiesEditor.getInstance();
			devicePropsEditor.initLogger();
			devicePropsEditor.open();
			Point size = devicePropsEditor.getSize();
			dialogShell.setLayout(new FillLayout());
			dialogShell.setText(Messages.getString(MessageIds.OSDE_MSGT0465));
			dialogShell.setImage(SWTResourceManager.getImage("osde/resource/EditHot.gif")); //$NON-NLS-1$
			dialogShell.setLocation(displayBounds.x < 0 ? -size.x : displayBounds.width+displayBounds.x-size.x, displayBounds.height-size.y-150);
			dialogShell.layout();
			Rectangle shellBounds = dialogShell.computeTrim(0, 0, size.x, size.y);
			dialogShell.setSize(shellBounds.width, shellBounds.height);
			dialogShell.setMinimumSize(shellBounds.width, shellBounds.height);
			dialogShell.open();
			
			if (args.length > 0) {
				String tmpDevFileName = args[0].replace(OSDE.FILE_SEPARATOR_WINDOWS, OSDE.FILE_SEPARATOR_UNIX);		
				tmpDevFileName = tmpDevFileName.toUpperCase().startsWith(devicePropsEditor.getDevicesPath().toUpperCase()) && tmpDevFileName.length() > devicePropsEditor.getDevicesPath().length()+6 // "/a.xml"
					? tmpDevFileName.substring(devicePropsEditor.getDevicesPath().length()+1) : tmpDevFileName;
				devicePropsEditor.openDevicePropertiesFile(tmpDevFileName);
			}

			while (!dialogShell.isDisposed()) {
				if (!display.readAndDispatch())
					display.sleep();
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void openAsDialog(DeviceConfiguration useDeviceConfiguration) {
		try {
			DevicePropertiesEditor devicePropsEditor = DevicePropertiesEditor.getInstance();
			devicePropsEditor.open();
			Point size = devicePropsEditor.getSize();
			dialogShell.setLayout(new FillLayout());
			dialogShell.setText(Messages.getString(MessageIds.OSDE_MSGT0465));
			dialogShell.setImage(SWTResourceManager.getImage("osde/resource/EditHot.gif")); //$NON-NLS-1$
			dialogShell.setLocation(100,50);
			dialogShell.layout();
			Rectangle shellBounds = dialogShell.computeTrim(0, 0, size.x, size.y);
			dialogShell.setSize(shellBounds.width, shellBounds.height);
			dialogShell.setMinimumSize(shellBounds.width, shellBounds.height);
			dialogShell.open();
			Display display = dialogShell.getDisplay();
			
			this.deviceConfig = useDeviceConfiguration;
			this.devicePropertiesFileName = this.deviceConfig.getPropertiesFileName();
			this.devicePropertiesFileName = this.devicePropertiesFileName.substring(this.getDevicesPath().length()+1);
			this.deviceFileNameText.setText(this.devicePropertiesFileName);
			this.deviceFileNameSelectionButton.setEnabled(false);
			update();

			while (!dialogShell.isDisposed()) {
				if (!display.readAndDispatch())
					display.sleep();
			}
		} catch (Exception e) {
			log.log(Level.SEVERE, e.getMessage(), e);
		}
	}

	/**
	 * initialize logger
	 */
	private void initLogger() {
		Handler logHandler;
		LogFormatter lf = new LogFormatter();
		Logger rootLogger = Logger.getLogger(OSDE.STRING_EMPTY);

		// cleanup previous log handler
		rootLogger.removeHandler(OSDE.logHandler);

		if (System.getProperty(OSDE.ECLIPSE_STRING) == null) { // running outside eclipse
			try {
				logHandler = new FileHandler(this.settings.getLogFilePath(), 5000000, 3);
				rootLogger.addHandler(logHandler);
				logHandler.setFormatter(lf);
				logHandler.setLevel(Level.ALL);
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
		else {
			logHandler = new ConsoleHandler();
			rootLogger.addHandler(logHandler);
			logHandler.setFormatter(lf);
			logHandler.setLevel(Level.ALL);
		}
		// set logging levels
		Logger logger = Logger.getLogger("osde.ui.dialog.edit"); //$NON-NLS-1$
    logger.setLevel(Level.INFO);
    logger.setUseParentHandlers(true);
	}

	public void open() {
		try {
			SWTResourceManager.registerResourceUser(this);
			this.setLayout(null);
			this.setSize(640, 460);
			this.getShell().addDisposeListener(new DisposeListener() {
				public void widgetDisposed(DisposeEvent evt) {
					DevicePropertiesEditor.log.log(Level.FINEST, "dialogShell.widgetDisposed, event=" + evt); //$NON-NLS-1$
					if (DevicePropertiesEditor.this.deviceConfig != null && DevicePropertiesEditor.this.deviceConfig.isChangePropery()) {
						String msg = Messages.getString(MessageIds.OSDE_MSGT0469, new String[] {DevicePropertiesEditor.this.devicePropertiesFileName});
						if (OpenSerialDataExplorer.getInstance().openYesNoMessageDialog(DevicePropertiesEditor.this.getShell(), msg) == SWT.YES) {
							DevicePropertiesEditor.this.deviceConfig.storeDeviceProperties();
						}
						
						FileUtils.runOnExitRenamer();
					}
				}
			});
			{
				this.deviceFileNameSelectionButton = new Button(this, SWT.PUSH | SWT.CENTER);
				this.deviceFileNameSelectionButton.setText(" ... "); //$NON-NLS-1$
				this.deviceFileNameSelectionButton.setFont(SWTResourceManager.getFont(OSDE.WIDGET_FONT_NAME, OSDE.WIDGET_FONT_SIZE, SWT.NORMAL, false, false));
				this.deviceFileNameSelectionButton.setToolTipText(Messages.getString(MessageIds.OSDE_MSGT0484));
				this.deviceFileNameSelectionButton.setBounds(580, 10, 30, 20);
				this.deviceFileNameSelectionButton.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent evt) {
						DevicePropertiesEditor.log.log(Level.FINEST, "fileSelectionButton.widgetSelected, event=" + evt); //$NON-NLS-1$
						if (DevicePropertiesEditor.this.deviceConfig != null && DevicePropertiesEditor.this.deviceConfig.isChangePropery()) {
							String msg = Messages.getString(MessageIds.OSDE_MSGT0469, new String[] {DevicePropertiesEditor.this.devicePropertiesFileName});
							if (OpenSerialDataExplorer.getInstance().openYesNoMessageDialog(DevicePropertiesEditor.this.getShell(), msg) == SWT.YES) {
								DevicePropertiesEditor.this.deviceConfig.storeDeviceProperties();
							}
						}
						FileDialog fileSelectionDialog = new FileDialog(DevicePropertiesEditor.this.getShell());
						fileSelectionDialog.setFilterPath(getDevicesPath());
						fileSelectionDialog.setText(Messages.getString(MessageIds.OSDE_MSGT0479));
						fileSelectionDialog.setFilterExtensions(new String[] { OSDE.FILE_ENDING_STAR_XML });
						fileSelectionDialog.setFilterNames(new String[] { Messages.getString(MessageIds.OSDE_MSGT0480) }); 
						fileSelectionDialog.open();
						String tmpFileName = fileSelectionDialog.getFileName();
						DevicePropertiesEditor.log.log(Level.INFO, "devicePropertiesFileName = " + tmpFileName); //$NON-NLS-1$

						if (tmpFileName != null && tmpFileName.length() > 4 && !tmpFileName.equals(DevicePropertiesEditor.this.devicePropertiesFileName)) {
							openDevicePropertiesFile(tmpFileName);
						}
					}
				});
			}
			{
				this.deviceFileNameText = new Text(this, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
				this.deviceFileNameText.setBounds(120, 9, 450, 22);
				this.deviceFileNameText.setFont(SWTResourceManager.getFont(OSDE.WIDGET_FONT_NAME, OSDE.WIDGET_FONT_SIZE, SWT.NORMAL, false, false));
				this.deviceFileNameText.addKeyListener(new KeyAdapter() {
					public void keyReleased(KeyEvent evt) {
						DevicePropertiesEditor.log.log(Level.FINEST, "deviceFileNameText.keyReleased, event=" + evt); //$NON-NLS-1$
						if (evt.keyCode == SWT.CR) {
							try {
								DevicePropertiesEditor.this.devicePropertiesFileName = DevicePropertiesEditor.this.deviceFileNameText.getText().trim();
								if (!DevicePropertiesEditor.this.devicePropertiesFileName.endsWith(OSDE.FILE_ENDING_DOT_XML)) {
									if (DevicePropertiesEditor.this.devicePropertiesFileName.lastIndexOf(OSDE.STRING_DOT) != -1) {
										DevicePropertiesEditor.this.devicePropertiesFileName = DevicePropertiesEditor.this.devicePropertiesFileName.substring(0, DevicePropertiesEditor.this.devicePropertiesFileName.lastIndexOf(OSDE.STRING_DOT));
									}
									DevicePropertiesEditor.this.devicePropertiesFileName = DevicePropertiesEditor.this.devicePropertiesFileName + OSDE.FILE_ENDING_DOT_XML;
								}
								DevicePropertiesEditor.log.log(Level.INFO, "devicePropertiesFileName = " + DevicePropertiesEditor.this.devicePropertiesFileName); //$NON-NLS-1$

								if (!(new File(getDevicesPath() + OSDE.FILE_SEPARATOR_UNIX + DevicePropertiesEditor.this.devicePropertiesFileName)).exists()) {
									MessageBox okCancelMessageDialog = new MessageBox(DevicePropertiesEditor.this.getShell(), SWT.PRIMARY_MODAL | SWT.OK | SWT.CANCEL | SWT.ICON_QUESTION);
									okCancelMessageDialog.setText(Messages.getString(MessageIds.OSDE_MSGT0465));
									okCancelMessageDialog.setMessage(Messages.getString(MessageIds.OSDE_MSGE0003) + DevicePropertiesEditor.this.devicePropertiesFileName + Messages.getString(MessageIds.OSDE_MSGT0481));
									if (SWT.OK == okCancelMessageDialog.open()) {
										if (FileUtils.extract(this.getClass(), "DeviceSample_" + DevicePropertiesEditor.this.settings.getLocale() + Settings.DEVICE_PROPERTIES_XSD_VERSION + OSDE.FILE_ENDING_DOT_XML, DevicePropertiesEditor.this.devicePropertiesFileName,//$NON-NLS-1$
												"resource/", getDevicesPath(), "555")) { //$NON-NLS-1$ //$NON-NLS-2$
											DevicePropertiesEditor.this.deviceConfig = new DeviceConfiguration(getDevicesPath() + OSDE.FILE_SEPARATOR_UNIX + DevicePropertiesEditor.this.devicePropertiesFileName);
										}
									}
								}
								else {
									DevicePropertiesEditor.this.deviceConfig = new DeviceConfiguration(getDevicesPath() + OSDE.FILE_SEPARATOR_UNIX + DevicePropertiesEditor.this.devicePropertiesFileName);
								}
								update();
							}
							catch (Throwable e) {
								DevicePropertiesEditor.log.log(Level.SEVERE, e.getMessage(), e);
							}
						}
					}
				});
			}
			{
				this.devicePropFileNamelabel = new Label(this, SWT.RIGHT);
				this.devicePropFileNamelabel.setText(Messages.getString(MessageIds.OSDE_MSGT0483));
				this.devicePropFileNamelabel.setFont(SWTResourceManager.getFont(OSDE.WIDGET_FONT_NAME, OSDE.WIDGET_FONT_SIZE, SWT.NORMAL, false, false));
				this.devicePropFileNamelabel.setToolTipText(Messages.getString(MessageIds.OSDE_MSGT0484));
				this.devicePropFileNamelabel.setBounds(0, 12, 100, 16);
			}
			{
				this.closeButton = new Button(this, SWT.PUSH | SWT.CENTER);
				this.closeButton.setText(Messages.getString(MessageIds.OSDE_MSGT0485));
				this.closeButton.setFont(SWTResourceManager.getFont(OSDE.WIDGET_FONT_NAME, OSDE.WIDGET_FONT_SIZE, SWT.NORMAL, false, false));
				this.closeButton.setBounds(338, 418, 250, 30);
				this.closeButton.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent evt) {
						DevicePropertiesEditor.log.log(Level.FINEST, "closeButton.widgetSelected, event=" + evt); //$NON-NLS-1$
						DevicePropertiesEditor.this.getShell().dispose();
					}
				});
			}
			{
				this.saveButton = new Button(this, SWT.PUSH | SWT.CENTER);
				this.saveButton.setText(Messages.getString(MessageIds.OSDE_MSGT0486));
				this.saveButton.setFont(SWTResourceManager.getFont(OSDE.WIDGET_FONT_NAME, OSDE.WIDGET_FONT_SIZE, SWT.NORMAL, false, false));
				this.saveButton.setBounds(50, 418, 250, 30);
				this.saveButton.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent evt) {
						DevicePropertiesEditor.log.log(Level.FINEST, "saveButton.widgetSelected, event=" + evt); //$NON-NLS-1$
						DevicePropertiesEditor.this.deviceConfig.storeDeviceProperties();
					}
				});
			}
			{
				this.tabFolder = new CTabFolder(this, SWT.BORDER);
				this.tabFolder.setSimple(false);
				this.tabFolder.setFont(SWTResourceManager.getFont(OSDE.WIDGET_FONT_NAME, OSDE.WIDGET_FONT_SIZE, SWT.NORMAL, false, false));
				this.tabFolder.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent evt) {
						DevicePropertiesEditor.log.log(Level.FINEST, "tabFolder.widgetSelected, event=" + evt); //$NON-NLS-1$
						DevicePropertiesEditor.this.enableContextMenu(DevicePropertiesEditor.this.tabFolder.getSelection().getText(), true);
					}
				});
				{
					this.deviceTabItem = new CTabItem(this.tabFolder, SWT.NONE);
					this.deviceTabItem.setText(Messages.getString(MessageIds.OSDE_MSGT0487));
					this.deviceTabItem.setFont(SWTResourceManager.getFont(OSDE.WIDGET_FONT_NAME, OSDE.WIDGET_FONT_SIZE, SWT.NORMAL, false, false));
					{
						this.deviceComposite = new Composite(this.tabFolder, SWT.NONE);
						this.deviceComposite.setLayout(null);
						this.deviceTabItem.setControl(this.deviceComposite);
						{
							this.deviceDescriptionlabel = new Label(this.deviceComposite, SWT.CENTER | SWT.WRAP);
							this.deviceDescriptionlabel.setText(Messages.getString(MessageIds.OSDE_MSGT0488));
							this.deviceDescriptionlabel.setFont(SWTResourceManager.getFont(OSDE.WIDGET_FONT_NAME, OSDE.WIDGET_FONT_SIZE, SWT.NORMAL, false, false));
							this.deviceDescriptionlabel.setBounds(12, 8, 604, 50);
						}
						{
							this.deviceLabelComposite = new Composite(this.deviceComposite, SWT.NONE);
							this.deviceLabelComposite.setLayout(null);
							this.deviceLabelComposite.setBounds(20, 70, 145, 190);
							{
								this.deviceNameLabel = new Label(this.deviceLabelComposite, SWT.RIGHT);
								this.deviceNameLabel.setText(Messages.getString(MessageIds.OSDE_MSGT0489));
								this.deviceNameLabel.setFont(SWTResourceManager.getFont(OSDE.WIDGET_FONT_NAME, OSDE.WIDGET_FONT_SIZE, SWT.NORMAL, false, false));
								this.deviceNameLabel.setForeground(osde.ui.SWTResourceManager.getColor(SWT.COLOR_BLACK));
								this.deviceNameLabel.setBounds(0, 0, 137, 16);
							}
							{
								this.manufacturerLabel = new Label(this.deviceLabelComposite, SWT.RIGHT);
								this.manufacturerLabel.setText(Messages.getString(MessageIds.OSDE_MSGT0490));
								this.manufacturerLabel.setFont(SWTResourceManager.getFont(OSDE.WIDGET_FONT_NAME, OSDE.WIDGET_FONT_SIZE, SWT.NORMAL, false, false));
								this.manufacturerLabel.setBounds(0, 33, 137, 19);
							}
							{
								this.manufURLabel = new Label(this.deviceLabelComposite, SWT.RIGHT);
								this.manufURLabel.setText(Messages.getString(MessageIds.OSDE_MSGT0491));
								this.manufURLabel.setFont(SWTResourceManager.getFont(OSDE.WIDGET_FONT_NAME, OSDE.WIDGET_FONT_SIZE, SWT.NORMAL, false, false));
								this.manufURLabel.setBounds(0, 64, 137, 19);
							}
							{
								this.imageFileNameLabel = new Label(this.deviceLabelComposite, SWT.RIGHT);
								this.imageFileNameLabel.setText(Messages.getString(MessageIds.OSDE_MSGT0492));
								this.imageFileNameLabel.setFont(SWTResourceManager.getFont(OSDE.WIDGET_FONT_NAME, OSDE.WIDGET_FONT_SIZE, SWT.NORMAL, false, false));
								this.imageFileNameLabel.setBounds(0, 95, 137, 19);
							}
							{
								this.usageLabel = new Label(this.deviceLabelComposite, SWT.RIGHT);
								this.usageLabel.setText(Messages.getString(MessageIds.OSDE_MSGT0493));
								this.usageLabel.setFont(SWTResourceManager.getFont(OSDE.WIDGET_FONT_NAME, OSDE.WIDGET_FONT_SIZE, SWT.NORMAL, false, false));
								this.usageLabel.setBounds(0, 126, 137, 19);
							}
							{
								this.groupLabel = new Label(this.deviceLabelComposite, SWT.RIGHT);
								this.groupLabel.setText(Messages.getString(MessageIds.OSDE_MSGT0494));
								this.groupLabel.setFont(SWTResourceManager.getFont(OSDE.WIDGET_FONT_NAME, OSDE.WIDGET_FONT_SIZE, SWT.NORMAL, false, false));
								this.groupLabel.setBounds(0, 157, 137, 16);
							}
						}
						{
							this.devicePropsComposite = new Composite(this.deviceComposite, SWT.NONE);
							this.devicePropsComposite.setLayout(null);
							this.devicePropsComposite.setBounds(170, 70, 450, 190);
							this.devicePropsComposite.addPaintListener(new PaintListener() {
								public void paintControl(PaintEvent evt) {
									DevicePropertiesEditor.log.log(Level.FINEST, "devicePropsComposite.paintControl, event=" + evt); //$NON-NLS-1$
									if (DevicePropertiesEditor.this.deviceComposite.isVisible()) {
										DevicePropertiesEditor.this.deviceFileNameText.setText(DevicePropertiesEditor.this.devicePropertiesFileName);
										DevicePropertiesEditor.this.nameText.setText(DevicePropertiesEditor.this.deviceName);
										DevicePropertiesEditor.this.manufURLText.setText(DevicePropertiesEditor.this.manufacuturerURL);
										DevicePropertiesEditor.this.imageFileNameText.setText(DevicePropertiesEditor.this.imageFileName);
										DevicePropertiesEditor.this.manufacturerText.setText(DevicePropertiesEditor.this.manufacturer);
										DevicePropertiesEditor.this.usageButton.setSelection(DevicePropertiesEditor.this.isDeviceUsed);
										DevicePropertiesEditor.this.groupSelectionCombo.select(DevicePropertiesEditor.this.deviceGroup.ordinal());
									}
								}
							});
							{
								this.nameText = new Text(this.devicePropsComposite, SWT.BORDER);
								this.nameText.setFont(SWTResourceManager.getFont(OSDE.WIDGET_FONT_NAME, OSDE.WIDGET_FONT_SIZE, SWT.NORMAL, false, false));
								this.nameText.setBounds(0, 0, 409, 22);
								this.nameText.addKeyListener(new KeyAdapter() {
									public void keyReleased(KeyEvent evt) {
										DevicePropertiesEditor.log.log(Level.FINEST, "nameText.keyReleased, event=" + evt); //$NON-NLS-1$
										DevicePropertiesEditor.this.deviceConfig.setName(DevicePropertiesEditor.this.deviceName = DevicePropertiesEditor.this.nameText.getText());
									}
								});
							}
							{
								this.manufacturerText = new Text(this.devicePropsComposite, SWT.BORDER);
								this.manufacturerText.setFont(SWTResourceManager.getFont(OSDE.WIDGET_FONT_NAME, OSDE.WIDGET_FONT_SIZE, SWT.NORMAL, false, false));
								this.manufacturerText.setBounds(0, 32, 409, 22);
								this.manufacturerText.addKeyListener(new KeyAdapter() {
									public void keyReleased(KeyEvent evt) {
										DevicePropertiesEditor.log.log(Level.FINEST, "manufacturerText.keyReleased, event=" + evt); //$NON-NLS-1$
										DevicePropertiesEditor.this.deviceConfig.setManufacturer(DevicePropertiesEditor.this.manufacturer = DevicePropertiesEditor.this.manufacturerText.getText());
									}
								});
							}
							{
								this.manufURLText = new Text(this.devicePropsComposite, SWT.BORDER);
								this.manufURLText.setFont(SWTResourceManager.getFont(OSDE.WIDGET_FONT_NAME, OSDE.WIDGET_FONT_SIZE, SWT.NORMAL, false, false));
								this.manufURLText.setBounds(0, 64, 409, 22);
								this.manufURLText.addKeyListener(new KeyAdapter() {
									public void keyReleased(KeyEvent evt) {
										DevicePropertiesEditor.log.log(Level.FINEST, "manufURLText.keyReleased, event=" + evt); //$NON-NLS-1$
										DevicePropertiesEditor.this.deviceConfig.setManufacturerURL(DevicePropertiesEditor.this.manufacuturerURL = DevicePropertiesEditor.this.manufURLText.getText());
									}
								});
							}
							{
								this.imageFileNameText = new Text(this.devicePropsComposite, SWT.BORDER);
								this.imageFileNameText.setFont(SWTResourceManager.getFont(OSDE.WIDGET_FONT_NAME, OSDE.WIDGET_FONT_SIZE, SWT.NORMAL, false, false));
								this.imageFileNameText.setBounds(0, 95, 409, 22);
								this.imageFileNameText.addKeyListener(new KeyAdapter() {
									public void keyReleased(KeyEvent evt) {
										DevicePropertiesEditor.log.log(Level.FINEST, "imageFileNameText.keyReleased, event=" + evt); //$NON-NLS-1$
										DevicePropertiesEditor.this.deviceConfig.setImageFileName(DevicePropertiesEditor.this.imageFileName = DevicePropertiesEditor.this.imageFileNameText.getText());
									}
								});
							}
							{
								this.usageButton = new Button(this.devicePropsComposite, SWT.CHECK);
								this.usageButton.setBounds(3, 126, 159, 22);
								this.usageButton.addSelectionListener(new SelectionAdapter() {
									public void widgetSelected(SelectionEvent evt) {
										DevicePropertiesEditor.log.log(Level.FINEST, "usageButton.widgetSelected, event=" + evt); //$NON-NLS-1$
										if (DevicePropertiesEditor.this.deviceConfig != null) {
											DevicePropertiesEditor.this.deviceConfig.setUsed(DevicePropertiesEditor.this.isDeviceUsed = usageButton.getSelection());
										}
									}
								});
							}
							{
								this.groupSelectionCombo = new CCombo(this.devicePropsComposite, SWT.BORDER);
								this.groupSelectionCombo.setFont(SWTResourceManager.getFont(OSDE.WIDGET_FONT_NAME, OSDE.WIDGET_FONT_SIZE, SWT.NORMAL, false, false));
								this.groupSelectionCombo.setItems(DeviceTypes.valuesAsStingArray());
								this.groupSelectionCombo.setBounds(0, 154, 409, 22);
								this.groupSelectionCombo.addSelectionListener(new SelectionAdapter() {
									public void widgetSelected(SelectionEvent evt) {
										DevicePropertiesEditor.log.log(Level.FINEST, "groupSelectionCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
										if (DevicePropertiesEditor.this.deviceConfig != null) {
											DevicePropertiesEditor.this.deviceConfig.setDeviceGroup(DevicePropertiesEditor.this.deviceGroup = DeviceTypes.valueOf(DevicePropertiesEditor.this.groupSelectionCombo.getText()));
										}
									}
								});
							}
							{
								this.fileSelectButton = new Button(this.devicePropsComposite, SWT.PUSH | SWT.CENTER);
								this.fileSelectButton.setText(" ... "); //$NON-NLS-1$
								this.fileSelectButton.setToolTipText(Messages.getString(MessageIds.OSDE_MSGT0502));
								this.fileSelectButton.setFont(SWTResourceManager.getFont(OSDE.WIDGET_FONT_NAME, OSDE.WIDGET_FONT_SIZE, SWT.NORMAL, false, false));
								this.fileSelectButton.setBounds(415, 96, 30, 20);
								this.fileSelectButton.addSelectionListener(new SelectionAdapter() {
									@SuppressWarnings("unchecked")
									public void widgetSelected(SelectionEvent evt) {
										FileDialog fileSelectionDialog = new FileDialog(DevicePropertiesEditor.this.getShell());
										fileSelectionDialog.setText("OpenSerialDataExplorer Device Image File"); //$NON-NLS-1$
										fileSelectionDialog.setFilterPath(getDevicesPath());
										fileSelectionDialog.setFilterExtensions(new String[] { OSDE.FILE_ENDING_STAR_JPG, OSDE.FILE_ENDING_STAR_GIF, OSDE.FILE_ENDING_STAR_PNG });
										fileSelectionDialog.setFilterNames(new String[] { Messages.getString(MessageIds.OSDE_MSGT0215), Messages.getString(MessageIds.OSDE_MSGT0214), Messages.getString(MessageIds.OSDE_MSGT0213) });
										fileSelectionDialog.open();
										DevicePropertiesEditor.this.imageFileName = fileSelectionDialog.getFileName();
										String fullQualifiedImageSourceName = fileSelectionDialog.getFilterPath()+OSDE.FILE_SEPARATOR_UNIX+DevicePropertiesEditor.this.imageFileName;
										if (DevicePropertiesEditor.this.imageFileName != null && DevicePropertiesEditor.this.imageFileName.length() > 5) {
											DevicePropertiesEditor.this.imageFileNameText.setText(DevicePropertiesEditor.this.imageFileName);
											DevicePropertiesEditor.log.log(Level.INFO, "imageFileName = " + DevicePropertiesEditor.this.imageFileName); //$NON-NLS-1$
											if (DevicePropertiesEditor.this.deviceConfig != null) {
												DevicePropertiesEditor.this.deviceConfig.setImageFileName(DevicePropertiesEditor.this.imageFileName = DevicePropertiesEditor.this.imageFileNameText.getText());
												Image deviceImage = new Image(Display.getDefault(), new Image(Display.getDefault(), fullQualifiedImageSourceName).getImageData().scaledTo(225, 165));
												boolean isStartedWithinEclipse = DevicePropertiesEditor.class.getProtectionDomain().getCodeSource().getLocation().getPath().endsWith(OSDE.FILE_SEPARATOR_UNIX);
												String deviceImplName = DevicePropertiesEditor.this.deviceConfig.getDeviceImplName().replace(OSDE.STRING_BLANK, OSDE.STRING_EMPTY).replace(OSDE.STRING_DASH, OSDE.STRING_EMPTY);
												if (isStartedWithinEclipse) {
													log.log(Level.INFO, "started within Eclipse"); //$NON-NLS-1$
													String basPath = DevicePropertiesEditor.class.getProtectionDomain().getCodeSource().getLocation().getPath();
													String fullQualifiedImageTargetName = basPath.substring(0, basPath.indexOf(OSDE.OSDE_NAME_LONG))+deviceImplName+"/src/resource/" + DevicePropertiesEditor.this.imageFileName; //$NON-NLS-1$
													log.log(Level.INFO, "fullQualifiedImageTargetName = " + fullQualifiedImageTargetName); //$NON-NLS-1$
													ImageLoader imageLoader = new ImageLoader();
													imageLoader.data = new ImageData[] { deviceImage.getImageData() };
													try {
														imageLoader.save(new FileOutputStream(fullQualifiedImageTargetName), SWT.IMAGE_JPEG);	
													}
													catch (IOException e) {
														log.log(Level.WARNING, e.getMessage(), e);
													}
												}
												else 
												{
													if (OpenSerialDataExplorer.application != null) { // started within OSDE
														log.log(Level.INFO, "started within OpenSerialDataExplorer"); //$NON-NLS-1$
													}
													else { // started outside OSDE
														log.log(Level.INFO, "started outside OpenSerialDataExplorer"); //$NON-NLS-1$
														try {
															Thread.currentThread().setContextClassLoader(OSDE.getClassLoader());
														}
														catch (Throwable e) {
															log.log(Level.WARNING, e.getMessage(), e);
														}
													}
													String deviceJarPath = null;
													String tmpDeviceJarPath = null;
													try {
														{
															IDevice newInst = null;
															String className = "osde.device." + DevicePropertiesEditor.this.deviceConfig.getManufacturer().toLowerCase().replace(OSDE.STRING_BLANK, OSDE.STRING_EMPTY).replace(OSDE.STRING_DASH, OSDE.STRING_EMPTY) + OSDE.STRING_DOT + deviceImplName; //$NON-NLS-1$
															log.log(Level.INFO, "loading Class " + className); //$NON-NLS-1$
															ClassLoader loader = Thread.currentThread().getContextClassLoader();
															Class c = loader.loadClass(className);
															Constructor constructor = c.getDeclaredConstructor(new Class[] { DeviceConfiguration.class });
															log.log(Level.INFO, "constructor != null -> " + (constructor != null ? "true" : "false")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
															if (constructor != null) {
																newInst = (IDevice) constructor.newInstance(new Object[] { DevicePropertiesEditor.this.deviceConfig });
																deviceJarPath = newInst.getClass().getProtectionDomain().getCodeSource().getLocation().getPath().replace(OSDE.STRING_URL_BLANK, OSDE.STRING_BLANK);
															}
															else
																throw new NoClassDefFoundError(Messages.getString(MessageIds.OSDE_MSGE0016));
														}
														//Thread.currentThread().setContextClassLoader(new URLClassLoader(new URL[0]));
																	
														//if (isStartedWithinEclipse) {
														//	deviceJarPath = "c:\\Program Files\\OpenSerialDataExplorer\\devices\\Simulator.jar";
														//}
														tmpDeviceJarPath = deviceJarPath.substring(0, deviceJarPath.lastIndexOf(OSDE.STRING_DOT)) + OSDE.FILE_ENDING_DOT_TMP;
														String addJarEntryName = "resource/" + DevicePropertiesEditor.this.imageFileName; //$NON-NLS-1$
														log.log(Level.INFO, "deviceJarPath = " + deviceJarPath + "; tmpDeviceJarPath = " + tmpDeviceJarPath); //$NON-NLS-1$ //$NON-NLS-2$

														FileUtils.updateJarContent(deviceJarPath, tmpDeviceJarPath, addJarEntryName, deviceImage, DevicePropertiesEditor.dialogShell);
													}
													catch (NoClassDefFoundError e) {
														log.log(Level.SEVERE, e.getMessage(), e);
													}
													catch (Exception e) {
														log.log(Level.SEVERE, e.getMessage(), e);
													}
												}
											}
										}
									}
								});
							}
						}
					}
				}
				{
					this.serialPortTabItem = new SeriaPortTypeTabItem(this.tabFolder, SWT.CLOSE, 1);
				}
				{
					this.timeBaseTabItem = new CTabItem(this.tabFolder, SWT.NONE);
					SWTResourceManager.registerResourceUser(this.timeBaseTabItem);
					this.timeBaseTabItem.setText(Messages.getString(MessageIds.OSDE_MSGT0495));
					this.timeBaseTabItem.setFont(SWTResourceManager.getFont(OSDE.WIDGET_FONT_NAME, OSDE.WIDGET_FONT_SIZE, SWT.NORMAL, false, false));
					{
						this.timeBaseComposite = new Composite(this.tabFolder, SWT.NONE);
						this.timeBaseTabItem.setControl(this.timeBaseComposite);
						this.timeBaseComposite.setLayout(null);
						this.timeBaseComposite.addPaintListener(new PaintListener() {
							public void paintControl(PaintEvent evt) {
								DevicePropertiesEditor.log.log(Level.FINEST, "timeBaseComposite.paintControl, event=" + evt); //$NON-NLS-1$
								DevicePropertiesEditor.this.timeBaseTimeStepText.setText(String.format("%.1f", DevicePropertiesEditor.this.timeStep_ms)); //$NON-NLS-1$
							}
						});
						{
							this.timeBaseDescriptionLabel = new Label(this.timeBaseComposite, SWT.CENTER | SWT.WRAP);
							this.timeBaseDescriptionLabel.setText(Messages.getString(MessageIds.OSDE_MSGT0496));
							this.timeBaseDescriptionLabel.setFont(SWTResourceManager.getFont(OSDE.WIDGET_FONT_NAME, OSDE.WIDGET_FONT_SIZE, SWT.NORMAL, false, false));
							this.timeBaseDescriptionLabel.setBounds(17, 12, 591, 71);
						}
						{
							this.timeBaseNameLabel = new Label(this.timeBaseComposite, SWT.RIGHT);
							this.timeBaseNameLabel.setText(Messages.getString(MessageIds.OSDE_MSGT0497));
							this.timeBaseNameLabel.setFont(SWTResourceManager.getFont(OSDE.WIDGET_FONT_NAME, OSDE.WIDGET_FONT_SIZE, SWT.NORMAL, false, false));
							this.timeBaseNameLabel.setBounds(142, 95, 150, 20);
						}
						{
							this.timeBaseNameText = new Text(this.timeBaseComposite, SWT.CENTER | SWT.BORDER);
							this.timeBaseNameText.setText(Messages.getString(MessageIds.OSDE_MSGT0271).split(OSDE.STRING_BLANK)[0]);
							this.timeBaseNameText.setFont(SWTResourceManager.getFont(OSDE.WIDGET_FONT_NAME, OSDE.WIDGET_FONT_SIZE, SWT.NORMAL, false, false));
							this.timeBaseNameText.setBounds(322, 94, 60, 20);
							this.timeBaseNameText.setEditable(false);
						}
						{
							this.timeBaseSymbolLabel = new Label(this.timeBaseComposite, SWT.RIGHT);
							this.timeBaseSymbolLabel.setText(Messages.getString(MessageIds.OSDE_MSGT0498));
							this.timeBaseSymbolLabel.setFont(SWTResourceManager.getFont(OSDE.WIDGET_FONT_NAME, OSDE.WIDGET_FONT_SIZE, SWT.NORMAL, false, false));
							this.timeBaseSymbolLabel.setBounds(142, 125, 150, 20);
						}
						{
							this.timeBaseSymbolText = new Text(this.timeBaseComposite, SWT.CENTER | SWT.BORDER);
							this.timeBaseSymbolText.setText(Messages.getString(MessageIds.OSDE_MSGT0271).split(OSDE.STRING_BLANK)[3]);
							this.timeBaseSymbolText.setFont(SWTResourceManager.getFont(OSDE.WIDGET_FONT_NAME, OSDE.WIDGET_FONT_SIZE, SWT.NORMAL, false, false));
							this.timeBaseSymbolText.setBounds(322, 124, 60, 20);
							this.timeBaseSymbolText.setEditable(false);
						}
						{
							this.timeBaseUnitLabel = new Label(this.timeBaseComposite, SWT.RIGHT);
							this.timeBaseUnitLabel.setText(Messages.getString(MessageIds.OSDE_MSGT0499));
							this.timeBaseUnitLabel.setFont(SWTResourceManager.getFont(OSDE.WIDGET_FONT_NAME, OSDE.WIDGET_FONT_SIZE, SWT.NORMAL, false, false));
							this.timeBaseUnitLabel.setBounds(142, 155, 150, 20);
						}
						{
							this.timeBaseUnitText = new Text(this.timeBaseComposite, SWT.CENTER | SWT.BORDER);
							this.timeBaseUnitText.setText(Messages.getString(MessageIds.OSDE_MSGT0271).split(OSDE.STRING_BLANK)[7]);
							this.timeBaseUnitText.setFont(SWTResourceManager.getFont(OSDE.WIDGET_FONT_NAME, OSDE.WIDGET_FONT_SIZE, SWT.NORMAL, false, false));
							this.timeBaseUnitText.setBounds(322, 154, 60, 20);
							this.timeBaseUnitText.setEditable(false);
						}
						{
							this.timeBaseTimeStepLabel = new Label(this.timeBaseComposite, SWT.RIGHT);
							this.timeBaseTimeStepLabel.setText(Messages.getString(MessageIds.OSDE_MSGT0500));
							this.timeBaseTimeStepLabel.setFont(SWTResourceManager.getFont(OSDE.WIDGET_FONT_NAME, OSDE.WIDGET_FONT_SIZE, SWT.NORMAL, false, false));
							this.timeBaseTimeStepLabel.setToolTipText(Messages.getString(MessageIds.OSDE_MSGT0501));
							this.timeBaseTimeStepLabel.setBounds(142, 185, 150, 20);
						}
						{
							this.timeBaseTimeStepText = new Text(this.timeBaseComposite, SWT.RIGHT | SWT.BORDER);
							this.timeBaseTimeStepText.setText("1000.0"); //$NON-NLS-1$
							this.timeBaseTimeStepText.setFont(SWTResourceManager.getFont(OSDE.WIDGET_FONT_NAME, OSDE.WIDGET_FONT_SIZE, SWT.NORMAL, false, false));
							this.timeBaseTimeStepText.setBounds(322, 184, 60, 20);
							this.timeBaseTimeStepText.addVerifyListener(new VerifyListener() {
								public void verifyText(VerifyEvent evt) {
									DevicePropertiesEditor.log.log(Level.FINEST, "timeBaseTimeStepText.verifyText, event=" + evt); //$NON-NLS-1$
									evt.doit = StringHelper.verifyTypedInput(DataTypes.DOUBLE, evt.text);
								}
							});
							this.timeBaseTimeStepText.addKeyListener(new KeyAdapter() {
								public void keyReleased(KeyEvent evt) {
									DevicePropertiesEditor.log.log(Level.FINEST, "timeBaseTimeStepText.keyReleased, event=" + evt); //$NON-NLS-1$
									try {
										DevicePropertiesEditor.this.timeStep_ms = Double.parseDouble(DevicePropertiesEditor.this.timeBaseTimeStepText.getText().replace(OSDE.STRING_COMMA, OSDE.STRING_DOT));
										DevicePropertiesEditor.this.timeStep_ms = Double.parseDouble(String.format(Locale.ENGLISH, "%.1f", DevicePropertiesEditor.this.timeStep_ms)); //$NON-NLS-1$
										if (DevicePropertiesEditor.this.deviceConfig != null) DevicePropertiesEditor.this.deviceConfig.setTimeStep_ms(DevicePropertiesEditor.this.timeStep_ms);
									}
									catch (NumberFormatException e) {
										// ignore input
									}
								}
							});
						}
					}
				}
				{
					createDataBlockType();
				}
				{
					createStateTabItem();
				}
				{
					this.channelConfigurationTabItem = new CTabItem(this.tabFolder, SWT.NONE);
					this.channelConfigurationTabItem.setText(Messages.getString(MessageIds.OSDE_MSGT0503));
					this.channelConfigurationTabItem.setFont(SWTResourceManager.getFont(OSDE.WIDGET_FONT_NAME, OSDE.WIDGET_FONT_SIZE, SWT.NORMAL, false, false));
					{
						this.channelConfigComposite = new Composite(this.tabFolder, SWT.NONE);
						this.channelConfigComposite.setLayout(null);
						this.channelConfigurationTabItem.setControl(this.channelConfigComposite);
						this.channelConfigComposite.addPaintListener(new PaintListener() {
							public void paintControl(PaintEvent evt) {
								DevicePropertiesEditor.log.log(Level.FINEST, "channleConfigComposite.paintControl, event=" + evt); //$NON-NLS-1$
								if (DevicePropertiesEditor.this.deviceConfig != null) {
									for (int i = 1; i <= DevicePropertiesEditor.this.deviceConfig.getChannelCount(); i++) {
										DevicePropertiesEditor.this.deviceConfig.getChannelTypes(i);
									}
								}
							}
						});
						{
							this.channelConfigDescriptionLabel = new Label(this.channelConfigComposite, SWT.CENTER | SWT.WRAP);
							this.channelConfigDescriptionLabel.setText(Messages.getString(MessageIds.OSDE_MSGT0504));
							this.channelConfigDescriptionLabel.setFont(SWTResourceManager.getFont(OSDE.WIDGET_FONT_NAME, OSDE.WIDGET_FONT_SIZE, SWT.NORMAL, false, false));
							this.channelConfigDescriptionLabel.setBounds(12, 5, 602, 38);
						}
						{
							this.channelConfigInnerTabFolder = new CTabFolder(this.channelConfigComposite, SWT.NONE | SWT.BORDER);
							this.channelConfigInnerTabFolder.setFont(SWTResourceManager.getFont(OSDE.WIDGET_FONT_NAME, OSDE.WIDGET_FONT_SIZE, SWT.NORMAL, false, false));
							this.channelConfigInnerTabFolder.setBounds(0, 49, 626, 285);
							{
								//initial channel TabItem
								new ChannelTypeTabItem(this.channelConfigInnerTabFolder, SWT.NONE, 0);
							}
							this.channelConfigInnerTabFolder.setSelection(0);
							this.channelConfigInnerTabFolder.addCTabFolder2Listener(new CTabFolder2Adapter() {
								@Override
								public void restore(CTabFolderEvent evt) {
									log.log(Level.FINE, "measurementsTabFolder.restore, event=" + evt); //$NON-NLS-1$
								}

								@Override
								public void close(CTabFolderEvent evt) {
									log.log(Level.FINE, "measurementsTabFolder.close, event=" + evt); //$NON-NLS-1$
									ChannelTypeTabItem tabItem = ((ChannelTypeTabItem) evt.item);
									if (deviceConfig != null) {
										deviceConfig.removeChannelType(tabItem.channelConfigNumber);
									}
									channelConfigInnerTabFolder.setSelection(tabItem.channelConfigNumber - 1);
									tabItem.dispose();
									int itemCount = channelConfigInnerTabFolder.getItemCount();
									if (itemCount > 1)
										channelConfigInnerTabFolder.getItem(itemCount-1).setShowClose(true);
								}
							});
						}
					}
				}
				{
					this.destopTabItem = new CTabItem(this.tabFolder, SWT.NONE);
					this.destopTabItem.setText(Messages.getString(MessageIds.OSDE_MSGT0505));
					this.destopTabItem.setFont(SWTResourceManager.getFont(OSDE.WIDGET_FONT_NAME, OSDE.WIDGET_FONT_SIZE, SWT.NORMAL, false, false));
					{
						this.desktopComposite = new Composite(this.tabFolder, SWT.NONE);
						this.desktopComposite.setLayout(null);
						this.destopTabItem.setControl(this.desktopComposite);
						{
							this.desktopDescriptionLabel = new Label(this.desktopComposite, SWT.CENTER | SWT.WRAP);
							this.desktopDescriptionLabel.setText(Messages.getString(MessageIds.OSDE_MSGT0506));
							this.desktopDescriptionLabel.setFont(SWTResourceManager.getFont(OSDE.WIDGET_FONT_NAME, OSDE.WIDGET_FONT_SIZE, SWT.NORMAL, false, false));
							this.desktopDescriptionLabel.setBounds(12, 5, 602, 57);
						}
						{
							this.desktopTabFolder = new CTabFolder(this.desktopComposite, SWT.BORDER);
							GridLayout appDesktopTabCompositeLayout = new GridLayout();
							appDesktopTabCompositeLayout.makeColumnsEqualWidth = true;
							this.desktopTabFolder.setFont(SWTResourceManager.getFont(OSDE.WIDGET_FONT_NAME, OSDE.WIDGET_FONT_SIZE, SWT.NORMAL, false, false));
							this.desktopTabFolder.setLayout(appDesktopTabCompositeLayout);
							this.desktopTabFolder.setBounds(135, 68, 360, 196);
							{
								this.desktopInnerTabItem1 = new PropertyTypeTabItem(this.desktopTabFolder, SWT.NONE, DesktopPropertyTypes.TABLE_TAB.value(), null);
							}
							{
								this.desktopInnerTabItem2 = new PropertyTypeTabItem(this.desktopTabFolder, SWT.NONE, DesktopPropertyTypes.DIGITAL_TAB.value(), null);
							}
							{
								this.desktopInnerTabItem3 = new PropertyTypeTabItem(this.desktopTabFolder, SWT.NONE, DesktopPropertyTypes.ANALOG_TAB.value(), null);
							}
							{
								this.desktopInnerTabItem4 = new PropertyTypeTabItem(this.desktopTabFolder, SWT.NONE, DesktopPropertyTypes.VOLTAGE_PER_CELL_TAB.value(), null);
							}
							this.desktopTabFolder.setSelection(0);
						}
					}
				}
				this.tabFolder.setSelection(0);
				this.tabFolder.setBounds(0, 45, 632, 360);
				this.tabFolder.addCTabFolder2Listener(new CTabFolder2Adapter() {
					public void restore(CTabFolderEvent evt) {
						DevicePropertiesEditor.log.log(Level.FINEST, "tabFolder.restore, event=" + evt); //$NON-NLS-1$
						((CTabItem) evt.item).getControl();
					}

					public void close(CTabFolderEvent evt) {
						DevicePropertiesEditor.log.log(Level.INFO, "tabFolder.close, event=" + evt); //$NON-NLS-1$
						CTabItem tabItem = ((CTabItem) evt.item);
						if (tabItem.getText().equals(Messages.getString(MessageIds.OSDE_MSGT0470))) {
							tabItem.dispose();
							stateTabItem = null;
							if (DevicePropertiesEditor.this.deviceConfig != null) DevicePropertiesEditor.this.deviceConfig.removeStateType();
						}
						else if (tabItem.getText().equals(Messages.getString(MessageIds.OSDE_MSGT0510))) {
							tabItem.dispose();
							serialPortTabItem = null;
							if (DevicePropertiesEditor.this.deviceConfig != null) DevicePropertiesEditor.this.deviceConfig.removeSerialPortType();
						}
						else if (tabItem.getText().equals(Messages.getString(MessageIds.OSDE_MSGT0515))) {
							tabItem.dispose();
							dataBlockTabItem = null;
							if (DevicePropertiesEditor.this.deviceConfig != null) DevicePropertiesEditor.this.deviceConfig.removeDataBlockType();
						}
						if (DevicePropertiesEditor.this.deviceConfig != null) update();
					}
				});
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * enable the context menu to create missing tab items
	 * @param tabItemName to enable the popup menu
	 * @param enable (always true, will be recalled internally for housekeeping)
	 */
	void enableContextMenu(String tabItemName, boolean enable) {
		if (this.lastTabItemName != null && !this.lastTabItemName.equals(tabItemName)) {
			this.enableContextMenu(this.lastTabItemName, false);
		}
		this.lastTabItemName = tabItemName;
		//this.tabFolder.setMenu(this.popupMenu);

		if (tabItemName.equals(Messages.getString(MessageIds.OSDE_MSGT0510))) { // Serial port
			this.serialPortTabItem.enableContextmenu(enable);
		}
		else {
			if (enable && (this.popupMenu == null || this.contextMenu == null)) {
				this.popupMenu = new Menu(this.tabFolder.getShell(), SWT.POP_UP);
				//this.popupMenu = SWTResourceManager.getMenu("Contextmenu", this.tabFolder.getShell(), SWT.POP_UP);
				this.contextMenu = new ContextMenu(this.popupMenu, this.tabFolder);
				this.contextMenu.create();
			}
			else if (this.popupMenu != null ) {
				this.popupMenu.dispose();
				this.popupMenu = null;
				this.contextMenu = null;
			}
			if (tabItemName.equals(Messages.getString(MessageIds.OSDE_MSGT0487))) { //Device
				this.deviceComposite.setMenu(this.popupMenu);
				this.deviceDescriptionlabel.setMenu(this.popupMenu);
				this.deviceLabelComposite.setMenu(this.popupMenu);
				this.deviceNameLabel.setMenu(this.popupMenu);
				this.manufacturerLabel.setMenu(this.popupMenu);
				this.manufURLabel.setMenu(this.popupMenu);
				this.imageFileNameLabel.setMenu(this.popupMenu);
				this.usageLabel.setMenu(this.popupMenu);
				this.groupLabel.setMenu(this.popupMenu);
				this.devicePropsComposite.setMenu(this.popupMenu);
				//this.nameText.setMenu(this.popupMenu);
				//this.manufacturerText.setMenu(this.popupMenu);
				//this.manufURLText.setMenu(this.popupMenu);
				//this.imageFileNameText.setMenu(this.popupMenu);
				this.usageButton.setMenu(this.popupMenu);
				//this.groupSelectionCombo.setMenu(this.popupMenu);
			}
			else if (tabItemName.equals(Messages.getString(MessageIds.OSDE_MSGT0495))) { //Time base
				this.timeBaseComposite.setMenu(this.popupMenu);
				this.timeBaseDescriptionLabel.setMenu(this.popupMenu);
				this.timeBaseNameLabel.setMenu(this.popupMenu);
				this.timeBaseNameText.setMenu(this.popupMenu);
				this.timeBaseSymbolLabel.setMenu(this.popupMenu);
				this.timeBaseSymbolText.setMenu(this.popupMenu);
				this.timeBaseUnitLabel.setMenu(this.popupMenu);
				this.timeBaseUnitText.setMenu(this.popupMenu);
				this.timeBaseTimeStepLabel.setMenu(this.popupMenu);
				this.timeBaseTimeStepText.setMenu(this.popupMenu);
			}
			else if (tabItemName.equals(Messages.getString(MessageIds.OSDE_MSGT0515))) { // Data block
				this.dataBlockComposite.setMenu(this.popupMenu);
				this.dataBlockDescriptionLabel.setMenu(this.popupMenu);
				this.dataBlockRequiredGroup.setMenu(this.popupMenu);
				this.dataBlockFormatLabel.setMenu(this.popupMenu);
				this.dataBlockSizeLabel.setMenu(this.popupMenu);
				this.dataBlockOptionalGroup.setMenu(this.popupMenu);
				this.dataBlockCheckSumFormatButton.setMenu(this.popupMenu);
				this.dataBlockCheckSumTypeLabel.setMenu(this.popupMenu);
				this.dataBlockEndingButton.setMenu(this.popupMenu);
				this.preferredDataLocationButton.setMenu(this.popupMenu);
				this.preferredFileExtensionButton.setMenu(this.popupMenu);
			}
			else if (tabItemName.equals(Messages.getString(MessageIds.OSDE_MSGT0470))) { // State
				this.stateComposite.setMenu(this.popupMenu);
				this.stateDescriptionLabel.setMenu(this.popupMenu);
			}
			else if (tabItemName.equals(Messages.getString(MessageIds.OSDE_MSGT0503))) { // Channel/Configuration
				this.channelConfigComposite.setMenu(this.popupMenu);
				this.channelConfigDescriptionLabel.setMenu(this.popupMenu);
				this.channelConfigInnerTabFolder.setMenu(this.popupMenu);
			}
			else if (tabItemName.equals(Messages.getString(MessageIds.OSDE_MSGT0505))) { // Desktop
				this.desktopComposite.setMenu(this.popupMenu);
				this.desktopDescriptionLabel.setMenu(this.popupMenu);
			}
		}
	}

	/**
	 * open a device properties file
	 * @param devicePropertiesFileName
	 */
	private void openDevicePropertiesFile(String devicePropertiesFileName) {
		try {
			DevicePropertiesEditor.this.deviceConfig = new DeviceConfiguration(getDevicesPath() + OSDE.FILE_SEPARATOR_UNIX + devicePropertiesFileName);

			DevicePropertiesEditor.this.devicePropertiesFileName = devicePropertiesFileName;
			DevicePropertiesEditor.this.deviceFileNameText.setText(DevicePropertiesEditor.this.devicePropertiesFileName);
			update();
		}
		catch (JAXBException e) {
			log.log(Level.WARNING, e.getMessage(), e);
			if (e.getLinkedException() instanceof SAXParseException) {
				SAXParseException spe = (SAXParseException) e.getLinkedException();
				openWarningMessageBox(Messages.getString(MessageIds.OSDE_MSGW0039, new String[] { spe.getSystemId().replace(OSDE.STRING_URL_BLANK, OSDE.STRING_BLANK), spe.getLocalizedMessage() }));
			}
		}
		catch (Exception e) {
			log.log(Level.WARNING, e.getMessage(), e);
		}
	}

	/**
	 * create a new data block type and place it right after time base
	 */
	public void createDataBlockType() {
		for (int i = 1; i < this.tabFolder.getItemCount(); i++) {
			if (this.tabFolder.getItem(i).getText().equals(Messages.getString(MessageIds.OSDE_MSGT0495))) {
				this.dataBlockTabItem = new CTabItem(this.tabFolder, SWT.CLOSE, i + 1);
				SWTResourceManager.registerResourceUser(this.dataBlockTabItem);
				break;
			}
		}
		this.dataBlockTabItem.setText(Messages.getString(MessageIds.OSDE_MSGT0515));
		{
			this.dataBlockComposite = new Composite(this.tabFolder, SWT.NONE);
			this.dataBlockComposite.setLayout(null);
			this.dataBlockTabItem.setControl(this.dataBlockComposite);
			{
				this.dataBlockDescriptionLabel = new Label(this.dataBlockComposite, SWT.CENTER);
				this.dataBlockDescriptionLabel
						.setText(Messages.getString(MessageIds.OSDE_MSGT0516));
				this.dataBlockDescriptionLabel.setFont(SWTResourceManager.getFont(OSDE.WIDGET_FONT_NAME, OSDE.WIDGET_FONT_SIZE, SWT.NORMAL, false, false));
				this.dataBlockDescriptionLabel.setBounds(12, 5, 602, 51);
			}
			{
				this.dataBlockRequiredGroup = new Group(this.dataBlockComposite, SWT.NONE);
				this.dataBlockRequiredGroup.setLayout(null);
				this.dataBlockRequiredGroup.setText(Messages.getString(MessageIds.OSDE_MSGT0517));
				this.dataBlockRequiredGroup.setFont(SWTResourceManager.getFont(OSDE.WIDGET_FONT_NAME, OSDE.WIDGET_FONT_SIZE, SWT.NORMAL, false, false));
				this.dataBlockRequiredGroup.setBounds(40, 80, 200, 170);
				this.dataBlockRequiredGroup.addPaintListener(new PaintListener() {
					public void paintControl(PaintEvent evt) {
						DevicePropertiesEditor.log.log(Level.FINEST, "dataBlockRequiredGroup.paintControl, event=" + evt); //$NON-NLS-1$
						DevicePropertiesEditor.this.dataBlockFormatCombo.select(DevicePropertiesEditor.this.dataBlockFormat == FormatTypes.TEXT ? 0 : 1);
						DevicePropertiesEditor.this.dataBlockSizeText.setText(OSDE.STRING_EMPTY + DevicePropertiesEditor.this.dataBlockSize);
						
						DevicePropertiesEditor.this.dataBlockSeparatorLabel.setEnabled(DevicePropertiesEditor.this.dataBlockFormat == FormatTypes.TEXT);
						DevicePropertiesEditor.this.dataBlockSeparatorCombo.setEnabled(DevicePropertiesEditor.this.dataBlockFormat == FormatTypes.TEXT);
						DevicePropertiesEditor.this.dataBlockSeparatorCombo.select(DevicePropertiesEditor.this.dataBlockSeparator.ordinal());
						
					}
				});
				{
					this.dataBlockFormatLabel = new Label(this.dataBlockRequiredGroup, SWT.RIGHT);
					this.dataBlockFormatLabel.setText(Messages.getString(MessageIds.OSDE_MSGT0518));
					this.dataBlockFormatLabel.setFont(SWTResourceManager.getFont(OSDE.WIDGET_FONT_NAME, OSDE.WIDGET_FONT_SIZE, SWT.NORMAL, false, false));
					this.dataBlockFormatLabel.setBounds(5, 50, 80, 20);
				}
				{
					this.dataBlockFormatCombo = new CCombo(this.dataBlockRequiredGroup, SWT.BORDER);
					this.dataBlockFormatCombo.setFont(SWTResourceManager.getFont(OSDE.WIDGET_FONT_NAME, OSDE.WIDGET_FONT_SIZE, SWT.NORMAL, false, false));
					this.dataBlockFormatCombo.setBounds(90, 50, 80, 20);
					this.dataBlockFormatCombo.setItems(StringHelper.enumValues2StringArray(FormatTypes.values()));
					this.dataBlockFormatCombo.setBackground(OpenSerialDataExplorer.COLOR_WHITE);
					this.dataBlockFormatCombo.setLayout(null);
					this.dataBlockFormatCombo.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							DevicePropertiesEditor.log.log(Level.FINEST, "dataBlockFormatCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							DevicePropertiesEditor.this.dataBlockFormat = FormatTypes.valueOf(DevicePropertiesEditor.this.dataBlockFormatCombo.getText());
							if (DevicePropertiesEditor.this.deviceConfig != null) {
								DevicePropertiesEditor.this.deviceConfig.setDataBlockFormat(DevicePropertiesEditor.this.dataBlockFormat);
								if (DevicePropertiesEditor.this.dataBlockFormat == FormatTypes.BINARY) {
									DevicePropertiesEditor.this.deviceConfig.setDataBlockSeparator(null);
								}
								else {
									DevicePropertiesEditor.this.deviceConfig.setDataBlockSeparator(DevicePropertiesEditor.this.dataBlockSeparator);
								}
							}
						}
					});
				}
				{
					this.dataBlockSizeLabel = new Label(this.dataBlockRequiredGroup, SWT.RIGHT);
					this.dataBlockSizeLabel.setText(Messages.getString(MessageIds.OSDE_MSGT0520));
					this.dataBlockSizeLabel.setFont(SWTResourceManager.getFont(OSDE.WIDGET_FONT_NAME, OSDE.WIDGET_FONT_SIZE, SWT.NORMAL, false, false));
					this.dataBlockSizeLabel.setToolTipText(Messages.getString(MessageIds.OSDE_MSGT0521));
					this.dataBlockSizeLabel.setBounds(5, 80, 80, 20);
				}
				{
					this.dataBlockSizeText = new Text(this.dataBlockRequiredGroup, SWT.RIGHT | SWT.BORDER);
					this.dataBlockSizeText.setFont(SWTResourceManager.getFont(OSDE.WIDGET_FONT_NAME, OSDE.WIDGET_FONT_SIZE, SWT.NORMAL, false, false));
					this.dataBlockSizeText.setBounds(90, 80, 40, 20);
					this.dataBlockSizeText.addKeyListener(new KeyAdapter() {
						public void keyReleased(KeyEvent evt) {
							DevicePropertiesEditor.log.log(Level.FINEST, "dataBlockSizeText.keyReleased, event=" + evt); //$NON-NLS-1$
							DevicePropertiesEditor.this.dataBlockSize = Integer.parseInt(DevicePropertiesEditor.this.dataBlockSizeText.getText());
							if (DevicePropertiesEditor.this.deviceConfig != null) {
								DevicePropertiesEditor.this.deviceConfig.setDataBlockSize(DevicePropertiesEditor.this.dataBlockSize);
							}
						}
					});
					this.dataBlockSizeText.addVerifyListener(new VerifyListener() {
						public void verifyText(VerifyEvent evt) {
							DevicePropertiesEditor.log.log(Level.FINEST, "dataBlockSizeText.verifyText, event=" + evt); //$NON-NLS-1$
							evt.doit = StringHelper.verifyTypedInput(DataTypes.INTEGER, evt.text);
						}
					});
				}
				{
					this.dataBlockSeparatorLabel = new Label(this.dataBlockRequiredGroup, SWT.RIGHT);
					this.dataBlockSeparatorLabel.setText(Messages.getString(MessageIds.OSDE_MSGT0476));
					this.dataBlockSeparatorLabel.setFont(SWTResourceManager.getFont(OSDE.WIDGET_FONT_NAME, OSDE.WIDGET_FONT_SIZE, SWT.NORMAL, false, false));
					this.dataBlockSeparatorLabel.setToolTipText(Messages.getString(MessageIds.OSDE_MSGT0329));
					this.dataBlockSeparatorLabel.setBounds(5, 110, 80, 20);
				}
				{
					this.dataBlockSeparatorCombo = new CCombo(this.dataBlockRequiredGroup, SWT.BORDER);
					this.dataBlockSeparatorCombo.setFont(SWTResourceManager.getFont(OSDE.WIDGET_FONT_NAME, OSDE.WIDGET_FONT_SIZE, SWT.BOLD, false, false));
					this.dataBlockSeparatorCombo.setBounds(90, 110, 40, 20);
					this.dataBlockSeparatorCombo.setItems(CommaSeparatorTypes.valuesAsStingArray());
					this.dataBlockSeparatorCombo.setEditable(false);
					this.dataBlockSeparatorCombo.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
					this.dataBlockSeparatorCombo.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							DevicePropertiesEditor.log.log(Level.FINEST, "dataBlockCommaSeparatorCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							DevicePropertiesEditor.this.dataBlockSeparator = CommaSeparatorTypes.fromValue(DevicePropertiesEditor.this.dataBlockSeparatorCombo.getText());
							if (DevicePropertiesEditor.this.deviceConfig != null) {
								DevicePropertiesEditor.this.deviceConfig.setDataBlockSeparator(DevicePropertiesEditor.this.dataBlockSeparator);
							}
						}
					});
				}
			}
			{
				this.dataBlockOptionalGroup = new Group(this.dataBlockComposite, SWT.NONE);
				this.dataBlockOptionalGroup.setLayout(null);
				this.dataBlockOptionalGroup.setText(Messages.getString(MessageIds.OSDE_MSGT0522));
				this.dataBlockOptionalGroup.setFont(SWTResourceManager.getFont(OSDE.WIDGET_FONT_NAME, OSDE.WIDGET_FONT_SIZE, SWT.NORMAL, false, false));
				this.dataBlockOptionalGroup.setBounds(280, 80, 300, 220);
				this.dataBlockOptionalGroup.addPaintListener(new PaintListener() {
					public void paintControl(PaintEvent evt) {
						DevicePropertiesEditor.log.log(Level.FINEST, "dataBlockOptionalGroup.paintControl, event=" + evt); //$NON-NLS-1$
						DevicePropertiesEditor.this.dataBlockEndingButton.setSelection(DevicePropertiesEditor.this.isDataBlockOptionalEndingEnabled);
						DevicePropertiesEditor.this.dataBlockEndingCombo.setEnabled(DevicePropertiesEditor.this.isDataBlockOptionalEndingEnabled);
						DevicePropertiesEditor.this.dataBlockEndingCombo.setText(DevicePropertiesEditor.this.dataBlockEnding);

						DevicePropertiesEditor.this.dataBlockCheckSumFormatButton.setSelection(DevicePropertiesEditor.this.isDataBlockOptionalChecksumEnabled);
						DevicePropertiesEditor.this.dataBlockcheckSumFormatCombo.setEnabled(DevicePropertiesEditor.this.isDataBlockOptionalChecksumEnabled);
						DevicePropertiesEditor.this.dataBlockcheckSumFormatCombo.select(DevicePropertiesEditor.this.dataBlockcheckSumFormat == FormatTypes.TEXT ? 0 : 1);
						DevicePropertiesEditor.this.dataBlockCheckSumTypeCombo.setEnabled(DevicePropertiesEditor.this.isDataBlockOptionalChecksumEnabled);
						DevicePropertiesEditor.this.dataBlockCheckSumTypeCombo.select(DevicePropertiesEditor.this.dataBlockCheckSumType == CheckSumTypes.XOR ? 0 : 1);

						DevicePropertiesEditor.this.preferredDataLocationButton.setSelection(DevicePropertiesEditor.this.isDataBlockOptionalDataLocationEnabled);
						DevicePropertiesEditor.this.preferredDataLocationText.setEnabled(DevicePropertiesEditor.this.isDataBlockOptionalDataLocationEnabled);
						DevicePropertiesEditor.this.preferredDataLocationText.setText(DevicePropertiesEditor.this.dataBlockOptionalDataLocation == null ? OSDE.STRING_EMPTY : DevicePropertiesEditor.this.dataBlockOptionalDataLocation);
						DevicePropertiesEditor.this.preferredFileExtensionButton.setSelection(DevicePropertiesEditor.this.isDataBlockOptionalFileExtentionEnabled);
						DevicePropertiesEditor.this.preferredFileExtensionText.setEnabled(DevicePropertiesEditor.this.isDataBlockOptionalFileExtentionEnabled);
						DevicePropertiesEditor.this.preferredFileExtensionText.setText(DevicePropertiesEditor.this.dataBlockOptionalFileExtention == null ? OSDE.STRING_EMPTY : DevicePropertiesEditor.this.dataBlockOptionalFileExtention);
					}
				});
				{
					this.dataBlockEndingButton = new Button(this.dataBlockOptionalGroup, SWT.CHECK | SWT.RIGHT);
					this.dataBlockEndingButton.setText(Messages.getString(MessageIds.OSDE_MSGT0468));
					this.dataBlockEndingButton.setToolTipText(Messages.getString(MessageIds.OSDE_MSGT0475));
					this.dataBlockEndingButton.setFont(SWTResourceManager.getFont(OSDE.WIDGET_FONT_NAME, OSDE.WIDGET_FONT_SIZE, SWT.NORMAL, false, false));
					this.dataBlockEndingButton.setBounds(10, 30, 120, 20);
					this.dataBlockEndingButton.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							DevicePropertiesEditor.log.log(Level.FINEST, "dataBlockEndingButton.widgetSelected, event=" + evt); //$NON-NLS-1$
							DevicePropertiesEditor.this.isDataBlockOptionalEndingEnabled = DevicePropertiesEditor.this.dataBlockEndingButton.getSelection();
							DevicePropertiesEditor.this.dataBlockEndingCombo.setEnabled(DevicePropertiesEditor.this.isDataBlockOptionalEndingEnabled);
							if (deviceConfig != null) {
								if (DevicePropertiesEditor.this.isDataBlockOptionalEndingEnabled) {
									deviceConfig.setDataBlockEnding(StringHelper.convert2ByteArray(dataBlockEnding));
								}
								else {
									deviceConfig.setDataBlockEnding(null);
								}
							}
						}
					});
				}
				{
					this.dataBlockEndingCombo = new CCombo(this.dataBlockOptionalGroup, SWT.BORDER);
					this.dataBlockEndingCombo.setFont(SWTResourceManager.getFont(OSDE.WIDGET_FONT_NAME, OSDE.WIDGET_FONT_SIZE, SWT.NORMAL, false, false));
					this.dataBlockEndingCombo.setBounds(140, 30, 60, 20);
					this.dataBlockEndingCombo.setItems(LineEndingTypes.valuesAsStingArray());
					this.dataBlockEndingCombo.setEnabled(false);
					this.dataBlockEndingCombo.addKeyListener(new KeyAdapter() {
						public void keyReleased(KeyEvent evt) {
							DevicePropertiesEditor.log.log(Level.FINEST, "dataBlockEndingText.keyReleased, event=" + evt); //$NON-NLS-1$
							DevicePropertiesEditor.this.dataBlockEnding = DevicePropertiesEditor.this.dataBlockEndingCombo.getText();
							if (DevicePropertiesEditor.this.deviceConfig != null) {
								DevicePropertiesEditor.this.deviceConfig.setDataBlockEnding(StringHelper.convert2ByteArray(DevicePropertiesEditor.this.dataBlockEnding));
							}
						}
					});
					this.dataBlockEndingCombo.addVerifyListener(new VerifyListener() {
						public void verifyText(VerifyEvent evt) {
							DevicePropertiesEditor.log.log(Level.FINEST, "dataBlockEndingText.verifyText, event=" + evt); //$NON-NLS-1$
							evt.doit = StringHelper.verifyHexAsString(evt.text);
						}
					});
				}
				{
					this.dataBlockCheckSumFormatButton = new Button(this.dataBlockOptionalGroup, SWT.CHECK | SWT.RIGHT);
					this.dataBlockCheckSumFormatButton.setText(Messages.getString(MessageIds.OSDE_MSGT0466));
					this.dataBlockCheckSumFormatButton.setFont(SWTResourceManager.getFont(OSDE.WIDGET_FONT_NAME, OSDE.WIDGET_FONT_SIZE, SWT.NORMAL, false, false));
					this.dataBlockCheckSumFormatButton.setBounds(10, 80, 120, 20);
					this.dataBlockCheckSumFormatButton.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							DevicePropertiesEditor.log.log(Level.FINEST, "dataBlockCheckSumFormatButton.widgetSelected, event=" + evt); //$NON-NLS-1$
							DevicePropertiesEditor.this.isDataBlockOptionalChecksumEnabled = DevicePropertiesEditor.this.dataBlockCheckSumFormatButton.getSelection();
							enableDataBlockOptionalChecksumPart(DevicePropertiesEditor.this.isDataBlockOptionalChecksumEnabled);
							if (deviceConfig != null) {
								if (DevicePropertiesEditor.this.isDataBlockOptionalChecksumEnabled) {
									deviceConfig.setDataBlockCheckSumFormat(dataBlockcheckSumFormat);
									deviceConfig.setDataBlockCheckSumType(dataBlockCheckSumType);
								}
								else {
									deviceConfig.setDataBlockCheckSumFormat(null);
									deviceConfig.setDataBlockCheckSumType(null);
								}
							}
						}
					});
				}
				{
					this.dataBlockcheckSumFormatCombo = new CCombo(this.dataBlockOptionalGroup, SWT.BORDER);
					this.dataBlockcheckSumFormatCombo.setFont(SWTResourceManager.getFont(OSDE.WIDGET_FONT_NAME, OSDE.WIDGET_FONT_SIZE, SWT.NORMAL, false, false));
					this.dataBlockcheckSumFormatCombo.setItems(StringHelper.enumValues2StringArray(FormatTypes.values()));
					this.dataBlockcheckSumFormatCombo.setBounds(140, 80, 90, 20);
					this.dataBlockcheckSumFormatCombo.setEditable(false);
					this.dataBlockcheckSumFormatCombo.setEnabled(false);
					this.dataBlockcheckSumFormatCombo.select(1);
					this.dataBlockcheckSumFormatCombo.setBackground(OpenSerialDataExplorer.COLOR_WHITE);
					this.dataBlockcheckSumFormatCombo.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							DevicePropertiesEditor.log.log(Level.FINEST, "dataBlockceckSumFormatCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							DevicePropertiesEditor.this.dataBlockcheckSumFormat = FormatTypes.valueOf(DevicePropertiesEditor.this.dataBlockcheckSumFormatCombo.getText());
							if (DevicePropertiesEditor.this.deviceConfig != null) {
								DevicePropertiesEditor.this.deviceConfig.setDataBlockCheckSumFormat(DevicePropertiesEditor.this.dataBlockcheckSumFormat);
							}
						}
					});
				}
				{
					this.dataBlockCheckSumTypeLabel = new Label(this.dataBlockOptionalGroup, SWT.CHECK | SWT.RIGHT);
					this.dataBlockCheckSumTypeLabel.setText(Messages.getString(MessageIds.OSDE_MSGT0467));
					this.dataBlockCheckSumTypeLabel.setFont(SWTResourceManager.getFont(OSDE.WIDGET_FONT_NAME, OSDE.WIDGET_FONT_SIZE, SWT.NORMAL, false, false));
					this.dataBlockCheckSumTypeLabel.setBounds(10, 105, 120, 20);
				}
				{
					this.dataBlockCheckSumTypeCombo = new CCombo(this.dataBlockOptionalGroup, SWT.RIGHT | SWT.BORDER);
					this.dataBlockCheckSumTypeCombo.setFont(SWTResourceManager.getFont(OSDE.WIDGET_FONT_NAME, OSDE.WIDGET_FONT_SIZE, SWT.NORMAL, false, false));
					this.dataBlockCheckSumTypeCombo.setItems(StringHelper.enumValues2StringArray(CheckSumTypes.values()));
					this.dataBlockCheckSumTypeCombo.setBounds(140, 105, 90, 20);
					this.dataBlockCheckSumTypeCombo.setEditable(false);
					this.dataBlockCheckSumTypeCombo.setEnabled(false);
					this.dataBlockCheckSumTypeCombo.select(1);
					this.dataBlockCheckSumTypeCombo.setBackground(OpenSerialDataExplorer.COLOR_WHITE);
					this.dataBlockCheckSumTypeCombo.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							DevicePropertiesEditor.log.log(Level.FINEST, "dataBlockCheckSumCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							DevicePropertiesEditor.this.dataBlockCheckSumType = CheckSumTypes.valueOf(DevicePropertiesEditor.this.dataBlockCheckSumTypeCombo.getText());
							if (DevicePropertiesEditor.this.deviceConfig != null) {
								DevicePropertiesEditor.this.deviceConfig.setDataBlockCheckSumType(DevicePropertiesEditor.this.dataBlockCheckSumType);
							}
						}
					});
				}
			{
				this.preferredDataLocationButton = new Button(this.dataBlockOptionalGroup, SWT.CHECK | SWT.RIGHT);
				this.preferredDataLocationButton.setText(Messages.getString(MessageIds.OSDE_MSGT0523));
				this.preferredDataLocationButton.setToolTipText(Messages.getString(MessageIds.OSDE_MSGT0524));
				this.preferredDataLocationButton.setFont(SWTResourceManager.getFont(OSDE.WIDGET_FONT_NAME, OSDE.WIDGET_FONT_SIZE, SWT.NORMAL, false, false));
				this.preferredDataLocationButton.setBounds(10, 150, 120, 20);
				this.preferredDataLocationButton.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent evt) {
						DevicePropertiesEditor.log.log(Level.FINEST, "preferredDataLocationButton.widgetSelected, event=" + evt); //$NON-NLS-1$
						DevicePropertiesEditor.this.isDataBlockOptionalDataLocationEnabled = DevicePropertiesEditor.this.preferredDataLocationButton.getSelection();
						DevicePropertiesEditor.this.preferredDataLocationText.setEnabled(DevicePropertiesEditor.this.isDataBlockOptionalDataLocationEnabled);
						if (deviceConfig != null) {
							if (DevicePropertiesEditor.this.isDataBlockOptionalDataLocationEnabled) {
								deviceConfig.setDataBlockPreferredDataLocation(DevicePropertiesEditor.this.dataBlockOptionalDataLocation);
							}
							else {
								deviceConfig.setDataBlockPreferredDataLocation(null);
							}
						}
					}
				});
			}
			{
				this.preferredDataLocationText = new Text(this.dataBlockOptionalGroup, SWT.BORDER | SWT.SCROLL_LINE);
				this.preferredDataLocationText.setFont(SWTResourceManager.getFont(OSDE.WIDGET_FONT_NAME, OSDE.WIDGET_FONT_SIZE, SWT.NORMAL, false, false));
				this.preferredDataLocationText.setBounds(140, 150, 140, 20);
				this.preferredDataLocationText.setEnabled(false);
				this.preferredDataLocationText.addKeyListener(new KeyAdapter() {
					public void keyReleased(KeyEvent evt) {
						DevicePropertiesEditor.log.log(Level.FINEST, "preferredDataLocationText.keyReleased, event=" + evt); //$NON-NLS-1$
						DevicePropertiesEditor.this.dataBlockOptionalDataLocation = DevicePropertiesEditor.this.preferredDataLocationText.getText();
						if (DevicePropertiesEditor.this.deviceConfig != null) {
							DevicePropertiesEditor.this.deviceConfig.setDataBlockPreferredDataLocation(DevicePropertiesEditor.this.dataBlockOptionalDataLocation);
						}
					}
				});
				this.preferredDataLocationText.addVerifyListener(new VerifyListener() {
					public void verifyText(VerifyEvent evt) {
						DevicePropertiesEditor.log.log(Level.FINEST, "preferredDataLocationText.verifyText, event=" + evt); //$NON-NLS-1$
						evt.doit = StringHelper.verifyTypedInput(DataTypes.STRING, evt.text);
					}
				});
			}
		}
			{
				this.preferredFileExtensionButton = new Button(this.dataBlockOptionalGroup, SWT.CHECK | SWT.RIGHT);
				this.preferredFileExtensionButton.setText(Messages.getString(MessageIds.OSDE_MSGT0508));
				this.preferredFileExtensionButton.setToolTipText(Messages.getString(MessageIds.OSDE_MSGT0509));
				this.preferredFileExtensionButton.setFont(SWTResourceManager.getFont(OSDE.WIDGET_FONT_NAME, OSDE.WIDGET_FONT_SIZE, SWT.NORMAL, false, false));
				this.preferredFileExtensionButton.setBounds(10, 180, 120, 20);
				this.preferredFileExtensionButton.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent evt) {
						DevicePropertiesEditor.log.log(Level.FINEST, "preferredFileExtensionButton.widgetSelected, event=" + evt); //$NON-NLS-1$
						DevicePropertiesEditor.this.isDataBlockOptionalFileExtentionEnabled = DevicePropertiesEditor.this.preferredFileExtensionButton.getSelection();
						DevicePropertiesEditor.this.preferredFileExtensionText.setEnabled(DevicePropertiesEditor.this.isDataBlockOptionalFileExtentionEnabled);
						DevicePropertiesEditor.this.dataBlockOptionalFileExtention = DevicePropertiesEditor.this.preferredFileExtensionText.getText();
						if (deviceConfig != null) {
							if (DevicePropertiesEditor.this.isDataBlockOptionalFileExtentionEnabled) {
								deviceConfig.setDataBlockPreferredFileExtention(DevicePropertiesEditor.this.dataBlockOptionalFileExtention);
							}
							else {
								deviceConfig.setDataBlockPreferredFileExtention(null);
							}
						}
					}
				});
			}
			{
				this.preferredFileExtensionText = new Text(this.dataBlockOptionalGroup, SWT.BORDER);
				this.preferredFileExtensionText.setFont(SWTResourceManager.getFont(OSDE.WIDGET_FONT_NAME, OSDE.WIDGET_FONT_SIZE, SWT.NORMAL, false, false));
				this.preferredFileExtensionText.setBounds(140, 180, 40, 20);
				this.preferredFileExtensionText.setEnabled(false);
				this.preferredFileExtensionText.addKeyListener(new KeyAdapter() {
					public void keyReleased(KeyEvent evt) {
						DevicePropertiesEditor.log.log(Level.FINEST, "preferredFileExtensionText.keyReleased, event=" + evt); //$NON-NLS-1$
						DevicePropertiesEditor.this.dataBlockOptionalFileExtention = DevicePropertiesEditor.this.preferredFileExtensionText.getText();
						if (DevicePropertiesEditor.this.deviceConfig != null) {
							DevicePropertiesEditor.this.deviceConfig.setDataBlockPreferredFileExtention(DevicePropertiesEditor.this.dataBlockOptionalFileExtention);
						}
					}
				});
				this.preferredFileExtensionText.addVerifyListener(new VerifyListener() {
					public void verifyText(VerifyEvent evt) {
						DevicePropertiesEditor.log.log(Level.FINEST, "preferredFileExtensionText.verifyText, event=" + evt); //$NON-NLS-1$
						evt.doit = StringHelper.verifyTypedInput(DataTypes.STRING, evt.text);
					}
				});
			}
		}
		
		if(this.deviceConfig != null) this.deviceConfig.addDataBlockType();
	}

	/**
	 * create a new mode state tabulator with one mode state entry
	 */
	public void createStateTabItem() {
		int index = 0;
		for (CTabItem tabItem : this.tabFolder.getItems()) {
			if (tabItem.getText().equals(Messages.getString(MessageIds.OSDE_MSGT0503))) break;
			++index;
		}
		this.stateTabItem = new CTabItem(this.tabFolder, SWT.CLOSE, index);
		SWTResourceManager.registerResourceUser(this.stateTabItem);
		this.stateTabItem.setText(Messages.getString(MessageIds.OSDE_MSGT0470));
		this.stateTabItem.setFont(SWTResourceManager.getFont(OSDE.WIDGET_FONT_NAME, OSDE.WIDGET_FONT_SIZE, SWT.NORMAL, false, false));
		{
			this.stateComposite = new Composite(this.tabFolder, SWT.NONE);
			this.stateComposite.setLayout(null);
			this.stateTabItem.setControl(this.stateComposite);
			this.stateComposite.addPaintListener(new PaintListener() {
				public void paintControl(PaintEvent evt) {
					DevicePropertiesEditor.log.log(Level.FINEST, "modeStateComposite.paintControl, event=" + evt); //$NON-NLS-1$
					for (CTabItem child : DevicePropertiesEditor.this.stateTabFolder.getItems()) {
						((PropertyTypeTabItem) child).propertyTypeComposite.redraw();
					}
				}
			});
			{
				this.stateDescriptionLabel = new Label(this.stateComposite, SWT.LEFT);
				this.stateDescriptionLabel.setText(Messages.getString(MessageIds.OSDE_MSGT0471));
				this.stateDescriptionLabel.setFont(SWTResourceManager.getFont(OSDE.WIDGET_FONT_NAME, OSDE.WIDGET_FONT_SIZE, SWT.NORMAL, false, false));
				this.stateDescriptionLabel.setBounds(165, 4, 449, 55);
			}
			{
				this.stateTabFolder = new CTabFolder(this.stateComposite, SWT.BORDER);
				this.stateTabFolder.setFont(SWTResourceManager.getFont(OSDE.WIDGET_FONT_NAME, OSDE.WIDGET_FONT_SIZE, SWT.NORMAL, false, false));
				this.stateTabFolder.setBounds(165, 65, 300, 207);
				{
						createStateTypeProperty();
				}
				this.stateTabFolder.setSelection(0);
				this.stateTabFolder.addCTabFolder2Listener(new CTabFolder2Adapter() {
					public void close(CTabFolderEvent evt) {
						DevicePropertiesEditor.log.log(Level.FINEST, "stateTabFolder.close, event=" + evt); //$NON-NLS-1$
						PropertyTypeTabItem tmpTabItem = (PropertyTypeTabItem)evt.item;
						if (DevicePropertiesEditor.this.deviceConfig != null) {
							int childIndex = DevicePropertiesEditor.this.deviceConfig.getStateType().getProperty().indexOf(tmpTabItem.propertyType);
							DevicePropertiesEditor.this.deviceConfig.removeStateType(DevicePropertiesEditor.this.deviceConfig.getStateType().getProperty().get(childIndex));
						}
						tmpTabItem.dispose();
					}
				});
			}
			{
				this.addButton = new Button(this.stateComposite, SWT.PUSH | SWT.CENTER);
				this.addButton.setText(Messages.getString(MessageIds.OSDE_MSGT0472));
				this.addButton.setFont(SWTResourceManager.getFont(OSDE.WIDGET_FONT_NAME, OSDE.WIDGET_FONT_SIZE, SWT.NORMAL, false, false));
				this.addButton.setBounds(165, 284, 300, 30);
				this.addButton.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent evt) {
						DevicePropertiesEditor.log.log(Level.FINEST, "addButton.widgetSelected, event=" + evt); //$NON-NLS-1$
						{
							createStateTypeProperty();
							DevicePropertiesEditor.this.stateTabFolder.setSelection(DevicePropertiesEditor.this.stateTabFolder.getItemCount()-1);
						}
					}
				});
			}
		}
	}

	/**
	 * creates a new state type property and display it as a PropertyTabItem
	 * appends the PropertyType to device configuration XML if necessary
	 */
	private void createStateTypeProperty() {
		PropertyType property = new ObjectFactory().createPropertyType();
		property.setName(Messages.getString(MessageIds.OSDE_MSGT0473));
		property.setType(DataTypes.INTEGER);
		property.setValue(OSDE.STRING_EMPTY + (this.stateTabFolder.getItemCount()+1));
		property.setDescription(Messages.getString(MessageIds.OSDE_MSGT0474));
		PropertyTypeTabItem tmpPropertyTypeTabItem = new PropertyTypeTabItem(DevicePropertiesEditor.this.stateTabFolder, SWT.CLOSE, Messages.getString(MessageIds.OSDE_MSGT0470), null);
		if (this.deviceConfig != null) {
			this.deviceConfig.appendStateType(property);
		}
		boolean isNoneSpecified = MeasurementPropertyTypes.isNoneSpecified(property.getName());
		tmpPropertyTypeTabItem.setProperty(deviceConfig, property, isNoneSpecified, isNoneSpecified ? MeasurementPropertyTypes.valuesAsStingArray() : null, isNoneSpecified ? DataTypes.valuesAsStingArray() : null, false);
	}

	/**
	 * create a new serial port tabulator
	 */
	public void createSerialPortTabItem() {
		this.serialPortTabItem = new SeriaPortTypeTabItem(this.tabFolder, SWT.CLOSE, 1);
	}

	/**
	 * query the Devices path to open, query, save the device properties
	 * @return
	 */
	private String getDevicesPath() {
		String osname = System.getProperty("os.name", OSDE.STRING_EMPTY).toLowerCase(); //$NON-NLS-1$
		String applHomePath = OSDE.STRING_EMPTY;
		if (osname.startsWith("windows")) { //$NON-NLS-1$
			applHomePath = (System.getenv("APPDATA") + OSDE.FILE_SEPARATOR_UNIX + OSDE.OSDE_NAME_LONG + OSDE.FILE_SEPARATOR_UNIX).replace("\\", OSDE.FILE_SEPARATOR_UNIX); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		}
		else if (osname.startsWith("linux") || osname.startsWith("mac")) { //$NON-NLS-1$ //$NON-NLS-2$
			applHomePath = System.getProperty("user.home") + OSDE.FILE_SEPARATOR_UNIX + ".OpenSerialDataExplorer" + OSDE.FILE_SEPARATOR_UNIX; //$NON-NLS-1$ //$NON-NLS-2$
		}
		return applHomePath + "Devices"; //$NON-NLS-1$
	}

	/**
	 * update internal variables by device properties
	 */
	public void update() {
		Runnable updateJob = new Runnable() {
			
			public void run() {
				//SWTResourceManager.listResourceStatus();
				//DeviceType begin
				DevicePropertiesEditor.this.deviceName = DevicePropertiesEditor.this.deviceConfig.getName();
				DevicePropertiesEditor.this.manufacturer = DevicePropertiesEditor.this.deviceConfig.getManufacturer();
				DevicePropertiesEditor.this.manufacuturerURL = DevicePropertiesEditor.this.deviceConfig.getManufacturerURL();
				DevicePropertiesEditor.this.imageFileName = DevicePropertiesEditor.this.deviceConfig.getImageFileName();
				DevicePropertiesEditor.this.isDeviceUsed = DevicePropertiesEditor.this.deviceConfig.isUsed();
				DevicePropertiesEditor.this.deviceGroup = DevicePropertiesEditor.this.deviceConfig.getDeviceGroup();
				DevicePropertiesEditor.this.devicePropsComposite.redraw();
				//DeviceType end

				//SerialPortType begin
				if (DevicePropertiesEditor.this.deviceConfig.getSerialPortType() == null && DevicePropertiesEditor.this.serialPortTabItem != null && !DevicePropertiesEditor.this.serialPortTabItem.isDisposed()) {
					DevicePropertiesEditor.this.serialPortTabItem.dispose();
					DevicePropertiesEditor.this.serialPortTabItem = null;
				}
				else if (DevicePropertiesEditor.this.deviceConfig.getSerialPortType() != null && (DevicePropertiesEditor.this.serialPortTabItem == null || DevicePropertiesEditor.this.serialPortTabItem.isDisposed())) {
					DevicePropertiesEditor.this.serialPortTabItem = new SeriaPortTypeTabItem(DevicePropertiesEditor.this.tabFolder, SWT.CLOSE, 1);
				}
				if (DevicePropertiesEditor.this.deviceConfig.getSerialPortType() != null && DevicePropertiesEditor.this.serialPortTabItem != null && !DevicePropertiesEditor.this.serialPortTabItem.isDisposed()) {
					DevicePropertiesEditor.this.serialPortTabItem.setDeviceConfig(DevicePropertiesEditor.this.deviceConfig);
				}
				//SerialPortType end

				//TimeBaseType begin
				DevicePropertiesEditor.this.timeStep_ms = DevicePropertiesEditor.this.deviceConfig.getTimeStep_ms();
				//TimeBaseType end

				//DataBlockType begin
				if (DevicePropertiesEditor.this.deviceConfig.getDataBlockType() == null && DevicePropertiesEditor.this.dataBlockTabItem != null && !DevicePropertiesEditor.this.dataBlockTabItem.isDisposed()) {
					DevicePropertiesEditor.this.dataBlockTabItem.dispose();
					DevicePropertiesEditor.this.dataBlockTabItem = null;
				}
				else {
					if (DevicePropertiesEditor.this.deviceConfig.getDataBlockType() != null) {
						if ( DevicePropertiesEditor.this.dataBlockTabItem != null && DevicePropertiesEditor.this.dataBlockTabItem.isDisposed()) {
							createDataBlockType();
						}
						DevicePropertiesEditor.this.dataBlockFormat = DevicePropertiesEditor.this.deviceConfig.getDataBlockFormat();
						DevicePropertiesEditor.this.dataBlockSize = DevicePropertiesEditor.this.deviceConfig.getDataBlockSize();
						DevicePropertiesEditor.this.dataBlockSeparator = DevicePropertiesEditor.this.deviceConfig.getDataBlockSeparator();
						DevicePropertiesEditor.this.dataBlockRequiredGroup.redraw();

						DevicePropertiesEditor.this.dataBlockEnding = StringHelper.convertHexInput(DevicePropertiesEditor.this.deviceConfig.getDataBlockEnding());
						DevicePropertiesEditor.this.isDataBlockOptionalEndingEnabled = DevicePropertiesEditor.this.deviceConfig.isDataBlockEndingDefined();
						
						DevicePropertiesEditor.this.dataBlockcheckSumFormat = DevicePropertiesEditor.this.deviceConfig.getDataBlockCheckSumFormat();
						DevicePropertiesEditor.this.isDataBlockOptionalChecksumEnabled = DevicePropertiesEditor.this.deviceConfig.isDataBlockCheckSumDefined();
						DevicePropertiesEditor.this.dataBlockCheckSumType = DevicePropertiesEditor.this.deviceConfig.getDataBlockCheckSumType();
						
						DevicePropertiesEditor.this.dataBlockOptionalDataLocation = DevicePropertiesEditor.this.deviceConfig.getDataBlockPreferredDataLocation();
						DevicePropertiesEditor.this.isDataBlockOptionalDataLocationEnabled = DevicePropertiesEditor.this.dataBlockOptionalDataLocation != null;

						DevicePropertiesEditor.this.dataBlockOptionalFileExtention = DevicePropertiesEditor.this.deviceConfig.getDataBlockPreferredFileExtention();
						DevicePropertiesEditor.this.isDataBlockOptionalFileExtentionEnabled = DevicePropertiesEditor.this.deviceConfig.isDataBlockPreferredFileExtentionDefined();
					}
				}
				//DataBlockType end

				//StateType begin
				int stateCount = (DevicePropertiesEditor.this.deviceConfig.getStateType() == null) ? 0 : DevicePropertiesEditor.this.deviceConfig.getStateSize();
				if (DevicePropertiesEditor.this.deviceConfig.getStateType() == null || (stateCount == 0 && (DevicePropertiesEditor.this.deviceConfig.getStateType() != null && !DevicePropertiesEditor.this.stateTabFolder.isDisposed()))) {
					if (DevicePropertiesEditor.this.stateTabItem != null) {
						for (CTabItem tmpPropertyTabItem : DevicePropertiesEditor.this.stateTabFolder.getItems()) {
							((PropertyTypeTabItem) tmpPropertyTabItem).dispose();
						}
						DevicePropertiesEditor.this.stateTabItem.dispose();
						DevicePropertiesEditor.this.stateTabItem = null;
					}
				}
				else {
					if (DevicePropertiesEditor.this.deviceConfig.getStateType() != null && (DevicePropertiesEditor.this.stateTabItem == null || DevicePropertiesEditor.this.stateTabItem.isDisposed())) {
						createStateTabItem();
					}
					if (DevicePropertiesEditor.this.deviceConfig.getStateType() != null && DevicePropertiesEditor.this.stateTabItem != null && !DevicePropertiesEditor.this.stateTabItem.isDisposed()) {
						if (stateCount > DevicePropertiesEditor.this.stateTabFolder.getItemCount()) {
							for (int i = DevicePropertiesEditor.this.stateTabFolder.getItemCount(); i < DevicePropertiesEditor.this.deviceConfig.getStateSize(); i++) {
								new PropertyTypeTabItem(DevicePropertiesEditor.this.stateTabFolder, SWT.CLOSE, Messages.getString(MessageIds.OSDE_MSGT0470) + DevicePropertiesEditor.this.stateTabFolder.getItemCount(), null);
							}
						}
						else if (stateCount < DevicePropertiesEditor.this.stateTabFolder.getItemCount()) {
							CTabItem[] childs = DevicePropertiesEditor.this.stateTabFolder.getItems();
							for (int i = stateCount - 1; i < childs.length; i++) {
								((PropertyTypeTabItem) childs[i]).dispose();
							}
						}
						int index = 1;
						for (CTabItem child : DevicePropertiesEditor.this.stateTabFolder.getItems()) {
							((PropertyTypeTabItem) child).setProperty(DevicePropertiesEditor.this.deviceConfig, DevicePropertiesEditor.this.deviceConfig.getStateProperty(index++), true, null, new String[]{DataTypes.INTEGER.value()}, false);
						}
						DevicePropertiesEditor.this.stateTabFolder.setSelection(0);
					}
				}
				//StateType end

				//ChannelType begin
				int channelTypeCount = DevicePropertiesEditor.this.deviceConfig.getChannelCount();
				int actualTabItemCount = DevicePropertiesEditor.this.channelConfigInnerTabFolder.getItemCount();
				if (channelTypeCount < actualTabItemCount) {
					for (int i = channelTypeCount; i < actualTabItemCount; i++) {
						ChannelTypeTabItem tmpChannelTabItem = (ChannelTypeTabItem) DevicePropertiesEditor.this.channelConfigInnerTabFolder.getItem(channelTypeCount);
						for (CTabItem tmpMeasurementTypeTabItem : tmpChannelTabItem.measurementsTabFolder.getItems()) {
							if (((MeasurementTypeTabItem) tmpMeasurementTypeTabItem).measurementPropertiesTabFolder != null) { // dispose PropertyTypes
								for (CTabItem tmpPropertyTypeTabItem : ((MeasurementTypeTabItem) tmpMeasurementTypeTabItem).measurementPropertiesTabFolder.getItems()) {
									((PropertyTypeTabItem) tmpPropertyTypeTabItem).dispose();
								}
								((MeasurementTypeTabItem) tmpMeasurementTypeTabItem).measurementPropertiesTabItem.dispose();
							}
							if (((MeasurementTypeTabItem) tmpMeasurementTypeTabItem).statisticsTypeTabItem != null) { // dispose StatisticsType
								((MeasurementTypeTabItem) tmpMeasurementTypeTabItem).statisticsTypeTabItem.dispose();
							}
							tmpMeasurementTypeTabItem.dispose();
						}
						tmpChannelTabItem.dispose();
					}
				}
				else if (channelTypeCount > actualTabItemCount) {
					for (int i = actualTabItemCount; i < channelTypeCount; i++) {
						new ChannelTypeTabItem(DevicePropertiesEditor.this.channelConfigInnerTabFolder, SWT.NONE, i);
					}
				}
				int itemCount = DevicePropertiesEditor.this.channelConfigInnerTabFolder.getItemCount();
				if (itemCount > 1)
					DevicePropertiesEditor.this.channelConfigInnerTabFolder.getItem(itemCount-1).setShowClose(true);

				for (int i = 0; i < channelTypeCount; i++) {
					ChannelTypeTabItem channelTabItem = (ChannelTypeTabItem) DevicePropertiesEditor.this.channelConfigInnerTabFolder.getItem(i);
					channelTabItem.setChannelType(DevicePropertiesEditor.this.deviceConfig, DevicePropertiesEditor.this.deviceConfig.getChannelType(i + 1), (i + 1));
				}
				//ChannelType end

				//DesktopType begin
				DevicePropertiesEditor.this.desktopInnerTabItem1.setProperty(DevicePropertiesEditor.this.deviceConfig, DevicePropertiesEditor.this.deviceConfig.getDesktopProperty(DesktopPropertyTypes.TABLE_TAB), false, null, DataTypes.valuesAsStingArray(), true);
				DevicePropertiesEditor.this.desktopInnerTabItem2.setProperty(DevicePropertiesEditor.this.deviceConfig, DevicePropertiesEditor.this.deviceConfig.getDesktopProperty(DesktopPropertyTypes.DIGITAL_TAB), false, null, DataTypes.valuesAsStingArray(), true);
				DevicePropertiesEditor.this.desktopInnerTabItem3.setProperty(DevicePropertiesEditor.this.deviceConfig, DevicePropertiesEditor.this.deviceConfig.getDesktopProperty(DesktopPropertyTypes.ANALOG_TAB), false, null, DataTypes.valuesAsStingArray(), true);
				DevicePropertiesEditor.this.desktopInnerTabItem4.setProperty(DevicePropertiesEditor.this.deviceConfig, DevicePropertiesEditor.this.deviceConfig.getDesktopProperty(DesktopPropertyTypes.VOLTAGE_PER_CELL_TAB), false, null, DataTypes.valuesAsStingArray(), true);
				DevicePropertiesEditor.this.desktopTabFolder.setSelection(0);
				//DesktopType end
				
				DevicePropertiesEditor.this.tabFolder.setSelection(0);
				//SWTResourceManager.listResourceStatus();
			}	
		};
		BusyIndicator.showWhile(this.getDisplay(), updateJob);
		updateJob.run();
		
		this.enableContextMenu(Messages.getString(MessageIds.OSDE_MSGT0487), true); //Device
	}

	/**
	 * enable or disable data block optional checksum properties
	 */
	void enableDataBlockOptionalChecksumPart(boolean enable) {
		this.dataBlockCheckSumFormatButton.setSelection(enable);
		this.dataBlockcheckSumFormatCombo.setEnabled(enable);
		this.dataBlockCheckSumTypeCombo.setEnabled(enable);
	}
	
	public void openWarningMessageBox(String errorMessage) {
  	MessageBox messageDialog = new MessageBox(dialogShell, SWT.OK | SWT.ICON_WARNING);
  	if (errorMessage.contains(OSDE.STRING_SEMICOLON)) {
			String[] messages = errorMessage.split(OSDE.STRING_SEMICOLON);
			messageDialog.setText(messages[0]);
			messageDialog.setMessage(messages[1]);
		}
  	else {
  		messageDialog.setText(OSDE.OSDE_NAME_LONG);
			messageDialog.setMessage(errorMessage);
  	}
		messageDialog.open();

	}
}
