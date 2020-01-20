/*
 * Copyright (c) 2002-2020 Manorrock.com. All Rights Reserved.
 *
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 *
 *   1. Redistributions of source code must retain the above copyright notice, 
 *      this list of conditions and the following disclaimer.
 *   2. Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in the
 *      documentation and/or other materials provided with the distribution.
 *   3. Neither the name of the copyright holder nor the names of its 
 *      contributors may be used to endorse or promote products derived from
 *      this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE 
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE 
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR 
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF 
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS 
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN 
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package cloud.piranha.servlet.webxml;

import static java.util.logging.Level.WARNING;
import static javax.xml.xpath.XPathConstants.NODE;
import static javax.xml.xpath.XPathConstants.NODESET;
import static javax.xml.xpath.XPathConstants.STRING;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration.Dynamic;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathException;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import cloud.piranha.api.WebApplication;
import javax.servlet.FilterRegistration;

/**
 * The web.xml initializer.
 *
 * @author Manfred Riem (mriem@manorrock.com)
 */
public class WebXmlInitializer implements ServletContainerInitializer {

    /**
     * Stores the logger.
     */
    private static final Logger LOGGER = Logger.getLogger(WebXmlInitializer.class.getName());

    /**
     * Stores the WebXML context-param name.
     */
    private static final String WEB_FRAGMENTS = "cloud.piranha.servlet.webxml.WebFragments";

    /**
     * On startup.
     *
     * @param classes the classes.
     * @param servletContext the servlet context.
     * @throws ServletException when a servlet error occurs.
     */
    @Override
    public void onStartup(Set<Class<?>> classes, ServletContext servletContext) throws ServletException {
        try {
            WebXmlManager manager = new WebXmlManager();
            servletContext.setAttribute(WebXmlManager.KEY, manager);
            
            WebApplication webApp = (WebApplication) servletContext;
            InputStream inputStream = servletContext.getResourceAsStream("WEB-INF/web.xml");
            if (inputStream != null) {
                WebXml webXml = parseWebXml(servletContext.getResourceAsStream("WEB-INF/web.xml"));
                manager.setWebXml(webXml);
            }

            Enumeration<URL> webFragments = servletContext.getClassLoader().getResources("/META-INF/web-fragment.xml");
            ArrayList<WebXml> webXmls = new ArrayList<>();
            while (webFragments.hasMoreElements()) {
                URL url = webFragments.nextElement();
                webXmls.add(parseWebXml(url.openStream()));
            }
            if (!webXmls.isEmpty()) {
                webApp.setAttribute(WEB_FRAGMENTS, webXmls);
            }

            if (manager.getWebXml() == null) {
                List<WebXml> fragments = (List<WebXml>) webApp.getAttribute(WEB_FRAGMENTS);
                if (fragments != null && !fragments.isEmpty()) {
                    manager.setWebXml(fragments.get(0));
                }
            }

            if (manager.getWebXml() != null) {
                WebXml webXml = (WebXml) manager.getWebXml();
                DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                Document document = documentBuilder.parse(inputStream);
                XPath xPath = XPathFactory.newInstance().newXPath();

                /*
                 * Process <display-name> content.
                 */
                String displayName = (String) xPath.evaluate("//display-name/text()", document, XPathConstants.STRING);
                webApp.setServletContextName(displayName);

                /*
                 * Process
                 */
                processContextParameters(webApp);
                processErrorPages(webApp);
                processFilters(webApp);
                processFilterMappings(webApp);
                processListeners(webApp);
                processMimeMappings(webApp);
                processServlets(webApp);
                processServletMappings(webApp);

                /*
                 * Process <security-constraint> entries
                 */
                NodeList list = (NodeList) xPath.evaluate("//security-constraint", document, NODESET);
                if (list != null) {
                    processSecurityConstraints(webXml, list);
                }

                /*
                 * Process <deny-uncovered-http-methods> entry
                 */
                Node node = (Node) xPath.evaluate("//deny-uncovered-http-methods", document, NODE);
                if (node != null) {
                    webXml.denyUncoveredHttpMethods = true;
                }
            } else {
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.info("No web.xml found!");
                }
            }
        } catch (SAXException | XPathExpressionException | IOException
                | ParserConfigurationException e) {
            LOGGER.log(WARNING, "Unable to parse web.xml", e);
        }
    }

    /**
     * Parse WebXml.
     *
     * @param inputStream the input stream.
     * @return the WebXml.
     */
    public WebXml parseWebXml(InputStream inputStream) {
        WebXml result = new WebXml();
        try {
            DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document document = documentBuilder.parse(inputStream);
            XPath xPath = XPathFactory.newInstance().newXPath();
            result.servlets.addAll((List<WebXml.Servlet>) parseList(
                    xPath, document, "//servlet", WebXmlInitializer::parseServlet));

            parseContextParameters(result, xPath, document);
            parseErrorPages(result, xPath, document);
            parseFilters(result, xPath, document);
            parseFilterMappings(result, xPath, document);
            parseListeners(result, xPath, document);
            parseLoginConfig(result, xPath, document);
            parseMimeMappings(result, xPath, document);
            parseServletMappings(result, xPath, document);

        } catch (Throwable t) {
            LOGGER.log(WARNING, "Unable to parse web.xml", t);
        }

        return result;
    }

    // ### Private methods
    /**
     * Parse a servlet.
     *
     * @param xPath the XPath to use.
     * @param node the DOM node to parse.
     * @return the servlet, or null if an error occurred.
     */
    private static WebXml.Servlet parseServlet(XPath xPath, Node node) {
        WebXml.Servlet result = new WebXml.Servlet();
        try {
            result.asyncSupported = (boolean) applyOrDefault(parseBoolean(xPath, node, "async-supported/text()"), false);
            result.name = (String) xPath.evaluate("servlet-name/text()", node, XPathConstants.STRING);
            result.className = (String) xPath.evaluate("servlet-class/text()", node, XPathConstants.STRING);
            Double loadOnStartupDouble = (Double) xPath.evaluate("load-on-startup/text()", node, XPathConstants.NUMBER);
            if (loadOnStartupDouble != null) {
                result.loadOnStartup = loadOnStartupDouble.intValue();
            } else {
                result.loadOnStartup = -1;
            }
            result.initParams.addAll((List<WebXml.Servlet.InitParam>) parseList(
                    xPath, node, "init-param", WebXmlInitializer::parseServletInitParam));
            return result;
        } catch (XPathExpressionException xee) {
            LOGGER.log(WARNING, "Unable to parse <servlet>", xee);
            result = null;
        }

        return result;
    }

    /**
     * Parse a servlet init-param.
     *
     * @param xPath the XPath to use.
     * @param node the DOM node to parse.
     * @return the init-param, or null if an error occurred.
     */
    private static WebXml.Servlet.InitParam parseServletInitParam(XPath xPath, Node node) {
        WebXml.Servlet.InitParam result = new WebXml.Servlet.InitParam();
        try {
            result.name = (String) xPath.evaluate("param-name/text()", node, XPathConstants.STRING);
            result.value = (String) xPath.evaluate("param-value/text()", node, XPathConstants.STRING);
        } catch (XPathExpressionException xee) {
            if (LOGGER.isLoggable(WARNING)) {
                LOGGER.log(WARNING, "Unable to parse <init-param>", xee);
            }
            result = null;
        }
        return result;
    }

    /**
     * Process the WebXml.Servlet list.
     *
     * @param webApplication the web application.
     */
    private void processServlets(WebApplication webApplication) {
        WebXml webXml = (WebXml) getWebXmlManager(webApplication).getWebXml();
        Iterator<WebXml.Servlet> iterator = webXml.servlets.iterator();
        while (iterator.hasNext()) {
            WebXml.Servlet servlet = iterator.next();
            Dynamic registration = webApplication.addServlet(servlet.name, servlet.className);
            if (servlet.asyncSupported) {
                registration.setAsyncSupported(true);
            }
            if (!servlet.initParams.isEmpty()) {
                servlet.initParams.forEach((initParam) -> {
                    registration.setInitParameter(initParam.name, initParam.value);
                });
            }
        }
    }

    private void processSecurityConstraints(WebXml webXml, NodeList nodeList) {
        for (int i = 0; i < nodeList.getLength(); i++) {
            processSecurityConstraint(webXml, nodeList.item(i));
        }
    }

    private void processSecurityConstraint(WebXml webXml, Node node) {
        try {
            XPath xPath = XPathFactory.newInstance().newXPath();
            WebXml.SecurityConstraint securityConstraint = new WebXml.SecurityConstraint();

            forEachNode(xPath, node, "//web-resource-collection", webResourceCollectionNode -> {
                WebXml.SecurityConstraint.WebResourceCollection webResourceCollection = new WebXml.SecurityConstraint.WebResourceCollection();

                forEachString(xPath, webResourceCollectionNode, "//url-pattern",
                        urlPattern -> webResourceCollection.urlPatterns.add(urlPattern)
                );

                forEachString(xPath, webResourceCollectionNode, "//http-method",
                        httpMethod -> webResourceCollection.httpMethods.add(httpMethod)
                );

                forEachString(xPath, webResourceCollectionNode, "//http-method-omission",
                        httpMethodOmission -> webResourceCollection.httpMethodOmissions.add(httpMethodOmission)
                );

                securityConstraint.webResourceCollections.add(webResourceCollection);
            });

            forEachString(xPath, getNodes(xPath, node, "//auth-constraint"), "//role-name/text()",
                    roleName -> securityConstraint.roleNames.add(roleName)
            );

            securityConstraint.transportGuarantee = getString(xPath, node, "//user-data-constraint/transport-guarantee/text()");

            webXml.securityConstraints.add(securityConstraint);

        } catch (Exception xpe) {
            LOGGER.log(WARNING, "Unable to parse <servlet> section", xpe);
        }
    }

    // ### Utility methods
    /**
     * Short-cut method for forEachNode - forEachString, when only one node's
     * string value is needed
     *
     */
    private void forEachString(XPath xPath, NodeList nodes, String expression, ThrowingConsumer<String> consumer) throws XPathExpressionException {
        for (int i = 0; i < nodes.getLength(); i++) {
            consumer.accept((String) xPath.evaluate(expression, nodes.item(i), XPathConstants.STRING));
        }
    }

    private void forEachString(XPath xPath, Node parent, String expression, ThrowingConsumer<String> consumer) throws XPathExpressionException {
        forEachNode(xPath, parent, expression, node -> consumer.accept(getString(xPath, node, "child::text()")));
    }

    private void forEachNode(XPath xPath, Node node, String expression, ThrowingConsumer<Node> consumer) throws XPathExpressionException {
        NodeList nodes = (NodeList) xPath.evaluate(expression, node, NODESET);
        for (int i = 0; i < nodes.getLength(); i++) {
            consumer.accept(nodes.item(i));
        }
    }

    private String getString(XPath xPath, Node node, String expression) throws XPathExpressionException {
        return (String) xPath.evaluate(expression, node, STRING);
    }

    private NodeList getNodes(XPath xPath, Node node, String expression) throws XPathExpressionException {
        return (NodeList) xPath.evaluate(expression, node, NODESET);
    }

    private interface ThrowingConsumer<T> {

        /**
         * Performs this operation on the given argument.
         *
         * @param t the input argument
         */
        void accept(T t) throws XPathExpressionException;
    }

    /**
     * Apply non null value or default value.
     *
     * @param object the object.
     * @param defaultValue the default value.
     * @return the non null value (either the object or the default value).
     */
    private static Object applyOrDefault(Object object, Object defaultValue) {
        Object result = object;
        if (object == null) {
            result = defaultValue;
        }
        return result;
    }

    /**
     * Parse a boolean.
     *
     * @param xPath the XPath to use.
     * @param node the node to use.
     * @param expression the expression to use.
     * @param defaultValue the default value.
     * @return the boolean, or the default value if an error occurred.
     */
    private static Boolean parseBoolean(XPath xPath, Node node, String expression) {
        Boolean result = null;
        try {
            result = (Boolean) xPath.evaluate(expression, node, XPathConstants.BOOLEAN);
        } catch (XPathExpressionException xee) {
            if (LOGGER.isLoggable(WARNING)) {
                LOGGER.log(WARNING, "Unable to parse boolean", xee);
            }
        }
        return result;
    }

    /**
     * Parse a list.
     *
     * <pre>
     *  1. Get the node list based on the expression.
     *  2. For each element in the node list
     *      a. Call the bi-function
     *      b. Add the result of the bi-function to the result list.
     *  3. Return the result list.
     * </pre>
     *
     * @param xPath the XPath to use.
     * @param node the DOM node to parse.
     * @param expression the XPath expression.
     * @param biFunction the bi-function.
     * @return the list, or null if an error occurred.
     */
    private static List parseList(XPath xPath, Node node, String expression, BiFunction<XPath, Node, Object> biFunction) {
        ArrayList result = new ArrayList();
        try {
            NodeList nodeList = (NodeList) xPath.evaluate(expression, node, NODESET);
            for (int i = 0; i < nodeList.getLength(); i++) {
                Object functionResult = biFunction.apply(xPath, nodeList.item(i));
                if (functionResult != null) {
                    result.add(functionResult);
                }
            }
        } catch (XPathExpressionException xee) {
            if (LOGGER.isLoggable(WARNING)) {
                LOGGER.log(WARNING, "Unable to parse <" + expression + ">", xee);
            }
            result = null;
        }
        return result;
    }

    // -------------------------------------------------------------------------
    //  Parsing
    // -------------------------------------------------------------------------
    /**
     * Parse the context-param section.
     *
     * @param webXml the web.xml to add to.
     * @param xPath the XPath to use.
     * @param nodeList the node list.
     */
    private void parseContextParameters(WebXml webXml, XPath xPath, Node node) {
        try {
            NodeList nodeList = (NodeList) xPath.evaluate("//context-param", node, NODESET);
            if (nodeList != null) {
                for (int i = 0; i < nodeList.getLength(); i++) {
                    String name = parseString(xPath, "//param-name/text()", nodeList.item(i));
                    String value = parseString(xPath, "//param-value/text()", nodeList.item(i));
                    webXml.addContextParam(name, value);
                }
            }
        } catch (XPathException xpe) {
            LOGGER.log(WARNING, "Unable to parse context parameters", xpe);
        }
    }

    /**
     * Parse the error-page section.
     *
     * @param webXml the web.xml to add to.
     * @param xPath the XPath to use.
     * @param node the DOM node.
     */
    private void parseErrorPages(WebXml webXml, XPath xPath, Node node) {
        try {
            NodeList nodeList = (NodeList) xPath.evaluate("//error-page", node, NODESET);
            if (nodeList != null) {
                for (int i = 0; i < nodeList.getLength(); i++) {
                    String errorCode = parseString(xPath, "error-code/text()", nodeList.item(i));
                    String exceptionType = parseString(xPath, "exception-type/text()", nodeList.item(i));
                    String location = parseString(xPath, "location/text()", nodeList.item(i));
                    webXml.addErrorPage(errorCode, exceptionType, location);
                }
            }
        } catch (XPathException xpe) {
            LOGGER.log(WARNING, "Unable to parse error pages", xpe);
        }
    }

    /**
     * Parse the filter section.
     *
     * @param webXml the web.xml to add to.
     * @param xPath the XPath to use.
     * @param node the DOM node.
     */
    private void parseFilters(WebXml webXml, XPath xPath, Node node) {
        try {
            NodeList nodeList = (NodeList) xPath.evaluate("//filter", node, NODESET);
            if (nodeList != null) {
                for (int i = 0; i < nodeList.getLength(); i++) {
                    WebXmlFilter filter = new WebXmlFilter();
                    String filterName = parseString(xPath, "filter-name/text()", nodeList.item(i));
                    filter.setFilterName(filterName);
                    String className = parseString(xPath, "filter-class/text()", nodeList.item(i));
                    filter.setClassName(className);
                    String servletName = parseString(xPath, "servlet-name/text()", nodeList.item(i));
                    filter.setServletName(servletName);
                    webXml.addFilter(filter);
                    NodeList paramNodeList = (NodeList) xPath.evaluate("//init-param", node, NODESET);
                    for (int j = 0; j < paramNodeList.getLength(); j++) {
                        WebXmlFilterInitParam initParam = new WebXmlFilterInitParam();
                        String name = parseString(xPath, "param-name/text()", paramNodeList.item(j));
                        initParam.setName(name);
                        String value = parseString(xPath, "param-value/text()", paramNodeList.item(j));
                        initParam.setValue(value);
                        filter.addInitParam(initParam);
                    }
                }
            }
        } catch (XPathException xpe) {
            LOGGER.log(WARNING, "Unable to parse listeners", xpe);
        }
    }


    /**
     * Parse the filter-mapping section.
     *
     * @param webXml the web.xml to use.
     * @param xPath the XPath to use.
     * @param nodeList the Node list to parse.
     */
    private void parseFilterMappings(WebXml webXml, XPath xPath, Node node) {
        try {
            NodeList nodeList = (NodeList) xPath.evaluate("//filter-mapping", node, NODESET);
            if (nodeList != null) {
                for (int i = 0; i < nodeList.getLength(); i++) {
                    String filterName = parseString(xPath, "filter-name/text()", nodeList.item(i));
                    String urlPattern = parseString(xPath, "url-pattern/text()", nodeList.item(i));
                    webXml.addFilterMapping(filterName, urlPattern);
                }
            }
        } catch (XPathExpressionException xee) {
            if (LOGGER.isLoggable(WARNING)) {
                LOGGER.log(WARNING, "Unable to parse filter mappings", xee);
            }
        }
    }
    
    /**
     * Parse the listener section.
     *
     * @param webXml the web.xml to add to.
     * @param xPath the XPath to use.
     * @param node the DOM node.
     */
    private void parseListeners(WebXml webXml, XPath xPath, Node node) {
        try {
            NodeList nodeList = (NodeList) xPath.evaluate("//listener", node, NODESET);
            if (nodeList != null) {
                for (int i = 0; i < nodeList.getLength(); i++) {
                    String className = parseString(xPath, "listener-class/text()", nodeList.item(i));
                    webXml.addListener(className);
                }
            }
        } catch (XPathException xpe) {
            LOGGER.log(WARNING, "Unable to parse listeners", xpe);
        }
    }

    /**
     * Parse the login-config section.
     *
     * @param webXml the web.xml to add to.
     * @param xPath the XPath to use.
     * @param node the DOM node.
     */
    private void parseLoginConfig(WebXml webXml, XPath xPath, Node node) {
        try {
            Node configNode = (Node) xPath.evaluate("//login-config", node, NODE);
            if (configNode != null) {
                String authMethod = parseString(xPath,
                        "//auth-method/text()", configNode);
                String realmName = parseString(xPath,
                        "//realm-name/text()", configNode);
                String formLoginPage = parseString(xPath,
                        "//form-login-config/form-login-page/text()", configNode);
                String formErrorPage = parseString(xPath,
                        "//form-login-config/form-error-page/text()", configNode);
                WebXmlLoginConfig config = new WebXmlLoginConfig(
                        authMethod, realmName, formLoginPage, formErrorPage);
                webXml.setLoginConfig(config);
            }
        } catch (XPathException xpe) {
            LOGGER.log(WARNING, "Unable to parse login config", xpe);
        }
    }

    /**
     * Parse the mime-mapping section.
     *
     * @param webXml the web.xml to add to.
     * @param xPath the XPath to use.
     * @param nodeList the node list.
     */
    private void parseMimeMappings(WebXml webXml, XPath xPath, Node node) {
        try {
            NodeList nodeList = (NodeList) xPath.evaluate("//mime-mapping", node, NODESET);
            if (nodeList != null) {
                for (int i = 0; i < nodeList.getLength(); i++) {
                    String extension = parseString(xPath, "//extension/text()", nodeList.item(i));
                    String mimeType = parseString(xPath, "//mime-type/text()", nodeList.item(i));
                    webXml.addMimeMapping(extension, mimeType);
                }
            }
        } catch (XPathException xpe) {
            LOGGER.log(WARNING, "Unable to parse mime mappings", xpe);
        }
    }

    /**
     * Parse the servlet-mapping section.
     *
     * @param webXml the web.xml to use.
     * @param xPath the XPath to use.
     * @param nodeList the Node list to parse.
     */
    private void parseServletMappings(WebXml webXml, XPath xPath, Node node) {
        try {
            NodeList nodeList = (NodeList) xPath.evaluate("//servlet-mapping", node, NODESET);
            if (nodeList != null) {
                for (int i = 0; i < nodeList.getLength(); i++) {
                    String servletName = parseString(xPath, "servlet-name/text()", nodeList.item(i));
                    String urlPattern = parseString(xPath, "url-pattern/text()", nodeList.item(i));
                    webXml.addServletMapping(servletName, urlPattern);
                }
            }
        } catch (XPathExpressionException xee) {
            if (LOGGER.isLoggable(WARNING)) {
                LOGGER.log(WARNING, "Unable to parse servlet mappings", xee);
            }
        }
    }

    /**
     * Parse a string.
     *
     * @param xPath the XPath to use.
     * @param expression the expression.
     * @param node the node.
     * @return the string.
     * @throws XPathExpressionException when the expression was invalid.
     */
    private String parseString(XPath xPath, String expression, Node node)
            throws XPathExpressionException {
        return (String) xPath.evaluate(expression, node, XPathConstants.STRING);
    }

    // -------------------------------------------------------------------------
    //  Processing
    // -------------------------------------------------------------------------
    /**
     * Process the context parameters.
     *
     * @param webApplication the web application.
     */
    private void processContextParameters(WebApplication webApplication) {
        Iterator<WebXmlContextParam> iterator = getWebXmlManager(webApplication)
                .getWebXml().getContextParams().iterator();
        while (iterator.hasNext()) {
            WebXmlContextParam contextParam = iterator.next();
            webApplication.setInitParameter(contextParam.getName(), contextParam.getValue());
        }
    }

    /**
     * Process the error pages.
     *
     * @param webApplication the web application.
     */
    private void processErrorPages(WebApplication webApplication) {
        WebXml webXml = getWebXmlManager(webApplication).getWebXml();
        for (WebXmlErrorPage errorPage : webXml.getErrorPages()) {
            if (errorPage.getErrorCode() != null && !errorPage.getErrorCode().isEmpty()) {
                webApplication.addErrorPage(Integer.parseInt(errorPage.getErrorCode()), errorPage.getLocation());
            } else if (errorPage.getExceptionType() != null && !errorPage.getExceptionType().isEmpty()) {
                webApplication.addErrorPage(errorPage.getExceptionType(), errorPage.getLocation());
            }
        }
    }

    /**
     * Process the filters.
     *
     * @param webApplication the web application.
     */
    private void processFilters(WebApplication webApplication) {
        WebXml webXml = getWebXmlManager(webApplication).getWebXml();
        webXml.getFilters().forEach((filter) -> {
            FilterRegistration.Dynamic dynamic = null;
            if (filter.getClassName() != null) {
                dynamic = webApplication.addFilter(
                        filter.getFilterName(), filter.getClassName());
            }
            if (filter.getServletName() != null) {
                dynamic = webApplication.addFilter(
                    filter.getFilterName(), filter.getServletName());
            }
            if (dynamic != null) {
                for (WebXmlFilterInitParam initParam : filter.getInitParams()) {
                    dynamic.setInitParameter(initParam.getName(), initParam.getValue());
                }
            }
        });
    }

    /**
     * Process the filter mappings mappings.
     *
     * @param webApplication the web application.
     */
    private void processFilterMappings(WebApplication webApplication) {
        WebXml webXml = getWebXmlManager(webApplication).getWebXml();
        Iterator<WebXmlFilterMapping> iterator = webXml.getFilterMappings().iterator();
        while (iterator.hasNext()) {
            WebXmlFilterMapping filterMapping = iterator.next();
            webApplication.addServletMapping(
                    filterMapping.getFilterName(), filterMapping.getUrlPattern());
        }
    }

    /**
     * Process the listeners.
     *
     * @param webApplication the web application.
     */
    private void processListeners(WebApplication webApplication) {
        Iterator<WebXmlListener> iterator = 
                getWebXmlManager(webApplication).getWebXml().getListeners().iterator();
        while (iterator.hasNext()) {
            WebXmlListener listener = iterator.next();
            webApplication.addListener(listener.getClassName());
        }
    }

    /**
     * Process the mime mappings.
     *
     * @param webApplication the web application.
     */
    private void processMimeMappings(WebApplication webApplication) {
        Iterator<WebXmlMimeMapping> mappingIterator = 
                getWebXmlManager(webApplication).getWebXml().getMimeMappings().iterator();
        while (mappingIterator.hasNext()) {
            WebXmlMimeMapping mapping = mappingIterator.next();
            webApplication.getMimeTypeManager()
                    .addMimeType(mapping.getExtension(), mapping.getMimeType());
        }
    }

    /**
     * Process the servlet mappings.
     *
     * @param webApplication the web application.
     */
    private void processServletMappings(WebApplication webApplication) {
        WebXml webXml = getWebXmlManager(webApplication).getWebXml();
        Iterator<WebXmlServletMapping> iterator = webXml.getServletMappings().iterator();
        while (iterator.hasNext()) {
            WebXmlServletMapping mapping = iterator.next();
            webApplication.addServletMapping(
                    mapping.getServletName(), mapping.getUrlPattern());
        }
    }
    
    /**
     * Get the web.xml manager.
     * 
     * @param webApplication the web application.
     * @return the web.xml manager.
     */
    private WebXmlManager getWebXmlManager(WebApplication webApplication) {
        return (WebXmlManager) webApplication.getAttribute(WebXmlManager.KEY);
    }
}
