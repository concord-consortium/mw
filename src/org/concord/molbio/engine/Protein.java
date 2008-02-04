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

package org.concord.molbio.engine;

import java.util.Vector;

public class Protein {

	Vector<Aminoacid> amino = new Vector<Aminoacid>();

	protected Protein() {
	}

	public Protein(Aminoacid[] aminos) {
		if (aminos == null || aminos.length < 1)
			return;
		for (Aminoacid aa : aminos)
			amino.addElement(aa);
	}

	public Aminoacid[] getAminoacids() {
		if (amino == null || amino.size() < 1)
			return null;
		Aminoacid[] aminos = new Aminoacid[amino.size()];
		for (int i = 0; i < amino.size(); i++)
			aminos[i] = amino.get(i);
		return aminos;
	}

	protected void addAminoacid(Aminoacid a) {
		amino.addElement(a);
	}

	protected void addAminoacid(String abbreviation) {
		Aminoacid a = Aminoacid.getByAbbreviation(abbreviation);
		if (a != null)
			addAminoacid(a);
	}

	public String getSymbolSequence() {
		if (amino == null || amino.size() < 1)
			return "";
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < amino.size(); i++) {
			sb.append(amino.elementAt(i).getSymbol());
		}
		return sb.toString();
	}

	public String getAbbrSequence() {
		if (amino == null || amino.size() < 1)
			return "";
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < amino.size(); i++) {
			Aminoacid a = amino.elementAt(i);
			if (a == null)
				break;
			sb.append(a.getAbbreviation());
			if (i != amino.size() - 1)
				sb.append(" ");
		}
		return sb.toString();
	}

	public String toString() {
		return getAbbrSequence();
	}

	public DNA guessDNA() {
		if (amino == null || amino.size() < 1)
			return null;
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < amino.size(); i++) {
			Aminoacid a = amino.elementAt(i);
			String codon = a.getDNA35Codon();
			sb.append(codon);
		}
		return new DNA(sb.toString());
	}

}