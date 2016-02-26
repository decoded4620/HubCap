# Welcome to Hub Cap
The GitHub Organization Analyzer

This tool will help you discover the most widely used and popular GitHub repositories, along with a search interface to help you narrow it down. The tools is a Java Executable, threaded, and provides several api functions to help you find the best of the best. 

the tool supports a REPL (Read-Evaluate-Print-Loop) mode which will allow the user to spawn new searches, and repeat old searches.
Searches can be run in parallel.

### Environment
#### System Variables
##### M2
The Maven Bin directory (i.e. C:\Apache\maven-3.3.9\bin)

##### M2_HOME
The Maven Installation directory (i.e. C:\Apache\maven-3.3.9\)

##### PATH
includes `%M2%`

#### Software
* Maven 3.3.9
* Ruby 2.4.5.1
* Travis 1.8.2
* Eclipse "Mars"
* Git (Windows 64) https://git-scm.com/download/win
* GPG (Windows) https://www.gpg4win.org/download.html

##### Verifying Signed Package
gpg --keyserver hkp://pgp.mit.edu --recv-keys 4EB44968

#### Basic Testing
Using JUnit Test Cases and Maven SureFire Plugin

#### NOTE ON CI
Cannot use Travis CI as I don't currently use a paid plan, and the requirement for this Repository is to be private. Thus I cannot use the ssh key to implement true CI. If the repository was public that may be different.

I've included a default travis yml for sport. 


### Usage

#### Command Line
Using the Jar from a Linux or Windows Console.

##### Authenticate First!
Setup session long username and password/token for using this tool.

`java -jar hubcap.jar auth yourUserName yourPassOrToken`

You can also add a file to 'resources' called local.json. The content looks like this:

`{ "userName":"yourUserName", "passwordOrToken":"yourPasswordOrToken"}`

HubCap will read this file if available and set the session authentication that way.
You can use a token (rather than a password) if you so choose by visiting: `https://github.com/settings/tokens/new`


##### REPL Mode

Read-Evaluate-Print-Loop, where 'Evaluate' means to take input from the user, and parse the input as new arguments. Each time new arguments are entered a new Search Job is created.
By default, HubCap will fall back into REPL mode, using non-blocking i.o on System.in(). After each Search Job completes. The user can then type parameters at the Command line, and hit Enter, causing the parameters to be evaluated 'on-the-fly'. Should the Search Job take some time, they can run additional queries in the mean time. Search Jobs are all run in parallel, and aggregated in parallel.

You can simply start with no commands and you'll be in REPL by default.


`java -jar hubcap.jar`


##### Default Search Mode

`java -jar hubcap.jar "myOrg" 10`

This will search for the top 10 repositories from 'myOrg' organization. This will crawl every repository for this organization, find all of the pull requests,
and aggregate the complex Pull Information for each.

You can also run parallel searches like this:

`java -jar hubcap.jar org1 10 org2 20 org3 30 ...`

Each pair of arguments represents a new parallel query. Results are aggregated as each job returns data.


##### NO-REPL (One-Off mode)

`java -jar hubcap.jar myOrg 10 --no-repl`

This will execute the program against the organization `myOrg` and list the top `10` repositories sorted by default a default criterion, and then exit.

Normally, without this option, HubCap will fall back to `REPL` mode, allowing the user to continue to enter more searches.

