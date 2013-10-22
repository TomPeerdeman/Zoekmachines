/**
 * File: QueryGenerator.java
 * 
 */
package nl.uva.search;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author Ruben Janssen
 * @author Tom Peerdeman
 * 
 */
public class QueryGenerator {
	private List<String> where;
	private Map<String, String[]> parameters;
	
	/**
	 * @param parameters
	 */
	public QueryGenerator(Map<String, String[]> parameters) {
		this.parameters = parameters;
		where = new LinkedList<String>();
		
		addWhereLike("doc_id", "doc_id");
		addWhereLike("title", "title");
		addWhereLike("category", "category");
		addWhereLike("answerers", "answerers");
		addWhereLike("keywords", "keywords");
		addWhereLike("questioners", "questioners");
		addWhereRLike("questioners_party", "questioners_party",
				"^(.+, )?%VAL%(, .+)?$");
		addWhereLike("answerers_ministry", "answerers_ministry");
		addWhereMatch(new String[] {"questions"}, "questions");
		addWhereMatch(new String[] {"answers"}, "answers");
		addWhereBetween("entering_date", new String[] {"entering_min",
														"entering_max"}, false);
		String answered = getParameter("answered");
		if(answered == null || !answered.equals("n")) {
			addWhereBetween("answering_date", new String[] {"answering_min",
															"answering_max"},
					true);
			if(answered != null && answered.equals("y")) {
				where.add("NOT answers=''");
				where.add("answers IS NOT NULL");
			}
		} else {
			where.add("(answers='' OR answers IS NULL)");
		}
	}
	
	private void addWhereLike(String db, String param) {
		String v = getParameter(param);
		if(v != null && v.length() > 0) {
			where.add(db + " LIKE '%" + v + "%'");
		}
	}
	
	private void addWhereRLike(String db, String param, String regex) {
		String v = getParameter(param);
		if(v != null && v.length() > 0) {
			where.add(db + " RLIKE '" + regex.replaceAll("%VAL%", v) + "'");
		}
	}
	
	private void addWhereMatch(String[] dbParams, String param) {
		String v = getParameter(param);
		if(v != null && v.length() > 0) {
			String db = "";
			for(String p : dbParams) {
				db += p.trim() + ", ";
			}
			if(db.length() > 0) {
				db = db.substring(0, db.length() - 2);
				where.add("MATCH (" + db + ") AGAINST('%" + v + "%')");
			}
		}
	}
	
	private void addWhereBetween(String db, String[] params, boolean allowNull) {
		if(params.length != 2) {
			throw new IllegalArgumentException();
		}
		
		String v1 = getParameter(params[0]);
		String v2 = getParameter(params[1]);
		
		if(v1 != null && v2 != null && v1.length() > 0 && v2.length() > 0) {
			if(!allowNull) {
				// x BETWEEN x1 AND x2
				where.add(db + " BETWEEN '" + v1 + "' AND '" + v2 + "'");
			} else {
				// ((x BETWEEN x1 AND x2) OR x IS NULL)
				where.add("((" + db + " BETWEEN '" + v1 + "' AND '" + v2
						+ "') OR " + db + " IS NULL)");
			}
		}
	}
	
	private String getParameter(String name) {
		if(parameters.containsKey(name)) {
			String[] arr = parameters.get(name);
			if(arr != null && arr.length > 0 && arr[0] != null) {
				return arr[0];
			}
		}
		return null;
	}
	
	/**
	 * @param select
	 *            Contains SELECT x FROM y
	 * @param whereExtra
	 *            Extra clauses in the where
	 * @param tail
	 *            Extra requirements such as LIMIT, ORDER, GROUP BY
	 * @return The generated query
	 */
	public String generate(String select, String[] whereExtra, String tail) {
		StringBuilder query = new StringBuilder();
		query.append(select.trim());
		query.append(" ");
		
		boolean first = true;
		if(where.size() > 0) {
			for(String v : where) {
				if(first) {
					query.append("WHERE ");
					first = false;
				} else {
					query.append("AND ");
				}
				query.append(v);
				query.append(" ");
			}
		}
		if(whereExtra != null) {
			for(String v : whereExtra) {
				if(first) {
					query.append("WHERE ");
					first = false;
				} else {
					query.append("AND ");
				}
				query.append(v);
				query.append(" ");
			}
		}
		
		if(tail != null) {
			query.append(tail);
		}
		
		return query.toString();
	}
}
