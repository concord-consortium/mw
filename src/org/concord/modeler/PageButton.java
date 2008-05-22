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
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.Timer;

import org.concord.modeler.event.ModelEvent;
import org.concord.modeler.text.Page;
import org.concord.modeler.text.XMLCharacterEncoder;

/**
 * This is a button that can be embedded onto a page to control an embedded model. Left-click this button will fire the
 * desired action, right-click will invoke a dialog box for changing its properties.
 */

public class PageButton extends JButton implements Embeddable, ModelCommunicator {

	Page page;
	boolean autoSize = true;
	String modelClass;
	int modelID = -1;
	boolean continuousFire;
	boolean disabledAtRun;
	boolean disabledAtScript;
	private int index;
	private String id;
	private boolean marked;
	private boolean transparent;
	private String borderType;
	private Color buttonBackground;
	private boolean wasOpaque;
	private Timer holdTimer;
	private static Color defaultButtonBackground, defaultButtonForeground;
	private JPopupMenu popupMenu;
	private static PageButtonMaker maker;
	private MouseListener popupMouseListener;

	private MouseListener keepFire = new MouseAdapter() {
		public void mousePressed(MouseEvent e) {
			if (!continuousFire)
				return;
			if (ModelerUtilities.isRightClick(e))
				return;
			if (holdTimer == null) {
				// FIXME: This used to work, but action now locks itself up before button is released.
				holdTimer = new Timer(100, getAction());
				holdTimer.setRepeats(true);
				holdTimer.start();
			}
			else {
				ActionListener[] act = holdTimer.getActionListeners();
				if (act.length > 1)
					throw new RuntimeException("More than one action listener has been registered with this button!");
				if (act.length == 1) {
					if (!act[0].toString().equals(getAction().toString())) {
						holdTimer = new Timer(100, getAction());
						holdTimer.setRepeats(true);
					}
				}
				if (!holdTimer.isRunning())
					holdTimer.restart();
			}
		}

		public void mouseReleased(MouseEvent e) {
			if (!continuousFire)
				return;
			if (ModelerUtilities.isRightClick(e))
				return;
			if (holdTimer != null)
				holdTimer.stop();
		}
	};

	public PageButton() {
		super();
		init();
	}

	public PageButton(String text) {
		super(text);
		init();
	}

	public PageButton(PageButton button, Page parent) {
		this();
		setPage(parent);
		setBackground(button.getBackground());
		if (!Page.isNativeLookAndFeelUsed()) {
			setBorderType(button.getBorderType());
			setOpaque(button.isOpaque());
		}
		setModelClass(button.modelClass);
		setModelID(button.modelID);
		setName(button.getName());
		setAction(button.getAction());
		setText(button.getText());
		setToolTipText(button.getToolTipText());
		setContinuousFire(button.continuousFire);
		setDisabledAtRun(button.disabledAtRun);
		setDisabledAtScript(button.disabledAtScript);
		setAutoSize(button.autoSize);
		setPreferredSize(button.getPreferredSize());
		setChangable(page.isEditable());
		setId(button.id);
		Object o = button.getClientProperty("script");
		if (o != null)
			putClientProperty("script", o);
		o = button.getClientProperty("increment");
		if (o != null)
			putClientProperty("increment", o);
		if (isTargetClass()) {
			try {
				o = page.getEmbeddedComponent(Class.forName(modelClass), modelID);
				if (o instanceof BasicModel)
					((BasicModel) o).addModelListener(this);
			}
			catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
		}
		else {
			ModelCanvas mc = page.getComponentPool().get(modelID);
			if (mc != null)
				mc.getContainer().getModel().addModelListener(this);
		}
	}

	boolean isTargetClass() {
		if (modelClass == null)
			return false;
		for (Class c : targetClass) {
			if (modelClass.equals(c.getName()))
				return true;
		}
		return false;
	}

	public void destroy() {
		setAction(null);
		ActionListener[] al = getActionListeners();
		if (al != null) {
			for (ActionListener i : al)
				removeActionListener(i);
		}
		MouseListener[] ml = getMouseListeners();
		if (ml != null) {
			for (MouseListener i : ml)
				removeMouseListener(i);
		}
		if (modelID != -1) {
			if (isTargetClass()) {
				try {
					Object o = page.getEmbeddedComponent(Class.forName(modelClass), modelID);
					if (o instanceof BasicModel)
						((BasicModel) o).removeModelListener(this);
				}
				catch (ClassNotFoundException e) {
					e.printStackTrace();
				}
			}
			else {
				ModelCanvas mc = page.getComponentPool().get(modelID);
				if (mc != null)
					mc.getContainer().getModel().removeModelListener(this);
			}
		}
		page = null;
		holdTimer = null;
		popupMouseListener = null;
		keepFire = null;
		if (maker != null)
			maker.setObject(null);
	}

	private void init() {
		if (defaultButtonBackground == null)
			defaultButtonBackground = getBackground();
		if (defaultButtonForeground == null)
			defaultButtonForeground = getForeground();
		popupMouseListener = new PopupMouseListener(this);
		addMouseListener(popupMouseListener);
		setFont(new Font(null, Font.PLAIN, Page.getDefaultFontSize() - 1));
	}

	public JPopupMenu getPopupMenu() {
		return popupMenu;
	}

	public void createPopupMenu() {
		popupMenu = new JPopupMenu();
		popupMenu.setInvoker(this);
		String s = Modeler.getInternationalText("CustomizeButton");
		JMenuItem mi = new JMenuItem((s != null ? s : "Customize This Button") + "...");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (maker == null) {
					maker = new PageButtonMaker(PageButton.this);
				}
				else {
					maker.setObject(PageButton.this);
				}
				maker.invoke(page);
			}
		});
		popupMenu.add(mi);
		s = Modeler.getInternationalText("RemoveButton");
		mi = new JMenuItem(s != null ? s : "Remove This Button");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				page.removeComponent(PageButton.this);
			}
		});
		popupMenu.add(mi);
		s = Modeler.getInternationalText("CopyButton");
		mi = new JMenuItem(s != null ? s : "Copy This Button");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				page.copyComponent(PageButton.this);
			}
		});
		popupMenu.add(mi);
		popupMenu.pack();
	}

	public void setIndex(int i) {
		index = i;
	}

	public int getIndex() {
		return index;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getId() {
		return id;
	}

	public void setMarked(boolean b) {
		marked = b;
		if (page == null)
			return;
		if (b) {
			wasOpaque = isOpaque();
			setOpaque(true);
			buttonBackground = getBackground();
		}
		else {
			setOpaque(wasOpaque);
		}
		setBackground(b ? page.getSelectionColor() : buttonBackground);
		setForeground(b ? page.getSelectedTextColor() : defaultButtonForeground);
	}

	public boolean isMarked() {
		return marked;
	}

	public void setOpaque(boolean b) {
		transparent = !b;
		if (Page.isNativeLookAndFeelUsed())
			return;
		super.setOpaque(b);
	}

	public void setContinuousFire(boolean b) {
		continuousFire = b;
		if (b) {
			addMouseListener(keepFire);
		}
		else {
			removeMouseListener(keepFire);
		}
	}

	public boolean getContinuousFire() {
		return continuousFire;
	}

	public void setDisabledAtRun(boolean b) {
		disabledAtRun = b;
		if (!b) {
			EventQueue.invokeLater(new Runnable() {
				public void run() {
					setEnabled(true);
				}
			});
		}
	}

	public boolean getDisabledAtRun() {
		return disabledAtRun;
	}

	public void setDisabledAtScript(boolean b) {
		disabledAtScript = b;
		if (!b) {
			EventQueue.invokeLater(new Runnable() {
				public void run() {
					setEnabled(true);
				}
			});
		}
	}

	public boolean getDisabledAtScript() {
		return disabledAtScript;
	}

	public String getBorderType() {
		if (borderType == null)
			return BorderManager.getBorder(this);
		return borderType;
	}

	public void setBorderType(String s) {
		borderType = s;
		if (Page.isNativeLookAndFeelUsed())
			return;
		BorderManager.setBorder(this, s, page.getBackground());
	}

	public void setModelClass(String s) {
		modelClass = s;
	}

	public String getModelClass() {
		return modelClass;
	}

	public void setModelID(int i) {
		modelID = i;
	}

	public int getModelID() {
		return modelID;
	}

	public void setPage(Page p) {
		page = p;
	}

	public Page getPage() {
		return page;
	}

	public void setChangable(boolean b) {
		if (b) {
			if (!isChangable())
				addMouseListener(popupMouseListener);
		}
		else {
			if (isChangable())
				removeMouseListener(popupMouseListener);
		}
	}

	public boolean isChangable() {
		MouseListener[] ml = getMouseListeners();
		for (MouseListener x : ml) {
			if (x == popupMouseListener)
				return true;
		}
		return false;
	}

	public static PageButton create(Page page) {
		if (page == null)
			return null;
		PageButton button = new PageButton();
		if (maker == null) {
			maker = new PageButtonMaker(button);
		}
		else {
			maker.setObject(button);
		}
		maker.invoke(page);
		if (maker.cancel)
			return null;
		return button;
	}

	public void setText(String text) {
		super.setText(text);
		if (!autoSize)
			return;
		if (text == null)
			return;
		FontMetrics fm = getFontMetrics(getFont());
		int w = fm.stringWidth(text);
		int h = fm.getHeight();
		Icon icon = getIcon();
		if (icon != null) {
			w += icon.getIconWidth() + getIconTextGap();
			h = Math.max(h, icon.getIconHeight());
		}
		Insets margin = getMargin();
		w += margin.left + margin.right + 25;
		h += margin.top + margin.bottom + 8;
		setPreferredSize(new Dimension(w, h));
	}

	public void setIcon(Icon icon) {
		super.setIcon(icon);
		if (!autoSize)
			return;
		int w = 0, h = 0;
		if (getText() != null) {
			FontMetrics fm = getFontMetrics(getFont());
			w = fm.stringWidth(getText());
			h = fm.getHeight();
		}
		if (icon != null) {
			w += icon.getIconWidth() + getIconTextGap();
			h = Math.max(h, icon.getIconHeight());
		}
		Insets margin = getMargin();
		w += margin.left + margin.right + 25;
		h += margin.top + margin.bottom + 8;
		setPreferredSize(new Dimension(w, h));
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

	public void setAutoSize(boolean b) {
		autoSize = b;
	}

	private void enableButton(final boolean b, Object source) {
		if (modelID == -1)
			return;
		ModelCanvas mc = page.getComponentPool().get(modelID);
		if (mc == null)
			return;
		if (mc.getContainer().getModel() != source)
			return;
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				setEnabled(b);
			}
		});
	}

	public void modelUpdate(ModelEvent e) {

		if (isTargetClass()) {
			//
		}
		else {
			switch (e.getID()) {
			case ModelEvent.SCRIPT_START:
				if (disabledAtScript)
					enableButton(false, e.getSource());
				break;
			case ModelEvent.SCRIPT_END:
				if (disabledAtScript)
					enableButton(true, e.getSource());
				break;
			case ModelEvent.MODEL_RUN:
				if (disabledAtRun)
					enableButton(false, e.getSource());
				break;
			case ModelEvent.MODEL_STOP:
				if (disabledAtRun)
					enableButton(true, e.getSource());
				break;
			}
		}

	}

	public String toString() {
		Action a = getAction();
		StringBuffer sb = new StringBuffer("<class>" + getClass().getName() + "</class>\n");
		sb.append("<title>" + XMLCharacterEncoder.encode(getText()) + "</title>\n");
		String toolTip = getToolTipText();
		if (toolTip != null) {
			if (a != null && !toolTip.equals(a.getValue(Action.SHORT_DESCRIPTION))) {
				sb.append("<tooltip>" + XMLCharacterEncoder.encode(toolTip) + "</tooltip>\n");
			}
		}
		if (!autoSize)
			sb.append("<width>" + getWidth() + "</width><height>" + getHeight() + "</height>\n");
		if (!getBackground().equals(defaultButtonBackground)) {
			sb.append("<bgcolor>" + Integer.toString(getBackground().getRGB(), 16) + "</bgcolor>\n");
		}
		if (borderType != null)
			sb.append("<border>" + borderType + "</border>\n");
		if (transparent)
			sb.append("<opaque>false</opaque>\n");
		if (modelClass != null)
			sb.append("<modelclass>" + modelClass + "</modelclass>\n");
		sb.append("<model>" + modelID + "</model>\n");
		if (disabledAtRun)
			sb.append("<disabled_at_run>true</disabled_at_run>\n");
		if (disabledAtScript)
			sb.append("<disabled_at_script>true</disabled_at_script>\n");
		if (continuousFire)
			sb.append("<continuous_fire>true</continuous_fire>\n");
		String s = (String) getClientProperty("script");
		if (s != null && !s.trim().equals(""))
			sb.append("<script>" + XMLCharacterEncoder.encode(s) + "</script>\n");
		if (a != null) {
			sb.append("<action>" + a.getValue(Action.SHORT_DESCRIPTION) + "</action>\n");
			Object o = a.getValue("increment");
			if (o != null) {
				sb.append("<step>" + o + "</step>\n");
			}
		}
		return sb.toString();
	}

}