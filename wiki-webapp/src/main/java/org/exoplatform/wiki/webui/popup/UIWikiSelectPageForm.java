package org.exoplatform.wiki.webui.popup;

import java.util.ArrayList;
import java.util.List;

import org.exoplatform.container.PortalContainer;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.webui.commons.EventUIComponent;
import org.exoplatform.webui.commons.EventUIComponent.EVENTTYPE;
import org.exoplatform.webui.commons.UISpacesSwitcher;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.UIPopupComponent;
import org.exoplatform.webui.core.UIPopupContainer;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.wiki.commons.Utils;
import org.exoplatform.wiki.mow.api.Page;
import org.exoplatform.wiki.mow.api.Wiki;
import org.exoplatform.wiki.resolver.TitleResolver;
import org.exoplatform.wiki.service.WikiPageParams;
import org.exoplatform.wiki.service.WikiService;
import org.exoplatform.wiki.tree.TreeNode;
import org.exoplatform.wiki.tree.TreeNode.TREETYPE;
import org.exoplatform.wiki.tree.utils.TreeUtils;
import org.exoplatform.wiki.webui.UIWikiEmptyAjaxBlock;
import org.exoplatform.wiki.webui.UIWikiPortlet;
import org.exoplatform.wiki.webui.UIWikiPortlet.PopupLevel;
import org.exoplatform.wiki.webui.tree.UITreeExplorer;

@ComponentConfig(
    lifecycle = UIFormLifecycle.class,
    template = "/templates/wiki/webui/popup/UIWikiSelectPageForm.gtmpl",
    events = {
        @EventConfig(listeners = UIWikiSelectPageForm.SetCurrentPageActionListener.class),
        @EventConfig(listeners = UIWikiSelectPageForm.SelectPageActionListener.class),
        @EventConfig(listeners = UIWikiSelectPageForm.CancelActionListener.class),
        @EventConfig(listeners = UIWikiSelectPageForm.SwitchSpaceActionListener.class)
    }
)
public class UIWikiSelectPageForm extends UIForm implements UIPopupComponent {
  private static final Log log                     = ExoLogger.getLogger(UIWikiSelectPageForm.class);
  
  public static final String FORM_ID = "UIWikiSelectPageForm";
  
  private String currentNodeValue = ""; 
  
  public static final String UI_TREE_ID = "UIPageTree";
  
  private static final String SWITCH_SPACE_ACTION = "SwitchSpace";
  
  private static final String SPACE_SWITCHER = "uiSpaceSwitcher_UIWikiSelectPageForm";
  
  public UIWikiSelectPageForm() throws Exception {
    setId(FORM_ID);
    UITreeExplorer uiTree = addChild(UITreeExplorer.class, null, UI_TREE_ID);
    EventUIComponent eventComponent = new EventUIComponent(FORM_ID,
                                                           "SetCurrentPage",
                                                           EVENTTYPE.EVENT);
    StringBuilder initURLSb = new StringBuilder(Utils.getCurrentRestURL());
    initURLSb.append("/wiki/tree/").append(TREETYPE.ALL.toString());
    StringBuilder childrenURLSb = new StringBuilder(Utils.getCurrentRestURL());
    childrenURLSb.append("/wiki/tree/").append(TREETYPE.CHILDREN.toString());
    uiTree.init(initURLSb.toString(), childrenURLSb.toString(), getInitParam(Utils.getCurrentWikiPagePath()), eventComponent, false);
    
    // Init space switcher
    UISpacesSwitcher uiWikiSpaceSwitcher = addChild(UISpacesSwitcher.class, null, SPACE_SWITCHER);
    uiWikiSpaceSwitcher.setCurrentSpaceName(Utils.upperFirstCharacter(Utils.getCurrentSpaceName()));
    EventUIComponent eventComponent1 = new EventUIComponent(FORM_ID, SWITCH_SPACE_ACTION, EVENTTYPE.EVENT);
    uiWikiSpaceSwitcher.init(eventComponent1);
  }
  
  /**
   * list of ui component needed to updated when form is submitted.
   */
  private List<UIComponent> updatedComponents = new ArrayList<UIComponent>();
  
  public void addUpdatedComponent(UIComponent component) {
    updatedComponents.add(component);
  }
  
  public void removeUpdatedComponent(UIComponent component) {
    updatedComponents.remove(component);
  }
  
  private String getInitParam(String currentPath) throws Exception {
    StringBuilder sb = new StringBuilder();
    sb.append("?")
      .append(TreeNode.PATH)
      .append("=")
      .append(currentPath)
      .append("&")
      .append(TreeNode.CURRENT_PATH)
      .append("=")
      .append(currentPath);
    return sb.toString();
  }
  
  @Override
  public void activate() {
  }
  
  @Override
  public void deActivate() {
  }
  
  static public class SetCurrentPageActionListener extends EventListener<UIWikiSelectPageForm> {

    @Override
    public void execute(Event<UIWikiSelectPageForm> event) throws Exception {
      UIWikiSelectPageForm uiform = event.getSource();
      UIWikiEmptyAjaxBlock emptyBlock = uiform.getAncestorOfType(UIWikiPortlet.class).getChild(UIWikiEmptyAjaxBlock.class);
      String param = event.getRequestContext().getRequestParameter(OBJECTID);
      if (param != null) uiform.currentNodeValue = param;
      event.getRequestContext().addUIComponentToUpdateByAjax(emptyBlock);
    }
  }
  
  static public class SelectPageActionListener extends EventListener<UIWikiSelectPageForm> {

    @Override
    public void execute(Event<UIWikiSelectPageForm> event) throws Exception {
      UIWikiSelectPageForm uiform = event.getSource();
      UIWikiPortlet wikiPortlet = uiform.getAncestorOfType(UIWikiPortlet.class);
      try {
        if (uiform.currentNodeValue.length() > 0) {
          String currentNodeValue = TitleResolver.getId(uiform.currentNodeValue, false);
          WikiPageParams params = TreeUtils.getPageParamsFromPath(currentNodeValue);

          WikiService service = uiform.getApplicationComponent(WikiService.class);
          service.addRelatedPage(Utils.getCurrentWikiPageParams(), params);
        }
      } catch (Exception e) {
         if (log.isWarnEnabled()) log.warn("can not execute 'SelectPage' action", e);
      }
      UIPopupContainer popupContainer = wikiPortlet.getPopupContainer(PopupLevel.L1);
      popupContainer.cancelPopupAction();
      event.getRequestContext().addUIComponentToUpdateByAjax(popupContainer);
      for (UIComponent c : uiform.updatedComponents) {
        event.getRequestContext().addUIComponentToUpdateByAjax(c);
      }
    }
  }
  
  static public class CancelActionListener extends EventListener<UIWikiSelectPageForm> {

    @Override
    public void execute(Event<UIWikiSelectPageForm> event) throws Exception {
      UIWikiSelectPageForm uiform = event.getSource();
      UIWikiPortlet wikiPortlet = uiform.getAncestorOfType(UIWikiPortlet.class);
      UIPopupContainer popupContainer = wikiPortlet.getPopupContainer(PopupLevel.L1);
      popupContainer.cancelPopupAction();
      event.getRequestContext().addUIComponentToUpdateByAjax(popupContainer);
    }
  }
  
  public static class SwitchSpaceActionListener extends EventListener<UIWikiSelectPageForm> {
    public void execute(Event<UIWikiSelectPageForm> event) throws Exception {
      String wikiId = event.getRequestContext().getRequestParameter(UISpacesSwitcher.SPACE_ID_PARAMETER);
      UIWikiSelectPageForm uiWikiSelectPageForm = event.getSource();
      UISpacesSwitcher uiWikiSpaceSwitcher = uiWikiSelectPageForm.getChildById(SPACE_SWITCHER);
      WikiService wikiService = (WikiService) PortalContainer.getComponent(WikiService.class);
      
      Wiki wiki = wikiService.getWikiById(wikiId);
      Page wikiHome = wiki.getWikiHome();
      WikiPageParams params = new WikiPageParams(wiki.getType(), wiki.getOwner(), wikiHome.getName());
      uiWikiSpaceSwitcher.setCurrentSpaceName(Utils.upperFirstCharacter(wikiService.getWikiNameById(wikiId)));
      
      // Change the init page of tree
      UITreeExplorer uiTree = uiWikiSelectPageForm.getChildById(UI_TREE_ID);
      StringBuilder initParams = new StringBuilder();
      initParams.append("?")
        .append(TreeNode.PATH)
        .append("=")
        .append(TreeUtils.getPathFromPageParams(params))
        .append("&")
        .append(TreeNode.CURRENT_PATH)
        .append("=")
        .append(Utils.getCurrentWikiPagePath());
      uiTree.setInitParam(initParams.toString());
      
      event.getRequestContext().addUIComponentToUpdateByAjax(uiWikiSelectPageForm.getParent());
    }
  }
}
