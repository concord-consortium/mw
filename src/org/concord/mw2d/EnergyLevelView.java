/*
 *   Copyright (C) 2007  The Concord Consortium, Inc.,
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

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.concord.mw2d.models.EnergyLevel;

/**
 * @author Charles Xie
 * 
 */
class EnergyLevelView implements Comparable {

	final static int THICKNESS = 3;
	private static DecimalFormat format;
	private EnergyLevel energyLevel;
	private Rectangle rect;
	private int cx, cy;
	private List<ElectronView> electronViewList;

	EnergyLevelView(EnergyLevel energyLevel) {
		this.energyLevel = energyLevel;
		rect = new Rectangle();
		if (format == null) {
			format = new DecimalFormat("##.##");
			format.setMinimumFractionDigits(2);
		}
	}

	EnergyLevel getModel() {
		return energyLevel;
	}

	void setRect(int x, int y, int w, int h) {
		rect.setRect(x, y, w, h);
		cx = x + w / 2;
		cy = y + h / 2;
		setElectronPositions();
	}

	int getWidth() {
		return rect.width;
	}

	int getHeight() {
		return rect.height;
	}

	void setX(int x) {
		rect.x = x;
		cx = x + rect.width / 2;
		setElectronPositions();
	}

	int getX() {
		return rect.x;
	}

	void setY(int y) {
		rect.y = y;
		cy = y + rect.height / 2;
		setElectronPositions();
	}

	int getY() {
		return rect.y;
	}

	boolean contains(int x, int y) {
		return rect.contains(x, y);
	}

	boolean contains(int x, int y, int dx, int dy) {
		return Math.abs(x - rect.x - rect.width / 2) < rect.width / 2 + dx
				&& Math.abs(y - rect.y - rect.height / 2) < rect.height / 2 + dy;
	}

	ElectronView whichElectron(int x, int y) {
		if (electronViewList == null)
			return null;
		synchronized (electronViewList) {
			for (ElectronView ev : electronViewList) {
				if (ev.contains(x, y))
					return ev;
			}
		}
		return null;
	}

	void removeAllElectrons() {
		if (electronViewList == null)
			return;
		electronViewList.clear();
	}

	void removeElectron(ElectronView e) {
		if (electronViewList == null)
			return;
		electronViewList.remove(e);
		setElectronPositions();
	}

	void addElectron(ElectronView e) {
		if (electronViewList == null)
			electronViewList = Collections.synchronizedList(new ArrayList<ElectronView>());
		if (!electronViewList.contains(e))
			electronViewList.add(e);
		setElectronPositions();
	}

	boolean hasElectron(ElectronView e) {
		if (electronViewList == null)
			return false;
		return electronViewList.contains(e);
	}

	private void setElectronPositions() {
		int n = electronViewList == null ? 0 : electronViewList.size();
		if (n > 0 && n <= rect.width / (2 * ElectronView.getRadius())) {
			int x0 = cx - ElectronView.getRadius() * n;
			synchronized (electronViewList) {
				for (int i = 0; i < n; i++) {
					electronViewList.get(i).setLocation(x0 + ElectronView.getRadius() * 2 * i,
							cy - ElectronView.getRadius());
				}
			}
		}
	}

	void paint(Graphics2D g, Color color, boolean selected) {

		g.setColor(color);
		g.fill(rect);
		if (selected) {
			int t2 = THICKNESS >> 1;
			int t1 = THICKNESS + 2;
			g.setColor(Color.white);
			g.fillRect(rect.x - t2, rect.y - t2, t1, t1);
			g.fillRect(rect.x + rect.width - t2, rect.y - t2, t1, t1);
			g.setColor(Color.black);
			g.drawRect(rect.x - t2, rect.y - t2, t1, t1);
			g.drawRect(rect.x + rect.width - t2, rect.y - t2, t1, t1);
		}
		g.drawString(format.format(energyLevel.getEnergy()), rect.x - ElectronicStructureViewer.H_MARGIN * 2, cy + 2
				* g.getFontMetrics().getHeight() / 5);

		int n = electronViewList == null ? 0 : electronViewList.size();
		if (n <= 0) {
			// no electron
		}
		else if (n <= rect.width / (ElectronView.getRadius() * 2)) {
			synchronized (electronViewList) {
				for (int i = 0; i < n; i++) {
					electronViewList.get(i).draw(g);
				}
			}
		}
		else {
			g.setColor(Color.gray);
			g.drawString(n + "", cx - 2, cy - 2);
		}

	}

	public int compareTo(Object o) {
		if (!(o instanceof EnergyLevelView))
			throw new IllegalArgumentException("o must be an EnergyLevelView");
		EnergyLevel level = ((EnergyLevelView) o).getModel();
		return energyLevel.compareTo(level);
	}

}
