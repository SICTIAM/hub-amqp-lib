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

import akka.stream.alpakka.amqp.{IncomingMessage, OutgoingMessage}
import akka.util.ByteString
import fr.sictiam.amqp.AmqpSpec
import fr.sictiam.amqp.api.AmqpMessage
import fr.sictiam.amqp.api.rpc.AmqpRpcTopicController
import play.api.libs.json.{JsString, JsValue}

import scala.collection.mutable
import scala.concurrent.Await
import scala.concurrent.duration._


/**
  * Created by Nicolas DELAFORGE (nicolas.delaforge@mnemotix.com).
  * Date: 2019-02-25
  */

class AmqpRpcTopicControllerSpec extends AmqpSpec {

  val headers = Map.empty[String, JsValue]

  override implicit val patienceConfig = PatienceConfig(10.seconds)

  "The AmqpRpcTopicController" should {

    val messages = Vector(
      AmqpMessage(headers, JsString("One")),
      AmqpMessage(headers, JsString("Two")),
      AmqpMessage(headers, JsString("Three"))
    )
    val outputBuffer1 = mutable.Buffer[String]()
    val outputBuffer2 = mutable.Buffer[String]()
    val topic = "graph.create.node"

    val node1 = new AmqpRpcTopicController("testExchange", "rpcNode1Test") {
      override def onCommand(msg: IncomingMessage): OutgoingMessage = ???
    }

    val node2 = new AmqpRpcTopicController("testExchange", "rpcNode2Test") {
      override def onCommand(msg: IncomingMessage): OutgoingMessage = {
        println(s"Consummer1 message received : ${msg.bytes.utf8String}")
        outputBuffer1 += msg.bytes.utf8String
        OutgoingMessage(ByteString("Reponse received from Consumer 1"), false, false).withProperties(msg.properties)
      }
    }

    val node3 = new AmqpRpcTopicController("testExchange", "rpcNode3Test") {
      override def onCommand(msg: IncomingMessage): OutgoingMessage = {
        println(s"Consummer2 message received : ${msg.bytes.utf8String}")
        outputBuffer2 += msg.bytes.utf8String
        OutgoingMessage(ByteString("Reponse received from Consumer 2"), false, false).withProperties(msg.properties)
      }
    }
    "receive a command message, process it and write back the response to sender" in {
      val futureResult = node1.publish(topic, messages)
      println("Messages published.")
      node2.consume(topic, 10)
      node3.consume(topic, 10, true)
      Await.result(futureResult, Duration.Inf)
      outputBuffer1 shouldEqual messages.map(_.toString)
      outputBuffer2 shouldEqual messages.map(_.toString)
    }
  }
}
