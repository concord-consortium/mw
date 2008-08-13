/* $RCSfile: ModelManager.java,v $
 * $Author: qxie $
 * $Date: 2007-11-12 17:16:45 $
 * $Revision: 1.21 $

 *
 * Copyright (C) 2003-2005  Miguel, Jmol Development, www.jmol.org
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

import org.myjmol.api.JmolAdapter;
import org.myjmol.symmetry.SpaceGroup;
import org.myjmol.symmetry.UnitCell;
import org.myjmol.util.Logger;
import org.myjmol.viewer.Frame.CellInfo;

import java.util.BitSet;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Vector;

import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;
import java.awt.Rectangle;

class ModelManager {

	final Viewer viewer;
	Frame frame;

	String fullPathName;
	String fileName;
	String modelSetName;
	boolean haveFile;

	// XIE
	BitSet findAtomsInOval(Rectangle rectangle) {
		if (frame == null)
			return null;
		return frame.findAtomsInOval(rectangle);
	}

	ModelManager(Viewer viewer) {
		this.viewer = viewer;
	}

	void clear() {
		if (frame != null) {
			frame.clear();
		}
		fullPathName = fileName = modelSetName = null;
		frame = null;
		haveFile = false;
	}

	void zap() {
		fullPathName = fileName = modelSetName = "zapped";
		frame = new Frame(viewer, "empty");
		haveFile = false;
	}

	void setClientFile(String fullPathName, String fileName, JmolAdapter adapter, Object clientFile) {
		if (clientFile == null) {
			clear();
			return;
		}
		this.fullPathName = fullPathName;
		this.fileName = fileName;
		modelSetName = adapter.getAtomSetCollectionName(clientFile);
		if (modelSetName != null) {
			modelSetName = modelSetName.trim();
			if (modelSetName.length() == 0)
				modelSetName = null;
		}
		if (modelSetName == null)
			modelSetName = reduceFilename(fileName);
		frame = new Frame(viewer, adapter, clientFile);
		haveFile = true;
		if (frame.atomCount == 0)
			zap(); // XIE ???
	}

	String reduceFilename(String fileName) {
		if (fileName == null)
			return null;
		int ichDot = fileName.indexOf('.');
		if (ichDot > 0)
			fileName = fileName.substring(0, ichDot);
		if (fileName.length() > 24)
			fileName = fileName.substring(0, 20) + " ...";
		return fileName;
	}

	Frame getFrame() {
		return frame;
	}

	JmolAdapter getExportJmolAdapter() {
		return frame.getExportJmolAdapter();
	}

	String getModelSetName() {
		return modelSetName;
	}

	String getModelSetFileName() {
		return fileName;
	}

	String getModelSetPathName() {
		return fullPathName;
	}

	Properties getModelSetProperties() {
		return frame.getModelSetProperties();
	}

	String getModelSetProperty(String propertyName) {
		return frame.getModelSetProperty(propertyName);
	}

	Hashtable getModelSetAuxiliaryInfo() {
		return frame.getModelSetAuxiliaryInfo();
	}

	Object getModelSetAuxiliaryInfo(String keyName) {
		return frame.getModelSetAuxiliaryInfo(keyName);
	}

	boolean modelSetHasVibrationVectors() {
		return frame.modelSetHasVibrationVectors();
	}

	boolean modelHasVibrationVectors(int modelIndex) {
		return frame.modelHasVibrationVectors(modelIndex);
	}

	String getModelSetTypeName() {
		if (frame == null)
			return null; // XIE
		return frame.getModelSetTypeName();
	}

	boolean isPDB() {
		if (frame == null)
			return false; // XIE
		return frame.isPDB;
	}

	boolean isPDB(int modelIndex) {
		if (frame == null)
			return false; // XIE
		return frame.mmset.getModel(modelIndex).isPDB;
	}

	int getModelCount() {
		if (frame == null)
			return 0; // XIE
		return frame.getModelCount();
	}

	String getModelInfoAsString() {
		int modelCount = getModelCount();
		String str = "model count = " + modelCount + "\nmodelSetHasVibrationVectors:" + modelSetHasVibrationVectors();
		Properties props = getModelSetProperties();
		str = str.concat(listProperties(props));
		for (int i = 0; i < modelCount; ++i) {
			str = str.concat("\n" + i + ":" + getModelNumber(i) + ":" + getModelName(i) + "\nmodelHasVibrationVectors:"
					+ modelHasVibrationVectors(i));
			// str = str.concat(listProperties(getModelProperties(i)));
		}
		return str;
	}

	String getSymmetryInfoAsString() {
		int modelCount = getModelCount();
		String str = "Symmetry Information:";
		for (int i = 0; i < modelCount; ++i) {
			str += "\nmodel #" + getModelNumber(i) + "; name=" + getModelName(i) + "\n"
					+ frame.getSymmetryInfoAsString(i);
		}
		return str;
	}

	String getModelName(int modelIndex) {
		// necessary for status manager frame change?
		return frame == null ? null : frame.getModelName(modelIndex);
	}

	int getModelNumber(int modelIndex) {
		if (frame == null)
			return 0; // XIE
		return frame.getModelNumber(modelIndex);
	}

	Properties getModelProperties(int modelIndex) {
		if (frame == null)
			return null; // XIE
		return frame.getModelProperties(modelIndex);
	}

	String getModelProperty(int modelIndex, String propertyName) {
		if (frame == null)
			return null; // XIE
		return frame.getModelProperty(modelIndex, propertyName);
	}

	Hashtable getModelAuxiliaryInfo(int modelIndex) {
		if (frame == null)
			return null; // XIE
		return frame.getModelAuxiliaryInfo(modelIndex);
	}

	Object getModelAuxiliaryInfo(int modelIndex, String keyName) {
		if (frame == null)
			return null; // XIE
		return frame.getModelAuxiliaryInfo(modelIndex, keyName);
	}

	int getModelNumberIndex(int modelNumber) {
		if (frame == null)
			return 0; // XIE
		return frame.getModelNumberIndex(modelNumber);
	}

	Point3f getSpinCenter(String axisID, int modelIndex) {
		Draw draw = (Draw) frame.shapes[JmolConstants.SHAPE_DRAW];
		if (draw == null)
			return null;
		int meshIndex = draw.getIndexFromName(axisID);
		if (meshIndex < 0) {
			return null;
		}
		return draw.getSpinCenter(meshIndex, modelIndex);
	}

	Vector3f getSpinAxis(String axisID, int modelIndex) {
		Draw draw = (Draw) frame.shapes[JmolConstants.SHAPE_DRAW];
		if (draw == null)
			return null;
		int meshIndex = draw.getIndexFromName(axisID);
		if (meshIndex < 0) {
			return null;
		}
		return draw.getSpinAxis(meshIndex, modelIndex);
	}

	float calcRotationRadius(Point3f center) {
		if (frame == null)
			return 10; // XIE
		return frame.calcRotationRadius(center);
	}

	Point3f getBoundBoxCenter() {
		if (frame == null)
			return new Point3f(); // XIE
		return frame.getBoundBoxCenter();
	}

	Point3f getAverageAtomPoint() {
		if (frame == null)
			return new Point3f(); // XIE
		return frame.getAverageAtomPoint();
	}

	Vector3f getBoundBoxCornerVector() {
		if (frame == null)
			return new Vector3f();
		return frame.getBoundBoxCornerVector();
	}

	Point3f getAtomSetCenter(BitSet bs) {
		if (frame == null)
			return new Point3f();
		return frame.getAtomSetCenter(bs);
	}

	int firstAtomOf(BitSet bs) {
		if (frame == null)
			return 0;
		return frame.firstAtomOf(bs);
	}

	BitSet getAtomBits(String setType) {
		if (frame == null)
			return null;
		return frame.getAtomBits(setType);
	}

	BitSet getAtomBits(String setType, String specInfo) {
		if (frame == null)
			return null;
		return frame.getAtomBits(setType, specInfo);
	}

	BitSet getAtomBits(String setType, int specInfo) {
		if (frame == null)
			return null;
		return frame.getAtomBits(setType, specInfo);
	}

	BitSet getAtomBits(String setType, int[] specInfo) {
		if (frame == null)
			return null;
		return frame.getAtomBits(setType, specInfo);
	}

	int getAtomCount() {
		if (frame == null)
			return 0; // XIE
		return frame.getAtomCount();
	}

	int getAtomCountInModel(int modelIndex) {
		return modelIndex < 0 ? getAtomCount() : frame.getAtomCountInModel(modelIndex);
	}

	int getBondCount() {
		if (frame == null)
			return 0; // XIE
		return frame.getBondCount();
	}

	int getBondCountInModel(int modelIndex) {
		if (frame == null)
			return 0; // XIE
		return frame.getBondCountInModel(modelIndex);
	}

	int getGroupCount() {
		if (frame == null)
			return 0; // XIE
		return frame.getGroupCount();
	}

	int getGroupCountInModel(int modelIndex) {
		if (frame == null)
			return 0; // XIE
		return modelIndex < 0 ? getGroupCount() : frame.getGroupCountInModel(modelIndex);
	}

	int getChainCount() {
		if (frame == null)
			return 0; // XIE
		return frame.getChainCount();
	}

	int getChainCountInModel(int modelIndex) {
		if (frame == null)
			return 0; // XIE
		return modelIndex < 0 ? getChainCount() : frame.getChainCountInModel(modelIndex);
	}

	int getPolymerCount() {
		if (frame == null)
			return 0; // XIE
		return frame.getPolymerCount();
	}

	int getPolymerCountInModel(int modelIndex) {
		return modelIndex < 0 ? getPolymerCount() : frame.getPolymerCountInModel(modelIndex);
	}

	int getMoleuleCount() {
		if (frame == null)
			return 0; // XIE
		return frame.getMoleculeCount();
	}

	int makeConnections(float minDistance, float maxDistance, short order, int connectOperation, BitSet bsA, BitSet bsB) {
		return frame.makeConnections(minDistance, maxDistance, order, connectOperation, bsA, bsB);
	}

	void rebond() {
		frame.rebond();
	}

	boolean frankClicked(int x, int y) {
		return getShapeSize(JmolConstants.SHAPE_FRANK) != 0 && frame.frankClicked(x, y);
	}

	int findNearestAtomIndex(int x, int y) {
		if (frame == null)
			return -1;
		return frame.findNearestAtomIndex(x, y);
	}

	BitSet findAtomsInRectangle(Rectangle rectRubber) {
		if (frame == null)
			return null;
		return frame.findAtomsInRectangle(rectRubber);
	}

	/*******************************************************************************************************************
	 * shape support
	 ******************************************************************************************************************/

	int[] shapeSizes = new int[JmolConstants.SHAPE_MAX];

	// Hashtable[] shapeProperties = new Hashtable[JmolConstants.SHAPE_MAX];

	void loadShape(int shapeID) {
		if (frame != null)
			frame.loadShape(shapeID);
	}

	void setShapeSize(int shapeType, int size, BitSet bsSelected) {
		shapeSizes[shapeType] = size;
		if (frame != null)
			frame.setShapeSize(shapeType, size, bsSelected);
	}

	int getShapeSize(int shapeType) {
		return shapeSizes[shapeType];
	}

	void setShapeProperty(int shapeType, String propertyName, Object value, BitSet bsSelected) {
		// this one is necessary, because we do try to load some shapes
		// while the frame itself is loading.
		if (frame != null)
			frame.setShapeProperty(shapeType, propertyName.intern(), value, bsSelected);
	}

	Object getShapeProperty(int shapeType, String propertyName, int index) {
		if (frame == null)
			return null;
		return frame.getShapeProperty(shapeType, propertyName, index);
	}

	int getShapeIdFromObjectName(String objectName) {
		int i;
		for (i = JmolConstants.SHAPE_MIN_MESH_COLLECTION; i < JmolConstants.SHAPE_MAX; ++i) {
			MeshCollection shape = (MeshCollection) frame.shapes[i];
			if (shape != null && shape.getIndexFromName(objectName) >= 0)
				return i;
		}
		i = JmolConstants.SHAPE_DIPOLES;
		Dipoles dipoles = (Dipoles) frame.shapes[i];
		if (dipoles != null && dipoles.getIndexFromName(objectName) >= 0)
			return i;
		return -1;
	}

	int getAtomIndexFromAtomNumber(int atomNumber) {
		if (frame == null)
			return 0; // XIE
		return frame.getAtomIndexFromAtomNumber(atomNumber);
	}

	BitSet getElementsPresentBitSet() {
		if (frame == null)
			return null; // XIE
		return frame.getElementsPresentBitSet();
	}

	public Hashtable getHeteroList(int modelIndex) {
		if (frame == null)
			return null; // XIE
		return frame.mmset.getHeteroList(modelIndex);
	}

	BitSet getVisibleSet() {
		if (frame == null)
			return null; // XIE
		return frame.getVisibleSet();
	}

	BitSet getClickableSet() {
		if (frame == null)
			return null; // XIE
		return frame.getClickableSet();
	}

	BitSet getModelAtomBitSet(int modelIndex) {
		if (frame == null)
			return null; // XIE
		return frame.getModelAtomBitSet(modelIndex);
	}

	BitSet getModelBitSet(BitSet atomList) {
		if (frame == null)
			return null; // XIE
		return frame.getModelBitSet(atomList);
	}

	BitSet getMoleculeBitSet(int modelIndex) {
		if (frame == null)
			return null; // XIE
		return frame.getMoleculeBitSet(modelIndex);
	}

	void calcSelectedGroupsCount(BitSet bsSelected) {
		if (frame != null)
			frame.calcSelectedGroupsCount(bsSelected);
	}

	void calcSelectedMonomersCount(BitSet bsSelected) {
		if (frame != null)
			frame.calcSelectedMonomersCount(bsSelected);
	}

	void calcSelectedMoleculesCount(BitSet bsSelected) {
		if (frame != null)
			frame.calcSelectedGroupsCount(bsSelected);
	}

	// //////////////////////////////////////////////////////////////
	// Access to atom properties for clients
	// //////////////////////////////////////////////////////////////

	String getAtomInfo(int i) {
		if (frame == null)
			return "";
		return frame.getAtomAt(i).getInfo();
	}

	String getAtomInfoXYZ(int i) {
		if (frame == null)
			return "";
		return frame.getAtomAt(i).getInfoXYZ();
	}

	String getElementSymbol(int i) {
		if (frame == null)
			return "";
		return frame.getAtomAt(i).getElementSymbol();
	}

	int getElementNumber(int i) {
		if (frame == null)
			return -1;
		return frame.getAtomAt(i).getElementNumber();
	}

	String getElementName(int i) {
		if (frame == null)
			return "";
		return JmolConstants.elementNameFromNumber(frame.getAtomAt(i).getAtomicAndIsotopeNumber());
	}

	String getAtomName(int i) {
		if (frame == null)
			return "";
		return frame.getAtomAt(i).getAtomName();
	}

	boolean getAtomVisibility(int i) {
		if (frame == null)
			return false;
		return frame.getAtomAt(i).isVisible();
	}

	int getAtomNumber(int i) {
		if (frame == null)
			return 0;
		return frame.getAtomAt(i).getAtomNumber();
	}

	float getAtomX(int i) {
		if (frame == null)
			return 0;
		return frame.getAtomAt(i).x;
	}

	float getAtomY(int i) {
		if (frame == null)
			return 0;
		return frame.getAtomAt(i).y;
	}

	float getAtomZ(int i) {
		if (frame == null)
			return 0;
		return frame.getAtomAt(i).z;
	}

	Point3f getAtomPoint3f(int i) {
		if (frame == null)
			return new Point3f();
		return frame.getAtomAt(i);
	}

	float getAtomRadius(int i) {
		if (frame == null)
			return 0;
		return frame.getAtomAt(i).getRadius();
	}

	// XIE
	float getVdwRadius(int i) {
		if (frame == null)
			return 0;
		return frame.getAtomAt(i).getVanderwaalsRadiusFloat();
	}

	short getAtomColix(int i) {
		if (frame == null)
			return 0;
		return frame.getAtomAt(i).getColix();
	}

	String getAtomChain(int i) {
		return "" + frame.getAtomAt(i).getChainID();
	}

	String getAtomSequenceCode(int i) {
		if (frame == null)
			return "";
		return frame.getAtomAt(i).getSeqcodeString();
	}

	int getAtomModelIndex(int i) {
		if (frame == null)
			return 0;
		return frame.getAtomAt(i).getModelIndex();
	}

	Point3f getBondPoint3f1(int i) {
		if (frame == null)
			return new Point3f();
		return frame.getBondAt(i).getAtom1();
	}

	Point3f getBondPoint3f2(int i) {
		if (frame == null)
			return new Point3f();
		return frame.getBondAt(i).getAtom2();
	}

	float getBondRadius(int i) {
		if (frame == null)
			return 0;
		return frame.getBondAt(i).getRadius();
	}

	short getBondOrder(int i) {
		if (frame == null)
			return 0;
		return frame.getBondAt(i).getOrder();
	}

	float getBondLength(int i) {
		if (frame == null)
			return 0;
		return getBondAtom1(i).distance(getBondAtom2(i));
	}

	Atom getBondAtom1(int i) {
		if (frame == null)
			return null;
		return frame.getBondAt(i).getAtom1();
	}

	Atom getBondAtom2(int i) {
		if (frame == null)
			return null;
		return frame.getBondAt(i).getAtom2();
	}

	short getBondColix1(int i) {
		if (frame == null)
			return 0;
		return frame.getBondAt(i).getColix1();
	}

	short getBondColix2(int i) {
		if (frame == null)
			return 0;
		return frame.getBondAt(i).getColix2();
	}

	int getBondModelIndex(int i) {
		if (frame == null)
			return 0;
		Atom atom = frame.getBondAt(i).getAtom1();
		if (atom != null) {
			return atom.getModelIndex();
		}
		atom = frame.getBondAt(i).getAtom2();
		if (atom != null) {
			return atom.getModelIndex();
		}
		return 0;
	}

	public Point3f[] getPolymerLeadMidPoints(int modelIndex, int polymerIndex) {
		if (frame == null)
			return null;
		Polymer polymer = frame.getPolymerAt(modelIndex, polymerIndex);
		return polymer.getLeadMidpoints();
	}

	BitSet getAtomsWithin(String withinWhat, String specInfo, BitSet bs) {
		if (withinWhat.equals("sequence"))
			return withinSequence(specInfo, bs);
		return null;
	}

	BitSet getAtomsWithin(String withinWhat, BitSet bs) {
		if (withinWhat.equals("group"))
			return withinGroup(bs);
		if (withinWhat.equals("chain"))
			return withinChain(bs);
		if (withinWhat.equals("molecule"))
			return withinMolecule(bs);
		if (withinWhat.equals("model"))
			return withinModel(bs);
		if (withinWhat.equals("element"))
			return withinElement(bs);
		if (withinWhat.equals("site"))
			return withinSite(bs);
		return null;
	}

	BitSet withinGroup(BitSet bs) {
		// Logger.debug("withinGroup");
		Group groupLast = null;
		BitSet bsResult = new BitSet();
		for (int i = getAtomCount(); --i >= 0;) {
			if (!bs.get(i))
				continue;
			Atom atom = frame.getAtomAt(i);
			Group group = atom.getGroup();
			if (group != groupLast) {
				group.selectAtoms(bsResult);
				groupLast = group;
			}
		}
		return bsResult;
	}

	BitSet withinChain(BitSet bs) {
		Chain chainLast = null;
		BitSet bsResult = new BitSet();
		for (int i = getAtomCount(); --i >= 0;) {
			if (!bs.get(i))
				continue;
			Atom atom = frame.getAtomAt(i);
			Chain chain = atom.getChain();
			if (chain != chainLast) {
				chain.selectAtoms(bsResult);
				chainLast = chain;
			}
		}
		return bsResult;
	}

	BitSet withinMolecule(BitSet bs) {
		if (frame == null)
			return null;
		return frame.getMoleculeBitSet(bs);
	}

	BitSet withinModel(BitSet bs) {
		if (frame == null)
			return null;
		BitSet bsResult = new BitSet();
		BitSet bsThis = new BitSet();
		for (int i = getAtomCount(); --i >= 0;)
			if (bs.get(i))
				bsThis.set(frame.getAtomAt(i).modelIndex);
		for (int i = getAtomCount(); --i >= 0;)
			if (bsThis.get(frame.getAtomAt(i).modelIndex))
				bsResult.set(i);
		return bsResult;
	}

	BitSet withinSite(BitSet bs) {
		if (frame == null)
			return null;
		// Logger.debug("withinGroup");
		BitSet bsResult = new BitSet();
		BitSet bsThis = new BitSet();
		for (int i = getAtomCount(); --i >= 0;)
			if (bs.get(i))
				bsThis.set(frame.getAtomAt(i).atomSite);
		for (int i = getAtomCount(); --i >= 0;)
			if (bsThis.get(frame.getAtomAt(i).atomSite))
				bsResult.set(i);
		return bsResult;
	}

	BitSet withinElement(BitSet bs) {
		// Logger.debug("withinGroup");
		BitSet bsResult = new BitSet();
		BitSet bsThis = new BitSet();
		for (int i = getAtomCount(); --i >= 0;)
			if (bs.get(i))
				bsThis.set(getElementNumber(i));
		for (int i = getAtomCount(); --i >= 0;)
			if (bsThis.get(getElementNumber(i)))
				bsResult.set(i);
		return bsResult;
	}

	BitSet withinSequence(String specInfo, BitSet bs) {
		// Logger.debug("withinSequence");
		String sequence = "";
		int lenInfo = specInfo.length();
		BitSet bsResult = new BitSet();
		if (lenInfo == 0)
			return bsResult;
		int modelCount = viewer.getModelCount();
		for (int i = 0; i < modelCount; ++i) {
			int polymerCount = getPolymerCountInModel(i);
			for (int ip = 0; ip < polymerCount; ip++) {
				sequence = getPolymerSequence(i, ip);
				int j = -1;
				while ((j = sequence.indexOf(specInfo, ++j)) >= 0)
					getPolymerSequenceAtoms(i, ip, j, lenInfo, bs, bsResult);
			}
		}
		return bsResult;
	}

	void selectModelIndexAtoms(int modelIndex, BitSet bsResult) {
		Frame frame = viewer.getFrame();
		for (int i = viewer.getAtomCount(); --i >= 0;)
			if (frame.getAtomAt(i).getModelIndex() == modelIndex)
				bsResult.set(i);
	}

	BitSet getAtomsWithin(float distance, BitSet bs) {
		if (frame == null)
			return null;
		BitSet bsResult = new BitSet();
		for (int i = frame.getAtomCount(); --i >= 0;) {
			if (bs.get(i)) {
				Atom atom = frame.getAtomAt(i);
				AtomIterator iterWithin = frame.getWithinModelIterator(atom, distance);
				while (iterWithin.hasNext())
					bsResult.set(iterWithin.next().getAtomIndex());
			}
		}
		return bsResult;
	}

	BitSet getAtomsWithin(float distance, Point3f coord) {
		if (frame == null)
			return null;
		BitSet bsResult = new BitSet();
		for (int i = frame.getAtomCount(); --i >= 0;) {
			Atom atom = frame.getAtomAt(i);
			if (atom.distance(coord) <= distance)
				bsResult.set(atom.atomIndex);
		}
		return bsResult;
	}

	BitSet getAtomsConnected(float min, float max, BitSet bs) {
		if (frame == null)
			return null;
		BitSet bsResult = new BitSet();
		int atomCount = getAtomCount();
		int[] nBonded = new int[atomCount];
		int bondCount = getBondCount();
		for (int ibond = 0; ibond < bondCount; ibond++) {
			Bond bond = frame.bonds[ibond];
			if (bond.order > 0) {
				if (bs.get(bond.atom1.atomIndex))
					nBonded[bond.atom2.atomIndex]++;
				if (bs.get(bond.atom2.atomIndex))
					nBonded[bond.atom1.atomIndex]++;
			}
		}
		for (int i = atomCount; --i >= 0;)
			if (nBonded[i] >= min && nBonded[i] <= max)
				bsResult.set(i);
		return bsResult;
	}

	String getModelExtract(BitSet bs) {
		int atomCount = getAtomCount();
		int bondCount = getBondCount();
		int nAtoms = 0;
		int nBonds = 0;
		int[] atomMap = new int[atomCount];
		StringBuffer mol = new StringBuffer();
		StringBuffer s = new StringBuffer();

		for (int i = 0; i < atomCount; i++) {
			if (bs.get(i)) {
				atomMap[i] = ++nAtoms;
				getAtomRecordMOL(s, i);
			}
		}
		for (int i = 0; i < bondCount; i++) {
			if (bs.get(frame.getBondAt(i).getAtom1().atomIndex) && bs.get(frame.getBondAt(i).getAtom2().atomIndex)) {
				int order = getBondOrder(i);
				if (order >= 1 && order < 3) {
					getBondRecordMOL(s, i, atomMap);
					nBonds++;
				}
			}
		}
		if (nAtoms > 999 || nBonds > 999) {
			Logger.error("ModelManager.java::getModel: ERROR atom/bond overflow");
			return "";
		}
		// 21 21 0 0 0
		rFill(mol, "   ", "" + nAtoms);
		rFill(mol, "   ", "" + nBonds);
		mol.append("  0  0  0\n");
		mol.append(s);
		return mol.toString();
	}

	void getAtomRecordMOL(StringBuffer s, int i) {
		// -0.9920 3.2030 9.1570 Cl 0 0 0 0 0
		// 3.4920 4.0920 5.8700 Cl 0 0 0 0 0
		// 012345678901234567890123456789012
		rFill(s, "          ", safeTruncate(getAtomX(i), 9));
		rFill(s, "          ", safeTruncate(getAtomY(i), 9));
		rFill(s, "          ", safeTruncate(getAtomZ(i), 9));
		s.append(" " + (getElementSymbol(i) + "  ").substring(0, 2) + "\n");
	}

	void getBondRecordMOL(StringBuffer s, int i, int[] atomMap) {
		// 1 2 1
		Bond b = frame.getBondAt(i);
		rFill(s, "   ", "" + atomMap[b.getAtom1().atomIndex]);
		rFill(s, "   ", "" + atomMap[b.getAtom2().atomIndex]);
		s.append("  " + getBondOrder(i) + "\n");
	}

	private void rFill(StringBuffer s, String s1, String s2) {
		s.append(s1.substring(0, s1.length() - s2.length()));
		s.append(s2);
	}

	private String safeTruncate(float f, int n) {
		if (f > -0.001 && f < 0.001)
			f = 0;
		return (f + "         ").substring(0, n);
	}

	/*
	 * final static String[] pdbRecords = { "ATOM ", "HELIX ", "SHEET ", "TURN ", "MODEL ", "SCALE", "HETATM", "SEQRES",
	 * "DBREF ", };
	 */

	final static String[] pdbRecords = { "ATOM  ", "MODEL ", "HETATM" };

	String getFileHeader() {
		if (frame.isPDB)
			return getFullPDBHeader();
		String info = getModelSetProperty("fileHeader");
		if (info == null)
			info = getModelSetName();
		if (info != null)
			return info;
		return "no header information found";
	}

	String getPDBHeader() {
		return (frame.isPDB ? getFullPDBHeader() : "!Not a pdb file!\n" + getFileHeader());
	}

	String getFullPDBHeader() {
		String info = viewer.getCurrentFileAsString();
		int ichMin = info.length();
		for (int i = pdbRecords.length; --i >= 0;) {
			int ichFound;
			String strRecord = pdbRecords[i];
			switch (ichFound = (info.startsWith(strRecord) ? 0 : info.indexOf("\n" + strRecord))) {
			case -1:
				continue;
			case 0:
				return "";
			default:
				if (ichFound < ichMin)
					ichMin = ++ichFound;
			}
		}
		return info.substring(0, ichMin);
	}

	@SuppressWarnings("unchecked")
	Hashtable getModelInfo() {
		Hashtable info = new Hashtable();
		int modelCount = viewer.getModelCount();
		info.put("modelSetName", getModelSetName());
		info.put("modelCount", new Integer(modelCount));
		info.put("modelSetHasVibrationVectors", new Boolean(modelSetHasVibrationVectors()));
		Properties props = viewer.getModelSetProperties();
		if (props != null)
			info.put("modelSetProperties", props);
		Vector models = new Vector();
		for (int i = 0; i < modelCount; ++i) {
			Hashtable model = new Hashtable();
			model.put("_ipt", new Integer(i));
			model.put("num", new Integer(viewer.getModelNumber(i)));
			model.put("name", viewer.getModelName(i));
			model.put("vibrationVectors", new Boolean(modelHasVibrationVectors(i)));
			model.put("atomCount", new Integer(getAtomCountInModel(i)));
			model.put("bondCount", new Integer(getBondCountInModel(i)));
			model.put("groupCount", new Integer(getGroupCountInModel(i)));
			model.put("polymerCount", new Integer(getPolymerCountInModel(i)));
			model.put("chainCount", new Integer(getChainCountInModel(i)));
			props = viewer.getModelProperties(i);
			if (props != null)
				model.put("modelProperties", props);
			models.add(model);
		}
		info.put("models", models);
		return info;
	}

	@SuppressWarnings("unchecked")
	Hashtable getAuxiliaryInfo() {
		Hashtable info = new Hashtable();
		info = getModelSetAuxiliaryInfo();
		if (info == null)
			return info;
		Vector models = new Vector();
		int modelCount = viewer.getModelCount();
		for (int i = 0; i < modelCount; ++i) {
			Hashtable modelinfo = getModelAuxiliaryInfo(i);
			models.add(modelinfo);
		}
		info.put("models", models);
		return info;
	}

	@SuppressWarnings("unchecked")
	Vector getAllAtomInfo(BitSet bs) {
		Vector V = new Vector();
		int atomCount = viewer.getAtomCount();
		for (int i = 0; i < atomCount; i++)
			if (bs.get(i))
				V.add(getAtomInfoLong(i));
		return V;
	}

	Vector getMoleculeInfo(BitSet bsAtoms) {
		return frame.getMoleculeInfo(bsAtoms);
	}

	@SuppressWarnings("unchecked")
	void getAtomIdentityInfo(int i, Hashtable info) {
		info.put("_ipt", new Integer(i));
		info.put("atomno", new Integer(getAtomNumber(i)));
		info.put("info", getAtomInfo(i));
		info.put("sym", getElementSymbol(i));
	}

	@SuppressWarnings("unchecked")
	Hashtable getAtomInfoLong(int i) {
		Atom atom = frame.getAtomAt(i);
		Hashtable info = new Hashtable();
		getAtomIdentityInfo(i, info);
		info.put("element", getElementName(i));
		info.put("elemno", new Integer(getElementNumber(i)));
		info.put("x", new Float(getAtomX(i)));
		info.put("y", new Float(getAtomY(i)));
		info.put("z", new Float(getAtomZ(i)));
		if (frame.vibrationVectors != null && frame.vibrationVectors[i] != null) {
			info.put("vibVector", new Vector3f(frame.vibrationVectors[i]));
		}
		info.put("bondCount", new Integer(atom.getCovalentBondCount()));
		info.put("radius", new Float((atom.getRasMolRadius() / 120.0)));
		info.put("model", new Integer(atom.getModelTagNumber()));
		info.put("visible", new Boolean(getAtomVisibility(i)));
		info.put("clickabilityFlags", new Integer(atom.clickabilityFlags));
		info.put("visibilityFlags", new Integer(atom.shapeVisibilityFlags));
		info.put("spacefill", new Integer(atom.madAtom >> 3));
		String strColor = viewer.getHexColorFromIndex(atom.colixAtom);
		if (strColor != null)
			info.put("color", strColor);
		info.put("colix", new Integer(atom.colixAtom));
		boolean isTranslucent = atom.isTranslucent();
		if (isTranslucent)
			info.put("translucent", new Boolean(isTranslucent));
		info.put("formalCharge", new Integer(atom.getFormalCharge()));
		info.put("partialCharge", new Float(atom.getPartialCharge()));
		float d = atom.getSurfaceDistance();
		if (d >= 0)
			info.put("surfaceDistance", new Float(d));
		if (isPDB(atom.modelIndex)) {
			info.put("resname", atom.getGroup3());
			int seqNum = atom.getSeqNumber();
			char insCode = atom.getInsertionCode();
			if (seqNum > 0)
				info.put("resno", new Integer(seqNum));
			if (insCode != 0)
				info.put("insertionCode", "" + insCode);
			char chainID = atom.getChainID();
			info.put("name", getAtomName(i));
			info.put("chain", (chainID == '\0' ? "" : "" + chainID));
			info.put("atomID", new Integer(atom.getSpecialAtomID()));
			info.put("groupID", new Integer(atom.getGroupID()));
			if (atom.alternateLocationID != '\0')
				info.put("altLocation", new String("" + atom.alternateLocationID));
			info.put("structure", new Integer(atom.getProteinStructureType()));
			info.put("polymerLength", new Integer(atom.getPolymerLength()));
			info.put("occupancy", new Integer(atom.getOccupancy()));
			int temp = atom.getBfactor100();
			info.put("temp", new Integer((temp < 0 ? 0 : temp / 100)));
		}
		return info;
	}

	@SuppressWarnings("unchecked")
	Vector getAllBondInfo(BitSet bs) {
		Vector V = new Vector();
		int bondCount = getBondCount();
		for (int i = 0; i < bondCount; i++)
			if (bs.get(frame.getBondAt(i).getAtom1().atomIndex) && bs.get(frame.getBondAt(i).getAtom2().atomIndex))
				V.add(getBondInfo(i));
		return V;
	}

	@SuppressWarnings("unchecked")
	Hashtable getBondInfo(int i) {
		Bond bond = frame.getBondAt(i);
		Atom atom1 = getBondAtom1(i);
		Atom atom2 = getBondAtom2(i);
		Hashtable info = new Hashtable();
		info.put("_bpt", new Integer(i));
		Hashtable infoA = new Hashtable();
		getAtomIdentityInfo(atom1.atomIndex, infoA);
		Hashtable infoB = new Hashtable();
		getAtomIdentityInfo(atom2.atomIndex, infoB);
		info.put("atom1", infoA);
		info.put("atom2", infoB);
		info.put("order", new Integer(getBondOrder(i)));
		info.put("radius", new Float(bond.mad / 2000.));
		info.put("length_Ang", new Float(getBondLength(i)));
		info.put("visible", new Boolean(bond.shapeVisibilityFlags != 0));
		String strColor = viewer.getHexColorFromIndex(bond.colix);
		if (strColor != null)
			info.put("color", strColor);
		info.put("colix", new Integer(bond.colix));
		boolean isTranslucent = bond.isTranslucent();
		if (isTranslucent)
			info.put("translucent", new Boolean(isTranslucent));
		return info;
	}

	String listProperties(Properties props) {
		String str = "";
		if (props == null) {
			str = str.concat("\nProperties: null");
		}
		else {
			Enumeration e = props.propertyNames();
			str = str.concat("\nProperties:");
			while (e.hasMoreElements()) {
				String propertyName = (String) e.nextElement();
				str = str.concat("\n " + propertyName + "=" + props.getProperty(propertyName));
			}
		}
		return str;
	}

	@SuppressWarnings("unchecked")
	Hashtable listPropertiesAsObject(Properties props) {
		Hashtable info = new Hashtable();
		if (props == null)
			return info;
		Enumeration e = props.propertyNames();
		while (e.hasMoreElements()) {
			String propertyName = (String) e.nextElement();
			info.put(propertyName, props.getProperty(propertyName));
		}
		return info;
	}

	@SuppressWarnings("unchecked")
	Hashtable getAllChainInfo(BitSet bs) {
		Hashtable finalInfo = new Hashtable();
		Vector modelVector = new Vector();
		int modelCount = getModelCount();
		for (int i = 0; i < modelCount; ++i) {
			Hashtable modelInfo = new Hashtable();
			Vector info = getChainInfo(i, bs);
			if (info.size() > 0) {
				modelInfo.put("modelIndex", new Integer(i));
				modelInfo.put("chains", info);
				modelVector.add(modelInfo);
			}
		}
		finalInfo.put("models", modelVector);
		return finalInfo;
	}

	@SuppressWarnings("unchecked")
	Vector getChainInfo(int modelIndex, BitSet bs) {
		Model model = frame.mmset.getModel(modelIndex);
		int nChains = model.getChainCount();
		Vector infoChains = new Vector();
		for (int i = 0; i < nChains; i++) {
			Chain chain = model.getChain(i);
			Vector infoChain = new Vector();
			int nGroups = chain.getGroupCount();
			Hashtable arrayName = new Hashtable();
			for (int igroup = 0; igroup < nGroups; igroup++) {
				Group group = chain.getGroup(igroup);
				if (!bs.get(group.firstAtomIndex))
					continue;
				Hashtable infoGroup = new Hashtable();
				infoGroup.put("groupIndex", new Integer(igroup));
				infoGroup.put("groupID", new Short(group.getGroupID()));
				infoGroup.put("seqCode", group.getSeqcodeString());
				infoGroup.put("_apt1", new Integer(group.firstAtomIndex));
				infoGroup.put("_apt2", new Integer(group.lastAtomIndex));
				infoGroup.put("atomInfo1", getAtomInfo(group.firstAtomIndex));
				infoGroup.put("atomInfo2", getAtomInfo(group.lastAtomIndex));
				infoGroup.put("visibilityFlags", new Integer(group.shapeVisibilityFlags));
				infoChain.add(infoGroup);
			}
			if (!infoChain.isEmpty()) {
				arrayName.put("residues", infoChain);
				infoChains.add(arrayName);
			}
		}
		return infoChains;
	}

	@SuppressWarnings("unchecked")
	Hashtable getAllPolymerInfo(BitSet bs) {
		Hashtable finalInfo = new Hashtable();
		Vector modelVector = new Vector();
		int modelCount = getModelCount();
		for (int i = 0; i < modelCount; ++i) {
			Hashtable modelInfo = new Hashtable();
			Vector info = new Vector();
			int polymerCount = getPolymerCountInModel(i);
			for (int ip = 0; ip < polymerCount; ip++) {
				Hashtable polyInfo = getPolymerInfo(i, ip, bs);
				if (!polyInfo.isEmpty())
					info.add(polyInfo);
			}
			if (info.size() > 0) {
				modelInfo.put("modelIndex", new Integer(i));
				modelInfo.put("polymers", info);
				modelVector.add(modelInfo);
			}
		}
		finalInfo.put("models", modelVector);
		return finalInfo;
	}

	String getPolymerSequence(int iModel, int iPolymer) {
		String sequence = "";
		Polymer polymer = frame.mmset.getModel(iModel).getPolymer(iPolymer);
		int monomerCount = polymer.monomerCount;
		for (int i = 0; i < monomerCount; i++)
			sequence += polymer.monomers[i].getGroup1();
		return sequence;
	}

	void getPolymerSequenceAtoms(int iModel, int iPolymer, int group1, int nGroups, BitSet bsInclude, BitSet bsResult) {
		Polymer polymer = frame.mmset.getModel(iModel).getPolymer(iPolymer);
		int monomerCount = polymer.monomerCount;
		int max = group1 + nGroups;
		for (int i = group1; i < monomerCount && i < max; i++) {
			int jfirst = polymer.monomers[i].firstAtomIndex;
			int jlast = polymer.monomers[i].lastAtomIndex;
			for (int j = jfirst; j <= jlast; j++)
				if (bsInclude.get(j))
					bsResult.set(j);
		}
	}

	@SuppressWarnings("unchecked")
	Hashtable getPolymerInfo(int iModel, int iPolymer, BitSet bs) {
		Hashtable returnInfo = new Hashtable();
		Vector info = new Vector();
		Polymer polymer = frame.mmset.getModel(iModel).getPolymer(iPolymer);
		int monomerCount = polymer.monomerCount;
		String sequence = "";
		for (int i = 0; i < monomerCount; i++) {
			if (bs.get(polymer.monomers[i].getLeadAtomIndex())) {
				Hashtable monomerInfo = polymer.monomers[i].getMyInfo();
				monomerInfo.put("monomerIndex", new Integer(i));
				info.add(monomerInfo);
				sequence += polymer.monomers[i].getGroup1();
			}
		}
		if (info.size() > 0) {
			returnInfo.put("sequence", sequence);
			returnInfo.put("monomers", info);
		}
		return returnInfo;
	}

	void addStateScript(String script) {
		if (frame != null)
			frame.addStateScript(script);
	}

	String getState() {
		if (frame == null)
			return "";
		return frame.getState();
	}

	@SuppressWarnings("unchecked")
	Hashtable getBoundBoxInfo() {
		Hashtable info = new Hashtable();
		info.put("center", getBoundBoxCenter());
		info.put("edge", getBoundBoxCornerVector());
		return info;
	}

	void setModelVisibility() {

		if (frame == null) // necessary for file chooser
			return;

		// named objects must be set individually
		// in the future, we might include here a BITSET of models rather than just a modelIndex

		BitSet bs = viewer.getVisibleFramesBitSet();
		for (int i = JmolConstants.SHAPE_MIN_NAMED_OBJECT; i < JmolConstants.SHAPE_MAX; i++)
			if (frame.shapes[i] != null)
				frame.shapes[i].setVisibilityFlags(bs);
		Polyhedra p = (Polyhedra) frame.shapes[JmolConstants.SHAPE_POLYHEDRA];
		if (p != null)
			p.setVisibilityFlags(bs);
		if (frame.shapes[JmolConstants.SHAPE_HALOS] != null)
			frame.shapes[JmolConstants.SHAPE_HALOS].setVisibilityFlags(bs);
		// BALLS sets the JmolConstants.ATOM_IN_MODEL flag.
		frame.shapes[JmolConstants.SHAPE_BALLS].setVisibilityFlags(bs);

		// set clickability -- this enables measures and such
		for (int i = 0; i < JmolConstants.SHAPE_MAX; ++i) {
			if (frame == null)
				continue; // XIE
			Shape shape = frame.shapes[i];
			if (shape != null)
				shape.setModelClickability();
		}
	}

	void checkObjectClicked(int x, int y, int modifiers) {
		for (int i = 0; i < JmolConstants.SHAPE_MAX; ++i) {
			Shape shape = frame.shapes[i];
			if (shape != null) {
				shape.checkObjectClicked(x, y, modifiers);
			}
		}
	}

	void checkObjectHovered(int x, int y) {
		if (frame == null)
			return;
		for (int i = 0; i < JmolConstants.SHAPE_MAX; ++i) {
			Shape shape = frame.shapes[i];
			if (shape != null && shape.checkObjectHovered(x, y))
				break;
		}
	}

	void checkObjectDragged(int prevX, int prevY, int deltaX, int deltaY, int modifiers) {
		for (int i = 0; i < JmolConstants.SHAPE_MAX; ++i) {
			Shape shape = frame.shapes[i];
			if (shape != null) {
				shape.checkObjectDragged(prevX, prevY, deltaX, deltaY, modifiers);
			}
		}
	}

	@SuppressWarnings("unchecked")
	Hashtable getShapeInfo() {
		Hashtable info = new Hashtable();
		StringBuffer commands = new StringBuffer();
		for (int i = 0; i < JmolConstants.SHAPE_MAX; ++i) {
			Shape shape = frame.shapes[i];
			if (shape != null) {
				String shapeType = JmolConstants.shapeClassBases[i];
				if ("Draw,Dipoles,Isosurface,LcaoOrbital,MolecularOrbital".indexOf(shapeType) >= 0) {
					Hashtable shapeinfo = new Hashtable();
					shapeinfo.put("obj", shape.getShapeDetail());
					info.put(shapeType, shapeinfo);
				}
			}
		}
		if (commands.length() > 0)
			info.put("shapeCommands", commands.toString());
		return info;
	}

	Point3f getAveragePosition(int atomIndex1, int atomIndex2) {
		if (frame == null)
			return new Point3f();
		return frame.getAveragePosition(atomIndex1, atomIndex2);
	}

	Vector3f getAtomVector(int atomIndex1, int atomIndex2) {
		if (frame == null)
			return new Vector3f();
		return frame.getAtomVector(atomIndex1, atomIndex2);
	}

	Vector3f getModelDipole() {
		if (frame == null)
			return new Vector3f();
		return frame.getModelDipole();
	}

	void getBondDipoles() {
		if (frame != null)
			frame.getBondDipoles();
	}

	boolean modelsHaveSymmetry() {
		if (frame == null)
			return false;
		return frame.someModelsHaveSymmetry;
	}

	BitSet setConformation(int modelIndex, BitSet bsConformation) {
		if (frame == null)
			return null;
		frame.setConformation(modelIndex, bsConformation);
		return bsConformation;
	}

	BitSet setConformation(int modelIndex, int conformationIndex) {
		if (frame == null)
			return null;
		return frame.setConformation(modelIndex, conformationIndex);
	}

	String getAltLocListInModel(int modelIndex) {
		if (frame == null)
			return "";
		if (modelIndex < 0)
			return "";
		return frame.getAltLocListInModel(modelIndex);
	}

	void autoHbond(BitSet bsFrom, BitSet bsTo) {
		if (frame != null)
			frame.autoHbond(bsFrom, bsTo);
	}

	boolean hbondsAreVisible(int modelIndex) {
		if (frame == null)
			return false;
		int bondCount = getBondCount();
		Bond[] bonds = frame.bonds;
		for (int i = bondCount; --i >= 0;)
			if (modelIndex < 0 || modelIndex == bonds[i].atom1.modelIndex)
				if (bonds[i].isHydrogen() && bonds[i].mad > 0)
					return true;
		return false;
	}

	void convertFractionalCoordinates(int modelIndex, Point3f pt) {
		if (frame != null)
			frame.convertFractionalCoordinates(modelIndex, pt);
	}

	void clearBfactorRange() {
		if (frame != null)
			frame.clearBfactorRange();
	}

	void setZeroBased() {
		if (frame != null)
			frame.setZeroBased();
	}

	public void setAtomCoord(int atomIndex, float x, float y, float z) {
		if (frame != null)
			frame.setAtomCoord(atomIndex, x, y, z);
	}

	void setAtomCoordRelative(int atomIndex, float x, float y, float z) {
		if (frame != null)
			frame.setAtomCoordRelative(atomIndex, x, y, z);
	}

	void setAtomCoordRelative(Point3f offset, BitSet bs) {
		if (frame != null)
			frame.setAtomCoordRelative(bs, offset.x, offset.y, offset.z);
	}

	boolean getPrincipalAxes(int atomIndex, Vector3f z, Vector3f x, String lcaoType, boolean hybridizationCompatible) {
		if (frame == null)
			return false;
		return frame.getPrincipalAxes(atomIndex, z, x, lcaoType, hybridizationCompatible);
	}

	Point3f[] getAdditionalHydrogens(BitSet atomSet) {
		if (frame == null)
			return null;
		return frame.getAdditionalHydrogens(atomSet);
	}

	String getUnitCellInfoText() {
		if (frame == null)
			return "";
		int modelIndex = viewer.getDisplayModelIndex();
		if (modelIndex < 0)
			return "no single current model";
		if (frame.cellInfos == null)
			return "not applicable";
		return frame.cellInfos[modelIndex].getUnitCellInfo();
	}

	String getSpaceGroupInfoText(String spaceGroup) {
		if (frame == null)
			return "";
		SpaceGroup sg;
		String strOperations = "";
		int modelIndex = viewer.getDisplayModelIndex();
		if (spaceGroup == null) {
			if (modelIndex < 0)
				return "no single current model";
			if (frame.cellInfos == null)
				return "not applicable";
			CellInfo cellInfo = frame.cellInfos[modelIndex];
			spaceGroup = cellInfo.spaceGroup;
			if (spaceGroup.indexOf("[") >= 0)
				spaceGroup = spaceGroup.substring(0, spaceGroup.indexOf("[")).trim();
			if (spaceGroup == "spacegroup unspecified")
				return "no space group identified in file";
			sg = SpaceGroup.determineSpaceGroup(spaceGroup, cellInfo.getNotionalUnitCell());
			strOperations = "\nSymmetry operations employed:" + frame.getModelSymmetryList(modelIndex);
		}
		else if (spaceGroup.equalsIgnoreCase("ALL")) {
			return SpaceGroup.dumpAll();
		}
		else {
			sg = SpaceGroup.determineSpaceGroup(spaceGroup);
			if (sg == null)
				sg = SpaceGroup.createSpaceGroup(spaceGroup);
		}
		if (sg == null)
			return "could not identify space group from name: " + spaceGroup;
		return sg.dumpInfo() + strOperations;
	}

	public int getSpaceGroupIndexFromName(String spaceGroup) {
		return SpaceGroup.determineSpaceGroupIndex(spaceGroup);
	}

	public void setSelectionHaloEnabled(boolean selectionHaloEnabled) {
		if (frame != null)
			frame.setSelectionHaloEnabled(selectionHaloEnabled);
	}

	boolean getSelectionHaloEnabled() {
		if (frame == null)
			return true; // XIE
		return frame.getSelectionHaloEnabled();
	}

	void calculateStructures() {
		if (frame != null)
			frame.calculateStructures(true);
	}

	boolean getEchoStateActive() {
		if (frame == null)
			return false; // XIE
		return frame.getEchoStateActive();
	}

	void setEchoStateActive(boolean TF) {
		if (frame != null)
			frame.setEchoStateActive(TF);
	}

	boolean havePartialCharges() {
		if (frame == null)
			return false;
		return frame.partialCharges != null;
	}

	void setFormalCharges(BitSet bs, int formalCharge) {
		if (frame != null)
			frame.setFormalCharges(bs, formalCharge);
	}

	UnitCell getUnitCell(int modelIndex) {
		if (frame == null)
			return null;
		if (modelIndex < 0)
			return null;
		return (frame.cellInfos == null ? null : frame.cellInfos[modelIndex].unitCell);
	}

	Point3f getUnitCellOffset(int modelIndex) {
		// from "unitcell {i j k}" via uccage
		UnitCell unitCell = getUnitCell(modelIndex);
		if (unitCell == null)
			return null;
		return unitCell.getCartesianOffset();
	}

	boolean setUnitCellOffset(int modelIndex, Point3f pt) {
		// from "unitcell {i j k}" via uccage
		UnitCell unitCell = getUnitCell(modelIndex);
		if (unitCell == null)
			return false;
		unitCell.setOffset(pt);
		return true;
	}

	boolean setUnitCellOffset(int modelIndex, int nnn) {
		UnitCell unitCell = getUnitCell(modelIndex);
		if (unitCell == null)
			return false;
		unitCell.setOffset(nnn);
		return true;
	}

}
