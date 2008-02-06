/*
 *   Copyright (C) 2007  The Concord Consortium, Inc.,
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

import java.awt.EventQueue;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Matcher;

import javax.swing.JOptionPane;
import javax.swing.JScrollPane;

import org.concord.modeler.ComponentScripter;
import org.concord.modeler.ConnectionManager;
import org.concord.modeler.Navigator;
import org.concord.modeler.script.Compiler;
import org.concord.modeler.ui.HTMLPane;
import org.concord.modeler.util.FileUtilities;

/**
 * @author Charles Xie
 * 
 */
class PageScripter extends ComponentScripter {

	private Page page;

	PageScripter(Page page) {
		super(true);
		this.page = page;
		setName("Page script runner");
	}

	protected void evalCommand(String ci) {

		evaluateSingleKeyword(ci);

		// load
		Matcher matcher = Compiler.LOAD.matcher(ci);
		if (matcher.find()) {
			String address = ci.substring(matcher.end()).trim();
			if (FileUtilities.isRelative(address))
				address = FileUtilities.getCodeBase(page.getAddress()) + address;
			page.getNavigator().visitLocation(address);
			return;
		}

		// show message
		matcher = Compiler.MESSAGE.matcher(ci);
		if (matcher.find()) {
			String s = XMLCharacterDecoder.decode(ci.substring(matcher.end()).trim());
			String slc = s.toLowerCase();
			int a = slc.indexOf("<t>");
			int b = slc.indexOf("</t>");
			String info;
			if (a != -1 && b != -1) {
				info = s.substring(a, b + 4).trim();
				slc = info.toLowerCase();
				if (!slc.startsWith("<html>")) {
					info = "<html>" + info;
				}
				if (!slc.endsWith("</html>")) {
					info = info + "</html>";
				}
			}
			else {
				matcher = Compiler.HTML_EXTENSION.matcher(s);
				if (matcher.find()) {
					info = readText(s);
				}
				else {
					info = "Unknown text";
				}
			}
			if (EventQueue.isDispatchThread()) {
				showMessageDialog(info);
			}
			else {
				final String info2 = info;
				EventQueue.invokeLater(new Runnable() {
					public void run() {
						showMessageDialog(info2);
					}
				});
			}
			return;
		}

	}

	private void showMessageDialog(String message) {
		HTMLPane h = new HTMLPane("text/html", message);
		h.setEditable(false);
		try {
			h.setBase(page.getURL());
		}
		catch (MalformedURLException e) {
			e.printStackTrace();
		}
		JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(page), new JScrollPane(h));
	}

	private void evaluateSingleKeyword(String s) {

		// back
		if ("back".equalsIgnoreCase(s)) {
			if (EventQueue.isDispatchThread()) {
				page.getNavigator().getAction(Navigator.BACK).actionPerformed(null);
			}
			else {
				EventQueue.invokeLater(new Runnable() {
					public void run() {
						page.getNavigator().getAction(Navigator.BACK).actionPerformed(null);
					}
				});
			}
			return;
		}

		// forward
		if ("forward".equalsIgnoreCase(s)) {
			if (EventQueue.isDispatchThread()) {
				page.getNavigator().getAction(Navigator.FORWARD).actionPerformed(null);
			}
			else {
				EventQueue.invokeLater(new Runnable() {
					public void run() {
						page.getNavigator().getAction(Navigator.FORWARD).actionPerformed(null);
					}
				});
			}
			return;
		}

		// home
		if ("home".equalsIgnoreCase(s)) {
			if (EventQueue.isDispatchThread()) {
				page.getNavigator().getAction(Navigator.HOME).actionPerformed(null);
			}
			else {
				EventQueue.invokeLater(new Runnable() {
					public void run() {
						page.getNavigator().getAction(Navigator.HOME).actionPerformed(null);
					}
				});
			}
			return;
		}

		// reload the current page
		if ("reload".equalsIgnoreCase(s) || "refresh".equalsIgnoreCase(s)) {
			if (EventQueue.isDispatchThread()) {
				page.visit(page.getAddress());
			}
			else {
				EventQueue.invokeLater(new Runnable() {
					public void run() {
						page.visit(page.getAddress());
					}
				});
			}
			return;
		}

	}

	private String readText(String address) {
		if (address == null || address.equals("")) {
			return null;
		}
		if (FileUtilities.isRelative(address)) {
			address = page.getPathBase() + address;
		}
		InputStream is = null;
		if (FileUtilities.isRemote(address)) {
			URL url = null;
			try {
				url = new URL(address);
			}
			catch (MalformedURLException e) {
				e.printStackTrace();
			}
			if (url != null) {
				File file = null;
				if (ConnectionManager.sharedInstance().isCachingAllowed()) {
					try {
						file = ConnectionManager.sharedInstance().shouldUpdate(url);
						if (file == null)
							file = ConnectionManager.sharedInstance().cache(url);
					}
					catch (IOException e) {
						e.printStackTrace();
					}
				}
				if (file == null) {
					try {
						is = url.openStream();
					}
					catch (IOException ioe) {
						ioe.printStackTrace();
					}
				}
				else {
					try {
						is = new FileInputStream(file);
					}
					catch (IOException ioe) {
						ioe.printStackTrace();
					}
				}
			}
		}
		else {
			try {
				is = new FileInputStream(new File(address));
			}
			catch (IOException ioe) {
				ioe.printStackTrace();
			}
		}
		if (is == null) {
			final String errorAddress = address;
			EventQueue.invokeLater(new Runnable() {
				public void run() {
					JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(page), "File " + errorAddress
							+ " was not found.", "File not found", JOptionPane.ERROR_MESSAGE);
				}
			});
			return null;
		}
		StringBuffer buffer = new StringBuffer();
		byte[] b = new byte[1024];
		int n = -1;
		try {
			while ((n = is.read(b)) != -1) {
				buffer.append(new String(b, 0, n));
			}
		}
		catch (IOException ioe) {
			ioe.printStackTrace();
		}
		finally {
			try {
				is.close();
			}
			catch (IOException e) {
			}
		}
		return buffer.toString();
	}

}
