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

package org.concord.mw3d.models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import javax.vecmath.Point3f;

import org.concord.modeler.util.FloatQueue;
import org.concord.modeler.util.FloatQueueTriplet;

public class Atom {

	public final static byte UNMOVABLE = 0x00;
	public final static byte INVISIBLE = 0x01;
	private final static Pattern GENERIC_PARTICLE = Pattern.compile("X\\d");

	private short elementNumber;
	private String symbol;
	private boolean movable = true;
	private boolean visible = true;
	float rx, ry, rz;
	float dx, dy, dz;
	float vx, vy, vz;
	float ax, ay, az;
	float fx, fy, fz;
	float sigma = 2.2f;
	float epsilon = 0.05f;
	float mass = 10.0f;
	float charge;
	float damp;
	int index;
	private List<RBond> rbondList;
	private List<ABond> abondList;
	private List<TBond> tbondList;

	MolecularModel model;
	FloatQueueTriplet rQ, vQ, aQ;

	/** create an abstract atom: for using its data structure only. */
	public Atom(MolecularModel model) {
		setModel(model);
	}

	Atom(String symbol, MolecularModel model) {
		this(model);
		setSymbol(symbol);
	}

	public boolean isSelected() {
		if (model == null || index < 0)
			return false;
		return model.getView().getViewer().getSelectionSet().get(index);
	}

	public void setAtom(Atom a) {
		setModel(a.model);
		setSymbol(a.symbol);
		elementNumber = a.elementNumber;
		movable = a.movable;
		model = a.model;
		rx = a.rx;
		ry = a.ry;
		rz = a.rz;
		vx = a.vx;
		vy = a.vy;
		vz = a.vz;
	}

	public void setModel(MolecularModel model) {
		this.model = model;
	}

	public MolecularModel getModel() {
		return model;
	}

	public boolean isBonded() {
		if (rbondList == null || rbondList.isEmpty())
			return false;
		return true;
	}

	public boolean isBonded(Atom a) {
		if (rbondList == null || rbondList.isEmpty())
			return false;
		synchronized (rbondList) {
			for (RBond rbond : rbondList) {
				if (rbond.getAtom1() == a || rbond.getAtom2() == a)
					return true;
			}
		}
		return false;
	}

	public boolean isABonded(Atom a) {
		if (abondList == null || abondList.isEmpty())
			return false;
		synchronized (abondList) {
			for (ABond abond : abondList) {
				if (abond.getAtom1() == a || abond.getAtom2() == a || abond.getAtom3() == a)
					return true;
			}
		}
		return false;
	}

	public boolean isTBonded(Atom a) {
		if (tbondList == null || tbondList.isEmpty())
			return false;
		synchronized (tbondList) {
			for (TBond t : tbondList) {
				if (t.getAtom1() == a || t.getAtom2() == a || t.getAtom3() == a || t.getAtom4() == a)
					return true;
			}
		}
		return false;
	}

	void addRBond(RBond rbond) {
		if (rbondList == null)
			rbondList = Collections.synchronizedList(new ArrayList<RBond>());
		if (!rbondList.contains(rbond))
			rbondList.add(rbond);
	}

	void removeRBond(RBond rbond) {
		if (rbondList == null)
			return;
		rbondList.remove(rbond);
	}

	void addABond(ABond abond) {
		if (abondList == null)
			abondList = Collections.synchronizedList(new ArrayList<ABond>());
		if (!abondList.contains(abond))
			abondList.add(abond);
	}

	void removeABond(ABond abond) {
		if (abondList == null)
			return;
		abondList.remove(abond);
	}

	void addTBond(TBond tbond) {
		if (tbondList == null)
			tbondList = Collections.synchronizedList(new ArrayList<TBond>());
		if (!tbondList.contains(tbond))
			tbondList.add(tbond);
	}

	void removeTBond(TBond tbond) {
		if (tbondList == null)
			return;
		tbondList.remove(tbond);
	}

	void clearBondLists() {
		if (rbondList != null && !rbondList.isEmpty()) {
			synchronized (rbondList) {
				for (RBond rbond : rbondList) {
					model.getRBonds().remove(rbond);
				}
				rbondList.clear();
			}
		}
		if (abondList != null && !abondList.isEmpty()) {
			synchronized (abondList) {
				for (ABond abond : abondList) {
					model.getABonds().remove(abond);
				}
				abondList.clear();
			}
		}
		if (tbondList != null && !tbondList.isEmpty()) {
			synchronized (tbondList) {
				for (TBond tbond : tbondList) {
					model.getTBonds().remove(tbond);
				}
				tbondList.clear();
			}
		}
	}

	public void setElementNumber(short i) {
		elementNumber = i;
	}

	public short getElementNumber() {
		return elementNumber;
	}

	public boolean isGenericParticle() {
		return isGenericParticle(symbol);
	}

	public static boolean isGenericParticle(String symbol) {
		return GENERIC_PARTICLE.matcher(symbol).matches();
	}

	public void setSymbol(String symbol) {
		if (symbol == null)
			throw new IllegalArgumentException("symbol cannot be null.");
		this.symbol = symbol;
		charge = 0.0f;
		float[] par = model.paramMap.get(symbol);
		if (par != null) {
			elementNumber = (short) Math.round(par[0]);
			mass = par[1];
			sigma = par[2];
			epsilon = par[3];
		}
	}

	public String getSymbol() {
		return symbol;
	}

	public void setVisible(boolean b) {
		visible = b;
	}

	public boolean isVisible() {
		return visible;
	}

	public void setMovable(boolean b) {
		movable = b;
		if (!movable) {
			zeroVelocity();
		}
	}

	public void zeroVelocity() {
		vx = vy = vz = 0;
	}

	public void zeroAcceleration() {
		ax = ay = az = 0;
	}

	public boolean isMovable() {
		return movable;
	}

	/** only for the generic particles */
	public void setMass(float mass) {
		this.mass = mass;
	}

	public float getMass() {
		return mass;
	}

	/** only for the generic particles */
	public void setSigma(float sigma) {
		this.sigma = sigma;
	}

	public float getSigma() {
		return sigma;
	}

	/** only for the generic particles */
	public void setEpsilon(float epsilon) {
		this.epsilon = epsilon;
	}

	public float getEpsilon() {
		return epsilon;
	}

	public int getIndex() {
		return index;
	}

	public String toString() {
		return index + symbol;
	}

	public float distance(Atom a) {
		return (float) Math.sqrt(distanceSquare(a));
	}

	public float distanceSquare(Atom a) {
		return (a.rx - rx) * (a.rx - rx) + (a.ry - ry) * (a.ry - ry) + (a.rz - rz) * (a.rz - rz);
	}

	public synchronized void setLocation(float x, float y, float z) {
		setRx(x);
		setRy(y);
		setRz(z);
	}

	public synchronized void setLocation(Point3f p) {
		setLocation(p.x, p.y, p.z);
	}

	public synchronized void translate(float dx, float dy, float dz) {
		setRx(rx + dx);
		setRy(ry + dy);
		setRz(rz + dz);
	}

	public synchronized boolean overlapCuboid(float x, float y, float z, float a, float b, float c) {
		float rd = sigma * 0.5f;
		if (rx + rd > x - a && rx - rd < x + a && ry + rd > y - b && ry - rd < y + b && rz + rd > z - c
				&& rz - rd < z + c)
			return true;
		return false;
	}

	public synchronized boolean overlapCylinder(float x, float y, float z, char axis, float h, float r) {
		float rd = r + sigma * 0.5f;
		float h2 = h * 0.5f;
		float a12, b12;
		switch (axis) {
		case 'x':
			if (rx + 0.5f * sigma > x - h2 && rx - 0.5f * sigma < x + h2) {
				a12 = ry - y;
				b12 = rz - z;
				if (a12 * a12 + b12 * b12 < rd * rd)
					return true;
			}
			break;
		case 'y':
			if (ry + 0.5f * sigma > y - h2 && ry - 0.5f * sigma < y + h2) {
				a12 = rx - x;
				b12 = rz - z;
				if (a12 * a12 + b12 * b12 < rd * rd)
					return true;
			}
			break;
		case 'z':
			if (rz + 0.5f * sigma > z - h2 && rz - 0.5 * sigma < z + h2) {
				a12 = rx - x;
				b12 = ry - y;
				if (a12 * a12 + b12 * b12 < rd * rd)
					return true;
			}
			break;
		}
		return false;
	}

	public synchronized boolean isTooClose(Point3f p) {
		float d = rx - p.x;
		float r = d * d;
		d = ry - p.y;
		r += d * d;
		d = rz - p.z;
		r += d * d;
		return r < sigma * 0.25f;
	}

	public boolean isTooClose(Atom a) {
		float d = rx - a.rx;
		float r = d * d;
		d = ry - a.ry;
		r += d * d;
		d = rz - a.rz;
		r += d * d;
		return r < (sigma + a.sigma) * 0.5f;
	}

	public float getKe() {
		return mass * (vx * vx + vy * vy + vz * vz) * 0.5f;
	}

	public float[] getTrajectoryRx() {
		if (rQ == null)
			return null;
		return (float[]) rQ.getQueue1().getData();
	}

	public float[] getTrajectoryRy() {
		if (rQ == null)
			return null;
		return (float[]) rQ.getQueue2().getData();
	}

	public float[] getTrajectoryRz() {
		if (rQ == null)
			return null;
		return (float[]) rQ.getQueue3().getData();
	}

	public synchronized float getRx() {
		return rx;
	}

	public synchronized void setRx(float rx) {
		this.rx = rx;
	}

	public synchronized float getRy() {
		return ry;
	}

	public synchronized void setRy(float ry) {
		this.ry = ry;
	}

	public synchronized float getRz() {
		return rz;
	}

	public synchronized void setRz(float rz) {
		this.rz = rz;
	}

	public synchronized float getVx() {
		return vx;
	}

	public synchronized void setVx(float vx) {
		this.vx = vx;
	}

	public synchronized float getVy() {
		return vy;
	}

	public synchronized void setVy(float vy) {
		this.vy = vy;
	}

	public synchronized float getVz() {
		return vz;
	}

	public synchronized void setVz(float vz) {
		this.vz = vz;
	}

	public synchronized float getAx() {
		return ax;
	}

	public synchronized void setAx(float ax) {
		this.ax = ax;
	}

	public synchronized float getAy() {
		return ay;
	}

	public synchronized void setAy(float ay) {
		this.ay = ay;
	}

	public synchronized float getAz() {
		return az;
	}

	public synchronized void setAz(float az) {
		this.az = az;
	}

	public float getCharge() {
		return charge;
	}

	public void setCharge(float charge) {
		this.charge = charge;
	}

	public void addCharge(float x) {
		charge += x;
	}

	public void setDamp(float damp) {
		this.damp = damp;
	}

	public float getDamp() {
		return damp;
	}

	public Object getProperty(String name) {
		return null;
	}

	/* predict this atom's new position using 2nd order Taylor expansion */
	void predict(float dt, float dt2) {
		if (!movable)
			return;
		dx = vx * dt + ax * dt2;
		dy = vy * dt + ay * dt2;
		dz = vz * dt + az * dt2;
		rx += dx;
		ry += dy;
		rz += dz;
		vx += ax * dt;
		vy += ay * dt;
		vz += az * dt;
	}

	/*
	 * correct the position predicted by the <tt>predict</tt> method. <b>Important</b>: <tt>fx, fy, fz</tt> were
	 * used in the force calculation routine to store the new acceleration data. <tt>ax, ay, az</tt> were used to hold
	 * the old acceleration data before calling this method. After calling this method, new acceleration data will be
	 * assigned to <tt>ax, ay, az</tt>, whereas the forces and torques to <tt>fx, fy, fz</tt>. <b>Be aware</b>:
	 * the acceleration and force properties of a particle are correct ONLY after this correction method has been
	 * called.
	 * 
	 * @param half half of the time increment
	 */
	void correct(float half) {
		if (!movable)
			return;
		vx += half * (fx - ax);
		vy += half * (fy - ay);
		vz += half * (fz - az);
		ax = fx;
		ay = fy;
		az = fz;
		fx *= mass;
		fy *= mass;
		fz *= mass;
	}

	public void initMovieQ(int n) {
		initRQ(n);
		initVQ(n);
		initAQ(n);
	}

	/** initialize coordinate queues. If the passed integer is less than 1, nullify the queues. */
	public void initRQ(int n) {
		if (rQ == null) {
			if (n < 1)
				return; // already null
			rQ = new FloatQueueTriplet(new FloatQueue("Rx: " + toString(), n), new FloatQueue("Ry: " + toString(), n),
					new FloatQueue("Rz: " + toString(), n));
			rQ.setInterval(model.movieUpdater.getInterval());
			rQ.setPointer(0);
			rQ.setCoordinateQueue(model.modelTimeQueue);
			// model.movieQueueGroup.add(rQ);
		}
		else {
			rQ.setLength(n);
			if (n < 1) {
				// model.movieQueueGroup.remove(rQ);
				rQ = null;
			}
			else {
				rQ.setPointer(0);
			}
		}
	}

	/** initialize velocity queues. If the passed integer is less than 1, nullify the array. */
	public void initVQ(int n) {
		if (vQ == null) {
			if (n < 1)
				return; // already null
			vQ = new FloatQueueTriplet(new FloatQueue("Vx: " + toString(), n), new FloatQueue("Vy: " + toString(), n),
					new FloatQueue("Vz: " + toString(), n));
			vQ.setInterval(model.movieUpdater.getInterval());
			vQ.setPointer(0);
			vQ.setCoordinateQueue(model.modelTimeQueue);
			// model.movieQueueGroup.add(vQ);
		}
		else {
			vQ.setLength(n);
			if (n < 1) {
				// model.movieQueueGroup.remove(vQ);
				vQ = null;
			}
			else {
				vQ.setPointer(0);
			}
		}
	}

	/** initialize acceleration queues. If the passed integer is less than 1, nullify the array. */
	public void initAQ(int n) {
		if (aQ == null) {
			if (n < 1)
				return; // already null
			aQ = new FloatQueueTriplet(new FloatQueue("Ax: " + toString(), n), new FloatQueue("Ay: " + toString(), n),
					new FloatQueue("Az: " + toString(), n));
			aQ.setInterval(model.movieUpdater.getInterval());
			aQ.setPointer(0);
			aQ.setCoordinateQueue(model.modelTimeQueue);
			// model.movieQueueGroup.add(aQ);
		}
		else {
			aQ.setLength(n);
			if (n < 1) {
				// model.movieQueueGroup.remove(aQ);
				aQ = null;
			}
			else {
				aQ.setPointer(0);
			}
		}
	}

	public FloatQueueTriplet getRQ() {
		return rQ;
	}

	public FloatQueueTriplet getVQ() {
		return vQ;
	}

	/** push current coordinate into the coordinate queue */
	public synchronized void updateRQ() {
		if (rQ == null || rQ.isEmpty())
			throw new RuntimeException("Attempt to write to the empty queue");
		rQ.update(rx, ry, rz);
	}

	/** push current velocity into the velocity queue */
	public synchronized void updateVQ() {
		if (vQ == null || vQ.isEmpty())
			throw new RuntimeException("Attempt to write to the empty queue");
		vQ.update(vx, vy, vz);
	}

	/** push current acceleration into the acceleration queue */
	public synchronized void updateAQ() {
		if (aQ == null || aQ.isEmpty())
			throw new RuntimeException("Attempt to write to the empty queue");
		aQ.update(ax, ay, az);
	}

	/**
	 * When an array is initialized and its elements subsequently filled, it occurs that until the array is full, some
	 * of the elements are empty, the pointer points to the begin index of unfilled segment. The pointer will stop at
	 * the last index of the array once the whole array is filled up.
	 */
	public synchronized int getRPointer() {
		if (rQ == null || rQ.isEmpty())
			return -1;
		return rQ.getPointer();
	}

	/** @see org.concord.mw3d.models.Atom#getRPointer */
	public synchronized void moveRPointer(int i) {
		if (rQ == null || rQ.isEmpty())
			return;
		rQ.setPointer(i);
	}

	/** @see org.concord.mw3d.models.Atom#getRPointer */
	public synchronized int getVPointer() {
		if (vQ == null || vQ.isEmpty())
			return -1;
		return vQ.getPointer();
	}

	/** @see org.concord.mw3d.models.Atom#getRPointer */
	public synchronized void moveVPointer(int i) {
		if (vQ == null || vQ.isEmpty())
			return;
		vQ.setPointer(i);
	}

	/** @see org.concord.mw3d.models.Atom#getRPointer */
	public synchronized int getAPointer() {
		if (aQ == null || aQ.isEmpty())
			return -1;
		return aQ.getPointer();
	}

	/** @see org.concord.mw3d.models.Atom#getRPointer */
	public synchronized void moveAPointer(int i) {
		if (aQ == null || aQ.isEmpty())
			return;
		aQ.setPointer(i);
	}

}