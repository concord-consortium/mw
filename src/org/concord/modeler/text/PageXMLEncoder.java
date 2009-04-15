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
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.List;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.text.AbstractDocument;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.StyleConstants;
import javax.swing.text.html.HTML;

import org.concord.modeler.ActivityButton;
import org.concord.modeler.AudioPlayer;
import org.concord.modeler.HtmlService;
import org.concord.modeler.ModelCanvas;
import org.concord.modeler.Modeler;
import org.concord.modeler.ModelerUtilities;
import org.concord.modeler.PageApplet;
import org.concord.modeler.PageButton;
import org.concord.modeler.PageCheckBox;
import org.concord.modeler.PageJContainer;
import org.concord.modeler.PageMd3d;
import org.concord.modeler.PageMolecularViewer;
import org.concord.modeler.PageRadioButton;
import org.concord.modeler.draw.FillMode;
import org.concord.modeler.util.FileUtilities;
import org.concord.mw2d.models.MDModel;

/* Ad hoc XML-encoder for styled document. */

final class PageXMLEncoder {

	private final static String LINE_SEPARATOR = System.getProperty("line.separator");
	private final static float ZERO = 1000.f * Float.MIN_VALUE;

	private Page page;
	private JProgressBar progressBar;

	public PageXMLEncoder(Page page) {
		if (page == null)
			throw new IllegalArgumentException("null input");
		this.page = page;
	}

	void destroy() {
		page = null;
		progressBar = null;
	}

	public void setProgressBar(JProgressBar pb) {
		progressBar = pb;
	}

	public JProgressBar getProgressBar() {
		return progressBar;
	}

	private void setProgressMessage(final String message) {
		if (progressBar != null) {
			EventQueue.invokeLater(new Runnable() {
				public void run() {
					progressBar.setString(message);
				}
			});
		}
	}

	static void saveResource(final Component owner, final String name, final File parent) {
		switch (ModelerUtilities.copyResourceToDirectory(name, parent)) {
		case FileUtilities.SOURCE_NOT_FOUND:
			EventQueue.invokeLater(new Runnable() {
				public void run() {
					JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(owner), "Source " + name
							+ " is not found.", "File not found", JOptionPane.ERROR_MESSAGE);
				}
			});
			break;
		case FileUtilities.FILE_ACCESS_ERROR:
			EventQueue.invokeLater(new Runnable() {
				public void run() {
					JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(owner), "Directory " + parent
							+ " inaccessible.", "File access error", JOptionPane.ERROR_MESSAGE);
				}
			});
			break;
		case FileUtilities.WRITING_ERROR:
			EventQueue.invokeLater(new Runnable() {
				public void run() {
					JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(owner),
							"Encountered error while writing to directory " + parent, "Writing error",
							JOptionPane.ERROR_MESSAGE);
				}
			});
			break;
		}
	}

	public synchronized boolean write(final File file) {

		if (file == null)
			throw new IllegalArgumentException("null input file");

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

		String s = Modeler.getInternationalText("WritingTo");
		setProgressMessage((s != null ? s : "Writing to") + " " + file + "......");

		XMLCharacterEncoder.setCharacterEncoding(page.getCharacterEncoding());
		sb.append("<?xml version=\"1.0\" encoding=\"" + page.getCharacterEncoding() + "\"?>");
		sb.append(LINE_SEPARATOR);

		sb.append("<document>"); // document begins
		sb.append(LINE_SEPARATOR);

		sb.append("<language>" + page.getCharacterEncoding() + "</language>");
		sb.append(LINE_SEPARATOR);

		/* write background sound */
		s = page.getBackgroundSound();
		if (s != null) {
			saveResource(page, page.getPathBase() + FileUtilities.getFileName(s), file.getParentFile());
			if (page.getLoopBackgroundSound()) {
				sb.append("<bgsound loop=\"true\">" + XMLCharacterEncoder.encode(s) + "</bgsound>");
			}
			else {
				sb.append("<bgsound>" + XMLCharacterEncoder.encode(s) + "</bgsound>");
			}
			sb.append(LINE_SEPARATOR);
		}

		/* write the title of this document */
		s = page.getTitle();
		if (s != null && !s.trim().equals("")) {
			sb.append("<page_title>" + XMLCharacterEncoder.encode(s) + "</page_title>");
			sb.append(LINE_SEPARATOR);
		}

		/* write the names of the files linked indirectly to this page (e.g. those refered in scripts) */
		s = page.getAdditionalResourceFiles();
		if (s != null && !s.trim().equals("")) {
			sb.append("<referenced_files>" + XMLCharacterEncoder.encode(s) + "</referenced_files>");
			sb.append(LINE_SEPARATOR);
			ResourceFileService.saveAdditionalResourceFiles(file.getParentFile(), page);
		}

		FillMode fillMode = page.getFillMode();

		/* if in image fill mode, store a pointer to the image, and the background */
		if (fillMode instanceof FillMode.ImageFill) {
			Color background = page.getBackground();
			if (!background.equals(Color.white)) {
				sb.append("<background>");
				sb.append("<Red>" + background.getRed() + "</Red>");
				sb.append("<Green>" + background.getGreen() + "</Green>");
				sb.append("<Blue>" + background.getBlue() + "</Blue>");
				sb.append("</background>");
				sb.append(LINE_SEPARATOR);
			}
			sb.append("<bg_image>"
					+ XMLCharacterEncoder.encode(FileUtilities.getFileName(page.getBackgroundImageName()))
					+ "</bg_image>");
			sb.append(LINE_SEPARATOR);
			String pb = page.getBackgroundImageName();
			if (pb != null) {
				if (page.isRemote()) {
					pb = page.getPathBase() + FileUtilities.getFileName(pb);
				}
				saveResource(page, pb, file.getParentFile());
			}
		}
		/* if in color fill mode, write the background color */
		else if (fillMode instanceof FillMode.ColorFill) {
			Color background = page.getBackground();
			if (!background.equals(Color.white)) {
				sb.append("<bg_color>");
				sb.append("<Red>" + background.getRed() + "</Red>");
				sb.append("<Green>" + background.getGreen() + "</Green>");
				sb.append("<Blue>" + background.getBlue() + "</Blue>");
				sb.append("</bg_color>");
				sb.append(LINE_SEPARATOR);
			}
		}
		/* if in gradient mode, write the information about it */
		else if (fillMode instanceof FillMode.GradientFill) {
			sb.append("<bg_gradient>");
			Color c = ((FillMode.GradientFill) fillMode).getColor1();
			sb.append("<gradient_color1>");
			sb.append("<Red>" + c.getRed() + "</Red>");
			sb.append("<Green>" + c.getGreen() + "</Green>");
			sb.append("<Blue>" + c.getBlue() + "</Blue>");
			sb.append("</gradient_color1>");
			sb.append(LINE_SEPARATOR);
			c = ((FillMode.GradientFill) fillMode).getColor2();
			sb.append("<gradient_color2>");
			sb.append("<Red>" + c.getRed() + "</Red>");
			sb.append("<Green>" + c.getGreen() + "</Green>");
			sb.append("<Blue>" + c.getBlue() + "</Blue>");
			sb.append("</gradient_color2>");
			sb.append(LINE_SEPARATOR);
			int i = ((FillMode.GradientFill) fillMode).getStyle();
			sb.append("<gradient_style>" + i + "</gradient_style>");
			sb.append(LINE_SEPARATOR);
			i = ((FillMode.GradientFill) fillMode).getVariant();
			sb.append("<gradient_variant>" + i + "</gradient_variant>");
			sb.append(LINE_SEPARATOR);
			sb.append("</bg_gradient>");
			sb.append(LINE_SEPARATOR);
		}
		/* if in pattern mode, write the information about it */
		else if (fillMode instanceof FillMode.PatternFill) {
			sb.append("<bg_pattern>");
			int i = ((FillMode.PatternFill) fillMode).getForeground();
			sb.append("<pattern_fg>" + Integer.toString(i, 16) + "</pattern_fg>");
			sb.append(LINE_SEPARATOR);
			i = ((FillMode.PatternFill) fillMode).getBackground();
			sb.append("<pattern_bg>" + Integer.toString(i, 16) + "</pattern_bg>");
			sb.append(LINE_SEPARATOR);
			i = ((FillMode.PatternFill) fillMode).getStyle();
			sb.append("<pattern_style>" + i + "</pattern_style>");
			sb.append(LINE_SEPARATOR);
			i = ((FillMode.PatternFill) fillMode).getCellWidth();
			sb.append("<pattern_width>" + i + "</pattern_width>");
			sb.append(LINE_SEPARATOR);
			i = ((FillMode.PatternFill) fillMode).getCellHeight();
			sb.append("<pattern_height>" + i + "</pattern_height>");
			sb.append(LINE_SEPARATOR);
			sb.append("</bg_pattern>");
			sb.append(LINE_SEPARATOR);
		}

		/* write the content of the document in plain text first */

		String text = null;
		try {
			text = doc.getText(0, doc.getLength());
		}
		catch (BadLocationException e) {
			e.printStackTrace();
			text = "Error in getting text from the document";
		}
		sb.append("<text>" + XMLCharacterEncoder.encode(text) + "</text>");
		sb.append(LINE_SEPARATOR);

		/* write the styles of the content next */

		sb.append("<section"); // section begins
		sb.append(" start=\"" + section.getStartOffset() + "\"");
		sb.append(" end=\"" + section.getEndOffset() + "\">");
		sb.append(LINE_SEPARATOR);

		/* a section is supposed to have a number of paragraphs, which are branch elements. */

		Enumeration enum1 = section.children();
		AbstractDocument.BranchElement paragraph = null;
		Enumeration enum2 = null;
		while (enum1.hasMoreElements()) {

			paragraph = (AbstractDocument.BranchElement) enum1.nextElement();

			sb.append("<paragraph"); // paragraph begins
			sb.append(" start=\"" + paragraph.getStartOffset() + "\"");
			sb.append(" end=\"" + paragraph.getEndOffset() + "\">");
			sb.append(LINE_SEPARATOR);

			enum2 = paragraph.getAttributeNames();

			Object name = null, attr = null;
			while (enum2.hasMoreElements()) {
				name = enum2.nextElement();
				attr = paragraph.getAttribute(name);
				if (name.equals(StyleConstants.Alignment)) {
					if (((Integer) attr).intValue() != StyleConstants.ALIGN_LEFT) {
						sb.append("<Alignment>" + attr + "</Alignment>");
						sb.append(LINE_SEPARATOR);
					}
				}
				else if (name.equals(StyleConstants.LineSpacing)) {
					if (Math.abs(((Float) attr).floatValue()) > ZERO) {
						sb.append("<LineSpacing>" + attr + "</LineSpacing>");
						sb.append(LINE_SEPARATOR);
					}
				}
				else if (name.equals(StyleConstants.FirstLineIndent)) {
					if (Math.abs(((Float) attr).floatValue()) > ZERO) {
						sb.append("<FirstLineIndent>" + attr + "</FirstLineIndent>");
						sb.append(LINE_SEPARATOR);
					}
				}
				else if (name.equals(StyleConstants.LeftIndent)) {
					if (Math.abs(((Float) attr).floatValue()) > ZERO) {
						sb.append("<LeftIndent>" + attr + "</LeftIndent>");
						sb.append(LINE_SEPARATOR);
					}
				}
				else if (name.equals(StyleConstants.RightIndent)) {
					if (Math.abs(((Float) attr).floatValue()) > ZERO) {
						sb.append("<RightIndent>" + attr + "</RightIndent>");
						sb.append(LINE_SEPARATOR);
					}
				}
				else if (name.equals(StyleConstants.SpaceAbove)) {
					if (Math.abs(((Float) attr).floatValue()) > ZERO) {
						sb.append("<SpaceAbove>" + attr + "</SpaceAbove>");
						sb.append(LINE_SEPARATOR);
					}
				}
				else if (name.equals(StyleConstants.SpaceBelow)) {
					if (Math.abs(((Float) attr).floatValue()) > ZERO) {
						sb.append("<SpaceBelow>" + attr + "</SpaceBelow>");
						sb.append(LINE_SEPARATOR);
					}
				}
				else if (name.equals(StyleConstants.Orientation)) {
					sb.append("<Orientation>" + attr + "</Orientation>");
					sb.append(LINE_SEPARATOR);
				}
			}

			enum2 = paragraph.children();
			AbstractDocument.LeafElement content = null;

			int dl = doc.getLength();
			boolean isLink = false;
			while (enum2.hasMoreElements()) {

				content = (AbstractDocument.LeafElement) enum2.nextElement();
				/* content offset might exceed document length!!!! */
				if (content.getStartOffset() == dl)
					break;
				sb.append("<content"); // content begins
				sb.append(" start=\"" + content.getStartOffset() + "\"");
				sb.append(" end=\"" + content.getEndOffset() + "\">");
				sb.append(LINE_SEPARATOR);

				isLink = isLinkElement(content.getAttributeNames());

				Enumeration enum3 = content.getAttributeNames();

				while (enum3.hasMoreElements()) {

					name = enum3.nextElement();
					attr = content.getAttribute(name);

					if (name.equals(StyleConstants.FontSize)) {
						int fs = (Integer) attr - page.getFontIncrement();
						if (fs != Page.getDefaultFontSize())
							sb.append("<size>" + fs + "</size>");
					}

					else if (name.equals(StyleConstants.FontFamily)) {
						if (!attr.toString().equalsIgnoreCase(Page.getDefaultFontFamily())) {
							sb.append("<family>" + attr + "</family>");
						}
					}

					else if (name.equals(StyleConstants.Bold)) {
						if ((Boolean) attr) {
							sb.append("<bold>true</bold>");
						}
					}

					else if (name.equals(StyleConstants.Italic)) {
						if ((Boolean) attr) {
							sb.append("<italic>true</italic>");
						}
					}

					else if (!isLink && name.equals(StyleConstants.Underline)) {
						if ((Boolean) attr) {
							sb.append("<underline>true</underline>");
						}
					}

					else if (name.equals(StyleConstants.Subscript)) {
						if ((Boolean) attr) {
							sb.append("<subscript>true</subscript>");
						}
					}

					else if (name.equals(StyleConstants.Superscript)) {
						if ((Boolean) attr) {
							sb.append("<superscript>true</superscript>");
						}
					}

					else if (name.equals(StyleConstants.StrikeThrough)) {
						if ((Boolean) attr) {
							sb.append("<strikethrough>true</strikethrough>");
						}
					}

					else if (!isLink && name.equals(StyleConstants.Foreground)) {
						Color c = (Color) attr;
						if (!c.equals(Color.black)) {
							sb.append("<foreground>");
							sb.append(c.getRed() + " " + c.getGreen() + " " + c.getBlue());
							sb.append("</foreground>");
						}
					}

					else if (name.toString().startsWith("$e")) {
						// save nothing for this attribute
					}

					else if (name.equals(StyleConstants.IconAttribute)) {
						if (attr != null) {
							sb.append("<icon>");
							if (attr instanceof ImageIcon) {
								String fn = FileUtilities.getFileName(attr.toString());
								sb.append(XMLCharacterEncoder.encode(fn));
								saveImageIcon(page, (ImageIcon) attr, file.getParentFile());
							}
							else if (attr instanceof BulletIcon) {
								sb.append(attr.getClass().getName());
							}
							else if (attr instanceof LineIcon) {
								sb.append(attr.toString());
							}
							sb.append("</icon>");
						}
					}

					else if (name.equals(StyleConstants.ComponentAttribute)) {

						if (attr instanceof ModelCanvas) {
							final ModelCanvas mc = (ModelCanvas) attr;
							final MDModel model = mc.getMdContainer().getModel();
							final String filename = (String) model.getProperty("filename");
							final File modelFile = createFile(file, filename);
							if (page.isRemote()) {
								model.output(modelFile);
								page.getProperties().put(mc, mc.getURL());
							}
							else {
								if (modelFile.exists()) {
									SaveComponentStateReminder.ask(page, filename, new Runnable() {
										public void run() {
											model.output(modelFile);
											page.getProperties().put(mc, mc.getURL());
										}
									}, new Runnable() {
										public void run() {
											model.resetWithoutAsking();
										}
									});
								}
								else {
									model.output(modelFile);
									page.getProperties().put(mc, mc.getURL());
								}
							}
						}
						else if (attr instanceof PageMolecularViewer) {
							PageMolecularViewer mv = (PageMolecularViewer) attr;
							File dir = file.getParentFile();
							String fileName = FileUtilities.getFileName(mv.getResourceAddress());
							if (fileName != null) {
								if (!new File(dir, fileName).exists())
									ModelerUtilities.copyResourceToDirectory(mv.getResourceAddress(), dir);
							}
							mv.output(new File(FileUtilities.removeSuffix(file.getAbsolutePath()) + "$" + mv.getIndex()
									+ ".jms"));
						}
						else if (attr instanceof PageMd3d) {
							PageMd3d md = (PageMd3d) attr;
							md.output(new File(FileUtilities.removeSuffix(file.getAbsolutePath()) + "$" + md.getIndex()
									+ ".mdd"));
						}
						else if (attr instanceof HtmlService) {
							saveLinkedFilesInHTML((HtmlService) attr, file.getParentFile());
						}
						else if (attr instanceof AudioPlayer) {
							AudioPlayer ap = (AudioPlayer) attr;
							String fileName = ap.getClipName();
							if (fileName != null) {
								saveResource(page, page.getPathBase() + FileUtilities.getFileName(fileName), file
										.getParentFile());
							}
						}
						else if (attr instanceof PageApplet) {
							PageApplet pa = (PageApplet) attr;
							pa.saveJars(file.getParentFile());
							pa.saveState(file);
						}
						else if (attr instanceof PageJContainer) {
							PageJContainer pjc = (PageJContainer) attr;
							pjc.saveResources(file.getParentFile());
							pjc.saveJars(file.getParentFile());
							pjc.saveState(file);
						}
						else if (attr instanceof ActivityButton || attr instanceof PageButton) {
							JButton button = (JButton) attr;
							Icon icon = button.getIcon();
							if (icon instanceof ImageIcon) {
								String image = ((ImageIcon) icon).getDescription();
								if (image != null && image.indexOf(":") == -1) {
									saveResource(page, page.getPathBase() + FileUtilities.getFileName(image), file
											.getParentFile());
								}
							}
						}
						else if (attr instanceof PageCheckBox) {
							PageCheckBox pcb = (PageCheckBox) attr;
							String imageFileName = pcb.getImageFileNameSelected();
							if (imageFileName != null && !imageFileName.trim().equals(""))
								saveResource(page, page.getPathBase() + FileUtilities.getFileName(imageFileName), file
										.getParentFile());
							imageFileName = pcb.getImageFileNameDeselected();
							if (imageFileName != null && !imageFileName.trim().equals(""))
								saveResource(page, page.getPathBase() + FileUtilities.getFileName(imageFileName), file
										.getParentFile());
						}
						else if (attr instanceof PageRadioButton) {
							PageRadioButton prb = (PageRadioButton) attr;
							String imageFileName = prb.getImageFileNameSelected();
							if (imageFileName != null && !imageFileName.trim().equals(""))
								saveResource(page, page.getPathBase() + FileUtilities.getFileName(imageFileName), file
										.getParentFile());
							imageFileName = prb.getImageFileNameDeselected();
							if (imageFileName != null && !imageFileName.trim().equals(""))
								saveResource(page, page.getPathBase() + FileUtilities.getFileName(imageFileName), file
										.getParentFile());
						}

						// do not write the convenient JButtons
						if (attr.getClass() != JButton.class) {
							// special treatment for embedded icons: remove their wrappers if any
							if (attr instanceof IconWrapper) {
								sb.append("<icon>");
								Icon icon = ((IconWrapper) attr).getIcon();
								if (icon instanceof ImageIcon) {
									String fn = FileUtilities.getFileName(((ImageIcon) icon).getDescription());
									sb.append(XMLCharacterEncoder.encode(fn));
									saveImageIcon(page, (ImageIcon) icon, file.getParentFile());
								}
								else if (icon instanceof LineIcon) {
									sb.append(icon.toString());
								}
								sb.append("</icon>");
							}
							else {
								sb.append("<component>" + attr + "</component>");
							}
						}

					}
					else {

						if ((name.equals(StyleConstants.Underline) || name.equals(StyleConstants.Foreground)) && isLink) {
							// styles associated with hyperlinks are treated differently.
						}
						else {
							if (!name.equals(StyleConstants.ResolveAttribute)) {
								sb.append("<" + name + ">");
								sb.append(XMLCharacterEncoder.encode(attr.toString()));
								sb.append("</" + name + ">");
							}
						}
					}

					// if(!name.toString().startsWith("$e")) sb.append(LINE_SEPARATOR);

				}

				sb.append("</content>"); // content ends
				sb.append(LINE_SEPARATOR);
				sb.append(LINE_SEPARATOR);

			}

			sb.append("</paragraph>"); // paragraph ends
			sb.append(LINE_SEPARATOR);

		}

		sb.append("</section>"); // section ends
		sb.append(LINE_SEPARATOR);

		sb.append("</document>"); // document ends

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
				e.printStackTrace();
			}
		}

		s = Modeler.getInternationalText("SavedTo");
		setProgressMessage((s != null ? s : "Saved to") + " " + file);

		return true;

	}

	static void saveImageIcon(Page page, ImageIcon icon, File parentFile) {
		String fn = FileUtilities.getFileName(icon.toString());
		if (page.isRemote()) {
			String path = page.getPathBase() == null ? icon.toString() : (page.getPathBase() + fn);
			saveResource(page, path, parentFile);
		}
		else {
			String path = icon.toString();
			File x = new File(path);
			if (!x.isAbsolute())
				x = new File(page.getPathBase(), path);
			if (x.exists()) {
				saveResource(page, x.getAbsolutePath(), parentFile);
			}
			else {
				if (icon.getImage() instanceof RenderedImage) {
					ModelerUtilities.write((RenderedImage) icon.getImage(), new File(parentFile, fn), false);
				}
				else {
					ModelerUtilities.saveImageIcon(icon, new File(parentFile, fn), false);
				}
			}
		}
		icon.setDescription(new File(parentFile, fn).toString());
	}

	/* save the images embedded in html body of these text components */
	private void saveLinkedFilesInHTML(HtmlService c, File parentFile) {
		String href = c.getAttribute("link", "href");
		if (href != null) {
			saveResource(page, page.getPathBase() + href, parentFile);
		}
		String bgImage = c.getAttribute("body", "background");
		if (bgImage != null) {
			saveResource(page, page.getPathBase() + bgImage, parentFile);
		}
		List<String> list = c.getImageNames();
		if (list != null && !list.isEmpty()) {
			try {
				for (String s : list) {
					saveResource(page, page.getPathBase() + FileUtilities.getFileName(s), parentFile);
				}
			}
			catch (Exception e) {
				// a missing file is not critical, stop propogating the exception upwards, so that
				// it will NOT cause the writing process to fail
				e.printStackTrace();
			}
		}
	}

	private boolean isLinkElement(Enumeration e) {
		if (e == null)
			return false;
		while (e.hasMoreElements()) {
			if (e.nextElement().equals(HTML.Attribute.HREF))
				return true;
		}
		return false;
	}

	static File createFile(File file, String filename) {
		StringBuffer sb = new StringBuffer(file.getAbsolutePath());
		int dot = sb.lastIndexOf(System.getProperty("file.separator"));
		sb.replace(dot + 1, sb.length(), filename);
		return new File(new String(sb));
	}

	static String unicode(String text) {
		String s = null;
		try {
			s = new String(text.getBytes("UTF-16"), "UTF-16");
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return s;
	}

}