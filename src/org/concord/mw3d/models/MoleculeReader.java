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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

import javax.swing.JOptionPane;

final class MoleculeReader extends ColumnDataParser {

	private MolecularModel model;
	private int atomCount;
	private String description;

	public MoleculeReader(MolecularModel model) {
		this.model = model;
	}

	public void read(File file) {
		if (!file.exists())
			return;
		reset();
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(file));
			readAtomCount(reader);
			if (atomCount == 0)
				return;
			if (atomCount + model.getAtomCount() > model.getMaxAtom()) {
				JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(model.getView()), "Number of atoms "
						+ atomCount + " exceeds simulation capacitiy (" + model.getMaxAtom() + ").", "Too many atoms",
						JOptionPane.ERROR_MESSAGE);
				return;
			}
			readDescription(reader);
			readAtoms(reader);
		}
		catch (Exception e) {
			e.printStackTrace(System.err);
		}
		finally {
			if (reader != null) {
				try {
					reader.close();
				}
				catch (IOException e) {
				}
			}
		}
	}

	public void read(URL url) {
		reset();
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new InputStreamReader(url.openStream()));
			readAtomCount(reader);
			if (atomCount == 0)
				return;
			if (atomCount + model.getAtomCount() > model.getMaxAtom()) {
				JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(model.getView()), "Number of atoms "
						+ atomCount + " exceeds simulation capacitiy (" + model.getMaxAtom() + ").", "Too many atoms",
						JOptionPane.ERROR_MESSAGE);
				return;
			}
			readDescription(reader);
			readAtoms(reader);
		}
		catch (Exception e) {
			e.printStackTrace(System.err);
		}
		finally {
			if (reader != null) {
				try {
					reader.close();
				}
				catch (IOException e) {
				}
			}
		}
	}

	private void reset() {
		atomCount = 0;
		description = null;
	}

	private void readAtomCount(BufferedReader reader) throws Exception {
		String line = reader.readLine();
		if (line != null)
			atomCount = parseInt(line);
	}

	private void readDescription(BufferedReader reader) throws Exception {
		String line = reader.readLine();
		if (line == null)
			description = null;
		else description = line.trim();
	}

	public String getDescription() {
		return description;
	}

	private void readAtoms(BufferedReader reader) throws Exception {

		String line;
		int n0 = model.getAtomCount();

		for (int i = 0; i < atomCount; i++) {

			line = reader.readLine();
			model.addAtom(parseToken(line), parseFloat(line, ichNextParse), parseFloat(line, ichNextParse), parseFloat(
					line, ichNextParse), parseFloat(line, ichNextParse), parseFloat(line, ichNextParse), parseFloat(
					line, ichNextParse), parseFloat(line, ichNextParse));

		}

		String s;
		int i1, i2, i3, i4;
		Atom atom1, atom2, atom3, atom4;
		RBond rbond;
		ABond abond;
		TBond tbond;
		float strength;
		while ((line = reader.readLine()) != null) {
			s = parseToken(line).intern();
			if (s == "bond") {
				i1 = n0 + parseInt(line, ichNextParse);
				i2 = n0 + parseInt(line, ichNextParse);
				atom1 = model.getAtom(i1);
				atom2 = model.getAtom(i2);
				rbond = new RBond(atom1, atom2);
				rbond.setLength(atom1.distance(atom2));
				strength = parseFloat(line, ichNextParse);
				if (strength != Float.NaN && strength > 0)
					rbond.setStrength(strength);
				model.addRBond(rbond);
			}
			else if (s == "angle") {
				i1 = n0 + parseInt(line, ichNextParse);
				i2 = n0 + parseInt(line, ichNextParse);
				i3 = n0 + parseInt(line, ichNextParse);
				atom1 = model.getAtom(i1);
				atom2 = model.getAtom(i2);
				atom3 = model.getAtom(i3);
				abond = new ABond(atom1, atom2, atom3);
				abond.setAngle((float) ABond.getAngle(atom1, atom2, atom3));
				strength = parseFloat(line, ichNextParse);
				if (strength != Float.NaN && strength > 0)
					abond.setStrength(strength);
				model.addABond(abond);
			}
			else if (s == "torsion") {
				i1 = n0 + parseInt(line, ichNextParse);
				i2 = n0 + parseInt(line, ichNextParse);
				i3 = n0 + parseInt(line, ichNextParse);
				i4 = n0 + parseInt(line, ichNextParse);
				atom1 = model.getAtom(i1);
				atom2 = model.getAtom(i2);
				atom3 = model.getAtom(i3);
				atom4 = model.getAtom(i4);
				tbond = new TBond(atom1, atom2, atom3, atom4);
				tbond.setAngle((float) TBond.getAngle(atom1, atom2, atom3, atom4));
				strength = parseFloat(line, ichNextParse);
				if (strength != Float.NaN && strength > 0)
					tbond.setStrength(strength);
				model.addTBond(tbond);
			}
		}

	}

}