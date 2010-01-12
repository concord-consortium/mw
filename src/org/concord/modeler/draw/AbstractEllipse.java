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

package org.concord.modeler.draw;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Paint;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.Toolkit;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.net.MalformedURLException;
import java.net.URL;
import javax.swing.ImageIcon;

import org.concord.modeler.util.FileUtilities;

import static org.concord.modeler.draw.AbstractRectangle.UPPER_LEFT;
import static org.concord.modeler.draw.AbstractRectangle.UPPER_RIGHT;
import static org.concord.modeler.draw.AbstractRectangle.LOWER_LEFT;
import static org.concord.modeler.draw.AbstractRectangle.LOWER_RIGHT;
import static org.concord.modeler.draw.AbstractRectangle.RIGHT;
import static org.concord.modeler.draw.AbstractRectangle.TOP;
import static org.concord.modeler.draw.AbstractRectangle.LEFT;
import static org.concord.modeler.draw.AbstractRectangle.BOTTOM;

public abstract class AbstractEllipse implements DrawingElement {

	private static Rectangle handleNW, handleNE, handleSE, handleSW;
	private static Rectangle handleN, handleE, handleS, handleW;
	private final static Stroke THIN = new BasicStroke(1);

	private FillMode fillMode = FillMode.getNoFillMode();
	private ImageIcon bgImage;
	private Image fullImage;
	private Paint gradientPaint;
	private Color lineColor = Color.red;
	private short alpha = 255;
	private short alphaAtCenter = 255;
	private short alphaAtEdge = 255;
	private byte lineWeight = 2;
	private byte lineStyle = LineStyle.STROKE_NUMBER_1;
	private BasicStroke stroke = new BasicStroke(lineWeight, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER);
	private int x, y, width, height;
	private float angle;
	private boolean selected;
	private boolean selectionDrawn = true;
	private byte selectedHandle = (byte) -1;
	private Component component;
	private Ellipse2D bounds;

	protected AbstractEllipse() {
	}

	public AbstractEllipse(EllipseState s) {
		this();
		x = (int) s.getX();
		y = (int) s.getY();
		width = (int) s.getWidth();
		height = (int) s.getHeight();
		angle = s.getAngle();
		setFillMode(s.getFillMode());
		setAlpha(s.getAlpha());
		setAlphaAtCenter(s.getAlphaAtCenter());
		setAlphaAtEdge(s.getAlphaAtEdge());
		setLineWeight(s.getLineWeight());
		setLineStyle(s.getLineStyle());
		setLineColor(s.getLineColor());
	}

	public AbstractEllipse(AbstractEllipse e) {
		this();
		component = e.component;
		set(e);
	}

	public Shape getBounds() {
		if (bounds == null)
			bounds = new Ellipse2D.Float(x, y, width, height);
		else bounds.setFrame(x, y, width, height);
		return bounds;
	}

	public void setOval(AbstractEllipse e) {
		this.x = e.x;
		this.y = e.y;
		this.width = e.width;
		this.height = e.height;
	}

	public void set(AbstractEllipse e) {
		setOval(e);
		fillMode = e.fillMode;
		alpha = e.alpha;
		alphaAtCenter = e.alphaAtCenter;
		alphaAtEdge = e.alphaAtEdge;
		angle = e.angle;
		lineColor = e.lineColor;
		lineWeight = e.lineWeight;
		lineStyle = e.lineStyle;
		stroke = e.stroke;
	}

	protected abstract void attachToHost();

	protected abstract void setVisible(boolean b);

	protected abstract boolean isVisible();

	public boolean intersects(Rectangle r) {
		return r.intersects(x, y, width, height);
	}

	/**
	 * set the UI component whose context this rectangle will be drawn upon.
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
		if (handleNW == null) {
			handleNW = new Rectangle(x - 3, y - 3, 6, 6);
		}
		else {
			handleNW.x = x - 3;
			handleNW.y = y - 3;
		}
		if (handleNE == null) {
			handleNE = new Rectangle(x + width - 3, y - 3, 6, 6);
		}
		else {
			handleNE.x = x + width - 3;
			handleNE.y = y - 3;
		}
		if (handleSE == null) {
			handleSE = new Rectangle(x + width - 3, y + height - 3, 6, 6);
		}
		else {
			handleSE.x = x + width - 3;
			handleSE.y = y + height - 3;
		}
		if (handleSW == null) {
			handleSW = new Rectangle(x - 3, y + height - 3, 6, 6);
		}
		else {
			handleSW.x = x - 3;
			handleSW.y = y + height - 3;
		}
		if (handleN == null) {
			handleN = new Rectangle(x + width / 2 - 3, y - 3, 6, 6);
		}
		else {
			handleN.x = x + width / 2 - 3;
			handleN.y = y - 3;
		}
		if (handleE == null) {
			handleE = new Rectangle(x + width - 3, y + height / 2 - 3, 6, 6);
		}
		else {
			handleE.x = x + width - 3;
			handleE.y = y + height / 2 - 3;
		}
		if (handleS == null) {
			handleS = new Rectangle(x + width / 2 - 3, y + height - 3, 6, 6);
		}
		else {
			handleS.x = x + width / 2 - 3;
			handleS.y = y + height - 3;
		}
		if (handleW == null) {
			handleW = new Rectangle(x - 3, y + height / 2 - 3, 6, 6);
		}
		else {
			handleW.x = x - 3;
			handleW.y = y + height / 2 - 3;
		}
	}

	public boolean contains(double x1, double y1) {
		return x1 >= x && x1 <= x + width && y1 >= y && y1 <= y + height;
	}

	/**
	 * @return the handle that is within 4 pixels of the input coordinate (0,1,2,3). If the coordinate is close to none,
	 *         return -1.
	 */
	public byte nearHandle(double x, double y) {
		double dx = this.x - x;
		double dy = this.y - y;
		if (dx * dx + dy * dy < 16)
			return UPPER_LEFT;
		dx = this.x + this.width - x;
		dy = this.y - y;
		if (dx * dx + dy * dy < 16)
			return UPPER_RIGHT;
		dx = this.x + this.width - x;
		dy = this.y + this.height - y;
		if (dx * dx + dy * dy < 16)
			return LOWER_RIGHT;
		dx = this.x - x;
		dy = this.y + this.height - y;
		if (dx * dx + dy * dy < 16)
			return LOWER_LEFT;
		dx = this.x + this.width / 2 - x;
		dy = this.y - y;
		if (dx * dx + dy * dy < 16)
			return TOP;
		dx = this.x + this.width - x;
		dy = this.y + this.height / 2 - y;
		if (dx * dx + dy * dy < 16)
			return RIGHT;
		dx = this.x + this.width / 2 - x;
		dy = this.y + this.height - y;
		if (dx * dx + dy * dy < 16)
			return BOTTOM;
		dx = this.x - x;
		dy = this.y + this.height / 2 - y;
		if (dx * dx + dy * dy < 16)
			return LEFT;
		return -1;
	}

	public void translateTo(double x1, double y1) {
		x1 = x1 - (x + width * 0.5);
		y1 = y1 - (y + height * 0.5);
		x += (int) x1;
		y += (int) y1;
		if (selected)
			setHandles();
	}

	public void setLocation(double x, double y) {
		translateTo(x, y);
	}

	public void snapPosition(byte positionCode) {
		switch (positionCode) {
		case SNAP_TO_CENTER:
			setLocation(component.getWidth() * 0.5, component.getHeight() * 0.5);
			break;
		case SNAP_TO_NORTH_SIDE:
			setLocation(component.getWidth() * 0.5, height * 0.5);
			break;
		case SNAP_TO_SOUTH_SIDE:
			setLocation(component.getWidth() * 0.5, component.getHeight() - height * 0.5);
			break;
		case SNAP_TO_EAST_SIDE:
			setLocation(component.getWidth() - width * 0.5, component.getHeight() * 0.5);
			break;
		case SNAP_TO_WEST_SIDE:
			setLocation(width * 0.5, component.getHeight() * 0.5);
			break;
		}
	}

	public void translateBy(double dx, double dy) {
		x += (int) dx;
		y += (int) dy;
		if (selected)
			setHandles();
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
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

	public void setAlphaAtCenter(short i) {
		alphaAtCenter = i;
	}

	public short getAlphaAtCenter() {
		return alphaAtCenter;
	}

	public void setAlphaAtEdge(short i) {
		alphaAtEdge = i;
	}

	public short getAlphaAtEdge() {
		return alphaAtEdge;
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
		return x + 0.5 * width;
	}

	/** @return the y coordinate of the center of this element. */
	public double getRy() {
		return y + 0.5 * height;
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
		return x;
	}

	public void setX(float x) {
		this.x = (int) x;
		if (selected)
			setHandles();
	}

	public float getY() {
		return y;
	}

	public void setY(float y) {
		this.y = (int) y;
		if (selected)
			setHandles();
	}

	public void setWidth(float w) {
		width = (int) w;
		if (selected)
			setHandles();
	}

	public void setHeight(float h) {
		height = (int) h;
		if (selected)
			setHandles();
	}

	public void setSize(float w, float h) {
		width = (int) w;
		height = (int) w;
		if (selected)
			setHandles();
	}

	public void setOval(float x, float y, float w, float h) {
		this.x = (int) x;
		this.y = (int) y;
		this.width = (int) w;
		this.height = (int) h;
		if (selected)
			setHandles();
	}

	public void paint(Graphics g) {

		Graphics2D g2 = (Graphics2D) g;

		Color oldColor = g.getColor();
		Stroke oldStroke = g2.getStroke();

		AffineTransform at = g2.getTransform();
		if (angle != 0)
			g2.rotate(angle * Math.PI / 180.0, x + width * 0.5, y + height * 0.5);

		attachToHost();

		if (isVisible()) {

			if (fillMode instanceof FillMode.ColorFill) {
				if (alpha == 255) {
					g2.setColor(((FillMode.ColorFill) fillMode).getColor());
					g2.fillOval(x, y, width, height);
				}
				else if (alpha > 0) {
					g2.setColor(new Color((alpha << 24)
							| (0x00ffffff & ((FillMode.ColorFill) fillMode).getColor().getRGB()), true));
					g2.fillOval(x, y, width, height);
				}
			}
			else if (fillMode instanceof FillMode.ImageFill) {
				if (bgImage != null) {
					if (bgImage.getIconWidth() != width || bgImage.getIconHeight() != height)
						bgImage = new ImageIcon(fullImage.getScaledInstance(width, height, Image.SCALE_DEFAULT));
					bgImage.paintIcon(component, g, x, y);
				}
			}
			else if (fillMode instanceof FillMode.GradientFill) {
				FillMode.GradientFill gfm = (FillMode.GradientFill) fillMode;
				Color c1 = new Color((alphaAtCenter << 24) | (0x00ffffff & gfm.getColor1().getRGB()), true);
				Color c2 = new Color((alphaAtEdge << 24) | (0x00ffffff & gfm.getColor2().getRGB()), true);
				GradientFactory.paintOval(gradientPaint, g2, c1, c2, x, y, width, height, angle);
			}
			else if (fillMode instanceof FillMode.PatternFill) {
				FillMode.PatternFill tfm = (FillMode.PatternFill) fillMode;
				Color c1 = new Color((alpha << 24) | (0x00ffffff & tfm.getForeground()), true);
				Color c2 = new Color((alpha << 24) | (0x00ffffff & tfm.getBackground()), true);
				g2.setPaint(PatternFactory.createPattern(tfm.getStyle(), tfm.getCellWidth(), tfm.getCellHeight(), c1,
						c2));
				g2.fillOval(x, y, width, height);
			}

			if (lineWeight > 0) {
				g.setColor(lineColor);
				g2.setStroke(stroke);
				g2.drawOval(x, y, width, height);
			}

		}

		g2.setTransform(at);

		if (selected && selectionDrawn) {
			g2.setStroke(THIN);
			g.setColor(Color.yellow);
			g2.fill(handleNW);
			g2.fill(handleNE);
			g2.fill(handleSE);
			g2.fill(handleSW);
			g2.fill(handleN);
			g2.fill(handleE);
			g2.fill(handleS);
			g2.fill(handleW);
			g.setColor(Color.black);
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