/*******************************************************************************
 * Copyright (c) 2017 Aston University
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the Eclipse
 * Public License, v. 2.0 are satisfied: GNU General Public License, version 3.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-3.0
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

