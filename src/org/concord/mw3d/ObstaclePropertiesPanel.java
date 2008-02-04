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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SpringLayout;
import javax.vecmath.Point3f;

import org.concord.modeler.ModelerUtilities;
import org.concord.modeler.process.Executable;
import org.concord.modeler.ui.ColorComboBox;
import org.concord.modeler.ui.ColorRectangle;
import org.concord.modeler.ui.ComboBoxRenderer;
import org.concord.modeler.ui.FloatNumberTextField;
import org.concord.mw3d.models.CuboidObstacle;
import org.concord.mw3d.models.CylinderObstacle;
import org.concord.mw3d.models.Obstacle;

class ObstaclePropertiesPanel extends PropertiesPanel {

	private JDialog dialog;

	void destroy() {
		if (dialog != null)
			dialog.dispose();
	}

	ObstaclePropertiesPanel(final Obstacle obs) {

		super(new BorderLayout(5, 5));

		final JLabel indexLabel = createLabel(obs.getModel().indexOfObstacle(obs));

		String type = "Cuboid";
		if (obs instanceof CylinderObstacle)
			type = "Cylinder";
		final JLabel typeLabel = createLabel(type);

		final ColorComboBox colorComboBox = new ColorComboBox(obs.getModel().getView());
		colorComboBox.setRenderer(new ComboBoxRenderer.ColorCell(obs.getColor()));
		colorComboBox.setPreferredSize(new Dimension(40, 20));
		setColorComboBox(colorComboBox, obs.getColor());
		colorComboBox.addActionListener(new ColorListener(obs));

		final FloatNumberTextField rxField = new FloatNumberTextField(obs.getCenter().x, -100, 100, 10);
		final FloatNumberTextField ryField = new FloatNumberTextField(obs.getCenter().y, -100, 100, 10);
		final FloatNumberTextField rzField = new FloatNumberTextField(obs.getCenter().z, -100, 100, 10);

		Point3f corner = new Point3f();

		if (obs instanceof CuboidObstacle) {
			corner.set(((CuboidObstacle) obs).getCorner());
			corner.scale(2);
		}
		else if (obs instanceof CylinderObstacle) {
			corner.y = corner.x = 2 * ((CylinderObstacle) obs).getRadius();
			corner.z = ((CylinderObstacle) obs).getHeight();
		}

		final FloatNumberTextField lxField = new FloatNumberTextField(corner.x, 1, 100, 10);
		final FloatNumberTextField lyField = new FloatNumberTextField(corner.y, 1, 100, 10);
		final FloatNumberTextField lzField = new FloatNumberTextField(corner.z, 1, 100, 10);

		JButton okButton = new JButton();

		String s = MolecularContainer.getInternationalText("Cancel");
		JButton cancelButton = new JButton(s != null ? s : "Cancel");
		cancelButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				destroy();
			}
		});

		/* OK listener */

		Action okAction = new ModelAction(obs.getModel(), new Executable() {

			public void execute() {

				boolean changed = false;

				if (Math.abs(obs.getCenter().x - rxField.getValue()) > ZERO) {
					setRx(obs, rxField.getValue());
					changed = true;
				}
				if (Math.abs(obs.getCenter().y - ryField.getValue()) > ZERO) {
					setRy(obs, ryField.getValue());
					changed = true;
				}
				if (Math.abs(obs.getCenter().z - rzField.getValue()) > ZERO) {
					setRz(obs, rzField.getValue());
					changed = true;
				}

				if (changed) {
					obs.getModel().getView().repaint();
					obs.getModel().notifyChange();
				}
				destroy();

			}
		}) {
		};

		rxField.setAction(okAction);
		ryField.setAction(okAction);
		rzField.setAction(okAction);
		lxField.setAction(okAction);
		lyField.setAction(okAction);
		lzField.setAction(okAction);
		okButton.setAction(okAction);
		s = MolecularContainer.getInternationalText("OK");
		okButton.setText(s != null ? s : "OK");

		/* layout components */

		JPanel panel = new JPanel(new SpringLayout());
		panel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createRaisedBevelBorder(), BorderFactory
				.createEmptyBorder(10, 10, 10, 10)));
		add(panel, BorderLayout.CENTER);

		// row 1
		s = MolecularContainer.getInternationalText("TypeLabel");
		panel.add(new JLabel(s != null ? s : "Type"));
		panel.add(typeLabel);
		panel.add(new JPanel());

		// row 2
		s = MolecularContainer.getInternationalText("IndexLabel");
		panel.add(new JLabel(s != null ? s : "Index"));
		panel.add(indexLabel);
		panel.add(new JPanel());

		// row 3
		s = MolecularContainer.getInternationalText("Color");
		panel.add(new JLabel(s != null ? s : "Color"));
		panel.add(colorComboBox);
		panel.add(new JPanel());

		// row 4
		panel.add(new JLabel("X"));
		panel.add(rxField);
		panel.add(createSmallerFontLabel("<html>&#197;</html>"));

		// row 5
		panel.add(new JLabel("Y"));
		panel.add(ryField);
		panel.add(createSmallerFontLabel("<html>&#197;</html>"));

		// row 6
		panel.add(new JLabel("Z"));
		panel.add(rzField);
		panel.add(createSmallerFontLabel("<html>&#197;</html>"));

		// row 7
		panel.add(new JLabel("Length"));
		panel.add(lxField);
		panel.add(createSmallerFontLabel("<html>&#197;</html>"));

		// row 8
		panel.add(new JLabel("Width"));
		panel.add(lyField);
		panel.add(createSmallerFontLabel("<html>&#197;</html>"));

		// row 9
		panel.add(new JLabel("Height"));
		panel.add(lzField);
		panel.add(createSmallerFontLabel("<html>&#197;</html>"));

		makeCompactGrid(panel, 9, 3, 5, 5, 10, 2);

		panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		add(panel, BorderLayout.SOUTH);

		panel.add(okButton);
		panel.add(cancelButton);

	}

	private void setRx(Obstacle obs, float x) {
		obs.getCenter().x = x;
		updateObstaclePosition(obs);
	}

	private void setRy(Obstacle obs, float y) {
		obs.getCenter().y = y;
		updateObstaclePosition(obs);
	}

	private void setRz(Obstacle obs, float z) {
		obs.getCenter().z = z;
		updateObstaclePosition(obs);
	}

	private void updateObstaclePosition(Obstacle obs) {
		// MolecularView v = obs.getModel().getView();
		// JmolViewer viewer = v.getViewer();
		// viewer.setAtomCoordinates(a.getModel().getAtomIndex(a), a.getRx(), a.getRy(), a.getRz());
		// v.repaint();
	}

	void setDialog(JDialog d) {
		dialog = d;
	}

	void windowActivated() {
	}

	private static class ColorListener implements ActionListener {

		private Color color6 = Color.white;
		private Obstacle obs;
		private MolecularView view;

		ColorListener(Obstacle obs) {
			this.obs = obs;
			view = (MolecularView) obs.getModel().getView();
		}

		public void actionPerformed(ActionEvent e) {
			final JComboBox cb = (JComboBox) e.getSource();
			int id = ((Integer) cb.getSelectedItem()).intValue();
			if (id == ColorComboBox.INDEX_COLOR_CHOOSER) {
				String s = MolecularContainer.getInternationalText("MoreColors");
				JColorChooser.createDialog(view, s != null ? s : "More Colors", true, ModelerUtilities.colorChooser,
						new ActionListener() {
							public void actionPerformed(ActionEvent ae) {
								color6 = ModelerUtilities.colorChooser.getColor();
								view.setObstacleColor(obs, color6, obs.isTranslucent());
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
							view.setObstacleColor(obs, colorComboBox.getMoreColor(), obs.isTranslucent());
						}
					});
				}
			}
			else if (id == ColorComboBox.INDEX_MORE_COLOR) {
				view.setObstacleColor(obs, color6, obs.isTranslucent());
			}
			else {
				view.setObstacleColor(obs, ColorRectangle.COLORS[id], obs.isTranslucent());
			}
		}

	}

}