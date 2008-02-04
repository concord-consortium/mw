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

/**
 * This collects parameters a hyperlink can be assigned for openning a new window. See
 * http://www.yourhtmlsource.com/javascript/popupwindows.html
 */

public class HyperlinkParameter {

	public final static String ATTRIBUTE_LEFT = "link_left";
	public final static String ATTRIBUTE_TOP = "link_top";
	public final static String ATTRIBUTE_WIDTH = "link_width";
	public final static String ATTRIBUTE_HEIGHT = "link_height";
	public final static String ATTRIBUTE_RESIZABLE = "link_resizable";
	public final static String ATTRIBUTE_FULLSCREEN = "link_fullscreen";
	public final static String ATTRIBUTE_TOOLBAR = "link_toolbar";
	public final static String ATTRIBUTE_MENUBAR = "link_menubar";
	public final static String ATTRIBUTE_STATUSBAR = "link_statusbar";

	private int left;
	private int top;
	private int width;
	private int height;
	private boolean resizable = true;
	private boolean fullscreen;
	private boolean toolbar = true;
	private boolean menubar = true;
	private boolean statusbar = true;

	void reset() {
		left = top = width = height = 0;
		resizable = toolbar = menubar = statusbar = true;
		fullscreen = false;
	}

	public int getLeft() {
		return left;
	}

	public void setLeft(int i) {
		left = i;
	}

	public int getTop() {
		return top;
	}

	public void setTop(int i) {
		top = i;
	}

	public int getWidth() {
		return width;
	}

	public void setWidth(int i) {
		width = i;
	}

	public int getHeight() {
		return height;
	}

	public void setHeight(int i) {
		height = i;
	}

	public boolean getResizable() {
		return resizable;
	}

	public void setResizable(boolean b) {
		resizable = b;
	}

	public boolean getFullscreen() {
		return fullscreen;
	}

	public void setFullscreen(boolean b) {
		fullscreen = b;
	}

	public boolean getToolbar() {
		return toolbar;
	}

	public void setToolbar(boolean b) {
		toolbar = b;
	}

	public boolean getMenubar() {
		return menubar;
	}

	public void setMenubar(boolean b) {
		menubar = b;
	}

	public boolean getStatusbar() {
		return statusbar;
	}

	public void setStatusbar(boolean b) {
		statusbar = b;
	}

}