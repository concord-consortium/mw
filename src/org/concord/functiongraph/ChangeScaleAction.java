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
package org.concord.functiongraph;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;

/**
 * @author Charles Xie
 * 
 */
class ChangeScaleAction extends AbstractAction {

	private Graph graph;

	ChangeScaleAction(Graph graph) {
		super();
		this.graph = graph;
		putValue(NAME, "Change Scale");
		putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_G, InputEvent.CTRL_MASK));
	}

	public String toString() {
		return (String) getValue(NAME);
	}

	public void actionPerformed(ActionEvent e) {

		final JTextField xminField = new JTextField(2);
		final JTextField xmaxField = new JTextField(2);
		final JTextField yminField = new JTextField(2);
		final JTextField ymaxField = new JTextField(2);
		final JTextField xIncrementField = new JTextField(2);
		final JTextField yIncrementField = new JTextField(2);
		JOptionPane op = new JOptionPane();
		op.setLayout(new BorderLayout(6, 6));
		String s = Graph.getInternationalText("ChangeScale");
		final JDialog dialog = op.createDialog(JOptionPane.getFrameForComponent(graph), s != null ? s : "Change scale");
		ActionListener okListener = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				float xIncrement, yIncrement;
				try {
					graph.oldXmin = Float.parseFloat(xminField.getText());
					graph.oldXmax = Float.parseFloat(xmaxField.getText());
					graph.oldYmin = Float.parseFloat(yminField.getText());
					graph.oldYmax = Float.parseFloat(ymaxField.getText());
					xIncrement = Float.parseFloat(xIncrementField.getText());
					yIncrement = Float.parseFloat(yIncrementField.getText());
					String msg = null;
					if (graph.oldXmin >= graph.oldXmax) {
						msg = "x max must be greater than x min.";
					}
					else if (graph.oldYmin >= graph.oldYmax) {
						msg = "y max must be greater than y min.";
					}
					else if (xIncrement >= (graph.oldXmax - graph.oldXmin)) {
						msg = "x tick should be smaller than x-axis range.";
					}
					else if (yIncrement >= (graph.oldYmax - graph.oldYmin)) {
						msg = "y tick should be smaller than y-axis range.";
					}
					if (msg != null) {
						JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(graph), msg, "Error",
								JOptionPane.ERROR_MESSAGE);
						return;
					}
					graph.setXBounds(graph.oldXmin, graph.oldXmax);
					graph.setYBounds(graph.oldYmin, graph.oldYmax);
					graph.setOriginalScope(graph.oldXmin, graph.oldXmax, graph.oldYmin, graph.oldYmax);
					graph.xAxis.setMajorTic(xIncrement);
					graph.yAxis.setMajorTic(yIncrement);
					graph.repaint();
				}
				catch (Exception exception) {
					JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(graph),
							"You must input valid numbers. Please try again.", "Error", JOptionPane.ERROR_MESSAGE);
					return;
				}
				dialog.dispose();
			}
		};

		JPanel panel = new JPanel(new GridLayout(3, 4, 5, 5));
		op.add(panel, BorderLayout.NORTH);

		s = Graph.getInternationalText("Xmin");
		JLabel label = new JLabel(s != null ? s : "x min :");
		panel.add(label);
		xminField.setText(Graph.format.format(graph.xAxis.getMin()));
		xminField.addActionListener(okListener);
		panel.add(xminField);

		s = Graph.getInternationalText("Xmax");
		label = new JLabel(s != null ? s : "x max :");
		panel.add(label);
		xmaxField.setText(Graph.format.format(graph.xAxis.getMax()));
		xmaxField.addActionListener(okListener);
		panel.add(xmaxField);

		s = Graph.getInternationalText("Ymin");
		label = new JLabel(s != null ? s : "y min :");
		panel.add(label);
		yminField.setText(Graph.format.format(graph.yAxis.getMin()));
		yminField.addActionListener(okListener);
		panel.add(yminField);

		s = Graph.getInternationalText("Ymax");
		label = new JLabel(s != null ? s : "y max :");
		panel.add(label);
		ymaxField.setText(Graph.format.format(graph.yAxis.getMax()));
		ymaxField.addActionListener(okListener);
		panel.add(ymaxField);

		s = Graph.getInternationalText("Xtick");
		label = new JLabel(s != null ? s : "x tick :");
		panel.add(label);
		xIncrementField.setText(Graph.format.format(graph.xAxis.getMajorTic()));
		xIncrementField.addActionListener(okListener);
		panel.add(xIncrementField);

		s = Graph.getInternationalText("Ytick");
		label = new JLabel(s != null ? s : "y tick :");
		panel.add(label);
		yIncrementField.setText(Graph.format.format(graph.yAxis.getMajorTic()));
		yIncrementField.addActionListener(okListener);
		panel.add(yIncrementField);

		panel = new JPanel();
		op.add(panel, BorderLayout.SOUTH);

		s = Graph.getInternationalText("OK");
		JButton button = new JButton(s != null ? s : "OK");
		button.addActionListener(okListener);
		panel.add(button);
		s = Graph.getInternationalText("Cancel");
		button = new JButton(s != null ? s : "Cancel");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				dialog.dispose();
			}
		});
		panel.add(button);

		dialog.addWindowListener(new WindowAdapter() {
			public void windowActivated(WindowEvent e) {
				xminField.selectAll();
				xminField.requestFocus();
			}
		});

		dialog.pack();
		dialog.setLocationRelativeTo(graph);
		dialog.setVisible(true);

	}

}