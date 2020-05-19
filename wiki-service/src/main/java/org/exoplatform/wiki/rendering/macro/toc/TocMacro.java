package org.exoplatform.wiki.rendering.macro.toc;

import org.xwiki.component.annotation.Component;
import org.xwiki.rendering.block.*;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.macro.toc.TocMacroParameters;
import org.xwiki.rendering.transformation.MacroTransformationContext;

import javax.inject.Named;
import javax.inject.Singleton;
import java.util.Collections;
import java.util.List;

@Component
@Named("toc")
@Singleton
public class TocMacro extends org.xwiki.rendering.internal.macro.toc.TocMacro {

  public List<Block> execute(TocMacroParameters parameters, String content, MacroTransformationContext context) throws MacroExecutionException {

    List<Block> tocBlock = super.execute(parameters, content, context);

    Block container = new GroupBlock(tocBlock);
    container.setParameter("class", "toc");

    return Collections.singletonList(container);
  }
}
