/*******************************************************************************
 * Copyright (c) 2017 Aston University
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * Contributors:
 *     Orjuwan Al-Wadeai - Initial Implementation of Hawk Server Configuration
 ******************************************************************************/
package org.hawk.service.servlet.config;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public class SchemaErrorHandler implements ErrorHandler {

    @Override
    public void warning(SAXParseException exception) throws SAXException {
    	System.err.println(getMessage("Warning", exception));
    }

    @Override
    public void error(SAXParseException exception) throws SAXException {
        System.err.println(getMessage("Error", exception));
    }

    @Override
    public void fatalError(SAXParseException exception) throws SAXException {
    	 throw new SAXException(getMessage("Fatal", exception));
    }

    private String getMessage(String level, SAXParseException exception)  {
        int lineNumber = exception.getLineNumber();
        int columnNumber = exception.getColumnNumber();
        String message = exception.getMessage();
        return ("[" + level + "] line nr: " + lineNumber + " column nr: " + columnNumber + " message: " + message);
    }

}

