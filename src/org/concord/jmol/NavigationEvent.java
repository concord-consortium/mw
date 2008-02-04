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
package org.concord.jmol;

import java.util.EventObject;

/**
 * @author Charles Xie
 * 
 */
class NavigationEvent extends EventObject {

	public final static byte ARRIVAL = 0;
	public final static byte DEPARTURE = 1;

	private int currentSceneIndex;
	private int nextSceneIndex;
	private int sceneCount;
	private String preScript;
	private String postScript;
	private byte type = -1;

	public NavigationEvent(Object source, byte type, int currentSceneIndex, int nextSceneIndex, int sceneCount,
			String preScript, String postScript) {
		super(source);
		this.type = type;
		this.currentSceneIndex = currentSceneIndex;
		this.nextSceneIndex = nextSceneIndex;
		this.sceneCount = sceneCount;
		this.preScript = preScript;
		this.postScript = postScript;
	}

	public byte getType() {
		return type;
	}

	public String getPreScript() {
		return preScript;
	}

	public String getPostScript() {
		return postScript;
	}

	public int getCurrentSceneIndex() {
		return currentSceneIndex;
	}

	public int getNextSceneIndex() {
		return nextSceneIndex;
	}

	public int getSceneCount() {
		return sceneCount;
	}

}