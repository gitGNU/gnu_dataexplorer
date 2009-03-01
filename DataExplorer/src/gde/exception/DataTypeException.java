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
package osde.exception;

/**
 * Exception class to be used in any case of data type missmatch
 * @author Winfried Brügmann
 */
public class DataTypeException extends Exception { 
	static final long serialVersionUID = 26031957;

	/**
	 * @param message
	 */
	public DataTypeException(String message) {
		super(message);
	}

	/**
	 * @param message
	 * @param cause
	 */
	public DataTypeException(String message, Throwable cause) {
		super(message, cause);
	}

}
