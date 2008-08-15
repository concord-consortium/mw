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

import java.awt.EventQueue;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import javax.swing.Action;
import javax.swing.JOptionPane;
import javax.vecmath.AxisAngle4f;
import javax.vecmath.Matrix3f;
import javax.vecmath.Matrix4f;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

import org.concord.mw3d.MolecularView;
import org.concord.modeler.DisasterHandler;
import org.concord.modeler.SlideMovie;
import org.concord.modeler.event.ScriptExecutionListener;
import org.concord.modeler.event.ScriptListener;
import org.concord.modeler.process.AbstractLoadable;
import org.concord.modeler.process.Job;
import org.concord.modeler.process.Loadable;
import org.concord.modeler.util.FileUtilities;
import org.concord.modeler.util.FloatQueue;
import org.concord.modeler.util.HomoQueueGroup;
import org.myjmol.api.Pair;

/**
 * For physical units used in the molecular dynamics simulations, we assume the following:
 * <ul>
 * <li> Avogadro's constant = 6 x 10<sup>23</sup> per mole.
 * <li> An electron volt (eV) = 1.6 x 10<sup>-19</sup> joules.
 * <li> Boltzmann's constant = 1.38 x 10<sup>-23</sup> joules/kelvin.
 * <li> Planck's constant (h-bar) = 6.583 x 10<sup>-16</sup> eV*s
 * <li> Coulomb's constant * electron charge ^2 = 14.4 eV*angstrom
 * <li> Unit of mass = g/mol (atomic mass unit)
 * <li> Unit of length = angstrom
 * <li> Unit of time = femtosecond = 10<sup>-15</sup> second.
 * <li> Unit of temperature = kelvin.
 * </ul>
 * 
 * @TODO need a way to guard the iAtom pointer
 * 
 * @author Charles Xie
 */

public class MolecularModel {

	public final static short SIZE = 1000;
	final static byte REFLECTING_BOUNDARY = 0;
	final static byte PERIODIC_BOUNDARY = 1;
	private final static byte GENERIC_PARTICLE_TYPES = 4;

	/*
	 * convert m*v*v into eV: ( E-3 / 6E23 ) [kg] x ( E-10 / E-15 )^2 [m^2/s^2] / 1.6E-19 [J] divided by 2 (save the
	 * multiplier prefactor 0.5 for computing kinetic energy)
	 */
	private final static float EV_CONVERTER = 100.0f / (1.6f * 1.2f);

	/* converts electron volt into kelvin */
	private final static float UNIT_EV_OVER_KB = 16000.0f / 1.38f;

	/* converts kelvin into angstrom/femtonsecond */
	private final static float VT_CONVERSION_CONSTANT = 0.00002882f;

	private final static float radiansPerDegree = (float) (2 * Math.PI / 360);

	private static byte jobIndex;

	/* the minimum float that is considered zero for judging changes. */
	final static float ZERO = 1000000 * Float.MIN_VALUE;
	private final static short DEFAULT_MINIMUM_JOB_CYCLE_TIME = 20;

	MolecularView view;
	private Eval3D evalAction;
	private Thread evalThread;
	float modelTime;
	float timeStep = 0.5f;
	Atom[] atom;
	List<Obstacle> obstacles;
	int iAtom;
	boolean coulombicIsOn;
	byte boundaryType = REFLECTING_BOUNDARY;

	private float timeStep2 = timeStep * timeStep * 0.5f;
	private ForceCalculator forceCalculator;
	private HeatBath heatBath;
	private MoleculeImporter moleculeImporter;

	/* Movie queue group for this model */
	HomoQueueGroup movieQueueGroup;

	/* Store the real model time in a queue for reconstructing time series later. */
	FloatQueue modelTimeQueue;

	SlideMovie movie;
	boolean recorderDisabled;

	private volatile boolean stopAtNextRecordingStep;
	private long systemTimeOfLastStepEnd;
	private static short minimumJobCycleTime = DEFAULT_MINIMUM_JOB_CYCLE_TIME;
	private long systemTimeElapsed;
	private Random random;
	boolean initializationScriptToRun;
	private String initializationScript;
	private boolean exclusiveSelection = true;

	private Runnable changeNotifier, runNotifier;

	Job job;

	List<RBond> rBonds;
	List<ABond> aBonds;
	List<TBond> tBonds;
	List<Molecule> molecules;
	GField gField;
	EField eField;
	BField bField;

	private float kin, pot, tot;
	private FloatQueue kine, pote, tote;

	private List<Atom> removedAtomList, keptAtomList;
	private Matrix3f rotationMatrix, tempMatrix1, tempMatrix2, tempMatrix3;
	private Matrix4f translationMatrix, tempMatrix4f;
	private Point3f tempPoint3f;
	private Vector3f tempVector3f;
	private AxisAngle4f axisAngle;
	private Point3f[] minmax;
	private BitSet tempAtomBitSet;

	private List<Atom> bondedAtomList;
	private List<Atom> atomList1, atomList2;

	Map<String, float[]> paramMap = new LinkedHashMap<String, float[]>();

	/* the subtask of painting the view at a given frequency */
	private Loadable paintView = new AbstractLoadable(50) {
		public void execute() {
			systemTimeElapsed = System.currentTimeMillis() - systemTimeOfLastStepEnd;
			if (systemTimeElapsed < minimumJobCycleTime) {
				try {
					Thread.sleep(minimumJobCycleTime - systemTimeElapsed);
				}
				catch (InterruptedException e) {
					e.printStackTrace();
				}
				// System.out.println(systemTimeElapsed+","+minimumJobCycleTime);
			}
			systemTimeOfLastStepEnd = System.currentTimeMillis();
			view.refresh();
			if (movie.getMovieSlider().isShowing())
				movie.getMovieSlider().repaint();
		}

		public int getPriority() {
			return Thread.MIN_PRIORITY;
		}

		public String getName() {
			return "Painting view";
		}

		public int getLifetime() {
			return ETERNAL;
		}

		public String getDescription() {
			return "This task updates the view to create continuous animation for the simulation.\nYou can decrease the interval parameter to smoothen the animation, or\nincrease to speed up the overall simulation.";
		}
	};

	/* the subtask of updating the movie queues */
	Loadable movieUpdater = new AbstractLoadable(200) {
		public void execute() {
			/*
			 * if(isEmpty()) { stopImmediately(); stopAtNextRecordingStep=false; return; }
			 */
			record();
			movie.setCurrentFrameIndex(getTapePointer() - 1);
			if (stopAtNextRecordingStep) {
				stopImmediately();
				stopAtNextRecordingStep = false;
			}
			view.refreshTrajectories();
		}

		public String getName() {
			return "Recording the simulation";
		}

		public int getLifetime() {
			return ETERNAL;
		}

		public String getDescription() {
			return "This task records the simulation.";
		}
	};

	private MolecularModel() {
		try {
			new ParameterReader().read(MolecularModel.class.getResource("resources/elements.dat"), paramMap);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public MolecularModel(int tapeLength) {

		this();

		atom = new Atom[SIZE];
		forceCalculator = new ForceCalculator(this);

		movieQueueGroup = new HomoQueueGroup("Movie");
		movie = new EmbeddedMovie();
		movie.setCapacity(tapeLength);
		modelTimeQueue = new FloatQueue("Time (fs)", movie.getCapacity());
		modelTimeQueue.setInterval(movieUpdater.getInterval());

		kine = new FloatQueue("Kinetic energy per atom", movie.getCapacity());
		kine.setReferenceUpperBound(5);
		kine.setReferenceLowerBound(-5);
		kine.setCoordinateQueue(modelTimeQueue);
		kine.setInterval(movieUpdater.getInterval());
		kine.setPointer(0);
		movieQueueGroup.add(kine);

		pote = new FloatQueue("Potential energy per atom", movie.getCapacity());
		pote.setReferenceUpperBound(5);
		pote.setReferenceLowerBound(-5);
		pote.setCoordinateQueue(modelTimeQueue);
		pote.setInterval(movieUpdater.getInterval());
		pote.setPointer(0);
		movieQueueGroup.add(pote);

		tote = new FloatQueue("Total energy per atom", movie.getCapacity());
		tote.setReferenceUpperBound(5);
		tote.setReferenceLowerBound(-5);
		tote.setCoordinateQueue(modelTimeQueue);
		tote.setInterval(movieUpdater.getInterval());
		tote.setPointer(0);
		movieQueueGroup.add(tote);

		// FIXME: perhaps no need to use CopyOnWriteArrayList. (a) CopyOnWriteArrayList.iterator doesn't support
		// remove(). To use it, the code that uses iterator.remove() needs to be changed. (b) The throughput
		// gain by using CopyOnWriteArrayList may not be a lot since we have only two threads that need to
		// frequently traverse the lists - the view updater and the computational engine. While the latter
		// runs at each step, the former just runs every 50 steps. Hence, the lock contention may not be a
		// severe issue.
		molecules = Collections.synchronizedList(new ArrayList<Molecule>());
		rBonds = Collections.synchronizedList(new ArrayList<RBond>());
		aBonds = Collections.synchronizedList(new ArrayList<ABond>());
		tBonds = Collections.synchronizedList(new ArrayList<TBond>());

	}

	public static byte getGenericParticleTypes() {
		return GENERIC_PARTICLE_TYPES;
	}

	public String getSymbol(int id) {
		for (String s : paramMap.keySet()) {
			float[] x = paramMap.get(s);
			if (Math.round(x[0]) == id)
				return s;
		}
		return null;
	}

	/** set the mass of the specified generic particle */
	public void setElementMass(String symbol, float mass) {
		float[] x = paramMap.get(symbol);
		if (x == null)
			throw new IllegalArgumentException("Cannot find the element: " + symbol);
		x[1] = mass;
	}

	public float getElementMass(String symbol) {
		float[] x = paramMap.get(symbol);
		if (x == null)
			throw new IllegalArgumentException("Cannot find the element: " + symbol);
		return x[1];
	}

	/** set the sigma of the specified generic particle */
	public void setElementSigma(String symbol, float sigma) {
		float[] x = paramMap.get(symbol);
		if (x == null)
			throw new IllegalArgumentException("Cannot find the element: " + symbol);
		x[2] = sigma;
	}

	public float getElementSigma(String symbol) {
		float[] x = paramMap.get(symbol);
		if (x == null)
			throw new IllegalArgumentException("Cannot find the element: " + symbol);
		return x[2];
	}

	/** set the epsilon of the specified generic particle */
	public void setElementEpsilon(String symbol, float epsilon) {
		float[] x = paramMap.get(symbol);
		if (x == null)
			throw new IllegalArgumentException("Cannot find the element: " + symbol);
		x[3] = epsilon;
	}

	public float getElementEpsilon(String symbol) {
		float[] x = paramMap.get(symbol);
		if (x == null)
			throw new IllegalArgumentException("Cannot find the element: " + symbol);
		return x[3];
	}

	public Set getSupportedElements() {
		return paramMap.keySet();
	}

	public void setInitializationScript(String s) {
		initializationScript = s;
	}

	public String getInitializationScript() {
		return initializationScript;
	}

	public void setInitializationScriptToRun(boolean b) {
		initializationScriptToRun = b;
	}

	public boolean isActionScriptRunning() {
		if (evalAction == null)
			return false;
		return !evalAction.isStopped();
	}

	public void addScriptExecutionListener(ScriptExecutionListener l) {
		initEvalAction();
		evalAction.addExecutionListener(l);
	}

	/**
	 * stop the script execution thread.
	 */
	public void haltScriptExecution() {
		if (evalAction != null)
			evalAction.halt();
	}

	public String runScript(String script) {
		initEvalAction();
		evalAction.setNotifySaver(!initializationScriptToRun);
		return runScript2(script);
	}

	public void runMouseScript(int eventType, int x, int y) {
		initEvalAction();
		String s = evalAction.getMouseScript(eventType);
		if (s == null)
			return;
		evalAction.setNotifySaver(false);
		evalAction.setMouseLocation(x, y);
		runScript2(s);
	}

	public void clearMouseScripts() {
		if (evalAction != null)
			evalAction.clearMouseScripts();
	}

	private void initEvalAction() {
		if (evalAction == null)
			evalAction = new Eval3D(this);
	}

	public String runScript2(final String script) {
		evalAction.appendScript(script);
		if (!evalAction.isStopped())
			return null;
		if (evalThread == null) {
			evalThread = new Thread("3D Model Script Runner") {
				public void run() {
					try {
						evalAction.evaluate();
					}
					catch (InterruptedException e) {
					}
				}
			};
			evalThread.setPriority(Thread.NORM_PRIORITY - 1);
			evalThread.setUncaughtExceptionHandler(new DisasterHandler(DisasterHandler.SCRIPT_ERROR, new Runnable() {
				public void run() {
					evalThread = null;
					evalAction.clearScriptQueue();
					evalAction.halt();
				}
			}, null, getView()));
			evalThread.start();
		}
		else {
			// just in case there is a missed notification problem
			try {
				Thread.sleep(100);
			}
			catch (InterruptedException e) {
			}
			synchronized (evalAction) {
				evalAction.notifyAll();
			}
		}
		getView().repaint();
		return null;
	}

	public void addScriptListener(ScriptListener listener) {
		if (evalAction == null)
			evalAction = new Eval3D(this);
		evalAction.addScriptListener(listener);
	}

	public void removeScriptListener(ScriptListener listener) {
		if (evalAction == null)
			return;
		evalAction.removeScriptListener(listener);
	}

	public void setChangeNotifier(Runnable notifier) {
		changeNotifier = notifier;
	}

	public void notifyChange() {
		if (changeNotifier != null)
			changeNotifier.run();
	}

	public void setRunNotifier(Runnable notifier) {
		runNotifier = notifier;
	}

	void notifyRun() {
		if (runNotifier != null)
			runNotifier.run();
	}

	/** return true if the molecule has been successfully imported. */
	public boolean importMolecule(int id, Point3f position) {
		if (moleculeImporter == null)
			moleculeImporter = new MoleculeImporter(this);
		int n0 = iAtom;
		String s = moleculeImporter.read(id, position);
		if (s != null && iAtom - n0 > 1) {
			Molecule mol = new Molecule();
			for (int i = n0; i < iAtom; i++) {
				mol.addAtom(atom[i]);
			}
			molecules.add(mol);
		}
		return s != null;
	}

	public void addMolecule(Molecule m) {
		molecules.add(m);
	}

	public void removeMolecule(Molecule m) {
		molecules.remove(m);
	}

	public void formMolecules() {
		if (rBonds.isEmpty()) {
			molecules.clear();
			return;
		}
		if (bondedAtomList == null) {
			bondedAtomList = new ArrayList<Atom>();
		}
		else {
			bondedAtomList.clear();
		}
		for (int i = 0; i < iAtom; i++) {
			if (atom[i].isBonded())
				bondedAtomList.add(atom[i]);
		}
		molecules.clear();
		if (bondedAtomList.isEmpty())
			return;
		if (atomList1 == null) {
			atomList1 = new ArrayList<Atom>();
			atomList2 = new ArrayList<Atom>();
		}
		Atom at1, at2;
		do {
			at1 = bondedAtomList.get(0); // get the 1st of the remaining atoms
			atomList1.clear();
			atomList1.add(at1);
			bondedAtomList.remove(at1);
			do {
				atomList2.clear();
				for (Iterator it1 = atomList1.iterator(); it1.hasNext();) {
					at1 = (Atom) it1.next();
					for (Iterator it2 = bondedAtomList.iterator(); it2.hasNext();) {
						at2 = (Atom) it2.next();
						if (at1.isBonded(at2)) {
							it2.remove();
							atomList2.add(at2);
						}
					}
				}
				atomList1.addAll(atomList2);
			} while (!atomList2.isEmpty());
			molecules.add(new Molecule(atomList1));
		} while (!bondedAtomList.isEmpty());
	}

	public void setExclusiveSelection(boolean b) {
		exclusiveSelection = b;
	}

	public boolean getExclusiveSelection() {
		return exclusiveSelection;
	}

	/** select a set of particles according to the instruction BitSet. */
	public void setAtomSelectionSet(BitSet set) {
		view.setAtomSelected(-1);
		if (set != null) {
			setExclusiveSelection(false);
			for (int i = 0; i < iAtom; i++) {
				if (set.get(i))
					view.setAtomSelected(i);
			}
		}
		view.repaint();
	}

	/** return the selected set of particles in BitSet. */
	public BitSet getAtomSelectionSet() {
		BitSet bs = new BitSet(iAtom);
		for (int i = 0; i < iAtom; i++) {
			if (atom[i].isSelected())
				bs.set(i);
		}
		return bs;
	}

	/** select a set of radial bonds according to the instruction BitSet. */
	public void setRBondSelectionSet(BitSet set) {
		view.selectRBond(-1);
		if (set != null) {
			setExclusiveSelection(false);
			for (RBond rb : rBonds) {
				int i = rBonds.indexOf(rb);
				boolean b = set.get(i);
				rb.setSelected(b);
				view.getViewer().setBondSelected(i, b);
			}
		}
		view.repaint();
	}

	/** return the selected set of radial bonds in BitSet. */
	public BitSet getRBondSelectionSet() {
		BitSet bs = new BitSet(rBonds.size());
		for (RBond rb : rBonds) {
			if (rb.isSelected())
				bs.set(rBonds.indexOf(rb));
		}
		return bs;
	}

	/** select a set of angular bonds according to the instruction BitSet. */
	public void setABondSelectionSet(BitSet set) {
		view.selectABond(-1);
		if (set != null) {
			setExclusiveSelection(false);
			for (ABond ab : aBonds) {
				int i = aBonds.indexOf(ab);
				boolean b = set.get(i);
				ab.setSelected(b);
				view.getViewer().setABondSelected(i, b);
			}
		}
		view.repaint();
	}

	/** return the selected set of angular bonds in BitSet. */
	public BitSet getABondSelectionSet() {
		BitSet bs = new BitSet(aBonds.size());
		for (ABond ab : aBonds) {
			if (ab.isSelected())
				bs.set(aBonds.indexOf(ab));
		}
		return bs;
	}

	/** select a set of torsional bonds according to the instruction BitSet. */
	public void setTBondSelectionSet(BitSet set) {
		view.selectTBond(-1);
		if (set != null) {
			setExclusiveSelection(false);
			for (TBond tb : tBonds) {
				int i = tBonds.indexOf(tb);
				boolean b = set.get(i);
				tb.setSelected(b);
				view.getViewer().setTBondSelected(i, b);
			}
		}
		view.repaint();
	}

	/** return the selected set of torsional bonds in BitSet. */
	public BitSet getTBondSelectionSet() {
		BitSet bs = new BitSet(tBonds.size());
		for (TBond tb : tBonds) {
			if (tb.isSelected())
				bs.set(tBonds.indexOf(tb));
		}
		return bs;
	}

	public float getLength() {
		return forceCalculator.xbox * 2;
	}

	public void setLength(float length) {
		forceCalculator.xbox = 0.5f * length;
	}

	public float getWidth() {
		return forceCalculator.ybox * 2;
	}

	public void setWidth(float width) {
		forceCalculator.ybox = 0.5f * width;
	}

	public float getHeight() {
		return forceCalculator.zbox * 2;
	}

	public void setHeight(float height) {
		forceCalculator.zbox = 0.5f * height;
	}

	/** @return the minimum distance between the 1/2 of VDW surface of the selected and that of the unselected. */
	public float getMinimumDistance(BitSet bs) {
		float min = Float.MAX_VALUE;
		boolean bi = false;
		boolean bj = false;
		float xij, yij, zij, rij;
		int imin = -1, jmin = -1;
		for (int i = 0; i < iAtom; i++) {
			bi = bs.get(i);
			for (int j = i; j < iAtom; j++) {
				bj = bs.get(j);
				if ((bi && bj) || (!bi && !bj))
					continue;
				xij = atom[i].rx - atom[j].rx;
				yij = atom[i].ry - atom[j].ry;
				zij = atom[i].rz - atom[j].rz;
				rij = xij * xij + yij * yij + zij * zij;
				if (min > rij) {
					min = rij;
					imin = i;
					jmin = j;
				}
			}
		}
		if (imin == -1 && jmin == -1)
			return min;
		// allow the VDW surfaces to overlap at 50% size
		return (float) Math.sqrt(min) - 0.25f * (atom[imin].sigma + atom[jmin].sigma);
	}

	public float getAbsXmax() {
		if (iAtom <= 0)
			return 50;
		float max = 0;
		float val;
		for (int i = 0; i < iAtom; i++) {
			if (atom[i].rx > 0) {
				val = Math.abs(atom[i].rx + atom[i].sigma);
			}
			else {
				val = Math.abs(atom[i].rx - atom[i].sigma);
			}
			if (max < val)
				max = val;
		}
		return max;
	}

	public float getAbsYmax() {
		if (iAtom <= 0)
			return 25;
		float max = 0;
		float val;
		for (int i = 0; i < iAtom; i++) {
			if (atom[i].ry > 0) {
				val = Math.abs(atom[i].ry + atom[i].sigma);
			}
			else {
				val = Math.abs(atom[i].ry - atom[i].sigma);
			}
			if (max < val)
				max = val;
		}
		return max;
	}

	public float getAbsZmax() {
		if (iAtom <= 0)
			return 25;
		float max = 0;
		float val;
		for (int i = 0; i < iAtom; i++) {
			if (atom[i].rz > 0) {
				val = Math.abs(atom[i].rz + atom[i].sigma);
			}
			else {
				val = Math.abs(atom[i].rz - atom[i].sigma);
			}
			if (max < val)
				max = val;
		}
		return max;
	}

	public Point3f[] getMinMaxCoordinates(BitSet bs) {
		if (iAtom <= 0)
			return null;
		if (bs.cardinality() <= 0)
			return null;
		if (minmax == null)
			minmax = new Point3f[] { new Point3f(), new Point3f(), new Point3f(), new Point3f() };
		minmax[0].set(Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE);
		minmax[1].set(-Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE);
		Atom a = null;
		for (int i = 0; i < iAtom; i++) {
			if (bs.get(i)) {
				a = atom[i];
				if (minmax[0].x > a.getRx()) {
					minmax[0].x = a.getRx();
					minmax[2].x = i;
				}
				if (minmax[1].x < a.getRx()) {
					minmax[1].x = a.getRx();
					minmax[3].x = i;
				}
				if (minmax[0].y > a.getRy()) {
					minmax[0].y = a.getRy();
					minmax[2].y = i;
				}
				if (minmax[1].y < a.getRy()) {
					minmax[1].y = a.getRy();
					minmax[3].y = i;
				}
				if (minmax[0].z > a.getRz()) {
					minmax[0].z = a.getRz();
					minmax[2].z = i;
				}
				if (minmax[1].z < a.getRz()) {
					minmax[1].z = a.getRz();
					minmax[3].z = i;
				}
			}
		}
		return minmax;
	}

	public Point3f getCenterOfAtoms() {
		Point3f p = new Point3f();
		if (iAtom <= 0)
			return p;
		for (int i = 0; i < iAtom; i++) {
			p.x += atom[i].rx;
			p.y += atom[i].ry;
			p.z += atom[i].rz;
		}
		float inverse = 1.0f / iAtom;
		p.x *= inverse;
		p.y *= inverse;
		p.z *= inverse;
		return p;
	}

	public void translateAtomsTo(Point3f p) {
		if (iAtom <= 0)
			return;
		Point3f center = getCenterOfAtoms();
		for (int i = 0; i < iAtom; i++) {
			atom[i].rx += p.x - center.x;
			atom[i].ry += p.y - center.y;
			atom[i].rz += p.z - center.z;
		}
	}

	private Point3f getCenterOfAtoms(int m, int n) {
		Point3f p = new Point3f();
		if (iAtom <= 0 || m >= n)
			return p;
		for (int i = m; i < n; i++) {
			p.x += atom[i].rx;
			p.y += atom[i].ry;
			p.z += atom[i].rz;
		}
		float inverse = 1.0f / (n - m);
		p.x *= inverse;
		p.y *= inverse;
		p.z *= inverse;
		return p;
	}

	/** translate atoms from index i (inclusive) to index j (exclusive) so that their center is at the specified point. */
	public boolean translateAtomsTo(int i, int j, Point3f p) {
		if (i < 0 || j < 0 || i >= iAtom || j > iAtom)
			return false;
		if (tempAtomBitSet == null)
			tempAtomBitSet = new BitSet(SIZE);
		Point3f center = getCenterOfAtoms(i, j);
		for (int k = i; k < j; k++) {
			atom[k].rx += p.x - center.x;
			atom[k].ry += p.y - center.y;
			atom[k].rz += p.z - center.z;
			tempAtomBitSet.set(k);
		}
		if (outOfSimulationBox(tempAtomBitSet) || overlapWithObstacles(tempAtomBitSet)
				|| getMinimumDistance(tempAtomBitSet) < 0) {
			removeAtoms(tempAtomBitSet);
			return false;
		}
		formMolecules();
		return true;
	}

	public void translateSelectedAtomsTo(BitSet bs, Point3f p) {
		if (iAtom <= 0 || bs.cardinality() <= 0)
			return;
		Point3f center = getCenterOfSelectedAtoms(bs);
		for (int i = 0; i < iAtom; i++) {
			if (bs.get(i)) {
				atom[i].rx += p.x - center.x;
				atom[i].ry += p.y - center.y;
				atom[i].rz += p.z - center.z;
			}
		}
	}

	public boolean atomsOverlapCuboid(float x, float y, float z, float a, float b, float c) {
		if (iAtom <= 0)
			return false;
		for (int i = 0; i < iAtom; i++) {
			if (atom[i].overlapCuboid(x, y, z, a, b, c))
				return true;
		}
		return false;
	}

	public boolean atomsOverlapCylinder(float x, float y, float z, char axis, float h, float r) {
		if (iAtom <= 0)
			return false;
		for (int i = 0; i < iAtom; i++) {
			if (atom[i].overlapCylinder(x, y, z, axis, h, r))
				return true;
		}
		return false;
	}

	public boolean overlapWithObstacles(BitSet bs) {
		if (obstacles == null || obstacles.isEmpty())
			return false;
		int length = bs.length();
		if (length <= 0)
			return false;
		for (Obstacle obs : obstacles) {
			for (int i = 0; i < length; i++) {
				if (bs.get(i) && obs.contains(atom[i]))
					return true;
			}
		}
		return false;
	}

	public boolean outOfSimulationBox(BitSet bs) {
		Point3f[] p = getMinMaxCoordinates(bs);
		if (p == null)
			return false;
		if (p[0].x <= 0.5f * (atom[Math.round(p[2].x)].sigma - getLength()))
			return true;
		if (p[1].x >= 0.5f * (getLength() - atom[Math.round(p[3].x)].sigma))
			return true;
		if (p[0].y <= 0.5f * (atom[Math.round(p[2].y)].sigma - getWidth()))
			return true;
		if (p[1].y >= 0.5f * (getWidth() - atom[Math.round(p[3].y)].sigma))
			return true;
		if (p[0].z <= 0.5f * (atom[Math.round(p[2].z)].sigma - getHeight()))
			return true;
		if (p[1].z >= 0.5f * (getHeight() - atom[Math.round(p[3].z)].sigma))
			return true;
		return false;
	}

	public boolean contains(Point3f p) {
		if (p == null)
			return false;
		if (Math.abs(p.x) > forceCalculator.xbox)
			return false;
		if (Math.abs(p.y) > forceCalculator.ybox)
			return false;
		if (Math.abs(p.z) > forceCalculator.zbox)
			return false;
		return true;
	}

	public Point3f getCenterOfSelectedAtoms(BitSet bs) {
		Point3f p = new Point3f();
		if (iAtom <= 0 || bs.cardinality() <= 0)
			return p;
		int n = 0;
		for (int i = 0; i < iAtom; i++) {
			if (bs.get(i)) {
				p.x += atom[i].rx;
				p.y += atom[i].ry;
				p.z += atom[i].rz;
				n++;
			}
		}
		float inverse = 1.0f / n;
		p.x *= inverse;
		p.y *= inverse;
		p.z *= inverse;
		return p;
	}

	/**
	 * pass the current rotational transformation matrix from the viewer so that the translation can coordinate with the
	 * user's operations.
	 */
	public void translateSelectedAtomsXYBy(Matrix3f transform, BitSet bs, float dx, float dy) {
		if (translationMatrix == null)
			translationMatrix = new Matrix4f();
		if (tempMatrix1 == null)
			tempMatrix1 = new Matrix3f();
		if (tempMatrix4f == null)
			tempMatrix4f = new Matrix4f();
		if (tempPoint3f == null)
			tempPoint3f = new Point3f();
		if (tempVector3f == null)
			tempVector3f = new Vector3f();
		// *watch* the order of matrix multiplications: first transform the atomic coordinates into the current
		// view space, and then apply translation, and then transform back to the model space.
		tempMatrix4f.setIdentity();
		tempMatrix1.invert(transform);
		tempMatrix4f.setRotation(tempMatrix1);
		translationMatrix.set(tempMatrix4f);
		tempMatrix4f.setIdentity();
		tempVector3f.set(dx * 0.05f, -dy * 0.05f, 0);
		tempMatrix4f.setTranslation(tempVector3f);
		translationMatrix.mul(tempMatrix4f);
		tempMatrix4f.setRotation(transform);
		tempVector3f.set(0, 0, 0);
		tempMatrix4f.setTranslation(tempVector3f);
		translationMatrix.mul(tempMatrix4f);
		for (int i = 0; i < iAtom; i++) {
			if (bs.get(i)) {
				tempPoint3f.x = atom[i].rx;
				tempPoint3f.y = atom[i].ry;
				tempPoint3f.z = atom[i].rz;
				translationMatrix.transform(tempPoint3f);
				atom[i].setLocation(tempPoint3f);
			}
		}
	}

	/**
	 * pass the current rotational transformation matrix from the viewer so that the rotation can coordinate with the
	 * user's operations.
	 */
	public void rotateSelectedAtomsXYBy(Matrix3f transform, BitSet bs, float dx, float dy) {
		if (rotationMatrix == null)
			rotationMatrix = new Matrix3f();
		if (tempMatrix1 == null)
			tempMatrix1 = new Matrix3f();
		if (tempMatrix2 == null)
			tempMatrix2 = new Matrix3f();
		if (tempMatrix3 == null)
			tempMatrix3 = new Matrix3f();
		if (tempPoint3f == null)
			tempPoint3f = new Point3f();
		Point3f center = getCenterOfSelectedAtoms(bs);
		// *watch* the order of matrix multiplications: first transform the atomic coordinates into the current
		// view space, and then apply rotX and rotY, and then transform back to the model space.
		tempMatrix3.invert(transform);
		tempMatrix1.rotX(dy * radiansPerDegree);
		tempMatrix2.rotY(dx * radiansPerDegree);
		rotationMatrix.set(tempMatrix3);
		rotationMatrix.mul(tempMatrix1);
		rotationMatrix.mul(tempMatrix2);
		rotationMatrix.mul(transform);
		for (int i = 0; i < iAtom; i++) {
			if (bs.get(i)) {
				tempPoint3f.x = atom[i].rx;
				tempPoint3f.y = atom[i].ry;
				tempPoint3f.z = atom[i].rz;
				rotationMatrix.transform(tempPoint3f);
				atom[i].setLocation(tempPoint3f);
			}
		}
		translateSelectedAtomsTo(bs, center);
	}

	public void rotateSelectedAtoms(BitSet bs, char axis, float angle) {
		if (rotationMatrix == null)
			rotationMatrix = new Matrix3f();
		if (tempPoint3f == null)
			tempPoint3f = new Point3f();
		if (axisAngle == null)
			axisAngle = new AxisAngle4f();
		Point3f center = getCenterOfSelectedAtoms(bs);
		switch (axis) {
		case 'x':
			axisAngle.set(1, 0, 0, angle);
			break;
		case 'y':
			axisAngle.set(0, 1, 0, angle);
			break;
		default:
			axisAngle.set(0, 0, 1, angle);
		}
		rotationMatrix.set(axisAngle);
		for (int i = 0; i < iAtom; i++) {
			if (bs.get(i)) {
				tempPoint3f.x = atom[i].rx;
				tempPoint3f.y = atom[i].ry;
				tempPoint3f.z = atom[i].rz;
				rotationMatrix.transform(tempPoint3f);
				atom[i].setLocation(tempPoint3f);
			}
		}
		translateSelectedAtomsTo(bs, center);
	}

	public void activateHeatBath(boolean b) {
		if (b) {
			if (heatBath == null)
				heatBath = new HeatBath(this);
			if (job == null)
				initializeJob();
			if (job.isStopped()) {
				if (!job.contains(heatBath))
					job.add(heatBath);
			}
			else {
				if (!job.toBeAdded(heatBath) && !job.contains(heatBath))
					job.add(heatBath);
			}
		}
		else {
			if (heatBath != null && job != null) {
				job.remove(heatBath);
				heatBath.setCompleted(true);
				heatBath = null;
			}
		}
		if (job != null && job.isStopped())
			job.processPendingRequests();
	}

	public boolean heatBathActivated() {
		if (job == null || heatBath == null)
			return false;
		if (job.isStopped())
			return job.contains(heatBath);
		if (job.toBeAdded(heatBath))
			return true;
		if (job.toBeRemoved(heatBath))
			return false;
		return job.contains(heatBath);
	}

	public HeatBath getHeatBath() {
		return heatBath;
	}

	public Pair[] getVdwPairs() {
		return forceCalculator.generateVdwPairs();
	}

	/** Clienst must guard their code against multi-threading hazards when acquiring this collection. */
	public List<RBond> getRBonds() {
		return rBonds;
	}

	/** Clienst must guard their code against multi-threading hazards when acquiring this collection. */
	public List<ABond> getABonds() {
		return aBonds;
	}

	/** Clienst must guard their code against multi-threading hazards when acquiring this collection. */
	public List<TBond> getTBonds() {
		return tBonds;
	}

	public int getMaxAtom() {
		return atom.length;
	}

	public int getAtomCount() {
		return iAtom;
	}

	public boolean isEmpty() {
		return iAtom < 1 && (obstacles == null || obstacles.isEmpty());
	}

	public int getMoleculeCount() {
		return molecules.size();
	}

	public int getRBondCount() {
		return rBonds.size();
	}

	public int getABondCount() {
		return aBonds.size();
	}

	public int getTBondCount() {
		return tBonds.size();
	}

	public int getMoleculeIndex(Molecule mol) {
		return molecules.indexOf(mol);
	}

	public Molecule getMolecule(Atom a) {
		synchronized (molecules) {
			for (Molecule m : molecules) {
				if (m.contains(a))
					return m;
			}
		}
		return null;
	}

	public int getAtomIndex(Atom a) {
		for (int i = 0; i < iAtom; i++) {
			if (a == atom[i])
				return i;
		}
		return -1;
	}

	public Atom getAtom(int index) {
		if (index < 0 || index >= atom.length)
			return null;
		return atom[index];
	}

	public RBond getRBond(int index) {
		synchronized (rBonds) {
			if (index < 0 || index >= rBonds.size())
				return null;
			return rBonds.get(index);
		}
	}

	public boolean addRBond(RBond rbond) {
		synchronized (rBonds) {
			if (rBonds.contains(rbond))
				return false;
			rBonds.add(rbond);
		}
		return true;
	}

	public void removeRBond(RBond rbond) {
		rBonds.remove(rbond);
		rbond.getAtom1().removeRBond(rbond);
		rbond.getAtom2().removeRBond(rbond);
		if (!aBonds.isEmpty()) {
			ABond abond;
			synchronized (aBonds) {
				for (Iterator it = aBonds.iterator(); it.hasNext();) {
					abond = (ABond) it.next();
					if (abond.contains(rbond.getAtom1(), rbond.getAtom2()))
						it.remove();
				}
			}
		}
		if (!tBonds.isEmpty()) {
			TBond tbond;
			synchronized (tBonds) {
				for (Iterator it = tBonds.iterator(); it.hasNext();) {
					tbond = (TBond) it.next();
					if (tbond.contains(rbond.getAtom1(), rbond.getAtom2()))
						it.remove();
				}
			}
		}
	}

	public ABond getABond(int index) {
		synchronized (aBonds) {
			if (index < 0 || index >= aBonds.size())
				return null;
			return aBonds.get(index);
		}
	}

	public boolean addABond(ABond abond) {
		synchronized (aBonds) {
			if (aBonds.contains(abond))
				return false;
			aBonds.add(abond);
		}
		return true;
	}

	public void removeABond(ABond abond) {
		aBonds.remove(abond);
		abond.getAtom1().removeABond(abond);
		abond.getAtom2().removeABond(abond);
		abond.getAtom3().removeABond(abond);
		if (!tBonds.isEmpty()) {
			TBond tbond;
			synchronized (tBonds) {
				for (Iterator it = tBonds.iterator(); it.hasNext();) {
					tbond = (TBond) it.next();
					if (tbond.contains(abond.getAtom1(), abond.getAtom2(), abond.getAtom3()))
						it.remove();
				}
			}
		}
	}

	public TBond getTBond(int index) {
		synchronized (tBonds) {
			if (index < 0 || index >= tBonds.size())
				return null;
			return tBonds.get(index);
		}
	}

	public boolean addTBond(TBond tbond) {
		synchronized (tBonds) {
			if (tBonds.contains(tbond))
				return false;
			tBonds.add(tbond);
		}
		return true;
	}

	public void removeTBond(TBond tbond) {
		tBonds.remove(tbond);
		tbond.getAtom1().removeTBond(tbond);
		tbond.getAtom2().removeTBond(tbond);
		tbond.getAtom3().removeTBond(tbond);
		tbond.getAtom4().removeTBond(tbond);
	}

	/**
	 * Adding an atom of the specified type, velocity and charge to the specified position. Note: This method does not
	 * check if the added atom overlaps with others.
	 */
	public boolean addAtom(String element, float rx, float ry, float rz, float vx, float vy, float vz, float charge) {
		if (iAtom == atom.length)
			return false;
		if (atom[iAtom] == null) {
			atom[iAtom] = new Atom(element, this);
			atom[iAtom].setModel(this);
		}
		else {
			atom[iAtom].setSymbol(element);
		}
		atom[iAtom].index = iAtom;
		atom[iAtom].rx = rx;
		atom[iAtom].ry = ry;
		atom[iAtom].rz = rz;
		atom[iAtom].vx = vx;
		atom[iAtom].vy = vy;
		atom[iAtom].vz = vz;
		atom[iAtom].ax = 0;
		atom[iAtom].ay = 0;
		atom[iAtom].az = 0;
		atom[iAtom].charge = charge;
		atom[iAtom].damp = 0;
		iAtom++;
		return true;
	}

	public void removeAtoms(BitSet bs) {
		if (bs == null)
			return;
		int m = iAtom - bs.cardinality();
		if (removedAtomList == null)
			removedAtomList = new ArrayList<Atom>(bs.cardinality());
		else removedAtomList.clear();
		if (keptAtomList == null)
			keptAtomList = new ArrayList<Atom>(m);
		else keptAtomList.clear();
		for (int i = 0; i < iAtom; i++) {
			if (bs.get(i)) {
				removedAtomList.add(atom[i]);
			}
			else {
				keptAtomList.add(atom[i]);
			}
		}
		int n = keptAtomList.size();
		for (int i = 0; i < n; i++) {
			atom[i] = keptAtomList.get(i);
			atom[i].index = i;
		}
		m = removedAtomList.size();
		int j;
		for (int i = 0; i < m; i++) {
			j = n + i;
			atom[j] = removedAtomList.get(i);
			atom[j].setMovable(true);
			atom[j].setCharge(0);
			atom[j].clearBondLists();
		}
		iAtom = n;
	}

	public BitSet removeAtom(Atom a) {
		if (a == null)
			return null;
		BitSet bs = new BitSet(iAtom);
		for (int i = 0; i < iAtom; i++) {
			bs.set(i, a == atom[i]);
		}
		removeAtoms(bs);
		return bs;
	}

	public BitSet removeAtom(int index) {
		if (index < 0 || index >= iAtom)
			return null;
		BitSet bs = new BitSet(iAtom);
		bs.set(index);
		removeAtoms(bs);
		return bs;
	}

	public void immobilizeAtoms(BitSet bs) {
		int n = bs.length();
		if (n == 0)
			return;
		for (int i = 0; i < n; i++) {
			if (bs.get(i))
				atom[i].setMovable(false);
		}
	}

	public void mobilizeAtoms(BitSet bs) {
		int n = bs.length();
		if (n == 0)
			return;
		for (int i = 0; i < n; i++) {
			if (bs.get(i))
				atom[i].setMovable(true);
		}
	}

	public int getObstacleCount() {
		if (obstacles == null)
			return 0;
		return obstacles.size();
	}

	public Obstacle getObstacle(int index) {
		if (obstacles == null)
			return null;
		return obstacles.get(index);
	}

	public int indexOfObstacle(Obstacle obs) {
		if (obstacles == null)
			return -1;
		return obstacles.indexOf(obs);
	}

	public void addObstacle(Obstacle obs) {
		if (obstacles == null)
			obstacles = Collections.synchronizedList(new ArrayList<Obstacle>());
		if (obstacles.contains(obs))
			return;
		obs.setModel(this);
		obstacles.add(obs);
	}

	public boolean removeObstacle(Obstacle obs) {
		if (obstacles == null)
			return false;
		return obstacles.remove(obs);
	}

	public void removeObstacle(int index) {
		if (obstacles == null)
			return;
		obstacles.remove(index);
	}

	public void setSimulationBox(float length, float width, float height) {
		forceCalculator.setSimulationBox(0.5f * length, 0.5f * width, 0.5f * height);
	}

	public String getAtomPropertySelection(byte property) {
		if (iAtom <= 0)
			return null;
		String s = "";
		switch (property) {
		case Atom.UNMOVABLE:
			for (int i = 0; i < iAtom; i++) {
				if (!atom[i].isMovable())
					s += i + " ";
			}
			break;
		case Atom.INVISIBLE:
			for (int i = 0; i < iAtom; i++) {
				if (!atom[i].isVisible())
					s += i + " ";
			}
			break;
		}
		return s.trim();
	}

	void record() {
		modelTimeQueue.update(getModelTime());
		kin = getKin();
		if (getModelTime() <= ZERO)
			pot = compute(0);
		tot = kin + pot;
		kine.update(kin);
		pote.update(pot);
		tote.update(tot);
		// System.out.println("[" + iAtom + "]" + getModelTime() + ": " + kin + ", " + pot + ", " + tot);
		updateAtomQs();
	}

	private void updateAtomQs() {
		int c = movie.getCapacity();
		for (int i = 0; i < iAtom; i++) {
			if (!atom[i].isMovable())
				continue;
			try {
				atom[i].updateRQ();
			}
			catch (Exception e) {
				atom[i].initRQ(c);
				atom[i].updateRQ();
			}
			try {
				atom[i].updateVQ();
			}
			catch (Exception e) {
				atom[i].initVQ(c);
				atom[i].updateVQ();
			}
			try {
				atom[i].updateAQ();
			}
			catch (Exception e) {
				atom[i].initAQ(c);
				atom[i].updateAQ();
			}
		}
	}

	/**
	 * this method is used to "get rid of" a tape when a user action that results in frame inconsistency has occurred,
	 * and "insert a new tape".
	 */
	public int resetTape() {
		if (recorderDisabled)
			return JOptionPane.YES_OPTION;
		if (movie.length() <= 0)
			return JOptionPane.YES_OPTION;
		int i = JOptionPane.YES_OPTION;
		// i=actionReminder.show(ActionReminder.RESET_TAPE);
		if (i == JOptionPane.YES_OPTION || i == JOptionPane.CANCEL_OPTION)
			insertNewTape();
		return i;
	}

	void insertNewTape() {
		modelTime = 0;
		movie.setCurrentFrameIndex(0);
		movie.getMovieSlider().repaint();
		if (hasEmbeddedMovie())
			setTapePointer(0);
	}

	public SlideMovie getMovie() {
		return movie;
	}

	public void setView(MolecularView view) {
		this.view = view;
	}

	public MolecularView getView() {
		return view;
	}

	public void setTimeStep(float timeStep) {
		this.timeStep = timeStep;
		timeStep2 = timeStep * timeStep * 0.5f;
	}

	public float getTimeStep() {
		return timeStep;
	}

	public float getModelTime() {
		return modelTime;
	}

	public void setModelTime(float t) {
		modelTime = t;
		if (t < ZERO) {
			if (job != null)
				job.setIndexOfStep(0);
		}
	}

	private void initializeJob() {
		if (job == null) {
			job = new Job("3D Molecular Simulator #" + jobIndex++) {

				public void run() {
					while (true) {
						super.run();
						while (!isStopped()) {
							modelTime += getTimeStep();
							advance(indexOfStep++);
							execute();
						}
						synchronized (this) {
							try {
								wait();
							}
							catch (InterruptedException e) {
								// e.printStackTrace();
								break;
							}
						}
					}
				}

				public void runScript(String script) {
					if (script != null)
						MolecularModel.this.runScript(script);
				}

				public void notifyChange() {
					MolecularModel.this.notifyChange();
				}

			};
		}
		if (!recorderDisabled && !job.contains(movieUpdater))
			job.add(movieUpdater);
		if (!job.contains(paintView))
			job.add(paintView);
	}

	public Job getJob() {
		if (job == null)
			initializeJob();
		return job;
	}

	private boolean needMinimization() {
		float xij, yij, zij, rijsq;
		for (int i = 0; i < iAtom - 1; i++) {
			for (int j = i + 1; j < iAtom; j++) {
				if (atom[i].isBonded(atom[j]))
					continue;
				xij = atom[i].rx - atom[j].rx;
				yij = atom[i].ry - atom[j].ry;
				zij = atom[i].rz - atom[j].rz;
				rijsq = xij * xij + yij * yij + zij * zij;
				xij = 0.25f * (atom[i].sigma + atom[j].sigma); // allow 50% overlapping?
				if (rijsq < xij * xij) {
					return true;
				}
			}
		}
		return false;
	}

	public void minimize(int nstep, float delta) {
		float oldPot = compute(0);
		for (int i = 0; i <= nstep; i++) {
			pot = SteepestDescentMinimizer.minimize(this, delta);
			if (i % 10 == 0) {
				view.setInfoString("Energy minimizer: " + (pot - oldPot) + " eV");
				view.refresh();
				view.repaint();
				// System.out.println(i + "=" + pot);
			}
		}
		view.setInfoString(null);
		view.repaint();
	}

	public void minimize(int nstep, float delta, BitSet selectionSet) {
		float oldPot = compute(0);
		for (int i = 0; i <= nstep; i++) {
			pot = SteepestDescentMinimizer.minimize(this, delta, selectionSet);
			if (i % 10 == 0) {
				view.setInfoString("Energy minimizer: " + (pot - oldPot) + " eV");
				view.refresh();
				view.repaint();
				// System.out.println(i + "=" + pot);
			}
		}
		view.setInfoString(null);
		view.repaint();
	}

	public void run() {
		if (job != null)
			job.processPendingRequests();
		if (needMinimization()) {
			Runnable r = new Runnable() {
				public void run() {
					minimize(50, 1.0f);
					run2();
				}
			};
			new Thread(r, "Energy Minimizer Before Running").start();
		}
		else {
			run2();
		}
	}

	private synchronized void run2() {
		checkCharges();
		if (job == null)
			initializeJob();
		job.start();
		stopAtNextRecordingStep = false;
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				Action a = view.getActionMap().get("run");
				if (a != null)
					a.setEnabled(false);
				a = view.getActionMap().get("stop");
				if (a != null)
					a.setEnabled(true);
				notifyRun();
			}
		});
	}

	public boolean isRunning() {
		if (job == null)
			return false;
		return !job.isStopped();
	}

	/**
	 * stop running this model. NOTE: When the recording mode is invoked, calling this method does not stop the model
	 * immediately; it will stop at the next recording step. If not in the recording mode, it will stop immediately.
	 */
	public void stop() {
		if (getRecorderDisabled()) {
			stopImmediately();
		}
		else {
			if (movie.getCurrentFrameIndex() > 0) {
				stopAtNextRecordingStep = true;
			}
			else {
				stopImmediately();
			}
		}
	}

	/** stop the model immediately without retarding to the next recording step */
	public void stopImmediately() {
		if (job == null)
			return;
		if (job.isTurnedOff())
			return;
		job.turnOff();
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				view.repaint();
				/*
				 * view.enableEditor(true); stop.setEnabled(false); play.setEnabled(true); revert.setEnabled(true);
				 * notifyModelListeners(new ModelEvent(MDModel.this, ModelEvent.MODEL_STOP));
				 */
			}
		});
	}

	public boolean isTapeFull() {
		if (movie == null)
			throw new RuntimeException("There is no tape in the recorder");
		return getTapePointer() == movie.getCapacity();
	}

	public boolean getRecorderDisabled() {
		return recorderDisabled;
	}

	public void setRecorderDisabled(boolean b) {
		recorderDisabled = b;
	}

	public int getTapeLength() {
		if (modelTimeQueue != null)
			return modelTimeQueue.getLength();
		return 0;
	}

	public int getTapePointer() {
		return modelTimeQueue.getPointer();
	}

	void setTapePointer(int n) {
		if (!hasEmbeddedMovie())
			throw new RuntimeException("cannot set pointer because there is no tape");
		modelTimeQueue.setPointer(n);
		for (int i = 0; i < iAtom; i++) {
			if (atom[i].isMovable()) {
				atom[i].moveRPointer(n);
				atom[i].moveVPointer(n);
				atom[i].moveAPointer(n);
			}
		}
		kine.setPointer(n);
		pote.setPointer(n);
		tote.setPointer(n);
	}

	private void setQueueLength(int n) {
		modelTimeQueue.setLength(n);
		kine.setLength(n);
		pote.setLength(n);
		tote.setLength(n);
	}

	public FloatQueue getModelTimeQueue() {
		return modelTimeQueue;
	}

	public FloatQueue getTote() {
		return tote;
	}

	public FloatQueue getKine() {
		return kine;
	}

	public FloatQueue getPote() {
		return pote;
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
			int n = movie.getCapacity();
			setQueueLength(n);
			for (int i = 0; i < iAtom; i++) {
				if (i == 0 || atom[i].isMovable())
					atom[i].initMovieQ(n);
			}
		}
		else {
			if (job != null && job.contains(movieUpdater))
				job.remove(movieUpdater);
			setQueueLength(-1);
			for (int i = 0; i < iAtom; i++) {
				atom[i].initMovieQ(-1);
			}
		}
		movie.setCurrentFrameIndex(0);
		setRecorderDisabled(!b);
	}

	public boolean hasEmbeddedMovie() {
		if (getTapePointer() <= 0)
			return false;
		if (iAtom > 0) {
			for (int i = 0; i < iAtom; i++) {
				if (atom[i].isMovable()) {
					if (atom[i].rQ == null || atom[i].rQ.isEmpty())
						return false;
				}
			}
		}
		return true;
	}

	public void clear() {
		// haltScriptExecution();
		// if we halt scripts, then the scripts that may call this method will not be able to run
		tot = 0;
		kin = 0;
		pot = 0;
		setModelTime(0);
		setTimeStep(2);
		stopAtNextRecordingStep = false;
		if (hasEmbeddedMovie())
			insertNewTape();
		if (movie != null) {
			movie.enableAllMovieActions(false);
			movie.getMovieSlider().repaint();
		}
		if (job != null) {
			job.setIndexOfStep(0);
		}
		for (int i = 0; i < iAtom; i++) {
			atom[i].setMovable(true);
			atom[i].setCharge(0);
			atom[i].setDamp(0);
			atom[i].zeroVelocity();
			atom[i].zeroAcceleration();
			atom[i].clearBondLists();
		}
		iAtom = 0;
		forceCalculator.setUpdateList(true);
		if (obstacles != null)
			obstacles.clear();
		rBonds.clear();
		aBonds.clear();
		tBonds.clear();
		molecules.clear();
		activateHeatBath(false);
	}

	public void transferKE(float amount) {
		if (iAtom <= 0)
			return;
		float k0 = getKin();
		if (k0 < ZERO)
			return;
		synchronized (forceCalculator) {
			for (int i = 0; i < iAtom; i++) {
				if (!atom[i].isMovable())
					continue;
				k0 = EV_CONVERTER * atom[i].mass
						* (atom[i].vx * atom[i].vx + atom[i].vy * atom[i].vy + atom[i].vz * atom[i].vz);
				if (k0 <= ZERO)
					k0 = ZERO;
				k0 = (k0 + amount) / k0;
				if (k0 <= ZERO)
					k0 = ZERO;
				k0 = (float) Math.sqrt(k0);
				if (Float.isNaN(k0) || Float.isInfinite(k0))
					continue; // any abnormal number must be skipped
				atom[i].vx *= k0;
				atom[i].vy *= k0;
				atom[i].vz *= k0;
			}
		}
	}

	void applyBoundary() {
		forceCalculator.applyBoundary();
	}

	float compute(int time) {
		return forceCalculator.compute(time);
	}

	void predictor() {
		applyBoundary();
		synchronized (forceCalculator) {
			if (iAtom == 0) {
				atom[0].predict(timeStep, timeStep2);
			}
			else if (iAtom > 0) {
				for (int i = 0; i < iAtom; i++) {
					if (atom[i].isMovable())
						atom[i].predict(timeStep, timeStep2);
				}
			}
		}
	}

	void corrector() {
		synchronized (forceCalculator) {
			if (iAtom == 0) {
				atom[0].ax = atom[0].fx;
				atom[0].ay = atom[0].fy;
				atom[0].az = atom[0].fz;
				atom[0].fx *= atom[0].mass;
				atom[0].fy *= atom[0].mass;
				atom[0].fz *= atom[0].mass;
			}
			else if (iAtom > 0) {
				float halfTimeStep = timeStep * 0.5f;
				for (int i = 0; i < iAtom; i++) {
					if (atom[i].isMovable())
						atom[i].correct(halfTimeStep);
				}
			}
			if (obstacles != null) {
				int n = obstacles.size();
				if (n > 0) {
					for (int i = 0; i < n; i++) {
						obstacles.get(i).collide();
					}
				}
			}
		}
	}

	void advance(int i) {
		predictor();
		pot = compute(i);
		corrector();
	}

	private void checkCharges() {
		coulombicIsOn = false;
		for (int i = 0; i < iAtom; i++) {
			if (Math.abs(atom[i].charge) > ZERO) {
				coulombicIsOn = true;
				break;
			}
		}
		if (coulombicIsOn) {
			minimumJobCycleTime = 2 * DEFAULT_MINIMUM_JOB_CYCLE_TIME;
		}
		else {
			minimumJobCycleTime = DEFAULT_MINIMUM_JOB_CYCLE_TIME;
		}
		if (System.getProperty("os.name").startsWith("Mac"))
			minimumJobCycleTime *= 2;
	}

	public float getTemperature() {
		return getKin() * UNIT_EV_OVER_KB;
	}

	/**
	 * calculates the kinetic energy per atom.
	 * 
	 * @return average kinetic energy
	 */
	public float getKin() {
		if (iAtom < 0)
			return 0;
		float e = 0;
		int count = 0;
		Atom a = null;
		synchronized (forceCalculator) {
			for (int i = 0; i < iAtom; i++) {
				a = atom[i];
				if (!a.isMovable())
					continue;
				e += (a.vx * a.vx + a.vy * a.vy + a.vz * a.vz) * a.mass;
				count++;
			}
		}
		if (count == 0)
			return 0;
		/*
		 * the prefactor 0.5 doesn't show up here because of mass unit conversion.
		 */
		return e * EV_CONVERTER / count;
	}

	/** change the temperature by percentage */
	public void changeTemperature(float percent) {
		if (percent < -1.0f)
			percent = -1.0f;
		if (!heatBathActivated()) {
			float temp1 = getKin() * UNIT_EV_OVER_KB;
			if (temp1 < ZERO)
				assignTemperature(100);
			rescaleVelocities((float) Math.sqrt(percent + 1));
		}
		else {
			heatBath.changeExpectedTemperature(percent);
			setTemperature(heatBath.getExpectedTemperature());
		}
	}

	private void rescaleVelocities(float ratio) {
		synchronized (forceCalculator) {
			Atom a = null;
			for (int i = 0; i < iAtom; i++) {
				a = atom[i];
				if (!a.isMovable())
					continue;
				a.vx *= ratio;
				a.vy *= ratio;
				a.vz *= ratio;
			}
		}
	}

	/** assign velocities to atoms according to the Boltzman-Maxwell distribution */
	public void assignTemperature(float temperature) {
		if (temperature < ZERO)
			temperature = 0.0f;
		float rtemp = (float) Math.sqrt(temperature) * VT_CONVERSION_CONSTANT;
		float sumVx = 0.0f;
		float sumVy = 0.0f;
		float sumVz = 0.0f;
		float sumMass = 0.0f;
		if (iAtom == 0) {
			// atom[0].setRandomVelocity(rtemp);
		}
		else {
			if (random == null)
				random = new Random();
			for (int i = 0; i < iAtom; i++) {
				if (!atom[i].isMovable())
					continue;
				atom[i].vx = rtemp * (float) random.nextGaussian();
				atom[i].vy = rtemp * (float) random.nextGaussian();
				atom[i].vz = rtemp * (float) random.nextGaussian();
				sumVx += atom[i].vx * atom[i].mass;
				sumVy += atom[i].vy * atom[i].mass;
				sumVz += atom[i].vz * atom[i].mass;
				sumMass += atom[i].mass;
			}
		}
		if (sumMass > ZERO) {
			sumVx /= sumMass;
			sumVy /= sumMass;
			sumVz /= sumMass;
			if (iAtom > 1) {
				for (int i = 0; i < iAtom; i++) {
					if (!atom[i].isMovable())
						continue;
					atom[i].vx -= sumVx;
					atom[i].vy -= sumVy;
					atom[i].vz -= sumVz;
				}
			}
			setTemperature(temperature);
		}
	}

	public void setTemperature(float temperature) {
		if (temperature < ZERO)
			temperature = 0.0f;
		double temp1 = getKin() * UNIT_EV_OVER_KB;
		if (temp1 < ZERO && temperature > ZERO) {
			assignTemperature(temperature);
			temp1 = getKin() * UNIT_EV_OVER_KB;
		}
		if (temp1 > ZERO)
			rescaleVelocities((float) Math.sqrt(temperature / temp1));
	}

	public BField getBField() {
		return bField;
	}

	public void setBField(float intensity, Vector3f direction) {
		if (intensity > ZERO) {
			if (bField == null)
				bField = new BField();
			bField.setIntensity(intensity);
			if (direction != null)
				bField.setDirection(direction.x, direction.y, direction.z);
		}
		else {
			bField = null;
		}
	}

	public EField getEField() {
		return eField;
	}

	public void setEField(float intensity, Vector3f direction) {
		if (intensity > ZERO) {
			if (eField == null)
				eField = new EField();
			eField.setIntensity(intensity);
			if (direction != null)
				eField.setDirection(direction.x, direction.y, direction.z);
		}
		else {
			eField = null;
		}
	}

	public GField getGField() {
		return gField;
	}

	public void setGField(float intensity, Vector3f direction) {
		if (intensity > ZERO) {
			if (gField == null)
				gField = new GField();
			gField.setIntensity(intensity);
			if (direction != null) {
				gField.setDirection(direction.x, direction.y, direction.z);
				gField.setAlwaysDown(false);
			}
			else {
				gField.setAlwaysDown(true);
			}
		}
		else {
			gField = null;
		}
	}

	/** notify anyone interested in knowing the rotation matrix of the view. */
	public void setRotationMatrix(Matrix3f rotation) {
		if (gField != null && gField.isAlwaysDown())
			gField.setRotation(rotation);
	}

	/* show the <i>i</i>-th frame of the movie */
	void showMovieFrame(int frame) {
		if (frame < 0 || movie.length() <= 0)
			return;
		if (frame >= movie.length())
			throw new IllegalArgumentException("Frame " + frame + " does not exist");
		setModelTime(modelTimeQueue.getData(frame));
		Atom a = null;
		for (int i = 0; i < iAtom; i++) {
			a = atom[i];
			if (!a.isMovable())
				continue;
			a.rx = a.rQ.getQueue1().getData(frame);
			a.ry = a.rQ.getQueue2().getData(frame);
			a.rz = a.rQ.getQueue3().getData(frame);
			a.vx = a.vQ.getQueue1().getData(frame);
			a.vy = a.vQ.getQueue2().getData(frame);
			a.vz = a.vQ.getQueue3().getData(frame);
			a.ax = a.aQ.getQueue1().getData(frame);
			a.ay = a.aQ.getQueue2().getData(frame);
			a.az = a.aQ.getQueue3().getData(frame);
			a.fx = a.ax * a.mass;
			a.fy = a.ay * a.mass;
			a.fz = a.az * a.mass;
		}
	}

	class EmbeddedMovie extends SlideMovie {

		public synchronized int length() {
			if (modelTimeQueue == null || modelTimeQueue.isEmpty())
				return 0;
			if (modelTimeQueue.getPointer() == modelTimeQueue.getLength())
				return modelTimeQueue.getLength();
			return modelTimeQueue.getPointer();
		}

		/* show the i-th frame of the movie */
		public void showFrame(int frame) {
			super.showFrame(frame);
			showMovieFrame(frame);
			view.refresh();
			view.repaint();
		}

	}

	public String toString() {
		if (view == null)
			return super.toString();
		String s = view.getResourceAddress();
		if (s == null)
			return super.toString();
		return FileUtilities.getFileName(s);
	}

}