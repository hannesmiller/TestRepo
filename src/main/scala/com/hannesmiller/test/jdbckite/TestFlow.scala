package com.hannesmiller.test.jdbckite

import java.util

import org.apache.camel.{Exchange, Processor}
import org.apache.camel.builder.RouteBuilder
import org.apache.camel.impl.DefaultCamelContext


object TestFlow extends App {
  val context = new DefaultCamelContext
  context.start()

  val route = new RouteBuilder {
    override def configure(): Unit = {
      from("direct:start")
        .doTry()
        /*
        .process {
          new Processor {
            override def process(exchange: Exchange): Unit = {
              throw new IllegalStateException("Test Exception!!!")
            }
          }
        }
        */
        .setBody(simple("insert into person(firstName, lastName) values('${header.firstName}','${header.lastName}')"))
        .to("direct:dataSource")
        .setBody(simple("select * from person where id in (select max(id) from person)"))
        .to("direct:dataSource")
        .doCatch(classOf[Exception])
        .process {
          new Processor {
            override def process(exchange: Exchange): Unit = {
              println(s"found exception: ${exchange.getIn.getBody()}")
            }
          }
        }
        .doFinally()
        .endDoTry()

      from("direct:dataSource")
        .process {
          new Processor {
            override def process(exchange: Exchange): Unit = {
              println(exchange.getIn.getBody)
            }
          }
        }
    }
  }
  context.addRoutes(route)
  context.start()

  val template = context.createProducerTemplate()
  template.setDefaultEndpointUri("direct:start")
  val headers = new util.LinkedHashMap[String, AnyRef]()
  headers.put("firstName", "hannes")
  headers.put("lastName", "miller")
  template.sendBodyAndHeaders("hello world", headers)


  Thread.sleep(5000L)
}
