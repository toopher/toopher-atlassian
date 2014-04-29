# Toopher Seraph Filter

This is the Seraph filter for use with the Toopher Atlassian Plugin.

To build this filter you will need the Atlassian SDK.

    atlas-mvn package

## Installing the Filter
Copy the JAR to your product's `WEB-INF/lib` directory. For example,

    cp toopher-seraph-filter-1.0-SNAPSHOT.jar {$CONFLUENCE_DIR}/confluence/WEB-INF/lib

Or

    cp toopher-seraph-filter-1.0-SNAPSHOT.jar {$JIRA_DIR}/atlassian-jira/WEB-INF/lib

## Connect the filter
Edit your product's `WEB-INF/web.xml`

Add a Toopher filter under the existing `security` `<filter>`.

    <filter>
        <filter-name>toopher-seraph-filter</filter-name>
        <filter-class>com.toopher.integrations.seraph.filter.ToopherSeraphFilter</filter-class>
        <init-param>
            <param-name>TOOPHER_CONSUMER_KEY</param-name>
            <param-value>xxx</param-value>
        </init-param>
        <init-param>
            <param-name>TOOPHER_CONSUMER_SECRET</param-name>
            <param-value>xxx</param-value>
        </init-param>
    </filter>

Then add the Toopher filter-mapping under the existing `security`
`filter-mapping`.

     <filter-mapping>
        <filter-name>toopher-seraph-filter</filter-name>
        <url-pattern>/*</url-pattern>
        <dispatcher>REQUEST</dispatcher>
        <dispatcher>FORWARD</dispatcher>
    </filter-mapping>

Restart Confluence or Jira.

---

You have successfully created an Atlassian Plugin!

Here are the SDK commands you'll use immediately:

* atlas-run   -- installs this plugin into the product and starts it on localhost
* atlas-debug -- same as atlas-run, but allows a debugger to attach at port 5005
* atlas-cli   -- after atlas-run or atlas-debug, opens a Maven command line window:
                 - 'pi' reinstalls the plugin into the running product instance
* atlas-help  -- prints description for all commands in the SDK

Full documentation is always available at:

https://developer.atlassian.com/display/DOCS/Introduction+to+the+Atlassian+Plugin+SDK
