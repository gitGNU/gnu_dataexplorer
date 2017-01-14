//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.1.5-b01-fcs 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2017.01.21 at 01:09:13 PM MEZ 
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
 *       &lt;attribute name="classType" use="required" type="{}transition_class_types" />
 *       &lt;attribute name="valueType" use="required" type="{}transition_value_types" />
 *       &lt;attribute name="thresholdValue" use="required" type="{http://www.w3.org/2001/XMLSchema}double" />
 *       &lt;attribute name="recoveryValue" type="{http://www.w3.org/2001/XMLSchema}double" />
 *       &lt;attribute name="referenceTimeMsec" use="required" type="{http://www.w3.org/2001/XMLSchema}int" />
 *       &lt;attribute name="thresholdTimeMsec" use="required" type="{http://www.w3.org/2001/XMLSchema}int" />
 *       &lt;attribute name="recoveryTimeMsec" type="{http://www.w3.org/2001/XMLSchema}int" />
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
    protected TransitionClassTypes classType;
    @XmlAttribute(required = true)
    protected TransitionValueTypes valueType;
    @XmlAttribute(required = true)
    protected double thresholdValue;
    @XmlAttribute
    protected Double recoveryValue;
    @XmlAttribute(required = true)
    protected int referenceTimeMsec;
    @XmlAttribute(required = true)
    protected int thresholdTimeMsec;
    @XmlAttribute
    protected Integer recoveryTimeMsec;
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
     * Gets the value of the classType property.
     * 
     * @return
     *     possible object is
     *     {@link TransitionClassTypes }
     *     
     */
    public TransitionClassTypes getClassType() {
        return classType;
    }

    /**
     * Sets the value of the classType property.
     * 
     * @param value
     *     allowed object is
     *     {@link TransitionClassTypes }
     *     
     */
    public void setClassType(TransitionClassTypes value) {
        this.classType = value;
    }

    /**
     * Gets the value of the valueType property.
     * 
     * @return
     *     possible object is
     *     {@link TransitionValueTypes }
     *     
     */
    public TransitionValueTypes getValueType() {
        return valueType;
    }

    /**
     * Sets the value of the valueType property.
     * 
     * @param value
     *     allowed object is
     *     {@link TransitionValueTypes }
     *     
     */
    public void setValueType(TransitionValueTypes value) {
        this.valueType = value;
    }

    /**
     * Gets the value of the thresholdValue property.
     * 
     */
    public double getThresholdValue() {
        return thresholdValue;
    }

    /**
     * Sets the value of the thresholdValue property.
     * 
     */
    public void setThresholdValue(double value) {
        this.thresholdValue = value;
    }

    /**
     * Gets the value of the recoveryValue property.
     * 
     * @return
     *     possible object is
     *     {@link Double }
     *     
     */
    public Double getRecoveryValue() {
        return recoveryValue;
    }

    /**
     * Sets the value of the recoveryValue property.
     * 
     * @param value
     *     allowed object is
     *     {@link Double }
     *     
     */
    public void setRecoveryValue(Double value) {
        this.recoveryValue = value;
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
     * @return
     *     possible object is
     *     {@link Integer }
     *     
     */
    public Integer getRecoveryTimeMsec() {
        return recoveryTimeMsec;
    }

    /**
     * Sets the value of the recoveryTimeMsec property.
     * 
     * @param value
     *     allowed object is
     *     {@link Integer }
     *     
     */
    public void setRecoveryTimeMsec(Integer value) {
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
