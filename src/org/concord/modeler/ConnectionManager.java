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

import java.awt.Toolkit;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.ImageIcon;

import org.concord.modeler.event.ProgressListener;
import org.concord.modeler.util.FileUtilities;

public class ConnectionManager {

	private static int connectTimeout = 5000;
	private static int readTimeout = 30000;
	private final static ConnectionManager sharedInstance = new ConnectionManager();
	private AtomicBoolean checkUpdate = new AtomicBoolean(true);
	private boolean updateFirstFile;
	private boolean allowCaching = true;
	private boolean workOffline;
	private List<ProgressListener> progressListeners;

	private ConnectionManager() {
	}

	public final static ConnectionManager sharedInstance() {
		return sharedInstance;
	}

	public void addProgressListener(ProgressListener listener) {
		// TODO: I do not know how to tell the owner of the progress from this singleton
		if (progressListeners == null)
			progressListeners = new ArrayList<ProgressListener>();
		progressListeners.add(listener);
	}

	public void removeProgressListener(ProgressListener listener) {
		if (progressListeners == null)
			return;
		progressListeners.remove(listener);
	}

	/** return a HttpURLConnection with the current timeout settings */
	public static HttpURLConnection getConnection(String address) {
		if (address == null)
			return null;
		URL u = null;
		try {
			u = new URL(FileUtilities.httpEncode(address));
		}
		catch (MalformedURLException e) {
			e.printStackTrace();
			return null;
		}
		return getConnection(u);
	}

	/** return a HttpURLConnection with the current timeout settings */
	public static HttpURLConnection getConnection(URL u) {
		URLConnection connection;
		try {
			connection = u.openConnection();
		}
		catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		connection.setConnectTimeout(connectTimeout);
		connection.setReadTimeout(readTimeout);
		return (HttpURLConnection) connection;
	}

	public void setCachingAllowed(boolean b) {
		allowCaching = b;
	}

	public boolean isCachingAllowed() {
		return allowCaching;
	}

	public static void setConnectTimeout(int i) {
		connectTimeout = i;
	}

	public static int getConnectTimeout() {
		return connectTimeout;
	}

	public static void setReadTimeout(int i) {
		readTimeout = i;
	}

	public static int getReadTimeout() {
		return readTimeout;
	}

	public void setCheckUpdate(boolean b) {
		checkUpdate.set(b);
	}

	public void setWorkOffline(boolean b) {
		workOffline = b;
	}

	public boolean getWorkOffline() {
		return workOffline;
	}

	/**
	 * get the cached file for a resource specified by the URL, return null if not cached
	 */
	public File getLocalCopy(URL url) {
		if (url == null)
			throw new IllegalArgumentException("Null URL");
		return new File(getCacheDirectory(), convertURLToFileName(url));
	}

	/**
	 * get the cached file for a resource specified by the path, return null if not cached
	 */
	public File getLocalCopy(String path) {
		URL url = null;
		try {
			url = new URL(path);
		}
		catch (MalformedURLException e) {
			e.printStackTrace();
		}
		if (url == null)
			return null;
		return getLocalCopy(url);
	}

	/** if a local file is a copy of a remote file, return the remote file's URL */
	public URL getRemoteCopy(File file) {
		if (file == null)
			return null;
		return getRemoteCopy(file.toString());
	}

	public URL getRemoteCopy(String t) {
		if (t == null)
			return null;
		boolean isWindows = System.getProperty("os.name").startsWith("Windows");
		if (isWindows && !FileUtilities.isRemote(t)) {
			t = t.replace('/', '\\');
		}
		String s = getCacheDirectory().toString();
		if (!t.startsWith(s))
			return null;
		s = t.substring(s.length());
		s = s.replace('@', ':');
		if (isWindows) {
			s = s.replace(System.getProperty("file.separator").charAt(0), '/');
		}
		URL u = null;
		try {
			u = new URL("http:/" + s);
		}
		catch (MalformedURLException e) {
			e.printStackTrace();
		}
		return u;
	}

	/**
	 * check if there is a local copy for this image. If no, cache the image. Always return the local copy, always set
	 * the description to be the URL (not the cache address).
	 */
	public ImageIcon loadImage(URL url) {
		if (url == null)
			return null;
		if (!allowCaching)
			return new ImageIcon(url);
		File file = null;
		try {
			file = shouldUpdate(url);
			if (file == null)
				file = cache(url);
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		if (file == null)
			return null;
		return new ImageIcon(Toolkit.getDefaultToolkit().createImage(file.toString()), url.toString());
	}

	/**
	 * return null if cache should update or caching is not permitted. Return the file otherwise.
	 */
	public File shouldUpdate(String s) {
		if (!allowCaching)
			return null;
		if (isDynamicalContent(s))
			return null;
		if (s.toLowerCase().indexOf("http://") == -1)
			throw new IllegalArgumentException("Illegal URL address");
		URL u = null;
		try {
			u = new URL(s);
		}
		catch (MalformedURLException e) {
			e.printStackTrace();
			return null;
		}
		return shouldUpdate(u);
	}

	/**
	 * return null if cache should update or caching is not permitted. Return the file otherwise.
	 */
	public File shouldUpdate(URL url) {
		if (!allowCaching)
			return null;
		if (isDynamicalContent(url))
			return null;
		if (url.toString().toLowerCase().startsWith("jar:"))
			return null;
		File file = null;
		boolean update = false;
		if (isCached(url)) {
			file = sharedInstance.getLocalCopy(url);
			if (file == null) {
				update = true;
			}
			else {
				if (file.exists()) {
					if (workOffline)
						return file;
					// ONLY check update for the first cml file when a batch of files for a page are to be loaded.
					if (checkUpdate.get()) {
						long lm = getLastModified(url);
						// System.out.println(url + "=" + new java.util.Date(lm));
						// what about people from different time zone?
						if (lm > 0L && lm - file.lastModified() > 5000L) {
							update = true; // allow 5 second tolerance
						}
						else {
							update = false;
							// lm=0 means query for last modified time has failed, use the local copy
						}
						updateFirstFile = update;
						checkUpdate.set(false);
					}
					// if the first file has been updated, all the other files are assumed to need update
					// check as well, regardless of wether or not they have actually been changed.
					if (updateFirstFile)
						update = true;
				}
				else {
					update = true;
				}
			}
		}
		else {
			update = true;
		}
		return update ? null : file;
	}

	public static void clearCache() {
		FileUtilities.deleteAllFiles(getCacheDirectory());
	}

	public static File getCacheDirectory() {
		return Initializer.sharedInstance().getCacheDirectory();
	}

	private boolean isCached(URL url) {
		if (!allowCaching)
			return false;
		if (url == null)
			throw new IllegalArgumentException("Null URL");
		if (url.toString().toLowerCase().startsWith("jar:"))
			return false;
		return new File(getCacheDirectory(), convertURLToFileName(url)).exists();
	}

	/**
	 * cache a Web resource into a local file. Returns null if caching is not allowed.
	 */
	public File cache(String s) throws FileNotFoundException {
		if (!allowCaching)
			return null;
		if (s.toLowerCase().startsWith("jar:"))
			return null;
		if (s.toLowerCase().indexOf("http://") == -1)
			throw new IllegalArgumentException("Illegal URL address");
		URL u = null;
		try {
			u = new URL(FileUtilities.httpEncode(s));
		}
		catch (MalformedURLException e) {
			e.printStackTrace();
			return null;
		}
		return cache(u);
	}

	/**
	 * cache a Web resource into a local file. Returns null if caching is not allowed.
	 */
	public File cache(URL url) throws FileNotFoundException {

		if (!allowCaching)
			return null;
		if (url == null)
			throw new IllegalArgumentException("Null URL");
		if (url.toString().toLowerCase().startsWith("jar:"))
			return null;

		// if (!ModelerUtilities.ping(url, 10000)) return null;

		URLConnection connect = getConnection(url);
		if (connect == null)
			return null;

		InputStream is = null;
		try {
			is = connect.getInputStream();
		}
		catch (FileNotFoundException e) {
			throw new FileNotFoundException(e.getMessage());
		}
		catch (IOException e) {
			e.printStackTrace();
			return null;
		}

		File cachedFile = new File(Initializer.sharedInstance().getCacheDirectory(), convertURLToFileName(url));
		cachedFile.getParentFile().mkdirs();

		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(cachedFile);
		}
		catch (FileNotFoundException e) {
			e.printStackTrace();
			throw new FileNotFoundException(e.getMessage());
		}

		byte b[] = new byte[1024];
		int amount = 0;
		boolean error = false;
		try {
			while ((amount = is.read(b)) != -1)
				fos.write(b, 0, amount);
		}
		catch (IOException e) {
			e.printStackTrace();
			error = true;
		}
		finally {
			if (is != null) {
				try {
					is.close();
				}
				catch (IOException e) {
				}
			}
			try {
				fos.close();
			}
			catch (IOException e) {
			}
		}
		if (error)
			return null;
		cachedFile.setLastModified(connect.getLastModified());

		return cachedFile;

	}

	/**
	 * get the last-modified time of this Web resource. The result is the number of milliseconds since January 1, 1970
	 * GMT. This is used to check update for a resource.
	 */
	private static long getLastModified(URL url) {
		// System.out.println(Thread.currentThread()+":"+url);
		if (url == null)
			throw new IllegalArgumentException("Null URL");
		if (sharedInstance.getWorkOffline())
			return 0;
		// if(!ServerChecker.sharedInstance().shouldCheck(url.getHost())) return 0;
		HttpURLConnection conn = getConnection(url);
		if (conn == null)
			return 0;
		try {
			conn.setRequestMethod("HEAD");
		}
		catch (ProtocolException e) {
			e.printStackTrace();
		}
		// conn.disconnect();
		return conn.getLastModified();
	}

	/**
	 * get the last-modified time and size of this Web resource. The time is the number of milliseconds since January 1,
	 * 1970 GMT.
	 */
	public static long[] getLastModifiedAndContentLength(URL url) {
		if (url == null)
			throw new IllegalArgumentException("Null URL");
		long[] t = new long[2];
		t[0] = 0L;
		t[1] = 0L;
		if (sharedInstance.getWorkOffline())
			return t;
		File file = sharedInstance.getLocalCopy(url);
		if (file == null || !file.exists()) {
			HttpURLConnection conn = getConnection(url);
			if (conn == null)
				return null;
			try {
				conn.setRequestMethod("HEAD");
			}
			catch (ProtocolException e) {
				e.printStackTrace();
			}
			t[0] = conn.getLastModified();
			t[1] = conn.getContentLength();
		}
		else {
			t[0] = file.lastModified();
			t[1] = file.length();
		}
		return t;
	}

	public static String convertURLToFileName(URL url) {
		if (url == null)
			return null;
		if (url.getPort() == -1)
			return url.getHost() + FileUtilities.httpDecode(url.getPath());
		return url.getHost() + "@" + url.getPort() + FileUtilities.httpDecode(url.getPath());
	}

	public static boolean isDynamicalContent(String address) {
		if (address == null)
			return false;
		if (!FileUtilities.isRemote(address))
			return false;
		if (address.endsWith(".jsp"))
			return true;
		URL u;
		try {
			u = new URL(address);
		}
		catch (MalformedURLException e) {
			e.printStackTrace();
			return false;
		}
		return u.getQuery() != null;
	}

	public static boolean isDynamicalContent(URL url) {
		if (url == null)
			return false;
		if (url.toString().endsWith(".jsp"))
			return true;
		return url.getQuery() != null;
	}

}