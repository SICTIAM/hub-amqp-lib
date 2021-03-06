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
import fr.sictiam.amqp.api.rpc.{AmqpRpcConsumer, AmqpRpcProducer}
import play.api.libs.json.{JsString, JsValue}

import scala.collection.mutable
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

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

    val outputBuffer = mutable.Buffer[String]()

    val producer = new AmqpRpcProducer("producerQueue", "Producer") {
      override def onReply(msg: ByteString): Unit = println(s"Reply received: ${msg.utf8String}")
    }

    val consumer = new AmqpRpcConsumer("consumerQueue", "Consumer") {
      override def onMessage(msg: IncomingMessage, params: String*)(implicit ec: ExecutionContext): Future[OutgoingMessage] = {
        outputBuffer += msg.bytes.utf8String
        Future(OutgoingMessage(ByteString(" OK"), false, false).withProperties(msg.properties))(ec)
      }
    }

    "receive a command, process it and write to reply queue without error" in {
      val futureResult = producer.publish("consumerQueue", messages)
      consumer.consume()
      futureResult.futureValue shouldBe Done
      outputBuffer shouldEqual messages.map(_.toString)
    }
  }
}
