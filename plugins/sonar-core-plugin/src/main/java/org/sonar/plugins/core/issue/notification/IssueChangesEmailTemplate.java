/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.plugins.core.issue.notification;

import org.apache.commons.lang.StringUtils;
import org.sonar.api.config.EmailSettings;
import org.sonar.api.notifications.Notification;
import org.sonar.api.user.User;
import org.sonar.api.user.UserFinder;
import org.sonar.plugins.emailnotifications.api.EmailMessage;
import org.sonar.plugins.emailnotifications.api.EmailTemplate;

import javax.annotation.Nullable;

/**
 * Creates email message for notification "issue-changes".
 *
 * @since 3.6
 */
public class IssueChangesEmailTemplate extends EmailTemplate {

  private final EmailSettings settings;
  private final UserFinder userFinder;

  public IssueChangesEmailTemplate(EmailSettings settings, UserFinder userFinder) {
    this.settings = settings;
    this.userFinder = userFinder;
  }

  @Override
  public EmailMessage format(Notification notif) {
    if (!"issue-changes".equals(notif.getType())) {
      return null;
    }
    String issueKey = notif.getFieldValue("key");
    String author = notif.getFieldValue("changeAuthor");

    StringBuilder sb = new StringBuilder();
    String projectName = notif.getFieldValue("projectName");
    appendField(sb, "Project", null, projectName);
    appendField(sb, "Component", null, StringUtils.defaultString(notif.getFieldValue("componentName"), notif.getFieldValue("componentKey")));
    appendField(sb, "Rule", null, notif.getFieldValue("ruleName"));
    appendField(sb, "Message", null, notif.getFieldValue("message"));
    appendField(sb, "Comment", null, notif.getFieldValue("comment"));
    sb.append('\n');

    appendField(sb, "Assignee", notif.getFieldValue("old.assignee"), notif.getFieldValue("new.assignee"));
    appendField(sb, "Severity", notif.getFieldValue("old.severity"), notif.getFieldValue("new.severity"));
    appendField(sb, "Resolution", notif.getFieldValue("old.resolution"), notif.getFieldValue("new.resolution"));
    appendField(sb, "Status", notif.getFieldValue("old.status"), notif.getFieldValue("new.status"));
    appendField(sb, "Message", notif.getFieldValue("old.message"), notif.getFieldValue("new.message"));

    appendFooter(sb, notif);

    EmailMessage message = new EmailMessage()
      .setMessageId("issue-changes/" + issueKey)
      .setSubject("Project "+ projectName +", change on issue #" + issueKey)
      .setMessage(sb.toString());
    if (author != null) {
      message.setFrom(getUserFullName(author));
    }
    return message;
  }

  private void appendField(StringBuilder sb, String name, @Nullable String oldValue, @Nullable String newValue) {
    if (oldValue != null || newValue != null) {
      sb.append(name).append(": ");
      if (newValue != null) {
        sb.append(newValue);
      }
      if (oldValue != null) {
        sb.append(" (was ").append(oldValue).append(")");
      }
      sb.append('\n');
    }
  }

  private void appendFooter(StringBuilder sb, Notification notification) {
    String issueKey = notification.getFieldValue("key");
    sb.append("\n")
      .append("See it in SonarQube: ").append(settings.getServerBaseURL()).append("/issue/show/").append(issueKey).append('\n');
  }

  private String getUserFullName(@Nullable String login) {
    if (login == null) {
      return null;
    }
    User user = userFinder.findByLogin(login);
    if (user == null) {
      // most probably user was deleted
      return login;
    }
    return StringUtils.defaultIfBlank(user.name(), login);
  }

}
