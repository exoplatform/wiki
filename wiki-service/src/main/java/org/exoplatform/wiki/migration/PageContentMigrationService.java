package org.exoplatform.wiki.migration;

import org.exoplatform.wiki.mow.api.Page;

public interface PageContentMigrationService {

  void migratePage(Page page) throws Exception;

}
