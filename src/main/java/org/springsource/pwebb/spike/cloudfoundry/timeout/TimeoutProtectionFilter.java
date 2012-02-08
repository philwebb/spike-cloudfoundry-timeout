package org.springsource.pwebb.spike.cloudfoundry.timeout;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

import org.springframework.util.Assert;
import org.springsource.pwebb.spike.cloudfoundry.timeout.monitor.HttpServletResponseMonitorFactory;
import org.springsource.pwebb.spike.cloudfoundry.timeout.monitor.MonitoredHttpServletResponseWrapper;

/**
 * Servlet {@link Filter} that can be used to transparently protect against CloudFoundry gateway timeout errors.
 * 
 * @author Phillip Webb
 */
public class TimeoutProtectionFilter implements Filter {

	private TimeoutProtector protector;

	public void init(FilterConfig filterConfig) throws ServletException {
	}

	public void destroy() {
	}

	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
			ServletException {
		Assert.state(this.protector != null, "Please set the TimeoutProtector");
		TimeoutProtectionHttpRequest timeoutProtectionRequest = TimeoutProtectionHttpRequest.get(request);
		if (timeoutProtectionRequest == null) {
			chain.doFilter(request, response);
		} else {
			doFilter(timeoutProtectionRequest, (HttpServletResponse) response, chain);
		}
	}

	private void doFilter(TimeoutProtectionHttpRequest request, HttpServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		HttpServletResponseMonitorFactory monitor = this.protector.getMonitorFactory(request);
		try {
			MonitoredHttpServletResponseWrapper monitoredHttpResponse = new MonitoredHttpServletResponseWrapper(
					response, monitor);
			chain.doFilter(request.getServletRequest(), monitoredHttpResponse);
		} finally {
			this.protector.cleanup(request, monitor);
		}
	}

	public void setProtector(TimeoutProtector protector) {
		this.protector = protector;
	}
}
