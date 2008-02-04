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

import java.util.EventObject;

public class PageComponentEvent extends EventObject {

	public final static byte COMPONENT_RESIZED = 0x00;
	public final static byte COMPONENT_LOADED = 0x01;
	public final static byte COMPONENT_CHANGED = 0x02;
	public final static byte COMPONENT_RUN = 0x03;
	public final static byte SNAPSHOT_TAKEN = 0x04;
	public final static byte COMPONENT_RESET = 0x05;
	public final static byte SNAPSHOT_TAKEN2 = 0x06;

	private byte id = -0x01;

	public PageComponentEvent(Object source, byte id) {
		super(source);
		this.id = id;
	}

	public byte getID() {
		return id;
	}

}