/*
 * Copyright 2004,2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.axis2.saaj;

import org.apache.ws.commons.om.OMOutputFormat;
import org.apache.axis2.transport.http.HTTPConstants;

import javax.xml.soap.AttachmentPart;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPHeader;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPPart;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

public class SOAPMessageImpl extends SOAPMessage {

    private SOAPPart soapPart;
    private Collection attachmentParts = new ArrayList();
    private MimeHeadersEx mimeHeaders;

    private Map props = new Hashtable();

    public SOAPMessageImpl(SOAPEnvelopeImpl soapEnvelope) {
        String contentType = null;
        if (mimeHeaders != null) {
            String contentTypes[] = mimeHeaders.getHeader("Content-Type");
            contentType = (contentTypes != null) ? contentTypes[0] : null;
        }

        setCharsetEncoding(contentType);

        soapPart = new SOAPPartImpl(this, soapEnvelope);

        this.mimeHeaders = new MimeHeadersEx();
    }

    public SOAPMessageImpl(InputStream inputstream,
                           javax.xml.soap.MimeHeaders mimeHeaders) throws SOAPException {
        String contentType = null;
        if (mimeHeaders != null) {
            String contentTypes[] = mimeHeaders.getHeader("Content-Type");
            contentType = (contentTypes != null) ? contentTypes[0] : null;
        }

        setCharsetEncoding(contentType);
        soapPart = new SOAPPartImpl(this, inputstream);

        this.mimeHeaders = (mimeHeaders == null) ?
                           new MimeHeadersEx() :
                           new MimeHeadersEx(mimeHeaders);
    }

    /**
     * Retrieves a description of this <CODE>SOAPMessage</CODE>
     * object's content.
     *
     * @return a <CODE>String</CODE> describing the content of this
     *         message or <CODE>null</CODE> if no description has been
     *         set
     * @see #setContentDescription(java.lang.String) setContentDescription(java.lang.String)
     */
    public String getContentDescription() {
        String values[] = mimeHeaders.getHeader(HTTPConstants.HEADER_CONTENT_DESCRIPTION);
        if (values != null && values.length > 0) {
            return values[0];
        }
        return null;
    }

    /**
     * Sets the description of this <CODE>SOAPMessage</CODE>
     * object's content with the given description.
     *
     * @param description a <CODE>String</CODE>
     *                    describing the content of this message
     * @see #getContentDescription() getContentDescription()
     */
    public void setContentDescription(String description) {
        mimeHeaders.setHeader(HTTPConstants.HEADER_CONTENT_DESCRIPTION, description);
    }

    /**
     * Gets the SOAP part of this <CODE>SOAPMessage</CODE> object.
     * <p/>
     * <p/>
     * <P>If a <CODE>SOAPMessage</CODE> object contains one or
     * more attachments, the SOAP Part must be the first MIME body
     * part in the message.</P>
     *
     * @return the <CODE>SOAPPart</CODE> object for this <CODE>
     *         SOAPMessage</CODE> object
     */
    public SOAPPart getSOAPPart() {
        return soapPart;
    }

    /**
     * Removes all <CODE>AttachmentPart</CODE> objects that have
     * been added to this <CODE>SOAPMessage</CODE> object.
     * <p/>
     * <P>This method does not touch the SOAP part.</P>
     */
    public void removeAllAttachments() {
        attachmentParts.clear();
    }

    /**
     * Gets a count of the number of attachments in this
     * message. This count does not include the SOAP part.
     *
     * @return the number of <CODE>AttachmentPart</CODE> objects
     *         that are part of this <CODE>SOAPMessage</CODE>
     *         object
     */
    public int countAttachments() {
        return attachmentParts.size();
    }

    /**
     * Retrieves all the <CODE>AttachmentPart</CODE> objects
     * that are part of this <CODE>SOAPMessage</CODE> object.
     *
     * @return an iterator over all the attachments in this
     *         message
     */
    public Iterator getAttachments() {
        return attachmentParts.iterator();
    }

    /**
     * Retrieves all the AttachmentPart objects that have header entries that match the specified
     * headers.
     * Note that a returned attachment could have headers in addition to those specified.
     *
     * @param headers a {@link javax.xml.soap.MimeHeaders}
     *                object containing the MIME headers for which to search
     * @return an iterator over all attachments({@link javax.xml.soap.AttachmentPart})
     *         that have a header that matches one of the given headers
     */
    public Iterator getAttachments(javax.xml.soap.MimeHeaders headers) {
        Collection matchingAttachmentParts = new ArrayList();
        Iterator iterator = getAttachments();
        {
            AttachmentPartImpl part;
            while (iterator.hasNext()) {
                part = (AttachmentPartImpl) iterator.next();
                if (part.matches(headers)) {
                    matchingAttachmentParts.add(part);
                }
            }
        }
        return matchingAttachmentParts.iterator();
    }

    /**
     * Adds the given <CODE>AttachmentPart</CODE> object to this
     * <CODE>SOAPMessage</CODE> object. An <CODE>
     * AttachmentPart</CODE> object must be created before it can be
     * added to a message.
     *
     * @param attachmentPart an <CODE>
     *                       AttachmentPart</CODE> object that is to become part of
     *                       this <CODE>SOAPMessage</CODE> object
     * @throws java.lang.IllegalArgumentException
     *
     */
    public void addAttachmentPart(AttachmentPart attachmentPart) {
        if (attachmentPart != null) {
            attachmentParts.add(attachmentPart);
            mimeHeaders.setHeader("Content-Type", "multipart/related");
        }
    }

    /**
     * Creates a new empty <CODE>AttachmentPart</CODE> object.
     * Note that the method <CODE>addAttachmentPart</CODE> must be
     * called with this new <CODE>AttachmentPart</CODE> object as
     * the parameter in order for it to become an attachment to this
     * <CODE>SOAPMessage</CODE> object.
     *
     * @return a new <CODE>AttachmentPart</CODE> object that can be
     *         populated and added to this <CODE>SOAPMessage</CODE>
     *         object
     */
    public AttachmentPart createAttachmentPart() {
        return new AttachmentPartImpl();
    }

    /**
     * Returns all the transport-specific MIME headers for this
     * <CODE>SOAPMessage</CODE> object in a transport-independent
     * fashion.
     *
     * @return a <CODE>MimeHeaders</CODE> object containing the
     *         <CODE>MimeHeader</CODE> objects
     */
    public javax.xml.soap.MimeHeaders getMimeHeaders() {
        return mimeHeaders;
    }

    /**
     * Updates this <CODE>SOAPMessage</CODE> object with all the
     * changes that have been made to it. This method is called
     * automatically when a message is sent or written to by the
     * methods <CODE>ProviderConnection.send</CODE>, <CODE>
     * SOAPConnection.call</CODE>, or <CODE>
     * SOAPMessage.writeTo</CODE>. However, if changes are made to
     * a message that was received or to one that has already been
     * sent, the method <CODE>saveChanges</CODE> needs to be
     * called explicitly in order to save the changes. The method
     * <CODE>saveChanges</CODE> also generates any changes that
     * can be read back (for example, a MessageId in profiles that
     * support a message id). All MIME headers in a message that
     * is created for sending purposes are guaranteed to have
     * valid values only after <CODE>saveChanges</CODE> has been
     * called.
     * <p/>
     * <P>In addition, this method marks the point at which the
     * data from all constituent <CODE>AttachmentPart</CODE>
     * objects are pulled into the message.</P>
     *
     * @throws SOAPException if there was a problem saving changes to this message.
     */
    public void saveChanges() throws SOAPException {
        // TODO not sure of the implementation
//        throw new UnsupportedOperationException("TODO");
    }

    /**
     * Indicates whether this <CODE>SOAPMessage</CODE> object
     * has had the method {@link #saveChanges()} called on
     * it.
     *
     * @return <CODE>true</CODE> if <CODE>saveChanges</CODE> has
     *         been called on this message at least once; <CODE>
     *         false</CODE> otherwise.
     */
    public boolean saveRequired() {
        return false;
    }

    /**
     * Writes this <CODE>SOAPMessage</CODE> object to the given
     * output stream. The externalization format is as defined by
     * the SOAP 1.1 with Attachments specification.
     * <p/>
     * <P>If there are no attachments, just an XML stream is
     * written out. For those messages that have attachments,
     * <CODE>writeTo</CODE> writes a MIME-encoded byte stream.</P>
     *
     * @param out the <CODE>OutputStream</CODE>
     *            object to which this <CODE>SOAPMessage</CODE> object will
     *            be written
     * @throws SOAPException if there was a problem in externalizing this SOAP message
     * @throws IOException   if an I/O error occurs
     */
    public void writeTo(OutputStream out) throws SOAPException, IOException {
        try {
            OMOutputFormat format = new OMOutputFormat();
            format.setCharSetEncoding((String) getProperty(CHARACTER_SET_ENCODING));
            String writeXmlDecl = (String) getProperty(WRITE_XML_DECLARATION);
            if (writeXmlDecl == null || writeXmlDecl.equals("false")) {

                //SAAJ default case doesn't send XML decl
                format.setIgnoreXMLDeclaration(true);
            }

            //the writeTo method forces the elements to be built!!!
            ((SOAPEnvelopeImpl) soapPart.getEnvelope()).getOMEnvelope().serialize(out, format);
        } catch (Exception e) {
            e.printStackTrace();
            throw new SOAPException(e);
        }
    }

    /**
     * Associates the specified value with the specified property. If there was
     * already a value associated with this property, the old value is replaced.
     * <p/>
     * The valid property names include <code>WRITE_XML_DECLARATION</code> and
     * <code>CHARACTER_SET_ENCODING</code>. All of these standard SAAJ
     * properties are prefixed by "javax.xml.soap". Vendors may also add
     * implementation specific properties. These properties must be prefixed
     * with package names that are unique to the vendor.
     * <p/>
     * Setting the property <code>WRITE_XML_DECLARATION</code> to
     * <code>"true"</code> will cause an XML Declaration to be written out at
     * the start of the SOAP message. The default value of "false" suppresses
     * this declaration.
     * <p/>
     * The property <code>CHARACTER_SET_ENCODING</code> defaults to the value
     * <code>"utf-8"</code> which causes the SOAP message to be encoded using
     * UTF-8. Setting <code>CHARACTER_SET_ENCODING</code> to
     * <code>"utf-16"</code> causes the SOAP message to be encoded using UTF-16.
     * <p/>
     * Some implementations may allow encodings in addition to UTF-8 and UTF-16.
     * Refer to your vendor's documentation for details.
     *
     * @param property the property with which the specified value is to be
     *                 associated
     * @param value    the value
     *                 to be associated with the specified property
     */
    public void setProperty(String property, Object value) {
        props.put(property, value);
    }

    /**
     * Retrieves value of the specified property.
     *
     * @param property the name of the property to retrieve
     * @return the value of the property or <code>null</code> if no such
     *         property exists
     * @throws SOAPException if the property name is not recognized
     */
    public Object getProperty(String property) throws SOAPException {
        return props.get(property);
    }

    /**
     * Gets the SOAP Header contained in this <code>SOAPMessage</code> object.
     *
     * @return the <code>SOAPHeader</code> object contained by this
     *         <code>SOAPMessage</code> object
     * @throws javax.xml.soap.SOAPException if the SOAP Header does not exist or cannot be
     *                                      retrieved
     */
    public SOAPHeader getSOAPHeader() throws SOAPException {
        return this.soapPart.getEnvelope().getHeader();
    }

    /**
     * Gets the SOAP Body contained in this <code>SOAPMessage</code> object.
     *
     * @return the <code>SOAPBody</code> object contained by this
     *         <code>SOAPMessage</code> object
     * @throws javax.xml.soap.SOAPException if the SOAP Body does not exist or cannot be
     *                                      retrieved
     */
    public SOAPBody getSOAPBody() throws SOAPException {
        return this.soapPart.getEnvelope().getBody();
    }

    /**
     * Set the character encoding based on the <code>contentType</code> parameter
     *
     * @param contentType
     */
    private void setCharsetEncoding(final String contentType) {
        if (contentType != null) {
            int delimiterIndex = contentType.lastIndexOf("charset");
            if (delimiterIndex > 0) {
                String charsetPart = contentType.substring(delimiterIndex);
                int charsetIndex = charsetPart.indexOf('=');
                String charset = charsetPart.substring(charsetIndex + 1).trim();
                if ((charset.startsWith("\"") || charset.startsWith("\'"))) {
                    charset = charset.substring(1, charset.length());
                }
                if ((charset.endsWith("\"") || charset.endsWith("\'"))) {
                    charset = charset.substring(0, charset.length() - 1);
                }
                setProperty(SOAPMessage.CHARACTER_SET_ENCODING, charset);
            }
        }
    }
}
