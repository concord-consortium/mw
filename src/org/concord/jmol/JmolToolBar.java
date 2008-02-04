/*
 *   Copyright (C) 2007  The Concord Consortium, Inc.,
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
package org.concord.jmol;

import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

import org.concord.modeler.ModelerUtilities;
import org.concord.modeler.ui.IconPool;

/**
 * @author Charles Xie
 * 
 */
class JmolToolBar extends JPanel {

	private JmolContainer jmolContainer;

	private JToggleButton buttonShowSelection;

	JmolToolBar(JmolContainer container) {
		super(new FlowLayout(FlowLayout.LEFT, 0, 2));
		jmolContainer = container;
		createToolBar();
	}

	void updateButtonsAfterLoading() {
		ModelerUtilities.setWithoutNotifyingListeners(buttonShowSelection, false);
	}

	private void createToolBar() {

		int m = System.getProperty("os.name").startsWith("Mac") ? 6 : 2;
		Insets margin = new Insets(m, m, m, m);

		AbstractButton button = new JButton(jmolContainer.opener);
		button.setText(null);
		button.setMargin(margin);
		add(button);

		button = new JButton(jmolContainer.jmol.getActionMap().get("reset"));
		button.setText(null);
		button.setMargin(margin);
		add(button);

		buttonShowSelection = new JToggleButton(jmolContainer.jmol.getActionMap().get("show selection"));
		buttonShowSelection.setText(null);
		buttonShowSelection.setMargin(margin);
		add(buttonShowSelection);

		button = new JButton(IconPool.getIcon("zoom_in"));
		button.setToolTipText("Zoom in");
		button.setMargin(margin);
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				jmolContainer.jmol.zoom(true);
			}
		});
		add(button);

		button = new JButton(IconPool.getIcon("zoom_out"));
		button.setToolTipText("Zoom out");
		button.setMargin(margin);
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				jmolContainer.jmol.zoom(false);
			}
		});
		add(button);

	}

}
