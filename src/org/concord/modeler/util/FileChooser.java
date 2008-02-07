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

package org.concord.modeler.util;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.filechooser.FileFilter;

import org.concord.modeler.FileFilterFactory;

/**
 * This is a file chooser that is able to remember its last-visited path and files (4), provided that a
 * <code>Preferences</code> be set to store these information.
 * 
 * @author Charles Xie
 */

public class FileChooser extends JFileChooser {

	private final static int NUM_FILES = 4;
	private final static String[] RECENT_FILES = new String[] { "Recent File 1", "Recent File 2", "Recent File 3",
			"Recent File 4" };
	private final static File DUMMY_FILE = new File("");

	private List<String> recentFiles;
	private Preferences preference;
	private String lastVisitedPath;

	public FileChooser() {
		super();
		init();
	}

	public FileChooser(File currentDirectory) {
		super(currentDirectory);
		init();
	}

	public FileChooser(String currentDirectoryPath) {
		super(currentDirectoryPath);
		init();
	}

	public void addChoosableFileFilters(String[] ext) {
		for (String s : ext)
			addChoosableFileFilter(FileFilterFactory.getFilter(s));
	}

	public void setPreferences(Preferences p) {
		preference = p;
		if (preference == null)
			return;
		for (int i = 3; i >= 0; i--) {
			String s = preference.get(RECENT_FILES[i], null);
			if (s != null)
				recentFiles.add(s);
		}
	}

	/** remember the last opened file */
	public void recallLastFile(File file) {
		if (file != null) {
			FileFilter ff = getFileFilter();
			if (ff != null && ff.accept(file)) {
				setSelectedFile(file);
			}
			else {
				resetTextField();
			}
		}
		else {
			resetTextField();
		}
	}

	/* Why did we reset the text field to "Untitled.", appended by the current extension before? */
	public void resetTextField() {
		/*
		 * File dir = getCurrentDirectory(); if (dir != null) { String fn = "Untitled"; if (getFileFilter() != null) fn +=
		 * "." + getFileFilter().getDescription().toLowerCase(); setSelectedFile(new File(dir, fn)); } else {
		 * clearTextField(); }
		 */
		clearTextField();
	}

	public void clearTextField() {
		setSelectedFile(DUMMY_FILE);
	}

	/** save the last visited path to the hard drive */
	public void rememberPath(String path) {
		lastVisitedPath = path;
		if (preference == null)
			return;
		preference.put("Latest Path", path);
	}

	public String getLastVisitedPath() {
		if (preference == null) {
			return lastVisitedPath;
		}
		return preference.get("Latest Path", null);
	}

	public void rememberFile(String fileName, JMenu recentFilesMenu) {
		if (preference == null)
			return;
		if (fileName == null)
			return;
		if (recentFiles.contains(fileName)) {
			recentFiles.remove(fileName);
		}
		else {
			if (recentFiles.size() >= NUM_FILES)
				recentFiles.remove(0);
		}
		recentFiles.add(fileName);
		int n = recentFiles.size();
		for (int i = 0; i < 4; i++) {
			if (i < recentFiles.size())
				preference.put(RECENT_FILES[3 - i], recentFiles.get(i));
		}
		if (recentFilesMenu == null)
			return;
		JMenuItem mi;
		for (int i = 0; i < n; i++) {
			mi = recentFilesMenu.getItem(i);
			mi.setText(recentFiles.get(n - 1 - i));
		}
	}

	public String[] getRecentFiles() {
		int n = recentFiles.size();
		if (n == 0)
			return new String[] {};
		String[] s = new String[n];
		for (int i = 0; i < n; i++) {
			s[n - 1 - i] = recentFiles.get(i);
		}
		return s;
	}

	private void init() {
		setMultiSelectionEnabled(false);
		setFileHidingEnabled(true);
		setFileView(new CustomizedFileView());
		recentFiles = new ArrayList<String>();
		addPropertyChangeListener(new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent e) {
				String s = e.getPropertyName();
				if (JFileChooser.DIRECTORY_CHANGED_PROPERTY.equals(s)) {
					if (getDialogType() == OPEN_DIALOG) {
						clearTextField();
					}
				}
			}
		});
	}

}