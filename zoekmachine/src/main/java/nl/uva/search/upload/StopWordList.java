/**
 * File: StopWordList.java
 * 
 */
package nl.uva.search.upload;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashSet;

import javax.servlet.ServletContext;

/**
 * @author Ruben Janssen
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
	
	/**
	 * @param word
	 * @return True if the word is in the stop word list, otherwise false
	 */
	public boolean inList(String word) {
		return wordList.contains(word);
	}
}
