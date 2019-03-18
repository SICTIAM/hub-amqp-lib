/*
 *  Copyright (C) 2019 SICTIAM
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as
 *  published by the Free Software Foundation, either version 3 of the
 *  License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package fr.sictiam.amqp.rpc

import akka.Done
import akka.stream.alpakka.amqp.{IncomingMessage, OutgoingMessage}
import akka.util.ByteString
import fr.sictiam.amqp.AmqpSpec
import fr.sictiam.amqp.api.AmqpMessage
import fr.sictiam.amqp.api.rpc.{AmqpRpcTopicConsumer, AmqpRpcTopicProducer}
import play.api.libs.json.{JsString, JsValue}

import scala.collection.mutable
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}


/**
  * Created by Nicolas DELAFORGE (nicolas.delaforge@mnemotix.com).
  * Date: 2019-02-25
  */

class AmqpRpcTopicServerSpec extends AmqpSpec {

  val headers = Map.empty[String, JsValue]

  override implicit val patienceConfig = PatienceConfig(10.seconds)

  "The AmqpRpcTopicServer" should {

    val messages = Vector(
      AmqpMessage(headers, JsString("One")),
      AmqpMessage(headers, JsString("Two")),
      AmqpMessage(headers, JsString("Three"))
    )
    val outputBuffer1 = mutable.Buffer[String]()
    val outputBuffer2 = mutable.Buffer[String]()
    val replyBuffer = mutable.Buffer[String]()
    val topic = "graph.create.node"
    val exchange = "rpcTopicTestExchange"

    val producer = new AmqpRpcTopicProducer(exchange, "Main Producer") {
      override def onReply(msg: ByteString): Unit = {
        replyBuffer += msg.utf8String
        println(s"$serviceName / Reply received: ${msg.utf8String}")
      }
    }

    val consumer1 = new AmqpRpcTopicConsumer(topic, exchange, "Consumer 1") {
      override def onMessage(msg: IncomingMessage, params: String*)(implicit ec: ExecutionContext): Future[OutgoingMessage] = {
        println(s"$serviceName / Message received : ${msg.bytes.utf8String}")
        outputBuffer1 += msg.bytes.utf8String
        Future(OutgoingMessage(ByteString(s"Reponse received from $serviceName"), false, false).withProperties(msg.properties))(ec)
      }
    }

    val consumer2 = new AmqpRpcTopicConsumer(topic, exchange, "Consumer 2") {
      override def onMessage(msg: IncomingMessage, params: String*)(implicit ec: ExecutionContext): Future[OutgoingMessage] = {
        println(s"$serviceName / Message received : ${msg.bytes.utf8String}")
        outputBuffer2 += msg.bytes.utf8String
        Future(OutgoingMessage(ByteString(s"Reponse received from $serviceName"), false, false).withProperties(msg.properties))(ec)
      }
    }

    "receive a command message, process it and write back the response to sender" in {
      val futureResult = producer.publish(topic, messages)
      consumer1.consume()
      consumer2.consume(true)
      futureResult.futureValue shouldBe Done
      outputBuffer1 shouldEqual messages.map(_.toString)
      outputBuffer2 shouldEqual messages.map(_.toString)
      replyBuffer.size shouldEqual messages.size
    }
  }
}
