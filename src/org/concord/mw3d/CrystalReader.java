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

package org.concord.mw3d;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.vecmath.Point3f;

import org.concord.mw3d.models.ColumnDataParser;
import org.concord.mw3d.models.Crystal;

class CrystalReader extends ColumnDataParser {

	private MolecularContainer container;

	CrystalReader(MolecularContainer container) {
		this.container = container;
	}

	public void read(URL url, JMenu menu) throws Exception {

		BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));

		String line;
		JMenu subMenu = null;
		JMenuItem menuItem;

		while ((line = reader.readLine()) != null) {

			if (line.startsWith("//"))
				continue;

			if (line.toLowerCase().startsWith("class:")) {
				subMenu = new JMenu(line.substring(6).trim());
				menu.add(subMenu);
			}
			else {

				String elem = parseToken(line);
				final String strk = parseToken(line, ichNextParse);
				final int nx = parseInt(line, ichNextParse);
				final int ny = parseInt(line, ichNextParse);
				final int nz = parseInt(line, ichNextParse);
				final float a = parseFloat(line, ichNextParse);
				final float b = parseFloat(line, ichNextParse);
				final float c = parseFloat(line, ichNextParse);
				final float alpha = parseFloat(line, ichNextParse);
				final float beta = parseFloat(line, ichNextParse);
				final float gamma = parseFloat(line, ichNextParse);

				final String[] el = elem.split(":");

				menuItem = new JMenuItem(strk + " - " + elem);
				menuItem.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						container.stop();
						try {
							Thread.sleep(200);
						}
						catch (InterruptedException ie) {
						}
						container.model.clear();
						container.reset();
						container.view.setCpkPercent(100);
						byte type = Crystal.SIMPLE_CUBIC;
						if (strk.equalsIgnoreCase("FCC"))
							type = Crystal.FACE_CENTERED_CUBIC;
						else if (strk.equalsIgnoreCase("BCC"))
							type = Crystal.BODY_CENTERED_CUBIC;
						else if (strk.equalsIgnoreCase("HCP"))
							type = Crystal.HCP;
						else if (strk.equalsIgnoreCase("DIAMOND"))
							type = Crystal.DIAMOND;
						else if (strk.equalsIgnoreCase("NACL"))
							type = Crystal.NACL;
						else if (strk.equalsIgnoreCase("CSCL"))
							type = Crystal.CSCL;
						else if (strk.equalsIgnoreCase("A3B(L12)"))
							type = Crystal.L12;
						else if (strk.equalsIgnoreCase("ZNS"))
							type = Crystal.ZNS;
						else if (strk.equalsIgnoreCase("CAF2"))
							type = Crystal.CAF2;
						else if (strk.equalsIgnoreCase("CATIO3"))
							type = Crystal.CATIO3;
						container.model.setSimulationBox(100, 50, 50);
						Crystal.create(type, el, nx, ny, nz, a, b, c, alpha, beta, gamma, container.model);
						container.model.translateAtomsTo(new Point3f());
						container.view.setOrientation(container.view.getViewer().getCurrentOrientation());
						container.view.renderModel(false);
						container.view.setMolecularStyle(container.view.getMolecularStyle());
					}
				});
				if (subMenu != null)
					subMenu.add(menuItem);

			}

		}

	}

}
