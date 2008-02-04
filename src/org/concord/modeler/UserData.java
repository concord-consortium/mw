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

package org.concord.modeler;

import java.util.HashMap;
import java.util.TreeMap;
import java.util.Map;
import java.util.Set;

public class UserData {

	private final static UserData sharedInstance = new UserData();

	private Map<String, QuestionAndAnswer> map;

	private UserData() {
		map = new TreeMap<String, QuestionAndAnswer>();
	}

	public final static UserData sharedInstance() {
		return sharedInstance;
	}

	public Map<String, QuestionAndAnswer> getCopy() {
		Map<String, QuestionAndAnswer> copy = new HashMap<String, QuestionAndAnswer>();
		QuestionAndAnswer val;
		for (String key : map.keySet()) {
			val = map.get(key);
			copy.put(new String(key), new QuestionAndAnswer(val));
		}
		return copy;
	}

	public void putData(String key, QuestionAndAnswer value) {
		map.put(key, value);
	}

	public QuestionAndAnswer getData(String key) {
		return map.get(key);
	}

	public QuestionAndAnswer removeData(String key) {
		return map.remove(key);
	}

	public Set<String> keySet() {
		return map.keySet();
	}

	public String toString() {
		return map.toString();
	}

}