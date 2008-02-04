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
import java.awt.EventQueue;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.swing.JOptionPane;
import javax.swing.JProgressBar;

import org.concord.modeler.ui.ProcessMonitor;
import org.concord.modeler.util.SwingWorker;

public class Zipper {

	private final static Zipper sharedInstance = new Zipper();
	private ProcessMonitor monitor;
	private List<Component> componentToLock;
	private long byteCount;

	private Zipper() {
	}

	public final static Zipper sharedInstance() {
		return sharedInstance;
	}

	public void setProcessMonitor(ProcessMonitor pm) {
		monitor = pm;
	}

	public ProcessMonitor getProcessMonitor() {
		return monitor;
	}

	public void addComponentToLock(Component c) {
		if (componentToLock == null)
			componentToLock = new ArrayList<Component>();
		componentToLock.add(c);
	}

	public void removeComponentToLock(Component c) {
		if (componentToLock == null)
			return;
		componentToLock.remove(c);
	}

	private void lockComponents(final boolean b) {
		if (componentToLock == null || componentToLock.isEmpty())
			return;
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				for (Component c : componentToLock) {
					c.setEnabled(!b);
				}
			}
		});
	}

	/** unzip the resource at the address to the folder with a thread */
	public void unzipInAThread(final URL url, final File dir) {
		new SwingWorker("Unzipper") {
			public Object construct() {
				lockComponents(true);
				unzip(url, dir);
				return dir;
			}

			public void finished() {
				lockComponents(false);
			}
		}.start();
	}

	/** unzip the resource at the address to the folder with a thread */
	public void unzipInAThread(final String url, final File dir) {
		new SwingWorker("Unzipper") {
			public Object construct() {
				lockComponents(true);
				unzip(url, dir);
				return dir;
			}

			public void finished() {
				lockComponents(false);
			}
		}.start();
	}

	/* unzip the resources at the addresses to the folder with a thread */
	void unzipInAThread(final String[] url, final String[] msg, final File dir) {
		new SwingWorker("Unzipper") {
			public Object construct() {
				lockComponents(true);
				for (int i = 0; i < url.length; i++) {
					final int i2 = i;
					EventQueue.invokeLater(new Runnable() {
						public void run() {
							Zipper.sharedInstance().getProcessMonitor().setTitle(msg[i2]);
						}
					});
					unzip(url[i], dir);
				}
				return dir;
			}

			public void finished() {
				lockComponents(false);
			}
		}.start();
	}

	/* unzip the resource at the address to the folder */
	void unzip(String address, File dir) {
		try {
			unzip(new URL(address), dir);
		}
		catch (MalformedURLException e) {
			e.printStackTrace();
		}
	}

	/* unzip the resource at the address to the folder */
	void unzip(URL url, File dir) {
		if (url == null)
			return;
		URLConnection conn = ConnectionManager.getConnection(url);
		if (conn == null)
			return;
		long total = getTotalSize(url);
		if (total == -1L) {
			if (monitor != null)
				monitor.hide();
			return;
		}
		ZipInputStream zis = null;
		try {
			zis = new ZipInputStream(conn.getInputStream());
		}
		catch (Exception e) {
			e.printStackTrace();
			return;
		}
		unzip(zis, dir, total);
	}

	/**
	 * unzip from a <code>ZipInputStream</code> to the designated directory <code>dir</code>. If the total size of
	 * the source file is known, please specify in the <code>total</code> argument.
	 */
	public List<String> unzip(ZipInputStream zis, File dir, final long total) {

		if (zis == null)
			return null;

		if (monitor != null) {
			EventQueue.invokeLater(new Runnable() {
				public void run() {
					if (!monitor.isShowing())
						monitor.show(400, 300);
				}
			});
		}

		byteCount = 0L;
		int amount;
		FileOutputStream fos = null;
		byte b[] = new byte[1024];
		File file = null, parent = null;
		ZipEntry ze = null;
		boolean writeFile;
		List<String> list = null;

		try {

			while ((ze = zis.getNextEntry()) != null) {

				if (!ze.isDirectory()) {

					writeFile = true;
					file = new File(dir, ze.getName());
					if (file.exists()) {
						/*
						 * cached files are not supposed to be changed by anyone. If someone modified a cached file,
						 * calling this function again will restore it. This guarantees that the integrity of data, is
						 * always under our control, and that leaves the user a chance to overwrite ruined data.
						 */
						/*
						 * if(file.lastModified()>=ze.getTime()){ writeFile=false; }
						 */
					}
					else {
						parent = file.getParentFile();
						if (parent != null)
							parent.mkdirs();
					}

					if (writeFile) {
						fos = new FileOutputStream(file);
						while ((amount = zis.read(b)) != -1) {
							fos.write(b, 0, amount);
							byteCount += amount;
						}
						try {
							fos.close();
						}
						catch (IOException e) {
						}
						file.setLastModified(ze.getTime());
					}
					else {
						byteCount += ze.getSize();
					}

					if (monitor != null) {
						EventQueue.invokeLater(new Runnable() {
							public void run() {
								if (monitor != null) {
									if (monitor.getProgressBar() == null) {
										monitor.setProgressBar(new JProgressBar());
									}
									if (total > 0) {
										monitor.getProgressBar().setValue((int) (100 * (float) byteCount / total));
										monitor.getProgressBar().setString(monitor.getProgressBar().getValue() + "%");
									}
									else {
										monitor.getProgressBar().setString(byteCount + " bytes read");
									}
								}
							}
						});
					}

				}
				else {
					if (list == null)
						list = new ArrayList<String>();
					list.add(ze.getName());
				}
			}

		}
		catch (IOException e) {
			if (monitor != null) {
				EventQueue.invokeLater(new Runnable() {
					public void run() {
						monitor.hide();
						JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(monitor.getProgressBar()),
								"There is a connection problem to the server\n"
										+ "Either the server is down, or your computer\n"
										+ "is offline. If you are offline now, please\n" + "plug in and try again.",
								"Connection problem", JOptionPane.ERROR_MESSAGE);
						lockComponents(false);
					}
				});
			}
			e.printStackTrace();
		}
		finally {
			if (fos != null) { // lest the last fos is not closed
				try {
					fos.close();
				}
				catch (IOException e) {
				}
			}
			if (zis != null) {
				try {
					zis.close();
				}
				catch (IOException e) {
				}
			}
			if (monitor != null) {
				EventQueue.invokeLater(new Runnable() {
					public void run() {
						if (monitor != null) {
							monitor.hide();
						}
					}
				});
			}
		}

		return list;

	}

	private long getTotalSize(URL url) {

		if (url == null)
			return -1;

		URLConnection conn = ConnectionManager.getConnection(url);
		if (conn == null)
			return -1;

		if (monitor != null) {
			EventQueue.invokeLater(new Runnable() {
				public void run() {
					if (!monitor.isShowing())
						monitor.show(400, 300);
				}
			});
		}

		ZipInputStream zis = null;
		byteCount = 0L;
		int n = 0;
		try {
			zis = new ZipInputStream(conn.getInputStream());
			ZipEntry ze = null;
			while ((ze = zis.getNextEntry()) != null) {
				if (!ze.isDirectory()) {
					byteCount += ze.getSize();
					n++;
					if (monitor != null) {
						final int m = n;
						EventQueue.invokeLater(new Runnable() {
							public void run() {
								if (monitor != null) {
									monitor.getProgressBar().setString("Scanning files: " + m + "......");
								}
							}
						});
					}
				}
			}
		}
		catch (IOException e) {
			e.printStackTrace();
			String errMsg = null;
			if (e instanceof FileNotFoundException) {
				errMsg = url.toString() + "\nwas not found.";
			}
			else {
				errMsg = "There is a connection problem to the server.\nEither the server is down, or your computer\nis offline. If you are offline now, please\nplug in and try again.";
			}
			if (monitor != null) {
				final String s = errMsg;
				EventQueue.invokeLater(new Runnable() {
					public void run() {
						monitor.hide();
						JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(monitor.getProgressBar()), s,
								"Connection problem", JOptionPane.ERROR_MESSAGE);
					}
				});
			}
			byteCount = -1L;
		}
		finally {
			try {
				zis.close();
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}

		return byteCount;

	}

}