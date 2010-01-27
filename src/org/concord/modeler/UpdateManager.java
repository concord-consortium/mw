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

import java.awt.Component;
import java.awt.Dimension;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLConnection;

import javax.swing.ImageIcon;
import javax.swing.JOptionPane;

import org.concord.modeler.text.ExternalClient;
import org.concord.modeler.ui.ProcessMonitor;

class UpdateManager {

	private final static String JAR_NAME = "mw.jar";
	private final static String PACK_NAME = JAR_NAME + ".pack.gz";
	private static File packFile;
	private static String jarLocation;

	private static String getPackedJarPath() {
		return Modeler.getStaticRoot() + "lib/" + PACK_NAME;
	}

	static File getPackFile() {
		return packFile;
	}

	static void showUpdateReminder(final Modeler modeler) {

		if (!shouldUpdateJar())
			return;

		ImageIcon icon = null;
		try {
			icon = new ImageIcon(new URL(Modeler.getStaticRoot() + "mwlogo.gif"));
		}
		catch (MalformedURLException e) {
			e.printStackTrace();
		}

		Download download = new Download();
		download.addDownloadListener(modeler);

		String ls = System.getProperty("line.separator");
		String s = Modeler.getInternationalText("UpdateNoticeContent");
		String msg = s != null ? s : "An update is available. " + Modeler.NAME
				+ " will restart shortly after downloading." + ls
				+ "If it does not, or you keep seeing this message, please go to" + ls + Modeler.getContextRoot()
				+ " to download the new version.";

		s = Modeler.getInternationalText("UpdateNotice");
		JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(modeler), msg, s != null ? s : "Update Notice",
				JOptionPane.INFORMATION_MESSAGE, icon);
		if (!downloadUpdates(modeler, download))
			ExternalClient.open(ExternalClient.HTML_CLIENT, Modeler.getContextRoot());

	}

	private static boolean shouldUpdateJar() {
		String jarLocation = System.getProperty("java.class.path");
		if (Modeler.isMac())
			jarLocation = ModelerUtilities.validateJarLocationOnMacOSX(jarLocation);
		long latestVersionTimeStamp = checkTimeStamp(getPackedJarPath());
		if (latestVersionTimeStamp < 0L)
			return false;
		File currentJarFile = new File(jarLocation);
		long currentVersionTimeStamp = currentJarFile.lastModified();
		if (currentVersionTimeStamp >= latestVersionTimeStamp)
			return false;
		return true;
	}

	private static long checkTimeStamp(String s) {
		URLConnection conn = ConnectionManager.getConnection(s);
		if (!(conn instanceof HttpURLConnection))
			return -1;
		try {
			((HttpURLConnection) conn).setRequestMethod("HEAD");
		}
		catch (ProtocolException e) {
			e.printStackTrace();
			return -1;
		}
		long t = conn.getLastModified();
		((HttpURLConnection) conn).disconnect();
		return t;
	}

	private static boolean downloadUpdates(Component parent, Download download) {
		if (download.getProcessMonitor() == null) {
			ProcessMonitor m = new ProcessMonitor(JOptionPane.getFrameForComponent(parent));
			m.getProgressBar().setMinimum(0);
			m.getProgressBar().setMaximum(100);
			m.getProgressBar().setPreferredSize(new Dimension(300, 20));
			download.setProcessMonitor(m);
		}
		download.getProcessMonitor().setTitle(" Downloading MW updates ...");
		download.getProcessMonitor().setLocationRelativeTo(parent);
		URL url = null;
		try {
			url = new URL(getPackedJarPath());
		}
		catch (MalformedURLException me) {
			me.printStackTrace();
			return false;
		}
		jarLocation = System.getProperty("java.class.path");
		if (Modeler.isMac())
			jarLocation = ModelerUtilities.validateJarLocationOnMacOSX(jarLocation);
		File jarFile = new File(jarLocation);
		packFile = new File(jarFile.getParentFile(), PACK_NAME);
		download.getFileInfo(url);
		download.downloadInAThread(url, packFile);
		return true;
	}

	// unpack the gz file
	static boolean unpack() {
		try {
			String command = "unpack200 " + PACK_NAME + " " + JAR_NAME;
			Process p = Runtime.getRuntime().exec(command, null, packFile.getParentFile());
			// flash out stderr in case of any: if we do not do this the process may hang if the buffer of
			// stderr fills up
			InputStreamReader isr = new InputStreamReader(p.getErrorStream());
			BufferedReader br = new BufferedReader(isr);
			String line = null;
			while ((line = br.readLine()) != null)
				System.out.println(line);
			br.close();
			isr.close();
			if (p.waitFor() != 0) {
				JOptionPane.showMessageDialog(null, "There is a problem in unpacking " + PACK_NAME + ".",
						"Unpack200 error", JOptionPane.ERROR_MESSAGE);
			}
			else {
				File jarFile = new File(jarLocation);
				jarFile.setLastModified(packFile.lastModified());
				// FIXME: somehow this doesn't change the last modified time. Probably caused by the fact
				// that the jar is still being used? The consequence is that the user's jar file will have
				// the last modified time different than that of the actual file on our server. Since the LMT
				// is created by the user's computer, there is a danger that the user will actually have
				// a LMT up to 12 hours later than the one of the file on the server. If we update the jar
				// file again within 12 hours, then those who downloaded the jar within these 12 hours will
				// have a risk of not being able to get the updated jar.
			}
			return true;
		}
		catch (Throwable t) {
			t.printStackTrace();
			return false;
		}
	}

}