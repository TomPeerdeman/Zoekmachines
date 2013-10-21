/**
 * File: ValueComparator.java
 * 
 */
package nl.uva.search;

import java.util.Comparator;
import java.util.Map;

/**
 * Source:
 * http://stackoverflow.com/a/1283722
 * 
 */
public class ValueComparator implements Comparator<String> {
	Map<String, Integer> base;
	
	public ValueComparator(Map<String, Integer> base) {
		this.base = base;
	}
	
	// Note: this comparator imposes orderings that are inconsistent with
	// equals.
	public int compare(String a, String b) {
		if(base.get(a) >= base.get(b)) {
			return -1;
		} else {
			return 1;
		} // returning 0 would merge keys
	}
}