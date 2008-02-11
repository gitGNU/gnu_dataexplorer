/**************************************************************************************
  	This file is part of OpenSerialdataExplorer.

    OpenSerialdataExplorer is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    OpenSerialdataExplorer is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with OpenSerialdataExplorer.  If not, see <http://www.gnu.org/licenses/>.
****************************************************************************************/
package osde.device;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.math.BigInteger;
import java.util.List;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import osde.config.Settings;
import osde.log.LogFormatter;

/**
 * Device Configuration class makes the parsed DeviceProperties XML accessible for the application
 * @author Winfried Brügmann
 */
public class DeviceConfiguration {
	private final static Logger									log												= Logger.getLogger(DeviceSerialPort.class.getName());

	// JAXB XML environment
	private final Unmarshaller								unmarshaller;
	private final Marshaller									marshaller;
	private File															xmlFile;
	// XML JAXB representation
	private JAXBElement<DevicePropertiesType>	elememt;
	private DevicePropertiesType							deviceProps;
	private DeviceType												device;
	private SerialPortType										serialPort;
	private TimeBaseType											timeBase;
	private boolean														isChangePropery						= false;

	public final static int										DEVICE_TYPE_CHARGER				= 1;
	public final static int										DEVICE_TYPE_LOGGER				= 2;
	public final static int										DEVICE_TYPE_BALANCER			= 3;
	public final static int										DEVICE_TYPE_CURRENT_SINK	= 4;
	public final static int										DEVICE_TYPE_POWER_SUPPLY	= 5;
	public final static int										DEVICE_TYPE_GPS						= 5;
	public final static int										DEVICE_TYPE_RECEIVER			= 7;
	public final static int										DEVICE_TYPE_MULTIMETER		= 8;

	/**
	 * method to test this class
	 * @param args
	 */
	@SuppressWarnings("unchecked")
	public static void main(String[] args) {
		Handler ch = new ConsoleHandler();
		LogFormatter lf = new LogFormatter();
		ch.setFormatter(lf);
		ch.setLevel(Level.ALL);
		Logger.getLogger("").addHandler(ch);
		Logger.getLogger("").setLevel(Level.ALL);

		String basePath = "C:/Documents and Settings/brueg/Application Data/OpenSerialDataExplorer/Devices/";
		//String basePath = "D:/Belchen2/workspaces/test/OpenSerialDataExplorer/doc/";

		try {
      Schema schema = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema(new File(basePath + "DeviceProperties_V03.xsd"));
			JAXBContext jc = JAXBContext.newInstance("osde.device");
			Unmarshaller unmarshaller = jc.createUnmarshaller();
			unmarshaller.setSchema(schema);
			JAXBElement<DevicePropertiesType> elememt;
			
			// simple test with Picolario.xml
			elememt = (JAXBElement<DevicePropertiesType>)unmarshaller.unmarshal(new File (basePath + "Picolario.xml"));
			DevicePropertiesType devProps = elememt.getValue();
			DeviceType device = devProps.getDevice();
			log.info("device.getName() = " + device.getName());
			SerialPortType serialPort = devProps.getSerialPort();
			log.info("serialPort.getPort() = " + serialPort.getPort());
			serialPort.setPort("COM10");
			log.info("serialPort.getPort() = " + serialPort.getPort());
			
			
			
			// store back manipulated XML
			Marshaller marshaller = jc.createMarshaller();
	    marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT,  new Boolean(true));
	    marshaller.setProperty(Marshaller.JAXB_SCHEMA_LOCATION,  Settings.DEVICE_PROPERTIES_XSD_NAME);

	    marshaller.marshal(elememt,
	    	   new FileOutputStream(basePath + "jaxbOutput.xml"));
			
		}
		catch (Exception e) {
			log.log(Level.SEVERE, e.getMessage(), e);
		}
		catch (Throwable t) {
			log.log(Level.SEVERE, t.getMessage(), t);
		}
	}

	@SuppressWarnings("unchecked") // cast to (JAXBElement<DevicePropertiesType>)
	public DeviceConfiguration(String xmlFileName) throws FileNotFoundException, JAXBException {

		if (!(this.xmlFile = new File(xmlFileName)).exists()) throw new FileNotFoundException("Die Gerätedatei wurde nicht gefunden - " + xmlFileName);

		Settings settings = Settings.getInstance();
		this.unmarshaller = settings.getUnmarshaller();
		this.marshaller = settings.getMarshaller();

		this.elememt = (JAXBElement<DevicePropertiesType>)unmarshaller.unmarshal(this.xmlFile);
		this.deviceProps = elememt.getValue();
		this.device = deviceProps.getDevice();
		this.serialPort = deviceProps.getSerialPort();
		this.timeBase = deviceProps.getTimeBase();
		
		if (log.isLoggable(Level.FINE)) log.fine(this.toString());
	}

	/**
	 * copy constructor
	 */
	public DeviceConfiguration(DeviceConfiguration deviceConfig) {
		this.unmarshaller = deviceConfig.unmarshaller;
		this.marshaller = deviceConfig.marshaller;
		this.xmlFile = deviceConfig.xmlFile;
		this.elememt = deviceConfig.elememt;
		this.deviceProps = deviceConfig.deviceProps;
		this.device = deviceConfig.device;
		this.serialPort = deviceConfig.serialPort;	
		this.timeBase = deviceConfig.timeBase;	
		this.isChangePropery = deviceConfig.isChangePropery;

		if (log.isLoggable(Level.FINE)) log.fine(this.toString());
	}

//	public DeviceConfiguration(DeviceType device, SerialPortType serialPort, TimeBaseType timeBase, HashMap<String, ChannelType> channels) {
//		this.device = device;
//		this.serialPort = serialPort;
//		this.timeBase = timeBase;
//		for (int i = 1; i <= channels.size(); ++i) {
//			this.channels.put(i, channels.get(i));
//		}
//
//		if (log.isLoggable(Level.FINE)) log.fine(this.toString());
//	}

	/**
	 * writes updated device properties XML
	 */
	public void storeDeviceProperties() {
		if (isChangePropery) {
			try {
		    marshaller.marshal(this.elememt,  new FileOutputStream(this.xmlFile));
			}
			catch (Throwable t) {
				log.log(Level.SEVERE, t.getMessage(), t);
			}
		}
	}

	/**
	 * @return the device
	 */
	public DeviceType getDeviceType() {
		return device;
	}

	/**
	 * @return the serialPort
	 */
	public SerialPortType getSerialPortType() {
		return serialPort;
	}

	/**
	 * @return the timeBase
	 */
	public TimeBaseType getTimeBaseType() {
		return timeBase;
	}

	public boolean isUsed() {
		return this.device.isUsage();
	}

	public void setUsed(boolean value) {
		this.isChangePropery = true;
		this.device.setUsage(value);
	}

	public String getPropertiesFileName() {
		return this.xmlFile.getAbsolutePath();
	}

	public String getName() {
		return this.device.getName();
	}

	public void setName(String newName) {
		this.isChangePropery = true;
		this.device.setName(newName);
	}

	public String getManufacturer() {
		return this.device.getManufacturer();
	}

	public String getManufacturerURL() {
		return this.device.getManufacturerURL();
	}

	public String getDeviceGroup() {
		return this.device.getGroup();
	}

	public String getImageFileName() {
		return this.device.getImage();
	}

	public int getTimeStep_ms() {
		return this.timeBase.getTimeStep().intValue();
	}

	public void setTimeStep_ms(int newTimeStep_ms) {
		this.isChangePropery = true;
		this.timeBase.setTimeStep(new BigInteger("" + newTimeStep_ms));
	}

	public String getPort() {
		return this.serialPort.getPort();
	}

	public void setPort(String newPort) {
		this.isChangePropery = true;
		this.serialPort.setPort(newPort);
	}

	public int getBaudeRate() {
		return this.serialPort.getBaudeRate().intValue();
	}

	public int getDataBits() {
		return this.serialPort.getDataBits().intValue();
	}

	public int getStopBits() {
		return this.serialPort.getStopBits().ordinal()+1; // starts with 1
	}

	public int getFlowCtrlMode() {
		return this.serialPort.getFlowControlMode().ordinal();
	}

	public int getParity() {
		return this.serialPort.getParity().ordinal();
	}

	public int getDataBlockSize() {
		return this.serialPort.getDataBlock().getSize().intValue();
	}

	public void setDataBlockSize(int newSize) {
		this.isChangePropery = true;
		this.serialPort.getDataBlock().setSize(new BigInteger("" + newSize));
	}

	public byte getEndingByte() {
		return this.serialPort.getDataBlock().getEndingByte();
	}

	public boolean isDTR() {
		return this.serialPort.isIsDTR();
	}

	public boolean isRTS() {
		return this.serialPort.isIsRTS();
	}

	/**
	 * @return the channel count
	 */
	public int getChannelCount() {
		return this.deviceProps.getChannel().size();
	}

	/**
	 * @return the channel name
	 */
	public String getChannelName(int channelNumber) {
		return this.deviceProps.getChannel().get(channelNumber - 1).getName();
	}

	/**
	 * @param channelName - size should not exceed 15 char length
	 * @param channelNumber
	 * @return the channel name
	 */
	public void setChannelName(String channelName, int channelNumber) {
		this.isChangePropery = true;
		this.deviceProps.getChannel().get(channelNumber - 1).setName(channelName);
	}

	/**
	 * @return the channel type by given channel number 
	 * 0 = TYPE_OUTLET, 1 = TYPE_CONFIG;
	 */
	public int getChannelType(int channelNumber) {
		return this.deviceProps.getChannel().get(channelNumber - 1).getType().ordinal();
	}
	
	/**
	 * @return the channel type by given channel configuration key (name)
	 * 0 = TYPE_OUTLET, 1 = TYPE_CONFIG;
	 */
	public int getChannelType(String channelConfigKey) {
		return this.getChannel(channelConfigKey).getType().ordinal();
	}
	
	/**
	 * @return the number of measurements of a channel by given channel number
	 */
	public int getNumberOfMeasurements(int channelNumber) {
		return this.deviceProps.getChannel().get(channelNumber - 1).getMeasurement().size();
	}

	/**
	 * @return the number of measurements of a channel by given channel configuration key (name)
	 */
	public int getNumberOfMeasurements(String channelConfigKey) {
		return this.getChannel(channelConfigKey).getMeasurement().size();
	}

	/**
	 * get the channel type by given channel configuration key (name)
	 * @param channelConfigKey
	 * @return
	 */
	private ChannelType getChannel(String channelConfigKey) {
		ChannelType channel = null;
		for (ChannelType c : this.deviceProps.getChannel()) {
			if(c.getName().trim().equals(channelConfigKey)) {
				channel = c;
				break;
			}
		}
		return channel;
	}

	/**
	 * set active status of an measurement
	 * @param channelConfigKey
	 * @param measurementKey
	 * @param isActive
	 */
	public void setMeasurementActive(String channelConfigKey, String measurementKey, boolean isActive) {
		if(log.isLoggable(Level.FINER)) log.finer("channelKey = \"" + channelConfigKey + "\" measurementKey = \"" + measurementKey + "\"");
		this.isChangePropery = true;
		this.getMeasurement(channelConfigKey, measurementKey).setActive(isActive);
	}

	/**
	 * get the measurement to get/set measurement specific parameter/properties
	 * @param channelConfigKey
	 * @param measurementKey
	 * @return
	 */
	public MeasurementType getMeasurement(String channelConfigKey, String measurementKey) {
		MeasurementType measurement = null;
		for (MeasurementType meas : this.getChannel(channelConfigKey).getMeasurement()) {
			if (meas.getName().equals(measurementKey)) {
				measurement = meas;
				break;
			}
		}
		return measurement;
	}
	
	/**
	 * set new name of specified measurement
	 * @param channelConfigKey
	 * @param measurementKey
	 * @param name
	 */
	public void setMeasurementName(String channelConfigKey, String measurementKey, String name) {
		if(log.isLoggable(Level.FINER)) log.finer("channelKey = \"" + channelConfigKey + "\" measurementKey = \"" + measurementKey + "\"");
		this.isChangePropery = true;
		this.getMeasurement(channelConfigKey, measurementKey).setName(name);
	}
	
	/**
	 * method to query the unit of measurement data unit by a given record key
	 * @param channelConfigKey
	 * @param measurementKey
	 * @return dataUnit as string
	 */
	public String getMeasurementUnit(String channelConfigKey, String measurementKey) {
		if(log.isLoggable(Level.FINER)) log.finer("channelKey = \"" + channelConfigKey + "\" measurementKey = \"" + measurementKey + "\"");
		return this.getMeasurement(channelConfigKey, measurementKey.split("_")[0]).getUnit();
	}

	/**
	 * method to set the unit of measurement by a given measurement key
	 * @param channelConfigKey
	 * @param measurementKey
	 * @param unit
	 */
	public void setMeasurementUnit(String channelConfigKey, String measurementKey, String unit) {
		if(log.isLoggable(Level.FINER)) log.finer("channelKey = \"" + channelConfigKey + "\" measurementKey = \"" + measurementKey + "\"");
		this.isChangePropery = true;
		this.getMeasurement(channelConfigKey, measurementKey.split("_")[0]).setUnit(unit);
	}
	
	/**
	 * get the symbol of specified measurement
	 * @param channelConfigKey
	 * @param measurementKey
	 * @param symbol
	 */
	public String getMeasurementSymbol(String channelConfigKey, String measurementKey) {
		return this.getMeasurement(channelConfigKey, measurementKey.split("_")[0]).getSymbol();
	}

	/**
	 * set new symbol of specified measurement
	 * @param channelConfigKey
	 * @param measurementKey
	 * @param symbol
	 */
	public void setMeasurementSymbol(String channelConfigKey, String measurementKey, String symbol) {
		this.isChangePropery = true;
		this.getMeasurement(channelConfigKey, measurementKey).setSymbol(symbol);
	}
	
	/**
	 * @return the sorted measurement names
	 */
	public String[] getMeasurementNames(String channelConfigKey) {
		StringBuilder sb = new StringBuilder();
		ChannelType channel = this.getChannel(channelConfigKey);
		if (channel != null) {
			List<MeasurementType> measurement = channel.getMeasurement();
			for (MeasurementType measurementType : measurement) {
				sb.append(measurementType.getName()).append(";");
			}
		}
		return sb.toString().split(";");
	}

	/**
	 * get property with given channel configuration key, measurement key and property type key (IDevice.OFFSET, ...)
	 * @param channelConfigKey
	 * @param measurementKey
	 * @param propertyKey
	 * @return
	 */
	public PropertyType getMeasruementProperty(String channelConfigKey, String measurementKey, String propertyKey) {
		PropertyType property = null;
		List<PropertyType> properties = this.getMeasurement(channelConfigKey, measurementKey.split("_")[0]).getProperty();
		for (PropertyType propertyType : properties) {
			if(propertyType.getName().equals(propertyKey)) {
				property = propertyType;
				break;
			}
		}
		return property;
	}

	/**
	 * get the offset value of the specified measurement
	 * @param channelConfigKey
	 * @param measurementKey
	 * @return the offset, if property does not exist return 0.0 as default value
	 */
	public double getMeasurementOffset(String channelConfigKey, String measurementKey) {
		if(log.isLoggable(Level.FINER)) log.finer("get offset from measurement name = " + this.getMeasurement(channelConfigKey, measurementKey).getName()); 
		PropertyType property = this.getMeasruementProperty(channelConfigKey, measurementKey.split("_")[0], IDevice.OFFSET);
		if (property == null) // property does not exist
			return 0.0;
		else
			return new Double(property.getValue()).doubleValue();
	}

	/**
	 * set new value for offset at the specified measurement
	 * @param channelConfigKey
	 * @param measurementKey
	 * @param offset the offset to set
	 */
	public void setMeasurementOffset(String channelConfigKey, String measurementKey, double offset) {
		if(log.isLoggable(Level.FINER)) log.finer("set offset onto measurement name = " + this.getMeasurement(channelConfigKey, measurementKey).getName()); 
		PropertyType property = this.getMeasruementProperty(channelConfigKey, measurementKey, IDevice.OFFSET);
		if (property == null) {
			createProperty(channelConfigKey, measurementKey, IDevice.OFFSET, DataTypes.DOUBLE, offset);
		}
		else {
			property.setValue("" + offset);
		}
	}

	/**
	 * get the factor value of the specified measurement
	 * @param channelConfigKey
	 * @param measurementKey
	 * @return the factor, if property does not exist return 1.0 as default value
	 */
	public double getMeasurementFactor(String channelConfigKey, String measurementKey) {
		if(log.isLoggable(Level.FINER)) log.finer("get factor from measurement name = " + this.getMeasurement(channelConfigKey, measurementKey).getName()); 
		PropertyType property = getMeasruementProperty(channelConfigKey, measurementKey.split("_")[0], IDevice.FACTOR);
		if (property == null) // property does not exist
			return 1.0;
		else
			return new Double(property.getValue()).doubleValue();
	}

	/**
	 * set new value for factor at the specified measurement
	 * @param channelConfigKey
	 * @param measurementKey
	 * @param factor the offset to set
	 */
	public void setMeasurementFactor(String channelConfigKey, String measurementKey, double factor) {
		if(log.isLoggable(Level.FINER)) log.finer("set factor onto measurement name = " + this.getMeasurement(channelConfigKey, measurementKey).getName()); 
		PropertyType property = this.getMeasruementProperty(channelConfigKey, measurementKey, IDevice.FACTOR);
		if (property == null) {
			createProperty(channelConfigKey, measurementKey, IDevice.FACTOR, DataTypes.DOUBLE, factor);
		}
		else {
			property.setValue("" + factor);
		}
	}

	/**
	 * get a property of specified measurement, the data type must be known - data conversion is up to implementation
	 * @param channelConfigKey
	 * @param measurementKey
	 * @param propertyKey
	 * @return the property from measurement defined by key, if property does not exist return 1 as default value
	 */
	public Object getMeasurementPropertyValue(String channelConfigKey, String measurementKey, String propertyKey) {
		PropertyType property = this.getMeasruementProperty(channelConfigKey, measurementKey.split("_")[0], propertyKey);
		return property != null ? property.getValue() : 1;
	}
	
	/**
	 * set new property value of specified measurement, if the property does not exist it will be created
	 * @param channelConfigKey
	 * @param measurementKey
	 * @param propertyKey
	 * @param type of DataTypes
	 * @param value
	 */
	public void setMeasurementPropertyValue(String channelConfigKey, String measurementKey, String propertyKey, DataTypes type, Object value) {
		this.isChangePropery = true;
		PropertyType property = this.getMeasruementProperty(channelConfigKey, measurementKey, propertyKey);
		if (property == null) {
			createProperty(channelConfigKey, measurementKey, propertyKey, type, ("" + value).replace(',', '.'));
		}
		else {
			property.setValue(("" + value).replace(',', '.'));
		}
	}

	/**
	 * @param channelConfigKey
	 * @param measurementKey
	 * @param propertyKey
	 * @param type
	 * @param value
	 */
	private void createProperty(String channelConfigKey, String measurementKey, String propertyKey, DataTypes type, Object value) {
		ObjectFactory factory = new ObjectFactory();
		PropertyType newProperty = factory.createPropertyType();
		newProperty.setName(propertyKey);
		newProperty.setType(type);
		newProperty.setValue("" + value);
		this.getMeasurement(channelConfigKey, measurementKey).getProperty().add(newProperty);
	}

	/**
	 * @param isChangePropery the isChangePropery to set
	 */
	public void setChangePropery(boolean isChangePropery) {
		this.isChangePropery = isChangePropery;
	}
}
