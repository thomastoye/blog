title: "Using MacWire DI in an existing Play! Framework 2.3 application"
date: 2015-05-08 18:58:09
tags:
---

I wanted to convert a project of mine to use dependency injection in Play, and I settled on [MacWire](https://github.com/adamw/macwire). I will confess that I don't have a lot of experience with DI, and that it wasn't easy to wrap my head around at first.

![The final product](/2015/using-macwire-di-in-an-existing-play-framework-2-3-application/final.png)

<!-- more -->

# Getting started

## Add the MacWire dependency

In your `build.sbt`, add the following dependency:

    "com.softwaremill.macwire" %% "macros" % "1.0.1"

## Make controllers classes and pass dependencies as constructor parameters

Your controllers probably look a little like this:

    package controllers

    import models.json.AnimatorJson.animatorWrites
    import models.repository.AnimatorRepository
    import play.api.db.slick.DBAction
    import play.api.libs.json.Json
    import play.api.mvc._

    object ApiAnimators extends Controller {
      val animatorRepo = new AnimatorRepository
      
      def allAnimators = DBAction { implicit req =>
        Ok(Json.toJson(animatorRepo.findAll(req.dbSession)))
      }

      def animatorById(id: Long) = DBAction { implicit req =>
        animatorRepo.findById(id)(req.dbSession).fold(BadRequest()) { animator =>
          Ok(Json.toJson(animator))
        }
      }
    }

Not a whole lot to see here: controllers are objects that extend from `Controller` and are in the `controllers` package. We can see a dependency here: `AnimatorRepository`. We want to make this dependency explicit. The way we do that is by passing in through the constructor. Since `object`s have no constructor, we make the controller a class instead:

    class ApiAnimators(animatorRepo: AnimatorRepository) extends Controller {
      def allAnimators = DBAction { implicit req =>
        Ok(Json.toJson(animatorRepo.findAll(req.dbSession)))
      }

      def animatorById(id: Long) = DBAction { implicit req =>
        animatorRepo.findById(id)(req.dbSession).fold(BadRequest()) { animator =>
          Ok(Json.toJson(animator))
        }
      }
    }

## Wire up

Create an object `Application` in the root package. This is where we'll wire up our dependencies.

    import com.softwaremill.macwire._
    import models.repository.ChildRepository
    import controllers.ApiChildren

    object Application {
      lazy val animatorRepo = wire[AnimatorRepository]
      lazy val animatorController = wire[ApiAnimators]
    }


## Update your Global object

If you don't have one yet, create a `Global` object in the package root. Extend `GlobalSettings` with `Macwire`:

    import play.api._
    import com.softwaremill.macwire.{InstanceLookup, Macwire}

    object Global extends GlobalSettings with Macwire {
        val wired = wiredInModule(Application)
        override def getControllerInstance[A](controllerClass: Class[A]) = wired.lookupSingleOrThrow(controllerClass)
    }

## Update your routes

One more thing before our code will compile: updating our routes. It's only a small change though: adding an `@` sign before the controllers that are now instantiated through MacWire:

    # Before
    GET           /api/animator/all         controllers.ApiAnimators.allAnimators
    GET           /api/animator/:id         controllers.ApiAnimators.animatorById(id: Long)
    
    # After                             ---v---
    GET           /api/animator/all        @controllers.ApiAnimators.allAnimators
    GET           /api/animator/:id        @controllers.ApiAnimators.animatorById(id: Long)

Your application should now compile and run fine!

# Testing

Now that we pass an explicit `AnimatorRepository` into the controller, we'll have an easier time testing.

## Extract an interface out of what you want to test

Before we can jump into testing, we need to extract an interface so they can be easily mocked and stubbed. It's not really necessary for what we're going to do in the next section, but you will need this if you have a services layer.

In IntelliJ IDEA, you can go to the class (`AnimatorRepository`) and extract and interface out of it (*Right-click* > *Refactor* > *Extract* > *Trait...*).

The trait I use as an interface now looks like this:

    import models.Animator
    import play.api.db.slick.Config.driver.simple._

    trait AnimatorRepository {
      def findById(id: Long)(implicit s: Session): Option[Animator]
      def findAll(implicit s: Session): List[Animator]
      def insert(animator: Animator)(implicit s: Session): Unit
      def count(implicit s: Session): Int
      def update(animator: Animator)(implicit s: Session): Unit
    }

Notice the name. The implementation will be renamed to `SlickAnimaterRepository`. Don't forget to update your wiring in your `Application.scala`, while the controllers get passed in the trait (yay polymorphism).

Side note: there's a bit of code smell here: the explicit Slick `Session`. I might fix this later, but since the implementation overrides these methods, it needs the same method signatures.

Implementation of the trait:

    class SlickAnimatorRepository extends AnimatorRepository {
      val animators = TableQuery[AnimatorTable]

      override def findById(id: Long)(implicit s: Session): Option[Animator] = animators.filter(_.id === id).firstOption

      override def findAll(implicit s: Session): List[Animator] = animators.list

      override def insert(animator: Animator)(implicit s: Session): Unit = animators.insert(animator)

      override def count(implicit s: Session): Int = animators.length.run

      override def update(animator: Animator)(implicit s: Session): Unit = ??? // omitted
    }

## Tests

Now we can write some tests! First, the setup:

    import java.time.LocalDate

    import controllers.ApiAnimators
    import models.Animator
    import models.repository.AnimatorRepository
    import models.json.AnimatorJson.animatorReads
    import org.junit.runner._
    import org.specs2.mock._
    import org.specs2.runner._
    import play.api.test._

    @RunWith(classOf[JUnitRunner])
    class AnimatorControllerTest extends PlaySpecification with Mockito {
        // test methods go here
    }

Here, we're using specs2 and Mockito. Let's see how we'll write our test methods:

    "Requesting an all animators" should {
      "Return a correct response" in new WithApplication {
        val mockedRepo = mock[AnimatorRepository]
        mockedRepo.findAll(org.mockito.Matchers.any()) returns List(exampleAnimator)

        val animatorController = new ApiAnimators(mockedRepo)

        val result = animatorController.allAnimators.apply(FakeRequest())

        status(result) must be equalTo OK
        contentType(result).map { res => res must be equalTo "application/json" }

        val validated = contentAsJson(result).validate[Seq[Animator]]
        validated.isSuccess must beTrue
        validated.get must be equalTo Seq(exampleAnimator)
      }
    }

What's going on here?

* Notice the `WithApplication`: our controllers needs a running `Application` or it will burn up at runtime with a `There is no started application` message
* We mock the repository using `mock[AnimatorRepository]`. Then we stub a call to `findAll` with a list with a single example
* The stub will catch all calls, thanks to the `Matchers.any()`. You can read more about mocking and stubbing with Mockito [here](https://etorreborre.github.io/specs2/guide/SPECS2-3.6/org.specs2.guide.UseMockito.html).
* Then we get to our assertions: we check the status and content type that is returned. You can find these in [`play.api.test.ResultExtractors`](https://www.playframework.com/documentation/2.3.x/api/scala/index.html#play.api.test.ResultExtractors).
* As the last assert, we read the JSON and try to parse it, then check if it equals what we originally passed in.

That concludes this blog post on using MacWire DI with Play. I hope it helped you to write better code!

# Resources

* [macwire-activator on Github](https://github.com/adamw/macwire-activator): an example project that shows how to use MacWire with Play! 2
* [Dependency Injection in Play! with MacWire](http://www.warski.org/blog/2013/08/dependency-injection-in-play-with-macwire/): this blog post is from 2013 and about MacWire 0.4, still mostly relevant
* [DI in Scala: Guide](http://di-in-scala.github.io/): general introduction to MacWire and DI in Scala
* [Using Scala traits as modules, or the “Thin Cake” Pattern](http://www.warski.org/blog/2014/02/using-scala-traits-as-modules-or-the-thin-cake-pattern/): introduces the "traits as modules" pattern (which wasn't used here), as seen in the Activator template
