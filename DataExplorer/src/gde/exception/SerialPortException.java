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
****************************************************************************************/
package gde.exception;

/**
 * Exception class to be used if the serial port can not be openend due to internal errors, not configuration error
 * @author Winfried Brügmann
 */
public class SerialPortException extends Exception {
	static final long serialVersionUID = 26031957;

	/**
	 * @param message
	 */
	public SerialPortException(String message) {
		super(message);
	}

	/**
	 * @param message
	 * @param cause
	 */
	public SerialPortException(String message, Throwable cause) {
		super(message, cause);
	}

}
