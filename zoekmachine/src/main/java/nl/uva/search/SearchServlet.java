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
import java.util.Map;

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
		if(req.getParameter("adv") == null
				|| !req.getParameter("adv").equals("true")) {
			resp.sendError(HttpServletResponse.SC_FORBIDDEN);
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
		
		Statement stm = null;
		ResultSet res = null;
		try {
			stm = conn.createStatement();
			res =
				stm.executeQuery("SELECT MAX(entering_date) AS emax, "
						+ "MIN(entering_date) AS emin, "
						+ "MAX(answering_date) AS amax, "
						+ "MIN(answering_date) AS amin "
						+ "FROM documents");
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
	
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		// Get a map of the request parameters
		@SuppressWarnings("unchecked")
		Map<String, String[]> parameters = req.getParameterMap();
		
		if(parameters.containsKey("simple_query")) {
			boolean simple = req.getParameter("simple_query").equals("true");
			
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
			String order = "";
			if(simple) {
				String input = req.getParameter("query");
				query =
					"SELECT *, MATCH (title, contents, category, questions, answers, answerers, answerers_ministry, keywords, questioners, questioners_party, doc_id) AGAINST ('"
							+ input
							+ "') as Score FROM documents WHERE MATCH (title, contents, category, questions, answers, answerers, answerers_ministry, keywords, questioners, questioners_party, doc_id) AGAINST ('"
							+ input + "') ORDER BY Score DESC";
				count_query = "SELECT COUNT(*) as results FROM documents WHERE MATCH (title, contents, category, questions, answers, answerers, answerers_ministry, keywords, questioners, questioners_party, doc_id) AGAINST ('"
							+ input
							+ "')";
			} else {
				String match_against = "";
				if(req.getParameter("questions").length() != 0
						&& req.getParameter("answers").length() != 0) {
					match_against =
						",(MATCH questions AGAINST ('%"
								+ req.getParameter("questions")
								+ "%') + MATCH answers AGAINST ('%"
								+ req.getParameter("answers")
								+ "%')) AS score ";
					order = "ORDER BY score DESC";
				}
				else if(req.getParameter("questions").length() != 0) {
					match_against =
						",(MATCH questions AGAINST ('%"
								+ req.getParameter("questions")
								+ "%')) AS score ";
					order = "ORDER BY score DESC";
				}
				else if(req.getParameter("answers").length() != 0) {
					match_against =
						",(MATCH answers AGAINST ('%"
								+ req.getParameter("answers")
								+ "%')) AS score ";
					order = "ORDER BY score DESC";
				}
				
				query = "SELECT *" + match_against + "FROM documents ";
				boolean where = false;
				
				if(req.getParameter("doc_id").length() != 0) {
					query +=
						"WHERE doc_id LIKE '%" + req.getParameter("doc_id")
								+ "%' ";
					where = true;
				}
				if(req.getParameter("title").length() != 0) {
					if(!where) {
						query +=
							"WHERE title LIKE '%" + req.getParameter("title")
									+ "%' ";
						where = true;
					}
					else
						query +=
							"AND title LIKE '%" + req.getParameter("title")
									+ "%' ";
				}
				if(req.getParameter("category").length() != 0) {
					if(!where) {
						query +=
							"WHERE category LIKE '%"
									+ req.getParameter("category")
									+ "%' ";
						where = true;
					}
					else
						query +=
							"AND category LIKE '%"
									+ req.getParameter("category")
									+ "%' ";
				}
				if(req.getParameter("questions").length() != 0) {
					if(!where) {
						query +=
							"WHERE MATCH questions AGAINST ('%"
									+ req.getParameter("questions") + "%') ";
						where = true;
					}
					else {
						query +=
							"AND MATCH questions AGAINST ('%"
									+ req.getParameter("questions")
									+ "%') ";
					}
				}
				if(req.getParameter("answers").length() != 0) {
					if(!where) {
						query +=
							"WHERE MATCH answers AGAINST ('%"
									+ req.getParameter("answers")
									+ "%') ";
						where = true;
					}
					else
						query +=
							"AND MATCH answers AGAINST ('%"
									+ req.getParameter("answers")
									+ "%') ";
				}
				if(req.getParameter("answerers").length() != 0) {
					if(!where) {
						query +=
							"WHERE answerers LIKE '%"
									+ req.getParameter("answerers") + "%' ";
						where = true;
					}
					else
						query +=
							"AND answerers LIKE '%"
									+ req.getParameter("answerers")
									+ "%' ";
				}
				if(req.getParameter("keywords").length() != 0) {
					if(!where) {
						query +=
							"WHERE keywords LIKE '%"
									+ req.getParameter("keywords")
									+ "%' ";
						where = true;
					}
					else
						query +=
							"AND keywords LIKE '%"
									+ req.getParameter("keywords")
									+ "%' ";
				}
				if(req.getParameter("questioners").length() != 0) {
					if(!where) {
						query +=
							"WHERE questioners LIKE '%"
									+ req.getParameter("questioners") + "%' ";
						where = true;
					}
					else
						query +=
							"AND questioners LIKE '%"
									+ req.getParameter("questioners") + "%' ";
				}
				if(req.getParameter("questioners_party").length() != 0) {
					if(!where) {
						query +=
							"WHERE questioners_party LIKE '%"
									+ req.getParameter("questioners_party")
									+ "%' ";
						where = true;
					}
					else
						query +=
							"AND questioners_party LIKE '%"
									+ req.getParameter("questioners_party")
									+ "%' ";
				}
				if(req.getParameter("answerers_ministry").length() != 0) {
					if(!where) {
						query +=
							"WHERE answerers_ministry LIKE '%"
									+ req.getParameter("answerers_ministry")
									+ "%' ";
						where = true;
					}
					else
						query +=
							"AND answerers_ministry LIKE '%"
									+ req.getParameter("answerers_ministry")
									+ "%' ";
				}
				
				// Modify query with entering_date and answering_date
				if(parameters.containsKey("entering_max")
						&& parameters.containsKey("entering_min")
						&& parameters.containsKey("answering_max")
						&& parameters.containsKey("answering_min")) {
					// TODO: See if parameter is actually a date
					
					if(!where) {
						query += "WHERE";
					} else {
						query += "AND";
					}
					query +=
						" entering_date BETWEEN '"
								+ req.getParameter("entering_min")
								+ "' AND '"
								+ req.getParameter("entering_max") + "'"
								+ " AND answering_date BETWEEN '"
								+ req.getParameter("answering_min")
								+ "' AND '"
								+ req.getParameter("answering_max") + "' ";
				}
				String[] parts = query.split("WHERE");
				String query_substring = parts[parts.length - 1];
				count_query = "SELECT COUNT(*) as results FROM documents WHERE " + query_substring;
			}

			query += order + " LIMIT 50";
			
			
			Statement stm = null;
			ResultSet res = null;
			Statement stm_count = null;
			ResultSet res_count = null;
			try {
				stm = conn.createStatement();
				res = stm.executeQuery(query);
				stm_count = conn.createStatement();
				res_count = stm_count.executeQuery(count_query);
				res_count.next();

				
				
				if(res_count.getInt(1) == 0) {
					out.print("<center>Your search did not match any documents</center");
					return;
				}
				
				out.print("<table width='600' align='center' style='margin: 0px auto;'>");
				out.print("<tr>");
				out.print("<td>");
				out.print("<font size='2' color='grey'>" + res_count.getInt(1) + " results found</font>");
				out.print("</td>");
				out.print("</tr>");
				int j = 0;
				while(res.next()) {
					j++;
					out.print("<tr>");
					out.print("<td>");
					out.print("<a href=''>" + res.getString(3) + "</a>");
					out.print("</td>");
					out.print("</tr>");
					
					out.print("<tr>");
					out.print("<td>");
					out.print("<font size='2'> By: " + res.getString(10) + " (" + res.getString(11) + ") on " + res.getString(13) + "</font>");
					out.print("</td>");
					out.print("</tr>");
					
					out.print("<tr>");
					out.print("<td>");
					out.print("<br>");
					out.print("</td>");
					out.print("</tr>");
					/*out.print("<tr>");
					
					out.print("<td>");
					out.print(j);
					out.print("</td>");
					
					// Doc ID
					out.print("<td>");
					out.print(res.getString(2));
					out.print("</td>");
					
					// Title
					out.print("<td>");
					out.print(res.getString(3));
					out.print("</td>");
					
					// Date of issue
					out.print("<td>");
					out.print(res.getString(13));
					out.print("</td>");
					
					// Date of response
					out.print("<td>");
					out.print(res.getString(14));
					out.print("</td>");
					
					// Issuer
					out.print("<td>");
					out.print(res.getString(10));
					out.print("</td>");
					
					// Issuerś Party
					out.print("<td>");
					out.print(res.getString(11));
					out.print("</td>");
					
					// Score
					if(res.getMetaData().getColumnCount() >= 15) {
						out.print("<td>");
						out.print(res.getString(15));
						out.print("</td>");
					} else {
						out.print("<td>0</td>");
					}
					
					out.print("</tr>");*/
				}
				out.print("</table>");
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
}
