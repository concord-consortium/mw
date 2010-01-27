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
import java.awt.EventQueue;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLConnection;
import java.util.zip.ZipOutputStream;

import javax.swing.JOptionPane;

import org.concord.modeler.ConnectionManager;
import org.concord.modeler.MultipageZipper;
import org.concord.modeler.Upload;
import org.concord.modeler.util.FileUtilities;
import org.concord.modeler.util.SwingWorker;

/**
 * @author Charles Xie
 * 
 */
class ZipUploader {

	private Page page;
	private Upload upload;
	private URLConnection connect;

	ZipUploader(Page page, Upload upload) {
		this.page = page;
		this.upload = upload;
	}

	void upload() {
		connect = ConnectionManager.getConnection(upload.getURL());
		if (connect == null)
			return;
		connect.setDoOutput(true);
		connect.setDoInput(true);
		switch (upload.getType()) {
		case Upload.UPLOAD_PAGE:
		case Upload.UPLOAD_REPORT:
			uploadPage();
			break;
		case Upload.UPLOAD_FOLDER:
			uploadFolder();
			break;
		}
	}

	private void uploadPage() {

		final String oldAddress = page.getAddress();
		if (EventQueue.isDispatchThread())
			page.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

		new SwingWorker("Uploading thread") {

			public Object construct() {
				OutputStream os = null;
				try {
					os = connect.getOutputStream();
				}
				catch (IOException e) {
					e.printStackTrace();
					final String s = e.toString();
					EventQueue.invokeLater(new Runnable() {
						public void run() {
							JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(page), s, "Uploading Error",
									JOptionPane.ERROR_MESSAGE);
						}
					});
					return null;
				}
				return page.writePage(new ZipOutputStream(os)) ? Boolean.TRUE : Boolean.FALSE;
			}

			public void finished() {
				if (get() == Boolean.TRUE) {
					finish();
				}
				else {
					abort();
				}
				page.setAddress(oldAddress); // reset address
				page.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
			}

		}.start();

	}

	private void uploadFolder() {

		final File file = new File(FileUtilities.getCodeBase(page.getAddress()));
		if (!file.isDirectory()) {
			EventQueue.invokeLater(new Runnable() {
				public void run() {
					JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(page), file
							+ " is not a valid folder.", "Folder error", JOptionPane.ERROR_MESSAGE);
				}
			});
			return;
		}

		new SwingWorker("ZIP uploader", Thread.NORM_PRIORITY - 1) {

			public Object construct() {
				OutputStream os = null;
				try {
					os = connect.getOutputStream();
				}
				catch (IOException e) {
					e.printStackTrace();
					final String s = e.toString();
					EventQueue.invokeLater(new Runnable() {
						public void run() {
							JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(page), s, "Uploading Error",
									JOptionPane.ERROR_MESSAGE);
						}
					});
					return null;
				}
				MultipageZipper.sharedInstance().addProgressListener(page);
				if (MultipageZipper.sharedInstance().zip(FileUtilities.getFileName(upload.getEntryPage()), file,
						new ZipOutputStream(new BufferedOutputStream(os)))) {
					MultipageZipper.sharedInstance().removeProgressListener(page);
					return Boolean.TRUE;
				}
				MultipageZipper.sharedInstance().removeProgressListener(page);
				return Boolean.FALSE;
			}

			public void finished() {
				if (get() == Boolean.TRUE) {
					finish();
				}
				else {
					abort();
				}
			}

		}.start();

	}

	private void finish() {
		if (connect == null)
			return;
		new PostSubmissionHandler(page).open(connect, upload.getType());
	}

	private void abort() {
		JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(page), "Due to errors, uploading was aborted.",
				"Upload Error", JOptionPane.ERROR_MESSAGE);
	}

}