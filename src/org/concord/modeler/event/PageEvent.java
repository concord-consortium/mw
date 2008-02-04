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

public class PageEvent extends EventObject {

	public final static byte PAGE_REFRESH = 0x00;
	public final static byte PAGE_READ_BEGIN = 0x01;
	public final static byte PAGE_READ_END = 0x02;
	public final static byte PAGE_WRITE_BEGIN = 0x03;
	public final static byte PAGE_WRITE_END = 0x04;
	public final static byte PAGE_OVERWRITE_BEGIN = 0x05;
	public final static byte PAGE_OVERWRITE_END = 0x06;
	public final static byte NEW_PAGE = 0x07;
	public final static byte LOAD_ERROR = 0x08;
	public final static byte OPEN_NEW_WINDOW = 0x09;
	public final static byte CLOSE_CURRENT_WINDOW = 0x0a;
	public final static byte FONT_CHANGED = 0x0b;
	public final static byte STORE_VIEW_POSITION = 0x0c;
	public final static byte RESTORE_VIEW_POSITION = 0x0d;

	private byte type = -0x01;
	private String description;
	private Object properties;

	public PageEvent(Object source, byte type) {
		super(source);
		this.type = type;
	}

	public PageEvent(Object source, byte type, String description) {
		super(source);
		this.type = type;
		this.description = description;
	}

	public PageEvent(Object source, byte type, String description, Object properties) {
		super(source);
		this.type = type;
		this.description = description;
		this.properties = properties;
	}

	public byte getType() {
		return type;
	}

	public String getDescription() {
		return description;
	}

	public Object getProperties() {
		return properties;
	}

}