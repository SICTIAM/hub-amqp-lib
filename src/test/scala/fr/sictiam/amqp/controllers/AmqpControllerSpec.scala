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
package fr.sictiam.amqp.controllers

import akka.stream.alpakka.amqp.{IncomingMessage, OutgoingMessage}
import akka.util.ByteString
import fr.sictiam.amqp.AmqpSpec
import fr.sictiam.amqp.api.AmqpMessage
import fr.sictiam.amqp.api.controllers.AmqpController
import fr.sictiam.amqp.api.rpc.AmqpRpcTopicServer
import play.api.libs.json._

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}

/**
  * Created by Nicolas DELAFORGE (nicolas.delaforge@mnemotix.com).
  * Date: 2019-02-27
  */

class AmqpControllerSpec extends AmqpSpec {
  "AmqpController" should {

    val exName = "testExchange"

    val topic1 = "graph.create.triples"
    val topic2 = "graph.read.triples"
    val topic3 = "graph.update.triples"
    val topic4 = "graph.delete.triples"

    val headers = Map.empty[String, JsValue]

    val controller = new AmqpController(exName, "serviceTest")
    controller.registerTask(topic1, new FakeCreateTask)
    controller.registerTask(topic2, new FakeReadTask)
    controller.registerTask(topic3, new FakeUpdateTask)
    controller.registerTask(topic4, new FakeDeleteTask)
    "start without error" in {
      controller.start
    }

    "dispatch processing through atomic tasks regarding the topic tasks were registered with" in {

      val producer = new AmqpRpcTopicServer(exName, "producerTest") {
        override def onMessage(msg: IncomingMessage, params: String*)(implicit ec: ExecutionContext): Future[OutgoingMessage] = {
          println(s"Message received : ${msg.bytes.utf8String}")
          Future(OutgoingMessage(ByteString("OK"), true, true))(ec)
        }

        override def onReply(msg: ByteString): Unit = {
          println(s"Reply received : ${msg.utf8String}")
        }
      }

      val f1 = producer.publish(topic1, Vector(AmqpMessage(headers, JsString("One"))))
      val f2 = producer.publish(topic2, Vector(AmqpMessage(headers, JsString("Two"))))
      val f3 = producer.publish(topic3, Vector(AmqpMessage(headers, JsString("Three"))))
      val f4 = producer.publish(topic4, Vector(AmqpMessage(headers, JsString("Three"))))

      val overallFuture = Future.sequence(Seq(f1, f2, f3, f4))
      Await.result(overallFuture, 5 seconds)

    }

    "stop without error" in {
      controller.shutdown
    }
  }
}
