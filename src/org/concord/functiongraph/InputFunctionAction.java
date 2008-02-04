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

import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;

/**
 * @author Charles Xie
 * 
 */
class InputFunctionAction extends AbstractAction {

	private Graph graph;

	InputFunctionAction(Graph graph) {
		super();
		this.graph = graph;
		putValue(NAME, "Input a Function");
		putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_MASK));
	}

	public String toString() {
		return (String) getValue(NAME);
	}

	public void actionPerformed(ActionEvent e) {
		String s = Graph.getInternationalText("InputFunction");
		String expression = JOptionPane.showInputDialog(JOptionPane.getFrameForComponent(graph), s != null ? s
				: "Please input a function, using x as the variable." + '\n'
						+ "Examples: 3*x+4, 5sin(x), 2*e^((-x/10)^2)", s != null ? s : "Inputting a function",
				JOptionPane.PLAIN_MESSAGE);
		if (expression != null)
			graph.addFunction(expression, false);
	}

}