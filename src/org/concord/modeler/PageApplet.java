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

import java.applet.Applet;
import java.applet.AppletContext;
import java.applet.AppletStub;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

import javax.swing.JApplet;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import org.concord.modeler.text.Page;
import org.concord.modeler.util.FileUtilities;
import org.concord.modeler.util.SwingWorker;

public class PageApplet extends PagePlugin {

	Applet applet;
	private boolean cachingAllowed;
	private static PageAppletMaker maker;
	private AppletScripter scripter;

	public PageApplet() {
		super();
	}

	public PageApplet(PageApplet pageApplet, Page parent) {
		super(pageApplet, parent);
	}

	public void start() {

		if (jarName == null || jarName.isEmpty() || className == null) {
			setErrorMessage("No applet has been set.");
			return;
		}

		setInitMessage();

		// create URLs and cached files
		int n = jarName.size();
		final URL[] url = new URL[n];
		final File[] file = new File[n];
		if (page.isRemote()) {
			for (int i = 0; i < url.length; i++) {
				try {
					url[i] = new URL(FileUtilities.getCodeBase(page.getAddress()) + jarName.get(i));
				}
				catch (MalformedURLException e) {
					e.printStackTrace();
					setErrorMessage("Errors in forming URL: " + url[i]);
					return;
				}
				if (cachingAllowed) {
					try {
						file[i] = ConnectionManager.sharedInstance().shouldUpdate(url[i]);
						if (file[i] == null)
							file[i] = ConnectionManager.sharedInstance().cache(url[i]);
						// this thread can run for a while and it will set the checkupdate flag in the Cache
						// Manager to false when it is done. This will result in the cml file to be unable to
						// update. Hence, we should set the checkupdate flag to true afterwards.
						ConnectionManager.sharedInstance().setCheckUpdate(true);
						url[i] = file[i].toURI().toURL();
					}
					catch (IOException e) {
						e.printStackTrace();
						setErrorMessage("Errors in caching jar file: " + url[i]);
						return;
					}
				}
			}
		}
		else {
			for (int i = 0; i < url.length; i++) {
				file[i] = new File(FileUtilities.getCodeBase(page.getAddress()), jarName.get(i));
				try {
					url[i] = file[i].toURI().toURL();
				}
				catch (MalformedURLException e) {
					e.printStackTrace();
					setErrorMessage("Errors in forming URL: " + file[i]);
					return;
				}
			}
		}

		// load classes
		Class c = null;
		URLClassLoader loader = URLClassLoader.newInstance(url, Modeler.class.getClassLoader());
		try {
			if (className.endsWith(".class")) {
				c = loader.loadClass(FileUtilities.removeSuffix(className));
			}
			else {
				c = loader.loadClass(className);
			}
		}
		catch (Throwable e) {
			e.printStackTrace();
			setErrorMessage(e);
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

		// initialize
		if (o instanceof Applet) {
			applet = (Applet) o;
			applet.setStub(new AppletStub() {

				public void appletResize(int width, int height) {
				}

				public AppletContext getAppletContext() {
					return null;
				}

				public URL getCodeBase() {
					String s = FileUtilities.getCodeBase(url[0].toString());
					URL u = null;
					if (FileUtilities.isRemote(s)) {
						try {
							u = new URL(s);
						}
						catch (MalformedURLException e) {
							e.printStackTrace();
							return null;
						}
					}
					else {
						if (file[0] != null) {
							s = FileUtilities.getCodeBase(file[0].toString());
							u = ConnectionManager.sharedInstance().getRemoteCopy(s);
							if (u != null)
								return u;
						}
						try {
							u = new File(ModelerUtilities.convertURLToFilePath(s)).toURI().toURL();
						}
						catch (MalformedURLException e) {
							e.printStackTrace();
						}
						// javax.swing.JOptionPane.showMessageDialog(PageApplet.this, "codebase=" + u);
					}
					return u;
				}

				public URL getDocumentBase() {
					return getCodeBase();
				}

				public String getParameter(String name) {
					if (parameterMap != null)
						return parameterMap.get(name);
					return null;
				}

				public boolean isActive() {
					return true;
				}
			});
			EventQueue.invokeLater(new Runnable() {
				public void run() {
					initApplet();
				}
			});
		}
		else {
			setErrorMessage("Error: The main class does not implement " + Applet.class.getName());
		}

	}

	private void addPopupMouseListener() {
		applet.addMouseListener(popupMouseListener);
		if (implementMwService()) {
			Method method;
			Object c = null;
			try {
				method = applet.getClass().getMethod("getSnapshotComponent", (Class[]) null);
				if (method != null) {
					c = method.invoke(applet, (Object[]) null);
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
	}

	private void initApplet() {
		removeAll();
		if (applet == null)
			return;
		if (applet instanceof JApplet) {
			JApplet ja = (JApplet) applet;
			if (ja.getComponentCount() > 0) { // if there are components inside the applet,
				add(ja.getContentPane(), BorderLayout.CENTER);
			}
			else {
				add(ja, BorderLayout.CENTER); // surely as heavyweighted as JApplet
			}
		}
		else {
			add(applet, BorderLayout.CENTER); // surely as heavyweighted as Applet
		}
		applet.resize(getPreferredSize());
		applet.setPreferredSize(getPreferredSize()); // FIXME
		validate();
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				// put into a non-event-dispatching thread, because the thread assigned by a typical
				// web browser cannot be the event-dispatching thread (there are some methods that
				// do not like to be invoked from the event-dispatching thread, e.g.
				// EventQueue.invokeAndWait(Runnable r).
				SwingWorker worker = new SwingWorker("Applet Starter", Thread.MIN_PRIORITY + 1) {
					public Object construct() {
						try {
							applet.init();
							loadState();
							applet.start();
						}
						catch (Throwable e) {
							e.printStackTrace();
							setErrorMessage(e);
						}
						return null;
					}

					public void finished() {
						addPopupMouseListener();
					}
				};
				worker.start();
			}
		});
	}

	private boolean implementMwService() {
		if (applet == null)
			return false;
		Class[] clazz = applet.getClass().getInterfaces();
		if (clazz == null || clazz.length == 0)
			return false;
		for (Class i : clazz) {
			if (i.getName().equals(MwService.class.getName()))
				return true;
		}
		return false;
	}

	void loadState(InputStream is) throws Exception {
		if (applet == null)
			return;
		Method method = applet.getClass().getMethod("loadState", new Class[] { InputStream.class });
		if (method != null)
			method.invoke(applet, new Object[] { is });
	}

	public void loadState() {
		if (!implementMwService())
			return;
		super.loadState();
	}

	/**
	 * If the applet implements MWService, then try to save its state. Security note: Applets cannot access user's disk,
	 * but MW can. MW opens an OutputStream to a file and then passes it on to an applet that implements the MwService.
	 * The applet can then write to the OutputStream. This should be safe because an applet cannot write to any file
	 * other than those given by MW.
	 * 
	 * @param parent
	 *            the parent CML file.
	 */
	public void saveState(File parent) {
		if (parent == null)
			return;
		if (!implementMwService())
			return;
		String s = parent.toString();
		String dir = FileUtilities.getCodeBase(s);
		String name = FileUtilities.getFileName(s);
		name = FileUtilities.getPrefix(name) + "$applet$" + index + ".aps";
		saveStateToFile(new File(dir, name));
	}

	public void saveStateToFile(File file) {
		OutputStream os = null;
		try {
			os = new BufferedOutputStream(new FileOutputStream(file));
			Method method = applet.getClass().getMethod("saveState", new Class[] { OutputStream.class });
			if (method != null)
				method.invoke(applet, new Object[] { os });
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
		// page = null;
		if (applet != null) {
			applet.stop();
			applet.destroy();
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

		s = Modeler.getInternationalText("CustomizeApplet");
		final JMenuItem miCustom = new JMenuItem((s != null ? s : "Customize This Applet") + "...");
		miCustom.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (maker == null) {
					maker = new PageAppletMaker(PageApplet.this);
				}
				else {
					maker.setApplet(PageApplet.this);
				}
				maker.invoke(page);
			}
		});
		popupMenu.add(miCustom);

		s = Modeler.getInternationalText("RemoveApplet");
		final JMenuItem miRemove = new JMenuItem(s != null ? s : "Remove This Applet");
		miRemove.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				page.removeComponent(PageApplet.this);
			}
		});
		popupMenu.add(miRemove);

		s = Modeler.getInternationalText("CopyApplet");
		mi = new JMenuItem(s != null ? s : "Copy This Applet");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				page.copyComponent(PageApplet.this);
			}
		});
		popupMenu.add(mi);

		if (applet instanceof MwService) {
			JPopupMenu pp = null;
			try {
				pp = ((MwService) applet).getPopupMenu();
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
		if (scripter == null)
			scripter = new AppletScripter(this);
		return scripter.runScript(script);
	}

	public String runNativeScript(String script) {
		if (implementMwService()) {
			try {
				Method method = applet.getClass().getMethod("runNativeScript", new Class[] { String.class });
				if (method != null)
					return (String) method.invoke(applet, new Object[] { script });
			}
			catch (Exception e) {
				e.printStackTrace();
				return e.getMessage();
			}
		}
		return "error";
	}

	void snapshot() {
		boolean b = false;
		if (implementMwService()) {
			Method method;
			Object c = null;
			try {
				method = applet.getClass().getMethod("getSnapshotComponent", (Class[]) null);
				if (method != null) {
					c = method.invoke(applet, (Object[]) null);
				}
			}
			catch (Exception e1) {
				e1.printStackTrace();
				return;
			}
			if (c instanceof Component && ((Component) c).isShowing()) {
				SnapshotGallery.sharedInstance().takeSnapshot(page.getAddress(), c);
				b = true;
			}
		}
		if (!b) {
			SnapshotGallery.sharedInstance().takeSnapshot(page.getAddress(),
					applet.isShowing() ? applet : PageApplet.this);
		}
	}

	public void setCachingAllowed(boolean b) {
		cachingAllowed = b;
	}

	public boolean isCachingAllowed() {
		return cachingAllowed;
	}

	public static PageApplet create(Page page) {
		if (page == null)
			return null;
		PageApplet applet = new PageApplet();
		if (maker == null) {
			maker = new PageAppletMaker(applet);
		}
		else {
			maker.setApplet(applet);
		}
		maker.invoke(page);
		if (maker.cancel)
			return null;
		return applet;
	}

	public String toString() {
		String s = super.toString();
		if (cachingAllowed)
			s += "<caching>true</caching>";
		return s;
	}

}