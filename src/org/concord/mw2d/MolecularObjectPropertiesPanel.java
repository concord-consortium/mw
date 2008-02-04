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
import java.awt.FlowLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SpringLayout;

import org.concord.modeler.ModelerUtilities;
import org.concord.modeler.draw.FillMode;
import org.concord.modeler.ui.BackgroundComboBox;
import org.concord.mw2d.models.CurvedRibbon;
import org.concord.mw2d.models.CurvedSurface;
import org.concord.mw2d.models.MolecularModel;
import org.concord.mw2d.models.MolecularObject;

class MolecularObjectPropertiesPanel extends MoleculePropertiesPanel {

	MolecularObjectPropertiesPanel(final MolecularObject molo) {

		super(new BorderLayout(5, 5));

		if (molo.getHostModel() == null)
			throw new NullPointerException("Error: This molecular object does not contain a model reference");

		final Point com = molo.getCenterOfMass();
		final float rg = molo.getRadiusOfGyration(com);
		rbc = ((MolecularModel) molo.getHostModel()).getBonds();
		abc = ((MolecularModel) molo.getHostModel()).getBends();
		mc = ((MolecularModel) molo.getHostModel()).getMolecules();
		av = (AtomisticView) molo.getHostModel().getView();

		final BackgroundComboBox bgComboBox = new BackgroundComboBox(molo.getHostModel().getView(),
				ModelerUtilities.colorChooser, ModelerUtilities.fillEffectChooser);
		bgComboBox.setFillMode(molo.getFillMode());
		bgComboBox.getColorMenu().setColor(molo.getBackground());
		bgComboBox.getColorMenu().setNoFillAction(new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				FillMode fm = FillMode.getNoFillMode();
				if (fm.equals(molo.getFillMode()))
					return;
				molo.setFillMode(fm);
				molo.getHostModel().getView().repaint();
				molo.getHostModel().notifyChange();
			}
		});
		bgComboBox.getColorMenu().setColorArrayAction(new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				FillMode fm = new FillMode.ColorFill(bgComboBox.getColorMenu().getColor());
				if (fm.equals(molo.getFillMode()))
					return;
				molo.setFillMode(fm);
				molo.getHostModel().getView().repaint();
				molo.getHostModel().notifyChange();
			}
		});
		bgComboBox.getColorMenu().setMoreColorAction(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				FillMode fm = new FillMode.ColorFill(bgComboBox.getColorMenu().getColorChooser().getColor());
				if (fm.equals(molo.getFillMode()))
					return;
				molo.setFillMode(fm);
				molo.getHostModel().getView().repaint();
				molo.getHostModel().notifyChange();
			}
		});
		bgComboBox.getColorMenu().setFillEffectActions(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				FillMode fm = bgComboBox.getColorMenu().getFillEffectChooser().getFillMode();
				if (fm.equals(molo.getFillMode()))
					return;
				molo.setFillMode(fm);
				molo.getHostModel().getView().repaint();
				molo.getHostModel().notifyChange();
			}
		}, null);

		JTabbedPane tabbedPane = new JTabbedPane();
		add(tabbedPane, BorderLayout.CENTER);

		JPanel panel = new JPanel(new SpringLayout());
		panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		// row 1
		panel.add(new JLabel("Object Type"));
		if (molo instanceof CurvedSurface)
			panel.add(createLabel("Curved Surface"));
		else if (molo instanceof CurvedRibbon)
			panel.add(createLabel("Curved Ribbon"));
		else panel.add(createLabel("Unspecified"));
		panel.add(new JPanel());

		// row 2
		panel.add(new JLabel("Index"));
		panel.add(createLabel(mc.indexOf(molo) + ""));
		panel.add(new JPanel());

		// row 3
		panel.add(new JLabel("Background"));
		panel.add(bgComboBox);
		panel.add(new JPanel());

		// row 4
		panel.add(new JLabel("Number of Sites"));
		panel.add(createLabel(molo.size()));
		panel.add(new JPanel());

		// row 5
		panel.add(new JLabel("<html><em>X</em><sub>center of mass</sub></html>"));
		panel.add(createLabel(DECIMAL_FORMAT.format(com.x * 0.1)));
		panel.add(createSmallerFontLabel("angstrom"));

		// row 6
		panel.add(new JLabel("<html><em>Y</em><sub>center of mass</sub></html>"));
		panel.add(createLabel(DECIMAL_FORMAT.format(com.y * 0.1)));
		panel.add(createSmallerFontLabel("angstrom"));

		// row 7
		panel.add(new JLabel("Radius of Gyration"));
		panel.add(createLabel(DECIMAL_FORMAT.format(rg * 0.1)));
		panel.add(createSmallerFontLabel("angstrom"));

		makeCompactGrid(panel, 7, 3, 5, 5, 10, 2);

		JPanel p = new JPanel(new BorderLayout());
		p.add(panel, BorderLayout.NORTH);
		tabbedPane.add("General", p);

		panel = createBondPanel(molo);
		if (panel != null)
			tabbedPane.add("Radial Bonds", panel);

		panel = createBendPanel(molo);
		if (panel != null)
			tabbedPane.add("Angular Bonds", panel);

		panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

		JButton button = new JButton("Close");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				destroy();
			}
		});
		panel.add(button);

		add(panel, BorderLayout.SOUTH);

	}

}