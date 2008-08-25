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
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.concord.modeler.AudioPlayer;
import org.concord.modeler.Modeler;
import org.concord.modeler.util.FileUtilities;

class SoundInputDialog extends JDialog {

	private Page page;
	private JComboBox clipNameComboBox;
	private JCheckBox loopCheckBox;

	SoundInputDialog(Page page0) {

		super(JOptionPane.getFrameForComponent(page0), "Background Sound", true);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		String s = Modeler.getInternationalText("BackgroundSound");
		if (s != null)
			setTitle(s);

		page = page0;

		JPanel panel = new JPanel(new BorderLayout(8, 8));
		panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		setContentPane(panel);

		clipNameComboBox = new JComboBox();
		fillClipNameComboBox();
		panel.add(clipNameComboBox, BorderLayout.CENTER);

		s = Modeler.getInternationalText("SelectBackgroundSoundForCurrentPage");
		panel.add(new JLabel(s != null ? s : "Set the background sound for the current page:"), BorderLayout.NORTH);

		JPanel p = new JPanel();

		s = Modeler.getInternationalText("LoopBackgroundSound");
		loopCheckBox = new JCheckBox(s != null ? s : "Loop");
		loopCheckBox.setSelected(page.getLoopBackgroundSound());
		p.add(loopCheckBox);

		s = Modeler.getInternationalText("OKButton");
		JButton button = new JButton(s != null ? s : "OK");
		if (clipNameComboBox.getItemCount() > 0) {
			button.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					if ("None".equals(clipNameComboBox.getSelectedItem())) {
						page.setBackgroundSound(null);
					}
					else {
						page.setBackgroundSound((String) clipNameComboBox.getSelectedItem());
						page.setLoopBackgroundSound(loopCheckBox.isSelected());
					}
					page.saveReminder.setChanged(true);
					dispose();
				}
			});
		}
		else {
			button.setEnabled(false);
		}
		p.add(button);

		s = Modeler.getInternationalText("CancelButton");
		button = new JButton(s != null ? s : "Cancel");
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
				clipNameComboBox.requestFocus();
			}
		});

		pack();
		setLocationRelativeTo(JOptionPane.getFrameForComponent(page));

	}

	private void fillClipNameComboBox() {
		File parent = new File(page.getAddress()).getParentFile();
		if (parent == null)
			return;
		File[] files = parent.listFiles(AudioPlayer.fileFilter);
		if (files == null || files.length == 0)
			return;
		clipNameComboBox.addItem("None");
		for (File i : files)
			clipNameComboBox.addItem(FileUtilities.getFileName(i.toString()));
		for (int i = 0; i < clipNameComboBox.getItemCount(); i++) {
			if (clipNameComboBox.getItemAt(i).equals(page.getBackgroundSound())) {
				clipNameComboBox.setSelectedIndex(i);
				break;
			}
		}
	}

}