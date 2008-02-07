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

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.zip.ZipInputStream;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import org.concord.modeler.ConnectionManager;
import org.concord.modeler.Modeler;
import org.concord.modeler.ModelerUtilities;
import org.concord.modeler.Zipper;
import org.concord.modeler.ui.ProcessMonitor;
import org.concord.modeler.util.FileUtilities;
import org.concord.modeler.util.SwingWorker;

/**
 * @author Charles Xie
 * 
 */
class ZipDownloader {

	private static JFileChooser folderChooser;
	private String src;
	private File des;
	private Page page;
	private List<String> dirList;
	private boolean showReminder = true;

	ZipDownloader(Page page, String src) {
		this.page = page;
		this.src = FileUtilities.httpEncode(src);
	}

	ZipDownloader(Page page, String src, File des, boolean reminder) {
		this(page, src);
		this.des = des;
		showReminder = reminder;
	}

	void download() {
		if (des == null) {
			EventQueue.invokeLater(new Runnable() {
				public void run() {
					String s1 = Modeler.getInternationalText("NoDestinationSelected");
					String s2 = Modeler.getInternationalText("DownloadError");
					JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(page), s1 != null ? s1
							: "No destination directory is selected.", s2 != null ? s2 : "Download Aborted",
							JOptionPane.ERROR_MESSAGE);
				}
			});
			return;
		}
		if (src == null || !FileUtilities.isRemote(src)) {
			EventQueue.invokeLater(new Runnable() {
				public void run() {
					String s1 = Modeler.getInternationalText("DownloadAddressIsLocal");
					String s2 = Modeler.getInternationalText("DownloadError");
					JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(page), (s1 != null ? s1
							: "The download address is local:")
							+ "\n" + src, s2 != null ? s2 : "Download Error", JOptionPane.ERROR_MESSAGE);
				}
			});
			return;
		}
		new SwingWorker("Downloading thread") {
			public Object construct() {
				return unzip() ? Boolean.TRUE : Boolean.FALSE;
			}

			public void finished() {
				if (get() == Boolean.TRUE) {
					open();
				}
				else {
					JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(page), "Error in connecting to "
							+ src);
				}
			}
		}.start();
	}

	private boolean unzip() {
		URL url = null;
		try {
			url = new URL(src);
		}
		catch (MalformedURLException e) {
			e.printStackTrace();
			return false;
		}
		if (EventQueue.isDispatchThread())
			page.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		if (Zipper.sharedInstance().getProcessMonitor() == null) {
			ProcessMonitor m = new ProcessMonitor(JOptionPane.getFrameForComponent(page));
			m.getProgressBar().setMinimum(0);
			m.getProgressBar().setMaximum(100);
			m.getProgressBar().setPreferredSize(new Dimension(300, 20));
			Zipper.sharedInstance().setProcessMonitor(m);
		}
		String s = Modeler.getInternationalText("DownloadingAndUncompressing");
		Zipper.sharedInstance().getProcessMonitor().setTitle(
				"<html><body>" + (s != null ? s : "Downloading and uncompressing......") + "<br>" + src
						+ "</body></html>");
		Zipper.sharedInstance().getProcessMonitor().setLocationRelativeTo(JOptionPane.getFrameForComponent(page));
		URLConnection conn = ConnectionManager.getConnection(url);
		if (conn == null) {
			page.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
			return false;
		}
		InputStream is = null;
		try {
			is = conn.getInputStream();
		}
		catch (IOException e) {
			e.printStackTrace();
			page.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
			return false;
		}
		if (is instanceof ZipInputStream) {
			dirList = Zipper.sharedInstance().unzip((ZipInputStream) is, des, -1L);
		}
		else {
			dirList = Zipper.sharedInstance().unzip(new ZipInputStream(is), des, -1L);
		}
		return true;
	}

	// FIXME: The following code assumes that a packed activity contains only a single directory that contains
	// every CML file, or all the CML files without a directory.
	private void open() {
		File dir;
		if (dirList != null && !dirList.isEmpty()) {
			dir = new File(des, dirList.get(0));
		}
		else {
			dir = des;
		}
		File[] all = dir.listFiles();
		if (all == null || all.length == 0) {
			JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(page), "No entry was found.", "Empty zip",
					JOptionPane.INFORMATION_MESSAGE);
			return;
		}
		File[] cml = dir.listFiles(new java.io.FileFilter() {
			public boolean accept(File pathname) {
				return pathname.toString().toLowerCase().endsWith(".cml");
			}
		});
		boolean cmlFound = cml != null && cml.length > 0;
		if (cmlFound) {
			if (cml != null && cml.length > 1) {
				File entry = new File(dir, "entry.txt");
				String entryPage = null;
				if (entry.exists()) {
					BufferedReader br = null;
					try {
						br = new BufferedReader(new FileReader(entry));
						entryPage = br.readLine();
					}
					catch (IOException ioe) {
						ioe.printStackTrace();
					}
					finally {
						if (br != null) {
							try {
								br.close();
							}
							catch (IOException iox) {
							}
						}
					}
				}
				if (entryPage == null) {
					String s = Modeler.getInternationalText("Download");
					JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(page),
							"More than one CML file was found in the downloaded files,\n"
									+ "but none of them is labeled as the initial page. MW will\n"
									+ "open the first one in the alphabetical order.", s != null ? s : "Download",
							JOptionPane.INFORMATION_MESSAGE);
					page.getNavigator().visitLocation(cml[0].toString());
				}
				else {
					page.getNavigator().visitLocation(FileUtilities.getCodeBase(cml[0].toString()) + entryPage);
				}
			}
			else {
				if (cml != null && cml[0] != null)
					page.getNavigator().visitLocation(cml[0].toString());
			}
		}
		Zipper.sharedInstance().setProcessMonitor(null);
		page.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		if (!showReminder)
			return;
		String s2 = Modeler.getInternationalText("DownloadCompleted");
		if (cmlFound) {
			Page.getFileChooser().rememberPath(dir.toString());
			JOptionPane
					.showMessageDialog(
							JOptionPane.getFrameForComponent(page),
							"<html>"
									+ all.length
									+ "  files have been downloaded to:<br><br><b>"
									+ des
									+ ".</b><br><br><font color=\"#ff0000\">You are now viewing the first page of the downloaded files.</font></html>",
							s2 != null ? s2 : "Download completed", JOptionPane.INFORMATION_MESSAGE);
		}
		else {
			String s1 = Modeler.getInternationalText("FileDownloadedToDirectory");
			JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(page), all.length + " "
					+ (s1 != null ? s1 : " files have been downloaded to:") + "\n" + des + ".", s2 != null ? s2
					: "Download completed", JOptionPane.INFORMATION_MESSAGE);
		}
	}

	boolean openFolderOnDesktopMacOSX() {
		des = new File(System.getProperty("user.home") + "/Desktop", "MW-"
				+ FileUtilities.httpDecode(FileUtilities.removeSuffix(FileUtilities.getFileName(src))));
		if (des.exists()) {
			String s = Modeler.getInternationalText("FolderExists");
			String s1 = Modeler.getInternationalText("Folder");
			String s2 = Modeler.getInternationalText("Overwrite");
			String s3 = Modeler.getInternationalText("Exists");
			if (JOptionPane.showConfirmDialog(JOptionPane.getFrameForComponent(page), (s1 != null ? s1 : "Folder")
					+ " " + des.getName() + " " + (s3 != null ? s3 : "exists") + ", " + (s2 != null ? s2 : "overwrite")
					+ "?", s != null ? s : "Folder exists", JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION) {
				return false;
			}
		}
		else {
			des.mkdir();
		}
		return true;
	}

	boolean chooseFile() {
		folderChooser = ModelerUtilities.folderChooser;
		folderChooser.setDialogType(JFileChooser.SAVE_DIALOG);
		String s = Modeler.getInternationalText("Download");
		folderChooser.setDialogTitle(s != null ? s : "Download");
		folderChooser.setApproveButtonText(s);
		folderChooser.setApproveButtonToolTipText("Download and uncompress to the selected directory");
		File dir = null;
		int i = folderChooser.showDialog(JOptionPane.getFrameForComponent(page), folderChooser.getDialogTitle());
		if (i != JFileChooser.APPROVE_OPTION)
			return false;
		dir = folderChooser.getSelectedFile();
		des = new File(dir, "MW-"
				+ FileUtilities.httpDecode(FileUtilities.removeSuffix(FileUtilities.getFileName(src))));
		if (des.exists()) {
			s = Modeler.getInternationalText("FolderExists");
			String s1 = Modeler.getInternationalText("Folder");
			String s2 = Modeler.getInternationalText("Overwrite");
			String s3 = Modeler.getInternationalText("Exists");
			if (JOptionPane.showConfirmDialog(JOptionPane.getFrameForComponent(page), (s1 != null ? s1 : "Folder")
					+ " " + des.getName() + " " + (s3 != null ? s3 : "exists") + ", " + (s2 != null ? s2 : "overwrite")
					+ "?", s != null ? s : "Folder exists", JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION) {
				return false;
			}
		}
		else {
			des.mkdir();
		}
		folderChooser.setCurrentDirectory(dir);
		return true;
	}

}