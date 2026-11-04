/*
 * iMaps Android location bridge v2.
 * The GPS button and fix are NATIVE Android UI/LocationManager.
 * This JavaScript only paints the GPS point in the existing Leaflet WebGIS
 * and fills the existing geo form. It creates NO Leaflet controls, to avoid
 * disturbing OpenStreetMap attribution, zoom and existing map controls.
 */
(function () {
  'use strict';
  if (location.protocol !== 'https:' ||
      !/(^|\.)i-maps\.ir$/i.test(location.hostname)) return;
  if (window.IMAPSAndroidLocation && window.IMAPSAndroidLocation.version === 2) return;
  var marker = null, accuracyCircle = null, currentMap = null;
  var notice = null, noticeTimeout = 0;

  // Keep OSM attribution in the bottom-right corner of the MAP, never
  // floating at the center or below. Styles are scoped to Android WebGIS.
  function repairAttribution() {
    if (document.getElementById('imaps-android-osm-corner')) return;
    var style = document.createElement('style');
    style.id = 'imaps-android-osm-corner';
    style.textContent = [
      '.imaps-gis .leaflet-control-container .leaflet-bottom.leaflet-right {',
      ' position:absolute!important; right:2px!important; bottom:2px!important;',
      ' left:auto!important; top:auto!important; transform:none!important;',
      ' width:auto!important; height:auto!important; margin:0!important;',
      ' z-index:800!important; }',
      '.imaps-gis .leaflet-control-attribution {',
      ' margin:0!important; padding:1px 4px!important;',
      ' line-height:1.35!important; font-size:8px!important;',
      ' max-width:160px!important; box-shadow:none!important;',
      ' background:rgba(255,255,255,.87)!important;',
      ' white-space:normal!important; }',
      '.imaps-gis .leaflet-control-attribution a {font-size:8px!important;}'
    ].join('\n');
    (document.head || document.documentElement).appendChild(style);
  }

  function getReady() {
    var active = window.imapsWebGISReady;
    var root = active && active.root, map = active && active.map;
    if (!root || !map || !window.L || !root.classList ||
        !root.classList.contains('imaps-gis') || !map.getContainer ||
        !document.documentElement.contains(map.getContainer())) return null;
    return { root: root, map: map };
  }
  function notify(root, message, isError) {
    if (!notice || !notice.isConnected || !root.contains(notice)) {
      notice = document.createElement('div');
      notice.className = 'imaps-android-gps-note';
      notice.setAttribute('dir', 'rtl');
      notice.setAttribute('role', 'status');
      notice.style.cssText = [
        'position:relative;z-index:10;direction:rtl;text-align:center;',
        'font-size:12px;line-height:1.7;padding:10px 14px;',
        'border-radius:10px;margin:8px 8px 12px;',
        'box-shadow:0 3px 12px rgba(0,0,0,.14);'
      ].join('');
      var mapWrap = root.querySelector('.imaps-map-wrap');
      if (mapWrap && mapWrap.parentNode) {
        mapWrap.parentNode.insertBefore(notice, mapWrap);
      } else {
        root.insertBefore(notice, root.firstChild);
      }
    }
    notice.style.backgroundColor = isError ? '#9c2e31' : '#095c78';
    notice.style.color = '#fff';
    notice.textContent = message;
    if (noticeTimeout) clearTimeout(noticeTimeout);
    noticeTimeout = setTimeout(function () {
      if (notice && notice.isConnected) notice.remove();
    }, 11500);
  }
  function fillOneGeographicPoint(root, lat, lon) {
    var method = root.querySelector('input[name="imaps-iq-method"][value="geo"]');
    var panel = root.querySelector('[data-iq-method="geo"]');
    if (!method || !panel) return false;
    method.checked = true;
    method.dispatchEvent(new Event('change', { bubbles: true }));
    var rows = Array.prototype.slice.call(
      panel.querySelectorAll('.imaps-iq-coordinate-row'));
    if (!rows.length) return false;
    for (var i = rows.length - 1; i > 0; --i) rows[i].remove();
    var lonInput = rows[0].querySelector('[data-iq="longitude"]');
    var latInput = rows[0].querySelector('[data-iq="latitude"]');
    if (!latInput || !lonInput) return false;
    latInput.value = lat.toFixed(7);
    lonInput.value = lon.toFixed(7);
    [latInput, lonInput].forEach(function (el) {
      el.dispatchEvent(new Event('input', { bubbles: true }));
      el.dispatchEvent(new Event('change', { bubbles: true }));
    });
    var add = panel.querySelector('[data-add-point="geo"]');
    if (add) add.disabled = false;
    return true;
  }
  function drawLocation(active, lat, lon, accuracy) {
    var map = active.map, root = active.root;
    if (!window.L || !Number.isFinite(lat) || !Number.isFinite(lon) ||
        lat < -90 || lat > 90 || lon < -180 || lon > 180) return false;
    repairAttribution();
    if (currentMap !== map) {
      marker = null; accuracyCircle = null; currentMap = map;
    }
    if (marker && map.hasLayer(marker)) map.removeLayer(marker);
    if (accuracyCircle && map.hasLayer(accuracyCircle)) map.removeLayer(accuracyCircle);

    marker = L.circleMarker([lat,lon], {
      radius: 9, color: '#ffffff', weight: 3, fillColor: '#1978df',
      fillOpacity: 1, opacity: 1, bubblingMouseEvents: false
    }).addTo(map);

    if (Number.isFinite(accuracy) && accuracy > 0) {
      accuracyCircle = L.circle([lat,lon], {
        radius: Math.min(10000, accuracy), color: '#1982d2',
        weight: 1.4, opacity: .7, fillColor: '#1982d2',
        fillOpacity: .12, interactive: false
      }).addTo(map);
    }
    marker.bindPopup('📍 موقعیت من' +
      '<br>عرض: ' + lat.toFixed(6) +
      '<br>طول: ' + lon.toFixed(6) +
      (Number.isFinite(accuracy) ? '<br>دقت تقریبی: ' +
        Math.round(accuracy) + ' متر' : ''));
    map.invalidateSize({pan:false});
    map.setView([lat,lon], Math.max(map.getZoom(), 15), {animate:true});
    marker.openPopup();

    if (lat < 24 || lat > 41 || lon < 43 || lon > 64) {
      notify(root,
        'موقعیت فعلی شما روی نقشه مشخص شد؛ این نقطه خارج از پوشش استعلام ایران است.',
        true);
      return true;
    }

    if (!fillOneGeographicPoint(root, lat, lon)) {
      notify(root,
        'موقعیت روی نقشه مشخص شد، اما فرم استعلام هنوز بارگذاری نشده است.',
        true);
      return true;
    }
    notify(root,
      'موقعیت شما روی نقشه نمایش داده شد و مختصات در فرم ثبت شد. اکنون «استعلام» را بزنید.' +
      (accuracy > 100 ? ' دقت GPS تقریبی است؛ برای مرز زمین از مختصات دقیق استفاده کنید.' : ''),
      false);
    return true;
  }

  window.IMAPSAndroidLocation = {
    version: 2,
    apply: function (latitude, longitude, accuracy) {
      var lat = Number(latitude), lon = Number(longitude), acc = Number(accuracy);
      var attempts = 0;
      function paint() {
        var ready = getReady();
        if (ready) {
          try { return drawLocation(ready, lat, lon, acc); }
          catch (err) { return false; }
        }
        return false;
      }
      if (paint()) return true;
      var id = setInterval(function () {
        if (paint() || ++attempts >= 45) {
          clearInterval(id);
          if (attempts >= 45 && document.body) {
            var box = document.createElement('div');
            box.textContent = 'نقشه آماده نشده است؛ صفحه را تازه‌سازی و دوباره امتحان کنید.';
            box.style.cssText = 'background:#a32929;color:#fff;padding:12px;text-align:center;';
            document.body.prepend(box);
            setTimeout(function () {box.remove();}, 8000);
          }
        }
      }, 350);
      return true;
    }
  };

  repairAttribution();
})();
