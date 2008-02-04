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

import java.util.EventObject;

/**
 * @author Charles Xie
 * 
 */
class SnapshotEvent extends EventObject {

	final static byte SNAPSHOT_ADDED = 0x01;
	final static byte SNAPSHOT_CHANGED = 0x02;
	final static byte SNAPSHOT_REMOVED = 0x03;

	private byte type;
	private String previousImageName;
	private String currentImageName;

	SnapshotEvent(Object source, byte type) {
		super(source);
		this.type = type;
	}

	SnapshotEvent(Object source, byte type, String currentImageName) {
		this(source, type);
		this.currentImageName = currentImageName;
	}

	SnapshotEvent(Object source, byte type, String previousImageName, String currentImageName) {
		this(source, type, currentImageName);
		this.previousImageName = previousImageName;
	}

	byte getType() {
		return type;
	}

	String getPreviousImageName() {
		return previousImageName;
	}

	String getCurrentImageName() {
		return currentImageName;
	}

}