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
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.geom.Rectangle2D;
import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.event.ChangeListener;

import org.concord.modeler.ModelerUtilities;
import org.concord.modeler.draw.FillMode;
import org.concord.modeler.event.ModelEvent;
import org.concord.modeler.process.AbstractLoadable;
import org.concord.modeler.process.Loadable;
import org.concord.modeler.ui.IconPool;
import org.concord.modeler.util.DataQueue;
import org.concord.modeler.util.FloatQueue;
import org.concord.modeler.util.ObjectQueue;
import org.concord.molbio.engine.Aminoacid;
import org.concord.mw2d.AtomisticView;
import org.concord.mw2d.MDView;
import org.concord.mw2d.event.ParameterChangeEvent;
import org.concord.mw2d.event.UpdateEvent;

import static org.concord.mw2d.models.Element.*;

/**
 * <p>
 * This is the base class to derive an atomic-scale model. <b>Warning</b>: This class being non-abstract is a side
 * effect of backward compatibility with Pedagogica scripts. Please do NOT use this class directly. Use
 * <code>MolecularModel</code> instead.
 * </p>
 * 
 * <p>
 * This class contains an NVE/NVT/NPT molecular dynamics (MD) engine. It performs the standard steps needed to unfold a
 * model:
 * </p>
 * <ol>
 * <li> Initialize the force fields;
 * <li> Compute forces;
 * <li> Do energy minimization;
 * <li> Integrate the classical equations of motion;
 * <li> Return kinetic energy and potential energies;
 * </ol>
 * <p>
 * The potential energy function includes the following non-bonded interaction terms:
 * </p>
 * <ol>
 * <li> Lennard-Jones potential: All atoms are created with it;
 * <li> Electrostatic potential: Only charged atoms have it;
 * <li> Axilrod-Teller potential: Optional three-body interactomic potential, seldom needed but reserved for academic
 * interest, if turned on it greatly slows down the engine;
 * <li> Pointwise harmonic restraint: Used to pinpoint an atom around a position;
 * </ol>
 * 
 * <p>
 * The MD uses Verlet's neighbor list procedure to accelerate computation. A smoothing function can be applied to reduce
 * the numerical errors due to potential truncation.
 * </p>
 * 
 * @author Charles Xie
 */

public abstract class AtomicModel extends MDModel {

	/** maximum number of atoms allowed */
	private static short NMAX = 500;

	public final static String COMPUTE_PHOTON = "Photon Absorption and Emission";

	/* the atom array */
	Atom[] atom;
	AtomisticView view;
	Element nt;
	Element pl;
	Element ws;
	Element ck;
	Element mo; // fixed-size element used to construct molecular objects.
	Element sp; // fixed element used to represent the sugar-phosphate part of nucleotide
	Affinity affinity;
	QuantumRule quantumRule;

	final static Element[] aminoAcidElement = new Element[20];
	final static Element[] nucleotideElement = new Element[5];

	volatile int numberOfAtoms;

	Grid grid;
	boolean updateList = true;
	int[] neighborList, pointer;

	FloatQueue[] kep = new FloatQueue[4];
	FloatQueue[] msd = new FloatQueue[4];

	/* the time step for unfolding this model */
	private volatile double timeStep = 2.0;
	private double timeStep2 = timeStep * timeStep * 0.5;

	private static final float SIX_TIMES_UNIT_FORCE = 6.0f * GF_CONVERSION_CONSTANT;

	/* geometric parameters */
	private float rCutOff = 2.0f, rList = 2.5f;
	private double[][] cutOffSquareMatrix = new double[Element.NMAX][Element.NMAX];
	private double[][] listSquareMatrix = new double[Element.NMAX][Element.NMAX];

	/* force parameters */
	private boolean interCoulomb = true, hasCoulomb;
	private int crossRepulsionIntensity = 10;
	private boolean cutOffShift = true;
	private double[][] poten_LJ = new double[Element.NMAX][Element.NMAX];
	private double[][] slope_LJ = new double[Element.NMAX][Element.NMAX];
	private float hbStrength = 1.0f;

	/* internal parameters and arrays */
	boolean ljBetweenBondPairs = true;
	boolean[][] bondTable;
	private float[][] epsab, sigab;
	private double[] rx0, ry0;
	private volatile boolean updateParArray;
	private double eKT, eLJ, eES, eEF, eGF, eAF, eRS;
	private int nlist;
	private int jbeg, jend;
	private double rxi, ryi, fxi, fyi, rxij, ryij, rijsq, xbox, ybox;
	private double sr2, sr6, sr12, vij, wij, fij, fxij, fyij;

	private List<Atom> typeList;
	private List<Photon> photonList;
	private ObjectQueue photonQueue;
	private LightSource lightSource;
	private boolean photonEnabled;
	private boolean collisionalDeexcitation;
	private boolean atomFlowEnabled;
	AtomSource atomSource;

	/* analysis tools */
	private TimeSeriesGenerator tsGenerator;
	private Mvd mvd;
	private Pcf pcf;
	private Tcf tcf;

	/* the subtask for radiating photons */
	private Loadable photonGun = new AbstractLoadable(1000) {
		public void execute() {
			shootPhotons();
		}

		public String getName() {
			return "Light source";
		}

		public int getLifetime() {
			return ETERNAL;
		}

		public String getDescription() {
			return "This task models a light source, which periodically radiates photons.";
		}
	};

	/*
	 * the subtask for simulating the electronic dynamics, which is viewed as the random walk of electrons in the energy
	 * level space and described by the master equation.
	 */
	private Loadable electronicDynamics = new AbstractLoadable(20) {
		public void execute() {
			thermalExciteAtoms();
			photonHitAtom();
			deexciteElectrons();
		}

		public String getName() {
			return "Electronic dynamics";
		}

		public int getLifetime() {
			return ETERNAL;
		}

		public String getDescription() {
			return "This task models the electronic dynamics, which consists of the following\nprocesses: excitation of electrons through the absorption of photons,\nexcitation of electrons through the absorption of kinetic energy, stimulated\nemission induced by incident photons, spontaneous emission, and radiationless\ntransition.";
		}
	};

	private Loadable updateGrid = new AbstractLoadable(50) {
		public void execute() {
			if (grid == null)
				return;
			grid.cellAccumulate(numberOfAtoms, atom);
			view.repaint();
		}

		public String getName() {
			return "Updating grid";
		}
	};

	/** this constructs an empty model */
	public AtomicModel() {
		this(DataQueue.DEFAULT_SIZE);
	}

	public AtomicModel(int xbox, int ybox) {
		this();
		if (xbox <= 0 || ybox <= 0)
			throw new IllegalArgumentException("width and height must be greater than 0");
		boundary.setRect(0, 0, xbox, ybox);
		boundary.setView(boundary);
		init(NMAX);
	}

	/**
	 * create a model of the given size
	 * 
	 * @param xbox
	 *            width of the simulation box
	 * @param ybox
	 *            height of the simulation box
	 * @param tapeLength
	 *            the length of the recorder tape
	 */
	public AtomicModel(int xbox, int ybox, int tapeLength) {
		this(tapeLength);
		if (xbox <= 0 || ybox <= 0)
			throw new IllegalArgumentException("width and height must be greater than 0");
		boundary.setRect(0, 0, xbox, ybox);
		boundary.setView(boundary);
		init(NMAX);
	}

	private AtomicModel(int tapeLength) {

		super();

		String s = System.getProperty("nmax2d");
		if (s != null) {
			try {
				short i = Short.parseShort(s);
				if (i > NMAX)
					NMAX = i;
			}
			catch (Exception e) {
				// ignore
			}
		}

		if (tapeLength < 0)
			throw new IllegalArgumentException("tape length cannot be negative");

		setDefaultTapeLength(tapeLength);

		resetElements();

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

		for (byte i = 0; i < 4; i++) {

			kep[i] = new FloatQueue("Kinetic Energy/Particle " + idToName(i), movie.getCapacity());
			kep[i].setReferenceUpperBound(5);
			kep[i].setReferenceLowerBound(-5);
			kep[i].setCoordinateQueue(modelTimeQueue);
			kep[i].setInterval(movieUpdater.getInterval());
			kep[i].setPointer(0);
			movieQueueGroup.add(kep[i]);

			msd[i] = new FloatQueue("Mean Square Displacement/Particle " + idToName(i), movie.getCapacity());
			msd[i].setReferenceUpperBound(100);
			msd[i].setReferenceLowerBound(0);
			msd[i].setCoordinateQueue(modelTimeQueue);
			msd[i].setInterval(movieUpdater.getInterval());
			msd[i].setPointer(0);
			movieQueueGroup.add(msd[i]);

		}

		Action a = null;
		ChangeListener c = null;
		for (byte i = 0; i < 4; i++) {

			for (byte j = i; j < 4; j++) {
				a = createPCFAction(i, j);
				actionMap.put((String) a.getValue(Action.SHORT_DESCRIPTION), a);
			}
			a = new MvdAction(this, false, i);
			actionMap.put((String) a.getValue(Action.SHORT_DESCRIPTION), a);
			a = new MvdAction(this, true, i);
			actionMap.put((String) a.getValue(Action.SHORT_DESCRIPTION), a);
			a = createHeatAction(i, 0.01, "Heat " + idToName(i) + " Atoms");
			actionMap.put((String) a.getValue(Action.SHORT_DESCRIPTION), a);
			a = createHeatAction(i, -0.01, "Cool " + idToName(i) + " Atoms");
			actionMap.put((String) a.getValue(Action.SHORT_DESCRIPTION), a);
			a = new InsertAction(this, i);
			actionMap.put((String) a.getValue(Action.SHORT_DESCRIPTION), a);

			c = new EpsilonChanger(this, i);
			changeMap.put(c.toString(), c);
			c = new MassChanger(this, i);
			changeMap.put(c.toString(), c);

		}

		a = new AbstractAction() {
			public Object getValue(String key) {
				if (key.equalsIgnoreCase("state"))
					return rCutOff > 1.5 ? Boolean.TRUE : Boolean.FALSE;
				return super.getValue(key);
			}

			public void actionPerformed(ActionEvent e) {
				if (ModelerUtilities.stopFiring(e))
					return;
				setCutOffMatrix(rCutOff < 1.5 ? 2.0f : 1.0f);
			}

			public String toString() {
				return (String) getValue(SHORT_DESCRIPTION);
			}
		};
		a.putValue(Action.NAME, "Van der Waals Attractions");
		a.putValue(Action.SHORT_DESCRIPTION, "I want van der Waals attractions");
		switchMap.put((String) a.getValue(Action.SHORT_DESCRIPTION), a);

		a = new AbstractAction() {
			public Object getValue(String key) {
				if (key.equalsIgnoreCase("state"))
					return lightSource.isOn() ? Boolean.TRUE : Boolean.FALSE;
				return super.getValue(key);
			}

			public void actionPerformed(ActionEvent e) {
				if (ModelerUtilities.stopFiring(e))
					return;
				lightSource.setOn(!lightSource.isOn());
				setLightSourceEnabled(lightSource.isOn());
			}

			public String toString() {
				return (String) getValue(SHORT_DESCRIPTION);
			}
		};
		a.putValue(Action.NAME, "Light Source");
		a.putValue(Action.SHORT_DESCRIPTION, "Turn on light source");
		switchMap.put((String) a.getValue(Action.SHORT_DESCRIPTION), a);

		a = new AbstractAction() {
			public Object getValue(String key) {
				if (key.equalsIgnoreCase("state"))
					return lightSource.isMonochromatic() ? Boolean.TRUE : Boolean.FALSE;
				return super.getValue(key);
			}

			public void actionPerformed(ActionEvent e) {
				if (ModelerUtilities.stopFiring(e))
					return;
				lightSource.setMonochromatic(!lightSource.isMonochromatic());
			}

			public String toString() {
				return (String) getValue(SHORT_DESCRIPTION);
			}
		};
		a.putValue(Action.NAME, "Light Source Monochromaticity");
		a.putValue(Action.SHORT_DESCRIPTION, "Monochromatic light source");
		switchMap.put((String) a.getValue(Action.SHORT_DESCRIPTION), a);

		c = new LightSourceFrequencyChanger(this);
		changeMap.put(c.toString(), c);
		c = new LightSourceIntensityChanger(this);
		changeMap.put(c.toString(), c);
		c = new LightSimulationSpeedChanger(this);
		changeMap.put(c.toString(), c);
		c = new FluxChanger(this);
		changeMap.put(c.toString(), c);

		lightSource = new LightSource();
		quantumRule = new QuantumRule();
		photonGun.setInterval(lightSource.getRadiationPeriod());
		photonGun.setPriority(6);

		atomSource = new AtomSource(this);

	}

	public static short getMaximumNumberOfAtoms() {
		return NMAX;
	}

	public void setView(MDView v) {
		if (!(v instanceof AtomisticView))
			throw new IllegalArgumentException("must be AtomisticView");
		super.setView(v);
		view = (AtomisticView) v;
		EngineAction a = new EngineAction(this);
		view.getActionMap().put(a.toString(), a);
	}

	public JComponent getView() {
		return view;
	}

	public void markSelection() {
		super.markSelection();
		if (view.getUseJmol())
			view.refreshJmol();
		view.repaint();
	}

	/** return true if this model contains no mobile object at all. */
	public synchronized boolean isEmpty() {
		if (photonList != null && !photonList.isEmpty())
			return false;
		return super.isEmpty();
	}

	/**
	 * align atoms in a two-dimensional lattice. You may choose any number for the inputs, but only those that fall
	 * within the current boundary will be added.
	 */
	public void alignParticles(int id, int m, int n, double xoffset, double yoffset, double xspacing, double yspacing,
			boolean randomize) {
		Element e = getElement(id);
		if (e == null)
			throw new IllegalArgumentException("atom id incorrect");
		double sigma = e.getSigma();
		if (xspacing < sigma || yspacing < sigma)
			throw new IllegalArgumentException("spacings may be too small");
		if (xoffset < 0 || yoffset < 0)
			throw new IllegalArgumentException("negative offset");
		if (m <= 0 || n <= 0)
			throw new IllegalArgumentException("m and n must be positive");
		int k = 0;
		double w = boundary.getWidth();
		double h = boundary.getHeight();
		double x = boundary.getX();
		double y = boundary.getY();
		double rx, ry;
		terminate: for (int i = 0; i < m; i++) {
			for (int j = 0; j < n; j++) {
				rx = xoffset + i * xspacing;
				ry = yoffset + j * yspacing;
				if (rx - sigma * 0.5 >= x && rx + sigma * 0.5 < x + w && ry - sigma * 0.5 >= y
						&& ry + sigma * 0.5 < y + h) {
					atom[k].rx = rx;
					atom[k].ry = ry;
					if (randomize) {
						atom[k].rx += (0.5 - Math.random()) * xspacing;
						atom[k].ry += (0.5 - Math.random()) * yspacing;
					}
					atom[k].setElement(e);
					atom[k].setCharge(0);
					atom[k].setRestraint(null);
					atom[k].setFriction(0);
					atom[k].setRadical(true);
					atom[k].setUserField(null);
					k++;
					if (k >= atom.length)
						break terminate;
				}
			}
		}
		setNumberOfParticles(k);
		if (randomize) {
			putInBounds();
			Thread t = new Thread() {
				public void run() {
					for (int i = 0; i < 100; i++) {
						steepestDescent(1.0);
					}
				}
			};
			t.setPriority(Thread.NORM_PRIORITY);
			t.start();
		}
	}

	void initializeJob() {
		super.initializeJob();
		if (photonEnabled) {
			if (!job.contains(electronicDynamics))
				job.add(electronicDynamics);
		}
		setLightSourceEnabled(lightSource.isOn());
		if (atomFlowEnabled) {
			if (!job.contains(atomSource))
				job.add(atomSource);
		}
	}

	public void setLJBetweenBondPairs(boolean b) {
		ljBetweenBondPairs = b;
	}

	public boolean getLJBetweenBondPairs() {
		return ljBetweenBondPairs;
	}

	public void setAtomFlowEnabled(boolean b) {
		atomFlowEnabled = b;
		boundary.getWall().reset();
		if (job != null) {
			if (atomFlowEnabled) {
				if (!job.contains(atomSource))
					job.add(atomSource);
			}
			else {
				job.remove(atomSource);
			}
		}
	}

	public boolean isAtomFlowEnabled() {
		return atomFlowEnabled;
	}

	public void setFlowInSide(byte side) {
		if (atomSource == null)
			return;
		atomSource.setWall(side);
	}

	public byte getFlowInSide() {
		if (atomSource == null)
			return Wall.WEST;
		return atomSource.getWall();
	}

	public void setFlowOutSide(byte side) {
		boundary.getWall().setSink(side, true);
	}

	public byte getFlowOutSide() {
		return boundary.getWall().getSinkSide();
	}

	public void setFlowInType(byte[] b) {
		atomSource.setType(b);
	}

	public byte[] getFlowInType() {
		return atomSource.getType();
	}

	public void setFlowOutType(byte[] b) {
		boundary.getWall().setFlowOutType(b);
	}

	public byte[] getFlowOutType() {
		return boundary.getWall().getFlowOutType();
	}

	public void setFlowInterval(int interval) {
		if (atomSource == null)
			return;
		atomSource.setInterval(interval);
	}

	public int getFlowInterval() {
		if (atomSource == null)
			return 500;
		return atomSource.getInterval();
	}

	public QuantumRule getQuantumRule() {
		return quantumRule;
	}

	public void setCollisionalDeexcitation(boolean b) {
		collisionalDeexcitation = b;
	}

	public boolean getCollisionDeexcitation() {
		return collisionalDeexcitation;
	}

	public void setPhotonEnabled(boolean b) {
		photonEnabled = b;
		if (job != null) {
			if (photonEnabled) {
				if (!job.contains(electronicDynamics))
					job.add(electronicDynamics);
			}
			else {
				job.remove(electronicDynamics);
			}
		}
	}

	public boolean isPhotonEnabled() {
		return photonEnabled;
	}

	public void setLightSource(LightSource lightSource) {
		this.lightSource = lightSource;
	}

	public LightSource getLightSource() {
		return lightSource;
	}

	public void setLightSourceEnabled(boolean b) {
		if (job == null) {
			lightSource.setOn(false);
			return;
		}
		if (b) {
			if (!job.contains(photonGun) && !job.toBeAdded(photonGun))
				job.add(photonGun);
			setPhotonEnabled(true);
		}
		else {
			job.remove(photonGun);
		}
		lightSource.setOn(b);
	}

	public boolean isLightSourceEnabled() {
		if (job == null)
			return false;
		return job.contains(photonGun);
	}

	boolean rotateSelectedParticles(double angleInDegrees) {
		double xc = 0, yc = 0;
		int n = 0;
		for (int i = 0; i < numberOfAtoms; i++) {
			if (atom[i].isSelected()) {
				xc += atom[i].rx;
				yc += atom[i].ry;
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
		for (int i = 0; i < numberOfAtoms; i++) {
			if (!atom[i].isSelected())
				continue;
			atom[i].storeCurrentState();
			xi = atom[i].rx;
			yi = atom[i].ry;
			atom[i].rx = xc + (xi - xc) * costheta - (yi - yc) * sintheta;
			atom[i].ry = yc + (xi - xc) * sintheta + (yi - yc) * costheta;
			if (!boundary.contains(atom[i].rx, atom[i].ry)) {
				b = false;
				n = i;
				break;
			}
		}
		if (b) {
			for (int i = 0; i < numberOfAtoms; i++) {
				if (!atom[i].isSelected())
					continue;
				PointRestraint pr = atom[i].getRestraint();
				if (pr != null) {
					xi = pr.getX0();
					yi = pr.getY0();
					pr.setX0(xc + (xi - xc) * costheta - (yi - yc) * sintheta);
					pr.setY0(yc + (xi - xc) * sintheta + (yi - yc) * costheta);
				}
			}
			if (view.getUseJmol())
				view.refreshJmol();
			view.repaint();
			return true;
		}
		for (int i = 0; i <= n; i++) {
			if (atom[i].isSelected())
				atom[i].restoreState();
		}
		return false;
	}

	/** translate all components of the model by the specified distance */
	public boolean translateWholeModel(double dx, double dy) {
		boolean b = true;
		int n = 0;
		for (int i = 0; i < numberOfAtoms; i++) {
			atom[i].storeCurrentState();
			atom[i].translateBy(dx, dy);
			if (!boundary.contains(atom[i].rx, atom[i].ry)) {
				b = false;
				n = i;
				break;
			}
		}
		if (b) {
			for (int i = 0; i < numberOfAtoms; i++) {
				PointRestraint pr = atom[i].getRestraint();
				if (pr != null) {
					pr.setX0(pr.getX0() + dx);
					pr.setY0(pr.getY0() + dy);
				}
			}
		}
		else {
			for (int i = 0; i <= n; i++)
				atom[i].translateBy(-dx, -dy);
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
			for (int i = 0; i < numberOfAtoms; i++)
				atom[i].translateBy(-dx, -dy);
			for (int i = 0; i <= n; i++)
				obstacles.get(i).translateBy(-dx, -dy);
			return false;
		}
		if (view.getUseJmol())
			view.refreshJmol();
		view.repaint();
		return true;
	}

	public List<Photon> getPhotons() {
		return photonList;
	}

	public void addPhoton(Photon p) {
		if (p == null)
			return;
		if (photonList == null)
			photonList = Collections.synchronizedList(new ArrayList<Photon>());
		p.setModel(this);
		photonList.add(p);
	}

	public void removePhoton(Photon p) {
		if (p == null)
			return;
		if (photonList == null)
			return;
		p.setModel(null); // allow garbage collector to get rid of it
		photonList.remove(p);
	}

	public void setLightSourceInterval(int i) {
		photonGun.setInterval(i);
		lightSource.setRadiationPeriod(i);
	}

	public void setLightSimulationSpeed(float step) {
		if (Math.abs(getTimeStep() - step) < ZERO)
			return;
		float x = (float) getTimeStep() / step;
		setTimeStep(step);
		// NOTE: whether or not the light source is turned on, the interval should be changed in order to avoid
		// the bug that the interval changes abnormally.
		if (lightSource != null)
			setLightSourceInterval((int) (photonGun.getInterval() * x));
	}

	public float[] getBoundsOfObjects() {
		if (numberOfAtoms <= 0 && (obstacles == null || obstacles.isEmpty()))
			return null;
		if (numberOfAtoms > 0) {
			range_xmin = (float) atom[0].getMinX();
			range_ymin = (float) atom[0].getMinY();
			range_xmax = (float) atom[0].getMaxX();
			range_ymax = (float) atom[0].getMaxY();
			id_xmax = id_xmin = id_ymax = id_ymin = 0;
		}
		switch (boundary.getType()) {
		case RectangularBoundary.DBC_ID:
		case RectangularBoundary.RBC_ID:
			for (short i = 1; i < numberOfAtoms; i++) {
				Atom a = atom[i];
				if (a.getMaxX() > range_xmax) {
					range_xmax = (float) (a.getMaxX());
					id_xmax = i;
				}
				else if (a.getMinX() < range_xmin) {
					range_xmin = (float) (a.getMinX());
					id_xmin = i;
				}
				if (a.getMaxY() > range_ymax) {
					range_ymax = (float) (a.getMaxY());
					id_ymax = i;
				}
				else if (a.getMinY() < range_ymin) {
					range_ymin = (float) (a.getMinY());
					id_ymin = i;
				}
			}
			break;
		case RectangularBoundary.PBC_ID:
			for (short i = 1; i < numberOfAtoms; i++) {
				Atom a = atom[i];
				if (a.getMaxX() > range_xmax) {
					range_xmax = (float) (a.getRx());
					id_xmax = i;
				}
				else if (a.getMinX() < range_xmin) {
					range_xmin = (float) (a.getRx());
					id_xmin = i;
				}
				if (a.getMaxY() > range_ymax) {
					range_ymax = (float) (a.getRy());
					id_ymax = i;
				}
				else if (a.getMinY() < range_ymin) {
					range_ymin = (float) (a.getRy());
					id_ymin = i;
				}
			}
			break;
		case RectangularBoundary.XPYRBC_ID:
			for (short i = 1; i < numberOfAtoms; i++) {
				Atom a = atom[i];
				if (a.getMaxX() > range_xmax) {
					range_xmax = (float) (a.getRx());
					id_xmax = i;
				}
				else if (a.getMinX() < range_xmin) {
					range_xmin = (float) (a.getRx());
					id_xmin = i;
				}
				if (a.getMaxY() > range_ymax) {
					range_ymax = (float) (a.getMaxY());
					id_ymax = i;
				}
				else if (a.getMinY() < range_ymin) {
					range_ymin = (float) (a.getMinY());
					id_ymin = i;
				}
			}
			break;
		case RectangularBoundary.XRYPBC_ID:
			for (short i = 1; i < numberOfAtoms; i++) {
				Atom a = atom[i];
				if (a.getMaxX() > range_xmax) {
					range_xmax = (float) (a.getMaxX());
					id_xmax = i;
				}
				else if (a.getMinX() < range_xmin) {
					range_xmin = (float) (a.getMinX());
					id_xmin = i;
				}
				if (a.getMaxY() > range_ymax) {
					range_ymax = (float) (a.getRy());
					id_ymax = i;
				}
				else if (a.getMinY() < range_ymin) {
					range_ymin = (float) (a.getRy());
					id_ymin = i;
				}
			}
			break;
		}
		if (obstacles != null && !obstacles.isEmpty()) {
			if (numberOfAtoms <= 0)
				range_xmax = range_ymax = 1;
			Obstacle o;
			synchronized (obstacles.getSynchronizationLock()) {
				for (Iterator it = obstacles.iterator(); it.hasNext();) {
					o = (Obstacle) it.next();
					if (o.getMaxX() > range_xmax)
						range_xmax = (float) o.getMaxX();
					if (o.getMaxY() > range_ymax)
						range_ymax = (float) o.getMaxY();
					if (o.getMinX() < range_xmin)
						range_xmin = (float) o.getMinX();
					if (o.getMinY() < range_ymin)
						range_ymin = (float) o.getMinY();
				}
			}
		}
		return new float[] { range_xmin, range_ymin, range_xmax, range_ymax };
	}

	public void clearTimeSeries() {
		super.clearTimeSeries();
		for (int i = 0; i < msd.length; i++) {
			movieQueueGroup.add(kep[i]);
			movieQueueGroup.add(msd[i]);
		}
	}

	void record() {
		super.record();
		updateAllRQ();
		updateAllVQ();
		updateAllAQ();
		updateAllDQ();
		updatePressureQ();
		for (byte i = 0; i < 4; i++)
			kep[i].update((float) getKinForType(i));
		if (computeList != null) {
			if (computeList.contains(COMPUTE_MSD)) {
				updateMSD();
			}
			else {
				// move the pointers of uncalculated arrays to avoid errors
				int p = getTapePointer();
				for (int i = 0; i < msd.length; i++)
					msd[i].setPointer(p);
			}
		}
		if (photonEnabled || (lightSource != null && lightSource.isOn()))
			updatePhotonQ();
	}

	private synchronized void updatePhotonQ() {
		if (photonEnabled) {
			int c = movie.getCapacity();
			for (int i = 0; i < numberOfAtoms; i++) {
				if (atom[i].getElectrons().isEmpty())
					continue;
				try {
					atom[i].updateExcitationQ();
				}
				catch (Exception e) {
					atom[i].initializeExcitationQ(c);
					atom[i].updateExcitationQ();
				}
			}
		}
		if (photonQueue == null)
			photonQueue = new ObjectQueue(movie.getCapacity());
		if (photonList != null && !photonList.isEmpty()) {
			ArrayList<Photon.Delegate> list = new ArrayList<Photon.Delegate>();
			for (Photon p : photonList) {
				list.add(new Photon.Delegate(p));
			}
			photonQueue.update(list);
		}
		else {
			photonQueue.update(null);
		}
	}

	private void updatePressureQ() {
		if (obstacles == null || obstacles.isEmpty())
			return;
		int c = movie.getCapacity();
		RectangularObstacle obs = null;
		synchronized (obstacles.getSynchronizationLock()) {
			for (int i = 0, n = obstacles.size(); i < n; i++) {
				obs = obstacles.get(i);
				try {
					obs.updatePQ();
				}
				catch (Exception e) {
					obs.initializePEQ(c);
					obs.initializePWQ(c);
					obs.initializePSQ(c);
					obs.initializePNQ(c);
					obs.updatePQ();
				}
			}
		}
	}

	private synchronized void updateMSD() {
		int p = getTapePointer();
		if (p < 0)
			return;
		int[] n = new int[4];
		Arrays.fill(n, 0);
		for (int i = 0; i < numberOfAtoms; i++) {
			if (atom[i].id >= 0 && atom[i].id < 4)
				n[atom[i].id]++;
		}
		if (n[0] == 0 && n[1] == 0 && n[2] == 0 && n[3] == 0)
			return;
		float[] c = new float[4];
		float sx, sy;
		for (int k = 0; k < p; k++) {
			Arrays.fill(c, 0f);
			for (int i = 0; i < numberOfAtoms; i++) {
				if (atom[i].id >= 0 && atom[i].id < 4) {
					sx = atom[i].dxdyQ.getQueue1().sum(0, k);
					sy = atom[i].dxdyQ.getQueue2().sum(0, k);
					c[atom[i].id] += sx * sx + sy * sy;
				}
			}
			for (int i = 0; i < 4; i++)
				if (n[i] != 0)
					msd[i].setData(k, c[i] / n[i]);
		}
		for (int i = 0; i < 4; i++)
			msd[i].setPointer(p);
	}

	public Atom createAtomOfElement(int id) {
		switch (id) {
		case ID_NT:
			return new Atom(nt);
		case ID_PL:
			return new Atom(pl);
		case ID_WS:
			return new Atom(ws);
		case ID_CK:
			return new Atom(ck);
		case ID_MO:
			return new Atom(mo);
		case ID_SP:
			return new Atom(sp);
		}
		if (id >= ID_ALA && id <= ID_VAL)
			return new Atom(aminoAcidElement[id - ID_ALA]);
		if (id >= ID_A && id <= ID_U)
			return new Atom(nucleotideElement[id - ID_A]);
		return null;
	}

	public Element getElement(int id) {
		switch (id) {
		case ID_NT:
			return nt;
		case ID_PL:
			return pl;
		case ID_WS:
			return ws;
		case ID_CK:
			return ck;
		case ID_MO:
			return mo;
		case ID_SP:
			return sp;
		}
		if (id >= ID_ALA && id <= ID_VAL)
			return aminoAcidElement[id - ID_ALA];
		if (id >= ID_A && id <= ID_U)
			return nucleotideElement[id - ID_A];
		return null;
	}

	public Element getElement(String s) {
		if (s == null)
			return null;
		if (s.equalsIgnoreCase("nt"))
			return nt;
		if (s.equalsIgnoreCase("pl"))
			return pl;
		if (s.equalsIgnoreCase("ws"))
			return ws;
		if (s.equalsIgnoreCase("ck"))
			return ck;
		if (s.equalsIgnoreCase("mo"))
			return mo;
		if (s.equalsIgnoreCase("sp"))
			return sp;
		if (s.equalsIgnoreCase("ala"))
			return aminoAcidElement[ID_ALA - ID_ALA];
		if (s.equalsIgnoreCase("arg"))
			return aminoAcidElement[ID_ARG - ID_ALA];
		if (s.equalsIgnoreCase("asn"))
			return aminoAcidElement[ID_ASN - ID_ALA];
		if (s.equalsIgnoreCase("asp"))
			return aminoAcidElement[ID_ASP - ID_ALA];
		if (s.equalsIgnoreCase("cys"))
			return aminoAcidElement[ID_CYS - ID_ALA];
		if (s.equalsIgnoreCase("gln"))
			return aminoAcidElement[ID_GLN - ID_ALA];
		if (s.equalsIgnoreCase("glu"))
			return aminoAcidElement[ID_GLU - ID_ALA];
		if (s.equalsIgnoreCase("gly"))
			return aminoAcidElement[ID_GLY - ID_ALA];
		if (s.equalsIgnoreCase("his"))
			return aminoAcidElement[ID_HIS - ID_ALA];
		if (s.equalsIgnoreCase("ile"))
			return aminoAcidElement[ID_ILE - ID_ALA];
		if (s.equalsIgnoreCase("leu"))
			return aminoAcidElement[ID_LEU - ID_ALA];
		if (s.equalsIgnoreCase("lys"))
			return aminoAcidElement[ID_LYS - ID_ALA];
		if (s.equalsIgnoreCase("met"))
			return aminoAcidElement[ID_MET - ID_ALA];
		if (s.equalsIgnoreCase("phe"))
			return aminoAcidElement[ID_PHE - ID_ALA];
		if (s.equalsIgnoreCase("pro"))
			return aminoAcidElement[ID_PRO - ID_ALA];
		if (s.equalsIgnoreCase("ser"))
			return aminoAcidElement[ID_SER - ID_ALA];
		if (s.equalsIgnoreCase("thr"))
			return aminoAcidElement[ID_THR - ID_ALA];
		if (s.equalsIgnoreCase("trp"))
			return aminoAcidElement[ID_TRP - ID_ALA];
		if (s.equalsIgnoreCase("tyr"))
			return aminoAcidElement[ID_TYR - ID_ALA];
		if (s.equalsIgnoreCase("val"))
			return aminoAcidElement[ID_VAL - ID_ALA];
		if (s.equalsIgnoreCase("a"))
			return nucleotideElement[ID_A - ID_A];
		if (s.equalsIgnoreCase("c"))
			return nucleotideElement[ID_C - ID_A];
		if (s.equalsIgnoreCase("g"))
			return nucleotideElement[ID_G - ID_A];
		if (s.equalsIgnoreCase("t"))
			return nucleotideElement[ID_T - ID_A];
		if (s.equalsIgnoreCase("u"))
			return nucleotideElement[ID_U - ID_A];
		return null;
	}

	public Affinity getAffinity() {
		return affinity;
	}

	/**
	 * set the strength of a single hydrogen bond between base pairs in the DNA model. The A-T/A-U pair has two hydrogen
	 * bonds, whereas the C-G pair has three hydrogen bonds.
	 */
	public void setHydrogenBondStrength(float s) {
		hbStrength = s;
	}

	public float getHydrogenBondStrength() {
		return hbStrength;
	}

	public void run() {
		checkCharges();
		view.showContourPlot(false, null);
		double[] data = new double[numberOfAtoms * 4];
		for (int i = 0; i < numberOfAtoms; i++) {
			data[i] = atom[i].rx;
			data[i + numberOfAtoms] = atom[i].ry;
			data[i + numberOfAtoms * 2] = atom[i].vx;
			data[i + numberOfAtoms * 3] = atom[i].vy;
		}
		if (obstacles.isEmpty()) {
			stateHolder = new StateHolder(getModelTime(), heatBathActivated() ? heatBath.getExpectedTemperature() : 0,
					getNumberOfParticles(), data);
		}
		else {
			int nmov = 0;
			for (int i = 0; i < obstacles.size(); i++) {
				if (obstacles.get(i).isMovable())
					nmov++;
			}
			if (nmov == 0) {
				stateHolder = new StateHolder(getModelTime(), heatBathActivated() ? heatBath.getExpectedTemperature()
						: 0, getNumberOfParticles(), data);
			}
			else {
				double[] data2 = new double[nmov * 4];
				int k = 0;
				RectangularObstacle obs = null;
				for (int i = 0; i < obstacles.size(); i++) {
					obs = obstacles.get(i);
					if (obs.isMovable()) {
						data2[k] = obs.x;
						data2[k + nmov] = obs.y;
						data2[k + nmov * 2] = obs.vx;
						data2[k + nmov * 3] = obs.vy;
						k++;
					}
				}
				stateHolder = new StateHolder(getModelTime(), heatBathActivated() ? heatBath.getExpectedTemperature()
						: 0, getNumberOfParticles(), data, nmov, data2);
			}
		}
		super.run();
	}

	public boolean revert() {
		if (stateHolder == null)
			return false;
		setModelTime(stateHolder.getTime());
		setNumberOfParticles(stateHolder.getNumberOfParticles());
		double[] data = stateHolder.getParticleData();
		for (int i = 0; i < numberOfAtoms; i++) {
			atom[i].translateTo(data[i], data[i + numberOfAtoms]);
			atom[i].vx = data[i + numberOfAtoms * 2];
			atom[i].vy = data[i + numberOfAtoms * 3];
			atom[i].moveRPointer(0);
			atom[i].moveVPointer(0);
			atom[i].moveAPointer(0);
			atom[i].moveDPointer(0);
		}
		kine.setPointer(0);
		pote.setPointer(0);
		tote.setPointer(0);
		for (FloatQueue q : channelTs)
			q.setPointer(0);
		for (int i = 0; i < 4; i++) {
			kep[i].setPointer(0);
			msd[i].setPointer(0);
		}
		modelTimeQueue.setPointer(0);
		int nmov = stateHolder.getNumberOfObstacles();
		if (nmov > 0) {
			double[] data2 = stateHolder.getObstacleData();
			if (data2 != null) {
				int k = 0;
				RectangularObstacle obs = null;
				for (int i = 0; i < obstacles.size(); i++) {
					obs = obstacles.get(i);
					if (obs.isMovable()) {
						obs.translateTo(data2[k], data2[k + nmov]);
						obs.vx = data2[k + nmov * 2];
						obs.vy = data2[k + nmov * 3];
						obs.moveRPointer(0);
						obs.moveVPointer(0);
						obs.moveAPointer(0);
						obs.movePPointer(0);
						k++;
					}
				}
			}
		}
		if (heatBathActivated()) {
			heatBath.setExpectedTemperature(stateHolder.getHeatBathTemperature());
		}
		if (view.getUseJmol())
			view.refreshJmol();
		view.repaint();
		notifyModelListeners(new ModelEvent(this, ModelEvent.MODEL_RESET));
		return true;
	}

	/**
	 * <p>
	 * update the Lennard-Jones force parameter array, this is to believe that looking up this table when calculating
	 * interatomic forces is faster than recomputing the Lennard-Jones parameters at each time step. Whenever the model
	 * has a change related to the parameter array, e.g. an atom inserted, removed, muated, or van der Waals parameters
	 * and affinities are changed, this parameter array must be refreshed. The update is done by calling this method and
	 * passing <tt>true</tt> to it. The force calculation routine will check this flag first. If it is true, then
	 * immediately update the parameter array and set <tt>false</tt> to it before computing any forces.
	 * </p>
	 * <p>
	 * The disadvantage is that it needs larger memory.
	 * </p>
	 */
	public void setUpdateParArray(boolean b) {
		updateParArray = b;
	}

	void setTapePointer(int n) {
		if (!hasEmbeddedMovie())
			throw new RuntimeException("cannot set pointer because there is no tape");
		modelTimeQueue.setPointer(n);
		for (int i = 0; i < numberOfAtoms; i++) {
			atom[i].moveRPointer(n);
			atom[i].moveVPointer(n);
			atom[i].moveAPointer(n);
			atom[i].moveDPointer(n);
		}
		if (obstacles != null && !obstacles.isEmpty()) {
			RectangularObstacle obs = null;
			synchronized (obstacles.getSynchronizationLock()) {
				for (Iterator it = obstacles.iterator(); it.hasNext();) {
					obs = (RectangularObstacle) it.next();
					obs.moveRPointer(n);
					obs.moveVPointer(n);
					obs.moveAPointer(n);
					obs.movePPointer(n);
				}
			}
		}
		if (boundary.getQueue() != null)
			boundary.getQueue().setPointer(n);
		kine.setPointer(n);
		pote.setPointer(n);
		tote.setPointer(n);
		for (FloatQueue q : channelTs)
			q.setPointer(n);
		for (int i = 0; i < 4; i++) {
			kep[i].setPointer(n);
			msd[i].setPointer(n);
		}
	}

	private void setQueueLength(int n) {
		modelTimeQueue.setLength(n);
		kine.setLength(n);
		pote.setLength(n);
		tote.setLength(n);
		for (FloatQueue q : channelTs)
			q.setLength(n);
		for (int i = 0; i < 4; i++) {
			kep[i].setLength(n);
			msd[i].setLength(n);
		}
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
			for (int i = 0; i < 4; i++) {
				kep[i].setInterval(m);
				msd[i].setInterval(m);
			}
			int n = movie.getCapacity();
			setQueueLength(n);
			for (int i = 0; i < numberOfAtoms; i++) {
				atom[i].initializeMovieQ(n);
				if (!atom[i].getElectrons().isEmpty())
					atom[i].initializeExcitationQ(n);
			}
			if (obstacles != null && !obstacles.isEmpty()) {
				synchronized (obstacles.getSynchronizationLock()) {
					for (Iterator it = obstacles.iterator(); it.hasNext();)
						((RectangularObstacle) it.next()).initializeMovieQ(n);
				}
			}
		}
		else {
			if (job != null && job.contains(movieUpdater))
				job.remove(movieUpdater);
			setQueueLength(-1);
			for (int i = 0; i < numberOfAtoms; i++) {
				atom[i].initializeMovieQ(-1);
				atom[i].initializeExcitationQ(-1);
			}
			if (obstacles != null && !obstacles.isEmpty()) {
				synchronized (obstacles.getSynchronizationLock()) {
					for (Iterator it = obstacles.iterator(); it.hasNext();)
						((RectangularObstacle) it.next()).initializeMovieQ(-1);
				}
			}
		}
		movie.setCurrentFrameIndex(0);
		setRecorderDisabled(!b);
	}

	public boolean hasEmbeddedMovie() {
		if (isEmpty() || getTapePointer() <= 0)
			return false;
		if (numberOfAtoms > 0) {
			if (atom[0].rxryQ == null || atom[0].rxryQ.isEmpty())
				return false;
		}
		if (obstacles != null && !obstacles.isEmpty()) {
			RectangularObstacle o = obstacles.get(0);
			if (o.rxryQ == null || o.rxryQ.isEmpty())
				return false;
		}
		return true;
	}

	private void updateAllRQ() {
		int c = movie.getCapacity();
		for (int i = 0; i < numberOfAtoms; i++) {
			try {
				atom[i].updateRQ();
			}
			catch (Exception e) {
				atom[i].initializeRQ(c);
				atom[i].updateRQ();
			}
		}
		if (obstacles != null && !obstacles.isEmpty()) {
			RectangularObstacle obs = null;
			synchronized (obstacles.getSynchronizationLock()) {
				for (int i = 0, n = obstacles.size(); i < n; i++) {
					obs = obstacles.get(i);
					if (obs.isMovable()) {
						try {
							obs.updateRQ();
						}
						catch (Exception e) {
							obs.initializeRQ(c);
							obs.updateRQ();
						}
					}
				}
			}
		}
	}

	private void updateAllVQ() {
		int c = movie.getCapacity();
		for (int i = 0; i < numberOfAtoms; i++) {
			try {
				atom[i].updateVQ();
			}
			catch (Exception e) {
				atom[i].initializeVQ(c);
				atom[i].updateVQ();
			}
		}
		if (obstacles != null && !obstacles.isEmpty()) {
			RectangularObstacle obs = null;
			synchronized (obstacles.getSynchronizationLock()) {
				for (int i = 0, n = obstacles.size(); i < n; i++) {
					obs = obstacles.get(i);
					if (obs.isMovable()) {
						try {
							obs.updateVQ();
						}
						catch (Exception e) {
							obs.initializeVQ(c);
							obs.updateVQ();
						}
					}
				}
			}
		}
	}

	private void updateAllAQ() {
		int c = movie.getCapacity();
		for (int i = 0; i < numberOfAtoms; i++) {
			try {
				atom[i].updateAQ();
			}
			catch (Exception e) {
				atom[i].initializeAQ(c);
				atom[i].updateAQ();
			}
		}
		if (obstacles != null && !obstacles.isEmpty()) {
			RectangularObstacle obs = null;
			synchronized (obstacles.getSynchronizationLock()) {
				for (int i = 0, n = obstacles.size(); i < n; i++) {
					obs = obstacles.get(i);
					if (obs.isMovable()) {
						try {
							obs.updateAQ();
						}
						catch (Exception e) {
							obs.initializeAQ(c);
							obs.updateAQ();
						}
					}
				}
			}
		}
	}

	private void updateAllDQ() {
		int c = movie.getCapacity();
		for (int i = 0; i < numberOfAtoms; i++) {
			try {
				atom[i].updateDQ();
			}
			catch (Exception e) {
				atom[i].initializeDQ(c);
				atom[i].updateDQ();
			}
		}
	}

	private Action createPCFAction(final byte e1, final byte e2) {
		Action a = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				if (ModelerUtilities.stopFiring(e))
					return;
				int rmax = (int) (Math.max(getElement(e1).getSigma(), getElement(e2).getSigma()) * 5);
				showPCF(new Pcf.Parameter[] { new Pcf.Parameter(e1, e2, rmax, boundary) });
			}

			public String toString() {
				return (String) getValue(SHORT_DESCRIPTION);
			}
		};
		a.putValue(Action.NAME, "Show Pair Correlation Function: " + idToName(e1) + "-" + idToName(e2));
		a.putValue(Action.SHORT_DESCRIPTION, "Show pair correlation function: " + idToName(e1) + "-" + idToName(e2));
		return a;
	}

	private Action createHeatAction(final byte element, double amount, String name) {
		Action a = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				if (ModelerUtilities.stopFiring(e))
					return;
				Object o = getValue("increment");
				if (o instanceof Double) {
					transferHeatToType(element, ((Double) o).doubleValue());
					view.repaint();
				}
			}

			public String toString() {
				return (String) getValue(SHORT_DESCRIPTION);
			}
		};
		a.putValue(Action.MNEMONIC_KEY, new Integer(amount >= 0 ? KeyEvent.VK_H : KeyEvent.VK_C));
		a.putValue(Action.SMALL_ICON, IconPool.getIcon(amount >= 0 ? "heat" : "cool"));
		a.putValue(Action.NAME, name);
		a.putValue(Action.SHORT_DESCRIPTION, name);
		a.putValue("increment", new Double(amount));
		return a;
	}

	/**
	 * setup grid simulation. if nx or ny is less than 1, cancel grid. Otherwise, set up a grid of size <tt>nx*ny</tt>.
	 */
	public void setupGrid(int nx, int ny) {
		if (nx > 0 && ny > 0) {
			if (boundary.getType() != RectangularBoundary.DBC_ID) {
				grid = new Grid(boundary, new int[] { nx, ny });
			}
			else {
				grid = new Grid((Rectangle2D.Double) boundary.getView(), new int[] { nx, ny });
			}
			view.setGrid(grid);
			if (job == null)
				initializeJob();
			if (!job.contains(updateGrid))
				job.add(updateGrid);
		}
		else {
			grid = null;
			view.setGrid(null);
			view.setGridMode((byte) -1);
			if (job != null)
				job.remove(updateGrid);
		}
	}

	public void setupGrid() {
		if (grid != null)
			return;
		if (boundary.getType() != RectangularBoundary.DBC_ID) {
			grid = new Grid(boundary);
		}
		else {
			grid = new Grid((Rectangle2D.Double) boundary.getView());
		}
		view.setGrid(grid);
		if (job == null)
			initializeJob();
		if (!job.contains(updateGrid))
			job.add(updateGrid);
	}

	public Grid getGrid() {
		return grid;
	}

	public void setCutOffShift(boolean b) {
		cutOffShift = b;
		if (b)
			setShiftMatrix(rCutOff);
	}

	public boolean getCutOffShift() {
		return cutOffShift;
	}

	/** set time steplength for integrating the equation of motion */
	public void setTimeStep(double timeStep) {
		super.setTimeStep(timeStep);
		this.timeStep = timeStep;
		timeStep2 = timeStep * timeStep * 0.5;
	}

	/** get time steplength */
	public double getTimeStep() {
		return timeStep;
	}

	public float getCutOff() {
		return rCutOff;
	}

	public float getRList() {
		return rList;
	}

	/** set the cut-off radius matrix for force truncation. */
	public void setCutOffMatrix(float cutoff) {
		if (cutoff <= 0)
			throw new IllegalArgumentException("cutoff<=0");
		rCutOff = cutoff;
		double[] sig = new double[Element.NMAX];
		sig[ID_NT] = nt.getSigma();
		sig[ID_PL] = pl.getSigma();
		sig[ID_WS] = ws.getSigma();
		sig[ID_CK] = ck.getSigma();
		sig[ID_MO] = mo.getSigma();
		sig[ID_SP] = sp.getSigma();
		for (int i = ID_ALA; i <= ID_VAL; i++)
			sig[i] = aminoAcidElement[i - ID_ALA].getSigma();
		for (int i = ID_A; i <= ID_U; i++)
			sig[i] = nucleotideElement[i - ID_A].getSigma();
		int n = sig.length;
		for (int i = 0; i < n - 1; i++) {
			for (int j = i + 1; j < n; j++) {
				if (((i == ID_A && j == ID_T) || (i == ID_T && j == ID_A))
						|| ((i == ID_C && j == ID_G) || (i == ID_G && j == ID_C))
						|| ((i == ID_A && j == ID_U) || (i == ID_U && j == ID_A))) {
					cutOffSquareMatrix[j][i] = cutOffSquareMatrix[i][j] = sig[j] * sig[i] * 4 * cutoff * cutoff;
				}
				else {
					cutOffSquareMatrix[j][i] = cutOffSquareMatrix[i][j] = sig[j] * sig[i] * cutoff * cutoff;
				}
			}
		}
		for (int i = 0; i < n; i++) {
			cutOffSquareMatrix[i][i] = sig[i] * sig[i] * cutoff * cutoff;
		}
		setShiftMatrix(cutoff);
	}

	/**
	 * set the shift potential matrix. Shifting potentials is for achieving better energy conservation. This method
	 * should be called for the all-atom simulator that implements user-defined interspecies interactions.
	 */
	public void setShiftMatrix(float cutoff) {

		if (cutoff <= 0)
			throw new IllegalArgumentException("cutoff<=0");

		double eps[] = new double[Element.NMAX];
		eps[ID_NT] = nt.getEpsilon();
		eps[ID_PL] = pl.getEpsilon();
		eps[ID_WS] = ws.getEpsilon();
		eps[ID_CK] = ck.getEpsilon();
		eps[ID_MO] = mo.getEpsilon();
		eps[ID_SP] = sp.getEpsilon();

		double sig[] = new double[Element.NMAX];
		sig[ID_NT] = nt.getSigma();
		sig[ID_PL] = pl.getSigma();
		sig[ID_WS] = ws.getSigma();
		sig[ID_CK] = ck.getSigma();
		sig[ID_MO] = mo.getSigma();
		sig[ID_SP] = sp.getSigma();

		for (int i = ID_ALA; i <= ID_VAL; i++) {
			eps[i] = aminoAcidElement[i - ID_ALA].getEpsilon();
			sig[i] = aminoAcidElement[i - ID_ALA].getSigma();
		}

		for (int i = ID_A; i <= ID_U; i++) {
			eps[i] = nucleotideElement[i - ID_A].getEpsilon();
			sig[i] = nucleotideElement[i - ID_A].getSigma();
		}

		int n = eps.length;
		double epsij[][] = new double[n][n];
		double sigij[][] = new double[n][n];
		for (int i = 0; i < n; i++) {
			epsij[i][i] = getElement(i).getEpsilon();
			sigij[i][i] = getElement(i).getSigma();
		}
		for (int i = 0; i < n - 1; i++) {
			for (int j = i + 1; j < n; j++) {
				epsij[j][i] = epsij[i][j] = affinity.getEpsilon(getElement(i), getElement(j));
				sigij[j][i] = sigij[i][j] = affinity.getSigma(getElement(i), getElement(j));
			}
		}

		double r2 = 1.0 / (cutoff * cutoff);
		double sr6 = r2 * r2 * r2;
		double sr12 = sr6 * sr6;
		double rtemp = 1.0;
		boolean hate = false, mix = false;
		for (int i = 0; i < n; i++) {
			for (int j = 0; j < n; j++) {
				hate = isRepulsive(i, j);
				mix = i != j ? affinity.isLBMixed(getElement(i), getElement(j)) : false;
				if (hate) {
					poten_LJ[i][j] = 4.0 * epsij[i][j] * sr6 * crossRepulsionIntensity;
					rtemp = 1.0 / (cutoff * sigij[i][j]);
					slope_LJ[i][j] = -24.0 * epsij[i][j] * sr6 * rtemp * crossRepulsionIntensity
							* GF_CONVERSION_CONSTANT;
				}
				else if (!hate && !mix) {
					poten_LJ[i][j] = 4.0 * epsij[i][j] * (sr12 - sr6);
					rtemp = 1.0 / (cutoff * sigij[i][j]);
					slope_LJ[i][j] = 4.0 * epsij[i][j] * (-12.0 * sr12 * rtemp + 6.0 * sr6 * rtemp)
							* GF_CONVERSION_CONSTANT;
				}
				else {
					poten_LJ[i][j] = 2.0 * (eps[i] + eps[j]) * (sr12 - sr6);
					rtemp = 1.0 / (cutoff * Math.sqrt(sig[i] * sig[j]));
					slope_LJ[i][j] = 2.0 * (eps[i] + eps[j]) * (-12.0 * sr12 * rtemp + 6.0 * sr6 * rtemp)
							* GF_CONVERSION_CONSTANT;
				}
			}
		}

	}

	/** set the radius matrix for neighbor list updates */
	public void setListMatrix(float rList) {
		if (rList <= 0)
			throw new IllegalArgumentException("rList<=0");
		this.rList = rList;
		double[] sig = new double[Element.NMAX];
		sig[ID_NT] = nt.getSigma();
		sig[ID_PL] = pl.getSigma();
		sig[ID_WS] = ws.getSigma();
		sig[ID_CK] = ck.getSigma();
		sig[ID_MO] = mo.getSigma();
		sig[ID_SP] = sp.getSigma();
		for (int i = ID_ALA; i <= ID_VAL; i++)
			sig[i] = aminoAcidElement[i - ID_ALA].getSigma();
		for (int i = ID_A; i <= ID_U; i++)
			sig[i] = nucleotideElement[i - ID_A].getSigma();
		int n = sig.length;
		for (int i = 0; i < n - 1; i++) {
			for (int j = i + 1; j < n; j++) {
				listSquareMatrix[j][i] = listSquareMatrix[i][j] = sig[j] * sig[i] * rList * rList;
			}
		}
		for (int i = 0; i < n; i++) {
			listSquareMatrix[i][i] = sig[i] * sig[i] * rList * rList;
		}
	}

	public synchronized int getNumberOfParticles() {
		return numberOfAtoms;
	}

	public synchronized void setNumberOfParticles(int n) {
		setNumberOfAtoms(n);
	}

	/** same as <tt>getNumberOfParticles()</tt> */
	public synchronized int getNumberOfAtoms() {
		return numberOfAtoms;
	}

	/** same as <tt>setNumberOfParticles(int n)</tt> */
	public synchronized void setNumberOfAtoms(int n) {
		if (n < 0)
			throw new IllegalArgumentException("# of atoms cannot be negative");
		numberOfAtoms = n;
		updateParArray = true;
		view.notifyNOPChange();
	}

	/**
	 * @param givenID
	 *            the input ID number for a specified type of atom
	 * @return the number of atoms of the specified ID
	 */
	public int getNumberOfAtoms(int givenID) {
		int n = 0;
		for (int i = 0; i < numberOfAtoms; i++) {
			if (atom[i].id == givenID)
				n++;
		}
		return n;
	}

	public synchronized float getTotalKineticEnergy() {
		getKin();
		return (float) eKT;
	}

	public synchronized float getTotalLJEnergy() {
		return (float) eLJ;
	}

	public synchronized float getTotalElectrostaticEnergy() {
		return (float) eES;
	}

	public synchronized float getTotalElectricFieldEnergy() {
		return (float) eEF;
	}

	public synchronized float getTotalGravitationalFieldEnergy() {
		return (float) eGF;
	}

	public synchronized float getTotalRestraintEnergy() {
		return (float) eRS;
	}

	void showTimeSeries(TimeSeriesGenerator.Parameter[] p) {
		if (tsGenerator == null)
			tsGenerator = new TimeSeriesGenerator(this);
		tsGenerator.show(p);
	}

	void showPCF(Pcf.Parameter[] p) {
		if (pcf == null)
			pcf = new Pcf(this);
		pcf.show(p);
	}

	void showTCF(Tcf.Parameter[] p) {
		if (tcf == null)
			tcf = new Tcf(this);
		tcf.show(p);
	}

	void showMVD(Mvd.Parameter[] p) {
		if (mvd == null)
			mvd = new Mvd(this);
		mvd.show(p);
	}

	/**
	 * @return the <i>i</i>-th atom of the atom array, <tt>null</tt> if the passed index is out of the array's bound.
	 */
	public Atom getAtom(int i) {
		if (i < 0 || i >= atom.length)
			return null;
		return atom[i];
	}

	/** same as <tt>getAtom(int i)</tt>, but you have to downcast */
	public Particle getParticle(int i) {
		if (i < 0 || i >= atom.length)
			return null;
		return atom[i];
	}

	public Atom[] getAtoms() {
		return atom;
	}

	/**
	 * rescale velocities such that the temperature will be equal to the input value, without setting the total momentum
	 * to be zero.
	 */
	public synchronized void setTemperature(double temperature) {
		if (temperature < ZERO)
			temperature = 0.0;
		boolean electricFieldOn = false;
		synchronized (fields) {
			for (int n = 0, m = fields.size(); n < m; n++) {
				if (fields.elementAt(n) instanceof ElectricField) {
					electricFieldOn = true;
					break;
				}
			}
		}
		if (electricFieldOn && hasCoulomb && interCoulomb) {
			List[] list = splitParticlesByCharges();
			for (List l : list)
				setTemperature(l, temperature);
		}
		else if (isSteeringOn()) {
			List[] list = splitParticlesBySteering();
			for (List l : list)
				setTemperature(l, temperature);
		}
		else {
			double temp1 = getKin() * UNIT_EV_OVER_KB;
			if (temp1 < ZERO && temperature > ZERO) {
				assignTemperature(temperature);
				temp1 = getKin() * UNIT_EV_OVER_KB;
			}
			if (temp1 > ZERO)
				rescaleVelocities(Math.sqrt(temperature / temp1));
		}
	}

	private List[] splitParticlesByCharges() {
		List[] list = new List[2];
		list[0] = new ArrayList<Atom>();
		list[1] = new ArrayList<Atom>();
		for (int i = 0; i < numberOfAtoms; i++)
			list[atom[i].charge != 0 ? 0 : 1].add(atom[i]);
		return list;
	}

	private boolean isSteeringOn() {
		for (int i = 0; i < numberOfAtoms; i++)
			if (atom[i].userField != null)
				return true;
		return false;
	}

	private List[] splitParticlesBySteering() {
		List[] list = new List[2];
		list[0] = new ArrayList<Atom>();
		list[1] = new ArrayList<Atom>();
		for (int i = 0; i < numberOfAtoms; i++)
			list[atom[i].userField != null ? 0 : 1].add(atom[i]);
		return list;
	}

	/**
	 * set the temperature of the atoms of the specified ID to be the specified value.
	 */
	public synchronized void setTemperature(byte elementID, double temperature) {
		if (typeList == null) {
			typeList = new ArrayList<Atom>();
		}
		else {
			typeList.clear();
		}
		for (int i = 0; i < numberOfAtoms; i++)
			if (atom[i].id == elementID)
				typeList.add(atom[i]);
		if (typeList.isEmpty())
			return;
		setTemperature(typeList, temperature);
	}

	/** set the temperature of the atom list to be the specified value. */
	public synchronized void setTemperature(List<Atom> list, double temperature) {
		if (list == null || list.isEmpty())
			return;
		if (temperature < ZERO)
			temperature = 0.0;
		double temp1 = getKinForParticles(list) * UNIT_EV_OVER_KB;
		if (temp1 < ZERO) {
			assignTemperature(list, 100.0);
			temp1 = getKinForParticles(list) * UNIT_EV_OVER_KB;
		}
		rescaleVelocities(list, Math.sqrt(temperature / temp1));
	}

	/** change the temperature by percentage */
	public void changeTemperature(double percent) {
		if (percent < -1.0)
			percent = -1.0;
		if (!heatBathActivated()) {
			double temp1 = getKin() * UNIT_EV_OVER_KB;
			if (temp1 < ZERO)
				assignTemperature(100.0);
			rescaleVelocities(Math.sqrt(percent + 1.0));
		}
		else {
			heatBath.changeExpectedTemperature(percent);
			setTemperature(heatBath.getExpectedTemperature());
		}
	}

	public double getTemperature() {
		return getKin() * UNIT_EV_OVER_KB;
	}

	private double[] getTotalKineticEnergy(byte type, Shape shape) {
		double[] result = new double[2];
		result[0] = result[1] = 0;
		Atom a = null;
		for (int i = 0; i < numberOfAtoms; i++) {
			a = atom[i];
			if (type == -1) {
				if (shape == null || a.isCenterOfMassContained(shape)) {
					result[0]++;
					result[1] += (a.vx * a.vx + a.vy * a.vy) * a.mass;
				}
			}
			else {
				if (a.id == type) {
					if (shape == null || a.isCenterOfMassContained(shape)) {
						result[0]++;
						result[1] += (a.vx * a.vx + a.vy * a.vy) * a.mass;
					}
				}
			}
		}
		// the prefactor 0.5 doesn't show up here because it has been included in the conversion factors.
		result[1] *= EV_CONVERTER;
		return result;
	}

	public double getKineticEnergy(byte type, Shape shape) {
		return getTotalKineticEnergy(type, shape)[1];
	}

	public double getTemperature(byte type, Shape shape) {
		double[] result = getTotalKineticEnergy(type, shape);
		if (result[0] < 0.99)
			return 0;
		return result[1] * UNIT_EV_OVER_KB / result[0];
	}

	public int getParticleCount(byte type, Shape shape) {
		int n = 0;
		for (int i = 0; i < numberOfAtoms; i++) {
			if (type == -1) {
				if (shape == null || atom[i].isCenterOfMassContained(shape)) {
					n++;
				}
			}
			else {
				if (atom[i].id == type) {
					if (shape == null || atom[i].isCenterOfMassContained(shape)) {
						n++;
					}
				}
			}
		}
		return n;
	}

	public double getAverageSpeed(String direction, byte type, Shape shape) {
		double v = 0;
		boolean isVx = "x".equalsIgnoreCase(direction);
		boolean isVy = "y".equalsIgnoreCase(direction);
		int n = 0;
		for (int i = 0; i < numberOfAtoms; i++) {
			if (type == -1) {
				if (shape == null || atom[i].isCenterOfMassContained(shape)) {
					if (isVx)
						v += atom[i].vx;
					else if (isVy)
						v += atom[i].vy;
					n++;
				}
			}
			else if (atom[i].id == type) {
				if (shape == null || atom[i].isCenterOfMassContained(shape)) {
					if (isVx)
						v += atom[i].vx;
					else if (isVy)
						v += atom[i].vy;
					n++;
				}
			}
		}
		return n == 0 ? 0 : v / n;
	}

	public void transferHeat(double amount) {
		if (getNumberOfAtoms() <= 0)
			return;
		double k0 = getKin();
		if (k0 < ZERO)
			assignTemperature(1);
		for (int i = 0; i < numberOfAtoms; i++) {
			Atom a = atom[i];
			k0 = EV_CONVERTER * a.mass * (a.vx * a.vx + a.vy * a.vy);
			if (k0 <= ZERO)
				k0 = ZERO;
			k0 = (k0 + amount) / k0;
			if (k0 <= ZERO)
				k0 = ZERO;
			k0 = Math.sqrt(k0);
			a.vx *= k0;
			a.vy *= k0;
		}
	}

	private void transferHeatToType(byte elementID, double amount) {
		if (typeList == null) {
			typeList = new ArrayList<Atom>();
		}
		else {
			typeList.clear();
		}
		for (int i = 0; i < numberOfAtoms; i++) {
			if (atom[i].id == elementID)
				typeList.add(atom[i]);
		}
		if (typeList.isEmpty())
			return;
		transferHeatToParticles(typeList, amount);
	}

	public void transferHeatToParticles(List list, double amount) {
		if (list == null || list.isEmpty())
			return;
		double k0 = getKinForParticles(list);
		if (k0 < ZERO)
			assignTemperature(list, 1);
		Atom a = null;
		Object o = null;
		for (Iterator it = list.iterator(); it.hasNext();) {
			o = it.next();
			if (o instanceof Atom) {
				a = (Atom) o;
				k0 = EV_CONVERTER * a.mass * (a.vx * a.vx + a.vy * a.vy);
				if (k0 <= ZERO)
					k0 = ZERO;
				k0 = (k0 + amount) / k0;
				if (k0 <= ZERO)
					k0 = ZERO;
				k0 = Math.sqrt(k0);
				a.vx *= k0;
				a.vy *= k0;
			}
		}
	}

	/* assign velocities to a group of atoms according to the temperature given */
	void assignTemperature(List list, double temperature) {
		if (list == null || list.isEmpty())
			return;
		if (temperature < ZERO)
			temperature = ZERO;
		double rtemp = Math.sqrt(temperature) * VT_CONVERSION_CONSTANT;
		Atom a = null;
		if (list.size() == 1) {
			((Atom) list.get(0)).setRandomVelocity(rtemp);
			return;
		}
		Object o = null;
		for (Iterator it = list.iterator(); it.hasNext();) {
			o = it.next();
			if (o instanceof Atom) {
				a = (Atom) o;
				a.vx = rtemp * RANDOM.nextGaussian();
				a.vy = rtemp * RANDOM.nextGaussian();
			}
		}
	}

	/** assign velocities to atoms according to the Boltzman-Maxwell distribution */
	public synchronized void assignTemperature(double temperature) {
		if (temperature < ZERO)
			temperature = 0.0;
		double rtemp = Math.sqrt(temperature) * VT_CONVERSION_CONSTANT;
		double sumVx = 0.0;
		double sumVy = 0.0;
		double sumMass = 0.0;
		if (numberOfAtoms == 1) {
			atom[0].setRandomVelocity(rtemp);
		}
		else {
			for (int i = 0; i < numberOfAtoms; i++) {
				Atom a = atom[i];
				a.vx = rtemp * RANDOM.nextGaussian();
				a.vy = rtemp * RANDOM.nextGaussian();
				sumVx += a.vx * a.mass;
				sumVy += a.vy * a.mass;
				sumMass += a.mass;
			}
		}
		if (obstacles != null && !obstacles.isEmpty()) {
			RectangularObstacle obs = null;
			double mass = 1.0;
			synchronized (obstacles.getSynchronizationLock()) {
				for (Iterator it = obstacles.iterator(); it.hasNext();) {
					obs = (RectangularObstacle) it.next();
					if (obs.getDensity() < RectangularObstacle.HEAVY - 10) {
						obs.vx = rtemp * RANDOM.nextGaussian();
						obs.vy = rtemp * RANDOM.nextGaussian();
						mass = obs.getMass();
						sumVx += obs.vx * mass;
						sumVy += obs.vy * mass;
						sumMass += mass;
					}
				}
			}
		}
		if (sumMass > ZERO) {
			sumVx /= sumMass;
			sumVy /= sumMass;
			if (numberOfAtoms > 1) {
				for (int i = 0; i < numberOfAtoms; i++) {
					atom[i].vx -= sumVx;
					atom[i].vy -= sumVy;
				}
			}
			if (obstacles != null && !obstacles.isEmpty()) {
				RectangularObstacle obs = null;
				synchronized (obstacles.getSynchronizationLock()) {
					for (Iterator it = obstacles.iterator(); it.hasNext();) {
						obs = (RectangularObstacle) it.next();
						if (obs.getDensity() < RectangularObstacle.HEAVY - 10) {
							obs.vx -= sumVx;
							obs.vy -= sumVy;
						}
					}
				}
			}
			setTemperature(temperature);
		}
	}

	synchronized void rescaleVelocities(double ratio) {
		for (int i = 0; i < numberOfAtoms; i++) {
			Atom a = atom[i];
			a.vx *= ratio;
			a.vy *= ratio;
		}
		if (obstacles != null && !obstacles.isEmpty()) {
			RectangularObstacle obs = null;
			synchronized (obstacles.getSynchronizationLock()) {
				for (int i = 0, n = obstacles.size(); i < n; i++) {
					obs = obstacles.get(i);
					if (obs.isPartOfSystem() && obs.isMovable()) {
						obs.vx *= ratio;
						obs.vy *= ratio;
					}
				}
			}
		}
	}

	private synchronized void rescaleVelocities(List<Atom> list, double ratio) {
		for (Atom a : list) {
			a.vx *= ratio;
			a.vy *= ratio;
		}
	}

	synchronized void resetIntegrator() {
		for (Atom a : atom) {
			a.vx = a.vy = a.ax = a.ay = a.fx = a.fy = 0.0;
		}
		if (obstacles != null && !obstacles.isEmpty()) {
			RectangularObstacle obs = null;
			synchronized (obstacles.getSynchronizationLock()) {
				for (Iterator it = obstacles.iterator(); it.hasNext();) {
					obs = (RectangularObstacle) it.next();
					obs.vx = obs.vy = obs.ax = obs.ay = 0.0;
				}
			}
		}
	}

	public synchronized void setFriction(float friction) {
		for (int i = 0; i < numberOfAtoms; i++)
			atom[i].setFriction(friction);
		if (obstacles != null && !obstacles.isEmpty()) {
			RectangularObstacle obs = null;
			synchronized (obstacles.getSynchronizationLock()) {
				for (Iterator it = obstacles.iterator(); it.hasNext();) {
					obs = (RectangularObstacle) it.next();
					obs.setFriction(friction);
				}
			}
		}
	}

	/**
	 * @return true if any atom is detected charged. If no charge is found, the procedure of computing the electrostatic
	 *         forces can be skipped.
	 */
	public boolean checkCharges() {
		hasCoulomb = false;
		for (int i = 0; i < numberOfAtoms; i++) {
			if (atom[i].charge != 0) {
				hasCoulomb = true;
				break;
			}
		}
		return hasCoulomb;
	}

	public void setInterCoulomb(boolean b) {
		interCoulomb = b;
	}

	public boolean getInterCoulomb() {
		return interCoulomb;
	}

	synchronized void advance(int indexOfStep) {
		if (heatBathActivated()) {
			if (heatBath != null && heatBath.getExpectedTemperature() < 0.01)
				return;
		}
		predictor();
		if (indexOfStep <= 1) {
			setUpdateList(true);
		}
		else {
			checkNeighborList();
		}
		pot = computeForce(indexOfStep);
		corrector();
		movePhotons();
	}

	private void movePhotons() {
		if (photonList == null || photonList.isEmpty())
			return;
		synchronized (photonList) {
			for (Photon p : photonList) {
				p.move((float) timeStep);
			}
		}
	}

	private void shootPhotons() {
		double w = boundary.width;
		double h = boundary.height;
		float angle = 0;
		int direct = lightSource.getDirection();
		switch (direct) {
		case LightSource.EAST:
			angle = (float) Math.PI;
			break;
		case LightSource.NORTH:
			angle = (float) Math.PI * 0.5f;
			break;
		case LightSource.SOUTH:
			angle = (float) Math.PI * 1.5f;
			break;
		case LightSource.OTHER:
			angle = lightSource.getAngleOfIncidence();
			break;
		}
		if (lightSource.isSingleBeam()) {
			switch (direct) {
			case LightSource.WEST:
				addPhotonAtAngle(0, (int) (h * 0.5), angle);
				break;
			case LightSource.EAST:
				addPhotonAtAngle((int) w, (int) (h * 0.5), angle);
				break;
			case LightSource.NORTH:
				addPhotonAtAngle((int) (w * 0.5), 0, angle);
				break;
			case LightSource.SOUTH:
				addPhotonAtAngle((int) (w * 0.5), (int) h, angle);
				break;
			}
		}
		else {
			int spacing = 40;
			switch (direct) {
			case LightSource.WEST:
				int n = (int) (h / spacing);
				for (int i = 0; i <= n; i++)
					addPhotonAtAngle(0, spacing * i, angle);
				break;
			case LightSource.EAST:
				n = (int) (h / spacing);
				for (int i = 0; i <= n; i++)
					addPhotonAtAngle((int) w, spacing * i, angle);
				break;
			case LightSource.NORTH:
				int m = (int) (w / spacing);
				for (int i = 0; i <= m; i++)
					addPhotonAtAngle(spacing * i, 0, angle);
				break;
			case LightSource.SOUTH:
				m = (int) (w / spacing);
				for (int i = 0; i <= m; i++)
					addPhotonAtAngle(spacing * i, (int) h, angle);
				break;
			case LightSource.OTHER:
				int dx = (int) (spacing / Math.abs(Math.sin(angle)));
				int dy = (int) (spacing / Math.abs(Math.cos(angle)));
				m = (int) (w / dx);
				n = (int) (h / dy);
				if (angle > 0 && angle < 0.5 * Math.PI) {
					for (int i = 1; i <= m; i++)
						addPhotonAtAngle(dx * i, 0, angle);
					for (int i = 0; i <= n; i++)
						addPhotonAtAngle(0, dy * i, angle);
				}
				else if (angle < 0 && angle > -0.5 * Math.PI) {
					for (int i = 1; i <= m; i++)
						addPhotonAtAngle(dx * i, (int) h, angle);
					for (int i = 0; i <= n; i++)
						addPhotonAtAngle(0, (int) (h - dy * i), angle);
				}
				else if (angle < Math.PI && angle > 0.5 * Math.PI) {
					for (int i = 0; i <= m; i++)
						addPhotonAtAngle((int) (w - dx * i), 0, angle);
					for (int i = 1; i <= n; i++)
						addPhotonAtAngle((int) w, dy * i, angle);
				}
				else if (angle > -Math.PI && angle < -0.5 * Math.PI) {
					for (int i = 0; i <= m; i++)
						addPhotonAtAngle((int) (w - dx * i), (int) h, angle);
					for (int i = 1; i <= n; i++)
						addPhotonAtAngle((int) w, (int) (h - dy * i), angle);
				}
				break;
			}
		}
	}

	private void addPhotonAtAngle(int x, int y, float angle) {
		float freq = lightSource.isMonochromatic() ? lightSource.getFrequency() : LightSource.getRandomFrequency();
		Photon p = new Photon(x, y, freq);
		p.setAngle(angle);
		p.setModel(this);
		p.setFromLightSource(true);
		addPhoton(p);
	}

	/* thermal excitation: absorb heat and excite electrons. */
	private void thermalExciteAtoms() {
		double rxi, ryi, rxij, ryij, rijsq, sig;
		for (int i = 0; i < numberOfAtoms - 1; i++) {
			rxi = atom[i].rx;
			ryi = atom[i].ry;
			for (int j = i + 1; j < numberOfAtoms; j++) {
				if (atom[i].isExcitable() || atom[j].isExcitable()) {
					rxij = rxi - atom[j].rx;
					ryij = ryi - atom[j].ry;
					rijsq = rxij * rxij + ryij * ryij;
					sig = 0.5 * (atom[i].sigma + atom[j].sigma);
					sig *= sig;
					if (rijsq < sig) {
						atom[i].thermalExcitation();
						atom[j].thermalExcitation();
					}
				}
			}
		}
	}

	/*
	 * Two things can happen when a photon hits an atom. The first is absorption: the atom absorbs photonic energy and
	 * its electron is excited. The second is stimulated emission: the photon induces the emission of another photon and
	 * causes the atom to de-excite. In both cases, the incident photon must have the energy identical to the energy
	 * difference between the two states involved in the transition.
	 */
	void photonHitAtom() {
		if (photonList == null || photonList.isEmpty())
			return;
		float prob;
		try {
			prob = quantumRule.getProbability(QuantumRule.STIMULATED_EMISSION);
		}
		catch (Exception e) {
			prob = 0.5f;
		}
		Photon p;
		double s2;
		List<Photon> tmpList = null;
		double dx, dy;
		for (int k = 0; k < numberOfAtoms; k++) {
			if (photonList.isEmpty())
				break; // no more photons
			s2 = atom[k].sigma * atom[k].sigma * 0.25;
			synchronized (photonList) {
				for (Iterator it = photonList.iterator(); it.hasNext();) {
					p = (Photon) it.next();
					dx = p.x - atom[k].rx;
					dy = p.y - atom[k].ry;
					if (dx * dx + dy * dy < s2) {
						Photon p2 = atom[k].hitByPhoton(p, prob);
						if (p2 == p) {
							p.setModel(null);
							it.remove();
							notifyModelListeners(new ModelEvent(this, "Photon absorbed", null, p));
						}
						else if (p2 != null) {
							if (tmpList == null)
								tmpList = new ArrayList<Photon>();
							tmpList.add(p2);
							// notifyModelListeners(new ModelEvent(this, "Photon emitted", null, p2));
							// postone to be handled by RectangularBoundary when the photon exits
						}
					}
				}
				if (tmpList != null) {
					photonList.addAll(tmpList);
					tmpList.clear();
				}
			}
		}
	}

	private void collisionalDeexcite(float prob) {
		double rxi, ryi, rxij, ryij, rijsq, sig;
		for (int i = 0; i < numberOfAtoms - 1; i++) {
			rxi = atom[i].rx;
			ryi = atom[i].ry;
			for (int j = i + 1; j < numberOfAtoms; j++) {
				if (atom[i].isExcitable() || atom[j].isExcitable()) {
					rxij = rxi - atom[j].rx;
					ryij = ryi - atom[j].ry;
					rijsq = rxij * rxij + ryij * ryij;
					sig = 0.5 * (atom[i].sigma + atom[j].sigma);
					sig *= sig;
					if (rijsq < sig) {
						deexcite(atom[i], prob);
						deexcite(atom[j], prob);
					}
				}
			}
		}
	}

	private void lifetimeDeexcite(float prob) {
		for (int k = 0; k < numberOfAtoms; k++) {
			deexcite(atom[k], prob);
		}
	}

	private void deexcite(Atom a, float prob) {
		Photon p = a.deexciteElectron(prob);
		if (p != null) {
			p.setModel(this);
			p.setAngle((float) (Math.random() * 2 * Math.PI));
			double x = a.sigma * 0.51 * Math.cos(p.getAngle());
			double y = a.sigma * 0.51 * Math.sin(p.getAngle());
			p.setX(p.getX() + (float) x);
			p.setY(p.getY() + (float) y);
			addPhoton(p);
			// notifyModelListeners(new ModelEvent(this, "Photon emitted", null, p));
			// postone to be handled by RectangularBoundary when the photon exits
		}
	}

	private void deexciteElectrons() {
		float prob;
		try {
			prob = quantumRule.getProbability(QuantumRule.RADIATIONLESS_TRANSITION);
		}
		catch (Exception e) {
			prob = 0.5f;
		}
		if (collisionalDeexcitation) {
			collisionalDeexcite(prob);
		}
		else {
			lifetimeDeexcite(prob);
		}
	}

	void putInBounds() {
		switch (boundary.getType()) {
		case RectangularBoundary.DBC_ID:
			boundary.setRBC(this);
			break;
		case RectangularBoundary.RBC_ID:
			boundary.setRBC(this);
			break;
		case RectangularBoundary.PBC_ID:
			boundary.setPBC(this);
			break;
		case RectangularBoundary.XRYPBC_ID:
			boundary.setXRYPBC(this);
			break;
		case RectangularBoundary.XPYRBC_ID:
			boundary.setXPYRBC(this);
			break;
		}
	}

	/* applying the minimum image conventions */
	private void minimumImageConvention() {
		switch (boundary.getType()) {
		case RectangularBoundary.PBC_ID:
			if (rxij > xbox * 0.5) {
				rxij -= xbox;
			}
			if (rxij <= -xbox * 0.5) {
				rxij += xbox;
			}
			if (ryij > ybox * 0.5) {
				ryij -= ybox;
			}
			if (ryij <= -ybox * 0.5) {
				ryij += ybox;
			}
			break;
		case RectangularBoundary.XRYPBC_ID:
			if (ryij > ybox * 0.5) {
				ryij -= ybox;
			}
			if (ryij <= -ybox * 0.5) {
				ryij += ybox;
			}
			break;
		case RectangularBoundary.XPYRBC_ID:
			if (rxij > xbox * 0.5) {
				rxij -= xbox;
			}
			if (rxij <= -xbox * 0.5) {
				rxij += xbox;
			}
			break;
		}
	}

	/**
	 * compute forces on the atoms from the potentials. This is the most expensive part of calculation. This method is
	 * synchronized so that no intermediate data can be fetched before a round of computation is completed.
	 * 
	 * @param time
	 *            the current time, used in computing time-dependent forces
	 * @return potential energy per atom
	 */
	public synchronized double computeForce(final int time) {

		double vsum = 0.0;
		eKT = 0.0;
		eLJ = 0.0;
		eES = 0.0;
		eEF = 0.0;
		eGF = 0.0;
		eAF = 0.0;
		eRS = 0.0;
		if (time <= 0) {
			updateList = true; // the neighbor list has to be updated in order for the force vector to be plotted
		}

		for (int i = 0; i < numberOfAtoms; i++) {
			atom[i].fx = 0;
			atom[i].fy = 0;
		}
		if (obstacles != null) {
			RectangularObstacle obs;
			synchronized (obstacles.getSynchronizationLock()) {
				if (!obstacles.isEmpty()) {
					for (int jobs = 0, nobs = obstacles.size(); jobs < nobs; jobs++) {
						obs = obstacles.get(jobs);
						obs.ax = 0;
						obs.ay = 0;
					}
				}
			}
		}

		if (numberOfAtoms == 1) {

			atom[0].fx = atom[0].hx * GF_CONVERSION_CONSTANT / atom[0].mass;
			atom[0].fy = atom[0].hy * GF_CONVERSION_CONSTANT / atom[0].mass;

			double etemp;

			if (atom[0].friction > ZERO) {
				double dmp = GF_CONVERSION_CONSTANT * universe.getViscosity() * atom[0].friction / atom[0].mass;
				atom[0].fx -= dmp * atom[0].vx;
				atom[0].fy -= dmp * atom[0].vy;
			}

			for (VectorField f : fields) {
				if (f instanceof GravitationalField) {
					GravitationalField gf = (GravitationalField) f;
					gf.dyn(atom[0]);
					etemp = gf.getPotential(atom[0], time);
					vsum += etemp;
					eGF += etemp;
					if (obstacles != null && !obstacles.isEmpty()) {
						RectangularObstacle obs = null;
						synchronized (obstacles.getSynchronizationLock()) {
							for (int jobs = 0, nobs = obstacles.size(); jobs < nobs; jobs++) {
								obs = obstacles.get(jobs);
								gf.dyn(obs);
								etemp = gf.getPotential(obs, time);
								vsum += etemp;
								eGF += etemp;
							}
						}
					}
				}
				else if (f instanceof ElectricField) {
					if (Math.abs(atom[0].charge) > 0) {
						ElectricField ef = (ElectricField) f;
						ef.dyn(universe.getDielectricConstant(), atom[0], time);
						etemp = ef.getPotential(atom[0], time);
						vsum += etemp;
						eEF += etemp;
					}
				}
				else if (f instanceof MagneticField) {
					if (Math.abs(atom[0].charge) > 0) {
						MagneticField mf = (MagneticField) f;
						mf.dyn(atom[0]);
					}
				}
				else if (f instanceof AccelerationalField) {
					AccelerationalField af = (AccelerationalField) f;
					af.dyn(atom[0]);
					etemp = af.getPotential(atom[0], time);
					vsum += etemp;
					eAF += etemp;
					if (obstacles != null && !obstacles.isEmpty()) {
						RectangularObstacle obs = null;
						synchronized (obstacles.getSynchronizationLock()) {
							for (int jobs = 0, nobs = obstacles.size(); jobs < nobs; jobs++) {
								obs = obstacles.get(jobs);
								af.dyn(obs);
								etemp = af.getPotential(obs, time);
								vsum += etemp;
								eAF += etemp;
							}
						}
					}
				}
			}

			if (atom[0].restraint != null) {
				atom[0].restraint.dyn(atom[0]);
				etemp = atom[0].restraint.getEnergy(atom[0]);
				vsum += etemp;
				eRS += etemp;
			}

			if (atom[0].getUserField() != null) {
				atom[0].getUserField().dyn(atom[0]);
			}

			if (time < 0) {
				atom[0].fx *= atom[0].mass;
				atom[0].fy *= atom[0].mass;
			}

			return vsum;

		}

		if (updateParArray) {
			resetParArray();
			updateParArray = false;
		}

		xbox = boundary.width;
		ybox = boundary.height;
		double coul = 0.0;
		double rCD = 1.0;
		if (hasCoulomb && interCoulomb)
			rCD = universe.getCoulombConstant() / universe.getDielectricConstant();

		if (updateList) {

			for (int i = 0; i < numberOfAtoms; i++) {
				rx0[i] = atom[i].rx;
				ry0[i] = atom[i].ry;
			}

			nlist = 0;

			for (int i = 0, imax1 = numberOfAtoms - 1; i < imax1; i++) {

				pointer[i] = nlist;
				rxi = atom[i].rx;
				ryi = atom[i].ry;
				fxi = atom[i].fx;
				fyi = atom[i].fy;

				for (int j = i + 1; j < numberOfAtoms; j++) {

					if (!ljBetweenBondPairs && bondTable[i][j])
						continue;

					rxij = rxi - atom[j].rx;
					ryij = ryi - atom[j].ry;
					minimumImageConvention();
					rijsq = rxij * rxij + ryij * ryij;

					if (rijsq < listSquareMatrix[atom[i].id][atom[j].id]) {
						neighborList[nlist++] = j;
					}

					if (rijsq < cutOffSquareMatrix[atom[i].id][atom[j].id]) {

						sr2 = sigab[i][j] / rijsq;
						/* check if this pair gets too close */
						if (sr2 > 2.0) {
							sr2 = 2.0;
							rijsq = 0.5 * sigab[i][j];
						}
						sr6 = sr2 * sr2 * sr2;
						sr12 = sr6 * sr6;

						if (isRepulsive(atom[i].id, atom[j].id)) {
							vij = sr6 * epsab[i][j] * crossRepulsionIntensity;
							wij = vij;
						}
						else {
							vij = (sr12 - sr6) * epsab[i][j];
							wij = vij + sr12 * epsab[i][j];
						}
						if (cutOffShift) {
							vij -= poten_LJ[atom[i].id][atom[j].id];
							wij -= slope_LJ[atom[i].id][atom[j].id];
						}

						vsum += vij;
						eLJ += vij;
						fij = wij / rijsq * SIX_TIMES_UNIT_FORCE;
						fxij = fij * rxij;
						fyij = fij * ryij;
						fxi += fxij;
						fyi += fyij;
						atom[j].fx -= fxij;
						atom[j].fy -= fyij;

					}

					if (hasCoulomb && interCoulomb) {
						if (Math.abs(atom[i].charge) > ZERO && Math.abs(atom[j].charge) > ZERO) {
							coul = atom[i].charge * atom[j].charge / Math.sqrt(rijsq) * rCD;
							vsum += coul;
							eES += coul;
							fij = coul / rijsq * GF_CONVERSION_CONSTANT;
							fxij = fij * rxij;
							fyij = fij * ryij;
							fxi += fxij;
							fyi += fyij;
							atom[j].fx -= fxij;
							atom[j].fy -= fyij;
						}
					}

				}

				atom[i].fx = fxi;
				atom[i].fy = fyi;

			}

			if (numberOfAtoms > 0)
				pointer[numberOfAtoms - 1] = nlist;

		}
		else {

			for (int i = 0, imax1 = numberOfAtoms - 1; i < imax1; i++) {

				rxi = atom[i].rx;
				ryi = atom[i].ry;
				fxi = atom[i].fx;
				fyi = atom[i].fy;

				jbeg = pointer[i];
				jend = pointer[i + 1];

				if (jbeg < jend) {

					for (int jnab = jbeg; jnab < jend; jnab++) {

						int j = neighborList[jnab];
						if (!ljBetweenBondPairs && bondTable[i][j])
							continue;
						rxij = rxi - atom[j].rx;
						ryij = ryi - atom[j].ry;
						minimumImageConvention();
						rijsq = rxij * rxij + ryij * ryij;

						if (rijsq < cutOffSquareMatrix[atom[i].id][atom[j].id]) {

							sr2 = sigab[i][j] / rijsq;
							/* check if this pair gets too close */
							if (sr2 > 2.0) {
								sr2 = 2.0;
								rijsq = 0.5 * sigab[i][j];
							}
							sr6 = sr2 * sr2 * sr2;
							sr12 = sr6 * sr6;

							if (isRepulsive(atom[i].id, atom[j].id)) {
								vij = sr6 * epsab[i][j] * crossRepulsionIntensity;
								wij = vij;
							}
							else {
								vij = (sr12 - sr6) * epsab[i][j];
								wij = vij + sr12 * epsab[i][j];
							}
							if (cutOffShift) {
								vij -= poten_LJ[atom[i].id][atom[j].id];
								wij -= slope_LJ[atom[i].id][atom[j].id];
							}

							vsum += vij;
							eLJ += vij;

							fij = wij / rijsq * SIX_TIMES_UNIT_FORCE;
							fxij = fij * rxij;
							fyij = fij * ryij;
							fxi += fxij;
							fyi += fyij;
							atom[j].fx -= fxij;
							atom[j].fy -= fyij;

						}
					}
				}

				if (hasCoulomb && interCoulomb) { // no Verlet neighbor list for long-range electrostatic potentials

					for (int j = i + 1; j < numberOfAtoms; j++) {
						if (!ljBetweenBondPairs && bondTable[i][j])
							continue;
						if (Math.abs(atom[i].charge) > ZERO && Math.abs(atom[j].charge) > ZERO) {
							rxij = rxi - atom[j].rx;
							ryij = ryi - atom[j].ry;
							minimumImageConvention();
							rijsq = rxij * rxij + ryij * ryij;
							coul = atom[i].charge * atom[j].charge / Math.sqrt(rijsq) * rCD;
							vsum += coul;
							eES += coul;
							fij = coul / rijsq * GF_CONVERSION_CONSTANT;
							fxij = fij * rxij;
							fyij = fij * ryij;
							fxi += fxij;
							fyi += fyij;
							atom[j].fx -= fxij;
							atom[j].fy -= fyij;
						}
					}

				}

				atom[i].fx = fxi;
				atom[i].fy = fyi;

			}

		}

		double inverseMass;
		for (int i = 0; i < numberOfAtoms; i++) {
			atom[i].fx += atom[i].hx * GF_CONVERSION_CONSTANT;
			atom[i].fy += atom[i].hy * GF_CONVERSION_CONSTANT;
			inverseMass = 1.0 / atom[i].mass;
			atom[i].fx *= inverseMass;
			atom[i].fy *= inverseMass;
		}

		// pointwise space restraints do not contribute to the internal pressure
		// but they maintains the energy conservation law.
		for (int i = 0; i < numberOfAtoms; i++) {
			double etemp;
			Atom a = atom[i];
			if (a.restraint != null) {
				a.restraint.dyn(a);
				etemp = a.restraint.getEnergy(a);
				vsum += etemp;
				eRS += etemp;
			}
			if (a.friction > ZERO) {
				inverseMass = GF_CONVERSION_CONSTANT * universe.getViscosity() * a.friction / a.mass;
				a.fx -= inverseMass * a.vx;
				a.fy -= inverseMass * a.vy;
			}
			if (a.getUserField() != null) {
				a.getUserField().dyn(a);
			}
		}

		double etemp = 0.0;
		for (VectorField f : fields) {
			if (f instanceof GravitationalField) {
				GravitationalField gf = (GravitationalField) f;
				if (obstacles != null && !obstacles.isEmpty()) {
					RectangularObstacle obs = null;
					synchronized (obstacles.getSynchronizationLock()) {
						for (int jobs = 0, nobs = obstacles.size(); jobs < nobs; jobs++) {
							obs = obstacles.get(jobs);
							gf.dyn(obs);
							etemp = gf.getPotential(obs, time);
							vsum += etemp;
							eGF += etemp;
						}
					}
				}
				for (int i = 0; i < numberOfAtoms; i++) {
					gf.dyn(atom[i]);
					etemp = gf.getPotential(atom[i], time);
					vsum += etemp;
					eGF += etemp;
				}
			}
			else if (f instanceof ElectricField) {
				ElectricField ef = (ElectricField) f;
				for (int i = 0; i < numberOfAtoms; i++) {
					if (Math.abs(atom[i].charge) > ZERO) {
						ef.dyn(universe.getDielectricConstant(), atom[i], time);
						etemp = ef.getPotential(atom[i], time);
						vsum += etemp;
						eEF += etemp;
					}
				}
			}
			else if (f instanceof MagneticField) {
				MagneticField mf = (MagneticField) f;
				for (int i = 0; i < numberOfAtoms; i++) {
					if (Math.abs(atom[i].charge) > ZERO)
						mf.dyn(atom[i]);
				}
			}
			else if (f instanceof AccelerationalField) {
				AccelerationalField af = (AccelerationalField) f;
				if (obstacles != null && !obstacles.isEmpty()) {
					RectangularObstacle obs = null;
					synchronized (obstacles.getSynchronizationLock()) {
						for (int jobs = 0, nobs = obstacles.size(); jobs < nobs; jobs++) {
							obs = obstacles.get(jobs);
							af.dyn(obs);
							etemp = af.getPotential(obs, time);
							vsum += etemp;
							eAF += etemp;
						}
					}
				}
				for (int i = 0; i < numberOfAtoms; i++) {
					af.dyn(atom[i]);
					etemp = af.getPotential(atom[i], time);
					vsum += etemp;
					eAF += etemp;
				}
			}
		}

		if (time < 0) {
			for (int i = 0; i < numberOfAtoms; i++) {
				atom[i].fx *= atom[i].mass;
				atom[i].fy *= atom[i].mass;
			}
		}

		return numberOfAtoms > 0 ? vsum / numberOfAtoms : vsum;

	}

	/**
	 * minimize the potential energy using the steepest descent method. If the maximum force computed is not big, either
	 * exit or slowly descent. This is for use in conjunction with the molecular dynamics engine and structure editor.
	 * For purely minimization purpose, use sd() or cp() instead.
	 */
	public synchronized void steepestDescent(double stepLength) {
		if (numberOfAtoms == 1)
			return;
		double maxForce, delta;
		computeForce(-1);
		maxForce = 0.0;
		for (int i = 0; i < numberOfAtoms; i++) {
			if (maxForce < Math.abs(atom[i].fx))
				maxForce = Math.abs(atom[i].fx);
			if (maxForce < Math.abs(atom[i].fy))
				maxForce = Math.abs(atom[i].fy);
		}
		do {
			delta = stepLength < maxForce ? stepLength / maxForce : 0.1;
			for (int i = 0; i < numberOfAtoms; i++) {
				if (!atom[i].isMovable())
					continue;
				atom[i].rx += atom[i].fx * delta;
				atom[i].ry += atom[i].fy * delta;
			}
			putInBounds();
			computeForce(-1);
			maxForce = 0.0;
			for (int i = 0; i < numberOfAtoms; i++) {
				if (maxForce < Math.abs(atom[i].fx))
					maxForce = Math.abs(atom[i].fx);
				if (maxForce < Math.abs(atom[i].fy))
					maxForce = Math.abs(atom[i].fy);
			}
		} while (maxForce > (hasCoulomb && interCoulomb ? 5.0 : 0.1));
	}

	/**
	 * report the kinetic energy of all objects in the system, including atoms and obstacles, and average it to the atom
	 * basis.
	 * 
	 * @return average kinetic energy per atom
	 */
	public synchronized double getKin() {
		eKT = 0.0;
		for (int i = 0; i < numberOfAtoms; i++) {
			eKT += (atom[i].vx * atom[i].vx + atom[i].vy * atom[i].vy) * atom[i].mass;
		}
		if (obstacles != null && !obstacles.isEmpty()) {
			RectangularObstacle obs = null;
			synchronized (obstacles.getSynchronizationLock()) {
				for (int jobs = 0, nobs = obstacles.size(); jobs < nobs; jobs++) {
					obs = obstacles.get(jobs);
					if (obs.isPartOfSystem() && obs.isMovable())
						eKT += (obs.vx * obs.vx + obs.vy * obs.vy) * obs.getMass();
				}
			}
		}
		eKT *= EV_CONVERTER;
		// the prefactor 0.5 doesn't show up here because it has been included in the mass unit conversion factor.
		kin = numberOfAtoms > 0 ? eKT / numberOfAtoms : eKT;
		return kin;
	}

	/**
	 * @return the kinetic energy of the specified type of atoms.
	 * @see org.concord.mw2d.models.Element
	 */
	public double getKinForType(byte element) {
		if (typeList == null) {
			typeList = new ArrayList<Atom>();
		}
		else {
			typeList.clear();
		}
		for (int i = 0; i < numberOfAtoms; i++) {
			if (atom[i].id == element)
				typeList.add(atom[i]);
		}
		return getKinForParticles(typeList);
	}

	/**
	 * return the kinetic energy of the atom list, if there is no atom in the list, return 0.
	 */
	public double getKinForParticles(List list) {
		if (list == null || list.isEmpty())
			return 0.0;
		double x = 0.0;
		Object o = null;
		Atom a = null;
		int n = 0;
		synchronized (list) {
			for (Iterator it = list.iterator(); it.hasNext();) {
				o = it.next();
				if (o instanceof Atom) {
					a = (Atom) o;
					x += (a.vx * a.vx + a.vy * a.vy) * a.mass;
					n++;
				}
			}
		}
		x *= EV_CONVERTER;
		// the prefactor 0.5 doesn't show up here because of mass unit conversion.
		return n > 0 ? x / n : x;
	}

	/** <tt>setUpdateList(true)</tt> forces updating neighbor list EVERY step. */
	public void setUpdateList(boolean value) {
		updateList = value;
	}

	/** this method being public is a side effect of interface implementation */
	public void parameterChanged(ParameterChangeEvent e) {

		Object source = e.getSource();

		if (source instanceof Element) {
			int elementID = ((Element) source).getID();
			int eventType = e.getType();
			if (eventType == Element.ALL_CHANGED || eventType == Element.SIGMA_CHANGED) {
				setCutOffMatrix(rCutOff);
				setListMatrix(rList);
			}
			for (int i = 0; i < numberOfAtoms; i++) {
				if (atom[i].id == elementID)
					atom[i].setElement(getElement(elementID));
			}
			if (view.getUseJmol())
				view.refreshJmol();
			updateParArray = true;
		}
		else if (source instanceof Affinity) {
			updateParArray = true;
		}
		else {

			String name = e.getParameterName();
			if (name.equals("Excited state removed")) {
				Element elem = (Element) e.getNewValue();
				EnergyLevel level = (EnergyLevel) e.getOldValue();
				for (int i = 0; i < numberOfAtoms; i++) {
					if (atom[i].id == elem.getID()) {
						if (atom[i].getElectron(0).getEnergyLevel() == level) {
							atom[i].resetElectronsToGroundState();
						}
					}
				}
				notifyChange();
				view.repaint();
			}
			else if (name.equals("All excited states removed")) {
				Element elem = (Element) e.getNewValue();
				for (int i = 0; i < numberOfAtoms; i++) {
					if (atom[i].id == elem.getID())
						atom[i].resetElectronsToGroundState();
				}
				notifyChange();
				view.repaint();
			}
			else if (name.equals("Excited state moved") || name.equals("Excited state inserted")
					|| name.equals("Lifetime changed")) {
				notifyChange();
			}

		}

	}

	/** destroy this model to prevent memory leak. */
	public void destroy() {
		super.destroy();
		for (Atom a : atom) {
			a.initializeMovieQ(-1);
			a.initializeRadicalQ(-1);
			a.initializeExcitationQ(-1);
			a.setModel(null);
			a = null;
		}
		electronicDynamics = null;
		updateGrid = null;
	}

	public void clear() {
		super.clear();
		for (Atom a : atom) {
			a.setMovable(true);
			a.setVisible(true);
			a.setCharge(0.0);
			a.setRestraint(null);
			a.setUserField(null);
			a.setShowRTraj(false);
			a.setShowRMean(false);
			a.setShowFMean(false);
			a.clearMeasurements();
			a.setFriction(0);
			a.setRadical(true);
			a.setColor(null);
			a.setMarked(false);
			a.setSelected(false);
			a.setVelocitySelection(false);
			a.resetElectronsToGroundState();
		}
		if (photonList != null)
			photonList.clear();
		if (photonQueue != null)
			photonQueue.clear();
		setNumberOfAtoms(0);
		resetElements();
		resetIntegrator();
		setPhotonEnabled(false);
		setLightSourceEnabled(false);
		setAtomFlowEnabled(false);
	}

	public String toString() {
		return "<Atomic Model> " + getProperty("filename");
	}

	/*
	 * this method initializes the working arrays to the given capacity. These working arrays are: <p> <ul> <li>The
	 * atom array; <li>The van der Waals parameter array; <li>The neighbor list array; <li>The neighbor list pointer
	 * array; <li>The x,y coordinates at last step; <li>The x,y displacements since last step; <li>If the integration
	 * order is higher, higher derivatives arrays. </ul> </p>
	 * 
	 * @param n the capacity of the working arrays
	 */
	private int init(short n) {
		atom = new Atom[n];
		for (short i = 0; i < n; i++) {
			atom[i] = new Atom(nt);
			atom[i].setIndex(i);
			atom[i].setModel(this);
		}
		bondTable = new boolean[n][n];
		epsab = new float[n][n];
		sigab = new float[n][n];
		neighborList = new int[n * n / 2];
		pointer = new int[n];
		rx0 = new double[n];
		ry0 = new double[n];
		return n;
	}

	/*
	 * predict the system conformation at the next time step according to Taylor's expansion. We prefer Gear's
	 * predictor-corrector method. The Verlet method is not used, because of the following reasons: <ol> <li> Velocities
	 * do not enter the propogation, therefore, velocities cannot be changed by the user. <li> Reflecting boundary
	 * conditions cannot be applied with the Verlet method. </ol> The improved version of the Verlet method, the
	 * so-called velocity Verlet method is equivalent to the 3rd order Gear method.
	 */
	synchronized void predictor() {
		if (numberOfAtoms == 1) {
			atom[0].predict(timeStep, timeStep2);
		}
		else if (numberOfAtoms > 1) {
			for (int i = 0; i < numberOfAtoms; i++) {
				atom[i].predict(timeStep, timeStep2);
			}
		}
		putInBounds();
	}

	/* correct the prediction according to the most recently computed forces */
	synchronized void corrector() {
		if (numberOfAtoms == 1) {
			atom[0].ax = atom[0].fx;
			atom[0].ay = atom[0].fy;
			atom[0].fx *= atom[0].mass;
			atom[0].fy *= atom[0].mass;
		}
		else {
			double halfTimeStep = timeStep * 0.5;
			for (int i = 0; i < numberOfAtoms; i++)
				atom[i].correct(halfTimeStep);
		}
		if (obstacles != null && obstacles.size() > 0) {
			obstacles.collide(numberOfAtoms, atom);
			obstacles.move(timeStep, timeStep2, numberOfAtoms, atom);
		}
		int n = view.getNumberOfInstances(LineComponent.class);
		if (n > 0) {
			LineComponent[] lines = view.getLines();
			for (LineComponent lc : lines) {
				for (int i = 0; i < numberOfAtoms; i++) {
					lc.reflect(atom[i]);
				}
			}
		}
	}

	/*
	 * determines whether or not the neighbor list table should be updated, based on the result of scanning wether or
	 * not there have been atoms that have drifted out of the buffer zone (skin).
	 */
	void checkNeighborList() {
		double dispmax = 0.0;
		for (int i = 0; i < numberOfAtoms; i++) {
			dispmax = Math.max(dispmax, Math.abs(atom[i].rx - rx0[i]));
			dispmax = Math.max(dispmax, Math.abs(atom[i].ry - ry0[i]));
		}
		dispmax = 2.0 * Math.sqrt(3.0 * dispmax * dispmax);
		updateList = dispmax > rList - rCutOff;
	}

	private void resetElements() {

		// remove ParameterChangeListeners on editable elements to prevent memory leaks
		if (nt != null)
			nt.removeParameterChangeListener(this);
		if (pl != null)
			pl.removeParameterChangeListener(this);
		if (ws != null)
			ws.removeParameterChangeListener(this);
		if (ck != null)
			ck.removeParameterChangeListener(this);
		if (mo != null)
			mo.removeParameterChangeListener(this);
		if (affinity != null)
			affinity.destroy();

		nt = new Element(ID_NT, 20.0 / 120.0, 7.0, 0.1);
		pl = new Element(ID_PL, 40.0 / 120.0, 14.0, 0.1);
		ws = new Element(ID_WS, 0.5, 21.0, 0.1);
		ck = new Element(ID_CK, 80.0 / 120.0, 28.0, 0.1);
		mo = new Element(ID_MO, 5.0, 12.0, 0.1);
		sp = new Element(ID_SP, 5.0, 16.0, 0.01);
		for (byte i = ID_ALA; i <= ID_VAL; i++) {
			if (aminoAcidElement[i - ID_ALA] == null) {
				Aminoacid aa = AminoAcidAdapter.getAminoAcid(i);
				aminoAcidElement[i - ID_ALA] = new Element(i, ((Float) aa.getProperty("mass")).floatValue(),
						((Double) aa.getProperty("sigma")).doubleValue(), 0.1);
			}
		}
		for (int i = ID_A; i <= ID_U; i++) {
			nucleotideElement[i - ID_A] = new Element(i, 5.0, 12.0, 0.01);
		}

		nt.addParameterChangeListener(this);
		pl.addParameterChangeListener(this);
		ws.addParameterChangeListener(this);
		ck.addParameterChangeListener(this);
		mo.addParameterChangeListener(this);

		affinity = new Affinity(new Element[] { nt, pl, ws, ck, mo });
		affinity.addParameterChangeListener(this);
		setCutOffMatrix(2.0f);
		setListMatrix(2.5f);

	}

	private void resetParArray() {
		if (numberOfAtoms > 1) {
			for (int i = 0; i < numberOfAtoms - 1; i++) {
				for (int j = i + 1; j < numberOfAtoms; j++) {
					sigab[j][i] = sigab[i][j] = (float) multiplySigmaFor(atom[i].id, atom[j].id);
					epsab[j][i] = epsab[i][j] = 2.0f * (float) plusEpsilonFor(atom[i].id, atom[j].id);
				}
			}
		}
		for (int i = 0; i < numberOfAtoms; i++) {
			sigab[i][i] = (float) multiplySigmaFor(atom[i].id, atom[i].id);
			epsab[i][i] = 2.0f * (float) plusEpsilonFor(atom[i].id, atom[i].id);
		}
	}

	private double multiplySigmaFor(int idOfI, int idOfJ) {

		/* same element */
		if (idOfI == idOfJ)
			return getElement(idOfI).getSigma() * getElement(idOfJ).getSigma();

		/* others */
		if (affinity.isLBMixed(getElement(idOfI), getElement(idOfJ)))
			return getElement(idOfI).getSigma() * getElement(idOfJ).getSigma();

		double x = affinity.getSigma(getElement(idOfI), getElement(idOfJ));
		return x * x;

	}

	// unused
	double plusSigmaFor(int idOfI, int idOfJ) {

		/* same element */
		if (idOfI == idOfJ)
			return getElement(idOfI).getSigma() + getElement(idOfJ).getSigma();

		/* others */
		if (affinity.isLBMixed(getElement(idOfI), getElement(idOfJ)))
			return getElement(idOfI).getSigma() + getElement(idOfJ).getSigma();

		double x = affinity.getSigma(getElement(idOfI), getElement(idOfJ));
		return x + x;

	}

	private double plusEpsilonFor(int idOfI, int idOfJ) {

		/* same element */
		if (idOfI == idOfJ)
			return getElement(idOfI).getEpsilon() + getElement(idOfJ).getEpsilon();

		/* disulfide bonds */
		if ((idOfI == ID_CYS || idOfI == ID_MET) && (idOfJ == ID_CYS || idOfJ == ID_MET))
			return 10.0;

		/* A-T complement */
		if ((idOfI == ID_A && idOfJ == ID_T) || (idOfI == ID_T && idOfJ == ID_A))
			return 2.0f * hbStrength;

		/* C-G complement */
		if ((idOfI == ID_C && idOfJ == ID_G) || (idOfI == ID_G && idOfJ == ID_C))
			return 3.0f * hbStrength;

		/* A-U complement */
		if ((idOfI == ID_A && idOfJ == ID_U) || (idOfI == ID_U && idOfJ == ID_A))
			return 2.0f * hbStrength;

		/* others */
		if (affinity.isLBMixed(getElement(idOfI), getElement(idOfJ)))
			return getElement(idOfI).getEpsilon() + getElement(idOfJ).getEpsilon();

		return 2.0 * affinity.getEpsilon(getElement(idOfI), getElement(idOfJ));

	}

	// Unused
	double multiplyEpsilonFor(int idOfI, int idOfJ) {

		/* same element */
		if (idOfI == idOfJ)
			return getElement(idOfI).getEpsilon() * getElement(idOfJ).getEpsilon();

		/* disulfide bonds */
		if ((idOfI == ID_CYS || idOfI == ID_MET) && (idOfJ == ID_CYS || idOfJ == ID_MET))
			return 100.0;

		/* A-T complement */
		if ((idOfI == ID_A && idOfJ == ID_T) || (idOfI == ID_T && idOfJ == ID_A))
			return 4.0f * hbStrength * hbStrength;

		/* C-G complement */
		if ((idOfI == ID_C && idOfJ == ID_G) || (idOfI == ID_G && idOfJ == ID_C))
			return 9.0f * hbStrength * hbStrength;

		/* A-U complement */
		if ((idOfI == ID_A && idOfJ == ID_U) || (idOfI == ID_U && idOfJ == ID_A))
			return 4.0f * hbStrength * hbStrength;

		/* others */
		if (affinity.isLBMixed(getElement(idOfI), getElement(idOfJ)))
			return getElement(idOfI).getEpsilon() * getElement(idOfJ).getEpsilon();

		double x = affinity.getEpsilon(getElement(idOfI), getElement(idOfJ));
		return x * x;

	}

	private boolean isRepulsive(int idOfI, int idOfJ) {

		/* special treatment of same nucleotides */
		if ((idOfI == ID_A && idOfJ == ID_A) || (idOfI == ID_C && idOfJ == ID_C) || (idOfI == ID_G && idOfJ == ID_G)
				|| (idOfI == ID_T && idOfJ == ID_T) || (idOfI == ID_U && idOfJ == ID_U)
				|| (idOfI == ID_SP && idOfJ == ID_SP))
			return true;

		/* same element other than nucleotides */
		if (idOfI == idOfJ)
			return false;

		/* A-C */
		if ((idOfI == ID_A && idOfJ == ID_C) || (idOfI == ID_C && idOfJ == ID_A))
			return true;

		/* A-G */
		if ((idOfI == ID_A && idOfJ == ID_G) || (idOfI == ID_G && idOfJ == ID_A))
			return true;

		/* C-T */
		if ((idOfI == ID_C && idOfJ == ID_T) || (idOfI == ID_T && idOfJ == ID_C))
			return true;

		/* C-U */
		if ((idOfI == ID_C && idOfJ == ID_U) || (idOfI == ID_U && idOfJ == ID_C))
			return true;

		/* G-T */
		if ((idOfI == ID_G && idOfJ == ID_T) || (idOfI == ID_T && idOfJ == ID_G))
			return true;

		/* G-U */
		if ((idOfI == ID_G && idOfJ == ID_U) || (idOfI == ID_U && idOfJ == ID_G))
			return true;

		/* others */
		return affinity.isRepulsive(getElement(idOfI), getElement(idOfJ));

	}

	void encode(XMLEncoder out) throws Exception {

		// remove dependencies on non-serializable fields to serialize objects
		Component savedAncestor = view.getAncestor();
		view.setAncestor(null);
		for (Atom a : atom) {
			a.setView(null);
			a.setSelected(false);
		}

		AtomisticView.State vs = new AtomisticView.State();
		vs.setRenderingMethod(view.getRenderingMethod());
		vs.setUseJmol(view.getUseJmol());
		vs.setElementColors(view.getElementColors());
		vs.setBackground(view.getBackground());
		vs.setMarkColor(view.getMarkColor().getRGB());
		vs.setDrawCharge(view.getDrawCharge());
		vs.setDrawExternalForce(view.getDrawExternalForce());
		vs.setShowParticleIndex(view.getShowParticleIndex());
		vs.setShowMirrorImages(view.getShowMirrorImages());
		vs.setShowClock(view.getShowClock());
		vs.setColorCode(view.getColorCoding());
		vs.setEnergizer(view.getEnergizer());
		vs.setDisplayStyle(view.getDisplayStyle());
		vs.setRestraintStyle(view.getRestraintStyle());
		vs.setTrajectoryStyle(view.getTrajectoryStyle());
		if (view.getFillMode() != FillMode.getNoFillMode())
			vs.setFillMode(view.getFillMode());
		vs.setShading(view.shadingShown());
		vs.setChargeShading(view.chargeShadingShown());
		vs.setShowVVectors(view.velocityVectorShown());
		vs.setShowPVectors(view.momentumVectorShown());
		vs.setShowAVectors(view.accelerationVectorShown());
		vs.setShowFVectors(view.forceVectorShown());
		vs.setShowContour(view.contourPlotShown());
		vs.setShowVDWCircles(view.vdwCirclesShown());
		vs.setVDWCircleStyle(view.getVDWCircleStyle());
		vs.setShowVDWLines(view.vdwLinesShown());
		vs.setVDWLinesRatio(view.getVDWLinesRatio());
		vs.setShowChargeLines(view.chargeLinesShown());
		vs.setShowSSLines(view.ssLinesShown());
		vs.setShowBPLines(view.bpLinesShown());
		vs.setShowEFieldLines(view.eFieldLinesShown());
		vs.setEFCellSize(view.getCellSizeForEFieldLines());
		vs.setVelocityFlavor(view.getVelocityFlavor());
		vs.setMomentumFlavor(view.getMomentumFlavor());
		vs.setAccelerationFlavor(view.getAccelerationFlavor());
		vs.setForceFlavor(view.getForceFlavor());
		vs.setShowExcitation(view.excitationShown());
		if (vs.getShowContour()) {
			vs.setProbeID(view.getProbeAtom().id);
			vs.setProbeCharge(view.getProbeAtom().getCharge());
		}
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
		Color[] c = getMolecularObjectColors();
		if (c != null)
			vs.setMolecularObjectColors(c);

		State state = new State(numberOfAtoms);
		if (job != null)
			state.addTasks(job.getCustomTasks());
		state.setComputeList(computeList);
		state.setLJBetweenBondPairs(ljBetweenBondPairs);
		state.setInterCoulomb(interCoulomb);
		state.setUniverse(universe);
		state.setProperties(properties);
		state.setFields(fields);
		state.setCutOff(rCutOff);
		state.setCutOffShift(cutOffShift);
		state.setRList(rList);
		state.setTimeStep(timeStep);
		state.setScript(initializationScript);
		state.setFrameInterval(movieUpdater.getInterval());
		state.setMoEpsilon(mo.getEpsilon());
		state.setMoMass(mo.getMass());
		state.setPhotonEnabled(isPhotonEnabled());
		state.setLightSource(lightSource);
		state.setQuantumRule(quantumRule);
		state.setCollisionalDeexcitation(collisionalDeexcitation);
		state.setReminderEnabled(isReminderEnabled());
		if (isReminderEnabled()) {
			state.setRepeatReminder(reminder.getLifetime() == Loadable.ETERNAL);
			state.setReminderInterval(reminder.getInterval());
			state.setReminderMessage(reminderMessage);
		}
		state.setAtomFlowEnabled(isAtomFlowEnabled());
		if (isAtomFlowEnabled()) {
			state.setFlowInSide(getFlowInSide());
			state.setFlowOutSide(getFlowOutSide());
			byte[] type = getFlowInType();
			Byte[] type2 = new Byte[type.length];
			for (byte i = 0; i < type.length; i++)
				type2[i] = new Byte(type[i]);
			state.setFlowInType(type2);
			type = getFlowOutType();
			type2 = new Byte[type.length];
			for (byte i = 0; i < type.length; i++)
				type2[i] = new Byte(type[i]);
			state.setFlowOutType(type2);
			state.setFlowInterval(getFlowInterval());
		}

		if (photonEnabled) {
			// save the excited states of the electrons of the atoms
			ExcitedStates excitedStates = new ExcitedStates();
			for (int i = 0; i < numberOfAtoms; i++) {
				if (atom[i].isExcitable()) {
					List<Electron> electrons = atom[i].getElectrons();
					if (electrons != null && !electrons.isEmpty()) {
						for (Electron e : electrons) {
							int levelIndex = e.getEnergyLevelIndex();
							if (levelIndex == -1)
								continue;
							excitedStates.getExcitationMap().put(i, new int[] { electrons.indexOf(e), levelIndex });
						}
					}
				}
			}
			state.setExcitedStates(excitedStates);
		}

		// write out the boundary
		RectangularBoundary.Delegate rectBound = boundary.createDelegate();
		state.setBoundary(rectBound);

		if (heatBath != null) {
			heatBath.setModel(null);
			state.setHeatBath(heatBath);
		}

		monitor.setProgressMessage("Writing model...");
		monitor.setMaximum(getNumberOfParticles() + 8);

		out.writeObject(nt);
		out.writeObject(pl);
		out.writeObject(ws);
		out.writeObject(ck);
		out.writeObject(affinity);

		if (obstacles != null && !obstacles.isEmpty())
			state.setObstacles(obstacles.getList());
		Object prop = removeProperty("old url"); // hack to remove temporary properties
		out.writeObject(state);
		out.writeObject(vs);
		out.flush();
		if (prop != null)
			putProperty("old url", prop); // add temporary properties back

		for (int i = 0; i < numberOfAtoms; i++) {
			monitor.setProgressMessage("Writing atom " + i + "...");
			out.writeObject(atom[i]);
			out.flush();
		}

		encodeBonds(out);

		// restore non-serializable fields for serialized objects
		view.setAncestor(savedAncestor);
		for (Atom a : atom)
			a.setView(view);
		if (heatBath != null)
			heatBath.setModel(this);

	}

	// skeleton method
	void encodeBonds(XMLEncoder out) {
	}

	// skeleton method
	Color[] getMolecularObjectColors() {
		return null;
	}

	void prepareToRead() {
		super.prepareToRead();
		setupGrid(-1, -1);
		view.showContourPlot(false, null);
	}

	void decode(XMLDecoder in) throws Exception {

		super.decode(in);

		if (nt != null)
			nt.removeParameterChangeListener(this);
		if (pl != null)
			pl.removeParameterChangeListener(this);
		if (ws != null)
			ws.removeParameterChangeListener(this);
		if (ck != null)
			ck.removeParameterChangeListener(this);
		if (mo != null)
			mo.removeParameterChangeListener(this);
		if (affinity != null)
			affinity.destroy();
		if (quantumRule != null)
			quantumRule.reset();

		/* read elements */
		nt = (Element) in.readObject();
		pl = (Element) in.readObject();
		ws = (Element) in.readObject();
		ck = (Element) in.readObject();
		affinity = (Affinity) in.readObject();
		nt.addParameterChangeListener(this);
		pl.addParameterChangeListener(this);
		ws.addParameterChangeListener(this);
		ck.addParameterChangeListener(this);
		mo.addParameterChangeListener(this);
		affinity = new Affinity(new Element[] { nt, pl, ws, ck, mo }, affinity);
		affinity.addParameterChangeListener(this);

		monitor.setProgressMessage("Reading model state...");

		final State state = (State) in.readObject();
		setLJBetweenBondPairs(state.getLJBetweenBondPairs());
		setInterCoulomb(state.getInterCoulomb());
		setUniverse(state.getUniverse() == null ? new Universe() : state.getUniverse());
		setPhotonEnabled(state.getPhotonEnabled());
		setLightSource(state.getLightSource() == null ? new LightSource() : state.getLightSource());
		photonGun.setInterval(lightSource.getRadiationPeriod());
		quantumRule = state.getQuantumRule() != null ? state.getQuantumRule() : new QuantumRule();
		setCollisionalDeexcitation(state.getCollisionalDeexcitation());
		enableReminder(state.getReminderEnabled());
		if (isReminderEnabled()) {
			reminder.setInterval(state.getReminderInterval());
			reminder.setLifetime(state.getRepeatReminder() ? Loadable.ETERNAL : reminder.getInterval());
			reminderMessage = state.getReminderMessage();
		}
		setAtomFlowEnabled(state.isAtomFlowEnabled());
		if (isAtomFlowEnabled()) {
			setFlowInSide(state.getFlowInSide());
			setFlowOutSide(state.getFlowOutSide());
			Byte[] type = state.getFlowInType();
			byte[] type2 = new byte[type.length];
			for (byte i = 0; i < type.length; i++)
				type2[i] = type[i].byteValue();
			setFlowInType(type2);
			type = state.getFlowOutType();
			type2 = new byte[type.length];
			for (byte i = 0; i < type.length; i++)
				type2[i] = type[i].byteValue();
			setFlowOutType(type2);
			setFlowInterval(state.getFlowInterval());
		}
		monitor.setMaximum(2 * state.getNumberOfParticles() + 40);
		setCutOffMatrix(state.getCutOff());
		setListMatrix(state.getRList());
		setCutOffShift(state.getCutOffShift());
		setTimeStep(state.getTimeStep());
		setInitializationScript(state.getScript());
		setLightSimulationSpeed((float) getTimeStep());
		mo.setEpsilon(state.getMoEpsilon());
		mo.setMass(state.getMoMass());
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
		for (int i = 0; i < 4; i++) {
			kep[i].clear();
			msd[i].clear();
		}
		Arrays.fill(channels, 0);
		movieUpdater.setInterval(state.getFrameInterval());
		if (heatBath != null)
			heatBath.destroy();
		heatBath = state.getHeatBath();
		if (heatBath != null)
			heatBath.setModel(this);

		/* restore view */

		monitor.setProgressMessage("Reading view...");
		final AtomisticView.State vs = (AtomisticView.State) in.readObject();
		view.setRenderingMethod(vs.getRenderingMethod());
		view.setUseJmol(vs.getUseJmol() || !vs.getMonochromatic());
		view.setElementColors(vs.getElementColors());
		view.setFillMode(vs.getFillMode());
		view.setBackground(vs.getBackground());
		view.setMarkColor(new Color(vs.getMarkColor()));
		view.setEnergizer(vs.getEnergizer());
		view.setDrawCharge(vs.getDrawCharge());
		view.setDrawExternalForce(vs.getDrawExternalForce());
		view.setShowParticleIndex(vs.getShowParticleIndex());
		view.setShowMirrorImages(vs.getShowMirrorImages());
		view.setColorCoding(vs.getColorCode());
		view.setShowClock(vs.getShowClock());
		view.setShowSites(false);
		view.showVDWCircles(vs.getShowVDWCircles());
		view.setVDWCircleStyle(vs.getVDWCircleStyle());
		view.showVDWLines(vs.getShowVDWLines());
		view.setVDWLinesRatio(vs.getVDWLinesRatio());
		view.showChargeLines(vs.getShowChargeLines());
		view.showSSLines(vs.getShowSSLines());
		view.showBPLines(vs.getShowBPLines());
		view.showEFieldLines(vs.getShowEFieldLines(), vs.getEFCellSize());
		view.showShading(vs.getShading());
		view.showChargeShading(vs.getChargeShading());
		view.showVelocityVector(vs.getShowVVectors());
		view.showMomentumVector(vs.getShowPVectors());
		view.showAccelerationVector(vs.getShowAVectors());
		view.showForceVector(vs.getShowFVectors());
		view.setDisplayStyle(vs.getDisplayStyle());
		view.setRestraintStyle(vs.getRestraintStyle());
		view.setTrajectoryStyle(vs.getTrajectoryStyle());
		view.setVelocityFlavor(vs.getVelocityFlavor());
		view.setMomentumFlavor(vs.getMomentumFlavor());
		view.setAccelerationFlavor(vs.getAccelerationFlavor());
		view.setForceFlavor(vs.getForceFlavor());
		view.showExcitation(vs.getShowExcitation());

		/* restore atom by atom */

		Atom at = null;
		int pointer = 0;
		int nas = state.getNumberOfParticles();
		while (pointer < nas) {
			if (pointer % 10 == 0)
				monitor.setProgressMessage("Reading atom " + pointer + "...");
			at = (Atom) in.readObject();
			atom[pointer].destroy();
			atom[pointer] = at;
			atom[pointer].setIndex(pointer);
			atom[pointer].setView(view);
			atom[pointer].setModel(this);
			atom[pointer].fx = atom[pointer].ax * atom[pointer].mass;
			atom[pointer].fy = atom[pointer].ay * atom[pointer].mass;
			String savedCodon = at.getCodon();
			switch (atom[pointer].id) {
			case ID_NT:
				atom[pointer].setElement(nt);
				break;
			case ID_PL:
				atom[pointer].setElement(pl);
				break;
			case ID_WS:
				atom[pointer].setElement(ws);
				break;
			case ID_CK:
				atom[pointer].setElement(ck);
				break;
			case ID_MO:
				atom[pointer].setElement(mo);
				break;
			case ID_SP:
				atom[pointer].setElement(sp);
				break;
			case ID_ALA:
				atom[pointer].setElement(aminoAcidElement[ID_ALA - ID_ALA]);
				break;
			case ID_ARG:
				atom[pointer].setElement(aminoAcidElement[ID_ARG - ID_ALA]);
				break;
			case ID_ASN:
				atom[pointer].setElement(aminoAcidElement[ID_ASN - ID_ALA]);
				break;
			case ID_ASP:
				atom[pointer].setElement(aminoAcidElement[ID_ASP - ID_ALA]);
				break;
			case ID_CYS:
				atom[pointer].setElement(aminoAcidElement[ID_CYS - ID_ALA]);
				break;
			case ID_GLN:
				atom[pointer].setElement(aminoAcidElement[ID_GLN - ID_ALA]);
				break;
			case ID_GLU:
				atom[pointer].setElement(aminoAcidElement[ID_GLU - ID_ALA]);
				break;
			case ID_GLY:
				atom[pointer].setElement(aminoAcidElement[ID_GLY - ID_ALA]);
				break;
			case ID_HIS:
				atom[pointer].setElement(aminoAcidElement[ID_HIS - ID_ALA]);
				break;
			case ID_ILE:
				atom[pointer].setElement(aminoAcidElement[ID_ILE - ID_ALA]);
				break;
			case ID_LEU:
				atom[pointer].setElement(aminoAcidElement[ID_LEU - ID_ALA]);
				break;
			case ID_LYS:
				atom[pointer].setElement(aminoAcidElement[ID_LYS - ID_ALA]);
				break;
			case ID_MET:
				atom[pointer].setElement(aminoAcidElement[ID_MET - ID_ALA]);
				break;
			case ID_PHE:
				atom[pointer].setElement(aminoAcidElement[ID_PHE - ID_ALA]);
				break;
			case ID_PRO:
				atom[pointer].setElement(aminoAcidElement[ID_PRO - ID_ALA]);
				break;
			case ID_SER:
				atom[pointer].setElement(aminoAcidElement[ID_SER - ID_ALA]);
				break;
			case ID_THR:
				atom[pointer].setElement(aminoAcidElement[ID_THR - ID_ALA]);
				break;
			case ID_TRP:
				atom[pointer].setElement(aminoAcidElement[ID_TRP - ID_ALA]);
				break;
			case ID_TYR:
				atom[pointer].setElement(aminoAcidElement[ID_TYR - ID_ALA]);
				break;
			case ID_VAL:
				atom[pointer].setElement(aminoAcidElement[ID_VAL - ID_ALA]);
				break;
			case ID_A:
				atom[pointer].setElement(nucleotideElement[ID_A - ID_A]);
				break;
			case ID_C:
				atom[pointer].setElement(nucleotideElement[ID_C - ID_A]);
				break;
			case ID_G:
				atom[pointer].setElement(nucleotideElement[ID_G - ID_A]);
				break;
			case ID_T:
				atom[pointer].setElement(nucleotideElement[ID_T - ID_A]);
				break;
			case ID_U:
				atom[pointer].setElement(nucleotideElement[ID_U - ID_A]);
				break;
			}
			if (atom[pointer].isAminoAcid() && savedCodon != null)
				atom[pointer].setCodon(savedCodon);
			if (!atom[pointer].isMovable())
				atom[pointer].vx = atom[pointer].vy = 0;
			pointer++;
		}
		setNumberOfAtoms(pointer);
		checkCharges();

		if (computeList != null)
			computeList.clear();
		else computeList = new ArrayList<String>();
		List<String> compList = state.getComputeList();
		if (compList != null)
			computeList.addAll(compList);

		/* finally restore boundary. This should be placed at last. */

		monitor.setProgressMessage("Retrieving boundary...");
		boundary.constructFromDelegate(state.getBoundary());
		if (state.getFields() != null && !state.getFields().isEmpty()) {
			monitor.setProgressMessage("Retrieving fields...");
			addAllNonLocalFields(state.getFields());
		}
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				view.resize(state.getBoundary().getViewSize(), true);
				EventQueue.invokeLater(new Runnable() {
					// anything that depends on the size must be called in the event queue after view is resized
					public void run() {
						if (vs.getShowContour()) {
							Atom probe = createAtomOfElement(vs.getProbeID());
							probe.setCharge(vs.getProbeCharge());
							view.showContourPlot(true, probe, view.getPreferredSize());
							view.repaint();
						}
						if (view.getUseJmol())
							view.refreshJmol();
					}
				});
			}
		});

		decodeBonds(in, vs);
		loadLayeredComponent(vs);

		ExcitedStates excitedStates = state.getExcitedStates();
		if (excitedStates != null) {
			Map<Integer, int[]> excitationMap = excitedStates.getExcitationMap();
			if (excitationMap != null && !excitationMap.isEmpty()) {
				for (Integer iat : excitationMap.keySet()) {
					int[] excite = excitationMap.get(iat);
					if (excite != null && excite.length >= 2) {
						EnergyLevel level = getElement(atom[iat].id).getElectronicStructure().getEnergyLevel(excite[1]);
						atom[iat].getElectron(excite[0]).setEnergyLevel(level);
					}
				}
			}
		}

		initializeJob();
		addCustomTasks(state.getTasks());
		if (heatBath != null && !job.contains(heatBath))
			job.add(heatBath);
		job.processPendingRequests();
		computeForce(-1); // call to build the neighbor list

	}

	// skeleton method
	void decodeBonds(XMLDecoder in, AtomisticView.State vs) throws Exception {
	}

	/* show the <i>i</i>-th frame of the movie */
	void showMovieFrame(int frame) {
		if (frame < 0 || movie.length() <= 0)
			return;
		if (frame >= movie.length())
			throw new IllegalArgumentException("Frame " + frame + " does not exist");
		view.showFrameOfImages(frame);
		modelTime = modelTimeQueue.getData(frame);
		for (int i = 0; i < numberOfAtoms; i++) {
			Atom a = atom[i];
			a.rx = a.rxryQ.getQueue1().getData(frame);
			a.ry = a.rxryQ.getQueue2().getData(frame);
			a.vx = a.vxvyQ.getQueue1().getData(frame);
			a.vy = a.vxvyQ.getQueue2().getData(frame);
			a.ax = a.axayQ.getQueue1().getData(frame);
			a.ay = a.axayQ.getQueue2().getData(frame);
			a.tx = a.dxdyQ.getQueue1().getData(frame);
			a.ty = a.dxdyQ.getQueue2().getData(frame);
			a.fx = a.ax * a.mass;
			a.fy = a.ay * a.mass;
		}
		if (obstacles != null && !obstacles.isEmpty()) {
			RectangularObstacle obs = null;
			synchronized (obstacles.getSynchronizationLock()) {
				for (Iterator it = obstacles.iterator(); it.hasNext();) {
					obs = (RectangularObstacle) it.next();
					if (obs.isMovable()) {
						if (obs.rxryQ != null && !obs.rxryQ.isEmpty()) {
							obs.setRect(obs.rxryQ.getQueue1().getData(frame), obs.rxryQ.getQueue2().getData(frame), obs
									.getWidth(), obs.getHeight());
						}
						if (obs.vxvyQ != null && !obs.vxvyQ.isEmpty()) {
							obs.vx = obs.vxvyQ.getQueue1().getData(frame);
							obs.vy = obs.vxvyQ.getQueue2().getData(frame);
						}
					}
				}
			}
		}
		if (photonEnabled) {
			for (int i = 0; i < numberOfAtoms; i++) {
				if (atom[i].getElectrons().isEmpty())
					continue;
				atom[i].getElectron(0).setEnergyLevel(
						getElement(atom[i].id).getElectronicStructure().getEnergyLevel(
								atom[i].excitationQ.getData(frame)));
			}
			notifyUpdateListeners(new UpdateEvent(AtomicModel.this, UpdateEvent.VIEW_UPDATED));
		}
		if (photonEnabled || (lightSource != null && lightSource.isOn())) {
			if (photonQueue != null) {
				if (photonList != null)
					photonList.clear();
				Object o = photonQueue.getData(frame);
				if (o instanceof ArrayList) {
					List<Photon.Delegate> list = (ArrayList<Photon.Delegate>) o;
					for (Photon.Delegate pd : list) {
						Photon p = new Photon(pd.getX(), pd.getY(), pd.getOmega());
						p.setModel(AtomicModel.this);
						p.setAngle(pd.getAngle());
						photonList.add(p);
					}
				}
			}
		}
		if (view.getUseJmol())
			view.refreshJmol();
	}

	/** Delegate of state of this model. */
	public static class State extends MDModel.State {

		private boolean ljBetweenBondPairs = true;
		private boolean interCoulomb = true;
		private float cutOff = 2.0f, rList = 2.5f;
		private boolean cutOffShift = true;
		private double moEpsilon = 0.1;
		private double moMass = 5.0;
		private boolean photonEnabled2;
		private boolean collisionalDeexcitation;
		private LightSource lightSource;
		private QuantumRule quantumRule;
		private boolean atomFlowEnabled2;
		private byte flowInSide = Wall.WEST;
		private byte flowOutSide = Wall.EAST;
		private Byte[] flowInType = new Byte[] { ID_NT };
		private Byte[] flowOutType = new Byte[] { ID_NT, ID_PL, ID_WS, ID_CK };
		private int flowInterval = 500;
		private ExcitedStates excitedStates;

		public State() {
			super();
		}

		public State(int n) throws ArrayIndexOutOfBoundsException {
			this();
			this.setNumberOfParticles(n);
		}

		public void setInterCoulomb(boolean b) {
			interCoulomb = b;
		}

		public boolean getInterCoulomb() {
			return interCoulomb;
		}

		public void setLJBetweenBondPairs(boolean b) {
			ljBetweenBondPairs = b;
		}

		public boolean getLJBetweenBondPairs() {
			return ljBetweenBondPairs;
		}

		public void setMoEpsilon(double x) {
			moEpsilon = x;
		}

		public double getMoEpsilon() {
			return moEpsilon;
		}

		public void setMoMass(double x) {
			moMass = x;
		}

		public double getMoMass() {
			return moMass;
		}

		public void setCutOff(float x) {
			cutOff = x;
		}

		public float getCutOff() {
			return cutOff;
		}

		public void setCutOffShift(boolean b) {
			cutOffShift = b;
		}

		public boolean getCutOffShift() {
			return cutOffShift;
		}

		public void setRList(float x) {
			rList = x;
		}

		public float getRList() {
			return rList;
		}

		public void setPhotonEnabled(boolean b) {
			photonEnabled2 = b;
		}

		public boolean getPhotonEnabled() {
			return photonEnabled2;
		}

		public void setCollisionalDeexcitation(boolean b) {
			collisionalDeexcitation = b;
		}

		public boolean getCollisionalDeexcitation() {
			return collisionalDeexcitation;
		}

		public void setLightSource(LightSource s) {
			lightSource = s;
		}

		public LightSource getLightSource() {
			return lightSource;
		}

		public void setQuantumRule(QuantumRule q) {
			quantumRule = q;
		}

		public QuantumRule getQuantumRule() {
			return quantumRule;
		}

		public void setAtomFlowEnabled(boolean b) {
			atomFlowEnabled2 = b;
		}

		public boolean isAtomFlowEnabled() {
			return atomFlowEnabled2;
		}

		public void setFlowInSide(byte b) {
			flowInSide = b;
		}

		public byte getFlowInSide() {
			return flowInSide;
		}

		public void setFlowOutSide(byte b) {
			flowOutSide = b;
		}

		public byte getFlowOutSide() {
			return flowOutSide;
		}

		public void setFlowInType(Byte[] b) {
			flowInType = b;
		}

		public Byte[] getFlowInType() {
			return flowInType;
		}

		public void setFlowOutType(Byte[] b) {
			flowOutType = b;
		}

		public Byte[] getFlowOutType() {
			return flowOutType;
		}

		public void setFlowInterval(int i) {
			flowInterval = i;
		}

		public int getFlowInterval() {
			return flowInterval;
		}

		public void setExcitedStates(ExcitedStates excitedStates) {
			this.excitedStates = excitedStates;
		}

		public ExcitedStates getExcitedStates() {
			return excitedStates;
		}

	}

}