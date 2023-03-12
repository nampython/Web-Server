<h1 align="center">
  <a href="https://github.com/nampython/IoC-Container">
    <img src="slug/HappyFace.svg" alt="Logo" width="125" height="125">
  </a>
</h1>

<div align="center">
  Amazing Project - Feel free to learn!
  <br />
  <br />
  <a href="https://github.com/nampython/IoC-Container/issues/new?assignees=&labels=bug&template=bug_report.md&title=">Report a Bug</a>
  ·
  <a href="https://github.com/nampython/IoC-Container/issues/new?assignees=&labels=enhancement&template=feature_request.md&title=">Request a Feature</a>
  .
  <a href="https://github.com/nampython/IoC-Container/discussions">Ask a Question</a>
</div>




# Web Server
Web Server consists of multiple parts that are all custom implemented. Simplified server but still functional web server platform and by doing that also help to better understand how things such as IoC, HTTP, and Resource handling work under the hood.

## Table of Contents
* [General Information](#general-information)
* [Prerequisites](#prerequisites)
* [Installation and Getting Started](#installationandgettingstarted)

## General Information
A web server is software and hardware that uses HTTP (Hypertext Transfer Protocol) and other protocols to respond to [c](https://www.techtarget.com/searchenterprisedesktop/definition/client)lient requests made over the World Wide Web. The main job of a web server is to display website content through storing, processing, and delivering web pages to users. Apache Tomcat is one of the web servers. Apache Tomcat is a free and open-source implementation of the Jakarta Servlet, Jakarta Expression Language, and WebSocket technologies. It provides a "pure Java" HTTP web server environment in which Java code can also run

It's very hard and complicated to actually know what's behind it. That's why I made this program. As everyone knows apache tomcat has 3 main components which are:

- **Catalina**: Servlet container
- **Jasper**: JSP engine
- **Coyote:** HTTP connector
- **Cluster**: is a load balancer to manage large-scale applications.
- So on …

I try to integrate these components into one and called it as a Web server. Basically, every
 request received by the Web server:

1. Is processed by a RequestProcessor class (on multiple threads). The responsible for this class is to listen for incoming connections on configured TCP ports on the server and forward requests to the ResourceHandler for processing
2. ResourceHandler class is a class to find a suitable resource to back the client.
3. ServletContainer for processing dynamic HTTP requests. It will look for a class with mapping that matches the requested URL and will forward the requested content to the given class.
4. FallbackHandler is last to be called in case of any previous request handler does not intercept.


## Prerequisites
- Having a basic knowledge of the Tomcat server
- Understand the working principle of the IoC container. I made a library based on this principle: [https://github.com/nampython/IoC-Container](https://github.com/nampython/IoC-Container)
- Work with files
- Know about TCP, an HTTP client, and an HTTP server, Session, Cookies


## Installation and Getting Started
Here is a quick teaser of a complete Web Server application in Java:

- The entry point for your application.

```java
import org.nampython.StartServer;

public class App {
    public static void main(String[] args) {
        StartServer.run(8000, App.class);
    }
}
```

- Here are some methods

```java
@Controller("/")
public class Home extends BaseHttp {
    @Override
    protected void doGet(HttpRequest request, HttpResponse response) {
        response.sendRedirect(super.createRoute("/index.html"));
    }
}
```

```java
@Controller("/logged-user/profile")
public class UserProfile extends BaseHttp {
    @Override
    protected void doGet(HttpRequest request, HttpResponse response) {
        if (!request.getSession().getAttributes().containsKey(WebConstants.USERNAME_SESSION_ID)) {
            response.sendRedirect(super.createRoute("/login"));
        } else {
            response.sendRedirect(super.createRoute("/user-details.html"));
        }
    }
}
```

Check out [https://github.com/nampython/TESTSERVER.git](https://github.com/nampython/TESTSERVER.git) to deeply into an example made by this library
