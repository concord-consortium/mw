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

import java.util.Enumeration;
import java.awt.EventQueue;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AbstractDocument;
import javax.swing.text.Element;

import org.concord.modeler.BasicModel;
import org.concord.modeler.Embeddable;
import org.concord.modeler.Engine;
import org.concord.modeler.InstancePool;
import org.concord.modeler.ModelCanvas;
import org.concord.modeler.ModelCommunicator;
import org.concord.modeler.PageApplet;

/**
 * Respond to edit action involving model(s). The document tree structure is searched to determine whether or not the
 * last edit involves model(s). If one or more models are removed, they should be properly shut down, and the model
 * manager of the page should be notified.
 * 
 * @author Charles Xie
 */

class EditResponder implements DocumentListener {

	private AbstractDocument.BranchElement section, paragraph;
	private AbstractDocument.LeafElement content;
	private Object name, attr;
	private Enumeration enum2, enum3;
	private DocumentEvent.ElementChange change;
	private Element[] elementFlow;
	private Page page;

	EditResponder(Page page) {
		if (page == null)
			throw new IllegalArgumentException("Page arg must not be null");
		setPage(page);
	}

	void setPage(Page page) {
		this.page = page;
	}

	public void changedUpdate(DocumentEvent e) {
	}

	public void insertUpdate(DocumentEvent e) {
		section = (AbstractDocument.BranchElement) page.getDocument().getDefaultRootElement();
		if (section == null)
			return;
		enum2 = section.children();
		synchronized (enum2) {
			while (enum2.hasMoreElements()) {
				paragraph = (AbstractDocument.BranchElement) enum2.nextElement();
				change = e.getChange(paragraph);
				if (change != null) {
					elementFlow = change.getChildrenAdded();
					if (elementFlow != null) {
						EventQueue.invokeLater(new Runnable() {
							public void run() {
								updateComponents(true);
							}
						});
					}
				}
			}
		}
		page.getSaveReminder().setChanged(true);
	}

	public void removeUpdate(DocumentEvent e) {
		section = (AbstractDocument.BranchElement) page.getDocument().getDefaultRootElement();
		if (section == null)
			return;
		enum2 = section.children();
		synchronized (enum2) {
			while (enum2.hasMoreElements()) {
				paragraph = (AbstractDocument.BranchElement) enum2.nextElement();
				change = e.getChange(paragraph);
				if (change != null) {
					elementFlow = change.getChildrenRemoved();
					if (elementFlow != null) {
						EventQueue.invokeLater(new Runnable() {
							public void run() {
								updateComponents(false);
							}
						});
					}
				}
			}
		}
		page.getSaveReminder().setChanged(true);
	}

	/**
	 * when a component is removed from the page, its reference should be nullified in order to prevent memory leak.
	 */
	private void updateComponents(boolean inserted) {
		Map<Class, Object> map = new HashMap<Class, Object>();
		for (Element i : elementFlow) {
			content = (AbstractDocument.LeafElement) i;
			enum3 = content.getAttributeNames();
			synchronized (enum3) {
				while (enum3.hasMoreElements()) {
					name = enum3.nextElement();
					attr = content.getAttribute(name);
					if (name.toString().equals("component")) {
						if (attr instanceof Engine) {
							engineRespond((Engine) attr, inserted);
							map.put(attr.getClass(), attr);
						}
						else if (attr instanceof ModelCommunicator) {
							communicatorRespond((ModelCommunicator) attr, inserted);
						}
						else if (attr instanceof PageApplet) {
							appletRespond((PageApplet) attr, inserted);
						}
					}
				}
			}
		}
		if (!map.isEmpty() && page != null) {
			for (Class c : map.keySet()) {
				indexifyEngines(c);
			}
		}
	}

	private void indexifyEngines(Class c) {
		Map m = page.getEmbeddedComponent(c);
		if (m != null && !m.isEmpty()) {
			int i = 0;
			Embeddable e;
			for (Object o : m.values()) {
				e = (Embeddable) o;
				if (e instanceof Engine) {
					e.setIndex(i++);
				}
			}
		}
	}

	private static void appletRespond(PageApplet applet, boolean inserted) {
		if (!inserted) {
			// FIXME: copy and paste causes removeUpdate to be called.
			applet.destroy();
		}
	}

	private void engineRespond(Engine engine, boolean inserted) {

		// make sure that the engine is stopped: danger: this could cause problems if stopImmediately()
		// changes the current state of the engine.
		if (!engine.isShowing())
			engine.stopImmediately();

		if (!inserted) {
			if (engine instanceof ModelCanvas) {
				BasicModel model = ((ModelCanvas) engine).getMdContainer().getModel();
				((ModelCanvas) engine).setUsed(false);
				page.getComponentPool().processInsertionOrRemoval(false,
						((ModelCanvas) engine).getMdContainer().getRepresentationName());
				page.nameModels();
				List list = model.getModelListeners();
				if (list != null)
					list.clear();
			}
			else {
				InstancePool.sharedInstance().setStatus(engine, false);
			}
		}
		else {
			if (engine instanceof ModelCanvas) {
				((ModelCanvas) engine).setUsed(true);
			}
			else {
				InstancePool.sharedInstance().setStatus(engine, true);
			}
		}

	}

	private void communicatorRespond(ModelCommunicator comm, boolean inserted) {

		if (page == null)
			return;

		if (!inserted) {

			if (comm.getModelClass() == null || comm.getModelClass().indexOf("org.concord.mw2d") != -1) {
				ModelCanvas mc = page.getComponentPool().get(comm.getModelID());
				if (mc != null) {
					BasicModel m = mc.getMdContainer().getModel();
					if (m != null) {
						m.removeModelListener(comm);
					}
				}
			}
			else {
				Class clazz = null;
				try {
					clazz = Class.forName(comm.getModelClass());
				}
				catch (Exception e) {
					clazz = null;
				}
				if (clazz != null) {
					Object o = page.getEmbeddedComponent(clazz, comm.getModelID());
					if (o instanceof BasicModel)
						((BasicModel) o).removeModelListener(comm);
				}
			}

		}

	}

}
