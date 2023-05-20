from pynats import NATSClient
import sys
import xml.etree.ElementTree as ET
"""
Previously: Subscribe.py
Receive message from StockPublisher through NATS Server

Run commands Line: "MSFT", "AMZN", "BLIZ"

look which markets they are belongs to
"""
# sys.argv[0] is StockerMonitor.py


def callback(msg):
    print("Received a message with subject: " + msg.subject)
    content = msg.payload.decode()
    xml_data = content

    myroot = ET.fromstring(xml_data)
    timestamp = myroot.attrib['sent']
    symbol = myroot[0][0].text
    adjustment = myroot[0][1].text
    price = myroot[0][2].text

    print("timestamp: ", timestamp)
    print(symbol, adjustment, price)

    # # write in a file - create price log for current symbol
    filename = symbol + '.txt'
    f = open(filename,"a")
    f.write("Timestamp: {t}, Adjustment: {a}, Current Price: {p}\r\n".format(t=timestamp, a=adjustment, p=price))
    f.close()


def connect(natsurl):
    with NATSClient(url=natsurl) as nc:
        nc.connect()
        print("connected")

        if len(sys.argv) <= 1:
            nc.subscribe(subject="*", callback=callback)
        else:
            for i in range(len(sys.argv)):
                symbol = sys.argv[i]
                print(sys.argv[i])
                nc.subscribe(subject=symbol, callback=callback)

        nc.wait()

    input("Press Enter to terminate")


if __name__ == "__main__":
    natsurl = "nats://localhost:4222"
    print("Starting Python message producer....")
    connect(natsurl)
