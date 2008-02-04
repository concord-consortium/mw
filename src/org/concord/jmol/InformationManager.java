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
package org.concord.jmol;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SpringLayout;

import org.concord.modeler.ModelerUtilities;
import org.concord.modeler.ui.PastableTextArea;

/**
 * @author Charles Xie
 * 
 */
class InformationManager {

	private Jmol jmol;

	String title;
	String subtitle;

	InformationManager(Jmol jmol) {
		this.jmol = jmol;
	}

	void clear() {
		title = subtitle = null;
	}

	void displayInformation() {
		String s = null;
		if (title != null && subtitle != null) {
			s = title + "\n\n\n\n" + subtitle;
		}
		else if (title != null) {
			s = title + "\n\n\n\n";
		}
		else if (subtitle != null) {
			s = "\n\n\n\n" + subtitle;
		}
		if (s != null)
			jmol.viewer
					.runScriptImmediatelyWithoutThread("set echo bottom center;color echo yellow;echo \"" + s + "\"");
	}

	JDialog getEditor(Component parent) {

		final JTextArea titleArea = new PastableTextArea(title);
		final JTextArea subtitleArea = new PastableTextArea(subtitle);

		String s = JmolContainer.getInternationalText("AnimationTitle");
		final JDialog d = new JDialog(JOptionPane.getFrameForComponent(parent), s != null ? s : "Animation Title", true);
		d.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		JPanel contentPane = new JPanel(new BorderLayout(5, 5));
		d.setContentPane(contentPane);

		JPanel panel = new JPanel(new SpringLayout());
		contentPane.add(panel, BorderLayout.CENTER);

		s = JmolContainer.getInternationalText("Title");
		panel.add(new JLabel(s != null ? s : "Title:"));
		titleArea.addFocusListener(new FocusAdapter() {
			public void focusLost(FocusEvent e) {
				String s = titleArea.getText();
				if (s == null || s.trim().equals("")) {
					title = null;
				}
				else {
					title = s;
				}
			}
		});
		JScrollPane scroller = new JScrollPane(titleArea);
		scroller.setPreferredSize(new Dimension(300, 50));
		panel.add(scroller);
		s = JmolContainer.getInternationalText("Subtitle");
		panel.add(new JLabel(s != null ? s : "Subtitle:"));
		subtitleArea.addFocusListener(new FocusAdapter() {
			public void focusLost(FocusEvent e) {
				String s = subtitleArea.getText();
				if (s == null || s.trim().equals("")) {
					subtitle = null;
				}
				else {
					subtitle = s;
				}
			}
		});
		scroller = new JScrollPane(subtitleArea);
		scroller.setPreferredSize(new Dimension(300, 100));
		panel.add(scroller);
		ModelerUtilities.makeCompactGrid(panel, 4, 1, 5, 5, 5, 5);

		panel = new JPanel();
		contentPane.add(panel, BorderLayout.SOUTH);
		s = JmolContainer.getInternationalText("Close");
		JButton button = new JButton(s != null ? s : "Close");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				displayInformation();
				d.dispose();
			}
		});
		panel.add(button);

		d.pack();
		return d;

	}

}