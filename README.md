## Librairie AMQP pour le Hub de Données

API JAVA/SCALA de référence pour la création de modules de traitement connectés au bus de données principal.

Pour déclarer un module AMQP, il est nécessaire d'instancier la classe `AmqpController` et d'enregistrer des tâches qui seront déclenchées à la réception de certains messages.

Par exemple :
```scala

    val exName = "hddExchange"

    val createTask = new AmqpTask {
      override def process(parameter: JsValue)(implicit ec: ExecutionContext): Future[JsValue] = {
      // CREATE TRIPLES INTO THE GRAPH
        Future(JsNumber(Json.stringify(parameter).length))(ec)
      }
    }

    val updateTask = new AmqpTask {
      override def process(parameter: JsValue)(implicit ec: ExecutionContext): Future[JsValue] = {
      // UPDATE TRIPLES INTO THE GRAPH
        Future(JsString("Awesome processing"))(ec)
      }
    }

    val deleteTask = new AmqpTask {
      override def process(parameter: JsValue)(implicit ec: ExecutionContext): Future[JsValue] = {
      // DELETE TRIPLES INTO THE GRAPH
        Future(JsBoolean(true))(ec)
      }
    }
    
    val controller = new AmqpController(exName, "serviceTest")
    controller.registerTask("graph.create.triples", createTask)
    controller.registerTask("graph.update.triples", updateTask)
    controller.registerTask("graph.delete.triples", deleteTask)
    
    controller.start

``` 