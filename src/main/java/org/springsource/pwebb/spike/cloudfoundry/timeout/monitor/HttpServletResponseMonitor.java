package org.springsource.pwebb.spike.cloudfoundry.timeout.monitor;

import java.io.IOException;
import java.util.Locale;

import javax.servlet.http.Cookie;

public interface HttpServletResponseMonitor {

	public static final HttpServletResponseMonitor NONE = new HttpServletResponseMonitor() {

		public void write(byte[] b, int off, int len) {
		}

		public void write(byte[] b) {
		}

		public void write(int b) {
		}

		public void setStatus(int sc, String sm) {
		}

		public void setStatus(int sc) {
		}

		public void setLocale(Locale loc) {
		}

		public void setIntHeader(String name, int value) {
		}

		public void setHeader(String name, String value) {
		}

		public void setDateHeader(String name, long date) {
		}

		public void setContentType(String type) {
		}

		public void setContentLength(int len) {
		}

		public void setBufferSize(int size) {
		}

		public void sendRedirect(String location) throws IOException {
		}

		public void sendError(int sc) throws IOException {
		}

		public void sendError(int sc, String msg) throws IOException {
		}

		public void resetBuffer() {
		}

		public void reset() {
		}

		public void flushBuffer() throws IOException {
		}

		public void addIntHeader(String name, int value) {
		}

		public void addHeader(String name, String value) {
		}

		public void addDateHeader(String name, long date) {
		}

		public void addCookie(Cookie cookie) {
		}
	};

	void addCookie(Cookie cookie);

	void sendError(int sc, String msg) throws IOException;

	void sendError(int sc) throws IOException;

	void sendRedirect(String location) throws IOException;

	void setDateHeader(String name, long date);

	void addDateHeader(String name, long date);

	void setHeader(String name, String value);

	void addHeader(String name, String value);

	void setIntHeader(String name, int value);

	void addIntHeader(String name, int value);

	void setStatus(int sc);

	void setStatus(int sc, String sm);

	void setContentLength(int len);

	void setContentType(String type);

	void setBufferSize(int size);

	void flushBuffer() throws IOException;

	void reset();

	void resetBuffer();

	void setLocale(Locale loc);

	void write(int b);

	void write(byte[] b);

	void write(byte[] b, int off, int len);
}
