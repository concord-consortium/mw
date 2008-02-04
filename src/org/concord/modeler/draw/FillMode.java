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

package org.concord.modeler.draw;

import java.awt.Color;
import java.awt.Paint;
import java.io.Serializable;

import org.concord.modeler.util.HashCodeUtil;

/**
 * Type-safe class for background filling mode.
 * 
 * @author Charles Xie
 */

public abstract class FillMode implements Serializable {

	private final static FillMode NO_FILL = new NoFill();

	public static FillMode getNoFillMode() {
		return NO_FILL;
	}

	private static class NoFill extends FillMode {
	}

	public static class ColorFill extends FillMode {

		private Color color;

		public ColorFill() {
		}

		public ColorFill(Color c) {
			setColor(c);
		}

		public Color getColor() {
			return color;
		}

		public void setColor(Color c) {
			color = c;
		}

		public boolean equals(Object obj) {
			if (!(obj instanceof ColorFill))
				return false;
			Color c = ((ColorFill) obj).getColor();
			return color.equals(c);
		}

		public int hashCode() {
			return color.hashCode();
		}

		public String toString() {
			return color.toString();
		}

	}

	public static class ImageFill extends FillMode {

		private String url;

		public ImageFill() {
		}

		public ImageFill(String s) {
			setURL(s);
		}

		public String getURL() {
			return url;
		}

		public void setURL(String s) {
			url = s;
		}

		public boolean equals(Object obj) {
			if (!(obj instanceof ImageFill))
				return false;
			String s = ((ImageFill) obj).getURL();
			return url.equals(s);
		}

		public int hashCode() {
			return url.hashCode();
		}

		public String toString() {
			return url;
		}

	}

	public static class GradientFill extends FillMode {

		private Color color1;
		private Color color2;
		private int style;
		private int variant;

		public GradientFill() {
		}

		public GradientFill(Color color1, Color color2, int style, int variant) {
			this.color1 = color1;
			this.color2 = color2;
			this.style = style;
			this.variant = variant;
		}

		public Color getColor1() {
			return color1;
		}

		public void setColor1(Color c) {
			color1 = c;
		}

		public Color getColor2() {
			return color2;
		}

		public void setColor2(Color c) {
			color2 = c;
		}

		public int getStyle() {
			return style;
		}

		public void setStyle(int i) {
			style = i;
		}

		public int getVariant() {
			return variant;
		}

		public void setVariant(int i) {
			variant = i;
		}

		public boolean equals(Object obj) {
			if (!(obj instanceof GradientFill))
				return false;
			Color c = ((GradientFill) obj).getColor1();
			if (!c.equals(color1))
				return false;
			c = ((GradientFill) obj).getColor2();
			if (!c.equals(color2))
				return false;
			int i = ((GradientFill) obj).getStyle();
			if (i != style)
				return false;
			i = ((GradientFill) obj).getVariant();
			return i == variant;
		}

		public int hashCode() {
			int result = HashCodeUtil.SEED;
			result = HashCodeUtil.hash(result, color1);
			result = HashCodeUtil.hash(result, color2);
			result = HashCodeUtil.hash(result, style);
			result = HashCodeUtil.hash(result, variant);
			return result;
		}

		public String toString() {
			return "Gradient";
		}

	}

	public static class PatternFill extends FillMode {

		private int fgColor = 0xff000000;
		private int bgColor = 0xffffffff;
		private byte type;
		private int cellWidth;
		private int cellHeight;
		private transient Paint texturePaint;

		public PatternFill() {
		}

		public PatternFill(int fgColor, int bgColor, byte type, int cellWidth, int cellHeight) {
			this.fgColor = fgColor;
			this.bgColor = bgColor;
			this.type = type;
			this.cellWidth = cellWidth;
			this.cellHeight = cellHeight;
			texturePaint = PatternFactory.createPattern(type, cellWidth, cellHeight, new Color(fgColor), new Color(
					bgColor));
		}

		public Paint getPaint() {
			return texturePaint;
		}

		public byte getStyle() {
			return type;
		}

		public void setStyle(byte i) {
			type = i;
			if (cellWidth <= 0 || cellHeight <= 0)
				return;
			texturePaint = PatternFactory.createPattern(type, cellWidth, cellHeight, new Color(fgColor), new Color(
					bgColor));
		}

		public int getCellWidth() {
			return cellWidth;
		}

		public void setCellWidth(int w) {
			cellWidth = w;
			if (cellWidth <= 0 || cellHeight <= 0)
				return;
			texturePaint = PatternFactory.createPattern(type, cellWidth, cellHeight, new Color(fgColor), new Color(
					bgColor));
		}

		public int getCellHeight() {
			return cellHeight;
		}

		public void setCellHeight(int h) {
			cellHeight = h;
			if (cellWidth <= 0 || cellHeight <= 0)
				return;
			texturePaint = PatternFactory.createPattern(type, cellWidth, cellHeight, new Color(fgColor), new Color(
					bgColor));
		}

		public int getBackground() {
			return bgColor;
		}

		public void setBackground(int c) {
			bgColor = c;
			if (cellWidth <= 0 || cellHeight <= 0)
				return;
			texturePaint = PatternFactory.createPattern(type, cellWidth, cellHeight, new Color(fgColor), new Color(
					bgColor));
		}

		public int getForeground() {
			return fgColor;
		}

		public void setForeground(int c) {
			fgColor = c;
			if (cellWidth <= 0 || cellHeight <= 0)
				return;
			texturePaint = PatternFactory.createPattern(type, cellWidth, cellHeight, new Color(fgColor), new Color(
					bgColor));
		}

		public boolean equals(Object obj) {
			if (!(obj instanceof PatternFill))
				return false;
			int i = ((PatternFill) obj).getForeground();
			if (i != fgColor)
				return false;
			i = ((PatternFill) obj).getBackground();
			if (i != bgColor)
				return false;
			i = ((PatternFill) obj).getStyle();
			if (i != type)
				return false;
			i = ((PatternFill) obj).getCellWidth();
			if (i != cellWidth)
				return false;
			i = ((PatternFill) obj).getCellHeight();
			return i == cellHeight;
		}

		public int hashCode() {
			int result = HashCodeUtil.SEED;
			result = HashCodeUtil.hash(result, fgColor);
			result = HashCodeUtil.hash(result, bgColor);
			result = HashCodeUtil.hash(result, type);
			result = HashCodeUtil.hash(result, cellWidth);
			result = HashCodeUtil.hash(result, cellHeight);
			return result;
		}

		public String toString() {
			return "Pattern";
		}

	}

}
