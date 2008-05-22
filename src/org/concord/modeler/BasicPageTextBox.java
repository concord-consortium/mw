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
import java.net.MalformedURLException;
import java.util.Enumeration;

import javax.swing.BorderFactory;
import javax.swing.border.Border;
import javax.swing.text.AttributeSet;
import javax.swing.text.Element;
import javax.swing.text.ElementIterator;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;

import org.concord.modeler.event.HotlinkListener;
import org.concord.modeler.event.SelfScriptEvent;
import org.concord.modeler.event.SelfScriptListener;
import org.concord.modeler.text.IconWrapper;
import org.concord.modeler.text.Page;
import org.concord.modeler.ui.BorderRectangle;
import org.concord.modeler.ui.DashedLineBorder;
import org.concord.modeler.ui.TextBox;
import org.concord.modeler.util.FileUtilities;

/**
 * @author Charles Xie
 * 
 */
public abstract class BasicPageTextBox extends TextBox implements AutoResizable, Embeddable, Scriptable,
		SelfScriptListener {

	protected int index = -1;
	protected String id;
	protected Page page;
	protected boolean widthIsRelative, heightIsRelative;
	protected float widthRatio = 1, heightRatio = 1;
	protected Color textBackground;
	protected static Color defaultTextBackground, defaultTextForeground;

	private boolean wasOpaque;
	private boolean marked;
	private TextBoxScripter scripter;

	public BasicPageTextBox() {
		super("Your text");
		addSelfScriptListener(this);
	}

	public String runSelfScript(SelfScriptEvent e) {
		if (e == null)
			return "";
		if (e.getScript() == null)
			return "";
		return runScript(e.getScript());
	}

	public String runScript(String script) {
		if (scripter == null)
			scripter = new TextBoxScripter(this);
		return scripter.runScript(script);
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

	public boolean isChangable() {
		return true;
	}

	public void setChangable(boolean b) {
		if (b) {
			textBody.removeLinkMonitor();
		}
		else {
			textBody.addLinkMonitor();
		}
	}

	public void setMarked(boolean b) {
		marked = b;
		if (page == null)
			return;
		if (b) {
			wasOpaque = textBody.isOpaque();
			setOpaque(true);
			textBackground = textBody.getBackground();
		}
		else {
			setOpaque(wasOpaque);
		}
		setBackground(b ? page.getSelectionColor() : textBackground);
		textBody.setForeground(b ? page.getSelectedTextColor() : defaultTextForeground);
	}

	public boolean isMarked() {
		return marked;
	}

	public String getBorderType() {
		return BorderManager.getBorder(this);
	}

	public void setBorderType(String s) {
		BorderManager.setBorder(this, s, page.getBackground());
	}

	private Border createTitledBorder(Border border, String title) {
		return BorderFactory.createTitledBorder(border, title, 0, 0, null, new Color(0xffffff ^ page.getBackground()
				.getRGB()));
	}

	public void showBoundary(boolean b) {
		String t = "";
		if (this instanceof IconWrapper) {
			t = "Color bar: #" + (index + 1) + " (" + getWidth() + "x" + getHeight() + ")";
		}
		else {
			t = "Text box: #" + (index + 1) + " (" + getWidth() + "x" + getHeight() + ")";
		}
		String s = (String) getClientProperty("border");
		if (s == null || BorderRectangle.EMPTY_BORDER.equals(s)) {
			if (b) {
				setBorder(createTitledBorder(new DashedLineBorder(), t));
			}
			else {
				setBorderType(BorderRectangle.EMPTY_BORDER);
			}
		}
		else {
			if (b) {
				setBorder(createTitledBorder(getBorder(), t));
			}
			else {
				setBorderType(s);
			}
		}
		repaint();
	}

	public void setPage(Page p) {
		page = p;
		HotlinkListener[] listeners = getHotlinkListeners();
		if (listeners != null) {
			for (HotlinkListener l : listeners)
				removeHotlinkListener(l);
		}
		addHotlinkListener(page);
		/* Sun's HyperlinkListener added to make image map work */
		textBody.addHyperlinkListener(page);
		textBody.setPopupMenus(page.getPopupMenus());
		try {
			setBase(page.getURL());
		}
		catch (MalformedURLException e) {
			e.printStackTrace();
		}
		textBody.setFont(new Font(Page.getDefaultFontFamily(), Font.PLAIN, Page.getDefaultFontSize()));
	}

	public Page getPage() {
		return page;
	}

	public void setWidthRelative(boolean b) {
		widthIsRelative = b;
	}

	public boolean isWidthRelative() {
		return widthIsRelative;
	}

	public void setWidthRatio(float wr) {
		widthRatio = wr;
	}

	public float getWidthRatio() {
		return widthRatio;
	}

	public void setHeightRelative(boolean b) {
		heightIsRelative = b;
	}

	public boolean isHeightRelative() {
		return heightIsRelative;
	}

	public void setHeightRatio(float hr) {
		heightRatio = hr;
	}

	public float getHeightRatio() {
		return heightRatio;
	}

	public void setPreferredSize(Dimension dim) {
		super.setPreferredSize(dim);
		super.setMinimumSize(dim);
		super.setMaximumSize(dim);
	}

	public void decodeText(String s) {
		super.decodeText(s);
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				markVisitedLinks();
			}
		});
	}

	private void markVisitedLinks() {
		if (!(textBody.getDocument() instanceof HTMLDocument))
			return;
		HTMLDocument doc = (HTMLDocument) textBody.getDocument();
		ElementIterator i = new ElementIterator(doc);
		Element e;
		AttributeSet as;
		Enumeration en;
		Object name;
		String str;
		SimpleAttributeSet sas;
		while ((e = i.next()) != null) {
			as = e.getAttributes();
			en = as.getAttributeNames();
			while (en.hasMoreElements()) {
				name = en.nextElement();
				if (name == HTML.Tag.A) {
					if (as != null)
						as = (AttributeSet) as.getAttribute(name);
					if (as == null)
						continue;
					str = (String) as.getAttribute(HTML.Attribute.HREF);
					if (FileUtilities.isRelative(str))
						str = page.resolvePath(str);
					if (!FileUtilities.isRemote(str))
						str = FileUtilities.useSystemFileSeparator(str);
					if (HistoryManager.sharedInstance().wasVisited(str)) {
						sas = new SimpleAttributeSet();
						StyleConstants.setForeground(sas, Page.getVisitedColor());
						doc.setCharacterAttributes(e.getStartOffset(), e.getEndOffset() - e.getStartOffset(), sas,
								false);
					}
					break;
				}
			}
		}
	}

	public void destroy() {
		textBody.removeHyperlinkListener(page);
		removeHotlinkListener(page);
		page = null;
	}

}