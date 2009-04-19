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

package org.concord.modeler.g2d;

import java.awt.Button;
import java.awt.Color;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

/**
 * A popup window for altering the range of the plot
 * 
 * @version $Revision: 1.10 $, $Date: 2007-07-16 14:00:42 $.
 * @author Leigh Brookshaw
 * @author Modified by Qian Xie to be compliant with JDK1.3
 */

class Range extends Frame {

	private Graph2D g2d = null;

	private Label xminLabel = new Label("Xmin");
	private Label yminLabel = new Label("Ymin");
	private Label xmaxLabel = new Label("Xmax");
	private Label ymaxLabel = new Label("Ymax");

	private TextField xminText, yminText, xmaxText, ymaxText;

	private Button cancel, done;

	public Range(Graph2D g) {

		super.setTitle("Set Plot Range");

		g2d = g;

		xminText = new TextField(20);
		yminText = new TextField(20);
		xmaxText = new TextField(20);
		ymaxText = new TextField(20);

		xminText.addKeyListener(new KeyAdapter() {
			public void keyTyped(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ENTER) {
					xmaxText.requestFocusInWindow();
				}
			}
		});
		xmaxText.addKeyListener(new KeyAdapter() {
			public void keyTyped(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ENTER) {
					yminText.requestFocusInWindow();
				}
			}
		});
		yminText.addKeyListener(new KeyAdapter() {
			public void keyTyped(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ENTER) {
					ymaxText.requestFocusInWindow();
				}
			}
		});
		ymaxText.addKeyListener(new KeyAdapter() {
			public void keyTyped(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ENTER) {
					xminText.requestFocusInWindow();
				}
			}
		});

		setLayout(new GridLayout(5, 2, 5, 10));

		xminLabel.setAlignment(Label.LEFT);
		xmaxLabel.setAlignment(Label.LEFT);
		yminLabel.setAlignment(Label.LEFT);
		ymaxLabel.setAlignment(Label.LEFT);

		add("xminLabel", xminLabel);
		add("xminText", xminText);

		add("xmaxLabel", xmaxLabel);
		add("xmaxText", xmaxText);

		add("yminLabel", yminLabel);
		add("yminText", yminText);

		add("ymaxLabel", ymaxLabel);
		add("ymaxText", ymaxText);

		done = new Button("Done");
		done.setName("Range");
		done.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (g2d != null)
					g2d.dispatchEvent(e);
				Range.this.setVisible(false);
			}
		});

		add("done", done);
		done.setBackground(Color.green);

		cancel = new Button("Cancel");
		cancel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Range.this.setVisible(false);
			}
		});

		add("cancel", cancel);
		cancel.setBackground(Color.red);

		setSize(250, 250);

	}

	public Double getXmin() {
		try {
			return Double.valueOf(xminText.getText());
		}
		catch (Exception ex) {
			return null;
		}
	}

	public Double getXmax() {
		try {
			return Double.valueOf(xmaxText.getText());
		}
		catch (Exception ex) {
			return null;
		}
	}

	public Double getYmin() {
		try {
			return Double.valueOf(yminText.getText());
		}
		catch (Exception ex) {
			return null;
		}
	}

	public Double getYmax() {
		try {
			return Double.valueOf(ymaxText.getText());
		}
		catch (Exception ex) {
			return null;
		}
	}

	public void requestFocus() {
		xminText.requestFocusInWindow();
	}

}
