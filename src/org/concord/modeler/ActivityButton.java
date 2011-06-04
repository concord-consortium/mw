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
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseListener;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;

import org.concord.modeler.text.Page;
import org.concord.modeler.text.XMLCharacterEncoder;

public class ActivityButton extends JButton implements Embeddable {

	Page page;
	String pageNameGroup;
	String reportTitle;
	boolean autoSize = true;
	private int index;
	private String uid;
	private boolean transparent;
	private boolean marked;
	private boolean wasOpaque;
	private String borderType;
	private Color buttonBackground;
	private static Color defaultButtonBackground, defaultButtonForeground;
	private JPopupMenu popupMenu;
	private static ActivityButtonMaker maker;
	private MouseListener popupMouseListener;

	public ActivityButton() {
		super();
		init();
	}

	public ActivityButton(String text) {
		super(text);
		init();
	}

	public ActivityButton(ActivityButton button, Page parent) {
		this();
		setPage(parent);
		setBackground(button.getBackground());
		if (!Page.isNativeLookAndFeelUsed()) {
			setBorderType(button.getBorderType());
			setOpaque(button.isOpaque());
		}
		setAction(button.getAction());
		setUid(button.uid);
		setText(button.getText());
		setIcon(button.getIcon());
		setToolTipText(button.getToolTipText());
		setAutoSize(button.autoSize);
		setPreferredSize(button.getPreferredSize());
		setPageNameGroup(button.pageNameGroup);
		setReportTitle(button.reportTitle);
		setChangable(page.isEditable());
		Object o = button.getClientProperty("hint");
		if (o != null)
			putClientProperty("hint", o);
		o = button.getClientProperty("script");
		if (o != null)
			putClientProperty("script", o);
		o = button.getClientProperty("grade_uri");
		if (o != null)
			putClientProperty("grade_uri", o);
	}

	public void destroy() {
		setAction(null);
		ActionListener[] al = getActionListeners();
		if (al != null) {
			for (ActionListener l : al)
				removeActionListener(l);
		}
		page = null;
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
		if (Page.isApplet()) {
			addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					if (ActivityButton.this.getAction() == null && reportTitle != null) {
						JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(ActivityButton.this),
								"<html>Report doesn't work in the applet mode. If you need this functionality,<br>please run the "
										+ Modeler.NAME + " software.</html>");
					}
				}
			});
		}
	}

	/** side effect of implementing Embeddable */
	public void createPopupMenu() {

		if (popupMenu != null)
			return;

		popupMenu = new JPopupMenu();
		popupMenu.setInvoker(this);

		String s = Modeler.getInternationalText("CustomizeButton");
		JMenuItem mi = new JMenuItem((s != null ? s : "Customize This Button") + "...");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (maker == null) {
					maker = new ActivityButtonMaker(ActivityButton.this);
				}
				else {
					maker.setObject(ActivityButton.this);
				}
				maker.invoke(page);
			}
		});
		popupMenu.add(mi);

		s = Modeler.getInternationalText("RemoveButton");
		mi = new JMenuItem(s != null ? s : "Remove This Button");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				page.removeComponent(ActivityButton.this);
			}
		});
		popupMenu.add(mi);

		s = Modeler.getInternationalText("CopyButton");
		mi = new JMenuItem(s != null ? s : "Copy This Button");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				page.copyComponent(ActivityButton.this);
			}
		});
		popupMenu.add(mi);

		popupMenu.pack();

	}

	public JPopupMenu getPopupMenu() {
		return popupMenu;
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

	public void setPage(Page p) {
		page = p;
		setReportTitle(null);
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
		for (MouseListener l : ml) {
			if (l == popupMouseListener)
				return true;
		}
		return false;
	}

	public static ActivityButton create(Page page) {
		if (page == null)
			return null;
		ActivityButton button = new ActivityButton();
		if (maker == null) {
			maker = new ActivityButtonMaker(button);
		}
		else {
			maker.setObject(button);
		}
		maker.invoke(page);
		if (maker.cancel)
			return null;
		return button;
	}

	// old setText method
	public void setText2(String text) {
		super.setText(text);
		if (text == null)
			return;
		if (!autoSize)
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

	public void setAutoSize(boolean b) {
		autoSize = b;
	}

	public void setPageNameGroup(String s) {
		pageNameGroup = s;
	}

	public String getPageNameGroup() {
		return pageNameGroup;
	}

	public void setReportTitle(String s) {
		reportTitle = s;
		page.setReportTitle(reportTitle);
	}

	public String getReportTitle() {
		return reportTitle;
	}

	boolean isActionHint() {
		String s = null;
		if (getAction() != null) {
			s = (String) getAction().getValue(Action.SHORT_DESCRIPTION);
		}
		return "Hint".equals(s);
	}

	boolean isActionScript() {
		String s = null;
		if (getAction() != null) {
			s = (String) getAction().getValue(Action.SHORT_DESCRIPTION);
		}
		return "Script".equals(s);
	}

	boolean isActionGrade() {
		String s = null;
		if (getAction() != null) {
			s = (String) getAction().getValue(Action.SHORT_DESCRIPTION);
		}
		return "Submit for grade".equals(s);
	}

	public String toString() {
		StringBuffer sb = new StringBuffer("<class>" + getClass().getName() + "</class>\n");
		if (uid != null)
			sb.append("<uid>" + uid + "</uid>\n");
		sb.append("<title>" + XMLCharacterEncoder.encode(getText()) + "</title>\n");
		Icon icon = getIcon();
		if (icon instanceof ImageIcon) {
			String imageFileName = ((ImageIcon) icon).getDescription();
			if (imageFileName.indexOf(":") == -1)
				sb.append("<imagefile>" + imageFileName + "</imagefile>\n");
		}
		String toolTip = getToolTipText();
		if (toolTip != null) {
			if (!toolTip.equals(getAction().getValue(Action.SHORT_DESCRIPTION))) {
				sb.append("<tooltip>" + XMLCharacterEncoder.encode(getToolTipText()) + "</tooltip>\n");
			}
		}
		if (reportTitle != null)
			sb.append("<description>" + XMLCharacterEncoder.encode(reportTitle) + "</description>\n");
		if (pageNameGroup != null)
			sb.append("<group>" + XMLCharacterEncoder.encode(pageNameGroup) + "</group>\n");
		if (!autoSize)
			sb.append("<width>" + getWidth() + "</width><height>" + getHeight() + "</height>\n");
		if (!getBackground().equals(defaultButtonBackground))
			sb.append("<bgcolor>" + Integer.toString(getBackground().getRGB(), 16) + "</bgcolor>\n");
		if (borderType != null)
			sb.append("<border>" + borderType + "</border>\n");
		if (transparent)
			sb.append("<opaque>false</opaque>\n");
		if (isActionHint()) {
			String s = (String) getClientProperty("hint");
			if (s != null && !s.trim().equals("")) {
				sb.append("<hint_text>" + XMLCharacterEncoder.encode(s) + "</hint_text>\n");
			}
		}
		if (isActionScript()) {
			String s = (String) getClientProperty("script");
			if (s != null && !s.trim().equals("")) {
				sb.append("<script>" + XMLCharacterEncoder.encode(s) + "</script>\n");
			}
		}
		if (isActionGrade()) {
			String s = (String) getClientProperty("grade_uri");
			if (s != null && !s.trim().equals("")) {
				sb.append("<gradeuri>" + XMLCharacterEncoder.encode(s) + "</gradeuri>");
			}
		}
		if (getAction() != null) {
			sb.append("<action>" + getAction().getValue(Action.SHORT_DESCRIPTION) + "</action>");
		}
		return sb.toString();
	}

}