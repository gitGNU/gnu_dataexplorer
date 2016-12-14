//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.1.5-b01-fcs 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2016.12.10 at 08:50:32 AM MEZ 
//
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
    
    Copyright (c) 2016 Thomas Eickert
****************************************************************************************/

package gde.histocache;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import javax.xml.validation.SchemaFactory;

import gde.GDE;
import gde.config.Settings;
import gde.data.HistoRecordSet;
import gde.device.IDevice;
import gde.exception.DataInconsitsentException;
import gde.exception.NotSupportedFileFormatException;
import gde.io.HistoOsdReaderWriter;
import gde.log.Level;
import gde.ui.DataExplorer;
import gde.utils.StringHelper;

/**
 * aggregated history recordset data related to
 * 				measurements, settlements and scores
 * 			
 * 
 * <p>Java class for histoVault complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="histoVault">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="vaultName" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="vaultDirectory" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="vaultCreated_ms" type="{http://www.w3.org/2001/XMLSchema}long"/>
 *         &lt;element name="vaultDataExplorerVersion" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="vaultDeviceKey" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="vaultDeviceName" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="vaultChannelNumber" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *         &lt;element name="vaultObjectKey" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="vaultSamplingTimespan_ms" type="{http://www.w3.org/2001/XMLSchema}long"/>
 *         &lt;element name="logFilePath" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="logFileLastModified" type="{http://www.w3.org/2001/XMLSchema}long"/>
 *         &lt;element name="logObjectDirectory" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="logFileVersion" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *         &lt;element name="logRecordSetSize" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *         &lt;element name="logRecordSetOrdinal" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *         &lt;element name="logRecordsetBaseName" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="logDeviceName" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="logChannelNumber" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *         &lt;element name="logObjectKey" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="logStartTimestamp_ms" type="{http://www.w3.org/2001/XMLSchema}long"/>
 *         &lt;element name="measurements" type="{}entries"/>
 *         &lt;element name="settlements" type="{}entries"/>
 *         &lt;element name="scores" type="{}entryPoints"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
/**
 * suitable for history persistence and xml serialization.
 * find the constructors and non-xsd code a good way down for simplified merging with JAXB generated class.  
 * @author Thomas Eickert
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "histoVault", propOrder = { "vaultName", "vaultDirectory", "vaultCreatedMs", "vaultDataExplorerVersion", "vaultDeviceKey", "vaultDeviceName", "vaultChannelNumber", "vaultObjectKey",
		"vaultSamplingTimespanMs", "logFilePath", "logFileLastModified", "logObjectDirectory", "logFileVersion", "logRecordSetSize", "logRecordSetOrdinal", "logRecordsetBaseName", "logDeviceName",
		"logChannelNumber", "logObjectKey", "logStartTimestampMs", "measurements", "settlements", "scores" })
public class HistoVault {
	final private static String	$CLASS_NAME	= HistoVault.class.getName();
	final private static Logger	log					= Logger.getLogger($CLASS_NAME);

	private static Path					activeDevicePath;																	// criterion for the active device version key cache
	private static String				activeDeviceKey;																	// caches the version key for the active device which is calculated only if the device is changed by the user
	private static long					activeDeviceLastModified_ms;											// caches the version key for the active device which is calculated only if the device is changed by the user
	private static JAXBContext	jaxbContext;
	private static Unmarshaller	jaxbUnmarshaller;
	private static Marshaller		jaxbMarshaller;

	@XmlTransient
	private final DataExplorer	application	= DataExplorer.getInstance();
	@XmlTransient
	private final Settings			settings		= Settings.getInstance();
	@XmlTransient
	private final IDevice				device			= this.application.getActiveDevice();

	@XmlElement(required = true)
	protected String						vaultName;
	@XmlElement(required = true)
	protected String						vaultDirectory;
	@XmlElement(name = "vaultCreated_ms")
	protected long							vaultCreatedMs;
	@XmlElement(required = true)
	protected String						vaultDataExplorerVersion;
	@XmlElement(required = true)
	protected String						vaultDeviceKey;
	@XmlElement(required = true)
	protected String						vaultDeviceName;
	protected int								vaultChannelNumber;
	@XmlElement(required = true)
	protected String						vaultObjectKey;
	@XmlElement(name = "vaultSamplingTimespan_ms")
	protected long							vaultSamplingTimespanMs;
	@XmlElement(required = true)
	protected String						logFilePath;
	protected long							logFileLastModified;
	@XmlElement(required = true)
	protected String						logObjectDirectory;
	protected int								logFileVersion;
	protected int								logRecordSetSize;
	protected int								logRecordSetOrdinal;
	@XmlElement(required = true)
	protected String						logRecordsetBaseName;
	@XmlElement(required = true)
	protected String						logDeviceName;
	protected int								logChannelNumber;
	@XmlElement(required = true)
	protected String						logObjectKey;
	@XmlElement(name = "logStartTimestamp_ms")
	protected long							logStartTimestampMs;
	@XmlElement(required = true)
	protected Entries						measurements;
	@XmlElement(required = true)
	protected Entries						settlements;
	@XmlElement(required = true)
	protected EntryPoints				scores;

	/**
	   * Gets the value of the vaultName property.
	   * 
	   * @return
	   *     possible object is
	   *     {@link String }
	   *     
	   */
	public String getVaultName() {
		return vaultName;
	}

	/**
	 * Gets the value of the vaultDirectory property.
	* 
	* @return
	*     possible object is
	*     {@link String }
	*     
	*/
	public String getVaultDirectory() {
		return vaultDirectory;
	}

	/**
	 * Gets the value of the vaultCreatedMs property.
	 * 
	 */
	public long getVaultCreated_ms() {
		return vaultCreatedMs;
	}

	/**
	   * Gets the value of the vaultDataExplorerVersion property.
	 *     
	   * @return
	   *     possible object is
	   *     {@link String }
	   *     
	 */
	public String getVaultDataExplorerVersion() {
		return vaultDataExplorerVersion;
	}

	/**
	   * Gets the value of the vaultDeviceKey property.
	 * 
	 * @return
	 *     possible object is
	   *     {@link String }
	 *     
	 */
	public String getVaultDeviceKey() {
		return vaultDeviceKey;
	}

	/**
	   * Gets the value of the vaultDeviceName property.
	   * 
	   * @return
	   *     possible object is
	   *     {@link String }
	   * 
	   */
	public String getVaultDeviceName() {
		return vaultDeviceName;
	}

	/**
	   * Gets the value of the vaultChannelNumber property.
	 * 
	 */
	public int getVaultChannelNumber() {
		return vaultChannelNumber;
	}

	/**
	   * Gets the value of the vaultObjectKey property.
	 * 
	 * @return
	 *     possible object is
	 *     {@link String }
	 *     
	 */
	public String getVaultObjectKey() {
		return vaultObjectKey;
	}

	/**
	 * Gets the value of the vaultSamplingTimespanMs property.
	 * 
	 */
	public long getVaultSamplingTimespan_ms() {
		return vaultSamplingTimespanMs;
	}

	/**
	 * Gets the value of the logFilePath property.
	 * 
	 * @return
	 *     possible object is
	 *     {@link String }
	 *     
	 */
	public String getLogFilePath() {
		return logFilePath;
	}

	/**
	 * Gets the value of the logFileLastModified property.
	 * 
	 */
	public long getLogFileLastModified() {
		return logFileLastModified;
	}

	/**
	 * Gets the value of the logObjectDirectory property.
	 * 
	 * @return
	 *     possible object is
	 *     {@link String }
	 *     
	 */
	public String getLogObjectDirectory() {
		return logObjectDirectory;
	}

	/**
	 * Gets the value of the logFileVersion property.
	 * 
	 */
	public int getLogFileVersion() {
		return logFileVersion;
	}

	/**
	 * Gets the value of the logRecordSetSize property.
	 * 
	 */
	public int getLogRecordSetSize() {
		return logRecordSetSize;
	}

	/**
	 * Gets the value of the logRecordSetOrdinal property.
	 * 
	 */
	public int getLogRecordSetOrdinal() {
		return logRecordSetOrdinal;
	}

	/**
	 * Gets the value of the logRecordsetName property.
	 * 
	 * @return
	 *     possible object is
	 *     {@link String }
	 *     
	 */
	public String getLogRecordsetBaseName() {
		return logRecordsetBaseName;
	}

	/**
	 * Gets the value of the logDeviceName property.
	 * 
	 * @return
	 *     possible object is
	 *     {@link String }
	 *     
	 */
	public String getLogDeviceName() {
		return logDeviceName;
	}

	/**
	 * Gets the value of the logChannelNumber property.
	 * 
	 */
	public int getLogChannelNumber() {
		return logChannelNumber;
	}

	/**
	 * Gets the value of the logObjectKey property.
	 * 
	 * @return
	 *     possible object is
	 *     {@link String }
	 *     
	 */
	public String getLogObjectKey() {
		return logObjectKey;
	}

	/**
	 * Gets the value of the logStartTimestampMs property.
	 * 
	 */
	public long getLogStartTimestamp_ms() {
		return logStartTimestampMs;
	}

	/**
	* Gets the value of the measurements property.
	* 
	* @return
	*     possible object is
	*     {@link Entries }
	*     
	*/
	public Entries getMeasurements() {
		return measurements;
	}

	/**
	 * Sets the value of the measurements property.
	 * 
	 * @param value
	 *     allowed object is
	 *     {@link Entries }
	 *     
	 */
	public void setMeasurements(Entries value) {
		this.measurements = value;
	}

	/**
	 * Gets the value of the settlements property.
	 * 
	 * @return
	 *     possible object is
	 *     {@link Entries }
	 *     
	 */
	public Entries getSettlements() {
		return settlements;
	}

	/**
	 * Sets the value of the settlements property.
	 * 
	 * @param value
	 *     allowed object is
	 *     {@link Entries }
	 *     
	 */
	public void setSettlements(Entries value) {
		this.settlements = value;
	}

	/**
	   * Gets the value of the scores property.
	 * 
	 * @return
	 *     possible object is
	 *     {@link EntryPoints }
	 *     
	 */
	public EntryPoints getScores() {
		return scores;
	}

	/**
	   * Sets the value of the scores property.
	 * 
	 * @param value
	 *     allowed object is
	 *     {@link EntryPoints }
	 *     
	 */
	public void setScores(EntryPoints value) {
		this.scores = value;
	}

	@Deprecated // for marshalling purposes only
	public HistoVault() {
	}

	/**
	 * @param objectDirectory validated object key
	 * @param filePath file name + lastModified are a simple solution for getting a SHA-1 hash from the file contents
	 * @param fileLastModified_ms file name + lastModified are a simple solution for getting a SHA-1 hash from the file contents
	 * @param fileVersion is the version of the log origin file
	 * @param logRecordSetSize is the number of recordsets in the log origin file
	 * @param logRecordSetOrdinal identifies multiple recordsets within on single file
	 * @param logRecordsetBaseName the base name without recordset number
	 * @param logStartTimestamp_ms of the log or recordset
	 * @param logDeviceName 
	 * @param logChannelNumber may differ from UI settings in case of channel mix
	 * @param logObjectKey may differ from UI settings (empty in OSD files, validated parent path for bin files)
	 */
	private HistoVault(String objectDirectory, Path filePath, long fileLastModified_ms, int fileVersion, int logRecordSetSize, int logRecordSetOrdinal, String logRecordSetBaseName, String logDeviceName,
			long logStartTimestamp_ms, int logChannelNumber, String logObjectKey) {
		this.vaultDataExplorerVersion = GDE.VERSION;
		this.vaultDeviceKey = HistoVault.getActiveDeviceKey();
		this.vaultDeviceName = this.application.getActiveDevice().getName();
		this.vaultChannelNumber = this.application.getActiveChannelNumber();
		this.vaultObjectKey = this.application.getObjectKey();
		this.vaultSamplingTimespanMs = this.settings.getSamplingTimespan_ms();
		this.logFilePath = filePath.toString(); // toString due to avoid 'Object' during marshalling
		this.logFileLastModified = fileLastModified_ms;
		this.logFileVersion = fileVersion;
		this.logRecordSetSize = logRecordSetSize;
		this.logObjectDirectory = objectDirectory;
		this.logRecordSetOrdinal = logRecordSetOrdinal;
		this.logRecordsetBaseName = logRecordSetBaseName;
		this.logDeviceName = logDeviceName;
		this.logChannelNumber = logChannelNumber;
		this.logObjectKey = logObjectKey;
		this.logStartTimestampMs = logStartTimestamp_ms;

		this.vaultDirectory = HistoVault.getVaultsDirectory();
		this.vaultName = HistoVault.getVaultName(filePath, fileLastModified_ms, logRecordSetOrdinal);
		this.vaultCreatedMs = System.currentTimeMillis();
		if (log.isLoggable(Level.FINER)) log.log(Level.FINER,
				String.format("HistoVault.ctor  objectDirectory=%s  path=%s  lastModified=%s  logRecordSetOrdinal=%d  logRecordSetBaseName=%s  startTimestamp_ms=%d   channelConfigNumber=%d   objectKey=%s",
						objectDirectory, filePath.getFileName().toString(), logRecordSetBaseName, getOriginLastModifiedFormatted(), getStartTimeStampFormatted(), logChannelNumber, logObjectKey));
		if (log.isLoggable(Level.FINER)) log.log(Level.FINER, String.format("vaultDirectory=%s  vaultName=%s", this.vaultDirectory, this.vaultName)); //$NON-NLS-1$
	}

	/**
	 * @param logStartTimestamp_ms of the log or recordset
	 * @param measurementEntries
	 * @param settlementEntries
	 * @param scorePoints
	 */
	public void complementTruss(long newStartTimestamp_ms, Entries measurementEntries, Entries settlementEntries, EntryPoints scorePoints) {
		this.logStartTimestampMs = newStartTimestamp_ms;

		this.setMeasurements(measurementEntries);
		this.setSettlements(settlementEntries);
		this.setScores(scorePoints);
	}

	/**
	 * @param objectDirectory validated object key
	 * @param file is the log origin file (not a link file)
	 * @param fileVersion is the version of the log origin file
	 * @param logRecordSetSize is the number of recordsets in the log origin file
	 * @param logRecordSetOrdinal identifies multiple recordsets within on single file
	 * @param logRecordsetBaseName the base name without recordset number
	 * @param logStartTimestamp_ms of the log or recordset
	 * @param logDeviceName 
	 * @param logChannelNumber may differ from UI settings in case of channel mix
	 * @param logObjectKey may differ from UI settings (empty in OSD files, validated parent path for bin files)
	 * @return new instance with a basic set of data
	 */
	public static HistoVault createTruss(String objectDirectory, File file, int fileVersion, int logRecordSetSize, int logRecordSetOrdinal, String logRecordsetBaseName, String logDeviceName,
			long logStartTimestamp_ms, int logChannelNumber, String logObjectKey) {
		HistoVault newHistoVault = new HistoVault(objectDirectory, file.toPath(), file.lastModified(), fileVersion, logRecordSetSize, logRecordSetOrdinal, logRecordsetBaseName, logDeviceName,
				logStartTimestamp_ms, logChannelNumber, logObjectKey);
		newHistoVault.setMeasurements(new Entries());
		newHistoVault.setSettlements(new Entries());
		newHistoVault.setScores(new EntryPoints("truss", 0));
		return newHistoVault;
	}

	/**
	 * @param objectDirectory validated object key
	 * @param file is the bin origin file (not a link file)
	 * @param fileVersion is the version of the log origin file
	 * @param logRecordSetSize is the number of recordsets in the log origin file
	 * @param logRecordsetBaseName the base name without recordset number
	 * @return new instance with a basic set of data
	 */
	public static HistoVault createTruss(String objectDirectory, File file, int fileVersion, int logRecordSetSize, String logRecordsetBaseName) {
		HistoVault newHistoVault = new HistoVault(objectDirectory, file.toPath(), file.lastModified(), fileVersion, logRecordSetSize, 0, logRecordsetBaseName, "native", file.lastModified(),
				DataExplorer.getInstance().getActiveChannelNumber(), objectDirectory);
		newHistoVault.setMeasurements(new Entries());
		newHistoVault.setSettlements(new Entries());
		newHistoVault.setScores(new EntryPoints("truss", 0));
		return newHistoVault;
	}

	/**
	 * @param fullQualifiedFileName path
	 * @return new instance 
	 */
	public static HistoVault load(Path fullQualifiedFileName) {
		HistoVault newHistoVault = null;
		try {
			newHistoVault = (HistoVault) HistoVault.getUnmarshaller().unmarshal(fullQualifiedFileName.toFile());
		}
		catch (JAXBException e) {
			e.printStackTrace();
		}
		return newHistoVault;
	}

	/**
	 * @param inputStream
	 * @return new instance 
	 */
	public static HistoVault load(InputStream inputStream) {
		HistoVault newHistoVault = null;
		try {
			newHistoVault = (HistoVault) HistoVault.getUnmarshaller().unmarshal(inputStream);
		}
		catch (JAXBException e) {
			e.printStackTrace();
		}
		return newHistoVault;
	}

	/**
	 * writes device properties XML to given full qualified file name
	 * @param fullQualifiedFileName
	 */
	public void store(Path fullQualifiedFileName) {
		try {
			HistoVault.getMarshaller().marshal(this, fullQualifiedFileName.toFile());
		}
		catch (JAXBException e) {
			e.printStackTrace();
		}
	}

	/**
	 * writes device properties XML to given full qualified file name
	 * @param fullQualifiedFileName
	 */
	public void store(OutputStream outputStream) {
		try {
			HistoVault.getMarshaller().marshal(this, outputStream);
		}
		catch (JAXBException e) {
			e.printStackTrace();
		}
	}

	/**
	 * @return context singleton (creating the context is slow)
	 */
	private static JAXBContext getJaxbContext() {
		if (HistoVault.jaxbContext == null) {
			try {
				HistoVault.jaxbContext = JAXBContext.newInstance(HistoVault.class);
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		return HistoVault.jaxbContext;
	}

	/**
	 * @return cached instance (unmarshaller is not thread safe) which is ~100 ms faster than creating a new instance from a cached JaxbContext instance
	 */
	private static Unmarshaller getUnmarshaller() {
		if (HistoVault.jaxbUnmarshaller == null) {
			final Path path = Paths.get(Settings.getInstance().getApplHomePath(), Settings.HISTO_CACHE_ENTRIES_DIR_NAME, Settings.HISTO_CACHE_ENTRIES_XSD_NAME);
			try {
				HistoVault.jaxbUnmarshaller = getJaxbContext().createUnmarshaller();
				HistoVault.jaxbUnmarshaller.setSchema(SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema(path.toFile()));
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		return HistoVault.jaxbUnmarshaller;
	}

	/**
	 * @return cached instance (marshaller is not thread safe) which is ~100 ms faster than creating a new instance from a cached JaxbContext instance
	 */
	private static Marshaller getMarshaller() {
		if (HistoVault.jaxbMarshaller == null) {
			final Path path = Paths.get(Settings.getInstance().getApplHomePath(), Settings.HISTO_CACHE_ENTRIES_DIR_NAME, Settings.HISTO_CACHE_ENTRIES_XSD_NAME);
			try {
				HistoVault.jaxbMarshaller = getJaxbContext().createMarshaller();
				HistoVault.jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
				HistoVault.jaxbMarshaller.setSchema(SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema(path.toFile()));
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		return HistoVault.jaxbMarshaller;
	}

	/**
	 * checks if the vault conforms to the current environment.
	 * does not check if the file is accessible, carries the last modified timestamp and holds the recordset ordinal.
	 * @return true if the vault object conforms to current versions of the Data Explorer / device XML, to current user settings (e.g. sampling timespan) and to various additional attributes
	 */
	public boolean isValid() {
		return this.vaultName.equals(HistoVault.getVaultName(Paths.get(this.logFilePath), this.logFileLastModified, this.logRecordSetOrdinal));
	}

	/**
	 * checks if the vault conforms to the current environment, if the file is accessible and has the same last modified timestamp.
	 * reads the history recordset from the file based on the active device, converts it into a vault and compares the key values.
	 * @return true if the vault's origin file exists and produces the same vault key values compared to this vault
	 * @throws DataInconsitsentException 
	 * @throws NotSupportedFileFormatException 
	 * @throws IOException 
	 */
	public boolean isSubstantiated() throws IOException, NotSupportedFileFormatException, DataInconsitsentException {
		File file = Paths.get(Settings.getInstance().getApplHomePath(), Settings.HISTO_CACHE_ENTRIES_DIR_NAME, this.vaultDirectory, this.vaultName).toFile();
		if (this.vaultName.equals(HistoVault.getVaultName(Paths.get(this.logFilePath), file.lastModified(), this.logRecordSetOrdinal))) {
			List<HistoVault> trusses = new ArrayList<HistoVault>();
			trusses.add(this);
			List<HistoVault> histoRecordSets = HistoOsdReaderWriter.readHisto(file.toPath(), trusses);
			return this.getVaultName().equals(histoRecordSets.get(0).getVaultName());
		}
		else {
			return false;
		}
	}

	/**
	 * @param newVaultDirectory directory or zip file name
	 * @return true if the vault directory name conforms to current versions of the Data Explorer / device XML, to the current channel and to user settings (e.g. sampling timespan) and to various additional attributes
	 */
	public static boolean isValidDirectory(String newVaultsDirectory) {
		return newVaultsDirectory.equals(HistoVault.getVaultsDirectory());
	}

	/**
	 * @return directory or zip file name as a unique identifier encoding the data explorer version, the device xml file contents(sha1) plus channel number and some settings values
	 */
	public static String getVaultsDirectory() {
		String tmpSubDirectoryLongKey = String.format("%s,%s,%d,%d", GDE.VERSION, HistoVault.getActiveDeviceKey(), DataExplorer.getInstance().getActiveChannelNumber(), //$NON-NLS-1$
				Settings.getInstance().getSamplingTimespan_ms());
		return HistoVault.sha1(tmpSubDirectoryLongKey);

	}

	private static String getVaultName(Path newLogFileName, long newFileLastModified_ms, int newLogRecordSetOrdinal) {
		return sha1(HistoVault.getVaultsDirectory() + String.format("%s,%d,%d", newLogFileName.getFileName(), newFileLastModified_ms, newLogRecordSetOrdinal));
	}

	/**
	 * @param newLogFileName file name + lastModified are a simple solution for getting a SHA-1 hash from the file contents
	 * @param newFileLastModified_ms file name + lastModified are a simple solution for getting a SHA-1 hash from the file contents
	 * @param newLogRecordSetOrdinal identifies multiple recordsets in one single file
	 * @return the path with filename as a unique identifier (sha1)
	 */
	public static Path getVaultRelativePath(Path newLogFileName, long newFileLastModified_ms, int newLogRecordSetOrdinal) {
		// do not include as these attributes are determined after reading the histoset: logChannelNumber, logObjectKey, logStartTimestampMs
		Path subDirectory = Paths.get(HistoVault.getVaultsDirectory());
		return subDirectory.resolve(HistoVault.getVaultName(newLogFileName.getFileName(), newFileLastModified_ms, newLogRecordSetOrdinal));
	}

	/**
	 * @return sha1 key as a unique identifier for the device xml file contents
	 */
	private static String getActiveDeviceKey() {
		File file = new File(DataExplorer.getInstance().getActiveDevice().getPropertiesFileName());
		if (HistoVault.activeDeviceKey == null || HistoVault.activeDevicePath == null || !HistoVault.activeDevicePath.equals(file.toPath())
				|| HistoVault.activeDeviceLastModified_ms != file.lastModified()) {
			try {
				HistoVault.activeDeviceKey = HistoVault.sha1(file);
				HistoVault.activeDevicePath = file.toPath();
				HistoVault.activeDeviceLastModified_ms = file.lastModified();
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}
		return HistoVault.activeDeviceKey;
	}

	/**
	 * source: http://www.sha1-online.com/sha1-java/
	 * @param input
	 * @return the SHA-1 hash value rendered as a hexadecimal number, 40 digits long
	 */
	private static String sha1(String input) {
		byte[] hashBytes = null;
		try {
			hashBytes = MessageDigest.getInstance("SHA1").digest(input.getBytes()); //$NON-NLS-1$
		}
		catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < hashBytes.length; i++) {
			sb.append(Integer.toString((hashBytes[i] & 0xff) + 0x100, 16).substring(1));
		}
		return sb.toString();
	}

	/**
	 * source: http://www.sha1-online.com/sha1-java/
	 * @param file 
	 * @return the file's full data SHA1 checksum
	 * @throws IOException
	 */
	private static String sha1(File file) throws IOException {
		MessageDigest sha1 = null;
		try {
			sha1 = MessageDigest.getInstance("SHA1"); //$NON-NLS-1$
		}
		catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		byte[] hashBytes = null;
		try (FileInputStream fis = new FileInputStream(file)) {
			byte[] data = new byte[8192]; // most file systems are configured to use block sizes of 4096 or 8192
			int read = 0;
			while ((read = fis.read(data)) != -1) {
				sha1.update(data, 0, read);
			}
			hashBytes = sha1.digest();
		}
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < hashBytes.length; i++) {
			sb.append(Integer.toString((hashBytes[i] & 0xff) + 0x100, 16).substring(1));
		}
		return sb.toString();
	}

	public List<Point> getMeasurements(int measurementOrdinal) {
		if (this.measurements.getEntryPoints().get(measurementOrdinal).getId() != measurementOrdinal) {
			throw new RuntimeException("measurementOrdinal sequence or numbering mismatch"); //$NON-NLS-1$
		}
		return this.measurements.getEntryPoints().get(measurementOrdinal).getPoints();
	}

	public Integer getMeasurement(int measurementOrdinal, int trailTextSelectedIndex, int trailOrdinal) { // todo handle xml null values for points
		if (this.measurements.getEntryPoints().get(measurementOrdinal).getId() != measurementOrdinal) {
			throw new RuntimeException("measurementOrdinal sequence or numbering mismatch"); //$NON-NLS-1$
		}
		if (this.measurements.getEntryPoints().get(measurementOrdinal).getPoints().size() == 0) {
			return null;
		}
		else {
			if (trailTextSelectedIndex > this.measurements.getEntryPoints().get(measurementOrdinal).getPoints().size()) //
				throw new UnsupportedOperationException();
			if (this.measurements.getEntryPoints().get(measurementOrdinal).getPoints().get(trailTextSelectedIndex).getId() != trailOrdinal) {
				if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, String.format("shortcut access to the point is not possible %d %d %d", measurementOrdinal, trailTextSelectedIndex, trailOrdinal)); //$NON-NLS-1$
				for (Point point : this.measurements.getEntryPoints().get(measurementOrdinal).getPoints()) {
					if (point.getId() == trailOrdinal) {
						return point.getValue();
					}
				}
			}
			return this.measurements.getEntryPoints().get(measurementOrdinal).getPoints().get(trailTextSelectedIndex).getValue();
		}
	}

	public List<Point> getSettlements(int settlementId) {
		if (this.settlements.getEntryPoints().get(settlementId).getId() != settlementId) {
			throw new RuntimeException("settlementId sequence or numbering mismatch"); //$NON-NLS-1$
		}
		return this.settlements.getEntryPoints().get(settlementId).getPoints();
	}

	public Integer getSettlement(int settlementId, int trailTextSelectedIndex, int trailOrdinal) {
		if (this.settlements.getEntryPoints().get(settlementId).getId() != settlementId) {
			throw new RuntimeException("settlementId sequence or numbering mismatch"); //$NON-NLS-1$
		}
		if (this.settlements.getEntryPoints().get(settlementId).getPoints().size() == 0) {
			return null;
		}
		else {
			if (trailTextSelectedIndex >= this.settlements.getEntryPoints().get(settlementId).getPoints().size()) //
				throw new UnsupportedOperationException();
			if (this.settlements.getEntryPoints().get(settlementId).getPoints().get(trailTextSelectedIndex).getId() != trailOrdinal) {
				if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, String.format("shortcut access to the point is not possible %d %d %d", settlementId, trailTextSelectedIndex, trailOrdinal)); //$NON-NLS-1$
				for (Point point : this.settlements.getEntryPoints().get(settlementId).getPoints()) {
					if (point.getId() == trailOrdinal) {
						return point.getValue();
					}
				}
			}
			return this.settlements.getEntryPoints().get(settlementId).getPoints().get(trailTextSelectedIndex).getValue();
		}
	}

	public List<Point> getScorePoints() {
		return this.scores.getPoints();
	}

	public Integer getScorePoint(int scoreLabelOrdinal) {
		if (this.scores.getPoints() == null) {
			return null;
		}
		else {
			if (scoreLabelOrdinal >= this.scores.getPoints().size())//
				throw new UnsupportedOperationException();
			if (this.scores.getPoints().get(scoreLabelOrdinal).getId() != scoreLabelOrdinal) {
				throw new RuntimeException("scoreLabelOrdinal sequence or numbering mismatch"); //$NON-NLS-1$
			}
			return this.scores.getPoints().get(scoreLabelOrdinal).getValue();
		}
	}

	/**
	 * @return yyyy-MM-dd HH:mm:ss
	 */
	public String getVaultCreatedFormatted() {
		return StringHelper.getFormatedTime("yyyy-MM-dd HH:mm:ss", this.vaultCreatedMs);
	}

	/**
	 * @return yyyy-MM-dd HH:mm:ss
	 */
	public String getOriginLastModifiedFormatted() {
		return StringHelper.getFormatedTime("yyyy-MM-dd HH:mm:ss", this.logFileLastModified);
	}

	/**
	 * @return yyyy-MM-dd HH:mm:ss
	 */
	public String getStartTimeStampFormatted() {
		return StringHelper.getFormatedTime("yyyy-MM-dd HH:mm:ss.SSS", this.logStartTimestampMs);
	}

	/**
	 * @return relative path (directory name) 
	 */
	public Path getVaultDirectoryPath() {
		return Paths.get(this.vaultDirectory);
	}

	public Path getVaultFileName() {
		return Paths.get(this.vaultName);
	}

	/**
	 * @return the non-validated object key or alternatively (if empty) the non-validated object directory
	 */
	public String getRectifiedObjectKey() {
		return this.logObjectKey.isEmpty() ? this.logObjectDirectory : this.logObjectKey;
	}

	/**
	 * @return the validated object key or alternatively (if empty) the validated object directory
	 */
	public String getValidatedObjectKey() {
		String validObjectKey = this.settings.getValidatedObjectKey(this.logObjectKey).orElse(GDE.STRING_EMPTY);
		return this.logObjectKey.isEmpty() ? this.settings.getValidatedObjectKey(this.logObjectKey).orElse(GDE.STRING_EMPTY) : validObjectKey;
	}

	public boolean isTruss() {
		return this.measurements.entryPoints == null;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(this.vaultName).append(GDE.STRING_COMMA);
		sb.append("logRecordSetOrdinal=").append(this.logRecordSetOrdinal).append(GDE.STRING_COMMA);
		sb.append("logRecordsetBaseName=").append(this.logRecordsetBaseName).append(GDE.STRING_COMMA);
		sb.append("logChannelNumber=").append(this.logChannelNumber).append(GDE.STRING_COMMA);
		sb.append("logObjectKey=").append(this.logObjectKey).append(GDE.STRING_COMMA);
		sb.append("logStartTimestampMs=").append(this.logStartTimestampMs).append(GDE.STRING_COMMA);
		sb.append(this.logFilePath).append(GDE.STRING_COMMA);
		sb.append("vaultDirectory=").append(this.vaultDirectory);
		sb.append("isTruss=").append(isTruss());
		return sb.toString();
	}
}
