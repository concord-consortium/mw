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

package org.concord.modeler.text;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.concord.modeler.Modeler;
import org.concord.modeler.ui.PastableTextField;

class TitleInputDialog extends JDialog {

	private Page page;

	TitleInputDialog(Page page0) {

		super(JOptionPane.getFrameForComponent(page0), "Title", true);
		String s = Modeler.getInternationalText("Title");
		if (s != null)
			setTitle(s);

		page = page0;

		JPanel panel = new JPanel(new BorderLayout(8, 8));
		panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		setContentPane(panel);

		final JTextField tf = new PastableTextField(page.getTitle(), 30);
		tf.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (!tf.getText().equals(page.getTitle())) {
					page.setTitle(tf.getText());
					page.saveReminder.setChanged(true);
				}
				dispose();
			}
		});
		panel.add(tf, BorderLayout.CENTER);

		s = Modeler.getInternationalText("TypeTitleForCurrentPage");
		String t = Modeler.getInternationalText("TypeBelowToChangeTitle");
		panel.add(new JLabel(tf.getText() == null || tf.getText().trim().equals("") ? (s != null ? s
				: "Please type a title for the current page")
				+ ":" : (t != null ? t : "To change the title, please type below") + ":"), BorderLayout.NORTH);

		JPanel p = new JPanel();
		s = Modeler.getInternationalText("OKButton");
		JButton button = new JButton(s != null ? s : " OK ");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (!tf.getText().equals(page.getTitle())) {
					page.setTitle(tf.getText());
					page.saveReminder.setChanged(true);
				}
				dispose();
			}
		});
		p.add(button);

		s = Modeler.getInternationalText("CancelButton");
		button = new JButton(s != null ? s : " Cancel ");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				dispose();
			}
		});
		p.add(button);

		panel.add(p, BorderLayout.SOUTH);

		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				dispose();
			}

			public void windowActivated(WindowEvent e) {
				tf.selectAll();
				tf.requestFocus();
			}
		});

		pack();
		setLocationRelativeTo(JOptionPane.getFrameForComponent(page));

	}

}