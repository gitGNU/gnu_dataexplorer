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
package osde.device;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Shell;

import osde.ui.OpenSerialDataExplorer;

/**
 * DeviceDialog is the abstract class as parent for device dialog implementations
 * @author Winfried Brügmann
 */
public abstract class DeviceDialog extends Dialog {

	protected Shell	dialogShell;
	
	protected boolean 	isFailedConnectionWarned = false; // if focus adapter opens port this flag eleminates warning loops in case of none modal dialog
	
	protected	int				shellAlpha = 50; //TODO settings
	protected boolean		isAlphaOn = true;//TODO settings
	protected boolean		isInDialog = false; // if dialog alpha fading is used this flag is used to switch off mouseExit and mouseEnter inner events
	
	protected boolean 	isClosePossible = true; // use this variable to manage if dialog can be disposed 
	protected String 		disposeDisabledMessage = "Der Dialog ist aktiv und kann nicht geschlossen werden !";
	
	protected final OpenSerialDataExplorer application;

	/**
	 * constructor for the dialog, in most cases this dialog should not modal  
	 * @param parent
	 */
	public DeviceDialog(Shell parent) {
		super(parent, SWT.NONE);
		this.application = OpenSerialDataExplorer.getInstance();
	}

	/**
	 * default method where the default controls are defined, this needs to be overwritten by specific device dialog
	 */
	abstract public void open();

	/**
	 * default method to dispose (close) a dialog shell
	 * implement all cleanup operation in a disposeListener method
	 */
	public void dispose() {
		if (this.isClosePossible) {
			this.dialogShell.dispose();
			this.application.setStatusMessage("");
		}
		else this.application.setStatusMessage(this.disposeDisabledMessage, SWT.COLOR_RED);
	}

	public void close() {
		this.dispose();
	}

	/**
	 * default method to dispose (close) a dialog shell
	 * implement all cleanup operation in a disposeListener method
	 */
	public boolean isDisposed() {
		return this.dialogShell != null ? this.dialogShell.isDisposed() : true;
	}

	/**
	 * default method to drive visibility of a dialog shell
	 */
	public void setVisible(boolean value) {
		this.dialogShell.setVisible(value);
	}

	/**
	 * default method to set the focus of a dialog shell
	 */
	public boolean setFocus() {
		return this.dialogShell != null ? this.dialogShell.setFocus() : false;
	}

	/**
	 * @return the dialogShell
	 */
	public Shell getDialogShell() {
		return this.dialogShell;
	}

	/**
	 * @return the isClosePossible
	 */
	public boolean isClosePossible() {
		return this.isClosePossible;
	}

	/**
	 * @param enabled the boolean isClosePossible value to set
	 */
	public void setClosePossible(boolean enabled) {
		this.isClosePossible = enabled;
	}

	public int getShellAlpha() {
		return this.shellAlpha;
	}

	public synchronized void setShellAlpha(int newShellAlpha) {
			if (newShellAlpha > this.shellAlpha) {
				//System.out.println("fade-in");
				for (int i = this.shellAlpha; i < 254; i+=5) {
					this.dialogShell.setAlpha(i);
					//System.out.print(i + " ");
				}
				this.dialogShell.setAlpha(254);
				//System.out.println();
			}
			else {
				//System.out.println("fade-out");
				for (int i = 254; i > this.shellAlpha; i-=5) {
					this.dialogShell.setAlpha(i);
					//System.out.print(i + " ");
				}
				//System.out.println();
				this.dialogShell.setAlpha(this.shellAlpha);
			}
	}

	public boolean isAlphaOn() {
		return this.isAlphaOn;
	}

	public void setAlphaOn(boolean enable) {
		this.isAlphaOn = enable;
	}

	/**
	 * @return the isFailedConnectionWarned
	 */
	public boolean isFailedConnectionWarned() {
		return this.isFailedConnectionWarned;
	}

	/**
	 * @param isFailedConnectionWarned the isFailedConnectionWarned to set
	 */
	public void setFailedConnectionWarned(boolean enabled) {
		this.isFailedConnectionWarned = enabled;
	}
}
