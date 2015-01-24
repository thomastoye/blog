# Slick 2.1 recipes (mainly for Play!2)

## Getting set up

I mainly want to learn Slick to use it with Play!2. This guide will work with Play too, because it includes everything you need to get started (it starts the H2 database in memory, shows when your database needs to evolve with the SQL that will be run, it automatically generates those evolutions, an easy way to start the H2 web viewer, etc.). Keep in mind that I'm still learning, I may make mistakes in these recipes, or just be plain wrong.

Here's how you get ready:

1. Install [Typesafe Activator](http://typesafe.com/get-started)
1. Optional but recommended: add Activator to your `PATH`
1. Start Activator UI (with `activator ui`, or `path/to/activator/bin/activator ui` if you didn't add it to your path, or by clicking on the `activator.bat` file if you're on Windows) and create a new project using the "Play Scala Seed"
1. Configure your `application.conf`: enable the default H2 in-memory database, and add or uncomment this line: `slick.default="models.*"`
1. Add the following lines to your `build.sbt` (not all of them are needed for all of the recipes here):


    "com.typesafe.slick" %% "slick" % "2.1.0",
    "com.typesafe.play" %% "play-slick" % "0.8.0",
    "com.h2database" % "h2" % "1.3.175",
    "mysql" % "mysql-connector-java" % "5.1.21"

You should now reload the project.

## Creating a simple table

Let's say we have a simple need: we want to create a table that tracks products. Each product has an ID, name and a description.

A class for our product would look like this:

    case class Product(id: Long, name: String, description: String)

How do we get from a case class to the database?

    package models
    import play.api.db.slick.Config.driver.simple._
    
    class Products(tag: Tag) extends Table[(Long, String, String)](tag, "MY_PRODUCTS_TABLE") {
      def id = column[Long]("ID", O.PrimaryKey)
      def name = column[String]("NAME")
      def description = column[String]("DESC")

      def * = (id, name, description)
    }

That's our basic table. Let's go over this:

1. Since we're working with the Play Framework, we place models in the `models` package
1. We create a new class that inherits from Table, with a type parameter `(Long, String, String)`. The type paramter is used to indicate what kind of data we want to store.
1. For every column we want to create, we make a method. As we have an id of type Long on our case class, we use `def id = column[Long]("ID", O.PrimaryKey)`. The type parameter here obviously defines what kind of data we want to store. Possible values are Byte, Short, Int, Long, BigDecimal, Float, Double, Boolean, java.sql.Date, java.sql.Timestamp, and some more types. You can find a list [here](http://slick.typesafe.com/doc/2.0.0/schemas.html#tables), under the heading Tables. Then comes the name we want for the column in the database, and finally the options we want. Here, the name of the column is "ID", and this column will be a primary key.
1. There's a method called `*` (pronounced "star"), this is the "star projection". Every table needs this. It's defined on [AbstractTable](http://slick.typesafe.com/doc/2.1.0/api/#scala.slick.lifted.AbstractTable), and it must match the row type (type parameter of the `Table` class).

If you run your Play app, you should see this (if you don't, check your `application.conf`: did you uncomment all the database lines, did you add `slick.default="models.*"`?):

![Database default needs evolution](needs_evolution_1.png)

Slick generated the following SQL from our `Table`:

    create table "MY_PRODUCTS_TABLE" ("ID" BIGINT NOT NULL PRIMARY KEY,"NAME" VARCHAR NOT NULL,"DESC" VARCHAR NOT NULL)

Clicking on "Apply this script now!" will run that script against the database. You can stop Play and open the H2 web viewer by typing `h2-browser` on the sbt/activator console. If you then apply your script, you'll see a new table in there:

![Products table in the H2 browser](products_table_in_h2_browser.png)

## Simple table CRUD

[Relevant documentation on queries](http://slick.typesafe.com/doc/2.1.0/queries.html)

We now have a simple table, but what can we do with it? You might notice there's no methods for data alteration on the `Table` class. Indeed, for data operations, we need an instance of `TableQuery`.

    val tq = TableQuery[Products] // with Products being the Table we defined in the previous heading

We'll start with the easiest methods available on `tq`.

    tq.length.run
    tq.list
    tq.insert((1, "Laptop", "A brand new laptop"))
    tq.filter(id === 1)
    tq.filter(id === 1).update(row => (row.name, row.description).update("Desktop", "A brand new desktop"))
    tq.delete
    tq.filter(id === 1).delete

### Listing all entries (SELECT)

### Inserting a new entry (INSERT)

### Searching entries (SELECT WHERE)

### Updating a entries (UPDATE)

### Deleting entries (DELETE and DELETE WHERE)

## Slick from the REPL

When on the sbt/activator console, you can start an interactive Scala REPL console in the context of your current application by typing the command `console`. When starting a REPL, you first need to start your application and use a Session provider to be able to use the database.

    import play.core.StaticApplication
    new StaticApplication(new java.io.File("."))

This starts the application on the console. If you get messages about evolutions, run sbt with `-DapplyEvolutions.default=true` (see also [this blog post](https://playlatam.wordpress.com/2012/04/01/play-framework-2-quicktip-interactively-play-with-your-application-from-the-scala-console/)).

Now, grab a `Session`:

    import models._, play.api.Play.current, scala.slick.jdbc.JdbcBackend.Session, play.api.db.slick.Config.driver.simple._
    
    play.api.db.slick.DB.withSession {implicit session: Session => 
        // your code
    }

Or create a new session and pass that every time you need it (you don't want to use that wrapper all the time in the REPL!):

    val session = play.api.db.slick.DB.createSession
    TableQuery[Products].list(session) // example usage
    TableQuery[Products].insert((1, "name", "description"))(session) // example insert

## Using case classes instead of tuples

    class Products(tag: Tag) extends Table[Product](tag, "MY_PRODUCTS_TABLE") {
      def id = column[Long]("ID", O.PrimaryKey)
      def name = column[String]("NAME")
      def description = column[String]("DESC")

      def * = (id, name, description) <> (Product.tupled, Product.unapply _)
    }

## Intermezzo: `tupled` and `unapply` with case classes

## Classes you'll meet

### The `Table` class

### The `TableQuery` class

### The `ProvenShape` class

## The big O

Remember how we used `O.PrimaryKey` to set a field as a primary key? That `O` is defined [here](http://slick.typesafe.com/doc/2.1.0/api/#scala.slick.profile.RelationalTableComponent$Table) and contains some more interesting things for `JdbcProfile` ([official documentation here](http://slick.typesafe.com/doc/2.1.0/schemas.html#table-rows), scroll down a little):

1. `O.PrimaryKey`: non-compound primary key
1. `O.Default[T](defaultValue: T)`: set a default value
1. `O.DBType(dbType: String)`: use a custom database type (e.g. `DBType("VARCHAR(20)")`)
1. `O.AutoInc`: automatically increment this field
1. `O.NotNull`: this field may not be null
1. `O.Nullable`: this field may be null

Note on `O.NotNull` and `O.Nullable`: you should usually not specify these. Slick will use nullable database fields by default if you use an `Option` type.

## Multiple primary keys

## Mapping columns

## Using Global.scala to inject sample data

1. Enable Global.scala in `application.scala`
1. Override `def onStart(app: Application)`

## What's "lifted embedding"? What's "direct embedding"?

### Lifted embedding

### Direct embedding

## Connecting to a different database engine

### MySQL

### Supported engines
