/**
 * File: UploadXMLParser.java
 * Author: Tom Peerdeman
 */
package nl.uva.search.upload;

import java.sql.Connection;
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
	private Map<String, String> docDescr;
	private StringBuilder text;
	
	private StringBuilder questions;
	private StringBuilder questioners;
	private StringBuilder questioners_party;
	private StringBuilder answers;
	private StringBuilder answerers;
	private StringBuilder answerers_ministry;
	
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
		columnMapping.put("Document-id", "doc_id");
		columnMapping.put("Trefwoorden", "keywords");
		
		docDescr = new HashMap<String, String>();
		
		questions = new StringBuilder();
		questioners = new StringBuilder();
		questioners_party = new StringBuilder();
		answers = new StringBuilder();
		answerers = new StringBuilder();
		answerers_ministry = new StringBuilder();
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
				} else if(itemAttr.equalsIgnoreCase("Bibliografische_omschrijving")) {
					// Parse title
					int start = text.lastIndexOf("over");
					start += 5;
					if(start < 5) {
						start = text.lastIndexOf("inzake");
						start += 7;
						if(start < 7) {
							start = -1;
						}
					}
					
					if(start > 5) {
						int end = text.lastIndexOf("(");
						System.out.printf("Title end %d\n", end);
						if(end > 0) {
							String title =
								text.substring(start + 1, end);
							// Add capitalized first letter
							title =
								text.substring(start, start + 1).toUpperCase()
										+ title;
							docDescr.put("title", escape(title));
						}
					}
				}
			} else if(qName.equalsIgnoreCase("vraag")) {
				questions.append(escape(attr.get("nummer")));
				questions.append(" ");
				questions.append(escape(text.toString()));
				questions.append(" ");
			} else if(qName.equalsIgnoreCase("antwoord")) {
				answers.append(escape(attr.get("nummer")));
				answers.append(" ");
				answers.append(escape(text.toString()));
				answers.append(" ");
			} else if(qName.equalsIgnoreCase("vrager")) {
				questioners_party.append(escape(attr.get("partij")));
				questioners_party.append(" ");
				questioners.append(escape(text.toString()));
				questioners.append(" ");
			} else if(qName.equalsIgnoreCase("antwoorder")) {
				answerers_ministry.append(escape(attr.get("ministerie")));
				answerers_ministry.append(" ");
				answerers.append(escape(text.toString()));
				answerers.append(" ");
			} else if(qName.equalsIgnoreCase("kvr")) {
				// End of document
				
				docDescr.put("questions", questions.toString());
				docDescr.put("questioners", questioners.toString());
				docDescr.put("questioners_party", questioners_party.toString());
				
				docDescr.put("answers", answers.toString());
				docDescr.put("answerers", answerers.toString());
				docDescr.put("answerers_ministry",
						answerers_ministry.toString());
				
				insertDocument();
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
			try {
				stm = db.createStatement();
				stm.execute("INSERT INTO documents (" + cols + ")  VALUES ("
						+ values + ")");
			} catch(SQLException e) {
				throw new SAXException(e);
			} finally {
				if(stm != null) {
					try {
						stm.close();
					} catch(SQLException e) {
					}
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
