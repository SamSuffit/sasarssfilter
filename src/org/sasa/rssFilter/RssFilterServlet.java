package org.sasa.rssFilter;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.datanucleus.util.StringUtils;

import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.SyndFeedOutput;
import com.sun.syndication.io.XmlReader;


public class RssFilterServlet extends HttpServlet {
	
	 private static final String FILTER_SEPARATOR = ",";

	private static final Logger logger = Logger.getLogger(RssFilterServlet.class.getName());
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -727511543571334356L;

	public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws IOException {
		
		String feedUrl = request.getParameter("feedUrl");
//		System.out.println(feedUrl);
		if(StringUtils.isEmpty(feedUrl) || StringUtils.isWhitespace(feedUrl))  {
			response.sendError(403,"No feedUrl paramter provided");
			return;
		}
		
		try {

			//Download the feed
			SyndFeed feed = downloadFeed(feedUrl);
			
			//Filter entry
			filterEntries(feed,request.getParameterValues("filter"));
			
			//Generate Output
			ServletOutputStream outputStream = response.getOutputStream();
			response.setCharacterEncoding(feed.getEncoding());
			response.setContentType("application/xml");
			SyndFeedOutput output = new SyndFeedOutput();
			output.output(feed,new OutputStreamWriter(outputStream,"UTF-8"));
		} catch (Exception e) {
			logger.severe("Error while generating output Feed");
			e.printStackTrace();
			response.sendError(403,e.getLocalizedMessage());
		}
		
	}


	private void filterEntries(SyndFeed feed, String[] filters) {
		
		//Nothing to do
		if(filters == null) {
			return;
		}
		
		HashMap<String,String> filtersMap = new HashMap<String, String>(filters.length);
		for(String s : filters) {
			if(s != null && s.contains(FILTER_SEPARATOR) ) {
				String[] splitRule = s.split(FILTER_SEPARATOR);
				if(splitRule != null && splitRule.length == 2) {
					filtersMap.put(splitRule[0],splitRule[1]);
				}
			}
		}
		@SuppressWarnings("unchecked")
		List<SyndEntry> entries = feed.getEntries();
		List<SyndEntry> s = new ArrayList<SyndEntry>();
		for(SyndEntry e : entries) {
			boolean addEntry = true;
			//Flemme de faire ça en relfexion
			if(filtersMap.get("title") != null) {
				if(!e.getTitle().contains(filtersMap.get("title"))) {
					addEntry = false;
				}
			}
			if(addEntry) {
				s.add(e);
			}
		}
		feed.setEntries(s);
	}
	
	
	/**
	 * @param feeds
	 * @return downloaded entries
	 */
	SyndFeed downloadFeed(String feedUrl) throws Exception {
		//Download 
    	logger.fine("Feetching feed [" + feedUrl + "]");
		SyndFeedInput input = new SyndFeedInput();
		URL feedURL = new URL(feedUrl);
        SyndFeed feed = input.build(new XmlReader(feedURL));
        return feed;
	}
	
}
