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

import java.awt.EventQueue;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.concord.modeler.event.ProgressEvent;
import org.concord.modeler.event.ProgressListener;

public class MultipageZipper {

	private static MultipageZipper sharedInstance = new MultipageZipper();
	private static String[] exclusion = { ".tmp" };
	private List<File> keep, linkFiles, models, resources;
	private List<ProgressListener> progressListenerList;

	private MultipageZipper() {
		progressListenerList = new CopyOnWriteArrayList<ProgressListener>();
	}

	public static MultipageZipper sharedInstance() {
		return sharedInstance;
	}

	public void addProgressListener(ProgressListener pl) {
		progressListenerList.add(pl);
	}

	public void removeProgressListener(ProgressListener pl) {
		progressListenerList.remove(pl);
	}

	protected void notifyProgressListeners(final ProgressEvent e) {
		if (progressListenerList.isEmpty())
			return;
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				for (ProgressListener l : progressListenerList) {
					l.progressReported(e);
				}
			}
		});
	}

	private void exclude(String entry, File folder) {
		if (keep == null) {
			keep = new ArrayList<File>();
		}
		else {
			keep.clear();
		}
		notifyProgressListeners(new ProgressEvent(this, 0, 0, 100, "Excluding files......"));
		File[] f = folder.listFiles();
		boolean excluded;
		for (File i : f) {
			excluded = false;
			if (i.isDirectory()) {
				excluded = true;
			}
			else {
				for (String m : exclusion) {
					if (i.getName().toLowerCase().endsWith(m)) {
						excluded = true;
						break;
					}
				}
			}
			if (!excluded) {
				keep.add(i);
			}
		}
		excludeNotLinked(entry);
	}

	private void excludeNotLinked(String entry) {

		// sort the files into three categories

		if (linkFiles == null) {
			linkFiles = new ArrayList<File>();
		}
		else {
			linkFiles.clear();
		}

		if (models == null) {
			models = new ArrayList<File>();
		}
		else {
			models.clear();
		}

		if (resources == null) {
			resources = new ArrayList<File>();
		}
		else {
			resources.clear();
		}

		notifyProgressListeners(new ProgressEvent(this, 0, 0, 100, "Adding files......"));
		for (File file : keep) {
			String s = file.getName().toLowerCase();
			if (s.endsWith(".cml") || s.endsWith(".mml") || s.endsWith(".gbl") || s.endsWith(".html")
					|| s.endsWith(".htm") || s.endsWith(".mws") || s.endsWith(".spt")) {
				linkFiles.add(file);
			}
			else if (s.endsWith(".mml") || s.endsWith(".gbl") || s.endsWith(".xyz") || s.endsWith(".mdd")
					|| s.endsWith(".aps") || s.endsWith(".jms")) {
				models.add(file);
			}
			else {
				resources.add(file);
			}
		}

		// if no link file is found, clear and return

		if (linkFiles.isEmpty()) {
			keep.clear();
			return;
		}

		// exclude the link files that are not linked directly or indirectly to the entry page

		List<File> isolatedFiles = new ArrayList<File>();
		for (File file : linkFiles) {
			if (entry != null && file.getAbsolutePath().endsWith(entry))
				continue;
			notifyProgressListeners(new ProgressEvent(this, 0, 0, 100, "Checking " + file + "......"));
			if (!isLinked(file, linkFiles)) {
				isolatedFiles.add(file);
			}
		}

		if (!isolatedFiles.isEmpty()) {
			linkFiles.removeAll(isolatedFiles);
			keep.removeAll(isolatedFiles);
		}

		// exclude the model files that are not linked

		if (!models.isEmpty()) {

			for (Iterator it = models.iterator(); it.hasNext();) {
				File file = (File) it.next();
				notifyProgressListeners(new ProgressEvent(this, 0, 0, 100, "Checking " + file + "......"));
				if (!isLinked(file, linkFiles)) {
					it.remove();
					keep.remove(file);
				}
			}

		}

		// exclude the resource files that are not linked

		if (!resources.isEmpty()) {

			for (Iterator it = resources.iterator(); it.hasNext();) {
				File file = (File) it.next();
				notifyProgressListeners(new ProgressEvent(this, 0, 0, 100, "Checking " + file + "......"));
				if (isLinked(file, linkFiles))
					it.remove();
			}

			if (!models.isEmpty()) {
				for (Iterator it = resources.iterator(); it.hasNext();) {
					File file = (File) it.next();
					notifyProgressListeners(new ProgressEvent(this, 0, 0, 100, "Checking " + file + "......"));
					if (isLinked(file, models))
						it.remove();
				}
			}

			if (!resources.isEmpty())
				keep.removeAll(resources);

		}

	}

	private boolean isLinked(File file, List<File> list) {
		String s = file.getName();
		BufferedReader br = null;
		String line;
		for (File f : list) {
			if (!f.equals(file)) {
				try {
					br = new BufferedReader(new FileReader(f));
					line = br.readLine();
					while (line != null) {
						if (line.indexOf(s) != -1)
							return true;
						line = br.readLine();
					}
				}
				catch (IOException ioe) {
					ioe.printStackTrace();
				}
				finally {
					if (br != null) {
						try {
							br.close();
						}
						catch (IOException ioe) {
							ioe.printStackTrace();
						}
					}
				}
			}
		}
		return false;
	}

	public boolean zip(String entry, File folder, ZipOutputStream zos) {

		if (folder == null)
			throw new IllegalArgumentException("folder cannot be null");
		if (!folder.isDirectory())
			throw new IllegalArgumentException("arg must be a directory");
		if (zos == null)
			throw new IllegalArgumentException("outputstream cannot be null");

		exclude(entry, folder);
		if (keep.isEmpty())
			return false;

		int max = keep.size() + 1;

		FileInputStream in = null;
		int c;
		boolean b = true;
		try {
			if (entry != null) {
				zos.putNextEntry(new ZipEntry("entry.txt"));
				zos.write(entry.getBytes());
				zos.flush();
				zos.closeEntry();
				notifyProgressListeners(new ProgressEvent(this, 1, 0, max, "entry.txt"));
			}
			int i = 1;
			for (File file : keep) {
				zos.putNextEntry(new ZipEntry(file.getName()));
				in = new FileInputStream(file);
				while ((c = in.read()) != -1)
					zos.write(c);
				in.close();
				zos.flush();
				zos.closeEntry();
				notifyProgressListeners(new ProgressEvent(this, ++i, 0, max, file.getName()));
			}
			notifyProgressListeners(new ProgressEvent(this, 0, 0, max, "Done."));
		}
		catch (IOException e) {
			e.printStackTrace();
			b = false;
		}
		finally {
			try {
				zos.close();
			}
			catch (IOException e) {
				e.printStackTrace();
			}
			if (in != null) {
				try {
					in.close();
				}
				catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		return b;

	}

}