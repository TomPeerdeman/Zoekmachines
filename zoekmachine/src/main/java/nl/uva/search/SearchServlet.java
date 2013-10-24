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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
	
	private final static int RESULTS_PER_PAGE = 10;
	
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
		} else if(req.getPathInfo() != null
				&& req.getPathInfo().startsWith("/wordcloud")) {
			generateWordCloud(req, resp);
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
	
	/**
	 * @param req
	 * @param resp
	 * @throws IOException
	 * @throws ServletException
	 */
	private void generateWordCloud(HttpServletRequest req,
			HttpServletResponse resp) throws IOException, ServletException {
		String doc = req.getParameter("d");
		
		if(doc == null) {
			resp.sendError(HttpServletResponse.SC_NOT_ACCEPTABLE);
			return;
		}
		
		int docId = Integer.parseInt(doc);
		if(docId < 1) {
			resp.sendError(HttpServletResponse.SC_NOT_ACCEPTABLE);
			return;
		}
		
		resp.setContentType("text/html");
		PrintWriter out = resp.getWriter();
		
		Connection conn = null;
		try {
			conn = db.getConnection();
		} catch(SQLException e) {
			resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			e.printStackTrace();
			throw new ServletException(e);
		}
		
		out.print("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.1//EN\"\n" +
				"    \"http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd\">" +
				"<html xmlns=\"http://www.w3.org/1999/xhtml\">" +
				"<head>" +
				"<meta charset=\"utf-8\" />" +
				"<title>Elgoog KVR doc " + docId + "  wordcloud</title>"
				+ "</head>" +
				"<body>");
		
		try {
			out.print("<em><strong>Document wordcloud</strong></em>");
			new WordcloudGenerator(conn, "DOC", docId, out);
			out.print("<br /><br /><em><strong>Questions wordcloud</strong></em>");
			new WordcloudGenerator(conn, "QUESTION", docId, out);
			out.print("<br /><br /><em><strong>Answers wordcloud</strong></em>");
			new WordcloudGenerator(conn, "ANSWER", docId, out);
			out.print("</body></html>");
		} catch(Exception e1) {
			throw new ServletException(e1);
		} finally {
			try {
				conn.close();
			} catch(SQLException e) {
				e.printStackTrace();
			}
		}
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
			
			// Add *'s around the words for better boolean search
			String[] splits = input.split(" ");
			input = "";
			for(String split : splits) {
				if(split.length() > 3) {
					input += " *" + split + "*";
				}
			}
			input = input.trim();
			
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
			
			// Add *'s around the words for better boolean search
			String[] splits = input.split(" ");
			input = "";
			for(String split : splits) {
				if(split.length() > 3) {
					input += " *" + split + "*";
				}
			}
			input = input.trim();
			
			query =
				"SELECT *, MATCH (title, contents, category, questions, answers, answerers, answerers_ministry, keywords, questioners, questioners_party, doc_id) AGAINST ('"
						+ input
						+ "' IN BOOLEAN MODE) as Score FROM documents WHERE MATCH (title, contents, category, questions, answers, answerers, answerers_ministry, keywords, questioners, questioners_party, doc_id) AGAINST ('"
						+ input
						+ "' IN BOOLEAN MODE) ORDER BY Score DESC";
			count_query =
				"SELECT COUNT(*) as results FROM documents WHERE MATCH (title, contents, category, questions, answers, answerers, answerers_ministry, keywords, questioners, questioners_party, doc_id) AGAINST ('"
						+ input + "')";
			
			facet_query =
				"SELECT questioners_party, COUNT(*) FROM documents WHERE MATCH (title, contents, category, questions, answers, answerers, answerers_ministry, keywords, questioners, questioners_party, doc_id) AGAINST ('"
						+ input + "') GROUP BY questioners_party";
		} else {
			List<String> match = new ArrayList<String>(5);
			
			if(req.getParameter("questions") != null
					&& req.getParameter("questions").length() != 0) {
				match.add("MATCH (questions) AGAINST ('%"
						+ req.getParameter("questions") + "%')");
			}
			
			if(req.getParameter("answers") != null
					&& req.getParameter("answers").length() != 0) {
				match.add("MATCH (answers) AGAINST ('%"
						+ req.getParameter("answers") + "%')");
			}
			
			if(req.getParameter("keywords") != null
					&& req.getParameter("keywords").length() != 0) {
				match.add("MATCH (keywords) AGAINST ('%"
						+ req.getParameter("keywords") + "%')");
			}
			
			if(req.getParameter("questioners") != null
					&& req.getParameter("questioners").length() != 0) {
				match.add("MATCH (questioners) AGAINST ('%"
						+ req.getParameter("questioners") + "%')");
			}
			
			if(req.getParameter("answerers") != null
					&& req.getParameter("answerers").length() != 0) {
				match.add("MATCH (answerers) AGAINST ('%"
						+ req.getParameter("answerers") + "%')");
			}
			
			String match_against = null;
			if(match.size() > 0) {
				match_against = ", (";
				for(int i = 0; i < match.size(); i++) {
					match_against += "(" + match.get(i) + ")";
					if(i + 1 < match.size()) {
						match_against += " + ";
					}
				}
				match_against += ") AS Score ";
			}
			
			QueryGenerator gen = new QueryGenerator(parameters);
			
			if(match_against != null) {
				query = gen.generate("SELECT *" + match_against
						+ "FROM documents ", null, "ORDER BY score DESC");
			} else {
				query = gen.generate("SELECT * FROM documents ", null, null);
			}
			
			count_query = gen.generate(
					"SELECT COUNT(*) as results FROM documents", null, null);
			
			facet_query = gen.generate(
					"SELECT questioners_party, COUNT(*) FROM documents", null,
					"GROUP BY questioners_party");
		}
		
		System.out.println(query);
		System.out.println();
		
		int page;
		if(parameters.containsKey("page")) {
			page = Integer.parseInt(req.getParameter("page"));
		} else {
			page = 1;
		}
		
		if(page <= 0) {
			page = 1;
		}
		
		boolean showPrev = page > 1;
		boolean showNext = true;
		
		query +=
			" LIMIT " + ((page - 1) * RESULTS_PER_PAGE) + ", "
					+ RESULTS_PER_PAGE;
		
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
			
			int nResults = res_count.getInt(1);
			
			if(nResults == 0 && isSimple(parameters)) {
				out.print("<center>Your search did not match any documents. <br>Please check your spelling and note that at least 4 characters have to be used as input.</center");
				return;
			}
			
			if(nResults == 0 && !isSimple(parameters)) {
				out.print("<center>Your search did not match any documents. <br>Please check your spelling and note that at least 4 characters are required for the questions and answers input.</center");
				return;
			}
			
			if(nResults <= page * RESULTS_PER_PAGE) {
				showNext = false;
			}
			
			// Amount of Results
			out.print("<div id='main'>");
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
				out.print("<cite><font size='2'><font color='green'>Document: "
						+ res.getString(2)
						+ "</font><br />By: " + res.getString(10) + " ("
						+ res.getString(11) + ") on " + res.getString(13)
						+ "</font></cite>");
				out.print("</td>");
				out.print("</tr>");
				
				out.print("<tr>");
				out.print("<td>");
				out.print("<a href='/search/wordcloud?&d="
						+ res.getInt(1)
						+ "' target='_blank' class='hoverlink'>"
						+ "<font size='2'>Word Cloud</font></a></a>");
				out.print("</td>");
				out.print("</tr>");
				
				out.print("<tr>");
				out.print("<td>");
				out.print("<br>");
				out.print("</td>");
				out.print("</tr>");
			}
			
			out.print("<tr><td>");
			if(showPrev) {
				out.print("<a href='#' class='paging' onclick='setpage("
						+ (page - 1)
						+ ")'>&lt;&lt;</a> ");
			}
			out.print("Page ");
			out.print(page);
			out.print(" of ");
			out.print((int) Math.ceil((double) nResults
					/ (double) RESULTS_PER_PAGE));
			if(showNext) {
				out.print(" <a href='#' class='paging' onclick='setpage("
						+ (page + 1)
						+ ")'>&gt;&gt;</a>");
			}
			out.print("</td></tr>");
			out.print("</table>");
			out.print("</div>");
			
			out.print("<div id='sidebar'>");
			// Timeline
			out.print("<table width='400'>");
			out.print("<tr><td><img src=\"search/chart/" + getQuery
					+ "\"></td></tr>");
			out.print("</table>");
			
			// Party Facet Table
			if(facet_query != null && !isSimple(parameters)) {
				stm_facet = conn.createStatement();
				res_facet = stm_facet.executeQuery(facet_query);
				Map<String, Integer> hashmap = new HashMap<String, Integer>();
				while(res_facet.next()) {
					String string = res_facet.getString(1);
					String[] parts = string.split(", ");
					
					for(int i = 0; i < parts.length; i++) {
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
					out.print("<a href='#' class='hoverlink' onclick='applyPartyFilter(\""
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
