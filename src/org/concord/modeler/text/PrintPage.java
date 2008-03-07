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

package org.concord.modeler.text;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.print.PageFormat;
import java.awt.print.Pageable;
import java.awt.print.Printable;
import java.util.Enumeration;

import javax.swing.AbstractButton;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JTextPane;
import javax.swing.border.MatteBorder;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Element;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import org.concord.modeler.ModelerUtilities;
import org.concord.modeler.PageTextBox;

/*
 * Split a <code>Page</code> instance into multiple pages of paper size. All text needs to be re-arranged on the space
 * specified by the size of a printer paper. All the embedded components are replaced by their screenshots, i.e. their
 * ImageIcon instances).
 * 
 * @author Charles Xie
 */

class PrintPage extends JComponent implements Printable, Pageable {

	private static PageFormat pageFormat;

	/*
	 * the end offset of the last <b>content element</b> to be rendered on the current page
	 */
	private static int lastElement = -1;

	/*
	 * the end offset of the last <b>word</b> to be rendered on the current page, relative to the start offset of the
	 * last element.
	 */
	private static int lastWord = -1;

	/* index of the current page */
	private int pageIndex;

	private int xtp, ytp, wtp, htp;
	private Page page;

	private static String footer;

	static void reset() {
		lastElement = lastWord = -1;
	}

	static void setFooter(String s) {
		footer = s;
	}

	static void setPageFormat(PageFormat pf) {
		pageFormat = pf;
	}

	public PrintPage(Page page0, int pIndex) {

		if (page0 == null)
			throw new IllegalArgumentException("No page is input");
		if (pIndex < 0)
			throw new IllegalArgumentException("Page index cannot be negative");
		if (pIndex > pageIndex) {
			if (lastElement >= page0.getDocument().getLength())
				return;
			pageIndex = pIndex;
		}
		else {
			if (pageIndex != 0)
				throw new IllegalArgumentException("Current page index not greater than previous one");
		}
		page = page0;

		setBorder(new MatteBorder(1, 1, 3, 3, Color.black));
		setBackground(Color.white);
		setFocusable(false);
		setRequestFocusEnabled(false);
		setPreferredSize(new Dimension((int) pageFormat.getWidth(), (int) pageFormat.getHeight()));

		DefaultStyledDocument dsd = new DefaultStyledDocument();
		JTextPane tp = new JTextPane(dsd);
		tp.setEditable(false);
		PrintParameters pp = Page.getPrintParameters();
		xtp = (int) (pageFormat.getImageableX() + pp.getLeftMargin());
		ytp = (int) (pageFormat.getImageableY() + pp.getTopMargin());
		wtp = (int) (pageFormat.getImageableWidth() - pp.getLeftMargin() - pp.getRightMargin());
		htp = (int) (pageFormat.getImageableHeight() - pp.getTopMargin() - pp.getBottomMargin());
		tp.setBounds(xtp, ytp, wtp, htp);
		add(tp);

		StyledDocument doc = page.getStyledDocument();
		Element root = doc.getDefaultRootElement();
		Element para, content;
		SimpleAttributeSet sas;
		String text;
		int pCount = root.getElementCount(); // number of paragraphs on this page
		int eCount;
		int startOffset, endOffset;
		float indentScale = Page.getPrintParameters().getIndentScale();
		Rectangle rect = null;
		int iFirst = 0, temp = 0;

		/* IMPORTANT DEFINITIONS OF INDICES */

		// store the start offset of the last element in the original document on a split page
		int iLastElement = 0;

		// store the offset of the last word RELATIVE to the start offset of the last element on the previous
		// split page.
		int iLastWord = 0;

		// store the offset of the last character in the original document on a split page. It follows
		// that: iLastOffset = iLastElement + iLastWord.
		int iLastOffset = 0;

		/* start traversing the document tree */

		finishThisPage: for (int i = 0; i < pCount; i++) {

			para = root.getElement(i);
			// construct a mutable attribute set for the current paragraph
			sas = new SimpleAttributeSet(para.getAttributes());
			// reduce the left and right indents (because they appear longer on paper)
			StyleConstants.setFirstLineIndent(sas, StyleConstants.getFirstLineIndent(sas) * indentScale);
			StyleConstants.setLeftIndent(sas, StyleConstants.getLeftIndent(sas) * indentScale);
			StyleConstants.setRightIndent(sas, StyleConstants.getRightIndent(sas) * indentScale);
			dsd.setParagraphAttributes(para.getStartOffset() - Math.max(lastElement + lastWord, 0), para.getEndOffset()
					- para.getStartOffset(), sas, true);
			eCount = para.getElementCount(); // number of elements in a paragraph

			for (int j = 0; j < eCount; j++) {

				content = para.getElement(j);
				startOffset = content.getStartOffset();
				if (startOffset < lastElement)
					continue;

				endOffset = content.getEndOffset();
				iLastElement = Math.max(lastElement, 0);
				iLastWord = Math.max(lastWord, 0);
				iLastOffset = iLastElement + iLastWord;
				// construct a mutable attribute set for the current element
				sas = new SimpleAttributeSet(content.getAttributes());
				// reduce the font size to a portion of its original size
				StyleConstants.setFontSize(sas, (int) (StyleConstants.getFontSize(sas)
						* Page.getPrintParameters().getCharacterScale() * 0.5));

				try {

					if (startOffset == lastElement) {
						// when this element is the first one on this page, splitting of the last element between
						// the previous and the current page must be considered.
						text = doc.getText(startOffset + iLastWord, endOffset - startOffset - iLastWord);
						iFirst = 0;
						temp = iLastWord;
					}
					else {
						// if this element is not the first one on this page, it will not be split between pages.
						// the only thing we need to consider is its new offset (*iFirst*) on the current page.
						text = doc.getText(startOffset, endOffset - startOffset);
						iFirst = startOffset - iLastOffset;
						temp = 0;
					}

					handleComponent(sas, wtp);
					handleIcon(sas, wtp, htp);
					dsd.insertString(iFirst, text, sas);

					for (int k = startOffset + temp; k < endOffset; k++) {
						if (k < doc.getLength() - 1) { // ignore the last break-line
							rect = tp.modelToView(k - iLastOffset);
							if (rect.y + rect.height > htp) {
								lastElement = startOffset;
								lastWord = k - lastElement;
								// remove the already inserted segment of text, which will be rendered on the
								// next page. "k - iLastOffset" is the new offset of this *character* on the
								// current page.
								dsd.remove(k - iLastOffset, endOffset - k);
								break finishThisPage;
							}
						}
					}
				}
				catch (BadLocationException ble) {
					ble.printStackTrace();
				}

			}

		}

		// set the last element to the end of the document to terminate the page-formating process. This
		// constructor will then return no content (i.e. no JTextPane contained withint it).
		if (lastElement == -1 || (rect != null && rect.y + rect.height <= htp)) {
			lastElement = doc.getLength();
		}

	}

	/*
	 * fit images to the page's width and height. Also guard the case when a user puts in a large image that is longer
	 * than a page's height.
	 */
	private void handleIcon(SimpleAttributeSet sas, final int width, final int height) {
		Icon icon = StyleConstants.getIcon(sas);
		if (icon == null)
			return;
		boolean alreadyScaled = false;
		Object name = null;
		if (icon instanceof ImageIcon) {
			boolean tooTall = false;
			int w = icon.getIconWidth();
			int h = icon.getIconHeight();
			if (w > width) { // too wide
				// add 100 pixels to w to draw the rightmost pixels
				float s = (float) width / (float) (w + 100);
				if (s * h < height) {
					Enumeration e = sas.getAttributeNames();
					while (e.hasMoreElements()) {
						name = e.nextElement();
						if (name.equals(StyleConstants.IconAttribute)) {
							sas.removeAttribute(name);
							icon = ModelerUtilities.scaleImageIcon((ImageIcon) icon, s);
							StyleConstants.setIcon(sas, icon);
							alreadyScaled = true;
							break;
						}
					}
				}
				else {
					tooTall = true;
				}
			}
			else {
				if (h > height) {
					tooTall = true;
				}
			}
			if (tooTall) {
				float s = (float) height / (float) (h + 100);
				Enumeration e = sas.getAttributeNames();
				while (e.hasMoreElements()) {
					name = e.nextElement();
					if (name.equals(StyleConstants.IconAttribute)) {
						sas.removeAttribute(name);
						StyleConstants.setIcon(sas, ModelerUtilities.scaleImageIcon((ImageIcon) icon, s));
						break;
					}
				}
			}
			else {
				// when this is an embedded image not too big
				if (!alreadyScaled && Page.getPrintParameters().getImageScale() < 1.0f) {
					Enumeration e = sas.getAttributeNames();
					while (e.hasMoreElements()) {
						name = e.nextElement();
						if (name.equals(StyleConstants.IconAttribute)) {
							sas.removeAttribute(name);
							StyleConstants.setIcon(sas, ModelerUtilities.scaleImageIcon((ImageIcon) icon, Page
									.getPrintParameters().getImageScale()));
							break;
						}
					}
				}
			}
		}
		else if (icon instanceof LineIcon) {
			Enumeration e = sas.getAttributeNames();
			while (e.hasMoreElements()) {
				name = e.nextElement();
				if (name.equals(StyleConstants.IconAttribute)) {
					sas.removeAttribute(name);
					StyleConstants.setIcon(sas, new LineIcon((LineIcon) icon) {
						public int getIconWidth() {
							if (getWidth() <= 1)
								return (int) (width * getWidth());
							return (int) getWidth();
						}
					});
					break;
				}
			}
		}
		else if (icon instanceof BulletIcon) {
			Enumeration e = sas.getAttributeNames();
			while (e.hasMoreElements()) {
				name = e.nextElement();
				if (name.equals(StyleConstants.IconAttribute)) {
					sas.removeAttribute(name);
					StyleConstants.setIcon(sas, ((BulletIcon) icon).getScaledInstance(Page.getPrintParameters()
							.getImageScale()));
					break;
				}
			}
		}
	}

	/*
	 * Convert embedded components into images. Directly printing a component is buggy. FIXME: Rescaling component's
	 * screenshot causes the image to be fuzzy. What can we do?
	 */
	private JComponent handleComponent(SimpleAttributeSet sas, final int width) {
		JComponent component = (JComponent) StyleConstants.getComponent(sas);
		if (component == null)
			return null;
		boolean isIconWrapper = component instanceof IconWrapper;
		Object name = null;
		Enumeration e = sas.getAttributeNames();
		while (e.hasMoreElements()) {
			name = e.nextElement();
			if (name.equals(StyleConstants.ComponentAttribute)) {
				sas.removeAttribute(name);
				boolean b = true;
				Color c = component.getBackground();
				b = component.isOpaque();
				if (!b && !isIconWrapper) {
					component.setOpaque(true);
					component.setBackground(Color.white);
				}
				float scale = Page.getPrintParameters().getComponentScale();
				if (component instanceof PageTextBox) {
					PageTextBox t = (PageTextBox) component;
					if (t.isWidthRelative()) {
						scale = t.getWidthRatio() * width / t.getWidth();
					}
				}
				if (component instanceof AbstractButton || component instanceof JComboBox) {
					StyleConstants.setIcon(sas, ModelerUtilities.componentToImageIcon(component, component.toString(),
							false, 1.0f));
				}
				else {
					StyleConstants.setIcon(sas, ModelerUtilities.componentToImageIcon(component, component.toString(),
							false, scale));
				}
				if (!b && !isIconWrapper) {
					component.setOpaque(false);
					component.setBackground(c);
				}
				break;
			}
		}
		return component;
	}

	public void paint(Graphics g) {
		g.setColor(getBackground());
		g.fillRect(0, 0, getWidth(), getHeight());
		paintBorder(g);
		super.paint(g);
		g.setColor(Color.black);
		String s = "" + (pageIndex + 1);
		if (footer == null) {
			g.setFont(new Font("TimesRoman", Font.PLAIN, 12));
			FontMetrics fm = g.getFontMetrics();
			g.drawString(s, (getWidth() - fm.stringWidth(s)) / 2, ytp + htp + fm.getHeight() + 10);
		}
		else {
			g.setFont(new Font("TimesRoman", Font.ITALIC, 11));
			FontMetrics fm = g.getFontMetrics();
			g.drawString(footer, (getWidth() - fm.stringWidth(footer)) / 2, ytp + htp + fm.getHeight() + 10);
			g.setFont(new Font("TimesRoman", Font.PLAIN, 12));
			fm = g.getFontMetrics();
			g.drawString(s, getWidth() - fm.stringWidth(s) - 80, ytp + htp + fm.getHeight() + 10);
		}
	}

	/**
	 * The passed arguments <code>pageFormat0</code> and <code>pageIndex0</code> are not used. Instead, the page
	 * format and page index passed through the constructor are used to avoid possible inconsistency.
	 */
	public int print(Graphics g, PageFormat pageFormat0, int pageIndex0) {
		g.translate(0, 0);
		int w = (int) pageFormat.getWidth();
		int h = (int) pageFormat.getHeight();
		g.setClip(0, 0, w, h);
		paint(g);
		page.getProgressBar().setString("Printing " + page.getAddress() + " - p. " + (pageIndex + 1) + "......");
		return PAGE_EXISTS;
	}

	public int getNumberOfPages() {
		return 1;
	}

	public PageFormat getPageFormat(int pageIndex) {
		return pageFormat;
	}

	public Printable getPrintable(int pageIndex) {
		return this;
	}

}