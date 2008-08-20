/* $RCSfile: ColorManager.java,v $
 * $Author: qxie $
 * $Date: 2006-11-29 22:46:10 $
 * $Revision: 1.13 $
 *
 * Copyright (C) 2003-2006  Miguel, Jmol Development, www.jmol.org
 *
 * Contact: miguel@jmol.org
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.myjmol.viewer;

import org.myjmol.g3d.*;
import org.myjmol.util.Logger;

class ColorManager {

	Viewer viewer;
	Graphics3D g3d;
	int[] argbsCpk;
	int[] altArgbsCpk;

	ColorManager(Viewer viewer, Graphics3D g3d) {
		this.viewer = viewer;
		this.g3d = g3d;
		argbsCpk = JmolConstants.argbsCpk;
		altArgbsCpk = new int[JmolConstants.altArgbsCpk.length];
		for (int i = JmolConstants.altArgbsCpk.length; --i >= 0;)
			altArgbsCpk[i] = JmolConstants.altArgbsCpk[i];
		resetElementColors();
	}

	// XIE
	short getColixSelection() {
		return Graphics3D.GOLD;
	}

	boolean isDefaultColorRasmol;

	boolean getDefaultColorRasmol() {
		return isDefaultColorRasmol;
	}

	void resetElementColors() {
		setDefaultColors("Jmol");
	}

	void setDefaultColors(String colorScheme) {
		if (colorScheme.equalsIgnoreCase("Jmol")) {
			isDefaultColorRasmol = false;
			argbsCpk = JmolConstants.argbsCpk;
			// viewer.setStringProperty("backgroundColor", "black");
			// must be a very old idea -- clear dots color with
			// default color reset, but that makes no sense now.
			// viewer.setShapeColorProperty(JmolConstants.SHAPE_DOTS, 0);
			// if colors are reset with dots or labels or anything else
			// defined, they are automatically reset.
		}
		else if (colorScheme.equalsIgnoreCase("RasMol")) {
			isDefaultColorRasmol = true;
			copyArgbsCpk();
			int argb = JmolConstants.argbsCpkRasmol[0] | 0xFF000000;
			for (int i = argbsCpk.length; --i >= 0;)
				argbsCpk[i] = argb;
			for (int i = JmolConstants.argbsCpkRasmol.length; --i >= 0;) {
				argb = JmolConstants.argbsCpkRasmol[i];
				int atomNo = argb >> 24;
				argb |= 0xFF000000;
				argbsCpk[atomNo] = argb;
			}
			// viewer.setShapeColorProperty(JmolConstants.SHAPE_DOTS, 0);
		}
		else {
			Logger.error("unrecognized color scheme");
			return;
		}
		for (int i = JmolConstants.argbsCpk.length; --i >= 0;)
			g3d.changeColixArgb((short) i, argbsCpk[i]);
		for (int i = JmolConstants.altArgbsCpk.length; --i >= 0;)
			g3d.changeColixArgb((short) (JmolConstants.elementNumberMax + i), altArgbsCpk[i]);
	}

	void copyArgbsCpk() {
		argbsCpk = new int[JmolConstants.argbsCpk.length];
		for (int i = JmolConstants.argbsCpk.length; --i >= 0;)
			argbsCpk[i] = JmolConstants.argbsCpk[i];
		altArgbsCpk = new int[JmolConstants.altArgbsCpk.length];
		for (int i = JmolConstants.altArgbsCpk.length; --i >= 0;)
			altArgbsCpk[i] = JmolConstants.altArgbsCpk[i];
	}

	short colixRubberband = Graphics3D.HOTPINK;

	void setRubberbandArgb(int argb) {
		colixRubberband = (argb == 0 ? 0 : Graphics3D.getColix(argb));
	}

	short colixBackgroundContrast;

	void setColixBackgroundContrast(int argb) {
		colixBackgroundContrast = ((Graphics3D.calcGreyscaleRgbFromRgb(argb) & 0xFF) < 128 ? Graphics3D.WHITE
				: Graphics3D.BLACK);
	}

	/**
	 * black or white, whichever contrasts more with the current background
	 * 
	 * 
	 * @return black or white colix value
	 */
	short getColixBackgroundContrast() {
		// not implemented
		return colixBackgroundContrast;
	}

	short getColixAtomPalette(Atom atom, byte pid) {
		int argb = 0;
		int index;
		short id;
		Frame frame;
		float lo, hi;
		switch (pid) {
		case JmolConstants.PALETTE_JMOL:
			id = atom.getAtomicAndIsotopeNumber();
			argb = getJmolOrRasmolArgb(id, Token.jmol);
			break;
		case JmolConstants.PALETTE_RASMOL:
			id = atom.getAtomicAndIsotopeNumber();
			argb = getJmolOrRasmolArgb(id, Token.rasmol);
			break;
		case JmolConstants.PALETTE_NONE:
		case JmolConstants.PALETTE_CPK:
			// Note that CPK colors can be changed based upon user preference
			// therefore, a changable colix is allocated in this case
			id = atom.getAtomicAndIsotopeNumber();
			if (id < 256)
				return g3d.getChangableColix(id, argbsCpk[id]);
			id = (short) JmolConstants.altElementIndexFromNumber(id);
			return g3d.getChangableColix((short) (JmolConstants.elementNumberMax + id), altArgbsCpk[id]);
		case JmolConstants.PALETTE_PARTIAL_CHARGE:
			// This code assumes that the range of partial charges is [-1, 1].
			index = quantize(atom.getPartialCharge(), -1, 1, JmolConstants.PARTIAL_CHARGE_RANGE_SIZE);
			return g3d.getChangableColix((short) (JmolConstants.PARTIAL_CHARGE_COLIX_RED + index),
					JmolConstants.argbsRwbScale[index]);
		case JmolConstants.PALETTE_FORMAL_CHARGE:
			index = atom.getFormalCharge() - JmolConstants.FORMAL_CHARGE_MIN;
			return g3d.getChangableColix((short) (JmolConstants.FORMAL_CHARGE_COLIX_RED + index),
					JmolConstants.argbsFormalCharge[index]);
		case JmolConstants.PALETTE_TEMP:
		case JmolConstants.PALETTE_FIXEDTEMP:
			if (pid == JmolConstants.PALETTE_TEMP) {
				frame = viewer.getFrame();
				lo = frame.getBfactor100Lo();
				hi = frame.getBfactor100Hi();
			}
			else {
				lo = 0;
				hi = 100 * 100; // scaled by 100
			}
			index = quantize(atom.getBfactor100(), lo, hi, JmolConstants.argbsRwbScale.length);
			index = JmolConstants.argbsRwbScale.length - 1 - index;
			argb = JmolConstants.argbsRwbScale[index];
			break;
		case JmolConstants.PALETTE_SURFACE:
			hi = viewer.getFrame().getSurfaceDistanceMax();
			index = quantize(atom.getSurfaceDistance(), 0, hi, JmolConstants.argbsRwbScale.length);
			// index = JmolConstants.argbsRwbScale.length - 1 - index;
			argb = JmolConstants.argbsRwbScale[index];
			break;
		case JmolConstants.PALETTE_STRUCTURE:
			argb = JmolConstants.argbsStructure[atom.getProteinStructureType()];
			break;
		case JmolConstants.PALETTE_AMINO:
			index = atom.getGroupID();
			if (index < 0 || index >= JmolConstants.GROUPID_AMINO_MAX)
				index = 0;
			argb = JmolConstants.argbsAmino[index];
			break;
		case JmolConstants.PALETTE_SHAPELY:
			index = atom.getGroupID();
			if (index < 0 || index >= JmolConstants.GROUPID_SHAPELY_MAX)
				index = 0;
			argb = JmolConstants.argbsShapely[index];
			break;
		case JmolConstants.PALETTE_CHAIN:
			int chain = atom.getChainID() & 0x1F;
			if (chain < 0)
				chain = 0;
			if (chain >= JmolConstants.argbsChainAtom.length)
				chain = chain % JmolConstants.argbsChainAtom.length;
			argb = (atom.isHetero() ? JmolConstants.argbsChainHetero : JmolConstants.argbsChainAtom)[chain];
			break;
		case JmolConstants.PALETTE_GROUP:
			// viewer.calcSelectedGroupsCount() must be called first ...
			// before we call getSelectedGroupCountWithinChain()
			// or getSelectedGropuIndexWithinChain
			// however, do not call it here because it will get recalculated
			// for each atom
			// therefore, we call it in Eval.colorObject();
			index = quantize(atom.getSelectedGroupIndexWithinChain(), 0, atom.getSelectedGroupCountWithinChain() - 1,
					JmolConstants.argbsRoygbScale.length);
			index = JmolConstants.argbsRoygbScale.length - 1 - index;
			argb = JmolConstants.argbsRoygbScale[index];
			break;
		case JmolConstants.PALETTE_MONOMER:
			// viewer.calcSelectedMonomersCount() must be called first ...
			index = quantize(atom.getSelectedMonomerIndexWithinPolymer(), 0, atom
					.getSelectedMonomerCountWithinPolymer() - 1, JmolConstants.argbsRoygbScale.length);
			index = JmolConstants.argbsRoygbScale.length - 1 - index;
			argb = JmolConstants.argbsRoygbScale[index];
			break;
		case JmolConstants.PALETTE_MOLECULE:
			frame = viewer.getFrame();
			index = quantize(frame.getMoleculeIndex(atom.atomIndex), 0,
					frame.getMoleculeCountInModel(atom.modelIndex) - 1, JmolConstants.argbsRoygbScale.length);
			argb = JmolConstants.argbsRoygbScale[index];
			break;
		case JmolConstants.PALETTE_ALTLOC:
			frame = viewer.getFrame();
			// very inefficient!
			index = quantize(frame.getAltLocIndexInModel(atom.modelIndex, (char) atom.alternateLocationID), 0, frame
					.getAltLocCountInModel(atom.modelIndex), JmolConstants.argbsRoygbScale.length);
			argb = JmolConstants.argbsRoygbScale[index];
			break;
		case JmolConstants.PALETTE_INSERTION:
			frame = viewer.getFrame();
			// very inefficient!
			index = quantize(frame.getInsertionCodeIndexInModel(atom.modelIndex, atom.getInsertionCode()), 0, frame
					.getInsertionCountInModel(atom.modelIndex), JmolConstants.argbsRoygbScale.length);
			argb = JmolConstants.argbsRoygbScale[index];
			break;
		}
		return (argb == 0 ? Graphics3D.HOTPINK : Graphics3D.getColix(argb));
	}

	int quantize(float val, float lo, float hi, int segmentCount) {
		/*
		 * oy! Say you have an array with 10 values, so segmentCount=10 then we expect 0,1,2,...,9 EVENLY If f =
		 * fractional distance from lo to hi, say 0.0 to 10.0 again, then one might expect 10 even placements. BUT:
		 * (int) (f * segmentCount + 0.5) gives
		 * 
		 * 0.0 ---> 0 0.5 ---> 1 1.0 ---> 1 1.5 ---> 2 2.0 ---> 2 ... 8.5 ---> 9 9.0 ---> 9 9.5 ---> 10 --> 9
		 * 
		 * so the first bin is underloaded, and the last bin is overloaded. With integer quantities, one would not
		 * notice this, because 0, 1, 2, 3, .... --> 0, 1, 2, 3, .....
		 * 
		 * but with fractional quantities, it will be noticeable.
		 * 
		 * What we really want is:
		 * 
		 * 0.0 ---> 0 0.5 ---> 0 1.0 ---> 1 1.5 ---> 1 2.0 ---> 2 ... 8.5 ---> 8 9.0 ---> 9 9.5 ---> 9
		 * 
		 * that is, no addition of 0.5. Instead, I add 0.0001, just for discreteness sake.
		 * 
		 * Bob Hanson, 5/2006
		 * 
		 */
		float range = hi - lo;
		if (range <= 0 || Float.isNaN(val))
			return segmentCount / 2;
		float t = val - lo;
		if (t <= 0)
			return 0;
		float quanta = range / segmentCount;
		int q = (int) (t / quanta + 0.0001f); // was 0.5f!
		if (q >= segmentCount)
			q = segmentCount - 1;
		return q;
	}

	short getColixFromPalette(float val, float lo, float hi, String palette) {
		if (palette.equals("rwb")) {
			int index = quantize(val, lo, hi, JmolConstants.argbsRwbScale.length);
			return Graphics3D.getColix(JmolConstants.argbsRwbScale[index]);
		}
		if (palette.equals("roygb")) {
			int index = quantize(val, lo, hi, JmolConstants.argbsRoygbScale.length);
			return Graphics3D.getColix(JmolConstants.argbsRoygbScale[index]);
		}
		int ihalf = JmolConstants.argbsRoygbScale.length / 2;
		if (palette.equals("low")) {
			int index = quantize(val, lo, hi, ihalf);
			return Graphics3D.getColix(JmolConstants.argbsRoygbScale[index]);
		}

		if (palette.equals("high")) {
			int index = quantize(val, lo, hi, ihalf);
			return Graphics3D.getColix(JmolConstants.argbsRoygbScale[ihalf + index]);
		}
		return Graphics3D.GRAY;
	}

	short getColixHbondType(short order) {
		int argbIndex = ((order & JmolConstants.BOND_HYDROGEN_MASK) >> JmolConstants.BOND_HBOND_SHIFT);
		return Graphics3D.getColix(JmolConstants.argbsHbondType[argbIndex]);
	}

	void flushCachedColors() {
	}

	private void flushCaches() {
		g3d.flushShadesAndImageCaches();
		viewer.refresh(0, "ColorManager:flushCaches()");
	}

	void setSpecular(boolean specular) {
		g3d.setSpecular(specular);
		flushCaches();
	}

	boolean getSpecular() {
		return g3d.getSpecular();
	}

	void setSpecularPower(int specularPower) {
		g3d.setSpecularPower(specularPower);
		flushCaches();
	}

	void setAmbientPercent(int ambientPercent) {
		g3d.setAmbientPercent(ambientPercent);
		flushCaches();
	}

	void setDiffusePercent(int diffusePercent) {
		g3d.setDiffusePercent(diffusePercent);
		flushCaches();
	}

	void setSpecularPercent(int specularPercent) {
		g3d.setSpecularPercent(specularPercent);
		flushCaches();
	}

	void setLightsourceZ(float dist) {
		g3d.setLightsourceZ(dist);
		flushCaches();
	}

	int getJmolOrRasmolArgb(int id, int argb) {
		if (argb == Token.jmol) {
			return (id < 256 ? JmolConstants.argbsCpk[id] : JmolConstants.altArgbsCpk[JmolConstants
					.altElementIndexFromNumber(id)]);
		}
		if (argb == Token.rasmol) {
			if (id >= 256)
				return JmolConstants.altArgbsCpk[JmolConstants.altElementIndexFromNumber(id)];
			argb = JmolConstants.argbsCpk[id];
			for (int i = JmolConstants.argbsCpkRasmol.length; --i >= 0;) {
				int argbRasmol = JmolConstants.argbsCpkRasmol[i];
				int atomNo = argbRasmol >> 24;
				if (atomNo == id) {
					argb = argbRasmol | 0xFF000000;
					break;
				}
			}
			return argb;
		}
		return argb |= 0xFF000000;
	}

	void setElementArgb(int id, int argb) {
		if (argb == Token.jmol && argbsCpk == JmolConstants.argbsCpk)
			return;
		argb = getJmolOrRasmolArgb(id, argb);
		if (argbsCpk == JmolConstants.argbsCpk)
			copyArgbsCpk();
		if (id < 256) {
			argbsCpk[id] = argb;
			g3d.changeColixArgb((short) id, argb);
			return;
		}
		id = JmolConstants.altElementIndexFromNumber(id);
		altArgbsCpk[JmolConstants.altElementIndexFromNumber(id)] = argb;
		g3d.changeColixArgb((short) (JmolConstants.elementNumberMax + id), argb);
	}
}
