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

import java.util.concurrent.Executors

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import fr.sictiam.amqp.api.controllers.AmqpController

import scala.concurrent.ExecutionContext

/**
  * Created by Nicolas DELAFORGE (nicolas.delaforge@mnemotix.com).
  * Date: 2019-03-12
  */

object FakeAmqpController extends App {

  implicit val system = ActorSystem("FakeAmqpControllerSystem")
  implicit val materializer = ActorMaterializer()
  implicit val ec: ExecutionContext = new ExecutionContext {
    val threadPool = Executors.newFixedThreadPool(10)

    def execute(runnable: Runnable) {
      threadPool.submit(runnable)
    }

    def reportFailure(t: Throwable) {}
  }

  val controller = new AmqpController("testExchange", "Fake Controller")
  controller.registerTask("graph.create", new FakeCreateTask)
  controller.registerTask("graph.read", new FakeReadTask)
  controller.registerTask("graph.update", new FakeUpdateTask)
  controller.registerTask("graph.delete", new FakeDeleteTask)

  controller.start

  sys addShutdownHook {
    controller.shutdown
  }

}
