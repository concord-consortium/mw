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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.text.JTextComponent;
import javax.swing.text.html.HTMLDocument;

import org.concord.modeler.event.HotlinkListener;
import org.concord.modeler.text.Page;
import org.concord.modeler.text.XMLCharacterEncoder;
import org.concord.modeler.ui.BorderRectangle;
import org.concord.modeler.ui.HTMLPane;

/**
 * This class is not thread-safe.
 * 
 * @author Charles Xie
 * 
 */
public class ImageQuestion extends JPanel implements Embeddable, TransferListener, HtmlService, Searchable {

	Page page;
	int index;
	String name;
	private boolean marked;
	private boolean opened;
	private boolean changable;
	private String borderType = BorderRectangle.LOWERED_ETCHED_BORDER;

	private JSplitPane splitPane;
	private ThumbnailImagePanel thumbnailPanel;
	private ImageContainer imageContainer;
	private HTMLPane questionArea;
	private JPanel toolBarPanel;
	private JScrollPane scroller;
	private JPanel lowerPanel;
	private JButton openButton;
	private JPopupMenu popupMenu;
	private static ImageQuestionMaker maker;

	public ImageQuestion() {

		super(new BorderLayout());
		setOpaque(false);
		setPreferredSize(new Dimension(400, 300));

		questionArea = new HTMLPane("text/html", "<html><body marginwidth=5 marginheight=5>Question</body></html>");
		questionArea.setEditable(false);
		add(questionArea, BorderLayout.NORTH);

		lowerPanel = new JPanel(new BorderLayout());
		add(lowerPanel, BorderLayout.CENTER);

		splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		splitPane.setDividerLocation(splitPane.getPreferredSize().height);
		splitPane.setDividerSize(0);
		splitPane.setOneTouchExpandable(true);
		splitPane.setBorder(BorderFactory.createEtchedBorder());
		lowerPanel.add(splitPane, BorderLayout.CENTER);

		imageContainer = new ImageContainer();
		splitPane.setTopComponent(imageContainer);
		SnapshotGallery.sharedInstance().addSnapshotListener(imageContainer);

		thumbnailPanel = SnapshotGallery.sharedInstance().createThumbnailImagePanel();
		thumbnailPanel.addTransferListener(this);
		thumbnailPanel.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				// make sure the scaled size will be current
				imageContainer.setScaledSize(splitPane.getWidth(), splitPane.getHeight());
				thumbnailPanel.processMousePressedEvent(e);
				if (e.getClickCount() < 2)
					return;
				ImageIcon icon = SnapshotGallery.sharedInstance().loadSelectedAnnotatedImage();
				imageContainer.setImage(icon);
				imageContainer.repaint();
				storeAnswer();
			}
		});
		scroller = new JScrollPane(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		scroller.getViewport().setView(thumbnailPanel);

		toolBarPanel = new JPanel();
		lowerPanel.add(toolBarPanel, BorderLayout.SOUTH);

		String s = Modeler.getInternationalText("Open");
		openButton = new JButton(s != null ? s : "Open");
		openButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				toggleThumbnailPanel();
			}
		});
		openButton.setToolTipText("Open/close the Snapshot Gallery Panel for selection");
		toolBarPanel.add(openButton);

		s = Modeler.getInternationalText("Clear");
		JButton button = new JButton(s != null ? s : "Clear");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				name = null;
				imageContainer.setImage(null);
				imageContainer.repaint();
				storeAnswer();
				setInstruction(!opened);
			}
		});
		button.setToolTipText("Remove the current image");
		toolBarPanel.add(button);

		addMouseListener(new PopupMouseListener(this));

	}

	public ImageQuestion(ImageQuestion iq, Page parent) {
		this();
		setPage(parent);
		setBorderType(iq.borderType);
		setPreferredSize(iq.getPreferredSize());
		setQuestion(iq.getQuestion());
		setBackground(iq.getBackground());
		setChangable(page.isEditable());
	}

	public boolean isTextSelected() {
		return questionArea.getSelectedText() != null;
	}

	public void setOpaque(boolean b) {
		super.setOpaque(b);
		if (questionArea != null)
			questionArea.setOpaque(b);
	}

	public void setBackground(Color c) {
		// super.setBackground(c);
		if (questionArea != null)
			questionArea.setBackground(c);
	}

	public Color getBackground() {
		if (questionArea != null)
			return questionArea.getBackground();
		return super.getBackground();
	}

	private void toggleThumbnailPanel() {
		String s1 = Modeler.getInternationalText("Open");
		String s2 = Modeler.getInternationalText("Close");
		openButton.setText(opened ? (s1 != null ? s1 : "Open") : (s2 != null ? s2 : "Close"));
		if (opened) {
			splitPane.setDividerLocation(splitPane.getHeight());
		}
		else {
			if (splitPane.getBottomComponent() == null)
				splitPane.setBottomComponent(scroller);
			splitPane.setDividerLocation(splitPane.getHeight() - ThumbnailImagePanel.IMAGE_HEIGHT - 36);
		}
		setInstruction(opened);
		opened = !opened;
		thumbnailPanel.respondToSnapshotEvent(null);
	}

	void storeAnswer() {
		String key = page.getAddress() + "#" + ModelerUtilities.getSortableString(index, 3) + "%"
				+ ImageQuestion.class.getName();
		QuestionAndAnswer q = UserData.sharedInstance().getData(key);
		if (imageContainer.getImage() == null) {
			if (q != null)
				q.setAnswer(null);
			return;
		}
		name = SnapshotGallery.sharedInstance().getSelectedImageName();
		if (name == null) {
			if (q != null)
				q.setAnswer(null);
			return;
		}
		if (q != null) {
			q.setAnswer(name);
		}
		else {
			q = new QuestionAndAnswer(getQuestion(), name);
			UserData.sharedInstance().putData(key, q);
		}
		q.setTimestamp(System.currentTimeMillis());
	}

	private void setInstruction(boolean b) {
		if (imageContainer.getImage() != null)
			return;
		if (b) {
			String s = Modeler.getInternationalText("ClickOpenButtonAndThenDragThumbnailHere");
			imageContainer.setString(s != null ? s : "Click the Open Button,\nand then drag a thumbnail here.");
		}
		else {
			String s = Modeler.getInternationalText("DragThumbnailImageFromSnapshotPanelBelow");
			imageContainer.setString(s != null ? s : "Drag a thumbnail image\nfrom the snapshot panel below.");
		}
	}

	public void setBase(URL u) {
		if (questionArea.getDocument() instanceof HTMLDocument)
			((HTMLDocument) questionArea.getDocument()).setBase(u);
	}

	public URL getBase() {
		if (questionArea.getDocument() instanceof HTMLDocument)
			return ((HTMLDocument) questionArea.getDocument()).getBase();
		return null;
	}

	public List<String> getImageNames() {
		return questionArea.getImageNames();
	}

	public String getBackgroundImage() {
		return questionArea.getBackgroundImage();
	}

	public void cacheImages(String codeBase) {
		questionArea.cacheImages(codeBase);
	}

	public void useCachedImages(boolean b, String codeBase) {
		questionArea.useCachedImages(b, codeBase);
	}

	public JTextComponent getTextComponent() {
		return questionArea;
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
		questionArea.addHyperlinkListener(page);
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

	public void destroy() {
		page = null;
		thumbnailPanel.removeTransferListener(this);
		thumbnailPanel.destroy();
		imageContainer.destroy();
		if (maker != null) {
			maker.setObject(null);
		}
	}

	public void setPreferredSize(Dimension dim) {
		super.setPreferredSize(dim);
		super.setMinimumSize(dim);
		super.setMaximumSize(dim);
	}

	public void setIndex(int i) {
		index = i;
	}

	public int getIndex() {
		return index;
	}

	public void setMarked(boolean b) {
		marked = b;
	}

	public boolean isMarked() {
		return marked;
	}

	public String getBorderType() {
		return BorderManager.getBorder(this);
	}

	public void setBorderType(String s) {
		borderType = s;
		BorderManager.setBorder(this, s, page.getBackground());
	}

	public void addMouseListener(MouseListener listener) {
		super.addMouseListener(listener);
		questionArea.addMouseListener(listener);
		imageContainer.addMouseListener(listener);
	}

	public void removeMouseListener(MouseListener listener) {
		super.removeMouseListener(listener);
		questionArea.removeMouseListener(listener);
		imageContainer.removeMouseListener(listener);
	}

	public void setChangable(boolean b) {
		changable = b;
	}

	public boolean isChangable() {
		return changable;
	}

	public void setQuestion(String s) {
		questionArea.setText(s);
	}

	public String getQuestion() {
		return questionArea.getText();
	}

	/** Note: called by PageXMLDecoder only. */
	public void setImage(ImageIcon image) {
		Dimension d = getPreferredSize();
		imageContainer.setScaledSize(d.width, d.height - 80);
		imageContainer.setImage(image);
		name = image.getDescription();
	}

	/** Note: called by PageXMLDecoder only. */
	public void setImage(String name) {
		this.name = name;
		if (name == null)
			return;
		if (name.equals(QuestionAndAnswer.NO_ANSWER))
			return;
		Dimension d = getPreferredSize();
		imageContainer.setScaledSize(d.width, d.height - 80);
		imageContainer.setImage(SnapshotGallery.sharedInstance().loadAnnotatedImage(name));
	}

	/** Note: called internally only */
	public ImageIcon getImage() {
		if (name == null)
			return null;
		return getImage(name);
	}

	public static ImageIcon getImage(String name) {
		return SnapshotGallery.sharedInstance().loadAnnotatedImage(name);
	}

	public JPopupMenu getPopupMenu() {
		return popupMenu;
	}

	public void createPopupMenu() {

		popupMenu = new JPopupMenu();
		popupMenu.setInvoker(this);

		String s = Modeler.getInternationalText("CustomizeImageQuestion");
		final JMenuItem miCustomize = new JMenuItem((s != null ? s : "Customize This Image Question") + "...");
		miCustomize.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (maker == null) {
					maker = new ImageQuestionMaker(ImageQuestion.this);
				}
				else {
					maker.setObject(ImageQuestion.this);
				}
				maker.invoke(page);
			}
		});
		popupMenu.add(miCustomize);

		s = Modeler.getInternationalText("RemoveImageQuestion");
		final JMenuItem miRemove = new JMenuItem(s != null ? s : "Remove This Image Questoin");
		miRemove.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				page.removeComponent(ImageQuestion.this);
			}
		});
		popupMenu.add(miRemove);

		s = Modeler.getInternationalText("CopyImageQuestion");
		JMenuItem mi = new JMenuItem(s != null ? s : "Copy This Image Question");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				page.copyComponent(ImageQuestion.this);
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

	public static ImageQuestion create(Page page) {
		if (page == null)
			return null;
		ImageQuestion iq = new ImageQuestion();
		iq.setChangable(true);
		if (maker == null) {
			maker = new ImageQuestionMaker(iq);
		}
		else {
			maker.setObject(iq);
		}
		maker.invoke(page);
		if (maker.cancel)
			return null;
		return iq;
	}

	public void exportDone(TransferEvent e) {
		storeAnswer();
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				toggleThumbnailPanel();
			}
		});
	}

	public String toString() {
		StringBuffer sb = new StringBuffer("<class>" + getClass().getName() + "</class>");
		sb.append("<width>" + getWidth() + "</width>");
		sb.append("<height>" + getHeight() + "</height>");
		sb.append("<title>" + XMLCharacterEncoder.encode(questionArea.getText()) + "</title>");
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