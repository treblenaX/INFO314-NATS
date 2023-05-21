import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import io.nats.client.Nats;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.FileWriter;
import java.io.StringReader;

public class SEC {
  private static DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
  public SEC(String natsUrl) throws Exception {
    Connection nc = Nats.connect(natsUrl);
    // listen for brokers
    Dispatcher d = nc.createDispatcher((msg) -> {
      String data = new String(msg.getData());
      String subject = msg.getSubject();
      try {
        printToLog(data, subject);
      } catch (Exception e) {
        e.printStackTrace();
      }
    });
    d.subscribe(">");
  }

  private void printToLog(String xml, String subject) throws Exception {
    DocumentBuilder db = dbf.newDocumentBuilder();
    Document doc = db.parse(new InputSource(new StringReader(xml)));
    NodeList receiptElementList = doc.getElementsByTagName("orderReceipt");

    if (receiptElementList.getLength() == 0) return;

    Element typeEl = (Element) receiptElementList.item(0).getFirstChild();

    String type = typeEl.getNodeName();
    String symbol = typeEl.getAttribute("symbol");
    int shareAmount = Integer.parseInt(typeEl.getAttribute("amount"));

    Element completeEl = (Element) receiptElementList.item(0).getLastChild();
    int pennyAmount = Integer.parseInt(completeEl.getAttribute("amount"));

    if (pennyAmount < 500000) return;

    File file = new File("logs/suspicions.log");

    FileWriter fw = new FileWriter(file, true);
    String data = subject + " " + type + " " + symbol + " " + shareAmount + " " + pennyAmount;
    fw.write(data + '\n');
    System.out.println(data);

    fw.close();
  }

  public static void main(String[] args) throws Exception {
    String natsUrl = (args.length == 2) ? "nats://" + args[0] + ":" + args[1] : "nats://localhost:4222";
    SEC sec = new SEC(natsUrl);
  }
}
