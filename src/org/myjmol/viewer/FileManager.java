/* $RCSfile: FileManager.java,v $
 * $Author: qxie $
 * $Date: 2006-11-29 22:46:10 $
 * $Revision: 1.11 $
 *
 * Copyright (C) 2003-2005  Miguel, Jmol Development Team
 *
 * Contact: jmol-developers@lists.sf.net
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.myjmol.viewer;

import org.myjmol.api.JmolAdapter;
import org.myjmol.util.CompoundDocument;
import org.myjmol.util.Logger;

import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.net.MalformedURLException;
import java.io.InputStream;
import java.io.BufferedInputStream;
import java.io.FilterInputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.Reader;
import java.util.zip.GZIPInputStream;

class FileManager {

	Viewer viewer;
	JmolAdapter modelAdapter;
	private String openErrorMessage;

	// for applet proxy
	URL appletDocumentBase = null;
	URL appletCodeBase = null;
	String appletProxy;

	// for expanding names into full path names
	// private boolean isURL;
	private String nameAsGiven;
	private String fullPathName;
	String fileName;
	String inlineData;
	String[] inlineDataArray;
	boolean isInline;
	boolean isDOM;

	private String loadScript;
	private File file;

	private FileOpenThread fileOpenThread;
	private FilesOpenThread filesOpenThread;
	private DOMOpenThread aDOMOpenThread;

	FileManager(Viewer viewer, JmolAdapter modelAdapter) {
		this.viewer = viewer;
		this.modelAdapter = modelAdapter;
		clear();
	}

	String getState() {
		StringBuffer commands = new StringBuffer("# file state;\n");
		commands.append(loadScript);
		commands.append("\n\n");
		return commands.toString();
	}

	void openFile(String name) {
		openFile(name, null, null);
	}

	void clear() {
		setLoadScript("");
	}

	void setLoadScript(String script) {
		loadScript = viewer.getLoadState() + script + "\n";
	}

	void openFile(String name, int[] params, String loadScript) {
		setLoadScript(loadScript);
		String sp = "";
		if (params != null)
			for (int i = 0; i < params.length; i++)
				sp += "," + params[i];
		Logger.info("\nFileManager.openFile(" + name + sp + ")");
		nameAsGiven = name;
		openErrorMessage = fullPathName = fileName = null;
		classifyName(name);
		if (openErrorMessage != null) {
			Logger.error("openErrorMessage=" + openErrorMessage);
			return;
		}
		fileOpenThread = new FileOpenThread(fullPathName, name, params);
		fileOpenThread.run();
	}

	void openFiles(String modelName, String[] names, String loadScript) {
		setLoadScript(loadScript);
		String[] fullPathNames = new String[names.length];
		for (int i = 0; i < names.length; i++) {
			nameAsGiven = names[i];
			openErrorMessage = fullPathName = fileName = null;
			classifyName(names[i]);
			if (openErrorMessage != null) {
				Logger.error("openErrorMessage=" + openErrorMessage);
				return;
			}
			fullPathNames[i] = fullPathName;
		}

		fullPathName = fileName = nameAsGiven = modelName;
		inlineData = "";
		isInline = false;
		isDOM = false;
		filesOpenThread = new FilesOpenThread(fullPathNames, names);
		filesOpenThread.run();
	}

	void openStringInline(String strModel) {
		openStringInline(strModel, null);
	}

	void openStringInline(String strModel, int[] params) {
		loadScript = "data \"model inline\"" + strModel + "end \"model inline\";";
		setLoadScript(loadScript);
		String sp = "";
		if (params != null)
			for (int i = 0; i < params.length; i++)
				sp += "," + params[i];
		Logger.info("FileManager.openStringInline(" + sp + ")");
		openErrorMessage = null;
		fullPathName = fileName = "string";
		inlineData = strModel;
		isInline = true;
		isDOM = false;
		if (params == null)
			fileOpenThread = new FileOpenThread(fullPathName, new StringReader(strModel));
		else fileOpenThread = new FileOpenThread(fullPathName, new StringReader(strModel), params);
		fileOpenThread.run();
	}

	void openStringInline(String[] arrayModels, int[] params) {
		loadScript = "set dataSeparator \"~~~next file~~~\";\ndata \"model inline\"";
		for (int i = 0; i < arrayModels.length; i++) {
			if (i > 0)
				loadScript += "~~~next file~~~";
			loadScript += arrayModels[i];
		}
		loadScript += "end \"model inline\";";
		setLoadScript(loadScript);

		String sp = "";
		if (params != null)
			for (int i = 0; i < params.length; i++)
				sp += "," + params[i];
		Logger.info("FileManager.openStringInline(string[]" + sp + ")");
		openErrorMessage = null;
		fullPathName = fileName = "string[]";
		inlineDataArray = arrayModels;
		isInline = true;
		isDOM = false;
		String[] fullPathNames = new String[arrayModels.length];
		StringReader[] readers = new StringReader[arrayModels.length];
		for (int i = 0; i < arrayModels.length; i++) {
			fullPathNames[i] = "string[" + i + "]";
			readers[i] = new StringReader(arrayModels[i]);
		}
		filesOpenThread = new FilesOpenThread(fullPathNames, readers);
		filesOpenThread.run();
	}

	void openDOM(Object DOMNode) {
		openErrorMessage = null;
		fullPathName = fileName = "JSNode";
		inlineData = "";
		isInline = false;
		isDOM = true;
		aDOMOpenThread = new DOMOpenThread(DOMNode);
		aDOMOpenThread.run();
	}

	void openReader(String fullPathName, String name, Reader reader) {
		openErrorMessage = null;
		this.fullPathName = fullPathName;
		fileName = name;
		fileOpenThread = new FileOpenThread(fullPathName, reader);
		fileOpenThread.run();
	}

	boolean isGzip(InputStream is) throws Exception {
		byte[] abMagic = new byte[4];
		is.mark(5);
		int countRead = is.read(abMagic, 0, 4);
		is.reset();
		return (countRead == 4 && abMagic[0] == (byte) 0x1F && abMagic[1] == (byte) 0x8B);
	}

	boolean isCompoundDocument(InputStream is) throws Exception {
		byte[] abMagic = new byte[8];
		is.mark(9);
		int countRead = is.read(abMagic, 0, 8);
		is.reset();
		return (countRead == 8 && abMagic[0] == (byte) 0xD0 && abMagic[1] == (byte) 0xCF && abMagic[2] == (byte) 0x11
				&& abMagic[3] == (byte) 0xE0 && abMagic[4] == (byte) 0xA1 && abMagic[5] == (byte) 0xB1
				&& abMagic[6] == (byte) 0x1A && abMagic[7] == (byte) 0xE1);
	}

	String getFileAsString(String name) {
		Logger.info("FileManager.getFileAsString(" + name + ")");
		Object t = getInputStreamOrErrorMessageFromName(name);
		if (t instanceof String)
			return "Error:" + t;
		try {
			BufferedInputStream bis = new BufferedInputStream((InputStream) t, 8192);
			InputStream is = bis;
			if (isCompoundDocument(is)) {
				CompoundDocument doc = new CompoundDocument(name, bis);
				return "" + doc.getAllData();
			}
			else if (isGzip(is)) {
				is = new GZIPInputStream(bis);
			}
			BufferedReader br = new BufferedReader(new InputStreamReader(is));
			StringBuffer sb = new StringBuffer(8192);
			String line;
			while ((line = br.readLine()) != null) {
				sb.append(line);
				sb.append('\n');
			}
			return "" + sb;
		}
		catch (Exception ioe) {
			return ioe.getMessage();
		}
	}

	Object waitForClientFileOrErrorMessage() {
		Object clientFile = null;
		if (fileOpenThread != null) {
			clientFile = fileOpenThread.clientFile;
			if (fileOpenThread.errorMessage != null)
				openErrorMessage = fileOpenThread.errorMessage;
			else if (clientFile == null)
				openErrorMessage = "Client file is null loading:" + nameAsGiven;
			fileOpenThread = null;
		}
		else if (filesOpenThread != null) {
			clientFile = filesOpenThread.clientFile;
			if (filesOpenThread.errorMessage != null)
				openErrorMessage = filesOpenThread.errorMessage;
			else if (clientFile == null)
				openErrorMessage = "Client file is null loading:" + nameAsGiven;
		}
		else if (aDOMOpenThread != null) {
			clientFile = aDOMOpenThread.clientFile;
			if (aDOMOpenThread.errorMessage != null)
				openErrorMessage = aDOMOpenThread.errorMessage;
			else if (clientFile == null)
				openErrorMessage = "Client file is null loading:" + nameAsGiven;
			aDOMOpenThread = null;
		}
		if (openErrorMessage != null)
			return openErrorMessage;
		return clientFile;
	}

	String getFullPathName() {
		return fullPathName != null ? fullPathName : nameAsGiven;
	}

	String getFileName() {
		return fileName != null ? fileName : nameAsGiven;
	}

	String getAppletDocumentBase() {
		if (appletDocumentBase == null)
			return "";
		return appletDocumentBase.toString();
	}

	void setAppletContext(URL documentBase, URL codeBase, String jmolAppletProxy) {
		appletDocumentBase = documentBase;
		Logger.info("appletDocumentBase=" + documentBase);
		// dumpDocumentBase("" + documentBase);
		appletCodeBase = codeBase;
		appletProxy = jmolAppletProxy;
	}

	void setAppletProxy(String appletProxy) {
		this.appletProxy = (appletProxy == null || appletProxy.length() == 0 ? null : appletProxy);
	}

	void dumpDocumentBase(String documentBase) {
		Logger.info("dumpDocumentBase:" + documentBase);
		Object inputStreamOrError = getInputStreamOrErrorMessageFromName(documentBase);
		if (inputStreamOrError == null) {
			Logger.error("?Que? ?null?");
		}
		else if (inputStreamOrError instanceof String) {
			Logger.error("Error:" + inputStreamOrError);
		}
		else {
			BufferedReader br = new BufferedReader(new InputStreamReader((InputStream) inputStreamOrError));
			String line;
			try {
				while ((line = br.readLine()) != null)
					Logger.info(line);
				br.close();
			}
			catch (Exception ex) {
				Logger.error("exception caught:" + ex);
			}
		}
	}

	// mth jan 2003 -- there must be a better way for me to do this!?
	final String[] urlPrefixes = { "http:", "https:", "ftp:", "file:" };

	private void classifyName(String name) {
		// isURL = false;
		if (name == null)
			return;
		if (appletDocumentBase != null) {
			// This code is only for the applet
			// isURL = true;
			String defaultDirectory = viewer.getDefaultDirectory();
			try {
				if (defaultDirectory != null && name.indexOf(":/") < 0)
					name = defaultDirectory + "/" + name;
				URL url = new URL(appletDocumentBase, name);
				fullPathName = url.toString();
				// we add one to lastIndexOf(), so don't worry about -1 return value
				fileName = fullPathName.substring(fullPathName.lastIndexOf('/') + 1, fullPathName.length());
			}
			catch (MalformedURLException e) {
				openErrorMessage = e.getMessage();
			}
			return;
		}
		// This code is for the app
		for (int i = 0; i < urlPrefixes.length; ++i) {
			if (name.startsWith(urlPrefixes[i])) {
				// isURL = true;
				try {
					URL url = new URL(name);
					fullPathName = url.toString();
					fileName = fullPathName.substring(fullPathName.lastIndexOf('/') + 1, fullPathName.length());
				}
				catch (MalformedURLException e) {
					openErrorMessage = e.getMessage();
				}
				return;
			}
		}
		// isURL = false;
		file = new File(name);
		fullPathName = file.getAbsolutePath();
		fileName = file.getName();
	}

	Object getInputStreamOrErrorMessageFromName(String name) {
		String errorMessage = null;
		int iurlPrefix;
		for (iurlPrefix = urlPrefixes.length; --iurlPrefix >= 0;)
			if (name.startsWith(urlPrefixes[iurlPrefix]))
				break;
		boolean isURL = (iurlPrefix >= 0);
		boolean isApplet = (appletDocumentBase != null);
		InputStream in;
		int length;
		String defaultDirectory = viewer.getDefaultDirectory();
		try {
			if (isApplet || isURL) {
				if (isApplet && isURL && appletProxy != null)
					name = appletProxy + "?url=" + URLEncoder.encode(name, "utf-8");
				else if (!isURL && defaultDirectory != null)
					name = defaultDirectory + "/" + name;
				URL url = (isApplet ? new URL(appletDocumentBase, name) : new URL(name));
				Logger.info("FileManager opening " + url.toString());
				URLConnection conn = url.openConnection();
				length = conn.getContentLength();
				in = conn.getInputStream();
			}
			else {
				Logger.info("FileManager opening " + name);
				File file = new File(name);
				length = (int) file.length();
				in = new FileInputStream(file);
			}
			return new MonitorInputStream(in, length);
		}
		catch (Exception e) {
			errorMessage = "" + e;
		}
		return errorMessage;
	}

	Object getBufferedReaderForString(String string) {
		return new BufferedReader(new StringReader(string));
	}

	Object getUnzippedBufferedReaderOrErrorMessageFromName(String name) {
		Object t = getInputStreamOrErrorMessageFromName(name);
		if (t instanceof String)
			return t;
		try {
			BufferedInputStream bis = new BufferedInputStream((InputStream) t, 8192);
			InputStream is = bis;
			if (isCompoundDocument(is)) {
				CompoundDocument doc = new CompoundDocument(name, bis);
				return getBufferedReaderForString("" + doc.getAllData());
			}
			else if (isGzip(is)) {
				is = new GZIPInputStream(bis);
			}
			return new BufferedReader(new InputStreamReader(is));
		}
		catch (Exception ioe) {
			return ioe.getMessage();
		}
	}

	class DOMOpenThread implements Runnable {
		boolean terminated;
		String errorMessage;
		Object aDOMNode;
		Object clientFile;

		DOMOpenThread(Object DOMNode) {
			this.aDOMNode = DOMNode;
		}

		public void run() {
			clientFile = modelAdapter.openDOMReader(aDOMNode);
			errorMessage = null;
			terminated = true;
		}
	}

	class FileOpenThread implements Runnable {
		boolean terminated;
		String errorMessage;
		String fullPathNameInThread;
		String nameAsGivenInThread;
		Object clientFile;
		Reader reader;
		int[] params;

		FileOpenThread(String fullPathName, String nameAsGiven) {
			this.fullPathNameInThread = fullPathName;
			this.nameAsGivenInThread = nameAsGiven;
			this.params = null;
		}

		FileOpenThread(String fullPathName, String nameAsGiven, int[] params) {
			this.fullPathNameInThread = fullPathName;
			this.nameAsGivenInThread = nameAsGiven;
			this.params = params;
		}

		FileOpenThread(String name, Reader reader, int[] params) {
			nameAsGivenInThread = fullPathNameInThread = name;
			this.reader = reader;
			this.params = params;
		}

		FileOpenThread(String name, Reader reader) {
			nameAsGivenInThread = fullPathNameInThread = name;
			this.reader = reader;
		}

		public void run() {
			if (reader != null) {
				openReader(reader);
			}
			else {
				Object t = getInputStreamOrErrorMessageFromName(nameAsGivenInThread);
				if (!(t instanceof InputStream)) {
					errorMessage = (t == null ? "error opening:" + nameAsGivenInThread : (String) t);
				}
				else {
					openInputStream(fullPathNameInThread, fileName, (InputStream) t);
				}
			}
			if (errorMessage != null)
				Logger.error("error opening " + fullPathNameInThread + "\n" + errorMessage);
			terminated = true;
		}

		private void openInputStream(String fullPathName, String fileName, InputStream istream) {
			BufferedInputStream bis = new BufferedInputStream(istream, 8192);
			InputStream is = bis;
			try {
				if (isCompoundDocument(is)) {
					CompoundDocument doc = new CompoundDocument(fullPathName, bis);
					openReader(new StringReader("" + doc.getAllData()));
					return;
				}
				else if (isGzip(is)) {
					is = new GZIPInputStream(bis);
				}
				openReader(new InputStreamReader(is));
			}
			catch (Exception ioe) {
				errorMessage = ioe.getMessage();
			}
		}

		private void openReader(Reader reader) {
			Object clientFile = modelAdapter.openBufferedReader(fullPathNameInThread, new BufferedReader(reader),
					params);
			if (clientFile instanceof String)
				errorMessage = (String) clientFile;
			else this.clientFile = clientFile;
		}
	}

	class FilesOpenThread implements Runnable {
		boolean terminated;
		String errorMessage;
		String[] fullPathNameInThread;
		String[] nameAsGivenInThread;
		Object clientFile;
		Reader[] reader;

		FilesOpenThread(String[] fullPathName, String[] nameAsGiven) {
			this.fullPathNameInThread = fullPathName;
			this.nameAsGivenInThread = nameAsGiven;
		}

		FilesOpenThread(String[] name, Reader[] reader) {
			nameAsGivenInThread = fullPathNameInThread = name;
			this.reader = reader;
		}

		public void run() {
			if (reader != null) {
				openReader(reader);
			}
			else {
				InputStream[] istream = new InputStream[nameAsGivenInThread.length];
				for (int i = 0; i < nameAsGivenInThread.length; i++) {
					Object t = getInputStreamOrErrorMessageFromName(nameAsGivenInThread[i]);
					if (!(t instanceof InputStream)) {
						errorMessage = (t == null ? "error opening:" + nameAsGivenInThread : (String) t);
						terminated = true;
						return;
					}
					istream[i] = (InputStream) t;
				}
				openInputStream(fullPathNameInThread, istream);
			}
			if (errorMessage != null)
				Logger.error("error opening " + fullPathNameInThread + "\n" + errorMessage);
			terminated = true;
		}

		private void openInputStream(String[] fullPathName, InputStream[] istream) {
			Reader[] zistream = new Reader[istream.length];
			for (int i = 0; i < istream.length; i++) {
				BufferedInputStream bis = new BufferedInputStream(istream[i], 8192);
				InputStream is = bis;
				try {
					if (isCompoundDocument(is)) {
						CompoundDocument doc = new CompoundDocument(fullPathName[i], bis);
						zistream[i] = new StringReader("" + doc.getAllData());
					}
					else if (isGzip(is)) {
						zistream[i] = new InputStreamReader(new GZIPInputStream(bis));
					}
					else {
						zistream[i] = new InputStreamReader(is);
					}
				}
				catch (Exception ioe) {
					errorMessage = ioe.getMessage();
					return;
				}
			}
			openReader(zistream);
		}

		private void openReader(Reader[] reader) {
			BufferedReader[] buffered = new BufferedReader[reader.length];
			for (int i = 0; i < reader.length; i++) {
				buffered[i] = new BufferedReader(reader[i]);
			}
			Object clientFile = modelAdapter.openBufferedReaders(fullPathNameInThread, buffered);
			if (clientFile instanceof String)
				errorMessage = (String) clientFile;
			else this.clientFile = clientFile;
		}
	}
}

class MonitorInputStream extends FilterInputStream {
	int length;
	int position;
	int markPosition;
	int readEventCount;
	long timeBegin;

	MonitorInputStream(InputStream in, int length) {
		super(in);
		this.length = length;
		this.position = 0;
		timeBegin = System.currentTimeMillis();
	}

	public int read() throws IOException {
		++readEventCount;
		int nextByte = super.read();
		if (nextByte >= 0)
			++position;
		return nextByte;
	}

	public int read(byte[] b) throws IOException {
		++readEventCount;
		int cb = super.read(b);
		if (cb > 0)
			position += cb;
		return cb;
	}

	public int read(byte[] b, int off, int len) throws IOException {
		++readEventCount;
		int cb = super.read(b, off, len);
		if (cb > 0)
			position += cb;
		return cb;
	}

	public long skip(long n) throws IOException {
		long cb = super.skip(n);
		// this will only work in relatively small files ... 2Gb
		position = (int) (position + cb);
		return cb;
	}

	public void mark(int readlimit) {
		super.mark(readlimit);
		markPosition = position;
	}

	public void reset() throws IOException {
		position = markPosition;
		super.reset();
	}

	int getPosition() {
		return position;
	}

	int getLength() {
		return length;
	}

	int getPercentageRead() {
		return position * 100 / length;
	}

	int getReadingTimeMillis() {
		return (int) (System.currentTimeMillis() - timeBegin);
	}

}
