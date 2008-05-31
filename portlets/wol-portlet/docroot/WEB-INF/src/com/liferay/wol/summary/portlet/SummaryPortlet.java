/**
 * Copyright (c) 2000-2008 Liferay, Inc. All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.liferay.wol.summary.portlet;

import com.liferay.portal.kernel.servlet.SessionErrors;
import com.liferay.portal.kernel.servlet.SessionMessages;
import com.liferay.portal.kernel.util.Constants;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.model.Contact;
import com.liferay.portal.model.Group;
import com.liferay.portal.model.Organization;
import com.liferay.portal.model.Role;
import com.liferay.portal.model.User;
import com.liferay.portal.security.permission.ActionKeys;
import com.liferay.portal.service.ContactLocalServiceUtil;
import com.liferay.portal.service.GroupLocalServiceUtil;
import com.liferay.portal.service.OrganizationLocalServiceUtil;
import com.liferay.portal.service.RoleLocalServiceUtil;
import com.liferay.portal.service.UserLocalServiceUtil;
import com.liferay.portal.service.permission.UserPermissionUtil;
import com.liferay.portal.theme.ThemeDisplay;
import com.liferay.portlet.expando.service.ExpandoValueLocalServiceUtil;
import com.liferay.portlet.social.model.SocialRelationConstants;
import com.liferay.portlet.social.service.SocialRelationLocalServiceUtil;
import com.liferay.portlet.social.service.SocialRequestLocalServiceUtil;
import com.liferay.util.bridges.jsp.JSPPortlet;
import com.liferay.util.dao.hibernate.QueryUtil;
import com.liferay.wol.friends.social.FriendsRequestKeys;
import com.liferay.wol.members.social.MembersRequestKeys;

import java.io.IOException;

import java.util.LinkedHashMap;
import java.util.List;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <a href="SummaryPortlet.java.html"><b><i>View Source</i></b></a>
 *
 * @author Brian Wing Shun Chan
 *
 */
public class SummaryPortlet extends JSPPortlet {

	public void processAction(ActionRequest req, ActionResponse res)
		throws IOException, PortletException {

		try {
			String cmd = ParamUtil.getString(req, Constants.CMD);

			if (cmd.equals("add_friend")) {
				addFriend(req);
			}
			else if (cmd.equals("join_organization")) {
				joinOrganization(req);
			}
			else if (cmd.equals("leave_organization")) {
				leaveOrganization(req);
			}
			else if (cmd.equals("remove_friend")) {
				removeFriend(req);
			}
			else if (cmd.equals(Constants.UPDATE)) {
				updateSummary(req);
			}

			if (SessionErrors.isEmpty(req)) {
				SessionMessages.add(req, "request_processed");
			}

			String redirect = ParamUtil.getString(req, "redirect");

			res.sendRedirect(redirect);
		}
		catch (Exception e) {
			throw new PortletException(e);
		}
	}

	protected void addFriend(ActionRequest req) throws Exception {
		ThemeDisplay themeDisplay = (ThemeDisplay)req.getAttribute(
			WebKeys.THEME_DISPLAY);

		Group group = GroupLocalServiceUtil.getGroup(
			themeDisplay.getPortletGroupId());

		User user = UserLocalServiceUtil.getUserById(group.getClassPK());

		SocialRequestLocalServiceUtil.addRequest(
			themeDisplay.getUserId(), 0, User.class.getName(),
			themeDisplay.getUserId(), FriendsRequestKeys.ADD_FRIEND,
			StringPool.BLANK, user.getUserId());
	}

	protected void joinOrganization(ActionRequest req) throws Exception {
		ThemeDisplay themeDisplay = (ThemeDisplay)req.getAttribute(
			WebKeys.THEME_DISPLAY);

		Group group = GroupLocalServiceUtil.getGroup(
			themeDisplay.getPortletGroupId());

		Organization organization =
			OrganizationLocalServiceUtil.getOrganization(group.getClassPK());

		Role role = RoleLocalServiceUtil.getRole(
			themeDisplay.getCompanyId(), "Organization Webmin");

		LinkedHashMap<String, Object> userParams =
			new LinkedHashMap<String, Object>();

		userParams.put(
			"userGroupRole",
			new Long[] {new Long(group.getGroupId()),
			new Long(role.getRoleId())});

		List<User> users = UserLocalServiceUtil.search(
			themeDisplay.getCompanyId(), null, Boolean.TRUE, userParams,
			QueryUtil.ALL_POS, QueryUtil.ALL_POS, null);

		for (User user : users) {
			SocialRequestLocalServiceUtil.addRequest(
				themeDisplay.getUserId(), 0, Organization.class.getName(),
				organization.getOrganizationId(), MembersRequestKeys.ADD_MEMBER,
				StringPool.BLANK, user.getUserId());
		}
	}

	protected void leaveOrganization(ActionRequest req) throws Exception {
		ThemeDisplay themeDisplay = (ThemeDisplay)req.getAttribute(
			WebKeys.THEME_DISPLAY);

		Group group = GroupLocalServiceUtil.getGroup(
			themeDisplay.getPortletGroupId());

		UserLocalServiceUtil.unsetOrganizationUsers(
			group.getClassPK(), new long[] {themeDisplay.getUserId()});
	}

	protected void removeFriend(ActionRequest req) throws Exception {
		ThemeDisplay themeDisplay = (ThemeDisplay)req.getAttribute(
			WebKeys.THEME_DISPLAY);

		Group group = GroupLocalServiceUtil.getGroup(
			themeDisplay.getPortletGroupId());

		User user = UserLocalServiceUtil.getUserById(group.getClassPK());

		SocialRelationLocalServiceUtil.deleteRelation(
			themeDisplay.getUserId(), user.getUserId(),
			SocialRelationConstants.TYPE_BI_FRIEND);
	}

	protected void updateSummary(ActionRequest req) throws Exception {
		ThemeDisplay themeDisplay = (ThemeDisplay)req.getAttribute(
			WebKeys.THEME_DISPLAY);

		if (!themeDisplay.isSignedIn()) {
			return;
		}

		Group group = GroupLocalServiceUtil.getGroup(
			themeDisplay.getPortletGroupId());

		User user = null;

		if (group.isUser()) {
			user = UserLocalServiceUtil.getUserById(group.getClassPK());
		}
		else {
			return;
		}

		if (!UserPermissionUtil.contains(
				themeDisplay.getPermissionChecker(), user.getUserId(),
				ActionKeys.UPDATE)) {

			return;
		}

		String jiraUserId = ParamUtil.getString(req, "jiraUserId");
		String sfUserId = ParamUtil.getString(req, "sfUserId");
		String jobTitle = ParamUtil.getString(req, "jobTitle");
		String aboutMe = ParamUtil.getString(req, "aboutMe");

		try {
			ExpandoValueLocalServiceUtil.addValue(
				User.class.getName(), "WOL", "jiraUserId", user.getUserId(),
				jiraUserId);
			ExpandoValueLocalServiceUtil.addValue(
				User.class.getName(), "WOL", "sfUserId", user.getUserId(),
				sfUserId);
			ExpandoValueLocalServiceUtil.addValue(
				User.class.getName(), "WOL", "aboutMe", user.getUserId(),
				aboutMe);

			Contact contact = user.getContact();

			contact.setJobTitle(jobTitle);

			ContactLocalServiceUtil.updateContact(contact);
		}
		catch (Exception e) {
			_log.error(e, e);
		}
	}

	private static Log _log = LogFactory.getLog(SummaryPortlet.class);

}