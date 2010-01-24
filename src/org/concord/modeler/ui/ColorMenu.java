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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeListener;
import java.util.EventListener;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JColorChooser;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.event.PopupMenuListener;

import org.concord.modeler.ModelerUtilities;
import org.concord.modeler.draw.FillMode;

public class ColorMenu extends JMenu {

	public final static String FILLING = "Filling";
	private static ResourceBundle bundle;
	private static boolean isUSLocale;

	protected JColorChooser colorChooser;
	protected FillEffectChooser fillEffectChooser;

	private Component parent;
	private JMenuItem noFillMenuItem;
	private JMenuItem moreColorMenuItem;
	private JMenuItem hexColorMenuItem;
	private JMenuItem fillEffectMenuItem;
	private ColorArrayPane cap;

	public ColorMenu(Component parent, String name, JColorChooser color) {
		this(parent, name, color, null);
	}

	public ColorMenu(Component parent, String name, JColorChooser color, FillEffectChooser fillEffect) {

		super(name);

		if (bundle == null) {
			isUSLocale = Locale.getDefault().equals(Locale.US);
			try {
				bundle = ResourceBundle.getBundle("org.concord.modeler.ui.images.ColorMenu", Locale.getDefault());
			}
			catch (MissingResourceException e) {
			}
		}

		this.parent = parent;
		colorChooser = color;
		fillEffectChooser = fillEffect;

		String s = getInternationalText("NoFill");
		noFillMenuItem = new JMenuItem(s != null ? s : "No Fill");
		add(noFillMenuItem);
		addSeparator();

		cap = new ColorArrayPane();
		cap.addColorArrayListener(new ColorArrayListener() {
			public void colorSelected(ColorArrayEvent e) {
				doSelection();
				ColorMenu.this.firePropertyChange(FILLING, null, new FillMode.ColorFill(e.getSelectedColor()));
			}
		});
		add(cap);
		addSeparator();

		s = getInternationalText("MoreColors");
		moreColorMenuItem = new JMenuItem((s != null ? s : "More Colors") + "...");
		add(moreColorMenuItem);

		s = getInternationalText("HexColor");
		hexColorMenuItem = new JMenuItem((s != null ? s : "Hex Color") + "...");
		add(hexColorMenuItem);

		if (fillEffectChooser != null) {
			s = getInternationalText("FillEffects");
			fillEffectMenuItem = new JMenuItem((s != null ? s : "Fill Effects") + "...");
			add(fillEffectMenuItem);
		}

	}

	public void setColorSelectionOnly(boolean b) {
		if (b) {
			remove(noFillMenuItem);
			if (fillEffectMenuItem != null)
				remove(fillEffectMenuItem);
			remove(0);
		}
		else {
			insert(noFillMenuItem, 0);
		}
	}

	static String getInternationalText(String name) {
		if (bundle == null)
			return null;
		if (name == null)
			return null;
		if (isUSLocale)
			return null;
		String s = null;
		try {
			s = bundle.getString(name);
		}
		catch (MissingResourceException e) {
			s = null;
		}
		return s;
	}

	public void destroy() {
		EventListener[] a = getListeners(PropertyChangeListener.class);
		if (a != null) {
			for (EventListener x : a) {
				removePropertyChangeListener((PropertyChangeListener) x);
			}
		}
		PopupMenuListener[] pml = getPopupMenu().getPopupMenuListeners();
		if (pml != null) {
			for (PopupMenuListener x : pml)
				getPopupMenu().removePopupMenuListener(x);
		}
		setParent(null);
		setColorChooser(null);
		setFillEffectChooser(null);
		clearNoFillActions();
		clearColorArrayActions();
		clearMoreColorActions();
		clearFillEffectActions();
	}

	public void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
		super.firePropertyChange(propertyName, oldValue, newValue);
	}

	public void setParent(Component parent) {
		this.parent = parent;
	}

	public void setColorChooser(JColorChooser cc) {
		colorChooser = cc;
	}

	public JColorChooser getColorChooser() {
		return colorChooser;
	}

	public void setFillEffectChooser(FillEffectChooser fec) {
		fillEffectChooser = fec;
	}

	public FillEffectChooser getFillEffectChooser() {
		return fillEffectChooser;
	}

	public void addNoFillListener(ActionListener a) {
		noFillMenuItem.addActionListener(a);
	}

	public void removeNoFillListener(ActionListener a) {
		noFillMenuItem.removeActionListener(a);
	}

	public void setNoFillAction(Action a) {
		noFillMenuItem.setAction(a);
		String s = getInternationalText("NoFill");
		if (s != null)
			noFillMenuItem.setText(s);
	}

	void clearNoFillActions() {
		if (noFillMenuItem == null)
			return;
		noFillMenuItem.setAction(null);
		ActionListener[] a = noFillMenuItem.getActionListeners();
		if (a == null)
			return;
		for (ActionListener l : a)
			noFillMenuItem.removeActionListener(l);
	}

	public void addColorArrayListener(ActionListener a) {
		addActionListener(a);
	}

	public void removeColorArrayListener(ActionListener a) {
		removeActionListener(a);
	}

	public void setColorArrayAction(Action a) {
		setAction(a);
	}

	void clearColorArrayActions() {
		setAction(null);
		ActionListener[] a = getActionListeners();
		if (a == null)
			return;
		for (ActionListener l : a)
			removeActionListener(l);
	}

	public Color getHexInputColor(Color oldColor) {
		String s = oldColor != null ? Integer.toHexString(oldColor.getRGB() & 0x00ffffff) : "";
		int m = 6 - s.length();
		if (m != 6 && m != 0) {
			for (int k = 0; k < m; k++)
				s = "0" + s;
		}
		String hex = JOptionPane.showInputDialog(parent, "Input a hex color number:", s);
		if (hex == null)
			return null;
		return ModelerUtilities.convertToColor(hex);
	}

	public void addMoreColorListener(final ActionListener a) {
		moreColorMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String s = getInternationalText("MoreColors");
				JColorChooser.createDialog(parent, s != null ? s : "Background Color", true, colorChooser, a, null)
						.setVisible(true);
			}
		});
	}

	public void addHexColorListener(ActionListener a) {
		hexColorMenuItem.addActionListener(a);
	}

	public void setMoreColorAction(final ActionListener a) {
		moreColorMenuItem.setAction(new AbstractAction("More Colors") {
			public void actionPerformed(ActionEvent e) {
				String s = getInternationalText("MoreColors");
				JColorChooser.createDialog(parent, s != null ? s : "Background Color", true, colorChooser, a, null)
						.setVisible(true);
			}
		});
		String s = getInternationalText("MoreColors");
		if (s != null)
			moreColorMenuItem.setText(s);
	}

	void clearMoreColorActions() {
		if (moreColorMenuItem == null)
			return;
		moreColorMenuItem.setAction(null);
		ActionListener[] a = moreColorMenuItem.getActionListeners();
		if (a == null)
			return;
		for (ActionListener l : a)
			moreColorMenuItem.removeActionListener(l);
	}

	public void addFillEffectListeners(final ActionListener ok, final ActionListener cancel) {
		if (fillEffectMenuItem != null)
			fillEffectMenuItem.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					String s = getInternationalText("FillEffects");
					FillEffectChooser.createDialog(parent, s != null ? s : "Background Filling", true,
							fillEffectChooser, ok, cancel).setVisible(true);
				}
			});
	}

	public void setFillEffectActions(final ActionListener ok, final ActionListener cancel) {
		fillEffectMenuItem.setAction(new AbstractAction("Fill Effects") {
			public void actionPerformed(ActionEvent e) {
				String s = getInternationalText("FillEffects");
				FillEffectChooser.createDialog(parent, s != null ? s : "Background Filling", true, fillEffectChooser,
						ok, cancel).setVisible(true);
			}
		});
		String s = getInternationalText("FillEffects");
		if (s != null)
			fillEffectMenuItem.setText(s);
	}

	void clearFillEffectActions() {
		if (fillEffectMenuItem == null)
			return;
		fillEffectMenuItem.setAction(null);
		ActionListener[] a = fillEffectMenuItem.getActionListeners();
		for (ActionListener l : a)
			fillEffectMenuItem.removeActionListener(l);
	}

	public void setColor(Color c) {
		cap.setSelectedColor(c);
	}

	public Color getColor() {
		return cap.getSelectedColor();
	}

	public void doSelection() {
		fireActionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, getActionCommand()));
	}

}
