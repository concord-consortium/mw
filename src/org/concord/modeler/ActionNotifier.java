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

import java.awt.Component;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JOptionPane;

/**
 * Notify the user an action, and give the user a chance to turn the notifier off in this session.
 * 
 * @author Charles Xie
 */

public class ActionNotifier {

	public final static Byte BACK_TO_ACTUAL_SIZE = 0;

	private Component parent;
	private Map<Byte, Boolean> holder;
	private final static String[] OK_DONTSHOW_OPTIONS = { "OK", "Don't show this message again." };

	public ActionNotifier(Component parent) {
		setParentComponent(parent);
		holder = new HashMap<Byte, Boolean>();
		holder.put(BACK_TO_ACTUAL_SIZE, Boolean.TRUE);
	}

	public void setParentComponent(Component parent) {
		this.parent = parent;
	}

	public int show(Byte type) {

		if (type.equals(BACK_TO_ACTUAL_SIZE)) {
			if (holder.get(BACK_TO_ACTUAL_SIZE)) {
				int i = JOptionPane.showOptionDialog(JOptionPane.getFrameForComponent(parent),
						"Text size must be set to the actual size in editor mode.", "Action Notifier",
						JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE, null, OK_DONTSHOW_OPTIONS,
						OK_DONTSHOW_OPTIONS[0]);
				if (i == JOptionPane.NO_OPTION) {
					holder.put(BACK_TO_ACTUAL_SIZE, Boolean.FALSE);
				}
				return i;
			}
		}

		return -1;

	}

}