# Toopher Atlassian Plugin

To build this plugin you will need the Atlassian SDK. Then, simply
execute the following command from this directory.

    atlas-mvn package

You can then find a `toopher-atlassian` JAR in your `target` directory.

The plugin was created to work for both Jira and Confluence, however, if
you run into an issue, you can try building specifically for Jira or
Confluence.

To build the package for Jira

    atlas-mvn -f jira-pom.xml package

To build the package for Confluence

    atlas-mvn -f confluence-pom.xml package

---

# Toopher Confluence Integration
Installing Toopher Atlassian in Confluence.

* Install `toopher-atlassian` by uploading the JAR file in Confluence's plugin manager.
* Copy the dependent JARs and the Toopher Seraph Filter JAR to your `{$CONFLUENCE_DIR}/confluence/WEB-INF/lib` directory.
* Create a filter in `{$CONFLUENCE_DIR}/confluence/WEB-INF/web.xml`
* Hook up the filter by creating a filter-mapping, also in `{$CONFLUENCE_DIR}/confluence/WEB-INF/web.xml`
* Restart Confluence

---

# Toopher Jira Integration
Installing Toopher Atlassian in Jira.

* Install `toopher-atlassian` by uploading the JAR file in Jira's plugin manager.
* Copy the dependent JARs and the Toopher Seraph Filter JAR to your `{$JIRA_DIR}/atlassian-jira/WEB-INF/lib` directory.
* Create a filter in `{$JIRA_DIR}/atlassian-jira/WEB-INF/web.xml`
* Hook up the filter by creating a filter-mapping, also in `{$JIRA_DIR}/atlassian-jira/WEB-INF/web.xml`
* Restart Jira

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
