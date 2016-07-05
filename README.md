mBreak - [Minecraft Mod][homepage]
===============================
Version: 1.10.2-1

![IMAGE](http://i.imgur.com/ONkhdsq.gif "")


0. Mod 機能一覧
---------------

  - 各機能の詳細に関しては [Wiki](../../wiki/Function) を参照してください
  - ブロックの採掘速度を変更
  - ごく限定的なブロックの連鎖破壊
  - ブロック採掘時に松明を自動で設置


1. 既知の不具合
---------------

  - [Issues](../../issues) を参照してください


2. 今後の更新予定
-----------------

  - トグルキーで切り替えた機能のオン/オフ状態が保存されるように
  - 松明が自動で設置される場所をプレイヤーの目線の高さに限定


3. インストール
---------------

  - 事前に [Minecraft Forge][forge] をインストールしておいてください
    - インストールする Minecraft Forge のバージョンは 依存関係 の項目を参考にしてください
  - [DependencyResolver][resolver] をインストールしてください
  - [ダウンロード][homepage]した jar ファイルを mods フォルダに移動させます
  - 作業は以上です


4. 依存関係
---------------

    Note:  
      表記されている Minecraft Forge のバージョンは開発に使用したものです  
      このバージョンでなければ動作しない､ということはないのであくまで参考程度に考えてください


  - 1.10.2-1
    - [Minecraft Forge][forge]:       1.10.2-12.18.0.2006-1.10.0
    - [DependencyResolver][resolver]: 1.0 ~
    - [MC-Commons][commons]:          1.10.2-0.0.2


  - 1.9.4-2
    - [Minecraft Forge][forge]:       1.9.4-12.17.0.1940
    - [DependencyResolver][resolver]: 1.0
    - [MC-Commons][commons]:          1.9.4-0.0.1


  - 1.9.4-1
    - [Minecraft Forge][forge]: 1.9.4-12.17.0.1940
    - [MC-Commons][commons]:    1.9.4-0.0.1


  - 1.9-2
    - [Minecraft Forge][forge]: 1.9-12.16.1.1891


  - 1.9-1
    - [Minecraft Forge][forge]: 1.9-12.16.0.1767-1.9 ( ~ 1.9-12.16.0.1885 )


  - 1.8.9-1 ~ 1.8.9-2
    - [Minecraft Forge][forge]: 1.8.9-11.15.0.1694


  - 1.8-1
    - [Minecraft Forge][forge]: 1.8-11.14.4.1577


5. コンフィグ
-------------

  - 設定項目の詳細に関しては [Wiki](../../wiki/Configuration) を参照してください
  - ゲーム内で変更する場合 ( 推奨 )
    - タイトル画面から Mods -> 一覧から mBreak を選択 -> Config ボタンを押下
    - ゲームメニュー ( ゲーム中 ESC ) の Mod Options... を選択 -> 一覧から mBreak を選択 -> Config ボタンを押下
      - Note: 古いバージョンの Minecraft Forge だと動作しないことがあります
  - コンフィグファイルを直接編集する場合
    - Forge 環境コンフィグフォルダの moj_mbreak.cfg をエディタで編集


6. 更新履歴
-----------

  - 1.10.2-1
    - Minecraft 1.10.2 に対応
    - コンフィグのトグルキー設定項目を廃止､ゲーム内GUIから設定するように変更
    - トグルキーでの機能切り替え時に表示されるチャットメッセージがプレイヤーの発言になっていたのを修正


  - 1.9.4-2
    - [DependencyResolver][resolver] での依存関係解決に対応


  - 1.9.4-1
    - Minecraft 1.9.4 に対応
    - 依存関係に [MC-Commons][commons] を追加


  - 1.9-2
    - Minecraft Forge 1.9-12.16.0.1886 以降に対応


  - 1.9-1
    - Minecraft 1.9 に対応
    - コンフィグのファイル名を Mod名.cfg から ModID.cfg に変更


  - 1.8.9-2
    - 各機能をキーボード操作でオン/オフ切り替えできるように
    - UpdateJSONに対応
    - ゲーム内表記の調整


  - 1.8.9-1
    - Minecraft 1.8.9 に対応
    - 松明の自動設置により手持ちの松明がなくなった場合､チャットでプレイヤーに通知するようになった
    - より適切に松明の自動設置が行われるようになった


  - 1.8-1
    - 公開


7. ライセンス
-------------

(c) Man of J, 2015-2016

この Mod は [Minecraft Mod Public License - Version 1.0.1](./LICENSE.md) のもとで提供されています


--------------------------------

ご意見,ご要望,バグ報告などありましたら [Issues](../../issues) か下記の連絡手段にてお願いします
  - E-mail: <man.of.j@outlook.com>
  - Twitter: [_ManOfJ](https://twitter.com/_ManOfJ)

--------------------------------

[//]: # ( リンクのエイリアス一覧 )

[homepage]: http://manofj.com/minecraft/
[forge]:    http://files.minecraftforge.net/
[resolver]: https://github.com/ManOfJ/DependencyResolver
[commons]:  https://github.com/ManOfJ/MC-Commons
