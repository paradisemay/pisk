
BEGIN;

INSERT INTO users (username, password_hash, password_salt, password_alg, avatar_path, role) VALUES
('timofey', decode('cf2727de92eeeb80bdaa6a3fc08597894882acdc976fea308dec81f2a5966134da2efc3721c544c9f25f62a34b57a5de','hex'), decode('65cfc60f08fe727d5693726f46d79e97','hex'), 'SHA-384', '/avatars/timofey.png', 0),
('masha',   decode('5a442fd4fd014187d4151a8e9b239a99d8617ed78ca765e905915eea5b17298cfec0389a9df239d72f5c77332863932a','hex'), decode('359e446afcb74c5e1667c17a59a2e7db','hex'), 'SHA-384', '/avatars/masha.png',   0),
('petr',    decode('1373c60dcfe26c901aa3d29645c2236e781fb70087540326339538898b43412a005d8beac5f434efaae6213a860b3c6e','hex'),  decode('3d75059ab140533addc1929967bf5668','hex'), 'SHA-384', '/avatars/petr.png',    0),
('olga',    decode('a48190f92414aadb451febf9f4f2e776e2556dce4385d613f26501b6992385ab36ef894611e88a53ae0ca8e75e69b3c2','hex'), decode('fa8a2e7b6b72c6fc40246280cc7982c4','hex'), 'SHA-384', '/avatars/olga.png',    0),
('ivan',    decode('b6d19d708154c379ff4f7c8ae6feb4ae3becec5edd07374ebd0cc7560ab6c01e84ab67b419613945fbf8a3cf20ecb87a','hex'), decode('87a6c9d9566cc8691a4cd7887c97910d','hex'), 'SHA-384', '/avatars/ivan.png',    0),
('sveta',   decode('d8f140855c88e277689309df6ce85742c7dfbfeccc3d278269330931690108c5bee0b512266528436f8743172d7991ec','hex'), decode('82545d01e33be271ed7ebf6abfdebc9e','hex'), 'SHA-384', '/avatars/sveta.png',   0),
('sergey',  decode('9bccdfb667e5132d6b326a5bbc7a148bf1b78f63624b20b5f9edd273d3903a9d526426432b6870a7eeab3b3b7db0815c','hex'), decode('7245cce8f1aa967e9a4ec84f5399eab0','hex'), 'SHA-384', '/avatars/sergey.png',  0),
('diana',   decode('e05f55cc7852da4f81630b859c9dc3485e4f440240e3c46e14f768f7a92ef19def8373afa1727a3c352bdd7d0f2410ba','hex'), decode('0d080b68f534c9abcc214904406c3885','hex'), 'SHA-384', '/avatars/diana.png',   0),
('admin',   decode('1b70f4d2b2d60dd3a39a3b33e2bc8858238b7dffd4daf5c6a0e5433779f7e3f6829ee4aac3676b66929c401597940d5b','hex'), decode('1bd7ba6971494c05259be303717fd35d','hex'), 'SHA-384', '/avatars/admin.png',   1)
ON CONFLICT (username) DO NOTHING;

INSERT INTO label (name, description) VALUES
 ('Creation Records', 'UK independent label founded in 1983'),
 ('FRWRD MUSIC',      'Цифровой музыкальный дистрибьютор/лейбл'),
 ('Parlophone',       'British record label'),
 ('Columbia',         'US major label'),
 ('Virgin',           'British record label'),
 ('DGC',              'David Geffen Company'),
 ('Harvest',          'British label (EMI)'),
 ('Apple',            'Apple Records (The Beatles)'),
 ('Interscope',       'US major label')
ON CONFLICT (name) DO NOTHING;

INSERT INTO genre (title, description, color) VALUES
 ('Shoegaze',            'Shoegaze',                        '#FF66CC'),
 ('Noise Pop',           'Noise pop',                       '#FF99CC'),
 ('Dream Pop',           'Dream pop',                       '#CC99FF'),
 ('Alternative Rock',    'Alternative rock',                '#6699FF'),
 ('Grunge',              'Grunge',                          '#336699'),
 ('Progressive Rock',    'Progressive rock',                '#33CCCC'),
 ('Psychedelic Rock',    'Psychedelic rock',                '#66CCCC'),
 ('Rock',                'Classic/Pop rock',                '#999999'),
 ('Hip Hop',             'Hip hop / rap',                   '#FFCC33'),
 ('Jazz Rap',            'Jazz rap (TPAB)',                 '#FFDD66'),
 ('G-Funk',              'G-funk (TPAB)',                   '#FFBB33'),
 ('Electronic',          'Electronic music',                '#33CC99'),
 ('French House',        'French house (Discovery)',        '#66FFCC'),
 ('Disco',               'Disco / nu-disco',                '#FFCC99')
ON CONFLICT (title) DO NOTHING;

INSERT INTO artist (name, description, avatar_path)
SELECT 'My Bloody Valentine', 'Irish-English shoegaze band', '/artist/mbv.png'
WHERE NOT EXISTS (SELECT 1 FROM artist WHERE name='My Bloody Valentine');

INSERT INTO artist (name, description, avatar_path)
SELECT 'Куок', 'КУОК / QUOK', '/artist/quok.png'
WHERE NOT EXISTS (SELECT 1 FROM artist WHERE name='Куок');

INSERT INTO artist (name, description, avatar_path)
SELECT 'Radiohead', 'English rock band', '/artist/radiohead.png'
WHERE NOT EXISTS (SELECT 1 FROM artist WHERE name='Radiohead');

INSERT INTO artist (name, description, avatar_path)
SELECT 'Kendrick Lamar', 'US rapper', '/artist/kdot.png'
WHERE NOT EXISTS (SELECT 1 FROM artist WHERE name='Kendrick Lamar');

INSERT INTO artist (name, description, avatar_path)
SELECT 'Daft Punk', 'French electronic duo', '/artist/daftpunk.png'
WHERE NOT EXISTS (SELECT 1 FROM artist WHERE name='Daft Punk');

INSERT INTO artist (name, description, avatar_path)
SELECT 'Nirvana', 'US rock band', '/artist/nirvana.png'
WHERE NOT EXISTS (SELECT 1 FROM artist WHERE name='Nirvana');

INSERT INTO artist (name, description, avatar_path)
SELECT 'Pink Floyd', 'English rock band', '/artist/pinkfloyd.png'
WHERE NOT EXISTS (SELECT 1 FROM artist WHERE name='Pink Floyd');

INSERT INTO artist (name, description, avatar_path)
SELECT 'The Beatles', 'English rock band', '/artist/beatles.png'
WHERE NOT EXISTS (SELECT 1 FROM artist WHERE name='The Beatles');

WITH g AS (SELECT title, id FROM genre),
     l AS (SELECT name,  id FROM label)
INSERT INTO album
(name, description, cover_url, genre, track_number, explicit, inoagent, avg_rating, label_id, publication_date)
VALUES
 ('Loveless',
  'Second studio album by My Bloody Valentine',
  'https://upload.wikimedia.org/wikipedia/en/8/8d/My_Bloody_Valentine_-_Loveless.png',
  (SELECT id FROM g WHERE title='Shoegaze'),
  11, FALSE, FALSE, 0,
  (SELECT id FROM l WHERE name='Creation Records'),
  '1991-11-04 00:00:00'),

 ('QTOPIA',
  'Альбом КУОК (Quok); релиз FRWRD MUSIC',
  'https://i.scdn.co/image/ab67616d0000b273-placeholder-qtopia',
  (SELECT id FROM g WHERE title='Hip Hop'),
  19, TRUE, FALSE, 0,
  (SELECT id FROM l WHERE name='FRWRD MUSIC'),
  '2025-09-26 00:00:00'),

 ('OK Computer',
  'Third studio album by Radiohead',
  'https://upload.wikimedia.org/wikipedia/en/a/a1/Radiohead.okcomputer.albumart.jpg',
  (SELECT id FROM g WHERE title='Alternative Rock'),
  12, FALSE, FALSE, 0,
  (SELECT id FROM l WHERE name='Parlophone'),
  '1997-06-16 00:00:00'),

 ('Random Access Memories',
  'Fourth studio album by Daft Punk',
  'https://upload.wikimedia.org/wikipedia/en/a/a7/Random_Access_Memories.jpg',
  (SELECT id FROM g WHERE title='Electronic'),
  13, FALSE, FALSE, 0,
  (SELECT id FROM l WHERE name='Columbia'),
  '2013-05-17 00:00:00'),

 ('Discovery',
  'Second studio album by Daft Punk',
  'https://upload.wikimedia.org/wikipedia/en/a/ae/Daft_Punk_-_Discovery.png',
  (SELECT id FROM g WHERE title='Electronic'),
  14, FALSE, FALSE, 0,
  (SELECT id FROM l WHERE name='Virgin'),
  '2001-03-12 00:00:00'),

 ('Nevermind',
  'Second studio album by Nirvana',
  'https://upload.wikimedia.org/wikipedia/en/b/b7/NirvanaNevermindalbumcover.jpg',
  (SELECT id FROM g WHERE title='Grunge'),
  12, FALSE, FALSE, 0,
  (SELECT id FROM l WHERE name='DGC'),
  '1991-09-24 00:00:00'),

 ('The Dark Side of the Moon',
  'Eighth studio album by Pink Floyd',
  'https://upload.wikimedia.org/wikipedia/en/3/3b/Dark_Side_of_the_Moon.png',
  (SELECT id FROM g WHERE title='Progressive Rock'),
  10, FALSE, FALSE, 0,
  (SELECT id FROM l WHERE name='Harvest'),
  '1973-03-01 00:00:00'),

 ('Abbey Road',
  'Eleventh studio album by The Beatles',
  'https://upload.wikimedia.org/wikipedia/en/4/42/Beatles_-_Abbey_Road.jpg',
  (SELECT id FROM g WHERE title='Rock'),
  17, FALSE, FALSE, 0,
  (SELECT id FROM l WHERE name='Apple'),
  '1969-09-26 00:00:00'),

 ('To Pimp a Butterfly',
  'Third studio album by Kendrick Lamar',
  'https://upload.wikimedia.org/wikipedia/en/9/9e/To_Pimp_a_Butterfly.png',
  (SELECT id FROM g WHERE title='Hip Hop'),
  16, TRUE, FALSE, 0,
  (SELECT id FROM l WHERE name='Interscope'),
  '2015-03-15 00:00:00'),

 ('good kid, m.A.A.d city',
  'Second studio album by Kendrick Lamar',
  'https://upload.wikimedia.org/wikipedia/en/0/05/KendrickGKMC.jpg',
  (SELECT id FROM g WHERE title='Hip Hop'),
  12, TRUE, FALSE, 0,
  (SELECT id FROM l WHERE name='Interscope'),
  '2012-10-22 00:00:00');

WITH
  u  AS (SELECT id, username FROM users),
  a  AS (SELECT id, name     FROM album),
  ar AS (SELECT id, name     FROM artist),
  g  AS (SELECT id, title    FROM genre)
INSERT INTO track
(name, author_id, duration, filesize, rating, rate_count, album_id, artist_id, genre_id, filepath, number_on_album, explicit, inoagent)
VALUES
('Only Shallow',    (SELECT id FROM u WHERE username='admin'),   257,  9388608, 0,0, (SELECT id FROM a WHERE name='Loveless'), (SELECT id FROM ar WHERE name='My Bloody Valentine'), (SELECT id FROM g WHERE title='Shoegaze'), '/music/mbv/loveless/only_shallow.flac', 1,  FALSE, FALSE),
('When You Sleep',  (SELECT id FROM u WHERE username='timofey'), 252,  9031680, 0,0, (SELECT id FROM a WHERE name='Loveless'), (SELECT id FROM ar WHERE name='My Bloody Valentine'), (SELECT id FROM g WHERE title='Shoegaze'), '/music/mbv/loveless/when_you_sleep.flac', 6,  FALSE, FALSE),
('Soon',            (SELECT id FROM u WHERE username='masha'),   420, 12582912, 0,0, (SELECT id FROM a WHERE name='Loveless'), (SELECT id FROM ar WHERE name='My Bloody Valentine'), (SELECT id FROM g WHERE title='Shoegaze'), '/music/mbv/loveless/soon.flac',          11, FALSE, FALSE),

('ENTER QTOPIA',        (SELECT id FROM u WHERE username='admin'), 140,  7340032, 0,0, (SELECT id FROM a WHERE name='QTOPIA'), (SELECT id FROM ar WHERE name='Куок'), (SELECT id FROM g WHERE title='Hip Hop'), '/music/quok/qtopia/enter_qtopia.flac',          1,  TRUE,  FALSE),
('НАС ОТКЛЮЧАЮТ',       (SELECT id FROM u WHERE username='sergey'),180,  8388608, 0,0, (SELECT id FROM a WHERE name='QTOPIA'), (SELECT id FROM ar WHERE name='Куок'), (SELECT id FROM g WHERE title='Hip Hop'), '/music/quok/qtopia/nas_otklyuchayut.flac',      5,  TRUE,  FALSE),
('МЫ СОЗДАЛИ МОНСТРА',  (SELECT id FROM u WHERE username='diana'), 195,  9437184, 0,0, (SELECT id FROM a WHERE name='QTOPIA'), (SELECT id FROM ar WHERE name='Куок'), (SELECT id FROM g WHERE title='Hip Hop'), '/music/quok/qtopia/my_sozdali_monstra.flac',   12,  TRUE,  FALSE),

('Paranoid Android', (SELECT id FROM u WHERE username='admin'), 383, 12582912, 0,0, (SELECT id FROM a WHERE name='OK Computer'), (SELECT id FROM ar WHERE name='Radiohead'), (SELECT id FROM g WHERE title='Alternative Rock'), '/music/radiohead/okc/paranoid_android.flac', 2, FALSE, FALSE),
('Karma Police',     (SELECT id FROM u WHERE username='olga'),  261,  9437184, 0,0, (SELECT id FROM a WHERE name='OK Computer'), (SELECT id FROM ar WHERE name='Radiohead'), (SELECT id FROM g WHERE title='Alternative Rock'), '/music/radiohead/okc/karma_police.flac',     6, FALSE, FALSE),
('No Surprises',     (SELECT id FROM u WHERE username='petr'),  228,  8388608, 0,0, (SELECT id FROM a WHERE name='OK Computer'), (SELECT id FROM ar WHERE name='Radiohead'), (SELECT id FROM g WHERE title='Alternative Rock'), '/music/radiohead/okc/no_surprises.flac',    10, FALSE, FALSE),

('Get Lucky',        (SELECT id FROM u WHERE username='ivan'),  369, 11534336, 0,0, (SELECT id FROM a WHERE name='Random Access Memories'), (SELECT id FROM ar WHERE name='Daft Punk'), (SELECT id FROM g WHERE title='Electronic'), '/music/daftpunk/ram/get_lucky.flac',   8,  FALSE, FALSE),
('Instant Crush',    (SELECT id FROM u WHERE username='sveta'), 337, 11010048, 0,0, (SELECT id FROM a WHERE name='Random Access Memories'), (SELECT id FROM ar WHERE name='Daft Punk'), (SELECT id FROM g WHERE title='Electronic'), '/music/daftpunk/ram/instant_crush.flac', 5,  FALSE, FALSE),
('Contact',          (SELECT id FROM u WHERE username='admin'), 379, 12058624, 0,0, (SELECT id FROM a WHERE name='Random Access Memories'), (SELECT id FROM ar WHERE name='Daft Punk'), (SELECT id FROM g WHERE title='Electronic'), '/music/daftpunk/ram/contact.flac',       13, FALSE, FALSE),

('One More Time',    (SELECT id FROM u WHERE username='timofey'),320, 10485760, 0,0, (SELECT id FROM a WHERE name='Discovery'), (SELECT id FROM ar WHERE name='Daft Punk'), (SELECT id FROM g WHERE title='Electronic'), '/music/daftpunk/discovery/one_more_time.flac', 1, FALSE, FALSE),
('Digital Love',     (SELECT id FROM u WHERE username='masha'),  300,  9961472, 0,0, (SELECT id FROM a WHERE name='Discovery'), (SELECT id FROM ar WHERE name='Daft Punk'), (SELECT id FROM g WHERE title='Electronic'), '/music/daftpunk/discovery/digital_love.flac',  3, FALSE, FALSE),
('Harder, Better, Faster, Stronger',
                     (SELECT id FROM u WHERE username='sergey'), 224,  7864320, 0,0, (SELECT id FROM a WHERE name='Discovery'), (SELECT id FROM ar WHERE name='Daft Punk'), (SELECT id FROM g WHERE title='Electronic'), '/music/daftpunk/discovery/hbfs.flac',          4, FALSE, FALSE),

('Smells Like Teen Spirit', (SELECT id FROM u WHERE username='admin'),301, 11010048, 0,0, (SELECT id FROM a WHERE name='Nevermind'), (SELECT id FROM ar WHERE name='Nirvana'), (SELECT id FROM g WHERE title='Grunge'), '/music/nirvana/nevermind/slts.flac', 1, FALSE, FALSE),
('Come As You Are',         (SELECT id FROM u WHERE username='petr'), 219,  8912896, 0,0, (SELECT id FROM a WHERE name='Nevermind'), (SELECT id FROM ar WHERE name='Nirvana'), (SELECT id FROM g WHERE title='Grunge'), '/music/nirvana/nevermind/come_as_you_are.flac', 3, FALSE, FALSE),
('Lithium',                 (SELECT id FROM u WHERE username='olga'), 257,  9437184, 0,0, (SELECT id FROM a WHERE name='Nevermind'), (SELECT id FROM ar WHERE name='Nirvana'), (SELECT id FROM g WHERE title='Grunge'), '/music/nirvana/nevermind/lithium.flac',         5, FALSE, FALSE),

('Time',        (SELECT id FROM u WHERE username='ivan'),  413, 13172736, 0,0, (SELECT id FROM a WHERE name='The Dark Side of the Moon'), (SELECT id FROM ar WHERE name='Pink Floyd'), (SELECT id FROM g WHERE title='Progressive Rock'), '/music/pinkfloyd/dsotm/time.flac',        4, FALSE, FALSE),
('Money',       (SELECT id FROM u WHERE username='sveta'), 382, 12582912, 0,0, (SELECT id FROM a WHERE name='The Dark Side of the Moon'), (SELECT id FROM ar WHERE name='Pink Floyd'), (SELECT id FROM g WHERE title='Progressive Rock'), '/music/pinkfloyd/dsotm/money.flac',       6, FALSE, FALSE),
('Us and Them', (SELECT id FROM u WHERE username='sergey'),462, 13631488, 0,0, (SELECT id FROM a WHERE name='The Dark Side of the Moon'), (SELECT id FROM ar WHERE name='Pink Floyd'), (SELECT id FROM g WHERE title='Progressive Rock'), '/music/pinkfloyd/dsotm/us_and_them.flac', 7, FALSE, FALSE),

('Come Together',     (SELECT id FROM u WHERE username='admin'), 259, 10092544, 0,0, (SELECT id FROM a WHERE name='Abbey Road'), (SELECT id FROM ar WHERE name='The Beatles'), (SELECT id FROM g WHERE title='Rock'), '/music/beatles/abbey_road/come_together.flac', 1, FALSE, FALSE),
('Something',         (SELECT id FROM u WHERE username='masha'), 182,  7340032, 0,0, (SELECT id FROM a WHERE name='Abbey Road'), (SELECT id FROM ar WHERE name='The Beatles'), (SELECT id FROM g WHERE title='Rock'), '/music/beatles/abbey_road/something.flac',      2, FALSE, FALSE),
('Here Comes the Sun',(SELECT id FROM u WHERE username='diana'), 185,  7602176, 0,0, (SELECT id FROM a WHERE name='Abbey Road'), (SELECT id FROM ar WHERE name='The Beatles'), (SELECT id FROM g WHERE title='Rock'), '/music/beatles/abbey_road/here_comes_the_sun.flac', 7, FALSE, FALSE),

('King Kunta',             (SELECT id FROM u WHERE username='admin'),   234,  8388608, 0,0, (SELECT id FROM a WHERE name='To Pimp a Butterfly'), (SELECT id FROM ar WHERE name='Kendrick Lamar'), (SELECT id FROM g WHERE title='Hip Hop'), '/music/kendrick/tpab/king_kunta.flac',           3,  TRUE, FALSE),
('Alright',                (SELECT id FROM u WHERE username='timofey'), 219,  7864320, 0,0, (SELECT id FROM a WHERE name='To Pimp a Butterfly'), (SELECT id FROM ar WHERE name='Kendrick Lamar'), (SELECT id FROM g WHERE title='Hip Hop'), '/music/kendrick/tpab/alright.flac',               7,  TRUE, FALSE),
('The Blacker the Berry',  (SELECT id FROM u WHERE username='petr'),    315,  9961472, 0,0, (SELECT id FROM a WHERE name='To Pimp a Butterfly'), (SELECT id FROM ar WHERE name='Kendrick Lamar'), (SELECT id FROM g WHERE title='Hip Hop'), '/music/kendrick/tpab/the_blacker_the_berry.flac', 13, TRUE, FALSE),

('Swimming Pools (Drank)', (SELECT id FROM u WHERE username='olga'),   234,  8388608, 0,0, (SELECT id FROM a WHERE name='good kid, m.A.A.d city'), (SELECT id FROM ar WHERE name='Kendrick Lamar'), (SELECT id FROM g WHERE title='Hip Hop'), '/music/kendrick/gkmc/swimming_pools.flac', 7,  TRUE, FALSE),
('Bitch, Don''t Kill My Vibe',(SELECT id FROM u WHERE username='ivan'),321, 11010048, 0,0, (SELECT id FROM a WHERE name='good kid, m.A.A.d city'), (SELECT id FROM ar WHERE name='Kendrick Lamar'), (SELECT id FROM g WHERE title='Hip Hop'), '/music/kendrick/gkmc/bdkmv.flac',          2,  TRUE, FALSE),
('m.A.A.d city',            (SELECT id FROM u WHERE username='sveta'), 345, 11534336, 0,0, (SELECT id FROM a WHERE name='good kid, m.A.A.d city'), (SELECT id FROM ar WHERE name='Kendrick Lamar'), (SELECT id FROM g WHERE title='Hip Hop'), '/music/kendrick/gkmc/maad_city.flac',      11, TRUE, FALSE);

INSERT INTO artist_track (artist_id, track_id)
SELECT t.artist_id, t.id FROM track t
ON CONFLICT DO NOTHING;

INSERT INTO track_genre (track_id, genre_id)
SELECT t.id, (SELECT id FROM genre WHERE title='Dream Pop')
FROM track t JOIN album al ON al.id = t.album_id
WHERE al.name='Loveless'
ON CONFLICT DO NOTHING;

INSERT INTO track_genre (track_id, genre_id)
SELECT t.id, (SELECT id FROM genre WHERE title='Noise Pop')
FROM track t JOIN album al ON al.id = t.album_id
WHERE al.name='Loveless'
ON CONFLICT DO NOTHING;

INSERT INTO track_genre (track_id, genre_id)
SELECT t.id, (SELECT id FROM genre WHERE title='French House')
FROM track t JOIN album al ON al.id = t.album_id
WHERE al.name='Discovery'
ON CONFLICT DO NOTHING;

INSERT INTO liked (user_id, track_id, created_at)
SELECT u.id, t.id, now() - (random()*'90 days'::interval)
FROM users u
CROSS JOIN LATERAL (SELECT id FROM track ORDER BY random() LIMIT 1) AS t
WHERE u.username IN ('timofey','masha','petr','olga','ivan','sveta','sergey','diana','admin')
ON CONFLICT DO NOTHING;

INSERT INTO liked (user_id, track_id) VALUES
 ((SELECT id FROM users WHERE username='timofey'), (SELECT id FROM track WHERE name='Only Shallow')),
 ((SELECT id FROM users WHERE username='masha'),   (SELECT id FROM track WHERE name='Karma Police')),
 ((SELECT id FROM users WHERE username='petr'),    (SELECT id FROM track WHERE name='No Surprises')),
 ((SELECT id FROM users WHERE username='olga'),    (SELECT id FROM track WHERE name='Get Lucky')),
 ((SELECT id FROM users WHERE username='ivan'),    (SELECT id FROM track WHERE name='One More Time')),
 ((SELECT id FROM users WHERE username='sveta'),   (SELECT id FROM track WHERE name='Time')),
 ((SELECT id FROM users WHERE username='sergey'),  (SELECT id FROM track WHERE name='Money')),
 ((SELECT id FROM users WHERE username='diana'),   (SELECT id FROM track WHERE name='Here Comes the Sun')),
 ((SELECT id FROM users WHERE username='admin'),   (SELECT id FROM track WHERE name='Paranoid Android')),
 ((SELECT id FROM users WHERE username='sergey'),  (SELECT id FROM track WHERE name='ENTER QTOPIA')),
 ((SELECT id FROM users WHERE username='diana'),   (SELECT id FROM track WHERE name='НАС ОТКЛЮЧАЮТ')),
 ((SELECT id FROM users WHERE username='masha'),   (SELECT id FROM track WHERE name='МЫ СОЗДАЛИ МОНСТРА')),
 ((SELECT id FROM users WHERE username='timofey'), (SELECT id FROM track WHERE name='Smells Like Teen Spirit')),
 ((SELECT id FROM users WHERE username='petr'),    (SELECT id FROM track WHERE name='Come As You Are')),
 ((SELECT id FROM users WHERE username='olga'),    (SELECT id FROM track WHERE name='Lithium')),
 ((SELECT id FROM users WHERE username='ivan'),    (SELECT id FROM track WHERE name='Swimming Pools (Drank)')),
 ((SELECT id FROM users WHERE username='sveta'),   (SELECT id FROM track WHERE name='Bitch, Don''t Kill My Vibe')),
 ((SELECT id FROM users WHERE username='sergey'),  (SELECT id FROM track WHERE name='m.A.A.d city')),
 ((SELECT id FROM users WHERE username='admin'),   (SELECT id FROM track WHERE name='The Blacker the Berry')),
 ((SELECT id FROM users WHERE username='masha'),   (SELECT id FROM track WHERE name='Digital Love'))
ON CONFLICT DO NOTHING;

INSERT INTO user_rating (user_id, track_id, rate) VALUES
 ((SELECT id FROM users WHERE username='timofey'), (SELECT id FROM track WHERE name='Only Shallow'), 5),
 ((SELECT id FROM users WHERE username='masha'),   (SELECT id FROM track WHERE name='Only Shallow'), 4),
 ((SELECT id FROM users WHERE username='petr'),    (SELECT id FROM track WHERE name='Paranoid Android'), 5),
 ((SELECT id FROM users WHERE username='olga'),    (SELECT id FROM track WHERE name='Karma Police'), 5),
 ((SELECT id FROM users WHERE username='ivan'),    (SELECT id FROM track WHERE name='Get Lucky'), 5),
 ((SELECT id FROM users WHERE username='sveta'),   (SELECT id FROM track WHERE name='One More Time'), 5),
 ((SELECT id FROM users WHERE username='sergey'),  (SELECT id FROM track WHERE name='Time'), 5),
 ((SELECT id FROM users WHERE username='diana'),   (SELECT id FROM track WHERE name='Here Comes the Sun'), 5),
 ((SELECT id FROM users WHERE username='admin'),   (SELECT id FROM track WHERE name='King Kunta'), 5),
 ((SELECT id FROM users WHERE username='petr'),    (SELECT id FROM track WHERE name='Swimming Pools (Drank)'), 4),
 ((SELECT id FROM users WHERE username='olga'),    (SELECT id FROM track WHERE name='m.A.A.d city'), 5),
 ((SELECT id FROM users WHERE username='sergey'),  (SELECT id FROM track WHERE name='ENTER QTOPIA'), 4),
 ((SELECT id FROM users WHERE username='diana'),   (SELECT id FROM track WHERE name='НАС ОТКЛЮЧАЮТ'), 5)
ON CONFLICT DO NOTHING;

INSERT INTO comments (user_id, track_id, body, track_time) VALUES
 ((SELECT id FROM users WHERE username='timofey'), (SELECT id FROM track WHERE name='Only Shallow'), 'Шикарный звук и фактура гитар. Любовь. ❤️', 45),
 ((SELECT id FROM users WHERE username='masha'),   (SELECT id FROM track WHERE name='When You Sleep'), 'Стеки вокалов улетают в сон. ✨', 120),
 ((SELECT id FROM users WHERE username='petr'),    (SELECT id FROM track WHERE name='Soon'), 'Классика шугейза, гипноз. 😵‍💫', 210),

 ((SELECT id FROM users WHERE username='admin'),   (SELECT id FROM track WHERE name='ENTER QTOPIA'), 'Отличное интро, цепляет с первой секунды. 🔥', 12),
 ((SELECT id FROM users WHERE username='sergey'),  (SELECT id FROM track WHERE name='НАС ОТКЛЮЧАЮТ'), 'Гротеск и энергия — топ. 💥', 89),
 ((SELECT id FROM users WHERE username='diana'),   (SELECT id FROM track WHERE name='МЫ СОЗДАЛИ МОНСТРА'), 'Куплеты летят, продакшн жирный. 🧠', 115),

 ((SELECT id FROM users WHERE username='petr'),    (SELECT id FROM track WHERE name='Paranoid Android'), 'Свитч-секции делают трек вечным. 🌀', 160),
 ((SELECT id FROM users WHERE username='olga'),    (SELECT id FROM track WHERE name='Karma Police'), 'Фортепиано и атмосфера — мурашки. ❄️', 70),
 ((SELECT id FROM users WHERE username='masha'),   (SELECT id FROM track WHERE name='No Surprises'), 'Колыбельная для взрослых. 🌙', 30),

 ((SELECT id FROM users WHERE username='ivan'),    (SELECT id FROM track WHERE name='Get Lucky'), 'Грув на репите. 🕺', 100),
 ((SELECT id FROM users WHERE username='sveta'),   (SELECT id FROM track WHERE name='Instant Crush'), 'Очень мелодично и мягко. 🎶', 90),
 ((SELECT id FROM users WHERE username='admin'),   (SELECT id FROM track WHERE name='Contact'), 'Космос в финале альбома. 🚀', 250),

 ((SELECT id FROM users WHERE username='timofey'), (SELECT id FROM track WHERE name='One More Time'), 'Химия радости. 😍', 15),
 ((SELECT id FROM users WHERE username='masha'),   (SELECT id FROM track WHERE name='Digital Love'), 'Романтика синтов. 💜', 75),
 ((SELECT id FROM users WHERE username='sergey'),  (SELECT id FROM track WHERE name='Harder, Better, Faster, Stronger'), 'Культовый луп. 🔁', 60),

 ((SELECT id FROM users WHERE username='admin'),   (SELECT id FROM track WHERE name='Smells Like Teen Spirit'), 'Рифф века. ⚡', 35),
 ((SELECT id FROM users WHERE username='petr'),    (SELECT id FROM track WHERE name='Come As You Are'), 'Мокрый хорус — фирменно. 🌊', 50),
 ((SELECT id FROM users WHERE username='olga'),    (SELECT id FROM track WHERE name='Lithium'), 'Динамика куплет/припев шик. 📈', 110),

 ((SELECT id FROM users WHERE username='ivan'),    (SELECT id FROM track WHERE name='Time'), 'Колокола и соло — мурашки. ⏰', 200),
 ((SELECT id FROM users WHERE username='sveta'),   (SELECT id FROM track WHERE name='Money'), 'Ритмика 7/4 — кайф. 💵', 75),
 ((SELECT id FROM users WHERE username='sergey'),  (SELECT id FROM track WHERE name='Us and Them'), 'Сакс и пространство. 🌌', 190),

 ((SELECT id FROM users WHERE username='diana'),   (SELECT id FROM track WHERE name='Here Comes the Sun'), 'Самый тёплый лучик. ☀️', 22),
 ((SELECT id FROM users WHERE username='admin'),   (SELECT id FROM track WHERE name='King Kunta'), 'Фанк качает нереально. 🏃‍♂️', 60),
 ((SELECT id FROM users WHERE username='olga'),    (SELECT id FROM track WHERE name='m.A.A.d city'), 'Кинематографичный сторителлинг. 🎬', 140)
ON CONFLICT DO NOTHING;

UPDATE track t
SET rate_count = sub.cnt,
    rating     = sub.sum_rate
FROM (
  SELECT r.track_id, COUNT(*)::int AS cnt, SUM(r.rate)::bigint AS sum_rate
  FROM user_rating r
  GROUP BY r.track_id
) sub
WHERE t.id = sub.track_id;

COMMIT;

