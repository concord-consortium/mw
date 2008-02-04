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

package org.concord.mw3d;

import java.awt.Color;
import java.awt.Container;
import java.awt.Font;
import java.awt.LayoutManager;
import java.awt.event.ActionListener;
import java.awt.event.ItemListener;
import java.awt.event.MouseListener;
import java.text.DecimalFormat;
import java.text.NumberFormat;

import javax.swing.AbstractButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.plaf.basic.BasicBorders;

import org.concord.modeler.ModelerUtilities;
import org.concord.modeler.ui.ColorRectangle;
import org.concord.modeler.ui.FloatNumberTextField;
import org.concord.modeler.ui.HyperlinkLabel;

abstract class PropertiesPanel extends JPanel {

	final static double ZERO = 0.000001;

	final static BasicBorders.ButtonBorder BUTTON_BORDER = new BasicBorders.ButtonBorder(Color.lightGray, Color.white,
			Color.black, Color.gray);

	final static NumberFormat DECIMAL_FORMAT = NumberFormat.getNumberInstance();
	final static DecimalFormat SCIENTIFIC_FORMAT = new DecimalFormat();

	static {
		DECIMAL_FORMAT.setMaximumFractionDigits(6);
		DECIMAL_FORMAT.setMaximumIntegerDigits(6);
		SCIENTIFIC_FORMAT.applyPattern("0.###E00");
	}

	PropertiesPanel(LayoutManager layout) {
		super(layout);
	}

	abstract void windowActivated();

	static void removeListenersForLabel(JLabel b) {
		if (b == null)
			return;
		MouseListener[] ml = b.getMouseListeners();
		if (ml != null) {
			for (int i = 0; i < ml.length; i++)
				b.removeMouseListener(ml[i]);
		}
		if (b instanceof HyperlinkLabel) {
			((HyperlinkLabel) b).setAction(null);
		}
	}

	static void removeListenersForAbstractButton(AbstractButton b) {
		if (b == null)
			return;
		b.setAction(null);
		ActionListener[] al = b.getActionListeners();
		if (al != null) {
			for (int i = 0; i < al.length; i++)
				b.removeActionListener(al[i]);
		}
		MouseListener[] ml = b.getMouseListeners();
		if (ml != null) {
			for (int i = 0; i < ml.length; i++)
				b.removeMouseListener(ml[i]);
		}
	}

	static void removeListenersForComboBox(JComboBox b) {
		if (b == null)
			return;
		b.setAction(null);
		ActionListener[] al = b.getActionListeners();
		if (al != null) {
			for (int i = 0; i < al.length; i++)
				b.removeActionListener(al[i]);
		}
		ItemListener[] il = b.getItemListeners();
		if (il != null) {
			for (int i = 0; i < il.length; i++)
				b.removeItemListener(il[i]);
		}
	}

	static void removeListenersForTextField(JTextField t) {
		if (t == null)
			return;
		t.setAction(null);
		ActionListener[] al = t.getActionListeners();
		if (al != null) {
			for (int i = 0; i < al.length; i++)
				t.removeActionListener(al[i]);
		}
		MouseListener[] ml = t.getMouseListeners();
		if (ml != null) {
			for (int i = 0; i < ml.length; i++)
				t.removeMouseListener(ml[i]);
		}
	}

	static void applyBounds(FloatNumberTextField t) {
		float x = t.getValue();
		if (x > t.getMaxValue()) {
			t.setValue(t.getMaxValue());
		}
		else if (x < t.getMinValue()) {
			t.setValue(t.getMinValue());
		}
	}

	static void setColorComboBox(JComboBox cb, Color c) {
		int k = ColorRectangle.COLORS.length;
		for (int i = 0; i < ColorRectangle.COLORS.length; i++) {
			if (c.equals(ColorRectangle.COLORS[i]))
				k = i;
		}
		if (k == ColorRectangle.COLORS.length) {
			((ColorRectangle) cb.getRenderer()).setMoreColor(c);
		}
		setComboBox(cb, k);
	}

	static void setComboBox(JComboBox cb, int k) {
		ActionListener[] al = cb.getActionListeners();
		if (al != null) {
			for (int i = 0; i < al.length; i++)
				cb.removeActionListener(al[i]);
		}
		ItemListener[] il = cb.getItemListeners();
		if (il != null) {
			for (int i = 0; i < il.length; i++)
				cb.removeItemListener(il[i]);
		}
		cb.setSelectedIndex(k);
		if (al != null) {
			for (int i = 0; i < al.length; i++)
				cb.addActionListener(al[i]);
		}
		if (il != null) {
			for (int i = 0; i < il.length; i++)
				cb.addItemListener(il[i]);
		}
	}

	static JLabel createLabel(int i) {
		JLabel l = new JLabel(Integer.toString(i));
		l.setBorder(BUTTON_BORDER);
		return l;
	}

	static JLabel createLabel(double d) {
		JLabel l = new JLabel(DECIMAL_FORMAT.format(d));
		l.setBorder(BUTTON_BORDER);
		return l;
	}

	static JLabel createLabel2(double d) {
		JLabel l = new JLabel(SCIENTIFIC_FORMAT.format(d));
		l.setBorder(BUTTON_BORDER);
		return l;
	}

	static JLabel createLabel(String s) {
		JLabel l = new JLabel(s);
		l.setBorder(BUTTON_BORDER);
		return l;
	}

	static JLabel createSmallerFontLabel(String s) {
		JLabel l = new JLabel(s);
		Font font = l.getFont();
		l.setFont(new Font(font.getFontName(), font.getStyle(), font.getSize() - 1));
		return l;
	}

	static void makeCompactGrid(Container parent, int rows, int cols, int initialX, int initialY, int xPad, int yPad) {
		ModelerUtilities.makeCompactGrid(parent, rows, cols, initialX, initialY, xPad, yPad);
	}

}