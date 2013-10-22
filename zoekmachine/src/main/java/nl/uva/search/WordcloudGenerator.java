/**
 * File: WordcloudGenerator.java
 * Author: Tom Peerdeman
 */
package nl.uva.search;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletException;

/**
 * @author Tom Peerdeman
 * 
 */
public class WordcloudGenerator {
	private static final Pattern patternComma = Pattern.compile(", ");
	private static final Pattern patternColon = Pattern.compile("(\\d+):(.+)");
	
	/**
	 * @param conn
	 * @param type
	 * @param docId
	 * @param out
	 * @throws IOException
	 * @throws ServletException
	 */
	public WordcloudGenerator(Connection conn, String type, int docId,
			PrintWriter out)
			throws IOException, ServletException {
		
		Statement stm = null;
		ResultSet res = null;
		try {
			stm = conn.createStatement();
			res =
				stm.executeQuery("SELECT data FROM wordclouds WHERE doc_id='"
						+ docId + "' AND type='" + type + "' LIMIT 1");
			
			int max = 0;
			int min = Integer.MAX_VALUE;
			List<WordcloudEntry> entries = new LinkedList<WordcloudEntry>();
			if(res.next()) {
				String[] parts = patternComma.split(res.getString(1));
				for(String part : parts) {
					if(part.length() == 0) {
						break;
					}
					
					Matcher m = patternColon.matcher(part);
					if(m.matches()) {
						int n = Integer.parseInt(m.group(1));
						entries.add(new WordcloudEntry(m.group(2), n));
						if(n > max) {
							max = n;
						}
						
						if(n < min) {
							min = n;
						}
					}
				}
				
				Collections.shuffle(entries);
				
				int dSize = max - min;
				double mod = 40.0 / (double) dSize;
				
				Random r = new Random();
				
				out.print("<div style='width: 500px;'>");
				for(WordcloudEntry e : entries) {
					out.print("<span style='font-size: " + e.getSize(mod, min)
							+ "px; color: rgb(" + r.nextInt(256) + ","
							+ r.nextInt(256) + "," + r.nextInt(256) + ")'>");
					out.print(e.getWord());
					out.print("</span> ");
				}
				out.print("</div>");
			}
		} catch(SQLException e1) {
			e1.printStackTrace();
			throw new ServletException(e1);
		} finally {
			// Close statement
			if(stm != null) {
				try {
					stm.close();
				} catch(SQLException e) {
					e.printStackTrace();
				}
			}
			
			// Close result
			if(res != null) {
				try {
					res.close();
				} catch(SQLException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	private class WordcloudEntry {
		private final String word;
		private final int size;
		
		/**
		 * @param word
		 * @param size
		 */
		public WordcloudEntry(String word, int size) {
			super();
			this.word = word;
			this.size = size;
		}
		
		/**
		 * @return the word
		 */
		public String getWord() {
			return word;
		}
		
		/**
		 * @return the size
		 */
		public int getSize(double modifier, int min) {
			return (int) Math.round((size - min) * modifier) + 12;
		}
	}
}
