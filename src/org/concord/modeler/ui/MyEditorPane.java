/*
 *   Copyright (C) 2006  The Concord Consortium, Inc.,
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
package org.concord.modeler.ui;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;

import javax.swing.JEditorPane;
import javax.swing.text.AttributeSet;
import javax.swing.text.Element;
import javax.swing.text.ElementIterator;
import javax.swing.text.html.HTML;

/**
 * This class can be used to retrieve the image elements of a HTML document reliably.
 * 
 * @author Charles Xie
 * 
 */
public class MyEditorPane extends JEditorPane {

	public MyEditorPane() {
		super();
		setContentType("text/html");
	}

	public MyEditorPane(String type, String text) {
		super(type, text);
	}

	public MyEditorPane(String url) throws IOException {
		super(url);
		setContentType("text/html");
	}

	public MyEditorPane(URL initialPage) throws IOException {
		super(initialPage);
		setContentType("text/html");
	}

	public String getAttribute(String tag, String name) {
		ElementIterator ei = new ElementIterator(getDocument());
		Element e = ei.next();
		AttributeSet as;
		Object o;
		Enumeration en;
		HashMap<String, String> map = new HashMap<String, String>();
		while (e != null) {
			as = e.getAttributes();
			en = as.getAttributeNames();
			map.clear();
			while (en.hasMoreElements()) {
				o = en.nextElement();
				map.put(o.toString(), as.getAttribute(o).toString());
			}
			if (map.containsValue(tag))
				return map.get(name);
			e = ei.next();
		}
		return null;
	}

	/** return a list of all embedded images in this HTML text */
	public List<String> getImageNames() {
		ElementIterator it = new ElementIterator(getDocument());
		AttributeSet as = null;
		Element el = null;
		Enumeration en = null;
		List<String> list = null;
		String s = null;
		Object obj = null;
		synchronized (getDocument()) {
			while (it.next() != null) {
				el = it.current();
				if (el.getName().equalsIgnoreCase(HTML.Tag.IMG.toString())) {
					as = el.getAttributes();
					en = as.getAttributeNames();
					while (en.hasMoreElements()) {
						obj = en.nextElement();
						if (obj.toString().equalsIgnoreCase(HTML.Attribute.SRC.toString())) {
							if (list == null)
								list = new ArrayList<String>();
							// s=FileUtilities.getFileName(as.getAttribute(obj).toString());
							s = as.getAttribute(obj).toString();
							if (!list.contains(s))
								list.add(s);
						}
					}
				}
			}
		}
		return list;
	}

}
