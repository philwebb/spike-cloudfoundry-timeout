package org.springsource.pwebb.spike.cloudfoundry.timeout.monitor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;

/**
 * {@link HttpServletResponseMonitorFactory} that can be used to create a {@link ReplayableHttpServletResponseMonitor}
 * instance.
 * 
 * @author Phillip Webb
 */
public class ReplayableHttpServletResponseMonitorFactory extends
		BridgedHttpServletResponseMonitorFactory<ReplayableHttpServletResponseMonitor> {

	private static final List<MethodHandler> METHOD_HANDLERS;
	static {
		METHOD_HANDLERS = new ArrayList<MethodHandler>();
		METHOD_HANDLERS.add(new WriteByteMethodHandler());
		METHOD_HANDLERS.add(new WriteBytesMethodHandler());
		METHOD_HANDLERS.add(new WriteBytesWithOffsetMethodHandler());
		METHOD_HANDLERS.add(new ReplayMethodHandler());
		METHOD_HANDLERS.add(new EqualsMethodHandler());
		METHOD_HANDLERS.add(new HashCodeMethodHandler());
		METHOD_HANDLERS.add(new ToStringMethodHandler());
		METHOD_HANDLERS.add(new DirectlyMappedMethodHandler());
	}

	@Override
	protected Collection<MethodHandler> getMethodHandlers() {
		return METHOD_HANDLERS;
	}

	@Override
	protected HttpServletResponseBridge newResponseBridge() {
		return new ReplayableHttpServletResponseBridge();
	}

	/**
	 * Stores invocations that can subsequently be replayed. Method invocations will be stored and replayed in the order
	 * that they happen with the exception of {@link OutputStream} <tt>write</tt> methods, these will be collated into a
	 * single write operation.
	 */
	private static class ReplayableHttpServletResponseBridge implements HttpServletResponseBridge {

		private List<ReplayableInvocation> replayableInvocations = new ArrayList<ReplayableInvocation>();

		/**
		 * Provides access to the single output stream invocation. This will be lazily created when need and will also
		 * appear as an entry in {@link #replayableInvocations}.
		 */
		private ReplayableOutputStreamInvocation outputStreamInvocation;

		public void invoke(Method method, Object[] args) throws Throwable {
			Assert.isTrue(method.getDeclaringClass().isAssignableFrom(HttpServletResponse.class),
					"Method must be from HttpServletResponse");
			this.replayableInvocations.add(new ReplayableMethodInvocation(method, args));
		}

		/**
		 * Returns a previously create {@link OutputStream} or records an invocation and returns a new
		 * {@link OutputStream}.
		 * @return the output stream
		 */
		public OutputStream getOutputStream() throws IOException {
			if (this.outputStreamInvocation == null) {
				this.outputStreamInvocation = new ReplayableOutputStreamInvocation();
				this.replayableInvocations.add(this.outputStreamInvocation);
			}
			return this.outputStreamInvocation.getOutputStream();
		}

		/**
		 * Replay all invocations to the specified response.
		 * @param response the response used to replay invocation
		 * @throws Throwable
		 */
		public void replay(HttpServletResponse response) throws Throwable {
			for (ReplayableInvocation replayableInvocation : this.replayableInvocations) {
				replayableInvocation.replay(response);
			}
		}
	}

	/**
	 * A single {@link ReplayableInvocation}.
	 */
	private static interface ReplayableInvocation {

		/**
		 * Replay the invocation
		 * @param response the response used to replay invocation
		 * @throws Throwable
		 */
		void replay(HttpServletResponse response) throws Throwable;
	}

	/**
	 * A {@link ReplayableInvocation} for a method call.
	 */
	private static class ReplayableMethodInvocation implements ReplayableInvocation {

		private Method method;
		private Object[] args;

		public ReplayableMethodInvocation(Method method, Object[] args) {
			this.method = method;
			this.args = args;
		}

		public void replay(HttpServletResponse response) throws Throwable {
			this.method.invoke(response, this.args);
		}
	}

	/**
	 * A {@link ReplayableInvocation} for {@link OutputStream} <tt>write</tt> operations.
	 */
	private static class ReplayableOutputStreamInvocation implements ReplayableInvocation {

		private ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

		public OutputStream getOutputStream() {
			return this.outputStream;
		}

		public void replay(HttpServletResponse response) throws Throwable {
			ServletOutputStream servletOutputStream = response.getOutputStream();
			FileCopyUtils.copy(this.outputStream.toByteArray(), servletOutputStream);
			servletOutputStream.flush();
		}
	}

	/**
	 * {@link MethodHandler} to deal with {@link ReplayableHttpServletResponseMonitor#replay(HttpServletResponse)}.
	 */
	private static class ReplayMethodHandler extends AbstractMethodHandler {
		public ReplayMethodHandler() {
			super("replay", HttpServletResponse.class);
		}

		public Object invoke(HttpServletResponseBridge bridge, Object proxy, Method method, Object[] args)
				throws Throwable {
			((ReplayableHttpServletResponseBridge) bridge).replay((HttpServletResponse) args[0]);
			return null;
		}
	}
}
