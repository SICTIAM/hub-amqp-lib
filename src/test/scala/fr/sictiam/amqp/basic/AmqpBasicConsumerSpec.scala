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
package fr.sictiam.amqp.basic

import akka.Done
import akka.stream.alpakka.amqp.IncomingMessage
import fr.sictiam.amqp.AmqpSpec
import fr.sictiam.amqp.api.AmqpMessage
import fr.sictiam.amqp.api.basic.{AmqpBasicConsumer, AmqpBasicProducer}
import play.api.libs.json.{JsString, JsValue}

import scala.collection.mutable
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

/**
  * Created by Nicolas DELAFORGE (nicolas.delaforge@mnemotix.com).
  * Date: 2019-02-01
  */

class AmqpBasicConsumerSpec extends AmqpSpec {
  val headers = Map.empty[String, JsValue]

  override implicit val patienceConfig = PatienceConfig(10.seconds)

  "The AmqpBasicConsumer" should {
    val queueName: String = s"basic-consumer-test-queue-" + System.currentTimeMillis()

    val producer = new AmqpBasicProducer(queueName, "producerTest")

    val messages = Vector(
      AmqpMessage(headers, JsString("One")),
      AmqpMessage(headers, JsString("Two")),
      AmqpMessage(headers, JsString("Three"))
    )
    val outputBuffer = mutable.Buffer[String]()

    val consumer = new AmqpBasicConsumer(queueName, "consumerTest") {
      override def onMessage(msg: IncomingMessage, params: String*)(implicit ec: ExecutionContext) = {
        outputBuffer += msg.bytes.utf8String
      }
    }

    "receive a message without error" in {
      consumer.consumeOnce(messages.size)
      producer.publish(messages).futureValue shouldBe Done
      outputBuffer.size shouldEqual 3
    }
  }
}
