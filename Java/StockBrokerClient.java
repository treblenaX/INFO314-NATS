import io.nats.client.Connection;
import io.nats.client.Nats;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public class StockBrokerClient {
    private final String natsUrl;
    private final String brokerName;
    private Connection nc;

    public StockBrokerClient(String natsUrl, String brokerName) {
        this.natsUrl = natsUrl;
        this.brokerName = brokerName;
    }

    public void connect() throws Exception {
        nc = Nats.connect(natsUrl);
    }

    public void placeBuyOrder(String symbol, int amount) throws Exception {
        String order = String.format("<order><buy symbol=\"%s\" amount=\"%d\" /></order>", symbol, amount);
        byte[] response = nc.request("broker." + brokerName, order.getBytes(), Duration.ofSeconds(10)).getData();
        System.out.println("Received Response: " + new String(response, StandardCharsets.UTF_8));
    }

    public void placeSellOrder(String symbol, int amount) throws Exception {
        String natsUrl = "nats://localhost:4222";
        String brokerName = "elb";
        StockBrokerClient client = new StockBrokerClient(natsUrl, brokerName);
        client.connect();
        client.placeBuyOrder("AMZN", 100);
//        client.placeSellOrder("GOOG", 50);
    }

    public static void main(String[] args) throws Exception {
        String natsUrl = (args.length == 2) ? "nats://" + args[0] + ":" + args[1] : "nats://localhost:4222";
        StockBrokerClient client = new StockBrokerClient(natsUrl, "elb");
        client.connect();
        client.placeBuyOrder("AMZN", 1000);
    }
}