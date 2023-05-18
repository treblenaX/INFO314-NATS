import io.nats.client.Connection;
import io.nats.client.Nats;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Take the NATS URL on the command-line.
 */
public class StockPublisher {
  private static String natsUrl = null;
  public static void main(String... args) throws Exception {
//    if (args.length != 2)
//      throw new Exception("Usage: StockPublisher <host> <port>");
//    String host = args[0];
//    String port = args[1];

//    natsUrl = "nats://" + host + ":" + port;

    natsUrl = "nats://localhost:4222";
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
  public synchronized static void publishMessage(String symbol, int adjustment, int price) throws IOException, InterruptedException {
    Connection nc = Nats.connect(natsUrl);
    String xml = "";
    nc.publish(symbol, "hello".getBytes());
  }
}