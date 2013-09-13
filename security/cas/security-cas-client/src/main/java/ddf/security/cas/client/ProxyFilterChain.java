/**
 * Copyright (c) Codice Foundation
 * 
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 * 
 **/
package ddf.security.cas.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.log4j.Logger;

/**
 * 
 * Proxy filter chain used to call of the required CAS filters in a specific order.
 * 
 */
public class ProxyFilterChain implements FilterChain {
    private static final Logger LOGGER = Logger.getLogger(ProxyFilterChain.class);

    private FilterChain filterChain;

    private List<Filter> filters;

    private Iterator<Filter> iterator;

    /**
     * @param filterChain
     *            The filter chain from the web container.
     */
    public ProxyFilterChain(FilterChain filterChain) {
        this.filterChain = filterChain;
        filters = new ArrayList<Filter>();
    }

    /**
     * @param filter
     *            The servlet filter to add.
     */
    public void addFilter(Filter filter) {
        if (iterator != null) {
            throw new IllegalStateException();
        }

        LOGGER.debug("Adding filter " + filter.getClass().getName() + " to filter chain.");
        filters.add(filter);
    }

    /**
     * @param filters
     *            The servlet filters to add.
     */
    public void addFilters(List<Filter> filters) {
        if (iterator != null) {
            throw new IllegalStateException();
        }

        this.filters.addAll(filters);

        if (LOGGER.isDebugEnabled()) {
            for (Filter filter : filters) {
                LOGGER.debug("Added filter " + filter.getClass().getName() + " to filter chain.");
            }
        }
    }

    /**
     * Calls the next filter in the chain.
     * 
     * @see javax.servlet.FilterChain#doFilter(javax.servlet.ServletRequest,
     *      javax.servlet.ServletResponse)
     */
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse)
        throws IOException, ServletException {
        if (iterator == null) {
            iterator = filters.iterator();
        }

        if (iterator.hasNext()) {
            Filter filter = iterator.next();
            LOGGER.debug("Calling " + filter.getClass().getName() + ".doFilter(" + servletRequest
                    + ", " + servletResponse + ", " + this + ")");
            filter.doFilter(servletRequest, servletResponse, this);
        } else {
            LOGGER.debug("Calling " + filterChain.getClass().getName() + ".doFilter("
                    + servletRequest + ", " + servletResponse + ")");
            filterChain.doFilter(servletRequest, servletResponse);
        }
    }

}
