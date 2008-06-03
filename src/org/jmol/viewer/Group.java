/* $RCSfile: Group.java,v $
 * $Author: qxie $
 * $Date: 2007-03-28 15:24:21 $
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

import org.jmol.util.Logger;
import org.jmol.util.ArrayUtil;

import java.util.Hashtable;
import java.util.BitSet;

class Group {

	Chain chain;
	int seqcode;
	short groupID;
	int firstAtomIndex = -1;
	int lastAtomIndex;
	int shapeVisibilityFlags = 0;
	int minZ;
	float phi = Float.NaN;
	float psi = Float.NaN;

	Group(Chain chain, String group3, int seqcode, int firstAtomIndex, int lastAtomIndex) {
		this.chain = chain;
		this.seqcode = seqcode;

		if (group3 == null)
			group3 = "";
		this.groupID = getGroupID(group3);
		this.firstAtomIndex = firstAtomIndex;
		this.lastAtomIndex = lastAtomIndex;
	}

	final void setShapeVisibility(int visFlag, boolean isVisible) {
		if (isVisible) {
			shapeVisibilityFlags |= visFlag;
		}
		else {
			shapeVisibilityFlags &= ~visFlag;
		}
	}

	final boolean isGroup3(String group3) {
		return group3Names[groupID].equalsIgnoreCase(group3);
	}

	final String getGroup3() {
		return group3Names[groupID];
	}

	static String getGroup3(short groupID) {
		return group3Names[groupID];
	}

	final String getGroup1() {
		if (groupID >= JmolConstants.predefinedGroup1Names.length)
			return "?";
		return JmolConstants.predefinedGroup1Names[groupID];
	}

	final short getGroupID() {
		return groupID;
	}

	final boolean isGroup3Match(String strWildcard) {
		int cchWildcard = strWildcard.length();
		int ichWildcard = 0;
		String group3 = group3Names[groupID];
		int cchGroup3 = group3.length();
		if (cchWildcard < cchGroup3)
			return false;
		while (cchWildcard > cchGroup3) {
			// wildcard is too long
			// so strip '?' from the beginning and the end, if possible
			if (strWildcard.charAt(ichWildcard) == '?') {
				++ichWildcard;
			}
			else if (strWildcard.charAt(ichWildcard + cchWildcard - 1) != '?') {
				return false;
			}
			--cchWildcard;
		}
		for (int i = cchGroup3; --i >= 0;) {
			char charWild = strWildcard.charAt(ichWildcard + i);
			if (charWild == '?')
				continue;
			if (charWild != group3.charAt(i))
				return false;
		}
		return true;
	}

	final char getChainID() {
		return chain.chainID;
	}

	int getPolymerLength() {
		return 0;
	}

	int getPolymerIndex() {
		return -1;
	}

	byte getProteinStructureType() {
		return JmolConstants.PROTEIN_STRUCTURE_NONE;
	}

	Hashtable getMyInfo() {
		return null;
	}

	boolean isProtein() {
		return false;
	}

	boolean isNucleic() {
		return false;
	}

	boolean isDna() {
		return false;
	}

	boolean isRna() {
		return false;
	}

	boolean isPurine() {
		return false;
	}

	boolean isPyrimidine() {
		return false;
	}

	boolean isCarbohydrate() {
		return false;
	}

	// //////////////////////////////////////////////////////////////
	// static stuff for group ids
	// //////////////////////////////////////////////////////////////

	private static Hashtable htGroup = new Hashtable();

	static String[] group3Names = new String[128];
	static short group3NameCount = 0;

	static {
		for (int i = 0; i < JmolConstants.predefinedGroup3Names.length; ++i) {
			addGroup3Name(JmolConstants.predefinedGroup3Names[i]);
		}
	}

	static void clear() {
		htGroup.clear();
	}

	@SuppressWarnings("unchecked")
	synchronized static short addGroup3Name(String group3) {
		if (group3NameCount == group3Names.length)
			group3Names = ArrayUtil.doubleLength(group3Names);
		short groupID = group3NameCount++;
		group3Names[groupID] = group3;
		htGroup.put(group3, new Short(groupID));
		return groupID;
	}

	static short getGroupID(String group3) {
		if (group3 == null)
			return -1;
		short groupID = lookupGroupID(group3);
		return (groupID != -1) ? groupID : addGroup3Name(group3);
	}

	static short lookupGroupID(String group3) {
		if (group3 != null) {
			Short boxedGroupID = (Short) htGroup.get(group3);
			if (boxedGroupID != null)
				return boxedGroupID.shortValue();
		}
		return -1;
	}

	// //////////////////////////////////////////////////////////////
	// seqcode stuff
	// //////////////////////////////////////////////////////////////

	final int getResno() {
		return seqcode >> 8;
	}

	final int getSeqcode() {
		return seqcode;
	}

	final int getSeqNumber() {
		return seqcode >> 8;
	}

	final String getSeqcodeString() {
		return getSeqcodeString(seqcode);
	}

	static int getSeqcode(int sequenceNumber, char insertionCode) {
		if (sequenceNumber == Integer.MIN_VALUE)
			return sequenceNumber;
		if (!((insertionCode >= 'A' && insertionCode <= 'Z') || (insertionCode >= 'a' && insertionCode <= 'z')
				|| (insertionCode >= '0' && insertionCode <= '9') || insertionCode == '?' || insertionCode == '*')) {
			if (insertionCode != ' ' && insertionCode != '\0')
				Logger.warn("unrecognized insertionCode:" + insertionCode);
			insertionCode = '\0';
		}
		return (sequenceNumber << 8) + insertionCode;
	}

	static String getSeqcodeString(int seqcode) {
		if (seqcode == Integer.MIN_VALUE)
			return null;
		return (seqcode & 0xFF) == 0 ? "" + (seqcode >> 8) : "" + (seqcode >> 8) + '^' + (char) (seqcode & 0xFF);
	}

	char getInsertionCode() {
		if (seqcode == Integer.MIN_VALUE)
			return '\0';
		return (char) (seqcode & 0xFF);
	}

	static char getInsertionCode(int seqcode) {
		if (seqcode == Integer.MIN_VALUE)
			return '\0';
		return (char) (seqcode & 0xFF);
	}

	final void selectAtoms(BitSet bs) {
		for (int i = firstAtomIndex; i <= lastAtomIndex; ++i)
			bs.set(i);
	}

	boolean isSelected(BitSet bs) {
		for (int i = firstAtomIndex; i <= lastAtomIndex; ++i)
			if (bs.get(i))
				return true;
		return false;
	}

	boolean isHetero() {
		// just look at the first atom of the group
		return chain.frame.atoms[firstAtomIndex].isHetero();
	}

	public String toString() {
		return "[" + getGroup3() + "-" + getSeqcodeString() + "]";
	}

}
