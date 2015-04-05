title: Continuous deployment with Play! Framework 2
date: 2015/03/27 16:00:00
---

In a [previous blog post](/2015/play-framework-ci/), I discussed setting up Jenkins for continuous integration with Play. But today will go one step further: we will contiously deploy our application.

## A high-level overview

In this article, we will set up a Digital Ocean droplet with [Dokku](https://github.com/progrium/dokku), a project that brings a Heroku-like PaaS server to your own systems. This means that you can just "push to deploy": when you push a new commit, it gets deployed on the server.

After we set up Dokku, we will integrate Jenkins and Dokku, for a full pipeline: Jenkins will push to Dokku, which will deploy the application. This is continuous deployment (also known as continuous delivery). We will polish this a bit, so Dokku uses a persistant production database, add a `CHECKS` file so that instances that aren't ready don't go live, and discuss how to use different database types for testing and production.

The end result will be that a single commit starts a whole process: firstly, the code goes live on GitHub, then Jenkins builds and tests the application, then it goes live if the lights are green.

<!-- more -->

## What's Dokku?

[Dokku](https://github.com/progrium/dokku) is a project that aims to bring a Heroku-like PaaS system to your own servers. It uses a few components, namely [Docker](https://www.docker.com/). You can watch [this screencast](https://vimeo.com/68631325) for a quick introduction to Dokku. Docs can be found [here](http://progrium.viewdocs.io/dokku/index).

## Setting up Dokku

Once again, we'll be using Digital Ocean. This time we'll create a droplet, but instead of deploying a distribution, we'll deploy an Application. Select the _Applications_ tab, and choose _Dokku_.

As the SSH key, select your personal SSH key (the one you use to SSH into remote computers).

Go ahead and create the droplet.

![Creating a Dokku droplet](create-dokku-droplet.png)
![Setting up a droplet with an SSH key](create-with-ssh-key.png)
![A new Dokku droplet has been set up](new-dokku-droplet.png)

## Dokku config

Go to the IP address assigned to the new droplet, you can find that on the details page. It asks you for an SSH key again. We'll create a new keypair in the next section. Later in this tutorial, we will set up Jenkins to push to Dokku. That means we will have to deploy our private key on Jenkins. Obviously, we don't want to use our main key for that.

![Dokku's setup page](dokku-empty-setup.png)

### Generating a new keypair

We will create a new key pair, and add it to our SSH agent.

    $ ssh-keygen
    Generating public/private rsa key pair.
    Enter file in which to save the key (/home/thomas/.ssh/id_rsa): /home/thomas/.ssh/id_rsa_dokku
    Enter passphrase (empty for no passphrase): 
    Enter same passphrase again: 
    Your identification has been saved in /home/thomas/.ssh/id_rsa_dokku.
    Your public key has been saved in /home/thomas/.ssh/id_rsa_dokku.pub.
    The key fingerprint is:
    4c:60:3a:29:ae:29:4e:0d:b6:ac:9d:4d:4f:cc:4e:e0 thomas@mybox
    The key's randomart image is:
    +---[RSA 2048]----+
    |      o          |
    |     + .         |
    |  . +   .        |
    | . . . o         |
    | o. .   S        |
    |oo+. +           |
    |++ .E =          |
    |=. + =           |
    |..o . o          |
    +-----------------+
    $ ls .ssh
    id_rsa id_rsa.pub id_rsa_dokku  id_rsa_dokku.pub  known_hosts
    $ ssh-add ~/.ssh/id_rsa_dokku
    $ ssh-add ~/.ssh/id_rsa_dokku
    Identity added: /home/thomas/.ssh/id_rsa_dokku (rsa w/o comment)

You might have to restart your SSH agent after adding the key, it should show up with a `ssh-add -L`.

Add the public part of the key to the Dokku configuration:

![Adding the public key to the Dokku initial configuration](dokku-finish-setup.png)

You can now finish the setup, we won't set up VHOSTs here, but that should be easy.

## Adding a remote

Dokku works by connecting a few pieces together. The entry piece is [git-receive](https://github.com/progrium/gitreceive), it accepts git pushes on the fly. What this means is that you can `git push` to your new Dokku droplet.

    git remote add <remote-name> dokku@<your-droplet>:{app-name}

For example:

    git remote add prod dokku@178.62.255.127:speelsysteem

* _remote-name_, in this case `prod`, is the name of the remote. It's what you use when `git push`ing, e.g. `git push prod master`
* _your-droplet_, in this case `178.62.255.127`, is the hostname or IP of your Dokku droplet
* _app-name_, in this case `speelsysteem`, is a magic name that Dokku will use for the new app. It can be anything you want.

## Testing it out

Try out `git push dokku master`. This may take a very long time the first time (since the Ivy cache is empty), you should see something like this:

    $ git remote add dokku-test dokku@128.199.36.181:my-new-app
    $ git push dokku-test master
    The authenticity of host '128.199.36.181 (128.199.36.181)' can't be established.
    ECDSA key fingerprint is 37:3a:c5:04:b1:c5:e8:56:5e:64:55:0e:47:49:ef:97.
    Are you sure you want to continue connecting (yes/no)? yes
    Warning: Permanently added '128.199.36.181' (ECDSA) to the list of known hosts.
    Counting objects: 1136, done.
    Delta compression using up to 8 threads.
    Compressing objects: 100% (610/610), done.
    Writing objects: 100% (1136/1136), 157.87 KiB | 0 bytes/s, done.
    Total 1136 (delta 673), reused 772 (delta 469)
    -----> Cleaning up...
    -----> Building my-new-app from buildstep...
    -----> Adding BUILD_ENV to build environment...
    -----> Play 2.x - Scala app detected
    -----> Installing OpenJDK 1.6...done
    -----> Downloading SBT...done
    -----> Priming Ivy cache (Scala-2.11, Play-2.3)... done
    -----> Running: sbt update
           [info] Loading global plugins from /tmp/scala_buildpack_build_dir/.sbt_home/plugins
           [info] Updating {file:/tmp/scala_buildpack_build_dir/.sbt_home/plugins/}global-plugins...
           [info] {Resolving xxxxxxxx times 200 cut here}
    -----> Running: sbt compile stage
           [info] {Output from build cut here}
           [success] Total time: 3 s, completed Mar 6, 2015 9:58:28 PM
    -----> Dropping ivy cache from the slug
    -----> Dropping compilation artifacts from the slug
    -----> Discovering process types
           Procfile declares types -> web
    -----> Releasing my-new-app...
    -----> Deploying my-new-app...
    -----> Running pre-flight checks
           check-deploy: /home/dokku/my-new-app/CHECKS not found. attempting to retrieve it from container ...
           CHECKS file not found in container. skipping checks.
    -----> Running post-deploy
    unsupported vhost config found. disabling vhost support
    =====> Application deployed:
           http://128.199.36.181:49153

    To dokku@128.199.36.181:my-new-app
     * [new branch]      master -> master


When going to the URL shown in _post-deploy_, you should see your new application. If you don't, check out the following section on troubleshooting.

### Troubleshooting

Here are the most common errors:

* Not enough RAM (`Error occurred during initialization of VM: Could not reserve enough space for object heap!`): add a swapfile or swap partition; see [this guide](https://www.digitalocean.com/community/tutorials/how-to-add-swap-on-ubuntu-14-04)
* Not being able to push (`Permission denied (publickey). fatal: The remote end hung up unexpectedly`): you made a mistake with your SSH keys. Did you add the right key to Dokku? Did you add the key locally to the agent with `ssh-add`?
* Timeouts (`gzip: stdin: unexpected end of file` , `tar: Child returned status 1`, `tar: Error is not recoverable: exiting now`): probably curl is timing out. You'll have to set the `CURL_TIMEOUT` environment variable to something higher. See [here](https://github.com/progrium/dokku/issues/763) and [here](http://progrium.viewdocs.io/dokku/configuration-management) for more information.

When troubleshooting, try these commands (ssh into the server and run them there):

    dokku logs {app-name}
    docker logs -f `cat /home/dokku/{app-name}/CONTAINER` # follows the output live (like tail -f)

## Using a database

### Installing Postgres

We obviously don't want to use the H2 database in production. I use [Slick](http://slick.typesafe.com/) for database access, so I'll go with [PostgreSQL](http://www.postgresql.org/). Be sure to have `"postgresql" % "postgresql" % "9.1-901.jdbc4"` in your `libraryDependencies`. A MariaDB plugin for Dokku is available [here](https://github.com/Kloadut/dokku-md-plugin), but I haven't used it.

Install [Postgres as a plugin for Dokku](https://github.com/Kloadut/dokku-pg-plugin):

    # ssh root@<dokku IP>
    # cd /var/lib/dokku/plugins
    # git clone https://github.com/Kloadut/dokku-pg-plugin postgresql
    # dokku plugins-install
    # sudo apt-get install postgresql-client-9.3 # if you want to backup/restore databases

Now create a new database:

    root@speel-dokku:~# dokku postgresql:create <same-name-as-app-name>

    -----> PostgreSQL container created: postgresql/speelsysteem

           Host: 172.17.42.1
           Port: 49158
           User: 'root'
           Password: 'passwordhere'
           Database: 'db'

           Url: 'postgres://root:passwordhere@172.17.42.1:49158/db'

The name should be the same as your app's name. You need not note the password, all of the information will be provided in an environment variable.

### Making Play use the Postgres database

Edit your `application.conf` and remove or uncomment all `db.default` entries. Then, add `db.default.url` with a value of `${DATABASE_URL}` and the PostgreSQL driver:

    db.default.driver=org.postgresql.Driver
    db.default.url=${DATABASE_URL}

The `${DATABASE_URL}` will be substituted by the value of the `DATABASE_URL` environment variable that will be set by Dokku. No username or password needed, they will be supplied in the database URL.

### Starting locally

I run Postgres locally too. You can start an sbt shell (or [Typesafe Activator](http://typesafe.com/get-started)) with `sbt -DDATABASE_URL=postgres://<postgres-user>:<postgres-password>@127.0.0.1:5432/<database-name>` (The double _D_ is not a typo). For example:

    sbt -DDATABASE_URL=postgres://speelsysteem_local:speelsysteem_local@127.0.0.1:5432/speelsysteem_local

or

    export DATABASE_URL=postgres://speelsysteem_local:speelsysteem_local@127.0.0.1:5432/speelsysteem_local
    sbt

I do this because I want to make sure I don't run into problems. Early on, I stumbled over the generated evolutions: they were generated for H2 locally, and when pushing to Dokku, Postgres would complain since it didn't understand some H2-specific statements.

### Applying evolutions

We want to automatically apply evolutions on the production server, but not locally. We can use the following `Procfile`:

    web: target/universal/stage/bin/{app-name} -Dhttp.port=${PORT} -DapplyEvolutions.default=true

Save this in a file called `Procfile` (note the uppercase _P_) in the root of your project.

### Troubleshooting

`dokku logs {app-name}` is your best friend here. The app may fail at start-up due to not being able to access the database, in which case you'll get _Unable to connect_ in your browser. With `dokku logs` you'll quickly see why it failed.

A `dokku ps:restartall` might help in some cases, `dokku ps {app-name}` and `docker ps -a` can help to figure out what is running.

### Using different databases (H2 locally, production database on Dokku)

Now that our app works with Postgres both locally and on Dokku, it's time to spice things up. I'd like to use H2 locally, so that I don't need to deploy Postgres on every dev box.

Open up `application.conf` again, and we will use an environment variable for the driver too:

    # Database configuration
    db.default.driver=${DATABASE_DRIVER}
    db.default.url=${DATABASE_URL}

Try it out locally with `sbt -DDATABASE_URL=<JDBC URL here> -DDATABASE_DRIVER=org.postgresql.Driver run`. It should still work.

Now try it with `sbt -DDATABASE_URL=jdbc:h2:mem:play -DDATABASE_DRIVER=org.h2.Driver`: that should work too!

### Preparing for Dokku

* Make sure your `application.conf` sources the database driver and url from environment variable (as in the step above)
* Remove the auto-generated evolutions (if you use them) from version control (if you store them in version control). If you write your own evolutions, you'll need to make sure they work with both H2 and Postgres: take a look at the _MODE_ parameter for H2 [here](https://www.playframework.com/documentation/2.3.0/Developing-with-the-H2-Database).
* Update the `Procfile`: set `DATABASE_DRIVER` to `org.postgresql.Driver`, should look like this:


    web: target/universal/stage/bin/speelsysteem -Dhttp.port=${PORT} -DapplyEvolutions.default=true -DDATABASE_DRIVER=org.postgresql.Driver

## Integrating with Jenkins

In the previous post, we set up Jenkins to build and test our application. Now we will update Jenkins to pull the latest code on changes, test it with the H2 database, and push to Dokku if the tests succeed.

### Make Jenkins use the H2 in-memory database for testing

If you pushed after updating your `application.conf`, you might see that your build on Jenkins failed with a slew of _Configuration error[./conf/application.conf: Could not resolve substitution to a value: ${DATABASE_DRIVER}]_. That's because we didn't set the environment variables, obviously. There's few ways you could solve this: export these variables in `~jenkins/.profile`, set global Jenkins options, or per-project configuration of environment variables. The first option will set the environment for everything the `jenkins` user uses (not what we want), the second option will set the environment variable for all project this Jenkins instance manages (not recommend) and the last option will require us to install an extra plugin. Let's do that!

I use the [Environment Injector Plugin](https://wiki.jenkins-ci.org/display/JENKINS/EnvInject+Plugin) to manage per-project environment variables. Install it with Jenkins, restart Jenkins, and configure your project. Go to the new section called _Build Environment_ and place your environment variables here:

    DATABASE_URL=jdbc:h2:mem:play
    DATABASE_DRIVER=org.h2.Driver

![The Environment Injector Plugin, installed in Jenkins](env-injector-plugin.png)
![The Environment Injector Plugin, installed in Jenkins](env-injector-plugin-variables.png)


(Note that we could set default values in `application.conf` if the environment variables aren't set. That way, we wouldn't have to specify we want to use 
H2 everytime we run locally, and we wouldn't have to edit our project on Jenkins. I'm not a fan of this option: if you accidently have a production server with the environment variables not set, it would use H2 as a fall-back, without reporting anything. That's why I choose for explicit `DATABASE_*` environment variables, but feel free to do otherwise.)

### Push to production when tests succeeded

In this section, we will make Jenkins push to Dokku.

#### Install the SSH Agent Plugin

We're going to make it easy for ourselves with a Jenkins plugin called _SSH Agent Plugin_. Install, restart, you now this by now.

![Installing the SSH Agent Plugin for Jenkins](ssh-agent-plugin.png)

#### Add the Dokku private key

Take the private key of the keypair we installed on Dokku. In Jenkins, go to _Configure_ > _Credentials_ (global config, not the config for your project). Add a new credential for the SSH key, optionally in a domain. You can find the private key in `~/.ssh/id_rsa_dokku`.

![Adding a private key to Jenkins](private-key-jenkins.png)

#### Set up the new repository for your project

Go to your build configuration, to the _Source Code Management_ section, and click on add.

* As the URL, use _dokku@<droplet IP>:{app-name}_
* Select the new SSH key we created to be able to push to Dokku
* As name, choose _dokku_
* The _Branches to build_ option is either broken or behaves very weirdly. I could only get it working with the following: `refs/remotes/origin/master`. Anything else and it will pull a commit from Dokku and build that, not what we want

![Adding a new git repository to the build](jenkins-git-repos.png)

#### Push when the build succeeds

Go to the post-build actions and add a new one, choose _Git Publisher_ with some sane options, like _Push only if build succeeds_ and push the master branch. Make sure the repository is `dokku`, this should match the name you gave your repository in the step above. You might want to turn on force pushing if you committed things to Dokku directly without committing to GitHub.

![Add a new post-build action to push to Dokku](git-publisher.png)

## CHECKS

Your Play application might take some time to start up. Dokku will switch all traffic over to the new application as soon as possible however, and that might result in some HTTP errors. Luckily, there's a mechanism to prevent switching to the new app before it's ready: `CHECKS`. You can read more about it on [this page](http://progrium.viewdocs.io/dokku/application-deployment#zero-downtime-deploy), under the heading _Zero downtime deploy_.

### Creating a heartbeat

I'll use a very simple check: I'll create a `/heartbeat` route that will just return `online`. Dokku can then request `/heartbeat`, when it gets `online` back, it knows that this instance is up and ready to accept new requests.

Add a new route to `conf/routes`:

    GET     /heartbeat              controllers.Application.heartbeat

Add a controller method:

    object Application {
        // ...
        def heartbeat = Action { Ok("online") }
    }

### Creating a CHECKS file

From the docs:

    A check is a relative URL and may be followed by expected content from the page.

In our case, we want it to look like this:

    /heartbeat          online

### Setting the config

We'll update two configuration variables, `DOKKU_CHECKS_WAIT` and `DOKKU_WAIT_TO_RETIRE`. They do what you expect. Check the [docs](http://progrium.viewdocs.io/dokku/application-deployment) for more details.

We can set those variables with `dokku config:set`

SSH into the dokku machine and `su` to the Dokku user, then run this command:

    dokku config:set {app-name} DOKKU_CHECKS_WAIT=30 DOKKU_WAIT_TO_RETIRE=120

Now push to Dokku (or to GitHub, but my Jenkins server was down so I did it directly :)):

    $ git push dokku
    [output cut]
    ...
    -----> Running pre-flight checks
           check-deploy: /home/dokku/speelsysteem/CHECKS not found. attempting to retrieve it from container ...
           CHECKS file found in container
    Waiting 30 seconds ...
    checking with: curl -q --compressed --fail --location --max-time 30 http://127.0.0.1:49193/heartbeat
    remote:   % Total    % Received % Xferd  Average Speed   Time    Time     Time  Current
    remote:                                  Dload  Upload   Total   Spent    Left  Speed
    remote: 100     6  100     6    0     0      9      0 --:--:-- --:--:-- --:--:--     9
    http://127.0.0.1:49193/heartbeat => "online"
    All checks successful!
           removing CHECKS file copied from container
    -----> Running post-deploy
    -----> NO_VHOST config detected
    -----> Shutting down old container in 60 seconds
    =====> Application deployed:
           http://178.62.255.127:49193

    To dokku@178.62.255.127:speelsysteem
       e44bb7f..a4009c2  master -> master


### Enable VHOSTS (apps as sub-domains)

The easy way is to reinstall Dokku. It's also possible to get to enable them after creation, but I wouldn't recommend it (I just spent a few hours figuring it out). Firstly, make sure your DNS is set correctly. Then, set the hostname of your droplet to your domain. Add a `~dokku/VHOST` file containing your domain. Add a domain using `dokku domains:add`. Now destroy your apps (no amount of rebuilding nginx config will do, you really need to delete them). Then create them again. I got into trouble with Postgres, and had to manually restore the database (evolutions wouldn't work).

Seriously though, you're better off just reinstalling Dokku as you have to remove your apps anyways. It gives you the added benifit of starting with a clean server.

## Wrapping up

That concludes my first two blog posts in a while. This was the first time I used Jenkins, and while it didn't have the nicest interface, I found it to be pleasant and very flexible. Dokku is a dream to use for those of us who'd rather not dabble with system administration stuff. This pipeline took me a while to figure out, but in the end it was well worth it. I'm still amazed that a single commit now kicks off this entire process!

## Further reading

* [Deploy Scala and Play Applications to Heroku from Jenkins CI](https://devcenter.heroku.com/articles/deploy-scala-and-play-applications-to-heroku-from-jenkins-ci), if you want to deploy to Heroku
* [Continuous Deployment with Dokku (Full Guide)](http://blog.thesparktree.com/post/88872191934/continuous-deployment-with-dokku-full-guide)
