/**
 * File: SearchServlet.java
 * 
 */
package nl.uva.search;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.xy.StandardXYBarPainter;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.data.time.Month;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

/**
 * @author Ruben Janssen
 * @author Tom Peerdeman
 * 
 */
public class SearchServlet extends HttpServlet {
	private static final long serialVersionUID = -3460489316807949031L;
	
	private DataSource db;
	
	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		
		// Open the database connection pool, configured in contex.xml
		Context c;
		try {
			c = new InitialContext();
			db = (DataSource) c.lookup("java:comp/env/jdbc/database");
		} catch(NamingException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		if(req.getPathInfo() != null && req.getPathInfo().startsWith("/chart")) {
			generateChart(req, resp);
			return;
		} else if(req.getParameter("adv") == null
				|| !req.getParameter("adv").equals("true")) {
			// Invalid path, redirect to home
			resp.sendRedirect("/");
			return;
		}
		
		Connection conn = null;
		try {
			conn = db.getConnection();
		} catch(SQLException e) {
			resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			e.printStackTrace();
			throw new ServletException(e);
		}
		
		// Fetch the bounds for the entering_date and answering_date slider
		Statement stm = null;
		ResultSet res = null;
		try {
			stm = conn.createStatement();
			res = stm.executeQuery("SELECT MAX(entering_date) AS emax, "
					+ "MIN(entering_date) AS emin, "
					+ "MAX(answering_date) AS amax, "
					+ "MIN(answering_date) AS amin " + "FROM documents");
			while(res.next()) {
				for(int i = 0; i < res.getMetaData().getColumnCount(); i++) {
					req.setAttribute(res.getMetaData().getColumnName(i + 1),
							res.getString(i + 1));
				}
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
		
		try {
			conn.close();
		} catch(SQLException e) {
			e.printStackTrace();
		}
		
		RequestDispatcher view = req.getRequestDispatcher("/advanced.jsp");
		view.forward(req, resp);
	}
	
	private boolean isSimple(Map<String, String[]> parameters) {
		if(parameters.containsKey("simple_query")) {
			String[] arr = parameters.get("simple_query");
			return(arr != null && arr.length > 0 && arr[0] != null && arr[0]
																			.equals("true"));
		}
		return false;
	}
	
	/**
	 * @param req
	 * @param resp
	 * @throws ServletException
	 * @throws IOException
	 */
	private void generateChart(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		Connection conn = null;
		try {
			conn = db.getConnection();
		} catch(SQLException e) {
			resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			e.printStackTrace();
			throw new ServletException(e);
		}
		
		TimeSeries series = new TimeSeries("Issue count");
		
		// Get a map of the request parameters
		@SuppressWarnings("unchecked")
		Map<String, String[]> parameters = req.getParameterMap();
		
		String query = null;
		if(isSimple(parameters)) {
			String input = req.getParameter("query");
			query =
				"SELECT COUNT(*), YEAR(entering_date) AS year, MONTH(entering_date) AS month "
						+ "FROM documents "
						+ "WHERE MATCH (title, contents, category, questions, answers, answerers, answerers_ministry, keywords, questioners, questioners_party, doc_id)"
						+ " AGAINST ('"
						+ input
						+ "') AND entering_date IS NOT NULL "
						+ "GROUP BY year, month " + "ORDER BY year, month";
		} else {
			QueryGenerator gen = new QueryGenerator(parameters);
			query =
				gen.generate(
						"SELECT COUNT(*), YEAR(entering_date) AS year, MONTH(entering_date) AS month "
								+ "FROM documents",
						new String[] {"entering_date IS NOT NULL"},
						"GROUP BY year, month ORDER BY year, month");
		}
		
		Statement stm = null;
		ResultSet res = null;
		try {
			stm = conn.createStatement();
			res = stm.executeQuery(query);
			while(res.next()) {
				series.add(new Month(res.getInt(3), res.getInt(2)),
						res.getInt(1));
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
			
			try {
				conn.close();
			} catch(SQLException e) {
				e.printStackTrace();
			}
		}
		
		TimeSeriesCollection timeColl = new TimeSeriesCollection();
		timeColl.addSeries(series);
		
		JFreeChart chart = ChartFactory.createXYBarChart("", "Issue date",
				true, "# of documents", timeColl, PlotOrientation.VERTICAL,
				false, false, false);
		
		XYBarRenderer renderer = (XYBarRenderer) chart.getXYPlot()
														.getRenderer();
		renderer.setShadowVisible(false);
		renderer.setDrawBarOutline(false);
		renderer.setBarPainter(new StandardXYBarPainter());
		
		resp.setContentType("image/png");
		ChartUtilities.writeChartAsPNG(resp.getOutputStream(), chart, 500, 300);
	}
	
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		// Get a map of the request parameters
		@SuppressWarnings("unchecked")
		Map<String, String[]> parameters = req.getParameterMap();
		
		// Convert POST parameters to GET parameters for timeline call
		String getQuery = "?";
		for(Entry<String, String[]> e : parameters.entrySet()) {
			if(e.getValue() != null && e.getValue().length > 0
					&& e.getValue()[0].length() > 0) {
				getQuery += e.getKey() + "=" + e.getValue()[0] + "&";
			}
		}
		if(getQuery.length() > 1) {
			getQuery = getQuery.substring(0, getQuery.length() - 1);
		}
		
		// Open db connection
		Connection conn = null;
		try {
			conn = db.getConnection();
		} catch(SQLException e) {
			resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			e.printStackTrace();
			throw new ServletException(e);
		}
		
		resp.setContentType("text/html");
		PrintWriter out = resp.getWriter();
		
		String query = null;
		String count_query = null;
		String facet_query = null;
		if(isSimple(parameters)) {
			String input = req.getParameter("query");
			query =
				"SELECT *, MATCH (title, contents, category, questions, answers, answerers, answerers_ministry, keywords, questioners, questioners_party, doc_id) AGAINST ('"
						+ input
						+ "') as Score FROM documents WHERE MATCH (title, contents, category, questions, answers, answerers, answerers_ministry, keywords, questioners, questioners_party, doc_id) AGAINST ('"
						+ input + "') ORDER BY Score DESC";
			count_query =
				"SELECT COUNT(*) as results FROM documents WHERE MATCH (title, contents, category, questions, answers, answerers, answerers_ministry, keywords, questioners, questioners_party, doc_id) AGAINST ('"
						+ input + "')";
			
			facet_query =
				"SELECT questioners_party, COUNT(*) FROM documents WHERE MATCH (title, contents, category, questions, answers, answerers, answerers_ministry, keywords, questioners, questioners_party, doc_id) AGAINST ('"
						+ input + "') GROUP BY questioners_party";
		} else {
			String match_against = null;
			if(req.getParameter("questions").length() != 0
					&& req.getParameter("answers").length() != 0) {
				match_against = ",(MATCH questions AGAINST ('%"
						+ req.getParameter("questions")
						+ "%') + MATCH answers AGAINST ('%"
						+ req.getParameter("answers") + "%')) AS score ";
			} else if(req.getParameter("questions").length() != 0) {
				match_against = ",(MATCH questions AGAINST ('%"
						+ req.getParameter("questions") + "%')) AS score ";
			} else if(req.getParameter("answers").length() != 0) {
				match_against = ",(MATCH answers AGAINST ('%"
						+ req.getParameter("answers") + "%')) AS score ";
			}
			
			QueryGenerator gen = new QueryGenerator(parameters);
			
			if(match_against != null) {
				query = gen.generate("SELECT *" + match_against
						+ "FROM documents ", null, "ORDER BY score DESC");
			} else {
				query = gen.generate("SELECT * FROM documents ", null, null);
			}
			
			String[] parts = query.split("WHERE");
			String query_substring = parts[parts.length - 1];
			count_query = "SELECT COUNT(*) as results FROM documents WHERE "
					+ query_substring;
			
			count_query = gen.generate(
					"SELECT COUNT(*) as results FROM documents", null, null);
			
			facet_query = gen.generate(
					"SELECT questioners_party, COUNT(*) FROM documents", null,
					"GROUP BY questioners_party");
		}
		query += " LIMIT 10";
		
		Statement stm = null;
		ResultSet res = null;
		Statement stm_count = null;
		ResultSet res_count = null;
		Statement stm_facet = null;
		ResultSet res_facet = null;
		
		try {
			stm = conn.createStatement();
			res = stm.executeQuery(query);
			
			stm_count = conn.createStatement();
			res_count = stm_count.executeQuery(count_query);
			res_count.next();
			
			if(res_count.getInt(1) == 0 && isSimple(parameters)) {
				out.print("<center>Your search did not match any documents. <br>Please check your spelling and note that at least 4 characters have to be used as input.</center");
				return;
			}
			
			if(res_count.getInt(1) == 0 && !isSimple(parameters)) {
				out.print("<center>Your search did not match any documents. <br>Please check your spelling and note that at least 4 characters are required for the questions and answers input.</center");
				return;
			}
			
			out.print("<div id='results'>");
			// Amount of Results
			out.print("<table width='450'>");
			out.print("<tr>");
			out.print("<td>");
			out.print("<font size='2' color='grey'>" + res_count.getInt(1)
					+ " results found</font>");
			out.print("</td>");
			out.print("</tr>");
			
			while(res.next()) {
				out.print("<tr>");
				out.print("<td>");
				out.print("<a href='http://polidocs.nl/XML/KVR/"
						+ res.getString(2) + ".xml' target='_blank'>"
						+ res.getString(3) + "</a>");
				out.print("</td>");
				out.print("</tr>");
				
				out.print("<tr>");
				out.print("<td>");
				out.print("<cite><font size='2'><font color='green'>Document: " + res.getString(2)
						+ "</font><br />By: " + res.getString(10) + " ("
						+ res.getString(11) + ") on " + res.getString(13)
						+ "</font></cite>");
				out.print("</td>");
				out.print("</tr>");
				
				out.print("<tr>");
				out.print("<td>");
				out.print("<a href='#' class='wordcloud'><font size='2'>Word Cloud</font></a>");
				out.print("</td>");
				out.print("</tr>");
				
				out.print("<tr>");
				out.print("<td>");
				out.print("<br>");
				out.print("</td>");
				out.print("</tr>");
			}
			out.print("</table>");
			out.print("</div>");
			
			out.print("<div id='sidebar'>");
			// Timeline
			out.print("<table width='400'>");
			out.print("<tr><td><img src=\"search/chart/" + getQuery
					+ "\"></td></tr>");
			out.print("</table>");
			
			// Party Facet Table
			if (facet_query != null && !isSimple(parameters)) {
				stm_facet = conn.createStatement();
				res_facet = stm_facet.executeQuery(facet_query);
				Map<String, Integer> hashmap = new HashMap<String, Integer>();
				while(res_facet.next()) {
					String string = res_facet.getString(1);
					String[] parts = string.split(", ");
					
					for(int i = 0; i < parts.length; i++) {
						// TODO Adjust values to match the rest (C DA --> CDA)
						// TODO Handle words like beiden CDA
						
						// Very inefficient way to see if this part has already
						// occurred.
						boolean known = false;
						for(int j = 0; j < i; j++) {
							if(parts[i].equals(parts[j])) {
								known = true;
							}
						}
						
						if(known) {
							continue;
						}
						
						Integer key = hashmap.get(parts[i]);
						if(key == null)
							hashmap.put(parts[i], res_facet.getInt(2));
						else
							hashmap.put(parts[i], key + res_facet.getInt(2));
					}
				}
				
				// Sort the map by it's values
				TreeMap<String, Integer> treeMap =
					new TreeMap<String, Integer>(
							new ValueComparator(hashmap));
				treeMap.putAll(hashmap);
				
				out.print("<br><table width='300' align='center'>");
				out.print("<center><font size='4'>Party Facets: click to filter</font></center>");
				out.print("<tr><td>Party</td><td>Count</td></tr>");
				for(Map.Entry<String, Integer> entry : treeMap.entrySet()) {
					out.print("<tr>");
					out.print("<td>");
					out.print("<a href='#' onclick='applyPartyFilter(\""
							+ entry.getKey() + "\");'>" + entry.getKey()
							+ "</a>");
					out.print("</td>");
					
					out.print("<td>");
					out.print(entry.getValue());
					out.print("</td>");
					out.print("</tr>");
				}
				out.print("<tr><td><br></td></tr>");
				out.print("</table>");
			}
			out.print("</div>");
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
			
			// Close stm_count
			if(stm_count != null) {
				try {
					stm_count.close();
				} catch(SQLException e) {
					e.printStackTrace();
				}
			}
			
			// Close res_count
			if(res_count != null) {
				try {
					res_count.close();
				} catch(SQLException e) {
					e.printStackTrace();
				}
			}
			
			// Close stm_count
			if(stm_facet != null) {
				try {
					stm_facet.close();
				} catch(SQLException e) {
					e.printStackTrace();
				}
			}
			
			// Close res_count
			if(res_facet != null) {
				try {
					res_facet.close();
				} catch(SQLException e) {
					e.printStackTrace();
				}
			}
			
			try {
				// Close db connection
				conn.close();
			} catch(SQLException e) {
				e.printStackTrace();
			}
		}
		
		out.flush();
	}
}
