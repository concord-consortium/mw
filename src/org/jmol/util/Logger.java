/* $RCSfile: Logger.java,v $
 * $Author: qxie $
 * $Date: 2006-11-29 22:46:17 $
 * $Revision: 1.1 $
 *
 * Copyright (C) 2006  The Jmol Development Team
 *
 * Contact: jmol-developers@lists.sf.net
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 *  02110-1301, USA.
 */

package org.jmol.util;

/**
 * Logger mechanism.
 */
public final class Logger {

  private static LoggerInterface _logger = new DefaultLogger();

  public final static int LEVEL_DEBUG = 0;
  public final static int LEVEL_INFO = LEVEL_DEBUG + 1;
  public final static int LEVEL_WARN = LEVEL_INFO + 1;
  public final static int LEVEL_ERROR = LEVEL_WARN + 1;
  public final static int LEVEL_FATAL = LEVEL_ERROR + 1;
  public final static int NB_LEVELS = LEVEL_FATAL + 1;

  private final static boolean[] _activeLevels = new boolean[NB_LEVELS];
  private       static boolean   _logLevel = false;
  static {
    _activeLevels[LEVEL_DEBUG] = getProperty("debug",    false);
    _activeLevels[LEVEL_INFO]  = getProperty("info",     true);
    _activeLevels[LEVEL_WARN]  = getProperty("warn",     true);
    _activeLevels[LEVEL_ERROR] = getProperty("error",    true);
    _activeLevels[LEVEL_FATAL] = getProperty("fatal",    true);
    _logLevel                  = getProperty("logLevel", false);
  }

  private static boolean getProperty(String level, boolean defaultValue) {
    try {
      String property = System.getProperty("jmol.logger." + level);
      if (property != null) {
        return Boolean.TRUE.equals(Boolean.valueOf(property));
      }
    } catch (Exception e) {
      // applet can't do this.
    }
    return defaultValue;
  }

  /**
   * Replaces the current logger implementation by a new one.
   * 
   * @param logger New logger implementation.
   */
  public static void setLogger(LoggerInterface logger) {
    _logger = logger;
  }
  
  // XIE
  public static void setSilent(boolean b) {
	  _logger.setSilent(b);
  }

  /**
   * Tells if a logging level is active.
   * 
   * @param level Logging level.
   * @return Active.
   */
  public static boolean isActiveLevel(int level) {
    if (_logger == null) {
      return false;
    }
    if ((level >= 0) && (level < _activeLevels.length)) {
      return _activeLevels[level];
    }
    return false;
  }

  /**
   * Changes the activation state for a logging level.
   * 
   * @param level Level.
   * @param active New activation state.
   */
  public static void setActiveLevel(int level, boolean active) {
    if ((level >= 0) && (level < _activeLevels.length)) {
      _activeLevels[level] = active;
    }
  }

  /**
   * Returns the text corresponding to a level.
   * 
   * @param level Level.
   * @return Corresponding text.
   */
  public static String getLevel(int level) {
    switch (level) {
    case LEVEL_DEBUG:
      return "DEBUG";
    case LEVEL_INFO:
      return "INFO";
    case LEVEL_WARN:
      return "WARN";
    case LEVEL_ERROR:
      return "ERROR";
    case LEVEL_FATAL:
      return "FATAL";
    }
    return "????";
  }

  /**
   * Indicates if the level is logged.
   * 
   * @return Indicator.
   */
  public static boolean logLevel() {
    return _logLevel;
  }

  /**
   * Indicates if the level is logged.
   * 
   * @param log Indicator.
   */
  public static void logLevel(boolean log) {
    _logLevel = log;
  }

  /**
   * Writes a log at DEBUG level.
   * 
   * @param txt String to write.
   */
  public static void debug(String txt) {
    try {
      if (isActiveLevel(LEVEL_DEBUG)) {
        _logger.debug(txt);
      }
    } catch (Throwable t) {
      //
    }
  }

  /**
   * Writes a log at INFO level.
   * 
   * @param txt String to write.
   */
  public static void info(String txt) {
    try {
      if (isActiveLevel(LEVEL_INFO)) {
        _logger.info(txt);
      }
    } catch (Throwable t) {
      //
    }
  }

  /**
   * Writes a log at WARN level.
   * 
   * @param txt String to write.
   */
  public static void warn(String txt) {
    try {
      if (isActiveLevel(LEVEL_WARN)) {
        _logger.warn(txt);
      }
    } catch (Throwable t) {
      //
    }
  }

  /**
   * Writes a log at WARN level with detail on exception.
   * 
   * @param txt String to write.
   * @param e Exception.
   */
  public static void warn(String txt, Throwable e) {
    try {
      if (isActiveLevel(LEVEL_WARN)) {
        _logger.warn(txt, e);
      }
    } catch (Throwable t) {
      //
    }
  }

  /**
   * Writes a log at ERROR level.
   * 
   * @param txt String to write.
   */
  public static void error(String txt) {
    try {
      if (isActiveLevel(LEVEL_ERROR)) {
        _logger.error(txt);
      }
    } catch (Throwable t) {
      //
    }
  }

  /**
   * Writes a log at ERROR level with detail on exception.
   * 
   * @param txt String to write.
   * @param e Exception.
   */
  public static void error(String txt, Throwable e) {
    try {
      if (isActiveLevel(LEVEL_ERROR)) {
        _logger.error(txt, e);
      }
    } catch (Throwable t) {
      //
    }
  }

  /**
   * Writes a log at FATAL level.
   * 
   * @param txt String to write.
   */
  public static void fatal(String txt) {
    try {
      if (isActiveLevel(LEVEL_FATAL)) {
        _logger.fatal(txt);
      }
    } catch (Throwable t) {
      //
    }
  }

  /**
   * Writes a log at FATAL level with detail on exception.
   * 
   * @param txt String to write.
   * @param e Exception.
   */
  public static void fatal(String txt, Throwable e) {
    try {
      if (isActiveLevel(LEVEL_FATAL)) {
        _logger.fatal(txt, e);
      }
    } catch (Throwable t) {
      //
    }
  }
}
