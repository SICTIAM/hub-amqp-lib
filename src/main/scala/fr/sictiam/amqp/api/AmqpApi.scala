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
package fr.sictiam.amqp.api

import akka.Done
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.alpakka.amqp._

import scala.collection.immutable
import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by Nicolas DELAFORGE (nicolas.delaforge@mnemotix.com).
  * Date: 2019-01-30
  */

trait AmqpGenericAgent extends GenericService {

  val serviceName: String

  implicit val system: ActorSystem
  implicit val materializer: ActorMaterializer
  implicit val ec: ExecutionContext

  lazy val user: String = AmqpConfiguration.user
  lazy val pwd: String = AmqpConfiguration.pwd
  lazy val host: String = AmqpConfiguration.host
  lazy val port: Int = AmqpConfiguration.port
  lazy val durable: Boolean = AmqpConfiguration.durable
  lazy val fairDispatch: Boolean = AmqpConfiguration.fairDispatch
  lazy val prefetchCount: Int = AmqpConfiguration.prefetchCount
  lazy val timeout: Int = AmqpConfiguration.timeout

  lazy val queueName: String = s"$serviceName-queue-" + System.currentTimeMillis()
  lazy val queueDeclaration = QueueDeclaration(queueName).withDurable(durable)

  // use a list of host/port pairs where one is normally invalid, but it should still work as expected,
  val connectionProvider = AmqpDetailsConnectionProvider("invalid", 5673)
    .withCredentials(AmqpCredentials(user, pwd))
    .withHostsAndPorts(immutable.Seq(host -> port))

  //  val connectionProvider = AmqpUriConnectionProvider("amqp://admin:Pa55w0rd@localhost:5672")

  override def shutdown: Unit = {
    system.terminate()
  }
}

trait AmqpConsumer extends AmqpGenericAgent {
  /**
    * Consumes a fixed number of messages from the queue
    *
    * @param nbMsgToTake the number of messages to consume from the queue
    * @return a future collection of messages
    */
  def consume(nbMsgToTake: Long): Future[Seq[AmqpMessage]]
}

trait AmqpProducer extends AmqpGenericAgent {

  /**
    * Publishes a message to the broker with the name of the queue to
    *
    * @param messages : a collection of messages to publish
    * @return a future value set to Done if the publishing was successful
    */
  def publish(messages: Vector[AmqpMessage]): Future[Done]
}

trait AmqpServer extends AmqpConsumer with AmqpProducer
