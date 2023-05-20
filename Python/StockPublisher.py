from pynats import NATSClient
from StockMarket import StockMarket, symbolThread
import time

"""
connect to the NATS server and spin up three "stock markets":
publish message from StockMarket

"""


def main(natsurl):
    # get the message from Stock Market
    print("Starting stock publisher....")
    sm1 = StockMarket(["AMZN", "GOOG", "MSFT"])
    sm2 = StockMarket(["GE", "GMC", "FORD"])

    with NATSClient(url=natsurl) as nc:
        nc.connect()

        # an infinite loop
        while (True):
            time.sleep(2)
            thread1 = symbolThread(sm1)
            thread2 = symbolThread(sm2)
            # start the thread for 1st market
            thread1.start()
            thread1.join()                  # wait for the thread to finish
            data1 = thread1.return_val      # get the value returned from the thread
            print(data1[0], data1[1], data1[2], data1[3])
            subject1, adjust1, price1 = data1[0], str(data1[1]), str(data1[2])
            time1 = "'{t}'".format(t=data1[3])
            message1 = '''<?xml version="1.0" encoding="UTF-8"?>
                            <message sent={time}>
                            <stock>
                                <name>{symbol}</name>
                                <adjustment>{adjust}</adjustment>
                                <adjustedPrice>{price}</adjustedPrice>
                            </stock>
                            </message>
                        '''.format(time=time1, symbol=subject1, adjust=adjust1, price=price1)
            nc.publish(subject=subject1, payload=message1);

            # start the thread for 2nd market
            thread2.start()
            thread2.join()
            data2 = thread2.return_val
            print(data2[0], data2[1], data2[2], data2[3])
            subject2, adjust2, price2 = data2[0], str(data2[1]), str(data2[2])
            time2 = "'{t}'".format(t=data2[3])
            message2 = '''<?xml version="1.0" encoding="UTF-8"?>
                            <message sent={time}>
                            <stock>
                                <name>{symbol}</name>
                                <adjustment>{adjust}</adjustment>
                                <adjustedPrice>{price}</adjustedPrice>
                            </stock>
                            </message>
                        '''.format(time=time2, symbol=subject2, adjust=adjust2, price=price2)
            # Publish a message
            # nc.publish(subject=subject1, payload=bytes(message1, "UTF-8"));
            nc.publish(subject=subject2, payload=message2);
            # print("Exiting main...")

natsurl = "nats://localhost:4222"
print("Starting Python message producer....")
main(natsurl)

