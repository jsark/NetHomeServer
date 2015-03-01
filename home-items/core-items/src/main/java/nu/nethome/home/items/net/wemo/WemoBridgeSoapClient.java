package nu.nethome.home.items.net.wemo;

import com.sun.org.apache.xerces.internal.parsers.DOMParser;
import nu.nethome.home.impl.ModelException;
import nu.nethome.home.items.net.wemo.soap.LightSoapClient;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.soap.SOAPException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.*;

public class WemoBridgeSoapClient extends LightSoapClient {
    public static final String GET_END_DEVICES = "GetEndDevices";
    public static final String BRIDGE_SERVICE_URL = "/upnp/control/bridge1";
    public static final String BRIDGE_NAMESPACE = "urn:Belkin:service:bridge:1";
    public static final int UNKNOWN = -1;

    private String wemoURL;

    public WemoBridgeSoapClient(String wemoURL) {
        this.wemoURL = wemoURL;
    }

    public List<BridgeDevice> getEndDevices(String deviceUdn) throws WemoException {
        try {
            List<Argument> arguments = new ArrayList<>();
            arguments.add(new Argument("DevUDN", deviceUdn));
            arguments.add(new Argument("ReqListType", "PAIRED_LIST"));
            Map<String, String> result = sendRequest(BRIDGE_NAMESPACE, wemoURL + BRIDGE_SERVICE_URL, GET_END_DEVICES, arguments);
            String deviceLists = result.get("DeviceLists");
            return parseDeviceList(deviceLists);
        } catch (SOAPException | IOException e) {
            throw new WemoException(e);
        }
    }

    private List<BridgeDevice> parseDeviceList(String deviceLists) {
        DOMParser parser = new DOMParser();
        ByteArrayInputStream byteStream;
        try {
            byteStream = new ByteArrayInputStream(deviceLists.getBytes("UTF-8"));
            InputSource source = new InputSource(byteStream);
            parser.parse(source);
            Document document = parser.getDocument();
            Node deviceListsx = getChildNode(document, "DeviceLists");;
            Node deviceList = getChildNode(deviceListsx, "DeviceList");;
            Node deviceInfos = getChildNode(deviceList, "DeviceInfos");
            NodeList deviceNodes = deviceInfos.getChildNodes();
            List<BridgeDevice> devices = new ArrayList<>();
            for (int i = 0; i < deviceNodes.getLength(); i++) {
                Node possibleDevice = deviceNodes.item(i);
                if (possibleDevice.getNodeType() == Node.ELEMENT_NODE && possibleDevice.getLocalName().equals("DeviceInfo")) {
                    devices.add(parseBridgeDevice(deviceNodes.item(i)));
                }
            }
            return devices;
        } catch (SAXException | IOException e) {
            return Collections.emptyList();
        }
    }

    private Node getChildNode(Node parentNode, String childNodeName) throws SAXException {
        NodeList childNodes = parentNode.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node currentChild = childNodes.item(i);
            if (currentChild.getNodeType() == Node.ELEMENT_NODE && currentChild.getLocalName().equals(childNodeName)) {
                return currentChild;
            }
        }
        throw new SAXException("Could not find node " + childNodeName);
    }

    private BridgeDevice parseBridgeDevice(Node item) {
        int deviceIndex = 0;
        String deviceID = "";
        String friendlyName = "";
        String firmwareVersion = "";
        String capabilityIDs = "";
        String currentState = "";
        NodeList childNodes = item.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node possibleAttribute = childNodes.item(i);
            if (isElementNamed(possibleAttribute, "DeviceIndex")) {
                deviceIndex = Integer.parseInt(possibleAttribute.getTextContent());
            }
            if (isElementNamed(possibleAttribute, "DeviceID")) {
                deviceID = possibleAttribute.getTextContent();
            }
            if (isElementNamed(possibleAttribute, "FriendlyName")) {
                friendlyName = possibleAttribute.getTextContent();
            }
            if (isElementNamed(possibleAttribute, "FirmwareVersion")) {
                firmwareVersion = possibleAttribute.getTextContent();
            }
            if (isElementNamed(possibleAttribute, "CapabilityIDs")) {
                capabilityIDs = possibleAttribute.getTextContent();
            }
            if (isElementNamed(possibleAttribute, "CurrentState")) {
                currentState = possibleAttribute.getTextContent();
            }
        }
        return new BridgeDevice(deviceIndex, deviceID, friendlyName, "", firmwareVersion, capabilityIDs, currentState);
    }

    private boolean isElementNamed(Node possibleAttribute, String deviceIndex) {
        return possibleAttribute.getNodeType() == Node.ELEMENT_NODE && possibleAttribute.getLocalName().equals(deviceIndex);
    }

    public String getWemoURL() {
        return wemoURL;
    }

    public void setWemoURL(String wemoURL) {
        this.wemoURL = wemoURL;
    }
}
