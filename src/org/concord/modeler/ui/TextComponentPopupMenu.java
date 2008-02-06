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

import java.awt.Component;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.JTextComponent;

public class TextComponentPopupMenu extends JPopupMenu {

	protected Map<Object, Action> actions;
	protected JTextComponent text;
	protected JMenuItem miCut, miPaste, miCopy, miSelectAll;

	private static ResourceBundle bundle;
	private static boolean isUSLocale;

	public TextComponentPopupMenu(JTextComponent t) {

		if (bundle == null) {
			isUSLocale = Locale.getDefault().equals(Locale.US);
			try {
				bundle = ResourceBundle.getBundle("org.concord.modeler.ui.images.TextComponentPopupMenu", Locale
						.getDefault());
			}
			catch (MissingResourceException e) {
			}
		}

		text = t;

		actions = new HashMap<Object, Action>();
		for (Action act : text.getActions())
			actions.put(act.getValue(Action.NAME), act);

		miCopy = new JMenuItem(actions.get(DefaultEditorKit.copyAction));
		String s = getInternationalText("Copy");
		miCopy.setText(s == null ? "Copy" : s);
		miCopy.setIcon(IconPool.getIcon("copy"));
		miCopy.setAccelerator(System.getProperty("os.name").startsWith("Mac") ? KeyStroke.getKeyStroke(KeyEvent.VK_C,
				KeyEvent.ALT_MASK) : KeyStroke.getKeyStroke(KeyEvent.VK_C, KeyEvent.CTRL_MASK));
		add(miCopy);

		miCut = new JMenuItem(actions.get(DefaultEditorKit.cutAction));
		s = getInternationalText("Cut");
		miCut.setText(s == null ? "Cut" : s);
		miCut.setIcon(IconPool.getIcon("cut"));
		miCut.setAccelerator(System.getProperty("os.name").startsWith("Mac") ? KeyStroke.getKeyStroke(KeyEvent.VK_X,
				KeyEvent.ALT_MASK) : KeyStroke.getKeyStroke(KeyEvent.VK_X, KeyEvent.CTRL_MASK));
		add(miCut);

		miPaste = new JMenuItem(actions.get(DefaultEditorKit.pasteAction));
		s = getInternationalText("Paste");
		miPaste.setText(s == null ? "Paste" : s);
		miPaste.setIcon(IconPool.getIcon("paste"));
		miPaste.setAccelerator(System.getProperty("os.name").startsWith("Mac") ? KeyStroke.getKeyStroke(KeyEvent.VK_V,
				KeyEvent.ALT_MASK) : KeyStroke.getKeyStroke(KeyEvent.VK_V, KeyEvent.CTRL_MASK));
		add(miPaste);

		miSelectAll = new JMenuItem(actions.get(DefaultEditorKit.selectAllAction));
		s = getInternationalText("SelectAll");
		miSelectAll.setText(s == null ? "Select All" : s);
		miSelectAll.setIcon(IconPool.getIcon("selectall"));
		miSelectAll.setAccelerator(System.getProperty("os.name").startsWith("Mac") ? KeyStroke.getKeyStroke(
				KeyEvent.VK_A, KeyEvent.ALT_MASK) : KeyStroke.getKeyStroke(KeyEvent.VK_A, KeyEvent.CTRL_MASK));
		add(miSelectAll);

	}

	static String getInternationalText(String name) {
		if (name == null)
			return null;
		if (bundle == null)
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

	/** destroy this object to prevent memory leak. */
	public void destroy() {
		setInvoker(null);
		Component c;
		AbstractButton b;
		for (int i = 0, n = getComponentCount(); i < n; i++) {
			c = getComponent(i);
			if (c instanceof AbstractButton) {
				b = (AbstractButton) c;
				b.setAction(null);
				ActionListener[] al = b.getActionListeners();
				if (al != null) {
					for (int k = 0; k < al.length; k++)
						b.removeActionListener(al[k]);
				}
			}
		}
		removeAll();
		setPasteAction(null);
		actions.clear();
		text = null;
	}

	public void show(Component invoker, int x, int y) {
		super.show(invoker, x, y);
		miCut.setEnabled(text.getSelectedText() != null && text.isEditable());
		miCopy.setEnabled(text.getSelectedText() != null);
		miPaste.setEnabled(text.isEditable());
		miSelectAll.setEnabled(text.getText() != null);
	}

	public JTextComponent getTextComponent() {
		return text;
	}

	/** set a customized action for pasting */
	public void setPasteAction(ActionListener listener) {
		// somehow removing action listener also removes text and icon so we must save them and restore later
		String text = miPaste.getText();
		Icon icon = miPaste.getIcon();
		ActionListener[] al = miPaste.getActionListeners();
		if (al != null) {
			for (int i = 0; i < al.length; i++)
				miPaste.removeActionListener(al[i]);
		}
		if (listener != null)
			miPaste.addActionListener(listener);
		miPaste.setText(text);
		miPaste.setIcon(icon);
	}

}
