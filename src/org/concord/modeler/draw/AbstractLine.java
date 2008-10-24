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
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.geom.Line2D;

public abstract class AbstractLine implements DrawingElement {

	public final static byte CENTER = 11;
	public final static byte ENDPOINT1 = 12;
	public final static byte ENDPOINT2 = 13;

	public final static byte DEFAULT = 21;
	public final static byte OUTLINED = 22;

	private final static float COS45 = (float) Math.cos(Math.toRadians(45.0));
	private final static float SIN45 = (float) Math.sin(Math.toRadians(45.0));

	private static Rectangle end1, end2;
	private final static Stroke THIN = new BasicStroke(1);

	private byte attachmentPosition = CENTER;
	private Color color = Color.red;
	private byte option = DEFAULT;
	private byte lineWeight = 1;
	private byte lineStyle = LineStyle.STROKE_NUMBER_1;
	private BasicStroke stroke = new BasicStroke(lineWeight, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER);
	private Line2D.Float line;
	private Line2D.Float outline;
	private boolean selected;
	private boolean selectionDrawn = true;
	private int selectedEndPoint = -1;
	private byte beginStyle = ArrowRectangle.NO_ARROW;
	private byte endStyle = ArrowRectangle.NO_ARROW;
	private Component component;
	private float arrowx, arrowy, wingx, wingy;
	private int arrowWeight;
	private Polygon polygon;
	private Font font = new Font("Arial", Font.PLAIN | Font.BOLD, 14);

	protected AbstractLine() {
		line = new Line2D.Float();
	}

	public AbstractLine(LineState s) {
		this();
		setX1(s.getX1());
		setY1(s.getY1());
		setX2(s.getX2());
		setY2(s.getY2());
		setLineWeight(s.getWeight());
		setColor(s.getColor());
		setLineStyle(s.getLineStyle());
		setBeginStyle(s.getBeginStyle());
		setEndStyle(s.getEndStyle());
		setOption(s.getOption());
	}

	public AbstractLine(AbstractLine l) {
		this();
		set(l);
		component = l.component;
	}

	public void set(AbstractLine l) {
		setLine(l);
		beginStyle = l.beginStyle;
		endStyle = l.endStyle;
		color = l.color;
		lineWeight = l.lineWeight;
		lineStyle = l.lineStyle;
		stroke = l.stroke;
		option = l.option;
	}

	public void setLine(AbstractLine l) {
		setLine(l.line.x1, l.line.y1, l.line.x2, l.line.y2);
	}

	public boolean intersects(Rectangle rect) {
		return line.intersects(rect);
	}

	public void setAttachmentPosition(byte b) {
		attachmentPosition = b;
	}

	public byte getAttachmentPosition() {
		return attachmentPosition;
	}

	/** set the UI component whose graphics context this line will be drawn upon. */
	public void setComponent(Component c) {
		component = c;
	}

	public Component getComponent() {
		return component;
	}

	public void setSelected(boolean b) {
		selected = b;
		if (selected) {
			setEnds();
		}
		else {
			selectedEndPoint = -1;
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

	public void setBeginStyle(byte beginStyle) {
		this.beginStyle = beginStyle;
	}

	public byte getBeginStyle() {
		return beginStyle;
	}

	public void setEndStyle(byte endStyle) {
		this.endStyle = endStyle;
	}

	public byte getEndStyle() {
		return endStyle;
	}

	public void setOption(byte option) {
		this.option = option;
	}

	public byte getOption() {
		return option;
	}

	public void setSelectedEndPoint(int i) {
		selectedEndPoint = i;
	}

	public int getSelectedEndPoint() {
		return selectedEndPoint;
	}

	private void setEnds() {
		if (end1 == null) {
			end1 = new Rectangle((int) (line.x1 - 3), (int) (line.y1 - 3), 6, 6);
		}
		else {
			end1.x = (int) (line.x1 - 3);
			end1.y = (int) (line.y1 - 3);
		}
		if (end2 == null) {
			end2 = new Rectangle((int) (line.x2 - 3), (int) (line.y2 - 3), 6, 6);
		}
		else {
			end2.x = (int) (line.x2 - 3);
			end2.y = (int) (line.y2 - 3);
		}
	}

	public boolean contains(double x, double y) {
		return line.ptSegDistSq(x, y) < 16;
	}

	/**
	 * @return the end point that is within 4 pixels of the input coordinate (1 or 2). If the coordinate is not close to
	 *         either one, return -1.
	 */
	public int nearEndPoint(double x, double y) {
		if ((line.x1 - x) * (line.x1 - x) + (line.y1 - y) * (line.y1 - y) < 16)
			return 1;
		if ((line.x2 - x) * (line.x2 - x) + (line.y2 - y) * (line.y2 - y) < 16)
			return 2;
		return -1;
	}

	public void translateTo(double x, double y) {
		x = x - (line.x1 + line.x2) * 0.5;
		y = y - (line.y1 + line.y2) * 0.5;
		line.x1 += (float) x;
		line.y1 += (float) y;
		line.x2 += (float) x;
		line.y2 += (float) y;
		if (selected)
			setEnds();
	}

	public void setLocation(double x, double y) {
		translateTo(x, y);
	}

	public void translateBy(double dx, double dy) {
		line.x1 += (float) dx;
		line.y1 += (float) dy;
		line.x2 += (float) dx;
		line.y2 += (float) dy;
		if (selected)
			setEnds();
	}

	/** @return the width of the bounding box. */
	public int getWidth() {
		return line.getBounds().width;
	}

	/** @return the height of the bounding box. */
	public int getHeight() {
		return line.getBounds().height;
	}

	public void setColor(Color color) {
		this.color = color;
	}

	public Color getColor() {
		return color;
	}

	public void setLineWeight(byte i) {
		lineWeight = i;
		stroke = new BasicStroke(lineWeight, stroke.getEndCap(), stroke.getLineJoin(), stroke.getMiterLimit(), stroke
				.getDashArray(), stroke.getDashPhase());
	}

	public byte getLineWeight() {
		return lineWeight;
	}

	/** @see org.concord.modeler.draw.LineStyle */
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
		return 0.5 * (line.x1 + line.x2);
	}

	/** @return the y coordinate of the center of this element. */
	public double getRy() {
		return 0.5 * (line.y1 + line.y2);
	}

	/** @return the length of this line */
	public double getLength() {
		// can you believe Line2D doesn't have a getLength() method?
		return Math.hypot(line.x1 - line.x2, line.y1 - line.y2);
	}

	public Point getCenter() {
		return new Point((int) getRx(), (int) getRy());
	}

	public float getX1() {
		return line.x1;
	}

	public void setX1(float x1) {
		line.x1 = x1;
		if (selected)
			setEnds();
	}

	public float getY1() {
		return line.y1;
	}

	public void setY1(float y1) {
		line.y1 = y1;
		if (selected)
			setEnds();
	}

	public float getX2() {
		return line.x2;
	}

	public void setX2(float x2) {
		line.x2 = x2;
		if (selected)
			setEnds();
	}

	public float getY2() {
		return line.y2;
	}

	public void setY2(float y2) {
		line.y2 = y2;
		if (selected)
			setEnds();
	}

	public void setEndPoint1(float x1, float y1) {
		line.x1 = x1;
		line.y1 = y1;
		if (selected)
			setEnds();
	}

	public void setEndPoint2(float x2, float y2) {
		line.x2 = x2;
		line.y2 = y2;
		if (selected)
			setEnds();
	}

	public void setLine(float x1, float y1, float x2, float y2) {
		line.setLine(x1, y1, x2, y2);
		if (selected)
			setEnds();
	}

	protected abstract void attachToHost();

	protected abstract void setVisible(boolean b);

	protected abstract boolean isVisible();

	public void paint(Graphics g) {

		attachToHost();

		Graphics2D g2 = (Graphics2D) g;

		Color oldColor = g.getColor();
		Stroke oldStroke = g2.getStroke();

		if (lineWeight > 0 && isVisible()) {
			g.setColor(color);
			g2.setStroke(stroke);
			g2.draw(line);
			drawArrow(g, beginStyle, line.x1, line.y1, line.x2, line.y2);
			drawArrow(g, endStyle, line.x2, line.y2, line.x1, line.y1);
			if (option == OUTLINED) {
				float dx = line.x2 - line.x1;
				float dy = line.y2 - line.y1;
				float d = 0.5f * lineWeight / (float) Math.hypot(dx, dy);
				float cos = dx * d;
				float sin = dy * d;
				g2.setStroke(THIN);
				g2.setColor(Color.black);
				if (outline == null)
					outline = new Line2D.Float();
				outline.setLine(line.x2 - sin, line.y2 + cos, line.x1 - sin, line.y1 + cos);
				g2.draw(outline);
				outline.setLine(line.x2 + sin, line.y2 - cos, line.x1 + sin, line.y1 - cos);
				g2.draw(outline);
				outline.setLine(line.x1 - sin, line.y1 + cos, line.x1 + sin, line.y1 - cos);
				g2.draw(outline);
				outline.setLine(line.x2 - sin, line.y2 + cos, line.x2 + sin, line.y2 - cos);
				g2.draw(outline);
			}
		}

		if (selected && selectionDrawn) {
			g2.setStroke(THIN);
			g.setColor(Color.yellow);
			if (attachmentPosition != ENDPOINT1)
				g2.fill(end1);
			if (attachmentPosition != ENDPOINT2)
				g2.fill(end2);
			g.setColor(Color.black);
			if (attachmentPosition != ENDPOINT1)
				g2.draw(end1);
			if (attachmentPosition != ENDPOINT2)
				g2.draw(end2);
			g2.setFont(font);
			g2.drawString("1", end1.x + end1.width, end1.y + end1.height);
			g2.drawString("2", end2.x + end2.width, end2.y + end2.height);
		}

		g.setColor(oldColor);
		g2.setStroke(oldStroke);

	}

	private void drawArrow(Graphics g, byte style, float x1, float y1, float x2, float y2) {

		arrowWeight = 5 + 2 * lineWeight;

		switch (style) {

		case ArrowRectangle.STYLE1:
			if (polygon == null)
				polygon = new Polygon();
			else polygon.reset();
			arrowx = x1 - x2;
			arrowy = y1 - y2;
			float a = 1.0f / (float) Math.hypot(arrowx, arrowy);
			arrowx *= a;
			arrowy *= a;
			polygon.addPoint((int) (x1 + arrowx * lineWeight), (int) (y1 + arrowy * lineWeight));
			wingx = arrowWeight * (arrowx * COS45 + arrowy * SIN45);
			wingy = arrowWeight * (arrowy * COS45 - arrowx * SIN45);
			polygon.addPoint((int) (x1 - wingx), (int) (y1 - wingy));
			wingx = arrowWeight * (arrowx * COS45 - arrowy * SIN45);
			wingy = arrowWeight * (arrowy * COS45 + arrowx * SIN45);
			polygon.addPoint((int) (x1 - wingx), (int) (y1 - wingy));
			g.fillPolygon(polygon);
			break;

		case ArrowRectangle.STYLE2:
			arrowx = x1 - x2;
			arrowy = y1 - y2;
			a = 1.0f / (float) Math.hypot(arrowx, arrowy);
			arrowx *= a;
			arrowy *= a;
			wingx = arrowWeight * (arrowx * COS45 + arrowy * SIN45);
			wingy = arrowWeight * (arrowy * COS45 - arrowx * SIN45);
			g.drawLine((int) (x1 + arrowx * lineWeight), (int) (y1 + arrowy * lineWeight), (int) (x1 - wingx),
					(int) (y1 - wingy));
			wingx = arrowWeight * (arrowx * COS45 - arrowy * SIN45);
			wingy = arrowWeight * (arrowy * COS45 + arrowx * SIN45);
			g.drawLine((int) (x1 + arrowx * lineWeight), (int) (y1 + arrowy * lineWeight), (int) (x1 - wingx),
					(int) (y1 - wingy));
			break;

		case ArrowRectangle.STYLE3:
			if (polygon == null)
				polygon = new Polygon();
			else polygon.reset();
			polygon.addPoint((int) x1, (int) y1);
			arrowx = x1 - x2;
			arrowy = y1 - y2;
			a = 1.0f / (float) Math.hypot(arrowx, arrowy);
			arrowx *= a;
			arrowy *= a;
			wingx = arrowWeight * (arrowx * COS45 + arrowy * SIN45);
			wingy = arrowWeight * (arrowy * COS45 - arrowx * SIN45);
			polygon.addPoint((int) (x1 - wingx), (int) (y1 - wingy));
			polygon.addPoint((int) (x1 + arrowx * arrowWeight), (int) (y1 + arrowy * arrowWeight));
			wingx = arrowWeight * (arrowx * COS45 - arrowy * SIN45);
			wingy = arrowWeight * (arrowy * COS45 + arrowx * SIN45);
			polygon.addPoint((int) (x1 - wingx), (int) (y1 - wingy));
			g.fillPolygon(polygon);
			break;

		case ArrowRectangle.STYLE4:
			if (polygon == null)
				polygon = new Polygon();
			else polygon.reset();
			arrowx = x1 - x2;
			arrowy = y1 - y2;
			a = 1.0f / (float) Math.hypot(arrowx, arrowy);
			arrowx *= a;
			arrowy *= a;
			x1 += arrowx * lineWeight;
			y1 += arrowy * lineWeight;
			polygon.addPoint((int) x1, (int) y1);
			wingx = arrowWeight * (arrowx * COS45 + arrowy * SIN45);
			wingy = arrowWeight * (arrowy * COS45 - arrowx * SIN45);
			polygon.addPoint((int) (x1 - wingx), (int) (y1 - wingy));
			polygon
					.addPoint((int) (x1 - 1.41421f * arrowx * arrowWeight),
							(int) (y1 - 1.41421f * arrowy * arrowWeight));
			wingx = arrowWeight * (arrowx * COS45 - arrowy * SIN45);
			wingy = arrowWeight * (arrowy * COS45 + arrowx * SIN45);
			polygon.addPoint((int) (x1 - wingx), (int) (y1 - wingy));
			g.fillPolygon(polygon);
			break;

		case ArrowRectangle.STYLE5:
			g.fillOval((int) (x1 - arrowWeight * 0.5f), (int) (y1 - arrowWeight * 0.5f), arrowWeight, arrowWeight);
			break;

		}

	}

}