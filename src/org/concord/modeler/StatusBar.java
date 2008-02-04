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
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.border.Border;
import javax.swing.plaf.basic.BasicBorders;

import org.concord.modeler.event.ProgressEvent;
import org.concord.modeler.event.ProgressListener;

class StatusBar extends JComponent implements ProgressListener {

	TipBar tipBar;
	JLabel systemInfo;
	private JProgressBar progressBar;

	private static Icon hdIcon = new ImageIcon(StatusBar.class.getResource("images/HardDrive.gif"));
	private static Icon webIcon = new ImageIcon(StatusBar.class.getResource("images/WebIcon.gif"));
	private static Border border = new BasicBorders.ButtonBorder(Color.lightGray, Color.white, Color.black, Color.gray);
	private static String webFile, localFile;

	public StatusBar() {

		systemInfo = new JLabel();
		if (Modeler.isUSLocale)
			systemInfo.setFont(systemInfo.getFont().deriveFont(systemInfo.getFont().getSize() - 2.0f));
		progressBar = new JProgressBar(JProgressBar.HORIZONTAL, 0, 100);
		progressBar.setStringPainted(true);
		progressBar.setFont(progressBar.getFont().deriveFont(progressBar.getFont().getSize() - 2.0f));

		setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

		JPanel p = new JPanel(new BorderLayout());
		p.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));

		tipBar = new TipBar();
		if (Modeler.isUSLocale)
			tipBar.setFont(tipBar.getFont().deriveFont(tipBar.getFont().getSize() - 2.0f));
		tipBar.setBorder(border);
		p.add(tipBar, BorderLayout.CENTER);
		p.setPreferredSize(new Dimension(240, 20));

		add(p);

		p = new JPanel(new BorderLayout());
		p.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
		p.setPreferredSize(new Dimension(300, 20));
		progressBar.setBorder(border);
		p.add(progressBar, BorderLayout.CENTER);

		add(p);

		p = new JPanel(new BorderLayout());
		p.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
		p.setPreferredSize(new Dimension(40, 20));

		systemInfo.setText("Ready");
		systemInfo.setBorder(border);
		p.add(systemInfo, BorderLayout.CENTER);

		add(p);

	}

	public JProgressBar getProgressBar() {
		return progressBar;
	}

	void showNewPageStatus() {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				systemInfo.setEnabled(false);
				String s = Modeler.getInternationalText("UnsavedStatus");
				systemInfo.setText(s != null ? s : "Unsaved file");
				systemInfo.setIcon(hdIcon);
			}
		});
	}

	void showWebPageStatus() {
		if (webFile == null) {
			webFile = Modeler.getInternationalText("WebFile");
			if (webFile == null)
				webFile = "Web file";
		}
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				systemInfo.setEnabled(true);
				systemInfo.setText(webFile);
				systemInfo.setIcon(webIcon);
			}
		});
	}

	void showLocalPageStatus() {
		if (localFile == null) {
			localFile = Modeler.getInternationalText("LocalFile");
			if (localFile == null)
				localFile = "Local file";
		}
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				systemInfo.setEnabled(true);
				systemInfo.setText(localFile);
				systemInfo.setIcon(hdIcon);
			}
		});
	}

	public void progressReported(final ProgressEvent e) {
		if (e == null)
			return;
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				if (e.getPercent() >= 0)
					progressBar.setValue(e.getPercent());
				if (e.getDescription() != null)
					progressBar.setString(e.getDescription());
			}
		});
	}

}