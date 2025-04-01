/*
 * Copyright (c) 2013 L2jMobius
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 * IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.l2jmobius.gameserver.script;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class ScriptDocument
{
	private static final Logger LOGGER = Logger.getLogger(ScriptDocument.class.getName());
	
	private Document _document;
	private final String _name;
	
	public ScriptDocument(String name, InputStream input)
	{
		_name = name;
		
		final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		try
		{
			_document = factory.newDocumentBuilder().parse(input);
		}
		catch (SAXException sxe)
		{
			LOGGER.warning(getClass().getSimpleName() + ": " + (sxe.getException() != null ? sxe.getException() : sxe).getMessage());
		}
		catch (ParserConfigurationException pce)
		{
			// Parser with specified options can't be built
			LOGGER.log(Level.WARNING, "", pce);
		}
		catch (IOException ioe)
		{
			// I/O error
			LOGGER.log(Level.WARNING, "", ioe);
		}
	}
	
	public Document getDocument()
	{
		return _document;
	}
	
	/**
	 * @return Returns the _name.
	 */
	public String getName()
	{
		return _name;
	}
	
	@Override
	public String toString()
	{
		return _name;
	}
}
