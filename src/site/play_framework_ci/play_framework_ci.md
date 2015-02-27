# Continuous intgration for Play! Framework 2 with Digital Ocean

GitHub offers a [student pack](https://education.github.com/pack) to those who are still studying. Among the long list is $100 in credit for [Digital Ocean](https://cloud.digitalocean.com). I took the opportunity to try out continuous integration with [Play! 2](https://www.playframework.com/).

## Creating a new droplet

After registering with Digital Ocean, I applied the discount.

I deployed Ubuntu 14.10 x64 on the cheapest droplet (maybe 32-bit would've been better with the low memory)

![Creating a droplet on Digital Ocean, the smallest size is selected](create-droplet-1.png)
![Creating a droplet on Digital Ocean, Ubuntu 14.10 x64 is selected as operating system](create-droplet-2.png)
![Digital Ocean is creating the new droplet](create-droplet-3.png)
![First login over SSH on this new server](first-login.png)

# Jenkins

## Installing Jenkins

I followed [this guide](https://www.digitalocean.com/community/tutorials/how-to-install-and-use-jenkins-on-ubuntu-12-04) to install it on the new Ubuntu droplet.

Basically:

    # wget -q -O - http://pkg.jenkins-ci.org/debian/jenkins-ci.org.key | apt-key add -
    # echo deb http://pkg.jenkins-ci.org/debian binary/ > /etc/apt/sources.list.d/jenkins.list
    # apt-get update
    # apt-get install jenkins git unzip openjdk-8-jdk # also grab git, OpenJDK 8 and unzip here
    # service jenkins start # or restart

![Adding Jenkins PGP key and the repository](installing-jenkins-1.png)
![Installing Jenkins and needed software](installing-jenkins-2.png)
![Starting the Jenkins service](installing-jenkins-3.png)

And then Jenkins will be running on the IP of your droplet, port 8080.

![Jenkins first run](jenkins-first-run.png)

Follow the security as described in the guide mentioned above.

![Add security](jenkins-first-run-security.png)

I also installed JDK 8u31, very nice that Jenkins can do this for me (I had to create an Oracle account though).

## Install Scala and sbt

Based on [this](https://gist.github.com/visenger/5496675), we'll now install Scala and sbt.

    # wget http://www.scala-lang.org/files/archive/scala-2.11.5.deb
    # dpkg -i scala-2.11.5.deb
    # rm scala-2.11.5.deb
    # wget http://dl.bintray.com/sbt/debian/sbt-0.13.7.deb
    # dpkg -i sbt-0.13.7.deb
    # rm sbt-0.13.7.deb

I later found out that sbt has a repository, you could just as well do this:

    # echo "deb https://dl.bintray.com/sbt/debian /" > /etc/apt/sources.list.d/sbt.list
    # apt-get update
    # apt-get install sbt

![Installing scala](install-scala-sbt-1.png)
![Installing sbt](install-scala-sbt-2.png)

## Setting up swap

I choose the smallest instance, which would soon run out of memory. Therefore, I added a 4G swap file, by following [this guide](https://www.digitalocean.com/community/tutorials/how-to-add-swap-on-ubuntu-14-04).


![Creating a 4G swap file](swap.png)

## Testing your commands out

Create a new, non-root user, add him to the `sudo` group, and `su`:

    # adduser thomas
    # usermod -a -G sudo thomas
    # su thomas

Go to the new home directory and try out your build to see if everything was installed correctly:

    $ cd ~
    $ mkdir tmp
    $ cd tmp
    $ git clone https://github.com/thomastoye/speelsysteem.git
    $ cd speelsysteem
    $ sbt compile # if this is the first time, the Ivy cache will be empty and this might take a while

![Creating a new non-root user](new-user-1.png)
![Trying to build manually](new-user-2.png)

## Installing plugins for Jenkins

Go to _Manage Jenkins_ > _Manage plugins_ and install the following plugins:

* [sbt plugin](https://wiki.jenkins-ci.org/display/JENKINS/sbt+plugin)
* [git plugin](https://wiki.jenkins-ci.org/display/JENKINS/Git+Plugin)
* [GitHub plugin](https://wiki.jenkins-ci.org/display/JENKINS/GitHub+Plugin)

Click on _Install without restart_. Check the _Restart Jenkins when installation is complete and no jobs are running_ checkmark and wait a few minutes.

Now we need to configure the sbt plugin (no configuration is needed for the git plugin since we already installed git) by following the steps [here](https://wiki.jenkins-ci.org/display/JENKINS/sbt+plugin):

Go to _Manage_ > _Configure Jenkins_, then scroll down to the sbt section. Click on `Add` and enter the following path: `/usr/share/sbt-launcher-packaging/bin/sbt-launch.jar`. This is assuming that you installed the deb package or used the package manager to install sbt. As the name, I choose "sbt managed by package manager". Then save the page.

![Navigating to Manage Jenkins](install-plugins-1.png)
![Navigating to Manage Plugins](install-plugins-2.png)
![Installing the sbt plugin](install-plugins-3.png)
![Installing the git plugin](install-plugins-4.png)
![Installing the GitHub plugin](install-plugins-5.png)
![Jenkins is busy installing the plugins](install-plugins-6.png)
![Jenkins will restart after installing all plugins](install-plugins-7.png)
![Where to set up sbt](sbt-setup-1.png)
![How to set up sbt](sbt-setup-2.png)

## Set the JDK


    $ sudo update-alternatives --config java
    There are 2 choices for the alternative java (providing /usr/bin/java).

      Selection    Path                                            Priority   Status
    ------------------------------------------------------------
    * 0            /usr/lib/jvm/java-7-openjdk-amd64/jre/bin/java   1071      auto mode
      1            /usr/lib/jvm/java-7-openjdk-amd64/jre/bin/java   1071      manual mode
      2            /usr/lib/jvm/java-8-openjdk-amd64/jre/bin/java   1069      manual mode

    Press enter to keep the current choice[*], or type selection number: 2
    update-alternatives: using /usr/lib/jvm/java-8-openjdk-amd64/jre/bin/java to provide /usr/bin/java (java) in manual mode
    $ sudo update-java-alternatives -s /usr/lib/jvm/java-1.8.0-openjdk-amd64

I choose to use OpenJDK 8, because in some projects I make heavy use of lambdas and streams.

![Setting the default JDK](set-jdk.png)

Now, set the JDK in Jenkins. Go to _Configure_ > _Manage Jenkins_, and click on _JDK Installations..._. Click on _Add JDK_. Uncheck _Install automatically_ and choose `/usr/lib/jvm/java-1.8.0-openjdk-amd64` as `JAVA_HOME`. Give it a good name ("Java 8 (OpenJDK)" ought to do it).

![Where to add the JDK](add-jdk-1.png)
![Adding OpenJDK](add-jdk-2.png)

## A first Jenkins job

Now that we have all of the configuration out of the way, let's create the job.

From the dashboard, create a new job

Give it an appropriate name, and choose "freestyle project".

* Under GitHub project, use `https://github.com/user/project.git`, for example `https://github.com/thomastoye/speelsysteem.git`
* Select the JDK we set up
* Choose git as version control
    * Enter the name of your repository. I'll be using `https://github.com/thomastoye/speelsysteem.git`. Note that we still have to enter this, even though we configured GitHub above.
    * No credentials are needed for public projects
    * I want to build all branches, so I blank the _Branch specifier_
* Select _Build when a change is pushed to GitHub_ under _Build triggers_
* Add a _Build step_. Choose `Build using sbt`
    * Select the sbt launcher we made earlier
    * Fill in the sbt tasks you want to run. I'll just use `compile` for now

Now click _Build now_. You'll get a notification that a build is scheduled. The first build can take a while. The Ivy cache for the user `jenkins`, which was created when installing Jenkins and is used to run it, is empty. And we told it to build all branches, too.

![Creating a new freestyle project](new-project-1.png)
![Setting the GitHub project](jenkins-job-1.png)
![Setting up how to pull from git](jenkins-job-2.png)
![Setting up to automatically build on GitHub pushes and to build with sbt](jenkins-job-2.png)

## Integrating with GitHub

### Auto-build on commit

We have a build trigger that is set to _Build when a change is pushed to GitHub_, but we need to do one more thing before this works. Jenkins needs to know when a change is pushed, and for that we can use GitHub webhooks. Instead of managing them ourselves, we will offload this to the GitHub plugin for Jenkins.

Now go to [GitHub](https://github.com) and go to [_Settings_ > _Applications_ > _Personal access tokens_ > _Generate new token_](https://github.com/settings/tokens/new). Select the following and generate the token:

* `repo`
* `repo:status`
* `write:repo_hook`
* `admin:repo_hook`
* `admin:org_hook`

Unfortunately, the documentation doesn't mention what scopes the application needs, so this is mostly guesswork. `repo:status` will be used in the next section. Copy the token, it will only be shown once.

Go back to Jenkins and go to _Manage Jenkins_ > _Configure system_. Scroll to the bottom and under _GitHub Web Hook_, use the following:

* _Let Jenkins auto-manage hook URLs_
* _Override Hook URL_: leave unchecked
* _API URL_: leave blank
* _Username_: your GitHub username
* _OAuth token_: the token from GitHub.

You can now try `Test Credential`.

Finally, check if the webhook shows up in your project. Go to your project on GitHub, then _Settings_, then _Webhooks and Services_. There should be a _Jenkins (GitHub plugin)_ under services. If not, go to your Jenkins project, click _Configure_, and click _Apply_ without changing anything. The webhook should show up on the GitHub side then.

![Creating a GitHub OAuth token](GitHub OAuth)
![Creating the credential on the Jenkins side](jenkins-oauth-1.PNG)
![The webhook shows up on GitHub](jenkins-oauth-2.PNG)
![Jenkins building a project](busy-building.png)

### Show build status

No doubt that you have seen projects on GitHub that use statuses on branches to indicate which ones built/tested successfully and which ones didn't. Now that we have a continuous integration server in place, we can tell it to give provide GitHub with the statuses of builds.

Go to your project on Jenkins, hit _Configure_ once again and scroll to the bottom. Click on `Add post-build action` and choose `Set build status on GitHub commit`. It doesn't get easier than this!

Click on `Build now`, once the scheduled build is done running, you can check your branches on GitHub. You're probably a little disappointed to only see a green checkmark next to the master branch and nothing next to the others, this will change as soon as you push commits to those branches.

![Set build status](jenkins-job-4.png)

## Testing it out

Create a new branch `jenkins-test`, commit something on it and push. Jenkins should pick it up, build it and update the status on GitHub. Awesome!
