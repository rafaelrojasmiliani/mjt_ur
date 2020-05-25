package it.unibz.smf.mjt_client_ur.impl;

import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.apache.xmlrpc.client.XmlRpcCommonsTransportFactory;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.Socket;

public class XMLRPCClient {
  public static Object request(String hostname, String portNumber, String service, Object[] params) {
    if (!isServerAvailable(hostname, portNumber, 1000)) {
      return null;
    }

    try {
      XmlRpcClientConfigImpl xmlRpcConfig = new XmlRpcClientConfigImpl();
      xmlRpcConfig.setServerURL(getServerURL(hostname, portNumber));
      XmlRpcClient xmlRpcClient = new XmlRpcClient();
      xmlRpcClient.setTransportFactory(new XmlRpcCommonsTransportFactory(xmlRpcClient));
      xmlRpcClient.setConfig(xmlRpcConfig);
      return xmlRpcClient.execute(service, params);
    } catch(Exception e) {
      Swing.error("XMLRPC client", "Unexpected error when trying to execute the RPC method " + service + ": " + e.getMessage());
      return null;
    }
  }

  public static URL getServerURL(String hostname, String portNumber) throws MalformedURLException {
    return new URL("http://" + hostname + ":" + portNumber + Common.getDefault(Common.SERVICE_RPC_PATH));
  }

  public static boolean isServerAvailable(String hostname, String portNumber, int waitTimeout) {
    // host name to IP conversion
    InetAddress inet = null;
    try {
      inet = InetAddress.getByName(hostname);
    } catch (Exception e) {
        Swing.error("XMLRPC client", "Service not available: cannot get " + hostname + " IP");
        return false;
    }

    // ping
    try {
      if (!inet.isReachable(waitTimeout)) {
        Swing.error("XMLRPC client", "Service not available: " + hostname + " is not reachable");
        return false;
      }
    } catch (Exception e) {
        Swing.error("XMLRPC client", "Service not available: cannot test " + hostname + "reachability");
        return false;
    }

    // service
    Socket socket = null;
    try {
        socket = new Socket(inet, Integer.parseInt(portNumber));
    } catch (Exception e) {
        Swing.error("XMLRPC client", "Service not available: unable connect to " + hostname + " on port " + portNumber);
        return false;
    }
    try {
      socket.close();
    } catch(Exception e) {
    }

    return true;
  }
}
