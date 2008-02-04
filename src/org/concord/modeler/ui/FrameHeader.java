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

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;

import javax.swing.JPanel;

/**
 * @author Charles Xie
 * 
 */
class FrameHeader extends JPanel {

	private Runnable closeCode;
	private Runnable editCode;

	FrameHeader() {
		super();
		setOpaque(false);
		setPreferredSize(new Dimension(200, 12));
		addMouseListener(new MouseAdapter() {
			public void mouseReleased(MouseEvent e) {
				int x = e.getX();
				int w = getWidth();
				int h = getHeight();
				if (x > w - h && x < w) {
					if (closeCode != null)
						closeCode.run();
				}
				else if (x > w - 2 * h && x < w - h) {
					if (editCode != null)
						editCode.run();
				}
			}
		});
		addMouseMotionListener(new MouseMotionAdapter() {
			public void mouseMoved(MouseEvent e) {
				int x = e.getX();
				int w = getWidth();
				int h = getHeight();
				boolean b = false;
				if (editCode != null) {
					b = x > w - 2 * h && x < w - h;
				}
				if (!b) {
					if (closeCode != null)
						b = x > w - h && x < w;
				}
				setCursor(Cursor.getPredefinedCursor(b ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
			}
		});
	}

	void setCloseCode(Runnable r) {
		closeCode = r;
	}

	void setEditCode(Runnable r) {
		editCode = r;
	}

	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		int w = getWidth();
		int h = getHeight();
		int x = 0;
		if (closeCode != null) {
			g.setColor(Color.white);
			x = w - h;
			g.fill3DRect(x, 0, h, h, true);
			g.setColor(Color.gray);
			g.drawLine(x + 3, 3, x + h - 4, h - 4);
			g.drawLine(x + 3, h - 4, x + h - 4, 3);
		}
		if (editCode != null) {
			g.setColor(Color.white);
			x = w - 2 * h;
			g.fill3DRect(x, 0, h, h, true);
			g.setColor(Color.gray);
			g.drawLine(x + 4, h - 3, x + h - 3, 4);
			g.drawLine(x + 2, h - 5, x + h - 5, 2);
			g.drawLine(x + 4, h - 3, x + 2, h - 5);
			g.drawLine(x + h - 3, 4, x + h - 5, 2);
			g.drawLine(x + 4, h - 3, x + 2, h - 3);
			g.drawLine(x + 2, h - 5, x + 2, h - 3);
		}
	}

}