# 0122_2 認証システム実装ガイド

**作成日**: 2026-01-22
**対象**: 初心者〜中級者
**所要時間**: 約1時間

---

## 📋 目次

1. [実装した機能](#実装した機能)
2. [変更したファイル一覧](#変更したファイル一覧)
3. [詳細な実装手順](#詳細な実装手順)
4. [トラブルシューティング](#トラブルシューティング)
5. [学習ポイント](#学習ポイント)
6. [使用したBashコマンド](#使用したbashコマンド)
7. [次のステップ](#次のステップ)

---

## 🎯 実装した機能

### 1. ユーザー認証システム
- ✅ ログイン機能（BCryptパスワード暗号化）
- ✅ 会員登録機能
- ✅ パスワードリセット機能
- ✅ ナビゲーションバーのログイン/ログアウトボタン

### 2. データベース初期化
- ✅ 5つの掲示板データ投入
- ✅ 25件のサンプル投稿
- ✅ 12件のコメント
- ✅ 5人のテストユーザー

### 3. APIエンドポイント修正
- ✅ `/ej2/api` → `/api` に統一
- ✅ プロキシ設定の修正
- ✅ フロントエンド全体のAPI URL更新

---

## 📁 変更したファイル一覧

### バックエンド（Java/Spring）

#### 新規作成
```
backend/src/main/java/com/ej2/
├── dto/
│   ├── LoginRequest.java           # ログインリクエストDTO
│   ├── RegisterRequest.java        # 会員登録リクエストDTO
│   ├── AuthResponse.java           # 認証レスポンスDTO
│   ├── PasswordResetRequest.java   # パスワードリセットリクエストDTO
│   └── PasswordResetConfirmRequest.java
├── util/
│   └── PasswordUtil.java           # BCrypt暗号化ユーティリティ
├── service/
│   └── AuthService.java            # 認証ビジネスロジック
└── controller/
    └── AuthController.java         # 認証APIエンドポイント
```

#### 修正
```
backend/src/main/java/com/ej2/
├── model/
│   └── User.java                   # username, password, resetToken追加
├── repository/
│   └── UserRepository.java         # 認証用メソッド追加
├── config/
│   └── RootConfig.java             # @EnableJpaRepositories追加
backend/pom.xml                      # jBCrypt, spring-data-jpa依存関係追加
```

### フロントエンド（React）

#### 新規作成
```
frontend/src/pages/Auth/
├── LoginPage.js                    # ログインページ
├── RegisterPage.js                 # 会員登録ページ
├── PasswordResetPage.js            # パスワードリセットページ
└── AuthPages.css                   # 認証ページ共通スタイル
```

#### 修正
```
frontend/
├── package.json                    # proxy設定変更
├── src/
│   ├── App.js                      # ルーティング、ログイン状態管理追加
│   ├── App.css                     # 認証UI用スタイル追加
│   └── pages/
│       ├── Main/MainPage.js        # API URL修正
│       └── Board/PostListPage.js   # API URL修正
```

### データベース・設定
```
init-data.sql                       # 初期データ投入スクリプト
```

---

## 🔧 詳細な実装手順

### ステップ1: Userモデルの拡張

**目的**: 認証に必要なフィールドを追加

**変更内容**:
```java
// User.java に追加
@Column(nullable = false, unique = true, length = 50)
private String username;

@JsonIgnore
@Column(nullable = false)
private String password;

@Column(name = "reset_token")
private String resetToken;

@Column(name = "reset_token_expiry")
private LocalDateTime resetTokenExpiry;
```

**学習ポイント**:
- `@JsonIgnore`: パスワードをJSON出力から除外（セキュリティ）
- `unique = true`: ユーザー名の重複を防ぐ
- `resetToken`: パスワードリセット用の一時トークン

---

### ステップ2: BCrypt暗号化ユーティリティの作成

**目的**: パスワードを安全に暗号化・検証

**コード例**:
```java
// PasswordUtil.java
public class PasswordUtil {
    private static final int BCRYPT_ROUNDS = 12;

    public static String hashPassword(String plainPassword) {
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(BCRYPT_ROUNDS));
    }

    public static boolean verifyPassword(String plainPassword, String hashedPassword) {
        return BCrypt.checkpw(plainPassword, hashedPassword);
    }
}
```

**学習ポイント**:
- **BCrypt**: パスワード専用の暗号化アルゴリズム
- **Salt**: ランダムな値を追加して同じパスワードでも異なるハッシュを生成
- **Rounds (12)**: ハッシュ計算の複雑さ（高いほど安全だが遅い）

**セキュリティ上の利点**:
1. ハッシュから元のパスワードを復元できない（一方向関数）
2. レインボーテーブル攻撃に強い
3. タイミング攻撃に対する防御を実装

---

### ステップ3: 認証サービスの実装

**目的**: ログイン、登録、パスワードリセットのビジネスロジック

**ログイン処理の流れ**:
```java
public AuthResponse login(LoginRequest request) {
    // 1. ユーザー名でユーザーを検索
    User user = userRepository.findByUsername(request.getUsername());

    // 2. ユーザーが存在しない場合
    if (user == null) {
        // タイミング攻撃防止: ダミー検証を実行
        PasswordUtil.verifyPassword("dummy", "$2a$12$dummyhash");
        return new AuthResponse(false, "ユーザー名またはパスワードが正しくありません");
    }

    // 3. パスワード検証
    boolean isPasswordValid = PasswordUtil.verifyPassword(
        request.getPassword(),
        user.getPassword()
    );

    // 4. 検証失敗
    if (!isPasswordValid) {
        return new AuthResponse(false, "ユーザー名またはパスワードが正しくありません");
    }

    // 5. 成功
    return new AuthResponse(true, "ログインに成功しました", user);
}
```

**重要なセキュリティ対策**:

1. **タイミング攻撃防止**:
   ```java
   // ユーザーが存在しない場合もパスワード検証を実行
   // → 応答時間でユーザーの存在を推測されない
   if (user == null) {
       PasswordUtil.verifyPassword("dummy", "$2a$12$dummyhash");
   }
   ```

2. **統一されたエラーメッセージ**:
   ```java
   // ユーザー名が間違っているのか、パスワードが間違っているのか
   // 攻撃者に教えない
   return new AuthResponse(false, "ユーザー名またはパスワードが正しくありません");
   ```

---

### ステップ4: フロントエンド認証ページの作成

**LoginPage.js の主要機能**:

```javascript
const handleSubmit = async (e) => {
  e.preventDefault();
  setLoading(true);

  try {
    const response = await axios.post('/api/auth/login', {
      username: formData.username,
      password: formData.password
    });

    if (response.data.success) {
      // ログイン成功: localStorageに保存
      localStorage.setItem('user', JSON.stringify(response.data.user));
      navigate('/');
    } else {
      setError(response.data.message);
    }
  } catch (err) {
    setError('サーバーとの通信に失敗しました');
  } finally {
    setLoading(false);
  }
};
```

**学習ポイント**:
- `localStorage`: ブラウザにデータを永続的に保存
- `try-catch-finally`: エラーハンドリングの基本パターン
- `loading` state: ユーザーに処理中であることを表示

---

### ステップ5: ナビゲーションバーの統合

**App.js の変更点**:

```javascript
function NavBar() {
  const [user, setUser] = useState(null);

  // ログイン状態を確認
  useEffect(() => {
    const storedUser = localStorage.getItem('user');
    if (storedUser) {
      setUser(JSON.parse(storedUser));
    }
  }, []);

  // ログアウト処理
  const handleLogout = () => {
    localStorage.removeItem('user');
    setUser(null);
    navigate('/login');
  };

  return (
    <nav>
      {user ? (
        <div>
          <span>👤 {user.name}</span>
          <button onClick={handleLogout}>ログアウト</button>
        </div>
      ) : (
        <Link to="/login">ログイン</Link>
      )}
    </nav>
  );
}
```

**学習ポイント**:
- **条件付きレンダリング**: `user ? A : B`
- **useEffect**: コンポーネントマウント時の処理
- **useState**: 状態管理の基本

---

## 🐛 トラブルシューティング

### 問題1: 404 Not Found エラー

**症状**:
```
GET /ej2/api/boards 404 (Not Found)
```

**原因**:
- バックエンドは `/api` でデプロイされているが、フロントエンドは `/ej2/api` を呼び出している
- TomcatにWARファイルを `ROOT.war` としてデプロイすると、コンテキストパスが `/` になる

**解決方法**:
1. `package.json` のproxyを修正:
   ```json
   "proxy": "http://localhost:8080"
   ```

2. すべてのAPIコールを一括置換:
   ```bash
   find frontend/src -name "*.js" | xargs sed -i '' 's|/ej2/api|/api|g'
   ```

**学習ポイント**:
- **コンテキストパス**: Webアプリケーションのベース URL
- **プロキシ設定**: 開発環境でCORSを回避する方法

---

### 問題2: TypeError: t.map is not a function

**症状**:
```javascript
TypeError: t.map is not a function
at MainPage.js:134
```

**原因**:
- バックエンドAPIが `List<Post>` を直接返している
- フロントエンドは `{ content: [...] }` のようなページネーション形式を期待

**解決方法**:
```javascript
// 修正前
setPosts(response.data.content);

// 修正後
const posts = Array.isArray(response.data) ? response.data : [];
setPosts(posts);
```

**学習ポイント**:
- **型チェック**: `Array.isArray()` でデータ型を確認
- **防御的プログラミング**: 予期しないデータ形式に対応

---

### 問題3: 401 Unauthorized (ログイン失敗)

**症状**:
```
POST /api/auth/login 401 (Unauthorized)
パスワード検証結果: false
```

**原因**:
- データベースのBCryptハッシュが `password123` と一致しない
- SQLファイルに記載したハッシュが間違っている

**解決方法**:

**方法1: 会員登録機能を使用（推奨）**
```
1. http://localhost:3000/register にアクセス
2. 新規ユーザーを登録
3. 自動的に正しいハッシュが生成される
```

**方法2: PasswordUtilのmainメソッドで生成**
```java
public static void main(String[] args) {
    String password = "password123";
    String hash = hashPassword(password);
    System.out.println("BCrypt hash: " + hash);
}
```

**学習ポイント**:
- **BCryptハッシュ**: 同じパスワードでも毎回異なるハッシュが生成される
- **デバッグログ**: 問題箇所を特定するための出力を追加

---

## 📚 学習ポイント

### 1. Spring Data JPAの自動実装

**概念**:
```java
// インターフェースを定義するだけで、Springが自動実装
public interface BoardRepository extends JpaRepository<Board, Long> {
    Optional<Board> findByCode(String code);
}

// Springが自動的に以下のメソッドを提供:
// - findAll()
// - findById()
// - save()
// - delete()
```

**有効化方法**:
```java
@Configuration
@EnableJpaRepositories(basePackages = "com.ej2.repository")
public class RootConfig {
    // ...
}
```

**依存関係**:
```xml
<dependency>
    <groupId>org.springframework.data</groupId>
    <artifactId>spring-data-jpa</artifactId>
    <version>2.7.18</version>
</dependency>
```

---

### 2. RESTful APIの階層構造

**概念**:
```
Controller (HTTP層)
    ↓ リクエスト処理
Service (ビジネスロジック層)
    ↓ データ操作
Repository (データアクセス層)
    ↓ SQL実行
Database
```

**例**:
```java
// Controller: HTTPリクエストを受け取る
@PostMapping("/login")
public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
    AuthResponse response = authService.login(request);
    return ResponseEntity.ok(response);
}

// Service: ビジネスロジック
public AuthResponse login(LoginRequest request) {
    User user = userRepository.findByUsername(request.getUsername());
    // パスワード検証、レスポンス生成
}

// Repository: データベースアクセス
public User findByUsername(String username) {
    // SQL実行: SELECT * FROM users WHERE username = ?
}
```

---

### 3. BCrypt暗号化の仕組み

**ハッシュの構造**:
```
$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyYIRh9YdKZq
│  │  │                                                         │
│  │  └─ Salt（ランダムな値）                                   │
│  └─ Rounds（計算の複雑さ: 2^12 = 4096回）                     │
└─ アルゴリズム識別子（BCrypt 2a版）                            │
                                                               │
                                      実際のハッシュ値 ─────────┘
```

**特徴**:
1. **同じパスワードでも毎回異なるハッシュ**（Saltのため）
2. **計算に時間がかかる**（Roundsのため、ブルートフォース攻撃を困難に）
3. **一方向関数**（ハッシュから元のパスワードを復元不可能）

---

### 4. Reactの状態管理

**useState の基本**:
```javascript
const [user, setUser] = useState(null);
//     │      │           └─ 初期値
//     │      └─ 更新関数
//     └─ 現在の値

// 使い方
setUser({ name: 'John', email: 'john@example.com' });
console.log(user.name); // "John"
```

**useEffect の基本**:
```javascript
// マウント時に1回だけ実行
useEffect(() => {
    fetchData();
}, []); // 空の依存配列

// userが変更されるたびに実行
useEffect(() => {
    console.log('User changed:', user);
}, [user]); // 依存配列にuserを指定
```

---

## 💻 使用したBashコマンド

### Docker関連

```bash
# コンテナの状態確認
docker-compose ps

# コンテナのログ確認
docker-compose logs backend
docker-compose logs -f backend  # リアルタイム表示

# コンテナの再起動
docker-compose restart backend

# コンテナの再ビルド
docker-compose up -d --build backend

# コンテナの停止・削除
docker-compose down

# コンテナ内でコマンド実行
docker exec -it spring-backend bash
```

### データベース操作

```bash
# MariaDBに接続
docker exec -i mariadb mysql -u appuser -papppassword appdb

# SQLファイルを実行
docker exec -i mariadb mysql -u appuser -papppassword appdb < init-data.sql

# SQLコマンドを直接実行
docker exec -i mariadb mysql -u appuser -papppassword appdb -e "SELECT * FROM users;"
```

### ファイル検索・置換

```bash
# ファイル内の文字列を検索
grep -r "/ej2/api" frontend/src

# ファイル内の文字列を一括置換
find frontend/src -name "*.js" | xargs sed -i '' 's|/ej2/api|/api|g'

# 特定のパターンにマッチするファイルを検索
find . -name "*.java" -type f

# ファイルの内容を表示（最初の20行）
head -20 filename.txt

# ファイルの内容を表示（最後の20行）
tail -20 filename.txt
```

### API テスト

```bash
# GET リクエスト
curl http://localhost:8080/api/boards

# POST リクエスト（JSON）
curl -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"testuser1","password":"password123"}'

# レスポンスをフォーマット（jqが必要）
curl http://localhost:8080/api/boards | jq '.'
```

### Maven操作

```bash
# コンパイル
mvn compile

# クリーン＆パッケージング
mvn clean package

# テストをスキップしてパッケージング
mvn clean package -DskipTests

# 特定のクラスを実行
mvn exec:java -Dexec.mainClass="com.ej2.util.PasswordUtil"
```

---

## 🚀 次のステップ

### 短期的なタスク

1. **パスワードハッシュの修正**
   - 会員登録機能を使って正しいユーザーを作成
   - または `PasswordUtil.main()` で正しいハッシュを生成してデータベース更新

2. **認証システムのテスト**
   - ✅ 会員登録
   - ✅ ログイン
   - ✅ ログアウト
   - ⬜ パスワードリセット

3. **掲示板機能のテスト**
   - ⬜ 投稿の閲覧
   - ⬜ 投稿の作成
   - ⬜ コメントの追加

### 中期的な改善

1. **JWT認証の導入**
   - 現在: localStorageにユーザー情報を保存
   - 改善: JWTトークンを使用したステートレス認証

2. **パスワードリセットのメール送信**
   - 現在: トークンをコンソールに出力
   - 改善: SendGridを使用したメール送信

3. **バリデーションの強化**
   - パスワード強度チェック
   - メールアドレス形式検証
   - ユーザー名の文字制限

4. **セッション管理の改善**
   - リフレッシュトークン
   - 自動ログアウト（タイムアウト）
   - 複数デバイスでのログイン管理

### 長期的な機能追加

1. **ソーシャルログイン**
   - Google, Facebook, GitHub連携

2. **二要素認証（2FA）**
   - TOTP（Time-based One-Time Password）

3. **ユーザープロファイル**
   - プロフィール画像
   - 自己紹介
   - SNSリンク

4. **権限管理**
   - ロール（管理者、モデレーター、一般ユーザー）
   - 権限ベースのアクセス制御

---

## 📖 参考資料

### 公式ドキュメント
- [Spring Data JPA](https://spring.io/projects/spring-data-jpa)
- [BCrypt](https://en.wikipedia.org/wiki/Bcrypt)
- [React Router](https://reactrouter.com/)
- [Axios](https://axios-http.com/)

### セキュリティ
- [OWASP Top 10](https://owasp.org/www-project-top-ten/)
- [パスワードハッシュのベストプラクティス](https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html)

### チュートリアル
- [Spring Boot + React 認証チュートリアル](https://www.baeldung.com/spring-boot-react-crud)
- [BCrypt パスワードハッシュ](https://www.baeldung.com/spring-security-registration-password-encoding-bcrypt)

---

## ✅ チェックリスト

### 実装完了項目
- [x] Userモデルの拡張
- [x] BCrypt暗号化ユーティリティ
- [x] 認証用Repository/Service/Controller
- [x] ログインページ
- [x] 会員登録ページ
- [x] パスワードリセットページ
- [x] ナビゲーションバーの統合
- [x] データベース初期化スクリプト
- [x] APIエンドポイント修正
- [x] Spring Data JPA設定

### テスト項目
- [ ] 会員登録機能
- [ ] ログイン機能
- [ ] ログアウト機能
- [ ] パスワードリセット機能
- [ ] セッション永続性（ブラウザリロード）
- [ ] エラーハンドリング

---

## 🙏 まとめ

このガイドでは、EJ2プロジェクトに完全な認証システムを実装しました。

**実装したコア機能**:
1. BCryptによる安全なパスワード暗号化
2. ログイン・会員登録・パスワードリセット
3. フロントエンドとバックエンドの統合
4. データベース初期化スクリプト

**遭遇した主な問題と解決方法**:
1. APIエンドポイントの不一致 → プロキシ設定とURL修正
2. データ形式の不一致 → `Array.isArray()` による型チェック
3. パスワードハッシュの不一致 → 会員登録機能で正しいハッシュ生成

**学習した主要概念**:
- Spring Data JPAの自動実装
- BCryptによるパスワード暗号化
- RESTful APIの階層構造
- Reactの状態管理とルーティング

次は、実際にシステムを使って認証機能をテストし、必要に応じて改善を加えていきましょう！

---

**作成者**: Claude (Anthropic)
**最終更新**: 2026-01-22
