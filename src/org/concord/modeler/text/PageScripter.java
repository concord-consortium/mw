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
package org.concord.modeler.text;

import static java.util.regex.Pattern.compile;

import java.awt.EventQueue;
import java.awt.event.ActionListener;
import java.awt.event.ItemListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.AbstractButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.event.ChangeListener;

import org.concord.modeler.ComponentScripter;
import org.concord.modeler.ConnectionManager;
import org.concord.modeler.Embeddable;
import org.concord.modeler.Navigator;
import org.concord.modeler.script.Compiler;
import org.concord.modeler.ui.HTMLPane;
import org.concord.modeler.util.FileUtilities;

/**
 * @author Charles Xie
 * 
 */
class PageScripter extends ComponentScripter {

	private final static Pattern ENABLE_COMPONENT = compile("(^(?i)enablecomponent\\b){1}");
	private final static Pattern SELECT_COMPONENT = compile("(^(?i)selectcomponent\\b){1}");
	private final static Pattern SELECT_COMBOBOX = compile("(^(?i)selectcombobox\\b){1}");
	private final static Pattern IMPORT = compile("(^(?i)import\\b){1}");

	private Page page;

	PageScripter(Page page) {
		super(true);
		this.page = page;
		setName("Page Script Runner");
	}

	protected void evalCommand(String ci) {

		evaluateSingleKeyword(ci);

		// import (load when used as an applet)
		Matcher matcher = IMPORT.matcher(ci);
		if (matcher.find()) {
			String address = ci.substring(matcher.end()).trim();
			try {
				page.importPage(new URL(page.getCodeBase(), address));
			}
			catch (Exception e) {
				e.printStackTrace();
			}
			return;
		}

		// set
		matcher = Compiler.SET.matcher(ci);
		if (matcher.find()) {
			String s = ci.substring(matcher.end()).trim().toLowerCase();
			if (s.startsWith("frank")) {
				page.setFrank(!s.endsWith("false"));
			}
			return;
		}

		// load
		matcher = Compiler.LOAD.matcher(ci);
		if (matcher.find()) {
			String address = ci.substring(matcher.end()).trim();
			if (FileUtilities.isRelative(address))
				address = FileUtilities.getCodeBase(page.getAddress()) + address;
			if (page.getNavigator() != null) {
				page.getNavigator().visitLocation(address);
			}
			else {
				page.visit(address);
			}
			return;
		}

		// show message
		matcher = Compiler.MESSAGE.matcher(ci);
		if (matcher.find()) {
			String s = XMLCharacterDecoder.decode(ci.substring(matcher.end()).trim());
			String slc = s.toLowerCase();
			int a = slc.indexOf("<t>");
			int b = slc.indexOf("</t>");
			String info;
			if (a != -1 && b != -1) {
				info = s.substring(a, b + 4).trim();
				slc = info.toLowerCase();
				if (!slc.startsWith("<html>")) {
					info = "<html>" + info;
				}
				if (!slc.endsWith("</html>")) {
					info = info + "</html>";
				}
			}
			else {
				matcher = Compiler.HTML_EXTENSION.matcher(s);
				if (matcher.find()) {
					info = readText(s);
				}
				else {
					info = "Unknown text";
				}
			}
			if (EventQueue.isDispatchThread()) {
				showMessageDialog(info);
			}
			else {
				final String info2 = info;
				EventQueue.invokeLater(new Runnable() {
					public void run() {
						showMessageDialog(info2);
					}
				});
			}
			return;
		}

		// enable component
		matcher = ENABLE_COMPONENT.matcher(ci);
		if (matcher.find()) {
			String[] s = ci.substring(matcher.end()).trim().split("\\s+");
			if (s.length == 1) {
				int i = -1;
				try {
					i = Integer.parseInt(s[0]);
				}
				catch (NumberFormatException e) {
				}
				if (i >= 0) {
					final int i2 = i;
					EventQueue.invokeLater(new Runnable() {
						public void run() {
							Object o = page.getEmbeddedComponent(Embeddable.class, i2);
							if (o instanceof JComponent)
								((JComponent) o).setEnabled(true);
						}
					});
				}
				else {
					final String uid = s[0];
					EventQueue.invokeLater(new Runnable() {
						public void run() {
							Embeddable o = page.getEmbeddedComponent(uid);
							if (o instanceof JComponent)
								((JComponent) o).setEnabled(true);
						}
					});
				}
			}
			else if (s.length == 2) {
				int i = -1;
				try {
					i = Integer.parseInt(s[0]);
				}
				catch (NumberFormatException e) {
				}
				final boolean b = "true".equalsIgnoreCase(s[1]);
				if (i >= 0) {
					final int i2 = i;
					EventQueue.invokeLater(new Runnable() {
						public void run() {
							Object o = page.getEmbeddedComponent(Embeddable.class, i2);
							if (o instanceof JComponent)
								((JComponent) o).setEnabled(b);
						}
					});
				}
				else {
					final String uid = s[0];
					EventQueue.invokeLater(new Runnable() {
						public void run() {
							Embeddable o = page.getEmbeddedComponent(uid);
							if (o instanceof JComponent)
								((JComponent) o).setEnabled(b);
						}
					});
				}
			}
			return;
		}

		// select component
		matcher = SELECT_COMPONENT.matcher(ci);
		if (matcher.find()) {
			String[] s = ci.substring(matcher.end()).trim().split("\\s+");
			if (s.length >= 1) {
				int i = -1;
				try {
					i = Integer.parseInt(s[0]);
				}
				catch (NumberFormatException e) {
				}
				boolean a = true;
				if (s.length >= 2)
					a = "true".equalsIgnoreCase(s[1]);
				boolean b = false;
				if (s.length >= 3)
					b = "execute".equalsIgnoreCase(s[2]);
				final boolean a2 = a;
				final boolean b2 = b;
				if (i >= 0) {
					final int i2 = i;
					EventQueue.invokeLater(new Runnable() {
						public void run() {
							setComponentSelected(i2, a2, b2);
						}
					});
				}
				else {
					final String uid = s[0];
					EventQueue.invokeLater(new Runnable() {
						public void run() {
							setComponentSelected(uid, a2, b2);
						}
					});
				}
			}
			return;
		}

		// select index from a combo box
		matcher = SELECT_COMBOBOX.matcher(ci);
		if (matcher.find()) {
			String[] s = ci.substring(matcher.end()).trim().split("\\s+");
			if (s.length >= 2) {
				int i = -1, j = -1;
				try {
					i = Integer.parseInt(s[0]);
				}
				catch (NumberFormatException e) {
				}
				try {
					j = Integer.parseInt(s[1]);
				}
				catch (NumberFormatException e) {
					e.printStackTrace();
				}
				if (j >= 0) {
					final int j2 = j;
					boolean b = false;
					if (s.length >= 3)
						b = "execute".equalsIgnoreCase(s[2]);
					final boolean b2 = b;
					if (i >= 0) {
						final int i2 = i;
						EventQueue.invokeLater(new Runnable() {
							public void run() {
								Object o = page.getEmbeddedComponent(JComboBox.class, i2);
								if (o instanceof JComboBox) {
									JComboBox cb = (JComboBox) o;
									if (j2 < cb.getItemCount())
										setSelectedIndex(cb, j2, b2);
								}
							}
						});
					}
					else {
						final String uid = s[0];
						EventQueue.invokeLater(new Runnable() {
							public void run() {
								Embeddable o = page.getEmbeddedComponent(uid);
								if (o instanceof JComboBox) {
									JComboBox cb = (JComboBox) o;
									if (j2 < cb.getItemCount())
										setSelectedIndex(cb, j2, b2);
								}
							}
						});
					}
				}
			}
			return;
		}

	}

	/* set the selection state of a component without or with causing the listeners on it to fire */
	private void setComponentSelected(int i, boolean b, boolean execute) {
		List<Embeddable> list = page.getEmbeddedComponents();
		if (list == null || i >= list.size() || i < 0)
			return;
		Embeddable c = list.get(i);
		if (c instanceof AbstractButton) {
			AbstractButton ab = (AbstractButton) c;
			if (execute) {
				if (b)
					ab.doClick();
				else ab.setSelected(false);
			}
			else {
				ItemListener[] il = ab.getItemListeners();
				if (il != null)
					for (ItemListener x : il)
						ab.removeItemListener(x);
				ActionListener[] al = ab.getActionListeners();
				if (al != null)
					for (ActionListener x : al)
						ab.removeActionListener(x);
				ChangeListener[] cl = ab.getChangeListeners();
				if (cl != null)
					for (ChangeListener x : cl)
						ab.removeChangeListener(x);
				ab.setSelected(b);
				if (il != null)
					for (ItemListener x : il)
						ab.addItemListener(x);
				if (al != null)
					for (ActionListener x : al)
						ab.addActionListener(x);
				if (cl != null)
					for (ChangeListener x : cl)
						ab.addChangeListener(x);
			}
		}
	}

	/* set the selection state of a component without or with causing the listeners on it to fire */
	private void setComponentSelected(String uid, boolean b, boolean execute) {
		List<Embeddable> list = page.getEmbeddedComponents();
		if (list == null || list.isEmpty() || uid == null)
			return;
		for (Embeddable c : list) {
			if (uid.equals(c.getUid())) {
				if (c instanceof AbstractButton) {
					AbstractButton ab = (AbstractButton) c;
					if (execute) {
						if (b)
							ab.doClick();
						else ab.setSelected(false);
					}
					else {
						ItemListener[] il = ab.getItemListeners();
						if (il != null)
							for (ItemListener x : il)
								ab.removeItemListener(x);
						ActionListener[] al = ab.getActionListeners();
						if (al != null)
							for (ActionListener x : al)
								ab.removeActionListener(x);
						ChangeListener[] cl = ab.getChangeListeners();
						if (cl != null)
							for (ChangeListener x : cl)
								ab.removeChangeListener(x);
						ab.setSelected(b);
						if (il != null)
							for (ItemListener x : il)
								ab.addItemListener(x);
						if (al != null)
							for (ActionListener x : al)
								ab.addActionListener(x);
						if (cl != null)
							for (ChangeListener x : cl)
								ab.addChangeListener(x);
					}
				}
			}
		}
	}

	/* set the selected index of a combo box without or with causing the listeners on it to fire */
	private void setSelectedIndex(JComboBox cb, int iSelected, boolean execute) {
		if (execute) {
			cb.setSelectedIndex(iSelected);
		}
		else {
			ActionListener[] al = cb.getActionListeners();
			if (al != null)
				for (ActionListener x : al)
					cb.removeActionListener(x);
			ItemListener[] il = cb.getItemListeners();
			if (il != null)
				for (ItemListener x : il)
					cb.removeItemListener(x);
			cb.setSelectedIndex(iSelected);
			if (al != null)
				for (ActionListener x : al)
					cb.addActionListener(x);
			if (il != null)
				for (ItemListener x : il)
					cb.addItemListener(x);
		}
	}

	private void showMessageDialog(String message) {
		HTMLPane h = new HTMLPane("text/html", message);
		h.setEditable(false);
		h.setBase(page.getURL());
		JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(page), new JScrollPane(h));
	}

	private void evaluateSingleKeyword(String s) {

		// back
		if ("back".equalsIgnoreCase(s)) {
			if (EventQueue.isDispatchThread()) {
				page.getNavigator().getAction(Navigator.BACK).actionPerformed(null);
			}
			else {
				EventQueue.invokeLater(new Runnable() {
					public void run() {
						page.getNavigator().getAction(Navigator.BACK).actionPerformed(null);
					}
				});
			}
			return;
		}

		// forward
		if ("forward".equalsIgnoreCase(s)) {
			if (EventQueue.isDispatchThread()) {
				page.getNavigator().getAction(Navigator.FORWARD).actionPerformed(null);
			}
			else {
				EventQueue.invokeLater(new Runnable() {
					public void run() {
						page.getNavigator().getAction(Navigator.FORWARD).actionPerformed(null);
					}
				});
			}
			return;
		}

		// home
		if ("home".equalsIgnoreCase(s)) {
			if (EventQueue.isDispatchThread()) {
				page.getNavigator().getAction(Navigator.HOME).actionPerformed(null);
			}
			else {
				EventQueue.invokeLater(new Runnable() {
					public void run() {
						page.getNavigator().getAction(Navigator.HOME).actionPerformed(null);
					}
				});
			}
			return;
		}

		// reload the current page
		if ("reload".equalsIgnoreCase(s) || "refresh".equalsIgnoreCase(s)) {
			if (EventQueue.isDispatchThread()) {
				page.visit(page.getAddress());
			}
			else {
				EventQueue.invokeLater(new Runnable() {
					public void run() {
						page.visit(page.getAddress());
					}
				});
			}
			return;
		}

	}

	private String readText(String address) {
		if (address == null || address.equals("")) {
			return null;
		}
		if (FileUtilities.isRelative(address)) {
			address = page.getPathBase() + address;
		}
		InputStream is = null;
		if (FileUtilities.isRemote(address)) {
			URL url = null;
			try {
				url = new URL(address);
			}
			catch (MalformedURLException e) {
				e.printStackTrace();
			}
			if (url != null) {
				File file = null;
				if (ConnectionManager.sharedInstance().isCachingAllowed()) {
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
					try {
						is = url.openStream();
					}
					catch (IOException ioe) {
						ioe.printStackTrace();
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
		if (is == null) {
			final String errorAddress = address;
			EventQueue.invokeLater(new Runnable() {
				public void run() {
					JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(page), "File " + errorAddress
							+ " was not found.", "File not found", JOptionPane.ERROR_MESSAGE);
				}
			});
			return null;
		}
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
			}
		}
		return buffer.toString();
	}

}
