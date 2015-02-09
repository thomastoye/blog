# My annoyances with ASP.NET MVC 5

## Route safety

By far my biggest gripe. There is no reverse routing! I like to know at compile whether or not I made any typos when referencing a route.

I wanted to remove the default index page in a new project. Well, surprise at runtime, because there are these lingering links everywhere that might or might not still link to iti.

### Example

    @Html.ActionLink("Back to list", "Index")

But all methods that generate a link to a route have this API, which I find confusing: you first specify the name of the action, then the name of the controller if you want to use a different one. I have to think every time I need to insert one.

### How can it be improved?

Here's how Play!2 solves this:

All routes are defined in a central `conf\routes` files. Based on this file, a reverse router is generated that is also typesafe.

Snippet of conf/routes
Compiled reverse router class image

Doing it exactly as Play! sacrifices annotation-based routing, that's true. But in principle, this reverse routing class could also be generated from annotations.

## Slow turn-around

I miss hitting refresh in my browser and seeing changes immediately. I even had a live-reload feature in a node.js build that would update my browser automatically when I changed a file! The `debug -> stop -> edit -> start` cycle is way to slow. I would still whine if I had to wait ten seconds, but at the moment I wait some two minutes for a solution with two relatively vanilla ASP.NET MVC projects in it. What is taking it so long?

### How can this be improved?

Make it faster! I don't know how it can be this slow in the first place. Maybe compile in the background while waiting?

## NuGet

I don't know a lot about `NuGet`, but I used a couple of packages and I was disappointed. It seems like packages are gods, they can do whatever they want to your project. They should be self-contained. They should not be able to [open web pages when installing](https://github.com/JamesNK/Newtonsoft.Json/blob/ee170dc5510bb3ffd35fc1b0d986f34e33c51ab9/Build/install.ps1#L3). They should not be able to [execute arbitrary code when installing](http://www.wenda.io/questions/3014489/executing-c-sharp-code-in-nuget-package.html).

As much as I like that they saw the light and brought a package manager to M$ country, I can't be happy with this.

### How can this be improved?

* Use a sane format for managing dependencies. Stop XML! I should not have to use a GUI/CLI utility when I want to CRUD a dependency of my project.
* Don't let packages execute arbitrary code. Do away with `.ps1` files. And fire the guy that came up with it.
* Dependencies should not be applied directly to my project. I install a package, and suddenly there's a slew of new files in folders of **my** project. What the...?

Here's how sbt handles dependency declaration:

    libraryDependencies ++= Seq(
        "org.webjars" % "bootstrap" % "3.1.1-2",
        "com.h2database" % "h2" % "1.3.175",
        "mysql" % "mysql-connector-java" % "5.1.21",
        "joda-time" % "joda-time" % "2.7"
    )

Here's how npm does it:

    {
        // ...
        },
        "dependencies": {
        "express": "~3.3.4",
        "jade": "~0.34.0",
        "nconf": "^0.6.9",
        "winston": "^0.7.3"
        }
    }

npm has a folder in which it stores packages called `node_modules`. Dependencies never leave their folder and never touch my project files.

## Too much magic

I had a method called GetXyz in a Web API Controller. To experiment, I wanted to convert it into a `POST`, so I changed `[HttpGet]` to `[HttpPost]` above the method. And everything worked as it should have worked!

Except it didn't. I'll spare you the head-ache, but ASP.NET automatically makes methods starting with "Get" available as HTTP `GET`s, no matter what you annotate it with. It doesn't even warn you about that, nevermind refusing to compile.

Sure, you say, but you new should never have `POST` method starting with "Get...". Fair point, I was just trying some stuff out, but that's no excuse for ASP.NET silently ignoring my very clear directions.

And that's not the only thing. How about the fact that you can't supply value types in a `POST` body (they have to be part of the query string)? The default HTTP method for MVC controllers is `GET` but for Web API Controllers it's `POST` (?? sure ??)

Convention-based routing is just plain dangerous in my opinion. New method in your controller? Surprise! It's automagically reachable. I can see people making mistakes with this. Oh, security nightmare...

And then there's creating a new project. [29,471 additions](https://github.com/thomastoye/nmct-dropclone/commit/950df0929c9d8fabad7d3049e07ef734080ca061) for a new ASP.NET MVC project. Makes wonder if ASP.NET is a framework or the project scaffold here. Come on, it litters my `Scripts` folder with jQuery and bootrap and some other stuff... I already said it, this is not the place for dependencies. We have [bower](http://bower.io), which manages client-side dependencies (it's not the best, but it works). We have [WebJARs](http://www.webjars.org/), which bundle client-side dependencies in a JAR. But placing them directly in my public folder? Why should I have `bootstrap.min.css` directly under **my** source control? Why should I be responsible for managing those dependencies when tools can do that?

## Error pages

And now for a lighter one. This really is nit-picking, but can we get some more modern error templates please? :)

Image of ASP.NET error page
Image of Play!2 error

It's small things like this that make the dev experience so much better!

## Dynamic views

## Partial views

## Entity Framework black magic

