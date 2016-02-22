# Welcome to Hub Cap
The GitHub Organization Analyzer

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
