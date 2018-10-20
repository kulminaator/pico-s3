package com.github.kulminaator.s3.xml;

import com.github.kulminaator.s3.S3Object;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class S3XmlParser {

    public static Document parseS3Xml(String xml) {
        try {
            final DocumentBuilderFactory builder = DocumentBuilderFactory.newInstance();
            final InputStream inputStream = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
            final Document document = builder.newDocumentBuilder().parse(inputStream);
            document.normalize();
            return document;
        } catch (IOException | ParserConfigurationException | SAXException e) {
            throw new IllegalStateException("Unable to parse aws s3 xml ", e);
        }
    }

    public static List<S3Object> parseObjectsFromXml(Document s3XmlDocument) {
        final NodeList elements = s3XmlDocument.getDocumentElement().getElementsByTagName("Contents");
        final List<S3Object> objectList = new ArrayList<>(elements.getLength());
        for (int i = 0; i < elements.getLength(); i++) {
            final Element element = (Element) elements.item(i);
            S3Object object = new S3Object();
            object.setKey(element.getElementsByTagName("Key").item(0).getTextContent());
            object.setETag(element.getElementsByTagName("ETag").item(0).getTextContent());
            object.setSize(Long.valueOf(element.getElementsByTagName("Size").item(0).getTextContent()));
            object.setLastModified(element.getElementsByTagName("LastModified").item(0).getTextContent());
            objectList.add(object);
        }
        return objectList;
    }
}
