eXo.require("eXo.projects.Module");
eXo.require("eXo.projects.Product");

function getModule(params) {
  var ws = params.ws;
  var portal = params.portal;
  var module = new Module();

  module.version = "${project.version}";
  module.relativeMavenRepo = "org/exoplatform/wiki";
  module.relativeSRCRepo = "wiki";
  module.name = "wiki";
  
  var commonsVersion = "${org.exoplatform.commons.version}";

  module.commons = {};
  module.commons.extension = 
    new Project("org.exoplatform.commons", "commons-extension-webapp", "war", commonsVersion).
      addDependency(new Project("org.exoplatform.commons", "commons-component-common", "jar", commonsVersion)).
      addDependency(new Project("org.exoplatform.commons", "commons-webui-component", "jar", commonsVersion));
  module.commons.extension.deployName = "wiki-commons-extension";
    
  module.webuiExt = new Project("org.exoplatform.commons", "commons-webui-ext", "jar", commonsVersion);

  // Wiki
  module.rendering = new Project("org.exoplatform.wiki", "wiki-renderer", "jar", module.version).
                            addDependency(new Project("org.exoplatform.wiki", "wiki-macros-iframe", "jar", module.version));
  
  module.upgrade = new Project("org.exoplatform.commons", "commons-component-upgrade", "jar", commonsVersion).
    addDependency(new Project("org.exoplatform.commons", "commons-component-product", "jar", commonsVersion));
  
  //WIKI
  module.wiki = 
    new Project("org.exoplatform.wiki", "wiki-webapp", "war", module.version).
    addDependency(new Project("org.exoplatform.wiki", "wiki-service", "jar",  module.version)).
    addDependency(new Project("com.google.gwt", "gwt-servlet", "jar",  "${gwt.version}")).
    addDependency(new Project("com.google.gwt", "gwt-user", "jar",  "${gwt.version}")).
    addDependency(new Project("javax.inject", "javax.inject", "jar",  "${javax.inject.version}")).
    addDependency(new Project("net.sourceforge.cssparser", "cssparser", "jar",  "${cssparser.version}")).
    addDependency(new Project("org.apache.commons", "commons-lang3", "jar",  "${org.apache.commons.version}")).
    addDependency(new Project("javax.validation", "validation-api", "jar",  "${javax.validation.version}")).
    addDependency(new Project("org.python", "jython-standalone", "jar",  "${jython-standalone.version}")).
    addDependency(new Project("pygments", "pygments", "jar",  "${pygments.version}")).
    addDependency(new Project("net.sourceforge.htmlcleaner", "htmlcleaner", "jar",  "${net.sourceforge.htmlcleaner.version}")).
    addDependency(new Project("org.xwiki.commons", "xwiki-commons-configuration-api", "jar",  "${org.xwiki.platform.version}")).
    addDependency(new Project("org.xwiki.commons", "xwiki-commons-context", "jar",  "${org.xwiki.platform.version}")).
    addDependency(new Project("org.xwiki.commons", "xwiki-commons-component-api", "jar",  "${org.xwiki.platform.version}")).
    addDependency(new Project("org.xwiki.commons", "xwiki-commons-component-default", "jar",  "${org.xwiki.platform.version}")).
    addDependency(new Project("org.xwiki.commons", "xwiki-commons-properties", "jar",  "${org.xwiki.platform.version}")).
    addDependency(new Project("org.xwiki.commons", "xwiki-commons-xml", "jar",  "${org.xwiki.platform.version}")).
    addDependency(new Project("org.xwiki.commons", "xwiki-commons-script", "jar",  "${org.xwiki.platform.version}")).
    addDependency(new Project("org.xwiki.commons", "xwiki-commons-text", "jar",  "${org.xwiki.platform.version}")).
    addDependency(new Project("org.xwiki.rendering", "xwiki-rendering-api", "jar",  "${org.xwiki.platform.version}")).
    addDependency(new Project("org.hibernate", "hibernate-validator", "jar",  "${hibernate-validator.version}")).
    addDependency(new Project("org.xwiki.rendering", "xwiki-rendering-syntax-wikimodel", "jar",  "${org.xwiki.platform.version}")).
    addDependency(new Project("org.xwiki.rendering", "xwiki-rendering-syntax-xwiki20", "jar",  "${org.xwiki.platform.version}")).
    addDependency(new Project("org.xwiki.rendering", "xwiki-rendering-syntax-xwiki21", "jar",  "${org.xwiki.platform.version}")).
    addDependency(new Project("org.xwiki.rendering", "xwiki-rendering-syntax-xhtml", "jar",  "${org.xwiki.platform.version}")).
    addDependency(new Project("org.xwiki.rendering", "xwiki-rendering-syntax-confluence", "jar",  "${org.xwiki.platform.version}")).
    addDependency(new Project("org.xwiki.rendering", "xwiki-rendering-syntax-plain", "jar",  "${org.xwiki.platform.version}")).    
    addDependency(new Project("org.xwiki.rendering", "xwiki-rendering-transformation-macro", "jar",  "${org.xwiki.platform.version}")).
    addDependency(new Project("org.xwiki.rendering", "xwiki-rendering-transformation-icon", "jar",  "${org.xwiki.platform.version}")).
    addDependency(new Project("org.xwiki.rendering", "xwiki-rendering-macro-toc", "jar",  "${org.xwiki.platform.version}")).
    addDependency(new Project("org.xwiki.rendering", "xwiki-rendering-macro-box", "jar",  "${org.xwiki.platform.version}")).
    addDependency(new Project("org.xwiki.rendering", "xwiki-rendering-macro-message", "jar",  "${org.xwiki.platform.version}")).
    addDependency(new Project("org.xwiki.platform", "xwiki-platform-rendering-macro-code", "jar",  "${org.xwiki.platform.version}")).
    addDependency(new Project("org.xwiki.rendering", "xwiki-rendering-wikimodel", "jar",  "${org.xwiki.platform.version}")).
    addDependency(new Project("org.xwiki.platform", "xwiki-platform-model", "jar",  "${org.xwiki.platform.version}")).
    addDependency(new Project("org.xwiki.platform", "xwiki-platform-wysiwyg-client", "jar",  "${org.xwiki.platform.version}")).
    addDependency(new Project("org.gatein.web", "redirect", "jar",  "${org.exoplatform.portal.version}")).
    addDependency(new Project("org.jdom", "jdom2", "jar",  "${org.jdom.version}")).
    addDependency(new Project("org.suigeneris", "jrcs.diff", "jar",  "${org.suigeneris.version}")).
    addDependency(new Project("org.suigeneris", "jrcs.rcs", "jar",  "${org.suigeneris.version}")).
    addDependency(new Project("com.lowagie", "itext", "jar",  "${itext.version}")).
    addDependency(new Project("org.xhtmlrenderer", "flying-saucer-core", "jar",  "${flying-saucer-core.version}")).
    addDependency(new Project("org.xhtmlrenderer", "flying-saucer-pdf", "jar",  "${flying-saucer-pdf.version}")).
    addDependency(new Project("ecs", "ecs", "jar",  "${ecs.version}"));
  module.wiki.deployName = "wiki";

  // Wiki extension for tomcat
  module.extension = {};
  module.extension.webapp = 
    new Project("org.exoplatform.wiki", "wiki-extension-webapp", "war", module.version);
  module.extension.webapp.deployName = "wiki-extension";
   
  module.server = {}
  module.server.tomcat = {}
  module.server.tomcat.patch =
    new Project("org.exoplatform.wiki", "wiki-server-tomcat-patch", "jar", module.version);
 
  // Wiki demo 
  module.demo = {};
  // demo portal
  module.demo.portal =
    new Project("org.exoplatform.wiki", "wiki-demo-webapp", "war", module.version).
    addDependency(new Project("org.exoplatform.wiki", "wiki-injector", "jar", module.version)).
    addDependency(new Project("org.exoplatform.wiki", "wiki-demo-config", "jar", module.version));
  module.demo.portal.deployName = "wikidemo";

  // demo rest endpoint	   
  module.demo.rest =
    new Project("org.exoplatform.wiki", "wiki-demo-rest", "war", module.version).
    addDependency(ws.frameworks.servlet); 
  module.demo.rest.deployName = "rest-wikidemo"; 

  // GateIn Portal Redirect Portlet
  module.redirect = {};
  module.redirect.portlet = 
    new Project("org.gatein.portal.portlet", "redirect", "war", "${org.exoplatform.portal.version}");
  module.redirect.portlet.deployName = "redirect-portlet"; 
  return module;
}
