/* Copyright (c) 2026 Olo Labs; SPDX-License-Identifier: Apache-2.0 */
package org.olo.app.filter;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.HandlerMapping;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;

class RequestLoggingFilterTest {
    @Test void diagnosticsExcludePrivateContentWithoutBufferingTheResponse() throws Exception {
        Logger logger = (Logger) LoggerFactory.getLogger(RequestLoggingFilter.class);
        var appender = new ListAppender<ILoggingEvent>();
        appender.start(); logger.addAppender(appender);
        try {
            var request = new MockHttpServletRequest("POST", "/api/runs/private-path/events");
            request.setContent("private-prompt".getBytes(StandardCharsets.UTF_8));
            request.setQueryString("token=private-token");
            request.addHeader("Authorization", "Bearer private-credential");
            var response = new MockHttpServletResponse();
            new RequestLoggingFilter().doFilter(request, response, (incoming, outgoing) -> {
                incoming.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "/api/runs/{id}/events");
                assertEquals("private-prompt", new String(incoming.getInputStream().readAllBytes(), StandardCharsets.UTF_8));
                outgoing.getWriter().write("private-response");
            });
            assertEquals("private-response", response.getContentAsString());
            String diagnostic = appender.list.stream().map(ILoggingEvent::getFormattedMessage)
                    .reduce("", String::concat);
            assertTrue(diagnostic.contains("route=/api/runs/{id}/events status=200"));
            assertFalse(diagnostic.contains("private-"));
        } finally { logger.detachAppender(appender); appender.stop(); }
    }
}
