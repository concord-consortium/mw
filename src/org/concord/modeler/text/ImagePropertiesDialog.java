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
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import org.concord.modeler.ConnectionManager;
import org.concord.modeler.Modeler;
import org.concord.modeler.ModelerUtilities;
import org.concord.modeler.ui.PastableTextField;
import org.concord.modeler.util.FileUtilities;

class ImagePropertiesDialog extends JDialog {

	private JTextField urlField;
	private JLabel protocolLabel;
	private JLabel typeLabel;
	private JLabel sizeLabel;
	private JLabel timeLabel;
	private JLabel dimensionLabel;
	private ImageIcon image;
	private Page page;

	ImagePropertiesDialog(Page page0) {

		super(JOptionPane.getFrameForComponent(page0), "Properties", true);
		String s = Modeler.getInternationalText("Properties");
		if (s != null)
			setTitle(s);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);

		this.page = page0;

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

		s = Modeler.getInternationalText("PageLocation");
		label = new JLabel((s != null ? s : "Location") + ":");
		child.add(label);
		child.add(Box.createVerticalStrut(10));

		s = Modeler.getInternationalText("FileSize");
		label = new JLabel((s != null ? s : "Size") + ":");
		child.add(label);
		child.add(Box.createVerticalStrut(10));

		s = Modeler.getInternationalText("LastModified");
		label = new JLabel((s != null ? s : "Last Modified") + ":");
		child.add(label);
		child.add(Box.createVerticalStrut(10));

		s = Modeler.getInternationalText("ImageDimension");
		label = new JLabel((s != null ? s : "Dimension") + ":");
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

		sizeLabel = new JLabel("Unknown");
		child.add(sizeLabel);
		child.add(Box.createVerticalStrut(10));

		timeLabel = new JLabel("Unknown");
		child.add(timeLabel);
		child.add(Box.createVerticalStrut(10));

		dimensionLabel = new JLabel("Unknown");
		child.add(dimensionLabel);
		child.add(Box.createVerticalStrut(10));

		JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT));

		s = Modeler.getInternationalText("CloseButton");
		JButton button = new JButton(s != null ? s : "Close");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				ImagePropertiesDialog.this.dispose();
			}
		});
		p.add(button);

		container.add(BorderLayout.SOUTH, p);

	}

	public void setImage(ImageIcon image) {
		this.image = image;
	}

	public ImageIcon getImage() {
		return image;
	}

	public void setLocation(String url) {
		image = null;
		setParam(url);
		dimensionLabel.setText("Not calculated");
	}

	private void setParam(String url) {

		if (url.toLowerCase().endsWith("gif")) {
			typeLabel.setText("GIF File");
		}
		else if (url.toLowerCase().endsWith("png")) {
			typeLabel.setText("PNG File");
		}
		else if (url.toLowerCase().endsWith("jpg") || url.toLowerCase().endsWith("jpe")
				|| url.toLowerCase().endsWith("jpeg")) {
			typeLabel.setText("JPEG File");
		}

		if (FileUtilities.isRemote(url)) {
			URL u = null;
			try {
				u = new URL(url);
			}
			catch (MalformedURLException e) {
				e.printStackTrace(System.err);
			}
			if (u != null) {
				long[] t = ConnectionManager.getLastModifiedAndContentLength(u);
				sizeLabel.setText(t[1] + " bytes");
				timeLabel.setText(new Date(t[0]).toString());
			}
			urlField.setText(url);
		}
		else {
			File f = null;
			String s = ModelerUtilities.convertURLToFilePath(url);
			URL u = ConnectionManager.sharedInstance().getRemoteCopy(new File(s));
			if (u == null) {
				f = new File(FileUtilities.getCodeBase(page.getAddress()), FileUtilities.getFileName(s));
			}
			else {
				f = new File(s);
			}
			sizeLabel.setText(f.length() + " bytes");
			timeLabel.setText(new Date(f.lastModified()).toString());
			String s2 = FileUtilities.getCodeBase(page.getAddress());
			urlField.setText((s2 == null ? "" : s2) + FileUtilities.getFileName(s));
		}

	}

	public void setCurrentValues() {

		if (image != null) {
			setParam(image.toString());
			dimensionLabel.setText(image.getIconWidth() + " x " + image.getIconHeight() + " pixels");
		}

		if (FileUtilities.isRemote(urlField.getText())) {
			protocolLabel.setText("HyperText Transfer Protocol");
		}
		else {
			protocolLabel.setText("File Protocol");
		}

	}

}