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
import java.awt.EventQueue;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Enumeration;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JProgressBar;
import javax.swing.text.AbstractDocument;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.StyleConstants;
import javax.swing.text.html.HTML;

import org.concord.modeler.Embeddable;
import org.concord.modeler.JNLPSaver;
import org.concord.modeler.Model;
import org.concord.modeler.ModelCanvas;
import org.concord.modeler.ModelerUtilities;
import org.concord.modeler.draw.FillMode;
import org.concord.modeler.util.FileUtilities;

/**
 * This class converts the CML format into the HTML format, with the following conventions:
 * <p>
 * Embedded models:
 * <ul>
 * <li>An embedded model is converted into an embedded screenshot, which is linked with a JNLP file to launch the whole
 * page.
 * </ul>
 * </p>
 * <p>
 * Hyperlinks to CML pages:
 * <ul>
 * <li>If a hyperlink is relative, it is converted into a link to a HTML page, with an assumption that the CML page the
 * link points to will be converted into HTML as well. For example, if the linked page is "a.cml", then the hyperlink
 * will be converted into <code><a href="a.html"></code>.
 * <li>If a hyperlink is remote, it is converted into a JNLP link that will launch the CML page it points to.
 * </ul>
 * </p>
 * 
 * @author Charles Xie
 */

final class PageHTMLEncoder {

	private final static String LINE_SEPARATOR = System.getProperty("line.separator");
	private final static String FILE_SEPARATOR = System.getProperty("file.separator");

	private JProgressBar progressBar;
	private Page page;

	public PageHTMLEncoder(Page page) {
		if (page == null)
			throw new IllegalArgumentException("Page encoder does not accept null");
		this.page = page;
	}

	public void setProgressBar(JProgressBar pb) {
		progressBar = pb;
	}

	public JProgressBar getProgressBar() {
		return progressBar;
	}

	public boolean write(final File file) throws Exception {

		if (file == null)
			throw new IllegalArgumentException("encoder must write to a file");

		boolean isBold = false;
		boolean isItalic = false;
		boolean underline = false;
		boolean strikeThrough = false;
		boolean isSub = false;
		boolean isSup = false;
		boolean isList = false;
		boolean isHR = false;
		boolean alignmentSet = false;
		boolean newLine = false;
		boolean moreThanOneElement = false;

		int leftIndent = 0;
		int lastLeftIndent = 0;
		int brAbove = 0;
		int brBelow = 0;
		int iComponent = 0;

		String modelScreenshot = null;
		String href = null;
		String jnlpURL = null;
		String jnlpLink = null;
		String imgURL = null;
		String contentString = null;

		FileOutputStream out = null;
		try {
			out = new FileOutputStream(file);
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		if (out == null)
			return false;

		Document doc = page.getStyledDocument();
		AbstractDocument.BranchElement section = (AbstractDocument.BranchElement) doc.getDefaultRootElement();

		/* initially give the buffer a capacity of 10000 characters */
		StringBuffer sb = new StringBuffer(10000);

		if (progressBar != null) {
			EventQueue.invokeLater(new Runnable() {
				public void run() {
					progressBar.setIndeterminate(true);
					progressBar.setString("Writing......");
				}
			});
		}

		XMLCharacterEncoder.setCharacterEncoding(page.getCharacterEncoding());
		sb.append("<html>"); // document begins
		sb.append(LINE_SEPARATOR);
		sb.append("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=" + page.getCharacterEncoding()
				+ "\">");

		sb.append("<head>"); // head begins
		sb.append(LINE_SEPARATOR);

		/* write the title of this document */

		sb.append("<title>" + XMLCharacterEncoder.encode(page.getTitle()) + "</title>");
		sb.append(LINE_SEPARATOR);

		sb.append("</head>"); // head ends
		sb.append(LINE_SEPARATOR);

		/* body begins */

		/* set background */

		FillMode fillMode = page.getFillMode();
		if (fillMode instanceof FillMode.ImageFill) {
			sb.append("<body background=\"" + FileUtilities.getFileName(page.getBackgroundImageName()) + "\">");
			sb.append(LINE_SEPARATOR);
			String pb = page.getBackgroundImageName();
			if (pb != null) {
				if (page.isRemote()) {
					pb = page.getPathBase() + FileUtilities.getFileName(pb);
				}
				PageXMLEncoder.saveResource(page, pb, file.getParentFile());
			}
		}
		else if (fillMode instanceof FillMode.ColorFill) {
			Color background = page.getBackground();
			sb.append("<body bgcolor=\"#" + ModelerUtilities.convertToHexRGB(background) + "\">");
			sb.append(LINE_SEPARATOR);
		}
		else {
			sb.append("<body>");
		}
		sb.append(LINE_SEPARATOR);

		/* get the content of the document in plain text first */

		String text = null;
		try {
			text = doc.getText(0, doc.getLength());
		}
		catch (BadLocationException e) {
			e.printStackTrace();
			text = "Error in getting text from the document";
		}

		// a section is supposed to have a number of paragraphs, which are branch elements.

		Enumeration enum1 = section.children();
		AbstractDocument.BranchElement paragraph = null;
		AbstractDocument.LeafElement content = null;
		Enumeration enum2 = null;
		Object name = null, attr = null;

		while (enum1.hasMoreElements()) {

			paragraph = (AbstractDocument.BranchElement) enum1.nextElement();

			// paragraph begins

			enum2 = paragraph.getAttributeNames();
			alignmentSet = false;

			while (enum2.hasMoreElements()) {

				name = enum2.nextElement();
				attr = paragraph.getAttribute(name);

				if (name.equals(StyleConstants.Alignment)) {
					int align = ((Integer) attr).intValue();
					switch (align) {
					case StyleConstants.ALIGN_CENTER:
						sb.append("<p><div align=\"center\">");
						alignmentSet = true;
						break;
					case StyleConstants.ALIGN_RIGHT:
						sb.append("<p><div align=\"right\">");
						alignmentSet = true;
						break;
					}
				}
				else if (name.equals(StyleConstants.LeftIndent)) {
					leftIndent = (int) ((Float) attr).floatValue();
				}
				else if (name.equals(StyleConstants.SpaceAbove)) {
					brAbove = (int) (((Float) attr).floatValue() / 20.0f);
				}
				else if (name.equals(StyleConstants.SpaceBelow)) {
					brBelow = (int) (((Float) attr).floatValue() / 20.0f);
				}

			}

			for (int i = 0; i < brAbove; i++)
				sb.append("<br>");

			int paragraphStart = sb.length();

			enum2 = paragraph.children();

			newLine = false;
			moreThanOneElement = false;

			/* iterate the content elements in this paragraph */

			while (enum2.hasMoreElements()) {

				content = (AbstractDocument.LeafElement) enum2.nextElement();
				if (content.getEndOffset() > text.length()) {
					contentString = XMLCharacterEncoder.encode(text.substring(content.getStartOffset(), text.length()));
				}
				else {
					contentString = XMLCharacterEncoder.encode(text.substring(content.getStartOffset(), content
							.getEndOffset()));
				}
				if (contentString.length() == 1) {
					newLine = contentString.charAt(0) == '\n';
					if (newLine)
						continue;
				}

				moreThanOneElement = true;

				Enumeration enum3 = content.getAttributeNames();
				sb.append("<font");

				while (enum3.hasMoreElements()) {

					name = enum3.nextElement();
					attr = content.getAttribute(name);

					if (name.equals(StyleConstants.FontSize)) {
						int fs = ((Integer) attr).intValue() - page.getFontIncrement();
						int fs1 = 1 + fs / 10;
						sb.append(" size=\"" + fs1 + "\"");
					}

					else if (name.equals(StyleConstants.Foreground)) {
						Color c = (Color) attr;
						if (!c.equals(Color.black)) {
							sb.append(" color=\"#" + ModelerUtilities.convertToHexRGB(c) + "\"");
						}
					}

					else if (name.equals(StyleConstants.FontFamily)) {
						sb.append(" face=\"" + attr + "\"");
					}

					else if (name.equals(StyleConstants.Bold)) {
						isBold = ((Boolean) attr).booleanValue();
					}

					else if (name.equals(StyleConstants.Italic)) {
						isItalic = ((Boolean) attr).booleanValue();
					}

					else if (name.equals(StyleConstants.Underline)) {
						underline = ((Boolean) attr).booleanValue();
					}

					else if (name.equals(StyleConstants.Subscript)) {
						isSub = ((Boolean) attr).booleanValue();
					}

					else if (name.equals(StyleConstants.Superscript)) {
						isSup = ((Boolean) attr).booleanValue();
					}

					else if (name.equals(StyleConstants.StrikeThrough)) {
						strikeThrough = ((Boolean) attr).booleanValue();
					}

					else if (name.equals(HTML.Attribute.HREF)) {
						href = attr.toString();
						if (FileUtilities.isRemote(href)) {
							if (href.toLowerCase().endsWith(".cml")) {
								String parentDirectory = file.getParent();
								String fileName = FileUtilities.getFileName(href);
								jnlpLink = FileUtilities.changeExtension(parentDirectory + FILE_SEPARATOR + fileName,
										"jnlp");
								JNLPSaver.write(href, page.getTitle(), new File(jnlpLink));
							}
						}
						else {
							if (href.endsWith(".cml")) {
								href = href.replaceAll(".cml", ".html");
							}
							else if (href.endsWith(".CML")) {
								href = href.replaceAll(".CML", ".html");
							}
						}
					}

					else if (name.equals(StyleConstants.IconAttribute)) {
						if (attr instanceof ImageIcon) {
							String fn = FileUtilities.getFileName(attr.toString());
							PageXMLEncoder.saveImageIcon(page, (ImageIcon) attr, file.getParentFile());
							imgURL = fn;
						}
						else if (attr instanceof BulletIcon) {
							isList = true;
						}
						else if (attr instanceof LineIcon) {
							isHR = true;
						}
					}

					else if (name.equals(StyleConstants.ComponentAttribute)) {
						/* if there is a model, create a JNLP link to it */
						if (attr instanceof ModelCanvas) {
							Model model = ((ModelCanvas) attr).getMdContainer().getModel();
							String modelName = (String) model.getProperty("filename");
							String path = file.getParent() + FILE_SEPARATOR + modelName;
							jnlpURL = FileUtilities.changeExtension(path, "jnlp");
							JNLPSaver.write(page.getAddress(), page.getTitle(), new File(jnlpURL));
							modelScreenshot = modelName.replaceAll(".mml", "_mml");
							modelScreenshot = modelScreenshot.replaceAll(".gbl", "_gbl") + ".jpg";
							modelScreenshot = FileUtilities.getCodeBase(file.getAbsolutePath()) + modelScreenshot;
							ModelerUtilities.screenshot((ModelCanvas) attr, modelScreenshot, false);
						}
						else if (attr instanceof Embeddable) {
							String componentName = FileUtilities.getFileName(page.getAddress());
							componentName = FileUtilities.getPrefix(componentName) + "_" + iComponent + ".jpg";
							modelScreenshot = FileUtilities.getCodeBase(file.getAbsolutePath()) + componentName;
							JComponent c = (JComponent) attr;
							boolean opaque = c.isOpaque();
							c.setOpaque(true);
							ModelerUtilities.screenshot(c, modelScreenshot, false);
							c.setOpaque(opaque);
							iComponent++;
						}
					}

				}

				sb.append(">");

				if (isHR)
					sb.append("<hr>");

				if (jnlpLink != null) {
					sb.append("<a href=\"" + FileUtilities.getFileName(jnlpLink) + "\">");
				}
				else {
					if (href != null)
						sb.append("<a href=\"" + href + "\">");
				}

				if (isBold)
					sb.append("<b>");
				if (isItalic)
					sb.append("<i>");
				if (underline)
					sb.append("<u>");
				if (strikeThrough)
					sb.append("<s>");
				if (isSub)
					sb.append("<sub>");
				if (isSup)
					sb.append("<sup>");

				if (imgURL != null) {
					sb.append("<img src=\"" + imgURL + "\"/>");
				}
				else {
					sb.append(contentString);
				}

				if (modelScreenshot != null) {
					if (jnlpURL != null)
						sb.append("<a href=\"" + FileUtilities.getFileName(jnlpURL) + "\">");
					sb.append("<img src=\"" + FileUtilities.getFileName(modelScreenshot) + "\"/>");
					if (jnlpURL != null)
						sb.append("</a>");
				}

				if (isSup)
					sb.append("</sup>");
				if (isSub)
					sb.append("</sub>");
				if (strikeThrough)
					sb.append("</s>");
				if (underline)
					sb.append("</u>");
				if (isItalic)
					sb.append("</i>");
				if (isBold)
					sb.append("</b>");
				if (href != null)
					sb.append("</a>");

				sb.append("</font>");

				// font ends

				isHR = false;
				isBold = false;
				isItalic = false;
				underline = false;
				strikeThrough = false;
				isSub = false;
				isSup = false;
				imgURL = null;
				href = null;
				jnlpLink = null;
				jnlpURL = null;
				modelScreenshot = null;

			}

			if (isList) {
				int d2 = (leftIndent - lastLeftIndent) / 40;
				if (d2 > 0) {
					for (int i = 0; i < d2; i++) {
						sb.insert(paragraphStart, "<ul>");
						paragraphStart += 4;
						sb.append("</ul>");
					}
				}
				sb.insert(paragraphStart, "<ul><li>");
				sb.append("</ul>");
			}

			if (!newLine || (newLine && moreThanOneElement)) {
				int mident = leftIndent / 30;
				for (int i = 0; i < mident; i++) {
					sb.insert(paragraphStart, "<dl>");
					sb.append("</dl>");
				}
			}

			for (int i = 0; i < brBelow; i++)
				sb.append("<br>");

			// paragraph ends
			if (alignmentSet) {
				sb.append("</div></p>");
			}
			else {
				sb.append("<br>");
			}
			sb.append(LINE_SEPARATOR);

			if (!isList)
				lastLeftIndent = leftIndent;

			isList = false;
			leftIndent = 0;
			brAbove = 0;
			brBelow = 0;

		}

		sb.append("</body>"); // body ends
		sb.append(LINE_SEPARATOR);

		sb.append("</html>"); // html ends

		try {
			out.write(sb.toString().getBytes());
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		finally {
			try {
				out.close();
			}
			catch (IOException e) {
			}
		}

		if (progressBar != null) {
			EventQueue.invokeLater(new Runnable() {
				public void run() {
					progressBar.setString("Done");
					progressBar.setIndeterminate(false);
				}
			});
		}

		return true;

	}
}