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
package fr.sictiam.amqp.api.rpc

import akka.Done
import akka.actor.ActorSystem
import akka.stream._
import akka.stream.alpakka.amqp._
import akka.stream.alpakka.amqp.scaladsl.AmqpRpcFlow
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import akka.util.ByteString
import fr.sictiam.amqp.api._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

/**
  * Created by Nicolas DELAFORGE (nicolas.delaforge@mnemotix.com).
  * Date: 2019-02-20
  */

abstract class AmqpRpcProducer(val queueName: String, val serviceName: String, override val ackRequired: Boolean = false)(implicit val system: ActorSystem, val materializer: ActorMaterializer, val ec: ExecutionContext) extends AmqpGenericRpcProducer with NamedQueue {

  override def init: Unit = {}

  override def publish(toQueueName: String, messages: Vector[AmqpMessage]) = {

    val toQueueDeclaration = QueueDeclaration(toQueueName).withDurable(durable)

    // declare a simple RPC flow with a sink
    val amqpRpcFlow: Flow[ByteString, ByteString, Future[String]] = AmqpRpcFlow.simple(
      AmqpSinkSettings(connectionProvider)
        .withRoutingKey(toQueueName)
        .withDeclaration(toQueueDeclaration),
      1
    )

    // Declare a Sink for processing messages from reply queue
    val resultSink = Sink.foreach[ByteString] { msg =>
      beforeReply(msg)
      onReply(msg)
      afterReply(msg)
    }
    beforePublish(toQueueName, messages)

    // Send messages through the flow
    val (_, done: Future[Done]) = Source(messages).map(s => ByteString(s.toString)).viaMat(amqpRpcFlow)(Keep.right).toMat(resultSink)(Keep.both).run

    done.onComplete {
      case Success(_) => afterPublish(toQueueName, messages)
      case Failure(err) => onError(toQueueName, messages, err)
    }
    done
  }
}
