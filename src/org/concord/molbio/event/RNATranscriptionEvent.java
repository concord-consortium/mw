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

public class RNATranscriptionEvent extends EventObject {

	private int mode = RNATranscriptionListener.MODE_UNKNOWN;
	private int baseIndex;
	private boolean consumed;

	public RNATranscriptionEvent(Object src, int baseIndex, int mode) {
		super(src);
		this.mode = mode;
		this.baseIndex = baseIndex;
		consumed = false;
	}

	public int getBaseIndex() {
		return baseIndex;
	}

	public int getMode() {
		return mode;
	}

	public void setBaseIndex(int baseIndex) {
		this.baseIndex = baseIndex;
	}

	public void setMode(int mode) {
		this.mode = mode;
	}

	public boolean isConsumed() {
		return consumed;
	}

	public void setConsumed(boolean consumed) {
		this.consumed = consumed;
	}

}