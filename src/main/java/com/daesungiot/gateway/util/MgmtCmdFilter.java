package com.daesungiot.gateway.util;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

public class MgmtCmdFilter implements javax.servlet.Filter {

	@Override
	public void destroy() {

		
	}

	@Override
	public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
			throws IOException, ServletException {
		
		HttpServletRequest httpReq = (HttpServletRequest) req;
		if(httpReq.getRequestURI().indexOf("mgmtCmd-") > -1 ) {
	        MutableHttpServletRequest mutableRequest = new MutableHttpServletRequest(httpReq);	
			mutableRequest.putHeader("Content-Type", "application/xml");
			chain.doFilter(mutableRequest, res);
		} else {
			chain.doFilter(req, res);
		}
		
	}

	@Override
	public void init(FilterConfig arg0) throws ServletException {

		
	}

}
