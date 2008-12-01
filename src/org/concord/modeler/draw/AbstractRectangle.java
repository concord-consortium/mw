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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.Toolkit;
import java.awt.geom.AffineTransform;
import java.awt.geom.RoundRectangle2D;
import java.net.MalformedURLException;
import java.net.URL;
import javax.swing.ImageIcon;

import org.concord.modeler.util.FileUtilities;

public abstract class AbstractRectangle implements DrawingElement {

	public final static byte UPPER_LEFT = 0x00;
	public final static byte UPPER_RIGHT = 0x01;
	public final static byte LOWER_RIGHT = 0x02;
	public final static byte LOWER_LEFT = 0x03;
	public final static byte TOP = 0x04;
	public final static byte RIGHT = 0x05;
	public final static byte BOTTOM = 0x06;
	public final static byte LEFT = 0x07;
	public final static byte ARC_HANDLE = 0x08;

	private static Rectangle handleNW, handleNE, handleSE, handleSW;
	private static Rectangle handleN, handleE, handleS, handleW;
	private static Polygon handleArc;
	private final static Stroke THIN = new BasicStroke(1);

	private FillMode fillMode = FillMode.getNoFillMode();
	private ImageIcon bgImage;
	private Image fullImage;
	private Color lineColor = Color.red;
	private short alpha = 255;
	private byte lineWeight = 1;
	private byte lineStyle = LineStyle.STROKE_NUMBER_1;
	private BasicStroke stroke = new BasicStroke(lineWeight, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER);
	private RoundRectangle2D.Float rect;
	private float angle;
	private boolean selected;
	private boolean selectionDrawn = true;
	private byte selectedHandle = (byte) -1;
	private Component component;

	protected AbstractRectangle() {
		rect = new RoundRectangle2D.Float();
	}

	public AbstractRectangle(RectangleState s) {
		this();
		rect.x = s.getX();
		rect.y = s.getY();
		rect.width = s.getWidth();
		rect.height = s.getHeight();
		rect.arcwidth = s.getArcWidth();
		rect.archeight = s.getArcHeight();
		angle = s.getAngle();
		setFillMode(s.getFillMode());
		setAlpha(s.getAlpha());
		setLineWeight(s.getLineWeight());
		setLineStyle(s.getLineStyle());
		setLineColor(s.getLineColor());
	}

	public AbstractRectangle(AbstractRectangle r) {
		this();
		component = r.component;
		set(r);
	}

	public Shape getBounds() {
		return rect;
	}

	public void setRect(AbstractRectangle r) {
		rect.setFrame(r.rect.getFrame());
	}

	public void set(AbstractRectangle r) {
		setRect(r);
		rect.arcwidth = r.getArcWidth();
		rect.archeight = r.getArcHeight();
		fillMode = r.fillMode;
		alpha = r.alpha;
		angle = r.angle;
		lineColor = r.lineColor;
		lineWeight = r.lineWeight;
		lineStyle = r.lineStyle;
		stroke = r.stroke;
	}

	protected abstract void attachToHost();

	protected abstract void setVisible(boolean b);

	protected abstract boolean isVisible();

	public boolean intersects(Rectangle r) {
		return rect.intersects(r);
	}

	/**
	 * set the UI component whose graphics context this rectangle will be drawn upon.
	 */
	public void setComponent(Component c) {
		component = c;
	}

	public Component getComponent() {
		return component;
	}

	public void setSelected(boolean b) {
		selected = b;
		if (selected) {
			setHandles();
		}
		else {
			selectedHandle = -1;
		}
	}

	public boolean isSelected() {
		return selected;
	}

	public void setSelectionDrawn(boolean b) {
		selectionDrawn = b;
	}

	public boolean isSelectionDrawn() {
		return selectionDrawn;
	}

	public void setSelectedHandle(byte i) {
		selectedHandle = i;
	}

	public byte getSelectedHandle() {
		return selectedHandle;
	}

	private void setHandles() {
		if (handleArc == null)
			handleArc = new Polygon();
		else handleArc.reset();
		float x0 = rect.x + rect.arcwidth * 0.5f;
		handleArc.addPoint(Math.round(x0 - 4), Math.round(rect.y - 14));
		handleArc.addPoint(Math.round(x0), Math.round(rect.y - 18));
		handleArc.addPoint(Math.round(x0 + 4), Math.round(rect.y - 14));
		handleArc.addPoint(Math.round(x0), Math.round(rect.y - 10));
		if (handleNW == null) {
			handleNW = new Rectangle(Math.round(rect.x - 3), Math.round(rect.y - 3), 6, 6);
		}
		else {
			handleNW.x = Math.round(rect.x - 3);
			handleNW.y = Math.round(rect.y - 3);
		}
		if (handleNE == null) {
			handleNE = new Rectangle(Math.round(rect.x + rect.width - 3), Math.round(rect.y - 3), 6, 6);
		}
		else {
			handleNE.x = Math.round(rect.x + rect.width - 3);
			handleNE.y = Math.round(rect.y - 3);
		}
		if (handleSE == null) {
			handleSE = new Rectangle(Math.round(rect.x + rect.width - 3), Math.round(rect.y + rect.height - 3), 6, 6);
		}
		else {
			handleSE.x = Math.round(rect.x + rect.width - 3);
			handleSE.y = Math.round(rect.y + rect.height - 3);
		}
		if (handleSW == null) {
			handleSW = new Rectangle(Math.round(rect.x - 3), Math.round(rect.y + rect.height - 3), 6, 6);
		}
		else {
			handleSW.x = Math.round(rect.x - 3);
			handleSW.y = Math.round(rect.y + rect.height - 3);
		}
		if (handleN == null) {
			handleN = new Rectangle(Math.round(rect.x + rect.width / 2 - 3), Math.round(rect.y - 3), 6, 6);
		}
		else {
			handleN.x = Math.round(rect.x + rect.width / 2 - 3);
			handleN.y = Math.round(rect.y - 3);
		}
		if (handleE == null) {
			handleE = new Rectangle(Math.round(rect.x + rect.width - 3), Math.round(rect.y + rect.height / 2 - 3), 6, 6);
		}
		else {
			handleE.x = Math.round(rect.x + rect.width - 3);
			handleE.y = Math.round(rect.y + rect.height / 2 - 3);
		}
		if (handleS == null) {
			handleS = new Rectangle(Math.round(rect.x + rect.width / 2 - 3), Math.round(rect.y + rect.height - 3), 6, 6);
		}
		else {
			handleS.x = Math.round(rect.x + rect.width / 2 - 3);
			handleS.y = Math.round(rect.y + rect.height - 3);
		}
		if (handleW == null) {
			handleW = new Rectangle(Math.round(rect.x - 3), Math.round(rect.y + rect.height / 2 - 3), 6, 6);
		}
		else {
			handleW.x = Math.round(rect.x - 3);
			handleW.y = Math.round(rect.y + rect.height / 2 - 3);
		}
	}

	public boolean contains(double x, double y) {
		return rect.contains(x, y);
	}

	/**
	 * @return the handle that is within 4 pixels of the input coordinate (0,1,2,3). If the coordinate is close to none,
	 *         return -1.
	 */
	public byte nearHandle(double x, double y) {
		double dx = rect.x - x;
		double dy = rect.y - y;
		if (dx * dx + dy * dy < 16)
			return UPPER_LEFT;
		dx = rect.x + rect.width - x;
		dy = rect.y - y;
		if (dx * dx + dy * dy < 16)
			return UPPER_RIGHT;
		dx = rect.x + rect.width - x;
		dy = rect.y + rect.height - y;
		if (dx * dx + dy * dy < 16)
			return LOWER_RIGHT;
		dx = rect.x - x;
		dy = rect.y + rect.height - y;
		if (dx * dx + dy * dy < 16)
			return LOWER_LEFT;
		dx = rect.x + rect.width / 2 - x;
		dy = rect.y - y;
		if (dx * dx + dy * dy < 16)
			return TOP;
		dx = rect.x + rect.width - x;
		dy = rect.y + rect.height / 2 - y;
		if (dx * dx + dy * dy < 16)
			return RIGHT;
		dx = rect.x + rect.width / 2 - x;
		dy = rect.y + rect.height - y;
		if (dx * dx + dy * dy < 16)
			return BOTTOM;
		dx = rect.x - x;
		dy = rect.y + rect.height / 2 - y;
		if (dx * dx + dy * dy < 16)
			return LEFT;
		if (handleArc != null && handleArc.contains(x, y))
			return ARC_HANDLE;
		return -1;
	}

	public void translateTo(double x, double y) {
		x = x - (rect.x + rect.width * 0.5);
		y = y - (rect.y + rect.height * 0.5);
		rect.x += x;
		rect.y += y;
		if (selected)
			setHandles();
	}

	public void setLocation(double x, double y) {
		translateTo(x, y);
	}

	public void translateBy(double dx, double dy) {
		rect.x += dx;
		rect.y += dy;
		if (selected)
			setHandles();
	}

	public void setLineColor(Color color) {
		lineColor = color;
	}

	public Color getLineColor() {
		return lineColor;
	}

	public void setFillMode(FillMode fm) {
		fillMode = fm;
		if (fillMode instanceof FillMode.ImageFill) {
			String s = ((FillMode.ImageFill) fillMode).getURL();
			if (FileUtilities.isRemote(s)) {
				try {
					bgImage = new ImageIcon(new URL(s));
				}
				catch (MalformedURLException e) {
					e.printStackTrace(System.err);
				}
				fullImage = bgImage.getImage();
			}
			else {
				fullImage = Toolkit.getDefaultToolkit().createImage(s);
				bgImage = new ImageIcon(fullImage);
			}
		}
		else {
			bgImage = null;
			fullImage = null;
		}
	}

	public FillMode getFillMode() {
		return fillMode;
	}

	public void setAlpha(short i) {
		alpha = i;
	}

	public short getAlpha() {
		return alpha;
	}

	public void setLineWeight(byte i) {
		lineWeight = i;
		stroke = new BasicStroke(lineWeight, stroke.getEndCap(), stroke.getLineJoin(), stroke.getMiterLimit(), stroke
				.getDashArray(), stroke.getDashPhase());
	}

	public byte getLineWeight() {
		return lineWeight;
	}

	/** @see org.concord.modeler.ui.LineStyle */
	public void setLineStyle(byte i) {
		lineStyle = i;
		stroke = StrokeFactory.changeStyle(stroke, LineStyle.STROKES[i].getDashArray());
	}

	public byte getLineStyle() {
		return lineStyle;
	}

	public void setStroke(BasicStroke stroke) {
		this.stroke = stroke;
	}

	public BasicStroke getStroke() {
		return stroke;
	}

	/** @return the x coordinate of the center of this element. */
	public double getRx() {
		return rect.x + 0.5 * rect.width;
	}

	/** @return the y coordinate of the center of this element. */
	public double getRy() {
		return rect.y + 0.5 * rect.height;
	}

	public Point getCenter() {
		return new Point((int) getRx(), (int) getRy());
	}

	public void setAngle(float angle) {
		this.angle = angle;
	}

	public float getAngle() {
		return angle;
	}

	public float getX() {
		return rect.x;
	}

	public void setX(float x) {
		rect.x = x;
		if (selected)
			setHandles();
	}

	public float getY() {
		return rect.y;
	}

	public void setY(float y) {
		rect.y = y;
		if (selected)
			setHandles();
	}

	public void setWidth(float w) {
		rect.width = w;
		if (selected)
			setHandles();
	}

	public int getWidth() {
		return Math.round(rect.width);
	}

	public void setHeight(float h) {
		rect.height = h;
		if (selected)
			setHandles();
	}

	public int getHeight() {
		return Math.round(rect.height);
	}

	public void setSize(float w, float h) {
		rect.width = w;
		rect.height = h;
		if (selected)
			setHandles();
	}

	public void setRect(float x, float y, float w, float h) {
		rect.x = x;
		rect.y = y;
		rect.width = w;
		rect.height = h;
		if (selected)
			setHandles();
	}

	public void setArcWidth(float arcWidth) {
		rect.arcwidth = arcWidth;
		if (selected)
			setHandles();
	}

	public float getArcWidth() {
		return rect.arcwidth;
	}

	public void setArcHeight(float arcHeight) {
		rect.archeight = arcHeight;
		if (selected)
			setHandles();
	}

	public float getArcHeight() {
		return rect.archeight;
	}

	public void paint(Graphics g) {

		Graphics2D g2 = (Graphics2D) g;

		Color oldColor = g.getColor();
		Stroke oldStroke = g2.getStroke();

		AffineTransform at = g2.getTransform();
		if (angle != 0)
			g2.rotate(angle * Math.PI / 180.0, getRx(), getRy());

		attachToHost();

		if (isVisible()) {
			if (fillMode instanceof FillMode.ColorFill) {
				if (alpha == 255) {
					g2.setColor(((FillMode.ColorFill) fillMode).getColor());
					g2.fill(rect);
				}
				else if (alpha > 0) {
					g2.setColor(new Color((alpha << 24)
							| (0x00ffffff & ((FillMode.ColorFill) fillMode).getColor().getRGB()), true));
					g2.fill(rect);
				}
			}
			else if (fillMode instanceof FillMode.ImageFill) {
				if (bgImage != null) {
					if (bgImage.getIconWidth() != rect.width || bgImage.getIconHeight() != rect.height)
						bgImage = new ImageIcon(fullImage.getScaledInstance(Math.round(rect.width), Math
								.round(rect.height), Image.SCALE_DEFAULT));
					bgImage.paintIcon(component, g, Math.round(rect.x), Math.round(rect.y));
				}
			}
			else if (fillMode instanceof FillMode.GradientFill) {
				FillMode.GradientFill gfm = (FillMode.GradientFill) fillMode;
				Color c1 = new Color((alpha << 24) | (0x00ffffff & gfm.getColor1().getRGB()), true);
				Color c2 = new Color((alpha << 24) | (0x00ffffff & gfm.getColor2().getRGB()), true);
				GradientFactory.paintRect(g2, gfm.getStyle(), gfm.getVariant(), c1, c2, rect.x, rect.y, rect.width,
						rect.height);
			}
			else if (fillMode instanceof FillMode.PatternFill) {
				FillMode.PatternFill tfm = (FillMode.PatternFill) fillMode;
				Color c1 = new Color((alpha << 24) | (0x00ffffff & tfm.getForeground()), true);
				Color c2 = new Color((alpha << 24) | (0x00ffffff & tfm.getBackground()), true);
				g2.setPaint(PatternFactory.createPattern(tfm.getStyle(), tfm.getCellWidth(), tfm.getCellHeight(), c1,
						c2));
				g2.fill(rect);
			}

			if (lineWeight > 0) {
				g.setColor(lineColor);
				g2.setStroke(stroke);
				g2.draw(rect);
			}
		}

		g2.setTransform(at);

		if (selected && selectionDrawn) {
			g2.setStroke(THIN);
			g2.setColor(Color.pink);
			g2.fill(handleArc);
			g2.setColor(Color.yellow);
			g2.fill(handleNW);
			g2.fill(handleNE);
			g2.fill(handleSE);
			g2.fill(handleSW);
			g2.fill(handleN);
			g2.fill(handleE);
			g2.fill(handleS);
			g2.fill(handleW);
			g2.setColor(Color.black);
			g2.draw(handleArc);
			g2.draw(handleNW);
			g2.draw(handleNE);
			g2.draw(handleSE);
			g2.draw(handleSW);
			g2.draw(handleN);
			g2.draw(handleE);
			g2.draw(handleS);
			g2.draw(handleW);
		}

		g.setColor(oldColor);
		g2.setStroke(oldStroke);

	}

}