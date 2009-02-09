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

package org.concord.molbio.event;

import java.util.EventObject;

public class DNAHistoryEvent extends EventObject {

	public static final byte HISTORY_UNKNOWN_EVENT = 0;
	public static final byte HISTORY_MODIFIED_EVENT = 1;
	public static final byte HISTORY_CLEARED_EVENT = 2;

	private int id = HISTORY_UNKNOWN_EVENT;

	public DNAHistoryEvent(Object source, int id) {
		super(source);
		this.id = id;
	}

	public int getID() {
		return id;
	}

}
