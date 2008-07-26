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
package org.myjmol.viewer;

import java.awt.Color;
import java.awt.Component;
import java.awt.Polygon;
import java.util.BitSet;
import java.util.Iterator;

import javax.vecmath.AxisAngle4f;
import javax.vecmath.Matrix3f;
import javax.vecmath.Matrix4f;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

import org.myjmol.api.JmolAdapter;
import org.myjmol.api.Pair;
import org.myjmol.g3d.Graphics3D;

/**
 * @author Charles Xie
 * 
 */
public class ExtendedViewer extends Viewer {

	final static float MIN_OBSTACLE_SIZE = 0.5f;
	private final static float COS45 = 0.70710678f;

	boolean aBondRendered, tBondRendered;
	boolean simulationBoxVisible = true;
	private int axisDiameter = -1;
	private boolean showAtomIndex;
	private boolean showCharge;
	private short velocityScalingFactor = 1000;
	private Plane dropPlane;
	private Plane highlightPlane;
	private Triangle highlightTriangle;
	private Ellipse highlightEllipse;
	private Cylinder highlightCylinder;
	private TBond highlightTBond;
	private Cuboid vectorBox;
	private Cuboid rectangle;
	private short[] obstacleIndexAndFace = new short[] { -1, -1 };
	private Object3D selectedObstacle;
	private Cylinder ellipse;

	BitSet velocityBitSet;
	BitSet trajectoryBitSet;
	BitSet translucentBitSet;
	BitSet hidenBitSet;
	int iAtomOfSelectedVelocity = -1;

	private AxisAngle4f aaMoveTo;
	private Matrix3f rotationTransform;

	private Matrix4f tempMatrix4f;
	private Point3f pmin, pmax, planePoint;

	private static Polygon polygon;

	ExtendedViewer(Component c, JmolAdapter modelAdapter) {
		super(c, modelAdapter);
	}

	public void setABondRendered(boolean b) {
		aBondRendered = b;
	}

	public void setTBondRendered(boolean b) {
		tBondRendered = b;
	}

	public void setMarBond(short marBond) {
		super.setMarBond(marBond);
		if (modelManager.frame == null)
			return;
		int n = modelManager.frame.bondCount;
		if (n <= 0)
			return;
		Bond[] bonds = modelManager.frame.bonds;
		for (int i = 0; i < n; i++) {
			bonds[i].setMad(marBond);
		}
	}

	/** Note that this method is different from setPercentVdwAtom(int percentVdwAtom) */
	public void setCpkPercent(int index, int percentage) {
		if (modelManager.frame == null)
			return;
		int n = modelManager.frame.atomCount;
		if (n <= 0)
			return;
		if (index < 0 || index >= n)
			return;
		Atom[] atoms = modelManager.frame.atoms;
		if (percentage == 100) {
			atoms[index].setMadAtom((short) -100);
		}
		else {
			atoms[index].setMadAtom(getMadAtom());
		}
	}

	/** A simplified instant moveTo */
	public void moveTo(float axisX, float axisY, float axisZ, float degrees, int zoom, int dx, int dy) {
		if (aaMoveTo == null) {
			aaMoveTo = new AxisAngle4f();
			rotationTransform = new Matrix3f();
		}
		rotationTransform.setIdentity();
		if (Math.abs(axisX) > 0.000001 || Math.abs(axisY) > 0.000001 || Math.abs(axisZ) > 0.000001) {
			aaMoveTo.set(axisX, axisY, axisZ, degrees * (float) Math.PI / 180);
			rotationTransform.set(aaMoveTo);
		}
		transformManager.zoomToPercent(zoom);
		transformManager.translateToXPercent(dx);
		transformManager.translateToYPercent(dy);
		transformManager.setRotation(rotationTransform);
	}

	public boolean isAxisFlipped(char axis) {
		transformManager.getRotation(rotationTransform);
		switch (axis) {
		case 'x':
			return rotationTransform.m00 < 0;
		case 'y':
			return rotationTransform.m11 < 0;
		case 'z':
			return rotationTransform.m22 < 0;
		}
		return false;
	}

	public void clear() {
		((ExtendedFrameRenderer) repaintManager.frameRenderer).clear();
		super.clear();
	}

	private void clearVectorBox() {
		Cuboids cuboids = ((ExtendedFrameRenderer) repaintManager.frameRenderer).cuboids;
		if (vectorBox != null) {
			cuboids.removeCuboid(vectorBox);
		}
		Planes planes = ((ExtendedFrameRenderer) repaintManager.frameRenderer).planes;
		if (highlightPlane != null)
			planes.removePlane(highlightPlane);
	}

	public void removeAll() {
		super.removeAll();
		clear();
		// forceRefresh();
	}

	public void setIndexOfAtomOfSelectedVelocity(int i) {
		iAtomOfSelectedVelocity = i;
		if (i == -1)
			clearVectorBox();
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

	public void setAxisDiameter(int i) {
		axisDiameter = i;
	}

	public int getAxisDiameter() {
		return axisDiameter;
	}

	public void setVelocityVectorScalingFactor(short s) {
		velocityScalingFactor = s;
	}

	short getVelocityVectorScalingFactor() {
		return velocityScalingFactor;
	}

	public void setAtomVelocities(int index, float vx, float vy, float vz) {
		Frame frame = modelManager.frame;
		if (frame == null)
			return;
		if (index >= frame.atomCount)
			return;
		if (frame.vibrationVectors == null)
			frame.vibrationVectors = new Vector3f[frame.atoms.length];
		if (frame.vibrationVectors[index] == null) {
			frame.vibrationVectors[index] = new Vector3f(vx, vy, vz);
		}
		else {
			frame.vibrationVectors[index].set(vx, vy, vz);
		}
	}

	public void setCharge(int index, float charge) {
		Frame frame = modelManager.frame;
		if (frame == null)
			return;
		if (frame.partialCharges == null)
			frame.partialCharges = new float[frame.atoms.length];
		modelManager.frame.partialCharges[index] = charge;
	}

	public void clearABonds() {
		ABonds abonds = ((ExtendedFrameRenderer) repaintManager.frameRenderer).abonds;
		abonds.clear();
	}

	public void clearTBonds() {
		TBonds tbonds = ((ExtendedFrameRenderer) repaintManager.frameRenderer).tbonds;
		tbonds.clear();
	}

	public void removeAtoms(BitSet bs) {
		Frame frame = modelManager.frame;
		if (frame == null)
			return;
		super.removeAtoms(bs);
		ABonds abonds = ((ExtendedFrameRenderer) repaintManager.frameRenderer).abonds;
		int n = abonds.count();
		if (n > 0) {
			ABond abond;
			synchronized (abonds.getLock()) {
				for (Iterator it = abonds.iterator(); it.hasNext();) {
					abond = (ABond) it.next();
					if (bs.get(abond.atom1) || bs.get(abond.atom2) || bs.get(abond.atom3)) {
						it.remove();
					}
				}
			}
		}
		TBonds tbonds = ((ExtendedFrameRenderer) repaintManager.frameRenderer).tbonds;
		n = tbonds.count();
		if (n > 0) {
			TBond tbond;
			synchronized (tbonds.getLock()) {
				for (Iterator it = tbonds.iterator(); it.hasNext();) {
					tbond = (TBond) it.next();
					if (bs.get(tbond.atom1) || bs.get(tbond.atom2) || bs.get(tbond.atom3) || bs.get(tbond.atom4)) {
						it.remove();
					}
				}
			}
		}
	}

	public void addRBond(Object atomUid1, Object atomUid2) {
		super.addRBond(atomUid1, atomUid2);
		if (modelManager.frame != null) {
			int n = modelManager.frame.bondCount;
			if (n > 0) {
				Bond bond = modelManager.frame.bonds[n - 1];
				if (bond != null)
					bond.mad = (short) (getMadBond() >> 1);
			}
		}
	}

	public void removeRBond(int index) {
		Bond[] bonds = modelManager.frame.bonds;
		if (bonds == null)
			return;
		if (index < 0 || index >= modelManager.frame.bondCount)
			return;
		Bond bond = bonds[index];
		int atom1 = bond.atom1.atomIndex;
		int atom2 = bond.atom2.atomIndex;
		modelManager.frame.deleteBond(bond);
		ABonds abonds = ((ExtendedFrameRenderer) repaintManager.frameRenderer).abonds;
		int n = abonds.count();
		if (n > 0) {
			ABond abond;
			synchronized (abonds.getLock()) {
				for (Iterator it = abonds.iterator(); it.hasNext();) {
					abond = (ABond) it.next();
					if (atom1 == abond.atom1 || atom1 == abond.atom2 || atom1 == abond.atom3 || atom2 == abond.atom1
							|| atom2 == abond.atom2 || atom2 == abond.atom3) {
						it.remove();
					}
				}
			}
		}
		TBonds tbonds = ((ExtendedFrameRenderer) repaintManager.frameRenderer).tbonds;
		n = tbonds.count();
		if (n > 0) {
			TBond tbond;
			synchronized (tbonds.getLock()) {
				for (Iterator it = tbonds.iterator(); it.hasNext();) {
					tbond = (TBond) it.next();
					if (atom1 == tbond.atom1 || atom1 == tbond.atom2 || atom1 == tbond.atom3 || atom1 == tbond.atom4
							|| atom2 == tbond.atom1 || atom2 == tbond.atom2 || atom2 == tbond.atom3
							|| atom2 == tbond.atom4) {
						it.remove();
					}
				}
			}
		}
	}

	public void addABond(int i, int j, int k) {
		ABonds abonds = ((ExtendedFrameRenderer) repaintManager.frameRenderer).abonds;
		abonds.addABond(new ABond(i, j, k));
	}

	public void removeABond(int index) {
		ABonds abonds = ((ExtendedFrameRenderer) repaintManager.frameRenderer).abonds;
		ABond abond = abonds.getABond(index);
		abonds.removeABond(index);
		int a1 = abond.atom1;
		int a2 = abond.atom2;
		int a3 = abond.atom3;
		TBonds tbonds = ((ExtendedFrameRenderer) repaintManager.frameRenderer).tbonds;
		if (tbonds.count() > 0) {
			TBond tbond;
			synchronized (tbonds.getLock()) {
				for (Iterator it = tbonds.iterator(); it.hasNext();) {
					tbond = (TBond) it.next();
					if (a1 == tbond.atom1 || a1 == tbond.atom2 || a1 == tbond.atom3 || a1 == tbond.atom4
							|| a2 == tbond.atom1 || a2 == tbond.atom2 || a2 == tbond.atom3 || a2 == tbond.atom4
							|| a3 == tbond.atom1 || a3 == tbond.atom2 || a3 == tbond.atom3 || a3 == tbond.atom4) {
						it.remove();
					}
				}
			}
		}
	}

	public void addTBond(int i, int j, int k, int l) {
		TBonds tbonds = ((ExtendedFrameRenderer) repaintManager.frameRenderer).tbonds;
		tbonds.addTBond(new TBond(i, j, k, l));
	}

	public void removeTBond(int index) {
		TBonds tbonds = ((ExtendedFrameRenderer) repaintManager.frameRenderer).tbonds;
		tbonds.removeTBond(index);
	}

	void setVectorBox(Atom a, Vector3f v, float s) {
		if (vectorBox == null) {
			vectorBox = new Cuboid(0, 0, 0, 0, 0, 0);
			vectorBox.setMode(Cuboid.LINE_MODE);
		}
		vectorBox.setCenter(a.getAtomX() + v.x * s, a.getAtomY() + v.y * s, a.getAtomZ() + v.z * s);
		vectorBox.setCorner(v.x > 0 ? Math.max(0.25f, v.x * s) : Math.min(-0.25f, v.x * s), v.y > 0 ? Math.max(0.25f,
				v.y * s) : Math.min(-0.25f, v.y * s), v.z > 0 ? Math.max(0.25f, v.z * s) : Math.min(-0.25f, v.z * s));
	}

	public void setSimulationBoxVisible(boolean b) {
		simulationBoxVisible = b;
	}

	/**
	 * This is not the bounding box determined from the max and min coordinates of the atoms. This is the simulation
	 * box, within which atoms will move.
	 */
	public void setSimulationBox(float xlen, float ylen, float zlen) {
		Cuboids c = ((ExtendedFrameRenderer) repaintManager.frameRenderer).cuboids;
		if (c.isEmpty()) {
			SimulationBox box = new SimulationBox(xlen, ylen, zlen);
			c.addCuboid(box);
		}
		else {
			Cuboid first = c.getCuboid(0);
			if (first instanceof SimulationBox) {
				first.setCorner(xlen * 0.5f, ylen * 0.5f, zlen * 0.5f);
			}
			else {
				throw new RuntimeException("The first cuboid is not simulation box.");
			}
		}
	}

	public void updateSimulationBoxFace(byte face) {
		if (face < 0)
			return;
		if (dropPlane == null)
			return;
		Cuboids c = ((ExtendedFrameRenderer) repaintManager.frameRenderer).cuboids;
		SimulationBox box = (SimulationBox) c.getCuboid(0);
		Planes planes = ((ExtendedFrameRenderer) repaintManager.frameRenderer).planes;
		dropPlane.setVertices(box.getFaceVertices(face));
		planes.addPlane(dropPlane);
		dropPlane.setColix(Graphics3D.getTranslucentColix(Graphics3D.HOTPINK));
		dropPlane.setMode(Plane.FILL_WITH_DOTS_MODE);
	}

	public byte getSimulationBoxFace(int x, int y) {
		Cuboids c = ((ExtendedFrameRenderer) repaintManager.frameRenderer).cuboids;
		SimulationBox box = (SimulationBox) c.getCuboid(0);
		Planes planes = ((ExtendedFrameRenderer) repaintManager.frameRenderer).planes;
		initDropPlane();
		byte face = box.getFace(this, x, y);
		if (face == -1) {
			planes.removePlane(dropPlane);
		}
		else {
			updateSimulationBoxFace(face);
		}
		return face;
	}

	public void updateVectorBoxFace(byte face) {
		if (face < 0)
			return;
		if (highlightPlane == null || vectorBox == null)
			return;
		highlightPlane.setVertices(vectorBox.getFaceVertices(face));
		Planes planes = ((ExtendedFrameRenderer) repaintManager.frameRenderer).planes;
		planes.addPlane(highlightPlane);
	}

	public byte getVectorBoxFace(int x, int y) {
		if (vectorBox == null)
			return -1;
		initHighlightPlane();
		Planes planes = ((ExtendedFrameRenderer) repaintManager.frameRenderer).planes;
		Cuboids c = ((ExtendedFrameRenderer) repaintManager.frameRenderer).cuboids;
		byte face = vectorBox.getFace(this, x, y);
		if (face == -1) {
			c.removeCuboid(vectorBox);
			planes.removePlane(highlightPlane);
			BallsRenderer.drawVectorComponents = false;
		}
		else {
			highlightPlane.setColix(Graphics3D.TEAL);
			c.addCuboid(vectorBox);
			updateVectorBoxFace(face);
			BallsRenderer.drawVectorComponents = true;
		}
		return face;
	}

	public void setHighlightPlaneVisible(boolean b) {
		Planes planes = ((ExtendedFrameRenderer) repaintManager.frameRenderer).planes;
		if (b) {
			initHighlightPlane();
			planes.addPlane(highlightPlane);
		}
		else {
			planes.removePlane(highlightPlane);
		}
	}

	public void setHighlightTriangleVertices(Point3f p1, Point3f p2, Point3f p3) {
		initHighlightTriangle();
		highlightTriangle.setVertices(p1, p2, p3);
	}

	public void setHighlightTriangleVisible(boolean b) {
		Triangles triangles = ((ExtendedFrameRenderer) repaintManager.frameRenderer).triangles;
		if (b) {
			initHighlightTriangle();
			triangles.addTriangle(highlightTriangle);
		}
		else {
			triangles.removeTriangle(highlightTriangle);
		}
	}

	public void setHighlightCylinderVisible(boolean b) {
		Cylinders cylinders = ((ExtendedFrameRenderer) repaintManager.frameRenderer).cylinders;
		if (b) {
			initHighlightCylinder();
			cylinders.addCylinder(highlightCylinder);
		}
		else {
			cylinders.removeCylinder(highlightCylinder);
		}
	}

	public void setHighlightTBondVisible(boolean b) {
		TBonds tbonds = ((ExtendedFrameRenderer) repaintManager.frameRenderer).tbonds;
		if (b) {
			initHighlightTBond();
			tbonds.addTBond(highlightTBond);
		}
		else {
			tbonds.removeTBond(highlightTBond);
		}
	}

	public void setHighlightTBond(int i, int j, int k, int l) {
		initHighlightTBond();
		highlightTBond.atom1 = i;
		highlightTBond.atom2 = j;
		highlightTBond.atom3 = k;
		highlightTBond.atom4 = l;
	}

	public int findNearestBondIndex(int x, int y) {
		if (modelManager.frame == null)
			return -1;
		Bond[] bonds = modelManager.frame.bonds;
		if (bonds == null)
			return -1;
		initHighlightCylinder();
		Cylinders cylinders = ((ExtendedFrameRenderer) repaintManager.frameRenderer).cylinders;
		cylinders.removeCylinder(highlightCylinder);
		int foundIndex = super.findNearestBondIndex(x, y);
		if (foundIndex >= 0) {
			highlightCylinder.setEnds(bonds[foundIndex].atom1, bonds[foundIndex].atom2);
			highlightCylinder.a = bonds[foundIndex].mad * 0.0008f;
			highlightCylinder.b = bonds[foundIndex].mad * 0.0008f;
			highlightCylinder.height = 10;
			highlightCylinder.axis = '0';
			cylinders.addCylinder(highlightCylinder);
		}
		return foundIndex;
	}

	public int findNearestABondIndex(int x, int y) {
		ABonds abonds = ((ExtendedFrameRenderer) repaintManager.frameRenderer).abonds;
		if (abonds == null)
			return -1;
		int n = 0;
		synchronized (abonds.getLock()) {
			n = abonds.count();
		}
		if (n <= 0)
			return -1;
		initHighlightTriangle();
		Triangles triangles = ((ExtendedFrameRenderer) repaintManager.frameRenderer).triangles;
		triangles.removeTriangle(highlightTriangle);
		int zmin = Integer.MAX_VALUE;
		ABond abond;
		int zDepth;
		int foundIndex = -1;
		Atom[] atom = modelManager.frame.atoms;
		if (polygon == null)
			polygon = new Polygon();
		Atom a1, a2, a3;
		synchronized (abonds.getLock()) {
			for (int i = 0; i < n; i++) {
				abond = abonds.getABond(i);
				a1 = atom[abond.atom1];
				a2 = atom[abond.atom2];
				a3 = atom[abond.atom3];
				polygon.reset();
				polygon.addPoint(a1.screenX, a1.screenY);
				polygon.addPoint(a2.screenX, a2.screenY);
				polygon.addPoint(a3.screenX, a3.screenY);
				if (polygon.contains(x, y)) {
					zDepth = a1.screenZ + a2.screenZ + a3.screenZ;
					if (zDepth < zmin) {
						zmin = zDepth;
						foundIndex = i;
					}
				}
			}
		}
		if (foundIndex >= 0) {
			triangles.addTriangle(highlightTriangle);
			abond = abonds.getABond(foundIndex);
			highlightTriangle.setVertices(atom[abond.atom1], atom[abond.atom2], atom[abond.atom3]);
		}
		return foundIndex;
	}

	public int findNearestTBondIndex(int x, int y) {
		TBonds tbonds = ((ExtendedFrameRenderer) repaintManager.frameRenderer).tbonds;
		if (tbonds == null || tbonds.isEmpty())
			return -1;
		initHighlightTBond();
		tbonds.removeTBond(highlightTBond);
		int zmin = Integer.MAX_VALUE;
		TBond tbond = null;
		TBond tbondFound = null;
		int zDepth;
		int foundIndex = -1;
		Atom[] atom = modelManager.frame.atoms;
		if (polygon == null)
			polygon = new Polygon();
		Atom a1, a2, a3, a4;
		synchronized (tbonds.getLock()) {
			for (Iterator it = tbonds.iterator(); it.hasNext();) {
				tbond = (TBond) it.next();
				if (tbond == highlightTBond)
					continue;
				a1 = atom[tbond.atom1];
				a2 = atom[tbond.atom2];
				a3 = atom[tbond.atom3];
				a4 = atom[tbond.atom4];
				polygon.reset();
				polygon.addPoint(a1.screenX, a1.screenY);
				polygon.addPoint(a2.screenX, a2.screenY);
				polygon.addPoint(a3.screenX, a3.screenY);
				polygon.addPoint(a4.screenX, a4.screenY);
				if (polygon.contains(x, y)) {
					zDepth = a1.screenZ + a2.screenZ + a3.screenZ + a4.screenZ;
					if (zDepth < zmin) {
						zmin = zDepth;
						foundIndex = tbonds.indexOf(tbond);
						tbondFound = tbond;
					}
				}
			}
		}
		if (foundIndex >= 0 && tbondFound != null) {
			highlightTBond.atom1 = tbondFound.atom1;
			highlightTBond.atom2 = tbondFound.atom2;
			highlightTBond.atom3 = tbondFound.atom3;
			highlightTBond.atom4 = tbondFound.atom4;
			tbonds.addTBond(highlightTBond);
		}
		return foundIndex;
	}

	public short[] findNearestObstacleIndexAndFace(int x, int y) {
		Planes planes = ((ExtendedFrameRenderer) repaintManager.frameRenderer).planes;
		planes.removePlane(highlightPlane);
		Ellipses ellipses = ((ExtendedFrameRenderer) repaintManager.frameRenderer).ellipses;
		ellipses.removeEllipse(highlightEllipse);
		Cylinders cylinders = ((ExtendedFrameRenderer) repaintManager.frameRenderer).cylinders;
		cylinders.removeCylinder(highlightCylinder);
		obstacleIndexAndFace[0] = obstacleIndexAndFace[1] = -1;
		Obstacles obstacles = ((ExtendedFrameRenderer) repaintManager.frameRenderer).obstacles;
		selectedObstacle = null;
		synchronized (obstacles.getLock()) {
			int n = obstacles.count();
			if (n <= 0)
				return obstacleIndexAndFace;
			Object3D o = null;
			byte face = -1;
			float zmin = Float.MAX_VALUE;
			for (short i = 0; i < n; i++) {
				o = obstacles.getObstacle(i);
				if (o instanceof Cuboid) {
					initHighlightPlane();
					Cuboid cuboid = (Cuboid) o;
					face = cuboid.getFace(this, x, y);
					if (face >= 0) {
						if (zmin > cuboid.zDepth) {
							obstacleIndexAndFace[0] = i;
							obstacleIndexAndFace[1] = face;
							selectedObstacle = cuboid;
							zmin = cuboid.zDepth;
						}
					}
				}
				else if (o instanceof Cylinder) {
					initHighlightEllipse();
					initHighlightCylinder();
					Cylinder cylinder = (Cylinder) o;
					face = cylinder.getFace(this, x, y);
					if (face >= 0) {
						if (zmin > cylinder.zDepth) {
							obstacleIndexAndFace[0] = i;
							obstacleIndexAndFace[1] = face;
							selectedObstacle = cylinder;
							zmin = cylinder.zDepth;
						}
					}
				}
			}
		}
		if (selectedObstacle instanceof Cuboid) {
			if (obstacleIndexAndFace[0] >= 0 && obstacleIndexAndFace[1] >= 0) {
				highlightPlane.setVertices(((Cuboid) selectedObstacle).getFaceVertices((byte) obstacleIndexAndFace[1]));
				highlightPlane.setColix(Graphics3D.getTranslucentColix(colorManager.getColixSelection()));
				planes.addPlane(highlightPlane);
			}
		}
		else if (selectedObstacle instanceof Cylinder) {
			if (obstacleIndexAndFace[0] >= 0 && obstacleIndexAndFace[1] >= 0) {
				Cylinder c = (Cylinder) selectedObstacle;
				if (obstacleIndexAndFace[1] == Cylinder.LATERAL) {
					highlightCylinder.center.set(c.center);
					highlightCylinder.a = c.a;
					highlightCylinder.b = c.b;
					highlightCylinder.height = c.height;
					highlightCylinder.axis = c.axis;
					cylinders.addCylinder(highlightCylinder);
				}
				else {
					highlightEllipse.center.set(c.getFaceCenter((byte) obstacleIndexAndFace[1]));
					highlightEllipse.a = c.a;
					highlightEllipse.b = c.b;
					highlightEllipse.axis = c.axis;
					ellipses.addEllipse(highlightEllipse);
				}
			}
		}
		return obstacleIndexAndFace;
	}

	public void setObstacleColor(int index, Color color, boolean translucent) {
		Obstacles obstacles = ((ExtendedFrameRenderer) repaintManager.frameRenderer).obstacles;
		Object3D o = obstacles.getObstacle(index);
		if (o instanceof Cylinder) {
			((Cylinder) o).colix = Graphics3D.getTranslucentColix(Graphics3D.getColix(color.getRGB()), translucent);
		}
		else if (o instanceof Cuboid) {
			((Cuboid) o).colix = Graphics3D.getTranslucentColix(Graphics3D.getColix(color.getRGB()), translucent);
		}
	}

	public void setObstacleLocation(int index, Point3f p) {
		Obstacles obstacles = ((ExtendedFrameRenderer) repaintManager.frameRenderer).obstacles;
		Object3D o = obstacles.getObstacle(index);
		if (o instanceof Cylinder) {
			Cylinder c = (Cylinder) o;
			c.center.set(p);
			byte face = (byte) obstacleIndexAndFace[1];
			if (face != -1) {
				if (face == Cylinder.LATERAL) {
					if (highlightCylinder != null)
						highlightCylinder.center.set(c.center);
				}
				else {
					if (highlightEllipse != null)
						highlightEllipse.center.set(c.getFaceCenter(face));
				}
			}
		}
		else if (o instanceof Cuboid) {
			Cuboid c = (Cuboid) o;
			c.center.set(p);
			highlightPlane.setVertices(c.getFaceVertices((byte) obstacleIndexAndFace[1]));
		}
	}

	public void setObstacleGeometry(int index, float rx, float ry, float rz, float lx, float ly, float lz) {
		Obstacles obstacles = ((ExtendedFrameRenderer) repaintManager.frameRenderer).obstacles;
		Object3D o = obstacles.getObstacle(index);
		if (o instanceof Cuboid) {
			Cuboid c = (Cuboid) o;
			c.center.set(rx, ry, rz);
			c.corner.set(lx, ly, lz);
			highlightPlane.setVertices(c.getFaceVertices((byte) obstacleIndexAndFace[1]));
		}
	}

	public void removeObstacle(int index) {
		Obstacles obstacles = ((ExtendedFrameRenderer) repaintManager.frameRenderer).obstacles;
		Object3D o = obstacles.getObstacle(index);
		obstacles.removeObstacle(o);
		if (o instanceof Cylinder) {
			Ellipses ellipses = ((ExtendedFrameRenderer) repaintManager.frameRenderer).ellipses;
			ellipses.removeEllipse(highlightEllipse);
			Cylinders cylinders = ((ExtendedFrameRenderer) repaintManager.frameRenderer).cylinders;
			cylinders.removeCylinder(highlightCylinder);
		}
		else if (o instanceof Cuboid) {
			Planes planes = ((ExtendedFrameRenderer) repaintManager.frameRenderer).planes;
			planes.removePlane(highlightPlane);
		}
	}

	public void addCuboidObstacle(float rx, float ry, float rz, float lx, float ly, float lz) {
		Obstacles obstacles = ((ExtendedFrameRenderer) repaintManager.frameRenderer).obstacles;
		Cuboid cuboid = new Cuboid(rx, ry, rz, lx * 2, ly * 2, lz * 2);
		cuboid.colix = Graphics3D.CYAN;
		obstacles.addObstacle(cuboid);
	}

	public void addCylinderObstacle(float rx, float ry, float rz, char axis, float h, float r) {
		Obstacles c = ((ExtendedFrameRenderer) repaintManager.frameRenderer).obstacles;
		Cylinder cylinder = new Cylinder();
		cylinder.setCenter(rx, ry, rz);
		cylinder.axis = axis;
		cylinder.height = h;
		cylinder.a = cylinder.b = r * COS45;
		cylinder.colix = Graphics3D.CYAN;
		c.addObstacle(cylinder);
	}

	public void addCuboidObstacle() {
		if (rectangle == null)
			return;
		Obstacles obstacles = ((ExtendedFrameRenderer) repaintManager.frameRenderer).obstacles;
		obstacles.removeObstacle(rectangle);
		if (Math.abs(rectangle.corner.x) < MIN_OBSTACLE_SIZE || Math.abs(rectangle.corner.y) < MIN_OBSTACLE_SIZE
				|| Math.abs(rectangle.corner.z) < MIN_OBSTACLE_SIZE)
			return;
		Cuboid cuboid = new Cuboid(0, 0, 0);
		cuboid.center.set(rectangle.center);
		cuboid.corner.set(rectangle.corner);
		cuboid.colix = Graphics3D.CYAN;
		obstacles.addObstacle(cuboid);
	}

	public void addCylinderObstacle() {
		if (ellipse == null)
			return;
		Obstacles c = ((ExtendedFrameRenderer) repaintManager.frameRenderer).obstacles;
		c.removeObstacle(ellipse);
		if (ellipse.height < MIN_OBSTACLE_SIZE || ellipse.a < MIN_OBSTACLE_SIZE || ellipse.b < MIN_OBSTACLE_SIZE)
			return;
		Cylinder cylinder = new Cylinder();
		cylinder.axis = ellipse.axis;
		cylinder.center.set(ellipse.center);
		cylinder.a = ellipse.a;
		cylinder.b = ellipse.b;
		cylinder.height = ellipse.height;
		cylinder.colix = Graphics3D.CYAN;
		c.addObstacle(cylinder);
	}

	public void updateCuboidObstacleFace(char axis, float center, float corner) {
		if (obstacleIndexAndFace[0] < 0 || obstacleIndexAndFace[1] < 0)
			return;
		if (highlightPlane == null || !(selectedObstacle instanceof Cuboid))
			return;
		Cuboid c = (Cuboid) selectedObstacle;
		Planes planes = ((ExtendedFrameRenderer) repaintManager.frameRenderer).planes;
		switch (axis) {
		case 'x':
			c.center.x = center;
			c.corner.x = corner;
			break;
		case 'y':
			c.center.y = center;
			c.corner.y = corner;
			break;
		case 'z':
			c.center.z = center;
			c.corner.z = corner;
			break;
		}
		highlightPlane.setVertices(c.getFaceVertices((byte) obstacleIndexAndFace[1]));
		planes.addPlane(highlightPlane);
	}

	public void updateCylinderObstacleFace(char axis, float center, float a, float b, float height) {
		if (obstacleIndexAndFace[0] < 0 || obstacleIndexAndFace[1] < 0)
			return;
		if (highlightEllipse == null || highlightCylinder == null || !(selectedObstacle instanceof Cylinder))
			return;
		Cylinder c = (Cylinder) selectedObstacle;
		switch (axis) {
		case 'x':
			c.center.x = center;
			break;
		case 'y':
			c.center.y = center;
			break;
		case 'z':
			c.center.z = center;
			break;
		}
		c.height = height;
		c.a = a;
		c.b = b;
		Ellipses ellipses = ((ExtendedFrameRenderer) repaintManager.frameRenderer).ellipses;
		Cylinders cylinders = ((ExtendedFrameRenderer) repaintManager.frameRenderer).cylinders;
		if (obstacleIndexAndFace[1] == Cylinder.LATERAL) {
			highlightCylinder.center.set(c.center);
			highlightCylinder.a = c.a;
			highlightCylinder.b = c.b;
			highlightCylinder.height = c.height;
			highlightCylinder.axis = c.axis;
			cylinders.addCylinder(highlightCylinder);
			ellipses.removeEllipse(highlightEllipse);
		}
		else {
			highlightEllipse.center.set(c.getFaceCenter((byte) obstacleIndexAndFace[1]));
			ellipses.addEllipse(highlightEllipse);
			cylinders.removeCylinder(highlightCylinder);
		}
	}

	public void setRectangleVisible(boolean b) {
		initRectangle();
		Obstacles c = ((ExtendedFrameRenderer) repaintManager.frameRenderer).obstacles;
		if (b) {
			c.addObstacle(rectangle);
		}
		else {
			c.removeObstacle(rectangle);
		}
	}

	public void setEllipseVisible(boolean b) {
		initEllipse();
		Obstacles c = ((ExtendedFrameRenderer) repaintManager.frameRenderer).obstacles;
		if (b) {
			c.addObstacle(ellipse);
		}
		else {
			c.removeObstacle(ellipse);
		}
	}

	public void setRectangle(char axis, Point3f p1, Point3f p2) {
		if (rectangle == null || dropPlane == null)
			return;
		switch (axis) {
		case 'x':
			rectangle.setCenter(dropPlane.getVertex(0).x, 0.5f * (p1.y + p2.y), 0.5f * (p1.z + p2.z));
			rectangle.setCorner(MIN_OBSTACLE_SIZE, 0.5f * Math.abs(p2.y - p1.y), 0.5f * Math.abs(p2.z - p1.z));
			break;
		case 'y':
			rectangle.setCenter(0.5f * (p1.x + p2.x), dropPlane.getVertex(0).y, 0.5f * (p1.z + p2.z));
			rectangle.setCorner(0.5f * Math.abs(p2.x - p1.x), MIN_OBSTACLE_SIZE, 0.5f * Math.abs(p2.z - p1.z));
			break;
		case 'z':
			rectangle.setCenter(0.5f * (p1.x + p2.x), 0.5f * (p1.y + p2.y), dropPlane.getVertex(0).z);
			rectangle.setCorner(0.5f * Math.abs(p2.x - p1.x), 0.5f * Math.abs(p2.y - p1.y), MIN_OBSTACLE_SIZE);
			break;
		}
	}

	public void setEllipse(char axis, Point3f p1, Point3f p2) {
		if (ellipse == null || dropPlane == null)
			return;
		ellipse.axis = axis;
		switch (axis) {
		case 'x':
			ellipse.setCenter(dropPlane.getVertex(0).x, 0.5f * (p1.y + p2.y), 0.5f * (p1.z + p2.z));
			ellipse.a = 0.5f * Math.abs(p2.y - p1.y);
			ellipse.b = 0.5f * Math.abs(p2.z - p1.z);
			break;
		case 'y':
			ellipse.setCenter(0.5f * (p1.x + p2.x), dropPlane.getVertex(0).y, 0.5f * (p1.z + p2.z));
			ellipse.a = 0.5f * Math.abs(p2.x - p1.x);
			ellipse.b = 0.5f * Math.abs(p2.z - p1.z);
			break;
		case 'z':
			ellipse.setCenter(0.5f * (p1.x + p2.x), 0.5f * (p1.y + p2.y), dropPlane.getVertex(0).z);
			ellipse.a = 0.5f * Math.abs(p2.x - p1.x);
			ellipse.b = 0.5f * Math.abs(p2.y - p1.y);
			break;
		}
		ellipse.a = ellipse.b = Math.max(ellipse.a, ellipse.b);
	}

	private void initRectangle() {
		if (rectangle == null) {
			rectangle = new Cuboid(0, 0, 0);
			rectangle.setColix(Graphics3D.MAROON);
		}
	}

	private void initEllipse() {
		if (ellipse == null) {
			ellipse = new Cylinder();
			ellipse.setColix(Graphics3D.MAROON);
		}
	}

	private void initHighlightPlane() {
		if (highlightPlane == null) {
			highlightPlane = new Plane();
		}
	}

	private void initHighlightTriangle() {
		if (highlightTriangle == null) {
			highlightTriangle = new Triangle();
			highlightTriangle.colix = Graphics3D.getTranslucentColix(colorManager.getColixSelection());
		}
	}

	private void initHighlightEllipse() {
		if (highlightEllipse == null) {
			highlightEllipse = new Ellipse();
			highlightEllipse.colix = Graphics3D.getTranslucentColix(colorManager.getColixSelection());
		}
	}

	private void initHighlightCylinder() {
		if (highlightCylinder == null) {
			highlightCylinder = new Cylinder();
			highlightCylinder.colix = Graphics3D.getTranslucentColix(colorManager.getColixSelection());
			highlightCylinder.endcaps = Graphics3D.ENDCAPS_NONE;
		}
	}

	private void initHighlightTBond() {
		if (highlightTBond == null) {
			highlightTBond = new TBond();
			highlightTBond.highlight = true;
			highlightTBond.colix = Graphics3D.getTranslucentColix(colorManager.getColixSelection());
		}
	}

	private void initDropPlane() {
		if (dropPlane == null) {
			dropPlane = new Plane();
		}
	}

	public void setDropPlaneVisible(char direction, boolean visible) {
		if (modelManager.frame == null)
			return;
		Planes planes = ((ExtendedFrameRenderer) repaintManager.frameRenderer).planes;
		Cuboids c = ((ExtendedFrameRenderer) repaintManager.frameRenderer).cuboids;
		SimulationBox box = (SimulationBox) c.getCuboid(0);
		Atom at = null;
		if (modelManager.frame.atomCount > 0) {
			for (int i = 0; i < modelManager.frame.atomCount; i++) {
				if (isSelected(modelManager.frame.atoms[i].atomIndex)) {
					at = modelManager.frame.atoms[i];
					break;
				}
			}
		}
		float g = 0;
		switch (direction) {
		case 'x':
			if (visible) {
				initDropPlane();
				g = at != null ? at.getAtomX() : dropPlane.getCenter().x;
				dropPlane.setVertices(g, box.corner.y, box.corner.z, g, -box.corner.y, box.corner.z, g, -box.corner.y,
						-box.corner.z, g, box.corner.y, -box.corner.z);
				dropPlane.m = (short) box.corner.y;
				dropPlane.n = (short) box.corner.z;
				dropPlane.axis = direction;
				planes.addPlane(dropPlane);
			}
			else {
				planes.removePlane(dropPlane);
			}
			break;
		case 'y':
			if (visible) {
				initDropPlane();
				g = at != null ? at.getAtomY() : dropPlane.getCenter().y;
				dropPlane.setVertices(box.corner.x, g, box.corner.z, -box.corner.x, g, box.corner.z, -box.corner.x, g,
						-box.corner.z, box.corner.x, g, -box.corner.z);
				dropPlane.m = (short) box.corner.x;
				dropPlane.n = (short) box.corner.z;
				dropPlane.axis = direction;
				planes.addPlane(dropPlane);
			}
			else {
				planes.removePlane(dropPlane);
			}
			break;
		case 'z':
			if (visible) {
				initDropPlane();
				g = at != null ? at.getAtomZ() : dropPlane.getCenter().z;
				dropPlane.setVertices(box.corner.x, box.corner.y, g, -box.corner.x, box.corner.y, g, -box.corner.x,
						-box.corner.y, g, box.corner.x, -box.corner.y, g);
				dropPlane.m = (short) box.corner.x;
				dropPlane.n = (short) box.corner.y;
				dropPlane.axis = direction;
				planes.addPlane(dropPlane);
			}
			else {
				planes.removePlane(dropPlane);
			}
			break;
		}
		if (visible) {
			dropPlane.setColix(Graphics3D.getTranslucentColix(Graphics3D.LIME));
			dropPlane.setMode(Plane.GRID_MODE);
		}
	}

	public void moveDropPlaneTo(char direction, float coordinate) {
		if (dropPlane == null)
			return;
		for (int i = 0; i < 4; i++) {
			switch (direction) {
			case 'x':
				dropPlane.getVertex(i).x = coordinate;
				break;
			case 'y':
				dropPlane.getVertex(i).y = coordinate;
				break;
			case 'z':
				dropPlane.getVertex(i).z = coordinate;
				break;
			}
		}
		dropPlane.computeCenter();
	}

	public float getDropPlanePosition(char axis) {
		if (dropPlane == null)
			return 0;
		switch (axis) {
		case 'x':
			return dropPlane.getCenter().x;
		case 'y':
			return dropPlane.getCenter().y;
		case 'z':
			return dropPlane.getCenter().z;
		}
		return 0;
	}

	public void translateDropPlane(float dx, float dy, float dz) {
		if (dropPlane == null)
			return;
		Cuboids cuboids = ((ExtendedFrameRenderer) repaintManager.frameRenderer).cuboids;
		SimulationBox box = (SimulationBox) cuboids.getCuboid(0);
		if (dropPlane.getCenter().x + dx < box.getMinX())
			return;
		if (dropPlane.getCenter().x + dx > box.getMaxX())
			return;
		if (dropPlane.getCenter().y + dy < box.getMinY())
			return;
		if (dropPlane.getCenter().y + dy > box.getMaxY())
			return;
		if (dropPlane.getCenter().z + dz < box.getMinZ())
			return;
		if (dropPlane.getCenter().z + dz > box.getMaxZ())
			return;
		dropPlane.move(dx, dy, dz);
	}

	public int findNearestAtomIndexOnDropPlane(char direction, int x, int y) {
		int i = findNearestAtomIndex(x, y);
		if (isDropPlaneIntersected(direction, i))
			return i;
		return -1;
	}

	private boolean isDropPlaneIntersected(char direction, int index) {
		if (index < 0 || index >= modelManager.frame.atomCount)
			return false;
		Atom at = modelManager.frame.atoms[index];
		Point3f center = dropPlane.getCenter();
		float radius = at.getVanderwaalsRadiusFloat();
		switch (direction) {
		case 'x':
			if (Math.abs(at.getAtomX() - center.x) < radius)
				return true;
			break;
		case 'y':
			if (Math.abs(at.getAtomY() - center.y) < radius)
				return true;
			break;
		case 'z':
			if (Math.abs(at.getAtomZ() - center.z) < radius)
				return true;
			break;
		}
		return false;
	}

	public Point3f findPointOnDropPlane(char direction, int x, int y) {
		float c = 0;
		if (dropPlane != null) {
			switch (direction) {
			case 'x':
				c = dropPlane.getCenter().x;
				break;
			case 'y':
				c = dropPlane.getCenter().y;
				break;
			case 'z':
				c = dropPlane.getCenter().z;
				break;
			}
		}
		return findPointOnPlane(direction, x, y, c);
	}

	public Point3f findPointOnPlane(char direction, int x, int y, float c) {
		if (tempMatrix4f == null)
			tempMatrix4f = new Matrix4f();
		tempMatrix4f.setIdentity();
		tempMatrix4f.invert(transformManager.matrixTransform);
		float xa = 0, ya = 0, za = 0;
		if (pmin == null)
			pmin = new Point3f();
		pmin.set(transformManager.reversePerspectiveAdjustments(x, y, -1000));
		tempMatrix4f.transform(pmin);
		if (pmax == null)
			pmax = new Point3f();
		pmax.set(transformManager.reversePerspectiveAdjustments(x, y, 1000));
		tempMatrix4f.transform(pmax);
		float dx = pmax.x - pmin.x;
		float dy = pmax.y - pmin.y;
		float dz = pmax.z - pmin.z;
		switch (direction) {
		case 'x':
			xa = c;
			ya = (xa - pmin.x) / dx * dy + pmin.y;
			za = (xa - pmin.x) / dx * dz + pmin.z;
			break;
		case 'y':
			ya = c;
			xa = (ya - pmin.y) / dy * dx + pmin.x;
			za = (ya - pmin.y) / dy * dz + pmin.z;
			break;
		case 'z':
			za = c;
			xa = (za - pmin.z) / dz * dx + pmin.x;
			ya = (za - pmin.z) / dz * dy + pmin.y;
			break;
		}
		if (planePoint == null)
			planePoint = new Point3f();
		planePoint.set(xa, ya, za);
		return planePoint;
	}

	public void setVelocityBitSet(BitSet bs) {
		velocityBitSet = bs;
	}

	public void setTrajectoryBitSet(BitSet bs) {
		trajectoryBitSet = bs;
	}

	public void setTranslucentBitSet(BitSet bs) {
		translucentBitSet = bs;
	}

	public void setHidenBitSet(BitSet bs) {
		hidenBitSet = bs;
	}

	public void setVdwForceLines(Pair[] pairs) {
		VdwForceLines lines = ((ExtendedFrameRenderer) repaintManager.frameRenderer).vdwForceLines;
		lines.pairs = pairs;
	}

	public void setTrajectory(int index, int m, float[] x, float[] y, float[] z) {
		Trajectories trajectories = ((ExtendedFrameRenderer) repaintManager.frameRenderer).trajectories;
		if (trajectoryBitSet.get(index)) {
			synchronized (trajectories.getLock()) {
				int n = trajectories.count();
				Trajectory t = null;
				for (int i = 0; i < n; i++) {
					Trajectory t2 = trajectories.getTrajectory(i);
					if (t2.getIndex() == index) {
						t = t2;
						break;
					}
				}
				if (t == null) {
					t = new Trajectory(index);
					trajectories.addTrajectory(t);
				}
				t.setPoints(m, x, y, z);
			}
		}
		else {
			synchronized (trajectories.getLock()) {
				int n = trajectories.count();
				Trajectory t;
				for (int i = 0; i < n; i++) {
					t = trajectories.getTrajectory(i);
					if (t.getIndex() == index) {
						trajectories.removeTrajectory(t);
						break;
					}
				}
			}
		}
	}

}