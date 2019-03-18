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
import akka.actor.{ActorSystem, Terminated}
import akka.stream.ActorMaterializer
import com.typesafe.scalalogging.LazyLogging
import fr.sictiam.amqp.api.AmqpClientConfiguration
import fr.sictiam.common.ServiceListener

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

/**
  * Created by Nicolas DELAFORGE (nicolas.delaforge@mnemotix.com).
  * Date: 2019-02-26
  */

class AmqpRpcController(val serviceName: String)(implicit val system: ActorSystem, val materializer: ActorMaterializer, val ec: ExecutionContext) extends LazyLogging {

  var listeners = mutable.Buffer[ServiceListener]()
  var tasks = mutable.HashMap[String, AmqpRpcTask]()

  def registerListener(listener: ServiceListener) = {
    logger.info(s"""Service "$serviceName" registered a listener (${listener.getClass.getSimpleName}).""")
    listeners += listener
  }

  def unregisterListener(listener: ServiceListener) = {
    listeners -= listener
  }

  def registerTask(topic: String, task: AmqpRpcTask) = {
    logger.info(s"""Service "$serviceName" registered a task (${task.getClass.getSimpleName}).""")
    tasks += (topic -> task)
  }

  def unregisterTask(topic: String) = {
    tasks -= topic
  }

  def start: Future[Seq[Done]] = {
    logger.info(s"""Service "$serviceName" listening on: [rabbitmq://${AmqpClientConfiguration.user}:${AmqpClientConfiguration.pwd.map(_ => "*").mkString("")}@${AmqpClientConfiguration.host}:${AmqpClientConfiguration.port}]""")
    listeners.foreach(l => l.onStartup())
    val futureStart = Future.sequence(tasks.values.map(t => t.start()).toSeq)
    futureStart onComplete {
      case Success(_) => logger.info(s"""Service "$serviceName" started.""")
      case Failure(error) => logger.error(s"""Service "$serviceName" an error occured during startup. Exiting.""", error)
    }
    futureStart
  }

  def shutdown: Future[Terminated] = {
    logger.info(s"""Service "$serviceName" is shutting down...""")
    listeners.foreach(l => l.onShutdown())
    val futureClosing = system.terminate()
    futureClosing onComplete {
      case Success(_) => {
        tasks.values.foreach(t => t.stop())
        logger.info(s"""Service "$serviceName" exited successfully.""")
      }
      case Failure(error) => logger.error("Shutdown error", error)
    }
    futureClosing
  }

}
