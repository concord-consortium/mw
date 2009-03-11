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

package org.concord.jmol;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelListener;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.ImageIcon;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

import org.concord.modeler.ConnectionManager;
import org.concord.modeler.ModelerUtilities;
import org.concord.modeler.PageScriptConsole;
import org.concord.modeler.draw.Draw;
import org.concord.modeler.draw.FillMode;
import org.concord.modeler.draw.GradientFactory;
import org.concord.modeler.util.FileUtilities;
import org.jmol.api.Attachment;
import org.jmol.api.SiteAnnotation;
import org.myjmol.adapter.smarter.SmarterJmolAdapter;
import org.myjmol.api.Cockpit;
import org.myjmol.api.JmolStatusListener;
import org.myjmol.api.JmolViewer;
import org.myjmol.api.Navigator;
import org.myjmol.api.Scene;

class Jmol extends Draw {

	private final static Font FONT_BOLD_18 = new Font(null, Font.BOLD, 18);
	private final static Font FONT_BOLD_15 = new Font(null, Font.BOLD, 15);

	JmolViewer viewer;
	Navigator navigator;
	private JmolContainer container;
	private SteeringForceMeter steeringForceMeter;

	private Cockpit cockpit;
	private FillMode fillMode = FillMode.getNoFillMode();
	private ImageIcon backgroundImage;
	private int iconWidth, iconHeight;
	private final Object lock = new Object();
	private Rectangle clipRect = new Rectangle();
	private Dimension currentSize = new Dimension();
	private volatile boolean paintLoadingMessage;
	private Component scriptConsole;
	private Scene startingScene;
	private MouseWheelListener jmolMouseWheelListener;
	private String resourceAddress;
	private int cameraAtom = -1;
	private boolean waitForInitializationScript;

	private List<LoadMoleculeListener> loadMoleculeListeners;
	private List<CommandListener> commandListenerList;
	private List<ImageComponent> imageList;

	private JmolStatusListener jmolListener = new JmolStatusListener() {

		public void notifyFileLoaded(String fullPathName, String fileName, String modelName, Object clientFile,
				String errorMessage) {
			if (fullPathName == null && fileName == null && modelName == null && clientFile == null)
				return;
			notifyLoadMoleculeListeners();
		}

		public void notifyScriptTermination(String statusMessage, int msWalltime) {
			setScriptRunning(false);
			if (scriptConsole instanceof PageScriptConsole) {
				((PageScriptConsole) scriptConsole).notifyScriptTermination(statusMessage, msWalltime);
			}
			if (waitForInitializationScript) {
				container.initializationScriptCompleted();
				waitForInitializationScript = false;
			}
		}

		public void handlePopupMenu(int x, int y) {
		}

		public void notifyFrameChanged(int frameNo) {
		}

		public void notifyAtomPicked(int atomIndex, String strInfo) {
		}

		public void showUrl(String url) {
		}

		public void showConsole(boolean showConsole) {
		}

		public void createImage(String file, String type, int quality) {
		}

		public String eval(String strEval) {
			return null;
		}

		public float functionXY(String functionName, int x, int y) {
			return 0;
		}

		public void notifyAtomHovered(int atomIndex, String strInfo) {
		}

		public void notifyNewDefaultModeMeasurement(int count, String strInfo) {
		}

		public void notifyNewPickingModeMeasurement(int iatom, String strMeasure) {
		}

		public void notifyScriptStart(String statusMessage, String additionalInfo) {
		}

		public void sendConsoleEcho(String strEcho) {
			if (scriptConsole instanceof PageScriptConsole) {
				((PageScriptConsole) scriptConsole).scriptEcho(strEcho);
			}
		}

		public void sendConsoleMessage(String strStatus) {
			if (strStatus != null) {
				if (strStatus.startsWith("command expected")) {
					notifyCommandListeners(new CommandEvent(this, CommandEvent.COMPILATION_ERROR, strStatus));
				}
			}
			if (scriptConsole instanceof PageScriptConsole) {
				((PageScriptConsole) scriptConsole).scriptStatus(strStatus);
			}
		}

		public void sendSyncScript(String script, String appletName) {
		}

		public void setCallbackFunction(String callbackType, String callbackFunction) {
		}

	};

	private ComponentAdapter resizeListener = new ComponentAdapter() {
		public void componentResized(ComponentEvent e) {
			updateSize();
			repaint();
		}
	};

	Jmol(JmolContainer c) {

		super();

		container = c;
		setPopupMenuEnabled(false);

		viewer = JmolViewer.allocateViewer(this, new SmarterJmolAdapter(null));
		viewer.setBackgroundArgb(Color.black.getRGB());
		viewer.setPerspectiveDepth(true);
		viewer.setCameraSpin(true);
		viewer.setFrankOn(true);
		viewer.setAxisStyle((byte) 0);
		viewer.setDisablePopupMenu(true);
		viewer.setPercentVdwAtom(20);
		viewer.setJmolStatusListener(jmolListener);
		try {
			jmolMouseWheelListener = getMouseWheelListeners()[0];
		}
		catch (Exception e) {
			// ignore
		}
		if (jmolMouseWheelListener != null)
			removeMouseWheelListener(jmolMouseWheelListener);

		navigator = new Navigator(viewer) {
			public void home() {
				Jmol.this.home();
			}
		};
		navigator.setLocation(4, 4);
		navigator.setBackground(Color.black);
		navigator.setEngineOnCallback(new Runnable() {
			public void run() {
				viewer.setEngineOn(true);
			}
		});
		navigator.setEngineOffCallback(new Runnable() {
			public void run() {
				viewer.setEngineOn(false);
			}
		});
		navigator.setSteeringUpCallback(new Runnable() {
			public void run() {
				container.motionGenerator.changeSteeringStrength(0.5f);
			}
		});
		navigator.setSteeringOffCallback(new Runnable() {
			public void run() {
				container.motionGenerator.setSteeringStrength(0);
			}
		});

		cockpit = new Cockpit(viewer);
		cockpit.setBackground(Color.black);

		addFocusListener(new FocusListener() {
			public void focusGained(FocusEvent e) {
				if (jmolMouseWheelListener != null)
					addMouseWheelListener(jmolMouseWheelListener);
				if (container.isRoverMode())
					container.setRoverGo(true);
			}

			public void focusLost(FocusEvent e) {
				if (jmolMouseWheelListener != null)
					removeMouseWheelListener(jmolMouseWheelListener);
				if (container.isRoverMode())
					container.setRoverGo(false);
			}
		});

	}

	void setResizeListener(boolean b) {
		if (b) {
			ComponentListener[] cl = getComponentListeners();
			for (ComponentListener x : cl)
				if (x == resizeListener)
					return;
			addComponentListener(resizeListener);
		}
		else removeComponentListener(resizeListener);
	}

	void waitForInitializationScript() {
		waitForInitializationScript = true;
	}

	protected void processMouseMoved(MouseEvent e) {
		super.processMouseMoved(e);
		int x = e.getX();
		int y = e.getY();
		if (viewer.getNavigationMode())
			navigator.mouseHover(x, y);
		viewer.setActiveKey(Attachment.ATOM_HOST, viewer.getAttachmentHost(Attachment.ATOM_HOST, x, y,
				SiteAnnotation.class), SiteAnnotation.class);
		viewer.setActiveKey(Attachment.BOND_HOST, viewer.getAttachmentHost(Attachment.BOND_HOST, x, y,
				SiteAnnotation.class), SiteAnnotation.class);
		repaint();
	}

	protected void processMouseReleased(MouseEvent e) {
		super.processMouseReleased(e);
		navigator.clear();
		navigator.engineOff();
	}

	protected void processMousePressed(MouseEvent e) {
		super.processMousePressed(e);
		int x = e.getX();
		int y = e.getY();
		if (viewer.getNavigationMode()) {
			navigator.navigate(x, y);
		}
	}

	void setCameraAtom(int i) {
		cameraAtom = i;
		if (cameraAtom >= 0) {
			if (viewer.getNavigationMode()) {
				if (!viewer.isCameraSpin())
					viewer.setCameraSpin(true);
				float[] x = new float[7]; // x[0], x[1], x[2] - rotation axis; x[3] - rotation degrees
				x[0] = 0;
				x[1] = 1;
				x[2] = 0;
				x[3] = 0.1f;
				String s = viewer.getCurrentOrientation();
				if (s != null) {
					String[] t = s.split("\\s");
					for (int k = 0; k < Math.min(t.length, 4); k++) {
						x[k] = Float.parseFloat(t[k]);
					}
				}
				Point3f p = viewer.getAtomPoint3f(cameraAtom);
				viewer.moveCameraTo(0, x[0], x[1], x[2], x[3], p.x, p.y, p.z);
			}
		}
	}

	int getCameraAtom() {
		return cameraAtom;
	}

	void setResourceAddress(String s) {
		resourceAddress = s;
	}

	void zoom(boolean in) {
		if (viewer.getNavigationMode()) {
			viewer.translateByScreenPixels(0, 0, in ? navigator.getSpeed() : -navigator.getSpeed());
		}
		else {
			viewer.evalString(in ? "move 0 0 0 40 0 0 0 0 1" : "move 0 0 0 -40 0 0 0 0 1");
		}
	}

	void home() {
		if (viewer.getNavigationMode()) {
			if (container.getSceneCount() == 0) {
				if (startingScene != null) {
					if (!container.isRoverMode()) {
						Thread t = new Thread("Fly to home position") {
							public void run() {
								viewer.moveCameraToScene(startingScene, false);
							}
						};
						t.setPriority(Thread.MIN_PRIORITY);
						t.start();
					}
					else {
						viewer.moveCameraToScene(startingScene, true);
						container.moveRoverTo(viewer.getCameraPosition());
						container.motionGenerator.resetSpeed();
					}
				}
			}
			else {
				container.moveToScene(0, false);
			}
		}
		else {
			viewer.evalString(startingScene != null ? "moveto " + 1 + " " + startingScene.rotationToString()
					: "moveto 1 0 0 0 0");
		}
	}

	public void clear() {
		super.clear();
		if (navigator != null)
			navigator.clear();
		navigator.setSteering(false);
		viewer.stopMotion(true);
		viewer.removeAll();
		viewer.setFrankOn(true);
		removeAllImages();
		startingScene = null;
		if (viewer.getNavigationMode())
			viewer.setCameraPosition(0, 0, 0);
		cameraAtom = -1;
		viewer.setDepthCueing(false);
		viewer.setInteractionCentersVisible(true);
		waitForInitializationScript = false;
		repaint();
	}

	void setScriptRunning(final boolean b) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				setCursor(Cursor.getPredefinedCursor(b ? Cursor.WAIT_CURSOR : Cursor.DEFAULT_CURSOR));
			}
		});
	}

	// For backward compatibility. Use setStartingScene instead.
	void setRotationAndZoom(Vector3f rotationAxis, float degrees, float zoomPercent) {
		startingScene = new Scene(viewer.getCameraPosition(), rotationAxis, degrees, zoomPercent);
		startingScene.setTransitionTime((short) 1);
	}

	void setStartingScene(SceneState ss) {
		String geoData = ss.getGeoData();
		Point3f p = new Point3f();
		Vector3f v = new Vector3f();
		float[] x = new float[] { 0, 0, 0, 0, 0, 0, 0, 100 };
		String[] t = geoData.split("\\s");
		for (int j = 0; j < Math.min(t.length, x.length); j++)
			x[j] = Float.parseFloat(t[j]);
		p.set(x[0], x[1], x[2]);
		v.set(x[3], x[4], x[5]);
		if (startingScene == null) {
			startingScene = new Scene(p, v, x[6], x[7]);
		}
		else {
			startingScene.getCameraPosition().set(p);
			startingScene.getRotationAxis().set(v);
			startingScene.setRotationAngle(x[6]);
			startingScene.setZoomPercent(x[7]);
		}
		startingScene.setProperty("selection", new Byte(ss.getAtomSelection()));
		startingScene.setProperty("atomcoloring", new Byte(ss.getAtomColoring()));
		startingScene.setProperty("scheme", ss.getScheme());
		startingScene.setXTrans(ss.getXTrans());
		startingScene.setYTrans(ss.getYTrans());
	}

	Scene getStartingScene() {
		return startingScene;
	}

	void addImage(ImageComponent image) {
		if (image == null)
			return;
		if (imageList == null)
			imageList = new ArrayList<ImageComponent>();
		imageList.add(image);
		repaint();
	}

	void removeImage(ImageComponent image) {
		if (image == null)
			return;
		if (imageList == null)
			return;
		imageList.remove(image);
		repaint();
	}

	void removeAllImages() {
		if (imageList != null)
			imageList.clear();
		repaint();
	}

	int getNumberOfImages() {
		if (imageList == null)
			return 0;
		return imageList.size();
	}

	ImageComponent getImage(int i) {
		if (i < 0 || i >= getNumberOfImages())
			return null;
		return imageList.get(i);
	}

	Iterator getImageIterator() {
		if (imageList == null)
			return null;
		return imageList.iterator();
	}

	public void addCommandListener(CommandListener cl) {
		if (cl == null)
			return;
		if (commandListenerList == null)
			commandListenerList = new ArrayList<CommandListener>();
		if (commandListenerList.contains(cl))
			return;
		commandListenerList.add(cl);
	}

	public void removeCommandListener(CommandListener cl) {
		if (cl == null)
			return;
		if (commandListenerList == null)
			return;
		commandListenerList.remove(cl);
	}

	void notifyCommandListeners(CommandEvent e) {
		if (commandListenerList == null)
			return;
		for (CommandListener l : commandListenerList) {
			switch (e.getType()) {
			case CommandEvent.COMPILATION_ERROR:
				l.compilerErrorReported(e);
				break;
			}
		}
	}

	void setLoadingMessagePainted(boolean b) {
		synchronized (lock) {
			paintLoadingMessage = b;
		}
		repaint();
	}

	boolean isLoadingMessagePainted() {
		synchronized (lock) {
			return paintLoadingMessage;
		}
	}

	void haltScriptExecution() {
		viewer.haltScriptExecution();
	}

	Color getAtomColor(int index) {
		int n = viewer.getAtomCount();
		if (n <= 0)
			return Color.white;
		if (index >= 0 && index < n)
			return new Color(viewer.getAtomArgb(index));
		return Color.white;
	}

	/*
	 * return the color of the the bond with the specified index, if the bond color is not inherited. If it is
	 * inherited, the bond has two colors that are determinted by the color of the two bonded atoms. In this case, this
	 * method always returns the color of the first atom.
	 */
	Color getBondColor(int index) {
		int n = viewer.getBondCount();
		if (n <= 0)
			return Color.white;
		if (index >= 0 && index < n)
			return new Color(viewer.getBondArgb1(index));
		return Color.white;
	}

	public FillMode getFillMode() {
		return fillMode;
	}

	public void setFillMode(FillMode fm) {
		fillMode = fm;
		if (fillMode == FillMode.getNoFillMode()) {
			setBackground(Color.black);
			setBackgroundImage(null);
		}
		else if (fillMode instanceof FillMode.ColorFill) {
			setBackground(((FillMode.ColorFill) fillMode).getColor());
			setBackgroundImage(null);
		}
		else if (fillMode instanceof FillMode.ImageFill) {
			setBackground(new Color(0x00000000, true));
			String s = ((FillMode.ImageFill) fillMode).getURL();
			if (FileUtilities.isRelative(s)) {
				if (resourceAddress == null) {
					setFillMode(FillMode.getNoFillMode());
				}
				s = FileUtilities.getCodeBase(resourceAddress) + s;
			}
			URL remoteCopy = ConnectionManager.sharedInstance().getRemoteLocation(s);
			if (remoteCopy != null)
				s = remoteCopy.toString();
			if (FileUtilities.isRemote(s)) {
				URL url = null;
				try {
					url = new URL(s);
				}
				catch (MalformedURLException e) {
					setBackgroundImage(null);
					repaint();
					return;
				}
				ImageIcon icon = ConnectionManager.sharedInstance().loadImage(url);
				if (icon != null && icon.getIconWidth() > 0 && icon.getIconHeight() > 0) {
					setBackgroundImage(icon);
					icon.setDescription(FileUtilities.getFileName(s));
				}
				else {
					setBackgroundImage(null);
				}
			}
			else {
				File f2 = new File(FileUtilities.getCodeBase(resourceAddress), FileUtilities.getFileName(s));
				if (!f2.exists() && !f2.toString().equals(s)) {
					ModelerUtilities.copyResourceToDirectory(s, f2.getParentFile());
				}
				ImageIcon icon = new ImageIcon(Toolkit.getDefaultToolkit().createImage(s));
				icon.setDescription(FileUtilities.getFileName(s));
				setBackgroundImage(icon);
			}
		}
		else {
			setBackground(new Color(0x00000000, true));
			setBackgroundImage(null);
		}
		repaint();
	}

	private void setBackgroundImage(ImageIcon icon) {
		if (icon == null) {
			backgroundImage = null;
			return;
		}
		backgroundImage = new ImageIcon(icon.getImage());
		backgroundImage.setDescription(icon.getDescription());
		iconWidth = backgroundImage.getIconWidth();
		iconHeight = backgroundImage.getIconHeight();
		if (fillMode instanceof FillMode.ImageFill) {
			((FillMode.ImageFill) fillMode).setURL(FileUtilities.getFileName(icon.getDescription()));
		}
	}

	public void changeFillMode(FillMode fm) {
		if (fm == null)
			return;
		if (fm.equals(getFillMode()))
			return;
		setFillMode(fm);
		if (fm instanceof FillMode.ColorFill) {
			setBackground(((FillMode.ColorFill) fm).getColor());
		}
	}

	public void setBackground(final Color c) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				Jmol.super.setBackground(c);
			}
		});
		viewer.setBackgroundArgb(c.getRGB());
		navigator.setBackground(c);
		cockpit.setBackground(c);
	}

	void setPerspectiveDepth(boolean b) {
		viewer.setPerspectiveDepth(b);
	}

	boolean getPerspectiveDepth() {
		return viewer.getPerspectiveDepth();
	}

	public void setAlternativeScriptConsole(Component console) {
		scriptConsole = console;
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
		if (fillMode instanceof FillMode.GradientFill) {
			FillMode.GradientFill gfm = (FillMode.GradientFill) fillMode;
			GradientFactory.paintRect((Graphics2D) g, gfm.getStyle(), gfm.getVariant(), gfm.getColor1(), gfm
					.getColor2(), 0, 0, getWidth(), getHeight());
		}
		else if (fillMode instanceof FillMode.PatternFill) {
			Graphics2D g2 = (Graphics2D) g;
			g2.setPaint(((FillMode.PatternFill) fillMode).getPaint());
			g2.fillRect(0, 0, getWidth(), getHeight());
		}
		super.paintComponent(g);
		update(g);
	}

	private void paintSteering(Graphics2D g) {
		if (container.isRoverMode()) {
			int x = (int) (container.motionGenerator.getSteeringStrength() * 10);
			if (x != 0) {
				if (steeringForceMeter == null)
					steeringForceMeter = new SteeringForceMeter();
				steeringForceMeter.setCurrentReading(x);
				steeringForceMeter.paint(g);
			}
			else {
				if (steeringForceMeter != null)
					steeringForceMeter.setCurrentReading(0);
			}
		}
	}

	public void update(Graphics g) {
		if (paintLoadingMessage) {
			Color bg = viewer.getColorBackground();
			g.setColor(bg);
			g.fillRect(0, 0, getWidth(), getHeight());
			g.setColor(new Color(0xffffff ^ bg.getRGB()));
			String s = JmolContainer.getInternationalText("PleaseWait");
			if (s == null)
				s = "Loading data, please wait...";
			g.setFont(FONT_BOLD_18);
			FontMetrics fm = g.getFontMetrics();
			g.drawString(s, (getWidth() - fm.stringWidth(s)) >> 1, (getHeight() - fm.getHeight()) >> 1);
			s = "Jmol Container, Molecular Workbench";
			g.setFont(FONT_BOLD_15);
			fm = g.getFontMetrics();
			g.drawString(s, (getWidth() - fm.stringWidth(s)) >> 1, (getHeight() >> 1) + fm.getHeight());
			return;
		}
		if (viewer == null || currentSize == null || currentSize.width <= 0 || currentSize.height <= 0)
			return;
		synchronized (lock) {
			g.getClipBounds(clipRect);
			viewer.renderScreenImage(g, currentSize, clipRect);
			if (viewer.getNavigationMode()) {
				navigator.paint(g);
				cockpit.paint(g);
			}
			if (imageList != null && !imageList.isEmpty()) {
				for (ImageComponent ic : imageList) {
					ic.paint(this, g);
				}
			}
		}
		paintSteering((Graphics2D) g);
		super.update(g);
	}

	// TODO: Loading a big molecule can take a long time. Implement progress bar here.
	void load(String address) {
		viewer.openFile(address);
		// misnamed in Jmol -- really this opens the file, gets the data, and returns error or null
		viewer.getOpenFileError();
	}

	public void setSize(int w, int h) {
		super.setSize(w, h);
		updateSize();
	}

	public void setSize(Dimension d) {
		super.setSize(d);
		updateSize();
	}

	void updateSize() {
		getSize(currentSize);
		viewer.setScreenDimension(currentSize);
	}

	private void notifyLoadMoleculeListeners() {
		if (loadMoleculeListeners == null)
			return;
		LoadMoleculeEvent e = new LoadMoleculeEvent(this, null);
		for (LoadMoleculeListener l : loadMoleculeListeners)
			l.moleculeLoaded(e);
	}

	void addLoadMoleculeListener(LoadMoleculeListener l) {
		if (loadMoleculeListeners == null)
			loadMoleculeListeners = new ArrayList<LoadMoleculeListener>();
		if (l == null || loadMoleculeListeners == null)
			return;
		if (!loadMoleculeListeners.contains(l))
			loadMoleculeListeners.add(l);
	}

	void removeLoadMoleculeListener(LoadMoleculeListener l) {
		if (l == null || loadMoleculeListeners == null)
			return;
		if (loadMoleculeListeners.contains(l))
			loadMoleculeListeners.remove(l);
	}

	protected void processKeyPressed(KeyEvent e) {
		super.processKeyPressed(e);
		if (!e.isControlDown()) {
			navigator.keyPressed(e);
		}
		// MUST consume in order to stop the event from propogating to the parent components
		e.consume();
	}

	protected void processKeyReleased(KeyEvent e) {
		super.processKeyReleased(e);
		navigator.keyReleased(e); // remove pressed bits
		navigator.engineOff();
		// MUST not consume this event and leave this to the key binding to work. As a result, key binding
		// must set KeyStroke with onKeyRelease flag set to true.
		// e.consume();
	}

	class SteeringForceMeter {

		private int currentReading;

		void setCurrentReading(int currentReading) {
			this.currentReading = currentReading;
		}

		void paint(Graphics2D g) {
			if (currentReading == 0)
				return;
			int x = getWidth() - 60;
			int y = getHeight() - 15;
			g.setColor(Color.lightGray);
			g.fillRect(x, y, 50, 8);
			g.setColor(Color.red);
			g.fillRect(x + 1, y + 1, currentReading, 7);
			g.setColor(contrastBackground());
			g.drawRect(x, y, 50, 8);

		}

	}

}