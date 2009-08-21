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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import javax.swing.ButtonModel;
import javax.swing.JToggleButton;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;

/**
 * A construct for HTML's Input.
 * 
 * @author Charles Xie
 * 
 */
public class HtmlInput {

	// standard input attributes
	private String type;
	private String name;
	private String value;
	private String alt;

	// MW-only input attributes
	private String uid;
	private String selectedScript;
	private String deselectedScript;
	private String selectedSelfScript;
	private String deselectedSelfScript;
	private String question;
	private boolean enabled = true;

	private Object model;
	private Element sourceElement;

	public HtmlInput() {
	}

	public void setEnabled(boolean b) {
		enabled = b;
	}

	public boolean getEnabled() {
		return enabled;
	}

	public void setUid(String uid) {
		this.uid = uid;
	}

	public String getUid() {
		return uid;
	}

	public void setAlt(String alt) {
		this.alt = alt;
	}

	public String getAlt() {
		return alt;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getType() {
		return type;
	}

	public void setSelectedScript(String script) {
		selectedScript = script;
	}

	public String getSelectedScript() {
		return selectedScript;
	}

	public void setDeselectedScript(String script) {
		deselectedScript = script;
	}

	public String getDeselectedScript() {
		return deselectedScript;
	}

	public void setSelectedSelfScript(String script) {
		selectedSelfScript = script;
	}

	public String getSelectedSelfScript() {
		return selectedSelfScript;
	}

	public void setDeselectedSelfScript(String script) {
		deselectedSelfScript = script;
	}

	public String getDeselectedSelfScript() {
		return deselectedSelfScript;
	}

	public void setQuestion(String question) {
		this.question = question;
	}

	public String getQuestion() {
		return question;
	}

	public void setModel(Object m) {
		model = m;
	}

	public Object getModel() {
		return model;
	}

	public void setSourceElement(Element e) {
		sourceElement = e;
	}

	public Element getSourceElement() {
		return sourceElement;
	}

	public String getQueryString() {
		if (model instanceof Document) {
			Document doc = (Document) model;
			if ("text".equalsIgnoreCase(type)) {
				if (doc.getLength() > 0) {
					try {
						return name + "=" + URLEncoder.encode(doc.getText(0, doc.getLength()), "UTF-8");
					}
					catch (BadLocationException e) {
						e.printStackTrace();
					}
					catch (UnsupportedEncodingException e) {
						e.printStackTrace();
					}
				}
			}
			else if ("password".equalsIgnoreCase(type)) {
				if (doc.getLength() > 0) {
					try {
						return name + "=" + URLEncoder.encode(doc.getText(0, doc.getLength()), "UTF-8");
					}
					catch (BadLocationException e) {
						e.printStackTrace();
					}
					catch (UnsupportedEncodingException e) {
						e.printStackTrace();
					}
				}
			}
		}
		else {
			if (value != null && value.length() > 0) {
				if ("hidden".equalsIgnoreCase(type)) {
					try {
						return name + "=" + URLEncoder.encode(value, "UTF-8");
					}
					catch (UnsupportedEncodingException e) {
						e.printStackTrace();
					}
				}
			}
		}
		return "";
	}

	public String toString() {
		if (model instanceof JToggleButton.ToggleButtonModel) {
			boolean b = ((ButtonModel) model).isSelected();
			return "<input type=\"" + type + "\" name=\"" + name + "\" value=\"" + value + "\" selected=\"" + b + "\">";
		}
		return "<input type=\"" + type + "\" name=\"" + name + "\" value=\"" + value + "\">:" + model;
	}

}
