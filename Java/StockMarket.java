import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;

/**
 * This is a new feature of Java, called a "functional interface";
 * it defines a lambda type that means we can pass in a three-argument
 * method (instance or static) as a parameter to the StockMarket
 * constructor. See the StockPublisher code for an example.
 * 
 * In general, "function parameters" like this are a powerful way
 * to combine functionality without using traditional inheritance--in
 * essence, you are "customizing" the behavior of the recipient
 * by passing that functionality in as a parameter.
 */
@FunctionalInterface
interface Function3<T1, T2, T3> {
    public void apply(T1 one, T2 two, T3 three) throws IOException, InterruptedException;
}

/**
 * A StockMarket spends its time doing two different things:
 * one, thinking (essentially sleeping), and two, changing a stock
 * ticker symbol to a new value. Note that a given SMT will only
 * change stocks for a small set of companies/symbols. It will be
 * assumed that no other SMTs represent the same set of symbols (yet).
 */
public class StockMarket implements Runnable {
    private AtomicBoolean quit = new AtomicBoolean(false);
    private String[] symbols = null;
    private Map<String, Integer> stocks = new HashMap<>();
        // price is always in pennies--no floats! Floating-point money is evil.

    public Function3<String, Integer, Integer> publish = null;
        // You must provide the function instance that does the NATS work
  
    public StockMarket(Function3<String, Integer, Integer> publishFn, String... symbols)
        throws java.io.IOException, InterruptedException {
      this.symbols = symbols;
  
      // Set up initial stock prices
      Random startingPriceRandom = new Random();
      for (String symbol : symbols) {
        int price = startingPriceRandom.nextInt(10000) + 1000; // min $10.00
        stocks.put(symbol, price);
        System.out.println(symbol + " " + price(price));
      }

      this.publish = publishFn;
    }
  
    public void run() {
      Random waiter = new Random();
      Random stockChooser = new Random();
      Random priceFluctuator = new Random();
  
      try {
        while (quit.get() == false) {
          // Sleep up to 5 seconds
          Thread.sleep(waiter.nextInt(5) * 1000);
  
          // Choose a stock at random
          String symbol = symbols[stockChooser.nextInt(symbols.length)];
          int price = stocks.get(symbol);
  
          // Choose a fluctuation at random, +/- $5.00
          int adjustment = (priceFluctuator.nextInt(1000) - 500);
          price += adjustment;
          System.out.println(symbol + " " + price(adjustment) + " = " + price(price));
          stocks.put(symbol, price);
  
          // STUDENT: Publish the change to the NATS server
          publish.apply(symbol, adjustment, price);
        }
      }
      catch (InterruptedException intEx) {
        // These exceptions are designed to do exactly this:
        // interrupt us! Nothing bad to handle here. Just exit.
      }
      catch (IOException ioEx) {

      }
    }
    // Convenient method to transform pennies into USD
    public String price(int price) { return "$" + (price / 100.f); }
}
  