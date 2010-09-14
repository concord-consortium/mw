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

package org.concord.mw3d;


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

import org.myjmol.api.JmolStatusListener;

import org.concord.jmol.CommandEvent;
import org.concord.jmol.CommandListener;
import org.concord.modeler.ConnectionManager;
import org.concord.modeler.Model;
import org.concord.modeler.ModelerUtilities;
import org.concord.modeler.Movie;
import org.concord.modeler.MovieSlider;
import org.concord.modeler.draw.DrawingElement;
import org.concord.modeler.draw.DrawingElementStateFactory;
import org.concord.modeler.draw.FillMode;
import org.concord.modeler.event.ModelEvent;
import org.concord.modeler.event.ModelListener;
import org.concord.modeler.event.PageComponentEvent;
import org.concord.modeler.event.PageComponentListener;
import org.concord.modeler.event.ProgressEvent;
import org.concord.modeler.event.ProgressListener;
import org.concord.modeler.event.ScriptExecutionEvent;
import org.concord.modeler.event.ScriptExecutionListener;
import org.concord.modeler.process.Executable;
import org.concord.modeler.process.ImageStreamGenerator;
import org.concord.modeler.process.Job;
import org.concord.modeler.ui.ComboBoxRenderer;
import org.concord.modeler.ui.IconPool;
import org.concord.modeler.util.DataQueue;
import org.concord.modeler.util.FileChooser;
import org.concord.modeler.util.FileUtilities;
import org.concord.modeler.util.FloatQueue;
import org.concord.modeler.util.HomoQueueGroup;
import org.concord.modeler.util.ScreenshotSaver;
import org.concord.mw3d.models.ABond;
import org.concord.mw3d.models.Atom;
import org.concord.mw3d.models.CuboidObstacle;
import org.concord.mw3d.models.CuboidObstacleState;
import org.concord.mw3d.models.CylinderObstacle;
import org.concord.mw3d.models.CylinderObstacleState;
import org.concord.mw3d.models.MolecularModel;
import org.concord.mw3d.models.ObstacleState;
import org.concord.mw3d.models.TBond;
import org.concord.mw3d.models.XyzReader;
import org.concord.mw3d.models.XyzWriter;

import static javax.swing.Action.*;
import static org.concord.mw3d.UserAction.*;

public abstract class MolecularContainer extends JComponent implements Model, JmolStatusListener, CommandListener,
		ProgressListener, ScriptExecutionListener {

	final static String REGEX_SEPARATOR = "[\\s&&[^\\r\\n]]+";

	private static ResourceBundle bundle;
	private static boolean isUSLocale;

	private static boolean asApplet;

	MolecularModel model;
	MolecularView view;

	private String resourceAddress;
	private XyzReader modelReader;
	private XyzWriter modelWriter;
	private ImageStreamGenerator imageStreamGenerator;

	/* event handling */
	private List<ModelListener> modelListenerList;
	private List<PageComponentListener> pageComponentListenerList;
	private PageComponentEvent modelChangeEvent;
	private boolean boxRotated;
	private ActionReminder actionReminder;

	/* GUI components */
	FileChooser fileChooser;
	private JMenuBar menuBar;
	private JComponent toolBar;
	private JPanel topPanel;
	private JPanel runPanel, moviePanel;
	private JProgressBar progressBar;
	private JMenuItem snapshotMenuItem;
	private Action runAction, stopAction, resetAction;
	private boolean bottomBarShown = true;
	private AbstractButton rotateButton;
	private ButtonGroup toolBarButtonGroup;

	private JPopupMenu defaultPopupMenu;
	private SelectToolPopupMenu selectToolPopupMenu;
	private DropToolPopupMenu dropToolPopupMenu;
	private DropObstaclePopupMenu dropObstaclePopupMenu;
	private ElementSelectionPanel elementSelectionPanel;
	private MoleculeSelectionPanel moleculeSelectionPanel;
	private RotateToolPopupMenu rotateToolPopupMenu;
	private DeleteToolPopupMenu deleteToolPopupMenu;
	private MiscToolPopupMenu miscToolPopupMenu;
	private BondToolPopupMenu bondToolPopupMenu;
	MinimizerDialog minimizerDialog;

	public MolecularContainer(int tapeLength) {

		fileChooser = ModelerUtilities.fileChooser;
		setLayout(new BorderLayout());

		if (bundle == null) {
			isUSLocale = Locale.getDefault().equals(Locale.US);
			try {
				bundle = ResourceBundle.getBundle("org.concord.mw3d.resources.MolecularContainer", Locale.getDefault());
			}
			catch (MissingResourceException e) {
			}
		}

		toolBarButtonGroup = new ButtonGroup();

		view = new MolecularView();
		view.setContainer(this);
		view.setBorder(BorderFactory.createLoweredBevelBorder());

		model = new MolecularModel(tapeLength);
		model.setView(view);
		view.setModel(model);
		view.setSimulationBox();

		Dimension dim = new Dimension(300, 300);
		setPreferredSize(dim);
		setMinimumSize(dim);
		setMaximumSize(dim);

		view.setShowClock(true);
		view.setRightClickJob(new Runnable() {
			public void run() {
				resetToolBar();
			}
		});
		view.getViewer().setJmolStatusListener(this);
		add(view, BorderLayout.CENTER);

		createActions();

		topPanel = new JPanel(new BorderLayout());
		add(topPanel, BorderLayout.NORTH);

		createMenuBar();
		topPanel.add(menuBar, BorderLayout.NORTH);

		createToolBar();
		topPanel.add(toolBar, BorderLayout.CENTER);

		createMoviePanel();
		add(moviePanel, BorderLayout.SOUTH);

		model.setChangeNotifier(new Runnable() {
			public void run() {
				notifyChange();
			}
		});

		model.setRunNotifier(new Runnable() {
			public void run() {
				notifyPageComponentListeners(new PageComponentEvent(this, PageComponentEvent.COMPONENT_RUN));
			}
		});

		actionReminder = new ActionReminder();
		actionReminder.setParentComponent(this);

	}

	public static void setApplet(boolean b) {
		asApplet = b;
	}

	public static boolean isApplet() {
		return asApplet;
	}

	public void addModelListener(ModelListener ml) {
		if (modelListenerList == null)
			modelListenerList = new ArrayList<ModelListener>();
		if (!modelListenerList.contains(ml))
			modelListenerList.add(ml);
	}

	public void removeModelListener(ModelListener ml) {
		if (modelListenerList == null)
			return;
		modelListenerList.remove(ml);
	}

	public List<ModelListener> getModelListeners() {
		return modelListenerList;
	}

	public void notifyModelListeners(ModelEvent e) {
		if (modelListenerList == null)
			return;
		for (ModelListener l : modelListenerList)
			l.modelUpdate(e);
	}

	protected List<PageComponentListener> getPageComponentListeners() {
		return pageComponentListenerList;
	}

	public void addPageComponentListener(PageComponentListener pcl) {
		if (pcl == null)
			return;
		if (pageComponentListenerList == null)
			pageComponentListenerList = new ArrayList<PageComponentListener>();
		if (!pageComponentListenerList.contains(pcl))
			pageComponentListenerList.add(pcl);
	}

	public void removePageComponentListener(PageComponentListener pcl) {
		if (pcl == null)
			return;
		if (pageComponentListenerList != null)
			pageComponentListenerList.remove(pcl);
	}

	public void notifyPageComponentListeners(PageComponentEvent e) {
		if (pageComponentListenerList == null || pageComponentListenerList.isEmpty())
			return;
		for (PageComponentListener l : pageComponentListenerList)
			l.pageComponentChanged(e);
	}

	protected void notifyChange() {
		if (modelChangeEvent == null)
			modelChangeEvent = new PageComponentEvent(this, PageComponentEvent.COMPONENT_CHANGED);
		notifyPageComponentListeners(modelChangeEvent);
	}

	public void setProgressBar(JProgressBar pb) {
		progressBar = pb;
	}

	public void progressReported(final ProgressEvent e) {
		if (progressBar == null)
			return;
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				progressBar.setMinimum(e.getMinimum());
				progressBar.setMaximum(e.getMaximum());
				progressBar.setValue(e.getPercent());
				if (e.getPercent() > 0) {
					progressBar.setString(e.getDescription() + e.getPercent() + "%");
				}
				else {
					progressBar.setString(e.getDescription());
				}
			}
		});
	}

	private void setProgressMessage(final String msg) {
		if (progressBar == null)
			return;
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				progressBar.setValue(progressBar.getMinimum());
				progressBar.setString(msg);
			}
		});
	}

	protected void init() {
		view.renderModel(false);
		// must call the above method at least once for the case in which a new container is inserted into a page
		view.createPopupMenusRelatedToContainer(this);
	}

	static String getInternationalText(String name) {
		if (bundle == null)
			return null;
		if (name == null)
			return null;
		if (isUSLocale)
			return null;
		String s = null;
		try {
			s = bundle.getString(name);
		}
		catch (MissingResourceException e) {
			s = null;
		}
		return s;
	}

	public void run() {
		model.run();
		if (view.getCameraAtom() >= 0) {
			view.navigator.setEnabled(false);
		}
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				view.deselectAll();
				// view.runJmolScript("label OFF"); //Should we do this?
				runAction.setEnabled(false);
				stopAction.setEnabled(true);
			}
		});
	}

	public void stopImmediately() {
		model.stopImmediately();
		view.navigator.setEnabled(true);
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				runAction.setEnabled(true);
				stopAction.setEnabled(false);
			}
		});
	}

	public void stop() {
		model.stop();
		view.navigator.setEnabled(true);
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				runAction.setEnabled(true);
				stopAction.setEnabled(false);
			}
		});
	}

	public boolean isRunning() {
		return model.isRunning();
	}

	public String runJmolScript(String s) {
		return view.runJmolScript(s);
	}

	public String runMwScript(String s) {
		model.addScriptExecutionListener(this);
		return model.runScript(s);
	}

	public String runMwScriptImmediately(String s) {
		model.addScriptExecutionListener(this);
		return model.runScriptImmediately(s);
	}

	public void setInitializationScriptToRun(boolean b) {
		model.setInitializationScriptToRun(b);
	}

	public void haltScriptExecution() {
		model.haltScriptExecution();
		view.runJmolScript("exit");
	}

	public void scriptExecuted(final ScriptExecutionEvent e) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				scriptExecuted(e.getDescription());
			}
		});
	}

	private void scriptExecuted(String description) {
		if ("reset".equals(description)) {
			resetAction.actionPerformed(null);
		}
		else if ("run".equals(description)) {
			runAction.actionPerformed(null);
		}
		else if ("stop".equals(description)) {
			stopAction.actionPerformed(null);
		}
		else if ("script end".equals(description)) {
			notifyModelListeners(new ModelEvent(this, ModelEvent.SCRIPT_END));
		}
		else if ("script start".equals(description)) {
			notifyModelListeners(new ModelEvent(this, ModelEvent.SCRIPT_START));
		}
	}

	public void reset() {
		view.navigator.setEnabled(true);
		model.stopImmediately();
		stopAction.actionPerformed(null);
		model.clear();
		view.reset();
		repaint();
		// resourceAddress=null; this would disable the reset button.
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				resetButtons();
			}
		});
	}

	void resetButtons() {
		view.setSpinOn(false);
		resetToolBar();
	}

	public void setLoadingMessagePainted(boolean b) {
		view.setLoadingMessagePainted(b);
	}

	public String getResourceAddress() {
		return resourceAddress;
	}

	/*
	 * Set the screen dimension of the viewer to the preferred size. On some OS (e.g. Mac) the viewer might not have
	 * been given a non-zero size (i.e. made visible) when we are going to create the jmol screen image in the following
	 * code. The following line is not an ideal solution to fix this problem, because the preferred size is not
	 * necessarily the final size (although in the usual mode, the final width should be 2 pixels shorter than the
	 * preferred with).
	 */
	private void presetSize() {
		// if (view.getSize().width == 0 || view.getSize().height == 0)
		view.getViewer().setScreenDimension(getPreferredSize());
	}

	public void input(String address, boolean reset) {
		presetSize();
		resourceAddress = address;
		view.setResourceAddress(resourceAddress);
		view.setCodeBase(FileUtilities.getCodeBase(resourceAddress));
		if (address == null)
			return;
		if (FileUtilities.isRemote(address)) {
			URL url = null;
			try {
				url = new URL(address);
			}
			catch (MalformedURLException e) {
				e.printStackTrace();
				return;
			}
			input(url, reset);
		}
		else {
			input(new File(address), reset);
		}
	}

	public void input(File file) {
		input(file, false);
	}

	public void input(File file, boolean reset) {
		if (file == null)
			return;
		resourceAddress = file.getAbsolutePath();
		view.setResourceAddress(resourceAddress);
		inputXyz(new File(FileUtilities.changeExtension(resourceAddress, "xyz")));
		inputMdd(file);
		view.setLoadingMessagePainted(false);
		view.refresh();
		if (!reset) {
			model.clearMouseScripts();
		}
	}

	private void inputMdd(File file) {
		if (progressBar != null) {
			EventQueue.invokeLater(new Runnable() {
				public void run() {
					progressBar.setIndeterminate(true);
				}
			});
		}
		try {
			decode(new XMLDecoder(new BufferedInputStream(new FileInputStream(file))));
		}
		catch (Exception e) {
			e.printStackTrace();
			EventQueue.invokeLater(new Runnable() {
				public void run() {
					JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(view), resourceAddress
							+ " was not found or has a problem.", "File error", JOptionPane.ERROR_MESSAGE);
				}
			});
		}
		finally {
			if (progressBar != null) {
				EventQueue.invokeLater(new Runnable() {
					public void run() {
						progressBar.setIndeterminate(false);
					}
				});
			}
		}
	}

	void inputXyz(File file) {
		reset();
		inputXyzWithoutClearing(file);
	}

	void inputXyzWithoutClearing(File file) {
		if (modelReader == null) {
			modelReader = new XyzReader(model);
			modelReader.addProgressListener(this);
		}
		try {
			modelReader.read(file);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void input(URL url) {
		input(url, false);
	}

	public void input(URL url, boolean reset) {
		if (url == null)
			return;
		resourceAddress = url.toString();
		view.setResourceAddress(resourceAddress);
		try {
			String xyzAddress = FileUtilities.changeExtension(resourceAddress, "xyz");
			xyzAddress = FileUtilities.httpEncode(xyzAddress);
			File file = ConnectionManager.sharedInstance().shouldUpdate(xyzAddress);
			if (file == null)
				file = ConnectionManager.sharedInstance().cache(xyzAddress);
			file = ConnectionManager.sharedInstance().shouldUpdate(resourceAddress);
			if (file == null) {
				try {
					file = ConnectionManager.sharedInstance().cache(resourceAddress);
				}
				catch (FileNotFoundException fnfe) {
				}
			}
			if (file != null) {
				input(file, reset);
			}
			else {
				try {
					inputXyz(new URL(xyzAddress));
					inputMdd(new URL(FileUtilities.httpEncode(resourceAddress)));
				}
				catch (Exception e) {
					e.printStackTrace();
				}
				view.setLoadingMessagePainted(false);
				view.refresh();
			}
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		if (!reset) {
			model.clearMouseScripts();
		}
	}

	private void inputMdd(URL url) {
		if (progressBar != null) {
			EventQueue.invokeLater(new Runnable() {
				public void run() {
					progressBar.setIndeterminate(true);
				}
			});
		}
		try {
			decode(new XMLDecoder(new BufferedInputStream(url.openStream())));
		}
		catch (Exception e) {
			e.printStackTrace();
			EventQueue.invokeLater(new Runnable() {
				public void run() {
					JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(view), resourceAddress
							+ " was not found or has a problem.", "File error", JOptionPane.ERROR_MESSAGE);
				}
			});
		}
		finally {
			if (progressBar != null) {
				EventQueue.invokeLater(new Runnable() {
					public void run() {
						progressBar.setIndeterminate(false);
					}
				});
			}
		}
	}

	private void inputXyz(URL url) {
		reset();
		if (modelReader == null) {
			modelReader = new XyzReader(model);
			modelReader.addProgressListener(this);
		}
		try {
			modelReader.read(url);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void outputXyz(File file) {
		stopImmediately();
		if (file == null)
			return;
		if (modelWriter == null) {
			modelWriter = new XyzWriter(model);
			modelWriter.addProgressListener(this);
		}
		try {
			modelWriter.write(file);
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void output(final File file) {
		model.clearScriptQueue();
		MyImageSaver.saveImages(view, file.getParentFile()); // save images first before resourceAddress changes
		resourceAddress = file.toString();
		File xyzFile = new File(FileUtilities.changeExtension(resourceAddress, "xyz"));
		outputXyz(xyzFile);
		if (progressBar != null) {
			EventQueue.invokeLater(new Runnable() {
				public void run() {
					progressBar.setIndeterminate(true);
				}
			});
		}
		view.setResourceAddress(resourceAddress);
		OutputStream os = null;
		try {
			os = new BufferedOutputStream(new FileOutputStream(file));
		}
		catch (IOException e) {
			e.printStackTrace();
			EventQueue.invokeLater(new Runnable() {
				public void run() {
					JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(view), "Error in writing to "
							+ resourceAddress, "Write Error", JOptionPane.ERROR_MESSAGE);
				}
			});
			return;
		}
		XMLEncoder out = new XMLEncoder(os);
		try {
			synchronized (view.getUpdateLock()) {
				encode(out, FileUtilities.getFileName(xyzFile.toString()));
			}
		}
		catch (Exception e) {
			e.printStackTrace();
			EventQueue.invokeLater(new Runnable() {
				public void run() {
					JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(view), "Encoding error: "
							+ resourceAddress, "Write Error", JOptionPane.ERROR_MESSAGE);
				}
			});
		}
		finally {
			out.close();
			try {
				os.close();
			}
			catch (IOException iox) {
			}
			EventQueue.invokeLater(new Runnable() {
				public void run() {
					progressBar.setIndeterminate(false);
					runAction.setEnabled(true);
					stopAction.setEnabled(false);
					setProgressMessage("Model saved to " + file);
				}
			});
		}
	}

	private void encode(XMLEncoder out, String xyzFileName) throws Exception {

		setProgressMessage("Writing states ......");
		ModelState state = new ModelState();
		state.setXyzFileName(xyzFileName);
		state.setRotation(view.getViewer().getCurrentOrientation());
		state.setNavigationMode(view.getViewer().getNavigationMode());
		state.setPerspectiveDepth(view.getViewer().getPerspectiveDepth());
		int cameraAtom = view.getCameraAtom();
		if (cameraAtom >= 0) {
			state.setCameraAtom(view.getCameraAtom());
		}
		else {
			Point3f cp = view.getViewer().getCameraPosition();
			state.setCameraPosition(cp.x + " " + cp.y + " " + cp.z);
		}
		state.setFrameInterval(model.getFrameInterval());
		state.setViewRefreshInterval(model.getViewRefreshInterval());
		state.setInitScript(model.getInitializationScript());
		state.setZDepthMagnification(view.getViewer().getZDepthMagnification());
		state.setShowAtomIndex(view.getShowAtomIndex());
		state.setShowGlassSimulationBox(view.getShowGlassSimulationBox());
		state.setShowClock(view.getShowClock());
		state.setShowAxes(view.areAxesShown());
		state.setAxisStyle(view.getViewer().getAxisStyle());
		state.setShowEnergizer(view.getShowEnergizer());
		state.setKeShading(view.getKeShading());
		state.setShowCharge(view.getShowCharge());
		state.setFullSizeUnbondedAtoms(view.getFullSizeUnbondedAtoms());
		state.setShowVdwLines(view.getShowVdwLines());
		state.setVdwLinesRatio(view.getVdwLinesRatio());
		state.setVelocityVectorScalingFactor(view.getVelocityVectorScalingFactor());
		state.setMoleculeStyle(view.getMolecularStyle());
		state.setTimeStep(model.getTimeStep());
		state.setLength(model.getLength());
		state.setWidth(model.getWidth());
		state.setHeight(model.getHeight());
		state.setHeatBath(model.getHeatBath());
		state.setX1Mass(model.getElementMass("X1"));
		state.setX2Mass(model.getElementMass("X2"));
		state.setX3Mass(model.getElementMass("X3"));
		state.setX4Mass(model.getElementMass("X4"));
		state.setX1Sigma(model.getElementSigma("X1"));
		state.setX2Sigma(model.getElementSigma("X2"));
		state.setX3Sigma(model.getElementSigma("X3"));
		state.setX4Sigma(model.getElementSigma("X4"));
		state.setX1Epsilon(model.getElementEpsilon("X1"));
		state.setX2Epsilon(model.getElementEpsilon("X2"));
		state.setX3Epsilon(model.getElementEpsilon("X3"));
		state.setX4Epsilon(model.getElementEpsilon("X4"));
		state.setX1Argb(view.getElementArgb("X1"));
		state.setX2Argb(view.getElementArgb("X2"));
		state.setX3Argb(view.getElementArgb("X3"));
		state.setX4Argb(view.getElementArgb("X4"));
		if (model.getGField() != null) {
			if (!model.getGField().isAlwaysDown())
				state.setGFieldDirection(model.getGField().getDirection());
			state.setGravitationalAcceleration(model.getGField().getIntensity());
		}
		if (model.getBField() != null) {
			state.setBFieldDirection(model.getBField().getDirection());
			state.setBFieldIntensity(model.getBField().getIntensity());
		}
		if (model.getEField() != null) {
			state.setEFieldDirection(model.getEField().getDirection());
			state.setEFieldIntensity(model.getEField().getIntensity());
		}
		int n = model.getObstacleCount();
		if (n > 0) {
			for (int i = 0; i < n; i++) {
				state.addObstacle(model.getObstacle(i));
			}
		}
		if (view.getFillMode() != FillMode.getNoFillMode()) {
			if (view.getFillMode() instanceof FillMode.ImageFill) {
				FillMode.ImageFill imgFillMode = (FillMode.ImageFill) view.getFillMode();
				imgFillMode.setURL(FileUtilities.getFileName(imgFillMode.getURL()));
			}
			state.setFillMode(view.getFillMode());
		}

		if (view.getDrawList() != null && !view.getDrawList().isEmpty()) {
			List<Object> drawList = new ArrayList<Object>();
			Object o;
			for (DrawingElement e : view.getDrawList()) {
				o = DrawingElementStateFactory.createState(e);
				if (o != null)
					drawList.add(o);
			}
			state.setDrawList(drawList);
		}

		String s = null;
		if (view.getVelocityBitSet().cardinality() > 0) {
			s = view.getVelocityBitSet().toString().replaceAll(",", "");
			state.setVelocitySelection(s.substring(1, s.length() - 1));
		}
		if (view.getTrajectoryBitSet().cardinality() > 0) {
			s = view.getTrajectoryBitSet().toString().replaceAll(",", "");
			state.setTrajectorySelection(s.substring(1, s.length() - 1));
		}
		if (view.getTranslucentBitSet().cardinality() > 0) {
			s = view.getTranslucentBitSet().toString().replaceAll(",", "");
			state.setTranslucentSelection(s.substring(1, s.length() - 1));
		}
		s = model.getAtomPropertySelection(Atom.UNMOVABLE);
		if (s != null && !s.equals("")) {
			state.setUnmovableSelection(s);
		}
		s = model.getAtomPropertySelection(Atom.INVISIBLE);
		if (s != null && !s.equals("")) {
			state.setInvisibleSelection(s);
		}

		if (model.getJob() != null)
			state.addTasks(model.getJob().getCustomTasks());
		out.writeObject(state);

	}

	private void addBonds() {
		ABond abond;
		for (int i = 0; i < model.getABondCount(); i++) {
			abond = model.getABond(i);
			view.getViewer().addABond(abond.getAtom1().getIndex(), abond.getAtom2().getIndex(),
					abond.getAtom3().getIndex());
		}
		TBond tbond;
		for (int i = 0; i < model.getTBondCount(); i++) {
			tbond = model.getTBond(i);
			view.getViewer().addTBond(tbond.getAtom1().getIndex(), tbond.getAtom2().getIndex(),
					tbond.getAtom3().getIndex(), tbond.getAtom4().getIndex());
		}
	}

	private void decode(XMLDecoder in) throws Exception {
		setProgressMessage("Reading states ......");
		ModelState state = (ModelState) in.readObject();
		model.setElementMass("X1", state.getX1Mass());
		model.setElementMass("X2", state.getX2Mass());
		model.setElementMass("X3", state.getX3Mass());
		model.setElementMass("X4", state.getX4Mass());
		model.setElementSigma("X1", state.getX1Sigma());
		model.setElementSigma("X2", state.getX2Sigma());
		model.setElementSigma("X3", state.getX3Sigma());
		model.setElementSigma("X4", state.getX4Sigma());
		model.setElementEpsilon("X1", state.getX1Epsilon());
		model.setElementEpsilon("X2", state.getX2Epsilon());
		model.setElementEpsilon("X3", state.getX3Epsilon());
		model.setElementEpsilon("X4", state.getX4Epsilon());
		view.setElementArgb("X1", state.getX1Argb());
		view.setElementArgb("X2", state.getX2Argb());
		view.setElementArgb("X3", state.getX3Argb());
		view.setElementArgb("X4", state.getX4Argb());
		view.getViewer().setNavigationMode(state.getNavigationMode());
		if (state.getCameraAtom() < 0) {
			if (state.getCameraPosition() != null) {
				String[] cp = state.getCameraPosition().split("\\s");
				float cpx = Float.parseFloat(cp[0]);
				float cpy = Float.parseFloat(cp[1]);
				float cpz = Float.parseFloat(cp[2]);
				view.getViewer().setCameraPosition(cpx, cpy, cpz);
			}
		}
		else {
			Atom at = model.getAtom(state.getCameraAtom());
			view.getViewer().setCameraPosition(at.getRx(), at.getRy(), at.getRz());
		}
		String s = state.getRotation();
		view.setOrientation(s);
		if (state.getCameraAtom() < 0) {
			if (s != null) {
				String[] t = s.trim().split("\\s");
				float[] x = new float[] { 0, 0, 0, 0, 100 };
				for (int i = 0; i < Math.min(t.length, 5); i++)
					x[i] = Float.parseFloat(t[i]);
				if (x[0] == 0 && x[1] == 0 && x[2] == 0) // rotation axis (0, 0, 0) has no meaning, set it to z-axis
					x[2] = 1;
				view.setStartingScene(new Vector3f(x[0], x[1], x[2]), x[3], x[4]);
			}
		}
		view.setShowClock(state.getShowClock());
		view.setShowEnergizer(state.getShowEnergizer());
		view.setFillMode(state.getFillMode());
		List drawList = state.getDrawList();
		if (drawList != null && !drawList.isEmpty()) {
			DrawingElement e;
			for (Iterator it = drawList.iterator(); it.hasNext();) {
				e = DrawingElementStateFactory.createElement(it.next());
				if (e != null) {
					e.setComponent(view);
					view.addElement(e);
				}
			}
		}
		model.setFrameInterval(state.getFrameInterval());
		model.setViewRefreshInterval(state.getViewRefreshInterval());
		model.setInitializationScript(state.getInitScript());
		model.setTimeStep(state.getTimeStep());
		model.setLength(state.getLength());
		model.setWidth(state.getWidth());
		model.setHeight(state.getHeight());
		if (state.getHeatBath() != null) {
			model.activateHeatBath(true);
			model.getHeatBath().setExpectedTemperature(state.getHeatBath().getExpectedTemperature());
			model.getHeatBath().setInterval(state.getHeatBath().getInterval());
		}
		else {
			model.activateHeatBath(false);
		}
		if (state.getGravitationalAcceleration() > 0) {
			model.setGField(state.getGravitationalAcceleration(), state.getGFieldDirection() == null ? null
					: new Vector3f(state.getGFieldDirection()));
		}
		else {
			model.setGField(0, null);
		}
		if (state.getBFieldIntensity() > 0 && state.getBFieldDirection() != null) {
			model.setBField(state.getBFieldIntensity(), new Vector3f(state.getBFieldDirection()));
		}
		else {
			model.setBField(0, null);
		}
		if (state.getEFieldIntensity() > 0 && state.getEFieldDirection() != null) {
			model.setEField(state.getEFieldIntensity(), new Vector3f(state.getEFieldDirection()));
		}
		else {
			model.setEField(0, null);
		}
		view.setSimulationBox();
		addBonds();
		model.formMolecules();
		setProgressMessage("Building model ......");
		view.renderModel(true); // must render the model so that the following settings can take effect
		view.getViewer().setPerspectiveDepth(state.getPerspectiveDepth());
		view.getViewer().setZDepthMagnification(state.getZDepthMagnification());
		view.setCameraAtom(state.getCameraAtom());
		view.setShowGlassSimulationBox(state.getShowGlassSimulationBox());
		view.setMolecularStyle(state.getMoleculeStyle());
		view.setAxesShown(state.getShowAxes());
		view.getViewer().setAxisStyle(state.getAxisStyle());
		view.setShowAtomIndex(state.getShowAtomIndex());
		view.setKeShading(state.getKeShading());
		view.setShowCharge(state.getShowCharge());
		view.setShowVdwLines(state.getShowVdwLines());
		view.setFullSizeUnbondedAtoms(state.getFullSizeUnbondedAtoms());
		view.setVdwLinesRatio(state.getVdwLinesRatio());
		view.setVelocityVectorScalingFactor(state.getVelocityVectorScalingFactor());
		s = state.getVelocitySelection();
		if (s != null) {
			String[] t = s.split(REGEX_SEPARATOR);
			int n = t.length;
			if (n > 0) {
				for (int i = 0; i < n; i++) {
					view.showVelocity(Integer.parseInt(t[i]), true);
				}
			}
		}
		s = state.getTrajectorySelection();
		if (s != null) {
			String[] t = s.split(REGEX_SEPARATOR);
			int n = t.length;
			if (n > 0) {
				for (int i = 0; i < n; i++) {
					view.showTrajectory(Integer.parseInt(t[i]), true);
				}
			}
		}
		s = state.getTranslucentSelection();
		if (s != null) {
			String[] t = s.split(REGEX_SEPARATOR);
			int n = t.length;
			if (n > 0) {
				for (int i = 0; i < n; i++) {
					view.getTranslucentBitSet().set(Integer.parseInt(t[i]));
				}
			}
		}
		s = state.getUnmovableSelection();
		if (s != null) {
			String[] t = s.split(REGEX_SEPARATOR);
			int n = t.length;
			if (n > 0) {
				for (int i = 0; i < n; i++) {
					model.getAtom(Integer.parseInt(t[i])).setMovable(false);
				}
			}
		}
		s = state.getInvisibleSelection();
		if (s != null) {
			String[] t = s.split(REGEX_SEPARATOR);
			int n = t.length;
			if (n > 0) {
				for (int i = 0; i < n; i++) {
					Atom a = model.getAtom(Integer.parseInt(t[i]));
					a.setVisible(false);
					view.setVisible(a, false);
				}
			}
		}
		List<ObstacleState> list = state.getObstacles();
		if (list != null && !list.isEmpty()) {
			for (ObstacleState o : list) {
				if (o instanceof CuboidObstacleState) {
					CuboidObstacleState cos = (CuboidObstacleState) o;
					CuboidObstacle obs = new CuboidObstacle(cos);
					model.addObstacle(obs);
					view.getViewer().addCuboidObstacle(obs.getCenter().x, obs.getCenter().y, obs.getCenter().z,
							obs.getCorner().x, obs.getCorner().y, obs.getCorner().z);
					view.setObstacleColor(obs, new Color(cos.getColor()), cos.isTranslucent());
				}
				else if (o instanceof CylinderObstacleState) {
					CylinderObstacleState cos = (CylinderObstacleState) o;
					CylinderObstacle obs = new CylinderObstacle(cos);
					model.addObstacle(obs);
					view.getViewer().addCylinderObstacle(obs.getCenter().x, obs.getCenter().y, obs.getCenter().z,
							obs.getAxis(), obs.getHeight(), obs.getRadius());
					view.setObstacleColor(obs, new Color(cos.getColor()), cos.isTranslucent());
				}
			}
		}
		view.setStartingSceneWhenCameraIsOnAtom();
		model.setRotationMatrix(view.getViewer().getRotationMatrix());
		model.addCustomTasks(state.getTasks());
		model.getJob().processPendingRequests();
	}

	private void setGenericParticles() {
		int n = model.getAtomCount();
		for (int i = 0; i < n; i++) {
			Atom a = model.getAtom(i);
			if (!a.isGenericParticle())
				continue;
			String symbol = a.getSymbol();
			float mass = model.getElementMass(symbol);
			a.setMass(mass);
			float epsilon = model.getElementEpsilon(symbol);
			a.setEpsilon(epsilon);
			float sigma = model.getElementSigma(symbol);
			a.setSigma(sigma);
			view.getViewer().setAtomSize(i, a.getSigma() * 1000);
			view.getViewer().setAtomColor(i, view.getElementArgb(symbol));
		}
	}

	public void notifyFileLoaded(String fullPathName, String fileName, String modelName, Object clientFile,
			String errorMessage) {
		view.getViewer().setSelectionHaloEnabled(true);
		view.setLoadingMessagePainted(false);
		if (view.isRenderingCallTriggeredByLoading()) {
			notifyPageComponentListeners(new PageComponentEvent(this, PageComponentEvent.COMPONENT_LOADED));
			progressReported(new ProgressEvent(this, resourceAddress + " loaded to 3D Simulator."));
		}
		setGenericParticles();
		view.refresh();
	}

	/** not implemented */
	public void notifyFileNotLoaded(String fullPathName, String errorMsg) {
	}

	/** not implemented */
	public void setStatusMessage(String statusMessage) {
	}

	/** not implemented */
	public void scriptEcho(String strEcho) {
	}

	/** not implemented */
	public void scriptStatus(String strStatus) {
	}

	/** not implemented */
	public void notifyScriptTermination(String statusMessage, int msWalltime) {
		if (boxRotated) {
			model.setRotationMatrix(view.getViewer().getRotationMatrix());
			boxRotated = false;
		}
	}

	/** not implemented */
	public void handlePopupMenu(int x, int y) {
	}

	/** not implemented */
	public void notifyMeasurementsChanged() {
	}

	/** not implemented */
	public void notifyFrameChanged(int frameNo) {
	}

	/** not implemented */
	public void notifyAtomPicked(int atomIndex, String strInfo) {
	}

	/** not implemented */
	public void showUrl(String url) {
	}

	/** not implemented */
	public void showConsole(boolean showConsole) {
	}

	/** fit to the BasicModel interface */
	public JComponent getView() {
		return view;
	}

	public MolecularView getMolecularView() {
		return view;
	}

	public MolecularModel getMolecularModel() {
		return model;
	}

	protected abstract void setViewerSize(Dimension dim);

	private void createActions() {

		// save model action
		Action a = new SaveModelAction(this);
		view.getActionMap().put("save model", a);
		view.getInputMap().put((KeyStroke) a.getValue(ACCELERATOR_KEY), "save model");

		// open model action
		a = new OpenModelAction(this);
		view.getActionMap().put("open model", a);
		view.getInputMap().put((KeyStroke) a.getValue(ACCELERATOR_KEY), "open model");

		// spin action
		a = new DefaultAction() {
			public void actionPerformed(ActionEvent e) {
				view.setSpinOn(((AbstractButton) e.getSource()).isSelected());
			}
		};
		a.putValue(NAME, "Spin");
		a.putValue(SHORT_DESCRIPTION, "Spin the model");
		a.putValue(MNEMONIC_KEY, KeyEvent.VK_I);
		a.putValue(SMALL_ICON, IconPool.getIcon("spin"));
		a.putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_F9, 0));
		a.putValue("state", view.isSpinOn() ? Boolean.TRUE : Boolean.FALSE);
		view.getInputMap().put((KeyStroke) a.getValue(ACCELERATOR_KEY), "spin");
		view.getActionMap().put("spin", a);

		// perspective depth switch
		a = new DefaultAction() {
			public void actionPerformed(ActionEvent e) {
				AbstractButton ab = (AbstractButton) e.getSource();
				view.getViewer().setPerspectiveDepth(ab.isSelected());
				view.repaint();
			}
		};
		a.putValue(NAME, "Perspective Depth");
		a.putValue(SHORT_DESCRIPTION, "Perspective Depth");
		a.putValue(MNEMONIC_KEY, new Integer(KeyEvent.VK_V));
		a.putValue("state", view.getViewer().getPerspectiveDepth() ? Boolean.TRUE : Boolean.FALSE);
		view.getActionMap().put("perspective depth", a);

		// run action
		runAction = new DefaultAction() {
			public void actionPerformed(ActionEvent e) {
				resetToolBar();
				setToolBarEnabled(false);
				model.getMovie().enableMovieActions(false);
				if (model.getMovie().getCurrentFrameIndex() < model.getMovie().length()) {
					Thread t = new Thread("Movie Player") {
						public void run() {
							if (model.getMovie().play()) {
								EventQueue.invokeLater(new Runnable() {
									public void run() {
										MolecularContainer.this.run();
									}
								});
							}
						}
					};
					t.setPriority(Thread.MIN_PRIORITY);
					t.start();
				}
				else {
					run();
				}
				notifyModelListeners(new ModelEvent(MolecularContainer.this, ModelEvent.MODEL_RUN));
				notifyPageComponentListeners(new PageComponentEvent(MolecularContainer.this,
						PageComponentEvent.COMPONENT_RUN));
			}
		};
		runAction.putValue(NAME, "Run");
		runAction.putValue(SHORT_DESCRIPTION, "Run the model");
		runAction.putValue(MNEMONIC_KEY, new Integer(KeyEvent.VK_R));
		runAction.putValue(SMALL_ICON, IconPool.getIcon("play"));
		view.getActionMap().put("run", runAction);
		model.getMovie().setRunAction(runAction);

		// stop action
		stopAction = new DefaultAction() {
			public void actionPerformed(ActionEvent e) {
				if (model.getMovie().getCurrentFrameIndex() >= model.getMovie().length() - 2)
					stop();
				model.getMovie().pause();
				model.getMovie().enableMovieActions(true);
				setToolBarEnabled(true);
				notifyModelListeners(new ModelEvent(MolecularContainer.this, ModelEvent.MODEL_STOP));
			}
		};
		stopAction.putValue(NAME, "Stop");
		stopAction.putValue(SHORT_DESCRIPTION, "Stop");
		stopAction.putValue(MNEMONIC_KEY, new Integer(KeyEvent.VK_P));
		stopAction.putValue(SMALL_ICON, IconPool.getIcon("pause"));
		stopAction.setEnabled(false);
		view.getActionMap().put("stop", stopAction);
		model.getMovie().setStopAction(stopAction);

		// reset action
		resetAction = new DefaultAction() {
			public void actionPerformed(ActionEvent e) {
				if (model.getMovie().getCurrentFrameIndex() >= model.getMovie().length() - 2)
					stop();
				if (e != null) {
					if (actionReminder.show(ActionReminder.RESET_TO_SAVED_STATE) == JOptionPane.NO_OPTION)
						return;
				}
				if (resourceAddress != null) {
					if (isApplet()) {
						try {
							input(new URL(resourceAddress), true);
						}
						catch (MalformedURLException e1) {
							e1.printStackTrace();
						}
					}
					else {
						input(resourceAddress, true);
					}
				}
				else reset();
				model.readdMouseAndKeyScripts();
				notifyModelListeners(new ModelEvent(MolecularContainer.this, ModelEvent.MODEL_RESET));
				notifyPageComponentListeners(new PageComponentEvent(MolecularContainer.this,
						PageComponentEvent.COMPONENT_RESET));
			}
		};
		resetAction.putValue(NAME, "Reset");
		resetAction.putValue(SHORT_DESCRIPTION, "Reset");
		resetAction.putValue(MNEMONIC_KEY, new Integer(KeyEvent.VK_T));
		resetAction.putValue(SMALL_ICON, IconPool.getIcon("reset"));
		view.getActionMap().put("reset", resetAction);

		// heat action
		a = new DefaultAction() {
			public void actionPerformed(ActionEvent e) {
				model.transferKE(0.005f);
			}
		};
		a.putValue(NAME, "Heat");
		a.putValue(SHORT_DESCRIPTION, "Heat");
		a.putValue(MNEMONIC_KEY, new Integer(KeyEvent.VK_H));
		a.putValue(SMALL_ICON, IconPool.getIcon("heat"));
		view.getActionMap().put("heat", a);

		// cool action
		a = new DefaultAction() {
			public void actionPerformed(ActionEvent e) {
				model.transferKE(-0.005f);
			}
		};
		a.putValue(NAME, "Cool");
		a.putValue(SHORT_DESCRIPTION, "Cool");
		a.putValue(MNEMONIC_KEY, new Integer(KeyEvent.VK_C));
		a.putValue(SMALL_ICON, IconPool.getIcon("cool"));
		view.getActionMap().put("cool", a);

		a = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				view.showViewProperties();
			}
		};
		a.putValue(NAME, "Show View Options");
		a.putValue(MNEMONIC_KEY, new Integer(KeyEvent.VK_V));
		a.putValue(SHORT_DESCRIPTION, "Show view options");
		a.putValue(SMALL_ICON, IconPool.getIcon("view"));
		a.putValue(ACCELERATOR_KEY, System.getProperty("os.name").startsWith("Mac") ? KeyStroke.getKeyStroke(
				KeyEvent.VK_V, KeyEvent.ALT_MASK | KeyEvent.META_MASK, true) : KeyStroke.getKeyStroke(KeyEvent.VK_V,
				KeyEvent.ALT_MASK | KeyEvent.CTRL_MASK, true));
		view.getInputMap().put((KeyStroke) a.getValue(ACCELERATOR_KEY), "view options");
		view.getActionMap().put("view options", a);

		a = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				ModelProperties modelProperties = new ModelProperties(JOptionPane
						.getFrameForComponent(MolecularContainer.this), model);
				if (e == null)
					modelProperties.selectInitializationScriptTab();
				modelProperties.setLocationRelativeTo(MolecularContainer.this);
				modelProperties.setVisible(true);
			}
		};
		a.putValue(NAME, "Access Model Properties");
		a.putValue(MNEMONIC_KEY, new Integer(KeyEvent.VK_P));
		a.putValue(SHORT_DESCRIPTION, "Access model properties");
		a.putValue(SMALL_ICON, IconPool.getIcon("properties"));
		a.putValue(ACCELERATOR_KEY, System.getProperty("os.name").startsWith("Mac") ? KeyStroke.getKeyStroke(
				KeyEvent.VK_M, KeyEvent.ALT_MASK | KeyEvent.META_MASK, true) : KeyStroke.getKeyStroke(KeyEvent.VK_M,
				KeyEvent.ALT_MASK | KeyEvent.CTRL_MASK, true));
		view.getInputMap().put((KeyStroke) a.getValue(ACCELERATOR_KEY), "properties");
		view.getActionMap().put("properties", a);

		a = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				showTaskManager();
			}
		};
		a.putValue(NAME, "Task Manager");
		a.putValue(SHORT_DESCRIPTION, "Task manager");
		a.putValue(SMALL_ICON, IconPool.getIcon("taskmanager"));
		view.getActionMap().put("task manager", a);

		view.getActionMap().put("show energy", new ShowEnergyAction(this));

	}

	protected void destroy() {
		if (modelReader != null) {
			modelReader.removeProgressListener(this);
		}
		if (modelWriter != null) {
			modelWriter.removeProgressListener(this);
		}
	}

	public void compilerErrorReported(CommandEvent e) {
		JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(this), e.getDescription(), "Script Error",
				JOptionPane.ERROR_MESSAGE);
	}

	public void setScreenshotAction(Action a) {
		JMenu menu = (JMenu) menuBar.getComponent(0);
		JMenuItem mi = new JMenuItem(a);
		a.putValue(NAME, "Save a Screenshot of Model");
		String s = getInternationalText("Screenshot");
		if (s != null)
			mi.setText(s);
		menu.insert(mi, 2);
	}

	public void setSnapshotListener(ActionListener a) {
		snapshotMenuItem.addActionListener(a);
		view.setSnapshotListener(a);
	}

	private void produceImageStream() {
		if (imageStreamGenerator == null) {
			imageStreamGenerator = new ImageStreamGenerator(getView(), model.getJob());
		}
		imageStreamGenerator.chooseDirectory();
	}

	// methods for the GUI follow

	public void enableMenuBar(boolean b) {
		if (b) {
			topPanel.add(menuBar, BorderLayout.NORTH);
		}
		else {
			topPanel.remove(menuBar);
		}
		validate();
		view.updateSize();
	}

	public boolean isMenuBarEnabled() {
		int n = topPanel.getComponentCount();
		for (int i = 0; i < n; i++) {
			if (menuBar == topPanel.getComponent(i))
				return true;
		}
		return false;
	}

	public void enableToolBar(boolean b) {
		if (b) {
			topPanel.add(toolBar, BorderLayout.CENTER);
		}
		else {
			topPanel.remove(toolBar);
		}
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				validate();
			}
		});
		view.updateSize();
	}

	public boolean isToolBarEnabled() {
		int n = topPanel.getComponentCount();
		for (int i = 0; i < n; i++) {
			if (toolBar == topPanel.getComponent(i))
				return true;
		}
		return false;
	}

	public void enableBottomBar(boolean b) {
		if (moviePanel != null)
			remove(moviePanel);
		if (runPanel != null)
			remove(runPanel);
		if (b) {
			if (model.getRecorderDisabled()) {
				if (runPanel == null)
					createRunPanel();
				add(runPanel, BorderLayout.SOUTH);
			}
			else {
				add(moviePanel, BorderLayout.SOUTH);
			}
		}
		else {
			if (model.getRecorderDisabled()) {
				if (runPanel == null)
					createRunPanel();
				remove(runPanel);
			}
			else {
				remove(moviePanel);
			}
		}
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				validate();
			}
		});
		view.updateSize();
	}

	public boolean isBottomBarEnabled() {
		int n = getComponentCount();
		for (int i = 0; i < n; i++) {
			if (moviePanel == getComponent(i) || runPanel == getComponent(i))
				return true;
		}
		return false;
	}

	private void createMenuBar() {
		menuBar = new JMenuBar();
		if (!asApplet)
			menuBar.add(createFileMenu());
		menuBar.add(createEditMenu());
		menuBar.add(createViewMenu());
		menuBar.add(createModelMenu());
		menuBar.add(createOptionMenu());
	}

	private JMenu createModelMenu() {
		String s = getInternationalText("Template");
		JMenu menu = new JMenu(s != null ? s : "Template");
		s = getInternationalText("Crystals");
		JMenu subMenu = new JMenu(s != null ? s : "Crystals");
		menu.add(subMenu);
		try {
			CrystalReader reader = new CrystalReader(this);
			reader.read(MolecularContainer.class.getResource("resources/crystal.dat"), subMenu);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return menu;
	}

	private JMenu createFileMenu() {

		String s = getInternationalText("File");
		JMenu menu = new JMenu(s != null ? s : "File");

		JMenuItem mi = new JMenuItem(view.getActionMap().get("open model"));
		s = getInternationalText("OpenModel");
		mi.setText((s != null ? s : mi.getText()) + "...");
		menu.add(mi);

		mi = new JMenuItem(view.getActionMap().get("save model"));
		s = getInternationalText("SaveModel");
		mi.setText((s != null ? s : mi.getText()) + "...");
		menu.add(mi);

		Action a = new ScreenshotSaver(fileChooser, view, false);
		a.putValue(NAME, "Save a Screenshot of Model");
		mi = new JMenuItem(a);
		s = getInternationalText("Screenshot");
		if (s != null)
			a.putValue("i18n", s);
		mi.setText((s != null ? s : mi.getText()) + "...");
		menu.add(mi);

		s = getInternationalText("SaveImageStream");
		mi = new JMenuItem((s != null ? s : "Save Image Stream") + "...", IconPool.getIcon("movie"));
		mi.setMnemonic(KeyEvent.VK_I);
		mi.setToolTipText("Produce a series of images from the simulation");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				produceImageStream();
			}
		});
		menu.add(mi);
		menu.addSeparator();

		a = new ImportStructureAction(this);
		mi = new JMenuItem(a);
		s = getInternationalText("ImportStructure");
		mi.setText((s != null ? s : mi.getText()) + "...");
		menu.add(mi);

		a = new ExportStructureAction(this);
		mi = new JMenuItem(a);
		s = getInternationalText("ExportStructure");
		mi.setText((s != null ? s : mi.getText()) + "...");
		menu.add(mi);
		menu.addSeparator();

		s = getInternationalText("Print");
		mi = new JMenuItem((s != null ? s : "Print Model") + "...");
		mi.setIcon(IconPool.getIcon("printer"));
		mi.setMnemonic(KeyEvent.VK_P);
		mi.setAccelerator(System.getProperty("os.name").startsWith("Mac") ? KeyStroke.getKeyStroke(KeyEvent.VK_P,
				KeyEvent.META_MASK) : KeyStroke.getKeyStroke(KeyEvent.VK_P, KeyEvent.CTRL_MASK));
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				view.print();
			}
		});
		menu.add(mi);

		return menu;

	}

	private JMenu createEditMenu() {

		String s = getInternationalText("Edit");
		JMenu menu = new JMenu(s != null ? s : "Edit");

		s = getInternationalText("Undo");
		JMenuItem mi = new JMenuItem(s != null ? s : "Undo");
		mi.setEnabled(false);
		menu.add(mi);

		s = getInternationalText("Redo");
		mi = new JMenuItem(s != null ? s : "Redo");
		mi.setEnabled(false);
		menu.add(mi);
		menu.addSeparator();

		mi = new JMenuItem(view.getActionMap().get("cut"));
		s = getInternationalText("Cut");
		if (s != null)
			mi.setText(s);
		menu.add(mi);

		mi = new JMenuItem(view.getActionMap().get("copy"));
		s = getInternationalText("Copy");
		if (s != null)
			mi.setText(s);
		menu.add(mi);

		mi = new JMenuItem(view.getActionMap().get("paste"));
		s = getInternationalText("Paste");
		if (s != null)
			mi.setText(s);
		menu.add(mi);

		mi = new JMenuItem(view.getActionMap().get("invert selection"));
		s = getInternationalText("InvertSelection");
		if (s != null)
			mi.setText(s);
		menu.add(mi);
		menu.addSeparator();

		s = getInternationalText("Annotation");
		JMenu subMenu = new JMenu(s != null ? s : "Annotation");
		subMenu.setIcon(IconPool.getIcon("annotation"));
		menu.add(subMenu);
		menu.addSeparator();

		s = getInternationalText("AddTextBox");
		mi = new JRadioButtonMenuItem(s != null ? s : "Add Text Box");
		mi.setToolTipText("Add a text box");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				view.setActionID(DRAW_ID);
				view.setMode(MolecularView.DEFAULT_MODE);
				view.inputTextBox();
			}
		});
		subMenu.add(mi);
		toolBarButtonGroup.add(mi);

		s = getInternationalText("AddLine");
		mi = new JRadioButtonMenuItem(s != null ? s : "Add Line");
		mi.setToolTipText("Draw a line");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				view.setActionID(DRAW_ID);
				view.setMode(MolecularView.LINE_MODE);
			}
		});
		subMenu.add(mi);
		toolBarButtonGroup.add(mi);

		s = getInternationalText("AddRectangle");
		mi = new JRadioButtonMenuItem(s != null ? s : "Add Rectangle");
		mi.setToolTipText("Draw a rectangle");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				view.setActionID(DRAW_ID);
				view.setMode(MolecularView.RECT_MODE);
			}
		});
		subMenu.add(mi);
		toolBarButtonGroup.add(mi);

		s = getInternationalText("AddEllipse");
		mi = new JRadioButtonMenuItem(s != null ? s : "Add Ellipse");
		mi.setToolTipText("Draw an ellipse");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				view.setActionID(DRAW_ID);
				view.setMode(MolecularView.ELLIPSE_MODE);
			}
		});
		subMenu.add(mi);
		toolBarButtonGroup.add(mi);

		mi = new JMenuItem(new ResizeAction(this));
		s = getInternationalText("ResizeContainer");
		mi.setText((s != null ? s : mi.getText()) + "...");
		menu.add(mi);

		mi = new JMenuItem(new ChangeTimeStepAction(this));
		s = getInternationalText("ChangeTimeStep");
		mi.setText((s != null ? s : mi.getText()) + "...");
		menu.add(mi);

		return menu;

	}

	private JMenu createViewMenu() {

		String s = getInternationalText("View");
		JMenu menu = new JMenu(s != null ? s : "View");

		s = getInternationalText("NavigationMode");
		final JMenuItem miNavigation = new JCheckBoxMenuItem(s != null ? s : "Navigation Mode", new ImageIcon(
				MolecularContainer.class.getResource("resources/Immersive.gif")));
		miNavigation.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				view.getViewer().setNavigationMode(e.getStateChange() == ItemEvent.SELECTED);
				if (view.getViewer().getNavigationMode()) {
					if (!view.getViewer().getPerspectiveDepth()) {
						JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(MolecularContainer.this),
								"Perspective depth is required for the navigation mode. It will be turned on.");
						view.getViewer().setPerspectiveDepth(true);
					}
				}
				else {
					view.setCameraAtom(-1);
				}
				view.repaint();
			}
		});
		menu.add(miNavigation);

		s = getInternationalText("DetachCameraFromMovingObject");
		final JMenuItem miDetach = new JMenuItem(s != null ? s : "Detach Camera from Moving Object", new ImageIcon(
				MolecularContainer.class.getResource("resources/DetachCamera.gif")));
		miDetach.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				view.setCameraAtom(-1);
				// view.getViewer().homePosition();
				view.refresh();
			}
		});
		menu.add(miDetach);

		s = getInternationalText("ZDepthMagnification");
		final JMenu zDepthMenu = new JMenu(s != null ? s : "Z-Depth Magnification");
		zDepthMenu.setIcon(new ImageIcon(MolecularContainer.class.getResource("resources/ZDepth.gif")));
		menu.add(zDepthMenu);
		menu.addSeparator();

		ButtonGroup bg = new ButtonGroup();

		final JMenuItem miOne = new JRadioButtonMenuItem("1x");
		miOne.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					view.getViewer().setZDepthMagnification(1);
					view.repaint();
				}
			}
		});
		zDepthMenu.add(miOne);
		bg.add(miOne);

		final JMenuItem miFive = new JRadioButtonMenuItem("5x");
		miFive.setSelected(true);
		miFive.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					view.getViewer().setZDepthMagnification(5);
					view.repaint();
				}
			}
		});
		zDepthMenu.add(miFive);
		bg.add(miFive);

		final JMenuItem miTen = new JRadioButtonMenuItem("10x");
		miTen.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					view.getViewer().setZDepthMagnification(10);
					view.repaint();
				}
			}
		});
		zDepthMenu.add(miTen);
		bg.add(miTen);

		s = getInternationalText("Spin");
		final JMenuItem miSpin = new JCheckBoxMenuItem(s != null ? s : "Spin", IconPool.getIcon("spin"));
		miSpin.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				view.setSpinOn(e.getStateChange() == ItemEvent.SELECTED);
			}
		});
		menu.add(miSpin);

		s = getInternationalText("ShowGlassSimulationBox");
		final JMenuItem miGlassBox = new JCheckBoxMenuItem(s != null ? s : "Show Glass Simulation Box", new ImageIcon(
				MolecularContainer.class.getResource("resources/GlassBox.gif")));
		miGlassBox.setSelected(true);
		miGlassBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				view.setShowGlassSimulationBox(e.getStateChange() == ItemEvent.SELECTED);
			}
		});
		menu.add(miGlassBox);

		s = getInternationalText("FitGlassSimulationBoxIntoWindow");
		JMenuItem mi = new JMenuItem(s != null ? s : "Fit Glass Simulation Box Into Window");
		mi.setIcon(new ImageIcon(MolecularContainer.class.getResource("resources/FitIntoWindow.gif")));
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				view.fitIntoWindow();
				boxRotated = true;
			}
		});
		menu.add(mi);

		s = getInternationalText("ShowAllAtoms");
		mi = new JMenuItem(s != null ? s : "Show All Atoms", new ImageIcon(MolecularContainer.class
				.getResource("resources/ShowAllAtoms.gif")));
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				view.showAllAtoms();
			}
		});
		menu.add(mi);

		s = getInternationalText("ClearMeasurements");
		mi = new JMenuItem(s != null ? s : "Clear Measurements");
		mi.setIcon(IconPool.getIcon("erase"));
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				view.getViewer().clearMeasurements();
			}
		});
		menu.add(mi);

		menu.addMenuListener(new MenuListener() {
			public void menuSelected(MenuEvent e) {
				miDetach.setEnabled(view.getCameraAtom() >= 0);
				ModelerUtilities.setWithoutNotifyingListeners(miGlassBox, view.getShowGlassSimulationBox());
				ModelerUtilities.setWithoutNotifyingListeners(miSpin, view.isSpinOn());
				boolean b = view.getViewer().getNavigationMode();
				ModelerUtilities.setWithoutNotifyingListeners(miNavigation, b);
				zDepthMenu.setEnabled(b);
				if (b) {
					switch (view.getViewer().getZDepthMagnification()) {
					case 1:
						ModelerUtilities.setWithoutNotifyingListeners(miOne, true);
						break;
					case 5:
						ModelerUtilities.setWithoutNotifyingListeners(miFive, true);
						break;
					case 10:
						ModelerUtilities.setWithoutNotifyingListeners(miTen, true);
						break;
					}
				}
			}

			public void menuCanceled(MenuEvent e) {
			}

			public void menuDeselected(MenuEvent e) {
			}
		});

		return menu;

	}

	private JMenu createOptionMenu() {

		String s = getInternationalText("DisableRecorder");
		final JMenuItem disableRecorderItem = new JCheckBoxMenuItem(s != null ? s : "Disable Recorder");

		s = getInternationalText("Option");
		JMenu menu = new JMenu(s != null ? s : "Options");
		menu.addMenuListener(new MenuListener() {
			public void menuSelected(MenuEvent e) {
				ModelerUtilities.setWithoutNotifyingListeners(disableRecorderItem, model.getRecorderDisabled());
			}

			public void menuCanceled(MenuEvent e) {
			}

			public void menuDeselected(MenuEvent e) {
			}
		});

		disableRecorderItem.setMnemonic(KeyEvent.VK_D);
		disableRecorderItem.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				enableRecorder(e.getStateChange() == ItemEvent.DESELECTED);
				repaint();
				notifyChange();
			}
		});
		menu.add(disableRecorderItem);
		menu.addSeparator();

		JMenuItem mi = new JMenuItem(view.getActionMap().get("properties"));
		s = MolecularContainer.getInternationalText("Properties");
		mi.setText((s != null ? s : mi.getText()) + "...");
		mi.setIcon(null);
		menu.add(mi);

		mi = new JMenuItem(view.getActionMap().get("view options"));
		s = getInternationalText("ViewOption");
		mi.setText((s != null ? s : mi.getText()) + "...");
		mi.setIcon(null);
		menu.add(mi);

		mi = new JMenuItem(view.getActionMap().get("task manager"));
		s = getInternationalText("TaskManager");
		mi.setText((s != null ? s : mi.getText()) + "...");
		mi.setIcon(null);
		menu.add(mi);

		s = getInternationalText("Snapshot");
		snapshotMenuItem = new JMenuItem((s != null ? s : "Take a Snapshot") + "...");
		snapshotMenuItem.setToolTipText("Create a snapshot image and put it into the Snapshot Gallery");
		menu.add(snapshotMenuItem);
		menu.addSeparator();

		s = MolecularContainer.getInternationalText("RunEnergyMinimization");
		final JMenu minimizationMenu = new JMenu(s != null ? s : "Run Energy Minimization");
		menu.add(minimizationMenu);

		s = MolecularContainer.getInternationalText("ForSelectedAtoms");
		final JMenuItem miMinimizeSelected = new JMenuItem((s != null ? s : "For Selected Atoms") + "...");
		miMinimizeSelected.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (minimizerDialog == null)
					minimizerDialog = new MinimizerDialog(model);
				minimizerDialog.setLocationRelativeTo(MolecularContainer.this);
				minimizerDialog.setVisible(true);
				minimizerDialog.runMinimizer(view.getSelectionSet());
			}
		});
		minimizationMenu.add(miMinimizeSelected);

		s = MolecularContainer.getInternationalText("ForUnselectedAtoms");
		final JMenuItem miMinimizeUnselected = new JMenuItem((s != null ? s : "For Unselected Atoms") + "...");
		miMinimizeUnselected.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (minimizerDialog == null)
					minimizerDialog = new MinimizerDialog(model);
				minimizerDialog.setLocationRelativeTo(MolecularContainer.this);
				minimizerDialog.setVisible(true);
				int n = view.getModel().getAtomCount();
				BitSet bs = new BitSet(n);
				bs.set(0, n);
				bs.andNot(view.getSelectionSet());
				minimizerDialog.runMinimizer(bs);
			}
		});
		minimizationMenu.add(miMinimizeUnselected);

		s = MolecularContainer.getInternationalText("ForAllAtoms");
		mi = new JMenuItem((s != null ? s : "For All Atoms") + "...");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (minimizerDialog == null)
					minimizerDialog = new MinimizerDialog(model);
				minimizerDialog.setLocationRelativeTo(MolecularContainer.this);
				minimizerDialog.setVisible(true);
				minimizerDialog.runMinimizer();
			}
		});
		minimizationMenu.add(mi);

		s = getInternationalText("ToolBox");
		JMenu subMenu = new JMenu(s != null ? s : "Toolbox");
		menu.add(subMenu);

		s = getInternationalText("HeatCool");
		final JMenuItem miEnergizer = new JCheckBoxMenuItem(s != null ? s : "Quick Heat and Cool", IconPool
				.getIcon("thermometer"));
		miEnergizer.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				view.setShowEnergizer(e.getStateChange() == ItemEvent.SELECTED);
			}
		});
		subMenu.add(miEnergizer);

		s = getInternationalText("HeatBath");
		mi = new JMenuItem((s != null ? s : "Heat Bath") + "...", IconPool.getIcon("heat bath"));
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (ModelerUtilities.stopFiring(e))
					return;
				new HeatBathPanel(model).createDialog().setVisible(true);
			}
		});
		subMenu.add(mi);

		mi = new JMenuItem(view.getActionMap().get("show energy"));
		s = getInternationalText("EnergyTimeSeries");
		mi.setText((s != null ? s : mi.getText()) + "...");
		subMenu.add(mi);

		menu.addMenuListener(new MenuListener() {
			public void menuSelected(MenuEvent e) {
				ModelerUtilities.setWithoutNotifyingListeners(miEnergizer, view.getShowEnergizer());
				minimizationMenu.setEnabled(!view.getModel().isRunning());
				miMinimizeSelected.setEnabled(view.getSelectionSet().cardinality() > 0);
				miMinimizeUnselected.setEnabled(view.getSelectionSet().cardinality() < view.getModel().getAtomCount());
			}

			public void menuCanceled(MenuEvent e) {
			}

			public void menuDeselected(MenuEvent e) {
			}
		});

		return menu;

	}

	private void setToolBarEnabled(final boolean b) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				int n = toolBar.getComponentCount();
				for (int i = 1; i < n; i++)
					toolBar.getComponent(i).setEnabled(b);
			}
		});
	}

	void resetToolBar() {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				rotateButton.setSelected(true);
			}
		});
	}

	private void createToolBar() {

		Dimension buttonDimension = ModelerUtilities.getSystemToolBarButtonSize();

		toolBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		toolBar.setBorder(BorderFactory.createEmptyBorder(3, 0, 3, 0));

		// tool for setting view angles

		ImageIcon[] icons = new ImageIcon[6];
		Class clazz = MolecularContainer.class;
		icons[0] = new ImageIcon(clazz.getResource("resources/FrontView.gif"));
		icons[0].setDescription("Present front view");
		icons[1] = new ImageIcon(clazz.getResource("resources/RearView.gif"));
		icons[1].setDescription("Present rear view");
		icons[2] = new ImageIcon(clazz.getResource("resources/TopView.gif"));
		icons[2].setDescription("Present top view");
		icons[3] = new ImageIcon(clazz.getResource("resources/BottomView.gif"));
		icons[3].setDescription("Present bottom view");
		icons[4] = new ImageIcon(clazz.getResource("resources/RightView.gif"));
		icons[4].setDescription("Present right view");
		icons[5] = new ImageIcon(clazz.getResource("resources/LeftView.gif"));
		icons[5].setDescription("Present left view");

		JComboBox comboBox = new JComboBox(icons);
		int cbWidth = icons[0].getIconWidth() + buttonDimension.width;
		cbWidth += System.getProperty("os.name").startsWith("Mac") ? 14 : 4;
		Dimension dim = new Dimension(cbWidth, buttonDimension.height);
		comboBox.setPreferredSize(dim);
		comboBox.setMaximumSize(dim);
		comboBox.setRenderer(new ComboBoxRenderer.IconRenderer());
		comboBox.setToolTipText("Set view angle");
		comboBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JComboBox cb = (JComboBox) e.getSource();
				view.setViewAngle((byte) (cb.getSelectedIndex() + MolecularView.FRONT_VIEW));
				boxRotated = true;
			}
		});
		toolBar.add(comboBox);

		// rotation tool

		rotateButton = new JToggleButton(new ImageIcon(MolecularContainer.class
				.getResource("resources/DefaultMode.gif")));
		rotateButton.setSelected(true);
		rotateButton.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					Object o = rotateButton.getClientProperty("action_id");
					if (o instanceof Byte) {
						view.setActionID(((Byte) o).byteValue());
					}
					else {
						view.setActionID(DEFA_ID);
					}
				}
			}
		});
		rotateButton.setPreferredSize(buttonDimension);
		rotateButton.setToolTipText("Default mode");
		toolBar.add(rotateButton);
		toolBarButtonGroup.add(rotateButton);

		// pan tool

		AbstractButton button = new JToggleButton(new ImageIcon(MolecularContainer.class
				.getResource("resources/Pan.gif")));
		button.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					view.setMode(MolecularView.DEFAULT_MODE);
					view.setActionID(PANN_ID);
				}
			}
		});
		button.setPreferredSize(buttonDimension);
		button.setToolTipText("Pan");
		toolBar.add(button);
		toolBarButtonGroup.add(button);

		// selection tool

		MouseAdapter selectMouseAdapter = new MouseAdapter() {
			private boolean popupTrigger;

			public void mousePressed(MouseEvent e) {
				popupTrigger = e.isPopupTrigger();
			}

			public void mouseReleased(MouseEvent e) {
				if (popupTrigger || e.isPopupTrigger()) {
					if (selectToolPopupMenu == null) {
						selectToolPopupMenu = new SelectToolPopupMenu(view);
					}
					selectToolPopupMenu.setStateID(view.getActionID());
					selectToolPopupMenu.show((AbstractButton) e.getSource(), 5, 5);
				}
			}
		};
		button = new RightClickToggleButton(new ImageIcon(MolecularContainer.class
				.getResource("resources/selectrect.gif")), buttonDimension);
		button.addMouseListener(selectMouseAdapter);
		button.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					AbstractButton b = (AbstractButton) e.getSource();
					Object o = b.getClientProperty("action_id");
					if (o instanceof Byte) {
						view.setActionID(((Byte) o).byteValue());
					}
					else {
						view.setActionID(SLRT_ID);
					}
				}
			}
		});
		button.setToolTipText("Select atoms in an area.");
		button.setPreferredSize(buttonDimension);
		toolBar.add(button);
		toolBarButtonGroup.add(button);

		// adding atom tools

		MouseAdapter dropAtomButtonMouseAdapter = new MouseAdapter() {
			private boolean popupTrigger;

			public void mousePressed(MouseEvent e) {
				popupTrigger = e.isPopupTrigger();
			}

			public void mouseReleased(MouseEvent e) {
				if (popupTrigger || e.isPopupTrigger()) {
					if (dropToolPopupMenu == null) {
						dropToolPopupMenu = new DropToolPopupMenu(view);
					}
					dropToolPopupMenu.setStateID(view.getActionID());
					dropToolPopupMenu.show((AbstractButton) e.getSource(), 5, 5);
				}
				else {
					AbstractButton b = (AbstractButton) e.getSource();
					b.doClick();
					if (view.isAddingAtomMode()) {
						if (elementSelectionPanel == null) {
							elementSelectionPanel = new ElementSelectionPanel(view);
						}
						elementSelectionPanel.showPopup(b);
					}
					else if (view.isAddingMoleculeMode()) {
						if (moleculeSelectionPanel == null) {
							moleculeSelectionPanel = new MoleculeSelectionPanel(view);
						}
						moleculeSelectionPanel.showPopup(b);
					}
				}
			}
		};
		final ModelAction dropAtomAction = new DefaultModelAction(model);
		dropAtomAction.setExecutable(new Executable() {
			public void execute() {
				view.setMode(MolecularView.DEFAULT_MODE);
				Object o = dropAtomAction.getValue("action_id");
				if (o instanceof Byte) {
					view.setActionID(((Byte) o).byteValue());
				}
				else {
					view.setActionID(XADD_ID);
				}
			}
		});
		button = new RightClickToggleButton(dropAtomAction, buttonDimension);
		button.addMouseListener(dropAtomButtonMouseAdapter);
		button.setIcon(new ImageIcon(MolecularContainer.class.getResource("resources/AddAtomOnXPlane.gif")));
		button.setToolTipText("Drop an atom on a plane perpendicular to the x axis");
		button.setPreferredSize(buttonDimension);
		toolBar.add(button);
		toolBarButtonGroup.add(button);

		// adding rectangle tools

		MouseAdapter dropRectButtonMouseAdapter = new MouseAdapter() {
			private boolean popupTrigger;

			public void mousePressed(MouseEvent e) {
				popupTrigger = e.isPopupTrigger();
			}

			public void mouseReleased(MouseEvent e) {
				if (popupTrigger || e.isPopupTrigger()) {
					if (dropObstaclePopupMenu == null) {
						dropObstaclePopupMenu = new DropObstaclePopupMenu(view);
					}
					dropObstaclePopupMenu.setStateID(view.getActionID());
					dropObstaclePopupMenu.show((AbstractButton) e.getSource(), 5, 5);
				}
			}
		};
		final ModelAction dropRectAction = new DefaultModelAction(model);
		dropRectAction.setExecutable(new Executable() {
			public void execute() {
				view.setMode(MolecularView.DEFAULT_MODE);
				Object o = dropRectAction.getValue("action_id");
				if (o instanceof Byte) {
					view.setActionID(((Byte) o).byteValue());
				}
				else {
					view.setActionID(XREC_ID);
				}
			}
		});
		button = new RightClickToggleButton(dropRectAction, buttonDimension);
		button.addMouseListener(dropRectButtonMouseAdapter);
		button.setIcon(new ImageIcon(MolecularContainer.class.getResource("resources/AddRectangleOnXPlane.gif")));
		button.setToolTipText("Draw a cuboid obstacle on a plane perpendicular to the x axis");
		button.setPreferredSize(buttonDimension);
		toolBar.add(button);
		toolBarButtonGroup.add(button);

		// rotation tool

		MouseAdapter rotateButtonMouseAdapter = new MouseAdapter() {
			private boolean popupTrigger;

			public void mousePressed(MouseEvent e) {
				popupTrigger = e.isPopupTrigger();
			}

			public void mouseReleased(MouseEvent e) {
				if (popupTrigger || e.isPopupTrigger()) {
					if (rotateToolPopupMenu == null) {
						rotateToolPopupMenu = new RotateToolPopupMenu(view);
					}
					rotateToolPopupMenu.setStateID(view.getActionID());
					rotateToolPopupMenu.show((AbstractButton) e.getSource(), 5, 5);
				}
			}
		};
		final ModelAction rotateAtomsAction = new DefaultModelAction(model);
		rotateAtomsAction.setExecutable(new Executable() {
			public void execute() {
				view.setMode(MolecularView.DEFAULT_MODE);
				Object o = rotateAtomsAction.getValue("action_id");
				if (o instanceof Byte) {
					view.setActionID(((Byte) o).byteValue());
				}
				else {
					view.setActionID(ROTA_ID);
				}
			}
		});
		button = new RightClickToggleButton(rotateAtomsAction, buttonDimension);
		button.setIcon(new ImageIcon(MolecularContainer.class.getResource("resources/RotateAtoms.gif")));
		button.setToolTipText("Rotate a molecule or a group of atoms");
		button.addMouseListener(rotateButtonMouseAdapter);
		button.setPreferredSize(buttonDimension);
		toolBar.add(button);
		toolBarButtonGroup.add(button);

		// deletion tool

		MouseAdapter deleteButtonMouseAdapter = new MouseAdapter() {
			private boolean popupTrigger;

			public void mousePressed(MouseEvent e) {
				popupTrigger = e.isPopupTrigger();
			}

			public void mouseReleased(MouseEvent e) {
				if (popupTrigger || e.isPopupTrigger()) {
					if (deleteToolPopupMenu == null) {
						deleteToolPopupMenu = new DeleteToolPopupMenu(view);
					}
					deleteToolPopupMenu.setStateID(view.getActionID());
					deleteToolPopupMenu.show((AbstractButton) e.getSource(), 5, 5);
				}
			}
		};
		final ModelAction removeAtomsAction = new DefaultModelAction(model);
		removeAtomsAction.setExecutable(new Executable() {
			public void execute() {
				view.setMode(MolecularView.DEFAULT_MODE);
				Object o = removeAtomsAction.getValue("action_id");
				if (o instanceof Byte) {
					view.setActionID(((Byte) o).byteValue());
				}
				else {
					view.setActionID(DELR_ID);
				}
			}
		});
		button = new RightClickToggleButton(removeAtomsAction, buttonDimension);
		button.setIcon(IconPool.getIcon("remove rect"));
		button.setToolTipText("Delete atoms intersected by the selected area");
		button.addMouseListener(deleteButtonMouseAdapter);
		button.setPreferredSize(buttonDimension);
		toolBar.add(button);
		toolBarButtonGroup.add(button);

		// bonding tools

		MouseAdapter bondButtonMouseAdapter = new MouseAdapter() {
			private boolean popupTrigger;

			public void mousePressed(MouseEvent e) {
				popupTrigger = e.isPopupTrigger();
			}

			public void mouseReleased(MouseEvent e) {
				if (popupTrigger || e.isPopupTrigger()) {
					if (bondToolPopupMenu == null) {
						bondToolPopupMenu = new BondToolPopupMenu(view);
					}
					bondToolPopupMenu.setStateID(view.getActionID());
					bondToolPopupMenu.show((AbstractButton) e.getSource(), 5, 5);
				}
			}
		};
		final ModelAction bondAction = new DefaultModelAction(model);
		bondAction.setExecutable(new Executable() {
			public void execute() {
				view.setMode(MolecularView.DEFAULT_MODE);
				Object o = bondAction.getValue("action_id");
				if (o instanceof Byte) {
					view.setActionID(((Byte) o).byteValue());
				}
				else {
					view.setActionID(RBND_ID);
				}
			}
		});
		button = new RightClickToggleButton(bondAction, buttonDimension);
		button.setIcon(IconPool.getIcon("radial bond"));
		button.setToolTipText("Build a radial bond between two atoms");
		button.addMouseListener(bondButtonMouseAdapter);
		button.setPreferredSize(buttonDimension);
		toolBar.add(button);
		toolBarButtonGroup.add(button);

		// miscellaneous tools

		MouseAdapter miscButtonMouseAdapter = new MouseAdapter() {
			private boolean popupTrigger;

			public void mousePressed(MouseEvent e) {
				popupTrigger = e.isPopupTrigger();
			}

			public void mouseReleased(MouseEvent e) {
				if (popupTrigger || e.isPopupTrigger()) {
					if (miscToolPopupMenu == null) {
						miscToolPopupMenu = new MiscToolPopupMenu(view);
					}
					miscToolPopupMenu.setStateID(view.getActionID());
					miscToolPopupMenu.show((AbstractButton) e.getSource(), 5, 5);
				}
			}
		};
		final ModelAction miscAction = new DefaultModelAction(model);
		miscAction.setExecutable(new Executable() {
			public void execute() {
				view.setMode(MolecularView.DEFAULT_MODE);
				Object o = miscAction.getValue("action_id");
				if (o instanceof Byte) {
					view.setActionID(((Byte) o).byteValue());
				}
				else {
					view.setActionID(EXOB_ID);
				}
			}
		});
		button = new RightClickToggleButton(miscAction, buttonDimension);
		button.setIcon(new ImageIcon(MolecularContainer.class.getResource("resources/Extrude.gif")));
		button.setToolTipText("Extrude an obstacle by pushing/pulling its faces");
		button.addMouseListener(miscButtonMouseAdapter);
		button.setPreferredSize(buttonDimension);
		toolBar.add(button);
		toolBarButtonGroup.add(button);

	}

	void createMoviePanel() {

		moviePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 4));
		moviePanel.setBorder(BorderFactory.createEmptyBorder(1, 3, 1, 1));

		int m = System.getProperty("os.name").startsWith("Mac") ? 2 : 0;
		Insets margin = new Insets(m, m, m, m);
		Dimension dim = new Dimension(20, 20);

		MovieSlider ms = model.getMovie().getMovieSlider();
		ms.setPreferredSize(new Dimension(130, 20));
		ms.setBorder(BorderFactory.createEmptyBorder());
		moviePanel.add(ms);

		JButton button = new JButton(view.getActionMap().get("reset"));
		button.setText(null);
		button.setHorizontalAlignment(SwingConstants.CENTER);
		button.setMargin(margin);
		button.setMaximumSize(dim);
		button.setMinimumSize(dim);
		button.setPreferredSize(dim);
		button.setFocusPainted(false);
		moviePanel.add(button);

		button = new JButton(model.getMovie().rewindMovie);
		button.setText(null);
		button.setHorizontalAlignment(SwingConstants.CENTER);
		button.setMargin(margin);
		button.setMaximumSize(dim);
		button.setMinimumSize(dim);
		button.setPreferredSize(dim);
		button.setFocusPainted(false);
		moviePanel.add(button);

		button = new JButton(model.getMovie().stepBackMovie);
		button.setText(null);
		button.setHorizontalAlignment(SwingConstants.CENTER);
		button.setMargin(margin);
		button.setMaximumSize(dim);
		button.setMinimumSize(dim);
		button.setPreferredSize(dim);
		button.setFocusPainted(false);
		moviePanel.add(button);

		button = new JButton(view.getActionMap().get("stop"));
		button.setText(null);
		button.setHorizontalAlignment(SwingConstants.CENTER);
		button.setMargin(margin);
		button.setMaximumSize(dim);
		button.setMinimumSize(dim);
		button.setPreferredSize(dim);
		button.setFocusPainted(false);
		moviePanel.add(button);

		button = new JButton(model.getMovie().stepForwardMovie);
		button.setText(null);
		button.setHorizontalAlignment(SwingConstants.CENTER);
		button.setMargin(margin);
		button.setMaximumSize(dim);
		button.setMinimumSize(dim);
		button.setPreferredSize(dim);
		button.setFocusPainted(false);
		moviePanel.add(button);

		button = new JButton(view.getActionMap().get("run"));
		button.setText(null);
		button.setHorizontalAlignment(SwingConstants.CENTER);
		button.setMargin(margin);
		button.setMaximumSize(dim);
		button.setMinimumSize(dim);
		button.setPreferredSize(dim);
		button.setFocusPainted(false);
		moviePanel.add(button);

		moviePanel.addMouseListener(new MouseAdapter() {
			private boolean popupTrigger;

			public void mousePressed(MouseEvent e) {
				popupTrigger = e.isPopupTrigger();
			}

			public void mouseReleased(MouseEvent e) {
				if (popupTrigger || e.isPopupTrigger()) {
					if (defaultPopupMenu == null)
						createDefaultPopupMenu();
					defaultPopupMenu.show(moviePanel, e.getX(), e.getY());
				}
			}
		});

	}

	private void createRunPanel() {

		runPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));

		JButton button = new JButton(view.getActionMap().get("run"));
		String s = getInternationalText("Run");
		if (s != null) {
			button.setText(s);
			button.setToolTipText(s);
		}
		runPanel.add(button);

		button = new JButton(view.getActionMap().get("stop"));
		s = getInternationalText("Stop");
		if (s != null) {
			button.setText(s);
			button.setToolTipText(s);
		}
		runPanel.add(button);

		button = new JButton(view.getActionMap().get("reset"));
		s = getInternationalText("Reset");
		if (s != null) {
			button.setText(s);
			button.setToolTipText(s);
		}
		runPanel.add(button);

		runPanel.addMouseListener(new MouseAdapter() {
			private boolean popupTrigger;

			public void mousePressed(MouseEvent e) {
				popupTrigger = e.isPopupTrigger();
			}

			public void mouseReleased(MouseEvent e) {
				if (popupTrigger || e.isPopupTrigger()) {
					if (defaultPopupMenu == null)
						createDefaultPopupMenu();
					defaultPopupMenu.show(runPanel, e.getX(), e.getY());
				}
			}
		});

	}

	private void createDefaultPopupMenu() {

		defaultPopupMenu = new JPopupMenu();

		String s = MolecularContainer.getInternationalText("ShowMenuBar");
		final JMenuItem miMenuBar = new JCheckBoxMenuItem(s != null ? s : "Show Menu Bar");
		miMenuBar.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				enableMenuBar(e.getStateChange() == ItemEvent.SELECTED);
				notifyChange();
			}
		});
		defaultPopupMenu.add(miMenuBar);

		s = MolecularContainer.getInternationalText("ShowToolBar");
		final JMenuItem miToolBar = new JCheckBoxMenuItem(s != null ? s : "Show Tool Bar");
		miToolBar.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				enableToolBar(e.getStateChange() == ItemEvent.SELECTED);
				notifyChange();
			}
		});
		defaultPopupMenu.add(miToolBar);

		defaultPopupMenu.pack();

		defaultPopupMenu.addPopupMenuListener(new PopupMenuListener() {
			public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
				ModelerUtilities.setWithoutNotifyingListeners(miMenuBar, isMenuBarEnabled());
				ModelerUtilities.setWithoutNotifyingListeners(miToolBar, isToolBarEnabled());
			}

			public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
			}

			public void popupMenuCanceled(PopupMenuEvent e) {
			}
		});

	}

	public int enableRecorder(final boolean b) {
		if (!EventQueue.isDispatchThread())
			throw new RuntimeException("must be called in the EDT thread.");
		model.stopImmediately();
		if (b) {
			/* IMPORTANT!!! we should turn the recorder on */
			if (!model.hasEmbeddedMovie()) {
				model.activateEmbeddedMovie(true);
				/*
				 * activate embedded movie will automatically turn the recorder on at the end of array initialization
				 */
			}
			else {
				/*
				 * if there is no embedded movie, we should compulsorily turn on the recorder mode, otherwise the
				 * recording process will not be added to the task pool upon job initialization.
				 */
				model.setRecorderDisabled(false);
			}
			if (runPanel != null)
				remove(runPanel);
			if (bottomBarShown) {
				add(moviePanel, BorderLayout.SOUTH);
				repaint();
				getParent().validate();
			}
		}
		else {
			model.activateEmbeddedMovie(false);
			remove(moviePanel);
			if (bottomBarShown) {
				if (runPanel == null)
					createRunPanel();
				add(runPanel, BorderLayout.SOUTH);
				repaint();
				getParent().validate();
			}
		}
		return JOptionPane.YES_OPTION;
	}

	private void showTaskManager() {
		if (!EventQueue.isDispatchThread())
			throw new RuntimeException("must be called in event thread.");
		Job job = model.getJob();
		if (job != null) {
			job.show(getView());
		}
		else {
			JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(MolecularContainer.this),
					"There is no task yet. Please run the model.", "No task assigned", JOptionPane.WARNING_MESSAGE);
		}
	}

	public Job getJob() {
		return model.getJob();
	}

	public float getModelTime() {
		return model.getModelTime();
	}

	public FloatQueue getModelTimeQueue() {
		return model.getModelTimeQueue();
	}

	public Movie getMovie() {
		return model.getMovie();
	}

	public HomoQueueGroup getMovieQueueGroup() {
		return model.getMovieQueueGroup();
	}

	public DataQueue getQueue(String name) {
		return model.getQueue(name);
	}

	public boolean getRecorderDisabled() {
		return model.getRecorderDisabled();
	}

	// TODO
	public void stopInput() {
	}

	public Object getProperty(Object key) {
		return model.getProperty(key);
	}

	public void putProperty(Object key, Object value) {
		model.putProperty(key, value);
	}

	private abstract class DefaultAction extends AbstractAction {
		public String toString() {
			return (String) getValue(SHORT_DESCRIPTION);
		}
	}

}