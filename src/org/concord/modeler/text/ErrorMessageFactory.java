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

import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.NoRouteToHostException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import org.xml.sax.SAXException;

class ErrorMessageFactory {

	private ErrorMessageFactory() {
	}

	static ErrorMessage createErrorMessage(Exception e, Page page) {
		ErrorMessage m = null;
		if (e instanceof SocketTimeoutException)
			m = new ConnectionFailureMessage(page);
		else if (e instanceof SocketException)
			m = new ConnectionFailureMessage(page);
		else if (e instanceof FileNotFoundException)
			m = new PageUnavailableMessage(page);
		else if (e instanceof UnknownHostException)
			m = new UnknownHostMessage(page);
		else if (e instanceof NoRouteToHostException)
			m = new NoRouteToHostMessage(page);
		else if (e instanceof SAXException)
			m = new CorruptedDataMessage(page);
		else if (e instanceof UnsupportedFormatException)
			m = new WrongFormatMessage(page);
		else if (e instanceof MalformedURLException)
			m = new InvalidURLErrorMessage(page);
		else if (e instanceof UnsupportedEncodingException)
			m = new UnsupportedEncodingMessage(page);
		else if (e instanceof CdModeException)
			m = new CdModeErrorMessage(page);
		if (m != null)
			m.setException(e);
		return m;
	}

}