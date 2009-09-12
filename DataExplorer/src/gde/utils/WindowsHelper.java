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

import java.util.Date;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import osde.messages.MessageIds;
import osde.messages.Messages;
import osde.serial.DeviceSerialPort;
import osde.ui.OpenSerialDataExplorer;




/**
 * @author Winfried Brügmann
 * helper class to create a windows shell link using JNI to access native windows OLE functions in WinHelper library 
 */
public class WindowsHelper {
	private final static Logger	log	= Logger.getLogger(WindowsHelper.class.getName());
	
	static {
		try {
			//using internal load functionality where the library may packed within the jar
			String prop = System.getProperty("sun.arch.data.model"); //$NON-NLS-1$
			if (prop == null) prop = System.getProperty("com.ibm.vm.bitmode"); //$NON-NLS-1$

			log.log(Level.INFO, "bitmode = " + prop); //$NON-NLS-1$
			Library.loadLibrary( "WinHelper" + prop ); //$NON-NLS-1$
		}
		catch (Throwable t) {
			log.log(Level.SEVERE, t.getMessage(), t);
			if (t instanceof UnsatisfiedLinkError)
				OpenSerialDataExplorer.getInstance().openMessageDialog(Messages.getString(MessageIds.OSDE_MSGE0037, new Object[] {t.getClass().getSimpleName()}) 
						+ Messages.getString(MessageIds.OSDE_MSGI0033));
			else
				OpenSerialDataExplorer.getInstance().openMessageDialog(Messages.getString(MessageIds.OSDE_MSGE0038, new Object[] {t.getLocalizedMessage()}));
		}
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {

//		String [] shellLinkArgs = {
//				"C:\\Documents and Settings\\brueg\\Desktop\\OpenSerialDataExplorer.lnk", //%USERPROFILE% //$NON-NLS-1$
//				"%WINDIR%\\system32\\javaw.exe", //$NON-NLS-1$
//				"-jar -Xms40M -Xmx256M \"C:\\Program Files\\OpenSerialDataExplorer\\OpenserialDataExplorer.jar\"", //$NON-NLS-1$
//				"C:\\Program Files\\OpenSerialDataExplorer", //$NON-NLS-1$
//				"C:\\Program Files\\OpenSerialDataExplorer\\OpenSerialDataExplorer.ico",  //$NON-NLS-1$
//				"OpenSerialData Explorer" }; //$NON-NLS-1$
//		
//		createDesktopLink(shellLinkArgs[0], shellLinkArgs[1], shellLinkArgs[2], shellLinkArgs[3], shellLinkArgs[4], 0, shellLinkArgs[5]);
		registerSerialPorts();
	}

	public static void registerSerialPorts() {
		long startTime = new Date().getTime();
		String[] enumPorts = enumerateSerialPorts();
		if (enumPorts[0].startsWith("OSDE_MSG")) {			
			log.log(Level.WARNING, Messages.getString(MessageIds.OSDE_MSGW0035, new Object[] {enumPorts[0].split(";")[1]}));
			return;
		}
		
		TreeMap<Integer, String> winPorts = DeviceSerialPort.getWindowsPorts();
		winPorts.clear();
		for (String portString : enumPorts) {
			if (portString != null && portString.length() > 1 && !portString.toLowerCase().contains("bluetooth")) {
					try {
						int portNumber = Integer.parseInt(portString.substring(portString.indexOf("COM")+3, portString.indexOf(')')));
						String[] tmpDesc = portString.split(";");
						String portDescription = tmpDesc[1].substring(0, tmpDesc[1].indexOf("COM")-2);
						String manufacturer = tmpDesc[0].split(" ")[0];
						if (manufacturer.length() > 1) {
							if (!portDescription.contains(manufacturer)) {
								portDescription = manufacturer + " " + portDescription;
							}
						}
						log.log(Level.FINE, "COM" + portNumber + " - " +portDescription);
						winPorts.put(portNumber, portDescription);
					}
					catch (NumberFormatException e) {
						log.log(Level.FINER, portString);
					}
			}
		}
		
		if (log.isLoggable(Level.FINER)) {
			for (int number : winPorts.keySet()) {
				log.log(Level.FINE, "COM" + number + " - " + winPorts.get(number));
			}
		}
		
		if (winPorts.size() > 0) {
			StringBuilder sb = new StringBuilder();
			for (int number : winPorts.keySet()) {
				sb.append("COM").append(number).append(";");
			}
			log.log(Level.FINE, "Windows port list = " + sb.toString());
			System.setProperty("gnu.io.rxtx.SerialPorts", sb.toString());
		}
		else {
			System.setProperty("gnu.io.rxtx.SerialPorts", "");
			//System.clearProperty("gnu.io.rxtx.SerialPorts");
		}
		log.log(Level.FINE, "enum Windows ports  takes = " + StringHelper.getFormatedTime("ss:SSS", (new Date().getTime() - startTime)));
	}

	/**
	 * native method called via load library to enable use of native windows ole32 functions
	 * @param fqShellLinkPath
	 * @param fqExecutablePath
	 * @param executableArguments
	 * @param workingDirectory
	 * @param fqIconPath
	 * @param iconPosition
	 * @param description
	 */
	public static native String createDesktopLink(
			String fqShellLinkPath,
			String fqExecutablePath,
			String executableArguments,
			String workingDirectory, 
			String fqIconPath, 
			int 	 iconPosition,
			String description
		);

	/**
	 * native method called via load library to enable use of native windows ole32 functions
	 * @param fqShellLinkPath
	 * @return contained full qualified file path
	 */
	public static native String getFilePathFromLink(String fqShellLinkPath);

	/**
	 * native method called via load library to enable use of native windows functions to register OpenSerialData MIME type to associate .osd
	 * @param applicationInstallationDirectory
	 */
	//public static native String registerMimeType(	String applicationInstallationDirectory	) throws SecurityException, IOException;

	/**
	 * native method called via load library to enable use of native windows functions to remove file type association
	 */
	//public static native String deregisterMimeType() throws SecurityException, IOException;

	/**
	 * native method called via load library to enable use of native windows ole32 functions
	 * @return enum list of system serial ports
	 */
	public static native String[] enumerateSerialPorts();
}
