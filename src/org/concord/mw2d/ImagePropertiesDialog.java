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

package org.concord.mw2d;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SpringLayout;
import javax.swing.border.EmptyBorder;

import org.concord.modeler.ConnectionManager;
import org.concord.modeler.ui.IntegerTextField;
import org.concord.modeler.util.FileUtilities;
import org.concord.mw2d.models.Atom;
import org.concord.mw2d.models.GayBerneParticle;
import org.concord.mw2d.models.ImageComponent;
import org.concord.mw2d.models.RectangularObstacle;

class ImagePropertiesDialog extends JDialog {

	private ImageComponent image;
	private String baseURL;
	private ActionListener okListener;

	private JLabel urlLabel;
	private JLabel typeLabel;
	private JLabel indexLabel;
	private JLabel sizeLabel;
	private JLabel timeLabel;
	private JLabel dimensionLabel;
	private JLabel logicalScreenLabel;
	private JLabel offsetLabel;
	private JLabel nFrameLabel;
	private JLabel currentFrameLabel;
	private JLabel currentDelayLabel;
	private JLabel attachLabel;
	private IntegerTextField loopField;
	private JTabbedPane tabbedPane;
	private JPanel animPanel;

	ImagePropertiesDialog(MDView view) {

		super(JOptionPane.getFrameForComponent(view), "Image Properties", true);
		String s = MDView.getInternationalText("ImageProperties");
		if (s != null)
			setTitle(s);

		baseURL = (String) view.getModel().getProperty("url");

		Container container = getContentPane();

		tabbedPane = new JTabbedPane();
		container.add(BorderLayout.CENTER, tabbedPane);

		JPanel mainPanel = new JPanel(new SpringLayout());
		mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
		s = MDView.getInternationalText("GeneralTab");
		tabbedPane.addTab(s != null ? s : "General", mainPanel);

		// row 1
		s = MDView.getInternationalText("ImageTypeLabel");
		mainPanel.add(new JLabel((s != null ? s : "Type") + ":"));
		typeLabel = new JLabel("Unknown");
		mainPanel.add(typeLabel);

		// row 2
		s = MDView.getInternationalText("IndexLabel");
		mainPanel.add(new JLabel((s != null ? s : "Index") + ":"));
		indexLabel = new JLabel("Unknown");
		mainPanel.add(indexLabel);

		// row 3
		s = MDView.getInternationalText("FileLabel");
		mainPanel.add(new JLabel((s != null ? s : "Location") + ":"));
		urlLabel = new JLabel("Unknown");
		mainPanel.add(urlLabel);

		// row 4
		s = MDView.getInternationalText("SizeLabel");
		mainPanel.add(new JLabel((s != null ? s : "Size") + ":"));
		sizeLabel = new JLabel("Unknown");
		mainPanel.add(sizeLabel);

		// row 5
		s = MDView.getInternationalText("LastModifiedLabel");
		mainPanel.add(new JLabel(s != null ? s : "Last Modified:"));
		timeLabel = new JLabel("Unknown");
		mainPanel.add(timeLabel);

		// row 6
		s = MDView.getInternationalText("DimensionLabel");
		mainPanel.add(new JLabel((s != null ? s : "Dimension") + ":"));
		dimensionLabel = new JLabel("Unknown");
		mainPanel.add(dimensionLabel);

		// row 7
		s = MDView.getInternationalText("AttachToLabel");
		mainPanel.add(new JLabel((s != null ? s : "Attached to") + ":"));
		attachLabel = new JLabel("None");
		mainPanel.add(attachLabel);

		PropertiesPanel.makeCompactGrid(mainPanel, 7, 2, 5, 5, 20, 10);

		JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));

		okListener = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (image != null && loopField != null) {
					image.setLoopCount(loopField.getValue());
				}
				dispose();
			}
		};

		s = MDView.getInternationalText("OKButton");
		final JButton button = new JButton(s != null ? s : "OK");
		button.addActionListener(okListener);
		p.add(button);

		container.add(BorderLayout.SOUTH, p);

		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				dispose();
			}

			public void windowActivated(WindowEvent e) {
				button.requestFocus();
			}
		});

	}

	private void addAnimPanel() {

		String s = MDView.getInternationalText("Animation");

		animPanel = new JPanel(new BorderLayout());

		JPanel p = new JPanel(new BorderLayout(5, 5));
		p.setBorder(new EmptyBorder(10, 10, 10, 10));
		animPanel.add(p, BorderLayout.NORTH);

		JPanel child = new JPanel(new SpringLayout());
		p.add(child, BorderLayout.NORTH);

		// row 1
		s = MDView.getInternationalText("NumberOfFrames");
		child.add(new JLabel((s != null ? s : "Number of Frames") + ":"));
		nFrameLabel = new JLabel("Unknown");
		child.add(nFrameLabel);

		// row 2
		s = MDView.getInternationalText("CurrentFrame");
		child.add(new JLabel((s != null ? s : "Current Frame") + ":"));
		currentFrameLabel = new JLabel("Unknown");
		child.add(currentFrameLabel);

		// row 3
		s = MDView.getInternationalText("CurrentOffset");
		child.add(new JLabel((s != null ? s : "Current Offset") + ":"));
		offsetLabel = new JLabel("Unknown");
		child.add(offsetLabel);

		// row 4
		s = MDView.getInternationalText("CurrentDelayTime");
		child.add(new JLabel((s != null ? s : "Current Delay") + ":"));
		currentDelayLabel = new JLabel("Unknown");
		child.add(currentDelayLabel);

		// row 5
		s = MDView.getInternationalText("LogicalScreen");
		child.add(new JLabel((s != null ? s : "Logical Screen") + ":"));
		logicalScreenLabel = new JLabel("Unknown");
		child.add(logicalScreenLabel);

		// row 6
		s = MDView.getInternationalText("NumberOfLoops");
		child.add(new JLabel((s != null ? s : "Number of Loops") + ":"));
		loopField = new IntegerTextField(1000, 1, 100000, 8);
		child.add(loopField);

		PropertiesPanel.makeCompactGrid(child, 6, 2, 5, 5, 20, 10);

		loopField.addActionListener(okListener);

		nFrameLabel.setText(image.getFrames() + "");
		currentFrameLabel.setText(image.getCurrentFrame() + "");
		currentDelayLabel.setText(image.getDelayTime(image.getCurrentFrame()) + " milliseconds");
		offsetLabel.setText("( " + (int) (image.getXFrame() - image.getRx()) + " , "
				+ (int) (image.getYFrame() - image.getRy()) + " ) pixels");
		logicalScreenLabel.setText(image.getLogicalScreenWidth() + " x " + image.getLogicalScreenHeight() + " pixels");
		loopField.setValue(image.getLoopCount());

		tabbedPane.addTab(s != null ? s : "Animation", animPanel);

	}

	private void removeAnimPanel() {
		if (animPanel != null)
			tabbedPane.remove(animPanel);
	}

	public void setImage(ImageComponent image) {

		this.image = image;

		if (image != null) {

			String s = image.toString();

			if (s.toLowerCase().endsWith(".gif")) {
				if (image.getFrames() <= 1) {
					typeLabel.setText("GIF");
					removeAnimPanel();
				}
				else {
					typeLabel.setText("Animated GIF (" + image.getFrames() + " frames)");
					addAnimPanel();
				}
			}
			else if (s.toLowerCase().endsWith(".jpg") || s.toLowerCase().endsWith(".jpe")
					|| s.toLowerCase().endsWith(".jpeg")) {
				typeLabel.setText("JPEG");
				removeAnimPanel();
			}
			else if (s.toLowerCase().endsWith(".png")) {
				typeLabel.setText("PNG");
				removeAnimPanel();
			}
			else if (s.toLowerCase().endsWith(".bmp")) {
				typeLabel.setText("BMP");
				removeAnimPanel();
			}
			else if (s.toLowerCase().endsWith(".wbmp")) {
				typeLabel.setText("WBMP");
				removeAnimPanel();
			}
			else {
				typeLabel.setText("Unknown format");
				removeAnimPanel();
			}

			indexLabel.setText(((MDView) image.getHostModel().getView()).getLayeredComponentIndex(image) + "");

			if (image.getHost() != null) {
				if (image.getHost() instanceof Atom) {
					attachLabel.setText("Atom " + image.getHost().toString());
				}
				else if (image.getHost() instanceof GayBerneParticle) {
					attachLabel.setText("Gay-Berne Particle " + image.getHost().toString());
				}
				else if (image.getHost() instanceof RectangularObstacle) {
					attachLabel.setText("Rectangle " + image.getHost().toString());
				}
			}
			else {
				attachLabel.setText("None");
			}

			dimensionLabel.setText(image.getWidth() + " x " + image.getHeight() + " pixels, at (" + (int) image.getRx()
					+ ", " + (int) image.getRy() + ")");

			if (FileUtilities.isRemote(s)) {
				URL u = null;
				try {
					u = new URL(s);
				}
				catch (MalformedURLException e) {
					e.printStackTrace(System.err);
				}
				if (u != null) {
					long[] t = ConnectionManager.getLastModifiedAndContentLength(u);
					sizeLabel.setText(t[1] + " bytes");
					timeLabel.setText(new Date(t[0]).toString());
				}
				urlLabel.setText(s);
			}
			else {
				if (FileUtilities.isRemote(baseURL)) {
					URL u = null;
					try {
						u = new URL(FileUtilities.getCodeBase(baseURL) + FileUtilities.getFileName(s));
					}
					catch (MalformedURLException e) {
						e.printStackTrace(System.err);
					}
					if (u != null) {
						long[] t = ConnectionManager.getLastModifiedAndContentLength(u);
						sizeLabel.setText(t[1] + " bytes");
						timeLabel.setText(new Date(t[0]).toString());
					}
					urlLabel.setText(u.toString());
				}
				else {
					File f = new File(FileUtilities.getCodeBase(baseURL) + FileUtilities.getFileName(s));
					sizeLabel.setText(f.length() + " bytes");
					timeLabel.setText(new Date(f.lastModified()).toString());
					urlLabel.setText(f.toString());
				}
			}

		}

	}

}