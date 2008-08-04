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

import java.awt.AWTException;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.cert.Certificate;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.LookAndFeel;
import javax.swing.RepaintManager;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.Spring;
import javax.swing.SpringLayout;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.parser.ParserDelegator;

import org.concord.modeler.g2d.XYGrapher;
import org.concord.modeler.text.Page;
import org.concord.modeler.ui.ColorComboBox;
import org.concord.modeler.ui.ComboBoxRenderer;
import org.concord.modeler.ui.FillEffectChooser;
import org.concord.modeler.ui.ImagePreview;
import org.concord.modeler.util.FileChooser;
import org.concord.modeler.util.FileUtilities;
import org.concord.modeler.util.ImageReader;

public final class ModelerUtilities {

	public static String[] FONT_FAMILY_NAMES;
	public final static Integer[] FONT_SIZE = new Integer[65];

	public static FileChooser fileChooser;
	public static JFileChooser folderChooser;

	public static JColorChooser colorChooser;

	public static FillEffectChooser fillEffectChooser;

	static ImagePreview imagePreview;

	static ImageReader imageReader;

	private static Robot robot;
	private static Method setMenuBarVisibleMethod;
	private static ParserDelegator parserDelegator;
	private static String str;
	private static Pattern unicodePattern;

	private final static FileFilter DIR_FILTER = new FileFilter() {
		public boolean accept(File f) {
			return f.isDirectory();
		}

		public String getDescription() {
			return Modeler.IS_MAC ? "FOLDER" : "DIRECTORY";
		}
	};

	private ModelerUtilities() {
	}

	public static void init() {

		Initializer.sharedInstance().setMessage("Initializing...");
		fileChooser = new FileChooser(System.getProperty("user.dir"));
		fileChooser.setFileHidingEnabled(false);
		Page.setFileChooser(ModelerUtilities.fileChooser);
		XYGrapher.setFileChooser(fileChooser);
		JNLPSaver.setFileChooser(fileChooser);
		imagePreview = new ImagePreview(fileChooser);
		ImageReader.setImagePreview(imagePreview);
		imageReader = new ImageReader("Input image", fileChooser, null);

		folderChooser = new JFileChooser();
		folderChooser.addChoosableFileFilter(DIR_FILTER);
		folderChooser.setAcceptAllFileFilterUsed(false);
		folderChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

		colorChooser = new JColorChooser();
		colorChooser.setColor(Color.white);
		ColorComboBox.setColorChooser(colorChooser);

		fillEffectChooser = new FillEffectChooser();
		fillEffectChooser.setImageReader(imageReader);

		try {
			FONT_FAMILY_NAMES = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
		}
		catch (Throwable t) {
			t.printStackTrace();
			FONT_FAMILY_NAMES = new String[] { "Arial", "Arial Black", "Book Antiqua", "Comic Sans MS", "Courier New",
					"Default", "Dialog", "DialogInput", "Monospaced", "SansSerif", "Serif", "Times New Roman",
					"Verdana" };
		}
		for (int i = 8; i <= 72; i++)
			FONT_SIZE[i - 8] = i;
	}

	/** ping the MW server */
	public static boolean pingMwServer() {
		try {
			return ping(new URL(Modeler.getContextRoot()), 10000);
		}
		catch (MalformedURLException e) {
			e.printStackTrace();
			return false;
		}
	}

	public static boolean ping(URL url, int timeout) {
		return ping(url.getHost(), timeout);
	}

	private static boolean ping(String host, int timeout) {
		InetAddress address = null;
		try {
			address = InetAddress.getByName(host);
		}
		catch (UnknownHostException e) {
			e.printStackTrace();
			return false;
		}
		boolean b = false;
		try {
			b = address.isReachable(timeout);
		}
		catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return b;
	}

	static boolean testQuicktime() {
		try {
			Class.forName("quicktime.QTSession");
			return true;
		}
		catch (Throwable e) {
			e.printStackTrace();
			return false;
		}
	}

	public static Certificate getCertificate(Class klass) {
		Certificate[] c = klass.getProtectionDomain().getCodeSource().getCertificates();
		if (c == null || c.length <= 0)
			return null;
		return c[0];
	}

	public static Dimension getSystemToolBarButtonSize() {
		if (Modeler.isMac())
			return new Dimension(30, 30);
		return new Dimension(24, 24);
	}

	public static Cursor createCursor(URL url, Point hotSpot, String name) {
		return createCursor(new ImageIcon(url), hotSpot, name);
	}

	public static Cursor createCursor(ImageIcon cursorIcon, Point hotSpot, String name) {
		return createCursor(cursorIcon.getImage(), hotSpot, name);
	}

	public static Cursor createCursor(Image cursorImage, Point hotSpot, String name) {
		if (hotSpot == null)
			hotSpot = new Point();
		Dimension prefDimension = Toolkit.getDefaultToolkit().getBestCursorSize(hotSpot.x, hotSpot.y);
		if (hotSpot.x > prefDimension.width - 1)
			hotSpot.x = prefDimension.width - 1;
		else if (hotSpot.x < 0)
			hotSpot.x = 0;
		if (hotSpot.y > prefDimension.height - 1)
			hotSpot.y = prefDimension.height - 1;
		else if (hotSpot.y < 0)
			hotSpot.y = 0;
		return Toolkit.getDefaultToolkit().createCustomCursor(cursorImage, hotSpot, name);
	}

	/** Victor Yacovlev 2007-09-25: de-Unicode HTML text. */
	public static String deUnicode(String text) {
		if (text != null && text.trim().length() >= 6) {
			if (text.trim().substring(0, 6).toLowerCase().equals("<html>")) {
				if (unicodePattern == null)
					unicodePattern = Pattern.compile("&#[0-9]+;");
				Matcher m = unicodePattern.matcher(text);
				while (m.find()) {
					String codedText = m.group();
					String strCode = codedText.substring(2, codedText.lastIndexOf(";"));
					int code = 0;
					try {
						code = Integer.parseInt(strCode);
						// don't use new Integer(strCode) that needs to create an object
					}
					catch (NumberFormatException e) {
						e.printStackTrace();
						continue;
					}
					text = m.replaceFirst(Character.toString((char) code));
					m = unicodePattern.matcher(text);
				}
				text = text.replaceAll("&quot;", "\"");
			}
		}
		return text;
	}

	/** convert a html string into plain text. Skip images and hotlinks. */
	public static String extractPlainText(final String html) {
		if (parserDelegator == null)
			parserDelegator = new ParserDelegator();
		str = "";
		try {
			parserDelegator.parse(new StringReader(html), new HTMLEditorKit.ParserCallback() {
				public void handleSimpleTag(HTML.Tag t, MutableAttributeSet a, int pos) {
					if (t == HTML.Tag.BR)
						str += '\n';
				}

				public void handleText(char[] data, int pos) {
					str += new String(data);
				}

				public void handleStartTag(HTML.Tag t, MutableAttributeSet a, int pos) {
					if (t == HTML.Tag.P)
						str += '\n';
				}
			}, false);
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		return str;
	}

	/*
	 * Mac OS X's Java 1.5.0_06 implementation returns things like :
	 * /Users/user/workbench.jar:/System/Library/Frameworks/JavaVM.framework/Versions/1.5.0/Classes
	 * /.compatibility/14compatibility.jar
	 */
	static String validateJarLocationOnMacOSX(String jarLocation) {
		int i = jarLocation.indexOf(".jar:/");
		if (i != -1)
			jarLocation = jarLocation.substring(0, i) + ".jar";
		return jarLocation;
	}

	public static void setMacOSXMenuBarVisible(boolean val) {
		if (setMenuBarVisibleMethod == null) {
			Class<?> clazz = null;
			try {
				clazz = Class.forName("com.apple.cocoa.application.NSMenu");
			}
			catch (Throwable t) {
			}
			if (clazz == null) {
				try {
					ClassLoader cocoaClassLoader = new AllPermissionClassLoader(new URL[] { new URL(
							"file:///System/Library/Java/") }, ModelerUtilities.class.getClassLoader());
					clazz = cocoaClassLoader.loadClass("com.apple.cocoa.application.NSMenu");
				}
				catch (Throwable t) {
					t.printStackTrace();
				}
			}
			if (clazz == null)
				return;
			try {
				setMenuBarVisibleMethod = clazz.getDeclaredMethod("setMenuBarVisible", new Class[] { boolean.class });
			}
			catch (Throwable t) {
				t.printStackTrace();
			}
		}
		if (setMenuBarVisibleMethod != null) {
			try {
				setMenuBarVisibleMethod.invoke(null, new Object[] { val ? Boolean.TRUE : Boolean.FALSE });
			}
			catch (Throwable t) {
				t.printStackTrace();
			}
		}
	}

	/**
	 * @return a sortable string of the specified integer with the specified digits. For instance, return 001 for 1 if
	 *         the number of digits is given to be 3.
	 * @throws IllegalArgumentException
	 *             if n < 0 or n >= Math.power(10, digit)
	 */
	public static String getSortableString(int n, int digit) {
		if (n < 0 || n >= Math.pow(10, digit))
			throw new IllegalArgumentException("i cannot be negative");
		char[] c = new char[digit];
		Arrays.fill(c, '0');
		StringBuilder s = new StringBuilder(new String(c));
		String t = "" + n;
		int k = t.length();
		for (int i = 0; i < k; i++) {
			s.setCharAt(digit - k + i, t.charAt(i));
		}
		return s.toString();
	}

	/**
	 * convert the non-ASCII characters of the string into Unicode decimal values, enclosed between "&#" and ";". The
	 * ASCII characters remain.
	 */
	public static String getUnicode(String s) {
		char[] c = s.toCharArray();
		StringBuffer buffer = new StringBuffer();
		int n;
		for (int i = 0; i < c.length; i++) {
			n = c[i];
			if (n > 0x007f) {
				buffer.append("&#");
				buffer.append((int) c[i]);
				buffer.append(";");
			}
			else {
				buffer.append(c[i]);
			}
		}
		return buffer.toString();
	}

	public static String currentTimeToString() {
		Calendar c = Calendar.getInstance();
		return Integer.toString(c.get(Calendar.YEAR)) + Integer.toString(c.get(Calendar.MONTH) + 1)
				+ Integer.toString(c.get(Calendar.DATE)) + Integer.toString(c.get(Calendar.HOUR_OF_DAY))
				+ Integer.toString(c.get(Calendar.MINUTE)) + Integer.toString(c.get(Calendar.SECOND));
	}

	public static String currentTimeMillisToString() {
		Calendar c = Calendar.getInstance();
		return Integer.toString(c.get(Calendar.YEAR)) + Integer.toString(c.get(Calendar.MONTH) + 1)
				+ Integer.toString(c.get(Calendar.DATE)) + Integer.toString(c.get(Calendar.HOUR_OF_DAY))
				+ Integer.toString(c.get(Calendar.MINUTE)) + Integer.toString(c.get(Calendar.SECOND))
				+ Integer.toString(c.get(Calendar.MILLISECOND));
	}

	/**
	 * platform-independent check for Windows' equivalent of right click of mouse button. This can be used as an
	 * alternative as MouseEvent.isPopupTrigger(), which requires checking within both mousePressed() and
	 * mouseReleased() methods.
	 */
	public static boolean isRightClick(MouseEvent e) {
		if ((e.getModifiers() & MouseEvent.BUTTON3_MASK) != 0)
			return true;
		if (Modeler.isMac() && e.isControlDown())
			return true;
		return false;
	}

	/**
	 * On Windows and Linux, if the user right-clicks on a button, no <code>ActionEvent</code> is fired. Some Mac
	 * computers, however, do not have a two-button mouse. Windows right-click on Mac OS is usually mimicked with
	 * CTRL+Click (or Meta+Click). Unfortunately, when CTRL+Click is pressed, an <code>ActionEvent</code> will be
	 * fired. For the Mac CTRL+Click action to have the same behavior as Windows right-click, this method should be
	 * called at the beginning of the <code>actionPerformed(ActionEvent e)</code> method of the
	 * <code>ActionListener</code> associated with the button. If CTRL+Click is pressed, the method will return
	 * immediately without executing the rest of the code in the method body.
	 */
	public static boolean stopFiring(ActionEvent e) {
		if (!Modeler.isMac())
			return false;
		if (e == null)
			return false;
		int modifier = e.getModifiers();
		if (modifier == (ActionEvent.META_MASK | ActionEvent.MOUSE_EVENT_MASK)
				|| modifier == (ActionEvent.CTRL_MASK | ActionEvent.MOUSE_EVENT_MASK))
			return true;
		return false;
	}

	public static void updateFileChooserUI(JFileChooser fc) {
		if (Modeler.isMac()) {
			LookAndFeel lnf = UIManager.getLookAndFeel();
			// JFileChooser on Mac OS X with the native L&F doesn't work well.
			// If the native L&F of Mac is selected, disable it for the file chooser
			if (lnf.isNativeLookAndFeel()) {
				try {
					UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
				}
				catch (Exception e) {
					e.printStackTrace();
				}
				fc.updateUI();
				try {
					UIManager.setLookAndFeel(lnf);
				}
				catch (UnsupportedLookAndFeelException e) {
					e.printStackTrace();
				}
			}
		}
		else {
			fc.updateUI();
		}
	}

	static void updateUI() {
		if (fileChooser != null)
			updateFileChooserUI(fileChooser);
		if (folderChooser != null)
			updateFileChooserUI(folderChooser);
		if (colorChooser != null)
			colorChooser.updateUI();
		if (fillEffectChooser != null)
			SwingUtilities.updateComponentTreeUI(fillEffectChooser);
	}

	/** convert a "file:/" URL name to native file name */
	public static String convertURLToFilePath(String url) {
		if (url == null)
			return null;
		if (url.toLowerCase().startsWith("file:")) {
			String os = System.getProperty("os.name");
			if (os.startsWith("Mac") || os.startsWith("Linux")) {
				// file:/Users/***
				url = url.substring(5);
			}
			else if (os.startsWith("Windows")) {
				// file:/C:/***
				url = url.substring(6);
				char separator = System.getProperty("file.separator").charAt(0);
				url = url.replace('/', separator);
			}
		}
		return FileUtilities.httpDecode(url);
	}

	/** convert a "file:/" URL name to native file name * */
	public static File convertURLToFile(String url) {
		if (url == null)
			return null;
		if (url.toLowerCase().startsWith("file:"))
			return new File(convertURLToFilePath(url));
		throw new IllegalArgumentException(url + " is not a file.");
	}

	/**
	 * The speed and quality of printing suffers dramatically if any of the containers have double buffering turned on.
	 * So this turns if off globally.
	 * 
	 * @param c
	 *            processed component
	 */
	public static void disableDoubleBuffering(Component c) {
		RepaintManager currentManager = RepaintManager.currentManager(c);
		currentManager.setDoubleBufferingEnabled(false);
	}

	/**
	 * Re-enables double buffering globally.
	 * 
	 * @param c
	 *            processed component
	 */
	public static void enableDoubleBuffering(Component c) {
		RepaintManager currentManager = RepaintManager.currentManager(c);
		currentManager.setDoubleBufferingEnabled(true);
	}

	/**
	 * ImageIcon's constructors use MediaTracker to load an image, which is cached. As a result, subsequent changes on
	 * the content of the image do not show up. This method uses ImageIO class to remove the caching effect. Caution:
	 * You should NOT use this method to create ImageIcon's whose content will NOT change.
	 */
	public static ImageIcon createNonCachedImageIcon(File file) {
		try {
			return new ImageIcon(ImageIO.read(file));
		}
		catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	/** create a BufferedImage from a plain Image */
	public BufferedImage getBufferedImage(Image image) {
		if (image == null)
			return null;
		if (image instanceof BufferedImage)
			return (BufferedImage) image;
		BufferedImage bi = new BufferedImage(image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_INT_ARGB);
		Graphics g = bi.createGraphics();
		g.drawImage(image, 0, 0, null);
		return bi;
	}

	/** convert an image into a byte array */
	public byte[] getImageArray(Image image, String formatName) {
		BufferedImage bi = getBufferedImage(image);
		if (bi == null)
			return null;
		ByteArrayOutputStream baos = new ByteArrayOutputStream(1000);
		try {
			ImageIO.write(bi, formatName, baos);
			baos.flush();
			byte[] result = baos.toByteArray();
			baos.close();
			return result;
		}
		catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	/** scale an image. This is an alternative to Image.getScaledInstance() */
	public static BufferedImage scale(BufferedImage src, float sx, float sy) {
		AffineTransform t = new AffineTransform();
		t.scale(sx, sy);
		AffineTransformOp op = new AffineTransformOp(t, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
		return op.filter(src, null);
	}

	/** copy a resource represented by a path to a file */
	public static int copyResource(String path, File file) {
		if (path == null || file == null)
			throw new IllegalArgumentException("Resource location error");
		String s2 = path;
		if (FileUtilities.isRemote(path)) {
			File f2 = ConnectionManager.sharedInstance().shouldUpdate(path);
			if (f2 != null)
				s2 = f2.getAbsolutePath();
		}
		return FileUtilities.copy(s2, file);
	}

	/** copy a resource represented by a path to a directory without changing the name */
	public static int copyResourceToDirectory(String path, File dir) {
		if (path == null || dir == null)
			throw new IllegalArgumentException("Resource location error");
		String s2 = path;
		if (FileUtilities.isRemote(path)) {
			File f2 = ConnectionManager.sharedInstance().shouldUpdate(path);
			if (f2 != null)
				s2 = f2.toString();
		}
		return FileUtilities.copy(s2, new File(dir, FileUtilities.getFileName(path)));
	}

	/**
	 * convert hexadecimal RGB to color --- Color.decode(String nm) does not seem to work. The passed string can start
	 * with an "#".
	 */
	public static Color convertToColor(String s) {
		if (s == null)
			throw new IllegalArgumentException("Did you mean to convert hexadecimal RGB to color?");
		if (s.length() == 7 && s.charAt(0) == '#') {
			try {
				int r = Integer.parseInt(s.substring(1, 3), 16);
				int g = Integer.parseInt(s.substring(3, 5), 16);
				int b = Integer.parseInt(s.substring(5, 7), 16);
				return new Color(r, g, b);
			}
			catch (NumberFormatException e) {
				System.out.println(s + " not recognizable as a HTML color code");
			}
		}
		else if (s.length() == 6) {
			try {
				int r = Integer.parseInt(s.substring(0, 2), 16);
				int g = Integer.parseInt(s.substring(2, 4), 16);
				int b = Integer.parseInt(s.substring(4, 6), 16);
				return new Color(r, g, b);
			}
			catch (NumberFormatException e) {
				System.out.println(s + " not recognizable as a hex color code");
			}
		}
		return Color.white;
	}

	/** convert color to hexadecimal RGB to use in HTML */
	public static String convertToHexRGB(Color c) {
		if (c == null)
			throw new IllegalArgumentException("Did you mean to convert a color to hexadecimal RGB?");
		return Integer.toString(c.getRGB() & 0x00ffffff, 16);
	}

	/** @return a median color of the input ones, including the alpha values. */
	public static Color getMedianColor(Color c1, Color c2) {
		return new Color(((c1.getAlpha() + c2.getAlpha()) >> 1) << 24 | ((c1.getRed() + c2.getRed()) >> 1) << 16
				| ((c1.getGreen() + c2.getGreen()) >> 1) << 8 | ((c1.getBlue() + c2.getBlue()) >> 1));
	}

	public static JDialog getChildDialog(Component parent, boolean modal) {
		Window window = SwingUtilities.getWindowAncestor(parent);
		JDialog dialog;
		if (window instanceof Frame) {
			dialog = new JDialog((Frame) window, modal);
		}
		else if (window instanceof Dialog) {
			dialog = new JDialog((Dialog) window, modal);
		}
		else {
			dialog = new JDialog();
			dialog.setModal(modal);
		}
		return dialog;
	}

	public static JDialog getChildDialog(Component parent, String title, boolean modal) {
		Window window = SwingUtilities.getWindowAncestor(parent);
		JDialog dialog;
		if (window instanceof Frame) {
			dialog = new JDialog((Frame) window, title, modal);
		}
		else if (window instanceof Dialog) {
			dialog = new JDialog((Dialog) window, title, modal);
		}
		else {
			dialog = new JDialog();
			dialog.setModal(modal);
			dialog.setTitle(title);
		}
		return dialog;
	}

	public static JComboBox createFontSizeComboBox() {
		JComboBox c = new JComboBox(FONT_SIZE);
		c.setToolTipText("Font size");
		c.setSelectedIndex(4);
		FontMetrics fm = c.getFontMetrics(c.getFont());
		int w = fm.stringWidth(FONT_SIZE[FONT_SIZE.length - 1].toString()) + (Modeler.isMac() ? 40 : 30);
		int h = fm.getHeight() + 4;
		c.setPreferredSize(new Dimension(w, h));
		c.setEditable(false);
		c.setRequestFocusEnabled(false);
		return c;
	}

	public static JComboBox createFontSizeComboBox(int n) {
		n = Math.min(n, FONT_SIZE.length);
		Integer[] i = new Integer[n];
		System.arraycopy(FONT_SIZE, 0, i, 0, n);
		JComboBox c = new JComboBox(i);
		c.setSelectedIndex(Math.min(4, n));
		c.setToolTipText("Font size");
		FontMetrics fm = c.getFontMetrics(c.getFont());
		int w = fm.stringWidth(i[i.length - 1].toString()) + (Modeler.isMac() ? 40 : 30);
		int h = fm.getHeight() + 4;
		c.setPreferredSize(new Dimension(w, h));
		c.setEditable(false);
		c.setRequestFocusEnabled(false);
		return c;
	}

	public static JComboBox createFontNameComboBox() {
		JComboBox c = new JComboBox(FONT_FAMILY_NAMES);
		c.setRenderer(new ComboBoxRenderer.FontLabel());
		c.setToolTipText("Font type");
		FontMetrics fm = c.getFontMetrics(c.getFont());
		int w = longestFontName(fm) + 50;
		int h = fm.getHeight() + 4;
		c.setPreferredSize(new Dimension(w, h));
		c.setEditable(false);
		c.setRequestFocusEnabled(false);
		return c;
	}

	private static int longestFontName(FontMetrics fm) {
		if (fm == null)
			throw new IllegalArgumentException("Null font metrics");
		int max = 0, n = 0;
		for (int i = 0; i < FONT_FAMILY_NAMES.length; i++) {
			n = fm.stringWidth(FONT_FAMILY_NAMES[i]);
			if (max < n)
				max = n;
		}
		return max;
	}

	/**
	 * shut down later.
	 * 
	 * @param delay
	 *            the amount of delay time in miliseconds
	 */
	public static void shutdown(int delay) {
		Timer timer = new Timer(delay, new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				System.exit(1);
			}
		});
		timer.setRepeats(false);
		timer.start();
	}

	public static void printCurrentThreadGroup() {
		printThreadGroup(Thread.currentThread().getThreadGroup(), "");
	}

	public static void printThreads() {
		ThreadGroup tg = Thread.currentThread().getThreadGroup();
		ThreadGroup mainGroup = tg;
		while (tg != null) {
			tg = tg.getParent();
			if (tg != null) {
				mainGroup = tg;
			}
		}
		printThreadGroup(mainGroup, "");
	}

	public static void printThreadGroup(ThreadGroup tg, String prefix) {
		if (tg == null)
			return;
		int ngroups = tg.activeGroupCount();
		int nthreads = tg.activeCount();
		System.out.println(prefix + "ThreadGroup " + tg);
		prefix += "    ";
		if (nthreads > 0) {
			Thread tlist[] = new Thread[nthreads];
			int realnthreads = tg.enumerate(tlist, false);
			for (int t = 0; t < realnthreads; t++) {
				System.out.println(prefix + "Thread " + tlist[t]);
				if (ngroups > 0) {
					ThreadGroup tglist[] = new ThreadGroup[ngroups];
					int realngroups = tg.enumerate(tglist, false);
					for (int g = 0; g < realngroups; g++) {
						printThreadGroup(tglist[g], prefix);
					}
				}
			}
		}
	}

	static void printSystemProperties() {
		Enumeration en = System.getProperties().propertyNames();
		while (en.hasMoreElements()) {
			String o = (String) en.nextElement();
			System.out.println(o + "=" + System.getProperty(o));
		}
	}

	/** save the target an URL points to the local file system */
	public static boolean saveTargetAs(JComponent parent, final String url) {

		/* install filter */
		FileFilter filter = FileFilterFactory.getFilter(FileUtilities.getSuffix(url));
		if (filter != null) {
			fileChooser.setAcceptAllFileFilterUsed(false);
			fileChooser.addChoosableFileFilter(filter);
		}
		else {
			fileChooser.setAcceptAllFileFilterUsed(true);
		}

		/* customize the file dialog */
		fileChooser.setDialogType(JFileChooser.SAVE_DIALOG);
		String latestPath = fileChooser.getLastVisitedPath();
		if (latestPath != null)
			fileChooser.setCurrentDirectory(new File(latestPath));
		fileChooser.setAccessory(null);
		try {
			fileChooser.setSelectedFile(new File(fileChooser.getCurrentDirectory(), FileUtilities.getFileName(url)));
		}
		catch (NullPointerException npe) {
			npe.printStackTrace();
		}

		boolean success = false;

		final Window window = SwingUtilities.getWindowAncestor(parent);
		int returnValue = fileChooser.showSaveDialog(window);

		String filename = null;

		if (returnValue == JFileChooser.APPROVE_OPTION) {
			filename = FileUtilities.fileNameAutoExtend(filter, fileChooser.getSelectedFile());
			final File file = new File(filename);
			if (file.exists()) {
				int opt = JOptionPane.showConfirmDialog(window, "File " + file.getName() + " exists, overwrite?",
						"File exists", JOptionPane.YES_NO_OPTION);
				if (opt != 0) {
					if (filter != null)
						fileChooser.removeChoosableFileFilter(filter);
					return false;
				}
			}
			int idot = filename.lastIndexOf('.');
			String extension = filename.substring(idot + 1).toUpperCase();

			if (filter != null) {
				if (!extension.equals(filter.getDescription())) {
					JOptionPane.showMessageDialog(window, "Sorry, extension name should be " + filter.getDescription()
							+ ". Write aborted.");
					success = false;
				}
			}

			if (!success) {
				new Thread(new Runnable() {
					public void run() {
						FileUtilities.copy(url, file);
					}
				}, "Download: " + url).start();
				fileChooser.rememberPath(fileChooser.getCurrentDirectory().toString());
				success = true;
			}
		}

		if (filter != null)
			fileChooser.removeChoosableFileFilter(filter);

		return success;

	}

	public static void saveImageIcon(ImageIcon icon, File file, boolean borderless) {
		if (icon == null)
			return;
		BufferedImage bi = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_INT_RGB);
		Graphics2D g2 = bi.createGraphics();
		icon.paintIcon(null, g2, 0, 0);
		write(bi, file, borderless);
	}

	public static void screenshot(Component c, String s, boolean borderless) {
		Dimension size = c.getSize();
		BufferedImage bi = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_RGB);
		Graphics2D g2 = bi.createGraphics();
		c.paint(g2);
		write(bi, new File(s), borderless);
	}

	public static void write(RenderedImage image, File file, boolean borderless) {
		if (!borderless) {
			if (image instanceof BufferedImage) {
				BufferedImage bi = (BufferedImage) image;
				int x = bi.getMinX();
				int y = bi.getMinY();
				int w = bi.getWidth();
				int h = bi.getHeight();
				for (int i = x; i < x + w; i++) {
					bi.setRGB(i, y, 0x000000);
					bi.setRGB(i, y + h - 1, 0x000000);
				}
				for (int i = y; i < y + h; i++) {
					bi.setRGB(x, i, 0x000000);
					bi.setRGB(x + w - 1, i, 0x000000);
				}
			}
		}
		OutputStream out = null;
		try {
			out = new FileOutputStream(file);
			ImageIO.write(image, FileUtilities.getExtensionInLowerCase(file), out);
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		finally {
			if (out != null) {
				try {
					out.close();
				}
				catch (IOException e) {
				}
			}
		}
	}

	/** get a scaled instance of the input ImageIcon */
	public static ImageIcon scaleImageIcon(ImageIcon icon, float scale) {
		if (icon == null)
			return null;
		if (scale > 1.0f || scale <= 0.0f)
			throw new IllegalArgumentException("scale must be in (0, 1].");
		int w = icon.getIconWidth();
		int h = icon.getIconHeight();
		return new ImageIcon(icon.getImage()
				.getScaledInstance((int) (w * scale), (int) (h * scale), Image.SCALE_SMOOTH));
	}

	/** create an ImageIcon delegate of a component */
	public static ImageIcon componentToImageIcon(Component c, String description, boolean paintBorder) {
		return componentToImageIcon(c, description, paintBorder, 1.0f);
	}

	/** create an ImageIcon delegate of a component with the specified scale */
	public static ImageIcon componentToImageIcon(Component c, String description, boolean paintBorder, float scale) {
		if (c == null)
			return null;
		if (!c.isShowing())
			return null;
		if (scale > 1.0f || scale <= 0.0f)
			throw new IllegalArgumentException("scale must be in (0, 1].");
		Dimension size = c.getSize();
		BufferedImage bi = null;
		if (c instanceof JComponent) {
			bi = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_RGB);
			Graphics2D g2 = bi.createGraphics();
			c.paint(g2);
			// FIXME: if c is a JEditorPane, calling the paint method forces the images to be reloaded incorrectly.
			g2.dispose(); // This caused empty image before. Was it fixed?
		}
		else {
			if (robot == null) {
				try {
					robot = new Robot();
				}
				catch (AWTException e) {
					e.printStackTrace();
					robot = null;
				}
			}
			if (robot != null) {
				bi = robot.createScreenCapture(new Rectangle(c.getLocationOnScreen().x, c.getLocationOnScreen().y, c
						.getWidth(), c.getHeight()));
			}
			else {
				bi = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_RGB);
				Graphics2D g2 = bi.createGraphics();
				c.paint(g2);
				g2.dispose();
			}
		}
		ImageIcon icon = null;
		if (scale < 1.0f) {
			icon = new ImageIcon(bi.getScaledInstance((int) (bi.getWidth() * scale), (int) (bi.getHeight() * scale),
					Image.SCALE_SMOOTH));
		}
		else {
			if (paintBorder) {
				int x = bi.getMinX();
				int y = bi.getMinY();
				int w = bi.getWidth();
				int h = bi.getHeight();
				for (int i = x; i < x + w; i++) {
					bi.setRGB(i, y, 0x000000);
					bi.setRGB(i, y + h - 1, 0x000000);
				}
				for (int i = y; i < y + h; i++) {
					bi.setRGB(x, i, 0x000000);
					bi.setRGB(x + w - 1, i, 0x000000);
				}
			}
			icon = new ImageIcon(bi);
		}
		icon.setDescription(description);
		return icon;
	}

	/** save an ImageIcon to a JPEG file in the local file system */
	public static boolean saveImageAs(Component parent, final ImageIcon icon) {

		fileChooser.setAcceptAllFileFilterUsed(false);
		fileChooser.addChoosableFileFilters(new String[] { "png", "jpg" });

		/* customize the file dialog */
		fileChooser.setDialogType(JFileChooser.SAVE_DIALOG);
		String latestPath = fileChooser.getLastVisitedPath();
		if (latestPath != null)
			fileChooser.setCurrentDirectory(new File(latestPath));
		fileChooser.setAccessory(imagePreview);
		imagePreview.setFile(null);

		boolean success = false;

		final Window window = SwingUtilities.getWindowAncestor(parent);
		int returnValue = fileChooser.showSaveDialog(window);

		String filename = null;

		if (returnValue == JFileChooser.APPROVE_OPTION) {
			filename = FileUtilities.fileNameAutoExtend(fileChooser.getFileFilter(), fileChooser.getSelectedFile());
			final File file = new File(filename);
			if (file.exists()) {
				int opt = JOptionPane.showConfirmDialog(window, "File " + file.getName() + " exists, overwrite?",
						"File exists", JOptionPane.YES_NO_OPTION);
				if (opt != 0) {
					fileChooser.resetChoosableFileFilters();
					return false;
				}
			}
			int idot = filename.lastIndexOf('.');
			String extension = filename.substring(idot + 1).toUpperCase();

			if (!extension.equals(fileChooser.getFileFilter().getDescription())) {
				JOptionPane.showMessageDialog(window, "Sorry, extension name should be "
						+ fileChooser.getFileFilter().getDescription() + ". Write aborted.");
				success = false;
			}

			if (!success) {
				// save the image in the background
				Thread t = new Thread(new Runnable() {
					public void run() {
						saveImageIcon(icon, file, false);
					}
				}, "Save Image:" + icon);
				t.setPriority(Thread.MIN_PRIORITY);
				t.start();
				fileChooser.rememberPath(fileChooser.getCurrentDirectory().toString());
				success = true;
			}
		}

		fileChooser.resetChoosableFileFilters();
		return success;

	}

	/* Used by makeCompactGrid. */
	private static SpringLayout.Constraints getConstraintsForCell(int r, int c, Container parent, int cols) {
		SpringLayout layout = (SpringLayout) parent.getLayout();
		Component component = parent.getComponent(r * cols + c);
		return layout.getConstraints(component);
	}

	/**
	 * Aligns the first <code>rows</code> <code>cols</code> components of <code>parent</code> in a grid. Each
	 * component in a column is as wide as the maximum preferred width of the components in that column; height is
	 * similarly determined for each row. The parent is made just big enough to fit them all.
	 * 
	 * @param rows
	 *            number of rows
	 * @param cols
	 *            number of columns
	 * @param initialX
	 *            x location to start the grid at
	 * @param initialY
	 *            y location to start the grid at
	 * @param xPad
	 *            x padding between cells
	 * @param yPad
	 *            y padding between cells
	 */
	public static void makeCompactGrid(Container parent, int rows, int cols, int initialX, int initialY, int xPad,
			int yPad) {
		SpringLayout layout;
		try {
			layout = (SpringLayout) parent.getLayout();
		}
		catch (ClassCastException exc) {
			System.err.println("The first argument to makeCompactGrid must use SpringLayout.");
			return;
		}

		// Align all cells in each column and make them the same width.
		Spring x = Spring.constant(initialX);
		for (int c = 0; c < cols; c++) {
			Spring width = Spring.constant(0);
			for (int r = 0; r < rows; r++) {
				width = Spring.max(width, getConstraintsForCell(r, c, parent, cols).getWidth());
			}
			for (int r = 0; r < rows; r++) {
				SpringLayout.Constraints constraints = getConstraintsForCell(r, c, parent, cols);
				constraints.setX(x);
				constraints.setWidth(width);
			}
			x = Spring.sum(x, Spring.sum(width, Spring.constant(xPad)));
		}

		// Align all cells in each row and make them the same height.
		Spring y = Spring.constant(initialY);
		for (int r = 0; r < rows; r++) {
			Spring height = Spring.constant(0);
			for (int c = 0; c < cols; c++) {
				height = Spring.max(height, getConstraintsForCell(r, c, parent, cols).getHeight());
			}
			for (int c = 0; c < cols; c++) {
				SpringLayout.Constraints constraints = getConstraintsForCell(r, c, parent, cols);
				constraints.setY(y);
				constraints.setHeight(height);
			}
			y = Spring.sum(y, Spring.sum(height, Spring.constant(yPad)));
		}

		// Set the parent's size.
		SpringLayout.Constraints pCons = layout.getConstraints(parent);
		pCons.setConstraint(SpringLayout.SOUTH, y);
		pCons.setConstraint(SpringLayout.EAST, x);

	}

	public static void setWithoutNotifyingListeners(AbstractButton button, boolean selected) {
		if (button == null)
			return;
		Action a = button.getAction();
		String text = button.getText();
		ItemListener[] il = button.getItemListeners();
		if (il != null) {
			for (ItemListener x : il)
				button.removeItemListener(x);
		}
		ActionListener[] al = button.getActionListeners();
		if (al != null) {
			for (ActionListener x : al)
				button.removeActionListener(x);
		}
		button.setSelected(selected);
		if (il != null) {
			for (ItemListener x : il)
				button.addItemListener(x);
		}
		if (al != null) {
			for (ActionListener x : al)
				button.addActionListener(x);
		}
		if (a != null)
			button.setAction(a);
		button.setText(text);
	}

	public static void selectWithoutNotifyingListeners(JComboBox comboBox, int selectedIndex) {
		ItemListener[] il = comboBox.getItemListeners();
		if (il != null) {
			for (ItemListener x : il)
				comboBox.removeItemListener(x);
		}
		comboBox.setSelectedIndex(selectedIndex);
		if (il != null) {
			for (ItemListener x : il)
				comboBox.addItemListener(x);
		}
	}

	public static void adjustWithoutNotifyingListeners(JSlider slider, int value) {
		ChangeListener[] cl = slider.getChangeListeners();
		if (cl != null) {
			for (ChangeListener x : cl)
				slider.removeChangeListener(x);
		}
		slider.setValue(value);
		if (cl != null) {
			for (ChangeListener x : cl)
				slider.addChangeListener(x);
		}
	}

	public static void adjustWithoutNotifyingListeners(JSpinner spinner, double value) {
		ChangeListener[] cl = spinner.getChangeListeners();
		if (cl != null) {
			for (ChangeListener x : cl)
				spinner.removeChangeListener(x);
		}
		((SpinnerNumberModel) spinner.getModel()).setValue(new Double(value));
		if (cl != null) {
			for (ChangeListener x : cl)
				spinner.addChangeListener(x);
		}
	}

}