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

import java.awt.Color;

import javax.swing.ImageIcon;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;

final class LoadingErrorMessage implements ErrorMessage {

	private Exception exception;
	private Style[] styles;

	public LoadingErrorMessage(Page page) {

		styles = new Style[2];

		styles[0] = page.addStyle(null, null);
		StyleConstants.setIcon(styles[0], new ImageIcon(getClass().getResource("images/Refresh.gif")));

		styles[1] = page.addStyle(null, null);
		StyleConstants.setFontSize(styles[1], 16);
		StyleConstants.setForeground(styles[1], Color.black);
		StyleConstants.setFontFamily(styles[1], Page.getDefaultFontFamily());
		StyleConstants.setBold(styles[1], true);

	}

	public Style[] getStyles() {
		return styles;
	}

	public String[] getStrings() {
		String[] text = {
				" ",
				"  A temporary loading error has occurred. Please press the Refresh Button, or simply press F5 key (Windows) or CTRL+R (Mac OS X) to try again. (The Refresh Button is on the top tool bar.)" };
		return text;
	}

	public void setException(Exception e) {
		exception = e;
	}

	public Exception getException() {
		return exception;
	}

}