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

import javax.swing.JOptionPane;

public final class ExternalClient {

	private ExternalClient() {
	}

	public final static byte HTML_CLIENT = 21;
	public final static byte EMAIL_CLIENT = 22;
	public final static byte FLASH_CLIENT = 23;
	public final static byte JNLP_CLIENT = 24;
	public final static byte QUICKTIME_CLIENT = 25;
	public final static byte REALPLAYER_CLIENT = 26;
	public final static byte PDF_CLIENT = 27;

	/**
	 * open the address with the specified external client.
	 * 
	 * @param type
	 *            the type of client, such as an email client or a web browser.
	 * @param address
	 *            in http, file and mailto protocols.
	 */
	public static void open(byte type, String address) {
		boolean clientOK = false;
		switch (type) {
		case HTML_CLIENT:
		case PDF_CLIENT:
			try {
				if (Page.OS.startsWith("Windows")) {
					Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler " + address);
					clientOK = true;
				}
				else if (Page.OS.startsWith("Mac OS")) {
					Runtime.getRuntime().exec(new String[] { "open", address });
					clientOK = true;
				}
				else { // linux or unix
					String[] browsers = { "firefox", "mozilla", "opera", "netscape", "konqueror", "epiphany" };
					String browser = null;
					for (int i = 0; i < browsers.length; i++) {
						if (Runtime.getRuntime().exec(new String[] { "which", browsers[i] }).waitFor() == 0) {
							browser = browsers[i];
							break;
						}
					}
					if (browser != null) {
						Runtime.getRuntime().exec(new String[] { browser, address });
						clientOK = true;
					}
				}
			}
			catch (Exception ex) {
				ex.printStackTrace(System.err);
				clientOK = false;
			}
			break;
		case FLASH_CLIENT:
			try {
				if (Page.OS.startsWith("Windows")) {
					Runtime.getRuntime().exec(new String[] { "explorer", address });
					clientOK = true;
				}
				else if (Page.OS.startsWith("Mac OS")) {
					Runtime.getRuntime().exec(new String[] { "open", address });
					clientOK = true;
				}
				else { // linux or unix
					String[] browsers = { "firefox", "mozilla", "opera", "netscape", "konqueror", "epiphany" };
					String browser = null;
					for (int i = 0; i < browsers.length; i++) {
						if (Runtime.getRuntime().exec(new String[] { "which", browsers[i] }).waitFor() == 0) {
							browser = browsers[i];
							break;
						}
					}
					if (browser != null) {
						Runtime.getRuntime().exec(new String[] { browser, address });
						clientOK = true;
					}
				}
			}
			catch (Exception ex) {
				ex.printStackTrace(System.err);
				clientOK = false;
			}
			break;
		case JNLP_CLIENT:
			clientOK = startCommand("javaws", address);
			break;
		case QUICKTIME_CLIENT:
			clientOK = startCommand("QuickTimePlayer", address);
			break;
		case REALPLAYER_CLIENT:
			clientOK = startCommand("realplay", address);
			break;
		case EMAIL_CLIENT:
			clientOK = startCommand("mailto:" + address, null);
			break;
		}
		if (!clientOK) {
			String s = null;
			switch (type) {
			case EMAIL_CLIENT:
				s = "default Email client";
				break;
			case HTML_CLIENT:
			case FLASH_CLIENT:
				s = "default Web browser";
				break;
			case REALPLAYER_CLIENT:
				s = "Real Player";
				break;
			case QUICKTIME_CLIENT:
				s = "Quick Time Player";
				break;
			case JNLP_CLIENT:
				s = "Java Web Start command";
				break;
			case PDF_CLIENT:
				s = "Adobe PDF Reader";
				break;
			}
			if (Page.isApplet()) {
				JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(null),
						"The linked application is not permitted to run in the applet mode.");

			}
			else {
				JOptionPane.showMessageDialog(null, s != null ? "The " + s + " was not found."
						: "No such client supported", "External client", JOptionPane.ERROR_MESSAGE);
			}
		}
	}

	/* start a command on different platforms */
	private static boolean startCommand(String command, String param) {
		boolean clientOK = false;
		if (Page.OS.startsWith("Windows 2000") || Page.OS.startsWith("Windows XP")) {
			try {
				if (param == null) {
					Runtime.getRuntime().exec(new String[] { "cmd", "/c", "start", command });
				}
				else {
					Runtime.getRuntime().exec(new String[] { "cmd", "/c", "start", command, param });
				}
				clientOK = true;
			}
			catch (Exception e) {
				e.printStackTrace(System.err);
				clientOK = false;
			}
		}
		else if (Page.OS.startsWith("Windows 95") || Page.OS.startsWith("Windows 98")
				|| Page.OS.startsWith("Windows NT") || Page.OS.startsWith("Windows ME")) {
			try {
				if (param == null) {
					Runtime.getRuntime().exec(new String[] { "start", command });
				}
				else {
					Runtime.getRuntime().exec(new String[] { "start", command, param });
				}
				clientOK = true;
			}
			catch (Exception e) {
				e.printStackTrace(System.err);
				clientOK = false;
			}
		}
		else if (Page.OS.startsWith("Mac")) {
			try {
				Runtime.getRuntime().exec(new String[] { "open", param });
				clientOK = true;
			}
			catch (Exception e) {
				e.printStackTrace(System.err);
				clientOK = false;
			}
		}
		return clientOK;
	}

}