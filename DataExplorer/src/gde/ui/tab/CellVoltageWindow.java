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
package osde.ui.tab;

import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;

import osde.data.Channel;
import osde.data.Channels;
import osde.data.Record;
import osde.data.RecordSet;
import osde.ui.OpenSerialDataExplorer;
import osde.ui.SWTResourceManager;

/**
 * Display window parent of cellVoltage displays
 * @author Winfried Brügmann
 */
public class CellVoltageWindow {
	final static Logger											log	= Logger.getLogger(CellVoltageWindow.class.getName());

	Composite												cellVoltageMainComposite, coverComposite;
	TabItem													cellVoltageTab;
	Vector<CellVoltageDisplay>			displays = new Vector<CellVoltageDisplay>();
	CLabel													infoText;
	String 													info = "Die Anzeige ist ausgeschaltet!";

	final Channels									channels;
	final TabFolder									displayTab;
	RecordSet												oldRecordSet = null;
	Channel													oldChannel = null;
	
	Vector<Integer> 								voltageVector = new Vector<Integer>();
	int 														voltageDelta = 0;
	Point 													displayCompositeSize = new Point(0,0);

	public CellVoltageWindow(TabFolder currentDisplayTab) {
		this.displayTab = currentDisplayTab;
		this.channels = Channels.getInstance();
	}

	public void create() {
		this.cellVoltageTab = new TabItem(this.displayTab, SWT.NONE);
		this.cellVoltageTab.setText("Zellenspannung");
		SWTResourceManager.registerResourceUser(this.displayTab);
		{
			this.cellVoltageMainComposite = new Composite(this.displayTab, SWT.NONE);
			this.cellVoltageTab.setControl(this.cellVoltageMainComposite);
			this.cellVoltageMainComposite.addPaintListener(new PaintListener() {
				public void paintControl(PaintEvent evt) {
					log.fine("cellVoltageMainComposite.paintControl, event=" + evt);
					updateAndResize();
				}
			});
			setActiveInfoText(this.info);
			this.infoText.addPaintListener(new PaintListener() {
				public void paintControl(PaintEvent evt) {
					log.fine("infoText.paintControl, event=" + evt);
					updateAndResize();
				}
			});
			
			this.coverComposite = new Composite(this.cellVoltageMainComposite, SWT.NONE);
			FillLayout fillLayout = new FillLayout(SWT.HORIZONTAL);
			this.coverComposite.setLayout(fillLayout);
			
			this.cellVoltageMainComposite.setBackground(OpenSerialDataExplorer.COLOR_CANVAS_YELLOW);
			this.cellVoltageMainComposite.layout();
		}
	}

	/**
	 * create new info text
	 */
	private void setActiveInfoText(String updateInfo) {
		if (this.infoText == null || this.infoText.isDisposed()) {
			this.infoText = new CLabel(this.cellVoltageMainComposite, SWT.LEFT);
			this.infoText.setBackground(OpenSerialDataExplorer.COLOR_CANVAS_YELLOW);
			this.infoText.setForeground(OpenSerialDataExplorer.COLOR_BLACK);
			this.infoText.setBounds(10, 10, 200, 30);
			this.infoText.setText(updateInfo);
		}
	}

	/**
	 * method to update the window with its children
	 */
	public void updateChilds() {
		if (log.isLoggable(Level.FINER)) log.finer("voltageValues.length = " + this.voltageVector.size() + " displays.size() = " + this.displays.size());
		if (this.voltageVector.size() > 0 && this.voltageVector.size() == this.displays.size()) { // channel does not have a record set yet
			this.voltageDelta = calculateVoltageDelta(this.voltageVector);
			for (int i = 0; i < this.voltageVector.size(); ++i) {
				this.displays.get(i).setVoltage(i + 1, this.voltageVector.get(i));
				this.displays.get(i).redraw();
				if (log.isLoggable(Level.FINE)) log.fine("setVoltage cell " + i + " - " + this.voltageVector.get(i));
			}
		}
		else {
			update();
		}
	}

	/**
	 * method to update cellVoltage window by adding removing cellVoltage displays
	 */
	public void update() {
		Channel activeChannel = this.channels.getActiveChannel();
		if (activeChannel != null) {
			RecordSet recordSet = activeChannel.getActiveRecordSet();
			// check if just created  or device switched or disabled
			if (recordSet != null && recordSet.getDevice().isVoltagePerCellTabRequested()) {
				
				updateCellVoltageVector();
				
				// if recordSet name signature changed new displays need to be created
				boolean isUpdateRequired = this.oldRecordSet == null || !recordSet.getName().equals(this.oldRecordSet.getName())
				|| this.oldChannel == null  || !this.oldChannel.getName().equals(activeChannel.getName())
						|| this.displays.size() != this.voltageVector.size();
						
				if (log.isLoggable(Level.FINE)) log.fine("isUpdateRequired = " + isUpdateRequired);
				if (isUpdateRequired) {
					// remove into text 
					if (!this.infoText.isDisposed()) this.infoText.dispose();
					// cleanup
					for (CellVoltageDisplay display : this.displays) {
						if (display != null) {
							if (!display.isDisposed()) {
								display.dispose();
								display = null;
							}
						}
					}
					this.displays.removeAllElements();
					// add new
					for (int i=0; this.voltageVector!=null && i<this.voltageVector.size(); ++i) {
						int value = this.voltageVector.get(i);
						CellVoltageDisplay display = new CellVoltageDisplay(this.coverComposite, value);
						display.create();
						if (log.isLoggable(Level.FINER)) log.finer("created cellVoltage display for " + value);
						this.displays.add(display);
					}
					this.oldRecordSet = recordSet;
					this.oldChannel = activeChannel;
					this.updateChilds();
				}
			}
			else { // clean up after device switched
				for (CellVoltageDisplay display : this.displays) {
					if (display != null) {
						if (!display.isDisposed()) {
							display.dispose();
							display = null;
						}
					}
				}
				this.displays.removeAllElements();
				if (recordSet != null && !recordSet.getDevice().isVoltagePerCellTabRequested()) {
					if (this.infoText.isDisposed()) setActiveInfoText(this.info);
					else this.infoText.setText(this.info);
				}
			}
			this.cellVoltageMainComposite.layout();
			this.coverComposite.layout();
		}
	}

	/**
	 * check cell voltage availability and build cell voltage array
	 */
	void updateCellVoltageVector() {
		this.voltageVector = new Vector<Integer>();
		Channel activeChannel = this.channels.getActiveChannel();
		if (activeChannel != null) {
			RecordSet recordSet = activeChannel.getActiveRecordSet();
			// check if just created  or device switched or disabled
			if (recordSet != null && recordSet.getDevice().isVoltagePerCellTabRequested()) {
				String[] activeRecordKeys = recordSet.getRecordNames();
				for (String recordKey : activeRecordKeys) {
					Record record = recordSet.get(recordKey);
					int index = record.getName().length();
					//if (log.isLoggable(Level.FINER)) log.finer("record " + record.getName() + " symbol " + record.getSymbol() + " - " + record.getName().substring(index-1, index));
					if (record.getSymbol().endsWith(record.getName().substring(index - 1, index))) { // better use a propperty to flag as single cell voltage
						if(record.getLast() > 0)this.voltageVector.add(record.getLast());
						//if (log.isLoggable(Level.FINER)) log.finer("record.getLast() " + record.getLast());
					}
				}
			}
		}
		if (log.isLoggable(Level.FINE)) {
			StringBuilder sb = new StringBuilder();
			for (Integer value : this.voltageVector) {
				sb.append(value).append(" ");
			}
			if (log.isLoggable(Level.FINE)) log.fine("updateCellVoltageVector -> " + sb.toString());
		}
	}

	/**
	 * calculates the voltage delta over all given cell voltages
	 * @param newValues
	 */
	private int calculateVoltageDelta(Vector<Integer> newValues) {
		int min = newValues.firstElement();
		int max = newValues.firstElement();
		for (int value : newValues) {
			if (value < min) min = value;
			else if (value > max) max = value;
		}
		return max - min;
	}

	/**
	 * @return the voltageDelta
	 */
	public int getVoltageDelta() {
		return this.voltageDelta;
	}

	/**
	 * @return the displayCompositeSize
	 */
	public Point getDisplayCompositeSize() {
		return this.displayCompositeSize;
	}

	/**
	 * 
	 */
	void updateAndResize() {
		updateCellVoltageVector();
		Point mainSize = CellVoltageWindow.this.cellVoltageMainComposite.getSize();
		if (this.voltageVector.size() > 0) {
			//log.info("mainSize = " + mainSize.toString());
			int cellWidth = mainSize.x / 6;
			int x = (6 - CellVoltageWindow.this.voltageVector.size()) * cellWidth / 2;
			int width = mainSize.x - (2 * x);
			Rectangle bounds = new Rectangle(x, mainSize.y * 10 / 100, width, mainSize.y * 80 / 100);
			//log.info("cover bounds = " + bounds.toString());
			CellVoltageWindow.this.coverComposite.setBounds(bounds);
		}
		else {
			CellVoltageWindow.this.coverComposite.setSize(0,0);
		}
		update();
	}

	/**
	 * @return the cellVoltageMainComposite
	 */
	public Composite getCellVoltageMainComposite() {
		return this.cellVoltageMainComposite;
	}
}
