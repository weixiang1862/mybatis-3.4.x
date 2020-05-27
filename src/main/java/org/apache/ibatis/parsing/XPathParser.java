/**
 * Copyright 2009-2017 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ibatis.parsing;

import org.apache.ibatis.builder.BuilderException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

/**
 * 基于Xpath的xml文件解析器，可以通过xpath解析多种数据类型，并可以通过PropertyParser解析占位符
 *
 * @author Clinton Begin
 */
public class XPathParser {

    // xml文件对象
    private final Document document;
    // 是否开启验证dtd
    private boolean validation;
    // 如何解析dtd文件，默认是从网络下载dtd
    private EntityResolver entityResolver;
    // properties，可以通过PropertyResolver解析
    private Properties variables;
    // xpath搜索
    private XPath xpath;

    public XPathParser(String xml) {
        commonConstructor(false, null, null);
        this.document = createDocument(new InputSource(new StringReader(xml)));
    }

    public XPathParser(Reader reader) {
        commonConstructor(false, null, null);
        this.document = createDocument(new InputSource(reader));
    }

    public XPathParser(InputStream inputStream) {
        commonConstructor(false, null, null);
        this.document = createDocument(new InputSource(inputStream));
    }

    public XPathParser(Document document) {
        commonConstructor(false, null, null);
        this.document = document;
    }

    public XPathParser(String xml, boolean validation) {
        commonConstructor(validation, null, null);
        this.document = createDocument(new InputSource(new StringReader(xml)));
    }

    public XPathParser(Reader reader, boolean validation) {
        commonConstructor(validation, null, null);
        this.document = createDocument(new InputSource(reader));
    }

    public XPathParser(InputStream inputStream, boolean validation) {
        commonConstructor(validation, null, null);
        this.document = createDocument(new InputSource(inputStream));
    }

    public XPathParser(Document document, boolean validation) {
        commonConstructor(validation, null, null);
        this.document = document;
    }

    public XPathParser(String xml, boolean validation, Properties variables) {
        commonConstructor(validation, variables, null);
        this.document = createDocument(new InputSource(new StringReader(xml)));
    }

    public XPathParser(Reader reader, boolean validation, Properties variables) {
        commonConstructor(validation, variables, null);
        this.document = createDocument(new InputSource(reader));
    }

    public XPathParser(InputStream inputStream, boolean validation, Properties variables) {
        commonConstructor(validation, variables, null);
        this.document = createDocument(new InputSource(inputStream));
    }

    public XPathParser(Document document, boolean validation, Properties variables) {
        commonConstructor(validation, variables, null);
        this.document = document;
    }

    public XPathParser(String xml, boolean validation, Properties variables, EntityResolver entityResolver) {
        commonConstructor(validation, variables, entityResolver);
        this.document = createDocument(new InputSource(new StringReader(xml)));
    }

    public XPathParser(Reader reader, boolean validation, Properties variables, EntityResolver entityResolver) {
        commonConstructor(validation, variables, entityResolver);
        this.document = createDocument(new InputSource(reader));
    }

    public XPathParser(InputStream inputStream, boolean validation, Properties variables, EntityResolver entityResolver) {
        commonConstructor(validation, variables, entityResolver);
        this.document = createDocument(new InputSource(inputStream));
    }

    public XPathParser(Document document, boolean validation, Properties variables, EntityResolver entityResolver) {
        commonConstructor(validation, variables, entityResolver);
        this.document = document;
    }

    public void setVariables(Properties variables) {
        this.variables = variables;
    }

    /**
     * 解析string
     *
     * @param expression
     * @return
     */
    public String evalString(String expression) {
        return evalString(document, expression);
    }

    /**
     * 解析string，最重要的方法，其他的解析都是通过string类型转换的
     *
     * @param root
     * @param expression
     * @return
     */
    public String evalString(Object root, String expression) {
        String result = (String) evaluate(expression, root, XPathConstants.STRING);
        // 解析出的结果再通过PropertyParser转换一遍
        result = PropertyParser.parse(result, variables);
        return result;
    }

    /**
     * 解析boolean
     *
     * @param expression
     * @return
     */
    public Boolean evalBoolean(String expression) {
        return evalBoolean(document, expression);
    }

    /**
     * 解析boolean
     *
     * @param root
     * @param expression
     * @return
     */
    public Boolean evalBoolean(Object root, String expression) {
        return (Boolean) evaluate(expression, root, XPathConstants.BOOLEAN);
    }

    /**
     * 解析short
     *
     * @param expression
     * @return
     */
    public Short evalShort(String expression) {
        return evalShort(document, expression);
    }

    /**
     * 解析short
     *
     * @param root
     * @param expression
     * @return
     */
    public Short evalShort(Object root, String expression) {
        return Short.valueOf(evalString(root, expression));
    }

    /**
     * 解析int
     *
     * @param expression
     * @return
     */
    public Integer evalInteger(String expression) {
        return evalInteger(document, expression);
    }

    /**
     * 解析int
     *
     * @param root
     * @param expression
     * @return
     */
    public Integer evalInteger(Object root, String expression) {
        return Integer.valueOf(evalString(root, expression));
    }

    /**
     * 解析long
     *
     * @param expression
     * @return
     */
    public Long evalLong(String expression) {
        return evalLong(document, expression);
    }

    /**
     * 解析long
     *
     * @param root
     * @param expression
     * @return
     */
    public Long evalLong(Object root, String expression) {
        return Long.valueOf(evalString(root, expression));
    }

    /**
     * 解析float
     *
     * @param expression
     * @return
     */
    public Float evalFloat(String expression) {
        return evalFloat(document, expression);
    }

    /**
     * 解析float
     *
     * @param root
     * @param expression
     * @return
     */
    public Float evalFloat(Object root, String expression) {
        return Float.valueOf(evalString(root, expression));
    }

    /**
     * 解析double
     *
     * @param expression
     * @return
     */
    public Double evalDouble(String expression) {
        return evalDouble(document, expression);
    }

    /**
     * 解析double
     *
     * @param root
     * @param expression
     * @return
     */
    public Double evalDouble(Object root, String expression) {
        return (Double) evaluate(expression, root, XPathConstants.NUMBER);
    }

    /**
     * 解析nodes
     *
     * @param expression
     * @return
     */
    public List<XNode> evalNodes(String expression) {
        return evalNodes(document, expression);
    }

    /**
     * 解析nodes
     *
     * @param root
     * @param expression
     * @return
     */
    public List<XNode> evalNodes(Object root, String expression) {
        List<XNode> xnodes = new ArrayList<XNode>();
        NodeList nodes = (NodeList) evaluate(expression, root, XPathConstants.NODESET);
        for (int i = 0; i < nodes.getLength(); i++) {
            xnodes.add(new XNode(this, nodes.item(i), variables));
        }
        return xnodes;
    }

    /**
     * 解析node
     *
     * @param expression
     * @return
     */
    public XNode evalNode(String expression) {
        return evalNode(document, expression);
    }

    /**
     * 解析node
     *
     * @param root
     * @param expression
     * @return
     */
    public XNode evalNode(Object root, String expression) {
        Node node = (Node) evaluate(expression, root, XPathConstants.NODE);
        if (node == null) {
            return null;
        }
        return new XNode(this, node, variables);
    }

    /**
     * 使用xpath从root解析expression，返回值类型为returnType
     *
     * @param expression
     * @param root
     * @param returnType
     * @return
     */
    private Object evaluate(String expression, Object root, QName returnType) {
        try {
            return xpath.evaluate(expression, root, returnType);
        } catch (Exception e) {
            throw new BuilderException("Error evaluating XPath.  Cause: " + e, e);
        }
    }

    /**
     * 根据成员变量设置dom解析方式，并初始化document
     *
     * @param inputSource
     * @return
     */
    private Document createDocument(InputSource inputSource) {
        // important: this must only be called AFTER common constructor
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setValidating(validation);

            factory.setNamespaceAware(false);
            factory.setIgnoringComments(true);
            factory.setIgnoringElementContentWhitespace(false);
            factory.setCoalescing(false);
            factory.setExpandEntityReferences(true);

            DocumentBuilder builder = factory.newDocumentBuilder();
            builder.setEntityResolver(entityResolver);
            builder.setErrorHandler(new ErrorHandler() {
                @Override
                public void error(SAXParseException exception) throws SAXException {
                    throw exception;
                }

                @Override
                public void fatalError(SAXParseException exception) throws SAXException {
                    throw exception;
                }

                @Override
                public void warning(SAXParseException exception) throws SAXException {
                }
            });
            return builder.parse(inputSource);
        } catch (Exception e) {
            throw new BuilderException("Error creating document instance.  Cause: " + e, e);
        }
    }

    /**
     * 初始化成员变量
     *
     * @param validation
     * @param variables
     * @param entityResolver
     */
    private void commonConstructor(boolean validation, Properties variables, EntityResolver entityResolver) {
        this.validation = validation;
        this.entityResolver = entityResolver;
        this.variables = variables;
        XPathFactory factory = XPathFactory.newInstance();
        this.xpath = factory.newXPath();
    }

}
