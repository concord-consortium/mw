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

final class WrongFormatMessage implements ErrorMessage {

	private Exception exception;
	private Style[] styles;

	public WrongFormatMessage(Page page) {

		styles = new Style[3];

		styles[0] = page.addStyle(null, null);
		StyleConstants.setIcon(styles[0], new ImageIcon(getClass().getResource("images/WrongFormat.gif")));

		styles[1] = page.addStyle(null, null);
		StyleConstants.setFontSize(styles[1], Page.getDefaultFontSize() + 4);
		StyleConstants.setFontFamily(styles[1], Page.getDefaultFontFamily());
		StyleConstants.setBold(styles[1], true);

		styles[2] = page.addStyle(null, null);
		StyleConstants.setFontSize(styles[2], Page.getDefaultFontSize() - 1);
		StyleConstants.setFontFamily(styles[2], Page.getDefaultFontFamily());

	}

	public Style[] getStyles() {
		return styles;
	}

	public String[] getStrings() {
		return new String[] {
				" ",
				"  We are sorry, the content of this URL cannot be displayed.\n\n",
				"Error URL: "
						+ (exception != null ? exception.getMessage() : "Not specified")
						+ "\n\nThe "
						+ Page.getSoftwareName()
						+ " browses only through the HTTP and file protocol. It supports only these formats: *.cml (the main XML format for the Molecular Workbench pages), *.pdb (the Protein Data Bank format), *.xyz (a data format for storing three-dimensional coordinates), *.gif, *.png, *.jpg, and *.txt. It does not show content in the HTML format. Nor does it show a directory structure." };
	}

	public void setException(Exception e) {
		exception = e;
	}

	public Exception getException() {
		return exception;
	}

}