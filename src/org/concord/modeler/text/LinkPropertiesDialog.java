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
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import org.concord.modeler.Modeler;
import org.concord.modeler.ui.PastableTextField;
import org.concord.modeler.util.FileUtilities;

class LinkPropertiesDialog extends JDialog {

	private JTextField urlField;
	private JLabel protocolLabel;
	private JLabel typeLabel;
	private String link;

	LinkPropertiesDialog(Page page) {

		super(JOptionPane.getFrameForComponent(page), "Properties", true);
		String s = Modeler.getInternationalText("Properties");
		if (s != null)
			setTitle(s);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);

		setLocation(200, 200);
		Container container = getContentPane();

		JTabbedPane tabbedPane = new JTabbedPane();
		container.add(BorderLayout.CENTER, tabbedPane);

		Box parent = new Box(BoxLayout.X_AXIS);
		parent.setBorder(new EmptyBorder(10, 10, 10, 10));
		s = Modeler.getInternationalText("GeneralTab");
		tabbedPane.addTab(s != null ? s : "General", parent);

		Box child = new Box(BoxLayout.Y_AXIS);
		parent.add(child);
		parent.add(Box.createHorizontalStrut(10));

		s = Modeler.getInternationalText("Protocol");
		JLabel label = new JLabel((s != null ? s : "Protocol") + ":");
		child.add(label);
		child.add(Box.createVerticalStrut(10));

		s = Modeler.getInternationalText("FileType");
		label = new JLabel((s != null ? s : "Type") + ":");
		child.add(label);
		child.add(Box.createVerticalStrut(10));

		s = Modeler.getInternationalText("LinkLocation");
		label = new JLabel((s != null ? s : "Location") + ":");
		child.add(label);
		child.add(Box.createVerticalStrut(10));

		child = new Box(BoxLayout.Y_AXIS);
		parent.add(child);

		protocolLabel = new JLabel("Unknown");
		child.add(protocolLabel);
		child.add(Box.createVerticalStrut(10));

		typeLabel = new JLabel("Unknown");
		child.add(typeLabel);
		child.add(Box.createVerticalStrut(10));

		urlField = new PastableTextField("Unknown");
		urlField.setEditable(false);
		urlField.setBackground(typeLabel.getBackground());
		urlField.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
		child.add(urlField);
		child.add(Box.createVerticalStrut(10));

		JPanel p = new JPanel();
		p.setLayout(new FlowLayout(FlowLayout.RIGHT));

		s = Modeler.getInternationalText("CloseButton");
		JButton button = new JButton(s != null ? s : "Close");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				LinkPropertiesDialog.this.dispose();
			}
		});
		p.add(button);

		container.add(BorderLayout.SOUTH, p);

	}

	public void setLink(String link) {
		this.link = link;
	}

	public void setCurrentValues() {

		if (link.toLowerCase().startsWith("mailto")) {
			protocolLabel.setText("MailTo Protocol");
			typeLabel.setText("E-mail Address");
		}
		else if (link.toLowerCase().endsWith("jnlp")) {
			protocolLabel.setText("Java Network Launching Protocol");
			typeLabel.setText("JNLP File");
		}
		else {
			if (FileUtilities.isRemote(link)) {
				protocolLabel.setText("HyperText Transfer Protocol");
			}
			else {
				protocolLabel.setText("File Protocol");
			}
			if (link.toLowerCase().endsWith("cml")) {
				typeLabel.setText("CML File");
			}
			else if (link.toLowerCase().endsWith("html") || link.toLowerCase().endsWith("htm")) {
				typeLabel.setText("HTML File");
			}
			else if (link.toLowerCase().endsWith("swf")) {
				typeLabel.setText("Shockwave Flash File");
			}
			else {
				typeLabel.setText("HTML File");
			}
		}

		urlField.setText(link);

	}

}