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

package org.concord.mw2d;

import java.awt.Color;
import java.util.Arrays;

import org.concord.molbio.engine.Aminoacid;
import org.concord.mw2d.models.AminoAcidAdapter;
import org.concord.mw2d.models.Atom;
import org.concord.mw2d.models.Element;

class ColorManager {

	final static int POSITIVE_CHARGE_COLOR = 0xff0000;
	final static int NEGATIVE_CHARGE_COLOR = 0x00ff00;
	final static int HYDROPHOBIC_COLOR = 0xffafaf;
	final static int HYDROPHILIC_COLOR = 0x00ffff;
	final static int LEGO_HYDROPHOBIC_COLOR = 0xf5cd2f;
	final static int LEGO_UNCHARGED_POLAR_COLOR = 0x287f46;
	final static int LEGO_ACID_COLOR = 0xc4281b;
	final static int LEGO_BASIC_COLOR = 0x0d69ab;

	private final static int WHITE_ARGB = 0xffffff;

	private int[] elementColors;

	ColorManager() {
		elementColors = new int[Element.getNumberOfElements()];
		reset();
	}

	void reset() {
		Arrays.fill(elementColors, WHITE_ARGB);
		elementColors[Element.ID_PL] = 0x00ff00;
		elementColors[Element.ID_WS] = 0x0000ff;
		elementColors[Element.ID_CK] = 0xff00ff;
		elementColors[Element.ID_MO] = 0xffc800;
		elementColors[Element.ID_SP] = 0xffff00;
		elementColors[Element.ID_A] = 0x7da7d9;
		elementColors[Element.ID_C] = 0xc4df9a;
		elementColors[Element.ID_G] = 0xfdc588;
		elementColors[Element.ID_T] = 0xfff699;
		elementColors[Element.ID_U] = 0xfff699;
	}

	int[] getElementColors() {
		return elementColors;
	}

	void setElementColors(int[] i) {
		if (i == null)
			return;
		if (i.length != elementColors.length)
			return;
		System.arraycopy(i, 0, elementColors, 0, elementColors.length);
	}

	void setColor(byte element, Color color) {
		elementColors[element] = color.getRGB();
	}

	int getColor(Atom atom) {
		if (atom == null)
			return WHITE_ARGB;
		int i = atom.getID();
		if (i < 0 || i >= elementColors.length)
			return WHITE_ARGB;
		if (atom.isAminoAcid()) {
			String s = ((AtomisticView) atom.getHostModel().getView()).getColorCoding();
			if ("Charge".equals(s)) {
				Aminoacid a = AminoAcidAdapter.getAminoAcid((byte) atom.getID());
				if (a.getCharge() > 0.000001)
					return POSITIVE_CHARGE_COLOR;
				if (a.getCharge() < -0.000001)
					return NEGATIVE_CHARGE_COLOR;
			}
			else if ("Hydrophobicity".equals(s)) {
				Aminoacid a = AminoAcidAdapter.getAminoAcid((byte) atom.getID());
				if (a.getHydrophobicity() > 0.000001)
					return HYDROPHOBIC_COLOR;
				if (a.getHydrophobicity() < -0.000001)
					return HYDROPHILIC_COLOR;
			}
			else if ("Lego".equals(s)) {
				Aminoacid a = AminoAcidAdapter.getAminoAcid((byte) atom.getID());
				if (a.getCharge() > 0.000001)
					return LEGO_BASIC_COLOR;
				if (a.getCharge() < -0.000001)
					return LEGO_ACID_COLOR;
				if (a.getHydrophobicity() > 0.000001)
					return LEGO_HYDROPHOBIC_COLOR;
				if (a.getHydrophobicity() < -0.000001)
					return LEGO_UNCHARGED_POLAR_COLOR;
			}
		}
		return elementColors[atom.getID()];
	}

}