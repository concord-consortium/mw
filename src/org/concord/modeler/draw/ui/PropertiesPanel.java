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

package org.concord.modeler.draw.ui;

import java.awt.LayoutManager;
import java.awt.Point;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import javax.swing.JDialog;
import javax.swing.JPanel;

public abstract class PropertiesPanel extends JPanel {

	private static ResourceBundle bundle;
	private static boolean isUSLocale;
	static Point offset;

	JDialog dialog;
	boolean cancelled;

	PropertiesPanel(LayoutManager layout) {
		super(layout);
	}

	void localize() {
		if (bundle == null) {
			isUSLocale = Locale.getDefault().equals(Locale.US);
			if (!isUSLocale) {
				try {
					bundle = ResourceBundle.getBundle("org.concord.modeler.draw.ui.resources.PropertiesPanel", Locale
							.getDefault());
				}
				catch (MissingResourceException e) {
				}
			}
		}
	}

	static String getInternationalText(String name) {
		if (bundle == null)
			return null;
		if (isUSLocale)
			return null;
		if (name == null)
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

	public boolean isCancelled() {
		return cancelled;
	}

	public void setDialog(JDialog d) {
		dialog = d;
	}

	public static void setOffset(Point p) {
		offset = p;
	}

	public static Point getOffset() {
		return offset;
	}

	public int getIndex() {
		return -1;
	}

	public void notifyChange() {
	}

}
