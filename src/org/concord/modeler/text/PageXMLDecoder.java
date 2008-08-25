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
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Toolkit;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentListener;
import javax.swing.event.UndoableEditListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Element;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.text.StyledDocument;
import javax.swing.text.html.HTML;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import org.concord.functiongraph.DataSource;
import org.concord.modeler.ActivityButton;
import org.concord.modeler.AudioPlayer;
import org.concord.modeler.ConnectionManager;
import org.concord.modeler.DisasterHandler;
import org.concord.modeler.HistoryManager;
import org.concord.modeler.ImageQuestion;
import org.concord.modeler.InstancePool;
import org.concord.modeler.ModelCanvas;
import org.concord.modeler.ModelCommunicator;
import org.concord.modeler.Modeler;
import org.concord.modeler.ModelerUtilities;
import org.concord.modeler.PageApplet;
import org.concord.modeler.PageBarGraph;
import org.concord.modeler.PageButton;
import org.concord.modeler.PageCheckBox;
import org.concord.modeler.PageComboBox;
import org.concord.modeler.PageDiffractionInstrument;
import org.concord.modeler.PageDNAScroller;
import org.concord.modeler.PageElectronicStructureViewer;
import org.concord.modeler.PageFeedbackArea;
import org.concord.modeler.PageFunctionGraph;
import org.concord.modeler.PageJContainer;
import org.concord.modeler.PageMd3d;
import org.concord.modeler.PageMolecularViewer;
import org.concord.modeler.PageMultipleChoice;
import org.concord.modeler.PageNumericBox;
import org.concord.modeler.PagePeriodicTable;
import org.concord.modeler.PagePhotonSpectrometer;
import org.concord.modeler.PagePotentialHill;
import org.concord.modeler.PagePotentialWell;
import org.concord.modeler.PageRadioButton;
import org.concord.modeler.PageScriptConsole;
import org.concord.modeler.PageSlider;
import org.concord.modeler.PageSpinner;
import org.concord.modeler.PageTable;
import org.concord.modeler.PageTextArea;
import org.concord.modeler.PageTextBox;
import org.concord.modeler.PageTextField;
import org.concord.modeler.PageXYGraph;
import org.concord.modeler.QuestionAndAnswer;
import org.concord.modeler.SearchTextField;
import org.concord.modeler.UserData;
import org.concord.modeler.draw.FillMode;
import org.concord.modeler.draw.LineStyle;
import org.concord.modeler.draw.LineSymbols;
import org.concord.modeler.draw.LineWidth;
import org.concord.modeler.draw.StrokeFactory;
import org.concord.modeler.event.PageEvent;
import org.concord.modeler.g2d.Curve;
import org.concord.modeler.util.FileUtilities;
import org.concord.mw2d.ui.AtomContainer;

final class PageXMLDecoder {

	private final static int ARGB_NOT_SET = 0;
	private static DecimalFormat formatter;

	private Page page;
	private DefaultHandler saxHandler;
	private SAXParser saxParser;
	private StringBuffer textBuffer;
	private JProgressBar progressBar;
	private DefaultStyledDocument doc;
	private int indexOfComponent;
	private int indexOfJmol;
	private int indexOfMw3d;
	private int indexOfApplet;
	private int indexOfPlugin;
	private Mw2dConnector mw2dConnector;
	private Mw3dConnector mw3dConnector;
	private JmolConnector jmolConnector;
	private PluginConnector pluginConnector;
	private RadioButtonConnector radioButtonConnector;
	private HTMLComponentConnector htmlComponentConnector;
	private InputStream inputStream;
	private int elementCounter;
	private long loadingTime;
	private Thread resourceLoadingThread;
	private final Object lock = new Object();
	byte threadIndex;

	static {
		formatter = new DecimalFormat("#.#");
		formatter.setMaximumFractionDigits(3);
		formatter.setMaximumIntegerDigits(2);
	}

	public PageXMLDecoder(Page page) {
		if (page == null)
			throw new IllegalArgumentException("no page to decode");
		this.page = page;
		mw2dConnector = new Mw2dConnector(page);
		mw3dConnector = new Mw3dConnector(page);
		jmolConnector = new JmolConnector();
		pluginConnector = new PluginConnector();
		radioButtonConnector = new RadioButtonConnector();
		htmlComponentConnector = new HTMLComponentConnector();
		saxHandler = new XMLHandler();
		try {
			saxParser = SAXParserFactory.newInstance().newSAXParser();
		}
		catch (SAXException e) {
			e.printStackTrace();
		}
		catch (ParserConfigurationException e) {
			e.printStackTrace();
		}
	}

	void destroy() {
		page = null;
		saxHandler = null;
		saxParser = null;
		progressBar = null;
		doc = null;
		mw2dConnector.clear();
		mw3dConnector.clear();
		jmolConnector.clear();
		pluginConnector.clear();
		radioButtonConnector.clear();
		htmlComponentConnector.clear();
		if (resourceLoadingThread != null)
			resourceLoadingThread.interrupt();
		resourceLoadingThread = null;
	}

	/** TODO: stop parsing the current XML document */
	public void stop() {
		if (inputStream != null) {
			try {
				inputStream.close();
			}
			catch (IOException iox) {
			}
		}
	}

	private void resetIndices() {
		indexOfComponent = 0;
		indexOfJmol = 0;
		indexOfMw3d = 0;
		indexOfApplet = 0;
		indexOfPlugin = 0;
	}

	public StyledDocument getDocument() {
		return doc;
	}

	public void setProgressBar(JProgressBar pb) {
		progressBar = pb;
	}

	public JProgressBar getProgressBar() {
		return progressBar;
	}

	public boolean read(String path) throws IOException, SAXException {
		if (path == null)
			throw new IllegalArgumentException("Path cannot be null.");
		if (FileUtilities.isRemote(path)) {
			URL u = null;
			try {
				u = new URL(path);
			}
			catch (MalformedURLException e) {
				e.printStackTrace();
				return false;
			}
			if (ConnectionManager.sharedInstance().isCachingAllowed() && u.getQuery() == null) {
				File file = ConnectionManager.sharedInstance().shouldUpdate(u);
				if (file == null) {
					try {
						file = ConnectionManager.sharedInstance().cache(u);
						if (file == null) {
							if (!path.startsWith("jar:"))
								throw new SocketTimeoutException(path);
						}
					}
					catch (FileNotFoundException e) {
						throw new FileNotFoundException(e.getMessage());
					}
				}
				if (file != null)
					return read(file);
			}
			return read(u);
		}
		return read(new File(path));
	}

	private boolean read(File file) throws IOException, SAXException {
		if (file == null)
			throw new IllegalArgumentException("file cannot be null.");
		inputStream = new FileInputStream(file);
		return read(new BufferedInputStream(inputStream));
	}

	private boolean read(URL url) throws IOException, SAXException {
		if (url == null)
			throw new IllegalArgumentException("URL cannot be null.");
		URLConnection connection = ConnectionManager.getConnection(url);
		if (connection == null)
			return false;
		connection.connect();
		inputStream = connection.getInputStream();
		return read(new BufferedInputStream(inputStream));
	}

	/*
	 * From the Java API: "An implementation of SAXParser is NOT guaranteed to behave as per the specification if it is
	 * used concurrently by two or more threads. It is recommended to have one instance of the SAXParser per thread or
	 * it is up to the application to make sure about the use of SAXParser from more than one thread". If a SAX parser
	 * is reused, the following exception: "org.xml.sax.SAXException: Parser is already in use" will be thrown.
	 */
	private boolean read(InputStream in) throws IOException, SAXException {

		if (in == null)
			return false;

		loadingTime = System.currentTimeMillis();

		if (saxParser == null) {
			try {
				saxParser = SAXParserFactory.newInstance().newSAXParser();
			}
			catch (ParserConfigurationException e) {
				e.printStackTrace();
			}
		}
		if (saxParser == null) {
			in.close();
			return false;
		}

		// release dependency of document on previous objects for it to be garbage-collected
		if (doc != null) {
			DocumentListener[] dl = doc.getDocumentListeners();
			if (dl != null) {
				for (DocumentListener i : dl)
					doc.removeDocumentListener(i);
			}
			UndoableEditListener[] ue = doc.getUndoableEditListeners();
			if (ue != null) {
				for (UndoableEditListener i : ue)
					doc.removeUndoableEditListener(i);
			}
			try {
				doc.remove(0, doc.getLength());
			}
			catch (BadLocationException e) {
				e.printStackTrace();
			}
		}
		doc = new DefaultStyledDocument(); // do we really need to create a new instance?

		boolean success = true;
		boolean caughtUSE = false;
		boolean caughtIOE = false;
		boolean caughtSAX = false;
		String msg = null;

		// although this method throws exceptions, they must be handled within this method body in order to close I/O
		// channels, before they are propagated.

		try {
			saxParser.parse(new InputSource(in), saxHandler);
		}
		catch (SAXException e) {
			success = false;
			msg = e.getMessage();
			caughtSAX = true;
		}
		catch (IOException e) {
			success = false;
			msg = e.getMessage();
			if (e instanceof UnsupportedEncodingException) {
				caughtUSE = true;
			}
			else {
				caughtIOE = true;
			}
		}
		finally {
			in.close();
		}

		// propagate exceptions
		if (caughtUSE)
			throw new UnsupportedEncodingException(msg);
		if (caughtIOE)
			throw new IOException(msg);
		if (caughtSAX)
			throw new SAXException(msg);

		return success;

	}

	/* MUST be called by the Event Dispatching Thread! */
	private void finish() {
		if (!EventQueue.isDispatchThread())
			throw new RuntimeException("called by non-EDT thread");
		page.setReading(false);
		page.setReadingModel(false);
		loadingTime = System.currentTimeMillis() - loadingTime;
		page.getSaveReminder().setChanged(false);
		ConnectionManager.sharedInstance().setCheckUpdate(true);
		if (page.getBackgroundSound() != null) {
			page.playSound(page.getBackgroundSound());
		}
		if (progressBar != null) {
			String sec = Modeler.getInternationalText("TimeSecond");
			progressBar.setValue(0);
			progressBar.setString(elementCounter + " XML elements read in " + formatter.format(loadingTime * 0.001)
					+ " " + (sec != null ? sec : "seconds") + ".");
			progressBar.setIndeterminate(false);
		}
		// notify Editor to update buttons accordingly
		page.notifyPageListeners(new PageEvent(page, PageEvent.PAGE_READ_END));
	}

	private void loadBackgroundImage(String location) {
		if ("null".equals(location) || location == null)
			return;
		String s = FileUtilities.getFileName(location);
		if (page.isRemote())
			s = FileUtilities.httpEncode(s);
		page.setFillMode(new FillMode.ImageFill(FileUtilities.getCodeBase(page.getAddress()) + s));
	}

	private void loadInsertedImage(Style style, String path) {
		if ("null".equals(path) || path == null) {
			StyleConstants.setIcon(style, BulletIcon.ImageNotFoundIcon.sharedInstance());
			return;
		}
		Icon icon = null;
		if (page.isRemote()) {
			String fileName = FileUtilities.httpEncode(FileUtilities.getFileName(path));
			URL baseURL = null, url = null;
			boolean urlIsCorrect = true;
			try {
				baseURL = new URL(FileUtilities.httpEncode(FileUtilities.getCodeBase(page.getAddress())));
				url = new URL(baseURL, fileName);
			}
			catch (MalformedURLException mue) {
				mue.printStackTrace();
				urlIsCorrect = false;
			}
			if (urlIsCorrect) {
				icon = ConnectionManager.sharedInstance().loadImage(url);
				if (icon == null) {
					icon = BulletIcon.ImageNotFoundIcon.sharedInstance();
				}
				else {
					if (icon.getIconWidth() <= 0 || icon.getIconHeight() <= 0) {
						icon = BulletIcon.ImageNotFoundIcon.sharedInstance();
					}
				}
			}
			else {
				icon = BulletIcon.ImageNotFoundIcon.sharedInstance();
			}
		}
		else {
			String fileName = FileUtilities.getFileName(path);
			icon = new ImageIcon(Toolkit.getDefaultToolkit().createImage(
					FileUtilities.getCodeBase(page.getAddress()) + fileName));
			((ImageIcon) icon).setDescription(fileName);
			if (icon.getIconWidth() <= 0 || icon.getIconHeight() <= 0) {
				icon = BulletIcon.ImageNotFoundIcon.sharedInstance();
			}
		}
		setWrappedIcon(style, icon);
		page.setEmbeddedImageFound(true);
	}

	private void setWrappedIcon(Style style, Icon icon) {
		if (Page.wrapIconWithComponent(icon)) {
			IconWrapper iconWrapper = new IconWrapper(icon, page);
			iconWrapper.cacheLinkedFiles(page.getPathBase());
			StyleConstants.setComponent(style, iconWrapper);
			htmlComponentConnector.enroll(iconWrapper);
			if (icon instanceof LineIcon) {
				LineIcon li = (LineIcon) icon;
				if (li.getWidth() < 1.05f) {
					iconWrapper.setWidthRatio(li.getWidth());
					iconWrapper.setWidthRelative(true);
				}
			}
		}
		else {
			StyleConstants.setIcon(style, icon);
		}
	}

	class XMLHandler extends DefaultHandler {

		private String str;
		private StringBuffer buffer;
		private String attribName;
		private String attribValue;
		private int startOffset, endOffset;
		private Style currentStyle;
		private StyleContext styleContext;
		String resourceURL;
		String className;
		String mainClass, codeBase, cacheFiles;
		boolean caching;
		List<String> jarNames;
		String parameter;
		String titleText, toolTip, actionName, changeName;
		String description;
		String format;
		String modelClass;
		int modelIndex;
		int orientation = -1;
		int selectedIndex;
		int nstep, majorTicks;
		double minimum, maximum, value, stepsize;
		boolean drawTicks, drawLabels;
		String labeltable;
		boolean selected, opaque = true, transparent = true;
		String borderType;
		int nrow, ncol;
		int rowMargin = 10, colMargin = 10;
		String rowName, columnName;
		String[][] tableValue;
		int cellAlignment = SwingConstants.LEFT;
		int tableCellIndex;
		boolean showHLines, showVLines;
		boolean readingText;
		int argb = -1;
		int red = -1, green = -1, blue = -1, argb1;
		int Red = -1, Green = -1, Blue = -1, argb2;
		Color color1 = Color.white, color2 = Color.white;
		int gradientStyle, gradientVariant;
		byte patternStyle;
		int patternWidth, patternHeight;
		int alignment;
		float leftIndent, rightIndent, firstLineIndent;
		float spaceAbove, spaceBelow, lineSpacing;
		int cornerArc, arcWidth, arcHeight, topMargin, bottomMargin, leftMargin, rightMargin;
		boolean sizeSet;
		String timeSeries;
		String timeSeries_x;
		byte[] smoothers;
		String[] timeSeries_y;
		float[] multiplier_y;
		float[] addend_y;
		Color[] lineColor;
		int[] lineSymbol;
		float[] lineWidth;
		int[] lineStyle;
		int[] symbolSpacing, symbolSize;
		int legendX = -1, legendY = -1;
		String xAxisTitle, yAxisTitle;
		double dataWindow_xmin, dataWindow_xmax;
		double dataWindow_ymin, dataWindow_ymax;
		boolean autoFit = true, autoFitX = true, autoFitY = true;
		boolean autoUpdate = true;
		List<String> buttonGroup;
		boolean recorderDisabled; // backward compatible
		boolean recorderless;
		String dnaString;
		int dna_dt1 = -1, dna_dt2 = -1;
		boolean dnaContext = true;
		boolean continuousFire;
		boolean disabledAtRun, disabledAtScript;
		int iChoice;
		String[] choices = new String[10];
		boolean singleSelection = true;
		String answer;
		boolean submit, clear;
		boolean showMenuBar = true, showToolBar = true, showStatusBar = true;
		boolean hasMenu;
		int dataType;
		int maxFractionDigit = 3;
		int maxIntegerDigit = 3;
		String fontName;
		int fontSize;
		float multiplier = 1.0f;
		float addend;
		String layout;
		String group;
		int type = -1;
		int levelOfDetails;
		boolean loadScan, scriptScan;
		float scale = 8;
		String scheme;
		String rotation;
		boolean navigationMode;
		String script, script2;
		boolean dots, spin, axes, boundbox;
		byte atomColoring;
		boolean energizer;
		long groupID;
		String hintText, gradeURI;
		String pageTitle;
		String referencedFiles;
		boolean mute = true;
		float width, height;
		String expression;
		short dataPoint;
		float weight = -1;
		int style = -1;
		boolean lockEnergyLevel;
		boolean average;
		int samplingPoints = -1;
		float smoothingFactor = -1;
		List<DataSource> dataSourceList;

		private Runnable reportProgress = new Runnable() {
			public void run() {
				progressBar.setString(elementCounter + " elements read");
			}
		};

		public XMLHandler() {
			super();
			styleContext = StyleContext.getDefaultStyleContext();
			buffer = new StringBuffer();
			textBuffer = new StringBuffer();
		}

		public synchronized void startDocument() throws SAXException {
			argb = ARGB_NOT_SET;
			argb1 = ARGB_NOT_SET;
			argb2 = ARGB_NOT_SET;
			resetIndices();
			mw2dConnector.clear();
			mw3dConnector.clear();
			jmolConnector.clear();
			pluginConnector.clear();
			radioButtonConnector.clear();
			htmlComponentConnector.clear();
			textBuffer.setLength(0);
			elementCounter = 0;
			page.setBackgroundSound(null);
			pageTitle = null;
			referencedFiles = null;
			if (progressBar != null) {
				EventQueue.invokeLater(new Runnable() {
					public void run() {
						progressBar.setString("Loading......");
						progressBar.setIndeterminate(true);
					}
				});
			}
			page.setReading(true);
		}

		public synchronized void endDocument() throws SAXException {
			// Is this right? the following code fixes the missed notification problem in Mac OS X
			// when there is a mw3d container on the page with many atoms and bonds to load.
			try {
				Thread.sleep(100);
			}
			catch (InterruptedException e) {
			}
			synchronized (lock) {
				lock.notify();
			}
			if (referencedFiles != null)
				page.setReferencedFiles(referencedFiles);
			if (resourceLoadingThread == null) {
				resourceLoadingThread = new Thread("Resource Loader #" + threadIndex) {
					public void run() {
						while (true) {
							mw3dConnector.loadResources();
							mw2dConnector.loadResources();
							jmolConnector.loadResources();
							pluginConnector.start();
							EventQueue.invokeLater(new Runnable() {
								public void run() {
									progressBar.setValue(progressBar.getMaximum());
									page.setTitle(pageTitle);
									radioButtonConnector.connect();
									htmlComponentConnector.connect();
									mw3dConnector.finishLoading();
									mw2dConnector.finishLoading();
									jmolConnector.finishLoading();
									pluginConnector.connect();
									page.autoResizeComponents();
									finish();
								}
							});
							synchronized (lock) {
								try {
									lock.wait();
								}
								catch (InterruptedException e) {
									// e.printStackTrace();
									break;
								}
							}
						}
					}
				};
				resourceLoadingThread.setPriority(Page.LOADER_THREAD_PRIORITY);
				resourceLoadingThread.setUncaughtExceptionHandler(new DisasterHandler(DisasterHandler.LOAD_ERROR,
						new Runnable() {
							public void run() {
								// nullify the thread to allow a new one to be constructed when loading next page
								resourceLoadingThread = null;
							}
						}, null, page));
				resourceLoadingThread.start();
			}
		}

		public synchronized void startElement(String uri, String localName, String qName, Attributes attrib)
				throws SAXException {

			buffer.setLength(0);

			readingText = qName == "text";

			// qName = qName.intern();

			if (qName == "bgsound") {
				if (attrib != null) {
					for (int i = 0, n = attrib.getLength(); i < n; i++) {
						attribName = attrib.getQName(i).intern();
						attribValue = attrib.getValue(i);
						if (attribName == "loop") {
							page.setLoopBackgroundSound(Boolean.valueOf(attribValue).booleanValue());
						}
					}
				}
			}

			else if (qName == "content") {
				if (attrib != null) {
					for (int i = 0, n = attrib.getLength(); i < n; i++) {
						attribName = attrib.getQName(i).intern();
						attribValue = attrib.getValue(i);
						if (attribName == "start") {
							Integer ig = Parser.parseInt(attribValue);
							if (ig != null)
								startOffset = ig.intValue();
						}
						else if (attribName == "end") {
							Integer ig = Parser.parseInt(attribValue);
							if (ig != null)
								endOffset = ig.intValue();
						}
					}
				}
				currentStyle = styleContext.addStyle(null, null);
				StyleConstants.setFontSize(currentStyle, Page.getDefaultFontSize());
				StyleConstants.setFontFamily(currentStyle, Page.getDefaultFontFamily());
			}

			else if (qName == "elementarray") {
				tableValue = new String[nrow][ncol];
			}

			else if (qName.startsWith("time_series_y")) {
				if (attrib != null) {
					int symbol = -1;
					float widthOfLine = -1.0f;
					int styleOfLine = -1;
					int sizeOfSymbol = -1;
					int spacingOfSymbol = -1;
					byte sf = 0;
					float mlp = 1.0f;
					float add = 0.0f;
					for (int i = 0, n = attrib.getLength(); i < n; i++) {
						attribName = attrib.getQName(i).intern();
						attribValue = attrib.getValue(i);
						if (attribName == "color") {
							Integer ig = Parser.parseInt(attribValue, 16);
							if (ig != null)
								argb1 = ig.intValue();
						}
						else if (attribName == "symbol") {
							Integer ig = Parser.parseInt(attribValue);
							if (ig != null)
								symbol = ig.intValue();
						}
						else if (attribName == "style") {
							Integer ig = Parser.parseInt(attribValue);
							if (ig != null)
								styleOfLine = ig.intValue();
						}
						else if (attribName == "width") {
							Float fl = Parser.parseFloat(attribValue);
							if (fl != null)
								widthOfLine = fl.floatValue();
						}
						else if (attribName == "size") {
							Integer ig = Parser.parseInt(attribValue);
							if (ig != null)
								sizeOfSymbol = ig.intValue();
						}
						else if (attribName == "spacing") {
							Integer ig = Parser.parseInt(attribValue);
							if (ig != null)
								spacingOfSymbol = ig.intValue();
						}
						else if (attribName == "multiplier") {
							Float fl = Parser.parseFloat(attribValue);
							if (fl != null)
								mlp = fl.floatValue();
						}
						else if (attribName == "addend") {
							Float fl = Parser.parseFloat(attribValue);
							if (fl != null)
								add = fl.floatValue();
						}
						else if (attribName == "smoother") {
							Byte bt = Parser.parseByte(attribValue);
							if (bt != null)
								sf = bt.byteValue();
						}
					}
					if (lineColor == null)
						lineColor = new Color[PageXYGraph.MAX];
					if (sf != 0) {
						if (smoothers == null) {
							smoothers = new byte[PageXYGraph.MAX];
							Arrays.fill(smoothers, Curve.INSTANTANEOUS_VALUE);
						}
					}
					if (mlp != 1.0f) {
						if (multiplier_y == null) {
							multiplier_y = new float[PageXYGraph.MAX];
							Arrays.fill(multiplier_y, 1.0f);
						}
					}
					if (add != 0.0f) {
						if (addend_y == null) {
							addend_y = new float[PageXYGraph.MAX];
							Arrays.fill(addend_y, 0.0f);
						}
					}
					if (symbol != -1) {
						if (lineSymbol == null) {
							lineSymbol = new int[PageXYGraph.MAX];
							Arrays.fill(lineSymbol, LineSymbols.SYMBOL_NUMBER_1);
						}
					}
					if (styleOfLine != -1) {
						if (lineStyle == null)
							lineStyle = new int[PageXYGraph.MAX];
					}
					if (widthOfLine > -1.0f) {
						if (lineWidth == null) {
							lineWidth = new float[PageXYGraph.MAX];
							Arrays.fill(lineWidth, 1.0f);
						}
					}
					if (sizeOfSymbol != -1) {
						if (symbolSize == null) {
							symbolSize = new int[PageXYGraph.MAX];
							Arrays.fill(symbolSize, 4);
						}
					}
					if (spacingOfSymbol != -1) {
						if (symbolSpacing == null) {
							symbolSpacing = new int[PageXYGraph.MAX];
							Arrays.fill(symbolSpacing, 5);
						}
					}
					String afterY = qName.substring(qName.lastIndexOf("y") + 1);
					Integer ig = Parser.parseInt(afterY);
					if (ig != null) {
						int its = ig.intValue() - 1;
						if ((argb1 | ARGB_NOT_SET) != ARGB_NOT_SET) {
							lineColor[its] = new Color(argb1);
							argb1 = ARGB_NOT_SET;
						}
						if (symbol != -1)
							lineSymbol[its] = symbol;
						if (styleOfLine != -1)
							lineStyle[its] = styleOfLine;
						if (widthOfLine > -1.0f)
							lineWidth[its] = widthOfLine;
						if (sizeOfSymbol != -1)
							symbolSize[its] = sizeOfSymbol;
						if (spacingOfSymbol != -1)
							symbolSpacing[its] = spacingOfSymbol;
						if (mlp != 1.0f)
							multiplier_y[its] = mlp;
						if (add != 0.0f)
							addend_y[its] = add;
						if (sf != 0)
							smoothers[its] = sf;
					}
				}
			}

		}

		public synchronized void endElement(String uri, String localName, String qName) {

			if (progressBar != null) {
				elementCounter++;
				if (elementCounter % 20 == 0)
					EventQueue.invokeLater(reportProgress);
			}

			str = buffer.toString();

			// qName = qName.intern(); // it seems no need to do string pooling here

			if (qName == "language") {
				page.setCharacterEncoding(str);
			}

			else if (qName == "page_title") {
				pageTitle = str;
			}

			else if (qName == "referenced_files") {
				referencedFiles = str;
			}

			else if (qName == "bgsound") {
				page.setBackgroundSound(str);
			}

			else if (qName == "mute") {
				mute = Boolean.valueOf(str).booleanValue();
			}

			else if (qName == "color") {
				Integer ig = Parser.parseInt(str, 16);
				if (ig != null)
					argb = ig.intValue();
			}

			else if (qName == "bgcolor") {
				Integer ig = Parser.parseInt(str, 16);
				if (ig != null)
					argb1 = ig.intValue();
			}

			else if (qName == "fgcolor") {
				Integer ig = Parser.parseInt(str, 16);
				if (ig != null)
					argb2 = ig.intValue();
			}

			/* ********************************************************** */
			// back compatible to old color encoding
			else if (qName == "red") {
				Integer ig = Parser.parseInt(str);
				if (ig != null)
					red = ig.intValue();
			}
			else if (qName == "green") {
				Integer ig = Parser.parseInt(str);
				if (ig != null)
					green = ig.intValue();
			}
			else if (qName == "blue") {
				Integer ig = Parser.parseInt(str);
				if (ig != null)
					blue = ig.intValue();
				argb1 = (0xff << 24) | (red << 16) | (green << 8) | blue;
				red = green = blue = -1;
			}
			else if (qName == "Red") {
				Integer ig = Parser.parseInt(str);
				if (ig != null)
					Red = ig.intValue();
			}
			else if (qName == "Green") {
				Integer ig = Parser.parseInt(str);
				if (ig != null)
					Green = ig.intValue();
			}
			else if (qName == "Blue") {
				Integer ig = Parser.parseInt(str);
				if (ig != null)
					Blue = ig.intValue();
				argb2 = (0xff << 24) | (Red << 16) | (Green << 8) | Blue;
				Red = Green = Blue = -1;
			}
			/* ********************************************************** */

			else if (qName.startsWith("bg_")) {
				if (qName.endsWith("gradient")) {
					setBackgroundGradient();
				}
				else if (qName.endsWith("pattern")) {
					setBackgroundPattern();
				}
				else if (qName.endsWith("color")) {
					if ((argb2 | ARGB_NOT_SET) != ARGB_NOT_SET) {
						setBackgroundColor();
						argb2 = ARGB_NOT_SET;
					}
					else {
						Integer ig = Parser.parseInt(str, 16);
						if (ig != null) {
							argb2 = ig.intValue();
							if ((argb2 | ARGB_NOT_SET) != ARGB_NOT_SET) {
								setBackgroundColor();
								argb2 = ARGB_NOT_SET;
							}
						}
					}
				}
				else if (qName.endsWith("image")) {
					loadBackgroundImage(str);
				}
			}

			else if (qName.startsWith("gradient")) {
				if (qName.endsWith("_color1")) {
					if ((argb2 | ARGB_NOT_SET) != ARGB_NOT_SET) {
						color1 = new Color(argb2);
						argb2 = ARGB_NOT_SET;
					}
					else {
						Integer ig = Parser.parseInt(str, 16);
						if (ig != null) {
							argb2 = ig.intValue();
							if ((argb2 | ARGB_NOT_SET) != ARGB_NOT_SET) {
								color1 = new Color(argb2);
								argb2 = ARGB_NOT_SET;
							}
						}
					}
				}
				else if (qName.endsWith("_color2")) {
					if ((argb2 | ARGB_NOT_SET) != ARGB_NOT_SET) {
						color2 = new Color(argb2);
						argb2 = ARGB_NOT_SET;
					}
					else {
						Integer ig = Parser.parseInt(str, 16);
						if (ig != null) {
							argb2 = ig.intValue();
							if ((argb2 | ARGB_NOT_SET) != ARGB_NOT_SET) {
								color2 = new Color(argb2);
								argb2 = ARGB_NOT_SET;
							}
						}
					}
				}
				else if (qName.endsWith("_style")) {
					Integer ig = Parser.parseInt(str);
					if (ig != null)
						gradientStyle = ig.intValue();
				}
				else if (qName.endsWith("_variant")) {
					Integer ig = Parser.parseInt(str);
					if (ig != null)
						gradientVariant = ig.intValue();
				}
			}

			else if (qName.startsWith("pattern")) {
				if (qName.endsWith("_fg")) {
					Integer ig = Parser.parseInt(str, 16);
					if (ig != null)
						color1 = new Color(ig.intValue());
				}
				else if (qName.endsWith("_bg")) {
					Integer ig = Parser.parseInt(str, 16);
					if (ig != null)
						color2 = new Color(ig.intValue());
				}
				else if (qName.endsWith("_style")) {
					Byte ib = Parser.parseByte(str);
					if (ib != null)
						patternStyle = ib.byteValue();
				}
				else if (qName.endsWith("_width")) {
					Integer ig = Parser.parseInt(str);
					if (ig != null)
						patternWidth = ig.intValue();
				}
				else if (qName.endsWith("_height")) {
					Integer ig = Parser.parseInt(str);
					if (ig != null)
						patternHeight = ig.intValue();
				}
			}

			else if (qName == "href") {
				str = XMLCharacterDecoder.decode(str);
				currentStyle.addAttribute(HTML.Attribute.HREF, str);
				StyleConstants.setUnderline(currentStyle, Page.isLinkUnderlined());
				if (FileUtilities.isRelative(str))
					str = page.resolvePath(str);
				if (!FileUtilities.isRemote(str))
					str = FileUtilities.useSystemFileSeparator(str);
				StyleConstants.setForeground(currentStyle, HistoryManager.sharedInstance().wasVisited(str) ? Page
						.getVisitedColor() : Page.getLinkColor());
			}

			else if (qName == "target") {
				currentStyle.addAttribute(HTML.Attribute.TARGET, str);
			}

			else if (qName == HyperlinkParameter.ATTRIBUTE_RESIZABLE) {
				currentStyle.addAttribute(HyperlinkParameter.ATTRIBUTE_RESIZABLE, Boolean.valueOf(str));
			}

			else if (qName == HyperlinkParameter.ATTRIBUTE_FULLSCREEN) {
				currentStyle.addAttribute(HyperlinkParameter.ATTRIBUTE_FULLSCREEN, Boolean.valueOf(str));
			}

			else if (qName == HyperlinkParameter.ATTRIBUTE_TOOLBAR) {
				currentStyle.addAttribute(HyperlinkParameter.ATTRIBUTE_TOOLBAR, Boolean.valueOf(str));
			}

			else if (qName == HyperlinkParameter.ATTRIBUTE_MENUBAR) {
				currentStyle.addAttribute(HyperlinkParameter.ATTRIBUTE_MENUBAR, Boolean.valueOf(str));
			}

			else if (qName == HyperlinkParameter.ATTRIBUTE_STATUSBAR) {
				currentStyle.addAttribute(HyperlinkParameter.ATTRIBUTE_STATUSBAR, Boolean.valueOf(str));
			}

			else if (qName == HyperlinkParameter.ATTRIBUTE_LEFT) {
				Integer ig = Parser.parseInt(str);
				if (ig != null)
					currentStyle.addAttribute(HyperlinkParameter.ATTRIBUTE_LEFT, ig);
			}

			else if (qName == HyperlinkParameter.ATTRIBUTE_TOP) {
				Integer ig = Parser.parseInt(str);
				if (ig != null)
					currentStyle.addAttribute(HyperlinkParameter.ATTRIBUTE_TOP, ig);
			}

			else if (qName == HyperlinkParameter.ATTRIBUTE_WIDTH) {
				Integer ig = Parser.parseInt(str);
				if (ig != null)
					currentStyle.addAttribute(HyperlinkParameter.ATTRIBUTE_WIDTH, ig);
			}

			else if (qName == HyperlinkParameter.ATTRIBUTE_HEIGHT) {
				Integer ig = Parser.parseInt(str);
				if (ig != null)
					currentStyle.addAttribute(HyperlinkParameter.ATTRIBUTE_HEIGHT, ig);
			}

			else if (qName == "size") {
				// WARNING: the "size" tag is used here to represent font size only!
				Integer ig = Parser.parseInt(str);
				if (ig != null) {
					int fontSize = ig.intValue() + page.getFontIncrement();
					StyleConstants.setFontSize(currentStyle, fontSize < 8 ? 8 : fontSize);
					sizeSet = true;
				}
			}

			else if (qName == "bold") {
				StyleConstants.setBold(currentStyle, (Boolean.valueOf(str)).booleanValue());
			}

			else if (qName == "italic") {
				StyleConstants.setItalic(currentStyle, (Boolean.valueOf(str)).booleanValue());
			}

			else if (qName == "underline") {
				StyleConstants.setUnderline(currentStyle, (Boolean.valueOf(str)).booleanValue());
			}

			else if (qName == "strikethrough") {
				StyleConstants.setStrikeThrough(currentStyle, (Boolean.valueOf(str)).booleanValue());
			}

			else if (qName == "subscript") {
				StyleConstants.setSubscript(currentStyle, (Boolean.valueOf(str)).booleanValue());
			}

			else if (qName == "superscript") {
				StyleConstants.setSuperscript(currentStyle, (Boolean.valueOf(str)).booleanValue());
			}

			else if (qName == "family") {
				StyleConstants.setFontFamily(currentStyle, str);
			}

			else if (qName == "foreground") {
				StringTokenizer st = new StringTokenizer(str);
				int[] rgb = new int[3];
				int i = 0;
				while (st.hasMoreTokens()) {
					Integer ig = Parser.parseInt(st.nextToken());
					if (ig != null) {
						rgb[i] = ig.intValue();
						i++;
					}
				}
				if (i == 3)
					StyleConstants.setForeground(currentStyle, new Color(rgb[0], rgb[1], rgb[2]));
			}

			else if (qName == "cornerarc") {
				Integer in = Parser.parseInt(str);
				if (in != null)
					cornerArc = in.intValue();
			}

			else if (qName == "arcwidth") {
				Integer in = Parser.parseInt(str);
				if (in != null)
					arcWidth = in.intValue();
			}

			else if (qName == "archeight") {
				Integer in = Parser.parseInt(str);
				if (in != null)
					arcHeight = in.intValue();
			}

			else if (qName == "topmargin") {
				Integer in = Parser.parseInt(str);
				if (in != null)
					topMargin = in.intValue();
			}

			else if (qName == "bottommargin") {
				Integer in = Parser.parseInt(str);
				if (in != null)
					bottomMargin = in.intValue();
			}

			else if (qName == "leftmargin") {
				Integer in = Parser.parseInt(str);
				if (in != null)
					leftMargin = in.intValue();
			}

			else if (qName == "rightmargin") {
				Integer in = Parser.parseInt(str);
				if (in != null)
					rightMargin = in.intValue();
			}

			else if (qName == "Alignment") {
				Integer ig = Parser.parseInt(str);
				if (ig != null)
					alignment = ig.intValue();
			}

			else if (qName == "LeftIndent") {
				Float fl = Parser.parseFloat(str);
				if (fl != null)
					leftIndent = fl.floatValue();
			}

			else if (qName == "RightIndent") {
				Float fl = Parser.parseFloat(str);
				if (fl != null)
					rightIndent = fl.floatValue();
			}

			/*
			 * TODO: not implemented yet. else if(qName==StyleConstants.FirstLineIndent.toString()){ Float
			 * fl=Parser.parseFloat(str); if(fl!=null) firstLineIndent=fl.floatValue(); }
			 * 
			 * else if(qName==StyleConstants.LineSpacing.toString()){ System.out.println(qName); Float
			 * fl=Parser.parseFloat(str); if(fl!=null) lineSpacing=fl.floatValue(); }
			 */

			else if (qName == "SpaceAbove") {
				Float fl = Parser.parseFloat(str);
				if (fl != null)
					spaceAbove = fl.floatValue();
			}

			else if (qName == "SpaceBelow") {
				Float fl = Parser.parseFloat(str);
				if (fl != null)
					spaceBelow = fl.floatValue();
			}

			else if (qName == "icon") {
				if (className != null && className.endsWith("LineIcon")) {
					LineIcon lineIcon = new LineIcon(page);
					if ((argb1 | ARGB_NOT_SET) != ARGB_NOT_SET) {
						lineIcon.setColor(new Color(argb1));
						argb1 = ARGB_NOT_SET;
					}
					if (!opaque) {
						lineIcon.setFilled(false);
						opaque = true;
					}
					if (width > 0) {
						lineIcon.setWidth(width);
						width = 0;
					}
					if (height > 0) {
						lineIcon.setHeight((int) height);
						height = 0;
					}
					if (leftMargin > 0) {
						lineIcon.setLeftMargin(leftMargin);
						leftMargin = 0;
					}
					if (rightMargin > 0) {
						lineIcon.setRightMargin(rightMargin);
						rightMargin = 0;
					}
					if (topMargin != 0) {
						lineIcon.setTopMargin(topMargin);
						topMargin = 0;
					}
					if (bottomMargin != 0) {
						lineIcon.setBottomMargin(bottomMargin);
						bottomMargin = 0;
					}
					if (cornerArc != 0) {
						lineIcon.setCornerArc(cornerArc);
						cornerArc = 0;
					}
					if (arcWidth != 0) {
						lineIcon.setArcWidth(arcWidth);
						arcWidth = 0;
					}
					if (arcHeight != 0) {
						lineIcon.setArcHeight(arcHeight);
						arcHeight = 0;
					}
					if (titleText != null) {
						lineIcon.setText(XMLCharacterDecoder.decode(titleText));
						titleText = null;
					}
					setWrappedIcon(currentStyle, lineIcon);
					className = null;
				}
				else {
					if (str.endsWith("$SquareBulletIcon")) {
						StyleConstants.setIcon(currentStyle, BulletIcon.SquareBulletIcon.sharedInstance());
					}
					else if (str.endsWith("$SolidSquareBulletIcon")) {
						StyleConstants.setIcon(currentStyle, BulletIcon.SolidSquareBulletIcon.sharedInstance());
					}
					else if (str.endsWith("$OpenCircleBulletIcon")) {
						StyleConstants.setIcon(currentStyle, BulletIcon.OpenCircleBulletIcon.sharedInstance());
					}
					else if (str.endsWith("$SolidCircleBulletIcon")) {
						StyleConstants.setIcon(currentStyle, BulletIcon.SolidCircleBulletIcon.sharedInstance());
					}
					else if (str.endsWith("$PosTickBulletIcon")) {
						StyleConstants.setIcon(currentStyle, BulletIcon.PosTickBulletIcon.sharedInstance());
					}
					else if (str.endsWith("$NegTickBulletIcon")) {
						StyleConstants.setIcon(currentStyle, BulletIcon.NegTickBulletIcon.sharedInstance());
					}
					else if (str.endsWith("$DiamondBulletIcon")) {
						StyleConstants.setIcon(currentStyle, BulletIcon.DiamondBulletIcon.sharedInstance());
					}
					else if (str.endsWith("$NumberIcon")) {
						StyleConstants.setIcon(currentStyle, BulletIcon.SquareBulletIcon.sharedInstance());
					}
					else {
						loadInsertedImage(currentStyle, str);
					}
				}
			}

			else if (qName == "class") {
				className = str;
			}

			else if (qName == "appletclass") {
				mainClass = str;
			}

			else if (qName == "codebase") {
				codeBase = str;
			}

			else if (qName == "cachefile") {
				cacheFiles = str;
			}

			else if (qName == "appletjar") {
				if (jarNames == null)
					jarNames = new ArrayList<String>();
				jarNames.add(str);
			}

			else if (qName == "caching") {
				caching = Boolean.valueOf(str).booleanValue();
			}

			else if (qName == "parameter") {
				parameter = str;
			}

			else if (qName == "groupid") {
				Long id = Parser.parseLong(str);
				if (id != null)
					groupID = id.longValue();
			}

			else if (qName == "resource") {
				resourceURL = FileUtilities.getCodeBase(page.getAddress()) + FileUtilities.getFileName(str);
			}

			else if (qName == "script") {
				script = str;
			}

			else if (qName == "script2") {
				script2 = str;
			}

			else if (qName == "expression") {
				expression = str;
			}

			else if (qName == "datapoint") {
				Short sh = Parser.parseShort(str);
				if (sh != null)
					dataPoint = sh.shortValue();
			}

			else if (qName == "weight") {
				Float fl = Parser.parseFloat(str);
				if (fl != null)
					weight = fl.floatValue();
			}

			else if (qName == "style") {
				Integer ig = Parser.parseInt(str);
				if (ig != null)
					style = ig.intValue();
			}

			else if (qName == "function") {
				if (dataSourceList == null)
					dataSourceList = new ArrayList<DataSource>();
				dataSourceList.add(createDataSource());
			}

			else if (qName == "scheme") {
				scheme = str;
			}

			else if (qName == "rotation") {
				rotation = str;
			}

			else if (qName == "navigation") {
				navigationMode = Boolean.valueOf(str).booleanValue();
			}

			else if (qName == "dots") {
				dots = Boolean.valueOf(str).booleanValue();
			}

			else if (qName == "atomcolor") {
				Byte b = Parser.parseByte(str);
				if (b != null)
					atomColoring = b.byteValue();
			}

			else if (qName == "spin") {
				spin = Boolean.valueOf(str).booleanValue();
			}

			else if (qName == "axes") {
				axes = Boolean.valueOf(str).booleanValue();
			}

			else if (qName == "boundbox") {
				boundbox = Boolean.valueOf(str).booleanValue();
			}

			else if (qName == "menubar") {
				showMenuBar = Boolean.valueOf(str).booleanValue();
			}

			else if (qName == "toolbar") {
				showToolBar = Boolean.valueOf(str).booleanValue();
			}

			else if (qName == "statusbar") {
				showStatusBar = Boolean.valueOf(str).booleanValue();
			}

			else if (qName == "energizer") {
				energizer = Boolean.valueOf(str).booleanValue();
			}

			else if (qName == "layout") {
				layout = str;
			}

			else if (qName == "width") {
				Double db = Parser.parseDouble(str);
				if (db != null)
					width = (float) db.doubleValue();
			}

			else if (qName == "height") {
				Double db = Parser.parseDouble(str);
				if (db != null)
					height = (float) db.doubleValue();
			}

			else if (qName == "type") {
				Integer ig = Parser.parseInt(str);
				if (ig != null)
					type = ig.intValue();
			}

			else if (qName == "column") {
				Integer ig = Parser.parseInt(str);
				if (ig != null)
					ncol = ig.intValue();
			}

			else if (qName == "row") {
				Integer ig = Parser.parseInt(str);
				if (ig != null)
					nrow = ig.intValue();
			}

			else if (qName == "columnmargin") {
				Integer ig = Parser.parseInt(str);
				if (ig != null)
					colMargin = ig.intValue();
			}

			else if (qName == "rowmargin") {
				Integer ig = Parser.parseInt(str);
				if (ig != null)
					rowMargin = ig.intValue();
			}

			else if (qName == "border") {
				borderType = str;
			}

			else if (qName == "single") {
				singleSelection = Boolean.valueOf(str).booleanValue();
			}

			else if (qName == "answer") {
				answer = str;
			}

			else if (qName == "submit") {
				submit = Boolean.valueOf(str).booleanValue();
			}

			else if (qName == "clear") {
				clear = Boolean.valueOf(str).booleanValue();
			}

			else if (qName == "hasmenu") {
				hasMenu = Boolean.valueOf(str).booleanValue();
			}

			else if (qName == "opaque") {
				opaque = Boolean.valueOf(str).booleanValue();
			}

			else if (qName == "transparent") {
				transparent = Boolean.valueOf(str).booleanValue();
			}

			else if (qName == "continuous_fire") {
				continuousFire = Boolean.valueOf(str).booleanValue();
			}

			else if (qName == "disabled_at_run") {
				disabledAtRun = Boolean.valueOf(str).booleanValue();
			}

			else if (qName == "disabled_at_script") {
				disabledAtScript = Boolean.valueOf(str).booleanValue();
			}

			else if (qName == "level_of_details") {
				Integer ig = Parser.parseInt(str);
				if (ig != null)
					levelOfDetails = ig.intValue();
			}

			else if (qName == "loadscan") {
				loadScan = Boolean.valueOf(str).booleanValue();
			}

			else if (qName == "scriptscan") {
				scriptScan = Boolean.valueOf(str).booleanValue();
			}

			else if (qName == "lockenergylevel") {
				lockEnergyLevel = Boolean.valueOf(str).booleanValue();
			}

			else if (qName == "scale") {
				Float fg = Parser.parseFloat(str);
				if (fg != null)
					scale = fg.floatValue();
			}

			else if (qName == "tick") {
				drawTicks = Boolean.valueOf(str).booleanValue();
			}

			else if (qName == "label") {
				drawLabels = Boolean.valueOf(str).booleanValue();
			}

			else if (qName == "labeltable") {
				labeltable = str;
			}

			else if (qName == "selected") {
				selected = Boolean.valueOf(str).booleanValue();
			}

			else if (qName == "selectedIndex") {
				Integer ig = Parser.parseInt(str);
				if (ig != null)
					selectedIndex = ig.intValue();
			}

			else if (qName == "minimum") {
				Double d = Parser.parseDouble(str);
				if (d != null)
					minimum = d.doubleValue();
			}

			else if (qName == "maximum") {
				Double d = Parser.parseDouble(str);
				if (d != null)
					maximum = d.doubleValue();
			}

			else if (qName == "nstep") {
				Integer ig = Parser.parseInt(str);
				if (ig != null)
					nstep = ig.intValue();
			}

			else if (qName == "major_tick") {
				Integer ig = Parser.parseInt(str);
				if (ig != null)
					majorTicks = ig;
			}

			else if (qName == "value") {
				Double d = Parser.parseDouble(str);
				if (d != null)
					value = d;
			}

			else if (qName == "step") {
				Double d = Parser.parseDouble(str);
				if (d != null)
					stepsize = d;
			}

			else if (qName == "title") {
				titleText = str;
			}

			else if (qName == "tooltip") {
				toolTip = str;
			}

			else if (qName == "timeseries") {
				timeSeries = str;
			}

			else if (qName == "action") {
				actionName = str;
			}

			else if (qName == "change") {
				changeName = str;
			}

			else if (qName == "orientation") {
				Integer ig = Parser.parseInt(str);
				if (ig != null)
					orientation = ig;
			}

			else if (qName == "description") {
				description = str;
			}

			else if (qName == "cellalign") {
				Integer ig = Parser.parseInt(str);
				if (ig != null)
					cellAlignment = ig;
			}

			else if (qName == "rowname") {
				rowName = str;
			}

			else if (qName == "columnname") {
				columnName = str;
			}

			else if (qName == "hint_text") {
				hintText = str;
			}

			else if (qName == "gradeuri") {
				gradeURI = str;
			}

			else if (qName == "format") {
				format = str;
			}

			else if (qName == "multiplier") {
				Float fl = Parser.parseFloat(str);
				if (fl != null)
					multiplier = fl;
			}

			else if (qName == "addend") {
				Float fl = Parser.parseFloat(str);
				if (fl != null)
					addend = fl;
			}

			else if (qName == "datatype") {
				Integer ig = Parser.parseInt(str);
				if (ig != null)
					dataType = ig;
			}

			else if (qName == "average") {
				average = Boolean.valueOf(str);
			}

			else if (qName == "samplingpoints") {
				Integer ig = Parser.parseInt(str);
				if (ig != null)
					samplingPoints = ig;
			}

			else if (qName == "smoothingfactor") {
				Float fl = Parser.parseFloat(str);
				if (fl != null)
					smoothingFactor = fl;
			}

			else if (qName == "max_fraction_digits") {
				Integer ig = Parser.parseInt(str);
				if (ig != null)
					maxFractionDigit = ig;
			}

			else if (qName == "max_integer_digits") {
				Integer ig = Parser.parseInt(str);
				if (ig != null)
					maxIntegerDigit = ig;
			}

			else if (qName == "fontname") {
				fontName = str;
			}

			else if (qName == "fontsize") {
				Integer ig = Parser.parseInt(str);
				if (ig != null)
					fontSize = ig;
			}

			else if (qName == "choice") {
				choices[iChoice] = str;
				iChoice++;
			}

			else if (qName == "modelclass") {
				modelClass = str;
			}

			else if (qName == "model") {
				Integer ig = Parser.parseInt(str);
				if (ig != null)
					modelIndex = ig.intValue();
			}

			else if (qName == "recorder") {
				// NOTE: this tag acts against its name. Hence it is changed to recorderless below.
				recorderDisabled = Boolean.valueOf(str).booleanValue();
			}

			else if (qName == "recorderless") {
				recorderless = Boolean.valueOf(str).booleanValue();
			}

			else if (qName == "dna") {
				dnaString = str;
			}

			else if (qName == "dna_context") {
				dnaContext = Boolean.valueOf(str).booleanValue();
			}

			else if (qName == "dna_dt1") {
				Integer ig = Parser.parseInt(str);
				if (ig != null)
					dna_dt1 = ig.intValue();
			}

			else if (qName == "dna_dt2") {
				Integer ig = Parser.parseInt(str);
				if (ig != null)
					dna_dt2 = ig.intValue();
			}

			else if (qName == "te") {
				tableValue[tableCellIndex / ncol][tableCellIndex % ncol] = str;
				tableCellIndex++;
			}

			else if (qName == "hline") {
				showHLines = Boolean.valueOf(str).booleanValue();
			}

			else if (qName == "vline") {
				showVLines = Boolean.valueOf(str).booleanValue();
			}

			else if (qName.startsWith("legend")) {
				if (qName.endsWith("_x")) {
					Integer ig = Parser.parseInt(str);
					if (ig != null)
						legendX = ig.intValue();
				}
				else if (qName.endsWith("_y")) {
					Integer ig = Parser.parseInt(str);
					if (ig != null)
						legendY = ig.intValue();
				}
			}

			else if (qName == "autofit") {
				autoFit = Boolean.valueOf(str).booleanValue();
			}

			else if (qName == "autofitx") {
				autoFitX = Boolean.valueOf(str).booleanValue();
			}

			else if (qName == "autofity") {
				autoFitY = Boolean.valueOf(str).booleanValue();
			}

			else if (qName == "autoupdate") {
				autoUpdate = Boolean.valueOf(str).booleanValue();
			}

			else if (qName.startsWith("axis")) {
				if (qName.endsWith("x_title")) {
					xAxisTitle = str;
				}
				else if (qName.endsWith("y_title")) {
					yAxisTitle = str;
				}
				else if (qName.endsWith("x_min")) {
					Double db = Parser.parseDouble(str);
					if (db != null)
						dataWindow_xmin = db.doubleValue();
				}
				else if (qName.endsWith("x_max")) {
					Double db = Parser.parseDouble(str);
					if (db != null)
						dataWindow_xmax = db.doubleValue();
				}
				else if (qName.endsWith("y_min")) {
					Double db = Parser.parseDouble(str);
					if (db != null)
						dataWindow_ymin = db.doubleValue();
				}
				else if (qName.endsWith("y_max")) {
					Double db = Parser.parseDouble(str);
					if (db != null)
						dataWindow_ymax = db.doubleValue();
				}
			}

			else if (qName.startsWith("time_series")) {
				if (qName.endsWith("_x")) {
					timeSeries_x = str;
				}
				else if (qName.indexOf("_y") != -1) {
					Integer ig = Parser.parseInt(qName.substring(qName.lastIndexOf("y") + 1));
					if (ig != null) {
						int its = ig.intValue();
						if (timeSeries_y == null)
							timeSeries_y = new String[PageXYGraph.MAX];
						timeSeries_y[its - 1] = str;
					}
				}
			}

			else if (qName == "button") {
				if (buttonGroup == null)
					buttonGroup = new ArrayList<String>();
				buttonGroup.add(str);
			}

			else if (qName == "group") {
				group = str;
			}

			else if (qName == "component") {
				if (className != null) {
					createComponent(currentStyle, className);
					className = null;
				}
			}

			else if (qName == "content") {
				if (sizeSet) {
					sizeSet = false;
				}
				else {
					int fontSize = 12 + page.getFontIncrement();
					StyleConstants.setFontSize(currentStyle, fontSize < 8 ? 8 : fontSize);
				}
				String subString = null;
				try {
					subString = textBuffer.substring(startOffset, endOffset);
				}
				catch (StringIndexOutOfBoundsException e) {
					e.printStackTrace();
					subString = null;
				}
				if (subString != null) {
					try {
						doc.insertString(startOffset, subString, currentStyle);
					}
					catch (BadLocationException e) {
						e.printStackTrace();
					}
				}
			}

			else if (qName == "paragraph") {
				Element pe = doc.getParagraphElement((startOffset + endOffset) / 2);
				AttributeSet as = pe.getAttributes();
				SimpleAttributeSet sas = new SimpleAttributeSet(as);
				sas.addAttribute(StyleConstants.Alignment, new Integer(alignment));
				sas.addAttribute(StyleConstants.FirstLineIndent, new Float(firstLineIndent));
				sas.addAttribute(StyleConstants.LineSpacing, new Float(lineSpacing));
				float indent;
				if (leftIndent >= 1) {
					indent = leftIndent;
				}
				else {
					indent = leftIndent * page.getWidth();
				}
				sas.addAttribute(StyleConstants.LeftIndent, new Float(indent));
				if (rightIndent >= 1) {
					indent = rightIndent;
				}
				else {
					indent = rightIndent * page.getWidth();
				}
				sas.addAttribute(StyleConstants.RightIndent, new Float(indent));
				sas.addAttribute(StyleConstants.SpaceAbove, new Float(spaceAbove));
				sas.addAttribute(StyleConstants.SpaceBelow, new Float(spaceBelow));
				doc.setParagraphAttributes(startOffset, endOffset - startOffset, sas, false);
				alignment = StyleConstants.ALIGN_LEFT;
				firstLineIndent = 0.0f;
				lineSpacing = 0.0f;
				leftIndent = 0.0f;
				rightIndent = 0.0f;
				spaceAbove = 0.0f;
				spaceBelow = 0.0f;
			}

		}

		public synchronized void characters(char[] ch, int start, int length) {
			str = new String(ch, start, length);
			buffer.append(str);
			if (readingText)
				textBuffer.append(str);
		}

		private void setBackgroundColor() {
			if ((argb2 | ARGB_NOT_SET) == ARGB_NOT_SET)
				return;
			page.setFillMode(new FillMode.ColorFill(new Color(argb2)));
		}

		private void setBackgroundGradient() {
			if (color1 == null || color2 == null)
				return;
			page.setFillMode(new FillMode.GradientFill(color1, color2, gradientStyle, gradientVariant));
			gradientStyle = 0;
			gradientVariant = 0;
		}

		private DataSource createDataSource() {
			DataSource ds = new DataSource(dataPoint == 0 ? 200 : dataPoint * 2);
			ds.setVariableValue("x", 0);
			ds.setExpression(expression);
			ds.setHideHotSpot(true);
			if ((argb | ARGB_NOT_SET) != ARGB_NOT_SET) {
				ds.setColor(new Color(argb));
				argb = ARGB_NOT_SET;
			}
			if (weight > 0) {
				ds.setLineWeight(weight);
				weight = -1;
			}
			if (style > 0) {
				ds.setStroke(StrokeFactory.changeStyle(ds.getStroke(), LineStyle.getDashArray(style)));
				style = -1;
			}
			dataPoint = 0;
			return ds;
		}

		private void setBackgroundPattern() {
			if (color1 == null || color2 == null)
				return;
			page.setFillMode(new FillMode.PatternFill(color1.getRGB(), color2.getRGB(), patternStyle, patternWidth,
					patternHeight));
			patternStyle = 0;
			patternWidth = 0;
			patternHeight = 0;
		}

		/*
		 * creating a swing component within a single thread is safe: while the components are being created, no other
		 * thread can touch them. StyleConstants.setComponent() only acts to associate it with a StyledDocument, meaning
		 * that it doesn't actually change its internal states (hence no deadlock).
		 * 
		 * $$$ The above statement seems not true any more, because Sun changed to a stricter single-thread rule that
		 * mandates creating Swing components in the EDT.
		 */
		private void createComponent(Style style, String clazz) {
			clazz = clazz.intern();
			// legacy container names: the actual names have been renamed to org.concord.mw2d.ui.*
			if ("org.concord.mw2d.activity.AtomContainer" == clazz || "org.concord.mw2d.activity.GBContainer" == clazz
					|| "org.concord.mw2d.activity.ChemContainer" == clazz) {
				ModelCanvas mc = createModelCanvas(clazz);
				if (mc != null)
					StyleConstants.setComponent(style, mc);

			}
			else if (PageMd3d.class.getName() == clazz) {
				StyleConstants.setComponent(style, createMd3d());
			}
			else if (PageMolecularViewer.class.getName() == clazz) {
				StyleConstants.setComponent(style, createMolecularViewer());
			}
			else if (PageScriptConsole.class.getName() == clazz) {
				StyleConstants.setComponent(style, createScriptConsole());
			}
			else if (PageFeedbackArea.class.getName() == clazz) {
				StyleConstants.setComponent(style, createFeedbackArea());
			}
			else if (AudioPlayer.class.getName() == clazz) {
				StyleConstants.setComponent(style, createAudioPlayer());
			}
			else if (PageApplet.class.getName() == clazz) {
				StyleConstants.setComponent(style, createApplet());
			}
			else if (PageJContainer.class.getName() == clazz) {
				StyleConstants.setComponent(style, createPlugin());
			}
			else if (ActivityButton.class.getName() == clazz) {
				StyleConstants.setComponent(style, createActivityButton());
			}
			else if (PageButton.class.getName() == clazz) {
				StyleConstants.setComponent(style, createButton());
			}
			else if (PageComboBox.class.getName() == clazz) {
				StyleConstants.setComponent(style, createComboBox());
			}
			else if (PageRadioButton.class.getName() == clazz) {
				StyleConstants.setComponent(style, createRadioButton());
			}
			else if (PageCheckBox.class.getName() == clazz) {
				StyleConstants.setComponent(style, createCheckBox());
			}
			else if (PageSlider.class.getName() == clazz) {
				StyleConstants.setComponent(style, createSlider());
			}
			else if (PageSpinner.class.getName() == clazz) {
				StyleConstants.setComponent(style, createSpinner());
			}
			else if (PageTable.class.getName() == clazz) {
				StyleConstants.setComponent(style, createTable());
			}
			else if (PageTextField.class.getName() == clazz) {
				StyleConstants.setComponent(style, createTextField());
			}
			else if (SearchTextField.class.getName() == clazz) {
				StyleConstants.setComponent(style, createSearchTextField());
			}
			else if (PageTextArea.class.getName() == clazz) {
				StyleConstants.setComponent(style, createTextArea());
			}
			else if (PageTextBox.class.getName() == clazz) {
				StyleConstants.setComponent(style, createTextBox());
			}
			else if (PageMultipleChoice.class.getName() == clazz) {
				StyleConstants.setComponent(style, createMultipleChoice());
			}
			else if (ImageQuestion.class.getName() == clazz) {
				StyleConstants.setComponent(style, createImageQuestion());
			}
			else if (PageNumericBox.class.getName() == clazz) {
				StyleConstants.setComponent(style, createNumericBox());
			}
			else if (PageBarGraph.class.getName() == clazz) {
				StyleConstants.setComponent(style, createBarGraph());
			}
			else if (PageXYGraph.class.getName() == clazz) {
				StyleConstants.setComponent(style, createXYGraph());
			}
			else if (PagePotentialWell.class.getName() == clazz) {
				StyleConstants.setComponent(style, createPotentialWell());
			}
			else if (PagePotentialHill.class.getName() == clazz) {
				StyleConstants.setComponent(style, createPotentialHill());
			}
			else if (PageDNAScroller.class.getName() == clazz) {
				StyleConstants.setComponent(style, createDNAScroller());
			}
			else if (PageElectronicStructureViewer.class.getName() == clazz) {
				StyleConstants.setComponent(style, createElectronicStructureViewer());
			}
			else if (PageDiffractionInstrument.class.getName() == clazz) {
				StyleConstants.setComponent(style, createDiffractionInstrument());
			}
			else if (PagePhotonSpectrometer.class.getName() == clazz) {
				StyleConstants.setComponent(style, createPhotonSpectrometer());
			}
			else if (PagePeriodicTable.class.getName() == clazz) {
				StyleConstants.setComponent(style, createPeriodicTable());
			}
			else if (PageFunctionGraph.class.getName() == clazz) {
				StyleConstants.setComponent(style, createFunctionGraph());
			}
			else { // use reflection to construct unknown component
				try {
					Class<?> c = Class.forName(clazz);
					Object instance = c.newInstance();
					try {
						Method method = c.getMethod("init", (Class[]) null);
						if (method != null)
							method.invoke(instance, (Object[]) null);
					}
					catch (NoSuchMethodException e) {
					}
					if (instance instanceof Component)
						StyleConstants.setComponent(style, (Component) instance);
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

		private PageTextBox createTextBox() {
			PageTextBox t = new PageTextBox();
			if (titleText != null && titleText.trim().length() >= 6) {
				t.setContentType(titleText.trim().substring(0, 6).toLowerCase().equals("<html>") ? "text/html"
						: "text/plain");
			}
			else {
				t.setContentType("text/plain");
			}
			t.setIndex(indexOfComponent);
			t.setPage(page);
			if (titleText != null) {
				t.decodeText(titleText);
				t.setOriginalText(titleText);
				titleText = null;
			}
			if (!opaque) {
				t.setOpaque(false);
				opaque = true;
			}
			else {
				if ((argb1 | ARGB_NOT_SET) != ARGB_NOT_SET) {
					t.setBackground(new Color(argb1));
					argb1 = ARGB_NOT_SET;
				}
			}
			if (borderType != null) {
				t.setBorderType(borderType);
				t.putClientProperty("border", borderType);
				borderType = null;
			}
			t.setChangable(page.isEditable());
			if (width > 0 && height > 0) {
				if (width > 1 && height > 1) {
					t.setPreferredSize(new Dimension((int) width, (int) height));
				}
				else {
					// width and height are specified as percentages to the page's size
					if (width < 1.05f) {
						t.setWidthRelative(true);
						t.setWidthRatio(width);
						width *= page.getWidth();
					}
					if (height < 1.05f) {
						t.setHeightRelative(true);
						t.setHeightRatio(height);
						height *= page.getHeight();
					}
					t.setPreferredSize(new Dimension((int) width, (int) height));
				}
				width = height = 0;
			}
			if (ConnectionManager.sharedInstance().isCachingAllowed()) {
				if (page.isRemote())
					t.cacheLinkedFiles(page.getPathBase());
			}
			htmlComponentConnector.enroll(t);
			indexOfComponent++;
			return t;
		}

		private PageFunctionGraph createFunctionGraph() {
			PageFunctionGraph g = new PageFunctionGraph();
			g.setPage(page);
			g.setChangable(page.isEditable());
			if (width > 0 && height > 0) {
				g.setPreferredSize(new Dimension((int) width, (int) height));
				width = height = 0;
			}
			if (borderType != null) {
				g.setBorderType(borderType);
				borderType = null;
			}
			if (!opaque) {
				g.setOpaque(false);
				opaque = true;
			}
			else {
				g.setOpaque(true);
				if ((argb1 | ARGB_NOT_SET) != ARGB_NOT_SET) {
					g.setBackground(new Color(argb1));
					argb1 = ARGB_NOT_SET;
				}
			}
			if (dataWindow_xmin < dataWindow_xmax && dataWindow_ymin < dataWindow_ymax) {
				g.setScope((float) dataWindow_xmin, (float) dataWindow_xmax, (float) dataWindow_ymin,
						(float) dataWindow_ymax);
				dataWindow_xmin = dataWindow_xmax = dataWindow_ymin = dataWindow_ymax = 0.0;
			}
			if (dataSourceList != null && !dataSourceList.isEmpty()) {
				for (DataSource ds : dataSourceList)
					g.addDataSource(ds);
				dataSourceList.clear();
			}
			return g;
		}

		private PagePeriodicTable createPeriodicTable() {
			PagePeriodicTable t = new PagePeriodicTable();
			t.setPage(page);
			if (!mute) {
				t.mute(false);
				mute = true;
			}
			if (!opaque) {
				t.setOpaque(false);
				opaque = true;
			}
			else {
				if ((argb1 | ARGB_NOT_SET) != ARGB_NOT_SET) {
					t.setBackground(new Color(argb1));
					argb1 = ARGB_NOT_SET;
				}
			}
			if ((argb2 | ARGB_NOT_SET) != ARGB_NOT_SET) {
				t.setForeground(new Color(argb2));
				argb2 = ARGB_NOT_SET;
			}
			if (borderType != null) {
				t.setBorderType(borderType);
				borderType = null;
			}
			t.setChangable(page.isEditable());
			argb1 = ARGB_NOT_SET;
			return t;
		}

		private PageFeedbackArea createFeedbackArea() {
			PageFeedbackArea fa = new PageFeedbackArea();
			fa.setPage(page);
			if (borderType != null) {
				fa.setBorderType(borderType);
				borderType = null;
			}
			else {
				fa.setBorderType(null);
			}
			if (!opaque) {
				fa.setOpaque(false);
				opaque = true;
			}
			fa.setChangable(page.isEditable());
			if (width > 0 && height > 0) {
				if (width > 1 && height > 1) {
					fa.setPreferredSize(new Dimension((int) width, (int) height));
				}
				else {
					// width and height are specified as percentages to the page's size
					if (width < 1.05f) {
						fa.setWidthRelative(true);
						fa.setWidthRatio(width);
						width *= page.getWidth();
					}
					if (height < 1.05f) {
						fa.setHeightRelative(true);
						fa.setHeightRatio(height);
						height *= page.getHeight();
					}
					fa.setPreferredSize(new Dimension((int) width, (int) height));
				}
				width = height = 0;
			}
			// do not update the data if the page currently displays a local file
			if (page.isRemote())
				fa.updateData();
			return fa;
		}

		private SearchTextField createSearchTextField() {
			SearchTextField t = new SearchTextField();
			t.setPage(page);
			t.setIndex(indexOfComponent);
			if (type >= 0) {
				t.setDatabaseType(type);
				type = -1;
			}
			t.setChangable(page.isEditable());
			String key = page.getAddress() + "#" + ModelerUtilities.getSortableString(indexOfComponent, 3) + "%"
					+ t.getClass().getName();
			QuestionAndAnswer qa = UserData.sharedInstance().getData(key);
			if (qa != null) {
				t.setCategory(qa.getQuestion());
				t.setText(qa.getAnswer());
			}
			if (ncol > 0 && ncol != t.getColumns()) {
				t.setColumns(ncol);
				ncol = 0;
			}
			if (hasMenu) {
				t.addCategoryComboBox(true);
				hasMenu = false;
			}
			indexOfComponent++;
			return t;
		}

		private PagePhotonSpectrometer createPhotonSpectrometer() {
			PagePhotonSpectrometer s = new PagePhotonSpectrometer();
			s.setPage(page);
			s.setModelID(modelIndex);
			if (type != -1) {
				s.setType(type);
				type = -1;
			}
			if (borderType != null) {
				s.setBorderType(borderType);
				borderType = null;
			}
			if (width > 0 && height > 0) {
				s.setPreferredSize(new Dimension((int) width, (int) height));
				width = height = 0;
			}
			if (nstep > 0) {
				s.setNumberOfTicks(nstep);
				nstep = 0;
			}
			if (maximum > 0) {
				s.setUpperBound((float) maximum);
				maximum = 0;
			}
			if (minimum > 0) {
				s.setLowerBound((float) minimum);
				minimum = 0;
			}
			s.setChangable(page.isEditable());
			mw2dConnector.linkModelListener(modelIndex, s);
			return s;
		}

		private PageDiffractionInstrument createDiffractionInstrument() {
			PageDiffractionInstrument i = new PageDiffractionInstrument();
			i.setPage(page);
			i.setModelID(modelIndex);
			if (type != -1) {
				i.setType(type);
				type = -1;
			}
			if (loadScan) {
				i.setLoadScan(true);
				loadScan = false;
			}
			if (scriptScan) {
				i.setScriptScan(true);
				scriptScan = false;
			}
			if (levelOfDetails != 0) {
				i.setLevelOfDetails(levelOfDetails);
				levelOfDetails = 0;
			}
			if (scale != 8) {
				i.setScale((int) scale);
				scale = 8;
			}
			else {
				i.setScale(8);
			}
			if (borderType != null) {
				i.setBorderType(borderType);
				borderType = null;
			}
			i.setChangable(page.isEditable());
			mw2dConnector.linkModelListener(modelIndex, i);
			return i;
		}

		private PageElectronicStructureViewer createElectronicStructureViewer() {
			PageElectronicStructureViewer s = new PageElectronicStructureViewer();
			s.setModelID(modelIndex);
			s.setPage(page);
			if (titleText != null) {
				s.setTitle(titleText);
				titleText = null;
			}
			if (type >= 0) {
				s.setElementID(type);
				type = -1;
			}
			if (lockEnergyLevel) {
				s.setLockEnergyLevels(lockEnergyLevel);
				lockEnergyLevel = false;
			}
			if (nstep > 0) {
				s.setNumberOfTicks(nstep);
				nstep = 0;
			}
			if (drawTicks) {
				s.setDrawTicks(drawTicks);
				drawTicks = false;
			}
			s.setUpperBound((float) maximum);
			s.setLowerBound((float) minimum);
			if (borderType != null) {
				s.setBorderType(borderType);
				borderType = null;
			}
			if (!opaque) {
				s.setOpaque(false);
				opaque = true;
			}
			else {
				if ((argb1 | ARGB_NOT_SET) != ARGB_NOT_SET) {
					s.setBackground(new Color(argb1));
					argb1 = ARGB_NOT_SET;
				}
			}
			if ((argb2 | ARGB_NOT_SET) != ARGB_NOT_SET) {
				s.setForeground(new Color(argb2));
				argb2 = ARGB_NOT_SET;
			}
			s.setChangable(page.isEditable());
			if (width > 0 && height > 0) {
				s.setPreferredSize(new Dimension((int) width, (int) height + s.getVerticalMargin() * 2));
				width = height = 0;
			}
			mw2dConnector.linkModelListener(modelIndex, s);
			return s;
		}

		private PageDNAScroller createDNAScroller() {
			PageDNAScroller s = new PageDNAScroller();
			s.setPage(page);
			s.setModelID(modelIndex);
			if (borderType != null) {
				s.setBorderType(borderType);
				borderType = null;
			}
			if (!opaque) {
				s.setOpaque(false);
				opaque = true;
			}
			else {
				if ((argb1 | ARGB_NOT_SET) != ARGB_NOT_SET) {
					s.setBackground(new Color(argb1));
					argb1 = ARGB_NOT_SET;
				}
			}
			if (selectedIndex >= 0) {
				s.setProteinID(selectedIndex);
				selectedIndex = 0;
			}
			s.setChangable(page.isEditable());
			if (width > 0 && height > 0) {
				s.setPreferredSize(new Dimension((int) width, (int) height));
				width = height = 0;
			}
			mw2dConnector.linkModelListener(modelIndex, s);
			return s;
		}

		private PagePotentialHill createPotentialHill() {
			PagePotentialHill h = new PagePotentialHill();
			h.setModelID(modelIndex);
			h.setPage(page);
			if (!opaque) {
				h.setOpaque(false);
				opaque = true;
			}
			if ((argb1 | ARGB_NOT_SET) != ARGB_NOT_SET) {
				h.setColor(new Color(argb1));
				argb1 = ARGB_NOT_SET;
			}
			if (borderType != null) {
				h.setBorderType(borderType);
				borderType = null;
			}
			h.setChangable(page.isEditable());
			h.setMaximumDepth((float) maximum);
			h.setOwner(description);
			description = null;
			if (width > 0 && height > 0) {
				h.setPreferredSize(new Dimension((int) width, (int) height));
				width = height = 0;
			}
			mw2dConnector.linkModelListener(modelIndex, h);
			return h;
		}

		private PagePotentialWell createPotentialWell() {
			PagePotentialWell w = new PagePotentialWell();
			w.setPage(page);
			w.setModelID(modelIndex);
			if (!opaque) {
				w.setOpaque(false);
				opaque = true;
			}
			if ((argb1 | ARGB_NOT_SET) != ARGB_NOT_SET) {
				w.setColor(new Color(argb1));
				argb1 = ARGB_NOT_SET;
			}
			if (borderType != null) {
				w.setBorderType(borderType);
				borderType = null;
			}
			w.setChangable(page.isEditable());
			w.setMaximumDepth((float) maximum);
			w.setOwner(description);
			description = null;
			if (width > 0 && height > 0) {
				w.setPreferredSize(new Dimension((int) width, (int) height));
				width = height = 0;
			}
			mw2dConnector.linkModelListener(modelIndex, w);
			return w;
		}

		private PageXYGraph createXYGraph() {
			PageXYGraph b = new PageXYGraph();
			b.setPage(page);
			b.setModelID(modelIndex);
			if ((argb1 | ARGB_NOT_SET) != ARGB_NOT_SET) {
				b.getGraph().setGraphBackground(new Color(argb1));
				argb1 = ARGB_NOT_SET;
			}
			if ((argb2 | ARGB_NOT_SET) != ARGB_NOT_SET) {
				b.getGraph().setDataBackground(new Color(argb2));
				argb2 = ARGB_NOT_SET;
			}
			if (legendX != -1 && legendY != -1) {
				b.setLegendLocation(legendX, legendY);
				legendX = legendY = -1;
			}
			if (showHLines) {
				b.setDrawGrid(true);
				showHLines = false;
			}
			b.setLabelForXAxis(xAxisTitle);
			b.setLabelForYAxis(yAxisTitle);
			if (timeSeries_x != null) {
				if (timeSeries_x.endsWith("(eV)")) {
					int iev = timeSeries_x.lastIndexOf(" (eV)");
					if (iev != -1)
						timeSeries_x = timeSeries_x.substring(0, iev);
				}
				b.setDescription(0, timeSeries_x);
				timeSeries_x = null;
				if (multiplier != 1.0f) {
					b.setXMultiplier(multiplier);
					multiplier = 1.0f;
				}
				if (addend != 0.0f) {
					b.setXAddend(addend);
					addend = 0.0f;
				}
			}
			if (timeSeries_y != null) {
				for (int i = 0; i < timeSeries_y.length; i++) {
					if (timeSeries_y[i] != null) {
						if (timeSeries_y[i].endsWith("(eV)")) {
							int iev = timeSeries_y[i].lastIndexOf(" (eV)");
							if (iev != -1)
								timeSeries_y[i] = timeSeries_y[i].substring(0, iev);
						}
						else if (timeSeries_y[i].equals("Concentration A2(%)")) {
							timeSeries_y[i] = "Mole Fraction A2(%)";
						}
						else if (timeSeries_y[i].equals("Concentration B2(%)")) {
							timeSeries_y[i] = "Mole Fraction B2(%)";
						}
						else if (timeSeries_y[i].equals("Concentration AB(%)")) {
							timeSeries_y[i] = "Mole Fraction AB(%)";
						}
						b.setDescription(i + 1, timeSeries_y[i]);
						timeSeries_y[i] = null;
					}
					if (lineColor != null && lineColor[i] != null) {
						b.setLineColor(i, lineColor[i]);
						lineColor[i] = null;
					}
					if (lineSymbol != null) {
						b.setLineSymbol(i, lineSymbol[i]);
						lineSymbol[i] = LineSymbols.SYMBOL_NUMBER_1;
					}
					if (lineStyle != null) {
						b.setLineStyle(i, lineStyle[i]);
						lineStyle[i] = LineStyle.STROKE_NUMBER_1;
					}
					if (lineWidth != null) {
						b.setLineWidth(i, lineWidth[i]);
						lineWidth[i] = LineWidth.STROKE_WIDTH_1;
					}
					if (symbolSize != null) {
						b.setSymbolSize(i, symbolSize[i]);
						symbolSize[i] = 4;
					}
					if (symbolSpacing != null) {
						b.setSymbolSpacing(i, symbolSpacing[i]);
						symbolSpacing[i] = 5;
					}
					if (multiplier_y != null) {
						b.setMultiplier(i, multiplier_y[i]);
						multiplier_y[i] = 1.0f;
					}
					if (addend_y != null) {
						b.setAddend(i, addend_y[i]);
						addend_y[i] = 0.0f;
					}
					if (smoothers != null) {
						b.setSmoothFilter(i, smoothers[i]);
						smoothers[i] = 0;
					}
				}
			}
			if (!autoFit) {
				b.setAutoScale(false);
				b.setDataWindowXmin(dataWindow_xmin);
				b.setDataWindowXmax(dataWindow_xmax);
				b.setDataWindowYmin(dataWindow_ymin);
				b.setDataWindowYmax(dataWindow_ymax);
				autoFit = true;
				dataWindow_xmin = dataWindow_xmax = dataWindow_ymin = dataWindow_ymax = 0.0;
			}
			if (!autoFitX) {
				b.setAutoScaleX(false);
				b.setDataWindowXmin(dataWindow_xmin);
				b.setDataWindowXmax(dataWindow_xmax);
				autoFitX = true;
				dataWindow_xmin = dataWindow_xmax = 0.0;
			}
			if (!autoFitY) {
				b.setAutoScaleY(false);
				b.setDataWindowYmin(dataWindow_ymin);
				b.setDataWindowYmax(dataWindow_ymax);
				autoFitY = true;
				dataWindow_ymin = dataWindow_ymax = 0.0;
			}
			if (!autoUpdate) {
				b.setAutoUpdate(false);
				autoUpdate = true;
			}
			if (width > 0 && height > 0) {
				b.setPreferredSize(new Dimension((int) width, (int) height));
				width = height = 0;
			}
			if (showMenuBar) {
				b.addMenuBar();
			}
			else {
				b.removeMenuBar();
				showMenuBar = true;
			}
			if (showToolBar) {
				b.addToolBar();
			}
			else {
				b.removeToolBar();
				showToolBar = true;
			}
			if (borderType != null) {
				b.setBorderType(borderType);
				borderType = null;
			}
			b.setChangable(page.isEditable());
			mw2dConnector.linkModelListener(modelIndex, b);
			return b;
		}

		private PageBarGraph createBarGraph() {
			PageBarGraph b = new PageBarGraph();
			b.setPage(page);
			b.setModelID(modelIndex);
			b.setValue(value);
			b.setInitialValue(value);
			b.setMinimum(minimum);
			b.setMaximum(maximum);
			if (dataType != 0) {
				b.setAverageType((byte) dataType);
				dataType = 0;
				switch (b.getAverageType()) {
				case PageBarGraph.EXPONENTIAL_RUNNING_AVERAGE:
					if (smoothingFactor > 0) {
						b.setSmoothingFactor(smoothingFactor);
						smoothingFactor = -1;
					}
					break;
				case PageBarGraph.SIMPLE_RUNNING_AVERAGE:
					if (samplingPoints > 0) {
						b.setSamplingPoints(samplingPoints);
						samplingPoints = -1;
					}
					break;
				}
			}
			if (average) {
				b.setAverageOnly(average);
				average = false;
			}
			if (orientation == PageBarGraph.VERTICAL || orientation == PageBarGraph.HORIZONTAL) {
				b.setOrientation(orientation);
				orientation = -1;
			}
			if ((argb1 | ARGB_NOT_SET) != ARGB_NOT_SET) {
				b.setBackground(new Color(argb1));
				argb1 = ARGB_NOT_SET;
			}
			if ((argb2 | ARGB_NOT_SET) != ARGB_NOT_SET) {
				b.setForeground(new Color(argb2));
				argb2 = ARGB_NOT_SET;
			}
			b.setPaintTicks(drawTicks);
			b.setPaintLabels(drawLabels);
			if (format != null) {
				b.setFormat(format);
				format = null;
			}
			if (maxFractionDigit != 3) {
				b.setMaximumFractionDigits(maxFractionDigit);
				maxFractionDigit = 3;
			}
			if (maxIntegerDigit != 3) {
				b.setMaximumIntegerDigits(maxIntegerDigit);
				maxIntegerDigit = 3;
			}
			if (multiplier != 1.0f) {
				b.setMultiplier(multiplier);
				multiplier = 1.0f;
			}
			if (addend != 0.0f) {
				b.setAddend(addend);
				addend = 0.0f;
			}
			if (titleText != null) {
				b.setPaintTitle(titleText.equalsIgnoreCase("true"));
				titleText = null;
			}
			if (nstep > 0) {
				b.setMinorTicks(nstep);
				nstep = 0;
			}
			if (majorTicks > 0) {
				b.setMajorTicks(majorTicks);
				majorTicks = 0;
			}
			if (description != null) {
				b.setDescription(description);
				description = null;
			}
			if (timeSeries != null) {
				b.setTimeSeriesName(timeSeries);
				timeSeries = null;
			}
			else {
				b.setTimeSeriesName(b.getDescription());
			}
			if (width > 0 && height > 0) {
				b.setPreferredSize(new Dimension((int) width, (int) height));
				width = height = 0;
			}
			b.setChangable(page.isEditable());
			mw2dConnector.linkModelListener(modelIndex, b);
			drawTicks = drawLabels = false;
			return b;
		}

		private PageNumericBox createNumericBox() {
			PageNumericBox b = new PageNumericBox();
			b.setPage(page);
			b.setModelID(modelIndex);
			if ((argb2 | ARGB_NOT_SET) != ARGB_NOT_SET) {
				b.setForeground(new Color(argb2));
				argb2 = ARGB_NOT_SET;
			}
			if (description != null) {
				if (description.endsWith("(eV)")) {
					int iev = description.lastIndexOf(" (eV)");
					if (iev != -1)
						description = description.substring(0, iev);
				}
				else if (description.equals("Concentration A2(%)")) {
					description = "Mole Fraction A2(%)";
				}
				else if (description.equals("Concentration B2(%)")) {
					description = "Mole Fraction B2(%)";
				}
				else if (description.equals("Concentration AB(%)")) {
					description = "Mole Fraction AB(%)";
				}
				b.setDescription(description);
				description = null;
			}
			if (dataType != 0) {
				b.setDataType(dataType);
				dataType = 0;
			}
			if (multiplier != 1.0f) {
				b.setMultiplier(multiplier);
				multiplier = 1.0f;
			}
			if (addend != 0.0f) {
				b.setAddend(addend);
				addend = 0.0f;
			}
			if (format != null) {
				b.setFormat(format);
				format = null;
			}
			if (maxFractionDigit != 3) {
				b.setMaximumFractionDigits(maxFractionDigit);
				maxFractionDigit = 3;
			}
			if (maxIntegerDigit != 3) {
				b.setMaximumIntegerDigits(maxIntegerDigit);
				maxIntegerDigit = 3;
			}
			if (fontName != null) {
				b.setFontName(fontName);
				fontName = null;
			}
			if (fontSize > 0) {
				b.setFontSize(fontSize);
				fontSize = 0;
			}
			if (width > 0 && height > 0) {
				b.setPreferredSize(new Dimension((int) width, (int) height));
				width = height = 0;
			}
			if (borderType != null) {
				b.setBorderType(borderType);
				borderType = null;
			}
			b.setChangable(page.isEditable());
			mw2dConnector.linkModelListener(modelIndex, b);
			return b;
		}

		private PageMultipleChoice createMultipleChoice() {
			PageMultipleChoice m = new PageMultipleChoice();
			m.setIndex(indexOfComponent);
			m.setPage(page);
			m.setSingleSelection(singleSelection);
			m.changeNumberOfChoices(nrow == 0 ? 4 : nrow);
			for (int i = 0; i < nrow; i++)
				m.setChoice(i, choices[i]);
			if (answer != null) {
				StringTokenizer st = new StringTokenizer(answer);
				int[] key = new int[st.countTokens()];
				int k = 0;
				while (st.hasMoreTokens()) {
					Integer ig = Parser.parseInt(st.nextToken());
					if (ig != null)
						key[k++] = ig.intValue();
				}
				m.setAnswer(key);
				answer = null;
			}
			if (titleText != null) {
				m.setQuestion(titleText);
				titleText = null;
			}
			if (!opaque) {
				m.setOpaque(false);
				opaque = true;
			}
			else {
				if ((argb1 | ARGB_NOT_SET) != ARGB_NOT_SET) {
					m.setBackground(new Color(argb1));
					argb1 = ARGB_NOT_SET;
				}
			}
			if (script != null) {
				m.stringToScripts(script);
				script = null;
			}
			if (clear) {
				m.addClearAnswerButton();
				clear = false;
			}
			else {
				m.removeClearAnswerButton();
			}
			if (submit) {
				m.addCheckAnswerButton();
				submit = false;
			}
			else {
				m.removeCheckAnswerButton();
			}
			if (borderType != null) {
				m.setBorderType(borderType);
				borderType = null;
			}
			m.setChangable(page.isEditable());
			if (width > 0 && height > 0) {
				m.setPreferredSize(new Dimension((int) width, (int) height));
				width = height = 0;
			}
			if (ConnectionManager.sharedInstance().isCachingAllowed()) {
				if (page.isRemote())
					m.cacheLinkedFiles(page.getPathBase());
			}
			m.clearSelection();
			String key = page.getAddress() + "#" + ModelerUtilities.getSortableString(indexOfComponent, 3) + "%"
					+ m.getClass().getName();
			QuestionAndAnswer qa = UserData.sharedInstance().getData(key);
			if (qa != null) {
				answer = qa.getAnswer();
				if (answer != null && !answer.equals("-1")) {
					StringTokenizer st = new StringTokenizer(answer);
					int[] val = new int[st.countTokens()];
					int k = 0;
					while (st.hasMoreTokens()) {
						Integer ig = Parser.parseInt(st.nextToken());
						if (ig != null)
							val[k++] = ig.intValue();
					}
					if (val.length > 0) {
						for (int i : val)
							m.setSelected(i, true);
					}
					answer = null;
				}
			}
			else {
				qa = new QuestionAndAnswer(m.getQuestion() + '\n' + m.formatChoices() + "\nMy answer is ", "-1", m
						.answerToString());
				UserData.sharedInstance().putData(key, qa);
			}
			htmlComponentConnector.enroll(m.getQuestionTextBox());
			indexOfComponent++;
			iChoice = 0;
			singleSelection = true;
			nrow = 0;
			description = null;
			return m;
		}

		private ImageQuestion createImageQuestion() {
			ImageQuestion iq = new ImageQuestion();
			iq.setPage(page);
			iq.setIndex(indexOfComponent);
			if (width > 0 && height > 0) {
				iq.setPreferredSize(new Dimension((int) width, (int) height));
				width = height = 0;
			}
			if (titleText != null) {
				iq.setQuestion(titleText);
				titleText = null;
			}
			iq.setChangable(page.isEditable());
			String key = page.getAddress() + "#" + ModelerUtilities.getSortableString(indexOfComponent, 3) + "%"
					+ iq.getClass().getName();
			QuestionAndAnswer qa = UserData.sharedInstance().getData(key);
			if (qa != null) {
				if (!QuestionAndAnswer.NO_ANSWER.equals(qa.getAnswer()))
					iq.setImage(qa.getAnswer());
			}
			else {
				qa = new QuestionAndAnswer(iq.getQuestion(), QuestionAndAnswer.NO_ANSWER);
				UserData.sharedInstance().putData(key, qa);
			}
			if (!opaque) {
				iq.setOpaque(false);
				opaque = true;
			}
			else {
				iq.setOpaque(true);
				if ((argb1 | ARGB_NOT_SET) != ARGB_NOT_SET) {
					iq.setBackground(new Color(argb1));
					argb1 = ARGB_NOT_SET;
				}
				else {
					iq.setBackground(page.getBackground());
				}
			}
			if (borderType != null) {
				iq.setBorderType(borderType);
				borderType = null;
			}
			if (ConnectionManager.sharedInstance().isCachingAllowed()) {
				if (page.isRemote())
					iq.cacheLinkedFiles(page.getPathBase());
			}
			htmlComponentConnector.enroll(iq.getQuestionTextBox());
			indexOfComponent++;
			return iq;
		}

		private PageTextArea createTextArea() {
			PageTextArea t = new PageTextArea();
			t.setPage(page);
			t.setIndex(indexOfComponent);
			if (titleText != null) {
				t.setTitle(titleText);
				titleText = null;
			}
			if (description != null) {
				t.setReferenceAnswer(description);
				description = null;
			}
			t.setChangable(page.isEditable());
			String key = page.getAddress() + "#" + ModelerUtilities.getSortableString(indexOfComponent, 3) + "%"
					+ t.getClass().getName();
			QuestionAndAnswer qa = UserData.sharedInstance().getData(key);
			if (qa != null) {
				if (!QuestionAndAnswer.NO_ANSWER.equals(qa.getAnswer()))
					t.setText(qa.getAnswer());
			}
			else {
				qa = new QuestionAndAnswer(t.getTitle(), QuestionAndAnswer.NO_ANSWER, t.getReferenceAnswer());
				UserData.sharedInstance().putData(key, qa);
			}
			if (width > 0 && height > 0) {
				t.setPreferredSize(new Dimension((int) width, (int) height));
				width = height = 0;
			}
			else { // backward compatible
				if (nrow > 0 && nrow != t.getRows()) {
					t.setRows(nrow);
					nrow = 0;
				}
				if (ncol > 0 && ncol != t.getColumns()) {
					t.setColumns(ncol);
					ncol = 0;
				}
			}
			if (!opaque) {
				t.setOpaque(false);
				opaque = true;
			}
			else {
				t.setOpaque(true);
				if ((argb1 | ARGB_NOT_SET) != ARGB_NOT_SET) {
					t.setBackground(new Color(argb1));
					argb1 = ARGB_NOT_SET;
				}
				else {
					t.setBackground(page.getBackground());
				}
			}
			if (borderType != null) {
				t.setBorderType(borderType);
				borderType = null;
			}
			if (ConnectionManager.sharedInstance().isCachingAllowed()) {
				if (page.isRemote())
					t.cacheLinkedFiles(page.getPathBase());
			}
			htmlComponentConnector.enroll(t.getQuestionTextBox());
			indexOfComponent++;
			return t;
		}

		private PageTextField createTextField() {
			PageTextField t = new PageTextField();
			t.setPage(page);
			t.setIndex(indexOfComponent);
			if (titleText != null) {
				t.setTitle(titleText);
				titleText = null;
			}
			if (description != null) {
				t.setReferenceAnswer(description);
				description = null;
			}
			t.setChangable(page.isEditable());
			String key = page.getAddress() + "#" + ModelerUtilities.getSortableString(indexOfComponent, 3) + "%"
					+ t.getClass().getName();
			QuestionAndAnswer qa = UserData.sharedInstance().getData(key);
			if (qa != null) {
				if (!QuestionAndAnswer.NO_ANSWER.equals(qa.getAnswer()))
					t.setText(qa.getAnswer());
			}
			else {
				qa = new QuestionAndAnswer(t.getTitle(), QuestionAndAnswer.NO_ANSWER, t.getReferenceAnswer());
				UserData.sharedInstance().putData(key, qa);
			}
			if (layout != null) {
				t.setQuestionPosition(layout);
				layout = null;
			}
			if (width > 0 && height > 0) {
				t.setPreferredSize(new Dimension((int) width, (int) height));
				width = height = 0;
			}
			else { // backward compatible
				if (ncol > 0) {
					t.setColumns(ncol);
					ncol = 0;
				}
				else {
					t.setColumns(t.getColumns());
				}
			}
			if (!opaque) {
				t.setOpaque(false);
				opaque = true;
			}
			else {
				t.setOpaque(true);
				if ((argb1 | ARGB_NOT_SET) != ARGB_NOT_SET) {
					t.setBackground(new Color(argb1));
					argb1 = ARGB_NOT_SET;
				}
				else {
					t.setBackground(page.getBackground());
				}
			}
			if (borderType != null) {
				t.setBorderType(borderType);
				borderType = null;
			}
			if (ConnectionManager.sharedInstance().isCachingAllowed()) {
				if (page.isRemote())
					t.cacheLinkedFiles(page.getPathBase());
			}
			htmlComponentConnector.enroll(t.getQuestionTextBox());
			indexOfComponent++;
			return t;
		}

		private PageTable createTable() {
			// back compatible
			if (description != null) {
				columnName = description;
				description = null;
			}
			PageTable t = new PageTable(tableValue, columnName != null, rowName != null);
			t.setPage(page);
			if (rowMargin != 10 && rowMargin != t.getRowMargin()) {
				t.setRowMargin(rowMargin);
				rowMargin = 10;
			}
			if (colMargin != 10 && colMargin != t.getIntercellSpacing().width) {
				t.setIntercellSpacing(new Dimension(colMargin, t.getRowMargin()));
				colMargin = 10;
			}
			t.setShowHorizontalLines(showHLines);
			t.setShowVerticalLines(showVLines);
			if (borderType != null) {
				t.setBorderType(borderType);
				borderType = null;
			}
			t.setChangable(page.isEditable());
			if (width > 0 && height > 0) {
				if (width < 1.05f) {
					t.setWidthRelative(true);
					t.setWidthRatio(width);
					width *= page.getWidth();
				}
				if (height < 1.05f) {
					t.setHeightRelative(true);
					t.setHeightRatio(height);
					height *= page.getHeight();
				}
				t.setPreferredSize(new Dimension((int) width, (int) height));
				width = height = 0;
			}
			t.setRowHeight((int) ((float) t.getTableBodyHeight() / (float) nrow));
			if ((argb2 | ARGB_NOT_SET) != ARGB_NOT_SET) {
				t.setGridColor(new Color(argb2));
				argb2 = ARGB_NOT_SET;
			}
			if ((argb1 | ARGB_NOT_SET) != ARGB_NOT_SET) {
				t.setTableBackground(new Color(argb1));
				argb1 = ARGB_NOT_SET;
			}
			if (columnName != null) {
				t.setColumnNames(columnName);
				columnName = null;
			}
			if (rowName != null) {
				t.setRowNames(rowName);
				rowName = null;
			}
			if (layout != null) {
				t.setColumnWidths(layout);
				layout = null;
			}
			t.setCellAlignment(cellAlignment);
			t.setOpaque(opaque);
			tableCellIndex = 0;
			nrow = ncol = 0;
			showVLines = showHLines = false;
			opaque = true;
			cellAlignment = SwingConstants.LEFT;
			return t;
		}

		private void connect(ModelCommunicator mc) {
			mc.setModelID(modelIndex);
			if (modelClass != null) {
				mc.setModelClass(modelClass);
				if (modelClass.equals(PageMolecularViewer.class.getName())) {
					jmolConnector.linkModelListener(modelIndex, mc);
				}
				else if (modelClass.equals(PageMd3d.class.getName())) {
					mw3dConnector.linkModelListener(modelIndex, mc);
				}
				else if (modelClass.equals(PageJContainer.class.getName())
						|| modelClass.equals(PageApplet.class.getName())) {
					pluginConnector.linkModelCommunicator(modelIndex, mc);
				}
				else {
					mw2dConnector.linkModelListener(modelIndex, mc);
				}
				modelClass = null;
			}
			else {
				mw2dConnector.linkModelListener(modelIndex, mc); // backward compatiblity
			}
		}

		private PageSpinner createSpinner() {
			PageSpinner s = new PageSpinner();
			s.setPage(page);
			connect(s);
			s.setMinimum(minimum);
			s.setMaximum(maximum);
			if (stepsize != 0) {
				s.setStepSize(stepsize);
				stepsize = 0;
			}
			s.setValue(value);
			s.putClientProperty("value", value);
			s.setName(changeName);
			s.setChangable(page.isEditable());
			if (titleText != null) {
				s.setLabel(titleText);
				titleText = null;
			}
			else {
				s.setLabel(changeName);
			}
			if (toolTip != null) {
				s.setToolTipText(toolTip);
				toolTip = null;
			}
			s.autoSize();
			if (disabledAtRun) {
				s.setDisabledAtRun(disabledAtRun);
				disabledAtRun = false;
			}
			if (disabledAtScript) {
				s.setDisabledAtScript(disabledAtScript);
				disabledAtScript = false;
			}
			if (script != null) {
				s.setScript(script);
				script = null;
			}
			changeName = null;
			return s;
		}

		private PageSlider createSlider() {
			PageSlider s = new PageSlider();
			s.setPage(page);
			connect(s);
			if (orientation == PageSlider.VERTICAL || orientation == PageSlider.HORIZONTAL) {
				s.setOrientation(orientation);
				orientation = -1;
			}
			s.setName(changeName);
			s.setDoubleMinimum(minimum);
			s.setDoubleMaximum(maximum);
			s.setDoubleValue(value);
			s.setNumberOfSteps(nstep);
			s.adjustScale();
			s.putClientProperty("value", s.getValue());
			s.setPaintTicks(drawTicks);
			if (drawTicks)
				drawTicks = false;
			if (titleText != null) {
				s.setTitle(titleText);
				titleText = null;
			}
			if (toolTip != null) {
				s.setToolTipText(toolTip);
				toolTip = null;
			}
			if (labeltable != null) {
				s.setupLabels(labeltable);
				s.putClientProperty("Label", labeltable);
				labeltable = null;
			}
			if (borderType != null) {
				s.setBorderType(borderType);
				borderType = null;
			}
			s.setOpaque(opaque);
			if (!opaque) {
				opaque = true;
			}
			else {
				if ((argb1 | ARGB_NOT_SET) != ARGB_NOT_SET) {
					s.setBackground(new Color(argb1));
					argb1 = ARGB_NOT_SET;
				}
			}
			if (disabledAtRun) {
				s.setDisabledAtRun(disabledAtRun);
				disabledAtRun = false;
			}
			if (disabledAtScript) {
				s.setDisabledAtScript(disabledAtScript);
				disabledAtScript = false;
			}
			s.setChangable(page.isEditable());
			if (width > 0 && height > 0) {
				s.setPreferredSize(new Dimension((int) width, (int) height));
				width = height = 0;
			}
			if (script != null) {
				s.putClientProperty("Script", script);
				script = null;
			}
			changeName = null;
			return s;
		}

		private PageCheckBox createCheckBox() {
			PageCheckBox b = new PageCheckBox();
			b.setPage(page);
			connect(b);
			if (!transparent) {
				b.setOpaque(true);
				transparent = true;
				if ((argb1 | ARGB_NOT_SET) != ARGB_NOT_SET) {
					b.setBackground(new Color(argb1));
					argb1 = ARGB_NOT_SET;
				}
			}
			else {
				b.setOpaque(false);
			}
			if (selected) {
				b.setSelected(selected);
				b.putClientProperty("selected", Boolean.TRUE);
				selected = false;
			}
			b.setName(actionName);
			if (titleText != null) {
				b.setText(titleText);
				titleText = null;
			}
			else {
				b.setText(actionName);
			}
			if (toolTip != null) {
				b.setToolTipText(toolTip);
				toolTip = null;
			}
			b.setChangable(page.isEditable());
			if (width > 0 && height > 0) {
				b.setAutoSize(false);
				b.setPreferredSize(new Dimension((int) width, (int) height));
				width = height = 0;
			}
			if (disabledAtRun) {
				b.setDisabledAtRun(disabledAtRun);
				disabledAtRun = false;
			}
			if (disabledAtScript) {
				b.setDisabledAtScript(disabledAtScript);
				disabledAtScript = false;
			}
			if (script != null) {
				b.putClientProperty("selection script", script);
				script = null;
			}
			if (script2 != null) {
				b.putClientProperty("deselection script", script2);
				script2 = null;
			}
			actionName = null;
			return b;
		}

		private PageRadioButton createRadioButton() {
			PageRadioButton b = new PageRadioButton();
			if (groupID != 0) {
				b.setGroupID(groupID);
				groupID = 0;
			}
			radioButtonConnector.enroll(b);
			b.setPage(page);
			connect(b);
			if (!transparent) {
				b.setOpaque(true);
				transparent = true;
				if ((argb1 | ARGB_NOT_SET) != ARGB_NOT_SET) {
					b.setBackground(new Color(argb1));
					argb1 = ARGB_NOT_SET;
				}
			}
			else {
				b.setOpaque(false);
			}
			if (selected) {
				b.setSelected(selected);
				b.putClientProperty("selected", Boolean.TRUE);
				selected = false;
			}
			b.setName(actionName);
			if (titleText != null) {
				b.setText(titleText);
				titleText = null;
			}
			else {
				b.setText(actionName);
			}
			if (toolTip != null) {
				b.setToolTipText(toolTip);
				toolTip = null;
			}
			b.setChangable(page.isEditable());
			if (width > 0 && height > 0) {
				b.setAutoSize(false);
				b.setPreferredSize(new Dimension((int) width, (int) height));
				width = height = 0;
			}
			if (disabledAtRun) {
				b.setDisabledAtRun(disabledAtRun);
				disabledAtRun = false;
			}
			if (disabledAtScript) {
				b.setDisabledAtScript(disabledAtScript);
				disabledAtScript = false;
			}
			if (script != null) {
				b.putClientProperty("script", script);
				script = null;
			}
			actionName = null;
			return b;
		}

		private PageComboBox createComboBox() {
			PageComboBox cb = new PageComboBox();
			cb.setPage(page);
			connect(cb);
			if (actionName != null) {
				cb.setName(actionName);
				actionName = null;
			}
			if (toolTip != null) {
				cb.setToolTipText(toolTip);
				toolTip = null;
			}
			if (selectedIndex > 0) {
				cb.putClientProperty("Selected Index", new Integer(selectedIndex));
				selectedIndex = 0;
			}
			if (group != null) {
				cb.putClientProperty("Options", group);
				group = null;
			}
			if (script != null) {
				cb.putClientProperty("Script", script);
				script = null;
			}
			if (disabledAtRun) {
				cb.setDisabledAtRun(disabledAtRun);
				disabledAtRun = false;
			}
			if (disabledAtScript) {
				cb.setDisabledAtScript(disabledAtScript);
				disabledAtScript = false;
			}
			cb.setChangable(page.isEditable());
			return cb;
		}

		private PageButton createButton() {
			PageButton b = new PageButton();
			b.setPage(page);
			connect(b);
			if (borderType != null) {
				b.setBorderType(borderType);
				borderType = null;
			}
			if (!opaque) {
				b.setOpaque(false);
				opaque = true;
			}
			else {
				if ((argb1 | ARGB_NOT_SET) != ARGB_NOT_SET) {
					b.setBackground(new Color(argb1));
					argb1 = ARGB_NOT_SET;
				}
			}
			if (titleText != null) {
				b.setText(titleText);
				titleText = null;
			}
			else {
				b.setText(actionName);
			}
			if (toolTip != null) {
				b.setToolTipText(toolTip);
				toolTip = null;
			}
			b.setName(actionName);
			b.setChangable(page.isEditable());
			if (continuousFire) {
				b.setContinuousFire(continuousFire);
				continuousFire = false;
			}
			if (disabledAtRun) {
				b.setDisabledAtRun(disabledAtRun);
				disabledAtRun = false;
			}
			if (disabledAtScript) {
				b.setDisabledAtScript(disabledAtScript);
				disabledAtScript = false;
			}
			if (width > 0 && height > 0) {
				b.setAutoSize(false);
				b.setPreferredSize(new Dimension((int) width, (int) height));
				width = height = 0;
			}
			if (stepsize != 0) {
				b.putClientProperty("increment", new Double(stepsize));
				stepsize = 0;
			}
			if (script != null) {
				b.putClientProperty("script", script);
				script = null;
			}
			actionName = null;
			argb1 = ARGB_NOT_SET;
			return b;
		}

		private ActivityButton createActivityButton() {
			ActivityButton b = new ActivityButton();
			b.setPage(page);
			if (borderType != null) {
				b.setBorderType(borderType);
				borderType = null;
			}
			if (!opaque) {
				b.setOpaque(false);
				opaque = true;
			}
			else {
				if ((argb1 | ARGB_NOT_SET) != ARGB_NOT_SET) {
					b.setBackground(new Color(argb1));
					argb1 = ARGB_NOT_SET;
				}
			}
			Action a = null;
			if (actionName != null) {
				a = page.getActivityAction(actionName);
				if (a != null) {
					b.setAction(a);
					if (group != null) {
						b.setPageNameGroup(group);
						group = null;
					}
				}
			}
			b.setChangable(page.isEditable());
			if (titleText != null) {
				b.setText(titleText);
				titleText = null;
			}
			else {
				b.setText(actionName);
			}
			if (toolTip != null) {
				b.setToolTipText(toolTip);
				toolTip = null;
			}
			if (width > 0 && height > 0) {
				b.setAutoSize(false);
				b.setPreferredSize(new Dimension((int) width, (int) height));
				width = height = 0;
			}
			if (description != null) {
				b.setReportTitle(description);
				description = null;
			}
			if (hintText != null) {
				b.putClientProperty("hint", hintText);
				hintText = null;
			}
			if (script != null) {
				b.putClientProperty("script", script);
				script = null;
			}
			if (gradeURI != null) {
				b.putClientProperty("grade_uri", gradeURI);
				gradeURI = null;
			}
			actionName = null;
			return b;
		}

		private PageApplet createApplet() {
			PageApplet applet = new PageApplet();
			applet.setPage(page);
			applet.setIndex(indexOfApplet++);
			if (mainClass != null) {
				applet.setClassName(mainClass);
				mainClass = null;
			}
			if (jarNames != null) {
				for (String s : jarNames)
					applet.addJarName(s);
				jarNames.clear();
			}
			if (caching) {
				applet.setCachingAllowed(true);
				caching = false;
			}
			if (parameter != null) {
				applet.parseParameters(parameter);
				parameter = null;
			}
			if (width > 0 && height > 0) {
				applet.setPreferredSize(new Dimension((int) width, (int) height));
				width = height = 0;
			}
			if ((argb1 | ARGB_NOT_SET) != ARGB_NOT_SET) {
				applet.setBackground(new Color(argb1));
				argb1 = ARGB_NOT_SET;
			}
			if (borderType != null) {
				applet.setBorderType(borderType);
				borderType = null;
			}
			applet.setChangable(page.isEditable());
			pluginConnector.enroll(applet);
			return applet;
		}

		private PageJContainer createPlugin() {
			PageJContainer plugin = new PageJContainer();
			plugin.setPage(page);
			plugin.setIndex(indexOfPlugin++);
			if (codeBase != null) {
				plugin.setCodeBase(codeBase);
				codeBase = null;
			}
			if (cacheFiles != null) {
				plugin.setCachedFileNames(cacheFiles);
				cacheFiles = null;
			}
			if (mainClass != null) {
				plugin.setClassName(mainClass);
				mainClass = null;
			}
			if (jarNames != null) {
				for (String s : jarNames)
					plugin.addJarName(s);
				jarNames.clear();
			}
			if (parameter != null) {
				plugin.parseParameters(parameter);
				parameter = null;
			}
			if (width > 0 && height > 0) {
				plugin.setPreferredSize(new Dimension((int) width, (int) height));
				width = height = 0;
			}
			if ((argb1 | ARGB_NOT_SET) != ARGB_NOT_SET) {
				plugin.setBackground(new Color(argb1));
				argb1 = ARGB_NOT_SET;
			}
			if (borderType != null) {
				plugin.setBorderType(borderType);
				borderType = null;
			}
			plugin.setChangable(page.isEditable());
			pluginConnector.enroll(plugin);
			return plugin;
		}

		private AudioPlayer createAudioPlayer() {
			AudioPlayer player = new AudioPlayer();
			player.setPage(page);
			if (description != null) {
				player.setClipName(description);
				description = null;
			}
			if (titleText != null) {
				player.setText(titleText);
				titleText = null;
			}
			if (toolTip != null) {
				player.setToolTipText(toolTip);
				toolTip = null;
			}
			if ((argb1 | ARGB_NOT_SET) != ARGB_NOT_SET) {
				player.setBackground(new Color(argb1));
				argb1 = ARGB_NOT_SET;
			}
			if (borderType != null) {
				player.setBorderType(borderType);
				borderType = null;
			}
			player.setChangable(page.isEditable());
			return player;
		}

		private PageScriptConsole createScriptConsole() {
			PageScriptConsole sc = new PageScriptConsole();
			sc.setPage(page);
			connect(sc);
			if (borderType != null) {
				sc.setBorderType(borderType);
				borderType = null;
			}
			else {
				sc.setBorderType(null);
			}
			sc.setChangable(page.isEditable());
			if (width > 0 && height > 0) {
				sc.setPreferredSize(new Dimension((int) width, (int) height));
				width = height = 0;
			}
			return sc;
		}

		private PageMolecularViewer createMolecularViewer() {
			PageMolecularViewer mv = (PageMolecularViewer) InstancePool.sharedInstance().getUnusedInstance(
					PageMolecularViewer.class);
			jmolConnector.enroll(mv);
			mv.setPage(page);
			mv.reset();
			mv.setIndex(indexOfJmol++);
			mv.setChangable(page.isEditable());
			if (width > 0 && height > 0) {
				if (width > 1 && height > 1) {
					mv.setPreferredSize(new Dimension((int) width, (int) height));
				}
				else {
					// width and height are specified as percentages to the page's size
					if (width < 1.05f) {
						mv.setWidthRelative(true);
						mv.setWidthRatio(width);
						width *= page.getWidth();
					}
					if (height < 1.05f) {
						mv.setHeightRelative(true);
						mv.setHeightRatio(height);
						height *= page.getHeight();
					}
					mv.setPreferredSize(new Dimension((int) width, (int) height));
				}
				width = height = 0;
			}
			if ((argb1 | ARGB_NOT_SET) != ARGB_NOT_SET) {
				mv.setFillMode(new FillMode.ColorFill(new Color(argb1)));
				argb1 = ARGB_NOT_SET;
			}
			mv.setBorderType(borderType);
			borderType = null;
			mv.setResourceAddress(resourceURL);
			resourceURL = null;
			mv.setCustomInitializationScript(script);
			script = null;
			mv.setNavigationMode(navigationMode);
			navigationMode = false;
			if (rotation != null) { // backward compatible
				mv.setRotationAndZoom(rotation);
				rotation = null;
			}
			if (scheme != null) { // backward compatible
				mv.setScheme(scheme);
				scheme = null;
			}
			if (atomColoring >= 0) { // backward compatible
				mv.setAtomColoring(atomColoring);
				atomColoring = -1;
			}
			mv.setSpinOn(spin);
			mv.setDotsEnabled(dots);
			mv.setAxesShown(axes);
			mv.setBoundBoxShown(boundbox);
			spin = dots = axes = boundbox = false;
			mv.enableMenuBar(showMenuBar);
			mv.enableToolBar(showToolBar);
			mv.enableBottomBar(showStatusBar);
			showMenuBar = showToolBar = showStatusBar = true;
			return mv;
		}

		private PageMd3d createMd3d() {
			PageMd3d md = (PageMd3d) InstancePool.sharedInstance().getUnusedInstance(PageMd3d.class);
			mw3dConnector.enroll(md);
			md.setPage(page);
			md.reset();
			md.setIndex(indexOfMw3d++);
			if (borderType != null) {
				md.setBorderType(borderType);
				borderType = null;
			}
			else {
				md.setBorderType(null);
			}
			md.setChangable(page.isEditable());
			if (width > 0 && height > 0) {
				md.setPreferredSize(new Dimension((int) width, (int) height));
				width = height = 0;
			}
			if (rotation != null) {
				md.getMolecularView().setOrientation(rotation);
				rotation = null;
			}
			md.getMolecularModel().setRecorderDisabled(recorderless);
			if (!recorderless)
				md.getMolecularModel().activateEmbeddedMovie(true);
			recorderless = false;
			md.enableMenuBar(showMenuBar);
			showMenuBar = true;
			md.enableToolBar(showToolBar);
			showToolBar = true;
			md.enableBottomBar(showStatusBar);
			showStatusBar = true;
			md.getMolecularView().clear();
			md.setLoadingMessagePainted(true);
			resourceURL = null;
			return md;
		}

		private ModelCanvas createModelCanvas(String className) {
			ModelCanvas mc = page.getComponentPool().request(className);
			if (mc != null) {
				mw2dConnector.enroll(mc);
				mc.setUsed(true);
				mc.setResourceAddress(resourceURL);
				if (buttonGroup != null) {
					mc.setToolBarButtons(buttonGroup.isEmpty() ? null : buttonGroup.toArray());
					buttonGroup.clear();
				}
				else {
					mc.setToolBarButtons(null);
				}
				page.getProperties().put(mc, resourceURL);
				if (recorderDisabled) { // backward compatible
					mc.getContainer().getModel().setRecorderDisabled(recorderDisabled);
					recorderDisabled = false;
				}
				else {
					if (recorderless) {
						mc.getContainer().getModel().setRecorderDisabled(true);
						recorderless = false;
					}
					else {
						mc.getContainer().getModel().setRecorderDisabled(false);
					}
				}
				if (mc.getContainer() instanceof AtomContainer) {
					AtomContainer x = (AtomContainer) mc.getContainer();
					x.setDNAString(dnaString);
					x.setDNAContextEnabled(dnaContext);
					if (dnaString != null) {
						if (dna_dt1 != -1) {
							x.setTranscriptionTimeStep(dna_dt1);
							dna_dt1 = -1;
						}
						if (dna_dt2 != -1) {
							x.setTranslationMDStep(dna_dt2);
							dna_dt2 = -1;
						}
						x.getView().setClockPainted(false);
						x.createDNAPanel();
					}
				}
				mc.getContainer().setStatusBarShown(showStatusBar);
				if (!showStatusBar)
					showStatusBar = true;
				if (showMenuBar) {
					mc.addMenuBar();
				}
				else {
					mc.removeMenuBar();
					showMenuBar = true;
				}
				if (borderType != null) {
					mc.showBorder(false);
					borderType = null;
				}
				dnaString = null;
				dnaContext = true;
				width = height = 0;
			}
			className = null;
			resourceURL = null;
			return mc;
		}

	}

}