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
import java.util.regex.Pattern;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * @author Tom Peerdeman
 * 
 */
public class UploadXMLParser extends DefaultHandler {
	private final Pattern numberPattern = Pattern.compile("^\\d+$");
	
	private final Map<String, String> columnMapping;
	
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
		columnMapping.put("Trefwoorden", "keywords");
		columnMapping.put("Datum_indiening", "entering_date");
		columnMapping.put("Datum_reaktie", "answering_date");
		
		docDescr = new HashMap<String, String>();
		
		questions = new StringBuilder();
		questioners = new StringBuilder();
		questioners_party = new StringBuilder();
		answers = new StringBuilder();
		answerers = new StringBuilder();
		answerers_ministry = new StringBuilder();
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.xml.sax.helpers.DefaultHandler#startElement(java.lang.String,
	 * java.lang.String, java.lang.String, org.xml.sax.Attributes)
	 */
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
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.xml.sax.helpers.DefaultHandler#endElement(java.lang.String,
	 * java.lang.String, java.lang.String)
	 */
	@Override
	public void endElement(String uri, String localName,
			String qName) throws SAXException {
		// Get item from stack
		NameAttributePair pair = stack.getLast();
		if(pair.getName().equalsIgnoreCase(qName)) {
			// Remove item from stack, we don't need it anymore
			stack.removeLast();
			
			// Get the XML attributes
			Map<String, String> attr = pair.getAttributes();
			
			// Parse <item> entries
			if(qName.equalsIgnoreCase("item") && attr.containsKey("attribuut")) {
				// Get the item type
				String itemAttr = attr.get("attribuut");
				
				if(columnMapping.containsKey(itemAttr)) {
					// Map xml known items to database columns
					docDescr.put(columnMapping.get(itemAttr), text.toString());
				} else if(itemAttr.equalsIgnoreCase("Bibliografische_omschrijving")) {
					// Parse title
					String titleText = text.toString();
					
					// Find start of block, starts with 'over' or 'inzake'
					int start = titleText.indexOf("over");
					start += 5;
					if(start < 5) {
						start = titleText.indexOf("inzake");
						start += 7;
						if(start < 7) {
							start = -1;
						}
					}
					
					if(start > 5) {
						// Find end of block, ends with '(Ingezonden' or just
						// end of the text
						int end = titleText.lastIndexOf("(Ingezonden");
						if(end < 0 || start + 1 >= end) {
							end = titleText.lastIndexOf(";");
							if(end < 0 || start + 1 >= end) {
								end = titleText.length();
							}
						}
						
						// Block is valid
						if(end > 0) {
							String title =
								titleText.substring(start + 1, end);
							
							// Add capitalized first letter
							title =
								titleText.substring(start, start + 1)
											.toUpperCase()
										+ title;
							
							// Trim start & ending whitespace
							title = title.trim();
							
							// Remove ending dot
							if(title.endsWith(".")) {
								title = title.substring(0, title.length() - 1);
							}
							docDescr.put("title", title);
						}
					} else {
						// Put full description to prevent NULL titles
						docDescr.put("title", escape(text.toString()));
					}
				} else if(itemAttr.equalsIgnoreCase("Document-id")) {
					if(numberPattern.matcher(text).matches()) {
						// Only numbers in doc id, prefix with SG_KVR
						docDescr.put("doc_id",
								"SG_KVR" + escape(text.toString()));
					} else {
						// Insert as is
						docDescr.put("doc_id", escape(text.toString()));
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
				// Is the patrij attribute present, if not insert 'Unknown'
				if(attr.get("partij") == null || attr.get("partij").equals("")) {
					questioners_party.append("Unknown");
				} else {
					questioners_party.append(escape(attr.get("partij")));
				}
				questioners_party.append(", ");
				questioners.append(escape(text.toString()));
				questioners.append(", ");
			} else if(qName.equalsIgnoreCase("antwoorder")) {
				// Is the ministerie attribute present, if not insert 'Unknown'
				if(attr.get("ministerie") == null
						|| attr.get("ministerie").equals("")) {
					// Only insert unknown if we know the person
					if(text.length() > 0) {
						answerers_ministry.append("Unknown");
					}
				} else {
					answerers_ministry.append(escape(attr.get("ministerie")));
				}
				answerers_ministry.append(", ");
				answerers.append(escape(text.toString()));
				answerers.append(", ");
			} else if(qName.equalsIgnoreCase("kvr")) {
				// End of document
				
				docDescr.put("questions", questions.toString());
				docDescr.put("questioners", questioners.toString().substring(0,
						questioners.length() - 2));
				docDescr.put("questioners_party",
						questioners_party.toString().substring(0,
								questioners_party.length() - 2));
				
				docDescr.put("answers", answers.toString());
				docDescr.put("answerers", answerers.toString().substring(0,
						answerers.length() - 2));
				docDescr.put("answerers_ministry",
						answerers_ministry.toString().substring(0,
								answerers_ministry.length() - 2));
				
				insertDocument();
			}
		}
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.xml.sax.helpers.DefaultHandler#characters(char[], int, int)
	 */
	@Override
	public void characters(char ch[], int start, int length)
			throws SAXException {
		text.append(ch, start, length);
	}
	
	private void insertDocument() throws SAXException {
		String cols = "";
		String values = "";
		for(Entry<String, String> e : docDescr.entrySet()) {
			// Ignore weird date's this indicates there is no answer anyway
			if(e.getKey().equals("answering_date")
					&& e.getValue().equals("99-99-9999")) {
				continue;
			}
			
			cols += e.getKey() + ", ";
			values += "'" + escape(e.getValue()) + "', ";
		}
		
		if(cols.length() > 0 && values.length() > 0) {
			cols = cols.substring(0, cols.length() - 2);
			values = values.substring(0, values.length() - 2);
			
			Statement stm = null;
			try {
				// Perform the insert
				stm = db.createStatement();
				stm.execute("INSERT INTO documents (" + cols + ")  VALUES ("
						+ values + ")");
			} catch(SQLException e) {
				throw new SAXException("Query: " + "INSERT INTO documents ("
						+ cols + ")  VALUES ("
						+ values + ")\nError: " + e);
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
		// Remove starting & ending whitespace, remove "'" and "\" characters
		return val.trim().replaceAll("'|\\\\", "");
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
