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

import java.util.ArrayList;
import java.util.List;

import javax.swing.ButtonModel;
import javax.swing.JToggleButton;
import javax.swing.text.Document;

/**
 * A construct for HTML's form.
 * 
 * @author Charles Xie
 * 
 */
public class HtmlForm {

	List<HtmlInput> inputList;
	List<HtmlSelect> selectList;
	private String action;
	private String method;

	public HtmlForm() {
	}

	public void setAction(String action) {
		this.action = action;
	}

	public String getAction() {
		return action;
	}

	public void setMethod(String method) {
		this.method = method;
	}

	public String getMethod() {
		return method;
	}

	public void addInput(HtmlInput i) {
		if (inputList == null)
			inputList = new ArrayList<HtmlInput>();
		inputList.add(i);
	}

	public void addSelect(HtmlSelect s) {
		if (selectList == null)
			selectList = new ArrayList<HtmlSelect>();
		selectList.add(s);
	}

	public String getQueryString() {
		String s = "";
		if (inputList != null && !inputList.isEmpty()) {
			int n = 0;
			Object m;
			for (HtmlInput i : inputList) {
				m = i.getModel();
				if (m instanceof Document) {
					String query = i.getQueryString();
					if (query.length() > 0)
						s += "&" + query;
				}
				else if (m instanceof JToggleButton.ToggleButtonModel) {
					n++;
				}
				else {
					if (i.getType().equalsIgnoreCase("hidden")) {
						String query = i.getQueryString();
						if (query.length() > 0)
							s += "&" + query;
					}
				}
			}
			if (n > 0) {
				for (HtmlInput i : inputList) {
					if (i.getModel() instanceof JToggleButton.ToggleButtonModel) {
						ButtonModel bm = (ButtonModel) i.getModel();
						if (bm.isSelected()) {
							s += "&" + i.getName() + "=" + i.getValue();
						}
					}
				}
			}
		}
		if (selectList != null && !selectList.isEmpty()) {
			for (HtmlSelect i : selectList) {
				s += "&" + i.getQueryString();
			}
		}
		return s;
	}

	public String toString() {
		String s = "<form action=\"" + action + "\" method=\"" + method + "\">";
		if (inputList != null) {
			for (HtmlInput i : inputList) {
				s += "\n" + i;
			}
		}
		if (selectList != null) {
			for (HtmlSelect i : selectList) {
				s += "\n" + i;
			}
		}
		s += "\n</form>\n";
		return s;
	}

}
