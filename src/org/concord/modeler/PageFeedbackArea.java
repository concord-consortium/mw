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
import java.net.MalformedURLException;
import java.net.URL;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import org.concord.modeler.text.Page;

public class PageFeedbackArea extends FeedbackArea implements Embeddable, AutoResizable {

	Page page;
	boolean widthIsRelative, heightIsRelative;
	float widthRatio = 1, heightRatio = 1;
	private int index;
	private String uid;
	private boolean marked;
	private boolean wasOpaque;
	private Color background;
	private static Color defaultBackground, defaultForeground;
	private JPopupMenu popupMenu;
	private static PageFeedbackAreaMaker maker;
	private MouseListener popupMouseListener;

	public PageFeedbackArea() {
		super();
		init();
	}

	public PageFeedbackArea(PageFeedbackArea area, Page parent) {
		this();
		setPage(parent);
		setUid(area.uid);
		setBackground(area.getBackground());
		setBorderType(area.getBorderType());
		setOpaque(area.isOpaque());
		setWidthRelative(area.isWidthRelative());
		setHeightRelative(area.isHeightRelative());
		int w = area.getPreferredSize().width;
		int h = area.getPreferredSize().height;
		if (isWidthRelative()) {
			setWidthRatio(area.getWidthRatio());
			w = (int) (page.getWidth() * getWidthRatio());
		}
		if (isHeightRelative()) {
			setHeightRatio(area.getHeightRatio());
			h = (int) (page.getHeight() * getHeightRatio());
		}
		setPreferredSize(new Dimension(w, h));
		setChangable(page.isEditable());
	}

	private void init() {
		if (defaultBackground == null)
			defaultBackground = getBackground();
		if (defaultForeground == null)
			defaultForeground = getForeground();
		popupMouseListener = new PopupMouseListener(this);
		addMouseListener(popupMouseListener);
		URL u = null;
		try {
			u = new URL(Modeler.getContextRoot() + "comment");
		}
		catch (MalformedURLException e) {
			e.printStackTrace();
		}
		if (u != null)
			inputArea.setServletURL(u);
		setUser(Modeler.user);
	}

	CommentInputPane getInputArea() {
		return inputArea;
	}

	public void destroy() {
		page = null;
	}

	public JPopupMenu getPopupMenu() {
		return popupMenu;
	}

	public void createPopupMenu() {

		if (popupMenu != null)
			return;

		popupMenu = new JPopupMenu();
		popupMenu.setInvoker(this);

		JMenuItem mi = new JMenuItem("Customize This Feedback Area...");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (maker == null) {
					maker = new PageFeedbackAreaMaker(PageFeedbackArea.this);
				}
				else {
					maker.setObject(PageFeedbackArea.this);
				}
				maker.invoke(page);
			}
		});
		popupMenu.add(mi);

		mi = new JMenuItem("Remove This Feedback Area");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				page.removeComponent(PageFeedbackArea.this);
			}
		});
		popupMenu.add(mi);

		mi = new JMenuItem("Copy This Feedback Area");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				page.copyComponent(PageFeedbackArea.this);
			}
		});
		popupMenu.add(mi);

		popupMenu.pack();

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

	public void setTransparent(boolean b) {
		setOpaque(!b);
	}

	public boolean isTransparent() {
		return !isOpaque();
	}

	public void setWidthRelative(boolean b) {
		widthIsRelative = b;
	}

	public boolean isWidthRelative() {
		return widthIsRelative;
	}

	public void setWidthRatio(float wr) {
		widthRatio = wr;
	}

	public float getWidthRatio() {
		return widthRatio;
	}

	public void setHeightRelative(boolean b) {
		heightIsRelative = b;
	}

	public boolean isHeightRelative() {
		return heightIsRelative;
	}

	public void setHeightRatio(float hr) {
		heightRatio = hr;
	}

	public float getHeightRatio() {
		return heightRatio;
	}

	public void setMarked(boolean b) {
		marked = b;
		if (page == null)
			return;
		if (b) {
			wasOpaque = isOpaque();
			setOpaque(true);
			background = getBackground();
		}
		else {
			setOpaque(wasOpaque);
		}
		setBackground(b ? page.getSelectionColor() : background);
		setForeground(b ? page.getSelectedTextColor() : defaultForeground);
	}

	public boolean isMarked() {
		return marked;
	}

	public void setPage(Page p) {
		page = p;
		if (page != null) {
			inputArea.setPageAddress(page.getAddress());
			inputArea.setRegisterAction(new Runnable() {
				public void run() {
					page.getNavigator().visitLocation(Modeler.getContextRoot() + "register.jsp?client=mw");
				}
			});
		}
	}

	public Page getPage() {
		return page;
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

	public static PageFeedbackArea create(Page page) {
		if (page == null)
			return null;
		PageFeedbackArea area = new PageFeedbackArea();
		if (maker == null) {
			maker = new PageFeedbackAreaMaker(area);
		}
		else {
			maker.setObject(area);
		}
		maker.invoke(page);
		if (maker.cancel)
			return null;
		return area;
	}

	public void setPreferredSize(Dimension dim) {
		if (dim == null)
			throw new IllegalArgumentException("null object");
		if (dim.width == 0 || dim.height == 0)
			throw new IllegalArgumentException("zero dimension");
		super.setPreferredSize(dim);
		super.setMinimumSize(dim);
		super.setMaximumSize(dim);
		synchronized (displayPane) {
			displayPane.setPreferredSize(new Dimension(dim.width - 10, -1));
		}
	}

	public String getBorderType() {
		return BorderManager.getBorder(this);
	}

	public void setBorderType(String s) {
		BorderManager.setBorder(this, s, page.getBackground());
	}

	public String toString() {
		StringBuffer sb = new StringBuffer("<class>" + getClass().getName() + "</class>\n");
		if (uid != null)
			sb.append("<uid>" + uid + "</uid>\n");
		sb.append("<width>" + (widthIsRelative ? (widthRatio > 1.0f ? 1 : widthRatio) : getWidth()) + "</width>\n");
		sb.append("<height>" + (heightIsRelative ? (heightRatio > 1.0f ? 1 : heightRatio) : getHeight())
				+ "</height>\n");
		if (!isOpaque())
			sb.append("<opaque>false</opaque>\n");
		if (!getBorderType().equals(BorderManager.BORDER_TYPE[0]))
			sb.append("<border>" + getBorderType() + "</border>");
		return sb.toString();
	}

}