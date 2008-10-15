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
import java.util.HashMap;
import java.util.Map;

/**
 * This class specifies the rules used to model the quantum effects related to electrons and photons.
 */

public class QuantumRule implements Serializable {

	public final static Integer RADIATIONLESS_TRANSITION = new Integer(11);
	public final static Integer STIMULATED_EMISSION = new Integer(12);

	private Map<Object, Float> probabilityMap;
	private boolean disallowIonization;

	public QuantumRule() {
		probabilityMap = new HashMap<Object, Float>();
	}

	public void reset() {
		probabilityMap.clear();
		disallowIonization = false;
	}

	public void setIonizationDisallowed(boolean b) {
		disallowIonization = b;
	}

	public boolean isIonizationDisallowed() {
		return disallowIonization;
	}

	public void setProbabilityMap(Map<Object, Float> map) {
		probabilityMap = map;
	}

	public Map<Object, Float> getProbabilityMap() {
		return probabilityMap;
	}

	public void setProbability(Object key, float value) {
		probabilityMap.put(key, value);
	}

	public float getProbability(Object key) {
		Float x = probabilityMap.get(key);
		if (x == null)
			return 0.5f;
		return x;
	}

}