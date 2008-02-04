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

import java.awt.EventQueue;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.swing.JProgressBar;
import javax.swing.text.BadLocationException;

final class PlainTextWriter {

	private Page page;
	private JProgressBar progressBar;

	public PlainTextWriter(Page page) {
		if (page == null)
			throw new IllegalArgumentException("null input");
		this.page = page;
	}

	void destroy() {
		page = null;
		progressBar = null;
	}

	public void setProgressBar(JProgressBar pb) {
		progressBar = pb;
	}

	public JProgressBar getProgressBar() {
		return progressBar;
	}

	public synchronized boolean write(final File file) {

		if (file == null)
			throw new IllegalArgumentException("null input file");

		FileOutputStream out = null;
		try {
			out = new FileOutputStream(file);
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		if (out == null)
			return false;

		if (progressBar != null) {
			EventQueue.invokeLater(new Runnable() {
				public void run() {
					progressBar.setString("Writing......");
				}
			});
		}

		String text = null;
		try {
			text = page.getDocument().getText(0, page.getDocument().getLength());
		}
		catch (BadLocationException e) {
			e.printStackTrace();
			text = "Error in getting text from the document";
		}

		int clength = text.length();
		byte[] c = new byte[clength];
		c = text.getBytes();

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
				e.printStackTrace();
			}
		}

		if (progressBar != null)
			EventQueue.invokeLater(new Runnable() {
				public void run() {
					progressBar.setString("Done");
				}
			});

		return true;

	}

}