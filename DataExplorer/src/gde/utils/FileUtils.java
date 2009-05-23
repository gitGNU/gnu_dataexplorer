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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;

import osde.OSDE;
import osde.exception.ApplicationConfigurationException;
import osde.messages.MessageIds;
import osde.messages.Messages;
import osde.ui.OpenSerialDataExplorer;

/**
 * Utility class with helpers around file and directory handling
 * @author Winfried Brügmann
 */
public class FileUtils {
	private static final Logger	log			= Logger.getLogger(FileUtils.class.getName());

	public static void copyFile(File in, File out) throws IOException {
		FileChannel inChannel = new FileInputStream(in).getChannel();
		FileChannel outChannel = new FileOutputStream(out).getChannel();
		try {
			inChannel.transferTo(0, inChannel.size(), outChannel);
		}
		catch (IOException e) {
			throw e;
		}
		finally {
			if (inChannel != null) inChannel.close();
			if (outChannel != null) outChannel.close();
		}
	}

	public static void copyAllFiles(String srcDir, String tgtDir) throws IOException, ApplicationConfigurationException {
		File sourceDir = new File(srcDir);
		String[] files = sourceDir.list();
		if (files == null) {
			throw new ApplicationConfigurationException(Messages.getString(MessageIds.OSDE_MSGE0017, new Object[] { srcDir }));
		}
		for (String srcFile : files) {
			if (OSDE.IS_WINDOWS) {
				srcFile = srcFile.replace(OSDE.STRING_URL_BLANK, OSDE.STRING_BLANK); //$NON-NLS-1$ //$NON-NLS-2$
			}
			File src = new File(srcDir + OSDE.FILE_SEPARATOR_UNIX + srcFile);
			if (!src.isDirectory()) {
				File tgt = new File(tgtDir + OSDE.FILE_SEPARATOR_UNIX + srcFile);
				log.log(Level.FINE, "copy " + src.toString() + " to " + tgt.toString()); //$NON-NLS-1$ //$NON-NLS-2$
				FileUtils.copyFile(src, tgt);
			}
		}
	}

	/**
	 * check existens of a directory and create if not exist
	 * @param directory
	 * @return true false if directory needs to be created
	 */
	public static boolean checkDirectoryAndCreate(String directory) {
		boolean exist = true;
		File dir = new File(directory);
		if (!dir.exists() && !dir.isDirectory()) {
			exist = false;
			dir.mkdirs();
		}
		return exist;
	}

	/**
	 * check existent of a directory and file version and create if not exist, backup if version does not match
	 * @param directory
	 * @param versionFileName string qualifier "_V01" checks for file *_V01.* 
	 * @return true false if directory needs to be created
	 */
	public static boolean checkDirectoryAndCreate(String directory, String versionFileName) {
		boolean exist = true;
		File dir = new File(directory);
		if (!dir.exists() && !dir.isDirectory()) {
			exist = false;
			dir.mkdir();
		}
		else {
			File file = new File(directory + OSDE.FILE_SEPARATOR_UNIX + versionFileName);
			if (!file.exists()) {
					exist = false;
					String oldVersion = String.format("%02d", new Integer(versionFileName.substring(versionFileName.length()-6, versionFileName.length()-4)) - 1); //$NON-NLS-1$
					String oldVersionStr = versionFileName.substring(versionFileName.length()-8, versionFileName.length()-6) + oldVersion;
					dir.renameTo(new File(directory + oldVersionStr));
					log.log(Level.FINE, "found old version " + oldVersionStr + " and created a backup directory"); //$NON-NLS-1$ //$NON-NLS-2$
					File newDir = new File(directory);
					newDir.mkdir();
			}
		}
		return exist;
	}

	/**
	 * check if a file exist, the file path given must fully qualified
	 * @param fullQualifiedFileName
	 * @return true if file exist
	 */
	public static boolean checkFileExist(final String fullQualifiedFileName) {
		File file = new File(fullQualifiedFileName);
		return file.exists();
	}
	
	/**
	 * rename a file to given file extension
	 */
	public static void renameFile(String filePath, String extension) {
		if (checkFileExist(filePath)) {
			File file = new File(filePath);
			file.renameTo(new File(filePath.substring(0, filePath.lastIndexOf(".")+1) + extension)); //$NON-NLS-1$
		}
	}

	/**
	 * extract a file from source jar file to target file while replace a given placeholder key with a replacement
	 * supported Charset encoding :
	 * US-ASCII 	Seven-bit ASCII, a.k.a. ISO646-US, a.k.a. the Basic Latin block of the Unicode character set
	 * ISO-8859-1   	ISO Latin Alphabet No. 1, a.k.a. ISO-LATIN-1
	 * UTF-8 	Eight-bit UCS Transformation Format
	 * UTF-16BE 	Sixteen-bit UCS Transformation Format, big-endian byte order
	 * UTF-16LE 	Sixteen-bit UCS Transformation Format, little-endian byte order
	 * UTF-16 	Sixteen-bit UCS Transformation Format, byte order identified by an optional byte-order mark
	 * @param placeholderKey
	 * @param replacement
	 * @param jarFilePath
	 * @param jarInternalFilePath
	 * @param targetFilePath
	 * @param sourceEncoding "UTF-8", "ISO-8859-1"
	 * @param targetEncoding "UTF-8", "ISO-8859-1"
	 * @return
	 * @throws IOException
	 * @throws UnsupportedEncodingException
	 * @throws FileNotFoundException
	 */
	public static void extractWhileReplace(String placeholderKey, String replacement, String jarFilePath, String jarInternalFilePath, String targetFilePath, String sourceEncoding, String targetEncoding) throws IOException, UnsupportedEncodingException,
			FileNotFoundException {
		BufferedReader reader;
		BufferedWriter writer;
		String line;
		log.log(Level.INFO, "jarFilePath = " + jarFilePath); //$NON-NLS-1$
		JarFile jarFile = new JarFile(jarFilePath);

		reader = new BufferedReader(new InputStreamReader(FileUtils.getFileInputStream(jarFile, jarInternalFilePath), sourceEncoding)); 
		log.log(Level.INFO, "targetPath = " + targetFilePath); //$NON-NLS-1$
		writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(targetFilePath), targetEncoding)); //$NON-NLS-1$

		while ((line = reader.readLine()) != null) {
			log.log(Level.INFO, line);
			if (line.indexOf(placeholderKey) > -1) {
				StringBuilder sb = new StringBuilder();
				sb.append(line.substring(0, line.indexOf(placeholderKey)));
				sb.append(replacement);
				sb.append(line.substring(line.indexOf(placeholderKey) + placeholderKey.length()));
				line = sb.toString();
			}
			log.log(Level.INFO, line);
			writer.write(line+OSDE.LINE_SEPARATOR);
		}
		reader.close();
		writer.flush();
		writer.close();
	}

	/**
	 * copy a file from source to target while replace a given placeholder key with a replacement
	 * @param placeHolderKey
	 * @param replacement
	 * @param sourceFilePath
	 * @param targetFilePath
	 * @throws UnsupportedEncodingException
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public static void copyFileWhileReplaceKey(String placeHolderKey, String replacement, String sourceFilePath, String targetFilePath) throws UnsupportedEncodingException,
			FileNotFoundException, IOException {
		BufferedReader reader;
		BufferedWriter writer;
		String line;
		reader = new BufferedReader(new InputStreamReader(new FileInputStream(sourceFilePath), OSDE.STRING_UTF_8)); 
		writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(targetFilePath), OSDE.STRING_UTF_8)); 
		while ((line = reader.readLine()) != null) {
			log.log(Level.INFO, line);
			if (line.indexOf(placeHolderKey) > -1) {
				StringBuilder sb = new StringBuilder();
					sb.append(line.substring(0, line.indexOf(placeHolderKey)));
					sb.append(replacement);
					sb.append(line.substring(line.indexOf(placeHolderKey)+placeHolderKey.length()));
					line = sb.toString();
			}
			
			line = line.replace(OSDE.FILE_SEPARATOR_WINDOWS, OSDE.FILE_SEPARATOR_UNIX).replace(OSDE.STRING_URL_BLANK, OSDE.STRING_BLANK)  + OSDE.LINE_SEPARATOR;
			
			log.log(Level.INFO, line);
			writer.write(line);
		}
		reader.close();
		writer.flush();
		writer.close();
	}

	/**
	 * extract a jar internal file using the runtime class instance
	 * @param runtimeInstance
	 * @param fileName
	 * @param jarInternalSourceDirectory
	 * @param targetDirectory
	 * @param permissionsUNIX
	 */
	public static void extract(Class<?> runtimeInstance, String fileName, String jarInternalSourceDirectory, String targetDirectory, String permissionsUNIX) {
		// normalize input directorys
		jarInternalSourceDirectory = jarInternalSourceDirectory.replace(OSDE.FILE_SEPARATOR_WINDOWS, OSDE.FILE_SEPARATOR_UNIX);
		jarInternalSourceDirectory = jarInternalSourceDirectory.endsWith(OSDE.FILE_SEPARATOR_UNIX) ? jarInternalSourceDirectory : jarInternalSourceDirectory + OSDE.FILE_SEPARATOR_UNIX;
		targetDirectory = targetDirectory.replace(OSDE.FILE_SEPARATOR_WINDOWS, OSDE.FILE_SEPARATOR_UNIX);
		targetDirectory = targetDirectory.endsWith(OSDE.FILE_SEPARATOR_UNIX) ? targetDirectory : targetDirectory + OSDE.FILE_SEPARATOR_UNIX; 

		FileOutputStream os = null;
		InputStream is = null;
		File file = new File(targetDirectory + fileName); 
		try {
			if (!file.exists ()) {
				is = runtimeInstance.getClassLoader().getResourceAsStream (jarInternalSourceDirectory + fileName); 
				if (is != null) {
					int read;
					byte [] buffer = new byte [4096];
					os = new FileOutputStream (targetDirectory + fileName);
					while ((read = is.read (buffer)) != -1) {
						os.write(buffer, 0, read);
					}
					os.close ();
					is.close ();
					setAccessPermission(fileName, permissionsUNIX);
				}
			}
		} 
		catch (Throwable e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			try {
				if (os != null) os.close ();
			} catch (IOException e1) {}
			try {
				if (is != null) is.close ();
			} catch (IOException e1) {}
		}
	}

	/**
	 * get the input stream of a file from a given jar archive
	 * @param jarFile
	 * @param jarInternalFilePath
	 * @throws IOException 
	 */
	public static InputStream getFileInputStream(JarFile jarFile, String jarInternalFilePath) throws IOException {
		// normalize input file path
		jarInternalFilePath = jarInternalFilePath.replace(OSDE.FILE_SEPARATOR_WINDOWS, OSDE.FILE_SEPARATOR_UNIX);
		
		ZipEntry ze = jarFile.getEntry(jarInternalFilePath);

		InputStream is = jarFile.getInputStream(ze);
		
		return is;
	}
			
	/**
	 * extract a file from a Jar archive
	 * @param jarFile
	 * @param fileName
	 * @param jarSourceDirectory
	 * @param targetDirectory
	 * @param unixPermissions
	 * @throws IOException
	 */
	public static void extract(JarFile jarFile, String fileName, String jarSourceDirectory, String targetDirectory, String unixPermissions) {
		// normalize input directorys
		fileName = fileName.replace(OSDE.FILE_SEPARATOR_WINDOWS, OSDE.FILE_SEPARATOR_UNIX);
		fileName = fileName.startsWith(OSDE.FILE_SEPARATOR_UNIX) ? fileName.substring(1) : fileName;
		jarSourceDirectory = jarSourceDirectory.replace(OSDE.FILE_SEPARATOR_WINDOWS, OSDE.FILE_SEPARATOR_UNIX);
		jarSourceDirectory = jarSourceDirectory.endsWith(OSDE.FILE_SEPARATOR_UNIX) ? jarSourceDirectory : jarSourceDirectory.length() > 1 ? jarSourceDirectory + OSDE.FILE_SEPARATOR_UNIX : jarSourceDirectory;
		targetDirectory = targetDirectory.replace(OSDE.FILE_SEPARATOR_WINDOWS, OSDE.FILE_SEPARATOR_UNIX);
		targetDirectory = targetDirectory.endsWith(OSDE.FILE_SEPARATOR_UNIX) ? targetDirectory : targetDirectory.length() > 1 ? targetDirectory + OSDE.FILE_SEPARATOR_UNIX : targetDirectory;
		
		ZipEntry ze = jarFile.getEntry(jarSourceDirectory + fileName);

		int read;
		byte[] buffer = new byte[4096];
		InputStream is = null;
		FileOutputStream os = null;
		try {
			is = jarFile.getInputStream(ze);
			os = new FileOutputStream(targetDirectory + fileName);
			while ((read = is.read(buffer)) != -1) {
				os.write(buffer, 0, read);
			}
			os.close();
			is.close();
			setAccessPermission(fileName, unixPermissions);
		}
		catch (Throwable e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			try {
				if (os != null) os.close();
			}
			catch (IOException e1) {
			}
			try {
				if (is != null) is.close();
			}
			catch (IOException e1) {
			}
		}
	}

	/**
	 * extract directory from a Jar archive
	 * @param jarFile
	 * @param sourceFileName
	 * @param targetDirectory
	 */
	public static void extractDir(JarFile jarFile, String jarInternalSourceDirectory, String targetDirectory, String permissionsUNIX) {
		// normalize input directorys
		jarInternalSourceDirectory = jarInternalSourceDirectory.replace(OSDE.FILE_SEPARATOR_WINDOWS, OSDE.FILE_SEPARATOR_UNIX);
		jarInternalSourceDirectory = jarInternalSourceDirectory.endsWith(OSDE.FILE_SEPARATOR_UNIX) ? jarInternalSourceDirectory : jarInternalSourceDirectory + OSDE.FILE_SEPARATOR_UNIX;
		targetDirectory = targetDirectory.replace(OSDE.FILE_SEPARATOR_WINDOWS, OSDE.FILE_SEPARATOR_UNIX);
		targetDirectory = targetDirectory.endsWith(OSDE.FILE_SEPARATOR_UNIX) ? targetDirectory : targetDirectory + OSDE.FILE_SEPARATOR_UNIX;
		
		int read;
		byte[] buffer = new byte[4096];
		InputStream is = null;
		FileOutputStream os = null;
		
		FileUtils.checkDirectoryAndCreate(targetDirectory + jarInternalSourceDirectory);

		Enumeration<JarEntry> enties = jarFile.entries();
		while (enties.hasMoreElements()) {
			JarEntry jarEntry = enties.nextElement();
			String entryName = jarEntry.getName();
			if ((entryName.startsWith(jarInternalSourceDirectory) || entryName.endsWith(".css")) && entryName.contains(".") && !FileUtils.checkFileExist(targetDirectory + entryName)) { //$NON-NLS-1$ //$NON-NLS-2$
				ZipEntry ze = jarFile.getEntry(entryName);
				FileUtils.checkDirectoryAndCreate(targetDirectory + entryName.substring(0, entryName.lastIndexOf(OSDE.FILE_SEPARATOR_UNIX)));
				try {
					is = jarFile.getInputStream(ze);
					os = new FileOutputStream(targetDirectory + entryName);
					while ((read = is.read(buffer)) != -1) {
						os.write(buffer, 0, read);
					}
					os.close();
					is.close();
					setAccessPermission(entryName, permissionsUNIX);
				}
				catch (Throwable e) {
					log.log(Level.SEVERE, e.getMessage(), e);
					try {
						if (os != null) os.close();
					}
					catch (IOException e1) {
					}
					try {
						if (is != null) is.close();
					}
					catch (IOException e1) {
					}
				}
			}
		}
	}

	/**
	 * @param fullQualifiedFilePath
	 * @param unixPermissions
	 */
	private static void setAccessPermission(String fullQualifiedFilePath, String unixPermissions) {
		if (!OSDE.IS_WINDOWS) { //$NON-NLS-1$ //$NON-NLS-2$
			try {
				unixPermissions = unixPermissions.trim();
				try {
					new Integer(unixPermissions);
				}
				catch(NumberFormatException e) {
					log.log(Level.SEVERE, "Internal Error - permission not usable (" + unixPermissions +")"); //$NON-NLS-1$ //$NON-NLS-2$
				}
				//log.log(Level.FINE, "chmod 755 " + fileName);
				Runtime.getRuntime ().exec (new String []{"chmod", unixPermissions, fullQualifiedFilePath}).waitFor(); //$NON-NLS-1$ //$NON-NLS-2$
			} catch (Throwable e) {}
		}
	}

	/**
	 * find out the base path where the OSDE Jars are located  
	 * @return operating depending path where the device plug-in jar are located
	 */
	public static String getOsdeJarBasePath() {
		String	osname = System.getProperty("os.name", "").toLowerCase();	//$NON-NLS-1$ //$NON-NLS-2$
		String basePath;
		URL url = OpenSerialDataExplorer.class.getProtectionDomain().getCodeSource().getLocation();
		log.log(Level.FINE, "base URL = " + url.toExternalForm()); //$NON-NLS-1$
		if (url.getPath().endsWith("/")) { // running inside Eclipse //$NON-NLS-1$
			log.log(Level.FINE, "started inside Eclipse"); //$NON-NLS-1$
			String bitmode = System.getProperty("sun.arch.data.model"); //$NON-NLS-1$
			bitmode = bitmode != null && bitmode.length() == 2 ? bitmode : System.getProperty("com.ibm.vm.bitmode"); //$NON-NLS-1$
			basePath = url.getFile().substring(OSDE.IS_WINDOWS ? 1 : 0, url.getPath().indexOf(OpenSerialDataExplorer.class.getSimpleName()));
			basePath = basePath + "build" + "/target/" + System.getProperty("os.name").split(OSDE.STRING_BLANK)[0] + OSDE.STRING_UNDER_BAR + bitmode + "/OpenSerialDataExplorer"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
		}
		else { // started outside java -jar *.jar
			log.log(Level.FINE, "started outside with: java -jar *.jar"); //$NON-NLS-1$
			basePath = url.getFile().substring(0, url.getPath().lastIndexOf("/") + 1); //$NON-NLS-1$
			osname = System.getProperty("os.name").toLowerCase(); //$NON-NLS-1$
			if (osname.startsWith("windows")) { //$NON-NLS-1$
				basePath = basePath.replace(OSDE.STRING_URL_BLANK, OSDE.STRING_BLANK);  //$NON-NLS-1$//$NON-NLS-2$
			}
		}
		log.log(Level.FINE, "OSDE base path = " + basePath); //$NON-NLS-1$
		return basePath;
	}

	/**
	 * find out the base path where the device plug-in jar are located 
	 * @return operating depending path where the device plug-in jar are located
	 */
	public static String getDevicePluginJarBasePath() {
		String basePath;
		String jarPath = null;
		URL url = OpenSerialDataExplorer.class.getProtectionDomain().getCodeSource().getLocation();
		log.log(Level.FINE, "base URL = " + url.toExternalForm()); //$NON-NLS-1$
		if (url.getPath().endsWith("/")) { // running inside Eclipse //$NON-NLS-1$
			log.log(Level.FINE, "started inside Eclipse"); //$NON-NLS-1$
			basePath = url.getFile().substring(0, url.getPath().indexOf(OpenSerialDataExplorer.class.getSimpleName()));
			log.log(Level.FINE, "basePath = " + basePath); //$NON-NLS-1$
			try {
				//jarPath = basePath + "build" + OSDE.FILE_SEPARATOR_UNIX + "target" + OSDE.FILE_SEPARATOR_UNIX + Settings.DEVICE_PROPERTIES_DIR_NAME; //$NON-NLS-1$ //$NON-NLS-2$
				//targetDirectory this.applHomePath + OSDE.FILE_SEPARATOR_UNIX + Settings.DEVICE_PROPERTIES_DIR_NAME);
				jarPath = basePath + "build" + "/target/" + System.getProperty("os.name").split(OSDE.STRING_BLANK)[0] + OSDE.STRING_UNDER_BAR + OSDE.BIT_MODE + "/OpenSerialDataExplorer/devices"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}
			catch (Exception e) {
				e.printStackTrace(System.err);
			}
		}
		else { // started outside java -jar *.jar
			log.log(Level.FINE, "started outside with: java -jar *.jar"); //$NON-NLS-1$
			basePath = url.getFile().substring(0, url.getPath().lastIndexOf("/") + 1); //$NON-NLS-1$
			if (OSDE.IS_WINDOWS) { //$NON-NLS-1$
				basePath = basePath.replace(OSDE.STRING_URL_BLANK, OSDE.STRING_BLANK);
			}
			log.log(Level.FINE, "basePath = " + basePath); //$NON-NLS-1$
			try {
				//jarPath = basePath + Settings.DEVICE_PROPERTIES_DIR_NAME;
				//targetDirectory this.applHomePath + OSDE.FILE_SEPARATOR_UNIX + Settings.DEVICE_PROPERTIES_DIR_NAME);
				jarPath = basePath + "devices"; //$NON-NLS-1$
			}
			catch (Exception e) {
				e.printStackTrace(System.err);
			}
		}
		log.log(Level.FINE, "device plug-ins path = " + jarPath); //$NON-NLS-1$
		return jarPath;
	}

	/**
	 * get the names of the exported device services
	 * @param jarFileDir 
	 * @param jarFileName
	 * @return service names
	 * @throws IOException
	 */
	public static String[] getDeviceJarServicesNames(JarFile jarFile) throws IOException {
		Vector<String> pluginNamesVector = new Vector<String>();
		Manifest m = jarFile.getManifest();
		String services = m.getMainAttributes().getValue("Export-Service"); //$NON-NLS-1$
		log.log(Level.FINE, "Export-Service = " + services); //$NON-NLS-1$
		String[] seriveNames = services.split(", *"); //$NON-NLS-1$
		for (String name : seriveNames) {
			name = name.substring(name.lastIndexOf('.') + 1);
			log.log(Level.FINE, "service name = " + name); //$NON-NLS-1$
			pluginNamesVector.add(name);
		}
		return pluginNamesVector.toArray(new String[0]);
	}

	/**
	 * check the java executable version 
	 * @param javaFullQualifiedExecutablePath /usr/lib/jvm/java-sun/java
	 * @param expectedVersionString 1.6
	 * @return true if the expected version is lower or equal the version found by executing the given java executable
	 */
	public static boolean checkJavaExecutableVersion(String javaFullQualifiedExecutablePath, String expectedVersionString) {
		final String javaVersion = "java version"; //$NON-NLS-1$
		int actualVersion = 0;
		try {
			String line;
			if (javaFullQualifiedExecutablePath.indexOf("%WINDIR%") > -1) { //$NON-NLS-1$
				javaFullQualifiedExecutablePath = System.getenv("WINDIR")  //$NON-NLS-1$
					+ javaFullQualifiedExecutablePath.substring(javaFullQualifiedExecutablePath.indexOf("%WINDIR%") + "%WINDIR%".length()); //$NON-NLS-1$ //$NON-NLS-2$
			}
			if (javaFullQualifiedExecutablePath.indexOf("javaw") > -1) { //$NON-NLS-1$
				javaFullQualifiedExecutablePath = javaFullQualifiedExecutablePath.substring(0, javaFullQualifiedExecutablePath.indexOf("javaw")) //$NON-NLS-1$
					+ "java" + javaFullQualifiedExecutablePath.substring(javaFullQualifiedExecutablePath.indexOf("javaw")+ "javaw".length()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}
			Process process = new ProcessBuilder(javaFullQualifiedExecutablePath, "-version").start(); //$NON-NLS-1$
			InputStream is = process.getErrorStream();
			InputStreamReader isr = new InputStreamReader(is);
			BufferedReader br = new BufferedReader(isr);
			
			while ((line = br.readLine()) != null) { // clean std err
				if (line.startsWith(javaVersion)) actualVersion = parseJavaVersion(line.substring(javaVersion.length()+2));
			}
			
			is = process.getInputStream();
			isr = new InputStreamReader(is);
			br = new BufferedReader(isr);
			while ((line = br.readLine()) != null) {} // clean std out
		
			process.waitFor(); // waits until termination
			
			//if (process.exitValue() == 0) {
			//	System.out.println("success");
			//}
			//else
			//	System.out.println("no success");
		}
		catch (Throwable e) {
			log.log(Level.SEVERE, e.getMessage(), e);
		}
		
		log.log(Level.INFO, parseJavaVersion(expectedVersionString) + " <= " + actualVersion); //$NON-NLS-1$
		return (parseJavaVersion(expectedVersionString) <= actualVersion);
	}

	/**
	 * parse the java version 1.6.0** will result in 160
	 * @param version 1.6.0*
	 * @return 1.6.0** will result in 160
	 */
	static int parseJavaVersion(String version) {
		if (version == null) return 0;
		int major = 0, minor = 0, micro = 0, index = 0, start = 0;
		int versionStringLength = version.length();
		
		while (index < versionStringLength && Character.isDigit(version.charAt(index))) index++;
		try {
			if (start < versionStringLength) major = Integer.parseInt(version.substring(start, index));
		} catch (NumberFormatException e) {}
		start = ++index;
		while (index < versionStringLength && Character.isDigit(version.charAt(index))) index++;
		try {
			if (start < versionStringLength) minor = Integer.parseInt(version.substring(start, index));
		} catch (NumberFormatException e) {}
		start = ++index;
		while (index < versionStringLength && Character.isDigit(version.charAt(index))) index++;
		try {
			if (start < versionStringLength) micro = Integer.parseInt(version.substring(start, index));
		} catch (NumberFormatException e) {}
		
		return major*100 + minor*10 + micro;
	}

	/**
	  * Recursively walk a directory tree and return a List of all files found.
	  * @param rootDirectory is a valid directory
	  * @return List<File> sorted using File.compareTo()
	  * @throws FileNotFoundException
	  */
	public static List<File> getFileListing(File rootDirectory) throws FileNotFoundException {
		validateDirectory(rootDirectory);
		List<File> result = getFileListingNoSort(rootDirectory);
		Collections.sort(result);
		return result;
	}

	/**
	* Recursively walk a directory tree and return a List of all Files found;
	 * @param rootDirectory
	* @return List<File>
	 * @throws FileNotFoundException
	 */
	private static List<File> getFileListingNoSort(File rootDirectory) throws FileNotFoundException {
		List<File> result = new ArrayList<File>();
		File[] filesAndDirs = rootDirectory.listFiles();
		List<File> filesDirs = Arrays.asList(filesAndDirs);
		for (File file : filesDirs) {
			result.add(file);
			if (!file.isFile()) {
				//recursive walk by calling itself
				List<File> deeperList = getFileListingNoSort(file);
				result.addAll(deeperList);
			}
		}
		return result;
	}

	/**
	 * Directory is valid if it exists, does not represent a file, and can be read.
	 * @throws FileNotFoundException
	 * @throws IllegalArgumentException
	 */
	public static void validateDirectory(File directory) throws FileNotFoundException, IllegalArgumentException {
		if (directory == null) {
			throw new IllegalArgumentException("Directory should not be null."); //$NON-NLS-1$
		}
		if (!directory.exists()) {
			throw new FileNotFoundException("Directory does not exist: " + directory); //$NON-NLS-1$
		}
		if (!directory.isDirectory()) {
			throw new IllegalArgumentException("Is not a directory: " + directory); //$NON-NLS-1$
		}
		if (!directory.canRead()) {
			throw new IllegalArgumentException("Directory cannot be read: " + directory); //$NON-NLS-1$
		}
	}
}
