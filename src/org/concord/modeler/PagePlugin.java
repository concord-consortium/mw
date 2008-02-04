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
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.event.MouseListener;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingConstants;

import org.concord.modeler.text.ExternalClient;
import org.concord.modeler.text.Page;
import org.concord.modeler.text.XMLCharacterEncoder;
import org.concord.modeler.ui.HyperlinkLabel;
import org.concord.modeler.util.FileUtilities;

/**
 * <p>
 * This Plugin Container is a Dependency-Injection Container (DIC) that allows a developer to write plugin for the
 * Molecular Workbench. There are two types of plugins. The first type is Java applets, preferrably written in Swing. In
 * this case, the DIC uses Java applet to initialize the applet and introspect the jar files to look for implementations
 * of the org.concord.modeler.MwService interface. If your code does not implement the interface, it will be just a
 * plain applet. If your code does implement the interface, then it becomes an MW applet. For the extra functionalities
 * beyond applets, please check the interface's documentation. Applets are assumed to be not trustworthy. The second
 * type is Java plugins, which must be signed with a security certificate.
 * </p>
 * 
 * <p>
 * The main advantage of using a trusted Java plugin is that the resource it uses can be cached for offline use and
 * faster reloading. An applet cannot do caching. Even if an applet's jar files can be cached, it simply is blocked from
 * writing and reading local files. Even if MW can be a proxy to cache the resources for an applet, it still cannot read
 * the cached files.
 * </p>
 * 
 * @see org.concord.modeler.MwService
 * @see org.concord.modeler.PluginService
 * @see org.concord.modeler.PageApplet
 * @see org.concord.modeler.PagePlugin
 * @author Charles Xies
 * 
 */

abstract class PagePlugin extends JPanel implements Embeddable, Scriptable {

	final static String PARAMETER_PATTERN = "(?i)(name[\\s&&[^\\r\\n]]*=[\\s&&[^\\r\\n]]*)|(value[\\s&&[^\\r\\n]]*=[\\s&&[^\\r\\n]]*)";

	String className;
	List<String> jarName;
	Page page;
	int index;
	boolean changable = true;
	boolean marked;
	String borderType;
	Map<String, String> parameterMap;
	Color defaultBackground, defaultForeground;
	JPopupMenu popupMenu;
	MouseListener popupMouseListener;

	public PagePlugin() {
		super();
		init();
	}

	public PagePlugin(PagePlugin pagePlugin, final Page parent) {
		this();
		setPage(parent);
		setBorderType(pagePlugin.getBorderType());
		setPreferredSize(pagePlugin.getPreferredSize());
		setBackground(pagePlugin.getBackground());
		setChangable(page.isEditable());
		setClassName(pagePlugin.className);
		if (pagePlugin.jarName != null)
			jarName = Collections.synchronizedList(new ArrayList<String>(pagePlugin.jarName));
		if (pagePlugin.parameterMap != null)
			parameterMap = new HashMap<String, String>(pagePlugin.parameterMap);
		Thread t = new Thread("Plugin starter") {
			public void run() {
				setPage(parent); // set again in case destroy() is called before this.
				PagePlugin.this.start();
			}
		};
		t.setPriority(Thread.MIN_PRIORITY);
		t.start();
	}

	public abstract void destroy();

	public abstract void start();

	String getResourceAddress() {
		String dir = FileUtilities.getCodeBase(page.getAddress());
		String name = FileUtilities.getFileName(page.getAddress());
		name = FileUtilities.getPrefix(name) + (this instanceof PageApplet ? "$applet$" : "$plugin$") + index + ".aps";
		return dir + name;
	}

	abstract void loadState(InputStream is) throws Exception;

	/**
	 * If the applet implements MWService, then try to load its state. Security note: Applets do not have permission to
	 * access disk files. But MW does. So MW reads a file that stores the state and then passes the InputStream on to an
	 * applet that implements the MwService. This should be safe because an applet can only get what MW gives.
	 */
	public void loadState() {
		InputStream is = null;
		String address = getResourceAddress();
		File file = null;
		if (page.isRemote()) {
			try {
				file = ConnectionManager.sharedInstance().shouldUpdate(address);
				if (file == null)
					file = ConnectionManager.sharedInstance().cache(address);
			}
			catch (IOException e) {
				e.printStackTrace();
				return;
			}
		}
		else {
			file = new File(address);
		}
		if (file != null) {
			loadState(file);
		}
		else if (page.isRemote()) {
			URL url = null;
			try {
				url = new URL(address);
			}
			catch (MalformedURLException e) {
				e.printStackTrace();
				return;
			}
			if (url == null)
				return;
			try {
				is = url.openStream();
				loadState(is);
			}
			catch (Exception e) {
				e.printStackTrace();
			}
			finally {
				try {
					is.close();
				}
				catch (IOException e) {
				}
			}
		}
	}

	private void loadState(File file) {
		if (!file.exists())
			return;
		InputStream is = null;
		try {
			is = new BufferedInputStream(new FileInputStream(file));
			loadState(is);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			if (is != null) {
				try {
					is.close();
				}
				catch (IOException e) {
				}
			}
		}
	}

	abstract public void saveState(File parent);

	public void setClassName(String s) {
		className = s;
	}

	public void addJarName(String s) {
		if (jarName == null)
			jarName = Collections.synchronizedList(new ArrayList<String>());
		jarName.add(s);
	}

	public void parseParameters(String input) {
		if (input == null)
			return;
		int lb = input.indexOf('{');
		int rb = input.indexOf('}');
		if (lb == -1 && rb == -1)
			return;
		if (parameterMap == null)
			parameterMap = new HashMap<String, String>();
		else parameterMap.clear();
		String[] str = null;
		int lq, rq;
		String t, s;
		while (lb != -1 && rb != -1) {
			str = input.substring(lb + 1, rb).split(PARAMETER_PATTERN);
			lq = str[1].indexOf('"');
			rq = str[1].lastIndexOf('"');
			if (lq != -1 && rq != -1 && lq != rq)
				t = str[1].substring(lq + 1, rq).trim();
			else t = str[1].trim();
			lq = str[2].indexOf('"');
			rq = str[2].lastIndexOf('"');
			if (lq != -1 && rq != -1 && lq != rq)
				s = str[2].substring(lq + 1, rq).trim();
			else s = str[2].trim();
			parameterMap.put(t, s);
			lb = input.indexOf('{', lb + 1);
			rb = input.indexOf('}', rb + 1);
		}
	}

	String parametersToString() {
		if (parameterMap != null && !parameterMap.isEmpty()) {
			String s = "";
			for (String name : parameterMap.keySet()) {
				String value = parameterMap.get(name);
				s += "{name=\"" + name + "\" value=\"" + value + "\"}";
			}
			return s;
		}
		return null;
	}

	void removeAllParameters() {
		if (parameterMap != null)
			parameterMap.clear();
	}

	// should a be replaced by b?
	static boolean shouldReplace(File a, File b) {
		return a.lastModified() != b.lastModified() || a.length() != b.length();
	}

	public void saveJars(File parent) {
		if (jarName == null || jarName.isEmpty())
			return;
		File file;
		String codeBase = FileUtilities.getCodeBase(page.getAddress());
		boolean remotePage = page.isRemote();
		synchronized (jarName) {
			for (String s : jarName) {
				file = new File(parent, s);
				if (remotePage) {
					File cached = ConnectionManager.sharedInstance().getLocalCopy(codeBase + s);
					if (cached != null && cached.exists()) {
						if (!file.exists() || shouldReplace(file, cached)) {
							FileUtilities.copy(cached, file);
							file.setLastModified(cached.lastModified());
						}
					}
					else {
						FileUtilities.copy(codeBase + s, file);
					}
				}
				else {
					File original = new File(codeBase + s);
					if (!file.exists() || shouldReplace(file, original)) {
						FileUtilities.copy(original, file);
						file.setLastModified(original.lastModified());
					}
				}
			}
		}
	}

	public void setIndex(int i) {
		index = i;
	}

	public int getIndex() {
		return index;
	}

	public void setMarked(boolean b) {
		marked = b;
		if (page == null)
			return;
		setBackground(b ? page.getSelectionColor() : defaultBackground);
		setForeground(b ? page.getSelectedTextColor() : defaultForeground);
	}

	public boolean isMarked() {
		return marked;
	}

	public String getBorderType() {
		return BorderManager.getBorder(this);
	}

	public void setBorderType(String s) {
		borderType = s;
		BorderManager.setBorder(this, s, page.getBackground());
	}

	public void setPage(Page p) {
		page = p;
	}

	public Page getPage() {
		return page;
	}

	public void setChangable(boolean b) {
		changable = b;
	}

	public boolean isChangable() {
		return changable;
	}

	public void setPreferredSize(Dimension dim) {
		if (dim == null)
			throw new IllegalArgumentException("null object");
		if (dim.width == 0 || dim.height == 0)
			throw new IllegalArgumentException("zero dimension");
		super.setPreferredSize(dim);
		super.setMinimumSize(dim);
		super.setMaximumSize(dim);
	}

	void init() {

		setLayout(new BorderLayout());
		setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		setPreferredSize(new Dimension(200, 200));

		if (defaultBackground == null)
			defaultBackground = getBackground();
		if (defaultForeground == null)
			defaultForeground = getForeground();
		popupMouseListener = new PopupMouseListener(this);
		addMouseListener(popupMouseListener);

	}

	public JPopupMenu getPopupMenu() {
		return popupMenu;
	}

	void setInitMessage() {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				removeAll();
				add(new JLabel("<html>Starting " + (PagePlugin.this instanceof PageApplet ? "Applet " : "Plugin ")
						+ className + "......</html>", SwingConstants.CENTER), BorderLayout.CENTER);
				validate();
				repaint();
			}
		});
	}

	void setErrorMessage(Throwable e) {
		setErrorMessage(e.toString());
	}

	void setErrorMessage(final String s) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				removeAll();
				StringBuffer b = new StringBuffer("<html><table width=100%><tr><td><b>"
						+ (PagePlugin.this instanceof PageApplet ? "Applet" : "Plugin")
						+ " Info</b><hr></td></tr><tr><td>");
				b.append(s);
				b.append("</td></tr>");
				if (parameterMap != null) {
					String errorMessage = parameterMap.get("errormessage");
					if (errorMessage != null) {
						b.append("<tr><td>");
						b.append(errorMessage);
						b.append("</td></tr>");
					}
					String errorUrl = parameterMap.get("errorurl");
					if (errorUrl != null) {
						if (FileUtilities.isRelative(errorUrl)) {
							errorUrl = page.getPathBase() + errorUrl;
						}
						String t = "<html><a href=\"" + errorUrl + "\">Click here for an alternative link.</a></html>";
						HyperlinkLabel label = new HyperlinkLabel(t);
						label.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
						label.setHorizontalAlignment(SwingConstants.CENTER);
						final String url = errorUrl;
						label.setAction(new Runnable() {
							public void run() {
								if (url.toLowerCase().endsWith(".cml")) {
									page.getNavigator().visitLocation(url);
								}
								else {
									openLink(url);
								}
							}
						});
						add(label, BorderLayout.SOUTH);
					}
				}
				b.append("</table></html>");
				add(new JLabel(b.toString()), BorderLayout.NORTH);
				validate();
				repaint();
			}
		});
	}

	static void openLink(String t) {
		if (t == null)
			return;
		String s = t.toLowerCase();
		if (s.endsWith(".htm") || s.endsWith(".html")) {
			ExternalClient.open(ExternalClient.HTML_CLIENT, t);
		}
		else if (s.endsWith(".swf")) {
			ExternalClient.open(ExternalClient.FLASH_CLIENT, t);
		}
		else if (s.endsWith(".rm") || s.endsWith(".ram") || s.endsWith(".avi")) {
			ExternalClient.open(ExternalClient.REALPLAYER_CLIENT, t);
		}
		else if (s.endsWith(".qt") || s.endsWith(".mov")) {
			ExternalClient.open(ExternalClient.QUICKTIME_CLIENT, t);
		}
		else if (s.endsWith(".mpg") || s.endsWith(".mpeg") || s.endsWith(".mp3")) {
			if (Modeler.isMac()) {
				ExternalClient.open(ExternalClient.QUICKTIME_CLIENT, t);
			}
			else {
				ExternalClient.open(ExternalClient.REALPLAYER_CLIENT, t);
			}
		}
		else if (s.endsWith(".jnlp")) {
			ExternalClient.open(ExternalClient.JNLP_CLIENT, t);
		}
		else {
			ExternalClient.open(ExternalClient.HTML_CLIENT, t);
		}
	}

	public String toString() {

		StringBuffer sb = new StringBuffer("<class>" + getClass().getName() + "</class>\n");

		if (jarName != null && !jarName.isEmpty()) {
			synchronized (jarName) {
				for (String i : jarName) {
					sb.append("<appletjar>" + i + "</appletjar>\n");
				}
			}
		}
		if (className != null)
			sb.append("<appletclass>" + className + "</appletclass>\n");

		sb.append("<resource>" + XMLCharacterEncoder.encode(FileUtilities.getFileName(getResourceAddress()))
				+ "</resource>");

		String s = parametersToString();
		if (s != null)
			sb.append("<parameter>" + XMLCharacterEncoder.encode(s) + "</parameter>\n");

		sb.append("<width>" + getWidth() + "</width><height>" + getHeight() + "</height>\n");
		if (!getBackground().equals(defaultBackground)) {
			sb.append("<bgcolor>" + Integer.toString(getBackground().getRGB(), 16) + "</bgcolor>\n");
		}
		if (borderType != null) {
			sb.append("<border>" + borderType + "</border>");
		}

		return sb.toString();

	}

}
