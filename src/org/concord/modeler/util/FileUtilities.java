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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;
import java.util.HashMap;

import javax.swing.filechooser.FileFilter;

public class FileUtilities {

	public final static byte COPY_SUCCESS = 0;
	public final static byte SOURCE_NOT_FOUND = 1;
	public final static byte FILE_ACCESS_ERROR = 2;
	public final static byte WRITING_ERROR = 3;
	public final static byte DIRECTORY_ERROR = 4;
	public final static byte ILLEGAL_CHARACTER = 5;
	public final static byte NAMECHECK_SUCCESS = 6;

	private final static String FILE_SEPARATOR = System.getProperty("file.separator");
	private final static String OS = System.getProperty("os.name");

	private FileUtilities() {
	}

	/**
	 * parse the query string and put the parameters in key-value pairs into a <code>Map</code>
	 */
	public static Map getQueryParameterMap(String queryString) {
		if (queryString == null)
			return null;
		int i = queryString.indexOf("?");
		if (i == queryString.length() - 1)
			return null;
		String s = queryString.substring(i + 1);
		Map<String, String> map = new HashMap<String, String>();
		String[] sp = s.split("&");
		for (i = 0; i < sp.length; i++) {
			int j = sp[i].indexOf("=");
			if (j != -1) {
				map.put(sp[i].substring(0, j), sp[i].substring(j + 1));
			}
			else {
				map.put(sp[i], "");
			}
		}
		return map;
	}

	public static byte checkFileName(File file) {
		if (file == null)
			return FILE_ACCESS_ERROR;
		String parentDir = getCodeBase(file.toString());
		if (parentDir == null)
			return DIRECTORY_ERROR;
		if (!new File(parentDir).exists())
			return DIRECTORY_ERROR;
		String s = FileUtilities.getFileName(file.toString());
		if (OS.startsWith("Windows")) {
			if (s.indexOf('*') != -1 || s.indexOf('?') != -1 || s.indexOf(':') != -1 || s.indexOf('<') != -1
					|| s.indexOf('>') != -1 || s.indexOf('|') != -1 || s.indexOf('/') != -1 || s.indexOf('\"') != -1)
				return ILLEGAL_CHARACTER;
		}
		else {
			if (s.indexOf(':') != -1)
				return ILLEGAL_CHARACTER;
		}
		return NAMECHECK_SUCCESS;
	}

	public static String changeExtension(String fileName, String ext) {
		StringBuffer sb = new StringBuffer(fileName);
		int dot = sb.lastIndexOf(".");
		sb.replace(dot + 1, sb.length(), ext);
		return new String(sb);
	}

	/** @return the extension of a file name */
	public static String getSuffix(String filename) {
		String extension = null;
		int index = filename.lastIndexOf('.');
		if (index >= 1 && index < filename.length() - 1) {
			extension = filename.substring(index + 1);
		}
		return extension;
	}

	public static String removeSuffix(String fileName) {
		int doc = fileName.lastIndexOf(".");
		if (doc == -1)
			return fileName;
		return fileName.substring(0, doc);
	}

	public static String changeExtension(String fileName, String ext, int increment) {
		if (increment < 0)
			throw new IllegalArgumentException("Increment must be non-negative.");
		StringBuffer sb = new StringBuffer(fileName);
		int dot = sb.lastIndexOf(".");
		sb.replace(dot + 1, sb.length(), ext);
		sb.insert(dot, "$" + increment);
		return new String(sb);
	}

	/** return the file name of this path */
	public static String getFileName(String path) {
		if (path == null)
			return null;
		int i = path.lastIndexOf("/");
		if (i == -1)
			i = path.lastIndexOf("\\");
		if (i == -1)
			i = path.lastIndexOf(FILE_SEPARATOR);
		if (i == -1)
			return path;
		return path.substring(i + 1, path.length());
	}

	public static String getParentDirectoryName(String path) {
		if (path == null)
			return null;
		String s = getCodeBase(path);
		return getFileName(s.substring(0, s.length() - 1));
	}

	/** http-encode this URL path. */
	public static String httpEncode(String path) {
		if (path == null)
			return null;
		if (path.indexOf(" ") == -1)
			return path;
		return path.replaceAll(" ", "%20");
	}

	/** http-decode this URL path. */
	public static String httpDecode(String path) {
		if (path == null)
			return null;
		if (path.indexOf("%20") == -1)
			return path;
		return path.replaceAll("%20", " ");
	}

	/** return the parent path of this path. */
	public static String getCodeBase(String path) {
		if (path == null)
			return null;
		if (path.toLowerCase().indexOf("http://") != -1) {
			int i = path.lastIndexOf('/');
			if (i == -1)
				return null;
			return path.substring(0, i + 1);
		}
		int i = path.lastIndexOf(FILE_SEPARATOR);
		if (i == -1)
			i = path.lastIndexOf("/");
		if (i == -1)
			return null;
		return path.substring(0, i + 1);
	}

	public static String useSystemFileSeparator(String str) {
		if (str == null)
			return null;
		if (isRelative(str))
			return str;
		if (OS.startsWith("Windows")) {
			if (isRemote(str)) {
				str = str.replace(FILE_SEPARATOR.charAt(0), '/');
			}
			else {
				str = str.replace('/', FILE_SEPARATOR.charAt(0));
			}
		}
		else {
			str = str.replace('\\', FILE_SEPARATOR.charAt(0));
		}
		return str;
	}

	/**
	 * If the user does not input the extension specified by the file filter, automatically augment the file name with
	 * the specified extension.
	 */
	public static String fileNameAutoExtend(FileFilter filter, File file) {
		if (filter == null)
			return file.getAbsolutePath();
		String description = filter.getDescription().toLowerCase();
		String extension = getExtensionInLowerCase(file);
		String filename = file.getAbsolutePath();
		if (extension != null) {
			if (!filter.accept(file)) {
				filename = file.getAbsolutePath().concat(".").concat(description);
			}
		}
		else {
			filename = file.getAbsolutePath().concat(".").concat(description);
		}
		return filename;
	}

	/**
	 * @return the prefix of a file's name, i.e. the characters before the period. If the file represents a directory,
	 *         return null.
	 */
	public static String getPrefix(File file) {
		if (file == null || file.isDirectory())
			return null;
		String filename = file.getName();
		int index = filename.lastIndexOf('.');
		if (index >= 1 && index < filename.length() - 1)
			return filename.substring(0, index);
		return null;
	}

	/**
	 * @return the prefix of a file's name, i.e. the characters before the period.
	 */
	public static String getPrefix(String file) {
		int index = file.lastIndexOf('.');
		if (index >= 1 && index < file.length() - 1)
			return file.substring(0, index);
		return null;
	}

	/** @return the extension of a file name in lower case */
	public static String getExtensionInLowerCase(File file) {
		if (file == null || file.isDirectory())
			return null;
		String extension = getSuffix(file.getName());
		if (extension != null)
			return extension.toLowerCase();
		return null;
	}

	/** @return the extension of a file name in upper case */
	public static String getExtensionInUpperCase(File file) {
		if (file == null || file.isDirectory())
			return null;
		String extension = getSuffix(file.getName());
		if (extension != null)
			return extension.toUpperCase();
		return null;
	}

	/**
	 * copy a file. This method should be used for renaming a file instead of <code>File.renameTo()</code>. The
	 * latter involves native methods which cannot be predicted within a Java application.
	 * 
	 * @param s
	 *            the source
	 * @param d
	 *            the destination
	 * @return COPY_SUCCESS if copied successfully, SOURCE_NOT_FOUND if the source is not found, FILE_ACCESS_ERROR if
	 *         access to the destination is not allowed.
	 */
	public static byte copy(File s, File d) {
		if (s == null || d == null)
			throw new IllegalArgumentException("File cannot be null.");
		if (!s.exists())
			return SOURCE_NOT_FOUND;
		if (s.length() == 0)
			return FILE_ACCESS_ERROR;
		FileInputStream is = null;
		try {
			is = new FileInputStream(s);
		}
		catch (FileNotFoundException e) {
			e.printStackTrace();
			return SOURCE_NOT_FOUND;
		}
		File dest = null;
		if (s.equals(d)) {
			dest = new File(d.getAbsolutePath() + ".tmp");
		}
		else {
			dest = d;
		}
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(dest);
		}
		catch (IOException e) {
			e.printStackTrace();
			return FILE_ACCESS_ERROR;
		}
		byte b[] = new byte[1024];
		int amount;
		boolean returnError = false;
		try {
			while ((amount = is.read(b)) != -1) {
				fos.write(b, 0, amount);
			}
		}
		catch (IOException e) {
			e.printStackTrace();
			returnError = true;
		}
		finally {
			try {
				is.close();
			}
			catch (IOException e) {
				e.printStackTrace();
			}
			try {
				fos.close();
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}
		if (returnError)
			return WRITING_ERROR;
		if (dest != d) {
			copy(dest, d);
			// WARNING: The following comment-out method would slow the process - DON'T USE
			// System.gc(); // work around to make the file deletable
			// dest.delete();
			dest.deleteOnExit();
		}
		return COPY_SUCCESS;
	}

	/**
	 * download a remote document to the file system.
	 * 
	 * @param s
	 *            the source represented by an URL
	 * @param d
	 *            the destination in the file system
	 */
	public static byte copy(URL s, File d) {
		if (s == null)
			throw new IllegalArgumentException("URL cannot be null.");
		if (d == null)
			throw new IllegalArgumentException("File cannot be null.");
		InputStream is = null;
		URLConnection connect = null;
		try {
			connect = s.openConnection();
		}
		catch (IOException e) {
			e.printStackTrace();
			return SOURCE_NOT_FOUND;
		}
		if (connect != null) {
			try {
				is = connect.getInputStream();
			}
			catch (IOException e) {
				e.printStackTrace();
				return SOURCE_NOT_FOUND;
			}
		}
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(d);
		}
		catch (IOException e) {
			e.printStackTrace();
			return FILE_ACCESS_ERROR;
		}
		byte b[] = new byte[1024];
		int amount;
		boolean returnError = false;
		try {
			while ((amount = is.read(b)) != -1) {
				fos.write(b, 0, amount);
			}
		}
		catch (IOException e) {
			e.printStackTrace();
			returnError = true;
		}
		finally {
			try {
				is.close();
			}
			catch (IOException e) {
				e.printStackTrace();
			}
			try {
				fos.close();
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}
		if (returnError)
			return WRITING_ERROR;
		return COPY_SUCCESS;
	}

	public static byte copy(String s, File d) {
		if (isRemote(s)) {
			URL url = null;
			try {
				url = new URL(s);
			}
			catch (MalformedURLException e) {
				e.printStackTrace();
			}
			return copy(url, d);
		}
		return copy(new File(s), d);
	}

	public static boolean isRemote(String s) {
		if (s == null)
			throw new IllegalArgumentException("null input");
		String s1 = s.toLowerCase().trim();
		if (s1.startsWith("http://"))
			return true;
		if (s1.startsWith("jar:"))
			return true;
		return false;
	}

	public static boolean isRelative(String url) {
		if (url == null)
			throw new IllegalArgumentException("null input");
		if (url.indexOf(":") != -1)
			return false;
		if (OS.startsWith("Mac") || OS.startsWith("Linux")) {
			if (url.startsWith("/"))
				return false;
		}
		return true;
	}

	public static String concatenate(String basePath, String relativePath) {
		if (isRemote(basePath)) {
			if (!basePath.endsWith("/"))
				basePath += "/";
		}
		else {
			if (OS.startsWith("Windows")) {
				if (!basePath.endsWith("\\"))
					basePath += "\\";
			}
			else {
				if (!basePath.endsWith("/"))
					basePath += "/";
			}
		}
		if (!isRelative(relativePath))
			return basePath + relativePath;
		if (relativePath.startsWith("..")) {
			String s = "";
			if (isRemote(basePath) || !OS.startsWith("Windows")) {
				String[] s1 = basePath.split("/");
				String[] s2 = relativePath.split("/");
				int n = 0;
				for (String i : s2) {
					if ("..".equals(i))
						n++;
				}
				for (int i = 0; i < s1.length - n; i++)
					s += s1[i] + "/";
				for (int i = n; i < s2.length - 1; i++)
					s += s2[i] + "/";
				s += s2[s2.length - 1];
			}
			else {
				String[] s1 = basePath.split("\\");
				String[] s2 = relativePath.split("\\");
				int n = 0;
				for (String i : s2) {
					if ("..".equals(i))
						n++;
				}
				for (int i = 0; i < s1.length - n; i++)
					s += s1[i] + "\\";
				for (int i = n; i < s2.length - 1; i++)
					s += s2[i] + "\\";
				s += s2[s2.length - 1];
			}
			return s;
		}
		return basePath + relativePath;
	}

	/** remove the whole directory */
	public static void deleteAllFiles(File dir) {
		if (dir == null)
			return;
		File[] list = dir.listFiles();
		if (list == null)
			return;
		for (File i : list) {
			if (i.isFile()) {
				i.delete();
			}
			else {
				deleteAllFiles(i);
			}
		}
	}

}