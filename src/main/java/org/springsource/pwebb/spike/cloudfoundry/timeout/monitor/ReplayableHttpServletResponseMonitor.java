package org.springsource.pwebb.spike.cloudfoundry.timeout.monitor;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

/**
 * A {@link HttpServletResponseMonitor} that records all monitored evens such that they can be
 * {@link #replay(HttpServletResponse) replayed} to another {@link HttpServletResponse}.
 * 
 * @author Phillip Webb
 */
public interface ReplayableHttpServletResponseMonitor extends HttpServletResponseMonitor {

	/**
	 * Replay the all events monitored so far to the specified <tt>response</tt>.
	 * @param response the response used to replay the events
	 * @throws IOException
	 */
	void replay(HttpServletResponse response) throws IOException;

}
