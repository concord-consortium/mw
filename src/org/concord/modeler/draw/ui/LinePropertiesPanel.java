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
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.SystemColor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

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

import org.concord.modeler.draw.AbstractLine;
import org.concord.modeler.draw.ArrowRectangle;
import org.concord.modeler.draw.DefaultLine;
import org.concord.modeler.draw.LineStyle;
import org.concord.modeler.ui.ColorComboBox;
import org.concord.modeler.ui.ColorRectangle;
import org.concord.modeler.ui.ComboBoxRenderer;
import org.concord.modeler.ui.IntegerTextField;

public class LinePropertiesPanel extends PropertiesPanel {

	protected AbstractLine line;

	private ColorComboBox colorComboBox;
	private JComboBox optionComboBox;
	private JComboBox lineStyleComboBox;
	private JComboBox beginStyleComboBox;
	private JComboBox endStyleComboBox;
	private JSpinner thicknessSpinner;
	private AbstractLine savedCopy;

	public LinePropertiesPanel(AbstractLine l) {

		super(new BorderLayout(5, 5));

		if (l == null)
			throw new IllegalArgumentException("input cannot be null");
		localize();
		line = l;
		storeSettings();

		Border margin = BorderFactory.createEmptyBorder(0, 10, 0, 0);

		JPanel p = new JPanel(new GridLayout(4, 4, 5, 5));
		String s = getInternationalText("Line");
		p.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15), BorderFactory
				.createTitledBorder((s != null ? s : "Line"))));
		add(p, BorderLayout.NORTH);

		s = getInternationalText("Color");
		JLabel label = new JLabel((s != null ? s : "Color") + ":", JLabel.LEFT);
		label.setBorder(margin);
		p.add(label);
		colorComboBox = new ColorComboBox(this);
		colorComboBox.setColor(line.getColor());
		colorComboBox.setRenderer(new ComboBoxRenderer.ColorCell(line.getColor()));
		colorComboBox.setToolTipText("Line color");
		colorComboBox.setAction(new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				if (colorComboBox.getSelectedIndex() >= ColorRectangle.COLORS.length + 1) {
					colorComboBox.updateColor(new Runnable() {
						public void run() {
							line.setColor(colorComboBox.getMoreColor());
							line.getComponent().repaint();
						}
					});
				}
				else {
					line.setColor(colorComboBox.getSelectedColor());
					line.getComponent().repaint();
				}
			}
		});
		p.add(colorComboBox);

		s = getInternationalText("Style");
		label = new JLabel((s != null ? s : "Style") + ":", JLabel.LEFT);
		label.setBorder(margin);
		p.add(label);
		optionComboBox = new JComboBox(new String[] { "Default", "Outlined" });
		switch (line.getOption()) {
		case AbstractLine.OUTLINED:
			optionComboBox.setSelectedIndex(1);
			break;
		}
		optionComboBox.setAction(new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				switch (optionComboBox.getSelectedIndex()) {
				case 0:
					line.setOption(AbstractLine.DEFAULT);
					break;
				case 1:
					line.setOption(AbstractLine.OUTLINED);
					break;
				}
				line.getComponent().repaint();
			}
		});
		p.add(optionComboBox);

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
		lineStyleComboBox.setSelectedItem(new Integer(line.getLineStyle()));
		lineStyleComboBox.setAction(new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				line.setLineStyle(((Integer) lineStyleComboBox.getSelectedItem()).byteValue());
				line.getComponent().repaint();
			}
		});
		p.add(lineStyleComboBox);

		s = getInternationalText("Weight");
		label = new JLabel((s != null ? s : "Thickness") + ":", JLabel.LEFT);
		label.setBorder(margin);
		p.add(label);
		thicknessSpinner = new JSpinner(new SpinnerNumberModel(1, 0, 50, 1));
		thicknessSpinner.setValue(new Integer(line.getLineWeight()));
		thicknessSpinner.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				line.setLineWeight(((Integer) thicknessSpinner.getValue()).byteValue());
				line.getComponent().repaint();
			}
		});
		p.add(thicknessSpinner);

		s = getInternationalText("Pixels");
		label = new JLabel("<html>X<sub>1</sub> (" + (s != null ? s : "in pixels") + "):</html>", JLabel.LEFT);
		label.setBorder(margin);
		p.add(label);
		IntegerTextField tf = new IntegerTextField((int) line.getX1(), 0, 10000);
		tf.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				IntegerTextField tf = (IntegerTextField) e.getSource();
				line.setX1(tf.getValue());
				line.getComponent().repaint();
			}
		});
		tf.addFocusListener(new FocusAdapter() {
			public void focusLost(FocusEvent e) {
				IntegerTextField tf = (IntegerTextField) e.getSource();
				line.setX1(tf.getValue());
				line.getComponent().repaint();
			}
		});
		p.add(tf);

		label = new JLabel("<html>Y<sub>1</sub> (" + (s != null ? s : "in pixels") + "):</html>", JLabel.LEFT);
		label.setBorder(margin);
		p.add(label);
		tf = new IntegerTextField((int) line.getY1(), 0, 10000);
		tf.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				IntegerTextField tf = (IntegerTextField) e.getSource();
				line.setY1(tf.getValue());
				line.getComponent().repaint();
			}
		});
		tf.addFocusListener(new FocusAdapter() {
			public void focusLost(FocusEvent e) {
				IntegerTextField tf = (IntegerTextField) e.getSource();
				line.setY1(tf.getValue());
				line.getComponent().repaint();
			}
		});
		p.add(tf);

		label = new JLabel("<html>X<sub>2</sub> (" + (s != null ? s : "in pixels") + "):</html>", JLabel.LEFT);
		label.setBorder(margin);
		p.add(label);
		tf = new IntegerTextField((int) line.getX2(), 0, 10000);
		tf.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				IntegerTextField tf = (IntegerTextField) e.getSource();
				line.setX2(tf.getValue());
				line.getComponent().repaint();
			}
		});
		tf.addFocusListener(new FocusAdapter() {
			public void focusLost(FocusEvent e) {
				IntegerTextField tf = (IntegerTextField) e.getSource();
				line.setX2(tf.getValue());
				line.getComponent().repaint();
			}
		});
		p.add(tf);

		label = new JLabel("<html>Y<sub>2</sub> (" + (s != null ? s : "in pixels") + "):</html>", JLabel.LEFT);
		label.setBorder(margin);
		p.add(label);
		tf = new IntegerTextField((int) line.getY2(), 0, 10000);
		tf.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				IntegerTextField tf = (IntegerTextField) e.getSource();
				line.setY2(tf.getValue());
				line.getComponent().repaint();
			}
		});
		tf.addFocusListener(new FocusAdapter() {
			public void focusLost(FocusEvent e) {
				IntegerTextField tf = (IntegerTextField) e.getSource();
				line.setY2(tf.getValue());
				line.getComponent().repaint();
			}
		});
		p.add(tf);

		p = new JPanel(new GridLayout(2, 4, 5, 5));
		s = getInternationalText("Arrow");
		p.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15), BorderFactory
				.createTitledBorder(s != null ? s : "Arrows")));
		add(p, BorderLayout.CENTER);

		s = getInternationalText("BeginStyle");
		label = new JLabel(s != null ? s : "Begin Style");
		label.setBorder(margin);
		p.add(label);
		beginStyleComboBox = new JComboBox();
		beginStyleComboBox.setRenderer(new ComboBoxRenderer.ArrowCell());
		beginStyleComboBox.addItem(new Byte(ArrowRectangle.NO_ARROW));
		beginStyleComboBox.addItem(new Byte(ArrowRectangle.STYLE1));
		beginStyleComboBox.addItem(new Byte(ArrowRectangle.STYLE2));
		beginStyleComboBox.addItem(new Byte(ArrowRectangle.STYLE3));
		beginStyleComboBox.addItem(new Byte(ArrowRectangle.STYLE4));
		beginStyleComboBox.addItem(new Byte(ArrowRectangle.STYLE5));
		beginStyleComboBox.setSelectedItem(new Byte(line.getBeginStyle()));
		beginStyleComboBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					line.setBeginStyle(((Byte) beginStyleComboBox.getSelectedItem()).byteValue());
					line.getComponent().repaint();
				}
			}
		});
		p.add(beginStyleComboBox);

		s = getInternationalText("EndStyle");
		label = new JLabel(s != null ? s : "End Style");
		label.setBorder(margin);
		p.add(label);
		endStyleComboBox = new JComboBox();
		endStyleComboBox.setRenderer(new ComboBoxRenderer.ArrowCell());
		endStyleComboBox.addItem(new Byte(ArrowRectangle.NO_ARROW));
		endStyleComboBox.addItem(new Byte(ArrowRectangle.STYLE1));
		endStyleComboBox.addItem(new Byte(ArrowRectangle.STYLE2));
		endStyleComboBox.addItem(new Byte(ArrowRectangle.STYLE3));
		endStyleComboBox.addItem(new Byte(ArrowRectangle.STYLE4));
		endStyleComboBox.addItem(new Byte(ArrowRectangle.STYLE5));
		endStyleComboBox.setSelectedItem(new Byte(line.getEndStyle()));
		endStyleComboBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					line.setEndStyle(((Byte) endStyleComboBox.getSelectedItem()).byteValue());
					line.getComponent().repaint();
				}
			}
		});
		p.add(endStyleComboBox);

		s = getInternationalText("BeginSize");
		label = new JLabel(s != null ? s : "Begin Size");
		label.setBorder(margin);
		p.add(label);
		JComboBox cb = new JComboBox();
		cb.setEnabled(false);
		p.add(cb);

		s = getInternationalText("EndSize");
		label = new JLabel(s != null ? s : "End Size");
		label.setBorder(margin);
		p.add(label);
		cb = new JComboBox();
		cb.setEnabled(false);
		p.add(cb);

		p = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		add(p, BorderLayout.SOUTH);

		s = getInternationalText("Line");
		label = new JLabel("  " + (s != null ? s : "Line") + " # " + getIndex() + "  ");
		label.setBackground(SystemColor.controlLtHighlight);
		label.setOpaque(true);
		label.setBorder(BorderFactory.createLineBorder(SystemColor.controlDkShadow));
		p.add(label);

		s = getInternationalText("OK");
		JButton button = new JButton(s != null ? s : "OK");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				notifyChange();
				line.getComponent().repaint();
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
				line.getComponent().repaint();
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
			String s = getInternationalText("LineProperties");
			if (s != null)
				dialog.setTitle(s);
		}
	}

	public void storeSettings() {
		if (savedCopy == null)
			savedCopy = new DefaultLine();
		savedCopy.set(line);
	}

	public void restoreSettings() {
		if (savedCopy == null)
			return;
		line.set(savedCopy);
	}

}
