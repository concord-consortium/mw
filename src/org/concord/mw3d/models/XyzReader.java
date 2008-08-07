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
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JOptionPane;

import org.concord.modeler.event.ProgressEvent;
import org.concord.modeler.event.ProgressListener;

public final class XyzReader extends ColumnDataParser {

	private MolecularModel model;
	private int atomCount;
	private int rbondCount;
	private int abondCount;
	private int tbondCount;
	private String description;
	private String line;
	private String token;
	private int initialAtomCount;
	private List<ProgressListener> progressListeners;

	public XyzReader(MolecularModel model) {
		this.model = model;
	}

	public void addProgressListener(ProgressListener pl) {
		if (progressListeners == null)
			progressListeners = new ArrayList<ProgressListener>();
		progressListeners.add(pl);
	}

	public void removeProgressListener(ProgressListener pl) {
		if (progressListeners == null)
			return;
		progressListeners.remove(pl);
	}

	private void notifyProgressListeners(String description, int percent) {
		if (progressListeners == null || progressListeners.isEmpty())
			return;
		ProgressEvent e = new ProgressEvent(this, percent, description);
		for (ProgressListener pl : progressListeners)
			pl.progressReported(e);
	}

	public void read(File file) throws Exception {
		if (!file.exists())
			return;
		initialAtomCount = model.getAtomCount();
		BufferedReader reader = new BufferedReader(new FileReader(file));
		try {
			readAtomCount(reader);
			if (atomCount == 0)
				return;
			if (atomCount + initialAtomCount > model.getMaxAtom()) {
				JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(model.getView()), "Number of atoms "
						+ (atomCount + initialAtomCount) + " exceeds simulation capacitiy (" + model.getMaxAtom()
						+ ").", "Too many atoms", JOptionPane.ERROR_MESSAGE);
				return;
			}
			readDescription(reader);
			readAtoms(reader);
			readBonds(reader);
			reset();
		}
		catch (Exception e) {
			e.printStackTrace();
			throw new Exception(e.getMessage());
		}
		finally {
			reader.close();
		}
	}

	public void read(URL url) throws Exception {
		initialAtomCount = model.getAtomCount();
		BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
		try {
			readAtomCount(reader);
			if (atomCount == 0)
				return;
			if (atomCount + initialAtomCount > model.getMaxAtom()) {
				JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(model.getView()), "Number of atoms "
						+ (atomCount + initialAtomCount) + " exceeds simulation capacitiy (" + model.getMaxAtom()
						+ ").", "Too many atoms", JOptionPane.ERROR_MESSAGE);
				return;
			}
			readDescription(reader);
			readAtoms(reader);
			readBonds(reader);
			reset();
		}
		catch (Exception e) {
			throw new Exception(e.getMessage());
		}
		finally {
			reader.close();
		}
	}

	private void reset() {
		atomCount = 0;
		rbondCount = abondCount = tbondCount = 0;
		description = null;
	}

	private void readAtomCount(BufferedReader reader) throws Exception {
		line = reader.readLine();
		if (line != null)
			atomCount = parseInt(line);
	}

	private void readDescription(BufferedReader reader) throws Exception {
		line = reader.readLine();
		if (line == null)
			description = null;
		else description = line.trim();
		if (description != null && description.startsWith("#forces")) {
			int i = description.indexOf("[");
			int j = description.indexOf("]");
			if (i != -1 && j != -1) {
				String[] s = description.substring(i + 1, j).split("\\s+");
				rbondCount = Integer.parseInt(s[0]);
				abondCount = Integer.parseInt(s[1]);
				tbondCount = Integer.parseInt(s[2]);
			}
		}
	}

	private void readAtoms(BufferedReader reader) throws Exception {
		float inv = 100.0f / atomCount;
		int interval = atomCount / 10;
		for (int i = 0; i < atomCount; i++) {
			line = reader.readLine();
			model.addAtom(parseToken(line), parseFloat(line, ichNextParse), parseFloat(line, ichNextParse), parseFloat(
					line, ichNextParse), parseFloat(line, ichNextParse), parseFloat(line, ichNextParse), parseFloat(
					line, ichNextParse), parseFloat(line, ichNextParse));
			// MW-specific
			model.atom[i].setDamp(parseFloat(line, ichNextParse));
			if (atomCount > 20 && (i % interval == 0)) {
				notifyProgressListeners("Reading atoms: ", (int) (inv * i + 1));
			}
		}
	}

	private void readBonds(BufferedReader reader) throws Exception {
		int n1, n2, n3, n4;
		float strength, value;
		float invRb = rbondCount > 1 ? 100.0f / rbondCount : 1;
		float invAb = abondCount > 1 ? 100.0f / abondCount : 1;
		float invTb = tbondCount > 1 ? 100.0f / tbondCount : 1;
		int iRb = 0, iAb = 0, iTb = 0;
		int nRb = rbondCount / 10;
		int nAb = abondCount / 10;
		int nTb = tbondCount / 10;
		while ((line = reader.readLine()) != null) {
			token = parseToken(line).intern();
			if (token == "bond") {
				n1 = parseInt(line, ichNextParse) + initialAtomCount;
				n2 = parseInt(line, ichNextParse) + initialAtomCount;
				strength = parseFloat(line, ichNextParse);
				value = parseFloat(line, ichNextParse);
				addRBond(n1, n2, strength, value);
				iRb++;
				if (rbondCount > 20 && (iRb % nRb == 0)) {
					notifyProgressListeners("Reading radial bonds: ", (int) (invRb * iRb + 1));
				}
			}
			else if (token == "angle") {
				n1 = parseInt(line, ichNextParse) + initialAtomCount;
				n2 = parseInt(line, ichNextParse) + initialAtomCount;
				n3 = parseInt(line, ichNextParse) + initialAtomCount;
				strength = parseFloat(line, ichNextParse);
				value = parseFloat(line, ichNextParse);
				addABond(n1, n2, n3, strength, value);
				iAb++;
				if (abondCount > 20 && (iAb % nAb == 0)) {
					notifyProgressListeners("Reading angular bonds: ", (int) (invAb * iAb + 1));
				}
			}
			else if (token == "torsion") {
				n1 = parseInt(line, ichNextParse) + initialAtomCount;
				n2 = parseInt(line, ichNextParse) + initialAtomCount;
				n3 = parseInt(line, ichNextParse) + initialAtomCount;
				n4 = parseInt(line, ichNextParse) + initialAtomCount;
				strength = parseFloat(line, ichNextParse);
				value = parseFloat(line, ichNextParse);
				addTBond(n1, n2, n3, n4, strength, value);
				iTb++;
				if (tbondCount > 20 && (iTb % nTb == 0)) {
					notifyProgressListeners("Reading torsional bonds: ", (int) (invTb * iTb + 1));
				}
			}
		}
	}

	private void addRBond(int i, int j, float strength, float length) {
		RBond rbond = new RBond(model.getAtom(i), model.getAtom(j));
		rbond.setLength(length);
		rbond.setStrength(strength);
		model.addRBond(rbond);
	}

	private void addABond(int i, int j, int k, float strength, float angle) {
		ABond abond = new ABond(model.getAtom(i), model.getAtom(j), model.getAtom(k));
		abond.setAngle(angle);
		abond.setStrength(strength);
		model.addABond(abond);
	}

	private void addTBond(int i, int j, int k, int l, float strength, float angle) {
		TBond tbond = new TBond(model.getAtom(i), model.getAtom(j), model.getAtom(k), model.getAtom(l));
		tbond.setAngle(angle);
		tbond.setStrength(strength);
		model.addTBond(tbond);
	}

}