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

##### REPL Mode

`java -jar hubcap.jar -repl`

This will run the program with no parameters putting into a REPL on System.in(). The user can then type parameters at the Command line, and hit Enter, causing the parameters to be evaluated 'on-the-fly'


##### E-REPL Mode

`java -jar hubcap.jar -repl -Dorg=myOrg -DmaxResults=10`

This will execute the program against the organization `myOrg` and list the top `10` repositories sorted by default a default criterion, and then fall back to `REPL` mode, allowing the user to continue to enter more searches. The organization itself is also given a score (for this current session) based on the repositories pulled back from the search.



##### SCAVENGER MODE

`java -jar hubcap.jar -Dquery="jquery in:name" -DmaxResults=50 -DsearchDepth=1000`

This will execute the program and query git hub for the list of the top 50 repositories having 'jquery' in the name, searching a maximum of 1000 repositories on GitHub. If there are less than 50 repositories, all of them are returned, sorted by the default criterion. Note: searchDepths over 1000 must be run in sequential searches, and will take significantly longer. This is the Rate Limit of GitHub.


##### WATCH MODE

`java -jar hubcap.jar -Dorg=myOrg -DmaxResults=10 -watch -Dinterval=20000`

This will execute the program against the organization `myOrg` and list the top `10` repositories sorted by default a default criterion, and then continue to 'watch' this query(i.e. re-running it and updating the local results every 20000  milliseconds). Organization scores are also updated.

