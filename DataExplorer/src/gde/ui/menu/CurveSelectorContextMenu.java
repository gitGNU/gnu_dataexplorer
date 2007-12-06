package osde.ui.menu;

import java.util.logging.Logger;


import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.TableItem;

import osde.common.Channels;
import osde.common.RecordSet;
import osde.ui.OpenSerialDataExplorer;
import osde.ui.SWTResourceManager;
import osde.ui.dialog.AxisEndValuesDialog;

public class CurveSelectorContextMenu {
	private Logger							log	= Logger.getLogger(this.getClass().getName());

	private Menu								lineWidthMenu, lineTypeMenu, axisEndValuesMenu, axisNumberFormatMenu, axisPositionMenu;
	@SuppressWarnings("unused") // separator
	private MenuItem						lineVisible, lineColor, copyCurveCompare, cleanCurveCompare, separator;
	private MenuItem						lineWidth, lineWidthMenuItem1, lineWidthMenuItem2, lineWidthMenuItem3;
	private MenuItem						lineType, lineTypeMenuItem1, lineTypeMenuItem2, lineTypeMenuItem3;
	private MenuItem						axisEndValues, axisEndAuto, axisEndRound, axisStarts0, axisEndManual;
	private MenuItem						axisNumberFormat, axisNumberFormat0, axisNumberFormat1, axisNumberFormat2;
	private MenuItem						axisPosition, axisPositionLeft, axisPositionRight;
	
	private RecordSet 						recordSet;

	private AxisEndValuesDialog	axisEndValuesDialog;

	public CurveSelectorContextMenu() {
		super();
		this.axisEndValuesDialog = new AxisEndValuesDialog(OpenSerialDataExplorer.getInstance().getShell(), SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
	}
	
	public void createMenu(final OpenSerialDataExplorer application, final Menu popupmenu) {
		try {
			popupmenu.addMenuListener(new MenuListener() {
				public void menuShown(MenuEvent evt) {
					log.finest("popupmenu MenuListener " + evt);
					recordSet = Channels.getInstance().getActiveChannel().getActiveRecordSet();
					TableItem selectedItem = (TableItem) popupmenu.getData(OpenSerialDataExplorer.CURVE_SELECTION_ITEM);
					String recordNameKey = selectedItem.getText();
					lineVisible.setSelection(recordSet.getRecord(recordNameKey).isVisible());
				}

				public void menuHidden(MenuEvent evt) {
				}
			});

			lineVisible = new MenuItem(popupmenu, SWT.CHECK);
			lineVisible.setText("Kurve sichtbar");
			lineVisible.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					log.finest("lineVisibler Action performed!");
					TableItem selectedItem = (TableItem) popupmenu.getData(OpenSerialDataExplorer.CURVE_SELECTION_ITEM);
					String recordNameKey = selectedItem.getText();
					OpenSerialDataExplorer application = OpenSerialDataExplorer.getInstance();
					if (lineVisible.getSelection()) { // true
						recordSet.getRecord(recordNameKey).setVisible(true);
						selectedItem.setChecked(true);
					}
					else { // false
						recordSet.getRecord(recordNameKey).setVisible(false);
						selectedItem.setChecked(false);
					}
					application.updateGraphicsWindow();
				}
			});

			lineColor = new MenuItem(popupmenu, SWT.PUSH);
			lineColor.setText("KurvenLinienFarbe");
			lineColor.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event evt) {
					log.finest("lineColor performed! " + evt);
					TableItem selectedItem = (TableItem) popupmenu.getData(OpenSerialDataExplorer.CURVE_SELECTION_ITEM);
					String recordNameKey = selectedItem.getText();
					OpenSerialDataExplorer application = OpenSerialDataExplorer.getInstance();
					Color color = new Color(Display.getCurrent(), application.openColorDialog());
					selectedItem.setForeground(color);
					recordSet.getRecord(recordNameKey).setColor(color);
					application.updateGraphicsWindow();
				}
			});
			lineWidth = new MenuItem(popupmenu, SWT.CASCADE);
			lineWidth.setText("KurvenLinienDicke");
			lineWidthMenu = new Menu(lineWidth);
			lineWidth.setMenu(lineWidthMenu);
			lineWidthMenu.addMenuListener(new MenuListener() {
				public void menuShown(MenuEvent evt) {
					log.finest("lineWidthMenu MenuListener " + evt);
					TableItem selectedItem = (TableItem) popupmenu.getData(OpenSerialDataExplorer.CURVE_SELECTION_ITEM);
					String recordNameKey = selectedItem.getText();
					int width = recordSet.getRecord(recordNameKey).getLineWidth();
					switch (width) {
					case 1:
						lineWidthMenuItem1.setSelection(true);
						lineWidthMenuItem2.setSelection(false);
						lineWidthMenuItem3.setSelection(false);
						break;
					case 2:
						lineWidthMenuItem1.setSelection(false);
						lineWidthMenuItem2.setSelection(true);
						lineWidthMenuItem3.setSelection(false);
						break;
					case 3:
						lineWidthMenuItem1.setSelection(false);
						lineWidthMenuItem2.setSelection(false);
						lineWidthMenuItem3.setSelection(true);
						break;
					default:
						lineWidthMenuItem1.setSelection(false);
						lineWidthMenuItem2.setSelection(false);
						lineWidthMenuItem3.setSelection(false);
						break;
					}
				}

				public void menuHidden(MenuEvent evt) {
				}
			});

			lineWidthMenuItem1 = new MenuItem(lineWidthMenu, SWT.CHECK);
			lineWidthMenuItem1.setImage(SWTResourceManager.getImage("osde/resource/LineWidth1.jpg"));
			lineWidthMenuItem1.setText("  1");
			lineWidthMenuItem1.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					log.finest("Linienedicke 1");
					String recordNameKey = (String) popupmenu.getData(OpenSerialDataExplorer.RECORD_NAME);
					OpenSerialDataExplorer application = OpenSerialDataExplorer.getInstance();
					recordSet.getRecord(recordNameKey).setLineWidth(1);
					application.updateGraphicsWindow();
					lineWidthMenuItem2.setSelection(false);
					lineWidthMenuItem3.setSelection(false);
				}
			});
			lineWidthMenuItem2 = new MenuItem(lineWidthMenu, SWT.CHECK);
			lineWidthMenuItem2.setImage(SWTResourceManager.getImage("osde/resource/LineWidth2.jpg"));
			lineWidthMenuItem2.setText("  2");
			lineWidthMenuItem2.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					log.finest("Linienedicke 2");
					String recordNameKey = (String) popupmenu.getData(OpenSerialDataExplorer.RECORD_NAME);
					OpenSerialDataExplorer application = OpenSerialDataExplorer.getInstance();
					recordSet.getRecord(recordNameKey).setLineWidth(2);
					application.updateGraphicsWindow();
					lineWidthMenuItem1.setSelection(false);
					lineWidthMenuItem3.setSelection(false);
				}
			});
			lineWidthMenuItem3 = new MenuItem(lineWidthMenu, SWT.CHECK);
			lineWidthMenuItem3.setImage(SWTResourceManager.getImage("osde/resource/LineWidth3.jpg"));
			lineWidthMenuItem3.setText("  3");
			lineWidthMenuItem3.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					log.finest("Linienedicke 3");
					String recordNameKey = (String) popupmenu.getData(OpenSerialDataExplorer.RECORD_NAME);
					OpenSerialDataExplorer application = OpenSerialDataExplorer.getInstance();
					recordSet.getRecord(recordNameKey).setLineWidth(3);
					application.updateGraphicsWindow();
					lineWidthMenuItem1.setSelection(false);
					lineWidthMenuItem2.setSelection(false);
				}
			});

			lineType = new MenuItem(popupmenu, SWT.CASCADE);
			lineType.setText("KurvenLinienTyp");
			lineTypeMenu = new Menu(lineType);
			lineType.setMenu(lineTypeMenu);
			lineTypeMenu.addMenuListener(new MenuListener() {
				public void menuShown(MenuEvent evt) {
					log.finest("lineTypeMenu MenuListener " + evt);
					TableItem selectedItem = (TableItem) popupmenu.getData(OpenSerialDataExplorer.CURVE_SELECTION_ITEM);
					String recordNameKey = selectedItem.getText();
					int type = recordSet.getRecord(recordNameKey).getLineStyle();
					switch (type) {
					case SWT.LINE_SOLID:
						lineTypeMenuItem1.setSelection(true);
						lineTypeMenuItem2.setSelection(false);
						lineTypeMenuItem3.setSelection(false);
						break;
					case SWT.LINE_DASH:
						lineTypeMenuItem1.setSelection(false);
						lineTypeMenuItem2.setSelection(true);
						lineTypeMenuItem3.setSelection(false);
						break;
					case SWT.LINE_DOT:
						lineTypeMenuItem1.setSelection(false);
						lineTypeMenuItem2.setSelection(false);
						lineTypeMenuItem3.setSelection(true);
						break;
					default:
						lineTypeMenuItem1.setSelection(false);
						lineTypeMenuItem2.setSelection(false);
						lineTypeMenuItem3.setSelection(false);
						break;
					}
				}

				public void menuHidden(MenuEvent evt) {
				}
			});

			lineTypeMenuItem1 = new MenuItem(lineTypeMenu, SWT.CHECK);
			lineTypeMenuItem1.setImage(SWTResourceManager.getImage("osde/resource/LineType1.jpg"));
			lineTypeMenuItem1.setText("durchgezogen");
			lineTypeMenuItem1.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					log.finest("lineTypeMenuItem1");
					String recordNameKey = (String) popupmenu.getData(OpenSerialDataExplorer.RECORD_NAME);
					OpenSerialDataExplorer application = OpenSerialDataExplorer.getInstance();
					recordSet.getRecord(recordNameKey).setLineStyle(SWT.LINE_SOLID);
					application.updateGraphicsWindow();
					lineTypeMenuItem2.setSelection(false);
					lineTypeMenuItem3.setSelection(false);
				}
			});
			lineTypeMenuItem2 = new MenuItem(lineTypeMenu, SWT.CHECK);
			lineTypeMenuItem2.setImage(SWTResourceManager.getImage("osde/resource/LineType2.jpg"));
			lineTypeMenuItem2.setText("gestrichelt");
			lineTypeMenuItem2.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					log.finest("lineTypeMenuItem2");
					String recordNameKey = (String) popupmenu.getData(OpenSerialDataExplorer.RECORD_NAME);
					OpenSerialDataExplorer application = OpenSerialDataExplorer.getInstance();
					recordSet.getRecord(recordNameKey).setLineStyle(SWT.LINE_DASH);
					application.updateGraphicsWindow();
					lineTypeMenuItem1.setSelection(false);
					lineTypeMenuItem3.setSelection(false);
				}
			});
			lineTypeMenuItem3 = new MenuItem(lineTypeMenu, SWT.CHECK);
			lineTypeMenuItem3.setImage(SWTResourceManager.getImage("osde/resource/LineType3.jpg"));
			lineTypeMenuItem3.setText("gepunktet");
			lineTypeMenuItem3.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					log.finest("lineTypeMenuItem3");
					String recordNameKey = (String) popupmenu.getData(OpenSerialDataExplorer.RECORD_NAME);
					OpenSerialDataExplorer application = OpenSerialDataExplorer.getInstance();
					recordSet.getRecord(recordNameKey).setLineStyle(SWT.LINE_DOT);
					application.updateGraphicsWindow();
					lineTypeMenuItem1.setSelection(false);
					lineTypeMenuItem2.setSelection(false);
				}
			});
			separator = new MenuItem(popupmenu, SWT.SEPARATOR);

			axisEndValues = new MenuItem(popupmenu, SWT.CASCADE);
			axisEndValues.setText("Achsen-Endwerte");
			axisEndValuesMenu = new Menu(axisEndValues);
			axisEndValues.setMenu(axisEndValuesMenu);
			axisEndValuesMenu.addMenuListener(new MenuListener() {
				public void menuShown(MenuEvent evt) {
					log.finest("popupmenu MenuListener " + evt);
					TableItem selectedItem = (TableItem) popupmenu.getData(OpenSerialDataExplorer.CURVE_SELECTION_ITEM);
					String recordNameKey = selectedItem.getText();
					boolean isRounded = recordSet.getRecord(recordNameKey).isRoundOut();
					boolean isStart0 = recordSet.getRecord(recordNameKey).isStartpointZero();
					boolean isManual = recordSet.getRecord(recordNameKey).isStartEndDefined();

					if (isManual) {
						axisEndAuto.setSelection(false);
						axisEndRound.setSelection(false);
						axisStarts0.setSelection(false);
						axisEndManual.setSelection(true);
					}
					else if (isStart0) {
						axisEndAuto.setSelection(false);
						//axisEndRound.setSelection(false);
						axisStarts0.setSelection(true);
						axisEndManual.setSelection(false);
					}
					else if (isRounded) {
						axisEndAuto.setSelection(false);
						axisEndRound.setSelection(true);
						//axisStarts0.setSelection(false);
						axisEndManual.setSelection(false);
					}
					//					else
					//						axisEndAuto.setSelection(true);
				}

				public void menuHidden(MenuEvent evt) {
				}
			});

			axisEndAuto = new MenuItem(axisEndValuesMenu, SWT.CHECK);
			axisEndAuto.setText("automatic");
			axisEndAuto.setSelection(true);
			axisEndAuto.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					log.finest("axisEndAuto");
					String recordNameKey = (String) popupmenu.getData(OpenSerialDataExplorer.RECORD_NAME);
					OpenSerialDataExplorer application = OpenSerialDataExplorer.getInstance();
					axisStarts0.setSelection(false);
					recordSet.getRecord(recordNameKey).setStartpointZero(false);
					axisEndRound.setSelection(false);
					recordSet.getRecord(recordNameKey).setRoundOut(false);
					axisEndManual.setSelection(false);
					recordSet.getRecord(recordNameKey).setStartEndDefined(false, 0, 0);
					application.updateGraphicsWindow();
				}
			});
			axisEndRound = new MenuItem(axisEndValuesMenu, SWT.CHECK);
			axisEndRound.setText("gerundet");
			axisEndRound.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					log.finest("axisEndRound");
					String recordNameKey = (String) popupmenu.getData(OpenSerialDataExplorer.RECORD_NAME);
					OpenSerialDataExplorer application = OpenSerialDataExplorer.getInstance();
					if (axisEndRound.getSelection()) { //true
						//					axisStarts0.setSelection(false);
						//					recordSet.getRecord(recordNameKey).setStartpointZero(false);
						axisEndAuto.setSelection(false);
						recordSet.getRecord(recordNameKey).setRoundOut(true);
						axisEndManual.setSelection(false);
						recordSet.getRecord(recordNameKey).setStartEndDefined(false, 0, 0);
					}
					else { // false
						axisEndAuto.setSelection(true);
						recordSet.getRecord(recordNameKey).setRoundOut(false);
						axisEndManual.setSelection(false);
						recordSet.getRecord(recordNameKey).setStartEndDefined(false, 0, 0);
					}
					application.updateGraphicsWindow();
				}
			});
			axisStarts0 = new MenuItem(axisEndValuesMenu, SWT.CHECK);
			axisStarts0.setText("beginnt bei 0");
			axisStarts0.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					log.finest("axisStarts0");
					String recordNameKey = (String) popupmenu.getData(OpenSerialDataExplorer.RECORD_NAME);
					OpenSerialDataExplorer application = OpenSerialDataExplorer.getInstance();
					if (axisStarts0.getSelection()) { // true
						axisEndAuto.setSelection(false);
						recordSet.getRecord(recordNameKey).setStartpointZero(true);
						axisEndManual.setSelection(false);
						recordSet.getRecord(recordNameKey).setStartEndDefined(false, 0, 0);
					}
					else { // false
						axisEndAuto.setSelection(false);
						recordSet.getRecord(recordNameKey).setStartpointZero(false);
						axisEndManual.setSelection(false);
						recordSet.getRecord(recordNameKey).setStartEndDefined(false, 0, 0);
					}

					application.updateGraphicsWindow();;
				}
			});
			axisEndManual = new MenuItem(axisEndValuesMenu, SWT.CHECK);
			axisEndManual.setText("manuell");
			axisEndManual.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					log.finest("axisEndManual Action performed!");
					String recordNameKey = (String) popupmenu.getData(OpenSerialDataExplorer.RECORD_NAME);
					OpenSerialDataExplorer application = OpenSerialDataExplorer.getInstance();
					axisEndManual.setSelection(true);
					axisEndAuto.setSelection(false);
					axisStarts0.setSelection(false);
					recordSet.getRecord(recordNameKey).setStartpointZero(false);
					axisEndRound.setSelection(false);
					recordSet.getRecord(recordNameKey).setRoundOut(false);

					double[] newMinMax = axisEndValuesDialog.open();
					recordSet.getRecord(recordNameKey).setStartEndDefined(true, newMinMax[0], newMinMax[1]);
					application.updateGraphicsWindow();;
				}
			});

			axisNumberFormat = new MenuItem(popupmenu, SWT.CASCADE);
			axisNumberFormat.setText("Achsen-Zahlenformat");
			axisNumberFormatMenu = new Menu(axisNumberFormat);
			axisNumberFormat.setMenu(axisNumberFormatMenu);
			axisNumberFormatMenu.addMenuListener(new MenuListener() {
				public void menuShown(MenuEvent evt) {
					log.finest("axisNumberFormatMenu MenuListener " + evt);
					TableItem selectedItem = (TableItem) popupmenu.getData(OpenSerialDataExplorer.CURVE_SELECTION_ITEM);
					String recordNameKey = selectedItem.getText();
					int format = recordSet.getRecord(recordNameKey).getNumberFormat();
					switch (format) {
					case 0:
						axisNumberFormat0.setSelection(true);
						axisNumberFormat1.setSelection(false);
						axisNumberFormat2.setSelection(false);
						break;
					case 1:
						axisNumberFormat0.setSelection(false);
						axisNumberFormat1.setSelection(true);
						axisNumberFormat2.setSelection(false);
						break;
					case 2:
						axisNumberFormat0.setSelection(false);
						axisNumberFormat1.setSelection(false);
						axisNumberFormat2.setSelection(true);
						break;
					default:
						axisNumberFormat0.setSelection(false);
						axisNumberFormat1.setSelection(false);
						axisNumberFormat2.setSelection(false);
						break;
					}
				}

				public void menuHidden(MenuEvent evt) {
				}
			});

			axisNumberFormat0 = new MenuItem(axisNumberFormatMenu, SWT.CHECK);
			axisNumberFormat0.setText("0000");
			axisNumberFormat0.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					log.finest("axisNumberFormat0");
					String recordNameKey = (String) popupmenu.getData(OpenSerialDataExplorer.RECORD_NAME);
					recordSet.getRecord(recordNameKey).setNumberFormat(0);
					application.updateGraphicsWindow();;
				}
			});
			axisNumberFormat1 = new MenuItem(axisNumberFormatMenu, SWT.CHECK);
			axisNumberFormat1.setText("000.0");
			axisNumberFormat1.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					log.finest("axisNumberFormat1");
					String recordNameKey = (String) popupmenu.getData(OpenSerialDataExplorer.RECORD_NAME);
					recordSet.getRecord(recordNameKey).setNumberFormat(1);
					application.updateGraphicsWindow();;
				}
			});
			axisNumberFormat2 = new MenuItem(axisNumberFormatMenu, SWT.CHECK);
			axisNumberFormat2.setText("00.00");
			axisNumberFormat2.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					log.finest("axisNumberFormat2");
					String recordNameKey = (String) popupmenu.getData(OpenSerialDataExplorer.RECORD_NAME);
					recordSet.getRecord(recordNameKey).setNumberFormat(2);
					application.updateGraphicsWindow();;
				}
			});

			axisPosition = new MenuItem(popupmenu, SWT.CASCADE);
			axisPosition.setText("Achsen-Position");
			axisPositionMenu = new Menu(axisPosition);
			axisPosition.setMenu(axisPositionMenu);
			axisPositionMenu.addMenuListener(new MenuListener() {
				public void menuShown(MenuEvent evt) {
					log.finest("axisPositionMenu MenuListener " + evt);
					TableItem selectedItem = (TableItem) popupmenu.getData(OpenSerialDataExplorer.CURVE_SELECTION_ITEM);
					String recordNameKey = selectedItem.getText();
					boolean isLeft = recordSet.getRecord(recordNameKey).isPositionLeft();
					if (isLeft) {
						axisPositionLeft.setSelection(true);
						axisPositionRight.setSelection(false);
					}
					else {
						axisPositionLeft.setSelection(false);
						axisPositionRight.setSelection(true);
					}
				}

				public void menuHidden(MenuEvent evt) {
				}
			});

			axisPositionLeft = new MenuItem(axisPositionMenu, SWT.CHECK);
			axisPositionLeft.setText("linke Seite");
			axisPositionLeft.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					log.finest("axisPositionLeft Action performed!");
					String recordNameKey = (String) popupmenu.getData(OpenSerialDataExplorer.RECORD_NAME);
					recordSet.getRecord(recordNameKey).setPositionLeft(true);
					application.updateGraphicsWindow();;
				}
			});
			axisPositionRight = new MenuItem(axisPositionMenu, SWT.CHECK);
			axisPositionRight.setText("rechte Seite");
			axisPositionRight.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					log.finest("axisPositionRight Action performed!");
					String recordNameKey = (String) popupmenu.getData(OpenSerialDataExplorer.RECORD_NAME);
					recordSet.getRecord(recordNameKey).setPositionLeft(false);
					application.updateGraphicsWindow();;
				}
			});

			separator = new MenuItem(popupmenu, SWT.SEPARATOR);

			copyCurveCompare = new MenuItem(popupmenu, SWT.CHECK);
			copyCurveCompare.setText("Kopiere Kurvenvergleich");
			copyCurveCompare.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					log.finest("copyCurveCompare Action performed!");
					String recordNameKey = (String) popupmenu.getData(OpenSerialDataExplorer.RECORD_NAME);
					application.getCompareSet().put(recordNameKey, recordSet.get(recordNameKey));
					application.updateCompareWindow();
				}
			});
			cleanCurveCompare = new MenuItem(popupmenu, SWT.CHECK);
			cleanCurveCompare.setText("Lösche Kurvenvergleich");
			cleanCurveCompare.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					log.finest("cleanCurveCompare Action performed!");
					application.getCompareSet().clear();
					application.updateCompareWindow();
				}
			});
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
}
