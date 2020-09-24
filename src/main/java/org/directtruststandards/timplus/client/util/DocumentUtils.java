package org.directtruststandards.timplus.client.util;

import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

public class DocumentUtils
{
	public static Element deepCopyElement(Element rootElement, Document doc, String tag)
	{
		final Element newElement = doc.createElement(StringUtils.isEmpty(tag) ? rootElement.getTagName(): tag);

		final NamedNodeMap map = rootElement.getAttributes();
		for (int idx = 0; idx < map.getLength(); ++idx)
		{
			final Attr attr = (Attr)map.item(idx);
			newElement.setAttribute(attr.getName(), attr.getValue());
		}

		final NodeList childNodes = rootElement.getChildNodes();
		for (int idx = 0; idx < childNodes.getLength(); ++idx)
		{
			final Node childNode = childNodes.item(idx);

			if (childNode instanceof Element)
				newElement.appendChild(deepCopyElement((Element)childNode, doc, ""));
			else if (childNode instanceof Text)
				newElement.setTextContent(childNode.getTextContent());
		}
		
		return newElement;
	}
}
