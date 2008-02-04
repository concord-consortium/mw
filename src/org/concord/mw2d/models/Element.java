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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.concord.mw2d.event.ParameterChangeEvent;
import org.concord.mw2d.event.ParameterChangeListener;

/**
 * The Lennard-Jones particles are usually used to model atoms. But they are also used in our models to represent
 * chemical compounds. For example, they are used to represent amino acids and purines or pyrimidines. The elements that
 * are used to represent amino acids and purines are not editable, which means the end users cannot change their
 * properties. We provide four editable elements (Nt, Pl, Ws and Ck), which the end users can change their properties
 * from the GUI.
 * 
 * @author Charles Xie
 */

public class Element implements Serializable {

	final static byte ALL_CHANGED = -100;
	final static byte MASS_CHANGED = -101;
	final static byte EPSILON_CHANGED = -102;
	final static byte SIGMA_CHANGED = -103;

	private final static double ZERO = 0.000001;

	/** an editable element */
	public final static byte ID_NT = 0x00;

	/** an editable element */
	public final static byte ID_PL = 0x01;

	/** an editable element */
	public final static byte ID_WS = 0x02;

	/** an editable element */
	public final static byte ID_CK = 0x03;

	/**
	 * reserved for constructing <code>MolecularObject</code>.
	 * 
	 * @see org.concord.mw2d.models.MolecularObject
	 */
	public final static byte ID_MO = 0x04;

	/** element standing for Ala */
	public final static byte ID_ALA = 0x05;

	/** element standing for Arg */
	public final static byte ID_ARG = 0x06;

	/** element standing for Asn */
	public final static byte ID_ASN = 0x07;

	/** element standing for Asp */
	public final static byte ID_ASP = 0x08;

	/** element standing for Cys */
	public final static byte ID_CYS = 0x09;

	/** element standing for Gln */
	public final static byte ID_GLN = 0x0a;

	/** element standing for Glu */
	public final static byte ID_GLU = 0x0b;

	/** element standing for Gly */
	public final static byte ID_GLY = 0x0c;

	/** element standing for His */
	public final static byte ID_HIS = 0x0d;

	/** element standing for Ile */
	public final static byte ID_ILE = 0x0e;

	/** element standing for Leu */
	public final static byte ID_LEU = 0x0f;

	/** element standing for Lys */
	public final static byte ID_LYS = 0x10;

	/** element standing for Met */
	public final static byte ID_MET = 0x11;

	/** element standing for Phe */
	public final static byte ID_PHE = 0x12;

	/** element standing for Pro */
	public final static byte ID_PRO = 0x13;

	/** element standing for Ser */
	public final static byte ID_SER = 0x14;

	/** element standing for Thr */
	public final static byte ID_THR = 0x15;

	/** element standing for Trp */
	public final static byte ID_TRP = 0x16;

	/** element standing for Tyr */
	public final static byte ID_TYR = 0x17;

	/** element standing for Val */
	public final static byte ID_VAL = 0x18;

	/**
	 * element standing for sugar-phosphate part of a nucleotide. They are connected by ester links to form DNA
	 * backbone.
	 */
	public final static byte ID_SP = 0x19;

	/** element standing for adenine. */
	public final static byte ID_A = 0x1a;

	/** element standing for cytosine. */
	public final static byte ID_C = 0x1b;

	/** element standing for guanine. */
	public final static byte ID_G = 0x1c;

	/** element standing for thymine. */
	public final static byte ID_T = 0x1d;

	/** element standing for uracil. */
	public final static byte ID_U = 0x1e;

	final static byte NMAX = 0x1f;

	private volatile double mass = 1.0;
	private volatile double sigma = 12.0;
	private volatile double epsilon = 0.1;
	private volatile int id = ID_NT;
	private transient List<ParameterChangeListener> listenerList = new ArrayList<ParameterChangeListener>();
	private ElectronicStructure electronicStructure;

	/** by default, return an Nt element */
	public Element() {
		createDefaultElectronicStructure();
	}

	public Element(int id, double mass, double sigma, double epsilon) {
		if (id < ID_NT) {
			this.id = ID_NT;
			throw new IllegalArgumentException("Illegal element ID");
		}
		else if (id >= NMAX) {
			this.id = NMAX - 1;
			throw new IllegalArgumentException("Illegal element ID");
		}
		else {
			this.id = id;
		}
		setProperties(mass, sigma, epsilon);
		createDefaultElectronicStructure();
	}

	public static byte getNumberOfElements() {
		return NMAX;
	}

	private void createDefaultElectronicStructure() {
		electronicStructure = ElectronicStructure.createThreeStateSystem();
	}

	public void setElectronicStructure(ElectronicStructure es) {
		electronicStructure = es;
	}

	public ElectronicStructure getElectronicStructure() {
		return electronicStructure;
	}

	/** return the element reserved for constructing <code>MolecularObject</code>. */
	public static byte getMolecularObjectElement() {
		return ID_MO;
	}

	public void addParameterChangeListener(ParameterChangeListener pcl) {
		listenerList.add(pcl);
	}

	public void removeParameterChangeListener(ParameterChangeListener pcl) {
		listenerList.remove(pcl);
	}

	void fireParameterChange(byte type) {
		for (ParameterChangeListener l : listenerList)
			l.parameterChanged(new ParameterChangeEvent(this, type));
	}

	public void setProperties(double mass, double sigma, double epsilon) {

		if (sigma <= 0.0)
			throw new IllegalArgumentException("Atomic radius must be greater than zero");
		if (mass <= 0.0)
			throw new IllegalArgumentException("Atomic mass must be greater than zero");
		if (epsilon <= 0.0) {
			throw new IllegalArgumentException("Well depth must be greater than zero");
		}
		else if (epsilon >= 10.0) {
			throw new IllegalArgumentException("Well depth is too big");
		}

		boolean changed = false;
		if (Math.abs(this.mass - mass) > ZERO) {
			this.mass = mass;
			changed = true;
		}
		if (Math.abs(this.sigma - sigma) > ZERO) {
			this.sigma = sigma;
			changed = true;
		}
		if (Math.abs(this.epsilon - epsilon) > ZERO) {
			this.epsilon = epsilon;
			changed = true;
		}
		if (changed)
			fireParameterChange(ALL_CHANGED);

	}

	public void setElement(Element e) {
		setProperties(e.getMass(), e.getSigma(), e.getEpsilon());
	}

	public void setMass(double mass) {
		if (Math.abs(this.mass - mass) < ZERO)
			return;
		if (mass <= ZERO) {
			this.mass = 1.0 / 120;
		}
		else {
			this.mass = mass;
		}
		fireParameterChange(MASS_CHANGED);
	}

	public double getMass() {
		return mass;
	}

	public void setSigma(double sigma) {
		if (Math.abs(this.sigma - sigma) < ZERO)
			return;
		this.sigma = sigma;
		fireParameterChange(SIGMA_CHANGED);
	}

	public double getSigma() {
		return sigma;
	}

	public void setEpsilon(double epsilon) {
		if (Math.abs(this.epsilon - epsilon) < ZERO)
			return;
		this.epsilon = epsilon;
		fireParameterChange(EPSILON_CHANGED);
	}

	public double getEpsilon() {
		return epsilon;
	}

	public int getID() {
		return id;
	}

	public void setID(int i) {
		id = i;
	}

	public static String idToName(int i) {
		switch (i) {
		case ID_NT:
			return "Nt";
		case ID_PL:
			return "Pl";
		case ID_WS:
			return "Ws";
		case ID_CK:
			return "Ck";
		case ID_MO:
			return "Mo";
		case ID_SP:
			return "Sp";
		case ID_ALA:
			return "Ala";
		case ID_ARG:
			return "Arg";
		case ID_ASN:
			return "Asn";
		case ID_ASP:
			return "Asp";
		case ID_CYS:
			return "Cys";
		case ID_GLN:
			return "Gln";
		case ID_GLU:
			return "Glu";
		case ID_GLY:
			return "Gly";
		case ID_HIS:
			return "His";
		case ID_ILE:
			return "Ile";
		case ID_LEU:
			return "Leu";
		case ID_LYS:
			return "Lys";
		case ID_MET:
			return "Met";
		case ID_PHE:
			return "Phe";
		case ID_PRO:
			return "Pro";
		case ID_SER:
			return "Ser";
		case ID_THR:
			return "Thr";
		case ID_TRP:
			return "Trp";
		case ID_TYR:
			return "Tyr";
		case ID_VAL:
			return "Val";
		case ID_A:
			return "A";
		case ID_C:
			return "C";
		case ID_G:
			return "G";
		case ID_T:
			return "T";
		case ID_U:
			return "U";
		}
		return null;
	}

	public String getName() {
		return idToName(id);
	}

	public boolean equals(Object obj) {
		if (!(obj instanceof Element))
			return false;
		return id == ((Element) obj).id;
	}

	public int hashCode() {
		return id;
	}

	public String toString() {
		return "Element " + id + ":( mass=" + mass + ", sigma=" + sigma + ", epsilon=" + epsilon + ")";
	}

}