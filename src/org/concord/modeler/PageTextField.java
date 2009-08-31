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
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.text.JTextComponent;

import org.concord.modeler.event.HotlinkListener;
import org.concord.modeler.event.TextInputEvent;
import org.concord.modeler.event.TextInputListener;
import org.concord.modeler.text.Page;
import org.concord.modeler.text.XMLCharacterEncoder;
import org.concord.modeler.ui.PastableTextField;
import org.concord.modeler.ui.TextBox;

public class PageTextField extends JPanel implements Embeddable, HtmlService, Searchable {

	Page page;
	int index;
	String uid;
	String referenceAnswer;
	String layout = BorderLayout.NORTH;
	private boolean marked;
	private boolean changable;
	private static int defaultColumn = 40;
	private static Color defaultBackground, defaultForeground;
	private JTextField textField;
	private BasicPageTextBox questionArea;
	private JPanel buttonPanel;
	private JPopupMenu popupMenu;
	private List<TextInputListener> inputListeners;
	private static PageTextFieldMaker maker;

	public PageTextField() {

		super(new BorderLayout(0, 0));
		setPreferredSize(new Dimension(400, 60));

		textField = new PastableTextField(defaultColumn);
		textField.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				storeAnswer();
			}
		});
		textField.addFocusListener(new FocusAdapter() {
			public void focusLost(FocusEvent e) {
				storeAnswer();
			}
		});
		add(textField, BorderLayout.CENTER);

		if (defaultBackground == null)
			defaultBackground = textField.getBackground();
		if (defaultForeground == null)
			defaultForeground = textField.getForeground();

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

		setOpaque(false);

	}

	public PageTextField(PageTextField t, Page parent) {
		this();
		setPage(parent);
		setTitle(t.getTitle());
		setQuestionPosition(t.getQuestionPosition());
		setPreferredSize(t.getPreferredSize());
		setOpaque(t.isOpaque());
		setBackground(t.getBackground());
		setBorderType(t.getBorderType());
		setChangable(page.isEditable());
		referenceAnswer = t.referenceAnswer;
	}

	public void destroy() {
		questionArea.destroy();
		page = null;
		if (maker != null)
			maker.setObject(null);
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
				+ PageTextField.class.getName();
		QuestionAndAnswer q = UserData.sharedInstance().getData(key);
		if (q != null) {
			if (textField.getText() == null || textField.getText().trim().equals("")) {
				q.setAnswer(QuestionAndAnswer.NO_ANSWER);
			}
			else {
				q.setAnswer(textField.getText());
			}
		}
		else {
			q = new QuestionAndAnswer(questionArea.getText(), textField.getText(), referenceAnswer);
			UserData.sharedInstance().putData(key, q);
		}
		q.setTimestamp(System.currentTimeMillis());
		TextInputEvent e = new TextInputEvent(this, textField.getText());
		notifyTextInputListeners(e);
	}

	public JPopupMenu getPopupMenu() {
		return popupMenu;
	}

	public void createPopupMenu() {
		popupMenu = new JPopupMenu();
		popupMenu.setInvoker(this);
		String s = Modeler.getInternationalText("CustomizeTextField");
		final JMenuItem miCustomize = new JMenuItem((s != null ? s : "Customize This Text Field") + "...");
		miCustomize.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (maker == null) {
					maker = new PageTextFieldMaker(PageTextField.this);
				}
				else {
					maker.setObject(PageTextField.this);
				}
				maker.invoke(page);
			}
		});
		popupMenu.add(miCustomize);
		s = Modeler.getInternationalText("RemoveTextField");
		final JMenuItem miRemove = new JMenuItem(s != null ? s : "Remove This Text Field");
		miRemove.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				page.removeComponent(PageTextField.this);
				String key = page.getAddress() + "#" + ModelerUtilities.getSortableString(index, 3) + "%"
						+ PageTextField.class.getName();
				UserData.sharedInstance().removeData(key);
			}
		});
		popupMenu.add(miRemove);
		s = Modeler.getInternationalText("CopyTextField");
		JMenuItem mi = new JMenuItem(s != null ? s : "Copy This Text Field");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				page.copyComponent(PageTextField.this);
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

	public void setUid(String uid) {
		this.uid = uid;
	}

	public String getUid() {
		return uid;
	}

	public void setPreferredSize(Dimension dim) {
		super.setPreferredSize(dim);
		super.setMaximumSize(dim);
		super.setMinimumSize(dim);
	}

	private int getDesirableHeight() {
		if (layout.equals(BorderLayout.NORTH)) {
			int h1 = textField.getFontMetrics(textField.getFont()).getHeight();
			int h2 = 0;
			if (questionArea.getPreferredSize().height != 0) {
				h2 += questionArea.getPreferredSize().height;
			}
			else {
				h2 += questionArea.getFontMetrics(questionArea.getFont()).getHeight();
			}
			return h1 + h2;
		}
		int h1 = textField.getFontMetrics(textField.getFont()).getHeight();
		return h1;
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

	public void setBackground(Color c) {
		super.setBackground(c);
		if (questionArea != null)
			questionArea.setBackground(c);
		if (buttonPanel != null)
			buttonPanel.setBackground(c);
	}

	public Color getBackground() {
		if (buttonPanel == null)
			return super.getBackground();
		return buttonPanel.getBackground();
	}

	public String getBorderType() {
		return BorderManager.getBorder(this);
	}

	public void setBorderType(String s) {
		BorderManager.setBorder(this, s, page.getBackground());
	}

	public void setColumns(final int i) {
		textField.setColumns(i);
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				setPreferredSize(new Dimension(i * getColumnWidth(), getDesirableHeight()));
			}
		});
	}

	protected int getColumnWidth() {
		return textField.getFontMetrics(textField.getFont()).charWidth('m');
	}

	public int getColumns() {
		return textField.getColumns();
	}

	public void setMarked(boolean b) {
		marked = b;
		if (page == null)
			return;
		textField.setBackground(b ? page.getSelectionColor() : defaultBackground);
		textField.setForeground(b ? page.getSelectedTextColor() : defaultForeground);
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

	public TextBox getQuestionTextBox() {
		return questionArea;
	}

	public JTextComponent getTextComponent() {
		return questionArea.getTextComponent();
	}

	public Page getPage() {
		return page;
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
		setBase(page.getURL());
		questionArea.setFont(new Font(null, Font.PLAIN, Page.getDefaultFontSize()));
	}

	public void setBase(File file) {
		questionArea.setBase(file);
	}

	public void setBase(URL u) {
		questionArea.setBase(u);
	}

	public URL getBase() {
		return questionArea.getBase();
	}

	public List<String> getImageNames() {
		return questionArea.getImageNames();
	}

	public String getAttribute(String tag, String name) {
		return questionArea.getAttribute(tag, name);
	}

	public String getBackgroundImage() {
		return questionArea.getBackgroundImage();
	}

	public void cacheLinkedFiles(String codeBase) {
		questionArea.cacheLinkedFiles(codeBase);
	}

	public void setTitle(String title) {
		int i = title.indexOf("<html>");
		if (i == -1)
			i = title.indexOf("<HTML>");
		questionArea.setText(i > 0 ? title.substring(i) : title);
	}

	public String getTitle() {
		if (questionArea == null)
			return "Title";
		return questionArea.getText();
	}

	public void setReferenceAnswer(String s) {
		referenceAnswer = s;
	}

	public String getReferenceAnswer() {
		return referenceAnswer;
	}

	public String getText() {
		return textField.getText();
	}

	public void setText(String s) {
		textField.setText(s);
	}

	public void setQuestionPosition(String s) {
		layout = s;
		if (layout == null)
			return;
		add(questionArea, layout);
	}

	public String getQuestionPosition() {
		return layout;
	}

	public static PageTextField create(Page page) {
		if (page == null)
			return null;
		PageTextField field = new PageTextField();
		field.setChangable(true);
		if (maker == null) {
			maker = new PageTextFieldMaker(field);
		}
		else {
			maker.setObject(field);
		}
		maker.invoke(page);
		if (maker.cancel)
			return null;
		return field;
	}

	public String toString() {
		StringBuffer sb = new StringBuffer("<class>" + getClass().getName() + "</class>\n");
		if (uid != null)
			sb.append("<uid>" + uid + "</uid>\n");
		if (!layout.equals(BorderLayout.NORTH))
			sb.append("<layout>" + layout + "</layout>\n");
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