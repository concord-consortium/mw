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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipOutputStream;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;

import org.concord.modeler.util.FileChooser;
import org.concord.modeler.util.FileUtilities;
import org.concord.modeler.util.SwingWorker;

/* add all files of the current activity folder to a zip file, to be designated by the file chooser. */

class FolderCompressor {

	private Editor editor;

	FolderCompressor(Editor editor) {
		this.editor = editor;
	}

	boolean compressCurrentFolder(final String firstPageName) {

		final String pageAddress = editor.getPage().getAddress();
		if (FileUtilities.isRemote(pageAddress))
			return false;

		final FileChooser fileChooser = ModelerUtilities.fileChooser;
		FileFilter filter = FileFilterFactory.getFilter("zip");
		fileChooser.setAcceptAllFileFilterUsed(false);
		fileChooser.addChoosableFileFilter(filter);
		fileChooser.setDialogType(JFileChooser.SAVE_DIALOG);
		String s = Modeler.getInternationalText("CompressFolder");
		fileChooser.setDialogTitle(s != null ? s : "Compress current folder to file");
		String latestPath = fileChooser.getLastVisitedPath();
		if (latestPath != null)
			fileChooser.setCurrentDirectory(new File(latestPath));
		fileChooser.setAccessory(null);

		try {
			fileChooser.setSelectedFile(new File(fileChooser.getCurrentDirectory(), FileUtilities
					.getParentDirectoryName(pageAddress)
					+ ".zip"));
		}
		catch (NullPointerException npe) {
			npe.printStackTrace();
			fileChooser.resetTextField();
		}

		if (fileChooser.showSaveDialog(editor) == JFileChooser.APPROVE_OPTION) {
			final File file = new File(FileUtilities.fileNameAutoExtend(filter, fileChooser.getSelectedFile()));
			switch (FileUtilities.checkFileName(file)) {
			case FileUtilities.DIRECTORY_ERROR:
				JOptionPane.showMessageDialog(editor, "Directory error: " + file, "Path error",
						JOptionPane.ERROR_MESSAGE);
				fileChooser.resetChoosableFileFilters();
				return false;
			case FileUtilities.ILLEGAL_CHARACTER:
				JOptionPane.showMessageDialog(editor,
						"A file name cannot contain any of the following characters:\n\\/:*?\"<>|", "Path error",
						JOptionPane.ERROR_MESSAGE);
				fileChooser.resetChoosableFileFilters();
				return false;
			}
			if (file.exists()) {
				if (JOptionPane.showConfirmDialog(editor, "Zip file " + file.getName() + " exists, overwrite?\n\n"
						+ "WARNING: The old files contained in the existing zip\n"
						+ "file will be deleted if you choose to overwrite.", "File exists", JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION) {
					fileChooser.resetChoosableFileFilters();
					return false;
				}
			}

			ZipOutputStream zipOut = null;
			try {
				zipOut = new ZipOutputStream(new FileOutputStream(file, false));
			}
			catch (IOException e) {
				e.printStackTrace();
			}

			if (!editor.getPage().isWriting() && zipOut != null) {
				final ZipOutputStream zos = zipOut;
				new SwingWorker("Folder compressor", Thread.NORM_PRIORITY - 1) {
					public Object construct() {
						MultipageZipper.sharedInstance().addProgressListener(editor.getPage());
						MultipageZipper.sharedInstance().zip(firstPageName,
								new File(FileUtilities.getCodeBase(pageAddress)), zos);
						return null;
					}

					public void finished() {
						MultipageZipper.sharedInstance().removeProgressListener(editor.getPage());
					}
				}.start();
			}
			// fileChooser.rememberPath(fileChooser.getCurrentDirectory().toString());

		}

		fileChooser.resetChoosableFileFilters();
		return true;

	}

}