/* iMaps Android location-to-inquiry control.
 * Works with the EXISTING public WebGIS form and Leaflet map.
 * No raw location is transmitted until the user presses the site's استعلام button.
 * Requires an explicit tap and Android's runtime location permission.
 */
(function () {
  'use strict';
  // Only the official site may receive a native GPS enhancement.
  if (window.location.protocol !== 'https:' ||
      !/(^|\\.)i-maps\\.ir$/i.test(window.location.hostname)) return;
  if (window.__imapsAndroidLocationInstalled) return;
  window.__imapsAndroidLocationInstalled = true;

  var marker = null, accuracyCircle = null, attachedMap = null;
  function toast(root, message, error) {
    var status = root.querySelector('.imaps-android-location-status');
    if (!status) return;
    status.hidden = false;
    status.textContent = message;
    status.setAttribute('role', error ? 'alert' : 'status');
    status.style.color = error ? '#ffe3da' : '#ffffff';
    status.style.background = error ? '#a62b27' : '#124b6b';
  }

  function fillInquiry(root, lat, lng) {
    var method = root.querySelector('input[name="imaps-iq-method"][value="geo"]');
    var panel = root.querySelector('[data-iq-method="geo"]');
    if (!method || !panel) return false;
    method.checked = true;
    method.dispatchEvent(new Event('change', { bubbles: true }));

    var rows = panel.querySelectorAll('.imaps-iq-coordinate-row');
    if (!rows.length) return false;
    // A location inquiry is explicitly for ONE point: no stale coordinates
    // from previous multi-point inquiries can leak into this request.
    for (var i = rows.length - 1; i > 0; i--) rows[i].remove();

    var lonField = rows[0].querySelector('[data-iq="longitude"]');
    var latField = rows[0].querySelector('[data-iq="latitude"]');
    if (!lonField || !latField) return false;
    lonField.value = lng.toFixed(7);
    latField.value = lat.toFixed(7);
    [lonField, latField].forEach(function (field) {
      field.dispatchEvent(new Event('input', { bubbles: true }));
      field.dispatchEvent(new Event('change', { bubbles: true }));
    });

    var addButton = panel.querySelector('[data-add-point="geo"]');
    if (addButton) addButton.disabled = false;
    return true;
  }

  function found(root, map, position) {
    var lat = Number(position.coords.latitude);
    var lng = Number(position.coords.longitude);
    var accuracy = Number(position.coords.accuracy);
    if (!Number.isFinite(lat) || !Number.isFinite(lng) ||
        lat < -90 || lat > 90 || lng < -180 || lng > 180) {
      toast(root, 'موقعیت معتبر دریافت نشد؛ دوباره تلاش کنید.', true);
      return;
    }
    if (marker) map.removeLayer(marker);
    if (accuracyCircle) map.removeLayer(accuracyCircle);

    var circleOptions = { radius: 8, color: '#ffffff', weight: 3,
      fillColor: '#147ace', fillOpacity: 1, opacity: 1 };
    marker = window.L.circleMarker([lat, lng], circleOptions).addTo(map);
    var popupText = '📍 موقعیت فعلی شما<br>عرض: ' + lat.toFixed(6)
      + '<br>طول: ' + lng.toFixed(6)
      + (Number.isFinite(accuracy) ? '<br>دقت تقریبی: ' + Math.round(accuracy) + ' متر' : '');
    marker.bindPopup(popupText, {autoClose: true});
    if (Number.isFinite(accuracy) && accuracy > 0) {
      accuracyCircle = window.L.circle([lat, lng], {
        radius: Math.min(accuracy, 20000),
        color: '#168bc2', weight: 1.5, opacity: .6, fillColor: '#168bc2',
        fillOpacity: .12, interactive: false
      }).addTo(map);
    }
    map.setView([lat, lng], Math.max(map.getZoom(), 15), { animate: true });
    marker.openPopup();

    // Backend index coverage is in Iran only. Show the real device location
    // anywhere, but do not feed an unsupported coordinate to the inquiry.
    if (lat < 24 || lat > 41 || lng < 43 || lng > 64) {
      toast(root, 'موقعیت شما روی نقشه نمایش داده شد، اما خارج از محدوده استعلام ایران است.', true);
      return;
    }
    if (!fillInquiry(root, lat, lng)) {
      toast(root, 'نقطه روی نقشه مشخص شد؛ فرم مختصات هنوز آماده نیست. دوباره تلاش کنید.', true);
      return;
    }
    toast(root, 'مختصات موقعیت شما ثبت شد. برای دریافت سال‌های عکسبرداری، دکمه «استعلام» را بزنید.'
      + (Number.isFinite(accuracy) && accuracy > 100 ? ' دقت موقعیت فعلی تقریبی است.' : ''), false);
  }

  function locate(root, map, button) {
    if (!navigator.geolocation || !window.L) {
      toast(root, 'موقعیت‌یاب در دستگاه شما فعال نیست.', true);
      return;
    }
    button.disabled = true;
    button.textContent = '◌ در حال مکان‌یابی';
    toast(root, 'در حال دریافت موقعیت شما؛ اجازه دسترسی به موقعیت مکانی را تأیید کنید.', false);

    navigator.geolocation.getCurrentPosition(
      function (position) {
        button.disabled = false;
        button.textContent = '◎ موقعیت من';
        found(root, map, position);
      },
      function (error) {
        button.disabled = false;
        button.textContent = '◎ موقعیت من';
        var detail = error && error.code === 1
          ? 'اجازه دسترسی به موقعیت داده نشد؛ آن را در تنظیمات گوشی فعال کنید.'
          : error && error.code === 3
            ? 'دریافت موقعیت زمان‌بر شد. GPS گوشی را روشن و دوباره تلاش کنید.'
            : 'موقعیت در دسترس نیست. GPS و اینترنت گوشی را بررسی کنید.';
        toast(root, detail, true);
      },
      { enableHighAccuracy: true, timeout: 20000, maximumAge: 5000 }
    );
  }

  function install() {
    var ready = window.imapsWebGISReady;
    var root = ready && ready.root;
    var map = ready && ready.map;
    if (!root || !map || !window.L || !root.matches('.imaps-gis') ||
        !root.querySelector('input[name="imaps-iq-method"][value="geo"]')) {
      return false;
    }
    if (root.querySelector('.imaps-android-location-control')) return true;
    if (attachedMap !== map) {
      marker = null;
      accuracyCircle = null;
      attachedMap = map;
    }

    var control = window.L.control({ position: 'topright' });
    control.onAdd = function () {
      var wrap = window.L.DomUtil.create('div', 'imaps-android-location-control');
      wrap.setAttribute('dir', 'rtl');
      wrap.style.cssText = 'max-width:220px; direction:rtl; text-align:center;'
        + 'font-family:inherit; z-index:900;';
      var button = document.createElement('button');
      button.type = 'button';
      button.className = 'imaps-android-location-button';
      button.textContent = '◎ موقعیت من';
      button.setAttribute('aria-label', 'نمایش موقعیت فعلی روی نقشه');
      button.style.cssText = 'display:block; min-width:118px; padding:10px 12px;'
        + 'background:#fff; color:#064e65; border:1px solid #b6c9d5;'
        + 'border-radius:12px; box-shadow:0 3px 11px rgba(0,0,0,.18);'
        + 'font-weight:700; font-size:13px; cursor:pointer;';
      var status = document.createElement('div');
      status.className = 'imaps-android-location-status';
      status.hidden = true;
      status.style.cssText = 'margin-top:6px;max-width:208px;padding:8px 10px;'
        + 'border-radius:9px;box-shadow:0 3px 10px rgba(0,0,0,.15);'
        + 'font-size:11px;line-height:1.7;';
      wrap.appendChild(button);
      wrap.appendChild(status);
      window.L.DomEvent.disableClickPropagation(wrap);
      window.L.DomEvent.disableScrollPropagation(wrap);
      button.addEventListener('click', function () { locate(root, map, button); });
      return wrap;
    };
    control.addTo(map);
    return true;
  }

  if (install()) return;
  var tries = 0;
  var interval = setInterval(function () {
    if (install() || ++tries > 50) clearInterval(interval);
  }, 500);
  window.addEventListener('imaps-webgis-ready', function () {
    if (install()) clearInterval(interval);
  }, { passive: true });
})();
