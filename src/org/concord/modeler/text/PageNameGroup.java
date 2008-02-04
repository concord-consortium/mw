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

package org.concord.modeler.text;

import java.util.ArrayList;
import java.util.List;

public class PageNameGroup {

	private final static String REGEX_SEPARATOR = ",[\\s&&[^\\r\\n]]*";

	private List<String> list;

	public PageNameGroup() {
		list = new ArrayList<String>();
	}

	public void setNameGroup(String s) {
		list.clear();
		if (s == null || s.trim().equals(""))
			return;
		String[] ss = s.split(REGEX_SEPARATOR);
		for (String t : ss) {
			t = t.trim();
			if (t.equals(""))
				continue;
			if (list.contains(t))
				continue;
			list.add(t);
		}
	}

	public String getNameGroup() {
		if (list == null || list.isEmpty())
			return null;
		String s = "";
		int n = list.size();
		for (int i = 0; i < n - 1; i++)
			s += list.get(i) + ", ";
		s += list.get(n - 1);
		return s;
	}

	public void addPageName(String s) {
		if (list.contains(s))
			return;
		list.add(s);
	}

	public String getPageName(int index) {
		if (index < 0 || index >= size())
			return null;
		return (String) list.get(index);
	}

	public int size() {
		return list.size();
	}

	public boolean equals(Object object) {
		if (!(object instanceof PageNameGroup))
			return false;
		return list.equals(((PageNameGroup) object).list);
	}

	public int hashCode() {
		return list.hashCode();
	}

	public String toString() {
		return getNameGroup();
	}

}