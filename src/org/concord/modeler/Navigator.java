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

import java.awt.Color;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Stack;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.GrayFilter;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.border.Border;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import org.concord.modeler.ui.TextComponentPopupMenu;

/**
 * This class supports general navigation functionality.
 * 
 * <p>
 * This class services those classes that implement the <code>Navigable</code> interface. You must specify a
 * <code>Navigable</code> type upon calling the constructor.
 * 
 * <p>
 * It provides a JComboBox, which you can deploy on your GUI.
 * 
 * @see org.concord.modeler.Navigable
 * @author Charles Xie
 */

public class Navigator {

	public final static String HOME = "Home";
	public final static String BACK = "Back";
	public final static String FORWARD = "Forward";

	private Navigable navigable;

	/* The stack to store entry history earlier than the current one */
	private Stack<String> left;

	/* The stack to store entry history later than the current one */
	private Stack<String> right;

	/* the combo box to hold a history of recently visited pages */
	private JComboBox comboBox;

	private Map<String, Action> actionMap;
	private Action backAction, forwardAction;
	private final int maxLoc = 20;
	private String homeDirectory;
	private String homePage;
	private List<String> deadLinks;
	private TextComponentPopupMenu popupMenu;
	private boolean activated;
	private ItemListener itemListener;
	private ActionListener actionListener;
	private static ImageIcon defaultIcon, ccIcon;
	private ImageIcon pageIcon;
	private static Image disabledImage1, disabledImage2;
	private List<Component> enabledComponentsWhenRemote, enabledComponentsWhenLocal;
	private Action homeAction;

	public Navigator(Navigable nav) {

		setNavigable(nav);

		if (defaultIcon == null)
			defaultIcon = new ImageIcon(Navigator.class.getResource("images/PageIcon.gif"));
		if (ccIcon == null)
			ccIcon = new ImageIcon(Navigator.class.getResource("images/ccdomain.gif"));
		pageIcon = ccIcon;

		left = new Stack<String>();
		right = new Stack<String>();

		comboBox = new JComboBox();
		comboBox.setEditable(true);
		// getTextField().setRequestFocusEnabled(false);
		if (System.getProperty("os.name").startsWith("Windows")) {
			comboBox.setBorder(BorderFactory.createCompoundBorder(comboBox.getBorder(), new Border() {
				public Insets getBorderInsets(Component c) {
					return new Insets(1, pageIcon.getIconWidth() + 5, 1, 1);
				}

				public boolean isBorderOpaque() {
					return true;
				}

				public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
					String s = peek();
					if (s != null)
						pageIcon = s.indexOf("concord.org") != -1 ? ccIcon : defaultIcon;
					if (comboBox.isEnabled()) {
						pageIcon.paintIcon(c, g, x + 2, y + (height - pageIcon.getIconHeight()) / 2);
					}
					else {
						g.setColor(comboBox.getEditor().getEditorComponent().getBackground());
						g.fillRect(x + 1, y + 1, pageIcon.getIconWidth() + 4, height - 2);
						if (pageIcon == ccIcon) {
							if (disabledImage1 == null)
								disabledImage1 = GrayFilter.createDisabledImage(ccIcon.getImage());
							g.drawImage(disabledImage1, x + 2, y + (height - pageIcon.getIconHeight()) / 2, c);
						}
						else if (pageIcon == defaultIcon) {
							if (disabledImage2 == null)
								disabledImage2 = GrayFilter.createDisabledImage(defaultIcon.getImage());
							g.drawImage(disabledImage2, x + 2, y + (height - pageIcon.getIconHeight()) / 2, c);
						}
					}
				}
			}));
		}
		else {
			getTextField().setBorder(new Border() {
				public Insets getBorderInsets(Component c) {
					return new Insets(1, pageIcon.getIconWidth() + 5, 1, 1);
				}

				public boolean isBorderOpaque() {
					return true;
				}

				public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
					g.setColor(Color.darkGray);
					g.drawLine(x, y, x + width, y);
					g.drawLine(x, y + height - 1, x + width, y + height - 1);
					g.drawLine(x, y, x, y + height);
					g.drawLine(x + width - 1, y, x + width - 1, y + height);
					String s = peek();
					if (s != null) {
						pageIcon = s.indexOf("concord.org") != -1 ? ccIcon : defaultIcon;
					}
					else {
						pageIcon = defaultIcon;
					}
					if (comboBox.isEnabled()) {
						pageIcon.paintIcon(c, g, x + 2, y + (height - pageIcon.getIconHeight()) / 2);
					}
					else {
						g.setColor(comboBox.getEditor().getEditorComponent().getBackground());
						g.fillRect(x + 1, y + 1, pageIcon.getIconWidth() + 4, height - 2);
						if (pageIcon == ccIcon) {
							if (disabledImage1 == null)
								disabledImage1 = GrayFilter.createDisabledImage(ccIcon.getImage());
							g.drawImage(disabledImage1, x + 2, y + (height - pageIcon.getIconHeight()) / 2, c);
						}
						else if (pageIcon == defaultIcon) {
							if (disabledImage2 == null)
								disabledImage2 = GrayFilter.createDisabledImage(defaultIcon.getImage());
							g.drawImage(disabledImage2, x + 2, y + (height - pageIcon.getIconHeight()) / 2, c);
						}
					}
				}
			});
		}

		actionMap = new HashMap<String, Action>();
		backAction = new BackAction();
		actionMap.put(BACK, backAction);
		forwardAction = new ForwardAction();
		actionMap.put(FORWARD, forwardAction);
		homeAction = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				visitLocation(getHomePage());
			}
		};
		homeAction.putValue(Action.NAME, HOME);
		homeAction.putValue(Action.SHORT_DESCRIPTION, "Display home page");
		homeAction.putValue(Action.SMALL_ICON, new ImageIcon(getClass().getResource("images/Home.gif")));
		homeAction
				.putValue(Action.ACCELERATOR_KEY, Modeler.isMac() ? KeyStroke.getKeyStroke(KeyEvent.VK_H,
						KeyEvent.META_MASK | KeyEvent.SHIFT_MASK) : KeyStroke.getKeyStroke(KeyEvent.VK_HOME,
						KeyEvent.ALT_MASK));
		actionMap.put(HOME, homeAction);

		homeDirectory = Modeler.getStaticRoot();
		Locale l = Locale.getDefault();
		if (l.equals(Locale.PRC))
			homePage = homeDirectory + "cn/index.cml";
		else if (l.equals(Locale.TAIWAN))
			homePage = homeDirectory + "tw/index.cml";
		else homePage = homeDirectory + "index.cml";

		deadLinks = new ArrayList<String>();

		input();

		popupMenu = new TextComponentPopupMenu(getTextField());
		popupMenu.setPasteAction(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				popupMenu.getTextComponent().paste();
			}
		});
		getTextField().addMouseListener(new MouseAdapter() {
			private boolean popupTrigger;

			public void mousePressed(MouseEvent e) {
				popupTrigger = e.isPopupTrigger();
			}

			public void mouseReleased(MouseEvent e) {
				if (popupTrigger || e.isPopupTrigger())
					popupMenu.show(comboBox, e.getX(), e.getY());
			}
		});

		actionListener = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String text = getTextField().getText();
				pushAddress(text);
				int itemCount = comboBox.getItemCount();
				for (int i = 0; i < itemCount; i++) {
					if (comboBox.getItemAt(i).toString().equals(text)) {
						navigable.visit(text);
						return;
					}
				}
				if (itemCount < maxLoc) {
					comboBox.insertItemAt(text, 0);
				}
				else {
					comboBox.removeItemAt(maxLoc - 1);
					comboBox.insertItemAt(text, 0);
				}
				navigable.visit(text);
				getTextField().setText(text);
			}
		};
		getTextField().addActionListener(actionListener);

		comboBox.addPopupMenuListener(new PopupMenuListener() {
			public void popupMenuCanceled(PopupMenuEvent e) {
				comboBox.removeItemListener(itemListener);
			}

			public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
				comboBox.removeItemListener(itemListener);
			}

			public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
				comboBox.addItemListener(itemListener);
			}
		});

		itemListener = new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (!activated)
					return;
				if (e.getStateChange() == ItemEvent.SELECTED) {
					pushAddress((String) comboBox.getSelectedItem());
					navigable.visit(getSelectedLocation());
				}
			}
		};

		if (Modeler.isMac()) {
			enabledComponentsWhenRemote = new ArrayList<Component>();
			enabledComponentsWhenLocal = new ArrayList<Component>();
		}

	}

	public void destroy() {
		if (homeAction == null)
			return; // already destroyed
		if (Modeler.isMac()) {
			enabledComponentsWhenRemote.clear();
			enabledComponentsWhenLocal.clear();
		}
		actionMap.clear();
		deadLinks.clear();
		popupMenu.destroy();
		PopupMenuListener[] pml = comboBox.getPopupMenuListeners();
		if (pml != null) {
			for (PopupMenuListener l : pml)
				comboBox.removePopupMenuListener(l);
		}
		MouseListener[] ml = getTextField().getMouseListeners();
		if (ml != null) {
			for (MouseListener l : ml)
				getTextField().removeMouseListener(l);
		}
		ActionListener[] al = getTextField().getActionListeners();
		if (al != null) {
			for (ActionListener l : al)
				getTextField().removeActionListener(l);
		}
		comboBox.setBorder(null);
		actionListener = null;
		itemListener = null;
		backAction = null;
		forwardAction = null;
		homeAction = null;
		navigable = null;
		popupMenu = null;
	}

	public void setNavigable(Navigable navigable) {
		this.navigable = navigable;
	}

	public Navigable getNavigable() {
		return navigable;
	}

	public JComboBox getComboBox() {
		return comboBox;
	}

	JTextField getTextField() {
		return (JTextField) comboBox.getEditor().getEditorComponent();
	}

	public Action getAction(String key) {
		return actionMap.get(key);
	}

	public String getSelectedLocation() {
		return peek();
	}

	public void visitLocation(final String uri) {
		activated = true;
		if (uri == null)
			return;
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				pushLocation(uri);
			}
		});
	}

	/**
	 * store a location in the navigator's history. This action will NOT cause visit to that URI.
	 */
	public void storeLocation(final String uri) {
		if (uri == null || uri.trim().equals(""))
			return;
		activated = false;
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				pushLocation(uri);
			}
		});
	}

	private void pushLocation(String str) {
		if (str == null || str.trim().equals(""))
			return;
		if (containsItem(str)) {
			comboBox.setSelectedItem(str);
			getTextField().setText(str);
			pushAddress(str);
			if (activated)
				navigable.visit(str);
			return;
		}
		insertItem(str);
		pushAddress(str);
		if (activated)
			navigable.visit(str);
	}

	private boolean containsItem(String str) {
		int itemCount = comboBox.getItemCount();
		for (int i = 0; i < itemCount; i++) {
			if (comboBox.getItemAt(i).toString().equals(str))
				return true;
		}
		return false;
	}

	/* NOTE: Insertion will NOT cause item state to change. */
	private void insertItem(String str) {
		int itemCount = comboBox.getItemCount();
		if (itemCount == 0) {
			comboBox.insertItemAt(str, 0);
		}
		else {
			if (itemCount < maxLoc) {
				comboBox.insertItemAt(str, 1);
			}
			else {
				comboBox.removeItemAt(maxLoc - 1);
				comboBox.insertItemAt(str, 1);
			}
		}
		getTextField().setText(str);
	}

	private void pushAddress(String address) {
		if (address.indexOf("?client=mw") != -1 && address.indexOf(".zip&timestamp") != -1)
			return;
		if (!left.empty()) {
			if (!address.equals(peek())) {
				push(address);
			}
		}
		else {
			push(address);
		}
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				((BackAction) backAction).updateState();
				((ForwardAction) forwardAction).updateState();
			}
		});
	}

	public void setHomePage(String s) {
		homePage = s;
	}

	public String getHomePage() {
		return homePage;
	}

	public void reportDeadLink(String url) {
		deadLinks.add(url);
	}

	/* push a new URL into the history stack */
	private void push(String url) {
		left.push(url);
		if (!right.empty()) {
			right.removeAllElements();
		}
	}

	/* Looks at the URL at the top of this left stack without removing it from the stack */
	private String peek() {
		if (left.empty())
			return null;
		return left.peek();
	}

	/* go forward to next visited page */
	private void next() {
		if (!right.empty()) {
			// if(navigable instanceof Page) ((Page)navigable).setRememberViewPosition(true);
			left.push(right.pop());
			visitLocation(peek());
		}
	}

	/* go back to previous visited page */
	private void previous() {
		if (!left.empty()) {
			// if(navigable instanceof Page) (Page)navigable).setRememberViewPosition(true);
			right.push(left.pop());
			visitLocation(peek());
		}
	}

	void setHomeDirectory(String s) {
		homeDirectory = s;
	}

	public String getHomeDirectory() {
		return homeDirectory;
	}

	void output() {

		Properties locationHistory = new Properties();
		int m = 0;
		for (int i = comboBox.getItemCount() - 1; i >= 1; i--) {
			if (!deadLinks.contains(comboBox.getItemAt(i))) {
				m++;
				locationHistory.setProperty(m + "", comboBox.getItemAt(i).toString());
			}
		}
		File f = new File(Initializer.sharedInstance().getPropertyDirectory(), "locationhistory.properties");
		FileOutputStream out = null;
		try {
			out = new FileOutputStream(f);
			locationHistory.store(out, "#");
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		finally {
			if (out != null)
				try {
					out.close();
				}
				catch (IOException e) {
				}
		}

	}

	void input() {

		Properties prop = new Properties();

		FileInputStream in = null;
		File f = null;
		File propertiesDir = Initializer.sharedInstance().getPropertyDirectory();

		if (propertiesDir != null && propertiesDir.exists()) {
			f = new File(propertiesDir, "locationhistory.properties");
			if (f.exists()) {
				try {
					in = new FileInputStream(f);
					prop.load(in);
				}
				catch (IOException e1) {
					e1.printStackTrace();
				}
				catch (AccessControlException e2) {
					e2.printStackTrace();
				}
				finally {
					if (in != null)
						try {
							in.close();
						}
						catch (IOException e) {
						}
				}
			}
		}

		if (prop.isEmpty())
			return;

		for (int i = prop.size(); i >= 1; i--) {
			comboBox.addItem(prop.getProperty(new Integer(i).toString()));
		}

	}

	class BackAction extends AbstractAction {

		public BackAction() {
			super(BACK);
			setEnabled(false);
			putValue(NAME, BACK);
			putValue(SHORT_DESCRIPTION, "Go to previous page");
			putValue(SMALL_ICON, new ImageIcon(getClass().getResource("images/Back.gif")));
			putValue(ACCELERATOR_KEY, Modeler.isMac() ? KeyStroke.getKeyStroke(KeyEvent.VK_OPEN_BRACKET,
					KeyEvent.META_MASK) : KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, KeyEvent.ALT_MASK));
		}

		public void actionPerformed(ActionEvent e) {
			previous();
			updateState();
			((ForwardAction) forwardAction).updateState();
		}

		public void setEnabled(boolean b) {
			if (left.size() <= 1) {
				super.setEnabled(false);
				return;
			}
			super.setEnabled(b);
		}

		protected void updateState() {
			setEnabled(left.size() > 1);
			putValue(SHORT_DESCRIPTION, left.size() <= 1 ? "Go back one page" : "Go to "
					+ left.elementAt(left.size() - 2));
			putValue("enabled", isEnabled());
		}

	}

	class ForwardAction extends AbstractAction {

		public ForwardAction() {
			super(FORWARD);
			setEnabled(false);
			putValue(NAME, FORWARD);
			putValue(SHORT_DESCRIPTION, "Go to next page");
			putValue(SMALL_ICON, new ImageIcon(getClass().getResource("images/Forward.gif")));
			putValue(ACCELERATOR_KEY, Modeler.isMac() ? KeyStroke.getKeyStroke(KeyEvent.VK_CLOSE_BRACKET,
					KeyEvent.META_MASK) : KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, KeyEvent.ALT_MASK));
		}

		public void actionPerformed(ActionEvent e) {
			next();
			updateState();
			((BackAction) backAction).updateState();
		}

		public void setEnabled(boolean b) {
			if (right.isEmpty()) {
				super.setEnabled(false);
				return;
			}
			super.setEnabled(b);
		}

		protected void updateState() {
			setEnabled(!right.isEmpty());
			putValue(SHORT_DESCRIPTION, right.empty() ? "Go forward one page" : "Go to " + right.lastElement());
			putValue("enabled", isEnabled());
		}

	}

}