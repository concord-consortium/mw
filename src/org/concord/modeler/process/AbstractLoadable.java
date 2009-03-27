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

package org.concord.modeler.process;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

/**
 * This is a partial implementation of <tt>Loadable</tt>.
 * 
 * @author Qian Xie
 */

public abstract class AbstractLoadable implements Loadable {

	private int lifetime = ETERNAL;
	private int interval = 10;
	private int minInterval = 1;
	private int maxInterval = 5000;
	private int minLifetime = 100;
	private int maxLifetime = 1000;
	private String name = "Unknown";
	private String description;
	private boolean enabled = true;
	private boolean completed;
	private boolean systemTask = true;
	private String script;
	private int priority = Thread.NORM_PRIORITY;
	private transient PropertyChangeSupport pcs;

	public AbstractLoadable() {
		name += "@" + System.currentTimeMillis();
		pcs = new PropertyChangeSupport(this);
	}

	/**
	 * @param i
	 *            interval for executing this subtask
	 */
	public AbstractLoadable(int i) {
		this();
		setInterval(i);
	}

	/**
	 * @param i
	 *            interval for executing this subtask
	 * @param j
	 *            total steps for executing this subtask
	 */
	public AbstractLoadable(int i, int j) {
		this();
		setInterval(i);
		setLifetime(j);
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean b) {
		enabled = b;
	}

	public void setSystemTask(boolean b) {
		systemTask = b;
	}

	public boolean isSystemTask() {
		return systemTask;
	}

	public void setScript(String script) {
		this.script = script;
	}

	public String getScript() {
		return script;
	}

	public boolean equals(Object o) {
		if (!(o instanceof AbstractLoadable))
			return false;
		return ((AbstractLoadable) o).getName().equals(getName());
	}

	public int hashCode() {
		return getName().hashCode();
	}

	public void setCompleted(boolean b) {
		completed = b;
	}

	public boolean isCompleted() {
		return completed;
	}

	public void setPriority(int i) {
		priority = i;
	}

	public int getPriority() {
		return priority;
	}

	public void setLifetime(int i) {
		pcs.firePropertyChange("lifetime", lifetime, i);
		lifetime = i;
	}

	public int getLifetime() {
		return lifetime;
	}

	public int getMinLifetime() {
		return minLifetime;
	}

	public void setMinLifetime(int i) {
		minLifetime = i;
	}

	public int getMaxLifetime() {
		return maxLifetime;
	}

	public void setMaxLifetime(int i) {
		maxLifetime = i;
	}

	public int getInterval() {
		return interval;
	}

	public void setInterval(int i) {
		pcs.firePropertyChange("interval", interval, i);
		interval = i;
	}

	public int getMinInterval() {
		return minInterval;
	}

	public void setMinInterval(int i) {
		minInterval = i;
	}

	public int getMaxInterval() {
		return maxInterval;
	}

	public void setMaxInterval(int i) {
		maxInterval = i;
	}

	public void setName(String s) {
		name = s;
	}

	public String getName() {
		return name;
	}

	public void setDescription(String s) {
		description = s;
	}

	public String getDescription() {
		return description;
	}

	public String toString() {
		return getName() + ":" + getPriority();
	}

	public void addPropertyChangeListener(PropertyChangeListener pcl) {
		pcs.addPropertyChangeListener(pcl);
	}

	public void removePropertyChangeListener(PropertyChangeListener pcl) {
		pcs.removePropertyChangeListener(pcl);
	}

}
