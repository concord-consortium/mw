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

package org.concord.modeler.event;

import java.util.HashMap;
import java.util.Map;

import javax.swing.Action;
import javax.swing.event.ChangeListener;

public abstract class AbstractChange implements ChangeListener {

	/** The key used for storing the scale factor for the action of change */
	public final static String SCALE = "Scale";

	/** The key used for storing the name for the action of change */
	public final static String NAME = Action.NAME;

	/** The key used for storing a short description for the action of change, used for tooltip text. */
	public final static String SHORT_DESCRIPTION = Action.SHORT_DESCRIPTION;

	/**
	 * The key used for storing a longer description for the action of change, could be used for context-sensitive help.
	 */
	public final static String LONG_DESCRIPTION = Action.LONG_DESCRIPTION;

	private boolean enabled;
	private Map<String, Object> properties;

	public abstract double getMinimum();

	public abstract double getMaximum();

	public abstract double getStepSize();

	public abstract double getValue();

	public Object getProperty(String key) {
		if (properties == null || properties.isEmpty())
			return null;
		return properties.get(key);
	}

	public void putProperty(String key, Object value) {
		if (properties == null)
			properties = new HashMap<String, Object>();
		properties.put(key, value);
	}

	public void setEnabled(boolean b) {
		enabled = b;
	}

	public boolean isEnabled() {
		return enabled;
	}

}