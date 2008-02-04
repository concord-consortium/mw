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

import java.awt.Font;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

/**
 * <p>
 * Popup a window to output data after a Graphics Input command the window contains the following:
 * </p>
 * 
 * <pre>
 *           X  value
 *           Y  value
 * </pre>
 * 
 * @version $Revision: 1.10 $, $Date: 2007-07-16 14:00:43 $.
 * @author Leigh Brookshaw
 * @author Modified by Qian Xie to be compliant with JDK1.3
 */

class Gin extends Frame {

	private Label xlabel, ylabel;

	public Gin() {

		setLayout(new GridLayout(2, 1));

		xlabel = new Label();
		ylabel = new Label();
		xlabel.setAlignment(Label.LEFT);
		ylabel.setAlignment(Label.LEFT);

		setFont(new Font("Helvetica", Font.PLAIN, 20));

		add("x", xlabel);
		add("y", ylabel);

		setSize(150, 100);
		super.setTitle("Graphics Input");

		/*
		 * Catch the Key Down event 'h'. If the key is pressed then hide this window.
		 */
		addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_H) {
					Gin.this.setVisible(false);
				}
			}
		});

	}

	/*
	 * @param title the title to use on the pop-window.
	 */
	public Gin(String title) {
		this();
		if (title != null)
			super.setTitle(title);
	}

	/**
	 * Set the X value
	 * 
	 * @param d
	 *            The value to set it
	 */
	public void setXlabel(double d) {
		xlabel.setText(String.valueOf(d));
	}

	/**
	 * Set the Y value
	 * 
	 * @param d
	 *            The value to set it
	 */
	public void setYlabel(double d) {
		ylabel.setText(String.valueOf(d));
	}

	/**
	 * Set the both values
	 * 
	 * @param dx
	 *            The X value to set
	 * @param dy
	 *            The Y value to set
	 */
	public void setLabels(double dx, double dy) {
		xlabel.setText(String.valueOf(dx));
		ylabel.setText(String.valueOf(dy));
	}

	/**
	 * Set the display font
	 */
	public void setFont(Font f) {

		if (f == null)
			return;
		xlabel.setFont(f);
		ylabel.setFont(f);

	}

}
