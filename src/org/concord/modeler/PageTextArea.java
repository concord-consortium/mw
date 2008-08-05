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
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseListener;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.text.JTextComponent;
import javax.swing.text.html.HTMLDocument;

import org.concord.modeler.event.HotlinkListener;
import org.concord.modeler.event.TextInputEvent;
import org.concord.modeler.event.TextInputListener;
import org.concord.modeler.text.Page;
import org.concord.modeler.text.XMLCharacterEncoder;
import org.concord.modeler.ui.PastableTextArea;
import org.concord.modeler.ui.TextBox;

public class PageTextArea extends JPanel implements Embeddable, HtmlService, Searchable {

	Page page;
	int index;
	String id;
	String referenceAnswer;
	private boolean marked;
	private boolean changable;
	private static int defaultColumn = 40, defaultRow = 10;
	private static Color defaultBackground, defaultForeground;
	private JTextArea textArea;
	private BasicPageTextBox questionArea;
	private JPanel buttonPanel;
	private JScrollPane scrollPane;
	private JPopupMenu popupMenu;
	private List<TextInputListener> inputListeners;
	private static PageTextAreaMaker maker;

	public PageTextArea() {

		super(new BorderLayout());
		setPreferredSize(new Dimension(400, 200));

		textArea = new PastableTextArea(defaultRow, defaultColumn);
		textArea.addFocusListener(new FocusAdapter() {
			public void focusLost(FocusEvent e) {
				storeAnswer();
			}
		});

		if (defaultBackground == null)
			defaultBackground = textArea.getBackground();
		if (defaultForeground == null)
			defaultForeground = textArea.getForeground();

		scrollPane = new JScrollPane(textArea, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

		add(scrollPane, BorderLayout.CENTER);

		buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

		questionArea = new BasicPageTextBox() {
			public void createPopupMenu() {
			}

			public JPopupMenu getPopupMenu() {
				return null;
			}
		};
		questionArea.setText("<html><body marginwidth=5 marginheight=5>Question</body></html>");
		questionArea.setEditable(false);
		add(questionArea, BorderLayout.NORTH);

		MouseListener popupMouseListener = new PopupMouseListener(this);
		questionArea.addMouseListener(popupMouseListener);
		addMouseListener(popupMouseListener);

		// setOpaque(false);

	}

	public PageTextArea(PageTextArea t, Page parent) {
		this();
		setPage(parent);
		setTitle(t.getTitle());
		setRows(t.getRows());
		setColumns(t.getColumns());
		setPreferredSize(t.getPreferredSize());
		setOpaque(t.isOpaque());
		setBackground(t.getBackground());
		setBorderType(t.getBorderType());
		setChangable(page.isEditable());
		referenceAnswer = t.referenceAnswer;
		setId(t.id);
	}

	public void destroy() {
		questionArea.getHtmlPane().removeHyperlinkListener(page);
		page = null;
		if (maker != null)
			maker.setObject(null); // make sure this object is not held by someone
	}

	public void addTextInputListener(TextInputListener l) {
		if (inputListeners == null)
			inputListeners = new ArrayList<TextInputListener>();
		if (!inputListeners.contains(l))
			inputListeners.add(l);
	}

	public void removeTextInputListener(TextInputListener l) {
		if (inputListeners != null)
			inputListeners.remove(l);
	}

	private void notifyTextInputListeners(TextInputEvent e) {
		if (inputListeners == null || inputListeners.isEmpty())
			return;
		for (TextInputListener x : inputListeners) {
			x.textInput(e);
		}
	}

	private void storeAnswer() {
		if (page == null)
			return;
		String key = page.getAddress() + "#" + ModelerUtilities.getSortableString(index, 3) + "%"
				+ PageTextArea.class.getName();
		QuestionAndAnswer q = UserData.sharedInstance().getData(key);
		if (q != null) {
			if (textArea.getText() == null || textArea.getText().trim().equals("")) {
				q.setAnswer(QuestionAndAnswer.NO_ANSWER);
			}
			else {
				q.setAnswer(textArea.getText());
			}
		}
		else {
			q = new QuestionAndAnswer(questionArea.getText(), textArea.getText(), referenceAnswer);
			UserData.sharedInstance().putData(key, q);
		}
		q.setTimestamp(System.currentTimeMillis());
		TextInputEvent e = new TextInputEvent(this, textArea.getText());
		notifyTextInputListeners(e);
	}

	public JPopupMenu getPopupMenu() {
		return popupMenu;
	}

	public void createPopupMenu() {
		popupMenu = new JPopupMenu();
		popupMenu.setInvoker(this);
		String s = Modeler.getInternationalText("CustomizeTextArea");
		final JMenuItem miCustomize = new JMenuItem((s != null ? s : "Customize This Text Area") + "...");
		miCustomize.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (maker == null) {
					maker = new PageTextAreaMaker(PageTextArea.this);
				}
				else {
					maker.setObject(PageTextArea.this);
				}
				maker.invoke(page);
			}
		});
		popupMenu.add(miCustomize);
		s = Modeler.getInternationalText("RemoveTextArea");
		final JMenuItem miRemove = new JMenuItem(s != null ? s : "Remove This Text Area");
		miRemove.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				page.removeComponent(PageTextArea.this);
				String key = page.getAddress() + "#" + ModelerUtilities.getSortableString(index, 3) + "%"
						+ PageTextArea.class.getName();
				UserData.sharedInstance().removeData(key);
			}
		});
		popupMenu.add(miRemove);
		s = Modeler.getInternationalText("CopyTextArea");
		JMenuItem mi = new JMenuItem(s != null ? s : "Copy This Text Area");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				page.copyComponent(PageTextArea.this);
			}
		});
		popupMenu.add(mi);
		popupMenu.addPopupMenuListener(new PopupMenuListener() {
			public void popupMenuCanceled(PopupMenuEvent e) {
			}

			public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
			}

			public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
				miCustomize.setEnabled(changable);
				miRemove.setEnabled(changable);
			}
		});
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
		if (questionArea != null)
			questionArea.setOpaque(b);
		if (buttonPanel != null)
			buttonPanel.setOpaque(b);
	}

	public boolean isOpaque() {
		if (questionArea != null)
			return questionArea.isOpaque();
		return super.isOpaque();
	}

	public void setRows(int row) {
		textArea.setRows(row);
		setPreferredSize(new Dimension(getColumns() * getColumnWidth(), getDesirableHeight()));
	}

	public void setColumns(int col) {
		textArea.setColumns(col);
		setPreferredSize(new Dimension(col * getColumnWidth(), getDesirableHeight()));
	}

	public void setRowColumns(int row, int col) {
		textArea.setRows(row);
		textArea.setColumns(col);
		setPreferredSize(new Dimension(col * getColumnWidth(), getDesirableHeight()));
	}

	public void setPreferredSize(Dimension dim) {
		super.setPreferredSize(dim);
		super.setMaximumSize(dim);
		super.setMinimumSize(dim);
	}

	public int getRows() {
		return textArea.getRows();
	}

	public int getColumns() {
		return textArea.getColumns();
	}

	protected int getColumnWidth() {
		return textArea.getFontMetrics(textArea.getFont()).charWidth('m');
	}

	protected int getRowHeight() {
		return textArea.getFontMetrics(textArea.getFont()).getHeight();
	}

	int getDesirableHeight() {
		return getRowHeight() * getRows() + questionArea.getPreferredSize().height;
	}

	public void setBackground(Color c) {
		super.setBackground(c);
		if (buttonPanel != null)
			buttonPanel.setBackground(c);
		if (questionArea != null)
			questionArea.setBackground(c);
	}

	public Color getBackground() {
		if (buttonPanel != null)
			return buttonPanel.getBackground();
		return super.getBackground();
	}

	public String getBorderType() {
		return BorderManager.getBorder(this);
	}

	public void setBorderType(String s) {
		BorderManager.setBorder(this, s, page.getBackground());
	}

	public void setBase(URL u) {
		if (questionArea.getTextComponent().getDocument() instanceof HTMLDocument)
			((HTMLDocument) questionArea.getTextComponent().getDocument()).setBase(u);
	}

	public URL getBase() {
		if (questionArea.getTextComponent().getDocument() instanceof HTMLDocument)
			return ((HTMLDocument) questionArea.getTextComponent().getDocument()).getBase();
		return null;
	}

	public List<String> getImageNames() {
		return questionArea.getImageNames();
	}

	public String getBackgroundImage() {
		return questionArea.getBackgroundImage();
	}

	public String getAttribute(String tag, String name) {
		return questionArea.getAttribute(tag, name);
	}

	public void cacheLinkedFiles(String codeBase) {
		questionArea.cacheLinkedFiles(codeBase);
	}

	public void useCachedImages(boolean b, String codeBase) {
		questionArea.useCachedImages(b, codeBase);
	}

	public TextBox getQuestionTextBox() {
		return questionArea;
	}

	public JTextComponent getTextComponent() {
		return questionArea.getTextComponent();
	}

	public void setPage(Page p) {
		page = p;
		HotlinkListener[] listeners = questionArea.getHotlinkListeners();
		if (listeners != null) {
			for (HotlinkListener x : listeners)
				questionArea.removeHotlinkListener(x);
		}
		questionArea.addHotlinkListener(page);
		/* Sun's HyperlinkListener added to make image map work */
		questionArea.getHtmlPane().addHyperlinkListener(page);
		try {
			setBase(page.getURL());
		}
		catch (MalformedURLException e) {
			e.printStackTrace();
		}
		questionArea.setFont(new Font(null, Font.PLAIN, Page.getDefaultFontSize()));
	}

	public Page getPage() {
		return page;
	}

	public void setMarked(boolean b) {
		marked = b;
		if (page == null)
			return;
		textArea.setBackground(b ? page.getSelectionColor() : defaultBackground);
		textArea.setForeground(b ? page.getSelectedTextColor() : defaultForeground);
		questionArea.setOpaque(b);
		questionArea.setBackground(b ? page.getSelectionColor() : defaultBackground);
		questionArea.setForeground(b ? page.getSelectedTextColor() : defaultForeground);
	}

	public boolean isMarked() {
		return marked;
	}

	public void setChangable(boolean b) {
		changable = b;
		if (b) {
			questionArea.getHtmlPane().removeLinkMonitor();
		}
		else {
			questionArea.getHtmlPane().addLinkMonitor();
		}
	}

	public boolean isChangable() {
		return changable;
	}

	public void setTitle(String title) {
		int i = title.indexOf("<html>");
		if (i == -1)
			i = title.indexOf("<HTML>");
		questionArea.setText(i > 0 ? title.substring(i) : title);
	}

	public String getTitle() {
		return questionArea.getText();
	}

	public void setReferenceAnswer(String s) {
		referenceAnswer = s;
	}

	public String getReferenceAnswer() {
		return referenceAnswer;
	}

	public String getText() {
		return textArea.getText();
	}

	public void setText(String s) {
		textArea.setText(s);
	}

	public static PageTextArea create(Page page) {
		if (page == null)
			return null;
		PageTextArea area = new PageTextArea();
		area.setChangable(true);
		area.buttonPanel.setBackground(page.getBackground());
		if (maker == null) {
			maker = new PageTextAreaMaker(area);
		}
		else {
			maker.setObject(area);
		}
		maker.invoke(page);
		if (maker.cancel)
			return null;
		return area;
	}

	public String toString() {
		StringBuffer sb = new StringBuffer("<class>" + getClass().getName() + "</class>\n");
		sb.append("<width>" + getWidth() + "</width>\n");
		sb.append("<height>" + getHeight() + "</height>\n");
		if (getTitle() != null)
			sb.append("<title>" + XMLCharacterEncoder.encode(getTitle()) + "</title>\n");
		if (referenceAnswer != null && !referenceAnswer.trim().equals(""))
			sb.append("<description>" + XMLCharacterEncoder.encode(referenceAnswer) + "</description>\n");
		if (isOpaque()) {
			sb.append("<bgcolor>" + Integer.toString(getBackground().getRGB(), 16) + "</bgcolor>\n");
		}
		else {
			sb.append("<opaque>false</opaque>\n");
		}
		if (!getBorderType().equals(BorderManager.BORDER_TYPE[0])) {
			sb.append("<border>" + getBorderType() + "</border>\n");
		}
		return sb.toString();
	}

}