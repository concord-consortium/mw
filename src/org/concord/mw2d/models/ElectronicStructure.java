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
import java.util.Collections;
import java.util.List;

/** This class models the electronic structure of a system. */

public class ElectronicStructure implements Serializable {

	private List<EnergyLevel> levelList;

	public ElectronicStructure() {
		// NOTE: synchronized collections are not serializable!!!!!!!!!!!!!
		// levelList=Collections.synchronizedList(new ArrayList());
		levelList = new ArrayList<EnergyLevel>();
	}

	/* construct a three-state system that consists of a ground state and two excited states. */
	static ElectronicStructure createThreeStateSystem() {
		ElectronicStructure s = new ElectronicStructure();
		s.addEnergyLevel(new EnergyLevel(-4));
		s.addEnergyLevel(new EnergyLevel(-1));
		s.addEnergyLevel(new EnergyLevel(-0.5f));
		return s;
	}

	public synchronized float getHighestEnergy() {
		if (levelList.isEmpty())
			throw new RuntimeException("no energy level in this structure");
		return levelList.get(levelList.size() - 1).getEnergy();
	}

	public synchronized float getLowestEnergy() {
		if (levelList.isEmpty())
			throw new RuntimeException("no energy level in this structure");
		return levelList.get(0).getEnergy();
	}

	public void setEnergyLevels(List<EnergyLevel> list) {
		this.levelList = list;
	}

	/** Do NOT call this method to operate the list. */
	public List<EnergyLevel> getEnergyLevels() {
		return levelList;
	}

	public synchronized int getNumberOfEnergyLevels() {
		return levelList.size();
	}

	public synchronized EnergyLevel getEnergyLevel(int i) {
		if (levelList == null)
			return null;
		if (levelList.isEmpty())
			return null;
		return levelList.get(i);
	}

	public synchronized boolean containsEnergyLevel(EnergyLevel e) {
		return levelList.contains(e);
	}

	public synchronized void addEnergyLevel(EnergyLevel e) {
		if (e == null || levelList.contains(e))
			return;
		levelList.add(e);
		sort();
	}

	public synchronized void removeEnergyLevel(EnergyLevel e) {
		if (e == null)
			return;
		levelList.remove(e);
	}

	public synchronized int indexOf(EnergyLevel e) {
		if (e == null)
			return -1;
		return levelList.indexOf(e);
	}

	@SuppressWarnings("unchecked")
	public void sort() {
		Collections.sort(levelList);
	}

	public synchronized boolean contains(float energy, float snap) {
		if (levelList == null)
			return false;
		if (levelList.isEmpty())
			return false;
		for (EnergyLevel el : levelList) {
			if (Math.abs(el.getEnergy() - energy) < snap)
				return true;
		}
		return false;
	}

	public String toString() {
		return levelList.toString();
	}

}