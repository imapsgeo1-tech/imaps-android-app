/*
 * iMaps Android location bridge v3.
 * The location button sits INSIDE the actual map container (not the Leaflet
 * control pane). Clicking it invokes the native Android LocationManager via
 * an explicitly user-clicked custom URL. No background tracking.
 * The existing WebGIS geographic inputs and inquiry API are unchanged.
 */
(function () {
  'use strict';
  if (location.protocol !== 'https:' ||
      !/(^|\.)i-maps\.ir$/i.test(location.hostname)) return;
  if (window.IMAPSAndroidLocation && window.IMAPSAndroidLocation.version === 3) return;
  var marker = null, accuracyCircle = null, currentMap = null;
  var notice = null, noticeTimeout = 0;
  var locateButton = null;
  var locationMap = null;
  var locationResetBound = false;
  var setupAttempts = 0;

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

    // Clearly visible approximate GPS pin, drawn at the actual phone fix.
    // It does NOT assert a land boundary or survey-grade precision.
    var gpsPin = window.L.divIcon({
      className: 'imaps-android-gps-pin-wrap',
      html: '<span style="display:block;width:34px;height:40px;'
        + 'filter:drop-shadow(0 2px 4px rgba(0,0,0,.4))">'
        + '<svg xmlns="http://www.w3.org/2000/svg" width="34" height="40" '
        + 'viewBox="0 0 34 40" aria-hidden="true">'
        + '<path d="M17 1 C8 1 2 8 2 17 C2 29 17 39 17 39 '
        + 'C17 39 32 29 32 17 C32 8 26 1 17 1Z" '
        + 'fill="#1175df" stroke="#fff" stroke-width="2.6"/>'
        + '<circle cx="17" cy="16.5" r="6.3" fill="#fff"/>'
        + '</svg></span>',
      iconSize: [34,40],
      iconAnchor: [17,39],
      popupAnchor: [0,-35]
    });
    marker = window.L.marker([lat,lon], {
      icon: gpsPin, zIndexOffset: 1500, keyboard: false
    }).addTo(map);
    marker.bindTooltip('موقعیت تقریبی شما', {
      direction: 'top', offset:[0,-34], opacity: 0.95
    });

    if (Number.isFinite(accuracy) && accuracy > 0) {
      accuracyCircle = L.circle([lat,lon], {
        radius: Math.min(10000, accuracy), color: '#1982d2',
        weight: 1.4, opacity: .7, fillColor: '#1982d2',
        fillOpacity: .12, interactive: false
      }).addTo(map);
    }
    marker.bindPopup('📍 موقعیت تقریبی شما' +
      '<br>عرض: ' + lat.toFixed(6) +
      '<br>طول: ' + lon.toFixed(6) +
      (Number.isFinite(accuracy) ? '<br>دقت تقریبی: ' +
        Math.round(accuracy) + ' متر' : ''));
    map.invalidateSize({pan:false});
    map.setView([lat,lon], Math.max(map.getZoom(), 15), {animate:true});
    marker.openPopup();
    // Make the pin itself discoverable when the user closes the popup.
    marker.bringToFront && marker.bringToFront();

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

  function attachMapButton() {
    var ready = getReady();
    if (!ready || !ready.map || !ready.map.getContainer) return false;
    var mapContainer = ready.map.getContainer();
    if (!mapContainer || !mapContainer.isConnected) return false;
    // DOM overlay inside the Leaflet map, never a Leaflet control whose
    // position might be changed by custom theme/CSS rules.
    var existing = mapContainer.querySelector('#imaps-android-gps-on-map');
    if (existing) {
      locateButton = existing.querySelector('a');
      locationMap = ready.map;
      return true;
    }
    if (locateButton && locateButton.isConnected && locationMap !== ready.map) {
      locateButton.parentNode.remove();
    }
    var container = document.createElement('div');
    container.id = 'imaps-android-gps-on-map';
    // Compact icon at the lower-left INSIDE the map. This avoids the
    // existing map-settings toggle on the upper-right and OSM attribution
    // at the bottom-right. Keep a 48px inset for other map-edge overlays.
    container.style.cssText =
      'position:absolute!important;top:auto!important;right:auto!important;' +
      'bottom:56px!important;left:14px!important;z-index:1600!important;' +
      'width:46px!important;height:46px!important;pointer-events:auto!important;';
    var link = document.createElement('a');
    link.href = 'imaps://locate';
    // Icon-only, but retains a full accessible name and a 46px tap target.
    // The crosshair is inline SVG; no extra downloads or font dependencies.
    link.innerHTML = '<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" ' +
      'viewBox="0 0 24 24" fill="none" stroke="#08658c" stroke-width="2.2" ' +
      'stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">' +
      '<circle cx="12" cy="12" r="8"/><circle cx="12" cy="12" r="2.6"/>' +
      '<path d="M12 1.5v4M12 18.5v4M1.5 12h4M18.5 12h4"/>' +
      '</svg>';
    link.setAttribute('dir','rtl');
    link.setAttribute('role','button');
    link.setAttribute('aria-label','نمایش موقعیت فعلی روی نقشه');
    link.setAttribute('title','موقعیت من');
    link.style.cssText =
      'display:flex!important;align-items:center!important;' +
      'justify-content:center!important;box-sizing:border-box!important;' +
      'padding:0!important;width:46px!important;height:46px!important;' +
      'min-width:46px!important;min-height:46px!important;' +
      'border-radius:13px!important;' +
      'border:1px solid #bad2e2!important;background:#fff!important;' +
      'color:#074d6b!important;text-decoration:none!important;' +
      'box-shadow:0 2px 9px rgba(5,29,50,.25)!important;' +
      'touch-action:manipulation!important;cursor:pointer!important;';
    container.appendChild(link);
    mapContainer.appendChild(container);
    if (window.L.DomEvent) {
      window.L.DomEvent.disableClickPropagation(container);
      window.L.DomEvent.disableScrollPropagation(container);
    }
    locateButton = link;
    locationMap = ready.map;

    // Clearing the inquiry must also remove the location marker and circle;
    // this does not disturb user-drawn features or imported GIS files.
    if (!locationResetBound) {
      var resetButton = ready.root.querySelector('[data-iq="reset"]');
      if (resetButton) {
        resetButton.addEventListener('click', function () {
          if (marker && currentMap && currentMap.hasLayer(marker)) {
            currentMap.removeLayer(marker);
          }
          if (accuracyCircle && currentMap && currentMap.hasLayer(accuracyCircle)) {
            currentMap.removeLayer(accuracyCircle);
          }
          marker = null;
          accuracyCircle = null;
        });
        locationResetBound = true;
      }
    }
    return true;
  }

  function waitForMap() {
    if (attachMapButton()) return;
    if (++setupAttempts > 80) return;
    setTimeout(waitForMap, 400);
  }

  window.IMAPSAndroidLocation = {
    version: 3,
    setBusy: function (busy) {
      if (!locateButton || !locateButton.isConnected) attachMapButton();
      if (!locateButton) return;
      // Never swap the icon for text: a long label would again overlap
      // map settings. The icon dims while location acquisition runs.
      locateButton.style.opacity = busy ? '0.55' : '1';
      locateButton.setAttribute('aria-busy', busy ? 'true' : 'false');
    },
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
  // The map is typically initialized AFTER onPageCommitVisible; handle
  // either order, delayed Leaflet startup, and page ready notifications.
  waitForMap();
  window.addEventListener('imaps-webgis-ready', attachMapButton, {passive:true});
  document.addEventListener('DOMContentLoaded', attachMapButton, {once:true});
})();
