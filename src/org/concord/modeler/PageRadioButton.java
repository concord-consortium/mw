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
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseListener;

import javax.swing.Action;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.JMenuItem;
import javax.swing.JRadioButton;
import javax.swing.JPopupMenu;

import org.concord.modeler.event.ModelEvent;
import org.concord.modeler.text.Page;
import org.concord.modeler.text.XMLCharacterEncoder;

public class PageRadioButton extends JRadioButton implements Embeddable, ModelCommunicator {

	Page page;
	String modelClass;
	int modelID = -1;
	boolean autoSize = true;
	boolean disabledAtRun, disabledAtScript;
	private long groupID = -1;
	private int index;
	private String id;
	private String imageSelected, imageDeselected;
	private boolean marked;
	private boolean wasOpaque;
	private Color radioButtonBackground;
	private static Color defaultRadioButtonBackground, defaultRadioButtonForeground;
	private JPopupMenu popupMenu;
	private static PageRadioButtonMaker maker;
	private MouseListener popupMouseListener;

	public PageRadioButton() {
		super();
		init();
	}

	public PageRadioButton(String text) {
		super(text);
		init();
	}

	public PageRadioButton(PageRadioButton radioButton, Page parent) {
		this();
		setPage(parent);
		setModelClass(radioButton.modelClass);
		setModelID(radioButton.modelID);
		setGroupID(radioButton.groupID);
		setOpaque(radioButton.isOpaque());
		setBackground(radioButton.getBackground());
		setSelected(radioButton.isSelected());
		setName(radioButton.getName());
		setAction(radioButton.getAction());
		setAutoSize(radioButton.autoSize);
		setDisabledAtRun(radioButton.disabledAtRun);
		setDisabledAtScript(radioButton.disabledAtScript);
		setPreferredSize(radioButton.getPreferredSize());
		setChangable(page.isEditable());
		Object o = radioButton.getClientProperty("script");
		if (o != null)
			putClientProperty("script", o);
		o = radioButton.getClientProperty("button group");
		if (o instanceof ButtonGroup) {
			putClientProperty("button group", o);
			((ButtonGroup) o).add(this);
		}
		setText(radioButton.getText());
		setIcon(radioButton.getIcon());
		setImageFileNameSelected(radioButton.imageSelected);
		setImageFileNameDeselected(radioButton.imageDeselected);
		setToolTipText(radioButton.getToolTipText());
		BasicModel m = getBasicModel();
		if (m != null)
			m.addModelListener(this);
		setId(radioButton.id);
	}

	boolean isTargetClass() {
		return ComponentMaker.isTargetClass(modelClass);
	}

	private BasicModel getBasicModel() {
		return ComponentMaker.getBasicModel(page, modelClass, modelID);
	}

	public void destroy() {
		setAction(null);
		MouseListener[] ml = getMouseListeners();
		if (ml != null) {
			for (MouseListener i : ml)
				removeMouseListener(i);
		}
		ActionListener[] al = getActionListeners();
		if (al != null) {
			for (ActionListener i : al)
				removeActionListener(i);
		}
		ItemListener[] il = getItemListeners();
		if (il != null) {
			for (ItemListener i : il)
				removeItemListener(i);
		}
		BasicModel m = getBasicModel();
		if (m != null)
			m.removeModelListener(this);
		page = null;
		if (maker != null)
			maker.setObject(null);
	}

	private void init() {
		if (defaultRadioButtonBackground == null)
			defaultRadioButtonBackground = getBackground();
		if (defaultRadioButtonForeground == null)
			defaultRadioButtonForeground = getForeground();
		popupMouseListener = new PopupMouseListener(this);
		addMouseListener(popupMouseListener);
		setOpaque(false);
		setFont(new Font(null, Font.PLAIN, Page.getDefaultFontSize() - 1));
		addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				switch (e.getStateChange()) {
				case ItemEvent.SELECTED:
					if (imageSelected != null)
						setIcon(page.loadImage(imageSelected));
					break;
				case ItemEvent.DESELECTED:
					if (imageDeselected != null)
						setIcon(page.loadImage(imageDeselected));
					break;
				}
			}
		});
	}

	public JPopupMenu getPopupMenu() {
		return popupMenu;
	}

	public void createPopupMenu() {
		popupMenu = new JPopupMenu();
		popupMenu.setInvoker(this);
		String s = Modeler.getInternationalText("CustomizeRadioButton");
		JMenuItem mi = new JMenuItem((s != null ? s : "Customize This Radio Button") + "...");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (maker == null) {
					maker = new PageRadioButtonMaker(PageRadioButton.this);
				}
				else {
					maker.setObject(PageRadioButton.this);
				}
				maker.invoke(page);
			}
		});
		popupMenu.add(mi);
		s = Modeler.getInternationalText("RemoveRadioButton");
		mi = new JMenuItem(s != null ? s : "Remove This Radio Button");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				page.removeComponent(PageRadioButton.this);
			}
		});
		popupMenu.add(mi);
		s = Modeler.getInternationalText("CopyRadioButton");
		mi = new JMenuItem(s != null ? s : "Copy This Radio Button");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				page.copyComponent(PageRadioButton.this);
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

	public void setGroupID(long id) {
		groupID = id;
	}

	public long getGroupID() {
		return groupID;
	}

	public void setImageFileNameSelected(String imageSelected) {
		this.imageSelected = imageSelected;
	}

	public String getImageFileNameSelected() {
		return imageSelected;
	}

	public void setImageFileNameDeselected(String imageDeselected) {
		this.imageDeselected = imageDeselected;
	}

	public String getImageFileNameDeselected() {
		return imageDeselected;
	}

	public void setOpaque(boolean b) {
		super.setOpaque(b);
		if (page == null)
			return;
		if (!b) {
			setForeground(new Color(0xffffff ^ page.getBackground().getRGB()));
		}
	}

	public void setTransparent(boolean b) {
		setOpaque(!b);
	}

	public boolean isTransparent() {
		return !isOpaque();
	}

	public void setPage(Page p) {
		page = p;
	}

	public Page getPage() {
		return page;
	}

	public void setModelID(int i) {
		modelID = i;
	}

	public int getModelID() {
		return modelID;
	}

	public void setModelClass(String s) {
		modelClass = s;
	}

	public String getModelClass() {
		return modelClass;
	}

	public void setMarked(boolean b) {
		marked = b;
		if (page == null)
			return;
		if (b) {
			wasOpaque = isOpaque();
			setOpaque(true);
			radioButtonBackground = getBackground();
		}
		else {
			setOpaque(wasOpaque);
		}
		setBackground(b ? page.getSelectionColor() : radioButtonBackground);
		setForeground(b ? page.getSelectedTextColor() : defaultRadioButtonForeground);
	}

	public boolean isMarked() {
		return marked;
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

	public void setAutoSize(boolean b) {
		autoSize = b;
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

	public static PageRadioButton create(Page page) {
		if (page == null)
			return null;
		PageRadioButton rb = new PageRadioButton();
		if (maker == null) {
			maker = new PageRadioButtonMaker(rb);
		}
		else {
			maker.setObject(rb);
		}
		maker.invoke(page);
		if (maker.cancel)
			return null;
		return rb;
	}

	public static PageRadioButton create(String text, Page page) {
		PageRadioButton rb = new PageRadioButton(text);
		rb.setPage(page);
		return rb;
	}

	// old setText method
	public void setText2(String text) {
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
			w += icon.getIconWidth();
			w += getIconTextGap();
			h = Math.max(h, icon.getIconHeight());
		}
		Insets margin = getMargin();
		w += margin.left + margin.right + 25;
		h += margin.top + margin.bottom + 8;
		setPreferredSize(new Dimension(w, h));
	}

	public void setIconOLD(Icon icon) {
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
			w += icon.getIconWidth();
			w += getIconTextGap();
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

	private void enableRadioButton(final boolean b, Object source) {
		ComponentMaker.enable(this, b, source, modelID, modelClass, page);
	}

	public void modelUpdate(final ModelEvent e) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				switch (e.getID()) {
				case ModelEvent.SCRIPT_START:
					if (disabledAtScript)
						enableRadioButton(false, e.getSource());
					break;
				case ModelEvent.SCRIPT_END:
					if (disabledAtScript)
						enableRadioButton(true, e.getSource());
					break;
				case ModelEvent.MODEL_RUN:
					if (disabledAtRun)
						enableRadioButton(false, e.getSource());
					break;
				case ModelEvent.MODEL_STOP:
					if (disabledAtRun)
						enableRadioButton(true, e.getSource());
					break;
				}
			}
		});
	}

	public String toString() {
		Action a = getAction();
		StringBuffer sb = new StringBuffer("<class>" + getClass().getName() + "</class>\n");
		sb.append("<title>" + XMLCharacterEncoder.encode(getText()) + "</title>\n");
		if (imageSelected != null) {
			sb.append("<imagefile>" + imageSelected + "</imagefile>\n");
		}
		if (imageDeselected != null) {
			sb.append("<imagefiledeselected>" + imageDeselected + "</imagefiledeselected>\n");
		}
		String toolTip = getToolTipText();
		if (toolTip != null) {
			if (a != null && !toolTip.equals(a.getValue(Action.SHORT_DESCRIPTION))) {
				sb.append("<tooltip>" + XMLCharacterEncoder.encode(toolTip) + "</tooltip>\n");
			}
		}
		if (!autoSize)
			sb.append("<width>" + getWidth() + "</width><height>" + getHeight() + "</height>\n");
		if (disabledAtRun)
			sb.append("<disabled_at_run>true</disabled_at_run>\n");
		if (disabledAtScript)
			sb.append("<disabled_at_script>true</disabled_at_script>\n");
		if (isOpaque())
			sb.append("<transparent>false</transparent>\n");
		if (isSelected())
			sb.append("<selected>true</selected>\n");
		if (modelClass != null)
			sb.append("<modelclass>" + modelClass + "</modelclass>\n");
		sb.append("<model>" + modelID + "</model>\n");
		sb.append("<groupid>" + groupID + "</groupid>\n");
		String s = (String) getClientProperty("script");
		if (s != null && !s.trim().equals(""))
			sb.append("<script>" + XMLCharacterEncoder.encode(s) + "</script>\n");
		if (!getBackground().equals(defaultRadioButtonBackground))
			sb.append("<bgcolor>" + Integer.toString(getBackground().getRGB(), 16) + "</bgcolor>\n");
		if (a != null)
			sb.append("<action>" + a.getValue(Action.SHORT_DESCRIPTION) + "</action>");
		return sb.toString();
	}

}
