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

package org.concord.modeler.g2d;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Arrays;
import java.util.Hashtable;

import javax.swing.BorderFactory;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.concord.modeler.ModelerUtilities;
import org.concord.modeler.draw.LineStyle;
import org.concord.modeler.draw.LineSymbols;
import org.concord.modeler.draw.LineWidth;
import org.concord.modeler.ui.ColorComboBox;
import org.concord.modeler.ui.ColorRectangle;
import org.concord.modeler.ui.ComboBoxRenderer;

/**
 * <p>
 * Sets up a dialog box to interact with the <tt>Graph2D</tt> object.
 * </p>
 * 
 * <p>
 * <b>Note:</b> There is obviously a bug in the JSDK 1.3.1. When this dialog boxis launched by a double click, the
 * action listeners associated with the combo boxes of the dialog fire action-performed events, even though they are not
 * touched. This should not happen, however. The fix is to remove the action listeners when the dialog disappears, and
 * put them back immediately after the dialog has been invoked.
 * </p>
 * 
 * @author Qian Xie
 */

public class CurveDialog extends JDialog {

	private final String LINE_THICKNESS = "Line Thickness";
	private final String LINE_STYLES = "Line Styles";
	private final String LINE_COLORS = "Line Colors";
	private final String LINE_SYMBOLS = "Line Symbols";

	private DataSet set;
	private Graph2D graph;
	private Axis xaxis, yaxis;
	private JComboBox[] comboBox = new JComboBox[4];
	private ActionListener[] al = new ActionListener[4];
	private JSlider spacingSlider, sizeSlider;
	private final static Font littleFont = new Font("Arial", Font.PLAIN, 9);

	public CurveDialog(Graph2D g, Axis x, Axis y) {

		super();
		setModal(true);
		setSize(200, 200);
		setTitle("Curve Options");

		graph = g;
		xaxis = x;
		yaxis = y;

		graph.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				if ((e.getModifiers() & MouseEvent.BUTTON1_MASK) != 0 && e.getClickCount() == 2) {
					int ex = e.getX();
					int ey = e.getY();
					set = graph.getSet(ex, ey, xaxis, yaxis);
					if (set == null)
						return;
					recallCurveStates(set);
					setLocation(ex, ey);
					addComboBoxActionListeners();
					pack();
					setVisible(true);
				}
			}
		});

		getContentPane().add(setup(), BorderLayout.CENTER);

		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				removeComboBoxActionListeners();
				graph.repaint();
				dispose();
			}
		});

	}

	public void addComboBoxActionListeners() {
		for (int i = 0; i < 4; i++)
			comboBox[i].addActionListener(al[i]);
	}

	public void removeComboBoxActionListeners() {
		for (int i = 0; i < 4; i++)
			comboBox[i].removeActionListener(al[i]);
	}

	public JPanel setup() {

		JPanel panel = new JPanel(new GridLayout(2, 2));
		panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

		comboBox[0] = new JComboBox();
		comboBox[0].setRenderer(new ComboBoxRenderer.LineThickness());
		comboBox[0].setBorder(BorderFactory.createTitledBorder(LINE_THICKNESS));
		comboBox[0].setName(LINE_THICKNESS);
		comboBox[0].addItem(new Float(LineWidth.STROKE_WIDTH_0));
		comboBox[0].addItem(new Float(LineWidth.STROKE_WIDTH_1));
		comboBox[0].addItem(new Float(LineWidth.STROKE_WIDTH_2));
		comboBox[0].addItem(new Float(LineWidth.STROKE_WIDTH_3));
		comboBox[0].addItem(new Float(LineWidth.STROKE_WIDTH_4));
		comboBox[0].addItem(new Float(LineWidth.STROKE_WIDTH_5));
		al[0] = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JComboBox cb = (JComboBox) e.getSource();
				set.setLineStroke(((Float) cb.getSelectedItem()).floatValue());
				graph.repaint();
			}
		};

		comboBox[1] = new JComboBox();
		comboBox[1].setRenderer(new ComboBoxRenderer.LineStyles());
		comboBox[1].setBorder(BorderFactory.createTitledBorder(LINE_STYLES));
		comboBox[1].setName(LINE_STYLES);
		comboBox[1].addItem(new Integer(LineStyle.STROKE_NUMBER_1));
		comboBox[1].addItem(new Integer(LineStyle.STROKE_NUMBER_2));
		comboBox[1].addItem(new Integer(LineStyle.STROKE_NUMBER_3));
		comboBox[1].addItem(new Integer(LineStyle.STROKE_NUMBER_4));
		comboBox[1].addItem(new Integer(LineStyle.STROKE_NUMBER_5));
		al[1] = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JComboBox cb = (JComboBox) e.getSource();
				set.setLineStroke(((Integer) cb.getSelectedItem()).intValue());
				graph.repaint();
			}
		};

		comboBox[2] = new JComboBox();
		comboBox[2].setRenderer(new ComboBoxRenderer.ColorCell());
		comboBox[2].setBorder(BorderFactory.createTitledBorder(LINE_COLORS));
		comboBox[2].setName(LINE_COLORS);
		for (int i = 0; i <= ColorComboBox.INDEX_MORE_COLOR; i++)
			comboBox[2].addItem(new Integer(i));
		comboBox[2].addItem(new Integer(ColorComboBox.INDEX_COLOR_CHOOSER));
		al[2] = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				final JComboBox cb = (JComboBox) e.getSource();
				int i = (Integer) cb.getSelectedItem();
				if (i >= ColorComboBox.INDEX_COLOR_CHOOSER) {
					JColorChooser.createDialog(CurveDialog.this, "More Line Colors", true,
							ModelerUtilities.colorChooser, new ActionListener() {
								public void actionPerformed(ActionEvent ae) {
									set.setLineColor(ModelerUtilities.colorChooser.getColor());
									cb.setSelectedIndex(ColorComboBox.INDEX_MORE_COLOR);
									ColorRectangle cr = (ColorRectangle) cb.getRenderer();
									cr.setMoreColor(set.getLineColor());
								}
							}, null).setVisible(true);
				}
				else if (i == ColorComboBox.INDEX_MORE_COLOR) {
					ColorRectangle cr = (ColorRectangle) cb.getRenderer();
					set.setLineColor(cr.getMoreColor());
				}
				else {
					set.setLineColor(ColorRectangle.COLORS[i]);
				}
				graph.repaint();
			}
		};

		comboBox[3] = new JComboBox();
		comboBox[3].setRenderer(new ComboBoxRenderer.Symbols());
		comboBox[3].setBorder(BorderFactory.createTitledBorder(LINE_SYMBOLS));
		comboBox[3].setName(LINE_SYMBOLS);
		comboBox[3].addItem(new Integer(LineSymbols.SYMBOL_NUMBER_0));
		comboBox[3].addItem(new Integer(LineSymbols.SYMBOL_NUMBER_1));
		comboBox[3].addItem(new Integer(LineSymbols.SYMBOL_NUMBER_2));
		comboBox[3].addItem(new Integer(LineSymbols.SYMBOL_NUMBER_3));
		comboBox[3].addItem(new Integer(LineSymbols.SYMBOL_NUMBER_4));
		comboBox[3].addItem(new Integer(LineSymbols.SYMBOL_NUMBER_5));
		comboBox[3].addItem(new Integer(LineSymbols.SYMBOL_NUMBER_6));
		comboBox[3].addItem(new Integer(LineSymbols.SYMBOL_NUMBER_7));
		al[3] = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JComboBox cb = (JComboBox) e.getSource();
				set.setSymbol((Integer) cb.getSelectedItem());
				graph.repaint();
			}
		};

		for (int i = 0; i < 4; i++) {
			comboBox[i].setBackground(panel.getBackground());
			panel.add(comboBox[i]);
		}

		JPanel panel2 = new JPanel(new BorderLayout());
		panel2.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		panel2.add(panel, BorderLayout.NORTH);

		Hashtable<Integer, JLabel> tableOfLabels = new Hashtable<Integer, JLabel>();
		JLabel label0 = new JLabel("Dense");
		JLabel label1 = new JLabel("Sparse");
		label0.setFont(littleFont);
		label1.setFont(littleFont);
		tableOfLabels.put(1, label0);
		tableOfLabels.put(10, label1);

		spacingSlider = new JSlider(JSlider.HORIZONTAL, 1, 10, 5);
		spacingSlider.setBorder(BorderFactory.createTitledBorder("Symbol Spacing"));
		spacingSlider.setPaintTicks(true);
		spacingSlider.setPaintLabels(true);
		spacingSlider.setMajorTickSpacing(2);
		spacingSlider.setMinorTickSpacing(1);
		spacingSlider.setLabelTable(tableOfLabels);
		spacingSlider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				if (set == null)
					return;
				JSlider source = (JSlider) e.getSource();
				if (!source.getValueIsAdjusting()) {
					set.setSymbolSpacing(source.getValue());
					graph.repaint();
				}
			}
		});
		panel2.add(spacingSlider, BorderLayout.CENTER);

		tableOfLabels = new Hashtable<Integer, JLabel>();
		label0 = new JLabel("Small");
		label1 = new JLabel("Large");
		label0.setFont(littleFont);
		label1.setFont(littleFont);
		tableOfLabels.put(4, label0);
		tableOfLabels.put(12, label1);

		sizeSlider = new JSlider(JSlider.HORIZONTAL, 4, 12, 6);
		sizeSlider.setBorder(BorderFactory.createTitledBorder("Symbol Size"));
		sizeSlider.setPaintTicks(true);
		sizeSlider.setPaintLabels(true);
		sizeSlider.setMinorTickSpacing(1);
		sizeSlider.setMajorTickSpacing(2);
		sizeSlider.setLabelTable(tableOfLabels);
		sizeSlider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				if (set == null)
					return;
				JSlider source = (JSlider) e.getSource();
				if (!source.getValueIsAdjusting()) {
					set.setSymbolSize(source.getValue());
					graph.repaint();
				}
			}
		});
		panel2.add(sizeSlider, BorderLayout.SOUTH);

		return panel2;

	}

	public void recallCurveStates(DataSet ds) {

		int i, j;

		if (ds.getLineStroke() != null) {

			i = (int) ds.getLineStroke().getLineWidth();
			comboBox[0].setSelectedIndex(i);

			for (j = 0; j < LineStyle.STROKES.length; j++) {
				if (Arrays.equals(ds.getLineStroke().getDashArray(), LineStyle.STROKES[j].getDashArray())) {
					i = j;
					break;
				}
			}
			comboBox[1].setSelectedIndex(i);

		}

		i = 6;
		for (j = 0; j < ColorRectangle.COLORS.length; j++) {
			if (ds.getLineColor() == ColorRectangle.COLORS[j]) {
				i = j;
				break;
			}
		}
		comboBox[2].setSelectedIndex(i);
		if (i == ColorComboBox.INDEX_MORE_COLOR) {
			ColorRectangle cr = (ColorRectangle) comboBox[2].getRenderer();
			cr.setMoreColor(ds.getLineColor().getRed(), ds.getLineColor().getGreen(), ds.getLineColor().getBlue());
		}

		for (j = 0; j < LineSymbols.SYMBOLS.length; j++) {
			if (ds.getSymbol() == LineSymbols.SYMBOLS[j]) {
				i = j;
				break;
			}
		}
		comboBox[3].setSelectedIndex(i);

		spacingSlider.setValue(ds.getSymbolSpacing());
		sizeSlider.setValue(ds.getSymbolSize());

	}

}
