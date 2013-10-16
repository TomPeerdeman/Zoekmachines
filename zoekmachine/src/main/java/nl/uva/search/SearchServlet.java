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
import java.util.Enumeration;
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
		Connection conn = null;
		resp.setContentType("text/html");
		PrintWriter out = resp.getWriter();
		//out.println(req.getPathInfo());
		
		try {
			conn = db.getConnection();
		} catch(SQLException e) {
			resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			e.printStackTrace();
			throw new ServletException(e);
		}
		
		out.print(conn);
		
		Statement stm = null;
		ResultSet res = null;
		try {
			stm = conn.createStatement();
			res =
				stm.executeQuery("SELECT * FROM documents WHERE doc_id='ABC'");
			while(res.next()) {
				for(int i = 0; i < res.getMetaData().getColumnCount(); i++) {
					out.print("<br />\n" + res.getString(i + 1));
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
		
		out.flush();
	}
	
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) 
			throws ServletException, IOException {
		
	    String forward="";
	    // Get a map of the request parameters
	    @SuppressWarnings("unchecked")
	    Map parameters = req.getParameterMap();
	    if (parameters.containsKey("query"))
	      forward = RESULTS_JSP;
	    RequestDispatcher view = req.getRequestDispatcher(forward);
	    // uncomment to be forwarded to results page
	    //view.forward(req, resp);
	    PrintWriter out = resp.getWriter();
		String query = req.getParameter("query");
		out.print(query);
	}
}
