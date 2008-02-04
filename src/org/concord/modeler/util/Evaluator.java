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

package org.concord.modeler.util;

import java.util.Hashtable;

import org.nfunk.jep.JEP;

/**
 * This class is a simple wrapper around JEP package.
 * 
 * @author Dima Markman
 * @author Connie J. Chen
 */

public class Evaluator {

	/* JEP evaluator */
	private static JEP evaluator;

	/* The current expression */
	private String expression;

	private boolean allowUndeclared;

	private final Object lock = new Object();

	/**
	 * 
	 * @param expression
	 *            Example: g*t^2/2
	 */
	public Evaluator(String expression) {
		this(expression, true);
	}

	private Evaluator(String expression, boolean allowUndeclared) {
		this.allowUndeclared = allowUndeclared;
		init(allowUndeclared);
		setExpression(expression);
	}

	public Evaluator() {
		this(null, false);
	}

	public Hashtable getSymbolTable() {
		return evaluator.getSymbolTable();
	}

	public Hashtable getFunctionTable() {
		return evaluator.getFunTab();
	}

	private void init(boolean allowUndeclared) {
		if (evaluator == null) {
			evaluator = new JEP();
			evaluator.addStandardConstants();
			evaluator.addSpecialFunctions();
			evaluator.addStandardFunctions();
		}
		evaluator.setAllowUndeclared(allowUndeclared);
	}

	/**
	 * Set an expression for the evaluator
	 * 
	 * @param expression
	 *            Example: g*t^2/2
	 */
	public void setExpression(String expression) {
		synchronized (lock) {
			this.expression = expression == null ? null : expression.trim();
		}
	}

	public void setImplicitMultiplication(boolean b) {
		if (evaluator == null)
			return;
		synchronized (lock) {
			evaluator.setImplicitMul(b);
		}
	}

	public String getErrorInfo() {
		if (evaluator == null)
			return null;
		synchronized (lock) {
			return evaluator.getErrorInfo();
		}
	}

	/**
	 * Get current expression's value
	 * 
	 * @return a value of the Evaluator's expression
	 */

	public String getExpression() {
		synchronized (lock) {
			return expression;
		}
	}

	public void addVariable(String name) {
		setVariableValue(name, 0);
	}

	/**
	 * Set an value for the particular variable
	 * 
	 * @param name
	 *            variable name
	 * @param value
	 *            variable value
	 * @exception IllegalArgumentException
	 *                if expression doesn't contain variable
	 */
	public void setVariableValue(String name, double value) throws IllegalArgumentException {
		if (evaluator == null)
			return;
		synchronized (lock) {
			if (!allowUndeclared || evaluator.getSymbolTable().containsKey(name)) {
				evaluator.addVariable(name, value);
			}
			else {
				throw new IllegalArgumentException("Expression " + expression + " doesn't contain variable " + name);
			}
		}
	}

	/**
	 * Set an value for the particular variable
	 * 
	 * @param name
	 *            variable name
	 * @return a current value of the variable
	 * @exception IllegalArgumentException
	 *                if expression doesn't contain variable
	 */
	public double getVariableValue(String name) throws IllegalArgumentException {
		if (evaluator == null)
			return 0;
		double retValue = 0;
		synchronized (lock) {
			if (evaluator.getSymbolTable().containsKey(name)) {
				Object o = evaluator.getSymbolTable().get(name);
				if (o instanceof Number) {
					retValue = ((Number) o).doubleValue();
				}
			}
			else {
				throw new IllegalArgumentException("Expression " + expression + " doesn't contain variable " + name);
			}
			return retValue;
		}
	}

	/**
	 * check for existance the particular variable in the expression
	 * 
	 * @param name
	 *            variable name
	 * @return true if variable is in the expression
	 */
	public boolean isVariableInExpression(String name) {
		if (evaluator == null)
			return false;
		synchronized (lock) {
			return evaluator.getSymbolTable().containsKey(name);
		}
	}

	/** Remove all variables from expression */
	public void removeAllVariables() {
		if (evaluator == null)
			return;
		synchronized (lock) {
			evaluator.getSymbolTable().clear();
			evaluator.addStandardConstants();
		}
	}

	/**
	 * Remove the particular variable
	 * 
	 * @param name
	 *            variable name
	 * @exception IllegalArgumentException
	 *                if expression doesn't contain variable
	 */
	public void removeVariable(String name) {
		if (evaluator == null)
			return;
		synchronized (lock) {
			if (evaluator.getSymbolTable().containsKey(name)) {
				evaluator.removeVariable(name);
			}
			else {
				throw new IllegalArgumentException("Expression " + expression + " doesn't contain variable " + name);
			}
		}
	}

	/**
	 * Evaluate expression
	 * 
	 * @return a value of the evaluated expression
	 * @exception EvaluationException
	 *                if it was an error
	 */
	public double eval() throws EvaluationException {
		if (evaluator == null || expression == null)
			return 0;
		if (expression.startsWith("0x")) {
			try {
				return Integer.valueOf(expression.substring(2), 16);
			}
			catch (NumberFormatException e) {
				throw new EvaluationException("Evaluation exception: " + e.getMessage());
			}
		}
		if (expression.startsWith("#")) {
			try {
				return Integer.valueOf(expression.substring(1), 16);
			}
			catch (NumberFormatException e) {
				throw new EvaluationException("Evaluation exception: " + e.getMessage());
			}
		}
		synchronized (lock) {
			evaluator.parseExpression(expression);
			if (evaluator.hasError())
				throw new EvaluationException("Evaluation exception: " + evaluator.getErrorInfo());
			return evaluator.getValue();
		}
	}

}