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

package org.concord.modeler.util;

import java.awt.Color;
import java.awt.SystemColor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.Timer;

/** This can be used to blink a JComponent. */

public abstract class Blinker {

	public static Blinker getDefault() {
		return new DefaultBlinker();
	}

	public abstract Timer createTimer(final JComponent component);

	public abstract Timer createTimer(final JComponent component, Color blinkColor);

	public abstract Timer createTimer(final JComponent component, Color blinkColor, String flashText);

	/** the default implementation of Blinker */

	private static class DefaultBlinker extends Blinker {

		private Color blinkColor;
		private Color originalBackground;
		private Color originalForeground;
		private boolean wasOpaque;
		private String originalText;
		private int index;
		private int times = 6;

		public Timer createTimer(final JComponent c) {
			return createTimer(c, null);
		}

		public Timer createTimer(final JComponent c, Color color) {
			return createTimer(c, color, null);
		}

		public Timer createTimer(final JComponent c, final Color color, final String flashText) {

			if (c == null)
				throw new IllegalArgumentException("null component");

			originalBackground = c.getBackground();
			originalForeground = c.getForeground();
			wasOpaque = c.isOpaque();
			if (flashText != null) {
				if (c instanceof AbstractButton)
					originalText = ((AbstractButton) c).getText();
			}
			blinkColor = color == null ? SystemColor.textHighlight : color;

			final Timer timer = new Timer(250, null);
			timer.setRepeats(true);
			timer.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					c.putClientProperty("flashing", Boolean.TRUE);
					if (index < times) {
						if (index == 0)
							c.setOpaque(true);
						c.setBackground(index % 2 == 0 ? blinkColor : originalBackground);
						c.setForeground(index % 2 == 0 ? SystemColor.textHighlightText : originalForeground);
						if (flashText != null) {
							if (c instanceof AbstractButton)
								((AbstractButton) c).setText(index % 2 == 0 ? flashText : originalText);
						}
						index++;
					}
					else {
						timer.stop();
						index = 0;
						c.setBackground(originalBackground);
						c.setForeground(originalForeground);
						c.setOpaque(wasOpaque);
						if (flashText != null) {
							if (c instanceof AbstractButton)
								((AbstractButton) c).setText(originalText);
						}
						c.putClientProperty("flashing", Boolean.FALSE);
					}
					c.repaint();
				}
			});

			return timer;

		}

	}

}