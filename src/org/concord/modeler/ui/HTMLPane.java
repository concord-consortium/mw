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

import javax.swing.AbstractButton;
import javax.swing.AbstractListModel;
import javax.swing.ButtonModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.event.HyperlinkEvent;
import javax.swing.text.AttributeSet;
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
			requestFocusInWindow();
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
						imagePopupMenu.putClientProperty("image", image);
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

	public void destroy() {
		if (selfScriptListeners != null)
			selfScriptListeners.clear();
		if (componentList != null)
			componentList.clear();
		if (formList != null)
			formList.clear();
	}

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
							// Enumeration e = a.getAttributeNames();
							// while(e.hasMoreElements()){
							// Object o =e.nextElement();
							// System.out.println(o+"="+a.getAttribute(o));
							// }
							if (a != null) {
								AttributeSet b = (AttributeSet) a.getAttribute(HTML.Tag.A);
								if (b != null) {
									String s = (String) b.getAttribute(HTML.Attribute.HREF);
									if (s == null)
										return s;
									return FileUtilities.httpDecode(s);
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
					else if ("alt" == s) {
						input.setAlt(attr.toString());
					}
					else if ("enabled" == s) {
						input.setEnabled("true".equalsIgnoreCase(attr.toString()));
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
					else if (s.equals("enabled")) {
						select.setEnabled("true".equalsIgnoreCase(attr.toString()));
					}
					else if (s.startsWith("script")) {
						map.put(s, attr);
					}
					else if (s.startsWith("selfscript")) {
						selfMap.put(s, attr);
					}
				}
				if (model != null && (!map.isEmpty() || !selfMap.isEmpty())) {
					final JComboBox cb = getComboBox(model);
					if (cb != null) {
						cb.setEnabled(select.getEnabled());
						final Element a = elem;
						cb.addItemListener(new ItemListener() {
							public void itemStateChanged(ItemEvent e) {
								if (e.getStateChange() == ItemEvent.SELECTED) {
									int p = cb.getSelectedIndex();
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
						});
					}
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
			final AbstractButton ab = getAbstractButton((ButtonModel) input.getModel());
			if (ab == null)
				return;
			ab.setToolTipText(input.getAlt());
			ab.setEnabled(input.getEnabled());
			if (ab instanceof JToggleButton) {
				for (ItemListener l : ab.getItemListeners())
					ab.removeItemListener(l);
				ab.addItemListener(new ItemListener() {
					public void itemStateChanged(ItemEvent e) {
						boolean selected = e.getStateChange() == ItemEvent.SELECTED;
						String script = selected ? input.getSelectedScript() : input.getDeselectedScript();
						if (script != null)
							fireLinkUpdate(new HyperlinkEvent(HTMLPane.this, HyperlinkEvent.EventType.ACTIVATED, null,
									script, source));
						script = selected ? input.getSelectedSelfScript() : input.getDeselectedSelfScript();
						if (script != null)
							notifySelfScriptListeners(new SelfScriptEvent(source, script));
					}
				});
			}
			else {
				for (ActionListener l : ab.getActionListeners())
					ab.removeActionListener(l);
				ab.addActionListener(new ActionListener() {
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

	private AbstractButton getAbstractButton(ButtonModel bm) {
		if (getContentType().equals("text/plain"))
			return null;
		Component[] c = getComponents();
		if (c == null || c.length == 0)
			return null;
		Container container = null;
		JComponent comp = null;
		for (Component x : c) {
			if (x instanceof Container) {
				container = (Container) x;
				comp = (JComponent) container.getComponent(0);
				if (comp instanceof AbstractButton) {
					if (bm == ((AbstractButton) comp).getModel())
						return (AbstractButton) comp;
				}
			}
		}
		return null;
	}

	private JComboBox getComboBox(AbstractListModel lm) {
		if (getContentType().equals("text/plain"))
			return null;
		Component[] c = getComponents();
		if (c == null || c.length == 0)
			return null;
		Container container = null;
		JComponent comp = null;
		for (Component x : c) {
			if (x instanceof Container) {
				container = (Container) x;
				comp = (JComponent) container.getComponent(0);
				if (comp instanceof JComboBox) {
					if (lm == ((JComboBox) comp).getModel())
						return (JComboBox) comp;
				}
			}
		}
		return null;
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

	void cacheLinkedFiles(String codeBase) {

		if (!ConnectionManager.sharedInstance().isCachingAllowed())
			return;
		if (!FileUtilities.isRemote(codeBase))
			return;

		String bgImage = getAttribute("body", "background");
		if (bgImage != null) {
			String pb = codeBase + FileUtilities.getFileName(bgImage);
			try {
				File file = ConnectionManager.sharedInstance().shouldUpdate(pb);
				if (file == null)
					file = ConnectionManager.sharedInstance().cache(pb);
				if (file != null)
					setBackgroundImage(new ImageIcon(file.toURI().toURL()));
			}
			catch (IOException e) {
				e.printStackTrace();
			}
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
		}

		String href = getAttribute("link", "href");
		if (href != null) {
			String pb = codeBase + FileUtilities.getFileName(href);
			try {
				File file = ConnectionManager.sharedInstance().shouldUpdate(pb);
				if (file == null)
					file = ConnectionManager.sharedInstance().cache(pb);
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

}