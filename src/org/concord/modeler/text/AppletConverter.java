/*
 *   Copyright (C) 2010  The Concord Consortium, Inc.,
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.concord.modeler.Modeler;
import org.concord.modeler.util.FileUtilities;

/**
 * @author Charles Xie
 */

final class AppletConverter {

	private final static String LINE_SEPARATOR = System.getProperty("line.separator");

	private Page page;

	AppletConverter(Page page) {
		if (page == null)
			throw new IllegalArgumentException("page cannot be null");
		this.page = page;
	}

	void write(final File file) {

		FileOutputStream out = null;
		try {
			out = new FileOutputStream(file);
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		if (out == null)
			return;

		StringBuffer sb = new StringBuffer();

		sb.append("<html>");
		sb.append(LINE_SEPARATOR);

		sb.append("  <head>");
		sb.append(LINE_SEPARATOR);
		sb.append("    <title>" + XMLCharacterEncoder.encode(page.getTitle()) + "</title>");
		sb.append(LINE_SEPARATOR);
		sb.append("  </head>");
		sb.append(LINE_SEPARATOR);

		sb.append("  <body>");
		sb.append(LINE_SEPARATOR);

		sb.append("    <p>If nothing shows up, download <a href=\"" + Modeler.getStaticRoot()
				+ "lib/mw.jar\">mw.jar</a> to where this HTML file is located, and then refresh this page.</p>");
		sb.append(LINE_SEPARATOR);

		sb.append("    <center>");
		sb.append(LINE_SEPARATOR);

		sb
				.append("      <applet code=\"org.concord.modeler.MwApplet\" archive=\"mw.jar\" width=\"400\" height=\"600\">");
		sb.append(LINE_SEPARATOR);
		sb.append("        <param name=\"cache_archive\" value=\"mw.jar\">");
		sb.append(LINE_SEPARATOR);
		sb.append("        <param name=\"java_arguments\" value=\"-Djnlp.packEnabled=true\"/>");
		sb.append(LINE_SEPARATOR);
		sb.append("        <param name=\"script\" value=\"page:0:import "
				+ FileUtilities.getFileName(page.getAddress()) + "\"/>");
		sb.append(LINE_SEPARATOR);
		sb.append("      </applet>");
		sb.append(LINE_SEPARATOR);

		sb.append("    </center>");
		sb.append(LINE_SEPARATOR);

		sb.append("  </body>");
		sb.append(LINE_SEPARATOR);

		sb.append("</html>");

		try {
			out.write(sb.toString().getBytes());
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		finally {
			try {
				out.close();
			}
			catch (IOException e) {
			}
		}

	}

}