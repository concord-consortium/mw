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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseListener;
import java.text.DecimalFormat;

import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import org.concord.modeler.event.ModelEvent;
import org.concord.modeler.event.MovieEvent;
import org.concord.modeler.event.MovieListener;
import org.concord.modeler.text.Page;
import org.concord.modeler.text.XMLCharacterEncoder;
import org.concord.modeler.util.DataQueue;
import org.concord.modeler.util.FloatQueue;

public class PageNumericBox extends JLabel implements Embeddable, ModelCommunicator, MovieListener {

	final static int INSTANTANEOUS = 0;
	final static int AVERAGE = 1;
	final static int RMSD = 2;

	Page page;
	String modelClass;
	int modelID = -1;
	int dataType = INSTANTANEOUS;
	float multiplier = 1;
	float addend;
	DecimalFormat formatter;
	double value;
	private int index;
	private String uid;
	private boolean marked;
	private String format = "Fixed point";
	private Color originalForeground;
	private JPopupMenu popupMenu;
	private static PageNumericBoxMaker maker;
	private MouseListener popupMouseListener;

	public PageNumericBox() {
		super("???");
		setOpaque(false);
		popupMouseListener = new PopupMouseListener(this);
		addMouseListener(popupMouseListener);
		formatter = new DecimalFormat("#");
		formatter.setMaximumFractionDigits(3);
		formatter.setMaximumIntegerDigits(3);
	}

	public PageNumericBox(PageNumericBox box, Page parent) {
		this();
		setPage(parent);
		setModelID(box.modelID);
		setDataType(box.getDataType());
		setValue(box.getValue());
		setMultiplier(box.getMultiplier());
		setAddend(box.getAddend());
		setFont(box.getFont());
		setBorderType(box.getBorderType());
		setFormat(box.getFormat());
		formatter = box.formatter;
		setForeground(box.getForeground());
		setDescription(box.getDescription());
		setPreferredSize(box.getPreferredSize());
		Model m = getModel();
		if (m != null) {
			m.addModelListener(this);
			if (!m.getRecorderDisabled())
				m.getMovie().addMovieListener(this);
		}
		setChangable(page.isEditable());
	}

	boolean isTargetClass() {
		return ComponentMaker.isTargetClass(modelClass);
	}

	private Model getModel() {
		return ComponentMaker.getModel(page, modelClass, modelID);
	}

	public void destroy() {
		Model m = getModel();
		if (m != null) {
			m.removeModelListener(this);
			if (m.getMovie() != null)
				m.getMovie().removeMovieListener(this);
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
		String s = Modeler.getInternationalText("CustomizeNumericBox");
		JMenuItem mi = new JMenuItem((s != null ? s : "Customize This Numeric Box") + "...");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (maker == null) {
					maker = new PageNumericBoxMaker(PageNumericBox.this);
				}
				else {
					maker.setObject(PageNumericBox.this);
				}
				maker.invoke(page);
			}
		});
		popupMenu.add(mi);
		s = Modeler.getInternationalText("RemoveNumericBox");
		mi = new JMenuItem(s != null ? s : "Remove This Numeric Box");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				page.removeComponent(PageNumericBox.this);
			}
		});
		popupMenu.add(mi);
		s = Modeler.getInternationalText("CopyNumericBox");
		mi = new JMenuItem(s != null ? s : "Copy This Numeric Box");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				page.copyComponent(PageNumericBox.this);
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

	public void setUid(String uid) {
		this.uid = uid;
	}

	public String getUid() {
		return uid;
	}

	public void setValue(final double value) {
		this.value = value;
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				if (multiplier == 1.0f) {
					setText(formatter.format(value + addend));
				}
				else {
					setText(formatter.format(value * multiplier + addend));
				}
			}
		});
	}

	public double getValue() {
		return value;
	}

	public void setDataType(int i) {
		dataType = i;
	}

	public int getDataType() {
		return dataType;
	}

	public void setAddend(float addend) {
		this.addend = addend;
	}

	public float getAddend() {
		return addend;
	}

	public void setMultiplier(float multiplier) {
		this.multiplier = multiplier;
	}

	public float getMultiplier() {
		return multiplier;
	}

	public void setDescription(String s) {
		setToolTipText(s);
	}

	public String getDescription() {
		return getToolTipText();
	}

	public void setPage(Page p) {
		page = p;
		setFont(new Font(Page.getDefaultFontFamily(), Font.PLAIN, Page.getDefaultFontSize()));
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
		if (b) {
			originalForeground = getForeground();
			setOpaque(true);
			setBackground(page.getSelectionColor());
		}
		else {
			setOpaque(false);
		}
		setForeground(b ? page.getSelectedTextColor() : originalForeground);
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

	public void setPreferredSize(Dimension dim) {
		if (dim == null)
			throw new IllegalArgumentException("null object");
		if (dim.width == 0 || dim.height == 0)
			throw new IllegalArgumentException("zero dimension");
		super.setMaximumSize(dim);
		super.setMinimumSize(dim);
		super.setPreferredSize(dim);
	}

	public String getBorderType() {
		return BorderManager.getBorder(this);
	}

	public void setBorderType(String s) {
		BorderManager.setBorder(this, s, page.getBackground());
	}

	public void setFormat(String format) {
		this.format = format;
		if (format != null) {
			if (format.equalsIgnoreCase("scientific notation")) {
				formatter.applyPattern("0.###E00");
			}
			else if (format.equalsIgnoreCase("fixed point")) {
				formatter.applyPattern("#");
			}
			else {
				formatter.applyPattern("#");
			}
		}
		else {
			formatter.applyPattern("#");
		}
	}

	public String getFormat() {
		return format;
	}

	public void setMaximumFractionDigits(int i) {
		formatter.setMaximumFractionDigits(i);
	}

	public void setMaximumIntegerDigits(int i) {
		formatter.setMaximumIntegerDigits(i);
	}

	public void setFontName(String name) {
		Font font = getFont();
		setFont(new Font(name, font.getStyle(), font.getSize()));
	}

	public void setFontSize(int size) {
		Font font = getFont();
		setFont(new Font(font.getName(), font.getStyle(), size));
	}

	public static PageNumericBox create(Page page) {
		if (page == null)
			return null;
		PageNumericBox box = new PageNumericBox();
		box.setBackground(page.getBackground());
		if (maker == null) {
			maker = new PageNumericBoxMaker(box);
		}
		else {
			maker.setObject(box);
		}
		maker.invoke(page);
		if (maker.cancel)
			return null;
		return box;
	}

	public void modelUpdate(ModelEvent e) {
		Object src = e.getSource();
		int id = e.getID();
		if (src instanceof Model) {
			if (id == ModelEvent.MODEL_CHANGED) {
				Model theModel = (Model) src;
				DataQueue q = theModel.getQueue(getDescription());
				if (q != null && !q.isEmpty() && q.getPointer() > 0) {
					if (q instanceof FloatQueue) {
						switch (dataType) {
						case INSTANTANEOUS:
							setValue(((FloatQueue) q).getCurrentValue());
							break;
						case AVERAGE:
							setValue(((FloatQueue) q).getAverage());
							break;
						case RMSD:
							setValue(((FloatQueue) q).getRMSDeviation());
							break;
						}
					}
				}
				repaint();
			}
			else if (id == ModelEvent.MODEL_RESET) {
				// should we do something?
			}
		}
	}

	public void frameChanged(MovieEvent e) {
		Model m = getModel();
		if (m == null)
			return;
		DataQueue q = m.getQueue(getDescription());
		int frame = e.getFrame();
		if (q instanceof FloatQueue) {
			switch (dataType) {
			case INSTANTANEOUS:
				setValue(((FloatQueue) q).getData(frame));
				break;
			case AVERAGE:
			case RMSD:
			}
		}
		repaint();
	}

	public String toString() {
		StringBuffer sb = new StringBuffer("<class>" + getClass().getName() + "</class>\n");
		if (modelClass != null)
			sb.append("<modelclass>" + modelClass + "</modelclass>\n");
		sb.append("<model>" + getModelID() + "</model>\n");
		if (dataType != INSTANTANEOUS)
			sb.append("<datatype>" + dataType + "</datatype>\n");
		sb.append("<description>" + XMLCharacterEncoder.encode(getDescription()) + "</description>\n");
		sb.append("<width>" + getWidth() + "</width>\n");
		sb.append("<height>" + getHeight() + "</height>\n");
		if (!format.equals("Fixed point"))
			sb.append("<format>" + format + "</format>\n");
		if (multiplier != 1.0f)
			sb.append("<multiplier>" + multiplier + "</multiplier>\n");
		if (addend != 0.0f)
			sb.append("<addend>" + addend + "</addend>\n");
		if (formatter.getMaximumFractionDigits() != 3)
			sb.append("<max_fraction_digits>" + formatter.getMaximumFractionDigits() + "</max_fraction_digits>\n");
		if (formatter.getMaximumIntegerDigits() != 3)
			sb.append("<max_integer_digits>" + formatter.getMaximumIntegerDigits() + "</max_integer_digits>\n");
		if (!getFont().getName().equals(Page.getDefaultFontFamily()))
			sb.append("<fontname>" + getFont().getName() + "</fontname>\n");
		if (getFont().getSize() != Page.getDefaultFontSize())
			sb.append("<fontsize>" + getFont().getSize() + "</fontsize>\n");
		if (!getBorderType().equals(BorderManager.BORDER_TYPE[0]))
			sb.append("<border>" + getBorderType() + "</border>\n");
		if (!getForeground().equals(Color.black))
			sb.append("<fgcolor>" + Integer.toString(getForeground().getRGB(), 16) + "</fgcolor>\n");
		return sb.toString();
	}

}
