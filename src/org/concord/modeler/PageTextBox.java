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

import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.util.List;
import java.util.Properties;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPasswordField;
import javax.swing.JPopupMenu;
import javax.swing.text.JTextComponent;

import org.concord.modeler.text.Page;
import org.concord.modeler.util.FileUtilities;

public class PageTextBox extends BasicPageTextBox {

	private boolean imageCached = true;
	private JPopupMenu popupMenu;
	private static PageTextBoxMaker maker;
	private MouseListener popupMouseListener;

	public PageTextBox() {
		super();
		setEditable(false);
		setBorder(BorderFactory.createEmptyBorder());
		setPreferredSize(new Dimension(400, 200));
		popupMouseListener = new PopupMouseListener(this);
		addMouseListener(popupMouseListener);
		if (defaultTextBackground == null)
			defaultTextBackground = textBody.getBackground();
		if (defaultTextForeground == null)
			defaultTextForeground = textBody.getForeground();
	}

	public PageTextBox(PageTextBox box, Page parent) {
		this();
		setUid(box.uid);
		setPage(parent);
		setText(box.getText());
		setOpaque(box.isOpaque());
		setBorderType(box.getBorderType());
		setBackground(box.textBackground == null ? box.getBackground() : box.textBackground);
		setWidthRelative(box.isWidthRelative());
		setHeightRelative(box.isHeightRelative());
		int w = box.getPreferredSize().width;
		int h = box.getPreferredSize().height;
		if (isWidthRelative()) {
			setWidthRatio(box.getWidthRatio());
			w = (int) (page.getWidth() * getWidthRatio());
		}
		if (isHeightRelative()) {
			setHeightRatio(box.getHeightRatio());
			h = (int) (page.getHeight() * getHeightRatio());
		}
		setPreferredSize(new Dimension(w, h));
		setChangable(page.isEditable());
		Object o = box.getClientProperty("border");
		if (o != null)
			putClientProperty("border", o);
		showBoundary(parent.isEditable());
	}

	public void load(String address, boolean sendCookie) {
		if (address == null || address.equals(""))
			return;
		if (FileUtilities.isRelative(address))
			address = FileUtilities.getCodeBase(page.getAddress()) + address;
		InputStream is = null;
		if (Page.isApplet() || FileUtilities.isRemote(address)) {
			String formMethod = null;
			int i = address.indexOf("&formmethodtemporary=");
			if (i > 0) {
				int j = address.indexOf("&", i + 1);
				if (j == -1)
					j = address.length();
				int k = address.indexOf("=", i + 1);
				formMethod = address.substring(k + 1, j).toUpperCase();
				address = address.substring(0, i) + address.substring(j);
			}
			URL url = null;
			try {
				url = new URL(address);
			}
			catch (MalformedURLException e) {
				e.printStackTrace();
			}
			if (url != null) {
				ConnectionManager.sharedInstance().setCheckUpdate(true);
				File file = null;
				if (ConnectionManager.sharedInstance().isCachingAllowed() && !ConnectionManager.isDynamicalContent(url)) {
					try {
						file = ConnectionManager.sharedInstance().shouldUpdate(url);
						if (file == null)
							file = ConnectionManager.sharedInstance().cache(url);
					}
					catch (IOException e) {
						e.printStackTrace();
					}
				}
				if (file == null) {
					URLConnection conn = ConnectionManager.getConnection(url);
					if (!(conn instanceof HttpURLConnection))
						return;
					if (sendCookie) {
						String userID = Modeler.user.getUserID();
						String password = Modeler.user.getPassword();
						if (userID != null && password != null) {
							conn.setRequestProperty("Cookie", "userid=" + userID + "; password=" + password);
						}
					}
					if (formMethod != null) {
						try {
							((HttpURLConnection) conn).setRequestMethod(formMethod);
						}
						catch (ProtocolException e) {
							e.printStackTrace();
						}
					}
					storeCookie(conn.getHeaderField("Set-Cookie"));
					if (!Page.isApplet())
						Initializer.sharedInstance().recognizeUser();
					try {
						// Is it wise to pass instructions to the client through HTTP headers?
						// This is not standard HTML behavior. But it is very useful for the
						// MW client to handle some simple sessions, such as "should it go back
						// to the previous page with the same URL?".
						if (ConnectionManager.isDynamicalContent(page.getAddress())) {
							String action = conn.getHeaderField("action");
							if ("login".equals(action) || "logout".equals(action)) {
								EventQueue.invokeLater(new Runnable() {
									public void run() {
										String address = page.getAddress();
										int k = address.indexOf("?");
										String s = address.substring(0, k) + "?client=mw";
										if (page.getNavigator() != null) {
											page.getNavigator().visitLocation(s);
										}
										else {
											page.visit(s); // in case we just use the Page class in a dialog window
										}
									}
								});
								return;
							}
							else if ("back".equals(action)) {
								if (page.getNavigator() != null)
									EventQueue.invokeLater(new Runnable() {
										public void run() {
											page.getNavigator().getAction(Navigator.BACK).actionPerformed(null);
										}
									});
								return;
							}
						}
						is = conn.getInputStream();
					}
					catch (final IOException ioe) {
						ioe.printStackTrace();
						final String a = address;
						EventQueue.invokeLater(new Runnable() {
							public void run() {
								setText("<html><body face=Verdana><h2><font color=red>&nbsp;Error: " + a
										+ " cannot be opened.</font></h2><p>&nbsp;Caused by " + ioe + "</body></html>");
							}
						});
					}
				}
				else {
					try {
						is = new FileInputStream(file);
					}
					catch (IOException ioe) {
						ioe.printStackTrace();
					}
				}
			}
		}
		else {
			try {
				is = new FileInputStream(new File(address));
			}
			catch (IOException ioe) {
				ioe.printStackTrace();
			}
		}
		if (is != null) {
			StringBuffer buffer = new StringBuffer();
			byte[] b = new byte[1024];
			int n = -1;
			try {
				while ((n = is.read(b)) != -1) {
					buffer.append(new String(b, 0, n));
				}
			}
			catch (IOException ioe) {
				ioe.printStackTrace();
			}
			finally {
				try {
					is.close();
				}
				catch (IOException e) {
					e.printStackTrace();
				}
			}
			// remove the meta tag
			final String s;
			String s0 = buffer.toString();
			int i = s0.indexOf("<meta");
			if (i > 0) {
				int j = s0.indexOf(">", i);
				s = s0.substring(0, i) + s0.substring(j + 1);
			}
			else {
				s = buffer.toString();
			}
			EventQueue.invokeLater(new Runnable() {
				public void run() {
					setText(s.trim());
					setEmbeddedComponentAttributes();
					if (imageCached)
						cacheLinkedFiles(page.getPathBase());
					EventQueue.invokeLater(new Runnable() {
						public void run() {
							setViewPosition(0, 0);
							textBody.setCaretPosition(0);
						}
					});
				}
			});
		}
	}

	private boolean storeCookie(String cookie) {
		if (cookie == null)
			return false;
		if (cookie.startsWith("JSESSIONID")) // for now, do not store JSESSIONID
			return false;
		try {
			cookie = URLDecoder.decode(cookie, "UTF-8");
		}
		catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return false;
		}
		Properties p = new Properties();
		String[] s = cookie.split(";");
		String filename = null;
		for (String s1 : s) {
			int i = s1.indexOf("=");
			if (i >= 0) {
				String s2 = s1.substring(0, i).trim();
				if (filename == null)
					filename = s2;
				p.setProperty(s2, s1.substring(i + 1).trim());
			}
		}
		File f = new File(Initializer.sharedInstance().getPropertyDirectory(),
				(filename == null ? "unknown" : filename) + ".properties");
		FileOutputStream out = null;
		try {
			out = new FileOutputStream(f);
			p.store(out, "");
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		finally {
			if (out != null) {
				try {
					out.close();
				}
				catch (IOException e) {
				}
			}
		}
		return true;
	}

	public void setEmbeddedComponentAttributes() {
		super.setEmbeddedComponentAttributes();
		List<JComponent> list = getEmbeddedComponents();
		if (list == null || list.isEmpty()) {
			return;
		}
		if (index < 0)
			return;
		final String base = page.getAddress() + "#" + ModelerUtilities.getSortableString(index, 3) + "%"
				+ PageTextBox.class.getName() + ":";
		for (JComponent o : list) {
			if (o instanceof JTextComponent) {
				final JTextComponent t = (JTextComponent) o;
				if (t.getText() != null && !t.getText().trim().equals(""))
					continue;
				final String question = (String) t.getDocument().getProperty("question");
				final int n = list.indexOf(o);
				EventQueue.invokeLater(new Runnable() {
					public void run() {
						QuestionAndAnswer qa = UserData.sharedInstance().getData(base + n);
						if (qa != null && !QuestionAndAnswer.NO_ANSWER.equals(qa.getAnswer())) {
							t.setText(qa.getAnswer());
						}
					}
				});
				if (question != null) {
					t.addFocusListener(new FocusAdapter() {
						public void focusLost(FocusEvent e) {
							storeAnswer(n, question, t);
						}
					});
				}
				else {
					if (!(t instanceof JPasswordField)) {
						t.addFocusListener(new FocusAdapter() {
							public void focusLost(FocusEvent e) {
								storeAnswer(n, "input", t);
							}
						});
					}
				}
			}
		}
	}

	private void storeAnswer(int n, String question, JTextComponent t) {
		if (page == null)
			return;
		String key = page.getAddress() + "#" + ModelerUtilities.getSortableString(index, 3) + "%"
				+ PageTextBox.class.getName() + ":" + n;
		QuestionAndAnswer q = UserData.sharedInstance().getData(key);
		if (q != null) {
			if (t.getText() == null || t.getText().trim().equals("")) {
				q.setAnswer(QuestionAndAnswer.NO_ANSWER);
			}
			else {
				q.setAnswer(t.getText());
			}
		}
		else {
			q = new QuestionAndAnswer(question, t.getText());
			UserData.sharedInstance().putData(key, q);
		}
		q.setTimestamp(System.currentTimeMillis());
	}

	public void destroy() {
		super.destroy();
		if (maker != null)
			maker.setObject(null);
	}

	public JPopupMenu getPopupMenu() {
		return popupMenu;
	}

	public void createPopupMenu() {
		if (popupMenu != null)
			return;
		popupMenu = new JPopupMenu();
		popupMenu.setInvoker(textBody);
		String s = Modeler.getInternationalText("CustomizeTextBox");
		JMenuItem mi = new JMenuItem((s != null ? s : "Customize This Text Box") + "...");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (maker == null)
					maker = new PageTextBoxMaker(PageTextBox.this);
				else {
					maker.setObject(PageTextBox.this);
				}
				maker.invoke(page);
			}
		});
		popupMenu.add(mi);
		s = Modeler.getInternationalText("RemoveTextBox");
		mi = new JMenuItem(s != null ? s : "Remove This Text Box");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				page.removeComponent(PageTextBox.this);
			}
		});
		popupMenu.add(mi);
		s = Modeler.getInternationalText("CopyTextBox");
		mi = new JMenuItem(s != null ? s : "Copy This Text Box");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				page.copyComponent(PageTextBox.this);
			}
		});
		popupMenu.add(mi);
		popupMenu.pack();
	}

	public void setTransparent(boolean b) {
		setOpaque(!b);
	}

	public boolean isTransparent() {
		return !isOpaque();
	}

	public void setChangable(boolean b) {
		super.setChangable(b);
		if (b) {
			if (!isChangable()) {
				addMouseListener(popupMouseListener);
			}
		}
		else {
			if (isChangable()) {
				removeMouseListener(popupMouseListener);
			}
		}
	}

	public boolean isChangable() {
		MouseListener[] ml = getMouseListeners();
		if (ml == null)
			return false;
		for (MouseListener l : ml) {
			if (l == popupMouseListener)
				return true;
		}
		return false;
	}

	public void setImageCached(boolean b) {
		imageCached = b;
	}

	public boolean isImageCached() {
		return imageCached;
	}

	public static PageTextBox create(Page page) {
		if (page == null)
			return null;
		PageTextBox tb = new PageTextBox();
		if (maker == null) {
			maker = new PageTextBoxMaker(tb);
		}
		else {
			maker.setObject(tb);
		}
		maker.invoke(page);
		if (maker.cancel) {
			return null;
		}
		return tb;
	}

	public String toString() {
		StringBuffer sb = new StringBuffer("<class>" + getClass().getName() + "</class>\n");
		if (uid != null)
			sb.append("<uid>" + uid + "</uid>\n");
		sb.append("<width>" + (widthIsRelative ? (widthRatio > 1.0f ? 1 : widthRatio) : getWidth()) + "</width>\n");
		sb.append("<height>" + (heightIsRelative ? (heightRatio > 1.0f ? 1 : heightRatio) : getHeight())
				+ "</height>\n");
		sb.append("<title>" + encodeText() + "</title>\n");
		if (isOpaque()) {
			sb.append("<bgcolor>" + Integer.toString(getBackground().getRGB(), 16) + "</bgcolor>\n");
		}
		else {
			sb.append("<opaque>false</opaque>\n");
		}
		if (!getBorderType().equals(BorderManager.BORDER_TYPE[0]))
			sb.append("<border>" + getBorderType() + "</border>\n");
		return sb.toString();
	}

}
