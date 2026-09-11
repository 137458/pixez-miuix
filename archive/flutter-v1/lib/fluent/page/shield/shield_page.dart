/*
 * Copyright (C) 2020. by perol_notsf, All rights reserved
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 *
 */

import 'package:bot_toast/bot_toast.dart';
import 'package:dio/dio.dart';
import 'package:fluent_ui/fluent_ui.dart';
import 'package:flutter/services.dart';
import 'package:flutter_mobx/flutter_mobx.dart';
import 'package:pixez/er/leader.dart';
import 'package:pixez/fluent/page/shield/user_show_ai_setting.dart';
import 'package:pixez/i18n.dart';
import 'package:pixez/main.dart';
import 'package:pixez/models/ban_illust_id.dart';
import 'package:pixez/models/ban_tag.dart';
import 'package:pixez/models/ban_user_id.dart';
import 'package:pixez/models/show_ai_response.dart';
import 'package:pixez/network/api_client.dart';

class ShieldPage extends StatefulWidget {
  @override
  _ShieldPageState createState() => _ShieldPageState();
}

class _ShieldPageState extends State<ShieldPage> {
  @override
  void initState() {
    muteStore.fetchBanAI();
    muteStore.fetchBanIllusts();
    muteStore.fetchBanUserIds();
    muteStore.fetchBanComments();
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    return Observer(
      builder: (_) {
        final sortedBanTags = muteStore.banTags.toList()
          ..sort(
            (a, b) => a.name.toLowerCase().compareTo(b.name.toLowerCase()),
          );
        return ScaffoldPage.scrollable(
          header: PageHeader(
            title: Text(I18n.of(context).shielding_settings),
          ),
          children: [
            ListTile(
              leading: Icon(FluentIcons.robot),
              title: Text(I18n.of(context).ai_work_display_settings),
              trailing: Icon(FluentIcons.chevron_right),
              onPressed: () async {
                try {
                  BotToast.showLoading();
                  Response response = await apiClient.getUserAISettings();
                  var showAIResponse = ShowAIResponse.fromJson(response.data);
                  Leader.push(
                    context,
                    UserShowAISetting(showAI: showAIResponse.showAI),
                    icon: Icon(FluentIcons.robot),
                    title: Text(I18n.of(context).ai_work_display_settings),
                  );
                } catch (e) {
                } finally {
                  BotToast.closeAllLoading();
                }
              },
            ),
            ListTile(
              leading: Icon(FluentIcons.hide),
              title: Text(
                I18n.of(context).make_works_with_ai_generated_flags_invisible,
              ),
              trailing: ToggleSwitch(
                checked: muteStore.banAIIllust,
                onChanged: (v) {
                  muteStore.changeBanAI(v);
                },
              ),
            ),
            const Divider(),
            ListTile(
              leading: Icon(FluentIcons.tag),
              title: Text(I18n.of(context).tag),
              trailing: IconButton(
                onPressed: () => _showBanTagAddDialog(context),
                icon: Icon(FluentIcons.add),
              ),
            ),
            if (sortedBanTags.isNotEmpty)
              Padding(
                padding: const EdgeInsets.fromLTRB(16, 0, 16, 8),
                child: Wrap(
                  spacing: 8.0,
                  runSpacing: 8.0,
                  children: sortedBanTags
                      .map(
                        (f) => GestureDetector(
                          onLongPress: () {
                            Clipboard.setData(ClipboardData(text: f.name));
                            BotToast.showText(
                              text: I18n.of(context).copied_to_clipboard,
                            );
                          },
                          child: Button(
                            onPressed: () => deleteTag(context, f),
                            child: Text(f.name),
                          ),
                        ),
                      )
                      .toList(),
                ),
              ),
            const Divider(),
            ListTile(
              leading: Icon(FluentIcons.contact),
              title: Text(I18n.of(context).painter),
            ),
            if (muteStore.banUserIds.isNotEmpty)
              Padding(
                padding: const EdgeInsets.fromLTRB(16, 0, 16, 8),
                child: Wrap(
                  spacing: 8.0,
                  runSpacing: 8.0,
                  children: muteStore.banUserIds
                      .map(
                        (f) => Button(
                          onPressed: () => _deleteUserIdTag(context, f),
                          child: Text(f.name ?? ""),
                        ),
                      )
                      .toList(),
                ),
              ),
            const Divider(),
            ListTile(
              leading: Icon(FluentIcons.photo2),
              title: Text(I18n.of(context).illust),
            ),
            if (muteStore.banillusts.isNotEmpty)
              Padding(
                padding: const EdgeInsets.fromLTRB(16, 0, 16, 8),
                child: Wrap(
                  spacing: 8.0,
                  runSpacing: 8.0,
                  children: muteStore.banillusts
                      .map(
                        (f) => Button(
                          onPressed: () => _deleteIllust(context, f),
                          child: Text(f.name),
                        ),
                      )
                      .toList(),
                ),
              ),
          ],
        );
      },
    );
  }

  Future deleteTag(BuildContext context, BanTagPersist f) async {
    final result = await showDialog(
      context: context,
      useRootNavigator: false,
      builder: (context) {
        return ContentDialog(
          title: Text(I18n.of(context).delete),
          content: Text(I18n.of(context).delete_tag),
          actions: [
            Button(
              child: Text(I18n.of(context).cancel),
              onPressed: () {
                Navigator.pop(context);
              },
            ),
            FilledButton(
              onPressed: () {
                Navigator.pop(context, "OK");
              },
              child: Text(I18n.of(context).ok),
            ),
          ],
        );
      },
    );
    switch (result) {
      case "OK":
        {
          muteStore.deleteBanTag(f.id!);
        }
        break;
    }
  }

  _showBanTagAddDialog(BuildContext context) async {
    final controller = TextEditingController();
    final result = await showDialog(
      context: context,
      useRootNavigator: false,
      builder: (context) {
        return ContentDialog(
          title: Text(I18n.of(context).input),
          content: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            mainAxisSize: MainAxisSize.min,
            children: [
              Text('regex example:"r\'pattern\'"'),
              const SizedBox(height: 8),
              TextBox(
                controller: controller,
                autofocus: true,
                placeholder: I18n.of(context).input_regexp_hint,
                onSubmitted: (value) {
                  Navigator.pop(context, value);
                },
              ),
            ],
          ),
          actions: [
            Button(
              child: Text(I18n.of(context).cancel),
              onPressed: () {
                Navigator.pop(context);
              },
            ),
            FilledButton(
              onPressed: () {
                Navigator.pop(context, controller.text);
              },
              child: Text(I18n.of(context).ok),
            ),
          ],
        );
      },
    );
    if (result != null && result is String && result.isNotEmpty) {
      muteStore.insertBanTag(BanTagPersist(name: result, translateName: ""));
    }
  }

  Future _deleteIllust(BuildContext context, BanIllustIdPersist f) async {
    final result = await showDialog(
      context: context,
      useRootNavigator: false,
      builder: (context) {
        return ContentDialog(
          title: Text(I18n.of(context).delete),
          content: Text(I18n.of(context).delete_tag),
          actions: [
            Button(
              child: Text(I18n.of(context).cancel),
              onPressed: () {
                Navigator.pop(context);
              },
            ),
            FilledButton(
              onPressed: () {
                Navigator.pop(context, "OK");
              },
              child: Text(I18n.of(context).ok),
            ),
          ],
        );
      },
    );
    switch (result) {
      case "OK":
        {
          muteStore.deleteBanIllusts(f.id!);
        }
        break;
    }
  }

  Future _deleteUserIdTag(BuildContext context, BanUserIdPersist f) async {
    final result = await showDialog(
      context: context,
      useRootNavigator: false,
      builder: (context) {
        return ContentDialog(
          title: Text(I18n.of(context).delete),
          content: Text(I18n.of(context).delete_tag),
          actions: [
            Button(
              child: Text(I18n.of(context).cancel),
              onPressed: () {
                Navigator.pop(context);
              },
            ),
            FilledButton(
              onPressed: () {
                Navigator.pop(context, "OK");
              },
              child: Text(I18n.of(context).ok),
            ),
          ],
        );
      },
    );
    switch (result) {
      case "OK":
        {
          muteStore.deleteBanUserId(f.id!);
        }
        break;
    }
  }
}
