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

package org.concord.modeler.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;

import org.concord.modeler.ModelerUtilities;

/**
 * This is a combo box color chooser in which the <tt>JColorChooser</tt> is combined.
 * 
 * @author Qian Xie
 */

public class ColorComboBox extends JComboBox {

	public final static int INDEX_MORE_COLOR = ColorRectangle.COLORS.length;
	public final static int INDEX_COLOR_CHOOSER = 100;
	public final static int INDEX_HEX_INPUTTER = 200;

	private Color color6 = Color.white;
	private Color previousColor;
	private Runnable runnable;
	private static JColorChooser colorChooser;
	private Component parent;

	public ColorComboBox(Component parent0) {

		setParent(parent0);

		if (colorChooser == null)
			colorChooser = new JColorChooser();

		setRenderer(new ComboBoxRenderer.ColorCell());
		for (int i = 0; i <= INDEX_MORE_COLOR; i++)
			addItem(i);
		addItem(INDEX_COLOR_CHOOSER);
		addItem(INDEX_HEX_INPUTTER);
		addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				final JComboBox cb = (JComboBox) e.getSource();
				int id = ((Integer) cb.getSelectedItem()).intValue();
				if (id == INDEX_COLOR_CHOOSER) {
					String s = ColorMenu.getInternationalText("MoreColors");
					JColorChooser.createDialog(parent, s != null ? s : "More Colors", true, colorChooser,
							new ActionListener() {
								public void actionPerformed(ActionEvent ae) {
									color6 = colorChooser.getColor();
									cb.setSelectedIndex(INDEX_MORE_COLOR);
									ColorRectangle cr = (ColorRectangle) cb.getRenderer();
									cr.setMoreColor(color6);
									if (runnable != null)
										EventQueue.invokeLater(runnable);
								}
							}, null).setVisible(true);
				}
				else if (id == INDEX_HEX_INPUTTER) {
					String s = previousColor != null ? Integer.toHexString(previousColor.getRGB() & 0x00ffffff) : "";
					int m = 6 - s.length();
					if (m != 6 && m != 0) {
						for (int k = 0; k < m; k++)
							s = "0" + s;
					}
					String s2 = ColorMenu.getInternationalText("HexColor");
					String hex = JOptionPane.showInputDialog(parent, s2 != null ? s2 : "Input a hex color number:", s);
					if (hex == null)
						return;
					try {
						color6 = ModelerUtilities.convertToColor(hex);
					}
					catch (NumberFormatException ex) {
						JOptionPane.showMessageDialog(parent, "Input color (hex) is not valid.");
						return;
					}
					cb.setSelectedIndex(INDEX_MORE_COLOR);
					ColorRectangle cr = (ColorRectangle) cb.getRenderer();
					cr.setMoreColor(color6);
					if (runnable != null)
						EventQueue.invokeLater(runnable);
				}
				previousColor = getSelectedColor();
			}
		});

	}

	public void setParent(Component parent) {
		this.parent = parent;
	}

	public static void setColorChooser(JColorChooser cc) {
		colorChooser = cc;
	}

	public static JColorChooser getColorChooser() {
		return colorChooser;
	}

	/** @return the latest color selected from the <tt>JColorChooser</tt> */
	public Color getMoreColor() {
		return color6;
	}

	/**
	 * when the user chooses a color from the <tt>JColorChooser</tt>, do the job of updating the color of the target
	 * object if there is one. The job has to be passed through by a <tt>Runnable</tt> wrapper.
	 */
	public void updateColor(Runnable r) {
		runnable = r;
	}

	public void setColor(Color c) {
		if (c == null)
			return;
		boolean b = false;
		for (int i = 0; i < INDEX_MORE_COLOR; i++) {
			if (c.equals(ColorRectangle.COLORS[i])) {
				setSelectedIndex(i);
				b = true;
			}
		}
		if (!b) {
			((ColorRectangle) getRenderer()).setMoreColor(c);
			setSelectedIndex(INDEX_MORE_COLOR);
			color6 = c;
		}
	}

	public Color getSelectedColor() {
		int i = getSelectedIndex();
		if (i < INDEX_MORE_COLOR)
			return ColorRectangle.COLORS[i];
		return color6;
	}

}