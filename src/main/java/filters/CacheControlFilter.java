package filters;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletResponse;

@WebFilter(urlPatterns = {
        // 1. Percorso della cartella statica
        "/static/*",

        // 2. Pattern standard per estensione
        "*.css", "*.js", "*.png", "*.jpg", "*.gif", "*.svg", "*.ico",

        // 3. Pattern specifici per gestire l'anomalia del suffisso 'KB'
        "*.cssKB",
        "*.jsKB",
        "*.pngKB",
        "*.jpgKB",
        "*.gifKB",
        "*.svgKB",
        "*.icoKB"
})
public class CacheControlFilter implements Filter {

    private static final long ONE_YEAR_IN_SECONDS = TimeUnit.DAYS.toSeconds(365);
    private static final long ONE_YEAR_IN_MILLIS = TimeUnit.DAYS.toMillis(365);

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (response instanceof HttpServletResponse) {
            HttpServletResponse httpResponse = (HttpServletResponse) response;

            httpResponse.setHeader("Cache-Control", "public, max-age=" + ONE_YEAR_IN_SECONDS);
            long expiresTime = System.currentTimeMillis() + ONE_YEAR_IN_MILLIS;
            httpResponse.setDateHeader("Expires", expiresTime);

        }
        chain.doFilter(request, response);
    }

    @Override public void init(FilterConfig filterConfig) throws ServletException {}
    @Override public void destroy() {}
}