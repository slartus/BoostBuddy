package ru.slartus.boostbuddy.data.pairing.web

import kotlinx.serialization.json.JsonPrimitive

internal class PairingWebAsset(val contentType: String, val body: String)

internal object PairingWebPages {
    const val PATH_PAGE_P = "/p"
    const val PATH_SCRIPT_P = "/p.js"
    const val PATH_PAGE_S = "/s"
    const val PATH_SCRIPT_S = "/s.js"
    const val PATH_STYLE = "/pair.css"
    const val PATH_SEND_PREFIX = "/s/"
    const val MAX_SEALED_BYTES = 8 * 1024

    private const val CONTENT_TYPE_HTML = "text/html; charset=utf-8"
    private const val CONTENT_TYPE_JS = "text/javascript; charset=utf-8"
    private const val CONTENT_TYPE_CSS = "text/css; charset=utf-8"

    val BOOKMARKLET_JS: String = """
        (function () {
          'use strict';
          var BOX_ID = 'boostbuddy-pairing-status';
          var VERSION = 1;

          if (location.protocol !== 'https:' || location.hostname !== 'boosty.to') {
            fail('Запустите закладку на сайте boosty.to.');
            return;
          }
          var match = /(?:^#|&)bb=([^&]*)/.exec(location.hash);
          if (!match) {
            fail('Нет данных для переноса. Откройте boosty.to по ссылке со страницы BoostBuddy и запустите закладку снова.');
            return;
          }
          try {
            history.replaceState(history.state, '', location.pathname + location.search);
          } catch (e) {
          }
          var parts;
          try {
            parts = decodeURIComponent(match[1]).split('|');
          } catch (e) {
            parts = [];
          }
          var target = parts.length === 3 ? parseTarget(parts[0], parts[1], parts[2]) : null;
          if (!target) {
            fail('Данные для переноса повреждены. Отсканируйте QR-код на экране BoostBuddy заново.');
            return;
          }
          var auth = parseAuth(readValue('auth'));
          if (!auth) {
            fail('В этом браузере не найден вход в Boosty. Войдите в аккаунт на boosty.to и запустите закладку снова.');
            return;
          }
          var deviceId = stripQuotes(readValue('_clientId'));
          if (!deviceId) {
            fail('Не найден идентификатор браузера Boosty. Обновите страницу boosty.to и запустите закладку снова.');
            return;
          }
          if (!window.crypto || !window.crypto.subtle || typeof TextEncoder === 'undefined') {
            fail('Браузер не поддерживает шифрование. Обновите браузер и попробуйте снова.');
            return;
          }
          var payload = {
            accessToken: auth.accessToken,
            refreshToken: auth.refreshToken,
            expiresAt: auth.expiresAt,
            deviceId: deviceId,
            source: 'web',
            iat: Math.floor(Date.now() / 1000)
          };
          show('Передаём вход в BoostBuddy…', false);
          seal(target, JSON.stringify(payload)).then(function (sealed) {
            location.replace('http://' + target.host + '/s#s=' + target.sid + '&sealed=' + sealed);
          }, function () {
            fail('Не удалось зашифровать данные для переноса.');
          });

          function parseTarget(host, sid, key) {
            var m = /^(\d{1,3})\.(\d{1,3})\.(\d{1,3})\.(\d{1,3}):(\d{1,5})$/.exec(host);
            if (!m) return null;
            for (var i = 1; i <= 4; i++) {
              if ((m[i].length > 1 && m[i].charAt(0) === '0') || Number(m[i]) > 255) return null;
            }
            var a = Number(m[1]);
            var b = Number(m[2]);
            var port = Number(m[5]);
            var isPrivate = a === 10 || (a === 172 && b >= 16 && b <= 31) || (a === 192 && b === 168);
            if (!isPrivate || port < 1 || port > 65535) return null;
            if (!/^[A-Za-z0-9_-]{22}$/.test(sid) || !/^[A-Za-z0-9_-]{43}$/.test(key)) return null;
            var keyBytes;
            try {
              keyBytes = fromBase64Url(key);
            } catch (e) {
              return null;
            }
            if (keyBytes.length !== 32) return null;
            return { host: host, sid: sid, key: keyBytes };
          }

          function readValue(name) {
            var value = readCookie(name);
            if (value !== null) {
              try {
                value = decodeURIComponent(value);
              } catch (e) {
              }
            } else {
              try {
                value = window.localStorage.getItem(name);
              } catch (e) {
                value = null;
              }
            }
            if (value === null || value === undefined) return null;
            value = String(value).trim();
            return value.length ? value : null;
          }

          function readCookie(name) {
            var cookies = document.cookie ? document.cookie.split(';') : [];
            for (var i = 0; i < cookies.length; i++) {
              var separator = cookies[i].indexOf('=');
              if (separator < 0) continue;
              if (cookies[i].slice(0, separator).trim() === name) return cookies[i].slice(separator + 1).trim();
            }
            return null;
          }

          function stripQuotes(value) {
            if (!value) return null;
            if (value.length >= 2 && value.charAt(0) === '"' && value.charAt(value.length - 1) === '"') {
              value = value.slice(1, -1).trim();
            }
            return value.length ? value : null;
          }

          function parseAuth(text) {
            if (!text) return null;
            var auth;
            try {
              auth = JSON.parse(text);
              if (typeof auth === 'string') auth = JSON.parse(auth);
            } catch (e) {
              return null;
            }
            if (!auth || typeof auth.accessToken !== 'string' || !auth.accessToken) return null;
            if (typeof auth.refreshToken !== 'string' || !auth.refreshToken) return null;
            return { accessToken: auth.accessToken, refreshToken: auth.refreshToken, expiresAt: toEpoch(auth.expiresAt) };
          }

          function toEpoch(value) {
            var number = null;
            if (typeof value === 'number' && isFinite(value)) number = value;
            else if (typeof value === 'string' && /^\d+$/.test(value)) number = Number(value);
            if (number === null) return null;
            return Math.floor(number > 1e11 ? number / 1000 : number);
          }

          function seal(target, text) {
            var encoder = new TextEncoder();
            var nonce = window.crypto.getRandomValues(new Uint8Array(12));
            var aad = concat([encoder.encode('BB-PAIR'), new Uint8Array([VERSION]), encoder.encode(target.sid)]);
            return window.crypto.subtle.importKey('raw', target.key, { name: 'AES-GCM' }, false, ['encrypt'])
              .then(function (key) {
                return window.crypto.subtle.encrypt(
                  { name: 'AES-GCM', iv: nonce, additionalData: aad, tagLength: 128 },
                  key,
                  encoder.encode(text)
                );
              })
              .then(function (ciphertext) {
                return toBase64Url(concat([new Uint8Array([VERSION]), nonce, new Uint8Array(ciphertext)]));
              });
          }

          function concat(chunks) {
            var size = 0;
            for (var i = 0; i < chunks.length; i++) size += chunks[i].length;
            var result = new Uint8Array(size);
            var offset = 0;
            for (var j = 0; j < chunks.length; j++) {
              result.set(chunks[j], offset);
              offset += chunks[j].length;
            }
            return result;
          }

          function toBase64Url(bytes) {
            var binary = '';
            for (var i = 0; i < bytes.length; i++) binary += String.fromCharCode(bytes[i]);
            return btoa(binary).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
          }

          function fromBase64Url(text) {
            var binary = atob(text.replace(/-/g, '+').replace(/_/g, '/') + '==='.slice((text.length + 3) % 4));
            var bytes = new Uint8Array(binary.length);
            for (var i = 0; i < binary.length; i++) bytes[i] = binary.charCodeAt(i);
            return bytes;
          }

          function fail(text) {
            show(text, true);
          }

          function show(text, isError) {
            var box = document.getElementById(BOX_ID);
            if (!box) {
              box = document.createElement('div');
              box.id = BOX_ID;
              box.setAttribute('role', 'status');
              box.style.cssText = 'position:fixed;left:12px;right:12px;top:12px;z-index:2147483647;' +
                'padding:14px 16px;border-radius:8px;font:16px/1.4 sans-serif;color:#fff;' +
                'box-shadow:0 2px 12px rgba(0,0,0,.4)';
              document.body.appendChild(box);
            }
            box.style.background = isError ? '#b3261e' : '#1f6f43';
            box.textContent = text;
          }
        })();
    """.trimIndent().lines().joinToString("\n", transform = String::trim)

    val PAGE_P: String = """
        <!doctype html>
        <html lang="ru">
        <head>
        <meta charset="utf-8">
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <meta name="referrer" content="no-referrer">
        <title>BoostBuddy: вход по QR</title>
        <link rel="stylesheet" href="$PATH_STYLE">
        </head>
        <body>
        <main>
        <h1>Вход в BoostBuddy</h1>
        <p id="error" class="status error" hidden></p>
        <div id="content">
        <p class="warning">После переноса этот браузер выйдет из Boosty — войти в нём можно будет заново.</p>
        <section>
        <h2>BoostBuddy установлен на этом устройстве</h2>
        <p>Откройте приложение — оно передаст вход само.</p>
        <p><a id="open-app" class="button" href="#">Открыть в BoostBuddy</a></p>
        </section>
        <section>
        <h2>Перенести вход из браузера</h2>
        <ol>
        <li>
        <p>Скопируйте закладку:</p>
        <p><button id="copy-bookmarklet" type="button" class="button">Скопировать закладку</button></p>
        <textarea id="bookmarklet" readonly rows="3" aria-label="Текст закладки"></textarea>
        <p id="copy-status" class="hint" aria-live="polite"></p>
        </li>
        <li>
        <p>Создайте закладку и вставьте скопированный текст в поле адреса (URL). Название — например, «BB».</p>
        <h3>Компьютер</h3>
        <p>Нажмите Ctrl+D (на Mac — Cmd+D) на любой странице и сохраните закладку. Затем в списке закладок нажмите на неё правой кнопкой, выберите «Изменить» и замените адрес скопированным текстом.</p>
        <h3>Android, Chrome</h3>
        <p>Откройте меню (три точки), нажмите звёздочку, чтобы добавить страницу в закладки, затем «Изменить». Замените адрес скопированным текстом и сохраните.</p>
        <h3>iPhone, Safari</h3>
        <p>Нажмите «Поделиться» → «Добавить закладку» и сохраните. Откройте «Закладки» → «Изменить», выберите новую закладку и замените адрес скопированным текстом.</p>
        </li>
        <li>
        <p>Откройте Boosty в этом браузере (вы должны быть в нём авторизованы):</p>
        <p><a id="go-boosty" class="button" href="#" target="_blank" rel="noopener noreferrer">Перейти на boosty.to</a></p>
        </li>
        <li>
        <p>На открывшейся странице boosty.to запустите закладку:</p>
        <ul>
        <li>Компьютер — нажмите на закладку на панели или в меню закладок.</li>
        <li>Android, Chrome — начните вводить её название («BB») в адресной строке и выберите закладку из подсказок.</li>
        <li>iPhone, Safari — откройте «Закладки» и нажмите на неё.</li>
        </ul>
        </li>
        </ol>
        </section>
        </div>
        </main>
        <script src="$PATH_SCRIPT_P"></script>
        </body>
        </html>
    """.trimIndent()

    private val SCRIPT_P_BODY: String = """
        (function () {
          'use strict';
          var KEY_PATTERN = /^[A-Za-z0-9_-]{43}$/;
          var SID_PATTERN = /^[A-Za-z0-9_-]{22}$/;

          var params = parseHash(location.hash);
          try {
            history.replaceState(null, '', location.pathname);
          } catch (e) {
          }
          var key = params.k || '';
          var sid = params.s || '';
          if (!KEY_PATTERN.test(key) || !SID_PATTERN.test(sid)) {
            byId('content').hidden = true;
            var error = byId('error');
            error.textContent = 'Ссылка повреждена. Отсканируйте QR-код на экране BoostBuddy ещё раз.';
            error.hidden = false;
            return;
          }
          var host = location.hostname + ':' + (location.port || '80');
          byId('open-app').href = 'boostbuddy://pair?h=' + host + '&s=' + sid + '#k=' + key;
          byId('go-boosty').href = 'https://boosty.to/#bb=' + encodeURIComponent(host + '|' + sid + '|' + key);

          var bookmarklet = 'javascript:' + encodeURIComponent(BOOKMARKLET_SOURCE);
          var field = byId('bookmarklet');
          var copyStatus = byId('copy-status');
          field.value = bookmarklet;
          byId('copy-bookmarklet').addEventListener('click', function () {
            copyText(bookmarklet).then(function () {
              copyStatus.textContent = 'Скопировано. Теперь создайте закладку по инструкции ниже.';
            }, function () {
              copyStatus.textContent = 'Не удалось скопировать автоматически. Выделите весь текст в поле выше и скопируйте его вручную.';
            });
          });

          function copyText(text) {
            if (navigator.clipboard && window.isSecureContext) {
              return navigator.clipboard.writeText(text);
            }
            return new Promise(function (resolve, reject) {
              field.focus();
              field.select();
              field.setSelectionRange(0, text.length);
              var copied = false;
              try {
                copied = document.execCommand('copy');
              } catch (e) {
                copied = false;
              }
              if (copied) resolve(); else reject();
            });
          }

          function parseHash(hash) {
            var result = {};
            var parts = hash.replace(/^#/, '').split('&');
            for (var i = 0; i < parts.length; i++) {
              var separator = parts[i].indexOf('=');
              if (separator > 0) result[parts[i].slice(0, separator)] = parts[i].slice(separator + 1);
            }
            return result;
          }

          function byId(id) {
            return document.getElementById(id);
          }
        })();
    """.trimIndent()

    val SCRIPT_P: String =
        "var BOOKMARKLET_SOURCE = ${JsonPrimitive(BOOKMARKLET_JS)};\n$SCRIPT_P_BODY"

    val PAGE_S: String = """
        <!doctype html>
        <html lang="ru">
        <head>
        <meta charset="utf-8">
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <meta name="referrer" content="no-referrer">
        <title>BoostBuddy: передача входа</title>
        <link rel="stylesheet" href="$PATH_STYLE">
        </head>
        <body>
        <main>
        <h1>Передача входа в BoostBuddy</h1>
        <p id="status" class="status" aria-live="polite">Подготовка…</p>
        <p><button id="retry" type="button" class="button" hidden>Повторить</button></p>
        </main>
        <script src="$PATH_SCRIPT_S"></script>
        </body>
        </html>
    """.trimIndent()

    val SCRIPT_S: String = """
        (function () {
          'use strict';
          var SID_PATTERN = /^[A-Za-z0-9_-]{22}$/;
          var SEALED_PATTERN = /^[A-Za-z0-9_-]{39,$MAX_SEALED_BYTES}$/;
          var status = document.getElementById('status');
          var retry = document.getElementById('retry');

          var params = parseHash(location.hash);
          try {
            history.replaceState(null, '', location.pathname);
          } catch (e) {
          }
          if (!SID_PATTERN.test(params.s || '') || !SEALED_PATTERN.test(params.sealed || '')) {
            show('Данные для переноса не найдены или повреждены. Начните заново со сканирования QR-кода.', 'error');
            return;
          }
          retry.addEventListener('click', send);
          send();

          function send() {
            retry.hidden = true;
            show('Передаём вход в BoostBuddy…', '');
            fetch('$PATH_SEND_PREFIX' + params.s, {
              method: 'POST',
              headers: { 'Content-Type': 'text/plain' },
              body: params.sealed,
              cache: 'no-store',
              credentials: 'omit',
              referrerPolicy: 'no-referrer'
            }).then(function (response) {
              if (response.ok) {
                show('Готово! Вход передан в BoostBuddy. Эту страницу можно закрыть.', 'ok');
              } else if (response.status === 404 || response.status === 410) {
                show('Сеанс входа устарел. Отсканируйте новый QR-код на экране BoostBuddy.', 'error');
              } else {
                show('BoostBuddy не принял данные (код ' + response.status + ').', 'error');
                retry.hidden = !(response.status >= 500 || response.status === 408);
              }
            }, function () {
              show('Нет связи с BoostBuddy. Проверьте, что устройство в той же сети и экран входа по QR открыт.', 'error');
              retry.hidden = false;
            });
          }

          function show(text, kind) {
            status.textContent = text;
            status.className = 'status ' + kind;
          }

          function parseHash(hash) {
            var result = {};
            var parts = hash.replace(/^#/, '').split('&');
            for (var i = 0; i < parts.length; i++) {
              var separator = parts[i].indexOf('=');
              if (separator > 0) result[parts[i].slice(0, separator)] = parts[i].slice(separator + 1);
            }
            return result;
          }
        })();
    """.trimIndent()

    val STYLE: String = """
        body { margin: 0; background: #121212; color: #eee; font: 16px/1.5 -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; }
        main { max-width: 640px; margin: 0 auto; padding: 16px; }
        h1 { font-size: 1.5em; }
        h2 { font-size: 1.2em; margin-top: 1.5em; }
        h3 { font-size: 1em; margin-bottom: 0.25em; }
        a { color: #ff8a5c; }
        .warning { padding: 12px 16px; border-radius: 8px; background: #4a3b00; color: #ffe08a; }
        .button { display: inline-block; padding: 12px 20px; border: 0; border-radius: 8px; background: #f15f2c; color: #fff; font: inherit; text-decoration: none; cursor: pointer; }
        textarea { width: 100%; box-sizing: border-box; font: 12px/1.4 monospace; }
        .hint { color: #aaa; }
        .status.ok { color: #7fd99b; }
        .status.error { color: #ff8a80; }
        [hidden] { display: none !important; }
    """.trimIndent()

    val assets: Map<String, PairingWebAsset> = mapOf(
        PATH_PAGE_P to PairingWebAsset(CONTENT_TYPE_HTML, PAGE_P),
        PATH_SCRIPT_P to PairingWebAsset(CONTENT_TYPE_JS, SCRIPT_P),
        PATH_PAGE_S to PairingWebAsset(CONTENT_TYPE_HTML, PAGE_S),
        PATH_SCRIPT_S to PairingWebAsset(CONTENT_TYPE_JS, SCRIPT_S),
        PATH_STYLE to PairingWebAsset(CONTENT_TYPE_CSS, STYLE),
    )
}
