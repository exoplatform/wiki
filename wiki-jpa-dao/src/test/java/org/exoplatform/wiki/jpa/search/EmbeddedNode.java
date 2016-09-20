package org.exoplatform.wiki.jpa.search;

import java.util.Collection;

import org.elasticsearch.Version;
import org.elasticsearch.env.Environment;
import org.elasticsearch.node.Node;
import org.elasticsearch.plugins.Plugin;

/**
 * Node allowing to declare a list of plugins
 */
public class EmbeddedNode extends Node {

  private Version version;
  private Collection<Class<? extends Plugin>> plugins;

  public EmbeddedNode(Environment environment, Version version, Collection<Class<? extends Plugin>> classpathPlugins) {
    super(environment, version, classpathPlugins);
    this.version = version;
    this.plugins = classpathPlugins;
  }

  public Collection<Class<? extends Plugin>> getPlugins() {
    return plugins;
  }

  public Version getVersion() {
    return version;
  }
}

