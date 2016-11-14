//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.1.5-b01-fcs 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2016.11.13 at 10:52:56 AM MEZ 
//


package gde.device;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for CalculationType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="CalculationType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;attribute name="transitionId" use="required" type="{http://www.w3.org/2001/XMLSchema}int" />
 *       &lt;attribute name="calcType" use="required" type="{}calculation_types" />
 *       &lt;attribute name="unsigned" use="required" type="{http://www.w3.org/2001/XMLSchema}boolean" />
 *       &lt;attribute name="refOrdinal" use="required" type="{http://www.w3.org/2001/XMLSchema}int" />
 *       &lt;attribute name="leveling" type="{}leveling_types" />
 *       &lt;attribute name="refOrdinalDivisor" type="{http://www.w3.org/2001/XMLSchema}int" />
 *       &lt;attribute name="divisorLeveling" type="{}leveling_types" />
 *       &lt;attribute name="basedOnRecovery" type="{http://www.w3.org/2001/XMLSchema}boolean" />
 *       &lt;attribute name="comment" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "CalculationType")
public class CalculationType {

    @XmlAttribute(required = true)
    protected int transitionId;
    @XmlAttribute(required = true)
    protected CalculationTypes calcType;
    @XmlAttribute(required = true)
    protected boolean unsigned;
    @XmlAttribute(required = true)
    protected int refOrdinal;
    @XmlAttribute
    protected LevelingTypes leveling;
    @XmlAttribute
    protected Integer refOrdinalDivisor;
    @XmlAttribute
    protected LevelingTypes divisorLeveling;
    @XmlAttribute
    protected Boolean basedOnRecovery;
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
     * Gets the value of the calcType property.
     * 
     * @return
     *     possible object is
     *     {@link CalculationTypes }
     *     
     */
    public CalculationTypes getCalcType() {
        return calcType;
    }

    /**
     * Sets the value of the calcType property.
     * 
     * @param value
     *     allowed object is
     *     {@link CalculationTypes }
     *     
     */
    public void setCalcType(CalculationTypes value) {
        this.calcType = value;
    }

    /**
     * Gets the value of the unsigned property.
     * 
     */
    public boolean isUnsigned() {
        return unsigned;
    }

    /**
     * Sets the value of the unsigned property.
     * 
     */
    public void setUnsigned(boolean value) {
        this.unsigned = value;
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
     * Gets the value of the leveling property.
     * 
     * @return
     *     possible object is
     *     {@link LevelingTypes }
     *     
     */
    public LevelingTypes getLeveling() {
        return leveling;
    }

    /**
     * Sets the value of the leveling property.
     * 
     * @param value
     *     allowed object is
     *     {@link LevelingTypes }
     *     
     */
    public void setLeveling(LevelingTypes value) {
        this.leveling = value;
    }

    /**
     * Gets the value of the refOrdinalDivisor property.
     * 
     * @return
     *     possible object is
     *     {@link Integer }
     *     
     */
    public Integer getRefOrdinalDivisor() {
        return refOrdinalDivisor;
    }

    /**
     * Sets the value of the refOrdinalDivisor property.
     * 
     * @param value
     *     allowed object is
     *     {@link Integer }
     *     
     */
    public void setRefOrdinalDivisor(Integer value) {
        this.refOrdinalDivisor = value;
    }

    /**
     * Gets the value of the divisorLeveling property.
     * 
     * @return
     *     possible object is
     *     {@link LevelingTypes }
     *     
     */
    public LevelingTypes getDivisorLeveling() {
        return divisorLeveling;
    }

    /**
     * Sets the value of the divisorLeveling property.
     * 
     * @param value
     *     allowed object is
     *     {@link LevelingTypes }
     *     
     */
    public void setDivisorLeveling(LevelingTypes value) {
        this.divisorLeveling = value;
    }

    /**
     * Gets the value of the basedOnRecovery property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isBasedOnRecovery() {
        return basedOnRecovery;
    }

    /**
     * Sets the value of the basedOnRecovery property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setBasedOnRecovery(Boolean value) {
        this.basedOnRecovery = value;
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
