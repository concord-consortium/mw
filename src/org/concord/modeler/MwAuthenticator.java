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
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;

/**
 * @author Charles Xie
 * 
 */
final class MwAuthenticator {

	private String servletLocation;
	private int statusCode;

	public MwAuthenticator() {
		servletLocation = Modeler.getContextRoot() + "auth";
	}

	public boolean isAuthorized(String userID, String password) {

		statusCode = 0;

		String path = servletLocation;
		try {
			path += "?client=mw&userid=" + URLEncoder.encode(userID, "UTF-8") + "&password="
					+ URLEncoder.encode(password, "UTF-8");
		}
		catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return false;
		}

		URL url = null;
		try {
			url = new URL(path);
		}
		catch (MalformedURLException e) {
			e.printStackTrace();
			return false;
		}

		URLConnection conn = ConnectionManager.getConnection(url);
		if (conn == null)
			return false;

		try {
			statusCode = ((HttpURLConnection) conn).getResponseCode();
		}
		catch (IOException e) {
			e.printStackTrace();
		}

		return statusCode == HttpURLConnection.HTTP_OK;

	}

}
