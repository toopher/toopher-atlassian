# Toopher Atlassian Plugin

This plugin works for Jira or Confluence. The plugin consists of three components:

* the toopher-java jar (with IFRAME support--version 1.2.0+), which
  should be placed in the `WEB-INF/lib` directory
* the toopher-atlassian plugin, which gets installed through the
  application's plugin manager
* the toopher-seraph-filter jar, which should be placed in the
  application's `WEB-INF/lib`  directory

Installation requires that you install the plugin, configure the filter,
and copy the necessary JARs into the proper location. While this may
sound intimidating, it is quite simple.

## Prerequisites
Atlassian Confluence 5.x or Atlassian Jira 6.x.

Note: the plugin has not been tested against earlier versions of
Confluence or Jira, but it may work for them. Please let us know if you
have an older version of either application that you'd like to protect with
Toopher.

## Toopher Java
Consumed as a jar by `toopher-seraph-filter`. The library contains
code for creating and working with the Toopher API. `toopher-java`
depends on `httpclient`, `httpcore`, `oauth-signpost` and `json`.

## Toopher Atlassian
The plugin itself. This plugin is a simple Java servlet that serves the
Toopher IFRAME used for authentication after the initial
username/password check.

The plugin is enabled or disabled through the application's Plugin manager. 
Note that the plugin requires proper configuration of the Toopher Seraph filter.

## Toopher Seraph Filter
Redirects logins to the Toopher IFRAME and validates responses. If the
IFRAME check passes, the filter proceeds letting users in; otherwise, it
redirects to login.

The filter is enabled by adding a `<filter>` and `<filter-mapping>` entry
to the appropriate `web.xml` (either 
`${CONFLUENCE_INSTALLATION_DIRECTORY}/confluence/WEB-INF/web.xml` or 
`${JIRA_INSTALLATION_DIRECTORY}/atlassian-jira/WEB-INF/web.xml`).

Add the Toopher filter after the existing `security` filter.

    <filter>
        <filter-name>security</filter-name>
        <filter-class>com.atlassian.confluence.web.filter.ConfluenceSecurityFilter</filter-class>
    </filter>

    <!-- insert Toopher filter here -->
    <filter>
        <filter-name>toopher-seraph-filter</filter-name>
        <filter-class>com.toopher.integrations.seraph.filter.ToopherSeraphFilter</filter-class>
        <init-param>
            <param-name>TOOPHER_CONSUMER_KEY</param-name>
            <param-value>yourkey</param-value>
        </init-param>
        <init-param>
            <param-name>TOOPHER_CONSUMER_SECRET</param-name>
            <param-value>yoursecret</param-value>
        </init-param>
    </filter>
    <!-- end of Toopher filter -->

Note: for Jira the `<filter-class>` will point to the Jira class:

    <filter>
        <filter-name>security</filter-name>
        <filter-class>com.atlassian.jira.security.JiraSecurityFilter</filter-class>
    </filter>

After creating the filter, add the Toopher filter mapping below the existing `security`
filter-mapping.

    <filter-mapping>
        <filter-name>security</filter-name>
        <url-pattern>/*</url-pattern>
        <dispatcher>FORWARD</dispatcher>
        <dispatcher>REQUEST</dispatcher>
        <dispatcher>ERROR</dispatcher>
    </filter-mapping>

    <!-- insert Toopher filter-mapping -->
    <filter-mapping>
        <filter-name>toopher-seraph-filter</filter-name>
        <url-pattern>/*</url-pattern>
        <dispatcher>REQUEST</dispatcher>
        <dispatcher>FORWARD</dispatcher>
    </filter-mapping>
    <!-- end of Toopher filter-mapping -->

Note: for Jira the `<filter-mapping>` includes a comment on the
`FORWARD` but is otherwise identical to the Confluence filter shown
above.

    <filter-mapping>
        <filter-name>security</filter-name>
        <url-pattern>/*</url-pattern>
        <dispatcher>REQUEST</dispatcher>
        <dispatcher>FORWARD</dispatcher> <!-- we want security to be applied after urlrewrites, for example -->
    </filter-mapping>


### Configuration options
Most users will not need additional configuration; however, the
following properties can be set in the `web.xml`.

Only `TOOPHER_CONSUMER_KEY` and `TOOPHER_CONSUMER_SECRET` are required.
Exercise caution when editing the other values.

* `TOOPHER_CONSUMER_KEY` - required. Your Toopher credentials retrieved from the 
  [Toopher Dev Site](https://dev.toopher.com/)
* `TOOPHER_CONSUMER_SECRET` - required. Your Toopher credentials retrieved from 
  the [Toopher Dev Site](https://dev.toopher.com/)
* `BASE_URL` - (optional) default: `https://api.toopher.com/v1`. Change this if 
  you use an on-premise Toopher API.
* `OPT_OUT_ALLOWED` - (optional) default: `false`. Change this to `true` if users 
  are allowed to bypass Toopher authentication; they will still be prompted, 
  but will be allowed to enter after failing the Toopher login. This is not 
  recommended.
* `AUTOMATION_ALLOWED` - (optional) default `true`. Change this to `false` if you 
  do not want to allow automation. This is not recommended.
* `CHALLENGE_REQUIRED` - (optional) default `false`. Change this to `true` if 
  each request must also include a pattern lock.
* `TOOPHER_SERVLET_URL` - (optional) default: `/plugins/servlet/toopher` If the 
  Toopher plugin servlet is hosted in a different location, add the proper path 
  here.
* `TOOPHER_RESOURCES_DIRECTORY` - (optional) default: 
  `/download/resources/com.toopher.integrations.atlassian.servlet.toopher-atlassian:resources/`. 
  This location is set by the product (Jira or Confluence) and the Toopher Atlassian plugin.

These values would be added as new `<init-param>`s in the `<filter>`
block. 

These values are read at startup time. See the `init` method in the `toopher-seraph-filter`.

#### Example usage
For example, to disable automation, you would add a new `<init-param>`
named `AUTOMATION_ALLOWED` with a value of `false`:

        <init-param>
            <param-name>AUTOMATION_ALLOWED</param-name>
            <param-value>false</param-value>
        </init-param>

Your complete filter would then look like this:

    <filter>
        <filter-name>toopher-seraph-filter</filter-name>
        <filter-class>com.toopher.integrations.seraph.filter.ToopherSeraphFilter</filter-class>
        <init-param>
            <param-name>TOOPHER_CONSUMER_KEY</param-name>
            <param-value>yourkey</param-value>
        </init-param>
        <init-param>
            <param-name>TOOPHER_CONSUMER_SECRET</param-name>
            <param-value>yoursecret</param-value>
        </init-param>
        <init-param>
            <param-name>AUTOMATION_ALLOWED</param-name>
            <param-value>false</param-value>
        </init-param>
    </filter>

---

## Troubleshooting

Troubleshooting for common issues is presented below.

We would like you to enjoy your Toopher Atlassian experience, so please
contact us (dev@toopher.com) with any issues or suggestions. You can
also [open an issue directly through
GitHub](https://github.com/toopher/toopher-atlassian/issues).

The filter requires access to `toopher-java`, which depends on `httpclient`, `httpcore`, `signpost-commonshttp4`, and `json`. Failure to include these JARs results in exceptions when the filter runs. However, if two versions of the library are present in the Confluence or Jira system (if loaded by Confluence or Jira, other plugins, or placed in `WEB-INF/lib/`), errors may occur.

### Missing JARs
If you are missing the required JARs you may see an error like this:

    2015-01-01 12:34:56,819 INFO [http-8090-6] [atlassian.plugin.manager.DefaultPluginManager] 
    disableDependentPlugins Found dependent enabled plugins for uninstalled plugin 'com.toopher.integrations.atlassian.toopher-atlassian': [].  Disabling...
    2015-01-01 12:34:56,823 INFO [http-8090-6] [atlassian.plugin.manager.DefaultPluginManager] 
    notifyPluginDisabled Disabling com.toopher.confluence.plugins.toopher-confluence


### Doubly Exported JARs
Adding JARs to the `WEB-INF/lib` directory can lead to hard to debug errors if the JAR is already included by the product. The error may look something like this:

    2015-01-01 12:34:56,852 INFO [main] [atlassian.plugin.manager.DefaultPluginManager] 
    init Initialising the plugin system
    2015-01-01 12:34:56,489 WARN [main] [factory.transform.stage.ScanDescriptorForHostClassesStage] 
    execute The plugin 'atlassian-universal-plugin-manager-plugin-2.14.jar' uses a package 'org.apache.commons.fileupload.servlet' that is also exported by the application.  It is highly recommended that the plugin use its own packages.
    2015-01-01 12:34:56,030 WARN [main] [atlassian.soy.renderer.SoyResourceModuleDescriptor] 
    enabled soy-resource is deprecated. Please convert com.atlassian.confluence.plugins.soy:soy-core-functions to a web-resource and/or a soy-function

### Uninstalling the Plugin Without Disabling the Filter
If you uninstall the Toopher Atlassian plugin without disabling the accompanying filter, after logging in you will be redirected to `http://server:port/plugins/servlet/toopher`, which doesn't exist. The logs will look something like this:

    2015-01-01 12:34:56,504 INFO [http-8090-2] [atlassian.plugin.manager.DefaultPluginManager] 
    disableDependentPlugins Found dependent enabled plugins for uninstalled plugin 'com.toopher.confluence.servlet.toopher-confluence': [].  Disabling...
    2015-01-01 12:34:56,508 INFO [http-8090-2] [atlassian.plugin.manager.DefaultPluginManager] 
    notifyPluginDisabled Disabling com.toopher.confluence.servlet.toopher-confluence


---

# Changelog

v1.0

* initial release

