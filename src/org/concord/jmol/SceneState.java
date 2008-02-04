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

import java.io.Serializable;

import org.jmol.api.Scene;

/**
 * @author Charles Xie
 * 
 */
public class SceneState implements Serializable {

	private String geoData;
	private short stopTime = 1;
	private short transitionTime = 3;
	private String departScript;
	private String arriveScript;
	private String departInfo;
	private String arriveInfo;
	private byte atomColoring = JmolContainer.COLOR_ATOM_BY_ELEMENT;
	private String scheme = JmolContainer.BALL_AND_STICK;
	private byte atomSelection = JmolContainer.SELECT_ALL;

	public SceneState() {
	}

	public SceneState(Scene scene) {
		this();
		geoData = scene.toString();
		transitionTime = scene.getTransitionTime();
		stopTime = scene.getStopTime();
		departScript = scene.getDepartScript();
		arriveScript = scene.getArriveScript();
		departInfo = scene.getDepartInformation();
		arriveInfo = scene.getArriveInformation();
		Object o = scene.getProperty("scheme");
		if (o instanceof String) {
			scheme = (String) o;
		}
		o = scene.getProperty("atomcoloring");
		if (o instanceof Byte) {
			atomColoring = ((Byte) o).byteValue();
		}
		o = scene.getProperty("selection");
		if (o instanceof Byte) {
			atomSelection = ((Byte) o).byteValue();
		}
	}

	public void setScheme(String scheme) {
		this.scheme = scheme;
	}

	public String getScheme() {
		return scheme;
	}

	public void setAtomColoring(byte atomColoring) {
		this.atomColoring = atomColoring;
	}

	public byte getAtomColoring() {
		return atomColoring;
	}

	public void setAtomSelection(byte atomSelection) {
		this.atomSelection = atomSelection;
	}

	public byte getAtomSelection() {
		return atomSelection;
	}

	public void setGeoData(String s) {
		geoData = s;
	}

	public String getGeoData() {
		return geoData;
	}

	public void setTransitionTime(short t) {
		transitionTime = t;
	}

	public short getTransitionTime() {
		return transitionTime;
	}

	public void setStopTime(short t) {
		stopTime = t;
	}

	public short getStopTime() {
		return stopTime;
	}

	public void setDepartInfo(String s) {
		departInfo = s;
	}

	public String getDepartInfo() {
		return departInfo;
	}

	public void setArriveInfo(String s) {
		arriveInfo = s;
	}

	public String getArriveInfo() {
		return arriveInfo;
	}

	public void setDepartScript(String s) {
		departScript = s;
	}

	public String getDepartScript() {
		return departScript;
	}

	public void setArriveScript(String s) {
		arriveScript = s;
	}

	public String getArriveScript() {
		return arriveScript;
	}

	public String toString() {
		return geoData;
	}

}