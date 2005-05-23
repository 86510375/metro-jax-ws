/*
 * $Id: SOAPMessageContext.java,v 1.1 2005-05-23 22:30:17 bbissett Exp $
 */

/*
 * Copyright 2004 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.sun.xml.ws.encoding.soap.message;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.namespace.QName;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.MimeHeaders;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.transform.stream.StreamSource;

import com.sun.xml.messaging.saaj.util.ByteInputStream;
import com.sun.xml.ws.handler.MessageContextImpl;
import com.sun.xml.ws.util.NullIterator;
import com.sun.xml.ws.util.xml.XmlUtil;

/**
 * A MessageContext holds a SOAP message as well as a set
 * (possibly transport-specific) properties.
 *
 * @author JAX-RPC Development Team
 */
public class SOAPMessageContext extends MessageContextImpl
    implements com.sun.xml.ws.spi.runtime.SOAPMessageContext {

    public SOAPMessageContext() {
    }

    public Set<URI> getRoles() {
        return roles;
    }

    /*
    * Method called internally to set the roles
    */
    public void setRoles(Set<URI> roles) {
        this.roles = roles;
    }

    public SOAPMessage getMessage() {
        return _message;
    }

    public void setMessage(SOAPMessage message) {
        _message = message;
    }

    public Object[] getHeaders(QName header, JAXBContext context,
        boolean allRoles){

        return null;
    }


    public boolean isFailure() {
        return _failure;
    }

    public void setFailure(boolean b) {
        _failure = b;
    }

    int currentHandler = -1;

    public void setCurrentHandler(int i) {
        currentHandler = i;
    }

    public int getCurrentHandler() {
        return currentHandler;
    }

    public SOAPMessage createMessage() {
        try {
            return getMessageFactory().createMessage();
        } catch (SOAPException e) {
            throw new SOAPMsgCreateException(
                    "soap.msg.create.err",
                    new Object[] { e });
        }
    }

    public SOAPMessage createMessage(MimeHeaders headers, InputStream in)
        throws IOException {
        try {
            return getMessageFactory().createMessage(headers, in);
        } catch (SOAPException e) {
            throw new SOAPMsgCreateException(
                    "soap.msg.create.err",
                    new Object[] { e });
        }
    }

    public void writeInternalServerErrorResponse() {
        try {
            setFailure(true);
            SOAPMessage message = createMessage();
            message.getSOAPPart().setContent(
                    new StreamSource(
                                    XmlUtil.getUTF8ByteInputStream(
                                    DEFAULT_SERVER_ERROR_ENVELOPE)));
            setMessage(message);
        } catch (SOAPException e) {
            // this method is called as a last resort, so it fails we cannot possibly recover
        }
    }

    public void writeSimpleErrorResponse(QName faultCode, String faultString) {
        try {
            setFailure(true);
            SOAPMessage message = createMessage();
            ByteArrayOutputStream bufferedStream = new ByteArrayOutputStream();
            Writer writer = new OutputStreamWriter(bufferedStream, "UTF-8");
            writer.write(
                    "<?xml version='1.0' encoding='UTF-8'?>\n"
                            + "<env:Envelope xmlns:env=\"http://schemas.xmlsoap.org/soap/envelope/\">"
                            + "<env:Body><env:Fault><faultcode>env:");
            writer.write(faultCode.getLocalPart());
            writer.write("</faultcode>" + "<faultstring>");
            writer.write(faultString);
            writer.write(
                    "</faultstring>" + "</env:Fault></env:Body></env:Envelope>");
            writer.close();
            byte[] data = bufferedStream.toByteArray();
            message.getSOAPPart().setContent(
                        new StreamSource(new ByteInputStream(data, data.length)));
            setMessage(message);
        } catch (Exception e) {
            writeInternalServerErrorResponse();
        }
    }

    private static MessageFactory getMessageFactory() {
        try {
            if (_messageFactory == null) {
                _messageFactory = MessageFactory.newInstance();
            }
        } catch(SOAPException e) {
            throw new SOAPMsgFactoryCreateException(
                "soap.msg.factory.create.err",
                new Object[] { e });
        }
        return _messageFactory;
    }

    private SOAPMessage _message;
    private boolean _failure;
    private Set<URI> roles;
    private static MessageFactory _messageFactory;
    private final static String DEFAULT_SERVER_ERROR_ENVELOPE =
                "<?xml version='1.0' encoding='UTF-8'?>"
                + "<env:Envelope xmlns:env=\"http://schemas.xmlsoap.org/soap/envelope/\">"
                + "<env:Body>"
                + "<env:Fault>"
                + "<faultcode>env:Server</faultcode>"
                + "<faultstring>Internal server error</faultstring>"
                + "</env:Fault>"
                + "</env:Body>"
                + "</env:Envelope>";
}
