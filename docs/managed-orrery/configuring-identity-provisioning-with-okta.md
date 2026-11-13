---
title: "SCIM Integration: Okta and Orrery"
description: "Configure SCIM-based identity provisioning between Okta and Orrery Cloud to automate user and group lifecycle management."
---

import FeatureAvailability from '@site/src/components/FeatureAvailability';

# SCIM Integration: Okta and Orrery

<FeatureAvailability saasOnly />

## Overview

This document covers the steps required to enable SCIM provisioning from Okta to Orrery.

This document assumes you are using OIDC for SSO with Orrery.
Since Okta doesn't currently support SCIM with OIDC, you would need to create an additional SWA-app-integration to enable SCIM provisioning.

After completing this guide, Okta will automatically sync user and group changes from your SWA app integration to Orrery. This streamlines user and group provisioning in Orrery.

Important notes about roles and permissions:

- User and group roles from Okta are not transferred to Orrery
- When a group is first synced to Orrery, you can assign roles to that group directly in Orrery
- All users in the group will inherit the group's assigned roles
- You can also apply Orrery policies to groups, which will affect users based on their group membership

### Why SCIM provisioning?

Let us look at an example of the flows enabled through SCIM provisioning.

Consider the following configuration in Okta

- A group `governance-team`
- And it has two members `john` and `sid`

Through SCIM provisioning, the following are enabled:

- Okta can create a group `governance-team` in Orrery when you "Push Groups" in Okta.
- If the `governance-team` group is assigned to the Orrery app in Okta, Okta will create the users `john` and `sid` in Orrery.
- If you remove `john` from group `governance-team` then `john` would automatically get deactivated in Orrery.
- If you remove `sid` from the Orrery app in Okta, then `sid` would automatically get deactivated in Orrery.

By default, groups and users synced from Okta to Orrery through this integration have no roles assigned. You can assign
roles to these groups directly within Orrery.

Generally, any user assignment/unassignment to the app in Okta - directly or through groups - are automatically reflected in the Orrery application.

## Configuring SCIM provisioning

### 1. Create an SWA app integration

a). Create a new [SWA app integration](https://help.okta.com/en-us/content/topics/apps/apps_app_integration_wizard_swa.htm), called say, `Orrery-SCIM-SWA`.

Note: this app-integration will only be used for SCIM provisioning. You would continue to use the existing OIDC-app-integration for SSO.

b). In the `General` tab of the `Orrery-SCIM-SWA` application, check the `Enable SCIM provisioning` option

<p>
<img width="70%"  src="https://raw.githubusercontent.com/Dxlxz/static-assets/main/imgs/scim/okta/appSettingsEnableScim.png"/>
</p>

You may also want to configure the other selections as shown in the above image, so that this application isn't visible to your users.

### 2. Configure SCIM

a). Generate a personal access token from [Orrery](../../docs/authentication/personal-access-tokens.md#creating-personal-access-tokens).

b). In the `Provisioning` tab, configure the Orrery-SCIM endpoint as shown in the below image:

<p>
<img width="70%"  src="https://raw.githubusercontent.com/Dxlxz/static-assets/main/imgs/scim/okta/scimConfig1.png"/>
</p>

**Note**: Set the value of the `Bearer` field to the personal access token obtained in step (a) above.

c). Configure the `To App` section as shown below:

<p>
<img width="70%"  src="https://raw.githubusercontent.com/Dxlxz/static-assets/main/imgs/scim/okta/scimConfig2.png"/>
</p>

**Note**: We are not pushing passwords to Orrery over SCIM, since we are assuming SSO with OIDC as mentioned earlier.

### 3. Assign users & groups to the app

Assign users and groups to the app from the `Assignments` tab:

<p>
<img width="70%"  src="https://raw.githubusercontent.com/Dxlxz/static-assets/main/imgs/scim/okta/assignUsersGroups.png"/>
</p>

### The provisioning setup is now complete

Once the above steps are completed, user assignments/unassignments to the Orrery-SCIM-SWA app in Okta will get reflected in Orrery automatically.

> #### A note on user deletion
>
> Note that when users are unassigned or deactivated in Okta, the corresponding users in Orrery are also deactivated (marked "suspended").
> But when a user is _deleted_ in Okta, the corresponding user in Orrery does _not_ get deleted.
> Refer the Okta documentation on [Delete (Deprovision)](https://developer.okta.com/docs/concepts/scim/#delete-deprovision) for more details.

### 5. (Recommended): Configure push groups

When groups are assigned to the app, Okta pushes the group-members as users to Orrery, but the group itself isn't pushed.
To push group information to Orrery, configure the `Push Groups` tab accordingly as shown below:

<p>
<img width="70%"  src="https://raw.githubusercontent.com/Dxlxz/static-assets/main/imgs/scim/okta/pushGroups.png"/>
</p>

When you assign roles or policies to groups in Orrery, all users in those groups automatically inherit the same roles or policies
Refer to the Okta [Group Push](https://help.okta.com/en-us/content/topics/users-groups-profiles/app-assignments-group-push.htm) documentation for more details.
