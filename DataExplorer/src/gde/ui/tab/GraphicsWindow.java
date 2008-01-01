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
package osde.ui.tab;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import osde.data.Channels;
import osde.data.Record;
import osde.data.RecordSet;
import osde.device.IDevice;
import osde.ui.OpenSerialDataExplorer;
import osde.ui.SWTResourceManager;
import osde.ui.menu.CurveSelectorContextMenu;
import osde.utils.CurveUtils;
import osde.utils.TimeLine;


/**
* This code was edited or generated using CloudGarden's Jigloo
* SWT/Swing GUI Builder, which is free for non-commercial
* use. If Jigloo is being used commercially (ie, by a corporation,
* company or business for any purpose whatever) then you
* should purchase a license for each developer using Jigloo.
* Please visit www.cloudgarden.com for details.
* Use of Jigloo implies acceptance of these licensing terms.
* A COMMERCIAL LICENSE HAS NOT BEEN PURCHASED FOR
* THIS MACHINE, SO JIGLOO OR THIS CODE CANNOT BE USED
* LEGALLY FOR ANY CORPORATE OR COMMERCIAL PURPOSE.
*/
/**
 * This class defines the main graphics window as a sash form of a curve selection table and a drawing canvas
 * @author Winfried Brügmann
 */
public class GraphicsWindow {
	private Logger												log											= Logger.getLogger(this.getClass().getName());

	public static final int								TYPE_NORMAL							= 0;
	public static final int								TYPE_COMPARE						= 1;
	public static final String						WINDOW_TYPE							= "window_type";

	private final TabFolder								displayTab;
	private SashForm											graphicSashForm;
	// Curve Selector Table with popup menu
	private Composite											curveSelector;
	private CLabel												curveSelectorHeader;
	private Table													curveSelectorTable;
	private TableColumn										tableSelectorColumn;
	private TabItem												graphic;
	private Menu													popupmenu;
	private CurveSelectorContextMenu			contextMenu;
	// drawing canvas
	private Canvas												graphicCanvas;

	private final OpenSerialDataExplorer	application;
	private final Channels								channels;
	private final String									name;
	private final TimeLine								timeLine								= new TimeLine();
	private final int											type;
	private boolean												isCurveSelectorEnabled	= true;
	
	private int														downX = 0;
	private int														upX = 0;
	private int														lastEvtX = 0;
	private int														downY = 0;
	private int														upY = 0;
	private int														lastEvtY = 0;
	


	public GraphicsWindow(TabFolder displayTab, int type, String name) {
		this.displayTab = displayTab;
		this.type = type;
		this.name = name;
		this.application = OpenSerialDataExplorer.getInstance();
		this.channels = Channels.getInstance();
	}

	public void create() {
		graphic = new TabItem(displayTab, SWT.NONE);
		graphic.setText(name);
		{ // graphicSashForm
			graphicSashForm = new SashForm(displayTab, SWT.HORIZONTAL);
			graphic.setControl(graphicSashForm);
			GridLayout sashForm1Layout2 = new GridLayout();
			sashForm1Layout2.makeColumnsEqualWidth = true;
			graphicSashForm.setLayout(sashForm1Layout2);
			{ // curveSelector
				curveSelector = new Composite(graphicSashForm, SWT.BORDER);
				FormLayout curveSelectorLayout = new FormLayout();
				curveSelector.setLayout(curveSelectorLayout);
				GridData curveSelectorLData = new GridData();
				curveSelector.setLayoutData(curveSelectorLData);
				{
					curveSelectorHeader = new CLabel(curveSelector, SWT.NONE);
					curveSelectorHeader.setText("Kurvenselektor");
					//curveSelectorHeader.setFont(SWTResourceManager.getFont("Microsoft Sans Serif", 10, 1, false, false));
					FormData curveSelectorHeaderLData = new FormData();
					curveSelectorHeaderLData.width = 145;
					curveSelectorHeaderLData.height = 26;
					curveSelectorHeaderLData.left = new FormAttachment(0, 1000, 0);
					curveSelectorHeaderLData.top = new FormAttachment(0, 1000, 3);
					curveSelectorHeader.setLayoutData(curveSelectorHeaderLData);
					curveSelectorHeader.setBackground(OpenSerialDataExplorer.COLOR_LIGHT_GREY);
				}
				{
					curveSelectorTable = new Table(curveSelector, SWT.SINGLE | SWT.CHECK);
					curveSelectorTable.setLinesVisible(true);
					FormData curveTableLData = new FormData();
					curveTableLData.width = 82;
					curveTableLData.height = 457;
					curveTableLData.left = new FormAttachment(0, 1000, 4);
					curveTableLData.top = new FormAttachment(0, 1000, 37);
					curveTableLData.bottom = new FormAttachment(1000, 1000, 0);
					curveTableLData.right = new FormAttachment(1000, 1000, 0);
					curveSelectorTable.setLayoutData(curveTableLData);

					popupmenu = new Menu(application.getShell(), SWT.POP_UP);
					curveSelectorTable.setMenu(popupmenu);
					curveSelectorTable.layout();
					contextMenu = new CurveSelectorContextMenu();
					contextMenu.createMenu(popupmenu);
					curveSelectorTable.addPaintListener(new PaintListener() {
						public void paintControl(PaintEvent evt) {
							log.finest("curveTable.paintControl, event=" + evt);
							Point size = curveSelectorTable.getSize();
							tableSelectorColumn.setWidth(size.x);
						}
					});
					curveSelectorTable.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.finest("curveTable.widgetSelected, event=" + evt);
							TableItem item = (TableItem) evt.item;
							String recordName = ((TableItem) evt.item).getText();
							log.fine("selected = " + recordName);
							popupmenu.setData(OpenSerialDataExplorer.RECORD_NAME, recordName);
							popupmenu.setData(OpenSerialDataExplorer.CURVE_SELECTION_ITEM, evt.item);
							if (item.getChecked() != (Boolean) item.getData(OpenSerialDataExplorer.OLD_STATE)) {
								Record activeRecord;
								switch (type) {
								case TYPE_COMPARE:
									activeRecord = application.getCompareSet().getRecord(recordName);
									break;

								default:
									activeRecord = channels.getActiveChannel().getActiveRecordSet().getRecord(recordName);
									break;
								}
								if (item.getChecked()) {
									activeRecord.setVisible(true);
									popupmenu.getItem(0).setSelection(true);
									item.setData(OpenSerialDataExplorer.OLD_STATE, (boolean) true);
									item.setData(WINDOW_TYPE, type);
									graphicCanvas.redraw();
								}
								else {
									activeRecord.setVisible(false);
									popupmenu.getItem(0).setSelection(false);
									item.setData(OpenSerialDataExplorer.OLD_STATE, (boolean) false);
									item.setData(WINDOW_TYPE, type);
									graphicCanvas.redraw();
								}
							}
						}
					});
					{
						tableSelectorColumn = new TableColumn(curveSelectorTable, SWT.LEFT);
						tableSelectorColumn.setWidth(100);
					}
				}
			} // curveSelector
			{ // graphicCanvas
				graphicCanvas = new Canvas(graphicSashForm, SWT.NONE);
				FillLayout graphicCanvasLayout = new FillLayout(org.eclipse.swt.SWT.HORIZONTAL);
				graphicCanvas.setLayout(graphicCanvasLayout);
				graphicCanvas.addMouseMoveListener(new MouseMoveListener() {
					public void mouseMove(MouseEvent evt) {
						log.finest("graphicCanvas.mouseMove = " + evt);
						if (evt.stateMask == 524288) {
							Canvas canvas = (Canvas) evt.widget;
							//GC gc = new GC (canvas);
							GC gc = SWTResourceManager.getGC(canvas);
							gc.setLineWidth (1);
							if (downX < evt.x && downY < evt.y) {
								gc.drawRectangle(downX, downY, evt.x - downX, evt.y - downY);
								canvas.redraw(downX + 1, downY + 1, evt.x - downX - 1, evt.y - downY - 1, false);
								canvas.redraw(evt.x + 1, downY, lastEvtX - evt.x, lastEvtY + 1 - downY, false);
								canvas.redraw(downX, evt.y + 1, lastEvtX + 1 - downX, lastEvtY - evt.y + 1, false);
							}
							else if (downX < evt.x && downY > evt.y) {
								gc.drawRectangle(downX, evt.y, evt.x - downX, downY - evt.y);
								canvas.redraw(downX + 1, evt.y + 1, evt.x - downX - 1, downY - evt.y - 1, false);
								canvas.redraw(downX, lastEvtY, lastEvtX + 1 - downX, evt.y - lastEvtY, false);
								canvas.redraw(evt.x + 1, evt.y, lastEvtX - evt.x, downY - evt.y + 1, false);
							}
							else if (downX > evt.x && downY < evt.y) {
								gc.drawRectangle(evt.x, downY, downX - evt.x , evt.y - downY);
								canvas.redraw(evt.x + 1, downY + 1, downX - evt.x - 1, evt.y - downY - 1, false);
								canvas.redraw(lastEvtX, downY, evt.x - lastEvtX, lastEvtY - downY + 1, false);
								canvas.redraw(lastEvtX, evt.y + 1, downX - lastEvtX + 1, lastEvtY - evt.y, false);
							}
							else if (downX > evt.x && downY > evt.y) {
								gc.drawRectangle(evt.x, evt.y, downX - evt.x, downY - evt.y);
								canvas.redraw(evt.x + 1, evt.y + 1, downX - evt.x - 1, downY - evt.y - 1, false);
								canvas.redraw(lastEvtX, lastEvtY, downX - lastEvtX + 1, evt.y - lastEvtY, false);
								canvas.redraw(lastEvtX, lastEvtY, evt.x - lastEvtX, downY - lastEvtY + 1, false);
							}
							lastEvtX = evt.x;
							lastEvtY = evt.y;
							//gc.dispose ();
						}
						//TODO add your code for graphicCanvas.mouseMove
					}
				});
				graphicCanvas.addMouseTrackListener(new MouseTrackAdapter() {
					public void mouseExit(MouseEvent evt) {
						System.out.println("graphicCanvas.mouseExit, event="+evt);
						//TODO add your code for graphicCanvas.mouseExit
					}
					public void mouseEnter(MouseEvent evt) {
						System.out.println("graphicCanvas.mouseEnter, event="+evt);
						//TODO add your code for graphicCanvas.mouseEnter
					}
				});
				graphicCanvas.addMouseListener(new MouseAdapter() {
					public void mouseDown(MouseEvent evt) {
						System.out.println("graphicCanvas.mouseDown, event="+evt);
						downX = evt.x;
						downY = evt.y;
					}
					public void mouseUp(MouseEvent evt) {
						System.out.println("graphicCanvas.mouseUp, event="+evt);
						upX = evt.x;
						upY = evt.y;
						RecordSet recordSet = channels.getActiveChannel().getActiveRecordSet();
						if (recordSet!= null && downX < upX) { // zoom
							if (downY < upY) 
								channels.getActiveChannel().getActiveRecordSet().setZoomValues(downX, downY, upX, upY, true);
							else
								channels.getActiveChannel().getActiveRecordSet().setZoomValues(downX, upY, upX, downY, true);
						}
						else {
							if (recordSet!= null) recordSet.setZoomed(false);
						}
					}
				});
				graphicCanvas.setBackground(OpenSerialDataExplorer.COLOR_CANVAS_YELLOW); // light yellow
				graphicCanvas.addPaintListener(new PaintListener() {
					public void paintControl(PaintEvent evt) {
						drawAreaPaintControl(evt);
					}
				});
			} // graphicCanvas
			graphicSashForm.setWeights(new int[] { 9, 100 }); // 9:100  -> 9 == width required for curveSelectorTable
		} // graphicSashForm
	}

	/**
	 * this method is called in case of an paint event (redraw) and draw the containing records 
	 * @param evt
	 */
	private void drawAreaPaintControl(PaintEvent evt) {
		log.finest("drawAreaPaintControl.paintControl, event=" + evt);
		// Get the canvas and its dimensions
		Canvas canvas = (Canvas) evt.widget;
		int maxX = canvas.getSize().x - 5; // enable a small gap if no axis is shown 
		int maxY = canvas.getSize().y;
		log.fine("canvas size = " + maxX + " x " + maxY);

		//set the font to be used
		// do not use extra font since the OS window manager has control
		//evt.gc.setFont(this.font);

		switch (type) {
		case TYPE_COMPARE:
			if (application.getCompareSet() != null && application.getCompareSet().size() > 0) {
				drawCurves(application.getCompareSet(), evt.gc, canvas, maxX, maxY);
			}
			break;

		default: // TYPE_NORMAL
			if (channels.getActiveChannel() != null && channels.getActiveChannel().getActiveRecordSet() != null) {
				drawCurves(channels.getActiveChannel().getActiveRecordSet(), evt.gc, canvas, maxX, maxY);
			}
			break;
		}

	}

	/**
	 * method to draw the curves with it scales
	 * @param recordSet
	 * @param gc
	 * @param canvas
	 * @param maxX
	 * @param maxY
	 */
	private void drawCurves(RecordSet recordSet, GC gc, Canvas canvas, int maxX, int maxY) {
		int[] timeScale = timeLine.getScaleMaxTimeNumber(recordSet);
		int maxTimeNumber = timeScale[0];
		int scaleFactor = timeScale[1];

		//prepare measurement scales
		int numberCurvesRight = 0;
		int numberCurvesLeft = 0;
		String[] recordNames = recordSet.getRecordNames();
		for (String string : recordNames) {
			if (recordSet.getRecord(string).isVisible()) {
				if (recordSet.getRecord(string).isPositionLeft())
					numberCurvesLeft++;
				else
					numberCurvesRight++;
			}
		}
		if (recordSet.isCompareSet()) {
			numberCurvesLeft = numberCurvesLeft > 0 ? 1 : 0;
			numberCurvesRight = numberCurvesRight > 0 ? 1 : 0;
		}
		log.fine("nCurveLeft=" + numberCurvesLeft + ", nCurveRight=" + numberCurvesRight);

		// Calculate the horizontal area to used for plotting graphs
		int maxTime = maxTimeNumber; // alle 10 min/sec eine Markierung
		Point pt = gc.textExtent("000,00");
		int dataScaleWidth = pt.x + pt.y * 2 + 5; // space used for text and scales with description or legend
		int spaceLeft = numberCurvesLeft * dataScaleWidth;
		int spaceRight = numberCurvesRight * dataScaleWidth;
		int x0 = (int) maxX - (maxX - spaceLeft) + 5; // enable a small gap if no axis is shown
		int xMax = (int) maxX - spaceRight;
		int fitTimeWidth = (xMax - x0) - ((xMax - x0) % 10); // make time line modulo 10 to enable every 10 min/sec a tick mark
		int width = fitTimeWidth; // make the time width  the width for the curves
		xMax = x0 + width;

		int verticalSpace = 3 * pt.y;// space used for text and scales with description or legend
		int spaceTop = 20;
		int spaceBot = verticalSpace;
		int y0 = (int) maxY - spaceBot;
		int yMax = (int) maxY - (maxY - spaceTop);
		int height = (y0 - yMax) - (y0 - yMax) % 10; // make modulo 20
		yMax = y0 - height;
		if (log.isLoggable(Level.FINE)) log.fine("draw area x0=" + x0 + ", y0=" + y0 + ",xMax=" + xMax + ", yMax=" + yMax + "width=" + width + ", height=" + height + ", timeWidth=" + fitTimeWidth);

		// draw x coordinate		
		timeLine.drawTimeLine(gc, x0, y0, fitTimeWidth, maxTime, scaleFactor, OpenSerialDataExplorer.COLOR_BLACK);

		// draw clipping bounding 
		gc.setForeground(OpenSerialDataExplorer.COLOR_LIGHT_GREY);
		gc.drawLine(x0, yMax, xMax, yMax);
		gc.drawLine(x0, y0, x0, yMax);
		gc.drawLine(xMax, y0, xMax, yMax);

		// draw curves for each active record
		recordSet.setCurveBounds(new Rectangle(x0, y0-height, width, height));
		log.info("curve bounds = " + x0 + " " + (y0-height) + " " + width + " " + height);
		for (String record : recordSet.getRecordNames()) {
			Record actualRecord = recordSet.getRecord(record);
			log.fine("drawing record = " + actualRecord.getName());
			if (actualRecord.isVisible() && actualRecord.isDisplayable()) CurveUtils.draw(actualRecord, gc, x0, y0, width, height, dataScaleWidth);
			gc.setClipping(0, 0, canvas.getSize().x, canvas.getSize().y);
		}
	}

	/**
	 * redraws the graphics canvas as well as the curve selector table
	 */
	public void redrawGrahics() {
		OpenSerialDataExplorer.display.asyncExec(new Runnable() {
			public void run() {
				updateCurveSelectorTable();
				graphicCanvas.redraw();
			}
		});
	}

	/**
	 * method to update the curves displayed in the curve selector panel 
	 */
	public void updateCurveSelectorTable() {
		final IDevice device = application.getActiveDevice();
		final RecordSet recordSet = type == TYPE_NORMAL ? channels.getActiveChannel().getActiveRecordSet() : application.getCompareSet();

		if (isCurveSelectorEnabled && recordSet != null) {
			OpenSerialDataExplorer.display.asyncExec(new Runnable() {
				public void run() {
					curveSelectorTable.removeAll();
					
					String[] recordKeys = device.getMeasurementNames();
					for (int i = 0; i < recordSet.size(); i++) {
						Record record;
						switch (type) {
						case TYPE_COMPARE:
							String recordKey = recordSet.getRecordNames()[0].split("_")[0];
							record = recordSet.getRecord(recordKey + "_" + i);
							break;

						default: // TYPE_NORMAL
							record = recordSet.getRecord(recordKeys[i]);
							break;
						}
						if (log.isLoggable(Level.FINER)) log.finer(record.getName());

						TableItem item = new TableItem(curveSelectorTable, SWT.NULL);
						item.setForeground(record.getColor());
						item.setText(type == TYPE_NORMAL ? record.getName() : record.getName() + "_" + i);
						//item.setImage(SWTResourceManager.getImage("osde/resource/LineWidth1.jpg"));
						if (record.isDisplayable()) {
							if (record.isVisible()) {
								item.setChecked(true);
								item.setData(OpenSerialDataExplorer.OLD_STATE, (boolean) true);
								item.setData(WINDOW_TYPE, type);
							}
							else {
								item.setChecked(false);
								item.setData(OpenSerialDataExplorer.OLD_STATE, (boolean) false);
								item.setData(WINDOW_TYPE, type);
							}
						}
						else {
							item.setChecked(false);
							item.setData(OpenSerialDataExplorer.OLD_STATE, (boolean) false);
							item.setData(WINDOW_TYPE, type);
							item.dispose();
						}
					}
				}
			});
		}
		else curveSelectorTable.removeAll();
	}

	public Canvas getGraphicCanvas() {
		return graphicCanvas;
	}

	public boolean isCurveSelectorEnabled() {
		return isCurveSelectorEnabled;
	}

	public void setCurveSelectorEnabled(boolean isCurveSelectorEnabled) {
		this.isCurveSelectorEnabled = isCurveSelectorEnabled;
	}

	public SashForm getGraphicSashForm() {
		return graphicSashForm;
	}
}
