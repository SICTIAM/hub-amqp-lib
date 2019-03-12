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
package fr.sictiam.amqp.api.controllers

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.alpakka.amqp.{IncomingMessage, OutgoingMessage}
import akka.util.ByteString
import com.typesafe.scalalogging.LazyLogging
import fr.sictiam.amqp.api.AmqpClientConfiguration
import fr.sictiam.amqp.api.actors.Scheduler
import fr.sictiam.amqp.api.rpc.AmqpRpcTopicServer
import play.api.libs.json.Json

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by Nicolas DELAFORGE (nicolas.delaforge@mnemotix.com).
  * Date: 2019-02-26
  */

class AmqpController(val exchangeName: String, val serviceName: String)(implicit val system: ActorSystem, val materializer: ActorMaterializer, val ec: ExecutionContext) extends LazyLogging {

  val server = new AmqpRpcTopicServer(exchangeName, serviceName) {
    override def onMessage(msg: IncomingMessage, params: String*)(implicit ec: ExecutionContext): Future[OutgoingMessage] = {
      val topic = params(0)
      val task = tasks(topic)
      task.process(Json.parse(msg.bytes.utf8String)).map { jsval =>
        OutgoingMessage(
          ByteString(jsval.toString()),
          false,
          false
        ).withProperties(msg.properties)
      }
    }
  }

  lazy val scheduler = new Scheduler(server, tasks.keySet.toSet, 1, system, ec)

  var tasks = mutable.HashMap[String, AmqpTask]()

  def registerTask(topic: String, task: AmqpTask) = {
    logger.info(s"""Service "$serviceName" registered a task (${task.getClass.getSimpleName}).""")
    tasks += (topic -> task)
  }

  def unregisterTask(topic: String) = {
    tasks -= topic
  }

  def start: Unit = {
    logger.info(s"""Service "$serviceName" starting...[rabbitmq://${AmqpClientConfiguration.user}:${AmqpClientConfiguration.pwd.map(_ => "*").mkString("")}@${AmqpClientConfiguration.host}:${AmqpClientConfiguration.port}]""")
    try {
      scheduler.start()
      onStartup()
    } catch {
      case t: Throwable => {
        logger.error(s"""Service "$serviceName" an error occured during startup. Exiting.""", t)
        sys.exit(-1)
      }
    }
    logger.info(s"""Service "$serviceName" started.""")
  }

  def shutdown = {
    logger.info(s"""Service "$serviceName" is shuting down...""")
    scheduler.stop()
    onShutdown()
    logger.info(s"""Service "$serviceName" exited successfully.""")
  }

  //  def consume(nbMsgToTake: Long): Future[Done] = server.consume("*", nbMsgToTake)
  /**
    * Method intended to be overriden for controllers which want to add initialising stuff
    */
  def onStartup() = {}

  def onShutdown() = {}

}
