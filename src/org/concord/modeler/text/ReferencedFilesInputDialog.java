/*
 *   Copyright (C) 2008  The Concord Consortium, Inc.,
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

package org.concord.modeler.text;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import org.concord.modeler.Modeler;

class ReferencedFilesInputDialog extends JDialog {

	private Page page;
	private JTextArea area;

	ReferencedFilesInputDialog(Page page0) {

		super(JOptionPane.getFrameForComponent(page0), "Referenced Files", true);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		String s = Modeler.getInternationalText("ReferencedFiles");
		if (s != null)
			setTitle(s);

		page = page0;

		JPanel panel = new JPanel(new BorderLayout(8, 8));
		panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		setContentPane(panel);

		area = new JTextArea(page.getReferencedFiles(), 5, 40);
		s = Modeler.getInternationalText("TypeNamesOfFilesReferencedOnThisPage");
		panel.setBorder(BorderFactory.createTitledBorder((s != null ? s
				: "Type the names of the files referenced on this page")
				+ ":"));
		panel.add(new JScrollPane(area), BorderLayout.CENTER);

		JPanel p = new JPanel();
		panel.add(p, BorderLayout.SOUTH);

		s = Modeler.getInternationalText("OKButton");
		JButton button = new JButton(s != null ? s : "OK");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				page.setReferencedFiles(area.getText().trim());
				page.saveReminder.setChanged(true);
				dispose();
			}
		});
		p.add(button);

		s = Modeler.getInternationalText("CancelButton");
		button = new JButton(s != null ? s : "Cancel");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				dispose();
			}
		});
		p.add(button);

		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				dispose();
			}

			public void windowActivated(WindowEvent e) {
				area.requestFocus();
			}
		});

		pack();
		setLocationRelativeTo(JOptionPane.getFrameForComponent(page));

	}

}