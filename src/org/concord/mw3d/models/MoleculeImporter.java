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

import java.util.BitSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.vecmath.Point3f;

/**
 * @author Charles Xie
 * 
 */
public class MoleculeImporter {

	private static Map<String, String> moleculeMap = new LinkedHashMap<String, String>();

	private MoleculeReader reader;
	private MolecularModel model;
	private Point3f center;

	private static void loadAvailableMolecules() {
		if (moleculeMap.isEmpty()) {
			try {
				new MoleculeFileReader().read(MoleculeImporter.class.getResource("resources/molecules.dat"),
						moleculeMap);
			}
			catch (Exception e) {
				e.printStackTrace(System.err);
			}
		}
	}

	public static Set getAvailableMolecules() {
		loadAvailableMolecules();
		return moleculeMap.keySet();
	}

	MoleculeImporter(MolecularModel model) {
		this.model = model;
		loadAvailableMolecules();
	}

	String read(int id, Point3f position) {
		if (id < 0 || id >= moleculeMap.size())
			return null;
		if (reader == null)
			reader = new MoleculeReader(model);
		int n0 = model.getAtomCount();
		reader.read(getClass().getResource("resources/" + moleculeMap.get(moleculeMap.keySet().toArray()[id])));
		int n1 = model.getAtomCount();
		if (center == null) {
			center = new Point3f();
		}
		else {
			center.set(0, 0, 0);
		}
		for (int i = n0; i < n1; i++) {
			center.x += model.atom[i].rx;
			center.y += model.atom[i].ry;
			center.z += model.atom[i].rz;
		}
		float inv = 1.0f / (n1 - n0);
		center.x = position.x - center.x * inv;
		center.y = position.y - center.y * inv;
		center.z = position.z - center.z * inv;
		boolean overlap = false;
		getout: for (int i = n0; i < n1; i++) {
			model.atom[i].rx += center.x;
			model.atom[i].ry += center.y;
			model.atom[i].rz += center.z;
			for (int j = 0; j < n0; j++) {
				if (model.atom[i].isTooClose(model.atom[j])) {
					overlap = true;
					break getout;
				}
			}
		}
		if (overlap) {
			BitSet bs = new BitSet(n1);
			bs.set(n0, n1);
			model.removeAtoms(bs);
			return null;
		}
		return reader.getDescription();
	}

}