package org.springsource.pwebb.spike.cloudfoundry.timeout.monitor;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.ReflectionUtils.MethodCallback;

/**
 * {@link HttpServletResponseMonitorFactory} that can be used to create a {@link ReplayableHttpServletResponseMonitor}
 * instance.
 * 
 * @author Phillip Webb
 */
public class ReplayableHttpServletResponseMonitorFactory implements
		HttpServletResponseMonitorFactory<ReplayableHttpServletResponseMonitor> {

	private static final List<MethodHandler> METHOD_HANDLERS;
	static {
		METHOD_HANDLERS = new ArrayList<MethodHandler>();
		METHOD_HANDLERS.add(new WriteByteMethodHandler());
		METHOD_HANDLERS.add(new WriteBytesMethodHandler());
		METHOD_HANDLERS.add(new WriteBytesWithOffsetMethodHandler());
		METHOD_HANDLERS.add(new ReplayMethodHandler());
		METHOD_HANDLERS.add(new DirectlyMappedMethodHandler());
	}

	public ReplayableHttpServletResponseMonitor getMonitor() {
		return (ReplayableHttpServletResponseMonitor) Proxy.newProxyInstance(getClass().getClassLoader(),
				new Class<?>[] { ReplayableHttpServletResponseMonitor.class }, new InvocationHandlerImpl());
	}

	/**
	 * Proxy {@link InvocationHandler} used to implement {@link ReplayableHttpServletResponseMonitor}.
	 */
	private static class InvocationHandlerImpl implements InvocationHandler {

		private ReplayableInvocations invocations = new ReplayableInvocations();

		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			MethodHandler methodHandler = getMethodHandler(method);
			return methodHandler.invoke(this.invocations, proxy, method, args);
		}

		private MethodHandler getMethodHandler(Method method) {
			for (MethodHandler methodHandler : METHOD_HANDLERS) {
				if (methodHandler.canHandle(method)) {
					return methodHandler;
				}
			}
			throw new UnsupportedOperationException("Unsupported method " + method.getName());
		}
	}

	/**
	 * Strategy interface used by {@link InvocationHandlerImpl} to handle method calls.
	 */
	private static interface MethodHandler {

		boolean canHandle(Method method);

		Object invoke(ReplayableInvocations invocations, Object proxy, Method method, Object[] args) throws Throwable;
	}

	/**
	 * Convenient based class for {@link MethodHandler} implementations.
	 */
	private static abstract class AbstractMethodHandler implements MethodHandler {

		private String methodName;
		private Class<?>[] params;

		public AbstractMethodHandler(String methodName, Class<?>... params) {
			this.methodName = methodName;
			this.params = params;
		}

		public boolean canHandle(Method method) {
			return method.getName().equals(this.methodName) && Arrays.equals(method.getParameterTypes(), this.params);
		}
	}

	/**
	 * Stores invocations that can subsequently be replayed. Method invocations will be stored and replayed in the order
	 * that they happen with the exception of {@link OutputStream} <tt>write</tt> methods, these will be collated into a
	 * single write operation.
	 */
	private static class ReplayableInvocations {

		private List<ReplayableInvocation> replayableInvocations = new ArrayList<ReplayableInvocation>();

		/**
		 * Provides access to the single output stream invocation. This will be lazily created when need and will also
		 * appear as an entry in {@link #replayableInvocations}.
		 */
		private ReplayableOutputStreamInvocation outputStreamInvocation;

		/**
		 * Record a method invocation
		 * @param method A {@link HttpServletResponse} method
		 * @param args method arguments
		 */
		public void record(Method method, Object[] args) {
			Assert.isTrue(method.getDeclaringClass().isAssignableFrom(HttpServletResponse.class),
					"Method must be from HttpServletResponse");
			this.replayableInvocations.add(new ReplayableMethodInvocation(method, args));
		}

		/**
		 * Returns a previously create {@link OutputStream} or records an invocation and returns a new
		 * {@link OutputStream}.
		 * @return the output stream
		 */
		public OutputStream getOrAddOutputStream() {
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
	 * {@link MethodHandler} to deal with {@link HttpServletResponseMonitor#write(int)}.
	 */
	private static class WriteByteMethodHandler extends AbstractMethodHandler {

		public WriteByteMethodHandler() {
			super("write", int.class);
		}

		public Object invoke(ReplayableInvocations invocations, Object proxy, Method method, Object[] args)
				throws Throwable {
			invocations.getOrAddOutputStream().write((Integer) args[0]);
			return null;
		}
	}

	/**
	 * {@link MethodHandler} to deal with {@link HttpServletResponseMonitor#write(byte[])}.
	 */
	private static class WriteBytesMethodHandler extends AbstractMethodHandler {

		public WriteBytesMethodHandler() {
			super("write", byte[].class);
		}

		public Object invoke(ReplayableInvocations invocations, Object proxy, Method method, Object[] args)
				throws Throwable {
			invocations.getOrAddOutputStream().write((byte[]) args[0]);
			return null;
		}
	}

	/**
	 * {@link MethodHandler} to deal with {@link HttpServletResponseMonitor#write(byte[], int, int)}.
	 */
	private static class WriteBytesWithOffsetMethodHandler extends AbstractMethodHandler {

		public WriteBytesWithOffsetMethodHandler() {
			super("write", byte[].class, int.class, int.class);
		}

		public Object invoke(ReplayableInvocations invocations, Object proxy, Method method, Object[] args)
				throws Throwable {
			invocations.getOrAddOutputStream().write((byte[]) args[0], (Integer) args[1], (Integer) args[2]);
			return null;
		}
	}

	/**
	 * {@link MethodHandler} to deal with {@link ReplayableHttpServletResponseMonitor#replay(HttpServletResponse)}.
	 */
	private static class ReplayMethodHandler extends AbstractMethodHandler {
		public ReplayMethodHandler() {
			super("replay", HttpServletResponse.class);
		}

		public Object invoke(ReplayableInvocations invocations, Object proxy, Method method, Object[] args)
				throws Throwable {
			invocations.replay((HttpServletResponse) args[0]);
			return null;
		}
	}

	/**
	 * {@link MethodHandler} to deal with all {@link HttpServletResponseMonitor} methods that can be directly mapped to
	 * {@link HttpServletResponse}.
	 */
	private static class DirectlyMappedMethodHandler implements MethodHandler {

		private static final Map<Method, Method> MAPPINGS;
		static {
			MAPPINGS = new HashMap<Method, Method>();
			ReflectionUtils.doWithMethods(ReplayableHttpServletResponseMonitor.class, new MethodCallback() {

				public void doWith(Method method) throws IllegalArgumentException, IllegalAccessException {
					Method foundMethod = ReflectionUtils.findMethod(HttpServletResponse.class, method.getName(),
							method.getParameterTypes());
					if (foundMethod != null) {
						MAPPINGS.put(method, foundMethod);
					}
				}
			});
		}

		public boolean canHandle(Method method) {
			return MAPPINGS.containsKey(method);
		}

		public Object invoke(ReplayableInvocations invocations, Object proxy, Method method, Object[] args)
				throws Throwable {
			invocations.record(MAPPINGS.get(method), args);
			return null;
		}
	}
}
