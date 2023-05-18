import io.nats.client.Connection;
import io.nats.client.Nats;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Take the NATS URL on the command-line.
 */
public class StockPublisher {
  private static String natsUrl = null;
  private static DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
  public static void main(String... args) throws Exception {
    if (args.length != 0 || args.length != 2) throw new Exception("Usage: StockPublisher <host> <port>");
    natsUrl = (args.length == 2) ? "nats://" + args[0] + ":" + args[1] : "nats://localhost:4222";
    System.out.println("Starting stock publisher....");

    StockMarket sm1 = new StockMarket(StockPublisher::publishMessage, "AMZN", "MSFT", "GOOG");
    new Thread(sm1).start();
    StockMarket sm2 = new StockMarket(StockPublisher::publishMessage, "ACTV", "BLIZ", "ROVIO");
    new Thread(sm2).start();
    StockMarket sm3 = new StockMarket(StockPublisher::publishMessage, "GE", "GMC", "FORD");
    new Thread(sm3).start();
  }

  public synchronized static void publishDebugOutput(String symbol, int adjustment, int price) {
    System.console().writer().printf("PUBLISHING %s: %d -> %f\n", symbol, adjustment, (price / 100.f));
  }

  // When you have the NATS code here to publish a message, put "publishMessage"
  // in
  // the above where "publishDebugOutput" currently is
  public synchronized static void publishMessage(String symbol, int adjustment, int price) throws Exception {
    Connection nc = Nats.connect(natsUrl);
    nc.publish(symbol, buildMessageXML(symbol, "" + adjustment, "" + price).getBytes());
  }

  private static String buildMessageXML(String symbol, String adjustment, String price) throws Exception {
    DocumentBuilder db = dbf.newDocumentBuilder();
    Document doc = db.newDocument();

    Element messageElement = doc.createElement("message");
    messageElement.setAttribute("sent", (new Date().toString()));
    Element stockElement = doc.createElement("stock");
    Element nameElement = doc.createElement("name");
    Element adjustmentElement = doc.createElement("adjustment");
    Element adjustedPriceElement = doc.createElement("adjustedPrice");

    nameElement.setTextContent(symbol);
    adjustmentElement.setTextContent(adjustment);
    adjustedPriceElement.setTextContent(price);

    stockElement.appendChild(nameElement);
    stockElement.appendChild(adjustmentElement);
    stockElement.appendChild(adjustedPriceElement);

    messageElement.appendChild(stockElement);

    doc.appendChild(messageElement);

    TransformerFactory transformerFactory = TransformerFactory.newInstance();
    Transformer transformer = transformerFactory.newTransformer();
    DOMSource source = new DOMSource(doc);
    StringWriter writer = new StringWriter();
    StreamResult result = new StreamResult(writer);
    transformer.transform(source, result);

    return writer.toString();
  }
}