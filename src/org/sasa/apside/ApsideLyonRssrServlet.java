package org.sasa.apside;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.UrlEncodedContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.server.spi.IoUtil;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Transaction;


public class ApsideLyonRssrServlet extends HttpServlet {
	
	private static final long serialVersionUID = 9067454050546381255L;
	
	private static final Logger logger = Logger.getLogger(ApsideLyonRssrServlet.class.getName());
	
	public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws IOException {
		
		@SuppressWarnings("unchecked")
		Map<String,String> parameterMap = request.getParameterMap();
		if(parameterMap.size() == 0) {
			showFeed(response);
		}
		else if(StringUtils.equals("list",request.getParameter("action"))) {
			listDataStore(response);
		}
		else if(StringUtils.equals("add",request.getParameter("action"))) {
			addDataStore(request,response);
		}
		else {
			response.sendError(404);
		}
		
	}


	private void addDataStore(HttpServletRequest request, HttpServletResponse response) {
		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		Transaction txn = datastore.beginTransaction();
		try {
			Key key = KeyFactory.createKey("Apside","Apside");
			Entity apside =null;
			try  { 
				apside = datastore.get(key);
			}
			catch(EntityNotFoundException e) {}
			if(apside == null) {
				apside = new Entity(key);
			}
		    apside.setProperty(request.getParameter("name"),request.getParameter("value"));
		    datastore.put(apside);
		    txn.commit();
		} catch (Exception e) {
			logger.severe(e.getMessage());
		} finally {
		    if (txn.isActive()) {
		        txn.rollback();
		    }
		}
		listDataStore(response);		
	}

	private void listDataStore(HttpServletResponse response) {
		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		try {
			PrintWriter writer = response.getWriter();
			writer.append("data storage\n\n\n");
			Key key = KeyFactory.createKey("Apside","Apside");
			Entity entity = datastore.get(key);
			for(String s : entity.getProperties().keySet()) {
				writer.append(s).append(": ").append((String)entity.getProperties().get(s)).append("\n");
			}
		} catch (Exception e) {
			logger.severe(e.getMessage());
		} 
		
	}

	private void showFeed(HttpServletResponse response) throws IOException {
		try {
			
			HttpTransport httpTransport = new NetHttpTransport();
			HttpRequestFactory requestFactory = httpTransport.createRequestFactory();
			
			/*
			 * Get login page to parse it
			 */
			GenericUrl url = new GenericUrl("http://apsidelyon.free.fr/");
			HttpRequest _request = requestFactory.buildGetRequest(url);
			HttpResponse _resp = _request.execute();
			
			/*
			 * Parse login page to get all input from the login form
			 */
			ApsidePageParser app = new ApsidePageParser(_resp.getContent(),_resp.getContentEncoding());
//			for(Params p : app.getParams()) {
//				System.out.println(p.getName() + ":" + p.getValue());
//			}
			
			logToApsideSite(requestFactory, app);
			
			generateRssFeed(response, requestFactory);
			
			logger.info("Rss feed downloaded");
		} catch (Exception e) {
			logger.severe("Error while generating output Feed");
			e.printStackTrace();
			response.sendError(403,e.getLocalizedMessage());
		}
	}


	private void generateRssFeed(HttpServletResponse response,
			HttpRequestFactory requestFactory) throws IOException,
			UnsupportedEncodingException {
		HttpRequest feedRequest = requestFactory.buildGetRequest(new GenericUrl("http://apsidelyon.free.fr/index.php?format=feed&amp;type=rss"));
		HttpResponse feedResponse = feedRequest.execute();
		
		ServletOutputStream outputStream = response.getOutputStream();
		response.setCharacterEncoding(feedResponse.getContentEncoding());
		response.setContentType("application/xml");
		OutputStreamWriter osw = new OutputStreamWriter(outputStream,"UTF-8");
		osw.append(IoUtil.readStream(feedResponse.getContent()));
		osw.close();
	}


	private void logToApsideSite(HttpRequestFactory requestFactory,
			ApsidePageParser app) throws IOException {
		
		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		try {
			Key key = KeyFactory.createKey("Apside","Apside");
			Entity entity = datastore.get(key);
			GenericUrl loginUrl = new GenericUrl("http://apsidelyon.free.fr/index.php");
			Map<String, String> paramsMap = new HashMap<String, String>();
			paramsMap.put("username",(String)entity.getProperty("username"));
			paramsMap.put("passwd",(String)entity.getProperty("passwd"));
			for(Params p : app.getParams()) {
				paramsMap.put(p.getName() ,p.getValue());
			}
			HttpRequest loginRequest = requestFactory.buildPostRequest(loginUrl,new UrlEncodedContent(paramsMap));
			loginRequest.execute();
			
			
		} catch (Exception e) {
			logger.severe(e.getMessage());
		} 
		
		
	}
	
}
