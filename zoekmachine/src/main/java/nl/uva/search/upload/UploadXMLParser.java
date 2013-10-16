/**
 * File: UploadXMLParser.java
 * Author: Tom Peerdeman
 */
package nl.uva.search.upload;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * @author Tom Peerdeman
 * 
 */
public class UploadXMLParser extends DefaultHandler {
	private final Map<String, String> columnMapping;
	private final Pattern datePattern =
		Pattern.compile("^(\\d{4})-(\\d{1,2})-(\\d{1,2})$");
	
	private LinkedList<NameAttributePair> stack;
	private Connection db;
	private int docId = -1;
	private Map<String, String> docDescr;
	private StringBuilder text;
	
	/**
	 * @param db
	 */
	public UploadXMLParser(Connection db) {
		this.db = db;
		
		stack = new LinkedList<NameAttributePair>();
		text = new StringBuilder();
		
		columnMapping = new HashMap<String, String>();
		columnMapping.put("Inhoud", "contents");
		columnMapping.put("Rubriek", "category");
		columnMapping.put("Afkomstig_van", "origin");
		columnMapping.put("Document-id", "doc_id");
		
		docDescr = new HashMap<String, String>();
	}
	
	@Override
	public void startElement(String uri, String localName, String qName,
			Attributes attributes) throws SAXException {
		if(!qName.equalsIgnoreCase("voetnoot")) {
			// Clear text buffer
			text.delete(0, text.length());
		}
		
		// Add to stack
		stack.addLast(new NameAttributePair(qName, attributes));
	}
	
	@Override
	public void endElement(String uri, String localName,
			String qName) throws SAXException {
		NameAttributePair pair = stack.getLast();
		if(pair.getName().equalsIgnoreCase(qName)) {
			stack.removeLast();
			Map<String, String> attr = pair.getAttributes();
			
			if(qName.equalsIgnoreCase("item") && attr.containsKey("attribuut")) {
				String itemAttr = attr.get("attribuut");
				
				if(columnMapping.containsKey(itemAttr)) {
					docDescr.put(columnMapping.get(itemAttr), text.toString());
				} else if(itemAttr.equalsIgnoreCase("Datum_indiening")) {
					Matcher m = datePattern.matcher(text);
					if(m.matches()) {
						docDescr.put("entering_year", "" + m.group(1));
						docDescr.put("entering_month", "" + m.group(2));
						docDescr.put("entering_day", "" + m.group(3));
					}
				} else if(itemAttr.equalsIgnoreCase("Datum_reaktie")) {
					Matcher m = datePattern.matcher(text);
					if(m.matches()) {
						docDescr.put("answering_year", "" + m.group(1));
						docDescr.put("answering_month", "" + m.group(2));
						docDescr.put("answering_day", "" + m.group(3));
					}
				}
			} else if(qName.equalsIgnoreCase("metadata") && docId < 0
					&& docDescr.size() > 0) {
				insertDocument();
			} else if(qName.equalsIgnoreCase("vraag")) {
				insertQuestion(attr.get("nummer"), text.toString());
			} else if(qName.equalsIgnoreCase("antwoord")) {
				insertAnswer(attr.get("nummer"), text.toString());
			} else if(qName.equalsIgnoreCase("vrager")) {
				insertQuestioner(attr.get("partij"), text.toString());
			} else if(qName.equalsIgnoreCase("antwoorder")) {
				insertAnswerer(attr.get("functie"), attr.get("ministerie"),
						text.toString());
			}
		}
	}
	
	@Override
	public void characters(char ch[], int start, int length)
			throws SAXException {
		text.append(ch, start, length);
	}
	
	private void insertDocument() throws SAXException {
		String cols = "";
		String values = "";
		for(Entry<String, String> e : docDescr.entrySet()) {
			cols += e.getKey() + ", ";
			values += "'" + escape(e.getValue()) + "', ";
		}
		
		System.out.println(values);
		
		if(cols.length() > 0 && values.length() > 0) {
			cols = cols.substring(0, cols.length() - 2);
			values = values.substring(0, values.length() - 2);
			
			Statement stm = null;
			ResultSet res = null;
			try {
				stm = db.createStatement();
				stm.execute("INSERT INTO documents (" + cols + ")  VALUES ("
						+ values + ")", Statement.RETURN_GENERATED_KEYS);
				res = stm.getGeneratedKeys();
				
				if(res.next()) {
					docId = res.getInt(1);
					docDescr.clear();
				}
			} catch(SQLException e) {
				e.printStackTrace();
			} finally {
				if(res != null) {
					try {
						res.close();
					} catch(SQLException e) {
					}
				}
				if(stm != null) {
					try {
						stm.close();
					} catch(SQLException e) {
					}
				}
			}
		}
	}
	
	private void insertQuestion(String number, String text) throws SAXException {
		Statement stm = null;
		try {
			stm = db.createStatement();
			stm.execute("INSERT INTO questions (doc_id, question_id, question_text)"
					+ " VALUES ('"
					+ docId + "', '" + number + "', '" + escape(text) + "'"
					+ ")");
		} catch(SQLException e) {
			e.printStackTrace();
		} finally {
			if(stm != null) {
				try {
					stm.close();
				} catch(SQLException e) {
				}
			}
		}
	}
	
	private void insertAnswer(String number, String text) throws SAXException {
		Statement stm = null;
		try {
			
			stm = db.createStatement();
			stm.execute("INSERT INTO answers (doc_id, question_id, answer_text)"
					+ " VALUES ('"
					+ docId + "', '" + number + "', '" + escape(text) + "'"
					+ ")");
		} catch(SQLException e) {
			e.printStackTrace();
			throw new SAXException(
					"INSERT INTO answers (doc_id, question_id, answer_text)"
							+ " VALUES ('"
							+ docId + "', '" + number + "', '" + escape(text)
							+ "'"
							+ ")");
		} finally {
			if(stm != null) {
				try {
					stm.close();
				} catch(SQLException e) {
				}
			}
		}
	}
	
	private void insertQuestioner(String party, String name)
			throws SAXException {
		Statement stm = null;
		try {
			stm = db.createStatement();
			stm.execute("INSERT INTO questioners (doc_id, questioner_party, questioner_name)"
					+ " VALUES ('"
					+ docId
					+ "', '"
					+ escape(party)
					+ "', '"
					+ escape(name)
					+ "'"
					+ ")");
		} catch(SQLException e) {
			e.printStackTrace();
		} finally {
			if(stm != null) {
				try {
					stm.close();
				} catch(SQLException e) {
				}
			}
		}
	}
	
	private void insertAnswerer(String function, String ministry, String name)
			throws SAXException {
		Statement stm = null;
		try {
			stm = db.createStatement();
			stm.execute("INSERT INTO answerers "
					+ "(doc_id, answerer_function, answerer_ministry, answerer_name)"
					+ " VALUES ('"
					+ docId
					+ "', '"
					+ escape(function)
					+ "', '"
					+ escape(ministry)
					+ "', '"
					+ escape(name)
					+ "'"
					+ ")");
		} catch(SQLException e) {
			e.printStackTrace();
		} finally {
			if(stm != null) {
				try {
					stm.close();
				} catch(SQLException e) {
				}
			}
		}
	}
	
	private String escape(String val) {
		return val.trim().replaceAll("'", "");
	}
	
	private class NameAttributePair {
		private final String name;
		private final Map<String, String> attributes;
		
		/**
		 * @param name
		 * @param attr
		 */
		public NameAttributePair(String name, Attributes attr) {
			this.name = name;
			// The Attributes object is reused, so we can't simply store it.
			attributes = new HashMap<String, String>(1);
			for(int i = 0; i < attr.getLength(); i++) {
				attributes.put(attr.getLocalName(i), attr.getValue(i));
			}
		}
		
		/**
		 * @return the name
		 */
		public String getName() {
			return name;
		}
		
		/**
		 * @return the attributes
		 */
		public Map<String, String> getAttributes() {
			return attributes;
		}
	}
}
