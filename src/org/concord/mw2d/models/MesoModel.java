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

import java.awt.Color;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.Shape;
import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JOptionPane;

import org.concord.modeler.draw.FillMode;
import org.concord.modeler.event.ModelEvent;
import org.concord.modeler.process.Loadable;
import org.concord.modeler.util.DataQueue;
import org.concord.modeler.util.FloatQueue;
import org.concord.mw2d.MDView;
import org.concord.mw2d.MesoView;
import org.concord.mw2d.event.ParameterChangeEvent;

public class MesoModel extends MDModel {

	/* Maximum number of mesogens allowed in a meso model (100). */
	private static final short NMAX = 100;

	MesoView view;
	GayBerneParticle[] gb;
	volatile int numberOfParticles;

	private String name = "Default";
	private GayBerneForce gbForce;
	private ElectrostaticForce esForce;
	private double rCutOff;
	private volatile double dt = 1.0;
	private double dt2 = dt * dt * 0.5;
	private double nu = 1.0, mu = 2.0;
	private double fxij, fyij, x_ij, y_ij, r_ij;

	public MesoModel() {
		this(DEFAULT_WIDTH, DEFAULT_HEIGHT, DataQueue.DEFAULT_SIZE);
	}

	public MesoModel(int xbox, int ybox, int tapeLength) {

		super();

		setDefaultTapeLength(tapeLength);

		gb = new GayBerneParticle[NMAX];
		for (int i = 0, np = gb.length; i < np; i++) {
			gb[i] = new GayBerneParticle();
			gb[i].setIndex(i);
			gb[i].setModel(this);
		}
		gbForce = new GayBerneForce(mu, nu);
		esForce = new ElectrostaticForce();
		boundary = new RectangularBoundary(0, 0, xbox, ybox, this);
		boundary.setView(boundary);
		rCutOff = Math.max(xbox, ybox) / 2;

		movie.setCapacity(defaultTapeLength);
		modelTimeQueue = new FloatQueue("Time (fs)", movie.getCapacity());

		for (int i = 0; i < channelTs.length; i++) {
			channelTs[i] = new FloatQueue("Channel " + i, movie.getCapacity());
			channelTs[i].setReferenceUpperBound(1);
			channelTs[i].setReferenceLowerBound(0);
			channelTs[i].setCoordinateQueue(modelTimeQueue);
			channelTs[i].setInterval(movieUpdater.getInterval());
			channelTs[i].setPointer(0);
			movieQueueGroup.add(channelTs[i]);
		}

		kine = new FloatQueue("Kinetic Energy/Particle", movie.getCapacity());
		kine.setReferenceUpperBound(5);
		kine.setReferenceLowerBound(-5);
		kine.setCoordinateQueue(modelTimeQueue);
		kine.setInterval(movieUpdater.getInterval());
		kine.setPointer(0);
		movieQueueGroup.add(kine);

		pote = new FloatQueue("Potential Energy/Particle", movie.getCapacity());
		pote.setReferenceUpperBound(5);
		pote.setReferenceLowerBound(-5);
		pote.setCoordinateQueue(modelTimeQueue);
		pote.setInterval(movieUpdater.getInterval());
		pote.setPointer(0);
		movieQueueGroup.add(pote);

		tote = new FloatQueue("Total Energy/Particle", movie.getCapacity());
		tote.setReferenceUpperBound(5);
		tote.setReferenceLowerBound(-5);
		tote.setCoordinateQueue(modelTimeQueue);
		tote.setInterval(movieUpdater.getInterval());
		tote.setPointer(0);
		movieQueueGroup.add(tote);

	}

	public static short getMaximumNumberOfParticles() {
		return NMAX;
	}

	public void setView(MDView v) {
		if (!(v instanceof MesoView))
			throw new IllegalArgumentException("must be MesoView");
		super.setView(v);
		view = (MesoView) v;
	}

	public JComponent getView() {
		return view;
	}

	public void markSelection() {
		super.markSelection();
		view.repaint();
	}

	/** destroy this model to prevent memory leak. */
	public void destroy() {
		super.destroy();
		for (GayBerneParticle p : gb) {
			p.initializeRQ(-1);
			p.initializeVQ(-1);
			p.initializeAQ(-1);
			p.initializeThetaQ(-1);
			p.initializeOmegaQ(-1);
			p.initializeAlphaQ(-1);
			p.setModel(null);
			p = null;
		}
	}

	public float[] getBoundsOfObjects() {
		if (numberOfParticles <= 0)
			return null;
		range_xmin = (float) gb[0].getMinX();
		range_ymin = (float) gb[0].getMinY();
		range_xmax = (float) gb[0].getMaxX();
		range_ymax = (float) gb[0].getMaxY();
		id_xmax = id_xmin = id_ymax = id_ymin = 0;
		if (boundary.getType() == RectangularBoundary.DBC_ID || boundary.getType() == RectangularBoundary.RBC_ID) {
			double sinTheta, cosTheta;
			double dx, dy;
			for (short i = 1; i < numberOfParticles; i++) {
				GayBerneParticle p = gb[i];
				sinTheta = Math.sin(p.getTheta());
				cosTheta = Math.cos(p.getTheta());
				dx = p.getBreadth() * p.getBreadth() * sinTheta * sinTheta + p.getLength() * p.getLength() * cosTheta
						* cosTheta;
				dx = Math.sqrt(dx) * 0.5;
				dy = p.getBreadth() * p.getBreadth() * cosTheta * cosTheta + p.getLength() * p.getLength() * sinTheta
						* sinTheta;
				dy = Math.sqrt(dy) * 0.5;
				if (p.getRx() + dx > range_xmax) {
					range_xmax = (float) (p.getRx() + dx);
					id_xmax = i;
				}
				else if (p.getRx() - dx < range_xmin) {
					range_xmin = (float) (p.getRx() - dx);
					id_xmin = i;
				}
				if (p.getRy() + dy > range_ymax) {
					range_ymax = (float) (p.getRy() + dy);
					id_ymax = i;
				}
				else if (p.getRy() - dy < range_ymin) {
					range_ymin = (float) (p.getRy() - dy);
					id_ymin = i;
				}
			}
		}
		else {
			for (short i = 1; i < numberOfParticles; i++) {
				GayBerneParticle p = gb[i];
				if (p.getRx() > range_xmax) {
					range_xmax = (float) (p.getRx());
					id_xmax = i;
				}
				else if (p.getRx() < range_xmin) {
					range_xmin = (float) (p.getRx());
					id_xmin = i;
				}
				if (p.getRy() > range_ymax) {
					range_ymax = (float) (p.getRy());
					id_ymax = i;
				}
				else if (p.getRy() < range_ymin) {
					range_ymin = (float) (p.getRy());
					id_ymin = i;
				}
			}
		}
		if (obstacles != null && !obstacles.isEmpty()) {
			Obstacle o;
			for (Iterator it = obstacles.iterator(); it.hasNext();) {
				o = (Obstacle) it.next();
				if (o.getMaxX() > range_xmax)
					range_xmax = (float) o.getMaxX();
				if (o.getMaxY() > range_ymax)
					range_ymax = (float) o.getMaxY();
			}
		}
		return new float[] { range_xmin, range_ymin, range_xmax, range_ymax };
	}

	boolean rotateSelectedParticles(double angleInDegrees) {
		int n = 0;
		double xc = 0, yc = 0;
		for (int i = 0; i < numberOfParticles; i++) {
			if (gb[i].isSelected()) {
				xc += gb[i].rx;
				yc += gb[i].ry;
				n++;
			}
		}
		if (n == 0)
			return true;
		xc /= n;
		yc /= n;
		boolean b = true;
		double costheta = Math.cos(Math.toRadians(angleInDegrees));
		double sintheta = Math.sin(Math.toRadians(angleInDegrees));
		n = 0;
		double xi, yi;
		for (int i = 0; i < numberOfParticles; i++) {
			if (!gb[i].isSelected())
				continue;
			gb[i].storeCurrentState();
			xi = gb[i].rx;
			yi = gb[i].ry;
			gb[i].rx = xc + (xi - xc) * costheta - (yi - yc) * sintheta;
			gb[i].ry = yc + (xi - xc) * sintheta + (yi - yc) * costheta;
			if (!boundary.contains(gb[i].rx, gb[i].ry)) {
				b = false;
				n = i;
				break;
			}
		}
		if (b) {
			for (int i = 0; i < numberOfParticles; i++) {
				if (!gb[i].isSelected())
					continue;
				PointRestraint pr = gb[i].getRestraint();
				if (pr != null) {
					xi = pr.getX0();
					yi = pr.getY0();
					pr.setX0(xc + (xi - xc) * costheta - (yi - yc) * sintheta);
					pr.setY0(yc + (xi - xc) * sintheta + (yi - yc) * costheta);
				}
			}
			view.repaint();
			return true;
		}
		for (int i = 0; i <= n; i++)
			if (gb[i].isSelected())
				gb[i].restoreState();
		return false;
	}

	public boolean translateWholeModel(double dx, double dy) {
		boolean b = true;
		int n = 0;
		for (int i = 0; i < numberOfParticles; i++) {
			gb[i].storeCurrentState();
			gb[i].translateBy(dx, dy);
			if (!boundary.contains(gb[i].rx, gb[i].ry) || gb[i].intersects(boundary)) {
				b = false;
				n = i;
				break;
			}
		}
		if (b) {
			for (int i = 0; i < numberOfParticles; i++) {
				PointRestraint pr = gb[i].getRestraint();
				if (pr != null) {
					pr.setX0(pr.getX0() + dx);
					pr.setY0(pr.getY0() + dy);
				}
			}
		}
		else {
			for (int i = 0; i <= n; i++)
				gb[i].translateBy(-dx, -dy);
			return false;
		}
		if (obstacles != null) {
			int size = obstacles.size();
			RectangularObstacle obs = null;
			for (int i = 0; i < size; i++) {
				obs = obstacles.get(i);
				obs.storeCurrentState();
				obs.translateBy(dx, dy);
				if (!boundary.contains(obs)) {
					b = false;
					n = i;
					break;
				}
			}
		}
		if (!b) {
			for (int i = 0; i < numberOfParticles; i++)
				gb[i].translateBy(-dx, -dy);
			for (int i = 0; i <= n; i++)
				obstacles.get(i).translateBy(-dx, -dy);
			return false;
		}
		view.repaint();
		return true;
	}

	public void run() {
		double[] data = new double[numberOfParticles * 6];
		for (int i = 0; i < numberOfParticles; i++) {
			data[i] = gb[i].rx;
			data[i + numberOfParticles] = gb[i].ry;
			data[i + numberOfParticles * 2] = gb[i].theta;
			data[i + numberOfParticles * 3] = gb[i].vx;
			data[i + numberOfParticles * 4] = gb[i].vy;
			data[i + numberOfParticles * 5] = gb[i].omega;
		}
		stateHolder = new StateHolder(getModelTime(), heatBathActivated() ? heatBath.getExpectedTemperature() : 0,
				getNumberOfParticles(), data);
		super.run();
	}

	public boolean revert() {
		if (stateHolder == null)
			return false;
		setModelTime(stateHolder.getTime());
		setNumberOfParticles(stateHolder.getNumberOfParticles());
		double[] data = stateHolder.getParticleData();
		for (int i = 0; i < numberOfParticles; i++) {
			gb[i].translateTo(data[i], data[i + numberOfParticles]);
			gb[i].theta = data[i + numberOfParticles * 2];
			gb[i].vx = data[i + numberOfParticles * 3];
			gb[i].vy = data[i + numberOfParticles * 4];
			gb[i].omega = data[i + numberOfParticles * 5];
			gb[i].moveRPointer(0);
			gb[i].moveVPointer(0);
			gb[i].moveAPointer(0);
			gb[i].moveThetaPointer(0);
			gb[i].moveOmegaPointer(0);
			gb[i].moveAlphaPointer(0);
		}
		kine.setPointer(0);
		pote.setPointer(0);
		tote.setPointer(0);
		for (FloatQueue q : channelTs)
			q.setPointer(0);
		modelTimeQueue.setPointer(0);
		if (heatBathActivated()) {
			heatBath.setExpectedTemperature(stateHolder.getHeatBathTemperature());
		}
		view.repaint();
		notifyModelListeners(new ModelEvent(this, ModelEvent.MODEL_RESET));
		return true;
	}

	void record() {
		super.record();
		updateAllRQ();
		updateAllVQ();
		updateAllAQ();
		updateAllThetaQ();
		updateAllOmegaQ();
		updateAllAlphaQ();
	}

	void setTapePointer(int n) {
		if (!hasEmbeddedMovie())
			throw new RuntimeException("Cannot set pointer because there is no tape");
		modelTimeQueue.setPointer(n);
		for (int i = 0; i < numberOfParticles; i++) {
			GayBerneParticle p = gb[i];
			p.moveRPointer(n);
			p.moveVPointer(n);
			p.moveAPointer(n);
			p.moveThetaPointer(n);
			p.moveOmegaPointer(n);
			p.moveAlphaPointer(n);
		}
		kine.setPointer(n);
		pote.setPointer(n);
		tote.setPointer(n);
		for (FloatQueue q : channelTs)
			q.setPointer(n);
	}

	private void setQueueLength(int n) {
		modelTimeQueue.setLength(n);
		kine.setLength(n);
		pote.setLength(n);
		tote.setLength(n);
		for (int i = 0; i < numberOfParticles; i++) {
			GayBerneParticle p = gb[i];
			p.initializeRQ(n);
			p.initializeVQ(n);
			p.initializeAQ(n);
			p.initializeThetaQ(n);
			p.initializeOmegaQ(n);
			p.initializeAlphaQ(n);
		}
		for (FloatQueue q : channelTs)
			q.setLength(n);
	}

	public void activateEmbeddedMovie(boolean b) {
		if (b) {
			if (job != null && !job.contains(movieUpdater))
				job.add(movieUpdater);
			int m = movieUpdater.getInterval();
			modelTimeQueue.setInterval(m);
			kine.setInterval(m);
			pote.setInterval(m);
			tote.setInterval(m);
			for (FloatQueue q : channelTs)
				q.setInterval(m);
			setQueueLength(movie.getCapacity());
		}
		else {
			setQueueLength(-1);
			if (job != null && job.contains(movieUpdater))
				job.remove(movieUpdater);
		}
		movie.setCurrentFrameIndex(0);
		setRecorderDisabled(!b);
	}

	public boolean hasEmbeddedMovie() {
		if (numberOfParticles <= 0 || getTapePointer() <= 0)
			return false;
		if (gb[0].rQ == null || gb[0].rQ.isEmpty())
			return false;
		return true;
	}

	public void setUpdateList(boolean b) {
	}

	public void checkNeighborList() {
	}

	private void updateAllRQ() {
		int c = movie.getCapacity();
		for (int i = 0; i < numberOfParticles; i++) {
			try {
				gb[i].updateRQ();
			}
			catch (Exception e) {
				gb[i].initializeRQ(c);
				gb[i].updateRQ();
			}
		}
	}

	private void updateAllVQ() {
		int c = movie.getCapacity();
		for (int i = 0; i < numberOfParticles; i++) {
			try {
				gb[i].updateVQ();
			}
			catch (Exception e) {
				gb[i].initializeVQ(c);
				gb[i].updateVQ();
			}
		}
	}

	private void updateAllAQ() {
		int c = movie.getCapacity();
		for (int i = 0; i < numberOfParticles; i++) {
			try {
				gb[i].updateAQ();
			}
			catch (Exception e) {
				gb[i].initializeAQ(c);
				gb[i].updateAQ();
			}
		}
	}

	private void updateAllThetaQ() {
		int c = movie.getCapacity();
		for (int i = 0; i < numberOfParticles; i++) {
			try {
				gb[i].updateThetaQ();
			}
			catch (Exception e) {
				gb[i].initializeThetaQ(c);
				gb[i].updateThetaQ();
			}
		}
	}

	private void updateAllOmegaQ() {
		int c = movie.getCapacity();
		for (int i = 0; i < numberOfParticles; i++) {
			try {
				gb[i].updateOmegaQ();
			}
			catch (Exception e) {
				gb[i].initializeOmegaQ(c);
				gb[i].updateOmegaQ();
			}
		}
	}

	private void updateAllAlphaQ() {
		int c = movie.getCapacity();
		for (int i = 0; i < numberOfParticles; i++) {
			try {
				gb[i].updateAlphaQ();
			}
			catch (Exception e) {
				gb[i].initializeAlphaQ(c);
				gb[i].updateAlphaQ();
			}
		}
	}

	public String getName() {
		return name;
	}

	public void setName(String s) {
		name = s;
	}

	public void setBoundary(RectangularBoundary boundary) {
		this.boundary = boundary;
	}

	public RectangularBoundary getBoundary() {
		return boundary;
	}

	public void setMuNu(double mu, double nu) {
		this.mu = mu;
		this.nu = nu;
		gbForce = new GayBerneForce(mu, nu);
	}

	/**
	 * align atoms in a two-dimensional lattice. You may choose any number for the inputs, but only those that fall
	 * within the current box will be added.
	 */
	public void alignParticles(int m, int n, double length, double breadth, double xoffset, double yoffset,
			double xspacing, double yspacing) {
		if (xspacing < length || yspacing < breadth)
			throw new IllegalArgumentException("spacings may be too small");
		if (xoffset < 0 || yoffset < 0)
			throw new IllegalArgumentException("negative offset");
		if (m <= 0 || n <= 0)
			throw new IllegalArgumentException("m and n must be positive");
		int k = 0;
		double xmin, xmax, ymin, ymax;
		double w = boundary.getWidth();
		double h = boundary.getHeight();
		double x = boundary.getX();
		double y = boundary.getY();
		terminate: for (int i = 0; i < m; i++) {
			for (int j = 0; j < n; j++) {
				xmin = xoffset + i * xspacing - length * 0.5;
				xmax = xoffset + i * xspacing + length * 0.5;
				ymin = yoffset + j * yspacing - breadth * 0.5;
				ymax = yoffset + j * yspacing + breadth * 0.5;
				if (xmin >= x && xmax < x + w && ymin >= y && ymax < y + h) {
					gb[k].rx = xoffset + i * xspacing;
					gb[k].ry = yoffset + j * yspacing;
					gb[k].mass = 1.0;
					gb[k].charge = 0;
					gb[k].restraint = null;
					gb[k].length = length;
					gb[k].breadth = breadth;
					gb[k].theta = 0.0;
					gb[k].eeVsEs = 1.0;
					gb[k].epsilon0 = 0.1;
					gb[k].inertia = 0.5 * gb[k].mass * gb[k].length * gb[k].breadth * gb[k].mdFactor;
					k++;
					if (k >= gb.length)
						break terminate;
				}
			}
		}
		setNumberOfParticles(k);
	}

	public Particle getParticle(int i) {
		if (i < 0 || i >= gb.length)
			throw new IllegalArgumentException("No GB with index " + i);
		return gb[i];
	}

	public GayBerneParticle[] getParticles() {
		return gb;
	}

	/** set time steplength for integrating the equation of motion */
	public void setTimeStep(double timeStep) {
		super.setTimeStep(timeStep);
		dt = timeStep;
		dt2 = dt * dt * 0.5;
	}

	/** get time steplength */
	public double getTimeStep() {
		return dt;
	}

	public void setCutOff(int cutoff) {
		if (cutoff < 0)
			throw new IllegalArgumentException("cutoff cannot be negative");
		rCutOff = cutoff;
	}

	public int getCutOff() {
		return (int) rCutOff;
	}

	public synchronized void setFriction(float friction) {
		for (int i = 0; i < numberOfParticles; i++)
			gb[i].setFriction(friction);
	}

	public synchronized int getNumberOfParticles() {
		return numberOfParticles;
	}

	public synchronized void setNumberOfParticles(int n) {
		numberOfParticles = n;
		view.notifyNOPChange();
	}

	/** @return the current volume of the system */
	public double getVolume() {
		return boundary.width * boundary.height;
	}

	/**
	 * rescale the linear and angular velocities so that the resultant temperature equals to the passed value.
	 */
	public synchronized void setTemperature(double temperature) {
		if (temperature < ZERO)
			temperature = 0.0;
		double temp1 = getKin() * UNIT_EV_OVER_KB;
		if (temp1 < ZERO) {
			assignTemperature(100.0);
			temp1 = getKin() * UNIT_EV_OVER_KB;
		}
		rescaleVelocities(Math.sqrt(temperature / temp1));
	}

	public double getTemperature() {
		return getKin() * UNIT_EV_OVER_KB;
	}

	public double getTemperature(byte type, Shape shape) {
		double result = 0.0;
		int n = 0;
		GayBerneParticle p = null;
		for (int i = 0; i < numberOfParticles; i++) {
			p = gb[i];
			if (shape == null || p.isCenterOfMassContained(shape)) {
				n++;
				result += (p.vx * p.vx + p.vy * p.vy) * p.mass + p.inertia * p.omega * p.omega;
			}
		}
		if (n == 0)
			return 0;
		// the prefactor 0.5 doesn't show up here because it has been included in the mass conversion factor.
		return result * EV_CONVERTER * UNIT_EV_OVER_KB / n;
	}

	public double getKineticEnergy(byte type, Shape shape) {
		double result = 0.0;
		GayBerneParticle p = null;
		for (int i = 0; i < numberOfParticles; i++) {
			p = gb[i];
			if (shape == null || p.isCenterOfMassContained(shape)) {
				result += (p.vx * p.vx + p.vy * p.vy) * p.mass + p.inertia * p.omega * p.omega;
			}
		}
		// the prefactor 0.5 doesn't show up here because it has been included in the mass conversion factor.
		return result * EV_CONVERTER;
	}

	public int getParticleCount(byte type, Shape shape) {
		if (shape == null)
			return numberOfParticles;
		int n = 0;
		for (int i = 0; i < numberOfParticles; i++) {
			if (shape == null || gb[i].isCenterOfMassContained(shape)) {
				n++;
			}
		}
		return n;
	}

	public double getAverageSpeed(String direction, byte type, Shape shape) {
		double v = 0;
		boolean isVx = "x".equalsIgnoreCase(direction);
		boolean isVy = "y".equalsIgnoreCase(direction);
		int n = 0;
		for (int i = 0; i < numberOfParticles; i++) {
			if (shape == null || gb[i].isCenterOfMassContained(shape)) {
				if (isVx)
					v += gb[i].vx;
				else if (isVy)
					v += gb[i].vy;
				n++;
			}
		}
		return n == 0 ? 0 : v / n;
	}

	/** change the temperature by percentage */
	public void changeTemperature(double percent) {
		if (percent < -1.0)
			percent = -1.0;
		if (!heatBathActivated()) {
			rescaleVelocities(Math.sqrt(percent + 1.0));
		}
		else {
			heatBath.changeExpectedTemperature(percent);
		}
	}

	public void transferHeat(double amount) {
		if (numberOfParticles <= 0)
			return;
		double k0 = getKin();
		if (k0 < ZERO)
			assignTemperature(1);
		for (int i = 0; i < numberOfParticles; i++) {
			GayBerneParticle p = gb[i];
			k0 = EV_CONVERTER * (p.mass * (p.vx * p.vx + p.vy * p.vy) + p.inertia * p.omega * p.omega);
			if (k0 <= ZERO)
				k0 = ZERO;
			k0 = (k0 + amount) / k0;
			if (k0 <= ZERO)
				k0 = ZERO;
			k0 = Math.sqrt(k0);
			p.vx *= k0;
			p.vy *= k0;
			p.omega *= k0;
		}
	}

	public void transferHeatToParticles(List list, double amount) {
		if (list == null || list.isEmpty())
			return;
		double k0 = getKinForParticles(list);
		if (k0 < ZERO)
			assignTemperature(list, 1);
		GayBerneParticle p = null;
		Object o = null;
		for (Iterator it = list.iterator(); it.hasNext();) {
			o = it.next();
			if (o instanceof GayBerneParticle) {
				p = (GayBerneParticle) o;
				k0 = EV_CONVERTER * (p.mass * (p.vx * p.vx + p.vy * p.vy) + p.inertia * p.omega * p.omega);
				if (k0 <= ZERO)
					k0 = ZERO;
				k0 = (k0 + amount) / k0;
				if (k0 <= ZERO)
					k0 = ZERO;
				k0 = Math.sqrt(k0);
				p.vx *= k0;
				p.vy *= k0;
				p.omega *= k0;
			}
		}
	}

	private void assignTemperature(List list, double temperature) {
		if (list == null || list.isEmpty())
			return;
		if (temperature < ZERO)
			temperature = 0.0;
		double rtemp = Math.sqrt(temperature) * VT_CONVERSION_CONSTANT;
		Object o = null;
		GayBerneParticle p = null;
		for (Iterator it = list.iterator(); it.hasNext();) {
			o = it.next();
			if (o instanceof GayBerneParticle) {
				p = (GayBerneParticle) o;
				p.omega = rtemp * RANDOM.nextGaussian() / (0.5 * p.getLength());
				p.vx = rtemp * RANDOM.nextGaussian();
				p.vy = rtemp * RANDOM.nextGaussian();
			}
		}
	}

	/**
	 * assign velocities to particles according to the Boltzman-Maxwell distribution
	 */
	public synchronized void assignTemperature(double temperature) {
		if (temperature < ZERO)
			temperature = 0.0;
		double rtemp = Math.sqrt(temperature) * VT_CONVERSION_CONSTANT;
		double sumVx = 0.0;
		double sumVy = 0.0;
		double sumMass = 0.0;
		for (int i = 0; i < numberOfParticles; i++) {
			gb[i].omega = 0.0;
			gb[i].vx = rtemp * RANDOM.nextGaussian();
			gb[i].vy = rtemp * RANDOM.nextGaussian();
			sumVx += gb[i].vx * gb[i].mass;
			sumVy += gb[i].vy * gb[i].mass;
			sumMass += gb[i].mass;
		}
		if (sumMass > ZERO) {
			sumVx /= sumMass;
			sumVy /= sumMass;
			if (numberOfParticles > 1) {
				for (int i = 0; i < numberOfParticles; i++) {
					gb[i].vx -= sumVx;
					gb[i].vy -= sumVy;
				}
			}
			setTemperature(temperature);
		}
	}

	synchronized void advance(int time) {
		if (heatBathActivated()) {
			if (heatBath.getExpectedTemperature() < 0.01)
				return;
		}
		predictor();
		pot = computeForce(time);
		corrector();
	}

	/* TODO: */
	synchronized void advanceNPT(int time) {
		advance(time);
	}

	/** @return the total translational and rotational kinetic energy */
	public synchronized double getKin() {
		if (numberOfParticles <= 0)
			return 0.0;
		double kineticEnergy = 0.0;
		for (int i = 0; i < numberOfParticles; i++) {
			GayBerneParticle p = gb[i];
			kineticEnergy += (p.vx * p.vx + p.vy * p.vy) * p.mass;
			kineticEnergy += p.inertia * p.omega * p.omega;
		}
		kin = kineticEnergy * EV_CONVERTER / numberOfParticles;
		// the prefactor 0.5 doesn't show up here because it has been included in the mass unit conversion factor.
		return kin;
	}

	double getKinForParticles(List list) {
		if (list == null || list.isEmpty())
			return 0.0;
		double x = 0.0;
		GayBerneParticle p = null;
		int n = 0;
		Object o = null;
		synchronized (list) {
			for (Iterator it = list.iterator(); it.hasNext();) {
				o = it.next();
				if (o instanceof GayBerneParticle) {
					p = (GayBerneParticle) o;
					x += (p.vx * p.vx + p.vy * p.vy) * p.mass;
					x += p.inertia * p.omega * p.omega;
					n++;
				}
			}
		}
		if (n == 0)
			return 0.0;
		// the prefactor 0.5 doesn't show up here because of mass unit conversion.
		return x * EV_CONVERTER / n;
	}

	/**
	 * Implement the fixed-steplength steepest descents method.
	 * 
	 * @param delta
	 *            the steplength of minimization
	 * @return the potential energy after this step of minization
	 */
	public double steepestDescents(double delta) {
		if (numberOfParticles == 1)
			return -1.0;
		double pot = computeForce(-1);
		double d;
		double sumgrad = 0.0;
		for (int i = 0; i < numberOfParticles; i++) {
			sumgrad += gb[i].fx * gb[i].fx + gb[i].fy * gb[i].fy;
			d = gb[i].tau / (0.5 * (gb[i].length + gb[i].breadth));
			sumgrad += d * d;
		}
		sumgrad = Math.sqrt(sumgrad);
		if (sumgrad > 0) {
			for (int i = 0; i < numberOfParticles; i++) {
				gb[i].rx += gb[i].fx / sumgrad * delta;
				gb[i].ry += gb[i].fy / sumgrad * delta;
				gb[i].theta += gb[i].tau / (0.5 * (gb[i].length + gb[i].breadth) * sumgrad) * delta;
			}
			boundary.setRBC();
		}
		return pot;
	}

	public void parameterChanged(ParameterChangeEvent e) {
	}

	public void clear() {
		super.clear();
		setNumberOfParticles(0);
		for (GayBerneParticle p : gb) {
			p.setMovable(true);
			p.setVisible(true);
			p.setRestraint(null);
			p.setCharge(0.0);
			p.setUserField(null);
			p.setShowRTraj(false);
			p.setShowRMean(false);
			p.setShowFMean(false);
			p.clearMeasurements();
			p.setDipoleMoment(0.0);
			p.setMarked(false);
			p.setSelected(false);
			p.setVelocitySelection(false);
		}
		if (job != null)
			job.processPendingRequests();
	}

	public String toString() {
		return "<Meso Model> " + getProperty("filename");
	}

	synchronized void predictor() {
		for (int i = 0; i < numberOfParticles; i++)
			gb[i].predict(dt, dt2);
		putGBsInBounds();
	}

	synchronized void corrector() {
		if (numberOfParticles == 1) {
			gb[0].ax = gb[0].fx;
			gb[0].ay = gb[0].fy;
			gb[0].alpha = gb[0].tau;
			gb[0].fx *= gb[0].mass;
			gb[0].fy *= gb[0].mass;
			gb[0].tau *= gb[0].inertia;
			return;
		}
		double halfDt = dt * 0.5;
		for (int i = 0; i < numberOfParticles; i++)
			gb[i].correct(halfDt);
	}

	/* TODO: */
	boolean needMinimization() {
		return false;
	}

	synchronized void putGBsInBounds() {
		switch (boundary.getType()) {
		case RectangularBoundary.DBC_ID:
			boundary.setRBC();
			break;
		case RectangularBoundary.RBC_ID:
			boundary.setRBC();
			break;
		case RectangularBoundary.PBC_ID:
			boundary.setPBC();
			break;
		}
	}

	/**
	 * compute forces on the atoms from the potentials. This is the most expensive part of calculation.
	 * 
	 * @param time
	 *            the current time, used in computing time-dependent forces
	 * @return potential energy per atom
	 */
	public synchronized double computeForce(final int time) {

		double etemp = 0.0;
		double vsum = 0.0;
		for (int i = 0; i < numberOfParticles; i++) {
			gb[i].fx = 0;
			gb[i].fy = 0;
			gb[i].tau = 0;
		}

		if (numberOfParticles == 1) {

			gb[0].fx = gb[0].hx / gb[0].mass;
			gb[0].fy = gb[0].hy / gb[0].mass;
			gb[0].tau = gb[0].gamma / gb[0].inertia;

			if (gb[0].friction > 0.0f) {
				double dmp = GF_CONVERSION_CONSTANT * gb[0].friction * universe.getViscosity() / gb[0].mass;
				gb[0].fx -= dmp * gb[0].vx;
				gb[0].fy -= dmp * gb[0].vy;
				gb[0].tau -= GF_CONVERSION_CONSTANT * gb[0].friction * universe.getViscosity() * gb[0].omega
						/ gb[0].inertia;
			}

			VectorField f;
			for (int i = 0, nf = fields.size(); i < nf; i++) {
				f = fields.elementAt(i);
				if (f instanceof GravitationalField) {
					GravitationalField gf = (GravitationalField) f;
					gf.dyn(gb[0]);
					etemp = gf.getPotential(gb[0], time);
					vsum += etemp;
				}
				else if (f instanceof ElectricField) {
					if (Math.abs(gb[0].charge) > 0 || Math.abs(gb[0].dipoleMoment) > 0) {
						ElectricField ef = (ElectricField) f;
						ef.dyn(universe.getDielectricConstant(), gb[0], time);
						etemp = ef.getPotential(gb[0], time);
						vsum += etemp;
					}
				}
				else if (f instanceof MagneticField) {
					if (Math.abs(gb[0].charge) > 0 || Math.abs(gb[0].dipoleMoment) > 0) {
						MagneticField mf = (MagneticField) f;
						mf.dyn(gb[0]);
						etemp = mf.getPotential(gb[0], time);
						vsum += etemp;
					}
				}
			}

			if (gb[0].restraint != null) {
				gb[0].restraint.dyn(gb[0]);
				etemp = gb[0].restraint.getEnergy(gb[0]);
				vsum += etemp;
			}

			if (gb[0].getUserField() != null) {
				gb[0].getUserField().dyn(gb[0]);
			}

			return vsum;

		}

		for (int i = 0; i < numberOfParticles - 1; i++) {
			for (int j = i + 1; j < numberOfParticles; j++) {
				x_ij = gb[i].rx - gb[j].rx;
				y_ij = gb[i].ry - gb[j].ry;
				// apply the minimum image convention
				if (boundary.getType() == RectangularBoundary.PBC_ID) {
					if (x_ij > boundary.width * 0.5) {
						x_ij -= boundary.width;
					}
					if (x_ij <= -boundary.width * 0.5) {
						x_ij += boundary.width;
					}
					if (y_ij > boundary.height * 0.5) {
						y_ij -= boundary.height;
					}
					if (y_ij <= -boundary.height * 0.5) {
						y_ij += boundary.height;
					}
				}
				r_ij = Math.hypot(x_ij, y_ij);
				if (r_ij <= rCutOff) {
					gbForce.checkin(gb[i], gb[j], x_ij, y_ij, r_ij);
					etemp = gbForce.energy();
					vsum += etemp;
					gb[i].tau -= gbForce.torque_i();
					gb[j].tau -= gbForce.torque_j();
					fxij = gbForce.fx_i();
					fyij = gbForce.fy_i();
					gb[i].fx -= fxij;
					gb[i].fy -= fyij;
					gb[j].fx += fxij;
					gb[j].fy += fyij;
				}
				if ((Math.abs(gb[i].dipoleMoment) > Particle.ZERO || Math.abs(gb[i].charge) > Particle.ZERO)
						&& (Math.abs(gb[j].dipoleMoment) > Particle.ZERO || Math.abs(gb[j].charge) > Particle.ZERO)) {
					esForce.checkin(universe, gb[i], gb[j], x_ij, y_ij, r_ij);
					etemp = esForce.energy();
					vsum += etemp;
					gb[i].tau -= esForce.torque_i();
					gb[j].tau -= esForce.torque_j();
					fxij = esForce.fx_i();
					fyij = esForce.fy_i();
					gb[i].fx -= fxij;
					gb[i].fy -= fyij;
					gb[j].fx += fxij;
					gb[j].fy += fyij;
				}
			}
		}

		double inverseMass = 1.0;
		for (int i = 0; i < numberOfParticles; i++) {
			gb[i].fx += gb[i].hx;
			gb[i].fy += gb[i].hy;
			gb[i].tau += gb[i].gamma;
			inverseMass = GF_CONVERSION_CONSTANT / gb[i].mass;
			gb[i].fx *= inverseMass;
			gb[i].fy *= inverseMass;
			gb[i].tau *= GF_CONVERSION_CONSTANT / gb[i].inertia;
		}

		// pointwise space restraints do not contribute to internal pressure,
		// but they maintains the energy conservation law.
		for (int i = 0; i < numberOfParticles; i++) {
			GayBerneParticle p = gb[i];
			if (p.restraint != null) {
				p.restraint.dyn(p);
				vsum += p.restraint.getEnergy(p);
			}
			if (p.friction > 0.0f) {
				inverseMass = GF_CONVERSION_CONSTANT * universe.getViscosity() * p.friction / p.mass;
				p.fx -= inverseMass * p.vx;
				p.fy -= inverseMass * p.vy;
				p.tau -= GF_CONVERSION_CONSTANT * universe.getViscosity() * p.friction * p.omega / p.inertia;
			}
			if (p.getUserField() != null) {
				p.getUserField().dyn(p);
			}
		}

		VectorField f;
		for (int n = 0; n < fields.size(); n++) {
			f = fields.elementAt(n);
			if (f instanceof GravitationalField) {
				GravitationalField gf = (GravitationalField) f;
				for (int i = 0; i < numberOfParticles; i++) {
					gf.dyn(gb[i]);
					etemp = gf.getPotential(gb[i], time);
					vsum += etemp;
				}
			}
			else if (f instanceof ElectricField) {
				ElectricField ef = (ElectricField) f;
				for (int i = 0; i < numberOfParticles; i++) {
					if (Math.abs(gb[i].charge) > 0 || Math.abs(gb[i].dipoleMoment) > 0) {
						ef.dyn(universe.getDielectricConstant(), gb[i], time);
						etemp = ef.getPotential(gb[i], time);
						vsum += etemp;
					}
				}
			}
			else if (f instanceof MagneticField) {
				MagneticField mf = (MagneticField) f;
				for (int i = 0; i < numberOfParticles; i++) {
					if (Math.abs(gb[i].charge) > 0 || Math.abs(gb[i].dipoleMoment) > 0) {
						mf.dyn(gb[i]);
						etemp = mf.getPotential(gb[i], time);
						vsum += etemp;
					}
				}
			}
		}

		return vsum / numberOfParticles;

	}

	synchronized void rescaleVelocities(double ratio) {
		for (int i = 0; i < numberOfParticles; i++) {
			gb[i].vx *= ratio;
			gb[i].vy *= ratio;
			gb[i].omega *= ratio;
		}
	}

	void decode(XMLDecoder in) throws Exception {

		super.decode(in);

		monitor.setProgressMessage("Reading model state...");
		final State state = (State) in.readObject();
		setTimeStep(state.getTimeStep());
		setInitializationScript(state.getScript());
		setUniverse(state.getUniverse() != null ? state.getUniverse() : new Universe());
		enableReminder(state.getReminderEnabled());
		if (isReminderEnabled()) {
			reminder.setInterval(state.getReminderInterval());
			reminder.setLifetime(state.getRepeatReminder() ? Loadable.ETERNAL : reminder.getInterval());
			reminderMessage = state.getReminderMessage();
		}
		monitor.setMaximum(2 * state.getNumberOfParticles() + 40);
		String mprop = null;
		for (Iterator it = state.getProperties().keySet().iterator(); it.hasNext();) {
			mprop = (String) it.next();
			if (!mprop.equals("url") && !mprop.equals("filename") && !mprop.equals("codebase") && !mprop.equals("date")
					&& !mprop.equals("size"))
				putProperty(mprop, state.getProperties().get(mprop));
		}
		monitor.setProgressMessage("Retrieving obstacles...");
		setObstacles(state.getObstacles());
		kine.clear();
		pote.clear();
		tote.clear();
		for (FloatQueue q : channelTs)
			q.clear();
		Arrays.fill(channels, 0);
		movieUpdater.setInterval(state.getFrameInterval());
		if (heatBath != null)
			heatBath.destroy();
		heatBath = state.getHeatBath();
		if (heatBath != null)
			heatBath.setModel(this);

		monitor.setProgressMessage("Reading view parameters...");
		final MesoView.State vs = (MesoView.State) in.readObject();
		view.setRenderingMethod(vs.getRenderingMethod());
		view.setFillMode(vs.getFillMode());
		view.setBackground(vs.getBackground());
		view.setMarkColor(new Color(vs.getMarkColor()));
		view.setRestraintStyle(vs.getRestraintStyle());
		view.setEnergizer(vs.getEnergizer());
		view.setDrawCharge(vs.getDrawCharge());
		view.setDrawDipole(vs.getDrawDipole());
		view.setDrawExternalForce(vs.getDrawExternalForce());
		view.setShowParticleIndex(vs.getShowParticleIndex());
		view.setShowClock(vs.getShowClock());
		view.showLinearMomenta(vs.getShowVVectors());
		view.showAngularMomenta(vs.getShowOmegas());

		int n = state.getNumberOfParticles();
		if (n > gb.length) {
			n = gb.length;
			EventQueue.invokeLater(new Runnable() {
				public void run() {
					JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(view),
							"The model contains more particles than default.", "Error", JOptionPane.ERROR_MESSAGE);
				}
			});
		}
		GayBerneParticle gbp = null;
		int pointer = 0;
		while (pointer < n) {
			if (pointer % 10 == 0)
				monitor.setProgressMessage("Reading particle " + pointer + "...");
			gbp = (GayBerneParticle) in.readObject();
			gb[pointer].destroy();
			gb[pointer] = gbp;
			gb[pointer].setIndex(pointer);
			gb[pointer].setModel(this);
			gb[pointer].tau = gb[pointer].alpha * gb[pointer].inertia;
			pointer++;
		}
		setNumberOfParticles(pointer);

		loadLayeredComponent(vs);

		monitor.setProgressMessage("Retrieving boundary...");
		boundary.constructFromDelegate(state.getBoundary());
		if (state.getFields() != null && !state.getFields().isEmpty()) {
			monitor.setProgressMessage("Retrieving fields...");
			addAllNonLocalFields(state.getFields());
		}
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				view.resize(state.getBoundary().getViewSize(), true);
			}
		});

		initializeJob();
		addCustomTasks(state.getTasks());
		if (heatBath != null && !job.contains(heatBath))
			job.add(heatBath);
		job.processPendingRequests();

	}

	void encode(XMLEncoder out) throws Exception {

		// remove dependencies on non-serializable fields from objects to be serialized
		Component savedAncestor = view.getAncestor();
		view.setAncestor(null);
		for (GayBerneParticle p : gb) {
			p.setSelected(false);
			p.setSelectedToResize(false);
			p.setSelectedToRotate(false);
		}

		MesoView.State vs = new MesoView.State();
		vs.setRenderingMethod(view.getRenderingMethod());
		vs.setBackground(view.getBackground());
		vs.setMarkColor(view.getMarkColor().getRGB());
		if (view.getFillMode() != FillMode.getNoFillMode())
			vs.setFillMode(view.getFillMode());
		vs.setRestraintStyle(view.getRestraintStyle());
		vs.setEnergizer(view.getEnergizer());
		vs.setDrawCharge(view.getDrawCharge());
		vs.setDrawDipole(view.getDrawDipole());
		vs.setDrawExternalForce(view.getDrawExternalForce());
		vs.setShowParticleIndex(view.getShowParticleIndex());
		vs.setShowClock(view.getShowClock());
		vs.setShowVVectors(view.linearMomentaShown());
		vs.setShowOmegas(view.angularMomentaShown());
		ImageComponent[] im = view.getImages();
		if (im.length > 0) {
			ImageComponent.Delegate[] icd = new ImageComponent.Delegate[im.length];
			for (int i = 0; i < im.length; i++)
				icd[i] = new ImageComponent.Delegate(im[i]);
			vs.setImages(icd);
		}
		TextBoxComponent[] tb = view.getTextBoxes();
		if (tb.length > 0) {
			TextBoxComponent.Delegate[] tbd = new TextBoxComponent.Delegate[tb.length];
			for (int i = 0; i < tb.length; i++)
				tbd[i] = new TextBoxComponent.Delegate(tb[i]);
			vs.setTextBoxes(tbd);
		}
		LineComponent[] lc = view.getLines();
		if (lc.length > 0) {
			LineComponent.Delegate[] lcd = new LineComponent.Delegate[lc.length];
			for (int i = 0; i < lc.length; i++)
				lcd[i] = new LineComponent.Delegate(lc[i]);
			vs.setLines(lcd);
		}
		RectangleComponent[] rc = view.getRectangles();
		if (rc.length > 0) {
			RectangleComponent.Delegate[] rcd = new RectangleComponent.Delegate[rc.length];
			for (int i = 0; i < rc.length; i++)
				rcd[i] = new RectangleComponent.Delegate(rc[i]);
			vs.setRectangles(rcd);
		}
		EllipseComponent[] ec = view.getEllipses();
		if (ec.length > 0) {
			EllipseComponent.Delegate[] ecd = new EllipseComponent.Delegate[ec.length];
			for (int i = 0; i < ec.length; i++)
				ecd[i] = new EllipseComponent.Delegate(ec[i]);
			vs.setEllipses(ecd);
		}

		State state = new State();
		if (job != null)
			state.addTasks(job.getCustomTasks());
		state.setUniverse(universe);
		state.setProperties(properties);
		state.setObstacles(obstacles.getList());
		state.setBoundary(boundary.createDelegate());
		state.setFields(fields);
		state.setNumberOfParticles(numberOfParticles);
		state.setFrameInterval(movieUpdater.getInterval());
		state.setTimeStep(getTimeStep());
		state.setScript(initializationScript);
		state.setReminderEnabled(isReminderEnabled());
		if (isReminderEnabled()) {
			state.setRepeatReminder(reminder.getLifetime() == Loadable.ETERNAL);
			state.setReminderInterval(reminder.getInterval());
			state.setReminderMessage(reminderMessage);
		}
		if (heatBath != null) {
			heatBath.setModel(null);
			state.setHeatBath(heatBath);
		}

		monitor.setProgressMessage("Writing model...");
		monitor.setMaximum(state.getNumberOfParticles() + 4);

		Object prop = removeProperty("old url"); // hack to remove temporary properties
		out.writeObject(state);
		out.writeObject(vs);
		out.flush();
		if (prop != null)
			putProperty("old url", prop); // add temporary properties back

		for (int i = 0; i < numberOfParticles; i++) {
			monitor.setProgressMessage("Writing GB " + i + "...");
			out.writeObject(gb[i]);
			out.flush();
		}

		// restore non-serializable fields for the serialized objects
		if (heatBath != null)
			heatBath.setModel(this);
		view.setAncestor(savedAncestor);

	}

	/* show the <i>i</i>-th frame of the movie */
	void showMovieFrame(int frame) {
		if (frame < 0 || movie.length() <= 0)
			return;
		if (frame >= movie.length())
			throw new IllegalArgumentException("Frame " + frame + " does not exist");
		view.showFrameOfImages(frame);
		modelTime = modelTimeQueue.getData(frame);
		for (int i = 0; i < numberOfParticles; i++) {
			GayBerneParticle p = gb[i];
			p.rx = p.rQ.getQueue1().getData(frame);
			p.ry = p.rQ.getQueue2().getData(frame);
			p.vx = p.vQ.getQueue1().getData(frame);
			p.vy = p.vQ.getQueue2().getData(frame);
			p.ax = p.aQ.getQueue1().getData(frame);
			p.ay = p.aQ.getQueue2().getData(frame);
			p.fx = p.ax * p.mass;
			p.fy = p.ay * p.mass;
			p.theta = p.thetaQ.getData(frame);
			p.omega = p.omegaQ.getData(frame);
			p.alpha = p.alphaQ.getData(frame);
			p.tau = p.alpha * p.inertia;
		}
	}

	public static class State extends MDModel.State {
		public State() {
			super();
		}
	}

}