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
import java.awt.FontMetrics;
import java.awt.GridLayout;
import java.awt.Insets;
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
import org.concord.modeler.event.PageComponentEvent;
import org.concord.modeler.event.PageComponentListener;
import org.concord.modeler.event.PageEvent;
import org.concord.modeler.event.PageListener;
import org.concord.modeler.text.BulletIcon;
import org.concord.modeler.text.Page;
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

	final static Insets ZERO_INSETS = new Insets(0, 0, 0, 0);

	private static StyledEditorKit.FontSizeAction[] fsa;
	private static StyledEditorKit.ForegroundAction[] fga;
	private static StyledEditorKit.FontFamilyAction[] ffa;

	private static final Border SELECTED_BORDER = new BasicBorders.ButtonBorder(Color.lightGray, Color.white,
			Color.black, Color.gray);
	private final static Dimension SCREEN_SIZE = Toolkit.getDefaultToolkit().getScreenSize();
	private final static Dimension BUTTON_DIMENSION = new Dimension(24, 24);
	private static ImageIcon noEditIcon, editIcon;
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
	private JPanel toolBarPanel;
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

		init();

		setStatusBar(statusBar);

		if (actionNotifier == null) {
			actionNotifier = new ActionNotifier(this);
		}
		else {
			actionNotifier.setParentComponent(this);
		}

		if (noEditIcon == null)
			noEditIcon = new ImageIcon(getClass().getResource("images/NoEdit.gif"));
		if (editIcon == null)
			editIcon = new ImageIcon(getClass().getResource("images/Edit.gif"));

		desktopPane = new JDesktopPane();
		desktopPane.setBackground(Color.white);
		desktopPane.setDragMode(JDesktopPane.OUTLINE_DRAG_MODE);

		page = new Page();
		page.setEditor(this);
		page.setURLDisplay(statusBar.tipBar);
		page.addPageListener(this);
		page.setProgressBar(statusBar.getProgressBar());
		page.setActionNotifier(actionNotifier);
		page.addCaretListener(new CaretListener() {
			public void caretUpdate(CaretEvent e) {
				if (page.isEditable())
					showAttributes(e.getDot());
			}
		});

		componentPool = new ComponentPool(this);
		componentPool.setSelectionColor(page.getSelectionColor());
		page.setComponentPool(componentPool);

		scroller = new JScrollPane(page, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
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
		desktopHostPanel.setBorder(BorderFactory.createLoweredBevelBorder());
		desktopHostPanel.add(desktopPane, BorderLayout.CENTER);
		add(desktopHostPanel, BorderLayout.CENTER);

		enabledComponentsWhenEditable = new ArrayList<Component>();
		enabledComponentsWhenNotEditable = new ArrayList<Component>();
		disabledComponentsWhileLoading = new ArrayList<Component>();

		page.getInputMap().put((KeyStroke) fullScreenAction.getValue(Action.ACCELERATOR_KEY), "Full Screen");
		page.getActionMap().put("Full Screen", fullScreenAction);
		page.putActivityAction(openSnapshotGallery);

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
					editCheckBox.setToolTipText("Set editable");
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

	/**
	 * When the current instance is closed, destroy this component to prevent memory leak.
	 */
	public void destroy() {

		enabledComponentsWhenEditable.clear();
		enabledComponentsWhenNotEditable.clear();
		disabledComponentsWhileLoading.clear();
		getActionMap().clear();
		getInputMap().clear();

		SnapshotGallery.sharedInstance().removeFlashComponent(snapshotButton);
		SnapshotGallery.sharedInstance().setOwner(null);
		actionNotifier.setParentComponent(null);

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

		toolBarPanel = new JPanel(new BorderLayout(0, 0));
		toolBarPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
		toolBarPanel.add(toolBar[0], BorderLayout.CENTER);

		add(toolBarPanel, BorderLayout.NORTH);

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
		if (!Modeler.isMac())
			button.setBorderPainted(false);
		button.setMargin(ZERO_INSETS);
		button.setToolTipText("Exit the full screen mode");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				windowScreen();
			}
		});
		p.add(button);

		button = new JButton(page.getNavigator().getAction(Navigator.BACK));
		button.setText(null);
		if (!Modeler.isMac())
			button.setBorderPainted(false);
		button.setMargin(ZERO_INSETS);
		p.add(button);

		button = new JButton(page.getNavigator().getAction(Navigator.FORWARD));
		button.setText(null);
		if (!Modeler.isMac())
			button.setBorderPainted(false);
		button.setMargin(ZERO_INSETS);
		p.add(button);

		button = new JButton(page.getAction(Page.REFRESH));
		button.setText(null);
		if (!Modeler.isMac())
			button.setBorderPainted(false);
		button.setMargin(ZERO_INSETS);
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
				updateActions(b);
				editCheckBox.setToolTipText(b ? "Click to protect this page from changing"
						: "Click to make this page editable");
				statusBar.tipBar.setText(b ? "Editor mode" : "Viewer mode");
				notifyEditorListeners(new EditorEvent(Editor.this, b ? EditorEvent.EDITOR_ENABLED
						: EditorEvent.EDITOR_DISABLED));
			}
		});
	}

	private void updateActions(boolean b) {
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
		tb.setFloatable(false);
		tb.setMargin(new Insets(1, 1, 1, 1));
		tb.setLayout(new FlowLayout(FlowLayout.LEFT, 2, 1));
		tb.setBorder(BorderFactory.createEtchedBorder());
		tb.putClientProperty("JToolBar.isRollover", Boolean.TRUE);
		if (!Modeler.isMac())
			tb.add(new JLabel(toolBarHeaderIcon));

		editCheckBox = new JCheckBox(noEditIcon);
		editCheckBox.setHorizontalAlignment(SwingConstants.CENTER);
		if (Modeler.showToolBarText) {
			String s = Modeler.getInternationalText("EditCheckBox");
			editCheckBox.setText(s != null ? s : "Editor");
			editCheckBox.setMargin(ZERO_INSETS);
			FontMetrics fm = editCheckBox.getFontMetrics(editCheckBox.getFont());
			int w = fm.stringWidth(editCheckBox.getText());
			w += editCheckBox.getIconTextGap();
			w += editCheckBox.getIcon().getIconWidth();
			w += editCheckBox.getMargin().left + editCheckBox.getMargin().right;
			editCheckBox.setPreferredSize(new Dimension(w + 10, BUTTON_DIMENSION.height));
		}
		else {
			editCheckBox.setPreferredSize(BUTTON_DIMENSION);
			editCheckBox.setMargin(ZERO_INSETS);
		}
		if (!Modeler.isMac()) {
			editCheckBox.setBorderPainted(false);
			editCheckBox.setFocusPainted(false);
		}
		editCheckBox.setToolTipText("Click to make this page editable");
		editCheckBox.setSelected(false);
		// editCheckBox.setRequestFocusEnabled(false); // this will disable the caret
		editCheckBox.addActionListener(lockAction);
		tb.add(editCheckBox);
		if (Modeler.windowCount == 0)
			editCheckBox.setEnabled(false);
		addDisabledComponentWhileLoading(editCheckBox);
		// if(Modeler.showToolBarText) tb.add(new JLabel(Modeler.toolBarSeparatorIcon));

		JButton button = new JButton(page.getAction(Page.OPEN_PAGE));
		button.setHorizontalAlignment(SwingConstants.CENTER);
		button.setIcon(new ImageIcon(getClass().getResource("images/open.gif")));
		if (Modeler.showToolBarText) {
			String s = Modeler.getInternationalText("OpenButton");
			if (s != null)
				button.setText(s);
			button.setMargin(ZERO_INSETS);
			int w = button.getFontMetrics(button.getFont()).stringWidth(button.getText());
			w += button.getIconTextGap();
			w += button.getIcon().getIconWidth();
			button.setPreferredSize(new Dimension(w + 10, BUTTON_DIMENSION.height));
		}
		else {
			button.setText(null);
			button.setPreferredSize(BUTTON_DIMENSION);
			button.setMargin(ZERO_INSETS);
		}
		if (!Modeler.isMac()) {
			button.setBorderPainted(false);
			button.setFocusPainted(false);
		}
		button.setRequestFocusEnabled(false);
		if (Modeler.windowCount == 0)
			button.setEnabled(false);
		addDisabledComponentWhileLoading(button);
		tb.add(button);

		button = new JButton(page.getAction(Page.SAVE_PAGE));
		button.setHorizontalAlignment(SwingConstants.CENTER);
		button.setIcon(new ImageIcon(getClass().getResource("images/save.gif")));
		if (Modeler.showToolBarText) {
			String s = Modeler.getInternationalText("SaveButton");
			if (s != null)
				button.setText(s);
			button.setMargin(ZERO_INSETS);
			int w = button.getFontMetrics(button.getFont()).stringWidth(button.getText());
			w += button.getIconTextGap();
			w += button.getIcon().getIconWidth();
			button.setPreferredSize(new Dimension(w + 10, BUTTON_DIMENSION.height));
		}
		else {
			button.setText(null);
			button.setPreferredSize(BUTTON_DIMENSION);
			button.setMargin(ZERO_INSETS);
		}
		if (!Modeler.isMac()) {
			button.setBorderPainted(false);
			button.setFocusPainted(false);
		}
		button.setRequestFocusEnabled(false);
		if (Modeler.windowCount == 0)
			button.setEnabled(false);
		addDisabledComponentWhileLoading(button);
		tb.add(button);

		button = new JButton(new ImageIcon(getClass().getResource("images/Search.gif")));
		button.setHorizontalAlignment(SwingConstants.CENTER);
		button.setPreferredSize(BUTTON_DIMENSION);
		button.setMargin(ZERO_INSETS);
		if (!Modeler.isMac()) {
			button.setBorderPainted(false);
			button.setFocusPainted(false);
		}
		button.setToolTipText("Find on this page");
		button.setRequestFocusEnabled(false);
		button.addActionListener(findAction);
		if (Modeler.windowCount == 0)
			button.setEnabled(false);
		addDisabledComponentWhileLoading(button);
		tb.add(button);

		button = new JButton(page.getAction("Print"));
		button.setHorizontalAlignment(SwingConstants.CENTER);
		button.setText(null);
		button.setIcon(new ImageIcon(getClass().getResource("images/printer2.gif")));
		button.setPreferredSize(BUTTON_DIMENSION);
		button.setMargin(ZERO_INSETS);
		if (!Modeler.isMac()) {
			button.setBorderPainted(false);
			button.setFocusPainted(false);
		}
		button.setRequestFocusEnabled(false);
		if (Modeler.windowCount == 0)
			button.setEnabled(false);
		addDisabledComponentWhileLoading(button);
		tb.add(button);

		snapshotButton = new JButton(new ImageIcon(getClass().getResource("images/Album.gif")));
		snapshotButton.setHorizontalAlignment(SwingConstants.CENTER);
		if (!Modeler.isMac()) {
			snapshotButton.setBorderPainted(false);
			snapshotButton.setFocusPainted(false);
		}
		snapshotButton.setRequestFocusEnabled(false);
		snapshotButton.setToolTipText("Open snapshot gallery");
		snapshotButton.addActionListener(openSnapshotGallery);
		if (Modeler.showToolBarText) {
			String s = Modeler.getInternationalText("OpenSnapshotGalleryButton");
			snapshotButton.setText(s != null ? s : "Snapshots");
			snapshotButton.setMargin(ZERO_INSETS);
			int w = snapshotButton.getFontMetrics(snapshotButton.getFont()).stringWidth(snapshotButton.getText());
			w += snapshotButton.getIconTextGap();
			w += snapshotButton.getIcon().getIconWidth();
			snapshotButton.setPreferredSize(new Dimension(w + 10, BUTTON_DIMENSION.height));
		}
		else {
			snapshotButton.setPreferredSize(BUTTON_DIMENSION);
			snapshotButton.setMargin(ZERO_INSETS);
		}
		tb.add(snapshotButton);
		SnapshotGallery.sharedInstance().addFlashComponent(snapshotButton);
		if (Modeler.windowCount == 0)
			snapshotButton.setEnabled(false);
		addDisabledComponentWhileLoading(snapshotButton);

		submitCommentButton = new JButton(new ImageIcon(getClass().getResource("images/EditComment.gif")));
		if (Modeler.windowCount == 0)
			submitCommentButton.setEnabled(false);
		String s = Modeler.getInternationalText("MakeCommentButton");
		submitCommentButton.setText(s != null ? s : "Comment");
		submitCommentButton.setHorizontalAlignment(SwingConstants.CENTER);
		submitCommentButton.setPreferredSize(BUTTON_DIMENSION);
		if (!Modeler.isMac()) {
			submitCommentButton.setBorderPainted(false);
			submitCommentButton.setFocusPainted(false);
		}
		submitCommentButton.setToolTipText("Make comments on current page");
		submitCommentButton.setRequestFocusEnabled(false);
		submitCommentButton.addActionListener(serverGate.commentAction);
		if (Modeler.showToolBarText) {
			submitCommentButton.setMargin(ZERO_INSETS);
			int w = submitCommentButton.getFontMetrics(submitCommentButton.getFont()).stringWidth(
					submitCommentButton.getText());
			w += submitCommentButton.getIconTextGap();
			w += submitCommentButton.getIcon().getIconWidth();
			submitCommentButton.setPreferredSize(new Dimension(w + 10, BUTTON_DIMENSION.height));
		}
		else {
			submitCommentButton.setText(null);
			submitCommentButton.setPreferredSize(BUTTON_DIMENSION);
			submitCommentButton.setMargin(ZERO_INSETS);
		}
		tb.add(submitCommentButton);
		addDisabledComponentWhileLoading(submitCommentButton);

		viewCommentButton = new JButton(new ImageIcon(getClass().getResource("images/ViewComment.gif")));
		if (Modeler.windowCount == 0)
			viewCommentButton.setEnabled(false);
		viewCommentButton.setHorizontalAlignment(SwingConstants.CENTER);
		viewCommentButton.setPreferredSize(BUTTON_DIMENSION);
		if (!Modeler.isMac()) {
			viewCommentButton.setBorderPainted(false);
			viewCommentButton.setFocusPainted(false);
		}
		viewCommentButton.setToolTipText("Show discussions about current page");
		viewCommentButton.setRequestFocusEnabled(false);
		viewCommentButton.addActionListener(serverGate.viewCommentAction);
		viewCommentButton.setText(null);
		viewCommentButton.setPreferredSize(BUTTON_DIMENSION);
		viewCommentButton.setMargin(ZERO_INSETS);
		tb.add(viewCommentButton);
		addDisabledComponentWhileLoading(viewCommentButton);

		mwSpaceButton = new JButton(new ImageIcon(getClass().getResource("images/webmw.gif")));
		if (Modeler.windowCount == 0)
			mwSpaceButton.setEnabled(false);
		s = Modeler.getInternationalText("MyMwSpace");
		mwSpaceButton.setText(s != null ? s : "My MW Space");
		mwSpaceButton.setHorizontalAlignment(SwingConstants.CENTER);
		mwSpaceButton.setPreferredSize(BUTTON_DIMENSION);
		if (!Modeler.isMac()) {
			mwSpaceButton.setBorderPainted(false);
			mwSpaceButton.setFocusPainted(false);
		}
		mwSpaceButton.setToolTipText("Click to enter my MW Space");
		mwSpaceButton.setRequestFocusEnabled(false);
		mwSpaceButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				page.getNavigator().visitLocation(Modeler.getContextRoot() + "myhome.jsp?client=mw");
			}
		});
		if (Modeler.showToolBarText) {
			mwSpaceButton.setMargin(ZERO_INSETS);
			int w = mwSpaceButton.getFontMetrics(mwSpaceButton.getFont()).stringWidth(mwSpaceButton.getText());
			w += mwSpaceButton.getIconTextGap();
			w += mwSpaceButton.getIcon().getIconWidth();
			mwSpaceButton.setPreferredSize(new Dimension(w + 10, BUTTON_DIMENSION.height));
		}
		else {
			mwSpaceButton.setText(null);
			mwSpaceButton.setPreferredSize(BUTTON_DIMENSION);
			mwSpaceButton.setMargin(ZERO_INSETS);
		}
		tb.add(mwSpaceButton);
		addDisabledComponentWhileLoading(mwSpaceButton);

		return tb;

	}

	private JToolBar createToolBar3() {

		JToolBar tb = new JToolBar(SwingConstants.HORIZONTAL);
		tb.setFloatable(false);
		tb.setMargin(new Insets(1, 1, 1, 1));
		tb.setLayout(new FlowLayout(FlowLayout.LEFT, 2, 1));
		tb.setBorder(BorderFactory.createEmptyBorder());
		if (!Modeler.isMac())
			tb.add(new JLabel(toolBarHeaderIcon));
		tb.putClientProperty("JToolBar.isRollover", Boolean.TRUE);

		cutButton = new JButton(page.getAction(DefaultEditorKit.cutAction));
		cutButton.setHorizontalAlignment(SwingConstants.CENTER);
		cutButton.setIcon(new ImageIcon(getClass().getResource("images/cut.gif")));
		cutButton.setText(null);
		cutButton.setPreferredSize(BUTTON_DIMENSION);
		cutButton.setMargin(ZERO_INSETS);
		if (!Modeler.isMac()) {
			cutButton.setBorderPainted(false);
			cutButton.setFocusPainted(false);
		}
		cutButton.setRequestFocusEnabled(false);
		cutButton.setToolTipText("Cut");
		tb.add(cutButton);

		copyButton = new JButton(page.getAction(DefaultEditorKit.copyAction));
		copyButton.setHorizontalAlignment(SwingConstants.CENTER);
		copyButton.setIcon(new ImageIcon(getClass().getResource("images/copy.gif")));
		copyButton.setText(null);
		copyButton.setPreferredSize(BUTTON_DIMENSION);
		copyButton.setMargin(ZERO_INSETS);
		if (!Modeler.isMac()) {
			copyButton.setBorderPainted(false);
			copyButton.setFocusPainted(false);
		}
		copyButton.setRequestFocusEnabled(false);
		copyButton.setToolTipText("Copy");
		tb.add(copyButton);

		pasteButton = new JButton(page.getAction(DefaultEditorKit.pasteAction));
		pasteButton.setHorizontalAlignment(SwingConstants.CENTER);
		pasteButton.setIcon(new ImageIcon(getClass().getResource("images/paste.gif")));
		pasteButton.setText(null);
		pasteButton.setPreferredSize(BUTTON_DIMENSION);
		pasteButton.setMargin(ZERO_INSETS);
		if (!Modeler.isMac()) {
			pasteButton.setBorderPainted(false);
			pasteButton.setFocusPainted(false);
		}
		pasteButton.setRequestFocusEnabled(false);
		pasteButton.setToolTipText("Paste");
		tb.add(pasteButton);

		JButton button = new JButton(page.getAction(Page.UNDO));
		button.setHorizontalAlignment(SwingConstants.CENTER);
		button.setIcon(new ImageIcon(getClass().getResource("images/Undo2.gif")));
		button.setText(null);
		button.setPreferredSize(BUTTON_DIMENSION);
		button.setMargin(ZERO_INSETS);
		if (!Modeler.isMac()) {
			button.setBorderPainted(false);
			button.setFocusPainted(false);
		}
		button.setRequestFocusEnabled(false);
		tb.add(button);

		button = new JButton(page.getAction(Page.REDO));
		button.setHorizontalAlignment(SwingConstants.CENTER);
		button.setText(null);
		button.setIcon(new ImageIcon(getClass().getResource("images/Redo2.gif")));
		button.setPreferredSize(BUTTON_DIMENSION);
		button.setMargin(ZERO_INSETS);
		if (!Modeler.isMac()) {
			button.setBorderPainted(false);
			button.setFocusPainted(false);
		}
		button.setRequestFocusEnabled(false);
		tb.add(button);

		tb.add(new JLabel(Modeler.toolBarSeparatorIcon));

		button = new JButton(new ImageIcon(getClass().getResource("images/SetColor.gif")));
		button.setHorizontalAlignment(SwingConstants.CENTER);
		button.setPreferredSize(BUTTON_DIMENSION);
		button.setMargin(ZERO_INSETS);
		if (!Modeler.isMac()) {
			button.setBorderPainted(false);
			button.setFocusPainted(false);
		}
		button.setToolTipText("Set background");
		button.setRequestFocusEnabled(false);
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (backgroundPopupMenu == null)
					backgroundPopupMenu = createColorMenu().getPopupMenu();
				backgroundPopupMenu.show((JButton) e.getSource(), 5, 10);
			}
		});
		tb.add(button);

		button = new JButton(new ImageIcon(getClass().getResource("images/InsertComponent.gif")));
		button.setHorizontalAlignment(SwingConstants.CENTER);
		button.setPreferredSize(BUTTON_DIMENSION);
		button.setMargin(ZERO_INSETS);
		if (!Modeler.isMac()) {
			button.setBorderPainted(false);
			button.setFocusPainted(false);
		}
		button.setToolTipText("Insert a model component");
		button.setRequestFocusEnabled(false);
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (insertComponentPopupMenu == null)
					insertComponentPopupMenu = new InsertComponentPopupMenu(page);
				insertComponentPopupMenu.show((JButton) e.getSource(), 5, 10);
			}
		});
		tb.add(button);

		inputImageButton = new JButton(page.getAction("Input Image"));
		inputImageButton.setIcon(new ImageIcon(getClass().getResource("images/InsertPicture.gif")));
		inputImageButton.setHorizontalAlignment(SwingConstants.CENTER);
		inputImageButton.setText(null);
		inputImageButton.setToolTipText("Insert an image");
		inputImageButton.setPreferredSize(BUTTON_DIMENSION);
		inputImageButton.setMargin(ZERO_INSETS);
		if (!Modeler.isMac()) {
			inputImageButton.setBorderPainted(false);
			inputImageButton.setFocusPainted(false);
		}
		inputImageButton.setRequestFocusEnabled(false);
		tb.add(inputImageButton);

		tb.add(new JLabel(Modeler.toolBarSeparatorIcon));

		button = new JButton(page.getAction(Page.INSERT_COMPONENT));
		button.setText(null);
		button.setName("Text Box");
		button.setToolTipText("Insert a text box");
		button.setIcon(new ImageIcon(getClass().getResource("text/images/TextBox.gif")));
		button.setHorizontalAlignment(SwingConstants.CENTER);
		button.setPreferredSize(BUTTON_DIMENSION);
		button.setMargin(ZERO_INSETS);
		if (!Modeler.isMac()) {
			button.setBorderPainted(false);
			button.setFocusPainted(false);
		}
		button.setRequestFocusEnabled(false);
		tb.add(button);

		button = new JButton(page.getAction(Page.INSERT_COMPONENT));
		button.setText(null);
		button.setName("Multiple Choice");
		button.setToolTipText("Insert a multiple choice");
		button.setIcon(new ImageIcon(getClass().getResource("text/images/MultipleChoice.gif")));
		button.setHorizontalAlignment(SwingConstants.CENTER);
		button.setPreferredSize(BUTTON_DIMENSION);
		button.setMargin(ZERO_INSETS);
		if (!Modeler.isMac()) {
			button.setBorderPainted(false);
			button.setFocusPainted(false);
		}
		button.setRequestFocusEnabled(false);
		tb.add(button);

		button = new JButton(page.getAction(Page.INSERT_COMPONENT));
		button.setIcon(new ImageIcon(getClass().getResource("text/images/ImageQuestion.gif")));
		button.setText(null);
		button.setName("Image Question");
		button.setToolTipText("Insert an image question");
		button.setHorizontalAlignment(SwingConstants.CENTER);
		button.setPreferredSize(BUTTON_DIMENSION);
		button.setMargin(ZERO_INSETS);
		if (!Modeler.isMac()) {
			button.setBorderPainted(false);
			button.setFocusPainted(false);
		}
		button.setRequestFocusEnabled(false);
		tb.add(button);

		button = new JButton(page.getAction(Page.INSERT_COMPONENT));
		button.setIcon(new ImageIcon(getClass().getResource("text/images/TextArea.gif")));
		button.setText(null);
		button.setName("User Input Text Area");
		button.setToolTipText("Insert a user-input text area");
		button.setHorizontalAlignment(SwingConstants.CENTER);
		button.setPreferredSize(BUTTON_DIMENSION);
		button.setMargin(ZERO_INSETS);
		if (!Modeler.isMac()) {
			button.setBorderPainted(false);
			button.setFocusPainted(false);
		}
		button.setRequestFocusEnabled(false);
		tb.add(button);

		button = new JButton(page.getAction(Page.INSERT_COMPONENT));
		button.setText(null);
		button.setName("Table");
		button.setToolTipText("Insert a table");
		button.setIcon(new ImageIcon(getClass().getResource("text/images/Table.gif")));
		button.setHorizontalAlignment(SwingConstants.CENTER);
		button.setPreferredSize(BUTTON_DIMENSION);
		button.setMargin(ZERO_INSETS);
		if (!Modeler.isMac()) {
			button.setBorderPainted(false);
			button.setFocusPainted(false);
		}
		button.setRequestFocusEnabled(false);
		tb.add(button);

		button = new JButton(page.getAction(Page.INSERT_COMPONENT));
		button.setText(null);
		button.setName("Applet");
		button.setToolTipText("Insert an applet");
		button.setIcon(new ImageIcon(getClass().getResource("text/images/Applet.gif")));
		button.setHorizontalAlignment(SwingConstants.CENTER);
		button.setPreferredSize(BUTTON_DIMENSION);
		button.setMargin(ZERO_INSETS);
		if (!Modeler.isMac()) {
			button.setBorderPainted(false);
			button.setFocusPainted(false);
		}
		button.setRequestFocusEnabled(false);
		tb.add(button);

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
		tb.setFloatable(false);
		tb.setMargin(new Insets(1, 1, 1, 1));
		tb.setLayout(new FlowLayout(FlowLayout.LEFT, 2, 1));
		tb.setBorder(BorderFactory.createEtchedBorder());
		tb.putClientProperty("JToolBar.isRollover", Boolean.TRUE);
		if (!Modeler.isMac())
			tb.add(new JLabel(toolBarHeaderIcon));

		Initializer.sharedInstance().setMessage("Reading system fonts...");
		fontNameComboBox = ModelerUtilities.createFontNameComboBox();
		fontNameComboBox.addActionListener(fontFamilyAction);
		tb.add(fontNameComboBox);
		Initializer.sharedInstance().setMessage("Creating Editor's tool bar 2...");

		fontSizeComboBox = ModelerUtilities.createFontSizeComboBox();
		fontSizeComboBox.addActionListener(fontSizeAction);
		tb.add(fontSizeComboBox);

		fontColorComboBox = new ColorComboBox(this);
		fontColorComboBox.setRenderer(new ComboBoxRenderer.ColorCell());
		fontColorComboBox.setToolTipText("Font color");
		fontColorComboBox.setPreferredSize(new Dimension(80, fontSizeComboBox.getPreferredSize().height));
		fontColorComboBox.setRequestFocusEnabled(false);
		fontColorComboBox.addActionListener(fontColorAction);
		tb.add(fontColorComboBox);

		boldCheckBox = new JCheckBox(page.getAction(Page.BOLD));
		boldCheckBox.setHorizontalAlignment(SwingConstants.CENTER);
		boldCheckBox.setIcon(new ImageIcon(getClass().getResource("text/images/Bold.gif")));
		boldCheckBox.setText(null);
		boldCheckBox.setPreferredSize(BUTTON_DIMENSION);
		boldCheckBox.setMargin(ZERO_INSETS);
		if (!Modeler.isMac()) {
			boldCheckBox.setBorderPainted(false);
			boldCheckBox.setFocusPainted(false);
		}
		boldCheckBox.setToolTipText("Bold");
		boldCheckBox.setRequestFocusEnabled(false);
		boldCheckBox.addItemListener(boldAction);
		tb.add(boldCheckBox);

		italicCheckBox = new JCheckBox(page.getAction(Page.ITALIC));
		italicCheckBox.setHorizontalAlignment(SwingConstants.CENTER);
		italicCheckBox.setIcon(new ImageIcon(getClass().getResource("text/images/Italic.gif")));
		italicCheckBox.setText(null);
		italicCheckBox.setPreferredSize(BUTTON_DIMENSION);
		italicCheckBox.setMargin(ZERO_INSETS);
		if (!Modeler.isMac()) {
			italicCheckBox.setBorderPainted(false);
			italicCheckBox.setFocusPainted(false);
		}
		italicCheckBox.setToolTipText("Italic");
		italicCheckBox.setRequestFocusEnabled(false);
		italicCheckBox.addItemListener(italicAction);
		tb.add(italicCheckBox);

		underlineCheckBox = new JCheckBox(page.getAction(Page.UNDERLINE));
		underlineCheckBox.setHorizontalAlignment(SwingConstants.CENTER);
		underlineCheckBox.setIcon(new ImageIcon(getClass().getResource("text/images/Underline.gif")));
		underlineCheckBox.setText(null);
		underlineCheckBox.setPreferredSize(BUTTON_DIMENSION);
		underlineCheckBox.setMargin(ZERO_INSETS);
		if (!Modeler.isMac()) {
			underlineCheckBox.setBorderPainted(false);
			underlineCheckBox.setFocusPainted(false);
		}
		underlineCheckBox.setToolTipText("Underline");
		underlineCheckBox.setRequestFocusEnabled(false);
		underlineCheckBox.addItemListener(underlineAction);
		tb.add(underlineCheckBox);
		tb.addSeparator();

		tb.add(new JLabel(Modeler.toolBarSeparatorIcon));

		ButtonGroup bg = new ButtonGroup();

		leftAlignmentCheckBox = new JCheckBox(page.getAction(Page.LEFT_ALIGN));
		leftAlignmentCheckBox.setHorizontalAlignment(SwingConstants.CENTER);
		leftAlignmentCheckBox.setIcon(new ImageIcon(getClass().getResource("text/images/AlignLeft.gif")));
		leftAlignmentCheckBox.setText(null);
		leftAlignmentCheckBox.setPreferredSize(BUTTON_DIMENSION);
		leftAlignmentCheckBox.setMargin(ZERO_INSETS);
		if (!Modeler.isMac()) {
			leftAlignmentCheckBox.setBorderPainted(false);
			leftAlignmentCheckBox.setFocusPainted(false);
		}
		leftAlignmentCheckBox.setToolTipText("Align paragraph left");
		leftAlignmentCheckBox.setRequestFocusEnabled(false);
		leftAlignmentCheckBox.addItemListener(leftAlignmentAction);
		tb.add(leftAlignmentCheckBox);
		bg.add(leftAlignmentCheckBox);

		centerAlignmentCheckBox = new JCheckBox(page.getAction(Page.CENTER_ALIGN));
		centerAlignmentCheckBox.setHorizontalAlignment(SwingConstants.CENTER);
		centerAlignmentCheckBox.setIcon(new ImageIcon(getClass().getResource("text/images/AlignCenter.gif")));
		centerAlignmentCheckBox.setText(null);
		centerAlignmentCheckBox.setPreferredSize(BUTTON_DIMENSION);
		centerAlignmentCheckBox.setMargin(ZERO_INSETS);
		if (!Modeler.isMac()) {
			centerAlignmentCheckBox.setBorderPainted(false);
			centerAlignmentCheckBox.setFocusPainted(false);
		}
		centerAlignmentCheckBox.setToolTipText("Align paragraph center");
		centerAlignmentCheckBox.setRequestFocusEnabled(false);
		centerAlignmentCheckBox.addItemListener(centerAlignmentAction);
		tb.add(centerAlignmentCheckBox);
		bg.add(centerAlignmentCheckBox);

		rightAlignmentCheckBox = new JCheckBox(page.getAction(Page.RIGHT_ALIGN));
		rightAlignmentCheckBox.setHorizontalAlignment(SwingConstants.CENTER);
		rightAlignmentCheckBox.setIcon(new ImageIcon(getClass().getResource("text/images/AlignRight.gif")));
		rightAlignmentCheckBox.setText(null);
		rightAlignmentCheckBox.setPreferredSize(BUTTON_DIMENSION);
		rightAlignmentCheckBox.setMargin(ZERO_INSETS);
		if (!Modeler.isMac()) {
			rightAlignmentCheckBox.setBorderPainted(false);
			rightAlignmentCheckBox.setFocusPainted(false);
		}
		rightAlignmentCheckBox.setToolTipText("Align paragraph right");
		rightAlignmentCheckBox.setRequestFocusEnabled(false);
		rightAlignmentCheckBox.addItemListener(rightAlignmentAction);
		tb.add(rightAlignmentCheckBox);
		bg.add(rightAlignmentCheckBox);
		tb.addSeparator();

		bulletCheckBox = new JCheckBox(page.getAction(Page.BULLET));
		bulletCheckBox.setHorizontalAlignment(SwingConstants.CENTER);
		bulletCheckBox.setIcon(new ImageIcon(getClass().getResource("text/images/Bullet.gif")));
		bulletCheckBox.setText(null);
		bulletCheckBox.setPreferredSize(BUTTON_DIMENSION);
		bulletCheckBox.setMargin(ZERO_INSETS);
		if (!Modeler.isMac()) {
			bulletCheckBox.setBorderPainted(false);
			bulletCheckBox.setFocusPainted(false);
		}
		bulletCheckBox.setToolTipText("Add bullet to paragraph");
		bulletCheckBox.setRequestFocusEnabled(false);
		bulletCheckBox.addItemListener(bulletAction);
		tb.add(bulletCheckBox);

		JButton button = new JButton(page.getAction(Page.INCREASE_INDENT));
		button.setHorizontalAlignment(SwingConstants.CENTER);
		button.setIcon(new ImageIcon(getClass().getResource("text/images/LeftIndent.gif")));
		button.setText(null);
		button.setPreferredSize(BUTTON_DIMENSION);
		button.setMargin(ZERO_INSETS);
		if (!Modeler.isMac()) {
			button.setBorderPainted(false);
			button.setFocusPainted(false);
		}
		button.setToolTipText("Increase indentation");
		button.setRequestFocusEnabled(false);
		tb.add(button);

		button = new JButton(page.getAction(Page.DECREASE_INDENT));
		button.setHorizontalAlignment(SwingConstants.CENTER);
		button.setIcon(new ImageIcon(getClass().getResource("text/images/ReverseLeftIndent.gif")));
		button.setText(null);
		button.setPreferredSize(BUTTON_DIMENSION);
		button.setMargin(ZERO_INSETS);
		if (!Modeler.isMac()) {
			button.setBorderPainted(false);
			button.setFocusPainted(false);
		}
		button.setToolTipText("Decrease indentation");
		button.setRequestFocusEnabled(false);
		tb.add(button);

		tb.add(new JLabel(Modeler.toolBarSeparatorIcon));

		button = new JButton(page.getAction("Hyperlink"));
		button.setHorizontalAlignment(SwingConstants.CENTER);
		if (Modeler.showToolBarText) {
			button.setMargin(ZERO_INSETS);
			int w = button.getFontMetrics(button.getFont()).stringWidth(button.getText());
			w += button.getIconTextGap();
			w += button.getIcon().getIconWidth();
			button.setPreferredSize(new Dimension(w + 10, BUTTON_DIMENSION.height));
			String s = Modeler.getInternationalText("HyperlinkButton");
			if (s != null)
				button.setText(s);
		}
		else {
			button.setText(null);
			button.setPreferredSize(BUTTON_DIMENSION);
			button.setMargin(ZERO_INSETS);
		}
		if (!Modeler.isMac()) {
			button.setBorderPainted(false);
			button.setFocusPainted(false);
		}
		button.setRequestFocusEnabled(false);
		addEnabledComponentWhenEditable(button);
		tb.add(button);

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
		File dir = new File(ConnectionManager.sharedInstance().getCacheDirectory(), ConnectionManager
				.convertURLToFileName(url));
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
		File dir = new File(ConnectionManager.sharedInstance().getCacheDirectory(), ConnectionManager
				.convertURLToFileName(url));
		for (int i = 0; i < address.length; i++)
			address[i] = Modeler.getStaticRoot() + address[i];
		Zipper.sharedInstance().unzipInAThread(address, message, dir);
	}

	private void validateComponent(Object src, final boolean changed) {
		boolean onPage = false;
		MDContainer m = null;
		synchronized (componentPool) {
			for (ModelCanvas c : componentPool.getModels()) {
				if (!c.isUsed())
					continue;
				m = c.getContainer();
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
						if (dnaString == null)
							m.enableRecorder(!m.getModel().getRecorderDisabled());
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
						for (int i = 0; i < but.length; i++) {
							if (!(but[i] instanceof AbstractButton))
								continue;
							Action a = ((AbstractButton) but[i]).getAction();
							if (a != null) {
								String s = (String) a.getValue(Action.NAME);
								m.addToolBarButton(s);
							}
						}
					}
					c.setMaximumSize(c.getPreferredSize());
					c.setMinimumSize(c.getMaximumSize());
					c.validate();
					((MDView) c.getContainer().getModel().getView()).clearEditor(true);
					onPage = true;
				}
			}
		}
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
										//((PageMd3d) src).setInitializationScriptToRun(true);
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

		case PageComponentEvent.SNAPSHOT_TAKEN2:
			EventQueue.invokeLater(new Runnable() {
				public void run() {
					SnapshotGallery.sharedInstance().takeSnapshot(page.getAddress(), src, false);
				}
			});
			break;

		}

	}

	private void setToolbarButtons() {
		if (page.isRemote()) {
			if (editCheckBox.isSelected()) {
				setEditable(false);
				editCheckBox.requestFocusInWindow();
			}
			statusBar.showWebPageStatus();
			updateActions(false);
			toolBar[0].add(submitCommentButton);
			toolBar[0].add(viewCommentButton);
			toolBar[0].add(mwSpaceButton);
			toolBar[0].remove(editCheckBox);
		}
		else {
			statusBar.showLocalPageStatus();
			updateActions(editCheckBox.isSelected());
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
			for (Component c : disabledComponentsWhileLoading) {
				c.setEnabled(true);
			}
		}
	}

	private void enableDisabledComponentsWhileLoading(boolean b) {
		for (Component c : disabledComponentsWhileLoading) {
			c.setEnabled(b);
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
					setToolbarButtons();
					enableDisabledComponentsWhileLoading(true);
					statusBar.getProgressBar().setIndeterminate(false);
					statusBar.tipBar.setText(editCheckBox.isSelected() ? "Editor mode" : "Loaded");
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
			page.getSaveReminder().setChanged(false);
			final String s = e.getDescription();
			EventQueue.invokeLater(new Runnable() {
				public void run() {
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
			editCheckBox.setIcon(editIcon);
			toolBarPanel.add(toolBar[1], BorderLayout.SOUTH);
			toolBar[0].add(toolBar[2]);
		}
		else {
			editCheckBox.setIcon(noEditIcon);
			toolBarPanel.remove(toolBar[1]);
			toolBar[0].remove(toolBar[2]);
		}
		validate();
	}

}
