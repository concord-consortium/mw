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

package org.concord.modeler.ui;

import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseMotionListener;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.AbstractListModel;
import javax.swing.ButtonModel;
import javax.swing.DefaultButtonModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.ElementIterator;
import javax.swing.text.StyledDocument;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.Option;

import org.concord.modeler.ConnectionManager;
import org.concord.modeler.ModelerUtilities;
import org.concord.modeler.event.HotlinkListener;
import org.concord.modeler.event.SelfScriptEvent;
import org.concord.modeler.event.SelfScriptListener;
import org.concord.modeler.util.FileUtilities;

/**
 * This subclass of JEditorPane pops up a menu when a hyperlink is right-clicked, and jumps to the target upon left or
 * middle click. Albeit being called HTMLPane, this class of component can be used to display plain text as well, if the
 * content type is set to be "text/plain".
 * 
 * @author Charles Xie
 */

public class HTMLPane extends MyEditorPane {

	private List<JComponent> componentList;
	private List<HtmlForm> formList;
	private JPopupMenu defaultPopupMenu, hyperlinkPopupMenu, imagePopupMenu;
	private Icon backgroundImage;
	private int iconWidth, iconHeight;
	private TextBox textBox;
	private List<SelfScriptListener> selfScriptListeners;

	private MouseMotionListener motionMonitor = new MouseMotionAdapter() {

		public void mouseMoved(MouseEvent e) {
			Point p = new Point(e.getX(), e.getY());
			String link = isHyperlink(p);
			if (link != null) {
				URL u = null;
				try {
					u = new URL(link);
				}
				catch (MalformedURLException ex) {
					// ex.printStackTrace();
				}
				fireLinkUpdate(new HyperlinkEvent(HTMLPane.this, HyperlinkEvent.EventType.ENTERED, u, link,
						getSourceElement(p)));
			}
			else {
				fireLinkUpdate(new HyperlinkEvent(HTMLPane.this, HyperlinkEvent.EventType.EXITED, null, null,
						getSourceElement(p)));
			}
		}

	};

	private MouseListener linkMonitor = new MouseAdapter() {

		public void mousePressed(MouseEvent e) {
			requestFocus();
			if (ModelerUtilities.isRightClick(e)) {
				Point p = new Point(e.getX(), e.getY());
				if (hyperlinkPopupMenu != null) {
					String link = isHyperlink(p);
					if (link != null) {
						hyperlinkPopupMenu.setName(link);
						hyperlinkPopupMenu.show(HTMLPane.this, p.x, p.y);
						return;
					}
				}
				if (imagePopupMenu != null) {
					Object image = isImage(p);
					if (image != null) {
						imagePopupMenu.setName(image.toString());
						imagePopupMenu.putClientProperty("copy disabled", Boolean.TRUE);
						if (textBox == null)
							imagePopupMenu.show(HTMLPane.this, p.x, p.y);
						else imagePopupMenu.show(textBox, p.x, p.y);
						return;
					}
				}
				if (defaultPopupMenu != null) {
					if (getSelectedText() == null) {
						if (textBox == null)
							defaultPopupMenu.show(HTMLPane.this, p.x, p.y);
						else defaultPopupMenu.show(textBox, p.x, p.y);
					}
				}
			}
		}

		public void mouseReleased(MouseEvent e) {
			if (ModelerUtilities.isRightClick(e))
				return;
			Point p = new Point(e.getX(), e.getY());
			String link = isHyperlink(p);
			if (link != null) {
				URL u = null;
				try {
					u = new URL(link);
				}
				catch (MalformedURLException ex) {
					// ex.printStackTrace();
				}
				fireLinkUpdate(new HyperlinkEvent(HTMLPane.this, HyperlinkEvent.EventType.ACTIVATED, u, link,
						getSourceElement(p)));
			}
		}

	};

	public void addSelfScriptListener(SelfScriptListener l) {
		if (selfScriptListeners == null)
			selfScriptListeners = new ArrayList<SelfScriptListener>();
		selfScriptListeners.add(l);
	}

	public void removeSelfScriptListener(SelfScriptListener l) {
		if (selfScriptListeners != null)
			selfScriptListeners.remove(l);
	}

	private void notifySelfScriptListeners(SelfScriptEvent e) {
		for (SelfScriptListener l : selfScriptListeners)
			l.runSelfScript(e);
	}

	/**
	 * This method is currently for setting the TextBox owner of this HTMLPane for use with IconWrapper. You do not have
	 * to set it if you are not concerned with IconWrapper.
	 */
	public void setTextBox(TextBox t) {
		textBox = t;
	}

	public void setBase(URL u) {
		if (getDocument() instanceof HTMLDocument)
			((HTMLDocument) getDocument()).setBase(u);
	}

	public URL getBase() {
		if (getDocument() instanceof HTMLDocument)
			return ((HTMLDocument) getDocument()).getBase();
		return null;
	}

	private Element getSourceElement(Point p) {
		if (getDocument() instanceof StyledDocument) {
			int pos = viewToModel(p);
			if (pos >= 0) {
				try {
					return ((StyledDocument) getDocument()).getCharacterElement(pos);
				}
				catch (Exception e) {
					e.printStackTrace();
					return null;
				}
			}
		}
		return null;
	}

	private String isHyperlink(Point p) {
		if (getDocument() instanceof HTMLDocument) {
			int pos = viewToModel(p);
			if (pos >= 0) {
				try {
					HTMLDocument doc = (HTMLDocument) getDocument();
					if (doc != null) {
						Element elem = doc.getCharacterElement(pos);
						if (elem != null) {
							AttributeSet a = elem.getAttributes();
							// FIXME!!!: If an image is hyperlinked, the href attribute will be truncated at the
							// position where the first whitespace appears, if the code base is remote.
							if (a != null) {
								AttributeSet b = (AttributeSet) a.getAttribute(HTML.Tag.A);
								if (b != null) {
									return (String) b.getAttribute(HTML.Attribute.HREF);
								}
							}
						}
					}
				}
				catch (Exception e) {
					e.printStackTrace();
					return null;
				}
			}
		}
		return null;
	}

	private Object isImage(Point p) {
		if (getDocument() instanceof StyledDocument) {
			int pos = viewToModel(p);
			if (pos >= 0) {
				try {
					StyledDocument doc = (StyledDocument) getDocument();
					if (doc != null) {
						Element elem = doc.getCharacterElement(pos);
						if (elem != null) {
							AttributeSet a = elem.getAttributes();
							if (a != null)
								return a.getAttribute(HTML.Attribute.SRC);
						}
					}
				}
				catch (Exception e) {
					e.printStackTrace();
					return null;
				}
			}
		}
		return null;
	}

	public HTMLPane() {
		super();
		init();
		setContentType("text/html");
	}

	public HTMLPane(String url) throws IOException {
		super(url);
		init();
		setContentType("text/html");
	}

	public HTMLPane(String type, String text) {
		super(type, text);
		init();
	}

	public HTMLPane(URL initialPage) throws IOException {
		super(initialPage);
		init();
		setContentType("text/html");
	}

	private void init() {
		addMouseListener(linkMonitor);
		addMouseMotionListener(motionMonitor);
	}

	public void setPopupMenus(JPopupMenu[] pm) {
		if (pm == null)
			return;
		for (JPopupMenu x : pm) {
			String label = x.getLabel();
			if ("Hyperlink".equals(label))
				hyperlinkPopupMenu = x;
			else if ("Image".equals(label))
				imagePopupMenu = x;
			else if ("Default".equals(label))
				defaultPopupMenu = x;
		}
	}

	public void setDefaultPopupMenu(JPopupMenu pm) {
		defaultPopupMenu = pm;
	}

	public JPopupMenu getDefaultPopupMenu() {
		return defaultPopupMenu;
	}

	public void addLinkMonitor() {
		if (!hasLinkMonitor()) {
			addMouseListener(linkMonitor);
		}
		if (!hasMotionMonitor()) {
			addMouseMotionListener(motionMonitor);
		}
	}

	public void removeLinkMonitor() {
		if (hasLinkMonitor()) {
			removeMouseListener(linkMonitor);
		}
		if (hasMotionMonitor()) {
			removeMouseMotionListener(motionMonitor);
		}
	}

	private boolean hasLinkMonitor() {
		MouseListener[] ml = getMouseListeners();
		if (ml == null)
			return false;
		for (MouseListener l : ml) {
			if (l == linkMonitor)
				return true;
		}
		return false;
	}

	private boolean hasMotionMonitor() {
		MouseMotionListener[] ml = getMouseMotionListeners();
		if (ml == null)
			return false;
		for (MouseMotionListener l : ml) {
			if (l == motionMonitor)
				return true;
		}
		return false;
	}

	public void addHotlinkListener(HotlinkListener l) {
		listenerList.add(HotlinkListener.class, l);
	}

	public void removeHotlinkListener(HotlinkListener l) {
		listenerList.remove(HotlinkListener.class, l);
	}

	/**
	 * Returns an array of all the <code>HotlinkListener</code>s added to this component with
	 * <code>addHotlinkListener()</code>.
	 * 
	 * @see org.concord.modeler.event.HotlinkListener
	 * @return all of the <code>HotlinkListener</code>s added or an empty array if no listeners have been added
	 */
	public HotlinkListener[] getHotlinkListeners() {
		return listenerList.getListeners(HotlinkListener.class);
	}

	/**
	 * Notifies all listeners that have registered interest for notification on this event type.
	 * 
	 * @param e
	 *            the event
	 */
	protected void fireLinkUpdate(HyperlinkEvent e) {
		// Guaranteed to return a non-null array
		Object[] listeners = listenerList.getListenerList();
		// Process the listeners last to first, notifying those that are interested in this event
		for (int i = listeners.length - 2; i >= 0; i -= 2) {
			if (listeners[i] == HotlinkListener.class) {
				((HotlinkListener) listeners[i + 1]).hotlinkUpdate(e);
			}
		}
	}

	void wireEmbeddedComponents() {
		if (getDocument().getLength() <= 0)
			return;
		if (formList == null)
			formList = new ArrayList<HtmlForm>();
		else formList.clear();
		ElementIterator i = new ElementIterator(getDocument().getDefaultRootElement());
		ElementIterator j = new ElementIterator(i.first());
		Object name = null, attr = null;
		Element elem = j.first();
		String elemName = null;
		HtmlForm form = null;
		while (elem != null) {
			elemName = elem.getName();

			if (elemName.equalsIgnoreCase("form")) {
				form = new HtmlForm();
				formList.add(form);
				AttributeSet as = elem.getAttributes();
				for (Enumeration k = as.getAttributeNames(); k.hasMoreElements();) {
					name = k.nextElement();
					attr = elem.getAttributes().getAttribute(name);
					if (name.toString().equalsIgnoreCase("action")) {
						form.setAction(attr.toString());
					}
					else if (name.toString().equalsIgnoreCase("method")) {
						form.setMethod(attr.toString());
					}
				}
			}

			else if (elemName.equalsIgnoreCase("input")) {
				AttributeSet as = elem.getAttributes();
				HtmlInput input = new HtmlInput();
				input.setSourceElement(elem);
				if (form != null)
					form.addInput(input);
				for (Enumeration k = as.getAttributeNames(); k.hasMoreElements();) {
					name = k.nextElement();
					attr = elem.getAttributes().getAttribute(name);
					String s = name.toString().toLowerCase().intern();
					if ("type" == s) {
						input.setType(attr.toString());
					}
					else if ("name" == s) {
						if (!"input".equals(attr.toString())) // "name" tags collide!!!
							input.setName(attr.toString());
					}
					else if ("value" == s) {
						input.setValue(attr.toString());
					}
					else if ("model" == s) {
						input.setModel(attr);
					}
					else if ("script" == s || "script_selected" == s) {
						input.setSelectedScript(attr.toString());
					}
					else if ("script_deselected" == s) {
						input.setDeselectedScript(attr.toString());
					}
					else if ("selfscript" == s || "selfscript_selected" == s) {
						input.setSelectedSelfScript(attr.toString());
					}
					else if ("selfscript_deselected" == s) {
						input.setDeselectedSelfScript(attr.toString());
					}
					else if ("question" == s) {
						input.setQuestion(attr.toString());
					}
					else if ("disable_at_run" == s) {
						input.setDisableAtRun("true".equals(attr.toString()));
					}
				}
				if (form == null) // when a single input tag is used without a form envelop
					setupModels(input, elem);
			}

			else if (elemName.equalsIgnoreCase("textarea")) {
				HtmlInput input = new HtmlInput();
				input.setSourceElement(elem);
				input.setType("text");
				if (form != null)
					form.addInput(input);
				AttributeSet as = elem.getAttributes();
				for (Enumeration k = as.getAttributeNames(); k.hasMoreElements();) {
					name = k.nextElement();
					attr = as.getAttribute(name);
					String s = name.toString().toLowerCase().intern();
					if (s == "model") {
						input.setModel(attr);
					}
					else if (s == "question") {
						input.setQuestion(attr.toString());
					}
					else if (s == "name") {
						if (!attr.toString().equals("textarea")) // "name" tags collide!!!
							input.setName(attr.toString());
					}
				}
				if (form == null) // when a single input tag is used without a form envelop
					setupModels(input, elem);
			}

			else if (elemName.equalsIgnoreCase("select")) {
				HtmlSelect select = new HtmlSelect();
				select.setSourceElement(elem);
				if (form != null)
					form.addSelect(select);
				AttributeSet as = elem.getAttributes();
				AbstractListModel model = null;
				final Map<String, Object> map = new TreeMap<String, Object>();
				final Map<String, Object> selfMap = new TreeMap<String, Object>();
				for (Enumeration k = as.getAttributeNames(); k.hasMoreElements();) {
					name = k.nextElement();
					attr = as.getAttribute(name);
					String s = name.toString().toLowerCase();
					if (s.equals("model")) {
						if (attr instanceof DefaultComboBoxModel) {
							model = (DefaultComboBoxModel) attr;
							select.setModel(model);
							int n = model.getSize();
							Object eSelected = ((DefaultComboBoxModel) model).getSelectedItem();
							for (int i2 = 0; i2 < n; i2++) {
								HtmlOption option = new HtmlOption();
								Object e = model.getElementAt(i2);
								if (e instanceof Option) {
									option.setValue(((Option) e).getValue());
									if (e == eSelected)
										option.setSelected(true);
									select.addOption(option);
								}
							}
						}
						else if (attr instanceof DefaultListModel) {
							model = (DefaultListModel) attr;
							select.setModel(model);
						}
					}
					else if (s.equals("name")) {
						if (!attr.toString().equals("select")) // "name" tags collide!!!
							select.setName(attr.toString());
					}
					else if (s.startsWith("script")) {
						map.put(s, attr);
					}
					else if (s.startsWith("selfscript")) {
						selfMap.put(s, attr);
					}
				}
				if (model != null && (!map.isEmpty() || !selfMap.isEmpty())) {
					final Element a = elem;
					model.addListDataListener(new ListDataListener() {
						public void contentsChanged(ListDataEvent e) {
							Object src = e.getSource();
							if (src instanceof DefaultComboBoxModel) {
								int p = ((DefaultComboBoxModel) src).getIndexOf(((DefaultComboBoxModel) src)
										.getSelectedItem());
								if (!map.isEmpty()) {
									Object[] o = map.values().toArray();
									if (p >= 0 && p < o.length) {
										if (o[p] != null)
											fireLinkUpdate(new HyperlinkEvent(HTMLPane.this,
													HyperlinkEvent.EventType.ACTIVATED, null, o[p].toString(), a));

									}
								}
								if (!selfMap.isEmpty()) {
									Object[] o = selfMap.values().toArray();
									if (p >= 0 && p < o.length) {
										if (o[p] != null)
											notifySelfScriptListeners(new SelfScriptEvent(a, o[p].toString()));
									}
								}
							}
						}

						public void intervalAdded(ListDataEvent e) {
						}

						public void intervalRemoved(ListDataEvent e) {
						}
					});
				}
			}

			elem = j.next();

		}

		if (!formList.isEmpty()) {
			for (HtmlForm hf : formList) {
				if (hf.inputList != null && !hf.inputList.isEmpty()) {
					for (HtmlInput hi : hf.inputList) {
						if ("submit".equalsIgnoreCase(hi.getType())) {
							Object m = hi.getModel();
							if (m instanceof ButtonModel) {
								ButtonModel bm = (ButtonModel) m;
								final Element sourceElement = hi.getSourceElement();
								final HtmlForm hf2 = hf;
								final String t = hi.getName();
								bm.addActionListener(new ActionListener() {
									public void actionPerformed(ActionEvent e) {
										String queryString = hf2.getQueryString();
										String action = hf2.getAction();
										String s;
										if (action.indexOf("?") == -1) {
											s = action + "?client=mw&action=" + t + queryString
													+ "&formmethodtemporary=" + hf2.getMethod();
										}
										else {
											s = action + "&client=mw" + queryString + "&formmethodtemporary="
													+ hf2.getMethod();
										}
										fireLinkUpdate(new HyperlinkEvent(HTMLPane.this,
												HyperlinkEvent.EventType.ACTIVATED, null, s, sourceElement));
									}
								});
							}
						}
						else { // script support when a form envelop is present
							if (hi.getSelectedScript() != null || hi.getDeselectedScript() != null) {
								setupModels(hi, hi.getSourceElement());
							}
						}
					}
				}
			}
		}

	}

	private void setupModels(final HtmlInput input, final Element source) {
		if (input.getModel() instanceof Document) {
			if (input.getQuestion() != null)
				((Document) input.getModel()).putProperty("question", input.getQuestion());
		}
		else if (input.getModel() instanceof ButtonModel) {
			ButtonModel bm = (ButtonModel) input.getModel();
			if (bm instanceof JToggleButton.ToggleButtonModel) {
				bm.addItemListener(new ItemListener() {
					public void itemStateChanged(ItemEvent e) {
						String script = e.getStateChange() == ItemEvent.SELECTED ? input.getSelectedScript() : input
								.getDeselectedScript();
						if (script != null)
							fireLinkUpdate(new HyperlinkEvent(HTMLPane.this, HyperlinkEvent.EventType.ACTIVATED, null,
									script, source));
					}
				});
			}
			else if (bm != null) {
				if (bm instanceof DefaultButtonModel) {
					for (ActionListener l : ((DefaultButtonModel) bm).getActionListeners())
						bm.removeActionListener(l);
				}
				bm.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						String script = input.getSelectedScript();
						if (script != null)
							fireLinkUpdate(new HyperlinkEvent(HTMLPane.this, HyperlinkEvent.EventType.ACTIVATED, null,
									script, source));
						script = input.getSelectedSelfScript();
						if (script != null)
							notifySelfScriptListeners(new SelfScriptEvent(source, script));
					}
				});
			}
		}

	}

	public List<HtmlForm> getForms() {
		return formList;
	}

	public List<JComponent> getEmbeddedComponents() {
		if (getContentType().equals("text/plain"))
			return null;
		if (componentList == null)
			componentList = new ArrayList<JComponent>();
		else componentList.clear();
		Component[] c = getComponents();
		if (c == null || c.length == 0)
			return componentList;
		Container container = null;
		JComponent comp = null;
		for (Component x : c) {
			if (x instanceof Container) {
				container = (Container) x;
				comp = (JComponent) container.getComponent(0);
				if (comp instanceof JScrollPane) {
					comp = (JComponent) ((JScrollPane) comp).getViewport().getView();
				}
				componentList.add(comp);
			}
		}
		return componentList;
	}

	public List<JComponent> getEmbeddedComponents(Class type) {
		if (getContentType().equals("text/plain"))
			return null;
		if (type == null)
			return null;
		List<JComponent> list = new ArrayList<JComponent>();
		Component[] c = getComponents();
		if (c == null || c.length == 0)
			return list;
		Container container = null;
		JComponent comp = null;
		for (Component x : c) {
			if (x instanceof Container) {
				container = (Container) x;
				comp = (JComponent) container.getComponent(0);
				if (comp instanceof JScrollPane) {
					comp = (JComponent) ((JScrollPane) comp).getViewport().getView();
				}
				if (type.isInstance(comp))
					list.add(comp);
			}
		}
		return list;
	}

	public void setBackgroundImage(Icon bgImage) {
		backgroundImage = bgImage;
		if (backgroundImage != null) {
			iconWidth = backgroundImage.getIconWidth();
			iconHeight = backgroundImage.getIconHeight();
		}
	}

	public void paintComponent(Graphics g) {
		if (backgroundImage != null) {
			int imax = getWidth() / iconWidth + 1;
			int jmax = getHeight() / iconHeight + 1;
			for (int i = 0; i < imax; i++) {
				for (int j = 0; j < jmax; j++) {
					backgroundImage.paintIcon(this, g, i * iconWidth, j * iconHeight);
				}
			}
		}
		super.paintComponent(g);
	}

	/**
	 * workaround to set the background image of a HTML text box because we cannot directly set it through changing the
	 * HTML document. This is necessary for the background image of a text box to be displayed while offline. While
	 * online, this results in double rendering of the background image: one through the HTML text structure, the other
	 * through calling the setBackgroundImage method of the TextBox class.
	 */
	public void useCachedBackgroundImage(String codeBase) {
		if (!(getDocument() instanceof HTMLDocument))
			return;
		AttributeSet as = null;
		Element el = null;
		Enumeration en = null;
		String s = null;
		Object obj = null;
		HTMLDocument doc = (HTMLDocument) getDocument();
		ElementIterator it = new ElementIterator(doc);
		synchronized (doc) {
			while (it.next() != null) {
				el = it.current();
				if (el.getName().equalsIgnoreCase(HTML.Tag.BODY.toString())) {
					as = el.getAttributes();
					en = as.getAttributeNames();
					while (en.hasMoreElements()) {
						obj = en.nextElement();
						if (obj.toString().equalsIgnoreCase(HTML.Attribute.BACKGROUND.toString())) {
							s = as.getAttribute(obj).toString();
							break;
						}
					}
					if (s != null) {
						s = ConnectionManager.sharedInstance().getLocalCopy(codeBase + FileUtilities.getFileName(s))
								.toString();
						setBackgroundImage(new ImageIcon(s));
					}
				}
			}
		}
	}

	/**
	 * FIXME: The problem is that the RunElements in the HTMLDocument is not mutable. Because of this, we have to insert
	 * new elements that call cached images and remove the corresponding old elements that call remote images. Can we
	 * find a better way to do this?
	 */
	public void useCachedImages(boolean b, String codeBase) {

		if (!(getDocument() instanceof HTMLDocument))
			return;

		AttributeSet as = null;
		Element el = null;
		Enumeration en = null;
		String s = null;
		String usemap = null;
		String href = null;
		String border = null;
		String imgWidth = null;
		String imgHeight = null;
		Object obj = null;
		boolean baseSet = false;
		String s2 = null;

		HTMLDocument doc = (HTMLDocument) getDocument();
		URL originalBase = doc.getBase();
		ElementIterator it = new ElementIterator(doc);

		synchronized (doc) {
			while (it.next() != null) {
				el = it.current();
				if (el.getName().equalsIgnoreCase(HTML.Tag.IMG.toString())) {
					as = el.getAttributes();
					en = as.getAttributeNames();
					while (en.hasMoreElements()) {
						obj = en.nextElement();
						s2 = obj.toString();
						if (s2.equalsIgnoreCase(HTML.Attribute.SRC.toString())) {
							s = as.getAttribute(obj).toString();
						}
						else if (s2.equalsIgnoreCase(HTML.Attribute.USEMAP.toString())) {
							usemap = as.getAttribute(obj).toString();
						}
						else if (s2.equalsIgnoreCase(HTML.Tag.A.toString())) {
							href = as.getAttribute(obj).toString();
						}
						else if (s2.equalsIgnoreCase(HTML.Attribute.BORDER.toString())) {
							border = as.getAttribute(obj).toString();
						}
						else if (s2.equalsIgnoreCase(HTML.Attribute.WIDTH.toString())) {
							imgWidth = as.getAttribute(obj).toString();
						}
						else if (s2.equalsIgnoreCase(HTML.Attribute.HEIGHT.toString())) {
							imgHeight = as.getAttribute(obj).toString();
						}
					}
					if (b) {
						s = ConnectionManager.sharedInstance().getLocalCopy(FileUtilities.concatenate(codeBase, s))
								.toString();
						if (!baseSet) {
							try {
								doc.setBase(new File(FileUtilities.getCodeBase(s)).toURI().toURL());
							}
							catch (Exception e) {
								e.printStackTrace();
							}
							baseSet = true;
						}
						s = doc.getBase() + FileUtilities.getFileName(s);
					}
					else {
						s = FileUtilities.getFileName(s);
					}
					if (href != null) {
						s2 = "<a " + href.trim() + ">";
					}
					else {
						s2 = "";
					}
					if (usemap == null) {
						s2 += "<img src=\"" + s + "\"";
					}
					else {
						s2 += "<img usemap=\"" + usemap + "\" src=\"" + s + "\"";
					}
					if (border != null) {
						s2 += " border=\"" + border + "\"";
					}
					if (imgWidth != null) {
						s2 += " width=\"" + imgWidth + "\"";
					}
					if (imgHeight != null) {
						s2 += " height=\"" + imgHeight + "\"";
					}
					s2 += "\">";
					if (href != null) {
						s2 += "</a>";
					}
					try {
						doc.insertAfterEnd(el, s2);
					}
					catch (BadLocationException e) {
						e.printStackTrace();
						continue;
					}
					catch (IOException e) {
						e.printStackTrace();
						continue;
					}
					s = href = usemap = border = null;
					it.next(); // this is called to skip the original image element!!!
				}
			}
			doc.setBase(originalBase);
		}

		removeAllImages();

	}

	private void removeAllImages() {

		if (!(getDocument() instanceof HTMLDocument))
			return;
		HTMLDocument doc = (HTMLDocument) getDocument();

		ElementIterator it = new ElementIterator(doc);
		HTMLDocument.RunElement re = null;
		int startOffset, endOffset;
		Element el = null;
		AttributeSet as = null;
		List<Integer> startOffsetList = new ArrayList<Integer>();
		List<Integer> endOffsetList = new ArrayList<Integer>();

		synchronized (doc) {
			while (it.next() != null) {
				el = it.current();
				if (el.getName().equalsIgnoreCase(HTML.Tag.IMG.toString())) {
					as = el.getAttributes();
					if (as instanceof HTMLDocument.RunElement) {
						re = (HTMLDocument.RunElement) as;
						try {
							startOffset = re.getStartOffset();
							endOffset = re.getEndOffset();
							startOffsetList.add(startOffset);
							endOffsetList.add(endOffset);
						}
						catch (Exception e) {
							e.printStackTrace();
							continue;
						}
					}
					it.next();
				}
			}
		}

		int n = startOffsetList.size();
		for (int i = n - 1; i >= 0; i--) {
			startOffset = startOffsetList.get(i);
			endOffset = endOffsetList.get(i);
			try {
				doc.remove(startOffset, endOffset - startOffset);
			}
			catch (BadLocationException e) {
				e.printStackTrace();
			}
		}

	}

	public void cacheImages(String codeBase) {

		if (!ConnectionManager.sharedInstance().isCachingAllowed())
			return;
		if (!FileUtilities.isRemote(codeBase))
			return;

		String bgImage = getBackgroundImage();
		if (bgImage != null) {
			String pb = codeBase + FileUtilities.getFileName(bgImage);
			try {
				File file = ConnectionManager.sharedInstance().shouldUpdate(pb);
				if (file == null)
					file = ConnectionManager.sharedInstance().cache(pb);
			}
			catch (IOException e) {
				e.printStackTrace();
			}
			useCachedBackgroundImage(codeBase);
		}

		List<String> list = getImageNames();
		if (list != null && !list.isEmpty()) {
			File file = null;
			for (String pb : list) {
				pb = FileUtilities.concatenate(codeBase, pb);
				try {
					file = ConnectionManager.sharedInstance().shouldUpdate(pb);
					if (file == null)
						file = ConnectionManager.sharedInstance().cache(pb);
				}
				catch (IOException e) {
					e.printStackTrace();
				}
			}
			useCachedImages(true, codeBase);
		}

	}

}