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

package org.concord.mw2d.models;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.concord.modeler.util.QueueGroup;
import org.concord.modeler.util.QueueGroupTable;
import org.concord.mw2d.MDView;

class TimeSeriesRepository extends JDialog {

	private JTabbedPane tabbedPane;
	private JLabel label;

	TimeSeriesRepository(Frame owner) {

		super(owner, "Repository of Movie Time Series", false);
		String s = MDView.getInternationalText("TimeSeriesRepository");
		if (s != null)
			setTitle(s);
		setResizable(true);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);

		tabbedPane = new JTabbedPane();
		tabbedPane.setPreferredSize(new Dimension(450, 280));
		tabbedPane.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				if (tabbedPane.getTabCount() == 0)
					return;
				if (tabbedPane.getSelectedComponent() == null)
					return;
				Component c = tabbedPane.getSelectedComponent();
				if (c instanceof QueueGroupTable) {
					String s = MDView.getInternationalText("TimeSeriesEntries");
					label.setText(" " + (s != null ? s : "Entries") + ": " + ((QueueGroupTable) c).getRowCount());
				}
			}
		});

		getContentPane().add(tabbedPane, BorderLayout.NORTH);

		label = new JLabel();
		label.setHorizontalAlignment(SwingConstants.LEFT);
		getContentPane().add(label, BorderLayout.CENTER);

		JPanel p = new JPanel();

		s = MDView.getInternationalText("CloseButton");
		JButton b = new JButton(s != null ? s : "Close");
		b.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				dispose();
			}
		});
		p.add(b);

		getContentPane().add(p, BorderLayout.SOUTH);

	}

	public void addQueueGroup(QueueGroup qg) {
		String s = MDView.getInternationalText("MovieData");
		tabbedPane.add(s != null ? s : "Movie data", qg.getTable());
	}

	public void clear() {
		tabbedPane.removeAll();
		dispose();
	}

}