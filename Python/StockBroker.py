from pynats import NATSClient
import xml.etree.ElementTree as ET
import xml.dom.minidom
# StockBrokers are uniquely named (give each StockBroker a name constructor parameter that is used to identify this StockBroker everywhere in the system), and clients choose which StockBroker they use. When the client wishes
# they will send "buy" messages that look like the following:
class Broker:
    def __init__(self, name) -> None:
        self.name = name
        self.clients = {}
        self.natsurl = "nats://localhost:4222"
        self.curPrce = None

    def callback(self, msg):
        # print("Received a message with subject: " + msg.subject)
        content = msg.payload.decode()
        xml_data = content

        myroot = ET.fromstring(xml_data)   # parse xml file
        symbol = myroot[0][0].text
        price = myroot[0][2].text

        self.curPrice = price

        print("Current Price for " + symbol + " is: " + price)

    def parseRequest(self, req):
        # check buy or sell
        plan = "sell" if req[8] == 's' else "buy"

        # parse request
        doc = xml.dom.minidom.parseString(req)
        sym = doc.getElementsByTagName(plan)[0].getAttribute('symbol')
        amount = doc.getElementsByTagName(plan)[0].getAttribute('amount')
        return plan, sym, amount


    def transaction(self, plan, symbol, amount):
        cost = 0
        # check the current price
        with NATSClient(url=self.natsurl) as nc:
            nc.connect()
            print("Check the current price in StockPublisher:")
            nc.subscribe(subject=symbol, callback=self.callback)
            nc.wait(count=1)

        price = int(self.curPrice)
        # calculate cost
        if plan == 'sell':
            cost = price * int(amount) * 0.9
        elif plan == 'buy':
            cost = price * int(amount) * 1.1

        print("Complete: ", cost)
        cost = str(cost)

        originalOrder = "<{p} symbol='{s}' amount='{num}'/>".format(p=plan, s=symbol, num=amount)
        complete = "<complete amount='{c}'/>".format(c=cost)

        orderReceipt = "<orderReceipt>" + originalOrder + complete + "</orderReceipt>"

        print(orderReceipt)
        return orderReceipt

    def worker(self, name):
        with NATSClient(self.natsurl) as server:

            def callback(msg):
                print(f"Received a request on Customer'{msg.subject}:")
                req = msg.payload.decode()
                print(f"The request is: {req}")

                # parse request
                plan, symbol, amount = self.parseRequest(req)
                print(plan, symbol, amount)
                orderReceipt = self.transaction(plan, symbol, amount)

                # change from string to bytes
                receipt = bytes(orderReceipt, 'utf-8')

                server.publish(msg.reply, payload=receipt)

            server.subscribe(
                name, callback=callback, queue="test-queue", max_messages=2
            )
            server.wait(count=1)
