/*****************************************************************************

JEP - Java Math Expression Parser 2.24
      December 30 2002
      (c) Copyright 2002, Nathan Funk
      See LICENSE.txt for license information.

*****************************************************************************/
package org.nfunk.jep;
import java.util.Hashtable;

public class FunctionTable extends Hashtable
{
	public FunctionTable(){
	}
	
	/** overrided by Connie Chen to recognize both upper-and-lower case*/
	@SuppressWarnings("unchecked")
	public Object put(Object key, Object value){
		if(key instanceof String){
			return super.put(((String)key).toLowerCase(), value);
		}
		return super.put(key, value);
	}

	/** overrided by Connie Chen to recognize both upper-and-lower case*/
	public Object get(Object key){
		if(key instanceof String){
			return super.get(((String)key).toLowerCase());
		}
		return super.get(key);
	}
	
	/** overrided by Connie Chen to recognize both upper-and-lower case*/
	public boolean containsKey(Object key){
		if(key instanceof String){
			return super.containsKey(((String)key).toLowerCase());
		}
		return super.containsKey(key);
	}

	/** overrided by Connie Chen to recognize both upper-and-lower case*/
	public boolean contains(Object key){
		if(key instanceof String){
			return super.contains(((String)key).toLowerCase());
		}
		return super.contains(key);
	}
	
	/** overrided by Connie Chen to recognize both upper-and-lower case*/
	public Object remove(Object key){
		if(key instanceof String){
			return super.remove(((String)key).toLowerCase());
		}
		return super.remove(key);
	}

}
