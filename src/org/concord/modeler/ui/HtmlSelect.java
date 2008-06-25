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
import java.util.ArrayList;
import java.util.List;

import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.text.Element;

/**
 * A construct for HTML's Select.
 * 
 * @author Charles Xie
 * 
 */
public class HtmlSelect {

	// standard select tags
	private String name;

	// MW-specific tags
	private boolean enabled = true;

	private List<HtmlOption> options;

	private Object model;
	private Element sourceElement;

	public HtmlSelect() {
	}

	public void setEnabled(boolean b) {
		enabled = b;
	}

	public boolean getEnabled() {
		return enabled;
	}

	public void addOption(HtmlOption o) {
		if (options == null)
			options = new ArrayList<HtmlOption>();
		options.add(o);
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
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
		if (options == null)
			return "";
		String s = name + "=";
		int n = 0;
		int count = options.size();
		HtmlOption o = null;
		if (model instanceof DefaultComboBoxModel) {
			DefaultComboBoxModel cbm = (DefaultComboBoxModel) model;
			for (int i = 0; i < count; i++) {
				if (cbm.getSelectedItem() == cbm.getElementAt(i)) {
					o = options.get(i);
					try {
						s += URLEncoder.encode(o.getValue(), "UTF-8") + ",";
					}
					catch (UnsupportedEncodingException e) {
						e.printStackTrace();
					}
					n++;
				}
			}
		}
		else if (model instanceof DefaultListModel) {
		}
		if (n > 0)
			return s.substring(0, s.length() - 1);
		return s;
	}

	public String toString() {
		String s = "<select name=\"" + name + "\">";
		if (options != null && !options.isEmpty()) {
			for (HtmlOption o : options) {
				s += "\n" + o;
			}
		}
		s += "\n</select>:" + model;
		return s;
	}

}
