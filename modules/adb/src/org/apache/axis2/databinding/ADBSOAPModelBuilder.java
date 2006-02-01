package org.apache.axis2.databinding;

import org.apache.ws.commons.soap.SOAPConstants;
import org.apache.ws.commons.soap.SOAPEnvelope;
import org.apache.ws.commons.soap.SOAPFactory;
import org.apache.ws.commons.soap.impl.llom.builder.StAXSOAPModelBuilder;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamReader;

/**
 * Builds a SOAPEnvelope around an ADB pull parser
 */
public class ADBSOAPModelBuilder extends StAXSOAPModelBuilder {
    public ADBSOAPModelBuilder(XMLStreamReader parser, SOAPFactory factory) {
        super(new Envelope(parser).getPullParser(new QName(factory.getSoapVersionURI(), SOAPConstants.SOAPENVELOPE_LOCAL_NAME, SOAPConstants.SOAP_DEFAULT_NAMESPACE_PREFIX)),
                factory,
                factory.getSoapVersionURI());
    }

    public SOAPEnvelope getEnvelope() {
        return getSOAPEnvelope();
    }

    public static class Envelope
            implements org.apache.axis2.databinding.ADBBean {
        Body body;

        Envelope(XMLStreamReader parser) {
            body = new Body(parser);
        }

        public javax.xml.stream.XMLStreamReader getPullParser(javax.xml.namespace.QName qName) {
            java.util.ArrayList elementList = new java.util.ArrayList();
            elementList.add(new QName(qName.getNamespaceURI(), "Header", SOAPConstants.BODY_NAMESPACE_PREFIX));
            elementList.add(new Header());
            elementList.add(new QName(qName.getNamespaceURI(), "Body", SOAPConstants.BODY_NAMESPACE_PREFIX));
            elementList.add(body);
            return org.apache.axis2.databinding.utils.ADBPullParser.createPullParser(qName, elementList.toArray(), null);
        }
    }
    
    protected void identifySOAPVersion(String soapVersionURIFromTransport) {
        //Do nothing
    }

    public static class Body
            implements org.apache.axis2.databinding.ADBBean {
        Child child;

        Body(XMLStreamReader parser) {
            child = new Child(parser);
        }

        public javax.xml.stream.XMLStreamReader getPullParser(javax.xml.namespace.QName qName) {
            java.util.ArrayList elementList = new java.util.ArrayList();
            elementList.add(qName);
            elementList.add(child);
            return org.apache.axis2.databinding.utils.ADBPullParser.createPullParser(qName, elementList.toArray(), null);
        }
    }

    public static class Header
            implements org.apache.axis2.databinding.ADBBean {
        public javax.xml.stream.XMLStreamReader getPullParser(javax.xml.namespace.QName qName) {
            java.util.ArrayList elementList = new java.util.ArrayList();
            return org.apache.axis2.databinding.utils.ADBPullParser.createPullParser(qName, elementList.toArray(), null);
        }
    }

    public static class Child
            implements org.apache.axis2.databinding.ADBBean {
        XMLStreamReader parser;

        Child(XMLStreamReader parser) {
            this.parser = parser;
        }

        public javax.xml.stream.XMLStreamReader getPullParser(javax.xml.namespace.QName qName) {
            return parser;
        }
    }
}
