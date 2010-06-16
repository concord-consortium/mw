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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.HttpURLConnection;
import java.net.URLConnection;
import java.net.URLEncoder;

final class Receptionist {

	private int responseCode;

	void checkin() {

		if (!Modeler.directMW)
			return;

		StringBuffer sb = new StringBuffer();
		String os = System.getProperty("os.name") + " " + System.getProperty("os.version");
		String user = ModelerUtilities.getUnicode(System.getProperty("user.name"));
		try {
			sb.append("os=" + URLEncoder.encode(os, "UTF-8"));
			sb.append("&username=" + URLEncoder.encode(processSQLEscapeCharacters(user), "UTF-8"));
		}
		catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return;
		}
		sb.append("&javaversion=" + System.getProperty("java.version"));
		sb.append("&mwversion=" + Modeler.VERSION);
		boolean launcher = "yes".equals(System.getProperty("mw.launcher"));
		String jws = Modeler.launchedByJWS ? "yes" : (Modeler.directMW ? (launcher ? "via" : "no") : "emb");
		sb.append("&jws=" + jws);

		URL url = null;
		try {
			url = new URL(Modeler.getContextRoot() + "reception?" + sb);
		}
		catch (MalformedURLException e) {
			e.printStackTrace();
			return;
		}
		URLConnection con = ConnectionManager.getConnection(url);
		if (!(con instanceof HttpURLConnection))
			return;
		try {
			((HttpURLConnection) con).setRequestMethod("POST");
		}
		catch (ProtocolException e) {
			e.printStackTrace();
			return;
		}
		con.setDoOutput(true);

		try {
			responseCode = ((HttpURLConnection) con).getResponseCode();
		}
		catch (IOException e) {
			e.printStackTrace();
		}

	}

	boolean isHttpOK() {
		return responseCode == HttpURLConnection.HTTP_OK;
	}

	private static String processSQLEscapeCharacters(String s) {
		return s.replaceAll("\"", "\"\"").replaceAll("'", "''");
	}

}