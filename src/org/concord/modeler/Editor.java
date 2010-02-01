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
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDesktopPane;
import javax.swing.JDialog;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.plaf.basic.BasicBorders;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.Element;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledEditorKit;

import org.concord.modeler.draw.FillMode;
import org.concord.modeler.event.ModelListener;
import org.concord.modeler.event.PageComponentEvent;
import org.concord.modeler.event.PageComponentListener;
import org.concord.modeler.event.PageEvent;
import org.concord.modeler.event.PageListener;
import org.concord.modeler.text.BulletIcon;
import org.concord.modeler.text.Page;
import org.concord.modeler.ui.BackgroundPanel;
import org.concord.modeler.ui.ColorComboBox;
import org.concord.modeler.ui.ColorMenu;
import org.concord.modeler.ui.ColorRectangle;
import org.concord.modeler.ui.ComboBoxRenderer;
import org.concord.modeler.ui.IconPool;
import org.concord.modeler.ui.ProcessMonitor;
import org.concord.mw2d.MDView;
import org.concord.mw2d.ui.AtomContainer;
import org.concord.mw2d.ui.MDContainer;
import org.concord.mw2d.models.MDModel;

/**
 * This is the <code>Page</code> editor.
 * 
 * @see org.concord.modeler.text.Page
 * @author Charles Xie
 */

public class Editor extends JComponent implements PageListener, PageComponentListener {

	private static StyledEditorKit.FontSizeAction[] fsa;
	private static StyledEditorKit.ForegroundAction[] fga;
	private static StyledEditorKit.FontFamilyAction[] ffa;

	private static final Border SELECTED_BORDER = new BasicBorders.ButtonBorder(Color.lightGray, Color.white,
			Color.black, Color.gray);
	private final static Dimension SCREEN_SIZE = Toolkit.getDefaultToolkit().getScreenSize();
	private static ImageIcon editIcon, viewIcon;
	private static Icon toolBarHeaderIcon = new ImageIcon(Editor.class.getResource("images/ToolBarHeaderBar.gif"));
	private static boolean initialized;

	JCheckBox editCheckBox;

	private boolean signifyChanges;
	private boolean isFullScreen;
	private JDesktopPane desktopPane;
	private JPanel desktopHostPanel;
	private List<EditorListener> editorListenerList;
	private InsertComponentPopupMenu insertComponentPopupMenu;
	private JPopupMenu backgroundPopupMenu;
	private static ActionNotifier actionNotifier;
	private StatusBar statusBar;
	private JInternalFrame floatingButtons;
	private List<Component> enabledComponentsWhenEditable, enabledComponentsWhenNotEditable,
			disabledComponentsWhileLoading;
	private ComponentPool componentPool;
	private JToolBar[] toolBar;
	private BackgroundPanel toolBarPanel;
	private JScrollPane scroller;
	private Page page;
	private FindDialog findDialog;
	private JDialog projectorScreen;
	private JComboBox fontNameComboBox, fontSizeComboBox, fontColorComboBox;
	private JCheckBox boldCheckBox, italicCheckBox, underlineCheckBox;
	private JCheckBox leftAlignmentCheckBox, rightAlignmentCheckBox, centerAlignmentCheckBox;
	private JButton cutButton, copyButton, pasteButton;
	private JCheckBox bulletCheckBox;
	private JButton snapshotButton;
	private JButton submitCommentButton, viewCommentButton, mwSpaceButton;
	private JButton inputImageButton;
	private Map<String, Point> positionMap;
	private List<Object> loadModelAndPageList; // initialization script support - avoid redundant execution
	private ServerGate serverGate;

	ActionListener findAction;
	Action openSnapshotGallery;
	private ActionListener lockAction;
	private ActionListener fontFamilyAction;
	private ActionListener fontSizeAction;
	private ActionListener fontColorAction;
	private ItemListener boldAction;
	private ItemListener italicAction;
	private ItemListener underlineAction;
	private ItemListener leftAlignmentAction;
	private ItemListener centerAlignmentAction;
	private ItemListener rightAlignmentAction;
	private ItemListener bulletAction;
	private Action fullScreenAction;

	public Editor(StatusBar statusBar) {

		if (!Page.isApplet()) {
			init();
			setStatusBar(statusBar);
		}

		if (actionNotifier == null) {
			actionNotifier = new ActionNotifier(this);
		}
		else {
			actionNotifier.setParentComponent(this);
		}

		if (editIcon == null)
			editIcon = new ImageIcon(getClass().getResource("images/edit.png"));
		if (viewIcon == null)
			viewIcon = new ImageIcon(getClass().getResource("images/view.png"));

		desktopPane = new JDesktopPane();
		desktopPane.setBackground(Color.white);
		desktopPane.setDragMode(JDesktopPane.OUTLINE_DRAG_MODE);

		page = new Page();
		page.setEditor(this);
		if (statusBar != null) {
			page.setURLDisplay(statusBar.tipBar);
			page.setProgressBar(statusBar.getProgressBar());
		}
		page.addPageListener(this);
		page.setActionNotifier(actionNotifier);
		page.addCaretListener(new CaretListener() {
			public void caretUpdate(CaretEvent e) {
				if (page.isEditable())
					showAttributes(e.getDot());
			}
		});

		componentPool = new ComponentPool(page);
		componentPool.setSelectionColor(page.getSelectionColor());
		page.setComponentPool(componentPool);

		scroller = new JScrollPane(page, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scroller.setBorder(BorderFactory.createEmptyBorder());
		positionMap = new HashMap<String, Point>();
		desktopPane.add(scroller, JLayeredPane.DEFAULT_LAYER);
		desktopPane.addComponentListener(new ComponentAdapter() {
			public void componentResized(ComponentEvent e) {
				scroller.setBounds(desktopPane.getBounds());
				scroller.validate();
				Debugger.print("Desktop resized");
				EventQueue.invokeLater(new Runnable() {
					public void run() {
						if (System.getProperty("java.version").compareTo("1.6.0") >= 0) {
							int w = desktopPane.getWidth() - scroller.getVerticalScrollBar().getWidth();
							int h = page.getHeight();
							page.setSize(new Dimension(w, h));
							// this fix doesn't sound right
							Debugger.print("Page resized");
						}
						page.autoResizeComponents();
						Debugger.print("Embedded components autoresized");
					}
				});
			}
		});

		setLayout(new BorderLayout());

		desktopHostPanel = new JPanel(new BorderLayout(0, 0));
		if (!Page.isApplet())
			desktopHostPanel.setBorder(BorderFactory.createLoweredBevelBorder());
		desktopHostPanel.add(desktopPane, BorderLayout.CENTER);
		add(desktopHostPanel, BorderLayout.CENTER);

		enabledComponentsWhenEditable = new ArrayList<Component>();
		enabledComponentsWhenNotEditable = new ArrayList<Component>();
		disabledComponentsWhileLoading = new ArrayList<Component>();

		if (!Page.isApplet()) {
			page.getInputMap().put((KeyStroke) fullScreenAction.getValue(Action.ACCELERATOR_KEY), "Full Screen");
			page.getActionMap().put("Full Screen", fullScreenAction);
			page.putActivityAction(openSnapshotGallery);
		}

		loadModelAndPageList = new ArrayList<Object>();

	}

	private void init() {

		fsa = new StyledEditorKit.FontSizeAction[ModelerUtilities.FONT_SIZE.length];
		fga = new StyledEditorKit.ForegroundAction[ColorRectangle.COLORS.length + 1];
		ffa = new StyledEditorKit.FontFamilyAction[ModelerUtilities.FONT_FAMILY_NAMES.length];
		for (int i = 0; i < fsa.length; i++) {
			fsa[i] = new StyledEditorKit.FontSizeAction(ModelerUtilities.FONT_SIZE[i].toString(),
					ModelerUtilities.FONT_SIZE[i].intValue());
		}
		for (int i = 0; i < fga.length - 1; i++) {
			fga[i] = new StyledEditorKit.ForegroundAction(ColorRectangle.COLORS[i].toString(), ColorRectangle.COLORS[i]);
		}
		for (int i = 0; i < ffa.length; i++) {
			ffa[i] = new StyledEditorKit.FontFamilyAction(ModelerUtilities.FONT_FAMILY_NAMES[i],
					ModelerUtilities.FONT_FAMILY_NAMES[i]);
		}

		lockAction = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (page.isRemote()) {
					JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(Editor.this),
							"A page that is not on your disk cannot be edited.", "Error", JOptionPane.ERROR_MESSAGE);
					setEditable(false);
					editCheckBox.setToolTipText(isEditable() ? "Go to the viewing mode" : "Go to the editing mode");
					return;
				}
				setEditable(editCheckBox.isSelected());
				page.requestFocusInWindow();
			}
		};

		fontFamilyAction = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JComboBox cb = (JComboBox) e.getSource();
				if (!cb.isPopupVisible())
					return;
				ffa[cb.getSelectedIndex()].actionPerformed(e);
				if (page.getSelectedText() != null && page.isEditable())
					page.getSaveReminder().setChanged(true);
			}
		};

		fontSizeAction = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JComboBox cb = (JComboBox) e.getSource();
				if (!cb.isPopupVisible())
					return;
				fsa[cb.getSelectedIndex()].actionPerformed(e);
				if (page.getSelectedText() != null && page.isEditable())
					page.getSaveReminder().setChanged(true);
			}
		};

		fontColorAction = new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				final ColorComboBox cb = (ColorComboBox) e.getSource();
				if (!cb.isPopupVisible())
					return;
				if (cb.getSelectedIndex() >= fga.length) {
					cb.updateColor(new Runnable() {
						public void run() {
							Color mc = cb.getMoreColor();
							fga[fga.length - 1] = new StyledEditorKit.ForegroundAction(mc.toString(), mc);
							EventQueue.invokeLater(new Runnable() {
								public void run() {
									fga[fga.length - 1].actionPerformed(e);
								}
							});
							if (page.getSelectedText() != null && page.isEditable())
								page.getSaveReminder().setChanged(true);
						}
					});
					return;
				}
				if (cb.getSelectedIndex() == fga.length - 1) {
					Color mc = cb.getMoreColor();
					fga[fga.length - 1] = new StyledEditorKit.ForegroundAction(mc.toString(), mc);
				}
				fga[cb.getSelectedIndex()].actionPerformed(e);
				if (page.getSelectedText() != null && page.isEditable())
					page.getSaveReminder().setChanged(true);
			}
		};

		boldAction = new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				boolean b = e.getStateChange() == ItemEvent.SELECTED;
				boldCheckBox.setBorderPainted(b);
				toggleButtonLook(boldCheckBox, b);
				if (signifyChanges && page.getSelectedText() != null && page.isEditable())
					page.getSaveReminder().setChanged(true);
			}
		};

		italicAction = new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				boolean b = e.getStateChange() == ItemEvent.SELECTED;
				italicCheckBox.setBorderPainted(b);
				toggleButtonLook(italicCheckBox, b);
				if (signifyChanges && page.getSelectedText() != null && page.isEditable())
					page.getSaveReminder().setChanged(true);
			}
		};

		underlineAction = new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				boolean b = e.getStateChange() == ItemEvent.SELECTED;
				underlineCheckBox.setBorderPainted(b);
				toggleButtonLook(underlineCheckBox, b);
				if (signifyChanges && page.getSelectedText() != null && page.isEditable())
					page.getSaveReminder().setChanged(true);
			}
		};

		leftAlignmentAction = new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				boolean b = e.getStateChange() == ItemEvent.SELECTED;
				leftAlignmentCheckBox.setBorderPainted(b);
				toggleButtonLook(leftAlignmentCheckBox, b);
				if (signifyChanges && page.isEditable())
					page.getSaveReminder().setChanged(true);
			}
		};

		centerAlignmentAction = new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				boolean b = e.getStateChange() == ItemEvent.SELECTED;
				centerAlignmentCheckBox.setBorderPainted(b);
				toggleButtonLook(centerAlignmentCheckBox, b);
				if (signifyChanges && page.isEditable())
					page.getSaveReminder().setChanged(true);
			}
		};

		rightAlignmentAction = new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				boolean b = e.getStateChange() == ItemEvent.SELECTED;
				rightAlignmentCheckBox.setBorderPainted(b);
				toggleButtonLook(rightAlignmentCheckBox, b);
				if (signifyChanges && page.isEditable())
					page.getSaveReminder().setChanged(true);
			}
		};

		bulletAction = new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				boolean b = e.getStateChange() == ItemEvent.SELECTED;
				bulletCheckBox.setBorderPainted(b);
				toggleButtonLook(bulletCheckBox, b);
				if (signifyChanges && page.isEditable())
					page.getSaveReminder().setChanged(true);
			}
		};

		findAction = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (findDialog == null)
					findDialog = new FindDialog(page);
				findDialog.setCurrentValues();
				findDialog.setVisible(true);
			}
		};

		openSnapshotGallery = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				SnapshotGallery.sharedInstance().setOwner(Editor.this);
				SnapshotGallery.sharedInstance().show();
			}

			public String toString() {
				return (String) getValue(Action.SHORT_DESCRIPTION);
			}
		};
		openSnapshotGallery.putValue(Action.NAME, "View Snapshots");
		openSnapshotGallery.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_V);
		openSnapshotGallery.putValue(Action.SHORT_DESCRIPTION, "View snapshots");

		fullScreenAction = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				if (isFullScreen)
					windowScreen();
				else fullScreen();
			}
		};
		fullScreenAction.putValue(Action.NAME, "Full Screen");
		fullScreenAction.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_F);
		fullScreenAction.putValue(Action.SHORT_DESCRIPTION, "Toggle full screen mode");
		if (!Modeler.isMac())
			fullScreenAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0, true));

	}

	private void toggleButtonLook(AbstractButton button, boolean b) {
		if (b) {
			button.setBorder(SELECTED_BORDER);
			button.setBackground(Color.white);
		}
		else {
			button.setBorder(BorderFactory.createEmptyBorder());
			button.setBackground(toolBarPanel.getBackground());
		}
	}

	// for applet to override
	public URL getCodeBase() {
		return null;
	}

	/**
	 * When the current instance is closed, destroy this component to prevent memory leak.
	 */
	public void destroy() {

		enabledComponentsWhenEditable.clear();
		enabledComponentsWhenNotEditable.clear();
		disabledComponentsWhileLoading.clear();
		getActionMap().clear();
		getInputMap().clear();

		if (!Page.isApplet()) {
			SnapshotGallery.sharedInstance().removeFlashComponent(snapshotButton);
			SnapshotGallery.sharedInstance().setOwner(null);
			fontNameComboBox.removeActionListener(fontFamilyAction);
			fontSizeComboBox.removeActionListener(fontSizeAction);
			fontColorComboBox.removeActionListener(fontColorAction);
			inputImageButton.setAction(null);
			if (toolBar[0] != null)
				destroyToolBar(toolBar[0]);
			if (toolBar[1] != null)
				destroyToolBar(toolBar[1]);
			if (toolBar[2] != null)
				destroyToolBar(toolBar[2]);
		}
		actionNotifier.setParentComponent(null);

		page.destroy();
		componentPool.destroy();

		setStatusBar(null);
		scroller.removeAll();
		ComponentListener[] cpl = desktopPane.getComponentListeners();
		if (cpl != null) {
			for (ComponentListener l : cpl)
				desktopPane.removeComponentListener(l);
		}
		desktopPane.removeAll();
		desktopHostPanel.removeAll();
		if (toolBarPanel != null)
			toolBarPanel.removeAll();
		removeAll();

		if (floatingButtons != null)
			destroyFloatingButtons();
		if (projectorScreen != null)
			projectorScreen.dispose();

		lockAction = null;
		findAction = null;
		bulletAction = null;
		fontFamilyAction = null;
		fontSizeAction = null;
		fontColorAction = null;
		boldAction = null;
		italicAction = null;
		underlineAction = null;
		leftAlignmentAction = null;
		centerAlignmentAction = null;
		rightAlignmentAction = null;
		openSnapshotGallery = null;
		fullScreenAction = null;
		componentPool = null;
		toolBarPanel = null;
		page = null;

	}

	void setServerGate(ServerGate gate) {
		serverGate = gate;
	}

	private void destroyFloatingButtons() {
		if (floatingButtons == null)
			return;
		JComponent c = (JComponent) floatingButtons.getContentPane().getComponent(0);
		if (c != null) {
			Component[] cs = c.getComponents();
			AbstractButton b;
			for (Component x : cs) {
				if (x instanceof AbstractButton) {
					b = (AbstractButton) x;
					b.setAction(null);
					ActionListener[] al = b.getActionListeners();
					if (al != null) {
						for (ActionListener l : al)
							b.removeActionListener(l);
					}
				}
			}
		}
		floatingButtons.dispose();
	}

	private void destroyToolBar(JToolBar tb) {
		if (tb == null)
			return;
		Component c;
		AbstractButton b;
		for (int i = 0, n = tb.getComponentCount(); i < n; i++) {
			c = tb.getComponentAtIndex(i);
			if (c instanceof AbstractButton) {
				b = (AbstractButton) c;
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
			}
		}
		tb.removeAll();
	}

	void createToolBars() {

		toolBar = new JToolBar[3];

		Initializer.sharedInstance().setMessage("Creating Editor's tool bar 1...");
		toolBar[0] = createToolBar1();

		toolBar[1] = createToolBar2();

		Initializer.sharedInstance().setMessage("Creating Editor's tool bar 3...");
		toolBar[2] = createToolBar3();

		toolBarPanel = new BackgroundPanel(new BorderLayout(0, 0), new ImageIcon(Modeler.class.getResource(Modeler
				.isMac() ? "images/toolbar_bg_mac.png" : "images/toolbar_bg_win.png")));
		toolBarPanel.setBorder(BorderFactory.createEmptyBorder());
		toolBarPanel.add(toolBar[0], BorderLayout.CENTER);

		add(toolBarPanel, BorderLayout.NORTH);

	}

	void setToolBarOffset(int h) {
		toolBarPanel.setYOffset(h);
	}

	public void addEditorListener(EditorListener el) {
		if (el == null)
			throw new IllegalArgumentException("null input");
		if (editorListenerList == null) {
			editorListenerList = new ArrayList<EditorListener>();
		}
		else {
			if (editorListenerList.contains(el))
				return;
		}
		editorListenerList.add(el);
	}

	public void removeEditorListener(EditorListener el) {
		if (el == null)
			throw new IllegalArgumentException("null input");
		if (editorListenerList == null || editorListenerList.isEmpty())
			return;
		editorListenerList.remove(el);
	}

	protected void notifyEditorListeners(final EditorEvent e) {
		if (e == null)
			return;
		if (editorListenerList == null || editorListenerList.isEmpty())
			return;
		Runnable r = new Runnable() {
			public void run() {
				for (EditorListener el : editorListenerList) {
					switch (e.getID()) {
					case EditorEvent.EDITOR_ENABLED:
						el.editorEnabled(e);
						break;
					case EditorEvent.EDITOR_DISABLED:
						el.editorDisabled(e);
						break;
					}
				}
			}
		};
		if (EventQueue.isDispatchThread()) {
			r.run();
		}
		else {
			EventQueue.invokeLater(r); // make sure it is in AWT thread
		}
	}

	void setViewPosition(int x, int y) {
		setViewPosition(new Point(x, y));
	}

	void setViewPosition(Point p) {
		scroller.getViewport().setViewPosition(p);
	}

	Point getViewPosition() {
		return scroller.getViewport().getViewPosition();
	}

	void addEnabledComponentWhenEditable(Component c) {
		enabledComponentsWhenEditable.add(c);
	}

	void addEnabledComponentWhenNotEditable(Component c) {
		enabledComponentsWhenNotEditable.add(c);
	}

	void addDisabledComponentWhileLoading(Component c) {
		disabledComponentsWhileLoading.add(c);
	}

	public Page getPage() {
		return page;
	}

	public void setPage(Page p) {
		page = p;
	}

	public StatusBar getStatusBar() {
		return statusBar;
	}

	public void setStatusBar(StatusBar sb) {
		statusBar = sb;
	}

	public JToolBar[] getToolBars() {
		return toolBar;
	}

	private void fullScreen() {
		if (isFullScreen)
			return;
		if (projectorScreen == null) {
			projectorScreen = new JDialog(JOptionPane.getFrameForComponent(this));
			projectorScreen.setUndecorated(true);
			projectorScreen.setLocation(0, 0);
			projectorScreen.setSize(SCREEN_SIZE);
			projectorScreen.setResizable(false);
			projectorScreen.addWindowListener(new WindowAdapter() {
				public void windowActivated(WindowEvent e) {
					page.requestFocusInWindow();
				}
			});
		}
		isFullScreen = true;
		if (Modeler.isMac())
			ModelerUtilities.setMacOSXMenuBarVisible(false);
		String address = page.getAddress().toLowerCase();
		if (address.endsWith(".pdb") || address.endsWith(".xyz")) {
			Object o = page.getEmbeddedComponent(PageMolecularViewer.class, 0);
			if (o instanceof PageMolecularViewer) {
				((PageMolecularViewer) o).setPreferredSize(new Dimension(SCREEN_SIZE.width, SCREEN_SIZE.height - 20));
				((PageMolecularViewer) o).runScreensaver(true);
			}
		}
		projectorScreen.setContentPane(desktopPane);
		showFullScreenFloatingButtons(true);
		projectorScreen.validate();
		projectorScreen.setVisible(true);
	}

	private void windowScreen() {
		if (!isFullScreen)
			return;
		isFullScreen = false;
		if (Modeler.isMac())
			ModelerUtilities.setMacOSXMenuBarVisible(true);
		String address = page.getAddress().toLowerCase();
		if (address.endsWith(".pdb") || address.endsWith(".xyz")) {
			Object o = page.getEmbeddedComponent(PageMolecularViewer.class, 0);
			if (o instanceof PageMolecularViewer) {
				((PageMolecularViewer) o).setPreferredSize(getSize());
				((PageMolecularViewer) o).runScreensaver(false);
			}
		}
		projectorScreen.setVisible(false);
		projectorScreen.remove(desktopPane);
		desktopHostPanel.add(desktopPane, BorderLayout.CENTER);
		desktopHostPanel.validate();
		showFullScreenFloatingButtons(false);
	}

	private void createFullScreenFloatingButtons() {

		JPanel p = new JPanel(new GridLayout(1, 4, 2, 2));
		p.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));

		JButton button = new JButton(IconPool.getIcon("exit"));
		button.setBorderPainted(false);
		button.setToolTipText("Exit the full screen mode");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				windowScreen();
			}
		});
		p.add(button);

		button = new JButton(page.getNavigator().getAction(Navigator.BACK));
		button.setText(null);
		button.setBorderPainted(false);
		p.add(button);

		button = new JButton(page.getNavigator().getAction(Navigator.FORWARD));
		button.setText(null);
		button.setBorderPainted(false);
		p.add(button);

		button = new JButton(page.getAction(Page.REFRESH));
		button.setText(null);
		button.setBorderPainted(false);
		p.add(button);

		floatingButtons = new JInternalFrame(null, false, false, false, false);
		floatingButtons.setFrameIcon(null);
		floatingButtons.getContentPane().add(p, BorderLayout.CENTER);
		floatingButtons.pack();
		floatingButtons.setLocation(20, SCREEN_SIZE.height - floatingButtons.getHeight() - (Modeler.isMac() ? 50 : 20));

	}

	private void showFullScreenFloatingButtons(boolean b) {
		if (b) {
			if (floatingButtons == null)
				createFullScreenFloatingButtons();
			desktopPane.add(floatingButtons, JLayeredPane.PALETTE_LAYER);
			floatingButtons.setVisible(true);
		}
		else {
			if (floatingButtons != null) {
				desktopPane.remove(floatingButtons);
				floatingButtons.setVisible(false);
			}
		}
	}

	public void createNewPage() {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				page.getAction(Page.NEW_PAGE).actionPerformed(null);
				setToolbarButtons();
			}
		});
	}

	/** wrapper method of <tt>Page.getAddress()</tt> */
	public String getAddress() {
		return page.getAddress();
	}

	/** wrapper method of <tt>Page.getTitle()</tt> */
	public String getTitle() {
		return page.getTitle();
	}

	/** wrapper method of <tt>Page.setTitle(String s)</tt> */
	public void setTitle(String s) {
		page.setTitle(s);
	}

	public boolean isEditable() {
		return page.isEditable();
	}

	/**
	 * set this page and its embedded components editable. When page is editable, text must be set back to the actual
	 * size.
	 */
	public void setEditable(final boolean b) {
		page.setEditable(b);
		if (b) {
			if (page.getFontIncrement() != 0) {
				actionNotifier.setParentComponent(this);
				EventQueue.invokeLater(new Runnable() {
					public void run() {
						actionNotifier.show(ActionNotifier.BACK_TO_ACTUAL_SIZE);
						FontSizeChanger.step(page, -page.getFontIncrement());
						page.requestFocusInWindow();
					}
				});
			}
		}
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				editCheckBox.removeActionListener(lockAction);
				editCheckBox.setSelected(b);
				editCheckBox.addActionListener(lockAction);
				setToolBar(b);
				enableActions(b);
				editCheckBox.setToolTipText(b ? "Go to the viewing mode" : "Go to the editing mode");
				statusBar.tipBar.setText(b ? "Editing" : "Viewing");
				notifyEditorListeners(new EditorEvent(Editor.this, b ? EditorEvent.EDITOR_ENABLED
						: EditorEvent.EDITOR_DISABLED));
			}
		});
	}

	private void enableActions(boolean b) {
		if (!EventQueue.isDispatchThread()) {
			System.err.println("<ERROR> Single thread rule violation");
			return;
		}
		if (!enabledComponentsWhenEditable.isEmpty()) {
			for (Component c : enabledComponentsWhenEditable) {
				c.setEnabled(b);
			}
		}
		if (!enabledComponentsWhenNotEditable.isEmpty()) {
			for (Component c : enabledComponentsWhenNotEditable) {
				c.setEnabled(!b);
			}
		}
	}

	private JToolBar createToolBar1() {

		JToolBar tb = new JToolBar(SwingConstants.HORIZONTAL);
		tb.setOpaque(false);
		tb.setFloatable(false);
		tb.setLayout(new FlowLayout(FlowLayout.LEFT, 2, 1));
		tb.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
		tb.putClientProperty("JToolBar.isRollover", Boolean.TRUE);
		if (!Modeler.isMac())
			tb.add(new JLabel(toolBarHeaderIcon));

		editCheckBox = new JCheckBox(editIcon);
		if (Modeler.showToolBarText) {
			String s = Modeler.getInternationalText("EditingMode");
			editCheckBox.setText(s != null ? s : "Edit");
		}
		editCheckBox.setToolTipText("Go to the editing mode");
		editCheckBox.setSelected(false);
		editCheckBox.addActionListener(lockAction);
		addDisabledComponentWhileLoading(editCheckBox);
		tb.add(editCheckBox);
		Modeler.setToolBarButton(editCheckBox, true);

		JButton button = new JButton(page.getAction(Page.OPEN_PAGE));
		button.setIcon(new ImageIcon(getClass().getResource("images/open.png")));
		if (Modeler.showToolBarText) {
			String s = Modeler.getInternationalText("OpenButton");
			if (s != null)
				button.setText(s);
		}
		addDisabledComponentWhileLoading(button);
		tb.add(button);
		Modeler.setToolBarButton(button, true);

		button = new JButton(page.getAction(Page.SAVE_PAGE));
		button.setIcon(new ImageIcon(getClass().getResource("images/save.png")));
		if (Modeler.showToolBarText) {
			String s = Modeler.getInternationalText("SaveButton");
			if (s != null)
				button.setText(s);
		}
		addDisabledComponentWhileLoading(button);
		tb.add(button);
		Modeler.setToolBarButton(button, true);

		button = new JButton(new ImageIcon(getClass().getResource("images/find.png")));
		button.setToolTipText("Find on this page");
		button.addActionListener(findAction);
		addDisabledComponentWhileLoading(button);
		tb.add(button);
		Modeler.setToolBarButton(button, true);

		button = new JButton(page.getAction("Print"));
		button.setText(null);
		button.setIcon(new ImageIcon(getClass().getResource("images/print.png")));
		addDisabledComponentWhileLoading(button);
		tb.add(button);
		Modeler.setToolBarButton(button, true);

		snapshotButton = new JButton(new ImageIcon(getClass().getResource("images/album.png")));
		snapshotButton.setToolTipText("Open snapshot gallery");
		snapshotButton.addActionListener(openSnapshotGallery);
		if (Modeler.showToolBarText) {
			String s = Modeler.getInternationalText("OpenSnapshotGalleryButton");
			snapshotButton.setText(s != null ? s : "Snapshots");
		}
		tb.add(snapshotButton);
		SnapshotGallery.sharedInstance().addFlashComponent(snapshotButton);
		addDisabledComponentWhileLoading(snapshotButton);
		Modeler.setToolBarButton(snapshotButton, true);

		submitCommentButton = new JButton(new ImageIcon(getClass().getResource("images/comment.png")));
		String s = Modeler.getInternationalText("MakeCommentButton");
		submitCommentButton.setText(s != null ? s : "Comment");
		submitCommentButton.setToolTipText("Make comments on current page");
		submitCommentButton.addActionListener(serverGate.commentAction);
		tb.add(submitCommentButton);
		addDisabledComponentWhileLoading(submitCommentButton);
		Modeler.setToolBarButton(submitCommentButton);

		viewCommentButton = new JButton(new ImageIcon(getClass().getResource("images/view_comment.png")));
		viewCommentButton.setToolTipText("Show discussions about current page");
		viewCommentButton.addActionListener(serverGate.viewCommentAction);
		tb.add(viewCommentButton);
		addDisabledComponentWhileLoading(viewCommentButton);
		Modeler.setToolBarButton(viewCommentButton);

		mwSpaceButton = new JButton(new ImageIcon(getClass().getResource("images/mwspace.png")));
		s = Modeler.getInternationalText("MyMwSpace");
		mwSpaceButton.setText(s != null ? s : "My MW Space");
		mwSpaceButton.setToolTipText("Click to enter my MW Space");
		mwSpaceButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				page.getNavigator().visitLocation(Modeler.getContextRoot() + "home.jsp?client=mw");
			}
		});
		tb.add(mwSpaceButton);
		addDisabledComponentWhileLoading(mwSpaceButton);
		Modeler.setToolBarButton(mwSpaceButton);

		return tb;

	}

	private JToolBar createToolBar3() {

		JToolBar tb = new JToolBar(SwingConstants.HORIZONTAL);
		tb.setOpaque(false);
		tb.setFloatable(false);
		tb.setLayout(new FlowLayout(FlowLayout.LEFT, 2, 1));
		tb.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
		if (!Modeler.isMac())
			tb.add(new JLabel(toolBarHeaderIcon));
		tb.putClientProperty("JToolBar.isRollover", Boolean.TRUE);

		cutButton = new JButton(page.getAction(DefaultEditorKit.cutAction));
		cutButton.setIcon(new ImageIcon(getClass().getResource("images/cut.png")));
		cutButton.setText(null);
		cutButton.setToolTipText("Cut");
		tb.add(cutButton);
		Modeler.setToolBarButton(cutButton);

		copyButton = new JButton(page.getAction(DefaultEditorKit.copyAction));
		copyButton.setIcon(new ImageIcon(getClass().getResource("images/copy.png")));
		copyButton.setText(null);
		copyButton.setToolTipText("Copy");
		tb.add(copyButton);
		Modeler.setToolBarButton(copyButton);

		pasteButton = new JButton(page.getAction(DefaultEditorKit.pasteAction));
		pasteButton.setIcon(new ImageIcon(getClass().getResource("images/paste.png")));
		pasteButton.setText(null);
		pasteButton.setToolTipText("Paste");
		tb.add(pasteButton);
		Modeler.setToolBarButton(pasteButton);

		JButton button = new JButton(page.getAction(Page.UNDO));
		button.setIcon(new ImageIcon(getClass().getResource("images/undo.png")));
		button.setText(null);
		tb.add(button);
		Modeler.setToolBarButton(button);

		button = new JButton(page.getAction(Page.REDO));
		button.setText(null);
		button.setIcon(new ImageIcon(getClass().getResource("images/redo.png")));
		tb.add(button);
		Modeler.setToolBarButton(button);

		tb.add(new JLabel(Modeler.toolBarSeparatorIcon));

		button = new JButton(new ImageIcon(getClass().getResource("images/bgfill.png")));
		button.setToolTipText("Set background");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (backgroundPopupMenu == null)
					backgroundPopupMenu = createColorMenu().getPopupMenu();
				backgroundPopupMenu.show((JButton) e.getSource(), 5, 10);
			}
		});
		tb.add(button);
		Modeler.setToolBarButton(button);

		button = new JButton(new ImageIcon(getClass().getResource("images/insert_model.png")));
		button.setToolTipText("Insert a model component");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (insertComponentPopupMenu == null)
					insertComponentPopupMenu = new InsertComponentPopupMenu(page);
				insertComponentPopupMenu.show((JButton) e.getSource(), 5, 10);
			}
		});
		tb.add(button);
		Modeler.setToolBarButton(button);

		inputImageButton = new JButton(page.getAction("Input Image"));
		inputImageButton.setIcon(new ImageIcon(getClass().getResource("images/insert_picture.png")));
		inputImageButton.setText(null);
		inputImageButton.setToolTipText("Insert an image");
		tb.add(inputImageButton);
		Modeler.setToolBarButton(inputImageButton);

		tb.add(new JLabel(Modeler.toolBarSeparatorIcon));

		button = new JButton(page.getAction(Page.INSERT_COMPONENT));
		button.setText(null);
		button.setName("Text Box");
		button.setToolTipText("Insert a text box");
		button.setIcon(new ImageIcon(getClass().getResource("text/images/text_box.png")));
		tb.add(button);
		Modeler.setToolBarButton(button);

		button = new JButton(page.getAction(Page.INSERT_COMPONENT));
		button.setText(null);
		button.setName("Multiple Choice");
		button.setToolTipText("Insert a multiple choice");
		button.setIcon(new ImageIcon(getClass().getResource("text/images/multi_choice.png")));
		tb.add(button);
		Modeler.setToolBarButton(button);

		button = new JButton(page.getAction(Page.INSERT_COMPONENT));
		button.setIcon(new ImageIcon(getClass().getResource("text/images/image_question.png")));
		button.setText(null);
		button.setName("Image Question");
		button.setToolTipText("Insert an image question");
		tb.add(button);
		Modeler.setToolBarButton(button);

		button = new JButton(page.getAction(Page.INSERT_COMPONENT));
		button.setIcon(new ImageIcon(getClass().getResource("text/images/text_area.png")));
		button.setText(null);
		button.setName("User Input Text Area");
		button.setToolTipText("Insert a user-input text area");
		tb.add(button);
		Modeler.setToolBarButton(button);

		button = new JButton(page.getAction(Page.INSERT_COMPONENT));
		button.setText(null);
		button.setName("Table");
		button.setToolTipText("Insert a table");
		button.setIcon(new ImageIcon(getClass().getResource("text/images/table.png")));
		tb.add(button);
		Modeler.setToolBarButton(button);

		button = new JButton(page.getAction(Page.INSERT_COMPONENT));
		button.setText(null);
		button.setName("Applet");
		button.setToolTipText("Insert an applet");
		button.setIcon(new ImageIcon(getClass().getResource("text/images/applet.png")));
		tb.add(button);
		Modeler.setToolBarButton(button);

		button = new JButton(page.getAction(Page.INSERT_COMPONENT));
		button.setText(null);
		button.setName("Plugin");
		button.setToolTipText("Insert a plugin");
		button.setIcon(new ImageIcon(getClass().getResource("text/images/plugin.png")));
		tb.add(button);
		Modeler.setToolBarButton(button);

		return tb;

	}

	private ColorMenu createColorMenu() {
		final ColorMenu colorMenu = new ColorMenu(this, "Background", ModelerUtilities.colorChooser,
				ModelerUtilities.fillEffectChooser);
		colorMenu.addNoFillListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				page.changeFillMode(FillMode.getNoFillMode());
			}
		});
		colorMenu.addColorArrayListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				page.changeFillMode(new FillMode.ColorFill(colorMenu.getColor()));
			}
		});
		colorMenu.addMoreColorListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				page.changeFillMode(new FillMode.ColorFill(colorMenu.getColorChooser().getColor()));
			}
		});
		colorMenu.addHexColorListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Color c = colorMenu
						.getHexInputColor(page.getFillMode() instanceof FillMode.ColorFill ? ((FillMode.ColorFill) page
								.getFillMode()).getColor() : null);
				if (c == null)
					return;
				page.changeFillMode(new FillMode.ColorFill(c));
			}
		});
		colorMenu.addFillEffectListeners(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				page.changeFillMode(colorMenu.getFillEffectChooser().getFillMode());
			}
		}, null);
		colorMenu.getPopupMenu().addPopupMenuListener(new PopupMenuListener() {
			public void popupMenuCanceled(PopupMenuEvent e) {
			}

			public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
			}

			public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
				colorMenu.setColor(page.getBackground());
			}
		});
		return colorMenu;
	}

	private JToolBar createToolBar2() {

		JToolBar tb = new JToolBar(SwingConstants.HORIZONTAL);
		tb.setOpaque(false);
		tb.setFloatable(false);
		tb.setLayout(new FlowLayout(FlowLayout.LEFT, 2, 1));
		tb.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
		tb.putClientProperty("JToolBar.isRollover", Boolean.TRUE);
		if (!Modeler.isMac())
			tb.add(new JLabel(toolBarHeaderIcon));

		Initializer.sharedInstance().setMessage("Reading system fonts...");
		fontNameComboBox = ModelerUtilities.createFontNameComboBox();
		fontNameComboBox.setOpaque(false);
		fontNameComboBox.addActionListener(fontFamilyAction);
		tb.add(fontNameComboBox);
		Initializer.sharedInstance().setMessage("Creating Editor's tool bar 2...");

		fontSizeComboBox = ModelerUtilities.createFontSizeComboBox();
		fontSizeComboBox.setOpaque(false);
		fontSizeComboBox.addActionListener(fontSizeAction);
		tb.add(fontSizeComboBox);

		fontColorComboBox = new ColorComboBox(this);
		fontColorComboBox.setOpaque(false);
		fontColorComboBox.setRenderer(new ComboBoxRenderer.ColorCell());
		fontColorComboBox.setToolTipText("Font color");
		fontColorComboBox.setPreferredSize(new Dimension(80, fontSizeComboBox.getPreferredSize().height));
		fontColorComboBox.setRequestFocusEnabled(false);
		fontColorComboBox.addActionListener(fontColorAction);
		tb.add(fontColorComboBox);

		boldCheckBox = new JCheckBox(page.getAction(Page.BOLD));
		boldCheckBox.setIcon(new ImageIcon(getClass().getResource("text/images/bold.png")));
		boldCheckBox.setText(null);
		boldCheckBox.setToolTipText("Bold");
		boldCheckBox.addItemListener(boldAction);
		tb.add(boldCheckBox);
		Modeler.setToolBarButton(boldCheckBox);

		italicCheckBox = new JCheckBox(page.getAction(Page.ITALIC));
		italicCheckBox.setIcon(new ImageIcon(getClass().getResource("text/images/italic.png")));
		italicCheckBox.setText(null);
		italicCheckBox.setToolTipText("Italic");
		italicCheckBox.addItemListener(italicAction);
		tb.add(italicCheckBox);
		Modeler.setToolBarButton(italicCheckBox);

		underlineCheckBox = new JCheckBox(page.getAction(Page.UNDERLINE));
		underlineCheckBox.setIcon(new ImageIcon(getClass().getResource("text/images/underline.png")));
		underlineCheckBox.setText(null);
		underlineCheckBox.setToolTipText("Underline");
		underlineCheckBox.addItemListener(underlineAction);
		tb.add(underlineCheckBox);
		Modeler.setToolBarButton(underlineCheckBox);
		tb.addSeparator();

		tb.add(new JLabel(Modeler.toolBarSeparatorIcon));

		ButtonGroup bg = new ButtonGroup();

		leftAlignmentCheckBox = new JCheckBox(page.getAction(Page.LEFT_ALIGN));
		leftAlignmentCheckBox.setIcon(new ImageIcon(getClass().getResource("text/images/align_left.png")));
		leftAlignmentCheckBox.setText(null);
		leftAlignmentCheckBox.setToolTipText("Align paragraph left");
		leftAlignmentCheckBox.addItemListener(leftAlignmentAction);
		tb.add(leftAlignmentCheckBox);
		bg.add(leftAlignmentCheckBox);
		Modeler.setToolBarButton(leftAlignmentCheckBox);

		centerAlignmentCheckBox = new JCheckBox(page.getAction(Page.CENTER_ALIGN));
		centerAlignmentCheckBox.setIcon(new ImageIcon(getClass().getResource("text/images/align_center.png")));
		centerAlignmentCheckBox.setText(null);
		centerAlignmentCheckBox.setToolTipText("Align paragraph center");
		centerAlignmentCheckBox.addItemListener(centerAlignmentAction);
		tb.add(centerAlignmentCheckBox);
		bg.add(centerAlignmentCheckBox);
		Modeler.setToolBarButton(centerAlignmentCheckBox);

		rightAlignmentCheckBox = new JCheckBox(page.getAction(Page.RIGHT_ALIGN));
		rightAlignmentCheckBox.setIcon(new ImageIcon(getClass().getResource("text/images/align_right.png")));
		rightAlignmentCheckBox.setText(null);
		rightAlignmentCheckBox.setToolTipText("Align paragraph right");
		rightAlignmentCheckBox.addItemListener(rightAlignmentAction);
		tb.add(rightAlignmentCheckBox);
		bg.add(rightAlignmentCheckBox);
		Modeler.setToolBarButton(rightAlignmentCheckBox);
		tb.addSeparator();

		bulletCheckBox = new JCheckBox(page.getAction(Page.BULLET));
		bulletCheckBox.setIcon(new ImageIcon(getClass().getResource("text/images/add_bullets.png")));
		bulletCheckBox.setText(null);
		bulletCheckBox.setToolTipText("Add bullet to paragraph");
		bulletCheckBox.addItemListener(bulletAction);
		tb.add(bulletCheckBox);
		Modeler.setToolBarButton(bulletCheckBox);

		JButton button = new JButton(page.getAction(Page.INCREASE_INDENT));
		button.setIcon(new ImageIcon(getClass().getResource("text/images/increase_indentation.png")));
		button.setText(null);
		button.setToolTipText("Increase indentation");
		tb.add(button);
		Modeler.setToolBarButton(button);

		button = new JButton(page.getAction(Page.DECREASE_INDENT));
		button.setIcon(new ImageIcon(getClass().getResource("text/images/decrease_indentation.png")));
		button.setText(null);
		button.setToolTipText("Decrease indentation");
		tb.add(button);
		Modeler.setToolBarButton(button);

		tb.add(new JLabel(Modeler.toolBarSeparatorIcon));

		button = new JButton(page.getAction("Hyperlink"));
		if (Modeler.showToolBarText) {
			String s = Modeler.getInternationalText("HyperlinkButton");
			if (s != null)
				button.setText(s);
		}
		addEnabledComponentWhenEditable(button);
		tb.add(button);
		Modeler.setToolBarButton(button);

		return tb;

	}

	// Getting the attributes of the element at the given position for the first time is quite slow.
	private void showAttributes(int i) {
		int j = i > 0 ? i - 1 : 0;
		boolean b = false;
		try {
			b = page.getDocument().getText(j, 1).charAt(0) == '\n';
		}
		catch (BadLocationException e) {
		}
		final int p = b ? i : j;
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				if (page == null)
					return;
				AttributeSet a = page.getStyledDocument().getCharacterElement(p).getAttributes();
				showCharacterAttributes(a);
				Element elem = page.getStyledDocument().getParagraphElement(p);
				a = elem.getAttributes();
				showParagraphAttributes(a, elem);
			}
		});
	}

	private void showCharacterAttributes(final AttributeSet a) {
		String name = StyleConstants.getFontFamily(a);
		int k = 0;
		for (int i = 0; i < ModelerUtilities.FONT_FAMILY_NAMES.length; i++) {
			if (name.equals(ModelerUtilities.FONT_FAMILY_NAMES[i])) {
				k = i;
				break;
			}
		}
		fontNameComboBox.removeActionListener(fontFamilyAction);
		fontNameComboBox.setSelectedIndex(k);
		fontNameComboBox.addActionListener(fontFamilyAction);
		int size = StyleConstants.getFontSize(a);
		for (int i = 0; i < ModelerUtilities.FONT_SIZE.length; i++) {
			if (size == ModelerUtilities.FONT_SIZE[i].intValue()) {
				k = i;
				break;
			}
		}
		fontSizeComboBox.removeActionListener(fontSizeAction);
		fontSizeComboBox.setSelectedIndex(k);
		fontSizeComboBox.addActionListener(fontSizeAction);
		Color foreground = StyleConstants.getForeground(a);
		k = ColorRectangle.COLORS.length;
		for (int i = 0; i < ColorRectangle.COLORS.length; i++) {
			if (foreground.equals(ColorRectangle.COLORS[i])) {
				k = i;
				break;
			}
		}
		if (k == ColorRectangle.COLORS.length) {
			((ColorRectangle) fontColorComboBox.getRenderer()).setMoreColor(foreground);
		}
		fontColorComboBox.removeActionListener(fontColorAction);
		fontColorComboBox.setSelectedIndex(k);
		fontColorComboBox.addActionListener(fontColorAction);
		signifyChanges = false;
		boldCheckBox.setSelected(StyleConstants.isBold(a));
		italicCheckBox.setSelected(StyleConstants.isItalic(a));
		underlineCheckBox.setSelected(StyleConstants.isUnderline(a));
		signifyChanges = true;
	}

	private void showParagraphAttributes(AttributeSet a, Element e) {
		signifyChanges = false;
		switch (StyleConstants.getAlignment(a)) {
		case StyleConstants.ALIGN_LEFT:
			leftAlignmentCheckBox.setSelected(true);
			break;
		case StyleConstants.ALIGN_CENTER:
			centerAlignmentCheckBox.setSelected(true);
			break;
		case StyleConstants.ALIGN_RIGHT:
			rightAlignmentCheckBox.setSelected(true);
			break;
		}
		if (e != null) {
			Element elem = page.getStyledDocument().getCharacterElement(e.getStartOffset());
			Icon icon = StyleConstants.getIcon(elem.getAttributes());
			bulletCheckBox.setSelected(icon instanceof BulletIcon);
		}
		signifyChanges = true;
	}

	void precache(String stuff, String message) {
		if (Zipper.sharedInstance().getProcessMonitor() == null) {
			ProcessMonitor m = new ProcessMonitor(JOptionPane.getFrameForComponent(this));
			m.getProgressBar().setMinimum(0);
			m.getProgressBar().setMaximum(100);
			m.getProgressBar().setPreferredSize(new Dimension(300, 20));
			Zipper.sharedInstance().setProcessMonitor(m);
		}
		Zipper.sharedInstance().getProcessMonitor().setTitle(message);
		Zipper.sharedInstance().getProcessMonitor().setLocationRelativeTo(this);
		URL url = null;
		try {
			url = new URL(Modeler.getStaticRoot());
		}
		catch (MalformedURLException me) {
			me.printStackTrace();
			return;
		}
		File dir = new File(ConnectionManager.getCacheDirectory(), ConnectionManager.convertURLToFileName(url));
		Zipper.sharedInstance().unzipInAThread(Modeler.getStaticRoot() + stuff, dir);
	}

	void precache(String[] address, String[] message) {
		if (Zipper.sharedInstance().getProcessMonitor() == null) {
			ProcessMonitor m = new ProcessMonitor(JOptionPane.getFrameForComponent(this));
			m.getProgressBar().setMinimum(0);
			m.getProgressBar().setMaximum(100);
			m.getProgressBar().setPreferredSize(new Dimension(300, 20));
			Zipper.sharedInstance().setProcessMonitor(m);
		}
		Zipper.sharedInstance().getProcessMonitor().setTitle("Prefetch all");
		Zipper.sharedInstance().getProcessMonitor().setLocationRelativeTo(this);
		URL url = null;
		try {
			url = new URL(Modeler.getStaticRoot());
		}
		catch (MalformedURLException me) {
			me.printStackTrace();
			return;
		}
		File dir = new File(ConnectionManager.getCacheDirectory(), ConnectionManager.convertURLToFileName(url));
		for (int i = 0; i < address.length; i++)
			address[i] = Modeler.getStaticRoot() + address[i];
		Zipper.sharedInstance().unzipInAThread(address, message, dir);
	}

	private void validateComponent(Object src, final boolean changed) {
		if (!EventQueue.isDispatchThread())
			throw new RuntimeException("thread hazard");
		boolean onPage = false;
		MDContainer m = null;
		synchronized (componentPool) {
			for (ModelCanvas c : componentPool.getModels()) {
				if (!c.isUsed())
					continue;
				m = c.getMdContainer();
				if (m.getModel() == src) {
					// prepare to rearrange tool bar buttons
					Component[] but = null;
					if (m.hasToolbar()) {
						Component[] b1, b2 = null;
						b1 = m.getToolBar().getComponents();
						if (m.getExpandMenu() != null)
							b2 = m.getExpandMenu().getComponents();
						if (b2 != null) {
							but = new Component[b1.length + b2.length];
							System.arraycopy(b1, 0, but, 0, b1.length);
							System.arraycopy(b2, 0, but, b1.length, b2.length);
						}
						else {
							but = b1;
						}
						m.removeToolbar();
					}
					// The following lines are responsible for setting the bottom bar below the model's view.
					if (m instanceof AtomContainer) {
						String dnaString = ((AtomContainer) m).getDNAString();
						((AtomContainer) m).enableDNAScroller(dnaString != null);
						if (dnaString == null) {
							m.enableRecorder(!m.getModel().getRecorderDisabled());
						}
						else {
							((AtomContainer) m).setDNAScrollerColorScheme();
						}
					}
					else {
						m.enableRecorder(!m.getModel().getRecorderDisabled());
					}
					if (m.isStatusBarShown()) {
						m.addBottomBar();
					}
					else {
						m.removeBottomBar();
					}
					if (but != null) {
						// rearrange toolbar buttons according to current container size
						for (Component i : but) {
							if (!(i instanceof AbstractButton))
								continue;
							Action a = ((AbstractButton) i).getAction();
							if (a != null) {
								String s = (String) a.getValue(Action.NAME);
								m.addToolBarButton(s);
							}
						}
					}
					c.setMaximumSize(c.getPreferredSize());
					c.setMinimumSize(c.getMaximumSize());
					c.validate();
					if (!c.getSize().equals(c.getPreferredSize()))
						page.settleComponentSize();
					((MDView) c.getMdContainer().getModel().getView()).clearEditor(true);
					onPage = true;
				}
			}
		}
		if (page.getSaveReminder() != null) {
			if (onPage && changed) {
				page.getSaveReminder().setChanged(true);
			}
			else {
				// NOTE: temporary solution for setting the reminder's status. Seems there is something somewhere
				// in the event queue that signals the "changed" status. This fix appends a runnable that sets
				// the flag to false at the end of the event queue. This bug is hard to trace down because
				// there are too many places that signal the reminder.
				EventQueue.invokeLater(new Runnable() {
					public void run() {
						page.getSaveReminder().setChanged(false);
					}
				});
			}
		}
	}

	public void pageComponentChanged(PageComponentEvent e) {

		final Object src = e.getSource();

		switch (e.getID()) {

		case PageComponentEvent.COMPONENT_RESIZED:
			EventQueue.invokeLater(new Runnable() { // put into EventQueue in case ......
						public void run() {
							validateComponent(src, true);
						}
					});
			break;

		case PageComponentEvent.COMPONENT_LOADED:
			EventQueue.invokeLater(new Runnable() {
				public void run() {
					validateComponent(src, false);
					// initialization scripts are only run when the whole page is loaded.
					if (!loadModelAndPageList.contains(src)) {
						// ensure that the initialization script for each model is run ONLY once.
						loadModelAndPageList.add(src);
						if (src instanceof MDModel) {
							final String script = ((MDModel) src).getInitializationScript();
							if (script != null) {
								EventQueue.invokeLater(new Runnable() {
									public void run() {
										((MDModel) src).setInitializationScriptToRun(true);
										((MDModel) src).runScript(script);
									}
								});
							}
						}
						else if (src instanceof PageMd3d) {
							final String script = ((PageMd3d) src).getMolecularModel().getInitializationScript();
							if (script != null) {
								EventQueue.invokeLater(new Runnable() {
									public void run() {
										((PageMd3d) src).setInitializationScriptToRun(true);
										((PageMd3d) src).runScript(script);
									}
								});
							}
						}
					}
				}
			});
			break;

		// Reset action disables reminder. Need this call to enable it.
		case PageComponentEvent.COMPONENT_RESET:
			EventQueue.invokeLater(new Runnable() {
				public void run() {
					resetControllerStates(src);
					if (page.getSaveReminder() != null)
						page.getSaveReminder().setChanged(true);
				}
			});
			break;

		case PageComponentEvent.SNAPSHOT_TAKEN:
			EventQueue.invokeLater(new Runnable() {
				public void run() {
					SnapshotGallery.sharedInstance().takeSnapshot(page.getAddress(), src);
				}
			});
			break;

		case PageComponentEvent.SNAPSHOT_TAKEN_NODESCRIPTION:
			EventQueue.invokeLater(new Runnable() {
				public void run() {
					SnapshotGallery.sharedInstance().takeSnapshot(page.getAddress(), src, false);
				}
			});
			break;

		}

	}

	private void resetControllerStates(Object src) {
		if (!(src instanceof BasicModel))
			return;
		BasicModel model = (BasicModel) src;
		List<ModelListener> listener = model.getModelListeners();
		if (listener == null || listener.isEmpty())
			return;
		List<Embeddable> list = page.getEmbeddedComponents();
		if (list == null || list.isEmpty())
			return;
		for (Embeddable x : list) {
			if (listener.contains(x)) {
				if (x instanceof JToggleButton) {
					JToggleButton tb = (JToggleButton) x;
					Object o = tb.getClientProperty("selected");
					ModelerUtilities.setWithoutNotifyingListeners(tb, o == Boolean.TRUE);
					// some scripts might run for a long time, I am not sure if we should let them
					// tb.setSelected(o == Boolean.TRUE);
				}
				else if (x instanceof JSlider) {
					JSlider sl = (JSlider) x;
					Object o = sl.getClientProperty("value");
					if (o instanceof Integer) {
						ModelerUtilities.adjustWithoutNotifyingListeners(sl, (((Integer) o)).intValue());
						// some scripts might run for a long time, I am not sure if we should let them
						// sl.setValue((Integer) o);
					}
				}
				else if (x instanceof PageSpinner) {
					PageSpinner sp = (PageSpinner) x;
					Object o = sp.getClientProperty("value");
					if (o instanceof Double)
						sp.setValue((Double) o);
				}
				else if (x instanceof JComboBox) {
					JComboBox cb = (JComboBox) x;
					Object o = cb.getClientProperty("Selected Index");
					if (o instanceof Integer) {
						ModelerUtilities.selectWithoutNotifyingListeners(cb, (((Integer) o)).intValue());
						// some scripts might run for a long time, I am not sure if we should let them
						// cb.setSelectedIndex((Integer) o);
					}
				}
			}
		}
	}

	private void setToolbarButtons() {
		if (Page.isApplet())
			return;
		if (page.isRemote()) {
			if (editCheckBox.isSelected()) {
				setEditable(false);
				editCheckBox.requestFocusInWindow();
			}
			statusBar.showWebPageStatus();
			enableActions(false);
			toolBar[0].add(submitCommentButton);
			toolBar[0].add(viewCommentButton);
			toolBar[0].add(mwSpaceButton);
			toolBar[0].remove(editCheckBox);
		}
		else {
			statusBar.showLocalPageStatus();
			enableActions(editCheckBox.isSelected());
			if (isEditable())
				showAttributes(0);
			toolBar[0].remove(submitCommentButton);
			toolBar[0].remove(viewCommentButton);
			toolBar[0].remove(mwSpaceButton);
			toolBar[0].add(editCheckBox, Modeler.isMac() ? 0 : 1);
		}
		toolBar[0].validate();
		toolBar[0].repaint();
		if (!initialized) {
			initialized = true;
			enableDisabledComponentsWhileLoading(true);
		}
	}

	private void enableDisabledComponentsWhileLoading(boolean b) {
		if (!EventQueue.isDispatchThread())
			throw new RuntimeException("Called in an unsafe thread");
		for (Component c : disabledComponentsWhileLoading) {
			if (c instanceof AbstractButton) {
				Action a = ((AbstractButton) c).getAction();
				if (a != null) {
					Object o = a.getValue("enabled");
					if (o instanceof Boolean) {
						if ((Boolean) o)
							c.setEnabled(b);
					}
					else {
						c.setEnabled(b);
					}
				}
				else {
					c.setEnabled(b);
				}
			}
			else {
				c.setEnabled(b);
			}
		}
	}

	public void pageUpdate(PageEvent e) {

		switch (e.getType()) {

		case PageEvent.NEW_PAGE:
			EventQueue.invokeLater(new Runnable() {
				public void run() {
					showAttributes(0);
					setEditable(true);
					statusBar.showNewPageStatus();
					toolBar[0].remove(submitCommentButton);
					toolBar[0].remove(viewCommentButton);
					toolBar[0].remove(mwSpaceButton);
					toolBar[0].add(editCheckBox, 0);
				}
			});
			break;

		case PageEvent.PAGE_READ_BEGIN:
			EventQueue.invokeLater(new Runnable() {
				public void run() {
					enableDisabledComponentsWhileLoading(false);
				}
			});
			break;

		case PageEvent.PAGE_READ_END:
			loadModelAndPageList.clear();
			EventQueue.invokeLater(new Runnable() {
				public void run() {
					if (!Page.isApplet()) {
						setToolbarButtons();
						enableDisabledComponentsWhileLoading(true);
						statusBar.getProgressBar().setIndeterminate(false);
						statusBar.tipBar.setText(editCheckBox.isSelected() ? "Editing" : "Loaded");
					}
					page.requestFocusInWindow();
					setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
					page.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
				}
			});
			break;

		case PageEvent.STORE_VIEW_POSITION:
			if (EventQueue.isDispatchThread()) {
				Point p = getViewPosition();
				if (p.x == 0 && p.y == 0) {
					positionMap.remove(page.getAddress());
				}
				else {
					positionMap.put(page.getAddress(), p);
				}
			}
			break;

		case PageEvent.RESTORE_VIEW_POSITION:
			EventQueue.invokeLater(new Runnable() {
				public void run() {
					Point p = positionMap.get(page.getAddress());
					if (p != null)
						setViewPosition(p);
					EventQueue.invokeLater(new Runnable() {
						public void run() {
							page.deselect();
						}
					});
				}
			});
			break;

		case PageEvent.PAGE_WRITE_BEGIN:
		case PageEvent.PAGE_OVERWRITE_BEGIN:
			EventQueue.invokeLater(new Runnable() {
				public void run() {
					enableDisabledComponentsWhileLoading(false);
				}
			});
			break;

		case PageEvent.PAGE_WRITE_END:
		case PageEvent.PAGE_OVERWRITE_END:
			EventQueue.invokeLater(new Runnable() {
				public void run() {
					setToolbarButtons();
					enableDisabledComponentsWhileLoading(true);
					statusBar.showLocalPageStatus();
				}
			});
			break;

		case PageEvent.FONT_CHANGED:
			EventQueue.invokeLater(new Runnable() {
				public void run() {
					showAttributes(page.getSelectionEnd());
				}
			});
			break;

		case PageEvent.LOAD_ERROR:
			if (page.getSaveReminder() != null)
				page.getSaveReminder().setChanged(false);
			final String s = e.getDescription();
			EventQueue.invokeLater(new Runnable() {
				public void run() {
					if (statusBar != null)
						statusBar.getProgressBar().setIndeterminate(false);
					try {
						JOptionPane.getFrameForComponent(Editor.this).setTitle(
								s + " - " + Modeler.NAME + " V" + Modeler.VERSION);
					}
					catch (NullPointerException npe) {
						// If there is no parent frame, skip
					}
				}
			});
			if (s.equalsIgnoreCase("failed in connecting") || s.equalsIgnoreCase("unknown host")
					|| s.equalsIgnoreCase("no route to host")) {
				if (page.getAddress().indexOf(Modeler.getStaticRoot() + "tutorial/") != -1) {
					EventQueue.invokeLater(new Runnable() {
						public void run() {
							JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(Editor.this),
									"To download the User's Manual for offline use,\n"
											+ "connect to the Internet, select the Option Menu, and\n"
											+ "select The User's Manual from the Prefetch to Cache\n"
											+ "Submenu. Wait until the download process is complete,\n"
											+ "then reload the page.", "Needs to download the User's Manual",
									JOptionPane.ERROR_MESSAGE);
						}
					});
				}
			}
			break;

		}

	}

	public void removeAllToolBars() {
		remove(toolBarPanel);
		validate();
	}

	private void setToolBar(boolean editable) {
		if (editable) {
			editCheckBox.setIcon(viewIcon);
			if (Modeler.showToolBarText) {
				String s = Modeler.getInternationalText("ViewingMode");
				editCheckBox.setText(s != null ? s : "View");
			}
			editCheckBox.setToolTipText("Go to the viewing mode");
			toolBarPanel.add(toolBar[1], BorderLayout.SOUTH);
			toolBar[0].add(toolBar[2]);
		}
		else {
			editCheckBox.setIcon(editIcon);
			if (Modeler.showToolBarText) {
				String s = Modeler.getInternationalText("EditingMode");
				editCheckBox.setText(s != null ? s : "Edit");
			}
			editCheckBox.setToolTipText("Go to the editing mode");
			toolBarPanel.remove(toolBar[1]);
			toolBar[0].remove(toolBar[2]);
		}
		validate();
	}

}
