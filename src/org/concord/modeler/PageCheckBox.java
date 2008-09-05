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
import java.awt.event.ItemListener;
import java.awt.event.MouseListener;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import org.concord.modeler.event.ModelEvent;
import org.concord.modeler.text.Page;
import org.concord.modeler.text.XMLCharacterEncoder;

public class PageCheckBox extends JCheckBox implements Embeddable, ModelCommunicator {

	Page page;
	String modelClass;
	int modelID = -1;
	boolean autoSize = true;
	boolean disabledAtRun, disabledAtScript;
	private int index;
	private String id;
	private boolean marked;
	private Color checkBoxBackground;
	private boolean wasOpaque;
	private static Color defaultCheckBoxBackground, defaultCheckBoxForeground;
	private JPopupMenu popupMenu;
	private static PageCheckBoxMaker maker;
	private MouseListener popupMouseListener;

	public PageCheckBox() {
		super();
		init();
	}

	public PageCheckBox(String text) {
		super(text);
		init();
	}

	public PageCheckBox(PageCheckBox checkBox, Page parent) {
		this();
		setPage(parent);
		setModelClass(checkBox.modelClass);
		setModelID(checkBox.modelID);
		setOpaque(checkBox.isOpaque());
		setBackground(checkBox.getBackground());
		setSelected(checkBox.isSelected());
		setName(checkBox.getName());
		setAction(checkBox.getAction());
		setText(checkBox.getText());
		setToolTipText(checkBox.getToolTipText());
		setAutoSize(checkBox.autoSize);
		setDisabledAtRun(checkBox.disabledAtRun);
		setDisabledAtScript(checkBox.disabledAtScript);
		setPreferredSize(checkBox.getPreferredSize());
		setChangable(page.isEditable());
		setId(checkBox.id);
		Object o = checkBox.getClientProperty("selection script");
		if (o != null)
			putClientProperty("selection script", o);
		o = checkBox.getClientProperty("deselection script");
		if (o != null)
			putClientProperty("deselection script", o);
		BasicModel m = getBasicModel();
		if (m != null)
			m.addModelListener(this);
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
		if (defaultCheckBoxBackground == null)
			defaultCheckBoxBackground = getBackground();
		if (defaultCheckBoxForeground == null)
			defaultCheckBoxForeground = getForeground();
		popupMouseListener = new PopupMouseListener(this);
		addMouseListener(popupMouseListener);
		setOpaque(false);
		setFont(new Font(null, Font.PLAIN, Page.getDefaultFontSize() - 1));
	}

	public JPopupMenu getPopupMenu() {
		return popupMenu;
	}

	public void createPopupMenu() {
		popupMenu = new JPopupMenu();
		popupMenu.setInvoker(this);
		String s = Modeler.getInternationalText("CustomizeCheckBox");
		JMenuItem mi = new JMenuItem((s != null ? s : "Customize This Check Box") + "...");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (maker == null) {
					maker = new PageCheckBoxMaker(PageCheckBox.this);
				}
				else {
					maker.setObject(PageCheckBox.this);
				}
				maker.invoke(page);
			}
		});
		popupMenu.add(mi);
		s = Modeler.getInternationalText("RemoveCheckBox");
		mi = new JMenuItem(s != null ? s : "Remove This Check Box");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				page.removeComponent(PageCheckBox.this);
			}
		});
		popupMenu.add(mi);
		s = Modeler.getInternationalText("CopyCheckBox");
		mi = new JMenuItem(s != null ? s : "Copy This Check Box");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				page.copyComponent(PageCheckBox.this);
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

	public void setPage(Page p) {
		page = p;
	}

	public Page getPage() {
		return page;
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

	public void setMarked(boolean b) {
		marked = b;
		if (page == null)
			return;
		if (b) {
			wasOpaque = isOpaque();
			setOpaque(true);
			checkBoxBackground = getBackground();
		}
		else {
			setOpaque(wasOpaque);
		}
		setBackground(b ? page.getSelectionColor() : checkBoxBackground);
		setForeground(b ? page.getSelectedTextColor() : defaultCheckBoxForeground);
	}

	public boolean isMarked() {
		return marked;
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

	public static PageCheckBox create(Page page) {
		if (page == null)
			return null;
		PageCheckBox checkBox = new PageCheckBox();
		if (maker == null) {
			maker = new PageCheckBoxMaker(checkBox);
		}
		else {
			maker.setObject(checkBox);
		}
		maker.invoke(page);
		if (maker.cancel)
			return null;
		return checkBox;
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

	private void enableCheckBox(boolean b, Object source) {
		ComponentMaker.enable(this, b, source, modelID, modelClass, page);
	}

	public void modelUpdate(final ModelEvent e) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				Object src = e.getSource();
				switch (e.getID()) {
				case ModelEvent.SCRIPT_START:
					if (disabledAtScript)
						enableCheckBox(false, src);
					break;
				case ModelEvent.SCRIPT_END:
					if (disabledAtScript)
						enableCheckBox(true, src);
					break;
				case ModelEvent.MODEL_RUN:
					if (disabledAtRun)
						enableCheckBox(false, src);
					break;
				case ModelEvent.MODEL_STOP:
					if (disabledAtRun)
						enableCheckBox(true, src);
					break;
				case ModelEvent.MODEL_INPUT:
					if (src instanceof BasicModel) {
						Action a = getAction();
						if (a != null) {
							Object o = a.getValue("state");
							if (o instanceof Boolean) {
								boolean b = ((Boolean) o).booleanValue();
								if (b != isSelected())
									setSelected(b);
							}
						}
					}
					break;
				}
			}
		});
	}

	public String toString() {
		Action a = getAction();
		StringBuffer sb = new StringBuffer("<class>" + getClass().getName() + "</class>\n");
		sb.append("<title>" + XMLCharacterEncoder.encode(getText()) + "</title>\n");
		String toolTip = getToolTipText();
		if (toolTip != null) {
			if (a != null && !toolTip.equals(a.getValue(Action.SHORT_DESCRIPTION))) {
				sb.append("<tooltip>" + XMLCharacterEncoder.encode(getToolTipText()) + "</tooltip>\n");
			}
		}
		if (!autoSize)
			sb.append("<width>" + getWidth() + "</width><height>" + getHeight() + "</height>\n");
		if (isOpaque())
			sb.append("<transparent>false</transparent>\n");
		if (disabledAtRun)
			sb.append("<disabled_at_run>true</disabled_at_run>\n");
		if (disabledAtScript)
			sb.append("<disabled_at_script>true</disabled_at_script>\n");
		if (isSelected())
			sb.append("<selected>true</selected>\n");
		if (modelClass != null)
			sb.append("<modelclass>" + modelClass + "</modelclass>\n");
		sb.append("<model>" + modelID + "</model>\n");
		String s = (String) getClientProperty("selection script");
		if (s != null && !s.trim().equals("")) {
			sb.append("<script>" + XMLCharacterEncoder.encode(s) + "</script>\n");
		}
		s = (String) getClientProperty("deselection script");
		if (s != null && !s.trim().equals("")) {
			sb.append("<script2>" + XMLCharacterEncoder.encode(s) + "</script2>\n");
		}
		if (!getBackground().equals(defaultCheckBoxBackground))
			sb.append("<bgcolor>" + Integer.toString(getBackground().getRGB(), 16) + "</bgcolor>\n");
		if (a != null)
			sb.append("<action>" + a.getValue(Action.SHORT_DESCRIPTION) + "</action>\n");
		return sb.toString();
	}

}
