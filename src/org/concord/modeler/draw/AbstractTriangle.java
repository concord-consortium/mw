/*
 *   Copyright (C) 2008  The Concord Consortium, Inc.,
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
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.Toolkit;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.net.MalformedURLException;
import java.net.URL;
import javax.swing.ImageIcon;

import org.concord.modeler.util.FileUtilities;

public abstract class AbstractTriangle implements DrawingElement {

	private static Rectangle[] handle;
	private final static Stroke THIN = new BasicStroke(1);

	private FillMode fillMode = FillMode.getNoFillMode();
	private ImageIcon bgImage;
	private Image fullImage;
	private Color lineColor = Color.red;
	private short alpha = 255;
	private byte lineWeight = 1;
	private byte lineStyle = LineStyle.STROKE_NUMBER_1;
	private BasicStroke stroke = new BasicStroke(lineWeight, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER);
	private Triangle2D triangle;
	private float angle;
	private boolean selected;
	private boolean selectionDrawn = true;
	private byte selectedHandle = (byte) -1;
	private Component component;

	protected AbstractTriangle() {
		triangle = new Triangle2D(0, 0, 1, 1, 2, 2);
	}

	public AbstractTriangle(TriangleState s) {
		this();
		triangle.setVertex(0, s.getXa(), s.getYa());
		triangle.setVertex(1, s.getXb(), s.getYb());
		triangle.setVertex(2, s.getXc(), s.getYc());
		angle = s.getAngle();
		setFillMode(s.getFillMode());
		setAlpha(s.getAlpha());
		setLineWeight(s.getLineWeight());
		setLineStyle(s.getLineStyle());
		setLineColor(s.getLineColor());
	}

	public AbstractTriangle(AbstractTriangle r) {
		this();
		component = r.component;
		set(r);
	}

	public void set(AbstractTriangle r) {
		setVertices(r);
		fillMode = r.fillMode;
		alpha = r.alpha;
		angle = r.angle;
		lineColor = r.lineColor;
		lineWeight = r.lineWeight;
		lineStyle = r.lineStyle;
		stroke = r.stroke;
	}

	private void setVertices(AbstractTriangle r) {
		for (int i = 0; i < 3; i++) {
			Point2D.Float p = r.triangle.getVertex(i);
			triangle.setVertex(0, p.x, p.y);
		}
	}

	protected abstract void attachToHost();

	protected abstract void setVisible(boolean b);

	protected abstract boolean isVisible();

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
		if (handle == null)
			handle = new Rectangle[] { new Rectangle(), new Rectangle(), new Rectangle() };
		for (int i = 0; i < 3; i++) {
			handle[i].x = Math.round(triangle.getVertex(i).x - 3);
			handle[i].y = Math.round(triangle.getVertex(i).y - 3);
		}
	}

	public boolean contains(double x, double y) {
		return triangle.contains(x, y);
	}

	/**
	 * @return the handle that is within 4 pixels of the input coordinate (0,1,2). If the coordinate is close to none,
	 *         return -1.
	 */
	public byte nearHandle(double x, double y) {
		for (byte i = 0; i < 3; i++) {
			double dx = triangle.getVertex(i).x - x;
			double dy = triangle.getVertex(i).y - y;
			if (dx * dx + dy * dy < 16)
				return i;
		}
		return -1;
	}

	public void translateTo(float x, float y) {
		Point2D.Float center = triangle.getCenter();
		x = x - center.x;
		y = y - center.y;
		triangle.translate(x, y);
		if (selected)
			setHandles();
	}

	public void setLocation(float x, float y) {
		translateTo(x, y);
	}

	public void translateBy(float dx, float dy) {
		triangle.translate(dx, dy);
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
		return (triangle.getVertex(0).x + triangle.getVertex(1).x + triangle.getVertex(2).x) / 3;
	}

	/** @return the y coordinate of the center of this element. */
	public double getRy() {
		return (triangle.getVertex(0).y + triangle.getVertex(1).y + triangle.getVertex(2).y) / 3;
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

	public Point2D.Float getVertext(int i) {
		return triangle.getVertex(i);
	}

	public void setVertex(int i, float x, float y) {
		triangle.setVertex(i, x, y);
		if (selected)
			setHandles();
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
					g2.fill(triangle);
				}
				else if (alpha > 0) {
					g2.setColor(new Color((alpha << 24)
							| (0x00ffffff & ((FillMode.ColorFill) fillMode).getColor().getRGB()), true));
					g2.fill(triangle);
				}
			}
			else if (fillMode instanceof FillMode.ImageFill) {
				if (bgImage != null) {
					Rectangle rect = triangle.getBounds();
					if (bgImage.getIconWidth() != rect.width || bgImage.getIconHeight() != rect.height)
						bgImage = new ImageIcon(fullImage.getScaledInstance(rect.width, rect.height,
								Image.SCALE_DEFAULT));
					bgImage.paintIcon(component, g, rect.x, rect.y);
				}
			}
			else if (fillMode instanceof FillMode.GradientFill) {
				FillMode.GradientFill gfm = (FillMode.GradientFill) fillMode;
				Color c1 = new Color((alpha << 24) | (0x00ffffff & gfm.getColor1().getRGB()), true);
				Color c2 = new Color((alpha << 24) | (0x00ffffff & gfm.getColor2().getRGB()), true);
				Rectangle rect = triangle.getBounds();
				GradientFactory.paintRect(g2, gfm.getStyle(), gfm.getVariant(), c1, c2, rect.x, rect.y, rect.width,
						rect.height);
			}
			else if (fillMode instanceof FillMode.PatternFill) {
				FillMode.PatternFill tfm = (FillMode.PatternFill) fillMode;
				Color c1 = new Color((alpha << 24) | (0x00ffffff & tfm.getForeground()), true);
				Color c2 = new Color((alpha << 24) | (0x00ffffff & tfm.getBackground()), true);
				Rectangle rect = triangle.getBounds();
				g2.setPaint(PatternFactory.createPattern(tfm.getStyle(), tfm.getCellWidth(), tfm.getCellHeight(), c1,
						c2));
				g2.fill(rect);
			}

			if (lineWeight > 0) {
				g.setColor(lineColor);
				g2.setStroke(stroke);
				g2.draw(triangle);
			}
		}

		g2.setTransform(at);

		if (selected && selectionDrawn) {
			g2.setStroke(THIN);
			for (int i = 0; i < 3; i++) {
				g2.setColor(Color.yellow);
				g2.fill(handle[i]);
				g2.setColor(Color.black);
				g2.draw(handle[i]);
			}
		}

		g.setColor(oldColor);
		g2.setStroke(oldStroke);

	}

}