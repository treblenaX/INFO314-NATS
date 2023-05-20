import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import io.nats.client.Message;
import io.nats.client.Nats;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class StockBroker {
  private static DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
  private static Map<String, Integer> stockPrices = new HashMap<>();

  public StockBroker(String name, String natsUrl) throws Exception {
    Connection nc = Nats.connect(natsUrl);
    // listen for the stock changes
    Dispatcher stockDispatcher = nc.createDispatcher((msg) -> {
      String data = new String(msg.getData());
      try {
        processStock(data);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
    stockDispatcher.subscribe("*");

    new Thread(() -> {
      // listen to the client request
      Dispatcher clientDispatcher = nc.createDispatcher((msg) -> {
        String response = "";
        try {
          Order order = parseOrder(new String(msg.getData()));
          Thread.sleep(1000);
          response = buildOrderReceipt(order);
        } catch (Exception e) {
          e.printStackTrace();
        }
        // process message
        nc.publish(msg.getReplyTo(), response.getBytes());
      });
      clientDispatcher.subscribe("broker." + name);
    }).start();

    // TODO: take out - this is for testing, it will break if the symbol isn't in the map.
    Thread.sleep(5000);
    Message m  = nc.request("broker." + name, "<order><buy symbol=\"AMZN\" amount=\"100\" /></order>".getBytes(), Duration.ofSeconds(10));
    System.out.println(new String(m.getData(), StandardCharsets.UTF_8));
  }

  static class Order {
    String type;
    String symbol;
    int shares;
    Element orderElement;

    public Order(String type, String symbol, int shares, Element orderElement) {
      this.type = type;
      this.symbol = symbol;
      this.shares = shares;
      this.orderElement = orderElement;
    }

    public String toString() {
      return "[" + type + "] " + symbol + " - " + shares;
    }
  }

  private static Order parseOrder(String xml) throws Exception {
    DocumentBuilder db = dbf.newDocumentBuilder();
    Document doc = db.parse(new InputSource(new StringReader(xml)));
    NodeList typeElementList = doc.getElementsByTagName("buy");

    Element orderEl = (typeElementList.getLength() == 1)
            ? (Element) typeElementList.item(0)
            : (Element) doc.getElementsByTagName("sell").item(0);
    String type = orderEl.getNodeName();
    String symbol = orderEl.getAttribute("symbol");
    int amount = Integer.parseInt(orderEl.getAttribute("amount"));

    return new Order(type, symbol, amount, orderEl);
  }

  private static void processStock(String xml) throws Exception {
    DocumentBuilder db = dbf.newDocumentBuilder();
    Document doc = db.parse(new InputSource(new StringReader(xml)));

    NodeList nameElementList = doc.getElementsByTagName("name");
    NodeList adjustedPriceElementList = doc.getElementsByTagName("adjustedPrice");

    stockPrices.put(nameElementList.item(0).getTextContent(),
            Integer.parseInt(adjustedPriceElementList.item(0).getTextContent()));
  }

  private static String buildOrderReceipt(Order order) throws Exception {
    DocumentBuilder db = dbf.newDocumentBuilder();
    Document doc = db.newDocument();

    Element receiptElement = doc.createElement("orderReceipt");
    Element completeElement = doc.createElement("complete");

    // Calculate processing
    int amount = (order.type.equals("buy"))
            ? (int) ((stockPrices.get(order.symbol) * order.shares) * 1.10)
            : (int) ((stockPrices.get(order.symbol) * order.shares) * 0.90);
    completeElement.setAttribute("amount", "" + amount);

    receiptElement.appendChild(doc.adoptNode(order.orderElement.cloneNode(true)));
    receiptElement.appendChild(completeElement);

    doc.appendChild(receiptElement);

    TransformerFactory transformerFactory = TransformerFactory.newInstance();
    Transformer transformer = transformerFactory.newTransformer();
    DOMSource source = new DOMSource(doc);
    StringWriter writer = new StringWriter();
    StreamResult result = new StreamResult(writer);
    transformer.transform(source, result);

    return writer.toString();
  }
}
