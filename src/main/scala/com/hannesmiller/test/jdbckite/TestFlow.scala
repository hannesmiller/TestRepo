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
        .threads(5, 10)
        .split()
        .body()
        .streaming()
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
        .to("direct:dataSource1")
        .setBody(simple("select * from person where id in (select max(id) from person)"))
        .to("direct:dataSource2")
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

      from("direct:dataSource1")
        .process {
          new Processor {
            override def process(exchange: Exchange): Unit = {
              println(Thread.currentThread().getName + " - d1: " + exchange.getIn.getBody)
              Thread.sleep(100L)
            }
          }
        }

      from("direct:dataSource2")
        .process {
          new Processor {
            override def process(exchange: Exchange): Unit = {
              println(Thread.currentThread().getName + " - d2: " + exchange.getIn.getBody)
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

  template.sendBodyAndHeaders("hello there", headers)
  template.sendBodyAndHeaders("hello there", headers)

  Thread.sleep(5000L)
}
