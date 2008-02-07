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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;

/**
 * @author Charles Xie
 * 
 */
class RemoveFunctionAction extends AbstractAction {

	private Graph graph;

	RemoveFunctionAction(Graph graph) {
		super();
		this.graph = graph;
		putValue(NAME, "Remove Functions");
		putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_X, InputEvent.CTRL_MASK));
	}

	public String toString() {
		return (String) getValue(NAME);
	}

	public void actionPerformed(ActionEvent event) {
		if (graph.data.isEmpty()) {
			JOptionPane.showMessageDialog(graph, "No data has been found in this graph.", "No Data",
					JOptionPane.INFORMATION_MESSAGE);
			return;
		}
		final JList list = new JList(graph.data.toArray());
		JOptionPane op = new JOptionPane();
		op.setLayout(new BorderLayout());
		op.add(new JScrollPane(list), BorderLayout.CENTER);
		JPanel panel = new JPanel();
		op.add(panel, BorderLayout.SOUTH);
		String s = Graph.getInternationalText("SelectFunctionToRemove");
		final JDialog dialog = op.createDialog(JOptionPane.getFrameForComponent(graph), s != null ? s
				: "Select functions to remove");
		s = Graph.getInternationalText("Remove");
		JButton button = new JButton(s != null ? s : "Remove");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Object[] o = list.getSelectedValues();
				if (o != null) {
					for (int i = 0; i < o.length; i++) {
						if (o[i] instanceof DataSource)
							graph.removeDataSource((DataSource) o[i]);
					}
					graph.repaint();
				}
				dialog.dispose();
			}
		});
		panel.add(button);
		s = Graph.getInternationalText("Cancel");
		button = new JButton(s != null ? s : "Cancel");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				dialog.dispose();
			}
		});
		panel.add(button);
		dialog.pack();
		dialog.setVisible(true);
	}

}