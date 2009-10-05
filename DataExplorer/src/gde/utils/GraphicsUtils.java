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
package osde.utils;

import java.text.DecimalFormat;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;

import osde.data.Record;
import osde.data.RecordSet;
import osde.ui.OpenSerialDataExplorer;
import osde.ui.SWTResourceManager;


/**
 * Class contains utility methods for drawing curves with scales right hand side and left hand side
 * @author Winfried Brügmann
 */
public class GraphicsUtils {
	private static Logger log = Logger.getLogger(GraphicsUtils.class.getName());
	
	static OpenSerialDataExplorer application = OpenSerialDataExplorer.getInstance();

	/**
	 * draws tick marks to a scale in vertical direction (plus 90 degrees)
	 * @param record
	 * @param gc graphics context
	 * @param y0 start point in y horizontal direction
	 * @param x0 start point in x vertical direction
	 * @param height in points where the ticks should be drawn
	 * @param minValue the number where the scale should start to count
	 * @param maxValue the number where the scale should start to count, endNumber - startNumber -> number of ticks drawn
	 * @param ticklength of the main ticks
	 * @param miniticks number of mini ticks drawn between the main ticks
	 * @param gap distance between ticks and the number scale
	 * @param isPositionLeft position of to be drawn scale
	 * @param numberTickmarks 
	 */
	public static void drawVerticalTickMarks(Record record, GC gc, int x0, int y0, int height, double minValue, double maxValue, int ticklength, int miniticks, int gap, boolean isPositionLeft, int numberTickmarks) {

		int yTop = y0-height+1;
		double deltaScale = (maxValue - minValue);
		int numberTicks = numberTickmarks == 0 ? height / 50 >= 2 ? height / 50 : 1 : numberTickmarks; // initial start value 
		int maxNumberTicks = height / 25 >= 2 ? height / 25 : 1;
		double deltaScaleValue = deltaScale;
		double minScaleValue, maxScaleValue;
		if (record.isRoundOut() || record.isStartEndDefined()) {
			minScaleValue = minValue;
			maxScaleValue = maxValue;
		}
		else {
			Object[] roundResult = MathUtils.adaptRounding(minValue, maxValue, true, maxNumberTicks);
			minScaleValue = (Double)roundResult[0];
			maxScaleValue = (Double)roundResult[1];
			numberTicks = (Integer)roundResult[2];
			miniticks = (Integer)roundResult[3];
			deltaScaleValue = (maxScaleValue - minScaleValue);
		}
		DecimalFormat df = record.getDecimalFormat();
		 
		// prepare grid vector
		Vector<Integer> horizontalGrid = new Vector<Integer>();
		RecordSet recordSet = record.getParent();
		boolean isBuildGridVector = recordSet.getHorizontalGridType() != RecordSet.HORIZONTAL_GRID_NONE && recordSet.getHorizontalGridRecordName(true).equals(record.getName());
		
		if (record.getNumberScaleTicks() != numberTicks) {
			record.setNumberScaleTicks(numberTicks);
			int cleanwidth = 35; //ticklength + gap + dist;
			if (isPositionLeft) 
				gc.fillRectangle(x0 - cleanwidth, yTop, cleanwidth, height);
			else
				gc.fillRectangle(x0+1, yTop, cleanwidth, height);
		}

		int dist = 10;
		if (!isPositionLeft) {
			ticklength = ticklength * -1; // mirror drawing direction 
			gap = gap * -1;
			dist = dist * -1;
		}
		
		double deltaMainTickValue = deltaScaleValue / numberTicks; //deltaScale / numberTicks;
		log.log(Level.FINE, "minScaleValue = " + minScaleValue + "; maxScaleValue = " + maxScaleValue + ", deltaMainTickValue = " + deltaMainTickValue);
		double deltaMainTickPixel = deltaScaleValue * height / deltaScale / numberTicks; //1.0 * height / numberTicks;
		double deltaPosMini = deltaMainTickPixel / miniticks;

		//draw mini ticks below first main tick
		int yTickPositionMin = new Double(y0 - (Math.abs(minScaleValue - minValue) * height / deltaScale)).intValue(); //new Double(y0 - i * deltaMainTickPixel).intValue();
		for (int j = 1; j < miniticks; j++) {
			int yPosMini = yTickPositionMin + (int)(j * deltaPosMini);
			if (yPosMini >= y0) break;
			log.log(Level.FINEST, "yTickPosition=" + yTickPositionMin + ", xPosMini=" + yPosMini); //$NON-NLS-1$ //$NON-NLS-2$
			gc.drawLine(x0, yPosMini, x0 - ticklength / 2, yPosMini);
		}
		// draw main ticks and mini ticks
		for (int i=0; i <= numberTicks; i++) {
			//draw the main scale, length = 5 and gap to scale = 2
			int yTickPosition = new Double(yTickPositionMin - i * deltaMainTickPixel).intValue();
			gc.drawLine(x0, yTickPosition, x0 - ticklength, yTickPosition);
			if (isBuildGridVector) horizontalGrid.add(yTickPosition);
			//draw the sub scale according number of miniTicks
			for (int j = 1; j < miniticks && i < numberTicks; j++) {
				int yPosMini = yTickPosition - (int)(j * deltaPosMini);
				log.log(Level.FINEST, "yTickPosition=" + yTickPosition + ", xPosMini=" + yPosMini); //$NON-NLS-1$ //$NON-NLS-2$
				gc.drawLine(x0, yPosMini, x0 - ticklength / 2, yPosMini);
			}
			//draw numbers to the scale	
			drawTextCentered(df.format(minScaleValue + i * deltaMainTickValue), x0 - ticklength - gap - dist, yTickPosition, gc, SWT.HORIZONTAL);
		}
		//draw mini ticks above first main tick
		int yTickPositionMax = new Double(yTickPositionMin - numberTicks * deltaMainTickPixel).intValue();
		for (int j = 1; j < miniticks; j++) {
			int yPosMini = yTickPositionMax - (int)(j * deltaPosMini);
			if (yPosMini < yTop-1) break;
			log.log(Level.FINEST, "yTickPosition=" + yTickPositionMax + ", xPosMini=" + yPosMini); //$NON-NLS-1$ //$NON-NLS-2$
			gc.drawLine(x0, yPosMini, x0 - ticklength / 2, yPosMini);
		}
		if (isBuildGridVector) {
			recordSet.setHorizontalGrid(horizontalGrid);
		}
	}

	/**
	 * Draws text horizontal or vertically (rotates plus or minus 90 degrees) centered. 
	 * Uses the current font, color, and background.
	 * @param string the text to draw
	 * @param x the x coordinate of the center of the drawing rectangle
	 * @param y the y coordinate of the center of the drawing rectangle
	 * @param gc the GC on which to draw the text
	 * @param style the style (SWT.UP or SWT.DOWN)
	 */
	public static void drawTextCentered(String string, int x, int y, GC gc, int style) {
		Display display = Display.getCurrent();
		if (display == null) SWT.error(SWT.ERROR_THREAD_INVALID_ACCESS);

		Point pt = gc.textExtent(string); // string dimensions
		Image stringImage = SWTResourceManager.getImage(pt.x, pt.y);
		GC stringGc = SWTResourceManager.getGC(stringImage);
		stringGc.setForeground(gc.getForeground());
		stringGc.setBackground(gc.getBackground());
		stringGc.setFont(gc.getFont());
		stringGc.fillRectangle(0, 0, pt.x, pt.y);
		stringGc.drawText(string, 0, 0);

		boolean isHorizontal = (style & SWT.HORIZONTAL) == SWT.HORIZONTAL;
		if (isHorizontal) { // draw the horizontally image onto the original GC		
			gc.drawImage(stringImage, x - pt.x / 2, y - pt.y / 2);
		}
		else { // draw the image vertically onto the original GC	
			drawVerticalImage(stringImage, x, y - pt.x / 2, gc, style, string+(gc.getBackground().getRGB().toString()));
		}
	}

	/**
	 * Draws text horizontal or vertically (rotates plus or minus 90 degrees) centered. 
	 * Uses the current font, color, and background.
	 * @param string the text to draw
	 * @param x the x left delta coordinate of the drawing rectangle
	 * @param y the y top delta coordinate of the drawing rectangle
	 * @param gc the GC on which to draw the text
	 * @param style the style (SWT.UP or SWT.DOWN)
	 */
	public static void drawText(String string, int x, int y, GC gc, int style) {
		Display display = Display.getCurrent();
		if (display == null) SWT.error(SWT.ERROR_THREAD_INVALID_ACCESS);
	
		Point pt = gc.textExtent(string); // string dimensions
		Image stringImage = SWTResourceManager.getImage(pt.x, pt.y);
		GC stringGc = SWTResourceManager.getGC(stringImage);
		stringGc.setForeground(gc.getForeground());
		stringGc.setBackground(gc.getBackground());
		stringGc.setFont(gc.getFont());
		stringGc.fillRectangle(0, 0, pt.x, pt.y);
		stringGc.drawText(string, 0, 0);

		boolean isHorizontal = (style & SWT.HORIZONTAL) == SWT.HORIZONTAL;
		if (isHorizontal) {	// draw the horizontally image onto the original GC
			gc.drawImage(stringImage, x, y);
		}
		else { // draw the image vertically onto the original GC			
			drawVerticalImage(stringImage, x, y, gc, style, string);
		}
	}

	/**
	 * Draws time line text horizontal [min, hrs], where hrs is bold 
	 * attention: prerequisite is to have gc.setFont called before calling this method !
	 * @param string the text to draw
	 * @param x the x coordinate of the top left corner of the drawing rectangle
	 * @param y the y coordinate of the top left corner of the drawing rectangle
	 * @param gc the GC on which to draw the text
	 * @param style the style (SWT.UP or SWT.DOWN)
	 */
	public static void drawTimeLineText(String string, int x, int y, GC gc, int style) {
		Display display = Display.getCurrent();
		if (display == null) SWT.error(SWT.ERROR_THREAD_INVALID_ACCESS);

		Point pt = gc.textExtent(string); // string dimensions
		Image stringImage = SWTResourceManager.getImage(pt.x, pt.y);
		GC stringGc = SWTResourceManager.getGC(stringImage);
		stringGc.setForeground(gc.getForeground());
		stringGc.setBackground(gc.getBackground());
		stringGc.setFont(gc.getFont());
		stringGc.fillRectangle(0, 0, pt.x, pt.y);
		stringGc.drawText(string, 0, 0);
		
		if (string.contains(", ")) { // string [min, hrs]
			int boldTextOffset = gc.textExtent(string.split(", ")[0]+", ").x;
			stringGc.setFont(SWTResourceManager.getFont(gc, SWT.BOLD));
			stringGc.drawText(string.split(", |]")[1].trim(), boldTextOffset, 0);
			stringGc.setFont(SWTResourceManager.getFont(gc, SWT.NORMAL));
		}

		boolean isHorizontal = (style & SWT.HORIZONTAL) == SWT.HORIZONTAL;
		if (isHorizontal) { // draw the horizontally image onto the original GC
			gc.drawImage(stringImage, x - pt.x / 2, y - pt.y / 2);
		}
		else { // draw the image vertically onto the original GC
			drawVerticalImage(stringImage, x, y - pt.x / 2, gc, style, string);
		}
	}

	/**
	 * Draws an image vertically (rotates plus or minus 90 degrees)
	 * @param image the image to draw
	 * @param x the x coordinate of the top left corner of the drawing rectangle
	 * @param y the y coordinate of the top left corner of the drawing rectangle
	 * @param gc the GC on which to draw the image
	 * @param style the style (SWT.UP or SWT.DOWN)
	 */
	private static void drawVerticalImage(Image image, int x, int y, GC gc, int style, String imgKey) {
		gc.drawImage(SWTResourceManager.getRotatedImage(image, style, imgKey), x, y);
	}
}
