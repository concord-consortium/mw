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

import java.util.HashMap;
import java.util.Map;

import org.concord.molbio.engine.Aminoacid;

import static org.concord.mw2d.models.Element.*;

public class AminoAcidAdapter {

	private final static Map<Byte, Aminoacid> ELEMENT_AMINO_ACID_MAP = new HashMap<Byte, Aminoacid>();

	private final static byte[] ELEMENTS = { ID_GLY, ID_ALA, ID_VAL, ID_LEU, ID_ILE, ID_PHE, ID_PRO, ID_TRP, ID_MET,
			ID_CYS, ID_ASN, ID_GLN, ID_SER, ID_THR, ID_TYR, ID_ASP, ID_GLU, ID_LYS, ID_ARG, ID_HIS };

	static {
		Aminoacid[] acid = Aminoacid.getAllAminoacids();
		for (int i = 0; i < acid.length; i++) {
			ELEMENT_AMINO_ACID_MAP.put(ELEMENTS[i], acid[i]);
			acid[i].putProperty("element", new Byte(ELEMENTS[i]));
			// acid[i].putProperty("mass", new Float(5*acid[i].getMolWeight()/acid[0].getMolWeight()));
			acid[i].putProperty("mass", new Float(acid[i].getMolWeight() / 120));
			// NOTE: >>> 120 is due to the fact that the unit of mass in MW is 120 gram per mole.
			acid[i].putProperty("sigma", new Double(15 * Math.pow(acid[i].getVolume() / acid[0].getVolume(),
					0.3333333333333)));
		}
	}

	public static Aminoacid getAminoAcid(byte element) {
		return ELEMENT_AMINO_ACID_MAP.get(element);
	}

	public static byte getElementID(Aminoacid aa) {
		for (Byte key : ELEMENT_AMINO_ACID_MAP.keySet()) {
			if (aa == ELEMENT_AMINO_ACID_MAP.get(key))
				return key;
		}
		return -1;
	}

	/* express a RNA codon to an amino acid */
	static Aminoacid expressFromRNA(char[] code) {
		if (code == null)
			throw new IllegalArgumentException("input codon error");
		return Aminoacid.express(code, Aminoacid.EXPRESS_FROM_RNA);
	}

}