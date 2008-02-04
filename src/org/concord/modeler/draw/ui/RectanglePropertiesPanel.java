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
import org.concord.modeler.draw.AbstractRectangle;
import org.concord.modeler.draw.DefaultRectangle;
import org.concord.modeler.draw.FillMode;
import org.concord.modeler.draw.LineStyle;
import org.concord.modeler.ui.BackgroundComboBox;
import org.concord.modeler.ui.ColorComboBox;
import org.concord.modeler.ui.ColorMenu;
import org.concord.modeler.ui.ColorRectangle;
import org.concord.modeler.ui.ComboBoxRenderer;
import org.concord.modeler.ui.IntegerTextField;

public class RectanglePropertiesPanel extends PropertiesPanel {

	protected AbstractRectangle rect;

	private BackgroundComboBox bgComboBox;
	private ColorComboBox lineColorComboBox;
	private JComboBox lineStyleComboBox;
	private JSpinner lineWeightSpinner;
	private JSpinner alphaSpinner;
	private AbstractRectangle savedCopy;

	public RectanglePropertiesPanel(AbstractRectangle r) {

		super(new BorderLayout(5, 5));

		if (r == null)
			throw new IllegalArgumentException("input cannot be null");
		localize();
		rect = r;
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
		bgComboBox.setFillMode(rect.getFillMode());
		bgComboBox.getColorMenu().setNoFillAction(new AbstractAction("No Fill") {
			public void actionPerformed(ActionEvent e) {
				FillMode fm = FillMode.getNoFillMode();
				if (fm.equals(rect.getFillMode()))
					return;
				rect.setFillMode(fm);
				rect.getComponent().repaint();
				notifyChange();
				bgComboBox.getColorMenu().firePropertyChange(ColorMenu.FILLING, null, fm);
			}
		});
		bgComboBox.getColorMenu().setColorArrayAction(new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				FillMode fm = new FillMode.ColorFill(bgComboBox.getColorMenu().getColor());
				if (fm.equals(rect.getFillMode()))
					return;
				rect.setFillMode(fm);
				rect.getComponent().repaint();
				notifyChange();
			}
		});
		bgComboBox.getColorMenu().setMoreColorAction(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				FillMode fm = new FillMode.ColorFill(bgComboBox.getColorMenu().getColorChooser().getColor());
				if (fm.equals(rect.getFillMode()))
					return;
				rect.setFillMode(fm);
				rect.getComponent().repaint();
				notifyChange();
				bgComboBox.getColorMenu().firePropertyChange(ColorMenu.FILLING, null, fm);
			}
		});
		bgComboBox.getColorMenu().addHexColorListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Color c = bgComboBox.getColorMenu().getHexInputColor(
						rect.getFillMode() instanceof FillMode.ColorFill ? ((FillMode.ColorFill) rect.getFillMode())
								.getColor() : null);
				if (c == null)
					return;
				FillMode fm = new FillMode.ColorFill(c);
				if (fm.equals(rect.getFillMode()))
					return;
				rect.setFillMode(fm);
				rect.getComponent().repaint();
				notifyChange();
				bgComboBox.getColorMenu().firePropertyChange(ColorMenu.FILLING, null, fm);
			}
		});
		bgComboBox.getColorMenu().setFillEffectActions(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				FillMode fm = bgComboBox.getColorMenu().getFillEffectChooser().getFillMode();
				if (fm.equals(rect.getFillMode()))
					return;
				rect.setFillMode(fm);
				rect.getComponent().repaint();
				notifyChange();
				bgComboBox.getColorMenu().firePropertyChange(ColorMenu.FILLING, null, fm);
			}
		}, null);
		p.add(bgComboBox);

		s = getInternationalText("Transparency");
		label = new JLabel((s != null ? s : "Transparency") + " (alpha):", JLabel.LEFT);
		label.setBorder(margin);
		p.add(label);
		SpinnerNumberModel snm = new SpinnerNumberModel(rect.getAlpha(), 0, 255, 5);
		alphaSpinner = new JSpinner(snm);
		alphaSpinner.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				rect.setAlpha(((Integer) alphaSpinner.getValue()).shortValue());
				rect.getComponent().repaint();
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
		lineColorComboBox.setColor(rect.getLineColor());
		lineColorComboBox.setRenderer(new ComboBoxRenderer.ColorCell(rect.getLineColor()));
		lineColorComboBox.setToolTipText("Fill color");
		lineColorComboBox.setAction(new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				if (lineColorComboBox.getSelectedIndex() >= ColorRectangle.COLORS.length + 1) {
					lineColorComboBox.updateColor(new Runnable() {
						public void run() {
							rect.setLineColor(lineColorComboBox.getMoreColor());
							rect.getComponent().repaint();
						}
					});
				}
				else {
					rect.setLineColor(lineColorComboBox.getSelectedColor());
					rect.getComponent().repaint();
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
		lineStyleComboBox.setSelectedItem(new Integer(rect.getLineStyle()));
		lineStyleComboBox.setAction(new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				rect.setLineStyle(((Integer) lineStyleComboBox.getSelectedItem()).byteValue());
				rect.getComponent().repaint();
			}
		});
		p.add(lineStyleComboBox);

		s = getInternationalText("Weight");
		label = new JLabel((s != null ? s : "Thickness") + ":", JLabel.LEFT);
		label.setBorder(margin);
		p.add(label);
		lineWeightSpinner = new JSpinner(new SpinnerNumberModel(1, 0, 50, 1));
		lineWeightSpinner.setValue(new Integer(rect.getLineWeight()));
		lineWeightSpinner.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				rect.setLineWeight(((Integer) lineWeightSpinner.getValue()).byteValue());
				rect.getComponent().repaint();
			}
		});
		p.add(lineWeightSpinner);

		p = new JPanel(new GridLayout(4, 4, 5, 5));
		s = getInternationalText("PositionAndSize");
		p.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15), BorderFactory
				.createTitledBorder(s != null ? s : "Position and Size")));
		panel.add(p, BorderLayout.SOUTH);

		s = getInternationalText("Pixels");
		label = new JLabel("X (" + (s != null ? s : "pixels") + ")", JLabel.LEFT);
		label.setBorder(margin);
		p.add(label);
		IntegerTextField tf = new IntegerTextField((int) rect.getX(), 0, 1000);
		tf.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				IntegerTextField tf = (IntegerTextField) e.getSource();
				rect.setX(tf.getValue());
				rect.getComponent().repaint();
			}
		});
		tf.addFocusListener(new FocusAdapter() {
			public void focusLost(FocusEvent e) {
				IntegerTextField tf = (IntegerTextField) e.getSource();
				rect.setX(tf.getValue());
				rect.getComponent().repaint();
			}
		});
		p.add(tf);

		label = new JLabel("Y (" + (s != null ? s : "pixels") + ")", JLabel.LEFT);
		label.setBorder(margin);
		p.add(label);
		tf = new IntegerTextField((int) rect.getY(), 0, 1000);
		tf.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				IntegerTextField tf = (IntegerTextField) e.getSource();
				rect.setY(tf.getValue());
				rect.getComponent().repaint();
			}
		});
		tf.addFocusListener(new FocusAdapter() {
			public void focusLost(FocusEvent e) {
				IntegerTextField tf = (IntegerTextField) e.getSource();
				rect.setY(tf.getValue());
				rect.getComponent().repaint();
			}
		});
		p.add(tf);

		String s1 = getInternationalText("Width");
		label = new JLabel((s1 != null ? s1 : "Width") + " (" + (s != null ? s : "pixels") + ")", JLabel.LEFT);
		label.setBorder(margin);
		p.add(label);
		tf = new IntegerTextField(Math.round(rect.getWidth()), 1, 1000);
		tf.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				IntegerTextField tf = (IntegerTextField) e.getSource();
				rect.setWidth(tf.getValue());
				rect.getComponent().repaint();
			}
		});
		tf.addFocusListener(new FocusAdapter() {
			public void focusLost(FocusEvent e) {
				IntegerTextField tf = (IntegerTextField) e.getSource();
				rect.setWidth(tf.getValue());
				rect.getComponent().repaint();
			}
		});
		p.add(tf);

		s1 = getInternationalText("Height");
		label = new JLabel((s1 != null ? s1 : "Height") + " (" + (s != null ? s : "pixels") + ")", JLabel.LEFT);
		label.setBorder(margin);
		p.add(label);
		tf = new IntegerTextField(Math.round(rect.getHeight()), 1, 1000);
		tf.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				IntegerTextField tf = (IntegerTextField) e.getSource();
				rect.setHeight(tf.getValue());
				rect.getComponent().repaint();
			}
		});
		tf.addFocusListener(new FocusAdapter() {
			public void focusLost(FocusEvent e) {
				IntegerTextField tf = (IntegerTextField) e.getSource();
				rect.setHeight(tf.getValue());
				rect.getComponent().repaint();
			}
		});
		p.add(tf);

		s1 = getInternationalText("ArcWidth");
		label = new JLabel((s1 != null ? s1 : "Arc Width") + " (" + (s != null ? s : "pixels") + ")", JLabel.LEFT);
		label.setBorder(margin);
		p.add(label);
		tf = new IntegerTextField(Math.round(rect.getArcWidth()), 0, 500);
		tf.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				IntegerTextField tf = (IntegerTextField) e.getSource();
				rect.setArcWidth(tf.getValue());
				rect.getComponent().repaint();
			}
		});
		tf.addFocusListener(new FocusAdapter() {
			public void focusLost(FocusEvent e) {
				IntegerTextField tf = (IntegerTextField) e.getSource();
				rect.setArcWidth(tf.getValue());
				rect.getComponent().repaint();
			}
		});
		p.add(tf);

		s1 = getInternationalText("ArcHeight");
		label = new JLabel((s1 != null ? s1 : "Arc Height") + " (" + (s != null ? s : "pixels") + ")", JLabel.LEFT);
		label.setBorder(margin);
		p.add(label);
		tf = new IntegerTextField(Math.round(rect.getArcHeight()), 0, 500);
		tf.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				IntegerTextField tf = (IntegerTextField) e.getSource();
				rect.setArcHeight(tf.getValue());
				rect.getComponent().repaint();
			}
		});
		tf.addFocusListener(new FocusAdapter() {
			public void focusLost(FocusEvent e) {
				IntegerTextField tf = (IntegerTextField) e.getSource();
				rect.setArcHeight(tf.getValue());
				rect.getComponent().repaint();
			}
		});
		p.add(tf);

		s1 = getInternationalText("Rotation");
		label = new JLabel("<html>" + (s1 != null ? s1 : "Rotation") + " (&deg;)</html>", JLabel.LEFT);
		label.setBorder(margin);
		p.add(label);
		final JSpinner spinner = new JSpinner(new SpinnerNumberModel(0, -180, 180, 1));
		spinner.setValue((int) rect.getAngle());
		spinner.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				rect.setAngle((Integer) spinner.getValue());
				rect.getComponent().repaint();
			}
		});
		spinner.addFocusListener(new FocusAdapter() {
			public void focusLost(FocusEvent e) {
				rect.setAngle((Integer) spinner.getValue());
				rect.getComponent().repaint();
			}
		});
		p.add(spinner);

		p = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		add(p, BorderLayout.SOUTH);

		s = getInternationalText("Rectangle");
		label = new JLabel("  " + (s != null ? s : "Rectangle") + " # " + getIndex() + "  ");
		label.setBackground(SystemColor.controlLtHighlight);
		label.setOpaque(true);
		label.setBorder(BorderFactory.createLineBorder(SystemColor.controlDkShadow));
		p.add(label);

		s = getInternationalText("OK");
		JButton button = new JButton(s != null ? s : "OK");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				notifyChange();
				rect.getComponent().repaint();
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
				rect.getComponent().repaint();
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
			String s = getInternationalText("RectangleProperties");
			if (s != null)
				dialog.setTitle(s);
		}
	}

	public void storeSettings() {
		if (savedCopy == null)
			savedCopy = new DefaultRectangle();
		savedCopy.set(rect);
	}

	public void restoreSettings() {
		if (savedCopy == null)
			return;
		rect.set(savedCopy);
		if (rect.isSelected())
			rect.setSelected(true); // reset the handles.
	}

}
