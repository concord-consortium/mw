/*
 *   Copyright (C) 2008  The Concord Consortium, Inc.,
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

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JColorChooser;
import javax.swing.JComboBox;

import org.concord.modeler.ModelerUtilities;
import org.concord.modeler.ui.ColorComboBox;
import org.concord.modeler.ui.ColorRectangle;
import org.concord.mw3d.models.MolecularModel;

/**
 * @author Charles Xie
 * 
 */
class ElementColorListener implements ActionListener {

	private Color color6 = Color.white;
	private String symbol;
	private MolecularView view;

	ElementColorListener(String symbol, MolecularModel model) {
		this.symbol = symbol;
		view = model.getView();
	}

	public void actionPerformed(ActionEvent e) {
		final JComboBox cb = (JComboBox) e.getSource();
		int id = (Integer) cb.getSelectedItem();
		if (id == ColorComboBox.INDEX_COLOR_CHOOSER) {
			String s = MolecularContainer.getInternationalText("MoreColors");
			JColorChooser.createDialog(view, s != null ? s : "More Colors", true, ModelerUtilities.colorChooser,
					new ActionListener() {
						public void actionPerformed(ActionEvent ae) {
							color6 = ModelerUtilities.colorChooser.getColor();
							view.setElementColor(symbol, color6);
							cb.setSelectedIndex(6);
							ColorRectangle cr = (ColorRectangle) cb.getRenderer();
							cr.setMoreColor(color6);
						}
					}, null).setVisible(true);
		}
		else if (id == ColorComboBox.INDEX_HEX_INPUTTER) {
			if (cb instanceof ColorComboBox) {
				final ColorComboBox colorComboBox = (ColorComboBox) cb;
				colorComboBox.updateColor(new Runnable() {
					public void run() {
						view.setElementColor(symbol, colorComboBox.getMoreColor());
						view.repaint();
					}
				});
			}
		}
		else if (id == ColorComboBox.INDEX_MORE_COLOR) {
			view.setElementColor(symbol, color6);
		}
		else {
			view.setElementColor(symbol, ColorRectangle.COLORS[id]);
		}
		view.repaint();
	}

}
