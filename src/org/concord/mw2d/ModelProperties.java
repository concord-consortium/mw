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
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.text.NumberFormat;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.plaf.basic.BasicBorders;

import org.concord.modeler.Modeler;
import org.concord.mw2d.models.MDModel;

abstract class ModelProperties extends JDialog {

	final static BasicBorders.ButtonBorder BUTTON_BORDER = new BasicBorders.ButtonBorder(Color.lightGray, Color.white,
			Color.black, Color.gray);
	final static Dimension SCREEN_SIZE = Toolkit.getDefaultToolkit().getScreenSize();

	JTabbedPane tabbedPane;

	public ModelProperties(Frame owner) {

		super(owner, "Model Properties", false);
		setResizable(true);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);

		JPanel p = new JPanel(new BorderLayout());
		p.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
		setContentPane(p);

		tabbedPane = new JTabbedPane();
		p.add(tabbedPane, BorderLayout.CENTER);

		JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

		String s = MDView.getInternationalText("OKButton");
		JButton okButton = new JButton(s != null ? s : "OK");
		okButton.setMnemonic(KeyEvent.VK_O);
		okButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				confirm();
				dispose();
			}
		});

		s = MDView.getInternationalText("CancelButton");
		JButton cancelButton = new JButton(s != null ? s : "Cancel");
		cancelButton.setMnemonic(KeyEvent.VK_C);
		cancelButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				dispose();
			}
		});

		if (Modeler.isMac()) {
			panel.add(cancelButton);
			panel.add(okButton);
		}
		else {
			panel.add(okButton);
			panel.add(cancelButton);
		}

		p.add(panel, BorderLayout.SOUTH);

	}

	public void destroy() {
		dispose();
	}

	static String format(double number) {
		return NumberFormat.getInstance().format(number);
	}

	abstract void confirm();

	abstract void selectInitializationScriptTab();

	public abstract void setModel(MDModel m);

}