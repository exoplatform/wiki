/*
 * Copyright (C) 2003-2011 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.wiki.bench;

import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.bench.DataInjector;
import org.exoplatform.services.jcr.util.IdGenerator;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.wiki.WikiException;
import org.exoplatform.wiki.mow.api.*;
import org.exoplatform.wiki.resolver.TitleResolver;
import org.exoplatform.wiki.service.IDType;
import org.exoplatform.wiki.service.WikiService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/**
 * Plugin for injecting Wiki data.
 */
public class WikiDataInjector extends DataInjector {
  
  private HashMap<String, Integer> prefixesIndex = new HashMap<String, Integer>();
  
  public static final String       QUANTITY      = "q";

  public static final String       PREFIX        = "pre";

  public static final String       PAGE_SIZE     = "mP";

  public static final String       ATTACH_SIZE   = "maxAtt";

  public static final String       WIKI_OWNER    = "wo";

  public static final String       WIKI_TYPE     = "wt";

  public static final String       RECURSIVE     = "rcs";

  public static final String       PERMISSION    = "perm";
  
  public enum CONSTANTS {
    TYPE("type"), DATA("data"), PERM("perm");
    private final String name;

    CONSTANTS(String name) {
      this.name = name;
    }
    
    public String getName() {
      return name;
    }
  };
  
  public static final String             ARRAY_SPLIT   = ",";
  
  private static Log         log             = ExoLogger.getLogger(WikiDataInjector.class);
  
  private WikiService wikiService;

  public WikiDataInjector(WikiService wikiService,  InitParams params) {
    this.wikiService = wikiService;
  }
  
  private List<Integer> readQuantities(HashMap<String, String> queryParams) {
    String quantitiesString = queryParams.get(QUANTITY);
    List<Integer> quantities = new LinkedList<Integer>();
    for (String s : quantitiesString.split(ARRAY_SPLIT)) {
      if (s.length() > 0) {
        int quantity = Integer.parseInt(s.trim());
        quantities.add(quantity);
      }
    }
    return quantities;
  }
  
  private List<String> readPrefixes(HashMap<String, String> queryParams) {
    String prefixesString = queryParams.get(PREFIX);
    List<String> prefixes = new LinkedList<String>();
    for (String s : prefixesString.split(ARRAY_SPLIT)) {
      if (s.length() > 0) {
        prefixes.add(s);
      }
    }
    return prefixes;
  }
  
  private String readWikiOwner(HashMap<String, String> queryParams) {
    return queryParams.get(WIKI_OWNER);
  }
  
  private String readWikiType(HashMap<String, String> queryParams) {
    return queryParams.get(WIKI_TYPE);
  }

  private int readMaxAttachmentIfExist(HashMap<String, String> queryParams) {
    String value = queryParams.get(ATTACH_SIZE);
    if (value != null)
      return Integer.parseInt(value);
    else return 0;
  }
  
  private int readMaxPagesIfExist(HashMap<String, String> queryParams) {
    String value = queryParams.get(PAGE_SIZE);
    if (value != null)
      return Integer.parseInt(value);
    else return 0;
  }
  
  private boolean readRecursive(HashMap<String, String> queryParams) {
    boolean recursive = false;
    String value = queryParams.get(RECURSIVE);
    if (value != null) {
      recursive = Boolean.parseBoolean(value);
    }
    return recursive;
  }
  
  private List<Permission> readPermission(HashMap<String, String> queryParams) {
    String permString = queryParams.get(PERMISSION);
    List<Permission> permissions = new LinkedList<>();
    boolean flag = Integer.parseInt(permString.substring(0, 1)) > 0;
    if (flag) // check read permission
      permissions.add(new Permission(PermissionType.VIEWPAGE, true));
    
    flag = Integer.parseInt(permString.substring(1, 2)) > 0;
    if (flag) { // check edit permission
      permissions.add(new Permission(PermissionType.EDITPAGE, true));
    }
    return permissions;
  }
  
  private List<PermissionEntry> readPermissions(HashMap<String, String> queryParams) {
    List<PermissionEntry> permissionEntries = new ArrayList<>();
    Permission[] permissions = readPermission(queryParams).toArray(new Permission[] {});
    for (String identity : readGroupsIfExist(queryParams)) {
      permissionEntries.add(new PermissionEntry(identity, "", IDType.GROUP, permissions));
    }
    for (String identity : readUsersIfExist(queryParams)) {
      permissionEntries.add(new PermissionEntry(identity, "", IDType.USER, permissions));
    }
    for (String identity : readMembershipIfExist(queryParams)) {
      permissionEntries.add(new PermissionEntry(identity, "", IDType.MEMBERSHIP, permissions));
    }
    return permissionEntries;
  }
  
   
  private String makeTitle(String prefix, int order) {
    return new StringBuilder(prefix).append("_").append(order).toString();
  }
  
  private Page createPage(Page father, String title, String wikiOwner, String wikiType, int attSize) throws WikiException {
    Page newPage = new Page();
    newPage.setTitle(title);
    newPage.setContent(title + "_content " + randomParagraphs(10));
    Page page = wikiService.createPage(new Wiki(wikiType, wikiOwner), father.getName(), newPage);
    if (attSize > 0) {
      Attachment attachment = new Attachment();
      String attachmentTitle = "att" + IdGenerator.generate();
      attachment.setName(attachmentTitle + ".txt");
      attachment.setTitle(attachmentTitle);
      int sizeInBytes = attSize * 1024;
      StringBuilder content = new StringBuilder(sizeInBytes);
      content.append(attachment.getName()).append("_content ");
      while(content.length() <= sizeInBytes) {
        content.append(randomParagraphs(1));
      }
      attachment.setContent(content.toString().getBytes());
      attachment.setMimeType("text/plain");
      wikiService.addAttachmentToPage(attachment, page);
    }
    return page;
  }
  
  private void generatePages(List<Integer> quantities,
                             List<String> prefixes,
                             int depth,
                             int attSize,
                             int totalPages,
                             String wikiOwner,
                             String wikiType,
                             Page father) throws WikiException {
    int numOfPages = quantities.get(depth).intValue();
    String prefix = prefixes.get(depth);
    // Achieve 'prefix' pages
    List<Page> childrenPagesWithPrefix = getPagesByPrefix(prefix, father);
    for(Page childPage : childrenPagesWithPrefix) {
      log.info(String.format("%1$" + ((depth + 1) * 4) + "s Process page: %2$s in depth %3$s .......", " ", childPage.getTitle(), depth + 1));
      if (depth < quantities.size() - 1) {
        generatePages(quantities, prefixes, depth + 1, attSize, totalPages, wikiOwner, wikiType, childPage);
      }
    }
    int prefixSize = childrenPagesWithPrefix.size();
    // Check and add more pages to be equals to numOfPages
    if (prefixSize < numOfPages) {
      for (int i = prefixSize; i < numOfPages; i++) {
        Integer index = prefixesIndex.get(prefix);
        if (index == null) {
          index = i;
        }
        index++;
        prefixesIndex.put(prefix, index);
        Page page = createPage(father, makeTitle(prefix, index), wikiOwner, wikiType, attSize);
        log.info(String.format("%1$" + ((depth + 1)*4) + "s Process page: %2$s in depth %3$s .......", " ", page.getTitle(), depth + 1));
        if (depth < quantities.size() - 1) {
          generatePages(quantities, prefixes, depth + 1, attSize, totalPages, wikiOwner, wikiType, page);
        }
      }
    }
  }
  
  private void injectData(HashMap<String, String> queryParams) throws WikiException {
    log.info("Start to inject data ............... ");
    List<Integer> quantities = readQuantities(queryParams);
    List<String> prefixes = readPrefixes(queryParams);
    int attSize = readMaxAttachmentIfExist(queryParams);
    int totalPages = readMaxPagesIfExist(queryParams);
    String wikiOwner = readWikiOwner(queryParams);
    String wikiType = readWikiType(queryParams);    
    generatePages(quantities, prefixes, 0, attSize, totalPages, wikiOwner, wikiType, wikiService.getWikiByTypeAndOwner(wikiType, wikiOwner).getWikiHome());
    log.info("Injecting data has been done successfully!");
  }
  
  private void grantPermission(List<Integer> quantities, List<String> prefixes, int depth, Page father, String wikiOwner, String wikiType, List<PermissionEntry> permissions, boolean isRecursive) throws WikiException {
    int numOfPages = quantities.get(depth).intValue();
    String prefix = prefixes.get(depth);
    List<Page> childrenPagesWithPrefix = getPagesByPrefix(prefix, father);
    for(Page childPage : childrenPagesWithPrefix) {
      if(numOfPages-- <= 0) {
        break;
      }
      if (isRecursive || depth == (quantities.size() - 1)) {
        log.info(String.format("Grant permissions %1$s for page: %2$s", permissionsToString(permissions), childPage.getTitle()));
        childPage.setPermissions(permissions);
        wikiService.updatePage(childPage, null);
        if (depth < quantities.size() - 1) {
          grantPermission(quantities, prefixes, depth + 1, childPage, wikiOwner, wikiType, permissions, isRecursive);
        }
      }
    }
  }
  
  private String permissionsToString(List<PermissionEntry> permissions) {
    StringBuilder sb = new StringBuilder();
    sb.append("(");
    for(PermissionEntry permissionEntry : permissions) {
      Permission[] value = permissionEntry.getPermissions();
      sb.append("[" + permissionEntry.getIdType() + ":" + permissionEntry.getId() + ":");
      for (Permission permission : value) {
        if(permission.isAllowed()) {
          sb.append(permission.getPermissionType().toString()).append(",");
        }
      }
      sb.delete(sb.length() - 1, sb.length());
      sb.append("],");
    }
    if (sb.length() > 1) sb.delete(sb.length() - 1, sb.length());
    sb.append(")");
    return sb.toString();
  }
  
  private void grantPermission(HashMap<String, String> queryParams) throws WikiException {
    log.info("Start to grant permissions ............... ");
    List<Integer> quantities = readQuantities(queryParams);
    List<String> prefixes = readPrefixes(queryParams);
    String wikiOwner = readWikiOwner(queryParams);
    String wikiType = readWikiType(queryParams);
    List<PermissionEntry> permissions = readPermissions(queryParams);
    boolean isRecursive = readRecursive(queryParams);    
    grantPermission(quantities, prefixes, 0, wikiService.getWikiByTypeAndOwner(wikiType, wikiOwner).getWikiHome(), wikiOwner, wikiType, permissions, isRecursive);
    log.info("Permissions have been granted successfully!");
  }
  
  @Override
  public void inject(HashMap<String, String> queryParams) throws WikiException {
    String type = queryParams.get(CONSTANTS.TYPE.getName());
    if (CONSTANTS.DATA.getName().equalsIgnoreCase(type)) {
      injectData(queryParams);
    } else if (CONSTANTS.PERM.getName().equalsIgnoreCase(type)) {
      grantPermission(queryParams);
    }
  }

  @Override
  public void reject(HashMap<String, String> params) throws WikiException {
    log.info("Start to reject data ............. ");
    String wikiOwner = readWikiOwner(params);
    String wikiType = readWikiType(params);
    List<Integer> quantities = readQuantities(params);
    List<String> prefixes = readPrefixes(params);
    int numOfPages = quantities.get(0);
    String prefix = prefixes.get(0);    
      for (int i = 0; i < numOfPages; i++) {
        String title = makeTitle(prefix, i + 1);
        String pageId = TitleResolver.getId(title, true);
        if (wikiService.getPageOfWikiByName(wikiType, wikiOwner, pageId) != null) {
          if (log.isInfoEnabled()) 
            log.info(String.format("    Delete page: %1$s and its children ...", title));
          wikiService.deletePage(wikiType, wikiOwner, pageId);
        }
      }
    
    log.info("Rejecting data has been done successfully!");
  }

  public List<Page> getPagesByPrefix(String prefix, Page father) throws WikiException {
    List<Page> childrenPagesWithPrefix = new ArrayList<>();
    List<Page> childrenPages = wikiService.getChildrenPageOf(father);
    for(Page childPage : childrenPages) {
      if(childPage.getTitle() != null && childPage.getTitle().startsWith(prefix)) {
        childrenPagesWithPrefix.add(childPage);
      }
    }

    return childrenPagesWithPrefix;
  }

  @Override
  public Log getLog() {
    return log;
  }

  @Override
  public Object execute(HashMap<String, String> params) throws WikiException {
    return new Object();
  }

}

