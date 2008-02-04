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

import java.util.HashMap;

import javax.swing.ImageIcon;
import javax.swing.JLabel;

class TipBar extends JLabel {

	public final static String DEFAULT_TIP = "For help, press F1";

	private static HashMap<String, ImageIcon> map = new HashMap<String, ImageIcon>();

	private static ImageIcon bookIcon = new ImageIcon(TipBar.class.getResource("images/Book.gif"));
	private static ImageIcon editModeIcon = new ImageIcon(TipBar.class.getResource("text/images/EditorMode.gif"));
	private static ImageIcon viewModeIcon = new ImageIcon(TipBar.class.getResource("text/images/ViewerMode.gif"));
	private static ImageIcon mmlIcon = new ImageIcon(TipBar.class.getResource("text/images/MML.gif"));
	private static ImageIcon gblIcon = new ImageIcon(TipBar.class.getResource("text/images/GBL.gif"));
	private static ImageIcon xmlPageIcon = new ImageIcon(TipBar.class.getResource("text/images/XMLPage.gif"));
	private static ImageIcon htmlPageIcon = new ImageIcon(TipBar.class.getResource("text/images/HTMLPage.gif"));
	private static ImageIcon letterIcon = new ImageIcon(TipBar.class.getResource("text/images/Letter.gif"));
	private static ImageIcon jnlpIcon = new ImageIcon(TipBar.class.getResource("text/images/JNLP.gif"));
	private static ImageIcon imageIcon = new ImageIcon(TipBar.class.getResource("text/images/jpeg.gif"));
	private static ImageIcon movieIcon = new ImageIcon(TipBar.class.getResource("text/images/Film.gif"));
	private static ImageIcon waitIcon = new ImageIcon(TipBar.class.getResource("text/images/Wait.gif"));

	static {
		map.put(DEFAULT_TIP, bookIcon);
		map.put("Editor mode", editModeIcon);
		map.put("Viewer mode", viewModeIcon);
		map.put("Loading...", waitIcon);
		map.put("Loaded", xmlPageIcon);
		map.put("Loading aborted.", xmlPageIcon);
	}

	public void setText(String text) {
		if (text != null) {
			if (text.equals("Editor mode")) {
				String s = Modeler.getInternationalText("EditorMode");
				super.setText(s == null ? text : s);
			}
			else if (text.equals("Viewer mode")) {
				String s = Modeler.getInternationalText("ViewerMode");
				super.setText(s == null ? text : s);
			}
			else if (text.equals("Loading...")) {
				String s = Modeler.getInternationalText("Loading");
				super.setText(s == null ? text : s);
			}
			else if (text.equals("Loaded")) {
				String s = Modeler.getInternationalText("Loaded");
				super.setText(s == null ? text : s);
			}
			else {
				super.setText(text);
			}
			Object o = map.get(text);
			if (o instanceof ImageIcon) {
				setIcon((ImageIcon) o);
			}
			else {
				String s = text.toLowerCase();
				if (s.indexOf("mailto:") != -1) {
					setIcon(letterIcon);
				}
				else if (s.endsWith(".cml")) {
					setIcon(xmlPageIcon);
				}
				else if (s.endsWith(".mml")) {
					setIcon(mmlIcon);
				}
				else if (s.endsWith(".gbl")) {
					setIcon(gblIcon);
				}
				else if (s.endsWith(".jnlp")) {
					setIcon(jnlpIcon);
				}
				else if (s.endsWith(".rm") || s.endsWith(".ram") || s.endsWith(".mpg") || s.endsWith(".mpeg")
						|| s.endsWith(".qt") || s.endsWith(".mov")) {
					setIcon(movieIcon);
				}
				else if (s.endsWith(".gif") || s.endsWith(".png") || s.endsWith(".jpg") || s.endsWith(".jpeg")) {
					setIcon(imageIcon);
				}
				else if (s.endsWith(".html") || s.endsWith(".htm") || s.endsWith("/") || s.endsWith(".org")
						|| s.endsWith(".gov") || s.endsWith(".com") || s.endsWith(".edu")) {
					setIcon(htmlPageIcon);
				}
				else {
					setIcon(null);
				}
			}
		}
		else {
			String s = Modeler.getInternationalText("ForHelpPressF1");
			if (s == null) {
				super.setText(DEFAULT_TIP);
			}
			else {
				super.setText(s);
			}
			setIcon(bookIcon);
		}
	}

}