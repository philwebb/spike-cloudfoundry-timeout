package org.springsource.pwebb.spike.cloudfoundry.timeout.monitor;

/**
 * {@link HttpServletResponseMonitorFactory} that can be used to create a {@link ReplayableHttpServletResponseMonitor}
 * instance.
 * 
 * @author Phillip Webb
 */
public class ReplayableHttpServletResponseMonitorFactory implements
		HttpServletResponseMonitorFactory<ReplayableHttpServletResponseMonitor> {

	public ReplayableHttpServletResponseMonitor getMonitor() {
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

}
