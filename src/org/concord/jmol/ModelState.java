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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.concord.modeler.draw.FillMode;
import org.jmol.api.InteractionCenter;
import org.jmol.api.SiteAnnotation;

/**
 * @author Charles Xie
 * 
 */
public class ModelState implements Serializable {

	private String title;
	private String subtitle;
	private Map<Integer, SiteAnnotation> atomAnnotations, bondAnnotations;
	private Map<Integer, InteractionCenter> atomInteractions, bondInteractions;
	private SceneState startingScene;
	private List<SceneState> itinerary;
	private FillMode fillMode = FillMode.getNoFillMode();
	private boolean hover;
	private byte axisStyle;
	private int zDepthMagnification = 5;
	private boolean showAnimationControls;
	private boolean pauliRepulsionForAllAtoms;
	private boolean blinkInteractionCenters = true;
	private boolean roverMode;
	private float roverCharge = -1;
	private float roverDipole;
	private float roverMass = 2;
	private float roverMomentOfInertia = 100;
	private float roverFriction = 0.5f;
	private byte roverTurning = Rover.TURN_TO_VELOCITY;
	private int roverRgb = 0xffcccccc;
	private float chasePlaneDistance = 2;

	public ModelState() {
	}

	public void setAtomInteractions(Map<Integer, InteractionCenter> interactions) {
		atomInteractions = interactions;
	}

	public Map<Integer, InteractionCenter> getAtomInteractions() {
		return atomInteractions;
	}

	public void setBondInteractions(Map<Integer, InteractionCenter> interactions) {
		bondInteractions = interactions;
	}

	public Map<Integer, InteractionCenter> getBondInteractions() {
		return bondInteractions;
	}

	public void setAtomAnnotations(Map<Integer, SiteAnnotation> annotations) {
		atomAnnotations = annotations;
	}

	public Map<Integer, SiteAnnotation> getAtomAnnotations() {
		return atomAnnotations;
	}

	public void setBondAnnotations(Map<Integer, SiteAnnotation> annotations) {
		bondAnnotations = annotations;
	}

	public Map<Integer, SiteAnnotation> getBondAnnotations() {
		return bondAnnotations;
	}

	public void setZDepthMagnification(int i) {
		zDepthMagnification = i;
	}

	public int getZDepthMagnification() {
		return zDepthMagnification;
	}

	public void setHoverEnabled(boolean b) {
		hover = b;
	}

	public boolean isHoverEnabled() {
		return hover;
	}

	public void setAxisStyle(byte style) {
		axisStyle = style;
	}

	public byte getAxisStyle() {
		return axisStyle;
	}

	public void setStartingScene(SceneState ss) {
		startingScene = ss;
	}

	public SceneState getStartingScene() {
		return startingScene;
	}

	public void setFillMode(FillMode fillMode) {
		this.fillMode = fillMode;
	}

	public FillMode getFillMode() {
		return fillMode;
	}

	public void setTitle(String s) {
		title = s;
	}

	public String getTitle() {
		return title;
	}

	public void setSubtitle(String s) {
		subtitle = s;
	}

	public String getSubtitle() {
		return subtitle;
	}

	public void setItinerary(List<SceneState> l) {
		itinerary = l;
	}

	public List<SceneState> getItinerary() {
		return itinerary;
	}

	public void addScene(SceneState s) {
		if (itinerary == null)
			itinerary = new ArrayList<SceneState>();
		itinerary.add(s);
	}

	public int getSceneCount() {
		if (itinerary == null)
			return 0;
		return itinerary.size();
	}

	public SceneState getSceneState(int i) {
		if (itinerary == null)
			return null;
		return itinerary.get(i);
	}

	public void setRoverMode(boolean b) {
		roverMode = b;
	}

	public boolean isRoverMode() {
		return roverMode;
	}

	public void setRoverTurning(byte option) {
		roverTurning = option;
	}

	public byte getRoverTurning() {
		return roverTurning;
	}

	public void setRoverFriction(float friction) {
		roverFriction = friction;
	}

	public float getRoverFriction() {
		return roverFriction;
	}

	public void setRoverMomentOfInertia(float i) {
		roverMomentOfInertia = i;
	}

	public float getRoverMomentOfInertia() {
		return roverMomentOfInertia;
	}

	public void setRoverMass(float mass) {
		roverMass = mass;
	}

	public float getRoverMass() {
		return roverMass;
	}

	public void setRoverCharge(float i) {
		roverCharge = i;
	}

	public float getRoverCharge() {
		return roverCharge;
	}

	public void setRoverDipole(float i) {
		roverDipole = i;
	}

	public float getRoverDipole() {
		return roverDipole;
	}

	public void setRoverRgb(int i) {
		roverRgb = i;
	}

	public int getRoverRgb() {
		return roverRgb;
	}

	public void setShowAnimationControls(boolean b) {
		showAnimationControls = b;
	}

	public boolean getShowAnimationControls() {
		return showAnimationControls;
	}

	public void setPauliRepulsionForAllAtoms(boolean b) {
		pauliRepulsionForAllAtoms = b;
	}

	public boolean getPauliRepulsionForAllAtoms() {
		return pauliRepulsionForAllAtoms;
	}

	public void setBlinkInteractionCenters(boolean b) {
		blinkInteractionCenters = b;
	}

	public boolean getBlinkInteractionCenters() {
		return blinkInteractionCenters;
	}

	public void setChasePlaneDistance(float depth) {
		chasePlaneDistance = depth;
	}

	public float getChasePlaneDistance() {
		return chasePlaneDistance;
	}

}