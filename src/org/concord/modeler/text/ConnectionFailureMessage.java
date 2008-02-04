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

class ConnectionFailureMessage implements ErrorMessage {

	private Exception exception;
	private Style[] styles;

	public ConnectionFailureMessage(Page page) {

		styles = new Style[3];

		styles[0] = page.addStyle(null, null);
		StyleConstants.setIcon(styles[0], new ImageIcon(getClass().getResource("images/Connection.gif")));

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
				"  The server could not be found\n\n",
				"URL: "
						+ (exception != null ? exception.getMessage() : "not specified")
						+ "\n\nThe "
						+ Page.getSoftwareName()
						+ " cannot open the above page.\n\nPlease try the following:\n\n   1. Check if your computer is connected to the Internet. Try a public web site such as google.com with a web browser. If you cannot open google.com with your browser, there must be a problem for your Internet connection.\n\n   2. Is your computer behind a firewall or using a proxy server to access the Internet? If so, please contact your Network Administrator to get some help.\n\n   3. The timeouts are probably set to be too short (the default setting for the timeout for opening a connection is 5 seconds). Please select the \"Preference\" menu of the "
						+ Page.getSoftwareName()
						+ " and increase the timeouts in the \"Connection\" tab.\n\n   4. If all the above fails, it is most likely that the Internet Service Provider that hosts the "
						+ Page.getSoftwareName()
						+ " is down. Please select the \"Work Offline\" mode (use the \"Work Offline\" item under the \"File\" menu). If a page has been visited before, the cached version will be shown. Otherwise, you will see this page again.\n\n   5. Regardless of whether there is a connection problem or not, you can always work on the files stored on your disk, or anything that does not require an Internet connection. For example, you can use the \"New Blank Page\" item of the \"File\" menu to create a new blank page, type something on it, and use the items under the \"Insert\" menu to insert various components." };
	}

	public void setException(Exception e) {
		exception = e;
	}

	public Exception getException() {
		return exception;
	}

}