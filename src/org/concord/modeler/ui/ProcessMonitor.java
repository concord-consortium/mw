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

package org.concord.modeler.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Frame;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JWindow;

public class ProcessMonitor {

	private JLabel titleLabel;
	private JWindow window;
	private JProgressBar progressBar;
	private int index;
	private boolean isLocationRelativeToSet;
	private String message;

	private Runnable showMessage = new Runnable() {
		public void run() {
			progressBar.setValue(++index);
			progressBar.setString(message);
		}
	};

	public ProcessMonitor() {
		this(null);
	}

	public ProcessMonitor(Frame frame) {
		this(frame, true);
	}

	public ProcessMonitor(Frame frame, boolean hasWindow) {

		progressBar = new JProgressBar();
		progressBar.setStringPainted(true);
		progressBar.setFont(new Font("Arial", Font.PLAIN, 9));

		if (hasWindow) {
			progressBar.setPreferredSize(new Dimension(150, 16));
			JPanel p = new JPanel(new BorderLayout());
			p.setBorder(BorderFactory.createLineBorder(Color.black));
			p.add(progressBar, BorderLayout.CENTER);
			titleLabel = new JLabel();
			p.add(titleLabel, BorderLayout.NORTH);
			if (frame != null) {
				window = new JWindow(frame);
			}
			else {
				window = new JWindow();
			}
			window.setContentPane(p);
			window.pack();
		}

	}

	public void setTitle(final String title) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				titleLabel.setText(title);
			}
		});
	}

	public void resetProgressBar() {
		index = 0;
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				progressBar.setValue(progressBar.getMinimum());
			}
		});
	}

	public void setProgressBar(final JProgressBar pb) {
		if (pb == null)
			return;
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				pb.setMaximum(progressBar.getMaximum());
				pb.setMinimum(progressBar.getMinimum());
				progressBar = pb;
				progressBar.setIndeterminate(false);
			}
		});
	}

	public JProgressBar getProgressBar() {
		return progressBar;
	}

	/** send a message to the progress bar to indicate the working status */
	public void setProgressMessage(String s) {
		message = s;
		EventQueue.invokeLater(showMessage);
	}

	/** query for the progress bar's message status */
	public String getProgressMessage() {
		return progressBar.getString();
	}

	/** return the percent completed, from 0% to 100%. */
	public double getPercentComplete() {
		return 99.9999 * progressBar.getPercentComplete();
	}

	/**
	 * set the progress bar's orientation, which must be <tt>JProgressBar.VERTICAL</tt> or
	 * <tt>JProgressBar.HORIZONTAL</tt>.
	 */
	public void setOrientation(final int i) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				progressBar.setOrientation(i);
			}
		});
	}

	/** return the progress bar's orientation. */
	public int getOrientation() {
		return progressBar.getOrientation();
	}

	/** set the minimum value of the progress bar */
	public void setMinimum(final int n) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				progressBar.setMinimum(n);
			}
		});
	}

	/** set the maximum value of the progress bar */
	public void setMaximum(final int n) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				progressBar.setMaximum(n);
			}
		});
	}

	public void setLocationRelativeTo(final Component c) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				window.pack();
				window.setLocationRelativeTo(c);
				isLocationRelativeToSet = true;
			}
		});
	}

	/** show the progress bar at the given position */
	public void show(final int x, final int y) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				if (!isLocationRelativeToSet)
					window.setLocation(x, y);
				window.setVisible(true);
			}
		});
	}

	public boolean isShowing() {
		return window.isShowing();
	}

	/** hide the progress bar */
	public void hide() {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				window.setVisible(false);
				window.dispose();
				index = 0;
				progressBar.setValue(0);
			}
		});
	}

	/** return the size of the progress bar */
	public Dimension getSize() {
		if (window.getSize().width == 0 || window.getSize().height == 0) {
			return progressBar.getPreferredSize();
		}
		return window.getSize();
	}

}