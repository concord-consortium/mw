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

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JComponent;

class PreviewContainer extends JComponent {

	private static final byte H_GAP = 16;
	private static final byte V_GAP = 10;

	public PreviewContainer() {
		setBackground(Color.gray);
		addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				requestFocusInWindow();
			}
		});
	}

	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		g.setColor(getBackground());
		g.fillRect(0, 0, getWidth(), getHeight());
	}

	public Dimension getPreferredSize() {

		int n = getComponentCount();
		if (n == 0)
			return new Dimension(H_GAP, V_GAP);
		Component comp = getComponent(0);
		Dimension dc = comp.getPreferredSize();
		int w = dc.width;
		int h = dc.height;

		Dimension dp = getParent().getSize();
		int nCol = Math.max((dp.width - H_GAP) / (w + H_GAP), 1);
		int nRow = n / nCol;
		if (nRow * nCol < n)
			nRow++;

		int ww = nCol * (w + H_GAP) + H_GAP;
		int hh = nRow * (h + V_GAP) + V_GAP;
		Insets ins = getInsets();

		return new Dimension(ww + ins.left + ins.right, hh + ins.top + ins.bottom);

	}

	public void doLayout() {

		Insets ins = getInsets();
		int x = ins.left + H_GAP;
		int y = ins.top + V_GAP;
		int n = getComponentCount();
		if (n == 0)
			return;

		Component comp = getComponent(0);
		Dimension dc = comp.getPreferredSize();
		int w = dc.width;
		int h = dc.height;
		Dimension dp = getParent().getSize();

		int fit = (dp.width - H_GAP) / (w + H_GAP);
		int margin = dp.width - H_GAP - fit * (w + H_GAP);
		if (n < fit)
			margin += (fit - n) * (w + H_GAP);
		x += margin >> 1;

		int nCol = Math.max(fit, 1);
		int nRow = n / nCol;
		if (nRow * nCol < n)
			nRow++;

		int index = 0;
		for (int k = 0; k < nRow; k++) {
			for (int m = 0; m < nCol; m++) {
				if (index >= n)
					return;
				comp = getComponent(index++);
				comp.setBounds(x, y, w, h);
				x += w + H_GAP;
			}
			y += h + V_GAP;
			x = ins.left + H_GAP + (margin >> 1);
		}

	}

}