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
import fr.sictiam.amqp.api.rpc.AmqpRpcServer
import play.api.libs.json.{JsString, JsValue}

import scala.collection.mutable
import scala.concurrent.Await
import scala.concurrent.duration._

/**
  * Created by Nicolas DELAFORGE (nicolas.delaforge@mnemotix.com).
  * Date: 2019-02-25
  */

class AmqpRpcServerSpec extends AmqpSpec {

  val headers = Map.empty[String, JsValue]

  override implicit val patienceConfig = PatienceConfig(10.seconds)

  "The AmqpRpcServer" should {

    val messages = Vector(
      AmqpMessage(headers, JsString("One")),
      AmqpMessage(headers, JsString("Two")),
      AmqpMessage(headers, JsString("Three"))
    )

    val node1 = new AmqpRpcServer("node1Queue", "rpcNode1Test") {
      override def onCommand(msg: IncomingMessage): OutgoingMessage = ???
    }

    val outputBuffer = mutable.Buffer[String]()
    val node2 = new AmqpRpcServer("node2Queue", "rpcNode2Test") {
      override def onCommand(msg: IncomingMessage): OutgoingMessage = {
        outputBuffer += msg.bytes.utf8String
        OutgoingMessage(msg.bytes.concat(ByteString(" OK")), false, false).withProperties(msg.properties)
      }
    }

    "send a command without error" in {
      val futureResult = node1.sendCommand("node2Queue", messages)
      node2.consume(10)
      Await.result(futureResult, Duration.Inf)
      outputBuffer shouldEqual messages.map(_.toString)
    }
  }
}
