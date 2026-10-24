package com.nexus.app.security;

import java.io.IOException;

import jakarta.servlet.http.HttpServletResponse;

/** Writes the same RFC 9457 problem shape as ApiExceptionHandler; titles and details are fixed constants. */
final class JsonErrorWriter {

    private JsonErrorWriter() {
    }

    static void write(HttpServletResponse response, int status, String title, String detail)
            throws IOException {
        response.setStatus(status);
        response.setContentType("application/problem+json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"type\":\"about:blank\",\"title\":\"" + title
                + "\",\"status\":" + status
                + ",\"detail\":\"" + detail + "\"}");
    }
}
