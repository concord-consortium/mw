/* $RCSfile: Mmset.java,v $
 * $Author: qxie $
 * $Date: 2007-03-27 21:10:05 $
 * $Revision: 1.12 $
 *
 * Copyright (C) 2002-2005  The Jmol Development Team
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
package org.jmol.viewer;

import java.util.Hashtable;
import java.util.Properties;
import java.util.BitSet;
import java.util.Vector;
import java.util.Enumeration;

import org.jmol.util.ArrayUtil;

// Mmset == Molecular Model set

final class Mmset {
	Frame frame;

	Properties modelSetProperties;
	Hashtable modelSetAuxiliaryInfo;

	private int modelCount = 0;
	private Properties[] modelProperties = new Properties[1];
	private Hashtable[] modelAuxiliaryInfo = new Hashtable[1];
	private Model[] models = new Model[1];

	private int structureCount = 0;
	private Structure[] structures = new Structure[10];

	Mmset(Frame frame) {
		this.frame = frame;
	}

	void defineStructure(int modelIndex, String structureType, char startChainID, int startSequenceNumber,
			char startInsertionCode, char endChainID, int endSequenceNumber, char endInsertionCode) {
		if (structureCount == structures.length)
			structures = (Structure[]) ArrayUtil.setLength(structures, structureCount + 10);
		structures[structureCount++] = new Structure(modelIndex, structureType, startChainID, Group.getSeqcode(
				startSequenceNumber, startInsertionCode), endChainID, Group.getSeqcode(endSequenceNumber,
				endInsertionCode));
	}

	void clearStructures() {
		for (int i = modelCount; --i >= 0;)
			models[i].clearStructures();
	}

	void calculateStructures() {
		for (int i = modelCount; --i >= 0;)
			models[i].calculateStructures();
	}

	void setConformation(int modelIndex, BitSet bsConformation) {
		for (int i = modelCount; --i >= 0;)
			if (i == modelIndex || modelIndex < 0)
				models[i].setConformation(bsConformation);
	}

	@SuppressWarnings("unchecked")
	Hashtable getHeteroList(int modelIndex) {
		Hashtable htFull = new Hashtable();
		boolean ok = false;
		for (int i = modelCount; --i >= 0;)
			if (modelIndex < 0 || i == modelIndex) {
				Hashtable ht = (Hashtable) getModelAuxiliaryInfo(i, "hetNames");
				if (ht == null)
					continue;
				ok = true;
				Enumeration e = ht.keys();
				while (e.hasMoreElements()) {
					String key = (String) e.nextElement();
					htFull.put(key, ht.get(key));
				}
			}
		return (ok ? htFull : (Hashtable) getModelSetAuxiliaryInfo("hetNames"));
	}

	void freeze() {
		for (int i = modelCount; --i >= 0;) {
			models[i].freeze();
		}
		propogateSecondaryStructure();

	}

	void setModelSetProperties(Properties modelSetProperties) {
		this.modelSetProperties = modelSetProperties;
	}

	void setModelSetAuxiliaryInfo(Hashtable modelSetAuxiliaryInfo) {
		this.modelSetAuxiliaryInfo = modelSetAuxiliaryInfo;
	}

	Properties getModelSetProperties() {
		return modelSetProperties;
	}

	Hashtable getModelSetAuxiliaryInfo() {
		return modelSetAuxiliaryInfo;
	}

	String getModelSetProperty(String propertyName) {
		return (modelSetProperties == null ? null : modelSetProperties.getProperty(propertyName));
	}

	Object getModelSetAuxiliaryInfo(String keyName) {
		return (modelSetAuxiliaryInfo == null ? null : modelSetAuxiliaryInfo.get(keyName));
	}

	boolean getModelSetAuxiliaryInfoBoolean(String keyName) {
		return (modelSetAuxiliaryInfo != null && modelSetAuxiliaryInfo.containsKey(keyName) && ((Boolean) modelSetAuxiliaryInfo
				.get(keyName)).booleanValue());
	}

	int getModelSetAuxiliaryInfoInt(String keyName) {
		if (modelSetAuxiliaryInfo != null && modelSetAuxiliaryInfo.containsKey(keyName)) {
			return ((Integer) modelSetAuxiliaryInfo.get(keyName)).intValue();
		}
		return Integer.MIN_VALUE;
	}

	void setModelCount(int modelCount) {
		if (this.modelCount != 0)
			throw new NullPointerException();
		this.modelCount = modelCount;
		models = (Model[]) ArrayUtil.setLength(models, modelCount);
		modelProperties = (Properties[]) ArrayUtil.setLength(modelProperties, modelCount);
		modelAuxiliaryInfo = (Hashtable[]) ArrayUtil.setLength(modelAuxiliaryInfo, modelCount);
	}

	int setSymmetryAtomInfo(int modelIndex, int atomIndex, int atomCount) {
		models[modelIndex].preSymmetryAtomIndex = atomIndex;
		return models[modelIndex].preSymmetryAtomCount = atomCount;
	}

	int getPreSymmetryAtomIndex(int modelIndex) {
		return models[modelIndex].preSymmetryAtomIndex;
	}

	int getPreSymmetryAtomCount(int modelIndex) {
		return models[modelIndex].preSymmetryAtomCount;
	}

	String getModelName(int modelIndex) {
		return models[modelIndex].modelTag;
	}

	int getModelNumber(int modelIndex) {
		return models[modelIndex].modelNumber;
	}

	Properties getModelProperties(int modelIndex) {
		return modelProperties[modelIndex];
	}

	String getModelProperty(int modelIndex, String property) {
		Properties props = modelProperties[modelIndex];
		return props == null ? null : props.getProperty(property);
	}

	Hashtable getModelAuxiliaryInfo(int modelIndex) {
		return (modelIndex < 0 ? null : modelAuxiliaryInfo[modelIndex]);
	}

	@SuppressWarnings("unchecked")
	void setModelAuxiliaryInfo(int modelIndex, Object key, Object value) {
		modelAuxiliaryInfo[modelIndex].put(key, value);
	}

	Object getModelAuxiliaryInfo(int modelIndex, String key) {
		if (modelIndex < 0)
			return null;
		Hashtable info = modelAuxiliaryInfo[modelIndex];
		return info == null ? null : info.get(key);
	}

	boolean getModelAuxiliaryInfoBoolean(int modelIndex, String keyName) {
		Hashtable info = modelAuxiliaryInfo[modelIndex];
		return (info != null && info.containsKey(keyName) && ((Boolean) info.get(keyName)).booleanValue());
	}

	int getModelAuxiliaryInfoInt(int modelIndex, String keyName) {
		Hashtable info = modelAuxiliaryInfo[modelIndex];
		if (info != null && info.containsKey(keyName)) {
			return ((Integer) info.get(keyName)).intValue();
		}
		return Integer.MIN_VALUE;
	}

	Model getModel(int modelIndex) {
		return models[modelIndex];
	}

	int getModelNumberIndex(int modelNumber) {
		int i;
		for (i = modelCount; --i >= 0 && models[i].modelNumber != modelNumber;) {
		}
		return i;
	}

	int getNAltLocs(int modelIndex) {
		return models[modelIndex].nAltLocs;
	}

	int getNInsertions(int modelIndex) {
		return models[modelIndex].nInsertions;
	}

	boolean setModelNameNumberProperties(int modelIndex, String modelName, int modelNumber, Properties modelProperties,
			Hashtable modelAuxiliaryInfo, boolean isPDB) {

		this.modelProperties[modelIndex] = modelProperties;
		this.modelAuxiliaryInfo[modelIndex] = modelAuxiliaryInfo;
		models[modelIndex] = new Model(this, modelIndex, modelNumber, modelName);
		String codes = (String) getModelAuxiliaryInfo(modelIndex, "altLocs");
		models[modelIndex].setNAltLocs(codes == null ? 0 : codes.length());
		codes = (String) getModelAuxiliaryInfo(modelIndex, "insertionCodes");
		models[modelIndex].setNInsertions(codes == null ? 0 : codes.length());
		return models[modelIndex].isPDB = isPDB || getModelAuxiliaryInfoBoolean(modelIndex, "isPDB");
	}

	int getAltLocCountInModel(int modelIndex) {
		return models[modelIndex].nAltLocs;
	}

	private void propogateSecondaryStructure() {
		// issue arises with multiple file loading and multi-_data mmCIF files
		// that structural information may be model-specific

		for (int i = structureCount; --i >= 0;) {
			Structure structure = structures[i];
			for (int j = modelCount; --j >= 0;)
				if (structure.modelIndex == j || structure.modelIndex == -1)
					models[j].addSecondaryStructure(structure.type, structure.startChainID, structure.startSeqcode,
							structure.endChainID, structure.endSeqcode);
		}
	}

	int getModelCount() {
		return modelCount;
	}

	Model[] getModels() {
		return models;
	}

	int getChainCount() {
		int chainCount = 0;
		for (int i = modelCount; --i >= 0;)
			chainCount += models[i].getChainCount();
		return chainCount;
	}

	int getPolymerCount() {
		int polymerCount = 0;
		for (int i = modelCount; --i >= 0;)
			polymerCount += models[i].getPolymerCount();
		return polymerCount;
	}

	int getPolymerCountInModel(int modelIndex) {
		return models[modelIndex].getPolymerCount();
	}

	int getChainCountInModel(int modelIndex) {
		return models[modelIndex].getChainCount();
	}

	Polymer getPolymerAt(int modelIndex, int polymerIndex) {
		return models[modelIndex].getPolymer(polymerIndex);
	}

	int getGroupCount() {
		int groupCount = 0;
		for (int i = modelCount; --i >= 0;)
			groupCount += models[i].getGroupCount();
		return groupCount;
	}

	int getGroupCountInModel(int modelIndex) {
		return models[modelIndex].getGroupCount();
	}

	void calcSelectedGroupsCount(BitSet bsSelected) {
		for (int i = modelCount; --i >= 0;)
			models[i].calcSelectedGroupsCount(bsSelected);
	}

	void calcSelectedMonomersCount(BitSet bsSelected) {
		for (int i = modelCount; --i >= 0;)
			models[i].calcSelectedMonomersCount(bsSelected);
	}

	void calcHydrogenBonds(BitSet bsA, BitSet bsB) {
		for (int i = modelCount; --i >= 0;)
			models[i].calcHydrogenBonds(bsA, bsB);
	}

	void selectSeqcodeRange(int seqcodeA, int seqcodeB, BitSet bs) {
		for (int i = modelCount; --i >= 0;)
			models[i].selectSeqcodeRange(seqcodeA, seqcodeB, bs);
	}

	static class Structure {
		String typeName;
		byte type;
		char startChainID;
		int startSeqcode;
		char endChainID;
		int endSeqcode;
		int modelIndex;

		Structure(int modelIndex, String typeName, char startChainID, int startSeqcode, char endChainID, int endSeqcode) {
			this.modelIndex = modelIndex;
			this.typeName = typeName;
			this.startChainID = startChainID;
			this.startSeqcode = startSeqcode;
			this.endChainID = endChainID;
			this.endSeqcode = endSeqcode;
			if ("helix".equals(typeName))
				type = JmolConstants.PROTEIN_STRUCTURE_HELIX;
			else if ("sheet".equals(typeName))
				type = JmolConstants.PROTEIN_STRUCTURE_SHEET;
			else if ("turn".equals(typeName))
				type = JmolConstants.PROTEIN_STRUCTURE_TURN;
			else type = JmolConstants.PROTEIN_STRUCTURE_NONE;
		}

		@SuppressWarnings("unchecked")
		Hashtable toHashtable() {
			Hashtable info = new Hashtable();
			info.put("type", typeName);
			info.put("startChainID", startChainID + "");
			info.put("startSeqcode", new Integer(startSeqcode));
			info.put("endChainID", endChainID + "");
			info.put("endSeqcode", new Integer(endSeqcode));
			return info;
		}

	}

	@SuppressWarnings("unchecked")
	Vector getStructureInfo() {
		Vector info = new Vector();
		for (int i = 0; i < structureCount; i++)
			info.add(structures[i].toHashtable());
		return info;
	}

}
