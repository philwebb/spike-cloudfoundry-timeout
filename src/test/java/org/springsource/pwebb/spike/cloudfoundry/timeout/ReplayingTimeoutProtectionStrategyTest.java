package org.springsource.pwebb.spike.cloudfoundry.timeout;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springsource.pwebb.spike.cloudfoundry.timeout.monitor.HttpServletResponseMonitor;
import org.springsource.pwebb.spike.cloudfoundry.timeout.monitor.HttpServletResponseMonitorFactory;

/**
 * Tests for {@link ReplayingTimeoutProtectionStrategy}.
 * 
 * @author Phillip Webb
 */
public class ReplayingTimeoutProtectionStrategyTest {

	private static final long FAIL_TIMEOUT = 100;
	private static final long LONG_POLL_TIME = 200;
	private static final long THRESHOLD = 0;

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private ReplayingTimeoutProtectionStrategy strategy = new ReplayingTimeoutProtectionStrategy();

	@Mock
	private TimeoutProtectionHttpRequest request;

	@Mock
	private TimeoutProtectionHttpRequest secondRequest;

	@Mock
	private HttpServletResponse response;

	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
		this.strategy.setThreshold(THRESHOLD);
		this.strategy.setFailTimeout(FAIL_TIMEOUT);
		this.strategy.setLongPollTime(LONG_POLL_TIME);
		given(this.request.getUid()).willReturn("1");
		given(this.secondRequest.getUid()).willReturn("2");
	}

	@Test
	public void shouldNotUseMonitorIfUnderThreshold() throws Exception {
		this.strategy.setThreshold(100);
		HttpServletResponseMonitorFactory monitorFactory = this.strategy.handleRequest(this.request);
		HttpServletResponseMonitor monitor = monitorFactory.getMonitor();
		assertThat(monitor, is(nullValue()));
	}

	@Test
	public void shouldMonitorIfOverThreshold() throws Exception {
		this.strategy.setThreshold(100);
		HttpServletResponseMonitorFactory monitorFactory = this.strategy.handleRequest(this.request);
		Thread.sleep(150);
		HttpServletResponseMonitor monitor = monitorFactory.getMonitor();
		assertThat(monitor, is(not(nullValue())));
	}

	@Test
	public void shouldRecordAndReplayMonitored() throws Exception {
		HttpServletResponseMonitorFactory monitorFactory = this.strategy.handleRequest(this.request);
		HttpServletResponseMonitor monitor = monitorFactory.getMonitor();
		monitor.sendError(100);
		this.strategy.afterRequest(this.request, monitorFactory);
		this.strategy.handlePoll(this.request, this.response);
		verify(this.response).sendError(100);
	}

	@Test
	public void shouldPurgeUnpolledRequests() throws Exception {
		this.strategy.setFailTimeout(10);
		// 1st request is monitored but never polled
		HttpServletResponseMonitorFactory monitorFactory = this.strategy.handleRequest(this.request);
		monitorFactory.getMonitor();

		// 2nd request should cleanup the 1st
		monitorFactory = this.strategy.handleRequest(this.secondRequest);
		monitorFactory.getMonitor();
		Thread.sleep(20);
		this.strategy.afterRequest(this.secondRequest, monitorFactory);

		assertThat(this.strategy.getCompletedRequests().size(), is(1));
	}

	@Test
	public void shouldNotifyPollingThreadsAfterRequest() throws Exception {
		this.strategy.setLongPollTime(TimeUnit.MINUTES.toMillis(1));
		HttpServletResponseMonitorFactory monitorFactory = this.strategy.handleRequest(this.request);
		monitorFactory.getMonitor().sendError(100);
		TimedPollThread timedPollThread = new TimedPollThread();
		timedPollThread.start();
		Thread.sleep(10);
		this.strategy.afterRequest(this.request, monitorFactory);
		timedPollThread.assertTime(10, 40);
	}

	@Test
	public void shouldTimeoutPoll() throws Exception {
		this.strategy.setLongPollTime(100);
		TimedPollThread timedPollThread = new TimedPollThread();
		timedPollThread.start();
		timedPollThread.assertTime(100, 200);
	}

	@Test
	public void shouldNotTimeoutEarly() throws Exception {
		this.strategy.setLongPollTime(100);
		TimedPollThread timedPollThread = new TimedPollThread();
		timedPollThread.start();
		// Complete a second request to trigger notify
		HttpServletResponseMonitorFactory monitorFactory = this.strategy.handleRequest(this.secondRequest);
		monitorFactory.getMonitor();
		this.strategy.afterRequest(this.secondRequest, monitorFactory);
		// Poll should not return early
		timedPollThread.assertTime(100, 200);
	}

	private class TimedPollThread extends Thread {

		private Exception exception;
		private long runtime;

		@Override
		public void run() {
			try {
				long startTime = System.currentTimeMillis();
				ReplayingTimeoutProtectionStrategyTest.this.strategy.handlePoll(
						ReplayingTimeoutProtectionStrategyTest.this.request,
						ReplayingTimeoutProtectionStrategyTest.this.response);
				this.runtime = System.currentTimeMillis() - startTime;
			} catch (Exception e) {
				this.exception = e;
			}
		}

		public void assertTime(long min, long max) throws Exception {
			join();
			if (this.exception != null) {
				throw this.exception;
			}
			assertThat(this.runtime, is(greaterThanOrEqualTo(min)));
			assertThat(this.runtime, is(lessThan(max)));
		}

	}
}
