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

package org.concord.mw3d;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Polygon;
import java.awt.Rectangle;

import org.concord.mw3d.models.MolecularModel;

class Energizer {

	int x, y;
	Rectangle exitButton, heatButton, coolButton;
	volatile boolean buttonPressed;

	private MolecularModel model;
	private int length = 100;
	private Color exitButtonColor = Color.lightGray;
	private Color heatButtonColor = Color.lightGray;
	private Color coolButtonColor = Color.lightGray;

	Energizer(int x, int y, int length, MolecularModel model) {
		this.x = x;
		this.y = y;
		this.length = length;
		this.model = model;
		exitButton = new Rectangle(x, y - 10, 10, 10);
		heatButton = new Rectangle(x, y + length, 10, 10);
		coolButton = new Rectangle(x, y + length + 10, 10, 10);
	}

	void energize(int x, int y) {
		final boolean heating = heatButton.contains(x, y);
		final boolean cooling = coolButton.contains(x, y);
		if (heating || cooling) {
			buttonPressed = true;
			Thread t = new Thread(new Runnable() {
				public void run() {
					while (buttonPressed) {
						if (heating) {
							heat();
						}
						else {
							cool();
						}
						model.getView().refresh();
						model.getView().repaint();
						try {
							Thread.sleep(50);
						}
						catch (InterruptedException e) {
							buttonPressed = false;
							return;
						}
					}
					model.notifyChange();
				}
			});
			t.setName("Quick Heater for 3D");
			t.setPriority(Thread.MIN_PRIORITY);
			t.start();
		}
		else {
			if (exitButton.contains(x, y)) {
				exit();
				buttonPressed = false;
			}
		}
	}

	void setX(int x) {
		this.x = exitButton.x = heatButton.x = coolButton.x = x;
	}

	void exit() {
		model.getView().setShowEnergizer(false);
	}

	void heat() {
		model.changeTemperature(0.1f);
	}

	void cool() {
		model.changeTemperature(-0.1f);
	}

	void mouseEntered(int ex, int ey) {
		if (exitButton.contains(ex, ey)) {
			exitButtonColor = Color.gray;
			coolButtonColor = heatButtonColor = Color.lightGray;
		}
		else if (heatButton.contains(ex, ey)) {
			heatButtonColor = Color.gray;
			coolButtonColor = exitButtonColor = Color.lightGray;
		}
		else if (coolButton.contains(ex, ey)) {
			coolButtonColor = Color.gray;
			heatButtonColor = exitButtonColor = Color.lightGray;
		}
		else {
			mouseExited();
		}
	}

	void mouseExited() {
		coolButtonColor = heatButtonColor = exitButtonColor = Color.lightGray;
	}

	void paint(Graphics g) {
		setX(model.getView().getWidth() - 18);
		int sk = (int) (100.0 * Math.log(1.0 + 5 * model.getKin()));
		g.setColor(exitButtonColor);
		g.fillRect(exitButton.x, exitButton.y, exitButton.width, exitButton.height);
		g.setColor(heatButtonColor);
		g.fillRect(heatButton.x, heatButton.y, heatButton.width, heatButton.height);
		g.setColor(coolButtonColor);
		g.fillRect(coolButton.x, coolButton.y, coolButton.width, coolButton.height);
		g.setColor(model.getView().contrastBackground());
		g.drawRect(exitButton.x, exitButton.y, exitButton.width, exitButton.height);
		g.drawRect(heatButton.x, heatButton.y, heatButton.width, heatButton.height);
		g.drawRect(coolButton.x, coolButton.y, coolButton.width, coolButton.height);
		g.setColor(Color.black);
		g.drawLine(exitButton.x, exitButton.y, exitButton.x + exitButton.width, exitButton.y + exitButton.height);
		g.drawLine(exitButton.x, exitButton.y + exitButton.height, exitButton.x + exitButton.width, exitButton.y);
		g.setColor(Color.red);
		Polygon poly = new Polygon();
		poly.addPoint(heatButton.x + heatButton.width / 2, heatButton.y + 2);
		poly.addPoint(heatButton.x + 2, heatButton.y + heatButton.height - 2);
		poly.addPoint(heatButton.x + heatButton.width - 2, heatButton.y + heatButton.height - 2);
		g.fillPolygon(poly);
		g.setColor(Color.blue);
		poly = new Polygon();
		poly.addPoint(coolButton.x + coolButton.width / 2, coolButton.y + coolButton.height - 2);
		poly.addPoint(coolButton.x + 2, coolButton.y + 2);
		poly.addPoint(coolButton.x + coolButton.width - 2, coolButton.y + 2);
		g.fillPolygon(poly);
		g.setColor(Color.white);
		g.fillRect(x, y, 10, length);
		g.setColor(model.getView().contrastBackground());
		g.drawRect(x, y, 10, length);
		g.setColor(Color.red);
		g.fillRect(x + 1, y + length - sk, 9, sk);
	}

}
