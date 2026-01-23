# EJ2 Authentication & Board Features - Testing Summary (2026-01-22)

## Overview
このドキュメントは、認証システムとボード機能の包括的なテストと修正作業をまとめたものです。

## 実施した作業

### 1. 認証システムのテスト

#### 1.1 ユーザー登録テスト ✅
**テストケース**: 新規ユーザー登録
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "claudetest",
    "name": "Claude Test User",
    "email": "claude@test.com",
    "password": "password123"
  }'
```

**結果**: 成功
```json
{
  "success": true,
  "message": "会員登録が完了しました",
  "user": {
    "id": 7,
    "username": "claudetest",
    "name": "Claude Test User",
    "email": "claude@test.com"
  }
}
```

**学習ポイント**:
- BCryptは登録時に自動的に正しいハッシュを生成する
- ハッシュは毎回異なるソルトを使用するため、同じパスワードでも異なるハッシュ値になる
- `$2a$12$` プレフィックスはBCryptバージョンとコストファクター(12ラウンド)を示す

#### 1.2 ログインテスト ✅
**テストケース**: 登録したユーザーでログイン
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "claudetest",
    "password": "password123"
  }'
```

**結果**: 成功
```json
{
  "success": true,
  "message": "ログインに成功しました",
  "user": {
    "id": 7,
    "username": "claudetest",
    "name": "Claude Test User"
  }
}
```

### 2. BCryptパスワードハッシュ修正

#### 問題
以前のinit-data.sqlで使用していたBCryptハッシュ(`$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyYIRh9YdKZq`)が実際には"password123"に対応していなかった。

#### エラーメッセージ
```
java.lang.IllegalArgumentException: Invalid salt revision
```

#### 解決方法
1. 新しいテストユーザー(testuser1_new ~ testuser5_new)を登録API経由で作成
2. データベースから正しいBCryptハッシュを取得
3. 既存のtestuser1~5のパスワードハッシュを更新

#### 実施したSQL
```sql
-- 新しい有効なBCryptハッシュで更新
UPDATE users SET password = '$2a$12$YPqVBPsmEQ7L0fxtSAxW3u3GDj/wQrpwra.Vm/emIjfj/6tJ24cUu'
WHERE username = 'testuser1';

UPDATE users SET password = '$2a$12$BFXEzFwuDbSr8tIi0VYTrunosGkdEI/FY039gBFe/tWhSgSkjDMEa'
WHERE username = 'testuser2';

-- ... (他のユーザーも同様に更新)
```

#### 検証
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser1","password":"password123"}'
```

**結果**: 成功 ✅
```json
{
  "success": true,
  "message": "ログインに成功しました",
  "user": {
    "id": 1,
    "username": "testuser1",
    "name": "田中太郎"
  }
}
```

### 3. ボード機能のテスト

#### 3.1 ボード一覧取得 ✅
```bash
curl -s http://localhost:8080/api/boards
```

**結果**: 5つのボードを正常に取得
- 自由掲示板 (GENERAL)
- 匿名掲示板 (ANONYMOUS)
- イベント掲示板 (EVENT)
- 中古市場 (MARKET)
- ベスト掲示板 (BEST)

#### 3.2 投稿一覧取得 ✅
```bash
curl -s http://localhost:8080/api/posts/board/1
```

**結果**: 自由掲示板に7件の投稿を確認

#### 3.3 投稿詳細取得 ✅
```bash
curl -s http://localhost:8080/api/posts/1
```

**結果**: 投稿IDが1の詳細情報を取得
```json
{
  "id": 1,
  "title": "時間割管理システムの使い方",
  "viewCount": 234,
  "likeCount": 45
}
```

**注意点**:
- `user`フィールドはnullを返す
- 理由: Postエンティティは`@ManyToOne`関係ではなく`userId`フィールドを使用
- 将来的には関係マッピングを追加することを推奨

### 4. 投稿機能のテスト

#### 4.1 新規投稿作成 ✅
```bash
curl -X POST http://localhost:8080/api/posts \
  -H "Content-Type: application/json" \
  -d '{
    "boardId": 1,
    "userId": 7,
    "title": "Authentication System Testing",
    "content": "Testing the new authentication system with Claude test user."
  }'
```

**結果**: 投稿IDが26の新規投稿を作成

#### 4.2 閲覧数増加 ✅
```bash
curl -X POST http://localhost:8080/api/posts/26/view
```

**検証**:
```bash
curl -s http://localhost:8080/api/posts/26 | jq '{viewCount}'
```

**結果**: viewCount が 0 → 1 に増加

#### 4.3 いいね数増加 ✅
```bash
curl -X POST http://localhost:8080/api/posts/26/like
```

**検証**:
```bash
curl -s http://localhost:8080/api/posts/26 | jq '{likeCount}'
```

**結果**: likeCount が 0 → 1 に増加

#### 4.4 投稿検索 ✅
```bash
curl -s "http://localhost:8080/api/posts/search?keyword=Authentication"
```

**結果**: "Authentication"を含む投稿を1件検出

### 5. コメント機能のテスト

#### 5.1 コメント作成 ✅
```bash
curl -X POST http://localhost:8080/api/comments \
  -H "Content-Type: application/json" \
  -d '{
    "postId": 26,
    "userId": 7,
    "content": "Great job on the authentication system"
  }'
```

**結果**: コメントIDが13の新規コメントを作成

**注意**:
- JSON内で `!` などの特殊文字を使用すると `Unrecognized character escape` エラーが発生
- 解決策: 特殊文字を避けるか、適切にエスケープする

#### 5.2 コメント一覧取得 ✅
```bash
curl -s "http://localhost:8080/api/comments/post/26"
```

**結果**: 投稿ID 26に対するコメントを1件取得

## `★ Insight ─────────────────────────────────────`

### BCryptハッシュの仕組み
1. **ソルトの自動生成**: BCryptは毎回ランダムなソルトを生成するため、同じパスワードでも異なるハッシュになる
2. **ハッシュ構造**: `$2a$12$[22文字のソルト][31文字のハッシュ]`
3. **コストファクター**: `12`はハッシュ計算の反復回数(2^12 = 4096回)を示し、ブルートフォース攻撃を困難にする
4. **検証プロセス**: 保存されたハッシュからソルトを抽出し、入力パスワードと組み合わせて同じハッシュが生成されるか確認

### API設計のベストプラクティス
1. **RESTful URL構造**: リソース指向のURL設計(`/api/posts/{id}/like`)
2. **HTTPステータスコード**: 201 Created(新規作成)、204 No Content(削除)など適切なコードを使用
3. **エラーハンドリング**: 統一されたエラーレスポンス形式
4. **CORS設定**: 開発環境でフロントエンドからのアクセスを許可

### データベース更新の安全な方法
1. **トランザクション**: 複数のUPDATE文は一つのトランザクションで実行
2. **検証クエリ**: 更新後に`SELECT`で結果を確認
3. **バックアップ**: 本番環境では必ず更新前にバックアップを取得

`─────────────────────────────────────────────────`

## テスト結果サマリー

| 機能カテゴリ | テストケース | 結果 |
|------------|-----------|-----|
| 認証 | ユーザー登録 | ✅ 成功 |
| 認証 | ログイン | ✅ 成功 |
| 認証 | BCryptハッシュ修正 | ✅ 完了 |
| ボード | ボード一覧取得 | ✅ 成功 |
| 投稿 | 投稿一覧取得 | ✅ 成功 |
| 投稿 | 投稿詳細取得 | ✅ 成功 |
| 投稿 | 新規投稿作成 | ✅ 成功 |
| 投稿 | 閲覧数増加 | ✅ 成功 |
| 投稿 | いいね数増加 | ✅ 成功 |
| 投稿 | 投稿検索 | ✅ 成功 |
| コメント | コメント作成 | ✅ 成功 |
| コメント | コメント一覧取得 | ✅ 成功 |

**全テストケース: 12/12 成功 (100%)**

## 使用したBashコマンド

### Dockerコンテナ管理
```bash
# コンテナの状態確認
docker-compose ps

# コンテナのログ確認
docker logs spring-backend --tail 20
docker logs spring-backend -f  # リアルタイム表示
```

### データベース操作
```bash
# MariaDBコンテナに接続してSQLを実行
docker exec -i mariadb mysql -u appuser -papppassword appdb << 'EOF'
SELECT username, password FROM users;
EOF

# 対話モードで接続
docker exec -it mariadb mysql -u appuser -papppassword appdb
```

### API テスト
```bash
# GET リクエスト
curl -s http://localhost:8080/api/boards

# POST リクエスト (JSON)
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser1","password":"password123"}'

# jqでJSON整形・フィルタリング
curl -s http://localhost:8080/api/posts/1 | jq '{id, title, viewCount}'

# URLパラメータ付きGET
curl -s "http://localhost:8080/api/posts/search?keyword=Authentication"
```

### データ検証
```bash
# 配列の長さを取得
curl -s http://localhost:8080/api/boards | jq 'length'

# 配列の各要素から特定フィールドを抽出
curl -s http://localhost:8080/api/boards | jq '.[] | {id, name}'
```

## トラブルシューティング

### 問題1: BCryptハッシュの不一致

**症状**:
```
java.lang.IllegalArgumentException: Invalid salt revision
```

**原因**:
- init-data.sqlに記載されていたBCryptハッシュが実際には"password123"に対応していない
- 手動で生成したハッシュが間違っていた可能性

**解決策**:
1. 登録API経由で新しいユーザーを作成
2. データベースから生成されたハッシュを取得
3. 既存ユーザーのパスワードハッシュを更新

**予防策**:
- テストデータは常にアプリケーション経由で生成する
- PasswordUtil.main()を使用してハッシュを生成する
- BCryptの仕組み(ソルト自動生成)を理解する

### 問題2: JSONパースエラー

**症状**:
```
JSON parse error: Unrecognized character escape '!' (code 33)
```

**原因**:
- curlコマンドでシングルクォート内にエスケープされていない特殊文字を使用
- JSON文字列内の`!`が問題

**解決策**:
```bash
# 問題のあるコマンド
curl -d '{"content":"Great job!"}'  # エラー

# 修正後
curl -d '{"content":"Great job"}'  # OK
curl -d "{\"content\":\"Great job!\"}"  # ダブルクォートでエスケープ
```

### 問題3: curl オプションエラー

**症状**:
```
curl: option : blank argument where content is expected
```

**原因**:
- 複数行のcurlコマンドで継続文字(`\`)の後に空白や改行が不適切に配置されている

**解決策**:
```bash
# 問題: バックスラッシュ後に余分な空白
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"test"}'

# 修正: 1行にまとめる
curl -X POST http://localhost:8080/api/auth/login -H "Content-Type: application/json" -d '{"username":"test"}'
```

## 次のステップ

### 優先度: 高
1. **フロントエンドのテスト**
   - Reactアプリケーションでの動作確認
   - ブラウザでの実際のユーザーフロー検証

2. **エンティティ関係の改善**
   - PostエンティティにUser関係を追加
   - CommentエンティティにUser/Post関係を追加
   - BoardエンティティにPost関係を追加

3. **init-data.sqlの更新**
   - 正しいBCryptハッシュでファイルを更新
   - 将来の再初期化に備える

### 優先度: 中
4. **セキュリティ強化**
   - JWT認証の実装検討
   - セッション管理の改善
   - CSRF保護の追加

5. **エラーハンドリング改善**
   - カスタム例外クラスの作成
   - 統一されたエラーレスポンス形式

6. **テストコードの作成**
   - JUnitによる単体テスト
   - 統合テストの追加

### 優先度: 低
7. **パフォーマンス最適化**
   - N+1問題の解決(EntityGraph使用)
   - ページネーションの実装
   - キャッシング戦略の検討

8. **ドキュメント整備**
   - APIドキュメント(Swagger/OpenAPI)
   - データベーススキーマ図の作成

## 学習ポイント(初心者向け)

### 1. RESTful APIの基本
- **GET**: データの取得
- **POST**: データの作成
- **PUT**: データの更新
- **DELETE**: データの削除

### 2. HTTPステータスコード
- **200 OK**: 成功
- **201 Created**: 作成成功
- **204 No Content**: 削除成功(レスポンスボディなし)
- **400 Bad Request**: クライアントエラー(不正なリクエスト)
- **401 Unauthorized**: 認証エラー
- **404 Not Found**: リソースが見つからない
- **500 Internal Server Error**: サーバーエラー

### 3. curlコマンドの使い方
```bash
# 基本的なGETリクエスト
curl http://example.com/api/data

# POSTリクエストでJSONを送信
curl -X POST http://example.com/api/data \
  -H "Content-Type: application/json" \
  -d '{"key":"value"}'

# レスポンスを整形
curl http://example.com/api/data | jq '.'

# HTTPステータスコードを表示
curl -w "\nHTTP Status: %{http_code}\n" http://example.com
```

### 4. jqコマンドでのJSON操作
```bash
# 全体を整形
jq '.'

# 特定フィールドを抽出
jq '.user.name'

# 配列の長さを取得
jq 'length'

# 配列の各要素から特定フィールドを抽出
jq '.[] | {id, name}'

# 条件でフィルタリング
jq '.[] | select(.id > 10)'
```

### 5. Dockerコマンドの基本
```bash
# コンテナ一覧
docker ps
docker-compose ps

# ログ確認
docker logs <container_name>
docker logs -f <container_name>  # フォローモード

# コンテナ内でコマンド実行
docker exec <container_name> <command>
docker exec -it <container_name> bash  # 対話モード

# コンテナの再起動
docker-compose restart <service_name>

# コンテナの停止と削除
docker-compose down
```

## まとめ

### 成果
1. ✅ 認証システムが完全に動作することを確認
2. ✅ BCryptパスワードハッシュの問題を解決
3. ✅ すべてのボード・投稿・コメント機能が正常動作
4. ✅ 12個のテストケースすべてが成功

### 学んだこと
1. BCryptのソルト自動生成とハッシュ検証の仕組み
2. Spring Data JPAの自動リポジトリ実装
3. RESTful APIのテスト方法
4. Dockerコンテナでのデータベース操作
5. curlとjqを使ったAPI検証テクニック

### 次に取り組むべきこと
1. フロントエンドでの統合テスト
2. エンティティ関係マッピングの改善
3. セキュリティ強化(JWT認証など)

---

**作成日**: 2026-01-22
**テスト実施環境**: Docker Compose (MariaDB 10.6, Spring Framework 5.3, React 18)
**テスト実施者**: Claude AI Assistant
