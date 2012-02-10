package org.springsource.pwebb.spike.cloudfoundry.timeout;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.util.Assert;
import org.springsource.pwebb.spike.cloudfoundry.timeout.monitor.DuplicatingHttpServletResponseMonitorFactory;
import org.springsource.pwebb.spike.cloudfoundry.timeout.monitor.HttpServletResponseMonitor;
import org.springsource.pwebb.spike.cloudfoundry.timeout.monitor.HttpServletResponseMonitorFactory;

public class HotSwappingTimeoutProtector implements TimeoutProtector {

	private static final long POLL_TIMEOUT = 0;
	protected static final long POLL_THRESHOLD = 0;
	protected static final long FAIL_TIMEOUT = 0;

	private static Map<String, RequestCoordinator> requestCoordinators = new HashMap<String, RequestCoordinator>();

	public HttpServletResponseMonitorFactory getMonitorFactory(final TimeoutProtectionHttpRequest request) {
		final long startTime = System.currentTimeMillis();
		return new HttpServletResponseMonitorFactory<HttpServletResponseMonitor>() {
			public HttpServletResponseMonitor getMonitor() {
				if (System.currentTimeMillis() - startTime < POLL_THRESHOLD) {
					return null;
				}
				RequestCoordinator requestCoordinator = getRequestCoordinator(request);
				HttpServletResponse pollResponse;
				synchronized (requestCoordinator) {
					pollResponse = requestCoordinator.consumePollResponse();
				}
				if (pollResponse == null) {
					try {
						requestCoordinator.awaitPollResponse(FAIL_TIMEOUT);
					} catch (InterruptedException e) {
						throw new IllegalStateException("Timeout waiting for poll", e);
					}
					pollResponse = requestCoordinator.consumePollResponse();
					Assert.notNull(pollResponse, "Unable to consume poll response");
				}
				return new DuplicatingHttpServletResponseMonitorFactory(pollResponse).getMonitor();
			}
		};
	}

	public void cleanup(TimeoutProtectionHttpRequest request, HttpServletResponseMonitorFactory monitor) {
		RequestCoordinator requestCoordinator = getRequestCoordinator(request);
		synchronized (requestCoordinator) {
			if (!requestCoordinator.isPollResponseConsumed()) {
				deleteRequestCoordinator(request);
			}
		}
	}

	public void handlePoll(TimeoutProtectionHttpRequest request, HttpServletResponse response) {
		RequestCoordinator requestCoordinator = getRequestCoordinator(request);
		synchronized (requestCoordinator) {
			requestCoordinator.setPollResponse(response);
		}
		try {
			requestCoordinator.awaitPollReponseConsumed(POLL_TIMEOUT);
		} catch (InterruptedException e) {
		}
		synchronized (requestCoordinator) {
			if (requestCoordinator.isPollResponseConsumed()) {
				deleteRequestCoordinator(request);
			} else {
				requestCoordinator.clearPollResponse();
				response.setStatus(HttpStatus.NO_CONTENT.value());
			}
		}
	}

	private RequestCoordinator getRequestCoordinator(TimeoutProtectionHttpRequest request) {
		synchronized (requestCoordinators) {
			String uid = request.getUid();
			RequestCoordinator requestCoordinator = requestCoordinators.get(uid);
			if (requestCoordinator == null) {
				requestCoordinator = new RequestCoordinator();
				requestCoordinators.put(uid, requestCoordinator);
			}
			return requestCoordinator;
		}
	}

	private void deleteRequestCoordinator(TimeoutProtectionHttpRequest request) {
		synchronized (requestCoordinators) {
			requestCoordinators.remove(request.getUid());
		}
	}

	private class RequestCoordinator {

		private HttpServletResponse pollResponse;

		private volatile boolean pollResponseConsumed;

		private CountDownLatch pollResponseLatch = new CountDownLatch(1);

		private CountDownLatch pollResponseConsumedLatch = new CountDownLatch(1);

		public void setPollResponse(HttpServletResponse pollResponse) {
			Assert.state(!this.pollResponseConsumed, "Unable to set an already consumed poll response");
			this.pollResponse = pollResponse;
			this.pollResponseLatch.countDown();
		}

		public void clearPollResponse() {
			Assert.state(!this.pollResponseConsumed, "Unable to clear an already consumed poll response");
			this.pollResponse = null;
		}

		public HttpServletResponse consumePollResponse() {
			if (this.pollResponse != null) {
				this.pollResponseConsumed = true;
				this.pollResponseConsumedLatch.countDown();
			}
			return this.pollResponse;
		}

		public boolean isPollResponseConsumed() {
			return isPollResponseConsumed();
		}

		public void awaitPollResponse(long timeout) throws InterruptedException {
			this.pollResponseLatch.await(timeout, TimeUnit.MILLISECONDS);
		}

		public void awaitPollReponseConsumed(long timeout) throws InterruptedException {
			this.pollResponseConsumedLatch.await(timeout, TimeUnit.MILLISECONDS);
		}

	}

}
