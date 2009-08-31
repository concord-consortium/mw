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
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseListener;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import org.concord.modeler.chemistry.PeriodicTable;
import org.concord.modeler.text.Page;

public class PagePeriodicTable extends PeriodicTable implements Embeddable {

	Page page;
	private int index;
	private String uid;
	private boolean marked;
	private String borderType;
	private static Color defaultBackground, defaultForeground;
	private JPopupMenu popupMenu;
	private static PagePeriodicTableMaker maker;
	private MouseListener popupMouseListener;

	public PagePeriodicTable() {
		super();
		if (defaultBackground == null)
			defaultBackground = getBackground();
		if (defaultForeground == null)
			defaultForeground = getForeground();
		popupMouseListener = new PopupMouseListener(this);
		addMouseListener(popupMouseListener);
	}

	public PagePeriodicTable(PagePeriodicTable t, Page parent) {
		this();
		setPage(parent);
		setBackground(t.getBackground());
		setForeground(t.getForeground());
		setPreferredSize(t.getPreferredSize());
		setChangable(page.isEditable());
	}

	public void destroy() {
		page = null;
		if (maker != null)
			maker.setObject(null);
	}

	public JPopupMenu getPopupMenu() {
		return popupMenu;
	}

	public void createPopupMenu() {
		popupMenu = new JPopupMenu();
		popupMenu.setInvoker(this);
		String s = Modeler.getInternationalText("CustomizePeriodicTable");
		final JMenuItem miCustom = new JMenuItem((s != null ? s : "Customize This Periodic Table") + "...");
		miCustom.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (maker == null) {
					maker = new PagePeriodicTableMaker(PagePeriodicTable.this);
				}
				else {
					maker.setObject(PagePeriodicTable.this);
				}
				maker.invoke(page);
			}
		});
		popupMenu.add(miCustom);
		s = Modeler.getInternationalText("RemovePeriodicTable");
		final JMenuItem miRemove = new JMenuItem(s != null ? s : "Remove This Periodic Table");
		miRemove.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				page.removeComponent(PagePeriodicTable.this);
			}
		});
		popupMenu.add(miRemove);
		s = Modeler.getInternationalText("CopyPeriodicTable");
		JMenuItem mi = new JMenuItem(s != null ? s : "Copy This Periodic Table");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				page.copyComponent(PagePeriodicTable.this);
			}
		});
		popupMenu.add(mi);
		popupMenu.pack();
		popupMenu.addPopupMenuListener(new PopupMenuListener() {
			public void popupMenuCanceled(PopupMenuEvent e) {
			}

			public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
				miCustom.setEnabled(isChangable());
				miRemove.setEnabled(isChangable());
			}

			public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
			}
		});
	}

	public void setIndex(int i) {
		index = i;
	}

	public int getIndex() {
		return index;
	}

	public void setUid(String uid) {
		this.uid = uid;
	}

	public String getUid() {
		return uid;
	}

	public String getBorderType() {
		if (borderType == null)
			return BorderManager.getBorder(this);
		return borderType;
	}

	public void setBorderType(String s) {
		borderType = s;
		BorderManager.setBorder(this, s, page.getBackground());
	}

	public void setPreferredSize(Dimension dim) {
		super.setPreferredSize(dim);
		super.setMaximumSize(dim);
		super.setMinimumSize(dim);
	}

	public void setMarked(boolean b) {
		marked = b;
		if (page == null)
			return;
		setBackground(b ? page.getSelectionColor() : defaultBackground);
		setForeground(b ? page.getSelectedTextColor() : defaultForeground);
	}

	public boolean isMarked() {
		return marked;
	}

	public void setChangable(boolean b) {
		if (b) {
			if (!isChangable())
				addMouseListener(popupMouseListener);
		}
		else {
			if (isChangable())
				removeMouseListener(popupMouseListener);
		}
	}

	public boolean isChangable() {
		MouseListener[] ml = getMouseListeners();
		for (MouseListener x : ml) {
			if (x == popupMouseListener)
				return true;
		}
		return false;
	}

	public void setPage(Page p) {
		page = p;
	}

	public Page getPage() {
		return page;
	}

	public static PagePeriodicTable create(Page page) {
		if (page == null)
			return null;
		PagePeriodicTable pt = new PagePeriodicTable();
		if (maker == null) {
			maker = new PagePeriodicTableMaker(pt);
		}
		else {
			maker.setObject(pt);
		}
		maker.invoke(page);
		if (maker.cancel)
			return null;
		return pt;
	}

	public String toString() {
		StringBuffer sb = new StringBuffer("<class>" + getClass().getName() + "</class>\n");
		if (uid != null)
			sb.append("<uid>" + uid + "</uid>\n");
		if (!isMuted())
			sb.append("<mute>false</mute>\n");
		if (!isOpaque()) {
			sb.append("<opaque>false</opaque>\n");
		}
		else {
			if (!getBackground().equals(defaultBackground))
				sb.append("<bgcolor>" + Integer.toString(getBackground().getRGB(), 16) + "</bgcolor>\n");
		}
		if (!getForeground().equals(defaultForeground))
			sb.append("<fgcolor>" + Integer.toString(getForeground().getRGB(), 16) + "</fgcolor>\n");
		if (borderType != null)
			sb.append("<border>" + borderType + "</border>\n");
		return sb.toString();
	}

}
