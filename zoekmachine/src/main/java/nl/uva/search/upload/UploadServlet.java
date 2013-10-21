/**
 * File: UploadServlet.java
 * 
 */
package nl.uva.search.upload;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.xml.sax.SAXException;

/**
 * @author Ruben Janssen
 * @author Tom Peerdeman
 * 
 */
public class UploadServlet extends HttpServlet {
	private static final long serialVersionUID = -8629969900458356732L;
	
	private ServletFileUpload upload;
	private DataSource db;
	private StopWordList stopWordList;
	
	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		// Allow this servlet to receive uploaded files
		File tmp = (File) config.getServletContext()
								.getAttribute("javax.servlet.context.tempdir");
		DiskFileItemFactory fac = new DiskFileItemFactory();
		fac.setRepository(tmp);
		upload = new ServletFileUpload(fac);
		
		// Open the database connection pool, configured in contex.xml
		Context c;
		try {
			c = new InitialContext();
			db = (DataSource) c.lookup("java:comp/env/jdbc/database");
		} catch(NamingException e) {
			e.printStackTrace();
		}
		
		try {
			stopWordList = new StopWordList(config.getServletContext());
		} catch(IOException e) {
			throw new ServletException(e);
		}
	}
	
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		if(!ServletFileUpload.isMultipartContent(req)) {
			resp.sendError(HttpServletResponse.SC_NOT_ACCEPTABLE);
			return;
		}
		
		Connection conn = null;
		try {
			conn = db.getConnection();
			
			// Get the list of uploaded files
			List<FileItem> files = upload.parseRequest(req);
			
			// Create a new XML parser
			SAXParserFactory factory = SAXParserFactory.newInstance();
			SAXParser saxParser = factory.newSAXParser();
			for(FileItem file : files) {
				if(!file.isFormField()) {
					try {
						// Parse the uploaded XML file
						saxParser.parse(file.getInputStream(),
								new UploadXMLParser(conn, stopWordList));
					} catch(SAXException e) {
						throw new ServletException(e);
					}
				}
			}
		} catch(Exception e1) {
			resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			e1.printStackTrace();
			throw new ServletException(e1);
		} finally {
			if(conn != null) {
				try {
					conn.close();
				} catch(SQLException e) {
				}
			}
		}
	}
}
