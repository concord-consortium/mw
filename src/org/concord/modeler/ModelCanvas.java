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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.SystemColor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import org.concord.modeler.text.Page;
import org.concord.modeler.text.XMLCharacterEncoder;
import org.concord.modeler.util.FileUtilities;
import org.concord.mw2d.MDView;
import org.concord.mw2d.ui.AtomContainer;
import org.concord.mw2d.ui.MDContainer;

public class ModelCanvas extends JComponent implements Embeddable, Scriptable, Engine {

	private Page page;
	private MDContainer container;
	private String name;
	private String url;
	private String resourceAddress;
	private boolean marked;
	private boolean used;
	private boolean changable = true;
	private int index;
	private String uid;
	private Object[] toolBarButtons;
	private final Object lock = new Object();
	private static Border defaultBorder = BorderFactory.createRaisedBevelBorder();
	private static Border emptyBorder = BorderFactory.createEmptyBorder();
	private static Border innerBorder = BorderFactory.createLoweredBevelBorder();
	private static Border markedBorder = BorderFactory.createLineBorder(SystemColor.textHighlight, 2);

	public ModelCanvas(MDContainer c) {

		if (c == null)
			throw new IllegalArgumentException("null container input");
		setMaximumSize(new Dimension(406, 311));
		container = c;
		name = c.getRepresentationName();
		setLayout(new BorderLayout());
		setBorder(defaultBorder);
		add(container, BorderLayout.CENTER);
		addMenuBar();

		MDView view = container.getView();
		view.setAncestor(this);

		JPopupMenu pm = view.getDefaultPopupMenu();

		if (pm != null) {

			pm.addSeparator();

			String s = Modeler.getInternationalText("ShowMenuBar");
			final JCheckBoxMenuItem miMenuBar = new JCheckBoxMenuItem(s != null ? s : "Show Menu Bar");
			miMenuBar.setSelected(true);
			miMenuBar.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent e) {
					switch (e.getStateChange()) {
					case ItemEvent.SELECTED:
						addMenuBar();
						break;
					case ItemEvent.DESELECTED:
						removeMenuBar();
						break;
					}
					if (page.getSaveReminder() != null)
						page.getSaveReminder().setChanged(true);
				}
			});
			pm.add(miMenuBar);

			s = Modeler.getInternationalText("ShowBottomBar");
			final JCheckBoxMenuItem miBottomBar = new JCheckBoxMenuItem(s != null ? s : "Show Bottom Bar");
			miBottomBar.setSelected(true);
			miBottomBar.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent e) {
					switch (e.getStateChange()) {
					case ItemEvent.SELECTED:
						container.addBottomBar();
						break;
					case ItemEvent.DESELECTED:
						container.removeBottomBar();
						break;
					}
					if (page.getSaveReminder() != null)
						page.getSaveReminder().setChanged(true);
				}
			});
			pm.add(miBottomBar);

			s = Modeler.getInternationalText("RemoveToolBar");
			final JMenuItem miToolBar = new JMenuItem(s != null ? s : "Remove Tool Bar");
			miToolBar.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					if (container.hasToolbar()) {
						if (JOptionPane.showConfirmDialog(JOptionPane.getFrameForComponent(ModelCanvas.this),
								"Do you really want to remove the toolbar?", "Remove Toolbar",
								JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
							container.removeToolbar();
							if (page.getSaveReminder() != null)
								page.getSaveReminder().setChanged(true);
						}
					}
				}
			});
			pm.add(miToolBar);

			s = Modeler.getInternationalText("ShowBorder");
			final JMenuItem miBorder = new JCheckBoxMenuItem(s != null ? s : "Show Border");
			miBorder.setSelected(true);
			miBorder.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent e) {
					showBorder(e.getStateChange() == ItemEvent.SELECTED);
				}
			});
			pm.add(miBorder);

			s = Modeler.getInternationalText("RemoveThisModel");
			final JMenuItem miRemoveComponent = new JMenuItem(s != null ? s : "Remove This Component");
			miRemoveComponent.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					page.removeComponent(ModelCanvas.this);
				}
			});
			pm.add(miRemoveComponent);

			pm.addPopupMenuListener(new PopupMenuListener() {
				public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
					boolean b = page.isEditable();
					miMenuBar.setSelected(hasMenuBar());
					miMenuBar.setEnabled(b);
					miToolBar.setEnabled(b);
					miRemoveComponent.setEnabled(b);
					miBottomBar.setEnabled(b);
					miBottomBar.setSelected(container.hasBottomBar());
					miBorder.setEnabled(b);
					miBorder.setSelected(getBorder().equals(defaultBorder));
				}

				public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
				}

				public void popupMenuCanceled(PopupMenuEvent e) {
				}
			});

		}

		pm = container.getDefaultPopupMenu();
		if (pm != null) {

			String s = Modeler.getInternationalText("RemoveToolBar");
			final JMenuItem miToolBar = new JMenuItem(s != null ? s : "Remove Tool Bar");
			miToolBar.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					if (container.hasToolbar()) {
						if (JOptionPane.showConfirmDialog(JOptionPane.getFrameForComponent(ModelCanvas.this),
								"Do you really want to remove the toolbar?", "Remove Toolbar",
								JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
							container.removeToolbar();
							if (page.getSaveReminder() != null)
								page.getSaveReminder().setChanged(true);
						}
					}
				}
			});
			pm.add(miToolBar);

			s = Modeler.getInternationalText("ShowMenuBar");
			final JCheckBoxMenuItem miMenuBar = new JCheckBoxMenuItem(s != null ? s : "Show Menu Bar");
			miMenuBar.setSelected(true);
			miMenuBar.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent e) {
					switch (e.getStateChange()) {
					case ItemEvent.SELECTED:
						addMenuBar();
						break;
					case ItemEvent.DESELECTED:
						removeMenuBar();
						break;
					}
					if (page.getSaveReminder() != null)
						page.getSaveReminder().setChanged(true);
				}
			});
			pm.add(miMenuBar);

			pm.addPopupMenuListener(new PopupMenuListener() {
				public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
					miMenuBar.setSelected(hasMenuBar());
					miToolBar.setSelected(hasToolBar());
				}

				public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
				}

				public void popupMenuCanceled(PopupMenuEvent e) {
				}
			});
		}

	}

	public String runScript(String script) {
		return container.getModel().runScript(script);
	}

	public String runScriptImmediately(String script) {
		return container.getModel().runScriptImmediately(script);
	}

	public Object get(String variable) {
		if (variable == null)
			throw new IllegalArgumentException("variable cannot be null.");
		return container.getModel().getProperty(variable.toLowerCase());
	}

	public void showBorder(boolean b) {
		setBorder(b ? defaultBorder : emptyBorder);
		container.getView().setBorder(b ? innerBorder : emptyBorder);
	}

	public void loadCurrentResource() {
		if (Page.isApplet()) {
			URL url = null;
			try {
				url = new URL(resourceAddress);
			}
			catch (MalformedURLException mue) {
				mue.printStackTrace();
				if (!FileUtilities.isRemote(resourceAddress)) {
					try {
						url = new File(resourceAddress).toURI().toURL();
					}
					catch (MalformedURLException e) {
						e.printStackTrace();
					}
				}
			}
			if (url != null)
				container.getModel().input(url);
		}
		else {
			if (!page.isRemote()) {
				container.getModel().input(new File(resourceAddress));
			}
			else {
				String fileName = FileUtilities.httpEncode(FileUtilities.getFileName(resourceAddress));
				URL baseURL = null, url = null;
				try {
					baseURL = new URL(FileUtilities.httpEncode(FileUtilities.getCodeBase(page.getAddress())));
					url = new URL(baseURL, fileName);
				}
				catch (MalformedURLException mue) {
					mue.printStackTrace();
				}
				container.getModel().input(url);
			}
		}
	}

	public void loadToolBarButtons() {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				if (container.getToolBar() != null)
					container.getToolBar().removeAll();
				if (container.getExpandMenu() != null)
					container.getExpandMenu().removeAll();
				container.removeToolbar();
				if (toolBarButtons == null)
					return;
				for (int i = 0; i < toolBarButtons.length; i++)
					container.addToolBarButton((String) toolBarButtons[i]);
			}
		});
	}

	public void setToolBarButtons(Object[] buttons) {
		toolBarButtons = buttons;
	}

	public void stopImmediately() {
		if (container == null)
			return;
		if (container.getModel() == null)
			return;
		container.getModel().stopImmediately();
	}

	public void reset() {
	}

	public JPopupMenu getPopupMenu() {
		return null;
	}

	public void createPopupMenu() {
	}

	/**
	 * Calling this method causes no effect, because a <code>ModelCanvas</code> is not destroyed; it will be reused.
	 * This method is used to destroy an embedded component that is created when the content is loaded but not needed
	 * any more. It is different from the ComponentPool.destroy() method that will be called when the hosting window
	 * shuts down.
	 */
	public void destroy() {
	}

	void recycle() {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				removeAll();
				destroyPopupMenu(container.getView().getDefaultPopupMenu());
				destroyPopupMenu(container.getDefaultPopupMenu());
				page = null;
				container = null;
			}
		});
	}

	private void destroyPopupMenu(final JPopupMenu pm) {
		if (pm == null)
			return;
		pm.setInvoker(null);
		PopupMenuListener[] pml = pm.getPopupMenuListeners();
		if (pml != null) {
			for (PopupMenuListener i : pml)
				pm.removePopupMenuListener(i);
		}
		Component c;
		AbstractButton b;
		for (int i = 0, n = pm.getComponentCount(); i < n; i++) {
			c = pm.getComponent(i);
			if (c instanceof AbstractButton) {
				b = (AbstractButton) c;
				b.setAction(null);
				ActionListener[] al = b.getActionListeners();
				if (al != null) {
					for (ActionListener k : al)
						b.removeActionListener(k);
				}
				ItemListener[] il = b.getItemListeners();
				if (il != null) {
					for (ItemListener k : il)
						b.removeItemListener(k);
				}
			}
		}
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				pm.removeAll();
			}
		});
	}

	public void setIndex(int i) {
		synchronized (lock) {
			index = i;
		}
	}

	public int getIndex() {
		synchronized (lock) {
			return index;
		}
	}

	public void setUid(String uid) {
		this.uid = uid;
	}

	public String getUid() {
		return uid;
	}

	public void addMenuBar() {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				container.createMenuBar();
				add(container.getMenuBar(), BorderLayout.NORTH);
			}
		});
	}

	public void removeMenuBar() {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				remove(container.getMenuBar());
			}
		});
	}

	private boolean hasMenuBar() {
		int n = getComponentCount();
		for (int i = 0; i < n; i++) {
			if (getComponent(i) == container.getMenuBar())
				return true;
		}
		return false;
	}

	private boolean hasToolBar() {
		int n = getComponentCount();
		for (int i = 0; i < n; i++) {
			if (getComponent(i) == container.getToolBar())
				return true;
		}
		return false;
	}

	public void setPage(Page p) {
		page = p;
		if (page != null)
			container.getModel().setExternalScriptCallback(page.getScriptCallback());
	}

	public Page getPage() {
		return page;
	}

	public void setChangable(boolean b) {
		synchronized (lock) {
			changable = b;
		}
	}

	public boolean isChangable() {
		synchronized (lock) {
			return changable;
		}
	}

	public void setMarked(boolean b) {
		synchronized (lock) {
			marked = b;
		}
		if (page != null) {
			if (!((LineBorder) markedBorder).getLineColor().equals(page.getSelectionColor()))
				markedBorder = BorderFactory.createLineBorder(page.getSelectionColor(), 2);
		}
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				setBorder(marked ? markedBorder : defaultBorder);
			}
		});
	}

	public boolean isMarked() {
		synchronized (lock) {
			return marked;
		}
	}

	public void setUsed(boolean b) {
		synchronized (lock) {
			used = b;
		}
	}

	public boolean isUsed() {
		synchronized (lock) {
			return used;
		}
	}

	public void setResourceAddress(String s) {
		synchronized (lock) {
			resourceAddress = s;
		}
	}

	public String getResourceAddress() {
		synchronized (lock) {
			return resourceAddress;
		}
	}

	public String getName() {
		return name;
	}

	public MDContainer getMdContainer() {
		return container;
	}

	public Component getPanel() {
		return getComponent(0);
	}

	public String getURL() {
		Model model = container.getModel();
		url = (String) model.getProperty("url");
		return url;
	}

	public String toString() {
		getURL();
		StringBuffer palette = new StringBuffer();
		JPanel tb = container.getToolBar();
		int n = tb != null ? tb.getComponentCount() : 0;
		if (tb != null && n > 0) {
			Action action = null;
			for (int i = 0; i < n; i++) {
				try {
					action = ((AbstractButton) tb.getComponent(i)).getAction();
				}
				catch (ClassCastException e) {
					e.printStackTrace();
				}
				if (action != null)
					palette.append("<button>" + action.getValue(Action.NAME) + "</button>");
			}
		}
		JPopupMenu pm = container.getExpandMenu();
		n = pm != null ? pm.getComponentCount() : 0;
		if (pm != null && n > 0) {
			Action action = null;
			for (int i = 0; i < n; i++) {
				try {
					action = ((AbstractButton) pm.getComponent(i)).getAction();
				}
				catch (ClassCastException e) {
					e.printStackTrace();
				}
				if (action != null)
					palette.append("<button>" + action.getValue(Action.NAME) + "</button>");
			}
		}
		StringBuffer dnaSettings = new StringBuffer();
		if (container instanceof AtomContainer) {
			if (((AtomContainer) container).hasDNAScroller()) {
				AtomContainer x = (AtomContainer) container;
				if (x.getDNA() != null) {
					dnaSettings.append("<dna>" + x.getDNA() + "</dna>");
				}
				else {
					dnaSettings.append("<dna></dna>");
				}
				dnaSettings.append("<dna_dt1>" + x.getTranscriptionTimeStep() + "</dna_dt1>");
				dnaSettings.append("<dna_dt2>" + x.getTranslationMDStep() + "</dna_dt2>");
				dnaSettings.append("<dna_context>" + x.getDNAContextEnabled() + "</dna_context>");
			}
		}
		StringBuffer sb = new StringBuffer("<class>" + getName() + "</class>\n");
		sb.append("<resource>" + XMLCharacterEncoder.encode(FileUtilities.getFileName(url)) + "</resource>");
		if (!hasMenuBar())
			sb.append("<menubar>false</menubar>");
		if (!container.hasBottomBar())
			sb.append("<statusbar>false</statusbar>");
		if (!getBorder().equals(defaultBorder))
			sb.append("<border>none</border>");
		if (container.getModel().getRecorderDisabled())
			sb.append("<recorderless>true</recorderless>\n");
		sb.append(dnaSettings);
		sb.append(palette);
		return sb.toString();
	}

}