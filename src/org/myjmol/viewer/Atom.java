/* $RCSfile: Atom.java,v $
 * $Author: qxie $
 * $Date: 2007-09-13 17:35:56 $
 * $Revision: 1.28 $

 *
 * Copyright (C) 2003-2005  The Jmol Development Team
 *
 * Contact: jmol-developers@lists.sf.net
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

import org.myjmol.bspt.Tuple;
import org.myjmol.g3d.Graphics3D;
import org.myjmol.vecmath.Point3fi;

import java.util.Hashtable;
import java.util.BitSet;

import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;
import javax.vecmath.Point3i;

@SuppressWarnings("unchecked")
final public class Atom extends Point3fi implements Tuple {

	final static byte VIBRATION_VECTOR_FLAG = 0x02;
	final static byte IS_HETERO_FLAG = 0x04;
	final static byte FORMALCHARGE_FLAGS = 0x07;
	private final static java.awt.Dimension SCREEN_SIZE = java.awt.Toolkit.getDefaultToolkit().getScreenSize();

	Group group;
	int atomIndex;
	BitSet atomSymmetry;
	int atomSite;
	short screenDiameter;
	short modelIndex; // we want this here for the BallsRenderer
	private short atomicAndIsotopeNumber;
	byte formalChargeAndFlags;
	byte alternateLocationID;
	short madAtom;
	short colixAtom, colixCustom;
	byte paletteID = JmolConstants.PALETTE_CPK;
	short sigma = -1; // XIE in the unit of MAD
	boolean annotationKey; // XIE: this indicates that there is an annotation key on this atom
	boolean interactionKey; // XIE: this indicates that there is an interaction key on this atom
	AtomPin pin; // XIE
	short annotationKeyColix = Graphics3D.GOLD; // XIE: annotation key color
	short interactionKeyColix = Graphics3D.OLIVE; // XIE: interaction key color
	boolean visible = true;

	Bond[] bonds;
	int nBondsDisplayed = 0;
	int nBackbonesDisplayed = 0;
	int clickabilityFlags;
	int shapeVisibilityFlags;
	boolean isSimple = false;

	// miguel 2006 03 25
	// we should talk about this. if you need simple "points" for your shapes
	// then we should consider splitting out the 'pointness' from the
	// atom and have atom inherit from point
	Atom(Point3f pt) {
		// just a point -- just enough to determine a position
		isSimple = true;
		this.x = pt.x;
		this.y = pt.y;
		this.z = pt.z;
		// must be transformed later -- Polyhedra;
		formalChargeAndFlags = 0;
		madAtom = 0;
	}

	Atom(Frame frame, int modelIndex, int atomIndex, BitSet atomSymmetry, int atomSite, short atomicAndIsotopeNumber,
			String atomName, short mad, int formalCharge, float partialCharge, int occupancy, float bfactor, float x,
			float y, float z, boolean isHetero, int atomSerial, char chainID, String group3, float vibrationX,
			float vibrationY, float vibrationZ, char alternateLocationID, Object clientAtomReference) {
		this.group = frame.nullGroup;
		this.modelIndex = (short) modelIndex;
		this.atomSymmetry = atomSymmetry;
		this.atomSite = atomSite;
		this.atomIndex = atomIndex;
		this.atomicAndIsotopeNumber = atomicAndIsotopeNumber;
		setFormalCharge(formalCharge);
		this.colixAtom = frame.viewer.getColixAtomPalette(this, JmolConstants.PALETTE_CPK);
		this.alternateLocationID = (byte) alternateLocationID;
		if (mad != 0) // XIE
			setMadAtom(mad);
		this.x = x;
		this.y = y;
		this.z = z;
		if (isHetero)
			formalChargeAndFlags |= IS_HETERO_FLAG;

		if (atomName != null) {
			if (frame.atomNames == null)
				frame.atomNames = new String[frame.atoms.length];
			frame.atomNames[atomIndex] = atomName.intern();
		}

		byte specialAtomID = lookupSpecialAtomID(atomName);
		if (specialAtomID == JmolConstants.ATOMID_ALPHA_CARBON && "CA".equalsIgnoreCase(group3)) {
			specialAtomID = 0;
		}
		// Logger.debug("atom - "+atomName+" specialAtomID=" + specialAtomID);
		if (specialAtomID != 0) {
			if (frame.specialAtomIDs == null)
				frame.specialAtomIDs = new byte[frame.atoms.length];
			frame.specialAtomIDs[atomIndex] = specialAtomID;
		}

		if (occupancy < 0)
			occupancy = 0;
		else if (occupancy > 100)
			occupancy = 100;
		if (occupancy != 100) {
			if (frame.occupancies == null)
				frame.occupancies = new byte[frame.atoms.length];
			frame.occupancies[atomIndex] = (byte) occupancy;
		}

		if (atomSerial != Integer.MIN_VALUE) {
			if (frame.atomSerials == null)
				frame.atomSerials = new int[frame.atoms.length];
			frame.atomSerials[atomIndex] = atomSerial;
		}

		if (!Float.isNaN(partialCharge)) {
			if (frame.partialCharges == null)
				frame.partialCharges = new float[frame.atoms.length];
			frame.partialCharges[atomIndex] = partialCharge;
		}

		if (!Float.isNaN(bfactor) && bfactor != 0) {
			if (frame.bfactor100s == null)
				frame.bfactor100s = new short[frame.atoms.length];
			frame.bfactor100s[atomIndex] = (short) (bfactor * 100);
		}

		if (!Float.isNaN(vibrationX) && !Float.isNaN(vibrationY) && !Float.isNaN(vibrationZ)) {
			if (frame.vibrationVectors == null)
				frame.vibrationVectors = new Vector3f[frame.atoms.length];
			frame.vibrationVectors[atomIndex] = new Vector3f(vibrationX, vibrationY, vibrationZ);
			formalChargeAndFlags |= VIBRATION_VECTOR_FLAG;
		}
		if (clientAtomReference != null) {
			if (frame.clientAtomReferences == null)
				frame.clientAtomReferences = new Object[frame.atoms.length];
			frame.clientAtomReferences[atomIndex] = clientAtomReference;
		}
	}

	private static Hashtable htAtom = new Hashtable();
	static {
		for (int i = JmolConstants.specialAtomNames.length; --i >= 0;) {
			String specialAtomName = JmolConstants.specialAtomNames[i];
			if (specialAtomName != null) {
				Integer boxedI = new Integer(i);
				htAtom.put(specialAtomName, boxedI);
			}
		}
	}

	/*
	 * static String generateStarredAtomName(String primedAtomName) { int primeIndex = primedAtomName.indexOf('\''); if
	 * (primeIndex < 0) return null; return primedAtomName.replace('\'', '*'); }
	 */

	static String generatePrimeAtomName(String starredAtomName) {
		int starIndex = starredAtomName.indexOf('*');
		if (starIndex < 0)
			return starredAtomName;
		return starredAtomName.replace('*', '\'');
	}

	boolean isGenericAtom() {
		return atomicAndIsotopeNumber >= 110;
	}

	byte lookupSpecialAtomID(String atomName) {
		if (atomName != null) {
			atomName = generatePrimeAtomName(atomName);
			Integer boxedAtomID = (Integer) htAtom.get(atomName);
			if (boxedAtomID != null)
				return (byte) (boxedAtomID.intValue());
		}
		return 0;
	}

	final void setShapeVisibility(int shapeVisibilityFlag, boolean isVisible) {
		if (isVisible) {
			shapeVisibilityFlags |= shapeVisibilityFlag;
		}
		else {
			shapeVisibilityFlags &= ~shapeVisibilityFlag;
		}
	}

	void setFormalCharge(int charge) {
		// note,this may be negative
		formalChargeAndFlags = (byte) ((formalChargeAndFlags & FORMALCHARGE_FLAGS) | (charge << 3));
	}

	boolean isBonded(Atom atomOther) {
		if (bonds != null)
			for (int i = bonds.length; --i >= 0;)
				if (bonds[i].getOtherAtom(this) == atomOther)
					return true;
		return false;
	}

	Bond getBond(Atom atomOther) {
		if (bonds != null)
			for (int i = bonds.length; --i >= 0;)
				if (bonds[i].getOtherAtom(atomOther) != null)
					return bonds[i];
		return null;
	}

	void addDisplayedBond(int stickVisibilityFlag, boolean isVisible) {
		nBondsDisplayed += (isVisible ? 1 : -1);
		setShapeVisibility(stickVisibilityFlag, isVisible);
	}

	void addDisplayedBackbone(int backboneVisibilityFlag, boolean isVisible) {
		nBackbonesDisplayed += (isVisible ? 1 : -1);
		setShapeVisibility(backboneVisibilityFlag, isVisible);
	}

	void deleteBond(Bond bond) {
		// this one is used -- from Bond.deleteAtomReferences
		for (int i = bonds.length; --i >= 0;)
			if (bonds[i] == bond) {
				deleteBond(i);
				return;
			}
	}

	private void deleteBond(int i) {
		int newLength = bonds.length - 1;
		if (newLength == 0) {
			bonds = null;
			return;
		}
		Bond[] bondsNew = new Bond[newLength];
		int j = 0;
		for (; j < i; ++j)
			bondsNew[j] = bonds[j];
		for (; j < newLength; ++j)
			bondsNew[j] = bonds[j + 1];
		bonds = bondsNew;
	}

	void clearBonds() {
		bonds = null;
	}

	int getBondedAtomIndex(int bondIndex) {
		return bonds[bondIndex].getOtherAtom(this).atomIndex;
	}

	/*
	 * What is a MAR? - just a term that Miguel made up - an abbreviation for Milli Angstrom Radius that is: - a
	 * *radius* of either a bond or an atom - in *millis*, or thousandths of an *angstrom* - stored as a short
	 * 
	 * However! In the case of an atom radius, if the parameter gets passed in as a negative number, then that number
	 * represents a percentage of the vdw radius of that atom. This is converted to a normal MAR as soon as possible
	 * 
	 * (I know almost everyone hates bytes & shorts, but I like them ... gives me some tiny level of type-checking ... a
	 * rudimentary form of enumerations/user-defined primitive types)
	 */

	void setMadAtom(short madAtom) {
		if (this.madAtom == JmolConstants.MAR_DELETED)
			return;
		this.madAtom = convertEncodedMad(madAtom);
	}

	short convertEncodedMad(int size) {
		if (size == 0)
			return 0;
		if (size == -1000) { // temperature
			int diameter = getBfactor100() * 10 * 2;
			if (diameter > 4000)
				diameter = 4000;
			size = diameter;
		}
		else if (size == -1001) // ionic
			size = (getBondingMar() * 2);
		else if (size < 0) {
			size = -size;
			if (size > 200)
				size = 200;
			size = // we are going from a radius to a diameter
			(int) (size / 100f * getVanderwaalsMar() * 2);
		}
		else if (size >= 10000) {
			// radiusAngstroms = vdw + x, where size = (x*2)*1000 + 10000
			// and vdwMar = vdw * 1000
			// we want mad = diameterAngstroms * 1000 = (radiusAngstroms *2)*1000
			// = (vdw * 2 * 1000) + x * 2 * 1000
			// = vdwMar * 2 + (size - 10000)
			size = size - 10000 + getVanderwaalsMar() * 2;
		}
		return (short) size;
	}

	int getRasMolRadius() {
		if (madAtom == JmolConstants.MAR_DELETED)
			return 0;
		return madAtom / (4 * 2);
	}

	int getCovalentBondCount() {
		if (bonds == null)
			return 0;
		int n = 0;
		for (int i = bonds.length; --i >= 0;)
			if ((bonds[i].order & JmolConstants.BOND_COVALENT_MASK) != 0)
				++n;
		return n;
	}

	int getCovalentHydrogenCount() {
		if (bonds == null)
			return 0;
		int n = 0;
		for (int i = bonds.length; --i >= 0;)
			if ((bonds[i].order & JmolConstants.BOND_COVALENT_MASK) != 0
					&& (bonds[i].getOtherAtom(this).getElementNumber()) == 1)
				++n;
		return n;
	}

	Bond[] getBonds() {
		return bonds;
	}

	void setColixAtom(short colixAtom) {
		this.colixAtom = colixAtom;
	}

	void setPaletteID(byte paletteID) {
		this.paletteID = paletteID;
	}

	void setTranslucent(boolean isTranslucent) {
		colixAtom = Graphics3D.getColixTranslucent(colixAtom, isTranslucent);
	}

	boolean isTranslucent() {
		return Graphics3D.isColixTranslucent(colixAtom);
	}

	short getElementNumber() {
		return (short) (atomicAndIsotopeNumber % 128);
	}

	short getIsotopeNumber() {
		return (short) (atomicAndIsotopeNumber >> 7);
	}

	short getAtomicAndIsotopeNumber() {
		return atomicAndIsotopeNumber;
	}

	void setAtomicAndIsotopeNumber(short i) {
		atomicAndIsotopeNumber = i;
	}

	String getElementSymbol() {
		return JmolConstants.elementSymbolFromNumber(atomicAndIsotopeNumber);
	}

	boolean isAlternateLocationMatch(String strPattern) {
		if (strPattern == null)
			return (alternateLocationID == 0);
		if (strPattern.length() != 1)
			return false;
		char ch = strPattern.charAt(0);
		return (ch == '*' || ch == '?' && alternateLocationID != '\0' || alternateLocationID == ch);
	}

	boolean isHetero() {
		return (formalChargeAndFlags & IS_HETERO_FLAG) != 0;
	}

	int getFormalCharge() {
		return formalChargeAndFlags >> 3;
	}

	float getAtomX() {
		return x;
	}

	float getAtomY() {
		return y;
	}

	float getAtomZ() {
		return z;
	}

	public float getDimensionValue(int dimension) {
		return (dimension == 0 ? x : (dimension == 1 ? y : z));
	}

	short getVanderwaalsMar() {
		return JmolConstants.vanderwaalsMars[atomicAndIsotopeNumber % 128];
	}

	float getVanderwaalsRadiusFloat() {
		return JmolConstants.vanderwaalsMars[atomicAndIsotopeNumber % 128] / 1000f;
	}

	short getBondingMar() {
		return JmolConstants.getBondingMar(atomicAndIsotopeNumber % 128, formalChargeAndFlags >> 3);
	}

	float getBondingRadiusFloat() {
		return getBondingMar() / 1000f;
	}

	int getCurrentBondCount() {
		return bonds == null ? 0 : bonds.length;
		/*
		 * int currentBondCount = 0; for (int i = (bonds == null ? 0 : bonds.length); --i >= 0; ) currentBondCount +=
		 * bonds[i].order & JmolConstants.BOND_COVALENT; return currentBondCount;
		 */
	}

	// find the longest bond to discard
	// but return null if atomChallenger is longer than any
	// established bonds
	// note that this algorithm works when maximum valence == 0
	Bond getLongestBondToDiscard(Atom atomChallenger) {
		float dist2Longest = distanceSquared(atomChallenger);
		Bond bondLongest = null;
		for (int i = bonds.length; --i >= 0;) {
			Bond bond = bonds[i];
			float dist2 = distanceSquared(bond.getOtherAtom(this));
			if (dist2 > dist2Longest) {
				bondLongest = bond;
				dist2Longest = dist2;
			}
		}
		// Logger.debug("atom at " + point3f + " suggests discard of " +
		// bondLongest + " dist2=" + dist2Longest);
		return bondLongest;
	}

	short getColix() {
		return colixAtom;
	}

	byte getPaletteID() {
		return paletteID;
	}

	float getRadius() {
		if (madAtom == JmolConstants.MAR_DELETED)
			return 0;
		return madAtom / (1000f * 2);
	}

	int getAtomIndex() {
		return atomIndex;
	}

	int getAtomSite() {
		return atomSite;
	}

	BitSet getAtomSymmetry() {
		return atomSymmetry;
	}

	boolean isInLatticeCell(Point3f cell) {
		return isInLatticeCell(cell, 0.02f);
	}

	boolean isInLatticeCell(Point3f cell, float slop) {
		Point3f pt = getFractionalCoord();
		// {1 1 1} here is the original cell
		if (pt.x < cell.x - 1f - slop || pt.x > cell.x + slop)
			return false;
		if (pt.y < cell.y - 1f - slop || pt.y > cell.y + slop)
			return false;
		if (pt.z < cell.z - 1f - slop || pt.z > cell.z + slop)
			return false;
		return true;
	}

	void setGroup(Group group) {
		this.group = group;
	}

	Group getGroup() {
		return group;
	}

	// the following methods will work anytime, since we now have
	// a dummy group and chain

	Vector3f getVibrationVector() {
		Vector3f[] vibrationVectors = group.chain.frame.vibrationVectors;
		return vibrationVectors == null ? null : vibrationVectors[atomIndex];
	}

	void transform(Viewer viewer) {
		Point3i screen;
		Vector3f[] vibrationVectors;
		if ((formalChargeAndFlags & VIBRATION_VECTOR_FLAG) == 0 || group == null || // XIE
				(vibrationVectors = group.chain.frame.vibrationVectors) == null)
			screen = viewer.transformPoint(this);
		else screen = viewer.transformPoint(this, vibrationVectors[atomIndex]);
		screenX = screen.x;
		screenY = screen.y;
		screenZ = screen.z;
		screenDiameter = viewer.scaleToScreen(screenZ, madAtom);
	}

	String getAtomNameOrNull() {
		if (group == null)
			return JmolConstants.elementSymbols[atomicAndIsotopeNumber]; // XIE
		String[] atomNames = group.chain.frame.atomNames;
		return atomNames == null ? null : atomNames[atomIndex];
	}

	String getAtomName() {
		String atomName = getAtomNameOrNull();
		return (atomName != null ? atomName : getElementSymbol());
	}

	String getPdbAtomName4() {
		String atomName = getAtomNameOrNull();
		return atomName != null ? atomName : "";
	}

	/**
	 * matches atom name possibly with wildcard
	 * 
	 * @param strPattern --
	 *            for efficiency, upper case already
	 * @return true/false
	 */
	boolean isAtomNameMatch(String strPattern) {
		String atomName = getAtomNameOrNull();
		int cchAtomName = atomName == null ? 0 : atomName.length();
		int cchPattern = strPattern.length();
		int ich;
		for (ich = 0; ich < cchPattern; ++ich) {
			char charWild = strPattern.charAt(ich);
			if (charWild == '?')
				continue;
			if (ich >= cchAtomName || (atomName != null && charWild != Character.toUpperCase(atomName.charAt(ich))))
				return false;
		}
		return ich >= cchAtomName;
	}

	int getAtomNumber() {
		if (group == null)
			return atomIndex + 1; // XIE
		int[] atomSerials = group.chain.frame.atomSerials;
		if (atomSerials != null)
			return atomSerials[atomIndex];
		if (group.chain.frame.isZeroBased)
			return atomIndex;
		return atomIndex + 1;
	}

	boolean isModelVisible() {
		return ((shapeVisibilityFlags & JmolConstants.ATOM_IN_MODEL) != 0);
	}

	boolean isShapeVisible(int shapeVisibilityFlag) {
		return (isModelVisible() && (shapeVisibilityFlags & shapeVisibilityFlag) != 0);
	}

	float getPartialCharge() {
		float[] partialCharges = group.chain.frame.partialCharges;
		return partialCharges == null ? 0 : partialCharges[atomIndex];
	}

	int getArgb() {
		return group.chain.frame.viewer.getColixArgb(colixAtom);
	}

	// a percentage value in the range 0-100
	int getOccupancy() {
		byte[] occupancies = group.chain.frame.occupancies;
		return occupancies == null ? 100 : occupancies[atomIndex];
	}

	// This is called bfactor100 because it is stored as an integer
	// 100 times the bfactor(temperature) value
	int getBfactor100() {
		short[] bfactor100s = group.chain.frame.bfactor100s;
		if (bfactor100s == null)
			return 0;
		return bfactor100s[atomIndex];
	}

	int getSymmetryTranslation(int symop) {
		Frame.CellInfo[] c = group.chain.frame.cellInfos;
		if (c == null)
			return 0;
		Vector3f pt0 = new Vector3f(getFractionalCoord());
		pt0.sub(group.chain.frame.getSymmetryBaseAtom(modelIndex, atomSite, symop).getFractionalCoord());
		return ((int) (pt0.x + 5.01)) * 100 + ((int) (pt0.y + 5.01)) * 10 + ((int) (pt0.z + 5.01));
	}

	String getSymmetryOperatorList() {
		String str = "";
		if (atomSymmetry == null)
			return str;
		for (int i = 0; i < atomSymmetry.size(); i++)
			if (atomSymmetry.get(i))
				str += "," + (i + 1);
		return str.substring(1);
	}

	int getModelIndex() {
		return modelIndex;
	}

	int getMoleculeNumber() {
		return (group.chain.frame.getMoleculeIndex(atomIndex) + 1);
	}

	String getClientAtomStringProperty(String propertyName) {
		Object[] clientAtomReferences = group.chain.frame.clientAtomReferences;
		return ((clientAtomReferences == null || clientAtomReferences.length <= atomIndex) ? null
				: (group.chain.frame.viewer.getClientAtomStringProperty(clientAtomReferences[atomIndex], propertyName)));
	}

	boolean isDeleted() {
		return madAtom == JmolConstants.MAR_DELETED;
	}

	byte getSpecialAtomID() {
		byte[] specialAtomIDs = group.chain.frame.specialAtomIDs;
		return specialAtomIDs == null ? 0 : specialAtomIDs[atomIndex];
	}

	private float getFractionalCoord(char ch) {
		Point3f pt = getFractionalCoord();
		return (ch == 'X' ? pt.x : ch == 'Y' ? pt.y : pt.z);
	}

	Point3f getFractionalCoord() {
		Frame.CellInfo[] c = group.chain.frame.cellInfos;
		if (c == null)
			return this;
		Point3f pt = new Point3f(this);
		c[modelIndex].toFractional(pt);
		return pt;
	}

	boolean isCursorOnTopOf(int xCursor, int yCursor, int minRadius, Atom competitor) {
		// XIE: the following should be used to prevent dx2 or dy2 > Integer.MAX_VALUE
		if (screenX < 0 || screenX > SCREEN_SIZE.width || screenY < 0 || screenY > SCREEN_SIZE.height)
			return false;
		int r = screenDiameter / 2;
		if (r < minRadius)
			r = minRadius;
		int r2 = r * r;
		int dx = screenX - xCursor;
		int dx2 = dx * dx;
		if (dx2 > r2)
			return false;
		int dy = screenY - yCursor;
		int dy2 = dy * dy;
		int dz2 = r2 - (dx2 + dy2);
		if (dz2 < 0)
			return false;
		if (competitor == null)
			return true;
		int z = screenZ;
		int zCompetitor = competitor.screenZ;
		int rCompetitor = competitor.screenDiameter / 2;
		if (z < zCompetitor - rCompetitor)
			return true;
		int dxCompetitor = competitor.screenX - xCursor;
		int dx2Competitor = dxCompetitor * dxCompetitor;
		int dyCompetitor = competitor.screenY - yCursor;
		int dy2Competitor = dyCompetitor * dyCompetitor;
		int r2Competitor = rCompetitor * rCompetitor;
		int dz2Competitor = r2Competitor - (dx2Competitor + dy2Competitor);
		return (z - Math.sqrt(dz2) < zCompetitor - Math.sqrt(dz2Competitor));
	}

	/*******************************************************************************************************************
	 * disabled until I (Miguel) figure out how to generate pretty names without breaking inorganic compounds // this
	 * requires a 4 letter name, in PDB format // only here for transition purposes static String calcPrettyName(String
	 * name) { if (name.length() < 4) return name; char chBranch = name.charAt(3); char chRemote = name.charAt(2);
	 * switch (chRemote) { case 'A': chRemote = '\u03B1'; break; case 'B': chRemote = '\u03B2'; break; case 'C': case
	 * 'G': chRemote = '\u03B3'; break; case 'D': chRemote = '\u03B4'; break; case 'E': chRemote = '\u03B5'; break; case
	 * 'Z': chRemote = '\u03B6'; break; case 'H': chRemote = '\u03B7'; } String pretty = name.substring(0, 2).trim(); if
	 * (chBranch != ' ') pretty += "" + chRemote + chBranch; else pretty += chRemote; return pretty; }
	 * 
	 */

	/*
	 * DEVELOPER NOTE (BH):
	 * 
	 * The following methods may not return correct values until after frame.finalizeGroupBuild()
	 * 
	 */

	String getInfo() {
		return getIdentity();
	}

	String getInfoXYZ() {
		return getIdentity() + " " + x + " " + y + " " + z;
	}

	String getIdentity() {
		StringBuffer info = new StringBuffer();
		String group3 = getGroup3();
		String seqcodeString = getSeqcodeString();
		char chainID = getChainID();
		if (group3 != null && group3.length() > 0) {
			info.append("[");
			info.append(group3);
			info.append("]");
		}
		if (seqcodeString != null)
			info.append(seqcodeString);
		if (chainID != 0 && chainID != ' ') {
			info.append(":");
			info.append(chainID);
		}
		String atomName = getAtomNameOrNull();
		if (atomName != null) {
			if (info.length() > 0)
				info.append(".");
			info.append(atomName);
		}
		if (info.length() == 0) {
			info.append(getElementSymbol());
			info.append(" ");
			info.append(getAtomNumber());
		}
		if (alternateLocationID > 0) {
			info.append("%");
			info.append((char) alternateLocationID);
		}
		if (group != null) { // XIE
			if (group.chain.frame.getModelCount() > 1) {
				info.append("/");
				info.append(getModelTagNumber());
			}
		}
		info.append(" #");
		info.append(getAtomNumber());
		return "" + info;
	}

	String getGroup3() {
		if (group == null)
			return null; // XIE
		return group.getGroup3();
	}

	String getGroup1() {
		return group.getGroup1();
	}

	boolean isGroup3(String group3) {
		return group.isGroup3(group3);
	}

	boolean isGroup3Match(String strWildcard) {
		return group.isGroup3Match(strWildcard);
	}

	boolean isProtein() {
		return group.isProtein();
	}

	boolean isCarbohydrate() {
		return group.isCarbohydrate();
	}

	boolean isNucleic() {
		return group.isNucleic();
	}

	boolean isDna() {
		return group.isDna();
	}

	boolean isRna() {
		return group.isRna();
	}

	boolean isPurine() {
		return group.isPurine();
	}

	boolean isPyrimidine() {
		return group.isPyrimidine();
	}

	int getSeqcode() {
		if (group == null)
			return 0; // XIE
		return group.getSeqcode();
	}

	int getResno() {
		return group.getResno();
	}

	boolean isGroup3OrNameMatch(String strPattern) {
		return (getGroup3().length() > 0 ? isGroup3Match(strPattern) : isAtomNameMatch(strPattern));
	}

	boolean isClickable() {
		// certainly if it is not visible, then it can't be clickable
		if (!isVisible())
			return false;
		int flags = shapeVisibilityFlags | group.shapeVisibilityFlags;
		return ((flags & clickabilityFlags) != 0);
	}

	/**
	 * determine if an atom or its PDB group is visible
	 * 
	 * @return true if the atom is in the "select visible" set
	 */
	boolean isVisible() {
		// Is the atom's model visible? Is the atom NOT hidden?
		if (!isModelVisible() || group.chain.frame.bsHidden.get(atomIndex))
			return false;
		// Is any shape associated with this atom visible?
		int flags = shapeVisibilityFlags;
		// Is its PDB group visible in any way (cartoon, e.g.)?
		// An atom is considered visible if its PDB group is visible, even
		// if it does not show up itself as part of the structure
		// (this will be a difference in terms of *clickability*).
		flags |= group.shapeVisibilityFlags;
		// We know that (flags & AIM), so now we must remove that flag
		// and check to see if any others are remaining.
		// Only then is the atom considered visible.
		return ((flags & ~JmolConstants.ATOM_IN_MODEL) != 0);
	}

	float getGroupPhi() {
		return group.phi;
	}

	float getGroupPsi() {
		return group.psi;
	}

	char getChainID() {
		if (group == null)
			return '0'; // XIE
		return group.chain.chainID;
	}

	float getSurfaceDistance() {
		return group.chain.frame.getSurfaceDistance(atomIndex);
	}

	int getPolymerLength() {
		return group.getPolymerLength();
	}

	int getPolymerIndex() {
		return group.getPolymerIndex();
	}

	int getSelectedGroupCountWithinChain() {
		return group.chain.getSelectedGroupCount();
	}

	int getSelectedGroupIndexWithinChain() {
		return group.chain.getSelectedGroupIndex(group);
	}

	int getSelectedMonomerCountWithinPolymer() {
		if (group instanceof Monomer) {
			return ((Monomer) group).polymer.selectedMonomerCount;
		}
		return 0;
	}

	int getSelectedMonomerIndexWithinPolymer() {
		if (group instanceof Monomer) {
			Monomer monomer = (Monomer) group;
			return monomer.polymer.getSelectedMonomerIndex(monomer);
		}
		return -1;
	}

	Chain getChain() {
		return group.chain;
	}

	Model getModel() {
		return group.chain.model;
	}

	int getModelNumber() {
		return group.chain.model.modelNumber;
	}

	String getModelTag() {
		return group.chain.model.modelTag;
	}

	int getModelTagNumber() {
		if (group.chain.model.isPDB) {
			try {
				return Integer.parseInt(group.chain.model.modelTag);
			}
			catch (Exception e) {
			}
		}
		return getModelNumber();
	}

	byte getProteinStructureType() {
		return group.getProteinStructureType();
	}

	short getGroupID() {
		return group.groupID;
	}

	String getSeqcodeString() {
		return group.getSeqcodeString();
	}

	int getSeqNumber() {
		return group.getSeqNumber();
	}

	char getInsertionCode() {
		return group.getInsertionCode();
	}

	String formatLabel(String strFormat) {
		return formatLabel(strFormat, '\0', 0, "");
	}

	String formatLabel(String strFormat, char chAtom, float thisValue, String units) {
		if (strFormat == null || strFormat.length() == 0)
			return null;
		String strLabel = "";
		int cch = strFormat.length();
		int ich, ichPercent;
		for (ich = 0; (ichPercent = strFormat.indexOf('%', ich)) != -1;) {
			if (ich != ichPercent)
				strLabel += strFormat.substring(ich, ichPercent);
			ich = ichPercent + 1;
			try {
				String strT = "";
				float floatT = Float.NaN;
				boolean alignLeft = false;
				if (strFormat.charAt(ich) == '-') {
					alignLeft = true;
					++ich;
				}
				boolean zeroPad = false;
				if (strFormat.charAt(ich) == '0') {
					zeroPad = true;
					++ich;
				}
				char ch;
				int width = 0;
				while ((ch = strFormat.charAt(ich)) >= '0' && (ch <= '9')) {
					width = (10 * width) + (ch - '0');
					++ich;
				}
				int precision = -1;
				if (strFormat.charAt(ich) == '.') {
					++ich;
					if ((ch = strFormat.charAt(ich)) >= '0' && (ch <= '9')) {
						precision = ch - '0';
						++ich;
					}
				}
				/*
				 * the list:
				 * 
				 * case '%': case '{': case 'A': alternate location identifier case 'a': atom name case 'b': temperature
				 * factor ("b factor") case 'C': formal Charge case 'c': chain case 'D': atom inDex (was "X") case 'e':
				 * element symbol case 'i': atom number case 'I': Ionic radius case 'L': polymer Length case 'm': group1
				 * case 'M': Model number case 'n': group3 case 'N': molecule Number case 'o': symmetry operator set
				 * case 'P': Partial charge case 'q': occupancy case 'r': residue sequence code case 'S':
				 * crystllographic Site case 's': strand (chain) case 't': temperature factor case 'U': identity case
				 * 'u': sUrface distance or provided units case 'V': van der Waals case 'v': provided value case 'x': x
				 * coord case 'X': fractional X coord case 'y': y coord case 'Y': fractional Y coord case 'z': z coord
				 * case 'Z': fractional Z coord
				 * 
				 */
				char ch0 = ch = strFormat.charAt(ich++);

				if (chAtom != '\0' && ich < cch && ch != 'v' && ch != 'u') {
					if (strFormat.charAt(ich) == chAtom)
						strFormat = strFormat.substring(0, ich) + strFormat.substring(ich + 1);
					else ch = '\0'; // skip if not for this atom
				}
				switch (ch) {
				case 'i':
					strT = "" + getAtomNumber();
					break;
				case 'A':
					strT = (alternateLocationID != 0 ? ((char) alternateLocationID) + "" : "");
					break;
				case 'a':
					strT = getAtomName();
					break;
				case 'e':
					strT = getElementSymbol();
					break;
				case 'x':
					floatT = x;
					break;
				case 'y':
					floatT = y;
					break;
				case 'z':
					floatT = z;
					break;
				case 'X':
				case 'Y':
				case 'Z':
					floatT = getFractionalCoord(ch);
					break;
				case 'D':
					strT = "" + atomIndex;
					break;
				case 'C':
					int formalCharge = getFormalCharge();
					if (formalCharge > 0)
						strT = "" + formalCharge + "+";
					else if (formalCharge < 0)
						strT = "" + -formalCharge + "-";
					else strT = "0";
					break;
				case 'o':
					strT = getSymmetryOperatorList();
					break;
				case 'P':
					floatT = getPartialCharge();
					break;
				case 'V':
					floatT = getVanderwaalsRadiusFloat();
					break;
				case 'v':
					floatT = thisValue;
					break;
				case 'I':
					floatT = getBondingRadiusFloat();
					break;
				case 'b': // these two are the same
				case 't':
					floatT = getBfactor100() / 100f;
					break;
				case 'q':
					strT = "" + getOccupancy();
					break;
				case 'c': // these two are the same
				case 's':
					strT = "" + getChainID();
					break;
				case 'S':
					strT = "" + atomSite;
					break;
				case 'L':
					strT = "" + getPolymerLength();
					break;
				case 'M':
					strT = "" + getModelTagNumber();
					break;
				case 'm':
					strT = getGroup1();
					break;
				case 'n':
					strT = getGroup3();
					break;
				case 'r':
					strT = getSeqcodeString();
					break;
				case 'U':
					strT = getIdentity();
					break;
				case 'u':
					if (chAtom == '\0') {
						floatT = getSurfaceDistance();
					}
					else {
						strT = units;
					}
					break;
				case 'N':
					strT = "" + getMoleculeNumber();
					break;
				case '%':
					strT = "%";
					break;
				case '{': // client property name
					int ichCloseBracket = strFormat.indexOf('}', ich);
					if (ichCloseBracket > ich) { // also picks up -1 when no '}' is found
						String propertyName = strFormat.substring(ich, ichCloseBracket);
						String value = getClientAtomStringProperty(propertyName);
						if (value != null)
							strT = value;
						ich = ichCloseBracket + 1;
						break;
					}
					// malformed will fall into
				default:
					strT = "%" + ch0;
				}
				if (!Float.isNaN(floatT))
					strLabel += format(floatT, width, precision, alignLeft, zeroPad);
				else if (strT != null)
					strLabel += format(strT, width, precision, alignLeft, zeroPad);
			}
			catch (IndexOutOfBoundsException ioobe) {
				ich = ichPercent;
				break;
			}
		}
		strLabel += strFormat.substring(ich);
		if (strLabel.length() == 0)
			return null;
		return strLabel.intern();
	}

	String format(float value, int width, int precision, boolean alignLeft, boolean zeroPad) {
		return format(group.chain.frame.viewer.formatDecimal(value, precision), width, 0, alignLeft, zeroPad);
	}

	static String format(String value, int width, int precision, boolean alignLeft, boolean zeroPad) {
		if (value == null)
			return "";
		if (precision > value.length())
			value = value.substring(0, precision);
		int padLength = width - value.length();
		if (padLength <= 0)
			return value;
		boolean isNeg = (zeroPad && !alignLeft && value.charAt(0) == '-');
		char padChar = (zeroPad ? '0' : ' ');
		char padChar0 = (isNeg ? '-' : padChar);

		StringBuffer sb = new StringBuffer();
		if (alignLeft)
			sb.append(value);
		sb.append(padChar0);
		for (int i = padLength; --i > 0;)
			sb.append(padChar);
		if (!alignLeft)
			sb.append(isNeg ? padChar + value.substring(1) : value);
		return "" + sb;
	}
}
