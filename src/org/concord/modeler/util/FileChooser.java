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

import java.awt.EventQueue;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

import org.concord.modeler.FileFilterFactory;

/**
 * This is a file chooser that is able to remember its last-visited path and files (4).
 * 
 * @author Charles Xie
 */

public class FileChooser extends JFileChooser {

	private final static String LATEST_PATH = "Latest Path";
	private final static String[] RECENT_FILES = new String[] { "Recent File 1", "Recent File 2", "Recent File 3",
			"Recent File 4" };
	private final static File DUMMY_FILE = new File("");

	private Map<String, String> historyMap;
	private List<String> recentFiles;
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

	/* Why did we reset the text field to "Untitled.", appended by the current extension before? */
	public void resetTextField() {
		/*
		 * File dir = getCurrentDirectory(); if (dir != null) { String fn = "Untitled"; if (getFileFilter() != null) fn
		 * += "." + getFileFilter().getDescription().toLowerCase(); setSelectedFile(new File(dir, fn)); } else {
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
		historyMap.put(LATEST_PATH, path);
	}

	public String getLastVisitedPath() {
		if (historyMap == null)
			return lastVisitedPath;
		return historyMap.get(LATEST_PATH);
	}

	public void rememberFile(String fileName, final JMenu recentFilesMenu) {
		if (historyMap == null)
			return;
		if (fileName == null)
			return;
		int max = RECENT_FILES.length;
		if (recentFiles.contains(fileName)) {
			recentFiles.remove(fileName);
		}
		else {
			if (recentFiles.size() >= max)
				recentFiles.remove(0);
		}
		recentFiles.add(fileName);
		final int n = recentFiles.size();
		for (int i = 0; i < max; i++) {
			if (i < n)
				historyMap.put(RECENT_FILES[max - 1 - i], recentFiles.get(i));
		}
		if (recentFilesMenu != null)
			EventQueue.invokeLater(new Runnable() {
				public void run() {
					JMenuItem mi;
					for (int i = 0; i < n; i++) {
						mi = recentFilesMenu.getItem(i);
						mi.setText(recentFiles.get(n - 1 - i));
					}
				}
			});
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
		historyMap = new HashMap<String, String>();
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

	public void readHistory(File file) {
		XMLDecoder in = null;
		try {
			in = new XMLDecoder(new BufferedInputStream(new FileInputStream(file)));
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		if (in == null)
			return;
		try {
			historyMap = (HashMap) in.readObject();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			in.close();
		}
		if (historyMap == null)
			return;
		for (int i = 3; i >= 0; i--) {
			String s = historyMap.get(RECENT_FILES[i]);
			if (s != null)
				recentFiles.add(s);
		}
	}

	public void writeHistory(File file) {
		XMLEncoder out = null;
		try {
			out = new XMLEncoder(new BufferedOutputStream(new FileOutputStream(file)));
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		if (out == null)
			return;
		try {
			out.writeObject(historyMap);
		}
		finally {
			out.close();
		}
	}

}