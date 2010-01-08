//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.1.5-b01-fcs 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2010.01.08 at 05:54:19 PM GMT+01:00 
//


package osde.device;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for device_types.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="device_types">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="undefined"/>
 *     &lt;enumeration value="logger"/>
 *     &lt;enumeration value="charger"/>
 *     &lt;enumeration value="multimeter"/>
 *     &lt;enumeration value="variometer"/>
 *     &lt;enumeration value="flightrecorder"/>
 *     &lt;enumeration value="balancer"/>
 *     &lt;enumeration value="current-sink"/>
 *     &lt;enumeration value="receiver"/>
 *     &lt;enumeration value="global-positioning-system"/>
 *     &lt;enumeration value="power-supply"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "device_types")
@XmlEnum
public enum DeviceTypes {

    @XmlEnumValue("undefined")
    UNDEFINED("undefined"),
    @XmlEnumValue("logger")
    LOGGER("logger"),
    @XmlEnumValue("charger")
    CHARGER("charger"),
    @XmlEnumValue("multimeter")
    MULTIMETER("multimeter"),
    @XmlEnumValue("variometer")
    VARIOMETER("variometer"),
    @XmlEnumValue("flightrecorder")
    FLIGHTRECORDER("flightrecorder"),
    @XmlEnumValue("balancer")
    BALANCER("balancer"),
    @XmlEnumValue("current-sink")
    CURRENT_SINK("current-sink"),
    @XmlEnumValue("receiver")
    RECEIVER("receiver"),
    @XmlEnumValue("global-positioning-system")
    GLOBAL_POSITIONING_SYSTEM("global-positioning-system"),
    @XmlEnumValue("power-supply")
    POWER_SUPPLY("power-supply");
    private final String value;

    DeviceTypes(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static DeviceTypes fromValue(String v) {
        for (DeviceTypes c: DeviceTypes.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
