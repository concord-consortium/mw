/*
 *   Copyright (C) 2007  The Concord Consortium, Inc.,
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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import org.concord.modeler.text.Page;
import org.concord.modeler.ui.ProcessMonitor;
import org.concord.modeler.util.FileUtilities;
import org.concord.modeler.util.SwingWorker;

public class PageJContainer extends PagePlugin {

	PluginService plugin;
	private String codeBase;
	private String cachedFileNames;
	private static PageJContainerMaker maker;
	private PluginScripter scripter;
	private List<Download> downloadJobs;
	private volatile boolean downloadCancelled;

	public PageJContainer() {
		super();
	}

	public PageJContainer(PageJContainer pageJContainer, Page parent) {
		super(pageJContainer, parent);
		codeBase = pageJContainer.codeBase;
	}

	public void setCodeBase(String s) {
		codeBase = s;
	}

	public String getCodeBase() {
		return codeBase;
	}

	public void setCachedFileNames(String s) {
		cachedFileNames = s;
	}

	public String getCachedFileNames() {
		return cachedFileNames;
	}

	private File[] createJarFiles() {
		int n = jarName.size();
		File[] file = new File[n];
		if (page.isRemote()) {
			URL u = null;
			for (int i = 0; i < n; i++) {
				try {
					u = new URL(FileUtilities.getCodeBase(page.getAddress()) + jarName.get(i));
				}
				catch (MalformedURLException e) {
					e.printStackTrace();
					setErrorMessage("Errors in forming jar URL: " + u);
					return null;
				}
				try {
					file[i] = ConnectionManager.sharedInstance().shouldUpdate(u);
					if (file[i] == null)
						file[i] = ConnectionManager.sharedInstance().cache(u);
					// this thread can run for a while and it will set the checkupdate flag in the Cache
					// Manager to false when it is done. This will result in the cml file to be unable to
					// update. Hence, we should set the checkupdate flag to true afterwards.
					ConnectionManager.sharedInstance().setCheckUpdate(true);
				}
				catch (IOException e) {
					e.printStackTrace();
					setErrorMessage("Errors in caching jar file: " + u);
					return null;
				}
			}
		}
		else {
			for (int i = 0; i < n; i++) {
				file[i] = new File(FileUtilities.getCodeBase(page.getAddress()), jarName.get(i));
			}
		}
		return file;
	}

	private boolean downloadJarFiles() {
		if (downloadJobs == null)
			downloadJobs = new ArrayList<Download>();
		else downloadJobs.clear();
		File pluginDir = Initializer.sharedInstance().getPluginDirectory();
		for (String x : jarName) {
			File f = new File(pluginDir, x);
			long ftime = 0;
			if (f.exists()) {
				ftime = f.lastModified();
			}
			URL u = null;
			try {
				u = new URL(codeBase + x);
			}
			catch (MalformedURLException e) {
				e.printStackTrace();
				setErrorMessage("Error in URL for downloading plugin: " + (codeBase + x));
				return false;
			}
			URLConnection conn = ConnectionManager.getConnection(u);
			if (conn == null) {
				setErrorMessage("Cannot connect to the Internet to " + codeBase);
				return false;
			}
			long utime = conn.getLastModified();
			long usize = conn.getContentLength();
			long fsize = f.length();
			if (utime == 0 && ftime == 0) {
				setErrorMessage("Cannot connect to the Internet to download the plugin at: " + codeBase);
				return false;
			}
			else if (usize <= 0) {
				setErrorMessage("Remote plugin file: " + u + " may be corrupted.");
				return false;
			}
			else if (utime > ftime || fsize != usize) {
				Download download = new Download();
				downloadJobs.add(download);
				download.setInfo(utime, conn.getContentLength());
				ProcessMonitor m = new ProcessMonitor(JOptionPane.getFrameForComponent(this));
				m.getProgressBar().setMinimum(0);
				m.getProgressBar().setMaximum(100);
				m.getProgressBar().setPreferredSize(new Dimension(300, 20));
				download.setProcessMonitor(m);
				if (ftime == 0) {
					String s = Modeler.getInternationalText("DownloadingPlugin");
					download.getProcessMonitor().setTitle((s != null ? s : "Downloading plugin") + ": " + u + "...");
				}
				else {
					String s = Modeler.getInternationalText("DownloadingPluginUpdates");
					download.getProcessMonitor().setTitle(
							(s != null ? s : "Downloading plugin updates") + ": " + u + "...");
				}
				download.getProcessMonitor().setLocationRelativeTo(this);
				download.downloadWithoutThread(u, f);
			}
		}
		return true;
	}

	public void start() {

		if (jarName == null || jarName.isEmpty() || className == null) {
			setErrorMessage("No plugin has been set.");
			return;
		}

		destroyPlugin();

		setInitMessage();

		int n = jarName.size();
		final URL[] url = new URL[n];
		String cachedCodeBase = null;
		File pluginDir = Initializer.sharedInstance().getPluginDirectory();

		if (codeBase == null) {
			final File[] file = createJarFiles();
			if (file == null)
				return;
			cachedCodeBase = (file.length > 0 && file[0] != null) ? FileUtilities.getCodeBase(file[0].toString())
					: page.getPathBase();
			// copy the jar files to the plugin folder, which is a trusted placeholder
			File pluginJar;
			for (int i = 0; i < n; i++) {
				pluginJar = new File(pluginDir, jarName.get(i));
				if (!pluginJar.exists() || shouldReplace(pluginJar, file[i])) {
					FileUtilities.copy(file[i], pluginJar);
					pluginJar.setLastModified(file[i].lastModified());
				}
				try {
					url[i] = pluginJar.toURI().toURL();
				}
				catch (MalformedURLException e) {
					e.printStackTrace();
					setErrorMessage("Errors in forming jar URLs: " + url[i]);
					return;
				}
			}
		}
		else {
			if (!downloadJarFiles())
				return;
			if (downloadCancelled)
				return;
			if (page.isRemote()) {
				cachedCodeBase = FileUtilities.getCodeBase(ConnectionManager.sharedInstance().getLocalCopy(
						page.getAddress()).toString());
			}
			else {
				cachedCodeBase = page.getPathBase();
			}
			File pluginJar;
			for (int i = 0; i < n; i++) {
				pluginJar = new File(pluginDir, jarName.get(i));
				if (!pluginJar.exists()) {
					setErrorMessage(pluginJar + " was not found.");
					return;
				}
				try {
					url[i] = pluginJar.toURI().toURL();
				}
				catch (MalformedURLException e) {
					e.printStackTrace();
					setErrorMessage("Errors in forming jar URLs: " + url[i]);
					return;
				}
			}
		}

		// load the classes
		Class c = null;
		URLClassLoader loader = URLClassLoader.newInstance(url, Modeler.class.getClassLoader());
		try {
			c = loader.loadClass(className.endsWith(".class") ? FileUtilities.removeSuffix(className) : className);
		}
		catch (Throwable e) {
			e.printStackTrace();
			setErrorMessage(e);
			return;
		}

		// check security certificate
		Certificate cert1 = ModelerUtilities.getCertificate(Modeler.class);
		if (cert1 == null) {
			setErrorMessage("MW is not signed yet. Do not load anything.");
			return;
		}
		Certificate cert2 = ModelerUtilities.getCertificate(c);
		if (cert2 == null) {
			setErrorMessage("Sorry, this plugin does not have a security certificate:\n<code>" + c.getName()
					+ "</code>");
			return;
		}
		if (!cert2.equals(cert1)) {
			setErrorMessage("Sorry, we do not recognize the security certificate of this plugin:\n<code>" + c.getName()
					+ "</code>");
			return;
		}

		// instantiate
		Object o = null;
		try {
			o = c.newInstance();
		}
		catch (Throwable e) {
			e.printStackTrace();
			setErrorMessage(e);
			return;
		}

		// set parameters and then initialize the plugin
		if (o instanceof PluginService) {
			plugin = (PluginService) o;
			if (parameterMap != null && !parameterMap.isEmpty())
				for (String key : parameterMap.keySet()) {
					plugin.putParameter(key, parameterMap.get(key));
				}
			final String cachedCodeBase2 = cachedCodeBase;
			EventQueue.invokeLater(new Runnable() {
				public void run() {
					initPlugin(cachedCodeBase2);
				}
			});
		}
		else {
			setErrorMessage("Error: The main class <code>" + className + "</code> does not implement <code>"
					+ PluginService.class.getName() + "</code>");
		}

	}

	private void addPopupMouseListener() {
		plugin.addMouseListener(popupMouseListener);
		Method method;
		Object c = null;
		try {
			method = plugin.getClass().getMethod("getSnapshotComponent", (Class[]) null);
			if (method != null) {
				c = method.invoke(plugin, (Object[]) null);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
			return;
		}
		if (c instanceof Component) {
			((Component) c).addMouseListener(popupMouseListener);
		}
	}

	private void initPlugin(final String codeBase) {
		removeAll();
		if (plugin == null)
			return;
		plugin.getWindow().setPreferredSize(getPreferredSize());
		add(plugin.getWindow(), BorderLayout.CENTER);
		validate();
		SwingWorker worker = new SwingWorker("Cache plugin resources", Thread.MIN_PRIORITY + 1) {
			public Object construct() {
				cacheResources();
				plugin.putParameter("codebase", codeBase); // restore the cached code base
				return null;
			}

			public void finished() {
				try {
					plugin.init();
					loadState();
				}
				catch (Throwable e) {
					e.printStackTrace();
					setErrorMessage("Errors in initializing: " + e);
				}
				try {
					plugin.start();
				}
				catch (Throwable e) {
					e.printStackTrace();
					setErrorMessage("Errors in starting: " + e);
				}
				addPopupMouseListener();
			}
		};
		worker.start();
	}

	private URL createURL(String s) {
		if (s == null)
			return null;
		s = s.trim();
		if (!FileUtilities.isRemote(s)) {
			if (FileUtilities.isRelative(s)) {
				if (!page.isRemote())
					return null;
				s = page.getPathBase() + s;
			}
			else {
				return null;
			}
		}
		URL u = null;
		try {
			u = new URL(s);
		}
		catch (MalformedURLException e) {
			e.printStackTrace();
		}
		return u;
	}

	private void cacheResources() {
		if (plugin == null)
			return;
		if (cachedFileNames != null) {
			String[] t = cachedFileNames.split(",");
			for (int i = 0; i < t.length; i++) {
				t[i] = t[i].trim();
				if (t[i].equals(""))
					continue;
				URL x = createURL(t[i]);
				if (x != null)
					cache(x);
			}
		}
		plugin.putParameter("codebase", page.getPathBase()); // make sure the code base is remote
		URL[] cacheURL = plugin.getCacheResources();
		if (cacheURL != null) {
			for (URL x : cacheURL)
				cache(x);
		}
	}

	private void cache(URL u) {
		try {
			File file = ConnectionManager.sharedInstance().shouldUpdate(u);
			if (file == null)
				file = ConnectionManager.sharedInstance().cache(u);
			ConnectionManager.sharedInstance().setCheckUpdate(true);
		}
		catch (FileNotFoundException e) {
			e.printStackTrace();
			final URL u2 = u;
			EventQueue.invokeLater(new Runnable() {
				public void run() {
					JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(PageJContainer.this), u2
							+ " was not found. Please check your cache settings with plugin #" + index + ".",
							"File not found", JOptionPane.ERROR_MESSAGE);
				}
			});
		}
	}

	public void saveJars(File parent) {
		if (codeBase == null)
			super.saveJars(parent);
	}

	public void saveResources(File parent) {
		if (plugin == null)
			return;
		String[] resources = plugin.getResources();
		if (resources == null)
			return;
		File file = null;
		for (String s : resources) {
			if (FileUtilities.isRemote(s))
				continue;
			s = page.getPathBase() + FileUtilities.getFileName(s);
			if (page.isRemote()) {
				file = ConnectionManager.sharedInstance().getLocalCopy(s);
			}
			else {
				file = new File(s);
			}
			if (file.exists()) {
				FileUtilities.copy(file, new File(parent, FileUtilities.getFileName(s)));
			}
		}
	}

	void loadState(InputStream is) throws Exception {
		if (plugin == null)
			return;
		Method method = plugin.getClass().getMethod("loadState", new Class[] { InputStream.class });
		if (method != null)
			method.invoke(plugin, new Object[] { is });
	}

	public void saveState(File parent) {
		if (parent == null)
			return;
		if (plugin == null)
			return;
		String s = parent.toString();
		String dir = FileUtilities.getCodeBase(s);
		String name = FileUtilities.getFileName(s);
		name = FileUtilities.getPrefix(name) + "$plugin$" + index + ".aps";
		saveStateToFile(new File(dir, name));
	}

	public void saveStateToFile(File file) {
		OutputStream os = null;
		try {
			os = new BufferedOutputStream(new FileOutputStream(file));
			Method method = plugin.getClass().getMethod("saveState", new Class[] { OutputStream.class });
			if (method != null)
				method.invoke(plugin, new Object[] { os });
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			if (os != null) {
				try {
					os.close();
				}
				catch (IOException iox) {
				}
			}
		}
	}

	public void destroy() {
		destroyPlugin();
		if (downloadJobs != null) {
			// for (Download d : downloadJobs)
			// d.cancel();
			downloadCancelled = true;
			downloadJobs.clear();
		}
	}

	private void destroyPlugin() {
		if (plugin != null) {
			try {
				plugin.stop();
			}
			catch (Exception e) {
				e.printStackTrace();
				setErrorMessage("Errors in stopping: " + e);
			}
			try {
				plugin.destroy();
			}
			catch (Exception e) {
				e.printStackTrace();
				setErrorMessage("Errors in destroying: " + e);
			}
		}
	}

	public void createPopupMenu() {

		popupMenu = new JPopupMenu();
		popupMenu.setInvoker(this);

		String s = Modeler.getInternationalText("TakeSnapshot");
		JMenuItem mi = new JMenuItem((s != null ? s : "Take a Snapshot") + "...");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				snapshot();
			}
		});
		popupMenu.add(mi);
		popupMenu.addSeparator();

		s = Modeler.getInternationalText("CustomizePlugin");
		final JMenuItem miCustom = new JMenuItem((s != null ? s : "Customize This Plugin") + "...");
		miCustom.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (maker == null) {
					maker = new PageJContainerMaker(PageJContainer.this);
				}
				else {
					maker.setJContainer(PageJContainer.this);
				}
				maker.invoke(page);
			}
		});
		popupMenu.add(miCustom);

		s = Modeler.getInternationalText("RemovePlugin");
		final JMenuItem miRemove = new JMenuItem(s != null ? s : "Remove This Plugin");
		miRemove.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				page.removeComponent(PageJContainer.this);
			}
		});
		popupMenu.add(miRemove);

		s = Modeler.getInternationalText("CopyPlugin");
		mi = new JMenuItem(s != null ? s : "Copy This Plugin");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				page.copyComponent(PageJContainer.this);
			}
		});
		popupMenu.add(mi);

		if (plugin != null) {
			JPopupMenu pp = null;
			try {
				pp = plugin.getPopupMenu();
			}
			catch (Throwable e) {
				e.printStackTrace();
			}
			if (pp != null) {
				popupMenu.addSeparator();
				int n = pp.getComponentCount();
				if (n > 0) {
					Component[] c = new Component[n];
					for (int i = 0; i < n; i++) {
						c[i] = pp.getComponent(i);
					}
					for (Component x : c)
						popupMenu.add(x);
				}
			}
		}

		popupMenu.pack();

		popupMenu.addPopupMenuListener(new PopupMenuListener() {
			public void popupMenuCanceled(PopupMenuEvent e) {
			}

			public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
				miCustom.setEnabled(isChangable());
				miRemove.setEnabled(isChangable());
			}

			public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
			}
		});

	}

	public String runScript(String script) {
		if (plugin == null)
			return "No plugin";
		if (scripter == null)
			scripter = new PluginScripter(this);
		return scripter.runScript(script);
	}

	public String runNativeScript(String script) {
		if (plugin == null)
			return "plugin not initiated";
		try {
			Method method = plugin.getClass().getMethod("runNativeScript", new Class[] { String.class });
			if (method != null)
				return (String) method.invoke(plugin, new Object[] { script });
		}
		catch (Exception e) {
			e.printStackTrace();
			return e.getMessage();
		}
		return "error";
	}

	void snapshot() {
		if (plugin == null)
			return;
		Method method;
		Object c = null;
		try {
			method = plugin.getClass().getMethod("getSnapshotComponent", (Class[]) null);
			if (method != null) {
				c = method.invoke(plugin, (Object[]) null);
			}
		}
		catch (Exception e1) {
			e1.printStackTrace();
			return;
		}
		if (c instanceof Component && ((Component) c).isShowing()) {
			SnapshotGallery.sharedInstance().takeSnapshot(page.getAddress(), c);
		}
	}

	public static PageJContainer create(Page page) {
		if (page == null)
			return null;
		PageJContainer pageJContainer = new PageJContainer();
		if (maker == null) {
			maker = new PageJContainerMaker(pageJContainer);
		}
		else {
			maker.setJContainer(pageJContainer);
		}
		maker.invoke(page);
		if (maker.cancel)
			return null;
		return pageJContainer;
	}

	public String toString() {
		StringBuffer sb = new StringBuffer(super.toString());
		if (codeBase != null)
			sb.append("<codebase>" + codeBase + "</codebase>");
		if (cachedFileNames != null)
			sb.append("<cachefile>" + cachedFileNames + "</cachefile>");
		return sb.toString();
	}

}