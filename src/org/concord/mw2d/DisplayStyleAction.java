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

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.JComboBox;

class DisplayStyleAction extends AbstractAction {

	private final static String[] t = new String[] { "Spacefilling", "Ball & Stick", "Wireframe", "Stick",
			"Delaunay Triangulation", "Voronoi Diagram", "Delaunay & Voronoi Overlay" };
	private AtomisticView view;

	DisplayStyleAction(AtomisticView view) {
		super();
		this.view = view;
		putValue(NAME, "Display Style");
		putValue(SHORT_DESCRIPTION, "Display Style");
		putValue("options", t);
	}

	public Object getValue(String key) {
		if (key.equalsIgnoreCase("state")) {
			switch (view.styleManager.getStyle()) {
			case StyleConstant.SPACE_FILLING:
				return t[0];
			case StyleConstant.BALL_AND_STICK:
				return t[1];
			case StyleConstant.WIRE_FRAME:
				return t[2];
			case StyleConstant.STICK:
				return t[3];
			case StyleConstant.DELAUNAY:
				return t[4];
			case StyleConstant.VORONOI:
				return t[5];
			case StyleConstant.DELAUNAY_AND_VORONOI:
				return t[6];
			}
		}
		return super.getValue(key);
	}

	public void actionPerformed(ActionEvent e) {
		if (!isEnabled())
			return;
		Object o = e.getSource();
		if (o instanceof JComboBox) {
			if (!((JComboBox) o).isShowing())
				return;
			String s = (String) (((JComboBox) o).getSelectedItem());
			if (s.equals(t[0]))
				view.setDisplayStyle(StyleConstant.SPACE_FILLING);
			else if (s.equals(t[1]))
				view.setDisplayStyle(StyleConstant.BALL_AND_STICK);
			else if (s.equals(t[2]))
				view.setDisplayStyle(StyleConstant.WIRE_FRAME);
			else if (s.equals(t[3]))
				view.setDisplayStyle(StyleConstant.STICK);
			else if (s.equals(t[4]))
				view.setDisplayStyle(StyleConstant.DELAUNAY);
			else if (s.equals(t[5]))
				view.setDisplayStyle(StyleConstant.VORONOI);
			else if (s.equals(t[6]))
				view.setDisplayStyle(StyleConstant.DELAUNAY_AND_VORONOI);
		}
		view.repaint();
	}

	public String toString() {
		return (String) getValue(SHORT_DESCRIPTION);
	}

}