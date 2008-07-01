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

import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;

import osde.ui.OpenSerialDataExplorer;
import osde.ui.SWTResourceManager;

/**
 * Child display class displaying cell voltage display
 * @author Winfried Brügmann
 */
public class CellVoltageDisplay extends Composite {

	{
		//Register as a resource user - SWTResourceManager will
		//handle the obtaining and disposing of resources
		SWTResourceManager.registerResourceUser(this);
	}

	final static Logger	log						= Logger.getLogger(CellVoltageDisplay.class.getName());

	CLabel							textLabel;
	CLabel							actualDigitalLabel;
	Canvas							cellCanvas;
	Composite						fillRight;
	Composite						fillLeft;
	Composite						cellComposite;

	int									voltage;
	String							displayText1	= "Zelle ";
	String							displayText2	= " Spannung [ V ]";
	String							displayText		= this.displayText1 + "?" + this.displayText2;

	public CellVoltageDisplay(Composite cellVoltageMainComposite, int value) {
		super(cellVoltageMainComposite, SWT.BORDER);
		this.voltage = value;
		this.setBackground(OpenSerialDataExplorer.COLOR_CANVAS_YELLOW);
		GridLayout mainCompositeLayout = new GridLayout();
		mainCompositeLayout.makeColumnsEqualWidth = true;
		mainCompositeLayout.marginHeight = 0;
		mainCompositeLayout.marginWidth = 0;
		this.setLayout(mainCompositeLayout);
	}

	public void create() {
		{
			this.textLabel = new CLabel(this, SWT.CENTER | SWT.EMBEDDED);
			this.textLabel.setFont(SWTResourceManager.getFont("Microsoft Sans Serif", 12, 1, false, false));
			this.textLabel.setBackground(OpenSerialDataExplorer.COLOR_CANVAS_YELLOW);
			this.textLabel.setText(this.displayText);
			GridData text1LData = new GridData();
			text1LData.horizontalAlignment = GridData.FILL;
			text1LData.grabExcessHorizontalSpace = true;
			this.textLabel.setLayoutData(text1LData);
		}
		{
			this.actualDigitalLabel = new CLabel(this, SWT.CENTER | SWT.EMBEDDED);
			this.actualDigitalLabel.setBackground(OpenSerialDataExplorer.COLOR_CANVAS_YELLOW);
			this.actualDigitalLabel.setText("0,00");
			this.actualDigitalLabel.setFont(SWTResourceManager.getFont("Microsoft Sans Serif", 32, 0, false, false));
			GridData actualDigitalLabelLData = new GridData();
			actualDigitalLabelLData.horizontalAlignment = GridData.FILL;
			actualDigitalLabelLData.grabExcessHorizontalSpace = true;
			this.actualDigitalLabel.setLayoutData(actualDigitalLabelLData);
			//this.actualDigitalLabel.setBounds(0, 60, 50, 60);
		}
		{
			this.cellComposite = new Composite(this, SWT.NONE);
			GridData canvas1LData = new GridData();
			canvas1LData.horizontalAlignment = GridData.FILL;
			canvas1LData.grabExcessHorizontalSpace = true;
			canvas1LData.grabExcessVerticalSpace = true;
			canvas1LData.verticalAlignment = GridData.FILL;
			this.cellComposite.setLayoutData(canvas1LData);
			FillLayout canvas1Layout = new FillLayout(org.eclipse.swt.SWT.HORIZONTAL);
			canvas1Layout.marginHeight = 0;
			canvas1Layout.marginWidth = 0;
			this.cellComposite.setLayout(canvas1Layout);
			{
				this.fillLeft = new Composite(this.cellComposite, SWT.NONE);
				this.fillLeft.setDragDetect(false);
				this.fillLeft.setEnabled(false);
				this.fillLeft.setBackground(OpenSerialDataExplorer.COLOR_CANVAS_YELLOW);
			}
			{
				this.cellCanvas = new Canvas(this.cellComposite, SWT.NONE);
				//this.cellCanvas.setBackground(OpenSerialDataExplorer.COLOR_CANVAS_YELLOW);
				this.cellCanvas.setDragDetect(false);
				this.cellCanvas.addPaintListener(new PaintListener() {
					public void paintControl(PaintEvent evt) {
						CellVoltageDisplay.log.fine("cellCanvas.paintControl, evt = " + evt);
						voltagePaintControl(evt);
					}
				});
			}
			{
				this.fillRight = new Composite(this.cellComposite, SWT.NONE);
				this.fillRight.setEnabled(false);
				this.fillRight.setDragDetect(false);
				this.fillRight.setBackground(OpenSerialDataExplorer.COLOR_CANVAS_YELLOW);
			}
		}
		this.addPaintListener(new PaintListener() {
			public void paintControl(PaintEvent evt) {
				CellVoltageDisplay.log.fine("mainComposite.paintControl, evt = " + evt);
				CellVoltageDisplay.this.cellCanvas.redraw();
			}
		});
		this.layout();
	}

	/**
	 * @param newVoltage the voltage to set
	 */
	public void setVoltage(int cellNumber, int newVoltage) {
		boolean isUpdateRequired = false;
		if (!this.displayText.equals(this.displayText1 + cellNumber + this.displayText2)) {
			this.displayText = this.displayText1 + cellNumber + this.displayText2;
			isUpdateRequired = true;
		}
		if (this.voltage != newVoltage) {
			this.voltage = newVoltage;
			isUpdateRequired = true;
		}

		if (isUpdateRequired) {
			this.cellComposite.layout();
		}
	}

	/**
	 * 
	 */
	void voltagePaintControl(PaintEvent evt) {
		this.textLabel.setText(this.displayText);

		String valueText = String.format("%.2f", new Double(this.voltage / 1000.0));
		this.actualDigitalLabel.setText(valueText);

		Canvas canvas = (Canvas) evt.widget;
		GC gc = SWTResourceManager.getGC(canvas, CellVoltageDisplay.this.displayText);
		Rectangle rect = canvas.getClientArea();
		CellVoltageDisplay.log.info("cellCanvas.getBounds = " + rect);

		int baseVoltage = 2500;
		//		int cellVoltageDelta = CellVoltageDisplay.this.parent.getVoltageDelta();
		//		if (cellVoltageDelta < 200 && cellVoltageDelta != 0) 
		//			baseVoltage = 1000;

		int height = rect.height; // 4,2 - 2  = 2,2 (max voltage - min voltage)

		Double delta = (4200.0 - CellVoltageDisplay.this.voltage) * (height - 20) / baseVoltage;
		int top = delta.intValue();

		rect = new Rectangle(0, top, rect.width-1, height-1-top);

		if (CellVoltageDisplay.this.voltage < 2600 || CellVoltageDisplay.this.voltage > 4200)
			gc.setBackground(SWTResourceManager.getColor(SWT.COLOR_RED));
		else if (CellVoltageDisplay.this.voltage >= 2600 && CellVoltageDisplay.this.voltage < 4200)
			gc.setBackground(SWTResourceManager.getColor(SWT.COLOR_YELLOW));
		else
			// == 4200
			gc.setBackground(SWTResourceManager.getColor(SWT.COLOR_GREEN));

		CellVoltageDisplay.log.info("fillRectangle = " + rect);
		gc.fillRectangle(rect);
		gc.setForeground(SWTResourceManager.getColor(SWT.COLOR_BLACK));
		//gc.drawLine(0, 0, rect.width, rect.height+top);
		gc.drawLine(0, top, rect.width, top);
		gc.drawRectangle(0, 0, rect.width, rect.height+top);
	}
}
