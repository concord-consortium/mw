/* $RCSfile: Frame.java,v $
 * $Author: qxie $
 * $Date: 2007-11-06 21:32:53 $
 * $Revision: 1.35 $

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

import org.myjmol.api.JmolAdapter;
import org.myjmol.bspt.Bspf;
import org.myjmol.bspt.SphereIterator;
import org.myjmol.bspt.Tuple;
import org.myjmol.g3d.Graphics3D;
import org.myjmol.symmetry.UnitCell;
import org.myjmol.util.ArrayUtil;
import org.myjmol.util.Logger;

import javax.vecmath.Point3f;
import javax.vecmath.Matrix3f;
import javax.vecmath.Vector3f;
import javax.vecmath.AxisAngle4f;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;
import java.util.Vector;
import java.awt.Rectangle;

public final class Frame {

	Viewer viewer;
	FrameRenderer frameRenderer;
	private String modelSetTypeName;
	boolean isXYZ;
	boolean isPDB;
	boolean isMultiFile;
	boolean isArrayOfFiles;
	boolean isZeroBased;
	Mmset mmset;
	Graphics3D g3d;
	// the maximum BondingRadius seen in this set of atoms used in autobonding
	float maxBondingRadius = Float.MIN_VALUE;
	float maxVanderwaalsRadius = Float.MIN_VALUE;
	CellInfo[] cellInfos;
	int atomCount;
	public volatile Atom[] atoms;
	volatile int bondCount;
	Bond[] bonds;
	int groupCount;
	Group[] groups;
	// note: Molecules is set up to only be calculated WHEN NEEDED
	int moleculeCount;
	Molecule[] molecules = new Molecule[4];
	int modelCount;

	private BitSet elementsPresent;

	// //////////////////////////////////////////////////////////////
	// these may or may not be allocated
	// depending upon the AtomSetCollection characteristics
	Object[] clientAtomReferences;
	Vector3f[] vibrationVectors;
	byte[] occupancies;
	short[] bfactor100s;
	float[] partialCharges;
	float[] surfaceDistances;
	int[] surfaceAtoms;
	String[] atomNames;
	int[] atomSerials;
	byte[] specialAtomIDs;
	String[] group3Lists;
	int[][] group3Counts;

	BitSet bsHidden = new BitSet();

	// //////////////////////////////////////////////////////////////

	private final static int growthIncrement = 250;

	// deprecated due to multimodel issue:

	float[] notionalUnitcell; // required by an interface -- do NOT remove.

	// new way:
	boolean someModelsHaveSymmetry;
	boolean someModelsHaveUnitcells;
	boolean someModelsHaveFractionalCoordinates;

	boolean hasVibrationVectors;
	boolean fileHasHbonds;

	boolean structuresDefined;

	boolean hasBfactorRange;
	int bfactor100Lo;
	int bfactor100Hi;

	// XIE
	void clear() {
		htAtomMap.clear();
		if (removedAtomList != null)
			removedAtomList.clear();
		if (keptAtomList != null)
			keptAtomList.clear();
	}

	class Molecule {
		int moleculeIndex;
		int modelIndex;
		int indexInModel;
		int nAtoms;
		int nElements;
		int[] elementCounts = new int[JmolConstants.elementNumberMax];
		int[] altElementCounts = new int[JmolConstants.altElementMax];
		int elementNumberMax;
		int altElementMax;
		String mf;
		BitSet atomList;

		@SuppressWarnings("unchecked")
		Hashtable getInfo() {
			Hashtable info = new Hashtable();
			info.put("number", new Integer(moleculeIndex + 1)); // for now
			info.put("modelNumber", new Integer(modelIndex + 1)); // for now
			info.put("numberInModel", new Integer(indexInModel + 1));
			info.put("nAtoms", new Integer(nAtoms));
			info.put("nElements", new Integer(nElements));
			info.put("mf", mf);
			return info;
		}

		Molecule(int moleculeIndex, BitSet atomList, int modelIndex, int indexInModel) {
			this.atomList = atomList;
			this.moleculeIndex = moleculeIndex;
			this.modelIndex = modelIndex;
			this.indexInModel = indexInModel;
			getElementAndAtomCount(atomList);
			mf = getMolecularFormula();

			if (Logger.isActiveLevel(Logger.LEVEL_DEBUG))
				Logger.debug("new Molecule (" + mf + ") " + (indexInModel + 1) + "/" + (modelIndex + 1));
		}

		void getElementAndAtomCount(BitSet atomList) {
			for (int i = 0; i < atomCount; i++)
				if (atomList.get(i)) {
					nAtoms++;
					int n = atoms[i].getAtomicAndIsotopeNumber();
					if (n < 128) {
						elementCounts[n]++;
						if (elementCounts[n] == 1)
							nElements++;
						elementNumberMax = Math.max(elementNumberMax, n);
					}
					else {
						n = JmolConstants.altElementIndexFromNumber(n);
						altElementCounts[n]++;
						if (altElementCounts[n] == 1)
							nElements++;
						altElementMax = Math.max(altElementMax, n);
					}
				}
		}

		String getMolecularFormula() {
			String mf = "";
			String sep = "";
			int nX;
			for (int i = 1; i <= elementNumberMax; i++) {
				nX = elementCounts[i];
				if (nX != 0) {
					mf += sep + JmolConstants.elementSymbolFromNumber(i) + " " + nX;
					sep = " ";
				}
			}
			for (int i = 1; i <= altElementMax; i++) {
				nX = altElementCounts[i];
				if (nX != 0) {
					mf += sep + JmolConstants.elementSymbolFromNumber(JmolConstants.altElementNumberFromIndex(i)) + " "
							+ nX;
					sep = " ";
				}
			}
			return mf;
		}

	}

	Frame(Viewer viewer, String name) {
		this.viewer = viewer;
		initializeFrame(name, 1, null, null);
		initializeModel(null, null);
	}

	Frame(Viewer viewer, JmolAdapter adapter, Object clientFile) {
		this.viewer = viewer;
		initializeFrame(adapter.getFileTypeName(clientFile).toLowerCase().intern(), adapter
				.getEstimatedAtomCount(clientFile), adapter.getAtomSetCollectionProperties(clientFile), adapter
				.getAtomSetCollectionAuxiliaryInfo(clientFile));
		initializeModel(adapter, clientFile);
		adapter.finish(clientFile);
		if (false)
			dumpAtomSetNameDiagnostics(adapter, clientFile);
	}

	void initializeFrame(String name, int nAtoms, Properties properties, Hashtable info) {
		// long timeBegin = System.currentTimeMillis();
		modelSetTypeName = name;
		isXYZ = (modelSetTypeName == "xyz");
		isArrayOfFiles = (modelSetTypeName == "array");
		setZeroBased();
		mmset = new Mmset(this);
		frameRenderer = viewer.getFrameRenderer();
		g3d = viewer.getGraphics3D();

		initializeBuild(nAtoms);

		mmset.setModelSetProperties(properties);
		mmset.setModelSetAuxiliaryInfo(info);

		isMultiFile = mmset.getModelSetAuxiliaryInfoBoolean("isMultiFile");
		isPDB = mmset.getModelSetAuxiliaryInfoBoolean("isPDB");
		someModelsHaveSymmetry = mmset.getModelSetAuxiliaryInfoBoolean("someModelsHaveSymmetry");
		someModelsHaveUnitcells = mmset.getModelSetAuxiliaryInfoBoolean("someModelsHaveUnitcells");
		someModelsHaveFractionalCoordinates = mmset
				.getModelSetAuxiliaryInfoBoolean("someModelsHaveFractionalCoordinates");
		fileHasHbonds = false;
	}

	Chain nullChain;
	Group nullGroup;

	void initializeModel(JmolAdapter adapter, Object clientFile) {
		modelCount = (adapter == null ? 1 : adapter.getAtomSetCount(clientFile));
		mmset.setModelCount(modelCount);

		currentModelIndex = -1;
		if (adapter == null) {
			mmset.setModelNameNumberProperties(0, "", 1, null, null, false);
		}
		else {
			if (modelCount > 0) {
				nullChain = new Chain(this, mmset.getModel(0), ' ');
				nullGroup = new Group(nullChain, "", 0, -1, -1);
			}

			group3Lists = new String[modelCount + 1];
			group3Counts = new int[modelCount + 1][];
			for (int i = 0; i < modelCount; ++i) {
				int modelNumber = adapter.getAtomSetNumber(clientFile, i);
				String modelName = adapter.getAtomSetName(clientFile, i);
				if (modelName == null)
					modelName = "" + modelNumber;
				Properties modelProperties = adapter.getAtomSetProperties(clientFile, i);
				Hashtable modelAuxiliaryInfo = adapter.getAtomSetAuxiliaryInfo(clientFile, i);
				boolean isPDBModel = mmset.setModelNameNumberProperties(i, modelName, modelNumber, modelProperties,
						modelAuxiliaryInfo, isPDB);
				if (isPDBModel) {
					group3Lists[i] = JmolConstants.group3List;
					group3Counts[i] = new int[JmolConstants.group3Count + 10];
					if (group3Lists[modelCount] == null) {
						group3Lists[modelCount] = JmolConstants.group3List;
						group3Counts[modelCount] = new int[JmolConstants.group3Count + 10];
					}
				}
			}

			// atom is created, but not all methods are safe, because it
			// has no group -- this is only an issue for debugging

			short mad = viewer.getMadAtom();
			for (JmolAdapter.AtomIterator iterAtom = adapter.getAtomIterator(clientFile); iterAtom.hasNext();) {
				short elementNumber = (short) iterAtom.getElementNumber();
				if (elementNumber <= 0)
					elementNumber = JmolConstants.elementNumberFromSymbol(iterAtom.getElementSymbol());
				char alternateLocation = iterAtom.getAlternateLocationID();
				addAtom(iterAtom.getAtomSetIndex(), iterAtom.getAtomSymmetry(), iterAtom.getAtomSite(), iterAtom
						.getUniqueID(), elementNumber, iterAtom.getAtomName(), mad, iterAtom.getFormalCharge(),
						iterAtom.getPartialCharge(), iterAtom.getOccupancy(), iterAtom.getBfactor(), iterAtom.getX(),
						iterAtom.getY(), iterAtom.getZ(), iterAtom.getIsHetero(), iterAtom.getAtomSerial(), iterAtom
								.getChainID(), iterAtom.getGroup3(), iterAtom.getSequenceNumber(), iterAtom
								.getInsertionCode(), iterAtom.getVectorX(), iterAtom.getVectorY(), iterAtom
								.getVectorZ(), alternateLocation, iterAtom.getClientAtomReference());
			}

			JmolAdapter.BondIterator iterBond = adapter.getBondIterator(clientFile);
			if (iterBond != null)
				while (iterBond.hasNext()) {
					bondAtoms(iterBond.getAtomUniqueID1(), iterBond.getAtomUniqueID2(), (short) iterBond
							.getEncodedOrder());
				}
			JmolAdapter.StructureIterator iterStructure = adapter.getStructureIterator(clientFile);
			if (iterStructure != null)
				while (iterStructure.hasNext()) {
					if (!iterStructure.getStructureType().equals("turn"))
						defineStructure(iterStructure.getModelIndex(), iterStructure.getStructureType(), iterStructure
								.getStartChainID(), iterStructure.getStartSequenceNumber(), iterStructure
								.getStartInsertionCode(), iterStructure.getEndChainID(), iterStructure
								.getEndSequenceNumber(), iterStructure.getEndInsertionCode());
				}

			// define turns LAST. (pulled by the iterator first)
			// so that if they overlap they get overwritten:

			iterStructure = adapter.getStructureIterator(clientFile);
			if (iterStructure != null)
				while (iterStructure.hasNext()) {
					if (iterStructure.getStructureType().equals("turn"))
						defineStructure(iterStructure.getModelIndex(), iterStructure.getStructureType(), iterStructure
								.getStartChainID(), iterStructure.getStartSequenceNumber(), iterStructure
								.getStartInsertionCode(), iterStructure.getEndChainID(), iterStructure
								.getEndSequenceNumber(), iterStructure.getEndInsertionCode());
				}
		}

		/***************************************************************************************************************
		 * crystal cell must come first, in case atom coordinates need to be transformed to fit in the crystal cell
		 **************************************************************************************************************/

		loadShape(JmolConstants.SHAPE_BALLS);
		loadShape(JmolConstants.SHAPE_STICKS);
		loadShape(JmolConstants.SHAPE_HSTICKS);
		loadShape(JmolConstants.SHAPE_MEASURES);
		loadShape(JmolConstants.SHAPE_UCCAGE);

		if (adapter != null) {
			doUnitcellStuff();
			doAutobond();
		}
		finalizeGroupBuild(); // set group offsets and build monomers
		// only now can we access all of the atom's properties
		saveGroup3Info();
		buildPolymers();
		freeze();
		finalizeBuild();
		calcAverageAtomPoint();
		calcBoundBoxDimensions();
	}

	void dumpAtomSetNameDiagnostics(JmolAdapter adapter, Object clientFile) {
		int frameModelCount = getModelCount();
		int adapterAtomSetCount = adapter.getAtomSetCount(clientFile);
		Logger.debug("----------------\n" + "debugging of AtomSetName stuff\n" + "\nframeModelCount=" + frameModelCount
				+ "\nadapterAtomSetCount=" + adapterAtomSetCount + "\n -- \n");
		for (int i = 0; i < adapterAtomSetCount; ++i) {
			Logger.debug("atomSetName[" + i + "]=" + adapter.getAtomSetName(clientFile, i) + " atomSetNumber[" + i
					+ "]=" + adapter.getAtomSetNumber(clientFile, i));
		}
	}

	private final static int ATOM_GROWTH_INCREMENT = 2000;

	int currentModelIndex;
	Model currentModel;
	char currentChainID;
	Chain currentChain;
	int currentGroupSequenceNumber;
	char currentGroupInsertionCode;
	String currentGroup3;

	private final Hashtable htAtomMap = new Hashtable();

	void initializeBuild(int atomCountEstimate) {
		if (atomCountEstimate <= 0)
			atomCountEstimate = ATOM_GROWTH_INCREMENT;
		atoms = new Atom[atomCountEstimate];
		bonds = new Bond[250 + atomCountEstimate]; // was "2 *" -- WAY overkill.
		htAtomMap.clear();
		initializeGroupBuild();
	}

	void finalizeBuild() {
		currentModel = null;
		currentChain = null;
		// XIE: do not clear this, or we will have problems of making bonds.
		// htAtomMap.clear();
	}

	// XIE
	BitSet findAtomsInOval(Rectangle r) {
		bsFoundRectangle.and(bsEmpty);
		float dx, dy;
		Atom a;
		for (int i = atomCount; --i >= 0;) {
			a = atoms[i];
			dx = a.screenX - r.x - r.width * 0.5f;
			dy = a.screenY - r.y - r.height * 0.5f;
			dx *= 2.0f / r.width;
			dy *= 2.0f / r.height;
			if (dx * dx + dy * dy < 1) {
				bsFoundRectangle.set(i);
			}
		}
		return bsFoundRectangle;
	}

	// XIE
	@SuppressWarnings("unchecked")
	void addAtom(Object atomUid, short element, String atomName, int formalCharge, float partialCharge, float x,
			float y, float z, float vx, float vy, float vz, Object clientAtomReference) {
		if (atomCount == atoms.length)
			growAtomArrays();
		Atom at = new Atom(this, currentModelIndex, atomCount, null, 0, element, atomName, viewer.getMadAtom(),
				formalCharge, partialCharge, 0, 0, x, y, z, false, -1, '0', "", vx, vy, vz, '0', clientAtomReference);
		atoms[atomCount] = at;
		at.modelIndex = (short) viewer.getDisplayModelIndex();
		at.setMadAtom(viewer.getMadAtom());
		at.transform(viewer);
		htAtomMap.put(atomUid, at);
		atomCount++;
	}

	private List removedAtomList; // XIE
	private List keptAtomList; // XIE

	// XIE
	@SuppressWarnings("unchecked")
	void removeAtoms(BitSet bs) {
		if (bs == null)
			return;
		int m = atomCount - bs.cardinality();
		if (removedAtomList == null)
			removedAtomList = new ArrayList(bs.cardinality());
		else removedAtomList.clear();
		if (keptAtomList == null)
			keptAtomList = new ArrayList(m);
		else keptAtomList.clear();
		for (int i = 0; i < atomCount; i++) {
			if (bs.get(i)) {
				removedAtomList.add(atoms[i]);
			}
			else {
				keptAtomList.add(atoms[i]);
			}
			atoms[i] = null;
		}
		int n = keptAtomList.size();
		for (int i = 0; i < n; i++) {
			atoms[i] = (Atom) keptAtomList.get(i);
			atoms[i].atomIndex = i;
		}
		m = removedAtomList.size();
		for (int i = 0; i < m; i++) {
			atoms[n + i] = (Atom) removedAtomList.get(i);
		}
		atomCount = n;
	}

	// XIE
	void removeAtom(int index) {
		if (index < 0 || index >= atomCount)
			return;
		atomCount--;
		for (int i = index; i < atomCount; i++) {
			atoms[i] = atoms[i + 1];
			atoms[i].atomIndex = i;
		}
	}

	@SuppressWarnings("unchecked")
	void addAtom(int modelIndex, BitSet atomSymmetry, int atomSite, Object atomUid, short atomicAndIsotopeNumber,
			String atomName, short mad, int formalCharge, float partialCharge, int occupancy, float bfactor, float x,
			float y, float z, boolean isHetero, int atomSerial, char chainID, String group3, int groupSequenceNumber,
			char groupInsertionCode, float vectorX, float vectorY, float vectorZ, char alternateLocationID,
			Object clientAtomReference) {

		checkNewGroup(atomCount, modelIndex, chainID, group3, groupSequenceNumber, groupInsertionCode);

		if (atomCount == atoms.length)
			growAtomArrays();

		Atom atom = new Atom(this, currentModelIndex, atomCount, atomSymmetry, atomSite, atomicAndIsotopeNumber,
				atomName, mad, formalCharge, partialCharge, occupancy, bfactor, x, y, z, isHetero, atomSerial, chainID,
				group3, vectorX, vectorY, vectorZ, alternateLocationID, clientAtomReference);
		atoms[atomCount] = atom;
		++atomCount;
		htAtomMap.put(atomUid, atom);
	}

	void checkNewGroup(int atomIndex) {
		Atom atom = atoms[atomIndex];
		if (atom.group == null)
			checkNewGroup(atomIndex, atom.modelIndex, '\0', null, 0, '\0');
		else checkNewGroup(atomIndex, atom.modelIndex, atom.getChainID(), atom.getGroup3(), atom.getSeqNumber(), atom
				.getInsertionCode());
	}

	void checkNewGroup(int atomIndex, int modelIndex, char chainID, String group3, int groupSequenceNumber,
			char groupInsertionCode) {
		String group3i = (group3 == null ? null : group3.intern());
		if (modelIndex != currentModelIndex) {
			currentModel = mmset.getModel(modelIndex);
			currentModelIndex = modelIndex;
			currentChainID = '\uFFFF';
		}
		if (chainID != currentChainID) {
			currentChainID = chainID;
			currentChain = currentModel.getOrAllocateChain(chainID);
			currentGroupInsertionCode = '\uFFFF';
			currentGroupSequenceNumber = -1;
			currentGroup3 = "xxxx";
		}
		if (groupSequenceNumber != currentGroupSequenceNumber || groupInsertionCode != currentGroupInsertionCode
				|| group3i != currentGroup3) {
			currentGroupSequenceNumber = groupSequenceNumber;
			currentGroupInsertionCode = groupInsertionCode;
			currentGroup3 = group3i;
			startGroup(currentChain, group3, groupSequenceNumber, groupInsertionCode, atomIndex);
		}
	}

	short defaultCovalentMad;

	void bondAtoms(Object atomUid1, Object atomUid2, short order) {
		if (defaultCovalentMad == 0)
			defaultCovalentMad = viewer.getMadBond();
		Atom atom1 = (Atom) htAtomMap.get(atomUid1);
		if (atom1 == null) {
			Logger.error("bondAtoms cannot find atomUid1?:" + atomUid1);
			return;
		}
		Atom atom2 = (Atom) htAtomMap.get(atomUid2);
		if (atom2 == null) {
			Logger.error("bondAtoms cannot find atomUid2?:" + atomUid2);
			return;
		}
		// note that if the atoms are already bonded then
		// Atom.bondMutually(...) will return null
		Bond bond = bondMutually(atom1, atom2, order, getDefaultMadFromOrder(order));
		if (bond == null)
			return;
		if (bondCount == bonds.length)
			bonds = (Bond[]) ArrayUtil.setLength(bonds, bondCount + 2 * ATOM_GROWTH_INCREMENT);
		bonds[bondCount++] = bond;
		if ((order & JmolConstants.BOND_HYDROGEN_MASK) != 0)
			fileHasHbonds = true;
	}

	// XIE
	void growAtomArrays() {
		growAtomArrays(ATOM_GROWTH_INCREMENT);
	}

	void growAtomArrays(int byHowMuch) {
		int newLength = atomCount + byHowMuch;
		atoms = (Atom[]) ArrayUtil.setLength(atoms, newLength);
		if (clientAtomReferences != null)
			clientAtomReferences = (Object[]) ArrayUtil.setLength(clientAtomReferences, newLength);
		if (vibrationVectors != null)
			vibrationVectors = (Vector3f[]) ArrayUtil.setLength(vibrationVectors, newLength);
		if (occupancies != null)
			occupancies = ArrayUtil.setLength(occupancies, newLength);
		if (bfactor100s != null)
			bfactor100s = ArrayUtil.setLength(bfactor100s, newLength);
		if (partialCharges != null)
			partialCharges = ArrayUtil.setLength(partialCharges, newLength);
		if (atomNames != null)
			atomNames = ArrayUtil.setLength(atomNames, newLength);
		if (atomSerials != null)
			atomSerials = ArrayUtil.setLength(atomSerials, newLength);
		if (specialAtomIDs != null)
			specialAtomIDs = ArrayUtil.setLength(specialAtomIDs, newLength);
	}

	// //////////////////////////////////////////////////////////////
	// special handling for groups
	// //////////////////////////////////////////////////////////////

	final static int defaultGroupCount = 32;
	Chain[] chains;
	String[] group3s;
	int[] seqcodes;
	int[] firstAtomIndexes;

	final int[] specialAtomIndexes = new int[JmolConstants.ATOMID_MAX];

	void initializeGroupBuild() {
		groupCount = 0;
		chains = new Chain[defaultGroupCount];
		group3s = new String[defaultGroupCount];
		seqcodes = new int[defaultGroupCount];
		firstAtomIndexes = new int[defaultGroupCount];
		currentModelIndex = -1;
		currentModel = null;
		currentChainID = '\uFFFF';
		currentChain = null;
		currentGroupInsertionCode = '\uFFFF';
		currentGroup3 = "xxxxx";
	}

	void startGroup(Chain chain, String group3, int groupSequenceNumber, char groupInsertionCode, int firstAtomIndex) {
		if (groupCount == group3s.length) {
			chains = (Chain[]) ArrayUtil.doubleLength(chains);
			group3s = ArrayUtil.doubleLength(group3s);
			seqcodes = ArrayUtil.doubleLength(seqcodes);
			firstAtomIndexes = ArrayUtil.doubleLength(firstAtomIndexes);
		}
		firstAtomIndexes[groupCount] = firstAtomIndex;
		chains[groupCount] = chain;
		group3s[groupCount] = group3;
		seqcodes[groupCount] = Group.getSeqcode(groupSequenceNumber, groupInsertionCode);
		++groupCount;
	}

	void finalizeGroupBuild() {
		// run this loop in increasing order so that the
		// groups get defined going up
		groups = new Group[groupCount];
		for (int i = 0; i < groupCount; ++i) {
			distinguishAndPropagateGroup(i, chains[i], group3s[i], seqcodes[i], firstAtomIndexes[i],
					(i == groupCount - 1 ? atomCount : firstAtomIndexes[i + 1]));
			chains[i] = null;
			group3s[i] = null;
		}
		chains = null;
		group3s = null;
	}

	@SuppressWarnings("unchecked")
	void saveGroup3Info() {
		if (group3Lists == null)
			return;
		Hashtable info = getModelSetAuxiliaryInfo();
		if (info == null)
			return;
		info.put("group3Lists", group3Lists);
		info.put("group3Counts", group3Counts);
	}

	void distinguishAndPropagateGroup(int groupIndex, Chain chain, String group3, int seqcode, int firstAtomIndex,
			int maxAtomIndex) {
		/*
		 * called by finalizeGroupBuild()
		 * 
		 * first: build array of special atom names, for example "CA" for the alpha carbon is assigned #2 see
		 * JmolConstants.specialAtomNames[] the special atoms all have IDs based on Atom.lookupSpecialAtomID(atomName)
		 * these will be the same for each conformation
		 * 
		 * second: creates the monomers themselves based on this information thus building the byte offsets[] array for
		 * each monomer, indicating which position relative to the first atom in the group is which atom. Each
		 * monomer.offsets[i] then points to the specific atom of that type these will NOT be the same for each
		 * conformation
		 * 
		 */
		Group group = null;
		int lastAtomIndex = maxAtomIndex - 1;

		if (group3 != null) {
			int distinguishingBits = 0;

			// clear previous specialAtomIndexes
			for (int i = JmolConstants.ATOMID_MAX; --i >= 0;)
				specialAtomIndexes[i] = Integer.MIN_VALUE;

			if (specialAtomIDs != null) {
				// go last to first so that FIRST confirmation is default
				for (int i = maxAtomIndex; --i >= firstAtomIndex;) {
					int specialAtomID = specialAtomIDs[i];
					if (specialAtomID > 0) {
						if (specialAtomID < JmolConstants.ATOMID_DISTINGUISHING_ATOM_MAX) {
							int bit = 1 << specialAtomID;
							/*
							 * save for future option -- turns out the 1jsa bug was in relation to an author using the
							 * same group number for two different groups
							 * 
							 * System.out.println( i + " " + atoms[i].getIdentity() + " " + specialAtomID); if
							 * ((distinguishingBits & bit) != 0) {
							 * 
							 * //bh 9/21/2006: // "if the group has two of the same, that cannot be right." // Thus, for
							 * example, two C's doth not make a protein "carbonyl C" distinguishingBits = 0; break; }
							 */
							distinguishingBits |= bit;
						}
						specialAtomIndexes[specialAtomID] = i;
					}
				}
			}

			if (lastAtomIndex < firstAtomIndex)
				throw new NullPointerException();

			if ((distinguishingBits & JmolConstants.ATOMID_PROTEIN_MASK) == JmolConstants.ATOMID_PROTEIN_MASK) {
				group = AminoMonomer.validateAndAllocate(chain, group3, seqcode, firstAtomIndex, lastAtomIndex,
						specialAtomIndexes, atoms);
				countGroup(atoms[firstAtomIndex].modelIndex, "p>", group3);
			}
			else if (distinguishingBits == JmolConstants.ATOMID_ALPHA_ONLY_MASK) {
				group = AlphaMonomer.validateAndAllocate(chain, group3, seqcode, firstAtomIndex, lastAtomIndex,
						specialAtomIndexes, atoms);
				countGroup(atoms[firstAtomIndex].modelIndex, "p>", group3);
			}
			else if (((distinguishingBits & JmolConstants.ATOMID_NUCLEIC_MASK) == JmolConstants.ATOMID_NUCLEIC_MASK)) {
				group = NucleicMonomer.validateAndAllocate(chain, group3, seqcode, firstAtomIndex, lastAtomIndex,
						specialAtomIndexes, atoms);
				countGroup(atoms[firstAtomIndex].modelIndex, "n>", group3);
			}
			else if (distinguishingBits == JmolConstants.ATOMID_PHOSPHORUS_ONLY_MASK) {
				group = PhosphorusMonomer.validateAndAllocate(chain, group3, seqcode, firstAtomIndex, lastAtomIndex,
						specialAtomIndexes, atoms);
				countGroup(atoms[firstAtomIndex].modelIndex, "n>", group3);
			}
			else if (JmolConstants.checkCarbohydrate(group3)) {
				group = CarbohydrateMonomer.validateAndAllocate(chain, group3, seqcode, firstAtomIndex, lastAtomIndex,
						specialAtomIndexes, atoms);
				countGroup(atoms[firstAtomIndex].modelIndex, "c>", group3);
			}

		}
		if (group == null) {
			group = new Group(chain, group3, seqcode, firstAtomIndex, lastAtomIndex);
			if (group3 != null)
				countGroup(atoms[firstAtomIndex].modelIndex, "o>", group3);
		}

		chain.addGroup(group);
		groups[groupIndex] = group;

		for (int i = maxAtomIndex; --i >= firstAtomIndex;)
			atoms[i].setGroup(group);

	}

	void countGroup(int modelIndex, String code, String group3) {
		if (group3Lists == null || group3Lists[modelIndex] == null)
			return;
		String g3code = (group3 + "   ").substring(0, 3);
		int pt = group3Lists[modelIndex].indexOf(g3code);
		if (pt < 0) {
			group3Lists[modelIndex] += ",[" + g3code + "]";
			pt = group3Lists[modelIndex].indexOf(g3code);
			group3Counts[modelIndex] = ArrayUtil.setLength(group3Counts[modelIndex],
					group3Counts[modelIndex].length + 10);
		}
		group3Counts[modelIndex][pt / 6]++;
		pt = group3Lists[modelIndex].indexOf(",[" + g3code);
		if (pt >= 0)
			group3Lists[modelIndex] = group3Lists[modelIndex].substring(0, pt) + code
					+ group3Lists[modelIndex].substring(pt + 2);
		// becomes x> instead of ,[
		// these will be used for setting up the popup menu
		if (modelIndex < modelCount)
			countGroup(modelCount, code, group3);
	}

	// //////////////////////////////////////////////////////////////

	void buildPolymers() {
		for (int i = 0; i < groupCount; ++i) {
			Group group = groups[i];
			if (group instanceof Monomer) {
				Monomer monomer = (Monomer) group;
				if (monomer.polymer == null)
					Polymer.allocatePolymer(groups, i);
			}
		}
	}

	void clearPolymers() {
		for (int i = 0; i < groupCount; ++i) {
			Group group = groups[i];
			if (group instanceof Monomer) {
				Monomer monomer = (Monomer) group;
				if (monomer.polymer != null)
					monomer.polymer = null;
			}
		}
	}

	// //////////////////////////////////////////////////////////////
	FrameExportJmolAdapter exportJmolAdapter;

	JmolAdapter getExportJmolAdapter() {
		if (exportJmolAdapter == null)
			exportJmolAdapter = new FrameExportJmolAdapter(viewer, this);
		return exportJmolAdapter;
	}

	void freeze() {

		// //////////////////////////////////////////////////////////////
		// resize arrays
		if (atomCount < atoms.length)
			growAtomArrays(0);
		if (bondCount < bonds.length)
			bonds = (Bond[]) ArrayUtil.setLength(bonds, bondCount);

		freeBondsCache();

		// //////////////////////////////////////////////////////////////
		// see if there are any vectors
		hasVibrationVectors = vibrationVectors != null;

		// //////////////////////////////////////////////////////////////
		//
		setAtomNamesAndNumbers();

		// //////////////////////////////////////////////////////////////
		// find things for the popup menus
		findElementsPresent();

		// //////////////////////////////////////////////////////////////
		// finalize all group business
		calculateStructures(false);
	}

	void calculateStructures(boolean rebuild) {
		if (rebuild) {
			for (int i = JmolConstants.SHAPE_MAX_SECONDARY; --i >= JmolConstants.SHAPE_MIN_SECONDARY;)
				shapes[i] = null;
			clearPolymers();
			mmset.clearStructures();
			initializeGroupBuild();
			for (int i = 0; i < atomCount; i++)
				checkNewGroup(i);
			finalizeGroupBuild();
			buildPolymers();
			structuresDefined = false;
			moleculeCount = 0;
		}
		if (!structuresDefined)
			mmset.calculateStructures();
		mmset.freeze();
	}

	BitSet setConformation(int modelIndex, int conformationIndex) {
		BitSet bs = new BitSet();
		String altLocs = getAltLocListInModel(modelIndex);
		if (altLocs.length() > 0) {
			BitSet bsConformation = getModelAtomBitSet(modelIndex);
			if (conformationIndex >= 0)
				for (int c = getAltLocCountInModel(modelIndex); --c >= 0;)
					if (c != conformationIndex)
						bsConformation.andNot(getSpecAlternate(altLocs.substring(c, c + 1)));
			if (bsConformation.length() > 0) {
				setConformation(modelIndex, bsConformation);
				bs.or(bsConformation);
			}
		}
		return bs;
	}

	void setConformation(int modelIndex, BitSet bsConformation) {
		mmset.setConformation(modelIndex, bsConformation);
	}

	void setZeroBased() {
		isZeroBased = isXYZ && viewer.getZeroBasedXyzRasmol();
	}

	private void setAtomNamesAndNumbers() {
		// first, validate that all atomSerials are NaN
		if (atomSerials == null) {
			// now, we'll assign 1-based atom numbers within each model
			int lastModelIndex = Integer.MAX_VALUE;
			int modelAtomIndex = 0;
			atomSerials = new int[atomCount];
			for (int i = 0; i < atomCount; ++i) {
				Atom atom = atoms[i];
				if (atom.modelIndex != lastModelIndex) {
					lastModelIndex = atom.modelIndex;
					modelAtomIndex = (isZeroBased ? 0 : 1);
				}
				atomSerials[i] = modelAtomIndex++;
			}
		}
		if (atomNames == null)
			atomNames = new String[atomCount];
		for (int i = 0; i < atomCount; ++i)
			if (atomNames[i] == null) {
				Atom atom = atoms[i];
				atomNames[i] = atom.getElementSymbol() + atom.getAtomNumber();
				// System.out.println("atom " + i + " " + atomNames[i]);
			}
	}

	void defineStructure(int modelIndex, String structureType, char startChainID, int startSequenceNumber,
			char startInsertionCode, char endChainID, int endSequenceNumber, char endInsertionCode) {
		structuresDefined = true; // (in file)
		mmset.defineStructure(modelIndex, structureType, startChainID, startSequenceNumber, startInsertionCode,
				endChainID, endSequenceNumber, endInsertionCode);
	}

	int getAtomIndexFromAtomNumber(int atomNumber) {
		// definitely want FIRST (model) not last here
		for (int i = 0; i < atomCount; i++) {
			if (atoms[i].getAtomNumber() == atomNumber)
				return i;
		}
		return -1;
	}

	int getAltLocIndexInModel(int modelIndex, char alternateLocationID) {
		if (alternateLocationID == '\0')
			return 0;
		String altLocList = getAltLocListInModel(modelIndex);
		if (altLocList.length() == 0)
			return 0;
		return altLocList.indexOf(alternateLocationID) + 1;
	}

	int getInsertionCodeIndexInModel(int modelIndex, char insertionCode) {
		if (insertionCode == '\0')
			return 0;
		String codeList = getInsertionListInModel(modelIndex);
		if (codeList.length() == 0)
			return 0;
		return codeList.indexOf(insertionCode) + 1;
	}

	String getAltLocListInModel(int modelIndex) {
		String str = (String) getModelAuxiliaryInfo(modelIndex, "altLocs");
		return (str == null ? "" : str);
	}

	String getInsertionListInModel(int modelIndex) {
		String str = (String) getModelAuxiliaryInfo(modelIndex, "insertionCodes");
		return (str == null ? "" : str);
	}

	String getModelSymmetryList(int modelIndex) {
		String[] list = (String[]) getModelAuxiliaryInfo(modelIndex, "symmetryOperations");
		String str = "";
		if (list != null)
			for (int i = 0; i < list.length; i++)
				str += "\n" + list[i];
		return str;
	}

	int getAltLocCountInModel(int modelIndex) {
		return mmset.getNAltLocs(modelIndex);
	}

	int getInsertionCountInModel(int modelIndex) {
		return mmset.getNInsertions(modelIndex);
	}

	Properties getModelSetProperties() {
		return mmset.getModelSetProperties();
	}

	String getModelSetProperty(String propertyName) {
		return mmset.getModelSetProperty(propertyName);
	}

	Hashtable getModelSetAuxiliaryInfo() {
		return mmset.getModelSetAuxiliaryInfo();
	}

	Object getModelSetAuxiliaryInfo(String keyName) {
		return mmset.getModelSetAuxiliaryInfo(keyName);
	}

	boolean modelSetHasVibrationVectors() {
		return hasVibrationVectors;
	}

	boolean modelHasVibrationVectors(int modelIndex) {
		if (vibrationVectors != null)
			for (int i = atomCount; --i >= 0;)
				if ((modelIndex < 0 || atoms[i].modelIndex == modelIndex) && vibrationVectors[i] != null)
					return true;
		return false;
	}

	int getModelCount() {
		return mmset.getModelCount();
	}

	int getModelNumber(int modelIndex) {
		return mmset.getModelNumber(modelIndex);
	}

	String getModelName(int modelIndex) {
		return mmset.getModelName(modelIndex);
	}

	String getModelSetTypeName() {
		return modelSetTypeName;
	}

	Properties getModelProperties(int modelIndex) {
		return mmset.getModelProperties(modelIndex);
	}

	String getModelProperty(int modelIndex, String propertyName) {
		return mmset.getModelProperty(modelIndex, propertyName);
	}

	Hashtable getModelAuxiliaryInfo(int modelIndex) {
		return mmset.getModelAuxiliaryInfo(modelIndex);
	}

	Object getModelAuxiliaryInfo(int modelIndex, String keyName) {
		return mmset.getModelAuxiliaryInfo(modelIndex, keyName);
	}

	Model getModel(int modelIndex) {
		return mmset.getModel(modelIndex);
	}

	int getModelNumberIndex(int modelNumber) {
		return mmset.getModelNumberIndex(modelNumber);
	}

	// //////////////////////////////////////////////////////////////

	int getChainCount() {
		return mmset.getChainCount();
	}

	int getPolymerCount() {
		return mmset.getPolymerCount();
	}

	int getChainCountInModel(int modelIndex) {
		return mmset.getChainCountInModel(modelIndex);
	}

	int getPolymerCountInModel(int modelIndex) {
		return mmset.getPolymerCountInModel(modelIndex);
	}

	Polymer getPolymerAt(int modelIndex, int polymerIndex) {
		return mmset.getPolymerAt(modelIndex, polymerIndex);
	}

	int getGroupCount() {
		return mmset.getGroupCount();
	}

	int getGroupCountInModel(int modelIndex) {
		return mmset.getGroupCountInModel(modelIndex);
	}

	int getAtomCount() {
		return atomCount;
	}

	int getAtomCountInModel(int modelIndex) {
		int n = 0;
		for (int i = atomCount; --i >= 0;)
			if (atoms[i].modelIndex == modelIndex)
				n++;
		return n;
	}

	Atom[] getAtoms() {
		return atoms;
	}

	Atom getAtomAt(int atomIndex) {
		if (atomIndex < 0 || atomIndex >= atoms.length)
			return null;
		return atoms[atomIndex];
	}

	Point3f getAtomPoint3f(int atomIndex) {
		return atoms[atomIndex];
	}

	/**
	 * For use for setting a for() construct bound ONLY
	 * 
	 * @return size of the bonds array;
	 */
	int getBondCount() {
		// not necessarily the REAL bond count; this is an ARRAY MAXIMUM
		return bondCount;
	}

	/**
	 * for general use
	 * 
	 * @param modelIndex
	 *            the model of interest or -1 for all
	 * @return the actual number of connections
	 */
	int getBondCountInModel(int modelIndex) {
		int n = 0;
		for (int i = bondCount; --i >= 0;)
			if (modelIndex < 0 || bonds[i].atom1.modelIndex == modelIndex)
				n++;
		return n;
	}

	Bond getBondAt(int bondIndex) {
		return bonds[bondIndex];
	}

	/**
	 * When creating a new bond, determine bond diameter from order
	 * 
	 * @param order
	 * @return if hydrogen bond, default to 1; otherwise 0 (general default)
	 */
	short getDefaultMadFromOrder(short order) {
		return ((order & JmolConstants.BOND_HYDROGEN_MASK) > 0 ? 1 : defaultCovalentMad);
	}

	private Bond bondMutually(Atom atom, Atom atomOther, short order, short mad) {
		if (atom.isBonded(atomOther))
			return null;
		Bond bond = new Bond(atom, atomOther, order, mad, (short) 0);
		addBondToAtom(atom, bond);
		addBondToAtom(atomOther, bond);
		return bond;
	}

	private Bond addBond(Bond bond) {
		if (bond == null)
			return null;
		if (bondCount == bonds.length) {
			bonds = (Bond[]) ArrayUtil.setLength(bonds, bondCount + growthIncrement);
		}
		return bonds[bondCount++] = bond;
	}

	Bond bondAtoms(Atom atom1, Atom atom2, short order, short mad) {
		return addBond(bondMutually(atom1, atom2, order, mad));
	}

	void addHydrogenBond(Atom atom1, Atom atom2, short order, BitSet bsA, BitSet bsB) {
		boolean atom1InSetA = bsA == null || bsA.get(atom1.atomIndex);
		boolean atom1InSetB = bsB == null || bsB.get(atom1.atomIndex);
		boolean atom2InSetA = bsA == null || bsA.get(atom2.atomIndex);
		boolean atom2InSetB = bsB == null || bsB.get(atom2.atomIndex);
		if (atom1InSetA & atom2InSetB || atom1InSetB & atom2InSetA)
			addBond(bondMutually(atom1, atom2, order, (short) 1));
	}

	Shape allocateShape(int shapeID) {
		String classBase = JmolConstants.shapeClassBases[shapeID];
		String className = "org.myjmol.viewer." + classBase;

		try {
			Class shapeClass = Class.forName(className);
			Shape shape = (Shape) shapeClass.newInstance();
			shape.setViewerG3dFrame(viewer, g3d, this, shapeID);
			return shape;
		}
		catch (Exception e) {
			Logger.error("Could not instantiate shape:" + classBase, e);
		}
		return null;
	}

	final Shape[] shapes = new Shape[JmolConstants.SHAPE_MAX];

	void loadShape(int shapeID) {
		if (shapes[shapeID] == null) {
			shapes[shapeID] = allocateShape(shapeID);
		}
	}

	void setShapeSize(int shapeID, int size, BitSet bsSelected) {
		if (size != 0)
			loadShape(shapeID);
		if (shapes[shapeID] != null)
			shapes[shapeID].setSize(size, bsSelected);
	}

	void setShapeProperty(int shapeID, String propertyName, Object value, BitSet bsSelected) {
		if (shapes[shapeID] != null)
			shapes[shapeID].setProperty(propertyName, value, bsSelected);
	}

	Object getShapeProperty(int shapeID, String propertyName, int index) {
		return (shapes[shapeID] == null ? null : shapes[shapeID].getProperty(propertyName, index));
	}

	final Point3f averageAtomPoint = new Point3f();

	final Point3f centerBoundBox = new Point3f();
	final Vector3f boundBoxCornerVector = new Vector3f();
	final Point3f minBoundBox = new Point3f();
	final Point3f maxBoundBox = new Point3f();

	Point3f getBoundBoxCenter() {
		return centerBoundBox;
	}

	Point3f getAverageAtomPoint() {
		return averageAtomPoint;
	}

	Vector3f getBoundBoxCornerVector() {
		return boundBoxCornerVector;
	}

	private void calcAverageAtomPoint() {
		Point3f average = averageAtomPoint;
		average.set(0, 0, 0);
		if (atomCount == 0)
			return;
		for (int i = atomCount; --i >= 0;)
			average.add(atoms[i]);
		average.scale(1f / atomCount);
	}

	final static Point3f[] unitBboxPoints = { new Point3f(1, 1, 1), new Point3f(1, 1, -1), new Point3f(1, -1, 1),
			new Point3f(1, -1, -1), new Point3f(-1, 1, 1), new Point3f(-1, 1, -1), new Point3f(-1, -1, 1),
			new Point3f(-1, -1, -1), };

	final Point3f[] bboxVertices = new Point3f[8];

	// XIE
	private void calcCuboidsMinMax() {
		if (!(frameRenderer instanceof ExtendedFrameRenderer))
			return;
		Cuboids c = ((ExtendedFrameRenderer) frameRenderer).cuboids;
		if (c.isEmpty())
			return;
		Point3f min = c.getMin();
		Point3f max = c.getMax();
		if (min.x < minBoundBox.x)
			minBoundBox.x = min.x;
		if (min.y < minBoundBox.y)
			minBoundBox.y = min.y;
		if (min.z < minBoundBox.z)
			minBoundBox.z = min.z;
		if (max.x > maxBoundBox.x)
			maxBoundBox.x = max.x;
		if (max.y > maxBoundBox.y)
			maxBoundBox.y = max.y;
		if (max.z > maxBoundBox.z)
			maxBoundBox.z = max.z;
	}

	private void calcBoundBoxDimensions() {
		calcAtomsMinMax(minBoundBox, maxBoundBox);
		calcUnitCellMinMax(minBoundBox, maxBoundBox);
		calcCuboidsMinMax(); // XIE
		centerBoundBox.add(minBoundBox, maxBoundBox);
		centerBoundBox.scale(0.5f);
		boundBoxCornerVector.sub(maxBoundBox, centerBoundBox);

		for (int i = 8; --i >= 0;) {
			Point3f bbcagePoint = bboxVertices[i] = new Point3f(unitBboxPoints[i]);
			bbcagePoint.x *= boundBoxCornerVector.x;
			bbcagePoint.y *= boundBoxCornerVector.y;
			bbcagePoint.z *= boundBoxCornerVector.z;
			bbcagePoint.add(centerBoundBox);
		}
	}

	// XIE
	float calcRotationRadius(Point3f center) {
		float maxRadius = 0;
		// XIE: we must calculate the rotation radius for the simulation box, which is a cuboid
		if (frameRenderer instanceof ExtendedFrameRenderer) {
			// maxRadius = ((ExtendedFrameRenderer) frameRenderer).cuboids.getRotationRadius(center);
			// if we do not calculate the above, there will be a jump when the first time we add an atom.
			// but if we do, there will be a lot of problems because the rendering seems to depend a lot
			// on the value of the rotation radius, which is only determined from the atoms.
			maxRadius = calcRotationRadiusForAtoms(center);
		}
		else {
			maxRadius = calcRotationRadiusForAtoms(center);
		}
		return maxRadius == 0 ? 60 : maxRadius; // XIE: 6 nanometers for empty box
	}

	// XIE
	private float calcRotationRadiusForAtoms(Point3f center) {
		float maxRadius = 0;
		for (int i = atomCount; --i >= 0;) {
			Atom atom = atoms[i];
			float distAtom = center.distance(atom);
			float radiusVdw = atom.getVanderwaalsRadiusFloat();
			float outerVdw = distAtom + radiusVdw;
			if (outerVdw > maxRadius)
				maxRadius = outerVdw;
		}
		return maxRadius;
	}

	final static int measurementGrowthIncrement = 16;
	int measurementCount = 0;
	Measurement[] measurements = null;

	/*******************************************************************************************************************
	 * ============================================================== selection handling
	 * ==============================================================
	 */

	boolean frankClicked(int x, int y) {
		Shape frankShape = shapes[JmolConstants.SHAPE_FRANK];
		if (frankShape == null)
			return false;
		return frankShape.wasClicked(x, y);
	}

	final Closest closest = new Closest();

	int findNearestAtomIndex(int x, int y) {
		if (atomCount == 0)
			return -1;
		closest.atom = null;
		findNearestAtomIndex(x, y, closest);
		for (int i = 0; i < shapes.length; ++i) {
			if (closest.atom != null)
				break;
			Shape shape = shapes[i];
			if (shape != null)
				shape.findNearestAtomIndex(x, y, closest);
		}
		int closestIndex = (closest.atom == null ? -1 : closest.atom.atomIndex);
		closest.atom = null;
		return closestIndex;
	}

	final static int minimumPixelSelectionRadius = 6;

	/*
	 * generalized; not just balls
	 * 
	 * This algorithm assumes that atoms are circles at the z-depth of their center point. Therefore, it probably has
	 * some flaws around the edges when dealing with intersecting spheres that are at approximately the same z-depth.
	 * But it is much easier to deal with than trying to actually calculate which atom was clicked
	 * 
	 * A more general algorithm of recording which object drew which pixel would be very expensive and not worth the
	 * trouble
	 */
	void findNearestAtomIndex(int x, int y, Closest closest) {
		Atom champion = null;
		for (int i = atomCount; --i >= 0;) {
			Atom contender = atoms[i];
			if (contender.screenZ <= 1)
				continue; // XIE
			if (contender.isClickable() && contender.isCursorOnTopOf(x, y, minimumPixelSelectionRadius, champion)) {
				champion = contender;
			}
		}
		closest.atom = champion;
	}

	/**
	 * used by Frame and AminoMonomer and NucleicMonomer -- does NOT check for clickability
	 * 
	 * @param contender
	 * @param x
	 * @param y
	 * @param radius
	 * @param champion
	 * @return true if user is pointing to this atom
	 */
	boolean isCursorOnTopOf(Atom contender, int x, int y, int radius, Atom champion) {
		return contender.screenZ > 1 && !g3d.isClippedZ(contender.screenZ)
				&& g3d.isInDisplayRange(contender.screenX, contender.screenY)
				&& contender.isCursorOnTopOf(x, y, radius, champion);
	}

	// jvm < 1.4 does not have a BitSet.clear();
	// so in order to clear you "and" with an empty bitset.
	final BitSet bsEmpty = new BitSet();
	final BitSet bsFoundRectangle = new BitSet();

	BitSet findAtomsInRectangle(Rectangle rect) {
		bsFoundRectangle.and(bsEmpty);
		for (int i = atomCount; --i >= 0;) {
			Atom atom = atoms[i];
			if (rect.contains(atom.screenX, atom.screenY))
				bsFoundRectangle.set(i);
		}
		return bsFoundRectangle;
	}

	BondIterator getBondIterator(short bondType, BitSet bsSelected) {
		return new SelectedBondIterator(bondType, bsSelected);
	}

	BondIterator getBondIterator(BitSet bsSelected) {
		return new SelectedBondIterator(bsSelected);
	}

	class SelectedBondIterator implements BondIterator {

		short bondType;
		int iBond;
		BitSet bsSelected;
		boolean bondSelectionModeOr;
		boolean isBondBitSet;

		SelectedBondIterator(short bondType, BitSet bsSelected) {
			this.bondType = bondType;
			this.bsSelected = bsSelected;
			isBondBitSet = false;
			iBond = 0;
			bondSelectionModeOr = viewer.getBondSelectionModeOr();
		}

		SelectedBondIterator(BitSet bsSelected) {
			this.bsSelected = bsSelected;
			iBond = 0;
			isBondBitSet = true;
		}

		public boolean hasNext() {
			for (; iBond < bondCount; ++iBond) {
				Bond bond = bonds[iBond];
				if (isBondBitSet) {
					if (bsSelected.get(iBond))
						return true;
					continue;
				}
				else if (bondType != JmolConstants.BOND_ALL_MASK && (bond.order & bondType) == 0) {
					continue;
				}
				boolean isSelected1 = bsSelected.get(bond.atom1.atomIndex);
				boolean isSelected2 = bsSelected.get(bond.atom2.atomIndex);
				if ((!bondSelectionModeOr & isSelected1 & isSelected2)
						|| (bondSelectionModeOr & (isSelected1 | isSelected2)))
					return true;
			}
			return false;
		}

		public int nextIndex() {
			return iBond;
		}

		public Bond next() {
			return bonds[iBond++];
		}
	}

	Bspf bspf;

	private final static boolean MIX_BSPT_ORDER = false;

	void initializeBspf() {
		if (bspf == null) {
			long timeBegin = 0;
			if (showRebondTimes)
				timeBegin = System.currentTimeMillis();
			bspf = new Bspf(3);
			if (MIX_BSPT_ORDER) {
				Logger.debug("mixing bspt order");
				int stride = 3;
				int step = (atomCount + stride - 1) / stride;
				for (int i = 0; i < step; ++i)
					for (int j = 0; j < stride; ++j) {
						int k = i * stride + j;
						if (k >= atomCount)
							continue;
						Atom atom = atoms[k];
						if (!atom.isDeleted())
							bspf.addTuple(atom.modelIndex, atom);
					}
			}
			else {
				Logger.debug("sequential bspt order");
				for (int i = atomCount; --i >= 0;) {
					Atom atom = atoms[i];
					if (!atom.isDeleted())
						bspf.addTuple(atom.modelIndex, atom);
				}
			}
			if (showRebondTimes) {
				long timeEnd = System.currentTimeMillis();
				Logger.debug("time to build bspf=" + (timeEnd - timeBegin) + " ms");
				bspf.stats();
				// bspf.dump();
			}
		}
	}

	int getBsptCount() {
		if (bspf == null)
			initializeBspf();
		return bspf.getBsptCount();
	}

	private final WithinModelIterator withinModelIterator = new WithinModelIterator();

	AtomIterator getWithinModelIterator(Atom atomCenter, float radius) {
		withinModelIterator.initialize(atomCenter.modelIndex, atomCenter, radius);
		return withinModelIterator;
	}

	class WithinModelIterator implements AtomIterator {

		int bsptIndex;
		Tuple center;
		float radius;
		SphereIterator bsptIter;

		void initialize(int bsptIndex, Tuple center, float radius) {
			initializeBspf();
			this.bsptIndex = bsptIndex;
			bsptIter = bspf.getSphereIterator(bsptIndex);
			this.center = center;
			this.radius = radius;
			bsptIter.initialize(center, radius);
		}

		public boolean hasNext() {
			return bsptIter.hasMoreElements();
		}

		public Atom next() {
			return (Atom) bsptIter.nextElement();
		}

		public void release() {
			bsptIter.release();
			bsptIter = null;
		}
	}

	private final WithinAnyModelIterator withinAnyModelIterator = new WithinAnyModelIterator();

	AtomIterator getWithinAnyModelIterator(Atom atomCenter, float radius) {
		withinAnyModelIterator.initialize(atomCenter, radius);
		return withinAnyModelIterator;
	}

	class WithinAnyModelIterator implements AtomIterator {

		int bsptIndex;
		Tuple center;
		float radius;
		SphereIterator bsptIter;

		void initialize(Tuple center, float radius) {
			initializeBspf();
			bsptIndex = bspf.getBsptCount();
			bsptIter = null;
			this.center = center;
			this.radius = radius;
		}

		public boolean hasNext() {
			while (bsptIter == null || !bsptIter.hasMoreElements()) {
				if (--bsptIndex < 0) {
					bsptIter = null;
					return false;
				}
				bsptIter = bspf.getSphereIterator(bsptIndex);
				bsptIter.initialize(center, radius);
			}
			return true;
		}

		public Atom next() {
			return (Atom) bsptIter.nextElement();
		}

		public void release() {
			bsptIter.release();
			bsptIter = null;
		}
	}

	// //////////////////////////////////////////////////////////////
	// autobonding/connection stuff
	// //////////////////////////////////////////////////////////////
	void doAutobond() {
		// perform bonding if necessary
		boolean doBond = (bondCount == 0 || isMultiFile || isPDB && bondCount < atomCount / 2);
		if (viewer.getForceAutoBond() || doBond && viewer.getAutoBond() && getModelSetProperty("noautobond") == null) {
			autoBond(null, null);
		}
		viewer.setShapeProperty(JmolConstants.SHAPE_STICKS, "reset", null);
	}

	// XIE: remove the final keyword
	static boolean showRebondTimes = true;

	@SuppressWarnings("unchecked")
	void rebond() {
		// from eval "connect" or from app preferences panel
		stateScripts.add("connect;");
		deleteAllBonds();
		autoBond(null, null);
	}

	int autoBond(short order, BitSet bsA, BitSet bsB) {
		if (order == JmolConstants.BOND_ORDER_NULL)
			return autoBond(bsA, bsB);
		else if (order == JmolConstants.BOND_H_REGULAR)
			return autoHbond(bsA, bsB);
		else Logger.warn("autoBond() unknown order: " + order);
		return 0;
	}

	private int autoBond(BitSet bsA, BitSet bsB) {
		if (atomCount == 0)
			return 0;
		// null values for bitsets means "all"
		if (maxBondingRadius == Float.MIN_VALUE)
			findMaxRadii();
		float bondTolerance = viewer.getBondTolerance();
		float minBondDistance = viewer.getMinBondDistance();
		float minBondDistance2 = minBondDistance * minBondDistance;
		short mad = viewer.getMadBond();
		// char chainLast = '?';
		// int indexLastCA = -1;
		// Atom atomLastCA = null;
		int nNew = 0;
		initializeBspf();

		long timeBegin = 0;
		if (showRebondTimes)
			timeBegin = System.currentTimeMillis();
		/*
		 * miguel 2006 04 02 note that the way that these loops + iterators are constructed, everything assumes that all
		 * possible pairs of atoms are going to be looked at. for example, the hemisphere iterator will only look at
		 * atom indexes that are >= (or <= ?) the specified atom. if we are going to allow arbitrary sets bsA and bsB,
		 * then this will not work. so, for now I will do it the ugly way. maybe enhance/improve in the future.
		 */
		for (int i = atomCount; --i >= 0;) {
			boolean isAtomInSetA = (bsA == null || bsA.get(i));
			boolean isAtomInSetB = (bsB == null || bsB.get(i));
			if (!isAtomInSetA & !isAtomInSetB)
				continue;
			Atom atom = atoms[i];
			// Covalent bonds
			float myBondingRadius = atom.getBondingRadiusFloat();
			if (myBondingRadius == 0)
				continue;
			float searchRadius = myBondingRadius + maxBondingRadius + bondTolerance;
			SphereIterator iter = bspf.getSphereIterator(atom.modelIndex);
			iter.initializeHemisphere(atom, searchRadius);
			while (iter.hasMoreElements()) {
				Atom atomNear = (Atom) iter.nextElement();
				if (atomNear == atom)
					continue;
				int atomIndexNear = atomNear.atomIndex;
				boolean isNearInSetA = (bsA == null || bsA.get(atomIndexNear));
				boolean isNearInSetB = (bsB == null || bsB.get(atomIndexNear));
				if (!isNearInSetA & !isNearInSetB)
					continue;
				if (!(isAtomInSetA & isNearInSetB || isAtomInSetB & isNearInSetA))
					continue;
				short order = getBondOrder(atom, myBondingRadius, atomNear, atomNear.getBondingRadiusFloat(), iter
						.foundDistance2(), minBondDistance2, bondTolerance);
				if (order > 0) {
					checkValencesAndBond(atom, atomNear, order, mad);
					nNew++;
				}
			}
			iter.release();
		}
		if (showRebondTimes) {
			long timeEnd = System.currentTimeMillis();
			Logger.debug("Time to autoBond=" + (timeEnd - timeBegin));
		}

		return nNew;
	}

	private short getBondOrder(Atom atomA, float bondingRadiusA, Atom atomB, float bondingRadiusB, float distance2,
			float minBondDistance2, float bondTolerance) {
		if (bondingRadiusA == 0 || bondingRadiusB == 0)
			return 0;
		float maxAcceptable = bondingRadiusA + bondingRadiusB + bondTolerance;
		float maxAcceptable2 = maxAcceptable * maxAcceptable;
		if (distance2 < minBondDistance2) {
			return 0;
		}
		if (distance2 <= maxAcceptable2) {
			return 1;
		}
		return 0;
	}

	boolean haveWarned = false;

	void checkValencesAndBond(Atom atomA, Atom atomB, short order, short mad) {
		if (atomA.getCurrentBondCount() > JmolConstants.MAXIMUM_AUTO_BOND_COUNT
				|| atomB.getCurrentBondCount() > JmolConstants.MAXIMUM_AUTO_BOND_COUNT) {
			if (!haveWarned)
				Logger.warn("maximum auto bond count reached");
			haveWarned = true;
			return;
		}
		int formalChargeA = atomA.getFormalCharge();
		if (formalChargeA != 0) {
			int formalChargeB = atomB.getFormalCharge();
			if ((formalChargeA < 0 && formalChargeB < 0) || (formalChargeA > 0 && formalChargeB > 0))
				return;
		}
		if (atomA.alternateLocationID != atomB.alternateLocationID && atomA.alternateLocationID != 0
				&& atomB.alternateLocationID != 0)
			return;
		addBond(bondMutually(atomA, atomB, order, mad));
	}

	float hbondMax = 3.25f;
	float hbondMin = 2.5f;
	float hbondMin2 = hbondMin * hbondMin;

	boolean hbondsCalculated;

	boolean useRasMolHbondsCalculation = true;

	int autoHbond(BitSet bsA, BitSet bsB) {
		if (useRasMolHbondsCalculation && bondCount > 0) {
			if (mmset != null)
				mmset.calcHydrogenBonds(bsA, bsB);
			return 0;
		}
		// this method is not enabled and is probably error-prone.
		// it does not take into account anything but distance,
		// and as such is not really practical.

		int nNew = 0;
		initializeBspf();
		long timeBegin = 0;
		if (showRebondTimes)
			timeBegin = System.currentTimeMillis();
		for (int i = atomCount; --i >= 0;) {
			Atom atom = atoms[i];
			int elementNumber = atom.getElementNumber();
			if (elementNumber != 7 && elementNumber != 8)
				continue;
			// float searchRadius = hbondMax;
			SphereIterator iter = bspf.getSphereIterator(atom.modelIndex);
			iter.initializeHemisphere(atom, hbondMax);
			while (iter.hasMoreElements()) {
				Atom atomNear = (Atom) iter.nextElement();
				int elementNumberNear = atomNear.getElementNumber();
				if (elementNumberNear != 7 && elementNumberNear != 8)
					continue;
				if (atomNear == atom)
					continue;
				if (iter.foundDistance2() < hbondMin2)
					continue;
				if (atom.isBonded(atomNear))
					continue;
				addBond(bondMutually(atom, atomNear, JmolConstants.BOND_H_REGULAR, (short) 1));
				nNew++;
			}
			iter.release();
		}
		if (showRebondTimes) {
			long timeEnd = System.currentTimeMillis();
			Logger.debug("Time to hbond=" + (timeEnd - timeBegin));
		}
		return nNew;
	}

	void deleteAllBonds() {
		stateScripts.clear();
		viewer.setShapeProperty(JmolConstants.SHAPE_STICKS, "reset", null);
		for (int i = 0; i < bonds.length; i++) {
			if (bonds[i] != null) {
				bonds[i].deleteAtomReferences();
				bonds[i] = null;
			}
		}
		bondCount = 0;
	}

	// XIE v10.2 method
	void deleteBond(Bond bond) {
		// what a disaster ... I hate doing this
		for (int i = bondCount; --i >= 0;) {
			if (bonds[i] == bond) {
				bonds[i].deleteAtomReferences();
				System.arraycopy(bonds, i + 1, bonds, i, bondCount - i - 1);
				--bondCount;
				bonds[bondCount] = null;
				return;
			}
		}
	}

	void deleteBonds(BitSet bs) {
		int iSrc = 0;
		int iDst = 0;
		for (; iSrc < bondCount; ++iSrc) {
			Bond bond = bonds[iSrc];
			if (!bs.get(iSrc))
				bonds[iDst++] = bond;
			else bond.deleteAtomReferences();
		}
		for (int i = bondCount; --i >= iDst;)
			bonds[i] = null;
		bondCount = iDst;
	}

	void deleteCovalentBonds() {
		int indexNoncovalent = 0;
		for (int i = 0; i < bondCount; ++i) {
			Bond bond = bonds[i];
			if (bond == null)
				continue;
			if (!bond.isCovalent()) {
				if (i != indexNoncovalent) {
					bonds[indexNoncovalent++] = bond;
					bonds[i] = null;
				}
			}
			else {
				bond.deleteAtomReferences();
				bonds[i] = null;
			}
		}
		bondCount = indexNoncovalent;
	}

	Vector stateScripts = new Vector();
	int thisFrame = 0;

	@SuppressWarnings("unchecked")
	void addStateScript(String script) {
		int iFrame = viewer.getDisplayModelIndex();
		if (thisFrame != iFrame) {
			thisFrame = iFrame;
			script = "frame " + this.getModelNumber(iFrame) + ";\n" + script;
		}
		stateScripts.add(script);
	}

	@SuppressWarnings("unchecked")
	int makeConnections(float minDistance, float maxDistance, short order, int connectOperation, BitSet bsA, BitSet bsB) {
		String stateScript = "connect " + minDistance + " " + maxDistance + " " + StateManager.escape(bsA) + " "
				+ StateManager.escape(bsB) + " " + JmolConstants.getBondOrderNameFromOrder(order) + " "
				+ JmolConstants.connectOperationName(connectOperation) + ";";
		stateScripts.add(stateScript);
		Logger.debug("makeConnections(" + minDistance + "," + maxDistance + "," + order + "," + connectOperation + ","
				+ bsA + "," + bsB + ")");

		if (connectOperation == JmolConstants.DELETE_BONDS) {
			return deleteConnections(minDistance, maxDistance, order, bsA, bsB);
		}
		if (connectOperation == JmolConstants.AUTO_BOND) {
			return autoBond(order, bsA, bsB);
		}
		if (order == JmolConstants.BOND_ORDER_NULL)
			order = JmolConstants.BOND_COVALENT_SINGLE; // default
		float minDistanceSquared = minDistance * minDistance;
		float maxDistanceSquared = maxDistance * maxDistance;
		defaultCovalentMad = viewer.getMadBond();
		short mad = getDefaultMadFromOrder(order);
		int nNew = 0;
		int nModified = 0;
		for (int iA = atomCount; --iA >= 0;) {
			if (!bsA.get(iA))
				continue;
			Atom atomA = atoms[iA];
			Point3f pointA = atomA;
			for (int iB = atomCount; --iB >= 0;) {
				if (iB == iA)
					continue;
				if (!bsB.get(iB))
					continue;
				Atom atomB = atoms[iB];
				if (atomA.modelIndex != atomB.modelIndex)
					continue;
				if (atomA.alternateLocationID != atomB.alternateLocationID && atomA.alternateLocationID != 0
						&& atomB.alternateLocationID != 0)
					continue;
				Bond bondAB = atomA.getBond(atomB);
				if (JmolConstants.MODIFY_ONLY == connectOperation && bondAB == null)
					continue;
				if (JmolConstants.CREATE_ONLY == connectOperation && bondAB != null)
					continue;
				float distanceSquared = pointA.distanceSquared(atomB);
				if (distanceSquared < minDistanceSquared || distanceSquared > maxDistanceSquared)
					continue;
				if (bondAB != null) {
					bondAB.setOrder(order);
					nNew++;
				}
				else {
					bondAtoms(atomA, atomB, order, mad);
					nModified++;
				}
			}
		}
		Logger.info(nNew + " new bonds; " + nModified + " modified");
		return nNew + nModified;
	}

	int deleteConnections(float minDistance, float maxDistance, short order, BitSet bsA, BitSet bsB) {
		BitSet bsDelete = new BitSet();
		float minDistanceSquared = minDistance * minDistance;
		float maxDistanceSquared = maxDistance * maxDistance;
		if (order != JmolConstants.BOND_ORDER_NULL && (order & JmolConstants.BOND_HYDROGEN_MASK) != 0)
			order = JmolConstants.BOND_HYDROGEN_MASK;
		int nDeleted = 0;
		for (int i = bondCount; --i >= 0;) {
			Bond bond = bonds[i];
			Atom atom1 = bond.atom1;
			Atom atom2 = bond.atom2;
			if (bsA.get(atom1.atomIndex) && bsB.get(atom2.atomIndex) || bsA.get(atom2.atomIndex)
					&& bsB.get(atom1.atomIndex)) {
				if (bond.atom1.isBonded(bond.atom2)) {
					float distanceSquared = atom1.distanceSquared(atom2);
					if (distanceSquared >= minDistanceSquared && distanceSquared <= maxDistanceSquared)
						if (order == JmolConstants.BOND_ORDER_NULL
								|| order == (bond.order & ~JmolConstants.BOND_SULFUR_MASK)
								|| (order & bond.order & JmolConstants.BOND_HYDROGEN_MASK) != 0) {
							bsDelete.set(i);
							nDeleted++;
						}
				}
			}
		}
		deleteBonds(bsDelete);
		Logger.info(nDeleted + " bonds deleted");
		return nDeleted;
	}

	// ///////////////////////

	// for Dots only

	float getMaxVanderwaalsRadius() {
		if (maxVanderwaalsRadius == Float.MIN_VALUE)
			findMaxRadii();
		return maxVanderwaalsRadius;
	}

	ShapeRenderer getRenderer(int shapeID) {
		return frameRenderer.getRenderer(shapeID, g3d);
	}

	void doUnitcellStuff() {
		/*
		 * really THREE issues here: 1) does a model have an associated unit cell that could be displayed? 2) are the
		 * coordinates fractional and so need to be transformed? 3) does the model have symmetry operations that were
		 * applied?
		 * 
		 * This must be done for each model individually.
		 * 
		 */

		if (someModelsHaveUnitcells) {
			boolean doPdbScale = (modelCount == 1);
			cellInfos = new CellInfo[modelCount];
			for (int i = 0; i < modelCount; i++)
				cellInfos[i] = new CellInfo(i, doPdbScale);
		}
		if (someModelsHaveSymmetry) {
			for (int i = 0; i < modelCount; i++) {
				mmset.setSymmetryAtomInfo(i, mmset.getModelAuxiliaryInfoInt(i, "presymmetryAtomIndex"), mmset
						.getModelAuxiliaryInfoInt(i, "presymmetryAtomCount"));
			}
		}
		else {
			int ipt = 0;
			for (int i = 0; i < modelCount; i++) {
				ipt = mmset.setSymmetryAtomInfo(i, ipt, getAtomCountInModel(i));
			}
		}
		if (someModelsHaveFractionalCoordinates) {
			for (int i = atomCount; --i >= 0;) {
				int modelIndex = atoms[i].modelIndex;
				if (!cellInfos[modelIndex].coordinatesAreFractional)
					continue;
				cellInfos[modelIndex].toCartesian(atoms[i]);
				if (Logger.isActiveLevel(Logger.LEVEL_DEBUG))
					Logger.debug("atom " + i + ": " + atoms[i]);
			}
		}
	}

	void calcAtomsMinMax(Point3f pointMin, Point3f pointMax) {
		if (atomCount <= 0)
			return; // XIE
		if (atomCount < 2) {
			pointMin.set(-10, -10, -10);
			pointMax.set(10, 10, 10);
			return;
		}
		pointMin.set(atoms[0]);
		pointMax.set(atoms[0]);
		for (int i = atomCount; --i > 0;) {
			// note that the 0 element was set above
			checkMinMax(atoms[i], pointMin, pointMax);
		}
	}

	void calcUnitCellMinMax(Point3f pointMin, Point3f pointMax) {
		if (cellInfos == null)
			return;
		for (int i = 0; i < modelCount; i++) {
			if (!cellInfos[i].coordinatesAreFractional)
				continue;
			Point3f[] vertices = cellInfos[i].unitCell.getVertices();
			for (int j = 0; j < 8; j++)
				checkMinMax(vertices[j], pointMin, pointMax);
		}
	}

	void checkMinMax(Point3f pt, Point3f pointMin, Point3f pointMax) {
		float t = pt.x;
		if (t < pointMin.x)
			pointMin.x = t;
		else if (t > pointMax.x)
			pointMax.x = t;
		t = pt.y;
		if (t < pointMin.y)
			pointMin.y = t;
		else if (t > pointMax.y)
			pointMax.y = t;
		t = pt.z;
		if (t < pointMin.z)
			pointMin.z = t;
		else if (t > pointMax.z)
			pointMax.z = t;
	}

	Point3f getAtomSetCenter(BitSet bs) {
		Point3f ptCenter = new Point3f(0, 0, 0);
		int nPoints = viewer.cardinalityOf(bs);
		if (nPoints == 0)
			return ptCenter;
		for (int i = atomCount; --i >= 0;) {
			if (bs.get(i))
				ptCenter.add(atoms[i]);
		}
		ptCenter.scale(1.0f / nPoints);
		return ptCenter;
	}

	int firstAtomOf(BitSet bs) {
		if (bs == null)
			return -1;
		for (int i = 0; i < atomCount; i++)
			if (bs.get(i)) {
				return i;
			}
		return -1;
	}

	Atom getSymmetryBaseAtom(int modelIndex, int site, int symop) {
		Frame.CellInfo[] c = cellInfos;
		if (c != null)
			for (int i = 0; i < atomCount; i++)
				if (atoms[i].modelIndex == modelIndex && atoms[i].atomSite == site && atoms[i].atomSymmetry.get(symop))
					return atoms[i];
		return null;
	}

	// /// this set of methods is used by Eval

	/**
	 * general unqualified lookup of atom set type
	 * 
	 * @param setType
	 * @return BitSet; or null if we mess up the type
	 */
	BitSet getAtomBits(String setType) {
		if (setType.equals("specialPosition"))
			return getSpecialPosition();
		if (setType.equals("symmetry"))
			return getSymmetrySet();
		if (setType.equals("unitcell"))
			return getUnitCellSet();
		if (setType.equals("hetero"))
			return getHeteroSet();
		if (setType.equals("hydrogen"))
			return getHydrogenSet();
		if (setType.equals("protein"))
			return getProteinSet();
		if (setType.equals("carbohydrate"))
			return getCarbohydrateSet();
		if (setType.equals("nucleic"))
			return getNucleicSet();
		if (setType.equals("dna"))
			return getDnaSet();
		if (setType.equals("rna"))
			return getRnaSet();
		if (setType.equals("purine"))
			return getPurineSet();
		if (setType.equals("pyrimidine"))
			return getPyrimidineSet();
		return null;
	}

	private BitSet getSpecialPosition() {
		BitSet bs = new BitSet(atomCount);
		for (int i = atomCount; --i >= 0;) {
			BitSet bsSym = atoms[i].getAtomSymmetry();
			if (bsSym != null && viewer.cardinalityOf(bsSym) > 1)
				bs.set(i);
		}
		return bs;
	}

	private BitSet getUnitCellSet() {
		BitSet bsCell = new BitSet();
		UnitCell unitcell = viewer.getCurrentUnitCell();
		if (unitcell == null)
			return bsCell;
		Point3f cell = new Point3f(unitcell.getFractionalOffset());
		cell.x += 1;
		cell.y += 1;
		cell.z += 1;
		for (int i = atomCount; --i >= 0;)
			if (atoms[i].isInLatticeCell(cell))
				bsCell.set(i);
		return bsCell;
	}

	private BitSet getSymmetrySet() {
		// presumption here is that one cannot DELETE atoms
		BitSet bs = new BitSet(atomCount);
		for (int i = atomCount; --i >= 0;)
			bs.set(i);
		for (int i = 0; i < modelCount; i++) {
			int atomIndex = mmset.getPreSymmetryAtomIndex(i);
			int preSymAtomCount = mmset.getPreSymmetryAtomCount(i);
			if (atomIndex < 0)
				continue;
			for (int iatom = atomIndex + preSymAtomCount; --iatom >= atomIndex;)
				bs.clear(iatom);
		}
		return bs;
	}

	private BitSet getHeteroSet() {
		BitSet bs = new BitSet();
		for (int i = atomCount; --i >= 0;)
			if (atoms[i].isHetero())
				bs.set(i);
		return bs;
	}

	private BitSet getHydrogenSet() {
		BitSet bs = new BitSet();
		for (int i = atomCount; --i >= 0;) {
			if (atoms[i].getElementNumber() == 1)
				bs.set(i);
		}
		return bs;
	}

	private BitSet getProteinSet() {
		BitSet bs = new BitSet();
		for (int i = atomCount; --i >= 0;)
			if (atoms[i].isProtein())
				bs.set(i);
		return bs;
	}

	private BitSet getCarbohydrateSet() {
		BitSet bs = new BitSet();
		for (int i = atomCount; --i >= 0;)
			if (atoms[i].isCarbohydrate())
				bs.set(i);
		return bs;
	}

	private BitSet getNucleicSet() {
		BitSet bs = new BitSet();
		for (int i = atomCount; --i >= 0;)
			if (atoms[i].isNucleic())
				bs.set(i);
		return bs;
	}

	private BitSet getDnaSet() {
		BitSet bs = new BitSet();
		for (int i = atomCount; --i >= 0;)
			if (atoms[i].isDna())
				bs.set(i);
		return bs;
	}

	private BitSet getRnaSet() {
		BitSet bs = new BitSet();
		for (int i = atomCount; --i >= 0;)
			if (atoms[i].isRna())
				bs.set(i);
		return bs;
	}

	private BitSet getPurineSet() {
		BitSet bs = new BitSet();
		for (int i = atomCount; --i >= 0;)
			if (atoms[i].isPurine())
				bs.set(i);
		return bs;
	}

	private BitSet getPyrimidineSet() {
		BitSet bs = new BitSet();
		for (int i = atomCount; --i >= 0;)
			if (atoms[i].isPyrimidine())
				bs.set(i);
		return bs;
	}

	/**
	 * general lookup for String type
	 * 
	 * @param setType
	 * @param specInfo
	 * @return BitSet or null in certain cases
	 */
	BitSet getAtomBits(String setType, String specInfo) {
		if (setType.equals("IdentifierOrNull"))
			return getIdentifierOrNull(specInfo);
		if (setType.equals("SpecAtom"))
			return getSpecAtom(specInfo);
		if (setType.equals("SpecName"))
			return getSpecName(specInfo);
		if (setType.equals("SpecAlternate"))
			return getSpecAlternate(specInfo);
		if (setType.equals("SpecModel"))
			return getSpecModel(specInfo);
		return null;
	}

	/**
	 * overhauled by RMH Nov 1, 2006.
	 * 
	 * @param identifier
	 * @return null or bs
	 */
	private BitSet getIdentifierOrNull(String identifier) {
		// a primitive lookup scheme when [ ] are not used
		// nam
		// na?
		// nam45
		// nam45C
		// nam45^
		// nam45^A
		// nam45^AC -- note, no colon here -- if present, handled separately
		// nam4? does NOT match anything for PDB files, but might for others
		// atom specifiers:
		// H?
		// H32
		// H3?

		// in the case of a ?, we take the whole thing

		BitSet bs = getSpecNameOrNull(identifier);
		if (bs != null || identifier.indexOf("?") > 0)
			return bs;

		int pt = identifier.indexOf("*");
		if (pt > 0)
			return getSpecNameOrNull(identifier.substring(0, pt) + "??????????" + identifier.substring(pt + 1));
		int len = identifier.length();
		pt = 0;
		while (pt < len && Character.isLetter(identifier.charAt(pt)))
			++pt;
		bs = getSpecNameOrNull(identifier.substring(0, pt));
		if (pt == len)
			return bs;
		if (bs == null)
			bs = new BitSet();
		//
		// look for a sequence number or sequence number ^ insertion code
		//
		int pt0 = pt;
		while (pt < len && Character.isDigit(identifier.charAt(pt)))
			++pt;
		int seqNumber = 0;
		try {
			seqNumber = Integer.parseInt(identifier.substring(pt0, pt));
		}
		catch (NumberFormatException nfe) {
			return null;
		}
		char insertionCode = ' ';
		if (pt < len && identifier.charAt(pt) == '^')
			if (++pt < len)
				insertionCode = identifier.charAt(pt);
		int seqcode = Group.getSeqcode(seqNumber, insertionCode);
		BitSet bsInsert = getSpecSeqcode(seqcode, false);
		if (bsInsert == null) {
			if (insertionCode != ' ')
				bsInsert = getSpecSeqcode(Character.toUpperCase(identifier.charAt(pt)), false);
			if (bsInsert == null)
				return null;
			pt++;
		}
		bs.and(bsInsert);
		if (pt >= len)
			return bs;
		//
		// look for a chain spec -- no colon
		//
		char chainID = identifier.charAt(pt++);
		bs.and(getSpecChain(chainID));
		if (pt == len)
			return bs;
		//
		// not applicable
		//
		return null;
	}

	private BitSet getSpecAtom(String atomSpec) {
		BitSet bs = new BitSet();
		atomSpec = atomSpec.toUpperCase();
		for (int i = atomCount; --i >= 0;) {
			if (atoms[i].isAtomNameMatch(atomSpec)) {
				bs.set(i);
			}
		}
		return bs;
	}

	private BitSet getSpecName(String name) {
		BitSet bs = getSpecNameOrNull(name);
		if (bs != null)
			return bs;
		int pt = name.indexOf("*");
		if (pt > 0) {
			bs = getSpecNameOrNull(name.substring(0, pt) + "??????????" + name.substring(pt + 1));
		}
		return (bs == null ? new BitSet() : bs);
	}

	private BitSet getSpecNameOrNull(String name) {
		BitSet bs = null;
		name = name.toUpperCase();
		for (int i = atomCount; --i >= 0;)
			if (atoms[i].isGroup3OrNameMatch(name)) {
				if (bs == null)
					bs = new BitSet(i + 1);
				bs.set(i);
			}
		return bs;
	}

	private BitSet getSpecAlternate(String alternateSpec) {
		BitSet bs = new BitSet();
		for (int i = atomCount; --i >= 0;) {
			if (atoms[i].isAlternateLocationMatch(alternateSpec))
				bs.set(i);
		}
		return bs;
	}

	private BitSet getSpecModel(String modelTag) {
		int modelNumber = -1;
		try {
			modelNumber = Integer.parseInt(modelTag);
		}
		catch (NumberFormatException nfe) {
		}
		return getModelAtomBitSet(getModelNumberIndex(modelNumber));
	}

	/**
	 * general lookup for integer type -- from Eval
	 * 
	 * @param setType
	 * @param specInfo
	 * @return bitset; null only if we mess up with name
	 */
	BitSet getAtomBits(String setType, int specInfo) {
		if (setType.equals("SpecResid"))
			return getSpecResid(specInfo);
		if (setType.equals("SpecSeqcode"))
			return getSpecSeqcode(specInfo, true);
		if (setType.equals("SpecChain"))
			return getSpecChain((char) specInfo);
		if (setType.equals("atomno"))
			return getSpecAtomNumber(specInfo);
		return null;
	}

	private BitSet getSpecResid(int resid) {
		BitSet bs = new BitSet();
		for (int i = atomCount; --i >= 0;) {
			if (atoms[i].getGroupID() == resid)
				bs.set(i);
		}
		return bs;
	}

	private BitSet getSpecSeqcode(int seqcode, boolean returnEmpty) {
		BitSet bs = new BitSet();
		int seqNum = (seqcode >> 8);
		boolean isEmpty = true;
		char insCode = Group.getInsertionCode(seqcode);
		switch (insCode) {
		case '?':
			for (int i = atomCount; --i >= 0;) {
				int atomSeqcode = atoms[i].getSeqcode();
				if ((seqNum == 0 || seqNum == (atomSeqcode >> 8)) && (atomSeqcode & 0xFF) != 0) {
					bs.set(i);
					isEmpty = false;
				}
			}
			break;
		default:
			for (int i = atomCount; --i >= 0;) {
				int atomSeqcode = atoms[i].getSeqcode();
				if (seqcode == atomSeqcode || seqNum == 0 && seqcode == (atomSeqcode & 0xFF) || insCode == '*'
						&& seqNum == (atomSeqcode >> 8)) {
					bs.set(i);
					isEmpty = false;
				}
			}
		}
		return (!isEmpty || returnEmpty ? bs : null);
	}

	private BitSet getSpecChain(char chain) {
		boolean caseSensitive = viewer.getChainCaseSensitive();
		if (!caseSensitive)
			chain = Character.toUpperCase(chain);
		BitSet bs = new BitSet();
		for (int i = atomCount; --i >= 0;) {
			char ch = atoms[i].getChainID();
			if (!caseSensitive)
				ch = Character.toUpperCase(ch);
			if (chain == ch)
				bs.set(i);
		}
		return bs;
	}

	private BitSet getSpecAtomNumber(int atomno) {
		BitSet bs = new BitSet();
		for (int i = atomCount; --i >= 0;) {
			if (atoms[i].getAtomNumber() == atomno)
				bs.set(i);
		}
		return bs;
	}

	/**
	 * general lookup involving a range
	 * 
	 * @param setType
	 * @param specInfo
	 * @return BitSet; or null if mess up with type
	 */
	BitSet getAtomBits(String setType, int[] specInfo) {
		if (setType.equals("SpecSeqcodeRange"))
			return getSpecSeqcodeRange(specInfo[0], specInfo[1]);
		if (setType.equals("Cell"))
			return getCellSet(specInfo[0], specInfo[1], specInfo[2]);
		return null;
	}

	private BitSet getSpecSeqcodeRange(int seqcodeA, int seqcodeB) {
		BitSet bs = new BitSet();
		mmset.selectSeqcodeRange(seqcodeA, seqcodeB, bs);
		return bs;
	}

	private BitSet getCellSet(int ix, int jy, int kz) {
		BitSet bs = new BitSet();
		Point3f cell = new Point3f(ix / 1000f, jy / 1000f, kz / 1000f);
		for (int i = atomCount; --i >= 0;)
			if (atoms[i].isInLatticeCell(cell))
				bs.set(i);
		return bs;
	}

	BitSet getModelAtomBitSet(int modelIndex) {
		BitSet bs = new BitSet();
		for (int i = 0; i < atomCount; i++)
			if (atoms[i].modelIndex == modelIndex)
				bs.set(i);
		return bs;
	}

	BitSet getModelBitSet(BitSet atomList) {
		BitSet bs = new BitSet();
		for (int i = 0; i < atomCount; i++)
			if (atomList.get(i))
				bs.set(atoms[i].modelIndex);
		return bs;
	}

	void setLabel(String label, int atomIndex) {
	}

	void findElementsPresent() {
		elementsPresent = new BitSet();
		for (int i = atomCount; --i >= 0;) {
			int n = atoms[i].getAtomicAndIsotopeNumber();
			if (n >= 128)
				n = JmolConstants.elementNumberMax + JmolConstants.altElementIndexFromNumber(n);
			elementsPresent.set(n);
		}
	}

	BitSet getElementsPresentBitSet() {
		return elementsPresent;
	}

	void calcSelectedGroupsCount(BitSet bsSelected) {
		mmset.calcSelectedGroupsCount(bsSelected);
	}

	void calcSelectedMonomersCount(BitSet bsSelected) {
		mmset.calcSelectedMonomersCount(bsSelected);
	}

	BitSet selectedMolecules = new BitSet();
	BitSet bsTemp = new BitSet();
	int selectedMoleculeCount;

	void calcSelectedMoleculesCount(BitSet bsSelected) {
		if (moleculeCount == 0)
			getMolecules();
		selectedMolecules.xor(selectedMolecules);
		selectedMoleculeCount = 0;
		for (int i = 0; i < moleculeCount; i++) {
			bsTemp.clear();
			bsTemp.or(bsSelected);
			bsTemp.and(molecules[i].atomList);
			if (bsTemp.length() > 0) {
				selectedMolecules.set(i);
				selectedMoleculeCount++;
			}
		}
	}

	void findMaxRadii() {
		for (int i = atomCount; --i >= 0;) {
			Atom atom = atoms[i];
			float bondingRadius = atom.getBondingRadiusFloat();
			if (bondingRadius > maxBondingRadius)
				maxBondingRadius = bondingRadius;
			float vdwRadius = atom.getVanderwaalsRadiusFloat();
			if (vdwRadius > maxVanderwaalsRadius)
				maxVanderwaalsRadius = vdwRadius;
		}
	}

	BitSet bsSurfaceSet;

	void setSurfaceAtoms(BitSet bsSurface, BitSet bsEnclosed) {
		bsSurfaceSet = (BitSet) bsEnclosed.clone();
		surfaceDistances = null;
		int n = viewer.cardinalityOf(bsSurface);
		if (n == 0) {
			surfaceAtoms = null;
			return;
		}
		surfaceAtoms = new int[n];
		for (int i = atomCount, pt = 0; --i >= 0;)
			if (bsSurface.get(i))
				surfaceAtoms[pt++] = i;
	}

	float getSurfaceDistance(int atomIndex) {
		if (surfaceAtoms == null)
			return -1;
		if (surfaceDistances == null)
			calcSurfaceDistances();
		return surfaceDistances[atomIndex];
	}

	float surfaceDistanceMax;

	float getSurfaceDistanceMax() {
		if (surfaceDistances == null)
			calcSurfaceDistances();
		return surfaceDistanceMax;
	}

	private void calcSurfaceDistances() {
		surfaceDistanceMax = 0;
		if (bsSurfaceSet == null)
			return;
		surfaceDistances = new float[atomCount];
		for (int i = 0; i < atomCount; i++) {
			surfaceDistances[i] = -1;
			if (bsSurfaceSet.get(i)) {
				float dMin = Float.MAX_VALUE;
				Atom atom = atoms[i];
				for (int j = surfaceAtoms.length; --j >= 0;) {
					float d = atoms[surfaceAtoms[j]].distance(atom);
					dMin = Math.min(d, dMin);
				}
				surfaceDistances[i] = dMin;
				surfaceDistanceMax = Math.max(surfaceDistanceMax, dMin);
			}
		}
	}

	void calcBfactorRange() {
		calcBfactorRange(null);
	}

	void clearBfactorRange() {
		hasBfactorRange = false;
	}

	void calcBfactorRange(BitSet bs) {
		if (!hasBfactorRange) {
			bfactor100Lo = Integer.MAX_VALUE;
			bfactor100Hi = Integer.MIN_VALUE;
			for (int i = atomCount; --i > 0;)
				if (bs == null || bs.get(i)) {
					int bf = atoms[i].getBfactor100();
					if (bf < bfactor100Lo)
						bfactor100Lo = bf;
					else if (bf > bfactor100Hi)
						bfactor100Hi = bf;
				}
			hasBfactorRange = true;
		}
	}

	int getBfactor100Lo() {
		if (!hasBfactorRange) {
			if (viewer.isRangeSelected()) {
				calcBfactorRange(viewer.getSelectionSet());
			}
			else {
				calcBfactorRange(null);
			}
		}
		return bfactor100Lo;
	}

	int getBfactor100Hi() {
		getBfactor100Lo();
		return bfactor100Hi;
	}

	BitSet getVisibleSet() {
		BitSet bs = new BitSet();
		for (int i = atomCount; --i >= 0;)
			if (atoms[i].isVisible())
				bs.set(i);
		return bs;
	}

	BitSet getClickableSet() {
		BitSet bs = new BitSet();
		for (int i = atomCount; --i >= 0;)
			if (atoms[i].clickabilityFlags != 0)
				bs.set(i);
		return bs;
	}

	// //////////////////////////////////////////////////////////////
	// measurements
	// //////////////////////////////////////////////////////////////

	float getMeasurement(int[] countPlusIndices) {
		float value = Float.NaN;
		if (countPlusIndices == null)
			return value;
		int count = countPlusIndices[0];
		if (count < 2)
			return value;
		for (int i = count; --i >= 0;)
			if (countPlusIndices[i + 1] < 0) {
				return value;
			}
		switch (count) {
		case 2:
			value = getDistance(countPlusIndices[1], countPlusIndices[2]);
			break;
		case 3:
			value = getAngle(countPlusIndices[1], countPlusIndices[2], countPlusIndices[3]);
			break;
		case 4:
			value = getTorsion(countPlusIndices[1], countPlusIndices[2], countPlusIndices[3], countPlusIndices[4]);
			break;
		default:
			Logger.error("Invalid count in measurement calculation:" + count);
			throw new IndexOutOfBoundsException();
		}

		return value;
	}

	float getDistance(int atomIndexA, int atomIndexB) {
		return atoms[atomIndexA].distance(atoms[atomIndexB]);
	}

	Vector3f vectorBA;
	Vector3f vectorBC;

	float getAngle(int atomIndexA, int atomIndexB, int atomIndexC) {
		if (vectorBA == null) {
			vectorBA = new Vector3f();
			vectorBC = new Vector3f();
		}
		Point3f pointA = atoms[atomIndexA];
		Point3f pointB = atoms[atomIndexB];
		Point3f pointC = atoms[atomIndexC];
		vectorBA.sub(pointA, pointB);
		vectorBC.sub(pointC, pointB);
		float angle = vectorBA.angle(vectorBC);
		float degrees = toDegrees(angle);
		return degrees;
	}

	float getTorsion(int atomIndexA, int atomIndexB, int atomIndexC, int atomIndexD) {
		return computeTorsion(atoms[atomIndexA], atoms[atomIndexB], atoms[atomIndexC], atoms[atomIndexD]);
	}

	static float toDegrees(float angleRadians) {
		return angleRadians * 180 / (float) Math.PI;
	}

	static float computeTorsion(Point3f p1, Point3f p2, Point3f p3, Point3f p4) {

		float ijx = p1.x - p2.x;
		float ijy = p1.y - p2.y;
		float ijz = p1.z - p2.z;

		float kjx = p3.x - p2.x;
		float kjy = p3.y - p2.y;
		float kjz = p3.z - p2.z;

		float klx = p3.x - p4.x;
		float kly = p3.y - p4.y;
		float klz = p3.z - p4.z;

		float ax = ijy * kjz - ijz * kjy;
		float ay = ijz * kjx - ijx * kjz;
		float az = ijx * kjy - ijy * kjx;
		float cx = kjy * klz - kjz * kly;
		float cy = kjz * klx - kjx * klz;
		float cz = kjx * kly - kjy * klx;

		float ai2 = 1f / (ax * ax + ay * ay + az * az);
		float ci2 = 1f / (cx * cx + cy * cy + cz * cz);

		float ai = (float) Math.sqrt(ai2);
		float ci = (float) Math.sqrt(ci2);
		float denom = ai * ci;
		float cross = ax * cx + ay * cy + az * cz;
		float cosang = cross * denom;
		if (cosang > 1) {
			cosang = 1;
		}
		if (cosang < -1) {
			cosang = -1;
		}

		float torsion = toDegrees((float) Math.acos(cosang));
		float dot = ijx * cx + ijy * cy + ijz * cz;
		float absDot = Math.abs(dot);
		torsion = (dot / absDot > 0) ? torsion : -torsion;
		return torsion;
	}

	// //////////////////////////////////////////////////////////////

	BitSet getMoleculeBitSet(int atomIndex) {
		if (moleculeCount == 0)
			getMolecules();
		for (int i = 0; i < moleculeCount; i++)
			if (molecules[i].atomList.get(atomIndex))
				return molecules[i].atomList;
		return null;
	}

	BitSet getMoleculeBitSet(BitSet bs) {
		// returns cumulative sum of all atoms in molecules containing these atoms
		if (moleculeCount == 0)
			getMolecules();
		BitSet bsResult = (BitSet) bs.clone();
		BitSet bsInitial = (BitSet) bs.clone();
		int iLastBit;
		while ((iLastBit = bsInitial.length()) > 0) {
			bsTemp = getMoleculeBitSet(iLastBit - 1);
			bsInitial.andNot(bsTemp);
			bsResult.or(bsTemp);
		}
		return bsResult;
	}

	private void getMolecules() {
		if (moleculeCount > 0)
			return;
		moleculeCount = 0;
		int atomCount = getAtomCount();
		BitSet atomlist = new BitSet(atomCount);
		BitSet bs = new BitSet(atomCount);
		int thisModelIndex = -1;
		int modelIndex = -1;
		int indexInModel = -1;
		int moleculeCount0 = -1;
		for (int i = 0; i < atomCount; i++)
			if (!atomlist.get(i) && !bs.get(i)) {
				modelIndex = atoms[i].modelIndex;
				if (modelIndex != thisModelIndex) {
					indexInModel = -1;
					mmset.getModel(modelIndex).firstMolecule = moleculeCount;
					moleculeCount0 = moleculeCount - 1;
					thisModelIndex = modelIndex;
				}
				indexInModel++;
				bs = getConnectedBitSet(i);
				atomlist.or(bs);
				if (moleculeCount == molecules.length)
					molecules = (Molecule[]) ArrayUtil.setLength(molecules, moleculeCount * 2);
				molecules[moleculeCount] = new Molecule(moleculeCount, bs, thisModelIndex, indexInModel);
				mmset.getModel(thisModelIndex).moleculeCount = moleculeCount - moleculeCount0;
				moleculeCount++;
			}
	}

	private BitSet getConnectedBitSet(int atomIndex) {
		int atomCount = getAtomCount();
		BitSet bs = new BitSet(atomCount);
		BitSet bsToTest = getModelAtomBitSet(atoms[atomIndex].modelIndex);
		getCovalentlyConnectedBitSet(atoms[atomIndex], bs, bsToTest);
		return bs;
	}

	private void getCovalentlyConnectedBitSet(Atom atom, BitSet bs, BitSet bsToTest) {
		int atomIndex = atom.atomIndex;
		if (!bsToTest.get(atomIndex))
			return;
		bsToTest.clear(atomIndex);
		bs.set(atomIndex);
		if (atom.bonds == null)
			return;
		for (int i = atom.bonds.length; --i >= 0;) {
			Bond bond = atom.bonds[i];
			if ((bond.order & JmolConstants.BOND_HYDROGEN_MASK) != 0)
				continue;
			if (bond.atom1 == atom) {
				getCovalentlyConnectedBitSet(bond.atom2, bs, bsToTest);
			}
			else {
				getCovalentlyConnectedBitSet(bond.atom1, bs, bsToTest);
			}
		}
	}

	int getMoleculeCount() {
		return moleculeCount;
	}

	@SuppressWarnings("unchecked")
	Vector getMoleculeInfo(BitSet bsAtoms) {
		if (moleculeCount == 0)
			getMolecules();
		Vector V = new Vector();
		for (int i = 0; i < moleculeCount; i++) {
			bsTemp = (BitSet) bsAtoms.clone();
			bsTemp.and(molecules[i].atomList);
			if (bsTemp.length() > 0)
				V.add(molecules[i].getInfo());
		}
		return V;
	}

	int getMoleculeIndex(int atomIndex) {
		if (moleculeCount == 0)
			getMolecules();
		for (int i = 0; i < moleculeCount; i++) {
			if (molecules[i].atomList.get(atomIndex))
				return molecules[i].indexInModel;
		}
		return 0;
	}

	int getFirstMoleculeIndexInModel(int modelIndex) {
		if (moleculeCount == 0)
			getMolecules();
		return mmset.getModel(modelIndex).firstMolecule;
	}

	int getMoleculeCountInModel(int modelIndex) {
		// not implemented for pop-up menu -- will slow it down.
		int n = 0;
		if (moleculeCount == 0)
			getMolecules();
		for (int i = 0; i < modelCount; i++) {
			if (modelIndex == i || modelIndex < 0)
				n += mmset.getModel(i).moleculeCount;
		}
		return n;
	}

	// //////////////////////////////////////////////////////////////

	final static int MAX_BONDS_LENGTH_TO_CACHE = 5;
	final static int MAX_NUM_TO_CACHE = 200;
	int[] numCached = new int[MAX_BONDS_LENGTH_TO_CACHE];
	Bond[][][] freeBonds = new Bond[MAX_BONDS_LENGTH_TO_CACHE][][];
	{
		for (int i = MAX_BONDS_LENGTH_TO_CACHE; --i > 0;)
			// .GT. 0
			freeBonds[i] = new Bond[MAX_NUM_TO_CACHE][];
	}

	void addBondToAtom(Atom atom, Bond bond) {
		if (atom.bonds == null) {
			atom.bonds = new Bond[1];
			atom.bonds[0] = bond;
		}
		else {
			atom.bonds = addToBonds(bond, atom.bonds);
		}
	}

	Bond[] addToBonds(Bond newBond, Bond[] oldBonds) {
		Bond[] newBonds;
		if (oldBonds == null) {
			if (numCached[1] > 0)
				newBonds = freeBonds[1][--numCached[1]];
			else newBonds = new Bond[1];
			newBonds[0] = newBond;
		}
		else {
			int oldLength = oldBonds.length;
			int newLength = oldLength + 1;
			if (newLength < MAX_BONDS_LENGTH_TO_CACHE && numCached[newLength] > 0)
				newBonds = freeBonds[newLength][--numCached[newLength]];
			else newBonds = new Bond[newLength];
			newBonds[oldLength] = newBond;
			for (int i = oldLength; --i >= 0;)
				newBonds[i] = oldBonds[i];
			if (oldLength < MAX_BONDS_LENGTH_TO_CACHE && numCached[oldLength] < MAX_NUM_TO_CACHE)
				freeBonds[oldLength][numCached[oldLength]++] = oldBonds;
		}
		return newBonds;
	}

	void freeBondsCache() {
		for (int i = MAX_BONDS_LENGTH_TO_CACHE; --i > 0;) { // .GT. 0
			numCached[i] = 0;
			Bond[][] bondsCache = freeBonds[i];
			for (int j = bondsCache.length; --j >= 0;)
				bondsCache[j] = null;
		}
	}

	Point3f getAveragePosition(int atomIndex1, int atomIndex2) {
		Atom atom1 = atoms[atomIndex1];
		Atom atom2 = atoms[atomIndex2];
		return new Point3f((atom1.x + atom2.x) / 2, (atom1.y + atom2.y) / 2, (atom1.z + atom2.z) / 2);
	}

	Vector3f getAtomVector(int atomIndex1, int atomIndex2) {
		Vector3f V = new Vector3f(atoms[atomIndex1]);
		V.sub(atoms[atomIndex2]);
		return V;
	}

	Vector3f getModelDipole() {
		Vector3f dipole;
		dipole = (Vector3f) mmset.getModelSetAuxiliaryInfo("dipole");
		if (dipole == null)
			dipole = (Vector3f) mmset.getModelSetAuxiliaryInfo("DIPOLE_VEC");
		return dipole;
	}

	final static float E_ANG_PER_DEBYE = 0.208194f;

	void getBondDipoles() {
		if (partialCharges == null)
			return;
		loadShape(JmolConstants.SHAPE_DIPOLES);
		Dipoles dipoles = (Dipoles) shapes[JmolConstants.SHAPE_DIPOLES];
		dipoles.clear(true);
		for (int i = bondCount; --i >= 0;) {
			if (!bonds[i].isCovalent())
				continue;
			Atom atom1 = bonds[i].atom1;
			Atom atom2 = bonds[i].atom2;
			float c1 = partialCharges[atom1.atomIndex];
			float c2 = partialCharges[atom2.atomIndex];
			if (c1 != c2) {
				Dipole dipole = dipoles.findDipole(atom1, atom2, true);
				float value = (c1 - c2) / 2f * atom1.distance(atom2) / E_ANG_PER_DEBYE;
				if (value < 0) {
					dipole.set(atom2, atom1, -value);
				}
				else {
					dipole.set(atom1, atom2, value);
				}
				dipole.type = Dipole.DIPOLE_TYPE_BOND;
				dipole.modelIndex = atom1.modelIndex;
			}
		}
	}

	String getSymmetryInfoAsString(int modelIndex) {
		if (cellInfos == null)
			return "no symmetry information";
		return cellInfos[modelIndex].symmetryInfoString;
	}

	final static float toRadians = (float) Math.PI * 2 / 360;
	final static Point3f[] unitCubePoints = { new Point3f(0, 0, 0), new Point3f(0, 0, 1), new Point3f(0, 1, 0),
			new Point3f(0, 1, 1), new Point3f(1, 0, 0), new Point3f(1, 0, 1), new Point3f(1, 1, 0),
			new Point3f(1, 1, 1), };

	class CellInfo {

		int modelIndex;
		boolean coordinatesAreFractional;
		String spaceGroup;
		int symmetryCount;
		String[] symmetryOperations;
		String symmetryInfoString;
		UnitCell unitCell;

		CellInfo(int modelIndex, boolean doPdbScale) {
			notionalUnitcell = (float[]) mmset.getModelAuxiliaryInfo(modelIndex, "notionalUnitcell");
			this.modelIndex = modelIndex;
			spaceGroup = (String) getModelAuxiliaryInfo(modelIndex, "spaceGroup");
			if (spaceGroup == null || spaceGroup == "")
				spaceGroup = "spacegroup unspecified";
			symmetryCount = mmset.getModelAuxiliaryInfoInt(modelIndex, "symmetryCount");
			symmetryOperations = (String[]) mmset.getModelAuxiliaryInfo(modelIndex, "symmetryOperations");
			symmetryInfoString = "Spacegroup: " + spaceGroup;
			if (symmetryOperations == null) {
				symmetryInfoString += "\nNumber of symmetry operations: ?" + "\nSymmetry Operations: unspecified\n";
			}
			else {
				symmetryInfoString += "\nNumber of symmetry operations: " + (symmetryCount == 0 ? 1 : symmetryCount)
						+ "\nSymmetry Operations:";
				for (int i = 0; i < symmetryCount; i++)
					symmetryInfoString += "\n" + symmetryOperations[i];
			}
			symmetryInfoString += "\n";
			coordinatesAreFractional = mmset.getModelAuxiliaryInfoBoolean(modelIndex, "coordinatesAreFractional");
			if (notionalUnitcell == null || notionalUnitcell[0] == 0)
				return;
			unitCell = new UnitCell(notionalUnitcell);
			showInfo();
		}

		UnitCell getUnitCell() {
			return unitCell;
		}

		float[] getNotionalUnitCell() {
			return (unitCell == null ? null : unitCell.getNotionalUnitCell());
		}

		void toCartesian(Point3f pt) {
			unitCell.toCartesian(pt);
		}

		void toFractional(Point3f pt) {
			unitCell.toFractional(pt);
		}

		void showInfo() {
			if (Logger.isActiveLevel(Logger.LEVEL_DEBUG))
				Logger.debug("cellInfos[" + modelIndex + "]:\n" + unitCell.dumpInfo(true));
		}

		String getUnitCellInfo() {
			return (unitCell == null ? "no unit cell information" : unitCell.dumpInfo(false));
		}
	}

	void convertFractionalCoordinates(int modelIndex, Point3f pt) {
		if (modelIndex < 0)
			modelIndex = 0;
		if (modelIndex >= cellInfos.length || cellInfos[modelIndex] == null)
			return;
		String str = "Frame convertFractional " + pt + "--->";
		cellInfos[modelIndex].toCartesian(pt);
		Logger.info(str + pt);
	}

	boolean reportFormalCharges = false;

	void setFormalCharges(BitSet bs, int formalCharge) {
		for (int i = 0; i < atomCount; i++)
			if (bs.get(i))
				atoms[i].setFormalCharge(formalCharge);
		reportFormalCharges = true;
	}

	void setAtomCoord(int atomIndex, float x, float y, float z) {
		if (atomIndex < 0 || atomIndex >= atomCount)
			return;
		atoms[atomIndex].x = x;
		atoms[atomIndex].y = y;
		atoms[atomIndex].z = z;
	}

	void setAtomCoordRelative(int atomIndex, float x, float y, float z) {
		if (atomIndex < 0 || atomIndex >= atomCount)
			return;
		atoms[atomIndex].x += x;
		atoms[atomIndex].y += y;
		atoms[atomIndex].z += z;
	}

	void setAtomCoordRelative(BitSet atomSet, float x, float y, float z) {
		for (int i = 0; i < atomCount; i++)
			if (atomSet.get(i))
				setAtomCoordRelative(i, x, y, z);
	}

	String hybridization;

	boolean getPrincipalAxes(int atomIndex, Vector3f z, Vector3f x, String lcaoTypeRaw, boolean hybridizationCompatible) {
		String lcaoType = (lcaoTypeRaw.length() > 0 && lcaoTypeRaw.charAt(0) == '-' ? lcaoTypeRaw.substring(1)
				: lcaoTypeRaw);
		Atom atom = atoms[atomIndex];
		hybridization = "";
		z.set(0, 0, 0);
		x.set(0, 0, 0);
		Atom atom1 = atom;
		Atom atom2 = atom;
		int nBonds = 0;
		float _180 = (float) Math.PI * 0.95f;
		Vector3f n = new Vector3f();
		Vector3f x2 = new Vector3f();
		Vector3f x3 = new Vector3f(3.14159f, 2.71828f, 1.41421f);
		Vector3f x4 = new Vector3f();
		Vector3f y1 = new Vector3f();
		Vector3f y2 = new Vector3f();
		if (atom.bonds != null)
			for (int i = atom.bonds.length; --i >= 0;)
				if (atom.bonds[i].isCovalent()) {
					++nBonds;
					atom1 = atom.bonds[i].getOtherAtom(atom);
					n.sub(atom, atom1);
					n.normalize();
					z.add(n);
					switch (nBonds) {
					case 1:
						x.set(n);
						atom2 = atom1;
						break;
					case 2:
						x2.set(n);
						break;
					case 3:
						x3.set(n);
						x4.set(-z.x, -z.y, -z.z);
						break;
					case 4:
						x4.set(n);
						break;
					default:
						i = -1;
					}
				}
		switch (nBonds) {
		case 0:
			z.set(0, 0, 1);
			x.set(1, 0, 0);
			break;
		case 1:
			if (lcaoType.indexOf("sp3") == 0) { // align z as sp3 orbital
				hybridization = "sp3";
				x.cross(x3, z);
				y1.cross(z, x);
				x.normalize();
				y1.normalize();
				y2.set(x);
				z.normalize();
				x.scaleAdd(2.828f, x, z); // 2*sqrt(2)
				if (!lcaoType.equals("sp3a") && !lcaoType.equals("sp3")) {
					x.normalize();
					AxisAngle4f a = new AxisAngle4f(z.x, z.y, z.z, (lcaoType.equals("sp3b") ? 1 : -1) * 2.09439507f); // PI*2/3
					Matrix3f m = new Matrix3f();
					m.setIdentity();
					m.set(a);
					m.transform(x);
				}
				z.set(x);
				x.cross(y1, z);
				break;
			}
			hybridization = "sp";
			if (atom1.getCovalentBondCount() == 3) {
				// special case, for example R2C=O oxygen
				getPrincipalAxes(atom1.atomIndex, z, x3, lcaoType, false);
				x3.set(x);
				if (lcaoType.indexOf("sp2") == 0) { // align z as sp2 orbital
					hybridization = "sp2";
					z.scale(-1);
				}
			}
			x.cross(x3, z);
			break;
		case 2:
			if (z.length() < 0.1) {
				// linear A--X--B
				hybridization = "sp";
				if (!lcaoType.equals("pz")) {
					if (atom1.getCovalentBondCount() != 3)
						atom1 = atom2;
					if (atom1.getCovalentBondCount() == 3) {
						// special case, for example R2C=C=CR2 central carbon
						getPrincipalAxes(atom1.atomIndex, x, z, "pz", false);
						if (lcaoType.equals("px"))
							x.scale(-1);
						z.set(x2);
						break;
					}
				}
				z.set(x);
				x.cross(x3, z);
				break;
			}
			// bent A--X--B
			hybridization = (lcaoType.indexOf("sp3") == 0 ? "sp3" : "sp2");
			x3.cross(z, x);
			if (lcaoType.indexOf("sp") == 0) { // align z as sp2 orbital
				if (lcaoType.equals("sp2a") || lcaoType.equals("sp2b")) {
					z.set(lcaoType.indexOf("b") >= 0 ? x2 : x);
					z.scale(-1);
				}
				x.cross(z, x3);
				break;
			}
			if (lcaoType.indexOf("lp") == 0) { // align z as lone pair
				hybridization = "lp"; // any is OK
				x3.normalize();
				z.normalize();
				y1.scaleAdd(1.2f, x3, z);
				y2.scaleAdd(-1.2f, x3, z);
				z.set(lcaoType.indexOf("b") >= 0 ? y2 : y1);
				x.cross(z, x3);
				break;
			}
			hybridization = lcaoType;
			// align z as p orbital
			x.cross(z, x3);
			z.set(x3);
			if (z.z < 0) {
				z.set(-z.x, -z.y, -z.z);
				x.set(-x.x, -x.y, -x.z);
			}
			break;
		default:
			// 3 or 4 bonds
			if (x.angle(x2) < _180)
				y1.cross(x, x2);
			else y1.cross(x, x3);
			y1.normalize();
			if (x2.angle(x3) < _180)
				y2.cross(x2, x3);
			else y2.cross(x, x3);
			y2.normalize();
			if (Math.abs(y2.dot(y1)) < 0.95f) {
				hybridization = "sp3";
				if (lcaoType.indexOf("sp") == 0) { // align z as sp3 orbital
					z.set(lcaoType.equalsIgnoreCase("sp3") || lcaoType.indexOf("d") >= 0 ? x4
							: lcaoType.indexOf("c") >= 0 ? x3 : lcaoType.indexOf("b") >= 0 ? x2 : x);
					z.scale(-1);
					x.set(y1);
				}
				else { // needs testing here
					x.cross(z, x);
				}
				break;
			}
			hybridization = "sp2";
			if (lcaoType.indexOf("sp") == 0) { // align z as sp2 orbital
				z.set(lcaoType.equalsIgnoreCase("sp3") || lcaoType.indexOf("d") >= 0 ? x4
						: lcaoType.indexOf("c") >= 0 ? x3 : lcaoType.indexOf("b") >= 0 ? x2 : x);
				z.scale(-1);
				x.set(y1);
				break;
			}
			// align z as p orbital
			z.set(y1);
			if (z.z < 0) {
				z.set(-z.x, -z.y, -z.z);
				x.set(-x.x, -x.y, -x.z);
			}
		}

		x.normalize();
		z.normalize();

		Logger.debug(atom.getIdentity() + " nBonds=" + nBonds + " " + hybridization);
		if (hybridizationCompatible) {
			if (hybridization == "")
				return false;
			if (lcaoType.indexOf("p") == 0) {
				if (hybridization == "sp3")
					return false;
			}
			else {
				if (lcaoType.indexOf(hybridization) < 0)
					return false;
			}
		}
		return true;
	}

	Point3f[] getAdditionalHydrogens(BitSet atomSet) {
		int n = 0;
		Vector3f z = new Vector3f();
		Vector3f x = new Vector3f();
		Point3f pt;
		// just not doing aldehydes here -- all A-X-B bent == sp3 for now
		for (int i = 0; i < atomCount; i++)
			if (atomSet.get(i) && atoms[i].getElementNumber() == 6) {
				Atom atom = atoms[i];
				int nBonds = (atom.getCovalentHydrogenCount() > 0 ? 0 : atom.getCovalentBondCount());
				if (nBonds == 3 || nBonds == 2) { // could be XA3 sp2 or XA2 sp
					if (!viewer.getPrincipalAxes(i, z, x, "sp3", true) || hybridization == "sp")
						nBonds = 0;
				}
				if (nBonds > 0 && nBonds <= 4)
					n += 4 - nBonds;
			}
		Point3f[] hAtoms = new Point3f[n];
		n = 0;
		for (int i = 0; i < atomCount; i++)
			if (atomSet.get(i) && atoms[i].getElementNumber() == 6) {
				Atom atom = atoms[i];
				int nBonds = (atom.getCovalentHydrogenCount() > 0 ? 0 : atom.getCovalentBondCount());
				switch (nBonds) {
				case 1:
					viewer.getPrincipalAxes(i, z, x, "sp3a", false);
					pt = new Point3f(z);
					pt.scaleAdd(1.1f, atom);
					hAtoms[n++] = pt;
					viewer.getPrincipalAxes(i, z, x, "sp3b", false);
					pt = new Point3f(z);
					pt.scaleAdd(1.1f, atom);
					hAtoms[n++] = pt;
					viewer.getPrincipalAxes(i, z, x, "sp3c", false);
					pt = new Point3f(z);
					pt.scaleAdd(1.1f, atom);
					hAtoms[n++] = pt;
					break;
				case 2:
					if (viewer.getPrincipalAxes(i, z, x, "sp3", true) && hybridization != "sp") {
						viewer.getPrincipalAxes(i, z, x, "lpa", false);
						pt = new Point3f(z);
						pt.scaleAdd(1.1f, atom);
						hAtoms[n++] = pt;
						viewer.getPrincipalAxes(i, z, x, "lpb", false);
						pt = new Point3f(z);
						pt.scaleAdd(1.1f, atom);
						hAtoms[n++] = pt;
					}
					break;
				case 3:
					if (viewer.getPrincipalAxes(i, z, x, "sp3", true)) {
						pt = new Point3f(z);
						pt.scaleAdd(1.1f, atom);
						hAtoms[n++] = pt;
					}
				default:
				}
			}
		return hAtoms;
	}

	// leaving this one here, because it is volatile --
	// it will be reset when a new frame is instantiated
	boolean selectionHaloEnabled = false;

	public void setSelectionHaloEnabled(boolean selectionHaloEnabled) {
		this.selectionHaloEnabled = selectionHaloEnabled;
	}

	boolean getSelectionHaloEnabled() {
		return selectionHaloEnabled;
	}

	boolean echoShapeActive = false;

	boolean getEchoStateActive() {
		return echoShapeActive;
	}

	public void setEchoStateActive(boolean TF) {
		echoShapeActive = TF;
	}

	String getState() {
		StringBuffer commands = new StringBuffer("# connections;\n");
		String cmd;
		if (reportFormalCharges) {
			Hashtable ht = new Hashtable();
			for (int i = 0; i < atomCount; i++)
				StateManager.setStateInfo(ht, i, i, "set formalCharge " + atoms[i].getFormalCharge());
			commands.append(StateManager.getCommands(ht));
		}

		// connections

		Vector fs = stateScripts;
		int len = fs.size();
		for (int i = 0; i < len; i++)
			commands.append(fs.get(i) + "\n");

		viewer.loadShape(JmolConstants.SHAPE_LABELS);
		((Labels) shapes[JmolConstants.SHAPE_LABELS]).getShapeState(commands);

		commands.append("\n# model state;\n");
		// shape construction

		for (int i = 0; i < JmolConstants.SHAPE_MAX; ++i) {
			Shape shape = shapes[i];
			if (shape != null && (cmd = shape.getShapeState()) != null && cmd.length() > 1)
				commands.append(cmd);
		}
		commands.append("\n");
		return commands.toString();
	}

}
