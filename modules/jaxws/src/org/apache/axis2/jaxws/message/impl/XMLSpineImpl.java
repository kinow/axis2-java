/*
 * Copyright 2004,2005 The Apache Software Foundation.
 * Copyright 2006 International Business Machines Corp.
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
package org.apache.axis2.jaxws.message.impl;

import java.util.Iterator;

import javax.jws.soap.SOAPBinding.Style;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.ws.WebServiceException;

import org.apache.axiom.om.OMContainer;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMNamespace;
import org.apache.axiom.om.OMNode;
import org.apache.axiom.om.impl.OMContainerEx;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.axiom.om.impl.llom.OMElementImpl;
import org.apache.axiom.om.impl.llom.OMSourcedElementImpl;
import org.apache.axiom.soap.SOAP11Constants;
import org.apache.axiom.soap.SOAP12Constants;
import org.apache.axiom.soap.SOAPBody;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.axiom.soap.SOAPFault;
import org.apache.axiom.soap.SOAPFaultDetail;
import org.apache.axiom.soap.SOAPHeader;
import org.apache.axiom.soap.impl.llom.soap11.SOAP11Factory;
import org.apache.axiom.soap.impl.llom.soap12.SOAP12Factory;
import org.apache.axis2.jaxws.ExceptionFactory;
import org.apache.axis2.jaxws.i18n.Messages;
import org.apache.axis2.jaxws.message.Block;
import org.apache.axis2.jaxws.message.Message;
import org.apache.axis2.jaxws.message.Protocol;
import org.apache.axis2.jaxws.message.XMLFault;
import org.apache.axis2.jaxws.message.factory.BlockFactory;
import org.apache.axis2.jaxws.message.factory.OMBlockFactory;
import org.apache.axis2.jaxws.message.util.MessageUtils;
import org.apache.axis2.jaxws.message.util.Reader2Writer;
import org.apache.axis2.jaxws.message.util.XMLFaultUtils;
import org.apache.axis2.jaxws.registry.FactoryRegistry;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * XMLSpineImpl
 * 
 * An XMLSpine consists is an OMEnvelope (either a default one or one create from an incoming message).
 * As Blocks are added or requested, they are placed in the tree as OMSourcedElements.
 *
 */
class XMLSpineImpl implements XMLSpine {
	
    private static Log log = LogFactory.getLog(XMLSpine.class);
	private static OMBlockFactory obf = (OMBlockFactory) FactoryRegistry.getFactory(OMBlockFactory.class);
	
	private Protocol protocol = Protocol.unknown;
    private Style style = Style.DOCUMENT;
    private int indirection = 0;
	private SOAPEnvelope root = null;
	private SOAPFactory soapFactory = null;
	
	private boolean consumed = false;
    private Message parent = null;
    
	/**
	 * Create a lightweight representation of this protocol
	 * (i.e. the Envelope, Header and Body)
     * @param protocol Protocol
     * @param style Style
     * @param indirection (0 or 1) indicates location of body blocks
     * @param opQName QName if the Style is RPC
	 */
	public XMLSpineImpl(Protocol protocol, Style style, int indirection) {
		super();
		this.protocol = protocol;
        this.style = style;
        this.indirection = indirection;
		soapFactory = _getFactory(protocol);
		root = _createEmptyEnvelope(protocol, style, soapFactory);
	}
	
	/**
	 * Create spine from an existing OM tree
	 * @param envelope
     * @param style Style
     * @param indirection (0 or 1) indicates location of body blocks
	 * @throws WebServiceException
	 */
	public XMLSpineImpl(SOAPEnvelope envelope, Style style, int indirection) throws WebServiceException {
		super();
        this.style = style;
        this.indirection = indirection;
		init(envelope);
		if (root.getNamespace().getNamespaceURI().equals(SOAP11Constants.SOAP_ENVELOPE_NAMESPACE_URI)) {
			protocol = Protocol.soap11;
		} else if (root.getNamespace().getNamespaceURI().equals(SOAP12Constants.SOAP_ENVELOPE_NAMESPACE_URI)) {
			protocol = Protocol.soap12;
		} else {
			// TODO Support for REST
			throw ExceptionFactory.makeWebServiceException(Messages.getMessage("RESTIsNotSupported"));
		}
	} 

	/**
	 * @param envelope
	 * @throws WebServiceException
	 */
	private void init(SOAPEnvelope envelope) throws WebServiceException {
		root = envelope;
        soapFactory = MessageUtils.getSOAPFactory(root);
		
        // Advance past the header
		SOAPHeader header = root.getHeader();
		if (header == null) {
            header = soapFactory.createSOAPHeader(root);
        }

		// Now advance the parser to the body element
		SOAPBody body = root.getBody();
        if (body == null) {
            // Create the body if one does not exist
            body = soapFactory.createSOAPBody(root);
        }
	}
	
    
    
	/* (non-Javadoc)
	 * @see org.apache.axis2.jaxws.message.XMLPart#getProtocol()
	 */
	public Protocol getProtocol() {
		return protocol;
	}
    
    /*
     * (non-Javadoc)
     * @see org.apache.axis2.jaxws.message.XMLPart#getParent()
     */
    public Message getParent() {
        return parent;
    }
    
    /*
     * Set the backpointer to this XMLPart's parent Message
     */
    public void setParent(Message p) {
        parent = p;
    }

	/* (non-Javadoc)
	 * @see org.apache.axis2.jaxws.message.XMLPart#outputTo(javax.xml.stream.XMLStreamWriter, boolean)
	 */
	public void outputTo(XMLStreamWriter writer, boolean consume) throws XMLStreamException, WebServiceException {
		Reader2Writer r2w = new Reader2Writer(getXMLStreamReader(consume));
		r2w.outputTo(writer);
	}

	public XMLStreamReader getXMLStreamReader(boolean consume) throws WebServiceException {
		if (consume) {
            if (root.getBuilder() != null && !root.getBuilder().isCompleted()) {
                return root.getXMLStreamReaderWithoutCaching();
            } else {
               return root.getXMLStreamReader();
            }
        } else {
            return root.getXMLStreamReader();
        }
	}

	/* (non-Javadoc)
	 * @see org.apache.axis2.jaxws.message.impl.XMLSpine#getXMLFault()
	 */
	public XMLFault getXMLFault() throws WebServiceException {
		if (!isFault()) {
		    return null;
        }
        
        // Advance through all of the detail blocks
        int numDetailBlocks = getNumDetailBlocks();
        
        Block[] blocks = null;
        if (numDetailBlocks > 0) {
            blocks = new Block[numDetailBlocks];
            SOAPFaultDetail detail = root.getBody().getFault().getDetail();
            for (int i=0; i<numDetailBlocks; i++) {
                OMElement om = this._getChildOMElement(detail, i);
                blocks[i] = this._getBlockFromOMElement(om, null, obf, false);
                
            }
        }
        
        XMLFault xmlFault = XMLFaultUtils.createXMLFault(root.getBody().getFault(), blocks);
        return xmlFault;
	}
    
    private int getNumDetailBlocks() throws WebServiceException {
        if (isFault()) {
            SOAPFault fault = root.getBody().getFault();
            return _getNumChildElements(fault.getDetail());
        } 
        return 0;
    }
	
	public void setXMLFault(XMLFault xmlFault) throws WebServiceException {
        
        // Clear out the existing body and detail blocks
        SOAPBody body = root.getBody();
        getNumDetailBlocks(); // Forces parse of existing detail blocks
        getNumBodyBlocks();  // Forces parse over body
        OMNode child = body.getFirstOMChild();
        while (child != null) {
            child.detach();
            child = body.getFirstOMChild();
        }
        
	    // Add a SOAPFault to the body.
        SOAPFault soapFault =XMLFaultUtils.createSOAPFault(xmlFault, body, false);
	}

	public boolean isConsumed() {
		return consumed;
	}

	public OMElement getAsOMElement() throws WebServiceException {
        return root;
    }
	 

	/* (non-Javadoc)
	 * @see org.apache.axis2.jaxws.message.XMLPart#getNumBodyBlocks()
	 */
	public int getNumBodyBlocks() throws WebServiceException {
        return _getNumChildElements(_getBodyBlockParent());
	}
    
    
    
	/* (non-Javadoc)
	 * @see org.apache.axis2.jaxws.message.impl.XMLSpine#getBodyBlock(int, java.lang.Object, org.apache.axis2.jaxws.message.factory.BlockFactory)
	 */
    public Block getBodyBlock(int index, Object context, BlockFactory blockFactory) throws WebServiceException {
        
        if (log.isDebugEnabled()) {
            log.debug("getBodyBlock: Get the " + index + "block using the block factory, " + blockFactory);
        }
        
        // Forces the parser to read all of the blocks
        getNumBodyBlocks();
        
        // Get the indicated block
        OMElement omElement = _getChildOMElement(_getBodyBlockParent(), index);
        if (omElement == null) {
            // Null indicates that no block is available
            if (log.isDebugEnabled()) {
                log.debug("getBodyBlock: The block was not found " );
            }
            return null;
        }
        if (log.isDebugEnabled()) {
            log.debug("getBodyBlock: Found omElement " + omElement.getQName() );
        }
        return this._getBlockFromOMElement(omElement, context, blockFactory, false);
    }
    
    /* (non-Javadoc)
     * @see org.apache.axis2.jaxws.message.impl.XMLSpine#getBodyBlock(int, java.lang.Object, org.apache.axis2.jaxws.message.factory.BlockFactory)
     */
    public Block getBodyBlock(Object context, BlockFactory blockFactory) throws WebServiceException {
        
        if (log.isDebugEnabled()) {
            log.debug("getBodyBlock PERFORMANT: Get the block using the block factory, " + blockFactory);
        }
        
        // TODO Need to upgrade the code to get Blocks that represent text and elements.
        
        // Calling getBodyBlock assumes that there is only one or zero body blocks in the message.  Subsequent
        // Blocks are lost.  If the caller needs access to multiple body blocks, then getBodyBlocks(index,...) should be used
        
        // Get the indicated block
        OMElement omElement = _getChildOMElement(_getBodyBlockParent(), 0);
        if (omElement == null) {
            // Null indicates that no block is available
            if (log.isDebugEnabled()) {
                log.debug("getBodyBlock: The block was not found " );
            }
            return null;
        }
        if (log.isDebugEnabled()) {
            log.debug("getBodyBlock: Found omElement " + omElement.getQName() );
        }
        return this._getBlockFromOMElement(omElement, context, blockFactory, true);
    }

	public void setBodyBlock(int index, Block block) throws WebServiceException {
        
	    // Forces the parser to read all of the blocks
        getNumBodyBlocks();
        
        block.setParent(getParent());
        OMElement bElement = _getBodyBlockParent();
        OMElement om = this._getChildOMElement(bElement, index);
        
        // The block is supposed to represent a single element.  
        // But if it does not represent an element , the following will fail.
        QName qName = block.getQName();
        
        OMElement newOM = _createOMElementFromBlock(qName, block, soapFactory);
        if (om == null) {
           bElement.addChild(newOM);
        } else {
            om.insertSiblingBefore(newOM);
            om.detach();
        }
	}
    
	public void setBodyBlock(Block block) throws WebServiceException {
        
	    // Forces the parser to read all of the blocks
        getNumBodyBlocks();
        
        block.setParent(getParent());
        
        // Remove all of the children
        OMElement bElement = _getBodyBlockParent();
        Iterator it = bElement.getChildren();
        while (it.hasNext()) {
            it.next();
            it.remove();
        }
        
        if (block.isElementData()) {
            // If the block is element data then  it is safe to create
            // an OMElement representing the block

            // The block is supposed to represent a single element.  
            // But if it does not represent an element , the following will fail.
            QName qName = block.getQName();
        
            OMElement newOM = _createOMElementFromBlock(qName, block, soapFactory);
            bElement.addChild(newOM);
        } else {
            // This needs to be fixed, but for now we will require that there must be an element...otherwise no block is added
            try {
                QName qName = block.getQName();

                OMElement newOM = _createOMElementFromBlock(qName, block, soapFactory);
                bElement.addChild(newOM);
            } catch (Throwable t) {
                if (log.isDebugEnabled()) {
                    log.debug("An attempt was made to pass a Source or String that does not have an xml element. Processing continues with an empty payload.");
                }
            }
        }
    }

	public void removeBodyBlock(int index) throws WebServiceException {
	    // Forces the parser to read all of the blocks
        getNumBodyBlocks();
        
        OMElement om = this._getChildOMElement(_getBodyBlockParent(), index);
        if (om != null) {
            om.detach();
        }
	}

	public int getNumHeaderBlocks() throws WebServiceException {
		return _getNumChildElements(root.getHeader());
	}

	public Block getHeaderBlock(String namespace, String localPart, Object context, BlockFactory blockFactory) throws WebServiceException {
		OMElement om = _getChildOMElement(root.getHeader(), namespace, localPart);
        if (om == null) {
            return null;
        }
        return this._getBlockFromOMElement(om, context, blockFactory, false);
	}

	public void setHeaderBlock(String namespace, String localPart, Block block) throws WebServiceException {
        block.setParent(getParent());
        OMElement newOM = _createOMElementFromBlock(new QName(namespace, localPart), block, soapFactory);
        OMElement om = this._getChildOMElement(root.getHeader(), namespace, localPart);
        if (om == null) {
           if (root.getHeader() == null) {
               soapFactory.createSOAPHeader(root);
           }
           root.getHeader().addChild(newOM);
        } else {
            om.insertSiblingBefore(newOM);
            om.detach();
        }
	}

	
	public void removeHeaderBlock(String namespace, String localPart) throws WebServiceException {
        OMElement om = this._getChildOMElement(root.getHeader(), namespace, localPart);
        if (om != null) {
            om.detach();
        }
	}

	public String traceString(String indent) {
		// TODO Trace String Support
		return null;
	}

	public String getXMLPartContentType() {
        return "SPINE";
    }

    public boolean isFault() throws WebServiceException {
		return XMLFaultUtils.isFault(root);
	}

    public Style getStyle() {
        return style;
    }

    public QName getOperationElement() {
        OMElement omElement = this._getBodyBlockParent();
        if (omElement instanceof SOAPBody) {
            return null;
        } else {
            return omElement.getQName();
        }
    }

    public void setOperationElement(QName operationQName) {
        OMElement opElement = this._getBodyBlockParent();
        if (!(opElement instanceof SOAPBody)) {
            OMNamespace ns = soapFactory.createOMNamespace(operationQName.getNamespaceURI(), operationQName.getPrefix());
            opElement.setLocalName(operationQName.getLocalPart());
            opElement.setNamespace(ns);
        }
    }
    
    private Block _getBlockFromOMElement(OMElement om, Object context, BlockFactory blockFactory, boolean setComplete) throws WebServiceException {
        try {
            QName qName = om.getQName();
            /* TODO We could gain performance if OMSourcedElement exposed a getDataSource method 
             if (om instanceof OMSourcedElementImpl &&
             ((OMSourcedElementImpl) om).getDataSource() instanceof Block) {
             Block oldBlock = (Block) ((OMSourcedElementImpl) om).getDataSource();
             Block newBlock = blockFactory.createFrom(oldBlock, context);
             newBlock.setParent(getParent());
             if (newBlock != oldBlock) {
             // Replace the OMElement with the OMSourcedElement that delegates to the block
              OMSourcedElementImpl newOM = new OMSourcedElementImpl(qName, soapFactory, newBlock);
              om.insertSiblingBefore(newOM);
              om.detach();
              }
              return newBlock;
              } 
              */
            
            
            // Create the block
            Block block = blockFactory.createFrom(om, context, qName);
            block.setParent(getParent());
            
            // Get the business object to force a parse
            block.getBusinessObject(false);
            
            // Replace the OMElement with the OMSourcedElement that delegates to the block
            OMElement newOM = _createOMElementFromBlock(qName, block, soapFactory);
            om.insertSiblingBefore(newOM);
        
            // We want to set the om element and its parents to complete to 
            // shutdown the parsing.  
            // TODO It would also be nice to close the input XMLStreamReader connected
            // to the builder.
            if (setComplete) {
            	OMContainer o = om;
            	while (o != null && o instanceof OMContainerEx) {
            		((OMContainerEx)o).setComplete(true);
            		if ((o instanceof OMNode) &&
                        (((OMNode) o).getParent()) instanceof OMContainer) { 
            			o = ((OMNode) o).getParent();
            		} else {
            			o = null;
            		}
            	}
            }
            
  
            om.detach();
            return block;
        } catch (XMLStreamException xse) {
            throw ExceptionFactory.makeWebServiceException(xse);
        }
    }
    
    private static OMElement _createOMElementFromBlock(QName qName, Block b, SOAPFactory soapFactory) {
        return new OMSourcedElementImpl(qName, soapFactory, b);
    }
    
    /**
     * Gets the OMElement that is the parent of where the the body blocks are located
     * @return
     */
    private OMElement _getBodyBlockParent() {
        SOAPBody body = root.getBody();
        if (!body.hasFault() && indirection == 1) {
            //  For RPC the blocks are within the operation element
            OMElement op = body.getFirstElement();
            if (op == null) {
                // Create one
                OMNamespace ns = soapFactory.createOMNamespace("", "");
                op = soapFactory.createOMElement("PLACEHOLDER_OPERATION", ns, body);
            }   
            return op;
        }
        return body;
    }

    /**
     * Create a factory for the specified protocol
     * @param protocol
     * @return
     */
    private static SOAPFactory _getFactory(Protocol protocol) {
        SOAPFactory soapFactory;
        if (protocol == Protocol.soap11) {
            soapFactory = new SOAP11Factory();
        } else if (protocol == Protocol.soap12) {
            soapFactory = new SOAP12Factory();
        } else {
            // TODO REST Support is needed
            throw ExceptionFactory.makeWebServiceException(Messages.getMessage("RESTIsNotSupported"), null);
        }
        return soapFactory;
    }
    
    /**
     * Create an emtpy envelope
     * @param protocol
     * @param style
     * @param factory
     * @return
     */
    private static SOAPEnvelope _createEmptyEnvelope(Protocol protocol, Style style, SOAPFactory factory) {
        SOAPEnvelope env = factory.createSOAPEnvelope();
        // Add an empty body and header
        factory.createSOAPBody(env);
        factory.createSOAPHeader(env);
        
        // Create a dummy operation element if this is an rpc message
        if (style == Style.RPC) {
            OMNamespace ns = factory.createOMNamespace("", "");
            factory.createOMElement("PLACEHOLDER_OPERATION", ns, env.getBody());
        }

        return env;
    }

    /* (non-Javadoc)
     * @see org.apache.axis2.jaxws.message.XMLPart#getNumBodyBlocks()
     */
    private static int _getNumChildElements(OMElement om) throws WebServiceException {
        // Avoid calling this method.  It advances the parser.
        if (om == null) {
            return 0;
        }
        int num = 0;
        Iterator iterator = om.getChildElements();
        while (iterator.hasNext()) {
            num++;
            iterator.next();
        }
        return num;
    }
    
    /** Get the child om at the indicated index
     * @param om
     * @param index
     * @return child om or null
     */
    private static OMElement _getChildOMElement(OMElement om, int index) {
        if (om == null) {
            return null;
        }
        int i=0;
        for (OMNode child = om.getFirstOMChild();
            child != null;
            child = child.getNextOMSibling()) {
            if (child instanceof OMElement) {
                if (i == index) {
                    return (OMElement) child;
                }
                i++;
            }
        } 
        return null;
    }
    
    /** Get the child om at the indicated index
     * @param om
     * @param index
     * @return child om or null
     */
    private static OMElement _getChildOMElement(OMElement om, String namespace, String localPart) {
        if (om == null) {
            return null;
        }
        QName qName = new QName(namespace, localPart);
        Iterator it = om.getChildrenWithName(qName);
        if (it != null && it.hasNext()) {
            return (OMElement) it.next();
        }
        return null;
    }

}
