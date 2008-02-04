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

package org.concord.mw2d.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import org.concord.modeler.ui.IntegerTextField;

class ProteinSynthesisModelProperties extends JPanel {

	private JCheckBox transcriptionAnimationCheckBox;
	private JLabel transcriptionSpeedLabel1;
	private JLabel transcriptionSpeedLabel2;
	private IntegerTextField transcriptionSpeedField;
	private IntegerTextField translationSpeedField;
	private AtomContainer container;

	ProteinSynthesisModelProperties(AtomContainer c) {

		super(new BorderLayout(10, 10));
		setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		container = c;

		/* transcription animation options */

		JPanel panel = new JPanel(new BorderLayout(5, 5));
		panel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),
				"Transcription animation options:", 0, 0));
		add(panel, BorderLayout.NORTH);

		JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT));
		panel.add(p, BorderLayout.CENTER);

		transcriptionAnimationCheckBox = new JCheckBox("Show");
		transcriptionAnimationCheckBox.setSelected(true);
		transcriptionAnimationCheckBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				final boolean b = e.getStateChange() == ItemEvent.SELECTED;
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						transcriptionSpeedField.setEnabled(b);
						transcriptionSpeedLabel1.setEnabled(b);
						transcriptionSpeedLabel2.setEnabled(b);
						transcriptionSpeedField.setValue(b ? 50 : 0);
					}
				});
			}
		});
		p.add(transcriptionAnimationCheckBox);

		transcriptionSpeedLabel1 = new JLabel("Transcription speed: every", SwingConstants.LEFT);
		p.add(transcriptionSpeedLabel1);

		transcriptionSpeedField = new IntegerTextField(50, 20, 1000);
		transcriptionSpeedField.setPreferredSize(new Dimension(40, 20));
		p.add(transcriptionSpeedField);

		transcriptionSpeedLabel2 = new JLabel("milliseconds", SwingConstants.LEFT);
		p.add(transcriptionSpeedLabel2);

		p = new JPanel(new FlowLayout(FlowLayout.LEFT));
		panel.add(p, BorderLayout.SOUTH);

		p.add(new JLabel("*  1 millisecond = 0.001 second", SwingConstants.LEFT));

		/* translation animation options */

		panel = new JPanel(new BorderLayout());
		panel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),
				"Translation animation options:", 0, 0));
		add(panel, BorderLayout.CENTER);

		p = new JPanel(new FlowLayout(FlowLayout.LEFT));
		panel.add(p, BorderLayout.NORTH);

		p.add(new JLabel("Translation speed: every", SwingConstants.LEFT));

		translationSpeedField = new IntegerTextField(1000, 100, 5000);
		translationSpeedField.setPreferredSize(new Dimension(40, 20));
		p.add(translationSpeedField);

		p.add(new JLabel("molecular dynamics steps", SwingConstants.LEFT));

	}

	private void confirm() {
		if (container == null)
			return;
		if (!transcriptionAnimationCheckBox.isSelected()) {
			container.setTranscriptionTimeStep(0);
		}
		else {
			container.setTranscriptionTimeStep(transcriptionSpeedField.getValue());
		}
		container.setTranslationMDStep(translationSpeedField.getValue());
	}

	JDialog createDialog(Component parent) {

		transcriptionAnimationCheckBox.setSelected(container.getTranscriptionTimeStep() > 0);
		transcriptionSpeedField.setValue(container.getTranscriptionTimeStep());
		translationSpeedField.setValue(container.getTranslationMDStep());

		final JDialog d = new JDialog(JOptionPane.getFrameForComponent(parent), "Protein Synthesis Control Panel", true);

		d.getContentPane().add(this, BorderLayout.CENTER);

		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		d.getContentPane().add(buttonPanel, BorderLayout.SOUTH);

		ActionListener okListener = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				confirm();
				d.getContentPane().remove(ProteinSynthesisModelProperties.this);
				d.dispose();
			}
		};

		ActionListener[] al = transcriptionSpeedField.getActionListeners();
		if (al != null) {
			for (int i = 0; i < al.length; i++)
				transcriptionSpeedField.removeActionListener(al[i]);
		}
		transcriptionSpeedField.addActionListener(okListener);

		al = translationSpeedField.getActionListeners();
		if (al != null) {
			for (int i = 0; i < al.length; i++)
				translationSpeedField.removeActionListener(al[i]);
		}
		translationSpeedField.addActionListener(okListener);

		JButton button = new JButton("OK");
		button.addActionListener(okListener);
		buttonPanel.add(button);

		button = new JButton("Cancel");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				d.getContentPane().remove(ProteinSynthesisModelProperties.this);
				d.dispose();
			}
		});
		buttonPanel.add(button);

		d.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				d.getContentPane().remove(ProteinSynthesisModelProperties.this);
				d.dispose();
			}

			public void windowActivated(WindowEvent e) {
			}
		});

		d.pack();
		d.setLocationRelativeTo(parent);

		return d;

	}

}