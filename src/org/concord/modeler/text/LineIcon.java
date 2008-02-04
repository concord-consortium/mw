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
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.Icon;


/**
 * This class should have been named ColorBar. Its functionalities have been extended beyond drawing simple lines.
 * 
 * @author Charles Xie
 * 
 */

class LineIcon implements Icon {

	final static int UPPER_LEFT = 1;
	final static int LOWER_LEFT = 2;
	final static int UPPER_RIGHT = 4;
	final static int LOWER_RIGHT = 8;

	private Page page;
	private Color color;
	private Color defaultColor;
	private float width = 1.0f;
	private int height = 3;
	private int topMargin;
	private int bottomMargin;
	private int leftMargin;
	private int rightMargin;
	private int arcWidth, arcw2;
	private int arcHeight, arch2;
	private boolean filled = true;
	private int cornerArc;
	private String text;
	private IconWrapper wrapper;

	public LineIcon(Page page) {
		this.page = page;
		defaultColor = new Color(0xffffff ^ page.getBackground().getRGB());
	}

	public LineIcon(LineIcon lineIcon) {
		this(lineIcon.page);
		color = lineIcon.color;
		width = lineIcon.width;
		height = lineIcon.height;
		topMargin = lineIcon.topMargin;
		bottomMargin = lineIcon.bottomMargin;
		leftMargin = lineIcon.leftMargin;
		rightMargin = lineIcon.rightMargin;
		filled = lineIcon.filled;
		text = lineIcon.text;
		arcWidth = lineIcon.arcWidth;
		arcHeight = lineIcon.arcHeight;
		cornerArc = lineIcon.cornerArc;
	}

	public void setWrapper(IconWrapper ic) {
		wrapper = ic;
	}

	public IconWrapper getWrapper() {
		return wrapper;
	}

	public void setWidth(float width) {
		this.width = width;
	}

	public float getWidth() {
		return width;
	}

	public void setHeight(int i) {
		height = i;
	}

	public int getHeight() {
		return height;
	}

	public void setText(String s) {
		text = s;
		if (wrapper != null)
			wrapper.setText(s);
	}

	public String getText() {
		return text;
	}

	public void addCornerArc(int i) {
		cornerArc |= i;
	}

	public void removeCornerArc(int i) {
		cornerArc ^= i;
	}

	public void setCornerArc(int i) {
		cornerArc = i;
	}

	public int getCornerArc() {
		return cornerArc;
	}

	public void setTopMargin(int topMargin) {
		this.topMargin = topMargin;
	}

	public int getTopMargin() {
		return topMargin;
	}

	public void setBottomMargin(int bottomMargin) {
		this.bottomMargin = bottomMargin;
	}

	public int getBottomMargin() {
		return bottomMargin;
	}

	public void setLeftMargin(int i) {
		leftMargin = i;
	}

	public int getLeftMargin() {
		return leftMargin;
	}

	public void setRightMargin(int i) {
		rightMargin = i;
	}

	public int getRightMargin() {
		return rightMargin;
	}

	public void setArcWidth(int i) {
		arcWidth = i;
	}

	public int getArcWidth() {
		return arcWidth;
	}

	public void setArcHeight(int i) {
		arcHeight = i;
	}

	public int getArcHeight() {
		return arcHeight;
	}

	public void setFilled(boolean filled) {
		this.filled = filled;
	}

	public boolean isFilled() {
		return filled;
	}

	public void setColor(Color color) {
		this.color = color;
	}

	public Color getColor() {
		return color;
	}

	public void invert() {
		color = new Color(0xffffff ^ color.getRGB());
	}

	public void paintIcon(Component c, Graphics g, int x, int y) {
		if (color == null) {
			int i = 0xffffff ^ page.getBackground().getRGB();
			if (defaultColor.getRGB() != i)
				defaultColor = new Color(i);
			g.setColor(defaultColor);
		}
		else {
			g.setColor(color);
		}
		int w = getIconWidth();
		if (height > 1) {
			y += topMargin;
			if (filled) {
				if (arcWidth > 0 && arcHeight > 0) {
					Graphics2D g2 = (Graphics2D) g;
					g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
					arcw2 = arcWidth * 2;
					arch2 = arcHeight * 2;
					if ((cornerArc & UPPER_LEFT) == UPPER_LEFT) {
						g2.fillOval(x, y, arcw2, arch2);
					}
					else {
						g2.fillRect(x, y, arcWidth, arcHeight);
					}
					if ((cornerArc & UPPER_RIGHT) == UPPER_RIGHT) {
						g2.fillOval(x + w - arcw2, y, arcw2, arch2);
					}
					else {
						g2.fillRect(x + w - arcWidth, y, arcWidth, arcHeight);
					}
					if ((cornerArc & LOWER_LEFT) == LOWER_LEFT) {
						g2.fillOval(x, y + height - arch2, arcw2, arch2);
					}
					else {
						g2.fillRect(x, y + height - arcHeight, arcWidth, arcHeight);
					}
					if ((cornerArc & LOWER_RIGHT) == LOWER_RIGHT) {
						g2.fillOval(x + w - arcw2, y + height - arch2, arcw2, arch2);
					}
					else {
						g2.fillRect(x + w - arcWidth, y + height - arcHeight, arcWidth, arcHeight);
					}
					g2.fillRect(x + arcWidth, y, w - arcw2, height);
					g2.fillRect(x, y + arcHeight, w, height - arch2);
				}
				else {
					g.fillRect(x, y, w, height);
				}
			}
			else {
				if (arcWidth > 0 && arcHeight > 0) {
					Graphics2D g2 = (Graphics2D) g;
					g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
					arcw2 = arcWidth * 2;
					arch2 = arcHeight * 2;
					if ((cornerArc & UPPER_LEFT) == UPPER_LEFT) {
						g2.drawOval(x, y, arcw2, arch2);
					}
					else {
						g2.drawRect(x, y, arcWidth, arcHeight);
					}
					if ((cornerArc & UPPER_RIGHT) == UPPER_RIGHT) {
						g2.drawOval(x + w - arcw2, y, arcw2, arch2);
					}
					else {
						g2.drawRect(x + w - arcWidth, y, arcWidth, arcHeight);
					}
					if ((cornerArc & LOWER_LEFT) == LOWER_LEFT) {
						g2.drawOval(x, y + height - arch2, arcw2, arch2);
					}
					else {
						g2.drawRect(x, y + height - arcHeight, arcWidth, arcHeight);
					}
					if ((cornerArc & LOWER_RIGHT) == LOWER_RIGHT) {
						g2.drawOval(x + w - arcw2, y + height - arch2, arcw2, arch2);
					}
					else {
						g2.drawRect(x + w - arcWidth, y + height - arcHeight, arcWidth, arcHeight);
					}
					g2.drawRect(x + arcWidth, y, w - arcw2, height);
					g2.drawRect(x, y + arcHeight, w, height - arch2);
				}
				else {
					g.drawRect(x, y, w, height);
				}
			}
		}
		else {
			g.drawLine(x, y, x + w, y);
		}
	}

	public int getIconWidth() {
		if (width <= 1.05f)
			return (int) (page.getWidth() * width) - leftMargin - rightMargin;
		return (int) width;
	}

	public int getIconHeight() {
		return height + topMargin + bottomMargin;
	}

	public String toString() {
		StringBuffer sb = new StringBuffer("<class>" + getClass().getName() + "</class>");
		if (width != 1.0f)
			sb.append("<width>" + width + "</width>");
		if (height != 3)
			sb.append("<height>" + height + "</height>");
		if (arcWidth > 0)
			sb.append("<arcwidth>" + arcWidth + "</arcwidth>");
		if (arcHeight > 0)
			sb.append("<archeight>" + arcHeight + "</archeight>");
		if (cornerArc != 0)
			sb.append("<cornerarc>" + cornerArc + "</cornerarc>");
		if (!filled)
			sb.append("<opaque>false</opaque>");
		if (text != null && !text.trim().equals(""))
			sb.append("<title>" + XMLCharacterEncoder.encode(text) + "</title>");
		if (color != null)
			sb.append("<bgcolor>" + Integer.toString(color.getRGB(), 16) + "</bgcolor>");
		if (topMargin != 0)
			sb.append("<topmargin>" + topMargin + "</topmargin>");
		if (bottomMargin != 0)
			sb.append("<bottommargin>" + bottomMargin + "</bottommargin>");
		if (leftMargin > 0)
			sb.append("<leftmargin>" + leftMargin + "</leftmargin>");
		if (rightMargin > 0)
			sb.append("<rightmargin>" + rightMargin + "</rightmargin>");
		return sb.toString();
	}

}