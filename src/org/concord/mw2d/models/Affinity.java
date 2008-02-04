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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.concord.mw2d.event.ParameterChangeEvent;
import org.concord.mw2d.event.ParameterChangeListener;

/**
 * <p>
 * This class defines the "van del Waals chemical affinity" between two elements. The affinity is described by the
 * following parameters:
 * </p>
 * 
 * <ul>
 * <li>Wether the interation is purely repulsive or not
 * <ul>
 * <li>If the interaction is not purely repulsive, should the mean potential be used? If not, specify the cross-element
 * Lennard-Jones parameters.</li>
 * </ul>
 * </li>
 * </ul>
 */

public class Affinity implements Serializable {

	private final static double ZERO = 0.000001;
	private Pair[] pairs;
	private Map<Pair, Boolean> repulsive;
	private Map<Pair, Boolean> lbMixing;
	private Map<Pair, Double> sigma;
	private Map<Pair, Double> epsilon;
	private List<ParameterChangeListener> listenerList = new ArrayList<ParameterChangeListener>();

	class LbMixingMap<K, V> extends HashMap<K, V> {

		LbMixingMap() {
			super();
		}

		public V put(K key, V value) {
			V oldValue = get(key);
			super.put(key, value);
			if (value == Boolean.TRUE) {
				if (!(key instanceof Pair))
					throw new IllegalArgumentException("Key must be a pair of element!");
				Pair p = (Pair) key;
				Element e1 = p.getElement1();
				Element e2 = p.getElement2();
				sigma.put(p, Math.sqrt(e1.getSigma() * e2.getSigma()));
				epsilon.put(p, 0.5 * (e1.getEpsilon() + e2.getEpsilon()));
			}
			return oldValue;
		}

	}

	public Affinity() {
		repulsive = new HashMap<Pair, Boolean>(); // these maps must be non-synchronized for serialization!!!!!!!!!!
		lbMixing = new LbMixingMap<Pair, Boolean>();
		sigma = new HashMap<Pair, Double>();
		epsilon = new HashMap<Pair, Double>();
	}

	Affinity(Element[] e) {
		this(e, null);
	}

	Affinity(Element[] e, Affinity aff) {
		this();
		pairs = new Pair[e.length * (e.length - 1) / 2];
		int n = 0;
		for (int i = 0; i < e.length - 1; i++) {
			for (int j = i + 1; j < e.length; j++) {
				pairs[n] = new Pair(e[i], e[j]);
				if (aff != null) {
					lbMixing.put(pairs[n], (Boolean) aff.getObj(e[i].getID(), e[j].getID(), aff.getLbMixing()));
					repulsive.put(pairs[n], (Boolean) aff.getObj(e[i].getID(), e[j].getID(), aff.getRepulsive()));
					sigma.put(pairs[n], (Double) aff.getObj(e[i].getID(), e[j].getID(), aff.getSigma()));
					epsilon.put(pairs[n], (Double) aff.getObj(e[i].getID(), e[j].getID(), aff.getEpsilon()));
				}
				else {
					lbMixing.put(pairs[n], Boolean.TRUE);
					repulsive.put(pairs[n], Boolean.FALSE);
				}
				n++;
			}
		}
		if (aff != null)
			aff.destroy();
	}

	public void destroy() {
		listenerList.clear();
		lbMixing.clear();
		repulsive.clear();
		sigma.clear();
		epsilon.clear();
		if (pairs != null) {
			for (int i = 0; i < pairs.length; i++) {
				if (pairs[i] == null)
					continue;
				pairs[i].setElement1(null);
				pairs[i].setElement2(null);
				pairs[i] = null;
			}
		}
	}

	public void addParameterChangeListener(ParameterChangeListener pcl) {
		listenerList.add(pcl);
	}

	public void removeParameterChangeListener(ParameterChangeListener pcl) {
		listenerList.remove(pcl);
	}

	void fireParameterChange() {
		ParameterChangeEvent e = new ParameterChangeEvent(this);
		for (ParameterChangeListener l : listenerList)
			l.parameterChanged(e);
	}

	public void setRepulsive(Map<Pair, Boolean> hm) {
		repulsive = hm;
	}

	public Map<Pair, Boolean> getRepulsive() {
		return repulsive;
	}

	public void setLbMixing(Map<Pair, Boolean> hm) {
		lbMixing = hm;
	}

	public Map<Pair, Boolean> getLbMixing() {
		return lbMixing;
	}

	public void setSigma(Map<Pair, Double> hm) {
		sigma = hm;
	}

	public Map<Pair, Double> getSigma() {
		return sigma;
	}

	public void setEpsilon(Map<Pair, Double> hm) {
		epsilon = hm;
	}

	public Map<Pair, Double> getEpsilon() {
		return epsilon;
	}

	/**
	 * set the interaction between these two elements to be purely repulsive, or not
	 * 
	 * @return the value associated with this property before settting
	 */
	public boolean setRepulsive(Element e1, Element e2, boolean b) {
		Pair p = getPair(e1, e2);
		if (p == null)
			return false;
		boolean oldValue = repulsive.get(p);
		if (b != oldValue) {
			repulsive.put(p, b ? Boolean.TRUE : Boolean.FALSE);
		}
		return oldValue;
	}

	public boolean isRepulsive(Element e1, Element e2) {
		Pair p = getPair(e1, e2);
		if (p == null)
			return false;
		Boolean o = repulsive.get(p);
		if (o != null)
			return o;
		return false;
	}

	/**
	 * set the interaction between these two elements to be given by the mean potential or not.
	 * 
	 * @return the value associated with this property before settting
	 */
	public boolean setLBMixing(Element e1, Element e2, boolean b) {
		Pair p = getPair(e1, e2);
		if (p == null)
			return true;
		boolean oldValue = ((Boolean) lbMixing.get(p)).booleanValue();
		if (b != oldValue) {
			lbMixing.put(p, b ? Boolean.TRUE : Boolean.FALSE);
		}
		return oldValue;
	}

	public boolean isLBMixed(Element e1, Element e2) {
		Pair p = getPair(e1, e2);
		if (p == null)
			return true;
		Object o = lbMixing.get(p);
		if (o != null)
			return ((Boolean) o).booleanValue();
		return true;
	}

	public void setSigma(Element e1, Element e2, double d) {
		Pair p = getPair(e1, e2);
		if (p == null)
			return;
		if (((Boolean) lbMixing.get(p)).booleanValue()) {
			fireParameterChange();
		}
		else {
			double oldValue = sigma.get(p);
			if (Math.abs(d - oldValue) > ZERO) {
				sigma.put(p, d);
				fireParameterChange();
			}
		}
	}

	public double getSigma(Element e1, Element e2) {
		Pair p = getPair(e1, e2);
		if (p == null)
			return Math.sqrt(e1.getSigma() * e2.getSigma());
		if (isLBMixed(e1, e2))
			return Math.sqrt(e1.getSigma() * e2.getSigma());
		return sigma.get(p);
	}

	public void setEpsilon(Element e1, Element e2, double d) {
		Pair p = getPair(e1, e2);
		if (p == null)
			return;
		if (((Boolean) lbMixing.get(p)).booleanValue()) {
			fireParameterChange();
		}
		else {
			double oldValue = epsilon.get(p);
			if (Math.abs(d - oldValue) > ZERO) {
				epsilon.put(p, d);
				fireParameterChange();
			}
		}
	}

	public double getEpsilon(Element e1, Element e2) {
		Pair p = getPair(e1, e2);
		if (p == null)
			return 0.5 * (e1.getEpsilon() + e2.getEpsilon());
		if (isLBMixed(e1, e2))
			return 0.5 * (e1.getEpsilon() + e2.getEpsilon());
		return epsilon.get(p);
	}

	private Pair getPair(Element e1, Element e2) {
		for (Pair p : pairs) {
			if ((p.getElement1().equals(e1) && p.getElement2().equals(e2))
					|| (p.getElement1().equals(e2) && p.getElement2().equals(e1)))
				return p;
		}
		return null;
	}

	Object getObj(int id1, int id2, Map map) {
		Pair p;
		for (Iterator it = map.keySet().iterator(); it.hasNext();) {
			p = (Pair) it.next();
			if ((p.getElement1().getID() == id1 && p.getElement2().getID() == id2)
					|| (p.getElement1().getID() == id2 && p.getElement2().getID() == id1))
				return map.get(p);
		}
		return null;
	}

}