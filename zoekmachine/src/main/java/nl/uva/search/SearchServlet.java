/**
 * File: SearchServlet.java
 * Author: Tom Peerdeman
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
 * @author Tom Peerdeman
 * 
 */
public class SearchServlet extends HttpServlet {
	private static final long serialVersionUID = -3460489316807949031L;
	private static String INDEX_JSP = "/index.jsp";
	private static String RESULTS_JSP = "/results.jsp";
	private static String ADVANCED_JSP = "/advanced.jsp";
	
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
		
		// TODO: Change to advanced.jsp
		RequestDispatcher view = req.getRequestDispatcher("/slider.jsp");
		view.forward(req, resp);
	}
	
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		
		String forward = "";
		// Get a map of the request parameters
		@SuppressWarnings("unchecked")
		Map<String, String[]> parameters = req.getParameterMap();
		if(parameters.containsKey("advanced")) {
			forward = ADVANCED_JSP;
			RequestDispatcher view = req.getRequestDispatcher(forward);
			view.forward(req, resp);
		}
		else if(parameters.containsKey("simple_query")) {
			Connection conn = null;
			resp.setContentType("text/html");
			PrintWriter out = resp.getWriter();
			String input = req.getParameter("query");
			
			// Modify query with entering_date and answering_date
			String andQuery = "";
			if(parameters.containsKey("entering_max")
					&& parameters.containsKey("entering_min")
					&& parameters.containsKey("answering_max")
					&& parameters.containsKey("answering_min")) {
				// TODO: See if parameter is actually a date
				andQuery =
					" AND entering_date BETWEEN '"
							+ req.getParameter("entering_min")
							+ "' AND '"
							+ req.getParameter("entering_max") + "'"
							+ " AND answering_date BETWEEN '"
							+ req.getParameter("answering_min")
							+ "' AND '"
							+ req.getParameter("answering_max") + "'";
			}
			
			String query =
				"SELECT *, MATCH (title, contents, category, questions, answers, answerers, answerers_ministry, keywords, questioners, questioners_party, doc_id) AGAINST ('"
						+ input
						+ "') as Score FROM documents WHERE MATCH (title, contents, category, questions, answers, answerers, answerers_ministry, keywords, questioners, questioners_party, doc_id) AGAINST ('"
						+ input + "')" + andQuery + " ORDER BY Score DESC";
			
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
				res = stm.executeQuery(query);
				out.print("<table border='1'>");
				out.print("<tr><td>#</td><td>Document Id</td><td>Title</td><td>Date of Issue</td><td>Date of Response</td><td>Issuer</td><td>Issuer's Party</td><td>Score</td></tr>");
				int j = 0;
				while(res.next()) {
					j++;
					out.print("<tr>");
					
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
					out.print("<td>");
					out.print(res.getString(15));
					out.print("</td>");
					
					out.print("</tr>");
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
			}
			
			try {
				conn.close();
			} catch(SQLException e) {
				e.printStackTrace();
			}
			
			out.flush();
		}
		else if(parameters.containsKey("advanced_query")) {
			Connection conn = null;
			resp.setContentType("text/html");
			PrintWriter out = resp.getWriter();
			
			String query = "SELECT * FROM documents ";
			int where = 0;
			
			if (req.getParameter("doc_id").length() != 0) {
				query += "WHERE doc_id LIKE '%" + req.getParameter("doc_id") + "%' ";
				where = 1;
			}
			if (req.getParameter("title").length() != 0) {
				if (where == 0) {
					query += "WHERE title LIKE '%" + req.getParameter("title") + "%' ";
					where = 1;
				}
				else
					query += "AND title LIKE '%" + req.getParameter("title") + "%' ";			
			}
			if (req.getParameter("category").length() != 0) {
				if (where == 0) {
					query += "WHERE category LIKE '%" + req.getParameter("category") + "%' ";
					where = 1;
				}
				else
					query += "AND category LIKE '%" + req.getParameter("category") + "%' ";			
			}
			if (req.getParameter("questions").length() != 0) {
				if (where == 0) {
					query += "WHERE questions LIKE '%" + req.getParameter("questions") + "%' ";
					where = 1;
				}
				else
					query += "AND questions LIKE '%" + req.getParameter("questions") + "%' ";			
			}
			if (req.getParameter("answers").length() != 0) {
				if (where == 0) {
					query += "WHERE answers LIKE '%" + req.getParameter("answers") + "%' ";
					where = 1;
				}
				else
					query += "AND answers LIKE '%" + req.getParameter("answers") + "%' ";			
			}
			if (req.getParameter("answerers").length() != 0) {
				if (where == 0) {
					query += "WHERE answerers LIKE '%" + req.getParameter("answerers") + "%' ";
					where = 1;
				}
				else
					query += "AND answerers LIKE '%" + req.getParameter("answerers") + "%' ";			
			}
			if (req.getParameter("keywords").length() != 0) {
				if (where == 0) {
					query += "WHERE keywords LIKE '%" + req.getParameter("keywords") + "%' ";
					where = 1;
				}
				else
					query += "AND keywords LIKE '%" + req.getParameter("keywords") + "%' ";			
			}
			if (req.getParameter("questioners").length() != 0) {
				if (where == 0) {
					query += "WHERE questioners LIKE '%" + req.getParameter("questioners") + "%' ";
					where = 1;
				}
				else
					query += "AND questioners LIKE '%" + req.getParameter("questioners") + "%' ";			
			}
			if (req.getParameter("questioners_party").length() != 0) {
				if (where == 0) {
					query += "WHERE questioners_party LIKE '%" + req.getParameter("questioners_party") + "%' ";
					where = 1;
				}
				else
					query += "AND questioners_party LIKE '%" + req.getParameter("questioners_party") + "%' ";			
			}
			if (req.getParameter("answerers_ministry").length() != 0) {
				if (where == 0) {
					query += "WHERE answerers_ministry LIKE '%" + req.getParameter("answerers_ministry") + "%' ";
					where = 1;
				}
				else
					query += "AND answerers_ministry LIKE '%" + req.getParameter("answerers_ministry") + "%' ";			
			}
			
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
				res = stm.executeQuery(query);
				out.print("<center><img border='0' src='elgoog.jpg' width='327' height='264'><form method='POST' action='search'><input type='hidden' name='simple_query' /><input type='text' name='query'/><input type='submit' value='Search' /></form><form method='POST' action='advanced'><input type='submit' name='advanced' value='Advanced Search' /></form></center>");
				out.print("<table border='1'>");
				out.print("<tr><td>#</td><td>Document Id</td><td>Title</td><td>Date of Issue</td><td>Date of Response</td><td>Issuer</td><td>Issuer's Party</td><td>Score</td></tr>");
				int j = 0;
				while(res.next()) {
					j++;
					out.print("<tr>");
					
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
					out.print("<td>");
					//out.print(res.getString(15));
					out.print("</td>");
					
					out.print("</tr>");
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
			}
			
			try {
				conn.close();
			} catch(SQLException e) {
				e.printStackTrace();
			}
			
			out.flush();
			
		}
	}
}
