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
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemListener;
import java.awt.event.MouseListener;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.JComboBox;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import org.concord.modeler.event.ModelEvent;
import org.concord.modeler.text.Page;
import org.concord.modeler.text.XMLCharacterEncoder;
import org.concord.modeler.util.FileUtilities;

public class PageComboBox extends JComboBox implements Embeddable, ModelCommunicator {

	private final static String PATTERN = "(?i)(script[\\s&&[^\\r\\n]]*=[\\s&&[^\\r\\n]]*)|(option[\\s&&[^\\r\\n]]*=[\\s&&[^\\r\\n]]*)";

	Page page;
	String modelClass;
	int modelID = -1;
	boolean disabledAtRun, disabledAtScript;
	String optionGroup;
	private int index;
	private String id;
	private boolean marked;
	private Color comboBoxBackground;
	private static Color defaultComboBoxForeground;
	private JPopupMenu popupMenu;
	private static PageComboBoxMaker maker;
	private MouseListener popupMouseListener;

	public PageComboBox() {
		super();
		popupMouseListener = new PopupMouseListener(this);
		addMouseListener(popupMouseListener);
		setFont(new Font(null, Font.PLAIN, Page.getDefaultFontSize() - 1));
		if (defaultComboBoxForeground == null)
			defaultComboBoxForeground = getForeground();
	}

	public PageComboBox(PageComboBox comboBox, Page parent) {
		this();
		setPage(parent);
		setModelClass(comboBox.modelClass);
		setModelID(comboBox.modelID);
		setName(comboBox.getName());
		setAction(comboBox.getAction());
		Object o = comboBox.getClientProperty("Selected Index");
		if (o instanceof Integer) {
			putClientProperty("Selected Index", o);
			setSelectedIndex(((Integer) o).intValue());
		}
		o = comboBox.getClientProperty("Script");
		if (o instanceof String) {
			putClientProperty("Script", o);
			setupScripts((String) o);
		}
		setDisabledAtRun(comboBox.disabledAtRun);
		setDisabledAtScript(comboBox.disabledAtScript);
		setChangable(page.isEditable());
		BasicModel m = getBasicModel();
		if (m != null)
			m.addModelListener(this);
		setToolTipText(comboBox.getToolTipText());
		setId(comboBox.id);
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

	public JPopupMenu getPopupMenu() {
		return popupMenu;
	}

	public void createPopupMenu() {
		popupMenu = new JPopupMenu();
		popupMenu.setInvoker(this);
		String s = Modeler.getInternationalText("CustomizeComboBox");
		JMenuItem mi = new JMenuItem((s != null ? s : "Customize This Combo Box") + "...");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (maker == null) {
					maker = new PageComboBoxMaker(PageComboBox.this);
				}
				else {
					maker.setObject(PageComboBox.this);
				}
				maker.invoke(page);
			}
		});
		popupMenu.add(mi);
		s = Modeler.getInternationalText("RemoveComboBox");
		mi = new JMenuItem(s != null ? s : "Remove This Combo Box");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				page.removeComponent(PageComboBox.this);
			}
		});
		popupMenu.add(mi);
		s = Modeler.getInternationalText("CopyComboBox");
		mi = new JMenuItem(s != null ? s : "Copy This Combo Box");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				page.copyComponent(PageComboBox.this);
			}
		});
		popupMenu.add(mi);
		popupMenu.pack();
	}

	public void setAction(Action a) {
		if (!EventQueue.isDispatchThread())
			throw new RuntimeException("must be called in event thread.");
		super.setAction(null);
		removeAllItems();
		if (a == null)
			return;
		resetItems(a);
		super.setAction(a);
	}

	private void resetItems(Action a) {
		Object[] o = (Object[]) a.getValue("options");
		if (o == null)
			return;
		for (Object x : o)
			addItem(x);
		adjustSize();
		resetSelectedIndex();
	}

	private void adjustSize() {
		FontMetrics fm = getFontMetrics(getFont());
		int count = getItemCount();
		int w, wmax = 0, h = fm.getHeight();
		for (int i = 0; i < count; i++) {
			w = fm.stringWidth(getItemAt(i).toString());
			if (w > wmax)
				wmax = w;
		}
		if (Modeler.isMac())
			wmax += 20;
		Dimension dim = new Dimension(wmax + 30, h + 8);
		setMaximumSize(dim);
		setMinimumSize(dim);
		setPreferredSize(dim);
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

	public void setOptionGroup(String optionGroup) {
		this.optionGroup = optionGroup;
		if (optionGroup == null || optionGroup.trim().equals(""))
			return;
		String[] ss = optionGroup.split(",");
		for (int i = 0; i < ss.length; i++)
			ss[i] = ss[i].trim();
		String[] sss = new String[ss.length + 1];
		sss[0] = "Select a model";
		for (int i = 0; i < ss.length; i++)
			sss[i + 1] = ss[i];
		Action a = getAction();
		if (a != null) {
			a.putValue("options", sss);
			setAction(a);
		}
	}

	public String getOptionGroup() {
		return optionGroup;
	}

	public void setMarked(boolean b) {
		marked = b;
		if (page == null)
			return;
		if (b)
			comboBoxBackground = getBackground();
		setBackground(b ? page.getSelectionColor() : comboBoxBackground);
		setForeground(b ? page.getSelectedTextColor() : defaultComboBoxForeground);
	}

	public boolean isMarked() {
		return marked;
	}

	private void addMouseListenerToButton(MouseListener ml) {
		int n = getComponentCount();
		Component c;
		for (int i = 0; i < n; i++) {
			c = getComponent(i);
			if (c instanceof AbstractButton)
				c.addMouseListener(ml);
		}
	}

	private void removeMouseListenerToButton(MouseListener ml) {
		int n = getComponentCount();
		Component c;
		for (int i = 0; i < n; i++) {
			c = getComponent(i);
			if (c instanceof AbstractButton)
				c.removeMouseListener(ml);
		}
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

	public void setChangable(boolean b) {
		if (b) {
			if (!isChangable()) {
				addMouseListener(popupMouseListener);
				addMouseListenerToButton(popupMouseListener);
			}
		}
		else {
			if (isChangable()) {
				removeMouseListener(popupMouseListener);
				removeMouseListenerToButton(popupMouseListener);
			}
		}
	}

	public boolean isChangable() {
		MouseListener[] ml = getMouseListeners();
		for (MouseListener l : ml) {
			if (l == popupMouseListener)
				return true;
		}
		return false;
	}

	public static PageComboBox create(Page page) {
		if (page == null)
			return null;
		PageComboBox comboBox = new PageComboBox();
		if (maker == null) {
			maker = new PageComboBoxMaker(comboBox);
		}
		else {
			maker.setObject(comboBox);
		}
		maker.invoke(page);
		if (maker.cancel)
			return null;
		return comboBox;
	}

	public void setupScripts(String input) {
		int lb = input.indexOf('{');
		int rb = input.indexOf('}');
		String[] str = null;
		final Map<String, String> map = new LinkedHashMap<String, String>();
		int lq, rq;
		String t, s;
		while (lb != -1 && rb != -1) {
			str = input.substring(lb + 1, rb).split(PATTERN);
			lq = str[1].indexOf('"');
			rq = str[1].lastIndexOf('"');
			if (lq != -1 && rq != -1 && lq != rq)
				t = str[1].substring(lq + 1, rq).trim();
			else t = str[1].trim();
			lq = str[2].indexOf('"');
			rq = str[2].lastIndexOf('"');
			if (lq != -1 && rq != -1 && lq != rq)
				s = str[2].substring(lq + 1, rq).trim();
			else s = str[2].trim();
			map.put(t, s);
			lb = input.indexOf('{', lb + 1);
			rb = input.indexOf('}', rb + 1);
		}
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				String tooltip = getToolTipText();
				Action a = getAction();
				PageComboBox.super.setAction(null);
				removeAllItems();
				Object key;
				int i = 0;
				for (Iterator it = map.keySet().iterator(); it.hasNext();) {
					key = it.next();
					addItem(key);
					putClientProperty("script" + i++, map.get(key));
				}
				adjustSize();
				PageComboBox.super.setAction(a);
				setToolTipText(tooltip);
				Object o = PageComboBox.this.getClientProperty("Selected Index");
				if (o instanceof Integer) {
					ModelerUtilities.selectWithoutNotifyingListeners(PageComboBox.this, ((Integer) o).intValue());
				}
				else {
					ModelerUtilities.selectWithoutNotifyingListeners(PageComboBox.this, 0);
				}
			}
		});
	}

	private void enableComboBox(boolean b, Object source) {
		ComponentMaker.enable(this, b, source, modelID, modelClass, page);
	}

	private void resetSelectedIndex() {
		Object o = getClientProperty("Selected Index");
		if (o instanceof Integer) {
			ModelerUtilities.selectWithoutNotifyingListeners(this, ((Integer) o).intValue());
		}
		else {
			ModelerUtilities.selectWithoutNotifyingListeners(this, 0);
		}
	}

	public void modelUpdate(final ModelEvent e) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				modelUpdate2(e);
			}
		});
	}

	private void modelUpdate2(ModelEvent e) {
		Object src = e.getSource();
		switch (e.getID()) {
		case ModelEvent.SCRIPT_START:
			if (disabledAtScript)
				enableComboBox(false, src);
			break;
		case ModelEvent.SCRIPT_END:
			if (disabledAtScript)
				enableComboBox(true, src);
			break;
		case ModelEvent.MODEL_RUN:
			if (disabledAtRun)
				enableComboBox(false, src);
			break;
		case ModelEvent.MODEL_STOP:
			if (disabledAtRun)
				enableComboBox(true, src);
			break;
		case ModelEvent.MODEL_RESET:
			resetSelectedIndex();
			break;
		case ModelEvent.MODEL_INPUT:
			Action a = getAction();
			if (a != null) {
				Object o = a.getValue("state");
				if (o != null)
					setSelectedItem(o);
				String s = (String) a.getValue(Action.SHORT_DESCRIPTION);
				if ("Import a model".equals(s)) {
					if (src instanceof PageMolecularViewer) {
						String fn = FileUtilities.getFileName(((PageMolecularViewer) src).getResourceAddress());
						String[] option = (String[]) a.getValue("options");
						if (option != null) {
							boolean b = false;
							for (int i = 0; i < option.length; i++) {
								if (option[i].equals(fn)) {
									b = true;
									break;
								}
							}
							super.setAction(null);
							if (!b) {
								setSelectedIndex(0);
							}
							else {
								setSelectedItem(fn);
							}
							super.setAction(a);
						}
					}
					else if (src instanceof Model) {
						o = ((Model) src).getProperty("url");
						if (o instanceof String) {
							String fn = FileUtilities.getFileName((String) o);
							String[] option = (String[]) a.getValue("options");
							if (option != null) {
								boolean b = false;
								for (int i = 0; i < option.length; i++) {
									if (option[i].equals(fn)) {
										b = true;
										break;
									}
								}
								super.setAction(null);
								if (!b) {
									setSelectedIndex(0);
								}
								else {
									setSelectedItem(fn);
								}
								super.setAction(a);
							}
						}
					}
				}
			}
		}
	}

	public String toString() {
		StringBuffer sb = new StringBuffer("<class>" + getClass().getName() + "</class>\n");
		if (modelClass != null)
			sb.append("<modelclass>" + modelClass + "</modelclass>\n");
		sb.append("<model>" + modelID + "</model>\n");
		if (optionGroup != null && !optionGroup.trim().equals(""))
			sb.append("<group>" + XMLCharacterEncoder.encode(optionGroup) + "</group>\n");
		boolean stateless = false;
		Action a = getAction();
		if (a != null) {
			sb.append("<action>" + a.getValue(Action.SHORT_DESCRIPTION) + "</action>\n");
			StringBuffer script = new StringBuffer();
			String d = (String) a.getValue(Action.SHORT_DESCRIPTION);
			if (ComponentMaker.isScriptActionKey(d)) {
				int n = getItemCount();
				for (int i = 0; i < n; i++) {
					script.append("{option=\"" + getItemAt(i) + "\", script=\"" + getClientProperty("script" + i)
							+ "\"}");
				}
			}
			if (script.length() > 0)
				sb.append("<script>" + XMLCharacterEncoder.encode(script.toString()) + "</script>\n");
			Object o = a.getValue("stateless");
			if (o instanceof Boolean)
				stateless = (Boolean) o;
		}
		if (!stateless && getSelectedIndex() != 0)
			sb.append("<selectedIndex>" + getSelectedIndex() + "</selectedIndex>\n");
		if (disabledAtRun)
			sb.append("<disabled_at_run>true</disabled_at_run>\n");
		if (disabledAtScript)
			sb.append("<disabled_at_script>true</disabled_at_script>\n");
		String toolTip = getToolTipText();
		if (toolTip != null) {
			if (a != null && !toolTip.equals(a.getValue(Action.SHORT_DESCRIPTION))) {
				sb.append("<tooltip>" + XMLCharacterEncoder.encode(toolTip) + "</tooltip>\n");
			}
		}
		return sb.toString();
	}

}
