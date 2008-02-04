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

public class ModelEvent extends EventObject {

	public final static byte MODEL_INPUT = 0x00;
	public final static byte MODEL_OUTPUT = 0x01;
	public final static byte MODEL_CHANGED = 0x02;
	public final static byte MODEL_RESET = 0x03;
	public final static byte MODEL_RUN = 0x04;
	public final static byte MODEL_STOP = 0x05;
	public final static byte VIEW_CHANGED = 0x06;
	public final static byte CONTROLLER_CHANGED = 0x07;
	public final static byte COMPONENT_REMOVED = 0x08;
	public final static byte SCRIPT_START = 0x09;
	public final static byte SCRIPT_END = 0x0a;

	private byte id = -0x01;

	private String description = "";
	private Object previousState;
	private Object currentState;

	public ModelEvent(Object source, byte id) {
		super(source);
		this.id = id;
	}

	public ModelEvent(Object source, String description) {
		super(source);
		this.description = description;
	}

	public ModelEvent(Object source, byte id, Object previousState, Object currentState) {
		super(source);
		this.id = id;
		this.previousState = previousState;
		this.currentState = currentState;
	}

	public ModelEvent(Object source, String description, Object previousState, Object currentState) {
		super(source);
		this.description = description;
		this.previousState = previousState;
		this.currentState = currentState;
	}

	public byte getID() {
		return id;
	}

	public String getDescription() {
		return description;
	}

	public Object getPreviousState() {
		return previousState;
	}

	public Object getCurrentState() {
		return currentState;
	}

}