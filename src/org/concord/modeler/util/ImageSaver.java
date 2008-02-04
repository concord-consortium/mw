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

import java.awt.Component;
import java.awt.EventQueue;
import java.io.File;

import javax.swing.JOptionPane;

import org.concord.modeler.ModelerUtilities;
import org.concord.modeler.util.FileUtilities;

/**
 * @author Charles Xie
 * 
 */
public abstract class ImageSaver {

	protected static void saveImage(final Component c, final String name, final File parent) {
		final int i = ModelerUtilities.copyResourceToDirectory(name, parent);
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				switch (i) {
				case FileUtilities.SOURCE_NOT_FOUND:
					JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(c), "Source " + name
							+ " is not found.", "File not found", JOptionPane.ERROR_MESSAGE);
					break;
				case FileUtilities.FILE_ACCESS_ERROR:
					JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(c), "Directory " + parent
							+ " inaccessible.", "File access error", JOptionPane.ERROR_MESSAGE);
					break;
				case FileUtilities.WRITING_ERROR:
					JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(c),
							"Encountered error while writing to directory " + parent, "Writing error",
							JOptionPane.ERROR_MESSAGE);
					break;
				}
			}
		});
	}

	protected static void copyFile(final Component c, final String s, final File d) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				switch (FileUtilities.copy(new File(s), d)) {
				case FileUtilities.SOURCE_NOT_FOUND:
					JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(c),
							"Source " + s + " is not found.", "File not found", JOptionPane.ERROR_MESSAGE);
					break;
				case FileUtilities.FILE_ACCESS_ERROR:
					JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(c), "Destination " + d
							+ " cannot be created.", "File access error", JOptionPane.ERROR_MESSAGE);
					break;
				case FileUtilities.WRITING_ERROR:
					JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(c),
							"Encountered error while writing to " + d, "Writing error", JOptionPane.ERROR_MESSAGE);
					break;
				}
			}
		});
	}

}