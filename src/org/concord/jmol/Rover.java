/*
 *   Copyright (C) 2007  The Concord Consortium, Inc.,
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

/**
 * @author Charles Xie
 * 
 */
class Rover {

	final static byte TURN_TO_VELOCITY = 0;
	final static byte TURN_TO_FORCE = 1;

	private int rgb = 0xffcccccc;
	private float mass = 2;
	private float momentOfInertia = 100;
	private float charge = -1;
	private float dipole;
	private float friction = 0.5f;
	private float diameter = 4;
	private byte turningOption = TURN_TO_FORCE;
	private float chasePlaneDistance = 2;

	void reset() {
		rgb = 0xffcccccc;
		charge = -1;
		dipole = 0;
		mass = 2;
		momentOfInertia = 100;
		friction = 0.5f;
		diameter = 4;
		turningOption = TURN_TO_FORCE;
		chasePlaneDistance = 2;
	}

	void setChasePlaneDistance(float depth) {
		chasePlaneDistance = depth;
	}

	float getChasePlaneDistance() {
		return chasePlaneDistance;
	}

	void setTurningOption(byte option) {
		turningOption = option;
	}

	byte getTurningOption() {
		return turningOption;
	}

	void setDiameter(float d) {
		diameter = d;
	}

	float getDiameter() {
		return diameter;
	}

	void setFriction(float friction) {
		this.friction = friction;
	}

	float getFriction() {
		return friction;
	}

	void setMass(float mass) {
		this.mass = mass;
	}

	float getMass() {
		return mass;
	}

	void setMomentOfInertia(float i) {
		momentOfInertia = i;
	}

	float getMomentOfInertia() {
		return momentOfInertia;
	}

	void setCharge(float i) {
		charge = i;
	}

	float getCharge() {
		return charge;
	}

	void setDipole(float dipole) {
		this.dipole = dipole;
	}

	float getDipole() {
		return dipole;
	}

	void setColor(int rgb) {
		this.rgb = rgb;
	}

	int getColor() {
		return rgb;
	}

}
