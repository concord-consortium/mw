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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseListener;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JSlider;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeListener;

import org.concord.modeler.event.AbstractChange;
import org.concord.modeler.event.ModelEvent;
import org.concord.modeler.text.Page;
import org.concord.modeler.text.XMLCharacterEncoder;

public class PageSlider extends JSlider implements Embeddable, ModelCommunicator {

	private final static String LABEL_PATTERN = "(?i)(value[\\s&&[^\\r\\n]]*=[\\s&&[^\\r\\n]]*)|(label[\\s&&[^\\r\\n]]*=[\\s&&[^\\r\\n]]*)";

	Page page;
	int modelID = -1;
	String modelClass;
	double fmin = -1, fmax = -1, value;
	double scaleFactor = 1.0;
	int nstep = 50;
	boolean disabledAtRun, disabledAtScript;
	Map<String, String> actionLabelMap;
	private int index;
	private String id;
	private boolean marked;
	private boolean wasOpaque;
	private Color sliderBackground;
	private static Color defaultSliderBackground, defaultSliderForeground;
	private JPopupMenu popupMenu;
	private static PageSliderMaker maker;
	private MouseListener popupMouseListener;

	public PageSlider() {
		super();
		init();
	}

	public PageSlider(int o) {
		super(o);
		init();
	}

	public PageSlider(PageSlider slider, Page parent) {
		this();
		setPage(parent);
		setOrientation(slider.getOrientation());
		setModelID(slider.modelID);
		setModelClass(slider.modelClass);
		setName(slider.getName());
		setDoubleMinimum(slider.getDoubleMinimum());
		setDoubleMaximum(slider.getDoubleMaximum());
		setDoubleValue(slider.value);
		setNumberOfSteps(slider.nstep);
		adjustScale();
		setPaintTicks(slider.getPaintTicks());
		setTitle(slider.getTitle());
		setBorderType(slider.getBorderType());
		setOpaque(slider.isOpaque());
		setDisabledAtRun(slider.disabledAtRun);
		setDisabledAtScript(slider.disabledAtScript);
		setBackground(slider.getBackground());
		setPreferredSize(slider.getPreferredSize());
		ChangeListener[] cl = slider.getChangeListeners();
		if (cl != null) {
			for (ChangeListener i : cl) {
				if (i instanceof AbstractChange)
					addChangeListener(i);
			}
		}
		setChangable(page.isEditable());
		setToolTipText(slider.getToolTipText());
		BasicModel m = getBasicModel();
		if (m != null)
			m.addModelListener(this);
		Object o = slider.getClientProperty("Script");
		if (o instanceof String) {
			putClientProperty("Script", o);
		}
		o = slider.getClientProperty("Label");
		if (o instanceof String) {
			putClientProperty("Label", o);
			setupLabels((String) o);
		}
		setId(slider.id);
	}

	boolean isTargetClass() {
		return ComponentMaker.isTargetClass(modelClass);
	}

	private BasicModel getBasicModel() {
		return ComponentMaker.getBasicModel(page, modelClass, modelID);
	}

	public void destroy() {
		ChangeListener[] cl = getChangeListeners();
		if (cl != null) {
			for (ChangeListener i : cl)
				removeChangeListener(i);
		}
		BasicModel m = getBasicModel();
		if (m != null)
			m.removeModelListener(this);
		page = null;
		if (maker != null)
			maker.setObject(null);
	}

	private void init() {
		if (defaultSliderBackground == null)
			defaultSliderBackground = getBackground();
		if (defaultSliderForeground == null)
			defaultSliderForeground = getForeground();
		setSnapToTicks(true);
		setPaintTicks(true);
		setOpaque(false);
		popupMouseListener = new PopupMouseListener(this);
		addMouseListener(popupMouseListener);
		setBorder(new CompoundBorder(BorderFactory.createEmptyBorder(), new TitledBorder(BorderFactory
				.createEmptyBorder(), null, 0, 0)));
	}

	public JPopupMenu getPopupMenu() {
		return popupMenu;
	}

	public void createPopupMenu() {
		popupMenu = new JPopupMenu();
		popupMenu.setInvoker(this);
		String s = Modeler.getInternationalText("CustomizeSlider");
		JMenuItem mi = new JMenuItem((s != null ? s : "Customize This Slider") + "...");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (maker == null) {
					maker = new PageSliderMaker(PageSlider.this);
				}
				else {
					maker.setObject(PageSlider.this);
				}
				maker.invoke(page);
			}
		});
		popupMenu.add(mi);
		s = Modeler.getInternationalText("RemoveSlider");
		mi = new JMenuItem(s != null ? s : "Remove This Slider");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				page.removeComponent(PageSlider.this);
			}
		});
		popupMenu.add(mi);
		s = Modeler.getInternationalText("CopySlider");
		mi = new JMenuItem(s != null ? s : "Copy This Slider");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				page.copyComponent(PageSlider.this);
			}
		});
		popupMenu.add(mi);
		popupMenu.pack();
	}

	/**
	 * A <code>PageSlider</code> is allowed to control one variable. This method removes all previous * custom change
	 * listeners and then add the passed change listener.
	 */
	public void addChangeListener(ChangeListener cl) {
		if (cl == null)
			throw new IllegalArgumentException("Null change listener");
		ChangeListener[] listeners = getChangeListeners();
		if (listeners == null)
			return;
		for (ChangeListener i : listeners) {
			removeChangeListener(i);
		}
		super.addChangeListener(cl);
		setToolTipText(cl.toString());
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
			sliderBackground = getBackground();
		}
		else {
			setOpaque(wasOpaque);
		}
		setBackground(b ? page.getSelectionColor() : sliderBackground);
		setForeground(b ? page.getSelectedTextColor() : defaultSliderForeground);
	}

	public boolean isMarked() {
		return marked;
	}

	public void setOpaque(boolean b) {
		super.setOpaque(b);
		if (page == null)
			return;
		if (!b) {
			setForeground(new Color(0xffffff ^ page.getBackground().getRGB()));
		}
	}

	public void setForeground(Color c) {
		super.setForeground(c);
		setTitleColor(c);
		Dictionary d = getLabelTable();
		if (d != null) {
			Enumeration keys = d.keys();
			while (keys.hasMoreElements()) {
				Component label = (Component) d.get(keys.nextElement());
				label.setForeground(c);
			}
		}
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

	public String getBorderType() {
		return BorderManager.getBorder(this);
	}

	public void setBorderType(String s) {
		BorderManager.setBorder(this, s, page.getBackground());
	}

	public void setTitle(String title) {
		Border b = getBorder();
		boolean yes = false;
		if (b instanceof CompoundBorder) {
			if (((CompoundBorder) b).getInsideBorder() instanceof TitledBorder) {
				yes = true;
			}
		}
		if (!yes) {
			TitledBorder tb = new TitledBorder(BorderFactory.createEmptyBorder(), title, 0, 0);
			setBorder(new CompoundBorder(BorderFactory.createEmptyBorder(), tb));
			tb.setTitleFont(new Font(null, Font.PLAIN, Page.getDefaultFontSize() - 1));
		}
		else {
			TitledBorder tb = (TitledBorder) ((CompoundBorder) b).getInsideBorder();
			tb.setTitle(title);
			tb.setTitleFont(new Font(null, Font.PLAIN, Page.getDefaultFontSize() - 1));
		}
	}

	private void setTitleColor(Color c) {
		Border b = getBorder();
		if (b instanceof CompoundBorder) {
			if (((CompoundBorder) b).getInsideBorder() instanceof TitledBorder) {
				TitledBorder tb = (TitledBorder) ((CompoundBorder) b).getInsideBorder();
				tb.setTitleColor(c);
			}
		}
	}

	public String getTitle() {
		try {
			return ((TitledBorder) ((CompoundBorder) getBorder()).getInsideBorder()).getTitle();
		}
		catch (Exception e) {
			return null;
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

	/**
	 * set the scale factor for converting a floating number to an integer recognized by the slider.
	 */
	private void setScaleFactor(double scale) {
		scaleFactor = scale;
		putClientProperty(AbstractChange.SCALE, scale);
	}

	/**
	 * get the scale factor for converting a floating number to an integer recognized by the slider.
	 */
	public double getScaleFactor() {
		return scaleFactor;
	}

	public void setDoubleMinimum(double value) {
		fmin = value;
	}

	public double getDoubleMinimum() {
		return fmin;
	}

	public void setDoubleMaximum(double value) {
		fmax = value;
	}

	public double getDoubleMaximum() {
		return fmax;
	}

	public void setDoubleValue(double value) {
		this.value = value;
		setValue((int) (value * scaleFactor));
	}

	public double getDoubleValue() {
		return getValue() / scaleFactor;
	}

	public void setNumberOfSteps(int n) {
		nstep = n;
	}

	public int getNumberOfSteps() {
		return nstep;
	}

	public void setPreferredSize(Dimension dim) {
		if (dim == null)
			throw new IllegalArgumentException("null object");
		if (dim.width == 0 || dim.height == 0)
			throw new IllegalArgumentException("zero dimension");
		super.setMaximumSize(dim);
		super.setMinimumSize(dim);
		super.setPreferredSize(dim);
	}

	public static PageSlider create(Page page) {
		if (page == null)
			return null;
		PageSlider slider = new PageSlider();
		if (maker == null) {
			maker = new PageSliderMaker(slider);
		}
		else {
			maker.setObject(slider);
		}
		maker.invoke(page);
		if (maker.cancel)
			return null;
		return slider;
	}

	public void setupLabels(String input) {
		if (input == null) {
			setPaintLabels(false);
			return;
		}
		int lb = input.indexOf('{');
		int rb = input.indexOf('}');
		if (lb == -1 && rb == -1) {
			setPaintLabels(false);
			return;
		}
		String[] str = null;
		final Hashtable<Integer, JLabel> map = new Hashtable<Integer, JLabel>();
		int lq, rq;
		int i = 0;
		String t, s;
		while (lb != -1 && rb != -1) {
			str = input.substring(lb + 1, rb).split(LABEL_PATTERN);
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
			boolean b = false;
			try {
				i = Math.round(Float.valueOf(t) * (float) scaleFactor);
			}
			catch (NumberFormatException e) {
				b = true;
			}
			if (!b) {
				map.put(i, new JLabel(s));
			}
			lb = input.indexOf('{', lb + 1);
			rb = input.indexOf('}', rb + 1);
		}
		if (map.isEmpty()) {
			setPaintLabels(false);
		}
		else {
			setPaintLabels(true);
			setLabelTable(map);
			if (actionLabelMap == null)
				actionLabelMap = new HashMap<String, String>();
			actionLabelMap.put(getName(), input);
		}
	}

	public void adjustScale() {
		setScaleFactor(nstep / Math.abs(fmax - fmin));
		setMinimum(Math.round((float) (fmin * scaleFactor)));
		setMaximum(Math.round((float) (fmax * scaleFactor)));
		setValue(Math.round((float) (value * scaleFactor)));
		setMajorTickSpacing((getMaximum() - getMinimum()) / nstep);
		/*
		 * if(Math.abs(getValue()-scaleFactor*value)>0.01*value) { EventQueue.invokeLater(new Runnable(){ public void
		 * run(){ JOptionPane.showMessageDialog (JOptionPane.getFrameForComponent(PageSlider.this), "The current value
		 * "+value+" is changed to "+getValue()/scaleFactor "\nin order to be on the slider's scale. If this value is"
		 * "\nnot appropriate, please change it using the slider."); } }); }
		 */
	}

	AbstractChange getChange() {
		ChangeListener[] c = getChangeListeners();
		if (c == null || c.length == 0)
			return null;
		for (ChangeListener i : c) {
			if (i instanceof AbstractChange)
				return (AbstractChange) i;
		}
		return null;
	}

	private void enableSlider(boolean b, Object source) {
		ComponentMaker.enable(this, b, source, modelID, modelClass, page);
	}

	public void modelUpdate(final ModelEvent e) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				Object src = e.getSource();
				switch (e.getID()) {
				case ModelEvent.MODEL_INPUT:
					if (src instanceof Model) {
						AbstractChange c = getChange();
						if (c != null) {
							ChangeListener[] cl = getChangeListeners();
							for (ChangeListener i : cl)
								removeChangeListener(i);
							setValue((int) (value * scaleFactor));
							for (ChangeListener i : cl)
								addChangeListener(i);
						}
					}
					break;
				case ModelEvent.SCRIPT_START:
					if (disabledAtScript)
						enableSlider(false, src);
					break;
				case ModelEvent.SCRIPT_END:
					if (disabledAtScript)
						enableSlider(true, src);
					break;
				case ModelEvent.MODEL_RUN:
					if (disabledAtRun)
						enableSlider(false, src);
					break;
				case ModelEvent.MODEL_STOP:
					if (disabledAtRun)
						enableSlider(true, src);
					break;
				}
			}
		});
	}

	public String toString() {

		AbstractChange c = getChange();

		StringBuffer sb = new StringBuffer("<class>" + getClass().getName() + "</class>\n");
		if (modelClass != null)
			sb.append("<modelclass>" + getModelClass() + "</modelclass>\n");
		sb.append("<model>" + getModelID() + "</model>\n");
		sb.append("<value>" + getDoubleValue() + "</value>\n");
		sb.append("<minimum>" + getDoubleMinimum() + "</minimum>\n");
		sb.append("<maximum>" + getDoubleMaximum() + "</maximum>\n");
		sb.append("<nstep>" + getNumberOfSteps() + "</nstep>\n");

		String t = getTitle();
		if (t != null && !t.trim().equals(""))
			sb.append("<title>" + XMLCharacterEncoder.encode(t) + "</title>\n");
		t = getToolTipText();
		if (t == null || (c != null && t.equals(c.getProperty(AbstractChange.SHORT_DESCRIPTION)))) {
			// do nothing
		}
		else {
			sb.append("<tooltip>" + XMLCharacterEncoder.encode(t) + "</tooltip>\n");
		}
		sb.append("<width>" + getMaximumSize().width + "</width>\n");
		sb.append("<height>" + getMaximumSize().height + "</height>\n");
		if (getOrientation() == VERTICAL)
			sb.append("<orientation>" + VERTICAL + "</orientation>\n");

		if (isOpaque()) {
			sb.append("<bgcolor>" + Integer.toString(getBackground().getRGB(), 16) + "</bgcolor>\n");
		}
		else {
			sb.append("<opaque>false</opaque>\n");
		}
		if (disabledAtRun)
			sb.append("<disabled_at_run>true</disabled_at_run>\n");
		if (disabledAtScript)
			sb.append("<disabled_at_script>true</disabled_at_script>\n");
		if (getPaintTicks())
			sb.append("<tick>true</tick>\n");
		if (!getBorderType().equals(BorderManager.BORDER_TYPE[0]))
			sb.append("<border>" + getBorderType() + "</border>\n");

		if (getPaintLabels()) {
			Hashtable map = (Hashtable) getLabelTable();
			JLabel label;
			t = "";
			for (Object value : map.keySet()) {
				label = (JLabel) map.get(value);
				t += "{value=\"" + ((Integer) value / scaleFactor) + "\", label=\"" + label.getText() + "\"}";
			}
			if (!t.equals(""))
				sb.append("<labeltable>" + XMLCharacterEncoder.encode(t) + "</labeltable>\n");
		}

		if (c != null) {
			t = (String) c.getProperty(AbstractChange.SHORT_DESCRIPTION);
			if (ComponentMaker.isScriptActionKey(t)) {
				t = (String) getClientProperty("Script");
				if (t != null)
					sb.append("<script>" + XMLCharacterEncoder.encode(t) + "</script>\n");
			}
		}

		if (c != null)
			sb.append("<change>" + XMLCharacterEncoder.encode(c.toString()) + "</change>\n");

		return sb.toString();

	}
}