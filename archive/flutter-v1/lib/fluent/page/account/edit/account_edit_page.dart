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
import 'package:fluent_ui/fluent_ui.dart';
import 'package:flutter/services.dart';
import 'package:pixez/er/leader.dart';
import 'package:pixez/fluent/page/webview/account_deletion_webview_page.dart';
import 'package:pixez/i18n.dart';
import 'package:pixez/main.dart';
import 'package:pixez/page/account/edit/account_edit_store.dart';

class AccountEditPage extends StatefulWidget {
  @override
  _AccountEditPageState createState() => _AccountEditPageState();
}

class _AccountEditPageState extends State<AccountEditPage> {
  late TextEditingController _passwordController,
      _oldPasswordController,
      _emailController,
      _accountController;
  AccountEditStore _accountEditStore = AccountEditStore();

  @override
  void initState() {
    _passwordController = TextEditingController();
    _emailController = TextEditingController();
    _accountController = TextEditingController();
    _oldPasswordController = TextEditingController();
    if (accountStore.now != null) {
      if (accountStore.now!.isMailAuthorized != 1) {
        _oldPasswordController.text = accountStore.now!.passWord;
      }
      _accountController.text = accountStore.now!.account;
      _emailController.text = accountStore.now!.mailAddress;
    }
    super.initState();
  }

  @override
  void dispose() {
    _passwordController.dispose();
    _oldPasswordController.dispose();
    _emailController.dispose();
    _accountController.dispose();
    super.dispose();
  }

  Future<void> _save() async {
    if (_oldPasswordController.text.isEmpty || _emailController.text.isEmpty) {
      return;
    }
    if (_emailController.text.isNotEmpty &&
        !_emailController.text.contains('@')) {
      BotToast.showCustomText(
        toastBuilder: (_) => Align(
          alignment: Alignment(0, 0.8),
          child: Card(
            child: ListTile(
              leading: Icon(FluentIcons.error),
              title: Text("Email format error"),
            ),
          ),
        ),
      );
      return;
    }
    bool success = await _accountEditStore.fetch(
      _emailController.text,
      null,
      _oldPasswordController.text,
      null,
    );
    if (success) {
      if (accountStore.now != null) {
        if (_emailController.text.isNotEmpty) {
          accountStore.now!.mailAddress = _emailController.text;
        }
        accountStore.updateSingle(accountStore.now!);
      }
    } else if (mounted) {
      displayInfoBar(
        context,
        builder: (context, close) => InfoBar(
          title: Text('Error'),
          content: Text('${_accountEditStore.errorString}'),
          severity: InfoBarSeverity.error,
          onClose: close,
        ),
      );
    }
  }

  Future<void> _showChangePasswordDialog() async {
    await showDialog(
      context: context,
      useRootNavigator: false,
      builder: (context) => _ChangePasswordDialog(
        oldPasswordController: _oldPasswordController,
        passwordController: _passwordController,
        email: _emailController.text,
        accountEditStore: _accountEditStore,
      ),
    );
  }

  Future<void> _showAccountDeletionDialog() async {
    showDialog(
      context: context,
      useRootNavigator: false,
      builder: (ctx) {
        return ContentDialog(
          title: Text("${I18n.of(ctx).account_deletion}?"),
          content: Text("${I18n.of(ctx).account_deletion_subtitle}"),
          actions: [
            Button(
              onPressed: () => Navigator.of(ctx).pop(),
              child: Text(I18n.of(ctx).cancel),
            ),
            FilledButton(
              onPressed: () async {
                Navigator.of(ctx).pop();
                await accountStore.deleteAll();
                await Leader.push(
                  context,
                  AccountDeletionPage(),
                  icon: Icon(FluentIcons.account_management),
                  title: Text(I18n.of(context).account_deletion),
                );
              },
              child: Text(I18n.of(ctx).ok),
            ),
          ],
        );
      },
    );
  }

  @override
  Widget build(BuildContext context) {
    final errorColor = Colors.red;
    final showTokenExport =
        accountStore.now != null && accountStore.now!.isMailAuthorized == 1;

    return ScaffoldPage.scrollable(
      header: PageHeader(
        title: Text(I18n.of(context).account_message),
        commandBar: CommandBar(
          mainAxisAlignment: MainAxisAlignment.end,
          primaryItems: [
            CommandBarButton(
              icon: const Icon(FluentIcons.save),
              label: Text(I18n.of(context).save),
              onPressed: _save,
            ),
          ],
        ),
      ),
      children: [
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
          child: InfoLabel(
            label: I18n.of(context).account,
            child: TextBox(
              controller: _accountController,
              enabled: false,
              placeholder: I18n.of(context).account,
            ),
          ),
        ),
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
          child: InfoLabel(
            label: 'Email',
            child: TextBox(
              controller: _emailController,
              placeholder: 'Email',
            ),
          ),
        ),
        const Divider(),
        if (showTokenExport)
          ListTile(
            leading: Icon(FluentIcons.password_field),
            title: Text(I18n.of(context).export + " Token"),
            trailing: Icon(FluentIcons.copy),
            onPressed: () async {
              Clipboard.setData(
                ClipboardData(text: accountStore.now!.refreshToken),
              );
              BotToast.showText(text: I18n.of(context).copied_to_clipboard);
            },
          ),
        ListTile(
          leading: Icon(FluentIcons.lock),
          title: Text(I18n.of(context).change_password),
          trailing: Icon(FluentIcons.chevron_right),
          onPressed: _showChangePasswordDialog,
        ),
        const Divider(),
        ListTile(
          leading: Icon(FluentIcons.delete, color: errorColor),
          title: Text(
            I18n.of(context).account_deletion,
            style: TextStyle(color: errorColor),
          ),
          onPressed: _showAccountDeletionDialog,
        ),
      ],
    );
  }
}

class _ChangePasswordDialog extends StatefulWidget {
  final TextEditingController oldPasswordController;
  final TextEditingController passwordController;
  final String email;
  final AccountEditStore accountEditStore;

  const _ChangePasswordDialog({
    required this.oldPasswordController,
    required this.passwordController,
    required this.email,
    required this.accountEditStore,
  });

  @override
  State<_ChangePasswordDialog> createState() => _ChangePasswordDialogState();
}

class _ChangePasswordDialogState extends State<_ChangePasswordDialog> {
  String _errorMessage = "";

  Future<void> _submit() async {
    if (widget.oldPasswordController.text.isEmpty ||
        widget.passwordController.text.isEmpty) {
      return;
    }
    setState(() {
      _errorMessage = "";
    });
    final success = await widget.accountEditStore.fetch(
      widget.email,
      widget.passwordController.text,
      widget.oldPasswordController.text,
      null,
    );
    if (!mounted) return;
    if (success) {
      if (accountStore.now != null) {
        accountStore.now!.passWord = widget.passwordController.text;
        accountStore.updateSingle(accountStore.now!);
      }
      widget.oldPasswordController.text = widget.passwordController.text;
      widget.passwordController.clear();
      Navigator.of(context).pop();
    } else {
      setState(() {
        _errorMessage = widget.accountEditStore.errorString ?? "";
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return ContentDialog(
      title: Text(I18n.of(context).change_password),
      content: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          InfoLabel(
            label: I18n.of(context).current_password,
            child: PasswordBox(
              autofocus: true,
              controller: widget.oldPasswordController,
              placeholder: I18n.of(context).current_password,
            ),
          ),
          const SizedBox(height: 12),
          InfoLabel(
            label: I18n.of(context).new_password,
            child: PasswordBox(
              controller: widget.passwordController,
              placeholder: I18n.of(context).new_password,
              onEditingComplete: _submit,
            ),
          ),
          if (_errorMessage.isNotEmpty)
            Padding(
              padding: const EdgeInsets.only(top: 8.0),
              child: ConstrainedBox(
                constraints: BoxConstraints(
                  maxHeight: MediaQuery.of(context).size.height * 0.2,
                ),
                child: SingleChildScrollView(
                  child: Text(
                    _errorMessage,
                    style: TextStyle(color: Colors.red),
                  ),
                ),
              ),
            ),
        ],
      ),
      actions: [
        Button(
          onPressed: () => Navigator.of(context).pop(),
          child: Text(I18n.of(context).cancel),
        ),
        FilledButton(onPressed: _submit, child: Text(I18n.of(context).ok)),
      ],
    );
  }
}
