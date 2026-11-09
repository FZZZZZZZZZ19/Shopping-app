(function () {
  'use strict';
  var platform = __PLATFORM__;
  var result = {items: [], login: false, verification: false, empty: false, title: document.title || ''};
  var seen = new Set();
  function clean(s) { return String(s || '').replace(/\s+/g, ' ').trim(); }
  function text(n) { return n ? clean(n.textContent) : ''; }
  function shown(n) {
    if (!n || n.closest('[hidden], [aria-hidden="true"]')) return false;
    if (typeof getComputedStyle === 'function') {
      for (var p = n, depth = 0; p && depth < 6; p = p.parentElement, depth++) {
        var style = getComputedStyle(p); if (style.display === 'none' || style.visibility === 'hidden') return false;
      }
    }
    return true;
  }
  function https(u) {
    if (!clean(u)) return '';
    try { var v = new URL(u, location.href); return v.protocol === 'https:' && !v.username && !v.password ? v.href : ''; }
    catch (e) { return ''; }
  }
  function productUrl(u) {
    var url = https(u); if (!url) return '';
    var v = new URL(url), h = v.hostname.toLowerCase();
    if (platform === 'TAOBAO' || platform === 'TMALL') {
      if (!/(^|\.)(taobao|tmall)\.com$/.test(h)) return '';
      return /(?:item\.htm|detail\.htm|\/detail\/|\/i\d)/.test(v.pathname) && (v.searchParams.has('id') || /\/i\d/.test(v.pathname)) ? url : '';
    }
    if (platform === 'PDD') return /(^|\.)(yangkeduo|pinduoduo)\.com$/.test(h) && v.searchParams.has('goods_id') ? url : '';
    if (platform === 'JD') return /(^|\.)jd\.com$/.test(h) && (/\/(?:product\/)?\d+\.html$/.test(v.pathname) || v.searchParams.has('wareId') || v.searchParams.has('skuId')) ? url : '';
    return '';
  }
  function itemId(url) {
    try { var u = new URL(url); return u.searchParams.get('id') || u.searchParams.get('goods_id') || u.searchParams.get('wareId') || u.searchParams.get('skuId') || (u.pathname.match(/\d{4,}/) || [url])[0]; }
    catch (e) { return ''; }
  }
  function amount(s) {
    s = clean(s).replace(/[,，\s]/g, '');
    if (/每期|月供|立减|补贴|返现|运费|定金|订金|已售|券额|省/.test(s)) return null;
    var m = s.match(/^(?:¥|￥|人民币)?([0-9]+(?:\.[0-9]{1,2})?)(?:元|起)?$/);
    if (!m || Number(m[1]) > 99999999.99) return null;
    return {value: m[1], starting: /起/.test(s)};
  }
  function add(item) {
    if (!item || !item.id || !item.title || clean(item.title).length < 3 || !item.price || seen.has(String(item.id))) return;
    if (result.items.length >= 100) return;
    seen.add(String(item.id));
    result.items.push({id: String(item.id), title: clean(item.title).slice(0, 500), shop: clean(item.shop).slice(0, 150), price: String(item.price), url: item.url || '', image: item.image || '', starting: !!item.starting});
  }
  // Only consume public DOM/JSON-LD. No cookies, account fields, private APIs, or JS globals are read.
  function jsonLd(node) {
    if (!node || typeof node !== 'object') return;
    if (Array.isArray(node)) { node.forEach(jsonLd); return; }
    var type = node['@type'];
    if (type === 'Product' || (Array.isArray(type) && type.indexOf('Product') >= 0)) {
      var offer = Array.isArray(node.offers) ? node.offers[0] : node.offers;
      if (offer && (!offer.priceCurrency || offer.priceCurrency === 'CNY')) {
        var price = amount(String(offer.price === undefined ? '' : offer.price));
        var link = productUrl(offer.url || node.url || location.href);
        if (price && link) add({id: itemId(link), title: node.name, shop: offer.seller && offer.seller.name || node.seller && node.seller.name || '', price: price.value, url: link, image: https(Array.isArray(node.image) ? node.image[0] : node.image || ''), starting: false});
      }
    }
    if (node['@graph']) jsonLd(node['@graph']);
    if (node.itemListElement) jsonLd(node.itemListElement);
    if (node.item) jsonLd(node.item);
  }
  Array.from(document.querySelectorAll('script[type="application/ld+json"]')).forEach(function (s) { try { jsonLd(JSON.parse(s.textContent)); } catch (e) {} });

  var priceSelector = '[class*="price"], [class*="Price"], [data-price], [itemprop="price"]';
  var titleSelector = '[class*="goods-title"], [class*="goods_name"], [class*="goodsName"], [class*="ware-name"], [class*="product-name"], [class*="title"], [class*="Title"], [itemprop="name"]';
  var shopSelector = '[class*="shopName"], [class*="shop-name"], [class*="shop_name"], [class*="shopname"], [class*="shop--"], [class*="mallName"], [class*="mall-name"], [class*="mall_name"], [itemprop="seller"]';
  // PDD publishes CSS-module names in its referenced search_result bundle (checked 2026-11-09).
  if (platform === 'PDD') {
    priceSelector += ', ._3YpshYgC, ._2psv9tFx';
    titleSelector += ', ._3U_Fp4u_, ._1YcMjCeq';
    shopSelector += ', ._2wauhnMy, .vnyB3s9r, .v0_K-cGz';
  }
  function linksIn(card) {
    var all = []; if (card.matches && card.matches('a[href]')) all.push(card);
    return all.concat(Array.from(card.querySelectorAll('a[href]'))).map(function (a) { return productUrl(a.getAttribute('href')); }).filter(Boolean);
  }
  function titleIn(card) {
    var all = Array.from(card.querySelectorAll(titleSelector));
    for (var i = 0; i < all.length; i++) {
      if (!shown(all[i]) || /shop|mall|店铺/i.test(all[i].className || '')) continue;
      var t = clean(all[i].getAttribute('title') || text(all[i]));
      if (t.length >= 3 && t.length <= 300 && !/^(?:广告|点击|立即购买|查看详情)/.test(t)) return t;
    }
    var img = card.querySelector('img[alt]');
    if (img && clean(img.getAttribute('alt')).length >= 3) return clean(img.getAttribute('alt'));
    var a = card.querySelector('a[title]');
    if (a && productUrl(a.getAttribute('href'))) return clean(a.getAttribute('title'));
    return '';
  }
  function priceIn(card) {
    var all = Array.from(card.querySelectorAll(priceSelector));
    for (var i = 0; i < all.length; i++) {
      var n = all[i], cls = String(n.className || '');
      if (!shown(n) || n.closest('del,s,strike') || /old|origin|market|original|line|coupon|save|discount|installment|freight/i.test(cls)) continue;
      // Read whole price nodes before numeric fragments, e.g. <span>99</span><span>.90</span>.
      if (n.parentElement && n.parentElement.matches(priceSelector) && card.contains(n.parentElement)) continue;
      var raw = n.getAttribute('data-price') || n.getAttribute('content') || text(n);
      var parsed = amount(raw);
      if (parsed) return parsed;
    }
    return null;
  }
  function fromCard(card, explicitUrl, allowUnlinked) {
    if (!card || !shown(card) || /^(?:BODY|HTML)$/.test(card.tagName)) return;
    var links = linksIn(card); if (explicitUrl) links.push(explicitUrl);
    var ids = new Set(links.map(itemId));
    // Never take a price from a shared container containing multiple products.
    if (ids.size > 1 || (!allowUnlinked && ids.size !== 1)) return;
    var title = titleIn(card), price = priceIn(card);
    if (!title || !price) return;
    var shop = card.querySelector(shopSelector);
    if (!shop) {
      shop = Array.from(card.querySelectorAll('a[href]')).find(function (a) { var h = a.getAttribute('href') || ''; return /(?:shop|mall)\./.test(h) && !productUrl(h); });
    }
    var img = card.querySelector('img');
    add({id: ids.size ? Array.from(ids)[0] : 'dom:' + title + ':' + text(shop), title: title, shop: text(shop), price: price.value, starting: price.starting, url: links[0] || '', image: img ? https(img.getAttribute('src') || img.getAttribute('data-src') || '') : ''});
  }
  Array.from(document.querySelectorAll('a[href]')).slice(0, 1200).forEach(function (a) {
    if (!productUrl(a.getAttribute('href'))) return;
    var n = a;
    for (var depth = 0; n && depth < 7; depth++, n = n.parentElement) {
      var before = result.items.length; fromCard(n); if (result.items.length > before) break;
    }
  });
  Array.from(document.querySelectorAll('[data-goods-id], [data-itemid], [data-sku], li[sku]')).slice(0, 150).forEach(function (card) {
    var id = card.getAttribute('data-goods-id') || card.getAttribute('data-itemid') || card.getAttribute('data-sku') || card.getAttribute('sku') || '';
    if (!/^\d{4,}$/.test(id)) return;
    var url = platform === 'PDD' ? 'https://mobile.yangkeduo.com/goods.html?goods_id=' + id : platform === 'JD' ? 'https://item.m.jd.com/product/' + id + '.html' : 'https://item.taobao.com/item.htm?id=' + id;
    fromCard(card, url);
  });
  var explicitCards = '[class*="goods-item"], [class*="goodsItem"], [class*="product-item"], [class*="productItem"]';
  if (platform === 'PDD') explicitCards += ', ._13ArDeh4, ._2EYTUfrB';
  Array.from(document.querySelectorAll(explicitCards)).slice(0, 150).forEach(function (card) {
    if (card.querySelectorAll('img').length === 1) fromCard(card, '', true);
  });
  // Login and challenge detection is status only. No challenge is solved or bypassed.
  var sample = clean(document.body && document.body.innerText || document.body && document.body.textContent || '').slice(0, 12000);
  result.verification = /验证码|滑块验证|安全验证|访问过于频繁|访问受限|异常流量|风险验证|请完成验证/.test(sample) && !result.items.length;
  result.login = (/login|passport|signin/i.test(location.hostname + location.pathname) || !!document.querySelector('input[type="password"]') || /请先登录|登录后查看|登录后搜索|扫码登录|短信登录|验证码登录/.test(sample)) && !result.items.length;
  result.empty = /没有找到相关商品|暂无商品|无相关商品|没有相关商品|未找到相关商品/.test(sample) && !result.items.length;
  return JSON.stringify(result);
})();
