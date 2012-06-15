eXo.require("eXo.projects.Module") ;
eXo.require("eXo.projects.Product") ;

function getProduct(version) {
  var product = new Product();
  
  product.name = "eXoPortal" ;
  product.portalwar = "portal.war" ;
  product.codeRepo = "https://github.com/exodev/wiki" ;
  product.serverPluginVersion = "${org.exoplatform.portal.version}"; 

  var kernel = Module.GetModule("kernel") ;
  var core = Module.GetModule("core") ;
  var ws = Module.GetModule("ws", {kernel : kernel, core : core});
  var eXoJcr = Module.GetModule("jcr", {kernel : kernel, core : core, ws : ws}) ;
  var portal = Module.GetModule("portal", {kernel : kernel, ws:ws, core : core, eXoJcr : eXoJcr});
  var wiki = Module.GetModule("wiki", {portal:portal, ws:ws});
  
  product.addDependencies(portal.portlet.exoadmin) ;
  product.addDependencies(portal.portlet.web) ;
  product.addDependencies(portal.portlet.dashboard) ;
  product.addDependencies(portal.eXoGadgetServer) ;
  product.addDependencies(portal.eXoGadgets) ;
  product.addDependencies(portal.webui.portal);
  
  product.addDependencies(portal.web.eXoResources);

  product.addDependencies(portal.web.portal);
  
  // Portal extension starter required by wiki etension
  portal.starter = new Project("org.exoplatform.portal", "exo.portal.starter.war", "war", portal.version);
  portal.starter.deployName = "starter";
  //product.addDependencies(portal.starter);
  
  product.addDependencies(wiki.webuiExt);
   
  // WIKI extension
  product.addDependencies(wiki.upgrade);
  product.addDependencies(wiki.rendering);
  product.addDependencies(wiki.wiki);
  product.addDependencies(wiki.extension.webapp);
  product.addDependencies(wiki.commons.extension);

  // WIKI demo
  product.addDependencies(wiki.demo.portal);
  product.addDependencies(wiki.demo.rest);
   
  product.addServerPatch("tomcat", wiki.server.tomcat.patch) ;
  
  product.module = wiki ;
  product.dependencyModule = [ kernel, core, ws, eXoJcr];

  return product ;
}