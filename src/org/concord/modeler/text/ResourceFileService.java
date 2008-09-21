/*
 *   Copyright (C) 2008  The Concord Consortium, Inc.,
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
import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.net.URL;

import javax.swing.JOptionPane;

import org.concord.modeler.ConnectionManager;
import org.concord.modeler.util.FileUtilities;

/**
 * @author Charles Xie
 * 
 */
class ResourceFileService {

	static void saveAdditionalResourceFiles(File parent, Page page) {
		String referencedFiles = page.getAdditionalResourceFiles();
		if (referencedFiles != null) {
			String[] t = referencedFiles.split(",");
			for (String s : t) {
				s = s.trim();
				if (!s.equals(""))
					copyResource(page.getPathBase() + s, parent, page);
			}
		}
	}

	private static void copyResource(String s, File parent, Page page) {
		File file;
		if (page.isRemote()) {
			file = ConnectionManager.sharedInstance().getLocalCopy(s);
			if (!file.exists()) {
				URL u = null;
				try {
					u = new URL(s);
				}
				catch (MalformedURLException e) {
					e.printStackTrace();
				}
				if (u != null) {
					cacheResource(u, page);
				}
			}
		}
		else {
			file = new File(s);
		}
		if (file.exists()) {
			FileUtilities.copy(file, new File(parent, FileUtilities.getFileName(s)));
		}
	}

	private static void cacheResource(final URL u, final Page page) {
		try {
			File file = ConnectionManager.sharedInstance().shouldUpdate(u);
			if (file == null)
				file = ConnectionManager.sharedInstance().cache(u);
			// ConnectionManager.sharedInstance().setCheckUpdate(true);
		}
		catch (FileNotFoundException e) {
			e.printStackTrace();
			EventQueue.invokeLater(new Runnable() {
				public void run() {
					JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(page), u + " was not found.",
							"File not found", JOptionPane.ERROR_MESSAGE);
				}
			});
		}
	}

}
