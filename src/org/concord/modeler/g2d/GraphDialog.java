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
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Iterator;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;

import org.concord.modeler.ui.FontChooser;

public class GraphDialog extends JDialog {

	private Graph2D graph2D;
	private FontChooser legendFontChooser, labelFontChooser;
	private JTextField tfx, tfy;

	public GraphDialog(Graph2D g) {

		super();
		setModal(true);
		setSize(200, 200);
		String s = XYGrapher.getInternationalText("GraphSettings");
		setTitle(s != null ? s : "Graph Settings");

		graph2D = g;

		JTabbedPane tabbedPane = new JTabbedPane();
		getContentPane().add(tabbedPane, BorderLayout.CENTER);

		/* legend options */
		JPanel panel = new JPanel(new BorderLayout());

		legendFontChooser = new FontChooser();
		legendFontChooser.enableUnderline(false);
		legendFontChooser.enableStrikethrough(false);
		legendFontChooser.enableSubscript(false);
		legendFontChooser.enableSuperscript(false);
		legendFontChooser.enableFontColor(false);
		panel.add(legendFontChooser, BorderLayout.CENTER);

		JPanel p = new JPanel(new GridLayout(1, 4, 5, 5));

		JLabel label = new JLabel("  X (in pixels)");
		p.add(label);

		tfx = new JTextField();
		try {
			tfx.setText(Integer.toString(graph2D.getLegendLocation().x));
		}
		catch (NullPointerException e) {
			tfx.setText("Not set");
		}
		p.add(tfx);

		label = new JLabel(" Y (in pixels)");
		p.add(label);

		tfy = new JTextField();
		try {
			tfy.setText(Integer.toString(graph2D.getLegendLocation().y));
		}
		catch (NullPointerException e) {
			tfy.setText("Not set");
		}
		p.add(tfy);

		panel.add(p, BorderLayout.SOUTH);

		s = XYGrapher.getInternationalText("Legend");
		tabbedPane.addTab(s != null ? s : "Legends", panel);

		/* label options */
		panel = new JPanel(new BorderLayout());

		labelFontChooser = new FontChooser();
		labelFontChooser.enableUnderline(false);
		labelFontChooser.enableStrikethrough(false);
		labelFontChooser.enableSubscript(false);
		labelFontChooser.enableSuperscript(false);
		labelFontChooser.enableFontColor(false);
		panel.add(labelFontChooser, BorderLayout.NORTH);

		s = XYGrapher.getInternationalText("AxisLabel");
		tabbedPane.addTab(s != null ? s : "Axis Labels", panel);

		/* button panel */
		panel = new JPanel();

		s = XYGrapher.getInternationalText("OK");
		JButton b = new JButton(s != null ? s : "OK");
		b.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				confirm();
				graph2D.repaint();
				dispose();
			}
		});
		panel.add(b);

		s = XYGrapher.getInternationalText("Cancel");
		b = new JButton(s != null ? s : "Cancel");
		b.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				dispose();
			}
		});
		panel.add(b);

		getContentPane().add(panel, BorderLayout.SOUTH);

		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				dispose();
			}
		});

	}

	public Graph2D getGraph() {
		return graph2D;
	}

	public void setCurrentValues() {

		if (graph2D == null)
			throw new RuntimeException("No graph found");

		Font font = new Font("Arial", Font.PLAIN, 10);
		DataSet ds = graph2D.getSet(0);
		if (ds != null) {
			font = ds.getLegendFont();
		}
		legendFontChooser.setFontName(font.getFamily());
		legendFontChooser.setFontSize(font.getSize());
		legendFontChooser.setBold((font.getStyle() & Font.BOLD) != 0);
		legendFontChooser.setItalic((font.getStyle() & Font.ITALIC) != 0);

		Axis axis = (Axis) graph2D.getAxises().get(0);
		if (axis != null) {
			font = axis.getTitleFont();
		}
		labelFontChooser.setFontName(font.getFamily());
		labelFontChooser.setFontSize(font.getSize());
		labelFontChooser.setBold((font.getStyle() & Font.BOLD) != 0);
		labelFontChooser.setItalic((font.getStyle() & Font.ITALIC) != 0);

	}

	private void confirm() {
		int ix = 0, iy = 0;
		try {
			ix = Integer.parseInt(tfx.getText());
		}
		catch (NumberFormatException nfe) {
			JOptionPane.showMessageDialog(GraphDialog.this, "Input must be an integer.", "Unrecognized Input Error",
					JOptionPane.ERROR_MESSAGE);
			return;
		}
		try {
			iy = Integer.parseInt(tfy.getText());
		}
		catch (NumberFormatException nfe) {
			JOptionPane.showMessageDialog(GraphDialog.this, "Input must be an integer.", "Unrecognized Input Error",
					JOptionPane.ERROR_MESSAGE);
			return;
		}

		Font font = new Font(legendFontChooser.getFontName(), legendFontChooser.getFontStyle(), legendFontChooser
				.getFontSize());
		DataSet ds = null;
		int ii = 0;
		for (Iterator it = graph2D.getDataSets().iterator(); it.hasNext();) {
			ds = (DataSet) it.next();
			ds.setLegendLocation(ix, iy + ii * 15);
			ii++;
			ds.legendFont(font);
		}

		font = new Font(labelFontChooser.getFontName(), labelFontChooser.getFontStyle(), labelFontChooser.getFontSize());

		Axis axis = null;
		for (Iterator it = graph2D.getAxises().iterator(); it.hasNext();) {
			axis = (Axis) it.next();
			axis.setLabelFont(font);
			axis.setTitleFont(font);
			axis.setExponentFont(font);
		}

	}

}
