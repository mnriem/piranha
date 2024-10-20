# Testing with JUnit 5 and Playwright

If you are looking to use Piranha Web Profile with JUnit 5 and Playwright
then this guide will setup you up quickly!

In 8 steps you will learn how to do so. They are:

1. Create the Maven POM file
1. Add the helloplaywright.xhtml page
1. Add the HelloBean.java file
1. Add the web.xml file
1. Add the beans.xml file
1. Add an integration test
1. Test the application
1. Deploy the application

## Create the Maven POM file

Create an empty directory to store your Maven project. Inside of that directory 
create the ```pom.xml``` file with the content as below.

```xml
<?xml version="1.0" encoding="UTF-8"?>

<project
    xmlns="http://maven.apache.org/POM/4.0.0"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <groupId>cloud.piranha.guides.webprofile</groupId>
    <artifactId>playwright</artifactId>
    <version>24.11.0-SNAPSHOT</version>
    <packaging>war</packaging>
    <name>Testing with JUnit 5 and Playwright</name>
    <properties>
        <jakartaee.version>10.0.0</jakartaee.version>
        <java.version>21</java.version>
        <junit.version>5.10.0</junit.version>
        <maven-compiler-plugin.version>3.11.0</maven-compiler-plugin.version>
        <maven-failsafe-plugin.version>3.1.2</maven-failsafe-plugin.version>
        <maven-war-plugin.version>3.4.0</maven-war-plugin.version>
        <piranha.distribution>webprofile</piranha.distribution>
        <piranha.version>23.9.0</piranha.version>
        <playwright.version>1.38.0</playwright.version>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>
    <dependencies>
        <dependency>
            <groupId>jakarta.platform</groupId>
            <artifactId>jakarta.jakartaee-web-api</artifactId>
            <version>${jakartaee.version}</version>
            <scope>provided</scope>
        </dependency>
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter-api</artifactId>
            <version>${junit.version}</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter-engine</artifactId>
            <version>${junit.version}</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter-params</artifactId>
            <version>${junit.version}</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>com.microsoft.playwright</groupId>
            <artifactId>playwright</artifactId>
            <version>${playwright.version}</version>
            <scope>test</scope>
        </dependency>
    </dependencies>
    <build>
        <finalName>playwright</finalName>
        <plugins>
            <plugin>
                <groupId>cloud.piranha.maven.plugins</groupId>
                <artifactId>piranha-maven-plugin</artifactId>
                <version>${piranha.version}</version>
                <executions>
                    <execution>
                        <id>pre-integration-test</id>
                        <phase>pre-integration-test</phase>
                        <goals>
                            <goal>start</goal>
                        </goals>
                    </execution>
                    <execution>
                        <id>post-integration-test</id>
                        <phase>post-integration-test</phase>
                        <goals>
                            <goal>stop</goal>
                        </goals>
                    </execution>
                </executions>
                <configuration>
                    <distribution>${piranha.distribution}</distribution>
                </configuration>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>${maven-compiler-plugin.version}</version>
                <configuration>
                    <release>${java.version}</release>
                </configuration>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-failsafe-plugin</artifactId>
                <version>${maven-failsafe-plugin.version}</version>
                <executions>
                    <execution>
                        <goals>
                            <goal>integration-test</goal>
                            <goal>verify</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-war-plugin</artifactId>
                <version>${maven-war-plugin.version}</version>
                <configuration>
                    <failOnMissingWebXml>false</failOnMissingWebXml>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

## Add the helloplaywright.xhtml file

Add the helloplaywright.xhtml file in the `src/main/webapp` directory.

```html
<?xml version='1.0' encoding='UTF-8' ?>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">

<html lang="en"
      xmlns="http://www.w3.org/1999/xhtml"
      xmlns:f="jakarta.faces.core"
      xmlns:h="jakarta.faces.html"
      xmlns:pt="jakarta.faces.passthrough">
    <h:head>
        <title>Jakarta Faces application</title>
        <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
    </h:head>
    <h:body>
        <div>Jakarta Faces application</div>
        <h:form>
                <h:outputText value="#{helloBean.hello}"/>
            <br/>
        </h:form>
    </h:body>
</html>
```

## Add the HelloBean.java file

Add the HelloBean.java file in the `src/main/java/hello` directory.

```java
package hello;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Named;

@Named(value = "helloBean")
@RequestScoped
public class HelloBean {

    private String hello = "Hello Playwright!";

    public String getHello() {
        return hello;
    }

    public void setHello(String hello) {
        this.hello = hello;
    }
}
```

## Add the web.xml file

Add the web.xml file in the `src/main/webapp/WEB-INF` directory.

```xml
<?xml version="1.0" encoding="UTF-8"?>

<web-app version="3.0" xmlns="http://java.sun.com/xml/ns/javaee" 
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" 
         xsi:schemaLocation="http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/web-app_3_0.xsd">
    <servlet>
        <servlet-name>Faces Servlet</servlet-name>
        <servlet-class>jakarta.faces.webapp.FacesServlet</servlet-class>
        <load-on-startup>1</load-on-startup>
    </servlet>
    <servlet-mapping>
        <servlet-name>Faces Servlet</servlet-name>
        <url-pattern>*.xhtml</url-pattern>
    </servlet-mapping>
</web-app>
```

## Add the beans.xml file

Add the beans.xml file in the `src/main/webapp/WEB-INF` directory.

```xml
<?xml version="1.0" encoding="UTF-8"?>

<beans xmlns="http://xmlns.jcp.org/xml/ns/javaee"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xsi:schemaLocation="http://xmlns.jcp.org/xml/ns/javaee http://xmlns.jcp.org/xml/ns/javaee/beans_2_0.xsd"
       bean-discovery-mode="all">
</beans>
```

## Add an integration test

As we want to make sure the application gets tested before we release an 
integration test is added which will be executed as part of the build.

We'll add the integration test to the `src/test/java` directory.

```java
package hello;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

public class HelloIT {
    
    @Test
    public void testHelloPlaywrightXhtml() throws Exception {
        try (Playwright playwright = Playwright.create()) {
            BrowserType chromium = playwright.chromium();
            try (Browser browser = chromium.launch()) {
                Page page = browser.newPage();
                page.navigate("http://localhost:8080/playwright/helloplaywright.xhtml");
                assertTrue(page.content().contains("Hello Playwright!"));
            }
        }
    }
}
```

## Test the application

The application is setup to use JUnit to do integration testing using the 
Piranha Maven plugin so when you are building the application it will also 
execute an integration test validating the web application works.

To build and test the application execute the following command:

```bash
  mvn install
```

## Deploy the application

To deploy your application you will need 2 pieces. 

1. The Piranha Web Profile runtime JAR.
2. The WAR file you just produced. 

For the WAR file see the `target` directory. For the Piranha Web Profile
distribution go to Maven Central. And then the following command line will
deploy your application:

```bash
  java -jar piranha-dist-webprofile.jar --war-file playwright.war
```

## Conclusion

As you can see using JUnit 5 and Playwright with Piranha Web Profile is easy!

