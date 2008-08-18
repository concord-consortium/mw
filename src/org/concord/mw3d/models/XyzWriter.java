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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import org.concord.modeler.event.ProgressEvent;
import org.concord.modeler.event.ProgressListener;

public final class XyzWriter {

	final static DecimalFormat FORMAT = new DecimalFormat("#####.#######");
	private final static String LINE_SEPARATOR = System.getProperty("line.separator");
	private MolecularModel model;
	private List<ProgressListener> progressListeners;

	public XyzWriter(MolecularModel model) {
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

	public void write(File file) throws IOException {

		if (file == null)
			throw new IllegalArgumentException("null input file");
		if (model == null)
			throw new IllegalArgumentException("null model");

		int nAtom = model.getAtomCount();
		Atom a;

		FileOutputStream out = new FileOutputStream(file);

		StringBuffer sb = new StringBuffer(100 * (nAtom + 1));

		sb.append("" + nAtom);
		sb.append(LINE_SEPARATOR);

		if (nAtom >= 0) {
			sb.append("#symbol   rx   ry   rz   vx   vy   vz   charge   damp   ---  Molecular Workbench");
		}
		sb.append(LINE_SEPARATOR);

		if (nAtom > 0) {

			float inv = 100.0f / nAtom;
			int interval = nAtom / 10;
			for (int i = 0; i < nAtom; i++) {
				a = model.getAtom(i);
				sb.append(a.getSymbol() + "  " + FORMAT.format(a.getRx()) + "  " + FORMAT.format(a.getRy()) + "  "
						+ FORMAT.format(a.getRz()) + "  " + FORMAT.format(a.getVx()) + "  " + FORMAT.format(a.getVy())
						+ "  " + FORMAT.format(a.getVz()) + "  " + FORMAT.format(a.getCharge()));
				// MW-specific data
				sb.append("  " + FORMAT.format(a.getDamp()));
				sb.append(LINE_SEPARATOR);
				if (nAtom > 20 && (i % interval == 0)) {
					notifyProgressListeners("Writing atoms: ", (int) (inv * i + 1));
				}
			}

			int nbond = model.getRBondCount();
			if (nbond > 0) {
				inv = 100.0f / nbond;
				interval = nbond / 10;
				for (int i = 0; i < nbond; i++) {
					sb.append(model.getRBond(i));
					sb.append(LINE_SEPARATOR);
					if (nbond > 20 && (i % interval == 0)) {
						notifyProgressListeners("Writing radial bonds: ", (int) (inv * i + 1));
					}
				}
			}

			nbond = model.getABondCount();
			if (nbond > 0) {
				inv = 100.0f / nbond;
				interval = nbond / 10;
				for (int i = 0; i < nbond; i++) {
					sb.append(model.getABond(i));
					sb.append(LINE_SEPARATOR);
					if (nbond > 20 && (i % interval == 0)) {
						notifyProgressListeners("Writing angular bonds: ", (int) (inv * i + 1));
					}
				}
			}

			nbond = model.getTBondCount();
			if (nbond > 0) {
				inv = 100.0f / nbond;
				interval = nbond / 10;
				for (int i = 0; i < nbond; i++) {
					sb.append(model.getTBond(i));
					sb.append(LINE_SEPARATOR);
					if (nbond > 20 && (i % interval == 0)) {
						notifyProgressListeners("Writing torsional bonds: ", (int) (inv * i + 1));
					}
				}
			}

		}

		int clength = sb.length();
		byte[] c = new byte[clength];
		c = sb.toString().getBytes();

		try {
			out.write(c, 0, clength);
		}
		catch (IOException e) {
			e.printStackTrace(System.err);
			throw new IOException(e.getMessage());
		}
		finally {
			try {
				out.close();
			}
			catch (IOException e) {
				e.printStackTrace(System.err);
			}
			notifyProgressListeners("Structure exported to " + file, 0);
		}

	}

}