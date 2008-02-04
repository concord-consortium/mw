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

package org.concord.mw2d.event;

import java.util.EventObject;

public class BondChangeEvent extends EventObject {

	private Object oldValue;
	private Object newValue;
	private String parameterName;
	private byte type;

	public BondChangeEvent(Object source) {
		super(source);
	}

	/** construct an event when we do not care about the values before and after changes. */
	public BondChangeEvent(Object source, byte type) {
		this(source);
		this.type = type;
	}

	/** construct an event with old and new values */
	public BondChangeEvent(Object source, String parameterName, Object oldValue, Object newValue) {
		this(source);
		this.oldValue = oldValue;
		this.newValue = newValue;
		this.parameterName = parameterName;
	}

	public byte getType() {
		return type;
	}

	public Object getOldValue() {
		return oldValue;
	}

	public Object getNewValue() {
		return newValue;
	}

	public String getParameterName() {
		return parameterName;
	}

}