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
import akka.util.ByteString
import fr.sictiam.common.GenericService

import scala.collection.immutable
import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by Nicolas DELAFORGE (nicolas.delaforge@mnemotix.com).
  * Date: 2019-01-30
  */

trait AmqpConfiguration {
  lazy val user: String = AmqpClientConfiguration.user
  lazy val pwd: String = AmqpClientConfiguration.pwd
  lazy val host: String = AmqpClientConfiguration.host
  lazy val port: Int = AmqpClientConfiguration.port
  lazy val durable: Boolean = AmqpClientConfiguration.durable
  lazy val fairDispatch: Boolean = AmqpClientConfiguration.fairDispatch
  lazy val prefetchCount: Int = AmqpClientConfiguration.prefetchCount
  lazy val timeout: Int = AmqpClientConfiguration.timeout
}

trait AmqpGenericAgent extends GenericService with AmqpConfiguration {

  val serviceName: String

  implicit val system: ActorSystem
  implicit val materializer: ActorMaterializer
  implicit val ec: ExecutionContext

  // use a list of host/port pairs where one is normally invalid, but it should still work as expected,
  val connectionProvider = AmqpDetailsConnectionProvider("invalid", 5673)
    .withCredentials(AmqpCredentials(user, pwd))
    .withHostsAndPorts(immutable.Seq(host -> port))

  override def shutdown: Unit = {
    system.terminate()
  }
}

trait AmqpGenericRpcServer extends AmqpGenericAgent {

  def publish(toQueueName: String, messages: Vector[AmqpMessage]): Future[Done]

  def onMessage(msg: IncomingMessage, params: String*)(implicit ec: ExecutionContext): Future[OutgoingMessage]

  def onReply(msg: ByteString): Unit = {}

  def beforePublish(topic: String, messages: Vector[AmqpMessage]): Unit = {}

  def afterPublish(topic: String, messages: Vector[AmqpMessage]): Unit = {}

  def onError(topic: String, messages: Vector[AmqpMessage], err: Throwable): Unit = {
    logger.error(err.getMessage)
    err.printStackTrace()
  }

  def beforeReply(msg: ByteString) = {}

  def afterReply(msg: ByteString) = {}

}

trait AmqpGenericConsumer extends AmqpGenericAgent {
  /**
    * Consumes a fixed number of messages from the queue
    *
    * @param nbMsgToTake the number of messages to consume from the queue
    * @return a future collection of messages
    */
  def consume(nbMsgToTake: Long): Future[Seq[AmqpMessage]]
}

trait AmqpGenericProducer extends AmqpGenericAgent {
  /**
    * Publishes a message to the broker with the name of the queue to
    *
    * @param messages : a collection of messages to publish
    * @return a future value set to Done if the publishing was successful
    */
  def publish(messages: Vector[AmqpMessage]): Future[Done]
}

trait NamedQueue extends AmqpGenericAgent {
  val queueName: String
  lazy val queueDeclaration = QueueDeclaration(queueName).withDurable(durable)
  lazy val sourceSettings: AmqpSourceSettings = NamedQueueSourceSettings(connectionProvider, queueName).withDeclaration(queueDeclaration)
}

trait Exchange extends AmqpGenericAgent {
  val exchangeName: String
  val exchangeType: ExchangeTypes.ExchangeTypeVal
  lazy val exchangeDeclaration = ExchangeDeclaration(exchangeName, exchangeType.label)
  lazy val sourceSettings: AmqpSourceSettings = TemporaryQueueSourceSettings(connectionProvider, exchangeName).withDeclaration(exchangeDeclaration)
}
