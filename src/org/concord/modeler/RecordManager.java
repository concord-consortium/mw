/*
 *   Copyright (C) 2007  The Concord Consortium, Inc.,
 *   25 Love Lane, Concord, MA 01742
 *
 *   This program is free software; you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation; either version 2 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program; if not, write to the Free Software
 *   Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 * END LICENSE */
package org.concord.modeler;

/**
 * @author Charles Xie
 * 
 */
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.concord.modeler.util.FileUtilities;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * @author Charles Xie
 * 
 */
public class RecordManager {

	private final static RecordManager sharedInstance = new RecordManager();
	private SAXParser saxParser;
	private RecordHandler recordHandler;
	private Map<String, QuestionAndAnswer> copy;

	private RecordManager() {
		try {
			saxParser = SAXParserFactory.newInstance().newSAXParser();
		}
		catch (SAXException e) {
			e.printStackTrace();
		}
		catch (ParserConfigurationException e) {
			e.printStackTrace();
		}
		recordHandler = new RecordHandler();
	}

	public static RecordManager sharedInstance() {
		return sharedInstance;
	}

	void parse() {
		File file = new File(Initializer.sharedInstance().getCacheDirectory(), "data.xml");
		if (!file.exists())
			return;
		try {
			saxParser.parse(new InputSource(new FileInputStream(file)), recordHandler);
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		catch (SAXException e) {
			e.printStackTrace();
		}
		copy = UserData.sharedInstance().getCopy();
	}

	boolean isChanged(String key) {
		if (copy == null || copy.isEmpty()) {
			return true;
		}
		QuestionAndAnswer oldValue = copy.get(key);
		QuestionAndAnswer newValue = UserData.sharedInstance().getData(key);
		if (oldValue == null && newValue == null)
			return false;
		if (oldValue == null && newValue != null)
			return true;
		if (oldValue != null && newValue == null)
			return true;
		String oldAnswer = oldValue == null ? null : oldValue.getAnswer();
		String newAnswer = newValue == null ? null : newValue.getAnswer();
		if (newAnswer == null)
			return true; // if newAnswer is null, always return true?
		if (oldAnswer == null)
			return true;
		return !oldAnswer.equals(newAnswer);
	}

	boolean isChangedOnPage(String pageAddress) {
		String parent = FileUtilities.getCodeBase(pageAddress);
		for (String key : UserData.sharedInstance().keySet()) {
			if (FileUtilities.getCodeBase(key).equals(parent)) {
				if (isChanged(key))
					return true;
			}
		}
		return false;
	}

}