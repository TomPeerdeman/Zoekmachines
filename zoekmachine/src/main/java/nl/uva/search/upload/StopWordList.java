/**
 * File: StopWordList.java
 * Author: Tom Peerdeman
 */
package nl.uva.search.upload;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashSet;

import javax.servlet.ServletContext;

/**
 * @author Tom Peerdeman
 * 
 */
public class StopWordList {
	private HashSet<String> wordList;
	
	/**
	 * @param context
	 * @throws IOException
	 */
	public StopWordList(ServletContext context) throws IOException {
		wordList = new HashSet<String>();
		
		BufferedReader in =
			new BufferedReader(new InputStreamReader(
					context.getResourceAsStream("/WEB-INF/stop.txt")));
		String line;
		while((line = in.readLine()) != null) {
			if(!line.startsWith("#") && line.length() > 0) {
				wordList.add(line);
			}
		}
		
		in.close();
	}
	
	public boolean inList(String word) {
		return wordList.contains(word);
	}
}
