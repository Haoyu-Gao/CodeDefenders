/*
 * Copyright (C) 2022 Code Defenders contributors
 *
 * This file is part of Code Defenders.
 *
 * Code Defenders is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Code Defenders is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Code Defenders. If not, see <http://www.gnu.org/licenses/>.
 */

package org.codedefenders.util;

import java.io.IOException;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codedefenders.configuration.Configuration;

import io.prometheus.client.filter.MetricsFilter;

@WebFilter(filterName = "metricsFilter")
public class CodeDefendersMetricsFilter extends MetricsFilter {

    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    Configuration config;

    public CodeDefendersMetricsFilter() {
        super("http_request_duration_seconds", "help", 0, null, true);
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse,
            FilterChain filterChain) throws IOException, ServletException {
        if (!(servletRequest instanceof HttpServletRequest) || !(servletResponse instanceof HttpServletResponse)) {
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }
        // We do not need metrics for the static resources and the notification endpoint has to high cardinality.
        Pattern pattern = Pattern.compile("(/(js|images|webjars|css|notifications)/.*|/favicon.ico)");
        if (!config.isMetricsCollectionEnabled()
                || pattern.matcher(((HttpServletRequest) servletRequest).getServletPath()).matches()) {
            filterChain.doFilter(servletRequest, servletResponse);
        } else {
            super.doFilter(servletRequest, servletResponse, filterChain);
        }
    }
}
