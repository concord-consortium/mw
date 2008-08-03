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

package org.concord.mw3d;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.concord.modeler.draw.FillMode;
import org.concord.mw3d.models.ABond;
import org.concord.mw3d.models.ABondState;
import org.concord.mw3d.models.CuboidObstacle;
import org.concord.mw3d.models.CuboidObstacleState;
import org.concord.mw3d.models.CylinderObstacle;
import org.concord.mw3d.models.CylinderObstacleState;
import org.concord.mw3d.models.HeatBath;
import org.concord.mw3d.models.Obstacle;
import org.concord.mw3d.models.ObstacleState;
import org.concord.mw3d.models.RBond;
import org.concord.mw3d.models.RBondState;
import org.concord.mw3d.models.TBond;
import org.concord.mw3d.models.TBondState;

public class ModelState implements Serializable {

	private float timestep = 0.5f;
	private float length = 100;
	private float width = 50;
	private float height = 50;
	private boolean showClock = true;
	private boolean showGlassSimulationBox = true;
	private boolean fullSizeUnbondedAtoms;
	private boolean showAtomIndex;
	private boolean showCharge;
	private boolean showVdwLines;
	private float vdwLinesRatio = MolecularView.DEFAULT_VDW_LINE_RATIO;
	private boolean keShading;
	private boolean showAxes = true;
	private byte axisStyle = (byte) 1;
	private boolean navigationMode;
	private boolean perspectiveDepth = true;
	private boolean showEnergizer;
	private byte moleculeStyle = MolecularView.SPACE_FILLING;
	private short velocityVectorScalingFactor = 1000;
	private int cameraAtom = -1;
	private int zDepthMagnification = 5;

	private String rotation;
	private String cameraPosition;
	private String velocitySelection;
	private String trajectorySelection;
	private String unmovableSelection;
	private String translucentSelection;
	private String initScript;

	private FillMode fillMode = FillMode.getNoFillMode();
	private HeatBath heatBath;
	private List drawList;
	private List<ObstacleState> obstacles;
	private List<RBondState> rbonds;
	private List<ABondState> abonds;
	private List<TBondState> tbonds;

	private float gravitationalAcceleration;
	private float[] gFieldDirection;
	private float bFieldIntensity;
	private float[] bFieldDirection;
	private float eFieldIntensity;
	private float[] eFieldDirection;

	public ModelState() {
	}

	public void setInitScript(String s) {
		initScript = s;
	}

	public String getInitScript() {
		return initScript;
	}

	public void setZDepthMagnification(int i) {
		zDepthMagnification = i;
	}

	public int getZDepthMagnification() {
		return zDepthMagnification;
	}

	public void setCameraAtom(int i) {
		cameraAtom = i;
	}

	public int getCameraAtom() {
		return cameraAtom;
	}

	public void setNavigationMode(boolean b) {
		navigationMode = b;
	}

	public boolean getNavigationMode() {
		return navigationMode;
	}

	public void setCameraPosition(String s) {
		cameraPosition = s;
	}

	public String getCameraPosition() {
		return cameraPosition;
	}

	public void setTimeStep(float timestep) {
		this.timestep = timestep;
	}

	public float getTimeStep() {
		return timestep;
	}

	public void setRBonds(List<RBondState> rbonds) {
		this.rbonds = rbonds;
	}

	public List<RBondState> getRBonds() {
		return rbonds;
	}

	public void addRBond(RBond rbond) {
		if (rbonds == null)
			rbonds = new ArrayList<RBondState>();
		rbonds.add(new RBondState(rbond));
	}

	public void setABonds(List<ABondState> abonds) {
		this.abonds = abonds;
	}

	public List<ABondState> getABonds() {
		return abonds;
	}

	public void addABond(ABond abond) {
		if (abonds == null)
			abonds = new ArrayList<ABondState>();
		abonds.add(new ABondState(abond));
	}

	public void setTBonds(List<TBondState> tbonds) {
		this.tbonds = tbonds;
	}

	public List<TBondState> getTBonds() {
		return tbonds;
	}

	public void addTBond(TBond tbond) {
		if (tbonds == null)
			tbonds = new ArrayList<TBondState>();
		tbonds.add(new TBondState(tbond));
	}

	public void setObstacles(List<ObstacleState> o) {
		obstacles = o;
	}

	public List<ObstacleState> getObstacles() {
		return obstacles;
	}

	public void addObstacle(Obstacle o) {
		if (obstacles == null)
			obstacles = new ArrayList<ObstacleState>();
		if (o instanceof CuboidObstacle) {
			CuboidObstacle c = (CuboidObstacle) o;
			CuboidObstacleState s = new CuboidObstacleState();
			s.setCenter(c.getCenter());
			s.setCorner(c.getCorner());
			s.setColor(c.getColor().getRGB());
			s.setTranslucent(c.isTranslucent());
			obstacles.add(s);
		}
		else if (o instanceof CylinderObstacle) {
			CylinderObstacle c = (CylinderObstacle) o;
			CylinderObstacleState s = new CylinderObstacleState();
			s.setCenter(c.getCenter());
			s.setAxis(c.getAxis());
			s.setHeight(c.getHeight());
			s.setRadius(c.getRadius());
			s.setColor(c.getColor().getRGB());
			s.setTranslucent(c.isTranslucent());
			obstacles.add(s);
		}
	}

	public void setHeatBath(HeatBath heatBath) {
		this.heatBath = heatBath;
	}

	public HeatBath getHeatBath() {
		return heatBath;
	}

	public void setLength(float length) {
		this.length = length;
	}

	public float getLength() {
		return length;
	}

	public void setWidth(float width) {
		this.width = width;
	}

	public float getWidth() {
		return width;
	}

	public void setHeight(float height) {
		this.height = height;
	}

	public float getHeight() {
		return height;
	}

	public void setFillMode(FillMode fillMode) {
		this.fillMode = fillMode;
	}

	public FillMode getFillMode() {
		return fillMode;
	}

	public void setPerspectiveDepth(boolean b) {
		perspectiveDepth = b;
	}

	public boolean getPerspectiveDepth() {
		return perspectiveDepth;
	}

	public void setRotation(String s) {
		rotation = s;
	}

	public String getRotation() {
		return rotation;
	}

	public void setMoleculeStyle(byte style) {
		moleculeStyle = style;
	}

	public byte getMoleculeStyle() {
		return moleculeStyle;
	}

	public void setShowGlassSimulationBox(boolean b) {
		showGlassSimulationBox = b;
	}

	public boolean getShowGlassSimulationBox() {
		return showGlassSimulationBox;
	}

	public void setShowClock(boolean b) {
		showClock = b;
	}

	public boolean getShowClock() {
		return showClock;
	}

	public void setShowAxes(boolean b) {
		showAxes = b;
	}

	public boolean getShowAxes() {
		return showAxes;
	}

	public void setAxisStyle(byte style) {
		axisStyle = style;
	}

	public byte getAxisStyle() {
		return axisStyle;
	}

	public void setKeShading(boolean b) {
		keShading = b;
	}

	public boolean getKeShading() {
		return keShading;
	}

	public void setFullSizeUnbondedAtoms(boolean b) {
		fullSizeUnbondedAtoms = b;
	}

	public boolean getFullSizeUnbondedAtoms() {
		return fullSizeUnbondedAtoms;
	}

	public void setShowVdwLines(boolean b) {
		showVdwLines = b;
	}

	public boolean getShowVdwLines() {
		return showVdwLines;
	}

	public void setVdwLinesRatio(float ratio) {
		vdwLinesRatio = ratio;
	}

	public float getVdwLinesRatio() {
		return vdwLinesRatio;
	}

	public void setVelocityVectorScalingFactor(short factor) {
		velocityVectorScalingFactor = factor;
	}

	public short getVelocityVectorScalingFactor() {
		return velocityVectorScalingFactor;
	}

	public void setShowCharge(boolean b) {
		showCharge = b;
	}

	public boolean getShowCharge() {
		return showCharge;
	}

	public void setShowAtomIndex(boolean b) {
		showAtomIndex = b;
	}

	public boolean getShowAtomIndex() {
		return showAtomIndex;
	}

	public void setShowEnergizer(boolean b) {
		showEnergizer = b;
	}

	public boolean getShowEnergizer() {
		return showEnergizer;
	}

	public void setTranslucentSelection(String s) {
		translucentSelection = s;
	}

	public String getTranslucentSelection() {
		return translucentSelection;
	}

	public void setVelocitySelection(String s) {
		velocitySelection = s;
	}

	public String getVelocitySelection() {
		return velocitySelection;
	}

	public void setUnmovableSelection(String s) {
		unmovableSelection = s;
	}

	public String getUnmovableSelection() {
		return unmovableSelection;
	}

	public void setTrajectorySelection(String s) {
		trajectorySelection = s;
	}

	public String getTrajectorySelection() {
		return trajectorySelection;
	}

	public void setDrawList(List drawList) {
		this.drawList = drawList;
	}

	public List getDrawList() {
		return drawList;
	}

	public void setGravitationalAcceleration(float gravitationalAcceleration) {
		this.gravitationalAcceleration = gravitationalAcceleration;
	}

	public float getGravitationalAcceleration() {
		return gravitationalAcceleration;
	}

	public void setGFieldDirection(float[] gFieldDirection) {
		this.gFieldDirection = gFieldDirection;
	}

	public float[] getGFieldDirection() {
		return gFieldDirection;
	}

	public void setBFieldIntensity(float bFieldIntensity) {
		this.bFieldIntensity = bFieldIntensity;
	}

	public float getBFieldIntensity() {
		return bFieldIntensity;
	}

	public void setBFieldDirection(float[] bFieldDirection) {
		this.bFieldDirection = bFieldDirection;
	}

	public float[] getBFieldDirection() {
		return bFieldDirection;
	}

	public void setEFieldIntensity(float eFieldIntensity) {
		this.eFieldIntensity = eFieldIntensity;
	}

	public float getEFieldIntensity() {
		return eFieldIntensity;
	}

	public void setEFieldDirection(float[] eFieldDirection) {
		this.eFieldDirection = eFieldDirection;
	}

	public float[] getEFieldDirection() {
		return eFieldDirection;
	}

}