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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;

import javax.swing.JPanel;

import org.concord.modeler.ui.PieChart;
import org.concord.mw2d.models.AtomicModel;
import org.concord.mw2d.models.Element;

class AtomComposition extends JPanel {

	private final static Color[] COLOR = new Color[] { Color.white, Color.green, Color.blue, Color.magenta,
			Color.orange };

	private static String[] s = new String[5];
	private float[] p = new float[5];
	private PieChart pie;

	public AtomComposition(AtomicModel model) {

		super(new BorderLayout());

		if (s[0] == null) {
			s[0] = model.getElement(Element.ID_NT).getName();
			s[1] = model.getElement(Element.ID_PL).getName();
			s[2] = model.getElement(Element.ID_WS).getName();
			s[3] = model.getElement(Element.ID_CK).getName();
			s[4] = "Others";
		}

		setPercentage(model);

		pie = new PieChart(p, COLOR, s);
		pie.setEnabled(false);
		pie.setPreferredSize(new Dimension(220, 120));
		pie.setTotal(model.getNumberOfAtoms());
		add(pie, BorderLayout.CENTER);

	}

	public void setPercentage(AtomicModel model) {
		if (model.isEmpty())
			return;
		for (int i = 0; i < 5; i++)
			p[i] = 0;
		for (int i = 0, n = model.getNumberOfAtoms(); i < n; i++) {
			if (model.getAtom(i).getID() <= Element.ID_CK) {
				p[model.getAtom(i).getID() - Element.ID_NT]++;
			}
			else {
				p[4]++;
			}
		}
		for (int i = 0; i < 5; i++)
			p[i] /= model.getNumberOfAtoms();
	}

}