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

package org.concord.modeler;

import java.awt.EventQueue;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JOptionPane;

import org.concord.modeler.ui.ProcessMonitor;
import org.concord.modeler.util.SwingWorker;

class Download {

	private ProcessMonitor monitor;
	private long lastModified;
	private int contentLength;
	private List<DownloadListener> listenerList;
	private int byteCount;
	private volatile boolean pleaseCancel;

	Download() {
	}

	/* if the information has been obtained elsewhere, pass them on */
	void setInfo(long lastModified, int contentLength) {
		this.lastModified = lastModified;
		this.contentLength = contentLength;
	}

	private void destroy() {
		if (listenerList != null)
			listenerList.clear();
		monitor = null;
	}

	public void cancel() {
		pleaseCancel = true;
	}

	public void addDownloadListener(DownloadListener dll) {
		if (dll == null)
			return;
		if (listenerList == null)
			listenerList = new ArrayList<DownloadListener>();
		listenerList.add(dll);
	}

	public void removeDownloadListener(DownloadListener dll) {
		if (dll == null)
			return;
		if (listenerList == null || listenerList.isEmpty())
			return;
		listenerList.remove(dll);
	}

	private void fireDownloadEvent(DownloadEvent e) {
		if (listenerList == null || listenerList.isEmpty())
			return;
		for (DownloadListener dll : listenerList) {
			switch (e.getID()) {
			case DownloadEvent.DOWNLOAD_STARTED:
				dll.downloadStarted(e);
				break;
			case DownloadEvent.DOWNLOAD_COMPLETED:
				dll.downloadCompleted(e);
				break;
			case DownloadEvent.DOWNLOAD_ABORTED:
				dll.downloadAborted(e);
				break;
			}
		}
	}

	public void setProcessMonitor(ProcessMonitor pm) {
		monitor = pm;
	}

	public ProcessMonitor getProcessMonitor() {
		return monitor;
	}

	public void downloadWithoutThread(final URL url, final File des) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				fireDownloadEvent(new DownloadEvent(Download.this, DownloadEvent.DOWNLOAD_STARTED));
			}
		});
		download(url, des);
		final boolean success = des.length() == contentLength;
		des.setLastModified(lastModified);
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				fireDownloadEvent(new DownloadEvent(Download.this, success ? DownloadEvent.DOWNLOAD_COMPLETED
						: DownloadEvent.DOWNLOAD_ABORTED));
				destroy();
			}
		});
	}

	public void downloadInAThread(final URL url, final File des) {
		new SwingWorker("Downloading " + url) {
			public Object construct() {
				EventQueue.invokeLater(new Runnable() {
					public void run() {
						fireDownloadEvent(new DownloadEvent(Download.this, DownloadEvent.DOWNLOAD_STARTED));
					}
				});
				download(url, des);
				return null;
			}

			public void finished() {
				if (des.length() == contentLength) {
					des.setLastModified(lastModified);
					fireDownloadEvent(new DownloadEvent(Download.this, DownloadEvent.DOWNLOAD_COMPLETED));
				}
				else {
					fireDownloadEvent(new DownloadEvent(Download.this, DownloadEvent.DOWNLOAD_ABORTED));
				}
				destroy();
			}
		}.start();
	}

	private void showErrorMessage() {
		fireDownloadEvent(new DownloadEvent(Download.this, DownloadEvent.DOWNLOAD_ABORTED));
		if (monitor == null)
			return;
		monitor.hide();
		JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(monitor.getProgressBar()),
				"There is a connection problem to the server.\n" + "Either the server is down, or your computer\n"
						+ "is offline. If you are offline now, please\nplug in and try again.", "Connection problem",
				JOptionPane.ERROR_MESSAGE);
	}

	private void download(URL url, File des) {

		if (url == null)
			return;

		if (monitor != null) {
			EventQueue.invokeLater(new Runnable() {
				public void run() {
					monitor.show(400, 200);
				}
			});
		}

		if (lastModified == -1 && contentLength == -1)
			getFileInfo(url); // don't redo the work if they have been done
		if (contentLength == -1) {
			if (monitor != null) {
				EventQueue.invokeLater(new Runnable() {
					public void run() {
						monitor.hide();
					}
				});
			}
			return;
		}

		Runnable showError = new Runnable() {
			public void run() {
				showErrorMessage();
			}
		};

		URLConnection connect = ConnectionManager.getConnection(url);
		if (connect == null) {
			EventQueue.invokeLater(showError);
			return;
		}

		InputStream is = null;
		try {
			is = connect.getInputStream();
		}
		catch (IOException e) {
			e.printStackTrace();
			EventQueue.invokeLater(showError);
			return;
		}

		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(des);
		}
		catch (IOException e) {
			e.printStackTrace();
			EventQueue.invokeLater(showError);
			return;
		}

		byte b[] = new byte[1024];
		int amount;
		byteCount = 0;
		try {
			while ((amount = is.read(b)) != -1) {
				fos.write(b, 0, amount);
				byteCount += amount;
				if (pleaseCancel)
					break;
				// try {Thread.sleep(10);}catch (InterruptedException ie) {} // simulating network slowdown
				if (monitor != null) {
					EventQueue.invokeLater(new Runnable() {
						public void run() {
							int x = (int) (100 * (float) byteCount / contentLength);
							monitor.getProgressBar().setValue(x);
							monitor.getProgressBar().setString(x + "%");
						}
					});
				}
			}
		}
		catch (IOException ioe) {
			ioe.printStackTrace();
			EventQueue.invokeLater(showError);
		}
		finally {
			if (is != null) {
				try {
					is.close();
				}
				catch (Exception e) {
				}
			}
			try {
				fos.close();
			}
			catch (Exception e) {
			}
			if (monitor != null) {
				EventQueue.invokeLater(new Runnable() {
					public void run() {
						monitor.hide();
					}
				});
			}

		}

	}

	void getFileInfo(URL url) {
		URLConnection conn = ConnectionManager.getConnection(url);
		if (conn == null)
			return;
		lastModified = conn.getLastModified();
		contentLength = conn.getContentLength();
	}

}