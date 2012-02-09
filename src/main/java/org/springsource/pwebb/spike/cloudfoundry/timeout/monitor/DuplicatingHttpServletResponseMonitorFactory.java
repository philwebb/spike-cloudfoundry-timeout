package org.springsource.pwebb.spike.cloudfoundry.timeout.monitor;

import java.util.Collection;

import javax.servlet.http.HttpServletResponse;

/**
 * {@link HttpServletResponseMonitorFactory} that can be used to create a {@link HttpServletResponseMonitor} that
 * duplicates all calls to another {@link HttpServletResponse} instance.
 * 
 * @author Phillip Webb
 */
public class DuplicatingHttpServletResponseMonitorFactory extends
		BridgedHttpServletResponseMonitorFactory<HttpServletResponseMonitor> {

	@Override
	protected Collection<org.springsource.pwebb.spike.cloudfoundry.timeout.monitor.BridgedHttpServletResponseMonitorFactory.MethodHandler> getMethodHandlers() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	@Override
	protected org.springsource.pwebb.spike.cloudfoundry.timeout.monitor.BridgedHttpServletResponseMonitorFactory.HttpServletResponseBridge newResponseBridge() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

}
