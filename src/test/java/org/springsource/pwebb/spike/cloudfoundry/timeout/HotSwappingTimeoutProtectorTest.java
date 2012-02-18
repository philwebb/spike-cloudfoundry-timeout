package org.springsource.pwebb.spike.cloudfoundry.timeout;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.verify;

import javax.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springsource.pwebb.spike.cloudfoundry.timeout.HotSwappingTimeoutProtector.RequestCoordinator;
import org.springsource.pwebb.spike.cloudfoundry.timeout.HotSwappingTimeoutProtector.RequestCoordinators;
import org.springsource.pwebb.spike.cloudfoundry.timeout.monitor.HttpServletResponseMonitor;
import org.springsource.pwebb.spike.cloudfoundry.timeout.monitor.HttpServletResponseMonitorFactory;

/**
 * Tests for {@link HotSwappingTimeoutProtector}.
 * 
 * @author Phillip Webb
 */
public class HotSwappingTimeoutProtectorTest {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private HotSwappingTimeoutProtector protector = new HotSwappingTimeoutProtector();

	@Mock
	private TimeoutProtectionHttpRequest request;

	@Mock
	private HttpServletResponse response;

	@Mock
	private RequestCoordinators requestCoordinators;

	@Mock
	private RequestCoordinator requestCoordinator;

	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
		this.protector.setRequestCoordinators(this.requestCoordinators);
		given(this.requestCoordinators.get(this.request)).willReturn(this.requestCoordinator);
	}

	@Test
	public void shouldNoUseMonitorIfUnderTimeout() throws Exception {
		this.protector.setPollThreshold(100);
		HttpServletResponseMonitorFactory monitorFactory = this.protector.getMonitorFactory(this.request);
		HttpServletResponseMonitor monitor = monitorFactory.getMonitor();
		assertThat(monitor, is(nullValue()));
	}

	@Test
	public void shouldMonitorIfOverTimeout() throws Exception {
		this.protector.setPollThreshold(100);
		given(this.requestCoordinator.consumePollResponse()).willReturn(this.response);
		HttpServletResponseMonitorFactory monitorFactory = this.protector.getMonitorFactory(this.request);
		Thread.sleep(150);
		HttpServletResponseMonitor monitor = monitorFactory.getMonitor();
		assertThat(monitor, is(not(nullValue())));
	}

	@Test
	public void shouldConsumePollResponseIfAlreadyAvailble() throws Exception {
		this.protector.setPollThreshold(0);
		given(this.requestCoordinator.consumePollResponse()).willReturn(this.response);
		HttpServletResponseMonitorFactory monitorFactory = this.protector.getMonitorFactory(this.request);
		HttpServletResponseMonitor monitor = monitorFactory.getMonitor();
		assertThat(monitor, is(not(nullValue())));
	}

	@Test
	public void shouldAwaitPollResponse() throws Exception {
		this.protector.setPollThreshold(0);
		given(this.requestCoordinator.consumePollResponse()).willReturn(null, this.response);
		HttpServletResponseMonitorFactory monitorFactory = this.protector.getMonitorFactory(this.request);
		HttpServletResponseMonitor monitor = monitorFactory.getMonitor();
		assertThat(monitor, is(not(nullValue())));
		verify(this.requestCoordinator).awaitPollResponse(anyLong());
	}

	@Test
	public void shouldTimeoutAwaitingPollResponse() throws Exception {
		this.protector.setPollThreshold(0);
		given(this.requestCoordinator.consumePollResponse()).willReturn(null);
		willThrow(new InterruptedException()).given(this.requestCoordinator).awaitPollResponse(anyLong());
		HttpServletResponseMonitorFactory monitorFactory = this.protector.getMonitorFactory(this.request);
		this.thrown.expect(IllegalStateException.class);
		this.thrown.expectMessage("Timeout waiting for poll");
		monitorFactory.getMonitor();
	}

	// FIXME remaining tests

	@Test
	public void shouldCleanupUnconsumedPollResponse() throws Exception {

	}

	@Test
	public void shouldHandlePollResponseTimeout() throws Exception {

	}

	@Test
	public void shouldHandleConsumedPollResponse() throws Exception {

	}

}
