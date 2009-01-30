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
package osde.ui.menu;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

import osde.config.Settings;
import osde.ui.dialog.SettingsDialog;

/**
 * Context menu class of the settings dialog to select a log level
 * @author Winfried Brügmann
 */
public class LogLevelSelectionContextMenu {
	final static Logger	log					= Logger.getLogger(LogLevelSelectionContextMenu.class.getName());
	MenuItem						levelWarning, levelInfo, levelFine, levelFiner, levelFinest;
	String							loggerName	= "";
	
	final Settings						settings;
	final String strWarning = Level.WARNING.getName();
	final String strInfo = Level.INFO.getName();
	final String strFine = Level.FINE.getName();
	final String strFiner = Level.FINER.getName();
	final String strFinest = Level.FINEST.getName();
	
	public LogLevelSelectionContextMenu() {
		this.settings = Settings.getInstance();
	}

	public void createMenu(final Menu popupmenu) {
		popupmenu.addMenuListener(new MenuListener() {
			public void menuShown(MenuEvent evt) {
				log.log(Level.FINEST, "popupmenu MenuListener.menuShown " + evt); //$NON-NLS-1$
				LogLevelSelectionContextMenu.this.loggerName = (String) popupmenu.getData(SettingsDialog.LOGGER_NAME);

				LogLevelSelectionContextMenu.this.levelWarning.setSelection(false);
				LogLevelSelectionContextMenu.this.levelInfo.setSelection(false);
				LogLevelSelectionContextMenu.this.levelFine.setSelection(false);
				LogLevelSelectionContextMenu.this.levelFiner.setSelection(false);
				LogLevelSelectionContextMenu.this.levelFinest.setSelection(false);

				String logLevel = Settings.classbasedLogger.getProperty(LogLevelSelectionContextMenu.this.loggerName);
				if (LogLevelSelectionContextMenu.this.strWarning.equalsIgnoreCase(logLevel)) {
					LogLevelSelectionContextMenu.this.levelWarning.setSelection(true);
				}
				else if (LogLevelSelectionContextMenu.this.strInfo.equalsIgnoreCase(logLevel)) {
					LogLevelSelectionContextMenu.this.levelInfo.setSelection(true);
				}
				else if (LogLevelSelectionContextMenu.this.strFine.equalsIgnoreCase(logLevel)) {
					LogLevelSelectionContextMenu.this.levelFine.setSelection(true);
				}
				else if (LogLevelSelectionContextMenu.this.strFiner.equalsIgnoreCase(logLevel)) {
					LogLevelSelectionContextMenu.this.levelFiner.setSelection(true);
				}
				else if (LogLevelSelectionContextMenu.this.strFinest.equalsIgnoreCase(logLevel)) {
					LogLevelSelectionContextMenu.this.levelFinest.setSelection(true);
				}

			}

			public void menuHidden(MenuEvent evt) {
				log.log(Level.FINEST, "popupmenu MenuListener.menuHidden " + evt); //$NON-NLS-1$
			}
		});

		this.levelWarning = new MenuItem(popupmenu, SWT.CHECK);
		this.levelWarning.setText(this.strWarning);
		this.levelWarning.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				log.log(Level.FINEST, "levle warning selected! " + e); //$NON-NLS-1$
				Settings.classbasedLogger.put(LogLevelSelectionContextMenu.this.loggerName, LogLevelSelectionContextMenu.this.strWarning);
			}
		});

		this.levelInfo = new MenuItem(popupmenu, SWT.CHECK);
		this.levelInfo.setText(this.strInfo);
		this.levelInfo.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				log.log(Level.FINEST, "levle info selected! " + e); //$NON-NLS-1$
				Settings.classbasedLogger.put(LogLevelSelectionContextMenu.this.loggerName, LogLevelSelectionContextMenu.this.strInfo);
			}
		});

		this.levelFine = new MenuItem(popupmenu, SWT.CHECK);
		this.levelFine.setText(this.strFine);
		this.levelFine.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				log.log(Level.FINEST, "levle fine selected! " + e); //$NON-NLS-1$
				Settings.classbasedLogger.put(LogLevelSelectionContextMenu.this.loggerName, LogLevelSelectionContextMenu.this.strFine);
			}
		});

		this.levelFiner = new MenuItem(popupmenu, SWT.CHECK);
		this.levelFiner.setText(this.strFiner);
		this.levelFiner.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				log.log(Level.FINEST, "levle finer selected! " + e); //$NON-NLS-1$
				Settings.classbasedLogger.put(LogLevelSelectionContextMenu.this.loggerName, LogLevelSelectionContextMenu.this.strFiner);
			}
		});

		this.levelFinest = new MenuItem(popupmenu, SWT.CHECK);
		this.levelFinest.setText(this.strFinest);
		this.levelFinest.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				log.log(Level.FINEST, "levle finest selected! " + e); //$NON-NLS-1$
				Settings.classbasedLogger.put(LogLevelSelectionContextMenu.this.loggerName, LogLevelSelectionContextMenu.this.strFinest);
			}
		});

	}
}
