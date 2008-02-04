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
import java.awt.GridLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.concord.modeler.Initializer;
import org.concord.modeler.Modeler;
import org.concord.modeler.ui.IntegerTextField;

class AdvancedHyperlinkDialog extends JDialog {

	private final static Dimension SCREEN_SIZE = Toolkit.getDefaultToolkit().getScreenSize();

	HyperlinkParameter param;

	private IntegerTextField topField;
	private IntegerTextField leftField;
	private IntegerTextField widthField;
	private IntegerTextField heightField;

	private JCheckBox resizableBox;
	private JCheckBox toolbarBox;
	private JCheckBox menubarBox;
	private JCheckBox statusbarBox;
	private JCheckBox fullscreenBox;

	AdvancedHyperlinkDialog(Frame owner) {

		super(owner, "More Settings for Hyperlink", true);
		String s = Modeler.getInternationalText("MoreSettingsForHyperlink");
		if (s != null)
			setTitle(s);

		param = new HyperlinkParameter();

		int x = Initializer.sharedInstance().getPreferences().getInt("Upper Left X", -1);
		int y = Initializer.sharedInstance().getPreferences().getInt("Upper Left Y", -1);
		int w = Initializer.sharedInstance().getPreferences().getInt("Width", -1);
		int h = Initializer.sharedInstance().getPreferences().getInt("Height", -1);

		leftField = new IntegerTextField(x, 0, SCREEN_SIZE.width);
		topField = new IntegerTextField(y, 0, SCREEN_SIZE.height);
		widthField = new IntegerTextField(w, 0, SCREEN_SIZE.width);
		heightField = new IntegerTextField(h, 0, SCREEN_SIZE.height);
		heightField.setPreferredSize(new Dimension(50, 20));

		s = Modeler.getInternationalText("Resizable");
		resizableBox = new JCheckBox(s != null ? s : "Resizable");

		s = Modeler.getInternationalText("ToolBarIsVisible");
		toolbarBox = new JCheckBox(s != null ? s : "Tool bar is visible");

		s = Modeler.getInternationalText("MenuBarIsVisible");
		menubarBox = new JCheckBox(s != null ? s : "Menu bar is visible");

		s = Modeler.getInternationalText("StatusBarIsVisible");
		statusbarBox = new JCheckBox(s != null ? s : "Status bar is visible");

		s = Modeler.getInternationalText("FullScreen");
		fullscreenBox = new JCheckBox(s != null ? s : "Full screen");
		fullscreenBox.setEnabled(false);

		ActionListener okListener = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				confirm();
				dispose();
			}
		};
		topField.addActionListener(okListener);
		leftField.addActionListener(okListener);
		widthField.addActionListener(okListener);
		heightField.addActionListener(okListener);

		JPanel panel = new JPanel(new BorderLayout(5, 5));
		panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

		JPanel p = new JPanel(new GridLayout(1, 2, 5, 5));
		s = Modeler.getInternationalText("PositionAndSizeOfWindow");
		p.setBorder(BorderFactory.createTitledBorder(s != null ? s : "Position and size of the window:"));

		JPanel p1 = new JPanel(new GridLayout(2, 2, 5, 5));
		p1.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
		s = Modeler.getInternationalText("TopCoordinate");
		p1.add(new JLabel(s != null ? s : "Top"));
		p1.add(topField);
		s = Modeler.getInternationalText("LeftCoordinate");
		p1.add(new JLabel(s != null ? s : "Left"));
		p1.add(leftField);
		p.add(p1);

		p1 = new JPanel(new GridLayout(2, 2, 5, 5));
		p1.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
		s = Modeler.getInternationalText("WidthLabel");
		p1.add(new JLabel(s != null ? s : "Width"));
		p1.add(widthField);
		s = Modeler.getInternationalText("HeightLabel");
		p1.add(new JLabel(s != null ? s : "Height"));
		p1.add(heightField);
		p.add(p1);

		panel.add(p, BorderLayout.CENTER);

		p = new JPanel(new GridLayout(3, 2, 5, 5));
		s = Modeler.getInternationalText("CustomizeUserInterface");
		p.setBorder(BorderFactory.createTitledBorder(s != null ? s : "Customize user interface:"));
		p.add(resizableBox);
		p.add(toolbarBox);
		p.add(menubarBox);
		p.add(statusbarBox);
		p.add(fullscreenBox);
		panel.add(p, BorderLayout.SOUTH);

		p = new JPanel();

		s = Modeler.getInternationalText("OKButton");
		JButton button = new JButton(s != null ? s : "OK");
		button.addActionListener(okListener);
		p.add(button);

		s = Modeler.getInternationalText("CancelButton");
		button = new JButton(s != null ? s : "Cancel");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				dispose();
			}
		});
		p.add(button);

		getContentPane().add(p, BorderLayout.SOUTH);
		getContentPane().add(panel, BorderLayout.CENTER);

		pack();

	}

	private void confirm() {
		param.setResizable(resizableBox.isSelected());
		param.setFullscreen(fullscreenBox.isSelected());
		param.setToolbar(toolbarBox.isSelected());
		param.setMenubar(menubarBox.isSelected());
		param.setStatusbar(statusbarBox.isSelected());
		param.setLeft(leftField.getValue());
		param.setTop(topField.getValue());
		param.setWidth(widthField.getValue());
		param.setHeight(heightField.getValue());
	}

	public void setCurrentValues() {
		resizableBox.setSelected(param.getResizable());
		fullscreenBox.setSelected(param.getFullscreen());
		menubarBox.setSelected(param.getMenubar());
		toolbarBox.setSelected(param.getToolbar());
		statusbarBox.setSelected(param.getStatusbar());
		leftField.setValue(param.getLeft());
		topField.setValue(param.getTop());
		widthField.setValue(param.getWidth());
		heightField.setValue(param.getHeight());
	}

}