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

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Point;
import javax.swing.JOptionPane;

import org.concord.modeler.math.CubicPolynomial;
import org.concord.modeler.math.NaturalCubicSpline;
import org.concord.modeler.process.Executable;
import org.concord.modeler.util.SwingWorker;
import org.concord.mw2d.ModelAction;

class GenerateProteinAction extends ModelAction {

	private MolecularModel model;
	private int k;
	private Cursor oldCursor;
	private Point[] p;
	private WalkGenerator walkGenerator;
	private Walk walk;

	GenerateProteinAction(MolecularModel m) {

		super(m);
		model = m;

		putValue(SHORT_DESCRIPTION, "Generate a random self-avoided polypeptide conformation");
		putValue(NAME, "Generate Random Conformation");

		setExecutable(new Executable() {

			public void execute() {

				k = model.getNumberOfAtoms();
				oldCursor = model.view.getCursor();

				new SwingWorker("Reset Protein Action") {
					public Object construct() {
						EventQueue.invokeLater(new Runnable() {
							public void run() {
								model.view.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
							}
						});
						p = generateRandomPoints(model.getNumberOfAtoms() / 2);
						if (p != null)
							set(p);
						return null;
					}

					public void finished() {
						model.view.setCursor(oldCursor);
						if (p == null) {
							JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(model.view),
									"Failed in finding a conformation. Please try again.",
									"Self-avoided path not found yet", JOptionPane.INFORMATION_MESSAGE);
						}
					}
				}.start();

			}

		});

	}

	private void set(Point[] p) {
		int n = 0;
		for (int i = 0; i < k; i++) {
			if (model.atom[i].isAminoAcid()) {
				n = i;
				break;
			}
		}
		for (Atom a : model.atom) {
			if (a.isAminoAcid())
				a.setRestraint(null);
		}

		/* reset solvent */
		if (model.solvent != null) {
			switch (model.solvent.getType()) {
			case Solvent.WATER:
				model.view.setBackground(Solvent.WATER_COLOR);
				break;
			case Solvent.VACUUM:
				model.view.setBackground(Color.white);
				break;
			case Solvent.OIL:
				model.view.setBackground(Solvent.OIL_COLOR);
				break;
			}
		}

		double dij = 0.0, sq = 0.0, dx = 0.0, dy = 0.0;
		for (int m = 0; m < p.length; m++) {
			if (m == 0) {
				if (model.atom[n].isAminoAcid()) {
					model.atom[n].setRx(p[0].x);
					model.atom[n].setRy(p[0].y);
					n++;
				}
			}
			else {
				if (model.atom[n].isAminoAcid() && model.atom[n - 1].isAminoAcid()) {
					dx = model.atom[n - 1].rx - p[m].x;
					dy = model.atom[n - 1].ry - p[m].y;
					sq = dx * dx + dy * dy;
					dij = (model.atom[n - 1].getSigma() + model.atom[n].getSigma())
							* RadialBond.PEPTIDE_BOND_LENGTH_PARAMETER;
					if (sq >= dij * dij) {
						model.atom[n].setRx(p[m].x);
						model.atom[n].setRy(p[m].y);
						RadialBond rb = model.bonds.getBond(model.atom[n], model.atom[n - 1]);
						rb.setBondLength(dij);
						n++;
						if (n >= k)
							break;
					}
				}
			}
		}

		int w = (int) model.boundary.getWidth();
		int h = (int) model.boundary.getHeight();
		int xmin = w + w, xmax = -w, ymin = h + h, ymax = -h;
		for (int i = 0; i < k; i++) {
			if (model.atom[i].isAminoAcid()) {
				if (xmin > model.atom[i].rx)
					xmin = (int) model.atom[i].rx;
				if (xmax < model.atom[i].rx)
					xmax = (int) model.atom[i].rx;
				if (ymin > model.atom[i].ry)
					ymin = (int) model.atom[i].ry;
				if (ymax < model.atom[i].ry)
					ymax = (int) model.atom[i].ry;
			}
		}
		xmin = (xmin + xmax - w) / 2;
		ymin = (ymin + ymax - h) / 2;
		for (int i = 0; i < k; i++) {
			if (model.atom[i].isAminoAcid()) {
				model.atom[i].rx -= xmin;
				model.atom[i].ry -= ymin;
			}
		}

		model.assignTemperature(300);
		if (model.view.getUseJmol())
			model.view.refreshJmol();
		model.view.repaint();
		model.view.setCursor(oldCursor);
	}

	private Point[] generateRandomPoints(int n) {

		if (n <= 1)
			return null;

		double footStep = 3 * AtomicModel.aminoAcidElement[0].getSigma();
		Dimension dimension = new Dimension((int) (model.boundary.getSize().width / footStep), (int) (model.boundary
				.getSize().height / footStep));

		if (walkGenerator == null) {
			walkGenerator = new WalkGenerator(dimension);
		}
		else {
			if (!dimension.equals(walkGenerator.getDimension())) {
				walkGenerator.setDimension(dimension);
			}
		}
		int k = 0;
		do {
			walk = walkGenerator.generate((short) n);
			if (k == 100)
				break;
			k++;
		} while (walk == null);
		if (walk == null)
			return null;

		short[] x = new short[n + 2];
		short[] y = new short[n + 2];
		x[0] = walk.getOriginX();
		y[0] = walk.getOriginY();
		x[1] = walk.getFirstNodeX();
		y[1] = walk.getFirstNodeY();
		System.arraycopy(walk.getXArray(), 0, x, 2, n);
		System.arraycopy(walk.getYArray(), 0, y, 2, n);
		n++;

		int m = 50;
		Point[] p = new Point[n * m];

		CubicPolynomial[] cx = NaturalCubicSpline.compute(n, x);
		CubicPolynomial[] cy = NaturalCubicSpline.compute(n, y);
		float u = 0.0f;
		k = 0;
		for (short i = 0; i < n; i++) {
			for (short j = 0; j < m; j++) {
				u = (float) j / (float) m;
				p[k] = new Point((int) (cx[i].getValue(u) * footStep), (int) (cy[i].getValue(u) * footStep));
				k++;
			}
		}

		return p;

	}

	public String toString() {
		return (String) getValue(SHORT_DESCRIPTION);
	}

}