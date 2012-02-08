package org.springsource.pwebb.spike.cloudfoundry.timeout.monitor;

/**
 * Factory used to create {@link HttpServletResponseMonitor}s.
 * 
 * @see HttpServletResponseMonitor
 * @see MonitoredHttpServletResponseWrapper
 * 
 * @param <T> The actual {@link HttpServletResponseMonitor} type created by the factory
 * @author Phillip Webb
 */
public interface HttpServletResponseMonitorFactory<T extends HttpServletResponseMonitor> {

	/**
	 * Return a new {@link HttpServletResponseMonitor} instance or <tt>null</tt> if monitoring is not required.
	 * @return A {@link HttpServletResponseMonitor}.
	 */
	T getMonitor();

}
