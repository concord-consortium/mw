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
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.Toolkit;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.net.MalformedURLException;
import java.net.URL;

import javax.swing.ImageIcon;

import org.concord.modeler.util.FileUtilities;

/**
 * This class partially defines a text box that can be drawn in the graphics context of a component. The implementation of attachment to another object is left to a subclass.
 * 
 * @see org.concord.modeler.draw.TextContainer#attachToHost
 * @author Charles Xie
 */

public abstract class TextContainer implements DrawingElement {

	public final static byte BOX_CENTER = 11;
	public final static byte ARROW_HEAD = 12;

	private final static Stroke THIN_DASHED = new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1.0f, new float[] { 2.0f }, 0.0f);

	private float x = 20, y = 20;
	private float angle;
	private String text;
	private String[] lines;
	private boolean selected;
	private boolean selectionDrawn = true;
	private Font font = new Font("Arial", Font.PLAIN, 12);
	private FillMode fillMode = FillMode.getNoFillMode();
	private byte borderType;
	private byte shadowType;
	private Color fgColor = Color.black;
	private ImageIcon bgImage;
	private Image fullImage;
	private Component component;
	private FontMetrics fm;
	private int shadowSize = 4;
	private boolean callOut;
	private Point callOutPoint = new Point(20, 20);
	private boolean changingCallOut;
	private int[] xPoints = new int[4];
	private int[] yPoints = new int[4];
	private Rectangle rectangle = new Rectangle();
	private byte attachmentPosition = ARROW_HEAD;

	public TextContainer(String text) {
		setText(text);
	}

	public TextContainer(String text, double x, double y) {
		this(text);
		this.x = (float) x;
		this.y = (float) y;
	}

	public TextContainer(TextContainerState s) {
		this(s.getText());
		x = s.getX();
		y = s.getY();
		angle = s.getAngle();
		fgColor = s.getForegroundColor();
		font = s.getFont();
		borderType = s.getBorderType();
		shadowType = s.getShadowType();
		fillMode = s.getFillMode();
		setAttachmentPosition(s.getAttachmentPosition());
		setCallOut(s.isCallOut());
		setCallOutPoint(s.getCallOutPoint());
	}

	public TextContainer(TextContainer tb) {
		setTextContainer(tb);
	}

	public void setTextContainer(TextContainer tb) {
		if (tb == null)
			throw new IllegalArgumentException("null input");
		setText(tb.text);
		fgColor = tb.fgColor;
		fillMode = tb.fillMode;
		bgImage = tb.bgImage;
		fullImage = tb.fullImage;
		borderType = tb.borderType;
		shadowType = tb.shadowType;
		callOut = tb.callOut;
		callOutPoint.setLocation(tb.callOutPoint);
		font = tb.font;
		angle = tb.angle;
		attachmentPosition = tb.attachmentPosition;
		component = tb.component;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
		if (text != null) {
			lines = text.split("\n");
		} else {
			lines = null;
		}
	}

	public void setSelected(boolean b) {
		selected = b;
		if (!selected)
			changingCallOut = false;
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

	/**
	 * set the UI component whose graphics context this text box will be drawn upon.
	 */
	public void setComponent(Component c) {
		component = c;
	}

	public Component getComponent() {
		return component;
	}

	private Color contrastBackground() {
		if (component == null)
			return Color.black;
		return new Color(0xffffff ^ component.getBackground().getRGB());
	}

	public void setCallOut(boolean b) {
		callOut = b;
		if (b) {
			attachmentPosition = ARROW_HEAD;
			setCallOutLocation((int) (x + getWidth() + 20), (int) (y + getHeight() + 20));
		} else {
			attachmentPosition = BOX_CENTER;
		}
	}

	public boolean isCallOut() {
		return callOut;
	}

	public void setCallOutPoint(Point p) {
		callOutPoint.setLocation(p);
	}

	public Point getCallOutPoint() {
		return callOutPoint;
	}

	public void setCallOutLocation(int x, int y) {
		if (callOutPoint == null) {
			callOutPoint = new Point(x, y);
		} else {
			callOutPoint.setLocation(x, y);
		}
	}

	public void translateCallOutLocationBy(int dx, int dy) {
		if (callOutPoint == null)
			return;
		callOutPoint.setLocation(callOutPoint.x + dx, callOutPoint.y + dy);
	}

	public void setChangingCallOut(boolean b) {
		changingCallOut = b;
	}

	public boolean isChangingCallOut() {
		return changingCallOut;
	}

	public void setFontFamily(String s) {
		font = new Font(s, font.getStyle(), font.getSize());
	}

	public void setFontSize(int i) {
		font = new Font(font.getFamily(), font.getStyle(), i);
	}

	public void setFontStyle(int i) {
		font = new Font(font.getFamily(), i, font.getSize());
	}

	public void setFont(Font font) {
		this.font = font;
	}

	public Font getFont() {
		return font;
	}

	public void setBorderType(byte i) {
		borderType = i;
	}

	public byte getBorderType() {
		return borderType;
	}

	public void setShadowType(byte i) {
		shadowType = i;
	}

	public byte getShadowType() {
		return shadowType;
	}

	public void setAttachmentPosition(byte b) {
		attachmentPosition = b;
	}

	public byte getAttachmentPosition() {
		return attachmentPosition;
	}

	public void setFillMode(FillMode fm) {
		fillMode = fm;
		if (fillMode instanceof FillMode.ImageFill) {
			String s = ((FillMode.ImageFill) fillMode).getURL();
			if (FileUtilities.isRemote(s)) {
				try {
					bgImage = new ImageIcon(new URL(s));
				} catch (MalformedURLException e) {
					e.printStackTrace(System.err);
				}
				fullImage = bgImage.getImage();
			} else {
				fullImage = Toolkit.getDefaultToolkit().createImage(s);
				bgImage = new ImageIcon(fullImage);
			}
		} else {
			bgImage = null;
			fullImage = null;
		}
	}

	public FillMode getFillMode() {
		return fillMode;
	}

	public void setForegroundColor(Color c) {
		fgColor = c;
	}

	public Color getForegroundColor() {
		return fgColor;
	}

	public Shape getBounds() {
		return new Rectangle2D.Float(x, y, getWidth(), getHeight());
	}

	/** return the width of the text box. */
	public int getWidth() {
		if (fm == null)
			return 20;
		if (text == null || lines == null)
			return 20;
		int w = 0;
		for (String i : lines) {
			w = Math.max(w, fm.stringWidth(i));
		}
		return (int) (w + fm.getHeight() * 1.5);
	}

	/** return the height of the text box. */
	public int getHeight() {
		if (fm == null)
			return 24;
		if (text == null)
			return 24;
		return (int) (fm.getHeight() * (lines.length + 0.5));
	}

	public boolean nearCallOutPoint(int x, int y) {
		if (!callOut)
			return false;
		if (callOutPoint == null)
			return false;
		return x > callOutPoint.x - 5 && x < callOutPoint.x + 5 && y > callOutPoint.y - 5 && y < callOutPoint.y + 5;
	}

	/** return the current x coordinate of this text box */
	public double getRx() {
		return x;
	}

	/** return the current y coordinate of this text box */
	public double getRy() {
		return y;
	}

	/** set the x coordinate of this text box */
	public void setRx(double x) {
		this.x = (float) x;
	}

	/** set the y coordinate of this text box */
	public void setRy(double y) {
		this.y = (float) y;
	}

	/** set the location of this text box */
	public void setLocation(double x, double y) {
		setRx(x);
		setRy(y);
	}

	public void snapPosition(byte positionCode) {
		switch (positionCode) {
		case SNAP_TO_CENTER:
			setLocation((component.getWidth() - getWidth()) * 0.5, (component.getHeight() - getHeight()) * 0.5);
			break;
		case SNAP_TO_NORTH_SIDE:
			setLocation((component.getWidth() - getWidth()) * 0.5, 0);
			break;
		case SNAP_TO_SOUTH_SIDE:
			setLocation((component.getWidth() - getWidth()) * 0.5, component.getHeight() - getHeight());
			break;
		case SNAP_TO_EAST_SIDE:
			setLocation(component.getWidth() - getWidth(), (component.getHeight() - getHeight()) * 0.5);
			break;
		case SNAP_TO_WEST_SIDE:
			setLocation(0, (component.getHeight() - getHeight()) * 0.5);
			break;
		}
	}

	/** same as <code>setLocation(double, double)</code> */
	public void translateTo(double x, double y) {
		setLocation(x, y);
	}

	public void translateBy(double dx, double dy) {
		x += dx;
		y += dy;
		callOutPoint.x += dx;
		callOutPoint.y += dy;
	}

	/** return the location of this image */
	public Point getLocation() {
		return new Point((int) x, (int) y);
	}

	public Point getCenter() {
		return new Point((int) (x + getWidth() * 0.5), (int) (y + getHeight() * 0.5));
	}

	public boolean contains(double rx, double ry) {
		return rx >= x && rx <= x + getWidth() && ry >= y && ry <= y + getHeight();
	}

	public void setAngle(float angle) {
		this.angle = angle;
	}

	public float getAngle() {
		return angle;
	}

	protected abstract void attachToHost();

	protected abstract void setVisible(boolean b);

	protected abstract boolean isVisible();

	public void paint(Graphics g) {

		Color oldColor = g.getColor();
		Stroke oldStroke = ((Graphics2D) g).getStroke();

		AffineTransform at = ((Graphics2D) g).getTransform();
		if (angle != 0)
			((Graphics2D) g).rotate(angle * Math.PI / 180.0, x + getWidth() * 0.5, y + getHeight() * 0.5);

		Color contrastView = contrastBackground();
		g.setFont(font);
		fm = g.getFontMetrics();
		int x1 = (int) x;
		int y1 = (int) y;
		int w1 = getWidth();
		int h1 = getHeight();
		rectangle.setRect(x1, y1, w1, h1);
		int fontHeight = fm.getHeight();

		attachToHost();
		if (attachmentPosition == BOX_CENTER) {
			x1 = (int) x;
			y1 = (int) y;
			rectangle.x = x1;
			rectangle.y = y1;
		}

		if (isVisible()) {

			g.setColor(Color.gray);
			if (fillMode == FillMode.getNoFillMode()) {
				switch (borderType) {
				case 0:
				case 1:
					switch (shadowType) {
					case 1:
						g.drawRect(x1 + shadowSize, y1 + shadowSize, w1, h1);
						break;
					case 2:
						g.drawRect(x1 - shadowSize, y1 + shadowSize, w1, h1);
						break;
					case 3:
						g.drawRect(x1 + shadowSize, y1 - shadowSize, w1, h1);
						break;
					case 4:
						g.drawRect(x1 - shadowSize, y1 - shadowSize, w1, h1);
						break;
					}
					break;
				case 2:
					switch (shadowType) {
					case 1:
						g.drawRoundRect(x1 + shadowSize, y1 + shadowSize, w1, h1, 10, 10);
						break;
					case 2:
						g.drawRoundRect(x1 - shadowSize, y1 + shadowSize, w1, h1, 10, 10);
						break;
					case 3:
						g.drawRoundRect(x1 + shadowSize, y1 - shadowSize, w1, h1, 10, 10);
						break;
					case 4:
						g.drawRoundRect(x1 - shadowSize, y1 - shadowSize, w1, h1, 10, 10);
						break;
					}
					break;
				}
			} else {
				switch (borderType) {
				case 0:
				case 1:
					switch (shadowType) {
					case 1:
						g.fillRect(x1 + shadowSize, y1 + shadowSize, w1, h1);
						break;
					case 2:
						g.fillRect(x1 - shadowSize, y1 + shadowSize, w1, h1);
						break;
					case 3:
						g.fillRect(x1 + shadowSize, y1 - shadowSize, w1, h1);
						break;
					case 4:
						g.fillRect(x1 - shadowSize, y1 - shadowSize, w1, h1);
						break;
					}
					break;
				case 2:
					switch (shadowType) {
					case 1:
						g.fillRoundRect(x1 + shadowSize, y1 + shadowSize, w1, h1, 10, 10);
						break;
					case 2:
						g.fillRoundRect(x1 - shadowSize, y1 + shadowSize, w1, h1, 10, 10);
						break;
					case 3:
						g.fillRoundRect(x1 + shadowSize, y1 - shadowSize, w1, h1, 10, 10);
						break;
					case 4:
						g.fillRoundRect(x1 - shadowSize, y1 - shadowSize, w1, h1, 10, 10);
						break;
					}
					break;
				}
			}
			if (fillMode instanceof FillMode.ColorFill) {
				g.setColor(((FillMode.ColorFill) fillMode).getColor());
				switch (borderType) {
				case 0:
				case 1:
					g.fillRect(x1, y1, w1, h1);
					break;
				case 2:
					g.fillRoundRect(x1, y1, w1, h1, 10, 10);
					break;
				}
			} else if (fillMode instanceof FillMode.ImageFill) {
				if (bgImage != null) {
					if (bgImage.getIconWidth() != w1 || bgImage.getIconHeight() != h1)
						bgImage = new ImageIcon(fullImage.getScaledInstance(w1, h1, Image.SCALE_DEFAULT));
					bgImage.paintIcon(component, g, x1, y1);
				}
			} else if (fillMode instanceof FillMode.GradientFill) {
				FillMode.GradientFill gfm = (FillMode.GradientFill) fillMode;
				GradientFactory.paintRect((Graphics2D) g, gfm.getStyle(), gfm.getVariant(), gfm.getColor1(), gfm.getColor2(), x1, y1, w1, h1);
			} else if (fillMode instanceof FillMode.PatternFill) {
				FillMode.PatternFill tfm = (FillMode.PatternFill) fillMode;
				((Graphics2D) g).setPaint(PatternFactory.createPattern(tfm.getStyle(), tfm.getCellWidth(), tfm.getCellHeight(), new Color(tfm.getForeground()), new Color(tfm.getBackground())));
				switch (borderType) {
				case 0:
				case 1:
					g.fillRect(x1, y1, w1, h1);
					break;
				case 2:
					g.fillRoundRect(x1, y1, w1, h1, 10, 10);
					break;
				}
			}
			if (component != null)
				g.setColor(new Color(0xffffff ^ component.getBackground().getRGB()));
			switch (borderType) {
			case 1:
				g.drawRect(x1, y1, w1, h1);
				break;
			case 2:
				g.drawRoundRect(x1, y1, w1, h1, 10, 10);
				break;
			}
			if (callOut && callOutPoint != null) {
				xPoints[0] = callOutPoint.x;
				yPoints[0] = callOutPoint.y;
				int oc = rectangle.outcode(callOutPoint.x, callOutPoint.y);
				boolean b = true;
				if ((oc & Rectangle2D.OUT_BOTTOM) == Rectangle2D.OUT_BOTTOM) {
					if ((oc & Rectangle2D.OUT_LEFT) == Rectangle2D.OUT_LEFT) {
						xPoints[1] = rectangle.x;
						yPoints[1] = (int) (rectangle.y + rectangle.height * 0.8);
						xPoints[2] = (int) (rectangle.x + rectangle.width * 0.2);
						yPoints[2] = rectangle.y + rectangle.height;
					} else if ((oc & Rectangle2D.OUT_RIGHT) == Rectangle2D.OUT_RIGHT) {
						xPoints[1] = rectangle.x + rectangle.width;
						yPoints[1] = (int) (rectangle.y + rectangle.height * 0.8);
						xPoints[2] = (int) (rectangle.x + rectangle.width * 0.8);
						yPoints[2] = rectangle.y + rectangle.height;
					} else {
						xPoints[1] = (int) (rectangle.x + rectangle.width * 0.4);
						xPoints[2] = (int) (rectangle.x + rectangle.width * 0.6);
						yPoints[2] = yPoints[1] = rectangle.y + rectangle.height;
					}
				} else if ((oc & Rectangle2D.OUT_TOP) == Rectangle2D.OUT_TOP) {
					if ((oc & Rectangle2D.OUT_LEFT) == Rectangle2D.OUT_LEFT) {
						xPoints[1] = rectangle.x;
						yPoints[1] = (int) (rectangle.y + rectangle.height * 0.2);
						xPoints[2] = (int) (rectangle.x + rectangle.width * 0.2);
						yPoints[2] = rectangle.y;
					} else if ((oc & Rectangle2D.OUT_RIGHT) == Rectangle2D.OUT_RIGHT) {
						xPoints[1] = (int) (rectangle.x + rectangle.width * 0.8);
						yPoints[1] = rectangle.y;
						xPoints[2] = rectangle.x + rectangle.width;
						yPoints[2] = (int) (rectangle.y + rectangle.height * 0.2);
					} else {
						xPoints[1] = (int) (rectangle.x + rectangle.width * 0.4);
						xPoints[2] = (int) (rectangle.x + rectangle.width * 0.6);
						yPoints[2] = yPoints[1] = rectangle.y + 1;
					}
				} else if ((oc & Rectangle2D.OUT_LEFT) == Rectangle2D.OUT_LEFT) {
					xPoints[2] = xPoints[1] = rectangle.x + 1;
					yPoints[1] = (int) (rectangle.y + rectangle.height * 0.4);
					yPoints[2] = (int) (rectangle.y + rectangle.height * 0.6);
				} else if ((oc & Rectangle2D.OUT_RIGHT) == Rectangle2D.OUT_RIGHT) {
					xPoints[2] = xPoints[1] = rectangle.x + rectangle.width;
					yPoints[1] = (int) (rectangle.y + rectangle.height * 0.4);
					yPoints[2] = (int) (rectangle.y + rectangle.height * 0.6);
				} else {
					b = false;
				}
				if (b) {
					if (fillMode instanceof FillMode.ColorFill) {
						g.setColor(((FillMode.ColorFill) fillMode).getColor());
						g.fillPolygon(xPoints, yPoints, 3);
					}
					if (borderType != 0) {
						g.setColor(contrastView);
						g.drawLine(xPoints[0], yPoints[0], xPoints[1], yPoints[1]);
						g.drawLine(xPoints[0], yPoints[0], xPoints[2], yPoints[2]);
					}
				}
			}
			if (selected && selectionDrawn) {
				if (callOut && callOutPoint != null) {
					xPoints[0] = callOutPoint.x - 4;
					yPoints[0] = callOutPoint.y;
					xPoints[1] = callOutPoint.x;
					yPoints[1] = callOutPoint.y + 4;
					xPoints[2] = callOutPoint.x + 4;
					yPoints[2] = callOutPoint.y;
					xPoints[3] = callOutPoint.x;
					yPoints[3] = callOutPoint.y - 4;
					g.setColor(Color.yellow);
					g.fillPolygon(xPoints, yPoints, 4);
					g.setColor(contrastView);
					g.drawPolygon(xPoints, yPoints, 4);
				}
				g.setColor(contrastView);
				((Graphics2D) g).setStroke(THIN_DASHED);
				switch (borderType) {
				case 2:
					g.drawRoundRect(x1 - 2, y1 - 2, w1 + 4, h1 + 4, 12, 12);
					break;
				default:
					g.drawRect(x1 - 2, y1 - 2, w1 + 4, h1 + 4);
				}
			}
			g.setColor(fgColor);
			if (lines != null) {
				for (int i = 0; i < lines.length; i++)
					g.drawString(lines[i], (int) (x + fontHeight * 0.75f), (int) (y + (i + 1) * fontHeight));
			}

		}

		((Graphics2D) g).setTransform(at);
		g.setColor(oldColor);
		((Graphics2D) g).setStroke(oldStroke);

	}

	public String toString() {
		return "Text box: [" + text + "]";
	}

}