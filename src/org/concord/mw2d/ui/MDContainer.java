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

package org.concord.mw2d.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.SystemColor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import org.concord.modeler.ModelerUtilities;
import org.concord.modeler.MovieSlider;
import org.concord.modeler.PageBarGraph;
import org.concord.modeler.PageXYGraph;
import org.concord.modeler.SlideMovie;
import org.concord.modeler.event.ModelListener;
import org.concord.modeler.process.Job;
import org.concord.modeler.ui.ColorArrayEvent;
import org.concord.modeler.ui.ColorArrayListener;
import org.concord.modeler.ui.ColorArrayPane;
import org.concord.modeler.ui.CustomBorderLayout;
import org.concord.modeler.ui.IconPool;
import org.concord.modeler.ui.PastableTextField;
import org.concord.modeler.util.ComponentPrinter;
import org.concord.mw2d.ActionStateEvent;
import org.concord.mw2d.ActionStateListener;
import org.concord.mw2d.AtomisticView;
import org.concord.mw2d.MDView;
import org.concord.mw2d.UserAction;
import org.concord.mw2d.models.MDModel;
import org.concord.mw2d.models.ModelComponent;
import org.concord.mw2d.models.Particle;

/**
 * <p>
 * This abstract class defines the specifications of a MVC container needed for a molecular dynamics activity. Ideally,
 * all activities related to molecular dynamics simulation should be derived from this base classs.
 * </p>
 * 
 * <p>
 * <b>Note for Pedagogica scripters:</b> To obtain the UI component of an action, call
 * <code>getActionUIDelegate(String actionName)</code>. Action name must be one of the following:
 * </p>
 * 
 * <ul>
 * Components associated with the recorder:
 * <ul>
 * <li><code>movie slider</code>
 * <li><code>reload</code>
 * <li><code>rewind</code>
 * <li><code>step back</code>
 * <li><code>step forward</code>
 * <li><code>pause</code>
 * <li><code>play/run</code>
 * </ul>
 * 
 * Components associated with the "run/stop/reset" control panel:
 * <ul>
 * <li><code>run</code>
 * <li><code>stop</code>
 * <li><code>reload2</code>
 * </ul>
 * 
 * </ul>
 * 
 * <p>
 * You can query a UI delegate component, but it may not be showing. Call <code>component.isShowing()
 </code> to check
 * if it is showing on the screen.
 * </p>
 * 
 * @author Charles Xie
 */

public abstract class MDContainer extends JComponent implements ActionStateListener {

	private static final byte FORWARD = 101;
	private static final byte BACK = 102;
	private static final byte BEGIN = 103;

	static Preferences prefs;
	static ResourceBundle bundle;
	private static boolean isUSLocale = Locale.getDefault().equals(Locale.US);

	JPanel moviePanel, runPanel;
	List<Component> enabledComponentsWhenEditable;
	PointHeaterDialog pointHeaterDialog;
	JPopupMenu buttonMenu;
	ButtonGroup toolBarButtonGroup;
	ToolBarDialog toolBarDialog;
	JPopupMenu defaultPopupMenu;
	ColorArrayPane markColorArrayPane, trajColorArrayPane;

	Map<String, List> actionCategory;
	Map<Object, Runnable> customizationAction = new HashMap<Object, Runnable>();
	Map<String, Component> actionRegistry = new HashMap<String, Component>();

	private JButton expandButton;
	private JPopupMenu expandMenu;
	private boolean isLoading;
	private boolean statusBarShown = true;

	static Icon toolBarHeaderIcon, beginIcon, leftIcon, rightIcon, removeTBIcon, expandArrowIcon, pageSetupIcon;

	Action resizeModelAction;

	public MDContainer() {
		if (bundle == null && !isUSLocale) {
			try {
				bundle = ResourceBundle.getBundle("org.concord.mw2d.ui.images.ContainerBundle", Locale.getDefault());
			}
			catch (MissingResourceException e) {
			}
		}
		loadImages();
		toolBarButtonGroup = new ButtonGroup();
		enabledComponentsWhenEditable = new ArrayList<Component>();
		createDefaultPopupMenu();
		resizeModelAction = new ResizeModelAction(this);
	}

	void loadImages() {
		if (toolBarHeaderIcon == null) {
			toolBarHeaderIcon = new ImageIcon(MDContainer.class.getResource("images/ToolBarHeaderBar.gif"));
			beginIcon = new ImageIcon(MDContainer.class.getResource("images/pan_begin.gif"));
			leftIcon = new ImageIcon(MDContainer.class.getResource("images/pan_left.gif"));
			rightIcon = new ImageIcon(MDContainer.class.getResource("images/pan_right.gif"));
			removeTBIcon = new ImageIcon(MDContainer.class.getResource("images/RemoveToolBar.gif"));
			expandArrowIcon = new ImageIcon(MDContainer.class.getResource("images/ExpandArrow.gif"));
			pageSetupIcon = new ImageIcon(MDContainer.class.getResource("images/PageSetup.gif"));
		}
	}

	public abstract String getRepresentationName();

	static String getInternationalText(String name) {
		if (bundle == null)
			return null;
		if (name == null)
			return null;
		if (isUSLocale)
			return null;
		String s = null;
		try {
			s = bundle.getString(name);
		}
		catch (MissingResourceException e) {
			s = null;
		}
		return s;
	}

	/**
	 * When the parent of this container is closed, destroy it to prevent memory leak.
	 */
	public void destroy() {
		getActionMap().clear();
		getInputMap().clear();
		getView().getActionMap().clear();
		getView().getInputMap().clear();
		actionRegistry.clear();
		actionCategory.clear();
		customizationAction.clear();
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				removeAll();
				if (moviePanel != null)
					moviePanel.removeAll();
				if (runPanel != null)
					runPanel.removeAll();
				destroyToolBar();
			}
		});
		pointHeaterDialog = null;
	}

	private void destroyToolBar() {
		getToolBar().removeAll();
		Object o;
		AbstractButton b;
		for (Enumeration e = toolBarButtonGroup.getElements(); e.hasMoreElements();) {
			o = e.nextElement();
			if (o instanceof AbstractButton) {
				b = (AbstractButton) o;
				b.setAction(null);
				ActionListener[] al = b.getActionListeners();
				if (al != null) {
					for (ActionListener l : al)
						b.removeActionListener(l);
				}
				ItemListener[] il = b.getItemListeners();
				if (il != null) {
					for (ItemListener l : il)
						b.removeItemListener(l);
				}
				MouseListener[] ml = b.getMouseListeners();
				if (ml != null) {
					for (MouseListener l : ml)
						b.removeMouseListener(l);
				}
			}
		}
	}

	public void setAuthorable(final boolean b) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				for (Component c : enabledComponentsWhenEditable)
					c.setEnabled(b);
			}
		});
	}

	public boolean isAuthorable() {
		return enabledComponentsWhenEditable.get(0).isEnabled();
	}

	public void setStatusBarShown(boolean b) {
		statusBarShown = b;
	}

	public boolean isStatusBarShown() {
		return statusBarShown;
	}

	public void actionStateChanged(ActionStateEvent e) {
		Object o = e.getCurrentState();
		if (o instanceof Short) {
			final AbstractButton b = getToolBarButton(UserAction.getName(((Short) o).shortValue()));
			if (b != null) {
				EventQueue.invokeLater(new Runnable() {
					public void run() {
						if (!b.isSelected())
							b.setSelected(true);
					}
				});
			}
		}
	}

	AbstractButton createButton(Action a) {
		JToggleButton b = new JToggleButton(a);
		b.setPreferredSize(ModelerUtilities.getSystemToolBarButtonSize());
		b.setHorizontalAlignment(SwingConstants.CENTER);
		b.setIcon((Icon) a.getValue(Action.SMALL_ICON));
		b.setText(null);
		b.setFocusPainted(false);
		b.setBorderPainted(true);
		b.addMouseListener(new MouseAdapter() {
			private boolean popupTrigger;

			public void mousePressed(MouseEvent e) {
				popupTrigger = e.isPopupTrigger();
			}

			public void mouseReleased(MouseEvent e) {
				if (popupTrigger || e.isPopupTrigger()) {
					if (buttonMenu == null)
						createButtonMenu();
					AbstractButton c = (AbstractButton) e.getSource();
					JMenuItem m = (JMenuItem) buttonMenu.getComponent(3);
					m.setIcon(new RemovalIcon(c.getIcon()));
					if (!isButtonInExpandMenu(c)) {
						buttonMenu.show(c, 5, 5);
					}
					else {
						buttonMenu.show(getToolBar(), getToolBar().getWidth() - 5, 5);
						buttonMenu.setInvoker(c);
					}
				}
			}
		});
		return b;
	}

	Map getActionCategories() {
		return actionCategory;
	}

	Map getCustomizationAction() {
		return customizationAction;
	}

	public static void setPreferences(Preferences preferences) {
		prefs = preferences;
	}

	void createButtonMenu() {

		buttonMenu = new JPopupMenu();

		String s = getInternationalText("MoveButtonForward");
		JMenuItem mi = new JMenuItem(s != null ? s : "Move this Button Forward", leftIcon);
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				moveButton((AbstractButton) buttonMenu.getInvoker(), FORWARD);
				getModel().notifyChange();
			}
		});
		buttonMenu.add(mi);
		// mi.setEnabled(isAuthorable());
		enabledComponentsWhenEditable.add(mi);

		s = getInternationalText("MoveButtonBack");
		mi = new JMenuItem(s != null ? s : "Move this Button Back", rightIcon);
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				moveButton((AbstractButton) buttonMenu.getInvoker(), BACK);
				getModel().notifyChange();
			}
		});
		buttonMenu.add(mi);
		// mi.setEnabled(isAuthorable());
		enabledComponentsWhenEditable.add(mi);

		s = getInternationalText("MoveToBeginningPosition");
		mi = new JMenuItem(s != null ? s : "Move to the Beginning Position", beginIcon);
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				moveButton((AbstractButton) buttonMenu.getInvoker(), BEGIN);
				getModel().notifyChange();
			}
		});
		buttonMenu.add(mi);
		// mi.setEnabled(isAuthorable());
		enabledComponentsWhenEditable.add(mi);

		s = getInternationalText("RemoveButton");
		mi = new JMenuItem(s != null ? s : "Remove this Button");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String s = getInternationalText("DoYouReallyWantToRemoveButton");
				String t = getInternationalText("RemoveButton");
				if (JOptionPane.showConfirmDialog(MDContainer.this, s != null ? s
						: "Do you really want to remove this button from the toolbar?", t != null ? t
						: "Remove Toolbar Button", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
					Component invoker = buttonMenu.getInvoker();
					if (invoker instanceof AbstractButton) {
						removeToolBarButton((AbstractButton) invoker);
						getModel().notifyChange();
					}
				}
			}
		});
		buttonMenu.add(mi);
		// mi.setEnabled(isAuthorable());
		enabledComponentsWhenEditable.add(mi);

		s = getInternationalText("RemoveEntireToolBar");
		mi = new JMenuItem(s != null ? s : "Remove Entire Toolbar", removeTBIcon);
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String s = getInternationalText("DoYouReallyWantToRemoveToolBar");
				String t = getInternationalText("RemoveEntireToolBar");
				if (JOptionPane.showConfirmDialog(MDContainer.this, s != null ? s
						: "Do you really want to remove the toolbar?", t != null ? t : "Remove Toolbar",
						JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
					removeToolbar();
					getModel().notifyChange();
				}
			}
		});
		buttonMenu.add(mi);
		// mi.setEnabled(isAuthorable());
		enabledComponentsWhenEditable.add(mi);

		buttonMenu.pack();

	}

	public void setLoading(boolean b) {
		isLoading = b;
	}

	public boolean recorderEnabled() {
		int n = getComponentCount();
		for (int i = 0; i < n; i++) {
			if (getComponent(i) == moviePanel)
				return true;
		}
		return false;
	}

	/**
	 * Caution: This method must be called in the event thread. *
	 * 
	 * @throws RuntimeException
	 *             if this method is not called in the event thread.
	 */
	public int enableRecorder(boolean b) {
		if (!EventQueue.isDispatchThread())
			throw new RuntimeException("must be called in the event thread.");
		List list = getModel().getModelListeners();
		if (!b) {
			if (getModel().hasEmbeddedMovie()) {
				boolean hasGraphs = false;
				if (list != null && !list.isEmpty()) {
					ModelListener ml = null;
					for (Iterator it = list.iterator(); it.hasNext();) {
						ml = (ModelListener) it.next();
						if ((ml instanceof PageXYGraph) || (ml instanceof PageBarGraph)) {
							hasGraphs = true;
							break;
						}
					}
				}
				if (!isLoading) {
					if (JOptionPane.showConfirmDialog(JOptionPane.getFrameForComponent(MDContainer.this),
							"Disabling recorder will cause erasing data already\nrecorded in the tape. "
									+ (hasGraphs ? "This will also affect graphs on the page." : "")
									+ "Do you want to continue?", "Warning", JOptionPane.YES_NO_OPTION,
							JOptionPane.WARNING_MESSAGE) == JOptionPane.NO_OPTION)
						return JOptionPane.NO_OPTION;
				}
			}
		}
		getModel().stopImmediately();
		if (b) {
			/* IMPORTANT!!! we should turn the recorder on */
			if (!getModel().hasEmbeddedMovie()) {
				getModel().activateEmbeddedMovie(true);
				/*
				 * activate embedded movie will automatically turn the recorder on at the end of array initialization
				 */
			}
			else {
				/*
				 * if there is no embedded movie, we should compulsorily turn on the recorder mode, otherwise the
				 * recording process will not be added to the task pool upon job initialization.
				 */
				getModel().setRecorderDisabled(false);
			}
			if (runPanel != null)
				remove(runPanel);
			if (statusBarShown) {
				add(moviePanel, BorderLayout.SOUTH);
				validate();
				if (getView().getAncestor() != null)
					getView().getAncestor().validate();
			}
		}
		else {
			getModel().activateEmbeddedMovie(false);
			remove(moviePanel);
			if (statusBarShown) {
				if (runPanel == null)
					createRunPanel();
				add(runPanel, BorderLayout.SOUTH);
				validate();
				if (getView().getAncestor() != null)
					getView().getAncestor().validate();
			}
		}
		return JOptionPane.YES_OPTION;
	}

	void createRunPanel() {
		runPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
		JButton button = new JButton(getView().getActionMap().get("Play"));
		String s = getInternationalText("RunButton");
		if (s != null)
			button.setText(s);
		runPanel.add(button);
		actionRegistry.put("run", button);
		button = new JButton(getView().getActionMap().get("Stop"));
		s = getInternationalText("StopButton");
		if (s != null)
			button.setText(s);
		runPanel.add(button);
		actionRegistry.put("stop", button);
		button = new JButton(getView().getActionMap().get("Reload"));
		button.setText("Reset");
		s = getInternationalText("ResetButton");
		if (s != null)
			button.setText(s);
		runPanel.add(button);
		actionRegistry.put("reload2", button);
		runPanel.addMouseListener(new MouseAdapter() {
			private boolean popupTrigger;

			public void mousePressed(MouseEvent e) {
				popupTrigger = e.isPopupTrigger();
			}

			public void mouseReleased(MouseEvent e) {
				if (popupTrigger || e.isPopupTrigger())
					defaultPopupMenu.show(runPanel, e.getX(), e.getY());
			}
		});
	}

	void createMoviePanel() {

		moviePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 4));
		moviePanel.setBorder(BorderFactory.createEmptyBorder(1, 3, 1, 1));
		moviePanel.addMouseListener(new MouseAdapter() {
			private boolean popupTrigger;

			public void mousePressed(MouseEvent e) {
				popupTrigger = e.isPopupTrigger();
			}

			public void mouseReleased(MouseEvent e) {
				if (popupTrigger || e.isPopupTrigger())
					defaultPopupMenu.show(moviePanel, e.getX(), e.getY());
			}
		});

		MovieSlider ms = ((SlideMovie) getModel().getMovie()).getMovieSlider();
		ms.setPreferredSize(new Dimension(130, 20));
		ms.setBorder(BorderFactory.createEmptyBorder());
		moviePanel.add(ms);
		moviePanel.add(new JLabel(toolBarHeaderIcon));
		actionRegistry.put("movie slider", ms);

		int m = System.getProperty("os.name").startsWith("Mac") ? 2 : 0;
		Insets margin = new Insets(m, m, m, m);

		JButton button = new JButton(getView().getActionMap().get("Reload"));
		button.setText(null);
		button.setHorizontalAlignment(SwingConstants.CENTER);
		button.setMargin(margin);
		button.setFocusPainted(false);
		moviePanel.add(button);
		actionRegistry.put("reload", button);

		button = new JButton(((SlideMovie) getModel().getMovie()).rewindMovie);
		button.setText(null);
		button.setHorizontalAlignment(SwingConstants.CENTER);
		button.setMargin(margin);
		button.setFocusPainted(false);
		moviePanel.add(button);
		actionRegistry.put("rewind", button);

		button = new JButton(((SlideMovie) getModel().getMovie()).stepBackMovie);
		button.setText(null);
		button.setHorizontalAlignment(SwingConstants.CENTER);
		button.setMargin(margin);
		button.setFocusPainted(false);
		moviePanel.add(button);
		actionRegistry.put("step back", button);

		button = new JButton(getView().getActionMap().get("Stop"));
		button.setText(null);
		button.setHorizontalAlignment(SwingConstants.CENTER);
		button.setMargin(margin);
		button.setFocusPainted(false);
		moviePanel.add(button);
		actionRegistry.put("pause", button);

		button = new JButton(((SlideMovie) getModel().getMovie()).stepForwardMovie);
		button.setText(null);
		button.setHorizontalAlignment(SwingConstants.CENTER);
		button.setMargin(margin);
		button.setFocusPainted(false);
		moviePanel.add(button);
		actionRegistry.put("step forward", button);

		button = new JButton(getView().getActionMap().get("Play"));
		button.setText(null);
		button.setHorizontalAlignment(SwingConstants.CENTER);
		button.setMargin(margin);
		button.setFocusPainted(false);
		moviePanel.add(button);
		actionRegistry.put("play/run", button);

	}

	public Component getActionUIDelegate(String actionName) {
		if (actionName == null)
			return null;
		Object o = actionRegistry.get(actionName.toLowerCase());
		if (o instanceof Component)
			return (Component) o;
		return null;
	}

	public abstract MDView getView();

	public abstract MDModel getModel();

	public abstract JMenuBar createMenuBar();

	public abstract JMenuBar getMenuBar();

	public abstract JPanel getToolBar();

	public void addBottomBar() {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				if (getModel().getRecorderDisabled()) {
					if (runPanel == null)
						createRunPanel();
					add(runPanel, BorderLayout.SOUTH);
				}
				else {
					if (moviePanel == null)
						createMoviePanel();
					add(moviePanel, BorderLayout.SOUTH);
				}
				validate();
			}
		});
	}

	public void removeBottomBar() {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				if (runPanel != null)
					remove(runPanel);
				if (moviePanel != null)
					remove(moviePanel);
				validate();
			}
		});
	}

	public boolean hasBottomBar() {
		int n = getComponentCount();
		Component c = null;
		for (int i = 0; i < n; i++) {
			c = getComponent(i);
			if (c == runPanel || c == moviePanel)
				return true;
		}
		return false;
	}

	public JComponent getBottomPanel() {
		if (!getModel().getRecorderDisabled())
			return moviePanel;
		if (runPanel == null)
			createRunPanel();
		return runPanel;
	}

	public JComponent getMoviePanel() {
		return moviePanel;
	}

	public JPopupMenu getExpandMenu() {
		return expandMenu;
	}

	public JPopupMenu getDefaultPopupMenu() {
		return defaultPopupMenu;
	}

	public abstract JPanel createToolBar();

	/* Caution: This method must be called in the event thread. */
	void showToolBarDialog() {
		if (!EventQueue.isDispatchThread())
			throw new RuntimeException("must be called in the event thread.");
		if (toolBarDialog == null)
			toolBarDialog = new ToolBarDialog(MDContainer.this);
		toolBarDialog.displayButtonList();
		toolBarDialog.setLocationRelativeTo(getView());
		toolBarDialog.setVisible(true);
	}

	private void createDefaultPopupMenu() {

		defaultPopupMenu = new JPopupMenu();

		JMenuItem mi = new JMenuItem("Customize Tool Bar");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				showToolBarDialog();
			}
		});
		defaultPopupMenu.add(mi);

		final JMenuItem recorderMI = new JMenuItem("Change Recording Interval");
		recorderMI.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				showTaskManager("Recording the simulation");
			}
		});
		defaultPopupMenu.add(recorderMI);

		defaultPopupMenu.pack();

		defaultPopupMenu.addPopupMenuListener(new PopupMenuListener() {
			public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
				recorderMI.setEnabled(!getModel().getRecorderDisabled());
			}

			public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
			}

			public void popupMenuCanceled(PopupMenuEvent e) {
			}
		});

	}

	private AbstractButton getToolBarButton(String name) {
		AbstractButton button;
		for (Enumeration e = toolBarButtonGroup.getElements(); e.hasMoreElements();) {
			button = (AbstractButton) e.nextElement();
			if (((String) button.getAction().getValue(Action.NAME)).equals(name))
				return button;
		}
		return null;
	}

	public ButtonGroup getToolBarButtonGroup() {
		return toolBarButtonGroup;
	}

	public void setMenuEnabled(final boolean b) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				JMenuBar mb = getMenuBar();
				if (mb == null)
					return;
				int count = mb.getMenuCount();
				for (int i = 0; i < count; i++)
					mb.getMenu(i).setEnabled(b);
			}
		});
	}

	/**
	 * Caution: This method must be called in the event thread.
	 * 
	 * @throws RuntimeException
	 *             if this method is not called in the event thread.
	 */
	public boolean removeToolbar() {
		if (!EventQueue.isDispatchThread())
			throw new RuntimeException("must be called by the event thread.");
		if (!hasToolbar())
			return false;
		remove(getToolBar());
		getToolBar().removeAll();
		validate();
		if (expandMenu != null)
			expandMenu.removeAll();
		return true;
	}

	/**
	 * Caution: This method must be called in the event thread.
	 * 
	 * @throws RuntimeException
	 *             if this method is not called in the event thread.
	 */
	public void addAllToolBarButtons() {
		if (!EventQueue.isDispatchThread())
			throw new RuntimeException("must be called by the event thread.");
		addToolbar();
		for (List<AbstractButton> list : actionCategory.values()) {
			for (AbstractButton b : list) {
				addToolBarButton(b);
			}
		}
		getToolBar().repaint();
	}

	/**
	 * Caution: This method must be called in the event thread.
	 * 
	 * @throws RuntimeException
	 *             if this method is not called in the event thread.
	 */
	public boolean addToolbar() {
		if (!EventQueue.isDispatchThread())
			throw new RuntimeException("must be called by the event thread.");
		if (hasToolbar())
			return false;
		LayoutManager layout = getLayout();
		if (layout instanceof CustomBorderLayout) {
			((CustomBorderLayout) layout).setNorthComponent(getToolBar());
		}
		add(getToolBar(), BorderLayout.NORTH);
		validate();
		return true;
	}

	/* Caution: This method must be called in the event thread. */
	boolean hasToolbarButton(AbstractButton rb) {
		if (!EventQueue.isDispatchThread())
			throw new RuntimeException("must be called by the event thread.");
		if (rb == null)
			return false;
		int n = getToolBar().getComponentCount();
		if (n == 0)
			return false;
		Component c = null;
		for (int i = 0; i < n; i++) {
			c = getToolBar().getComponent(i);
			if (c == rb)
				return true;
		}
		if (expandMenu != null) {
			n = expandMenu.getComponentCount();
			if (n == 0)
				return false;
			for (int i = 0; i < n; i++) {
				c = expandMenu.getComponent(i);
				if (c == rb)
					return true;
			}
		}
		return false;
	}

	/**
	 * Caution: This method must be called in the event thread.
	 * 
	 * @throws RuntimeException
	 *             if this method is not called in the event thread.
	 */
	public boolean hasToolbar() {
		if (!EventQueue.isDispatchThread())
			throw new RuntimeException("must be called by the event thread.");
		int n = getComponentCount();
		Component c = null;
		for (int i = 0; i < n; i++) {
			c = getComponent(i);
			if (c == getToolBar())
				return true;
		}
		return false;
	}

	/**
	 * Caution: This method must be called in the event thread.
	 * 
	 * @throws RuntimeException
	 *             if this method is not called in the event thread.
	 */
	public void addDefaultToolBar() {
		if (!EventQueue.isDispatchThread())
			throw new RuntimeException("must be called by the event thread.");
		if (getToolBar() != null)
			getToolBar().removeAll();
		if (getExpandMenu() != null)
			getExpandMenu().removeAll();
		removeToolbar();
	}

	/* Caution: This method must be called in the event thread. */
	void addToolBarButton(AbstractButton button) {
		if (button == expandButton)
			return; // prevent accidently adding it
		if (!EventQueue.isDispatchThread())
			throw new RuntimeException("must be called by the event thread.");
		if (!hasToolbar())
			addToolbar();
		if (!hasToolbarButton(button)) {
			int n = getToolBar().getComponentCount();
			int w1 = button.getPreferredSize().width;
			int w2 = getPreferredSize().width;
			if (w2 - 13 < (n + 1) * (w1 + 1)) {
				if (expandButton == null) {
					expandButton = new JButton(expandArrowIcon);
					expandButton.setPreferredSize(new Dimension(14, 23));
					expandButton.setMaximumSize(new Dimension(14, 23));
					expandButton.setBorderPainted(UIManager.getLookAndFeel().getName().equals("CDE/Motif"));
					expandButton.setFocusPainted(false);
					expandButton.setToolTipText("More buttons");
					expandButton.setMargin(new Insets(0, 0, 0, 0));
					expandButton.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent e) {
							expandMenu.show(expandButton, 0, expandButton.getHeight());
						}
					});
					getToolBar().add(expandButton);
					getToolBar().validate();
				}
				else {
					if (!hasToolbarButton(expandButton))
						getToolBar().add(expandButton);
				}
				if (expandMenu == null)
					expandMenu = new JPopupMenu();
				int n1 = expandMenu.getComponentCount();
				if (n1 > 1) {
					int n2 = (int) Math.sqrt(n1) + 1;
					expandMenu.setLayout(new GridLayout(n2, n2, 1, 1));
				}
				expandMenu.add(button);
			}
			else {
				getToolBar().add(button);
			}
			if (!isLoading) {
				try {
					button.setEnabled(getModel().getJob().isStopped());
				}
				catch (Exception e) {
					// ignore
				}
				getView().firePropertyChange("Toolbar button added", false, true);
			}
		}
	}

	private int expandMenuHoldsButton(AbstractButton button) {
		if (expandMenu == null)
			return -1;
		Component[] c = expandMenu.getComponents();
		for (int i = 0; i < c.length; i++) {
			if (button == c[i])
				return i;
		}
		return -1;
	}

	private int getExpandButtonIndex() {
		if (expandButton == null)
			return -1;
		Component[] c = getToolBar().getComponents();
		for (int i = 0; i < c.length; i++) {
			if (expandButton == c[i])
				return i;
		}
		return -1;
	}

	/* Caution: This method must be called in the event thread. */
	void removeToolBarButton(AbstractButton button) {
		if (!EventQueue.isDispatchThread())
			throw new RuntimeException("must be called by the event thread.");
		if (expandMenu != null && expandMenuHoldsButton(button) != -1) {
			expandMenu.remove(button);
			if (expandMenu.getComponentCount() == 0) {
				getToolBar().remove(expandButton);
				getToolBar().validate();
				getToolBar().repaint();
			}
		}
		else {
			getToolBar().remove(button);
			int n = getToolBar().getComponentCount();
			int w1 = button.getPreferredSize().width;
			int w2 = getPreferredSize().width;
			if (w2 - 12 >= n * (w1 + 1)) {
				if (expandMenu != null && expandMenu.getComponentCount() > 0) {
					getToolBar().add(expandMenu.getComponent(0), getExpandButtonIndex());
					if (expandMenu.getComponentCount() == 0) {
						getToolBar().remove(expandButton);
					}
				}
			}
			if (getToolBar().getComponentCount() == 0) {
				removeToolbar();
			}
			else {
				getToolBar().validate();
				getToolBar().repaint();
			}
		}
		if (!isLoading) {
			getView().firePropertyChange("Toolbar button removed", false, true);
		}
	}

	/* Caution: This method must be called in the event thread. */
	private void moveButton(AbstractButton button, byte direction) {
		if (!EventQueue.isDispatchThread())
			throw new RuntimeException("must be called by the event thread.");
		int n = getToolBar().getComponentCount();
		int m = -1;
		for (int i = 0; i < n; i++) {
			if (getToolBar().getComponent(i) == button)
				m = i;
		}
		if (m == -1) {
			if (expandMenu == null)
				return;
			int k = expandMenuHoldsButton(button);
			if (k == -1)
				return;
			if (direction == FORWARD) {
				if (k > 0) {
					expandMenu.remove(button);
					expandMenu.add(button, k - 1);
				}
				else if (k == 0) {
					expandMenu.remove(button);
					Component c = getToolBar().getComponent(n - 2);
					getToolBar().remove(c);
					expandMenu.add(c, 0);
					getToolBar().add(button, n - 2);
				}
			}
			else if (direction == BACK) {
				if (k >= 0 && k < expandMenu.getComponentCount() - 1) {
					expandMenu.remove(button);
					expandMenu.add(button, k + 1);
				}
			}
			else if (direction == BEGIN) {
				expandMenu.remove(button);
				Component c = getToolBar().getComponent(n - 2);
				getToolBar().remove(c);
				expandMenu.add(c, 0);
				getToolBar().add(button, 0);
			}
			expandMenu.validate();
		}
		else {
			if (direction == FORWARD) {
				if (m > 0) {
					getToolBar().remove(button);
					getToolBar().add(button, m - 1);
				}
			}
			else if (direction == BACK) {
				int z = getToolBar().getComponent(n - 1) == expandButton ? n - 2 : n - 1;
				if (m >= 0 && m < z) {
					getToolBar().remove(button);
					getToolBar().add(button, m + 1);
				}
				else if (z == n - 2 && m == z) {
					if (expandMenu != null) {
						Component c = expandMenu.getComponent(0);
						if (c != null) {
							getToolBar().remove(button);
							getToolBar().add(c, z);
							expandMenu.add(button, 0);
						}
					}
				}
			}
			else if (direction == BEGIN) {
				getToolBar().remove(button);
				getToolBar().add(button, 0);
			}
			getToolBar().validate();
		}
	}

	/**
	 * Caution: This method must be called in the event thread.
	 * 
	 * @throws RuntimeException
	 *             if this method is not called in the event thread.
	 */
	public void addToolBarButton(String buttonName) {
		if (!EventQueue.isDispatchThread())
			throw new RuntimeException("must be called by the event thread.");
		AbstractButton b = getToolBarButton(buttonName);
		if (b != null)
			addToolBarButton(b);
		if (!isLoading) {
			getView().firePropertyChange("Toolbar button added", false, true);
		}
	}

	/**
	 * Caution: This method must be called in the event thread.
	 * 
	 * @throws RuntimeException
	 *             if this method is not called in the event thread.
	 */
	public void removeToolBarButton(String buttonName) {
		if (!EventQueue.isDispatchThread())
			throw new RuntimeException("must be called by the event thread.");
		AbstractButton b = getToolBarButton(buttonName);
		if (b != null)
			removeToolBarButton(b);
		if (!isLoading) {
			getView().firePropertyChange("Toolbar button removed", false, true);
		}
	}

	ItemListener createListenerForButton(final AbstractButton button) {
		return new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				switch (e.getStateChange()) {
				case ItemEvent.SELECTED:
					addToolBarButton(button);
					break;
				case ItemEvent.DESELECTED:
					removeToolBarButton(button);
					break;
				}
				getModel().notifyChange();
			}
		};
	}

	/* Caution: This method must be called in the event thread. */
	boolean isButtonInExpandMenu(AbstractButton b) {
		if (expandMenu == null)
			return false;
		if (!EventQueue.isDispatchThread())
			throw new RuntimeException("must be called by the event thread.");
		for (int i = 0; i < expandMenu.getComponentCount(); i++) {
			if (b == expandMenu.getComponent(i))
				return true;
		}
		return false;
	}

	/* Caution: This method must be called in the event thread. */
	void showTaskManager(String selectedTask) {
		if (!EventQueue.isDispatchThread())
			throw new RuntimeException("must be called in event thread.");
		Job job = getModel().getJob();
		if (job != null) {
			job.show(getView());
		}
		else {
			JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(MDContainer.this),
					"There is no task yet. Please run the model.", "No task assigned", JOptionPane.WARNING_MESSAGE);
		}
	}

	void showMessageWithPopupMenu(Component parent, String message) {
		final JPopupMenu popupMenu = new JPopupMenu();
		popupMenu.setBackground(SystemColor.info);
		popupMenu.setBorder(BorderFactory.createLineBorder(Color.black));
		JLabel descriptionLabel = new JLabel(message);
		descriptionLabel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
		popupMenu.add(descriptionLabel);
		popupMenu.pack();
		popupMenu.show(parent, 10, 10);
	}

	static void setMenuItemWithoutNotifyingListeners(final JMenuItem mi, final boolean selected) {
		if (mi == null)
			return;
		ItemListener[] il = mi.getItemListeners();
		if (il != null) {
			for (ItemListener x : il)
				mi.removeItemListener(x);
		}
		ActionListener[] al = mi.getActionListeners();
		if (al != null) {
			for (ActionListener x : al)
				mi.removeActionListener(x);
		}
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				mi.setSelected(selected);
			}
		});
		if (il != null) {
			for (ItemListener x : il)
				mi.addItemListener(x);
		}
		if (al != null) {
			for (ActionListener x : al)
				mi.addActionListener(x);
		}
	}

	JMenu createFileMenu() {

		String s = getInternationalText("File");
		JMenu menu = new JMenu(s != null ? s : "File");
		menu.setMnemonic(KeyEvent.VK_F);
		add(menu);

		s = getInternationalText("OpenLocation");
		JMenuItem menuItem = new JMenuItem((s != null ? s : "Open Model from Location") + "...", IconPool
				.getIcon("openweb"));
		menuItem.setMnemonic(KeyEvent.VK_L);
		menuItem.setAccelerator(System.getProperty("os.name").startsWith("Mac") ? KeyStroke.getKeyStroke(KeyEvent.VK_L,
				KeyEvent.META_MASK, true) : KeyStroke.getKeyStroke(KeyEvent.VK_L, KeyEvent.CTRL_MASK, true));
		menuItem.setToolTipText("Open a model using its URL");
		menuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				PastableTextField tf = new PastableTextField("http://");
				String inputURL = getInternationalText("PleaseInputURL");
				if (JOptionPane.showConfirmDialog(getView(), tf, inputURL != null ? inputURL : "Please input a URL:",
						JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
					final String s = tf.getText();
					if (s == null || s.trim().equals(""))
						return;
					if (!s.toLowerCase().endsWith("mml")) {
						JOptionPane.showMessageDialog(getView(), "File must be in MML format.", "Format error",
								JOptionPane.ERROR_MESSAGE);
						return;
					}
					Thread t = new Thread() {
						public void run() {
							URL url = null;
							try {
								url = new URL(s);
							}
							catch (MalformedURLException ee) {
							}
							getModel().input(url);
						}
					};
					t.setName("Model Inputter from a URL");
					t.setPriority(Thread.MIN_PRIORITY);
					t.start();
				}
			}
		});
		menu.add(menuItem);
		enabledComponentsWhenEditable.add(menuItem);

		menuItem = new JMenuItem(getView().getActionMap().get("Model Reader"));
		s = getInternationalText("OpenModel");
		if (s != null)
			menuItem.getAction().putValue("i18n", s);
		menuItem.setText((s != null ? s : "Open Model") + "...");
		menu.add(menuItem);
		enabledComponentsWhenEditable.add(menuItem);
		menu.addSeparator();

		menuItem = new JMenuItem(getView().getActionMap().get("Model Writer"));
		s = getInternationalText("SaveModel");
		if (s != null)
			menuItem.getAction().putValue("i18n", s);
		menuItem.setText((s != null ? s : "Save Model As") + "...");
		menu.add(menuItem);

		menuItem = new JMenuItem(getView().getActionMap().get("Screenshot"));
		s = getInternationalText("Screenshot");
		if (s != null)
			menuItem.getAction().putValue("i18n", s);
		menuItem.setText((s != null ? s : "Save Screenshot of Model") + "...");
		menu.add(menuItem);

		s = getInternationalText("SaveImageStream");
		menuItem = new JMenuItem((s != null ? s : "Save Image Stream") + "...", IconPool.getIcon("movie"));
		menuItem.setMnemonic(KeyEvent.VK_I);
		menuItem.setToolTipText("Produce a series of images from the simulation");
		menuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				getModel().exportSmilMovie();
			}
		});
		menu.add(menuItem);
		menu.addSeparator();

		menuItem = new JMenuItem(getView().getActionMap().get("Print"));
		s = getInternationalText("Print");
		menuItem.setText((s != null ? s : "Print") + "...");
		menu.add(menuItem);

		s = getInternationalText("PageSetup");
		menuItem = new JMenuItem((s != null ? s : "Page Setup") + "...", pageSetupIcon);
		menuItem.setMnemonic(KeyEvent.VK_U);
		menuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				((ComponentPrinter) getView().getActionMap().get("Print")).pageSetup();
			}
		});
		menu.add(menuItem);
		menu.addSeparator();

		menuItem = new JMenuItem(getView().getActionMap().get("Input Image"));
		s = getInternationalText("InputImage");
		if (s != null)
			menuItem.getAction().putValue("i18n", s);
		menuItem.setText((s != null ? s : "Input Image") + "...");
		menu.add(menuItem);
		enabledComponentsWhenEditable.add(menuItem);

		menuItem = new JMenuItem(getView().getActionMap().get("Input Text Box"));
		s = getInternationalText("InputTextBox");
		menuItem.setText((s != null ? s : "Input Text Box") + "...");
		menu.add(menuItem);
		enabledComponentsWhenEditable.add(menuItem);

		menuItem = new JMenuItem(getView().getActionMap().get("Properties"));
		s = getInternationalText("Properties");
		menuItem.setText((s != null ? s : "Properties") + "...");
		menu.add(menuItem);

		return menu;

	}

	JMenu createEditMenu() {

		final JMenuItem undoMI = new JMenuItem(getView().getActionMap().get("Undo"));
		final JMenuItem redoMI = new JMenuItem(getView().getActionMap().get("Redo"));

		final JMenuItem cutMI = new JMenuItem(getView().getActionMap().get("Cut"));
		String s = getInternationalText("Cut");
		if (s != null)
			cutMI.setText(s);

		final JMenuItem copyMI = new JMenuItem(getView().getActionMap().get("Copy"));
		s = getInternationalText("Copy");
		if (s != null)
			copyMI.setText(s);

		final JMenuItem pasteMI = new JMenuItem(getView().getActionMap().get("Paste"));
		s = getInternationalText("Paste");
		if (s != null)
			pasteMI.setText(s);

		final JMenuItem clearMI = new JMenuItem(getView().getActionMap().get("Clear"));
		s = getInternationalText("Clear");
		if (s != null)
			clearMI.setText(s);

		JMenu menu = new JMenu("Edit");
		s = getInternationalText("Edit");
		if (s != null)
			menu.setText(s);
		menu.setMnemonic(KeyEvent.VK_E);
		menu.getPopupMenu().addPopupMenuListener(new PopupMenuListener() {
			public void popupMenuCanceled(PopupMenuEvent e) {
			}

			public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
				boolean b = Math.abs(getModel().getMovie().getCurrentFrameIndex() - getModel().getMovie().length()) <= 1;
				if (!b) {
					undoMI.setEnabled(false);
					redoMI.setEnabled(false);
					cutMI.setEnabled(false);
					copyMI.setEnabled(false);
					pasteMI.setEnabled(false);
					clearMI.setEnabled(false);
				}
				else {
					b = getView().getSelectedComponent() != null;
					cutMI.setEnabled(b);
					copyMI.setEnabled(b);
					pasteMI.setEnabled(getView().getPasteBuffer() != null);
				}
			}

			public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
			}
		});

		menu.add(undoMI);
		getView().setUndoUIComponent(undoMI);
		menu.add(redoMI);
		getView().setRedoUIComponent(redoMI);

		JMenuItem menuItem = new JMenuItem(getView().getActionMap().get("Revert"));
		s = getInternationalText("Revert");
		if (s != null)
			menuItem.setText(s);
		menu.add(menuItem);
		menu.addSeparator();
		menu.add(cutMI);
		menu.add(copyMI);
		menu.add(pasteMI);
		menu.add(clearMI);
		menu.addSeparator();

		menuItem = new JMenuItem(resizeModelAction);
		s = getInternationalText("Resize");
		menuItem.setText((s != null ? s : "Change Model Size") + "...");
		menu.add(menuItem);

		menuItem = new JMenuItem(new TranslationAction(this));
		s = getInternationalText("TranslateWholeModel");
		menuItem.setText((s != null ? s : "Translate Whole Model") + "...");
		menu.add(menuItem);

		menuItem = new JMenuItem(new RotationAction(this));
		s = getInternationalText("RotateWholeModel");
		menuItem.setText((s != null ? s : "Rotate All Particles") + "...");
		menu.add(menuItem);

		menuItem = new JMenuItem(new ChangeTimeStepAction(this));
		s = getInternationalText("ChangeTimeStep");
		menuItem.setText((s != null ? s : "Change Time Step") + "...");
		menu.add(menuItem);
		menu.addSeparator();

		JMenu subMenu = new JMenu("External Fields");
		s = getInternationalText("ExternalField");
		if (s != null)
			subMenu.setText(s);
		subMenu.setMnemonic(KeyEvent.VK_E);
		subMenu.setIcon(new ImageIcon(getClass().getResource("images/FieldIcon.gif")));
		menu.add(subMenu);
		enabledComponentsWhenEditable.add(subMenu);

		menuItem = new JMenuItem(getView().getActionMap().get("Edit Gravitational Field"));
		s = getInternationalText("GField");
		menuItem.setText((s != null ? s : "Gravitational") + "...");
		menuItem.setMnemonic(KeyEvent.VK_G);
		subMenu.add(menuItem);

		menuItem = new JMenuItem(getView().getActionMap().get("Edit Electric Field"));
		s = getInternationalText("EField");
		menuItem.setText((s != null ? s : "Electrical") + "...");
		menuItem.setMnemonic(KeyEvent.VK_E);
		subMenu.add(menuItem);

		menuItem = new JMenuItem(getView().getActionMap().get("Edit Magnetic Field"));
		s = getInternationalText("BField");
		menuItem.setText((s != null ? s : "Magnetic") + "...");
		menuItem.setMnemonic(KeyEvent.VK_M);
		subMenu.add(menuItem);

		return menu;

	}

	JMenu createToolBarMenu(final JMenuItem removeToolBarItem) {

		String s = getInternationalText("ToolBar");
		JMenu menu = new JMenu(s != null ? s : "Toolbar");
		menu.setMnemonic(KeyEvent.VK_T);
		menu.getPopupMenu().addPopupMenuListener(new PopupMenuListener() {
			public void popupMenuCanceled(PopupMenuEvent e) {
			}

			public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
				removeToolBarItem.setEnabled(hasToolbar() && isAuthorable());
			}

			public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
			}
		});

		s = getInternationalText("AddAllButtons");
		JMenuItem menuItem = new JMenuItem(s != null ? s : "Add All Buttons");
		menuItem.setMnemonic(KeyEvent.VK_A);
		menuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				addAllToolBarButtons();
				MDContainer.this.validate();
				getModel().notifyChange();
			}
		});
		menu.add(menuItem);
		enabledComponentsWhenEditable.add(menuItem);

		s = getInternationalText("RemoveToolBar");
		if (s != null)
			removeToolBarItem.setText(s);
		removeToolBarItem.setMnemonic(KeyEvent.VK_R);
		removeToolBarItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String s = getInternationalText("DoYouReallyWantToRemoveToolBar");
				String t = getInternationalText("RemoveEntireToolBar");
				if (JOptionPane.showConfirmDialog(getView(), s != null ? s
						: "Do you really want to remove the toolbar?", t != null ? t : "Remove Toolbar",
						JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
					removeToolbar();
					getModel().notifyChange();
				}
			}
		});
		menu.add(removeToolBarItem);
		enabledComponentsWhenEditable.add(removeToolBarItem);
		menu.addSeparator();

		s = getInternationalText("CustomizeToolBar");
		menuItem = new JMenuItem((s != null ? s : "Customize") + "...");
		menuItem.setMnemonic(KeyEvent.VK_C);
		menuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				showToolBarDialog();
			}
		});
		enabledComponentsWhenEditable.add(menuItem);
		menu.add(menuItem);

		return menu;

	}

	class ToolBar extends JPanel {

		AbstractButton selectObjectButton;
		AbstractButton whatisButton;
		AbstractButton lineToolButton;
		AbstractButton rectToolButton;
		AbstractButton ellipseToolButton;
		AbstractButton markButton;
		AbstractButton measureButton;
		AbstractButton countButton;
		AbstractButton trajButton;
		AbstractButton avposButton;
		AbstractButton rotateObjectButton;
		AbstractButton removeObjectsButton;
		AbstractButton duplicateButton;
		AbstractButton heatButton;
		AbstractButton coolButton;
		AbstractButton iresButton;
		AbstractButton dresButton;
		AbstractButton idmpButton;
		AbstractButton ddmpButton;
		AbstractButton pcharButton;
		AbstractButton ncharButton;
		AbstractButton changeVelocityButton;

		ToolBar() {

			super(new FlowLayout(FlowLayout.LEFT, 0, 0));
			setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

			selectObjectButton = createButton(UserAction.getAction(UserAction.SELE_ID, getModel()));
			toolBarButtonGroup.add(selectObjectButton);

			whatisButton = createButton(UserAction.getAction(UserAction.WHAT_ID, getModel()));
			toolBarButtonGroup.add(whatisButton);

			lineToolButton = createButton(UserAction.getAction(UserAction.LINE_ID, getModel()));
			toolBarButtonGroup.add(lineToolButton);

			rectToolButton = createButton(UserAction.getAction(UserAction.RECT_ID, getModel()));
			toolBarButtonGroup.add(rectToolButton);

			ellipseToolButton = createButton(UserAction.getAction(UserAction.ELLI_ID, getModel()));
			toolBarButtonGroup.add(ellipseToolButton);

			measureButton = createButton(UserAction.getAction(UserAction.MEAS_ID, getModel()));
			toolBarButtonGroup.add(measureButton);

			rotateObjectButton = createButton(UserAction.getAction(UserAction.ROTA_ID, getModel()));
			toolBarButtonGroup.add(rotateObjectButton);

			duplicateButton = createButton(UserAction.getAction(UserAction.DUPL_ID, getModel()));
			toolBarButtonGroup.add(duplicateButton);

			countButton = createButton(UserAction.getAction(UserAction.COUN_ID, getModel()));
			toolBarButtonGroup.add(countButton);

			changeVelocityButton = createButton(UserAction.getAction(UserAction.VELO_ID, getModel()));
			toolBarButtonGroup.add(changeVelocityButton);

			trajButton = createButton(UserAction.getAction(UserAction.TRAJ_ID, getModel()));
			final Runnable trajRun = new Runnable() {
				public void run() {
					if (trajColorArrayPane == null) {
						trajColorArrayPane = new ColorArrayPane();
						trajColorArrayPane.addColorArrayListener(new ColorArrayListener() {
							public void colorSelected(ColorArrayEvent e) {
								ModelComponent mc = getView().getSelectedComponent();
								if (mc instanceof Particle) {
									((Particle) mc).setTrajectoryColor(e.getSelectedColor());
									getView().repaint();
								}
							}
						});
					}
					String s = getInternationalText("TrajectoryColor");
					trajColorArrayPane.createDialog(getView(), s != null ? s : "Trajectory Color",
							ModelerUtilities.colorChooser, new ActionListener() {
								public void actionPerformed(ActionEvent e) {
									ModelComponent mc = getView().getSelectedComponent();
									if (mc instanceof Particle) {
										((Particle) mc).setTrajectoryColor(ModelerUtilities.colorChooser.getColor());
										getView().repaint();
									}
								}
							}).setVisible(true);
				}
			};
			trajButton.addMouseListener(new MouseAdapter() {
				public void mouseClicked(MouseEvent e) {
					if (e.getClickCount() >= 2)
						trajRun.run();
				}
			});
			customizationAction.put(trajButton.getAction().getValue(Action.SHORT_DESCRIPTION), trajRun);
			toolBarButtonGroup.add(trajButton);

			avposButton = createButton(UserAction.getAction(UserAction.RAVE_ID, getModel()));
			toolBarButtonGroup.add(avposButton);

			pcharButton = createButton(UserAction.getAction(UserAction.CPOS_ID, getModel()));
			toolBarButtonGroup.add(pcharButton);

			ncharButton = createButton(UserAction.getAction(UserAction.CNEG_ID, getModel()));
			toolBarButtonGroup.add(ncharButton);

			iresButton = createButton(UserAction.getAction(UserAction.IRES_ID, getModel()));
			toolBarButtonGroup.add(iresButton);

			dresButton = createButton(UserAction.getAction(UserAction.DRES_ID, getModel()));
			toolBarButtonGroup.add(dresButton);

			idmpButton = createButton(UserAction.getAction(UserAction.IDMP_ID, getModel()));
			toolBarButtonGroup.add(idmpButton);

			ddmpButton = createButton(UserAction.getAction(UserAction.DDMP_ID, getModel()));
			toolBarButtonGroup.add(ddmpButton);

			removeObjectsButton = createButton(UserAction.getAction(UserAction.DELE_ID, getModel()));
			removeObjectsButton.addMouseListener(new MouseAdapter() {
				public void mouseClicked(MouseEvent e) {
					if (e.getClickCount() < 2)
						return;
					if (JOptionPane.showConfirmDialog(MDContainer.this,
							"Do you really want to remove all the objects?", "Object removal",
							JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
						getView().removeAllObjects();
					}
				}
			});
			toolBarButtonGroup.add(removeObjectsButton);

			markButton = createButton(UserAction.getAction(UserAction.MARK_ID, getModel()));
			final Runnable markRun = new Runnable() {
				public void run() {
					if (markColorArrayPane == null) {
						markColorArrayPane = new ColorArrayPane();
						markColorArrayPane.addColorArrayListener(new ColorArrayListener() {
							public void colorSelected(ColorArrayEvent e) {
								getView().setMarkColor(e.getSelectedColor());
								if (getView() instanceof AtomisticView)
									((AtomisticView) getView()).refreshJmol();
								getView().repaint();
							}
						});
					}
					String s = getInternationalText("MarkColor");
					markColorArrayPane.createDialog(getView(), s != null ? s : "Mark Color",
							ModelerUtilities.colorChooser, new ActionListener() {
								public void actionPerformed(ActionEvent e) {
									getView().setMarkColor(ModelerUtilities.colorChooser.getColor());
									if (getView() instanceof AtomisticView)
										((AtomisticView) getView()).refreshJmol();
									getView().repaint();
								}
							}).setVisible(true);
				}
			};
			markButton.addMouseListener(new MouseAdapter() {
				public void mouseClicked(MouseEvent e) {
					if (e.getClickCount() >= 2)
						markRun.run();
				}
			});
			customizationAction.put(markButton.getAction().getValue(Action.SHORT_DESCRIPTION), markRun);
			toolBarButtonGroup.add(markButton);

			heatButton = createButton(UserAction.getAction(UserAction.HEAT_ID, getModel()));
			final Runnable run6 = new Runnable() {
				public void run() {
					if (pointHeaterDialog == null)
						pointHeaterDialog = new PointHeaterDialog(getModel());
					JDialog d = pointHeaterDialog.showDialog(JOptionPane.getFrameForComponent(MDContainer.this));
					d.setLocationRelativeTo(MDContainer.this);
					d.setVisible(true);
				}
			};
			heatButton.addMouseListener(new MouseAdapter() {
				public void mouseClicked(MouseEvent e) {
					if (e.getClickCount() >= 2)
						run6.run();
				}
			});
			customizationAction.put(heatButton.getAction().getValue(Action.SHORT_DESCRIPTION), run6);
			toolBarButtonGroup.add(heatButton);

			coolButton = createButton(UserAction.getAction(UserAction.COOL_ID, getModel()));
			final Runnable run7 = new Runnable() {
				public void run() {
					if (pointHeaterDialog == null)
						pointHeaterDialog = new PointHeaterDialog(getModel());
					JDialog d = pointHeaterDialog.showDialog(JOptionPane.getFrameForComponent(MDContainer.this));
					d.setLocationRelativeTo(MDContainer.this);
					d.setVisible(true);
				}
			};
			coolButton.addMouseListener(new MouseAdapter() {
				public void mouseClicked(MouseEvent e) {
					if (e.getClickCount() >= 2)
						run7.run();
				}
			});
			customizationAction.put(coolButton.getAction().getValue(Action.SHORT_DESCRIPTION), run7);
			toolBarButtonGroup.add(coolButton);

			putClientProperty("Select button", selectObjectButton);

			actionCategory = new LinkedHashMap<String, List>();

			List<AbstractButton> list = new ArrayList<AbstractButton>();
			list.add(selectObjectButton);
			list.add(whatisButton);
			list.add(lineToolButton);
			list.add(rectToolButton);
			list.add(ellipseToolButton);
			list.add(markButton);
			list.add(countButton);
			list.add(measureButton);
			list.add(trajButton);
			list.add(avposButton);
			String s = getInternationalText("NonEditingActions");
			actionCategory.put(s != null ? s : "Non-Editing Actions", list);

		}

	}

}