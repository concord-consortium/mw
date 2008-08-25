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
package org.concord.modeler;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SpringLayout;
import javax.swing.SwingConstants;

import org.concord.modeler.text.Page;
import org.concord.modeler.ui.ColorComboBox;
import org.concord.modeler.ui.ComboBoxRenderer;
import org.concord.modeler.util.FileUtilities;

/**
 * @author Charles Xie
 * 
 */
class AudioPlayerMaker extends ComponentMaker {

	private AudioPlayer audioPlayer;
	private JDialog dialog;
	private JComboBox clipNameComboBox, borderComboBox;
	private ColorComboBox bgComboBox;
	private JButton okButton;
	private JTextField nameField, toolTipField;
	private JPanel contentPane;

	AudioPlayerMaker(AudioPlayer audioPlayer) {
		setObject(audioPlayer);
	}

	void setObject(AudioPlayer ap) {
		audioPlayer = ap;
	}

	private void confirm() {
		audioPlayer.requestStop();
		audioPlayer.setClipName((String) clipNameComboBox.getSelectedItem());
		audioPlayer.setText(nameField.getText());
		String toolTip = toolTipField.getText();
		if (toolTip != null && !toolTip.trim().equals(""))
			audioPlayer.setToolTipText(toolTip);
		audioPlayer.setBorderType((String) borderComboBox.getSelectedItem());
		audioPlayer.setBackground(bgComboBox.getSelectedColor());
		audioPlayer.page.getSaveReminder().setChanged(true);
		audioPlayer.page.reload();
	}

	void invoke(Page page) {

		audioPlayer.page = page;
		page.deselect();
		createContentPane();

		if (needNewDialog(dialog, page)) {
			String s = Modeler.getInternationalText("CustomizeAudioPlayerDialogTitle");
			dialog = ModelerUtilities.getChildDialog(page, s != null ? s : "Customize audio player", true);
			dialog.setContentPane(contentPane);
			dialog.pack();
			dialog.setLocationRelativeTo(dialog.getOwner());
			dialog.addWindowListener(new WindowAdapter() {
				public void windowClosing(WindowEvent e) {
					cancel = true;
					dialog.setVisible(false);
				}

				public void windowActivated(WindowEvent e) {
					nameField.selectAll();
					nameField.requestFocus();
				}
			});
		}

		fillClipNameComboBox();
		nameField.setText(audioPlayer.getText());
		toolTipField.setText(audioPlayer.getToolTipText());
		borderComboBox.setSelectedItem(audioPlayer.getBorderType());
		bgComboBox.setColor(audioPlayer.getBackground());
		okButton.setEnabled(clipNameComboBox.getItemCount() > 0);

		dialog.setVisible(true);

	}

	private void fillClipNameComboBox() {
		clipNameComboBox.removeAllItems();
		File parent = new File(audioPlayer.page.getAddress()).getParentFile();
		if (parent == null)
			return;
		File[] files = parent.listFiles(AudioPlayer.fileFilter);
		if (files == null || files.length == 0)
			return;
		for (File f : files)
			clipNameComboBox.addItem(FileUtilities.getFileName(f.toString()));
		for (int i = 0; i < files.length; i++) {
			if (clipNameComboBox.getItemAt(i).equals(audioPlayer.clipName)) {
				clipNameComboBox.setSelectedIndex(i);
				break;
			}
		}
	}

	private void createContentPane() {

		if (contentPane != null)
			return;

		contentPane = new JPanel(new BorderLayout());

		ActionListener okListener = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				confirm();
				dialog.setVisible(false);
				cancel = false;
			}
		};

		JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		contentPane.add(p, BorderLayout.SOUTH);

		String s = Modeler.getInternationalText("OKButton");
		okButton = new JButton(s != null ? s : "OK");
		okButton.addActionListener(okListener);
		p.add(okButton);

		s = Modeler.getInternationalText("CancelButton");
		JButton button = new JButton(s != null ? s : "Cancel");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				dialog.setVisible(false);
				cancel = true;
			}
		});
		p.add(button);

		p = new JPanel(new SpringLayout());
		p.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		contentPane.add(p, BorderLayout.CENTER);

		// row 1
		s = Modeler.getInternationalText("SelectAudioClip");
		p.add(new JLabel(s != null ? s : "Select an audio clip", SwingConstants.LEFT));
		clipNameComboBox = new JComboBox();
		clipNameComboBox.setFont(new Font(clipNameComboBox.getFont().getFamily(),
				clipNameComboBox.getFont().getStyle(), 10));
		clipNameComboBox.setToolTipText("Select an audio clip from the current directory.");
		p.add(clipNameComboBox);

		// row 2
		s = Modeler.getInternationalText("TextLabel");
		p.add(new JLabel(s != null ? s : "Text", SwingConstants.LEFT));
		nameField = new JTextField();
		nameField.setToolTipText("Type in the text that will appear on this audio player.");
		nameField.addActionListener(okListener);
		p.add(nameField);

		// row 3
		s = Modeler.getInternationalText("ToolTipLabel");
		p.add(new JLabel(s != null ? s : "Tool tip", SwingConstants.LEFT));
		toolTipField = new JTextField();
		toolTipField.setToolTipText("Type in the text that will appear as the tool tip.");
		toolTipField.addActionListener(okListener);
		p.add(toolTipField);

		// row 4
		s = Modeler.getInternationalText("BackgroundColorLabel");
		p.add(new JLabel(s != null ? s : "Background color", SwingConstants.LEFT));
		bgComboBox = new ColorComboBox(audioPlayer);
		bgComboBox.setSelectedIndex(ColorComboBox.INDEX_MORE_COLOR);
		bgComboBox.setRequestFocusEnabled(false);
		bgComboBox.setToolTipText("Select the background color for this audio player.");
		p.add(bgComboBox);

		// row 5
		s = Modeler.getInternationalText("BorderLabel");
		p.add(new JLabel(s != null ? s : "Border", SwingConstants.LEFT));
		borderComboBox = new JComboBox(BorderManager.BORDER_TYPE);
		borderComboBox.setRenderer(new ComboBoxRenderer.BorderCell());
		borderComboBox.setBackground(p.getBackground());
		borderComboBox.setToolTipText("Select the border type for this audio player.");
		borderComboBox.setPreferredSize(new Dimension(200, 24));
		p.add(borderComboBox);

		ModelerUtilities.makeCompactGrid(p, 5, 2, 5, 5, 10, 2);

	}

}