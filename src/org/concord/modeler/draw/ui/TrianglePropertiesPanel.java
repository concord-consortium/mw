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

package org.concord.modeler.draw.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.SystemColor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.geom.Point2D;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.concord.modeler.ModelerUtilities;
import org.concord.modeler.draw.AbstractTriangle;
import org.concord.modeler.draw.DefaultTriangle;
import org.concord.modeler.draw.FillMode;
import org.concord.modeler.draw.LineStyle;
import org.concord.modeler.ui.BackgroundComboBox;
import org.concord.modeler.ui.ColorComboBox;
import org.concord.modeler.ui.ColorMenu;
import org.concord.modeler.ui.ColorRectangle;
import org.concord.modeler.ui.ComboBoxRenderer;
import org.concord.modeler.ui.IntegerTextField;

public class TrianglePropertiesPanel extends PropertiesPanel {

	protected AbstractTriangle triangle;

	private BackgroundComboBox bgComboBox;
	private ColorComboBox lineColorComboBox;
	private JComboBox lineStyleComboBox;
	private JSpinner lineWeightSpinner;
	private JSpinner alphaSpinner;
	private AbstractTriangle savedCopy;

	public TrianglePropertiesPanel(AbstractTriangle r) {

		super(new BorderLayout(5, 5));

		if (r == null)
			throw new IllegalArgumentException("input cannot be null");
		localize();
		triangle = r;
		storeSettings();

		JPanel panel = new JPanel(new BorderLayout());
		add(panel, BorderLayout.CENTER);

		Border margin = BorderFactory.createEmptyBorder(0, 10, 0, 0);

		JPanel p = new JPanel(new GridLayout(1, 4, 5, 5));
		String s = getInternationalText("Fill");
		p.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15), BorderFactory
				.createTitledBorder((s != null ? s : "Fill"))));
		panel.add(p, BorderLayout.NORTH);

		s = getInternationalText("Color");
		JLabel label = new JLabel((s != null ? s : "Color") + ":", JLabel.LEFT);
		label.setBorder(margin);
		p.add(label);
		bgComboBox = new BackgroundComboBox(this, ModelerUtilities.colorChooser, ModelerUtilities.fillEffectChooser);
		bgComboBox.setToolTipText("Background filling");
		bgComboBox.setFillMode(triangle.getFillMode());
		bgComboBox.getColorMenu().setNoFillAction(new AbstractAction("No Fill") {
			public void actionPerformed(ActionEvent e) {
				FillMode fm = FillMode.getNoFillMode();
				if (fm.equals(triangle.getFillMode()))
					return;
				triangle.setFillMode(fm);
				triangle.getComponent().repaint();
				notifyChange();
				bgComboBox.getColorMenu().firePropertyChange(ColorMenu.FILLING, null, fm);
			}
		});
		bgComboBox.getColorMenu().setColorArrayAction(new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				FillMode fm = new FillMode.ColorFill(bgComboBox.getColorMenu().getColor());
				if (fm.equals(triangle.getFillMode()))
					return;
				triangle.setFillMode(fm);
				triangle.getComponent().repaint();
				notifyChange();
			}
		});
		bgComboBox.getColorMenu().setMoreColorAction(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				FillMode fm = new FillMode.ColorFill(bgComboBox.getColorMenu().getColorChooser().getColor());
				if (fm.equals(triangle.getFillMode()))
					return;
				triangle.setFillMode(fm);
				triangle.getComponent().repaint();
				notifyChange();
				bgComboBox.getColorMenu().firePropertyChange(ColorMenu.FILLING, null, fm);
			}
		});
		bgComboBox.getColorMenu().addHexColorListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Color c = bgComboBox.getColorMenu().getHexInputColor(
						triangle.getFillMode() instanceof FillMode.ColorFill ? ((FillMode.ColorFill) triangle
								.getFillMode()).getColor() : null);
				if (c == null)
					return;
				FillMode fm = new FillMode.ColorFill(c);
				if (fm.equals(triangle.getFillMode()))
					return;
				triangle.setFillMode(fm);
				triangle.getComponent().repaint();
				notifyChange();
				bgComboBox.getColorMenu().firePropertyChange(ColorMenu.FILLING, null, fm);
			}
		});
		bgComboBox.getColorMenu().setFillEffectActions(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				FillMode fm = bgComboBox.getColorMenu().getFillEffectChooser().getFillMode();
				if (fm.equals(triangle.getFillMode()))
					return;
				triangle.setFillMode(fm);
				triangle.getComponent().repaint();
				notifyChange();
				bgComboBox.getColorMenu().firePropertyChange(ColorMenu.FILLING, null, fm);
			}
		}, null);
		p.add(bgComboBox);

		s = getInternationalText("Transparency");
		label = new JLabel((s != null ? s : "Transparency") + " (alpha):", JLabel.LEFT);
		label.setBorder(margin);
		p.add(label);
		SpinnerNumberModel snm = new SpinnerNumberModel(triangle.getAlpha(), 0, 255, 5);
		alphaSpinner = new JSpinner(snm);
		alphaSpinner.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				triangle.setAlpha(((Integer) alphaSpinner.getValue()).shortValue());
				triangle.getComponent().repaint();
			}
		});
		p.add(alphaSpinner);

		p = new JPanel(new GridLayout(2, 4, 5, 5));
		s = getInternationalText("Line");
		p.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15), BorderFactory
				.createTitledBorder((s != null ? s : "Line"))));
		panel.add(p, BorderLayout.CENTER);

		s = getInternationalText("Color");
		label = new JLabel((s != null ? s : "Color") + ":", JLabel.LEFT);
		label.setBorder(margin);
		p.add(label);
		lineColorComboBox = new ColorComboBox(this);
		lineColorComboBox.setColor(triangle.getLineColor());
		lineColorComboBox.setRenderer(new ComboBoxRenderer.ColorCell(triangle.getLineColor()));
		lineColorComboBox.setToolTipText("Fill color");
		lineColorComboBox.setAction(new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				if (lineColorComboBox.getSelectedIndex() >= ColorRectangle.COLORS.length + 1) {
					lineColorComboBox.updateColor(new Runnable() {
						public void run() {
							triangle.setLineColor(lineColorComboBox.getMoreColor());
							triangle.getComponent().repaint();
						}
					});
				}
				else {
					triangle.setLineColor(lineColorComboBox.getSelectedColor());
					triangle.getComponent().repaint();
				}
			}
		});
		p.add(lineColorComboBox);

		s = getInternationalText("Style");
		label = new JLabel((s != null ? s : "Style") + ":", JLabel.LEFT);
		label.setBorder(margin);
		p.add(label);
		JComboBox cb = new JComboBox();
		cb.setEnabled(false);
		p.add(cb);

		s = getInternationalText("Dashed");
		label = new JLabel((s != null ? s : "Dashed") + ":", JLabel.LEFT);
		label.setBorder(margin);
		p.add(label);
		lineStyleComboBox = new JComboBox();
		lineStyleComboBox.setToolTipText("Select the line style");
		lineStyleComboBox.setRenderer(new ComboBoxRenderer.LineStyles());
		lineStyleComboBox.addItem(new Integer(LineStyle.STROKE_NUMBER_1));
		lineStyleComboBox.addItem(new Integer(LineStyle.STROKE_NUMBER_2));
		lineStyleComboBox.addItem(new Integer(LineStyle.STROKE_NUMBER_3));
		lineStyleComboBox.addItem(new Integer(LineStyle.STROKE_NUMBER_4));
		lineStyleComboBox.addItem(new Integer(LineStyle.STROKE_NUMBER_5));
		lineStyleComboBox.setSelectedItem(new Integer(triangle.getLineStyle()));
		lineStyleComboBox.setAction(new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				triangle.setLineStyle(((Integer) lineStyleComboBox.getSelectedItem()).byteValue());
				triangle.getComponent().repaint();
			}
		});
		p.add(lineStyleComboBox);

		s = getInternationalText("Weight");
		label = new JLabel((s != null ? s : "Thickness") + ":", JLabel.LEFT);
		label.setBorder(margin);
		p.add(label);
		lineWeightSpinner = new JSpinner(new SpinnerNumberModel(1, 0, 50, 1));
		lineWeightSpinner.setValue(new Integer(triangle.getLineWeight()));
		lineWeightSpinner.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				triangle.setLineWeight(((Integer) lineWeightSpinner.getValue()).byteValue());
				triangle.getComponent().repaint();
			}
		});
		p.add(lineWeightSpinner);

		p = new JPanel(new GridLayout(4, 4, 5, 5));
		s = getInternationalText("PositionAndSize");
		p.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15), BorderFactory
				.createTitledBorder(s != null ? s : "Position and Size")));
		panel.add(p, BorderLayout.SOUTH);

		s = getInternationalText("Pixels");
		label = new JLabel("X1 (" + (s != null ? s : "pixels") + ")", JLabel.LEFT);
		label.setBorder(margin);
		p.add(label);
		final Point2D.Float vertexA = triangle.getVertex(0);
		IntegerTextField tf = new IntegerTextField((int) vertexA.x, 0, 1000);
		tf.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				IntegerTextField tf = (IntegerTextField) e.getSource();
				vertexA.x = tf.getValue();
				triangle.setSelected(true);
				triangle.getComponent().repaint();
			}
		});
		tf.addFocusListener(new FocusAdapter() {
			public void focusLost(FocusEvent e) {
				IntegerTextField tf = (IntegerTextField) e.getSource();
				vertexA.x = tf.getValue();
				triangle.setSelected(true);
				triangle.getComponent().repaint();
			}
		});
		p.add(tf);

		label = new JLabel("Y1 (" + (s != null ? s : "pixels") + ")", JLabel.LEFT);
		label.setBorder(margin);
		p.add(label);
		tf = new IntegerTextField((int) vertexA.y, 0, 1000);
		tf.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				IntegerTextField tf = (IntegerTextField) e.getSource();
				vertexA.y = tf.getValue();
				triangle.setSelected(true);
				triangle.getComponent().repaint();
			}
		});
		tf.addFocusListener(new FocusAdapter() {
			public void focusLost(FocusEvent e) {
				IntegerTextField tf = (IntegerTextField) e.getSource();
				vertexA.y = tf.getValue();
				triangle.setSelected(true);
				triangle.getComponent().repaint();
			}
		});
		p.add(tf);

		s = getInternationalText("Pixels");
		label = new JLabel("X2 (" + (s != null ? s : "pixels") + ")", JLabel.LEFT);
		label.setBorder(margin);
		p.add(label);
		final Point2D.Float vertexB = triangle.getVertex(1);
		tf = new IntegerTextField((int) vertexB.x, 0, 1000);
		tf.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				IntegerTextField tf = (IntegerTextField) e.getSource();
				vertexB.x = tf.getValue();
				triangle.setSelected(true);
				triangle.getComponent().repaint();
			}
		});
		tf.addFocusListener(new FocusAdapter() {
			public void focusLost(FocusEvent e) {
				IntegerTextField tf = (IntegerTextField) e.getSource();
				vertexB.x = tf.getValue();
				triangle.setSelected(true);
				triangle.getComponent().repaint();
			}
		});
		p.add(tf);

		label = new JLabel("Y2 (" + (s != null ? s : "pixels") + ")", JLabel.LEFT);
		label.setBorder(margin);
		p.add(label);
		tf = new IntegerTextField((int) vertexB.y, 0, 1000);
		tf.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				IntegerTextField tf = (IntegerTextField) e.getSource();
				vertexB.y = tf.getValue();
				triangle.setSelected(true);
				triangle.getComponent().repaint();
			}
		});
		tf.addFocusListener(new FocusAdapter() {
			public void focusLost(FocusEvent e) {
				IntegerTextField tf = (IntegerTextField) e.getSource();
				vertexB.y = tf.getValue();
				triangle.setSelected(true);
				triangle.getComponent().repaint();
			}
		});
		p.add(tf);

		s = getInternationalText("Pixels");
		label = new JLabel("X3 (" + (s != null ? s : "pixels") + ")", JLabel.LEFT);
		label.setBorder(margin);
		p.add(label);
		final Point2D.Float vertexC = triangle.getVertex(2);
		tf = new IntegerTextField((int) vertexC.x, 0, 1000);
		tf.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				IntegerTextField tf = (IntegerTextField) e.getSource();
				vertexC.x = tf.getValue();
				triangle.setSelected(true);
				triangle.getComponent().repaint();
			}
		});
		tf.addFocusListener(new FocusAdapter() {
			public void focusLost(FocusEvent e) {
				IntegerTextField tf = (IntegerTextField) e.getSource();
				vertexC.x = tf.getValue();
				triangle.setSelected(true);
				triangle.getComponent().repaint();
			}
		});
		p.add(tf);

		label = new JLabel("Y3 (" + (s != null ? s : "pixels") + ")", JLabel.LEFT);
		label.setBorder(margin);
		p.add(label);
		tf = new IntegerTextField((int) vertexC.y, 0, 1000);
		tf.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				IntegerTextField tf = (IntegerTextField) e.getSource();
				vertexC.y = tf.getValue();
				triangle.setSelected(true);
				triangle.getComponent().repaint();
			}
		});
		tf.addFocusListener(new FocusAdapter() {
			public void focusLost(FocusEvent e) {
				IntegerTextField tf = (IntegerTextField) e.getSource();
				vertexC.y = tf.getValue();
				triangle.setSelected(true);
				triangle.getComponent().repaint();
			}
		});
		p.add(tf);

		s = getInternationalText("Rotation");
		label = new JLabel("<html>" + (s != null ? s : "Rotation") + " (&deg;)</html>", JLabel.LEFT);
		label.setBorder(margin);
		p.add(label);
		final JSpinner spinner = new JSpinner(new SpinnerNumberModel(0, -180, 180, 1));
		spinner.setValue((int) triangle.getAngle());
		spinner.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				triangle.setAngle((Integer) spinner.getValue());
				triangle.getComponent().repaint();
			}
		});
		spinner.addFocusListener(new FocusAdapter() {
			public void focusLost(FocusEvent e) {
				triangle.setAngle((Integer) spinner.getValue());
				triangle.getComponent().repaint();
			}
		});
		p.add(spinner);

		p = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		add(p, BorderLayout.SOUTH);

		s = getInternationalText("Triangle");
		label = new JLabel("  " + (s != null ? s : "Triangle") + " # " + getIndex() + "  ");
		label.setBackground(SystemColor.controlLtHighlight);
		label.setOpaque(true);
		label.setBorder(BorderFactory.createLineBorder(SystemColor.controlDkShadow));
		p.add(label);

		s = getInternationalText("OK");
		JButton button = new JButton(s != null ? s : "OK");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				notifyChange();
				triangle.getComponent().repaint();
				if (dialog != null) {
					offset = dialog.getLocationOnScreen();
					dialog.dispose();
				}
			}
		});
		p.add(button);

		s = getInternationalText("Cancel");
		button = new JButton(s != null ? s : "Cancel");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				restoreSettings();
				triangle.getComponent().repaint();
				cancelled = true;
				if (dialog != null) {
					offset = dialog.getLocationOnScreen();
					dialog.dispose();
				}
			}
		});
		p.add(button);

	}

	public void setDialog(JDialog d) {
		super.setDialog(d);
		if (dialog != null) {
			String s = getInternationalText("TriangleProperties");
			if (s != null)
				dialog.setTitle(s);
		}
	}

	public void storeSettings() {
		if (savedCopy == null)
			savedCopy = new DefaultTriangle();
		savedCopy.set(triangle);
	}

	public void restoreSettings() {
		if (savedCopy == null)
			return;
		triangle.set(savedCopy);
		if (triangle.isSelected())
			triangle.setSelected(true); // reset the handles.
	}

}
