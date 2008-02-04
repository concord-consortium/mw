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
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;

import org.nfunk.jep.function.PostfixMathCommand;

/**
 * @author Charles Xie
 * 
 */
class ViewSupportedFunctionsAction extends AbstractAction {

	private Graph graph;

	ViewSupportedFunctionsAction(Graph graph) {
		super();
		this.graph = graph;
		putValue(NAME, "View Supported Functions");
		putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_MASK));
	}

	public String toString() {
		return (String) getValue(NAME);
	}

	public void actionPerformed(ActionEvent e) {
		JOptionPane op = new JOptionPane();
		op.setLayout(new BorderLayout(6, 6));
		String s2 = Graph.getInternationalText("SupportedFunctionList");
		final JDialog dialog = op.createDialog(JOptionPane.getFrameForComponent(graph), s2 != null ? s2
				: "List of supported functions");
		Hashtable table = DataSource.getSupportedFunctions();
		int n = table.size();
		String[] s = new String[n];
		Object key, val;
		List<String> list = new ArrayList<String>();
		for (Object o : table.keySet()) {
			list.add(o.toString());
		}
		Collections.sort(list);
		String args = "";
		int i = 0;
		for (Iterator it = list.iterator(); it.hasNext();) {
			key = it.next();
			val = table.get(key);
			if (val instanceof PostfixMathCommand) {
				switch (((PostfixMathCommand) val).getNumberOfParameters()) {
				case 0:
					args = "()";
					break;
				case 1:
					args = "(x)";
					break;
				case 2:
					args = "(a, x)";
					break;
				case 3:
					args = "(a, b, x)";
					break;
				case 4:
					args = "(a, b, c, x)";
					break;
				}
				s[i] = key + args + '\t' + val;
				i++;
			}
		}
		JList ls = new JList(s);
		ls.setCellRenderer(new TabListCellRenderer());
		JScrollPane scroller = new JScrollPane(ls);
		scroller.setPreferredSize(new Dimension(400, 300));
		op.add(scroller, BorderLayout.NORTH);
		JPanel panel = new JPanel();
		op.add(panel, BorderLayout.SOUTH);
		s2 = Graph.getInternationalText("Close");
		JButton button = new JButton(s2 != null ? s2 : "Close");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				dialog.dispose();
			}
		});
		panel.add(button);
		panel = new JPanel();
		JLabel label = new JLabel(
				"<html><font size=2>About the arguments: Only <i>x</i> is the variable. <i>a, b, c</i> and so on must be numbers.</font></html>");
		panel.add(label);
		op.add(panel, BorderLayout.CENTER);

		dialog.pack();
		dialog.setLocationRelativeTo(graph);
		dialog.setVisible(true);
	}

}