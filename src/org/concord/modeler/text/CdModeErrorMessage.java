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

import javax.swing.ImageIcon;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.html.HTML;

class CdModeErrorMessage implements ErrorMessage {

	private Exception exception;
	private Style[] styles;

	public CdModeErrorMessage(Page page) {

		styles = new Style[4];

		styles[0] = page.addStyle(null, null);
		StyleConstants.setIcon(styles[0], new ImageIcon(getClass().getResource("images/Connection.gif")));

		styles[1] = page.addStyle(null, null);
		StyleConstants.setFontSize(styles[1], Page.getDefaultFontSize() + 4);
		StyleConstants.setFontFamily(styles[1], Page.getDefaultFontFamily());
		StyleConstants.setBold(styles[1], true);

		styles[2] = page.addStyle(null, null);
		StyleConstants.setFontSize(styles[2], Page.getDefaultFontSize());
		StyleConstants.setFontFamily(styles[2], Page.getDefaultFontFamily());

		styles[3] = page.addStyle(null, null);
		styles[3].addAttribute(HTML.Attribute.HREF, "http://mw.concord.org/modeler/index.html");
		StyleConstants.setFontSize(styles[3], Page.getDefaultFontSize() + 2);
		StyleConstants.setFontFamily(styles[3], Page.getDefaultFontFamily());
		StyleConstants.setBold(styles[3], true);
		StyleConstants.setUnderline(styles[3], Page.isLinkUnderlined());
		StyleConstants.setForeground(styles[3], Page.getLinkColor());

	}

	public Style[] getStyles() {
		return styles;
	}

	public String[] getStrings() {
		return new String[] {
				" ",
				"  This is a CD version of the " + Page.getSoftwareName() + " software.\n\n",
				"  This version has blocked access to the Web.\n\n  We highly recommend that you visit the following web site to download the fully-functioning version:\n\n       ",
				"http://mw.concord.org/modeler/index.html" };
	}

	public void setException(Exception e) {
		exception = e;
	}

	public Exception getException() {
		return exception;
	}

}