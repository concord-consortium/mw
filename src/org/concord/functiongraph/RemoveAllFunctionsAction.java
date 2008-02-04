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
class RemoveAllFunctionsAction extends AbstractAction {

	private Graph graph;

	RemoveAllFunctionsAction(Graph graph) {
		super();
		this.graph = graph;
		putValue(NAME, "Remove All Functions");
		putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_A, InputEvent.CTRL_MASK));
	}

	public String toString() {
		return (String) getValue(NAME);
	}

	public void actionPerformed(ActionEvent e) {
		if (graph.data.isEmpty())
			return;
		String s = Graph.getInternationalText("AreYouSureToRemoveAllFunctions");
		String s2 = Graph.getInternationalText("Remove");
		int response = JOptionPane.showConfirmDialog(JOptionPane.getFrameForComponent(graph), s != null ? s
				: "Are you sure to remove all the functions?", s2 != null ? s2 : "Remove", JOptionPane.YES_NO_OPTION,
				JOptionPane.QUESTION_MESSAGE);
		if (response == JOptionPane.YES_OPTION)
			graph.removeAll();
	}

}