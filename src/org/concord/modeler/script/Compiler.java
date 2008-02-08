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

package org.concord.modeler.script;

import java.util.regex.Pattern;

import static java.util.regex.Pattern.*;

public class Compiler {

	private Compiler() {
	}

	public final static String REGEX_SEPARATOR = "[,\\s&&[^\\r\\n]]";
	public final static String REGEX_WHITESPACE = "[\\s&&[^\\r\\n]]";
	public final static String REGEX_NOT = "[\\s&&[^\\r\\n]]*(?i)not[\\s&&[^\\r\\n]]+";
	public final static String REGEX_OR = "[\\s&&[^\\r\\n]]+(?i)or[\\s&&[^\\r\\n]]+";
	public final static String REGEX_AND = "[\\s&&[^\\r\\n]]+(?i)and[\\s&&[^\\r\\n]]+";
	public final static String REGEX_NONNEGATIVE_DECIMAL = "((\\d*\\.\\d+)|(\\d+\\.\\d*)|(\\d+))";
	public final static String REGEX_NONNEGATIVE_INTEGER = "\\d+";

	/** (a, b), a, b are integer */
	public final static String REGEX_INTEGER_GROUP = "^\\(*" + REGEX_SEPARATOR + "*(\\d+){1}(" + REGEX_SEPARATOR
			+ "+\\d+)*" + REGEX_SEPARATOR + "+(\\d+){1}" + REGEX_SEPARATOR + "*\\)*";

	/** (a, b) */
	public final static String REGEX_NUMBER_PAIR = "^\\(" + REGEX_WHITESPACE + "*(-?)" + REGEX_NONNEGATIVE_DECIMAL
			+ REGEX_SEPARATOR + "+(-?)" + REGEX_NONNEGATIVE_DECIMAL + REGEX_WHITESPACE + "*\\)";

	/** (a, b), a > 0, b > 0 */
	public final static String REGEX_POSITIVE_NUMBER_PAIR = "^\\(" + REGEX_WHITESPACE + "*" + REGEX_NONNEGATIVE_DECIMAL
			+ REGEX_SEPARATOR + "+" + REGEX_NONNEGATIVE_DECIMAL + REGEX_WHITESPACE + "*\\)";

	/** (a, b, c, d) */
	public final static String QUADRUPLE = "\\(" + REGEX_WHITESPACE + "*((" + REGEX_NONNEGATIVE_DECIMAL + "|"
			+ REGEX_NONNEGATIVE_INTEGER + ")" + REGEX_SEPARATOR + "+){3}" + "(" + REGEX_NONNEGATIVE_DECIMAL + "|"
			+ REGEX_NONNEGATIVE_INTEGER + ")" + REGEX_WHITESPACE + "*\\)";

	public final static Pattern COMMAND_BREAK = compile("(;|\\r?\\n|\\r)+");
	public final static Pattern COMMENT = compile("^(//|/\\*)");
	public final static Pattern INCREMENT_DECREMENT = compile("(%(.+)(((\\+){2}|(\\-){2}){1}))|((((\\+){2}|(\\-){2}){1})\\s*%(.+))");

	public final static Pattern LOGICAL_OPERATOR = compile("([\\s&&[^\\r\\n]]+(?i)(or|and)[\\s&&[^\\r\\n]]+)|([\\s&&[^\\r\\n]]*(?i)not[\\s&&[^\\r\\n]]+)");
	public final static Pattern AND_NOT = compile("(?i)and" + REGEX_WHITESPACE + "+not\\b");
	public final static Pattern OR_NOT = compile("(?i)or" + REGEX_WHITESPACE + "+not\\b");

	public final static Pattern RGB_COLOR = compile("^(\\[|\\()" + REGEX_NONNEGATIVE_DECIMAL + REGEX_SEPARATOR + "+"
			+ REGEX_NONNEGATIVE_DECIMAL + REGEX_SEPARATOR + "+" + REGEX_NONNEGATIVE_DECIMAL + "(\\]|\\))");
	public final static Pattern RGBA_COLOR = compile("^(\\[|\\()" + REGEX_NONNEGATIVE_DECIMAL + REGEX_SEPARATOR + "+"
			+ REGEX_NONNEGATIVE_DECIMAL + REGEX_SEPARATOR + "+" + REGEX_NONNEGATIVE_DECIMAL + REGEX_SEPARATOR + "+"
			+ REGEX_NONNEGATIVE_DECIMAL + "(\\]|\\))");

	public final static Pattern ELEMENT = compile("(^(?i)element\\b){1}");
	public final static Pattern ATOM = compile("(^(?i)(atom)|(particle)\\b){1}");
	public final static Pattern RBOND = compile("(^(?i)rbond\\b){1}");
	public final static Pattern ABOND = compile("(^(?i)abond\\b){1}");
	public final static Pattern MOLECULE = compile("(^(?i)molecule\\b){1}");
	public final static Pattern OBSTACLE = compile("(^(?i)obstacle\\b){1}");
	public final static Pattern IMAGE = compile("(^(?i)image\\b){1}");
	public final static Pattern TEXTBOX = compile("(^(?i)textbox\\b){1}");
	public final static Pattern LINE = compile("(^(?i)line\\b){1}");
	public final static Pattern RECTANGLE = compile("(^(?i)rectangle\\b){1}");
	public final static Pattern ELLIPSE = compile("(^(?i)ellipse\\b){1}");
	public final static Pattern BACKGROUND = compile("(^(?i)background\\b){1}");
	public final static Pattern MESSAGE = compile("(^(?i)message\\b){1}");
	public final static Pattern FORMAT_VARIABLE = compile("(((?i)formatvar)(\\s*)\\((.+),(.+)\\)){1}");

	public final static Pattern DELAY = compile("(^(?i)delay\\b){1}");
	public final static Pattern LOAD = compile("(^(?i)load\\b){1}");
	public final static Pattern SOURCE = compile("(^(?i)(script|source)\\b){1}");
	public final static Pattern SELECT = compile("(^(?i)select\\b){1}");

	public final static Pattern DEFINE_VAR = compile("(^(?i)define" + REGEX_WHITESPACE + "+%\\w+){1}");
	public final static Pattern SET_VAR = compile("(^(?i)set" + REGEX_WHITESPACE + "+%\\w+){1}");

	public final static Pattern IF = compile("(^(?i)if){1}" + REGEX_WHITESPACE + "*\\(.+\\)");
	public final static Pattern ELSE = compile("(^(?i)else\\b){1}");
	public final static Pattern ENDIF = compile("(^(?i)endif\\b){1}");
	public final static Pattern WHILE = compile("(^(?i)while){1}" + REGEX_WHITESPACE + "*\\(.+\\)");
	public final static Pattern ENDWHILE = compile("(^(?i)endwhile\\b){1}");

	public final static Pattern ALL = compile("^(?i)(all)|(\\*)\\z");
	public final static Pattern NONE = compile("^(?i)none\\z");
	public final static Pattern NOT_SELECTED = compile("(?i)not" + REGEX_WHITESPACE + "+selected\\b");
	public final static Pattern INDEX = compile(REGEX_NONNEGATIVE_INTEGER + "\\z");
	public final static Pattern INTEGER_GROUP = compile(REGEX_INTEGER_GROUP + "\\z");
	public final static Pattern RANGE = compile("\\d+" + REGEX_WHITESPACE + "*-" + REGEX_WHITESPACE + "*\\d+\\z");
	public final static Pattern RANGE_LEADING = compile("\\d+" + REGEX_WHITESPACE + "*-" + REGEX_WHITESPACE + "*\\d+");
	public final static Pattern MEAN = compile("^<.+>$");

	// public final static Pattern FOUR_NUMBERS = compile(FOUR_NUMBERS_IN_PARENTHESIS);

	/** within (x, y, w, h) */
	public final static Pattern WITHIN_RECTANGLE = compile("((?i)within){1}" + REGEX_WHITESPACE + "*(\\(|\\[)"
			+ REGEX_WHITESPACE + "*((" + REGEX_NONNEGATIVE_DECIMAL + "|" + REGEX_NONNEGATIVE_INTEGER + ")"
			+ REGEX_SEPARATOR + "+){3}" + "(" + REGEX_NONNEGATIVE_DECIMAL + "|" + REGEX_NONNEGATIVE_INTEGER + ")"
			+ REGEX_WHITESPACE + "*(\\]|\\))\\z");

	/** i within (x, y, w, h) */
	public final static Pattern INDEX_WITHIN_RECTANGLE = compile(REGEX_NONNEGATIVE_INTEGER + REGEX_WHITESPACE
			+ "+((?i)within){1}" + REGEX_WHITESPACE + "*(\\(|\\[)" + REGEX_WHITESPACE + "*(("
			+ REGEX_NONNEGATIVE_DECIMAL + "|" + REGEX_NONNEGATIVE_INTEGER + ")" + REGEX_SEPARATOR + "+){3}" + "("
			+ REGEX_NONNEGATIVE_DECIMAL + "|" + REGEX_NONNEGATIVE_INTEGER + ")" + REGEX_WHITESPACE + "*(\\]|\\))\\z");

	/** i-j within (x, y, w, h) */
	public final static Pattern RANGE_WITHIN_RECTANGLE = compile("\\d+" + REGEX_WHITESPACE + "*-" + REGEX_WHITESPACE
			+ "*\\d+" + REGEX_WHITESPACE + "+((?i)within){1}" + REGEX_WHITESPACE + "*(\\(|\\[)" + REGEX_WHITESPACE
			+ "*((" + REGEX_NONNEGATIVE_DECIMAL + "|" + REGEX_NONNEGATIVE_INTEGER + ")" + REGEX_SEPARATOR + "+){3}"
			+ "(" + REGEX_NONNEGATIVE_DECIMAL + "|" + REGEX_NONNEGATIVE_INTEGER + ")" + REGEX_WHITESPACE
			+ "*(\\]|\\))\\z");

	/** within (radius, index) */
	public final static Pattern WITHIN_RADIUS = compile("((?i)within\\b){1}" + REGEX_WHITESPACE + "*\\("
			+ REGEX_WHITESPACE + "*(" + REGEX_NONNEGATIVE_DECIMAL + "|" + REGEX_NONNEGATIVE_INTEGER + ")"
			+ REGEX_WHITESPACE + "*,");

	public final static Pattern MARK_COLOR = compile("(^(?i)mark" + REGEX_WHITESPACE + "+color\\b){1}");
	public final static Pattern SHOW = compile("(^(?i)show\\b){1}");
	public final static Pattern PLOT = compile("(^(?i)plot\\b){1}");

	public final static Pattern PRINT = compile("(^(?i)print\\b){1}");
	public final static Pattern SET = compile("(^(?i)set\\b){1}");
	public final static Pattern ACTION = compile("(^(?i)action\\b){1}");
	public final static Pattern ACTION_ID = compile("_ID");

	public final static Pattern SET_EPSILON = compile("^(?i)epsilon" + REGEX_WHITESPACE + "+"
			+ REGEX_NONNEGATIVE_DECIMAL + REGEX_SEPARATOR + "+" + REGEX_NONNEGATIVE_DECIMAL + REGEX_SEPARATOR + "+"
			+ REGEX_NONNEGATIVE_DECIMAL + "\\z");
	public final static Pattern BUILD_BOND = compile("^(?i)(bond|rbond)" + REGEX_WHITESPACE + "+"
			+ REGEX_NONNEGATIVE_DECIMAL + REGEX_SEPARATOR + "+" + REGEX_NONNEGATIVE_DECIMAL + REGEX_SEPARATOR + "+"
			+ REGEX_NONNEGATIVE_DECIMAL + "\\z");
	public final static Pattern BUILD_BEND = compile("^(?i)(bond|abond)" + REGEX_WHITESPACE + "+"
			+ REGEX_NONNEGATIVE_DECIMAL + REGEX_SEPARATOR + "+" + REGEX_NONNEGATIVE_DECIMAL + REGEX_SEPARATOR + "+"
			+ REGEX_NONNEGATIVE_DECIMAL + REGEX_SEPARATOR + "+" + REGEX_NONNEGATIVE_DECIMAL + "\\z");

	public final static Pattern ADD = compile("(^(?i)add\\b){1}");
	public final static Pattern MOVE = compile("(^(?i)move\\b){1}");
	public final static Pattern ROTATE = compile("(^(?i)rotate\\b){1}");
	public final static Pattern CHARGE = compile("(^(?i)charge\\b){1}");
	public final static Pattern RESTRAIN = compile("(^(?i)restrain\\b){1}");
	public final static Pattern DAMP = compile("(^(?i)damp\\b){1}");
	public final static Pattern HEAT = compile("(^(?i)heat\\b){1}");
	public final static Pattern TRAJECTORY = compile("(^(?i)(trajectory|traj)\\b){1}");
	public final static Pattern MINIMIZE = compile("(^(?i)minimize\\b){1}");
	public final static Pattern ATTACH = compile("(^(?i)attach\\b){1}");
	public final static Pattern SOUND = compile("(^(?i)sound\\b){1}");
	public final static Pattern STORE = compile("(^(?i)store\\b){1}");

	public final static Pattern ELEMENT_FIELD = compile("^%?((?i)element){1}(\\[){1}" + REGEX_WHITESPACE + "*("
			+ REGEX_NONNEGATIVE_INTEGER + "|" + REGEX_NONNEGATIVE_DECIMAL + ")" + REGEX_WHITESPACE + "*(\\]){1}\\.");
	public final static Pattern ATOM_FIELD = compile("^%?((?i)atom){1}(\\[){1}" + REGEX_WHITESPACE + "*("
			+ REGEX_NONNEGATIVE_INTEGER + "|" + REGEX_NONNEGATIVE_DECIMAL + ")" + REGEX_WHITESPACE + "*(\\]){1}\\.");
	public final static Pattern RBOND_FIELD = compile("^%?((?i)rbond){1}(\\[){1}" + REGEX_WHITESPACE + "*("
			+ REGEX_NONNEGATIVE_INTEGER + "|" + REGEX_NONNEGATIVE_DECIMAL + ")" + REGEX_WHITESPACE + "*(\\]){1}\\.");
	public final static Pattern ABOND_FIELD = compile("^%?((?i)abond){1}(\\[){1}" + REGEX_WHITESPACE + "*("
			+ REGEX_NONNEGATIVE_INTEGER + "|" + REGEX_NONNEGATIVE_DECIMAL + ")" + REGEX_WHITESPACE + "*(\\]){1}\\.");
	public final static Pattern PARTICLE_FIELD = compile("^%?((?i)particle){1}(\\[){1}" + REGEX_WHITESPACE + "*("
			+ REGEX_NONNEGATIVE_INTEGER + "|" + REGEX_NONNEGATIVE_DECIMAL + ")" + REGEX_WHITESPACE + "*(\\]){1}\\.");
	public final static Pattern OBSTACLE_FIELD = compile("^%?((?i)obstacle){1}(\\[){1}" + REGEX_WHITESPACE + "*("
			+ REGEX_NONNEGATIVE_INTEGER + "|" + REGEX_NONNEGATIVE_DECIMAL + ")" + REGEX_WHITESPACE + "*(\\]){1}\\.");
	public final static Pattern IMAGE_FIELD = compile("^%?((?i)image){1}(\\[){1}" + REGEX_WHITESPACE + "*("
			+ REGEX_NONNEGATIVE_INTEGER + "|" + REGEX_NONNEGATIVE_DECIMAL + ")" + REGEX_WHITESPACE + "*(\\]){1}\\.");
	public final static Pattern LINE_FIELD = compile("^%?((?i)line){1}(\\[){1}" + REGEX_WHITESPACE + "*("
			+ REGEX_NONNEGATIVE_INTEGER + "|" + REGEX_NONNEGATIVE_DECIMAL + ")" + REGEX_WHITESPACE + "*(\\]){1}\\.");
	public final static Pattern RECTANGLE_FIELD = compile("^%?((?i)rectangle){1}(\\[){1}" + REGEX_WHITESPACE + "*("
			+ REGEX_NONNEGATIVE_INTEGER + "|" + REGEX_NONNEGATIVE_DECIMAL + ")" + REGEX_WHITESPACE + "*(\\]){1}\\.");
	public final static Pattern ELLIPSE_FIELD = compile("^%?((?i)ellipse){1}(\\[){1}" + REGEX_WHITESPACE + "*("
			+ REGEX_NONNEGATIVE_INTEGER + "|" + REGEX_NONNEGATIVE_DECIMAL + ")" + REGEX_WHITESPACE + "*(\\]){1}\\.");
	public final static Pattern TEXTBOX_FIELD = compile("^%?((?i)textbox){1}(\\[){1}" + REGEX_WHITESPACE + "*("
			+ REGEX_NONNEGATIVE_INTEGER + "|" + REGEX_NONNEGATIVE_DECIMAL + ")" + REGEX_WHITESPACE + "*(\\]){1}\\.");

	public final static Pattern IMAGE_EXTENSION = compile("(?)\\.((jpeg)|(jpg)|(gif)|(png))");
	public final static Pattern TEXT_EXTENSION = compile("(?)\\.txt");
	public final static Pattern HTML_EXTENSION = compile("(?)\\.((html)|(htm))");

}
