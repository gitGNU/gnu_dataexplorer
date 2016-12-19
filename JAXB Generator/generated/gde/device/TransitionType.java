//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.1.5-b01-fcs 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2016.12.15 at 08:43:18 AM MEZ 
//


package gde.device;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for TransitionType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="TransitionType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;attribute name="transitionId" use="required" type="{http://www.w3.org/2001/XMLSchema}int" />
 *       &lt;attribute name="refOrdinal" use="required" type="{http://www.w3.org/2001/XMLSchema}int" />
 *       &lt;attribute name="peak" use="required" type="{http://www.w3.org/2001/XMLSchema}boolean" />
 *       &lt;attribute name="greater" use="required" type="{http://www.w3.org/2001/XMLSchema}boolean" />
 *       &lt;attribute name="percent" use="required" type="{http://www.w3.org/2001/XMLSchema}boolean" />
 *       &lt;attribute name="triggerLevel" use="required" type="{http://www.w3.org/2001/XMLSchema}int" />
 *       &lt;attribute name="recoveryLevel" use="required" type="{http://www.w3.org/2001/XMLSchema}int" />
 *       &lt;attribute name="referenceTimeMsec" use="required" type="{http://www.w3.org/2001/XMLSchema}int" />
 *       &lt;attribute name="thresholdTimeMsec" use="required" type="{http://www.w3.org/2001/XMLSchema}int" />
 *       &lt;attribute name="recoveryTimeMsec" use="required" type="{http://www.w3.org/2001/XMLSchema}int" />
 *       &lt;attribute name="comment" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "TransitionType")
public class TransitionType {

    @XmlAttribute(required = true)
    protected int transitionId;
    @XmlAttribute(required = true)
    protected int refOrdinal;
    @XmlAttribute(required = true)
    protected boolean peak;
    @XmlAttribute(required = true)
    protected boolean greater;
    @XmlAttribute(required = true)
    protected boolean percent;
    @XmlAttribute(required = true)
    protected int triggerLevel;
    @XmlAttribute(required = true)
    protected int recoveryLevel;
    @XmlAttribute(required = true)
    protected int referenceTimeMsec;
    @XmlAttribute(required = true)
    protected int thresholdTimeMsec;
    @XmlAttribute(required = true)
    protected int recoveryTimeMsec;
    @XmlAttribute
    protected String comment;

    /**
     * Gets the value of the transitionId property.
     * 
     */
    public int getTransitionId() {
        return transitionId;
    }

    /**
     * Sets the value of the transitionId property.
     * 
     */
    public void setTransitionId(int value) {
        this.transitionId = value;
    }

    /**
     * Gets the value of the refOrdinal property.
     * 
     */
    public int getRefOrdinal() {
        return refOrdinal;
    }

    /**
     * Sets the value of the refOrdinal property.
     * 
     */
    public void setRefOrdinal(int value) {
        this.refOrdinal = value;
    }

    /**
     * Gets the value of the peak property.
     * 
     */
    public boolean isPeak() {
        return peak;
    }

    /**
     * Sets the value of the peak property.
     * 
     */
    public void setPeak(boolean value) {
        this.peak = value;
    }

    /**
     * Gets the value of the greater property.
     * 
     */
    public boolean isGreater() {
        return greater;
    }

    /**
     * Sets the value of the greater property.
     * 
     */
    public void setGreater(boolean value) {
        this.greater = value;
    }

    /**
     * Gets the value of the percent property.
     * 
     */
    public boolean isPercent() {
        return percent;
    }

    /**
     * Sets the value of the percent property.
     * 
     */
    public void setPercent(boolean value) {
        this.percent = value;
    }

    /**
     * Gets the value of the triggerLevel property.
     * 
     */
    public int getTriggerLevel() {
        return triggerLevel;
    }

    /**
     * Sets the value of the triggerLevel property.
     * 
     */
    public void setTriggerLevel(int value) {
        this.triggerLevel = value;
    }

    /**
     * Gets the value of the recoveryLevel property.
     * 
     */
    public int getRecoveryLevel() {
        return recoveryLevel;
    }

    /**
     * Sets the value of the recoveryLevel property.
     * 
     */
    public void setRecoveryLevel(int value) {
        this.recoveryLevel = value;
    }

    /**
     * Gets the value of the referenceTimeMsec property.
     * 
     */
    public int getReferenceTimeMsec() {
        return referenceTimeMsec;
    }

    /**
     * Sets the value of the referenceTimeMsec property.
     * 
     */
    public void setReferenceTimeMsec(int value) {
        this.referenceTimeMsec = value;
    }

    /**
     * Gets the value of the thresholdTimeMsec property.
     * 
     */
    public int getThresholdTimeMsec() {
        return thresholdTimeMsec;
    }

    /**
     * Sets the value of the thresholdTimeMsec property.
     * 
     */
    public void setThresholdTimeMsec(int value) {
        this.thresholdTimeMsec = value;
    }

    /**
     * Gets the value of the recoveryTimeMsec property.
     * 
     */
    public int getRecoveryTimeMsec() {
        return recoveryTimeMsec;
    }

    /**
     * Sets the value of the recoveryTimeMsec property.
     * 
     */
    public void setRecoveryTimeMsec(int value) {
        this.recoveryTimeMsec = value;
    }

    /**
     * Gets the value of the comment property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getComment() {
        return comment;
    }

    /**
     * Sets the value of the comment property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setComment(String value) {
        this.comment = value;
    }

}
