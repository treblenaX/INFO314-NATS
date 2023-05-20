import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import io.nats.client.Nats;

public class StockMonitor {
  private static String natsUrl = null;
  public static void main(String[] args) throws Exception {
    if (args.length != 2)
      throw new Exception("Usage: StockMonitor <host> <port>");
    String host = args[0];
    String port = args[1];

    natsUrl = "nats://" + host + ":" + port;
    Connection nc = Nats.connect(natsUrl);
    Dispatcher d = nc.createDispatcher((msg) -> {
      System.out.println(new String(msg.getData()));
    });

    d.subscribe("*");
  }
}