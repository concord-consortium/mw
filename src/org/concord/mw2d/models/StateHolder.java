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

package org.concord.mw2d.models;

import java.util.List;

class StateHolder {

	private float time;
	private int numberOfParticles;
	private double heatBathTemperature;
	private double[] particleData;
	private int numberOfObstacles;
	private double[] obstacleData;
	private List bonds;

	void destroy() {
		if (bonds != null) {
			bonds.clear();
			bonds = null;
		}
		particleData = null;
		obstacleData = null;
	}

	StateHolder(float time, double heatBathTemperature, int numberOfParticles, double[] particleData) {
		this.time = time;
		this.heatBathTemperature = heatBathTemperature;
		this.numberOfParticles = numberOfParticles;
		this.particleData = particleData;
	}

	StateHolder(float time, double heatBathTemperature, int numberOfParticles, double[] particleData,
			int numberOfObstacles, double[] obstacleData) {
		this(time, heatBathTemperature, numberOfParticles, particleData);
		this.numberOfObstacles = numberOfObstacles;
		this.obstacleData = obstacleData;
	}

	StateHolder(float time, double heatBathTemperature, int numberOfParticles, double[] particleData,
			int numberOfObstacles, double[] obstacleData, List bonds) {
		this(time, heatBathTemperature, numberOfParticles, particleData);
		this.numberOfObstacles = numberOfObstacles;
		this.obstacleData = obstacleData;
		this.bonds = bonds;
	}

	float getTime() {
		return time;
	}

	double getHeatBathTemperature() {
		return heatBathTemperature;
	}

	int getNumberOfParticles() {
		return numberOfParticles;
	}

	double[] getParticleData() {
		return particleData;
	}

	int getNumberOfObstacles() {
		return numberOfObstacles;
	}

	double[] getObstacleData() {
		return obstacleData;
	}

	void setBonds(List bonds) {
		this.bonds = bonds;
	}

	List getBonds() {
		return bonds;
	}

}