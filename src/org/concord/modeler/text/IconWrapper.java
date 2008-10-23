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
package org.concord.modeler.text;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JPopupMenu;

import org.concord.modeler.BasicPageTextBox;
import org.concord.modeler.ModelerUtilities;

/**
 * This special component is used to wrap icons with a JLabel, in order to fix the alignment bug.
 * 
 * <p>
 * The alignment bug: When we put a component and an icon in the same line, there seems to be a bug that causes the icon
 * to shift vertically by 50%.
 * </p>
 * 
 * <p>
 * The Embeddable interface is not really implemented. Here it is used more as a type setter.
 * </p>
 * 
 * @author Charles Xie
 * 
 */
public class IconWrapper extends BasicPageTextBox {

	private Icon icon;

	private MouseListener popupMouseListener = new MouseAdapter() {
		public void mousePressed(MouseEvent e) {
			int i = getPage().getPosition(IconWrapper.this);
			if (i != -1)
				getPage().setCaretPosition(i);
			if (isTextSelected())
				return;
			if (ModelerUtilities.isRightClick(e)) {
				if (textBody.getDefaultPopupMenu() != null)
					textBody.getDefaultPopupMenu().show(IconWrapper.this, e.getX() + 5, e.getY() + 5);
			}
		}
	};

	public IconWrapper(Icon icon0, Page page0) {
		super();
		this.icon = icon0;
		setOpaque(false);
		setEditable(false);
		setPreferredSize(new Dimension(icon.getIconWidth(), icon.getIconHeight()));
		if (icon instanceof LineIcon) {
			int w = Math.min(10, icon.getIconWidth() / 10);
			int h = Math.min(10, icon.getIconHeight() / 10);
			setBorder(BorderFactory.createEmptyBorder(h, w, h, w));
			setContentType("text/html");
			LineIcon li = (LineIcon) icon;
			decodeText(li.getText());
			li.setWrapper(this);
			setPage(page0);
		}
		else {
			this.page = page0;
			setText(null);
		}
		textBody.setTextBox(this);
		if (icon instanceof LineIcon) {
			textBody.setDefaultPopupMenu(page.colorBarPopupMenu);
		}
		else if (icon instanceof ImageIcon) {
			textBody.setDefaultPopupMenu(page.imagePopupMenu);
		}
	}

	static IconWrapper newInstance(Icon icon, Page page) {
		if (icon instanceof LineIcon)
			return new IconWrapper(new LineIcon(((LineIcon) icon)), page);
		return new IconWrapper(icon, page);
	}

	public Icon getIcon() {
		return icon;
	}

	public void paintComponent(Graphics g) {
		if (icon != null)
			icon.paintIcon(this, g, 0, 0);
		super.paintComponent(g);
	}

	public void setChangable(boolean b) {
		super.setChangable(b);
		if (b) {
			if (!isChangable()) {
				addMouseListener(popupMouseListener);
			}
		}
		else {
			if (isChangable()) {
				removeMouseListener(popupMouseListener);
			}
		}
	}

	public boolean isChangable() {
		MouseListener[] ml = getMouseListeners();
		if (ml == null)
			return false;
		for (MouseListener l : ml) {
			if (l == popupMouseListener)
				return true;
		}
		return false;
	}

	public JPopupMenu getPopupMenu() {
		return textBody.getDefaultPopupMenu();
	}

	// ignore
	public void createPopupMenu() {

	}

}