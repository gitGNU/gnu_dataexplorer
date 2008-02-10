//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.1.5-b01-fcs 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2008.02.08 at 09:38:52 PM CET 
//


package osde.device;

import java.math.BigInteger;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for SerialPortType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="SerialPortType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="port" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="baudeRate" type="{}baud_rate_type"/>
 *         &lt;element name="dataBits" type="{}data_bits_type"/>
 *         &lt;element name="stopBits" type="{}stop_bits_type"/>
 *         &lt;element name="parity" type="{}parity_type"/>
 *         &lt;element name="flowControlMode" type="{}flow_control_type"/>
 *         &lt;element name="isRTS" type="{http://www.w3.org/2001/XMLSchema}boolean"/>
 *         &lt;element name="isDTR" type="{http://www.w3.org/2001/XMLSchema}boolean"/>
 *         &lt;sequence>
 *           &lt;element name="TimeOut" type="{}TimeOutType" minOccurs="0"/>
 *         &lt;/sequence>
 *         &lt;sequence>
 *           &lt;element name="DataBlock" type="{}DataBlockType" minOccurs="0"/>
 *         &lt;/sequence>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SerialPortType", propOrder = {
    "port",
    "baudeRate",
    "dataBits",
    "stopBits",
    "parity",
    "flowControlMode",
    "isRTS",
    "isDTR",
    "timeOut",
    "dataBlock"
})
public class SerialPortType {

    @XmlElement(required = true)
    protected String port;
    @XmlElement(required = true)
    protected BigInteger baudeRate;
    @XmlElement(required = true)
    protected BigInteger dataBits;
    @XmlElement(required = true)
    protected StopBitsType stopBits;
    @XmlElement(required = true)
    protected ParityType parity;
    @XmlElement(required = true)
    protected FlowControlType flowControlMode;
    protected boolean isRTS;
    protected boolean isDTR;
    @XmlElement(name = "TimeOut")
    protected TimeOutType timeOut;
    @XmlElement(name = "DataBlock")
    protected DataBlockType dataBlock;

    /**
     * Gets the value of the port property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getPort() {
        return port;
    }

    /**
     * Sets the value of the port property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setPort(String value) {
        this.port = value;
    }

    /**
     * Gets the value of the baudeRate property.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getBaudeRate() {
        return baudeRate;
    }

    /**
     * Sets the value of the baudeRate property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setBaudeRate(BigInteger value) {
        this.baudeRate = value;
    }

    /**
     * Gets the value of the dataBits property.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getDataBits() {
        return dataBits;
    }

    /**
     * Sets the value of the dataBits property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setDataBits(BigInteger value) {
        this.dataBits = value;
    }

    /**
     * Gets the value of the stopBits property.
     * 
     * @return
     *     possible object is
     *     {@link StopBitsType }
     *     
     */
    public StopBitsType getStopBits() {
        return stopBits;
    }

    /**
     * Sets the value of the stopBits property.
     * 
     * @param value
     *     allowed object is
     *     {@link StopBitsType }
     *     
     */
    public void setStopBits(StopBitsType value) {
        this.stopBits = value;
    }

    /**
     * Gets the value of the parity property.
     * 
     * @return
     *     possible object is
     *     {@link ParityType }
     *     
     */
    public ParityType getParity() {
        return parity;
    }

    /**
     * Sets the value of the parity property.
     * 
     * @param value
     *     allowed object is
     *     {@link ParityType }
     *     
     */
    public void setParity(ParityType value) {
        this.parity = value;
    }

    /**
     * Gets the value of the flowControlMode property.
     * 
     * @return
     *     possible object is
     *     {@link FlowControlType }
     *     
     */
    public FlowControlType getFlowControlMode() {
        return flowControlMode;
    }

    /**
     * Sets the value of the flowControlMode property.
     * 
     * @param value
     *     allowed object is
     *     {@link FlowControlType }
     *     
     */
    public void setFlowControlMode(FlowControlType value) {
        this.flowControlMode = value;
    }

    /**
     * Gets the value of the isRTS property.
     * 
     */
    public boolean isIsRTS() {
        return isRTS;
    }

    /**
     * Sets the value of the isRTS property.
     * 
     */
    public void setIsRTS(boolean value) {
        this.isRTS = value;
    }

    /**
     * Gets the value of the isDTR property.
     * 
     */
    public boolean isIsDTR() {
        return isDTR;
    }

    /**
     * Sets the value of the isDTR property.
     * 
     */
    public void setIsDTR(boolean value) {
        this.isDTR = value;
    }

    /**
     * Gets the value of the timeOut property.
     * 
     * @return
     *     possible object is
     *     {@link TimeOutType }
     *     
     */
    public TimeOutType getTimeOut() {
        return timeOut;
    }

    /**
     * Sets the value of the timeOut property.
     * 
     * @param value
     *     allowed object is
     *     {@link TimeOutType }
     *     
     */
    public void setTimeOut(TimeOutType value) {
        this.timeOut = value;
    }

    /**
     * Gets the value of the dataBlock property.
     * 
     * @return
     *     possible object is
     *     {@link DataBlockType }
     *     
     */
    public DataBlockType getDataBlock() {
        return dataBlock;
    }

    /**
     * Sets the value of the dataBlock property.
     * 
     * @param value
     *     allowed object is
     *     {@link DataBlockType }
     *     
     */
    public void setDataBlock(DataBlockType value) {
        this.dataBlock = value;
    }

}
