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

package org.concord.mw2d;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.concord.modeler.process.Loadable;
import org.concord.mw2d.models.TaskAttributes;

public abstract class MDState implements Serializable {

	private Map properties;
	private List<TaskAttributes> tasks;

	public MDState() {
		properties = new HashMap();
		tasks = new ArrayList<TaskAttributes>();
	}

	public List<TaskAttributes> getTasks() {
		return tasks;
	}

	public void setTasks(List<TaskAttributes> tasks) {
		this.tasks = tasks;
	}

	public void addTasks(List<Loadable> list) {
		if (list == null || list.isEmpty())
			return;
		for (Loadable l : list) {
			tasks.add(new TaskAttributes(l));
		}
	}

	public Map getProperties() {
		return properties;
	}

	public void setProperties(Map hm) {
		properties = hm;
	}

}