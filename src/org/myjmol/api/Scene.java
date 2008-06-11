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
package org.myjmol.api;

import java.util.HashMap;
import java.util.Map;

import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

/**
 * @author Charles Xie
 * 
 */
public class Scene {

	private Point3f cameraPosition;
	private Vector3f rotationAxis;
	private float rotationAngle;
	private float zoomPercent = 100;
	private String departureScript;
	private String arrivalScript;
	private String departureInfo;
	private String arrivalInfo;
	private short stopTime = 1;
	private short transitionTime = 3;
	private Map<Object, Object> properties;
	private Scene previous;
	private float xTrans, yTrans;

	public Scene(Point3f cameraPosition, Vector3f rotationAxis, float rotationAngle, float zoomPercent) {
		if (cameraPosition != null) {
			this.cameraPosition = new Point3f(cameraPosition);
		}
		else {
			cameraPosition = new Point3f();
		}
		this.rotationAxis = new Vector3f(rotationAxis);
		this.rotationAngle = rotationAngle;
		this.zoomPercent = zoomPercent;
	}

	public void setPrevious(Scene s) {
		previous = s;
	}

	public Scene getPrevious() {
		return previous;
	}

	public void setProperty(Object key, Object val) {
		if (properties == null)
			properties = new HashMap<Object, Object>();
		properties.put(key, val);
	}

	public Object getProperty(Object key) {
		if (properties == null)
			return null;
		return properties.get(key);
	}

	public void setTransitionTime(short transitionTime) {
		this.transitionTime = transitionTime;
	}

	public short getTransitionTime() {
		return transitionTime;
	}

	public void setStopTime(short stopTime) {
		this.stopTime = stopTime;
	}

	public short getStopTime() {
		return stopTime;
	}

	public void setDepartInformation(String s) {
		departureInfo = s;
	}

	public String getDepartInformation() {
		return departureInfo;
	}

	public void setArriveInformation(String s) {
		arrivalInfo = s;
	}

	public String getArriveInformation() {
		return arrivalInfo;
	}

	public void setDepartScript(String s) {
		departureScript = s;
	}

	public String getDepartScript() {
		return departureScript;
	}

	public void setArriveScript(String s) {
		arrivalScript = s;
	}

	public String getArriveScript() {
		return arrivalScript;
	}

	public Point3f getCameraPosition() {
		return cameraPosition;
	}

	public Vector3f getRotationAxis() {
		return rotationAxis;
	}

	public void setRotationAngle(float angle) {
		rotationAngle = angle;
	}

	public float getRotationAngle() {
		return rotationAngle;
	}

	public void setZoomPercent(float zoomPercent) {
		this.zoomPercent = zoomPercent;
	}

	public float getZoomPercent() {
		return zoomPercent;
	}

	/** if not in the navigation mode, store the percentage of translation in the x direction */
	public void setXTrans(float xTrans) {
		this.xTrans = xTrans;
	}

	/** if not in the navigation mode, returns the percentage of translation in the x direction */
	public float getXTrans() {
		return xTrans;
	}

	/** if not in the navigation mode, store the percentage of translation in the y direction */
	public void setYTrans(float yTrans) {
		this.yTrans = yTrans;
	}

	/** if not in the navigation mode, returns the percentage of translation in the y direction */
	public float getYTrans() {
		return yTrans;
	}

	public String rotationToString() {
		return rotationAxis.x + " " + rotationAxis.y + " " + rotationAxis.z + " " + rotationAngle + " " + zoomPercent;
	}

	public String toString() {
		return cameraPosition.x + " " + cameraPosition.y + " " + cameraPosition.z + " " + rotationToString();
	}

}