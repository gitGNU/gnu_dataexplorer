//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.1.5-b01-fcs 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2016.11.17 at 01:25:06 PM MEZ 
//


package gde.histocache;

import javax.xml.bind.annotation.XmlRegistry;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the gde.histocache package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {


    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: gde.histocache
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link Entries }
     * 
     */
    public Entries createEntries() {
        return new Entries();
    }

    /**
     * Create an instance of {@link HistoVault }
     * 
     */
    public HistoVault createHistoVault() {
        return new HistoVault();
    }

    /**
     * Create an instance of {@link Point }
     * 
     */
    public Point createPoint() {
        return new Point();
    }

    /**
     * Create an instance of {@link EntryPoints }
     * 
     */
    public EntryPoints createEntryPoints() {
        return new EntryPoints();
    }

}