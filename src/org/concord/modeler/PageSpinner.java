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
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseListener;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeListener;

import org.concord.modeler.event.AbstractChange;
import org.concord.modeler.event.ModelEvent;
import org.concord.modeler.text.Page;
import org.concord.modeler.text.XMLCharacterEncoder;

/**
 * This is a component containing a spin button and a label that can be embedded onto a page to control an embedded
 * model. Left-click this button will fire the desired change, right-click will invoke a dialog box for changing its
 * properties.
 */

public class PageSpinner extends JComponent implements Embeddable, ModelCommunicator {

	Page page;
	String modelClass;
	int modelID = -1;
	boolean disabledAtRun, disabledAtScript;
	JSpinner spinner;
	JLabel label;
	private int index;
	private String id;
	private boolean marked;
	private static Color defaultBackground, defaultForeground;
	private JPopupMenu popupMenu;
	private static PageSpinnerMaker maker;
	private MouseListener popupMouseListener;

	public PageSpinner() {

		setLayout(new BorderLayout(5, 5));
		setOpaque(false);
		spinner = new JSpinner(new SpinnerNumberModel(0.0, -1.0, 1.0, 0.1));
		popupMouseListener = new PopupMouseListener(this);
		label = new JLabel();
		label.addMouseListener(popupMouseListener);
		label.setFont(new Font(null, Font.PLAIN, Page.getDefaultFontSize() - 1));
		add(label, BorderLayout.WEST);
		add(spinner, BorderLayout.CENTER);

		JTextField t = ((JSpinner.DefaultEditor) spinner.getEditor()).getTextField();
		t.addMouseListener(popupMouseListener);
		if (defaultBackground == null)
			defaultBackground = t.getBackground();
		if (defaultForeground == null)
			defaultForeground = t.getForeground();

	}

	public PageSpinner(PageSpinner spinner, Page parent) {
		this();
		setPage(parent);
		setModelID(spinner.modelID);
		setModelClass(spinner.modelClass);
		setMinimum(((Double) spinner.getMinimum()).doubleValue());
		setMaximum(((Double) spinner.getMaximum()).doubleValue());
		setStepSize(((Double) spinner.getStepSize()).doubleValue());
		setValue(((Double) spinner.getValue()).doubleValue());
		setName(spinner.getName());
		setLabel(spinner.getLabel());
		autoSize();
		setDisabledAtRun(spinner.disabledAtRun);
		setDisabledAtScript(spinner.disabledAtScript);
		Object o = spinner.getScript();
		if (o instanceof String)
			setScript((String) o);
		ChangeListener[] cl = spinner.getChangeListeners();
		if (cl != null) {
			for (ChangeListener i : cl) {
				if (i instanceof AbstractChange)
					addChangeListener(i);
			}
		}
		setChangable(page.isEditable());
		setToolTipText(spinner.getToolTipText());
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
		setId(spinner.id);
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

	public void setToolTipText(String text) {
		super.setToolTipText(text);
		spinner.setToolTipText(text);
		label.setToolTipText(text);
	}

	public void destroy() {
		ChangeListener[] cl = spinner.getChangeListeners();
		if (cl != null) {
			for (ChangeListener i : cl)
				spinner.removeChangeListener(i);
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
		if (maker != null)
			maker.setObject(null);
	}

	public JPopupMenu getPopupMenu() {
		return popupMenu;
	}

	public void createPopupMenu() {
		popupMenu = new JPopupMenu();
		popupMenu.setInvoker(this);
		String s = Modeler.getInternationalText("CustomizeSpinner");
		JMenuItem mi = new JMenuItem((s != null ? s : "Customize This Spinner") + "...");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (maker == null) {
					maker = new PageSpinnerMaker(PageSpinner.this);
				}
				else {
					maker.setObject(PageSpinner.this);
				}
				maker.invoke(page);
			}
		});
		popupMenu.add(mi);
		s = Modeler.getInternationalText("RemoveSpinner");
		mi = new JMenuItem(s != null ? s : "Remove This Spinner");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				page.removeComponent(PageSpinner.this);
			}
		});
		popupMenu.add(mi);
		s = Modeler.getInternationalText("CopySpinner");
		mi = new JMenuItem(s != null ? s : "Copy This Spinner");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				page.copyComponent(PageSpinner.this);
			}
		});
		popupMenu.add(mi);
		popupMenu.pack();
	}

	public void setScript(String script) {
		spinner.putClientProperty("Script", script);
	}

	public String getScript() {
		return (String) spinner.getClientProperty("Script");
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

	public void setLabel(String s) {
		label.setText(s);
	}

	public String getLabel() {
		return label.getText();
	}

	public Comparable getMinimum() {
		return ((SpinnerNumberModel) spinner.getModel()).getMinimum();
	}

	public void setMinimum(double d) {
		((SpinnerNumberModel) spinner.getModel()).setMinimum(new Double(d));
	}

	public Comparable getMaximum() {
		return ((SpinnerNumberModel) spinner.getModel()).getMaximum();
	}

	public void setMaximum(double d) {
		((SpinnerNumberModel) spinner.getModel()).setMaximum(new Double(d));
	}

	public Number getStepSize() {
		return ((SpinnerNumberModel) spinner.getModel()).getStepSize();
	}

	public void setStepSize(double d) {
		((SpinnerNumberModel) spinner.getModel()).setStepSize(new Double(d));
	}

	public Object getValue() {
		return ((SpinnerNumberModel) spinner.getModel()).getValue();
	}

	public void setValue(double d) {
		((SpinnerNumberModel) spinner.getModel()).setValue(new Double(d));
	}

	public void addChangeListener(ChangeListener cl) {
		spinner.addChangeListener(cl);
	}

	public ChangeListener[] getChangeListeners() {
		return spinner.getChangeListeners();
	}

	/**
	 * A <code>PageSpinner</code> is allowed to control one variable. This method removes all previous custom change
	 * listeners. The change listener registered with its editor is the ONLY one that is always kept.
	 */
	protected void removeChangeListeners() {
		ChangeListener[] listeners = spinner.getChangeListeners();
		if (listeners == null)
			return;
		for (ChangeListener i : listeners) {
			if (!(i instanceof JSpinner.DefaultEditor)) {
				spinner.removeChangeListener(i);
			}
		}
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
		JTextField textField = ((JSpinner.DefaultEditor) spinner.getEditor()).getTextField();
		textField.setBackground(b ? page.getSelectionColor() : defaultBackground);
		textField.setForeground(b ? page.getSelectedTextColor() : defaultForeground);
		label.setOpaque(b ? true : false);
		label.setBackground(b ? page.getSelectionColor() : defaultBackground);
		label.setForeground(b ? page.getSelectedTextColor() : defaultForeground);
	}

	public boolean isMarked() {
		return marked;
	}

	public void setDisabledAtRun(boolean b) {
		disabledAtRun = b;
		if (!b) {
			EventQueue.invokeLater(new Runnable() {
				public void run() {
					spinner.setEnabled(true);
					label.setEnabled(true);
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
					spinner.setEnabled(true);
					label.setEnabled(true);
				}
			});
		}
	}

	public boolean getDisabledAtScript() {
		return disabledAtScript;
	}

	public void setChangable(boolean b) {
		JTextField t = ((JSpinner.DefaultEditor) spinner.getEditor()).getTextField();
		if (b) {
			if (!isChangable()) {
				t.addMouseListener(popupMouseListener);
				label.addMouseListener(popupMouseListener);
			}
		}
		else {
			if (isChangable()) {
				t.removeMouseListener(popupMouseListener);
				label.removeMouseListener(popupMouseListener);
			}
		}
	}

	public boolean isChangable() {
		JTextField t = ((JSpinner.DefaultEditor) spinner.getEditor()).getTextField();
		MouseListener[] ml = t.getMouseListeners();
		for (MouseListener x : ml) {
			if (x == popupMouseListener)
				return true;
		}
		return false;
	}

	public static PageSpinner create(Page page) {
		if (page == null)
			return null;
		PageSpinner spinner = new PageSpinner();
		if (maker == null) {
			maker = new PageSpinnerMaker(spinner);
		}
		else {
			maker.setObject(spinner);
		}
		maker.invoke(page);
		if (maker.cancel)
			return null;
		return spinner;
	}

	public void autoSize() {
		SpinnerNumberModel m = (SpinnerNumberModel) spinner.getModel();
		FontMetrics fm = spinner.getFontMetrics(spinner.getFont());
		int w = Math.max(fm.stringWidth(m.getMaximum().toString()), fm.stringWidth(m.getMinimum().toString()));
		int h = fm.getHeight();
		fm = label.getFontMetrics(label.getFont());
		if (label.getText() != null)
			w += fm.stringWidth(label.getText());
		Icon icon = label.getIcon();
		if (icon != null)
			w += icon.getIconWidth() + label.getIconTextGap();
		Dimension dim = new Dimension(w + 30, h + 8);
		setPreferredSize(dim);
		setMaximumSize(dim);
		setMinimumSize(dim);
	}

	AbstractChange getChange() {
		ChangeListener[] c = spinner.getChangeListeners();
		if (c == null || c.length == 0)
			return null;
		for (ChangeListener i : c) {
			if (i instanceof AbstractChange)
				return (AbstractChange) i;
		}
		return null;
	}

	private void enableSpinner(final boolean b, Object source) {
		if (modelID == -1)
			return;
		ModelCanvas mc = page.getComponentPool().get(modelID);
		if (mc == null)
			return;
		if (mc.getContainer().getModel() != source)
			return;
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				label.setEnabled(b);
				spinner.setEnabled(b);
			}
		});
	}

	public void modelUpdate(ModelEvent e) {
		Object src = e.getSource();
		switch (e.getID()) {
		case ModelEvent.MODEL_INPUT:
			if (src instanceof Model) {
				AbstractChange c = getChange();
				if (c != null) {
					spinner.setValue(new Double(c.getValue()));
				}
			}
			break;
		case ModelEvent.SCRIPT_START:
			if (disabledAtScript)
				enableSpinner(false, src);
			break;
		case ModelEvent.SCRIPT_END:
			if (disabledAtScript)
				enableSpinner(true, src);
			break;
		case ModelEvent.MODEL_RUN:
			if (disabledAtRun)
				enableSpinner(false, src);
			break;
		case ModelEvent.MODEL_STOP:
			if (disabledAtRun)
				enableSpinner(true, src);
			break;
		}
	}

	public String toString() {
		AbstractChange c = getChange();
		StringBuffer sb = new StringBuffer("<class>" + getClass().getName() + "</class>\n");
		sb.append("<title>" + XMLCharacterEncoder.encode(label.getText()) + "</title>\n");
		String s = getToolTipText();
		if (s == null || (c != null && s.equals(c.getProperty(AbstractChange.SHORT_DESCRIPTION)))) {
			// do nothing
		}
		else {
			sb.append("<tooltip>" + XMLCharacterEncoder.encode(s) + "</tooltip>\n");
		}
		if (modelClass != null)
			sb.append("<modelclass>" + getModelClass() + "</modelclass>\n");
		sb.append("<model>" + getModelID() + "</model>\n");
		sb.append("<minimum>" + getMinimum() + "</minimum>\n");
		sb.append("<maximum>" + getMaximum() + "</maximum>\n");
		sb.append("<step>" + getStepSize() + "</step>\n");
		sb.append("<value>" + getValue() + "</value>\n");
		if (disabledAtRun)
			sb.append("<disabled_at_run>true</disabled_at_run>\n");
		if (disabledAtScript)
			sb.append("<disabled_at_script>true</disabled_at_script>\n");
		if (c != null) {
			s = (String) c.getProperty(AbstractChange.SHORT_DESCRIPTION);
			if (ComponentMaker.isScriptActionKey(s)) {
				s = getScript();
				if (s != null)
					sb.append("<script>" + XMLCharacterEncoder.encode(s) + "</script>\n");
			}
		}
		if (c != null)
			sb.append("<change>" + XMLCharacterEncoder.encode(c.toString()) + "</change>\n");
		return sb.toString();
	}
}