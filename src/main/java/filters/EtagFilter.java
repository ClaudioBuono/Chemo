package filters;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Filter that implements ETag support for web responses.
 * It captures the response output, calculates an MD5 hash, and handles
 * the "If-None-Match" header to return 304 Not Modified when appropriate.
 *
 * This allows the browser to cache pages and save bandwidth/processing time.
 */
public class EtagFilter implements Filter {

    private static final String HEADER_ETAG = "ETag";
    private static final String HEADER_IF_NONE_MATCH = "If-None-Match";
    private static final String ALGORITHM_MD5 = "MD5";

    @Override
    public void init(final FilterConfig filterConfig) {
        // Initialization logic if needed
    }

    @Override
    public void destroy() {
        // Cleanup logic if needed
    }

    @Override
    public void doFilter(final ServletRequest req, final ServletResponse res, final FilterChain chain)
            throws IOException, ServletException {

        if (!(req instanceof final HttpServletRequest request) || !(res instanceof final HttpServletResponse response)) {
            chain.doFilter(req, res);
            return;
        }

        // 1. Wrap the response to capture the output stream
        final CharResponseWrapper wrappedResponse = new CharResponseWrapper(response);

        // 2. Process the request (execute Servlet, JSP, etc.)
        chain.doFilter(req, wrappedResponse);

        // 3. Retrieve the generated content bytes
        final byte[] responseBytes = wrappedResponse.getByteArray();

        // 4. Calculate the MD5 hash of the content
        final String etag = "\"" + getMd5Digest(responseBytes) + "\"";
        final String previousEtag = request.getHeader(HEADER_IF_NONE_MATCH);

        // 5. Compare the new ETag with the one sent by the browser
        if (previousEtag != null && previousEtag.equals(etag)) {
            // Cache Hit: The client has the latest version.
            // Return 304 (Not Modified) and do not send the body.
            response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
        } else {
            // Cache Miss: Send the new ETag and the content.
            response.setHeader(HEADER_ETAG, etag);

            // It is good practice to set Content-Length when buffering
            response.setContentLength(responseBytes.length);

            // Write the captured bytes to the real output stream
            response.getOutputStream().write(responseBytes);
        }
    }

    /**
     * Calculates the MD5 digest of a byte array and returns it as a hex string.
     */
    private String getMd5Digest(final byte[] bytes) {
        try {
            final MessageDigest md = MessageDigest.getInstance(ALGORITHM_MD5);
            final byte[] messageDigest = md.digest(bytes);
            // Convert byte array to Hex String using BigInteger/String.format
            final BigInteger number = new BigInteger(1, messageDigest);
            return String.format("%032x", number);
        } catch (final NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 algorithm not found", e);
        }
    }

    /**
     * Internal wrapper class to capture the response body.
     */
    private static class CharResponseWrapper extends HttpServletResponseWrapper {
        private final ByteArrayOutputStream capture;
        private ServletOutputStream output;
        private PrintWriter writer;

        public CharResponseWrapper(final HttpServletResponse response) {
            super(response);
            capture = new ByteArrayOutputStream(response.getBufferSize());
        }

        @Override
        public ServletOutputStream getOutputStream() {
            if (writer != null) {
                throw new IllegalStateException("getWriter() has already been called on this response.");
            }
            if (output == null) {
                output = new ServletOutputStream() {
                    @Override
                    public boolean isReady() { return true; }
                    @Override
                    public void setWriteListener(final WriteListener w) {
                        // Set Write Listener logic if needed
                    }
                    @Override
                    public void write(final int b) { capture.write(b); }
                    @Override
                    public void write(final byte[] b) throws IOException { capture.write(b); }
                    @Override
                    public void write(final byte[] b, final int off, final int len) { capture.write(b, off, len); }
                };
            }
            return output;
        }

        @Override
        public PrintWriter getWriter() throws IOException {
            if (output != null) {
                throw new IllegalStateException("getOutputStream() has already been called on this response.");
            }
            if (writer == null) {
                // Use the character encoding from the response to create the writer
                String encoding = getResponse().getCharacterEncoding();
                if (encoding == null) encoding = StandardCharsets.UTF_8.name();

                writer = new PrintWriter(new OutputStreamWriter(capture, encoding));
            }
            return writer;
        }

        /**
         * Flushes the streams and returns the captured byte array.
         */
        public byte[] getByteArray() throws IOException {
            if (writer != null) writer.flush();
            if (output != null) output.flush();
            return capture.toByteArray();
        }
    }
}