import random
import time
from threading import Thread

class StockMarket:
    def __init__(self, symbols) -> None:
        self.stocks = {}
        self.symbols = symbols

        for symbol in symbols:
            price = random.randint(0, 500) + 10 # min $10.00
            self.stocks[symbol] = price
            print(symbol + " " + str(price))  # AMZN 4761

    def get_symbols(self):
        return self.symbols

    def get_price(self, symbol):
        return self.stocks[symbol]

    def update_price(self, symbol, price):
        self.stocks[symbol] = price


class symbolThread(Thread):
    def __init__(self, stock_market):
        # execute the base constructor
        Thread.__init__(self)
        # set a default value
        self.return_val = None
        self.sm = stock_market

    def run(self):
        # Choose a stock to fluctuate at random, +/- $5.00
        symbols = self.sm.get_symbols()
        symbol = symbols[random.randrange(len(symbols))]
        oldPrice = self.sm.get_price(symbol)
        adjustment = random.randint(-5, 5)
        newPrice = oldPrice + adjustment

        # update price in stock
        self.sm.update_price(symbol, newPrice)

        # get time_stamp
        time_stamp = time.time()

        # print(symbol, adjustment, newPrice)
        self.return_val = [symbol, adjustment, newPrice, time_stamp]


if __name__ == "__main__":
    sm1 = StockMarket(["AMZN", "GOOG", "MSFT"])
    # print(sm1.get_price("AMZN"), sm1.get_price("GOOG"), sm1.get_price("MSFT"))
    sm2 = StockMarket(["GE", "GMC", "FORD"])
    quit = False
    while (quit == False):
        time.sleep(2)
        thread1 = symbolThread(sm1)
        thread2 = symbolThread(sm2)
        # start the thread
        thread1.start()
        # wait for the thread to finish
        thread1.join()
        # get the value returned from the thread
        data1 = thread1.return_val
        print(data1[0], str(data1[1]), str(data1[2]), str(data1[3]))
        # start the thread
        thread2.start()
        # wait for the thread to finish
        thread2.join()
        # get the value returned from the thread
        data2 = thread2.return_val
        print(data2[0], str(data2[1]), str(data2[2]), str(data2[3]))
        # print(type(data2[0]), type(str(data2[1])), type(str(data2[2])), type(str(data2[3])))

    print("Press Ctrl-C to terminate")
