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
import fr.sictiam.amqp.api.controllers.{AmqpController, AmqpTask}
import fr.sictiam.amqp.api.rpc.AmqpRpcTopicServer
import play.api.libs.json._

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}

/**
  * Created by Nicolas DELAFORGE (nicolas.delaforge@mnemotix.com).
  * Date: 2019-02-27
  */

class AmqpControllerSpec extends AmqpSpec {
  "AmqpController" should {

    val exName = "testExchange"
    val topic1 = "graph.create.triples"
    val topic2 = "graph.update.triples"
    val topic3 = "graph.delete.triples"

    val headers = Map.empty[String, JsValue]

    val task1 = new AmqpTask {
      override def process(parameter: JsValue)(implicit ec: ExecutionContext): Future[JsValue] = {
        println("task1/" + Json.stringify(parameter))
        Future(JsNumber(Json.stringify(parameter).length))(ec)
      }
    }

    val task2 = new AmqpTask {
      override def process(parameter: JsValue)(implicit ec: ExecutionContext): Future[JsValue] = {
        println("task2/" + Json.stringify(parameter))
        Future(JsString("Awesome processing"))(ec)
      }
    }

    val task3 = new AmqpTask {
      override def process(parameter: JsValue)(implicit ec: ExecutionContext): Future[JsValue] = {
        println("task3/" + Json.stringify(parameter))
        Future(JsBoolean(true))(ec)
      }
    }

    val controller = new AmqpController(exName, "serviceTest")
    controller.registerTask(topic1, task1)
    controller.registerTask(topic2, task2)
    controller.registerTask(topic3, task3)
    "start without error" in {
      controller.start
    }
    "dispatch processing through atomic tasks regarding the topic tasks were registered with" in {

      val producer = new AmqpRpcTopicServer(exName, "producerTest") {
        override def onMessage(msg: IncomingMessage, params: String*)(implicit ec: ExecutionContext): Future[OutgoingMessage] = ???

        override def onReply(msg: ByteString): Unit = {
          println(s"Reply received : ${msg.utf8String}")
        }
      }

      val messages = Vector(
        AmqpMessage(headers, JsString("One"))
      )
      val f1 = producer.publish(topic1, messages)
      val f2 = producer.publish(topic2, messages)
      val f3 = producer.publish(topic3, messages)
      val overallFuture = Future.sequence(Seq(f1, f2, f3))
      Await.result(overallFuture, Duration.Inf)
    }
    "stop without error" in {
      controller.shutdown
    }
  }
}
