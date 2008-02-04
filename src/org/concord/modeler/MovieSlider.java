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

package org.concord.modeler;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.SystemColor;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.RoundRectangle2D;

import javax.swing.JComponent;

import org.concord.modeler.event.MovieEvent;
import org.concord.modeler.event.MovieListener;

public class MovieSlider extends JComponent implements MovieListener {

	private static Font font;
	private int currentFrame, totalFrame = 100;
	private Rectangle knob;
	private boolean knobHeld;
	private int fillLocator;
	private int flasher;
	private RoundRectangle2D grabber;
	private Movie movie;

	MovieSlider(Movie m) {
		movie = m;
		if (font == null)
			font = new Font("Arial", Font.PLAIN, 9);
		setToolTipText("Pull the knob or roll the wheel to change frame");
		addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				processKeyPressedEvent(e);
			}

			public void keyReleased(KeyEvent e) {
				// setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
			}
		});
		addMouseListener(new MouseAdapter() {
			public void mouseReleased(MouseEvent e) {
				processMouseReleasedEvent(e);
			}

			public void mousePressed(MouseEvent e) {
				processMousePressedEvent(e);
			}
		});
		addMouseMotionListener(new MouseMotionAdapter() {
			public void mouseMoved(MouseEvent e) {
				processMouseMovedEvent(e);
			}

			public void mouseDragged(MouseEvent e) {
				processMouseDraggedEvent(e);
			}
		});
		addMouseWheelListener(new MouseWheelListener() {
			public void mouseWheelMoved(MouseWheelEvent e) {
				processMouseWheelMovedEvent(e);
			}
		});
	}

	private void processMouseWheelMovedEvent(MouseWheelEvent e) {
		if (!isEnabled())
			return;
		if (movie == null)
			return;
		if (e.getWheelRotation() > 0) {
			if (currentFrame >= movie.length() - 1)
				return;
			currentFrame++;
		}
		else {
			if (currentFrame <= 0)
				return;
			currentFrame--;
		}
		movie.setCurrentFrameIndex(currentFrame);
		movie.showFrame(currentFrame);
		((AbstractMovie) movie).changeToolTipText();
		movie.notifyMovieListeners(new MovieEvent(movie, MovieEvent.FRAME_CHANGED, currentFrame));
		repaint();
	}

	private void processMouseReleasedEvent(MouseEvent e) {
		if (!isEnabled())
			return;
		if (knobHeld) {
			knobHeld = false;
			setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
			if (movie == null)
				return;
			movie.setCurrentFrameIndex(currentFrame);
			movie.showFrame(currentFrame);
			((AbstractMovie) movie).changeToolTipText();
		}
	}

	private void processMousePressedEvent(MouseEvent e) {
		if (!isEnabled())
			return;
		requestFocus();
		knobHeld = knob.contains(e.getX(), e.getY());
	}

	private void processMouseDraggedEvent(MouseEvent e) {
		if (!isEnabled())
			return;
		int x = e.getX();
		int w2 = knob.width / 2;
		if (knobHeld) {
			if (x > w2 && x < fillLocator + w2) {
				knob.x = x - w2;
			}
			else if (x <= w2) {
				knob.x = 0;
			}
			else if (x >= fillLocator + w2) {
				knob.x = fillLocator;
			}
			calculateCurrentFrame();
			if (movie == null)
				return;
			movie.setCurrentFrameIndex(currentFrame);
			movie.showFrame(currentFrame);
			movie.notifyMovieListeners(new MovieEvent(movie, MovieEvent.FRAME_CHANGED, currentFrame));
			repaint();
		}
	}

	private void processMouseMovedEvent(MouseEvent e) {
		if (!isEnabled())
			return;
		setCursor(Cursor.getPredefinedCursor(knob.contains(e.getX(), e.getY()) ? Cursor.HAND_CURSOR
				: Cursor.DEFAULT_CURSOR));
	}

	private void processKeyPressedEvent(KeyEvent e) {
		if (movie == null)
			return;
		switch (e.getKeyCode()) {
		case KeyEvent.VK_RIGHT:
			if (currentFrame >= movie.length() - 1)
				return;
			// setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			currentFrame++;
			movie.setCurrentFrameIndex(currentFrame);
			movie.showFrame(currentFrame);
			((AbstractMovie) movie).changeToolTipText();
			movie.notifyMovieListeners(new MovieEvent(movie, MovieEvent.FRAME_CHANGED, currentFrame));
			repaint();
			break;
		case KeyEvent.VK_LEFT:
			if (currentFrame <= 0)
				return;
			// setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			currentFrame--;
			movie.setCurrentFrameIndex(currentFrame);
			movie.showFrame(currentFrame);
			((AbstractMovie) movie).changeToolTipText();
			movie.notifyMovieListeners(new MovieEvent(movie, MovieEvent.FRAME_CHANGED, currentFrame));
			repaint();
			break;
		}
	}

	public void setCurrentFrame(int i) {
		currentFrame = i;
		if (knob == null) {
			Dimension dim = getPreferredSize();
			knob = new Rectangle(0, 0, dim.width / 5, dim.height);
		}
		knob.x = (int) (((float) i) / (float) (totalFrame - 1) * (getWidth() - knob.width));
		if (movie instanceof SlideMovie) {
			if (((SlideMovie) movie).shouldFlash()) {
				if (fillLocator >= getWidth() - knob.width) {
					flasher = (flasher++) % 3 - 1;
				}
				else {
					flasher = 0;
				}
			}
		}
	}

	private void calculateFillLocator() {
		int oldValue = fillLocator;
		fillLocator = (int) (((float) movie.length()) / ((float) totalFrame) * (getWidth() - knob.width));
		if (oldValue < fillLocator) {
			knob.x = fillLocator;
			currentFrame = movie.length() - 1;
		}
	}

	public int getCurrentFrame() {
		return currentFrame;
	}

	public void setTotalFrame(int i) {
		totalFrame = i;
	}

	public int getTotalFrame() {
		return totalFrame;
	}

	private void calculateCurrentFrame() {
		currentFrame = (int) (((float) knob.x / (float) (getWidth() - knob.width) * (totalFrame - 1)));
	}

	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		update(g);
	}

	public void update(Graphics g) {

		Graphics2D g2 = (Graphics2D) g;

		g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setFont(font);

		Dimension dim = getSize();

		if (knob == null)
			knob = new Rectangle(0, 0, dim.width / 5, dim.height);

		g2.setColor(getBackground());
		g2.fillRect(0, 0, dim.width, dim.height);
		g2.setColor(Color.white);
		g2.fillRect(5, 5, dim.width - knob.width, dim.height - 10);
		g2.setColor(Color.blue);
		g2.fillRect(dim.width - knob.width + 1, 5, knob.width - 6, dim.height - 9);
		g2.drawRect(5, 5, dim.width - knob.width, dim.height - 10);

		calculateFillLocator();
		g2.setColor(SystemColor.controlShadow);
		g2.fillRect(6, 7, fillLocator, dim.height - 13);
		int i = 9 + flasher;
		while (i <= fillLocator) {
			g2.drawLine(i, 5, i, 7);
			g2.drawLine(i, dim.height - 6, i, dim.height - 8);
			i += 3;
		}

		if (grabber == null) {
			grabber = new RoundRectangle2D.Float(knob.x + 2, knob.y + 2, knob.width - 4, knob.height - 5, 8, 8);
		}
		else {
			grabber.setRoundRect(knob.x + 2, knob.y + 2, knob.width - 4, knob.height - 5, 8, 8);
		}
		g2.setColor(SystemColor.control);
		g2.fill(grabber);
		g2.setColor(isEnabled() ? Color.blue : SystemColor.textInactiveText);
		g2.draw(grabber);

		FontMetrics fm = g2.getFontMetrics();
		int h = fm.getAscent();
		g2.setColor(isEnabled() ? SystemColor.controlText : SystemColor.textInactiveText);
		String s = "0";
		if (movie.length() > 0) {
			if (currentFrame == movie.getCapacity()) {
				s = "" + currentFrame;
			}
			else {
				s = "" + (currentFrame + 1);
			}
		}
		int w = fm.stringWidth(s);
		g2.drawString(s, knob.x + knob.width / 2 - w / 2, knob.y + knob.height / 2 + h / 4);

		if (getBorder() != null)
			paintBorder(g2);

	}

	public void frameChanged(MovieEvent e) {

		if (e.getID() == MovieEvent.FRAME_CHANGED) {
			setCurrentFrame(e.getFrame());
			repaint();
		}

	}

}