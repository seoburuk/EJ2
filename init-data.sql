-- EJ2 初期データ投入スクリプト
-- 実行前に既存データをクリーンアップ

USE appdb;

-- 外部キー制約を一時的に無効化
SET FOREIGN_KEY_CHECKS = 0;

-- 既存データを削除（テスト用）
TRUNCATE TABLE comments;
TRUNCATE TABLE posts;
TRUNCATE TABLE boards;
TRUNCATE TABLE users;

-- 外部キー制約を再有効化
SET FOREIGN_KEY_CHECKS = 1;

-- ユーザーデータの投入
-- パスワードは "password123" をBCryptでハッシュ化したもの
INSERT INTO users (username, name, email, password, created_at, updated_at) VALUES
('testuser1', '田中太郎', 'tanaka@example.com', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyYIRh9YdKZq', NOW(), NOW()),
('testuser2', '佐藤花子', 'sato@example.com', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyYIRh9YdKZq', NOW(), NOW()),
('testuser3', '鈴木一郎', 'suzuki@example.com', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyYIRh9YdKZq', NOW(), NOW()),
('testuser4', '高橋美咲', 'takahashi@example.com', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyYIRh9YdKZq', NOW(), NOW()),
('testuser5', '伊藤健太', 'ito@example.com', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyYIRh9YdKZq', NOW(), NOW());

-- 掲示板データの投入
INSERT INTO boards (code, name, description, is_anonymous, created_at) VALUES
('GENERAL', '自由掲示板', '自由に話題を共有する掲示板です', false, NOW()),
('ANONYMOUS', '匿名掲示板', '匿名で自由に意見を交換できます', true, NOW()),
('EVENT', 'イベント掲示板', '大学のイベント情報を共有します', false, NOW()),
('MARKET', '中古市場', '中古品を売買する掲示板です', false, NOW()),
('BEST', 'ベスト掲示板', '人気投稿が集まる掲示板です', false, NOW());

-- 投稿データの投入
-- 自由掲示板の投稿
INSERT INTO posts (board_id, user_id, title, content, view_count, like_count, dislike_count, created_at, updated_at) VALUES
(1, 1, '時間割管理システムの使い方', '新しく追加された時間割機能、めちゃくちゃ便利ですね！使い方を共有します。', 234, 45, 5, DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)),
(1, 2, 'おすすめの教養科目教えてください', '来学期の履修登録で悩んでいます。おすすめの教養科目があれば教えてください！', 156, 38, 3, DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)),
(1, 3, '図書館の座席予約のコツ', '試験期間の図書館座席予約が激戦すぎる...コツがあれば教えてください。', 289, 56, 7, DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY)),
(1, 1, '履修登録期間について', '履修登録の開始日時が変更されたそうです。要確認！', 120, 10, 2, DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY)),
(1, 4, 'サークル新歓情報', '各サークルの新歓イベント情報をまとめました。新入生の方はぜひ！', 198, 32, 4, DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY)),
(1, 5, 'キャンパスWi-Fiが繋がりにくい', '最近キャンパスのWi-Fiが不安定な気がします。同じ症状の方いますか？', 167, 23, 6, DATE_SUB(NOW(), INTERVAL 4 DAY), DATE_SUB(NOW(), INTERVAL 4 DAY)),
(1, 2, 'おすすめのカフェ教えて！', 'キャンパス周辺でレポート作業に最適なカフェを探しています。', 145, 19, 1, DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY));

-- 匿名掲示板の投稿
INSERT INTO posts (board_id, user_id, title, content, view_count, like_count, dislike_count, created_at, updated_at) VALUES
(2, 5, '授業で寝てる人多すぎ', '1限の授業、毎回半分くらいの人が寝てる...教授も気づいてるよね？', 456, 67, 12, DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)),
(2, 1, '教授の評判ってどう？', '来学期取ろうと思ってる授業の教授、評判悪いって聞いたんだけど本当？', 534, 89, 8, DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)),
(2, 2, 'この大学選んでよかった', '入学前は不安だったけど、今は本当にこの大学に来てよかったと思ってる。', 567, 78, 5, DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY)),
(2, 3, '課題が多すぎる件', '毎週レポート3つとか無理ゲーすぎる...みんなどうやってこなしてるの？', 345, 28, 9, DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY)),
(2, 4, '食堂のおすすめメニュー', '学食のカレーライスが意外と美味しい。みんなのおすすめメニューは？', 189, 22, 3, DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY)),
(2, 5, 'バイトと勉強の両立', 'バイト週4で入ってるけど、勉強との両立がキツイ...', 278, 35, 7, DATE_SUB(NOW(), INTERVAL 4 DAY), DATE_SUB(NOW(), INTERVAL 4 DAY));
-- イベント掲示板の投稿
INSERT INTO posts (board_id, user_id, title, content, view_count, like_count, dislike_count, created_at, updated_at) VALUES
(3, 1, '学園祭ボランティア募集', '今年の学園祭でボランティアスタッフを募集しています！興味のある方はぜひご参加ください。', 423, 42, 3, DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)),
(3, 2, '就活セミナー開催のお知らせ', '3年生向けの就活セミナーを来月開催します。OB・OGの方々からお話を聞けます。', 634, 95, 5, DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY)),
(3, 3, 'スポーツ大会参加者募集', '学内スポーツ大会のフットサルチーム、メンバー募集中です！', 256, 31, 2, DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY)),
(3, 4, '交換留学説明会', '来年度の交換留学プログラムについての説明会を開催します。', 498, 72, 4, DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY)),
(3, 5, '図書館イベント情報', '図書館で読書会を開催します。興味のある本について語り合いましょう！', 187, 19, 1, DATE_SUB(NOW(), INTERVAL 4 DAY), DATE_SUB(NOW(), INTERVAL 4 DAY)),
(3, 1, 'プログラミング勉強会', '初心者向けのプログラミング勉強会を毎週土曜日に開催しています。', 312, 48, 6, DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY));

-- 中古市場の投稿
INSERT INTO posts (board_id, user_id, title, content, view_count, like_count, dislike_count, created_at, updated_at) VALUES
(4, 1, '教科書売ります（経済学入門）', '「経済学入門 第3版」ほぼ新品です。定価3,500円を2,000円でお譲りします。', 345, 28, 2, DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)),
(4, 2, 'ノートPC譲ります', 'MacBook Air 2020年モデル、使用感少なめ。65,000円希望です。', 689, 103, 3, DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)),
(4, 3, '自転車探してます', 'キャンパス通学用の自転車を探しています。予算1万円程度。', 234, 16, 1, DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY)),
(4, 4, '電子辞書買取希望', 'CASIO製の電子辞書、使わなくなったので買い取ってくれる方募集。', 198, 12, 1, DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY)),
(4, 5, '家具無料で差し上げます', '引っ越しで不要になった家具、取りに来ていただける方に無料で差し上げます。', 434, 58, 4, DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY)),
(4, 1, 'TOEIC参考書セット', 'TOEICの参考書3冊セット、ほぼ未使用。4,000円でお譲りします。', 223, 21, 2, DATE_SUB(NOW(), INTERVAL 4 DAY), DATE_SUB(NOW(), INTERVAL 4 DAY));
-- コメントデータの投入
INSERT INTO comments (post_id, user_id, content, like_count, created_at, updated_at) VALUES
-- 時間割管理システムの投稿へのコメント
(1, 2, 'スクリーンショット機能が便利ですよね！', 5, DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)),
(1, 3, '時間割の共有機能もあるといいな...', 8, DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)),
(1, 4, 'スマホ対応してくれたら完璧です！', 12, DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY)),

-- おすすめ教養科目の投稿へのコメント
(2, 1, '哲学入門がおすすめです！先生が面白い。', 15, DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)),
(2, 3, '心理学も人気ですよ。ただし試験は難しめ。', 10, DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)),
(2, 4, '芸術系の科目は楽単多いですよ〜', 7, DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY)),

-- 図書館座席予約の投稿へのコメント
(3, 2, '予約開始時刻ぴったりにアクセスするしかない...', 23, DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY)),
(3, 4, '平日の午前中なら比較的空いてますよ', 18, DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY)),

-- ノートPC譲渡の投稿へのコメント
(11, 3, 'まだ在庫ありますか？購入希望です！', 8, DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)),
(11, 4, 'スペックと使用期間を教えていただけますか？', 5, DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)),

-- 就活セミナーの投稿へのコメント
(8, 5, '参加申し込みはどこからできますか？', 12, DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY)),
(8, 1, '去年参加しましたが、とても参考になりました！', 15, DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY));

-- 統計情報の確認
SELECT 'Users' AS table_name, COUNT(*) AS count FROM users
UNION ALL
SELECT 'Boards', COUNT(*) FROM boards
UNION ALL
SELECT 'Posts', COUNT(*) FROM posts
UNION ALL
SELECT 'Comments', COUNT(*) FROM comments;

-- 完了メッセージ
SELECT '初期データの投入が完了しました！' AS message;
SELECT 'テストユーザー: testuser1 〜 testuser5' AS info;
SELECT 'パスワード: password123' AS info;
