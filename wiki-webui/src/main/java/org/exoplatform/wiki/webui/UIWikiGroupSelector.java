package org.exoplatform.wiki.webui;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.exoplatform.commons.serialization.api.annotations.Serialized;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.services.organization.Group;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.ComponentConfigs;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIBreadcumbs;
import org.exoplatform.webui.core.UITree;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.organization.account.UIGroupSelector;

@ComponentConfigs({
    @ComponentConfig(template = "system:/groovy/webui/organization/account/UIGroupSelector.gtmpl", events = {
        @EventConfig(phase = Event.Phase.DECODE, listeners = UIGroupSelector.ChangeNodeActionListener.class),
        @EventConfig(phase = Event.Phase.DECODE, listeners = UIGroupSelector.SelectGroupActionListener.class),
        @EventConfig(phase = Event.Phase.DECODE, listeners = UIGroupSelector.SelectPathActionListener.class) }),
    @ComponentConfig(type = UITree.class, id = "UITreeGroupSelector", template = "system:/groovy/webui/core/UITree.gtmpl", events = @EventConfig(phase = Event.Phase.DECODE, listeners = UITree.ChangeNodeActionListener.class)),
    @ComponentConfig(type = UIBreadcumbs.class, id = "BreadcumbGroupSelector", template = "system:/groovy/webui/core/UIBreadcumbs.gtmpl", events = @EventConfig(phase = Event.Phase.DECODE, listeners = UIBreadcumbs.SelectPathActionListener.class)) })
@Serialized
public class UIWikiGroupSelector extends UIGroupSelector {

  public UIWikiGroupSelector() throws Exception {
  }

  @Override
  protected List<Group> getGroups(Group parentGroup) throws Exception {
    OrganizationService organizationService = getApplicationComponent(OrganizationService.class);
    UserACL userACL = getApplicationComponent(UserACL.class);

    ConversationState conversationState = ConversationState.getCurrent();
    if (conversationState != null && conversationState.getIdentity() != null) {
      return organizationService.getGroupHandler()
                                .findGroups(parentGroup)
                                .stream()
                                .filter(group -> userACL.hasPermission(conversationState.getIdentity(), group, "wiki_permissions"))
                                .collect(Collectors.toList());
    } else {
      return Collections.EMPTY_LIST;
    }
  }
}
