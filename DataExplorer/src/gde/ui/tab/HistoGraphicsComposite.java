/**************************************************************************************
  	This file is part of GNU DataExplorer.

    GNU DataExplorer is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    DataExplorer is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with GNU DataExplorer.  If not, see <http://www.gnu.org/licenses/>.
    
    Copyright (c) 2008,2009,2010,2011,2012,2013,2014,2015,2016 Winfried Bruegmann
    					2016 Thomas Eickert
****************************************************************************************/
package gde.ui.tab;

import gde.GDE;
import gde.data.Channel;
import gde.data.Channels;
import gde.data.HistoRecordSet;
import gde.data.HistoSet;
import gde.data.Record;
import gde.data.RecordSet;
import gde.data.TrailRecord;
import gde.data.TrailRecordSet;
import gde.log.Level;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.ui.SWTResourceManager;
import gde.utils.GraphicsUtils;
import gde.utils.HistoCurveUtils;
import gde.utils.HistoTimeLine;
import gde.utils.StringHelper;
import gde.utils.TimeLine;

import java.util.Date;
import java.util.Properties;
import java.util.Vector;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.GestureEvent;
import org.eclipse.swt.events.GestureListener;
import org.eclipse.swt.events.HelpEvent;
import org.eclipse.swt.events.HelpListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;

/**
 * curve definition table for the histo graphics window.
 * @author Thomas Eickert
 */
public class HistoGraphicsComposite extends GraphicsComposite {
	final static String				$CLASS_NAME				= HistoGraphicsComposite.class.getName();
	final static Logger				log						= Logger.getLogger($CLASS_NAME);

	private final HistoTimeLine	timeLine					= new HistoTimeLine();

	// update graphics only area required
	private HistoRecordSet			oldActiveRecordSet	= null;

	HistoGraphicsComposite(final SashForm useParent) {
		super(useParent, GraphicsWindow.TYPE_HISTO);
	}

	void init() {
		this.setLayout(null);
		this.setDragDetect(false);
		this.setBackground(this.surroundingBackground);

		this.contextMenu.createMenu(this.popupmenu, this.windowType);

		// help lister does not get active on Composite as well as on Canvas
		this.addListener(SWT.Resize, new Listener() {
			@Override
			public void handleEvent(Event evt) {
				if (log.isLoggable(Level.FINER))
					log.log(Level.FINER, "GraphicsComposite.controlResized() = " + evt);
				Rectangle clientRect = HistoGraphicsComposite.this.getClientArea();
				Point size = new Point(clientRect.width, clientRect.height);
				if (log.isLoggable(Level.FINER))
					log.log(Level.FINER, HistoGraphicsComposite.this.oldSize + " - " + size);
				if (!HistoGraphicsComposite.this.oldSize.equals(size)) {
					if (log.isLoggable(Level.FINE))
						log.log(Level.FINE, "size changed, update " + HistoGraphicsComposite.this.oldSize + " - " + size);
					HistoGraphicsComposite.this.oldSize = size;
					setComponentBounds();
					doRedrawGraphics();
				}
			}
		});
		this.addHelpListener(new HelpListener() {
			@Override
			public void helpRequested(HelpEvent evt) {
				if (log.isLoggable(Level.FINER))
					log.log(Level.FINER, "GraphicsComposite.helpRequested " + evt); //$NON-NLS-1$
				switch (HistoGraphicsComposite.this.windowType) {
				default:
				case GraphicsWindow.TYPE_NORMAL:
					HistoGraphicsComposite.this.application.openHelpDialog("", "HelpInfo_4.html"); //$NON-NLS-1$ //$NON-NLS-2$
					break;
				case GraphicsWindow.TYPE_COMPARE:
					HistoGraphicsComposite.this.application.openHelpDialog("", "HelpInfo_91.html"); //$NON-NLS-1$ //$NON-NLS-2$
					break;
				case GraphicsWindow.TYPE_UTIL:
					HistoGraphicsComposite.this.application.openHelpDialog("", "HelpInfo_4.html"); //$NON-NLS-1$ //$NON-NLS-2$
					break;
				}
			}
		});
		{
			this.graphicsHeader = new Text(this, SWT.SINGLE | SWT.CENTER);
			this.graphicsHeader.setFont(SWTResourceManager.getFont(this.application, GDE.WIDGET_FONT_SIZE + 3, SWT.BOLD));
			this.graphicsHeader.setBackground(this.surroundingBackground);
			this.graphicsHeader.setMenu(this.popupmenu);
			this.graphicsHeader.addHelpListener(new HelpListener() {
				@Override
				public void helpRequested(HelpEvent evt) {
					if (log.isLoggable(Level.FINER))
						log.log(Level.FINER, "recordSetHeader.helpRequested " + evt); //$NON-NLS-1$
					HistoGraphicsComposite.this.application.openHelpDialog("", "HelpInfo_4.html"); //$NON-NLS-1$ //$NON-NLS-2$
				}
			});
			this.graphicsHeader.addPaintListener(new PaintListener() {
				@Override
				public void paintControl(PaintEvent evt) {
					if (log.isLoggable(Level.FINER))
						log.log(Level.FINER, "recordSetHeader.paintControl, event=" + evt); //$NON-NLS-1$
					// System.out.println("width = " + GraphicsComposite.this.getSize().x);
					if (HistoGraphicsComposite.this.windowType == GraphicsWindow.TYPE_UTIL) {
						RecordSet utilitySet = HistoGraphicsComposite.this.application.getUtilitySet();
						if (utilitySet != null) {
							String tmpHeader = utilitySet.getRecordSetDescription();
							if (HistoGraphicsComposite.this.graphicsHeaderText == null || !tmpHeader.equals(HistoGraphicsComposite.this.graphicsHeaderText)) {
								HistoGraphicsComposite.this.graphicsHeader.setText(HistoGraphicsComposite.this.graphicsHeaderText = tmpHeader);
							}
						}
					} else {
						Channel activeChannel = HistoGraphicsComposite.this.channels.getActiveChannel();
						if (activeChannel != null) {
							RecordSet recordSet = activeChannel.getActiveRecordSet();
							if (recordSet != null) {
								String tmpDescription = activeChannel.getFileDescription();
								if (tmpDescription.contains(":")) {
									tmpDescription = tmpDescription.substring(0, tmpDescription.indexOf(":"));
								}
								if (tmpDescription.contains(";")) {
									tmpDescription = tmpDescription.substring(0, tmpDescription.indexOf(";"));
								}
								if (tmpDescription.contains("\r")) {
									tmpDescription = tmpDescription.substring(0, tmpDescription.indexOf("\r"));
								}
								if (tmpDescription.contains("\n")) {
									tmpDescription = tmpDescription.substring(0, tmpDescription.indexOf("\n"));
								}
								String tmpHeader = tmpDescription + GDE.STRING_MESSAGE_CONCAT + recordSet.getName();
								if (HistoGraphicsComposite.this.graphicsHeaderText == null || !tmpHeader.equals(HistoGraphicsComposite.this.graphicsHeaderText)) {
									HistoGraphicsComposite.this.graphicsHeader.setText(HistoGraphicsComposite.this.graphicsHeaderText = tmpHeader);
								}
							}
						}
					}
				}
			});
			this.graphicsHeader.addKeyListener(new KeyAdapter() {
				@Override
				public void keyPressed(KeyEvent e) {
					if (log.isLoggable(Level.FINER))
						log.log(Level.FINER, "fileCommentText.keyPressed , event=" + e); //$NON-NLS-1$
					HistoGraphicsComposite.this.isFileCommentChanged = true;
				}
			});
			this.graphicsHeader.addFocusListener(new FocusListener() {
				@Override
				public void focusLost(FocusEvent evt) {
					if (log.isLoggable(Level.FINER))
						log.log(Level.FINER, "fileCommentText.focusLost() , event=" + evt); //$NON-NLS-1$
					HistoGraphicsComposite.this.isFileCommentChanged = false;
					setFileComment();
				}

				@Override
				public void focusGained(FocusEvent evt) {
					if (log.isLoggable(Level.FINER))
						log.log(Level.FINER, "fileCommentText.focusGained() , event=" + evt); //$NON-NLS-1$
				}
			});
		}
		{
			this.graphicCanvas = new Canvas(this, SWT.NONE);
			this.graphicCanvas.setBackground(this.surroundingBackground);
			this.graphicCanvas.setMenu(this.popupmenu);
			this.graphicCanvas.addMouseMoveListener(new MouseMoveListener() {
				@Override
				public void mouseMove(MouseEvent evt) {
					if (log.isLoggable(Level.FINEST))
						log.log(Level.FINEST, "graphicCanvas.mouseMove = " + evt); //$NON-NLS-1$
					mouseMoveAction(evt);
				}
			});
			this.graphicCanvas.addMouseTrackListener(new MouseTrackAdapter() {
				@Override
				public void mouseExit(MouseEvent evt) {
					if (log.isLoggable(Level.FINEST))
						log.log(Level.FINEST, "graphicCanvas.mouseExit, event=" + evt); //$NON-NLS-1$
					HistoGraphicsComposite.this.graphicCanvas.setCursor(HistoGraphicsComposite.this.application.getCursor());
				}
			});
			this.graphicCanvas.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseDown(MouseEvent evt) {
					if (log.isLoggable(Level.FINEST))
						log.log(Level.FINEST, "graphicCanvas.mouseDown, event=" + evt); //$NON-NLS-1$
					if (evt.button == 1) {
						mouseDownAction(evt);
					}
				}

				@Override
				public void mouseUp(MouseEvent evt) {
					if (log.isLoggable(Level.FINEST))
						log.log(Level.FINEST, "graphicCanvas.mouseUp, event=" + evt); //$NON-NLS-1$
					if (evt.button == 1) {
						mouseUpAction(evt);
					}
				}
			});
			this.graphicCanvas.addKeyListener(new KeyAdapter() {
				@Override
				public void keyPressed(KeyEvent e) {
					if (log.isLoggable(Level.FINEST))
						log.log(Level.FINEST, "graphicCanvas.keyPressed() , event=" + e); //$NON-NLS-1$
					if (HistoGraphicsComposite.this.isTransientZoom && !HistoGraphicsComposite.this.isTransientGesture) {
						HistoGraphicsComposite.this.isResetZoomPosition = false;
						Channel activeChannel = Channels.getInstance().getActiveChannel();
						if (activeChannel != null) {
							RecordSet recordSet = (HistoGraphicsComposite.this.windowType == GraphicsWindow.TYPE_NORMAL) ? Channels.getInstance().getActiveChannel().getActiveRecordSet() : HistoGraphicsComposite.this.application.getCompareSet();
							if (HistoGraphicsComposite.this.canvasImage != null && recordSet != null) {

								if (e.keyCode == 'x') {
									// System.out.println("x-direction");
									HistoGraphicsComposite.this.isZoomX = true;
									HistoGraphicsComposite.this.isZoomY = false;
								} else if (e.keyCode == 'y') {
									// System.out.println("y-direction");
									HistoGraphicsComposite.this.isZoomY = true;
									HistoGraphicsComposite.this.isZoomX = false;
								} else if (e.keyCode == '+' || e.keyCode == 0x100002b) {
									// System.out.println("enlarge");

									float boundsRelation = 1.0f * HistoGraphicsComposite.this.curveAreaBounds.width / HistoGraphicsComposite.this.curveAreaBounds.height;
									Point point = new Point(HistoGraphicsComposite.this.canvasBounds.width / 2, HistoGraphicsComposite.this.canvasBounds.height / 2);
									float mouseRelationX = 1.0f * point.x / HistoGraphicsComposite.this.curveAreaBounds.width * 2;
									float mouseRelationY = 1.0f * point.y / HistoGraphicsComposite.this.curveAreaBounds.height * 2;
									// System.out.println(point + " - " + mouseRelationX + " - " + mouseRelationY);

									int xStart, xEnd, yMin, yMax;
									if (HistoGraphicsComposite.this.isZoomX) {
										xStart = (int) (50 * boundsRelation * mouseRelationX);
										xEnd = (int) (HistoGraphicsComposite.this.curveAreaBounds.width - 50 * boundsRelation * (2 - mouseRelationX));
										yMin = 0;
										yMax = HistoGraphicsComposite.this.curveAreaBounds.height - HistoGraphicsComposite.this.curveAreaBounds.y;
									} else if (HistoGraphicsComposite.this.isZoomY) {
										xStart = 0;
										xEnd = HistoGraphicsComposite.this.curveAreaBounds.width;
										yMin = (int) (50 * (2 - mouseRelationY));
										yMax = (int) (HistoGraphicsComposite.this.curveAreaBounds.height - 50 * mouseRelationY);
									} else {
										xStart = (int) (50 * boundsRelation * mouseRelationX);
										xEnd = (int) (HistoGraphicsComposite.this.curveAreaBounds.width - 50 * boundsRelation * (2 - mouseRelationX));
										yMin = (int) (50 * (2 - mouseRelationY));
										yMax = (int) (HistoGraphicsComposite.this.curveAreaBounds.height - 50 * mouseRelationY);
									}

									if (log.isLoggable(Level.FINEST))
										log.log(Level.FINEST, "zoom xStart = " + xStart + " xEnd = " + xEnd + " yMin = " + yMin + " yMax = " + yMax); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
									if (xEnd - xStart > 5 && yMax - yMin > 5) {
										recordSet.setDisplayZoomBounds(new Rectangle(xStart, yMin, xEnd - xStart, yMax - yMin));
										redrawGraphics();
									}
								} else if (e.keyCode == '-' || e.keyCode == 0x100002d) {
									// System.out.println("reduce");
									if (HistoGraphicsComposite.this.isTransientZoom && !HistoGraphicsComposite.this.isTransientGesture) {

										float boundsRelation = 1.0f * HistoGraphicsComposite.this.curveAreaBounds.width / HistoGraphicsComposite.this.curveAreaBounds.height;
										Point point = new Point(HistoGraphicsComposite.this.canvasBounds.width / 2, HistoGraphicsComposite.this.canvasBounds.height / 2);
										float mouseRelationX = 1.0f * point.x / HistoGraphicsComposite.this.curveAreaBounds.width * 2;
										float mouseRelationY = 1.0f * point.y / HistoGraphicsComposite.this.curveAreaBounds.height * 2;
										// System.out.println(point + " - " + mouseRelationX + " - " + mouseRelationY);

										int xStart, xEnd, yMin, yMax;
										if (HistoGraphicsComposite.this.isZoomX) {
											xStart = (int) (-50 * boundsRelation * mouseRelationX);
											xEnd = (int) (HistoGraphicsComposite.this.curveAreaBounds.width + 50 * boundsRelation * (2 - mouseRelationX));
											yMin = 0;
											yMax = HistoGraphicsComposite.this.curveAreaBounds.height - HistoGraphicsComposite.this.curveAreaBounds.y;
										} else if (HistoGraphicsComposite.this.isZoomY) {
											xStart = 0;
											xEnd = HistoGraphicsComposite.this.curveAreaBounds.width;
											yMin = (int) (-50 * (2 - mouseRelationY));
											yMax = (int) (HistoGraphicsComposite.this.curveAreaBounds.height + 50 * mouseRelationY);
										} else {
											xStart = (int) (-50 * boundsRelation * mouseRelationX);
											xEnd = (int) (HistoGraphicsComposite.this.curveAreaBounds.width + 50 * boundsRelation * (2 - mouseRelationX));
											yMin = (int) (-50 * (2 - mouseRelationY));
											yMax = (int) (HistoGraphicsComposite.this.curveAreaBounds.height + 50 * mouseRelationY);
										}

										if (log.isLoggable(Level.FINE))
											log.log(Level.FINE, "zoom xStart = " + xStart + " xEnd = " + xEnd + " yMin = " + yMin + " yMax = " + yMax); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
										if (xEnd - xStart > 5 && yMax - yMin > 5) {
											recordSet.setDisplayZoomBounds(new Rectangle(xStart, yMin, xEnd - xStart, yMax - yMin));
											redrawGraphics();
										}
									}
								} else if (e.keyCode == 0x1000001) {
									// System.out.println("move top direction");
									recordSet.shift(0, -5); // 10% each direction
									redrawGraphics(); // this.graphicCanvas.redraw();?
								} else if (e.keyCode == 0x1000002) {
									// System.out.println("move bottom direction");
									recordSet.shift(0, +5); // 10% each direction
									redrawGraphics(); // this.graphicCanvas.redraw();?
								} else if (e.keyCode == 0x1000003) {
									// System.out.println("move left direction");
									recordSet.shift(+5, 0); // 10% each direction
									redrawGraphics(); // this.graphicCanvas.redraw();?
								} else if (e.keyCode == 0x1000004) {
									// System.out.println("move right direction");
									recordSet.shift(-5, 0); // 10% each direction
									redrawGraphics(); // this.graphicCanvas.redraw();?
								} else {
									// System.out.println("x,y off");
									HistoGraphicsComposite.this.isZoomX = HistoGraphicsComposite.this.isZoomY = false;
								}
							}
						}
					}
				}

				@Override
				public void keyReleased(KeyEvent e) {
					if (log.isLoggable(Level.FINEST))
						log.log(Level.FINEST, "graphicCanvas.keyReleased() , event=" + e); //$NON-NLS-1$
					// System.out.println("x,y off");
					HistoGraphicsComposite.this.isZoomX = HistoGraphicsComposite.this.isZoomY = false;
				}
			});
			this.graphicCanvas.addMouseWheelListener(new MouseWheelListener() {
				@Override
				public void mouseScrolled(MouseEvent evt) {
					if (log.isLoggable(Level.FINEST))
						log.log(Level.FINEST, "graphicCanvas.mouseScrolled, event=" + evt); //$NON-NLS-1$
					if (HistoGraphicsComposite.this.isTransientZoom && !HistoGraphicsComposite.this.isTransientGesture) {
						HistoGraphicsComposite.this.isResetZoomPosition = false;
						Channel activeChannel = Channels.getInstance().getActiveChannel();
						if (activeChannel != null) {
							RecordSet recordSet = (HistoGraphicsComposite.this.windowType == GraphicsWindow.TYPE_NORMAL) ? Channels.getInstance().getActiveChannel().getActiveRecordSet() : HistoGraphicsComposite.this.application.getCompareSet();
							if (HistoGraphicsComposite.this.canvasImage != null && recordSet != null) {

								float boundsRelation = 1.0f * HistoGraphicsComposite.this.curveAreaBounds.width / HistoGraphicsComposite.this.curveAreaBounds.height;
								Point point = checkCurveBounds(evt.x, evt.y);
								float mouseRelationX = 1.0f * point.x / HistoGraphicsComposite.this.curveAreaBounds.width * 2;
								float mouseRelationY = 1.0f * point.y / HistoGraphicsComposite.this.curveAreaBounds.height * 2;
								// System.out.println(point + " - " + mouseRelationX + " - " + mouseRelationY);

								int xStart, xEnd, yMin, yMax;
								if (evt.count < 0) { // reduce
									if (HistoGraphicsComposite.this.isZoomX) {
										xStart = (int) (-50 * boundsRelation * mouseRelationX);
										xEnd = (int) (HistoGraphicsComposite.this.curveAreaBounds.width + 50 * boundsRelation * (2 - mouseRelationX));
										yMin = 0;
										yMax = HistoGraphicsComposite.this.curveAreaBounds.height - HistoGraphicsComposite.this.curveAreaBounds.y;
									} else if (HistoGraphicsComposite.this.isZoomY) {
										xStart = 0;
										xEnd = HistoGraphicsComposite.this.curveAreaBounds.width;
										yMin = (int) (-50 * (2 - mouseRelationY));
										yMax = (int) (HistoGraphicsComposite.this.curveAreaBounds.height + 50 * mouseRelationY);
									} else {
										xStart = (int) (-50 * boundsRelation * mouseRelationX);
										xEnd = (int) (HistoGraphicsComposite.this.curveAreaBounds.width + 50 * boundsRelation * (2 - mouseRelationX));
										yMin = (int) (-50 * (2 - mouseRelationY));
										yMax = (int) (HistoGraphicsComposite.this.curveAreaBounds.height + 50 * mouseRelationY);
									}
								} else { // enlarge
									if (HistoGraphicsComposite.this.isZoomX) {
										xStart = (int) (50 * boundsRelation * mouseRelationX);
										xEnd = (int) (HistoGraphicsComposite.this.curveAreaBounds.width - 50 * boundsRelation * (2 - mouseRelationX));
										yMin = 0;
										yMax = HistoGraphicsComposite.this.curveAreaBounds.height - HistoGraphicsComposite.this.curveAreaBounds.y;
									} else if (HistoGraphicsComposite.this.isZoomY) {
										xStart = 0;
										xEnd = HistoGraphicsComposite.this.curveAreaBounds.width;
										yMin = (int) (50 * (2 - mouseRelationY));
										yMax = (int) (HistoGraphicsComposite.this.curveAreaBounds.height - 50 * mouseRelationY);
									} else {
										xStart = (int) (50 * boundsRelation * mouseRelationX);
										xEnd = (int) (HistoGraphicsComposite.this.curveAreaBounds.width - 50 * boundsRelation * (2 - mouseRelationX));
										yMin = (int) (50 * (2 - mouseRelationY));
										yMax = (int) (HistoGraphicsComposite.this.curveAreaBounds.height - 50 * mouseRelationY);
									}
								}
								if (log.isLoggable(Level.FINER))
									log.log(Level.FINER, "zoom xStart = " + xStart + " xEnd = " + xEnd + " yMin = " + yMin + " yMax = " + yMax); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
								if (xEnd - xStart > 5 && yMax - yMin > 5) {
									recordSet.setDisplayZoomBounds(new Rectangle(xStart, yMin, xEnd - xStart, yMax - yMin));
									redrawGraphics();
								}
							}
						}
					}
				}
			});
			this.graphicCanvas.addGestureListener(new GestureListener() {
				@Override
				public void gesture(GestureEvent evt) {
					if (evt.detail == SWT.GESTURE_BEGIN) {
						if (log.isLoggable(Level.FINEST))
							log.log(Level.FINEST, "BEGIN = " + evt); //$NON-NLS-1$
						HistoGraphicsComposite.this.isTransientGesture = true;
					} else if (evt.detail == SWT.GESTURE_MAGNIFY) {
						if (log.isLoggable(Level.FINEST))
							log.log(Level.FINEST, "MAGIFY = " + evt); //$NON-NLS-1$
						if (HistoGraphicsComposite.this.isTransientGesture) {
							HistoGraphicsComposite.this.isResetZoomPosition = false;
							Channel activeChannel = Channels.getInstance().getActiveChannel();
							if (activeChannel != null) {
								RecordSet recordSet = (HistoGraphicsComposite.this.windowType == GraphicsWindow.TYPE_NORMAL) ? Channels.getInstance().getActiveChannel().getActiveRecordSet() : HistoGraphicsComposite.this.application.getCompareSet();
								if (HistoGraphicsComposite.this.canvasImage != null && recordSet != null) {

									float boundsRelation = 1.0f * HistoGraphicsComposite.this.curveAreaBounds.width / HistoGraphicsComposite.this.curveAreaBounds.height;
									Point point = checkCurveBounds(evt.x, evt.y);
									float mouseRelationX = 1.0f * point.x / HistoGraphicsComposite.this.curveAreaBounds.width * 2;
									float mouseRelationY = 1.0f * point.y / HistoGraphicsComposite.this.curveAreaBounds.height * 2;
									// System.out.println(point + " - " + mouseRelationX + " - " + mouseRelationY);

									int xStart, xEnd, yMin, yMax;
									if (evt.magnification < 1) { // reduce
										if (HistoGraphicsComposite.this.isZoomX) {
											xStart = (int) (-25 * boundsRelation * mouseRelationX);
											xEnd = (int) (HistoGraphicsComposite.this.curveAreaBounds.width + 25 * boundsRelation * (2 - mouseRelationX));
											yMin = 0;
											yMax = HistoGraphicsComposite.this.curveAreaBounds.height - HistoGraphicsComposite.this.curveAreaBounds.y;
										} else if (HistoGraphicsComposite.this.isZoomY) {
											xStart = 0;
											xEnd = HistoGraphicsComposite.this.curveAreaBounds.width;
											yMin = (int) (-25 * (2 - mouseRelationY));
											yMax = (int) (HistoGraphicsComposite.this.curveAreaBounds.height + 25 * mouseRelationY);
										} else {
											xStart = (int) (-25 * boundsRelation * mouseRelationX);
											xEnd = (int) (HistoGraphicsComposite.this.curveAreaBounds.width + 25 * boundsRelation * (2 - mouseRelationX));
											yMin = (int) (-25 * (2 - mouseRelationY));
											yMax = (int) (HistoGraphicsComposite.this.curveAreaBounds.height + 25 * mouseRelationY);
										}
									} else { // enlarge
										if (HistoGraphicsComposite.this.isZoomX) {
											xStart = (int) (25 * boundsRelation * mouseRelationX);
											xEnd = (int) (HistoGraphicsComposite.this.curveAreaBounds.width - 25 * boundsRelation * (2 - mouseRelationX));
											yMin = 0;
											yMax = HistoGraphicsComposite.this.curveAreaBounds.height - HistoGraphicsComposite.this.curveAreaBounds.y;
										} else if (HistoGraphicsComposite.this.isZoomY) {
											xStart = 0;
											xEnd = HistoGraphicsComposite.this.curveAreaBounds.width;
											yMin = (int) (25 * (2 - mouseRelationY));
											yMax = (int) (HistoGraphicsComposite.this.curveAreaBounds.height - 25 * mouseRelationY);
										} else {
											xStart = (int) (25 * boundsRelation * mouseRelationX);
											xEnd = (int) (HistoGraphicsComposite.this.curveAreaBounds.width - 25 * boundsRelation * (2 - mouseRelationX));
											yMin = (int) (25 * (2 - mouseRelationY));
											yMax = (int) (HistoGraphicsComposite.this.curveAreaBounds.height - 25 * mouseRelationY);
										}
									}
									if (log.isLoggable(Level.FINER))
										log.log(Level.FINER, "zoom xStart = " + xStart + " xEnd = " + xEnd + " yMin = " + yMin + " yMax = " + yMax); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
									if (xEnd - xStart > 5 && yMax - yMin > 5) {
										recordSet.setDisplayZoomBounds(new Rectangle(xStart, yMin, xEnd - xStart, yMax - yMin));
										redrawGraphics();
									}
								}
							}
						}
					} else if (evt.detail == SWT.GESTURE_PAN) {
						if (log.isLoggable(Level.FINEST))
							log.log(Level.FINEST, "PAN = " + evt); //$NON-NLS-1$
						Channel activeChannel = Channels.getInstance().getActiveChannel();
						if (activeChannel != null && HistoGraphicsComposite.this.isTransientGesture) {
							RecordSet recordSet = (HistoGraphicsComposite.this.windowType == GraphicsWindow.TYPE_NORMAL) ? activeChannel.getActiveRecordSet() : HistoGraphicsComposite.this.application.getCompareSet();
							if (recordSet != null && HistoGraphicsComposite.this.canvasImage != null) {
								recordSet.shift(evt.xDirection, -1 * evt.yDirection); // 10% each direction
								redrawGraphics(); // this.graphicCanvas.redraw();?
							}
						}
					} else if (evt.detail == SWT.GESTURE_END) {
						if (log.isLoggable(Level.FINEST))
							log.log(Level.FINEST, "END = " + evt); //$NON-NLS-1$
						HistoGraphicsComposite.this.isTransientGesture = false;
					}
				}
			});
			this.graphicCanvas.addPaintListener(new PaintListener() {
				@Override
				public void paintControl(PaintEvent evt) {
					if (log.isLoggable(Level.FINER))
						log.log(Level.FINER, "graphicCanvas.paintControl, event=" + evt); //$NON-NLS-1$
					// System.out.println("width = " + GraphicsComposite.this.getSize().x);
					try {
						drawAreaPaintControl(evt);
					} catch (Exception e) {
						log.log(Level.SEVERE, e.getMessage(), e);
					}
				}
			});
		}
		{
			this.recordSetComment = new Text(this, SWT.MULTI | SWT.LEFT);
			this.recordSetComment.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE + 1, SWT.NORMAL));
			this.recordSetComment.setBackground(this.surroundingBackground);
			this.recordSetComment.setMenu(this.popupmenu);
			this.recordSetComment.addPaintListener(new PaintListener() {
				@Override
				public void paintControl(PaintEvent evt) {
					if (log.isLoggable(Level.FINER))
						log.log(Level.FINER, "recordSetComment.paintControl, event=" + evt); //$NON-NLS-1$
					if (HistoGraphicsComposite.this.channels.getActiveChannel() != null) {
						RecordSet recordSet = HistoGraphicsComposite.this.channels.getActiveChannel().getActiveRecordSet();
						if (recordSet != null && (HistoGraphicsComposite.this.recordSetCommentText == null
								|| !recordSet.getRecordSetDescription().equals(HistoGraphicsComposite.this.recordSetCommentText))) {
							HistoGraphicsComposite.this.recordSetComment.setText(HistoGraphicsComposite.this.recordSetCommentText = recordSet.getRecordSetDescription());
						}
					}
				}
			});

			this.recordSetComment.addHelpListener(new HelpListener() {
				@Override
				public void helpRequested(HelpEvent evt) {
					if (log.isLoggable(Level.FINER))
						log.log(Level.FINER, "recordSetCommentText.helpRequested " + evt); //$NON-NLS-1$
					DataExplorer.getInstance().openHelpDialog("", "HelpInfo_11.html"); //$NON-NLS-1$ //$NON-NLS-2$
				}
			});
			this.recordSetComment.addKeyListener(new KeyAdapter() {
				@Override
				public void keyPressed(KeyEvent e) {
					if (log.isLoggable(Level.FINEST))
						log.log(Level.FINEST, "recordSetComment.keyPressed() , event=" + e); //$NON-NLS-1$
					HistoGraphicsComposite.this.isRecordCommentChanged = true;
				}
			});
			this.recordSetComment.addFocusListener(new FocusListener() {
				@Override
				public void focusLost(FocusEvent evt) {
					if (log.isLoggable(Level.FINEST))
						log.log(Level.FINEST, "recordSetComment.focusLost() , event=" + evt); //$NON-NLS-1$
					updateRecordSetComment();
				}

				@Override
				public void focusGained(FocusEvent evt) {
					if (log.isLoggable(Level.FINEST))
						log.log(Level.FINEST, "recordSetComment.focusGained() , event=" + evt); //$NON-NLS-1$
				}
			});
		}
	}

	/**
	 * this method is called in case of an paint event (redraw) and draw the containing records 
	 * @param evt
	 */
	void drawAreaPaintControl(PaintEvent evt) {
		if (log.isLoggable(Level.FINEST))
			log.log(Level.FINEST, "drawAreaPaintControl.paintControl, event=" + evt); //$NON-NLS-1$
		// Get the canvas and its dimensions
		this.canvasBounds = this.graphicCanvas.getClientArea();
		if (log.isLoggable(Level.FINER))
			log.log(Level.FINER, "canvas size = " + this.canvasBounds); //$NON-NLS-1$

		if (this.canvasImage != null)
			this.canvasImage.dispose();
		this.canvasImage = new Image(GDE.display, this.canvasBounds);
		this.canvasImageGC = new GC(this.canvasImage); // SWTResourceManager.getGC(this.canvasImage);
		this.canvasImageGC.setBackground(this.surroundingBackground);
		this.canvasImageGC.fillRectangle(this.canvasBounds);
		this.canvasImageGC.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
		// get gc for other drawing operations
		this.canvasGC = new GC(this.graphicCanvas); // SWTResourceManager.getGC(this.graphicCanvas, "curveArea_" + this.windowType);

		RecordSet recordSet = null;
		switch (this.windowType) {
		case GraphicsWindow.TYPE_COMPARE:
			if (this.application.getCompareSet() != null && this.application.getCompareSet().size() > 0) {
				recordSet = this.application.getCompareSet();
			}
			break;

		case GraphicsWindow.TYPE_UTIL:
			if (this.application.getUtilitySet() != null && this.application.getUtilitySet().size() > 0) {
				recordSet = this.application.getUtilitySet();
			}
			break;

		default: // TYPE_NORMAL
			if (this.channels.getActiveChannel() != null && this.channels.getActiveChannel().getActiveRecordSet() != null) {
				recordSet = this.channels.getActiveChannel().getActiveRecordSet();
			}
			break;

		case GraphicsWindow.TYPE_HISTO:
			if (this.channels.getActiveChannel() != null) {
				// TODO raus recordSet = HistoSet.me.firstEntry().getValue().get(0);
				recordSet = HistoSet.getInstance().getTrailRecordSet();
			}
			break;
		}
		if (recordSet != null && recordSet.realSize() > 0) {
			drawCurves(recordSet, this.canvasBounds, this.canvasImageGC);
			this.canvasGC.drawImage(this.canvasImage, 0, 0);
			// changed curve selection may change the scale end values
			recordSet.syncScaleOfSyncableRecords();

			if (recordSet.isMeasurementMode(recordSet.getRecordKeyMeasurement()) || recordSet.isDeltaMeasurementMode(recordSet.getRecordKeyMeasurement())) {
				drawMeasurePointer(recordSet, HistoGraphicsComposite.MODE_MEASURE, true);
			} else if (this.isLeftCutMode) {
				drawCutPointer(HistoGraphicsComposite.MODE_CUT_LEFT, true, false);
			} else if (this.isRightCutMode) {
				drawCutPointer(HistoGraphicsComposite.MODE_CUT_RIGHT, false, true);
			}
		} else
			this.canvasGC.drawImage(this.canvasImage, 0, 0);

		this.canvasGC.dispose();
		this.canvasImageGC.dispose();
		// this.canvasImage.dispose(); //zooming, marking, ... needs a reference to canvasImage
	}

	/**
	 * method to draw the curves with it scales and defines the curve area
	 * @param recordSet the record set to be drawn
	 * @param bounds the bounds where the curves and scales are drawn
	 * @param gc the graphics context to be used for the graphics operations
	 */
	private void drawCurves(final RecordSet recordSet, final Rectangle bounds, final GC gc) {
		final HistoSet histoSet = HistoSet.getInstance();
		long startInitTime = new Date().getTime();
		// prime the record set regarding scope mode and/or zoom mode
		if (this.isScopeMode) {
			int offset = recordSet.get(0).realSize() - recordSet.getScopeModeSize();
			if (offset < 1) {
				recordSet.setScopeModeOffset(0);
				recordSet.setScopeMode(false);
			} else {
				recordSet.setScopeModeOffset(offset);
				recordSet.setScopeMode(true);
			}
		}

		// calculate number of curve scales, left and right side
		int numberCurvesRight = 0;
		int numberCurvesLeft = 0;
		for (Record tmpRecord : recordSet.getRecordsSortedForDisplay()) {
			if (tmpRecord != null && tmpRecord.isScaleVisible()) {
				// if (log.isLoggable(Level.FINER))
				log.log(Level.FINE, "==>> " + tmpRecord.getName() + " isScaleVisible = " + tmpRecord.isScaleVisible()); //$NON-NLS-1$ //$NON-NLS-2$
				if (tmpRecord.isPositionLeft())
					numberCurvesLeft++;
				else
					numberCurvesRight++;
			}
		}
		// correct scales and scale position according compare set requirements
		if (recordSet.isCompareSet()) {
			numberCurvesLeft = 1; // numberCurvesLeft > 0 ? 1 : 0;
			numberCurvesRight = 0; // numberCurvesRight > 0 && numberCurvesLeft == 0 ? 1 : 0;
		}
		if (log.isLoggable(Level.FINE))
			log.log(Level.FINE, "nCurveLeft=" + numberCurvesLeft + ", nCurveRight=" + numberCurvesRight); //$NON-NLS-1$ //$NON-NLS-2$

		// calculate the bounds left for the curves
		int dataScaleWidth; // horizontal space used for text and scales, numbers and caption
		int x0, y0; // the lower left corner of the curve area
		int xMax, yMax; // the upper right corner of the curve area
		int width; // x coordinate width - time scale
		int height; // y coordinate - make modulo 10 ??

		// calculate the horizontal space width to be used for the scales
		Point pt = gc.textExtent("-000,00"); //$NON-NLS-1$
		int horizontalGap = pt.x / 5;
		int horizontalNumberExtend = pt.x;
		int horizontalCaptionExtend = pt.y;
		dataScaleWidth = recordSet.isCompareSet() ? horizontalNumberExtend + horizontalGap : horizontalNumberExtend + horizontalCaptionExtend + horizontalGap;
		int spaceLeft = numberCurvesLeft * dataScaleWidth;
		int spaceRight = numberCurvesRight * dataScaleWidth;

		// calculate the horizontal area available for plotting graphs
		int gapSide = 10; // free gap left or right side of the curves
		x0 = spaceLeft + (numberCurvesLeft > 0 ? gapSide / 2 : gapSide);// enable a small gap if no axis is shown
		xMax = bounds.width - spaceRight - (numberCurvesRight > 0 ? gapSide / 2 : gapSide);
		width = ((xMax - x0) <= 0) ? 1 : (xMax - x0);

		// calculate the vertical area available for plotting graphs
		yMax = 10; // free gap on top of the curves
		int gapBot = 3 * pt.y + 4; // space used for time scale text and scales with description or legend;
		y0 = bounds.height - yMax - gapBot;
		height = y0 - yMax; // recalculate due to modulo 10 ??
		if (log.isLoggable(Level.FINER))
			log.log(Level.FINER, "draw area x0=" + x0 + ", y0=" + y0 + ", xMax=" + xMax + ", yMax=" + yMax + ", width=" + width + ", height=" + height); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
		// set offset values used for mouse measurement pointers
		this.offSetX = x0;
		this.offSetY = y0 - height;

		// draw curves for each active record
		this.curveAreaBounds = new Rectangle(x0, y0 - height, width, height);
		recordSet.setDrawAreaBounds(this.curveAreaBounds);
		if (log.isLoggable(Level.FINER))
			log.log(Level.FINER, "curve bounds = " + this.curveAreaBounds); //$NON-NLS-1$

		gc.setBackground(this.curveAreaBackground);
		gc.fillRectangle(this.curveAreaBounds);
		gc.setBackground(this.surroundingBackground);

		long minimumTimeStamp = histoSet.lastKey(); // TODO WRONG : for zoom must be leftPixelPosition etc
		long maximumTimeStamp = histoSet.firstKey();
		this.timeLine.initialize(recordSet, width, minimumTimeStamp, maximumTimeStamp);

		// TODO raus int startTimeFormated = drawTimeLineOBS(recordSet, gc, x0, y0, width);
		this.timeLine.drawTimeLine(gc, x0, y0);

		// draw draw area bounding
		gc.setForeground(this.curveAreaBorderColor);

		gc.drawLine(x0 - 1, yMax - 1, xMax + 1, yMax - 1);
		gc.drawLine(x0 - 1, yMax - 1, x0 - 1, y0);
		gc.drawLine(xMax + 1, yMax - 1, xMax + 1, y0);

		if (log.isLoggable(Level.TIME))
			log.log(Level.TIME, "draw init time   =  " + StringHelper.getFormatedTime("ss:SSS", (new Date().getTime() - startInitTime)));

		long startTime = new Date().getTime();
		// TODO raus drawRecordData(recordSet, gc, dataScaleWidth, x0, y0, width, height);
		drawTrailRecordSet((TrailRecordSet) recordSet, gc, dataScaleWidth, x0, y0, width, height);
		if (log.isLoggable(Level.TIME))
			log.log(Level.TIME, "draw records time = " + StringHelper.getFormatedTime("ss:SSS", (new Date().getTime() - startTime)));
	}

	/**
	 * draws the visible curves for all measurements.
	 * supports multiple curves for one single measurement.
	 * was drawHistoRecordData
	 */
	private void drawTrailRecordSet(TrailRecordSet trailRecordSet, GC gc, int dataScaleWidth, int x0, int y0, int width, int height) {
		// check for activated horizontal grid
		boolean isCurveGridEnabled = trailRecordSet.getHorizontalGridType() > 0;

		// draw each record using sorted record set names
		boolean isDrawScaleInRecordColor = this.settings.isDrawScaleInRecordColor();
		boolean isDrawNameInRecordColor = this.settings.isDrawNameInRecordColor();
		boolean isDrawNumbersInRecordColor = this.settings.isDrawNumbersInRecordColor();
		trailRecordSet.updateSyncRecordScale();
		for (Record actualRecord : trailRecordSet.getRecordsSortedForDisplay()) {
			boolean isActualRecordEnabled = actualRecord.isVisible() && actualRecord.isDisplayable();
			// if (log.isLoggable(Level.FINE) && isActualRecordEnabled)
			log.log(Level.FINE, "record=" + actualRecord.getName() + "  isVisibel=" + actualRecord.isVisible() + " isDisplayable=" + actualRecord.isDisplayable() //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					+ " isScaleSynced=" + actualRecord.isScaleSynced());
			if (actualRecord.isScaleVisible())
				HistoCurveUtils.drawScale(actualRecord, gc, x0, y0, width, height, dataScaleWidth, isDrawScaleInRecordColor, isDrawNameInRecordColor, isDrawNumbersInRecordColor);

			if (isCurveGridEnabled && actualRecord.getOrdinal() == trailRecordSet.getHorizontalGridRecordOrdinal()) // check for activated horizontal grid
				drawCurveGrid(trailRecordSet, gc, this.curveAreaBounds, this.settings.getGridDashStyle());

			if (isActualRecordEnabled) {
				// gc.setForeground(SWTResourceManager.getColor(SWT.COLOR_RED));
				// gc.drawRectangle(x0, y0-height, width, height);
				gc.setClipping(x0 - 1, y0 - height - 1, width + 2, height + 2);
				try {
					if (((TrailRecord) actualRecord).getTrailType().isSuite()) {
						HistoCurveUtils.drawHistoSuite((TrailRecord) actualRecord, gc, x0, y0, width, height, this.timeLine);
					} else {
						// CurveUtils.drawCurve(actualRecord, gc, x0, y0, width, height, recordSet.isCompareSet());
						HistoCurveUtils.drawHistoCurve((TrailRecord) actualRecord, gc, x0, y0, width, height, this.timeLine);
					}
				}
				catch (Exception e) {
					log.log(Level.SEVERE, actualRecord.getName() + " does not have usaable data");
					e.printStackTrace();
				}
				gc.setClipping(this.canvasBounds);
			}
		}
	}

	/**
	 * draw horizontal (curve) grid lines according the vector prepared during daring specified curve scale 
	 * @param recordSet
	 * @param gc the graphics context to be used
	 * @param bounds
	 * @param dashLineStyle to be used for the custom line style
	 */
	private void drawCurveGrid(RecordSet recordSet, GC gc, Rectangle bounds, int[] dashLineStyle) {
		gc.setLineWidth(1);
		gc.setLineDash(dashLineStyle);
		gc.setLineStyle(SWT.LINE_CUSTOM);
		gc.setForeground(recordSet.getHorizontalGridColor());

		Vector<Integer> horizontalGridVector = recordSet.getHorizontalGrid();
		for (int i = 0; i < horizontalGridVector.size(); i += recordSet.getHorizontalGridType()) {
			int y = horizontalGridVector.get(i);
			if (y > bounds.y && y < (bounds.y + bounds.height))
				gc.drawLine(bounds.x, y, bounds.x + bounds.width, y);
		}
	}

	/**
	 * draw vertical (time) grid lines according the vector defined during drawing of time scale
	 * @param recordSet
	 * @param gc the graphics context to be used
	 * @param bounds
	 * @param dashLineStyle to be used for the custom line style
	 */
	public void drawTimeGrid(RecordSet recordSet, GC gc, Rectangle bounds, int[] dashLineStyle) {
		gc.setLineWidth(1);
		gc.setLineDash(dashLineStyle);
		gc.setLineStyle(SWT.LINE_CUSTOM);
		gc.setForeground(recordSet.getColorTimeGrid());
		for (Integer x : recordSet.getTimeGrid()) {
			gc.drawLine(x, bounds.y, x, bounds.y + bounds.height);
		}
	}

	/**
	 * redraws the graphics canvas as well as the curve selector table
	 */
	public void redrawGraphics() {
		if (Thread.currentThread().getId() == this.application.getThreadId()) {
			doRedrawGraphics();
		} else {
			GDE.display.asyncExec(new Runnable() {
				@Override
				public void run() {
					doRedrawGraphics();
				}
			});
		}
	}

	/**
	 * updates the graphics canvas, while repeatable redraw calls it optimized to the required area
	 */
	synchronized void doRedrawGraphics() {
		this.graphicsHeader.redraw();

		if (!GDE.IS_LINUX) { // old code changed due to Mountain Lion refresh problems
			if (log.isLoggable(Level.FINER))
				log.log(Level.FINER, "this.graphicCanvas.redraw(5,5,5,5,true); // image based - let OS handle the update");
			Point size = this.graphicCanvas.getSize();
			this.graphicCanvas.redraw(5, 5, 5, 5, true); // image based - let OS handle the update
			this.graphicCanvas.redraw(size.x - 5, 5, 5, 5, true);
			this.graphicCanvas.redraw(5, size.y - 5, 5, 5, true);
			this.graphicCanvas.redraw(size.x - 5, size.y - 5, 5, 5, true);
		} else {
			if (log.isLoggable(Level.FINER))
				log.log(Level.FINER, "this.graphicCanvas.redraw(); // do full update where required");
			this.graphicCanvas.redraw(); // do full update where required
		}
		this.recordSetComment.redraw();
	}

	public void notifySelected() {
		this.recordSetComment.notifyListeners(SWT.FocusOut, new Event());
	}

	/**
	 * draw the start pointer for measurement modes
	 * @param recordSet
	 * @param mode
	 * @param isRefresh
	 */
	public void drawMeasurePointer(RecordSet recordSet, int mode, boolean isRefresh) {
		this.setModeState(mode); // cleans old pointer if required

		String measureRecordKey = recordSet.getRecordKeyMeasurement();
		Record record = recordSet.get(measureRecordKey);

		// set the gc properties
		this.canvasGC = new GC(this.graphicCanvas);
		this.canvasGC.setLineWidth(1);
		this.canvasGC.setLineStyle(SWT.LINE_DASH);
		this.canvasGC.setForeground(SWTResourceManager.getColor(SWT.COLOR_BLACK));

		if (recordSet.isMeasurementMode(measureRecordKey)) {
			// initial measure position
			this.xPosMeasure = isRefresh ? this.xPosMeasure : this.curveAreaBounds.width / 4;
			this.yPosMeasure = record.getVerticalDisplayPointValue(this.xPosMeasure);
			if (log.isLoggable(Level.FINE))
				log.log(Level.FINE, "initial xPosMeasure = " + this.xPosMeasure + " yPosMeasure = " + this.yPosMeasure); //$NON-NLS-1$ //$NON-NLS-2$

			drawVerticalLine(this.xPosMeasure, 0, this.curveAreaBounds.height);
			drawHorizontalLine(this.yPosMeasure, 0, this.curveAreaBounds.width);

			this.recordSetComment.setText(this.getSelectedMeasurementsAsTable());

			this.application.setStatusMessage(Messages.getString(MessageIds.GDE_MSGT0256, new Object[] { record.getName(),
					record.getVerticalDisplayPointAsFormattedScaleValue(this.yPosMeasure, this.curveAreaBounds), record.getUnit(),
					record.getHorizontalDisplayPointAsFormattedTimeWithUnit(this.xPosMeasure) }));
		} else if (recordSet.isDeltaMeasurementMode(measureRecordKey)) {
			this.xPosMeasure = isRefresh ? this.xPosMeasure : this.curveAreaBounds.width / 4;
			this.yPosMeasure = record.getVerticalDisplayPointValue(this.xPosMeasure);

			// measure position
			drawVerticalLine(this.xPosMeasure, 0, this.curveAreaBounds.height);
			drawHorizontalLine(this.yPosMeasure, 0, this.curveAreaBounds.width);

			// delta position
			this.xPosDelta = isRefresh ? this.xPosDelta : this.curveAreaBounds.width / 3 * 2;
			this.yPosDelta = record.getVerticalDisplayPointValue(this.xPosDelta);

			this.canvasGC.setForeground(SWTResourceManager.getColor(SWT.COLOR_BLUE));
			drawVerticalLine(this.xPosDelta, 0, this.curveAreaBounds.height);
			drawHorizontalLine(this.yPosDelta, 0, this.curveAreaBounds.width);

			drawConnectingLine(this.xPosMeasure, this.yPosMeasure, this.xPosDelta, this.yPosDelta, SWT.COLOR_BLACK);

			this.canvasGC.setForeground(SWTResourceManager.getColor(SWT.COLOR_BLACK));

			this.application.setStatusMessage(Messages.getString(MessageIds.GDE_MSGT0257, new Object[] { record.getName(), Messages.getString(MessageIds.GDE_MSGT0212),
					record.getVerticalDisplayDeltaAsFormattedValue(this.yPosMeasure - this.yPosDelta, this.curveAreaBounds),
					record.getUnit(),
					TimeLine.getFomatedTimeWithUnit(record.getHorizontalDisplayPointTime_ms(this.xPosDelta) - record.getHorizontalDisplayPointTime_ms(this.xPosMeasure)),
					record.getSlopeValue(new Point(this.xPosDelta - this.xPosMeasure, this.yPosMeasure - this.yPosDelta)), record.getUnit() }));
		}
		this.canvasGC.dispose();
	}

	/**
	 * draws horizontal line as defined relative to curve draw area, where there is an offset from left and an offset from top  
	 * for performance reason specify line width, line style and line color outside 
	 * @param posFromLeft
	 * @param posFromTop
	 * @param length
	 */
	private void drawVerticalLine(int posFromLeft, int posFromTop, int length) {
		this.canvasGC.drawLine(posFromLeft + this.offSetX, posFromTop + this.offSetY, posFromLeft + this.offSetX, posFromTop + this.offSetY + length - 1);
	}

	/**
	 * draws vertical line as defined relative to curve draw area, where there is an offset from left and an offset from top 
	 * for performance reason specify line width, line style and line color outside 
	 * @param posFromTop
	 * @param posFromLeft
	 * @param length
	 */
	private void drawHorizontalLine(int posFromTop, int posFromLeft, int length) {
		this.canvasGC.drawLine(posFromLeft + this.offSetX, posFromTop + this.offSetY, posFromLeft + this.offSetX + length - 1, posFromTop + this.offSetY);
	}

	/**
	 * draws line as defined relative to curve draw area, where there is an offset from left and an offset from top 
	 * for performance reason specify line width, line style and line color outside 
	 * @param posFromTop1
	 * @param posFromLeft1
	 * @param posFromTop2
	 * @param posFromLeft2
	 */
	private void drawConnectingLine(int posFromLeft1, int posFromTop1, int posFromLeft2, int posFromTop2, int swtColor) {
		this.canvasGC.setForeground(SWTResourceManager.getColor(swtColor));
		this.canvasGC.setLineStyle(SWT.LINE_SOLID);
		this.canvasGC.drawLine(posFromLeft1 + this.offSetX, posFromTop1 + this.offSetY, posFromLeft2 + this.offSetX, posFromTop2 + this.offSetY);
	}

	/**
	 * erase a vertical line by re-drawing the curve area image 
	 * @param posFromLeft
	 * @param posFromTop
	 * @param length
	 * @param lineWidth
	 */
	void eraseVerticalLine(int posFromLeft, int posFromTop, int length, int lineWidth) {
		this.canvasGC.drawImage(this.canvasImage, posFromLeft + this.offSetX, posFromTop + this.offSetY, lineWidth, length, posFromLeft + this.offSetX, posFromTop
				+ this.offSetY, lineWidth, length);
	}

	/**
	 * erase a horizontal line by re-drawing the curve area image 
	 * @param posFromTop
	 * @param posFromLeft
	 * @param length
	 * @param lineWidth
	 */
	void eraseHorizontalLine(int posFromTop, int posFromLeft, int length, int lineWidth) {
		this.canvasGC.drawImage(this.canvasImage, posFromLeft + this.offSetX, posFromTop + this.offSetY, length, lineWidth, posFromLeft + this.offSetX, posFromTop
				+ this.offSetY, length, lineWidth);
	}

	/**
	 * clean connecting line by re-drawing the untouched curve area image of this area
	 */
	void cleanConnectingLineObsoleteRectangle() {
		this.leftLast = this.leftLast == 0 ? this.xPosMeasure : this.leftLast;
		int left = this.xPosMeasure <= this.xPosDelta ? this.leftLast < this.xPosMeasure ? this.leftLast : this.xPosMeasure : this.leftLast < this.xPosDelta ? this.leftLast : this.xPosDelta;

		this.topLast = this.topLast == 0 ? this.yPosDelta : this.topLast;
		int top = this.yPosDelta <= this.yPosMeasure ? this.topLast < this.yPosDelta ? this.topLast : this.yPosDelta : this.topLast < this.yPosMeasure ? this.topLast : this.yPosMeasure;

		this.rightLast = this.rightLast == 0 ? this.xPosDelta - left : this.rightLast;
		int width = this.xPosDelta >= this.xPosMeasure ? this.rightLast > this.xPosDelta ? this.rightLast - left : this.xPosDelta
				- left : this.rightLast > this.xPosMeasure ? this.rightLast - left : this.xPosMeasure - left;

		this.bottomLast = this.bottomLast == 0 ? this.yPosMeasure - top : this.bottomLast;
		int height = this.yPosMeasure >= this.yPosDelta ? this.bottomLast > this.yPosMeasure ? this.bottomLast - top : this.yPosMeasure
				- top : this.bottomLast > this.yPosDelta ? this.bottomLast - top : this.yPosDelta - top;

		if (log.isLoggable(Level.FINER))
			log.log(Level.FINER, "leftLast = " + this.leftLast + " topLast = " + this.topLast + " rightLast = " + this.rightLast + " bottomLast = " + this.bottomLast); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

		if (width > 0 && height > 0 && width < this.curveAreaBounds.width && height < this.curveAreaBounds.height) {
			if (log.isLoggable(Level.FINER))
				log.log(Level.FINER, "left = " + left + " top = " + top + " width = " + width + " height = " + height); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			this.canvasGC.drawImage(this.canvasImage, left + this.offSetX, top + this.offSetY, width, height, left + this.offSetX, top + this.offSetY, width, height);
		}

		this.leftLast = this.xPosMeasure <= this.xPosDelta ? this.xPosMeasure : this.xPosDelta;
		this.topLast = this.yPosDelta <= this.yPosMeasure ? this.yPosDelta : this.yPosMeasure;
		this.rightLast = this.xPosDelta >= this.xPosMeasure ? this.xPosDelta : this.xPosMeasure;
		this.bottomLast = this.yPosDelta >= this.yPosMeasure ? this.yPosDelta : this.yPosMeasure;
		if (log.isLoggable(Level.FINER))
			log.log(Level.FINER, "leftLast = " + this.leftLast + " topLast = " + this.topLast + " rightLast = " + this.rightLast + " bottomLast = " + this.bottomLast); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
	}

	/**
	 * erase connecting line by re-drawing the curve area image 
	 * @param posFromLeft1
	 * @param posFromTop1
	 * @param posFromLeft2
	 * @param posFromTop2
	 */
	void eraseConnectingLine(int left, int top, int width, int height) {
		if (width > 0 && height > 0 && width < this.curveAreaBounds.width && height < this.curveAreaBounds.height) {
			this.canvasGC.drawImage(this.canvasImage, left, top, width, height, left + this.offSetX, top + this.offSetY, width, height);
		}
	}

	/**
	 * clean (old) measurement pointer - check pointer in curve area
	 */
	public void cleanMeasurementPointer() {
		try {
			boolean isGCset = false;
			if (this.canvasGC != null && this.canvasGC.isDisposed()) {
				this.canvasGC = new GC(this.graphicCanvas);
			}
			if ((this.xPosMeasure != 0 && (this.xPosMeasure < this.offSetX || this.xPosMeasure > this.offSetX + this.curveAreaBounds.width))
					|| (this.yPosMeasure != 0 && (this.yPosMeasure < this.offSetY || this.yPosMeasure > this.offSetY + this.curveAreaBounds.height))
					|| (this.xPosDelta != 0 && (this.xPosDelta < this.offSetX || this.xPosDelta > this.offSetX + this.curveAreaBounds.width))
					|| (this.yPosDelta != 0 && (this.yPosDelta < this.offSetY || this.yPosDelta > this.offSetY + this.curveAreaBounds.height))) {
				this.redrawGraphics();
				this.xPosMeasure = this.xPosDelta = 0;
			} else {
				if (this.xPosMeasure > 0) {
					eraseVerticalLine(this.xPosMeasure, 0, this.curveAreaBounds.height, 1);
					eraseHorizontalLine(this.yPosMeasure, 0, this.curveAreaBounds.width, 1);
				}
				if (this.xPosDelta > 0) {
					eraseVerticalLine(this.xPosDelta, 0, this.curveAreaBounds.height, 1);
					eraseHorizontalLine(this.yPosDelta, 0, this.curveAreaBounds.width, 1);
					cleanConnectingLineObsoleteRectangle();
				}
			}
			if (isGCset)
				this.canvasGC.dispose();
			if (this.recordSetCommentText != null) {
				this.recordSetComment.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE + 1, SWT.NORMAL));
				this.recordSetComment.setText(this.recordSetCommentText);
			}
			this.application.setStatusMessage(GDE.STRING_EMPTY);
		} catch (RuntimeException e) {
			log.log(Level.WARNING, e.getMessage(), e);
		}
	}

	/**
	 * draw the cut pointer for cut modes
	 * @param mode
	 * @param leftEnabled
	 * @param rightEnabled
	 */
	public void drawCutPointer(int mode, boolean leftEnabled, boolean rightEnabled) {
		this.setModeState(mode); // cleans old pointer if required

		// allow only get the record set to work with
		boolean isGraphicsWindow = this.windowType == GraphicsWindow.TYPE_NORMAL;
		if (isGraphicsWindow) {
			// set the gc properties
			this.canvasGC = new GC(this.graphicCanvas);
			this.canvasGC.setLineWidth(1);
			this.canvasGC.setLineStyle(SWT.LINE_SOLID);
			this.canvasGC.setForeground(SWTResourceManager.getColor(SWT.COLOR_BLUE));

			if (leftEnabled) {
				this.application.setStatusMessage(Messages.getString(MessageIds.GDE_MSGT0258));
				// cleanCutPointer();
				this.xPosCut = this.xPosCut > 0 ? this.xPosCut : this.curveAreaBounds.width * 1 / 4;
				this.canvasGC.setBackgroundPattern(SWTResourceManager.getPattern(0, 0, 50, 50, SWT.COLOR_CYAN, 128, SWT.COLOR_WIDGET_BACKGROUND, 128));
				this.canvasGC.fillRectangle(0 + this.offSetX, 0 + this.offSetY, this.xPosCut, this.curveAreaBounds.height);
				this.canvasGC.setAdvanced(false);
				drawVerticalLine(this.xPosCut, 0, this.curveAreaBounds.height);
			} else if (rightEnabled) {
				this.application.setStatusMessage(Messages.getString(MessageIds.GDE_MSGT0259));
				// cleanCutPointer();
				this.xPosCut = this.xPosCut > 0 ? this.xPosCut : this.curveAreaBounds.width * 3 / 4;
				this.canvasGC.setBackgroundPattern(SWTResourceManager.getPattern(0, 0, 50, 50, SWT.COLOR_CYAN, 128, SWT.COLOR_WIDGET_BACKGROUND, 128));
				this.canvasGC.fillRectangle(this.xPosCut + this.offSetX, 0 + this.offSetY, this.curveAreaBounds.width - this.xPosCut, this.curveAreaBounds.height);
				this.canvasGC.setAdvanced(false);
				drawVerticalLine(this.xPosCut, 0, this.curveAreaBounds.height);
			} else {
				cleanCutPointer();
			}
		}
		this.canvasGC.dispose();
	}

	/**
	 * clean cutting edge pointer
	 */
	public void cleanCutPointer() {
		this.application.setStatusMessage(" "); //$NON-NLS-1$
		eraseVerticalLine(this.xPosCut, 0, this.curveAreaBounds.height, 2);
	}

	/**
	 * switch graphics window mouse mode
	 * @param mode MODE_RESET, MODE_ZOOM, MODE_MEASURE, MODE_DELTA_MEASURE
	 */
	public void setModeState(int mode) {
		this.cleanMeasurementPointer();
		switch (mode) {
		case MODE_ZOOM:
			this.isZoomMouse = true;
			this.isLeftMouseMeasure = false;
			this.isRightMouseMeasure = false;
			this.isPanMouse = false;
			this.isScopeMode = false;
			break;
		case MODE_MEASURE:
			this.isZoomMouse = false;
			this.isLeftMouseMeasure = true;
			this.isRightMouseMeasure = false;
			this.isPanMouse = false;
			this.isScopeMode = false;
			break;
		case MODE_MEASURE_DELTA:
			this.isZoomMouse = false;
			this.isLeftMouseMeasure = false;
			this.isRightMouseMeasure = true;
			this.isPanMouse = false;
			this.isScopeMode = false;
			break;
		case MODE_PAN:
			this.isZoomMouse = false;
			this.isLeftMouseMeasure = false;
			this.isRightMouseMeasure = false;
			this.isPanMouse = true;
			this.isScopeMode = false;
			break;
		case MODE_CUT_LEFT:
			this.isZoomMouse = false;
			this.isLeftMouseMeasure = false;
			this.isRightMouseMeasure = false;
			this.isPanMouse = false;
			this.isLeftCutMode = true;
			this.isRightCutMode = false;
			this.isScopeMode = false;
			break;
		case MODE_CUT_RIGHT:
			this.isZoomMouse = false;
			this.isLeftMouseMeasure = false;
			this.isRightMouseMeasure = false;
			this.isPanMouse = false;
			this.isLeftCutMode = false;
			this.isRightCutMode = true;
			this.isScopeMode = false;
			break;
		case MODE_SCOPE:
			this.isZoomMouse = false;
			this.isLeftMouseMeasure = false;
			this.isRightMouseMeasure = false;
			this.isPanMouse = false;
			this.isLeftCutMode = false;
			this.isRightCutMode = false;
			this.isScopeMode = true;
			break;
		case MODE_RESET:
		default:
			this.isZoomMouse = false;
			this.isLeftMouseMeasure = false;
			this.isRightMouseMeasure = false;
			this.isPanMouse = false;
			this.isLeftCutMode = false;
			this.isRightCutMode = false;
			this.isScopeMode = false;
			this.application.setStatusMessage(GDE.STRING_EMPTY);
			this.xPosCut = -1;
			this.xLast = 0;
			this.yLast = 0;
			this.leftLast = 0;
			this.topLast = 0;
			this.rightLast = 0;
			this.bottomLast = 0;
			updatePanMenueButton();
			// updateCutModeButtons();
			this.application.getMenuToolBar().resetZoomToolBar();
			break;
		}
	}

	/**
	 * 
	 */
	private void updatePanMenueButton() {
		this.application.getMenuBar().enablePanButton(this.isZoomMouse || this.isPanMouse);
		this.application.getMenuToolBar().enablePanButton(this.isZoomMouse || this.isPanMouse);
	}

	/**
	 * check input x,y value against curve are bounds and correct to bound if required
	 * @param Point containing corrected x,y position value
	 */
	private Point checkCurveBounds(int xPos, int yPos) {
		if (log.isLoggable(Level.FINER))
			log.log(Level.FINER, "in  xPos = " + xPos + " yPos = " + yPos); //$NON-NLS-1$ //$NON-NLS-2$
		int tmpxPos = xPos - this.offSetX;
		int tmpyPos = yPos - this.offSetY;
		int minX = 0;
		int maxX = this.curveAreaBounds.width;
		int minY = 0;
		int maxY = this.curveAreaBounds.height;
		if (tmpxPos < minX || tmpxPos > maxX) {
			tmpxPos = tmpxPos < minX ? minX : maxX;
		}
		if (tmpyPos < minY || tmpyPos > maxY) {
			tmpyPos = tmpyPos < minY ? minY : maxY;
		}
		if (log.isLoggable(Level.FINER))
			log.log(Level.FINER, "out xPos = " + tmpxPos + " yPos = " + tmpyPos); //$NON-NLS-1$ //$NON-NLS-2$
		return new Point(tmpxPos, tmpyPos);
	}

	/**
	 * @param evt
	 */
	void mouseMoveAction(MouseEvent evt) {
		Channel activeChannel = Channels.getInstance().getActiveChannel();
		if (activeChannel != null) {
			RecordSet recordSet = (this.windowType == GraphicsWindow.TYPE_NORMAL) ? activeChannel.getActiveRecordSet() : this.application.getCompareSet();
			if (recordSet != null && this.canvasImage != null) {
				this.canvasGC = new GC(this.graphicCanvas);
				Point point = checkCurveBounds(evt.x, evt.y);
				evt.x = point.x;
				evt.y = point.y;

				String measureRecordKey = recordSet.getRecordKeyMeasurement();
				this.canvasGC.setLineWidth(1);
				this.canvasGC.setLineStyle(SWT.LINE_DASH);

				if ((evt.stateMask & SWT.NO_FOCUS) == SWT.NO_FOCUS) {
					try {
						if (this.isZoomMouse && recordSet.isZoomMode() && this.isResetZoomPosition) {
							if (log.isLoggable(Level.FINER))
								log.log(Level.FINER, String.format("xDown = %d, evt.x = %d, xLast = %d  -  yDown = %d, evt.y = %d, yLast = %d", this.xDown, evt.x, this.xLast, this.yDown, evt.y, this.yLast)); //$NON-NLS-1$

							// clean obsolete rectangle
							int left = this.xLast - this.xDown > 0 ? this.xDown : this.xLast;
							int top = this.yLast - this.yDown > 0 ? this.yDown : this.yLast;
							int width = this.xLast - this.xDown > 0 ? this.xLast - this.xDown : this.xDown - this.xLast;
							int height = this.yLast - this.yDown > 0 ? this.yLast - this.yDown : this.yDown - this.yLast;
							if (log.isLoggable(Level.FINER))
								log.log(Level.FINER, "clean left = " + left + " top = " + top + " width = " + width + " height = " + height); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
							eraseHorizontalLine(top, left, width + 1, 1);
							eraseVerticalLine(left, top, height + 1, 1);
							eraseHorizontalLine(top + height, left + 1, width, 1);
							eraseVerticalLine(left + width, top + 1, height, 1);

							left = evt.x - this.xDown > 0 ? this.xDown + this.offSetX : evt.x + this.offSetX;
							top = evt.y - this.yDown > 0 ? this.yDown + this.offSetY : evt.y + this.offSetY;
							width = evt.x - this.xDown > 0 ? evt.x - this.xDown : this.xDown - evt.x;
							height = evt.y - this.yDown > 0 ? evt.y - this.yDown : this.yDown - evt.y;
							if (log.isLoggable(Level.FINER))
								log.log(Level.FINER, "draw  left = " + (left - this.offSetX) + " top = " + (top - this.offSetY) + " width = " + width + " height = " + height); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
							this.canvasGC.drawRectangle(left, top, width, height);

							// detect directions to enable zoom or reset
							if (this.xDown < evt.x) { // left -> right
								// System.out.println("left -> right -> zoom selected area");
								this.isTransientZoom = true;
							}
							if (this.xDown > evt.x) { // right -> left
								// System.out.println("right -> left -> zoom reset");
								this.isTransientZoom = false;
							}

							this.xLast = evt.x;
							this.yLast = evt.y;
						} else if (this.isLeftMouseMeasure) {
							Record record = recordSet.getRecord(measureRecordKey);
							// clear old measure lines
							eraseVerticalLine(this.xPosMeasure, 0, this.curveAreaBounds.height, 1);
							// no change don't needs to be calculated, but the calculation limits to bounds
							this.yPosMeasure = record.getVerticalDisplayPointValue(this.xPosMeasure);
							eraseHorizontalLine(this.yPosMeasure, 0, this.curveAreaBounds.width, 1);

							if (recordSet.isDeltaMeasurementMode(measureRecordKey)) {
								// clear old delta measure lines
								eraseVerticalLine(this.xPosDelta, 0, this.curveAreaBounds.height, 1);
								// no change don't needs to be calculated, but the calculation limits to bounds
								this.yPosDelta = record.getVerticalDisplayPointValue(this.xPosDelta);
								eraseHorizontalLine(this.yPosDelta, 0, this.curveAreaBounds.width, 1);

								// clean obsolete rectangle of connecting line
								cleanConnectingLineObsoleteRectangle();

								this.canvasGC.setForeground(SWTResourceManager.getColor(SWT.COLOR_BLUE));
								this.canvasGC.setLineStyle(SWT.LINE_DASH);
								drawVerticalLine(this.xPosDelta, 0, this.curveAreaBounds.height);
								drawHorizontalLine(this.yPosDelta, 0, this.curveAreaBounds.width);
								this.canvasGC.setForeground(SWTResourceManager.getColor(SWT.COLOR_BLACK));
							}
							// all obsolete lines are cleaned up now draw new position marker
							this.xPosMeasure = evt.x; // evt.x is already relative to curve area
							drawVerticalLine(this.xPosMeasure, 0, this.curveAreaBounds.height);
							this.yPosMeasure = record.getVerticalDisplayPointValue(this.xPosMeasure);
							drawHorizontalLine(this.yPosMeasure, 0, this.curveAreaBounds.width);

							if (recordSet.isDeltaMeasurementMode(measureRecordKey)) {
								if (this.xPosMeasure != this.xPosDelta && this.yPosMeasure != this.yPosDelta) {
									drawConnectingLine(this.xPosMeasure, this.yPosMeasure, this.xPosDelta, this.yPosDelta, SWT.COLOR_BLACK);
								}
								this.application.setStatusMessage(Messages.getString(MessageIds.GDE_MSGT0257, new Object[] { record.getName(),
										Messages.getString(MessageIds.GDE_MSGT0212),
										record.getVerticalDisplayDeltaAsFormattedValue(this.yPosMeasure - this.yPosDelta, this.curveAreaBounds), record.getUnit(),
										TimeLine.getFomatedTimeWithUnit(record.getHorizontalDisplayPointTime_ms(this.xPosDelta)
												- record.getHorizontalDisplayPointTime_ms(this.xPosMeasure)),
										record.getSlopeValue(new Point(this.xPosDelta - this.xPosMeasure, this.yPosMeasure - this.yPosDelta)), record.getUnit() }));
							} else {
								this.recordSetComment.setText(this.getSelectedMeasurementsAsTable());
								this.application.setStatusMessage(Messages.getString(MessageIds.GDE_MSGT0256, new Object[] { record.getName(),
										record.getVerticalDisplayPointAsFormattedScaleValue(this.yPosMeasure, this.curveAreaBounds), record.getUnit(),
										record.getHorizontalDisplayPointAsFormattedTimeWithUnit(this.xPosMeasure) }));
							}
						} else if (this.isRightMouseMeasure) {
							Record record = recordSet.getRecord(measureRecordKey);
							// clear old delta measure lines
							eraseVerticalLine(this.xPosDelta, 0, this.curveAreaBounds.height, 1);
							// no change don't needs to be calculated, but the calculation limits to bounds
							this.yPosMeasure = record.getVerticalDisplayPointValue(this.xPosMeasure);
							eraseHorizontalLine(this.yPosDelta, 0, this.curveAreaBounds.width, 1);

							// clear old measure lines
							eraseVerticalLine(this.xPosMeasure, 0, this.curveAreaBounds.height, 1);
							// no change don't needs to be calculated, but the calculation limits to bounds
							this.yPosDelta = record.getVerticalDisplayPointValue(this.xPosDelta);
							eraseHorizontalLine(this.yPosMeasure, 0, this.curveAreaBounds.width, 1);

							// clean obsolete rectangle of connecting line
							cleanConnectingLineObsoleteRectangle();

							// always needs to draw measurement pointer
							drawVerticalLine(this.xPosMeasure, 0, this.curveAreaBounds.height);
							// no change don't needs to be calculated yPosMeasure = record.getDisplayPointDataValue(xPosMeasure, curveAreaBounds);
							drawHorizontalLine(this.yPosMeasure, 0, this.curveAreaBounds.width);

							// update the new delta position
							this.xPosDelta = evt.x; // evt.x is already relative to curve area
							this.canvasGC.setForeground(SWTResourceManager.getColor(SWT.COLOR_BLUE));
							this.canvasGC.setLineStyle(SWT.LINE_DASH);
							drawVerticalLine(this.xPosDelta, 0, this.curveAreaBounds.height);
							this.yPosDelta = record.getVerticalDisplayPointValue(this.xPosDelta);
							drawHorizontalLine(this.yPosDelta, 0, this.curveAreaBounds.width);

							if (this.xPosMeasure != this.xPosDelta && this.yPosMeasure != this.yPosDelta) {
								drawConnectingLine(this.xPosMeasure, this.yPosMeasure, this.xPosDelta, this.yPosDelta, SWT.COLOR_BLACK);
							}

							this.canvasGC.setForeground(SWTResourceManager.getColor(SWT.COLOR_BLACK));

							this.application.setStatusMessage(Messages.getString(MessageIds.GDE_MSGT0257, new Object[] { record.getName(), Messages.getString(MessageIds.GDE_MSGT0212),
									record.getVerticalDisplayDeltaAsFormattedValue(this.yPosMeasure - this.yPosDelta, this.curveAreaBounds), record.getUnit(),
									TimeLine.getFomatedTimeWithUnit(record.getHorizontalDisplayPointTime_ms(this.xPosDelta) - record.getHorizontalDisplayPointTime_ms(this.xPosMeasure)),
									record.getSlopeValue(new Point(this.xPosDelta - this.xPosMeasure, this.yPosMeasure - this.yPosDelta)), record.getUnit() }));
						} else if (this.isPanMouse) {
							this.xDeltaPan = (this.xLast != 0 && this.xLast != evt.x) ? (this.xDeltaPan + (this.xLast < evt.x ? -1 : 1)) : 0;
							this.yDeltaPan = (this.yLast != 0 && this.yLast != evt.y) ? (this.yDeltaPan + (this.yLast < evt.y ? 1 : -1)) : 0;
							if (log.isLoggable(Level.FINER))
								log.log(Level.FINER, " xDeltaPan = " + this.xDeltaPan + " yDeltaPan = " + this.yDeltaPan); //$NON-NLS-1$ //$NON-NLS-2$
							if ((this.xDeltaPan != 0 && this.xDeltaPan % 5 == 0) || (this.yDeltaPan != 0 && this.yDeltaPan % 5 == 0)) {
								recordSet.shift(this.xDeltaPan, this.yDeltaPan); // 10% each direction
								this.redrawGraphics(); // this.graphicCanvas.redraw();?
								this.xDeltaPan = this.yDeltaPan = 0;
							}
							this.xLast = evt.x;
							this.yLast = evt.y;
						} else if (this.isLeftCutMode) {
							// clear old cut area
							if (evt.x < this.xPosCut) {
								this.canvasGC.drawImage(this.canvasImage, evt.x + this.offSetX, this.offSetY, this.xPosCut - evt.x + 1, this.curveAreaBounds.height, evt.x
										+ this.offSetX, this.offSetY, this.xPosCut - evt.x + 1, this.curveAreaBounds.height);
							} else { // evt.x > this.xPosCut
								this.canvasGC.drawImage(this.canvasImage, this.xPosCut + this.offSetX, this.offSetY, evt.x - this.xPosCut, this.curveAreaBounds.height, this.xPosCut
										+ this.offSetX, this.offSetY, evt.x - this.xPosCut, this.curveAreaBounds.height);
								this.canvasGC.setBackgroundPattern(SWTResourceManager.getPattern(0, 0, 50, 50, SWT.COLOR_CYAN, 128, SWT.COLOR_WIDGET_BACKGROUND, 128));
								this.canvasGC.fillRectangle(this.xPosCut + this.offSetX, this.offSetY, evt.x - this.xPosCut, this.curveAreaBounds.height);
								this.canvasGC.setAdvanced(false);
							}
							this.xPosCut = evt.x;
							this.canvasGC.setLineStyle(SWT.LINE_SOLID);
							drawVerticalLine(this.xPosCut, 0, this.curveAreaBounds.height);
						} else if (this.isRightCutMode) {
							// clear old cut lines
							if (evt.x > this.xPosCut) {
								this.canvasGC.drawImage(this.canvasImage, this.xPosCut + this.offSetX, this.offSetY, evt.x - this.xPosCut, this.curveAreaBounds.height, this.offSetX
										+ this.xPosCut, this.offSetY, evt.x - this.xPosCut, this.curveAreaBounds.height);
							} else { // evt.x < this.xPosCut
								this.canvasGC.drawImage(this.canvasImage, evt.x + this.offSetX, this.offSetY, this.xPosCut - evt.x + 1, this.curveAreaBounds.height, evt.x
										+ this.offSetX, this.offSetY, this.xPosCut - evt.x + 1, this.curveAreaBounds.height);
								this.canvasGC.setBackgroundPattern(SWTResourceManager.getPattern(0, 0, 50, 50, SWT.COLOR_CYAN, 128, SWT.COLOR_WIDGET_BACKGROUND, 128));
								this.canvasGC.fillRectangle(evt.x + this.offSetX, 0 + this.offSetY, this.xPosCut - evt.x + 1, this.curveAreaBounds.height);
								this.canvasGC.setAdvanced(false);
							}
							this.xPosCut = evt.x;
							this.canvasGC.setLineStyle(SWT.LINE_SOLID);
							drawVerticalLine(this.xPosCut, 0, this.curveAreaBounds.height);
						}
					} catch (RuntimeException e) {
						log.log(Level.WARNING, "mouse pointer out of range", e); //$NON-NLS-1$
					}
				} else if (measureRecordKey != null && (recordSet.isMeasurementMode(measureRecordKey) || recordSet.isDeltaMeasurementMode(measureRecordKey))) {
					if (this.xPosMeasure + 1 >= evt.x && this.xPosMeasure - 1 <= evt.x || this.xPosDelta + 1 >= evt.x && this.xPosDelta - 1 <= evt.x) { // snap mouse pointer
						this.graphicCanvas.setCursor(SWTResourceManager.getCursor("gde/resource/MoveH.gif")); //$NON-NLS-1$
					} else {
						this.graphicCanvas.setCursor(this.application.getCursor());
					}
				} else if (this.isZoomMouse && !this.isPanMouse) {
					this.graphicCanvas.setCursor(SWTResourceManager.getCursor(SWT.CURSOR_CROSS));
				} else if (this.isPanMouse) {
					this.graphicCanvas.setCursor(SWTResourceManager.getCursor("gde/resource/Hand.gif")); //$NON-NLS-1$
				} else if (this.isLeftCutMode || this.isRightCutMode) {
					if (this.xPosCut + 1 >= evt.x && this.xPosCut - 1 <= evt.x) { // snap mouse pointer
						this.graphicCanvas.setCursor(SWTResourceManager.getCursor("gde/resource/MoveH.gif")); //$NON-NLS-1$
					} else {
						this.graphicCanvas.setCursor(this.application.getCursor());
					}
				} else {
					this.graphicCanvas.setCursor(this.application.getCursor());
				}
				this.canvasGC.dispose();
			}
		}
	}

	/**
	 * @param evt
	 */
	void mouseDownAction(MouseEvent evt) {
		Channel activeChannel = Channels.getInstance().getActiveChannel();
		if (activeChannel != null) {
			RecordSet recordSet = (this.windowType == GraphicsWindow.TYPE_NORMAL) ? activeChannel.getActiveRecordSet() : this.application.getCompareSet();
			if (this.canvasImage != null && recordSet != null) {
				String measureRecordKey = recordSet.getRecordKeyMeasurement();
				Point point = checkCurveBounds(evt.x, evt.y);
				this.xDown = point.x;
				this.yDown = point.y;

				if (measureRecordKey != null && (recordSet.isMeasurementMode(measureRecordKey) || recordSet.isDeltaMeasurementMode(measureRecordKey))
						&& this.xPosMeasure + 1 >= this.xDown
						&& this.xPosMeasure - 1 <= this.xDown) { // snap mouse pointer
					this.isLeftMouseMeasure = true;
					this.isRightMouseMeasure = false;
				} else if (measureRecordKey != null && recordSet.isDeltaMeasurementMode(measureRecordKey) && this.xPosDelta + 1 >= this.xDown && this.xPosDelta - 1 <= this.xDown) { // snap mouse pointer
					this.isRightMouseMeasure = true;
					this.isLeftMouseMeasure = false;
				} else if (!this.isPanMouse && !this.isLeftCutMode && !this.isRightCutMode) {
					if (!this.isZoomMouse) // setting zoom mode is only required at the beginning of zoom actions, it will reset scale values to initial values
						this.application.setGraphicsMode(HistoGraphicsComposite.MODE_ZOOM, true);
					this.xLast = this.xDown;
					this.yLast = this.yDown;
					this.isResetZoomPosition = true;
				} else {
					this.isLeftMouseMeasure = false;
					this.isRightMouseMeasure = false;
				}
				if (log.isLoggable(Level.FINER))
					log.log(Level.FINER, "isMouseMeasure = " + this.isLeftMouseMeasure + " isMouseDeltaMeasure = " + this.isRightMouseMeasure); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
	}

	/**
	 * @param evt
	 */
	void mouseUpAction(MouseEvent evt) {
		Channel activeChannel = Channels.getInstance().getActiveChannel();
		if (activeChannel != null) {
			RecordSet recordSet = (this.windowType == GraphicsWindow.TYPE_NORMAL) ? Channels.getInstance().getActiveChannel().getActiveRecordSet() : this.application.getCompareSet();
			if (this.canvasImage != null && recordSet != null) {
				Point point = checkCurveBounds(evt.x, evt.y);
				this.xUp = point.x;
				this.yUp = point.y;

				if (this.isZoomMouse) {
					if (this.isTransientZoom) {
						this.isResetZoomPosition = false;
						if (log.isLoggable(Level.FINEST))
							log.log(Level.FINEST, this.isZoomMouse + " - " + recordSet.isZoomMode() + " - " + this.isResetZoomPosition); //$NON-NLS-1$

						// sort the zoom values
						int xStart, xEnd, yMin, yMax;
						if (this.isZoomX) {
							xStart = this.xDown < this.xUp ? this.xDown : this.xUp;
							xEnd = this.xDown > this.xUp ? this.xDown + 1 : this.xUp;
							yMin = 0;
							yMax = this.curveAreaBounds.height - this.curveAreaBounds.y;
						} else if (this.isZoomY) {
							xStart = 0;
							xEnd = this.curveAreaBounds.width;
							yMin = this.curveAreaBounds.height - (this.yDown > this.yUp ? this.yDown : this.yUp);
							yMax = this.curveAreaBounds.height - (this.yDown < this.yUp ? this.yDown : this.yUp);
						} else {
							xStart = this.xDown < this.xUp ? this.xDown : this.xUp;
							xEnd = this.xDown > this.xUp ? this.xDown + 1 : this.xUp;
							yMin = this.curveAreaBounds.height - (this.yDown > this.yUp ? this.yDown : this.yUp);
							yMax = this.curveAreaBounds.height - (this.yDown < this.yUp ? this.yDown : this.yUp);
						}
						if (log.isLoggable(Level.FINER))
							log.log(Level.FINER, "zoom xStart = " + xStart + " xEnd = " + xEnd + " yMin = " + yMin + " yMax = " + yMax); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
						if (xEnd - xStart > 5 && yMax - yMin > 5) {
							recordSet.setDisplayZoomBounds(new Rectangle(xStart, yMin, xEnd - xStart, yMax - yMin));
							this.redrawGraphics(); // this.graphicCanvas.redraw();
						}
					} else {
						this.application.setGraphicsMode(HistoGraphicsComposite.MODE_RESET, false);
					}
				} else if (this.isLeftMouseMeasure) {
					this.isLeftMouseMeasure = false;
					// application.setStatusMessage(GDE.STRING_EMPTY);
				} else if (this.isRightMouseMeasure) {
					this.isRightMouseMeasure = false;
					// application.setStatusMessage(GDE.STRING_EMPTY);
				} else if (this.isLeftCutMode) {
					if (SWT.OK == this.application.openOkCancelMessageDialog(Messages.getString(MessageIds.GDE_MSGT0260))) {
						recordSet = recordSet.clone(recordSet.get(0).getHorizontalPointIndexFromDisplayPoint(this.xUp), true);
						recordSet.setRecalculationRequired();
						this.channels.getActiveChannel().put(recordSet.getName(), recordSet);
						this.application.getMenuToolBar().addRecordSetName(recordSet.getName());
						this.channels.getActiveChannel().switchRecordSet(recordSet.getName());
						setModeState(HistoGraphicsComposite.MODE_RESET);
					}
				} else if (this.isRightCutMode) {
					if (SWT.OK == this.application.openOkCancelMessageDialog(Messages.getString(MessageIds.GDE_MSGT0260))) {
						recordSet = recordSet.clone(recordSet.get(0).getHorizontalPointIndexFromDisplayPoint(this.xUp), false);
						recordSet.setRecalculationRequired();
						this.channels.getActiveChannel().put(recordSet.getName(), recordSet);
						this.application.getMenuToolBar().addRecordSetName(recordSet.getName());
						this.channels.getActiveChannel().switchRecordSet(recordSet.getName());
						setModeState(HistoGraphicsComposite.MODE_RESET);
					}
				}
				updatePanMenueButton();
				// updateCutModeButtons();
				if (log.isLoggable(Level.FINER))
					log.log(Level.FINER, "isMouseMeasure = " + this.isLeftMouseMeasure + " isMouseDeltaMeasure = " + this.isRightMouseMeasure); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
	}

	/**
	 * check if cut mode can be activated
	 * @param recordSet
	 */
	void updateCutModeButtons() {
		Channel activeChannel = Channels.getInstance().getActiveChannel();
		if (activeChannel != null) {
			RecordSet recordSet = (this.windowType == GraphicsWindow.TYPE_NORMAL) ? Channels.getInstance().getActiveChannel().getActiveRecordSet() : (this.windowType == GraphicsWindow.TYPE_COMPARE) ? this.application.getCompareSet() : this.application.getUtilitySet();
			if (this.canvasImage != null && recordSet != null) {
				//
				if (recordSet.isCutLeftEdgeEnabled()) {
					this.application.getMenuToolBar().enableCutButtons(true, false);
				} else if (recordSet.isCutRightEdgeEnabled()) {
					this.application.getMenuToolBar().enableCutButtons(false, true);
				} else {
					this.application.getMenuToolBar().enableCutButtons(false, false);
				}
			}
		}
	}

	/**
	 * enable display of graphics header
	 */
	public void enableGraphicsHeader(boolean enabled) {
		if (enabled) {
			this.headerGap = 5;
			GC gc = new GC(this.graphicsHeader);
			int stringHeight = gc.stringExtent(this.graphicsHeader.getText()).y;
			this.headerGap = 5;
			this.headerHeight = stringHeight;
			gc.dispose();
		} else {
			this.headerGap = 0;
			this.headerHeight = 0;
		}
		setComponentBounds();
	}

	/**
	 * enable display of record set comment
	 */
	public void enableRecordSetComment(boolean enabled) {
		if (enabled) {
			this.commentGap = 0;
			GC gc = new GC(this.recordSetComment);
			int stringHeight = gc.stringExtent(this.recordSetComment.getText()).y;
			this.commentHeight = stringHeight * 2 + 8;
			gc.dispose();
		} else {
			this.commentGap = 0;
			this.commentHeight = 0;
		}
		setComponentBounds();
	}

	public void clearHeaderAndComment() {
		if (HistoGraphicsComposite.this.channels.getActiveChannel() != null) {
			RecordSet recordSet = HistoGraphicsComposite.this.channels.getActiveChannel().getActiveRecordSet();
			if (recordSet == null) {
				HistoGraphicsComposite.this.recordSetComment.setText(GDE.STRING_EMPTY);
				HistoGraphicsComposite.this.graphicsHeader.setText(GDE.STRING_EMPTY);
				HistoGraphicsComposite.this.graphicsHeaderText = null;
				HistoGraphicsComposite.this.recordSetCommentText = null;
				this.recordSetComment.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE + 1, SWT.NORMAL));
			}
			updateCaptions();
		}
	}

	public synchronized void updateCaptions() {
		HistoGraphicsComposite.this.recordSetComment.redraw();
		HistoGraphicsComposite.this.graphicsHeader.redraw();
	}

	/**
	 * resize the three areas: header, curve, comment
	 */
	void setComponentBounds() {
		Rectangle graphicsBounds = this.getClientArea();
		// this.application.setGraphicsSashFormWeights(this.graphicSashForm.getSize().x - graphicsBounds.width);
		int x = 0;
		int y = this.headerGap;
		int width = graphicsBounds.width;
		int height = this.headerHeight;
		this.graphicsHeader.setBounds(x, y, width, height);
		if (log.isLoggable(Level.FINER))
			log.log(Level.FINER, "recordSetHeader.setBounds " + this.graphicsHeader.getBounds());

		y = this.headerGap + this.headerHeight;
		height = graphicsBounds.height - (this.headerGap + this.commentGap + this.commentHeight + this.headerHeight);
		this.graphicCanvas.setBounds(x, y, width, height);
		if (log.isLoggable(Level.FINER))
			log.log(Level.FINER, "graphicCanvas.setBounds " + this.graphicCanvas.getBounds());

		y = this.headerGap + this.headerHeight + height + this.commentGap;
		height = this.commentHeight;
		this.recordSetComment.setBounds(20, y, width - 40, height - 5);
		if (log.isLoggable(Level.FINER))
			log.log(Level.FINER, "recordSetComment.setBounds " + this.recordSetComment.getBounds());
	}

	/**
	 * @return the isRecordCommentChanged
	 */
	public boolean isRecordCommentChanged() {
		return this.isRecordCommentChanged;
	}

	public void updateRecordSetComment() {
		Channel activeChannel = HistoGraphicsComposite.this.channels.getActiveChannel();
		if (activeChannel != null) {
			RecordSet recordSet = activeChannel.getActiveRecordSet();
			if (recordSet != null) {
				if (this.isRecordCommentChanged) {
					recordSet.setRecordSetDescription(HistoGraphicsComposite.this.recordSetComment.getText());
					recordSet.setUnsaved(RecordSet.UNSAVED_REASON_DATA);
				} else {
					this.recordSetComment.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE + 1, SWT.NORMAL));
					this.recordSetComment.setText(this.recordSetCommentText = recordSet.getRecordSetDescription());
					String graphicsHeaderExtend = this.graphicsHeaderText == null ? GDE.STRING_MESSAGE_CONCAT + recordSet.getName() : this.graphicsHeaderText.substring(11);
					this.graphicsHeader.setText(this.graphicsHeaderText = String.format("%s %s", StringHelper.getFormatedTime("yyyy-MM-dd", recordSet.getStartTimeStamp()), graphicsHeaderExtend));
					this.graphicsHeader.redraw();
				}
				this.isRecordCommentChanged = false;
			}
		}
	}

	/**
	 * @return the graphic window content as image - only if compare window is visible return the compare window graphics
	 */
	public Image getGraphicsPrintImage() {
		Image graphicsImage = null;
		int graphicsHeight = 30 + this.canvasBounds.height + 40;
		// decide if normal graphics window or compare window should be copied
		if (this.windowType == GraphicsWindow.TYPE_COMPARE) {
			RecordSet compareRecordSet = DataExplorer.getInstance().getCompareSet();
			int numberCompareSetRecords = compareRecordSet.size();
			graphicsHeight = 30 + this.canvasBounds.height + 10 + numberCompareSetRecords * 20;
			graphicsImage = new Image(GDE.display, this.canvasBounds.width, graphicsHeight);
			GC graphicsGC = new GC(graphicsImage);
			graphicsGC.setBackground(this.surroundingBackground);
			graphicsGC.setForeground(this.graphicsHeader.getForeground());
			graphicsGC.fillRectangle(0, 0, this.canvasBounds.width, graphicsHeight);
			graphicsGC.setFont(this.graphicsHeader.getFont());
			GraphicsUtils.drawTextCentered(Messages.getString(MessageIds.GDE_MSGT0144), this.canvasBounds.width / 2, 20, graphicsGC, SWT.HORIZONTAL);
			graphicsGC.setFont(this.recordSetComment.getFont());
			for (int i = 0, yPos = 30 + this.canvasBounds.height + 5; i < numberCompareSetRecords; ++i, yPos += 20) {
				Record compareRecord = compareRecordSet.get(i);
				if (compareRecord != null) {
					graphicsGC.setForeground(compareRecord.getColor());
					String recordName = "--- " + compareRecord.getName(); //$NON-NLS-1$
					GraphicsUtils.drawText(recordName, 20, yPos, graphicsGC, SWT.HORIZONTAL);
					graphicsGC.setForeground(this.recordSetComment.getForeground());
					String description = compareRecord.getDescription();
					description = description.contains("\n") ? description.substring(0, description.indexOf("\n")) : description; //$NON-NLS-1$ //$NON-NLS-2$
					Point pt = graphicsGC.textExtent(recordName); // string dimensions
					GraphicsUtils.drawText(description, pt.x + 30, yPos, graphicsGC, SWT.HORIZONTAL);
				}
			}
			graphicsGC.drawImage(this.canvasImage, 0, 30);
			graphicsGC.dispose();
		} else if (this.windowType == GraphicsWindow.TYPE_UTIL) {
			graphicsHeight = 30 + this.canvasBounds.height;
			graphicsImage = new Image(GDE.display, this.canvasBounds.width, graphicsHeight);
			GC graphicsGC = new GC(graphicsImage);
			graphicsGC.setBackground(this.surroundingBackground);
			graphicsGC.setForeground(this.graphicsHeader.getForeground());
			graphicsGC.fillRectangle(0, 0, this.canvasBounds.width, graphicsHeight);
			graphicsGC.setFont(this.graphicsHeader.getFont());
			GraphicsUtils.drawTextCentered(this.graphicsHeader.getText(), this.canvasBounds.width / 2, 20, graphicsGC, SWT.HORIZONTAL);
			graphicsGC.drawImage(this.canvasImage, 0, 30);
			graphicsGC.dispose();
		} else {
			Channel activeChannel = this.channels.getActiveChannel();
			if (activeChannel != null) {
				RecordSet activeRecordSet = activeChannel.getActiveRecordSet();
				if (activeRecordSet != null) {
					if (this.canvasImage != null)
						this.canvasImage.dispose();
					this.canvasImage = new Image(GDE.display, this.canvasBounds);
					this.canvasImageGC = new GC(this.canvasImage); // SWTResourceManager.getGC(this.canvasImage);
					this.canvasImageGC.setBackground(this.surroundingBackground);
					this.canvasImageGC.fillRectangle(this.canvasBounds);
					this.canvasImageGC.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.canvasGC = new GC(this.graphicCanvas); // SWTResourceManager.getGC(this.graphicCanvas, "curveArea_" + this.windowType);
					drawCurves(activeRecordSet, this.canvasBounds, this.canvasImageGC);
					graphicsImage = new Image(GDE.display, this.canvasBounds.width, graphicsHeight);
					GC graphicsGC = new GC(graphicsImage);
					graphicsGC.setForeground(this.graphicsHeader.getForeground());
					graphicsGC.setBackground(this.surroundingBackground);
					graphicsGC.setFont(this.graphicsHeader.getFont());
					graphicsGC.fillRectangle(0, 0, this.canvasBounds.width, graphicsHeight);
					if (this.graphicsHeader.getText().length() > 1) {
						GraphicsUtils.drawTextCentered(this.graphicsHeader.getText(), this.canvasBounds.width / 2, 20, graphicsGC, SWT.HORIZONTAL);
					}
					graphicsGC.setFont(this.recordSetComment.getFont());
					if (this.recordSetComment.getText().length() > 1) {
						GraphicsUtils.drawText(this.recordSetComment.getText(), 20, graphicsHeight - 40, graphicsGC, SWT.HORIZONTAL);
					}
					graphicsGC.drawImage(this.canvasImage, 0, 30);
					graphicsGC.dispose();
					this.canvasGC.dispose();
					this.canvasImageGC.dispose();
				}
			}
		}
		return graphicsImage;
	}

	public void setFileComment() {
		Channel activeChannel = this.channels.getActiveChannel();
		if (activeChannel != null) {
			String fileComment = this.graphicsHeader.getText();
			if (fileComment.indexOf(GDE.STRING_MESSAGE_CONCAT) > 1) {
				fileComment = fileComment.substring(0, fileComment.indexOf(GDE.STRING_MESSAGE_CONCAT));
			} else {
				RecordSet activeRecordSet = activeChannel.getActiveRecordSet();
				if (activeRecordSet != null && fileComment.indexOf(activeRecordSet.getName()) > 1) {
					fileComment = fileComment.substring(0, fileComment.indexOf(activeRecordSet.getName()));
				}
			}
			activeChannel.setFileDescription(fileComment);
			activeChannel.setUnsaved(RecordSet.UNSAVED_REASON_DATA);
		}
	}

	private String getSelectedMeasurementsAsTable() {
		Properties displayProps = this.settings.getMeasurementDisplayProperties();
		RecordSet activeRecordSet = this.application.getActiveRecordSet();
		if (activeRecordSet != null) {
			this.recordSetComment.setFont(SWTResourceManager.getFont("Courier New", GDE.WIDGET_FONT_SIZE + 1, SWT.BOLD));
			Vector<Record> records = activeRecordSet.getVisibleAndDisplayableRecordsForMeasurement();
			String formattedTimeWithUnit = records.firstElement().getHorizontalDisplayPointAsFormattedTimeWithUnit(this.xPosMeasure);
			StringBuilder sb = new StringBuilder().append(String.format(" %16s ", formattedTimeWithUnit.substring(formattedTimeWithUnit.indexOf(GDE.STRING_LEFT_BRACKET))));
			for (Record record : records) {
				if (displayProps.getProperty(record.getName()) != null)
					sb.append(String.format("|%-10s", displayProps.getProperty(record.getName())));
				else {
					final String unit = GDE.STRING_LEFT_BRACKET + record.getUnit() + GDE.STRING_RIGHT_BRACKET;
					final String name = record.getName().substring(0, record.getName().length() >= 10 - unit.length() ? 10 - unit.length() : record.getName().length());
					final String format = "|%-" + (10 - unit.length()) + "s%" + unit.length() + "s";
					sb.append(String.format(format, name, unit));
				}
			}
			sb.append("| ").append(GDE.LINE_SEPARATOR).append(String.format("%16s  ", formattedTimeWithUnit.substring(0, formattedTimeWithUnit.indexOf(GDE.STRING_LEFT_BRACKET)
					- 1)));
			for (Record record : records) {
				sb.append(String.format("|%7s   ", record.getVerticalDisplayPointAsFormattedScaleValue(record.getVerticalDisplayPointValue(this.xPosMeasure), this.curveAreaBounds)));
			}
			return sb.append("|").toString();
		}
		this.recordSetComment.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE + 1, SWT.NORMAL));
		return this.recordSetCommentText != null ? this.recordSetCommentText : GDE.STRING_EMPTY;
	}
}