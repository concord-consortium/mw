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
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import org.concord.modeler.util.FileChooser;
import org.concord.modeler.util.FileUtilities;

public class JNLPSaver {

	private final static String JAR_LOCATION = "lib/workbench.jar";
	private final static String LINE_SEPARATOR = System.getProperty("line.separator");
	private static FileChooser fileChooser;
	private static JnlpSettingDialog settingDialog;

	public static void setFileChooser(FileChooser fc) {
		fileChooser = fc;
	}

	public static void save(Component parent, String url, String description, boolean customize) {

		if (!FileUtilities.isRemote(url)) {
			JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(parent),
					"Creating a JNLP file for a local file is not permitted.", "Local file",
					JOptionPane.INFORMATION_MESSAGE);
			return;
		}

		if (customize) {
			settingDialog = new JnlpSettingDialog(JOptionPane.getFrameForComponent(parent));
			settingDialog.setLocationRelativeTo(parent);
			settingDialog.setVisible(true);
		}
		else {
			if (settingDialog != null)
				settingDialog.useSettings = false;
		}

		if (fileChooser == null)
			throw new RuntimeException("no file chooser");

		fileChooser.setAcceptAllFileFilterUsed(false);
		fileChooser.addChoosableFileFilter(FileFilterFactory.getFilter("jnlp"));

		fileChooser.setDialogType(JFileChooser.SAVE_DIALOG);
		String s = Modeler.getInternationalText("CreateJNLP");
		fileChooser.setDialogTitle(s != null ? s : "Create JNLP launcher");
		String latestPath = fileChooser.getLastVisitedPath();
		if (latestPath != null)
			fileChooser.setCurrentDirectory(new File(latestPath));
		String fn = FileUtilities.getFileName(url);
		if (fn.indexOf("?") != -1 && fn.indexOf("=") != -1) {
			Map map = FileUtilities.getQueryParameterMap(fn);
			String filename = (String) map.get("filename");
			String timestamp = (String) map.get("timestamp");
			if (filename == null && timestamp == null) {
				fn = "filename.zip";
			}
			else {
				fn = null;
				String ext = FileUtilities.getSuffix(filename);
				if (filename != null)
					fn = FileUtilities.removeSuffix(filename);
				if (timestamp != null)
					fn += timestamp;
				if (ext != null)
					fn += "." + ext;
			}
		}
		fileChooser.recallLastFile(new File(latestPath, FileUtilities.changeExtension(fn, "jnlp")));

		int returnValue = fileChooser.showSaveDialog(parent);
		if (returnValue == JFileChooser.APPROVE_OPTION) {
			File file = fileChooser.getSelectedFile();
			String filename = FileUtilities.fileNameAutoExtend(fileChooser.getFileFilter(), file);
			File temp = new File(filename);
			if (temp.exists()) {
				if (JOptionPane.showConfirmDialog(JOptionPane.getFrameForComponent(parent), "File " + temp.getName()
						+ " exists, overwrite?", "File exists", JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION) {
					fileChooser.resetChoosableFileFilters();
					return;
				}
			}
			write(url, description, temp);
			fileChooser.rememberPath(file.getParent());
		}
		fileChooser.resetChoosableFileFilters();
		settingDialog = null;
	}

	/** Creates a JNLP file pointing to the URL specified by the string argument. */
	public static void write(String url, String description, File file) {

		FileOutputStream out = null;
		try {
			out = new FileOutputStream(file);
		}
		catch (IOException e) {
			e.printStackTrace();
			return;
		}

		StringBuffer sb = new StringBuffer(1000);

		sb.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
		sb.append(LINE_SEPARATOR);

		sb.append("<jnlp spec=\"1.0+\" codebase=\"" + Modeler.getStaticRoot() + "\">");
		sb.append(LINE_SEPARATOR);

		sb.append("  <information>");
		sb.append(LINE_SEPARATOR);

		sb.append("    <title>" + Modeler.NAME + "</title>");
		sb.append(LINE_SEPARATOR);

		sb.append("    <vendor>Concord Consortium, Inc.</vendor>");
		sb.append(LINE_SEPARATOR);

		sb.append("    <homepage href=\"index.html\"/>");
		sb.append(LINE_SEPARATOR);

		if (description != null) {
			sb.append("    <description>" + description + "</description>");
		}
		else {
			sb.append("    <description>An Interface to the Molecular World</description>");
		}
		sb.append(LINE_SEPARATOR);

		sb.append("    <icon href=\"mwlogo.gif\"/>");
		sb.append(LINE_SEPARATOR);

		sb.append("    <offline-allowed/>");
		sb.append(LINE_SEPARATOR);

		// sb.append(" <association mime-type=\"application-x/mw\" extensions=\"cml\"/>");
		// sb.append(LINE_SEPARATOR);

		sb.append("  </information>");
		sb.append(LINE_SEPARATOR);

		sb.append("  <resources>");
		sb.append(LINE_SEPARATOR);

		sb.append("    <j2se version=\"" + Modeler.MINIMUM_JAVA_VERSION + "+\"/>");
		sb.append(LINE_SEPARATOR);

		sb.append("    <jar href=\"" + JAR_LOCATION + "\"/>");
		sb.append(LINE_SEPARATOR);

		// Mac OS X system properties

		sb.append("    <property name=\"apple.awt.brushMetalLook\" value=\"true\"/>");
		sb.append(LINE_SEPARATOR);

		sb.append("    <property name=\"apple.laf.useScreenMenuBar\" value=\"true\"/>");
		sb.append(LINE_SEPARATOR);

		// MW system properties
		if (settingDialog != null && settingDialog.useSettings) {

			if (!settingDialog.hasWindowMenuBar()) {
				sb.append("    <property name=\"mw.window.menubar\" value=\"false\"/>");
				sb.append(LINE_SEPARATOR);
			}

			if (!settingDialog.hasWindowToolBar()) {
				sb.append("    <property name=\"mw.window.toolbar\" value=\"false\"/>");
				sb.append(LINE_SEPARATOR);
			}

			if (!settingDialog.hasWindowStatusBar()) {
				sb.append("    <property name=\"mw.window.statusbar\" value=\"false\"/>");
				sb.append(LINE_SEPARATOR);
			}

			if (!settingDialog.isWindowResizable()) {
				sb.append("    <property name=\"mw.window.resizable\" value=\"false\"/>");
				sb.append(LINE_SEPARATOR);
			}

			if (settingDialog.isWindowFullScreen()) {
				sb.append("    <property name=\"mw.window.fullscreen\" value=\"true\"/>");
				sb.append(LINE_SEPARATOR);
			}

			String s = settingDialog.getMWLocale();
			if (!s.equals("en-US")) {
				sb.append("    <property name=\"mw.locale\" value=\"" + s + "\"/>");
				sb.append(LINE_SEPARATOR);
			}

			sb.append("    <property name=\"mw.window.top\" value=\"" + settingDialog.getWindowTop() + "\"/>");
			sb.append(LINE_SEPARATOR);

			sb.append("    <property name=\"mw.window.left\" value=\"" + settingDialog.getWindowLeft() + "\"/>");
			sb.append(LINE_SEPARATOR);

			sb.append("    <property name=\"mw.window.width\" value=\"" + settingDialog.getWindowWidth() + "\"/>");
			sb.append(LINE_SEPARATOR);

			sb.append("    <property name=\"mw.window.height\" value=\"" + settingDialog.getWindowHeight() + "\"/>");
			sb.append(LINE_SEPARATOR);

		}

		sb.append("  </resources>");
		sb.append(LINE_SEPARATOR);

		sb.append(" <security>");
		sb.append(LINE_SEPARATOR);

		sb.append("    <all-permissions/>");
		sb.append(LINE_SEPARATOR);

		sb.append("  </security>");
		sb.append(LINE_SEPARATOR);

		sb.append("  <application-desc main-class=\"" + ModelerLauncher.class.getName() + "\">");
		sb.append(LINE_SEPARATOR);

		sb.append("    <argument>remote</argument>");
		sb.append(LINE_SEPARATOR);

		sb.append("    <argument>" + url + "</argument>");
		sb.append(LINE_SEPARATOR);

		sb.append("  </application-desc>");
		sb.append(LINE_SEPARATOR);

		sb.append("</jnlp>");

		int clength = sb.length();
		byte[] c = new byte[clength];
		c = sb.toString().getBytes();

		try {
			out.write(c, 0, clength);
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