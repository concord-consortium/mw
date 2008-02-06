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
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;

import javax.swing.Icon;
import javax.swing.ImageIcon;

/** A type-safe interface for bullet icons */

public abstract class BulletIcon implements Icon {

	public final static byte OPEN_SQUARE_BULLET = 11;
	public final static byte SOLID_SQUARE_BULLET = 12;
	public final static byte OPEN_CIRCLE_BULLET = 13;
	public final static byte SOLID_CIRCLE_BULLET = 14;
	public final static byte POS_TICK_BULLET = 15;
	public final static byte NEG_TICK_BULLET = 16;
	public final static byte DIAMOND_BULLET = 17;
	public final static byte NO_BULLET = 18;
	public final static byte NUMBER = 19;

	protected int w = 8, h = 8;

	public abstract byte getType();

	public void setIconWidth(int width) {
		w = width;
	}

	public int getIconWidth() {
		return w;
	}

	public void setIconHeight(int height) {
		h = height;
	}

	public int getIconHeight() {
		return h;
	}

	public BulletIcon getScaledInstance(float scale) {
		try {
			BulletIcon icon = getClass().newInstance();
			icon.setIconWidth((int) (scale * icon.getIconWidth()));
			icon.setIconHeight((int) (scale * icon.getIconHeight()));
			return icon;
		}
		catch (InstantiationException e) {
			e.printStackTrace(System.err);
			return null;
		}
		catch (IllegalAccessException e) {
			e.printStackTrace(System.err);
			return null;
		}
	}

	public final static BulletIcon get(int type) {
		switch (type) {
		case OPEN_SQUARE_BULLET:
			return SquareBulletIcon.sharedInstance();
		case SOLID_SQUARE_BULLET:
			return SolidSquareBulletIcon.sharedInstance();
		case OPEN_CIRCLE_BULLET:
			return OpenCircleBulletIcon.sharedInstance();
		case SOLID_CIRCLE_BULLET:
			return SolidCircleBulletIcon.sharedInstance();
		case POS_TICK_BULLET:
			return PosTickBulletIcon.sharedInstance();
		case NEG_TICK_BULLET:
			return NegTickBulletIcon.sharedInstance();
		case DIAMOND_BULLET:
			return DiamondBulletIcon.sharedInstance();
		default:
			return null;
		}
	}

	static class SquareBulletIcon extends BulletIcon {

		private final static SquareBulletIcon instance = new SquareBulletIcon();

		public static BulletIcon sharedInstance() {
			return instance;
		}

		public void paintIcon(Component c, Graphics g, int x, int y) {
			g.setColor(Color.white);
			g.fillRect(x, y, w - 1, h - 1);
			g.setColor(Color.black);
			g.drawRect(x, y, w - 1, h - 1);
			g.drawLine(x, y + h - 2, x + w - 1, y + h - 2);
			g.drawLine(x + w - 2, y, x + w - 2, y + h - 1);
		}

		public byte getType() {
			return OPEN_SQUARE_BULLET;
		}

	}

	static class SolidSquareBulletIcon extends BulletIcon {

		private final static SolidSquareBulletIcon instance = new SolidSquareBulletIcon();

		public static BulletIcon sharedInstance() {
			return instance;
		}

		public void paintIcon(Component c, Graphics g, int x, int y) {
			g.setColor(Color.black);
			g.fillRect(x + w / 6, y + h / 6, 2 * w / 3, 2 * h / 3);
		}

		public byte getType() {
			return SOLID_SQUARE_BULLET;
		}

	}

	static class OpenCircleBulletIcon extends BulletIcon {

		private final static OpenCircleBulletIcon instance = new OpenCircleBulletIcon();

		public static BulletIcon sharedInstance() {
			return instance;
		}

		public void paintIcon(Component c, Graphics g, int x, int y) {
			g.setColor(Color.black);
			g.drawOval(x + w / 4 - 1, y + h / 4 - 1, w / 2 + 2, h / 2 + 2);
		}

		public byte getType() {
			return OPEN_CIRCLE_BULLET;
		}

	}

	static class SolidCircleBulletIcon extends BulletIcon {

		private final static SolidCircleBulletIcon instance = new SolidCircleBulletIcon();

		public static BulletIcon sharedInstance() {
			return instance;
		}

		public void paintIcon(Component c, Graphics g, int x, int y) {
			g.setColor(Color.black);
			g.fillOval(x + w / 4, y + h / 4, w / 2, h / 2);
		}

		public byte getType() {
			return SOLID_CIRCLE_BULLET;
		}

	}

	static class PosTickBulletIcon extends BulletIcon {

		private final static PosTickBulletIcon instance = new PosTickBulletIcon();
		private static ImageIcon posTickImage;

		PosTickBulletIcon() {
			if (posTickImage == null)
				posTickImage = new ImageIcon(getClass().getResource("images/PosTick.gif"));
		}

		public static BulletIcon sharedInstance() {
			return instance;
		}

		public void paintIcon(Component c, Graphics g, int x, int y) {
			posTickImage.paintIcon(c, g, x, y);
		}

		public byte getType() {
			return POS_TICK_BULLET;
		}

	}

	static class NegTickBulletIcon extends BulletIcon {

		private final static NegTickBulletIcon instance = new NegTickBulletIcon();
		private static ImageIcon negTickImage;

		NegTickBulletIcon() {
			if (negTickImage == null)
				negTickImage = new ImageIcon(getClass().getResource("images/NegTick.gif"));
		}

		public static BulletIcon sharedInstance() {
			return instance;
		}

		public void paintIcon(Component c, Graphics g, int x, int y) {
			negTickImage.paintIcon(c, g, x, y);
		}

		public byte getType() {
			return NEG_TICK_BULLET;
		}

	}

	static class DiamondBulletIcon extends BulletIcon {

		private final static DiamondBulletIcon instance = new DiamondBulletIcon();
		private static ImageIcon diamondImage;

		DiamondBulletIcon() {
			if (diamondImage == null)
				diamondImage = new ImageIcon(getClass().getResource("images/Diamond.gif"));
		}

		public static BulletIcon sharedInstance() {
			return instance;
		}

		public void paintIcon(Component c, Graphics g, int x, int y) {
			diamondImage.paintIcon(c, g, x, y);
		}

		public byte getType() {
			return DIAMOND_BULLET;
		}

	}

	static class NumberIcon extends BulletIcon {

		private Font font = new Font("Arial", Font.PLAIN, 10);
		private Color foreground = Color.black;
		private Color background = Color.white;
		private int number;

		NumberIcon(Font f, Color bg, Color fg, int n) {
			if (f != null)
				font = f;
			if (fg != null)
				foreground = fg;
			if (bg != null)
				background = bg;
			number = n;
		}

		public void paintIcon(Component c, Graphics g, int x, int y) {
			g.setColor(background);
			g.fillRect(x, y, w, h);
			g.setColor(foreground);
			g.setFont(font);
			FontMetrics fm = g.getFontMetrics();
			g.drawString(number + ".", x, y + fm.getAscent() / 2);
		}

		public void setNumber(int n) {
			number = n;
		}

		public int getNumber() {
			return number;
		}

		public byte getType() {
			return NUMBER;
		}

	}

	static class ImageNotFoundIcon implements Icon {

		private final static ImageNotFoundIcon instance = new ImageNotFoundIcon();

		private ImageNotFoundIcon() {
		}

		public static ImageNotFoundIcon sharedInstance() {
			return instance;
		}

		public void paintIcon(Component c, Graphics g, int x, int y) {
			int w = getIconWidth();
			int h = getIconHeight();
			g.setColor(Color.white);
			g.fillRect(x, y, w - 1, h - 1);
			g.setColor(Color.black);
			g.drawRect(x, y, w - 1, h - 1);
			g.drawRect(x + 3, y + 3, w - 7, h - 7);
			int xc = x + w / 2;
			int yc = y + h / 2;
			g.drawLine(xc - 2, yc - 2, xc + 2, yc + 2);
			g.drawLine(xc - 2, yc + 2, xc + 2, yc - 2);
			g.setColor(Color.gray);
			g.drawLine(x + w, y, x + w, y + h);
			g.drawLine(x, y + h, x + h, y + h);
		}

		public int getIconWidth() {
			return 16;
		}

		public int getIconHeight() {
			return 16;
		}

	}

}